#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""权限目录可加载性门禁（Lead 2026-10-30 P0）。

背景：AdminPermissionCatalog.reload() 因 permissions.parent_id(varchar) ↔ Long 类型不匹配而失败，
     失败被 try-catch 降级成 WARN → catalog 空 → admin 只剩硬编码 352 码，pos:* 不在其中 →
     **admin 进不去 POS 端点**，而门禁一直全绿。

判据（写死）：
  ① permissions 表 type(parent_id) 必须是 bigint（或 integer），否则 FAIL —— 这是加载失败的直接原因；
  ② permissions 里 status=1 的码数 > 0；
  ③ status=1 的码里必须含 pos:order:pay（本次事故的直接缺失项）；
  ④ 不允许存在 parent_id 空串（会让 Long 转换失败）。

退出码：0 PASS / 2 BLOCK / 3 环境不可用
"""
import os
import subprocess
import sys

PSQL = os.environ.get("QA_PSQL", r"P:\my-new-project\PostgreSQL\18\bin\psql.exe")
DB = os.environ.get("QA_PGDB", "food_traceability")
ENC = {"encoding": "utf-8", "errors": "replace"}


def sql(q):
    env = dict(os.environ, PGPASSWORD=os.environ.get("PGPASSWORD", "123456"), PGCLIENTENCODING="UTF8")
    r = subprocess.run([PSQL, "-h", "localhost", "-p", "5432", "-U", "postgres", "-d", DB, "-A", "-t", "-c", q],
                       capture_output=True, text=True, env=env, **ENC)
    return (r.stdout or "").strip(), (r.stderr or "").strip()


def main():
    fails = 0
    out, err = sql("SELECT 1")
    if out != "1":
        print("[ENV] 数据库不可用：%s" % (err or out))
        return 3

    t, _ = sql("SELECT data_type FROM information_schema.columns "
               "WHERE table_schema='public' AND table_name='permissions' AND column_name='parent_id'")
    if t in ("bigint", "integer", "smallint"):
        print("[PASS] permissions.parent_id 类型 = %s（与实体 Long 一致）" % t)
    else:
        fails += 1
        print("[FAIL] permissions.parent_id 类型 = %r，实体 Permission.parentId 是 Long → "
              "AdminPermissionCatalog 会加载失败（P0 2026-10-30）" % t)

    n, _ = sql("SELECT count(*) FROM permissions WHERE status=1")
    if int(n or 0) > 0:
        print("[PASS] permissions status=1 码数 = %s" % n)
    else:
        fails += 1
        print("[FAIL] permissions status=1 码数为 0 → catalog 必空")

    pos, _ = sql("SELECT count(*) FROM permissions WHERE status=1 AND permission_code='pos:order:pay'")
    if int(pos or 0) > 0:
        print("[PASS] status=1 含 pos:order:pay")
    else:
        fails += 1
        print("[FAIL] status=1 缺 pos:order:pay → admin/POS 端点必然 403")

    empty, _ = sql("SELECT count(*) FROM permissions WHERE parent_id IS NOT NULL AND parent_id::text=''" if t not in ("bigint", "integer", "smallint")
                   else "SELECT count(*) FROM permissions WHERE false")
    if int(empty or 0) == 0:
        print("[PASS] 无 parent_id 空串")
    else:
        fails += 1
        print("[FAIL] parent_id 空串 %s 行 → Long 转换失败" % empty)

    print("汇总: %d FAIL" % fails)
    return 0 if fails == 0 else 2


if __name__ == "__main__":
    sys.exit(main())
