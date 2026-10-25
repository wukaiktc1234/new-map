#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P3-2 E2E：门店勾稽（/finance/store-recon）与渠道手续费汇总

AC-49（跨店越权）
  · qa_store_b（store_manager，location_id=2）调 GET /v1/finance/store-recon/daily?storeId=2 → 200 且 storeId=2
  · 同账号调 storeId=1 → HTTP 200 + body code=403（项目风格）或 HTTP 403，**断言文案含「无权」/「权限」**
    （即越权拒绝，不是「资源不存在」）
  · 页面层：打开 /finance/store-recon → 选店铺 1 → 出现 [data-test="store-recon-forbidden"]

AC-50（渠道手续费汇总对拍）
  · qa_admin 调 GET /v1/finance/channel-fee-summary
  · 与 SQL 对拍 diff_count / fee_total / diff_amount 三个字段
  · 页面 [data-test="channel-fee-total"] 的 data-fee-fen / data-diff-fen / data-count 与接口一致

用法：
  python scripts/qa/e2e_store_recon.py
  python scripts/qa/e2e_store_recon.py --store-b 2 --other-store 1
退出码：0 全绿 / 1 断言失败 / 2 环境未就绪（前端·后端·端点未落地）
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
from datetime import datetime
from pathlib import Path

try:
    from playwright.sync_api import sync_playwright
except ImportError:  # pragma: no cover
    print("[FATAL] 缺少 playwright：python -m pip install playwright && python -m playwright install chromium")
    sys.exit(2)

DEFAULT_ADMIN = "http://localhost:3002"
DEFAULT_API = "http://localhost:8081/api"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
SHOT_DIR = Path(r"P:\my-new-project\.verify-tmp\e2e-store")


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


def is_missing(status, body):
    if status in (-1, 404, 405, 501):
        return True
    if isinstance(body, dict):
        if body.get("code") in (404, 405, 501):
            return True
        msg = str(body.get("message") or "")
        return "资源不存在" in msg or "not found" in msg.lower()
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


class StoreReconE2E:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.admin = None
        self.store_b = None
        self.page = None
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
    def preflight(self):
        st1, b1 = http("POST", self.a.api + "/v1/auth/login",
                       body={"username": self.a.admin_user, "password": self.a.admin_password})
        self.admin = (data_of(b1) or {}).get("token") if ok(b1) else None
        st2, b2 = http("POST", self.a.api + "/v1/auth/login",
                       body={"username": self.a.store_user, "password": self.a.store_password})
        self.store_b = (data_of(b2) or {}).get("token") if ok(b2) else None
        if not (self.admin and self.store_b):
            return self.not_ready("后端 8081 与账号登录", "admin=%s store_b=%s" % (
                json.dumps(b1, ensure_ascii=False)[:120], json.dumps(b2, ensure_ascii=False)[:120]))
        self.step("账号登录（qa_admin / %s）" % self.a.store_user, True, "两账号 token 就绪")
        st, b = http("GET", self.a.api + "/v1/finance/store-recon/daily?storeId=%d" % self.a.store_b, self.admin)
        if is_missing(st, b):
            return self.not_ready("P3-2 端点就绪（store-recon/daily、channel-fee-summary）",
                                  "GET store-recon/daily → HTTP %s code=%s %s（前端/后端尚未落地）" % (
                                      st, (b or {}).get("code"), str((b or {}).get("message"))[:60]))
        return self.step("P3-2 端点就绪", True, "store-recon/daily HTTP %s" % st)

    # ---------------- AC-49 API ----------------
    def ac49_api(self):
        st, b = http("GET", self.a.api + "/v1/finance/store-recon/daily?storeId=%d" % self.a.store_b, self.store_b)
        d = data_of(b) or {}
        got_store = d.get("storeId") if isinstance(d, dict) else None
        ok_own = st == 200 and ok(b) and int(got_store or 0) == int(self.a.store_b)
        self.step("AC-49a 本店（storeId=%d）可查" % self.a.store_b, ok_own,
                  "HTTP %s code=%s storeId=%s %s" % (st, (b or {}).get("code"), got_store,
                                                     "" if ok_own else "；截图=" + self.shot("ac49a")))

        st2, b2 = http("GET", self.a.api + "/v1/finance/store-recon/daily?storeId=%d" % self.a.other_store, self.store_b)
        code2 = (b2 or {}).get("code")
        msg2 = str((b2 or {}).get("message") or "")
        denied = (st2 == 403) or (st2 == 200 and code2 == 403)
        is_authz = any(k in msg2 for k in ("无权", "权限", "禁止", "forbidden", "denied"))
        not_missing = not is_missing(st2, b2)
        okk = denied and is_authz and not_missing
        return self.step("AC-49b 跨店（storeId=%d）被拒且判定为越权" % self.a.other_store, okk,
                         "HTTP %s code=%s message=%r；denied=%s authz文案=%s 非资源不存在=%s%s" % (
                             st2, code2, msg2[:80], denied, is_authz, not_missing,
                             "" if okk else "；截图=" + self.shot("ac49b")))

    # ---------------- AC-50 API + SQL ----------------
    def ac50_api(self):
        st, b = http("GET", self.a.api + "/v1/finance/recon/channel-fee-summary", self.admin)
        if is_missing(st, b):
            return self.not_ready("AC-50 渠道手续费汇总端点", "HTTP %s code=%s" % (st, (b or {}).get("code")))
        d = data_of(b) or {}
        total = d.get("total") or {}
        rows_api = d.get("rows") or []
        api_count = int(total.get("diffCount") or 0)
        api_fee = int(total.get("feeTotalFen") or 0)
        api_diff = int(total.get("diffAmountFen") or 0)
        # SQL：CHANNEL_FEE 差异 + 关联外部流水的 fee
        rows = self.rows(
            "SELECT COUNT(*), COALESCE(SUM(COALESCE(bt.fee,0)),0), COALESCE(SUM(COALESCE(rd.diff_amount,0)),0)"
            " FROM recon_differences rd"
            " LEFT JOIN bank_transactions bt ON bt.external_txn_id = rd.external_ref AND bt.deleted=0"
            " WHERE rd.difference_type='CHANNEL_FEE'")
        sql_count, sql_fee, sql_diff = (int(rows[0][0]), int(rows[0][1]), int(rows[0][2])) if rows else (0, 0, 0)
        okk = api_count == sql_count and api_fee == sql_fee and api_diff == sql_diff
        return self.step("AC-50a 接口 total 三字段 == SQL 对拍（diffCount / feeTotalFen / diffAmountFen）", okk,
                         "接口 total: count=%s fee=%s diff=%s；rows=%s；SQL: count=%s fee=%s diff=%s%s" % (
                             api_count, api_fee, api_diff,
                             json.dumps(rows_api, ensure_ascii=False)[:160],
                             sql_count, sql_fee, sql_diff,
                             "" if okk else "；截图=" + self.shot("ac50a"))), (api_count, api_fee, api_diff)

    # ---------------- 浏览器 ----------------
    def browser_login(self, pw, user, pwd):
        exe = self.a.chrome_path or find_chromium()
        browser = pw.chromium.launch(headless=not self.a.headed, executable_path=exe)
        ctx = browser.new_context(viewport={"width": 1600, "height": 1000})
        page = ctx.new_page()
        self.page = page
        login_resp = {}

        def on_resp(r):
            if "/v1/auth/login" in r.url:
                try:
                    login_resp["status"] = r.status
                    login_resp["body"] = r.json()
                except Exception:
                    login_resp["body"] = "(no json)"
        page.on("response", on_resp)
        page.goto(self.a.admin + "/login", wait_until="domcontentloaded", timeout=90000)
        user_sels = ('input[placeholder="用户名"]', '.login-page .float-label input',
                     'input[type="text"]', 'input[name="username"]')
        pwd_sels = ('input[placeholder="密码"]', '.login-page .float-label input[type="password"]',
                    'input[type="password"]')
        btn_sels = ('button:has-text("登录")', '.login-btn', '.login-page .submit-btn', 'button[type="submit"]')
        for sel in user_sels:
            try:
                loc = page.locator(sel)
                if loc.count() > 0:
                    loc.first.fill(user, timeout=5000)
                    break
            except Exception:
                continue
        for sel in pwd_sels:
            try:
                loc = page.locator(sel)
                if loc.count() > 0:
                    loc.first.fill(pwd, timeout=5000)
                    break
            except Exception:
                continue
        for sel in btn_sels:
            try:
                loc = page.locator(sel)
                if loc.count() > 0:
                    loc.first.click(timeout=5000)
                    break
            except Exception:
                continue
        deadline, logged = time.time() + 45, False
        while time.time() < deadline:
            try:
                if page.evaluate("!!localStorage.getItem('token')"):
                    logged = True
                    break
            except Exception:
                pass
            time.sleep(0.4)
        body = login_resp.get("body") or {}
        msg = ""
        if isinstance(body, dict):
            msg = "code=%s message=%s" % (body.get("code"), str(body.get("message"))[:60])
        self.step("浏览器登录 %s" % user, logged,
                  "login接口: HTTP %s %s%s" % (login_resp.get("status"), msg,
                                               "" if logged else "；截图=" + self.shot("login")))
        page.remove_listener("response", on_resp)
        return browser, logged

    def ac49_ui(self, pw):
        browser, logged = self.browser_login(pw, self.a.store_user, self.a.store_password)
        if not logged:
            browser.close()
            return False
        okk = False
        try:
            self.page.goto(self.a.admin + "/finance/store-recon", wait_until="domcontentloaded", timeout=120000)
            # 页面结构：店铺输入框（默认=本店）+「查询」按钮
            filled = False
            for sel in ('input[placeholder*="门店ID"]', 'input[placeholder*="门店"]',
                        '.store-recon__filters .el-input__inner:not([placeholder*="日期"])'):
                try:
                    loc = self.page.locator(sel)
                    if loc.count() > 0:
                        loc.first.fill(str(self.a.other_store), timeout=8000)
                        filled = True
                        break
                except Exception:
                    continue
            clicked = False
            for sel in ('button:has-text("查询")', '.store-recon button.el-button--primary',
                        '[data-test="btn-query"]', 'button.el-button--primary'):
                try:
                    loc = self.page.locator(sel)
                    if loc.count() > 0:
                        loc.first.click(timeout=8000)
                        clicked = True
                        break
                except Exception:
                    continue
            # 日期框可能被误改（placeholder="业务日期"），显式回填业务日期后再查一次
            try:
                dsel = self.page.locator('input[placeholder*="业务日期"]')
                if dsel.count() > 0:
                    dsel.first.fill(self.a.business_date)
                    self.page.locator('button:has-text("查询")').first.click(timeout=8000)
            except Exception:
                pass
            time.sleep(2.5)
            try:
                self.page.wait_for_selector('[data-test="store-recon-forbidden"]', timeout=20000)
                okk = self.page.locator('[data-test="store-recon-forbidden"]').count() > 0
            except Exception:
                okk = False
            txt = ""
            try:
                txt = (self.page.locator('[data-test="store-recon-forbidden"]').first.inner_text() or "")[:80]
            except Exception:
                pass
            self.step("AC-49c 页面选店铺 1 → 显示 store-recon-forbidden", okk,
                      "填入店铺=%s 查询已点=%s；forbidden 文案=%r；URL=%s%s" % (
                          filled, clicked, txt, self.page.url,
                          "" if okk else "；截图=" + self.shot("ac49c")))
        except Exception as e:
            self.step("AC-49c 页面选店铺 1 → 显示 store-recon-forbidden", False,
                      "异常：%s；截图=%s" % (str(e)[:150], self.shot("ac49c")))
        browser.close()
        return okk

    def ac50_ui(self, pw, api_vals):
        browser, logged = self.browser_login(pw, self.a.admin_user, self.a.admin_password)
        if not logged:
            browser.close()
            return False
        okk = False
        try:
            self.page.goto(self.a.admin + "/finance/recon", wait_until="domcontentloaded", timeout=120000)
            self.page.wait_for_selector('[data-test="channel-fee-total"]', timeout=45000)
            loc = self.page.locator('[data-test="channel-fee-total"]').first
            prev, stable = None, False
            for _ in range(40):
                cur = (loc.get_attribute("data-fee-fen"), loc.get_attribute("data-diff-fen"),
                       loc.get_attribute("data-count"))
                if cur == prev and cur[0] not in (None, ""):
                    stable = True
                    break
                prev = cur
                time.sleep(0.4)
            fee, diff, cnt = prev if prev else (None, None, None)
            api_count, api_fee, api_diff = api_vals
            def same(a, b):
                try:
                    return int(a) == int(b)
                except Exception:
                    return False
            okk = same(fee, api_fee) and same(diff, api_diff) and same(cnt, api_count)
            self.step("AC-50b 页面 channel-fee-total 属性 == 接口", okk,
                      "属性稳定=%s；页面 data-fee-fen=%r data-diff-fen=%r data-count=%r；接口 fee=%s diff=%s count=%s%s" % (
                          stable, 
                          fee, diff, cnt, api_fee, api_diff, api_count,
                          "" if okk else "；截图=" + self.shot("ac50b")))
        except Exception as e:
            self.step("AC-50b 页面 channel-fee-total 属性 == 接口", False,
                      "异常：%s；截图=%s" % (str(e)[:150], self.shot("ac50b")))
        browser.close()
        return okk

    def run(self):
        print("=" * 88)
        print("P3-2 E2E：门店勾稽越权（AC-49） / 渠道手续费汇总对拍（AC-50）")
        print("=" * 88)
        if not self.preflight():
            return self.summary()
        self.ac49_api()
        vals = self.ac50_api()
        try:
            with sync_playwright() as pw:
                self.ac49_ui(pw)
                if isinstance(vals, tuple) and len(vals) == 2 and isinstance(vals[1], tuple):
                    self.ac50_ui(pw, vals[1])
        except Exception as e:
            self.step("浏览器会话", False, "异常：%s" % str(e)[:200])
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        nr = [r for r in self.results if r[1] == "NOT_READY"]
        print("\n" + "=" * 88)
        print("结果: %s  PASS=%d FAIL=%d%s" % (
            "全绿" if not fails and not nr else ("未就绪" if not fails else "存在失败"),
            len([r for r in self.results if r[1] == "PASS"]), len(fails),
            ("  NOT_READY=%d" % len(nr)) if nr else ""))
        for name, st, _ in self.results:
            print("  %-58s %s" % (name, st))
        if fails or nr:
            print("-" * 88)
            for name, st, detail in fails + nr:
                print("  * [%s] %s\n    %s" % (st, name, detail))
        print("截图目录: %s" % SHOT_DIR)
        print("=" * 88)
        if fails:
            return 1
        return 2 if nr else 0


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--admin", default=DEFAULT_ADMIN)
    p.add_argument("--api", default=DEFAULT_API)
    p.add_argument("--admin-user", default="qa_admin")
    p.add_argument("--admin-password", default="Admin@123456")
    p.add_argument("--store-user", default="qa_store_b")
    p.add_argument("--store-password", default="Admin@123456")
    p.add_argument("--store-b", type=int, default=2)
    p.add_argument("--other-store", type=int, default=1)
    p.add_argument("--business-date", default=datetime.now().strftime("%Y-%m-%d"))
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
        return StoreReconE2E(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
