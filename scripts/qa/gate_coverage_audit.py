#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""501 覆盖率核对：缺表对应的入口是否都被 @UnfinishedFeature 覆盖。

三类输出：
  已覆盖（501）   ：入口文件里存在 @UnfinishedFeature(table = "X")
  未覆盖（仍500） ：有 @*Mapping 入口但无注解  ← 本轮要补
  无入口          ：只有 Service/Mapper，HTTP 打不到 → 无需注解
"""
from __future__ import annotations

import os, re, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'backend' / 'src' / 'main' / 'java'
PSQL = str(ROOT / 'PostgreSQL' / '18' / 'bin' / 'psql.exe')
TABLE_RE = re.compile(r'@TableName\(\s*"([^"]+)"\s*\)')
ENTRY_RE = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping')
REGISTRY = ROOT / 'scripts' / 'qa' / 'unfinished-features-registry.txt'


def prod_tables():
    env = dict(os.environ, PGPASSWORD='123456', PGCLIENTENCODING='UTF8')
    sql = "SELECT table_name FROM information_schema.tables WHERE table_schema='public'"
    r = subprocess.run([PSQL, '-h', 'localhost', '-p', '5432', '-U', 'postgres', '-d', 'food_traceability',
                        '-A', '-t', '-c', sql], capture_output=True, text=True, encoding='utf-8',
                       errors='replace', env=env, timeout=180)
    return {x.strip() for x in (r.stdout or '').splitlines() if x.strip()}


def registry_tables():
    out = set()
    if REGISTRY.exists():
        for line in REGISTRY.read_text(encoding='utf-8-sig', errors='replace').splitlines():
            line = line.strip()
            if line and not line.startswith('#'):
                out.add(line.split('|')[0].strip())
    return out


def main():
    have = prod_tables()
    skip = registry_tables()
    files = {}
    for p in SRC.rglob('*.java'):
        try:
            files[p] = p.read_text(encoding='utf-8', errors='replace')
        except OSError:
            pass
    missing = {}
    for p, t in files.items():
        for m in TABLE_RE.finditer(t):
            tbl = m.group(1)
            if tbl in have or tbl in skip:
                continue
            missing[tbl] = p.stem
    covered, uncovered, no_entry = [], [], []
    for tbl, ent in sorted(missing.items()):
        svc = ent + 'Service'
        ctrl_hits = [p for p, t in files.items()
                     if p.stem.endswith('Controller') and ENTRY_RE.search(t)
                     and (svc in t or (ent + 'Mapper') in t)]
        if not ctrl_hits:
            no_entry.append((tbl, ent))
            continue
        for cp in ctrl_hits:
            has = ('@UnfinishedFeature(table = "%s")' % tbl) in files[cp] or ('(table = "%s")' % tbl) in files[cp]
            (covered if has else uncovered).append((tbl, cp.stem))
    print('# 501 覆盖率核对（缺表 %d 张）' % len(missing))
    print('')
    print('| 类别 | 数量 |')
    print('|------|------|')
    print('| 已覆盖（501） | %d |' % len(covered))
    print('| **未覆盖（仍 500）** | **%d** |' % len(uncovered))
    print('| 无入口（HTTP 打不到） | %d |' % len(no_entry))
    if uncovered:
        print('')
        print('## 未覆盖清单（需补注解）')
        print('')
        print('| 表 | Controller |')
        print('|----|------------|')
        for tbl, c in sorted(set(uncovered)):
            print('| `%s` | %s |' % (tbl, c))
    if no_entry:
        print('')
        print('## 无入口清单（无需注解）')
        print('')
        print('| 表 | 实体 |')
        print('|----|------|')
        for tbl, ent in sorted(no_entry):
            print('| `%s` | %s |' % (tbl, ent))
    return 2 if uncovered else 0


if __name__ == '__main__':
    sys.exit(main())
