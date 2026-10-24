#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P3 收口-4：GREATEST 号段并发测试（两线程 × 各 50 次「取号 + 写 fund_flows」）

路径选择：POS 支付（POST /v1/pos/orders/order 下单 → /v1/pos/orders/order/pay 支付），
每笔支付写 1 条 SALE（flow_category=1、direction=1）资金流水，走
FundFlowMapper#nextFlowSequence（键 FLOWyyyyMMdd，**无 GREATEST 兜底**）。
选它的理由：这是唯一「纯取号 + 单条插入」的最短路径，单笔耗时稳定（~150ms），
便于两线程真正并发；缴款单 SL6 每笔要先建班次/生成/提交/收款/夹具到账流水，
单笔 8+ 次调用，并发窗口内会大量时间花在非取号环节，压不到号段竞争。

断言：
  1) count(*) - count(distinct flow_no) = 0（全库无重复）
  2) 当天 FLOW<今天> 序号连续无洞 —— 先排除历史洞白名单 81..88（来源 WORKER_LOG §35.3）
  3) order_sequence[FLOW<今天>] 与当天 MAX 序号的关系（语义：每次调用 +1 后 RETURNING，
     故 sequence_value 应 == 当天 MAX；允许 MAX+1 表示「已取号但该笔插入失败」，
     出现 MAX+1 时必须在报告里点名）
  4) 后端日志（自本次开始偏移起）不得出现 DuplicateKeyException / fund_flows_flow_no_key

收尾：POS 单保留（真实业务数据）；输出账户 20/24 前后余额与解释。
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from datetime import date

DEFAULT_BASE = "http://127.0.0.1:8081/api"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
BASE = DEFAULT_BASE
HISTORICAL_GAPS = set(range(81, 89))   # WORKER_LOG §35.3：历史洞 81–88（不接受新增断言，仅排除）


def call(method, path, token=None, body=None, timeout=40):
    url = path if path.startswith("http") else BASE + path
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


def ok_code(b):
    return isinstance(b, dict) and b.get("code") in (0, 200)


def data_of(b):
    return b.get("data") if isinstance(b, dict) else None


def dump(o, n=180):
    try:
        return json.dumps(o, ensure_ascii=False)[:n]
    except Exception:
        return str(o)[:n]


class Runner:
    def __init__(self, a):
        self.a = a
        self.results = []
        self.token = None
        self.lock = threading.Lock()
        self.per_thread = {}

    def step(self, name, ok, detail):
        self.results.append((name, "PASS" if ok else "FAIL", detail))
        print("  [%s] %s -> %s" % ("PASS" if ok else "FAIL", name, detail))
        return ok

    # ---------- DB ----------
    def psql(self, sql, timeout=90):
        try:
            out = subprocess.run(
                [self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port), "-U", self.a.pg_user,
                 "-d", self.a.pg_db, "-t", "-A", "-F", "|", "-c", sql],
                capture_output=True, text=True, timeout=timeout, encoding="utf-8", errors="replace",
                env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
            return out.returncode, (out.stdout or "").strip(), (out.stderr or "").strip()
        except Exception as e:
            return -1, "", str(e)

    def scalar(self, sql, default="0"):
        rc, out, err = self.psql(sql)
        if rc != 0:
            raise RuntimeError("psql rc=%s %s" % (rc, (err or out)[:200]))
        return default if out == "" else out

    def rows(self, sql):
        rc, out, err = self.psql(sql)
        if rc != 0:
            raise RuntimeError("psql rc=%s %s" % (rc, (err or out)[:200]))
        return [l.split("|") for l in out.splitlines() if l.strip()]

    # ---------- 环境 ----------
    def preflight(self):
        st, b = call("POST", "/v1/auth/login", body={"username": "qa_admin", "password": "Admin@123456"})
        if st == -1:
            self.results.append(("后端 8081 在监听", "NOT_READY", dump(b, 120)))
            print("  [NOT_READY] 后端 8081 未监听")
            return False
        self.token = (data_of(b) or {}).get("token")
        st2, b2 = call("POST", "/v1/auth/login",
                       body={"username": self.a.cashier_user, "password": self.a.cashier_password})
        self.cashier = (data_of(b2) or {}).get("token")
        ok = bool(self.token and self.cashier)
        return self.step("前置：后端在监听 + qa_admin/vcashier01 可登录", ok,
                         "8081 OK；qa_admin token=%s；vcashier01 token=%s" % (
                             "有" if self.token else "无", "有" if self.cashier else "无"))

    def log_offset(self):
        p = self.a.backend_log
        try:
            return os.path.getsize(p) if os.path.exists(p) else 0
        except Exception:
            return 0

    def log_new_text(self, offset):
        p = self.a.backend_log
        if not os.path.exists(p):
            return ""
        with open(p, "rb") as f:
            f.seek(offset)
            raw = f.read()
        for enc in ("utf-8", "utf-16-le", "gbk"):
            try:
                return raw.decode(enc, "ignore")
            except Exception:
                continue
        return raw.decode("utf-8", "ignore")

    # ---------- 并发负载 ----------
    def worker(self, tid, n):
        okc, fail = 0, 0
        errs = []
        for i in range(n):
            yuan = round(10 + (tid * 0.1) + i * 0.01, 2)
            st, body = call("POST", "/v1/pos/orders/order", token=self.cashier, body={
                "tableNumber": (i % 20) + 1,
                "items": [{"id": self.food_code, "name": self.food_name, "quantity": 1, "price": yuan}],
                "totalAmount": yuan, "paymentMethod": "cash", "orderType": "DINE_IN",
                "storeId": str(self.a.store_id), "remark": "P3 并发号段 %d-%d" % (tid, i),
                "idempotencyKey": "p3conc-%d-%d-%d" % (int(time.time() * 1000), tid, i)})
            d = data_of(body) or {}
            oid = d.get("orderId") or d.get("id") or d.get("orderNumber")
            if not ok_code(body) or not oid:
                fail += 1
                if len(errs) < 3:
                    errs.append("下单 %s" % dump(body, 120))
                continue
            st2, b2 = call("POST", "/v1/pos/orders/order/pay", token=self.cashier,
                           body={"orderId": oid, "paymentMethod": "cash", "amount": yuan})
            if ok_code(b2):
                okc += 1
            else:
                fail += 1
                if len(errs) < 3:
                    errs.append("支付 %s" % dump(b2, 120))
        with self.lock:
            self.per_thread[tid] = {"ok": okc, "fail": fail, "errs": errs}

    def run_load(self):
        n = self.a.per_thread
        flows_before = set(r[0] for r in self.rows("SELECT flow_no FROM fund_flows WHERE deleted=0"))
        bal_before = {int(r[0]): (int(r[1]), int(r[2])) for r in self.rows(
            "SELECT account_id, COALESCE(balance,0), COALESCE(version,0) FROM bank_accounts "
            "WHERE account_id IN (%d,%d)" % (self.a.cash_account, self.a.bank_account))}
        off = self.log_offset()
        t0 = time.time()
        threads = [threading.Thread(target=self.worker, args=(tid, n)) for tid in range(1, self.a.threads + 1)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()
        dur = time.time() - t0
        time.sleep(1.5)   # 等异步落库/日志 flush
        new_flows = sorted(set(r[0] for r in self.rows("SELECT flow_no FROM fund_flows WHERE deleted=0")) - flows_before)
        bal_after = {int(r[0]): (int(r[1]), int(r[2])) for r in self.rows(
            "SELECT account_id, COALESCE(balance,0), COALESCE(version,0) FROM bank_accounts "
            "WHERE account_id IN (%d,%d)" % (self.a.cash_account, self.a.bank_account))}
        self.log_text = self.log_new_text(off)
        total_ok = sum(v["ok"] for v in self.per_thread.values())
        total_fail = sum(v["fail"] for v in self.per_thread.values())
        self.step("两线程 × %d 次 并发下单+支付" % n, total_fail == 0,
                  "耗时 %.1fs；成功 %d / 失败 %d；线程明细=%s；新增 fund_flows=%d 条 %s" % (
                      dur, total_ok, total_fail,
                      {k: (v["ok"], v["fail"]) for k, v in sorted(self.per_thread.items())},
                      len(new_flows), (new_flows[0] + ".." + new_flows[-1]) if new_flows else "-"))
        if total_fail:
            self.step("失败样本", True, "; ".join(
                e for v in self.per_thread.values() for e in v["errs"])[:400])
        return bal_before, bal_after, len(new_flows)

    # ---------- 断言 ----------
    def assert_duplicates(self):
        dup = int(self.scalar(
            "SELECT count(*) - count(DISTINCT flow_no) FROM fund_flows WHERE deleted=0"))
        rc, out, _ = self.psql(
            "SELECT flow_no||' ×'||count(*) FROM fund_flows WHERE deleted=0 GROUP BY flow_no "
            "HAVING count(*)>1 LIMIT 10")
        return self.step("断言1 全库 flow_no 无重复", dup == 0,
                         "count(*)-count(distinct)=%d%s" % (dup, ("；重复: " + (out or "")) if dup else ""))

    def today_nums(self):
        today = date.today().strftime("%Y%m%d")
        nums = []
        for r in self.rows("SELECT flow_no FROM fund_flows WHERE deleted=0 AND flow_no LIKE 'FF"
                           + today + "%' ORDER BY flow_no"):
            fn = r[0]
            tail = fn[10:]
            m = re.match(r"^(\d+)$", tail)
            if m:
                nums.append(int(m.group(1)))
        return today, nums

    def assert_gaps(self):
        today, nums = self.today_nums()
        if not nums:
            return self.step("断言2 当天号段连续无洞", False, "当天无 FLOW%s 流水" % today)
        lo, hi = min(nums), max(nums)
        have = set(nums)
        missing_all = [n for n in range(lo, hi + 1) if n not in have]
        missing_new = [n for n in missing_all if n not in HISTORICAL_GAPS]
        detail = ("FLOW%s: min=%d max=%d 条数=%d；缺失合计=%d %s；"
                  "白名单历史洞(81–88, WORKER_LOG §35.3)=%s；**白名单外缺失=%s**" % (
                      today, lo, hi, len(nums), len(missing_all),
                      missing_all[:20], sorted(HISTORICAL_GAPS & set(missing_all)) or "（本次未出现）",
                      missing_new or "无"))
        return self.step("断言2 当天号段连续无洞（排除历史洞 81–88）", len(missing_new) == 0, detail)

    def assert_sequence_value(self):
        today, nums = self.today_nums()
        max_seq = max(nums) if nums else 0
        key = "FLOW" + today
        val = int(self.scalar(
            "SELECT COALESCE(sequence_value,0) FROM order_sequence WHERE sequence_key='%s'" % key, "0") or 0)
        # 语义确认（FundFlowMapper#nextFlowSequence: sequence_value = sequence_value + 1 RETURNING）
        # → 每次取号后 sequence_value 即该次发出的号；故序列值应 == 当天 MAX；
        #   == MAX+1 说明有一次取号后插入失败（号被消耗但没有流水），须点名。
        diff = val - max_seq
        ok = diff in (0,)
        detail = ("order_sequence[%s]=%d，当天 MAX 序号=%d，差=%+d（语义：+1 RETURNING ⇒ 期望差=0；"
                  "差=+1 表示有取号未落库）" % (key, val, max_seq, diff))
        if diff == 1:
            detail += "；⚠ 存在 1 次取号后未落库（需结合日志排查）"
        return self.step("断言3 号段最终值 == 当天 MAX 序号", ok, detail)

    def assert_no_dupkey(self):
        txt = getattr(self, "log_text", "") or ""
        hits = []
        for pat in ("DuplicateKeyException", "fund_flows_flow_no_key"):
            for m in re.finditer(pat, txt):
                s = max(0, m.start() - 120)
                hits.append(txt[s:m.end() + 120].replace("\n", " ⏎ ")[:260])
                if len(hits) >= 5:
                    break
        return self.step("断言4 并发期间后端日志无 DuplicateKeyException / fund_flows_flow_no_key",
                         len(hits) == 0,
                         "日志=%s 本次新增 %d 字节；命中 %d 条%s" % (
                             self.a.backend_log, len(txt.encode('utf-8', 'ignore')), len(hits),
                             ("；片段: " + " || ".join(hits[:3])) if hits else ""))

    # ---------- 主流程 ----------
    def run(self):
        print("=" * 84)
        print("P3 收口-4：GREATEST 号段并发测试（%d 线程 × 各 %d 次 POS 支付）" % (
            self.a.threads, self.a.per_thread))
        print("=" * 84)
        if not self.preflight():
            return self.summary()
        if self.a.food_code:
            row = self.rows("SELECT food_code, food_name, stock FROM foods WHERE food_code='%s'"
                            % self.a.food_code)
            self.food_code, self.food_name = (row[0][0], row[0][1]) if row else (self.a.food_code, "指定菜品")
        else:
            # 取库存最大的在售菜品（避免「库存不足」污染并发结果）
            row = self.rows("SELECT food_code, food_name FROM foods WHERE deleted=0 AND status=1 "
                            "ORDER BY stock DESC LIMIT 1")
            self.food_code, self.food_name = (row[0][0], row[0][1]) if row else ("FD260926001", "QA-F6")
        self.step("菜品", True, "%s / %s" % (self.food_code, self.food_name))

        bal_b, bal_a, nflows = self.run_load()
        self.assert_duplicates()
        self.assert_gaps()
        self.assert_sequence_value()
        self.assert_no_dupkey()

        self.step("收尾：账户余额前后对比（POS 单保留）", True, "; ".join(
            "acct%d %d→%d（Δ%+d, ver %d→%d）" % (
                aid, bal_b.get(aid, (0, 0))[0], bal_a.get(aid, (0, 0))[0],
                bal_a.get(aid, (0, 0))[0] - bal_b.get(aid, (0, 0))[0],
                bal_b.get(aid, (0, 0))[1], bal_a.get(aid, (0, 0))[1])
            for aid in (self.a.cash_account, self.a.bank_account)))
        return self.summary()

    def summary(self):
        fails = [r for r in self.results if r[1] == "FAIL"]
        nr = [r for r in self.results if r[1] == "NOT_READY"]
        print("\n" + "=" * 84)
        print("结果: %s  PASS=%d FAIL=%d%s" % (
            "全绿" if not fails and not nr else ("未就绪" if not fails else "存在失败"),
            len([r for r in self.results if r[1] == "PASS"]), len(fails),
            ("  NOT_READY=%d" % len(nr)) if nr else ""))
        for name, state, detail in self.results:
            print("  %-56s %s" % (name, state))
        if fails or nr:
            print("-" * 84)
            for name, state, detail in fails + nr:
                print("  * [%s] %s\n    %s" % (state, name, detail))
        print("=" * 84)
        if fails:
            return 1
        return 2 if nr else 0


def main():
    global BASE
    p = argparse.ArgumentParser()
    p.add_argument("--base", default=DEFAULT_BASE)
    p.add_argument("--threads", type=int, default=2)
    p.add_argument("--per-thread", type=int, default=50)
    p.add_argument("--store-id", type=int, default=1)
    p.add_argument("--cash-account", type=int, default=20)
    p.add_argument("--bank-account", type=int, default=24)
    p.add_argument("--food-code", default="", help="指定菜品（默认取库存最大在售菜品）")
    p.add_argument("--cashier-user", default="vcashier01")
    p.add_argument("--cashier-password", default="123456")
    p.add_argument("--backend-log", default=r"P:\my-new-project\backend\logs\food-traceability.log")
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    BASE = a.base
    try:
        return Runner(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
