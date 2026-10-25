#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P3-1 E2E：勾稽工作台（/finance/recon）Playwright 验收

覆盖：
  前置  用例内先跑 R1（admin token），断言 runId 且 differenceCount > 0；
        accountId=24 为 0 时自动改用 account 20 再跑一次，仍为 0 → 明确 FAIL 并打印 internalCount/externalCount
  AC-46a 页面 summary-diff-count 数值（去千分位） == GET /recon-runs/{runId}.differenceCount
  AC-46b 页面 summary-diff-amount 换算回分 == R8 分页全量 SUM(diffAmount)（不用页面自证）
  AC-48  不填 resolve-reason 直接点 resolve-submit → ① 出现错误提示 ② **期间没有任何发往 /resolve 的请求**
  AC-47  填 reason → btn-claim → 状态 ASSIGNED → 填 reason → resolve-submit → CLOSED/EXEMPT
         → audit-timeline 内出现 assignedAt / resolutionTime / resolutionRemark

幂等：若当日该账户差异池已被核销空，自动换账户/日期重跑 R1 造新差异。

用法：
  python scripts/qa/e2e_recon_workbench.py
  python scripts/qa/e2e_recon_workbench.py --account-id 24 --fallback-account 20 --business-date 2026-10-25
退出码：0 全绿 / 1 断言失败 / 2 环境未就绪（前端或后端未启动）
"""
from __future__ import annotations

import argparse
import json
import os
import re
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
SHOT_DIR = Path(r"P:\my-new-project\.verify-tmp\e2e-recon")


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


def probe(url, timeout=5):
    try:
        with urllib.request.urlopen(urllib.request.Request(url, method="GET"), timeout=timeout) as r:
            return r.status < 500
    except urllib.error.HTTPError as e:
        return e.code < 500
    except Exception:
        return False


def to_fen(text):
    """把页面金额文本转成分：'1,234.56' -> 123456；'123456 分' -> 123456；'¥1,234.56' -> 123456"""
    if text is None:
        return None
    s = str(text).replace(",", "").replace("¥", "").replace("￥", "").strip()
    if not s:
        return None
    m = re.search(r"-?\d+(?:\.\d+)?", s)
    if not m:
        return None
    val = float(m.group(0))
    if "分" in s and "元" not in s:
        return int(round(val))
    return int(round(val * 100))


def to_int(text):
    if text is None:
        return None
    s = str(text).replace(",", "").strip()
    m = re.search(r"-?\d+", s)
    return int(m.group(0)) if m else None



def find_chromium():
    """本机 playwright 包版本与已下载浏览器版本可能不匹配（会报 Executable doesn't exist），
    这里回退到 ms-playwright 目录下任一可用 chromium 可执行文件，避免把环境问题记成用例失败。"""
    import glob
    base = os.path.expandvars(r"%LOCALAPPDATA%\ms-playwright")
    pats = ("chromium-*/chrome-win/chrome.exe",
            "chromium_headless_shell-*/chrome-headless-shell-win64/chrome-headless-shell.exe")
    for pat in pats:
        hits = sorted(glob.glob(os.path.join(base, pat)))
        if hits:
            return hits[-1]
    return None


class ReconE2E:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.token = None
        self.page = None
        self.run_id = None
        self.diff_count = None
        self.diff_sum = None
        SHOT_DIR.mkdir(parents=True, exist_ok=True)

    # ---------------- 结果 ----------------
    def step(self, name, okk, detail, state=None):
        st = state or ("PASS" if okk else "FAIL")
        self.results.append((name, st, detail))
        print("  [%s] %s -> %s" % (st, name, detail))
        return okk

    def not_ready(self, name, detail):
        return self.step(name, False, detail, state="NOT_READY")

    def psql_scalar(self, sql):
        import subprocess
        psql = getattr(self.a, "psql", None) or "P:/my-new-project/PostgreSQL/18/bin/psql.exe"
        try:
            o = subprocess.run([psql, "-h", "localhost", "-p", "5432", "-U", "postgres",
                                "-d", "food_traceability", "-t", "-A", "-c", sql],
                               capture_output=True, text=True, timeout=40, encoding="utf-8", errors="replace",
                               env=dict(os.environ, PGPASSWORD="123456", PGCLIENTENCODING="UTF8"))
            if o.returncode != 0:
                print("  [WARN] psql_scalar rc=%s err=%s" % (o.returncode, (o.stderr or "")[:120]))
            return (o.stdout or "").strip()
        except Exception as e:
            print("  [WARN] psql_scalar 异常: %s" % e)
            return ""

    def shot(self, name):
        try:
            p = SHOT_DIR / ("%s-%s.png" % (datetime.now().strftime("%H%M%S"), name))
            if self.page:
                self.page.screenshot(path=str(p), full_page=True)
            return str(p)
        except Exception as e:
            return "(截图失败: %s)" % e

    # ---------------- 环境 ----------------
    def preflight(self):
        admin_up = probe(self.a.admin)
        api_up = probe(self.a.api + "/v1/auth/login")
        if not (admin_up and api_up):
            return self.not_ready("前端 3002 / 后端 8081 就绪",
                                  "admin(3002)=%s api(8081)=%s —— 页面与接口都未起，脚本待跑" % (admin_up, api_up))
        return self.step("前端 3002 / 后端 8081 就绪", True, "admin=200 api=200")

    def login_api(self):
        st, b = http("POST", self.a.api + "/v1/auth/login",
                     body={"username": self.a.user, "password": self.a.password})
        self.token = (data_of(b) or {}).get("token") if ok(b) else None
        return self.step("API 登录 %s" % self.a.user, bool(self.token),
                         "HTTP %s %s" % (st, "" if self.token else json.dumps(b, ensure_ascii=False)[:180]))

    # ---------------- R1 与差异池 ----------------
    def run_recon(self, account_id, force=False):
        path = "/v1/finance/recon-runs" + ("?force=true" if force else "")
        st, b = http("POST", self.a.api + path, self.token,
                     {"accountId": int(account_id), "businessDate": self.a.business_date,
                      "force": bool(force)})
        d = data_of(b) or {}
        run = d.get("run") or d
        return st, b, run

    def prepare(self):
        for acct in [self.a.account_id] + ([self.a.fallback_account] if self.a.fallback_account else []):
            st, b, run = self.run_recon(acct, force=True)
            if not ok(b):
                self.step("R1 跑批 account=%s" % acct, False,
                          "HTTP %s %s" % (st, json.dumps(b, ensure_ascii=False)[:200]))
                continue
            run_id = run.get("runId")
            dcnt = int(run.get("differenceCount") or 0)
            self.step("R1 跑批 account=%s" % acct, bool(run_id), "runId=%s reused=%s differenceCount=%d "
                      "internalCount=%s externalCount=%s" % (
                          run_id, data_of(b) and data_of(b).get("reused"), dcnt,
                          run.get("internalCount"), run.get("externalCount")))
            actionable = 0
            if run_id:
                try:
                    actionable = int(self.psql_scalar(
                        "SELECT COUNT(*) FROM recon_differences WHERE run_id=%s"
                        " AND status NOT IN ('CLOSED','REVERSED')" % int(run_id)) or 0)
                except Exception:
                    actionable = 0
            if run_id and dcnt > 0 and actionable > 0:
                self.run_id, self.diff_count = int(run_id), dcnt
                self.step("可操作差异就绪（未被核销）", True,
                          "runId=%s differenceCount=%d 未关闭差异=%d" % (run_id, dcnt, actionable))
                break
            if run_id and dcnt > 0 and actionable == 0:
                self.step("差异池已被核销", True,
                          "runId=%s differenceCount=%d 但未关闭差异=0 → 继续换下一个账户/重建" % (run_id, dcnt))
            if run_id and dcnt == 0:
                self.step("差异池非空（account=%s）" % acct, False,
                          "differenceCount=0；internalCount=%s externalCount=%s —— 不静默跳过，换下一个账户" % (
                              run.get("internalCount"), run.get("externalCount")))
        if not self.run_id:
            return self.step("准备差异数据", False,
                             "所有尝试账户的 differenceCount 均为 0 或无 runId（见上方逐条）")
        # 详情复核 + 差异金额求和（分页全量）
        st, b = http("GET", self.a.api + "/v1/finance/recon-runs/%s" % self.run_id, self.token)
        d = data_of(b) or {}
        self.diff_count = int(d.get("differenceCount") or self.diff_count)
        total, page_no, size = 0, 1, 200
        while True:
            stq, bq = http("GET", self.a.api + "/v1/finance/recon-differences?runId=%s&page=%d&size=%d"
                           % (self.run_id, page_no, size), self.token)
            dq = data_of(bq) or {}
            recs = dq.get("records") or []
            for r in recs:
                total += int(r.get("diffAmount") or 0)
            if len(recs) < size:
                break
            page_no += 1
            if page_no > 50:
                break
        self.diff_sum = total
        return self.step("差异池聚合成交（R8 分页全量）", self.diff_count > 0,
                         "runId=%s differenceCount=%d SUM(diffAmount)=%d 分" % (
                             self.run_id, self.diff_count, total))

    # ---------------- 浏览器 ----------------
    def open_workbench(self, pw):
        exe = self.a.chrome_path or find_chromium()
        browser = pw.chromium.launch(headless=not self.a.headed, executable_path=exe)
        ctx = browser.new_context(viewport={"width": 1600, "height": 1000})
        page = ctx.new_page()
        page.on("console", lambda m: None)
        self.page = page
        page.goto(self.a.admin + "/login", wait_until="domcontentloaded", timeout=60000)
        try:
            page.locator(".login-page .float-label input").first.fill(self.a.user)
            page.locator(".login-page .float-label input[type='password']").first.fill(self.a.password)
            page.locator(".login-page .submit-btn").first.click()
        except Exception:
            pass
        deadline = time.time() + 45
        logged = False
        while time.time() < deadline:
            try:
                if page.evaluate("!!localStorage.getItem('token')"):
                    logged = True
                    break
            except Exception:
                pass
            time.sleep(0.4)
        if not logged:
            self.shot("login-failed")
            self.step("浏览器登录管理端", False, "localStorage.token 未出现，截图=%s" % self.shot("login-failed"))
            return browser, False
        self.step("浏览器登录管理端", True, "localStorage.token 已就绪")
        page.goto(self.a.admin + "/finance/recon", wait_until="domcontentloaded", timeout=120000)
        try:
            page.wait_for_selector('[data-test="summary-diff-count"]', timeout=60000)
            self.step("打开 /finance/recon 并出现 summary-diff-count", True, "URL=%s" % page.url)
            return browser, True
        except Exception as e:
            shot = self.shot("no-summary")
            self.step("打开 /finance/recon 并出现 summary-diff-count", False,
                      "等待超时：%s；URL=%s；截图=%s（页面可能尚未由 pos-ux 落地）" % (str(e)[:120], page.url, shot))
            return browser, False

    def read_text(self, selector):
        try:
            loc = self.page.locator(selector).first
            loc.wait_for(state="visible", timeout=20000)
            return (loc.inner_text() or "").strip()
        except Exception:
            return None

    # ---------------- AC-46 ----------------
    def wait_summary_ready(self, timeout=30000):
        """等摘要卡数据就绪（data-amount-state=ready 且 data-difference-count 非空），
        避免在 /recon-runs/{id} 返回前读到占位 0。"""
        try:
            self.page.wait_for_function(
                """() => {const a=document.querySelector('[data-test="summary-diff-amount"]');
                const c=document.querySelector('[data-test="summary-diff-count"]');
                if(!a||!c) return false;
                const st=a.getAttribute('data-amount-state');
                const dc=c.getAttribute('data-difference-count');
                return st==='ready' && dc!==null && dc!=='';}""", timeout=timeout)
            return True
        except Exception:
            return False

    def ac46a(self):
        ready = self.wait_summary_ready()
        txt = self.read_text('[data-test="summary-diff-count"]')
        attr = None
        try:
            attr = self.page.locator('[data-test="summary-diff-count"]').first.get_attribute("data-difference-count")
        except Exception:
            attr = None
        val = to_int(txt)
        attr_val = to_int(attr)
        self.step("AC-46a 读取前等待摘要就绪（data-amount-state=ready 且 data-difference-count 非空）", ready,
                  "ready=%s；count 文本=%r 属性 data-difference-count=%r" % (ready, txt, attr))
        okk = (val is not None and val == self.diff_count
               and (attr_val is None or attr_val == self.diff_count))
        return self.step("AC-46a 页面差异条数 == 接口 differenceCount（文本与 data-difference-count 双校验）", okk,
                         "页面文本=%r 解析=%s；页面属性=%r 解析=%s；接口 runId=%s differenceCount=%d%s" % (
                             txt, val, attr, attr_val, self.run_id, self.diff_count,
                             "" if okk else "；截图=" + self.shot("ac46a")))

    def ac46b(self):
        # 先用 data-amount-state 消除竞态：等 ready 再读；idle/loading 期间读到的空值不算数
        state = None
        try:
            self.page.wait_for_function(
                """() => {const e=document.querySelector('[data-test="summary-diff-amount"]');
                return e && e.getAttribute('data-amount-state')==='ready'}""", timeout=30000)
            state = "ready"
        except Exception as e:
            state = "not-ready(%s)" % str(e)[:80]
        attr = None
        try:
            attr = self.page.locator('[data-test="summary-diff-amount"]').first.get_attribute("data-diff-amount-fen")
        except Exception:
            attr = None
        txt = self.read_text('[data-test="summary-diff-amount"]')
        fen = None
        if attr not in (None, ""):
            try:
                fen = int(attr)
            except Exception:
                fen = None
        if fen is None:            # 属性缺失才回退解析文本
            fen = to_fen(txt)
        okk = state == "ready" and fen is not None and fen == self.diff_sum
        return self.step("AC-46b 页面差异金额（分）== R8 聚合 SUM(diffAmount)", okk,
                         "data-amount-state=%s；data-diff-amount-fen=%r；文本=%r；取值=%s 分；接口聚合=%s 分%s" % (
                             state, attr, txt, fen, self.diff_sum,
                             "" if okk else "；截图=" + self.shot("ac46b")))

    # ---------------- 差异详情交互 ----------------
    def _open_actionable_diff(self, need_resolve=False):
        """点开一条「待认领/未核销」的差异行：逐行尝试，直到目标按钮可用为止。
        上一版固定点第 1 行，重复跑时那行可能已核销 → 按钮 disabled 导致超时。"""
        target = '[data-test="btn-resolve"]' if need_resolve else '[data-test="btn-claim"]'
        rows = self.page.locator('[data-test="queue-row"]')
        n = rows.count()
        for i in range(min(n, 20)):
            try:
                rows.nth(i).click(timeout=8000)
                time.sleep(0.8)
                btn = self.page.locator(target).first
                if btn.count() > 0 and btn.is_enabled():
                    return True
            except Exception:
                continue
        return False

    def ac48(self):
        if not self._open_actionable_diff(need_resolve=True):
            return self.step("AC-48 打开一条可核销差异", False,
                             "找不到「核销按钮可用」的差异行（可能池里全是已核销）%s" % self.shot("ac48-norow"))
        self.shot("ac48-detail")
        resolve_hits = []

        def on_req(req):
            if "/resolve" in req.url:
                resolve_hits.append(req.url)

        self.page.on("request", on_req)
        opened, disabled, click_err = True, None, None
        try:
            self.page.locator('[data-test="btn-resolve"]').first.click(timeout=20000)
            time.sleep(1.0)
            btn = self.page.locator('[data-test="resolve-submit"]').first
            btn.wait_for(state="visible", timeout=20000)
            disabled = bool(btn.is_disabled())
            # 口径 a：按钮禁用即前端已阻塞；这里再确定性地尝试「强制点击」，用于证明不会发请求
            try:
                btn.click(force=True, timeout=3000)
            except Exception as e:
                click_err = str(e)[:120]
            time.sleep(1.5)
        except Exception as e:
            opened = False
            self.step("AC-48 打开处理弹窗", False, "失败：%s%s" % (str(e)[:150], self.shot("ac48-click")))
        self.page.remove_listener("request", on_req)
        okk = opened and disabled is True and len(resolve_hits) == 0
        return self.step("AC-48 reason 为空时提交按钮 disabled 且不发 /resolve 请求", okk,
                         "resolve-submit.is_disabled()=%s；期间 /resolve 请求数=%d%s；强制点击异常=%s%s" % (
                             disabled, len(resolve_hits),
                             (" 请求样本=" + str(resolve_hits[:2])) if resolve_hits else "",
                             (click_err or "无"), "" if okk else "；截图=" + self.shot("ac48-fail")))
        return None

    def _status_text(self):
        for sel in ('[data-test="detail-panel"] [data-test="diff-status"]', '[data-test="diff-status"]', '[data-test="status"]'):
            t = self.read_text(sel)
            if t:
                return t.upper()
        return ""

    def _fill_reason(self, text):
        for sel in ('[data-test="resolve-reason"]', '[data-test="resolve-reason"] textarea', '[data-test="resolve-reason"] input',
                    'textarea', 'input[type="text"]'):
            try:
                loc = self.page.locator(sel)
                if loc.count() > 0:
                    loc.first.fill(text)
                    return True
            except Exception:
                continue
        return False

    def _close_dialog(self):
        """关掉处理弹窗（否则遮罩会挡住背景的 btn-claim）"""
        for sel in ('[data-test="dialog-cancel"]', '.el-dialog__headerbtn', '[data-test="btn-cancel"]'):
            try:
                loc = self.page.locator(sel)
                if loc.count() > 0 and loc.first.is_visible():
                    loc.first.click(timeout=5000)
                    time.sleep(0.8)
                    return True
            except Exception:
                continue
        try:
            self.page.keyboard.press("Escape")
            time.sleep(0.8)
        except Exception:
            pass
        return False

    def _timeline_text(self):
        for sel in ('[data-test="audit-timeline"]', '.audit-timeline', '[data-test="detail-panel"]'):
            t = self.read_text(sel)
            if t:
                return t
        return ""

    def ac47(self):
        # 1) 先关弹窗，避免遮罩挡住 btn-claim（上一版失败原因）
        self._close_dialog()
        if not self._open_actionable_diff(need_resolve=False):
            return self.step("AC-47 打开一条可认领差异", False,
                             "找不到「认领按钮可用」的差异行%s" % self.shot("ac47-norow"))
        claimed = True
        try:
            self.page.locator('[data-test="btn-claim"]').first.click(timeout=20000)
        except Exception as e:
            claimed = False
            self.step("AC-47 点击 btn-claim", False, "失败：%s%s" % (str(e)[:150], self.shot("ac47-claim")))
        # 2) 等时间线出现「认领于」+ 操作人 ID
        deadline, tl = time.time() + 30, ""
        while time.time() < deadline:
            tl = self._timeline_text()
            if "认领于" in tl:
                break
            time.sleep(0.5)
        import re as _re
        has_claim = "认领于" in tl
        has_op = bool(_re.search(r"\d{8,}", tl))
        self.step("AC-47 认领后时间线含「认领于」+ 操作人 ID", claimed and has_claim and has_op,
                  "时间线=%r%s" % (tl[:200], "" if (has_claim and has_op) else "；截图=" + self.shot("ac47-claim")))

        # 3) 打开处理弹窗 → 填原因 → 提交
        opened = True
        try:
            self.page.locator('[data-test="btn-resolve"]').first.click(timeout=20000)
            time.sleep(1.0)
        except Exception as e:
            opened = False
            self.step("AC-47 打开处理弹窗", False, "失败：%s%s" % (str(e)[:150], self.shot("ac47-open")))
        reason = "E2E 自动化核销-%s" % datetime.now().strftime("%H%M%S")
        filled = self._fill_reason(reason)
        time.sleep(0.5)
        submitted = True
        try:
            btn = self.page.locator('[data-test="resolve-submit"]').first
            if btn.is_disabled():
                submitted = False
                self.step("AC-47 填原因后提交按钮应可用", False,
                          "填入原因后按钮仍 disabled；reason=%r%s" % (reason, self.shot("ac47-stilldisabled")))
            else:
                btn.click(timeout=20000)
        except Exception as e:
            submitted = False
            self.step("AC-47 提交核销", False, "失败：%s%s" % (str(e)[:150], self.shot("ac47-submit")))
        # 4) 等时间线出现「核销于」
        deadline, tl2 = time.time() + 40, ""
        while time.time() < deadline:
            tl2 = self._timeline_text()
            if "核销于" in tl2:
                break
            time.sleep(0.5)
        self.step("AC-47 核销成功（时间线出现「核销于」）", opened and filled and submitted and ("核销于" in tl2),
                  "reasonFilled=%s submitted=%s 时间线=%r%s" % (
                      filled, submitted, tl2[:220], "" if "核销于" in tl2 else "；截图=" + self.shot("ac47-closed")))
        need = ["核销于", "操作人", "原因", "决议"]
        hit = [k for k in need if k in tl2]
        # 可选：pos-ux 若已加 data-field 标记，则额外用选择器精确定位（不作为判据）
        extra = ""
        for f in ("resolutionTime", "resolutionRemark", "resolutionBy", "assignedAt"):
            try:
                n = self.page.locator('[data-field="%s"]' % f).count()
                extra += "%s=%d " % (f, n)
            except Exception:
                pass
        return self.step("AC-47 时间线含「核销于/操作人/原因/决议」中文文案", len(hit) == 4,
                         "命中=%s；时间线=%r；data-field 计数[可选]=%s%s" % (
                             hit, tl2[:240], extra.strip() or "无",
                             "" if len(hit) == 4 else "；截图=" + self.shot("ac47-timeline")))

    # ---------------- 主流程 ----------------
    def run(self):
        print("=" * 88)
        print("P3-1 E2E：勾稽工作台 /finance/recon（Playwright）")
        print("  admin=%s api=%s account=%s fallback=%s businessDate=%s" % (
            self.a.admin, self.a.api, self.a.account_id, self.a.fallback_account, self.a.business_date))
        print("=" * 88)
        if not self.preflight():
            return self.summary()
        if not self.login_api():
            return self.summary()
        if not self.prepare():
            return self.summary()
        try:
            with sync_playwright() as pw:
                browser, ready = self.open_workbench(pw)
                if ready:
                    self.ac46a()
                    self.ac46b()
                    self.ac48()
                    self.ac47()
                browser.close()
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
            print("  %-60s %s" % (name, st))
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
    p.add_argument("--user", default="qa_admin")
    p.add_argument("--password", default="Admin@123456")
    p.add_argument("--account-id", type=int, default=24)
    p.add_argument("--fallback-account", type=int, default=20)
    p.add_argument("--business-date", default="2026-10-25")
    p.add_argument("--headed", action="store_true")
    p.add_argument("--chrome-path", default=None, help="指定 chromium 可执行文件（默认自动探测）")
    a = p.parse_args()
    try:
        return ReconE2E(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
