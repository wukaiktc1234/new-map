#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
按 tag 清理 E2E 夹具（独立脚本，**不由巡检/用例调用**）

时序（Owner 2026-10-27 定）：
    E2E 造数（保留数据） → 跑巡检（分母 > 0） → 取证留档 → 本脚本按 tag 清理

标记体系：
    · pos_shifts.terminal_id   = T-E2E-<tag>
    · orders.idempotency_key   = E2E-<tag>-<n>
    · order_refund_records.refund_reason / refund_shift_attribution.reason / 关班 remark = 前缀 [E2E:<tag>]
    · deposit_slips            = 经 shift_id 关联（generate 未传 remark，无独立 tag）

⚠ **已知后端缺陷（bug-③）**：POS 下单接口**不落** orders.idempotency_key（实测库内 NULL），
   所以 "E2E-<tag>-<n>" 目前只是传参标记，**不能**当 orders 的定位依据；
   本脚本因此用「带 tag 的退款记录 → order_id」反查（两套件每单必经退款），
   幂等键条件保留，等 bug-③ 修好后自动生效。

⚠ **fund_flows.remark 由后端生成**（PosRefundFlowRecorder:132 "POS退款：退款单#… [POS]"），
   **不含 tag** → 流水只能经 order_id / refund_record_id 外键定位，**绝不能按 remark 找**，
   否则会漏删（这是 tag 体系唯一缺口，写死在此防后人误用）。

用法：
    python scripts/qa/e2e_cleanup.py --tag HIK1027152338            # 默认 dry-run
    python scripts/qa/e2e_cleanup.py --tag HIK1027152338 --apply    # 真删（幂等）
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys

DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"


class Cleaner:
    def __init__(self, a):
        self.a = a
        self.tag = a.tag
        self.mark = "[E2E:%s]" % a.tag
        self.term = "T-E2E-%s" % a.tag
        self.idem = "E2E-%s-" % a.tag
        self.applied = {}

    def sql(self, q):
        o = subprocess.run([self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port),
                            "-U", self.a.pg_user, "-d", self.a.pg_db, "-t", "-A",
                            "-c", q], capture_output=True, text=True, timeout=180,
                           encoding="utf-8", errors="replace",
                           env=dict(os.environ, PGPASSWORD=self.a.pg_password, PGCLIENTENCODING="UTF8"))
        return o.returncode, (o.stdout or "").strip(), (o.stderr or "").strip()

    def scalar(self, q):
        rc, out, _ = self.sql(q)
        if rc != 0:
            return None
        v = out.splitlines()[-1].strip() if out else ""
        return int(v) if v.lstrip("-").isdigit() else None

    def exists(self, table):
        return bool(self.scalar("SELECT count(*) FROM information_schema.tables"
                                " WHERE table_schema='public' AND table_name='%s'" % table))

    def has_col(self, table, col):
        return bool(self.scalar("SELECT count(*) FROM information_schema.columns"
                                " WHERE table_schema='public' AND table_name='%s' AND column_name='%s'"
                                % (table, col)))

    def delete(self, label, table, where):
        if not self.exists(table):
            print("  [SKIP] %-26s 表不存在" % label)
            return 0
        n = self.scalar("SELECT count(*) FROM %s WHERE %s" % (table, where))
        if n is None:
            print("  [SKIP] %-26s 条件无法评估（列缺失？）" % label)
            return 0
        if not self.a.apply:
            print("  [DRY ] %-26s 命中 %d 行" % (label, n))
            return n
        rc, out, err = self.sql("WITH del AS (DELETE FROM %s WHERE %s RETURNING 1)"
                                " SELECT count(*) FROM del" % (table, where))
        got = int(out.splitlines()[-1]) if rc == 0 and out else -1
        print("  [DEL ] %-26s 删除 %s 行%s" % (label, got, "" if rc == 0 else "（err=%s）" % err[:70]))
        self.applied[label] = got
        return got if got and got > 0 else 0

    def collect(self, table, col, where):
        if not self.exists(table) or not self.has_col(table, col):
            return []
        rc, out, _ = self.sql("SELECT %s FROM %s WHERE %s" % (col, table, where))
        return [x.strip() for x in out.splitlines() if x.strip()] if rc == 0 else []

    def run(self):
        t, m, term, idem = self.tag, self.mark, self.term, self.idem
        print("=" * 86)
        print("E2E 清理  tag=%s  模式=%s" % (t, "APPLY(真删)" if self.a.apply else "DRY-RUN(只数)"))
        print("  锚点：terminal_id LIKE '%s%%' ／ idempotency_key LIKE '%s%%' ／ 文本前缀 '%s'" % (term, idem, m))
        print("  注：fund_flows 只能经 order_id / refund_record_id 定位（remark 由后端生成、不含 tag）")
        print("=" * 86)

        shift_ids = self.collect("pos_shifts", "shift_id", "terminal_id LIKE '%s%%'" % term)
        order_ids = self.collect("orders", "order_id", "idempotency_key LIKE '%s%%'" % idem)

        refund_conds = []
        if order_ids:
            refund_conds.append("order_id IN (%s)" % ",".join("'%s'" % x for x in order_ids))
        if self.has_col("order_refund_records", "refund_reason"):
            refund_conds.append("refund_reason LIKE '%s%%'" % m)          # 主锚点：前缀 [E2E:<tag>]
            refund_conds.append("refund_reason LIKE '%%%s%%'" % t)         # 兜底：旧版脚本只写裸 tag 的行
        refund_ids = self.collect("order_refund_records", "refund_id",
                                  " OR ".join(refund_conds)) if refund_conds else []

        slip_conds = []
        if shift_ids:
            slip_conds.append("shift_id IN (%s)" % ",".join(shift_ids))
        if self.has_col("deposit_slips", "remark"):
            slip_conds.append("remark LIKE '%s%%'" % m)
        slip_ids = self.collect("deposit_slips", "slip_id", " OR ".join(slip_conds)) if slip_conds else []

        # 实测（2026-10-27）：POS 下单接口**不落** orders.idempotency_key（库里为 NULL），
        # 所以 orders 侧不能靠 idempotency_key 定位 → 用「带 tag 的退款记录」反查 order_id，
        # 这是当前唯一可靠的 orders 锚点（退款是 cash_recon / ui 两个套件的必经步骤）。
        if refund_ids and self.exists("order_refund_records"):
            rc, out, _ = self.sql("SELECT DISTINCT order_id FROM order_refund_records"
                                  " WHERE refund_id IN (%s)" % ",".join(refund_ids))
            if rc == 0:
                for x in out.splitlines():
                    x = x.strip()
                    if x and x not in order_ids:
                        order_ids.append(x)

        print("锚点命中：shifts=%d orders=%d refunds=%d slips=%d" % (
            len(shift_ids), len(order_ids), len(refund_ids), len(slip_ids)))

        def lit(v):
            """数值型 id 不加引号，文本型（如 orders.order_id='O1793…'）必须加引号，
            否则会拼成 order_id IN (O1793…) → SQL 报列不存在，被误判成「列缺失」跳过。"""
            v = str(v).strip()
            return v if v.lstrip("-").isdigit() else "'%s'" % v.replace("'", "''")

        def inl(col, ids):
            return "%s IN (%s)" % (col, ",".join(lit(x) for x in ids)) if ids else "1=0"

        self.delete("refund_shift_attribution", "refund_shift_attribution",
                    " OR ".join([inl("slip_id", slip_ids), inl("refund_record_id", refund_ids)]))
        self.delete("reconciliation_items", "reconciliation_items",
                    "statement_id IN (SELECT statement_id FROM reconciliation_statements"
                    " WHERE remark LIKE '%s%%')" % m)
        self.delete("reconciliation_statements", "reconciliation_statements", "remark LIKE '%s%%'" % m)
        self.delete("deposit_slips", "deposit_slips",
                    " OR ".join([inl("slip_id", slip_ids), inl("shift_id", shift_ids),
                                 "remark LIKE '%s%%'" % m]))
        self.delete("fund_flows", "fund_flows",
                    " OR ".join([inl("order_id", order_ids), inl("refund_record_id", refund_ids)]))
        self.delete("order_refund_records", "order_refund_records",
                    " OR ".join([inl("refund_id", refund_ids), inl("order_id", order_ids)]))
        self.delete("order_payment_records", "order_payment_records", inl("order_id", order_ids))
        self.delete("order_items", "order_items", inl("order_id", order_ids))
        self.delete("kitchen_order", "kitchen_order", inl("order_id", order_ids))
        self.delete("material_consumption", "material_consumption", inl("order_id", order_ids))
        self.delete("orders", "orders", inl("order_id", order_ids))
        self.delete("pos_shifts", "pos_shifts", "terminal_id LIKE '%s%%'" % term)

        print("-" * 86)
        if self.a.apply:
            total = sum(v for v in self.applied.values() if isinstance(v, int) and v > 0)
            print("完成：共删除 %d 行（可重复执行，第二次应全为 0）" % total)
        else:
            print("DRY-RUN：未删除任何数据。确认无误后加 --apply 执行。")
        return 0


def main():
    p = argparse.ArgumentParser(description="按 tag 清理 E2E 夹具（默认 dry-run）")
    p.add_argument("--tag", required=True, help="E2E tag，如 HIK1027152338 / SUI1026192306")
    p.add_argument("--apply", action="store_true", help="真正执行删除（默认只 dry-run 计数）")
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    try:
        return Cleaner(a).run()
    except Exception as e:
        print("执行错误: %s" % e, file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())