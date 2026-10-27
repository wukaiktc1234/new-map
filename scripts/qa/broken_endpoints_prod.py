#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生产环境已损坏接口清单：表在生产库不存在，但有 Controller 入口 → 调用即 SQL 报错。

输出：表名 | Controller 类#方法 + @*Mapping 路径 | 生产库有表 | 影响面 | 建议
"""
from __future__ import annotations

import os, re, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'backend' / 'src' / 'main' / 'java'
PSQL = str(ROOT / 'PostgreSQL' / '18' / 'bin' / 'psql.exe')
TABLE_RE = re.compile(r'@TableName\(\s*"([^"]+)"\s*\)')
CLASS_MAPPING_RE = re.compile(r'@RequestMapping\(\s*"([^"]+)"')
MAPPING_RE = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*\(([^)]*)\)')


def q(sql, db='food_traceability'):
    env = dict(os.environ, PGPASSWORD='123456', PGCLIENTENCODING='UTF8')
    r = subprocess.run([PSQL, '-h', 'localhost', '-p', '5432', '-U', 'postgres', '-d', db, '-A', '-t', '-c', sql],
                       capture_output=True, text=True, encoding='utf-8', errors='replace', env=env, timeout=120)
    return {x.strip() for x in (r.stdout or '').splitlines() if x.strip()}


def main():
    prod = q("SELECT table_name FROM information_schema.tables WHERE table_schema='public'")
    files = {}
    for p in SRC.rglob('*.java'):
        try:
            files[p] = p.read_text(encoding='utf-8', errors='replace')
        except OSError:
            pass
    rows = []
    for path, text in files.items():
        if not path.stem.endswith('Controller'):
            continue
        cm = CLASS_MAPPING_RE.search(text)
        base = cm.group(1) if cm else ''
        endpoints = []
        for m in MAPPING_RE.finditer(text):
            verb = m.group(1).upper()
            args = m.group(2) or ''
            hit = re.search(r'"(\/[^"]*)"', args)
            sub = hit.group(1) if hit else ''
            endpoints.append((verb, (base + sub) or base))
        if not endpoints:
            continue
        for tpath, ttext in files.items():
            for mm in TABLE_RE.finditer(ttext):
                table = mm.group(1)
                if table in prod:
                    continue
                ent = tpath.stem
                mapper = ent + 'Mapper'
                if mapper not in text and (ent + 'Service') not in text:
                    continue
                for verb, p in endpoints[:2]:
                    rows.append((table, path.stem, verb, p))
    seen = set()
    uniq = []
    for r in rows:
        k = (r[0], r[1])
        if k in seen:
            continue
        seen.add(k)
        uniq.append(r)
    print('# 生产环境已损坏接口清单（缺表 + 有 Controller 入口）' if False else '# placeholder')
    print('')
    print('| 表名 | Controller 类#方法 + 路径 | 生产库有表 | 影响面 | 建议 |')
    print('|------|---------------------------|-----------|--------|------|')
    for table, ctrl, verb, p in sorted(uniq, key=lambda x: (x[0], x[1])):
        print('| `%s` | %s （%s %s） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |' % (table, ctrl, verb, p))
    tables = {r[0] for r in uniq}
    print('')
    print('合计：**%d 张表**缺失，涉及 **%d 个 Controller**、**%d 条接口路径**。' % (len(tables), len({r[1] for r in uniq}), len(uniq)))
    return 0


if __name__ == '__main__':
    sys.exit(main())
