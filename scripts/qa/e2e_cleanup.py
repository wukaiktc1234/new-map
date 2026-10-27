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

✅ **bug-③ 已修（2026-10-27）**：POS 下单接口现在**落库** orders.idempotency_key（PosOrderCreateServiceImpl），
   并有唯一部分索引 uk_orders_idempotency_key（WHERE idempotency_key IS NOT NULL）。
   ⇒ "E2E-<tag>-<n>" 现在是**可靠的 orders 定位锚点**，本脚本可直接按它清理；
   退款记录反查作为兼容旧数据（修复前下的单，key 为 NULL）的兜底路径保留。

⚠ **fund_flows.remark 由后端生成**（PosRefundFlowRecorder:132 "POS退款：退款单#… [POS]"），
   **不含 tag** → 流水只能经 order_id / refund_record_id 外键定位，**绝不能按 remark 找**，
   否则会漏删（这是 tag 体系唯一缺口，写死在此防后人误用）。

两条护栏（Owner 2026-10-27 定，别拆）：

① 【--apply 前护栏】：锚点（shifts/orders/refunds/slips）全部为 0 时【拒绝执行并 exit 2】，
   提示「无命中，可能 tag 写错」。理由：打错一个字的 tag 会让人误以为「已经清完了」。

② 【清理后自动校准余额（口径 A）】：删完 fund_flows 后，在【同一事务内】对每个受影响账户执行
   balance := 该账户最新未删流水的 balance_after。理由：bank_accounts.balance 是【派生值】，
   fund_flows.balance_after 链才是事实源；只删流水不重算余额会让巡检 A7 直接判红
   （2026-10-27 已实测踩过：账户 20 余额 19002 / 链尾 29002 / 差 10000 → A7 FAIL accounts=1）。
   若某账户已被删到【没有任何流水】→ 余额置 0，并打印「该账户已无流水，余额置 0 需人工确认是否合理」。

③ APPLY 后复查：锚点必须归零，任一非 0 → exit 2（不靠人眼看）。

用法：
    python scripts/qa/e2e_cleanup.py --tag HIK1027152338            # 默认 dry-run
    python scripts/qa/e2e_cleanup.py --tag HIK1027152338 --apply    # 真删（幂等，含余额校准）
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

    def sql(self, q, extra=None):
        argv = [self.a.psql, "-h", self.a.pg_host, "-p", str(self.a.pg_port),
                "-U", self.a.pg_user, "-d", self.a.pg_db, "-t", "-A"] + (extra or []) + ["-c", q]
        o = subprocess.run(argv, capture_output=True, text=True, timeout=180,
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


    # ---------- 删流水 + 同一事务内校准余额（口径 A） ----------
    def delete_flows_and_rebalance(self, where):
        """删除符合条件的 fund_flows，并在**同一事务内**把受影响账户的 balance 重算为
        「该账户最新未删流水的 balance_after」；没有剩余流水则置 0 并告警。

        为什么必须同事务：bank_accounts.balance 是派生值（事实源是 fund_flows.balance_after 链）。
        分开做会留下「余额已扣、流水已删」的中间态，巡检 A7 会当场判红（2026-10-27 实测踩过）。
        """
        if not self.exists("fund_flows"):
            print("  [SKIP] %-26s 表不存在" % "fund_flows")
            return 0
        n = self.scalar("SELECT count(*) FROM fund_flows WHERE %s" % where)
        if n is None:
            print("  [SKIP] %-26s 条件无法评估" % "fund_flows")
            return 0
        if not self.a.apply:
            print("  [DRY ] %-26s 命中 %d 行（--apply 时将同事务校准受影响账户余额）" % ("fund_flows", n))
            return n
        # 注意：脚本走 psql -c，**不能**在 -c 里写 \set 元命令（语法错误 → 整笔回滚，
        # 却只打印一条 [FAIL] 容易被忽略；2026-10-27 实测导致 4 条退款流水漏删、A31 判红）。
        # ON_ERROR_STOP 改用 -v 传参。
        script = r"""
BEGIN;
CREATE TEMP TABLE aff ON COMMIT DROP AS
  SELECT DISTINCT account_id FROM fund_flows WHERE {where};
WITH del AS (DELETE FROM fund_flows WHERE {where} RETURNING 1)
  SELECT 'deleted=' || count(*) FROM del;
UPDATE bank_accounts b
   SET balance = COALESCE((SELECT f.balance_after FROM fund_flows f
                            WHERE f.account_id = b.account_id AND f.deleted = 0
                            ORDER BY f.flow_id DESC LIMIT 1), 0)
 WHERE b.account_id IN (SELECT account_id FROM aff);
SELECT 'acct=' || a.account_id || '|' || a.balance || '|flows=' ||
       (SELECT count(*) FROM fund_flows f WHERE f.account_id = a.account_id AND f.deleted = 0)
  FROM bank_accounts a WHERE a.account_id IN (SELECT account_id FROM aff) ORDER BY a.account_id;
COMMIT;
""" .format(where=where)
        rc, out, err = self.sql(script, extra=["-v", "ON_ERROR_STOP=1"])
        if rc != 0:
            print("  [FAIL] %-26s 删除+校准事务失败（已回滚）：%s" % ("fund_flows", err[:160]))
            self.applied["fund_flows"] = -1
            return 0
        deleted = 0
        for line in out.splitlines():
            line = line.strip()
            if line.startswith("deleted="):
                deleted = int(line.split("=")[1])
            elif line.startswith("acct="):
                aid, bal, fl = line[5:].split("|")
                flows_n = int(fl.split("=")[1])
                print("  [CAL ] %-26s account=%s → balance=%s（剩余流水 %d）" % (
                    "余额校准(口径A)", aid, bal, flows_n))
                if flows_n == 0:
                    print("  [WARN] 账户 %s 已无流水，余额置 0 —— 需人工确认是否合理" % aid)
        print("  [DEL ] %-26s 删除 %d 行（同事务已校准余额）" % ("fund_flows", deleted))
        self.applied["fund_flows"] = deleted
        return deleted
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

        # --apply 前护栏（Owner 2026-10-27）：锚点全 0 时拒绝执行 —— 打错一个字的 tag 会「静默清空」
        if self.a.apply and not (shift_ids or order_ids or refund_ids or slip_ids):
            print("!! 拒绝 --apply：锚点全部为 0，可能 --tag 写错（或该 tag 已清理过）。")
            print("!! 请先不带 --apply 跑一次 dry-run 确认命中，再执行。")
            return 2

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
        # 删流水 + 同事务校准余额（口径 A：balance := 最新未删流水 balance_after）
        self.delete_flows_and_rebalance(
            " OR ".join([inl("order_id", order_ids), inl("refund_record_id", refund_ids)]))
        self.delete("order_refund_records", "order_refund_records",
                    " OR ".join([inl("refund_id", refund_ids), inl("order_id", order_ids)]))
        self.delete("order_payment_records", "order_payment_records", inl("order_id", order_ids))
        self.delete("order_items", "order_items", inl("order_id", order_ids))
        self.delete("kitchen_order", "kitchen_order", inl("order_id", order_ids))
        self.delete("material_consumption", "material_consumption", inl("order_id", order_ids))
        self.delete("orders", "orders", inl("order_id", order_ids))
        self.delete("pos_shifts", "pos_shifts", "terminal_id LIKE '%s%%'" % term)

        # APPLY 后自动复查：锚点必须归零，否则 exit 2（不靠人眼看）
        if self.a.apply:
            left = {
                "pos_shifts(terminal)": self.scalar("SELECT count(*) FROM pos_shifts"
                                                    " WHERE terminal_id LIKE '%s%%'" % term),
                "refunds(reason)": self.scalar("SELECT count(*) FROM order_refund_records"
                                               " WHERE refund_reason LIKE '%s%%'"
                                               " OR refund_reason LIKE '%%%s%%'" % (m, t))
                if (self.exists("order_refund_records")
                    and self.has_col("order_refund_records", "refund_reason")) else 0,
                "slips(remark)": self.scalar("SELECT count(*) FROM deposit_slips"
                                             " WHERE remark LIKE '%s%%'" % m)
                if (self.exists("deposit_slips") and self.has_col("deposit_slips", "remark")) else 0,
            }
            dirty = {k: v for k, v in left.items() if v}
            print("-" * 86)
            print("复查（APPLY 后锚点应为 0）：%s" % left)
            if dirty:
                print("!! 复查不通过：仍有残留 %s → exit 2（不算清理成功）" % dirty)
                return 2

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