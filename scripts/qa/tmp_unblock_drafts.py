# -*- coding: utf-8 -*-
"""把阻塞结账的 2 张 DRAFT 调节表走接口提交+审批（无删除端点，405/404 已实测）。"""
import json, os, subprocess, urllib.request, urllib.error
API = "http://localhost:8081/api"
PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
ENV = dict(os.environ, PGPASSWORD="123456", PGCLIENTENCODING="UTF8")

def psql(sql):
    o = subprocess.run([PSQL, "-h", "localhost", "-p", "5432", "-U", "postgres", "-d", "food_traceability",
                        "-t", "-A", "-F", "|", "-c", sql], capture_output=True, text=True,
                       encoding="utf-8", errors="replace", env=ENV, timeout=120)
    return (o.stdout or "").strip()

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

def login(u, p):
    s, b = http("POST", "/v1/auth/login", body={"username": u, "password": p})
    return (b.get("data") or {}).get("token")

tk_admin = login("qa_admin", "Admin@123456")
tk_appr = login("qa_approver", "Admin@123456")
print("tokens:", "admin" if tk_admin else "-", "approver" if tk_appr else "-")

ids = [int(x) for x in psql("SELECT statement_id FROM reconciliation_statements WHERE deleted=0 AND status='DRAFT' ORDER BY statement_id").splitlines() if x.strip()]
print("DRAFT ids =", ids)
for sid in ids:
    s1, b1 = http("POST", "/v1/finance/recon/statements/%d/submit" % sid, tk_admin, {"remark": "E2E-period-guard 前置：提交 DRAFT 以解锁结账"})
    print("  submit %d -> HTTP %s code=%s msg=%s" % (sid, s1, b1.get("code"), str(b1.get("message"))[:70]))
    s2, b2 = http("POST", "/v1/finance/recon/statements/%d/approve" % sid, tk_appr, {"remark": "E2E-period-guard 前置：审批以解锁结账"})
    if not (s2 == 200 and b2.get("code") in (0, 200)):
        s2, b2 = http("POST", "/v1/finance/recon/statements/%d/approve" % sid, tk_admin, {"remark": "E2E-period-guard 前置：审批以解锁结账"})
    print("  approve %d -> HTTP %s code=%s msg=%s" % (sid, s2, b2.get("code"), str(b2.get("message"))[:70]))
print("DRAFT 剩余 =", psql("SELECT COUNT(*) FROM reconciliation_statements WHERE deleted=0 AND status='DRAFT'"))
print("状态分布 =", psql("SELECT status || '=' || COUNT(*) FROM reconciliation_statements WHERE deleted=0 GROUP BY status ORDER BY status").replace("\n", " | "))
print("OPEN 差异剩余 =", psql("SELECT COUNT(*) FROM recon_differences WHERE status IN ('OPEN','ASSIGNED','RESOLVING')"))
