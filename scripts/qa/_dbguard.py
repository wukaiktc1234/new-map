#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""DBGUARD —— 写操作目标库防呆（Lead 2026-10-29 死命令 1/2 的落地件）。

用途：任何**含写操作**（INSERT/UPDATE/DELETE/TRUNCATE）的 QA 脚本，启动第一步必须调用
      guard()；若目标库不在白名单（food_traceability_test / ft_gate_* / ft_exc / ft_r4），
      立刻打印 [DBGUARD] 拒绝并 exit 2，**一行 SQL 都不执行**。

为什么需要：2026-10-29 的事故全部是「脚本不知道自己在删什么」——
  deposit_slip_balance_guard_selftest.py 里有一句 DELETE FROM bank_accounts WHERE store_id=%d，
  在共享库跑一次就可能删掉真实门店的资金账户。

用法（每个含写操作的脚本开头三行）：
    from _dbguard import guard
    guard(__file__)

目标库来源优先级：命令行 --pg-db/--database → 环境变量 QA_PGDB / PGDATABASE → 默认 food_traceability
（默认值故意是**共享库**，这样忘记改参数的脚本会被拦下，而不是悄悄写进测试库）。
"""
import os
import re
import sys

WRITABLE_PREFIXES = ("food_traceability_test", "ft_gate_", "ft_exc", "ft_r4")
SHARED_DBS = ("food_traceability", "food_traceability_prod", "postgres", "template1")


def resolve_target(argv, default="food_traceability"):
    """从命令行/环境变量解析目标库。"""
    for i, a in enumerate(argv):
        if a in ("--pg-db", "--database", "-d") and i + 1 < len(argv):
            return argv[i + 1]
        m = re.match(r"--(?:pg-db|database)=(.+)$", a)
        if m:
            return m.group(1)
    return os.environ.get("QA_PGDB") or os.environ.get("PGDATABASE") or default


def is_writable(db):
    return any((db or "").startswith(p) for p in WRITABLE_PREFIXES)


def guard(script_file, argv=None, default="food_traceability"):
    """写操作脚本入口防呆。返回 0 表示放行；否则调用方应 sys.exit(2)。"""
    argv = list(sys.argv if argv is None else argv)
    db = resolve_target(argv[1:], default=default)
    name = os.path.basename(script_file)
    if is_writable(db):
        print("[DBGUARD] %s target=%s mode=write verdict=ALLOW" % (name, db))
        return 0
    reason = "白名单为 food_traceability_test / ft_gate_* / ft_exc / ft_r4"
    if db in SHARED_DBS:
        reason = "共享库禁止写操作（死命令 1：测试/门禁脚本一律不得写真实业务表）"
    print("[DBGUARD] 拒绝对 %s 执行写操作：%s" % (db, reason))
    print("[DBGUARD] %s target=%s mode=write verdict=REFUSE" % (name, db))
    print("[DBGUARD] 如需运行：QA_PGDB=food_traceability_test python %s（或 --pg-db food_traceability_test）" % name)
    return 2


if __name__ == "__main__":
    arg = sys.argv[1] if len(sys.argv) > 1 else "food_traceability"
    print("writable(%s) = %s" % (arg, is_writable(arg)))
    sys.exit(0 if is_writable(arg) else 1)
