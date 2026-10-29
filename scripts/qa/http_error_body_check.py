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
import sys

BASE = os.environ.get("DSH_API_BASE", "http://127.0.0.1:8081/api")
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
        return True, "HTTP<400 不参与判定"
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
        # ③（Lead 2026-10-29）：409 / 422 也必须带 JSON body（前端读 message 才能显示具体原因）
        ("409 重复处理", ["-X", "POST", BASE + "/v1/pos/exceptions/O1793282514070/resolve",
                       "-H", "Content-Type: application/json",
                       "-d", json.dumps({"idempotencyKey": "gate-409", "amountFen": 2200, "payType": "CASH"}),
                       "-H", "Authorization: Bearer " + tok], 409),
        ("422 金额不符", ["-X", "POST", BASE + "/v1/pos/exceptions/O1793282514070/resolve",
                       "-H", "Content-Type: application/json",
                       "-d", json.dumps({"idempotencyKey": "gate-422", "amountFen": 9999, "payType": "CASH"}),
                       "-H", "Authorization: Bearer " + tok], 422),
    ]
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
