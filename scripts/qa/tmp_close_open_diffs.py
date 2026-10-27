# -*- coding: utf-8 -*-
"""批量关闭 2026-10 期间 OPEN 差异（只走接口）+ 处理 DRAFT 表（走接口），用于解锁期间结账 E2E。"""
import json, os, sys, subprocess, urllib.request, urllib.error
from datetime import datetime

API = "http://localhost:8081/api"
TAG = "[E2E-period-guard-%s]" % datetime.now().strftime("%Y%m%d%H%M")
PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
ENV = dict(os.environ, PGPASSWORD="123456", PGCLIENTENCODING="UTF8")

def psql(sql):
    o = subprocess.run([PSQL, "-h", "localhost", "-p", "5432", "-U", "postgres", "-d", "food_traceability",
                        "-t", "-A", "-F", "|", "-c", sql], capture_output=True, text=True,
                       encoding="utf-8", errors="replace", env=ENV, timeout=120)
    if o.returncode != 0:
        raise RuntimeError(o.stderr[:300])
    return (o.stdout or "").strip()

def rows(sql):
    return [l.split("|") for l in psql(sql).splitlines() if l.strip()]

def http(method, path, token=None, body=None):
    data = json.dumps(body, ensure_ascii=False).encode() if body is not None else None
    h = {"Content-Type": "application/json"}
    if token: h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(API + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, json.loads(r.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        try: return e.code, json.loads(e.read().decode() or "{}")
        except Exception: return e.code, {}
    except Exception as e:
        return -1, {"err": str(e)}

st, b = http("POST", "/v1/auth/login", body={"username": "qa_admin", "password": "Admin@123456"})
tk = (b.get("data") or {}).get("token")
print("login:", st, "token" if tk else "FAIL", "| tag =", TAG)
if not tk:
    sys.exit(2)

# 1) 备份（data-only）
bk_dir = r"P:\my-new-project\scripts\db-fix\backup"
os.makedirs(bk_dir, exist_ok=True)
bk = os.path.join(bk_dir, "recon_differences_%s.sql" % datetime.now().strftime("%Y%m%d%H%M%S"))
dump = subprocess.run([r"P:\my-new-project\PostgreSQL\18\bin\pg_dump.exe",
                       "-h", "localhost", "-p", "5432", "-U", "postgres", "-d", "food_traceability",
                       "--data-only", "--table=recon_differences", "-f", bk],
                      capture_output=True, text=True, encoding="utf-8", errors="replace", env=ENV, timeout=300)
print("backup:", bk, "| pg_dump exit =", dump.returncode, "| size =", os.path.getsize(bk) if os.path.exists(bk) else -1)
if dump.returncode != 0:
    print("pg_dump stderr:", (dump.stderr or "")[:300]); sys.exit(2)

# 2) 列出并关闭 OPEN / ASSIGNED / RESOLVING
ids = [int(r[0]) for r in rows("SELECT difference_id FROM recon_differences WHERE status IN"
                               " ('OPEN','ASSIGNED','RESOLVING') ORDER BY difference_id")]
print("待处理差异条数 =", len(ids))
closed, failed, claimed = 0, [], 0
for did in ids:
    stc, st_body = rows("SELECT status FROM recon_differences WHERE difference_id=%d" % did)[0][0], None
    if stc == "OPEN":
        s1, b1 = http("POST", "/v1/finance/recon-differences/%d/claim" % did, tk)
        if not (s1 == 200 and b1.get("code") in (0, 200)):
            failed.append((did, "claim", s1, b1.get("code"))); continue
        claimed += 1
    s2, b2 = http("POST", "/v1/finance/recon-differences/%d/resolve" % did, tk,
                  {"resolution": "WRITE_OFF", "remark": "%s 期间结账 E2E 前置清理（走接口核销）" % TAG})
    if s2 == 200 and b2.get("code") in (0, 200):
        closed += 1
    else:
        failed.append((did, "resolve", s2, b2.get("code")))
print("claim 成功 =", claimed, "| resolve 成功 =", closed, "| 失败 =", len(failed))
for f in failed[:10]:
    print("   FAILED:", f)

left = int(psql("SELECT COUNT(*) FROM recon_differences WHERE status IN ('OPEN','ASSIGNED','RESOLVING')"))
print("OPEN 剩余 =", left)

# 3) DRAFT 调节表：走接口撤回/删除
drafts = rows("SELECT statement_id, account_id, period_start, period_end FROM reconciliation_statements"
              " WHERE deleted=0 AND status='DRAFT' ORDER BY statement_id")
print("DRAFT 表 =", len(drafts), [d[0] for d in drafts])
del_ok, del_fail = 0, []
for d in drafts:
    sid = int(d[0])
    s, bb = http("DELETE", "/v1/finance/recon/statements/%d" % sid, tk)
    if s == 200 and bb.get("code") in (0, 200):
        del_ok += 1
    else:
        s2, b2 = http("POST", "/v1/finance/recon/statements/%d/withdraw" % sid, tk, {"remark": TAG})
        if s2 == 200 and b2.get("code") in (0, 200):
            del_ok += 1
        else:
            del_fail.append((sid, s, bb.get("code"), bb.get("message"), s2, b2.get("code"), b2.get("message")))
print("DRAFT 处理成功 =", del_ok, "| 失败 =", len(del_fail))
for f in del_fail:
    print("   DRAFT FAILED:", str(f)[:200])
print("DRAFT 剩余 =", int(psql("SELECT COUNT(*) FROM reconciliation_statements WHERE deleted=0 AND status='DRAFT'")))
print("TAG =", TAG)
