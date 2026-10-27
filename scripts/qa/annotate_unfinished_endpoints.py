#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""给 P0 清单里的入口方法加 @UnfinishedFeature 注解（方法级，最小侵入）。

输入：docs/audit/broken-endpoints-prod.md 的表格行；
做法：解析 Controller 的 @RequestMapping 基路径 + 方法上的 @*Mapping 子路径，
      路径完全匹配的那一个方法，在其注解行之上插入 @UnfinishedFeature 与注释。
幂等：已存在注解的跳过。
"""
from __future__ import annotations

import re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'backend' / 'src' / 'main' / 'java'
DOC = ROOT / 'docs' / 'audit' / 'broken-endpoints-prod.md'
CLASS_RE = re.compile(r'@RequestMapping\(\s*"([^"]+)"')
METHOD_RE = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*\(([^)]*)\)')


def parse_rows():
    rows = []
    for line in DOC.read_text(encoding='utf-8', errors='replace').splitlines():
        if not line.startswith('| `'):
            continue
        cols = [c.strip() for c in line.split('|')]
        table = cols[1].replace('`', '')
        m = re.match(r'(\w+)\s*（(\w+)\s+(\S+)）', cols[2])
        if not m:
            continue
        rows.append((table, m.group(1), m.group(2), m.group(3)))
    return rows


def main():
    rows = parse_rows()
    changed = 0
    skipped = []
    for table, ctrl, verb, path in rows:
        cands = [p for p in SRC.rglob(ctrl + '.java')]
        if not cands:
            skipped.append((ctrl, 'file not found'))
            continue
        f = cands[0]
        text = f.read_text(encoding='utf-8', errors='replace')
        if 'UnfinishedFeature' in text and ('table = "%s"' % table) in text:
            skipped.append((ctrl + ' ' + path, 'already annotated'))
            continue
        lines = text.splitlines()
        target_idx = None
        for i, ln in enumerate(lines):
            m = METHOD_RE.search(ln)
            if not m:
                continue
            hit = re.search(r'"(\/[^"]*)"', m.group(2) or '')
            sub = hit.group(1) if hit else ''
            if sub and (sub == path or path.endswith(sub)):
                target_idx = i
                break
        if target_idx is None:
            skipped.append((ctrl + ' ' + path, 'method not matched'))
            continue
        indent = lines[target_idx][:len(lines[target_idx]) - len(lines[target_idx].lstrip())]
        new = [indent + '// P0-unfinished: %s 两边均无表，见 docs/audit/broken-endpoints-prod.md' % table,
               indent + '@UnfinishedFeature(table = "%s")' % table]
        lines[target_idx:target_idx] = new
        out = '\n'.join(lines)
        if 'import com.foodtraceability.common.annotation.UnfinishedFeature;' not in out:
            out = out.replace('package ', 'package ', 1)
            idx = out.find(';')
            out = out[:idx + 1] + '\n\nimport com.foodtraceability.common.annotation.UnfinishedFeature;' + out[idx + 1:]
        f.write_text(out, encoding='utf-8')
        changed += 1
    print('已加注解 %d 处；跳过 %d 条' % (changed, len(skipped)))
    for s, why in skipped:
        print('   skip: %s (%s)' % (s, why))
    return 0


if __name__ == '__main__':
    sys.exit(main())
