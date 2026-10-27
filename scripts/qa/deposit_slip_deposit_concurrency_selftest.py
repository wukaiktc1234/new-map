
# -*- coding: utf-8 -*-
"""审计 §5.2：SL6 deposit 并发幂等（条件更新闸门）+ AC-23/24/25/26/28 回归。"""
import sys, importlib.util, threading
spec = importlib.util.spec_from_file_location("bl4", r"P:\my-new-project\scripts\qa\deposit_slip_balance_guard_selftest.py")
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)

def main():
    for s in (sys.stdout, sys.stderr):
        try:
            s.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass

    # ---------- A. 并发存行 ----------
    cash_id, bank_id = m.setup()
    m.one("UPDATE bank_accounts SET balance = 500000 WHERE account_id = %d" % cash_id, write=True)
    slip = m.make_slip("DSBL4D0001", 99004, 34200, 34200)
    txn = m.make_txn(bank_id, 34200, "BL4-D")
    cash0 = m.acct(cash_id); bank0 = m.acct(bank_id); f0 = m.flows()
    print("并发前: 现金户=%d 银行户=%d fund_flows=%d" % (cash0[0], bank0[0], f0))

    barrier = threading.Barrier(2)
    out = {}
    def worker(tag, tok):
        barrier.wait()
        st, body = m.call("POST", "/v1/finance/deposit-slips/%d/deposit" % slip, {"bankTransactionId": txn}, token=tok)
        out[tag] = (st, (body or {}).get("code"), (body or {}).get("message"), m.d(body).get("status"))
    threads = [threading.Thread(target=worker, args=("T1", m.TOK1)), threading.Thread(target=worker, args=("T2", m.TOK2))]
    [t.start() for t in threads]; [t.join() for t in threads]
    for t in ("T1", "T2"):
        print("  线程%s: http=%s code=%s status=%s msg=%s" % (t, out[t][0], out[t][1], out[t][3], out[t][2]))

    cash1 = m.acct(cash_id); bank1 = m.acct(bank_id); f1 = m.flows()
    flows = m.sql("select flow_no, account_id, flow_direction, amount, balance_after from fund_flows where remark like '%%[depositSlip=%%' and remark like '%%DSBL4D0001%%' order by flow_id")
    ok_codes = sorted([str(out["T1"][1]), str(out["T2"][1])])
    m.check("并发：一个成功(200/code=0)、一个 409", ok_codes == ["0", "409"],
            "codes=%s" % (ok_codes,))
    m.check("[depositSlip=DSBL4D0001] 流水恰好 2 条", len(flows) == 2, "flows=%s" % ([f[0] for f in flows],))
    m.check("现金户只扣一次（500000→465800）", cash1[0] == 465800, "现金户 %d→%d（Δ%d）" % (cash0[0], cash1[0], cash1[0] - cash0[0]))
    m.check("银行户只入一次（0→34200）", bank1[0] == 34200, "银行户 %d→%d（Δ%d）" % (bank0[0], bank1[0], bank1[0] - bank0[0]))
    m.check("fund_flows 只 +2", f1 - f0 == 2, "flows=%d→%d" % (f0, f1))

    st, body = m.call("POST", "/v1/finance/deposit-slips/%d/deposit" % slip, {"bankTransactionId": txn})
    f2 = m.flows()
    m.check("再串行调一次 → 409/400，流水仍 2 条", str((body or {}).get("code")) in ("400", "409") and f2 == f1,
            "code=%s msg=%s flows=%d→%d" % ((body or {}).get("code"), (body or {}).get("message"), f1, f2))

    # ---------- B. AC 回归（测试门店 99001；真实门店现金户 20 已被他人在跑的业务消耗到 200 分，不适合做存行回归）----------
    exp = 34200
    sid = m.one("INSERT INTO deposit_slips (slip_no, store_id, shift_id, expected_amount, actual_amount, difference_amount, status, create_user_id, created_by, create_time, update_time, version, deleted) "
                "VALUES ('DSBL4E0001', %d, 99005, %d, 0, %d, 'SUBMITTED', 1, 'bl4-selftest', NOW(), NOW(), 0, 0) RETURNING slip_id"
                % (m.STORE, exp, -exp), write=True)[0]
    sid = int(sid)
    fa = m.flows()
    st, body = m.call("POST", "/v1/finance/deposit-slips/%d/receive" % sid, {"receivedAmountFen": exp})
    fb = m.flows()
    m.check("AC-23 RECEIVED 不产流水", m.d(body).get("status") == "RECEIVED" and fb == fa,
            "status=%s flows=%d→%d difference=%s" % (m.d(body).get("status"), fa, fb, m.d(body).get("differenceAmount")))
    txn2 = m.one("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id, direction, amount, fee, txn_time, business_date, dedupe_basis, raw_hash, create_user_id) "
                 "VALUES (0, %d, 'BL4REG', 'REGX%04d', 1, %d, 0, NOW(), CURRENT_DATE, 'REGX%04d', 'bl4', 1) RETURNING transaction_id" % (bank_id, sid, exp, sid), write=True)[0]
    m.one("UPDATE bank_accounts SET balance = 500000 WHERE account_id = %d" % cash_id, write=True)
    cash_b = m.acct(cash_id); bank_b = m.acct(bank_id)
    st, body = m.call("POST", "/v1/finance/deposit-slips/%d/deposit" % sid, {"bankTransactionId": int(txn2)})
    fc = m.flows(); cash_a = m.acct(cash_id); bank_a = m.acct(bank_id)
    m.check("AC-24 DEPOSITED 恰好 2 条流水", m.d(body).get("status") == "DEPOSITED" and fc - fb == 2, "flows=%d→%d" % (fb, fc))
    pair = m.sql("select flow_direction, amount, balance_after from fund_flows where remark like '%%[depositSlip=%s]%%' order by flow_id" % (m.d(body).get("slipNo") or ""))
    m.check("AC-26 余额一致（balance_after == 实际余额、两腿金额相等）",
            len(pair) == 2 and int(pair[0][1]) == int(pair[1][1])
            and any(int(x[2]) == cash_a[0] for x in pair) and any(int(x[2]) == bank_a[0] for x in pair),
            "pair=%s 现金户=%d 银行户=%d" % (pair, cash_a[0], bank_a[0]))
    st, body = m.call("POST", "/v1/finance/deposit-slips/%d/deposit" % sid, {"bankTransactionId": int(txn2)})
    m.check("AC-25 重复存行 → 400/409 且流水仍 2 条", str((body or {}).get("code")) in ("400", "409") and m.flows() == fc,
            "code=%s msg=%s" % ((body or {}).get("code"), (body or {}).get("message")))
    st, body = m.call("POST", "/v1/finance/deposit-slips/%d/reverse" % sid, {"reason": "§5.2 回归冲正"}, token=m.TOK2)
    cash_r = m.acct(cash_id); bank_r = m.acct(bank_id)
    m.check("AC-28 冲正常 P2 REVERSED、余额还原", m.d(body).get("status") == "REVERSED" and cash_r[0] == cash_b[0] and bank_r[0] == bank_b[0],
            "status=%s 现金户=%d(期望 %d) 银行户=%d(期望 %d)" % (m.d(body).get("status"), cash_r[0], cash_b[0], bank_r[0], bank_b[0]))

    print("\n== 汇总：%d 项，%d PASS / %d FAIL ==" % (len(m.RES), sum(1 for r in m.RES if r[1]), sum(1 for r in m.RES if not r[1])))
    return 0 if all(r[1] for r in m.RES) else 1

sys.exit(main())
