#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""财务不变式只读核对脚本（finance invariants, read-only）

用途
----
在财务「地基级」重构（bank_accounts.store_id / fund_flows.balance 并发更新与
create_user_id / receivables 清理 / 状态回填 2.2）前后，用同一份口径复核四条不变式：

  [a] fund_flows 中 SALE 收入（flow_direction=1 且 flow_category=1）的条数与合计金额（分）
      对比 orders 中 payment_status=2 且 deleted=0 的订单条数 / paid_amount 合计，给出差值。
  [b] bank_accounts.store_id：非删除账户中 store_id 为 NULL 的数量，以及 store_id
      指向不存在门店（stores）的数量（应为 0）。默认按 scope 判定：门店类账户
      （scope=STORE 或未标注 scope）必须非空，总部账户（scope=GLOBAL）允许为空；
      加 --strict-store-id 则回到字面口径（任何账户 NULL 都算 FAIL）。
  [c] 现结订单（orders.payment_status=2）关联的应收记录（receivables）数量（应为 0）。
  [d] fund_flows.create_user_id 为 NULL 的数量（应为 0）。

安全约定
--------
* 全程只读：每个查询批次都以 SET TRANSACTION READ ONLY 开头，并用
  PGOPTIONS=-c default_transaction_read_only=on 兜底；脚本不接受任何写语句。
* 不改业务代码、不写库、不产出临时表。
* 可反复执行，结果只与当前数据有关。

用法
----
  python scripts/check_finance_invariants.py
  python scripts/check_finance_invariants.py --json
  python scripts/check_finance_invariants.py --host 127.0.0.1 --port 5432

环境变量（均有默认值，适配本机开发库）
  PSQL_BIN   默认 P:\my-new-project\PostgreSQL\18\bin\psql.exe
  PGHOST / PGPORT / PGUSER / PGPASSWORD / PGDATABASE
             默认 localhost / 5432 / postgres / 123456 / food_traceability

退出码
  0  全部 PASS
  1  存在 FAIL
  2  执行错误（连不上库 / psql 缺失 / SQL 报错）
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from datetime import datetime
from typing import Any

DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"

# ============================================================
# SQL：每条查询只返回一行，字段用 '|' 分隔
# ============================================================

# [a] SALE 收入 vs 现结订单
SQL_A = """
SELECT
  f.flow_cnt,
  f.flow_amt,
  o.paid_cnt,
  o.paid_amt,
  (o.paid_cnt - f.flow_cnt) AS cnt_diff,
  (o.paid_amt - f.flow_amt) AS amt_diff
FROM (
  SELECT count(*) AS flow_cnt, COALESCE(sum(amount), 0) AS flow_amt
  FROM fund_flows
  WHERE deleted = 0 AND flow_direction = 1 AND flow_category = 1
) f
CROSS JOIN (
  SELECT count(*) AS paid_cnt, COALESCE(sum(paid_amount), 0) AS paid_amt
  FROM orders
  WHERE deleted = 0 AND payment_status = 2
) o
"""

# [a-诊断] 已支付订单中，能在 SALE 流水备注里匹配到 [orderId=xxx] 的数量
SQL_A_MATCH = """
SELECT
  count(*) AS matched_cnt
FROM orders o
WHERE o.deleted = 0
  AND o.payment_status = 2
  AND EXISTS (
    SELECT 1
    FROM fund_flows f
    WHERE f.deleted = 0
      AND f.flow_direction = 1
      AND f.flow_category = 1
      AND f.remark LIKE '%[orderId=' || o.order_id || ']%'
  )
"""

# [a-诊断] SALE 流水按 status 拆分，便于判断「待确认/已确认」口径
SQL_A_STATUS = """
SELECT
  COALESCE(sum(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS status1_cnt,
  COALESCE(sum(CASE WHEN status = 2 THEN 1 ELSE 0 END), 0) AS status2_cnt,
  COALESCE(sum(CASE WHEN status NOT IN (1, 2) THEN 1 ELSE 0 END), 0) AS status_other_cnt
FROM fund_flows
WHERE deleted = 0 AND flow_direction = 1 AND flow_category = 1
"""

# [b] bank_accounts.store_id 完整性
SQL_B = """
SELECT
  count(*) AS total_cnt,
  count(*) FILTER (WHERE ba.store_id IS NULL) AS null_cnt,
  count(*) FILTER (WHERE ba.store_id IS NULL AND ba.scope = 'GLOBAL') AS null_global_cnt,
  count(*) FILTER (
    WHERE ba.store_id IS NULL AND COALESCE(ba.scope, '') <> 'GLOBAL'
  ) AS null_store_scoped_cnt,
  count(*) FILTER (
    WHERE ba.store_id IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM stores s WHERE s.store_id = ba.store_id)
  ) AS orphan_cnt
FROM bank_accounts ba
WHERE ba.deleted = 0
"""

# [c] 现结订单对应的应收记录
SQL_C = """
SELECT
  count(*) AS receivable_total,
  COALESCE(sum(CASE WHEN r.order_id IS NULL THEN 1 ELSE 0 END), 0) AS unlinked_cnt,
  COALESCE(sum(CASE WHEN o.payment_status = 2 THEN 1 ELSE 0 END), 0) AS paid_order_receivable_cnt
FROM receivables r
LEFT JOIN orders o
  ON o.deleted = 0
 AND r.order_id IS NOT NULL
 AND o.order_id = r.order_id::text
WHERE r.deleted = 0
"""

# [d] fund_flows.create_user_id 空值
SQL_D = """
SELECT
  count(*) AS total_cnt,
  count(*) FILTER (WHERE create_user_id IS NULL) AS null_user_cnt,
  count(*) FILTER (WHERE created_by IS NULL) AS null_created_by_cnt
FROM fund_flows
WHERE deleted = 0
"""


class SqlError(RuntimeError):
    """psql 调用失败"""


class Db:
    """只读 psql 封装"""

    def __init__(self, psql: str, host: str, port: str, user: str, password: str, database: str) -> None:
        self.psql = psql
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.database = database

    def query(self, sql: str) -> list[list[str]]:
        """执行一条只读查询，返回按 '|' 切分的行"""
        if not os.path.isfile(self.psql):
            raise SqlError(f"psql 不存在: {self.psql}（可用 PSQL_BIN 覆盖）")

        cmd = [
            self.psql,
            "-X", "-q", "-t", "-A", "-F", "|",
            "-v", "ON_ERROR_STOP=1",
            "-h", self.host,
            "-p", self.port,
            "-U", self.user,
            "-d", self.database,
            "-c", "SET TRANSACTION READ ONLY; " + sql.strip().rstrip(";") + ";",
        ]
        env = os.environ.copy()
        env["PGPASSWORD"] = self.password
        env["PGCLIENTENCODING"] = "UTF8"
        # 兜底：本进程发起的所有事务默认只读
        env["PGOPTIONS"] = "-c default_transaction_read_only=on"
        proc = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            env=env,
        )
        if proc.returncode != 0:
            raise SqlError((proc.stderr or proc.stdout or "").strip() or f"psql 退出码 {proc.returncode}")
        rows: list[list[str]] = []
        for line in proc.stdout.splitlines():
            if line.strip() == "":
                continue
            rows.append([cell.strip() for cell in line.split("|")])
        return rows


def to_int(value: Any) -> int:
    try:
        return int(str(value).strip())
    except (TypeError, ValueError):
        return 0


def fmt_money(fen: int) -> str:
    """分 → 元，保留两位便于人读"""
    sign = "-" if fen < 0 else ""
    fen = abs(fen)
    return f"{sign}{fen // 100}.{fen % 100:02d} 元"


def signed(value: int) -> str:
    return f"+{value}" if value > 0 else str(value)


def check_a(db: Db) -> dict[str, Any]:
    row = db.query(SQL_A)[0]
    flow_cnt, flow_amt, paid_cnt, paid_amt, cnt_diff, amt_diff = (to_int(x) for x in row[:6])
    matched = to_int(db.query(SQL_A_MATCH)[0][0])
    st = [to_int(x) for x in db.query(SQL_A_STATUS)[0][:3]]
    passed = cnt_diff == 0 and amt_diff == 0
    return {
        "key": "a",
        "name": "SALE 收入流水 vs 现结订单（payment_status=2）",
        "passed": passed,
        "values": {
            "fund_flow_sale_cnt": flow_cnt,
            "fund_flow_sale_amount_fen": flow_amt,
            "orders_paid_cnt": paid_cnt,
            "orders_paid_amount_fen": paid_amt,
            "cnt_diff": cnt_diff,
            "amount_diff_fen": amt_diff,
            "matched_order_id_cnt": matched,
            "sale_flow_status_1_cnt": st[0],
            "sale_flow_status_2_cnt": st[1],
            "sale_flow_status_other_cnt": st[2],
        },
        "detail": [
            f"fund_flows SALE 收入(方向=1,分类=1,deleted=0) : {flow_cnt} 条 / {flow_amt} 分（{fmt_money(flow_amt)}）",
            f"orders 现结订单(payment_status=2,deleted=0)   : {paid_cnt} 条 / {paid_amt} 分（{fmt_money(paid_amt)}）",
            f"差值(order - flow)                          : 条数 {signed(cnt_diff)} / 金额 {signed(amt_diff)} 分",
            f"备注命中 [orderId=...] 的现结订单             : {matched} / {paid_cnt} 条",
            f"SALE 流水 status 拆分                       : status=1 {st[0]} 条 / status=2 {st[1]} 条 / 其他 {st[2]} 条",
        ],
    }


def check_b(db: Db, strict: bool = False) -> dict[str, Any]:
    row = db.query(SQL_B)[0]
    total, null_cnt, null_global, null_store_scoped, orphan = (to_int(x) for x in row[:5])
    if strict:
        passed = null_cnt == 0 and orphan == 0
        rule = "严格口径(--strict-store-id)：任何未删除账户的 store_id 均不得为 NULL"
    else:
        passed = null_store_scoped == 0 and orphan == 0
        rule = "默认口径：门店类账户(scope=STORE 或未标注)必须非空；总部账户(scope=GLOBAL)允许为空"
    return {
        "key": "b",
        "name": "bank_accounts.store_id 完整性（NULL / 悬空门店）",
        "passed": passed,
        "values": {
            "total_cnt": total,
            "null_store_id_cnt": null_cnt,
            "null_global_scope_cnt": null_global,
            "null_store_scoped_cnt": null_store_scoped,
            "orphan_store_id_cnt": orphan,
            "strict": strict,
        },
        "detail": [
            f"未删除账户总数                    : {total}",
            f"store_id 为 NULL 的数量           : {null_cnt}（其中 GLOBAL 总部账户 {null_global}，门店类账户 {null_store_scoped}）",
            f"store_id 指向不存在门店数量       : {orphan}（应为 0）",
            f"判定口径                          : {rule}",
        ],
    }


def check_c(db: Db) -> dict[str, Any]:
    row = db.query(SQL_C)[0]
    total, unlinked, paid_linked = (to_int(x) for x in row[:3])
    passed = paid_linked == 0
    return {
        "key": "c",
        "name": "现结订单（payment_status=2）关联的应收记录",
        "passed": passed,
        "values": {"receivable_total": total, "unlinked_cnt": unlinked, "paid_order_linked_cnt": paid_linked},
        "detail": [
            f"未删除应收记录总数                    : {total}",
            f"未关联订单(order_id IS NULL)          : {unlinked}",
            f"关联到现结订单(payment_status=2)的数量: {paid_linked}（应为 0）",
        ],
    }


def check_d(db: Db) -> dict[str, Any]:
    row = db.query(SQL_D)[0]
    total, null_user, null_created_by = (to_int(x) for x in row[:3])
    passed = null_user == 0
    return {
        "key": "d",
        "name": "fund_flows.create_user_id 空值",
        "passed": passed,
        "values": {
            "total_cnt": total,
            "null_create_user_id_cnt": null_user,
            "null_created_by_cnt": null_created_by,
        },
        "detail": [
            f"未删除流水总数                 : {total}",
            f"create_user_id 为 NULL 的数量  : {null_user}（应为 0）",
            f"created_by 为 NULL 的数量(参考): {null_created_by}",
        ],
    }


def main() -> int:
    # Windows 控制台默认编码常为 GBK，这里固定 UTF-8 输出，避免中文报告乱码
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure is not None:
            try:
                reconfigure(encoding="utf-8", errors="replace")
            except (OSError, ValueError):
                pass

    parser = argparse.ArgumentParser(description="财务不变式只读核对（PASS/FAIL + 退出码）")
    parser.add_argument("--psql", default=os.environ.get("PSQL_BIN", DEFAULT_PSQL), help="psql 可执行文件路径")
    parser.add_argument("--host", default=os.environ.get("PGHOST", "localhost"))
    parser.add_argument("--port", default=os.environ.get("PGPORT", "5432"))
    parser.add_argument("--user", default=os.environ.get("PGUSER", "postgres"))
    parser.add_argument("--password", default=os.environ.get("PGPASSWORD", "123456"))
    parser.add_argument("--database", default=os.environ.get("PGDATABASE", "food_traceability"))
    parser.add_argument("--json", action="store_true", help="以 JSON 输出（便于流水线消费）")
    parser.add_argument("--strict-store-id", action="store_true",
                        help="[b] 使用严格口径：任何账户 store_id 为 NULL 都判定 FAIL")
    args = parser.parse_args()

    db = Db(args.psql, args.host, args.port, args.user, args.password, args.database)
    started = datetime.now()

    try:
        results = [check_a(db), check_b(db, args.strict_store_id), check_c(db), check_d(db)]
    except SqlError as exc:
        print(f"[ERROR] 查询失败: {exc}", file=sys.stderr)
        return 2
    except (IndexError, ValueError) as exc:
        print(f"[ERROR] 结果解析失败: {exc}", file=sys.stderr)
        return 2

    failed = [r for r in results if not r["passed"]]
    exit_code = 1 if failed else 0

    if args.json:
        print(json.dumps(
            {
                "database": args.database,
                "host": f"{args.host}:{args.port}",
                "checked_at": started.isoformat(timespec="seconds"),
                "passed": len(results) - len(failed),
                "failed": len(failed),
                "exit_code": exit_code,
                "checks": results,
            },
            ensure_ascii=False,
            indent=2,
        ))
        return exit_code

    line = "=" * 72
    print(line)
    print(f"财务不变式只读核对  {args.database} @ {args.host}:{args.port}")
    print(f"核对时间: {started.strftime('%Y-%m-%d %H:%M:%S')}")
    print(line)
    for r in results:
        print(f"[{r['key']}] {'PASS' if r['passed'] else 'FAIL'}  {r['name']}")
        for detail in r["detail"]:
            print(f"      {detail}")
        print()
    print("-" * 72)
    print(f"汇总: {len(results)} 项检查，{len(results) - len(failed)} PASS / {len(failed)} FAIL → exit {exit_code}")
    if failed:
        print("FAIL 项: " + ", ".join(f"[{r['key']}]" for r in failed))
    print("-" * 72)
    return exit_code


if __name__ == "__main__":
    sys.exit(main())
