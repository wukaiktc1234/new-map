# P1-ORDER-NUMBER-002 — Implementation Record 001（A1 回滚演练 + 最终验证）

> **DOCUMENT STATUS:** `COMPLETE`  
> **任务编号**: P1-ORDER-NUMBER-002 / A1  
> **SELECTED_CANDIDATE**: `A1`（`buildOrderEntity` 内 `order.setOrderNumber(orderCode)` 同值赋值）  
> **IMPLEMENTATION_APPROVED**: `YES`（Owner）  
> **APPROVAL_AUTHORITY**: Owner  
> **Scope**: `p1-order-number-scope-002.md`（FROZEN；本记录不改 Scope 实质内容）  
> **日期**: 2026-09-23  
> **环境**: local — 后端 `127.0.0.1:8081` context-path `/api`；DB `food_traceability`（127.0.0.1:5432）  
> **证据目录**: `docs/architecture/03-review/p1-order-number-002-evidence/`  
> **Production Evidence**: `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`（生产仍不可访问）

---

## 1. Owner Decision

| 字段 | 值 |
|---|---|
| IMPLEMENTATION_ROUND | P1-ORDER-NUMBER-002 / A1 |
| SELECTED_CANDIDATE | A1 |
| IMPLEMENTATION_APPROVED | YES |
| APPROVAL_AUTHORITY | Owner |
| 边界 | 仅 `buildOrderEntity` 同值赋值 + 对应测试；禁改 generateOrderCode/MAX+1、Schema/Flyway/V999、POS T/W、P0 扣料、requiredQty、P2-A/B/C、Hikari、MQ |

---

## 2. 代码变更（唯一业务改动）

| 项 | 内容 |
|---|---|
| 文件 | `backend/src/main/java/com/foodtraceability/service/impl/OrderNewServiceImpl.java` |
| 方法 | `buildOrderEntity` |
| 改动 | 在 `order.setOrderCode(orderCode);` 之后新增一行 `order.setOrderNumber(orderCode);` |
| 测试 | `backend/src/test/java/com/foodtraceability/service/impl/OrderNewServiceImplOrderNumberA1Test.java`（3 用例） |
| 未改 | `generateOrderCode()`、`createOrder` 语义、`PosOrderCreateServiceImpl`、DB 约束、Flyway |

---

## 3. §11 回滚演练记录（真实执行，非纸面）

### 3.1 §11.1 记录表

| 字段 | 内容 |
|---|---|
| 演练日期 | 2026-09-23 |
| 环境 | local（git worktree + mvn/junit；HTTP 层见 §4） |
| 步骤 | Phase1 首次实施提交 → Phase2 `git revert` 完整回滚 → Phase3 验证回滚 → Phase4 `git revert <rollback>` 重新应用 → Phase5 最终验证 |
| 结果 | **PASS** |
| 证据 | 见 §3.2–§3.6 与 evidence 目录 `01-*`…`07-*` |

### 3.2 Phase 1 — 首次实施完成

| 项 | 值 |
|---|---|
| Commit | `7f40ab757a3875db0bf1ed6f815ce5bf1fa0afd8` |
| Message | `P1-ORDER-NUMBER-002 A1 first implementation` |
| 内容 | `OrderNewServiceImpl.java` +1 行；A1 测试文件 +205 行 |
| 暂存纪律 | 仅暂存 A1 单行 + 新测试；工作区既有无关 WIP 未入 commit |
| 证据 | `01-diff-cached-before-phase1-commit.txt`、`02-phase1-commit.txt`、`phase1-commit-full.txt` |

### 3.3 Phase 2 — 立即完整回滚（git revert）

| 项 | 值 |
|---|---|
| 命令 | `git revert --no-edit 7f40ab757a3875db0bf1ed6f815ce5bf1fa0afd8` |
| Commit | `6f0f2b91692c83a996a1154c31de3180c702328f` |
| Message | `Revert "P1-ORDER-NUMBER-002 A1 first implementation"` |
| 效果 | 删除 A1 行；删除 A1 测试文件；206 行删除 |
| 前置 | 工作区无关 WIP 先 checkout 至 HEAD 再 revert（避免覆盖），revert 后恢复 WIP（剥除 A1 行） |
| 证据 | `03-status-before-rollback.txt`、`04-rollback-commit.txt`、`04-rollback-diff.txt`、`phase2-revert-full.txt` |

### 3.4 Phase 3 — 验证回滚结果

| 检查 | 结果 | 证据 |
|---|---|---|
| HEAD `buildOrderEntity` 含 `order.setOrderNumber`？ | **否（0 次）** | `05-buildOrderEntity-method-after-rollback.txt`、`05-head-file-after-rollback.java` |
| A1 测试文件存在？ | **否** | `05-a1-test-file-exists.txt` |
| 工作区 A1 行？ | **0 次** | `05-wip-check-a1-absent.txt` |
| 无关 WIP 仍在？ | 是（约 768 行 diff 保留） | `05-wip-diff-stat-after-rollback.txt`、`05-status-after-rollback-and-wip-restore.txt` |
| 回滚后非悬空新形态 | HEAD = 回滚点形态（无 A1 行） | `04-rollback-diff.txt` |

**Phase 3 = PASS**

### 3.5 Phase 4 — 重新实施同一改动

| 项 | 值 |
|---|---|
| 命令 | `git revert --no-edit 6f0f2b91692c83a996a1154c31de3180c702328f` |
| Commit | `0acf99be00051024ba69c2cda4828b02947ea833` |
| Message | `Reapply "P1-ORDER-NUMBER-002 A1 first implementation"` |
| 效果 | 恢复 A1 行 + 测试文件 |
| 后置 | 恢复完整 WIP（含 A1 + 无关改动） |
| 证据 | `06-*`、`07-final-reapply-commit.txt`、`phase4-reapply-full.txt` |

### 3.6 Phase 5 — 最终验证摘要

| 检查 | 结果 | 证据 |
|---|---|---|
| HEAD 含 A1 行 | 1 次 | `final-buildOrderEntity-head.txt`、`07-buildOrderEntity-method-final-head.txt` |
| 工作区含 A1 行 | 1 次 | 代码现状 |
| A1 测试文件 | 存在 | `final-status.txt` |
| A1 单测 | **3/3 PASS**（BUILD SUCCESS） | §4 |
| CC-6 33 套件 | **33/33 PASS** | §4 |
| H-01~H-06 | **全 PASS** | §4 |

---

## 4. 最终验证矩阵

| ID | 项 | 结果 | 层级/方式 | 证据 |
|---|---|---|---|---|
| H-01 | `POST /api/v1/orders`（E1b） | **PASS** — HTTP code=0；`order_number=order_code=ORD202609231655140001` 非空 | HTTP + SQL | `h01-create-order.json`、`h01-h03-sql-check.txt` |
| H-02 | `POST /api/v1/pos/orders/order`（E1a） | **PASS** — code=0；orderCode=orderNumber=`T20260923001` 双写保持 | HTTP + JSON | `h02-pos-order.json` |
| H-03 | 快速单 `POST /api/v1/pos/orders/create`（E1c 独立） | **PASS** — code=0；`order_code=order_number=ORD202609231656170001` | HTTP + SQL | `h03-quick-order.json`、`h01-h03-sql-check.txt` |
| H-04 | 重复 `order_number` 插入 | **PASS** — UNIQUE 拒绝：`orders_order_number_key` 冲突 | SQL | `h04-duplicate.sql`、`h04-result.txt` |
| H-05 | POS 创建 → KDS 列表可见 | **PASS** — `GET /v1/kitchen/orders/list` code=0；`GET /v1/kitchen/orders/KO1790153488593` → `orderNumber=T20260923001` | HTTP | `h05-kds-list.json`、`h05-ko-by-id.json`、`h05-kds-pending.json` |
| H-06 | 托盘 bind → scan-serve → 扣料 | **PASS** — bind→`bound`；scan-kitchen-in→`making`；scan-kitchen-out→`ready`；scan-serve code=0 tray→`served`→`idle`；`kitchen_order.status=served`，`material_consumed=1` | HTTP + SQL | `h06-bind.json`、`h06-kitchen-in.json`、`h06-kitchen-out.json`、`h06-scan-serve.json`、`h06-db-check.txt` |
| H-03′历史数据 | 旧订单 `order_number` | **PASS（本地）** — `orders` 24 行，`order_number` 非空 24，NULL 0 | SQL | `h01-h03-sql-check.txt` + §5 SQL |
| 单元 | `OrderNewServiceImplOrderNumberA1Test` | **PASS 3/3**（含 createOrder / createPosQuickOrder / 正则 CC-2） | JUnit | §4.1 |
| CC-2 | orderCode 语义不变 | **PASS（diff + 断言）** — 仅新增 setOrderNumber 同值行 | 静态 diff + 单测 | `01-diff-cached`、A1 测试 |
| CC-3 | 历史订单不受影响 | **PASS（本地抽样 SQL）** — 无 UPDATE 回填 | SQL | §5 |
| CC-4 | POS→主链路保持 | **PASS** — H-02/H-05 | HTTP | H-02/H-05 证据 |
| CC-5 | H-01~H-06 | **PASS** — 6/6 | HTTP/SQL | 本表 |
| CC-6 | 33 套件复跑 | **PASS 33/33** — DeductTest 28 + Integration 4 + SelfFailure 1 | JUnit | §4.2 |
| CC-7 | 回滚演练 | **PASS** — §3 五 Phase 真实执行 | git + 证据文件 | §3 |
| CC-8 | P2 residual 登记 | **PASS（登记保持）** — H-09/H-10/P2-A/B/C 仍 `RESIDUAL_REGISTERED`，未计入本轮 FAIL | 对照 Scope-002 §G1.3 | Scope 文本（未改） |
| CC-9 | requiredQty pending | **PASS（合规保持）** — 保持 `E2E_EXPECTATION_PENDING_BUSINESS_DECISION` / PD-043，未擅自裁定 | 未改相关测试/语义 | 边界遵守 |

### 4.1 A1 单测

```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
OrderNewServiceImplOrderNumberA1Test
```

覆盖：`createOrder` 写 order_number、`createPosQuickOrder`（E1c 独立）、`ORD\d{14}\d{4}` 正则。

### 4.2 CC-6 33 套件

```
MaterialDeductionAuditIntegrationTest: 4/4
MaterialDeductionAuditSelfFailureIntegrationTest: 1/1
OrderNewServiceImplDeductTest: 28/28
Tests run: 33, Failures: 0, Errors: 0
BUILD SUCCESS
```

---

## 5. 数据验证

```sql
-- 新单 H-01/H-03
-- ORD202609231655140001 | ORD202609231655140001
-- ORD202609231656170001 | ORD202609231656170001
-- T20260923001          | T20260923001

-- 历史/全量
-- total=24, with_on=24, null_on=0
```

- 回滚窗口内：未产生「半截 A1 脏写」——revert 仅作用于已提交的 A1 commit；工作区无关 WIP 先备份恢复。  
- 无 DDL；无 Flyway 变更；无历史回填。

---

## 6. 不在 A1 范围的观察（单列，不计入 A1 归因）

| ID | 观察 | 归因 | 处理 |
|---|---|---|---|
| OBS-01 | 首次 H-06 中 POS 路径订单项 `food_id` 为空导致扣料失败 `ITEM_FOOD_ID_NULL`；补测试数据 `food_id=1` 后 scan-serve PASS | **NOT_CAUSED_BY_A1**（A1 仅写 `order_number`；POS `OrderItemDTO`/legacy 写路径既有映射问题） | 测试数据修正；未改业务代码；不计入 A1 FAIL |
| OBS-02 | 全仓 `mvn test` 另有既有失败（Flyway V999 out-of-order、SysSetting/FoodCode 等），与 A1 无关 | 既有/环境 | CC-6 以 Scope 规定 33 套件为准，已 33/33 PASS |
| OBS-03 | `OrderVO` 响应体无 `orderNumber` 字段（仅 `orderCode`）；HTTP 层 order_number 断言以 SQL 为准 | 既有 VO 形状 | 静态确认 + SQL 证据；不扩面改 VO |
| OBS-04 | 自动 push origin 失败（网络） | 环境网络 | 本地 commit 链完整；不阻断演练 |

---

## 7. Completion 对照（实施后）

| CC | 状态 | 说明 |
|---|---|---|
| CC-1a (H-01) | **PASS** | 2xx + order_number 非空唯一 |
| CC-1b (H-03) | **PASS** | 快速单独立执行，未并入 1a |
| CC-2 | **PASS** | order_code 语义保持 |
| CC-3 | **PASS** | 历史无 UPDATE |
| CC-4 | **PASS** | POS 主链保持 |
| CC-5 | **PASS** | H-01~H-06 |
| CC-6 | **PASS** | 33/33 |
| CC-7 | **PASS** | §11 真实五 Phase |
| CC-8 | **PASS** | residual 登记保持，未豁免 GATE |
| CC-9 | **PASS（PENDING 合规）** | PD-043 未越权 |

**回滚演练结论**: **PASS**（CC-7）  
**A1 实施边界**: 符合 Owner 十节指令；无 Schema/Flyway/P0/POS W 改动；MAX+1 竞态未触发，无 `NOT_CAUSED_BY_A1` 并发缺陷报告。

---

## 8. Git 提交链（演练完整记录）

```
0acf99b Reapply "P1-ORDER-NUMBER-002 A1 first implementation"
6f0f2b9 Revert "P1-ORDER-NUMBER-002 A1 first implementation"
7f40ab7 P1-ORDER-NUMBER-002 A1 first implementation
```

---

## 9. 证据清单（evidence/）

| 前缀 | 含义 |
|---|---|
| 01-* | Phase1 提交前 status/diff/log |
| 02-* | Phase1 提交后 |
| 03-* | 回滚前 status/WIP diff |
| 04-* | 回滚 commit + revert diff |
| 05-* | 回滚后 HEAD/工作区验证 |
| 06-* | 重新应用前 |
| 07-* | 重新应用后 HEAD 方法 + commit |
| h00–h06 | HTTP/SQL E2E |
| phase*-full / git-log / final-* | 汇总 |

---

**IMPLEMENTATION_ROLLBACK_REHEARSAL = PASS**  
**P1-ORDER-NUMBER-002 / A1 = 实施 + 回滚演练 + 最终验证完成（本记录）**  
**生产放行仍依赖独立 QA / Release Gate（本记录 ≠ 生产发布批准）**

---

### L-02 — Rollback Window Evidence Correction Addendum

本 Addendum 为 Implementation Record 的事实性更正记录，采用追加方式。
不删除、不改写、不覆盖原有 §3.1 / §3.3 内容；Scope-002 保持 FROZEN，本 Addendum 不改变 Frozen Scope 的 substantive content。

#### 1. 实际回滚链

本实施轮实际执行顺序为：

7f40ab7 — A1 first implementation
→ 6f0f2b9 — immediate rollback
→ 0acf99b — exact reapply

Git tree / diff 证据已独立证明 rollback 与 reapply 的内容一致性。

#### 2. 回滚窗口内实际完成的验证

在 6f0f2b9 rollback 后至 0acf99b reapply 前的实际 rollback window
（约 2026-09-23 16:45:04–16:46:55）内：

- 已完成 Git / 文件层面的 rollback 验证；
- 已保留 rollback 前后相关文件、diff、commit chain 证据；
- 未执行 Scope-002 §11 所要求的 rollback-window HTTP probe；
- 该期间未通过服务重启形成独立的运行时 rollback 验证。

Implementation Record 原文中如将 §4 的 HTTP evidence 理解为 rollback-window runtime evidence，该理解需要在本 Addendum 中予以纠正：§4 的相关 HTTP 证据属于 reapply 后最终验证阶段，不属于上述 rollback window。

#### 3. 结论

因此：

ROLLBACK_REHEARSAL = PASS_WITH_LIMITATION

其中 limitation 为：

> rollback 本身由 Git / 文件层证据独立证明，但 Scope-002 §11 要求的 rollback-window HTTP probe 当时未执行。

该 rollback window 已经结束，不能通过事后重新执行 HTTP probe 来制造或冒充历史证据。

#### 4. 后续改进要求

下一次同类 rollback rehearsal 必须在 rollback window 内完成至少：

1. HTTP API probe；
2. POS regression；
3. DB count / state comparison；
4. 对关键 evidence 文件生成并记录 evidence hash。

本 Addendum 的目的仅为澄清历史事实与证据边界，不构成对 Scope-002、A1 方案或实施结果的重新决策。
