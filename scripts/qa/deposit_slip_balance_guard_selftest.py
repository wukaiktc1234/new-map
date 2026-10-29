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
"""P3 收口-1 / BL-4：SL6 现金户实时余额校验（不足 → 409；刚好够 → 200）。"""
import base64, hashlib, hmac, json, os, subprocess, sys, time, urllib.error, urllib.request, uuid

BASE = "http://127.0.0.1:18080/api"
PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
SECRET = "dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security"
PGPASSWORD, PGDB = "123456", "food_traceability"
STORE = 99001
RES = []


def check(name, ok, detail=""):
    RES.append((name, ok, detail))
    print(("[PASS] " if ok else "[FAIL] ") + name + ("\n    " + detail if detail else ""))


def b64u(b):
    return base64.urlsafe_b64encode(b).rstrip(b"=").decode("ascii")


def mint(uid):
    now = int(time.time())
    h = {"alg": "HS512", "typ": "JWT"}
    p = {"iss": "food-traceability-system", "sub": "bl4-" + str(uid), "userId": str(uid),
         "username": "bl4-" + str(uid), "roles": ["ADMIN"],
         "permissions": ["finance:deposit-slip:create", "finance:deposit-slip:submit",
                         "finance:deposit-slip:receive", "finance:deposit-slip:deposit",
                         "finance:deposit-slip:verify", "finance:deposit-slip:close",
                         "finance:deposit-slip:reverse", "finance:deposit-slip:reject",
                         "finance:deposit-slip:view", "*"],
         "mfaEnabled": False, "mfaVerified": False, "tokenId": str(uuid.uuid4()), "iat": now, "exp": now + 3600}
    s = b64u(json.dumps(h, separators=(",", ":")).encode()) + "." + b64u(json.dumps(p, separators=(",", ":")).encode())
    return s + "." + b64u(hmac.new(SECRET.encode(), s.encode(), hashlib.sha512).digest())


TOK1, TOK2 = mint(1), mint(2)


def call(method, path, body=None, token=TOK1):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    req.add_header("Authorization", "Bearer " + token)
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
    p = subprocess.run([PSQL, "-X", "-q", "-t", "-A", "-F", "|", "-h", "localhost", "-p", "5432",
                        "-U", "postgres", "-d", PGDB, "-c", ("" if write else "SET TRANSACTION READ ONLY; ") + q],
                       capture_output=True, text=True, encoding="utf-8", env=env)
    if p.returncode != 0:
        raise RuntimeError(p.stderr.strip())
    return [l.split("|") for l in p.stdout.splitlines() if l.strip()]


def one(q, write=False):
    r = sql(q, write)
    return r[0] if r else []


def d(b):
    return (b or {}).get("data") or {}


def acct(i):
    r = one("select coalesce(balance,0), coalesce(version,0) from bank_accounts where account_id=%d" % i)
    return int(r[0]), int(r[1])


def flows():
    return int(one("select count(*) from fund_flows where deleted=0")[0])


def setup():
    # 清理旧夹具
    one("DELETE FROM fund_flows WHERE account_id IN (SELECT account_id FROM bank_accounts WHERE store_id=%d)" % STORE, write=True)
    one("DELETE FROM deposit_slips WHERE store_id=%d" % STORE, write=True)
    one("DELETE FROM bank_transactions WHERE source_type='BL4TEST'", write=True)
    one("DELETE FROM bank_accounts WHERE store_id=%d" % STORE, write=True)
    cash = one("INSERT INTO bank_accounts (account_name, account_number, account_type, store_id, balance, version, status, scope, created_by, create_time, update_time, deleted) "
               "VALUES ('BL4-TEST-CASH', 'BL4-CASH', 2, %d, 34200, 0, 1, 'GLOBAL', 'bl4-selftest', NOW(), NOW(), 0) RETURNING account_id" % STORE, write=True)[0]
    bank = one("INSERT INTO bank_accounts (account_name, account_number, account_type, store_id, balance, version, status, scope, created_by, create_time, update_time, deleted) "
               "VALUES ('BL4-TEST-BANK', 'BL4-BANK', 1, %d, 0, 0, 1, 'GLOBAL', 'bl4-selftest', NOW(), NOW(), 0) RETURNING account_id" % STORE, write=True)[0]
    return int(cash), int(bank)


def make_slip(slip_no, shift_id, expected, actual):
    r = one("INSERT INTO deposit_slips (slip_no, store_id, shift_id, expected_amount, actual_amount, difference_amount, status, collector_user_id, collector_name, create_user_id, created_by, create_time, update_time, version, deleted) "
            "VALUES ('%s', %d, %d, %d, %d, %d, 'RECEIVED', 1, 'bl4-1', 1, 'bl4-selftest', NOW(), NOW(), 0, 0) RETURNING slip_id"
            % (slip_no, STORE, shift_id, expected, actual, actual - expected), write=True)[0]
    return int(r)


def make_txn(account_id, amount, tag):
    return int(one("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id, direction, amount, fee, txn_time, business_date, dedupe_basis, raw_hash, create_user_id) "
                   "VALUES (0, %d, 'BL4TEST', '%s', 1, %d, 0, NOW(), CURRENT_DATE, '%s', 'bl4', 1) RETURNING transaction_id"
                   % (account_id, tag, amount, tag), write=True)[0])


def main():
    for s in (sys.stdout, sys.stderr):
        try:
            s.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass

    cash_id, bank_id = setup()
    print("fixtures: store=%d cash=%d(balance 34200) bank=%d(balance 0)" % (STORE, cash_id, bank_id))

    # ---------- 测试 1：余额不足 ----------
    slip_a = make_slip("DSBL4A0001", 99001, 34200, 40000)
    txn_a = make_txn(bank_id, 40000, "BL4-A")
    f0 = flows()
    st, body = call("POST", "/v1/finance/deposit-slips/%d/deposit" % slip_a, {"bankTransactionId": txn_a})
    msg = (body or {}).get("message", "")
    cash_after, _ = acct(cash_id)
    bank_after, _ = acct(bank_id)
    f1 = flows()
    status_a = one("select status from deposit_slips where slip_id=%d" % slip_a)[0]
    check("测试1 余额不足（34200 < 40000）→ 409 且文案含三个数字",
          str((body or {}).get("code")) == "409" and "34200" in msg and "40000" in msg and "5800" in msg,
          "code=%s\n    原文: %s" % ((body or {}).get("code"), msg))
    check("测试1 余额不变、无流水、单据仍 RECEIVED",
          cash_after == 34200 and bank_after == 0 and f1 == f0 and status_a == "RECEIVED",
          "现金户=%d 银行户=%d fund_flows=%d→%d slip.status=%s" % (cash_after, bank_after, f0, f1, status_a))

    # ---------- 测试 2：刚好够 ----------
    slip_b = make_slip("DSBL4B0001", 99002, 34200, 34200)
    txn_b = make_txn(bank_id, 34200, "BL4-B")
    f_before = flows()
    st, body = call("POST", "/v1/finance/deposit-slips/%d/deposit" % slip_b, {"bankTransactionId": txn_b})
    cash_d, cashv_d = acct(cash_id)
    bank_d, bankv_d = acct(bank_id)
    f_after = flows()
    check("测试2 刚好够（34200 = 34200）→ 200 成功", d(body).get("status") == "DEPOSITED",
          "status=%s slipNo=%s" % (d(body).get("status"), d(body).get("slipNo")))
    check("测试2 现金户余额归零、银行户 +34200、流水 +2",
          cash_d == 0 and bank_d == 34200 and f_after - f_before == 2,
          "现金户 34200→%d(v%d→v%d) 银行户 0→%d(v%d→v%d) fund_flows %d→%d"
          % (cash_d, 0, cashv_d, bank_d, 0, bankv_d, f_before, f_after))

    st, body = call("POST", "/v1/finance/deposit-slips/%d/reverse" % slip_b, {"reason": "BL-4 自测回滚"}, token=TOK2)
    cash_r, _ = acct(cash_id)
    bank_r, _ = acct(bank_id)
    orig = sql("select status from fund_flows where remark like '%%DSBL4B0001%%' and remark like '%%[depositSlip=%%'")
    check("测试2 随后的 SL10 冲正回滚：现金户 0→34200、银行户 34200→0",
          d(body).get("status") == "REVERSED" and cash_r == 34200 and bank_r == 0,
          "status=%s 现金户=%d 银行户=%d" % (d(body).get("status"), cash_r, bank_r))
    check("测试2 原流水 status=3", bool(orig) and all(o[0] == "3" for o in orig), "orig=%s" % (orig,))

    print("\n== 汇总：%d 项，%d PASS / %d FAIL ==" % (len(RES), sum(1 for r in RES if r[1]), sum(1 for r in RES if not r[1])))
    for n, ok, dt in RES:
        if not ok:
            print("  FAIL: " + n + "  " + dt)
    return 0 if all(r[1] for r in RES) else 1


if __name__ == "__main__":
    sys.exit(main())


