import json, subprocess, time, statistics, sys
ENC={"encoding":"utf-8","errors":"replace"}
BASE=sys.argv[1] if len(sys.argv)>1 else "http://127.0.0.1:8082/api"
N=int(sys.argv[2]) if len(sys.argv)>2 else 30
def timed(user, pwd):
    t0=time.perf_counter()
    r=subprocess.run(["curl.exe","-s","-o","NUL","-X","POST",BASE+"/v1/auth/login",
        "-H","Content-Type: application/json","-d",json.dumps({"username":user,"password":pwd})],
        capture_output=True,text=True,**ENC)
    return (time.perf_counter()-t0)*1000.0
def st(name, ss):
    ss=sorted(ss); n=len(ss)
    m=statistics.mean(ss); p50=ss[n//2]; p95=ss[min(n-1,int(n*0.95))]
    print("%-22s n=%-3d mean=%6.1fms p50=%6.1fms p95=%6.1fms" % (name,n,m,p50,p95))
    return m
groups=[("不存在用户","tt_nobody_%d"%int(time.time()),"x"),
        ("存在+密码错","tt_active","wrong-pw"),
        ("存在+已禁用","tt_disabled","wrong-pw")]
means={}
for name,u,p in groups:
    means[name]=st(name,[timed(u,p) for _ in range(N)])
base=means["存在+密码错"]
print()
for k,v in means.items():
    if k=="存在+密码错": continue
    print("差值 %-14s vs 密码错 = %+.1f ms (%.0f%%)" % (k, v-base, (v-base)/base*100))