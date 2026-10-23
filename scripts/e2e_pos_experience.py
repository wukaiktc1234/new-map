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
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request

DEFAULT_BASE = "http://127.0.0.1:8081/api"
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
