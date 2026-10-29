#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
开工闸门（preflight gate）—— **不是**「系统健康证明」，只是「能不能开工」的最低门槛。

⚠ 头号原则：**分母不足时 PASS 不可信**。
   巡检里大量断言在空库上会输出 skipped_no_data（分母=0），此时「FAIL=0」不代表正确性，
   只代表没有数据可判。所以本脚本同时打印 PASS / skipped / FAIL 三个数字与 skipped 占比，
   skipped > 50% 时给 WARN（不阻断，但必须显眼）。

两个档位（Owner 2026-10-27 定）：
  · 默认（快，~30s）：`mvn -o -B test-compile` + `python scripts/qa/consistency_check.py`
    —— 注意 test-compile **不等于** test：它只证明测试代码能编译，不证明用例通过。
  · `--full`（慢，真验证）：再加 `mvn -o -B test`（34 用例全跑）。在 E2E 前 / 发版前使用。

退出码：
  0  全绿（compile ok + FAIL=0 + 分母充足）
  2  巡检 FAIL > 0（阻断）
  3  仅 WARN：分母不足（skipped/(PASS+skipped) > 50%）或 test-compile 失败

发版 / CI 硬规则（Owner 2026-10-27）：
  `--full` 必须 **EXIT=0** 才准发版；EXIT=3 表示「有 WARN（分母不足 / 编译失败 / mvn test 失败）」，
  **不得发版**；EXIT=2 直接阻断（巡检 FAIL>0）。CI 里把本脚本的退出码当门禁即可，不要再靠人眼看输出。

用法：
  python scripts/qa/preflight.py            # 开工前快检
  python scripts/qa/preflight.py --full     # E2E 前 / 发版前全量（含 mvn test）
  python scripts/qa/preflight.py --verbose  # 同时打印 mvn 与巡检完整输出
"""
from __future__ import annotations

import argparse
import time
import os
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BACKEND = ROOT / "backend"
CHECK = Path(__file__).resolve().parent / "consistency_check.py"
WARN_RATIO = 0.5
# 干净库 gate 用到的外部命令（Windows 下 psql 来自仓库自带 PostgreSQL）
PSQL = str(ROOT / "PostgreSQL" / "18" / "bin" / "psql.exe")
PG_PASSWORD = "123456"


def run(cmd, cwd, timeout=1800, env=None):
    p = subprocess.run(cmd, cwd=str(cwd), capture_output=True, text=True, timeout=timeout,
                       encoding="utf-8", errors="replace", shell=False, env=env)
    return p.returncode, (p.stdout or "") + (p.stderr or "")


def find_mvn():
    """Windows 下 subprocess 直接调 'mvn' 找不到（它是 mvn.cmd，需 shutil.which 解析）。"""
    import shutil
    for name in ("mvn.cmd", "mvn.bat", "mvn"):
        p = shutil.which(name)
        if p:
            return p
    raise RuntimeError("找不到 mvn（请确认 Maven 在 PATH 上，如 H:\\fuwu\\apache-maven-3.9.11\\bin）")


def find_java_home():
    """JAVA_HOME 是本机易踩的坑（当前环境变量指向不存在的路径）。
    优先用仓库自带 JDK21，其次校验环境变量，最后才放弃。"""
    import os
    candidates = [str(ROOT / "JDK21"), os.environ.get("JAVA_HOME", "")]
    for c in candidates:
        if c and (Path(c) / "bin" / "java.exe").exists():
            return c
    return None


def _safe_print(text):
    """控制台编码不安全时降级（GBK 控制台下打印部署缺口明细会 UnicodeEncodeError）。"""
    try:
        print(text)
    except UnicodeEncodeError:
        enc = sys.stdout.encoding or 'utf-8'
        print(text.encode(enc, errors='replace').decode(enc, errors='replace'))


def run_gate_script(script, verbose):
    """跑 scripts/qa/<script>，返回 exit code（脚本不存在则跳过返回 0）。"""
    path = ROOT / "scripts" / "qa" / script
    if not path.exists():
        return 0
    rc, out = run([sys.executable, str(path)], ROOT, timeout=180)
    if verbose or rc != 0:
        print(out.strip())
    return rc


def run_gate_script(script, verbose, baseline_ratchet=False):
    """跑 scripts/qa/<script>；脚本不存在则跳过（返回 0）。

    baseline_ratchet=True 时（entity_column_audit）：把实测不一致数与
    scripts/qa/entity-column-baseline.txt 比较，<= 基线 → 0(PASS)，> 基线 → 2(BLOCK)。
    """
    path = ROOT / "scripts" / "qa" / script
    if not path.exists():
        return 0
    rc, out = run([sys.executable, str(path)], ROOT, timeout=180)
    if baseline_ratchet:
        import re as _re
        m = _re.search(r"合计 \*\*(\d+)\*\* 处不一致", out)
        if m:
            actual = int(m.group(1))
            base_file = ROOT / "scripts" / "qa" / "entity-column-baseline.txt"
            baseline = 137
            if base_file.exists():
                for line in base_file.read_text(encoding="utf-8").splitlines():
                    line = line.strip()
                    if line and not line.startswith("#"):
                        baseline = int(line)
                        break
            if actual <= baseline:
                print("[GATE] entity_column：实测 %d ≤ 基线 %d → PASS（棘轮，只许降）" % (actual, baseline))
                return 0
            print("[GATE] entity_column：实测 %d > 基线 %d → BLOCK（新增不一致必须修）" % (actual, baseline))
            return 2
    if verbose or rc != 0:
        print(out.strip())
    return rc


def mvn(goal, verbose):
    cmd = [find_mvn(), "-o", "-B"] + goal
    import os
    env = dict(os.environ)
    jh = find_java_home()
    if jh:
        env["JAVA_HOME"] = jh
        env["PATH"] = str(Path(jh) / "bin") + os.pathsep + env.get("PATH", "")
    else:
        print("[WARN] 未找到可用 JDK（JAVA_HOME=%r 不存在，仓库 JDK21 也不存在）→ mvn 会失败"
              % os.environ.get("JAVA_HOME"))
    print("[GATE] %s  (cwd=%s)" % (" ".join(cmd), BACKEND))
    rc, out = run(cmd, BACKEND, env=env)
    tail = [l for l in out.splitlines() if ("BUILD SUCCESS" in l or "BUILD FAILURE" in l or "ERROR" in l)]
    if verbose:
        print(out[-4000:])
    else:
        for l in tail[-6:]:
            print("       " + l.strip())
    print("[GATE] %s -> rc=%s" % (" ".join(goal), rc))
    return rc


def parse_inspection(text):
    """从巡检输出里取 PASS / skipped / FAIL 三个数字；取不到就返回 None 并说明原因。"""
    m = re.search(r"汇总:\s*(\d+)\s*PASS\s*/\s*(\d+)\s*FAIL", text)
    if not m:
        return None
    passed, failed = int(m.group(1)), int(m.group(2))
    sk = re.search(r"其中\s*(\d+)\s*条\s*skipped_no_data", text)
    skipped = int(sk.group(1)) if sk else 0
    total = re.search(r"判定项\s*(\d+)\s*条", text)
    judged = int(total.group(1)) if total else (passed + failed)
    raw = next((l.strip() for l in text.splitlines() if l.startswith("汇总:")), "")
    return {"pass": passed, "fail": failed, "skipped": skipped, "judged": judged, "raw": raw}


RUNTIME_EXC_BASELINE = ROOT / "scripts" / "qa" / "runtime-exception-baseline.txt"
JAVA_SRC = ROOT / "backend" / "src" / "main" / "java"
RUNTIME_EXC_PATTERN = "throw new RuntimeException("


def count_runtime_exceptions():
    """统计 backend/src/main/java 下 'throw new RuntimeException(' 的处数（静态文本计数）。"""
    total = 0
    for path in sorted(JAVA_SRC.rglob("*.java")):
        try:
            total += path.read_text(encoding="utf-8", errors="replace").count(RUNTIME_EXC_PATTERN)
        except OSError:
            continue
    return total


def read_runtime_exception_baseline():
    """读基线文件最后一个非注释数字行；缺失/非法返回 None。"""
    if not RUNTIME_EXC_BASELINE.exists():
        return None
    # utf-8-sig：兼容 Windows 编辑器写入的 BOM（否则数字行首字符带 \ufeff，int() 失败 → 静默不阻断）
    for line in reversed(RUNTIME_EXC_BASELINE.read_text(encoding="utf-8-sig", errors="replace").splitlines()):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        try:
            return int(line)
        except ValueError:
            return None
    return None


def check_runtime_exception_ratchet():
    """棘轮（bug-⑨）：裸 RuntimeException 只降不升。返回 0=通过，2=阻断。"""
    current = count_runtime_exceptions()
    base = read_runtime_exception_baseline()
    if base is None:
        print("[WARN] 棘轮基线缺失或不可解析：%s（无法判定，按不阻断处理）" % RUNTIME_EXC_BASELINE)
        return 0
    delta = current - base
    if delta > 0:
        print("[BLOCK] 棘轮失败：throw new RuntimeException( 由基线 %d 增加到 %d（+%d）" % (base, current, delta))
        print("        业务语义请改 BusinessException(ErrorCode.X, msg)；详见 scripts/qa/runtime-exception-baseline.txt")
        return 2
    print("[GATE] 棘轮通过：throw new RuntimeException( = %d（基线 %d，%s）"
          % (current, base, "减少 %d" % (-delta) if delta < 0 else "持平"))
    return 0


MIGRATION_DIR = ROOT / "backend" / "src" / "main" / "resources" / "db" / "migration"
FLYWAY_RUNNER_SRC = ROOT / "scripts" / "qa" / "db" / "FlywayMigrateRunner.java"
GATE_TMP = ROOT / ".preflight-tmp"
TEST_DB = "food_traceability_test"


def expected_latest_version():
    """从迁移文件名解析最大版本号（不写死数字）。

    Flyway 的版本排序按「数字段逐段比较」，所以用 tuple(int) 比较；返回原始字符串与段元组。
    """
    # 关键：Flyway 比版本时把短版本**补 0 到同长度**再逐段比较
    # （例：2026022701 与 20261027.001 → (2026022701, 0) vs (20261027, 1) → 前者更大），
    # 直接按不等长 tuple 比较会得出相反的「最新版本」。
    versions = []
    for f in MIGRATION_DIR.glob("V*.sql"):
        ver = f.name[1:].split("__", 1)[0]
        try:
            # Flyway 的分隔符同时支持 '.' 与 '_'（V20261027_001 即 20261027.001），
            # 只按 '.' 切会把 "_001" 当成整数的一部分 → 版本排序整体错位。
            versions.append((ver, [int(p) for p in re.split(r"[._]", ver) if p != ""]))
        except ValueError:
            continue
    if not versions:
        return None
    width = max(len(v[1]) for v in versions)
    padded = [(ver, segs + [0] * (width - len(segs))) for ver, segs in versions]
    return max(padded, key=lambda x: x[1])[0]


def _psql(db, sql):
    return run([PSQL, "-h", "localhost", "-p", "5432", "-U", "postgres", "-d", db, "-A", "-F", "|", "-t", "-c", sql],
               ROOT, timeout=300, env=dict(os.environ, PGPASSWORD=PG_PASSWORD, PGCLIENTENCODING="UTF8"))


def _mvn_path():
    return find_mvn()


def _java_paths():
    jh = find_java_home()
    if not jh:
        return None, None, None
    b = Path(jh) / "bin"
    return str(b / "java.exe"), str(b / "javac.exe"), Path(jh)


def check_clean_db_migration():
    """第三条 gate（--full 才跑）：干净库必须一次迁移成功（bug-⑦/⑧ 的回归闸门）。

    头注释（写在这里防遗忘）：
      **迁移链必须能在干净库一次建成功** —— 本 gate 就是为了防止第 4、5 个漂移断点。
      漂移的典型来源：迁移引用「历史库演进出来的列/序列」，活库永远不重跑所以看不见。
    """
    expected = expected_latest_version()
    if not expected:
        print("[WARN] 未从 %s 解析到任何迁移版本，跳过干净库 gate" % MIGRATION_DIR)
        return 0
    print("[GATE] 干净库迁移：期望最新版本 = %s（从目录解析，不写死）" % expected)

    rc, _ = _psql("postgres", "DROP DATABASE IF EXISTS %s" % TEST_DB)
    if rc != 0:
        print("[BLOCK] 干净库 gate：DROP DATABASE %s 失败（可能仍有连接）" % TEST_DB)
        return 2
    rc, out = _psql("postgres", "CREATE DATABASE %s WITH ENCODING 'UTF8' TEMPLATE template0" % TEST_DB)
    if rc != 0:
        print("[BLOCK] 干净库 gate：CREATE DATABASE 失败：%s" % out.strip()[:200])
        return 2

    GATE_TMP.mkdir(parents=True, exist_ok=True)
    cp_file = GATE_TMP / "cp.txt"
    if not cp_file.exists():
        jh = find_java_home()
        env_mvn = dict(os.environ, JAVA_HOME=str(jh)) if jh else None
        rc, out = run([_mvn_path(), "-o", "-B", "-q", "dependency:build-classpath",
                       "-Dmdep.outputFile=%s" % cp_file], ROOT / "backend", timeout=600, env=env_mvn)
        if rc != 0 or not cp_file.exists():
            print("[WARN] 干净库 gate：生成依赖 classpath 失败（跳过 gate）：%s" % out.strip()[:200])
            return 0
    classpath = cp_file.read_text(encoding="utf-8", errors="replace").strip() + str(os.pathsep) + str(GATE_TMP)

    java_exe, javac_exe, java_home = _java_paths()
    if not java_exe:
        print("[WARN] 干净库 gate：找不到 JDK（跳过 gate）")
        return 0
    rc, out = run([javac_exe, "-proc:none", "-encoding", "UTF-8", "-cp", classpath, "-d", str(GATE_TMP),
                   str(FLYWAY_RUNNER_SRC)], ROOT, timeout=600,
                  env=dict(os.environ, JAVA_HOME=str(java_home)))
    if rc != 0:
        print("[WARN] 干净库 gate：编译 FlywayMigrateRunner 失败（跳过 gate）：%s" % out.strip()[:200])
        return 0

    rc, out = run([java_exe, "-cp", classpath, "FlywayMigrateRunner", TEST_DB, str(MIGRATION_DIR)], ROOT, timeout=1800,
                  env=dict(os.environ, JAVA_HOME=str(java_home)))
    line = next((l for l in out.splitlines() if l.startswith("FLYWAY_RESULT")), "")
    if "ok=true" not in line:
        print("[BLOCK] 干净库 gate：迁移失败 → %s" % (line or out.strip().splitlines()[-1] if out.strip() else "(无输出)"))
        return 2

    rc, hist = _psql(TEST_DB, "SELECT count(*) FILTER (WHERE NOT success), max(installed_rank), "
                              "count(*) FILTER (WHERE success) FROM flyway_schema_history")
    rc2, ver = _psql(TEST_DB, "SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1")
    failed, max_rank, ok_cnt = [x.strip() for x in (hist.strip().split("|") + ["", "", ""])[:3]]
    latest = ver.strip()
    print("[GATE] 干净库迁移结果：%s ｜ failed=%s rank=%s success=%s latest=%s"
          % (line, failed, max_rank, ok_cnt, latest))
    if failed != "0":
        print("[BLOCK] 干净库 gate：flyway_schema_history 存在失败记录 failed=%s" % failed)
        return 2
    if latest != expected:
        print("[BLOCK] 干净库 gate：最新版本 %s ≠ 目录期望 %s（迁移未跑到最新）" % (latest, expected))
        return 2
    print("[GATE] 干净库迁移通过（%s，%s 条成功）" % (latest, ok_cnt))
    return 0


RESULT_ERROR_BASELINE = ROOT / "scripts" / "qa" / "result-error-string-baseline.txt"
RESULT_ERROR_PATTERN = 'Result.error("'


def check_result_error_ratchet():
    """棘轮（bug-⑫ 形态②）：Result.error("字面量") 只降不升。返回 0=通过，2=阻断。"""
    current = 0
    for path in sorted(JAVA_SRC.rglob("*.java")):
        try:
            current += path.read_text(encoding="utf-8", errors="replace").count(RESULT_ERROR_PATTERN)
        except OSError:
            continue
    base = None
    if RESULT_ERROR_BASELINE.exists():
        for line in reversed(RESULT_ERROR_BASELINE.read_text(encoding="utf-8-sig", errors="replace").splitlines()):
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            try:
                base = int(line)
            except ValueError:
                base = None
            break
    if base is None:
        print("[WARN] 形态②棘轮基线缺失或不可解析：%s（按不阻断处理）" % RESULT_ERROR_BASELINE)
        return 0
    delta = current - base
    if delta > 0:
        print("[BLOCK] 形态②棘轮失败：Result.error(\"字面量) 由基线 %d 增加到 %d（+%d）" % (base, current, delta))
        print("        业务语义请用 Result.error(ErrorCode.X.getCode(), msg)")
        return 2
    print("[GATE] 形态②棘轮通过：Result.error(\"字面量) = %d（基线 %d，%s）"
          % (current, base, "减少 %d" % (-delta) if delta < 0 else "持平"))
    return 0


MVN_BASELINE = ROOT / 'scripts' / 'qa' / 'mvn-test-baseline.txt'
FAIL_LINE_RE = re.compile(r"^\[ERROR\]\s{3}([\w.$]+)\.(\w+)(?::\d+)?\s*(.*)")
def parse_mvn_test_failures(log_path):
    items = {}
    path = Path(log_path)
    if not path.exists():
        return items
    for raw in path.read_text(encoding='utf-8', errors='replace').splitlines():
        line = raw.strip()
        m = FAIL_LINE_RE.match(line)
        if not m or 'Tests run:' in line:
            continue
        key = '%s#%s' % (m.group(1), m.group(2))
        rest = m.group(3) or ''
        is_error = ('NullPointer' in rest) or ('IllegalState' in rest) or rest.startswith('?') or ('Runtime' in rest)
        items[key] = 'Error' if is_error else 'Failure'
    return items
def _short(key):
    """类名去包名：# 前的最后一段（含内部类 $），保证日志短名与基线 FQCN 可比。"""
    if '#' not in key:
        return key
    cls, method = key.split('#', 1)
    return cls.split('.')[-1] + '#' + method


def load_mvn_baseline():
    allowed = {}
    if not MVN_BASELINE.exists():
        return allowed
    in_stable = False
    for line in MVN_BASELINE.read_text(encoding='utf-8-sig', errors='replace').splitlines():
        if line.strip().startswith('## '):
            in_stable = ('稳定红点' in line)
            continue
        if not in_stable:
            continue
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        parts = [x.strip() for x in line.split('|')]
        if len(parts) < 2:
            continue
        allowed[_short(parts[0])] = parts[1]
    return allowed
def check_mvn_test_baseline(log_path):
    current = parse_mvn_test_failures(log_path)
    baseline = load_mvn_baseline()
    if not baseline:
        print('[WARN] mvn-test 基线缺失或为空：%s' % MVN_BASELINE)
        return 3
    if not current:
        print('[BLOCK] mvn test 全绿 —— 请把基线降为 0 条并恢复硬闸门（EXIT=2）')
        return 2
    new_items = {k: v for k, v in current.items() if _short(k) not in baseline}
    if new_items:
        print('[BLOCK] mvn test 出现基线外新增失败（新回归）→ 阻断')
        for k, v in sorted(new_items.items()):
            print('        + %s (%s)' % (k, v))
        return 2
    print('[GATE] mvn test 红点全在基线内：%d 条（警告，正在还欠账）' % len(current))
    return 3
def check_deploy_gap(db='food_traceability_test'):
    """部署缺口审计：实体表在目标库不存在且有活 Mapper 引用 → 阻断（发版前必须清零）。"""
    script = ROOT / 'scripts' / 'qa' / 'deploy_gap_audit.py'
    r = subprocess.run([sys.executable, str(script), '--db', db], capture_output=True, text=True,
                       encoding='utf-8', errors='replace', timeout=600)
    tail = [x for x in (r.stdout or '').splitlines() if x.strip()][-1:]
    for line in tail:
        print('  ' + line)
    # 棘轮语义：0=通过、3=警告（正在还账，不阻断）、2=阻断（基线外新增 / 已清零需删基线）
    return r.returncode if r.returncode in (0, 3) else 2


def check_permission_code():
    """A46 固定 gate：代码声明的权限码 ↔ permissions 目录，缺失不为 0 → EXIT=2。"""
    script = ROOT / 'scripts' / 'qa' / 'permission_code_audit.py'
    r = subprocess.run([sys.executable, str(script)], capture_output=True, text=True,
                       encoding='utf-8', errors='replace', timeout=600)
    lines = [x for x in (r.stdout or '').splitlines() if x.strip()]
    for line in lines[:2]:
        print('  ' + line)
    return 0 if r.returncode == 0 else 2


def main():
    ap = argparse.ArgumentParser(description="开工闸门：test-compile + 巡检（--full 才跑 mvn test）")
    ap.add_argument("--full", action="store_true", help="E2E 前/发版前：额外跑 mvn test")
    ap.add_argument("--verbose", action="store_true", help="打印 mvn 与巡检完整输出")
    ap.add_argument("--skip-compile", action="store_true", help="跳过 mvn（仅看巡检）")
    a = ap.parse_args()
    print("=" * 86)
    print("PREFLIGHT GATE   mode=%s" % ("FULL(mvn test)" if a.full else "FAST(test-compile)"))
    print("  提醒：本脚本是开工闸门，不是系统健康证明；分母不足时 PASS 不可信。")
    print("=" * 86)
    ratchet_rc = check_runtime_exception_ratchet()
    result_error_rc = check_result_error_ratchet()
    warn = False
    compile_rc = 0
    if a.full and not a.skip_compile:
        clean_rc = mvn(["clean"], a.verbose)
        if clean_rc == 0:
            print("[GATE] clean 完成，避免 target/classes 残留")
        else:
            print("[WARN] clean 失败（rc=%s）—— 后续 test 可能跑到旧 class" % clean_rc)
            warn = True
    if not a.skip_compile:
        compile_rc = mvn(["test-compile"], a.verbose)
        if compile_rc != 0:
            print("[WARN] test-compile 失败（rc=%s）→ 注意：test-compile≠test，但连编译都不过就不该开工" % compile_rc)
            warn = True
    # ---- 轻量 gate（任何模式都跑，均 <5s）----
    mig_gate_rc = run_gate_script("migration_version_gate.py", a.verbose)
    if mig_gate_rc != 0:
        print("[GATE] 迁移版本 gate 失败（EXIT=2）：重复版本号会让应用启动失败")
        warn = True
    # ---- 权限常量 gate（Lead 2026-10-29）：代码集合常量里的码必须在 permissions 目录里 ----
    # 棘轮：⊆基线 → rc=3 WARN；出现基线外新缺失 → rc=2 BLOCK
    perm_const_rc = run_gate_script("permission_constant_gate.py", a.verbose)
    if perm_const_rc == 2:
        print("[GATE] 权限常量 gate BLOCK：出现基线外的新缺失码（代码有、目录无 → 隐形授权）")
        warn = True
    elif perm_const_rc == 3:
        print("[GATE] 权限常量 gate WARN：已知缺失在基线内（棘轮，不阻断）")
    clean_db_rc = 0
    if a.full:
        print("-" * 86)
        clean_db_rc = check_clean_db_migration()
    test_rc = None
    if a.full:
        test_rc = mvn(["test"], a.verbose)
        if test_rc != 0:
            print("[WARN] mvn test 失败（rc=%s）—— 这是真验证档，失败必须修" % test_rc)
            warn = True
    print("-" * 86)
    print("[GATE] 巡检：%s" % CHECK)
    rc, out = run([sys.executable, str(CHECK)], ROOT, timeout=1800)
    if a.verbose:
        print(out)
    stats = parse_inspection(out)
    if stats is None:
        print("[WARN] 巡检输出无法解析（未找到「汇总: N PASS / M FAIL」），原文尾 20 行：")
        for l in out.splitlines()[-20:]:
            print("       " + l)
        print("[RESULT] INCONCLUSIVE（无法判定）")
        return 3
    passed, failed, skipped = stats["pass"], stats["fail"], stats["skipped"]
    judged = passed + skipped if (passed + skipped) else stats["judged"]
    ratio = (skipped / judged) if judged else 0.0
    print("[GATE] 巡检统计：PASS=%d  skipped_no_data=%d  FAIL=%d  判定项=%d"
          % (passed, skipped, failed, stats["judged"]))
    print("[GATE] skipped 占比：%d/%d = %.0f%%（阈值 %.0f%%）"
          % (skipped, judged, ratio * 100, WARN_RATIO * 100))
    print("[GATE] 巡检原始汇总：%s" % stats["raw"])
    if failed > 0:
        print("[RESULT] BLOCKED：巡检 FAIL=%d > 0 → exit 2" % failed)
        return 2
    if ratchet_rc != 0:
        print("[RESULT] BLOCKED：裸 RuntimeException 棘轮上升 → exit 2")
        return 2
    if result_error_rc != 0:
        print("[RESULT] BLOCKED：Result.error(字面量) 棘轮上升 → exit 2")
        return 2
    if clean_db_rc != 0:
        print("[RESULT] BLOCKED：干净库迁移 gate 未通过 → exit 2")
        return 2
    if ratio > WARN_RATIO:
        print("[WARN] 分母不足，PASS 不可信：skipped %d/%d = %.0f%% > %.0f%%"
              % (skipped, judged, ratio * 100, WARN_RATIO * 100))
        warn = True
    # ---- 4 个 gate（Lead 2026-10-28 要求接入 --full；fast 不跑，避免拖慢）----
    if a.full:
        gate_specs = [
            ("schema_guard.py", "静态 schema 守卫", 1),
            ("fen_shadow_check.py", "*_fen 影子列应为全 NULL", 2),
            ("entity_column_audit.py", "实体列<->表列（不增加棘轮）", 3),
            ("coverage_check.py", "覆盖率门禁", 2),
        ]
        for script, label, cost in gate_specs:
            t0 = time.time()
            rc = run_gate_script(script, a.verbose, baseline_ratchet=(script == "entity_column_audit.py"))
            dt = time.time() - t0
            status = "PASS" if rc == 0 else ("WARN" if rc == 3 else "BLOCK")
            print("[GATE] %-34s %s  (%.1fs, ~%ds)" % (label, status, dt, cost))
            if rc not in (0, 3):
                warn = True
    perm_rc = check_permission_code()
    deploy_gap_rc = check_deploy_gap() if a.full else 0
    # 新表体检：只针对 2026-10-28 之后新建的表（既有表老账见 docs/audit/tech-debt-schema.md）
    health_rc = check_new_tables_health() if a.full else 0
    sec_rc = check_security_regression() if a.full else 0
    if sec_rc != 0:
        print('[RESULT] BLOCKED：安全回归未通过（EXIT=%s）→ exit 2' % sec_rc)
        return 2
    if health_rc != 0:
        warn = True
    summary = "PASS=%d skipped=%d (%d%%) FAIL=%d → %s" % (
        passed, skipped, round(ratio * 100), failed, "WARN" if warn else "OK")
    print("[RESULT] " + summary)
    if perm_rc != 0:
        print("[RESULT] BLOCKED：A46 权限码 ↔ 目录不一致（代码要求了目录里没有的权限）→ exit 2")
        return 2
    if deploy_gap_rc == 3:
        print("[GATE] 部署缺口在基线内（棘轮 WARN，不阻断）")
        warn = True
    if deploy_gap_rc not in (0, 3):
        print("[RESULT] BLOCKED：部署缺口未清零（干净库缺表且有活引用）→ exit 2")
        return 2
    return 3 if warn else 0

def check_security_regression():
    """安全回归 S1~S6（发布门禁）：EXIT=0 才允许发版。

    覆盖 R1-R5 攻击用例 + 每组正常对照（防恒真）：
      S1 价格篡改 / S2 售罄强攻 / S3 伪造开班 / S4 跨人交班 / S5 退款越权 / S6 响应价回显
    脚本内置 [SELF-CHECK]（表存在 + 关联键样例）与按 tag 清理，跑完打印残留数（应为 0）。
    只在 --full 跑（依赖运行中的后端 + 真实下单，约 20-40 秒）。
    """
    script = Path(__file__).resolve().parent / 'security_regression.py'
    if not script.exists():
        print('[WARN] 安全回归：找不到 %s' % script)
        return 1
    rc, out = run([sys.executable, str(script)], ROOT, timeout=900)
    tail = [l for l in out.splitlines() if l.startswith('总计') or l.startswith('EXIT=')]
    if rc == 0:
        print('[GATE] 安全回归 S1~S6：全 PASS（%s）' % ('；'.join(tail)[:120]))
        return 0
    if rc == 2:
        print('[WARN] 安全回归：未就绪（后端未起/自检未过）→ 本次不算验证通过')
        return 2
    print('[GATE] 安全回归失败（rc=%s）：' % rc)
    for l in out.splitlines():
        if l.strip().startswith('S') and 'FAIL' in l:
            print('       ' + l.strip()[:170])
    return rc


def check_new_tables_health():
    """新表体检（只跑「2026-10-28 之后新建」的表清单）。

    为什么只跑新表：既有表存在大量老账（_id 列无索引、金额用 numeric 元），
    一上来就 EXIT=2 会让 gate 变成噪音、最终被人关掉 —— gate 一旦被关就再也拦不住新问题。
    老账单独登记在 docs/audit/tech-debt-schema.md。

    表清单来源（按优先级）：
      1) scripts/qa/new-tables.txt（每行一个表名，团队维护）
      2) 环境变量 PREFLIGHT_NEW_TABLES（逗号分隔）
    清单为空 → 跳过并打印说明（不阻断）。
    """
    list_file = Path(__file__).resolve().parent / "new-tables.txt"
    tables = []
    if list_file.exists():
        tables = [ln.strip() for ln in list_file.read_text(encoding="utf-8-sig").splitlines()
                  if ln.strip() and not ln.strip().startswith("#")]
    if not tables:
        env = os.environ.get("PREFLIGHT_NEW_TABLES", "").strip()
        tables = [t.strip() for t in env.split(",") if t.strip()]
    if not tables:
        print("[GATE] 新表体检：跳过（无 scripts/qa/new-tables.txt / PREFLIGHT_NEW_TABLES 清单）")
        return 0
    script = Path(__file__).resolve().parent / "table_health_check.py"
    if not script.exists():
        print("[WARN] 新表体检：找不到 %s" % script)
        return 1
    rc, out = run([sys.executable, str(script), "--tables", ",".join(tables)], ROOT, timeout=600)
    if rc == 0:
        last = [l for l in out.splitlines() if l.strip()][-1:]
        print("[GATE] 新表体检：%d 张全部通过（%s）" % (len(tables), (last[0] if last else "").strip()))
        return 0
    print("[WARN] 新表体检不通过（rc=%s）：" % rc)
    for l in out.splitlines():
        if "✗" in l or "结果：" in l:
            print("       " + l.strip()[:180])
    return rc

if __name__ == "__main__":
    sys.exit(main())

