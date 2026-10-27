
# -*- coding: utf-8 -*-
"""BL-4 回归：generate/submit/receive/deposit/reverse 5 项（真实门店 1 / 班次 6）。"""
import sys, importlib.util
spec = importlib.util.spec_from_file_location("bl4", r"P:\my-new-project\scripts\qa\deposit_slip_balance_guard_selftest.py")
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)

# 释放班次 6：删掉我上一轮 REVERSED 的 DS202610250028 及其净额 0 的流水对
m.one("DELETE FROM deposit_slips WHERE slip_no='DS202610250028' AND status='REVERSED'", write=True)
m.one("DELETE FROM fund_flows WHERE remark LIKE '%DS202610250028%' AND flow_category=5 AND (remark LIKE '%[depositSlip=%' OR remark LIKE '%[reconReverseOf=%')", write=True)

st, body = m.call("POST", "/v1/finance/deposit-slips/generate", {"shiftId": 6, "storeId": 1})
slip = m.d(body).get("slip") or {}
sid, exp = slip.get("slipId"), slip.get("expectedAmount")
m.check("回归1 generate（显式 storeId=1）", sid is not None, "slipId=%s slipNo=%s expected=%s reused=%s" % (sid, slip.get("slipNo"), exp, m.d(body).get("reused")))

st, body = m.call("POST", "/v1/finance/deposit-slips/generate", {"shiftId": 6, "storeId": 1})
m.check("回归1b 幂等 reused=true", m.d(body).get("reused") is True, "slipId=%s" % (m.d(body).get("slip") or {}).get("slipId"))

st, body = m.call("POST", "/v1/finance/deposit-slips/%d/submit" % sid)
m.check("回归2 submit → SUBMITTED", m.d(body).get("status") == "SUBMITTED", "status=%s" % m.d(body).get("status"))

f0 = m.flows()
actual = int(exp) - 100
st, body = m.call("POST", "/v1/finance/deposit-slips/%d/receive" % sid, {"receivedAmountFen": actual})
f1 = m.flows()
m.check("回归3 receive → RECEIVED，差额=actual−expected=−100，不生成流水",
        m.d(body).get("status") == "RECEIVED" and m.d(body).get("differenceAmount") == -100 and f1 == f0,
        "status=%s difference=%s flows=%d→%d" % (m.d(body).get("status"), m.d(body).get("differenceAmount"), f0, f1))

cash_b = m.acct(20); bank_b = m.acct(24)
txn = m.one("INSERT INTO bank_transactions (batch_id, account_id, source_type, external_txn_id, direction, amount, fee, txn_time, business_date, dedupe_basis, raw_hash, create_user_id) "
            "VALUES (0, 24, 'BL4REG', 'REG%05d', 1, %d, 0, NOW(), CURRENT_DATE, 'REG%05d', 'bl4', 1) RETURNING transaction_id" % (sid, actual, sid), write=True)[0]
st, body = m.call("POST", "/v1/finance/deposit-slips/%d/deposit" % sid, {"bankTransactionId": int(txn)})
f2 = m.flows()
cash_a = m.acct(20); bank_a = m.acct(24)
m.check("回归4 deposit → DEPOSITED，+2 流水，余额/version 正确",
        m.d(body).get("status") == "DEPOSITED" and f2 - f1 == 2
        and cash_a[0] == cash_b[0] - actual and cash_a[1] == cash_b[1] + 1
        and bank_a[0] == bank_b[0] + actual and bank_a[1] == bank_b[1] + 1,
        "status=%s flows=%d→%d 现金户 %d→%d v%d→v%d 银行户 %d→%d v%d→v%d"
        % (m.d(body).get("status"), f1, f2, cash_b[0], cash_a[0], cash_b[1], cash_a[1], bank_b[0], bank_a[0], bank_b[1], bank_a[1]))

st, body = m.call("POST", "/v1/finance/deposit-slips/%d/reverse" % sid, {"reason": "BL-4 回归回滚"}, token=m.TOK2)
cash_r = m.acct(20); bank_r = m.acct(24)
orig = m.sql("select status from fund_flows where remark like '%%[depositSlip=%s]%%'" % (m.d(body).get("slipNo") or ""))
m.check("回归5 reverse → REVERSED，余额回滚、原流水 status=3",
        m.d(body).get("status") == "REVERSED" and cash_r[0] == cash_b[0] and bank_r[0] == bank_b[0] and orig and all(o[0] == "3" for o in orig),
        "status=%s 现金户=%d(期望 %d) 银行户=%d(期望 %d) orig=%s" % (m.d(body).get("status"), cash_r[0], cash_b[0], bank_r[0], bank_b[0], orig))

print("\n== 汇总：%d 项，%d PASS / %d FAIL ==" % (len(m.RES), sum(1 for r in m.RES if r[1]), sum(1 for r in m.RES if not r[1])))
sys.exit(0 if all(r[1] for r in m.RES) else 1)
