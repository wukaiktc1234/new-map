#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P4-1：三条取号路径**交替混跑**，验证不重号、不交叉

三条路径（每轮按 1→2→3 循环，交替发起，不串行）：
  1) POS 支付（POST /v1/pos/orders/order + /order/pay 现金）→ SALE 流水，
     取号走 FundFlowServiceImpl；账号默认 vcashier01（收银员）
  2) 缴款单存行（SL1 generate → submit → SL4 receive → SL5 deposit → SL6 verify）
     → 2 条内部转账，取号走 DepositSlipServiceImpl#nextFlowNo
  3) 差异冲正（POST /v1/finance/recon-differences/{id}/execute-reverse）
     → 取号走 ReconCashSupport#nextFlowNo

断言（逐条打印 PASS/FAIL）：
  A1 全库 flow_no 无重复；本次新增区段内也无重复
  A2 号段不扩大残差：运行前记录 (sequence_value − max(flow_no) 数字部分) 的基线差，
     结束后差值不扩大（历史残洞 BL-5 记为已知残差，不判红但要打印）
  A3 号段增量 == 落库增量
  A4 无 5xx；bank_accounts.balance < 0 计数为 0
  A5 烧号账增量 == 进入业务数 − 落库数
  A6 每条烧号账 reason 非空

限速：全局 90 次/分钟（RateLimitFilter 是 IP+URI 100 次/分钟，超了会被 429 拦在业务外，
拦掉的请求没有取号，不计入烧号口径）。

前置：e2e_setup.ensure_ready()（服务 + 库存）。
夹具：本次创建的 pos_shifts / deposit_slips / bank_transactions 都以 tag 标记，
      结尾按 --keep-fixtures 决定是否清理（默认保留，便于失败复盘；传 --cleanup 才清）。

用法：
  python scripts/qa/seq_mixed_paths_check.py --per-path 20
退出码：0 全绿 / 1 断言失败 / 2 环境未就绪
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
import uuid
from datetime import date, datetime

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
try:
    from e2e_setup import ensure_ready
except Exception as e:  # pragma: no cover
    print("[FATAL] 无法导入 e2e_setup：%s" % e)
    sys.exit(2)

DEFAULT_API = "http://localhost:8081/api"
DEFAULT_ADMIN = "http://localhost:3002"
DEFAULT_POS = "http://localhost:3001"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"


def call(base, method, path, token=None, body=None, timeout=60):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    h = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(base + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode("utf-8", "replace"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"_raw": raw[:200]}
    except Exception as e:
        return -1, {"error": str(e)}


def ok(b):
    return isinstance(b, dict) and b.get("code") in (0, 200)


def data_of(b):
    return b.get("data") if isinstance(b, dict) else None


class Mixed:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.tag = "MIX" + datetime.now().strftime("%m%d%H%M%S")
        self.lock = threading.Lock()
        self.gate = threading.Lock()
        self.window = []
        self.token_admin = None
        self.token_pos = None
        self.token_teller = None
        self.cheap_dish = None
        self.hits = {1: {"attempt": 0, "ok": 0, "err": []},
                     2: {"attempt": 0, "ok": 0, "err": []},
                     3: {"attempt": 0, "ok": 0, "err": []}}
        self.http5xx = 0
        self.stats = {}
        self.fixture_txns = 0        # 本次插入的 bank_transactions 夹具行数
        self._batch_id = 0
        self._user_id = 0

    # ---------- 基础 ----------
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
            raise RuntimeError("psql rc=%s %s" % (o.returncode, (o.stderr or "")[:200]))
        return (o.stdout or "").strip()

    def rows(self, sql):
        out = self.psql(sql)
        return [l.split("|") for l in out.splitlines() if l.strip()]

    def num(self, sql):
        v = self.psql(sql)
        return int(v) if v.strip().isdigit() else 0

    def pace(self):
        """全局 90 次/分钟闸门"""
        while True:
            with self.gate:
                now = time.time()
                while self.window and now - self.window[0] > 60.0:
                    self.window.pop(0)
                if len(self.window) < self.a.max_per_minute:
                    self.window.append(now)
                    return
                wait = 60.0 - (now - self.window[0]) + 0.05
            time.sleep(min(2.0, max(0.05, wait)))

    def req(self, method, path, token, body=None, path_no=None):
        """带限速与 429 退避的请求；path_no 用于统计哪条路进了业务"""
        self.pace()
        st, b = call(self.a.api, method, path, token, body)
        retry = 0
        while st == 429 and retry < 5:
            time.sleep(0.2 * (retry + 1))
            st, b = call(self.a.api, method, path, token, body)
            retry += 1
        with self.lock:
            if st >= 500 or st == -1:
                self.http5xx += 1
            if path_no and st != 429:
                self.hits[path_no]["attempt"] += 1
        return st, b

    # ---------- 登录 ----------
    def login(self):
        st, b = call(self.a.api, "POST", "/v1/auth/login",
                     body={"username": self.a.admin_user, "password": self.a.admin_password})
        self.token_admin = (data_of(b) or {}).get("token") if ok(b) else None
        self.step("登录 %s" % self.a.admin_user, bool(self.token_admin), "HTTP %s" % st)
        time.sleep(1.2)     # /v1/auth/login 有 IP 限流（60s/5 次）
        st2, b2 = call(self.a.api, "POST", "/v1/auth/login",
                       body={"username": self.a.pos_user, "password": self.a.pos_password})
        self.token_pos = (data_of(b2) or {}).get("token") if ok(b2) else None
        self.step("登录 %s" % self.a.pos_user, bool(self.token_pos), "HTTP %s" % st2)
        time.sleep(1.2)
        st3, b3 = call(self.a.api, "POST", "/v1/auth/login",
                       body={"username": self.a.teller_user, "password": self.a.teller_password})
        self.token_teller = (data_of(b3) or {}).get("token") if ok(b3) else None
        self.step("登录 %s（收款人，四眼要求≠复核人）" % self.a.teller_user,
                  bool(self.token_teller), "HTTP %s" % st3)
        return bool(self.token_admin and self.token_pos and self.token_teller)

    # ---------- 夹具 ----------
    def make_shifts(self, n):
        """克隆 n 个班次（pos_shifts 无 store_id；门店由订单 store_id + generate 显式 storeId 决定）"""
        made = []
        base = self.rows("SELECT shift_id, terminal_id, employee_id, employee_name, shift_type,"
                         " opening_cash, closing_cash FROM pos_shifts WHERE deleted=0"
                         " ORDER BY shift_id DESC LIMIT 1")
        if not base:
            self.not_ready("班次夹具", "pos_shifts 里没有可克隆的班次")
            return []
        b = base[0]
        for i in range(n):
            sql = ("INSERT INTO pos_shifts (terminal_id, employee_id, employee_name, shift_type,"
                   " start_time, end_time, opening_cash, closing_cash, total_orders, total_amount,"
                   " status, handover_remark, create_time, update_time, deleted)"
                   " VALUES ('%s','%s','%s','%s', now() - interval '1 day', now() + interval '1 day',"
                   " %d, %d, 0, 0, 'CLOSED', '%s-fix-%d', now(), now(), 0) RETURNING shift_id"
                   % (b[1] or 'T-E2E', b[2] or '0', (b[3] or 'qa_e2e_mixed').replace("'", ""),
                      b[4] or 'MORNING', int(float(b[5] or 0)), int(float(b[6] or 0)), self.tag, i))
            r = self.rows(sql)
            if r:
                made.append(int(r[0][0]))
        self.step("班次夹具就绪", len(made) == n, "创建 %d/%d 个班次（tag=%s）" % (len(made), n, self.tag))
        return made

    def bank_account(self, store_id):
        r = self.rows("SELECT account_id FROM bank_accounts WHERE store_id=%d AND account_type=1"
                      " AND deleted=0 ORDER BY account_id LIMIT 1" % store_id)
        return int(r[0][0]) if r else None

    def make_bank_txn(self, account_id, amount_fen, ref):
        """SL5 需要等额银行流水。

        按 lead 裁定：SQL 直插（等价 I1 落库终态），但必须与 I1 正常行一致：
          source_type='BANK'、fee=0、needs_review=false、match_status='UNMATCHED'，
          external_txn_id 用 MIX... 前缀 tag（便于识别与清理）。
        这些行是**测试夹具、非业务数据**，脚本结尾会打印行数与 tag。
        """
        batch_id = self.fixture_batch()
        user_id = self.fixture_user()
        dedupe = ("MIX" + ref[-10:])[:16]
        raw_hash = ("mix" + str(abs(hash(ref))))[:64]
        sql = ("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id,"
               " direction, is_refund, amount, fee, txn_time, business_date, balance_snapshot,"
               " counterparty_name, summary, dedupe_basis, raw_hash, match_status, needs_review,"
               " create_user_id, create_time, update_time, deleted, version)"
               " VALUES (%d,%d,'BANK','%s',1,false,%d,0, now(), current_date, 0,"
               " 'mix-fixture','mix-fixture','%s','%s','UNMATCHED',false,%d, now(), now(), 0, 0)"
               " RETURNING transaction_id" % (batch_id, account_id, ref[:64], amount_fen, dedupe, raw_hash, user_id))
        r = self.rows(sql)
        with self.lock:
            self.fixture_txns += 1
        return int(r[0][0]) if r else None

    def fixture_batch(self):
        if self._batch_id:
            return self._batch_id
        r = self.rows("SELECT COALESCE(MAX(batch_id), 0) FROM bank_transactions")
        self._batch_id = int(r[0][0]) if r else 0
        return self._batch_id

    def fixture_user(self):
        if self._user_id:
            return self._user_id
        r = self.rows("SELECT user_id FROM users WHERE username='%s' LIMIT 1" % self.a.admin_user)
        self._user_id = int(r[0][0]) if r else 0
        return self._user_id

    # ---------- 路径 1：POS 现金支付 ----------
    def path_pos(self):
        if not self.dish:
            return False, "无可用菜品"
        item = {"productId": str(self.dish["dishId"]), "productName": self.dish.get("dishName") or "",
                "quantity": 1, "price": float(self.dish.get("price") or 0),
                "dishType": self.dish.get("dishType") or "single"}
        st, b = self.req("POST", "/v1/pos/orders/order", self.token_pos,
                         {"orderType": "dinein", "items": [item],
                          "idempotencyKey": "mix-%s" % uuid.uuid4().hex[:12]}, path_no=1)
        if not ok(b):
            return False, "下单 HTTP %s code=%s %s" % (st, (b or {}).get("code"), str((b or {}).get("message"))[:60])
        d = data_of(b) or {}
        oid, amount = d.get("orderId"), round(float(d.get("totalAmount") or 0), 2)
        st2, b2 = self.req("POST", "/v1/pos/orders/order/pay", self.token_pos,
                           {"orderId": oid, "paymentMethod": "现金", "amount": amount}, path_no=1)
        if not ok(b2):
            return False, "支付 HTTP %s code=%s %s" % (st2, (b2 or {}).get("code"), str((b2 or {}).get("message"))[:60])
        return True, "orderId=%s 金额=%.2f" % (oid, amount)

    # ---------- 路径 2：缴款单存行（每轮自洽） ----------
    def path_slip(self, idx):
        """按 lead 定案：每轮自洽
          ① 克隆班次，时间窗 = [现在, 现在+30s]（互不重叠：每轮用各自的起点，前几轮的订单都在 start 之前）
          ② 在窗口内下一笔 POS 现金单（小额）→ 现金户 +金额
          ③ SL1 generate → submit → receive(收款人=teller) → deposit(bank txn=expected) → verify(复核人=admin)
        净现金效果 ≈ 0（+销售 / −存行），不会抽干现金户，也不会重复存同一笔现金。
        """
        from datetime import timedelta
        t0 = datetime.now().replace(microsecond=0)
        t1 = t0 + timedelta(seconds=30)
        shift = self.clone_shift(t0, t1, idx)
        if not shift:
            return False, "班次克隆失败"

        # ② 窗口内小额现金单（用最便宜菜品，避免抽干现金户）
        dish = self.cheap_dish or self.dish
        if not dish:
            return False, "无可用菜品"
        item = {"productId": str(dish["dishId"]), "productName": dish.get("dishName") or "",
                "quantity": 1, "price": float(dish.get("price") or 0),
                "dishType": dish.get("dishType") or "single"}
        st, b = self.req("POST", "/v1/pos/orders/order", self.token_pos,
                         {"orderType": "dinein", "items": [item],
                          "idempotencyKey": "mix-slip-%s" % uuid.uuid4().hex[:12]}, path_no=2)
        if not ok(b):
            return False, "窗口内下单 HTTP %s code=%s %s" % (st, (b or {}).get("code"), str((b or {}).get("message"))[:60])
        d = data_of(b) or {}
        oid, amount = d.get("orderId"), round(float(d.get("totalAmount") or 0), 2)
        st, b = self.req("POST", "/v1/pos/orders/order/pay", self.token_pos,
                         {"orderId": oid, "paymentMethod": "现金", "amount": amount}, path_no=2)
        if not ok(b):
            return False, "窗口内支付 HTTP %s code=%s %s" % (st, (b or {}).get("code"), str((b or {}).get("message"))[:60])

        # ③ 缴款单全链路
        st, b = self.req("POST", "/v1/finance/deposit-slips/generate", self.token_admin,
                         {"shiftId": shift, "storeId": int(self.a.store_id)}, path_no=2)
        if not ok(b):
            return False, "SL1 HTTP %s code=%s %s" % (st, (b or {}).get("code"), str((b or {}).get("message"))[:70])
        d = data_of(b) or {}
        d = d.get("slip") or d
        sid, expected = d.get("slipId"), int(d.get("expectedAmount") or 0)
        if expected <= 0:
            return False, "SL1 预期额=%d（班次窗口内无现金单）" % expected
        self.req("POST", "/v1/finance/deposit-slips/%s/submit" % sid, self.token_pos, {"remark": self.tag}, path_no=2)
        st3, b3 = self.req("POST", "/v1/finance/deposit-slips/%s/receive" % sid, self.token_teller,
                           {"receivedAmountFen": expected, "remark": self.tag}, path_no=2)
        if not ok(b3):
            return False, "SL4 HTTP %s code=%s %s" % (st3, (b3 or {}).get("code"), str((b3 or {}).get("message"))[:70])
        acct = self.bank_account(int(self.a.store_id))
        tid = self.make_bank_txn(acct, expected, "%s-slip-%d" % (self.tag, idx)) if acct else None
        st4, b4 = self.req("POST", "/v1/finance/deposit-slips/%s/deposit" % sid, self.token_admin,
                           {"bankTransactionId": tid, "depositRefNo": "%s-%d" % (self.tag, idx),
                            "remark": self.tag}, path_no=2)
        if not ok(b4):
            return False, "SL5 HTTP %s code=%s %s" % (st4, (b4 or {}).get("code"), str((b4 or {}).get("message"))[:70])
        st5, b5 = self.req("POST", "/v1/finance/deposit-slips/%s/verify" % sid, self.token_admin,
                           {"remark": self.tag}, path_no=2)
        if not ok(b5):
            return False, "SL6/verify HTTP %s code=%s %s" % (st5, (b5 or {}).get("code"), str((b5 or {}).get("message"))[:70])
        return True, "slipId=%s expected=%d orderId=%s" % (sid, expected, oid)

    def clone_shift(self, t0, t1, idx):
        base = self.rows("SELECT terminal_id, employee_id, employee_name, shift_type, opening_cash, closing_cash"
                         " FROM pos_shifts WHERE deleted=0 ORDER BY shift_id DESC LIMIT 1")
        if not base:
            return None
        b = base[0]
        sql = ("INSERT INTO pos_shifts (terminal_id, employee_id, employee_name, shift_type,"
               " start_time, end_time, opening_cash, closing_cash, total_orders, total_amount,"
               " status, handover_remark, create_time, update_time, deleted)"
               " VALUES ('%s','%s','%s','%s','%s','%s',%d,%d,0,0,'CLOSED','%s-fix-%d', now(), now(), 0)"
               " RETURNING shift_id"
               % (b[0] or 'T-E2E', b[1] or '0', (b[2] or 'qa_e2e_mixed').replace("'", ""),
                  b[3] or 'MORNING', t0.strftime("%Y-%m-%d %H:%M:%S"), t1.strftime("%Y-%m-%d %H:%M:%S"),
                  int(float(b[4] or 0)), int(float(b[5] or 0)), self.tag, idx))
        r = self.rows(sql)
        return int(r[0][0]) if r else None

    # ---------- 路径 3：差异冲正 ----------
    def path_reverse(self, idx):
        """每轮**重新取候选**：只取 OPEN + internal_ref 非空 + 原流水未取消（status<>3）。"""
        rows = self.rows(
            "SELECT rd.difference_id FROM recon_differences rd"
            " JOIN fund_flows f ON f.flow_no = rd.internal_ref"
            " WHERE rd.status='OPEN' AND rd.internal_ref IS NOT NULL AND f.status <> 3"
            " ORDER BY rd.difference_id LIMIT 1")
        if not rows:
            return False, "无可用候选（OPEN + 原流水未取消）"
        did = int(rows[0][0])
        st, b = self.req("POST", "/v1/finance/recon-differences/%s/claim" % did,
                         self.token_admin, {"remark": self.tag}, path_no=3)
        if not ok(b) and (b or {}).get("code") != 400:
            return False, "claim differenceId=%s HTTP %s code=%s %s" % (
                did, st, (b or {}).get("code"), str((b or {}).get("message"))[:60])
        st2, b2 = self.req("POST", "/v1/finance/recon-differences/%s/resolve" % did,
                           self.token_admin,
                           {"resolution": "REVERSE", "decision": "REVERSE", "remark": "%s 冲正" % self.tag,
                            "reason": "%s 冲正" % self.tag}, path_no=3)
        if not ok(b2):
            return False, "resolve differenceId=%s HTTP %s code=%s %s" % (
                did, st2, (b2 or {}).get("code"), str((b2 or {}).get("message"))[:70])
        st3, b3 = self.req("POST", "/v1/finance/recon-differences/%s/execute-reverse" % did,
                           self.token_admin, {"remark": self.tag, "reason": self.tag}, path_no=3)
        if not ok(b3):
            return False, "execute-reverse differenceId=%s HTTP %s code=%s %s" % (
                did, st3, (b3 or {}).get("code"), str((b3 or {}).get("message"))[:70])
        return True, "differenceId=%s" % did

    # ---------- 断言 ----------
    def snapshot(self):
        seq_key = "FLOW" + datetime.now().strftime("%Y%m%d")
        return {
            "flows": self.num("SELECT count(*) FROM fund_flows"),
            "burn": self.num("SELECT count(*) FROM fund_flow_no_burn_ledger"),
            "seq": self.num("SELECT COALESCE(max(sequence_value),0) FROM order_sequence WHERE sequence_key='%s'" % seq_key),
            "max_no": self.num("SELECT COALESCE(MAX(CAST(SUBSTRING(flow_no FROM '[0-9]+$') AS BIGINT)),0) FROM fund_flows"),
            "neg": self.num("SELECT count(*) FROM bank_accounts WHERE balance < 0 AND deleted=0"),
            "seq_key": seq_key,
        }

    def assertions(self, before, after, reached, landed):
        okall = True
        # A1
        dup_all = self.num("SELECT count(*) - count(DISTINCT flow_no) FROM fund_flows")
        new_dup = self.num(
            "SELECT count(*)-count(DISTINCT flow_no) FROM fund_flows WHERE flow_no IN"
            " (SELECT flow_no FROM fund_flows ORDER BY flow_id DESC LIMIT %d)" % max(1, landed))
        okall &= self.step("A1 flow_no 无重复（全库 + 本次新增区段）", dup_all == 0 and new_dup == 0,
                           "全库重复=%d；新增区段(%d 行) 重复=%d" % (dup_all, landed, new_dup))
        # A2：号段残差不扩大
        gap_before = before["seq"] - before["max_no"]
        gap_after = after["seq"] - after["max_no"]
        okall &= self.step("A2 号段残差不扩大（历史残洞记为已知）", gap_after <= gap_before,
                           "基线 seq=%d max_no=%d 差=%d → 结束 seq=%d max_no=%d 差=%d" % (
                               before["seq"], before["max_no"], gap_before,
                               after["seq"], after["max_no"], gap_after))
        # A3
        d_seq = after["seq"] - before["seq"]
        d_flow = after["flows"] - before["flows"]
        okall &= self.step("A3 号段增量 == 落库增量", d_seq == d_flow,
                           "号段增量=%d 落库增量=%d" % (d_seq, d_flow))
        # A4
        okall &= self.step("A4 无 5xx 且无负余额", self.http5xx == 0 and after["neg"] == 0,
                           "5xx/连接失败=%d 负余额账户=%d" % (self.http5xx, after["neg"]))
        # A5
        d_burn = after["burn"] - before["burn"]
        d_seq2 = after["seq"] - before["seq"]
        okall &= self.step("A5 烧号账增量 == 号段增量 − 落库增量（取号后未落库的号必须有账）",
                           d_burn == d_seq2 - landed,
                           "烧号增量=%d；号段增量=%d − 落库增量=%d = %d；"
                           "（进入业务=%d，其中 %d 条在取号前就被业务校验拒绝，不产生号）" % (
                               d_burn, d_seq2, landed, d_seq2 - landed, reached, reached - landed - d_burn))
        # A6
        empty_reason = self.num("SELECT count(*) FROM fund_flow_no_burn_ledger WHERE reason IS NULL OR trim(reason)=''")
        okall &= self.step("A6 烧号账 reason 非空", empty_reason == 0, "空 reason 行数=%d" % empty_reason)
        return okall

    # ---------- 主流程 ----------
    def run(self):
        print("=" * 92)
        print("P4-1 三路径混合取号：POS 支付 / 缴款单存行 / 差异冲正（交替 %d 轮）" % self.a.per_path)
        print("  限速 %d 次/分钟；tag=%s" % (self.a.max_per_minute, self.tag))
        print("=" * 92)
        if not ensure_ready(api=self.a.api, admin=self.a.admin, pos=self.a.pos):
            self.not_ready("前置就绪（e2e_setup.ensure_ready）", "服务/库存前置未通过，见上方输出")
            return self.summary()
        if not self.login():
            return self.summary()

        st, b = call(self.a.api, "GET", "/v1/pos/api/dishes", self.token_pos)
        dishes = (data_of(b) or []) if ok(b) else []
        self.dish = next((d for d in dishes if d.get("dishId") and (d.get("price") or 0) > 0), None)
        priced = [d for d in dishes if d.get("dishId") and (d.get("price") or 0) > 0]
        self.cheap_dish = min(priced, key=lambda x: float(x.get("price") or 0)) if priced else None
        self.step("菜品就绪（POS 下单用）", bool(self.dish),
                  "菜品数=%d，取=%s" % (len(dishes), (self.dish or {}).get("dishName")))

        n = self.a.per_path
        # 班次不再预克隆 20 个：路径 2 每轮按需克隆 [now, now+30s] 的独立窗口（互不重叠）
        self.step("班次策略", True, "路径 2 每轮即时克隆窗口 [T, T+30s]，不预建 20 个班次")
        # 路径 3 每轮自行重取候选（OPEN + 原流水未取消），这里只做一次可用量体检
        live_open = self.num(
            "SELECT count(*) FROM recon_differences rd"
            " JOIN fund_flows f ON f.flow_no = rd.internal_ref"
            " WHERE rd.status='OPEN' AND rd.internal_ref IS NOT NULL AND f.status <> 3")
        if live_open < n:
            # 造差异：对本轮现金户/存行户跑一次 force 重建（内部流水多于银行流水才会产生 A/B 类单边差异）
            made = []
            for acct in (self.a.cash_account, self.a.force_account):
                st, b = self.req("POST", "/v1/finance/recon-runs?force=true", self.token_admin,
                                 {"accountId": int(acct), "businessDate": self.a.business_date, "force": True})
                made.append("acct%d:%s" % (acct, "ok" if ok(b) else "code=%s" % (b or {}).get("code")))
            live_open = self.num(
                "SELECT count(*) FROM recon_differences rd"
                " JOIN fund_flows f ON f.flow_no = rd.internal_ref"
                " WHERE rd.status='OPEN' AND rd.internal_ref IS NOT NULL AND f.status <> 3")
            self.step("差异候选补齐（force 重建跑批造单边差异）", True,
                      "force 重建=%s → 可用候选 %d 条（需要 %d）" % (", ".join(made), live_open, n))
        self.step("差异候选体检（每轮重取）", live_open >= n,
                  "可用候选（OPEN + 原流水未取消）= %d 条（需要 %d）%s" % (
                      live_open, n, "" if live_open >= n else "；不足部分会如实记为路径 3 失败"))

        before = self.snapshot()
        print("基线：fund_flows=%d burn=%d seq=%d max_no=%d 负余额=%d" % (
            before["flows"], before["burn"], before["seq"], before["max_no"], before["neg"]))

        t0 = time.time()
        for i in range(n):
            for no, fn in ((1, lambda: self.path_pos()),
                           (2, lambda i=i: self.path_slip(i)),
                           (3, lambda i=i: self.path_reverse(i))):
                if no == 2:
                    time.sleep(1.0)     # 让本轮 POS 单落在新班次窗口起点之后
                okk, msg = fn()
                with self.lock:
                    if not okk:
                        self.hits[no]["err"].append(msg)
                    else:
                        self.hits[no]["ok"] += 1
            print("  第 %2d 轮完成  POS=%d/%d 缴款单=%d/%d 冲正=%d/%d  用时 %.1fs" % (
                i + 1, self.hits[1]["ok"], i + 1, self.hits[2]["ok"], i + 1,
                self.hits[3]["ok"], i + 1, time.time() - t0))

        after = self.snapshot()
        for no, name in ((1, "POS 支付"), (2, "缴款单存行"), (3, "差异冲正")):
            h = self.hits[no]
            self.step("路径 %d %s 完成 %d/%d" % (no, name, h["ok"], n), h["ok"] == n,
                      "成功=%d 尝试=%d 失败样例=%s" % (h["ok"], h["attempt"], h["err"][:3]))

        reached = sum(self.hits[i]["attempt"] for i in (1, 2, 3))
        landed = after["flows"] - before["flows"]
        self.assertions(before, after, reached, landed)

        self.step("夹具流水摘要（测试夹具、非业务数据）", True,
                  "本次 SQL 直插 bank_transactions 共 %d 行，external_txn_id 前缀 tag=%s"
                  "（source_type=BANK / fee=0 / needs_review=false / match_status=UNMATCHED）；"
                  "清库请按该前缀删除" % (self.fixture_txns, self.tag))
        if self.a.cleanup:
            self.cleanup()
        return self.summary()

    def cleanup(self):
        try:
            before = self.num("SELECT count(*) FROM bank_transactions WHERE external_txn_id LIKE '%s%%'" % self.tag)
            self.psql("DELETE FROM bank_transactions WHERE external_txn_id LIKE '%s%%'" % self.tag)
            after = self.num("SELECT count(*) FROM bank_transactions WHERE external_txn_id LIKE '%s%%'" % self.tag)
            self.step("夹具清理", after == 0,
                      "按 tag '%s%%' 删除 bank_transactions：删除前 %d 行 → 删除后 %d 行" % (self.tag, before, after))
        except Exception as e:
            self.step("夹具清理", False, "异常：%s" % str(e)[:120])

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        nr = [r for r in self.results if r[1] == "NOT_READY"]
        print("\n" + "=" * 92)
        print("结果: %s  PASS=%d FAIL=%d%s" % (
            "全绿" if not fails and not nr else ("未就绪" if not fails else "存在失败"),
            len([r for r in self.results if r[1] == "PASS"]), len(fails),
            ("  NOT_READY=%d" % len(nr)) if nr else ""))
        for name, st, _ in self.results:
            print("  %-60s %s" % (name, st))
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
    p.add_argument("--per-path", type=int, default=20, help="每条路径的笔数（三路径交替）")
    p.add_argument("--max-per-minute", type=int, default=90)
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--cash-account", type=int, default=20)
    p.add_argument("--teller-user", default="qa_teller")
    p.add_argument("--teller-password", default="Admin@123456")
    p.add_argument("--business-date", default=date.today().strftime("%Y-%m-%d"))
    p.add_argument("--force-account", type=int, default=24)
    p.add_argument("--force-account2", type=int, default=20)
    p.add_argument("--admin-user", default="qa_admin")
    p.add_argument("--admin-password", default="Admin@123456")
    p.add_argument("--pos-user", default="vcashier01")
    p.add_argument("--pos-password", default="123456")
    p.add_argument("--cleanup", action="store_true", help="结尾清理本次夹具（bank_transactions 按 tag）")
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    try:
        return Mixed(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
