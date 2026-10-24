
# -*- coding: utf-8 -*-
"""BL-7 TIME_DIFF + BL-8/TD-36 business_date 验证（独立脚本，无外部依赖）。"""
import base64, hashlib, hmac, json, os, subprocess, sys, time, urllib.error, urllib.request, uuid

BASE = "http://127.0.0.1:18080/api"
PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
SECRET = "dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security"
PGPASSWORD, PGDB = "123456", "food_traceability"
RES = []

def check(name, ok, detail=""):
    RES.append((name, ok, detail))
    print(("[PASS] " if ok else "[FAIL] ") + name + ("\n    " + detail if detail else ""))

def b64u(b):
    return base64.urlsafe_b64encode(b).rstrip(b"=").decode("ascii")

def mint(uid=1):
    now = int(time.time())
    h = {"alg": "HS512", "typ": "JWT"}
    p = {"iss": "food-traceability-system", "sub": "bl7-" + str(uid), "userId": str(uid), "username": "bl7-user",
         "roles": ["ADMIN"], "permissions": ["finance:bank:recon", "finance:bank:view",
         "finance:deposit-slip:create", "finance:deposit-slip:view", "*"],
         "mfaEnabled": False, "mfaVerified": False, "tokenId": str(uuid.uuid4()), "iat": now, "exp": now + 3600}
    s = b64u(json.dumps(h, separators=(",", ":")).encode()) + "." + b64u(json.dumps(p, separators=(",", ":")).encode())
    return s + "." + b64u(hmac.new(SECRET.encode(), s.encode(), hashlib.sha512).digest())

TOK = mint(1)

def call(method, path, body=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    req.add_header("Authorization", "Bearer " + TOK)
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            return r.status, json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}

def sql(q, write=False):
    env = dict(os.environ, PGPASSWORD=PGPASSWORD, PGCLIENTENCODING="UTF8")
    pr = subprocess.run([PSQL, "-X", "-q", "-t", "-A", "-F", "|", "-h", "localhost", "-p", "5432", "-U", "postgres",
                         "-d", PGDB, "-c", ("" if write else "SET TRANSACTION READ ONLY; ") + q],
                        capture_output=True, text=True, encoding="utf-8", env=env)
    if pr.returncode != 0:
        raise RuntimeError(pr.stderr.strip())
    return [l.split("|") for l in pr.stdout.splitlines() if l.strip()]

def one(q, write=False):
    r = sql(q, write)
    return r[0] if r else []

def d(b):
    return (b or {}).get("data") or {}

def main():
    for s in (sys.stdout, sys.stderr):
        try:
            s.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass

    AMOUNT, FLOW_NO, EXT_NO = 45678, "FFBL7E21T01", "BL7-EXT-E21-01"
    # 清掉本轮自测产生的旧 run（account 24 / 2026-10-24），避免重复跑批堆积
    one("DELETE FROM recon_differences WHERE run_id IN (SELECT run_id FROM recon_runs WHERE account_id=24 AND business_date=DATE '2026-10-24')", write=True)
    one("DELETE FROM recon_items WHERE run_id IN (SELECT run_id FROM recon_runs WHERE account_id=24 AND business_date=DATE '2026-10-24')", write=True)
    one("DELETE FROM recon_runs WHERE account_id=24 AND business_date=DATE '2026-10-24'", write=True)
    one("DELETE FROM fund_flows WHERE flow_no='%s'" % FLOW_NO, write=True)
    one("DELETE FROM bank_transactions WHERE external_txn_id='%s'" % EXT_NO, write=True)
    bal = int(one("select coalesce(balance,0) from bank_accounts where account_id=24")[0])

    one("INSERT INTO fund_flows (flow_no, account_id, flow_direction, flow_category, amount, balance_after, business_date, remark, order_id, status, create_user_id, created_by, create_time, update_time, version, deleted) "
        "VALUES ('%s', 24, 1, 1, %d, %d, DATE '2026-10-24', 'BL7 E2-1 internal SALE cross-day sample', 'E21-ORDER-1', 2, 1, 'bl7-selftest', TIMESTAMP '2026-10-24 23:58:00', TIMESTAMP '2026-10-24 23:58:00', 0, 0)" % (FLOW_NO, AMOUNT, bal), write=True)
    one("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id, direction, amount, fee, txn_time, business_date, dedupe_basis, raw_hash, create_user_id, create_time, update_time, deleted) "
        "VALUES (0, 24, 'BL7TEST', '%s', 1, %d, 0, TIMESTAMP '2026-10-25 00:03:00', DATE '2026-10-24', 'BL7-E21', 'bl7', 1, NOW(), NOW(), 0)" % (EXT_NO, AMOUNT), write=True)

    st, body = call("POST", "/v1/finance/recon-runs", {"accountId": 24, "businessDate": "2026-10-24"})
    run = d(body).get("run") or {}
    run_id = run.get("runId")
    print("跑批: http=%s code=%s runId=%s status=%s internal=%s external=%s reused=%s" % (
        st, (body or {}).get("code"), run_id, run.get("status"), run.get("internalCount"), run.get("externalCount"), d(body).get("reused")))
    check("勾稽跑批成功（仅一次）", run_id is not None, "runId=%s" % run_id)

    items = sql("select side, internal_ref, external_ref, subject_status, match_level, txn_time from recon_items where run_id=%s and (internal_ref='%s' or external_ref='%s') order by side" % (run_id, FLOW_NO, EXT_NO))
    diffs = sql("select difference_type, internal_ref, external_ref, amount_internal, amount_external, diff_amount, status, resolution_remark from recon_differences where run_id=%s and (internal_ref='%s' or external_ref='%s')" % (run_id, FLOW_NO, EXT_NO))
    print("  items = %s" % (items,))
    print("  diffs = %s" % (diffs,))
    check("BL-7 两侧 subject_status = MATCHED", len(items) == 2 and all(i[3] == "MATCHED" for i in items), "items=%s" % ([i[3] for i in items],))
    check("BL-7 difference_type = TIME_DIFF 且 diff_amount = 0", len(diffs) == 1 and diffs[0][0] == "TIME_DIFF" and diffs[0][5] == "0", "diffs=%s" % (diffs,))
    check("BL-7 未落 AMBIGUOUS / ONE_SIDED", (not diffs or diffs[0][0] == "TIME_DIFF"), "types=%s" % ([x[0] for x in diffs],))

    t = one("select f.business_date, f.create_time, t.business_date, t.txn_time from fund_flows f, bank_transactions t where f.flow_no='%s' and t.external_txn_id='%s'" % (FLOW_NO, EXT_NO))
    print("  内部: business_date=%s create_time=%s | 外部: business_date=%s txn_time=%s" % (t[0], t[1], t[2], t[3]))

    # ---- BL-8 / TD-36 ----
    one("DELETE FROM deposit_slips WHERE slip_id=61 AND status='DRAFT'", write=True)
    st, body = call("POST", "/v1/finance/deposit-slips/generate", {"shiftId": 85, "storeId": 1})
    slip = d(body).get("slip") or {}
    print("跨天班次 85: slipId=%s slipNo=%s expected=%s businessDate=%s reused=%s" % (
        slip.get("slipId"), slip.get("slipNo"), slip.get("expectedAmount"), slip.get("businessDate"), d(body).get("reused")))
    check("TD-36 跨天班次 business_date = 交班日 2026-10-24", str(slip.get("businessDate")) == "2026-10-24", "businessDate=%s" % slip.get("businessDate"))
    check("TD-36 expected_amount 按班次时间窗统计 = 516340", slip.get("expectedAmount") == 516340, "expected=%s" % slip.get("expectedAmount"))
    check("TD-36 VO 已映射 businessDate", slip.get("businessDate") is not None, "vo=%s" % slip.get("businessDate"))

    one("DELETE FROM fund_flows WHERE flow_no='%s'" % FLOW_NO, write=True)
    one("DELETE FROM bank_transactions WHERE external_txn_id='%s'" % EXT_NO, write=True)
    print("\n== 汇总：%d 项，%d PASS / %d FAIL ==" % (len(RES), sum(1 for r in RES if r[1]), sum(1 for r in RES if not r[1])))
    return 0 if all(r[1] for r in RES) else 1

sys.exit(main())
