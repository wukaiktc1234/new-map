#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
scan_auth_annotations.py — full-repo auth-annotation literal vs live-DB auditor (N32)

用途
----
静态扫描 backend/src/main/java 下所有鉴权注解，提取其中的字符串字面量，
再与活库的 roles.role_code / roles.role_id_str / permissions.permission_code 全量交叉比对，
输出 A/B/C 三类问题清单 + 位置错配清单。

覆盖注解
--------
@PreAuthorize / @PostAuthorize : 解析 hasRole / hasAnyRole / hasAuthority / hasAnyAuthority 的实参字面量
@Secured / @RolesAllowed       : 全部字面量按角色 authority 处理
@RequiresPermission            : value() 视为权限码（action()/description() 仅元数据，见 PermissionAspect L77）
@RequiresPermissions           : value()/values()/permissions() 视为权限码

提取规则
--------
- 只取字符串字面量；忽略 '*'、空串、含 '$' / '#{' 的表达式、含 '+' 的拼接片段
- 正确区分 Java 源码里的 \\' 转义写法（@PreAuthorize("hasAnyRole(\\'a\\', \\'b\\')")）
- 跳过注释行里的示例注解（Javadoc 中的 @PreAuthorize("hasAuthority('xxx')") 不算真注解）

用法
----
python scan_auth_annotations.py --root backend/src/main/java \
    --psql "<path>/psql.exe" --dbname food_traceability --dbuser postgres \
    --md-out docs/audit/auth-annotation-literal-audit-001.md \
    --json-out docs/audit/auth-annotation-literal-audit-001.json

离线：--snapshot snapshot.json，格式 {"role_codes":[...], "permission_codes":[...]}
"""

import argparse
import difflib
import json
import os
import re
import subprocess
import sys
from collections import defaultdict

ANN_START = re.compile(
    r"@(PreAuthorize|PostAuthorize|Secured|RolesAllowed|RequiresPermission|RequiresPermissions)\b\s*\("
)
CALL = re.compile(r"\b(hasRole|hasAnyRole|hasAuthority|hasAnyAuthority)\s*\(")

ROLE_POSITION_FUNCS = {"hasRole", "hasAnyRole"}


def read_paren(text, open_idx):
    """从 '(' 位置读到配对 ')'，返回 (内容, 结束索引)；字符串内的括号不计"""
    depth = 0
    i = open_idx
    n = len(text)
    in_str = None
    while i < n:
        ch = text[i]
        if in_str:
            if ch == "\\":
                i += 2
                continue
            if ch == in_str:
                in_str = None
        elif ch in "'\"":
            in_str = ch
        elif ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return text[open_idx + 1:i], i
        i += 1
    return text[open_idx + 1:], n


def spel_tokens(argtext):
    """SpEL 实参取词：源码里的 \\' 对扫描而言就是分隔符，先归一再去引号"""
    s = argtext.replace("\\'", "'").replace('\\"', '"')
    out = []
    i, n = 0, len(s)
    while i < n:
        if s[i] in "'\"":
            q = s[i]
            i += 1
            buf = []
            while i < n and s[i] != q:
                buf.append(s[i])
                i += 1
            i += 1
            out.append("".join(buf))
        else:
            i += 1
    return out


def literals_in(text):
    """普通字符串字面量提取（处理 \\' \\\" \\\\ 转义）"""
    out = []
    i, n = 0, len(text)
    while i < n:
        ch = text[i]
        if ch == "\\" and i + 1 < n and text[i + 1] in "'\"":
            i += 1
            ch = text[i]
        if ch in "'\"":
            quote = ch
            i += 1
            buf = []
            while i < n:
                c = text[i]
                if c == "\\" and i + 1 < n:
                    buf.append(text[i + 1])
                    i += 2
                    continue
                if c == quote:
                    i += 1
                    break
                buf.append(c)
                i += 1
            out.append("".join(buf))
            continue
        i += 1
    return out


def line_of(text, idx):
    return text.count("\n", 0, idx) + 1


def keep(lit):
    if not lit:
        return False
    s = lit.strip()
    if s in ("", "*"):
        return False
    if "$" in s or "#{" in s or "+" in s:
        return False
    return True


def scan_file(path, rel, findings):
    with open(path, "r", encoding="utf-8", errors="replace") as fh:
        text = fh.read()

    for m in ANN_START.finditer(text):
        ann = m.group(1)
        line_start = text.rfind("\n", 0, m.start()) + 1
        prefix = text[line_start:m.start()]
        if re.match(r"\s*(\*|//|/\*)", prefix):
            continue
        open_idx = m.end() - 1
        body, _ = read_paren(text, open_idx)
        ln = line_of(text, m.start())

        if ann in ("PreAuthorize", "PostAuthorize"):
            calls = list(CALL.finditer(body))
            if not calls:
                continue
            has_star = re.search(r"\\?['\"]\*\\?['\"]", body) is not None
            for cm in calls:
                fn = cm.group(1)
                cbody, _ = read_paren(body, cm.end() - 1)
                for lit in spel_tokens(cbody):
                    if not keep(lit):
                        continue
                    findings.append({
                        "file": rel, "line": ln, "annotation": ann, "func": fn,
                        "position": "role" if fn in ROLE_POSITION_FUNCS else "authority",
                        "literal": lit.strip(), "has_star": has_star,
                    })
        elif ann in ("Secured", "RolesAllowed"):
            for lit in literals_in(body):
                if not keep(lit):
                    continue
                findings.append({
                    "file": rel, "line": ln, "annotation": ann, "func": "@" + ann,
                    "position": "role", "literal": lit.strip(), "has_star": False,
                })
        else:
            vals = []
            for key in ("value", "values", "permissions"):
                km = re.search(r"\b" + key + r"\s*=\s*", body)
                if not km:
                    continue
                rest = body[km.end():].lstrip()
                if rest.startswith("{"):
                    end = rest.find("}")
                    vals.extend(literals_in(rest[:end + 1]))
                else:
                    vals.extend(literals_in(rest)[:1])
            if not vals:
                vals = literals_in(body)
            for lit in vals:
                if not keep(lit):
                    continue
                findings.append({
                    "file": rel, "line": ln, "annotation": ann, "func": "@" + ann,
                    "position": "authority", "literal": lit.strip(), "has_star": False,
                })


def load_db(args):
    if args.snapshot:
        with open(args.snapshot, "r", encoding="utf-8") as fh:
            snap = json.load(fh)
        return set(snap.get("role_codes", [])), set(snap.get("permission_codes", []))

    sql = (
        "SELECT 'ROLE|' || COALESCE(role_code,'') FROM roles WHERE deleted = 0 "
        "UNION SELECT 'ROLE|' || COALESCE(role_id_str,'') FROM roles WHERE deleted = 0 "
        "UNION SELECT 'PERM|' || COALESCE(permission_code,'') FROM permissions WHERE deleted = 0"
    )
    cmd = [args.psql, "-h", args.dbhost, "-U", args.dbuser, "-d", args.dbname, "-t", "-A", "-c", sql]
    env = dict(os.environ)
    env.setdefault("PGCLIENTENCODING", "UTF8")
    res = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", env=env)
    if res.returncode != 0:
        sys.exit("psql failed: " + (res.stderr or "").strip())

    roles, perms = set(), set()
    for line in res.stdout.splitlines():
        line = line.strip()
        if not line or "|" not in line:
            continue
        kind, code = line.split("|", 1)
        code = code.strip()
        if not code:
            continue
        (roles if kind == "ROLE" else perms).add(code)
    return roles, perms


def norm_role(code):
    s = code.replace("\\", "").strip()
    while s.upper().startswith("ROLE_") and len(s) > 5:
        s = s[5:]
    return s.lower()


def classify(finding, roles, perms):
    lit = finding["literal"]
    role_norms = {norm_role(r) for r in roles}
    rec = dict(finding)
    rec["kind"] = "permission" if ":" in lit else "role"
    rec["suggestions"] = []
    rec["flag"] = ""

    if ":" in lit:
        if lit in perms:
            rec["class"] = "VALID"
        else:
            rec["class"] = "B"
            rec["suggestions"] = difflib.get_close_matches(lit, sorted(perms), n=3, cutoff=0.6)
        if finding["position"] == "role":
            rec["flag"] = "PERMISSION_IN_ROLE_POSITION"
    else:
        n = norm_role(lit)
        if n in role_norms:
            rec["class"] = "VALID"
            if finding["position"] == "authority":
                rec["flag"] = ("ROLE_IN_AUTHORITY_POSITION"
                               if lit.upper().startswith("ROLE_") else "BARE_ROLE_IN_AUTHORITY_POSITION")
        else:
            sugg = difflib.get_close_matches(n, sorted(role_norms), n=3, cutoff=0.6)
            rec["suggestions"] = sugg
            rec["class"] = "A" if sugg else "C"
            if finding["position"] == "authority":
                rec["flag"] = ("ROLE_IN_AUTHORITY_POSITION"
                               if lit.upper().startswith("ROLE_") else "BARE_ROLE_IN_AUTHORITY_POSITION")
            else:
                rec["flag"] = "PHANTOM_IN_ROLE_POSITION"
    return rec


CLASS_TITLE = {
    "A": "A 类：角色幻码（可用近邻匹配纠正标识）",
    "B": "B 类：权限码幻码（permissions 表无此码）",
    "C": "C 类：完全未定义（角色与权限均无，需设计决策）",
    "VALID": "有效（库中存在）",
}
FLAG_MEANING = {
    "PHANTOM_IN_ROLE_POSITION": "幻码写在 hasRole/hasAnyRole（角色位）",
    "PERMISSION_IN_ROLE_POSITION": "权限码写在 hasRole/hasAnyRole → Spring 找 ROLE_<权限码>，必然失败",
    "ROLE_IN_AUTHORITY_POSITION": "ROLE_ 前缀角色码写在 hasAuthority/hasAnyAuthority；前缀对，但角色必须真实存在",
    "BARE_ROLE_IN_AUTHORITY_POSITION": "无前缀角色码写在 hasAuthority/hasAnyAuthority → authority 只有 ROLE_<CODE> 与权限码，必然失败",
}


def write_markdown(recs, roles, perms, root, out_path):
    by_class = defaultdict(list)
    for r in recs:
        by_class[r["class"]].append(r)

    lines = []
    a = lines.append
    a("# 全仓注解权限字面量核对报告（N32 扫描器产出）\n")
    a("> 扫描面：`{}`（全部 `.java`）".format(root.replace("\\", "/")))
    a("> 比对基准：活库 `roles.role_code` ∪ `roles.role_id_str` → {} 个归一化角色码；`permissions.permission_code` → {} 个权限码".format(
        len({norm_role(r) for r in roles}), len(perms)))
    a("> 分类：A=角色幻码（可近邻纠正）/ B=权限码幻码 / C=完全未定义 / VALID=库中存在\n")

    a("## 1. 总览\n")
    a("| 分类 | 出现次数 | 说明 |")
    a("|------|---------|------|")
    for k in ("VALID", "A", "B", "C"):
        a("| **{}** | {} | {} |".format(k, len(by_class[k]), CLASS_TITLE[k]))
    a("| **合计** | {} | |".format(len(recs)))
    a("")

    flags = defaultdict(list)
    for r in recs:
        if r["flag"]:
            flags[r["flag"]].append(r)
    a("## 2. 位置错配（A 卡归一化也覆盖不到的那一类）\n")
    if not flags:
        a("无。\n")
    else:
        a("| 标记 | 次数 | 含义 |")
        a("|------|------|------|")
        for k, v in sorted(flags.items(), key=lambda x: -len(x[1])):
            a("| `{}` | {} | {} |".format(k, len(v), FLAG_MEANING.get(k, "")))
        a("")

    sec = {"A": "3", "B": "4", "C": "5"}
    for cls in ("A", "B", "C"):
        group = by_class[cls]
        a("## {}. {}\n".format(sec[cls], CLASS_TITLE[cls]))
        if not group:
            a("无。\n")
            continue
        agg = defaultdict(list)
        for r in group:
            agg[r["literal"]].append(r)
        a("| 字面量 | 次数 | 近邻建议 | 涉及文件 |")
        a("|--------|------|----------|----------|")
        for lit, items in sorted(agg.items(), key=lambda x: (-len(x[1]), x[0])):
            files = sorted({i["file"].split("/")[-1] for i in items})
            sugg = items[0]["suggestions"]
            a("| `{}` | {} | {} | {} |".format(
                lit, len(items), (", ".join(sugg) if sugg else "—"),
                ", ".join(files[:6]) + ("…" if len(files) > 6 else "")))
        a("")
        a("<details><summary>逐条位置（{} 条）</summary>\n".format(len(group)))
        a("```")
        for r in sorted(group, key=lambda x: (x["file"], x["line"])):
            a("{}:{}  {}  {}('{}'){}".format(
                r["file"], r["line"], r["annotation"], r["func"], r["literal"],
                ("  [" + r["flag"] + "]") if r["flag"] else ""))
        a("```")
        a("</details>\n")

    valid = by_class["VALID"]
    if valid:
        agg = defaultdict(int)
        for r in valid:
            agg[r["literal"]] += 1
        a("## 6. 有效字面量 TOP 20（确认扫描不是在空转）\n")
        a("| 字面量 | 次数 |")
        a("|--------|------|")
        for lit, c in sorted(agg.items(), key=lambda x: -x[1])[:20]:
            a("| `{}` | {} |".format(lit, c))
        a("")

    with open(out_path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(lines))
    return by_class


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", required=True)
    ap.add_argument("--psql")
    ap.add_argument("--dbhost", default="localhost")
    ap.add_argument("--dbname", default="food_traceability")
    ap.add_argument("--dbuser", default="postgres")
    ap.add_argument("--snapshot")
    ap.add_argument("--md-out", required=True)
    ap.add_argument("--json-out")
    args = ap.parse_args()

    roles, perms = load_db(args)

    findings = []
    for dirpath, _dirnames, filenames in os.walk(args.root):
        for fn in filenames:
            if not fn.endswith(".java"):
                continue
            full = os.path.join(dirpath, fn)
            rel = os.path.relpath(full, args.root).replace("\\", "/")
            scan_file(full, rel, findings)

    recs = [classify(f, roles, perms) for f in findings]
    by_class = write_markdown(recs, roles, perms, args.root, args.md_out)

    if args.json_out:
        with open(args.json_out, "w", encoding="utf-8", newline="\n") as fh:
            json.dump({
                "role_codes_normalized": sorted({norm_role(r) for r in roles}),
                "permission_codes": sorted(perms),
                "findings": recs,
            }, fh, ensure_ascii=False, indent=2)

    print("annotation literals: {}".format(len(recs)))
    for k in ("VALID", "A", "B", "C"):
        print("  {}: {}".format(k, len(by_class[k])))
    print("report: " + args.md_out)


if __name__ == "__main__":
    main()
