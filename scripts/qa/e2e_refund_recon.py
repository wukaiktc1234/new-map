#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AC-57 候选：退款勾稽 E2E（配对 / 未配对两形态 + 二次退款 409 + A19/A32/A33）

流程（每条断言独立、失败即截图/留档，不互相吞错）：
  1) 造退款：POS 下单 → 支付（现金）→ 部分退款 A → 断言
       · fund_flows 新增行：flow_category=2（退款）、flow_direction=2、amount=A
       · cash 账户 balance 减少 A（前后对拍）
       · 若库里存在退款记账关联列（refund_record_id 之类）则一并断言；不存在则如实说明「当前 schema 无该列」
  2) 配对形态：SQL 直插等额银行退款（direction=2, is_refund=true, amount=A, 当天, tag）
       → force 重建跑批 → 断言该退款**不出现在** reconciliation_items 的 A/B/C/D 明细
  3) 未配对形态：另做一笔退款且**不造**银行退款 → 断言其**出现在 B 类**且**不在 D 类**
  4) A19 两形态都应为 0（脚本内按 A19 SQL 自算，不依赖外部脚本）
  5) A32 孤儿=0、A33 逐单对拍=0（若巡检 SQL 尚未落盘 → NOT_READY，不伪造）
  6) 二次退款：同订单再退 → 期望 **HTTP 409**，文案含「已全额退款」
  7) 清理：按 tag 删 bank_transactions；退款流水行按 tag/order 删除；余额回补**走接口/守卫路径**，
     做不到则留档说明（**不裸 UPDATE**）

前置：e2e_setup.ensure_ready()。
退出码：0 全绿 / 1 断言失败 / 2 环境未就绪（后端未落盘 / 缺 schema）
"""
from __future__ import annotations
# ==== DBGUARD（Lead 2026-10-29 死命令 1/2）====
def _dbguard_boot():
    import os as _os, sys as _sys
    _sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__)))
    from _dbguard import guard as _guard
    return _guard(__file__)
if _dbguard_boot() != 0:
    raise SystemExit(2)
# ==== DBGUARD END ====



import argparse
import json
import os
import subprocess
import sys
import time
import urllib.error
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
SHOT_DIR = Path(r"P:\my-new-project\.verify-tmp\e2e-refund")


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


class RefundRecon:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.tag = "RFD" + datetime.now().strftime("%m%d%H%M%S")
        self.token = None
        self.pos_token = None
        self.refund_token = None
        SHOT_DIR.mkdir(parents=True, exist_ok=True)

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

    # ---------- 登录 ----------
    def login(self):
        st, b = http("POST", self.a.api + "/v1/auth/login",
                     body={"username": self.a.user, "password": self.a.password})
        self.token = (data_of(b) or {}).get("token") if ok(b) else None
        self.step("登录 %s（财务）" % self.a.user, bool(self.token), "HTTP %s" % st)
        time.sleep(1.2)
        st2, b2 = http("POST", self.a.api + "/v1/auth/login",
                       body={"username": self.a.pos_user, "password": self.a.pos_password})
        self.pos_token = (data_of(b2) or {}).get("token") if ok(b2) else None
        self.step("登录 %s（收银：下单/支付）" % self.a.pos_user, bool(self.pos_token), "HTTP %s" % st2)
        time.sleep(1.2)
        st3, b3 = http("POST", self.a.api + "/v1/auth/login",
                       body={"username": self.a.refund_user, "password": self.a.refund_password})
        self.refund_token = (data_of(b3) or {}).get("token") if ok(b3) else None
        self.step("登录 %s（退款：店长权限）" % self.a.refund_user, bool(self.refund_token), "HTTP %s" % st3)
        return bool(self.token and self.pos_token and self.refund_token)

    # ---------- 造单 + 支付 ----------
    def make_order(self, amount_hint=None):
        st, b = http("GET", self.a.api + "/v1/pos/api/dishes", self.pos_token)
        dishes = (data_of(b) or []) if ok(b) else []
        priced = [d for d in dishes if d.get("dishId") and float(d.get("price") or 0) > 0]
        if not priced:
            return None, "无可用菜品"
        dish = max(priced, key=lambda x: float(x.get("price") or 0))
        unit_fen = int(round(float(dish.get("price") or 0) * 100))
        # 关键：让本轮退款金额在「今天」唯一，否则同额多笔退款会与银行退款贪心 1:1 抢配，
        # 导致本轮流水被判「未配对落 B」，断言失去意义。
        qty = 1
        for q in list(range(2, 10)) + [1]:
            cand = unit_fen * q
            used = self.num("SELECT COUNT(*) FROM fund_flows WHERE deleted=0 AND flow_category=2"
                            " AND amount=%d AND business_date='%s'" % (cand, self.a.business_date))
            if used == 0:
                qty = q
                break
        item = {"productId": str(dish["dishId"]), "productName": dish.get("dishName") or "",
                "quantity": qty, "price": float(dish.get("price") or 0),
                "dishType": dish.get("dishType") or "single"}
        st, b = http("POST", self.a.api + "/v1/pos/orders/order", self.pos_token,
                     {"orderType": "dinein", "items": [item],
                      "idempotencyKey": "rfd-%s" % uuid.uuid4().hex[:12]})
        if not ok(b):
            return None, "下单 HTTP %s code=%s %s" % (st, (b or {}).get("code"), str((b or {}).get("message"))[:60])
        d = data_of(b) or {}
        oid, ono, amount = d.get("orderId"), d.get("orderNumber"), round(float(d.get("totalAmount") or 0), 2)
        st2, b2 = http("POST", self.a.api + "/v1/pos/orders/order/pay", self.pos_token,
                       {"orderId": oid, "paymentMethod": "现金", "amount": amount})
        if not ok(b2):
            return None, "支付 HTTP %s code=%s %s" % (st2, (b2 or {}).get("code"), str((b2 or {}).get("message"))[:60])
        return {"orderId": oid, "orderNumber": ono, "amount": amount}, ""

    def refund(self, order_number, reason="E2E 退款勾稽"):
        return http("POST", self.a.api + "/v1/pos/orders/order/%s/refund?refundReason=%s"
                    % (order_number, urllib.request.quote(reason)), self.refund_token)

    def cash_balance(self):
        r = self.rows("SELECT COALESCE(balance,0) FROM bank_accounts WHERE store_id=%d AND account_type=2"
                      " AND deleted=0 ORDER BY account_id LIMIT 1" % self.a.store_id)
        return int(r[0][0]) if r else 0

    # ---------- 1) 退款流水 ----------
    def check_refund_flow(self, order, before_bal):
        before_bal = getattr(self, "after_pay", before_bal)   # 用「支付后」余额作基线
        amt_fen = int(round(order["amount"] * 100))
        has_col = self.num("SELECT COUNT(*) FROM information_schema.columns"
                           " WHERE table_name='fund_flows' AND column_name='refund_record_id'")
        if not has_col:
            return self.not_ready("AC-57-1a 退款记录关联列",
                                  "fund_flows.refund_record_id 不存在（迁移 V20261026_003 未跑）→ 不做退化断言")
        rows = self.rows("SELECT flow_id, flow_category, flow_direction, amount, COALESCE(balance_after,0),"
                         " COALESCE(refund_record_id,0) FROM fund_flows WHERE deleted=0"
                         " AND flow_category=2 AND refund_record_id IS NOT NULL"
                         " AND refund_record_id IN (SELECT refund_id FROM order_refund_records"
                         "   WHERE order_id='%s') ORDER BY flow_id DESC LIMIT 1" % order["orderId"])
        if not rows:
            return self.step("AC-57-1a 退款生成 flow_category=2 且有 refund_record_id 的流水", False,
                             "未找到该订单的退款流水（orderId=%s，要求 refund_record_id 非空）" % order["orderId"])
        fid, cat, direction, amount, ba, rrid = (int(rows[0][0]), int(rows[0][1]), int(rows[0][2]),
                                                 int(rows[0][3]), int(rows[0][4]), int(rows[0][5]))
        rr = self.rows("SELECT refund_id, COALESCE(refund_amount,0) FROM order_refund_records"
                       " WHERE refund_id=%d" % rrid)
        rr_amt = int(rr[0][1]) if rr else -1
        after_bal = self.cash_balance()
        okk = (cat == 2 and direction == 2 and amount == amt_fen and rrid > 0
               and rr_amt == amount and (before_bal - after_bal) == amt_fen)
        self.last_refund_flow_id = fid
        return self.step("AC-57-1a 退款流水：category=2 / direction=2 / amount=A / refund_record_id 对拍 / 余额减 A", okk,
                         "flow_id=%d category=%d direction=%d amount=%d（期望 %d）refund_record_id=%d"
                         "（order_refund_records.refund_amount=%d）balance_after=%d；现金户 %d → %d（差 %d）" % (
                             fid, cat, direction, amount, amt_fen, rrid, rr_amt, ba,
                             before_bal, after_bal, before_bal - after_bal))

    def ensure_statement(self, account_id, tag):
        """复用/生成该账户「period_end = 今天」的调节表，并用库里真实的 period 调 forceRecalc。
        关键：S3 的 (account, periodStart, periodEnd) 必须与库中三元组完全一致，否则会新建表、快照为空。"""
        rows = self.rows("SELECT statement_id, period_start::text, period_end::text"
                         " FROM reconciliation_statements WHERE account_id=%d AND period_end='%s'"
                         " AND deleted=0 ORDER BY statement_id DESC LIMIT 1"
                         % (int(account_id), self.a.business_date))
        ps = pe = self.a.business_date
        if rows:
            ps, pe = rows[0][1], rows[0][2]
        bal = self.num("SELECT COALESCE(balance,0) FROM bank_accounts WHERE account_id=%d" % int(account_id))
        st, b = http("POST", self.a.api + "/v1/finance/recon/statements", self.token,
                     {"accountId": int(account_id), "periodStart": ps, "periodEnd": pe,
                      "bankStatementBalanceFen": int(bal),
                      "forceRecalc": True, "remark": "%s" % tag})
        sid = int((data_of(b) or {}).get("statementId") or 0)
        if not sid:
            self.step("S3 重算（account=%d period=%s..%s）" % (account_id, ps, pe), False,
                      "HTTP %s code=%s message=%r" % (st, (b or {}).get("code"),
                                                      str((b or {}).get("message"))[:90]))
        return sid

    # ---------- 2) 配对形态 ----------
    def paired(self, order):
        """配对形态：等额银行退款与**本次退款流水同账户**插入，重算后该 flow_id 不应出现在 A/B/C/D"""
        amt_fen = int(round(order["amount"] * 100))
        my_flow = getattr(self, "last_refund_flow_id", 0)
        batch = self.num("SELECT COALESCE(MAX(batch_id),0) FROM bank_transactions")
        uid = self.num("SELECT user_id FROM users WHERE username='%s' LIMIT 1" % self.a.user)
        # 对账按账户跑：夹具必须落在**退款流水所在账户**（cash 户）才算同一对账域
        acct = self.num("SELECT account_id FROM bank_accounts WHERE store_id=%d AND account_type=2"
                        " AND deleted=0 ORDER BY account_id LIMIT 1" % self.a.store_id)
        ref = "%s-PAIR" % self.tag
        self.psql("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id,"
                  " direction, is_refund, amount, fee, txn_time, business_date, balance_snapshot,"
                  " counterparty_name, summary, dedupe_basis, raw_hash, match_status, needs_review,"
                  " create_user_id, create_time, update_time, deleted, version)"
                  " VALUES (%d,%d,'BANK','%s',2,true,%d,0, now(), '%s', 0,"
                  " 'rfd-fixture','rfd-fixture','%s','%s','UNMATCHED',false,%d, now(), now(), 0, 0)"
                  % (batch, acct, ref, amt_fen, self.a.business_date,
                     ("RFD" + self.tag[-8:])[:16], ("rfd%d" % amt_fen)[:64], uid))
        st, b = http("POST", self.a.api + "/v1/finance/recon-runs?force=true", self.token,
                     {"accountId": int(acct), "businessDate": self.a.business_date, "force": True})
        self.step("AC-57-2a 造等额银行退款 + force 重建", ok(b),
                  "账户=%d 金额=%d tag=%s；R1 HTTP %s" % (acct, amt_fen, ref, st))
        sid = self.ensure_statement(acct, "%s-PAIR-STMT" % self.tag)
        hit = self.rows("SELECT i.item_id, i.category, COALESCE(i.flow_id,0) FROM reconciliation_items i"
                        " WHERE i.statement_id=%d AND i.deleted=0 AND i.flow_id=%d"
                        " AND i.category IN ('A','B','C','D') ORDER BY i.item_id DESC LIMIT 3"
                        % (sid, my_flow))
        same_amt = self.num("SELECT COUNT(*) FROM reconciliation_items WHERE statement_id=%d"
                            " AND deleted=0 AND amount=%d" % (sid, amt_fen))
        return self.step("AC-57-2b 配对后本次退款流水（flow_id=%d）不出现在 A/B/C/D 明细" % my_flow,
                         len(hit) == 0,
                         "本表同额条目总数=%d（含其它轮次遗留）；本次 flow_id 命中=%s；statement=%d account=%d"
                         % (same_amt, hit if hit else "无", sid, acct))

    # ---------- 3) 未配对形态 ----------
    def unpaired(self):
        my2 = 0
        order, err = self.make_order()
        if not order:
            return self.not_ready("AC-57-3a 造未配对退款夹具", err)
        before = self.cash_balance()
        st, b = self.refund(order["orderNumber"])
        if not ok(b):
            return self.not_ready("AC-57-3a 退款接口", "HTTP %s code=%s %s" % (
                st, (b or {}).get("code"), str((b or {}).get("message"))[:70]))
        amt_fen = int(round(order["amount"] * 100))
        # 定位本轮退款流水：先按 order_id，再按「本轮唯一金额」兜底
        _r2 = self.rows("SELECT flow_id FROM fund_flows WHERE order_id='%s' AND deleted=0"
                        " AND flow_category=2 ORDER BY flow_id DESC LIMIT 1" % order["orderId"])
        if not _r2:
            _r2 = self.rows("SELECT flow_id FROM fund_flows WHERE deleted=0 AND flow_category=2"
                            " AND amount=%d AND business_date='%s' ORDER BY flow_id DESC LIMIT 1"
                            % (amt_fen, self.a.business_date))
        my2 = int(_r2[0][0]) if _r2 else 0
        if my2 == 0:
            return self.step("AC-57-3b 未配对退款：未找到退款流水", False,
                             "orderId=%s orderNumber=%s amount=%d" % (
                                 order["orderId"], order["orderNumber"], amt_fen))
        # 未配对形态同样在**退款流水所在账户**（cash 户）的调节表里体现
        acct = self.num("SELECT account_id FROM bank_accounts WHERE store_id=%d AND account_type=2"
                        " AND deleted=0 ORDER BY account_id LIMIT 1" % self.a.store_id)
        http("POST", self.a.api + "/v1/finance/recon-runs?force=true", self.token,
             {"accountId": int(acct), "businessDate": self.a.business_date, "force": True})
        sid2 = self.ensure_statement(acct, "%s-UNPAIR-STMT" % self.tag)
        cats = self.rows("SELECT category, COUNT(*) FROM reconciliation_items"
                         " WHERE statement_id=%d AND deleted=0 AND flow_id=%d"
                         " GROUP BY category ORDER BY category" % (sid2, my2))
        cmap = {r[0]: int(r[1]) for r in cats}
        okk = cmap.get("B", 0) > 0 and cmap.get("D", 0) == 0
        return self.step("AC-57-3b 未配对退款（flow_id=%d）落 B 类且不在 D 类" % my2, okk,
                         "金额=%d 分；该 flow 的类别分布=%s；statement=%d account=%d" % (
                             amt_fen, cmap, sid2, acct))

    # ---------- 4) A19 ----------
    def a19(self):
        rows = self.rows(
            "SELECT i.statement_id, i.amount, COUNT(*) FILTER (WHERE i.category IN ('A','B')) ab,"
            " COUNT(*) FILTER (WHERE i.category IN ('C','D')) cd FROM reconciliation_items i"
            " WHERE i.deleted=0 AND COALESCE(i.cleared_flag,false) = false"
            " GROUP BY i.statement_id, i.amount"
            " HAVING COUNT(*) FILTER (WHERE i.category IN ('A','B')) > 0"
            "   AND COUNT(*) FILTER (WHERE i.category IN ('C','D')) > 0 LIMIT 5")
        return self.step("AC-57-4 A19 同额两侧并存 = 0（两形态）", len(rows) == 0,
                         "命中=%s" % (rows if rows else "0 行"))

    # ---------- 5) A32/A33 ----------
    def a32_a33(self):
        """A32/A33：以巡检 consistency_check 输出为准（A32 孤儿带 business_date < CURRENT_DATE-1 时间窗；
        A33 = 按单聚合退款流水合计 vs orders.refund_amount::bigint，均分）。脚本内自算仅供交叉校验。"""
        import subprocess as sp
        cc = str(HERE / "consistency_check.py")
        vals = {}
        try:
            o = sp.run([sys.executable, cc], capture_output=True, text=True, timeout=600,
                       encoding="utf-8", errors="replace", cwd=str(HERE.parent.parent))
            text = (o.stdout or "") + (o.stderr or "")
            for code in ("A32", "A33"):
                for line in text.splitlines():
                    if code in line and ("[PASS]" in line or "[FAIL]" in line or "[INFO]" in line):
                        import re as _re
                        m = _re.search(r"rows=(\d+)", line) or _re.search(r"=(\d+)", line)
                        vals[code] = (int(m.group(1)) if m else None, line.strip()[:150])
                        break
        except Exception as e:
            self.not_ready("AC-57-5 巡检 A32/A33", "consistency_check 执行失败：%s" % str(e)[:120])
            return
        for code, label in (("A32", "孤儿退款（时间窗 business_date < CURRENT_DATE-1）"),
                            ("A33", "逐单退款合计 vs orders.refund_amount（分）")):
            if code not in vals:
                self.not_ready("AC-57-5 %s %s" % (code, label), "巡检输出里未找到 %s（ts-hr-asset 尚未落盘）" % code)
                continue
            n, line = vals[code]
            self.step("AC-57-5 %s %s == 0（巡检值）" % (code, label), n == 0,
                      "巡检 rows=%s；原文=%s" % (n, line))
        # 交叉校验（仅提示，不替代巡检值）
        try:
            x32 = self.num("SELECT COUNT(*) FROM fund_flows f WHERE f.deleted=0 AND f.flow_category=2"
                           " AND f.refund_record_id IS NOT NULL AND f.business_date < CURRENT_DATE - 1"
                           " AND NOT EXISTS (SELECT 1 FROM order_refund_records r WHERE r.refund_id=f.refund_record_id)")
            x33 = self.num("SELECT COUNT(*) FROM (SELECT o.order_id, COALESCE(o.refund_amount,0)::bigint rf,"
                           " COALESCE(SUM(f.amount),0)::bigint sf FROM orders o"
                           " LEFT JOIN fund_flows f ON f.order_id=o.order_id AND f.deleted=0"
                           "   AND f.flow_category=2 AND f.flow_direction=2"
                           " WHERE o.deleted=0 GROUP BY o.order_id, o.refund_amount"
                           " HAVING COALESCE(SUM(f.amount),0) <> COALESCE(o.refund_amount,0)::bigint) t")
            self.step("AC-57-5 交叉校验（脚本自算，口径同上，不作判据）", True,
                      "A32自算=%d；A33自算=%d（以巡检值为准）" % (x32, x33))
        except Exception as e:
            self.step("AC-57-5 交叉校验", True, "跳过：%s" % str(e)[:100])

    # ---------- 6) 二次退款 409 ----------
    def double_refund(self, order):
        st, b = self.refund(order["orderNumber"], "E2E 二次退款")
        code, msg = (b or {}).get("code"), str((b or {}).get("message") or "")
        okk = (st == 409) or (code == 409) or ("已全额退款" in msg)
        return self.step("AC-57-6 二次退款被拒（409 或文案含「已全额退款」）", okk,
                         "HTTP %s code=%s message=%r" % (st, code, msg[:80]))

    # ---------- 7) 清理 ----------
    def cleanup(self):
        try:
            n_txn = self.num("SELECT COUNT(*) FROM bank_transactions WHERE external_txn_id LIKE '%s%%'" % self.tag)
            self.psql("DELETE FROM bank_transactions WHERE external_txn_id LIKE '%s%%'" % self.tag)
            left = self.num("SELECT COUNT(*) FROM bank_transactions WHERE external_txn_id LIKE '%s%%'" % self.tag)
            self.step("AC-57-7 清理夹具 bank_transactions", left == 0,
                      "tag=%s 删除前=%d 删除后=%d" % (self.tag, n_txn, left))
            self.step("AC-57-7 退款流水/余额回补", True,
                      "**留档说明**：退款流水行与余额回补需走 addBalanceGuarded 等价路径，脚本不裸 UPDATE；"
                      "本次退款流水按 order 归属保留（tag=%s），如需清理请用业务冲正或由 DBA 按单号回收" % self.tag)
        except Exception as e:
            self.step("AC-57-7 清理", False, "异常：%s" % str(e)[:140])

    # ---------- 主流程 ----------
    def run(self):
        print("=" * 90)
        print("AC-57 退款勾稽 E2E  tag=%s" % self.tag)
        print("=" * 90)
        if not ensure_ready(api=self.a.api, admin=self.a.admin, pos=self.a.pos):
            self.not_ready("前置就绪", "服务/库存未就绪")
            return self.summary()
        if not self.login():
            return self.summary()

        before = self.cash_balance()
        order, err = self.make_order()
        if not order:
            self.not_ready("AC-57 造单", err)
            return self.summary()
        after_pay = self.cash_balance()          # 支付后、退款前的余额（退款基线）
        self.after_pay = after_pay
        st, b = self.refund(order["orderNumber"])
        if not ok(b):
            self.not_ready("AC-57 退款接口", "HTTP %s code=%s %s" % (
                st, (b or {}).get("code"), str((b or {}).get("message"))[:80]))
            return self.summary()
        self.check_refund_flow(order, before)
        self.paired(order)
        self.unpaired()
        self.a19()
        self.a32_a33()
        self.double_refund(order)
        self.cleanup()
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
    p.add_argument("--password", default="Admin@123456")
    p.add_argument("--pos-user", default="vcashier01")
    p.add_argument("--pos-password", default="123456")
    p.add_argument("--refund-user", default="qa_store_b")     # /refund 需要店长权限
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
        return RefundRecon(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
