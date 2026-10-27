#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""实体列名审计（bug-⑪ 系统性排查，只读）。

背景：EmployeeApproval 把 create_time 写成 created_time → 按 id 查必 500（BadSqlGrammar），
而这类错误**不会在启动时报**，只在 SQL 执行时爆 —— 典型静默失效。

做法：
  1) 扫 backend/src/main/java 下所有 @TableName 实体，解析字段与 @TableField 列名；
  2) 用 psql 查 information_schema.columns 拿真实列；
  3) 输出「实体列名 ↔ 实际列名」不一致清单（含相似列建议，如 created_time → create_time）。

用法：python scripts/qa/entity_column_audit.py [--db food_traceability]
"""
from __future__ import annotations

import argparse
import difflib
import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'backend' / 'src' / 'main' / 'java'
PSQL = str(ROOT / 'PostgreSQL' / '18' / 'bin' / 'psql.exe')

TABLE_RE = re.compile(r'@TableName\s*\(\s*(?:value\s*=\s*)?"([^"]+)"')
FIELD_RE = re.compile(r'@TableField\s*\(\s*(?:value\s*=\s*)?"([^"]+)"[^)]*\)\s*(?:@[\w.]+(?:\([^)]*\))?\s*)*private\s+[\w<>\[\], .]+?\s+(\w+)\s*[;=]')
PLAIN_FIELD_RE = re.compile(r'private\s+[\w<>\[\], .]+?\s+(\w+)\s*[;=]')


def camel_to_snake(name):
    s1 = re.sub(r'(.)([A-Z][a-z]+)', r'\1_\2', name)
    return re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', s1).lower()


def fetch_columns(db):
    env = dict(os.environ, PGPASSWORD='123456', PGCLIENTENCODING='UTF8')
    sql = ("SELECT table_name, column_name FROM information_schema.columns "
           "WHERE table_schema = 'public' ORDER BY 1, 2")
    r = subprocess.run([PSQL, '-h', 'localhost', '-p', '5432', '-U', 'postgres', '-d', db,
                        '-A', '-F', '|', '-t', '-c', sql], capture_output=True, text=True,
                       encoding='utf-8', errors='replace', env=env, timeout=120)
    cols = {}
    for line in (r.stdout or '').splitlines():
        if '|' not in line:
            continue
        t, c = line.split('|', 1)
        cols.setdefault(t.strip(), set()).add(c.strip())
    return cols


def fetch_rowcounts(db, tables):
    """每张表的有效行数（用于判断「表是活的还是空壳」）。"""
    if not tables:
        return {}
    env = dict(os.environ, PGPASSWORD='123456', PGCLIENTENCODING='UTF8')
    parts = []
    for t in sorted(tables):
        parts.append("SELECT '%s' AS t, count(*) AS n FROM %s" % (t, t))
    sql = " UNION ALL ".join(parts)
    r = subprocess.run([PSQL, '-h', 'localhost', '-p', '5432', '-U', 'postgres', '-d', db,
                        '-A', '-F', '|', '-t', '-c', sql], capture_output=True, text=True,
                       encoding='utf-8', errors='replace', env=env, timeout=180)
    out = {}
    for line in (r.stdout or '').splitlines():
        if '|' in line:
            t, n = line.split('|', 1)
            try:
                out[t.strip()] = int(n.strip())
            except ValueError:
                pass
    return out


def entity_usage(ent_name):
    """该实体是否被 Mapper/ServiceImpl 引用（粗判：全仓 grep 实体类名）。"""
    hits = 0
    for path in SRC.rglob('*.java'):
        if path.stem == ent_name:
            continue
        try:
            if ent_name in path.read_text(encoding='utf-8', errors='replace'):
                hits += 1
        except OSError:
            continue
    return hits


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--db', default='food_traceability')
    a = ap.parse_args()
    cols = fetch_columns(a.db)
    mismatches = []
    checked = 0
    for path in sorted(SRC.rglob('*.java')):
        text = path.read_text(encoding='utf-8', errors='replace')
        tm = TABLE_RE.search(text)
        if not tm:
            continue
        table = tm.group(1)
        actual = cols.get(table)
        if not actual:
            mismatches.append((table, path.name, '(表不存在)', '-', '-'))
            continue
        ent_name = path.stem
        for fm in FIELD_RE.finditer(text):
            column, field = fm.group(1), fm.group(2)
            checked += 1
            if column not in actual:
                # 相似列：用 difflib 找最接近的真实列（created_time → create_time 这种差 1 个字符的能命中）
                close = difflib.get_close_matches(column, sorted(actual), n=1, cutoff=0.75)
                mismatches.append((table, path.name, field, column, close[0] if close else '(无相似列)'))
    print('# 实体列名审计（bug-⑪ 系统性排查）\n')
    print('库：`%s` ｜ 扫描实体：`backend/src/main/java` ｜ 核对 `@TableField` 列数：%d\n' % (a.db, checked))
    if not mismatches:
        print('未发现不一致：所有 @TableField 列名都能在 information_schema 中命中。')
        return 0
    tables = {m[0] for m in mismatches if m[4] != '(表不存在)'}
    rows = fetch_rowcounts(a.db, tables)
    print('| # | 表 | 表行数 | 实体文件 | 字段 | 实体列名 | 相似真实列 | 判定 |')
    print('|---|----|--------|----------|------|----------|------------|------|')
    for i, (t, f, field, col, guess) in enumerate(mismatches, 1):
        n = rows.get(t)
        if guess == '(表不存在)' or n is None:
            verdict = 'B/表不存在 → 先判死代码'
        elif n == 0:
            verdict = '空表：可能是 legacy，改动无效'
        elif guess != '(无相似列)':
            verdict = 'A/疑似写错（表有数据）'
        else:
            verdict = 'C/列确实没有（表有数据）'
        print('| %d | `%s` | %s | %s | `%s` | `%s` | `%s` | %s |'
              % (i, t, ('?' if n is None else n), f, field, col, guess, verdict))
    print('\n合计 **%d** 处不一致。' % len(mismatches))
    return 1


if __name__ == "__main__":
    sys.exit(main())
