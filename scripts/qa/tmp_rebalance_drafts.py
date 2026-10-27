# -*- coding: utf-8 -*-
"""尝试让遗留 DRAFT 表重新配平：forceRecalc + bankStatementBalanceFen = bookBalance，再提交/审批。"""
import json, os, subprocess, urllib.request, urllib.error
API = "http://localhost:8081/api"
PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
ENV = dict(os.environ, PGPASSWORD="123456", PGCLIENTENCODING="UTF8")
TAG = "E2E-period-guard-draft-rebalance"

def psql(sql):
    o = subprocess.run([PSQL, "-h", "localhost", "-p", "5432", "-U", "postgres", "-d", "food_traceability",
                        "-t", "-A", "-F", "|", "-c", sql], capture_output=True, text=True,
                       encoding="utf-8", errors="replace", env=ENV, timeout=120)
    return (o.stdout or "").strip()

def rows(sql):
    return [l.split("|") for l in psql(sql).splitlines() if l.strip()]

def http(method, path, token=None, body=None):
    data = json.dumps(body, ensure_ascii=False).encode() if body is not None else None
    h = {"Content-Type": "application/json"}
    if token: h["Authorization"] = "Bearer " + token
    req = urllib.request.Request(API + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=90) as r:
            return r.status, json.loads(r.read().decode() or "{}")
    except urllib.error.HTTPError as e:
        try: return e.code, json.loads(e.read().decode() or "{}")
        except Exception: return e.code, {}
    except Exception as e:
        return -1, {"err": str(e)}

def login(u, p):
    s, b = http("POST", "/v1/auth/login", body={"username": u, "password": p})
    return (b.get("data") or {}).get("token")

tk = login("qa_admin", "Admin@123456")
tk2 = login("qa_approver", "Admin@123456")

for sid, acct, ps_, pe_ in [tuple(r) for r in rows(
        "SELECT statement_id, account_id, period_start::text, period_end::text"
        " FROM reconciliation_statements WHERE deleted=0 AND status='DRAFT' ORDER BY statement_id")]:
    sid, acct = int(sid), int(acct)
    book = int(rows("SELECT COALESCE(book_balance,0) FROM reconciliation_statements WHERE statement_id=%d" % sid)[0][0])
    s, b = http("POST", "/v1/finance/recon/statements", tk,
                {"accountId": acct, "periodStart": ps_, "periodEnd": pe_,
                 "bankStatementBalanceFen": book, "forceRecalc": True, "remark": TAG})
    d = (b.get("data") or {})
    print("recalc %d -> HTTP %s code=%s sid=%s balanced=%s adjBook=%s adjBank=%s amb=%s" % (
        sid, s, b.get("code"), d.get("statementId"), d.get("isBalanced"),
        d.get("adjustedBookBalanceFen"), d.get("adjustedBankBalanceFen"), d.get("ambiguousItemCount")))
    s2, b2 = http("POST", "/v1/finance/recon/statements/%d/submit" % sid, tk, {"remark": TAG + " submit"})
    print("   submit -> HTTP %s code=%s msg=%s" % (s2, b2.get("code"), str(b2.get("message"))[:80]))
    if s2 == 200 and b2.get("code") in (0, 200):
        s3, b3 = http("POST", "/v1/finance/recon/statements/%d/approve" % sid, tk2, {"remark": TAG + " approve"})
        print("   approve -> HTTP %s code=%s msg=%s" % (s3, b3.get("code"), str(b3.get("message"))[:80]))
print("DRAFT 剩余 =", psql("SELECT COUNT(*) FROM reconciliation_statements WHERE deleted=0 AND status='DRAFT'"))
