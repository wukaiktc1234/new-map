#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""403 语义门禁（Lead 2026-10-30 P0）。

背景：`GET /v1/pos/shift/stats` 在权限不足时返回 **HTTP 200 + body.code=403**（框架包装），
      前端只看 HTTP 就把「鉴权失败」当成功；还有一种更坏的情形是「200 + data:[] / data:0」，
      即 **fail-open**：用「空集」表达「无权限」，前端无法区分「本班没数据」。

判据（写死，两条都必须成立）：
  ① 无权限 token → **HTTP == 403** 且 body 含 code；**返回 200（哪怕 data 空）一律 FAIL**
  ② 有权限 token → **HTTP != 403**（200 或有业务意义的 4xx，如缺参 400）

额外覆盖数据边界：有权限但 terminalId 属于别人 → 期望 403。

用法：python permission_403_check.py [--self-test]
退出码：0 PASS / 2 BLOCK / 3 环境不可用
"""
import json
import os
import subprocess
import sys

BASE = os.environ.get("QA_API_BASE", "http://127.0.0.1:8081/api")
ENC = {"encoding": "utf-8", "errors": "replace"}

# 无 pos:shift:manage 的身份（vtest01 有 6 个 pos:* 但不含 shift:manage）
NO_PERM_USER = ("vtest01", "123456")
WITH_PERM_USER = ("vcashier01", "123456")
TERM_MINE = "T-SEC-SEC1029223714-S3-SELF"
TERM_OTHERS = "pos-localhost-1793287925144"


def curl(args):
    r = subprocess.run(["curl.exe", "-s", "-w", "\n__HTTP__%{http_code}"] + args,
                       capture_output=True, text=True, **ENC)
    out = r.stdout or ""
    body, _, code = out.rpartition("\n__HTTP__")
    return body.strip(), code.strip()


def login(user, pwd):
    b, c = curl(["-X", "POST", BASE + "/v1/pos/auth/login", "-H", "Content-Type: application/json",
                 "-d", json.dumps({"employeeId": user, "password": pwd})])
    try:
        j = json.loads(b)
    except Exception:
        return None
    if j.get("code") != 0:
        return None
    return j["data"]["token"]


def judge_no_perm(http, body):
    """① 无权限判定（纯函数，供 self-test 复用）"""
    if http == "403":
        try:
            j = json.loads(body)
        except Exception:
            return False, "HTTP 403 但 body 不是 JSON"
        if "code" not in j:
            return False, "HTTP 403 但 body 缺 code"
        return True, "HTTP 403 + body.code=%s" % j.get("code")
    if http == "200":
        return False, "**fail-open**：无权限却返回 HTTP 200（body=%s）" % (body[:60] or "空")
    return False, "期望 403，实际 HTTP %s" % http


def find_others_active_terminal():
    """只读查库：找 status=active 且 employee_id != 当前登录人 userId 的 terminal_id。"""
    psql = os.environ.get("QA_PSQL", r"P:\my-new-project\PostgreSQL\18\bin\psql.exe")
    db = os.environ.get("QA_PGDB", "food_traceability")
    user = WITH_PERM_USER[0]
    q = ("SELECT p.terminal_id FROM pos_shifts p WHERE p.status='active' "
         "AND p.employee_id <> (SELECT u.user_id::text FROM users u WHERE u.username='" + user + "') LIMIT 1")
    try:
        env = dict(os.environ, PGPASSWORD=os.environ.get("PGPASSWORD", "123456"), PGCLIENTENCODING="UTF8")
        r = subprocess.run([psql, "-h", "localhost", "-p", "5432", "-U", "postgres", "-d", db,
                            "-A", "-t", "-c", q], capture_output=True, text=True, env=env, **ENC)
        val = (r.stdout or "").strip()
        return (val, "") if val else (None, "查询为空")
    except Exception as e:
        return (None, "查询失败：%s" % e)


def self_test():
    cases = [
        ("200 + 空 data（fail-open）", "200", '{"code":0,"data":[]}', False),
        ("200 + code=403（框架包装）", "200", '{"code":403,"message":"x"}', False),
        ("真 403 + code", "403", '{"code":403,"message":"权限不足"}', True),
        ("403 但空 body", "403", "", False),
        ("401 未认证", "401", '{"code":401}', False),
    ]
    ok = 0
    for name, http, body, expect in cases:
        got, reason = judge_no_perm(http, body)
        flag = "PASS" if got == expect else "FAIL"
        if got == expect:
            ok += 1
        print("[%s] self-test %-26s expect=%-5s got=%-5s %s" % (flag, name, expect, got, reason))
    print("self-test: %d/%d PASS" % (ok, len(cases)))
    return 0 if ok == len(cases) else 2


def main():
    if "--self-test" in sys.argv:
        return self_test()

    tok_no = login(*NO_PERM_USER)
    tok_yes = login(*WITH_PERM_USER)
    if not tok_no or not tok_yes:
        print("[ENV] 无法登录测试账号（NO_PERM/WITH_PERM），跳过")
        return 3

    fails = 0
    # ① 无权限 → 403
    for ep in ["/v1/pos/shift/stats?terminalId=" + TERM_MINE,
               "/v1/pos/shift/exceptions?terminalId=" + TERM_MINE + "&tab=pending",
               "/v1/pos/shift/opening-cash?terminalId=" + TERM_MINE]:
        b, c = curl([BASE + ep, "-H", "Authorization: Bearer " + tok_no])
        ok, reason = judge_no_perm(c, b)
        if not ok:
            fails += 1
        print("[%s] 无权限 %-52s %s" % ("PASS" if ok else "FAIL", ep[:52], reason))

    # ② 有权限 → 非 403
    for ep in ["/v1/pos/shift/stats?terminalId=" + TERM_MINE,
               "/v1/pos/shift/exceptions?terminalId=" + TERM_MINE + "&tab=pending"]:
        b, c = curl([BASE + ep, "-H", "Authorization: Bearer " + tok_yes])
        if c == "403":
            fails += 1
            print("[FAIL] 有权限 %-52s 不应 403（body=%s）" % (ep[:52], b[:60]))
        else:
            print("[PASS] 有权限 %-52s HTTP %s" % (ep[:52], c))

    # ③ 数据边界（真阳性）：必须有【他人活跃班次】的终端才算测到
    cross_term, why = find_others_active_terminal()
    skipped_cross = False
    if not cross_term:
        print("[SKIP] 跨终端用例未执行：库中不存在「他人活跃班次」的终端（%s）—— 不计 PASS" % why)
        skipped_cross = True
    else:
        b, c = curl([BASE + "/v1/pos/shift/stats?terminalId=" + cross_term,
                     "-H", "Authorization: Bearer " + tok_yes])
        ok, reason = judge_no_perm(c, b)
        if ok:
            print("[PASS] 跨终端 403 ｜终端=%s（有他人活跃班次且不属于我）" % cross_term)
        else:
            fails += 1
            print("[FAIL] 跨终端 ｜终端=%s ｜%s" % (cross_term, reason))

    print("汇总: %d FAIL%s" % (fails, "（跨终端 SKIP，不计 PASS）" if skipped_cross else ""))
    if fails > 0:
        return 2
    # ①（Lead 2026-10-30）：SKIP 必须 exit 3（环境不足），不能混进 PASS
    return 3 if skipped_cross else 0


if __name__ == "__main__":
    sys.exit(main())
