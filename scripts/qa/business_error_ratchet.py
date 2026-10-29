#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""业务错误源头棘轮（Lead 2026-10-30）。

为什么要这条闸门：
  今天定位到的真根因链是——
    Service 用 `throw new RuntimeException("角色不存在")` 表达业务错误
      → 全局处理器只能当 500
        → 有人在 Controller 内层加 catch(Exception) 包装成 500（治标）
          → 又有人加白名单想透传（治标第二层）
            → 三次都没碰源头
  源头不减，内层 catch 就永远有人加回去。所以先装闸门，再逐个改。

统计口径（本脚本为正则权威）：
  service 层（路径含 /service/）里 `throw new RuntimeException(` 的次数。
  \u26a0 不含 test 目录。

规则：当前 > 基线 → EXIT=2（BLOCK，新增了「用 RuntimeException 表达业务错误」）；
      当前 < 基线 → EXIT=0 并提示下调基线；持平 → EXIT=0。
"""
import os
import re
import sys

BASELINE = 441   # 2026-10-30 首次实测值（service 层 throw new RuntimeException 共 441 处）；只许减不许增
ROOT = os.environ.get("QA_SRC_ROOT", r"P:\my-new-project\backend\src\main\java")
PATTERN = re.compile(r"throw\s+new\s+RuntimeException\s*\(")


def scan():
    hits = []
    for dirpath, _d, files in os.walk(ROOT):
        norm = dirpath.replace("\\", "/")
        if "/service/" not in norm or "/test/" in norm:
            continue
        for fn in files:
            if not fn.endswith(".java"):
                continue
            path = os.path.join(dirpath, fn)
            try:
                lines = open(path, encoding="utf-8", errors="replace").read().splitlines()
            except OSError:
                continue
            for i, line in enumerate(lines):
                if PATTERN.search(line):
                    hits.append((fn, i + 1, line.strip()[:110]))
    return hits


def main():
    hits = scan()
    cur = len(hits)
    print("[RATCHET] service 层 throw new RuntimeException：当前=%d ｜基线=%d" % (cur, BASELINE))
    if cur > BASELINE:
        print("[FAIL] 新增 %d 处「用 RuntimeException 表达业务错误」（源头污染）" % (cur - BASELINE))
        for fn, ln, txt in hits[:20]:
            print("       %s:%d  %s" % (fn, ln, txt))
        print("       修法：改为 throw new BusinessException(<4xx>, \"中文原因\")")
        return 2
    if cur < BASELINE:
        print("[PASS] 低于基线 %d 处 —— 请把 business_error_ratchet.py 的 BASELINE 下调到 %d" % (BASELINE - cur, cur))
        return 0
    print("[PASS] 与基线持平")
    for fn, ln, txt in hits[:10]:
        print("       （存量）%s:%d  %s" % (fn, ln, txt))
    return 0


if __name__ == "__main__":
    sys.exit(main())
