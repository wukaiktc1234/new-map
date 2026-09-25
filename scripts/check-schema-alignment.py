#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
P0-SCHEMA-SINGLE-SOURCE-001 — 实体/Flyway 对齐检查（最小实现）

用法：
    python scripts/check-schema-alignment.py [entity-name ...]
    （不带参数 = 检查全部实体；带参数 = 只检查指定实体，如 FoodCategory）

逻辑：
    1. 解析 backend/src/main/resources/db/migration/V*.sql 中每个表的
       CREATE TABLE 列集合（同表多次定义时，文件名序靠后的为准——与 Flyway 版本序一致）。
    2. 解析 backend/src/main/java/com/foodtraceability/entity/*.java 的
       @TableName / @TableId / @TableField，推导实体期望的列集合。
    3. 报告：实体列 ⊄ Flyway 列 的错位（含主键）；实体无对应 Flyway 表亦报告。

注意：
    - 只做"实体映射列是否存在于 Flyway 定义"的单向检查（足够拦截
      P1-POS-MENU-500-001 类事故）；不做类型/默认值比对。
    - 未标注 @TableField 的字段按 camelCase→snake_case 推导（MyBatis-Plus 默认）。
    - 退出码：0 = 无新增错位；1 = 存在错位（收口前必须处置或单独立卡）。
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MIG_DIR = ROOT / "backend" / "src" / "main" / "resources" / "db" / "migration"
ENTITY_DIR = ROOT / "backend" / "src" / "main" / "java" / "com" / "foodtraceability" / "entity"


def flyway_columns():
    tables = {}
    for sql in sorted(MIG_DIR.glob("V*.sql")):
        text = sql.read_text(encoding="utf-8", errors="replace")
        for m in re.finditer(r"CREATE TABLE\s+(?:IF NOT EXISTS\s+)?[\"']?(\w+)[\"']?\s*\((.*?)\);",
                             text, re.S | re.I):
            table, body = m.group(1).lower(), m.group(2)
            cols = set()
            for line in body.splitlines():
                line = line.strip().rstrip(",")
                if not line or re.match(r"(PRIMARY KEY|FOREIGN KEY|UNIQUE|CONSTRAINT|CHECK|INDEX|COMMENT)", line, re.I):
                    continue
                col = re.match(r"[\"']?(\w+)[\"']?\s", line)
                if col:
                    cols.add(col.group(1).lower())
            tables.setdefault(table, set()).update(cols)  # 后解析的 migration 叠加（ADD COLUMN 语义近似）
    return tables


def entity_mappings():
    out = []
    for java in sorted(ENTITY_DIR.glob("*.java")):
        text = java.read_text(encoding="utf-8", errors="replace")
        tm = re.search(r'@TableName\(\s*(?:value\s*=\s*)?"(\w+)"', text)
        if not tm:
            continue
        table = tm.group(1).lower()
        cols = {}
        for f in re.finditer(r"@TableId\s*(?:\(\s*(?:value\s*=\s*)?\"(\w+)\"[^)]*\))?\s*\r?\n\s*private\s+\S+\s+(\w+)",
                             text):
            cols[f.group(2)] = (f.group(1) or "id").lower()
        for f in re.finditer(r'@TableField\(\s*(?:value\s*=\s*)?"(\w+)"[^)]*\)\s*\r?\n\s*private\s+\S+\s+(\w+)',
                             text):
            cols[f.group(2)] = f.group(1).lower()
        for f in re.finditer(r"\r?\n\s+private\s+(?:String|Long|Integer|BigDecimal|LocalDateTime|Boolean|byte\[\])\s+(\w+)\s*;",
                             text):
            if f.group(1) not in cols:  # 无注解字段 → MyBatis-Plus 驼峰转下划线
                cols[f.group(1)] = re.sub(r"([A-Z])", r"_\1", f.group(1)).lower()
        out.append((java.name, table, cols))
    return out


def main():
    only = set(a for a in sys.argv[1:] if not a.startswith("-"))
    tables = flyway_columns()
    mismatches, no_table = [], []
    checked = 0
    entity_tables = set()
    for name, table, cols in entity_mappings():
        if only and not any(o.lower() in name.lower() for o in only):
            continue
        checked += 1
        entity_tables.add(table)
        if table not in tables:
            no_table.append(f"[NO-TABLE ] {name}: @TableName(\"{table}\") 在 Flyway migration 中无 CREATE TABLE（可能由 DatabaseFixConfig/遗留脚本建表）")
            continue
        missing = sorted(c for c in set(cols.values()) if c not in tables[table])
        if missing:
            mismatches.append(f"[MISMATCH] {name}: 表 {table} 缺实体映射列 {missing}")
    no_entity = sorted(t for t in tables if t not in entity_tables)
    print(f"检查实体 {checked} 个；Flyway 表 {len(tables)} 张")
    print(f"分类统计：MISMATCH={len(mismatches)}  NO-TABLE={len(no_table)}  NO-ENTITY={len(no_entity)}")
    for p in mismatches + no_table:
        print(" ", p)
    if "-v" in sys.argv:
        print("NO-ENTITY（Flyway 有表、无实体引用，通常为中间表/历史表，仅计数不处置）：")
        for t in no_entity:
            print("  ", t)
    if mismatches or no_table:
        print("存在错位：收口前必须处置或单独立卡")
        sys.exit(1)
    print("OK：全部对齐")


if __name__ == "__main__":
    main()
