#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
BL-5（P3-5 项 2）取号并发 + 烧号账对账测试

路径：POST /v1/finance/fund-flows（FundFlowServiceImpl#create：取号 → 余额原子更新 → insert，同一事务）
并发：默认 10 线程 × 每线程 20 次（可调 --threads / --per-thread）

断言：
  A1 fund_flows 全表无重复 flow_no（count(*) - count(distinct flow_no) = 0）
  A2 本次落库行数 = 成功次数（fund_flows 增量 == 2xx 响应数）
  A3 烧号账增量 = 尝试次数 − 落库次数（每个失败都必须有一条账，不许有洞没账）
  A4 无 5xx；账户余额 = 起始 + 成功笔数 × 单笔金额，且不为负

用法：
  python scripts/qa/seq_concurrency_check_p35.py --base http://127.0.0.1:8084/api \
         --threads 10 --per-thread 20 --account 24 --amount 1
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request

DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"


def call(base, method, path, token=None, body=None, timeout=60, client_ip=None):
    data = json.dumps(body, ensure_ascii=False).encode("utf-8") if body is not None else None
    h = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        h["Authorization"] = "Bearer " + token
    if client_ip:
        # RateLimitFilter 按 IP+URI 独立计数（100 次/分钟）：每个模拟客户端用独立 IP，
        # 避免压测被限流拦在业务之外（拦掉的请求没有取号，不该计入烧号账口径）
        h["X-Real-IP"] = client_ip
    req = urllib.request.Request(base + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read().decode("utf-8", "replace"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"_raw": raw[:200]}
    except Exception as e:
        return -1, {"error": str(e)}


class Ctx:
    def __init__(self, a):
        self.a = a
        self.lock = threading.Lock()
        self.ok = 0
        self.fail = 0
        self.http5xx = 0
        self.rejected = 0      # 被限流拦下（没取号，不计入烧号口径）
        self.reached = 0       # 真正进入业务的请求数
        self.codes = {}
        self.errors = []

    def psql(self, sql):
        out = subprocess.run(
            [self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port), "-U", self.a.pg_user,
             "-d", self.a.pg_db, "-t", "-A", "-c", sql],
            capture_output=True, text=True, timeout=90, encoding="utf-8", errors="replace",
            env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
        if out.returncode != 0:
            raise RuntimeError("psql: " + (out.stderr or "")[:200])
        return (out.stdout or "").strip()

    def num(self, sql):
        v = self.psql(sql)
        return int(v) if v else 0


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--base", default="http://127.0.0.1:8084/api")
    p.add_argument("--threads", type=int, default=10)
    p.add_argument("--per-thread", type=int, default=20)
    p.add_argument("--account", type=int, default=24)
    p.add_argument("--amount", type=int, default=1)
    p.add_argument("--business-date", default=__import__("datetime").date.today().isoformat())
    # RateLimitFilter 是「IP+URI 100 次/分钟」的内存限流（X-Real-IP 不作为计数键），
    # 压测必须自己控速，否则一半请求会在业务之外被 429 拦掉。
    p.add_argument("--max-per-minute", type=int, default=90)
    p.add_argument("--user", default="qa_admin")
    p.add_argument("--password", default="Admin@123456")
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="127.0.0.1")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()

    ctx = Ctx(a)
    st, b = call(a.base, "POST", "/v1/auth/login", body={"username": a.user, "password": a.password})
    token = ((b.get("data") or {}) if isinstance(b, dict) else {}).get("token")
    print("login: HTTP %s code=%s token=%s" % (st, (b or {}).get("code"), bool(token)))
    if not token:
        print("登录失败，无法继续"); return 2

    before_flows = ctx.num("SELECT count(*) FROM fund_flows")
    before_burn = ctx.num("SELECT count(*) FROM fund_flow_no_burn_ledger")
    before_bal = ctx.num("SELECT balance FROM bank_accounts WHERE account_id=%d" % a.account)
    seq_key = "FLOW" + ctx.psql("SELECT to_char(current_date,'YYYYMMDD')")
    before_seq = ctx.num("SELECT COALESCE(max(sequence_value),0) FROM order_sequence WHERE sequence_key='%s'" % seq_key)
    print("起始：fund_flows=%d burn=%d 余额=%d 号段[%s]=%d" % (before_flows, before_burn, before_bal, seq_key, before_seq))

    attempts = a.threads * a.per_thread
    # 全局限速：把「最近 60 秒内的请求数」压在 max_per_minute 之下
    gate = threading.Lock()
    sent_window = []
    min_interval = 60.0 / max(1, a.max_per_minute)

    def pace():
        while True:
            with gate:
                now = time.time()
                while sent_window and now - sent_window[0] > 60.0:
                    sent_window.pop(0)
                if len(sent_window) < a.max_per_minute:
                    sent_window.append(now)
                    return
                wait = 60.0 - (now - sent_window[0]) + 0.05
            time.sleep(min(2.0, max(0.05, wait)))

    t0 = time.time()

    def worker(tid):
        client_ip = "10.10.%d.%d" % (tid // 250, tid % 250 + 1)
        for i in range(a.per_thread):
            pace()
            payload = {"accountId": a.account, "amount": a.amount, "flowDirection": 1,
                       "flowCategory": 5, "businessDate": a.business_date,
                       "remark": "BL-5 并发取号 t%d-%d" % (tid, i)}
            st, b = call(a.base, "POST", "/v1/finance/fund-flows", token, payload, client_ip=client_ip)
            retry = 0
            while st == 429 and retry < 5:          # 限流是环境行为，不是业务失败：退避重试
                time.sleep(0.2 * (retry + 1))
                st, b = call(a.base, "POST", "/v1/finance/fund-flows", token, payload, client_ip=client_ip)
                retry += 1
            with ctx.lock:
                ctx.codes[st] = ctx.codes.get(st, 0) + 1
                if st == 429:
                    ctx.rejected += 1
                    continue
                ctx.reached += 1
                if 200 <= st < 300 and (b.get("code") in (0, 200)):
                    ctx.ok += 1
                else:
                    ctx.fail += 1
                    if st >= 500 or st == -1:
                        ctx.http5xx += 1
                    if len(ctx.errors) < 5:
                        ctx.errors.append((st, str(b.get("message") or b)[:160]))

    threads = [threading.Thread(target=worker, args=(t,)) for t in range(a.threads)]
    for t in threads: t.start()
    for t in threads: t.join()
    cost = time.time() - t0

    after_flows = ctx.num("SELECT count(*) FROM fund_flows")
    after_burn = ctx.num("SELECT count(*) FROM fund_flow_no_burn_ledger")
    after_bal = ctx.num("SELECT balance FROM bank_accounts WHERE account_id=%d" % a.account)
    after_seq = ctx.num("SELECT COALESCE(max(sequence_value),0) FROM order_sequence WHERE sequence_key='%s'" % seq_key)
    dup = ctx.num("SELECT count(*) - count(DISTINCT flow_no) FROM fund_flows")
    neg = ctx.num("SELECT count(*) FROM bank_accounts WHERE balance < 0")
    burn_orphan = ctx.num("SELECT count(*) FROM fund_flow_no_burn_ledger b JOIN fund_flows f ON f.flow_no = b.flow_no")

    print("\n=== 并发执行 ===")
    print("线程数=%d 每线程=%d 尝试=%d 耗时=%.2fs 吞吐=%.1f/s" % (a.threads, a.per_thread, attempts, cost, attempts / cost))
    print("HTTP 分布:", json.dumps(ctx.codes, ensure_ascii=False))
    print("进入业务=%d（其中被限流拦下=%d）成功=%d 失败=%d 5xx/连接失败=%d"
          % (ctx.reached, ctx.rejected, ctx.ok, ctx.fail, ctx.http5xx))
    if ctx.errors:
        print("失败样本:", ctx.errors)
    print("\n=== 结果 ===")
    print("fund_flows: %d -> %d (增量 %d)" % (before_flows, after_flows, after_flows - before_flows))
    print("burn_ledger: %d -> %d (增量 %d)" % (before_burn, after_burn, after_burn - before_burn))
    print("余额: %d -> %d (增量 %d)" % (before_bal, after_bal, after_bal - before_bal))
    print("号段[%s]: %d -> %d (增量 %d)" % (seq_key, before_seq, after_seq, after_seq - before_seq))

    results = []
    def check(name, ok, detail):
        results.append((name, ok, detail))
        print("  [%s] %s -> %s" % ("PASS" if ok else "FAIL", name, detail))

    print("\n=== 断言 ===")
    check("A1 全库 flow_no 无重复", dup == 0, "重复数=%d（期望 0）" % dup)
    check("A2 落库行数 = 成功次数", (after_flows - before_flows) == ctx.ok,
          "增量=%d 成功=%d" % (after_flows - before_flows, ctx.ok))
    expected_burn = ctx.reached - ctx.ok
    check("A3 烧号账增量 = 进入业务数 − 落库数", (after_burn - before_burn) == expected_burn,
          "burn 增量=%d 期望=%d（进入业务 %d（总尝试 %d，限流 %d）− 成功 %d）"
          % (after_burn - before_burn, expected_burn, ctx.reached, attempts, ctx.rejected, ctx.ok))
    check("A4 无 5xx/连接失败 且无负余额", ctx.http5xx == 0 and neg == 0,
          "5xx=%d 负余额账户=%d" % (ctx.http5xx, neg))
    bad_reason = ctx.num("SELECT count(*) FROM fund_flow_no_burn_ledger WHERE reason IS NULL OR btrim(reason)=''")
    check("A5 每条烧号账都有 reason", bad_reason == 0, "空 reason 行数=%d（期望 0）" % bad_reason)
    check("A6 余额 = 起始 + 成功笔数×金额", after_bal == before_bal + ctx.ok * a.amount,
          "%d vs %d" % (after_bal, before_bal + ctx.ok * a.amount))
    # 口径说明：取号与业务同事务 → 失败回滚会把号段自增一起回滚，号可被后续成功请求重用，
    # 因此「号段增量 == 落库数」是预期行为，烧号账记录的是「曾经消耗」的审计痕迹。
    check("A7 号段增量 = 落库数（同事务取号，回滚可重用）", (after_seq - before_seq) == (after_flows - before_flows),
          "号段增量=%d 落库增量=%d" % (after_seq - before_seq, after_flows - before_flows))
    print("  说明：烧号账中 %d 行，其中 %d 行的号已被后续成功请求重用（同事务取号的预期结果，见 A7）"
          % (after_burn, burn_orphan))

    fails = [r for r in results if not r[1]]
    print("\n汇总: %d 条断言，%d PASS / %d FAIL" % (len(results), len(results) - len(fails), len(fails)))
    return 0 if not fails else 1


if __name__ == "__main__":
    sys.exit(main())
