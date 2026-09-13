# D-IG — Inventory Semantic Reconstruction Task 001

> **状态**: ACTIVE_REFERENCE — RECONSTRUCTION TASK
> **Decision ID**: D-IG
> **Scope**: 仅针对 Inventory 业务语义、库存持有对象、余额记录粒度、对象关联及跨业务行为关系进行证据重建。
> **Identity Decision**: NOT_MADE
> **Required Identity Object Set**: NOT_STARTED
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO
> **Production Evidence**: BLOCKED

## 1. Purpose

本任务由 `INVENTORY-BUSINESS-TRUTH-RECON-001` Engineering Evidence Pack 触发。

目的不是证明既有 `Material Inventory` / `Product Inventory` 假设，也不是直接裁决 Identity，而是从业务行为、工程现实、数据、迁移历史和既有治理证据重新构建 Inventory 的语义边界，使后续 Business Owner Decision 建立在经过审查的业务问题框架上。

本任务采用“既有结论重新验证”而非“全部历史结果作废”的方式：每项既有结论最终必须归入 `KEEP / NARROW / INVALIDATE / SUPERSEDE / UNRESOLVED` 之一。

## 2. Trigger Findings

已提交 Recon Evidence Pack 已显示：

- `inventory.material_id` 存在完整的本地代码写入路径，活跃数据填充 11/11；
- `inventory.product_id` 自初始迁移即存在，但当前只有 1/11 填充，且实体层 `getProductId/setProductId` 实际别名到 `material_id`；
- 同一 inventory record 已出现 `material_id` 与 `product_id` 同时填充且分别指向不同对象的实例；
- 当前未找到稳定的 Material-vs-Product inventory 业务区分规则；
- inventory identity columns 在迁移历史中存在从 Product 形态到追加 Material 语义的演变；
- 部分 Mapper 查询与当前物理列不一致，显示历史语义/实现漂移。

以上只作为重建触发事实，不自动升级为业务真相。

## 3. Reconstruction Questions

### 3.1 Inventory Business Object

回答：Inventory 在业务上到底是什么业务概念？

不得从表名、字段名或当前 Entity 名称直接推导。

### 3.2 Inventory Holding Object Set

识别所有可能被库存余额正式持有的业务对象候选，至少检查：

- Material
- Product
- Food
- 半成品 / 中间品（如有）
- 其他实际被库存余额持有的业务对象

对每个候选必须给出证据与状态，不得因为当前缺少代码实现而静默排除。

### 3.3 Inventory Balance Grain

回答一条 inventory balance record 在业务上代表什么：

- 单一业务对象余额；
- 多对象复合持有；
- 其他稳定业务粒度。

重点审查同一 record 多列同时指向不同对象的业务含义，不从当前数据库允许双填自动推出业务合法性。

### 3.4 Quantity / UOM Semantics

确认库存数量、单位、批次、效期等属性究竟附着在哪个业务对象或库存持有关系上；只记录可证实的业务语义，不提出 Schema 设计。

### 3.5 Business Behavior Paths

从以下关键行为验证 Inventory 所持对象及其 referent：

- procurement / purchase request / purchase order
- receipt / inbound
- inventory balance / stock ledger
- transfer
- count / adjustment
- material consumption / recipe
- POS deduction / sales completion
- loss / internal consumption / tasting / damage
- traceability

不要求把整个系统重新审计一遍；只调查能改变 Inventory 对象语义或 referent 判断的行为路径。

### 3.6 Spatial Boundary

确认 `inventory`、`store_inventory`、warehouse/store context 的业务关系；区分“空间/仓位”与“业务对象身份”，不得把二者混为一谈。

### 3.7 Historical / Migration Semantics

审查 Product-first → Material-added 的迁移链及其业务含义：

- 哪些是历史技术实现；
- 哪些可能代表业务模型变化；
- 哪些只是兼容/过渡；
- 哪些结论目前无法从迁移历史单独推出。

迁移历史只能提供时间/结构证据，不能单独裁决当前业务真相。

### 3.8 Cross-namespace / Referential Conflicts

系统性检查 `product_id`、`material_id`、`food_id` 等命名在不同上下文中的实际 referent，建立 contradiction ledger，避免同名字段/数值相等被当成同一对象。

## 4. Evidence Method

证据优先级：

1. 可复现业务行为与数据事实；
2. 代码 + migration + DB schema 的一致性证据；
3. 已提交治理/Owner 记录；
4. 历史记录与审计证据；
5. 命名、字段名、注释只能作为弱辅助证据。

每一项关键结论必须记录：

- exact file / class / method；
- exact table / column；
- migration/version；
- representative records / query；
- behavior path；
- provenance；
- contradiction / negative evidence；
- confidence / evidence strength。

Production Evidence 仍为 `BLOCKED`。本地实例观察必须保持 `LIVE_LOCAL_INSTANCE` / local provenance，不得写成生产事实。

## 5. Adversarial Requirements

调查必须主动寻找能够推翻自身假设的证据，包括：

- Product 是否确实没有独立 inventory business behavior；
- Material 是否存在未覆盖的 alternative referent；
- Food 是否直接作为 inventory holding object；
- 是否存在第三种 inventory object grain；
- 是否存在一条合法业务路径允许一条 balance record 关联多个对象；
- 是否存在现有稳定分类规则但未被前次调查发现；
- 是否存在测试数据污染、历史残留、迁移中间态导致的假象。

不得以“Owner 倾向某结论”为调查前提。

## 6. Prohibited Outputs

本任务不：

- 决定 Material / Product / Food 是否最终 Identity；
- 决定 Required Identity Object Set；
- 设计 `inventory_type` 或其他新字段；
- 指定 canonical table / canonical column；
- 设计 Schema / Migration；
- 授权代码修复；
- 把工程现实直接升级为业务真相。

## 7. Required Deliverable

产出：

`03-review/d-ig-inventory-semantic-reconstruction-001.md`

该文档至少包含：

1. Executive Semantic Summary；
2. Inventory Business Object hypothesis set；
3. Inventory Holding Object candidates and evidence status；
4. Inventory Balance Grain analysis；
5. Behavior-path evidence matrix；
6. Material / Product / Food referent matrix；
7. Migration / historical interpretation；
8. Contradiction / negative-evidence ledger；
9. Existing conclusion revalidation matrix (`KEEP / NARROW / INVALIDATE / SUPERSEDE / UNRESOLVED`)；
10. Owner Decision Questions, limited to propositions that remain genuinely business-owned after evidence reconstruction；
11. Open boundaries and candidate re-entry triggers；
12. Explicit non-decisions。

## 8. Acceptance Criteria

任务不得以“找到了更多代码”作为完成标准。必须达到：

- Inventory business object has a bounded semantic proposition；
- all materially relevant holding-object hypotheses are explicitly listed；
- current evidence can distinguish fact / engineering interpretation / business hypothesis；
- `inventory.product_id` history and current semantics are separated；
- same-row dual reference is classified as fact first, business legality only as Owner-owned proposition；
- no stable classification rule is either invented or assumed；
- any unresolved ambiguity has named evidence gap and owner/next action；
- C-002 downstream E2/E4 impact is explicitly stated；
- no Schema / Migration / Identity decision is smuggled into the reconstruction.

## 9. Relation to Existing Governance

### C-001 Food

当前 C-001 的 Inventory-related findings不因本任务自动失效；若重建发现 Food/Inventory referent directly affects C-001 E4 scope，才建立受治理的 re-entry / targeted retest。

### C-002 Material

C-002 E2/E4 inventory segment 在本任务完成前保持 `CONDITIONAL / BLOCKING`；不得以现有 `material_id` 100% filling 单独推出 candidate-level PASS。

### C-003 / Product

若重建发现 Product 在业务上确实应是库存持有对象，应把该结论作为独立 Business Requirement / candidate input 处理，而不是把当前 `inventory.product_id` 直接升级为 Product Identity 事实。

### Required Set / H1/H2

继续保持 `NOT_STARTED / NOT_MADE`。重建结果只能作为后续候选/Owner/Requirement 输入。

## 10. Exit Conditions

本任务完成后必须产生以下明确出口之一：

```text
A. Inventory semantic boundary sufficiently reconstructed
   → proceed to Owner Decision on residual business propositions

B. Materially unresolved domain boundary discovered
   → expand to governed Inventory Domain Reconstruction phase 2

C. A new identity-grain candidate is discovered
   → trigger candidate re-entry into D-IG

D. Evidence remains insufficient
   → retain named gaps; no forced business conclusion
```

任何出口均不得直接授权 Schema / Migration / Identity Decision。
