#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""限流门禁（Lead 2026-10-30 P0，护栏 2）。

背景（真实故障形态）：
  Filter 一直生效，但阈值写死 100 次/60 秒 → 8 次登录根本到不了阈值；
  Interceptor 注册了但 excludePathPatterns("/v1/auth/**") → 登录永远不受它管；
  Aspect 被 TODO 短路 → 注解 5/60s 从未执行。
  → 三个「5 次上限」全是假的，真实上限 100 次/分钟。

因此本 gate 的判据是「刚好超过阈值」：连打 6 次（阈值 5），第 6 次必须 429。
只验证「打几次没被拦」毫无意义 —— 100 次/分钟的阈值在手工测试里几乎不可能触发。

用法：python rate_limit_gate.py [--skip-recovery] [--self-test]
退出码：0 PASS / 2 BLOCK / 3 环境不可用
"""
import json
import os
import subprocess
import sys
import time

BASE = os.environ.get("QA_API_BASE", "http://127.0.0.1:8081/api")
ENC = {"encoding": "utf-8", "errors": "replace"}
LIMIT = 5
ATTEMPTS = LIMIT + 1
WINDOW_MS = 60000


def call(path, body):
    r = subprocess.run(["curl.exe", "-s", "-w", "\n__HTTP__%{http_code}",
                        "-X", "POST", BASE + path, "-H", "Content-Type: application/json",
                        "-d", json.dumps(body)], capture_output=True, text=True, **ENC)
    out = r.stdout or ""
    b, _, c = out.rpartition("\n__HTTP__")
    return b.strip(), c.strip()


def judge_threshold(http, body, expect_http="429", expect_code=429):
    if http != expect_http:
        return False, "期望 HTTP %s，实际 %s" % (expect_http, http)
    try:
        j = json.loads(body)
    except Exception:
        return False, "body 不是 JSON：%s" % body[:60]
    if j.get("code") != expect_code:
        return False, "body.code 期望 %s，实际 %s" % (expect_code, j.get("code"))
    if not j.get("message"):
        return False, "body 缺 message（前端会看到空白）"
    return True, "HTTP 429 + body.code=429 + message"


def judge_sequence(responses, limit=LIMIT):
    if len(responses) < limit + 1:
        return False, "采样不足（需要 %d 次）" % (limit + 1)
    if all(http == "200" for http, _ in responses):
        return False, "**阈值过宽**：%d 次全部 200（正是今天「限流形同虚设」的故障形态）" % len(responses)
    http, body = responses[limit]
    return judge_threshold(http, body)


def self_test():
    cases = [
        ("6 次全 200（阈值 100，真实故障形态）", [("200", '{"code":401}')] * 6, False),
        ("第 6 次 429 + code=429（正确）", [("200", "{}")] * 5 + [("429", '{"code":429,"message":"请求过于频繁"}')], True),
        ("第 6 次 429 但 code=42900（未统一）", [("200", "{}")] * 5 + [("429", '{"code":42900,"message":"x"}')], False),
        ("第 6 次 429 但无 message", [("200", "{}")] * 5 + [("429", '{"code":429}')], False),
    ]
    ok = 0
    for name, seq, expect in cases:
        got, reason = judge_sequence(seq)
        flag = "PASS" if got == expect else "FAIL"
        if got == expect:
            ok += 1
        print("[%s] self-test %-38s expect=%-5s got=%-5s %s" % (flag, name, expect, got, reason))
    print("self-test: %d/%d PASS" % (ok, len(cases)))
    return 0 if ok == len(cases) else 2


def probe(path, payload_fn):
    res = []
    for i in range(ATTEMPTS):
        b, c = call(path, payload_fn(i))
        res.append((c, b))
    return res


def main():
    if "--self-test" in sys.argv:
        return self_test()
    tag = "rlgate-%d" % int(time.time())
    fails = 0
    res = probe("/v1/auth/login", lambda i: {"username": tag, "password": "wrong"})
    ok, reason = judge_sequence(res)
    print("[%s] 管理端 /v1/auth/login 第%d次 %s" % ("PASS" if ok else "FAIL", ATTEMPTS, reason))
    if not ok:
        fails += 1
    res2 = probe("/v1/pos/auth/login", lambda i: {"employeeId": tag, "password": "wrong"})
    ok2, reason2 = judge_sequence(res2)
    print("[%s] POS   /v1/pos/auth/login 第%d次 %s" % ("PASS" if ok2 else "FAIL", ATTEMPTS, reason2))
    if not ok2:
        fails += 1
    if "--with-recovery" not in sys.argv:
        # ②（Lead 2026-10-30）：默认不跑 62s 等待（会让 preflight 变慢），
        # 但**必须打一行说明**，不许静默跳过。
        print("[INFO] 窗口恢复验证未执行（--with-recovery 启用）")
    else:
        wait_s = WINDOW_MS / 1000.0 + 2
        print("[INFO] 等待 %.0fs 验证窗口恢复 …" % wait_s)
        time.sleep(wait_s)
        b, c = call("/v1/auth/login", {"username": tag, "password": "wrong"})
        if c == "429":
            fails += 1
            print("[FAIL] 窗口恢复：等待 %.0fs 后仍 429（用户会被锁死）" % wait_s)
        else:
            print("[PASS] 窗口恢复：等待后 HTTP %s（不再 429）" % c)
    print("汇总：%d FAIL" % fails)
    return 0 if fails == 0 else 2


if __name__ == "__main__":
    sys.exit(main())