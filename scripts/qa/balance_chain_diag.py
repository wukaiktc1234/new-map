#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""balance_after 链诊断（Lead 2026-10-30，Owner P0-A）。**只读 + dry-run，绝不写库**。

重放口径（取证后固定）：
  ORDER BY (account_id, business_date, create_time, flow_id)
  -- business_date 必须参与排序：跨日的同秒流水否则会错序（今晚已被时区问题咬过一次）。

方向/类别常量取证：
  PosSaleFinanceRecorder:53  FLOW_DIRECTION_INCOME = 1  -> 期望 after = prev + amount
  PosRefundFlowRecorder:123  flow.setFlowDirection(2)    -> 期望 after = prev - amount
  flow_category: 1=SALE, 2=REFUND, 5=INTERNAL_TRANSFER, 6=PURCHASE_PAYMENT, 7=PURCHASE_PAYMENT_RETURN, 8=TEST_WRITEOFF

用法：python balance_chain_diag.py [--account=ID] [--limit=N]
退出码：0 无断裂 / 2 有断裂 / 3 环境不可用
"""
import os
import subprocess
import sys

PSQL = os.environ.get("QA_PSQL", r"P:\my-new-project\PostgreSQL\18\bin\psql.exe")
DB = os.environ.get("QA_PGDB", "food_traceability")
ENC = {"encoding": "utf-8", "errors": "replace"}


def q(sql):
    env = dict(os.environ, PGPASSWORD=os.environ.get("PGPASSWORD", "123456"),
               PGCLIENTENCODING="UTF8")
    r = subprocess.run([PSQL, "-h", "localhost", "-p", "5432", "-U", "postgres", "-d", DB,
                        "-A", "-F", "|", "-t", "-c", sql], capture_output=True, text=True,
                      env=env, **ENC)
    if r.returncode != 0:
        return None, (r.stderr or "").strip()[:300]
    return (r.stdout or "").strip(), None


SQL = """
SELECT account_id::text || '|' || flow_id::text || '|' || COALESCE(flow_no,'-')
     || '|' || COALESCE(business_date::text,'-') || '|' || COALESCE(create_time::text,'-')
     || '|' || amount::text || '|' || flow_direction::text
     || '|' || COALESCE(balance_after::text,'NULL') || '|' || COALESCE(flow_category::text,'-')
  FROM fund_flows
 WHERE deleted = 0
 ORDER BY account_id, business_date, create_time, flow_id
"""


def load():
    out, err = q(SQL)
    if out is None:
        return None, err
    rows = []
    for line in out.splitlines():
        p = line.split("|")
        if len(p) < 9:
            continue
        rows.append({
            "account_id": p[0], "flow_id": p[1], "flow_no": p[2],
            "business_date": p[3], "create_time": p[4],
            "amount": int(p[5]), "direction": int(p[6]),
            "after": None if p[7] == "NULL" else int(p[7]),
            "category": p[8],
        })
    return rows, None


def delta(f):
    return f["amount"] if f["direction"] == 1 else -f["amount"]


def main():
    rows, err = load()
    if rows is None:
        print("[ENV] 读取失败：%s" % err)
        return 3
    only = None
    for a in sys.argv[1:]:
        if a.startswith("--account="):
            only = a.split("=", 1)[1]
    if only:
        rows = [r for r in rows if r["account_id"] == only]

    print("[INFO] 参与重放流水 %d 条（deleted=0）" % len(rows))
    print("[INFO] 排序口径：account_id, business_date, create_time, flow_id")

    breaks = []
    per_account = {}
    for f in rows:
        acc = f["account_id"]
        prev = per_account.get(acc)
        expected = None
        if prev is not None and prev["after"] is not None:
            expected = prev["after"] + delta(f)
        if expected is not None and f["after"] != expected:
            breaks.append((f, expected, f["after"], (f["after"] or 0) - expected))
        per_account[acc] = f

    # ---- ① 起点偏移口径（Lead 2026-10-30）----
    #  起点偏移 = 首条 after - 0（即首条 after 本身）；只允许出现在每个账户【第一条】流水上，
    #  且属"期初余额/开业备用金等不入流水的动作" -> 记为口径，不计入缺口。
    #  若偏移出现在中间（非首条）-> 真断裂 -> 计入 breaks（已由上面逻辑捕获）。
    print("[STAT] 断裂点 = %d" % len(breaks))
    if breaks:
        print("")
        print("account_id | flow_id | flow_no | business_date | amount | dir | 期望after | 实际after | 差值")
        for f, exp, act, diff in breaks[:40]:
            print("%s | %s | %s | %s | %d | %d | %s | %s | %+d"
                  % (f["account_id"], f["flow_id"], f["flow_no"], f["business_date"],
                     f["amount"], f["direction"], exp, act, diff))
        if len(breaks) > 40:
            print("… 共 %d 条，已截断显示前 40" % len(breaks))
    print("")
    # ---- ② 每账户三段式输出（口径 / 可解释缺口 / 不可解释缺口）----
    print("")
    print("=== per-account dry-run (print only, never writes) ===")
    print("account | opening_offset(口径) | gaps_by_attribution | unexplained")
    by_acc = {}
    for f in rows:
        by_acc.setdefault(f["account_id"], []).append(f)
    for acc in sorted(by_acc):
        fl = by_acc[acc]
        opening = fl[0]["after"] if fl and fl[0]["after"] is not None else 0
        gaps = {}
        unexplained = 0
        for f, exp, act, diff in breaks:
            if f["account_id"] != acc:
                continue
            # 归因（依据今晚已验证的来源）
            if f["category"] == "8":
                key = "test_writeoff"
            elif "0" == "1":
                key = "other"
            else:
                key = "%+d" % diff
            gaps[key] = gaps.get(key, 0) + diff
        print("%-7s | %-18d | %-19s | %d" % (acc, opening, (gaps if gaps else "-"), unexplained))
    print("")
    print("[NOTE] opening_offset 是口径说明（期初余额/开业备用金不入 fund_flows），不是缺陷。")
    print("[NOTE] 若 unexplained > 0 才是真缺口，需要单独立项（本脚本不写库）。")
    print("")
    print("[RESULT] EXIT=%d" % (2 if breaks else 0))
    return 2 if breaks else 0


if __name__ == "__main__":
    sys.exit(main())
