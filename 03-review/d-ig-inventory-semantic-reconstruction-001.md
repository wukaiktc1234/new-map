# D-IG — Inventory Semantic Reconstruction 001

> **状态**: ACTIVE_REFERENCE — RECONSTRUCTION EVIDENCE RECORD
> **Decision ID**: D-IG
> **Scope**: Inventory 业务语义、库存持有对象、余额记录粒度、对象关联及跨业务行为关系的受控证据重建。
> **Investigator Input**: External/local large-model engineering evidence investigation result
> **Production Evidence**: BLOCKED
> **Identity Decision**: NOT_MADE
> **Required Identity Object Set**: NOT_STARTED
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO
>
> 本记录固化本次模型调查提交的结果，但不将调查者解释自动升级为 Business Owner Decision 或 Identity Conclusion。所有本地工程/数据库观察均保持 local provenance；生产侧继续 `BLOCKED`。

## 1. Executive Disposition

本次 Inventory Semantic Reconstruction 在声明的受控范围内报告为：

`COMPLETED_PENDING_INDEPENDENT_ADVERSARIAL_REVIEW`

含义：调查者认为任务定义的 11 项 completion criteria 均已完成或有明确出口；但依据治理任务 001，独立反方审查仍是强制门禁，因此 `COMPLETED` 不等于独立审查通过，也不解除 C-002 inventory E2/E4 blocking。

本记录不得被解释为：

- Material 已经被正式裁决为 Inventory 唯一业务对象；
- Product 已经被排除；
- Food / 半成品 / 其他对象已经完成 Identity 排除；
- C-002 已重新 PASS；
- Owner 已完成 Q1/Q2/Q3/N1/N2 裁决；
- Schema / Migration 已获授权。

## 2. Evidence / Provenance Boundary

本记录来源层级：

- `[R-DOC]`：远程治理文档，用于任务边界、方法、门禁及既有结论。
- `[CODE]`：本地工作树源码调查，由本次大模型调查者报告；当前记录不宣称生产源码等价。
- `[DB-SCHEMA]`：本地 PostgreSQL schema/constraint 调查。
- `[LIVE-REPRO]`：本地 PostgreSQL `food_traceability` 实例的只读复现。
- `[MIGRATION]`：本地 migration 历史。
- `[TEST-DATA]`：本地 seed/fixture/样例数据。
- `[PROD]`：当前不可达，因此本记录不包含生产验证。

特别规则：模型调查者报告的工程事实进入本记录时仍应被视为“已提交的本地/调查证据”，不能因为表述完整而升级为 production fact；独立反方审查必须继续检查 provenance、覆盖度及解释跳跃。

## 3. Layer 1 — Facts

### 3.1 Inventory engineering reality

调查报告确认：

1. `inventory.material_id` 存在完整的本地代码写入路径，并在当前本地样例中 11/11 填充。[CODE][LIVE-REPRO]
2. `inventory.product_id` 从初始迁移即存在；当前本地样例只有 1/11 填充。[DB-SCHEMA][LIVE-REPRO][MIGRATION]
3. `inventory.product_id` 存在指向 `product(product_id)` 的数据库外键；`inventory.material_id` 当前无对应数据库外键。[DB-SCHEMA]
4. Inventory ORM 中 `getProductId()/setProductId()` 兼容访问器实际别名到 `materialId`，形成 Product 命名与 Material 落库之间的兼容层。[CODE]
5. 同一 Inventory record 已出现 `material_id` 与 `product_id` 同时填充并分别指向不同业务对象的本地实例（调查结果指出 `inventory_id=4`）。[LIVE-REPRO]
6. `uk_store_inventory_store_material` 被核认为唯一索引而非一般“约束”表述。[DB-SCHEMA]
7. `quantity` 在实体层映射为 `current_stock`；`productName`、`storeId`、`inventoryType` 被调查为 transient，不持久化。该事实用于解释为何代码中的空间/类型状态无法从该表直接稳定落库。[CODE]
8. TraceCode 扫码创建路径可形成 Inventory 行，但调查结果称该路径不携带任何 ID referent，仅保存文本名称，且 `inventory_type=2`；这意味着该类记录不能未经解释地作为 Material/Product referent 的正向证据。[CODE]
9. `MaterialArchive` 建档存在自动创建零库存行的 `ensureInventoryForMaterial` 路径，形成 Material archive → Inventory 的创建耦合。[CODE]
10. 采购入库路径由 `materialId` 驱动；调查结果同时发现代码注释将 `warehouseId` 作为 `storeId` 同步门店库存。[CODE]
11. `MaterialConsumptionServiceImpl.deductInventory` 在当前工程实现中仅设置 `inventoryDeducted=1`，不实际扣减库存数量；调查结果同时报告无有效创建调用方、相关实体/列错位及 0 行本地数据。[CODE][LIVE-REPRO]
12. LossOutbound 路径以 `productId` 参数进入，但实际通过兼容别名写入 `material_id`，表现为跨命名空间参数到 Material 余额键的技术转换。[CODE]
13. 多条库存扣减/出库路径使用 `materialId` 定位余额；调查结果将 SalesOrder 扣减列为唯一显著例外，其以销售域 product 值 + transient storeId 查找 Inventory，运行期具体列解析仍为未决。[CODE]

### 3.2 Data / schema reality

本次调查给出的代表性本地计数为：

| Object / table | Local count / observation | Provenance |
|---|---:|---|
| `inventory` | 11 rows | LIVE-REPRO |
| `inventory.material_id` | 11/11 filled | LIVE-REPRO |
| `inventory.product_id` | 1/11 filled | LIVE-REPRO |
| dual-filled inventory row | 1/11; `inventory_id=4` highlighted | LIVE-REPRO |
| `purchase_request_item` | 50 relevant rows; 46 resolvable, 4 anomalous | LIVE-REPRO |
| `purchase_order_items` | 84 | LIVE-REPRO |
| `inventory_transactions` | 27 | LIVE-REPRO |
| `material_archives` | 35 | LIVE-REPRO |
| `material_trace_code` | 18 | LIVE-REPRO |
| `material_consumption` | 0 | LIVE-REPRO |

以上数量全部是本地实例观察，不是生产统计。

### 3.3 Migration / historical facts

调查结果给出的 migration 演化事实：

- 初始 Inventory migration 以 Product 形态创建，包含 `product_id/product_name`。[MIGRATION]
- 后续 migration 添加 `material_id`。[MIGRATION]
- Material trace-code 类型经历过 BIGINT ↔ VARCHAR 的反向漂移。[MIGRATION]
- 当前 ORM 通过 Product 访问器别名到 Material，表现出历史兼容/语义漂移。[CODE]

以上事实不能单独推出“业务从 Product 转型为 Material”；该表述只能作为历史解释候选，并保持 inference 状态。

## 4. Layer 2 — Semantics

### 4.1 Inventory business-object reconstruction

当前最稳妥的工程语义表述为：

> 工程上，Inventory 表现为一个按空间上下文持有、以余额为主要记录形态、由业务 referent 键定位的库存持有记录；当前大量活跃写路径以 Material referent 为操作键。

该表述故意不把“Material = 唯一业务真相”写成既定事实，因为 Product-first 历史形态、Product FK、兼容别名和部分跨命名空间行为仍然存在未闭合语义问题。

### 4.2 Holding-object candidate set

受控枚举结果：

| Candidate | Current disposition | Evidence summary |
|---|---|---|
| Material | `SUPPORTED AS PRIMARY ENGINEERING REFERENT` | 多条创建/增加/扣减路径使用 `materialId`；MaterialArchive 自动建零库存；本地数据 11/11 `material_id` 填充。 |
| Product | `UNRESOLVED` | 初始 migration 及唯一 FK 指向 Product；当前仅 1/11 填充，且 ORM Product accessors alias 到 Material；尚无足够业务行为证据完成排除。 |
| Food | `NO_EVIDENCE IN inventory` | 调查未发现 Inventory 表的 Food 持有路径；既有 C-001 证据显示 food/foods 存在独立 stock 维度。 |
| Semi-finished / intermediate | `NO_EVIDENCE / NOT_IMPLEMENTED` | 未发现独立标记或持有路径；不能从无实现推出业务上“不需要”。 |
| Packaging / Consumable-like | `CONTEXTUALLY COVERED BY Material` | 调查未发现独立库存对象命名空间；目前作为 Material 用途归类，不作为独立 Identity candidate。 |
| Asset | `SEPARATE DOMAIN / NO inventory evidence` | 未发现 Inventory 余额以 Asset 为 referent。 |
| Unidentified text-only row | `UNKNOWN REFERENT CLASS` | TraceCode 路径可产生仅文本无 ID referent 的 Inventory 行；业务所指待 Owner N1。 |

本候选集是受控工程调查候选集，不等于 Required Identity Object Set。

### 4.3 Balance grain

调查结果支持以下工程粒度模型：

- 中央/仓库余额主键模式：`(materialId, warehouseId)`，主要通过代码查询 `getByMaterialAndWarehouse` 实现。[CODE]
- 门店余额主键模式：`(storeId, materialId)`；存在 `uk_store_inventory_store_material` 唯一索引。[DB-SCHEMA][CODE]
- Batch 未表现为余额主粒度；调查结果称重复入库会覆盖而非形成稳定 batch grain。[CODE]
- 中央侧缺少 DB 唯一约束，粒度主要由代码约定执行。[DB-SCHEMA][CODE]

因此，当前最强结论是：Inventory balance 的核心是“业务对象 × 空间”的持有关系，而非“batch × 业务对象”。但中央空间维度、门店同步模型及历史数据的完整性仍需独立审查。

### 4.4 Spatial semantics

调查结果将 `warehouse` / `store` 视为库存空间上下文，而不是业务对象 Identity 本身。

但存在重要实现事实：采购入库及调拨路径的代码注释明确把 `warehouseId` 用作 `storeId` 同步门店库存。这说明空间字段在工程实现中存在语义模糊，不能将当前列名直接等价为已经稳定的业务空间模型。

该问题影响余额粒度、门店对账与历史解释，但不自动形成新的 Identity candidate。

### 4.5 Dual-reference semantics

现有本地实例证明：同一 Inventory record 可以同时填充 `material_id` 与 `product_id`，且两者可指向不同对象。

当前事实：

- 无排他规则；
- 无一致性规则；
- 无 DB 约束阻止双填异指；
- Owner 尚未定义该状态是否业务合法。

因此双列异指的业务性质只能标记为：

`BUSINESS LEGALITY = UNRESOLVED`

“测试污染”与“Material 时代前遗留”均只能作为成因 hypothesis，不得写成事实。

### 4.6 Candidate relationship matrix

| Relationship | Current status | Rationale |
|---|---|---|
| Material ↔ Inventory | `CONTEXT-SPECIFIC / STRONG ENGINEERING SUPPORT` | 当前活跃路径和本地数据均强支持。 |
| Product ↔ Inventory | `UNRESOLVED` | 历史结构强、当前活跃持有行为弱；业务真相未裁决。 |
| Food ↔ Inventory | `DISTINCT / NO-EVIDENCE IN inventory` | 既有证据指向独立 Food stock 维度；未发现 Inventory referent 路径。 |
| Semi-finished ↔ Inventory | `UNRESOLVED` | 工程无实现不等于业务不存在。 |
| Text-only trace row ↔ Inventory | `UNRESOLVED` | 事实存在，业务所指未知。 |
| Product ↔ Material | `CONTEXT-SPECIFIC / UNRESOLVED` | 数值/字段存在跨域映射与兼容别名，但尚无业务规则证明 SAME。 |

## 5. Behavior-path findings

### 5.1 Create / initialize

调查报告列出的主要创建来源包括：

- MaterialArchive 建档自动建零库存；
- 采购/入库相关 Material 路径；
- TraceCode 扫码创建文本行；
- 其他库存初始化/门店同步路径。

其中 MaterialArchive 路径为 Material → Inventory 的最强系统性创建耦合；TraceCode 路径是最重要的反例，因为其不提供稳定 ID referent。

### 5.2 Increase / inbound

采购入库以 `materialId` 作为主要 referent；库存空间与门店同步存在 `warehouseId → storeId` 的代码约定。

### 5.3 Deduction / outbound

调查将损耗、出库、调整、作废、退货、追溯出库等路径归入 Material-keyed deduction；SalesOrder 是需单独核查的例外。

### 5.4 Consumption / recipe

`MaterialConsumptionServiceImpl.deductInventory` 当前被调查为非功能性数量扣减路径：只设置 flag，不减少余额；没有有效创建调用方且本地数据为 0 行。因此此前把该路径作为 C-002 E4 正向支持的证据应 `SUPERSEDE` 为 `NON-FUNCTIONAL`，而不是继续计入正证明。

### 5.5 SalesOrder

SalesOrder 扣减使用销售域 product 值并依赖 transient storeId 定位 Inventory；调查未完成运行期具体列解析闭合。因此该路径保持 `UNRESOLVED`，不得用于无条件 Material truth claim。

## 6. Counter-evidence / Negative evidence

最强反证不是“product_id 有一列”本身，而是以下组合：

1. Inventory 初始迁移以 Product 形态创建，并持续保留指向 Product 的唯一 DB FK。[MIGRATION][DB-SCHEMA]
2. 当前 Material 写入的部分可靠性依赖 ORM compatibility alias，而不是一致、明确的 namespace contract。[CODE]
3. TraceCode 可以创建没有任何 ID referent 的 Inventory 行，因此并非所有 Inventory 行都能直接归入 Material 或 Product。[CODE]
4. SalesOrder 使用 Product 值进入 Inventory lookup，具体 referent 未闭合。[CODE]
5. 双列异指实例已实际存在，且当前没有业务规则解释该状态。[LIVE-REPRO]

这些反证足以阻止“Material 是唯一无争议 Inventory business truth”的过强表述，但不足以证明 Product 是正式库存对象。

## 7. Contradiction ledger

| ID | Contradiction | Status | Impact |
|---|---|---|---|
| C1 | `material_id` 活跃使用 vs `product_id` historical FK | `UNRESOLVED` | Owner Q1/Q3 |
| C2 | ORM Product accessors alias to Material | `RESOLVED AS COMPATIBILITY FACT; SEMANTICS UNRESOLVED` | Product referent interpretation |
| C3 | Dual-filled same row points to different objects | `UNRESOLVED` | Owner Q1/Q3 |
| C4 | Trace-created inventory row has no ID referent | `UNRESOLVED` | Owner N1 |
| C5 | `warehouseId` used as `storeId` in code comments | `ENGINEERING FACT; BUSINESS IMPACT OPEN` | Spatial/balance semantics |
| C6 | `material_consumption` path does not perform quantity deduction | `SUPERSEDED → NON-FUNCTIONAL` | C-002 E4 evidence removal |
| C7 | SalesOrder product-valued lookup into Inventory | `UNRESOLVED` | Owner N2 / further Engineering evidence |
| C8 | `purchase_request_item.food_id` has 4/50 anomalies | `UNRESOLVED / BLOCKING` | C-002 E2 |
| C9 | Entity transient fields not persisted | `RESOLVED AS ENGINEERING FACT` | Store/type evidence quality |
| C10 | Product-first → Material-added migration | `FACT`; motive `INFERENCE` | Historical interpretation |
| C11 | Some Mapper/entity references do not align with physical schema | `ENGINEERING DRIFT` | Historical semantics / evidence quality |

## 8. Existing-conclusion revalidation

| Existing conclusion | Disposition | Reason |
|---|---|---|
| Material Inventory already exists | `KEEP, NARROW` | Engineering evidence strongly supports Material-keyed inventory behavior, but phrase as engineering-supported referent rather than unique business truth. |
| Product Inventory is a current business object | `UNRESOLVED` | Historical/product FK evidence remains; active holding behavior not proven. |
| `inventory.product_id` carries canonical business meaning | `INVALIDATE AS UNCONDITIONAL CLAIM` | Current evidence shows sparse fill, legacy FK and alias behavior; no stable business rule. |
| `inventory.material_id` carries Inventory referent semantics | `KEEP, NARROW` | Strong active-path evidence, but not yet sole business truth. |
| Dual-column coexistence is benign/valid | `INVALIDATE` | No business legality rule; dual-fill/different-target instance exists. |
| Stable Material/Product classification rule exists | `INVALIDATE` | Investigation reports `NO_STABLE_RULE`. |
| Inventory ↔ Store Inventory / Inventory Transaction existing relation | `KEEP, NARROW` | Relation exists in engineering behavior; exact business spatial semantics require further Owner / evidence closure. |
| MaterialConsumption is a functional inventory deduction proof | `SUPERSEDE` | Investigation found flag-only/non-quantity-changing implementation and 0 local rows. |
| Inventory rows are always ID-resolved to Material/Product | `INVALIDATE` | TraceCode path can create text-only rows with no ID referent. |
| Batch is part of primary Inventory balance grain | `INVALIDATE AS CURRENT EVIDENCE` | Current investigation found overwrite behavior rather than stable batch balance grain. |

## 9. Owner Decision Inputs

Existing `d-ig-c002-inventory-business-truth-owner-decision-request-001.md` remains the primary Owner request and retains Q1/Q2/Q3.

本次 Reconstruction 新增最小两项：

### N1 — Text-only Trace Inventory row 的业务所指

TraceCode 扫码路径可创建没有任何 stable ID referent、仅携带文本名称的 Inventory 行。请 Owner 说明该记录在业务上代表：

- 已知库存对象但 ID 暂时缺失；
- 一种独立业务对象/库存事项；
- 历史/异常记录；
- 其他正式业务语义。

该问题不要求 Owner 设计字段，仅要求定义 business referent。

### N2 — SalesOrder 出账的库存对账对象

SalesOrder 扣减路径使用销售域 Product 值进入 Inventory lookup。请 Owner / Product Owner 明确：

> 销售完成造成库存变化时，业务上必须与哪一个库存持有对象/余额维度完成对账？

本问题用于判定业务 referent，不要求 Owner 选择表、列或技术实现。

## 10. Requirement-only / Engineering-only Gaps

以下事项不适合塞入 Owner Decision：

- SalesOrder 运行期具体列解析及实际 lookup 成功条件；
- 4/50 `purchase_request_item.food_id` 异常的逐行 referent 归属；
- 生产侧 `product_id` 是否仍有活跃写入；
- warehouse/store 主数据对同步模型的实际影响；
- `inventory_losses` 等旁路实体/表的最终存在与业务接线；
- legacy Mapper 对物理 schema 的漂移清理。

其中生产证据在当前阶段继续 `BLOCKED`。

## 11. Candidate re-entry analysis

本次重构未识别出新的、尚未被现有 candidate framework 覆盖的 Identity-grain candidate，因此：

`candidate_re_entry = NOT_TRIGGERED`

但登记以下触发条件：

- T1：Owner Q1 选择“两类库存对象都合法”且给出稳定分类规则；
- T2：生产证据显示 `inventory.product_id` 仍存在活跃业务写入；
- T3：Requirement / Business Owner 正式确认半成品或其他第三对象需要独立身份并有独立库存持有行为；
- T4：SalesOrder 路径被运行期证据证实为独立、稳定的另一库存 referent。

触发任一条件时，不得强制并入 C-001/C-002，应按 candidate re-entry 治理流程重新登记。

## 12. C-002 Impact

C-002 Material 当前保持：

`CANDIDATE_PASS_NOT_YET_PROVEN`

Inventory E2/E4 segment：

`CONDITIONAL / BLOCKING`

原因：

- Material 支持很强，但并非业务唯一真相；
- Product 的历史/结构反证未闭合；
- 双列异指业务合法性未闭合；
- Text-only Inventory row 的 business referent 未闭合；
- SalesOrder referent 未闭合；
- 4/50 purchase_request_item anomalies 仍独立阻塞 C-002；
- MaterialConsumption 正向证据已降级为 `NON-FUNCTIONAL`。

因此本报告不执行 C-002 E2/E4 PASS，也不预设下一次重评估结果。

## 13. Completion Criteria Cross-check

依据 `d-ig-inventory-semantic-reconstruction-task-001.md` 的**正式 11 项 completion criteria**，本调查提交结果逐项自报：

| # | Criterion | Investigator status | Governance note |
|---|---|---|---|
| 1 | Candidate-set exhaustion | PASS | 受控枚举完成；未覆盖的替代 grain 有命名出口。 |
| 2 | Behavior coverage | PASS | 创建/增加/扣减/查询及关键外围路径已调查；SalesOrder 等剩余项显式命名。 |
| 3 | Evidence coverage per candidate | PASS | 每候选有支持、反证或 NO_EVIDENCE。 |
| 4 | Relationship closure | PASS | 已使用 `SAME/DISTINCT/HIERARCHICAL/CONTEXT-SPECIFIC/UNRESOLVED`。 |
| 5 | Balance-grain closure | PASS | 工程证据支持对象×空间粒度，保留空间语义限制。 |
| 6 | Historical grounding | PASS | Migration facts 与 business interpretation 分离。 |
| 7 | Contradiction closure | PASS | 关键矛盾登记为 resolved / unresolved / superseded。 |
| 8 | Existing conclusion revalidation | PASS | 已完成 KEEP/NARROW/INVALIDATE/SUPERSEDE/UNRESOLVED 重评估。 |
| 9 | Evidence stop condition | PASS | 普通继续搜索不会改变现有方向；剩余问题已进入 Owner/PROD/blocking lanes。 |
| 10 | Minimal Owner question set | PASS | 原 Q1/Q2/Q3 + 新 N1/N2；不要求工程方案。 |
| 11 | Decision-boundary readiness | PASS | Owner / Engineering / D-IG 后续分析边界已区分。 |

> **Correction against investigator wording:** 调查者摘要称“§20 全部 13 项条件”。正式任务 001 只有 11 项 completion criteria，本记录以任务 001 的 11 项为准；调查者额外列出的检查项可以作为内部自检，但不改变治理门槛。

## 14. Post-Reconstruction Governance Gate

按任务 001 的强制顺序，下一步必须是：

1. 独立反方审查本 Reconstruction 方法、覆盖度、候选完整性、数据污染风险、migration interpretation、referent interpretation、quantifier discipline；
2. 若独立审查 = `PASS` / `PASS_WITH_REPAIRS`，才允许进入 Owner Q1/Q2/Q3/N1/N2 裁决准备；
3. Owner 裁决完成后，4/50 anomaly lane 与其余 Engineering evidence lane 继续闭合；
4. 然后再做 aligned C-002 E2/E4 re-evaluation；
5. candidate-level adversarial re-review；
6. 最终 candidate verdict。

若独立审查 = `REOPEN_RECONSTRUCTION` 或 `BLOCKED`，则暂停 Owner Decision 与 C-002 E2/E4 re-evaluation。

## 15. Non-Goals Remain

本记录不授权：

- Identity Decision；
- Required Identity Object Set；
- H1/H2；
- Schema redesign；
- migration change；
- code refactor/fix；
- data repair；
- production status inference。

## 16. Final Investigator Summary

### Strongest supporting evidence

全部主要活跃库存写路径大体围绕 Material-keyed referent 运行，且 MaterialArchive 建档自动创建库存行；本地 Inventory 的 `material_id` 11/11 填充，是当前最强工程支持。[CODE][LIVE-REPRO]

### Strongest counter-evidence

Inventory 以 Product 形态诞生、保留 Product FK；Material 当前真相地位部分依赖兼容 alias；双列可异指；TraceCode 可形成 text-only row；SalesOrder 使用 Product-valued lookup。这些共同阻止无条件“Material 唯一真相”结论。[MIGRATION][DB-SCHEMA][CODE][LIVE-REPRO]

### Residual uncertainty

当前调查出口至少包括：

- `[PROD]` 生产侧全部未验证；
- SalesOrder 运行期具体 lookup；
- `purchase_request_item` 4/50 anomalies；
- dual-filled record 的形成时代与业务合法性；
- warehouse/store master-data 影响；
- food_code → Food ID 映射稳定性（C-001 lane）；
- inventory_losses 等旁路证据是否仍构成独立业务路径。

这些不确定性已经命名并分别进入 Owner、Engineering、PROD 或 C-002 blocking lane，不以继续普通代码搜索替代治理门禁。
