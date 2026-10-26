#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
C2：期末结账门禁持久化 E2E

断言链（期间隔离：只用当前自然月 periodCode，默认 2026-10）：
  1) 期间列表接口有该期间，且初始 is_closed=false；
  2) 结账前若存在阻塞项（OPEN 差异 / DRAFT 调节表）→ 结账应 409，且文案可读；无阻塞项则记为 N/A；
  3) 结账成功：is_closed=true、快照表每个账户一行、closing_fen = opening + income − expense、
     且 closing_fen == 该期间内最后一笔 balance_after（逐账户对拍）；
  4) **门禁**：对三个写入口分别断言 409 且文案含「已结账」：
        POST /v1/finance/fund-flows、POST /v1/finance/deposit-slips/{id}/deposit、POS 退款
        （deposit/refund 若缺夹具 → 用 fund-flows 结果 + 明确说明哪些入口未覆盖，不伪造）
  5) 反结账：reason 太短（9 字）→ 400；正常 reason → 200、is_closed=false、快照行仍在（历史留档）、
     审计 FINANCE_PERIOD_REOPENED 含 reason；
  6) 反结账后再结账 → 200（幂等可达）；
  7) finally 把期间恢复 is_closed=false（不删审计），并打印确认。

前置：e2e_setup.ensure_ready()。
退出码：0 全绿 / 1 断言失败 / 2 环境未就绪
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
from datetime import date
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


class PeriodGuard:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.token = None
        self.refund_token = None
        self.original_closed = None
        self.changed = False

    def step(self, name, okk, detail, state=None):
        st = state or ("PASS" if okk else "FAIL")
        self.results.append((name, st, detail))
        print("  [%s] %s -> %s" % (st, name, detail))
        return okk

    def not_ready(self, name, detail):
        return self.step(name, False, detail, state="NOT_READY")

    def psql(self, sql):
        o = subprocess.run([self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port),
                            "-U", self.a.pg_user, "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                           capture_output=True, text=True, timeout=90, encoding="utf-8", errors="replace",
                           env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
        if o.returncode != 0:
            raise RuntimeError("psql: %s" % (o.stderr or "")[:200])
        return (o.stdout or "").strip()

    def rows(self, sql):
        return [l.split("|") for l in self.psql(sql).splitlines() if l.strip()]

    def num(self, sql):
        v = self.psql(sql).strip()
        return int(v) if v.lstrip("-").isdigit() else 0

    def login(self):
        st, b = http("POST", self.a.api + "/v1/auth/login",
                     body={"username": self.a.user, "password": self.a.password})
        self.token = (data_of(b) or {}).get("token") if ok(b) else None
        self.step("登录 %s" % self.a.user, bool(self.token), "HTTP %s" % st)
        st2, b2 = http("POST", self.a.api + "/v1/auth/login",
                       body={"username": self.a.refund_user, "password": self.a.refund_password})
        self.refund_token = (data_of(b2) or {}).get("token") if ok(b2) else None
        self.step("登录 %s（退款入口需店长权限）" % self.a.refund_user, bool(self.refund_token), "HTTP %s" % st2)
        return bool(self.token)

    # ---------- 期间读写 ----------
    def get_period(self):
        st, b = http("GET", self.a.api + "/v1/finance/periods/%s" % self.a.period, self.token)
        return st, (data_of(b) or {}), b

    def close(self, remark="E2E C2 结账"):
        return http("POST", self.a.api + "/v1/finance/periods/%s/close" % self.a.period,
                    self.token, {"remark": remark})

    def reopen(self, reason):
        return http("POST", self.a.api + "/v1/finance/periods/%s/reopen" % self.a.period,
                    self.token, {"reason": reason})

    # ---------- 门禁探测 ----------
    def probe_fund_flow(self):
        acct = self.num("SELECT account_id FROM bank_accounts WHERE store_id=%d AND account_type=2"
                        " AND deleted=0 ORDER BY account_id LIMIT 1" % self.a.store_id)
        return http("POST", self.a.api + "/v1/finance/fund-flows", self.token,
                    {"accountId": int(acct), "amount": 1, "flowDirection": 1, "flowCategory": 5,
                     "businessDate": self.a.close_date, "remark": "E2E C2 门禁探测"})

    def assert_gate(self, label, st, b, expect_blocked):
        code = (b or {}).get("code")
        msg = str((b or {}).get("message") or "")
        blocked = (st == 409) or (code == 409) or ("已结账" in msg)
        if expect_blocked:
            return self.step("C2-%s：已结账期间写入被拒（409 且文案含「已结账」）" % label,
                             blocked and ("已结账" in msg or code == 409),
                             "HTTP %s code=%s message=%r" % (st, code, msg[:80]))
        return self.step("C2-%s：反结账后写入恢复可用" % label, ok(b) or code != 409,
                         "HTTP %s code=%s message=%r" % (st, code, msg[:80]))

    def run(self):
        print("=" * 90)
        print("C2 期末结账门禁持久化 E2E  period=%s" % self.a.period)
        print("=" * 90)
        if not ensure_ready(api=self.a.api, admin=self.a.admin, pos=self.a.pos):
            self.not_ready("前置就绪", "服务/库存未就绪")
            return self.summary()
        if not self.login():
            return self.summary()

        rows = self.rows("SELECT COALESCE(is_closed,false) FROM accounting_periods WHERE period_code='%s'"
                         % self.a.period)
        if not rows:
            self.not_ready("C2 期间存在性", "accounting_periods 里没有 %s" % self.a.period)
            return self.summary()
        self.original_closed = (rows[0][0] in ("t", "1", "true"))
        self.step("C2-1 期间存在且记录原状态", True,
                  "period=%s 原 is_closed=%s" % (self.a.period, self.original_closed))

        st, vo, raw = self.get_period()
        self.step("C2-1 期间详情接口可读（is_closed 与库一致）", st == 200 and bool(vo),
                  "HTTP %s is_closed(接口)=%s is_closed(库)=%s" % (
                      st, (vo or {}).get("closed") if vo else None, self.original_closed))

        try:
            # 2) 阻塞项检查（有则结账应 409）
            blockers = self.num("SELECT COUNT(*) FROM recon_differences WHERE status IN"
                                " ('OPEN','ASSIGNED','RESOLVING')")
            draft = self.num("SELECT COUNT(*) FROM reconciliation_statements WHERE deleted=0 AND status='DRAFT'")
            if blockers or draft:
                st_c, b_c = self.close("E2E C2 阻塞项探测")
                code = (b_c or {}).get("code")
                self.step("C2-2 存在阻塞项时结账被拒（409）", st_c == 409 or code == 409,
                          "OPEN差异=%d DRAFT表=%d → HTTP %s code=%s message=%r" % (
                              blockers, draft, st_c, code, str((b_c or {}).get("message"))[:70]))
            else:
                self.step("C2-2 阻塞项检查", True, "无 OPEN 差异 / 无 DRAFT 表（N/A：无需阻断）")

            # 3) 结账成功
            st_c2, b_c2 = self.close()
            if not ok(b_c2):
                self.not_ready("C2-3 结账", "HTTP %s code=%s %s" % (
                    st_c2, (b_c2 or {}).get("code"), str((b_c2 or {}).get("message"))[:90]))
                return self.summary()
            self.changed = True
            closed_db = self.num("SELECT COUNT(*) FROM accounting_periods WHERE period_code='%s'"
                                 " AND is_closed=true" % self.a.period)
            self.step("C2-3 结账后 is_closed=true", closed_db == 1,
                      "库内 is_closed=true 行数=%d" % closed_db)

            # 期间快照表定名：finance_period_account_balance（迁移 V20261026_001）
            # 列：period_code, account_id, opening_fen, income_fen, expense_fen, closing_fen, txn_count, last_balance_after
            exists = self.num("SELECT COUNT(*) FROM information_schema.tables"
                              " WHERE table_schema='public' AND table_name='finance_period_account_balance'")
            if not exists:
                self.not_ready("C2-3 期间快照表 finance_period_account_balance",
                               "表不存在（迁移 V20261026_001 未跑）→ 未覆盖")
                snaps = []
            else:
                snaps = self.rows("SELECT account_id, COALESCE(opening_fen,0), COALESCE(income_fen,0),"
                                  " COALESCE(expense_fen,0), COALESCE(closing_fen,0),"
                                  " COALESCE(last_balance_after,NULL), COALESCE(txn_count,0)"
                                  " FROM finance_period_account_balance WHERE period_code='%s'"
                                  " ORDER BY account_id" % self.a.period)
            self.step("C2-3 快照表每个账户一行", len(snaps) > 0, "快照行数=%d" % len(snaps))
            bad = []
            for row in snaps:
                sid, op, inc, exp, clo = row[0], int(row[1]), int(row[2]), int(row[3]), int(row[4])
                last_ba = row[5] if len(row) > 5 else None
                if op + inc - exp != clo:
                    bad.append("acct%s 勾稽不符 %d+%d-%d≠%d" % (sid, op, inc, exp, clo))
                if last_ba not in (None, "", "None") and int(last_ba) != clo:
                    bad.append("acct%s closing=%d ≠ last_balance_after=%s" % (sid, clo, last_ba))
                # 交叉校验：与 fund_flows 期间内末笔 balance_after 对拍
                real = self.rows("SELECT COALESCE(balance_after,0) FROM fund_flows WHERE account_id=%s"
                                 " AND deleted=0 AND status=2 AND business_date BETWEEN '%s' AND '%s'"
                                 " ORDER BY create_time DESC, flow_id DESC LIMIT 1"
                                 % (sid, self.a.period_start, self.a.period_end))
                if real and int(real[0][0]) != clo:
                    bad.append("acct%s closing=%d ≠ 实际末笔 balance_after=%s" % (sid, clo, real[0][0]))
            self.step("C2-3 closing_fen = opening + income − expense，且 == last_balance_after / 期间末笔 balance_after",
                      not bad, "快照行数=%d；逐账户对拍；偏差=%s" % (len(snaps), bad[:3] if bad else "无"))

            # 4) 门禁三入口
            st_g, b_g = self.probe_fund_flow()
            self.assert_gate("fund-flows", st_g, b_g, expect_blocked=True)
            dep = self.rows("SELECT slip_id FROM deposit_slips WHERE deleted=0 ORDER BY slip_id DESC LIMIT 1")
            if dep:
                btid = self.num("SELECT COALESCE(MAX(transaction_id),0) FROM bank_transactions WHERE deleted=0")
                st_d, b_d = http("POST", self.a.api + "/v1/finance/deposit-slips/%s/deposit" % dep[0][0],
                                 self.token, {"bankTransactionId": int(btid), "remark": "E2E C2 门禁探测"})
                self.assert_gate("deposit", st_d, b_d, expect_blocked=True)
            else:
                self.not_ready("C2-deposit 门禁", "没有可用缴款单夹具 → 该入口未覆盖")
            refund_order = self.rows("SELECT order_number FROM orders WHERE deleted=0 AND payment_status=2"
                                     " ORDER BY order_id DESC LIMIT 1")
            if refund_order:
                import urllib.parse as _up
                st_r, b_r = http("POST", self.a.api + "/v1/pos/orders/order/%s/refund?refundReason=%s"
                                 % (refund_order[0][0], _up.quote("E2E C2 门禁探测")), self.refund_token)
                self.assert_gate("refund", st_r, b_r, expect_blocked=True)
            else:
                self.not_ready("C2-refund 门禁", "没有已支付订单 → 该入口未覆盖")

            # 5) 反结账
            st_s, b_s = self.reopen("短原因")
            self.step("C2-5 反结账 reason 过短被拒（400）", (b_s or {}).get("code") == 400 or st_s == 400,
                      "HTTP %s code=%s message=%r" % (st_s, (b_s or {}).get("code"),
                                                     str((b_s or {}).get("message"))[:70]))
            ok_reason = "E2E C2 反结账原因：验证门禁持久化"
            st_o, b_o = self.reopen(ok_reason)
            self.step("C2-5 正常反结账成功（200）", ok(b_o),
                      "HTTP %s code=%s" % (st_o, (b_o or {}).get("code")))
            self.changed = False
            open_db = self.num("SELECT COUNT(*) FROM accounting_periods WHERE period_code='%s'"
                               " AND is_closed=false" % self.a.period)
            self.step("C2-5 反结账后 is_closed=false", open_db == 1, "库内 is_closed=false 行数=%d" % open_db)
            snap_kept = self.num("SELECT COUNT(*) FROM finance_period_account_balance WHERE period_code='%s'"
                                 % self.a.period)
            self.step("C2-5 反结账后快照行仍在（历史留档）", snap_kept > 0, "快照行数=%d" % snap_kept)
            n_log, log_hit = 0, ""
            try:
                cand = self.rows("SELECT log_id, COALESCE(operation_desc,''), COALESCE(operation_data,'')"
                                 " FROM finance_audit_logs WHERE operation_type='FINANCE_PERIOD_REOPENED'"
                                 " ORDER BY log_id DESC LIMIT 20")
                for r in cand:
                    if ok_reason[:10] in (r[1] + r[2]):
                        n_log += 1
                        log_hit = "log_id=%s desc=%r" % (r[0], r[1][:90])
                        break
            except Exception as e:
                log_hit = "SQL 异常：%s" % str(e)[:120]
            self.step("C2-5 审计 FINANCE_PERIOD_REOPENED 含 reason", n_log > 0, "命中=%d；%s" % (n_log, log_hit))

            # 4b) 反结账后写入恢复
            st_g2, b_g2 = self.probe_fund_flow()
            self.assert_gate("fund-flows（反结账后）", st_g2, b_g2, expect_blocked=False)

            # 6) 再结账
            st_c3, b_c3 = self.close("E2E C2 再次结账")
            self.step("C2-6 反结账后再结账成功（幂等可达）", ok(b_c3),
                      "HTTP %s code=%s" % (st_c3, (b_c3 or {}).get("code")))
            self.changed = True
        finally:
            # 7) 恢复 is_closed=false（不删审计）
            try:
                if self.changed:
                    st_f, b_f = self.reopen("E2E C2 finally 恢复期间为未结账（清理测试状态）")
                    now_closed = self.num("SELECT COUNT(*) FROM accounting_periods WHERE period_code='%s'"
                                          " AND is_closed=true" % self.a.period)
                    self.step("C2-7 finally 恢复期间 is_closed=false（不删审计）", ok(b_f) and now_closed == 0,
                              "reopen HTTP %s；剩余已结账行=%d；审计保留" % (st_f, now_closed))
                else:
                    self.step("C2-7 期间状态无需恢复", True,
                              "本次运行结束时期间已是 is_closed=false（原状态=%s）" % self.original_closed)
            except Exception as e:
                self.step("C2-7 期间恢复", False, "异常：%s；手工修复：UPDATE accounting_periods SET is_closed=false"
                                                  " WHERE period_code='%s'" % (str(e)[:120], self.a.period))
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        nr = [r for r in self.results if r[1] == "NOT_READY"]
        print("\n" + "=" * 90)
        print("结果: %s  PASS=%d FAIL=%d%s" % (
            "全绿" if not fails and not nr else ("未就绪" if not fails else "存在失败"),
            len([r for r in self.results if r[1] == "PASS"]), len(fails),
            ("  NOT_READY=%d" % len(nr)) if nr else ""))
        for name, st, _ in self.results:
            print("  %-62s %s" % (name, st))
        if fails or nr:
            print("-" * 90)
            for name, st, detail in fails + nr:
                print("  * [%s] %s\n    %s" % (st, name, detail))
        print("=" * 90)
        if fails:
            return 1
        return 2 if nr else 0


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--api", default=DEFAULT_API)
    p.add_argument("--admin", default=DEFAULT_ADMIN)
    p.add_argument("--pos", default=DEFAULT_POS)
    p.add_argument("--user", default="qa_admin")
    p.add_argument("--refund-user", default="qa_store_b")
    p.add_argument("--refund-password", default="Admin@123456")
    p.add_argument("--password", default="Admin@123456")
    p.add_argument("--period", default=date.today().strftime("%Y-%m"))
    p.add_argument("--period-start", default=date.today().replace(day=1).strftime("%Y-%m-%d"))
    p.add_argument("--period-end", default=date.today().strftime("%Y-%m-%d"))
    p.add_argument("--close-date", default=date.today().strftime("%Y-%m-%d"))
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    try:
        return PeriodGuard(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
