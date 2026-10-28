#!/usr/bin/env python3
"""migration_version_gate.py —— 迁移文件版本号静态闸门（EXIT=2 阻断）。

## 为什么需要它（2026-10-28 真实 P0）
V20261028_001__x.sql 与 V20261028_01__y.sql 肉眼几乎看不出区别，
但 Flyway 按段解析后都是版本 20261028.001 -> 扫描阶段直接报
"Found more than one migration with version 20261028.001" -> 应用启动失败。
这个错不会在任何单测里暴露，只会「应用起不来」，所以必须机器拦。

## 规则
1) 版本解析：下划线与点都当分隔符，按段补 0 后比较（与 Flyway 语义一致）；
2) 解析后版本重复 -> EXIT=2，列出 pairwise offenders；
3) 命名规范：必须 V<8位日期>_<3位序号>__<描述>.sql（_01 / _1 混用即报错）；
4) --self-test：喂一对冲突文件名 + 一对合法文件名，验证能否抓到。
"""
import argparse
import re
import sys
import tempfile
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MIG = ROOT / "backend" / "src" / "main" / "resources" / "db" / "migration"

NAME_RE = re.compile(r"^(?P<ver>V[0-9._]+)__(?P<desc>.+)\.sql$")
# 两种合法形态：
#  · 新标准：V<8位日期>_<3位序号>__desc.sql   （2026-10-28 起）
#  · 历史遗留：V1.0.0.400__desc.sql          （点分版本，已存在 41 个，不动）
STRICT_RE = re.compile(r"^V\d{8}_\d{3}__[A-Za-z0-9_]+\.sql$")

ALLOWLIST = ROOT / "scripts" / "qa" / "migration-legacy-allowlist.txt"


def load_allowlist():
    """只认文件里**具体文件名**；不按形状放行。"""
    allowed = {}
    if not ALLOWLIST.exists():
        return allowed
    for line in ALLOWLIST.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        parts = [x.strip() for x in line.split("|")]
        if parts and parts[0].endswith(".sql"):
            allowed[parts[0]] = parts[1] if len(parts) > 1 else ""
    return allowed


def parse_version(ver):
    body = ver[1:] if ver.startswith("V") else ver
    parts = [p for p in re.split(r"[._]", body) if p != ""]
    nums = []
    for p in parts:
        try:
            nums.append(int(p))
        except ValueError:
            return None
    while len(nums) > 1 and nums[-1] == 0:
        nums.pop()
    return tuple(nums)


def scan(dirpath):
    allowed = load_allowlist()
    dup = defaultdict(list)
    badname = []
    unparsed = []
    allowlisted = []
    for p in sorted(Path(dirpath).glob("*.sql")):
        m = NAME_RE.match(p.name)
        if not m:
            unparsed.append(p.name)
            continue
        v = parse_version(m.group("ver"))
        if v is None:
            unparsed.append(p.name)
            continue
        dup[v].append(p.name)
        if not STRICT_RE.match(p.name):
            if p.name in allowed:
                allowlisted.append(p.name)
            else:
                badname.append(p.name)
    scan.allowlisted = allowlisted
    return dup, badname, unparsed


def report(dup, badname, unparsed, verbose=True):
    conflicts = {v: names for v, names in dup.items() if len(names) > 1}
    fail = False
    if conflicts:
        fail = True
        if verbose:
            print("[FAIL] 版本号重复 %d 组（Flyway 会拒绝启动）：" % len(conflicts))
            for v, names in sorted(conflicts.items()):
                print("  版本 " + ".".join(str(x) for x in v) + " ->")
                for n in names:
                    print("     - " + n)
    if unparsed:
        fail = True
        if verbose:
            print("[FAIL] 文件名无法解析出 Flyway 版本 %d 个：" % len(unparsed))
            for n in unparsed:
                print("     - " + n)
    if badname:
        fail = True
        if verbose:
            print("[FAIL] 命名不规范 %d 个（应为 V<8位日期>_<3位序号>__desc.sql 或历史点分版本 V1.0.0.400__desc.sql）：" % len(badname))
            for n in badname:
                print("     - " + n)
    total = sum(len(v) for v in dup.values())
    n_allow = len(getattr(scan, "allowlisted", []))
    if verbose:
        print("[INFO] 放行 %d 个历史文件（白名单 scripts/qa/migration-legacy-allowlist.txt；只允许存量、不允许新增）" % n_allow)
    if not fail and verbose:
        print("[PASS] 迁移版本 gate：文件 %d 个，版本唯一、命名规范" % total)
    return fail


def self_test():
    ok = True
    with tempfile.TemporaryDirectory() as d:
        Path(d, "V20261028_001__a.sql").write_text("select 1;", encoding="utf-8")
        Path(d, "V20261028_01__b.sql").write_text("select 1;", encoding="utf-8")
        dup, badname, unparsed = scan(d)
        caught = any(len(v) > 1 for v in dup.values())
        print("  [%s] 冲突对 V20261028_001 / V20261028_01 被抓到" % ("PASS" if caught else "FAIL"))
        ok = ok and caught
    with tempfile.TemporaryDirectory() as d:
        Path(d, "V20261028_001__a.sql").write_text("select 1;", encoding="utf-8")
        Path(d, "V20261028_002__b.sql").write_text("select 1;", encoding="utf-8")
        dup, badname, unparsed = scan(d)
        clean = (not any(len(v) > 1 for v in dup.values())) and not badname and not unparsed
        print("  [%s] 合法命名 V20261028_001 / _002 无告警" % ("PASS" if clean else "FAIL"))
        ok = ok and clean
    with tempfile.TemporaryDirectory() as d:
        Path(d, "V20261028_1__x.sql").write_text("select 1;", encoding="utf-8")
        dup, badname, unparsed = scan(d)
        caught2 = len(badname) == 1
        print("  [%s] 序号只写 _1（非三位）被命名规范抓到" % ("PASS" if caught2 else "FAIL"))
        ok = ok and caught2
    print("自测结果：%s" % ("全部通过" if ok else "存在失败"))
    return 0 if ok else 1


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default=str(MIG))
    ap.add_argument("--self-test", action="store_true")
    a = ap.parse_args()
    if a.self_test:
        return self_test()
    dup, badname, unparsed = scan(a.dir)
    fail = report(dup, badname, unparsed)
    return 2 if fail else 0


if __name__ == "__main__":
    sys.exit(main())
