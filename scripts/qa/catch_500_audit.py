#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""bug-⑫ 只读审计：找出 Controller 用 catch(RuntimeException) + Result.error() 把业务异常压成 500 的位置。

为什么需要它：Result.error(String) 默认 code=500，所以 service 抛 BusinessException(NOT_FOUND/400/403)
只要在 controller 被 catch (RuntimeException e) { return Result.error(e.getMessage()); } 兜住，
前端拿到的就是 500 —— 只改 service 不改 controller，等于白改。
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "backend" / "src" / "main" / "java"

CATCH_RE = re.compile(r"catch\s*\(\s*RuntimeException\s+(\w+)\s*\)\s*\{")
CLASS_MAPPING_RE = re.compile(r'@RequestMapping\s*\(\s*(?:value\s*=\s*)?"([^"]*)"')
METHOD_MAPPING_RE = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping\s*\(\s*(?:value\s*=\s*)?"([^"]*)"')


def block_after(text, start):
    """从 start 处的 { 起取到配对的 }（简单括号计数）。"""
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start:i + 1]
    return text[start:]


def enclosing_method(text, pos):
    """向上找最近的方法名（粗略）。"""
    head = text[:pos]
    cands = list(re.finditer(r"\n\s{4}(?:public|private|protected)\s+[^\n;{}]{0,160}?(\w+)\s*\(", head))
    return cands[-1].group(1) if cands else '(unknown)'


def enclosing_mapping(text, pos):
    """向上找最近的 @XxxMapping 路径（含类级前缀）。"""
    head = text[:pos]
    cls = CLASS_MAPPING_RE.search(head)
    prefix = cls.group(1) if cls else ''
    maps = list(METHOD_MAPPING_RE.finditer(head))
    if not maps:
        return prefix or '(none)'
    m = maps[-1]
    path = m.group(2)
    if prefix:
        return prefix.rstrip('/') + '/' + path.lstrip('/')
    return path


def main():
    rows = []
    for path in sorted(SRC.rglob('*.java')):
        text = path.read_text(encoding='utf-8', errors='replace')
        if 'catch (RuntimeException' not in text:
            continue
        for m in CATCH_RE.finditer(text):
            body = block_after(text, m.end() - 1)
            if 'Result.error(' not in body:
                continue
            window = text[max(0, m.start() - 400):m.start()]
            has_biz = ('catch (BusinessException' in window)
            line = text[:m.start()].count('\n') + 1
            method = enclosing_method(text, m.start())
            mapping = enclosing_mapping(text, m.start())
            snippet = ' '.join(body.split())[:90].replace('|', '\\|')
            rows.append((str(path.relative_to(ROOT)).replace('\\', '/'), line, has_biz, mapping, method, snippet))

    print('# bug-⑫ 审计：Controller 把业务异常压成 500 的 catch 位置（只读报告）')
    print('')
    print('扫描范围：`backend/src/main/java` 全部 `.java`；判据：`catch (RuntimeException` 且体内出现 `Result.error(`')
    print('含义：`Result.error(String)` 默认 code=500 → service 抛的 BusinessException 会被压回 500')
    print('')
    print('| # | 文件:行 | BusinessException 前置分支 | 影响端点 | 方法 | 证据片段 |')
    print('|---|---------|---------------------------|----------|------|----------|')
    for i, (f, line, has_biz, mapping, method, snippet) in enumerate(rows, 1):
        print('| %d | `%s:%d` | %s | `%s` | `%s` | `%s` |'
              % (i, f, line, '有' if has_biz else '**无**', mapping, method, snippet))
    total = len(rows)
    nobiz = sum(1 for r in rows if not r[2])
    print('')
    print('合计 **%d** 处；其中**无 BusinessException 前置分支**的 %d 处（这些位置的业务异常一律变 500）。' % (total, nobiz))
    return 0


if __name__ == "__main__":
    sys.exit(main())
