#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
模块 1 财务地基 E2E（可复跑）：多门店隔离 / 并发安全 / 审计合规

覆盖断言：
  1.1 多门店隔离
      - 为门店 A、B 各下一单（未支付）→ 执行 scripts/db-fix/2026-10-23-seed-store-cash-accounts.sql
      - 两门店各自存在**三件套**收款账户：现金(account_type=2) / 微信(1) / 支付宝(1)
      - 分别支付 → fund_flows.account_id 必须属于**各自门店**（禁止跨店/全局回退）
  1.2 并发安全
      - 门店 A 并发 10 笔「下单+支付」
      - 断言：10 条 SALE 流水、每笔金额正确、门店 A 现金账户余额增量 == 10×单笔金额
  1.3 审计合规
      - 本轮新增流水 create_user_id 必须等于登录收银员 userId（>0）
      - 全库 fund_flows.create_user_id 不允许为 null（DDL 已 SET NOT NULL）
  附加渠道路由断言（seed 三件套之后）
      - 门店 B 用「微信支付」→ 必须入账：恰好 1 条流水，且 account_id == 门店 B 的微信商户账户
      - 门店 A 用「支付宝」→ 必须入账：恰好 1 条流水，且 account_id == 门店 A 的支付宝账户
      - 两者都不得落到别店账户（验证「按门店 + 渠道」双维度路由）

用法：
  python scripts/e2e_finance_foundation.py --password 123456
"""
import argparse
import json
import os
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from decimal import Decimal

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
DEFAULT_SEED_SQL = r"P:\my-new-project\scripts\db-fix\2026-10-23-seed-store-cash-accounts.sql"
BASE = DEFAULT_BASE

FLOW_DIRECTION_INCOME = 1
FLOW_CATEGORY_SALE = 1


def call(method, path, token=None, body=None, timeout=60):
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
        self.user_id = None
        self.amount_fen = int(round(a.food_price * a.quantity * 100))
        self.new_order_ids = []

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
                capture_output=True, text=True, timeout=40, encoding="utf-8", errors="replace",
                env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
            return out.returncode, (out.stdout or "").strip(), (out.stderr or "").strip()
        except Exception as e:
            return -1, "", str(e)

    def scalar(self, sql, default=None):
        rc, out, err = self.psql(sql)
        if rc != 0:
            raise RuntimeError("psql rc=%s %s" % (rc, (err or out)[:200]))
        return default if out == "" else out

    def rows(self, sql):
        rc, out, err = self.psql(sql)
        if rc != 0:
            raise RuntimeError("psql rc=%s %s" % (rc, (err or out)[:200]))
        return [line.split("|") for line in out.splitlines() if line.strip()]

    def run_sql_file(self, path):
        try:
            out = subprocess.run(
                [self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port), "-U", self.a.pg_user,
                 "-d", self.a.pg_db, "-v", "ON_ERROR_STOP=1", "-f", path],
                capture_output=True, text=True, timeout=60, encoding="utf-8", errors="replace",
                env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
            return out.returncode == 0, (out.stdout or "").strip()[:300] + (out.stderr or "").strip()[:300]
        except Exception as e:
            return False, str(e)

    def store_cash_accounts(self, store_id):
        rows = self.rows("SELECT account_id, account_name, account_type FROM bank_accounts "
                         "WHERE store_id = %d AND status = 1 AND deleted = 0 AND account_type = 2 "
                         "ORDER BY account_id" % store_id)
        return rows

    def store_channel_accounts(self, store_id):
        """三件套：返回 (cash_rows, wechat_rows, alipay_rows)，按账号列前缀识别（ASCII，规避客户端编码问题）"""
        def by_prefix(prefix):
            return self.rows(
                "SELECT account_id, account_name, account_type FROM bank_accounts "
                "WHERE store_id = %d AND status = 1 AND deleted = 0 AND account_number LIKE '%s-STORE-%%' "
                "ORDER BY account_id" % (store_id, prefix))
        return by_prefix("CASH"), by_prefix("WECHAT"), by_prefix("ALIPAY")

    def channel_account_id(self, store_id, prefix):
        rows = self.rows(
            "SELECT account_id FROM bank_accounts "
            "WHERE store_id = %d AND status = 1 AND deleted = 0 AND account_number LIKE '%s-STORE-%%' "
            "ORDER BY account_id LIMIT 1" % (store_id, prefix))
        return int(rows[0][0]) if rows else None

    def store_balance_sum(self, store_id):
        return int(self.scalar("SELECT COALESCE(SUM(balance),0) FROM bank_accounts "
                               "WHERE store_id = %d AND status = 1 AND deleted = 0" % store_id, "0"))

    def order_flow(self, order_id):
        """返回该订单的 SALE 流水行：flow_no|account_id|amount|create_user_id|account_store_id"""
        rows = self.rows(
            "SELECT f.flow_no, f.account_id, f.amount, f.create_user_id, COALESCE(a.store_id, -1) "
            "FROM fund_flows f LEFT JOIN bank_accounts a ON a.account_id = f.account_id "
            "WHERE f.flow_direction = %d AND f.flow_category = %d AND f.deleted = 0 "
            "AND f.order_id = '%s' ORDER BY f.flow_id" % (
                FLOW_DIRECTION_INCOME, FLOW_CATEGORY_SALE, order_id.replace("'", "''")))
        return rows

    def flow_count_total(self):
        return int(self.scalar("SELECT COUNT(*) FROM fund_flows WHERE flow_direction = %d AND flow_category = %d "
                               "AND deleted = 0" % (FLOW_DIRECTION_INCOME, FLOW_CATEGORY_SALE), "0"))

    # ---------- HTTP ----------
    def login(self):
        st, body = call("POST", "/v1/pos/auth/login", body={
            "employeeId": self.a.username, "password": self.a.password, "terminalId": self.a.terminal_id})
        data = body.get("data") or {}
        self.token = data.get("token")
        user_info = data.get("userInfo") or {}
        uid = user_info.get("userId") or user_info.get("id")
        try:
            self.user_id = int(uid) if uid is not None else None
        except (TypeError, ValueError):
            self.user_id = None
        return self.step("POS 登录", bool(self.token) and bool(self.user_id),
                         "HTTP %s token=%s userId=%s" % (st, "ok" if self.token else "-", self.user_id))

    def create_order(self, store_id, order_type="dinein"):
        st, body = call("POST", "/v1/pos/orders/order", token=self.token, body={
            "tableNumber": self.a.table_number,
            "items": [{"id": self.a.food_code, "name": self.a.food_name,
                       "quantity": self.a.quantity, "price": self.a.food_price}],
            "totalAmount": round(self.a.food_price * self.a.quantity, 2),
            "paymentMethod": "cash", "orderType": order_type,
            "storeId": str(store_id), "remark": "E2E 财务地基",
            "idempotencyKey": "e2e-fin-%s-%d" % (store_id, int(time.time() * 1000000))})
        data = (body.get("data") or {}) if ok_code(body) else {}
        oid = data.get("orderId")
        if oid:
            self.new_order_ids.append(oid)
        return oid, data.get("orderNumber"), st, body

    def pay(self, order_id, method="现金"):
        st, body = call("POST", "/v1/pos/orders/order/pay", token=self.token, body={
            "orderId": order_id, "paymentMethod": method,
            "amount": round(self.a.food_price * self.a.quantity, 2)})
        return st, body

    # ---------- 1.1 门店隔离 ----------
    def check_store_isolation(self):
        # 先给两个门店各下一单（此时 seed 会按 orders.store_id 补建账户）
        order_a, num_a, st_a, body_a = self.create_order(self.a.store_a)
        order_b, num_b, st_b, body_b = self.create_order(self.a.store_b)
        if not (order_a and order_b):
            return self.step("门店 A/B 下单", False,
                             "A: HTTP %s %s | B: HTTP %s %s" % (st_a, json.dumps(body_a, ensure_ascii=False)[:120],
                                                                st_b, json.dumps(body_b, ensure_ascii=False)[:120]))
        self.step("门店 A/B 下单", True, "A=%s(%s) B=%s(%s)" % (num_a, order_a, num_b, order_b))

        ok_sql, out = self.run_sql_file(self.a.seed_sql)
        triples = {}
        accounts_ok = ok_sql
        for sid in (self.a.store_a, self.a.store_b):
            cash, wechat, alipay = self.store_channel_accounts(sid)
            triples[sid] = (cash, wechat, alipay)
            if not (cash and wechat and alipay):
                accounts_ok = False
        self.step("seed 脚本补建门店三件套（现金/微信/支付宝）", accounts_ok,
                  "sql_ok=%s " % ok_sql + " ".join(
                      "store%d[cash=%s wechat=%s alipay=%s]" % (
                          sid,
                          [r[0] for r in triples[sid][0]],
                          [r[0] for r in triples[sid][1]],
                          [r[0] for r in triples[sid][2]])
                      for sid in (self.a.store_a, self.a.store_b)))
        if not accounts_ok:
            return False

        st_a, body_a2 = self.pay(order_a)
        st_b, body_b2 = self.pay(order_b)
        pay_ok = ok_code(body_a2) and ok_code(body_b2)
        flow_a = self.order_flow(order_a)
        flow_b = self.order_flow(order_b)
        # 每笔都必须恰好 1 条，且 account 的 store_id == 订单门店
        ok = (pay_ok and len(flow_a) == 1 and len(flow_b) == 1
              and int(flow_a[0][4]) == self.a.store_a
              and int(flow_b[0][4]) == self.a.store_b
              and int(flow_a[0][2]) == self.amount_fen
              and int(flow_b[0][2]) == self.amount_fen)
        return self.step("门店隔离：流水账户归属各自门店", ok,
                         "A: flow=%s account=%s store=%s | B: flow=%s account=%s store=%s" % (
                             flow_a[0][0] if flow_a else "-", flow_a[0][1] if flow_a else "-",
                             flow_a[0][4] if flow_a else "-",
                             flow_b[0][0] if flow_b else "-", flow_b[0][1] if flow_b else "-",
                             flow_b[0][4] if flow_b else "-"))

    # ---------- 1.2 并发 ----------
    def check_concurrency(self):
        n = self.a.parallel
        before_balance = self.store_balance_sum(self.a.store_a)
        before_flows = self.flow_count_total()

        results = []
        lock = threading.Lock()

        def worker(idx):
            oid, num, st_c, b_c = self.create_order(self.a.store_a)
            if not oid:
                with lock:
                    results.append((None, None, "create_failed:%s" % (b_c.get("message") if isinstance(b_c, dict) else b_c)))
                return
            st_p, b_p = self.pay(oid)
            with lock:
                results.append((oid, (b_p.get("data") or {}).get("fundFlowNo"), st_p))

        threads = [threading.Thread(target=worker, args=(i,)) for i in range(n)]
        started = time.time()
        for t in threads:
            t.start()
        for t in threads:
            t.join()
        cost = time.time() - started

        oids = [r[0] for r in results if r[0]]
        flow_rows = []
        for oid in oids:
            flow_rows.extend(self.order_flow(oid))
        after_balance = self.store_balance_sum(self.a.store_a)
        after_flows = self.flow_count_total()

        delta = after_balance - before_balance
        expected_delta = self.amount_fen * n
        ok_count = len(oids) == n and len(flow_rows) == n
        ok_delta = delta == expected_delta
        ok_amount = all(int(r[2]) == self.amount_fen for r in flow_rows)
        ok_store = all(int(r[4]) == self.a.store_a for r in flow_rows)
        ok_flows_total = (after_flows - before_flows) == n
        return self.step("并发 %d 笔：流水条数/余额增量精确" % n,
                         ok_count and ok_delta and ok_amount and ok_store and ok_flows_total,
                         "paid=%d flows=%d delta=%s(期望%s) 总流水变化=%d 耗时=%.1fs" % (
                             len(oids), len(flow_rows), delta, expected_delta,
                             after_flows - before_flows, cost))

    # ---------- 1.3 审计 ----------
    def check_audit(self):
        nulls = int(self.scalar("SELECT COUNT(*) FROM fund_flows WHERE create_user_id IS NULL", "0"))
        rows = []
        for oid in self.new_order_ids:
            rows.extend(self.order_flow(oid))
        bad = [r for r in rows if str(r[3]) != str(self.user_id)]
        ok = nulls == 0 and bool(rows) and not bad
        return self.step("审计：create_user_id 已写入且全库无 null",
                         ok, "本run流水=%d 操作人异常=%d 全库null=%d 期望userId=%s 样例=%s" % (
                             len(rows), len(bad), nulls, self.user_id,
                             [(r[0], r[3]) for r in rows[:3]]))

    # ---------- 渠道路由：微信/支付宝必须入账到本店对应账户 ----------
    def check_channel_routing(self):
        # 门店 B 微信 → 必须落到「门店B微信商户账户」
        wechat_b = self.channel_account_id(self.a.store_b, "WECHAT")
        oid_b, num_b, st_c, b_c = self.create_order(self.a.store_b)
        if not oid_b or wechat_b is None:
            return self.step("门店 B 微信支付入账到本店微信账户", False,
                             "下单失败或账户缺失: order=%s wechatAccount=%s %s" % (
                                 oid_b, wechat_b, (b_c.get("message") if isinstance(b_c, dict) else "")))
        st_pb, body_pb = self.pay(oid_b, method="微信支付")
        flows_b = self.order_flow(oid_b)
        ok_b = (ok_code(body_pb) and bool((body_pb.get("data") or {}).get("fundFlowNo"))
                and len(flows_b) == 1
                and int(flows_b[0][1]) == wechat_b
                and int(flows_b[0][4]) == self.a.store_b
                and int(flows_b[0][2]) == self.amount_fen)
        self.step("门店 B 微信支付：入账到门店B微信账户", ok_b,
                  "HTTP %s fundFlowNo=%s flows=%d account=%s(期望%s) store=%s" % (
                      st_pb, (body_pb.get("data") or {}).get("fundFlowNo"), len(flows_b),
                      flows_b[0][1] if flows_b else "-", wechat_b,
                      flows_b[0][4] if flows_b else "-"))

        # 门店 A 支付宝 → 必须落到「门店A支付宝账户」（证明渠道维度也生效）
        alipay_a = self.channel_account_id(self.a.store_a, "ALIPAY")
        oid_a, num_a, st_c2, b_c2 = self.create_order(self.a.store_a)
        if not oid_a or alipay_a is None:
            return self.step("门店 A 支付宝入账到本店支付宝账户", False,
                             "下单失败或账户缺失: order=%s alipayAccount=%s" % (oid_a, alipay_a))
        st_pa, body_pa = self.pay(oid_a, method="支付宝")
        flows_a = self.order_flow(oid_a)
        ok_a = (ok_code(body_pa) and bool((body_pa.get("data") or {}).get("fundFlowNo"))
                and len(flows_a) == 1
                and int(flows_a[0][1]) == alipay_a
                and int(flows_a[0][4]) == self.a.store_a
                and int(flows_a[0][2]) == self.amount_fen)
        self.step("门店 A 支付宝：入账到门店A支付宝账户", ok_a,
                  "HTTP %s fundFlowNo=%s flows=%d account=%s(期望%s) store=%s" % (
                      st_pa, (body_pa.get("data") or {}).get("fundFlowNo"), len(flows_a),
                      flows_a[0][1] if flows_a else "-", alipay_a,
                      flows_a[0][4] if flows_a else "-"))

        return ok_b and ok_a

    def run(self):
        print("=" * 78)
        print("模块 1 财务地基 E2E   base=%s  门店A=%s 门店B=%s 并发=%s 单笔=%s分" % (
            BASE, self.a.store_a, self.a.store_b, self.a.parallel, self.amount_fen))
        print("=" * 78)
        steps = (self.login, self.check_store_isolation, self.check_concurrency,
                 self.check_audit, self.check_channel_routing)
        for fn in steps:
            if not fn():
                print("\n[中断] 关键步骤失败：%s" % fn.__name__)
                break
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        print("\n" + "=" * 78)
        print("结果: %s    PASS=%d FAIL=%d" % (
            "全绿" if not fails else "存在失败", len(self.results) - len(fails), len(fails)))
        for name, state, detail in self.results:
            print("  %-42s %s" % (name, state))
        print("=" * 78)
        return 0 if not fails else 1


def main():
    global BASE
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--username", default="vcashier01")
    p.add_argument("--password", required=True)
    p.add_argument("--terminal-id", default="E2E-FIN-01")
    p.add_argument("--store-a", type=int, default=1)
    p.add_argument("--store-b", type=int, default=2)
    p.add_argument("--parallel", type=int, default=10)
    p.add_argument("--food-code", default="FD260925001")
    p.add_argument("--food-name", default="QA-红烧土豆")
    p.add_argument("--food-price", type=float, default=18.00)
    p.add_argument("--quantity", type=int, default=1)
    p.add_argument("--table-number", type=int, default=1)
    p.add_argument("--seed-sql", default=DEFAULT_SEED_SQL)
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
