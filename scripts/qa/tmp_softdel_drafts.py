# -*- coding: utf-8 -*-
"""lead 授权（一次性）：备份后软删 statement_id IN (107,108) 且 status='DRAFT' 的调节表与 items。"""
import os, subprocess, sys
from datetime import datetime

PG = r"P:\my-new-project\PostgreSQL\18\bin"
ENV = dict(os.environ, PGPASSWORD="123456", PGCLIENTENCODING="UTF8")
BK_DIR = r"P:\my-new-project\scripts\db-fix\backup"
TARGETS = [107, 108]

def psql(sql, multi=False):
    o = subprocess.run([PG + r"\psql.exe", "-h", "localhost", "-p", "5432", "-U", "postgres",
                        "-d", "food_traceability", "-t", "-A", "-F", "|", "-c", sql],
                       capture_output=True, text=True, encoding="utf-8", errors="replace", env=ENV, timeout=120)
    if o.returncode != 0:
        raise RuntimeError((o.stderr or "")[:300])
    return (o.stdout or "").strip()

os.makedirs(BK_DIR, exist_ok=True)
stamp = datetime.now().strftime("%Y%m%d%H%M%S")
bk_stmt = os.path.join(BK_DIR, "reconciliation_statements_%s.sql" % stamp)
bk_item = os.path.join(BK_DIR, "reconciliation_items_%s.sql" % stamp)
for tbl, out in (("reconciliation_statements", bk_stmt), ("reconciliation_items", bk_item)):
    d = subprocess.run([PG + r"\pg_dump.exe", "-h", "localhost", "-p", "5432", "-U", "postgres",
                        "-d", "food_traceability", "--data-only", "--table=%s" % tbl, "-f", out],
                       capture_output=True, text=True, encoding="utf-8", errors="replace", env=ENV, timeout=600)
    print("backup %-28s exit=%s size=%s  %s" % (tbl, d.returncode, os.path.getsize(out) if os.path.exists(out) else -1, out))
    if d.returncode != 0:
        print("  stderr:", (d.stderr or "")[:200]); sys.exit(2)

# 守卫：两张表必须都是 DRAFT，否则中止
states = psql("SELECT statement_id, status FROM reconciliation_statements WHERE statement_id IN (107,108) ORDER BY statement_id")
print("当前状态:", states.replace("\n", " | "))
bad = [l for l in states.splitlines() if not l.endswith("|DRAFT")]
if bad or len(states.splitlines()) != 2:
    print("ABORT：目标不全是 DRAFT 或数量不符 →", bad); sys.exit(2)

# 软删（先 items 后 statement；都带 status 守卫）
n_items = psql("WITH upd AS (UPDATE reconciliation_items SET deleted=1 WHERE statement_id IN (107,108)"
               " AND statement_id IN (SELECT statement_id FROM reconciliation_statements"
               "                       WHERE statement_id IN (107,108) AND status='DRAFT')"
               " RETURNING 1) SELECT COUNT(*) FROM upd")
n_stmt = psql("WITH upd AS (UPDATE reconciliation_statements SET deleted=1 WHERE statement_id IN (107,108)"
              " AND status='DRAFT' RETURNING 1) SELECT COUNT(*) FROM upd")
print("软删 items 行数 =", n_items)
print("软删 statements 行数 =", n_stmt)
print("DRAFT 剩余 =", psql("SELECT COUNT(*) FROM reconciliation_statements WHERE deleted=0 AND status='DRAFT'"))
print("OPEN 剩余 =", psql("SELECT COUNT(*) FROM recon_differences WHERE status IN ('OPEN','ASSIGNED','RESOLVING')"))
print("107/108 状态 =", psql("SELECT statement_id, status, deleted FROM reconciliation_statements WHERE statement_id IN (107,108) ORDER BY statement_id").replace("\n", " | "))
