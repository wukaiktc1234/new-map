#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""宽 catch 棘轮（Lead 2026-10-30）。

统计口径（与 docs/audit/wide-catch-swallow-audit.md 一致）：
  `catch (Exception` / `catch (Throwable` 之后 6 行内出现 500 / INTERNAL_SERVER_ERROR 的次数。

棘轮规则：基线写死 BASELINE，**只许减不许增**；
  当前值 > 基线 → EXIT=2（BLOCK，新增了「吞异常返 500」）
  当前值 <= 基线 → EXIT=0（持平/减少都放行；减少时提示可下调基线）

为什么需要：212 处不会一轮改完，但没有棘轮，改掉的会被新代码补回来。
（与既有的 `throw new RuntimeException(` 483 棘轮同一模式。）
"""
import os
import re
import sys

BASELINE = 218   # 2026-10-30 首次统计值（本脚本正则为准；PowerShell 粗扫曾报 212，差异因 500 匹配范围不同）
ROOT = os.environ.get("QA_SRC_ROOT", r"P:\my-new-project\backend\src\main\java")
CATCH_RE = re.compile(r"catch\s*\(\s*(Exception|Throwable)\b")
FIVEHUNDRED_RE = re.compile(r"(Result\.error\(\s*500|HttpStatus\.INTERNAL_SERVER_ERROR|status\(\s*500)")


def count_risky():
    risky = 0
    hits = []
    for dirpath, _dirnames, filenames in os.walk(ROOT):
        if os.sep + "test" + os.sep in dirpath + os.sep:
            continue
        for fn in filenames:
            if not fn.endswith(".java"):
                continue
            path = os.path.join(dirpath, fn)
            try:
                lines = open(path, encoding="utf-8", errors="replace").read().splitlines()
            except OSError:
                continue
            for i, line in enumerate(lines):
                if CATCH_RE.search(line):
                    window = "\n".join(lines[i:i + 7])
                    if FIVEHUNDRED_RE.search(window):
                        risky += 1
                        hits.append("%s:%d" % (fn, i + 1))
    return risky, hits


def main():
    cur, hits = count_risky()
    print("[RATCHET] 宽 catch 吞异常返 500：当前=%d ｜基线=%d" % (cur, BASELINE))
    if cur > BASELINE:
        print("[FAIL] 新增 %d 处「catch 后返回 500」（会吞掉业务/权限语义）" % (cur - BASELINE))
        for h in hits[:20]:
            print("       新增嫌疑：", h)
        print("       修法：catch (AccessDeniedException | BusinessException e) { throw e; } 再兜底")
        return 2
    if cur < BASELINE:
        print("[PASS] 低于基线 %d 处 —— 请把 wide_catch_ratchet.py 的 BASELINE 下调到 %d" % (BASELINE - cur, cur))
        return 0
    print("[PASS] 与基线持平")
    return 0


if __name__ == "__main__":
    sys.exit(main())
