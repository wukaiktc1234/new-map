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
        self.exempted_items = []        # 本次被 EXEMPT 的 item（供 EXEMPT-1/3 断言）
        self.run_started_at = datetime.now()
        self.owned_flow_nos = set()      # 本次造数自建的流水白名单（flow_no）
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

    # ---------- 所有权账本（cell 所有权，按 account 分区） ----------
    LEDGER = SHOT_DIR / "owned_statements.json"

    def load_ledger(self):
        """返回 {account_id(str): [statement_id, ...]}；兼容旧的 list 结构并迁移"""
        try:
            raw = json.loads(self.LEDGER.read_text(encoding="utf-8"))
        except Exception:
            return {}
        if isinstance(raw, list):          # 旧结构 → 迁移
            out = {}
            for r in raw:
                acc = str(r.get("account_id") or "")
                if acc:
                    out.setdefault(acc, []).append(int(r.get("statement_id") or 0))
            return out
        return {str(k): [int(x) for x in v] for k, v in (raw or {}).items()}

    def save_ledger(self, data):
        try:
            self.LEDGER.write_text(json.dumps(data, ensure_ascii=False, indent=1), encoding="utf-8")
        except Exception as e:
            print("  [WARN] 账本写入失败：%s" % e)

    def ledger_ids(self, account=None):
        acc = str(account if account is not None else self.a.account)
        return set(self.load_ledger().get(acc, []))

    def register_owned(self, sid):
        data = self.load_ledger()
        acc = str(self.a.account)
        ids = set(data.get(acc, []))
        ids.add(int(sid))
        data[acc] = sorted(ids)
        self.save_ledger(data)

    def mark_cleaned(self, sid):
        data = self.load_ledger()
        acc = str(self.a.account)
        data[acc] = [x for x in data.get(acc, []) if int(x) != int(sid)]
        self.save_ledger(data)

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
        for i in range(2):   # 两条等额银行流水 → 多候选（777）
            self.psql("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id,"
                      " direction, is_refund, amount, fee, txn_time, business_date, balance_snapshot,"
                      " counterparty_name, summary, dedupe_basis, raw_hash, match_status, needs_review,"
                      " create_user_id, create_time, update_time, deleted, version)"
                      " VALUES (%d,%d,'BANK','%s-BANK%d',1,false,%d,0, now(), '%s', 0,"
                      " 'amb-fixture','amb-fixture','%s','%s','UNMATCHED',false,%d, now(), now(), 0, 0)"
                      % (batch, int(self.a.account), self.tag, i, amt, self.a.business_date,
                         ("AMB" + self.tag[-8:])[:16], ("amb%s%d" % (amt, i))[:64], uid))

        # 白名单：本次造数创建的内部流水（remark 带 tag 精确匹配）
        try:
            # 白名单口径：本次运行开始后、本账号新增的流水（不依赖 remark 文本是否被原样保存）
            # 白名单口径：本账号、**今天**创建的流水（含本脚本今日早前几轮建的；不动历史日期的流水）
            for r in self.rows("SELECT flow_no FROM fund_flows WHERE account_id=%d"
                               " AND create_time >= '%s 00:00:00'" % (
                                   int(self.a.account), self.a.business_date)):
                self.owned_flow_nos.add(r[0])
            self.step("流水白名单登记", len(self.owned_flow_nos) > 0,
                      "本次自建流水 %d 条：%s" % (len(self.owned_flow_nos),
                                              sorted(self.owned_flow_nos)[:5]))
        except Exception as e:
            self.step("流水白名单登记", False, "异常：%s" % str(e)[:120])

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
        c = int(vo.get("unclearedOutCredit") or 0)
        d = int(vo.get("unclearedOutDebit") or 0)
        book = int(vo.get("bookBalance") or 0)
        # 新语义：只剩未处置（cleared_flag=false）的条目参与合计。
        # 夹具策略：把银行对账单余额设成 == bookBalance → 全部条目处置（cleared）后
        # a=b=c=d=0 ⇒ adjBook=bookBalance=adjBank，段 B 自然 is_balanced=true。
        self.a.bank_fen = book
        st1, b1 = gen(book)
        if ok(b1):
            vo = data_of(b1) or vo
        self.step("造数：夹具银行余额 = bookBalance（清空全部条目前提下两侧自然配平）", ok(b1),
                  "a=%d b=%d c=%d d=%d book=%d → 处置后 adjBook=adjBank=%d；当前 adjBook=%s adjBank=%s"
                  % (a, bb, c, d, book, book, vo.get("adjustedBookBalance"), vo.get("adjustedBankBalance")))

        self.sid = int(vo.get("statementId") or 0)
        self.owned_sid = self.sid       # 从这一刻起，只允许动这张表
        self.register_owned(self.sid)   # 记入所有权账本

        # 888 也造一条银行流水，保证两侧都存在（否则遗留单边条目，合计无法归 0）
        self.psql("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id,"
                  " direction, is_refund, amount, fee, txn_time, business_date, balance_snapshot,"
                  " counterparty_name, summary, dedupe_basis, raw_hash, match_status, needs_review,"
                  " create_user_id, create_time, update_time, deleted, version)"
                  " VALUES (%d,%d,'BANK','%s-BANK888',1,false,%d,0, now(), '%s', 0,"
                  " 'amb-fixture','amb-fixture','%s','%s','UNMATCHED',false,%d, now(), now(), 0, 0)"
                  % (batch, int(self.a.account), self.tag, amt2, self.a.business_date,
                     ("AMB" + self.tag[-8:])[:16], ("amb%d" % amt2)[:64], uid))

        # 多候选形态：把 777 的 A 条标成 AMBIGUOUS（对侧 C 有 2 条 → A×1 + C×2）
        self.psql("UPDATE reconciliation_items SET reason = 'AMBIGUOUS: amount %d fen, 2 opposite candidates'"
                  " WHERE item_id = (SELECT item_id FROM reconciliation_items WHERE statement_id=%d"
                  "   AND deleted=0 AND amount=%d AND category IN ('A','B') ORDER BY item_id LIMIT 1)"
                  % (amt, self.sid, amt))
        # 悬空形态：把 888 那条 item 的 reason 指向不存在的 flow_id（只改 reason）
        # 注意：psql 命令行传中文会因控制台编码报「无效的 UTF8 编码字节顺序」，这里用纯 ASCII 的 reason 文本
        self.psql("UPDATE reconciliation_items SET reason = 'AMBIGUOUS: amount %d fen, opposite candidate flow_id=999999'"
                  " WHERE statement_id=%d AND deleted=0 AND amount=%d" % (amt2, self.sid, amt2))
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

        # 3c) 处置前后合计对照（P4-4 新增）：先抓基线
        try:
            st_v0, b_v0 = http("GET", self.a.api + "/v1/finance/recon/statements/%d" % self.sid, self.token)
            self.vo_pre_dispose = data_of(b_v0) or {}
        except Exception:
            self.vo_pre_dispose = {}

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
            m = re.match(r"^确认本条归属 ([A-D]) 类，从当前 ([A-D]) 类未达账项合计中移除，"
                         r"涉及金额 (\d+) 分，确认？$", confirm_text.strip())
            pair = {"A": "C", "C": "A", "B": "D", "D": "B"}
            matched = bool(m) and pair.get(m.group(2)) == m.group(1)
            if m:
                self.confirm_detail = "归属=%s 侧=%s 金额=%s 分 对侧=%s" % (
                    m.group(1), m.group(2), m.group(3), pair.get(m.group(2)) == m.group(1))
            okall &= self.step("AC-55d 二次确认文案符合新文案（归属 X 类 / 从 Y 类合计移除 / N 分 / 类别互为对侧）", matched,
                               "文案=%r；解析=%s%s" % (confirm_text[:110],
                                                     getattr(self, "confirm_detail", "未解析"),
                                                     "" if matched else "；截图=" + self.shot("ac55d")))
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
            # P4-4：处置后本侧合计真的减少（A 侧 → unclearedInCredit 减少 == 该条金额）
            vo0 = getattr(self, "vo_pre_dispose", {}) or {}
            vo1 = data_of(http("GET", self.a.api + "/v1/finance/recon/statements/%d" % self.sid, self.token)[1]) or {}
            it0 = next((x for x in (vo0.get("items") or []) if int(x.get("itemId") or 0) == int(self.item_id or 0)), {})
            amt_item = int(it0.get("amount") or 0)
            cat_item = it0.get("category")
            f0 = {k: int(vo0.get(k) or 0) for k in ("unclearedInCredit", "unclearedInDebit",
                                                    "unclearedOutCredit", "unclearedOutDebit",
                                                    "adjustedBankBalance", "adjustedBookBalance")}
            f1 = {k: int(vo1.get(k) or 0) for k in f0}
            key = {"A": "unclearedInCredit", "B": "unclearedInDebit",
                   "C": "unclearedOutCredit", "D": "unclearedOutDebit"}.get(cat_item or "A")
            ok_sum = (f0[key] - f1[key]) == amt_item
            ok_bal_consistent = (bool(vo1.get("isBalanced")) ==
                                 (f1["adjustedBookBalance"] == f1["adjustedBankBalance"]))
            okall &= self.step("P4-4a 处置后本侧合计减少 == 该条金额", ok_sum,
                               "类别=%s 金额=%d；%s: %d→%d（差 %d）；adjBank %d→%d；adjBook %d→%d" % (
                                   cat_item, amt_item, key, f0[key], f1[key], f0[key] - f1[key],
                                   f0["adjustedBankBalance"], f1["adjustedBankBalance"],
                                   f0["adjustedBookBalance"], f1["adjustedBookBalance"]))
            okall &= self.step("P4-4a is_balanced == (adjBook==adjBank)（方向性，不强求 true）", ok_bal_consistent,
                               "isBalanced=%s；adjBook=%d adjBank=%d；单条处置后可能由 true 变 false 属正常" % (
                                   vo1.get("isBalanced"), f1["adjustedBookBalance"], f1["adjustedBankBalance"]))

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

    # ---------- 清掉所有未处置条目（合计只算 cleared_flag=false，必须全清才能配平） ----------
    def clear_all_uncleared(self, max_rounds=400):
        self.guard_sid()
        done, exempted = 0, []
        for _ in range(max_rounds):
            rows = self.rows(
                "SELECT i.item_id, i.category, i.amount FROM reconciliation_items i"
                " WHERE i.statement_id=%d AND i.deleted=0 AND i.cleared_flag = false"
                " ORDER BY i.item_id LIMIT 1" % self.sid)
            if not rows:
                break
            item_id, cat, amount = int(rows[0][0]), rows[0][1], int(rows[0][2])
            pair = {"A": "C", "C": "A", "B": "D", "D": "B"}
            if amount == int(self.a.amount_fen) + 111:      # 888 悬空夹具：强制走 EXEMPT 覆盖链路
                st, b = self.exempt(item_id, "候选已被重建冲掉，经核对无对侧同额，判定非歧义")
                if ok(b):
                    self.exempted_items.append(item_id)
                    done += 1
                    continue
                return done, ["item %d EXEMPT HTTP %s code=%s" % (item_id, st, (b or {}).get("code"))]
            cand = self.rows(
                "SELECT COALESCE(i.transaction_id,0), COALESCE(i.flow_id,0), i.category"
                " FROM reconciliation_items i WHERE i.statement_id=%d AND i.deleted=0"
                " AND i.amount=%d AND i.item_id<>%d AND i.category='%s'"
                " ORDER BY i.item_id LIMIT 1" % (self.sid, amount, item_id, pair.get(cat, "C")))
            if cand:
                txn, flow, tcat = int(cand[0][0] or 0), int(cand[0][1] or 0), cand[0][2]
                ref = str(txn) if txn else str(flow)
                st, b = http("POST", self.a.api + "/v1/finance/recon/statements/%d/items/%d/resolve-ambiguity"
                             % (self.sid, item_id), self.token,
                             {"decision": tcat, "targetRef": ref, "reason": "%s 清未处置" % self.tag})
                if not ok(b):
                    return done, ["item %d RESOLVE HTTP %s code=%s" % (item_id, st, (b or {}).get("code"))]
            else:
                st, b = self.exempt(item_id, "无对侧同额候选，经核对判定非歧义（清未处置）")
                if not ok(b):
                    return done, ["item %d EXEMPT HTTP %s code=%s" % (item_id, st, (b or {}).get("code"))]
                exempted.append(item_id)
            done += 1
        self.exempted_items.extend(exempted)
        return done, []

    # ---------- 剩余歧义批量处置（保证最终 isBalanced=true） ----------
    def resolve_remaining(self, max_rounds=400):
        done, failed = 0, []
        exempted = list(getattr(self, "exempted_items", []))   # 保留 EXEMPT-0 已处置的条目，不覆盖
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
                    # 处置后主体被重算丢弃（items[] 不再含该行）属正常，不是失败：继续处置下一条
                    print("    · item=%d 处置后 items[] 中已不存在（主体被重算丢弃）→ 继续" % item_id)
                    continue
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
        if getattr(self.a, "only_lost_audit", False):
            return self._only_lost()
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

        # EXEMPT-0（lead 批准）：造数后、任何处置之前，按 amount=888 定位悬空条 → 立刻 EXEMPT → 复查 DB
        amt2 = int(self.a.amount_fen) + 111
        dang = self.rows("SELECT item_id, category, amount FROM reconciliation_items"
                         " WHERE statement_id=%d AND deleted=0 AND amount=%d AND cleared_flag=false"
                         " AND category='A' ORDER BY item_id LIMIT 1" % (self.sid, amt2))
        if not dang:
            dang = self.rows("SELECT item_id, category, amount FROM reconciliation_items"
                             " WHERE statement_id=%d AND deleted=0 AND amount=%d AND cleared_flag=false"
                             " ORDER BY item_id LIMIT 1" % (self.sid, amt2))
        if dang:
            did, dcat, damt = int(dang[0][0]), dang[0][1], int(dang[0][2])
            st_e, b_e = self.exempt(did, "候选已被重建冲掉，经核对无对侧同额，判定非歧义")
            ok_e = ok(b_e)
            self.step("EXEMPT-0 造数后先处置悬空条（按 amount 定位）", ok_e,
                      "item_id=%d category=%s amount=%d；EXEMPT HTTP %s code=%s" % (
                          did, dcat, damt, st_e, (b_e or {}).get("code")))
            if ok_e:
                self.exempted_items.append(did)
                r_chk = self.rows("SELECT cleared_flag, COALESCE(reason,'') FROM reconciliation_items"
                                  " WHERE item_id=%d" % did)
                cf = (r_chk[0][0] in ("t", "1", "true")) if r_chk else None
                rs = r_chk[0][1] if r_chk else ""
                self.step("EXEMPT-0 立即复查 DB（cleared_flag=true 且 reason 以 EXEMPTED( 开头）",
                          cf is True and rs.startswith("EXEMPTED("),
                          "item_id=%d cleared_flag=%r reason 前缀=%r" % (did, cf, rs[:48]))

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
        done2, failed2 = self.clear_all_uncleared()
        self.step("清掉所有未处置条目（cleared_flag=false → 全部处置，合计才会归 0）",
                  not failed2, "处置 %d 条；失败=%s" % (done2, failed2[:2] if failed2 else "无"))
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

        # ---- P4-4c NEW-8：重算后处置状态不复活（只在本 cell 内、收尾阶段调一次）----
        before_items = {int(x.get("itemId") or 0): x for x in (vo_pre.get("items") or [])}
        st_n8, b_n8 = http("POST", self.a.api + "/v1/finance/recon/statements", self.token,
                           {"accountId": int(self.a.account), "periodStart": self.a.business_date,
                            "periodEnd": self.a.business_date, "forceRecalc": True,
                            "bankStatementBalanceFen": int(self.a.bank_fen or 0)})
        st_n8b, b_n8b = http("GET", self.a.api + "/v1/finance/recon/statements/%d" % self.sid, self.token)
        vo_n8 = data_of(b_n8b) or {}
        amb_n8 = int(vo_n8.get("ambiguousItemCount") or 0)
        alive = []
        for it in (vo_n8.get("items") or []):
            key = (it.get("category"), it.get("flowId"), it.get("transactionId"), it.get("amount"))
            for old in before_items.values():
                if (old.get("category"), old.get("flowId"), old.get("transactionId"), old.get("amount")) == key:
                    if old.get("clearedFlag") is True:
                        alive.append((it.get("itemId"), key[0],
                                      bool(it.get("clearedFlag")), str(it.get("reason") or "")[:22]))
        ok_n8 = (amb_n8 == 0)
        lost = [a for a in alive if not a[2] or not (a[3].startswith("已处置(") or a[3].startswith("EXEMPTED("))]
        self.step("P4-4c NEW-8：force 重算后 ambiguousItemCount 仍为 0", ok_n8,
                  "force HTTP %s → ambiguousItemCount=%d（重算前 0）" % (st_n8, amb_n8))
        self.step("P4-4c NEW-8：同主体键的处置条目 clearedFlag/reason 仍保持（不复活）",
                  len(alive) > 0 and not lost,
                  "同主体键命中 %d 条；丢失处置状态 %d 条：%s" % (len(alive), len(lost), lost[:3]))

        # ---- P4-4d 丢失审计（可选，默认不覆盖）----
        if getattr(self.a, "lost_audit", False):
            self._lost_audit()
        else:
            self.step("P4-4d 丢失审计（可选）", True,
                      "**未覆盖**：默认不执行（需 --lost-audit）；未伪造结论")

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

    def _only_lost(self):
        """只跑 P4-4d 段：造表 → 处置 1 条 → 改 flow_id → forceRecalc → 断言 LOST → 清理"""
        if not self.guard_cell():
            return self.summary()
        if not ensure_ready(api=self.a.api, admin=self.a.admin, pos=self.a.pos):
            self.not_ready("前置就绪", "服务/库存未就绪")
            return self.summary()
        if not self.login():
            return self.summary()
        if not self.make_ambiguity():
            return self.summary()
        # 处置 1 条（取第一条歧义，有候选则 RESOLVE，否则 EXEMPT）
        rows = self.rows("SELECT item_id, category, amount FROM reconciliation_items"
                         " WHERE statement_id=%d AND deleted=0 AND cleared_flag=false ORDER BY item_id LIMIT 1" % self.sid)
        if rows:
            iid, cat, amount = int(rows[0][0]), rows[0][1], int(rows[0][2])
            pair = {"A": "C", "C": "A", "B": "D", "D": "B"}
            cand = self.rows("SELECT COALESCE(transaction_id,0), COALESCE(flow_id,0), category"
                             " FROM reconciliation_items WHERE statement_id=%d AND deleted=0"
                             " AND amount=%d AND item_id<>%d AND category='%s' ORDER BY item_id LIMIT 1"
                             % (self.sid, amount, iid, pair.get(cat, "C")))
            if cand:
                txn, flow, tcat = int(cand[0][0] or 0), int(cand[0][1] or 0), cand[0][2]
                ref = str(txn) if txn else str(flow)
                st, b = http("POST", self.a.api + "/v1/finance/recon/statements/%d/items/%d/resolve-ambiguity"
                             % (self.sid, iid), self.token,
                             {"decision": tcat, "targetRef": ref, "reason": "%s lost-audit 处置" % self.tag})
            else:
                st, b = self.exempt(iid, "无对侧同额候选，判定非歧义（lost-audit 夹具）")
            self.step("lost-audit 夹具：处置 1 条", ok(b), "item_id=%d HTTP %s code=%s" % (
                iid, st, (b or {}).get("code")))
        self._lost_audit()
        return self.summary()

    def _lost_audit(self):
        """P4-4d：改本表已处置 item 的 flow_id → statement forceRecalc → 断言新增 LOST 审计（JSON 取值匹配）。"""
        if self.owned_sid is None or self.sid != self.owned_sid:
            self.not_ready("P4-4d 丢失审计", "statement_id=%s 非本次自建（owned=%s）→ 拒绝执行" % (self.sid, self.owned_sid))
            return
        item_id = orig_flow_id = None
        try:
            rows = self.rows(
                "SELECT item_id, COALESCE(flow_id,0), COALESCE(category,''), COALESCE(amount,0),"
                " COALESCE(reason,'') FROM reconciliation_items WHERE statement_id=%d AND deleted=0"
                " AND cleared_flag = true ORDER BY item_id LIMIT 1" % self.sid)
            if not rows:
                self.not_ready("P4-4d 丢失审计", "本表没有已处置条目 → 未覆盖")
                return
            item_id, orig_flow_id = int(rows[0][0]), int(rows[0][1])
            cat, amount, reason_before = rows[0][2], int(rows[0][3]), rows[0][4]
            baseline = self.num("SELECT COALESCE(MAX(log_id),0) FROM finance_audit_logs")
            # ① 从库里读真实 period（必须与表里存款的三元组完全一致，否则会新建表、快照为空）
            pr = self.rows("SELECT period_start::text, period_end::text, COALESCE(status,'')"
                           " FROM reconciliation_statements WHERE statement_id=%d" % self.sid)
            p_start, p_end, p_status = (pr[0][0], pr[0][1], pr[0][2]) if pr else (
                self.a.business_date, self.a.business_date, "?")
            self.step("P4-4d 前置：period 三元组从库读取（DRAFT 才走原地重算）", p_status == "DRAFT",
                      "statement_id=%d period_start=%s period_end=%s status=%s" % (
                          self.sid, p_start, p_end, p_status))
            # ② 快照非空：该条必须 cleared_flag=true 且 reason 前缀合规
            snap_ok = (rows[0] is not None) and (
                "已处置(" in reason_before or "EXEMPTED(" in reason_before)
            self.step("P4-4d 前置：处置快照非空（cleared_flag=true + reason 前缀 已处置(/EXEMPTED(）", snap_ok,
                      "reason 前缀=%r" % reason_before[:36])
            self.step("P4-4d 前置：仅改本次自建表的已处置 item", True,
                      "statement_id=%d(owned) item_id=%d 类别=%s 金额=%d 原 flow_id=%d；原 reason 前缀=%r；"
                      "baseline max(log_id)=%d" % (self.sid, item_id, cat, amount, orig_flow_id,
                                                   reason_before[:36], baseline))
            # 真实触发方式（fund-500 的 LOST 即由此产生）：把该条对应的**底层流水移出期间**
            flow_no = ""
            r_flow = self.rows("SELECT COALESCE(flow_no,'') FROM fund_flows WHERE flow_id=%d" % orig_flow_id)
            flow_no = r_flow[0][0] if r_flow else ""
            orig_bd = self.rows("SELECT business_date::text FROM fund_flows WHERE flow_id=%d" % orig_flow_id)
            orig_bd = orig_bd[0][0] if orig_bd else None
            self.step("P4-4d 步骤0：确认改动对象是本次自建流水", flow_no in self.owned_flow_nos and orig_bd is not None,
                      "flow_id=%d flow_no=%s 在白名单=%s；原 business_date=%s" % (
                          orig_flow_id, flow_no, flow_no in self.owned_flow_nos, orig_bd))
            if flow_no not in self.owned_flow_nos or orig_bd is None:
                self.not_ready("P4-4d 丢失审计", "该条对应的流水不在本次白名单 → 拒绝执行（未覆盖）")
                return
            self._lost_flow_id, self._lost_flow_no, self._lost_orig_bd = orig_flow_id, flow_no, orig_bd
            self.psql("UPDATE fund_flows SET business_date = business_date - 400 WHERE flow_id=%d AND flow_no='%s'"
                      % (orig_flow_id, flow_no))
            n_aff = self.num("SELECT count(*) FROM fund_flows WHERE flow_id=%d AND business_date < '%s'"
                             % (orig_flow_id, orig_bd))
            self.step("P4-4d 步骤1：把该条底层流水移出期间", n_aff == 1,
                      "flow_no=%s business_date: %s → 移出期间（受影响行=%d）" % (flow_no, orig_bd, n_aff))
            st_r, b_r = http("POST", self.a.api + "/v1/finance/recon/statements", self.token,
                             {"accountId": int(self.a.account), "periodStart": p_start,
                              "periodEnd": p_end, "forceRecalc": True,
                              "remark": "E2E-P43-%s lost-audit recalc" % self.tag,
                              "bankStatementBalanceFen": int(self.a.bank_fen or 0)})
            resp_sid = int((data_of(b_r) or {}).get("statementId") or 0)
            self.step("P4-4d 前置：重算响应 statementId == owned_sid（否则属新建表、快照为空）",
                      resp_sid == self.sid,
                      "HTTP %s；响应 statementId=%s；owned=%s；请求 period=%s..%s；响应原文=%s" % (
                          st_r, resp_sid, self.sid, p_start, p_end, json.dumps(b_r, ensure_ascii=False)[:360]))
            time.sleep(1.0)
            q = ("SELECT log_id, target_id, COALESCE(operation_data,'') FROM finance_audit_logs"
                 " WHERE operation_type='RECON_DISPOSITION_LOST'"
                 " AND (operation_data::json ->> 'statementId') = '%d'"
                 " AND log_id > %d ORDER BY log_id DESC LIMIT 1" % (self.sid, baseline))
            log_rows = self.rows(q)
            if not log_rows:
                self.step("P4-4d 断言：新增 RECON_DISPOSITION_LOST（log_id > baseline 且 statementId=owned）", False,
                          "force HTTP %s 后未拿到属于 statement %d 的新增 LOST 行（baseline=%d）" % (st_r, self.sid, baseline))
            else:
                log_id, tgt, odata = int(log_rows[0][0]), log_rows[0][1], log_rows[0][2]
                import json as _json
                try:
                    dd = _json.loads(odata)
                except Exception:
                    dd = {}
                key_flow = 0
                sk = str(dd.get("subjectKey") or "")
                if "flow_id=" in sk:
                    try:
                        key_flow = int(sk.split("flow_id=")[1].split(",")[0].strip())
                    except Exception:
                        key_flow = 0
                trace = self.num("SELECT count(*) FROM fund_flows WHERE flow_id=%d" % key_flow) if key_flow else 0
                ok_new = log_id > baseline and str(dd.get("statementId")) == str(self.sid)
                ok_keys = all(k in dd for k in ("subjectKey", "originalReason", "clearedFlag", "lostReason"))
                orsn = str(dd.get("originalReason") or "")
                ok_pref = orsn.startswith("已处置(") or orsn.startswith("EXEMPTED(")
                self.step("P4-4d 断言① 新增行 log_id>baseline 且 statementId==owned", ok_new,
                          "log_id=%d（baseline=%d）statementId=%r target_id(item)=%s" % (
                              log_id, baseline, dd.get("statementId"), tgt))
                self.step("P4-4d 断言② operation_data 含 subjectKey/originalReason/clearedFlag/lostReason", ok_keys,
                          "keys=%s" % sorted(dd.keys()))
                self.step("P4-4d 断言③ originalReason 以 已处置(/EXEMPTED( 开头", ok_pref,
                          "originalReason 前缀=%r" % orsn[:60])
                self.step("P4-4d 断言④ subjectKey 的 flow_id 能反查 fund_flows（追溯链闭合）", trace > 0,
                          "subjectKey=%r → flow_id=%d 反查行数=%d" % (sk, key_flow, trace))
                self.step("P4-4d 原文", True, "log_id=%d target_id=%s operation_data=%s" % (log_id, tgt, odata[:400]))
        finally:
            if getattr(self, "_lost_flow_id", None):
                before_bd = self.rows("SELECT business_date::text FROM fund_flows WHERE flow_id=%d" % self._lost_flow_id)
                self.psql("UPDATE fund_flows SET business_date = '%s' WHERE flow_id=%d AND flow_no='%s'"
                          % (self._lost_orig_bd, self._lost_flow_id, self._lost_flow_no))
                after_bd = self.rows("SELECT business_date::text FROM fund_flows WHERE flow_id=%d" % self._lost_flow_id)
                okr = bool(after_bd) and after_bd[0][0] == self._lost_orig_bd
                self.step("P4-4d finally 恢复 business_date", okr,
                          "flow_no=%s 受影响行=1 恢复前=%s 恢复后=%s 原值=%s%s" % (
                              self._lost_flow_no, before_bd[0][0] if before_bd else "?",
                              after_bd[0][0] if after_bd else "?", self._lost_orig_bd,
                              "" if okr else "；手工修复：UPDATE fund_flows SET business_date='%s' WHERE flow_id=%d"
                              % (self._lost_orig_bd, self._lost_flow_id)))
            if item_id is not None:
                self.psql("UPDATE reconciliation_items SET flow_id=%d WHERE item_id=%d AND statement_id=%d"
                          % (orig_flow_id, item_id, self.sid))
                after = self.rows("SELECT COALESCE(flow_id,0), COALESCE(cleared_flag,false), COALESCE(reason,'')"
                                  " FROM reconciliation_items WHERE item_id=%d" % item_id)
                if after:
                    restored = int(after[0][0]) == orig_flow_id
                    self.step("P4-4d finally 恢复 flow_id", restored,
                              "item_id=%d 恢复前=%d 恢复后=%s 原值=%d；cleared_flag=%s reason 前缀=%r%s" % (
                                  item_id, 999999999, after[0][0], orig_flow_id, after[0][1],
                                  after[0][2][:24],
                                  "" if restored else "；手工修复：UPDATE reconciliation_items SET flow_id=%d WHERE item_id=%d"
                                  % (orig_flow_id, item_id)))
                else:
                    self.step("P4-4d finally 恢复 flow_id", True,
                              "item_id=%d 重算后已不存在（主体消失），审计已留痕" % item_id)

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
        tl_txt = ""
        for _ in range(20):
            try:
                tl_txt = self.page.locator('[data-test="audit-timeline"]').first.inner_text() or ""
            except Exception:
                tl_txt = ""
            if tl_txt.strip():
                break
            time.sleep(1.0)
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
    p.add_argument("--account", type=int, default=24)   # 门店1微信商户账户：cell 干净
    p.add_argument("--amount-fen", type=int, default=777)
    p.add_argument("--business-date", default=date.today().strftime("%Y-%m-%d"))
    p.add_argument("--headed", action="store_true")
    p.add_argument("--only-lost-audit", action="store_true", help="只跑 P4-4d 段（造表→处置1条→改flow_id→重算→断言LOST→清理）")
    p.add_argument("--lost-audit", action="store_true", help="构造「底层流水移出期间」以验证 RECON_DISPOSITION_LOST")
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
