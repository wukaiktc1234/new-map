#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""Schema 一致性门禁（Lead 2026-10-30 P0「部署阻断」）。

背景：
  生产库 orders = **54 列**，干净 Flyway 库（food_traceability_test）= **30 列**，
  两者 flyway_schema_history 最大版本都是 999（都「全绿」）。
  → 24 列（含 final_amount / paid_amount / payment_status / shift_id）**只存在于生产库、无迁移**。
  → 任何「新环境部署 / 灾备恢复 / 新建分店库」都会缺这 24 列 → 启动即崩或功能全废。
  → 这解释了 from-zero gate 一直全绿却毫无意义：它只验证「迁移跑完」，不验证「结果形状」。

判据：
  ① 基准库可信度自证（否则 PASS 无意义）：
     · 两库 flyway 最大 version 必须相等；
     · 两库表名集合必须相等（差异说明有人手工改过基准库或生产库）；
     任一不成立 → **EXIT=3（基准不可信）**，不许用不可信基得出 PASS。
  ② 逐表逐列比 (column_name, data_type, char_len, num_precision, num_scale, is_nullable)：
     · 仅生产有 → FAIL（迁移缺失）；仅干净库有 → FAIL（生产少列/基准被改）；类型不一致 → FAIL。
  ③ 输出必须**逐列列名**，不许只说数量。

用法：python schema_parity_check.py [--db-clean food_traceability_test] [--db-prod food_traceability]
退出码：0 PASS / 2 BLOCK / 3 基准不可信或环境不可用
"""
import os
import subprocess
import sys

PSQL = os.environ.get("QA_PSQL", r"P:\my-new-project\PostgreSQL\18\bin\psql.exe")
CLEAN_DB = os.environ.get("QA_CLEAN_DB", "food_traceability_test")
PROD_DB = os.environ.get("QA_PROD_DB", "food_traceability")
ENC = {"encoding": "utf-8", "errors": "replace"}


def q(db, sql):
    env = dict(os.environ, PGPASSWORD=os.environ.get("PGPASSWORD", "123456"),
               PGCLIENTENCODING="UTF8")
    r = subprocess.run([PSQL, "-h", "localhost", "-p", "5432", "-U", "postgres",
                        "-d", db, "-A", "-t", "-c", sql], capture_output=True, text=True, env=env, **ENC)
    if r.returncode != 0:
        return None, (r.stderr or "").strip()[:200]
    return (r.stdout or "").strip(), None


EXCL_FILE = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
                         "docs", "audit", "schema-exclusions.txt")


def load_exclusions():
    """读排除清单：模式 | 理由 | 登记人 | 时间。被排除项**仍然打印**但标 [EXCLUDED]。"""
    pats = []
    if not os.path.exists(EXCL_FILE):
        return pats
    for line in open(EXCL_FILE, encoding="utf-8"):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        parts = [x.strip() for x in line.split("|")]
        if parts and parts[0]:
            pats.append(parts[0])
    return pats


def is_excluded(name, pats):
    import fnmatch
    return any(fnmatch.fnmatch(name, p) for p in pats)


COLS_SQL = (
    "SELECT table_name || chr(9) || column_name || chr(9) || data_type || chr(9) || "
    "COALESCE(character_maximum_length::text, '') || chr(9) || "
    "COALESCE(numeric_precision::text, '') || chr(9) || "
    "COALESCE(numeric_scale::text, '') || chr(9) || is_nullable "
    "FROM information_schema.columns WHERE table_schema = 'public' "
    "ORDER BY table_name, column_name")


def load(db):
    out, err = q(db, COLS_SQL)
    if out is None:
        return None, err
    cols = {}
    for line in out.splitlines():
        parts = line.split("\t")
        if len(parts) < 7:
            continue
        t, c, dt, clen, prec, scale, nullable = parts[:7]
        cols[(t, c)] = (dt, clen, prec, scale, nullable)
    return cols, None


def main():
    pats = load_exclusions()
    if pats:
        print("[INFO] schema 排除清单 %d 条（%s）" % (len(pats), EXCL_FILE))
    clean, e1 = load(CLEAN_DB)
    prod, e2 = load(PROD_DB)
    if clean is None or prod is None:
        print("[ENV] 读取失败 clean=%s prod=%s" % (e1, e2))
        return 3

    fails = 0
    # ---- ① 基准库可信度自证 ----
    v_clean, _ = q(CLEAN_DB, "SELECT COALESCE(max(version), '-') FROM flyway_schema_history WHERE success")
    v_prod, _ = q(PROD_DB, "SELECT COALESCE(max(version), '-') FROM flyway_schema_history WHERE success")
    if v_clean == v_prod:
        print("[PASS] flyway 最大版本一致 = %s" % v_clean)
    else:
        print("[FAIL] flyway 版本不一致：干净库=%s 生产=%s → **基准不可信**" % (v_clean, v_prod))
        print("汇总：基准不可信 → EXIT=3（先修基准库再比，不许用不可信基准给 PASS）")
        return 3

    tabs_clean = {t for (t, _c) in clean}
    tabs_prod = {t for (t, _c) in prod}
    only_t = {t for t in (tabs_prod - tabs_clean) if not is_excluded(t, pats)}
    lack_t = {t for t in (tabs_clean - tabs_prod) if not is_excluded(t, pats)}
    if not only_t and not lack_t:
        print("[PASS] 表名集合一致（%d 张表）" % len(tabs_prod))
    else:
        fails += 1
        if only_t:
            print("[FAIL] 仅生产有表 %d 张：%s" % (len(only_t), ", ".join(sorted(only_t))))
        if lack_t:
            print("[FAIL] 仅干净库有表 %d 张：%s" % (len(lack_t), ", ".join(sorted(lack_t))))

    # ---- ② 逐列比较 ----
    only_prod_all = sorted(set(prod) - set(clean))
    only_clean_all = sorted(set(clean) - set(prod))
    # ★ 互斥口径（Lead 2026-10-30）：每一条列级差异只能被 B/D/E 中**一个**计数。
    #   整表缺失 → 只计入 A；整表多余 → 只计入 C；其列不得再进 B/D。
    #   修前把「3 张整表的 39 列」也计进 B（113→95 的 39 = 13+17+9），
    #   与「把退款减进收款（−174.00）」是同一个错误：一个数字混两种语义。
    tabs_both = {t for (t, _c) in prod} & {t for (t, _c) in clean}
    only_prod = [k for k in only_prod_all if not is_excluded(k[0], pats) and k[0] in tabs_both]
    only_clean = [k for k in only_clean_all if not is_excluded(k[0], pats) and k[0] in tabs_both]
    excluded_prod = [k for k in only_prod_all if is_excluded(k[0], pats)]
    excluded_clean = [k for k in only_clean_all if is_excluded(k[0], pats)]
    if excluded_prod or excluded_clean:
        names = sorted({k[0] for k in excluded_prod} | {k[0] for k in excluded_clean})
        print("[EXCLUDED] %d 张表按清单排除（不计 FAIL，明细见 schema-exclusions.txt）：%s"
              % (len(names), ", ".join(names)))
    mismatch = []
    for k in sorted(set(prod) & set(clean)):
        if prod[k] != clean[k]:
            mismatch.append((k, prod[k], clean[k]))

    if only_prod:
        fails += 1
        by_table = {}
        for (t, c) in only_prod:
            by_table.setdefault(t, []).append(c)
        for t in sorted(by_table):
            cols = sorted(by_table[t])
            print("[FAIL] 仅生产有 %d 列（表 %s）：%s" % (len(cols), t, ", ".join(cols)))
    if only_clean:
        fails += 1
        by_table = {}
        for (t, c) in only_clean:
            by_table.setdefault(t, []).append(c)
        for t in sorted(by_table):
            print("[FAIL] 仅干净库有 %d 列（表 %s）：%s" % (len(by_table[t]), t, ", ".join(sorted(by_table[t]))))
    if mismatch:
        fails += 1
        print("[FAIL] 类型/可空性不一致 %d 处：" % len(mismatch))
        for (t, c), pv, cv in mismatch[:30]:
            print("       %s.%s 生产=%s 干净库=%s" % (t, c, pv, cv))

    # ★（Lead 2026-10-30）**一个指标只表达一件事**：四类差异各自独立计数。
    #   修前把「缺列」与「反向多余列」加总成 246，导致递减验收失去意义。
    tabs_only_prod = len({t for (t, _c) in prod} - {t for (t, _c) in clean} - {t for t in
                          ({t for (t, _c) in prod} - {t for (t, _c) in clean}) if is_excluded(t, pats)})
    tabs_only_clean = len({t for (t, _c) in clean} - {t for (t, _c) in prod} - {t for t in
                           ({t for (t, _c) in clean} - {t for (t, _c) in prod}) if is_excluded(t, pats)})
    cols_only_prod = len(only_prod)
    cols_only_clean = len(only_clean)
    excl_count = len({k[0] for k in excluded_prod} | {k[0] for k in excluded_clean})
    print("-" * 78)
    print("[STAT] 仅生产有表 = %d ｜ 仅生产有列 = %d" % (tabs_only_prod, cols_only_prod))
    print("[STAT] 仅干净库有表 = %d ｜ 仅干净库有列 = %d" % (tabs_only_clean, cols_only_clean))
    print("[STAT] 类型/可空性不一致 = %d" % len(mismatch))
    print("[STAT] 已排除（exclusions）= %d 项（明细见 docs/audit/schema-exclusions.txt）" % excl_count)
    print("[RESULT] EXIT=%d" % (2 if fails else 0))
    return 2 if fails else 0
    return 2 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
