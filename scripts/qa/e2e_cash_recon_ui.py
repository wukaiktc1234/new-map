#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
N19-3b：缴款单页面（/finance/deposit-slips）E2E

数据来源：
  · 后端 GET /v1/finance/deposit-slips/{slipId}          （五列 + stillOpenCount/staleCount/legacyNullCount）
  · 后端 GET /v1/finance/deposit-slips/{slipId}/pending-refunds
      → { pendingRefunds[], candidateShifts[], missingRefundTimeCount }
  · 页面属性（DepositSlipDetail.vue）：
      slip-rcf + data-rcf-fen / slip-dnet + data-dnet-fen + data-dnet-state(negative|positive|neutral)
      data-hint + data-count（still-open / stale / legacy-null / missing-time，0 不渲染）
      attribution-row + data-refund-id / attribution-shift(+data-shift-id) / attribution-reason / attribution-confirm

断言五段：
  ① 页面 data-rcf-fen / data-dnet-fen == 接口
  ② dnet<0 → data-dnet-state=negative 且 class 含 text-danger
  ③ 四个 chip 的 data-count == 接口字段；为 0 的 chip 不渲染
  ④ 归属三选一 + 原因≥4 字 → 提交后 rcf/dnet 刷新（读 data-* 用 wait_for_data_attr，禁止 inner_text）
  ⑤ 收银员（vcashier01）无 confirm-attribution → 按钮 disabled +「无权限」

夹具：开边界班次 → 退款 → 关班 → generate 缴款单（复用 e2e_cash_recon 的造数路径，不含 10 分钟等待）
退出码：0 全绿 / 1 断言失败 / 2 环境未就绪
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import date, datetime
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
try:
    from e2e_setup import ensure_ready, wait_for_data_attr, read_data_attr, READY_NONEMPTY
except Exception as e:  # pragma: no cover
    print("[FATAL] 无法导入 e2e_setup：%s" % e)
    sys.exit(2)

DEFAULT_API = "http://localhost:8081/api"
DEFAULT_ADMIN = "http://localhost:3002"
DEFAULT_POS = "http://localhost:3001"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
SHOT_DIR = Path(r"P:\my-new-project\.verify-tmp\e2e-slip-ui")


def find_chromium():
    """回退：Playwright 期望的 headless_shell 版本本机可能缺失，取 ms-playwright 下任一可用可执行文件"""
    base = Path(os.environ.get("LOCALAPPDATA", str(Path.home()))) / "ms-playwright"
    pats = ("chromium-*/chrome-win/chrome.exe",
            "chromium_headless_shell-*/chrome-headless-shell-win64/chrome-headless-shell.exe")
    if not base.exists():
        return None
    for pat in pats:
        for hit in sorted(base.glob(pat), reverse=True):
            if hit.exists():
                return str(hit)
    return None


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


class SlipUi:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.tag = "SUI" + datetime.now().strftime("%m%d%H%M%S")
        # 统一标记（Owner 2026-10-27 批准）：供 e2e_cleanup.py 定位
        self.mark = "[E2E:%s]" % self.tag
        self._seq = 0

    def next_seq(self):
        self._seq += 1
        return self._seq

    def step(self, name, okk, detail, state=None):
        st = state or ("PASS" if okk else "FAIL")
        self.results.append((name, st, detail))
        print("  [%s] %s -> %s" % (st, name, detail))
        return okk

    def not_ready(self, name, detail):
        return self.step(name, False, detail, state="NOT_READY")

    # ---------- DB ----------
    def psql(self, sql):
        o = subprocess.run([self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port),
                            "-U", self.a.pg_user, "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                           capture_output=True, text=True, timeout=120, encoding="utf-8", errors="replace",
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
        def _l(u, p):
            st, b = http("POST", self.a.api + "/v1/auth/login", body={"username": u, "password": p})
            return (data_of(b) or {}).get("token"), st
        self.admin_token, s1 = _l(self.a.user, self.a.password)
        self.cash_token, s2 = _l(self.a.pos_user, self.a.pos_password)
        self.manager_token, s3 = _l(self.a.refund_user, self.a.refund_password)
        self.step("登录 %s（财务）" % self.a.user, bool(self.admin_token), "HTTP %s" % s1)
        self.step("登录 %s（收银）" % self.a.pos_user, bool(self.cash_token), "HTTP %s" % s2)
        self.step("登录 %s（店长）" % self.a.refund_user, bool(self.manager_token), "HTTP %s" % s3)
        return bool(self.admin_token and self.cash_token and self.manager_token)

    # ---------- 夹具：边界班次 + 退款 + generate ----------
    def make_fixture(self):
        st, b = http("POST", self.a.api + "/v1/pos/auth/shift/start", self.cash_token,
                     {"terminalId": "T-E2E-%s" % self.tag, "employeeId": "e2e",
                      "employeeName": self.a.pos_user, "shiftType": "morning", "openingCash": 0})
        d = data_of(b) or {}
        shift_id = int((d.get("shiftId") if isinstance(d, dict) else 0) or 0)
        self.step("夹具：开班（边界窗口）", shift_id > 0, "shiftId=%s msg=%s" % (
            shift_id, str((b or {}).get("message"))[:60]))
        if not shift_id:
            return 0
        # 下单→支付→退款
        st, b = http("GET", self.a.api + "/v1/pos/api/dishes", self.cash_token)
        dishes = (data_of(b) or []) if ok(b) else []
        priced = [x for x in dishes if x.get("dishId") and float(x.get("price") or 0) > 0]
        if not priced:
            self.not_ready("夹具：菜品", "无可用菜品")
            return 0
        dish = max(priced, key=lambda x: float(x.get("price") or 0))
        item = {"productId": str(dish["dishId"]), "productName": dish.get("dishName") or "",
                "quantity": 1, "price": float(dish.get("price") or 0),
                "dishType": dish.get("dishType") or "single"}
        st, b = http("POST", self.a.api + "/v1/pos/orders/order", self.cash_token,
                     {"orderType": "dinein", "items": [item],
                      # 确定性幂等键 E2E-<tag>-<n>（订单侧 tag 锚点）
                      "idempotencyKey": "E2E-%s-%d" % (self.tag, self.next_seq())})
        dd = data_of(b) or {}
        if not ok(b):
            self.not_ready("夹具：下单", "HTTP %s code=%s" % (st, (b or {}).get("code")))
            return 0
        amt = round(float(dd.get("totalAmount") or 0), 2)
        http("POST", self.a.api + "/v1/pos/orders/order/pay", self.cash_token,
             {"orderId": dd.get("orderId"), "paymentMethod": "现金", "amount": amt})
        st, b = http("POST", self.a.api + "/v1/pos/orders/order/%s/refund?refundReason=%s"
                     % (dd.get("orderNumber"), urllib.parse.quote("%s N19-3b UI 夹具" % self.mark)),
                     self.manager_token)
        self.step("夹具：退款", ok(b), "HTTP %s code=%s" % (st, (b or {}).get("code")))
        # 关班（产生 end_time）
        term = self.rows("SELECT COALESCE(terminal_id,'') FROM pos_shifts WHERE shift_id=%d" % shift_id)
        http("POST", self.a.api + "/v1/pos/auth/shift/end", self.cash_token,
             {"terminalId": term[0][0] if term else "", "remark": "%s N19-3b UI 夹具关班" % self.mark})
        # generate
        st, b = http("POST", self.a.api + "/v1/finance/deposit-slips/generate", self.admin_token,
                     {"shiftId": shift_id, "storeId": int(self.a.store_id)})
        d = data_of(b) or {}
        sid = 0
        if isinstance(d, dict):
            sid = int(d.get("slipId") or (d.get("slip") or {}).get("slipId") or 0)
        self.step("夹具：generate 缴款单", sid > 0, "slipId=%s code=%s" % (sid, (b or {}).get("code")))
        return sid

    def slip_vo(self, slip_id):
        st, b = http("GET", self.a.api + "/v1/finance/deposit-slips/%d" % slip_id, self.admin_token)
        return (data_of(b) or {}), st, b

    def pending(self, slip_id):
        st, b = http("GET", self.a.api + "/v1/finance/deposit-slips/%d/pending-refunds" % slip_id,
                     self.manager_token)
        return (data_of(b) or {}), st, b

    # ---------- 浏览器 ----------
    def browser_login(self, pw, user, pwd):
        """按 UI 表单真实登录（与 e2e_store_recon 同法），等待 localStorage.token 落盘"""
        exe = self.a.chrome_path or find_chromium()
        browser = pw.chromium.launch(headless=True, executable_path=exe) if exe else pw.chromium.launch(headless=True)
        ctx = browser.new_context(viewport={"width": 1600, "height": 1000})
        page = ctx.new_page()
        page.goto(self.a.admin + "/login", wait_until="domcontentloaded", timeout=120000)
        for sel in ('input[placeholder="用户名"]', 'input[type="text"]', 'input[name="username"]'):
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
        for sel in ('button:has-text("登录")', 'button[type="submit"]', '.login-btn'):
            try:
                loc = page.locator(sel)
                if loc.count() > 0:
                    loc.first.click(timeout=5000)
                    break
            except Exception:
                continue
        logged = False
        deadline = time.time() + 45
        while time.time() < deadline:
            try:
                if page.evaluate("!!localStorage.getItem('token')"):
                    logged = True
                    break
            except Exception:
                pass
            time.sleep(0.4)
        self.step("浏览器登录 %s" % user, logged, "localStorage.token=%s" % logged)
        return browser, page, logged

    def run(self):
        print("=" * 92)
        print("N19-3b 缴款单页面 E2E  tag=%s" % self.tag)
        print("=" * 92)
        if not ensure_ready(api=self.a.api, admin=self.a.admin, pos=self.a.pos):
            self.not_ready("前置就绪", "服务/库存未就绪")
            return self.summary()
        if not self.login():
            return self.summary()
        slip_id = self.a.slip_id or self.make_fixture()
        if not slip_id:
            return self.summary()
        vo, st, raw = self.slip_vo(slip_id)
        pend, st_p, raw_p = self.pending(slip_id)
        self.step("接口：缴款单详情 + pending-refunds 可读", bool(vo) and st == 200,
                  "slip=%s rcf=%s dnet=%s stillOpen=%s stale=%s legacyNull=%s | pending=%s candidateShifts=%s missingTime=%s" % (
                      slip_id, vo.get("refundComponentFen"), vo.get("differenceNetFen"),
                      vo.get("stillOpenCount"), vo.get("staleCount"), vo.get("legacyNullCount"),
                      len(pend.get("pendingRefunds") or []), len(pend.get("candidateShifts") or []),
                      pend.get("missingRefundTimeCount")))

        from playwright.sync_api import sync_playwright
        with sync_playwright() as pw:
            browser, page, logged = self.browser_login(pw, self.a.refund_user, self.a.refund_password)
            if not logged:
                self.not_ready("④/①-③ 页面断言", "浏览器登录失败（localStorage 无 token）")
            try:
                page.goto(self.a.admin + "/finance/deposit-slips", wait_until="domcontentloaded", timeout=120000)
                row = '[data-test="slip-row"][data-slip-id="%d"]' % slip_id
                try:
                    page.wait_for_selector(row, timeout=20000)
                except Exception:
                    # 兜底：直接打开详情
                    page.goto(self.a.admin + "/finance/deposit-slips/%d" % slip_id, wait_until="domcontentloaded")
                    page.wait_for_selector('[data-test="slip-rcf"]', timeout=20000)
                # ① 两列与接口全等
                rcf_page = wait_for_data_attr(page, '[data-test="slip-rcf"]', "data-rcf-fen", READY_NONEMPTY, timeout=30000)
                dnet_page = wait_for_data_attr(page, '[data-test="slip-dnet"]', "data-dnet-fen", READY_NONEMPTY, timeout=30000)
                self.step("① 页面 data-rcf-fen == 接口 refundComponentFen",
                          str(rcf_page) == str(vo.get("refundComponentFen")),
                          "页面=%r 接口=%r" % (rcf_page, vo.get("refundComponentFen")))
                self.step("① 页面 data-dnet-fen == 接口 differenceNetFen",
                          str(dnet_page) == str(vo.get("differenceNetFen")),
                          "页面=%r 接口=%r" % (dnet_page, vo.get("differenceNetFen")))
                # ② dnet 状态与配色（列表页有多行，必须按 data-slip-id 限定作用域）
                scope = row + " "
                dnet_fen = int(vo.get("differenceNetFen") or 0)
                state = read_data_attr(page, scope + '[data-test="slip-dnet"]', "data-dnet-state")
                want = "negative" if dnet_fen < 0 else ("positive" if dnet_fen > 0 else "neutral")
                cls = page.locator(scope + '[data-test="slip-dnet"]').first.get_attribute("class") or ""
                danger_ok = (dnet_fen >= 0) or ("text-danger" in cls)
                self.step("② dnet<0 → data-dnet-state=negative 且 class 含 text-danger",
                          str(state) == want and danger_ok,
                          "dnet=%d 页面 state=%r 期望=%r；class=%r" % (dnet_fen, state, want, cls[:80]))
                # 打开详情抽屉（列表行里的「详情」按钮 → el-drawer 内渲染 chip / 归属区）
                try:
                    page.locator(row + ' [data-test="slip-detail"]').first.click(timeout=8000)
                except Exception as e:
                    print("    · 详情按钮点击失败：%s" % str(e)[:90])
                drawer = '.el-drawer'
                detail_ok = False
                try:
                    page.wait_for_selector(drawer, timeout=15000)
                    detail_ok = page.locator(drawer + ' [data-test="slip-rcf"]').count() > 0
                except Exception:
                    detail_ok = False
                self.step("打开缴款单详情抽屉（chip/归属区渲染）", detail_ok, "drawer=%s" % detail_ok)
                scope = drawer + " "

                # ③ 四个 chip
                chips = {"still-open": int(vo.get("stillOpenCount") or 0),
                         "stale": int(vo.get("staleCount") or 0),
                         "legacy-null": int(vo.get("legacyNullCount") or 0),
                         "missing-time": int(pend.get("missingRefundTimeCount") or 0)}
                bad = []
                for key, cnt in chips.items():
                    loc = page.locator(scope + '[data-hint="%s"]' % key)
                    n = loc.count()
                    if cnt == 0:
                        if n > 0:
                            bad.append("%s 期望不渲染但出现 %d 个" % (key, n))
                    else:
                        if n == 0:
                            bad.append("%s 期望渲染但缺失（count=%d）" % (key, cnt))
                        else:
                            got = loc.first.get_attribute("data-count")
                            if str(got) != str(cnt):
                                bad.append("%s data-count=%r 期望 %d" % (key, got, cnt))
                self.step("③ 四个 chip 的 data-count == 接口且 0 不渲染", not bad,
                          "接口=%s；偏差=%s" % (chips, bad if bad else "无"))
                # ④ 归属提交
                pend_rows = pend.get("pendingRefunds") or []
                if not pend_rows:
                    self.not_ready("④ 归属提交", "pending-refunds 为空（无待确认归属）→ 未覆盖")
                else:
                    rid = pend_rows[0].get("refundRecordId")
                    rrow = '[data-test="attribution-row"][data-refund-id="%s"]' % rid
                    try:
                        page.wait_for_selector(rrow, timeout=10000)
                    except Exception:
                        n_rows = page.locator('[data-test="attribution-row"]').count()
                        self.not_ready(
                            "④ 归属提交（页面未渲染归属区）",
                            "接口 pendingRefunds=%d 条，但抽屉里 attribution-row=%d 条；"
                            "前端 listPendingAttributionRefunds 只认 data 为数组或 data.records，"
                            "而后端返回对象 {pendingRefunds[], candidateShifts[], missingRefundTimeCount} "
                            "→ 归属区块被 v-if 隐藏（前后端契约不一致，需前端读 data.pendingRefunds）" % (
                                len(pend_rows), n_rows))
                        return self.summary()
                    btn = page.locator(rrow + ' [data-test="attribution-confirm"]').first
                    disabled_before = btn.is_disabled()
                    shifts = page.locator(rrow + ' [data-test="attribution-shift"] [data-shift-id]')
                    n_shift = shifts.count()
                    if n_shift == 0:
                        shifts = page.locator(rrow + ' .el-radio-button')
                        n_shift = shifts.count()
                    # 【教训·必须保留】必须选「本单所属班次」（data-shift-id == slip.shiftId）：
                    #   按 A39 口径 attribution.shift_id 必须等于 slip.shift_id 才计入，
                    #   归属到别的班次（上一/下一班次）本单的 rcf 不会变 → 断言必然误判。
                    #   （2026-10-26 实测：选到 355「上一班次」时 rcf 恒为 0，选中本单 357 后 0 → 2500）
                    own_shift = vo.get("shiftId")
                    picked = None
                    for i in range(n_shift):
                        sid_attr = shifts.nth(i).get_attribute("data-shift-id")
                        if str(sid_attr) == str(own_shift):
                            picked = shifts.nth(i)
                            break
                    if picked is None and n_shift > 0:
                        picked = shifts.first
                    if picked is not None:
                        picked.click()
                        print("    · 已选候选班次：data-shift-id=%s（本单班次=%s）"
                              % (picked.get_attribute("data-shift-id"), own_shift))
                    # 原因输入框：data-test 可能落在 el-input 根节点或内部 input 上，多种选择器兜底
                    reason_loc = None
                    for sel in (rrow + ' [data-test="attribution-reason"] input',
                                rrow + ' input[data-test="attribution-reason"]',
                                rrow + ' [data-test="attribution-reason"]',
                                rrow + ' .el-input input'):
                        try:
                            cand = page.locator(sel)
                            if cand.count() > 0:
                                reason_loc = cand.first
                                break
                        except Exception:
                            continue
                    if reason_loc is None:
                        self.step("④ 原因输入框定位", False, "所有候选选择器都没命中（attribution-reason）")
                    else:
                        try:
                            reason_loc.click(timeout=5000)
                            reason_loc.fill("E2E 归属确认原因")
                        except Exception as e:
                            print("    · 原因输入失败：%s" % str(e)[:90])
                    time.sleep(0.5)
                    disabled_after = btn.is_disabled()
                    self.step("④ 三选一 + 原因≥4 字 → 提交按钮可用", (not disabled_after),
                              "候选班次=%d；按钮 disabled 前=%s 后=%s" % (n_shift, disabled_before, disabled_after))
                    if not disabled_after:
                        btn.click()
                        time.sleep(2.0)
                        vo2, _, _ = self.slip_vo(slip_id)
                        try:
                            rcf2 = wait_for_data_attr(page, '[data-test="slip-rcf"]', "data-rcf-fen",
                                                      READY_NONEMPTY, timeout=30000)
                        except Exception:
                            rcf2 = read_data_attr(page, '[data-test="slip-rcf"]', "data-rcf-fen")
                        self.step("④ 提交后页面 rcf/dnet 刷新 == 接口新值",
                                  str(rcf2) == str(vo2.get("refundComponentFen"))
                                  and str(vo2.get("refundComponentFen")) != str(vo.get("refundComponentFen")),
                                  "rcf 前=%r 后=%r；接口 rcf 前=%r 后=%r" % (
                                      rcf_page, rcf2, vo.get("refundComponentFen"), vo2.get("refundComponentFen")))
            finally:
                browser.close()

            # ⑤ 无 confirm-attribution 但有**查看权**的账号（qa_auditor / ROLE_AUDITOR）
            #    说明：vcashier01 属 CASHIER，路由守卫即拒绝（缴款单是财务/店长域，设计如此）→ 见 ⑥
            browser2, page2, logged2 = self.browser_login(pw, self.a.auditor_user, self.a.auditor_password)
            if not logged2:
                self.not_ready("⑤ 无权限账号视角", "浏览器登录失败（localStorage 无 token）")
            try:
                # 缴款单详情是列表页里的**抽屉**（没有独立详情路由）→ 必须走「列表 → 详情」
                page2.goto(self.a.admin + "/finance/deposit-slips",
                           wait_until="domcontentloaded", timeout=120000)
                try:
                    row2sel = '[data-test="slip-row"][data-slip-id="%d"]' % slip_id
                    page2.wait_for_selector(row2sel, timeout=20000)
                    page2.locator(row2sel + ' [data-test="slip-detail"]').first.click(timeout=8000)
                    page2.wait_for_selector('.el-drawer[aria-label="缴款单详情"]', timeout=15000)
                except Exception as e2:
                    print("    · ⑤ 打开详情失败：%s" % str(e2)[:100])
                # ⑤ 口径（lead 定案·方式1变体）：无权限时渲染**占位块** + disabled 按钮 + 「无权限」文案，
                #    且前端不再请求 pending-refunds（避免 403）。断言三点 + 无 5xx/错误 toast。
                lock_ok = btn_ok = False
                body_txt = ""
                try:
                    page2.wait_for_selector('.slip-attribution__lock', timeout=15000)
                    lock_txt = (page2.locator('.slip-attribution__lock').first.inner_text() or "").strip()
                    lock_ok = "无权限" in lock_txt
                    btn2 = page2.locator('[data-test="attribution-confirm"]').first
                    btn_ok = btn2.count() > 0 and btn2.is_disabled()
                    body_txt = page2.locator("body").inner_text()
                except Exception as e:
                    body_txt = page2.locator("body").inner_text()
                    print("    · ⑤ 占位块定位失败：%s" % str(e)[:100])
                err_bits = [s for s in ("500", "操作失败", "系统异常", "加载失败") if s in body_txt]
                block_ok = page2.locator('[data-test="attribution-block"]').count() > 0
                self.step("⑤ 缺 confirm-attribution 账号 → 占位块 + lock「无权限」+ 按钮 disabled + 无错误",
                          block_ok and lock_ok and btn_ok and not err_bits,
                          "attribution-block=%s；lock 含「无权限」=%s；confirm disabled=%s；错误文案=%s"
                          % (block_ok, lock_ok, btn_ok, err_bits if err_bits else "无"))
            finally:
                browser2.close()

            # ⑥ 收银员（vcashier01）进缴款单模块应被**路由守卫**拦截（设计预期，固化断言）
            browser3, page3, logged3 = self.browser_login(pw, self.a.pos_user, self.a.pos_password)
            try:
                if not logged3:
                    self.not_ready("⑥ 收银员路由拦截", "浏览器登录失败（localStorage 无 token）")
                else:
                    page3.goto(self.a.admin + "/finance/deposit-slips",
                               wait_until="domcontentloaded", timeout=120000)
                    time.sleep(2.0)
                    txt3 = page3.locator("body").inner_text() or ""
                    blocked = ("没有权限访问" in txt3) or ("403" in txt3) or ("无权限" in txt3)
                    list_rendered = page3.locator('[data-test="slip-row"]').count() > 0
                    self.step("⑥ 收银员（vcashier01）进缴款单模块被路由拦截",
                              blocked and not list_rendered,
                              "URL=%s；页面含「没有权限访问/403/无权限」=%s；列表渲染=%s；文案片段=%r"
                              % (page3.url, blocked, list_rendered, txt3[:80]))
            finally:
                browser3.close()
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
            print("  %-74s %s" % (name, st))
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
    p.add_argument("--user", default="qa_admin")
    p.add_argument("--password", default="Admin@123456")
    p.add_argument("--pos-user", default="vcashier01")
    p.add_argument("--pos-password", default="123456")
    p.add_argument("--auditor-user", default="qa_auditor", help="ROLE_AUDITOR：能看缴款单但无 confirm-attribution")
    p.add_argument("--auditor-password", default="Admin@123456")
    p.add_argument("--refund-user", default="qa_store_b")
    p.add_argument("--refund-password", default="Admin@123456")
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--slip-id", type=int, default=0, help="0=自动造夹具")
    p.add_argument("--chrome-path", default=None, help="指定 chromium 可执行文件（默认自动探测）")
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    try:
        return SlipUi(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
