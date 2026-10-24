#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
专项审计 §4 跨日/时区边界 + §5 并发幂等（独立脚本，不复用/不重跑 p0/p1/p2 acceptance）

§4.1 E2-1：内部 SALE 业务日 D 23:58 + 外部 bank_transactions D+1 00:03（同额同账户）→ R1 → 期望 TIME_DIFF
§4.2 E3-1：班次 start=D 22:00 / end=D+1 02:00 → SL1 生成 → 期望 business_date=交班日(D+1)，expected 可复算
§4.3 时区：走 I1 导入两条 CSV（一条带 Z、一条不带）→ 断言落库为 Asia/Shanghai 时刻
§5.1 SL1 generate 同 shift 两线程 → deposit_slips 该 shift 只 1 条
§5.2 SL6 deposit 同 slip 两线程 → [depositSlip=] 只 1 对（2 条），余额只变一次
§5.3 S3 statements 同账户同期间两线程 → recon statements 只 1 张

收尾：自造缴款单全部 SL10 冲正；贴账户 20/24 前后余额。
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import threading
import urllib.error
import urllib.request
import uuid
from datetime import date, datetime, timedelta

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
BASE = DEFAULT_BASE


def call(method, path, token=None, body=None, timeout=60, raw=None, content_type=None):
    url = path if path.startswith("http") else BASE + path
    data = raw if raw is not None else (
        json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None)
    h = {"Accept": "application/json"}
    if content_type:
        h["Content-Type"] = content_type
    elif raw is None:
        h["Content-Type"] = "application/json"
    if token:
        h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            txt = r.read().decode("utf-8", "replace")
            try:
                return r.status, json.loads(txt)
            except Exception:
                return r.status, {"_raw": txt[:300]}
    except urllib.error.HTTPError as e:
        txt = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(txt)
        except Exception:
            return e.code, {"_raw": txt[:300]}
    except Exception as e:
        return -1, {"error": str(e)}


def ok_code(b):
    return isinstance(b, dict) and b.get("code") in (0, 200)


def data_of(b):
    return b.get("data") if isinstance(b, dict) else None


def dump(o, n=220):
    try:
        return json.dumps(o, ensure_ascii=False)[:n]
    except Exception:
        return str(o)[:n]


class Runner:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.token = None
        self.cashier = None
        self.teller = None
        self.fixtures = {"shifts": [], "txns": [], "slips": [], "import_batches": []}

    def step(self, name, ok, detail):
        self.results.append((name, "PASS" if ok else "FAIL", detail))
        print("  [%s] %s -> %s" % ("PASS" if ok else "FAIL", name, detail))
        return ok

    def not_ready(self, name, detail):
        self.results.append((name, "NOT_READY", detail))
        print("  [NOT_READY] %s -> %s" % (name, detail))
        return False

    def psql(self, sql, timeout=90):
        try:
            out = subprocess.run(
                [self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port), "-U", self.a.pg_user,
                 "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                capture_output=True, text=True, timeout=timeout, encoding="utf-8", errors="replace",
                env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
            return out.returncode, (out.stdout or "").strip(), (out.stderr or "").strip()
        except Exception as e:
            return -1, "", str(e)

    def scalar(self, sql, default="0"):
        rc, out, err = self.psql(sql)
        if rc != 0:
            raise RuntimeError("psql rc=%s %s" % (rc, (err or out)[:200]))
        return default if out == "" else out

    def rows(self, sql):
        rc, out, err = self.psql(sql)
        if rc != 0:
            raise RuntimeError("psql rc=%s %s" % (rc, (err or out)[:200]))
        return [l.split("|") for l in out.splitlines() if l.strip()]

    def balances(self):
        d = {}
        for r in self.rows("SELECT account_id, balance, version FROM bank_accounts WHERE account_id IN (%d,%d)"
                           % (self.a.cash_account, self.a.bank_account)):
            d[int(r[0])] = (int(r[1]), int(r[2]))
        return d

    def login(self):
        st, b = call("POST", "/v1/auth/login", body={"username": "qa_admin", "password": "Admin@123456"})
        if st == -1:
            return self.not_ready("后端 8081 在监听", dump(b, 120))
        self.token = (data_of(b) or {}).get("token")
        self.step("登录 qa_admin", bool(self.token), "HTTP %s" % st)
        st2, b2 = call("POST", "/v1/auth/login",
                       body={"username": self.a.cashier_user, "password": self.a.cashier_password})
        self.cashier = (data_of(b2) or {}).get("token")
        self.step("登录 %s" % self.a.cashier_user, bool(self.cashier), "HTTP %s" % st2)
        st3, b3 = call("POST", "/v1/auth/login",
                       body={"username": self.a.teller_user, "password": self.a.teller_password})
        self.teller = (data_of(b3) or {}).get("token")
        self.step("登录 %s" % self.a.teller_user, bool(self.teller), "HTTP %s" % st3)
        return bool(self.token)

    # ==================== 4.1 ====================
    def case_4_1(self):
        D = date.today() - timedelta(days=1)
        tag = "AUD41" + datetime.now().strftime("%H%M%S")
        amount = 45678
        rc1, o1, e1 = self.psql(
            "INSERT INTO fund_flows (flow_no, account_id, flow_direction, flow_category, amount, balance_after,"
            " business_date, status, order_id, create_user_id, create_time, update_time, deleted, remark)"
            " VALUES ('AUD%s-FF', %d, 1, 1, %d, %d, '%s', 2, 'AUD41-%s', 1, '%s 23:58:00', now(), 0,"
            " 'AUD41-internal-sale') RETURNING flow_id"
            % (tag, self.a.cash_account, amount, amount, D, tag, D))
        fid = int((o1 or "0").splitlines()[0].strip() or 0) if rc1 == 0 else 0
        rc2, o2, e2 = self.psql(
            "INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id, direction,"
            " is_refund, amount, txn_time, business_date, dedupe_basis, raw_hash, match_status, needs_review,"
            " create_user_id, create_time, update_time, deleted, version)"
            " VALUES (1, %d, 'BANK', 'AUD41-%s', 1, false, %d, '%s 00:03:00', '%s', 'EXTERNAL_ID', md5('%s'),"
            " 'UNMATCHED', false, 1, now(), now(), 0, 0) RETURNING transaction_id"
            % (self.a.cash_account, tag, amount, D + timedelta(days=1), D + timedelta(days=1), tag))
        tid = int((o2 or "0").splitlines()[0].strip() or 0) if rc2 == 0 else 0
        if not (fid and tid):
            return self.not_ready("4.1 造跨日样本", "内部/外部造数失败: %s / %s" % (e1 or "", e2 or ""))
        self.fixtures["txns"].append(tid)

        st, b = call("POST", "/v1/finance/recon-runs?force=true", self.token,
                     {"accountId": int(self.a.cash_account),
                      "businessDate": str(D + timedelta(days=1)), "force": True})
        if not ok_code(b):
            return self.not_ready("4.1 跑批", "R1 HTTP %s %s" % (st, dump(b, 160)))
        run = (data_of(b) or {})
        run = run.get("run") or run
        run_id = run.get("runId")

        item = self.rows(
            "SELECT subject_status, side, COALESCE(internal_ref,''), COALESCE(external_ref,'') FROM recon_items"
            " WHERE run_id=%s AND (internal_ref LIKE 'AUD%s%%' OR external_ref LIKE 'AUD41-%s%%')"
            % (run_id, tag, tag))
        diffs = self.rows(
            "SELECT difference_type, COALESCE(internal_ref,''), COALESCE(external_ref,'') FROM recon_differences"
            " WHERE run_id=%s AND (internal_ref LIKE 'AUD%s%%' OR external_ref LIKE 'AUD41-%s%%')"
            % (run_id, tag, tag))
        dtype = diffs[0][0] if diffs else "(无差异行)"
        subj = item[0][0] if item else "(无主体行)"
        ok = dtype == "TIME_DIFF"
        detail = ("内部 SALE：业务日 %s 23:58；外部流水：txn_time %s 00:03、business_date %s；run=%s；"
                  "recon_items.subject_status=%s；recon_differences.difference_type=%s"
                  % (D, D + timedelta(days=1), D + timedelta(days=1), run_id, subj, dtype))
        if not ok:
            detail += "；实际=%s（期望 TIME_DIFF）" % dtype
        return self.step("4.1 跨日对（23:58 / 次日 00:03）应落 TIME_DIFF", ok, detail)

    # ==================== 4.2 ====================
    def case_4_2(self):
        D = date.today() - timedelta(days=2)
        rc, out, err = self.psql(
            "INSERT INTO pos_shifts (terminal_id, employee_id, employee_name, shift_type, start_time, end_time,"
            " opening_cash, closing_cash, total_orders, total_amount, status, handover_remark, create_time,"
            " update_time, deleted) VALUES ('T-AUD42', 'E-AUD42', 'aud42', 1, '%s 22:00:00',"
            " '%s 02:00:00', 0, 0, 0, 0, 'completed', 'AUD42-FIXTURE', now(), now(), 0) RETURNING shift_id"
            % (D, D + timedelta(days=1)))
        shift_id = int((out or "0").splitlines()[0].strip() or 0) if rc == 0 else 0
        if not shift_id:
            return self.not_ready("4.2 造跨日班次", "insert 失败: %s" % (err or out)[:160])
        self.fixtures["shifts"].append(shift_id)

        st, b = call("POST", "/v1/finance/deposit-slips/generate", self.token,
                     {"shiftId": shift_id, "storeId": int(self.a.store_id)})
        if not ok_code(b):
            return self.not_ready("4.2 生成缴款单", "SL1 HTTP %s %s" % (st, dump(b, 160)))
        d = data_of(b) or {}
        d = d.get("slip") or d
        slip_id = d.get("slipId")
        if slip_id:
            self.fixtures["slips"].append((int(slip_id), d.get("slipNo")))
        has_bd_col = int(self.scalar(
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='public'"
            " AND table_name='deposit_slips' AND column_name='business_date'", "0")) > 0
        bd = str(d.get("businessDate") or "") if has_bd_col else "(deposit_slips 无 business_date 字段)"
        if not has_bd_col:
            bd_db = "(字段不存在)"
        else:
            bd_db = self.scalar("SELECT COALESCE(business_date::text,'') FROM deposit_slips"
                                " WHERE slip_id=%s" % slip_id, "")
        expected = int(d.get("expectedAmount") or 0)
        recomputed = int(self.scalar(
            "SELECT COALESCE(SUM(opr.payment_amount),0) FROM order_payment_records opr "
            "WHERE opr.deleted=0 AND opr.payment_method=1 AND opr.order_id IN ("
            " SELECT o.order_id FROM orders o WHERE o.deleted=0 AND o.store_id=%d"
            " AND o.create_time >= '%s 22:00:00' AND o.create_time <= '%s 02:00:00')"
            % (int(self.a.store_id), D, D + timedelta(days=1)), "0"))
        expect_bd = str(D + timedelta(days=1))
        # 设计 §3.8 E3-1 / TD-36：单据 business_date 取交班日(D+1)；字段缺失即不满足
        ok = has_bd_col and (bd_db == expect_bd) and expected == recomputed
        return self.step("4.2 跨日班次（22:00→次日 02:00）business_date=交班日 且 expected 可复算", ok,
                         "shift_id=%d business_date(响应)=%s / (库内)=%s（期望 %s）expected_amount=%d 分 "
                         "独立复算=%d 分（差 %+d）slip=%s" % (
                             shift_id, bd, bd_db, expect_bd, expected, recomputed,
                             expected - recomputed, d.get("slipNo")))

    # ==================== 4.3 ====================
    def _multipart(self, fields, files):
        boundary = "----aud" + uuid.uuid4().hex
        lines = []
        for k, v in fields.items():
            lines.append("--%s" % boundary)
            lines.append('Content-Disposition: form-data; name="%s"' % k)
            lines.append("")
            lines.append(str(v))
        for k, (fn, content) in files.items():
            lines.append("--%s" % boundary)
            lines.append('Content-Disposition: form-data; name="%s"; filename="%s"' % (k, fn))
            lines.append("Content-Type: text/csv")
            lines.append("")
            lines.append(content)
        lines.append("--%s--" % boundary)
        lines.append("")
        return "\r\n".join(lines).encode("utf-8"), "multipart/form-data; boundary=%s" % boundary

    def case_4_3(self):
        tag = "AUD43" + datetime.now().strftime("%H%M%S")
        csv = ("交易时间,交易金额,收/支,对方户名,流水号,备注\n"
               "2026-10-24T16:30:00Z,123.45,收入,AUD-UTC,%s-A,带Z时区\n"
               "2026-10-25 08:15:00,67.89,收入,AUD-LOCAL,%s-B,无时区\n" % (tag, tag))
        body, ctype = self._multipart({"sourceType": "BANK", "accountId": str(self.a.cash_account)},
                                      {"file": ("aud43.csv", csv)})
        st, b = call("POST", "/v1/finance/bank-transactions/import", self.token, raw=body, content_type=ctype)
        if not ok_code(b):
            return self.not_ready("4.3 导入 CSV", "I1 HTTP %s %s" % (st, dump(b, 200)))
        bid = (data_of(b) or {}).get("batchId")
        self.fixtures["import_batches"].append(bid)
        st2, b2 = call("POST", "/v1/finance/imports/%s/commit" % bid, self.token, {})
        commit_ok = ok_code(b2)
        rows = self.rows(
            "SELECT external_txn_id, txn_time FROM bank_transactions WHERE deleted=0"
            " AND external_txn_id IN ('%s-A','%s-B') ORDER BY external_txn_id" % (tag, tag))
        got = {r[0]: r[1] for r in rows}
        a_val = got.get("%s-A" % tag, "(未落库)")
        b_val = got.get("%s-B" % tag, "(未落库)")
        ok = commit_ok and a_val.startswith("2026-10-25 00:30") and b_val.startswith("2026-10-25 08:15")
        return self.step("4.3 时区：16:30Z → Asia/Shanghai 00:30；无时区按上海解释", ok,
                         "commit=%s；CSV=2026-10-24T16:30:00Z → txn_time=%s（期望 2026-10-25 00:30）；"
                         "CSV=2026-10-25 08:15:00 → txn_time=%s（期望 2026-10-25 08:15）" % (commit_ok, a_val, b_val))

    # ==================== §5 ====================
    def _two_threads(self, fn):
        out = {}

        def wrap(i):
            try:
                out[i] = fn(i)
            except Exception as e:
                out[i] = {"err": str(e)}
        ts = [threading.Thread(target=wrap, args=(i,)) for i in (1, 2)]
        for t in ts:
            t.start()
        for t in ts:
            t.join()
        return out

    def _clone_shift(self, remark):
        rc, o, e = self.psql(
            "INSERT INTO pos_shifts (terminal_id, employee_id, employee_name, shift_type, start_time, end_time,"
            " opening_cash, closing_cash, total_orders, total_amount, status, handover_remark, create_time,"
            " update_time, deleted) SELECT terminal_id, employee_id, employee_name, shift_type, start_time,"
            " end_time, opening_cash, closing_cash, total_orders, total_amount, 'completed', '%s',"
            " now(), now(), 0 FROM pos_shifts WHERE shift_id=%d RETURNING shift_id"
            % (remark, int(self.a.template_shift)))
        sid = int((o or "0").splitlines()[0].strip() or 0) if rc == 0 else 0
        if sid:
            self.fixtures["shifts"].append(sid)
        return sid

    def case_5_1(self):
        shift_id = self._clone_shift("AUD51-FIXTURE")
        if not shift_id:
            return self.not_ready("5.1 造班次", "clone 失败")

        def hit(i):
            st, b = call("POST", "/v1/finance/deposit-slips/generate", self.token,
                         {"shiftId": shift_id, "storeId": int(self.a.store_id)})
            d = data_of(b) or {}
            d = d.get("slip") or d
            return {"http": st, "code": (b or {}).get("code"), "slipId": d.get("slipId"),
                    "reused": (data_of(b) or {}).get("reused", d.get("reused"))}
        out = self._two_threads(hit)
        cnt = int(self.scalar("SELECT COUNT(*) FROM deposit_slips WHERE shift_id=%d AND deleted=0" % shift_id, "0"))
        for v in out.values():
            if v.get("slipId"):
                self.fixtures["slips"].append((int(v["slipId"]), None))
        self.fixtures["slips"] = list(dict.fromkeys(self.fixtures["slips"]))
        return self.step("5.1 SL1 generate 同 shift 两线程 → 该 shift 只有 1 条缴款单", cnt == 1,
                         "线程1=%s；线程2=%s；deposit_slips(shift_id=%d)=%d 条" % (
                             dump(out.get(1), 120), dump(out.get(2), 120), shift_id, cnt))

    def case_5_2(self):
        shift_id = self._clone_shift("AUD52-FIXTURE")
        if not shift_id:
            return self.not_ready("5.2 造班次", "clone 失败")
        st, b = call("POST", "/v1/finance/deposit-slips/generate", self.token,
                     {"shiftId": shift_id, "storeId": int(self.a.store_id)})
        d = data_of(b) or {}
        d = d.get("slip") or d
        slip_id, slip_no = d.get("slipId"), d.get("slipNo")
        if not slip_id:
            return self.not_ready("5.2 生成缴款单", dump(b, 160))
        self.fixtures["slips"].append((int(slip_id), slip_no))
        call("POST", "/v1/finance/deposit-slips/%d/submit" % slip_id, self.cashier or self.token, {"remark": "aud52"})
        expected = int(d.get("expectedAmount") or 0)
        actual = max(0, expected - 100)
        call("POST", "/v1/finance/deposit-slips/%d/receive" % slip_id, self.teller or self.token,
             {"receivedAmountFen": actual, "remark": "aud52-short"})
        tag = "AUD52" + datetime.now().strftime("%H%M%S")
        rc2, o2, e2 = self.psql(
            "INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id, direction,"
            " is_refund, amount, txn_time, business_date, dedupe_basis, raw_hash, match_status, needs_review,"
            " create_user_id, create_time, update_time, deleted, version) VALUES (1, %d, 'BANK', '%s', 1,"
            " false, %d, now(), CURRENT_DATE, 'EXTERNAL_ID', md5('%s'), 'UNMATCHED', false, 1, now(), now(),"
            " 0, 0) RETURNING transaction_id" % (self.a.bank_account, tag, actual, tag))
        tid = int((o2 or "0").splitlines()[0].strip() or 0) if rc2 == 0 else 0
        if tid:
            self.fixtures["txns"].append(tid)
        bal_before = self.balances()

        def hit(i):
            st, b = call("POST", "/v1/finance/deposit-slips/%d/deposit" % slip_id, self.teller or self.token,
                         {"bankTransactionId": tid or None, "depositRefNo": tag, "remark": "aud52-concurrent"})
            return {"http": st, "code": (b or {}).get("code"), "msg": (b or {}).get("message")}
        out = self._two_threads(hit)
        cnt = int(self.scalar(
            "SELECT COUNT(*) FROM fund_flows WHERE deleted=0 AND flow_category=5 "
            "AND remark LIKE '%%[depositSlip=%s]%%'" % slip_no, "0"))
        bal_after = self.balances()
        cash_delta = bal_before[self.a.cash_account][0] - bal_after[self.a.cash_account][0]
        return self.step("5.2 SL6 deposit 同 slip 两线程 → 只 1 对流水、余额只变一次",
                         cnt == 2 and cash_delta == actual,
                         "线程1=%s；线程2=%s；[depositSlip=%s] 流水=%d 条（期望 2）；现金户 %d→%d（Δ-%d，期望 -%d）" % (
                             dump(out.get(1), 120), dump(out.get(2), 120), slip_no, cnt,
                             bal_before[self.a.cash_account][0], bal_after[self.a.cash_account][0],
                             cash_delta, actual))

    def case_5_3(self):
        today = date.today()
        period = None
        for back in range(60, 95):
            ps = (today - timedelta(days=back)).strftime("%Y-%m-%d")
            pe = today.strftime("%Y-%m-%d")
            n = int(self.scalar("SELECT COUNT(*) FROM reconciliation_statements WHERE account_id=%d AND deleted=0"
                                " AND period_start='%s' AND period_end='%s'" % (self.a.cash_account, ps, pe), "0"))
            if n == 0:
                period = (ps, pe)
                break
        if not period:
            return self.not_ready("5.3 选择空期间", "60–95 天内期间均被占用")

        def hit(i):
            st, b = call("POST", "/v1/finance/recon/statements", self.token,
                         {"accountId": int(self.a.cash_account), "periodStart": period[0],
                          "periodEnd": period[1], "bankStatementBalanceFen": 1000000,
                          "remark": "aud53-concurrent"})
            d = data_of(b) or {}
            return {"http": st, "code": (b or {}).get("code"), "statementId": d.get("statementId"),
                    "status": d.get("status"), "msg": (b or {}).get("message")}
        out = self._two_threads(hit)
        cnt = int(self.scalar("SELECT COUNT(*) FROM reconciliation_statements WHERE account_id=%d AND deleted=0"
                              " AND period_start='%s' AND period_end='%s'"
                              % (self.a.cash_account, period[0], period[1]), "0"))
        return self.step("5.3 S3 statements 同账户同期间两线程 → 只 1 张", cnt == 1,
                         "期间=%s..%s；线程1=%s；线程2=%s；该期间调节表=%d 张" % (
                             period[0], period[1], dump(out.get(1), 120), dump(out.get(2), 120), cnt))

    def cleanup(self):
        slips = [s for s in self.fixtures["slips"] if s[0]]
        done = 0
        for slip_id, _ in slips:
            st, b = call("POST", "/v1/finance/deposit-slips/%d/reverse" % slip_id, self.token,
                         {"reason": "audit-cleanup", "reverseFlows": True})
            row = self.scalar("SELECT status FROM deposit_slips WHERE slip_id=%d" % slip_id, "")
            if ok_code(b) or str(row) == "REVERSED":
                done += 1
        self.step("收尾：%d 张缴款单 SL10 冲正" % len(slips), done == len(slips), "已冲正 %d/%d" % (done, len(slips)))

    def run(self):
        print("=" * 88)
        print("专项审计 §4 跨日/时区边界 + §5 并发幂等")
        print("=" * 88)
        if not self.login():
            return self.summary()
        bal_before = self.balances()
        print("\n--- 4 跨日/时区 ---")
        self.case_4_1()
        self.case_4_2()
        self.case_4_3()
        print("\n--- 5 并发幂等 ---")
        self.case_5_1()
        self.case_5_2()
        self.case_5_3()
        print("\n--- 收尾 ---")
        self.cleanup()
        bal_after = self.balances()
        self.step("账户余额前后对比", True, "; ".join(
            "acct%d %d→%d（Δ%+d）" % (aid, bal_before[aid][0], bal_after[aid][0],
                                     bal_after[aid][0] - bal_before[aid][0])
            for aid in (self.a.cash_account, self.a.bank_account)))
        self.step("夹具清单（可删）", True, "pos_shifts=%s；bank_transactions=%s；slips=%s" % (
            self.fixtures["shifts"], self.fixtures["txns"], [s[1] or s[0] for s in self.fixtures["slips"]]))
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        nr = [r for r in self.results if r[1] == "NOT_READY"]
        print("\n" + "=" * 88)
        print("结果: %s  PASS=%d FAIL=%d%s" % (
            "全绿" if not fails and not nr else ("未就绪" if not fails else "存在失败"),
            len([r for r in self.results if r[1] == "PASS"]), len(fails),
            ("  NOT_READY=%d" % len(nr)) if nr else ""))
        for name, state, _ in self.results:
            print("  %-62s %s" % (name, state))
        if fails or nr:
            print("-" * 88)
            for name, state, detail in fails + nr:
                print("  * [%s] %s\n    %s" % (state, name, detail))
        print("=" * 88)
        if fails:
            return 1
        return 2 if nr else 0


def main():
    global BASE
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--cash-account", type=int, default=20)
    p.add_argument("--bank-account", type=int, default=24)
    p.add_argument("--template-shift", type=int, default=19)
    p.add_argument("--cashier-user", default="vcashier01")
    p.add_argument("--cashier-password", default="123456")
    p.add_argument("--teller-user", default="qa_teller")
    p.add_argument("--teller-password", default="Admin@123456")
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    BASE = a.base
    try:
        return Runner(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
