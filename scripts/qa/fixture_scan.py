import subprocess, os, sys
ENC={"encoding":"utf-8","errors":"replace"}
PSQL=r"P:\\my-new-project\\PostgreSQL\\18\\bin\\psql.exe"
DB=os.environ.get("QA_PGDB","food_traceability")
PATTERNS=["T-%","AUDIT-%","%PROBE%"]
TABLES=[("pos_shifts","terminal_id"),("pos_shifts","employee_name"),("pos_held_orders","terminal_id"),
        ("pos_held_orders","order_no"),("orders","order_number"),("orders","remark"),
        ("fund_flows","remark"),("exception_ops","order_id"),("bank_accounts","account_name")]
def q(sql):
    env=dict(os.environ,PGPASSWORD="123456",PGCLIENTENCODING="UTF8")
    r=subprocess.run([PSQL,"-h","localhost","-p","5432","-U","postgres","-d",DB,"-A","-F","|","-t","-c",sql],capture_output=True,text=True,env=env,**ENC)
    return (r.stdout or "").strip(), (r.stderr or "").strip()
print("[INFO] db=%s 模式=%s" % (DB, ", ".join(PATTERNS)))
total=0
print("")
print("表.列 | 命中数 | 样例（最多 3）")
for tbl,col in TABLES:
    cond=" OR ".join(["%s LIKE '%s'" % (col,p) for p in PATTERNS])
    sql="SELECT count(*) FROM %s WHERE %s" % (tbl,cond)
    cnt,err=q(sql)
    if err:
        print("%s.%s | SKIP(%s)" % (tbl,col,err[:40])); continue
    try: n=int(cnt)
    except: n=0
    total+=n
    sample=""
    if n>0:
        s,_=q("SELECT string_agg(DISTINCT v, ', ') FROM (SELECT %s::text AS v FROM %s WHERE %s LIMIT 3) t" % (col,tbl,cond))
        sample=s
    print("%s.%s | %d | %s" % (tbl,col,n,sample))
print("")
print("[STAT] fixture_hits=%d" % total)
print("[RESULT] EXIT=%d" % (2 if total>0 else 0))
sys.exit(2 if total>0 else 0)