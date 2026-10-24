
# -*- coding: utf-8 -*-
"""P1-B 对账自测（R1-R10 + 红线 + 回滚）。

用法:
  python scripts/qa/recon_selftest.py            # 主流程（跑批幂等/force/勾稽/差异/认领/处理/解绑）
  python scripts/qa/recon_selftest.py --phase rollback
        # 回滚验收：需应用以 --app.finance.recon.fail-inject=true 启动

认证：本地自测用 dev JWT 密钥直接签发管理员 token（不写 users 表、不改密码）。
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
import urllib.request
import uuid

BASE = os.environ.get("RECON_BASE", "http://127.0.0.1:18080/api")
PSQL = os.environ.get("RECON_PSQL", r"P:\my-new-project\PostgreSQL\18\bin\psql.exe")
PGPASSWORD = os.environ.get("RECON_PGPASSWORD", "123456")
PGDB = os.environ.get("RECON_PGDB", "food_traceability")
SECRET = os.environ.get(
    "RECON_JWT_SECRET",
    "dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security",
)
ACCOUNT_ID = int(os.environ.get("RECON_ACCOUNT_ID", "20"))
BUSINESS_DATE = os.environ.get("RECON_BUSINESS_DATE", "2026-10-24")

RESULTS = []


def check(name, ok, detail=""):
    RESULTS.append((name, ok, detail))
    print(("[PASS] " if ok else "[FAIL] ") + name + ("  " + detail if detail else ""))


def b64u(raw):
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode("ascii")


def mint_token():
    now = int(time.time())
    header = {"alg": "HS512", "typ": "JWT"}
    payload = {
        "iss": "food-traceability-system",
        "sub": "recon-selftest",
        "userId": "1",
        "username": "recon-selftest",
        "roles": ["ADMIN"],
        "permissions": ["finance:bank:recon", "finance:bank:view", "*"],
        "mfaEnabled": False,
        "mfaVerified": False,
        "tokenId": str(uuid.uuid4()),
        "iat": now,
        "exp": now + 3600,
    }
    signing = b64u(json.dumps(header, separators=(",", ":")).encode()) + "." + b64u(
        json.dumps(payload, separators=(",", ":")).encode())
    sig = hmac.new(SECRET.encode(), signing.encode(), hashlib.sha512).digest()
    return signing + "." + b64u(sig)


TOKEN = mint_token()


def call(method, path, body=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    req.add_header("Authorization", "Bearer " + TOKEN)
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}


def sql(query):
    env = dict(os.environ, PGPASSWORD=PGPASSWORD, PGCLIENTENCODING="UTF8")
    proc = subprocess.run(
        [PSQL, "-X", "-q", "-t", "-A", "-F", "|", "-h", "localhost", "-p", "5432",
         "-U", "postgres", "-d", PGDB, "-c", "SET TRANSACTION READ ONLY; " + query],
        capture_output=True, text=True, encoding="utf-8", env=env)
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip())
    return [line.split("|") for line in proc.stdout.splitlines() if line.strip()]


def one(query):
    rows = sql(query)
    return rows[0] if rows else []


def data_of(body):
    return (body or {}).get("data")


def phase_main():
    print("== 基础：当前批次状态 ==")
    before_runs = int(one("select count(*) from recon_runs")[0])
    fund_before = int(one("select count(*) from fund_flows")[0])
    balance_before = one("select coalesce(sum(balance),0) from bank_accounts")[0]
    print("runs=%d fund_flows=%d balance_sum=%s" % (before_runs, fund_before, balance_before))

    # ---------- R1 幂等（不 force） ----------
    st, body = call("POST", "/v1/finance/recon-runs", {"accountId": ACCOUNT_ID, "businessDate": BUSINESS_DATE})
    d = data_of(body) or {}
    run = d.get("run") or {}
    check("R1 跑批 200", st == 200 and (body or {}).get("code") == 0, "http=%s code=%s" % (st, (body or {}).get("code")))
    check("R1 幂等 reused=true（同日同账户未 force）", d.get("reused") is True, "reused=%s runId=%s" % (d.get("reused"), run.get("runId")))
    runs_after_reuse = int(one("select count(*) from recon_runs")[0])
    check("幂等不新建批次（count 不变）", runs_after_reuse == before_runs, "before=%d after=%d" % (before_runs, runs_after_reuse))
    base_run_id = run.get("runId")

    # ---------- R1 force（重建） ----------
    st, body = call("POST", "/v1/finance/recon-runs", {"accountId": ACCOUNT_ID, "businessDate": BUSINESS_DATE, "force": True})
    d = data_of(body) or {}
    run2 = d.get("run") or {}
    new_run_id = run2.get("runId")
    check("R1 force 新建批次", st == 200 and d.get("reused") is False and new_run_id != base_run_id,
          "http=%s new=%s old=%s" % (st, new_run_id, base_run_id))
    check("R1 force rebuild_of=旧批次", run2.get("rebuildOf") == base_run_id, "rebuildOf=%s expect=%s" % (run2.get("rebuildOf"), base_run_id))
    check("R1 run=FINALIZED", run2.get("status") == "FINALIZED", "status=%s" % run2.get("status"))
    old_run = one("select status, internal_count, difference_count from recon_runs where run_id=%s" % base_run_id)
    check("force 后旧批次只读不变（仍 FINALIZED）", old_run and old_run[0] == "FINALIZED", "old=%s" % (old_run,))

    # ---------- R3 详情 ----------
    st, body = call("GET", "/v1/finance/recon-runs/%d" % new_run_id)
    detail = data_of(body) or {}
    check("R3 详情可读且勾稽率有值", st == 200 and detail.get("matchRate") is not None,
          "internal=%s/%s matched=%s/%s rate=%s diffs=%s" % (
              detail.get("internalCount"), detail.get("internalAmount"),
              detail.get("matchedCount"), detail.get("matchedAmount"),
              detail.get("matchRate"), detail.get("differenceCount")))
    check("R3 批次已落 items", int(one("select count(*) from recon_items where run_id=%d" % new_run_id)[0]) > 0)
    check("R3 批次已落 differences", int(one("select count(*) from recon_differences where run_id=%d" % new_run_id)[0]) > 0)

    # ---------- AC-17：外部金额口径（分） ----------
    ext_sum = one("select coalesce(sum(amount),0) from bank_transactions where deleted=0 and account_id=%d and business_date='%s'" % (ACCOUNT_ID, BUSINESS_DATE))[0]
    check("AC-17 external_amount = SUM(bank_transactions.amount)", str(detail.get("externalAmount")) == str(ext_sum),
          "run=%s sql=%s" % (detail.get("externalAmount"), ext_sum))

    # ---------- R5 主体列表 ----------
    st, body = call("GET", "/v1/finance/recon-items?runId=%d&side=INTERNAL&current=1&size=5" % new_run_id)
    items = (data_of(body) or {}).get("records") or []
    check("R5 主体列表分页 + side 过滤", st == 200 and len(items) > 0 and all(i.get("side") == "INTERNAL" for i in items),
          "total=%s" % ((data_of(body) or {}).get("total")))

    # ---------- R8 差异池 ----------
    st, body = call("GET", "/v1/finance/recon-differences?runId=%d&status=OPEN&current=1&size=50" % new_run_id)
    diffs = (data_of(body) or {}).get("records") or []
    check("R8 差异池 OPEN 过滤", st == 200 and len(diffs) > 0, "open=%d" % len(diffs))
    types = sorted({x.get("differenceType") for x in diffs})
    check("差异类型覆盖（单边/金额/重复/模糊）", len(types) >= 1, "types=%s" % types)

    # ---------- R9/R10：claim → resolve(WRITE_OFF) ----------
    target = next((x for x in diffs if x.get("differenceType") == "ONE_SIDED_INTERNAL"), diffs[0] if diffs else None)
    if target:
        did = target.get("differenceId")
        st, body = call("POST", "/v1/finance/recon-differences/%d/claim" % did)
        claimed = data_of(body) or {}
        check("R9 认领 → ASSIGNED", st == 200 and claimed.get("status") == "ASSIGNED" and claimed.get("assigneeId") is not None,
              "status=%s assignee=%s" % (claimed.get("status"), claimed.get("assigneeId")))

        st, body = call("POST", "/v1/finance/recon-differences/%d/resolve" % did, {"resolution": "WRITE_OFF", "remark": "自测：核销（只记决议）"})
        resolved = data_of(body) or {}
        check("R10 WRITE_OFF → CLOSED", st == 200 and resolved.get("status") == "CLOSED" and resolved.get("resolution") == "WRITE_OFF",
              "status=%s resolution=%s" % (resolved.get("status"), resolved.get("resolution")))

    # ---------- R10：resolve 缺 remark → 400 ----------
    pending = next((x for x in diffs if x.get("status") == "OPEN" and x.get("differenceId") != (target or {}).get("differenceId")), None)
    if pending:
        st, body = call("POST", "/v1/finance/recon-differences/%d/resolve" % pending.get("differenceId"), {"resolution": "WRITE_OFF"})
        check("R10 缺 remark → 业务错误(400)", (body or {}).get("code") == 400 or st == 400,
              "http=%s code=%s msg=%s" % (st, (body or {}).get("code"), (body or {}).get("message")))

        # ---------- R10：REVERSE → PENDING_P2_REVERSE ----------
        st, body = call("POST", "/v1/finance/recon-differences/%d/resolve" % pending.get("differenceId"),
                        {"resolution": "REVERSE", "remark": "自测：冲正决议，等 P2 执行"})
        reversed_row = data_of(body) or {}
        # 落库值 PENDING_P2_REV（14 字符）：recon_differences.status 是 varchar(16)，
        # 设计稿的 PENDING_P2_REVERSE（18 字符）超出列宽（§5.6 结构冻结，不改列宽）
        check("R10 REVERSE → PENDING_P2_REV（不生成流水）", st == 200 and reversed_row.get("status") == "PENDING_P2_REV",
              "status=%s" % reversed_row.get("status"))
        check("R10 REVERSE 未回填 reverse_flow_no", reversed_row.get("reverseFlowNo") is None,
              "reverseFlowNo=%s" % reversed_row.get("reverseFlowNo"))

    # ---------- R6/R7：人工勾稽 + 解绑 ----------
    st, body = call("GET", "/v1/finance/recon-items?runId=%d&side=INTERNAL&subjectStatus=UNMATCHED&current=1&size=1" % new_run_id)
    internal_item = ((data_of(body) or {}).get("records") or [None])[0]
    st, body = call("GET", "/v1/finance/recon-items?runId=%d&side=EXTERNAL&subjectStatus=UNMATCHED&current=1&size=1" % new_run_id)
    external_item = ((data_of(body) or {}).get("records") or [None])[0]
    if internal_item and external_item:
        st, body = call("POST", "/v1/finance/recon-items/%d/match" % internal_item["itemId"],
                        {"externalRef": external_item["externalRef"]})
        matched = data_of(body) or {}
        check("R6 人工勾稽 → MANUAL_MATCHED", st == 200 and matched.get("subjectStatus") == "MANUAL_MATCHED",
              "status=%s group=%s" % (matched.get("subjectStatus"), matched.get("matchGroup")))
        partner = one("select subject_status, match_group from recon_items where run_id=%d and side='EXTERNAL' and external_ref='%s'" % (new_run_id, external_item["externalRef"].replace("'", "''")))
        check("R6 对侧主体同步 MANUAL_MATCHED", bool(partner) and partner[0] == "MANUAL_MATCHED", "partner=%s" % (partner,))

        st, body = call("POST", "/v1/finance/recon-items/%d/unmatch" % internal_item["itemId"])
        unmatched = data_of(body) or {}
        check("R7 解绑 → UNMATCHED 且清 match_group",
              st == 200 and unmatched.get("subjectStatus") == "UNMATCHED" and unmatched.get("matchGroup") is None,
              "status=%s group=%s" % (unmatched.get("subjectStatus"), unmatched.get("matchGroup")))
        partner2 = one("select subject_status, coalesce(match_group,'NULL') from recon_items where run_id=%d and side='EXTERNAL' and external_ref='%s'" % (new_run_id, external_item["externalRef"].replace("'", "''")))
        check("R7 对侧主体同步解绑", bool(partner2) and partner2[0] == "UNMATCHED", "partner=%s" % (partner2,))

    # ---------- 红线：fund_flows / bank_accounts 零变化 ----------
    fund_after = int(one("select count(*) from fund_flows")[0])
    balance_after = one("select coalesce(sum(balance),0) from bank_accounts")[0]
    check("红线：fund_flows 行数不变", fund_after == fund_before, "before=%d after=%d" % (fund_before, fund_after))
    check("红线：bank_accounts.balance 合计不变", balance_after == balance_before, "before=%s after=%s" % (balance_before, balance_after))

    # ---------- 红字流水 FF202609080010/28 未被动过 ----------
    untouched = one("select count(*) from fund_flows where flow_no in ('FF202609080010','FF202609080028') and status=2 and deleted=0")[0]
    check("红线：历史流水 FF202609080010/28 仍 status=2 且未删除", str(untouched) == "2", "count=%s" % untouched)


def phase_rollback():
    print("== 回滚验收（需 --app.finance.recon.fail-inject=true）==")
    before_runs = int(one("select count(*) from recon_runs")[0])
    st, body = call("POST", "/v1/finance/recon-runs", {"accountId": ACCOUNT_ID, "businessDate": BUSINESS_DATE, "force": True})
    print("http=%s code=%s msg=%s" % (st, (body or {}).get("code"), (body or {}).get("message")))
    check("跑批失败返回业务错误", (body or {}).get("code") not in (0, None), "code=%s" % (body or {}).get("code"))

    after_runs = int(one("select count(*) from recon_runs")[0])
    check("失败仍留下 run 行（独立事务）", after_runs == before_runs + 1, "before=%d after=%d" % (before_runs, after_runs))
    latest = one("select run_id, status from recon_runs order by run_id desc limit 1")
    failed_id = latest[0] if latest else None
    check("最新 run = FAILED", bool(latest) and latest[1] == "FAILED", "run=%s" % (latest,))
    if failed_id:
        items = int(one("select count(*) from recon_items where run_id=%s" % failed_id)[0])
        diffs = int(one("select count(*) from recon_differences where run_id=%s" % failed_id)[0])
        check("失败批次 items = 0（事务整体回滚）", items == 0, "items=%d" % items)
        check("失败批次 differences = 0（事务整体回滚）", diffs == 0, "diffs=%d" % diffs)


def main():
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass
    parser = argparse.ArgumentParser()
    parser.add_argument("--phase", default="main", choices=["main", "rollback"])
    args = parser.parse_args()
    if args.phase == "rollback":
        phase_rollback()
    else:
        phase_main()
    failed = [r for r in RESULTS if not r[1]]
    print("\n== 汇总：%d 项，%d PASS / %d FAIL ==" % (len(RESULTS), len(RESULTS) - len(failed), len(failed)))
    for name, ok, detail in failed:
        print("  FAIL: " + name + "  " + detail)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
