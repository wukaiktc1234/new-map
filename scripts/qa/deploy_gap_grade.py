#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""部署缺口分级：真活 / 半活 / 死代码（Lead 三分法）。

判据：
  真活   : 实体 → Mapper → Service → Controller（有 @RequestMapping/@GetMapping/... 入口）调用链完整
  半活   : Service 存在但无 Controller 入口；查 @Scheduled / ApplicationRunner / CommandLineRunner / @RabbitListener / @KafkaListener
  死代码 : 除自身外无任何引用
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
ENTRY_RE = re.compile(r'@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)')
ASYNC_RE = re.compile(r'@(Scheduled|RabbitListener|KafkaListener|EventListener)|ApplicationRunner|CommandLineRunner')


def query_tables(db):
    env = dict(os.environ, PGPASSWORD='123456', PGCLIENTENCODING='UTF8')
    sql = "SELECT table_name FROM information_schema.tables WHERE table_schema='public'"
    r = subprocess.run([PSQL, '-h', 'localhost', '-p', '5432', '-U', 'postgres', '-d', db,
                        '-A', '-t', '-c', sql], capture_output=True, text=True,
                       encoding='utf-8', errors='replace', env=env, timeout=180)
    if r.returncode != 0:
        return None
    return {x.strip() for x in (r.stdout or '').splitlines() if x.strip()}


def main():
    db = 'food_traceability_test'
    if '--db' in sys.argv:
        db = sys.argv[sys.argv.index('--db') + 1]
    tables = query_tables(db)
    if tables is None:
        print('[ERROR] db query failed')
        return 2
    files = {}
    for p in SRC.rglob('*.java'):
        try:
            files[p] = p.read_text(encoding='utf-8', errors='replace')
        except OSError:
            pass
    rows = []
    for path, text in files.items():
        for m in TABLE_RE.finditer(text):
            table = m.group(1)
            if table in tables:
                continue
            ent = path.stem
            mapper_stem = ent + 'Mapper'
            mapper_paths = [q for q in files if q.stem == mapper_stem]
            if not mapper_paths:
                mapper_paths = [q for q in files if q.stem == ent + 'NewMapper']
            mapper_used = []
            for mp in mapper_paths:
                for q, t in files.items():
                    if q != mp and mapper_paths and (mp.stem in t) and ('Mapper' in q.stem or 'Service' in q.stem or 'Controller' in q.stem):
                        mapper_used.append(q)
            services = sorted({q for q in mapper_used})
            svc_names = set()
            for q in services:
                svc_names.add(q.stem)
                svc_names.add(q.stem.replace('Impl', ''))
                svc_names.add(q.stem.replace('ServiceImpl', 'Service'))
            ctrls = []
            for q, t in files.items():
                if not q.stem.endswith('Controller'):
                    continue
                if any(n in t for n in svc_names):
                    if ENTRY_RE.search(t):
                        ctrls.append(q.stem)
            async_hit = [q.stem for q in services if ASYNC_RE.search(files[q])]
            if not mapper_paths:
                grade, advice = '死代码', '确认不建表；进死代码删除清单'
            elif not services:
                grade, advice = '死代码', '确认不建表；进死代码删除清单'
            elif ctrls:
                grade, advice = '真活', '补迁移（先贴生产库 \\d 结构，逐列照抄）'
            elif async_hit:
                grade, advice = '半活', '补迁移（定时任务/MQ 消费链，无 HTTP 入口）'
            else:
                grade, advice = '半活', '补迁移或确认弃用（Service 无 Controller 入口、无调度）'
            rows.append((table, ent, mapper_stem if mapper_paths else '-',
                         ', '.join(sorted({q.stem for q in services})[:2]) or '-',
                         ', '.join(sorted(set(ctrls))[:2]) or '-', grade, advice))
    order = {'真活': 0, '半活': 1, '死代码': 2}
    rows.sort(key=lambda r: (order.get(r[5], 9), r[0]))
    print('# 部署缺口分级清单（库=%s）' % db)
    print('')
    from collections import Counter
    c = Counter(r[5] for r in rows)
    print('合计 %d 处：真活 %d ｜ 半活 %d ｜ 死代码 %d' % (len(rows), c.get('真活', 0), c.get('半活', 0), c.get('死代码', 0)))
    print('')
    print('| 表名 | 实体 | Mapper | Service | Controller入口 | 分级 | 建议 |')
    print('|------|------|--------|---------|----------------|------|------|')
    for r in rows:
        print('| `%s` | %s | %s | %s | %s | %s | %s |' % r)
    return 0


if __name__ == '__main__':
    sys.exit(main())
