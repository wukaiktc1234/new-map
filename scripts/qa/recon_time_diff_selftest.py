# ==== DBGUARD（Lead 2026-10-29 死命令 1/2）====
def _dbguard_boot():
    import os as _os, sys as _sys
    _sys.path.insert(0, _os.path.dirname(_os.path.abspath(__file__)))
    from _dbguard import guard as _guard
    return _guard(__file__)
if _dbguard_boot() != 0:
    raise SystemExit(2)
# ==== DBGUARD END ====

# -*- coding: utf-8 -*-
"""BL-7：TIME_DIFF 实现验证（E2-1 跨日场景）。"""
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
    p = {"iss": "food-traceability-system", "sub": "bl7-" + str(uid), "userId": str(uid),
         "username": "bl7-user", "roles": ["ADMIN"],
         "permissions": ["finance:bank:recon", "finance:bank:view", "*"],
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
    pr = subprocess.run([PSQL, "-X", "-q", "-t", "-A", "-F", "|", "-h", "localhost", "-p", "5432",
                         "-U", "postgres", "-d", PGDB, "-c", ("" if write else "SET TRANSACTION READ ONLY; ") + q],
                        capture_output=True, text=True, encoding="utf-8", env=env)
    if pr.returncode != 0:
        raise RuntimeError(pr.stderr.strip())
    return [l.split("|") for l in pr.stdout.splitlines() if l.strip()]

def one(q, write=False):
    r = sql(q, write)
    return r[0] if r else []

def main():
    for s in (sys.stdout, sys.stderr):
        try:
            s.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass

    AMOUNT = 45678
    FLOW_NO = "FFBL7E21T01"
    EXT_NO = "BL7-EXT-E21-01"
    # 清理可能残留的夹具（软删即可）
    one("DELETE FROM fund_flows WHERE flow_no='%s'" % FLOW_NO, write=True)
    one("DELETE FROM bank_transactions WHERE external_txn_id='%s'" % EXT_NO, write=True)
    one("DELETE FROM recon_differences WHERE internal_ref='%s' OR external_ref='%s'" % (FLOW_NO, EXT_NO), write=True)
    one("DELETE FROM recon_items WHERE internal_ref='%s' OR external_ref='%s'" % (FLOW_NO, EXT_NO), write=True)
    bal = int(one("select coalesce(balance,0) from bank_accounts where account_id=24")[0])

    # 内部 SALE：业务日 2026-10-24，入账时间 23:58
    one("INSERT INTO fund_flows (flow_no, account_id, flow_direction, flow_category, amount, balance_after, business_date, remark, order_id, status, create_user_id, created_by, create_time, update_time, version, deleted) "
        "VALUES ('%s', 24, 1, 1, %d, %d, DATE '2026-10-24', 'BL7 E2-1 internal SALE cross-day sample', 'E21-ORDER-1', 2, 1, 'bl7-selftest', TIMESTAMP '2026-10-24 23:58:00', TIMESTAMP '2026-10-24 23:58:00', 0, 0)"
        % (FLOW_NO, AMOUNT, bal), write=True)
    # 外部到账：txn_time 2026-10-25 00:03，账单业务日仍记 2026-10-24
    one("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id, direction, amount, fee, txn_time, business_date, dedupe_basis, raw_hash, create_user_id, create_time, update_time, deleted) "
        "VALUES (0, 24, 'BL7TEST', '%s', 1, %d, 0, TIMESTAMP '2026-10-25 00:03:00', DATE '2026-10-24', 'BL7-E21', 'bl7', 1, NOW(), NOW(), 0)"
        % (EXT_NO, AMOUNT), write=True)

    print("== 夹具 ==")
    print("  内部: flow_no=%s amount=%d business_date=%s create_time=%s" % (
        FLOW_NO, AMOUNT, one("select business_date from fund_flows where flow_no='%s'" % FLOW_NO)[0],
        one("select create_time from fund_flows where flow_no='%s'" % FLOW_NO)[0]))
    print("  外部: external_txn_id=%s amount=%d business_date=%s txn_time=%s" % (
        EXT_NO, AMOUNT, one("select business_date from bank_transactions where external_txn_id='%s'" % EXT_NO)[0],
        one("select txn_time from bank_transactions where external_txn_id='%s'" % EXT_NO)[0]))

    st, body = call("POST", "/v1/finance/recon-runs", {"accountId": 24, "businessDate": "2026-10-24", "force": True})
    d = (body or {}).get("data") or {}
    run = d.get("run") or d
    run_id = run.get("runId") or run.get("id") or d.get("runId")
    print("  跑批响应: http=%s code=%s run=%s" % (st, (body or {}).get("code"), json.dumps(d, ensure_ascii=False)[:220]))
    check("勾稽跑批成功（单次）", (body or {}).get("code") in (0, 200) and run_id, "runId=%s" % run_id)

    item = sql("select item_id, side, subject_status, match_level, matched_ref from recon_items where run_id=%s and (internal_ref='%s' or external_ref='%s')"
               % (run_id, FLOW_NO, EXT_NO)) if run_id else []
    print("  recon_items: %s" % (item,))
    diff = sql("select difference_id, difference_type, amount_internal, amount_external, diff_amount, status from recon_differences where run_id=%s and (internal_ref='%s' or external_ref='%s')"
               % (run_id, FLOW_NO, EXT_NO)) if run_id else []
    print("  recon_differences: %s" % (diff,))

    check("内部 SALE + 外部到账 均被置 MATCHED",
          len(item) == 2 and all(i[2] in ("MATCHED", "AUTO_MATCHED") for i in item), "items=%s" % (item,))
    check("生成 difference_type = TIME_DIFF（金额差 0）",
          len(diff) == 1 and diff[0][1] == "TIME_DIFF" and diff[0][4] == "0", "diffs=%s" % (diff,))
    check("未落 AMBIGUOUS / ONE_SIDED",
          all(i[2] not in ("UNMATCHED",) for i in item) and (not diff or diff[0][1] == "TIME_DIFF"),
          "subject_status=%s difference_type=%s" % ([i[2] for i in item], [d2[1] for d2 in diff]))

    # 收尾：软删夹具，保持 A7/A1 干净（recon 结果保留为证据）
    one("DELETE FROM fund_flows WHERE flow_no='%s'" % FLOW_NO, write=True)
    one("DELETE FROM bank_transactions WHERE external_txn_id='%s'" % EXT_NO, write=True)
    print("\n== 汇总：%d 项，%d PASS / %d FAIL ==" % (len(RES), sum(1 for r in RES if r[1]), sum(1 for r in RES if not r[1])))
    return 0 if all(r[1] for r in RES) else 1

sys.exit(main())


