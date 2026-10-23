#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
POS 支付 → 资金流水入账 E2E（可复跑）

校验链路：下单一笔（orders 新表） → POST /v1/pos/orders/order/pay → 资金流水入账 → 重复支付不重复入账

断言：
  1) 支付成功（code=0），返回 fundFlowNo 非空
  2) fund_flows 新增 1 条：flow_direction=1（收入）、flow_category=1（销售收款/SALE）、
     amount = 订单金额（分）、remark 含订单号、flow_no 形如 FFyyyyMMddNNNN
  3) 账户侧：该流水 account_id 对应的 bank_accounts.balance == flow.balance_after；
     全部启用账户余额合计增加 == 流水金额（证明 create() 的记账副作用生效）
  4) order_payment_records 该订单 payment_amount 合计 == 流水金额、payment_method=1（现金）
  5) 重复支付：接口返回失败（订单已支付），fund_flows 中该订单 SALE 流水仍只有 1 条

用法：
  python scripts/e2e_pay_to_fund.py --password 123456
  python scripts/e2e_pay_to_fund.py --food-price 18 --quantity 1 --store-id 1
"""
import argparse
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
BASE = DEFAULT_BASE

# 与 PosSaleFinanceRecorder 保持一致的枚举
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


class Runner:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.token = None
        self.order_id = None
        self.order_number = None
        self.amount_fen = int(round(a.food_price * a.quantity * 100))

    def step(self, name, ok, detail):
        state = "PASS" if ok else "FAIL"
        self.results.append((name, state, detail))
        print("  [%s] %s -> %s" % (state, name, detail))
        return ok

    # ---------- psql ----------
    def psql(self, sql):
        try:
            out = subprocess.run(
                [self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port), "-U", self.a.pg_user,
                 "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                capture_output=True, text=True, timeout=25,
                encoding="utf-8", errors="replace",
                env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
            return out.returncode, (out.stdout or "").strip(), (out.stderr or "").strip()
        except Exception as e:
            return -1, "", str(e)

    def scalar(self, sql, default=None):
        rc, out, err = self.psql(sql)
        if rc != 0:
            raise RuntimeError("psql rc=%s %s" % (rc, (err or out)[:200]))
        if out == "" or out is None:
            return default
        return out

    def sum_active_balance(self):
        val = self.scalar("SELECT COALESCE(SUM(balance),0) FROM bank_accounts WHERE status = 1 AND deleted = 0", "0")
        return int(val)

    def sale_flow_count(self, order_id):
        # 2026-10-24 起幂等键从 remark 技术串迁到 fund_flows.order_id（备注已清洗为人类可读文案）
        val = self.scalar(
            "SELECT COUNT(*) FROM fund_flows WHERE flow_direction = %d AND flow_category = %d "
            "AND order_id = '%s' AND deleted = 0" % (
                FLOW_DIRECTION_INCOME, FLOW_CATEGORY_SALE, order_id.replace("'", "''")), "0")
        return int(val)

    # ---------- 1. 登录 ----------
    def login(self):
        st, body = call("POST", "/v1/pos/auth/login", body={
            "employeeId": self.a.username, "password": self.a.password, "terminalId": self.a.terminal_id})
        self.token = ((body.get("data") or {}) if ok_code(body) else {}).get("token")
        return self.step("POS 登录", bool(self.token), "HTTP %s token=%s" % (
            st, ("%d 字符" % len(self.token)) if self.token else "-"))

    # ---------- 2. 下单 ----------
    def create_order(self):
        st, body = call("POST", "/v1/pos/orders/order", token=self.token, body={
            "tableNumber": self.a.table_number,
            "items": [{"id": self.a.food_code, "name": self.a.food_name,
                       "quantity": self.a.quantity, "price": self.a.food_price}],
            "totalAmount": round(self.a.food_price * self.a.quantity, 2),
            "paymentMethod": "cash", "orderType": "dinein",
            "storeId": str(self.a.store_id), "remark": "E2E 支付入账",
            "idempotencyKey": "e2e-fund-%d" % int(time.time() * 1000)})
        data = (body.get("data") or {}) if ok_code(body) else {}
        self.order_id = str(data.get("orderId") or "")
        self.order_number = str(data.get("orderNumber") or "")
        return self.step("POS 下单", ok_code(body) and bool(self.order_id),
                         "HTTP %s orderId=%s orderNumber=%s amount=%s分" % (
                             st, self.order_id or "-", self.order_number or "-", self.amount_fen))

    # ---------- 3. 支付前基线 ----------
    def baseline(self):
        try:
            self.balance_before = self.sum_active_balance()
            self.flow_before = self.sale_flow_count(self.order_id) if self.order_id else -1
        except Exception as e:
            return self.step("支付前基线", False, "psql 失败: %s" % e)
        return self.step("支付前基线（余额/该订单 SALE 流水）", self.flow_before == 0,
                         "balanceSum=%s orderFlowCount=%s" % (self.balance_before, self.flow_before))

    # ---------- 4. 支付 ----------
    def pay(self):
        st, body = call("POST", "/v1/pos/orders/order/pay", token=self.token, body={
            "orderId": self.order_id, "paymentMethod": "cash",
            "amount": round(self.a.food_price * self.a.quantity, 2)})
        data = (body.get("data") or {}) if ok_code(body) else {}
        self.fund_flow_no = data.get("fundFlowNo")
        return self.step("POS 支付（现金）", ok_code(body) and bool(self.fund_flow_no),
                         "HTTP %s code=%s fundFlowNo=%s %s" % (
                             st, (body or {}).get("code"), self.fund_flow_no or "-",
                             "" if ok_code(body) else json.dumps(body, ensure_ascii=False)[:200]))

    # ---------- 5. 资金流水断言 ----------
    def verify_fund_flow(self):
        # 按 order_id 精确定位本订单流水（备注已清洗，不再含 [orderId=...]）
        sql = ("SELECT flow_id, flow_no, account_id, flow_direction, flow_category, amount, balance_after, remark "
               "FROM fund_flows WHERE flow_direction = %d AND flow_category = %d "
               "AND order_id = '%s' AND deleted = 0"
               % (FLOW_DIRECTION_INCOME, FLOW_CATEGORY_SALE, self.order_id.replace("'", "''")))
        try:
            rc, out, err = self.psql(sql)
        except Exception as e:
            return self.step("fund_flows 新增 SALE 收入流水", False, "psql 失败: %s" % e)
        if rc != 0:
            return self.step("fund_flows 新增 SALE 收入流水", False, "psql rc=%s %s" % (rc, err[:160]))
        rows = [line.split("|") for line in out.splitlines() if line.strip()]
        if len(rows) != 1:
            return self.step("fund_flows 新增 SALE 收入流水", False, "命中 %d 条（期望 1）" % len(rows))

        flow_id, flow_no, account_id, direction, category, amount, balance_after, remark = (rows[0] + [""] * 8)[:8]
        self.account_id = account_id
        ok_amount = int(amount or 0) == self.amount_fen
        ok_remark = self.order_number in (remark or "")
        ok_no = (flow_no or "").startswith("FF") and len(flow_no) >= 12
        ok_dir = int(direction or 0) == FLOW_DIRECTION_INCOME and int(category or 0) == FLOW_CATEGORY_SALE
        detail = "flowNo=%s account=%s amount=%s分(期望%s) remark=%s" % (
            flow_no, account_id, amount, self.amount_fen, remark)
        ok = ok_amount and ok_remark and ok_no and ok_dir
        self.fund_flow_no = self.fund_flow_no or flow_no
        return self.step("fund_flows 新增 1 条 SALE 收入（金额/备注/流水号）", ok, detail)

    def verify_account_balance(self):
        try:
            acc_balance = self.scalar(
                "SELECT COALESCE(balance,0) FROM bank_accounts WHERE account_id = %s" % self.account_id, "0")
            balance_after = self.scalar(
                "SELECT COALESCE(balance_after,0) FROM fund_flows WHERE flow_no = '%s'" %
                str(self.fund_flow_no).replace("'", "''"), "0")
            balance_now = self.sum_active_balance()
        except Exception as e:
            return self.step("资金账户余额同步", False, "psql 失败: %s" % e)
        delta = balance_now - self.balance_before
        ok = int(acc_balance) == int(balance_after) and delta == self.amount_fen
        return self.step("资金账户余额同步（balance_after / 余额增量）", ok,
                         "account.balance=%s flow.balance_after=%s 余额增量=%s(期望%s)" % (
                             acc_balance, balance_after, delta, self.amount_fen))

    def verify_payment_record(self):
        try:
            row = self.scalar(
                "SELECT COUNT(*), COALESCE(SUM(payment_amount),0), COALESCE(MIN(payment_method),0) "
                "FROM order_payment_records WHERE order_id = '%s' AND deleted = 0" %
                self.order_id.replace("'", "''"), "0|0|0")
        except Exception as e:
            return self.step("order_payment_records 与流水金额一致", False, "psql 失败: %s" % e)
        cnt, amt, method = (row.split("|") + ["0", "0", "0"])[:3]
        ok = int(cnt) == 1 and int(amt) == self.amount_fen and int(method) == 1
        return self.step("order_payment_records 与流水金额一致", ok,
                         "rows=%s amount=%s分 method=%s(期望1现金)" % (cnt, amt, method))

    # ---------- 6. 幂等：重复支付不重复入账 ----------
    def verify_idempotent(self):
        before = self.sale_flow_count(self.order_id)
        st, body = call("POST", "/v1/pos/orders/order/pay", token=self.token, body={
            "orderId": self.order_id, "paymentMethod": "cash",
            "amount": round(self.a.food_price * self.a.quantity, 2)})
        rejected = not ok_code(body)
        after = self.sale_flow_count(self.order_id)
        ok = rejected and before == 1 and after == 1
        return self.step("重复支付不重复入账", ok,
                         "二次支付 HTTP %s code=%s msg=%s；SALE 流水 %s→%s" % (
                             st, (body or {}).get("code"), (body or {}).get("message"), before, after))

    def run(self):
        print("=" * 78)
        print("POS 支付 → 资金流水入账 E2E   base=%s 金额=%s分" % (BASE, self.amount_fen))
        print("=" * 78)
        steps = (self.login, self.create_order, self.baseline, self.pay,
                 self.verify_fund_flow, self.verify_account_balance, self.verify_payment_record,
                 self.verify_idempotent)
        for fn in steps:
            if not fn():
                print("\n[中断] 关键步骤失败：%s" % fn.__name__)
                return self.summary()
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        print("\n" + "=" * 78)
        print("结果: %s    PASS=%d FAIL=%d" % (
            "全绿" if not fails else "存在失败", len(self.results) - len(fails), len(fails)))
        for name, state, detail in self.results:
            print("  %-46s %s" % (name, state))
        print("=" * 78)
        return 0 if not fails else 1


def main():
    global BASE
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--username", default="vcashier01")
    p.add_argument("--password", required=True)
    p.add_argument("--terminal-id", default="E2E-FUND-01")
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--food-code", default="FD260925001")
    p.add_argument("--food-name", default="QA-红烧土豆")
    p.add_argument("--food-price", type=float, default=18.00)
    p.add_argument("--quantity", type=int, default=1)
    p.add_argument("--table-number", type=int, default=1)
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    BASE = a.base
    return Runner(a).run()


if __name__ == "__main__":
    sys.exit(main())
