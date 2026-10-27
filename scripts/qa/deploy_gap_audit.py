#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""部署缺口审计：实体声明的表在目标库不存在 + 有活引用 → 部署缺口（发版前必须清零）。

背景：position_code_rules 这类表在生产库是历史手工建的，迁移里只有 ALTER，
从零建的库（CI / 测试库 / 新环境）会缺表，上线才炸。本脚本把它变成闸门。
"""
from __future__ import annotations

import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'backend' / 'src' / 'main' / 'java'
PSQL = str(ROOT / 'PostgreSQL' / '18' / 'bin' / 'psql.exe')
TABLE_RE = re.compile(r'@TableName\(\s*"([^"]+)"\s*\)')


def query_tables(db):
    env = dict(os.environ, PGPASSWORD='123456', PGCLIENTENCODING='UTF8')
    sql = "SELECT table_name FROM information_schema.tables WHERE table_schema='public'"
    r = subprocess.run([PSQL, '-h', 'localhost', '-p', '5432', '-U', 'postgres', '-d', db,
                        '-A', '-t', '-c', sql], capture_output=True, text=True,
                       encoding='utf-8', errors='replace', env=env, timeout=180)
    if r.returncode != 0:
        print('[ERROR] query %s failed: %s' % (db, (r.stderr or '').strip()[:200]))
        return None
    return {x.strip() for x in (r.stdout or '').splitlines() if x.strip()}


def load_sources():
    out = []
    for p in SRC.rglob('*.java'):
        try:
            out.append((p, p.read_text(encoding='utf-8', errors='replace')))
        except OSError:
            continue
    return out


def main():
    db = 'food_traceability_test'
    if '--db' in sys.argv:
        db = sys.argv[sys.argv.index('--db') + 1]
    tables = query_tables(db)
    if tables is None:
        return 2
    files = load_sources()
    live = {}
    dead = 0
    for path, text in files:
        for m in TABLE_RE.finditer(text):
            t = m.group(1)
            if t in tables:
                continue
            # 严格口径：只算「Mapper 被别处真正使用」——找该实体的 Mapper 接口，
            # 再看除 Mapper 自身外是否还有文件提到这个 Mapper 类名（@Autowired/构造注入/调用）。
            mapper_stem = path.stem + 'Mapper'
            mapper_files = [q for q, txt in files if q.stem == mapper_stem]
            refs = []
            for mq in mapper_files:
                for q, txt in files:
                    if q == mq:
                        continue
                    if mapper_stem in txt:
                        refs.append(str(q.relative_to(ROOT)).replace(chr(92), '/'))
            key = (t, path.stem)
            if refs:
                live[key] = refs
            else:
                dead += 1
    print('# 部署缺口审计（库=%s）' % db)
    print('')
    print('表不存在：活引用 %d 个实体，死代码 %d 个' % (len(live), dead))
    if live:
        print('')
        print('| 表 | 实体 | 活引用处（前 3） |')
        print('|----|------|------------------|')
        for (t, ent), refs in sorted(live.items()):
            names = ', '.join(Path(x).name for x in refs[:3])
            print('| `%s` | %s | %s（共 %d 处） |' % (t, ent, names, len(refs)))
        print('')
        print('[BLOCK] 部署缺口 %d 处：从零建的库会缺表，发版前必须补迁移' % len(live))
        return 2
    print('')
    print('[GATE] 无部署缺口（表不存在且有活引用 = 0）')
    return 0


if __name__ == '__main__':
    sys.exit(main())
