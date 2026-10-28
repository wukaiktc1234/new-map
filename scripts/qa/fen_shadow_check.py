#!/usr/bin/env python3
"""fen_shadow_check.py —— *_fen 影子列「未被启用」校验（期望全 0）。

背景：E 批 2/3 为满足新标准加了 *_fen 列，但**双写未实现**，列恒为 NULL。
本脚本检查所有 *_fen 列的 NULL 计数：**期望全部为 0 个非空值**。
若某列非空值 > 0：说明有人开始写它了 → **必须立刻确认双写实现已上线**，
否则会出现「_fen 有值但元列不一致」的静默错账。

退出码：0 = 全部为 NULL（正常）；1 = 发现有非空（需人工确认双写是否上线）。
"""
import os
import subprocess
import sys

PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
DB = os.environ.get("FT_DB", "food_traceability")

TARGETS = [
    ("self_purchase", "total_amount_fen", "total_amount"),
    ("self_purchase_item", "unit_price_fen", "unit_price"),
    ("self_purchase_item", "subtotal_amount_fen", "subtotal_amount"),
    ("receivable", "amount_fen", "amount"),
    ("receivable", "received_amount_fen", "received_amount"),
    ("receivable", "remaining_amount_fen", "remaining_amount"),
    ("requisition_requests", "department_budget_fen", "department_budget"),
    ("requisition_requests", "estimated_cost_fen", "estimated_cost"),
    ("travel_requests", "estimated_budget_fen", "estimated_budget"),
    ("travel_requests", "actual_amount_fen", "actual_amount"),
    ("travel_requests", "advance_payment_fen", "advance_payment"),
    ("travel_requests", "daily_allowance_fen", "daily_allowance"),
]


def q(sql):
    env = dict(os.environ, PGPASSWORD="123456", PGCLIENTENCODING="UTF8")
    out = subprocess.run([PSQL, "-h", "localhost", "-p", "5432", "-U", "postgres",
                          "-d", DB, "-A", "-t", "-c", sql],
                         capture_output=True, text=True, env=env)
    return out.stdout.strip()


def main():
    bad = 0
    print("列                                 非空值数   元列不一致数")
    print("-" * 70)
    for table, fen, yuan in TARGETS:
        non_null = q(f"SELECT count(*) FROM {table} WHERE {fen} IS NOT NULL")
        mismatch = q(
            f"SELECT count(*) FROM {table} WHERE {fen} IS NOT NULL "
            f"AND {fen} IS DISTINCT FROM ({yuan} * 100)::bigint"
        )
        flag = "" if non_null == "0" else "  <== 有人开始写 _fen，请确认双写已上线"
        if non_null != "0":
            bad += 1
        print("%-34s %-10s %-10s%s" % (table + "." + fen, non_null, mismatch, flag))
    print("-" * 70)
    if bad:
        print("[FAIL] %d 个 _fen 列已有非空数据 —— 先确认双写实现与等值校验（tech-debt-schema.md P1-1）" % bad)
        return 1
    print("[PASS] 全部 *_fen 列均为 NULL（双写未启用，符合预期）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
