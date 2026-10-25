#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""阶段 2-A：Playwright UI 三处比对防线（POS 列表 / POS 详情 / 管理端财务流水详情）

一条命令跑完：
  python scripts/qa/ui_e2e_payment.py

做的事情
  0) 预检 POS 3001 / 管理端 3002 / 后端 8081（缺服务直接报错退出，不静默 SKIP）
  1) HTTP 造数：POS 登录 → 下单（1 份）→ 现金支付 → 拿 orderNumber / 应付金额
     → 再用 /v1/finance/fund-flows?keyword=<orderNumber> 定位 flowId（仅用于定位，UI 仍点开详情）
  2) 浏览器真实点击：
     a. POS「订单」查询弹窗 → 搜单号 → 读该行「支付方式」「金额」
     b. 该行「详情」→ 读订单详情弹窗里的「支付方式」「订单金额」
     c. 管理端 /finance/fund → 找到该单流水 → 「详情」→ 读「关联 POS 订单」区「支付方式」「订单金额(元)」
  3) 断言
     · 支付方式：去空格 + 去「支付」后缀后三者严格相等；同时比对三者原文是否完全一致（发现文案漂移）
     · 金额：三者按「元」两位小数严格相等，且等于下单金额
  4) 证据：.verify-tmp/qa-ui-payment/<时间戳>/{pos-list,pos-detail,admin-detail}.png + result.json
  5) 退出码 0/1；console.error 与 HTTP>=400（白名单 favicon/vite）计入失败原因

账号（可用参数覆盖）：POS vcashier01/123456；管理端 qa_admin/Admin@123456
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
from urllib.parse import quote

try:
    from playwright.sync_api import sync_playwright
except ImportError:  # pragma: no cover
    print("[FATAL] 缺少 playwright：python -m pip install playwright && python -m playwright install chromium")
    sys.exit(2)

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_POS = "http://localhost:3001"
DEFAULT_ADMIN = "http://localhost:3002"
DEFAULT_API = "http://localhost:8081/api"

IGNORE_URL_PARTS = ("/favicon.ico", "/@vite/client", ".map", "/node_modules/.vite", "/@id/")
IGNORE_CONSOLE_PARTS = ("favicon", "Download the Vue Devtools", "[vite] connected")


# ---------------------------------------------------------------- 基础工具
def now() -> str:
    return datetime.now().strftime("%H:%M:%S")


def http(method: str, url: str, token: str | None = None, body: dict | None = None, timeout: float = 30.0):
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
                return resp.status, {"_raw": raw[:300]}
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"_raw": raw[:300]}
    except Exception as e:
        return -1, {"error": str(e)}


def ok(body) -> bool:
    return isinstance(body, dict) and body.get("code") in (0, 200)


def wait_until(fn, timeout=30.0, interval=0.3) -> bool:
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            if fn():
                return True
        except Exception:
            pass
        time.sleep(interval)
    return False


def probe(url: str) -> bool:
    try:
        req = urllib.request.Request(url, method="GET")
        with urllib.request.urlopen(req, timeout=5) as resp:
            return resp.status < 500
    except urllib.error.HTTPError as e:
        return e.code < 500  # 401 等也算服务在
    except Exception:
        return False


def norm_payment(text: str) -> str:
    """去空格 + 去尾部「支付」后缀：现金支付/现金 → 现金；微信支付 → 微信"""
    t = re.sub(r"\s+", "", (text or "").strip())
    return t[:-2] if t.endswith("支付") and len(t) > 2 else t


def parse_amount(text: str) -> float | None:
    m = re.search(r"(-?\d+(?:\.\d+)?)", (text or "").replace(",", ""))
    return round(float(m.group(1)), 2) if m else None


NORMALIZERS = {"payment": norm_payment, "amount": parse_amount}


# ---------------------------------------------------------------- 采集器
class Collector:
    def __init__(self):
        self.events = []
        self.step = "init"

    def set_step(self, step: str):
        self.step = step

    def console(self, msg_type, text):
        if msg_type != "error":
            return
        if any(p in text for p in IGNORE_CONSOLE_PARTS):
            return
        self.events.append({"step": self.step, "kind": "console.error", "text": text[:400], "at": now()})

    def pageerror(self, text):
        self.events.append({"step": self.step, "kind": "pageerror", "text": str(text)[:400], "at": now()})

    def response(self, status, url, method):
        if status < 400:
            return
        if any(p in url for p in IGNORE_URL_PARTS):
            return
        self.events.append({"step": self.step, "kind": "http", "status": status,
                            "text": "%s %s" % (method, url), "at": now()})


# ---------------------------------------------------------------- 数据准备
class Prep:
    def __init__(self, a):
        self.a = a
        self.order_no = None
        self.order_id = None
        self.amount = None
        self.flow_no = None
        self.flow_id = None
        self.expected_payment = None

    def run(self) -> bool:
        st, body = http("POST", self.a.api + "/v1/pos/auth/login",
                        body={"employeeId": self.a.pos_user, "password": self.a.pos_password,
                              "terminalId": "qa-ui-" + str(int(time.time()))})
        if not ok(body):
            print("  [FAIL] POS 登录接口：HTTP %s %s" % (st, str(body)[:200]))
            return False
        token = body["data"]["token"]

        st, body = http("GET", self.a.api + "/v1/pos/api/dishes", token=token)
        dishes = (body.get("data") if isinstance(body, dict) else None) or []
        dish = next((d for d in dishes if d.get("dishId") and (d.get("price") or 0) > 0), None)
        if not dish:
            print("  [FAIL] 没有可用菜品，无法造单")
            return False

        item = {"productId": str(dish["dishId"]), "productName": dish.get("dishName") or "",
                "quantity": 1, "price": float(dish.get("price") or 0),
                "dishType": dish.get("dishType") or "single"}
        st, body = http("POST", self.a.api + "/v1/pos/orders/order", token=token,
                        body={"orderType": "dinein", "items": [item],
                              "idempotencyKey": "qa-ui-" + str(int(time.time() * 1000))})
        if not ok(body):
            print("  [FAIL] 下单失败：HTTP %s %s" % (st, str(body)[:200]))
            return False
        data = body["data"]
        self.order_id = data.get("orderId")
        self.order_no = data.get("orderNumber")
        self.amount = round(float(data.get("totalAmount") or 0), 2)

        st, body = http("POST", self.a.api + "/v1/pos/orders/order/pay", token=token,
                        body={"orderId": self.order_id, "paymentMethod": "现金", "amount": self.amount})
        if not ok(body):
            print("  [FAIL] 支付失败：HTTP %s %s" % (st, str(body)[:200]))
            return False
        pay = body.get("data") or {}

        # 管理端 token 只用于定位 flowId（UI 仍然真实点开详情）。
        # 注意 /v1/auth/login 有 IP 限流（60s 内 5 次），这一步失败不影响结论，UI 会按单号在列表里找行。
        st, body = http("POST", self.a.api + "/v1/auth/login",
                        body={"username": self.a.admin_user, "password": self.a.admin_password})
        admin_token = (body.get("data") or {}).get("token") if ok(body) else None
        if not admin_token:
            print("  [WARN] 造数阶段管理端登录不可用（%s），flowId 定位跳过，UI 步骤会按单号找行" %
                  str(body.get("message") if isinstance(body, dict) else body)[:80])
        if admin_token:
            st, body = http("GET", self.a.api + "/v1/finance/fund-flows?" +
                            "keyword=" + quote(self.order_no) + "&current=1&size=10", token=admin_token)
            records = ((body.get("data") or {}).get("records") or []) if ok(body) else []
            hit = next((r for r in records if self.order_no in str(r.get("remark") or "")), None)
            if hit:
                self.flow_no = hit.get("flowNo")
                self.flow_id = hit.get("flowId")

        self.expected_payment = "现金"
        print("  [PREP] 订单=%s 金额=%.2f 元 流水号=%s flowId=%s fundFlowNo=%s" %
              (self.order_no, self.amount, self.flow_no, self.flow_id, pay.get("fundFlowNo")))
        return True


# ---------------------------------------------------------------- 浏览器步骤
class Ui:
    def __init__(self, page, out_dir: Path, shot_prefix=""):
        self.page = page
        self.out = out_dir
        self.shots = {}

    def shot(self, name):
        path = self.out / ("%s.png" % name)
        self.page.screenshot(path=str(path), full_page=False)
        self.shots[name] = str(path)
        print("  [SHOT] %s" % path)
        return str(path)

    # ---- 通用：读 el-descriptions 里某个 label 的取值 ----
    def desc_value(self, scope: str, label: str) -> str:
        """读 el-descriptions（border 模式）里指定 label 的取值。

        注意：:column="2" 时一行表格里有多组 label/value（th,td,th,td），
        必须取「label 那个 th 的下一个 td」，不能用 tr 的最后一个 td（会串到相邻字段）。
        """
        scope_loc = self.page.locator(scope).first
        th = scope_loc.locator("th", has_text=label)
        if th.count():
            cell = th.first.locator("xpath=following-sibling::td[1]")
            if cell.count():
                return cell.first.inner_text().strip()
        # 兜底 1：非 border 模式（label/content 都在同一个 td 里）
        item = scope_loc.locator(".el-descriptions__label", has_text=label)
        if item.count():
            cell = item.first.locator("xpath=following-sibling::*[1]")
            if cell.count():
                return cell.first.inner_text().strip()
        # 兜底 2：整行文本里剥掉 label
        rows = scope_loc.locator("tr", has_text=label)
        if rows.count():
            return rows.first.inner_text().replace(label, "").strip()
        return ""

    def table_cell_by_header(self, table_scope: str, row_locator, header: str) -> str:
        heads = self.page.locator(table_scope + " .el-table__header th")
        idx = -1
        for i in range(heads.count()):
            if header in heads.nth(i).inner_text():
                idx = i
                break
        if idx < 0:
            return ""
        cells = row_locator.locator("td")
        return cells.nth(idx).inner_text().strip() if cells.count() > idx else ""

    # ---- a. POS 列表 ----
    def pos_list(self, order_no: str) -> dict:
        page = self.page
        page.goto(self.pos + "/login", wait_until="domcontentloaded", timeout=60000)
        page.wait_for_selector('input[placeholder="员工号"]', timeout=90000)
        page.fill('input[placeholder="员工号"]', self.pos_user)
        page.fill('input[placeholder="密码"]', self.pos_password)
        page.click(".login-btn")
        if not wait_until(lambda: page.evaluate("!!localStorage.getItem('pos-token')"), 60):
            raise RuntimeError("POS 登录未拿到 pos-token")
        page.wait_for_selector(".pos-top-bar", timeout=180000)

        page.click(".query-btn")
        dlg = page.locator(".order-query-dialog")
        dlg.wait_for(state="visible", timeout=30000)
        inp = dlg.locator('input[placeholder="输入订单号/叫号号/手机号搜索"]')
        inp.fill(order_no)
        dlg.get_by_role("button", name="查询").first.click()

        row = page.locator(".order-query-dialog .el-table__row").filter(has_text=order_no).first
        if not wait_until(lambda: row.count() > 0, 30):
            raise RuntimeError("POS 订单列表未出现订单 %s" % order_no)
        row.wait_for(state="visible", timeout=15000)

        payment = self.table_cell_by_header(".order-query-dialog", row, "支付方式")
        amount_text = self.table_cell_by_header(".order-query-dialog", row, "金额")
        self.shot("pos-list")

        row.get_by_role("button", name="详情").first.click()
        detail = page.locator(".order-detail-dialog")
        detail.wait_for(state="visible", timeout=30000)
        detail_payment = self.desc_value(".order-detail-dialog", "支付方式")
        detail_amount = self.desc_value(".order-detail-dialog", "订单金额")
        # 详情弹窗会被同时打开的「订单查询」弹窗压在下面，直接截该弹窗元素，证据更清晰
        detail_path = self.out / "pos-detail.png"
        detail.screenshot(path=str(detail_path))
        self.shots["pos-detail"] = str(detail_path)
        print("  [SHOT] %s" % detail_path)
        page.keyboard.press("Escape")
        page.wait_for_timeout(400)

        return {"payment": payment, "amount": amount_text,
                "detailPayment": detail_payment, "detailAmount": detail_amount}

    # ---- c. 管理端 ----
    def admin_detail(self, order_no: str) -> dict:
        page = self.page
        page.goto(self.admin + "/login", wait_until="domcontentloaded", timeout=90000)
        user = page.locator(".login-page .float-label input").first
        user.wait_for(state="visible", timeout=90000)
        user.fill(self.admin_user)
        page.locator(".login-page .float-label input[type='password']").first.fill(self.admin_password)

        # /v1/auth/login 有 IP 限流（60s/5 次）：被限流时退避重试，不把环境因素当业务失败
        logged = False
        for attempt in range(1, 4):
            page.locator(".login-page .submit-btn").first.click()
            logged = wait_until(lambda: page.evaluate("!!localStorage.getItem('token')"), 45)
            if logged:
                break
            if page.locator('.login-page input[placeholder="请输入图中字符"]').count():
                raise RuntimeError("管理端登录要求验证码（多次失败触发），请人工登录一次或检查账号")
            msg = ""
            if page.locator(".el-message").count():
                msg = page.locator(".el-message").first.inner_text()
            print("  [WARN] 管理端登录第 %d 次未成功（提示：%s），等待 25s 重试" % (attempt, msg or "无"))
            page.wait_for_timeout(25000)
        if not logged:
            raise RuntimeError("管理端登录未拿到 token（重试 3 次仍失败，检查 3002 服务与账号）")

        page.goto(self.admin + "/finance/fund", wait_until="domcontentloaded", timeout=180000)
        rows = page.locator(".el-table__row")
        if not wait_until(lambda: rows.count() > 0, 180, interval=0.5):
            raise RuntimeError("资金管理页未渲染出流水行（%s）" % page.url)
        row = rows.filter(has_text=order_no).first
        if not wait_until(lambda: row.count() > 0, 20):
            raise RuntimeError("资金管理首页未找到含单号 %s 的流水" % order_no)
        row.get_by_role("button", name="详情").first.click()

        dlg = page.locator(".fund-detail-dialog")
        dlg.wait_for(state="visible", timeout=30000)
        section = dlg.locator(".order-section")
        section.wait_for(state="visible", timeout=30000)
        # 关联订单明细是异步补拉的：等「支付方式」行出现
        if not wait_until(lambda: section.locator("tr", has_text="支付方式").count() > 0, 30):
            raise RuntimeError("关联 POS 订单区未渲染出支付方式（可能未关联订单）")
        payment = self.desc_value(".fund-detail-dialog .order-section", "支付方式")
        amount_text = self.desc_value(".fund-detail-dialog .order-section", "订单金额")
        self.shot("admin-detail")
        return {"payment": payment, "amount": amount_text}


# ---------------------------------------------------------------- 主流程
def main() -> int:
    p = argparse.ArgumentParser(description="UI 三处比对防线（POS 列表 / POS 详情 / 管理端流水详情）")
    p.add_argument("--pos", default=DEFAULT_POS)
    p.add_argument("--admin", default=DEFAULT_ADMIN)
    p.add_argument("--api", default=DEFAULT_API)
    p.add_argument("--pos-user", default="vcashier01")
    p.add_argument("--pos-password", default="123456")
    p.add_argument("--admin-user", default="qa_admin")
    p.add_argument("--admin-password", default="Admin@123456")
    p.add_argument("--headless", default=os.environ.get("HEADLESS", "1"))
    p.add_argument("--allow-copy-drift", action="store_true",
                   help="允许「支付方式」三处原文不一致（仅归一后必须一致）；默认原文不一致即 FAIL，用于发现文案漂移")
    a = p.parse_args()
    a.headless = str(a.headless).strip() not in ("0", "false", "False")

    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    out_dir = ROOT / ".verify-tmp" / "qa-ui-payment" / stamp
    out_dir.mkdir(parents=True, exist_ok=True)

    print("=" * 78)
    print("UI 三处比对防线  %s" % datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
    print("  POS=%s  ADMIN=%s  API=%s  HEADLESS=%s" % (a.pos, a.admin, a.api, a.headless))
    print("  证据目录=%s" % out_dir)
    print("=" * 78)

    # ---- 预检 + 库存自备（复用 e2e_setup；E2E 不再依赖「别人有没有提前补库存」）----
    try:
        sys.path.insert(0, str(Path(__file__).resolve().parent))
        from e2e_setup import ensure_ready
    except Exception as e:
        print("[FATAL] 无法导入 e2e_setup：%s" % e)
        return 2
    if not ensure_ready(api=a.api, admin=a.admin, pos=a.pos):
        print("[NOT_READY] 前置未就绪（见上），退出码 2")
        return 2

    prep = Prep(a)
    print("[1/3] HTTP 造数")
    if not prep.run():
        return 1

    collector = Collector()
    result = {
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "orderNumber": prep.order_no,
        "flowNo": prep.flow_no,
        "expected": {"payment": prep.expected_payment, "amount": prep.amount},
        "raw": {}, "normalized": {}, "checks": {}, "errors": [], "screenshots": {},
        "status": "FAIL",
    }

    with sync_playwright() as pw:
        browser = None
        channel = None
        for ch in (None, "msedge", "chrome"):
            try:
                kwargs = {"headless": a.headless}
                if ch:
                    kwargs["channel"] = ch
                browser = pw.chromium.launch(**kwargs)
                channel = ch
                break
            except Exception:
                continue
        if browser is None:
            print("[FATAL] 无法启动浏览器（chromium/msedge/chrome 均失败）")
            return 2
        print("[2/3] 浏览器 chromium=%s 版本=%s" % (channel or "bundled", browser.version))

        ctx = browser.new_context(viewport={"width": 1600, "height": 1000}, locale="zh-CN")
        page = ctx.new_page()
        page.set_default_timeout(20000)
        page.on("console", lambda m: collector.console(m.type, m.text))
        page.on("pageerror", lambda e: collector.pageerror(e))
        page.on("response", lambda r: collector.response(r.status, r.url, r.request.method))
        page.on("dialog", lambda d: d.dismiss())

        ui = Ui(page, out_dir)
        ui.pos = a.pos
        ui.admin = a.admin
        ui.pos_user = a.pos_user
        ui.pos_password = a.pos_password
        ui.admin_user = a.admin_user
        ui.admin_password = a.admin_password

        try:
            collector.set_step("POS 列表")
            print("[3/3] UI 点击")
            pos = ui.pos_list(prep.order_no)
            collector.set_step("POS 详情")
            # 详情数据已在 pos_list 里一并读取（点的是同一行的详情按钮）
            admin = ui.admin_detail(prep.order_no)
        except Exception as exc:
            print("[FAIL] UI 步骤异常：%s" % exc)
            try:
                ui.shot("error")
            except Exception:
                pass
            result["errors"] = [{"step": collector.step, "kind": "exception", "text": str(exc)[:300]}]
            result["screenshots"] = ui.shots
            (out_dir / "result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
            browser.close()
            print("报告：%s" % (out_dir / "result.json"))
            return 1

        browser.close()

    # ---- 断言 ----
    raw = {
        "posList": {"payment": pos["payment"], "amount": pos["amount"]},
        "posDetail": {"payment": pos["detailPayment"], "amount": pos["detailAmount"]},
        "adminDetail": {"payment": admin["payment"], "amount": admin["amount"]},
    }
    normalized = {k: {"payment": norm_payment(v["payment"]), "amount": parse_amount(v["amount"])}
                  for k, v in raw.items()}

    checks = {}
    pay_norm = [normalized[k]["payment"] for k in ("posList", "posDetail", "adminDetail")]
    pay_raw = [raw[k]["payment"] for k in ("posList", "posDetail", "adminDetail")]
    amt_norm = [normalized[k]["amount"] for k in ("posList", "posDetail", "adminDetail")]

    checks["paymentNormalizedEqual"] = len(set(pay_norm)) == 1 and pay_norm[0] != ""
    checks["paymentRawIdentical"] = len(set(pay_raw)) == 1      # 文案漂移探测（原文必须完全一致）
    checks["paymentMatchesExpected"] = pay_norm[0] == norm_payment(prep.expected_payment)
    checks["amountStrictEqual"] = len(set(amt_norm)) == 1 and amt_norm[0] is not None
    checks["amountMatchesOrder"] = amt_norm[0] == round(prep.amount, 2)

    # 浏览器对白名单静态资源（favicon 等）的加载失败只会打一条无 URL 的 console.error；
    # 若网络层没有任何真实 4xx/5xx，这类通用噪音不计入失败（真实接口失败会有带 URL 的 http 事件）。
    events = list(collector.events)
    if not [e for e in events if e["kind"] == "http"]:
        events = [e for e in events
                  if not (e["kind"] == "console.error" and e["text"].startswith("Failed to load resource"))]
    console_errors = [e for e in events if e["kind"] in ("console.error", "pageerror")]
    http_errors = [e for e in events if e["kind"] == "http"]
    checks["noConsoleError"] = not console_errors
    checks["noHttpError"] = not http_errors

    failed = [k for k, v in checks.items() if not v]
    if a.allow_copy_drift and failed == ["paymentRawIdentical"]:
        # 文案漂移已确认存在，按参数放行（归一值仍必须一致）
        failed = []

    result.update({"raw": raw, "normalized": normalized, "checks": checks,
                   "copyDriftAllowed": bool(a.allow_copy_drift),
                   "errors": events, "screenshots": ui.shots,
                   "status": "PASS" if not failed else "FAIL"})
    (out_dir / "result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")

    print()
    print("=" * 78)
    print("三处原文")
    for k, label in (("posList", "POS 列表"), ("posDetail", "POS 详情"), ("adminDetail", "管理端流水详情")):
        print("  %-12s 支付方式=%-10s 金额=%s" % (label, raw[k]["payment"] or "(空)", raw[k]["amount"] or "(空)"))
    print("归一后：支付方式=%s 金额=%s" % (pay_norm, amt_norm))
    print("期望：  支付方式=%s 金额=%.2f" % (prep.expected_payment, prep.amount))
    print("-" * 78)
    for name, passed in checks.items():
        if not passed and name == "paymentRawIdentical" and a.allow_copy_drift:
            print("  [WARN] %s（文案漂移已按 --allow-copy-drift 放行，归一值仍必须一致）" % name)
            continue
        print("  [%s] %s" % ("PASS" if passed else "FAIL", name))
    if events:
        print("-" * 78)
        print("console/network 错误：")
        for e in events:
            print("   - [%s] %s %s" % (e.get("step"), e.get("kind"), e.get("text")))
    print("-" * 78)
    print("截图：")
    for name, path in ui.shots.items():
        print("   %-12s %s" % (name, path))
    print("结果：%s  (FAIL 项 %d)" % (result["status"], len(failed)))
    print("报告：%s" % (out_dir / "result.json"))
    print("=" * 78)
    return 0 if result["status"] == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
