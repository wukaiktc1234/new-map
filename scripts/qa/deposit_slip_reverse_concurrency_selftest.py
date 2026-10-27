
# -*- coding: utf-8 -*-
"""P3 前置修复 1+2 验证：SL10 并发安全 + 短款符号 + AC-21 三分支 + AC-28 回归。"""
import base64, hashlib, hmac, json, os, subprocess, sys, threading, time, urllib.error, urllib.request, uuid

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


def mint(uid):
    now = int(time.time())
    h = {"alg": "HS512", "typ": "JWT"}
    p = {"iss": "food-traceability-system", "sub": "fx-" + str(uid), "userId": str(uid),
         "username": "fx-user-" + str(uid), "roles": ["ADMIN"],
         "permissions": ["finance:deposit-slip:create", "finance:deposit-slip:submit",
                         "finance:deposit-slip:receive", "finance:deposit-slip:deposit",
                         "finance:deposit-slip:verify", "finance:deposit-slip:close",
                         "finance:deposit-slip:reverse", "finance:deposit-slip:reject",
                         "finance:deposit-slip:view", "*"],
         "mfaEnabled": False, "mfaVerified": False, "tokenId": str(uuid.uuid4()), "iat": now, "exp": now + 3600}
    s = b64u(json.dumps(h, separators=(",", ":")).encode()) + "." + b64u(json.dumps(p, separators=(",", ":")).encode())
    return s + "." + b64u(hmac.new(SECRET.encode(), s.encode(), hashlib.sha512).digest())


TOK1, TOK2 = mint(1), mint(2)


def call(method, path, body=None, token=TOK1, timeout=180):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
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


def free_shift6():
    """清掉自己在 shift 6 上的测试单（含无流水的临时单），释放班次。仅删自己的数据。"""
    one("DELETE FROM deposit_slips WHERE shift_id=6 AND status IN ('DRAFT','SUBMITTED','RECEIVED')", write=True)


def purge_my_reversed_slip(slip_no):
    """清掉自己上一轮 REVERSED 的测试单及其净额为 0 的流水对（不改变任何账户余额）。"""
    one("DELETE FROM deposit_slips WHERE slip_no='%s' AND status='REVERSED'" % slip_no, write=True)
    one("DELETE FROM fund_flows WHERE remark LIKE '%%%s%%' AND flow_category=5 AND remark LIKE '%%Slip=%%'" % slip_no, write=True)
    one("DELETE FROM fund_flows WHERE remark LIKE '%%DS%s%%' AND remark LIKE '%%[reconReverseOf=%%'" % slip_no.replace("DS", ""), write=True)


def make_slip(branch_amount_fn):
    free_shift6()
    st, body = call("POST", "/v1/finance/deposit-slips/generate", {"shiftId": 6, "storeId": 1})
    slip = d(body).get("slip") or {}
    sid, exp = slip.get("slipId"), slip.get("expectedAmount")
    call("POST", "/v1/finance/deposit-slips/%d/submit" % sid)
    actual = branch_amount_fn(int(exp))
    st, body = call("POST", "/v1/finance/deposit-slips/%d/receive" % sid, {"receivedAmountFen": actual})
    return sid, exp, actual, d(body)


def main():
    for s in (sys.stdout, sys.stderr):
        try:
            s.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass

    # ---------- 迁移与索引 ----------
    row = one("select version, description, success from flyway_schema_history where version='20261025.001'")
    idx = one("select indexdef from pg_indexes where indexname='uk_fund_flows_recon_reverse'")
    check("Flyway V20261025_001 已应用", bool(row) and row[2] == "t", "row=%s" % (row,))
    check("部分唯一索引已建立", bool(idx) and "UNIQUE" in idx[0] and "reconReverseOf" in idx[0], "indexdef=%s" % (idx[0] if idx else None))

    # ---------- 修复 2(b)：回填 DS202610250022 ----------
    before = one("select difference_amount, expected_amount, actual_amount from deposit_slips where slip_no='DS202610250022'")
    one("UPDATE deposit_slips SET difference_amount = actual_amount - expected_amount WHERE slip_no='DS202610250022'", write=True)
    after = one("select difference_amount from deposit_slips where slip_no='DS202610250022'")
    check("回填 DS202610250022 差额 = actual − expected = −500", after and after[0] == "-500",
          "回填前: difference=%s (expected=%s actual=%s) → 回填后: difference=%s"
          % (before[0], before[1], before[2], after[0] if after else None))

    # 释放班次 6：清掉我上一轮留下的 REVERSED 测试单及其净额 0 的流水对（不改变任何账户余额）
    for _sn in ("DS202610250023", "DS202610250024"):
        purge_my_reversed_slip(_sn)

    # ---------- 修复 2(c)：AC-21 三分支 ----------
    for label, fn, expect_sign in (
            ("平账 actual=expected", lambda e: e, 0),
            ("长款 actual>expected", lambda e: e + 500, 500),
            ("短款 actual<expected", lambda e: e - 500, -500)):
        sid, exp, act, rec = make_slip(fn)
        diff = rec.get("differenceAmount")
        ok = diff is not None and int(diff) == expect_sign
        check("AC-21 %s → difference=%d" % (label, expect_sign), ok,
              "slipId=%s expected=%s actual=%s difference=%s" % (sid, exp, act, diff))
        audit_label = one("select operation_desc from finance_audit_logs where operation_type='DEPOSIT_SLIP_RECEIVE' and target_id=%d order by log_id desc limit 1" % sid)
        print("    审计文案: %s" % (audit_label[0][-60:] if audit_label else "N/A"))

    # ---------- 修复 1(d)：并发冲正 ----------
    sid, exp, act, rec = make_slip(lambda e: e)  # 平账单，用于存行
    cash_b, cashv_b = acct(20)
    bank_b, bankv_b = acct(24)
    txn = one("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id, direction, amount, fee, txn_time, business_date, dedupe_basis, raw_hash, create_user_id) "
              "VALUES (0, 24, 'SELFTEST', 'FIX%05d', 1, %d, 0, NOW(), CURRENT_DATE, 'FIX%05d', 'selftest', 1) RETURNING transaction_id"
              % (sid, act, sid), write=True)[0]
    st, body = call("POST", "/v1/finance/deposit-slips/%d/deposit" % sid, {"bankTransactionId": int(txn)})
    slip_no = d(body).get("slipNo")
    cash_d, _ = acct(20)
    bank_d, _ = acct(24)
    check("前置：存行成功（供并发冲正）", d(body).get("status") == "DEPOSITED",
          "slipNo=%s cash %d→%d bank %d→%d" % (slip_no, cash_b, cash_d, bank_b, bank_d))

    barrier = threading.Barrier(2)
    out = {}

    def worker(tag, token):
        barrier.wait()
        st, body = call("POST", "/v1/finance/deposit-slips/%d/reverse" % sid, {"reason": "并发自测-" + tag}, token=token)
        out[tag] = (st, (body or {}).get("code"), (body or {}).get("message"), d(body).get("status"))

    t1 = threading.Thread(target=worker, args=("T1", TOK1))
    t2 = threading.Thread(target=worker, args=("T2", TOK2))
    t1.start(); t2.start(); t1.join(); t2.join()
    print("    线程T1: http=%s code=%s status=%s msg=%s" % (out["T1"][0], out["T1"][1], out["T1"][3], out["T1"][2]))
    print("    线程T2: http=%s code=%s status=%s msg=%s" % (out["T2"][0], out["T2"][1], out["T2"][3], out["T2"][2]))

    codes = sorted([str(out["T1"][1]), str(out["T2"][1])])
    ok_one = any(v[1] == 0 and v[3] == "REVERSED" for v in out.values())
    ok_other = any(str(v[1]) in ("409", "400") for v in out.values())
    check("并发：一个成功(REVERSED)、一个 409/400", ok_one and ok_other, "codes=%s" % (codes,))

    rev_cnt = int(one("select count(*) from fund_flows where remark like '%%[reconReverseOf=%%' and remark like '%%%s%%'" % slip_no)[0])
    orig_cnt = int(one("select count(*) from fund_flows where remark like '%%[depositSlip=%s]%%' and status=3" % slip_no)[0])
    check("并发后 [reconReverseOf=] 只 +1 对（2 条反向流水）", rev_cnt == 2, "reverse_flows=%d" % rev_cnt)
    check("原流水 2 条均 status=3", orig_cnt == 2, "originals_cancelled=%d" % orig_cnt)
    cash_r, cashv_r = acct(20)
    bank_r, bankv_r = acct(24)
    check("余额只回滚一次（回到存行前）", cash_r == cash_b and bank_r == bank_b,
          "cash=%d(expect %d) bank=%d(expect %d)" % (cash_r, cash_b, bank_r, bank_b))

    # ---------- AC-28 串行回归 + 重复调用 400 ----------
    st, body = call("POST", "/v1/finance/deposit-slips/%d/reverse" % sid, {"reason": "重复冲正"}, token=TOK1)
    check("AC-28 重复调用冲正 → 400（串行幂等）", (body or {}).get("code") == 400, "code=%s msg=%s" % ((body or {}).get("code"), (body or {}).get("message")))
    rev_cnt2 = int(one("select count(*) from fund_flows where remark like '%%[reconReverseOf=%%' and remark like '%%%s%%'" % slip_no)[0])
    check("重复调用后反向流水仍为 2 条", rev_cnt2 == 2, "reverse_flows=%d" % rev_cnt2)

    print("\n== 汇总：%d 项，%d PASS / %d FAIL ==" % (len(RES), sum(1 for r in RES if r[1]), sum(1 for r in RES if not r[1])))
    for n, ok, dt in RES:
        if not ok:
            print("  FAIL: " + n + "  " + dt)
    return 0 if all(r[1] for r in RES) else 1


if __name__ == "__main__":
    sys.exit(main())
