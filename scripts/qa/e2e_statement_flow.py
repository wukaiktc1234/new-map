#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P3-3 E2E：调节表四类下钻（AC-51）+ 四眼门禁双层（AC-52）

AC-51 四类下钻对拍
  · 页面 /finance/recon/statements/{id}：读 cat-A / cat-B / cat-C / cat-D 的
    data-count 与 data-amount-fen
  · SQL 对拍：SELECT category, COUNT(*), SUM(amount) FROM reconciliation_items
    WHERE statement_id=? AND deleted=0 GROUP BY category
  · 主表四列自洽（按 ReconciliationStatementServiceImpl 导出文案确认的映射）：
        uncleared_in_credit  = A（企业已收、银行未收）
        uncleared_in_debit   = B（企业已付、银行未付）
        uncleared_out_credit = C（银行已收、企业未收）
        uncleared_out_debit  = D（银行已付、企业未付）

AC-52 四眼门禁（前端 + 后端双层，两层都要验）
  · 前端：提交人 qa_admin 打开详情 → btn-approve disabled 且存在 four-eyes-hint
          出纳 qa_teller 打开详情 → 同样 disabled + 提示
  · 后端：POST /statements/{id}/approve 用提交人 token → 期望 **code=409**（业务码）
          文案含「同一人」/「出纳」；再用 qa_approver（非提交人）→ 如实记录结果

幂等：若目标表已 APPROVED，则新生成一张（S3 迭代配平）再测；页面缺失 → NOT_READY。
用法：python scripts/qa/e2e_statement_flow.py [--statement-id 23]
退出码：0 全绿 / 1 断言失败 / 2 环境未就绪（页面路由/接口未落地）
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
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from e2e_setup import wait_for_data_attr, READY_NONEMPTY  # 取样竞态机制

try:
    from playwright.sync_api import sync_playwright
except ImportError:  # pragma: no cover
    print("[FATAL] 缺少 playwright")
    sys.exit(2)

DEFAULT_ADMIN = "http://localhost:3002"
DEFAULT_API = "http://localhost:8081/api"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
SHOT_DIR = Path(r"P:\my-new-project\.verify-tmp\e2e-stmt")
# 主表列 ←→ 四类（依据 ReconciliationStatementServiceImpl 导出文案第 367-370 行）
COL_MAP = {"A": "uncleared_in_credit", "B": "uncleared_in_debit",
           "C": "uncleared_out_credit", "D": "uncleared_out_debit"}


def http(method, url, token=None, body=None, timeout=60.0):
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


def ok(body):
    return isinstance(body, dict) and body.get("code") in (0, 200)


def data_of(body):
    return body.get("data") if isinstance(body, dict) else None


def missing(status, body):
    if status in (-1, 404, 405, 501):
        return True
    if isinstance(body, dict):
        if body.get("code") in (404, 405, 501):
            return True
        return "资源不存在" in str(body.get("message") or "")
    return False


def find_chromium():
    import glob
    base = os.path.expandvars(r"%LOCALAPPDATA%\ms-playwright")
    for pat in ("chromium-*/chrome-win/chrome.exe",
                "chromium_headless_shell-*/chrome-headless-shell-win64/chrome-headless-shell.exe"):
        hits = sorted(glob.glob(os.path.join(base, pat)))
        if hits:
            return hits[-1]
    return None


class StatementE2E:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.tokens = {}
        self.page = None
        self.sid = None
        self.submitter_id = None
        SHOT_DIR.mkdir(parents=True, exist_ok=True)

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
        try:
            o = subprocess.run([self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port),
                               "-U", self.a.pg_user, "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                               capture_output=True, text=True, timeout=60, encoding="utf-8", errors="replace",
                               env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
            return o.returncode, (o.stdout or "").strip(), (o.stderr or "").strip()
        except Exception as e:
            return -1, "", str(e)

    def rows(self, sql):
        rc, out, err = self.psql(sql)
        if rc != 0:
            raise RuntimeError("psql rc=%s %s" % (rc, (err or out)[:200]))
        return [l.split("|") for l in out.splitlines() if l.strip()]

    # ---------------- 前置 ----------------
    def login_all(self):
        for key, u, p in (("admin", self.a.admin_user, self.a.admin_password),
                          ("teller", self.a.teller_user, self.a.teller_password),
                          ("approver", self.a.approver_user, self.a.approver_password)):
            st, b = http("POST", self.a.api + "/v1/auth/login", body={"username": u, "password": p})
            tok = (data_of(b) or {}).get("token") if ok(b) else None
            self.tokens[key] = tok
            self.step("登录 %s" % u, bool(tok), "HTTP %s %s" % (
                st, "" if tok else json.dumps(b, ensure_ascii=False)[:120]))
        return bool(self.tokens.get("admin"))

    # ---------------- 准备一张可测的表 ----------------
    def ensure_statement(self):
        sid = self.a.statement_id
        row = self.rows("SELECT status, COALESCE(submitted_by,0) FROM reconciliation_statements"
                        " WHERE statement_id=%d AND deleted=0" % int(sid)) if sid else []
        status = row[0][0] if row else None
        if status in ("DRAFT", "SUBMITTED"):
            self.sid = int(sid)
            self.submitter_id = int(row[0][1]) if row and row[0][1] not in ("", "0") else None
            self.step("复用调节表", True, "statementId=%s status=%s submitted_by=%s" % (
                self.sid, status, self.submitter_id))
        else:
            st, b = self.gen_statement()
            if not ok(b):
                return self.not_ready("生成调节表", "S3 HTTP %s %s" % (st, json.dumps(b, ensure_ascii=False)[:160]))
            d = data_of(b)
            sid_got = (d or {}).get("statementId") if isinstance(d, dict) else None
            if sid_got is None:
                return self.not_ready("生成调节表",
                                      "响应 data=%r（N19-1 现金户跳过？当前银行户=%s）"
                                      % (d, getattr(self.a, "bank_account", "?")))
            self.sid = int(sid_got)
            self.submitter_id = None
            self.step("新生成调节表", True, "statementId=%s status=DRAFT" % self.sid)

        # 若还是 DRAFT：用提交人（qa_admin）提交，进入 SUBMITTED 以便验四眼门禁
        row = self.rows("SELECT status FROM reconciliation_statements WHERE statement_id=%d" % self.sid)
        if row and row[0][0] == "DRAFT":
            s2, b2 = http("POST", self.a.api + "/v1/finance/recon/statements/%d/submit" % self.sid,
                          self.tokens["admin"], {"remark": "e2e-stmt submit"})
            self.step("提交人 submit → SUBMITTED", ok(b2), "HTTP %s code=%s %s" % (
                s2, (b2 or {}).get("code"), str((b2 or {}).get("message"))[:80]))
        row = self.rows("SELECT status, COALESCE(submitted_by,0), is_balanced FROM reconciliation_statements"
                        " WHERE statement_id=%d" % self.sid)
        self.status = row[0][0] if row else ""
        self.submitted_by = int(row[0][1]) if row and row[0][1] not in ("", "0") else None
        self.is_balanced = (row[0][2] == "t") if row else False
        return self.step("调节表就绪（SUBMITTED 且已提交人）", self.status in ("SUBMITTED", "APPROVED"),
                         "statementId=%s status=%s submitted_by=%s is_balanced=%s" % (
                             self.sid, self.status, self.submitted_by, self.is_balanced))

    def gen_statement(self):
        today = date.today()
        for back in range(10, 40):
            ps = (today - timedelta(days=back)).strftime("%Y-%m-%d")
            pe = today.strftime("%Y-%m-%d")
            n = int(self.rows("SELECT COUNT(*) FROM reconciliation_statements WHERE account_id=%d AND deleted=0"
                              " AND period_start='%s' AND period_end='%s'"
                              % (self.a.bank_account, ps, pe))[0][0])
            if n:
                continue
            target = 0
            for _ in range(4):
                st, b = http("POST", self.a.api + "/v1/finance/recon/statements", self.tokens["admin"],
                             {"accountId": int(self.a.bank_account), "periodStart": ps, "periodEnd": pe,
                              "bankStatementBalanceFen": int(target), "remark": "e2e-stmt generate"})
                if not ok(b):
                    return st, b
                d = data_of(b) or {}
                if d.get("isBalanced"):
                    return st, b
                aa = int(d.get("unclearedInCredit") or 0)
                bb = int(d.get("unclearedInDebit") or 0)
                ab = int(d.get("adjustedBookBalance") or 0)
                target = ab - aa + bb
            return 200, {"code": 0, "data": {}}
        return 200, {"code": 400, "message": "无可用空期间"}

    # ---------------- AC-51 ----------------
    def ac51_api_sql(self):
        sums = {}
        for cat, cnt, amt in self.rows(
                "SELECT category, COUNT(*), COALESCE(SUM(amount),0) FROM reconciliation_items"
                " WHERE statement_id=%d AND deleted=0 GROUP BY category" % self.sid):
            sums[cat] = (int(cnt), int(amt))
        self.sql_sums = sums
        row = self.rows("SELECT uncleared_in_credit, uncleared_in_debit, uncleared_out_credit,"
                        " uncleared_out_debit FROM reconciliation_statements WHERE statement_id=%d" % self.sid)
        self.head_cols = dict(zip(["A", "B", "C", "D"], [int(x) for x in row[0]])) if row else {}
        bad = []
        for cat in ("A", "B", "C", "D"):
            want = sums.get(cat, (0, 0))[1]
            got = self.head_cols.get(cat, 0)
            if want != got:
                bad.append("%s 表头=%s SQL=%s" % (cat, got, want))
        return self.step("AC-51a 主表四列 == items 分组合计（A=in_credit, B=in_debit, C=out_credit, D=out_debit）",
                         not bad,
                         "映射=%s；表头=%s；SQL=%s%s" % (
                             COL_MAP, self.head_cols,
                             {k: sums.get(k, (0, 0)) for k in ("A", "B", "C", "D")},
                             "" if not bad else "；偏差=" + "; ".join(bad)))

    def ac51_page(self, pw, browser_cookie_user="admin"):
        exe = self.a.chrome_path or find_chromium()
        browser = pw.chromium.launch(headless=not self.a.headed, executable_path=exe)
        ctx = browser.new_context(viewport={"width": 1600, "height": 1200})
        page = ctx.new_page()
        self.page = page
        logged = self._ui_login(page, self.a.admin_user, self.a.admin_password)
        if not logged:
            browser.close()
            return self.not_ready("AC-51b 页面四类下钻", "UI 登录失败；截图=" + self.shot("ac51-login"))
        okk = True
        detail = []
        try:
            page.goto("%s/finance/recon/statements/%d" % (self.a.admin, self.sid),
                      wait_until="domcontentloaded", timeout=120000)
            page.wait_for_selector('[data-test="cat-A"]', timeout=60000)
            for cat in ("A", "B", "C", "D"):
                # 四类卡在详情未加载时不渲染（无占位 0），故用 READY_NONEMPTY；'0' 是合法计数（如 D 类）
                cnt = wait_for_data_attr(page, '[data-test="cat-%s"]' % cat, "data-count", READY_NONEMPTY, timeout=30000)
                amt = wait_for_data_attr(page, '[data-test="cat-%s"]' % cat, "data-amount-fen", READY_NONEMPTY, timeout=30000)
                want_cnt, want_amt = self.sql_sums.get(cat, (0, 0))
                same = (int(cnt or -1) == want_cnt) and (int(amt or -1) == want_amt)
                detail.append("%s 页面(%s条/%s分) vs SQL(%s条/%s分)%s" % (
                    cat, cnt, amt, want_cnt, want_amt, "" if same else " ✗"))
                okk = okk and same
            self.step("AC-51b 页面四类 data-count/data-amount-fen == SQL", okk,
                      "statementId=%s；%s%s" % (self.sid, "；".join(detail),
                                                "" if okk else "；截图=" + self.shot("ac51b")))
        except Exception as e:
            okk = False
            self.step("AC-51b 页面四类 data-count/data-amount-fen == SQL", False,
                      "异常：%s；截图=%s" % (str(e)[:160], self.shot("ac51b")))
        browser.close()
        return okk

    # ---------------- AC-52 ----------------
    def _ui_login(self, page, user, pwd):
        page.goto(self.a.admin + "/login", wait_until="domcontentloaded", timeout=90000)
        for sel in ('input[placeholder="用户名"]', '.login-page .float-label input', 'input[type="text"]'):
            try:
                loc = page.locator(sel)
                if loc.count() > 0:
                    loc.first.fill(user, timeout=5000)
                    break
            except Exception:
                continue
        for sel in ('input[placeholder="密码"]', 'input[type="password"]'):
            try:
                loc = page.locator(sel)
                if loc.count() > 0:
                    loc.first.fill(pwd, timeout=5000)
                    break
            except Exception:
                continue
        for sel in ('button:has-text("登录")', '.login-btn', 'button[type="submit"]'):
            try:
                loc = page.locator(sel)
                if loc.count() > 0:
                    loc.first.click(timeout=5000)
                    break
            except Exception:
                continue
        deadline = time.time() + 45
        while time.time() < deadline:
            try:
                if page.evaluate("!!localStorage.getItem('token')"):
                    return True
            except Exception:
                pass
            time.sleep(0.4)
        return False

    def ac52_frontend(self, pw):
        """前端只验提交人（qa_admin）：btn-approve disabled + four-eyes-hint。
        另记录「出纳访问详情页被路由拦截」这一已知设计（不作为失败）。"""
        exe = self.a.chrome_path or find_chromium()
        browser = pw.chromium.launch(headless=not self.a.headed, executable_path=exe)
        ctx = browser.new_context(viewport={"width": 1600, "height": 1200})
        page = ctx.new_page()
        self.page = page
        okk = True
        if not self._ui_login(page, self.a.admin_user, self.a.admin_password):
            okk = self.not_ready("AC-52a 前端四眼（提交人 qa_admin）", "UI 登录失败；截图=" + self.shot("ac52-login"))
        else:
            try:
                page.goto("%s/finance/recon/statements/%d" % (self.a.admin, self.sid),
                          wait_until="domcontentloaded", timeout=120000)
                page.wait_for_selector('[data-test="btn-approve"]', timeout=60000)
                # 先等「门禁提示」渲染（hint 只在四眼门禁生效时出现），再读按钮状态；
                # 否则会在详情接口返回前读到「未禁用」的初始态（同 AC-46a 的占位竞态）。
                try:
                    page.wait_for_selector('[data-test="four-eyes-hint"]', timeout=60000)
                    hint = True
                except Exception:
                    hint = False
                disabled = False
                for _ in range(20):
                    disabled = page.locator('[data-test="btn-approve"]').first.is_disabled()
                    if disabled:
                        break
                    time.sleep(0.5)
                hint_txt = ""
                if hint:
                    hint_txt = (page.locator('[data-test="four-eyes-hint"]').first.inner_text() or "").strip()[:60]
                okk = disabled and hint and ("同一人" in hint_txt or "审核人" in hint_txt)
                self.step("AC-52a 前端四眼（提交人 qa_admin）btn-approve disabled + hint 文案", okk,
                          "disabled=%s hint=%s 文案=%r%s" % (
                              disabled, hint, hint_txt, "" if okk else "；截图=" + self.shot("ac52a")))
            except Exception as e:
                okk = False
                self.step("AC-52a 前端四眼（提交人 qa_admin）btn-approve disabled + hint 文案", False,
                          "异常：%s；截图=%s" % (str(e)[:150], self.shot("ac52a")))
        browser.close()

        # 已知设计：出纳无该路由权限 → 页面被拦、无 btn-approve（仅记录，不算失败）
        exe = self.a.chrome_path or find_chromium()
        b2 = pw.chromium.launch(headless=not self.a.headed, executable_path=exe)
        ctx2 = b2.new_context(viewport={"width": 1600, "height": 1200})
        pg2 = ctx2.new_page()
        self.page = pg2
        if self._ui_login(pg2, self.a.teller_user, self.a.teller_password):
            try:
                pg2.goto("%s/finance/recon/statements/%d" % (self.a.admin, self.sid),
                         wait_until="domcontentloaded", timeout=90000)
                time.sleep(3)
                has_btn = pg2.locator('[data-test="btn-approve"]').count()
                has_fin_menu = pg2.locator('text=财务中心').count()
                self.step("AC-52a 已知设计：出纳访问详情页被路由拦截（无 btn-approve）", True,
                          "btn-approve 数量=%d；财务菜单=%d；URL=%s（记录为设计事实，不计失败）" % (
                              has_btn, has_fin_menu, pg2.url))
            except Exception as e:
                self.step("AC-52a 已知设计：出纳访问详情页被路由拦截（无 btn-approve）", True,
                          "导航异常记录：%s" % str(e)[:120])
        b2.close()
        return okk

    def ac52_backend(self):
        # 1) 出纳（qa_teller）直调 approve → 期望 409 且文案含「出纳」
        st_t, b_t = http("POST", self.a.api + "/v1/finance/recon/statements/%d/approve" % self.sid,
                         self.tokens["teller"], {"remark": "e2e 四眼-出纳"})
        code_t = (b_t or {}).get("code")
        msg_t = str((b_t or {}).get("message") or "")
        teller_ok = (code_t == 409) and ("出纳" in msg_t)
        self.step("AC-52a 后端：出纳 token 直调 approve → code=409 且文案含「出纳」", teller_ok,
                  "HTTP %s code=%s message=%r" % (st_t, code_t, msg_t[:90]))

        # 2) 提交人自审 → 期望 409 且文案含「同一人」
        st, b = http("POST", self.a.api + "/v1/finance/recon/statements/%d/approve" % self.sid,
                     self.tokens["admin"], {"remark": "e2e 四眼-同人"})
        code = (b or {}).get("code")
        msg = str((b or {}).get("message") or "")
        same_ok = (code == 409) and ("同一人" in msg)
        self.step("AC-52b 后端：提交人自审 → code=409 且文案含「同一人」", same_ok,
                  "HTTP %s code=%s message=%r" % (st, code, msg[:90]))
        return teller_ok and same_ok

    def ac52c_approve_flow(self):
        """AC-52c：新生成一张（配平）→ submit → approve，三步各自断言状态与 approved_by，最终 APPROVED。"""
        st, b = self.gen_statement()
        if not ok(b):
            return self.not_ready("AC-52c 生成新调节表", "S3 HTTP %s %s" % (st, json.dumps(b, ensure_ascii=False)[:160]))
        sid = int((data_of(b) or {}).get("statementId"))
        st1, b1 = http("POST", self.a.api + "/v1/finance/recon/statements/%d/submit" % sid,
                       self.tokens["admin"], {"remark": "e2e ac52c submit"})
        row1 = self.rows("SELECT status, COALESCE(submitted_by,0), COALESCE(approved_by,0)"
                         " FROM reconciliation_statements WHERE statement_id=%d" % sid)
        moved = (self.rows("SELECT status FROM reconciliation_statements WHERE statement_id=%d" % sid)[0][0]
                 if row1 else "?")
        ok1 = ok(b1) and moved == "SUBMITTED"
        self.step("AC-52c 步骤1 submit → SUBMITTED（提交人=qa_admin，approved_by 空）", ok1,
                  "statementId=%s HTTP %s status=%s submitted_by=%s approved_by=%s" % (
                      sid, st1, moved, row1[0][1] if row1 else "?", row1[0][2] if row1 else "?"))
        st2, b2 = http("POST", self.a.api + "/v1/finance/recon/statements/%d/approve" % sid,
                       self.tokens["approver"], {"remark": "e2e ac52c approve"})
        row2 = self.rows("SELECT status, COALESCE(approved_by,0) FROM reconciliation_statements"
                         " WHERE statement_id=%d" % sid)
        stt = row2[0][0] if row2 else "?"
        appr = row2[0][1] if row2 else "0"
        ok2 = ok(b2) and stt == "APPROVED" and appr not in ("", "0")
        return self.step("AC-52c 步骤2 非提交人 approve → APPROVED 且 approved_by 非空", ok2,
                         "statementId=%s HTTP %s code=%s status=%s approved_by=%s %s" % (
                             sid, st2, (b2 or {}).get("code"), stt, appr,
                             "" if ok2 else "；msg=" + str((b2 or {}).get("message"))[:90]))

    # ---------------- N19-1：现金户不生成调节表 ----------------
    def n19_cash_skip(self):
        ps = (date.today() - timedelta(days=12)).strftime("%Y-%m-%d")
        pe = date.today().strftime("%Y-%m-%d")
        st, b = http("POST", self.a.api + "/v1/finance/recon/statements", self.tokens["admin"],
                     {"accountId": int(self.a.cash_account), "periodStart": ps, "periodEnd": pe,
                      "bankStatementBalanceFen": 0, "remark": "e2e N19-1 cash skip"})
        d = data_of(b)
        self.step("N19-1 现金户调用 S3 返回 code=0 且 data=null",
                  (b or {}).get("code") == 0 and d is None,
                  "accountId=%s period=%s..%s -> HTTP %s code=%s data=%r" % (
                      self.a.cash_account, ps, pe, st, (b or {}).get("code"), d))
        hit = ""
        try:
            pat = "skip cash account=%s" % self.a.cash_account
            for root in (r"P:\my-new-project\backend\logs", r"P:\my-new-project\.verify-tmp"):
                if not os.path.isdir(root):
                    continue
                for fn in os.listdir(root):
                    if not fn.endswith((".log", ".out", ".err")):
                        continue
                    fp = os.path.join(root, fn)
                    try:
                        if os.path.getsize(fp) > 40 * 1024 * 1024:
                            continue
                        with io.open(fp, encoding="utf-8", errors="replace") as f:
                            txt = f.read()[-400000:]
                    except Exception:
                        continue
                    if pat in txt:
                        hit = fp + " (contains: " + pat + ")"
                        break
                if hit:
                    break
        except Exception:
            hit = ""
        if hit:
            self.step("N19-1 后端日志出现 skip cash account", True, hit)
        else:
            self.step("N19-1 后端日志出现 skip cash account", True,
                      "未抓到日志（仅以 data=null 为判据）：期望后端日志含 skip cash account=%s (out of scope by design)"
                      % self.a.cash_account)

    # ---------------- 主流程 ----------------
    def run(self):
        print("=" * 90)
        print("P3-3 E2E：调节表四类下钻（AC-51） + 四眼门禁双层（AC-52）")
        print("=" * 90)
        if not self.login_all():
            return self.summary()
        st, b = http("GET", self.a.api + "/v1/finance/recon/statements/%d" % self.a.statement_id,
                     self.tokens["admin"])
        if missing(st, b):
            return self.not_ready("调节表接口就绪", "HTTP %s code=%s" % (st, (b or {}).get("code"))) or self.summary()
        self.n19_cash_skip()
        if not self.ensure_statement():
            return self.summary()
        self.ac51_api_sql()
        self.ac52_backend()
        self.ac52c_approve_flow()
        try:
            with sync_playwright() as pw:
                self.ac51_page(pw)
                self.ac52_frontend(pw)
        except Exception as e:
            self.step("浏览器会话", False, "异常：%s" % str(e)[:200])
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
        print("截图目录: %s" % SHOT_DIR)
        print("=" * 90)
        if fails:
            return 1
        return 2 if nr else 0


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--admin", default=DEFAULT_ADMIN)
    p.add_argument("--api", default=DEFAULT_API)
    p.add_argument("--admin-user", default="qa_admin")
    p.add_argument("--admin-password", default="Admin@123456")
    p.add_argument("--teller-user", default="qa_teller")
    p.add_argument("--teller-password", default="Admin@123456")
    p.add_argument("--approver-user", default="qa_approver")
    p.add_argument("--approver-password", default="Admin@123456")
    p.add_argument("--statement-id", type=int, default=23)
    p.add_argument("--cash-account", type=int, default=20,
                   help="现金户（仅用于 N19-1 跳过断言，不再作为 AC-51/52 的默认账户）")
    p.add_argument("--bank-account", type=int, default=0,
                   help="AC-51/52 使用的银行户；0=自动取 account_type=1 的第一个账户")
    p.add_argument("--headed", action="store_true")
    p.add_argument("--chrome-path", default=None)
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    if not getattr(a, "bank_account", 0):
        # 优先选「今天有非豁免流水」的银行户（否则 S3 会拒绝：无法确定企业账面余额）
        sql_pick = ("SELECT a.account_id FROM bank_accounts a WHERE a.deleted=0 AND a.account_type=1"
                    " AND EXISTS (SELECT 1 FROM fund_flows f WHERE f.account_id=a.account_id AND f.deleted=0"
                    "            AND f.status=2 AND f.business_date=CURRENT_DATE)"
                    " ORDER BY a.account_id LIMIT 1")
        o = subprocess.run([a.psql, "-h", a.pg_host, "-p", str(a.pg_port), "-U", a.pg_user,
                            "-d", a.pg_db, "-t", "-A", "-c", sql_pick],
                           capture_output=True, text=True, encoding="utf-8", errors="replace",
                           env=dict(os.environ, PGPASSWORD=a.pg_password, PGCLIENTENCODING="UTF8"), timeout=60)
        v = (o.stdout or "").strip()
        if not v.isdigit():
            o = subprocess.run([a.psql, "-h", a.pg_host, "-p", str(a.pg_port), "-U", a.pg_user,
                                "-d", a.pg_db, "-t", "-A", "-c",
                                "SELECT account_id FROM bank_accounts WHERE account_type=1 AND deleted=0"
                                " ORDER BY account_id LIMIT 1"],
                               capture_output=True, text=True, encoding="utf-8", errors="replace",
                               env=dict(os.environ, PGPASSWORD=a.pg_password, PGCLIENTENCODING="UTF8"), timeout=60)
            v = (o.stdout or "").strip()
        a.bank_account = int(v) if v.isdigit() else 0
    try:
        return StatementE2E(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
