#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
E2E 前置准备（可被其它 E2E 脚本 import 复用）

解决的设计缺陷：E2E 依赖菜品库存，却不自备库存 → 一旦库存被前面用例消耗，
后续用例会出现「菜品库存不足」这种与业务无关的失败（ui_e2e_payment 曾 7/7 全红）。

做三件事：
  1) 探测 8081 / 3002 / 3001 是否就绪（不就绪 → exit 2，与其它脚本风格一致）
  2) 库存重置：把参与 E2E 的菜品 stock 补到 >= 500（幂等：只在 stock < 100 时更新），
     并把实际执行的 SQL 打进日志
  3) 打印 [READY] 摘要（各端口 + 库存重置条数），exit 0

用法：
  python scripts/qa/e2e_setup.py
  python scripts/qa/e2e_setup.py --api http://localhost:8081/api --admin http://localhost:3002 --pos http://localhost:3001

被复用：
  from e2e_setup import ensure_ready
  if not ensure_ready(api=..., admin=..., pos=...):
      return 2
"""
from __future__ import annotations

import argparse
import os
import subprocess
import sys
import urllib.error
import urllib.request

DEFAULT_API = "http://localhost:8081/api"
DEFAULT_ADMIN = "http://localhost:3002"
DEFAULT_POS = "http://localhost:3001"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"
STOCK_TARGET = 500
STOCK_THRESHOLD = 100


def probe(url, timeout=5):
    try:
        with urllib.request.urlopen(urllib.request.Request(url, method="GET"), timeout=timeout) as r:
            return r.status < 500
    except urllib.error.HTTPError as e:
        return e.code < 500
    except Exception:
        return False


def psql(sql, psql_path=DEFAULT_PSQL, host="localhost", port=5432, user="postgres",
         password="123456", db="food_traceability", timeout=60):
    try:
        o = subprocess.run([psql_path, "-h", host, "-p", str(port), "-U", user, "-d", db,
                            "-t", "-A", "-F", "|", "-c", sql],
                           capture_output=True, text=True, timeout=timeout, encoding="utf-8",
                           errors="replace",
                           env=dict(os.environ, PGPASSWORD=password, PGCLIENTENCODING="UTF8"))
        return o.returncode, (o.stdout or "").strip(), (o.stderr or "").strip()
    except Exception as e:
        return -1, "", str(e)


def ensure_ready(api=DEFAULT_API, admin=DEFAULT_ADMIN, pos=DEFAULT_POS, reset_stock=True,
                 psql_path=DEFAULT_PSQL, pg_host="localhost", pg_port=5432, pg_user="postgres",
                 pg_password="123456", pg_db="food_traceability", verbose=True):
    """返回 True 表示前置就绪；否则已打印原因并返回 False（调用方按 exit 2 处理）。"""
    def log(msg):
        if verbose:
            print(msg)

    missing = []
    if not probe(api + "/v1/auth/login"):
        missing.append("后端 API %s（mvn spring-boot:run -Dspring-boot.run.profiles=pg）" % api)
    if not probe(admin + "/"):
        missing.append("管理端前端 %s（cd frontend && npx vite --port 3002）" % admin)
    if not probe(pos + "/"):
        missing.append("POS 前端 %s（cd frontend-pos && npx vite --port 3001）" % pos)
    if missing:
        log("[NOT_READY] 依赖服务不可用：")
        for m in missing:
            log("   - %s" % m)
        return False

    updated = 0
    if reset_stock:
        sql = ("UPDATE food SET stock = %d WHERE stock < %d AND deleted = 0"
               % (STOCK_TARGET, STOCK_THRESHOLD))
        log("[SQL] %s" % sql)
        rc, out, err = psql(sql, psql_path, pg_host, pg_port, pg_user, pg_password, pg_db)
        if rc != 0:
            log("[NOT_READY] 库存重置失败：rc=%s %s" % (rc, (err or out)[:200]))
            return False
        # psql 的 UPDATE 会输出 "UPDATE N"
        for tok in (out or "").split():
            if tok.isdigit():
                updated = int(tok)
        rc2, out2, _ = psql("SELECT COUNT(*) FROM food WHERE deleted=0 AND stock >= %d" % STOCK_TARGET,
                            psql_path, pg_host, pg_port, pg_user, pg_password, pg_db)
        enough = int(out2) if rc2 == 0 and out2.isdigit() else -1
        log("[READY] api=%s admin=%s pos=%s ｜ 库存重置 %d 条（stock<%d → %d）；当前 stock>=%d 的菜品 %d 条"
            % (api, admin, pos, updated, STOCK_THRESHOLD, STOCK_TARGET, STOCK_TARGET, enough))
    else:
        log("[READY] api=%s admin=%s pos=%s ｜ 跳过库存重置" % (api, admin, pos))
    return True


def main():
    p = argparse.ArgumentParser(description="E2E 前置准备（服务探测 + 库存重置）")
    p.add_argument("--api", default=DEFAULT_API)
    p.add_argument("--admin", default=DEFAULT_ADMIN)
    p.add_argument("--pos", default=DEFAULT_POS)
    p.add_argument("--no-stock", action="store_true", help="只探测服务，不重置库存")
    p.add_argument("--psql", default=DEFAULT_PSQL)
    p.add_argument("--pg-host", default="localhost")
    p.add_argument("--pg-port", type=int, default=5432)
    p.add_argument("--pg-user", default="postgres")
    p.add_argument("--pg-password", default="123456")
    p.add_argument("--pg-db", default="food_traceability")
    a = p.parse_args()
    print("=" * 78)
    print("E2E 前置准备")
    print("=" * 78)
    okk = ensure_ready(api=a.api, admin=a.admin, pos=a.pos, reset_stock=not a.no_stock,
                       psql_path=a.psql, pg_host=a.pg_host, pg_port=a.pg_port,
                       pg_user=a.pg_user, pg_password=a.pg_password, pg_db=a.pg_db)
    print("=" * 78)
    print("结果: %s" % ("READY" if okk else "NOT_READY"))
    return 0 if okk else 2


if __name__ == "__main__":
    sys.exit(main())
