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

取样竞态规则（2026-10-25 两次事故的机制沉淀）
=====================================================================
事故 1（P3-2 AC-50b）：页面 channel-fee-total 元素已可见，但 data-fee-fen 仍是渲染前的 0，
                     脚本「元素出现即读值」→ 断言红；页面其实是对的。
事故 2（P3-4 AC-46a）：summary-diff-count 元素已出现，但 /recon-runs/{id} 还没返回，
                     摘要先渲染占位 0 → 读到 0；同一次运行里等过 data-amount-state=ready 的
                     AC-46b 反而读到正确值 104900。
结论（硬规则）：禁止 element.inner_text() / get_attribute() 直接读带 data- 的钩子；
             必须先等就绪标记。统一走本模块的：
               wait_for_data_attr(page, selector, attr, ready_value, timeout=30000)  # ready_value 必填
               read_data_attr(page, selector, attr)   # 仅在「已确认就绪」的场景使用
             读不到就抛异常（异常信息带 selector + attr），不返回 0/空字符串糊过去。
=====================================================================
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request

DEFAULT_API = "http://localhost:8081/api"
DEFAULT_ADMIN = "http://localhost:3002"
DEFAULT_POS = "http://localhost:3001"
DEFAULT_PSQL = r"P:\my-new-project\PostgreSQL\18\bin\psql.exe"

# 哨兵：表示「属性非空即就绪」。仅当调用方确认该元素在数据未到时不会渲染 0/占位值时才可用。
READY_NONEMPTY = "__nonempty__"
STOCK_TARGET = 500
STOCK_THRESHOLD = 100


def wait_for_data_attr(page, selector, attr, ready_value, timeout=30000):
    """等 [selector] 的 [attr] 达到 [ready_value] 后返回其值。**ready_value 必填（无默认值）**。

    【为什么必填（Owner 拍板 2026-10-25）】旧版 `ready_value=None` 的语义是「属性非空即就绪」，
    而 '0' 也算非空 → 若某个 data-* 钩子**没有就绪标记**（如 data-amount-state），页面的占位 0
    会被当成真实值，直接复现取样竞态。让调用方**必须写出它等的到底是什么状态**，
    新增钩子时忘记加就绪标记会立刻在调用处暴露，而不是静默读到 0。
    · 有就绪标记的场景：传标记值，例如 `ready_value='ready'`（配 `data-amount-state`/`data-fee-state`/`data-state`）；
    · 数值类钩子且**确认**页面在数据未到时不渲染该元素（或渲染的是空串而非 0）：用 `READY_NONEMPTY`；
    · 超时抛 RuntimeError，异常信息带 selector + attr + ready_value。

    【READY_NONEMPTY 的适用判据（P3 归档固化，防误用）】
    **仅适用于「元素后渲染」的钩子** —— 即整段被 `v-if` 包住（如 `StatementDetail.vue` 的
    `<template v-if="statement">`，数据未到时**元素根本不渲染**），元素一旦出现其属性就是真值，
    此时 `'0'` 是合法真实值（如 D 类计数）。
    **对「元素先渲染、属性先写占位值」的钩子**（如 `ref(0)` 初值、或初始渲染就输出 `0.00` 的卡片），
    **必须用显式就绪标记**（如 `data-amount-state='ready'` / `data-fee-state='ready'` / `data-state='ready'`），
    **不得**用 READY_NONEMPTY —— 否则会把占位 0 当成真实值（P3-2 AC-50b、P3-4 AC-46a 两次事故即此形）。
    判断口诀：**看元素是否先于数据渲染，而不是看值是否可能为 0。**

    实现说明：selector/attr 直接内联进 JS 表达式（不用 arg=），
    因为部分 Playwright 版本 wait_for_function 的 arg 传参不生效，会导致永远等不到。
    """
    if ready_value is None or ready_value == '':
        raise ValueError(
            "wait_for_data_attr 必须显式传 ready_value（不允许 None/空串）；数值类钩子若要"
            "『非空即就绪』语义，请显式传 READY_NONEMPTY，并确认该元素在数据未到时不会渲染 0")
    js = (
        "() => {const el = document.querySelector(%s); if (!el) return false;"
        " const v = el.getAttribute(%s); if (v === null) return false;"
        " return %s;}"
        % (json.dumps(selector), json.dumps(attr),
           ("v !== ''" if ready_value == READY_NONEMPTY else "v === %s" % json.dumps(ready_value)))
    )
    try:
        page.wait_for_function(js, timeout=timeout)
    except Exception as e:
        raise RuntimeError(
            "wait_for_data_attr 超时：selector=%r attr=%r ready_value=%r（%ss）：%s"
            % (selector, attr, ready_value, timeout // 1000, str(e)[:160]))
    return read_data_attr(page, selector, attr)


def read_data_attr(page, selector, attr):
    """不等待，只读 [attr] 的值（优先用 wait_for_data_attr；仅限已知就绪场景）。"""
    loc = page.locator(selector).first
    val = loc.get_attribute(attr)
    if val is None:
        raise RuntimeError("read_data_attr：selector=%r 上不存在属性 %r（是否还没就绪？）" % (selector, attr))
    return val


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
