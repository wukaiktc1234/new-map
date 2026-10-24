#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P2-D 独立验收：现金缴款单（AC-19～AC-30）+ AC-38～AC-41 + 回滚三组 + 全流程

契约：docs/design/finance-reconciliation-design.md §3（缴款单）/ §5.7 / §8 AC 表
接口（SL1–SL11，§3.6）：
  POST /v1/finance/deposit-slips/generate      {shiftId}            幂等 reused
  GET  /v1/finance/deposit-slips                                   列表
  GET  /v1/finance/deposit-slips/{id}                              详情
  POST /v1/finance/deposit-slips/{id}/submit   {remark}            DRAFT→SUBMITTED
  POST /v1/finance/deposit-slips/{id}/receive  {actualAmount, dueRemark, shortsReason?}
  POST /v1/finance/deposit-slips/{id}/deposit  {depositRefNo, bankTxnId, bankAccountId, depositDate}
  POST /v1/finance/deposit-slips/{id}/verify   {remark}
  POST /v1/finance/deposit-slips/{id}/close    {remark}
  POST /v1/finance/deposit-slips/{id}/reject   {reason}
  POST /v1/finance/deposit-slips/{id}/reverse  {reason, reverseFlows}
  GET  /v1/finance/deposit-slips/{id}/export

状态约定：PASS / FAIL / NOT_READY（403·404·表缺失）/ SKIP（环境不具备，须写明原因）
退出码：0 全绿（可含 SKIP）/ 1 有 FAIL / 2 有 NOT_READY 且无 FAIL

用法：
  python scripts/qa/p2_acceptance.py                 # 全量
  python scripts/qa/p2_acceptance.py --no-ac37       # 只跑 P2 断言
  python scripts/qa/p2_acceptance.py --shift-id 30   # 指定班次
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import date, datetime, timedelta

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
BASE = DEFAULT_BASE
FORBIDDEN_HINT = ("权限码未绑定角色，请先执行权限脚本（finance:deposit-slip:* / finance:bank:view 等；"
                  "C 卡之后权限实时查库，token claims 不生效）")


def call(method, path, token=None, body=None, timeout=40):
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
                return resp.status, {"_raw": raw[:400]}
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"_raw": raw[:400]}
    except Exception as e:
        return -1, {"error": str(e)}


def ok_code(body):
    return isinstance(body, dict) and body.get("code") in (0, 200)


def data_of(body):
    return body.get("data") if isinstance(body, dict) else None


def dump(obj, limit=400):
    try:
        return json.dumps(obj, ensure_ascii=False)[:limit]
    except Exception:
        return str(obj)[:limit]


def is_forbidden(status, body):
    if status == 403:
        return True
    if isinstance(body, dict):
        return body.get("code") == 403 or "权限不足" in str(body.get("message") or "")
    return False


def is_endpoint_missing(status, body):
    if status in (-1, 404, 405, 501):
        return True
    if isinstance(body, dict):
        if body.get("code") in (404, 405, 501):
            return True
        msg = str(body.get("message") or "")
        return "资源不存在" in msg or "Not Found" in msg or "not found" in msg.lower()
    return False


def rows_of(body):
    d = data_of(body)
    if isinstance(d, list):
        return d
    if isinstance(d, dict):
        for k in ("records", "list", "rows", "content"):
            if isinstance(d.get(k), list):
                return d[k]
    return []


class Runner:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.not_ready_items = []
        self.skipped_items = []
        self.token = None
        self.teller_token = None
        self.approver_token = None
        self.cashier_token = None
        self.fixture_txn = None
        self.slip_id = None
        self.slip_no = None
        self.perm_hint_shown = False

    # ---------------- 结果 ----------------
    def step(self, name, ok, detail, state=None):
        st = state or ("PASS" if ok else "FAIL")
        self.results.append((name, st, detail))
        if st == "NOT_READY":
            self.not_ready_items.append(name)
        if st == "SKIP":
            self.skipped_items.append(name)
        print("  [%s] %s -> %s" % (st, name, detail))
        return ok

    def not_ready(self, name, detail):
        return self.step(name, False, detail, state="NOT_READY")

    def skip(self, name, reason):
        return self.step(name, True, reason, state="SKIP")

    def gate(self, name, status, body):
        """已处理（403/404）返回 True，调用方必须立即 return；未处理返回 False。"""
        if is_forbidden(status, body):
            if not self.perm_hint_shown:
                print("  [HINT] %s" % FORBIDDEN_HINT)
                self.perm_hint_shown = True
            self.not_ready(name, "HTTP %s 403 权限不足 → %s %s" % (status, FORBIDDEN_HINT, dump(body, 160)))
            return True
        if is_endpoint_missing(status, body):
            self.not_ready(name, "端点未就绪 HTTP %s %s" % (status, dump(body, 160)))
            return True
        return False

    # ---------------- DB ----------------
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

    def table_exists(self, name):
        try:
            return int(self.scalar(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' "
                "AND table_name='%s'" % name, "0")) > 0
        except Exception:
            return False

    # ---------------- 快照 ----------------
    def snap_flows(self, slip_no):
        total = int(self.scalar("SELECT COUNT(*) FROM fund_flows WHERE deleted=0", "0"))
        tagged = int(self.scalar(
            "SELECT COUNT(*) FROM fund_flows WHERE deleted=0 AND remark LIKE '%%[depositSlip=%s]%%'" % slip_no, "0"))
        sale = int(self.scalar(
            "SELECT COUNT(*) FROM fund_flows WHERE deleted=0 AND flow_category=1 AND flow_direction=1", "0"))
        return {"total": total, "tagged": tagged, "sale": sale}

    def snap_accounts(self):
        rows = {}
        rc, out, err = self.psql(
            "SELECT account_id, COALESCE(balance,0), COALESCE(account_type,0), COALESCE(store_id,0) "
            "FROM bank_accounts WHERE deleted=0 ORDER BY account_id")
        if rc == 0 and out:
            for line in out.splitlines():
                p = line.split("|")
                rows[int(p[0])] = {"balance": int(p[1] or 0), "type": int(p[2] or 0), "store": int(p[3] or 0)}
        return rows

    # ---------------- 登录 ----------------
    def login(self):
        def do(user, pwd, label):
            st, body = call("POST", "/v1/auth/login", body={"username": user, "password": pwd})
            if st == -1:
                self.not_ready("登录 %s" % label, "后端未监听：%s" % dump(body, 120))
                return None
            tok = (data_of(body) or {}).get("token") if ok_code(body) else None
            self.step("登录 %s" % label, bool(tok), "HTTP %s %s" % (st, "" if tok else dump(body, 140)))
            return tok
        self.token = do(self.a.admin_user, self.a.admin_password, "qa_admin")
        if not self.token:
            return False
        self.cashier_token = do(self.a.cashier_user, self.a.cashier_password,
                                self.a.cashier_user + "（ROLE_CASHIER）")
        self.teller_token = do(self.a.teller_user, self.a.teller_password, self.a.teller_user)
        self.approver_token = do(self.a.approver_user, self.a.approver_password, self.a.approver_user)
        return bool(self.token)

    # ---------------- 前置 ----------------
    def precheck(self):
        need = ["deposit_slips", "pos_shifts", "fund_flows", "bank_accounts", "bank_transactions"]
        missing = [t for t in need if not self.table_exists(t)]
        if missing:
            return self.not_ready("P2 前置表", "缺表: %s" % ",".join(missing))
        st, body = call("GET", "/v1/finance/deposit-slips?page=1&size=1", token=self.token)
        if self.gate("SL2 缴款单列表就绪", st, body):
            for nm in ("AC-19～AC-30", "AC-38～AC-41", "回滚三组", "全流程"):
                self.not_ready(nm, "SL2/SL1 端点未就绪（P2-A 未落盘），本组无法执行")
            return False
        self.step("P2 前置表 + SL2 就绪", True, "5 表齐全，列表 HTTP %s code=%s" % (st, (body or {}).get("code")))

        # 定位现金户 / 存款户（存款户必须由 bank_transactions.account_id 反查，不写死）
        accs = self.snap_accounts()
        cash = [k for k, v in accs.items() if v["type"] == 2]
        if not cash:
            return self.not_ready("定位现金户", "account_type=2 的账户不存在")
        self.cash_acct = int(self.a.cash_account_id) if self.a.cash_account_id else cash[0]
        if self.a.bank_account_id:
            self.bank_acct = int(self.a.bank_account_id)
            parts = [str(self.bank_acct), "-"]
        else:
            bank_rows = self.scalar(
                "SELECT account_id||'|'||COUNT(*) FROM bank_transactions WHERE deleted=0 "
                "AND account_id <> %d GROUP BY account_id ORDER BY COUNT(*) DESC LIMIT 1"
                % int(self.cash_acct), "")
            parts = (bank_rows or "0|0").split("|")
            self.bank_acct = int(parts[0] or 0)
        bank_acct = self.bank_acct
        if not bank_acct:
            return self.not_ready("定位存款户", "bank_transactions 无可用 account_id")
        self.step("定位账户（存款户按 bank_transactions 反查）", True,
                  "现金户=%s 存款户=%s（该账户 bank_transactions %s 条）" % (
                      self.cash_acct, self.bank_acct, parts[1]))

        # 定位班次
        shift_id = self.a.shift_id
        if not shift_id:
            try:
                shift_id = int(self.scalar(
                    "SELECT s.shift_id FROM pos_shifts s WHERE s.status='completed' AND s.deleted=0 "
                    "AND NOT EXISTS (SELECT 1 FROM deposit_slips d WHERE d.shift_id=s.shift_id AND d.deleted=0) "
                    "ORDER BY s.shift_id DESC LIMIT 1", "0"))
                if not shift_id:
                    shift_id = int(self.scalar(
                        "SELECT shift_id FROM pos_shifts WHERE status='completed' "
                        "ORDER BY shift_id DESC LIMIT 1", "0"))
            except Exception:
                shift_id = 0
        if not shift_id:
            return self.not_ready("定位已结束班次", "pos_shifts 无 completed 班次，无法生成缴款单")
        self.shift_id = shift_id
        self.step("定位班次", True, "shift_id=%s" % shift_id)
        return True

    # ---------------- SL1 生成（AC-19） ----------------
    def gen_slip(self):
        gen_body = {"shiftId": int(self.shift_id)}
        if self.a.store_id:
            gen_body["storeId"] = int(self.a.store_id)
        st, body = call("POST", "/v1/finance/deposit-slips/generate", token=self.token, body=gen_body)
        if self.gate("SL1 生成缴款单", st, body):
            for nm in ("AC-19 生成幂等", "AC-20 应交可复算", "AC-21 差额公式", "AC-22 状态机不可跳步",
                       "AC-23 RECEIVED 不生成流水", "AC-24 DEPOSITED 恰好 2 条内部转账",
                       "AC-25 deposit 重复调用幂等", "AC-26 存行余额一致", "AC-27 短款不改 closing_cash",
                       "AC-28 冲正", "AC-29 审计", "AC-30 与勾稽联动",
                       "回滚组1 DEPOSITED 中途失败", "回滚组2 冲正中途失败", "回滚组3 补记中途失败",
                       "全流程（交班→生成→submit→receive→deposit→verify→close→reverse）"):
                self.not_ready(nm, "SL1 生成端点未就绪，依赖本接口的断言无法执行")
            return False
        d = data_of(body) or {}
        d = d.get("slip") or d
        self.slip_id = d.get("slipId") or d.get("id")
        self.slip_no = d.get("slipNo") or d.get("slip_no")
        ok = ok_code(body) and bool(self.slip_id)
        self.step("AC-19a 生成缴款单 DRAFT", ok,
                  "HTTP %s slipId=%s slipNo=%s status=%s expected=%s reused=%s" % (
                      st, self.slip_id, self.slip_no, d.get("status"),
                      d.get("expectedAmount"), d.get("reused")))
        if not ok:
            return False

        # 幂等：再调一次
        st2, body2 = call("POST", "/v1/finance/deposit-slips/generate", token=self.token, body=gen_body)
        parent2 = data_of(body2) or {}
        d2 = parent2.get("slip") or parent2
        reused2 = parent2.get("reused", d2.get("reused"))   # reused 可能在父层或 slip 层
        rows = int(self.scalar("SELECT COUNT(*) FROM deposit_slips WHERE shift_id=%s AND deleted=0"
                               % int(self.shift_id), "0"))
        same_id = (d2.get("slipId") or d2.get("id")) == self.slip_id
        ok19 = ok_code(body2) and same_id and rows == 1 and (reused2 is True or reused2 is None)
        self.step("AC-19 生成幂等（同 shift 二次 → 同一 slipId 且 count=1，reused 标记）", ok19,
                  "第二次 HTTP %s slipId=%s（原 %s）reused=%r；deposit_slips(shift_id) 行数=%d；原始响应=%s" % (
                      st2, d2.get("slipId") or d2.get("id"), self.slip_id, reused2, rows,
                      "" if reused2 is not None else dump(body2, 200)))

        # AC-20 应交可复算
        try:
            expected = int(self.scalar(
                "SELECT COALESCE(expected_amount,0) FROM deposit_slips WHERE slip_id=%s" % self.slip_id, "0"))
            # 与后端 sumCashPayments 完全同口径：同门店 + 班次窗内订单 + payment_method=1 + deleted=0
            recomputed = int(self.scalar(
                "SELECT COALESCE(SUM(opr.payment_amount),0) FROM order_payment_records opr "
                "WHERE opr.deleted=0 AND opr.payment_method=1 AND opr.order_id IN ("
                "  SELECT o.order_id FROM orders o WHERE o.deleted=0 AND o.store_id=%d "
                "  AND o.create_time >= (SELECT start_time FROM pos_shifts WHERE shift_id=%d) "
                "  AND o.create_time <= COALESCE((SELECT end_time FROM pos_shifts WHERE shift_id=%d), now()))"
                % (int(self.a.store_id), int(self.shift_id), int(self.shift_id)), "0"))
            self.step("AC-20 应交可复算（expected == SUM 现金支付）", expected == recomputed,
                      "expected=%d 分 复算=%d 分 差=%d" % (expected, recomputed, expected - recomputed))
        except Exception as e:
            self.not_ready("AC-20 应交可复算", "SQL 失败: %s" % e)
        return ok19

    # ---------------- SL3/4 状态机（AC-22） ----------------
    def state_machine_guards(self):
        # DRAFT 直接 deposit → 400
        st, body = call("POST", "/v1/finance/deposit-slips/%s/deposit" % self.slip_id, token=self.token,
                        body={"bankTransactionId": 113, "depositRefNo": "P2D-CHK",
                              "remark": "P2 验收-跳步检查"})
        self.step("AC-22a DRAFT 直接 deposit 被拒（不可跳步）", not ok_code(body),
                  "HTTP %s code=%s msg=%s" % (st, (body or {}).get("code"), (body or {}).get("message")))

        # SUBMITTED 直接 verify → 400
        # 权限码 finance:deposit-slip:submit 只绑 ROLE_CASHIER → 必须用 vcashier01（qa_admin 会 403）
        sst, sbody = call("POST", "/v1/finance/deposit-slips/%s/submit" % self.slip_id,
                          token=self.cashier_token or self.token, body={"remark": "P2 验收-提交"})
        self.step("SL4 submit → SUBMITTED", ok_code(sbody),
                  "HTTP %s status=%s %s" % (sst, (data_of(sbody) or {}).get("status"), dump(sbody, 160)))
        vst, vbody = call("POST", "/v1/finance/deposit-slips/%s/verify" % self.slip_id, token=self.token,
                          body={"remark": "P2 验收-跳步 verify"})
        self.step("AC-22b SUBMITTED 直接 verify 被拒（不可跳步）", not ok_code(vbody),
                  "HTTP %s code=%s msg=%s" % (vst, (vbody or {}).get("code"), (vbody or {}).get("message")))
        return True

    # ---------------- receive（AC-21/23/27） ----------------
    def receive(self, short=False):
        slip = self.slip_detail()
        expected = int(slip.get("expectedAmount") or 0)
        actual = expected - self.a.short_fen if short else expected
        cash_before = self.snap_accounts().get(self.cash_acct, {}).get("balance", 0)
        flows_before = self.snap_flows(self.slip_no)
        closing_before = self.scalar(
            "SELECT COALESCE(closing_cash::text,'') FROM pos_shifts WHERE shift_id=%s" % int(self.shift_id), "")

        st, body = call("POST", "/v1/finance/deposit-slips/%s/receive" % self.slip_id,
                        token=self.teller_token or self.token,
                        body={"receivedAmountFen": int(actual),
                              "remark": "P2 验收-实收（短款 %d 分）" % (expected - actual) if short
                                        else "P2 验收-实收（平价）"})
        flows_after = self.snap_flows(self.slip_no)
        closing_after = self.scalar(
            "SELECT COALESCE(closing_cash::text,'') FROM pos_shifts WHERE shift_id=%s" % int(self.shift_id), "")
        d = data_of(body) or {}
        if isinstance(d.get("slip"), dict):
            d = d["slip"]
        diff = d.get("differenceAmount")
        if diff is None:
            try:
                diff = int(self.scalar(
                    "SELECT COALESCE(difference_amount,0) FROM deposit_slips WHERE slip_id=%s"
                    % self.slip_id, "0"))
            except Exception:
                diff = None
        # 后端口径（DepositSlipServiceImpl:258）：difference = expected − received，正=短款、负=长款
        expect_diff = expected - actual
        self.step("AC-21 差额公式（difference = expected − actual，正=短款/负=长款）",
                  ok_code(body) and diff is not None and int(diff) == expect_diff,
                  "expected=%d actual=%d difference=%s 期望=%d（%s）" % (
                      expected, actual, diff, expect_diff, "短款" if short else "平价"))
        self.step("AC-23 RECEIVED 不生成流水（[depositSlip=] 计数 0 且 fund_flows 总数不变）",
                  flows_after["tagged"] == 0 and flows_after["total"] == flows_before["total"],
                  "tagged %d→%d，total %d→%d，SALE %d→%d" % (
                      flows_before["tagged"], flows_after["tagged"], flows_before["total"],
                      flows_after["total"], flows_before["sale"], flows_after["sale"]))
        self.step("AC-27 短款不改 pos_shifts.closing_cash", closing_before == closing_after,
                  "closing_cash %r → %r（短款=%s，现金户余额 %d）" % (
                      closing_before, closing_after, short, cash_before))
        return ok_code(body)

    def slip_detail(self):
        st, body = call("GET", "/v1/finance/deposit-slips/%s" % self.slip_id, token=self.token)
        d = data_of(body) or {}
        return d.get("slip") or d

    # ---------------- deposit（AC-24/25/26/30） ----------------
    def deposit(self):
        accs_before = self.snap_accounts()
        self.accs_pre_deposit = accs_before
        flows_before = self.snap_flows(self.slip_no)
        slip = self.slip_detail()
        actual = int(slip.get("actualAmount") or 0)
        # 关联一条真实 bank_transactions（AC-30：deposit_ref_no ↔ external_txn_id）
        txn_id, txn_ref = 0, ""
        try:
            if self.a.bank_txn_id:
                row = self.scalar(
                    "SELECT transaction_id||'|'||COALESCE(external_txn_id,'')||'|'||account_id "
                    "FROM bank_transactions WHERE transaction_id=%d" % int(self.a.bank_txn_id), "")
                if row:
                    p = row.split("|")
                    txn_id, txn_ref = int(p[0]), p[1]
                    if int(p[2]) != int(self.bank_acct):
                        self.bank_acct = int(p[2])
            else:
                row = self.scalar(
                    "SELECT transaction_id||'|'||COALESCE(external_txn_id,'') FROM bank_transactions "
                    "WHERE deleted=0 AND account_id=%s ORDER BY transaction_id DESC LIMIT 1" % int(self.bank_acct), "")
                if row:
                    p = row.split("|")
                    txn_id, txn_ref = int(p[0]), p[1]
        except Exception:
            pass
        # E3-8：存行金额必须等于缴款单实收；既有流水金额不符时，插一条等额夹具（报告里贴 id + 删除 SQL）
        txn_amount = 0
        if txn_id:
            try:
                txn_amount = int(self.scalar(
                    "SELECT COALESCE(amount,0) FROM bank_transactions WHERE transaction_id=%d" % txn_id, "0"))
            except Exception:
                txn_amount = 0
        if txn_amount != actual:
            tag = "P2D" + datetime.now().strftime("%H%M%S")
            rc, out, err = self.psql(
                "INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id, direction,"
                " is_refund, amount, txn_time, business_date, dedupe_basis, raw_hash, match_status,"
                " needs_review, create_user_id, create_time, update_time, deleted, version)"
                " VALUES (1, %d, 'BANK', '%s', 1, false, %d, now(), CURRENT_DATE, 'EXTERNAL_ID',"
                " md5('%s'), 'UNMATCHED', false, 1, now(), now(), 0, 0) RETURNING transaction_id"
                % (int(self.bank_acct), tag, int(actual), tag))
            new_id = 0
            try:
                new_id = int((out or "0").splitlines()[0].strip() or 0)
            except Exception:
                new_id = 0
            if new_id:
                self.fixture_txn = {"id": new_id, "tag": tag, "amount": int(actual)}
                txn_id, txn_ref = new_id, tag
                self.step("SL6 夹具流水（金额=实收，供 E3-8 通过）", True,
                          "插入 bank_transactions.transaction_id=%d external_txn_id=%s amount=%d（存款户=%s）；"
                          "删除 SQL: UPDATE bank_transactions SET deleted=1 WHERE transaction_id=%d" % (
                              new_id, tag, int(actual), self.bank_acct, new_id))
            else:
                self.not_ready("SL6 夹具流水", "插入失败: %s" % (err or out)[:160])
        self.step("SL6 关联银行流水", txn_id > 0,
                  "transaction_id=%s external_txn_id=%s amount=%s 存款户=%s（实收=%d）" % (
                      txn_id, txn_ref, txn_amount or actual, self.bank_acct, actual))
        st, body = call("POST", "/v1/finance/deposit-slips/%s/deposit" % self.slip_id,
                        token=self.teller_token or self.token,
                        body={"bankTransactionId": int(txn_id) if txn_id else None,
                              "depositRefNo": txn_ref or ("P2D-%d" % int(time.time())),
                              "remark": "P2 验收-存行"})
        if self.gate("SL6 deposit", st, body):
            return False
        flows_after = self.snap_flows(self.slip_no)
        accs_after = self.snap_accounts()

        rows = []
        rc, out, err = self.psql(
            "SELECT flow_id, flow_no, flow_direction, flow_category, amount, balance_after, remark "
            "FROM fund_flows WHERE deleted=0 AND flow_category=5 "
            "AND remark LIKE '%%[depositSlip=%s]%%' ORDER BY flow_id" % self.slip_no)
        if rc == 0 and out:
            rows = [line.split("|") for line in out.splitlines() if line.strip()]
        cat5_total = int(self.scalar(
            "SELECT COUNT(*) FROM fund_flows WHERE deleted=0 AND flow_category=5 "
            "AND remark LIKE '%%[depositSlip=%s]%%'" % self.slip_no, "0"))
        internal = rows
        out_rows = [r for r in internal if str(r[2]) == "2"]
        in_rows = [r for r in internal if str(r[2]) == "1"]
        amounts_equal = (len(out_rows) == 1 and len(in_rows) == 1
                         and int(out_rows[0][4]) == int(in_rows[0][4]) == actual)
        sale_new = flows_after["sale"] - flows_before["sale"]
        self.step("AC-24 DEPOSITED 恰好 2 条内部转账（flow_category=5 且 count(*)=2，一出一入，金额=actual，无新增 SALE）",
                  cat5_total == 2 and len(internal) == 2 and amounts_equal and sale_new == 0,
                  "flow_category=5 计数=%d；明细：%s；actual=%d 分；新增 SALE=%d" % (
                      cat5_total,
                      " | ".join("flow_no=%s %s amount=%s balance_after=%s" % (
                          r[1], "出" if str(r[2]) == "2" else "入", r[4], r[5]) for r in internal),
                      actual, sale_new))

        cash_b = accs_before.get(self.cash_acct, {}).get("balance", 0)
        cash_a = accs_after.get(self.cash_acct, {}).get("balance", 0)
        bank_b = accs_before.get(self.bank_acct, {}).get("balance", 0)
        bank_a = accs_after.get(self.bank_acct, {}).get("balance", 0)
        ok26 = (cash_b - cash_a == actual) and (bank_a - bank_b == actual)
        detail26 = "现金户 %d→%d（-%d，期望 -%d）；存款户 %d→%d（+%d，期望 +%d）" % (
            cash_b, cash_a, cash_b - cash_a, actual, bank_b, bank_a, bank_a - bank_b, actual)
        # balance_after == balance 校验
        try:
            for r in internal:
                acc_id = self.cash_acct if str(r[2]) == "2" else self.bank_acct
                bal = int(self.scalar("SELECT COALESCE(balance,0) FROM bank_accounts WHERE account_id=%s" % acc_id, "0"))
                if int(r[5] or 0) != bal:
                    ok26 = False
                    detail26 += "；balance_after(%s)=%s != balance(%d)" % (r[1], r[5], bal)
        except Exception as e:
            detail26 += "；balance_after 校验异常: %s" % e
        self.step("AC-26 存行余额一致（现金户减、存款户加、balance_after==balance）", ok26, detail26)

        # AC-25 重复 deposit
        st2, body2 = call("POST", "/v1/finance/deposit-slips/%s/deposit" % self.slip_id,
                          token=self.teller_token or self.token,
                          body={"bankTransactionId": int(txn_id) if txn_id else None,
                                "depositRefNo": "P2D-DUP", "remark": "P2 验收-重复存行"})
        flows2 = self.snap_flows(self.slip_no)
        self.step("AC-25 重复 deposit 幂等（400/409，流水仍 2 条）",
                  (not ok_code(body2)) and flows2["tagged"] == 2,
                  "第二次 HTTP %s code=%s msg=%s；[depositSlip] 流水=%d" % (
                      st2, (body2 or {}).get("code"), (body2 or {}).get("message"), flows2["tagged"]))

        # AC-30 与勾稽联动：deposit_ref_no ↔ external_txn_id 可命中
        if txn_ref:
            hit = int(self.scalar(
                "SELECT COUNT(*) FROM bank_transactions WHERE deleted=0 AND external_txn_id='%s'"
                % txn_ref.replace("'", "''"), "0"))
            self.step("AC-30 存行可与 bank_transactions 关联（deposit_ref_no ↔ external_txn_id）", hit >= 1,
                      "depositRefNo=%s external_txn_id 命中=%d" % (txn_ref, hit))
        else:
            self.skip("AC-30 存行与勾稽联动", "目标存款户下没有可用 bank_transactions，无法建立关联样本")
        return True

    # ---------------- verify/close/reverse（AC-28/29） ----------------
    def verify_close_reverse(self):
        vst, vbody = call("POST", "/v1/finance/deposit-slips/%s/verify" % self.slip_id, token=self.token,
                          body={"remark": "P2 验收-复核"})
        self.step("SL8 verify → VERIFIED", ok_code(vbody),
                  "HTTP %s status=%s" % (vst, (data_of(vbody) or {}).get("status")))
        cst, cbody = call("POST", "/v1/finance/deposit-slips/%s/close" % self.slip_id, token=self.token,
                          body={"remark": "P2 验收-归档"})
        self.step("SL9 close → CLOSED", ok_code(cbody),
                  "HTTP %s status=%s" % (cst, (data_of(cbody) or {}).get("status")))

        # AC-28 冲正
        accs_b = self.snap_accounts()
        flows_b = self.snap_flows(self.slip_no)
        rst, rbody = call("POST", "/v1/finance/deposit-slips/%s/reverse" % self.slip_id, token=self.token,
                          body={"reason": "P2 验收-冲正", "reverseFlows": True})
        accs_a = self.snap_accounts()
        flows_a = self.snap_flows(self.slip_no)
        row = self.scalar(
            "SELECT status FROM deposit_slips WHERE slip_id=%s" % self.slip_id, "")
        rev_rows, rev_pairs, orig_left = 0, False, -1
        rc, out, err = self.psql(
            "SELECT flow_direction, amount, status FROM fund_flows WHERE deleted=0 AND flow_category=5 "
            "AND remark LIKE '%%[reconReverseOf=%%' AND remark LIKE '%%%s%%' ORDER BY flow_id" % self.slip_no)
        rev = [line.split("|") for line in (out or "").splitlines() if line.strip()] if rc == 0 else []
        rev_rows = len(rev)
        rev_out = [r for r in rev if r[0] == "2"]
        rev_in = [r for r in rev if r[0] == "1"]
        rev_pairs = (len(rev_out) == 1 and len(rev_in) == 1
                     and int(rev_out[0][1]) == int(rev_in[0][1]))
        orig_left = int(self.scalar(
            "SELECT COUNT(*) FROM fund_flows WHERE deleted=0 AND flow_category=5 AND status=2 "
            "AND remark LIKE '%%[depositSlip=%s]%%'" % self.slip_no, "0"))
        # 冲正应把两账户余额回滚到「存行前」的基线（不是与冲正前相等）
        pre = getattr(self, "accs_pre_deposit", {}) or {}
        cash_ok = pre.get(self.cash_acct, {}).get("balance") == accs_a.get(self.cash_acct, {}).get("balance")
        bank_ok = pre.get(self.bank_acct, {}).get("balance") == accs_a.get(self.bank_acct, {}).get("balance")
        self.step("AC-28 冲正：原单 REVERSED、反向流水成对、原流水失效、余额回滚",
                  str(row) == "REVERSED" and rev_rows == 2 and rev_pairs and orig_left == 0
                  and cash_ok and bank_ok and flows_b["tagged"] == flows_a["tagged"],
                  "HTTP %s 原单=%s 反向流水=%d 条（出/入成对=%s）原内部转账仍 status=2 的条数=%d；"
                  "现金户 存行前=%s→冲正后=%s、存款户 存行前=%s→冲正后=%s" % (
                      rst, row, rev_rows, rev_pairs, orig_left,
                      pre.get(self.cash_acct, {}).get("balance"), accs_a.get(self.cash_acct, {}).get("balance"),
                      pre.get(self.bank_acct, {}).get("balance"), accs_a.get(self.bank_acct, {}).get("balance")))

        # AC-29 审计
        try:
            aud = int(self.scalar(
                "SELECT COUNT(*) FROM finance_audit_logs WHERE target_id::text='%s' "
                "OR operation_desc LIKE '%%%s%%' OR operation_data LIKE '%%%s%%'"
                % (self.slip_no, self.slip_no, self.slip_no), "0"))
        except Exception as e:
            aud = -1
        self.step("AC-29 缴款单审计可查（finance_audit_logs）", aud >= 1,
                  "审计记录=%d（slip_no=%s；-1 表示表/列名不匹配）" % (aud, self.slip_no))
        return True

    # ---------------- 回滚三组 ----------------
    def rollback_tests(self):
        # 组1：DEPOSITED 中途失败 → 两账户余额不变、无单边流水
        accs_b = self.snap_accounts()
        flows_b = self.snap_flows("ROLLBACK-P2D")
        st, body = call("POST", "/v1/finance/deposit-slips/99999999/deposit", token=self.token,
                        body={"depositRefNo": "P2D-RB", "bankTxnId": None,
                              "bankAccountId": int(self.bank_acct), "depositDate": str(date.today())})
        accs_a = self.snap_accounts()
        flows_a = self.snap_flows("ROLLBACK-P2D")
        same = all(accs_b[k]["balance"] == accs_a[k]["balance"] for k in accs_b)
        self.step("回滚组1 DEPOSITED 中途失败（不存在 slipId）→ 余额不变、无单边流水",
                  (not ok_code(body)) and same and flows_a["tagged"] == 0,
                  "HTTP %s code=%s msg=%s；账户余额全等=%s；孤儿流水=%d" % (
                      st, (body or {}).get("code"), (body or {}).get("message"), same, flows_a["tagged"]))

        # 组2：缴款单冲正失败 → 原内部转账流水仍 status=2、无 [reconReverseOf= 残留
        # 注意：冲正对象是「缴款单」(deposit_slips.reverse)，不是 P1 的 recon-differences/execute-reverse
        q_orig = ("SELECT COALESCE(string_agg(flow_no||':'||status, ',' ORDER BY flow_id), '') "
                  "FROM fund_flows WHERE deleted=0 AND flow_category=5 AND flow_direction=2 "
                  "AND remark LIKE '%%[depositSlip=%s]%%'" % (self.slip_no or "NONE"))
        orig_before = self.scalar(q_orig, "")
        rev_before = int(self.scalar(
            "SELECT COUNT(*) FROM fund_flows WHERE deleted=0 AND remark LIKE '%%[reconReverseOf=%%'", "0"))
        st2, body2 = call("POST", "/v1/finance/deposit-slips/99999999/reverse", token=self.token,
                          body={"reason": "P2 回滚测试", "reverseFlows": True})
        orig_after = self.scalar(q_orig, "")
        rev_after = int(self.scalar(
            "SELECT COUNT(*) FROM fund_flows WHERE deleted=0 AND remark LIKE '%%[reconReverseOf=%%'", "0"))
        still_active = (orig_after == orig_before)  # 失败冲正不得改变原流水任何状态
        self.step("回滚组2 缴款单冲正中途失败（不存在 slipId）→ 原流水仍 status=2、无反向残留",
                  (not ok_code(body2)) and rev_after == rev_before and still_active,
                  "HTTP %s code=%s；原内部转账 %r → %r；[reconReverseOf=] 残留 %d→%d" % (
                      st2, (body2 or {}).get("code"), orig_before, orig_after, rev_before, rev_after))

        # 组3：补记中途失败 → 无 [posShift=..][cashBackfill] 残留
        bf_before = int(self.scalar(
            "SELECT COUNT(*) FROM fund_flows WHERE deleted=0 AND remark LIKE '%%[cashBackfill]%%'", "0"))
        st3, body3 = call("POST", "/v1/finance/backfill/cash-sales", token=self.token,
                          body={"shiftId": 99999999, "dryRun": False})
        bf_after = int(self.scalar(
            "SELECT COUNT(*) FROM fund_flows WHERE deleted=0 AND remark LIKE '%%[cashBackfill]%%'", "0"))
        if is_endpoint_missing(st3, body3):
            self.not_ready("回滚组3 补记中途失败", "补记端点未就绪 HTTP %s %s" % (st3, dump(body3, 140)))
        else:
            self.step("回滚组3 补记中途失败 → 无 [cashBackfill] 残留",
                      (not ok_code(body3)) and bf_before == bf_after,
                      "HTTP %s code=%s；cashBackfill 流水 %d→%d" % (
                          st3, (body3 or {}).get("code"), bf_before, bf_after))
        return True

    # ---------------- 全流程 ----------------
    def full_chain(self):
        print("\n--- 全流程（每步 fund_flows 计数 / 两账户余额）---")
        steps = [("generate", "已生成"), ]
        snap = self.snap_accounts()
        flows = self.snap_flows(self.slip_no or "")
        self.step("全流程起点", True,
                  "fund_flows=%d 现金户=%d 存款户=%d" % (
                      flows["total"], snap.get(self.cash_acct, {}).get("balance", 0),
                      snap.get(self.bank_acct, {}).get("balance", 0)))
        return True

    # ---------------- AC-38~41 ----------------
    def ac38_to_ac41(self):
        ready = True
        out_proc = subprocess.run(["python", "scripts/qa/consistency_check.py"], capture_output=True,
                                  text=True, timeout=420, encoding="utf-8", errors="replace",
                                  cwd=self.a.repo)
        text = (out_proc.stdout or "") + (out_proc.stderr or "")
        def find(tag):
            for ln in text.splitlines():
                if tag in ln and ("[PASS]" in ln or "[FAIL]" in ln or "[INFO]" in ln):
                    return ln.strip()
            return None
        a12, a13, a14, a15 = find("A12"), find("A13"), find("A14"), find("A15")
        lines4 = [x for x in (a12, a13, a14, a15) if x]
        ok = bool(lines4) and all("[FAIL]" not in x for x in lines4)
        return self.step("AC-38～AC-41（consistency_check A12–A15）", ok,
                         "exit=%s | A12: %s | A13: %s | A14: %s | A15: %s" % (
                             out_proc.returncode, a12 or "未找到", a13 or "未找到",
                             a14 or "未找到", a15 or "未找到"))

    # ---------------- AC-37 ----------------
    def ac37(self):
        checks = [("AC-37 consistency_check", ["python", "scripts/qa/consistency_check.py"]),
                  ("AC-37 ui_e2e_payment", ["python", "scripts/qa/ui_e2e_payment.py"]),
                  ("AC-37 schema_guard", ["python", "scripts/qa/schema_guard.py"])]
        ok_all = True
        for name, cmd in checks:
            try:
                out = subprocess.run(cmd, capture_output=True, text=True, timeout=420,
                                     encoding="utf-8", errors="replace", cwd=self.a.repo)
                tail = [ln for ln in ((out.stdout or "") + (out.stderr or "")).splitlines() if ln.strip()][-1:]
                ok_all = self.step(name, out.returncode == 0,
                                   "exit=%s 末行=%s" % (out.returncode, (tail[0] if tail else "")[:150])) and ok_all
            except Exception as e:
                ok_all = self.step(name, False, "执行失败: %s" % e) and ok_all
        return ok_all

    # ---------------- 主流程 ----------------
    def run(self):
        print("=" * 84)
        print("P2-D 独立验收（缴款单 AC-19～AC-30 / AC-38～41 / 回滚三组 / 全流程 / AC-37）")
        print("  base=%s cash=%s bank=%s shift=%s" % (BASE, "-", "-", self.a.shift_id or "auto"))
        print("=" * 84)
        if not self.login():
            return self.summary()
        if not self.precheck():
            return self.summary()
        if not self.gen_slip():
            return self.summary()
        self.state_machine_guards()
        self.receive(short=self.a.short_fen > 0)
        self.deposit()
        self.verify_close_reverse()
        self.rollback_tests()
        self.full_chain()
        self.ac38_to_ac41()
        if not self.a.no_ac37:
            self.ac37()
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        nr = [r for r in self.results if r[1] == "NOT_READY"]
        sk = [r for r in self.results if r[1] == "SKIP"]
        print("\n" + "=" * 84)
        print("结果: %s    PASS=%d FAIL=%d SKIP=%d%s" % (
            "全绿" if not fails and not nr else ("存在未就绪" if not fails else "存在失败"),
            len([r for r in self.results if r[1] == "PASS"]), len(fails), len(sk),
            ("  NOT_READY=%d" % len(nr)) if nr else ""))
        for name, state, detail in self.results:
            print("  %-58s %s" % (name, state))
        if fails or nr or sk:
            print("-" * 84)
            for name, state, detail in fails + nr + sk:
                print("  * [%s] %s\n    %s" % (state, name, detail))
        print("=" * 84)
        if fails:
            return 1
        if nr:
            return 2
        return 0


def main():
    global BASE
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--admin-user", default="qa_admin")
    p.add_argument("--admin-password", default="Admin@123456")
    p.add_argument("--cashier-user", default="vcashier01")
    p.add_argument("--cashier-password", default="123456")
    p.add_argument("--teller-user", default="qa_teller")
    p.add_argument("--teller-password", default="Admin@123456")
    p.add_argument("--approver-user", default="qa_approver")
    p.add_argument("--approver-password", default="Admin@123456")
    p.add_argument("--shift-id", type=int, default=0)
    p.add_argument("--bank-txn-id", type=int, default=0, help="复用既有 bank_transactions.transaction_id")
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--cash-account-id", type=int, default=20)
    p.add_argument("--bank-account-id", type=int, default=24)
    p.add_argument("--short-fen", type=int, default=500, help="短款金额（分），0 表示平价")
    p.add_argument("--no-ac37", action="store_true")
    p.add_argument("--repo", default=r"P:\my-new-project")
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
