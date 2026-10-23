#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""SQL 一致性巡检（阶段 2-B）薄封装

执行 scripts/qa/consistency_check.sql，把 psql 输出原样打印，并据结果给退出码：

  A1  已支付订单缺 SALE 资金流水（fund_flows.order_id 关联）
  A2  orders.paid_amount <> SUM(order_payment_records.payment_amount)
  A3  负库存（inventory.quantity / food.stock / foods.stock）

输出约定（来自 SQL）：每类一行 '[PASS]/[FAIL] <断言名> rows=<n>'，最后 'TOTAL FAIL=<k>'。

退出码：0 全 PASS / 1 存在 FAIL / 2 执行错误（连不上库、psql 缺失、SQL 报错）

只读保证：事务内 SET TRANSACTION READ ONLY + PGOPTIONS=-c default_transaction_read_only=on 双保险。

用法：
  python scripts/qa/consistency_check.py
  python scripts/qa/consistency_check.py --host 127.0.0.1 --port 5432 --database food_traceability
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path

DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
DEFAULT_SQL = Path(__file__).with_name("consistency_check.sql")

FAIL_RE = re.compile(r"^\s*\[FAIL\]\s", re.MULTILINE)
PASS_RE = re.compile(r"^\s*\[PASS\]\s", re.MULTILINE)
TOTAL_RE = re.compile(r"^\s*TOTAL FAIL=(\d+)\s*$", re.MULTILINE)


def main() -> int:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure is not None:
            try:
                reconfigure(encoding="utf-8", errors="replace")
            except (OSError, ValueError):
                pass

    ap = argparse.ArgumentParser(description="SQL 一致性巡检（只读）")
    ap.add_argument("--psql", default=os.environ.get("PSQL_BIN", DEFAULT_PSQL))
    ap.add_argument("--sql", default=str(DEFAULT_SQL))
    ap.add_argument("--host", default=os.environ.get("PGHOST", "localhost"))
    ap.add_argument("--port", default=os.environ.get("PGPORT", "5432"))
    ap.add_argument("--user", default=os.environ.get("PGUSER", "postgres"))
    ap.add_argument("--password", default=os.environ.get("PGPASSWORD", "123456"))
    ap.add_argument("--database", default=os.environ.get("PGDATABASE", "food_traceability"))
    args = ap.parse_args()

    if not os.path.isfile(args.psql):
        print(f"[ERROR] 找不到 psql: {args.psql}", file=sys.stderr)
        return 2
    if not os.path.isfile(args.sql):
        print(f"[ERROR] 找不到 SQL 文件: {args.sql}", file=sys.stderr)
        return 2

    env = dict(os.environ)
    env["PGPASSWORD"] = args.password
    env["PGCLIENTENCODING"] = "UTF8"
    # 兜底只读：即使 SQL 文件被改动，也不允许写
    env["PGOPTIONS"] = "-c default_transaction_read_only=on"

    cmd = [
        args.psql, "-h", args.host, "-p", str(args.port),
        "-U", args.user, "-d", args.database,
        "-v", "ON_ERROR_STOP=1",
        "-f", args.sql,
    ]

    try:
        proc = subprocess.run(cmd, env=env, capture_output=True, timeout=120)
    except subprocess.TimeoutExpired:
        print("[ERROR] psql 执行超时（120s）", file=sys.stderr)
        return 2
    except OSError as exc:
        print(f"[ERROR] 启动 psql 失败: {exc}", file=sys.stderr)
        return 2

    out = proc.stdout.decode("utf-8", "replace")
    err = proc.stderr.decode("utf-8", "replace")
    if out:
        print(out.rstrip())
    if err.strip():
        print(err.rstrip(), file=sys.stderr)

    if proc.returncode != 0:
        print(f"[ERROR] psql 退出码 {proc.returncode}（SQL 报错或被只读限制拦截）", file=sys.stderr)
        return 2

    fails = FAIL_RE.findall(out)
    passes = PASS_RE.findall(out)
    total = TOTAL_RE.search(out)
    print()
    print(f"汇总: {len(passes) + len(fails)} 条断言，{len(passes)} PASS / {len(fails)} FAIL"
          + (f"（SQL 自报 TOTAL FAIL={total.group(1)}）" if total else ""))

    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
