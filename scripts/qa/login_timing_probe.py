import json, subprocess, time, statistics, sys
BASE = "http://127.0.0.1:8082/api"
ENC = {"encoding": "utf-8", "errors": "replace"}

def login_raw(user, pwd):
    t0 = time.perf_counter()
    subprocess.run(["curl.exe","-s","-o","NUL","-X","POST",BASE+"/v1/pos/auth/login",
                    "-H","Content-Type: application/json",
                    "-d", json.dumps({"employeeId": user, "password": pwd})],
                   capture_output=True, text=True, **ENC)
    return (time.perf_counter() - t0) * 1000.0

def stats(name, samples):
    samples = sorted(samples)
    n = len(samples)
    mean = statistics.mean(samples)
    p50 = samples[n // 2]
    p95 = samples[min(n - 1, int(n * 0.95))]
    print("%-22s n=%-3d mean=%6.1fms  p50=%6.1fms  p95=%6.1fms" % (name, n, mean, p50, p95))
    return mean

N = int(sys.argv[1]) if len(sys.argv) > 1 else 50
groups = [
    ("不存在用户", "no-such-user-timing", "whatever"),
    ("存在+密码错", "vcashier01", "definitely-wrong-pw"),
    ("存在+正确密码", "vcashier01", "123456"),
]
means = {}
for name, u, p in groups:
    ss = [login_raw(u, p) for _ in range(N)]
    means[name] = stats(name, ss)
print("")
base = means["存在+密码错"]
for k, v in means.items():
    if k == "存在+密码错":
        continue
    print("差异：%-16s vs 存在+密码错 = %+.1f ms（%.0f%%）" % (k, v - base, (v - base) / base * 100))