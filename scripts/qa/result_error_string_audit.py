#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""bug-⑫ 形态②审计：Result.error(字面量) 调用点（默认 code=500）。

判据：Result.error(" 后直接跟字符串字面量（不是 ErrorCode.X.getCode() 这类重载）。
输出：文件:行 | 方法 | 文案 | 推测语义 | 等价 ErrorCode
"""
from __future__ import annotations

import re
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'backend' / 'src' / 'main' / 'java'
PATTERN = re.compile(r'Result\.error\("([^"]{1,80})"')
METHOD_RE = re.compile(r'\n\s{4}(?:public|private|protected)\s+[^\n;{}]{0,160}?(\w+)\s*\(')

HINTS = [
    ('不存在(404)', ('不存在', '未找到', '找不到')),
    ('权限(403)', ('权限', '无权', '不是该', '越权')),
    ('状态非法(400)', ('状态', '已支付', '已取消', '不允许')),
    ('参数(400)', ('不能为空', '非法', '格式', '必须', '超出', '不支持', '无效', '不正确')),
]
CODE_MAP = {
    '不存在(404)': 'ErrorCode.NOT_FOUND (404)',
    '权限(403)': 'ErrorCode.FORBIDDEN (403)',
    '状态非法(400)': 'ErrorCode.PARAM_ERROR (400)',
    '参数(400)': 'ErrorCode.PARAM_ERROR (400)',
    '未判定': '(需人工判定)',
}


def classify(text):
    for sem, keys in HINTS:
        for k in keys:
            if k in text:
                return sem
    return '未判定'


def main():
    rows = []
    for path in sorted(SRC.rglob('*.java')):
        text = path.read_text(encoding='utf-8', errors='replace')
        if 'Result.error("' not in text:
            continue
        for m in PATTERN.finditer(text):
            msg = m.group(1)
            line = text[:m.start()].count('\n') + 1
            cands = METHOD_RE.findall(text[:m.start()])
            method = cands[-1] if cands else '(unknown)'
            rel = str(path.relative_to(ROOT)).replace('\\', '/')
            rows.append((rel, line, method, msg, classify(msg)))
    print('# bug-⑫ 形态②：Result.error 字面量审计（默认 code=500）')
    print('')
    print('扫描：backend/src/main/java 全部 .java；判据：Result.error( 后接字符串字面量')
    print('')
    print('| # | 文件:行 | 方法 | 文案 | 推测语义 | 等价 ErrorCode |')
    print('|---|---------|------|------|----------|----------------|')
    for i, (f, line, method, msg, sem) in enumerate(rows, 1):
        print('| %d | `%s:%d` | `%s` | %s | %s | %s |' % (i, f, line, method, msg, sem, CODE_MAP.get(sem, '-')))
    print('')
    print('合计 **%d** 处。按语义分布：' % len(rows))
    for sem, n in Counter(r[4] for r in rows).most_common():
        print('  - %s：%d' % (sem, n))
    return 0


if __name__ == '__main__':
    sys.exit(main())
