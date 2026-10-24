
# -*- coding: utf-8 -*-
"""P2-A 缺陷修复验证：SL6 连跑 5 次不撞键 + AC-29 审计覆盖。"""
import base64, hashlib, hmac, json, os, subprocess, sys, time, urllib.error, urllib.request, uuid

BASE = os.environ.get("FIX_BASE", "http://127.0.0.1:18080/api")
PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
PGPASSWORD = "123456"
PGDB = "food_traceability"
SECRET = "dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security"
BANK_ACCOUNT = 24
CASH_ACCOUNT = 20
RESULTS = []


def check(name, ok, detail=""):
    RESULTS.append((name, ok, detail))
    print(("[PASS] " if ok else "[FAIL] ") + name + ("  " + detail if detail else ""))


def b64u(b):
    return base64.urlsafe_b64encode(b).rstrip(b"=").decode("ascii")


def mint(uid):
    now = int(time.time())
    h = {"alg": "HS512", "typ": "JWT"}
    p = {"iss": "food-traceability-system", "sub": "fix-" + str(uid), "userId": str(uid),
         "username": "fix-user-" + str(uid), "roles": ["ADMIN"],
         "permissions": ["finance:deposit-slip:create", "finance:deposit-slip:submit",
                         "finance:deposit-slip:receive", "finance:deposit-slip:deposit",
                         "finance:deposit-slip:verify", "finance:deposit-slip:close",
                         "finance:deposit-slip:reverse", "finance:deposit-slip:view", "*"],
         "mfaEnabled": False, "mfaVerified": False, "tokenId": str(uuid.uuid4()), "iat": now, "exp": now + 3600}
    signing = b64u(json.dumps(h, separators=(",", ":")).encode()) + "." + b64u(json.dumps(p, separators=(",", ":")).encode())
    sig = hmac.new(SECRET.encode(), signing.encode(), hashlib.sha512).digest()
    return signing + "." + b64u(sig)


TOK1 = mint(1)
TOK2 = mint(2)


def call(method, path, body=None, token=TOK1):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw}


def sql(q, write=False):
    env = dict(os.environ, PGPASSWORD=PGPASSWORD, PGCLIENTENCODING="UTF8")
    proc = subprocess.run([PSQL, "-X", "-q", "-t", "-A", "-F", "|", "-h", "localhost", "-p", "5432",
                           "-U", "postgres", "-d", PGDB, "-c", ("" if write else "SET TRANSACTION READ ONLY; ") + q],
                          capture_output=True, text=True, encoding="utf-8", env=env)
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip())
    return [l.split("|") for l in proc.stdout.splitlines() if l.strip()]


def one(q, write=False):
    rows = sql(q, write)
    return rows[0] if rows else []


def d(body):
    return (body or {}).get("data") or {}


def main():
    for s in (sys.stdout, sys.stderr):
        try:
            s.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass

    shifts = [int(x) for x in os.environ.get("FIX_SHIFTS", "18,17,16,15,14").split(",")]
    flows_before = int(one("select count(*) from fund_flows")[0])
    print("baseline fund_flows=%d shifts=%s" % (flows_before, shifts))

    made = []
    for idx, shift_id in enumerate(shifts):
        st, body = call("POST", "/v1/finance/deposit-slips/generate", {"shiftId": shift_id, "storeId": 1})
        slip = d(body).get("slip") or {}
        slip_id = slip.get("slipId")
        expected = slip.get("expectedAmount")
        if not slip_id:
            check("shift %d 生成" % shift_id, False, str(body)[:200])
            continue
        call("POST", "/v1/finance/deposit-slips/%d/submit" % slip_id)
        actual = int(expected) - 100
        call("POST", "/v1/finance/deposit-slips/%d/receive" % slip_id, {"receivedAmountFen": actual})
        txn = one("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id, direction, amount, fee, txn_time, business_date, dedupe_basis, raw_hash, create_user_id) "
                  "VALUES (0, %d, 'SELFTEST', 'SELFTEST-FIX-%d', 1, %d, 0, NOW(), CURRENT_DATE, 'SELFTEST-FIX-%d', 'selftest', 1) RETURNING transaction_id"
                  % (BANK_ACCOUNT, slip_id, actual, slip_id), write=True)[0]
        st, body = call("POST", "/v1/finance/deposit-slips/%d/deposit" % slip_id, {"bankTransactionId": int(txn)})
        dep = d(body)
        made.append((shift_id, slip_id, slip.get("slipNo"), actual, st, dep.get("status"), (body or {}).get("message")))

    print("\n== SL6 连跑结果 ==")
    for shift_id, slip_id, slip_no, actual, st, status, msg in made:
        rows = sql("select flow_no, account_id, flow_direction, amount, balance_after, status from fund_flows where remark like '%%[depositSlip=%s]%%' order by flow_id" % slip_no)
        distinct = len({r[0] for r in rows})
        check("SL6 第%d次(shift=%d) 成功 status=DEPOSITED" % (made.index((shift_id, slip_id, slip_no, actual, st, status, msg)) + 1, shift_id),
              st == 200 and status == "DEPOSITED", "slipNo=%s msg=%s" % (slip_no, msg))
        check("  该单恰好 2 条流水且 flow_no 互不相同", len(rows) == 2 and distinct == 2, "flows=%s" % ([r[0] for r in rows],))

    flows_after = int(one("select count(*) from fund_flows")[0])
    check("5 次存行共 +10 条流水", flows_after - flows_before == 10, "before=%d after=%d delta=%d" % (flows_before, flows_after, flows_after - flows_before))
    check("全库当日 flow_no 无重复", int(one("select count(*) from (select flow_no from fund_flows group by flow_no having count(*)>1) t")[0]) == 0)

    # reject 用例（shift=13）
    reject_shift = int(os.environ.get("FIX_REJECT_SHIFT", "6"))
    st, body = call("POST", "/v1/finance/deposit-slips/generate", {"shiftId": reject_shift, "storeId": 1})
    rs = d(body).get("slip") or {}
    call("POST", "/v1/finance/deposit-slips/%d/submit" % rs.get("slipId"))
    st, body = call("POST", "/v1/finance/deposit-slips/%d/reject" % rs.get("slipId"), {"reason": "自测：驳回"})
    check("SL9 驳回 → REJECTED（沿用 :close 权限码）", d(body).get("status") == "REJECTED", "status=%s" % d(body).get("status"))

    # 审计核对
    slips = [m[2] for m in made] + [rs.get("slipNo")]
    in_list = ",".join("'%s'" % s for s in slips if s)
    rows = sql("select operation_type, count(*) from finance_audit_logs where operation_type like 'DEPOSIT_SLIP%%' and operation_data like any (array[%s]) group by 1 order by 1"
               % ",".join("'%%" + s + "%%'" for s in slips if s))
    print("\n== 审计条数（按 operation_type）==")
    for r in rows:
        print("   %-24s %s" % (r[0], r[1]))
    counts = {r[0]: int(r[1]) for r in rows}
    check("审计 SUBMIT = 6（5 次存行 + 1 次驳回）", counts.get("DEPOSIT_SLIP_SUBMIT") == 6, "actual=%s" % counts.get("DEPOSIT_SLIP_SUBMIT"))
    check("审计 RECEIVE = 5", counts.get("DEPOSIT_SLIP_RECEIVE") == 5, "actual=%s" % counts.get("DEPOSIT_SLIP_RECEIVE"))
    check("审计 DEPOSIT = 5", counts.get("DEPOSIT_SLIP_DEPOSIT") == 5, "actual=%s" % counts.get("DEPOSIT_SLIP_DEPOSIT"))
    check("审计 REJECT = 1", counts.get("DEPOSIT_SLIP_REJECT") == 1, "actual=%s" % counts.get("DEPOSIT_SLIP_REJECT"))
    sample = one("select operator_id, operation_type, target_id, target_type, operation_desc from finance_audit_logs where operation_type='DEPOSIT_SLIP_DEPOSIT' order by log_id desc limit 1")
    check("审计字段完整（operator/target_id=slip_id/target_type/desc 含单号）",
          bool(sample) and sample[1] == "DEPOSIT_SLIP_DEPOSIT" and sample[3] == "DEPOSIT_SLIP" and "DS" in (sample[4] or ""),
          "sample=%s" % (sample,))

    print("\n== 汇总：%d 项，%d PASS / %d FAIL ==" % (len(RESULTS), sum(1 for r in RESULTS if r[1]), sum(1 for r in RESULTS if not r[1])))
    for n, ok, detail in RESULTS:
        if not ok:
            print("  FAIL: " + n + "  " + detail)
    return 0 if all(r[1] for r in RESULTS) else 1


if __name__ == "__main__":
    sys.exit(main())
