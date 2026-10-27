#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
开工闸门（preflight gate）—— **不是**「系统健康证明」，只是「能不能开工」的最低门槛。

⚠ 头号原则：**分母不足时 PASS 不可信**。
   巡检里大量断言在空库上会输出 skipped_no_data（分母=0），此时「FAIL=0」不代表正确性，
   只代表没有数据可判。所以本脚本同时打印 PASS / skipped / FAIL 三个数字与 skipped 占比，
   skipped > 50% 时给 WARN（不阻断，但必须显眼）。

两个档位（Owner 2026-10-27 定）：
  · 默认（快，~30s）：`mvn -o -B test-compile` + `python scripts/qa/consistency_check.py`
    —— 注意 test-compile **不等于** test：它只证明测试代码能编译，不证明用例通过。
  · `--full`（慢，真验证）：再加 `mvn -o -B test`（34 用例全跑）。在 E2E 前 / 发版前使用。

退出码：
  0  全绿（compile ok + FAIL=0 + 分母充足）
  2  巡检 FAIL > 0（阻断）
  3  仅 WARN：分母不足（skipped/(PASS+skipped) > 50%）或 test-compile 失败

发版 / CI 硬规则（Owner 2026-10-27）：
  `--full` 必须 **EXIT=0** 才准发版；EXIT=3 表示「有 WARN（分母不足 / 编译失败 / mvn test 失败）」，
  **不得发版**；EXIT=2 直接阻断（巡检 FAIL>0）。CI 里把本脚本的退出码当门禁即可，不要再靠人眼看输出。

用法：
  python scripts/qa/preflight.py            # 开工前快检
  python scripts/qa/preflight.py --full     # E2E 前 / 发版前全量（含 mvn test）
  python scripts/qa/preflight.py --verbose  # 同时打印 mvn 与巡检完整输出
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BACKEND = ROOT / "backend"
CHECK = Path(__file__).resolve().parent / "consistency_check.py"
WARN_RATIO = 0.5


def run(cmd, cwd, timeout=1800, env=None):
    p = subprocess.run(cmd, cwd=str(cwd), capture_output=True, text=True, timeout=timeout,
                       encoding="utf-8", errors="replace", shell=False, env=env)
    return p.returncode, (p.stdout or "") + (p.stderr or "")


def find_mvn():
    """Windows 下 subprocess 直接调 'mvn' 找不到（它是 mvn.cmd，需 shutil.which 解析）。"""
    import shutil
    for name in ("mvn.cmd", "mvn.bat", "mvn"):
        p = shutil.which(name)
        if p:
            return p
    raise RuntimeError("找不到 mvn（请确认 Maven 在 PATH 上，如 H:\\fuwu\\apache-maven-3.9.11\\bin）")


def find_java_home():
    """JAVA_HOME 是本机易踩的坑（当前环境变量指向不存在的路径）。
    优先用仓库自带 JDK21，其次校验环境变量，最后才放弃。"""
    import os
    candidates = [str(ROOT / "JDK21"), os.environ.get("JAVA_HOME", "")]
    for c in candidates:
        if c and (Path(c) / "bin" / "java.exe").exists():
            return c
    return None


def mvn(goal, verbose):
    cmd = [find_mvn(), "-o", "-B"] + goal
    import os
    env = dict(os.environ)
    jh = find_java_home()
    if jh:
        env["JAVA_HOME"] = jh
        env["PATH"] = str(Path(jh) / "bin") + os.pathsep + env.get("PATH", "")
    else:
        print("[WARN] 未找到可用 JDK（JAVA_HOME=%r 不存在，仓库 JDK21 也不存在）→ mvn 会失败"
              % os.environ.get("JAVA_HOME"))
    print("[GATE] %s  (cwd=%s)" % (" ".join(cmd), BACKEND))
    rc, out = run(cmd, BACKEND, env=env)
    tail = [l for l in out.splitlines() if ("BUILD SUCCESS" in l or "BUILD FAILURE" in l or "ERROR" in l)]
    if verbose:
        print(out[-4000:])
    else:
        for l in tail[-6:]:
            print("       " + l.strip())
    print("[GATE] %s -> rc=%s" % (" ".join(goal), rc))
    return rc


def parse_inspection(text):
    """从巡检输出里取 PASS / skipped / FAIL 三个数字；取不到就返回 None 并说明原因。"""
    m = re.search(r"汇总:\s*(\d+)\s*PASS\s*/\s*(\d+)\s*FAIL", text)
    if not m:
        return None
    passed, failed = int(m.group(1)), int(m.group(2))
    sk = re.search(r"其中\s*(\d+)\s*条\s*skipped_no_data", text)
    skipped = int(sk.group(1)) if sk else 0
    total = re.search(r"判定项\s*(\d+)\s*条", text)
    judged = int(total.group(1)) if total else (passed + failed)
    raw = next((l.strip() for l in text.splitlines() if l.startswith("汇总:")), "")
    return {"pass": passed, "fail": failed, "skipped": skipped, "judged": judged, "raw": raw}


def main():
    ap = argparse.ArgumentParser(description="开工闸门：test-compile + 巡检（--full 才跑 mvn test）")
    ap.add_argument("--full", action="store_true", help="E2E 前/发版前：额外跑 mvn test")
    ap.add_argument("--verbose", action="store_true", help="打印 mvn 与巡检完整输出")
    ap.add_argument("--skip-compile", action="store_true", help="跳过 mvn（仅看巡检）")
    a = ap.parse_args()

    print("=" * 86)
    print("PREFLIGHT GATE   mode=%s" % ("FULL(mvn test)" if a.full else "FAST(test-compile)"))
    print("  提醒：本脚本是开工闸门，不是系统健康证明；分母不足时 PASS 不可信。")
    print("=" * 86)

    warn = False
    compile_rc = 0
    if not a.skip_compile:
        compile_rc = mvn(["test-compile"], a.verbose)
        if compile_rc != 0:
            print("[WARN] test-compile 失败（rc=%s）→ 注意：test-compile≠test，但连编译都不过就不该开工" % compile_rc)
            warn = True

    test_rc = None
    if a.full:
        test_rc = mvn(["test"], a.verbose)
        if test_rc != 0:
            print("[WARN] mvn test 失败（rc=%s）—— 这是真验证档，失败必须修" % test_rc)
            warn = True

    print("-" * 86)
    print("[GATE] 巡检：%s" % CHECK)
    rc, out = run([sys.executable, str(CHECK)], ROOT, timeout=1800)
    if a.verbose:
        print(out)
    stats = parse_inspection(out)
    if stats is None:
        print("[WARN] 巡检输出无法解析（未找到「汇总: N PASS / M FAIL」），原文尾 20 行：")
        for l in out.splitlines()[-20:]:
            print("       " + l)
        print("[RESULT] INCONCLUSIVE（无法判定）")
        return 3

    passed, failed, skipped = stats["pass"], stats["fail"], stats["skipped"]
    judged = passed + skipped if (passed + skipped) else stats["judged"]
    ratio = (skipped / judged) if judged else 0.0
    print("[GATE] 巡检统计：PASS=%d  skipped_no_data=%d  FAIL=%d  判定项=%d"
          % (passed, skipped, failed, stats["judged"]))
    print("[GATE] skipped 占比：%d/%d = %.0f%%（阈值 %.0f%%）"
          % (skipped, judged, ratio * 100, WARN_RATIO * 100))
    print("[GATE] 巡检原始汇总：%s" % stats["raw"])

    if failed > 0:
        print("[RESULT] BLOCKED：巡检 FAIL=%d > 0 → exit 2" % failed)
        return 2
    if ratio > WARN_RATIO:
        print("[WARN] 分母不足，PASS 不可信：skipped %d/%d = %.0f%% > %.0f%%"
              % (skipped, judged, ratio * 100, WARN_RATIO * 100))
        warn = True

    summary = "PASS=%d skipped=%d (%d%%) FAIL=%d → %s" % (
        passed, skipped, round(ratio * 100), failed, "WARN" if warn else "OK")
    print("[RESULT] " + summary)
    return 3 if warn else 0


if __name__ == "__main__":
    sys.exit(main())