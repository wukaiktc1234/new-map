#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
权限矩阵核对助手（只读）—— 给 fund-500 核对「没有权限被意外缩小或放大」。

用法：
  python scripts/qa/permission_matrix_diff.py --snapshot .verify-tmp/perm-before
  python scripts/qa/permission_matrix_diff.py --snapshot .verify-tmp/perm-after
  python scripts/qa/permission_matrix_diff.py --before .verify-tmp/perm-before --after .verify-tmp/perm-after
  python scripts/qa/permission_matrix_diff.py --self-test

⚠ join 口径（docs/design/permission-model.md，2026-10-28 修正）：
  role_permissions.permission_id 必须匹配 permissions.permission_id_str（PERM_* 形态）。
  上一版工具错用数字列 permission_id 当 join 键 → 把 1732 条正常授权全报成「未解析」，
  与 A44/A45 全绿自相矛盾。现口径：
    ① 主判定 rp.permission_id == p.permission_id_str
    ② 兼容判定 permission_id_str 未命中时按 p.permission_code 再试，命中标「兼容命中」
    ③ 两路都不命中才是「未解析（悬空授权）」

退出码：0 无差异（或仅新增且 --allow-add）/ 1 有差异 / 2 参数或环境问题
"""
from __future__ import annotations

import argparse
import csv
import os
import subprocess
import sys
from pathlib import Path

DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"


def psql_csv(a, sql):
    argv = [a.psql, "-h", a.pg_host, "-p", str(a.pg_port), "-U", a.pg_user, "-d", a.pg_db,
            "-t", "-A", "-c", r"\copy (%s) TO STDOUT WITH CSV HEADER" % sql]
    env = dict(os.environ, PGPASSWORD=a.pg_password, PGCLIENTENCODING="UTF8")
    p = subprocess.run(argv, capture_output=True, text=True, timeout=300,
                       encoding="utf-8", errors="replace", env=env)
    if p.returncode != 0:
        raise RuntimeError((p.stderr or p.stdout)[:300])
    return p.stdout


def take_snapshot(a, out_dir):
    d = Path(out_dir)
    d.mkdir(parents=True, exist_ok=True)
    # 与 A44/A45 同口径：只统计 deleted=0 的有效授权（软删除行不参与判定）
    rp = psql_csv(a, "SELECT role_id, permission_id FROM role_permissions"
                     " WHERE COALESCE(deleted,0) = 0 ORDER BY role_id, permission_id")
    soft = psql_csv(a, "SELECT role_id, permission_id FROM role_permissions"
                       " WHERE COALESCE(deleted,0) <> 0 ORDER BY role_id, permission_id")
    pm = psql_csv(a, "SELECT permission_id::text, permission_id_str, permission_code"
                     " FROM permissions ORDER BY permission_id_str NULLS LAST, permission_id")
    (d / "role_permissions.csv").write_text(rp, encoding="utf-8")
    (d / "role_permissions_soft_deleted.csv").write_text(soft, encoding="utf-8")
    (d / "permissions.csv").write_text(pm, encoding="utf-8")
    print("[SNAPSHOT] %s" % d)
    print("           role_permissions.csv = %d 行（deleted=0，与 A44/A45 同口径）"
          % max(0, len(rp.strip().splitlines()) - 1))
    print("           role_permissions_soft_deleted.csv = %d 行（软删除，不参与判定）"
          % max(0, len(soft.strip().splitlines()) - 1))
    print("           permissions.csv      = %d 行（含 permission_id_str / permission_code）"
          % max(0, len(pm.strip().splitlines()) - 1))
    return 0


def build_matrix(perm_rows, grant_rows):
    by_str, by_code = {}, {}
    for row in perm_rows:
        pid_str = (row.get("permission_id_str") or "").strip()
        code = (row.get("permission_code") or "").strip()
        if pid_str:
            by_str[pid_str] = code or pid_str
        if code:
            by_code[code] = pid_str or code
    matrix, stats = {}, {"exact": 0, "compat": 0, "unresolved": 0}
    for row in grant_rows:
        role = (row.get("role_id") or "").strip()
        key = (row.get("permission_id") or "").strip()
        if not role or not key:
            continue
        if key in by_str:
            matrix[(role, key)] = "exact"
            stats["exact"] += 1
        elif key in by_code:
            matrix[(role, key + "[兼容命中]")] = "compat"
            stats["compat"] += 1
        else:
            matrix[(role, key + "[未解析]")] = "unresolved"
            stats["unresolved"] += 1
    return matrix, stats


def load(dirpath):
    d = Path(dirpath)
    rp_path, pm_path = d / "role_permissions.csv", d / "permissions.csv"
    if not rp_path.exists() or not pm_path.exists():
        raise RuntimeError("%s 缺少 role_permissions.csv / permissions.csv" % d)
    perm_rows = list(csv.DictReader(pm_path.open(encoding="utf-8", newline="")))
    grant_rows = list(csv.DictReader(rp_path.open(encoding="utf-8", newline="")))
    header = list(perm_rows[0].keys()) if perm_rows else []
    if "permission_id_str" not in header:
        raise RuntimeError("permissions.csv 缺少 permission_id_str 列（旧版快照，请重新 --snapshot）")
    return build_matrix(perm_rows, grant_rows)


def self_test():
    perm_rows = [
        {"permission_id": "141", "permission_id_str": "PERM_USER_VIEW", "permission_code": "user:view"},
        {"permission_id": "142", "permission_id_str": "inventory:query", "permission_code": "inventory:query"},
    ]
    grant_rows = [
        {"role_id": "R_OK", "permission_id": "PERM_USER_VIEW"},
        {"role_id": "R_COMPAT", "permission_id": "user:view"},
        {"role_id": "R_BAD", "permission_id": "PERM_NOT_EXIST"},
    ]
    m, s = build_matrix(perm_rows, grant_rows)
    ok = (s["exact"] == 1 and s["compat"] == 1 and s["unresolved"] == 1
          and ("R_OK", "PERM_USER_VIEW") in m and ("R_BAD", "PERM_NOT_EXIST[未解析]") in m)
    print("[SELF-TEST] matrix=%s" % m)
    print("[SELF-TEST] stats=%s" % s)
    print("[SELF-TEST] %s" % ("PASS（1 主键 / 1 兼容 / 1 未解析，判定正确）" if ok
                              else "FAIL（判定逻辑不符预期，禁止用它下结论）"))
    return 0 if ok else 1


def main():
    ap = argparse.ArgumentParser(description="权限矩阵核对（只读）")
    ap.add_argument("--snapshot")
    ap.add_argument("--before")
    ap.add_argument("--after")
    ap.add_argument("--allow-add", action="store_true")
    ap.add_argument("--only-changes", action="store_true")
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--psql", default=DEFAULT_PSQL)
    ap.add_argument("--pg-host", default="localhost")
    ap.add_argument("--pg-port", type=int, default=5432)
    ap.add_argument("--pg-user", default="postgres")
    ap.add_argument("--pg-password", default="123456")
    ap.add_argument("--pg-db", default="food_traceability")
    a = ap.parse_args()

    if a.self_test:
        return self_test()
    try:
        if a.snapshot:
            return take_snapshot(a, a.snapshot)
        if not (a.before and a.after):
            print("需要 --snapshot DIR 或 --before DIR --after DIR（或 --self-test）", file=sys.stderr)
            return 2
        before, sb = load(a.before)
        after, sa = load(a.after)
    except Exception as e:
        print("[ERROR] %s" % e, file=sys.stderr)
        return 2

    keys = sorted(set(before) | set(after))
    added = [k for k in keys if k in after and k not in before]
    removed = [k for k in keys if k in before and k not in after]
    unchanged = [k for k in keys if k in before and k in after]

    print("=" * 96)
    print("权限矩阵差异   before=%s  after=%s" % (a.before, a.after))
    print("  组合数：改前 %d / 改后 %d ｜ 新增 %d ｜ 移除 %d ｜ 不变 %d" % (
        len(before), len(after), len(added), len(removed), len(unchanged)))
    print("  改前解析：主键 %d / 兼容 %d / 未解析 %d" % (sb["exact"], sb["compat"], sb["unresolved"]))
    print("  改后解析：主键 %d / 兼容 %d / 未解析 %d" % (sa["exact"], sa["compat"], sa["unresolved"]))
    print("=" * 96)
    print("%-24s %-46s %-6s %-6s %s" % ("角色", "权限键", "改前", "改后", "变化"))
    print("-" * 96)
    for k in keys:
        role, keyname = k
        if a.only_changes and k in unchanged:
            continue
        chg = ">>> 新增 <<<" if k in added else ("!!! 移除 !!!" if k in removed else "不变")
        print("%-24s %-46s %-6s %-6s %s" % (role[:24], keyname[:46],
                                            "有" if k in before else "-",
                                            "有" if k in after else "-", chg))

    print("-" * 96)
    if removed:
        print("[!!! 移除 !!!] %d 条（权限被缩小 —— 必须逐条确认是否有意）" % len(removed))
        for role, keyname in removed:
            print("    - %s : %s" % (role, keyname))
    if added:
        print("[>>> 新增 <<<] %d 条（权限被放大 —— 确认是否符合授权口径）" % len(added))
        for role, keyname in added:
            print("    + %s : %s" % (role, keyname))
    if sa["unresolved"] or sb["unresolved"]:
        print("-" * 96)
        print("[未解析] 主键与权限码两路都命中不到：改前 %d / 改后 %d（真悬空，A45 应报红）"
              % (sb["unresolved"], sa["unresolved"]))
    if sb["compat"] or sa["compat"]:
        print("[兼容命中] 按 permission_code 才命中 = **形态非法**（A44/A45 会判红，需归一到 PERM_*）：改前 %d / 改后 %d"
              % (sb["compat"], sa["compat"]))

    # 兼容命中 = A44 判红项，也视为「必须修」，故计入失败
    if (removed or (added and not a.allow_add) or sa["unresolved"] > sb["unresolved"]
            or sa["compat"] > 0):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())