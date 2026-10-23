#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
POS 交班聚合独立复核（可复跑）

校验三件事：
  1) GET  /api/v1/pos/auth/shift/summary?terminalId=...
     orderStats.total_orders / total_amount 必须与「canonical orders(payment_status=2)」日汇总一致
  2) payment_breakdown 各支付方式合计 == total_amount（容差 0.01）
  3) POST /api/v1/pos/auth/shift/end {terminalId, remark}
     返回 shiftInfo.status=completed、totalAmount 与 summary 一致；
     pos_shifts 新增/更新了一行（无活跃班次时按今日首笔支付时间补建）

鉴权：dev jwt.secret 自签 admin token（算法与 scripts/e2e_wip_pages.py 的 mint_jwt 相同）。
用法：python scripts/e2e_shift_summary.py
      python scripts/e2e_shift_summary.py --terminal-id pos-test-001 --pg-password 123456
"""
import argparse
import base64
import hashlib
import hmac
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from decimal import Decimal

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_JWT_SECRET = (
    "dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security"
)
DEFAULT_ISSUER = "food-traceability-system"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
BASE = DEFAULT_BASE
TOL = Decimal("0.01")


def b64u(b):
    return base64.urlsafe_b64encode(b).rstrip(b"=").decode("ascii")


def mint_jwt(user_id, username, role_codes, permissions, secret=DEFAULT_JWT_SECRET, issuer=DEFAULT_ISSUER):
    """与 scripts/e2e_wip_pages.py 完全一致的 HS512 自签实现。"""
    now = int(time.time())
    header = {"alg": "HS512", "typ": "JWT"}
    payload = {
        "userId": str(user_id), "username": username, "roles": role_codes,
        "permissions": permissions, "mfaEnabled": False, "mfaVerified": True,
        "tokenId": "shiftcheck-" + str(now), "sub": username, "iss": issuer,
        "iat": now, "exp": now + 3600, "jti": "shiftcheck-" + str(now),
    }
    signing_input = b64u(json.dumps(header, separators=(",", ":")).encode()) + "." + \
                    b64u(json.dumps(payload, separators=(",", ":")).encode())
    sig = hmac.new(secret.encode("utf-8"), signing_input.encode("ascii"), hashlib.sha512).digest()
    return signing_input + "." + b64u(sig)


def call(method, path, token=None, body=None, timeout=30):
    url = path if path.startswith("http") else BASE + path
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


def ok_code(body):
    return isinstance(body, dict) and body.get("code") in (0, 200)


def dec(v, default="0"):
    try:
        if v is None or v == "":
            return Decimal(default)
        return Decimal(str(v))
    except Exception:
        return Decimal(default)


class Runner:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.token = mint_jwt(
            "2080170081205395463", "shiftcheck-admin", ["admin"],
            ["*", "pos:shift:manage", "pos:shift:view", "order:view"],
        )
        self.summary_total = None
        self.summary_orders = None

    def step(self, name, ok, detail):
        state = "PASS" if ok else "FAIL"
        self.results.append((name, state, detail))
        print("  [%s] %s -> %s" % (state, name, detail))
        return ok

    def psql(self, sql):
        try:
            out = subprocess.run(
                [self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port), "-U", self.a.pg_user,
                 "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                capture_output=True, text=True, timeout=25,
                env=dict(os.environ, PGPASSWORD=self.a.pg_password))
            return out.returncode, (out.stdout or "").strip(), (out.stderr or "").strip()
        except Exception as e:
            return -1, "", str(e)

    def db_expected(self):
        """canonical orders 日汇总（唯一资金口径：payment_status=2，金额分→元）"""
        sql = ("SELECT COUNT(*), COALESCE(SUM(COALESCE(final_amount,total_amount)),0)/100.0 "
               "FROM orders WHERE payment_status=2 AND deleted=0 AND create_time::date=CURRENT_DATE")
        rc, out, err = self.psql(sql)
        if rc != 0 or not out:
            return None, None, "psql rc=%s %s" % (rc, (err or out)[:160])
        parts = out.split("|")
        return int(parts[0] or 0), dec(parts[1] if len(parts) > 1 else "0"), ""

    def db_breakdown(self):
        sql = ("SELECT r.payment_method, COALESCE(SUM(r.payment_amount),0)/100.0 "
               "FROM order_payment_records r JOIN orders o ON o.order_id=r.order_id "
               "WHERE o.create_time::date=CURRENT_DATE AND o.payment_status=2 AND o.deleted=0 AND r.deleted=0 "
               "GROUP BY r.payment_method ORDER BY r.payment_method")
        rc, out, err = self.psql(sql)
        if rc != 0:
            return None, "psql rc=%s %s" % (rc, err[:160])
        rows = {}
        for line in out.splitlines():
            if not line.strip():
                continue
            m, v = (line.split("|") + ["0", "0"])[:2]
            rows[int(m)] = dec(v)
        return rows, ""

    def shift_rows(self):
        sql = ("SELECT COUNT(*), "
               "COALESCE(MAX(total_orders) FILTER (WHERE status='completed'), -1), "
               "COALESCE(MAX(total_amount) FILTER (WHERE status='completed'), -1), "
               "COALESCE(MAX(status), '') "
               "FROM pos_shifts WHERE terminal_id = '%s'" % self.a.terminal_id.replace("'", "''"))
        rc, out, err = self.psql(sql)
        if rc != 0 or not out:
            return None, "psql rc=%s %s" % (rc, (err or out)[:160])
        parts = out.split("|")
        return {"count": int(parts[0] or 0),
                "completed_orders": int(parts[1] or -1),
                "completed_amount": dec(parts[2] if len(parts) > 2 else "-1"),
                "any_status": parts[3] if len(parts) > 3 else ""}, ""

    # ---------- 1. summary 与库内日汇总一致 ----------
    def check_summary(self):
        st, body = call("GET", "/v1/pos/auth/shift/summary?terminalId=%s" %
                        urllib.parse.quote(self.a.terminal_id), token=self.token)
        data = (body.get("data") or {}) if isinstance(body, dict) else {}
        stats = (data.get("orderStats") or {})
        api_orders = stats.get("total_orders")
        api_amount = dec(stats.get("total_amount"))
        self.summary_total = api_amount
        self.summary_orders = api_orders

        exp_orders, exp_amount, err = self.db_expected()
        if exp_orders is None:
            return self.step("shift/summary 与库内日汇总一致", False, "无法取数: %s" % err)

        ok_http = st == 200 and ok_code(body)
        ok_orders = api_orders is not None and int(api_orders) == exp_orders
        ok_amount = abs(api_amount - exp_amount) <= TOL
        detail = ("HTTP %s api(orders=%s amount=%s) db(orders=%s amount=%s) start=%s" % (
            st, api_orders, api_amount, exp_orders, exp_amount, data.get("summaryStartTime")))
        return self.step("shift/summary 与库内日汇总一致", ok_http and ok_orders and ok_amount, detail)

    # ---------- 2. payment_breakdown 合计 == total_amount ----------
    def check_breakdown(self):
        st, body = call("GET", "/v1/pos/auth/shift/summary?terminalId=%s" %
                        urllib.parse.quote(self.a.terminal_id), token=self.token)
        data = (body.get("data") or {}) if isinstance(body, dict) else {}
        stats = (data.get("orderStats") or {})
        bd = (stats.get("payment_breakdown") or {})
        api_total = dec(stats.get("total_amount"))
        bd_sum = sum((dec(v) for v in bd.values()), Decimal("0"))

        db_rows, err = self.db_breakdown()
        if db_rows is None:
            return self.step("payment_breakdown 合计 == total_amount", False, "无法取支付方式明细: %s" % err)
        db_sum = sum(db_rows.values(), Decimal("0"))

        mapping = {1: "cash", 2: "wechat", 3: "alipay", 4: "card", 5: "balance"}
        per_method_ok = True
        diffs = []
        for method, yuan in db_rows.items():
            key = mapping.get(method, "other")
            api_v = dec(bd.get(key))
            if abs(api_v - yuan) > TOL:
                per_method_ok = False
                diffs.append("%s(api=%s db=%s)" % (key, api_v, yuan))

        ok_total = abs(bd_sum - api_total) <= TOL
        ok_db = abs(db_sum - api_total) <= TOL
        detail = "breakdown=%s api_total=%s bd_sum=%s db_sum=%s %s" % (
            json.dumps({k: str(v) for k, v in bd.items()}, ensure_ascii=False),
            api_total, bd_sum, db_sum, ("; 分项差异: " + ", ".join(diffs)) if diffs else "")
        return self.step("payment_breakdown 合计 == total_amount", ok_total and ok_db and per_method_ok, detail)

    # ---------- 3. endShift：completed + 金额一致 + pos_shifts 留痕 ----------
    def check_end_shift(self):
        before, err = self.shift_rows()
        if before is None:
            return self.step("shift/end 完成交班并落库", False, "无法读 pos_shifts: %s" % err)

        st, body = call("POST", "/v1/pos/auth/shift/end", token=self.token,
                        body={"terminalId": self.a.terminal_id, "remark": "自检"})
        data = (body.get("data") or {}) if isinstance(body, dict) else {}
        info = data.get("shiftInfo") or {}
        stats = data.get("orderStats") or {}
        api_total = dec(stats.get("total_amount"))
        api_orders = stats.get("total_orders")

        ok_http = st == 200 and ok_code(body)
        ok_status = str(info.get("status")) == "completed"

        # 交班金额必须与 summary 一致；若期间恰好有新单支付（并发场景），
        # 退一步与「同一时刻的库内日汇总」比对，避免把真实并发误判为口径错误。
        same_as_summary = self.summary_total is not None and abs(api_total - self.summary_total) <= TOL
        db_orders_now, db_amount_now, _ = self.db_expected()
        same_as_db = db_amount_now is not None and abs(api_total - db_amount_now) <= TOL
        ok_amount = same_as_summary or same_as_db
        ok_detail = ("HTTP %s status=%s totalAmount=%s totalOrders=%s summaryTotal=%s dbNow=%s" % (
            st, info.get("status"), info.get("totalAmount"), info.get("totalOrders"),
            self.summary_total, db_amount_now))
        return self.step("shift/end 返回 completed 且金额与 summary 一致", ok_http and ok_status and ok_amount, ok_detail) \
            and self.check_shift_row(before, api_orders, api_total)

    def check_shift_row(self, before, api_orders, api_total):
        after, err = self.shift_rows()
        if after is None:
            return self.step("pos_shifts 留痕（补建/更新生效）", False, "无法读 pos_shifts: %s" % err)
        grew = after["count"] > before["count"]
        ok_row = (after["count"] >= 1
                  and after["completed_orders"] == int(api_orders if api_orders is not None else -1)
                  and abs(after["completed_amount"] - api_total) <= TOL)
        detail = ("rows %d→%d completed(orders=%s amount=%s) api(orders=%s amount=%s)" % (
            before["count"], after["count"], after["completed_orders"], after["completed_amount"],
            api_orders, api_total))
        return self.step("pos_shifts 留痕（补建/更新生效）", ok_row and (grew or before["count"] >= 1), detail)

    # ---------- 4. 交班后 summary 口径不回退 ----------
    def check_after_end(self):
        st, body = call("GET", "/v1/pos/auth/shift/summary?terminalId=%s" %
                        urllib.parse.quote(self.a.terminal_id), token=self.token)
        data = (body.get("data") or {}) if isinstance(body, dict) else {}
        stats = (data.get("orderStats") or {})
        api_total = dec(stats.get("total_amount"))
        ok = st == 200 and ok_code(body) and abs(api_total - (self.summary_total or Decimal("0"))) <= TOL
        return self.step("交班后 summary 金额不回退", ok,
                         "HTTP %s active=%s amount=%s (before=%s)" % (
                             st, data.get("shiftActive"), api_total, self.summary_total))

    def run(self):
        print("=" * 78)
        print("POS 交班聚合独立复核   base=%s terminal=%s" % (BASE, self.a.terminal_id))
        print("=" * 78)
        checks = (self.check_summary, self.check_breakdown, self.check_end_shift, self.check_after_end)
        for fn in checks:
            if not fn():
                pass  # 继续跑完全部断言，便于一次看到所有偏差
        fails = [r for r in self.results if r[1] == "FAIL"]
        print("\n" + "=" * 78)
        print("结果: %s    PASS=%d FAIL=%d" % (
            "全绿" if not fails else "存在失败", len(self.results) - len(fails), len(fails)))
        for name, state, detail in self.results:
            print("  %-42s %s" % (name, state))
        print("=" * 78)
        return 0 if not fails else 1


def main():
    global BASE
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--terminal-id", default="pos-test-001")
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    BASE = a.base
    return Runner(a).run()


if __name__ == "__main__":
    sys.exit(main())
