#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
入库明细 location_id 强约束巡检（只读，exit 0/1）

校验四条（对应 2026-10-24 整改的 DB 侧防线）：
  S1. purchase_stockin_items.location_id 的 is_nullable = 'NO'（NOT NULL 已生效）
  S2. 两条 FK 存在：
        a) purchase_stockin_items.location_id → locations(location_id)
        b) purchase_stockins.location_id      → locations(location_id)
  S3. select count(*) from purchase_stockin_items where location_id is null and deleted=0 = 0
  S4. select count(*) from purchase_stockins    where location_id is null and deleted=0 = 0

只读保证：PGOPTIONS=-c default_transaction_read_only=on，且 SQL 全部为 SELECT。
退出码：0 全 PASS / 1 有 FAIL / 2 执行错误（连不上库、psql 缺失）。

用法：
  python scripts/qa/schema_guard.py
  python scripts/qa/schema_guard.py --host 127.0.0.1 --port 5432 --database food_traceability
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys

DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"


def run_psql(args, sql):
    env = dict(os.environ,
               PGPASSWORD=args.password,
               PGCLIENTENCODING="UTF8",
               PGOPTIONS="-c default_transaction_read_only=on")
    cmd = [args.psql, "-h", args.host, "-p", str(args.port), "-U", args.user,
           "-d", args.database, "-t", "-A", "-F", "|", "-c", sql]
    try:
        out = subprocess.run(cmd, capture_output=True, text=True, timeout=25,
                             encoding="utf-8", errors="replace", env=env)
        return out.returncode, (out.stdout or "").strip(), (out.stderr or "").strip()
    except Exception as e:  # psql 缺失 / 超时
        return -1, "", str(e)


# 期望存在的新增表清单（**只做存在性检查，不是数据白名单**）：
#   由 P1–P3 财务对账 / 号段治理引入；缺表说明迁移未应用，需要补 Flyway。
EXPECTED_TABLES = [
    "fund_flow_no_burn_ledger",   # BL-5 烧号账（V20261025_003，A25 依赖）
]


class Guard:
    def __init__(self, args):
        self.a = args
        self.results = []

    def step(self, name, ok, detail):
        state = "PASS" if ok else "FAIL"
        self.results.append((name, state, detail))
        print("  [%s] %s -> %s" % (state, name, detail))
        return ok

    def col_nullable(self, table, column):
        rc, out, err = run_psql(
            self.a,
            "SELECT is_nullable FROM information_schema.columns "
            "WHERE table_schema='public' AND table_name='%s' AND column_name='%s'" % (table, column))
        if rc != 0:
            return None, "psql rc=%s %s" % (rc, (err or out)[:160])
        if not out:
            return None, "列不存在：%s.%s" % (table, column)
        return out.strip().upper(), ""

    def check_nullable(self, table, column):
        val, err = self.col_nullable(table, column)
        if val is None:
            return self.step("S1 %s.%s NOT NULL" % (table, column), False, err)
        return self.step("S1 %s.%s NOT NULL" % (table, column), val == "NO",
                         "is_nullable=%s（期望 NO）" % val)

    def check_fk(self, table, column):
        sql = ("SELECT c.conname, pg_get_constraintdef(c.oid) "
               "FROM pg_constraint c JOIN pg_class t ON t.oid = c.conrelid "
               "JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attname = '%s' "
               "WHERE c.contype = 'f' AND t.relname = '%s' AND a.attnum = ANY (c.conkey)" % (column, table))
        rc, out, err = run_psql(self.a, sql)
        if rc != 0:
            return self.step("S2 FK %s.%s → locations" % (table, column), False,
                             "psql rc=%s %s" % (rc, (err or out)[:160]))
        defs = " ; ".join(line for line in (out or "").splitlines() if line.strip())
        ok = "locations" in defs.lower() and "location_id" in defs.lower()
        return self.step("S2 FK %s.%s → locations(location_id)" % (table, column), ok,
                         defs or "未找到指向 locations 的外键")

    def check_table_exists(self, table, label):
        rc, out, err = run_psql(self.a, "SELECT to_regclass('public.%s')" % table)
        if rc != 0:
            return self.step("S%d 表存在 %s" % (label, table), False,
                             "psql rc=%s %s" % (rc, (err or out)[:160]))
        exists = bool((out or "").strip()) and (out or "").strip().lower() != "null"
        return self.step("S%d 表存在 %s" % (label, table), exists,
                         "存在" if exists else "缺失（迁移未应用？）")

    def check_nulls(self, table, label):
        rc, out, err = run_psql(
            self.a,
            "SELECT COUNT(*) FROM %s WHERE location_id IS NULL AND deleted = 0" % table)
        if rc != 0:
            return self.step("S%d %s.location_id 无 NULL" % (label, table), False,
                             "psql rc=%s %s" % (rc, (err or out)[:160]))
        cnt = int((out or "0").strip() or 0)
        return self.step("S%d %s.location_id 无 NULL" % (label, table), cnt == 0,
                         "NULL 行数=%d（期望 0）" % cnt)

    # ------------------------------------------------------------------
    # S5（BL-5）：烧号账表存在且关键列齐全（A25 的落账载体）
    # ------------------------------------------------------------------
    BURN_LEDGER_COLUMNS = ["ledger_id", "flow_no", "seq_key", "seq_value", "reason",
                           "archived_flag", "archived_time", "create_time"]

    def check_burn_ledger(self, label):
        rc, out, err = run_psql(
            self.a,
            "SELECT string_agg(column_name, ',' ORDER BY column_name) FROM information_schema.columns "
            "WHERE table_schema='public' AND table_name='fund_flow_no_burn_ledger'")
        if rc != 0:
            return self.step("S%d fund_flow_no_burn_ledger 表存在" % label, False,
                             "psql rc=%s %s" % (rc, (err or out)[:160]))
        cols = set((out or "").split(",")) if out else set()
        if not cols:
            return self.step("S%d fund_flow_no_burn_ledger 表存在" % label, False,
                             "表不存在或没有列（期望 BL-5 迁移 V20261025_003 已应用）")
        missing = [c for c in self.BURN_LEDGER_COLUMNS if c not in cols]
        return self.step("S%d fund_flow_no_burn_ledger 表存在" % label, not missing,
                         "%d 列齐全" % len(cols) if not missing else "缺列：%s" % ",".join(missing))

    def run(self):
        print("=" * 78)
        print("入库明细 location_id 强约束巡检   db=%s@%s:%s" % (self.a.database, self.a.host, self.a.port))
        print("=" * 78)
        self.check_nullable("purchase_stockin_items", "location_id")
        self.check_fk("purchase_stockin_items", "location_id")
        self.check_fk("purchase_stockins", "location_id")
        self.check_nulls("purchase_stockin_items", 3)
        self.check_nulls("purchase_stockins", 4)
        # 新增表的存活性检查（清单见模块级 EXPECTED_TABLES；只判存在，不校验数据）
        for idx, table in enumerate(EXPECTED_TABLES, start=10):
            self.check_table_exists(table, idx)
        # BL-5：烧号账表（A25 载体）—— 除存在性（见 EXPECTED_TABLES）外，再校验关键列齐全
        self.check_burn_ledger(6)
        fails = [r for r in self.results if r[1] == "FAIL"]
        print("\n" + "=" * 78)
        print("汇总: %d 条断言，%d PASS / %d FAIL" % (
            len(self.results), len(self.results) - len(fails), len(fails)))
        for name, state, detail in self.results:
            print("  %-58s %s" % (name, state))
        if fails:
            print("-" * 78)
            print("失败详情：")
            for name, state, detail in fails:
                print("  * %s\n    %s" % (name, detail))
        print("=" * 78)
        return 0 if not fails else 1


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--host", default="localhost")
    p.add_argument("--port", type=int, default=5432)
    p.add_argument("--user", default="postgres")
    p.add_argument("--password", default="123456")
    p.add_argument("--database", default="food_traceability")
    p.add_argument("--psql", default=DEFAULT_PSQL)
    a = p.parse_args()
    try:
        return Guard(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
