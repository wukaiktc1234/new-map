
# -*- coding: utf-8 -*-
"""收口 §3 三场景验证。S3_SCENARIO: e21(有交集5分钟) / noid(23h无交集) / h25(25h有交集)。"""
import base64, hashlib, hmac, json, os, subprocess, sys, time, urllib.error, urllib.request, uuid

BASE = "http://127.0.0.1:18080/api"
PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
SECRET = "dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security"
PGPASSWORD, PGDB = "123456", "food_traceability"
RES = []

def check(name, ok, detail=""):
    RES.append((name, ok, detail)); print(("[PASS] " if ok else "[FAIL] ") + name + ("\n    " + detail if detail else ""))

def b64u(b): return base64.urlsafe_b64encode(b).rstrip(b"=").decode("ascii")

def mint(uid=1):
    now = int(time.time()); h = {"alg": "HS512", "typ": "JWT"}
    p = {"iss": "food-traceability-system", "sub": "s3-" + str(uid), "userId": str(uid), "username": "s3-user",
         "roles": ["ADMIN"], "permissions": ["finance:bank:recon", "finance:bank:view", "*"],
         "mfaEnabled": False, "mfaVerified": False, "tokenId": str(uuid.uuid4()), "iat": now, "exp": now + 3600}
    s = b64u(json.dumps(h, separators=(",", ":")).encode()) + "." + b64u(json.dumps(p, separators=(",", ":")).encode())
    return s + "." + b64u(hmac.new(SECRET.encode(), s.encode(), hashlib.sha512).digest())

TOK = mint(1)

def call(method, path, body=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json"); req.add_header("Authorization", "Bearer " + TOK)
    try:
        with urllib.request.urlopen(req, timeout=180) as r: return r.status, json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try: return e.code, json.loads(raw)
        except Exception: return e.code, {"raw": raw}

def sql(q, write=False):
    env = dict(os.environ, PGPASSWORD=PGPASSWORD, PGCLIENTENCODING="UTF8")
    pr = subprocess.run([PSQL, "-X", "-q", "-t", "-A", "-F", "|", "-h", "localhost", "-p", "5432", "-U", "postgres",
                         "-d", PGDB, "-c", ("" if write else "SET TRANSACTION READ ONLY; ") + q],
                        capture_output=True, text=True, encoding="utf-8", env=env)
    if pr.returncode != 0: raise RuntimeError(pr.stderr.strip())
    return [l.split("|") for l in pr.stdout.splitlines() if l.strip()]

def one(q, write=False):
    r = sql(q, write); return r[0] if r else []

def main():
    for s in (sys.stdout, sys.stderr):
        try: s.reconfigure(encoding="utf-8", errors="replace")
        except Exception: pass
    sc = os.environ.get("S3_SCENARIO", "e21")
    amount = 45678
    if sc == "e21":
        flow_no, ext_no, order_id = "FFS3E21", "S3-E21-REF", "S3-E21-REF"
        t_flow, t_txn = "2026-10-24 23:58:00", "2026-10-25 00:03:00"
    elif sc == "noid":
        flow_no, ext_no, order_id = "FFS3N23", "S3-N23-EXT", "S3-N23-INT"
        t_flow, t_txn = "2026-10-24 00:30:00", "2026-10-24 23:30:00"
        amount = 51234
    else:
        flow_no, ext_no, order_id = "FFS3H25", "S3-H25-REF", "S3-H25-REF"
        t_flow, t_txn = "2026-10-23 22:00:00", "2026-10-24 23:00:00"
        amount = 52345

    one("DELETE FROM recon_differences WHERE run_id IN (SELECT run_id FROM recon_runs WHERE account_id=24 AND business_date=DATE '2026-10-24')", write=True)
    one("DELETE FROM recon_items WHERE run_id IN (SELECT run_id FROM recon_runs WHERE account_id=24 AND business_date=DATE '2026-10-24')", write=True)
    one("DELETE FROM recon_runs WHERE account_id=24 AND business_date=DATE '2026-10-24'", write=True)
    one("DELETE FROM fund_flows WHERE flow_no='%s'" % flow_no, write=True)
    one("DELETE FROM bank_transactions WHERE external_txn_id='%s'" % ext_no, write=True)
    bal = int(one("select coalesce(balance,0) from bank_accounts where account_id=24")[0])

    one("INSERT INTO fund_flows (flow_no, account_id, flow_direction, flow_category, amount, balance_after, business_date, remark, order_id, status, create_user_id, created_by, create_time, update_time, version, deleted) "
        "VALUES ('%s', 24, 1, 1, %d, %d, DATE '2026-10-24', 'S3 sample', '%s', 2, 1, 's3-selftest', TIMESTAMP '%s', TIMESTAMP '%s', 0, 0)"
        % (flow_no, amount, bal, order_id, t_flow, t_flow), write=True)
    one("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id, merchant_order_no, direction, amount, fee, txn_time, business_date, dedupe_basis, raw_hash, create_user_id, create_time, update_time, deleted) "
        "VALUES (0, 24, 'S3TEST', '%s', NULL, 1, %d, 0, TIMESTAMP '%s', DATE '2026-10-24', 'S3', 's3', 1, NOW(), NOW(), 0)"
        % (ext_no, amount, t_txn), write=True)

    st, body = call("POST", "/v1/finance/recon-runs", {"accountId": 24, "businessDate": "2026-10-24"})
    run = ((body or {}).get("data") or {}).get("run") or {}
    rid = run.get("runId")
    print("场景=%s run=%s status=%s matchedAmount=%s" % (sc, rid, run.get("status"), run.get("matchedAmount")))

    items = sql("select side, subject_status, match_level, amount from recon_items where run_id=%s and (internal_ref='%s' or external_ref='%s') order by side" % (rid, flow_no, ext_no))
    diffs = sql("select difference_type, internal_ref, external_ref, diff_amount, status, resolution_remark from recon_differences where run_id=%s and (internal_ref='%s' or external_ref='%s')" % (rid, flow_no, ext_no))
    auto_amount = int(one("select coalesce(sum(amount),0) from recon_items where run_id=%s and side='INTERNAL' and subject_status='AUTO_MATCHED'" % rid)[0])
    manual_amount = int(one("select coalesce(sum(amount),0) from recon_items where run_id=%s and side='INTERNAL' and subject_status='MANUAL_MATCHED'" % rid)[0])
    print("  items=%s" % (items,)); print("  diffs=%s" % (diffs,))
    print("  matched_amount=%s | auto_matched_amount(仅 AUTO_MATCHED)=%d | manual_matched_amount=%d" % (run.get("matchedAmount"), auto_amount, manual_amount))

    if sc == "e21":
        check("E2-1 有身份交集 + 5 分钟 → TIME_DIFF", len(diffs) == 1 and diffs[0][0] == "TIME_DIFF", "diffs=%s" % (diffs,))
        check("E2-1 两侧 MANUAL_MATCHED / match_level=MANUAL",
              len(items) == 2 and all(i[1] == "MANUAL_MATCHED" and i[2] == "MANUAL" for i in items), "items=%s" % (items,))
        check("E2-1 不计入自动勾稽率（auto=0，manual=本题金额）", auto_amount == 0 and manual_amount == amount, "auto=%d manual=%d" % (auto_amount, manual_amount))
        check("E2-1 remark 含『跨日时间差，需人工确认』", "需人工确认" in (diffs[0][5] or ""), "remark=%s" % (diffs[0][5],))
    elif sc == "noid":
        check("23h 无身份交集 → AMBIGUOUS（不判 TIME_DIFF）", len(diffs) == 1 and diffs[0][0] == "AMBIGUOUS", "diffs=%s" % (diffs,))
        check("23h 无交集 两侧 subject_status=MATCHED（待人工）", len(items) == 2 and all(i[1] == "MATCHED" for i in items), "items=%s" % ([i[1] for i in items],))
    else:
        check("25h 超窗（有交集）→ ONE_SIDED_*", len(diffs) == 2 and all(d[0].startswith("ONE_SIDED") for d in diffs), "diffs=%s" % ([d[0] for d in diffs],))
        check("25h 两侧保持 UNMATCHED", len(items) == 2 and all(i[1] == "UNMATCHED" for i in items), "items=%s" % ([i[1] for i in items],))

    one("DELETE FROM fund_flows WHERE flow_no='%s'" % flow_no, write=True)
    one("DELETE FROM bank_transactions WHERE external_txn_id='%s'" % ext_no, write=True)
    print("== 汇总：%d 项，%d PASS / %d FAIL ==" % (len(RES), sum(1 for r in RES if r[1]), sum(1 for r in RES if not r[1])))
    return 0 if all(r[1] for r in RES) else 1

sys.exit(main())
