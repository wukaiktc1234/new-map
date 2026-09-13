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

这些外部领域只在其与 Inventory referent 直接相关时进入调查，不要求重建整个外围领域。

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

### 4.1 Evidence vs interpretation rule

所有输出必须显式区分：

```text
Engineering Fact
    ≠
Governance Interpretation
    ≠
Business Hypothesis
    ≠
Business Decision
    ≠
Identity Conclusion
```

调查者不得因为某项工程事实“看起来合理”而直接将其升级为业务语义。

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

调查完成后，调查者必须明确列出：

- 支持自身当前解释的 strongest evidence；
- 最强反证；
- 无法消除的 residual uncertainty。

## 6. Existing Conclusion Revalidation

对现有相关结论逐条给出：

```text
KEEP
NARROW
INVALIDATE
SUPERSEDE
UNRESOLVED
```

至少覆盖：

- Material Inventory 已存在；
- Product Inventory 作为业务对象的当前状态；
- `inventory.product_id` 的业务语义；
- `inventory.material_id` 的业务语义；
- 双列同存 / 异指；
- “当前存在稳定 Material/Product 区分规则”的结论；
- Inventory 与 Store Inventory / Inventory Transaction 的既有关系结论。

重新评价不得因为“已有文档”而自动 KEEP，也不得因为发现冲突而自动 INVALIDATE；必须给出证据理由。

## 7. Required Deliverable

产出：

`03-review/d-ig-inventory-semantic-reconstruction-001.md`

该文档必须按三层组织：

### Layer 1 — Facts

- Inventory engineering reality；
- Inventory data reality；
- migration / historical facts；
- exact provenance / reproducible queries；
- fact-vs-inference boundaries。

### Layer 2 — Semantics

- Inventory business-object hypothesis set；
- Inventory holding-object candidates；
- behavior-path evidence；
- counter-evidence / negative evidence；
- candidate relationships：`SAME / DISTINCT / HIERARCHICAL / CONTEXT-SPECIFIC / UNRESOLVED`；
- balance grain；
- referent matrix；
- contradiction ledger。

### Layer 3 — Decision Inputs

- existing conclusion revalidation：`KEEP / NARROW / INVALIDATE / SUPERSEDE / UNRESOLVED`；
- remaining Owner Decision questions, minimized and non-leading；
- requirement-only gaps；
- candidate re-entry triggers；
- post-Reconstruction gate recommendation。

## 8. Completion Criteria

Reconstruction 只有在以下条件全部满足时才可标记 `COMPLETED`：

1. **Candidate-set exhaustion**：Inventory 持有对象候选集已完成受控枚举；对未纳入候选的明显替代粒度给出排除理由。
2. **Behavior coverage**：所有 materially relevant Inventory 写入、更新、读取及核心业务行为路径已调查，或明确记录不可覆盖的路径、原因和影响。
3. **Evidence coverage per candidate**：每个候选都能在至少一个相关业务行为中找到支持证据、反证或明确 `NO_EVIDENCE`，并给出 provenance。
4. **Relationship closure**：候选之间的关系已分类为 `SAME / DISTINCT / HIERARCHICAL / CONTEXT-SPECIFIC / UNRESOLVED`，不能留在隐含状态。
5. **Balance-grain closure**：余额记录粒度已有证据支持的解释，或正式登记为 `UNRESOLVED` 并说明具体缺口。
6. **Historical grounding**：migration 演化中的 observed facts 与 inferred interpretation 已严格分离；任何迁移动机推断必须标记为 inference。
7. **Contradiction closure**：所有直接影响 Inventory holding object、referent 或 balance grain 的矛盾都已逐项登记，并有 `RESOLVED / UNRESOLVED` 状态。
8. **Existing conclusion revalidation**：相关旧结论全部完成 `KEEP / NARROW / INVALIDATE / SUPERSEDE / UNRESOLVED` 重新评价。
9. **Evidence stop condition**：不存在仅靠继续扩大普通代码搜索就明显可以解决、且尚未调查的核心证据路径；其余无法解决的问题必须显式进入 `UNRESOLVED`，不能以无限调查代替结论。
10. **Minimal Owner question set**：剩余真正属于 Business Owner 的问题已经压缩到最小、互不重复、不预设答案且不夹带 Schema 实现方案。
11. **Decision-boundary readiness**：能够清楚指出哪些问题可以由 Owner 裁决，哪些必须继续 Engineering evidence，哪些属于后续 D-IG candidate analysis。

完成条件表示在**受控范围内足够闭合**，不是要求证明所有历史事实或所有业务语义绝对无误。

## 9. Explicit Boundaries / Non-Goals

本任务不：

1. 不重新执行 C-001 E1–E4；
2. 不执行 C-002 E1–E4 重评估（Reconstruction 后再做）；
3. 不决定 Material / Product / Food 哪个是 Identity；
4. 不创建或设计 Required Identity Object Set；
5. 不执行 H1/H2；
6. 不设计 Schema、Migration、`inventory_type`、canonical table / canonical column；
7. 不进行代码重构、修复或数据修复；
8. 不把 Inventory 之外的领域独立重建；Food / Recipe / Supplier / POS 等只在其与 Inventory referent / holding semantics 直接相关时进入；
9. 不把本地数据当作生产数据；Production Evidence 继续 `BLOCKED`；
10. 不把“调查发现的结构”直接写成业务真相；
11. 不因为范围较大而删减核心业务路径，也不因为发现新线索而无限制扩张到整个系统。

## 10. Independent Adversarial Review Gate

Reconstruction 标记 `COMPLETED` 后必须进入独立反方审查；该审查不得由 Reconstruction 调查者自行完成。

独立反方至少检查：

- method adequacy；
- path coverage adequacy；
- candidate-set completeness；
- data contamination risk；
- migration interpretation discipline；
- referent interpretation discipline；
- quantifier discipline；
- strongest counter-evidence 是否被公平处理；
- unresolved items 是否应阻塞 Owner Decision；
- 是否存在调查者将 engineering interpretation 偷换成 business semantics 的情况；
- 是否有结论超过证据范围。

独立反方输出至少给出：

```text
PASS
PASS_WITH_REPAIRS
REOPEN_RECONSTRUCTION
BLOCKED
```

若结果为 `REOPEN_RECONSTRUCTION` 或 `BLOCKED`，不得进入 Owner Decision 或 C-002 E2/E4 重评估。

## 11. Provenance Rules

所有结果必须区分：

- `[R-DOC]` repository governance/evidence document；
- `[CODE]` local source inspection；
- `[LIVE-REPRO]` local runtime/database reproduction；
- `[PROD]` production evidence，仅在真实生产证据可获得时使用。

没有 Production Evidence 时，不得写：`production verified`。

不得因 repository 文档与 local code 一致就自动升级为 production fact。

## 12. Relation to Existing Governance

### C-001 Food

当前 C-001 的 Inventory-related findings 不因本任务自动失效；若重建发现 Food/Inventory referent directly affects C-001 E4 scope，才建立受治理的 re-entry / targeted retest。

### C-002 Material

C-002 E2/E4 inventory segment 在本任务完成并通过独立反方审查前保持：

`PAUSED_PENDING_RECONSTRUCTION`

不得以现有 `material_id` 100% filling 单独推出 candidate-level PASS。

### C-003 / Product

若重建发现 Product 在业务上确实应是库存持有对象，应把该结论作为独立 Business Requirement / candidate input 处理，而不是把当前 `inventory.product_id` 直接升级为 Product Identity 事实。

### Required Set / H1/H2

继续保持 `NOT_STARTED / NOT_MADE`。重建结果只能作为后续候选、Owner、Requirement 输入。

## 13. Post-Reconstruction Gate

只有在 Reconstruction `COMPLETED` 且独立反方审查通过后，才能进入以下受控路径之一：

```text
Reconstruction COMPLETE
        ↓
Independent Adversarial Review
        ↓
┌────────────────────────────────────────────┐
│ A. Owner Decision                          │
│ B. Additional targeted evidence            │
│ C. New candidate / candidate re-entry      │
│ D. Reconstruction Phase 2 (if justified)   │
└────────────────────────────────────────────┘
        ↓
C-002 E2/E4 re-evaluation
        ↓
Candidate-level adversarial re-review
```

如果重建显示原来的问题定义本身错误，优先修订问题定义，而不是强行进入 C-002 原 E2/E4 复评。

## 14. Final Non-Decision

本任务本身不回答：

- Inventory 是否最终是 Material / Product / Food / 其他；
- 一条记录是否业务上允许多对象关联；
- Product 是否应该成为 Identity；
- Material 是否应该进入 Required Set；
- 是否应该采用 `inventory_type` 或任何其他具体工程实现。

这些都必须在证据闭合后的正确问题框架中再作 Owner / D-IG 决策。
