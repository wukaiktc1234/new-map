#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""@PreAuthorize 覆盖率棘轮（Lead 2026-10-30，第 4 道棘轮）。

判据（唯一有意义的判据）：**历史缺口用基线承载，新增缺口一律阻断**
  · 扫描所有 @RestController 的映射方法（@Get/Post/Put/Delete/PatchMapping）
  · 有 @PreAuthorize → 已覆盖
  · 无 @PreAuthorize 且不在基线 → BLOCK（防新增漏加门禁）
  · 无 @PreAuthorize 且在基线 → 允许（不追旧账）

模式与宽 catch 棘轮 218 / business_error 棘轮 441 一致：基线文件 + 只增不减 + self-test 必须能红。
用法：python preauthorize_coverage_gate.py [--self-test] [--write-baseline]
退出码：0 PASS / 2 BLOCK / 3 环境不可用
"""
import glob
import json
import os
import re
import sys

CTRL_DIR = os.environ.get("QA_CTRL_DIR", os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
    "backend", "src", "main", "java", "com", "foodtraceability", "controller"))
BASELINE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "preauthorize_baseline.json")
MAPPING = re.compile(r"@(Get|Post|Put|Delete|Patch)Mapping\b")


def scan_file(path):
    """返回 [(endpoint_key, has_gate)]。

    has_gate = 方法级 @PreAuthorize **或** 类级 @PreAuthorize（类级注解同样生效，必须计入，
    否则会把 PosOrderController 这类「类级门禁」的 11 个端点误判为无门禁）。
    """
    out = []
    with open(path, encoding="utf-8", errors="replace") as f:
        lines = f.read().splitlines()
    first_map = next((i for i, l in enumerate(lines) if MAPPING.search(l)), len(lines))
    class_gate = any("@PreAuthorize" in l for l in lines[:first_map])
    for i, line in enumerate(lines):
        if not MAPPING.search(line):
            continue
        # 向上看 6 行找 @PreAuthorize（注解可写在 mapping 之上）
        has_gate = class_gate or any("@PreAuthorize" in lines[j] for j in range(max(0, i - 6), i))
        # 向下找方法名
        name = "?"
        for j in range(i, min(len(lines), i + 12)):
            m = re.search(r"public\s+[\w<>, ?\[\]]+\s+(\w+)\s*\(", lines[j])
            if m:
                name = m.group(1)
                break
        out.append(("%s#%s" % (os.path.basename(path), name), has_gate))
    return out


def scan_all():
    files = sorted(glob.glob(os.path.join(CTRL_DIR, "**", "*.java"), recursive=True))
    rows = []
    for f in files:
        with open(f, encoding="utf-8", errors="replace") as fh:
            head = fh.read(4000)
        if "@RestController" not in head:
            continue
        rows.extend(scan_file(f))
    return rows


def self_test():
    import tempfile
    d = tempfile.mkdtemp()
    sample = os.path.join(d, "GateSample.java")
    with open(sample, "w", encoding="utf-8") as f:
        f.write("""@RestController
class GateSample {
    @GetMapping("/a")
    public String ok() { return "a"; }
    @PreAuthorize("hasAuthority('x')")
    @GetMapping("/b")
    public String gated() { return "b"; }
}
""")
    rows = scan_file(sample)
    ungated = sorted(k for k, g in rows if not g)
    gated = sorted(k for k, g in rows if g)
    ok1 = ungated == ["GateSample.java#ok"]
    ok2 = gated == ["GateSample.java#gated"]
    print("[%s] self-test 未加门禁端点被识别" % ("PASS" if ok1 else "FAIL"))
    print("[%s] self-test 已加门禁端点被识别" % ("PASS" if ok2 else "FAIL"))
    # 判据：基线内允许、基线外 BLOCK
    base = ["GateSample.java#ok"]
    extra = [k for k in ungated if k not in base]
    ok3 = extra == []
    print("[%s] self-test 基线内缺口不阻断" % ("PASS" if ok3 else "FAIL"))
    base2 = []
    extra2 = [k for k in ungated if k not in base2]
    ok4 = extra2 == ["GateSample.java#ok"]
    print("[%s] self-test 基线外新增缺口必须 BLOCK" % ("PASS" if ok4 else "FAIL"))
    n = sum([ok1, ok2, ok3, ok4])
    print("self-test: %d/4 PASS" % n)
    return 0 if n == 4 else 2


def main():
    if "--self-test" in sys.argv:
        return self_test()
    if not os.path.isdir(CTRL_DIR):
        print("[ENV] controller 目录不存在：%s" % CTRL_DIR)
        return 3
    rows = scan_all()
    if not rows:
        print("[ENV] 未扫描到任何端点")
        return 3
    ungated = sorted(k for k, g in rows if not g)
    gated = [k for k, g in rows if g]
    if "--write-baseline" in sys.argv:
        with open(BASELINE, "w", encoding="utf-8") as f:
            json.dump({"note": "历史缺口基线：只许降不许增", "endpoints": ungated}, f, ensure_ascii=False, indent=2)
        print("[BASELINE] 已写入 %d 个端点 → %s" % (len(ungated), BASELINE))
        return 0
    base = []
    if os.path.exists(BASELINE):
        try:
            base = json.load(open(BASELINE, encoding="utf-8")).get("endpoints", [])
        except Exception:
            base = []
    else:
        print("[WARN] 无基线文件，首次运行请用 --write-baseline 生成")
    new_gaps = [k for k in ungated if k not in base]
    for k in new_gaps:
        print("[BLOCK] 新增无 @PreAuthorize 端点：%s" % k)
    print("[GATE] preauthorize_coverage total=%d 有门禁=%d 基线内缺=%d 新增缺=%d exit=%d"
          % (len(rows), len(gated), len([k for k in ungated if k in base]), len(new_gaps),
             2 if new_gaps else 0))
    return 2 if new_gaps else 0


if __name__ == "__main__":
    sys.exit(main())
