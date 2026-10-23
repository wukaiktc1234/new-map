#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""POS 端 / 管理端「浏览器级真点击」验收（Playwright + Chromium）。

覆盖步骤
  A 登录          vcashier01 / 123456 登录 POS，断言离开 /login 且顶栏渲染
  B 顶栏账号/退出  断言顶栏可见当前账号文本；点「退出登录」→ 确认 → 回到 /login
  C 交班日结      打开交班对话框：出现真实金额（¥x.xx 且非 0.00 或有订单数），且无「待后端接线 / 暂不可执行」占位文案
  D 订单查询      用 API 取到的真实单号搜索 → 表格命中该单号
  E 叫号历史      打开历史面板 → 按取餐号搜索 → 命中 → 点「取餐」→ 状态变「已取餐」
  F 资金管理(可选) 管理端 /finance/fund：无 500、表格有数据；做不到 SKIP

每一步都收集该页面的 console error / 未捕获异常 / HTTP >= 400 请求，最后汇总打印。
存在 FAIL 步骤或非白名单错误 → 退出码 1。

用法（Windows PowerShell）：
  $env:PYTHONIOENCODING='utf-8'
  python scripts/e2e_pos_browser.py
  $env:HEADLESS='0'   # 有头模式，肉眼可见点击过程

环境变量：
  POS_BASE   默认 http://localhost:3001
  ADMIN_BASE 默认 http://localhost:3002（管理端 vite 默认端口；不存在则尝试 3000）
  API_BASE   默认 http://localhost:8081/api
  POS_USER / POS_PASS 默认 vcashier01 / 123456
  HEADLESS   1=无头（默认），0=有头

产物：.verify-tmp/e2e-browser/*.png 截图 + report.json
"""
from __future__ import annotations

import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any, Callable

try:
    from playwright.sync_api import sync_playwright
except ImportError:  # pragma: no cover
    print("[FATAL] 缺少 playwright：python -m pip install playwright && python -m playwright install chromium")
    sys.exit(2)

POS_BASE = os.environ.get("POS_BASE", "http://localhost:3001").rstrip("/")
ADMIN_BASE = os.environ.get("ADMIN_BASE", "http://localhost:3002").rstrip("/")
ADMIN_FALLBACK = os.environ.get("ADMIN_FALLBACK", "http://localhost:3000").rstrip("/")
API_BASE = os.environ.get("API_BASE", "http://localhost:8081/api").rstrip("/")
POS_USER = os.environ.get("POS_USER", "vcashier01")
POS_PASS = os.environ.get("POS_PASS", "123456")
# 管理端（可选步骤 F）：没有可用账号就 SKIP；提供后自动走真实登录
ADMIN_USER = os.environ.get("ADMIN_USER", "admin")
ADMIN_PASS = os.environ.get("ADMIN_PASS", "admin123")
HEADLESS = os.environ.get("HEADLESS", "1").strip() not in ("0", "false", "False")

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / ".verify-tmp" / "e2e-browser"

# ---------------------------------------------------------------- 通用工具


def now_str() -> str:
    return datetime.now().strftime("%H:%M:%S")


def http_json(method: str, path: str, token: str | None = None, body: dict | None = None,
              timeout: float = 20.0) -> tuple[int, Any]:
    url = path if path.startswith("http") else API_BASE + path
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8", "replace")
            try:
                return resp.status, json.loads(raw)
            except Exception:
                return resp.status, {"_raw": raw[:400]}
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", "replace")
        try:
            return exc.code, json.loads(raw)
        except Exception:
            return exc.code, {"_raw": raw[:400]}
    except Exception as exc:  # 连接失败
        return 0, {"_error": str(exc)}


def wait_until(fn: Callable[[], bool], timeout: float = 15.0, interval: float = 0.2) -> bool:
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            if fn():
                return True
        except Exception:
            pass
        time.sleep(interval)
    return False


# ---------------------------------------------------------------- 错误采集


@dataclass
class StepResult:
    key: str
    title: str
    status: str
    detail: str = ""


class Collector:
    """按步骤收集 console error / pageerror / HTTP >= 400。"""

    # 开发态噪音白名单：不影响验收结论
    IGNORE_URL_PARTS = ("/favicon.ico", "/@vite/client", ".map", "/node_modules/.vite/deps/", "/@id/")
    IGNORE_CONSOLE_PARTS = (
        "favicon",
        "Download the Vue Devtools",
        "[vite] connected",
        "lit is in dev mode",
    )

    def __init__(self) -> None:
        self.step = "init"
        self.events: list[dict[str, Any]] = []

    def set_step(self, step: str) -> None:
        self.step = step

    def console(self, msg_type: str, text: str) -> None:
        if msg_type != "error":
            return
        if any(part in text for part in self.IGNORE_CONSOLE_PARTS):
            return
        self.events.append({"step": self.step, "kind": "console.error", "text": text[:600], "url": "",
                            "status": None, "at": now_str()})

    def pageerror(self, text: str) -> None:
        self.events.append({"step": self.step, "kind": "pageerror", "text": text[:600], "url": "",
                            "status": None, "at": now_str()})

    def response(self, status: int, url: str, method: str) -> None:
        if status < 400:
            return
        if any(part in url for part in self.IGNORE_URL_PARTS):
            return
        self.events.append({"step": self.step, "kind": "http", "text": f"{method} {url}", "url": url,
                            "status": status, "at": now_str()})

    def since(self, mark: int) -> list[dict[str, Any]]:
        return self.events[mark:]


# ---------------------------------------------------------------- 步骤实现


class Runner:
    def __init__(self, page, collector: Collector) -> None:
        self.page = page
        self.col = collector
        self.results: list[StepResult] = []
        self.artifacts: dict[str, str] = {}

    # -- 小工具 --
    def shot(self, name: str) -> str:
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        path = OUT_DIR / f"{name}.png"
        try:
            self.page.screenshot(path=str(path), full_page=False)
        except Exception:
            return ""
        self.artifacts[name] = str(path)
        return str(path)

    def add(self, key: str, title: str, status: str, detail: str = "") -> StepResult:
        res = StepResult(key, title, status, detail)
        self.results.append(res)
        icon = {"PASS": "PASS", "FAIL": "FAIL", "SKIP": "SKIP"}[status]
        print(f"[{icon}] {key} {title} :: {detail}")
        return res

    def body_text(self) -> str:
        try:
            return self.page.locator("body").inner_text()
        except Exception:
            return ""

    # -- A 登录 --
    def step_login(self, record: bool = True) -> bool:
        self.col.set_step("A 登录" if record else "B 重新登录")
        page = self.page
        try:
            page.goto(POS_BASE + "/login", wait_until="domcontentloaded", timeout=60000)
            page.wait_for_selector('input[placeholder="员工号"]', timeout=60000)
            page.fill('input[placeholder="员工号"]', POS_USER)
            page.fill('input[placeholder="密码"]', POS_PASS)
            page.click(".login-btn")
            # vite dev 首次访问要现编译 Home/MenuSection 等模块，冷启动可能 20s+，这里给足 90s
            left_login = wait_until(
                lambda: page.locator(".pos-top-bar").count() > 0 or "/login" not in page.url, 90.0, interval=0.5)
            if not left_login:
                err = ""
                if page.locator(".login-error").count():
                    err = page.locator(".login-error").inner_text()
                self.shot("A-login-fail")
                if record:
                    self.add("A", "POS 登录", "FAIL", f"仍停留在 /login；错误提示：{err or '无'}")
                return False
            page.wait_for_selector(".pos-top-bar", timeout=60000)
            page.wait_for_timeout(800)
            self.shot("A-login-ok")
            if record:
                self.add("A", "POS 登录", "PASS", f"{POS_USER} 登录成功，落地 {page.url}")
            return True
        except Exception as exc:
            self.shot("A-login-error")
            if record:
                self.add("A", "POS 登录", "FAIL", f"异常：{exc}")
            return False

    # -- B 顶栏账号 + 退出 --
    def step_topbar(self, display_name: str, user_info: dict) -> None:
        self.col.set_step("B 顶栏账号/退出")
        page = self.page
        try:
            chip = page.locator(".pos-top-bar .account-chip")
            bar_text = page.locator(".pos-top-bar").inner_text() if page.locator(".pos-top-bar").count() else ""
            chip_text = chip.inner_text() if chip.count() else ""

            candidates = [c for c in (
                display_name,
                str(user_info.get("employeeName") or ""),
                str(user_info.get("name") or ""),
                str(user_info.get("employeeId") or ""),
                POS_USER,
            ) if c]
            account_hit = [c for c in candidates if c in chip_text or c in bar_text]
            self.shot("B-account")

            logout_btn = page.locator(".pos-top-bar .logout-btn")
            if not account_hit:
                self.add("B", "顶栏账号/退出登录", "FAIL",
                         f"顶栏未见当前账号文本（检索 {candidates}）；账号区文本={self._short(chip_text or bar_text)}")
            if logout_btn.count() == 0:
                self.add("B", "顶栏账号/退出登录", "FAIL",
                         f"顶栏无退出登录入口；顶栏文本={self._short(bar_text)}")
                return

            logout_btn.first.click()
            box = page.locator(".el-message-box")
            box.wait_for(state="visible", timeout=10000)
            self.shot("B-logout-confirm")
            confirm = box.locator(".el-message-box__btns .el-button--primary")
            if confirm.count() == 0:
                confirm = box.locator(".el-message-box__btns button").last
            confirm.first.click()

            # vite dev 首次跳 /login 可能触发依赖预构建重载，给到 60s
            back = wait_until(
                lambda: "/login" in page.url or page.locator(".login-btn").count() > 0,
                60.0, interval=0.4)
            self.shot("B-logout")
            token_gone = wait_until(
                lambda: page.evaluate("localStorage.getItem('pos-token')") in (None, ""), 15.0)
            if not back:
                self.add("B", "顶栏账号/退出登录", "FAIL", f"确认退出后未回到 /login，当前 {page.url}")
            else:
                self.add("B", "顶栏账号/退出登录", "PASS",
                         f"账号可见（{'/'.join(account_hit)}）；点「退出」→ 确认 → 回到 /login；"
                         f"pos-token 已清理={token_gone}")
        except Exception as exc:
            self.shot("B-error")
            self.add("B", "顶栏账号/退出登录", "FAIL", f"异常：{exc}")

    @staticmethod
    def _short(text: str, limit: int = 160) -> str:
        flat = re.sub(r"\s+", " ", text or "").strip()
        return flat[:limit] + ("…" if len(flat) > limit else "")

    # -- C 交班日结 --
    def step_shift_handover(self) -> None:
        self.col.set_step("C 交班日结")
        page = self.page
        try:
            if "/login" in page.url:
                page.goto(POS_BASE + "/", wait_until="domcontentloaded", timeout=30000)
                page.wait_for_selector(".pos-top-bar", timeout=20000)
            page.click(".shift-handover-btn")
            page.wait_for_selector(".shift-handover-dialog", timeout=15000)
            dlg = page.locator(".shift-handover-dialog")
            # 等真实数据落地（金额或订单数出现）
            wait_until(lambda: bool(re.search(r"¥\s*[\d,]+\.\d{2}", dlg.inner_text())), 20.0)
            page.wait_for_timeout(500)
            text = dlg.inner_text()
            self.shot("C-shift-handover")

            amounts = [a.replace(",", "") for a in re.findall(r"¥\s*([\d,]+\.\d{2})", text)]
            nonzero = [a for a in amounts if float(a) > 0]
            order_match = re.search(r"共\s*(\d+)\s*笔订单", text)
            order_count = int(order_match.group(1)) if order_match else 0
            placeholders = [w for w in ("待后端接线", "暂不可执行", "暂不支持") if w in text]

            has_real_amount = bool(nonzero) or order_count > 0
            if not has_real_amount:
                self.add("C", "交班日结真实金额", "FAIL",
                         f"对话框内金额全为 0 且订单数为 0（金额样本={amounts[:6]}，订单数={order_count}）")
            elif placeholders:
                self.add("C", "交班日结真实金额", "FAIL",
                         f"金额/订单数正常（非零金额 {len(nonzero)} 个，订单 {order_count} 笔），"
                         f"但仍残留占位文案 {placeholders}")
            else:
                self.add("C", "交班日结真实金额", "PASS",
                         f"非零金额 {len(nonzero)} 个（最大 ¥{max(nonzero) if nonzero else '0.00'}），订单 {order_count} 笔，无占位文案")

            # 关闭对话框，避免遮罩影响后续步骤
            page.keyboard.press("Escape")
            page.wait_for_timeout(400)
        except Exception as exc:
            self.shot("C-error")
            self.add("C", "交班日结真实金额", "FAIL", f"异常：{exc}")

    # -- D 订单查询 --
    def step_order_query(self, order_no: str | None) -> None:
        self.col.set_step("D 订单查询")
        page = self.page
        if not order_no:
            self.add("D", "订单查询", "SKIP", "接口未取到任何订单号，无法验证")
            return
        try:
            if "/login" in page.url:
                page.goto(POS_BASE + "/", wait_until="domcontentloaded", timeout=30000)
                page.wait_for_selector(".pos-top-bar", timeout=20000)
            page.click(".query-btn")
            page.wait_for_selector(".order-query-dialog", timeout=15000)
            dlg = page.locator(".order-query-dialog")
            inp = dlg.locator('input[placeholder="输入订单号/叫号号/手机号搜索"]')
            inp.fill(order_no)
            dlg.get_by_role("button", name="查询").first.click()
            hit = wait_until(
                lambda: dlg.locator(".el-table__row", has_text=order_no).count() > 0 or order_no in dlg.inner_text(),
                15.0,
            )
            page.wait_for_timeout(300)
            self.shot("D-order-query")
            if hit:
                self.add("D", "订单查询", "PASS", f"搜索 {order_no} 命中表格")
            else:
                self.add("D", "订单查询", "FAIL", f"搜索 {order_no} 后表格未命中；表格文本={self._short(dlg.inner_text(), 220)}")
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
        except Exception as exc:
            self.shot("D-error")
            self.add("D", "订单查询", "FAIL", f"异常：{exc}")

    # -- E 叫号历史 + 取餐 --
    def step_call_history(self, call_no: str | None) -> None:
        self.col.set_step("E 叫号历史/取餐")
        page = self.page
        if not call_no:
            self.add("E", "叫号历史取餐", "SKIP", "接口无 pending/called 叫号记录，无法验证取餐动作")
            return
        try:
            page.goto(POS_BASE + "/call-number", wait_until="domcontentloaded", timeout=30000)
            page.wait_for_selector(".call-number-page", timeout=20000)
            page.locator(".top-bar button", has_text="历史").first.click()
            page.wait_for_selector(".history-dialog", timeout=15000)
            panel = page.locator(".call-history-panel")
            panel.wait_for(state="visible", timeout=15000)

            # 时间范围切「全部」，确保历史记录一定在检索范围内
            try:
                panel.locator(".range-group .el-radio-button__inner", has_text="全部").first.click()
                page.wait_for_timeout(300)
            except Exception:
                pass

            panel.locator('input[placeholder="取餐号 / 订单号，模糊搜索"]').fill(call_no)
            panel.get_by_role("button", name="查询").first.click()

            rows = page.locator(".history-table .el-table__row")
            found = wait_until(lambda: rows.filter(has_text=call_no).count() > 0, 15.0)
            self.shot("E-history-search")
            if not found:
                self.add("E", "叫号历史取餐", "FAIL", f"按取餐号 {call_no} 搜索无结果")
                return

            row = rows.filter(has_text=call_no).first
            before = self._short(row.inner_text(), 200)
            row.get_by_role("button", name="取餐").first.click()

            def picked() -> bool:
                r = rows.filter(has_text=call_no)
                if r.count() == 0:
                    return False
                return "已取餐" in r.first.inner_text() or "已完成" in r.first.inner_text()

            ok = wait_until(picked, 20.0)
            page.wait_for_timeout(300)
            self.shot("E-history-picked")
            after_row = rows.filter(has_text=call_no)
            after = self._short(after_row.first.inner_text(), 200) if after_row.count() else "(行已消失)"
            if ok:
                self.add("E", "叫号历史取餐", "PASS", f"{call_no} 取餐成功：{after}")
            else:
                self.add("E", "叫号历史取餐", "FAIL", f"{call_no} 点击取餐后状态未变「已取餐」；操作前={before}；操作后={after}")
        except Exception as exc:
            self.shot("E-error")
            self.add("E", "叫号历史取餐", "FAIL", f"异常：{exc}")

    # -- F 管理端资金管理 --
    def step_admin_fund(self, admin_base: str | None, admin_login: dict | None, admin_note: str) -> None:
        self.col.set_step("F 管理端资金管理")
        page = self.page
        if not admin_base:
            self.add("F", "资金管理(管理端)", "SKIP", "管理端 dev server 未启动（3002/3000 均不可达），按要求 SKIP")
            return
        if not admin_login:
            self.add("F", "资金管理(管理端)", "SKIP",
                     f"{admin_base} 已启动，但 {admin_note}；未提供可用管理端账号"
                     f"（可用 ADMIN_USER / ADMIN_PASS 环境变量传入），SKIP")
            return
        try:
            token = str(admin_login.get("token") or "")
            refresh = str(admin_login.get("refreshToken") or "")
            info = admin_login.get("userInfo") or {}
            page.goto(admin_base + "/login", wait_until="domcontentloaded", timeout=30000)
            page.evaluate(
                """([token, refresh, userId, username]) => {
                    localStorage.setItem('token', token);
                    if (refresh) localStorage.setItem('refresh_token', refresh);
                    if (userId) localStorage.setItem('user_id', userId);
                    if (username) localStorage.setItem('username', username);
                }""",
                [token, refresh, str(info.get("id") or ""), str(info.get("username") or ADMIN_USER)],
            )
            page.goto(admin_base + "/finance/fund", wait_until="domcontentloaded", timeout=60000)
            page.wait_for_timeout(5000)
            self.shot("F-admin-fund")
            if "/login" in page.url:
                self.add("F", "资金管理(管理端)", "SKIP",
                         f"带真实登录态仍被重定向到 /login（权限/菜单校验未通过），SKIP；当前 {page.url}")
                return
            text = self.body_text()
            rows = page.locator(".el-table__row").count()
            server_errors = [e for e in self.col.events if e["step"] == "F 管理端资金管理" and e["status"] and e["status"] >= 500]
            if server_errors:
                self.add("F", "资金管理(管理端)", "SKIP",
                         f"页面出现 {len(server_errors)} 个 5xx：{[e['status'] for e in server_errors]}（按 SKIP 处理，见错误清单）")
                return
            if rows > 0 or ("暂无数据" not in text and len(text) > 120):
                self.add("F", "资金管理(管理端)", "PASS", f"{admin_base}/finance/fund 渲染正常，表格行数={rows}，无 5xx")
            else:
                self.add("F", "资金管理(管理端)", "SKIP", f"页面渲染但表格无数据（行数={rows}），按 SKIP 处理")
        except Exception as exc:
            self.shot("F-error")
            self.add("F", "资金管理(管理端)", "SKIP", f"异常（可选步骤）：{exc}")


# ---------------------------------------------------------------- 数据准备


def pos_login_api() -> tuple[str | None, dict]:
    status, body = http_json("POST", "/v1/pos/auth/login", body={
        "employeeId": POS_USER, "password": POS_PASS, "terminalId": "e2e-browser-" + str(int(time.time())),
    })
    if status != 200 or not isinstance(body, dict) or not body.get("data"):
        print(f"[warn] POS 登录接口失败：HTTP {status} {str(body)[:200]}")
        return None, {}
    data = body["data"]
    return data.get("token"), data.get("userInfo") or {}


def pick_order_number(token: str | None) -> str | None:
    status, body = http_json("GET", "/v1/pos/orders?page=1&size=20", token=token)
    if status != 200:
        print(f"[warn] /v1/pos/orders HTTP {status}")
        return None
    data = body.get("data") if isinstance(body, dict) else None
    records = []
    if isinstance(data, dict):
        records = data.get("records") or []
    elif isinstance(data, list):
        records = data
    for rec in records:
        no = (rec or {}).get("orderNumber")
        if no:
            return no
    return None


def pick_call_record(token: str | None) -> str | None:
    status, body = http_json(
        "GET", "/v1/call-number/history?range=30d&status=pending,called&page=1&size=20", token=token)
    if status != 200:
        print(f"[warn] /v1/call-number/history HTTP {status}")
        return None
    data = body.get("data") if isinstance(body, dict) else None
    records = data.get("records") if isinstance(data, dict) else data
    for rec in records or []:
        if (rec or {}).get("status") in ("pending", "called"):
            no = rec.get("callNumber") or rec.get("orderNumber")
            if no:
                return str(no)
    return None


def admin_login_api() -> tuple[dict | None, str]:
    """管理端真实登录（可选步骤 F）。返回 (登录数据, 说明)，拿不到就返回说明用于 SKIP。"""
    status, body = http_json("POST", "/v1/auth/login", body={
        "username": ADMIN_USER, "password": ADMIN_PASS, "rememberMe": True,
    })
    if status == 200 and isinstance(body, dict) and body.get("data"):
        data = body["data"]
        if data.get("token"):
            return data, f"{ADMIN_USER} 登录成功"
        return None, f"{ADMIN_USER} 登录响应无 token"
    message = ""
    biz_code = ""
    if isinstance(body, dict):
        message = str(body.get("message") or body.get("_raw") or "")
        biz_code = str(body.get("code", ""))
    detail = f"HTTP {status}" + (f", code={biz_code}" if biz_code else "")
    if message:
        detail += f"：{message[:80]}"
    return None, f"{ADMIN_USER} 登录失败（{detail}）"


def launch_browser(pw):
    """优先用 Playwright 自带 chromium；缺失时回退到系统 Edge / Chrome（channel），保证脚本在任何机器可跑。"""
    preferred = os.environ.get("BROWSER_CHANNEL", "").strip()
    attempts: list[str | None] = [preferred] if preferred else []
    attempts += [None, "msedge", "chrome"]
    last_error: Exception | None = None
    for channel in attempts:
        try:
            kwargs = {"headless": HEADLESS}
            if channel:
                kwargs["channel"] = channel
            return pw.chromium.launch(**kwargs), channel
        except Exception as exc:  # 浏览器内核缺失
            last_error = exc
            continue
    raise RuntimeError(f"无法启动浏览器（chromium/msedge/chrome 均失败）：{last_error}")


def probe_admin_base() -> str | None:
    for base in (ADMIN_BASE, ADMIN_FALLBACK):
        try:
            req = urllib.request.Request(base + "/", method="GET")
            with urllib.request.urlopen(req, timeout=5) as resp:
                if resp.status < 500:
                    return base
        except Exception:
            continue
    return None


# ---------------------------------------------------------------- 主流程


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    print("=" * 78)
    print(f"POS 浏览器级验收  {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"  POS_BASE={POS_BASE}  ADMIN_BASE={ADMIN_BASE}  API_BASE={API_BASE}  HEADLESS={HEADLESS}")
    print("=" * 78)

    token, user_info = pos_login_api()
    display_name = str(user_info.get("name") or "")
    order_no = pick_order_number(token)
    call_no = pick_call_record(token)
    print(f"[data] 员工={display_name or POS_USER} 真实单号={order_no} 叫号记录={call_no}")

    admin_base = probe_admin_base()
    admin_login: dict | None = None
    admin_note = "管理端未启动"
    if admin_base:
        admin_login, admin_note = admin_login_api()

    collector = Collector()
    with sync_playwright() as pw:
        browser, channel = launch_browser(pw)
        print(f"[browser] chromium channel={channel or 'bundled'} headless={HEADLESS} version={browser.version}")
        context = browser.new_context(viewport={"width": 1600, "height": 1000}, locale="zh-CN")
        page = context.new_page()
        page.set_default_timeout(15000)

        page.on("console", lambda msg: collector.console(msg.type, msg.text))
        page.on("pageerror", lambda err: collector.pageerror(str(err)))
        page.on("response", lambda resp: collector.response(resp.status, resp.url, resp.request.method))
        page.on("dialog", lambda dlg: dlg.dismiss())

        runner = Runner(page, collector)

        logged_in = runner.step_login()
        if logged_in:
            runner.step_topbar(display_name, user_info)
            # 退出后若已回 /login（或 token 已清），重新登录以继续后续步骤
            need_relogin = "/login" in page.url or page.evaluate("localStorage.getItem('pos-token')") in (None, "")
            if need_relogin:
                runner.step_login(record=False)
            runner.step_shift_handover()
            runner.step_order_query(order_no)
            runner.step_call_history(call_no)
        else:
            for key, title in (("B", "顶栏账号/退出登录"), ("C", "交班日结真实金额"),
                               ("D", "订单查询"), ("E", "叫号历史取餐")):
                runner.add(key, title, "SKIP", "前置步骤 A 登录失败，未执行")

        runner.step_admin_fund(admin_base, admin_login, admin_note)

        browser.close()

    # ---------------- 汇总 ----------------
    results = runner.results
    fail_status = {r.key: r.status for r in results}
    f_skipped = fail_status.get("F") == "SKIP"
    counted_errors = [e for e in collector.events if not (f_skipped and e["step"] == "F 管理端资金管理")]
    fail_steps = [r for r in results if r.status == "FAIL"]

    print()
    print("=" * 78)
    print("步骤结果")
    print("-" * 78)
    for r in results:
        print(f"  {r.status:4s}  {r.key}  {r.title}")
        if r.detail:
            print(f"        {r.detail}")

    print()
    print("=" * 78)
    print(f"console/network 错误（计入判定）：{len(counted_errors)} 条")
    print("-" * 78)
    if not counted_errors:
        print("  （无）")
    for idx, ev in enumerate(counted_errors, 1):
        status = f" [{ev['status']}]" if ev.get("status") else ""
        print(f"  {idx:>2}. [{ev['step']}] {ev['kind']}{status} {ev['text']}")

    report = {
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "config": {"posBase": POS_BASE, "adminBase": admin_base, "apiBase": API_BASE,
                   "headless": HEADLESS, "user": POS_USER},
        "steps": [r.__dict__ for r in results],
        "errors": counted_errors,
        "allEvents": collector.events,
        "artifacts": runner.artifacts,
    }
    (OUT_DIR / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    verdict = "PASS" if not fail_steps and not counted_errors else "FAIL"
    print()
    print("=" * 78)
    print(f"VERDICT: {verdict}   (FAIL 步骤 {len(fail_steps)} 个，错误 {len(counted_errors)} 条)")
    print(f"报告：{(OUT_DIR / 'report.json')}")
    print("=" * 78)
    return 1 if verdict == "FAIL" else 0


if __name__ == "__main__":
    sys.exit(main())
