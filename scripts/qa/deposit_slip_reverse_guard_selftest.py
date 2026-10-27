
# -*- coding: utf-8 -*-
"""专项审计 §1：SL10 冲正负 delta 分支余额下限校验（共用 addBalanceGuarded）。"""
import sys, importlib.util
spec = importlib.util.spec_from_file_location("bl4", r"P:\my-new-project\scripts\qa\deposit_slip_balance_guard_selftest.py")
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)

def main():
    for s in (sys.stdout, sys.stderr):
        try:
            s.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass

    cash_id, bank_id = m.setup()
    print("fixtures: store=%d cash=%d(34200) bank=%d(0)" % (m.STORE, cash_id, bank_id))

    slip = m.make_slip("DSBL4C0001", 99003, 34200, 34200)
    txn = m.make_txn(bank_id, 34200, "BL4-C")
    st, body = m.call("POST", "/v1/finance/deposit-slips/%d/deposit" % slip, {"bankTransactionId": txn})
    cash_a = m.acct(cash_id); bank_a = m.acct(bank_id)
    m.check("前置：存行成功（现金户→银行户 34200）", m.d(body).get("status") == "DEPOSITED",
            "status=%s 现金户=%d 银行户=%d" % (m.d(body).get("status"), cash_a[0], bank_a[0]))

    # 把测试银行户余额清 0（仅自己的测试账户），制造「冲正时从银行户转出但余额不足」
    m.one("UPDATE bank_accounts SET balance = 0, version = version + 1 WHERE account_id = %d" % bank_id, write=True)
    f0 = m.flows()
    st, body = m.call("POST", "/v1/finance/deposit-slips/%d/reverse" % slip, {"reason": "余额不足冲正"}, token=m.TOK2)
    msg = (body or {}).get("message", "")
    cash_r = m.acct(cash_id); bank_r = m.acct(bank_id)
    f1 = m.flows()
    orig = m.sql("select flow_no, status from fund_flows where remark like '%%[depositSlip=%%' and remark like '%%%s%%' order by flow_id"
                 % (m.one("select slip_no from deposit_slips where slip_id=%d" % slip)[0]))
    slip_status = m.one("select status from deposit_slips where slip_id=%d" % slip)[0]
    m.check("冲正时银行户余额不足 → 409 且文案含三数字",
            str((body or {}).get("code")) == "409" and "0 分" in msg and "34200 分" in msg and "差额 34200 分" in msg,
            "code=%s\n    原文: %s" % ((body or {}).get("code"), msg))
    m.check("失败后两账户余额不变（整体回滚）", cash_r[0] == 0 and bank_r[0] == 0,
            "现金户=%d(期望 0，未被 +34200) 银行户=%d(期望 0)" % (cash_r[0], bank_r[0]))
    m.check("失败后 fund_flows 无新增", f1 == f0, "flows=%d→%d" % (f0, f1))
    m.check("失败后原流水仍 status=2（未被置 3）、单据仍 DEPOSITED",
            len(orig) == 2 and all(o[1] == "2" for o in orig) and slip_status == "DEPOSITED",
            "orig=%s slip.status=%s" % (orig, slip_status))

    # 恢复余额后正常冲正（AC-28）
    m.one("UPDATE bank_accounts SET balance = 34200, version = version + 1 WHERE account_id = %d" % bank_id, write=True)
    st, body = m.call("POST", "/v1/finance/deposit-slips/%d/reverse" % slip, {"reason": "AC-28 正常冲正"}, token=m.TOK2)
    cash_ok = m.acct(cash_id); bank_ok = m.acct(bank_id)
    orig2 = m.sql("select status from fund_flows where remark like '%%[depositSlip=%%' and remark like '%%%s%%'" % (m.one("select slip_no from deposit_slips where slip_id=%d" % slip)[0]))
    m.check("AC-28 恢复余额后正常冲正 → REVERSED，余额还原、原流水 status=3",
            m.d(body).get("status") == "REVERSED" and cash_ok[0] == 34200 and bank_ok[0] == 0 and all(o[0] == "3" for o in orig2),
            "status=%s 现金户=%d 银行户=%d orig=%s" % (m.d(body).get("status"), cash_ok[0], bank_ok[0], orig2))

    st, body = m.call("POST", "/v1/finance/deposit-slips/%d/reverse" % slip, {"reason": "重复"}, token=m.TOK1)
    m.check("重复调用冲正 → 400", str((body or {}).get("code")) == "400",
            "code=%s msg=%s" % ((body or {}).get("code"), (body or {}).get("message")))

    print("\n== 汇总：%d 项，%d PASS / %d FAIL ==" % (len(m.RES), sum(1 for r in m.RES if r[1]), sum(1 for r in m.RES if not r[1])))
    return 0 if all(r[1] for r in m.RES) else 1

sys.exit(main())
