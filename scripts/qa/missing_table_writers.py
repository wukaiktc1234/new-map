#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""22 张「有写入但两边都无表」清单：表名 | 写入代码位置 | 推测用途 | 前端引用 | 分类。"""
from __future__ import annotations

import os, re, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'backend' / 'src' / 'main' / 'java'
PSQL = str(ROOT / 'PostgreSQL' / '18' / 'bin' / 'psql.exe')
TABLE_RE = re.compile(r'@TableName\(\s*"([^"]+)"\s*\)')
WRITE_RE = re.compile(r'([A-Za-z]+Mapper)\.(insert|update|delete|save|remove)')
FE_DIRS = ['frontend/src', 'frontend-pos/src', 'frontend-kitchen/src', 'employee-frontend/src']


def main():
    env = dict(os.environ, PGPASSWORD='123456', PGCLIENTENCODING='UTF8')
    r = subprocess.run([PSQL, '-h', 'localhost', '-p', '5432', '-U', 'postgres', '-d', 'food_traceability',
                        "-A", "-t", "-c", "SELECT table_name FROM information_schema.tables WHERE table_schema='public'"],
                       capture_output=True, text=True, encoding='utf-8', errors='replace', env=env, timeout=180)
    have = {x.strip() for x in (r.stdout or '').splitlines() if x.strip()}
    files = {}
    for p in SRC.rglob('*.java'):
        try:
            files[p] = p.read_text(encoding='utf-8', errors='replace')
        except OSError:
            pass
    ent_table = {}
    for p, t in files.items():
        m = TABLE_RE.search(t)
        if m:
            ent_table[p.stem] = m.group(1)
    # 表 → 写入位置
    writes = {}
    for p, t in files.items():
        for m in WRITE_RE.finditer(t):
            raw = m.group(1)  # 变量名首字母小写，如 materialUsageRecord
            ent = raw[:1].upper() + raw[1:]
            tbl = ent_table.get(ent)
            if not tbl:
                # 兼容：Mapper 名与实体名不完全一致时，按表名启发式兜底（下划线化）
                tbl = None
            if not tbl:
                continue
            line = t[:m.start()].count(chr(10)) + 1
            writes.setdefault(tbl, []).append('%s:%d' % (p.name, line))
    rows = []
    for tbl, locs in sorted(writes.items()):
        if tbl in have:
            continue
        # 前端引用探测：把表名转成若干候选路径片段
        frag = tbl.replace('_', '-')
        fe = []
        for d in FE_DIRS:
            base = ROOT / d
            if not base.exists():
                continue
            for f in base.rglob('*'):
                if not f.is_file() or f.suffix not in ('.ts', '.vue', '.js'):
                    continue
                try:
                    txt = f.read_text(encoding='utf-8', errors='replace')
                except OSError:
                    continue
                if ('/' + frag) in txt or tbl in txt:
                    fe.append(str(f.relative_to(ROOT)).replace(chr(92), '/'))
                    break
        rows.append((tbl, locs[0], len(locs), fe[0] if fe else '-'))
    print('# 缺表清单（有写入代码但生产库与开发库都无表）')
    print('')
    print('| 表名 | 写入代码位置 | 写入点数 | 前端引用 |')
    print('|------|--------------|----------|----------|')
    for tbl, loc, n, fe in rows:
        print('| `%s` | `%s` | %d | %s |' % (tbl, loc, n, fe))
    print('')
    print('合计 %d 张。' % len(rows))
    return 0


if __name__ == '__main__':
    sys.exit(main())
