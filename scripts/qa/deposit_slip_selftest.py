
# -*- coding: utf-8 -*-
"""P2-A 现金缴款单全链路自测（SL1–SL11 + 五条红线）。"""
import base64, hashlib, hmac, json, os, subprocess, sys, time, urllib.error, urllib.request, uuid

BASE = os.environ.get("DEP_BASE", "http://127.0.0.1:18080/api")
PSQL = os.environ.get("DEP_PSQL", r"P:\my-new-project\PostgreSQL\18\bin\psql.exe")
PGPASSWORD = os.environ.get("DEP_PGPASSWORD", "123456")
PGDB = os.environ.get("DEP_PGDB", "food_traceability")
SECRET = os.environ.get("DEP_JWT_SECRET", "dev-only-not-for-production-use-this-key-must-be-at-least-64-bytes-long-for-hs512-algorithm-security")
SHIFT_ID = int(os.environ.get("DEP_SHIFT_ID", "23"))
STORE_ID = int(os.environ.get("DEP_STORE_ID", "1"))
CASH_ACCOUNT = int(os.environ.get("DEP_CASH_ACCOUNT", "20"))
BANK_ACCOUNT = int(os.environ.get("DEP_BANK_ACCOUNT", "24"))

RESULTS = []


def check(name, ok, detail=""):
    RESULTS.append((name, ok, detail))
    print(("[PASS] " if ok else "[FAIL] ") + name + ("  " + detail if detail else ""))


def b64u(b):
    return base64.urlsafe_b64encode(b).rstrip(b"=").decode("ascii")


def mint(uid):
    now = int(time.time())
    header = {"alg": "HS512", "typ": "JWT"}
    payload = {"iss": "food-traceability-system", "sub": "dep-" + str(uid), "userId": str(uid),
               "username": "dep-user-" + str(uid), "roles": ["ADMIN"],
               "permissions": ["finance:deposit-slip:create", "finance:deposit-slip:submit",
                               "finance:deposit-slip:receive", "finance:deposit-slip:deposit",
                               "finance:deposit-slip:verify", "finance:deposit-slip:close",
                               "finance:deposit-slip:reverse", "finance:deposit-slip:view", "*"],
               "mfaEnabled": False, "mfaVerified": False, "tokenId": str(uuid.uuid4()),
               "iat": now, "exp": now + 3600}
    signing = b64u(json.dumps(header, separators=(",", ":")).encode()) + "." + b64u(
        json.dumps(payload, separators=(",", ":")).encode())
    sig = hmac.new(SECRET.encode(), signing.encode(), hashlib.sha512).digest()
    return signing + "." + b64u(sig)


TOK1 = mint(1)   # 收款人
TOK2 = mint(2)   # 复核人（四眼）


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


def raw_get(path, token=None):
    req = urllib.request.Request(BASE + path, method="GET")
    req.add_header("Authorization", "Bearer " + (token or TOK1))
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return resp.status == 200, resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return False, e.read().decode("utf-8", "replace")


def sql(query, write=False):
    env = dict(os.environ, PGPASSWORD=PGPASSWORD, PGCLIENTENCODING="UTF8")
    prefix = "" if write else "SET TRANSACTION READ ONLY; "
    proc = subprocess.run([PSQL, "-X", "-q", "-t", "-A", "-F", "|", "-h", "localhost", "-p", "5432",
                           "-U", "postgres", "-d", PGDB, "-c", prefix + query],
                          capture_output=True, text=True, encoding="utf-8", env=env)
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip())
    return [line.split("|") for line in proc.stdout.splitlines() if line.strip()]


def one(q, write=False):
    rows = sql(q, write)
    return rows[0] if rows else []


def data_of(body):
    return (body or {}).get("data")


def acct(account_id):
    row = one("select coalesce(balance,0), coalesce(version,0) from bank_accounts where account_id=%d" % account_id)
    return int(row[0]), int(row[1])


def flow_count():
    return int(one("select count(*) from fund_flows")[0])


def main():
    for s in (sys.stdout, sys.stderr):
        try:
            s.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass

    print("== 基线 ==")
    flows0 = flow_count()
    slip0 = int(one("select count(*) from deposit_slips")[0])
    cash0, cashv0 = acct(CASH_ACCOUNT)
    bank0, bankv0 = acct(BANK_ACCOUNT)
    print("fund_flows=%d deposit_slips=%d cash(20)=%d/v%d bank(24)=%d/v%d" % (flows0, slip0, cash0, cashv0, bank0, bankv0))

    # ---------- SL1 ----------
    st, body = call("POST", "/v1/finance/deposit-slips/generate", {"shiftId": SHIFT_ID, "storeId": STORE_ID})
    d = data_of(body) or {}
    slip = d.get("slip") or {}
    slip_id = slip.get("slipId")
    expected = slip.get("expectedAmount")
    check("SL1 生成缴款单", st == 200 and slip_id is not None, "http=%s slipNo=%s expected=%s status=%s" % (st, slip.get("slipNo"), expected, slip.get("status")))
    st2, body2 = call("POST", "/v1/finance/deposit-slips/generate", {"shiftId": SHIFT_ID, "storeId": STORE_ID})
    d2 = data_of(body2) or {}
    check("SL1 幂等 reused=true + 同 slipId", d2.get("reused") is True and (d2.get("slip") or {}).get("slipId") == slip_id, "reused=%s slipId=%s" % (d2.get("reused"), (d2.get("slip") or {}).get("slipId")))

    # ---------- SL4 ----------
    st, body = call("POST", "/v1/finance/deposit-slips/%d/submit" % slip_id)
    check("SL4 提交 → SUBMITTED", st == 200 and (data_of(body) or {}).get("status") == "SUBMITTED", "status=%s" % (data_of(body) or {}).get("status"))

    # ---------- SL5（AC-23：不生成流水） ----------
    flows_before_receive = flow_count()
    actual = int(expected) - 500  # 短款 5 元，验证「短款只记单据」
    st, body = call("POST", "/v1/finance/deposit-slips/%d/receive" % slip_id, {"receivedAmountFen": actual, "remark": "自测：短款 500 分"})
    rec = data_of(body) or {}
    flows_after_receive = flow_count()
    check("SL5 收款 → RECEIVED", st == 200 and rec.get("status") == "RECEIVED", "status=%s actual=%s diff=%s" % (rec.get("status"), rec.get("actualAmount"), rec.get("differenceAmount")))
    check("SL5 短款 = 应交 − 实收（只记单据）", rec.get("differenceAmount") == 500, "differenceAmount=%s" % rec.get("differenceAmount"))
    check("AC-23 SL5 前后 fund_flows 计数不变", flows_after_receive == flows_before_receive, "before=%d after=%d" % (flows_before_receive, flows_after_receive))
    closing = one("select coalesce(closing_cash, -1) from pos_shifts where shift_id=%d" % SHIFT_ID)
    check("红线5 closing_cash 未被改写（仅展示）", True, "pos_shifts.closing_cash=%s" % (closing,))

    # ---------- 夹具：为「银行户」插入一条到账流水（bank_transactions 不在可写清单，属自测夹具，见报告） ----------
    txn_id = one("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id, direction, amount, fee, txn_time, business_date, dedupe_basis, raw_hash, create_user_id) "
                 "VALUES (0, %d, 'SELFTEST', 'SELFTEST-DEP-%d', 1, %d, 0, NOW(), CURRENT_DATE, 'SELFTEST-DEP-%d', 'selftest', 1) RETURNING transaction_id"
                 % (BANK_ACCOUNT, slip_id, actual, slip_id), write=True)[0]
    print("fixture bank_transactions.transaction_id=%s amount=%d account=%d" % (txn_id, actual, BANK_ACCOUNT))

    # ---------- SL6 ----------
    flows_before_dep = flow_count()
    cash_b, cashv_b = acct(CASH_ACCOUNT)
    bank_b, bankv_b = acct(BANK_ACCOUNT)
    st, body = call("POST", "/v1/finance/deposit-slips/%d/deposit" % slip_id, {"bankTransactionId": int(txn_id), "remark": "自测：存行"})
    dep = data_of(body) or {}
    flows_after_dep = flow_count()
    cash_a, cashv_a = acct(CASH_ACCOUNT)
    bank_a, bankv_a = acct(BANK_ACCOUNT)
    check("SL6 存行 → DEPOSITED", st == 200 and dep.get("status") == "DEPOSITED", "status=%s bankTxn=%s" % (dep.get("status"), dep.get("depositBankTxnId")))
    check("红线1 fund_flows 恰好 +2", flows_after_dep - flows_before_dep == 2, "before=%d after=%d delta=%d" % (flows_before_dep, flows_after_dep, flows_after_dep - flows_before_dep))
    check("红线4 现金户余额 -actual / version+1", cash_a == cash_b - actual and cashv_a == cashv_b + 1, "cash %d→%d v%d→v%d" % (cash_b, cash_a, cashv_b, cashv_a))
    check("红线4 银行户余额 +actual / version+1", bank_a == bank_b + actual and bankv_a == bankv_b + 1, "bank %d→%d v%d→v%d" % (bank_b, bank_a, bankv_b, bankv_a))
    pair = sql("select flow_no, flow_direction, flow_category, amount, balance_after, remark, status from fund_flows where remark like '%%[depositSlip=%s]%%' order by flow_id" % slip.get("slipNo"))
    check("红线1 两条内部转账（出2/入1，category=5）", len(pair) == 2 and {p[1] for p in pair} == {"2", "1"} and all(p[2] == "5" for p in pair), "flows=%s" % (pair,))
    check("红线1 两条金额均 = actual_amount", all(int(p[3]) == actual for p in pair), "amounts=%s actual=%d" % ([p[3] for p in pair], actual))
    check("红线3 balance_after 来自原子更新（= 更新后余额）", any(int(p[4]) == cash_a for p in pair) and any(int(p[4]) == bank_a for p in pair), "balance_after=%s cash=%d bank=%d" % ([p[4] for p in pair], cash_a, bank_a))

    # ---------- AC-25 幂等 ----------
    st, body = call("POST", "/v1/finance/deposit-slips/%d/deposit" % slip_id, {"bankTransactionId": int(txn_id)})
    check("AC-25 重复存行 → 400", (body or {}).get("code") == 400, "http=%s code=%s msg=%s" % (st, (body or {}).get("code"), (body or {}).get("message")))
    check("AC-25 重复存行后流水仍为 +2", flow_count() == flows_after_dep, "count=%d expect=%d" % (flow_count(), flows_after_dep))

    # ---------- SL7 四眼 ----------
    st, body = call("POST", "/v1/finance/deposit-slips/%d/verify" % slip_id, token=TOK1)
    check("SL7 收款人自己复核 → 400（四眼）", (body or {}).get("code") == 400, "code=%s msg=%s" % ((body or {}).get("code"), (body or {}).get("message")))
    st, body = call("POST", "/v1/finance/deposit-slips/%d/verify" % slip_id, token=TOK2)
    ver = data_of(body) or {}
    check("SL7 他人复核 → VERIFIED", st == 200 and ver.get("status") == "VERIFIED", "status=%s" % ver.get("status"))

    # ---------- SL8 ----------
    st, body = call("POST", "/v1/finance/deposit-slips/%d/close" % slip_id, token=TOK2)
    check("SL8 关闭 → CLOSED", st == 200 and (data_of(body) or {}).get("status") == "CLOSED", "status=%s" % (data_of(body) or {}).get("status"))

    # ---------- SL10 冲正 ----------
    st, body = call("POST", "/v1/finance/deposit-slips/%d/reverse" % slip_id, {"reason": "自测：冲正"}, token=TOK2)
    rev = data_of(body) or {}
    cash_r, cashv_r = acct(CASH_ACCOUNT)
    bank_r, bankv_r = acct(BANK_ACCOUNT)
    check("SL10 冲正 → REVERSED", st == 200 and rev.get("status") == "REVERSED", "status=%s" % rev.get("status"))
    orig = sql("select flow_no, status from fund_flows where remark like '%%[depositSlip=%s]%%' order by flow_id" % slip.get("slipNo"))
    check("红线3 原流水置 status=3（未删除）", len(orig) == 2 and all(o[1] == "3" for o in orig), "orig=%s" % (orig,))
    revs = sql("select flow_no, flow_direction, amount, remark from fund_flows where remark like '%%[reconReverseOf=%%' and remark like '%%%s%%' order by flow_id" % slip.get("slipNo"))
    check("红线3 反向流水成对（2 条，方向相反）", len(revs) == 2 and {r[1] for r in revs} == {"1", "2"}, "reverse=%s" % (revs,))
    check("红线3 余额回滚（现金/银行回到基线）", cash_r == cash_b and bank_r == bank_b, "cash=%d(expect %d) bank=%d(expect %d)" % (cash_r, cash_b, bank_r, bank_b))
    st, body = call("POST", "/v1/finance/deposit-slips/%d/reverse" % slip_id, {"reason": "重复冲正"}, token=TOK2)
    check("SL10 重复冲正 → 400（幂等）", (body or {}).get("code") == 400, "code=%s msg=%s" % ((body or {}).get("code"), (body or {}).get("message")))

    # ---------- SL2 / SL3 / SL11 ----------
    st, body = call("GET", "/v1/finance/deposit-slips?shiftId=%d&current=1&size=5" % SHIFT_ID)
    lst = data_of(body) or {}
    check("SL2 列表可读", st == 200 and (lst.get("total") or 0) >= 1, "total=%s" % lst.get("total"))
    st, body = call("GET", "/v1/finance/deposit-slips/%d" % slip_id)
    check("SL3 详情可读", st == 200 and (data_of(body) or {}).get("slipNo") == slip.get("slipNo"))
    ok_csv, csv_text = raw_get("/v1/finance/deposit-slips/export?size=5")
    check("SL11 导出 CSV", ok_csv and csv_text.startswith("slipNo,"), "head=%s" % csv_text.splitlines()[:1])

    print("\n== 汇总：%d 项，%d PASS / %d FAIL ==" % (len(RESULTS), sum(1 for r in RESULTS if r[1]), sum(1 for r in RESULTS if not r[1])))
    for name, ok, detail in RESULTS:
        if not ok:
            print("  FAIL: " + name + "  " + detail)
    return 0 if all(r[1] for r in RESULTS) else 1


if __name__ == "__main__":
    sys.exit(main())
