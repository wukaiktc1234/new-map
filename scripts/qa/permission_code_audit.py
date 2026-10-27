#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""A46 断言：代码里声明的权限码 ↔ permissions 目录必须一一对应。

为什么：今天 hr:contract-template:view 就是「代码要求一个不存在的权限 → 除通配超管外谁都被 403，
而且不报错」。这类缺陷只在权限边界上咬人，日志里看不到。
与 A44（权限串形态）、A45（引用可解析）合起来三个方向：形态 / 引用 / 代码与目录的对应。

判据：grep hasAuthority('X') / hasAnyAuthority('X','Y') / hasRole 不纳入；
     取 X 与 permissions.permission_id_str 比对；输出「代码有、目录无」→ 期望 0 行。
退出码：有缺失 → 2；无缺失 → 0。
"""
from __future__ import annotations

import os, re, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'backend' / 'src' / 'main' / 'java'
PSQL = str(ROOT / 'PostgreSQL' / '18' / 'bin' / 'psql.exe')
AUTH_RE = re.compile(r"hasAuthority\s*\(\s*'([^']+)'\s*\)")
ANY_AUTH_RE = re.compile(r"hasAnyAuthority\s*\(([^)]*)\)")
ROLE_RE = re.compile(r"hasRole\s*\(\s*'([^']+)'\s*\)")
ANY_ROLE_RE = re.compile(r"hasAnyRole\s*\(([^)]*)\)")
STR_RE = re.compile(r"'([^']+)'")


def catalog(db='food_traceability'):
    """返回 (权限码集合, 角色码集合)：ROLE_* 属于角色，不该拿去和 permissions 比。"""
    env = dict(os.environ, PGPASSWORD='123456', PGCLIENTENCODING='UTF8')
    # 权威口径（2026-10-28 用 UserMapper.xml 复核后确定）：
    #   role_permissions.permission_id → permissions.permission_id_str（PERM_* 形态）做 join，
    #   但**授予给 Spring Security 的 authority 字符串是 permissions.permission_code**（user:view 形态）。
    #   所以 @PreAuthorize 里的码必须能在 permission_code 中找到；ROLE_* 走 roles。
    sql = ("SELECT permission_code FROM permissions WHERE COALESCE(deleted,0)=0 AND permission_code IS NOT NULL "
           "UNION ALL SELECT role_code FROM roles "
           "UNION ALL SELECT role_id_str FROM roles")
    r = subprocess.run([PSQL, '-h', 'localhost', '-p', '5432', '-U', 'postgres', '-d', db,
                        '-A', '-t', '-c', sql], capture_output=True, text=True,
                       encoding='utf-8', errors='replace', env=env, timeout=120)
    if r.returncode != 0:
        print('[ERROR] 查询 permissions 失败: %s' % (r.stderr or '')[:160])
        return None
    return {x.strip() for x in (r.stdout or '').splitlines() if x.strip()}


BLOCK_COMMENT_RE = re.compile(r'/\*.*?\*/', re.S)
LINE_COMMENT_RE = re.compile(r'//[^\n]*')


def strip_comments(text):
    """去掉注释再扫：javadoc 里常出现 hasAuthority('xxx') 这种示例，会被误判成真实权限码。"""
    return LINE_COMMENT_RE.sub('', BLOCK_COMMENT_RE.sub('', text))


def main():
    db = 'food_traceability'
    if '--db' in sys.argv:
        db = sys.argv[sys.argv.index('--db') + 1]
    have = catalog(db)
    if have is None:
        return 2
    used = {}
    for path in sorted(SRC.rglob('*.java')):
        text = strip_comments(path.read_text(encoding='utf-8', errors='replace'))
        if 'hasAuthority' not in text:
            continue
        rel = str(path.relative_to(ROOT)).replace(chr(92), '/')
        for m in AUTH_RE.finditer(text):
            used.setdefault(m.group(1).replace(chr(92), '').strip(), set()).add(rel)
        for m in ANY_AUTH_RE.finditer(text):
            for code in STR_RE.findall(m.group(1)):
                used.setdefault(code.replace(chr(92), '').strip(), set()).add(rel)
        # hasRole('X') 里 X 不带 ROLE_ 前缀，比对时补上
        for m in ROLE_RE.finditer(text):
            v = m.group(1).replace(chr(92), '').upper()
            used.setdefault(v if v.startswith('ROLE_') else 'ROLE_' + v, set()).add(rel)
        for m in ANY_ROLE_RE.finditer(text):
            for code in STR_RE.findall(m.group(1)):
                u = code.replace(chr(92), '').strip().upper()
                used.setdefault(u if u.startswith('ROLE_') else 'ROLE_' + u, set()).add(rel)
    perms = {x for x in have if not x.startswith('ROLE_') and not x.startswith('ROLE')}
    roles = {x for x in have if x not in perms}
    missing = {k: v for k, v in used.items() if k not in have and k != '*'}
    print('# A46：代码声明的权限码 ↔ permissions 目录')
    print('')
    used_perms = {k: v for k, v in used.items() if not k.startswith('ROLE_')}
    used_roles = {k: v for k, v in used.items() if k.startswith('ROLE_')}
    print('代码中权限码 %d 个（其中角色码 %d 个，按 roles 表比对）｜ 目录缺失：%d 个'
          % (len(used_perms), len(used_roles), len([k for k in missing if not k.startswith('ROLE_')])))
    if missing:
        print('')
        print('| 权限码（代码有、目录无） | 出现在 |')
        print('|--------------------------|----------|')
        for k in sorted(missing):
            files = sorted(missing[k])
            print('| `%s` | %s |' % (k, ', '.join(Path(x).name for x in files[:3]) + (' …共%d' % len(files) if len(files) > 3 else '')))
        print('')
        print('[BLOCK] A46 失败：%d 个权限码代码要求但目录不存在 → 除通配超管外全部 403 且不报错' % len(missing))
        return 2
    print('[GATE] A46 通过：代码声明的权限码全部在 permissions 目录中')
    return 0


if __name__ == '__main__':
    sys.exit(main())
