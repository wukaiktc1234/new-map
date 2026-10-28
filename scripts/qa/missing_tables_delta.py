#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""59 → 34 的逐表明细：对比「基线缺表清单」与「当前库实际缺表」，输出补齐/剩余/分类。

基线清单默认取 .verify-tmp/missing-baseline-59.txt（一行一个表名，由本脚本 --snapshot-baseline 生成）。
判据（2026-10-28 更正）：『有写入』必须以 Mapper 实例调用为准（xxxMapper.insert/update/delete/save/removeById），
不能用「文件里出现实体类名」代替（food_trace 就是被这样误判的）。

用法：
  python scripts/qa/missing_tables_delta.py --snapshot-baseline        # 记录当前缺表为基线
  python scripts/qa/missing_tables_delta.py                            # 出 59→N 明细
"""
from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(r"P:/my-new-project")
BE = ROOT / "backend" / "src" / "main" / "java"
FE = [ROOT / "frontend" / "src", ROOT / "frontend-pos" / "src"]
PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
BASELINE = ROOT / ".verify-tmp" / "missing-baseline-59.txt"
OUT = ROOT / "docs" / "audit" / "missing-tables-delta.md"
DEAD = {"sys_permissions", "sys_roles", "sys_stores", "inventory_log", "v_inventory_summary",
        "operation_type", "store_inventory_log"}
NO_WRITER = {"food_trace"}   # 2026-10-28 复核：无任何 Mapper 注入/调用 → 实体指向空表


def psql(sql):
    p = subprocess.run([PSQL, "-h", "localhost", "-p", "5432", "-U", "postgres", "-d", "food_traceability",
                        "-t", "-A", "-c", sql], capture_output=True, text=True, encoding="utf-8",
                       errors="replace", env=dict(os.environ, PGPASSWORD="123456", PGCLIENTENCODING="UTF8"))
    return [x for x in (p.stdout or "").splitlines() if x.strip()]


def entity_tables():
    t = {}
    for p in (BE / "com/foodtraceability/entity").rglob("*.java"):
        txt = p.read_text(encoding="utf-8", errors="replace")
        m = re.search(r'@TableName[(]"([A-Za-z0-9_]+)"[)]', txt)
        if m:
            t.setdefault(m.group(1), p.stem)
    return t


def have_tables():
    return {r.split("|")[0] for r in psql("SELECT table_name FROM information_schema.tables WHERE table_schema='public'")}


def mapper_call_hits(cls):
    """只认 Mapper 实例调用（可用性判据），返回 [(file, line)]。"""
    if not cls:
        return []
    pat_mapper = cls + "Mapper"
    pat = re.compile(r"\b" + re.escape(pat_mapper) + r"\b.*\.(insert|update|delete|save|remove|select)", re.I)
    pat2 = re.compile(r"\.(insert|update|delete|save|remove)[A-Za-z]*\(")
    hits = []
    for p in BE.rglob("*.java"):
        if p.stem == pat_mapper:
            continue
        txt = p.read_text(encoding="utf-8", errors="replace")
        if pat_mapper not in txt:
            continue
        for i, ln in enumerate(txt.splitlines(), 1):
            if pat_mapper in ln and pat2.search(ln) or pat.search(ln):
                hits.append((str(p.relative_to(BE)), i))
                break
    return hits


def fe_hits(table, cls):
    kebab = table.replace("_", "-")
    hits = []
    for d in FE:
        if not d.exists():
            continue
        for p in d.rglob("*"):
            if not p.is_file() or p.suffix not in (".ts", ".js", ".vue"):
                continue
            txt = p.read_text(encoding="utf-8", errors="replace")
            if table in txt or kebab in txt:
                for i, ln in enumerate(txt.splitlines(), 1):
                    if table in ln or kebab in ln:
                        hits.append((str(p.relative_to(d)), i))
                        break
                break
    return hits


def classify(table, cls):
    if table in NO_WRITER:
        return "实体指向空表（登记，不修）"
    if table in DEAD:
        return "废弃表直接删"
    w = mapper_call_hits(cls)
    f = fe_hits(table, cls)
    if w and f:
        return "核心业务必做（有写入+前端调用）"
    if w:
        return "核心业务必做（有写入）"
    if f:
        return "外围功能先下线（前端半成品调用）"
    return "外围功能先下线（无写入无调用）"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--snapshot-baseline", action="store_true")
    a = ap.parse_args()
    ent, have = entity_tables(), have_tables()
    missing_now = sorted(set(ent) - have)
    if a.snapshot_baseline:
        BASELINE.parent.mkdir(parents=True, exist_ok=True)
        BASELINE.write_text("\n".join(missing_now) + "\n", encoding="utf-8")
        print("baseline written: %s (%d 张)" % (BASELINE, len(missing_now)))
        return 0
    base = [x.strip() for x in BASELINE.read_text(encoding="utf-8").splitlines() if x.strip()] if BASELINE.exists() else []
    created = [t for t in base if t in have]
    remain = [t for t in missing_now if t in base] or missing_now

    L = []
    L.append("# 缺表收敛明细（基线 → 当前）")
    L.append("")
    L.append("基线 %d 张 → 当前缺 %d 张 ｜ 已补齐 **%d** 张 ｜ 仍缺 **%d** 张"
             % (len(base), len(missing_now), len(created), len(remain)))
    L.append("")
    L.append("判据：补齐 = 该表已存在于 `information_schema.tables`；『有写入』只认 Mapper 实例调用（2026-10-28 更正）。")
    L.append("")
    L.append("## 已补齐（%d 张）" % len(created))
    L.append("")
    L.append("| 表名 | 实体 | 分类 |")
    L.append("|---|---|---|")
    for t in created:
        L.append("| `%s` | `%s.java` | %s |" % (t, ent.get(t, "?"), classify(t, ent.get(t))))
    L.append("")
    L.append("## 仍缺（%d 张）" % len(remain))
    L.append("")
    L.append("| 表名 | 实体 | 分类 |")
    L.append("|---|---|---|")
    for t in remain:
        L.append("| `%s` | `%s.java` | %s |" % (t, ent.get(t, "?"), classify(t, ent.get(t))))
    L.append("")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(L) + "\n", encoding="utf-8")
    print("baseline=%d missing_now=%d created=%d remain=%d" % (len(base), len(missing_now), len(created), len(remain)))
    print("written: %s" % OUT)
    return 0


if __name__ == "__main__":
    sys.exit(main())