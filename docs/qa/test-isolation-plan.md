# 测试与门禁彻底隔离方案（Owner 2026-10-29 要求，本轮只出方案，不实施）

> ## 🔴 结论先行（Lead 2026-10-29）
> **在 DBGUARD 落地前，任何人不得在共享库 `food_traceability` 运行 `scripts/qa/` 下含写操作的脚本。**
>
> DBGUARD 已于 2026-10-29 落地（`scripts/qa/_dbguard.py` + 14 个含 DELETE 脚本硬拦截），
> 但**删除条件仍是 store_id / 硬编码 id，不是 tag** —— 隔离未完成前只允许在 `food_traceability_test` / `ft_gate_*` 运行。


## 一、现状盘点（脚本 → 目标库 → 是否写数据）

### 1.1 直连共享库 `food_traceability` 的脚本（**风险面**）

| 脚本 | 目标库 | 读/写 | 说明 |
|------|--------|-------|------|
| `scripts/qa/consistency_check.py`（+ `.sql`） | `food_traceability`（`--database` 默认值） | **只读**（纯 SELECT） | 巡检，只读安全 |
| `scripts/qa/security_regression.py` | `food_traceability` | **只读 + 部分写**（登录/接口调用会写登录日志） | 见 1.2 |
| `scripts/qa/http_error_body_check.py` | `food_traceability`（经后端 HTTP） | **不直接写库**，但会调 resolve → 经后端写 `fund_flows` | **已修为零流水**（预置元数据方案） |
| `scripts/qa/audit_*.py`、`broken_endpoints_prod.py`、`cross_layer_audit.py` | `food_traceability` | 只读 | 审计类 |
| `scripts/qa/deposit_slip_*_selftest.py`、`bl7_bl8_selftest.py` | `food_traceability` | **写**（自测会造缴款单/流水） | ⚠ 高风险 |
| `scripts/qa/e2e_*.py`（约 12 个） | `food_traceability`（`--pg-db` 默认值） | **写 + 清理**（`e2e_cleanup.py` 会 DELETE） | ⚠ 最高风险 |
| `scripts/qa/preflight.py` + `deploy_gap_*.py` | **`food_traceability_test`** | 写（仅测试库） | ✅ 已按目标形态 |

### 1.2 关键结论

```text
① 巡检（consistency_check）是纯 SELECT —— **可以直接连共享库**，无需隔离；
② 真正危险的是 3 类：e2e_*.py（写+删）、deposit_slip_*_selftest.py（写）、
   以及任何「经后端 HTTP 制造状态」的门禁（本轮 gate 第一版就是这类，已修）；
③ 现有 preflight 已经在用 food_traceability_test，说明**基础设施已具备**，只是没强制。
```

## 二、目标形态

```text
分层三档：
  A. 只读巡检（consistency_check / audit_* / cross_layer_audit）
     → 允许连共享库 food_traceability（只读事务，statement_timeout=30s）
  B. 会写数据的自测/门禁（e2e_*, deposit_slip_*_selftest, 任何走 HTTP 造状态的）
     → **必须**连 food_traceability_test 或一次性库 ft_gate_<timestamp>
  C. 需要「真实业务链」的集成用例（T2 并发、T4 交班、异常单 resolve 全链路）
     → 在 B 类隔离库里跑，靠 Flyway 迁移 + 夹具初始化脚本还原完整 schema
```

## 三、落地步骤

```text
Step 1 数据库清单固化（docs/qa/db-inventory.md）
   - food_traceability            生产/共享：只读工具可用；写=拒绝
   - food_traceability_test       共享测试库：B 类脚本默认目标
   - ft_gate_<ts>                 一次性库：CI 每次新建 → Flyway migrate → 跑 → DROP
   - ft_exc / ft_r4               已有的一次性库（迁移验证用），纳入同一套命名规范

Step 2 脚本改造点（统一 env 约定）
   - 所有 scripts/qa/*.py 增加:  PGDB = os.environ.get('QA_PGDB', <按分层默认>)
   - B 类脚本的默认值一律改为 food_traceability_test（不再默认共享库）
   - 新增 scripts/qa/_dbguard.py:
       def assert_writable_target(db):
           if db in ('food_traceability', 'food_traceability_prod'):
               print('[REFUSE] 目标库 %s 为共享/生产库，写操作被拒绝（死命令 1/2）'); sys.exit(2)
   - 所有 INSERT/UPDATE/DELETE 前调用该方法

Step 3 防呆（连错库直接退出）
   - B 类脚本启动先 SELECT current_database()，与期望比对，不符 → exit 2
   - 启动日志固定打印: [DBGUARD] target=<db> mode=<readonly|write> verdict=<allow|refuse>

Step 4 迁移与夹具初始化
   - 一次性库创建: FlywayMigrateRunner（已有工具，ft_exc/ft_r4 验证过的路径）
   - 夹具初始化: scripts/qa/fixtures/seed_min.sql（最小门店/账户/菜品/权限）
   - 永久 gate 夹具（[QA-GATE2:*]）在 seed 里预置，**不再依赖共享库**

Step 5 CI 接线
   - preflight 分两段: 只读段连共享库；写入段连 ft_gate_<ts>
   - 结束后 DROP DATABASE ft_gate_<ts>（失败也 drop，用 try/finally）
```

## 四、成本与风险

| 项 | 成本 | 风险/备注 |
|----|------|-----------|
| 12 个 e2e 脚本改默认库 | 每脚本 1 行 + 加 dbguard 调用 | 低；但需逐个回归确认不依赖共享库历史数据 |
| 5 个 deposit_slip/bl7 自测 | 同上 | 中；这些测试依赖已有门店/账户数据 → 需要 seed_min.sql |
| T2 并发、T4 交班（走 HTTP 全链路） | 需要在隔离库起一个**独立后端实例** | 高：`-Dspring.datasource.url` 指向 ft_gate_<ts>，端口 8082，避免与 8081 共享 |
| 一次性库创建+迁移耗时 | 约 40-60s/次（267 个迁移） | 中；CI 可复用 ft_gate_base 快照 + CREATE DATABASE ... TEMPLATE |
| 只读巡检继续连共享库 | 0 | 可接受：纯 SELECT，且已有 statement_timeout |

## 五、本轮已完成的部分（不是方案，是既成事实）

```text
✅ gate（http_error_body_check.py）已改零流水：不再经 HTTP 制造「已处理」态，改预置元数据；
✅ 夹具登记 docs/qa/fixtures-registry.md 明确「谁可清理/谁不可」；
✅ 死命令 1-4 写入 ADR-and-Danger-Zones.md。
❌ 尚未：e2e/self-test 脚本的库隔离（需单独排期，涉及独立后端实例）。
```
