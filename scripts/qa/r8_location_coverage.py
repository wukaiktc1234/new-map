#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""R8：location 归属覆盖率只读排查（薄封装）

执行 scripts/qa/r8_location_coverage.sql 并原样打印 psql 输出。

**只读**：SQL 事务内 SET TRANSACTION READ ONLY；本封装另用 PGOPTIONS=-c default_transaction_read_only=on 兜底。
**不改配置**：只读排查，绝不触碰 application.yml 或任何开关。

退出码：**固定 0**（无论数据如何）——这是给人看的排查报告，不是门禁；只有执行失败（psql 缺失/连不上）才返回 2。

用法：
  python scripts/qa/r8_location_coverage.py
  python scripts/qa/r8_location_coverage.py --json        # 机器可读（解析 §6 三个决策数字）
  python scripts/qa/r8_location_coverage.py --host 127.0.0.1 --port 5432 --database food_traceability
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
from pathlib import Path

DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
DEFAULT_SQL = Path(__file__).with_name("r8_location_coverage.sql")


def main() -> int:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure is not None:
            try:
                reconfigure(encoding="utf-8", errors="replace")
            except (OSError, ValueError):
                pass

    ap = argparse.ArgumentParser(description="R8 location 归属覆盖率只读排查")
    ap.add_argument("--psql", default=os.environ.get("PSQL_BIN", DEFAULT_PSQL))
    ap.add_argument("--sql", default=str(DEFAULT_SQL))
    ap.add_argument("--host", default=os.environ.get("PGHOST", "localhost"))
    ap.add_argument("--port", default=os.environ.get("PGPORT", "5432"))
    ap.add_argument("--user", default=os.environ.get("PGUSER", "postgres"))
    ap.add_argument("--password", default=os.environ.get("PGPASSWORD", "123456"))
    ap.add_argument("--database", default=os.environ.get("PGDATABASE", "food_traceability"))
    ap.add_argument("--json", action="store_true", help="输出机器可读 JSON（含三个决策数字）")
    args = ap.parse_args()

    if not os.path.isfile(args.psql):
        print(f"[ERROR] 找不到 psql: {args.psql}", file=sys.stderr)
        return 2
    if not os.path.isfile(args.sql):
        print(f"[ERROR] 找不到 SQL: {args.sql}", file=sys.stderr)
        return 2

    env = dict(os.environ)
    env["PGPASSWORD"] = args.password
    env["PGCLIENTENCODING"] = "UTF8"
    env["PGOPTIONS"] = "-c default_transaction_read_only=on"

    cmd = [args.psql, "-h", args.host, "-p", str(args.port), "-U", args.user,
           "-d", args.database, "-v", "ON_ERROR_STOP=1", "-f", args.sql]

    try:
        proc = subprocess.run(cmd, env=env, capture_output=True, timeout=180)
    except subprocess.TimeoutExpired:
        print("[ERROR] psql 执行超时", file=sys.stderr)
        return 2
    except OSError as exc:
        print(f"[ERROR] 启动 psql 失败: {exc}", file=sys.stderr)
        return 2

    out = proc.stdout.decode("utf-8", "replace")
    err = proc.stderr.decode("utf-8", "replace")

    if args.json:
        payload = {"exit_code": 0, "raw_sql_output": out}
        # 从 §6 的结论行里抠出三个决策数字
        m = re.search(r"^\s*(\d+)\s*\|\s*(\d+)\s*\|\s*([\d.]+)\s*\|\s*(\d+)\s*\|\s*(\d+)\s*\|\s*([\d.]+)\s*\|\s*(\d+)",
                      out, re.MULTILINE)
        if m:
            payload["decisions"] = {
                "active_users": int(m.group(1)),
                "active_users_with_location": int(m.group(2)),
                "pct_active_user_coverage": float(m.group(3)),
                "active_employees": int(m.group(4)),
                "active_employees_with_location": int(m.group(5)),
                "pct_active_employee_coverage": float(m.group(6)),
                "bad_location_refs": int(m.group(7)),
            }
        print(json.dumps(payload, ensure_ascii=False, indent=2))
    else:
        if out:
            print(out.rstrip())

    if err.strip():
        print(err.rstrip(), file=sys.stderr)
    if proc.returncode != 0:
        print(f"[ERROR] psql 退出码 {proc.returncode}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
