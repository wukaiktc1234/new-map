#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
逐表体检（E 阶段：新补的表要能被这套脚本「体检过关」）

对每张指定表检查六项：
  1) 主键存在               —— pg_index 里有 indisprimary
  2) deleted 列存在          —— 软删标记（缺失 → 不通过）
  3) create_time / update_time 存在
  4) 外键列有索引             —— 列名以 _id 结尾（或 store_id/order_id/user_id 等）时，
                              要求存在「以该列为第一列」的索引（主键/唯一/普通均可）
  5) 金额列类型               —— 列名含 amount/price/cost/fen 的必须是 bigint；
                              numeric/decimal → 报警（金融精度陷阱）
  6) 时间列类型               —— 报告是 timestamptz 还是 timestamp（与既有表区分；timestamp 记 WARN 不阻断）

用法：
  python scripts/qa/table_health_check.py --tables a,b,c            # 指定表
  python scripts/qa/table_health_check.py --from-file tables.txt
  python scripts/qa/table_health_check.py --all-missing             # 自动取「实体有 @TableName 但库里没有」
  python scripts/qa/table_health_check.py --self-test               # 自检：造缺索引/缺 deleted 的临时表

退出码：0 全通过 / 2 任一表不通过（供 CI 门禁用）
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys

DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"


def psql(a, sql, db=None):
    argv = [a.psql, "-h", a.pg_host, "-p", str(a.pg_port), "-U", a.pg_user,
            "-d", db or a.pg_db, "-t", "-A", "-F", "|", "-c", sql]
    env = dict(os.environ, PGPASSWORD=a.pg_password, PGCLIENTENCODING="UTF8")
    p = subprocess.run(argv, capture_output=True, text=True, timeout=180,
                       encoding="utf-8", errors="replace", env=env)
    if p.returncode != 0:
        raise RuntimeError((p.stderr or p.stdout)[:300])
    return [ln for ln in (p.stdout or "").splitlines() if ln.strip() != ""]


def columns(a, table, db=None):
    rows = psql(a, "SELECT column_name, data_type FROM information_schema.columns"
                  " WHERE table_schema = 'public' AND table_name = '%s' ORDER BY ordinal_position" % table, db)
    return [(r.split("|")[0], r.split("|")[1]) for r in rows]


def has_primary_key(a, table, db=None):
    rows = psql(a, "SELECT 1 FROM pg_index i JOIN pg_class c ON c.oid = i.indrelid"
                  " WHERE c.relname = '%s' AND i.indisprimary LIMIT 1" % table, db)
    return bool(rows)


def indexed_first_cols(a, table, db=None):
    """返回「作为索引第一列」的列名集合（主键/唯一/普通都算）。"""
    rows = psql(a,
                "SELECT a.attname FROM pg_index i"
                " JOIN pg_class c ON c.oid = i.indrelid"
                " JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = i.indkey[0]"
                " WHERE c.relname = '%s'" % table, db)
    return {r.split("|")[0] for r in rows}


ID_HINTS = {"store_id", "order_id", "user_id", "employee_id", "shift_id", "role_id", "food_id",
            "material_id", "product_id", "slip_id", "account_id", "refund_record_id", "supplier_id",
            "customer_id", "kitchen_order_id", "location_id", "department_id"}


def check_table(a, table, db=None):
    problems = []
    cols = columns(a, table, db)
    if not cols:
        return ["表不存在"], 6
    names = [c for c, _ in cols]
    types = dict(cols)

    # 1) 主键
    ok_pk = has_primary_key(a, table, db)
    if not ok_pk:
        problems.append("缺主键")

    # 2) deleted
    ok_deleted = "deleted" in names
    if not ok_deleted:
        problems.append("缺 deleted 列")

    # 3) create_time / update_time
    miss_t = [c for c in ("create_time", "update_time") if c not in names]
    ok_time = not miss_t
    if miss_t:
        problems.append("缺时间列：" + ",".join(miss_t))

    # 4) 外键列有索引
    idx = indexed_first_cols(a, table, db)
    fk_like = [c for c in names if c.endswith("_id") or c in ID_HINTS]
    no_idx = [c for c in fk_like if c not in idx]
    ok_fk = not no_idx
    if no_idx:
        problems.append("外键列无索引：" + ",".join(no_idx[:6]) + ("…" if len(no_idx) > 6 else ""))

    # 5) 金额列类型
    money_cols = [c for c in names if any(k in c for k in ("amount", "price", "cost", "fen"))]
    bad_money = [(c, types[c]) for c in money_cols if types[c] not in ("bigint",)]
    ok_money = not bad_money
    if bad_money:
        problems.append("金额列非 bigint：" + ",".join("%s(%s)" % (c, t) for c, t in bad_money[:6]))

    # 6) 时间列类型（观察项：timestamp 记 WARN，不阻断）
    ts_cols = [(c, types[c]) for c in names if "time" in c and "timestamp" in types.get(c, "")]
    tz = [c for c, t in ts_cols if t == "timestamp with time zone"]
    naive = [c for c, t in ts_cols if t == "timestamp without time zone"]
    if naive:
        problems.append("[WARN] 用 timestamp(无时区)：%s；timestamptz：%s"
                        % (",".join(naive[:6]) or "-", ",".join(tz[:6]) or "-"))

    passed = sum([ok_pk, ok_deleted, ok_time, ok_fk, ok_money])
    return problems, passed


def self_test(a):
    """造一张故意「缺索引 + 缺 deleted + 金额用 numeric」的临时表，验证六项能抓到。"""
    t = "zz_health_selftest"
    ddl = ["DROP TABLE IF EXISTS %s" % t,
           "CREATE TABLE %s (id bigint, store_id bigint, amount_fen numeric, create_time timestamp)" % t,
           "CREATE INDEX ON %s (id)" % t]
    for stmt in ddl:
        psql(a, stmt)
    problems, passed = check_table(a, t)
    got = "；".join(problems)
    expect = ["缺主键", "缺 deleted 列", "缺时间列", "外键列无索引", "金额列非 bigint"]
    ok = all(any(e.split("：")[0] in p for p in problems) for e in expect)
    print("[SELF-TEST] 表=%s 通过项=%d/5" % (t, passed))
    print("[SELF-TEST] 抓到的问题：%s" % got)
    print("[SELF-TEST] %s" % ("PASS（故意缺索引/缺 deleted/金额 numeric 都被抓到）" if ok
                              else "FAIL（有检查项没生效，禁止用它下结论）"))
    psql(a, "DROP TABLE IF EXISTS %s" % t)
    return 0 if ok else 1


def main():
    ap = argparse.ArgumentParser(description="逐表体检（主键/deleted/时间列/外键索引/金额类型/时间类型）")
    ap.add_argument("--tables", help="逗号分隔表名")
    ap.add_argument("--from-file", help="每行一个表名")
    ap.add_argument("--all-missing", action="store_true", help="自动取实体有 @TableName 但库里没有的表")
    ap.add_argument("--db", help="目标库（默认 food_traceability）")
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--psql", default=DEFAULT_PSQL)
    ap.add_argument("--pg-host", default="localhost")
    ap.add_argument("--pg-port", type=int, default=5432)
    ap.add_argument("--pg-user", default="postgres")
    ap.add_argument("--pg-password", default="123456")
    ap.add_argument("--pg-db", default="food_traceability")
    a = ap.parse_args()

    if a.self_test:
        return self_test(a)

    tables = []
    if a.tables:
        tables = [t.strip() for t in a.tables.split(",") if t.strip()]
    elif a.from_file:
        tables = [ln.strip() for ln in open(a.from_file, encoding="utf-8") if ln.strip()]
    elif a.all_missing:
        from pathlib import Path
        import re
        ent = Path(r"P:/my-new-project/backend/src/main/java/com/foodtraceability/entity")
        want = set()
        for p in ent.rglob("*.java"):
            m = re.search(r'@TableName[(]"([A-Za-z0-9_]+)"[)]', p.read_text(encoding="utf-8", errors="replace"))
            if m:
                want.add(m.group(1))
        have = {r.split("|")[0] for r in psql(a, "SELECT table_name FROM information_schema.tables"
                                             " WHERE table_schema='public'", a.db)}
        tables = sorted(want - have)
    if not tables:
        print("需要 --tables / --from-file / --all-missing / --self-test", file=sys.stderr)
        return 2

    print("=" * 96)
    print("逐表体检  共 %d 张  库=%s" % (len(tables), a.db or a.pg_db))
    print("=" * 96)
    print("%-34s %-8s %s" % ("表名", "六项", "问题明细"))
    print("-" * 96)
    bad = 0
    for t in tables:
        try:
            problems, passed = check_table(a, t, a.db)
        except Exception as e:
            print("%-34s %-8s %s" % (t, "-", "查询失败：%s" % str(e)[:80]))
            bad += 1
            continue
        okk = passed == 5 and not any(not p.startswith("[WARN]") for p in problems)
        if not okk:
            bad += 1
        print("%-34s %-8s %s" % (t, "%d/5%s" % (passed, "" if okk else " ✗"),
                                 "；".join(problems) if problems else "OK"))
    print("-" * 96)
    print("结果：%d/%d 通过%s" % (len(tables) - bad, len(tables), "" if bad == 0 else "，%d 张不通过 → EXIT=2" % bad))
    return 0 if bad == 0 else 2


if __name__ == "__main__":
    sys.exit(main())