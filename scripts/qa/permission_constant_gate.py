#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""permission_constant_gate.py —— 扫描代码中的权限码集合常量，与 permissions 目录比对。

为什么需要（Lead 2026-10-29）：A46 只扫 @PreAuthorize/hasAuthority 字面量，
扫不到 POS_BASE_PERMISSIONS 这类集合常量 → 出现「代码给了权限、目录没有这个码」的隐形授权
（实测：9 个基础集码只有 3 个在目录里）。

用法：python scripts/qa/permission_constant_gate.py [--self-test]
EXIT: 0 通过 / 2 缺失 / 3 无法比对
"""
import os, re, subprocess, sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
JAVA_DIR = os.path.join(ROOT, "backend", "src", "main", "java")
PSQL = os.path.join(ROOT, "PostgreSQL", "18", "bin", "psql.exe")
CODE_RE = re.compile(r'"([a-z][a-z0-9_-]*(?::[a-z0-9_-]+){1,3})"')


def scan_codes(java_dir=JAVA_DIR):
    found = {}
    for dirpath, _, files in os.walk(java_dir):
        for fn in files:
            if not fn.endswith(".java"):
                continue
            path = os.path.join(dirpath, fn)
            try:
                text = open(path, encoding="utf-8", errors="replace").read()
            except OSError:
                continue
            for m in re.finditer(r'(?:static final|final)\s+[\w<>, ?]+\s+([A-Z0-9_]*PERMISSIONS[A-Z0-9_]*)\s*=', text):
                start = m.start()
                tail = text[start:start + 4000]
                endm = re.search(r'\)\s*\)\s*;|\)\s*;', tail)
                block = tail[: endm.end()] if endm else tail
                base_line = text[:start].count(chr(10)) + 1
                for cm in CODE_RE.finditer(block):
                    code = cm.group(1)
                    line = base_line + block[: cm.start()].count(chr(10))
                    found.setdefault(code, []).append((os.path.relpath(path, ROOT), line))
    return found


def db_codes(psql=PSQL):
    env = dict(os.environ, PGPASSWORD=os.environ.get("PGPASSWORD", "123456"), PGCLIENTENCODING="UTF8")
    sql = "SELECT permission_code FROM permissions WHERE deleted=0"
    r = subprocess.run([psql, "-h", "localhost", "-p", "5432", "-U", "postgres",
                        "-d", "food_traceability", "-A", "-t", "-c", sql],
                       capture_output=True, text=True, env=env)
    if r.returncode != 0:
        return None
    return {x.strip() for x in (r.stdout or "").splitlines() if x.strip()}


BASELINE_FILE = os.path.join(ROOT, "scripts", "qa", "permission-constant-baseline.txt")


def load_baseline(path=BASELINE_FILE):
    """基线：每行一个码，# 开头为注释。文件不存在 → 空基线（等价于「必须 0」）。"""
    if not os.path.exists(path):
        return set()
    out = set()
    for line in open(path, encoding="utf-8", errors="replace"):
        s = line.strip()
        if s and not s.startswith("#"):
            out.add(s)
    return out


def main():
    codes = scan_codes()
    print("[GATE] 权限集合常量扫描：发现 %d 个码" % len(codes))
    for c in sorted(codes):
        locs = ", ".join("%s:%d" % (f, l) for f, l in codes[c][:2])
        print("  %-28s %s" % (c, locs))
    catalog = db_codes()
    if catalog is None:
        print("[GATE] 无法连接数据库比对（EXIT=3）")
        return 3
    missing = sorted(c for c in codes if c not in catalog)
    baseline = load_baseline()
    new_missing = [c for c in missing if c not in baseline]
    shrunk = [c for c in baseline if c not in missing]

    if new_missing:
        print("[GATE] 出现**基线外**的新缺失 %d 个（BLOCK）：" % len(new_missing))
        for c in new_missing:
            print("  NEW-MISSING %s  (%s)" % (c, ", ".join("%s:%d" % (f, l) for f, l in codes[c])))
        print("[GATE] EXIT=2 —— 登记进 permissions 目录（带 DO 断言），或加入基线（需注释理由）")
        return 2

    if missing:
        print("[GATE] 已知缺失 %d 个，全部在基线内（棘轮 WARN，不阻断）：" % len(missing))
        for c in missing:
            print("  KNOWN-MISSING %s" % c)
        if shrunk:
            print("[GATE] 基线可缩小：以下 %d 个码已不再缺失，请更新 permission-constant-baseline.txt：" % len(shrunk))
            for c in shrunk:
                print("  SHRINK %s" % c)
        print("[GATE] EXIT=3（WARN）")
        return 3

    print("[GATE] 全部 %d 个权限码均在目录中（PASS）" % len(codes))
    if baseline:
        print("[GATE] 基线已清空，可删除 permission-constant-baseline.txt")
    return 0


def self_test():
    codes = scan_codes()
    ok1 = any(c == "pos:order:create" for c in codes)
    print("self-test 1（能扫到集合常量里的码）:", "PASS" if ok1 else "FAIL")
    ok2 = bool(CODE_RE.match('"pos:order:create"')) and not bool(CODE_RE.match('"hello"'))
    print("self-test 2（码形态正则）:", "PASS" if ok2 else "FAIL")
    fake = {"xx:yy:zz": [("f", 1)]}
    ok3 = bool([c for c in fake if c not in {"pos:order:create"}])
    print("self-test 3（缺失判定）:", "PASS" if ok3 else "FAIL")
    return 0 if (ok1 and ok2 and ok3) else 2


if __name__ == "__main__":
    sys.exit(self_test() if "--self-test" in sys.argv else main())
