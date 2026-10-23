#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
POS 体验巡检（基于 API 的自动化体验扫描）

目的：不只跑「核心链路通不通」，而是模拟收银员日常点选，专门抓**静默失败**
（HTTP 200 但结果不对、参数根本没发出、图片拿不到、金额算错、支付方式串码等）。

覆盖：
  A. 订单查询页：存在单号 / 不存在单号 / 日期范围 / 叫号号 / 非法日期
  B. 商品菜单页：分类列表、菜品列表、按分类筛选、每张图片真实可下载
  C. 结算页：改数量后的金额、明细联动、支付方式编码落库
  D. 静默失败静态扫描：request.get(url, payload) 把查询参数当 axios config、模板直出裸 ISO、图片手拼 base

用法：python scripts/e2e_pos_experience.py --password 123456
退出码：0 全通过 / 1 有 FAIL
"""
import argparse
import base64
import hashlib
import hmac
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_JWT_SECRET = (
    "dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security"
)
DEFAULT_ISSUER = "food-traceability-system"
BASE = DEFAULT_BASE
REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BT = chr(96)  # 反引号（模板字符串定界符）


def call(method, path, token=None, body=None, timeout=30, raw=False):
    url = path if path.startswith("http") else BASE + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            if raw:
                return resp.status, resp.headers.get("Content-Type", ""), resp.read(2048)
            text = resp.read().decode("utf-8", "replace")
            try:
                return resp.status, json.loads(text)
            except Exception:
                return resp.status, {"_raw": text[:300]}
    except urllib.error.HTTPError as e:
        text = e.read().decode("utf-8", "replace")
        if raw:
            return e.code, e.headers.get("Content-Type", ""), text[:300]
        try:
            return e.code, json.loads(text)
        except Exception:
            return e.code, {"_raw": text[:300]}
    except Exception as e:
        return -1, {"error": str(e)}


def b64u(b):
    return base64.urlsafe_b64encode(b).rstrip(b"=").decode("ascii")


def mint_jwt(username, roles, permissions, user_id="2080170081205395463"):
    now = int(time.time())
    header = {"alg": "HS512", "typ": "JWT"}
    payload = {
        "userId": user_id, "username": username, "roles": roles, "permissions": permissions,
        "mfaEnabled": False, "mfaVerified": True, "tokenId": "sweep-" + str(now),
        "sub": username, "iss": DEFAULT_ISSUER, "iat": now, "exp": now + 3600,
        "jti": "sweep-" + str(now),
    }
    si = b64u(json.dumps(header, separators=(",", ":")).encode()) + "." + \
         b64u(json.dumps(payload, separators=(",", ":")).encode())
    sig = hmac.new(DEFAULT_JWT_SECRET.encode(), si.encode(), hashlib.sha512).digest()
    return si + "." + b64u(sig)


def ok_code(body):
    return isinstance(body, dict) and body.get("code") in (0, 200)


def payload(body):
    return body.get("data") if isinstance(body, dict) else None


class Sweep:
    def __init__(self, a):
        self.a = a
        self.token = None
        self.results = []
        self.skips = []
        self.findings = []

    def step(self, name, ok, detail, skip=False):
        state = "SKIP" if skip else ("PASS" if ok else "FAIL")
        self.results.append((name, state, detail))
        if skip:
            self.skips.append((name, detail))
        if state == "FAIL":
            self.findings.append((name, detail))
        print("  [%s] %s -> %s" % (state, name, detail))

    def login(self):
        st, body = call("POST", "/v1/pos/auth/login",
                        body={"employeeId": self.a.username, "password": self.a.password})
        data = payload(body)
        tok = None
        if isinstance(data, dict):
            tok = data.get("token") or data.get("accessToken")
        self.token = tok
        self.step("POS 收银员登录", st == 200 and ok_code(body) and bool(tok),
                  "HTTP %s %s" % (st, "token ok" if tok else json.dumps(body, ensure_ascii=False)[:140]))

    def sweep_order_query(self):
        st, body = call("GET", "/v1/pos/orders", token=self.token)
        rows = payload(body) or []
        self.step("订单查询-空条件返回列表", st == 200 and ok_code(body) and isinstance(rows, list),
                  "HTTP %s 返回 %d 条" % (st, len(rows) if isinstance(rows, list) else -1))
        if not rows:
            self.step("订单查询-按存在的单号", False, "库里没有订单，跳过后续条件查询", skip=True)
            return
        sample = rows[0]
        order_no = sample.get("orderNumber") or sample.get("orderCode")
        call_no = sample.get("callNumber")

        st, body = call("GET", "/v1/pos/orders?keyword=" + urllib.parse.quote(str(order_no)), token=self.token)
        hits = payload(body) or []
        matched = any((r.get("orderNumber") or r.get("orderCode")) == order_no for r in hits) if isinstance(hits, list) else False
        self.step("订单查询-按存在的单号命中", st == 200 and matched,
                  "keyword=%s 返回 %d 条 命中=%s" % (order_no, len(hits) if isinstance(hits, list) else -1, matched))

        st, body = call("GET", "/v1/pos/orders?keyword=T00000000000", token=self.token)
        hits = payload(body)
        is_empty_list = isinstance(hits, list) and len(hits) == 0
        self.step("订单查询-不存在的单号返回空列表（不 500/不静默全量）", st == 200 and is_empty_list,
                  "HTTP %s 返回 %s 条" % (st, len(hits) if isinstance(hits, list) else "非列表"))

        from datetime import datetime
        today = datetime.now().strftime("%Y-%m-%d")
        st, body = call("GET", "/v1/pos/orders?startDate=%s&endDate=%s" % (today, today), token=self.token)
        hits = payload(body)
        self.step("订单查询-日期范围（今天）", st == 200 and isinstance(hits, list) and len(hits) > 0,
                  "HTTP %s %s~%s 返回 %d 条" % (st, today, today, len(hits) if isinstance(hits, list) else -1))

        if call_no:
            st, body = call("GET", "/v1/pos/orders?keyword=" + urllib.parse.quote(str(call_no)), token=self.token)
            hits = payload(body) or []
            ok = isinstance(hits, list) and any(r.get("callNumber") == call_no for r in hits)
            self.step("订单查询-按叫号号命中（%s）" % call_no, ok,
                      "返回 %d 条" % (len(hits) if isinstance(hits, list) else -1))
        else:
            self.step("订单查询-按叫号号命中", False, "样本订单无 callNumber，跳过", skip=True)

        st, body = call("GET", "/v1/pos/orders?startDate=2026-13-99", token=self.token)
        self.step("订单查询-非法日期不 500（可报错但要优雅）", st == 200, "HTTP %s code=%s" % (
            st, body.get("code") if isinstance(body, dict) else "?"))

    def sweep_menu(self):
        st, body = call("GET", "/v1/pos/api/categories", token=self.token)
        cats = payload(body) or []
        self.step("菜单-分类列表非空", st == 200 and isinstance(cats, list) and len(cats) > 0,
                  "HTTP %s 分类 %d 个" % (st, len(cats) if isinstance(cats, list) else -1))

        st, body = call("GET", "/v1/pos/api/dishes", token=self.token)
        dishes = payload(body) or []
        self.step("菜单-菜品列表非空", st == 200 and isinstance(dishes, list) and len(dishes) > 0,
                  "HTTP %s 菜品 %d 个" % (st, len(dishes) if isinstance(dishes, list) else -1))

        if isinstance(dishes, list) and dishes:
            no_img = [d for d in dishes if not d.get("imageUrl")]
            names = ", ".join(str(d.get("dishName") or d.get("foodName") or d.get("name")) for d in no_img[:5])
            if not no_img:
                self.step("菜单-每个菜品都有图片字段", True, "全部 %d 个菜品有图" % len(dishes))
            elif len(no_img) == len(dishes):
                self.step("菜单-每个菜品都有图片字段", False, "全部菜品无图（多半是资源同步断了）")
            else:
                self.step("菜单-少量菜品无图（数据缺口，POS 走占位图）", False,
                          "%d/%d 无图：%s" % (len(no_img), len(dishes), names), skip=True)

            origin = BASE.rsplit("/api", 1)[0]
            bad = []
            checked = 0
            for d in dishes[:12]:
                url = d.get("imageUrl") or ""
                if not url:
                    continue
                candidates = []
                if url.startswith("http"):
                    candidates.append(url)
                else:
                    p = url if url.startswith("/api/") else ("/api" + url if url.startswith("/") else "/api/" + url)
                    candidates.append(origin + p)
                checked += 1
                okc = False
                for u in candidates:
                    stc, ctype, _ = call("GET", u, raw=True, timeout=15)
                    if stc == 200 and "image" in (ctype or ""):
                        okc = True
                        break
                if not okc:
                    bad.append("%s(%s)" % (d.get("dishName") or d.get("foodName") or d.get("name"), url))
            self.step("菜单-菜品图片真实可下载（抽查 %d 张）" % checked, len(bad) == 0,
                      "破图 %d 张%s" % (len(bad), "" if not bad else "：" + ", ".join(bad[:5])))

            if cats:
                # 分类筛选：逐类统计（有些分类本来就是空的，不能只看第一个）
                total_in_cats = 0
                non_empty = []
                for c in cats[:12]:
                    cid = c.get("categoryId") or c.get("id")
                    stc, bc = call("GET", "/v1/pos/api/dishes/category/%s" % cid, token=self.token)
                    sub = payload(bc)
                    n = len(sub) if isinstance(sub, list) else 0
                    total_in_cats += n
                    if n:
                        non_empty.append("%s:%d" % (c.get("categoryName"), n))
                all_dishes = len(dishes) if isinstance(dishes, list) else 0
                covered = total_in_cats >= max(1, int(all_dishes * 0.6))
                self.step("菜单-分类筛选覆盖大多数菜品", covered,
                          "分类合计 %d / 全部 %d（非空分类：%s）" % (total_in_cats, all_dishes,
                                                              ", ".join(non_empty[:6]) or "无"))

    def sweep_checkout(self):
        st, body = call("GET", "/v1/pos/api/dishes", token=self.token)
        dishes = payload(body) or []
        if not isinstance(dishes, list) or not dishes:
            self.step("结算-改数量后金额正确", False, "无菜品可用", skip=True)
            return
        dish = dishes[0]
        unit = float(dish.get("price") or dish.get("unitPrice") or 0)
        dish_id = dish.get("dishId") or dish.get("foodId") or dish.get("productId")
        qty = 2
        st, body = call("POST", "/v1/pos/orders/order", token=self.token, body={
            "orderType": 1,
            "storeId": 1,
            # 后端 OrderItemCreateDTO 要求 price/productName（服务端会再按菜品校验，不信任前端价）
            "items": [{"productType": 1, "productId": dish_id, "quantity": qty,
                       "price": unit, "productName": dish.get("dishName") or dish.get("foodName")}],
        })
        data = payload(body)
        order_id = data.get("orderId") if isinstance(data, dict) else None
        total = data.get("totalAmount") or data.get("actualAmount") if isinstance(data, dict) else None
        ok = st == 200 and ok_code(body) and order_id is not None
        self.step("结算-下单（2 份）成功", ok, "HTTP %s orderId=%s total=%s" % (st, order_id, total))
        if not ok:
            return

        expect = round(unit * qty, 2)
        got = round(float(total), 2) if total is not None else None
        self.step("结算-数量×单价=总额（%s×%d=%s）" % (unit, qty, expect), got == expect, "后端 total=%s" % got)

        detail_no = data.get("orderNumber") or data.get("orderCode")
        st, body = call("GET", "/v1/pos/orders/order/%s" % urllib.parse.quote(str(detail_no)), token=self.token)
        d = payload(body) or {}
        items = d.get("items") or d.get("orderItems") or []
        qty_ok = isinstance(items, list) and len(items) >= 1 and int(items[0].get("quantity") or 0) == qty
        self.step("结算-详情明细数量联动", st == 200 and qty_ok,
                  "HTTP %s 明细 %d 条 首条数量=%s" % (st, len(items) if isinstance(items, list) else -1,
                                                 items[0].get("quantity") if items else None))

        st, body = call("POST", "/v1/pos/orders/order/pay", token=self.token,
                        body={"orderId": order_id, "paymentMethod": "wechat", "amount": expect})
        paid = st == 200 and ok_code(body)
        self.step("结算-微信支付成功", paid, "HTTP %s %s" % (st, "" if paid else json.dumps(body, ensure_ascii=False)[:140]))
        if paid:
            st2, b2 = call("GET", "/v1/pos/orders/order/%s" % urllib.parse.quote(str(detail_no)), token=self.token)
            dd = payload(b2) or {}
            pm = dd.get("paymentMethod")
            self.step("结算-支付方式回显为微信", str(pm) in ("2", "wechat", "WECHAT", "微信支付"),
                      "详情 paymentMethod=%s" % pm)

    # ---------- E. 叫号历史闭环（下单 → 历史可见 → 呼叫 → 取餐） ----------
    def sweep_call_history(self):
        st, body = call("GET", "/v1/pos/api/dishes", token=self.token)
        dishes = payload(body) or []
        if not isinstance(dishes, list) or not dishes:
            self.step("叫号-下单生成叫号", False, "无菜品可用", skip=True)
            return
        dish = dishes[0]
        unit = float(dish.get("price") or 0)
        st, body = call("POST", "/v1/pos/orders/order", token=self.token, body={
            "orderType": 1, "storeId": 1,
            "items": [{"productType": 1, "productId": dish.get("dishId"), "quantity": 1,
                       "price": unit, "productName": dish.get("dishName")}],
        })
        data = payload(body) or {}
        order_id = data.get("orderId")
        call_no = data.get("callNumber")
        self.step("叫号-下单即生成叫号号", st == 200 and ok_code(body) and bool(call_no),
                  "orderId=%s callNumber=%s" % (order_id, call_no))
        if not order_id:
            return

        # 拉历史（新接口：分页对象 {records,total}）——按取餐号模糊搜索刚下的单
        st, body = call("GET", "/v1/call-number/history?keyword=%s&range=today&page=1&size=20"
                        % urllib.parse.quote(str(call_no)), token=self.token)
        page = payload(body) or {}
        recs = page.get("records") if isinstance(page, dict) else None
        found = [h for h in (recs or []) if h.get("orderId") == order_id]
        self.step("叫号-历史页按取餐号模糊搜索命中", st == 200 and bool(found),
                  "keyword=%s total=%s 命中=%d" % (call_no, page.get("total") if isinstance(page, dict) else "?",
                                                  len(found)))

        # 分页契约：size 生效且 total 与记录数自洽
        st, body = call("GET", "/v1/call-number/history?range=7d&page=1&size=2", token=self.token)
        pg = payload(body) or {}
        ok_page = (isinstance(pg, dict) and isinstance(pg.get("records"), list)
                   and len(pg.get("records")) <= 2 and int(pg.get("total") or 0) >= len(pg.get("records")))
        self.step("叫号-历史分页(size=2 生效)", st == 200 and ok_page,
                  "records=%s total=%s" % (len(pg.get("records") or []), pg.get("total")))

        # 时间范围筛选
        st, body = call("GET", "/v1/call-number/history?range=30d&page=1&size=1", token=self.token)
        pg30 = payload(body) or {}
        self.step("叫号-时间范围筛选(30d)可用", st == 200 and isinstance(pg30.get("records"), list),
                  "total(30d)=%s" % pg30.get("total"))

        st, body = call("POST", "/v1/call-number/call", token=self.token, body={"orderId": order_id})
        rec = payload(body) or {}
        self.step("叫号-点击呼叫：状态/首次叫号时间/次数",
                  st == 200 and rec.get("status") == "called"
                  and rec.get("callCount") == 1 and bool(rec.get("firstCallTime")),
                  "status=%s callCount=%s firstCallTime=%s" % (
                      rec.get("status"), rec.get("callCount"), rec.get("firstCallTime")))

        st, body = call("POST", "/v1/call-number/recall", token=self.token, body={"orderId": order_id})
        rec = payload(body) or {}
        self.step("叫号-重叫：次数累加到 2", st == 200 and rec.get("callCount") == 2,
                  "callCount=%s lastCallTime=%s" % (rec.get("callCount"), rec.get("lastCallTime")))

        st, body = call("POST", "/v1/call-number/pickup", token=self.token, body={"orderId": order_id})
        rec = payload(body) or {}
        self.step("叫号-取餐：pick_time 与等待秒数落库",
                  st == 200 and rec.get("status") == "picked" and bool(rec.get("pickTime")),
                  "status=%s pickTime=%s waitSeconds=%s" % (
                      rec.get("status"), rec.get("pickTime"), rec.get("waitSeconds")))

        st, body = call("GET", "/v1/call-number/stats", token=self.token)
        stats = payload(body) or {}
        self.step("叫号-统计数字随动作变化（picked≥1）", isinstance(stats, dict) and int(stats.get("picked") or 0) >= 1,
                  "pending=%s called=%s picked=%s" % (
                      stats.get("pending"), stats.get("called"), stats.get("picked")))

        # 取餐后刷新历史（带状态筛选），状态必须已是 picked —— 这就是刚点的「刷新历史页」场景
        st, body = call("GET", "/v1/call-number/history?keyword=%s&range=today&status=picked&page=1&size=20"
                        % urllib.parse.quote(str(call_no)), token=self.token)
        recs = (payload(body) or {}).get("records") or []
        after = [h for h in recs if h.get("orderId") == order_id]
        self.step("叫号-取餐后刷新历史(状态筛选 picked)命中",
                  st == 200 and bool(after),
                  "status=%s pickTime=%s" % (after[0].get("status") if after else None,
                                             after[0].get("pickTime") if after else None))

        # 过号机制：再造一单 → /pass → 历史 status=passed 且留痕 passTime
        st, body = call("POST", "/v1/pos/orders/order", token=self.token, body={
            "orderType": 1, "storeId": 1,
            "items": [{"productType": 1, "productId": dish.get("dishId"), "quantity": 1,
                       "price": unit, "productName": dish.get("dishName")}],
        })
        d2 = payload(body) or {}
        oid2, cn2 = d2.get("orderId"), d2.get("callNumber")
        st, body = call("POST", "/v1/call-number/call", token=self.token, body={"orderId": oid2})
        st, body = call("POST", "/v1/call-number/pass", token=self.token, body={"orderId": oid2})
        rec = payload(body) or {}
        self.step("叫号-过号：状态 passed 且 passTime 留痕",
                  st == 200 and rec.get("status") == "passed" and bool(rec.get("passTime")),
                  "callNumber=%s status=%s passTime=%s" % (cn2, rec.get("status"), rec.get("passTime")))

        st, body = call("GET", "/v1/call-number/history?keyword=%s&range=today&status=passed&page=1&size=10"
                        % urllib.parse.quote(str(cn2)), token=self.token)
        recs = (payload(body) or {}).get("records") or []
        self.step("叫号-过号可在历史中筛出", any(h.get("orderId") == oid2 for h in recs),
                  "keyword=%s 返回 %d 条" % (cn2, len(recs)))

    # ---------- F. 订单历史闭环 ----------
    def sweep_order_history(self):
        from datetime import datetime
        today = datetime.now().strftime("%Y-%m-%d")
        st, body = call("GET", "/v1/pos/orders?startDate=%s&endDate=%s" % (today, today), token=self.token)
        rows = payload(body) or []
        if not isinstance(rows, list) or not rows:
            self.step("订单历史-今日列表非空", False, "今日无订单", skip=True)
            return
        self.step("订单历史-今日列表非空", True, "共 %d 条" % len(rows))
        first = rows[0]
        # 列表字段完整性（历史页要展示的字段不能是 null）
        missing = [k for k in ("orderNumber", "createTime", "status", "totalAmount") if first.get(k) in (None, "")]
        self.step("订单历史-列表关键字段齐全", not missing,
                  "缺失字段：%s（样本 %s）" % (missing or "无", first.get("orderNumber")))
        st, body = call("GET", "/v1/pos/orders/order/%s" % urllib.parse.quote(str(first.get("orderNumber"))),
                        token=self.token)
        d = payload(body) or {}
        self.step("订单历史-详情可回查（与列表一致）",
                  st == 200 and d.get("orderNumber") == first.get("orderNumber"),
                  "detail.orderNumber=%s status=%s" % (d.get("orderNumber"), d.get("status")))

    # ---------- G. 库存变动流水闭环 ----------
    def sweep_inventory_logs(self):
        admin = mint_jwt("sweep-admin", ["admin"], ["*", "inventory:query"])
        st, body = call("GET", "/v1/inventory/logs/page?current=1&size=10", token=admin)
        page = payload(body) or {}
        records = page.get("records") if isinstance(page, dict) else None
        ok = st == 200 and ok_code(body) and isinstance(records, list) and len(records) > 0
        self.step("库存流水-分页查询有数据", ok,
                  "HTTP %s 返回 %s 条" % (st, len(records) if isinstance(records, list) else 0))
        if not ok:
            return
        blank = [r for r in records[:5]
                 if not (r.get("createTime") and r.get("operationType") and r.get("changeAmount") is not None)]
        self.step("库存流水-关键字段（时间/类型/变动量）非空", len(blank) == 0,
                  "前 5 条中空字段 %d 条；样本 %s/%s" % (
                      len(blank), records[0].get("operationType"), records[0].get("changeAmount")))
        # 业务单号不在独立列，落在 remark（如「KDS完成制作扣料 - 订单:T2026...」），据此核对可追溯性
        biz = [r for r in records[:10] if "订单" in str(r.get("remark") or "")]
        self.step("库存流水-扣料流水可追溯到订单号", bool(biz),
                  "前 10 条中带订单号 %d 条；样本：%s" % (
                      len(biz), (biz[0].get("remark") or "")[:48] if biz else "无"))

    # ---------- H. 日结对账历史闭环 ----------
    def sweep_daily_settlement(self):
        admin = mint_jwt("sweep-admin", ["admin"], ["*"])
        st, body = call("GET", "/v1/store-management/settlements?page=1&size=5", token=admin)
        page = payload(body) or {}
        records = page.get("records") if isinstance(page, dict) else None
        ok = st == 200 and ok_code(body) and isinstance(records, list)
        self.step("日结-列表可查询", ok, "HTTP %s 返回 %s 条" % (
            st, len(records) if isinstance(records, list) else 0))
        if not ok:
            self.step("日结-创建今日结账并可回查", False, "列表接口不可用，后续跳过", skip=True)
            return
        from datetime import datetime
        today = datetime.now().strftime("%Y-%m-%d")
        todays = [r for r in records if str(r.get("settlementDate") or r.get("settleDate") or "")[:10] == today]
        if todays:
            row = todays[0]
            self.step("日结-今日记录字段完整", bool(row.get("totalAmount") is not None or row.get("totalRevenue") is not None),
                      "settlementDate=%s total=%s status=%s" % (
                          row.get("settlementDate"), row.get("totalAmount") or row.get("totalRevenue"),
                          row.get("status")))
        else:
            # 主动造数据：先按今日生成日结，再回查（验证「生成 → 历史可见」闭环）
            st2, b2 = call("POST", "/v1/store-management/settlements", token=admin,
                           body={"storeId": "1", "settlementDate": today})
            ok2 = st2 == 200 and ok_code(b2)
            self.step("日结-生成今日日结", ok2, "HTTP %s %s" % (
                st2, "" if ok2 else json.dumps(b2, ensure_ascii=False)[:160]))
            if not ok2:
                return
            st3, b3 = call("GET", "/v1/store-management/settlements?page=1&size=5", token=admin)
            rows3 = (payload(b3) or {}).get("records") or []
            made = [r for r in rows3 if str(r.get("settlementDate") or "")[:10] == today]
            self.step("日结-生成的记录能回查到（含营收数字）", bool(made),
                      "settlementDate=%s totalRevenue=%s status=%s" % (
                          made[0].get("settlementDate"), made[0].get("totalRevenue"),
                          made[0].get("status")) if made else "回查为空")

    def scan_source(self):
        pos_src = os.path.join(REPO, "frontend-pos", "src")
        bad_calls, raw_iso, raw_img = [], [], []
        quotes = "\"'" + BT
        call_re = re.compile(r"request\.(get|delete)\s*\(\s*[" + quotes + r"]([^" + quotes + r"]+)[" + quotes
                             + r"]\s*,\s*([A-Za-z_$][\w$]*)\s*\)")
        for root, _, files in os.walk(pos_src):
            for fn in files:
                if not fn.endswith((".ts", ".vue")):
                    continue
                p = os.path.join(root, fn)
                try:
                    src = open(p, encoding="utf-8", errors="replace").read()
                except Exception:
                    continue
                rel = os.path.relpath(p, REPO)
                for m in call_re.finditer(src):
                    bad_calls.append("%s: %s" % (rel, m.group(0)[:70]))
                if re.search(r"\{\{\s*[\w.]*(createTime|updateTime|payTime)\s*\}\}", src):
                    raw_iso.append(rel)
                # 统一实现在 utils/image.ts 里本来就要读这个变量，只扫「页面/组件里手拼」
                if fn != "image.ts" and rel.replace("\\", "/").find("/utils/") < 0 and "VITE_API_BASE_URL" in src:
                    raw_img.append(rel)
        self.step("扫描-查询参数未包 params 的 request.get", len(bad_calls) == 0,
                  "%d 处%s" % (len(bad_calls), "" if not bad_calls else "：" + "; ".join(bad_calls[:4])))
        self.step("扫描-模板直出裸 ISO 时间字段", len(raw_iso) == 0,
                  "%d 处%s" % (len(raw_iso), "" if not raw_iso else "：" + ", ".join(sorted(set(raw_iso))[:4])))
        self.step("扫描-图片地址仍手拼 VITE_API_BASE_URL", len(raw_img) == 0,
                  "%d 处%s" % (len(raw_img), "" if not raw_img else "：" + ", ".join(sorted(set(raw_img))[:4])))

    def run(self):
        print("=" * 78)
        print("POS 体验巡检（API 级）   base=%s" % BASE)
        print("=" * 78)
        print("\n[A] 订单查询页")
        self.login()
        if not self.token:
            print("\n登录失败，后续全部跳过")
        else:
            self.sweep_order_query()
            print("\n[B] 商品菜单页")
            self.sweep_menu()
            print("\n[C] 结算页")
            self.sweep_checkout()
        print("\n[E] 叫号历史闭环")
        if self.token:
            self.sweep_call_history()
        print("\n[F] 订单历史闭环")
        if self.token:
            self.sweep_order_history()
        print("\n[G] 库存变动流水闭环")
        self.sweep_inventory_logs()
        print("\n[H] 日结对账历史闭环")
        self.sweep_daily_settlement()
        print("\n[D] 静默失败静态扫描")
        self.scan_source()

        fails = [r for r in self.results if r[1] == "FAIL"]
        print("\n" + "=" * 78)
        print("结果: %s   PASS=%d FAIL=%d SKIP=%d" % (
            "巡检通过" if not fails else "发现体验缺陷",
            len([r for r in self.results if r[1] == "PASS"]), len(fails), len(self.skips)))
        for name, state, detail in self.results:
            print("  %-44s %s" % (name, state))
        if fails:
            print("\n发现的问题:")
            for name, detail in self.findings:
                print("  - %s | %s" % (name, detail))
        if self.skips:
            print("\n跳过:")
            for name, detail in self.skips:
                print("  - %s: %s" % (name, detail))
        print("=" * 78)
        return 0 if not fails else 1


def main():
    global BASE
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--username", default="vcashier01")
    p.add_argument("--password", default="123456")
    a = p.parse_args()
    BASE = a.base
    return Sweep(a).run()


if __name__ == "__main__":
    sys.exit(main())
