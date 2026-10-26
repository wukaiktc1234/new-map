#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
R5 E2E：退款三入口 + 多笔部分退款 + 幂等

断言分组：
  1) POS 入口：下单 → 支付（收银）→ 退款（店长）→ 断言退款流水 **1 笔**
  2) 后台入口：POST /v1/order-new/{orderId}/refund（申请）→ POST /v1/order-new/refunds/{refundId}/approve（审核）
     → 按 fund-500 定的**唯一写入时点**断言：申请后**不产生**流水；审核后**恰产生 1 笔**
  3) 多笔部分退款：订单 100 元 → 退 30 元 → 再退 20 元
     → order_refund_records **2 条**、退款流水 **2 笔**（refund_record_id 不同、3000/2000 分）
     → 现金户余额累计减少 **5000 分** → 巡检 **A30/A31/A33 全为 0**
  4) 幂等：同一 refundId 重复 approve → 流水仍 **1 笔**、余额不再变化
  5) 清理按 tag（bank_transactions / 订单备注标记）；**不裸 UPDATE**

口径来源：
  · 退款流水落库时点与唯一性：fund-500 R5 实现（refund_record_id 唯一索引 uk_fund_flows_refund_record）
  · A30/A31/A33：scripts/qa/consistency_check.py 巡检输出（脚本内自算仅作交叉校验）

退出码：0 全绿 / 1 断言失败 / 2 环境未就绪
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import date, datetime
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
try:
    from e2e_setup import ensure_ready
except Exception as e:  # pragma: no cover
    print("[FATAL] 无法导入 e2e_setup：%s" % e)
    sys.exit(2)

DEFAULT_API = "http://localhost:8081/api"
DEFAULT_ADMIN = "http://localhost:3002"
DEFAULT_POS = "http://localhost:3001"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"


def http(method, url, token=None, body=None, timeout=90):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    h = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            txt = r.read().decode("utf-8", "replace")
            try:
                return r.status, json.loads(txt)
            except Exception:
                return r.status, {"_raw": txt[:200]}
    except urllib.error.HTTPError as e:
        txt = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(txt)
        except Exception:
            return e.code, {"_raw": txt[:200]}
    except Exception as e:
        return -1, {"error": str(e)}


def ok(b):
    return isinstance(b, dict) and b.get("code") in (0, 200)


def data_of(b):
    return b.get("data") if isinstance(b, dict) else None


class RefundEntries:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.tag = "RFE" + datetime.now().strftime("%m%d%H%M%S")

    def step(self, name, okk, detail, state=None):
        st = state or ("PASS" if okk else "FAIL")
        self.results.append((name, st, detail))
        print("  [%s] %s -> %s" % (st, name, detail))
        return okk

    def not_ready(self, name, detail):
        return self.step(name, False, detail, state="NOT_READY")

    # ---------- DB ----------
    def psql(self, sql):
        o = subprocess.run([self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port),
                            "-U", self.a.pg_user, "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                           capture_output=True, text=True, timeout=120, encoding="utf-8", errors="replace",
                           env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
        if o.returncode != 0:
            raise RuntimeError("psql: %s" % (o.stderr or "")[:200])
        return (o.stdout or "").strip()

    def rows(self, sql):
        return [l.split("|") for l in self.psql(sql).splitlines() if l.strip()]

    def num(self, sql):
        v = self.psql(sql).strip()
        return int(v) if v.lstrip("-").isdigit() else 0

    # ---------- 登录 ----------
    def login(self):
        def _l(u, p):
            st, b = http("POST", self.a.api + "/v1/auth/login", body={"username": u, "password": p})
            return (data_of(b) or {}).get("token"), st
        self.admin_token, s1 = _l(self.a.user, self.a.password)
        self.cash_token, s2 = _l(self.a.pos_user, self.a.pos_password)
        self.manager_token, s3 = _l(self.a.refund_user, self.a.refund_password)
        self.step("登录 %s（后台/财务）" % self.a.user, bool(self.admin_token), "HTTP %s" % s1)
        self.step("登录 %s（POS 收银）" % self.a.pos_user, bool(self.cash_token), "HTTP %s" % s2)
        self.step("登录 %s（POS 退款=店长）" % self.a.refund_user, bool(self.manager_token), "HTTP %s" % s3)
        self.user_id = self.num("SELECT user_id FROM users WHERE username='%s' LIMIT 1" % self.a.user)
        return all([self.admin_token, self.cash_token, self.manager_token])

    # ---------- 夹具 ----------
    def cash_balance(self):
        r = self.rows("SELECT COALESCE(balance,0) FROM bank_accounts WHERE store_id=%d AND account_type=2"
                      " AND deleted=0 ORDER BY account_id LIMIT 1" % self.a.store_id)
        return int(r[0][0]) if r else 0

    def cash_account(self):
        return self.num("SELECT account_id FROM bank_accounts WHERE store_id=%d AND account_type=2"
                        " AND deleted=0 ORDER BY account_id LIMIT 1" % self.a.store_id)

    def make_order(self, target_fen):
        """按目标金额挑菜品组合（尽量接近），返回 {orderId, orderNumber, amount}"""
        st, b = http("GET", self.a.api + "/v1/pos/api/dishes", self.cash_token)
        dishes = (data_of(b) or []) if ok(b) else []
        priced = [d for d in dishes if d.get("dishId") and float(d.get("price") or 0) > 0]
        if not priced:
            return None, "无可用菜品"
        best = None
        for d in priced:
            unit = int(round(float(d.get("price") or 0) * 100))
            q = max(1, min(9, target_fen // unit)) if unit else 1
            total = unit * q
            if best is None or abs(total - target_fen) < abs(best[2] - target_fen):
                best = (d, q, total)
        dish, qty, total = best
        item = {"productId": str(dish["dishId"]), "productName": dish.get("dishName") or "",
                "quantity": qty, "price": float(dish.get("price") or 0),
                "dishType": dish.get("dishType") or "single"}
        st, b = http("POST", self.a.api + "/v1/pos/orders/order", self.cash_token,
                     {"orderType": "dinein", "items": [item],
                      "idempotencyKey": "rfe-%s" % uuid.uuid4().hex[:12]})
        if not ok(b):
            return None, "下单 HTTP %s code=%s %s" % (st, (b or {}).get("code"), str((b or {}).get("message"))[:60])
        d = data_of(b) or {}
        amt = round(float(d.get("totalAmount") or 0), 2)
        st2, b2 = http("POST", self.a.api + "/v1/pos/orders/order/pay", self.cash_token,
                       {"orderId": d.get("orderId"), "paymentMethod": "现金", "amount": amt})
        if not ok(b2):
            return None, "支付 HTTP %s code=%s %s" % (st2, (b2 or {}).get("code"), str((b2 or {}).get("message"))[:60])
        return {"orderId": d.get("orderId"), "orderNumber": d.get("orderNumber"),
                "amount": amt, "amountFen": int(round(amt * 100))}, ""

    def pos_refund(self, order_number, reason):
        return http("POST", self.a.api + "/v1/pos/orders/order/%s/refund?refundReason=%s"
                    % (order_number, urllib.parse.quote(reason)), self.manager_token)

    def backend_apply(self, order_id, amount_fen, refund_type, reason):
        return http("POST", self.a.api + "/v1/order-new/%s/refund" % order_id, self.admin_token,
                    {"refundType": refund_type, "refundAmount": int(amount_fen),
                     "refundReason": reason, "refundMethod": 1})

    def backend_approve(self, refund_id):
        return http("POST", self.a.api + "/v1/order-new/refunds/%d/approve?approved=true&approveUserId=%d"
                    % (int(refund_id), int(self.user_id)), self.admin_token)

    def refund_flows(self, order_id):
        return self.rows("SELECT flow_id, COALESCE(refund_record_id,0), amount, COALESCE(balance_after,0)"
                         " FROM fund_flows WHERE order_id='%s' AND deleted=0 AND flow_category=2"
                         " ORDER BY flow_id" % order_id)

    # ---------- 1) POS 入口 ----------
    def section_pos(self):
        order, err = self.make_order(3000)
        if not order:
            return self.not_ready("R5-1 POS 入口夹具", err)
        st, b = self.pos_refund(order["orderNumber"], "R5 POS 入口退款 %s" % self.tag)
        if not ok(b):
            return self.step("R5-1 POS 入口退款成功", False,
                             "HTTP %s code=%s msg=%s" % (st, (b or {}).get("code"),
                                                        str((b or {}).get("message"))[:70]))
        flows = self.refund_flows(order["orderId"])
        return self.step("R5-1 POS 入口：退款流水恰好 1 笔", len(flows) == 1,
                         "order=%s amount=%d 分；退款流水=%s" % (order["orderNumber"], order["amountFen"], flows))

    # ---------- 2) 后台入口（申请 → 审核） ----------
    def section_backend(self):
        order, err = self.make_order(5000)
        if not order:
            return self.not_ready("R5-2 后台入口夹具", err)
        amt = order["amountFen"]
        before = len(self.refund_flows(order["orderId"]))
        st, b = self.backend_apply(order["orderId"], amt, 1, "R5 后台入口全额退款 %s" % self.tag)
        if not ok(b):
            return self.step("R5-2 后台申请退款成功", False,
                             "HTTP %s code=%s msg=%s" % (st, (b or {}).get("code"),
                                                        str((b or {}).get("message"))[:70]))
        vo = data_of(b) or {}
        rid = vo.get("refundId") or vo.get("id")
        self.step("R5-2 后台申请退款成功（生成退款单）", bool(rid), "refundId=%s 金额=%s" % (rid, vo.get("refundAmount")))
        if not rid:
            return False
        n_after_apply = len(self.refund_flows(order["orderId"]))
        self.step("R5-2 申请阶段不产生退款流水（唯一写入时点=审核）", n_after_apply == before,
                  "申请前后退款流水=%d → %d（fund-500 指定审核为唯一写入时点）" % (before, n_after_apply))
        st2, b2 = self.backend_approve(rid)
        if not ok(b2):
            return self.step("R5-2 后台审核退款成功", False,
                             "HTTP %s code=%s msg=%s" % (st2, (b2 or {}).get("code"),
                                                        str((b2 or {}).get("message"))[:70]))
        flows = self.refund_flows(order["orderId"])
        self.last_backend = {"orderId": order["orderId"], "refundId": rid, "flows": flows}
        return self.step("R5-2 审核后恰好产生 1 笔退款流水", len(flows) == 1,
                         "refundId=%s；退款流水=%s" % (rid, flows))

    # ---------- 3) 多笔部分退款 ----------
    def section_partial(self):
        order, err = self.make_order(10000)
        if not order:
            return self.not_ready("R5-3 部分退款夹具", err)
        order_id = order["orderId"]
        bal0 = self.cash_balance()
        rids, fails = [], []
        for amt in (3000, 2000):
            st, b = self.backend_apply(order_id, amt, 2, "R5 部分退款 %d 分 %s" % (amt, self.tag))
            vo = data_of(b) or {}
            rid = vo.get("refundId") or vo.get("id")
            if not ok(b) or not rid:
                fails.append("apply %d -> HTTP %s code=%s msg=%s" % (
                    amt, st, (b or {}).get("code"), str((b or {}).get("message"))[:60]))
                continue
            st2, b2 = self.backend_approve(rid)
            if not ok(b2):
                fails.append("approve %d -> HTTP %s code=%s msg=%s" % (
                    rid, st2, (b2 or {}).get("code"), str((b2 or {}).get("message"))[:60]))
                continue
            rids.append(int(rid))
        self.step("R5-3 两笔部分退款（30 元 / 20 元）申请并审核", not fails,
                  "refundIds=%s；失败=%s" % (rids, fails[:2] if fails else "无"))
        recs = self.rows("SELECT refund_id, refund_amount, refund_status FROM order_refund_records"
                         " WHERE order_id='%s' ORDER BY refund_id" % order_id)
        self.step("R5-3 order_refund_records 恰好 2 条", len(recs) == 2,
                  "记录=%s" % recs)
        flows = self.refund_flows(order_id)
        amts = sorted(int(f[2]) for f in flows)
        distinct = len({int(f[1]) for f in flows})
        self.step("R5-3 退款流水 2 笔且 refund_record_id 互不相同、金额 3000/2000",
                  len(flows) == 2 and amts == [2000, 3000] and distinct == 2,
                  "流水=%s；金额=%s；distinct refund_record_id=%d" % (flows, amts, distinct))
        bal1 = self.cash_balance()
        self.step("R5-3 现金户余额累计减少 5000 分", (bal0 - bal1) == 5000,
                  "余额 %d → %d（实际减少 %d）" % (bal0, bal1, bal0 - bal1))
        self.partial_order = order_id

    # ---------- 4) 幂等 ----------
    def section_idempotent(self):
        ctx = getattr(self, "last_backend", None)
        if not ctx:
            return self.not_ready("R5-4 幂等", "缺少后台入口上下文（上一段未通过）")
        before_flows = len(self.refund_flows(ctx["orderId"]))
        bal0 = self.cash_balance()
        st, b = self.backend_approve(ctx["refundId"])
        after_flows = len(self.refund_flows(ctx["orderId"]))
        bal1 = self.cash_balance()
        self.step("R5-4 重复 approve：流水笔数不变（仍 1 笔）",
                  after_flows == before_flows and after_flows == 1,
                  "refundId=%s 重复审核 HTTP %s code=%s；流水 %d → %d" % (
                      ctx["refundId"], st, (b or {}).get("code"), before_flows, after_flows))
        self.step("R5-4 重复 approve：余额不再变化", bal0 == bal1,
                  "余额 %d → %d（差 %d）" % (bal0, bal1, bal1 - bal0))

    # ---------- 5) 巡检 A30/A31/A33 ----------
    def section_inspection(self):
        vals = {}
        try:
            o = subprocess.run([sys.executable, str(HERE / "consistency_check.py")],
                               capture_output=True, text=True, timeout=900,
                               encoding="utf-8", errors="replace", cwd=str(HERE.parent.parent))
            text = (o.stdout or "") + (o.stderr or "")
            for code in ("A30", "A31", "A33"):
                for line in text.splitlines():
                    if code in line and ("[PASS]" in line or "[FAIL]" in line or "[INFO]" in line):
                        m = re.search(r"(?:rows|count)=(\d+)", line) or re.search(r"=(\d+)", line)
                        vals[code] = (int(m.group(1)) if m else None, line.strip()[:140])
                        break
        except Exception as e:
            return self.not_ready("R5-5 巡检 A30/A31/A33", "consistency_check 执行失败：%s" % str(e)[:120])
        for code in ("A30", "A31", "A33"):
            if code not in vals:
                self.not_ready("R5-5 巡检 %s" % code, "巡检输出未找到 %s（可能尚未落盘）" % code)
                continue
            n, line = vals[code]
            self.step("R5-5 巡检 %s == 0（巡检值）" % code, n == 0, "rows=%s；原文=%s" % (n, line))
        # 交叉校验（仅 INFO）
        try:
            x33 = self.num("SELECT COUNT(*) FROM (SELECT o.order_id, COALESCE(o.refund_amount,0)::bigint rf,"
                           " COALESCE(SUM(f.amount),0)::bigint sf FROM orders o"
                           " LEFT JOIN fund_flows f ON f.order_id=o.order_id AND f.deleted=0"
                           "   AND f.flow_category=2 AND f.flow_direction=2"
                           " WHERE o.deleted=0 GROUP BY o.order_id, o.refund_amount"
                           " HAVING COALESCE(SUM(f.amount),0) <> COALESCE(o.refund_amount,0)::bigint) t")
            self.step("R5-5 交叉校验（脚本自算，不作判据）", True, "A33自算=%d（以巡检值为准）" % x33)
        except Exception as e:
            self.step("R5-5 交叉校验", True, "跳过：%s" % str(e)[:100])

    # ---------- 6) 清理 ----------
    def cleanup(self):
        try:
            n = self.num("SELECT COUNT(*) FROM bank_transactions WHERE external_txn_id LIKE '%s%%'" % self.tag)
            if n:
                self.psql("DELETE FROM bank_transactions WHERE external_txn_id LIKE '%s%%'" % self.tag)
            left = self.num("SELECT COUNT(*) FROM bank_transactions WHERE external_txn_id LIKE '%s%%'" % self.tag)
            self.step("R5-6 清理夹具 bank_transactions", left == 0,
                      "tag=%s 删除前=%d 删除后=%d" % (self.tag, n, left))
            self.step("R5-6 退款流水/订单留档", True,
                      "退款流水与订单按业务归属保留（不裸 UPDATE、不物理删）；tag=%s 可据此回溯" % self.tag)
        except Exception as e:
            self.step("R5-6 清理", False, "异常：%s" % str(e)[:140])

    def run(self):
        print("=" * 92)
        print("R5 退款三入口 + 多笔部分退款 E2E  tag=%s" % self.tag)
        print("=" * 92)
        if not ensure_ready(api=self.a.api, admin=self.a.admin, pos=self.a.pos):
            self.not_ready("前置就绪", "服务/库存未就绪")
            return self.summary()
        if not self.login():
            return self.summary()
        self.section_pos()
        self.section_backend()
        self.section_partial()
        self.section_idempotent()
        self.section_inspection()
        self.cleanup()
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        nr = [r for r in self.results if r[1] == "NOT_READY"]
        print("\n" + "=" * 92)
        print("结果: %s  PASS=%d FAIL=%d%s" % (
            "全绿" if not fails and not nr else ("未就绪" if not fails else "存在失败"),
            len([r for r in self.results if r[1] == "PASS"]), len(fails),
            ("  NOT_READY=%d" % len(nr)) if nr else ""))
        for name, st, _ in self.results:
            print("  %-70s %s" % (name, st))
        if fails or nr:
            print("-" * 92)
            for name, st, detail in fails + nr:
                print("  * [%s] %s\n    %s" % (st, name, detail))
        print("=" * 92)
        if fails:
            return 1
        return 2 if nr else 0


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--api", default=DEFAULT_API)
    p.add_argument("--admin", default=DEFAULT_ADMIN)
    p.add_argument("--pos", default=DEFAULT_POS)
    p.add_argument("--user", default="qa_admin")
    p.add_argument("--password", default="Admin@123456")
    p.add_argument("--pos-user", default="vcashier01")
    p.add_argument("--pos-password", default="123456")
    p.add_argument("--refund-user", default="qa_store_b")
    p.add_argument("--refund-password", default="Admin@123456")
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--business-date", default=date.today().strftime("%Y-%m-%d"))
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    try:
        return RefundEntries(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
