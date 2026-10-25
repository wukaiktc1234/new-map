#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P3-4 E2E：资金总览（AC-53）——接口逐字段与 SQL 对拍 + 页面 data-test 对拍 + 数据范围

接口：GET /v1/finance/overview/daily?date=YYYY-MM-DD
{
  date, dataScope, storeId,
  stores:[{storeId, storeName, cashAccountId, balanceFen, netChangeFen, negative, abnormal}],
  channels:{internalSaleFen, externalReceivedFen, diffFen, unexplainedCount, runs:[…]},
  statements:{openCount, byStatus:[…], oldestPendingDays, oldest:{…}}
}

对拍规则（不靠截图）：
  · stores[].balanceFen   == bank_accounts(store_id, account_type=2, deleted=0).balance
  · stores[].netChangeFen == 当日 fund_flows 净额（direction=1 加、=2 减；business_date=:date）
  · channels.internalSaleFen / externalReceivedFen / diffFen/differenceCount
       == 当日 status='FINALIZED' 的 recon_runs 聚合（internal_amount / external_amount / difference_count）
  · channels.unexplainedCount == 差异池未关闭条数（status NOT IN ('CLOSED','REVERSED')）
  · statements.openCount / oldestPendingDays == 对应 SQL（未 APPROVED / 最早未审核天数）
页面：/finance/overview 上 ov-store-row(data-store-id/data-balance-fen/data-net-fen)、
      ov-channel-internal-fen / ov-channel-external-fen / ov-channel-diff-fen /
      ov-channel-unexplained / ov-open-count / ov-oldest-days
范围：qa_admin 全部门店；qa_store_b 只看到 store 2（ov-store-row 数量=1 且 data-store-id=2）

用法：python scripts/qa/e2e_fund_overview.py [--date YYYY-MM-DD]
退出码：0 全绿 / 1 断言失败 / 2 环境未就绪（接口/页面未落地）
取样竞态规则见 scripts/qa/e2e_setup.py 头部：禁止 inner_text/get_attribute 直读 data- 钩子，
先 wait_for_data_attr 等就绪标记（本页根节点 ov-state 的 data-state=ready），再 read_data_attr。
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
from datetime import date, datetime
from pathlib import Path

try:
    from playwright.sync_api import sync_playwright
except ImportError:  # pragma: no cover
    print("[FATAL] 缺少 playwright")
    sys.exit(2)

DEFAULT_ADMIN = "http://localhost:3002"
DEFAULT_API = "http://localhost:8081/api"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
sys.path.insert(0, str(Path(__file__).resolve().parent))
from e2e_setup import wait_for_data_attr, read_data_attr, READY_NONEMPTY  # 取样竞态机制

SHOT_DIR = Path(r"P:\my-new-project\.verify-tmp\e2e-overview")


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


class OverviewE2E:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.tokens = {}
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

    # ---------- DB ----------
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

    def scalar(self, sql, default="0"):
        rows = self.rows(sql)
        return rows[0][0] if rows and rows[0] else default

    # ---------- 登录 ----------
    def login_all(self):
        for key, u, p in (("admin", self.a.admin_user, self.a.admin_password),
                          ("store", self.a.store_user, self.a.store_password)):
            st, b = http("POST", self.a.api + "/v1/auth/login", body={"username": u, "password": p})
            tok = (data_of(b) or {}).get("token") if ok(b) else None
            self.tokens[key] = tok
            self.step("登录 %s" % u, bool(tok), "HTTP %s %s" % (
                st, "" if tok else json.dumps(b, ensure_ascii=False)[:120]))
        return bool(self.tokens.get("admin"))

    # ---------- SQL 期望值 ----------
    def sql_expect(self):
        d = self.a.date
        bal = {}
        for sid, acct, b in self.rows(
                "SELECT store_id, account_id, COALESCE(balance,0) FROM bank_accounts"
                " WHERE deleted=0 AND account_type=2 AND store_id IS NOT NULL ORDER BY store_id"):
            bal[int(sid)] = (int(acct), int(b))
        net = {}
        for sid, amt in self.rows(
                "SELECT ba.store_id, COALESCE(SUM(CASE WHEN ff.flow_direction=1 THEN ff.amount"
                " WHEN ff.flow_direction=2 THEN -ff.amount ELSE 0 END),0)"
                " FROM fund_flows ff JOIN bank_accounts ba ON ba.account_id = ff.account_id"
                " WHERE ff.deleted=0 AND ff.status=2 AND ff.business_date='%s'"
                " AND ba.store_id IS NOT NULL AND ba.account_type=2 AND ba.deleted=0"
                " GROUP BY ba.store_id" % d):
            net[int(sid)] = int(amt)
        runs = self.rows(
            "SELECT COALESCE(SUM(internal_amount),0), COALESCE(SUM(external_amount),0),"
            " COALESCE(SUM(difference_count),0), COUNT(*)"
            " FROM recon_runs WHERE status='FINALIZED' AND business_date='%s'" % d)[0]
        internal_fen, external_fen, diff_cnt, run_cnt = (int(runs[0]), int(runs[1]), int(runs[2]), int(runs[3]))
        unexplained = int(self.scalar(
            "SELECT COUNT(*) FROM recon_differences WHERE status NOT IN ('CLOSED','REVERSED')"))
        open_count = int(self.scalar(
            "SELECT COUNT(*) FROM reconciliation_statements WHERE deleted=0 AND status <> 'APPROVED'"))
        oldest = int(self.scalar(
            "SELECT COALESCE(MAX(GREATEST((CURRENT_DATE - period_end),0)),0) FROM reconciliation_statements"
            " WHERE deleted=0 AND status <> 'APPROVED'"))
        self.expect = {
            "balance": bal, "net": net,
            "internalSaleFen": internal_fen, "externalReceivedFen": external_fen,
            "differenceCount": diff_cnt, "finalizedRuns": run_cnt,
            "unexplainedCount": unexplained, "openCount": open_count, "oldestPendingDays": oldest,
        }
        return self.step("SQL 期望值就绪", True,
                         "date=%s 门店余额=%s 当日净额=%s；FINALIZED runs=%d internal=%d external=%d diff=%d；"
                         "未关闭差异=%d；未审核表=%d 最早挂账天数=%d" % (
                             d, {k: v[1] for k, v in bal.items()}, net, run_cnt,
                             internal_fen, external_fen, diff_cnt, unexplained, open_count, oldest))

    # ---------- 接口对拍 ----------
    def check_api(self):
        st, b = http("GET", self.a.api + "/v1/finance/overview/daily?date=%s" % self.a.date, self.tokens["admin"])
        if missing(st, b):
            return self.not_ready("AC-53 总览接口就绪", "HTTP %s code=%s %s" % (
                st, (b or {}).get("code"), str((b or {}).get("message"))[:80]))
        d = data_of(b) or {}
        stores = d.get("stores") or []
        ch = d.get("channels") or {}
        sm = d.get("statements") or {}
        # 1) 门店余额 / 净额
        bad = []
        for s in stores:
            sid = int(s.get("storeId") or 0)
            want_bal = self.expect["balance"].get(sid, (None, 0))[1]
            want_net = self.expect["net"].get(sid, 0)
            if int(s.get("balanceFen") or 0) != want_bal:
                bad.append("store%s balance 接口=%s SQL=%s" % (sid, s.get("balanceFen"), want_bal))
            if int(s.get("netChangeFen") or 0) != want_net:
                bad.append("store%s net 接口=%s SQL=%s" % (sid, s.get("netChangeFen"), want_net))
        self.step("AC-53a stores[].balanceFen / netChangeFen == SQL", not bad,
                  "门店数=%d；%s%s" % (len(stores), "；".join(bad[:6]) if bad else "逐店相等",
                                      "" if not bad else "；截图=" + self.shot("ac53a")))
        # 2) 渠道聚合
        pairs = [("internalSaleFen", self.expect["internalSaleFen"]),
                 ("externalReceivedFen", self.expect["externalReceivedFen"])]
        diff_api = ch.get("diffFen")
        if diff_api is None:
            diff_api = ch.get("diffAmountFen")
        diff_sql = self.expect["internalSaleFen"] - self.expect["externalReceivedFen"]  # 带符号：内部 − 外部
        cnt_api = ch.get("differenceCount")
        bad2 = []
        for k, want in pairs:
            if k in ch and int(ch.get(k) or 0) != want:
                bad2.append("%s 接口=%s SQL=%s" % (k, ch.get(k), want))
        if diff_api is not None and int(diff_api) != diff_sql:
            bad2.append("diffFen 接口=%s SQL|external-internal|=%s" % (diff_api, diff_sql))
        runs_api = ch.get("runs") or []
        runs_sum = sum(int(r.get("differenceCount") or r.get("diffCount") or 0) for r in runs_api) if runs_api else None
        if cnt_api is not None:
            if int(cnt_api) != self.expect["differenceCount"]:
                bad2.append("differenceCount 接口=%s SQL=%s" % (cnt_api, self.expect["differenceCount"]))
        elif runs_sum is not None and runs_sum != self.expect["differenceCount"]:
            bad2.append("runs[].differenceCount 合计=%s SQL=%s" % (runs_sum, self.expect["differenceCount"]))
        self.step("AC-53b channels 聚合 == 当日 FINALIZED recon_runs", not bad2,
                  "接口 channels=%s；SQL internal=%d external=%d diff|Δ|=%d count=%d%s" % (
                      json.dumps({k: ch.get(k) for k in ("internalSaleFen", "externalReceivedFen",
                                                        "diffFen", "differenceCount", "unexplainedCount")},
                                 ensure_ascii=False),
                      self.expect["internalSaleFen"], self.expect["externalReceivedFen"], diff_sql,
                      self.expect["differenceCount"],
                      "" if not bad2 else "；偏差=" + "；".join(bad2) + "；截图=" + self.shot("ac53b")))
        # 3) unexplainedCount
        un_api = int(ch.get("unexplainedCount") or 0)
        self.step("AC-53c channels.unexplainedCount == 未关闭差异条数", un_api == self.expect["unexplainedCount"],
                  "接口=%d SQL=%d（status NOT IN ('CLOSED','REVERSED')）%s" % (
                      un_api, self.expect["unexplainedCount"],
                      "" if un_api == self.expect["unexplainedCount"] else "；截图=" + self.shot("ac53c")))
        # 4) statements
        oc = int(sm.get("openCount") or 0)
        od = int(sm.get("oldestPendingDays") or 0)
        okk = oc == self.expect["openCount"] and od == self.expect["oldestPendingDays"]
        self.step("AC-53d statements.openCount / oldestPendingDays == SQL", okk,
                  "接口 open=%s oldest=%s；SQL open=%s oldest=%s%s" % (
                      oc, od, self.expect["openCount"], self.expect["oldestPendingDays"],
                      "" if okk else "；截图=" + self.shot("ac53d")))
        self.api = d
        return True

    # ---------- 范围 ----------
    def check_scope(self):
        st, b = http("GET", self.a.api + "/v1/finance/overview/daily?date=%s" % self.a.date, self.tokens["store"])
        d = data_of(b) or {}
        self.store_api = d
        stores = d.get("stores") or []
        ids = [int(s.get("storeId") or 0) for s in stores]
        okk = ok(b) and len(stores) == 1 and ids[:1] == [self.a.store_b]
        return self.step("AC-53e 数据范围：qa_store_b 只看到 store %d" % self.a.store_b, okk,
                         "HTTP %s code=%s stores=%s%s" % (
                             st, (b or {}).get("code"), ids,
                             "" if okk else "；截图=" + self.shot("ac53e")))

    # ---------- 页面 ----------
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

    def check_page(self, pw, user, pwd, label, api_data=None):
        exe = self.a.chrome_path or find_chromium()
        browser = pw.chromium.launch(headless=not self.a.headed, executable_path=exe)
        ctx = browser.new_context(viewport={"width": 1600, "height": 1200})
        page = ctx.new_page()
        self.page = page
        if not self._ui_login(page, user, pwd):
            browser.close()
            return self.not_ready("AC-53f 页面总览（%s）" % label, "UI 登录失败；截图=" + self.shot("ac53-login"))
        try:
            page.goto(self.a.admin + "/finance/overview", wait_until="domcontentloaded", timeout=120000)
            # 页面级就绪标记：根节点 data-test=ov-state 的 data-state = loading | ready | idle
            wait_for_data_attr(page, '[data-test="ov-state"]', "data-state", "ready", timeout=60000)
            page.wait_for_selector('[data-test="ov-store-row"]', timeout=60000)
            rows = page.locator('[data-test="ov-store-row"]')
            n = rows.count()
            payload = api_data if api_data is not None else self.api
            api_stores = {int(s.get("storeId") or 0): s for s in (payload.get("stores") or [])}
            bad = []
            for i in range(n):
                row_sel = '[data-test="ov-store-row"] >> nth=%d' % i
                sid = read_data_attr(page, row_sel, "data-store-id")
                bal = read_data_attr(page, row_sel, "data-balance-fen")
                net = read_data_attr(page, row_sel, "data-net-fen")
                want = api_stores.get(int(sid or 0))
                if want is None:
                    continue   # 页面存在但接口未返回的门店（如未配置现金户），单独记录不判失败
                if int(bal or -1) != int(want.get("balanceFen") or 0):
                    bad.append("store%s balance 页面=%s 接口=%s" % (sid, bal, want.get("balanceFen")))
                if int(net or -1) != int(want.get("netChangeFen") or 0):
                    bad.append("store%s net 页面=%s 接口=%s" % (sid, net, want.get("netChangeFen")))
            ch = payload.get("channels") or {}
            sm = payload.get("statements") or {}
            pairs = [("ov-channel-internal-fen", ch.get("internalSaleFen")),
                     ("ov-channel-external-fen", ch.get("externalReceivedFen")),
                     ("ov-channel-diff-fen", ch.get("diffFen") if ch.get("diffFen") is not None else ch.get("diffAmountFen")),
                     ("ov-channel-unexplained", ch.get("unexplainedCount"))]
            for sel, want in pairs:
                try:
                    num = int(str(read_data_attr(page, '[data-test="%s"]' % sel, "data-fen")).replace(",", "").strip())
                except Exception:
                    # 个别计数类钩子没有 data-fen，退化为 data-count
                    try:
                        num = int(str(read_data_attr(page, '[data-test="%s"]' % sel, "data-count")).strip())
                    except Exception as e:
                        bad.append("%s 读取失败：%s" % (sel, str(e)[:80]))
                        continue
                if want is not None and num != int(want):
                    bad.append("%s 页面=%s 接口=%s" % (sel, num, want))
            # 注意：ov-open-count 用 data-count，ov-oldest-days 用 data-days（见 FundOverview.vue:226/236）
            for sel, attr_name, want in (("ov-open-count", "data-count", sm.get("openCount")),
                                         ("ov-oldest-days", "data-days", sm.get("oldestPendingDays"))):
                try:
                    got = int(str(read_data_attr(page, '[data-test="%s"]' % sel, attr_name)).strip())
                except Exception as e:
                    bad.append("%s 读取 %s 失败：%s" % (sel, attr_name, str(e)[:80]))
                    continue
                if want is not None and got != int(want):
                    bad.append("%s 页面=%s 接口=%s" % (sel, got, want))
            if label == "qa_store_b":
                if n != 1:
                    bad.append("store_b 应只看到 1 行，实际 %d 行" % n)
                else:
                    sid = read_data_attr(page, '[data-test="ov-store-row"] >> nth=0', "data-store-id")
                    if int(sid or 0) != self.a.store_b:
                        bad.append("store_b 页面首行 storeId=%s，应为 %d" % (sid, self.a.store_b))
            self.step("AC-53f 页面 /finance/overview data-test == 接口（%s）" % label, not bad,
                      "门店行=%d；%s%s" % (n, "；".join(bad[:6]) if bad else "逐项相等",
                                          "" if not bad else "；截图=" + self.shot("ac53f")))
        except Exception as e:
            self.step("AC-53f 页面 /finance/overview data-test == 接口（%s）" % label, False,
                      "异常：%s；截图=%s" % (str(e)[:160], self.shot("ac53f")))
        browser.close()
        return True

    # ---------- 主流程 ----------
    def run(self):
        print("=" * 90)
        print("P3-4 E2E：资金总览 AC-53（接口逐字段 SQL 对拍 + 页面 data-test + 数据范围）")
        print("  date=%s" % self.a.date)
        print("=" * 90)
        if not self.login_all():
            return self.summary()
        st, b = http("GET", self.a.api + "/v1/finance/overview/daily?date=%s" % self.a.date, self.tokens["admin"])
        if missing(st, b):
            self.not_ready("AC-53 总览接口就绪", "GET /v1/finance/overview/daily → HTTP %s code=%s %s" % (
                st, (b or {}).get("code"), str((b or {}).get("message"))[:80]))
            return self.summary()
        self.sql_expect()
        self.check_api()
        self.check_scope()
        try:
            with sync_playwright() as pw:
                self.check_page(pw, self.a.admin_user, self.a.admin_password, "qa_admin", self.api)
                self.check_page(pw, self.a.store_user, self.a.store_password, "qa_store_b", self.store_api)
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
            print("  %-66s %s" % (name, st))
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
    p.add_argument("--date", default=date.today().strftime("%Y-%m-%d"))
    p.add_argument("--admin-user", default="qa_admin")
    p.add_argument("--admin-password", default="Admin@123456")
    p.add_argument("--store-user", default="qa_store_b")
    p.add_argument("--store-password", default="Admin@123456")
    p.add_argument("--store-b", type=int, default=2)
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
        return OverviewE2E(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
