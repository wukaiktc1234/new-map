#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
现结不入应收 E2E（可复跑）

背景（模块 2.1）：
  应收账款 "AR"+order_code 由 OrderCompletedEventListener 在「已完成但未付清」时生成。
  真实链路「后厨先完成、收银后付款」很常见，完成事件触发时 payment_status 还不是 2，
  于是现结单被误生成 ART… 应收，付款后再被核销 —— Owner 截图里的 ART20261023073 就是这么来的。
  修复：OrderCompletedEventListener 对 order_source=4（POS 现结单）一律不生成应收。
  本脚本负责把这条防线钉死。

场景：
  场景 1（现结·先付款）：POS 下单 → 现金支付 → 应收不变、资金流水正常新增
  场景 2（现结·先完成后付款，真实竞态）：POS 下单 → 后厨 start-make → complete-make（此时订单未付款）
        → 断言完成事件不生成应收 → 再现金支付 → 资金流水新增、应收仍为 0

断言：
  1) receivables 总数（all / active）在每一步前后不变
  2) 不存在 receivable_no = 'AR' + orderNumber（all 与 active 都查）
  3) 不存在 remark 里带该订单号的应收（防其它生成路径）
  4) fund_flows 新增 1 条：direction=1(收入)、category=1(销售收款)、amount=订单金额(分)、remark 含订单号
  5) 场景 2 的「完成事件之后、支付之前」应收仍为 0（关键回归点）

用法：
  python scripts/e2e_no_receivable.py --password 123456
  python scripts/e2e_no_receivable.py --password 123456 --skip-kitchen   # 跳过场景 2（无 kitchen:edit 时）
"""
import argparse
import base64
import hashlib
import hmac
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from urllib.parse import quote

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
DEFAULT_JWT_SECRET = (
    "dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security"
)
DEFAULT_ISSUER = "food-traceability-system"
BASE = DEFAULT_BASE
FLOW_DIRECTION_INCOME = 1
FLOW_CATEGORY_SALE = 1


def call(method, path, token=None, body=None, timeout=30):
    url = path if path.startswith("http") else BASE + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8", "replace")
            try:
                return resp.status, json.loads(raw)
            except Exception:
                return resp.status, {"_raw": raw[:300]}
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"_raw": raw[:300]}
    except Exception as e:
        return -1, {"error": str(e)}


def ok_code(body):
    return isinstance(body, dict) and body.get("code") in (0, 200)


def b64u(raw):
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode("ascii")


def mint_admin_token(user_id="1", username="admin"):
    """dev 环境自签 admin token（仅用于后厨接口 kitchen:edit 权限，生产绝不可用）。"""
    now = int(time.time())
    header = {"alg": "HS512", "typ": "JWT"}
    payload = {
        "userId": str(user_id), "username": username, "roles": ["SUPER_ADMIN"], "permissions": ["*"],
        "mfaEnabled": False, "mfaVerified": True, "tokenId": "norec-" + str(now),
        "sub": username, "iss": DEFAULT_ISSUER, "iat": now, "exp": now + 3600, "jti": "norec-" + str(now),
    }
    signing_input = (b64u(json.dumps(header, separators=(",", ":")).encode())
                     + "." + b64u(json.dumps(payload, separators=(",", ":")).encode()))
    sig = hmac.new(DEFAULT_JWT_SECRET.encode("utf-8"), signing_input.encode("ascii"), hashlib.sha512).digest()
    return signing_input + "." + b64u(sig)


class Runner:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.token = None

    def step(self, name, ok, detail):
        state = "PASS" if ok else "FAIL"
        self.results.append((name, state, detail))
        print("  [%s] %s -> %s" % (state, name, detail))
        return ok

    # ---------- DB ----------
    def psql(self, sql):
        try:
            out = subprocess.run(
                [self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port), "-U", self.a.pg_user,
                 "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                capture_output=True, text=True, timeout=30,
                encoding="utf-8", errors="replace",
                env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
        except Exception as e:
            print("    [psql error] %s" % e)
            return []
        if out.returncode != 0:
            print("    [psql rc=%s] %s" % (out.returncode, (out.stderr or "").strip()[:200]))
            return []
        return [line for line in out.stdout.strip().splitlines() if line.strip()]

    def scalar(self, sql, default=-1):
        rows = self.psql(sql)
        if not rows:
            return default
        try:
            return int(rows[0].split("|")[0])
        except Exception:
            return default

    def rec_counts(self):
        return (self.scalar("SELECT count(*) FROM receivables;"),
                self.scalar("SELECT count(*) FROM receivables WHERE deleted = 0;"))

    def ar_for_order(self, order_no):
        """该订单号对应的应收（任意形态：AR+单号 或 remark 里带单号）"""
        safe = order_no.replace("'", "''")
        sql = ("SELECT count(*) FROM receivables WHERE deleted = 0 AND ("
               "receivable_no = 'AR" + safe + "' OR remark LIKE '%" + safe + "%');")
        return self.scalar(sql)

    # ---------- 业务 ----------
    def login_pos(self):
        st, b = call("POST", "/v1/pos/auth/login",
                     body={"employeeId": self.a.user, "password": self.a.password,
                           "terminalId": "norec-" + str(int(time.time()))})
        if st != 200 or not ok_code(b):
            return False, "POS 登录失败: HTTP %s %s" % (st, str(b)[:200])
        self.token = b["data"]["token"]
        return True, "POS 登录成功"

    def pick_dish(self):
        st, b = call("GET", "/v1/pos/api/dishes", token=self.token)
        if st != 200 or not ok_code(b):
            return None
        dishes = b.get("data") or []
        for d in dishes:
            if d.get("dishId") and (d.get("price") or 0) > 0:
                return d
        return None

    def create_order(self, dish, tag):
        item = {"productId": str(dish["dishId"]), "productName": dish.get("dishName") or "",
                "quantity": 1, "price": float(dish.get("price") or 0),
                "dishType": dish.get("dishType") or "single"}
        body = {"orderType": "dinein", "items": [item],
                "idempotencyKey": "norec-%s-%d" % (tag, int(time.time() * 1000))}
        st, b = call("POST", "/v1/pos/orders/order", token=self.token, body=body)
        if st != 200 or not ok_code(b):
            return None, "下单失败: HTTP %s %s" % (st, str(b)[:200])
        return b.get("data") or {}, ""

    def pay_order(self, order_id, amount_yuan):
        st, b = call("POST", "/v1/pos/orders/order/pay", token=self.token,
                     body={"orderId": order_id, "paymentMethod": "现金", "amount": amount_yuan})
        if st != 200 or not ok_code(b):
            return None, "支付失败: HTTP %s %s" % (st, str(b)[:200])
        return b.get("data") or {}, ""

    def fund_flow_count(self, order_no):
        safe = order_no.replace("'", "''")
        return self.scalar("SELECT count(*) FROM fund_flows WHERE deleted = 0 AND remark LIKE '%" + safe + "%';")

    def fund_flow_row(self, order_no):
        safe = order_no.replace("'", "''")
        rows = self.psql("SELECT flow_direction, flow_category, amount, flow_no FROM fund_flows "
                         "WHERE deleted = 0 AND remark LIKE '%" + safe + "%' ORDER BY flow_id DESC LIMIT 1;")
        return rows[0] if rows else ""

    def order_status(self, order_id):
        """返回 (order_status, payment_status)，查不到返回 (-1, -1)。"""
        rows = self.psql("SELECT order_status, payment_status FROM orders WHERE order_id = '" +
                         str(order_id).replace("'", "''") + "' LIMIT 1;")
        if not rows:
            return (-1, -1)
        parts = rows[0].split("|")
        try:
            return (int(parts[0]), int(parts[1]))
        except Exception:
            return (-1, -1)

    def kitchen_order_id(self, order_id):
        rows = self.psql("SELECT kitchen_order_id FROM kitchen_order WHERE order_id = '" +
                         order_id.replace("'", "''") + "' ORDER BY create_time DESC LIMIT 1;")
        return rows[0].split("|")[0] if rows else ""


def main():
    p = argparse.ArgumentParser(description="现结不入应收 E2E")
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--user", default="vcashier01")
    p.add_argument("--password", required=True)
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", default=5432, type=int)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    p.add_argument("--skip-kitchen", action="store_true", help="跳过「先完成后付款」竞态场景")
    p.add_argument("--chef-id", type=int, default=2080155339590037512, help="后厨接单厨师ID（需真实员工）")
    p.add_argument("--chef-name", default="员工J")
    a = p.parse_args()

    global BASE
    BASE = a.base
    r = Runner(a)
    admin_token = mint_admin_token()

    print("=" * 74)
    print("现结不入应收 E2E  base=%s  user=%s" % (a.base, a.user))
    print("=" * 74)

    ok, detail = r.login_pos()
    if not r.step("S0.1 POS 登录", ok, detail):
        return 1

    dish = r.pick_dish()
    if not r.step("S0.2 取菜品", dish is not None, "菜品=%s 单价=%s" %
                  ((dish or {}).get("dishName"), (dish or {}).get("price"))):
        return 1

    base_all, base_active = r.rec_counts()
    r.step("S0.3 基线", base_all >= 0,
           "receivables all=%s active=%s" % (base_all, base_active))

    # ---------------- 场景 1：先付款 ----------------
    print("\n[场景1] 现结·先付款")
    order, err = r.create_order(dish, "s1")
    if not r.step("S1.1 下单", order is not None, err or ("订单=%s 金额=%s" %
                  ((order or {}).get("orderNumber"), (order or {}).get("totalAmount")))):
        return 1
    no1 = order.get("orderNumber")
    amt1 = float(order.get("totalAmount") or 0)

    before_flow = r.fund_flow_count(no1)
    paid, err = r.pay_order(order.get("orderId"), amt1)
    r.step("S1.2 现金支付", paid is not None,
           err or ("code=ok fundFlowNo=%s" % (paid or {}).get("fundFlowNo")))

    after_all, after_active = r.rec_counts()
    r.step("S1.3 应收未新增", (after_all == base_all and after_active == base_active),
           "支付前 all=%s active=%s → 支付后 all=%s active=%s" %
           (base_all, base_active, after_all, after_active))

    ar1 = r.ar_for_order(no1)
    r.step("S1.4 该单无应收", ar1 == 0, "订单 %s 命中应收 %s 条（应为 0）" % (no1, ar1))

    flow1 = r.fund_flow_row(no1)
    flow_ok = False
    if flow1:
        direction, category, amount, flow_no = flow1.split("|")
        flow_ok = (int(direction) == FLOW_DIRECTION_INCOME and int(category) == FLOW_CATEGORY_SALE
                   and int(amount) == int(round(amt1 * 100)))
    r.step("S1.5 资金流水正常入账", flow_ok and r.fund_flow_count(no1) == before_flow + 1,
           "流水=%s 期望(收入/销售/%s分)" % (flow1 or "(无)", int(round(amt1 * 100))))

    # ---------------- 场景 2：先完成后付款（真实竞态） ----------------
    if a.skip_kitchen:
        r.step("S2.0 后厨竞态场景", True, "已按参数跳过 --skip-kitchen")
    else:
        print("\n[场景2] 现结·先完成后付款（竞态回归）")
        order2, err = r.create_order(dish, "s2")
        if not r.step("S2.1 下单", order2 is not None, err or ("订单=%s" %
                      (order2 or {}).get("orderNumber"))):
            return 1
        no2 = order2.get("orderNumber")
        amt2 = float(order2.get("totalAmount") or 0)

        kid = r.kitchen_order_id(str(order2.get("orderId")))

        def kitchen_post(op, query=""):
            st, b = call("POST", "/v1/kitchen/orders/%s/%s%s" % (kid, op, query), token=admin_token)
            return st == 200 and ok_code(b) and b.get("data") is True, "HTTP %s %s" % (st, str(b)[:160])

        receive_ok, receive_msg = (False, "未找到 kitchenOrderId")
        if kid:
            receive_ok, receive_msg = kitchen_post(
                "receive", "?chefId=%s&chefName=%s" % (a.chef_id, quote(a.chef_name)))
        r.step("S2.2 后厨接单", receive_ok, "kitchenOrderId=%s %s" % (kid or "(未找到)",
               "" if receive_ok else receive_msg))

        start_ok, start_msg = (False, "接单未成功")
        if receive_ok:
            start_ok, start_msg = kitchen_post("start-make")
        r.step("S2.3 后厨开始制作", start_ok, "" if start_ok else start_msg)

        complete_ok, complete_msg = (False, "开始制作未成功")
        if start_ok:
            complete_ok, complete_msg = kitchen_post("complete-make")
            # complete-make 走 OrderLifecycleService：主订单完成 + 发布 OrderCompletedEvent
            complete_ok = complete_ok and r.order_status(str(order2.get("orderId")))[0] == 2
        r.step("S2.4 后厨完成制作（此时未付款）", complete_ok,
               ("订单 %s 已完成，payment_status 仍应为 0/1" % no2) if complete_ok else complete_msg)

        time.sleep(2.0)  # 完成事件是 @Async AFTER_COMMIT
        mid_all, mid_active = r.rec_counts()
        ar2_mid = r.ar_for_order(no2)
        r.step("S2.5 完成事件未生成应收（关键）",
               mid_all == base_all and mid_active == base_active and ar2_mid == 0,
               "完成事件后 all=%s active=%s，该单应收=%s（应全为基线/0）" %
               (mid_all, mid_active, ar2_mid))

        before_flow2 = r.fund_flow_count(no2)
        paid2, err = r.pay_order(order2.get("orderId"), amt2)
        r.step("S2.6 现金支付", paid2 is not None,
               err or ("fundFlowNo=%s" % (paid2 or {}).get("fundFlowNo")))

        end_all, end_active = r.rec_counts()
        ar2_end = r.ar_for_order(no2)
        r.step("S2.7 支付后应收仍为 0", (end_all == base_all and end_active == base_active and ar2_end == 0),
               "支付后 all=%s active=%s，该单应收=%s" % (end_all, end_active, ar2_end))

        flow2 = r.fund_flow_row(no2)
        flow2_ok = False
        if flow2:
            d2, c2, a2, _f2 = flow2.split("|")
            flow2_ok = (int(d2) == FLOW_DIRECTION_INCOME and int(c2) == FLOW_CATEGORY_SALE
                        and int(a2) == int(round(amt2 * 100)))
        r.step("S2.8 资金流水正常入账", flow2_ok and r.fund_flow_count(no2) == before_flow2 + 1,
               "流水=%s" % (flow2 or "(无)"))

    # ---------------- 汇总 ----------------
    print("\n" + "=" * 74)
    fails = [x for x in r.results if x[1] == "FAIL"]
    for name, state, detail in r.results:
        print("  %-4s %s" % (state, name))
    print("-" * 74)
    print("VERDICT: %s   (PASS %d / FAIL %d)" %
          ("PASS" if not fails else "FAIL", len(r.results) - len(fails), len(fails)))
    print("=" * 74)
    return 0 if not fails else 1


if __name__ == "__main__":
    sys.exit(main())
