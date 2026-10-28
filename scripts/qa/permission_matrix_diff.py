#!/usr/bin/env python3
"""permission_matrix_diff.py —— 端点×权限矩阵快照与对拍（v2，2026-10-28 修正）。

## 修正了什么（v1 的 bug）
v1 用「@PreAuthorize 之后的**下一个** Mapping」当端点 → 当注解写在 Mapping **之前/之后**顺序不同、
或方法体里出现其它 Mapping 字符串时会**错位**，导致矩阵表骗人。
v2 改为：以 @PreAuthorize 位置为中心取 ±600 字符窗口，选**距离最近**的 Mapping；并加 --self-test 用已知答案自测。

## 语义约定
`'*'` 不是通配符（Spring hasAuthority 精确匹配），目录里也不存在 permission_code='*'；
它由 SecurityUser 只给 admin 硬加，因此 `'*'` = **admin 兜底**。
因此：`码 or '*'` 的实际准入 = 「持有该码的角色」∪「admin」。
"""
import argparse, json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / 'backend' / 'src' / 'main' / 'java'

PRE = re.compile(r'@PreAuthorize\s*\(\s*"(.+?)"\s*\)', re.S)
MAP_RE = re.compile(r'@(Get|Post|Put|Delete|Patch)Mapping(?:\s*\(\s*(?:value\s*=\s*)?"([^"]*)")?')
CLASS_MAP = re.compile(r'@RequestMapping\s*\(\s*"([^"]*)"')
WINDOW = 600

def authorities(expr):
    codes = set(); roles = []
    for m in re.finditer(r"hasAuthority\(\s*'([^']+)'\s*\)", expr):
        codes.add(m.group(1))
    for m in re.finditer(r"hasAnyAuthority\(([^)]+)\)", expr):
        codes.update(re.findall(r"'([^']+)'", m.group(1)))
    for m in re.finditer(r"hasRole\(\s*'([^']+)'\s*\)", expr):
        roles.append('ROLE_' + m.group(1))
    return sorted(codes), sorted(roles)

def nearest_mapping(text, idx):
    lo = max(0, idx - WINDOW); hi = min(len(text), idx + WINDOW)
    best = None
    for m in MAP_RE.finditer(text, lo, hi):
        d = abs(m.start() - idx)
        if best is None or d < best[0]:
            best = (d, m)
    return best[1] if best else None

def scan_text(text, filename=None, base=''):
    out = []
    for m in PRE.finditer(text):
        mp = nearest_mapping(text, m.start())
        if not mp:
            continue
        verb = mp.group(1).upper()
        sub = mp.group(2) or ''
        endpoint = '%s %s%s' % (verb, base, sub)
        codes, roles = authorities(m.group(1))
        out.append({'endpoint': endpoint, 'file': filename,
                    'expr': ' '.join(m.group(1).split()), 'codes': codes, 'roles': roles})
    return out

def snapshot():
    rows = []
    for path in sorted(SRC.rglob('*Controller.java')):
        text = path.read_text(encoding='utf-8', errors='replace')
        cm = CLASS_MAP.search(text)
        rows.extend(scan_text(text, path.name, cm.group(1) if cm else ''))
    return rows

def is_star_only(r):
    return r['codes'] == ['*'] and not r['roles']

def is_admin_backstop(r):
    return '*' in r['codes'] and (len(r['codes']) > 1 or bool(r['roles']))

def self_test():
    ok = True
    # 样例 1：注解在 Mapping 之前
    t1 = '@PreAuthorize("hasAuthority(\'a:view\') or hasAuthority(\'*\')")\n@GetMapping("/x")\npublic void x(){}'
    r1 = scan_text(t1, 'T1.java', '/v1')
    e1 = (len(r1) == 1 and r1[0]['endpoint'] == 'GET /v1/x'
          and r1[0]['codes'] == ['*', 'a:view'] and not is_star_only(r1[0]) and is_admin_backstop(r1[0]))
    print('  [%s] 注解在 Mapping 之前 → %s' % ('PASS' if e1 else 'FAIL', r1[0]['endpoint'] if r1 else 'NONE'))
    ok &= e1
    # 样例 2：Mapping 在注解之前（v1 会错位）
    t2 = '@PostMapping("/y")\n@PreAuthorize("hasAuthority(\'*\')")\npublic void y(){}'
    r2 = scan_text(t2, 'T2.java', '/v1')
    e2 = (len(r2) == 1 and r2[0]['endpoint'] == 'POST /v1/y' and is_star_only(r2[0]))
    print('  [%s] Mapping 在注解之前 → %s（star-only=%s）' % ('PASS' if e2 else 'FAIL', r2[0]['endpoint'] if r2 else 'NONE', is_star_only(r2[0]) if r2 else '?'))
    ok &= e2
    # 样例 3：两个方法相邻，注解必须各归各的
    t3 = ('@PreAuthorize("hasAuthority(\'m1\')")\n@GetMapping("/one")\npublic void one(){}\n\n'
          '@PreAuthorize("hasAuthority(\'m2\')")\n@GetMapping("/two")\npublic void two(){}')
    r3 = scan_text(t3, 'T3.java', '/v1')
    got = {x['endpoint']: x['codes'] for x in r3}
    e3 = got.get('GET /v1/one') == ['m1'] and got.get('GET /v1/two') == ['m2']
    print('  [%s] 相邻两方法各归各 → %s' % ('PASS' if e3 else 'FAIL', got))
    ok &= e3
    print('自测结果：%s' % ('全部通过' if ok else '存在失败'))
    return 0 if ok else 1

def diff(a, b):
    A = {(x['endpoint'], tuple(x['codes']), tuple(x['roles'])) for x in a}
    B = {(x['endpoint'], tuple(x['codes']), tuple(x['roles'])) for x in b}
    added = sorted(B - A, key=lambda t: t[0])
    removed = sorted(A - B, key=lambda t: t[0])
    print('## 逐格 diff（端点 + 准入码 + 角色）')
    print('改前 %d 格 → 改后 %d 格 ｜ >>> 新增 %d ｜ !!! 移除 %d' % (len(A), len(B), len(added), len(removed)))
    print()
    print('### >>> 新增（改后才有）')
    for e in added:
        print('  >>> %s  码=%s 角色=%s' % (e[0], list(e[1]), list(e[2])))
    print()
    print('### !!! 移除（改前才有）')
    for e in removed:
        print('  !!! %s  码=%s 角色=%s' % (e[0], list(e[1]), list(e[2])))
    print()
    print('### 收紧/放宽判定（同一端点，改前→改后）')
    prev = {}
    for x in a:
        prev.setdefault(x['endpoint'], []).append(x)
    cur = {}
    for x in b:
        cur.setdefault(x['endpoint'], []).append(x)
    tighten = loosen = 0
    for ep in sorted(set(prev) & set(cur)):
        pa = prev[ep][0]; pb = cur[ep][0]
        if pa['codes'] == pb['codes'] and pa['roles'] == pb['roles']:
            continue
        lost_star = ('*' in pa['codes']) and ('*' not in pb['codes'])
        gained = set(pb['codes']) - set(pa['codes'])
        if lost_star and not gained:
            tighten += 1
            print('  收紧 %s: %s → %s' % (ep, pa['codes'], pb['codes']))
        else:
            loosen += 1
            print('  放宽?(需人工看) %s: %s → %s' % (ep, pa['codes'], pb['codes']))
    print()
    print('收紧 %d ｜ 需人工确认 %d' % (tighten, loosen))
    return len(added) + len(removed)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--snapshot')
    ap.add_argument('--diff', nargs=2)
    ap.add_argument('--stats', action='store_true')
    ap.add_argument('--self-test', action='store_true')
    a = ap.parse_args()
    if a.self_test:
        return self_test()
    if a.snapshot:
        rows = snapshot()
        Path(a.snapshot).write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding='utf-8')
        so = sum(1 for r in rows if is_star_only(r))
        ab = sum(1 for r in rows if is_admin_backstop(r))
        print('快照: %s ｜ 端点 %d ｜ star-only %d ｜ 码+* %d' % (a.snapshot, len(rows), so, ab))
        return 0
    if a.stats:
        rows = snapshot()
        so = [r for r in rows if is_star_only(r)]
        ab = [r for r in rows if is_admin_backstop(r)]
        print('端点 %d ｜ star-only %d ｜ 码+admin兜底 %d' % (len(rows), len(so), len(ab)))
        print('\nstar-only 端点（当前只有 admin 能调）：')
        for r in so:
            print('  · %s (%s)' % (r['endpoint'], r['file']))
        return 0
    if a.diff:
        A = json.loads(Path(a.diff[0]).read_text(encoding='utf-8'))
        B = json.loads(Path(a.diff[1]).read_text(encoding='utf-8'))
        diff(A, B)
        return 0
    ap.print_help()
    return 1

if __name__ == '__main__':
    sys.exit(main())
