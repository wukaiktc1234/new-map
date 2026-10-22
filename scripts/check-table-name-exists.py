#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
check-table-name-exists.py — @TableName 与活库真实对象（表/视图）对齐闸门

背景（夜间巡航 2026-10-23，雷达 2 实测）：
    仓内 368 处 @TableName 里有 67 处指向活库中根本不存在的对象。其中 40 处的
    Mapper 已被 Controller 链路注入，但 selectList 返回空列表、写入静默失败，
    **不会出现在任何错误日志里** —— 这类缺陷靠日志排查永远发现不了。
    既有 scripts/check-schema-alignment.py 只比对「实体 vs Flyway CREATE TABLE」，
    且只扫 entity/ 顶层目录（漏掉 entity/finance、entity/marketing 等子包），
    无法发现「Flyway 有表但活库是另一张表」以及「数据在手工 *_legacy 表」。

与既有脚本的分工：
    check-schema-alignment.py  → 实体列 ⊄ Flyway 列定义（静态，无 DB）
    本脚本                      → @TableName ⊄ 活库对象（动态，需连库）

用法：
    python scripts/check-table-name-exists.py                # 检查全部实体
    python scripts/check-table-name-exists.py --db           # 同上（默认已连库）
    python scripts/check-table-name-exists.py --no-db        # 跳过 DB，仅列映射
    python scripts/check-table-name-exists.py --update-baseline   # 重写基线（慎用）
    python scripts/check-table-name-exists.py FoodCategory   # 只看指定实体

退出码：
    0 = 无「基线外」的缺失映射
    1 = 存在基线外缺失映射（收口前必须处置或登记基线）
    2 = 环境问题（连不上库 / 找不到 psql）

环境变量：
    PGHOST / PGPORT / PGDATABASE / PGUSER / PGPASSWORD  （与 psql 一致）
    默认 localhost:5432 / food_traceability / postgres
"""
import argparse
import os
import re
import subprocess
import sys
from pathlib import Path

# Windows 控制台默认 GBK，中文报告会乱码 —— 统一按 UTF-8 输出（配合 chcp 65001 使用）
for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8")
    except Exception:
        pass

ROOT = Path(__file__).resolve().parent.parent
ENTITY_DIR = ROOT / "backend" / "src" / "main" / "java" / "com" / "foodtraceability" / "entity"
BASELINE = ROOT / "docs" / "night-patrol" / "schema-drift-baseline.txt"

PSQL_CANDIDATES = [
    os.environ.get("PSQL_BIN", ""),
    str(ROOT / "PostgreSQL" / "18" / "bin" / "psql.exe"),
    "psql",
]

TABLE_RE = re.compile(r'@TableName\s*\(\s*(?:value\s*=\s*)?"([^"]+)"')


def find_psql():
    for cand in PSQL_CANDIDATES:
        if not cand:
            continue
        if os.path.sep in cand or cand.endswith(".exe"):
            if Path(cand).is_file():
                return cand
            continue
        from shutil import which
        p = which(cand)
        if p:
            return p
    return None


def live_objects(psql):
    """返回 (tables, views) 两个小写集合。"""
    sql = (
        "select table_name || '|' || table_type from information_schema.tables "
        "where table_schema = 'public';"
    )
    env = dict(os.environ)
    env.setdefault("PGCLIENTENCODING", "UTF8")
    env.setdefault("PGHOST", "localhost")
    env.setdefault("PGPORT", "5432")
    env.setdefault("PGDATABASE", "food_traceability")
    env.setdefault("PGUSER", "postgres")
    env.setdefault("PGPASSWORD", "123456")
    out = subprocess.run(
        [psql, "-t", "-A", "-F", "|", "-c", sql],
        capture_output=True, text=True, env=env, timeout=60,
    )
    if out.returncode != 0:
        print("[FATAL] psql 查询失败：\n" + (out.stderr or "").strip(), file=sys.stderr)
        sys.exit(2)
    tables, views = set(), set()
    for line in out.stdout.splitlines():
        line = line.strip()
        if not line or "|" not in line:
            continue
        name, kind = line.split("|", 1)
        name = name.strip().lower()
        if kind.strip().upper() == "VIEW":
            views.add(name)
        else:
            tables.add(name)
    return tables, views


def entity_mappings():
    """递归扫 entity/ 全部子包，返回 [(相对路径, 实体类名, 表名)]。"""
    out = []
    if not ENTITY_DIR.is_dir():
        print(f"[FATAL] 找不到实体目录 {ENTITY_DIR}", file=sys.stderr)
        sys.exit(2)
    for java in sorted(ENTITY_DIR.rglob("*.java")):
        text = java.read_text(encoding="utf-8", errors="replace")
        for m in TABLE_RE.finditer(text):
            out.append((str(java.relative_to(ROOT)).replace("\\", "/"),
                        java.stem, m.group(1).strip()))
    return out


def load_baseline():
    if not BASELINE.is_file():
        return set()
    known = set()
    for line in BASELINE.read_text(encoding="utf-8").splitlines():
        line = line.split("#", 1)[0].strip()
        if line:
            known.add(line.lower())
    return known


def main():
    ap = argparse.ArgumentParser(add_help=True)
    ap.add_argument("filters", nargs="*", help="只检查类名/路径包含这些关键字的实体")
    ap.add_argument("--no-db", action="store_true", help="不连库，仅输出映射清单")
    ap.add_argument("--update-baseline", action="store_true", help="用当前缺失项重写基线")
    args = ap.parse_args()

    mappings = entity_mappings()
    if args.filters:
        mappings = [m for m in mappings
                    if any(f.lower() in (m[0] + " " + m[1]).lower() for f in args.filters)]

    if args.no_db:
        for path, cls, table in mappings:
            print(f"{table}\t{cls}\t{path}")
        print(f"\n[INFO] 共 {len(mappings)} 处 @TableName（未连库校验）")
        return 0

    psql = find_psql()
    if not psql:
        print("[FATAL] 找不到 psql，可用 PSQL_BIN 环境变量指定", file=sys.stderr)
        return 2
    tables, views = live_objects(psql)

    missing, ok, on_view, plural_hit = [], [], [], []
    for path, cls, table in mappings:
        key = table.lower()
        if key in tables:
            ok.append((path, cls, table))
        elif key in views:
            on_view.append((path, cls, table))
        else:
            # 单复数诊断：本表缺失，但另一侧存在 → 极可能是「代码读写空表」
            alt = []
            if key.endswith("s"):
                alt.append(key[:-1])
            else:
                alt.append(key + "s")
            if key.endswith("ies"):
                alt.append(key[:-3] + "y")
            hit = next((a for a in alt if a in tables or a in views), None)
            if hit:
                plural_hit.append((path, cls, table, hit))
            else:
                missing.append((path, cls, table))

    known = load_baseline()
    if args.update_baseline:
        BASELINE.parent.mkdir(parents=True, exist_ok=True)
        lines = [
            "# schema-drift-baseline.txt — 已知 @TableName 漂移基线（夜间巡航 2026-10-23 生成）",
            "# 格式：每行一个表名（小写），# 后为注释。",
            "# 语义：基线内的缺失映射不触发退出码 1；基线外新增缺失 = 回归，必须处置。",
            "# 依据：docs/night-patrol/radar2-schema-drift-report.md",
            "",
        ]
        for path, cls, table in sorted(set(missing + [(p, c, t) for p, c, t, _ in plural_hit]),
                                       key=lambda x: x[2]):
            lines.append(f"{table.lower()}    # {cls}  ({path})")
        BASELINE.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"[OK] 基线已重写：{BASELINE.relative_to(ROOT)}（{len(missing) + len(plural_hit)} 项）")
        return 0

    def known_or_new(rows, idx=2):
        return ([r for r in rows if r[idx].lower() in known],
                [r for r in rows if r[idx].lower() not in known])

    miss_known, miss_new = known_or_new(missing)
    dual_known, dual_new = known_or_new(plural_hit)

    print(f"实体映射总数 : {len(mappings)}")
    print(f"活库对象     : {len(tables)} 表 / {len(views)} 视图")
    print(f"映射命中表   : {len(ok)}")
    print(f"映射命中视图 : {len(on_view)}")
    print(f"映射缺失     : {len(missing)}（基线内 {len(miss_known)} / 新增 {len(miss_new)}）")
    print(f"单复数错侧   : {len(plural_hit)}（基线内 {len(dual_known)} / 新增 {len(dual_new)}）")

    if on_view:
        print("\n[VIEW] @TableName 指向视图（可读不可写，写操作必失败）：")
        for path, cls, table in on_view:
            print(f"  - {cls:<32} -> {table:<34} {path}")

    if dual_new:
        print("\n[DUAL] 新增单复数错侧（代码读写的是空表，另一侧才有数据）：")
        for path, cls, table, hit in dual_new:
            print(f"  - {cls:<32} -> {table:<30} 实际数据在 {hit}   {path}")

    if miss_new:
        print("\n[MISSING] 新增缺失映射（@TableName 指向活库不存在的对象）：")
        for path, cls, table in miss_new:
            print(f"  - {cls:<32} -> {table:<34} {path}")

    if miss_new or dual_new:
        print("\n[FAIL] 存在基线外漂移。处置方式：修 @TableName / 补 Flyway 迁移 / "
              "或登记进 docs/night-patrol/schema-drift-baseline.txt（需 Owner 批准）。")
        return 1

    print("\n[PASS] 无基线外漂移。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
