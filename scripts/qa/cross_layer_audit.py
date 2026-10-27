#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""跨层覆盖率审计：Service → Mapper → 表 → 上游 Controller 入口。

为什么要跨层：/v1/kitchen/scan 的漏判说明「Controller 里看不到的写入，不代表不存在」。
同一家族问题：A46（代码要的权限目录里没有）、bug-⑫（service 返回 Result.error 被 controller 吞）——
都是「跨层调用时，单层局部看起来都没问题」。

输出：
  1) 跨层引用清单：Service → Mapper → 表 → 上游 Controller（入口数 >1 或无法唯一确定 → 标「需人工确认」）
  2) 读/写分级：有写入 / 只有读取（外部写入假设）/ 读写皆无（死代码）
"""
from __future__ import annotations

import os, re, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'backend' / 'src' / 'main' / 'java'
PSQL = str(ROOT / 'PostgreSQL' / '18' / 'bin' / 'psql.exe')
TABLE_RE = re.compile(r'@TableName\(\s*"([^"]+)"\s*\)')
FIELD_MAPPER_RE = re.compile(r'private\s+(?:final\s+)?(\w+Mapper)\s+\w+\s*;')
ENTRY_RE = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping')
WRITE_RE = re.compile(r'\.(insert|update|delete|save|saveBatch|insertBatch|remove)\w*\s*\(')
READ_RE = re.compile(r'\.(select|count|page|list|getOne|findBy)\w*\s*\(')
REGISTRY = ROOT / 'scripts' / 'qa' / 'unfinished-features-registry.txt'


def prod_tables():
    env = dict(os.environ, PGPASSWORD='123456', PGCLIENTENCODING='UTF8')
    r = subprocess.run([PSQL, '-h', 'localhost', '-p', '5432', '-U', 'postgres', '-d', 'food_traceability',
                        '-A', '-t', '-c', "SELECT table_name FROM information_schema.tables WHERE table_schema='public'"],
                       capture_output=True, text=True, encoding='utf-8', errors='replace', env=env, timeout=180)
    return {x.strip() for x in (r.stdout or '').splitlines() if x.strip()}


def main():
    have = prod_tables()
    skip = set()
    if REGISTRY.exists():
        for line in REGISTRY.read_text(encoding='utf-8-sig', errors='replace').splitlines():
            line = line.strip()
            if line and not line.startswith('#'):
                skip.add(line.split('|')[0].strip())
    files = {}
    for p in SRC.rglob('*.java'):
        try:
            files[p] = p.read_text(encoding='utf-8', errors='replace')
        except OSError:
            pass
    # 实体 → 表
    ent_table = {}
    for p, t in files.items():
        m = TABLE_RE.search(t)
        if m:
            ent_table[p.stem] = m.group(1)
    # ServiceImpl → 注入的 Mapper
    svc_mappers = {}
    for p, t in files.items():
        if not p.stem.endswith('ServiceImpl'):
            continue
        ms = set(FIELD_MAPPER_RE.findall(t))
        if ms:
            svc_mappers[p.stem] = (ms, t)
    rows = []
    for svc, (mappers, text) in sorted(svc_mappers.items()):
        writes = bool(WRITE_RE.search(text))
        reads = bool(READ_RE.search(text))
        for mp in sorted(mappers):
            ent = mp[:-len('Mapper')]
            table = ent_table.get(ent)
            if not table or table in have or table in skip:
                continue
            svc_iface = svc.replace('ServiceImpl', 'Service')
            ctrls = [p.stem for p, t in files.items()
                     if p.stem.endswith('Controller') and ENTRY_RE.search(t)
                     and (svc_iface in t or svc in t)]
            covered = []
            for c in ctrls:
                cf = next((p for p in files if p.stem == c), None)
                if cf and ('(table = "%s")' % table) in files[cf]:
                    covered.append(c)
            kind = '有写入' if writes else ('只有读取' if reads else '读写皆无')
            rows.append((table, svc, mp, kind, ctrls, covered))
    print('# 跨层覆盖率审计（Service → Mapper → 表 → 上游 Controller）')
    print('')
    print('| 表 | Service | Mapper | 读写 | 上游 Controller 入口 | 501 覆盖 | 判定 |')
    print('|----|---------|--------|------|----------------------|----------|------|')
    manual = 0
    for table, svc, mp, kind, ctrls, covered in rows:
        if not ctrls:
            verdict = '无 HTTP 入口（内部调用）'
        elif len(ctrls) > 1 and len(covered) < len(ctrls):
            verdict = '**需人工确认**（多入口，未全覆盖）'
            manual += 1
        elif len(covered) == len(ctrls):
            verdict = '已覆盖'
        else:
            verdict = '**需人工确认**（入口未注解）'
            manual += 1
        print('| `%s` | %s | %s | %s | %s | %s | %s |' % (
            table, svc, mp, kind,
            ', '.join(ctrls) or '-',
            ('%d/%d' % (len(covered), len(ctrls))) if ctrls else '-',
            verdict))
    print('')
    print('合计 %d 条跨层引用；**需人工确认 %d 条**。' % (len(rows), manual))
    return 0


if __name__ == '__main__':
    sys.exit(main())
