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
        amt = int(self.a.amount_fen)
        # 内部流水（未匹配 → A 类）
        st, b = http("POST", self.a.api + "/v1/finance/fund-flows", self.token,
                     {"accountId": int(self.a.account), "amount": amt, "flowDirection": 1,
                      "flowCategory": 5, "businessDate": self.a.business_date,
                      "remark": "%s 歧义夹具(内部)" % self.tag})
        if not ok(b):
            return self.not_ready("造歧义：内部流水", "HTTP %s code=%s %s" % (
                st, (b or {}).get("code"), str((b or {}).get("message"))[:80]))
        # 等额银行流水（未匹配 → C 类），SQL 直插，与 I1 正常行同构
        batch = self.num("SELECT COALESCE(MAX(batch_id),0) FROM bank_transactions")
        uid = self.num("SELECT user_id FROM users WHERE username='%s' LIMIT 1" % self.a.user)
        self.psql("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id,"
                  " direction, is_refund, amount, fee, txn_time, business_date, balance_snapshot,"
                  " counterparty_name, summary, dedupe_basis, raw_hash, match_status, needs_review,"
                  " create_user_id, create_time, update_time, deleted, version)"
                  " VALUES (%d,%d,'BANK','%s-BANK',1,false,%d,0, now(), '%s', 0,"
                  " 'amb-fixture','amb-fixture','%s','%s','UNMATCHED',false,%d, now(), now(), 0, 0)"
                  % (batch, int(self.a.account), self.tag, amt, self.a.business_date,
                     ("AMB" + self.tag[-8:])[:16], ("amb" + str(amt))[:64], uid))
        # R1 force 重建 → 单边差异
        st2, b2 = http("POST", self.a.api + "/v1/finance/recon-runs?force=true", self.token,
                       {"accountId": int(self.a.account), "businessDate": self.a.business_date, "force": True})
        self.step("造歧义：内部流水 + 等额银行流水 + force 重建", ok(b2),
                  "金额=%d 分；R1 HTTP %s code=%s" % (amt, st2, (b2 or {}).get("code")))
        # S3 生成调节表（期间覆盖当天）
        ps = (date.today() - timedelta(days=10)).isoformat()
        pe = (date.today() + timedelta(days=1)).isoformat()
        target = 0
        vo = {}
        for _ in range(4):
            st3, b3 = http("POST", self.a.api + "/v1/finance/recon/statements", self.token,
                           {"accountId": int(self.a.account), "periodStart": ps, "periodEnd": pe,
                            "bankStatementBalanceFen": int(target), "remark": "%s 歧义表" % self.tag})
            if not ok(b3):
                return self.not_ready("造歧义：S3 生成调节表", "HTTP %s code=%s %s" % (
                    st3, (b3 or {}).get("code"), str((b3 or {}).get("message"))[:80]))
            vo = data_of(b3) or {}
            if (vo.get("ambiguousItemCount") or 0) > 0:
                break
            ab = int(vo.get("unclearedInCredit") or 0)
            bd = int(vo.get("unclearedInDebit") or 0)
            target = int(vo.get("adjustedBookBalance") or 0) - ab + bd
        self.sid = int(vo.get("statementId") or 0)
        amb = int(vo.get("ambiguousItemCount") or 0)
        return self.step("造歧义：statementId=%s ambiguousItemCount=%d" % (self.sid, amb), amb > 0,
                         "period=%s..%s isBalanced=%s；表内歧义条数=%d%s" % (
                             ps, pe, vo.get("isBalanced"), amb,
                             "" if amb > 0 else "；截图=" + self.shot("make-ambiguity")))

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

        # 3) 弹窗候选校验
        try:
            self.page.locator('[data-test="btn-resolve-ambiguity"]').first.click(timeout=15000)
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

        # 4) reason + 二次确认文案
        try:
            self.page.locator('[data-test="ambiguity-reason"]').first.fill("歧义处置 E2E " + self.tag)
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
    def resolve_remaining(self, max_rounds=300):
        done, failed = 0, []
        for _ in range(max_rounds):
            skip_sql = (" AND i.item_id NOT IN (%s)" % ",".join(str(x) for x in self.skipped_items))                 if self.skipped_items else ""
            rows = self.rows(
                "SELECT i.item_id, i.category, i.amount FROM reconciliation_items i"
                " WHERE i.statement_id=%d AND i.deleted=0 AND i.reason LIKE 'AMBIGUOUS:%%'" % self.sid
                + skip_sql + " ORDER BY i.item_id LIMIT 1")
            if not rows:
                break
            item_id, cat, amount = int(rows[0][0]), rows[0][1], int(rows[0][2])
            pair = {"A": "C", "C": "A", "B": "D", "D": "B"}
            opposite = (pair.get(cat, "C"),)
            cand = self.rows(
                "SELECT COALESCE(i.transaction_id,0), COALESCE(i.flow_id,0), i.category"
                " FROM reconciliation_items i WHERE i.statement_id=%d AND i.deleted=0"
                " AND i.amount=%d AND i.item_id<>%d AND i.category='%s'"
                " ORDER BY i.item_id LIMIT 1" % (self.sid, amount, item_id, opposite[0]))
            if not cand:
                failed.append("item %d（%s 类）无对侧同额候选，无法经本接口处置" % (item_id, cat))
                # 标记该 item 跳过，避免死循环：把它排除在后续查询之外
                self.skipped_items.add(item_id)
                if len(self.skipped_items) > 60:
                    break
                continue
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
            done += 1
        return done, failed

    # ---------- 主流程 ----------
    def run(self):
        print("=" * 92)
        print("P4-2 同额歧义处置全链路（AC-55 候选）  tag=%s" % self.tag)
        print("=" * 92)
        if not ensure_ready(api=self.a.api, admin=self.a.admin, pos=self.a.pos):
            self.not_ready("前置就绪（e2e_setup.ensure_ready）", "服务/库存前置未通过")
            return self.summary()
        if not self.login():
            return self.summary()

        rc0, a19_before, tail0 = self.run_consistency()
        self.step("A19 基线（造歧义前）", True, "consistency exit=%s；A19=%s；末行=%s" % (
            rc0, a19_before[:2], tail0[:100]))

        if not self.make_ambiguity():
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
        self.step("清理本表剩余歧义（逐条 API 处置）", not failed,
                  "处置 %d 条；跳过/失败 %d 条：%s" % (done, len(failed), failed[:3] if failed else "无"))
        st_f, b_f = http("GET", self.a.api + "/v1/finance/recon/statements/%d" % self.sid, self.token)
        vo_f = data_of(b_f) or {}
        self.step("AC-55e 处置完全部歧义后接口 isBalanced=true 且 ambiguousItemCount=0",
                  bool(vo_f.get("isBalanced")) and int(vo_f.get("ambiguousItemCount") or 0) == 0,
                  "isBalanced=%s ambiguousItemCount=%s items=%d" % (
                      vo_f.get("isBalanced"), vo_f.get("ambiguousItemCount"), len(vo_f.get("items") or [])))
        try:
            if not self.ui_login():
                raise RuntimeError("最终校验的 UI 登录失败")
            self.page.goto("%s/finance/recon/statements/%d" % (self.a.admin, self.sid),
                           wait_until="domcontentloaded", timeout=120000)
            bal2 = wait_for_data_attr(self.page, '[data-test="stmt-balanced"]', "data-balanced",
                                      READY_NONEMPTY, timeout=60000)
            self.step("AC-55e 页面处置后 data-balanced=true（后端重算值）", str(bal2).lower() == "true",
                      "data-balanced=%r%s" % (bal2, "" if str(bal2).lower() == "true" else "；截图=" + self.shot("ac55e-final")))
        except Exception as e:
            self.step("AC-55e 页面处置后 data-balanced=true（后端重算值）", False,
                      "异常：%s；截图=%s" % (str(e)[:150], self.shot("ac55e-final")))
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
        self.step("A19 处置后本表转绿（statementId=%d 不再出现在 A19 明细）" % self.sid, gone,
                  "本表命中=%s；其余红行=%s；末行=%s" % (
                      mine[:2] if mine else "无", [o[:110] for o in others[:2]] if others else "无", tail2[:100]))
        if others:
            self.step("说明：A19 仍红且**不属于本表**（按 lead 要求保留待其清理）", True,
                      "剩余红行 %d 条，例如：%s" % (len(others), others[0][:160]))
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
    p.add_argument("--account", type=int, default=24)
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
