#!/usr/bin/env python3
# -*- coding: utf-8 -*-
r"""Migration SQL static gate (Lead 2026-10-30; triggered by two real outages).

Incident history:
  1) V20261030_012 used `DO $` / `END $;` (single dollar) -> Flyway failed -> service down.
  2) V20261030_014 had CJK comment where a byte got eaten, closing quote lost ->
     SQL string literal never closed -> syntax error -> service down again.

Why a state machine (not regex): regex cannot separate comment vs string vs doubled quote.
  counter-examples: RAISE EXCEPTION 'a--b';   and   RAISE EXCEPTION 'it''s';

Rules:
  1) strict UTF-8 decode failure            -> BLOCK
  2) unterminated string at EOF             -> BLOCK  (odd single-quote count, precise form)
  3) odd number of $$ delimiters            -> BLOCK
  4) non-ASCII characters present           -> WARN   (Owner: keep migration files pure ASCII)
  5) -- or /* inside a string literal       -> WARN   (looks like a comment, easy to misread)
  6) already-executed migration hit         -> all BLOCK downgraded to WARN (must not edit)

Usage: python migration_sql_lint.py [--self-test] [--executed-versions=v1,v2]
Exit: 0 PASS / 2 BLOCK / 3 environment unavailable
"""
import glob
import os
import re
import sys

MIG_DIR = os.environ.get("QA_MIG_DIR",
    os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
                 "backend", "src", "main", "resources", "db", "migration"))
NEW_SINCE = "20261030"


def scan_sql(text):
    """State machine: returns dict with in_str, dollar_count, non_ascii, warns."""
    i = 0
    n = len(text)
    in_str = False
    in_line_c = False
    in_block_c = False
    dollar_count = 0
    non_ascii = 0
    warns = []
    single_dollar_hits = []
    while i < n:
        c = text[i]
        if ord(c) > 127:
            non_ascii += 1
        if in_line_c:
            if c == "\n":
                in_line_c = False
        elif in_block_c:
            if text.startswith("*/", i):
                in_block_c = False
                i += 1
        elif in_str:
            if c == "'":
                if i + 1 < n and text[i + 1] == "'":
                    i += 1
                else:
                    in_str = False
            elif text.startswith("--", i) or text.startswith("/*", i):
                warns.append("comment marker inside string literal near char %d" % i)
        else:
            if text.startswith("--", i):
                in_line_c = True
                i += 1
            elif text.startswith("/*", i):
                in_block_c = True
                i += 1
            elif c == "'":
                in_str = True
            elif text.startswith(chr(36) * 2, i):
                dollar_count += 1
                i += 1
            elif c == "$":
                # single-dollar delimiter (incident #1: DO $ ... END $;) -> BLOCK
                prev_word = text[:i].rstrip()
                single_dollar_hits.append(prev_word[-3:].upper() + "$ @%d" % i)
        i += 1
    return {"in_str": in_str, "dollar_count": dollar_count,
            "non_ascii": non_ascii, "warns": warns,
            "single_dollar": single_dollar_hits}


def check_bytes(raw, executed=False):
    blocks = []
    warns = []
    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError as e:
        blocks.append("strict UTF-8 decode failed: %s" % e)
        return blocks, warns
    res = scan_sql(text)
    if res["in_str"]:
        blocks.append("unterminated string literal at EOF (odd single-quote count)")
    if res["dollar_count"] % 2 != 0:
        blocks.append("odd number of $ delimiters (%d)" % res["dollar_count"])
    if res["single_dollar"]:
        blocks.append("single-dollar delimiter found (incident #1 form): %s"
                      % "; ".join(res["single_dollar"][:3]))
    if res["non_ascii"] > 0:
        if executed:
            warns.append("HISTORICAL_EXEMPT: %d non-ASCII chars (pre-20261030; editing would break checksum)"
                         % res["non_ascii"])
        else:
            warns.append("%d non-ASCII characters (Owner: keep new migrations pure ASCII)" % res["non_ascii"])
    warns.extend(res["warns"])
    if executed:
        warns.extend("[already executed - do not edit] " + b for b in blocks)
        blocks = []
    return blocks, warns


def self_test():
    cases = [
        ("bad: GBK bytes decoded as UTF-8", "中".encode("gbk"), False),
        ("bad: unterminated string", b"RAISE EXCEPTION 'abc);\n", False),
        ("good: English comment containing don't", b"-- don't edit\nSELECT 1;\n", True),
        ("good: doubled quote escape", b"RAISE EXCEPTION 'it''s ok';\n", True),
        ("good+warn: comment marker inside string", b"RAISE EXCEPTION 'a--b';\n", True),
        ("good: balanced dollar block", b"DO $$ BEGIN NULL; END $$;\n", True),
        ("bad: single dollar DO", b"DO $ BEGIN NULL; END $;\n", False),
    ]
    ok = 0
    for name, raw, expect_pass in cases:
        blocks, _w = check_bytes(raw)
        got = not blocks
        flag = "PASS" if got == expect_pass else "FAIL"
        if got == expect_pass:
            ok += 1
        print("[%s] self-test %-44s expect_pass=%-5s got=%-5s %s"
              % (flag, name, expect_pass, got, blocks[:1]))
    print("self-test: %d/%d PASS" % (ok, len(cases)))
    return 0 if ok == len(cases) else 2


def is_executed(fname, executed_set):
    m = re.match(r"V(\d{8})_(\d{3})__", os.path.basename(fname))
    if not m:
        return False
    if executed_set is not None:
        return ("%s.%s" % (m.group(1), m.group(2))) in executed_set
    return m.group(1) < NEW_SINCE


def main():
    if "--self-test" in sys.argv:
        return self_test()
    executed_set = None
    for a in sys.argv[1:]:
        if a.startswith("--executed-versions="):
            executed_set = {x.strip() for x in a.split("=", 1)[1].split(",") if x.strip()}
    files = sorted(glob.glob(os.path.join(MIG_DIR, "*.sql")))
    if not files:
        print("[ENV] migration dir not found: %s" % MIG_DIR)
        return 3
    total_block = 0
    total_warn = 0
    non_ascii_files = []
    for f in files:
        raw = open(f, "rb").read()
        ex = is_executed(f, executed_set)
        blocks, warns = check_bytes(raw, executed=ex)
        for b in blocks:
            print("[BLOCK] %s: %s" % (os.path.basename(f), b))
        for w in warns:
            print("[WARN]  %s: %s" % (os.path.basename(f), w))
        if any("non-ASCII" in w for w in warns):
            non_ascii_files.append(os.path.basename(f))
        total_block += len(blocks)
        total_warn += len(warns)
    print("-" * 78)
    print("[STAT] scanned=%d  BLOCK=%d  WARN=%d" % (len(files), total_block, total_warn))
    if non_ascii_files:
        print("[STAT] files with non-ASCII (WARN, %d): %s"
              % (len(non_ascii_files), ", ".join(non_ascii_files[:12]) + (" ..." if len(non_ascii_files) > 12 else "")))
    print("[RESULT] EXIT=%d" % (2 if total_block else 0))
    return 2 if total_block else 0


if __name__ == "__main__":
    sys.exit(main())
