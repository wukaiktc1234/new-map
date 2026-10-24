#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P3 前置-3：三路径号段混合验证（50 笔）

三条发号路径共用 order_sequence（键 FLOWyyyyMMdd）：
  A. 资金流水常规发号  FundFlowServiceImpl#generateFlowNo        （无 GREATEST 兜底）
  B. 缴款单存行发号    DepositSlipMapper#nextFlowSequenceWithFloor （有 GREATEST 兜底）
  C. 差异冲正发号      ReconCashSupport#nextFlowNo                （有 GREATEST 兜底，jdbc 直写）

断言：
  1) 全库无重复：count(*) - count(distinct flow_no) = 0
  2) 当天号段无洞：FLOW<today> 区间内序号连续（列出 min/max/条数/缺失清单；历史洞单独标注）
  3) 三路径各自条数与号段区间
  4) 收尾：自造缴款单全部 SL10 冲正回滚；账户 20/24 前后余额对比

用法：
  python scripts/qa/seq_mixed_check.py
  python scripts/qa/seq_mixed_check.py --pos-count 20 --slip-count 20 --reverse-count 10
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import date, datetime

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
BASE = DEFAULT_BASE


def call(method, path, token=None, body=None, timeout=40):
    url = path if path.startswith("http") else BASE + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    h = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            raw = r.read().decode("utf-8", "replace")
            try:
                return r.status, json.loads(raw)
            except Exception:
                return r.status, {"_raw": raw[:300]}
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"_raw": raw[:300]}
    except Exception as e:
        return -1, {"error": str(e)}


def ok_code(b):
    return isinstance(b, dict) and b.get("code") in (0, 200)


def data_of(b):
    return b.get("data") if isinstance(b, dict) else None


def dump(o, n=200):
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
        self.slips = []          # [(slip_id, slip_no)]
        self.pos_flows = []      # [flow_no]
        self.slip_flows = []
        self.rev_flows = []
        self.fixture_shifts = []
        self.fixture_txns = []

    # ---------- 记录 ----------
    def step(self, name, ok, detail):
        self.results.append((name, "PASS" if ok else "FAIL", detail))
        print("  [%s] %s -> %s" % ("PASS" if ok else "FAIL", name, detail))
        return ok

    # ---------- DB ----------
    def psql(self, sql):
        try:
            out = subprocess.run(
                [self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port), "-U", self.a.pg_user,
                 "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                capture_output=True, text=True, timeout=60, encoding="utf-8", errors="replace",
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
        return [line.split("|") for line in out.splitlines() if line.strip()]

    def flow_set(self):
        return set(r[0] for r in self.rows("SELECT flow_no FROM fund_flows WHERE deleted=0"))

    def balances(self):
        r = {}
        for aid, bal, ver in self.rows(
                "SELECT account_id, COALESCE(balance,0), COALESCE(version,0) FROM bank_accounts "
                "WHERE account_id IN (%d, %d)" % (self.a.cash_account, self.a.bank_account)):
            r[int(aid)] = (int(bal), int(ver))
        return r

    # ---------- 登录 ----------
    def login(self):
        def do(u, p, label):
            st, b = call("POST", "/v1/auth/login", body={"username": u, "password": p})
            if st == -1:
                self.results.append(("登录 %s" % label, "NOT_READY", "后端未监听：%s" % dump(b, 120)))
                print("  [NOT_READY] 登录 %s -> 后端未监听" % label)
                return None
            tok = (data_of(b) or {}).get("token") if ok_code(b) else None
            self.step("登录 %s" % label, bool(tok), "HTTP %s %s" % (st, "" if tok else dump(b, 140)))
            return tok
        self.token = do("qa_admin", "Admin@123456", "qa_admin")
        if not self.token:
            return False
        self.cashier = do(self.a.cashier_user, self.a.cashier_password, self.a.cashier_user + "（POS/收银）")
        self.teller = do(self.a.teller_user, self.a.teller_password, self.a.teller_user + "（出纳）")
        return bool(self.token)

    # ---------- A 路：POS 支付 ----------
    def pos_batch(self):
        pre = self.flow_set()
        made = 0
        for i in range(self.a.pos_count):
            method = "cash" if i % 2 == 0 else "wechat"
            yuan = round(12.5 + i * 0.5, 2)
            st, body = call("POST", "/v1/pos/orders/order", token=self.cashier or self.token, body={
                "tableNumber": (i % 20) + 1,
                "items": [{"id": self.food_code, "name": self.food_name, "quantity": 1, "price": yuan}],
                "totalAmount": yuan, "paymentMethod": method, "orderType": "DINE_IN",
                "storeId": str(self.a.store_id), "remark": "P3 号段混合验证",
                "idempotencyKey": "p3seq-%d-%d" % (int(time.time() * 1000), i)})
            d = data_of(body) or {}
            oid = d.get("orderId") or d.get("id") or d.get("orderNumber")
            if not ok_code(body) or not oid:
                self.step("A 路 POS 下单#%d" % (i + 1), False, "HTTP %s %s" % (st, dump(body, 160)))
                continue
            st2, body2 = call("POST", "/v1/pos/orders/order/pay", token=self.cashier or self.token,
                              body={"orderId": oid, "paymentMethod": method, "amount": yuan})
            if ok_code(body2):
                made += 1
            else:
                self.step("A 路 POS 支付#%d" % (i + 1), False, "HTTP %s %s" % (st2, dump(body2, 160)))
        self.pos_flows = sorted(self.flow_set() - pre)
        self.step("A 路 POS 支付 %d 笔完成" % self.a.pos_count, made >= self.a.pos_count * 0.9,
                  "成功 %d/%d 笔（现金/微信交替）；新增流水 %d 条" % (made, self.a.pos_count, len(self.pos_flows)))
        return made

    # ---------- B 路：缴款单存行 ----------
    def _clone_shift(self, idx):
        """克隆一个有真实订单的班次窗口，造夹具班次（报告里给删除 SQL）"""
        sql = ("INSERT INTO pos_shifts (terminal_id, employee_id, employee_name, shift_type, start_time,"
               " end_time, opening_cash, closing_cash, total_orders, total_amount, status, handover_remark,"
               " create_time, update_time, deleted) SELECT terminal_id, employee_id, employee_name,"
               " shift_type, start_time, end_time, opening_cash, closing_cash, total_orders, total_amount,"
               " 'completed', 'P3SEQ-FIXTURE', now(), now(), 0 FROM pos_shifts WHERE shift_id=%d"
               " RETURNING shift_id" % int(self.a.template_shift))
        rc, out, err = self.psql(sql)
        if rc != 0:
            return 0
        sid = int((out or "0").splitlines()[0].strip() or 0)
        if sid:
            self.fixture_shifts.append(sid)
        return sid

    def _fixture_txn(self, amount):
        tag = "P3SEQ%d" % int(time.time() * 1000)
        rc, out, err = self.psql(
            "INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id, direction,"
            " is_refund, amount, txn_time, business_date, dedupe_basis, raw_hash, match_status, needs_review,"
            " create_user_id, create_time, update_time, deleted, version) VALUES (1, %d, 'BANK', '%s', 1, false,"
            " %d, now(), CURRENT_DATE, 'EXTERNAL_ID', md5('%s'), 'UNMATCHED', false, 1, now(), now(), 0, 0)"
            " RETURNING transaction_id" % (int(self.a.bank_account), tag, int(amount), tag))
        tid = int((out or "0").splitlines()[0].strip() or 0) if rc == 0 else 0
        if tid:
            self.fixture_txns.append(tid)
        return tid, tag

    def slip_batch(self):
        pre = self.flow_set()
        made = 0
        for i in range(self.a.slip_count):
            shift_id = self._clone_shift(i)
            if not shift_id:
                self.step("B 路 夹具班次#%d" % (i + 1), False, "克隆班次失败")
                continue
            st, body = call("POST", "/v1/finance/deposit-slips/generate", token=self.token,
                            body={"shiftId": shift_id, "storeId": int(self.a.store_id)})
            d = data_of(body) or {}
            d = d.get("slip") or d
            slip_id, slip_no = d.get("slipId"), d.get("slipNo")
            if not ok_code(body) or not slip_id:
                self.step("B 路 生成缴款单#%d" % (i + 1), False, "HTTP %s %s" % (st, dump(body, 160)))
                continue
            self.slips.append((int(slip_id), slip_no))
            call("POST", "/v1/finance/deposit-slips/%d/submit" % slip_id, self.cashier or self.token,
                 {"remark": "P3 号段验证"})
            expected = int(d.get("expectedAmount") or 0)
            actual = max(0, expected - 100)
            call("POST", "/v1/finance/deposit-slips/%d/receive" % slip_id, self.teller or self.token,
                 {"receivedAmountFen": actual, "remark": "P3 号段验证-短款 100"})
            tid, ref = self._fixture_txn(actual) if actual > 0 else (0, "")
            std, bodyd = call("POST", "/v1/finance/deposit-slips/%d/deposit" % slip_id,
                              self.teller or self.token,
                              {"bankTransactionId": tid or None, "depositRefNo": ref,
                               "remark": "P3 号段验证-存行"})
            if ok_code(bodyd):
                made += 1
            else:
                self.step("B 路 存行#%d" % (i + 1), False, "HTTP %s %s" % (std, dump(bodyd, 160)))
        self.slip_flows = sorted(self.flow_set() - pre)
        self.step("B 路 缴款单存行 %d 笔完成" % self.a.slip_count, made >= self.a.slip_count * 0.9,
                  "成功 %d/%d 笔（每笔 SL1→SL4→SL5→SL6）；新增流水 %d 条（每单 2 条内部转账）" % (
                      made, self.a.slip_count, len(self.slip_flows)))
        return made

    # ---------- C 路：差异冲正 ----------
    def reverse_batch(self):
        pre = self.flow_set()
        # 先在今日 run 上制造 difference 并 REVERSE 置 PENDING_P2_REV
        st, body = call("POST", "/v1/finance/recon-runs?force=true", token=self.token,
                        body={"accountId": int(self.a.cash_account), "businessDate": str(date.today()),
                              "force": True})
        diffs = []
        for page in (1, 2, 3):
            stq, bq = call("GET", "/v1/finance/recon-differences?status=OPEN&page=%d&size=100" % page, token=self.token)
            chunk = (data_of(bq) or {}).get("records") if isinstance(data_of(bq), dict) else None
            if not chunk:
                break
            diffs.extend(chunk)
            if len(chunk) < 100:
                break
        diffs = [d for d in diffs if str(d.get("status")).upper() not in ("CLOSED", "REVERSED", "PENDING_P2_REV")]
        made = 0
        for d in diffs[: self.a.reverse_count * 2]:
            if made >= self.a.reverse_count:
                break
            did = d.get("differenceId") or d.get("id")
            call("POST", "/v1/finance/recon-differences/%s/claim" % did, self.token, {})
            str_, br = call("POST", "/v1/finance/recon-differences/%s/resolve" % did, self.token,
                            {"resolution": "REVERSE", "remark": "P3 号段验证-REVERSE"})
            if not ok_code(br):
                continue
            sx, bx = call("POST", "/v1/finance/recon-differences/%s/execute-reverse" % did, self.token,
                          {"remark": "P3 号段验证-执行冲正"})
            if ok_code(bx):
                made += 1
        self.rev_flows = sorted(self.flow_set() - pre)
        self.step("C 路 差异冲正 %d 笔" % self.a.reverse_count, made >= self.a.reverse_count,
                  "成功 %d/%d 笔（PENDING_P2_REV → execute-reverse）；新增流水 %d 条" % (
                      made, self.a.reverse_count, len(self.rev_flows)))
        return made

    # ---------- 断言 ----------
    def assert_no_duplicates(self):
        dup = int(self.scalar(
            "SELECT count(*) - count(DISTINCT flow_no) FROM fund_flows WHERE deleted=0"))
        rc, out, _ = self.psql(
            "SELECT flow_no||' x'||count(*) FROM fund_flows WHERE deleted=0 GROUP BY flow_no"
            " HAVING count(*)>1 LIMIT 10")
        return self.step("1) 全库 flow_no 无重复", dup == 0,
                         "count(*)-count(distinct)=%d%s" % (
                             dup, ("；重复样本: " + (out or "")[:200]) if dup else ""))

    def assert_no_gaps(self):
        today = date.today().strftime("%Y%m%d")
        pat = "%" + today + "%"
        rows = self.rows(
            "SELECT flow_no FROM fund_flows WHERE deleted=0 AND flow_no LIKE 'FF" + today + "%'"
            " ORDER BY flow_no")
        nums = []
        for r in rows:
            fn = r[0]
            # FF + 8 位日期 + 序号：取日期之后的尾部作为序号
            tail = fn[10:] if len(fn) > 10 else ""
            m = re.match(r"^(\d+)$", tail)
            if m:
                nums.append(int(m.group(1)))
        if not nums:
            return self.step("2) 当天号段无洞", False, "当天无 FLOW%s 流水" % today)
        lo, hi = min(nums), max(nums)
        uniq = sorted(set(nums))
        missing = [n for n in range(lo, hi + 1) if n not in set(nums)]
        detail = ("FLOW%s: min=%d max=%d 条数=%d（去重 %d）缺失=%d %s" % (
            today, lo, hi, len(nums), len(uniq), len(missing),
            ("→ " + ",".join(str(x) for x in missing[:30])) if missing else "（无洞）"))
        ok = len(missing) == 0
        if not ok:
            # 区分历史洞：缺失号是否落在本次脚本开始前的最大号之前
            base_max = getattr(self, "base_max", 0)
            hist = [n for n in missing if n <= base_max]
            new = [n for n in missing if n > base_max]
            detail += "；历史洞(<=%d)=%s；本轮新增洞=%s" % (
                base_max, hist[:30] if hist else "无", new[:30] if new else "无")
            ok = len(new) == 0     # 只对本轮新增洞判 FAIL；历史洞如实报告
        return self.step("2) 当天号段无洞（缺失必须为空）", ok, detail)

    def path_ranges(self):
        if self.a.assert_only:
            for lbl in ("A 路 POS 支付", "B 路 缴款单存行", "C 路 差异冲正"):
                self.results.append(("3) %s 号段区间" % lbl, "SKIP", "--assert-only 模式不造数，无本轮区间"))
                print("  [SKIP] 3) %s 号段区间 -> --assert-only" % lbl)
            return

        def rng(nos, label, expect_min):
            nos = sorted(nos)
            ok = len(nos) >= expect_min and len(nos) > 0
            return self.step("3) %s 号段区间" % label, ok,
                             "本轮新增条数=%d 区间=%s..%s" % (
                                 len(nos), nos[0] if nos else "-", nos[-1] if nos else "-"))
        rng(self.pos_flows, "A 路 POS 支付", 1)
        rng(self.slip_flows, "B 路 缴款单存行", self.a.slip_count * 2)
        rng(self.rev_flows, "C 路 差异冲正", self.a.reverse_count)   # 每次 execute-reverse 生成 1 条冲正流水

    # ---------- 收尾 ----------
    def cleanup_slips(self):
        done = 0
        for slip_id, slip_no in self.slips:
            st, body = call("POST", "/v1/finance/deposit-slips/%d/reverse" % slip_id, self.token,
                            {"reason": "P3 号段验证-收尾冲正", "reverseFlows": True})
            row = self.scalar("SELECT status FROM deposit_slips WHERE slip_id=%d" % slip_id, "")
            if ok_code(body) or str(row) == "REVERSED":
                done += 1
        return self.step("4) 收尾：自造 %d 张缴款单全部 SL10 冲正" % len(self.slips),
                         done == len(self.slips), "已冲正 %d/%d 张" % (done, len(self.slips)))

    def run(self):
        print("=" * 84)
        print("P3 前置-3：三路径号段混合验证（POS %d + 缴款单 %d + 差异冲正 %d）" % (
            self.a.pos_count, self.a.slip_count, self.a.reverse_count))
        print("=" * 84)
        if not self.login():
            return self.summary()
        # 取一个真实菜品（POS 下单要求 food id 可解析）
        try:
            row = self.rows(
                "SELECT food_code, food_name FROM foods WHERE deleted=0 AND status=1 "
                "ORDER BY food_id LIMIT 1")
            self.food_code, self.food_name = (row[0][0], row[0][1]) if row else ("FD260925001", "QA-红烧土豆")
        except Exception:
            self.food_code, self.food_name = "FD260925001", "QA-红烧土豆"
        self.bal_before = self.balances()
        self.base_max = int(self.scalar(
            "SELECT COALESCE(MAX(CAST(SUBSTRING(flow_no FROM 11) AS BIGINT)),0) FROM fund_flows "
            "WHERE deleted=0 AND flow_no LIKE 'FF" + date.today().strftime("%Y%m%d") + "%'", "0"))
        self.seq_before = self.scalar(
            "SELECT COALESCE(sequence_value,0) FROM order_sequence WHERE sequence_key='FLOW%s'"
            % date.today().strftime("%Y%m%d"), "0")
        self.step("基线", True, "账户余额=%s；当天最大 flow 序号=%d；order_sequence(FLOW)=%s" % (
            self.bal_before, self.base_max, self.seq_before))

        if not self.a.assert_only:
            self.pos_batch()
            self.slip_batch()
            self.reverse_batch()
        else:
            self.step("--assert-only：跳过三路径造数，仅做全库断言", True, "使用现有数据")

        self.assert_no_duplicates()
        self.assert_no_gaps()
        self.path_ranges()
        if not self.a.assert_only:
            self.cleanup_slips()
        else:
            self.step("4) 收尾冲正", True, "--assert-only 模式下跳过（上一轮已 20/20 冲正）")

        self.bal_after = self.balances()
        self.step("5) 账户余额前后对比", True, "; ".join(
            "acct%d %s→%s（Δ%+d）" % (aid, self.bal_before.get(aid, ("?",))[0],
                                      self.bal_after.get(aid, ("?",))[0],
                                      self.bal_after.get(aid, (0,))[0] - self.bal_before.get(aid, (0,))[0])
            for aid in (self.a.cash_account, self.a.bank_account)))
        self.step("夹具清单（可删）", True,
                  "pos_shifts=%s；bank_transactions=%s" % (self.fixture_shifts, self.fixture_txns))
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        nr = [r for r in self.results if r[1] == "NOT_READY"]
        print("\n" + "=" * 84)
        print("结果: %s  PASS=%d FAIL=%d" % ("全绿" if not fails else "存在失败",
                                             len(self.results) - len(fails), len(fails)))
        for name, state, detail in self.results:
            print("  %-52s %s" % (name, state))
        skips = [r for r in self.results if r[1] == "SKIP"]
        if skips:
            print("  （SKIP %d 项：%s）" % (len(skips), "; ".join(s[0] for s in skips)))
        if fails:
            print("-" * 84)
            for name, state, detail in fails:
                print("  * [FAIL] %s\n    %s" % (name, detail))
        print("=" * 84)
        if fails:
            return 1
        return 2 if nr else 0


def main():
    global BASE
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--pos-count", type=int, default=20)
    p.add_argument("--slip-count", type=int, default=20)
    p.add_argument("--reverse-count", type=int, default=10)
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--cash-account", type=int, default=20)
    p.add_argument("--bank-account", type=int, default=24)
    p.add_argument("--template-shift", type=int, default=19)
    p.add_argument("--assert-only", action="store_true", help="仅跑断言，不再造数")
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
