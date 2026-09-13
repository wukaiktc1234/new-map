# D-IG — Inventory Semantic Reconstruction Independent Adversarial Review Task 001

> **状态**: ACTIVE_REFERENCE — INDEPENDENT ADVERSARIAL REVIEW TASK
> **Decision ID**: D-IG
> **対象**: `d-ig-inventory-semantic-reconstruction-001.md`
> **Role**: Independent Adversarial Reviewer
> **Independence**: 必须不同于 Reconstruction 调查执行者；不得由原调查者自行完成
> **Production Evidence**: BLOCKED
> **Identity Decision**: NOT_MADE
> **Required Identity Object Set**: NOT_STARTED
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO

## 1. Purpose

本任务对 Inventory Semantic Reconstruction 001 做独立反方审查。

本任务不是重新进行一次大范围 Inventory Reconstruction，也不是 Owner Decision、Identity Decision、Schema 设计或 Migration 决策。

审查目标是验证：Reconstruction 调查中最关键的四个高影响结论，是否具有足够的证据强度、覆盖度、反证处理能力和 provenance discipline。

## 2. Governance Status Interpretation

Reconstruction 记录中的：

`COMPLETED_PENDING_INDEPENDENT_ADVERSARIAL_REVIEW`

必须解释为：

> 调查者声明其认为已满足 Reconstruction Task 001 的 11 项 completion criteria。
>
> 该声明不是治理链对完成状态的确认，也不是独立审查结论。
>
> 治理是否接受 Reconstruction 完成，以本独立反方审查的最终结果为准。

独立审查不得因调查者已经写出 `COMPLETED` 而降低证明标准。

## 3. Evidence Baseline Requirement

四个攻击点必须尽可能基于同一证据基线执行。

审查开始时必须登记：

- repository governance baseline commit / ref；
- local engineering workspace commit 或明确的 working-tree state；
- local PostgreSQL instance identity / snapshot time；
- migration baseline；
- test-data / fixture baseline（如适用）；
- production evidence status：`BLOCKED`。

若审查期间代码、数据库快照、migration 或治理输入发生变化：

1. 记录变化时间与范围；
2. 判断哪些攻击点受影响；
3. 对受影响攻击重新执行或重新确认；
4. 最终报告不得把不同基线下的结果伪装成同一批次结果。

## 4. Result Vocabulary

每个攻击点只能使用：

- `PASS`
- `FAIL`
- `INCONCLUSIVE`

`INCONCLUSIVE` 不得自动解释为 PASS，也不得自动解释为 FAIL。

INCONCLUSIVE 必须登记为：

`UNRESOLVED_ATTACK`

并明确：

- 缺失的具体证据；
- 为什么现有证据不足；
- 影响是局部结论强度下降，还是影响整个 Reconstruction 的可信度；
- 是否必须 reopen Reconstruction；
- 是否可以仅保留为 `CONDITIONAL`。

## 5. Attack A1 — Inventory Balance Grain / Locator Uniqueness

### Target claim

Reconstruction 当前认为：

- 中央/仓库侧主要按 `(materialId, warehouseId)` 定位余额；
- 门店侧主要按 `(storeId, materialId)` 定位余额；
- `getByMaterialAndWarehouse` 等方法代表核心粒度执行机制。

### Required adversarial search

检查是否存在：

1. 其他 Repository / Service 定位 Inventory 记录路径；
2. 直接按 `inventory.id` 或其他单键定位后更新余额的活跃业务路径；
3. Mapper / XML / native SQL 绕过既有 Repository 粒度方法；
4. 批处理、定时任务、事件监听器、脚本、数据同步路径直接定位或写入 Inventory；
5. 使用不同组合字段定位同一余额业务对象的活跃路径；
6. 既有路径只是测试、死代码、废弃代码、不可达代码还是实际活跃业务路径。

### Acceptance criteria

`PASS`：

在已声明证据范围内，所有 materially relevant 且被证明为活跃的 Inventory 定位/写入路径均符合当前粒度模型，或能够证明其他写法与当前粒度约定业务等价；没有活跃的实质性绕过路径。

`FAIL`：

发现至少一条已经证明在真实业务流程中使用、且对余额 referent/grain 产生实质不同解释的活跃绕过路径。

`INCONCLUSIVE`：

发现可疑绕过路径，但无法证明其活跃性、业务作用或实际 referent，或者关键代码面无法可靠覆盖。

### Special rule

“源码中存在另一个查询方法”不等于 FAIL；必须建立“活跃 + 业务相关 + 语义实质不同”的证据链。

## 6. Attack A2 — Consumption Deduction Reality

### Target claim

Reconstruction 将 `MaterialConsumptionServiceImpl.deductInventory` 当前行为描述为非功能性数量扣减路径，并将此前消费扣减正向证据 `SUPERSEDE → NON-FUNCTIONAL`。

### Required adversarial search

检查：

1. 其他 MaterialConsumption / Recipe consumption service 是否实际修改 Inventory 数量；
2. 是否通过 `inventory_transactions` 间接完成真正的库存变化；
3. 是否存在 event listener / scheduler / asynchronous worker 完成数量扣减；
4. 是否改写 `store_inventory` 或其他库存持有表，而不是 `inventory`；
5. 是否存在 DB trigger / procedure / native SQL 改变库存数量；
6. 是否存在消费流程只在当前类置 flag、随后由另一条活跃路径异步扣减的情况；
7. 当前 0 行数据是“未发生消费”还是“消费功能不存在”的可证性边界。

### Acceptance criteria

`PASS`：

能够证明在审查范围内，所有 materially relevant 的消费扣减路径均不会实际修改 `inventory` 数量，且不存在另一条已证明活跃的替代扣减机制足以改变 Reconstruction 对“非功能性路径”的结论。

`FAIL`：

存在至少一条已经证明活跃、业务相关，并实际导致 Inventory 数量变化的消费扣减路径。

`INCONCLUSIVE`：

存在可疑替代机制，但无法证明其活跃性、实际数量变化或业务关联。

### Special rule

“当前本地 0 行”只能证明当前快照中没有该类数据，不能单独证明生产/历史上从未发生，也不能单独证明功能不存在。

## 7. Attack A3 — `@Transient` vs Physical Persistence

### Target claim

Reconstruction 将 `productName`、`storeId`、`inventoryType` 描述为 transient / 不持久化，并据此解释空间/类型信息无法稳定从 `inventory` 表落库。

### Required adversarial search

必须分别验证：

1. ORM Entity 是否使用 transient / 非持久化定义；
2. 实际数据库物理列是否存在；
3. XML Mapper 是否写入这些列；
4. native SQL / JDBC 是否绕过 ORM 写入这些列；
5. 其他 Entity / view / sync table 是否承载同名持久化信息；
6. BE-04 中 `store_id` 0% 填充是否与实际 physical schema 一致；
7. “ORM 不持久化”与“数据库不存在对应持久化列”是否被正确区分。

### Acceptance criteria

`PASS`：

同时满足：

- ORM 层明确不持久化；
- DB physical schema 无对应持久化列；
- 没有已证明活跃的 Mapper/native SQL 等旁路写入。

`FAIL`：

数据库实际存在对应物理列，或已证明存在活跃旁路写入；此时必须将原结论缩窄为至少“该 ORM 属性不持久化”，不得继续写成“该业务信息无法在 inventory 物理层持久化”。

`INCONCLUSIVE`：

ORM 证据明确，但 physical schema 或旁路路径无法可靠确认。

### Special rule

`@Transient` 不得被单独解释为“数据库一定没有对应列”。

## 8. Attack A4 — Product-first Migration Interpretation

### Target claim

Reconstruction 将以下作为历史事实：

- Inventory 初始 migration 使用 `product_id/product_name`；
- 后续 migration 添加 `material_id`；
- 但不从此事实直接推出“业务从 Product 转型 Material”。

独立审查重点攻击的是“Product-first”是否被历史命名或技术表象过度解释。

### Required adversarial search

检查：

1. 初始 migration 时 `product` 表的当时业务语义；
2. 初始 `product_id` 是否确实代表 Product business object，而非历史命名下的 Material/其他对象；
3. 后续追加 `material_id` migration 是否存在注释、PR、commit message 或关联变更说明业务动机；
4. 是否存在重命名/兼容迁移，而不是新增业务语义；
5. migration 前后字段/表的关系是否支持“new object semantics”还是“same object relabeling”；
6. 历史代码是否提供与 migration 文字一致或相反的业务 referent 证据。

### Acceptance criteria

`PASS`：

能够确认 migration 的结构变化，但无法以 migration alone 推导 Product 是原始独立业务对象；Reconstruction 对“Product-first business semantics”保持 INFERENCE / UNRESOLVED discipline。

`FAIL`：

存在直接、明确的历史证据证明 migration 明确表达了 Product 与 Material 的业务模型切换，而 Reconstruction 仍将其完全视为无法解释的技术历史；或反之，发现初始 `product` 实际明确就是 Material，而 Reconstruction 仍把它表述成 Product business history。

`INCONCLUSIVE`：

关键历史 migration / schema / commit context 缺失、不可读或无法建立当时 referent。

### Special rule

“表名叫 product”不等于“业务对象就是 Product”；“字段叫 product_id”同样不等于业务 referent 已被证明。

## 9. Cross-Attack Dependency Rules

四个攻击可以分别执行，但**不得假设结果彼此完全独立**。

### Dependency A3 → A1

若 A3 发现 `storeId` / `inventoryType` 等存在 physical persistence 或旁路写入，必须重新检查 A1 的粒度输入是否因此发生实质变化。

### Dependency A1 → A2

若 A1 发现活跃的绕过定位路径，必须检查该路径是否就是消费扣减路径或其他库存变更路径；如是，A2 必须重跑受影响部分。

### Dependency A4 → Reconstruction historical layer / Owner Q1

若 A4 改变 Product-first 的历史语义解释，必须更新 Reconstruction 的历史 Interpretation，并检查是否影响 Q1 的提问前提。

### General rule

任何攻击点结果改变另一攻击点的输入条件：

`DO NOT retain old verdict silently`

必须重新执行受影响攻击，并记录“trigger → re-run → result”。

## 10. Evidence and Provenance Discipline

所有结论必须明确区分：

- `[R-DOC]`
- `[CODE]`
- `[DB-SCHEMA]`
- `[LIVE-REPRO]`
- `[MIGRATION]`
- `[TEST-DATA]`
- `[PROD]`

没有 Production Evidence 时继续使用：

`PROD = BLOCKED`

本地代码、数据库快照和测试数据不得自动升级为生产事实。

外部模型的结论若无法回到可复现源码/DB/migration/provenance，只能作为 `EXTERNAL_UNREPRODUCED`，不得作为 PASS 证据。

## 11. Overall Review Verdict

总体审查只能输出：

- `PASS`
- `PASS_WITH_REPAIRS`
- `REOPEN_RECONSTRUCTION`
- `BLOCKED`

推荐判定规则：

### PASS

四个攻击点均 `PASS`，不存在未解决且足以改变 Reconstruction 核心结论的 evidence gap，且 cross-attack 依赖检查闭合。

### PASS_WITH_REPAIRS

核心 Reconstruction 结论仍成立，但存在限定性 wording / provenance / scope repairs，不需要重新进行 Reconstruction 主体调查。

### REOPEN_RECONSTRUCTION

任一攻击点 `FAIL` 且该失败实质改变 Inventory holding object、balance grain、behavior-path 或历史语义核心结论；或多个 `INCONCLUSIVE` 共同导致 Reconstruction 的可信度不足。

### BLOCKED

关键证据入口不可获得，导致无法对 materially relevant claim 作出可靠判断，并且当前证据不足以支持继续保持 Reconstruction 的完成接受状态。

## 12. INCONCLUSIVE Handling

单个 `INCONCLUSIVE` 不得自动触发 REOPEN，也不得自动转换为 PASS。

必须先判断影响级别：

`LOCAL_CONCLUSION_STRENGTH_ONLY`

或

`RECONSTRUCTION_CORE_CREDIBILITY`

前者：对应结论保持 `CONDITIONAL / UNRESOLVED`，允许总体进入 `PASS_WITH_REPAIRS` 的可能性。

后者：应 `REOPEN_RECONSTRUCTION` 或 `BLOCKED`。

不得以“继续普通搜索也许会找到答案”无限拖延；必须明确缺口、证据入口和治理出口。

## 13. Required Review Output

最终必须输出一份独立审查记录：

`03-review/d-ig-inventory-semantic-reconstruction-independent-adversarial-review-001.md`

至少包含：

1. reviewer identity / independence declaration；
2. evidence baseline；
3. A1–A4 result table；
4. each attack evidence and exact provenance；
5. FAIL / INCONCLUSIVE rationale；
6. cross-attack dependency rerun log；
7. reconstruction conclusion impact；
8. overall verdict；
9. required repairs / reopen conditions；
10. Owner handoff readiness；
11. explicit statement that this review does not decide Identity / H1/H2 / Schema / Migration。

## 14. Downstream Gate

在本独立审查得到：

`PASS` 或 `PASS_WITH_REPAIRS`

之前：

- 不进入 Owner Decision execution；
- 不进行 C-002 E2/E4 aligned re-evaluation；
- 不建立 candidate-level PASS。

若结果为：

`REOPEN_RECONSTRUCTION` 或 `BLOCKED`

则返回 Reconstruction lane，先修复/补证，再重新独立审查。

Owner Decision Request 001 的问题集（Q1/Q2/Q3/N1/N2）在审查期间保持原样，不因反方审查自行增加业务答案。
