#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
POS 支付口径「三处一致」E2E（可复跑）

背景：
  同一笔 POS 订单的支付方式，必须在「POS 列表 / POS 详情 / 财务流水详情」三处严格一致。
  此前 orders.payment_method（旧编码 0微信/1支付宝/2现金/3银行卡/4余额）与
  order_payment_records（新编码 1现金/2微信/3支付宝/4银行卡/5余额）两套口径并行，
  且财务详情 posOrder=null，导致同一单出现「现金 / 未记录」的不一致。

断言（现金、微信各跑一遍）：
  A) order_payment_records 该 order_id 恰好 1 条；payment_amount = 订单应付（分）；
     payment_method = 1（现金）/ 2（微信）
  B) 三处支付方式字符串完全相等、非空、不为「未记录」：
       - POS 列表   GET /v1/pos/orders?keyword=<单号>            → 行.paymentMethod
       - POS 详情   GET /v1/pos/orders/order/<单号>              → data.paymentMethod
       - 财务详情   GET /v1/finance/fund-flows/<flowId>          → data.posOrder.paymentMethodName
         （flowId 先由 GET /v1/finance/fund-flows?keyword=<单号> 查，兜底按 orderId 本地过滤）
  C) POS 列表该行 itemCount > 0
  D) 支付方式语义与支付编码一致（现金单不能回显微信，反之亦然）
  财务详情 posOrder 为 null，或支付方式为「未记录 / （系统未记录流水，请查单）」→ FAIL，
  并打印三处原文便于定位。

用法：
  python scripts/e2e_payment_consistency.py --password 123456
  python scripts/e2e_payment_consistency.py --password 123456 --only cash
"""
import argparse
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
BASE = DEFAULT_BASE

# 非法/占位文案：出现即视为「未记录」
PLACEHOLDER_LABELS = (
    "",
    "未记录",
    "未记录支付方式",
    "系统未记录流水，请查单",
    "（系统未记录流水，请查单）",
    "(系统未记录流水，请查单)",
    "undefined",
    "null",
    "None",
)


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


def payload(body):
    """取统一响应里 data 字段（兼容裸对象）"""
    if isinstance(body, dict):
        return body.get("data")
    return None


def text_of(value):
    if value is None:
        return ""
    return str(value).strip()


def rows_of(body):
    """列表接口：data 可能是数组，也可能是 {records: [...]} / {list: [...]}"""
    data = payload(body)
    if isinstance(data, list):
        return data
    if isinstance(data, dict):
        for key in ("records", "list", "rows", "content"):
            got = data.get(key)
            if isinstance(got, list):
                return got
    return []


class Runner:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.token = None
        self.admin_token = None
        self.cases = [("cash", 1, ("现金", "cash"), "现金")]
        if a.only in (None, "", "all", "wechat"):
            self.cases.append(("wechat", 2, ("微信", "wechat"), "微信"))
        if a.only == "cash":
            self.cases = self.cases[:1]

    # ---------- 断言记录 ----------
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
        if out == "":
            return default
        return out

    # ---------- 登录 ----------
    def login_pos(self):
        st, body = call("POST", "/v1/pos/auth/login", body={
            "employeeId": self.a.username, "password": self.a.password,
            "terminalId": self.a.terminal_id})
        self.token = (payload(body) or {}).get("token") if ok_code(body) else None
        return self.step("POS 登录（%s）" % self.a.username, bool(self.token),
                         "HTTP %s token=%s" % (st, ("%d 字符" % len(self.token)) if self.token else "-"))

    def login_admin(self):
        st, body = call("POST", "/v1/auth/login", body={
            "username": self.a.admin_user, "password": self.a.admin_password})
        self.admin_token = (payload(body) or {}).get("token") if ok_code(body) else None
        return self.step("管理端登录（%s，财务详情用）" % self.a.admin_user, bool(self.admin_token),
                         "HTTP %s token=%s %s" % (
                             st, ("%d 字符" % len(self.admin_token)) if self.admin_token else "-",
                             "" if ok_code(body) else json.dumps(body, ensure_ascii=False)[:160]))

    # ---------- 下单 ----------
    def create_order(self, label, method):
        st, body = call("POST", "/v1/pos/orders/order", token=self.token, body={
            "tableNumber": self.a.table_number,
            "items": [{"id": self.a.food_code, "name": self.a.food_name,
                       "quantity": self.a.quantity, "price": self.a.food_price}],
            "totalAmount": round(self.a.food_price * self.a.quantity, 2),
            "paymentMethod": method, "orderType": "dinein",
            "storeId": str(self.a.store_id), "remark": "E2E 支付口径一致性",
            "idempotencyKey": "e2e-paycons-%s-%d" % (method, int(time.time() * 1000))})
        data = payload(body) or {}
        order_id = text_of(data.get("orderId"))
        order_number = text_of(data.get("orderNumber"))
        total = data.get("totalAmount")
        try:
            amount_fen = int(round(float(total) * 100))
        except Exception:
            amount_fen = int(round(self.a.food_price * self.a.quantity * 100))
        ok = ok_code(body) and bool(order_id) and bool(order_number)
        return ok, order_id, order_number, amount_fen, self.step(
            "[%s] POS 下单" % label, ok,
            "HTTP %s orderId=%s orderNumber=%s 应付=%s分 %s" % (
                st, order_id or "-", order_number or "-", amount_fen,
                "" if ok else json.dumps(body, ensure_ascii=False)[:200]))

    def pay(self, label, order_id, method, amount_fen):
        st, body = call("POST", "/v1/pos/orders/order/pay", token=self.token, body={
            "orderId": order_id, "paymentMethod": method,
            "amount": round(amount_fen / 100.0, 2)})
        data = payload(body) or {}
        flow_no = text_of(data.get("fundFlowNo"))
        ok = ok_code(body)
        return ok, flow_no, self.step(
            "[%s] POS 支付" % label, ok,
            "HTTP %s code=%s fundFlowNo=%s %s" % (
                st, (body or {}).get("code"), flow_no or "-",
                "" if ok else json.dumps(body, ensure_ascii=False)[:220]))

    # ---------- A 支付记录 ----------
    def assert_payment_record(self, label, order_id, amount_fen, expect_method):
        sql = ("SELECT COUNT(*), COALESCE(SUM(payment_amount),0), "
               "COALESCE(MIN(payment_method),0), COALESCE(MAX(payment_method),0) "
               "FROM order_payment_records WHERE order_id = '%s' AND deleted = 0"
               % order_id.replace("'", "''"))
        try:
            row = self.scalar(sql, "0|0|0|0")
        except Exception as e:
            return self.step("[%s] A 支付记录唯一且金额/方式正确" % label, False, "psql 失败: %s" % e)
        cnt, amt, mmin, mmax = (row.split("|") + ["0", "0", "0", "0"])[:4]
        ok = (int(cnt) == 1 and int(amt) == amount_fen
              and int(mmin) == expect_method and int(mmax) == expect_method)
        return self.step(
            "[%s] A 支付记录唯一且金额/方式正确" % label, ok,
            "rows=%s amount=%s分(期望%s) payment_method=%s/%s(期望%s)" % (
                cnt, amt, amount_fen, mmin, mmax, expect_method))

    # ---------- 三处取值 ----------
    def pos_list_row(self, order_number):
        st, body = call("GET", "/v1/pos/orders?keyword=%s" % urllib.parse.quote(order_number),
                        token=self.token)
        rows = rows_of(body)
        for row in rows:
            if text_of(row.get("orderNumber")) == order_number:
                return row, "HTTP %s 命中 1 行" % st
        return None, "HTTP %s 未命中（返回 %d 行）" % (st, len(rows))

    def pos_detail(self, order_number):
        st, body = call("GET", "/v1/pos/orders/order/%s" % urllib.parse.quote(order_number),
                        token=self.token)
        return (payload(body) or {}) if ok_code(body) else {}, "HTTP %s code=%s" % (
            st, (body or {}).get("code"))

    def find_fund_flow(self, order_number, order_id):
        notes = []
        st, body = call("GET", "/v1/finance/fund-flows?keyword=%s&size=50" % urllib.parse.quote(order_number),
                        token=self.admin_token)
        rows = rows_of(body)
        notes.append("keyword HTTP %s 返回 %d 行" % (st, len(rows)))
        matched = [r for r in rows if text_of(r.get("orderId")) == order_id]
        if not matched:
            st2, body2 = call("GET", "/v1/finance/fund-flows?page=1&size=100", token=self.admin_token)
            rows2 = rows_of(body2)
            notes.append("全量 HTTP %s 返回 %d 行" % (st2, len(rows2)))
            matched = [r for r in rows2 if text_of(r.get("orderId")) == order_id]
        if not matched:
            return None, "; ".join(notes) + "; 未找到 orderId=%s 的流水" % order_id
        matched.sort(key=lambda r: int(r.get("flowId") or 0), reverse=True)
        return matched[0], "; ".join(notes)

    def fund_detail(self, flow_id):
        st, body = call("GET", "/v1/finance/fund-flows/%s" % flow_id, token=self.admin_token)
        return (payload(body) or {}) if ok_code(body) else {}, "HTTP %s code=%s" % (
            st, (body or {}).get("code"))

    # ---------- B/C/D 三处一致 ----------
    def assert_three_way(self, label, order_number, order_id, expect_tokens):
        row, list_note = self.pos_list_row(order_number)
        list_method = text_of((row or {}).get("paymentMethod"))
        item_count = (row or {}).get("itemCount")

        detail, detail_note = self.pos_detail(order_number)
        detail_method = text_of(detail.get("paymentMethod"))

        flow, flow_note = self.find_fund_flow(order_number, order_id)
        if flow is None:
            return self.step(
                "[%s] B 三处支付方式一致" % label, False,
                "财务流水未找到：%s | POS列表=%r | POS详情=%r | posOrder=未取到" % (
                    flow_note, list_method, detail_method))

        flow_id = flow.get("flowId")
        fdetail, fnote = self.fund_detail(flow_id)
        pos_order = fdetail.get("posOrder")
        fund_method = text_of((pos_order or {}).get("paymentMethodName")) if isinstance(pos_order, dict) else ""

        # B：三处非空、相等、非占位
        values = [list_method, detail_method, fund_method]
        non_empty = all(v not in PLACEHOLDER_LABELS for v in values)
        equal = list_method == detail_method == fund_method
        b_ok = non_empty and equal
        raw = "POS列表=%r | POS详情=%r | 财务详情=%r（flowId=%s, orderId=%s）" % (
            list_method, detail_method, fund_method, flow_id, text_of(flow.get("orderId")))
        b_detail = raw if b_ok else ("%s；posOrder=%s；来源：%s / %s / %s" % (
            raw,
            "null" if pos_order is None else "有值",
            list_note, detail_note, flow_note + " / " + fnote))
        self.step("[%s] B 三处支付方式一致（非空/非未记录/完全相等）" % label, b_ok, b_detail)

        # C：itemCount > 0
        try:
            c_ok = int(item_count) > 0
        except Exception:
            c_ok = False
        self.step("[%s] C POS 列表 itemCount > 0" % label, c_ok,
                  "itemCount=%r（期望 >0）" % (item_count,))

        # D：语义与编码一致（现金单不能回显微信）
        joined = (list_method or "") + "|" + (detail_method or "") + "|" + (fund_method or "")
        lowered = joined.lower()
        d_ok = any(tok.lower() in lowered for tok in expect_tokens)
        self.step("[%s] D 支付方式语义与编码一致" % label, d_ok,
                  "三处合并=%r 期望含 %s" % (joined, "/".join(expect_tokens)))
        return b_ok

    # ---------- 单个用例 ----------
    def run_case(self, method, expect_code, expect_tokens, label):
        print("\n--- 用例：%s支付（payment_method=%d） ---" % (label, expect_code))
        ok, order_id, order_number, amount_fen, _ = self.create_order(label, method)
        if not ok:
            print("  [中断] %s支付单下单失败，跳过本用例" % label)
            return False
        ok, flow_no, _ = self.pay(label, order_id, method, amount_fen)
        if not ok:
            print("  [中断] %s支付失败，跳过本用例" % label)
            return False
        print("  [data] %s单：orderNumber=%s orderId=%s 应付=%s分 流水号=%s" % (
            label, order_number, order_id, amount_fen, flow_no or "-"))
        self.assert_payment_record(label, order_id, amount_fen, expect_code)
        self.assert_three_way(label, order_number, order_id, expect_tokens)
        return True

    def run(self):
        print("=" * 78)
        print("POS 支付口径三处一致 E2E   base=%s  用例=%s" % (
            BASE, "/".join([c[3] for c in self.cases])))
        print("=" * 78)
        if not self.login_pos():
            return self.summary()
        if not self.login_admin():
            return self.summary()
        for method, expect_code, tokens, label in self.cases:
            self.run_case(method, expect_code, tokens, label)
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        print("\n" + "=" * 78)
        print("结果: %s    PASS=%d FAIL=%d" % (
            "全绿" if not fails else "存在失败", len(self.results) - len(fails), len(fails)))
        for name, state, detail in self.results:
            print("  %-52s %s" % (name, state))
        if fails:
            print("-" * 78)
            print("失败详情：")
            for name, state, detail in fails:
                print("  * %s\n    %s" % (name, detail))
        print("=" * 78)
        return 0 if not fails else 1


def main():
    global BASE
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--username", default="vcashier01")
    p.add_argument("--password", required=True)
    p.add_argument("--admin-user", default="qa_admin")
    p.add_argument("--admin-password", default="Admin@123456")
    p.add_argument("--terminal-id", default="E2E-PAYCONS-01")
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--food-code", default="FD260925001")
    p.add_argument("--food-name", default="QA-红烧土豆")
    p.add_argument("--food-price", type=float, default=18.00)
    p.add_argument("--quantity", type=int, default=1)
    p.add_argument("--table-number", type=int, default=1)
    p.add_argument("--only", default="all", help="all/cash/wechat")
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
