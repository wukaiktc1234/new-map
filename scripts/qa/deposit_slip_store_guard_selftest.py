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
"""P3 前置：SL1 storeId 一致性校验（3 组）+ SL6/SL10 回归（generate/幂等/submit/receive/deposit/reverse）。"""
import base64, hashlib, hmac, json, os, subprocess, sys, time, urllib.error, urllib.request, uuid

BASE = os.environ.get("G_BASE", "http://127.0.0.1:18080/api")
PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
SECRET = "dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security"
PGPASSWORD, PGDB = "123456", "food_traceability"
RES = []


def check(name, ok, detail=""):
    RES.append((name, ok, detail))
    print(("[PASS] " if ok else "[FAIL] ") + name + ("  " + detail if detail else ""))


def b64u(b):
    return base64.urlsafe_b64encode(b).rstrip(b"=").decode("ascii")


def mint(uid):
    now = int(time.time())
    h = {"alg": "HS512", "typ": "JWT"}
    p = {"iss": "food-traceability-system", "sub": "p3-" + str(uid), "userId": str(uid),
         "username": "p3-user-" + str(uid), "roles": ["ADMIN"],
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
        with urllib.request.urlopen(req, timeout=120) as r:
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


def main():
    for s in (sys.stdout, sys.stderr):
        try:
            s.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass

    # 释放测试班次 6（清掉自己上一轮 REJECTED 的 DS…017，不动他人数据）
    one("DELETE FROM deposit_slips WHERE slip_id=17 AND shift_id=6 AND status='REJECTED'", write=True)
    print("freed shift 6 (deleted my REJECTED slip 17)")

    # ---------- 测试 2：显式正确 storeId → 成功生成 ----------
    st, body = call("POST", "/v1/finance/deposit-slips/generate", {"shiftId": 6, "storeId": 1})
    d2 = d(body)
    slip = d2.get("slip") or {}
    check("测试2 显式正确 storeId=1 → 成功", st == 200 and slip.get("slipId") is not None,
          "reused=%s slipNo=%s expected_amount=%s" % (d2.get("reused"), slip.get("slipNo"), slip.get("expectedAmount")))
    slip_id, slip_no = slip.get("slipId"), slip.get("slipNo")
    expected = slip.get("expectedAmount")

    # ---------- 测试 1：跨店班次 + 错误 storeId → 400 ----------
    st, body = call("POST", "/v1/finance/deposit-slips/generate", {"shiftId": 58, "storeId": 999})
    msg1 = (body or {}).get("message", "")
    check("测试1 跨店班次 + 错误 storeId → 400 并列冲突门店", (body or {}).get("code") == 400 and "999" in msg1,
          "\n    原文: " + msg1)

    # ---------- 测试 3：跨店班次 + 不传 storeId → 400 并列候选 ----------
    st, body = call("POST", "/v1/finance/deposit-slips/generate", {"shiftId": 58})
    msg3 = (body or {}).get("message", "")
    check("测试3 不传 storeId 且推导不唯一 → 400 并列候选", (body or {}).get("code") == 400 and "[1, 2]" in msg3,
          "\n    原文: " + msg3)

    # ---------- 回归 5 项 ----------
    st, body = call("POST", "/v1/finance/deposit-slips/generate", {"shiftId": 6, "storeId": 1})
    check("回归 generate 幂等 reused=true", d(body).get("reused") is True, "slipId=%s" % (d(body).get("slip") or {}).get("slipId"))
    st, body = call("POST", "/v1/finance/deposit-slips/%d/submit" % slip_id)
    check("回归 submit → SUBMITTED", d(body).get("status") == "SUBMITTED", "status=%s" % d(body).get("status"))
    flows_before = int(one("select count(*) from fund_flows")[0])
    actual = int(expected) - 100
    st, body = call("POST", "/v1/finance/deposit-slips/%d/receive" % slip_id, {"receivedAmountFen": actual})
    flows_after_recv = int(one("select count(*) from fund_flows")[0])
    check("回归 receive → RECEIVED 且不生成流水", d(body).get("status") == "RECEIVED" and flows_after_recv == flows_before,
          "status=%s flows=%d→%d diff=%s" % (d(body).get("status"), flows_before, flows_after_recv, d(body).get("differenceAmount")))

    cash_b, cashv_b = acct(20)
    bank_b, bankv_b = acct(24)
    txn = one("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id, direction, amount, fee, txn_time, business_date, dedupe_basis, raw_hash, create_user_id) "
              "VALUES (0, 24, 'SELFTEST', 'SELFTEST-P3-%d', 1, %d, 0, NOW(), CURRENT_DATE, 'SELFTEST-P3-%d', 'selftest', 1) RETURNING transaction_id"
              % (slip_id, actual, slip_id), write=True)[0]
    st, body = call("POST", "/v1/finance/deposit-slips/%d/deposit" % slip_id, {"bankTransactionId": int(txn)})
    flows_after_dep = int(one("select count(*) from fund_flows")[0])
    cash_a, cashv_a = acct(20)
    bank_a, bankv_a = acct(24)
    check("回归 deposit → DEPOSITED 且 +2 流水", d(body).get("status") == "DEPOSITED" and flows_after_dep - flows_after_recv == 2,
          "status=%s flows=%d→%d" % (d(body).get("status"), flows_after_recv, flows_after_dep))
    check("回归 余额/version 正确（现金 -actual / 银行 +actual）",
          cash_a == cash_b - actual and cashv_a == cashv_b + 1 and bank_a == bank_b + actual and bankv_a == bankv_b + 1,
          "cash %d→%d v%d→v%d | bank %d→%d v%d→v%d" % (cash_b, cash_a, cashv_b, cashv_a, bank_b, bank_a, bankv_b, bankv_a))

    # SL10 冲正回滚（lead 要求：产生的存行必须走冲正还原）
    st, body = call("POST", "/v1/finance/deposit-slips/%d/reverse" % slip_id, {"reason": "P3 前置自测回滚"}, token=TOK2)
    cash_r, cashv_r = acct(20)
    bank_r, bankv_r = acct(24)
    orig = sql("select status from fund_flows where remark like '%%[depositSlip=%s]%%'" % slip_no)
    check("回归 reverse → REVERSED 且余额回滚", d(body).get("status") == "REVERSED" and cash_r == cash_b and bank_r == bank_b,
          "status=%s cash=%d(expect %d) bank=%d(expect %d)" % (d(body).get("status"), cash_r, cash_b, bank_r, bank_b))
    check("回归 原流水 status=3", bool(orig) and all(o[0] == "3" for o in orig), "orig=%s" % (orig,))

    print("\n== 汇总：%d 项，%d PASS / %d FAIL ==" % (len(RES), sum(1 for r in RES if r[1]), sum(1 for r in RES if not r[1])))
    for n, ok, dt in RES:
        if not ok:
            print("  FAIL: " + n + "  " + dt)
    return 0 if all(r[1] for r in RES) else 1


if __name__ == "__main__":
    sys.exit(main())


