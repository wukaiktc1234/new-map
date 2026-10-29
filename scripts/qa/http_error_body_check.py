#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""HTTP 错误响应体门禁（Lead 2026-10-29）。

背景：前端实测「HTTP 400，响应体为空」→ 前端只能显示通用「失败，请重试」，用户永远不知道原因。
根因：错误由框架层抛出（未走我们的 handler），或 controller 返回空 ResponseEntity。

判据（写死）：
  HTTP >= 400 时，body **必须非空**，且必须能解析为 JSON、**必须含 code 字段**。
  任一不满足 → EXIT=2（BLOCK）。

用法：
  python scripts/qa/http_error_body_check.py            # 跑真实请求
  python scripts/qa/http_error_body_check.py --self-test # 自检（不连后端）

退出码：0 全 PASS / 2 有违规 / 3 环境不可用（后端未启动）
"""
import json
import os
import subprocess
import time
import sys

BASE = os.environ.get("DSH_API_BASE", "http://127.0.0.1:8081/api")

# ⚠ 永久 gate 夹具（禁止任何人清理，见 docs/qa/fixtures-registry.md）
#   [QA-GATE2:409] O1793284571539 —— 用于触发 409（**预置 exception_ops，零资金流水**）
#   [QA-GATE2:422] O1793284571541 —— 用于触发 422（金额不符，**不写任何数据**）
#   ⚠ 该夹具必须保持「未支付 + 无 exception_ops」；**禁止用正确金额调用 resolve**，
#     否则它会被 consume，422 分支再也走不到（gate 会如实报「未走到预期分支」）。
FIXTURE_409 = "O1793284571539"
FIXTURE_422 = "O1793284571541"
ENC = {"encoding": "utf-8", "errors": "replace"}
FAIL_EXIT = 2
ENV_EXIT = 3


def curl(args):
    r = subprocess.run(["curl.exe", "-s", "-w", "\n__HTTP__%{http_code}"] + args,
                       capture_output=True, text=True, **ENC)
    out = r.stdout or ""
    body, _, code = out.rpartition("\n__HTTP__")
    return body.strip(), code.strip()


def check(body, http, expect_code=None):
    """返回 (ok, reason)。纯函数，供 self-test 复用。"""
    if http in ("", "000"):
        return False, "连接失败（后端未启动？）"
    try:
        n = int(http)
    except ValueError:
        return False, "HTTP 码无法解析：%r" % http
    if n < 400:
        if expect_code is not None:
            return False, "**未走到预期错误分支**：期望 code=%s，实际 HTTP %d（成功）" % (expect_code, n)
        return True, "HTTP<400 不参与判定（未声明预期分支）"
    if not body:
        return False, "HTTP %d 但 **body 为空**" % n
    try:
        obj = json.loads(body)
    except json.JSONDecodeError:
        return False, "HTTP %d body 不是 JSON：%s" % (n, body[:60])
    if not isinstance(obj, dict) or "code" not in obj:
        return False, "HTTP %d body 缺 code 字段：%s" % (n, body[:60])
    # ③（Lead 2026-10-29）：409/422 必须带 message；replayed 若存在必须是布尔
    if n in (409, 422) and not obj.get("message"):
        return False, "HTTP %d body 缺 message 字段" % n
    if "replayed" in obj and not isinstance(obj.get("replayed"), bool):
        return False, "HTTP %d replayed 不是布尔：%r" % (n, obj.get("replayed"))
    # ④（Lead 2026-10-29）**分支证明**：「测试通过」≠「测试走到了它想测的分支」。
    #   反例：409 用例没带 token → 实际 401，只断言 HTTP>=400 就会「假通过」。
    #   因此必须断言 body.code == 预期分支码。
    if expect_code is not None and obj.get("code") != expect_code:
        return False, "**未走到预期分支**：期望 body.code=%s，实际 code=%s（HTTP %s）" % (
            expect_code, obj.get("code"), n)
    return True, "HTTP %d body 正常（code=%s，命中预期分支）" % (n, obj.get("code"))


def self_test():
    cases = [
        # (名称, body, http, 期望OK, 期望分支码)——第 5 项证明「分支被执行」
        ("空 body 的 400", "", "400", False, None),
        ("走到了错分支(401冒充409)", '{"code":401,"message":"未认证"}', "401", False, 409),
        ("非 JSON 的 500", "<html>err</html>", "500", False, None),
        ("JSON 但缺 code", '{"message":"x"}', "400", False, None),
        ("正常 400", '{"code":400,"message":"必填"}', "400", True, 400),
        ("正常 404", '{"code":404,"message":"资源不存在"}', "404", True, 404),
        ("200 不参与", "", "200", True, None),
        ("409 缺 message", '{"code":409}', "409", False, None),
        ("409 正常", '{"code":409,"message":"已被处理"}', "409", True, 409),
        ("422 缺 message", '{"code":422}', "422", False, None),
        ("422 正常", '{"code":422,"message":"金额不符"}', "422", True, 422),
        ("replayed 是字符串", '{"code":400,"message":"x","replayed":"true"}', "400", False, None),
        ("replayed 是布尔", '{"code":400,"message":"x","replayed":false}', "400", True, None),
    ]
    ok = 0
    for name, body, code, expect, exp_code in cases:
        got, reason = check(body, code, exp_code)
        flag = "PASS" if got == expect else "FAIL"
        if got == expect:
            ok += 1
        print("[%s] self-test %-16s expect=%-5s got=%-5s %s" % (flag, name, expect, got, reason))
    print("self-test: %d/%d PASS" % (ok, len(cases)))
    return 0 if ok == len(cases) else FAIL_EXIT


def main():
    if "--self-test" in sys.argv:
        return self_test()

    # 拿 token（内部用户）
    body, code = curl(["-X", "POST", BASE + "/v1/pos/auth/login",
                       "-H", "Content-Type: application/json",
                       "-d", json.dumps({"employeeId": "vcashier01", "password": "123456"})])
    if code != "200":
        print("[ENV] 后端不可用（login HTTP=%s）—— 跳过" % code)
        return ENV_EXIT
    tok = json.loads(body)["data"]["token"]

    cases = [
        # (名称, 参数, 期望分支码) —— 期望分支码 = 「这条用例真的走到了它要测的分支」
        ("400 缺必填参数", ["-X", "POST", BASE + "/v1/pos/exceptions/O1793280664365/resolve",
                          "-H", "Content-Type: application/json", "-d", "{}",
                          "-H", "Authorization: Bearer " + tok], 400),
        ("409 订单不存在→已处理口径", ["-X", "POST", BASE + "/v1/pos/exceptions/NOSUCHORDER/void",
                          "-H", "Content-Type: application/json", "-d", json.dumps({"idempotencyKey": "k1", "reason": "X"}),
                          "-H", "Authorization: Bearer " + tok], 409),
        ("401 无 token", ["-X", "POST", BASE + "/v1/pos/exceptions/O1793280664365/resolve",
                        "-H", "Content-Type: application/json", "-d", "{}"], 401),
        ("404 不存在路径", ["-X", "POST", BASE + "/v1/pos/nosuchpath",
                        "-H", "Authorization: Bearer " + tok], 404),
        ("401 缺 query 参数(未带 token)", [BASE + "/v1/pos/shift/stats"], 401),
        # ⚠ 409「已被其它终端处理」这一分支**不在本 gate 覆盖**（2026-10-29 记录）：
        #   要触发它必须让订单 payment_status=2，而 payment_status=2 且无 SALE 流水会被 A1 判红；
        #   若给它补一条 SALE 流水，又会改变真实账户余额（违反 gate 零流水铁律）。
        #   两个约束不可同时满足 → 该分支由集成测试（T4 / S4'' 系列）覆盖，本 gate 只覆盖
        #   「409 订单不存在」（同码不同因，message 不同）与 422。
        #   —— 这是有意识的取舍，不是遗漏。
        ("422 金额不符", ["-X", "POST", BASE + "/v1/pos/exceptions/" + FIXTURE_422 + "/resolve",
                       "-H", "Content-Type: application/json",
                       "-d", json.dumps({"idempotencyKey": "gate-422", "amountFen": 9999, "payType": "CASH"}),
                       "-H", "Authorization: Bearer " + tok], 422),
    ]
    # ②（Lead 2026-10-29）：gate 夹具缺失 → NOT_READY EXIT=3（不产出假 FAIL）
    b_chk, c_chk = curl([BASE + "/v1/pos/shift/stats?terminalId=__GATE_PROBE__",
                         "-H", "Authorization: Bearer " + tok])
    probe = json.loads(curl(["-X", "POST", BASE + "/v1/pos/exceptions/" + FIXTURE_422 + "/resolve",
                             "-H", "Content-Type: application/json",
                             "-d", json.dumps({"idempotencyKey": "gate-probe", "amountFen": 9999, "payType": "CASH"}),
                             "-H", "Authorization: Bearer " + tok])[0] or "{}")
    if probe.get("code") == 409 and "订单不存在" in (probe.get("message") or ""):
        print("[NOT_READY] gate 夹具 %s 不存在（可能被清理）—— EXIT=3，不产出假 FAIL" % FIXTURE_422)
        print("           重建方式：见 docs/qa/fixtures-registry.md 的 [QA-GATE:*] 一节")
        return ENV_EXIT

    # ⚠ ①（Lead 2026-10-29）：**不再 prime** —— prime 会真的走 resolve，往真实现金账户记一笔资金流水。
    #   409 夹具改为「预置 exception_ops 行」，见 docs/qa/fixtures-registry.md。
    #   gate 必须**零资金流水**：任何一次运行都不得改变 bank_accounts / fund_flows。

    fails = 0
    for name, args, exp_code in cases:
        b, c = curl(args)
        ok, reason = check(b, c, exp_code)
        if not ok:
            fails += 1
        print("[%s] %-20s %s" % ("PASS" if ok else "FAIL", name, reason))
    print("汇总: %d PASS / %d FAIL（共 %d 组）" % (len(cases) - fails, fails, len(cases)))
    return 0 if fails == 0 else FAIL_EXIT


if __name__ == "__main__":
    sys.exit(main())
