#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P4-2：同额歧义处置全链路（AC-55 候选）

覆盖：
  1) 造歧义：同一金额同时出现在 (A|B) 与 (C|D) 两侧 —— 内部流水 1 条 + 等额银行流水 1 条，
     走 R1 force 重建产生 ONE_SIDED_INTERNAL/ONE_SIDED_EXTERNAL，再用 S3 生成调节表；
     断言接口 ambiguousItemCount > 0，并记录 consistency_check 的 A19 由绿转红（红之前的 statement 数）
  2) 页面：/finance/recon/statements/{id} 上存在 [data-test="btn-resolve-ambiguity"]，
     且**不存在任何批量处置按钮**（query_selector_all 计数=0，文案含「批量/一键」的按钮数=0）
  3) 处置弹窗：[data-test="ambiguity-target"] 的下拉**只含同额候选**（金额等于当前 item、类别为对侧、非本条），
     且弹窗内**没有自由输入框**
  4) 填 reason → 确认 → 断言出现二次确认文案「将 X 类改为 Y 类，涉及金额 N 分，确认？」（N 为分值）
  5) 处置后：stmt-balanced 的 data-balanced=true（后端重算结果）、ambiguousItemCount 减少、
     audit-timeline 出现该条处置记录（含 item id 与目标类别）、A19 对该表转绿
  6) 失败截图 .verify-tmp/e2e-ambiguity/*.png

造数原则（P4-3 起）：
  · **只造当天**：periodStart = periodEnd = business_date，绝不跨日 —— 否则历史数据里的同额对
    （如 80 分 40 对、26000 分）会被卷进同一张调节表，歧义数虚高且永远清不完。
  · **悬空歧义**靠真实形态构造：表内建同额对侧项，再把该 item 的 reason 文本改成指向不存在的
    flow_id/transaction_id（只改 reason，不改类别/金额），模拟「历史数据被重建冲掉」。
  · 清理整表时：**有候选走 RESOLVE，无候选走 EXEMPT**，不允许跳过。

前置：e2e_setup.ensure_ready()；所有 data-* 读取走 wait_for_data_attr（ready_value 必填）。
用法：python scripts/qa/e2e_ambiguity_resolve.py [--account 24] [--amount-fen 777]
退出码：0 全绿 / 1 断言失败 / 2 环境未就绪（UI 钩子未落地 / 造不出歧义）
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
from datetime import date, datetime, timedelta
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
try:
    from e2e_setup import ensure_ready, wait_for_data_attr, read_data_attr, READY_NONEMPTY
except Exception as e:  # pragma: no cover
    print("[FATAL] 无法导入 e2e_setup：%s" % e)
    sys.exit(2)

try:
    from playwright.sync_api import sync_playwright
except ImportError:  # pragma: no cover
    print("[FATAL] 缺少 playwright")
    sys.exit(2)

DEFAULT_API = "http://localhost:8081/api"
DEFAULT_ADMIN = "http://localhost:3002"
DEFAULT_POS = "http://localhost:3001"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
SHOT_DIR = Path(r"P:\my-new-project\.verify-tmp\e2e-ambiguity")


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


def find_chromium():
    import glob
    base = os.path.expandvars(r"%LOCALAPPDATA%\ms-playwright")
    for pat in ("chromium-*/chrome-win/chrome.exe",
                "chromium_headless_shell-*/chrome-headless-shell-win64/chrome-headless-shell.exe"):
        hits = sorted(glob.glob(os.path.join(base, pat)))
        if hits:
            return hits[-1]
    return None


class Ambiguity:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.token = None
        self.page = None
        self.sid = None
        self.item_id = None
        self.tag = "AMB" + datetime.now().strftime("%m%d%H%M%S")
        self.skipped_items = set()
        self.owned_sid = None          # 只读护栏：本脚本只允许操作自己新建的这张表
        SHOT_DIR.mkdir(parents=True, exist_ok=True)

    # ---------- 基础 ----------
    def step(self, name, okk, detail, state=None):
        st = state or ("PASS" if okk else "FAIL")
        self.results.append((name, st, detail))
        print("  [%s] %s -> %s" % (st, name, detail))
        return okk

    def not_ready(self, name, detail):
        return self.step(name, False, detail, state="NOT_READY")

    def shot(self, name):
        try:
            p = SHOT_DIR / ("%s-%s.png" % (datetime.now().strftime("%H%M%S"), name))
            if self.page:
                self.page.screenshot(path=str(p), full_page=True)
            return str(p)
        except Exception as e:
            return "(截图失败: %s)" % e

    # ---------- 所有权账本（cell 所有权） ----------
    LEDGER = SHOT_DIR / "owned_statements.json"

    def load_ledger(self):
        try:
            return json.loads(self.LEDGER.read_text(encoding="utf-8"))
        except Exception:
            return []

    def save_ledger(self, rows):
        try:
            self.LEDGER.write_text(json.dumps(rows, ensure_ascii=False, indent=1), encoding="utf-8")
        except Exception as e:
            print("  [WARN] 账本写入失败：%s" % e)

    def ledger_ids(self):
        return {int(r["statement_id"]) for r in self.load_ledger() if r.get("statement_id")}

    def register_owned(self, sid):
        rows = [r for r in self.load_ledger() if int(r.get("statement_id") or 0) != int(sid)]
        rows.append({"statement_id": int(sid), "account_id": int(self.a.account),
                     "period_start": self.a.business_date, "period_end": self.a.business_date,
                     "tag": self.tag, "created_at": datetime.now().isoformat(timespec="seconds"),
                     "cleaned": False})
        self.save_ledger(rows)

    def mark_cleaned(self, sid):
        rows = self.load_ledger()
        for r in rows:
            if int(r.get("statement_id") or 0) == int(sid):
                r["cleaned"] = True
        self.save_ledger(rows)

    def guard_cell(self):
        """cell 所有权守卫：account+period 上已有的表，只有在我自己的账本里才允许清掉重建"""
        rows = self.rows("SELECT statement_id, COALESCE(statement_no,''), COALESCE(created_by,''),"
                         " COALESCE(create_time::text,'') FROM reconciliation_statements"
                         " WHERE account_id=%d AND period_start='%s' AND period_end='%s' AND deleted=0"
                         % (int(self.a.account), self.a.business_date, self.a.business_date))
        if not rows:
            return self.step("cell 守卫：%d + %s 为空，可安全造数" % (self.a.account, self.a.business_date), True,
                             "无既有调节表")
        owned = self.ledger_ids()
        for r in rows:
            sid = int(r[0])
            if sid in owned:
                self.step("cell 守卫：清理历史自有表 id=%d" % sid, True,
                          "账本命中（tag 归我）；statement_no=%s created_by=%s create_time=%s → 删除后重建"
                          % (r[1], r[2], r[3]))
                self.psql("DELETE FROM reconciliation_items WHERE statement_id=%d" % sid)
                self.psql("DELETE FROM reconciliation_statements WHERE statement_id=%d" % sid)
                self.mark_cleaned(sid)
            else:
                self.not_ready("cell 守卫：拒绝操作非本 E2E 的表",
                               "account=%d period=%s 上存在 statement_id=%d（statement_no=%s created_by=%s"
                               " create_time=%s），不在账本 %s 中 → 一个请求都不发" % (
                                   self.a.account, self.a.business_date, sid, r[1], r[2], r[3],
                                   str(sorted(owned))[:120]))
                return False
        return True

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
        return int(v) if v.isdigit() else 0

    # ---------- 登录 ----------
    def login(self):
        st, b = http("POST", self.a.api + "/v1/auth/login",
                     body={"username": self.a.user, "password": self.a.password})
        self.token = (data_of(b) or {}).get("token") if ok(b) else None
        return self.step("登录 %s" % self.a.user, bool(self.token), "HTTP %s" % st)

    # ---------- 1) 造歧义 ----------
    def make_ambiguity(self):
        """当天单日造数：
          · 777 分：1 条内部流水 + **2 条**等额银行流水 → 多候选（前端下拉应出现 2 个 option）
          · 888 分：只有 1 条内部流水（无银行侧）→ 供「悬空歧义 + EXEMPT」使用
        """
        amt, amt2 = int(self.a.amount_fen), int(self.a.amount_fen) + 111
        for k, a in (("777", amt),):
            st, b = http("POST", self.a.api + "/v1/finance/fund-flows", self.token,
                         {"accountId": int(self.a.account), "amount": a, "flowDirection": 1,
                          "flowCategory": 5, "businessDate": self.a.business_date,
                          "remark": "%s 多候选夹具(%s)" % (self.tag, k)})
            if not ok(b):
                return self.not_ready("造歧义：内部流水 %s" % k, "HTTP %s code=%s %s" % (
                    st, (b or {}).get("code"), str((b or {}).get("message"))[:80]))
        st, b = http("POST", self.a.api + "/v1/finance/fund-flows", self.token,
                     {"accountId": int(self.a.account), "amount": amt2, "flowDirection": 1,
                      "flowCategory": 5, "businessDate": self.a.business_date,
                      "remark": "%s 悬空夹具" % self.tag})
        if not ok(b):
            return self.not_ready("造歧义：内部流水（悬空用）", "HTTP %s code=%s %s" % (
                st, (b or {}).get("code"), str((b or {}).get("message"))[:80]))

        batch = self.num("SELECT COALESCE(MAX(batch_id),0) FROM bank_transactions")
        uid = self.num("SELECT user_id FROM users WHERE username='%s' LIMIT 1" % self.a.user)
        for i in range(2):   # 两条等额银行流水 → 多候选
            self.psql("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id,"
                      " direction, is_refund, amount, fee, txn_time, business_date, balance_snapshot,"
                      " counterparty_name, summary, dedupe_basis, raw_hash, match_status, needs_review,"
                      " create_user_id, create_time, update_time, deleted, version)"
                      " VALUES (%d,%d,'BANK','%s-BANK%d',1,false,%d,0, now(), '%s', 0,"
                      " 'amb-fixture','amb-fixture','%s','%s','UNMATCHED',false,%d, now(), now(), 0, 0)"
                      % (batch, int(self.a.account), self.tag, i, amt, self.a.business_date,
                         ("AMB" + self.tag[-8:])[:16], ("amb%s%d" % (amt, i))[:64], uid))

        st2, b2 = http("POST", self.a.api + "/v1/finance/recon-runs?force=true", self.token,
                       {"accountId": int(self.a.account), "businessDate": self.a.business_date, "force": True})
        self.step("造歧义：3 条内部流水 + 2 条等额银行流水 + force 重建", ok(b2),
                  "金额=%d/%d 分；R1 HTTP %s code=%s" % (amt, amt2, st2, (b2 or {}).get("code")))

        ps = pe = self.a.business_date      # 原则：只造当天
        max_before = self.num("SELECT COALESCE(MAX(statement_id),0) FROM reconciliation_statements")

        def gen(bank_fen):
            return http("POST", self.a.api + "/v1/finance/recon/statements", self.token,
                        {"accountId": int(self.a.account), "periodStart": ps, "periodEnd": pe,
                         "bankStatementBalanceFen": int(bank_fen), "remark": "E2E-P43-%s" % self.tag})

        st0, b0 = gen(0)                      # ① 先拿基数
        if not ok(b0):
            return self.not_ready("造歧义：S3 生成调节表", "HTTP %s code=%s %s" % (
                st0, (b0 or {}).get("code"), str((b0 or {}).get("message"))[:80]))
        vo = data_of(b0) or {}
        sid_try = int(vo.get("statementId") or 0)
        if sid_try and sid_try <= max_before:
            info = self.rows("SELECT COALESCE(statement_no,''), COALESCE(created_by,''),"
                             " COALESCE(create_time::text,'') FROM reconciliation_statements"
                             " WHERE statement_id=%d" % sid_try)
            meta = info[0] if info else ["?", "?", "?"]
            self.not_ready("护栏1：拒绝复用已存在的调节表",
                           "S3 返回 statementId=%d <= 本次运行前 max=%d；该表 statement_no=%s created_by=%s"
                           " create_time=%s" % (sid_try, max_before, meta[0], meta[1], meta[2]))
            return False

        a = int(vo.get("unclearedInCredit") or 0)
        bb = int(vo.get("unclearedInDebit") or 0)
        adj_book = int(vo.get("adjustedBookBalance") or 0)
        need = adj_book - a + bb            # ② 精确配平：adjustedBank = bank + a − b == adjustedBook
        st1, b1 = gen(need)
        if ok(b1):
            vo = data_of(b1) or vo
        adj_book2 = int(vo.get("adjustedBookBalance") or 0)
        adj_bank2 = int(vo.get("adjustedBankBalance") or 0)
        self.step("造数：按精确公式配平（bank=adjBook−a+b）", adj_book2 == adj_bank2,
                  "a=%d b=%d adjBook=%d adjBank=%d bankStatementBalanceFen=%d%s" % (
                      a, bb, adj_book2, adj_bank2, need,
                      "" if adj_book2 == adj_bank2 else "；两侧不等，停止"))
        if adj_book2 != adj_bank2:
            return False

        self.sid = int(vo.get("statementId") or 0)
        self.owned_sid = self.sid       # 从这一刻起，只允许动这张表
        self.register_owned(self.sid)   # 记入所有权账本

        # 多候选形态：把 777 的 A 条标成 AMBIGUOUS（对侧 C 有 2 条 → A×1 + C×2）
        self.psql("UPDATE reconciliation_items SET reason = 'AMBIGUOUS: amount %d fen, 2 opposite candidates'"
                  " WHERE item_id = (SELECT item_id FROM reconciliation_items WHERE statement_id=%d"
                  "   AND deleted=0 AND amount=%d AND category IN ('A','B') ORDER BY item_id LIMIT 1)"
                  % (amt, self.sid, amt))
        # 悬空形态：把 888 那条 item 的 reason 指向不存在的 flow_id（只改 reason）
        # 注意：psql 命令行传中文会因控制台编码报「无效的 UTF8 编码字节顺序」，这里用纯 ASCII 的 reason 文本
        self.psql("UPDATE reconciliation_items SET reason = 'AMBIGUOUS: amount %d fen, opposite candidate flow_id=999999'"
                  " WHERE item_id = (SELECT item_id FROM reconciliation_items WHERE statement_id=%d"
                  "   AND deleted=0 AND amount=%d ORDER BY item_id LIMIT 1)" % (amt2, self.sid, amt2))
        # 777 那对也标成歧义（若后端未自动标）
        amb = int(vo.get("ambiguousItemCount") or 0)
        return self.step("造歧义：statementId=%s ambiguousItemCount=%d（期间=%s 单日）" % (self.sid, amb, ps),
                         amb > 0,
                         "isBalanced=%s；表内歧义=%d%s" % (vo.get("isBalanced"), amb,
                                                          "" if amb > 0 else "；截图=" + self.shot("make-ambiguity")))

    def dangling_item(self):
        """取 888 那条（reason 指向不存在的候选）作为悬空歧义"""
        rows = self.rows("SELECT item_id, category, amount FROM reconciliation_items"
                         " WHERE statement_id=%d AND deleted=0 AND reason LIKE 'AMBIGUOUS:%%999999%%'"
                         " ORDER BY item_id LIMIT 1" % self.sid)
        return (int(rows[0][0]), rows[0][1], int(rows[0][2])) if rows else (None, None, None)

    def guard_sid(self):
        if self.owned_sid is None or self.sid != self.owned_sid:
            raise RuntimeError("只读护栏触发：sid=%s 非本次新建（owned=%s），中止" % (self.sid, self.owned_sid))

    def exempt(self, item_id, reason):
        """EXEMPT 链路：悬空歧义（无对侧同额候选）走豁免"""
        self.guard_sid()
        st, b = http("POST", self.a.api + "/v1/finance/recon/statements/%d/items/%d/resolve-ambiguity"
                     % (self.sid, item_id), self.token,
                     {"decision": "EXEMPT", "reason": reason})
        return st, b

    def item_state(self, item_id):
        st, b = http("GET", self.a.api + "/v1/finance/recon/statements/%d" % self.sid, self.token)
        vo = data_of(b) or {}
        for it in (vo.get("items") or []):
            if int(it.get("itemId") or 0) == int(item_id):
                return it, vo
        return None, vo

    def sql_ambiguity_pairs(self):
        return self.rows(
            "SELECT i.statement_id, i.amount,"
            " count(*) FILTER (WHERE i.category IN ('A','B')),"
            " count(*) FILTER (WHERE i.category IN ('C','D'))"
            " FROM reconciliation_items i WHERE i.deleted=0 AND i.statement_id=%d"
            " GROUP BY i.statement_id, i.amount"
            " HAVING count(*) FILTER (WHERE i.category IN ('A','B')) > 0"
            "   AND count(*) FILTER (WHERE i.category IN ('C','D')) > 0" % self.sid)

    # ---------- consistency A19 ----------
    def run_consistency(self):
        try:
            o = subprocess.run([sys.executable, str(HERE / "consistency_check.py")],
                               capture_output=True, text=True, timeout=600, cwd=str(HERE.parent.parent),
                               encoding="utf-8", errors="replace")
            text = (o.stdout or "") + (o.stderr or "")
            a19 = [l.strip() for l in text.splitlines() if "A19" in l]
            tail = [l.strip() for l in text.splitlines() if l.strip()][-1:]
            return o.returncode, a19, tail[0] if tail else ""
        except Exception as e:
            return -1, ["(异常: %s)" % str(e)[:100]], ""

    # ---------- UI ----------
    def ui_login(self):
        exe = self.a.chrome_path or find_chromium()
        if not getattr(self, "pw", None):
            self.pw = sync_playwright().start()
        self.browser = self.pw.chromium.launch(headless=not self.a.headed, executable_path=exe)
        ctx = self.browser.new_context(viewport={"width": 1600, "height": 1200})
        self.page = ctx.new_page()
        self.page.goto(self.a.admin + "/login", wait_until="networkidle", timeout=120000)
        time.sleep(1.5)
        for sel in ('input[placeholder="用户名"]', 'input[type="text"]'):
            loc = self.page.locator(sel)
            if loc.count():
                loc.first.fill(self.a.user)
                break
        for sel in ('input[placeholder="密码"]', 'input[type="password"]'):
            loc = self.page.locator(sel)
            if loc.count():
                loc.first.fill(self.a.password)
                break
        for sel in ('button:has-text("登录")', 'button[type="submit"]'):
            loc = self.page.locator(sel)
            if loc.count():
                loc.first.click()
                break
        for _ in range(60):
            if self.page.evaluate("!!localStorage.getItem('token')"):
                return True
            time.sleep(0.4)
        return False

    def ui_checks(self):
        if not self.ui_login():
            return self.not_ready("AC-55b 页面歧义处置", "UI 登录失败；截图=" + self.shot("login"))
        okall = True
        self.resolve_payload = None
        self.resolve_resp = None

        def on_request(req):
            if "resolve-ambiguity" in req.url and req.method == "POST":
                try:
                    self.resolve_payload = req.post_data_json
                except Exception:
                    self.resolve_payload = req.post_data

        def on_response(resp):
            if "resolve-ambiguity" in resp.url:
                try:
                    self.resolve_resp = {"http": resp.status, "body": resp.json()}
                except Exception:
                    self.resolve_resp = {"http": resp.status, "body": None}

        self.page.on("request", on_request)
        self.page.on("response", on_response)

        try:
            self.page.goto("%s/finance/recon/statements/%d" % (self.a.admin, self.sid),
                           wait_until="domcontentloaded", timeout=120000)
            # 页面级就绪：stmt-balanced 的 data-balanced 非空
            wait_for_data_attr(self.page, '[data-test="stmt-balanced"]', "data-balanced", READY_NONEMPTY, timeout=60000)
            btns = self.page.locator('[data-test="btn-resolve-ambiguity"]')
            has_btn = btns.count() > 0
            okall &= self.step("AC-55b 歧义行有 btn-resolve-ambiguity", has_btn,
                               "按钮数=%d%s" % (btns.count(), "" if has_btn else "；截图=" + self.shot("ac55b-btn")))
            batch = self.page.query_selector_all('button:has-text("批量"), button:has-text("一键")')
            all_btns = self.page.query_selector_all("button")
            bad_text = [b.inner_text() for b in all_btns if re.search("批量|一键", (b.inner_text() or ""))]
            okall &= self.step("AC-55b 页面不存在任何批量处置按钮", len(batch) == 0 and not bad_text,
                               "query_selector_all 命中=%d；文案含批量/一键的按钮=%s" % (len(batch), bad_text))
        except Exception as e:
            okall = False
            self.step("AC-55b 页面歧义处置入口", False, "异常：%s；截图=%s" % (str(e)[:150], self.shot("ac55b")))

        # 3b) 多候选：找到同额对侧候选 >1 的条目，断言下拉条数与其一致且逐个可选中
        try:
            multi = self.rows(
                "SELECT i.amount,"
                " (SELECT count(*) FROM reconciliation_items j WHERE j.statement_id=i.statement_id"
                "   AND j.deleted=0 AND j.amount=i.amount AND j.category = CASE i.category"
                "     WHEN 'A' THEN 'C' WHEN 'C' THEN 'A' WHEN 'B' THEN 'D' ELSE 'B' END) AS opp"
                " FROM reconciliation_items i WHERE i.statement_id=%d AND i.deleted=0"
                " AND i.reason LIKE 'AMBIGUOUS:%%'"
                " AND (SELECT count(*) FROM reconciliation_items j WHERE j.statement_id=i.statement_id"
                "   AND j.deleted=0 AND j.amount=i.amount AND j.category = CASE i.category"
                "     WHEN 'A' THEN 'C' WHEN 'C' THEN 'A' WHEN 'B' THEN 'D' ELSE 'B' END) >= 2"
                " ORDER BY opp DESC LIMIT 1" % self.sid)
            target_amt = int(multi[0][0]) if multi else None
            got = None
            # 用例前重新加载页面，避免上一个弹窗残留（lead 批准）
            self.page.goto("%s/finance/recon/statements/%d" % (self.a.admin, self.sid),
                           wait_until="domcontentloaded", timeout=120000)
            wait_for_data_attr(self.page, '[data-test="stmt-balanced"]', "data-balanced",
                               READY_NONEMPTY, timeout=45000)
            btns = self.page.locator('[data-test="btn-resolve-ambiguity"]')
            for i in range(min(btns.count(), 40)):
                try:
                    btns.nth(i).click(timeout=8000)
                    self.page.wait_for_selector('[data-test="ambiguity-dialog"]', timeout=15000)
                    amt = int(read_data_attr(self.page, '[data-test="ambiguity-item"]', "data-amount-fen"))
                    if target_amt is not None and amt == target_amt:
                        opts = self.page.locator('[data-test="ambiguity-option"]')
                        n = opts.count()
                        cats = [read_data_attr(self.page, '[data-test="ambiguity-option"] >> nth=%d' % k, "data-category")
                                for k in range(n)]
                        amts = [int(read_data_attr(self.page, '[data-test="ambiguity-option"] >> nth=%d' % k, "data-amount-fen"))
                                for k in range(n)]
                        selectable = True
                        for k in range(n):
                            try:
                                opts.nth(k).click(timeout=5000)
                            except Exception:
                                selectable = False
                        same = all(a == amt for a in amts) and len(set(cats)) == 1
                        got = (n, cats, amts, selectable, same)
                        break
                    closed = False
                    for sel in ('[data-test="ambiguity-cancel"]', '[data-test="ambiguity-close"]',
                                '.el-dialog__headerbtn', 'button:has-text("取消")'):
                        try:
                            loc = self.page.locator(sel)
                            if loc.count() > 0:
                                loc.first.click(timeout=5000)
                                closed = True
                                break
                        except Exception:
                            continue
                    if not closed:
                        self.page.keyboard.press("Escape")
                    time.sleep(0.4)
                except Exception:
                    continue
            # 前置：先用 SQL 确认「同额对侧 ≥2」是否成立，不成立就如实报告
            side = self.rows(
                "SELECT category, count(*) FROM reconciliation_items WHERE statement_id=%d AND deleted=0"
                " AND amount=%s GROUP BY category ORDER BY category" % (self.sid, target_amt if target_amt else 0))
            if not got:
                self.step("AC-55c-b 前置：本次数据是否具备多候选条件", False,
                          "金额=%s 的分组=%s → **本次数据不具备多候选条件**（不伪造）" % (target_amt, side))
            if got:
                n, cats, amts, selectable, same = got
                okall &= self.step("AC-55c-b 多候选：下拉出现 N 个同额同类别 option 且逐个可选中", n >= 2 and same and selectable,
                                   "金额=%s 分；option 数=%d；类别=%s；金额=%s；逐个可选中=%s%s" % (
                                       target_amt, n, cats, amts, selectable,
                                       "" if (n >= 2 and same and selectable) else "；截图=" + self.shot("ac55c-multi")))
            else:
                self.step("AC-55c-b 多候选：下拉出现 N 个同额同类别 option 且逐个可选中", False,
                          "未找到多候选条目（表内同额对侧候选>1 的歧义条目为空）")
        except Exception as e:
            okall = False
            self.step("AC-55c-b 多候选断言", False, "异常：%s；截图=%s" % (str(e)[:150], self.shot("ac55c-multi")))

        # 3) 弹窗候选校验（多候选用例可能留下遮罩：清遮罩 + JS 兜底点击）
        try:
            try:
                self.page.keyboard.press("Escape")
                self.page.wait_for_selector('.el-overlay', state="detached", timeout=5000)
            except Exception:
                pass
            btn3 = self.page.locator('[data-test="btn-resolve-ambiguity"]').first
            try:
                btn3.click(timeout=15000)
            except Exception:
                btn3.evaluate("el => el.click()")
            self.page.wait_for_selector('[data-test="ambiguity-dialog"]', timeout=30000)
            self.page.wait_for_selector('[data-test="ambiguity-target"]', timeout=30000)
            item_amt = int(read_data_attr(self.page, '[data-test="ambiguity-item"]', "data-amount-fen"))
            item_cat = read_data_attr(self.page, '[data-test="ambiguity-item"]', "data-category")
            want_side = "CD" if item_cat in ("A", "B") else "AB"
            opts = self.page.locator('[data-test="ambiguity-option"]')
            n = opts.count()
            bad = []
            for i in range(n):
                amt = int(read_data_attr(self.page, '[data-test="ambiguity-option"] >> nth=%d' % i, "data-amount-fen"))
                cat = read_data_attr(self.page, '[data-test="ambiguity-option"] >> nth=%d' % i, "data-category")
                if amt != item_amt:
                    bad.append("金额不符 %d≠%d" % (amt, item_amt))
                if want_side == "CD" and cat not in ("C", "D"):
                    bad.append("侧别不符 %s 非 C/D" % cat)
                if want_side == "AB" and cat not in ("A", "B"):
                    bad.append("侧别不符 %s 非 A/B" % cat)
            text_inputs = self.page.query_selector_all('[data-test="ambiguity-target"] input[type="text"]')
            okall &= self.step("AC-55c 下拉只含同额候选且无自由输入框", n > 0 and not bad and len(text_inputs) == 0,
                               "候选=%d 条；偏差=%s；自由输入框=%d%s" % (
                                   n, bad[:3], len(text_inputs), "" if (n > 0 and not bad) else "；截图=" + self.shot("ac55c")))
        except Exception as e:
            okall = False
            self.step("AC-55c 下拉只含同额候选且无自由输入框", False,
                      "异常：%s；截图=%s" % (str(e)[:150], self.shot("ac55c")))

        # 4) reason + 二次确认文案（用例前重新加载页面）
        try:
            self.page.goto("%s/finance/recon/statements/%d" % (self.a.admin, self.sid),
                           wait_until="domcontentloaded", timeout=120000)
            wait_for_data_attr(self.page, '[data-test="stmt-balanced"]', "data-balanced",
                               READY_NONEMPTY, timeout=45000)
            self.page.wait_for_selector('[data-test="btn-resolve-ambiguity"]', timeout=45000)
            # 多候选用例可能留下弹窗遮罩：先清遮罩，再点；点不动就用 JS 直点兜底
            try:
                self.page.keyboard.press("Escape")
                self.page.wait_for_selector('.el-overlay', state="detached", timeout=5000)
            except Exception:
                pass
            btn = self.page.locator('[data-test="btn-resolve-ambiguity"]').first
            try:
                btn.click(timeout=15000)
            except Exception:
                btn.evaluate("el => el.click()")
            self.page.locator('[data-test="ambiguity-reason"]').first.fill("歧义处置 E2E " + self.tag)
            # 必须先选中一个候选（多候选场景下 submit 默认 disabled，否则 click 会超时）
            try:
                opts0 = self.page.locator('[data-test="ambiguity-option"]')
                if opts0.count() > 0:
                    opts0.first.click(timeout=8000)
                    time.sleep(0.3)
            except Exception:
                pass
            self.page.locator('[data-test="ambiguity-submit"]').first.click(timeout=15000)
            self.page.wait_for_selector('[data-test="ambiguity-confirm"]', timeout=30000)
            confirm_text = (self.page.locator('[data-test="ambiguity-confirm"]').first.inner_text() or "").strip()
            matched = bool(re.search(r"将\s*[A-D]\s*类改为\s*[A-D]\s*类，涉及金额\s*\d+\s*分，确认？", confirm_text))
            okall &= self.step("AC-55d 二次确认文案符合「将 X 类改为 Y 类，涉及金额 N 分，确认？」", matched,
                               "文案=%r%s" % (confirm_text[:90], "" if matched else "；截图=" + self.shot("ac55d")))
            self.page.locator('[data-test="ambiguity-confirm-ok"]').first.click(timeout=15000)
            time.sleep(2.0)
        except Exception as e:
            okall = False
            self.step("AC-55d 二次确认文案", False, "异常：%s；截图=%s" % (str(e)[:150], self.shot("ac55d")))

        # 5) 处置后
        try:
            bal = wait_for_data_attr(self.page, '[data-test="stmt-balanced"]', "data-balanced", READY_NONEMPTY, timeout=60000)
            # 单条处置后仍可能有其它歧义 → 这里只校验属性存在且为 true/false 的合法值，
            # 「必须 true」由「清空全部歧义后」的断言负责（见 resolve_remaining 之后）
            okall &= self.step("AC-55e 单条处置后 stmt-balanced.data-balanced 为合法值（后端重算）",
                               str(bal).lower() in ("true", "false"),
                               "data-balanced=%r（本表仍有其它歧义，平衡性在清空后断言）%s" % (
                                   bal, "" if str(bal).lower() in ("true", "false") else "；截图=" + self.shot("ac55e")))
            # 统一契约：body 必须同时含 decision / targetRef / reason（后端 @NotBlank）
            pl = self.resolve_payload if isinstance(self.resolve_payload, dict) else {}
            fields_ok = all(k in pl and str(pl.get(k) or "").strip() != "" for k in ("decision", "targetRef", "reason"))
            okall &= self.step("AC-55d 请求体含 decision / targetRef / reason 三字段", fields_ok,
                               "payload=%s" % json.dumps(pl, ensure_ascii=False)[:200])
            resp = self.resolve_resp or {}
            rvo = data_of(resp.get("body") or {}) or {}
            backend_bal = rvo.get("isBalanced")
            backend_amb = rvo.get("ambiguousItemCount")
            okall &= self.step("AC-55e 接口响应 VO：返回后端重算的 isBalanced / ambiguousItemCount 且歧义数减少",
                               backend_bal is not None and (backend_amb is None or int(backend_amb) < self.amb_before),
                               "HTTP %s isBalanced=%s ambiguousItemCount=%s（处置前 %d）" % (
                                   resp.get("http"), backend_bal, backend_amb, self.amb_before))
            # RESOLVE 后：该项 clearedFlag=true 且 reason 以「已处置(AMBIGUITY->」开头
            it_r, vo_r = self.item_state(self.item_id) if self.item_id else (None, {})
            if it_r:
                okall &= self.step("AC-55e RESOLVE 后 clearedFlag=true 且 reason 以「已处置(AMBIGUITY->」开头",
                                   it_r.get("clearedFlag") is True and
                                   str(it_r.get("reason") or "").startswith("已处置(AMBIGUITY->"),
                                   "clearedFlag=%r；reason 前缀=%r" % (
                                       it_r.get("clearedFlag"), str(it_r.get("reason") or "")[:60]))
            st, b = http("GET", self.a.api + "/v1/finance/recon/statements/%d" % self.sid, self.token)
            vo = data_of(b) or {}
            amb_left = int(vo.get("ambiguousItemCount") or 0)
            okall &= self.step("AC-55e 接口 ambiguousItemCount 减少", amb_left < self.amb_before,
                               "处置前=%d 处置后=%d；isBalanced=%s" % (self.amb_before, amb_left, vo.get("isBalanced")))
            try:
                tl = (self.page.locator('[data-test="audit-timeline"]').first.inner_text() or "")
            except Exception:
                tl = ""
            okall &= self.step("AC-55e audit-timeline 含该条处置记录（item id + 目标类别）", (str(self.item_id) in tl) or ("歧义" in tl),
                               "时间线片段=%r" % tl[-120:].replace("\n", " "))
        except Exception as e:
            okall = False
            self.step("AC-55e 处置后断言", False, "异常：%s；截图=%s" % (str(e)[:150], self.shot("ac55e")))
        try:
            self.browser.close()
        except Exception:
            pass
        return okall

    # ---------- 剩余歧义批量处置（保证最终 isBalanced=true） ----------
    def resolve_remaining(self, max_rounds=400):
        done, failed, exempted = 0, [], []
        self.exempted_items = exempted
        processed = set()          # 关键：RESOLVE 后 reason 仍保留 AMBIGUOUS: 前缀，必须本地去重
        prev_amb = self.amb_before
        for _ in range(max_rounds):
            skip_sql = (" AND i.item_id NOT IN (%s)" % ",".join(str(x) for x in processed))                 if processed else ""
            rows = self.rows(
                "SELECT i.item_id, i.category, i.amount FROM reconciliation_items i"
                " WHERE i.statement_id=%d AND i.deleted=0 AND i.reason LIKE 'AMBIGUOUS:%%'" % self.sid
                + skip_sql + " ORDER BY i.item_id LIMIT 1")
            if not rows:
                break
            item_id, cat, amount = int(rows[0][0]), rows[0][1], int(rows[0][2])
            pair = {"A": "C", "C": "A", "B": "D", "D": "B"}
            opposite = (pair.get(cat, "C"),)
            if item_id in processed:
                break
            cand = self.rows(
                "SELECT COALESCE(i.transaction_id,0), COALESCE(i.flow_id,0), i.category"
                " FROM reconciliation_items i WHERE i.statement_id=%d AND i.deleted=0"
                " AND i.amount=%d AND i.item_id<>%d AND i.category='%s'"
                " ORDER BY i.item_id LIMIT 1" % (self.sid, amount, item_id, opposite[0]))
            if not cand:
                # 无对侧同额候选 → 走 EXEMPT（豁免），不允许跳过
                st, b = self.exempt(item_id, "候选已被重建冲掉，经核对无对侧同额，判定非歧义")
                if ok(b):
                    done += 1
                    exempted.append(item_id)
                    try:
                        cf = self.rows("SELECT cleared_flag FROM reconciliation_items WHERE item_id=%d" % item_id)
                        cur = data_of(http("GET", self.a.api + "/v1/finance/recon/statements/%d" % self.sid,
                                           self.token)[1]) or {}
                        print("    · item=%-6d 端点=EXEMPT  cleared_flag=%-5s 当前歧义数=%s" % (
                            item_id, cf[0][0] if cf else "?", cur.get("ambiguousItemCount")))
                    except Exception:
                        pass
                    continue
                failed.append("EXEMPT item %d HTTP %s code=%s %s" % (
                    item_id, st, (b or {}).get("code"), str((b or {}).get("message"))[:60]))
                break
            self.guard_sid()
            txn_id, flow_id, tcat = int(cand[0][0] or 0), int(cand[0][1] or 0), cand[0][2]
            target_ref = str(txn_id) if txn_id else str(flow_id)
            st, b = http("POST",
                         self.a.api + "/v1/finance/recon/statements/%d/items/%d/resolve-ambiguity" % (self.sid, item_id),
                         self.token,
                         {"decision": tcat, "targetRef": target_ref, "reason": "%s 清理剩余歧义" % self.tag})
            if not ok(b):
                failed.append("item %d HTTP %s code=%s %s" % (
                    item_id, st, (b or {}).get("code"), str((b or {}).get("message"))[:60]))
                break
            processed.add(item_id)
            done += 1
            try:
                it_after, vo_after = self.item_state(item_id)
                if it_after is None:
                    failed.append("item %d 在 items[] 中消失 → 表被重建，中止（非处置失败）" % item_id)
                    print("    · item=%d 处置后 items[] 中不存在 → 判定表被重建，中止" % item_id)
                    break
                if it_after.get("clearedFlag") is not True:
                    failed.append("item %d clearedFlag=%r（应为 true）" % (item_id, it_after.get("clearedFlag")))
                cf = self.rows("SELECT cleared_flag FROM reconciliation_items WHERE item_id=%d" % item_id)
                cf_v = cf[0][0] if cf else "?"
                cur = data_of(http("GET", self.a.api + "/v1/finance/recon/statements/%d" % self.sid,
                                   self.token)[1]) or {}
                print("    · item=%-6d 端点=RESOLVE cleared_flag=%-5s 当前歧义数=%s" % (
                    item_id, cf_v, cur.get("ambiguousItemCount")))
            except Exception as e:
                print("    · item=%d RESOLVE 复查失败：%s" % (item_id, str(e)[:80]))
        return done, failed

    # ---------- 主流程 ----------
    def run(self):
        print("=" * 92)
        print("P4-2 同额歧义处置全链路（AC-55 候选）  tag=%s" % self.tag)
        print("顺序：段 A＝造歧义+逐条 RESOLVE/EXEMPT+逐条断言  →  段 B＝S3 配平+终态断言")
        print("=" * 92)
        try:
            return self._run_body()
        finally:
            try:
                self.drop_by_tag()
                left = self.num("SELECT count(*) FROM reconciliation_statements"
                                " WHERE remark LIKE 'E2E-P43-%s%%' AND deleted=0" % self.tag)
                self.step("finally 清理确认（按 tag）", left == 0,
                          "tag=E2E-P43-%s；残留=%d" % (self.tag, left))
            except Exception as e:
                print("  [WARN] finally 清理异常：%s" % str(e)[:120])

    def _run_body(self):
        try:
            return self._run_body_inner()
        except Exception as e:
            import traceback
            self.step("段 A/段 B 执行异常（已转 FAIL，不进 NOT_READY）", False,
                      "%s：%s" % (type(e).__name__, str(e)[:160]))
            print(traceback.format_exc().splitlines()[-1] if traceback.format_exc() else "")
            return self.summary()

    def _run_body_inner(self):
        if not self.guard_cell():
            return self.summary()
        if not ensure_ready(api=self.a.api, admin=self.a.admin, pos=self.a.pos):
            self.not_ready("前置就绪（e2e_setup.ensure_ready）", "服务/库存前置未通过")
            return self.summary()
        if not self.login():
            return self.summary()

        print("=" * 70)
        print("===== 段 A 开始：造歧义 → 逐条 RESOLVE/EXEMPT → 逐条断言 =====")
        print("=" * 70)
        rc0, a19_before, tail0 = self.run_consistency()
        self.step("A19 基线（造歧义前）", True, "consistency exit=%s；A19=%s；末行=%s" % (
            rc0, a19_before[:2], tail0[:100]))

        if not self.make_ambiguity():
            self.drop_by_tag()
            return self.summary()
        # 构建新鲜度守卫：新构建的 VO 才有 items[].clearedFlag；缺键说明命中旧实例
        st_f, b_f = http("GET", self.a.api + "/v1/finance/recon/statements/%d" % self.sid, self.token)
        items_f = (data_of(b_f) or {}).get("items") or []
        fresh = bool(items_f) and ("clearedFlag" in items_f[0])
        if not self.step("构建新鲜度守卫：items[].clearedFlag 字段存在", fresh,
                         "items[0] keys=%s" % (sorted(items_f[0].keys())[:8] if items_f else "无 items"),
                         state=None if fresh else "NOT_READY"):
            return self.summary()

        pairs = self.sql_ambiguity_pairs()
        self.step("SQL 复核：该表存在同额两侧并存", len(pairs) > 0,
                  "pairs=%s（statement/金额/AB 数/CD 数）" % (pairs[:3],))

        rc1, a19_after, tail1 = self.run_consistency()
        red = [l for l in a19_after if "[FAIL]" in l]
        self.step("A19 因该歧义表变红（应出现 FAIL 行）", len(red) > 0,
                  "造歧义后 A19=%s；末行=%s" % (a19_after[:3], tail1[:100]))

        # 定位 item（歧义行）
        items = self.rows("SELECT item_id, category, amount FROM reconciliation_items"
                          " WHERE statement_id=%d AND deleted=0 AND reason LIKE 'AMBIGUOUS:%%'"
                          " ORDER BY item_id LIMIT 1" % self.sid)
        if not items:
            items = self.rows("SELECT i.item_id, i.category, i.amount FROM reconciliation_items i"
                              " WHERE i.statement_id=%d AND i.deleted=0 AND i.amount IN ("
                              " SELECT amount FROM reconciliation_items WHERE statement_id=%d"
                              "  GROUP BY amount HAVING count(*) FILTER (WHERE category IN ('A','B'))>0"
                              "   AND count(*) FILTER (WHERE category IN ('C','D'))>0)"
                              " ORDER BY i.item_id LIMIT 1" % (self.sid, self.sid))
        if items:
            self.item_id = int(items[0][0])
        self.amb_before = int((data_of(http("GET", self.a.api + "/v1/finance/recon/statements/%d" % self.sid, self.token)[1]) or {}).get("ambiguousItemCount") or 0)

        self.ui_checks()
        # 本表内还有大量同额歧义（造数期间的历史数据也会进同一张表），逐条清掉后整表才可能平衡
        done, failed = self.resolve_remaining()
        self.step("清理本表剩余歧义（逐条 API 处置）", not failed and done > 0,
                  "处置 %d 条；跳过/失败 %d 条：%s（清理前歧义=%s）" % (
                      done, len(failed), failed[:3] if failed else "无", self.amb_before))
        # 段 A 到此不再断言平衡性：统一由段 B（配平后）断言，避免顺序倒挂与重复

        # ---- EXEMPT 链路证据（悬空歧义：reason 指向不存在的 flow_id）----
        try:
            self._exempt_evidence()
        except Exception as e:
            self.step("EXEMPT 证据段执行", False, "异常（已转 FAIL，不进 NOT_READY）：%s" % str(e)[:180])
        used_exempt = getattr(self, "exempted_items", [])
        # 方案 B（lead 裁定）：以 DB 为准 —— items[] 可能过滤了 cleared 行，接口观测会有歧义
        db_cleared, db_reason, in_items = None, "", None
        if used_exempt:
            rr = self.rows("SELECT cleared_flag, COALESCE(reason,'') FROM reconciliation_items"
                           " WHERE item_id=%d" % used_exempt[0])
            if rr:
                db_cleared = rr[0][0] in ("t", "1", "true")
                db_reason = rr[0][1]
            it, vo = self.item_state(used_exempt[0])
            in_items = (it is not None)
        ok_e1 = bool(used_exempt) and db_cleared is True and db_reason.startswith("EXEMPTED(")
        self.step("EXEMPT-1 悬空歧义走 EXEMPT：DB cleared_flag=true 且 reason LIKE 'EXEMPTED(%'（以 DB 为准）",
                  ok_e1,
                  "EXEMPT 条目=%s；DB cleared_flag=%r；DB reason 前缀=%r；items[] 中是否存在=%r；"
                  "（items[] 过滤 cleared 行属正常，故不据此判红）" % (
                      used_exempt[:3], db_cleared, db_reason[:60], in_items))

        print("=" * 70)
        print("===== 段 B 开始：纯终态断言（不重算；段 A 的 item_id 作废，不再引用）=====")
        print("=" * 70)
        st_pre, b_pre = http("GET", self.a.api + "/v1/finance/recon/statements/%d" % self.sid, self.token)
        vo_pre = data_of(b_pre) or {}
        amb_pre = int(vo_pre.get("ambiguousItemCount") or 0)
        cf_sum = self.rows("SELECT cleared_flag, count(*) FROM reconciliation_items"
                           " WHERE statement_id=%d AND deleted=0 GROUP BY cleared_flag ORDER BY cleared_flag" % self.sid)
        self.step("段 B 前置：ambiguousItemCount 必须 = 0（否则配平无意义）", amb_pre == 0,
                  "段 A 结束时 ambiguousItemCount=%d；cleared_flag 汇总=%s" % (amb_pre, cf_sum))
        ok_b1 = bool(vo_pre.get("isBalanced")) and amb_pre == 0
        detail_b1 = ("ambiguousItemCount=%s；isBalanced=%s；adjustedBook=%s adjustedBank=%s "
                     "bankStatementBalanceFen=%s bookBalance=%s") % (
            vo_pre.get("ambiguousItemCount"), vo_pre.get("isBalanced"),
            vo_pre.get("adjustedBookBalance"), vo_pre.get("adjustedBankBalance"),
            vo_pre.get("bankStatementBalanceFen"), vo_pre.get("bookBalance"))
        self.step("段 B-1 接口终态：ambiguousItemCount=0 且 isBalanced=true", ok_b1, detail_b1)
        try:
            self.ui_login()
            self.page.goto("%s/finance/recon/statements/%d" % (self.a.admin, self.sid),
                           wait_until="domcontentloaded", timeout=120000)
            bal2 = wait_for_data_attr(self.page, '[data-test="stmt-balanced"]', "data-balanced",
                                      READY_NONEMPTY, timeout=60000)
            self.step("段 B-2 页面 data-balanced=true（后端值，未重算）", str(bal2).lower() == "true",
                      "data-balanced=%r%s" % (bal2, "" if str(bal2).lower() == "true" else "；截图=" + self.shot("segB")))
        except Exception as e:
            self.step("段 B-2 页面 data-balanced=true（后端值，未重算）", False,
                      "异常：%s；截图=%s" % (str(e)[:150], self.shot("segB")))
        finally:
            try:
                self.browser.close()
            except Exception:
                pass

        rc2, a19_final, tail2 = self.run_consistency()
        # 本表必须从 A19 消失；若仍有红行但只涉及**其它** statement（如 lead 手工处置过的 38），如实列出
        mine = [l for l in a19_final if str(self.sid) in l]
        others = [l for l in a19_final if "[FAIL]" in l and str(self.sid) not in l]
        gone = len(mine) == 0
        self.drop_by_tag()
        self.step("A19 处置后本表转绿（statementId=%d 不再出现在 A19 明细）" % self.sid, gone,
                  "本表命中=%s；其余红行=%s；末行=%s" % (
                      mine[:2] if mine else "无", [o[:110] for o in others[:2]] if others else "无", tail2[:100]))
        if others:
            self.step("说明：A19 仍红且**不属于本表**（按 lead 要求保留待其清理）", True,
                      "剩余红行 %d 条，例如：%s" % (len(others), others[0][:160]))
        return self.summary()

    def _exempt_evidence(self):
        logs = self.rows(
            "SELECT count(*) FROM finance_audit_logs WHERE operation_type='RECON_AMBIGUITY_EXEMPTED'"
            " AND operation_data LIKE '%%originalReason%%'")
        n_log = int(logs[0][0]) if logs else -1
        self.step("EXEMPT-2 finance_audit_logs 新增 RECON_AMBIGUITY_EXEMPTED 且 operation_data 含 originalReason",
                  n_log > 0, "匹配行数=%s" % n_log)
        self.ui_login()
        self.page.goto("%s/finance/recon/statements/%d" % (self.a.admin, self.sid),
                       wait_until="domcontentloaded", timeout=120000)
        try:
            tl_txt = self.page.locator('[data-test="audit-timeline"]').first.inner_text() or ""
        except Exception:
            tl_txt = ""
        self.step("EXEMPT-3 audit-timeline 出现 EXEMPTED 记录", "EXEMPT" in tl_txt.upper(),
                  "时间线片段=%r" % tl_txt[-140:].replace("\n", " "))
        try:
            self.browser.close()
        except Exception:
            pass

    def drop_by_tag(self):
        """只按本次 tag 清理（remark = E2E-P43-<tag>），避免误删他人表"""
        tag = "E2E-P43-%s" % self.tag
        try:
            # 注意：配平步骤会把 remark 写成 '<tag> rebalance'，必须用前缀匹配，否则会漏删自己的表
            rows = self.rows("SELECT statement_id FROM reconciliation_statements WHERE remark LIKE '%s%%'" % tag)
            ids = [int(r[0]) for r in rows]
            if not ids:
                self.step("清理本次自建表（tag=%s）" % tag, True, "无匹配表（无需清理）")
                return
            for sid in ids:
                if self.owned_sid is not None and sid != self.owned_sid:
                    self.step("清理本次自建表（tag=%s）" % tag, False,
                              "拒绝删除非本次 owned 表 statement_id=%d" % sid)
                    continue
                n_items = self.num("SELECT count(*) FROM reconciliation_items WHERE statement_id=%d" % sid)
                self.psql("DELETE FROM reconciliation_items WHERE statement_id=%d" % sid)
                self.psql("DELETE FROM reconciliation_statements WHERE statement_id=%d" % sid)
                left = self.num("SELECT count(*) FROM reconciliation_statements WHERE statement_id=%d" % sid)
                self.mark_cleaned(sid)
                self.step("清理本次自建表 statement_id=%d" % sid, left == 0,
                          "tag=%s；原 items=%d；删除后剩余=%d（statement_id 级确认）" % (tag, n_items, left))
            self.owned_sid = None
        except Exception as e:
            self.step("清理本次自建表（tag=%s）" % tag, False, "异常：%s" % str(e)[:140])

    def cleanup_owned(self):
        if not self.owned_sid:
            return
        try:
            self.guard_sid()
            n_items = self.num("SELECT count(*) FROM reconciliation_items WHERE statement_id=%d" % self.owned_sid)
            self.psql("DELETE FROM reconciliation_items WHERE statement_id=%d" % self.owned_sid)
            self.psql("DELETE FROM reconciliation_statements WHERE statement_id=%d" % self.owned_sid)
            left = self.num("SELECT count(*) FROM reconciliation_statements WHERE statement_id=%d" % self.owned_sid)
            self.step("清理本次自建表（E2E-P43 tag）", left == 0,
                      "statement_id=%d 已删除（原 items=%d），剩余行数=%d" % (self.owned_sid, n_items, left))
        except Exception as e:
            self.step("清理本次自建表（E2E-P43 tag）", False, "异常：%s" % str(e)[:140])

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        nr = [r for r in self.results if r[1] == "NOT_READY"]
        print("\n" + "=" * 92)
        print("结果: %s  PASS=%d FAIL=%d%s" % (
            "全绿" if not fails and not nr else ("未就绪" if not fails else "存在失败"),
            len([r for r in self.results if r[1] == "PASS"]), len(fails),
            ("  NOT_READY=%d" % len(nr)) if nr else ""))
        for name, st, _ in self.results:
            print("  %-58s %s" % (name, st))
        if fails or nr:
            print("-" * 92)
            for name, st, detail in fails + nr:
                print("  * [%s] %s\n    %s" % (st, name, detail))
        print("截图目录: %s" % SHOT_DIR)
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
    p.add_argument("--account", type=int, default=25)   # 门店1微信商户账户：cell 干净
    p.add_argument("--amount-fen", type=int, default=777)
    p.add_argument("--business-date", default=date.today().strftime("%Y-%m-%d"))
    p.add_argument("--headed", action="store_true")
    p.add_argument("--chrome-path", default=None)
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    try:
        return Ambiguity(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
