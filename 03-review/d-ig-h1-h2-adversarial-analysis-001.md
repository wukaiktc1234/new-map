# D-IG H1/H2 Adversarial Analysis 001

> **Status**: ACTIVE WORKING ANALYSIS — NOT A DECISION  
> **Decision**: D-IG  
> **Purpose**: 对 H1 Single Identity Grain 与 H2 Multiple Identity Layers 进行对称性攻击、反证测试与证据缺口识别。  
> **Decision Status**: OPEN  
> **Evidence Boundary**: LIVE_LOCAL_INSTANCE_EVIDENCE；Production Evidence Gate-0 = BLOCKED

---

## 1. Executive Conclusion

当前证据**不足以确认 H1 或 H2**。

本轮分析的工作结论为：

```text
H1 = PLAUSIBLE / NOT CONFIRMED
H2 = PLAUSIBLE / NOT CONFIRMED
Selected Hierarchy = NONE
Production-dependent conclusions = PROVISIONAL_PENDING_PRODUCTION_EVIDENCE
Schema / Migration = BLOCKED
```

目前最强的方向性判断是：**如果没有发现某个上层概念具有独立且不可压缩的 Identity 生命周期，H1 的解释力较强；如果能够证明两个不同抽象粒度的对象都必须被独立识别、独立治理，并存在稳定层间关系，则 H2 才获得成立基础。**

因此，D-IG 第一题不应从“设计几张表”开始，而应从“是否存在两个不可互相压缩的 Identity 业务概念”开始。

---

## 2. H1 的最强论证

### H1-A：单一可识别业务对象足以承载核心运营生命周期

**假设**：销售、采购、库存、Recipe、价格等核心操作最终都能引用同一个业务 Identity；其他差异通过 Commercial / Quantity / Holding / Tracking / Relationship / Attribute 表达。

**可观察预测**：
- 同一业务对象在不同 Domain 中仍可被稳定引用；
- Store / Warehouse 不需要产生新的 Identity；
- Batch / Lot 不需要产生新的 Identity；
- each / pack / case 不需要产生新的 Identity；
- 不同采购 UOM 不需要产生新的 Identity；
- Brand / Product Family 若存在独立治理，也可通过分类、属性或关系表达，而不需要另一个 Identity。

**当前证据**：A1 已确认 Identity 与 Quantity / Commercial / Inventory / Tracking / Transformation 分离；本地库存结构与采购结构显示存在大量“同一对象在不同业务上下文中的不同表现”。

**反证条件**：发现某一上层对象必须在不依赖下层对象的情况下拥有自己的稳定身份、生命周期与治理边界，且用 Classification / Relationship / Attribute 无法无损表达。

**状态**：PLAUSIBLE。

### H1-B：each / pack / case 更像 Commercial / Packaging 关系，而非 Identity 层级

**假设**：包装层级改变数量、商业包装关系或物流表达，但不必然创造新的业务 Identity。

**预测**：
- 同一核心业务对象可以关联多个包装层级；
- 包装层级可以改变，而核心 Identity 不变；
- 包装关系可以独立维护。

**反证**：如果某些包装层级必须独立定价、独立销售、独立追踪、独立合规，并且这种独立性不能由 Commercial Unit / Relationship 表达，则需重新评估。

**状态**：PLAUSIBLE，不能直接推出 H1 已成立。

### H1-C：Store / Warehouse / Batch / Lot 不构成 Identity 的充分理由

这些概念分别更接近 Holding / Location / Tracking 语义。即使它们对库存业务重要，也不能仅因为它们有独立记录就升级为 Identity。

**反证**：只有当某一 Location / Batch / Lot 本身成为独立业务对象，并且其他 Domain 必须以其作为稳定身份引用时，才可重新进入 H2 论证。

**状态**：STRONG AGAINST FALSE-H2，但不是 H1 的充分证明。

---

## 3. H1 的最强攻击点

H1 最大风险不是“无法建表”，而是**语义压缩**。

### H1-F1：如果上层概念具有独立生命周期，H1 会把它压成属性

例如存在一个“品牌/产品家族”对象，其合同、合规、促销、治理或主数据生命周期独立于具体 Variant / SKU：

```text
Upper Concept
    ↓ independent lifecycle
Lower Identity
```

如果上层对象可以被独立创建、停用、治理、授权、关联合同，并且下层可以在不改变上层 Identity 的情况下新增/删除/替换，那么强行 H1 可能造成语义压缩。

**需要的证据**：真实业务规则、合同/促销/合规规则、Owner 访谈或生产系统数据。

### H1-F2：多供应商共享一个业务概念可能暴露粒度问题

若供应商 A/B 都提供同一个业务对象，但供应商代码、包装、采购条件不同，需要判断：

- 这些差异是 Supplier Relationship / Commercial Unit；还是
- 它们实际上代表两个必须独立识别的 Identity。

仅凭“供应商不同”不能证明 H2。

### H1-F3：External Integration 不能直接证明 H2

ERP / POS / WMS 的多代码结构可能只是 Integration Mapping / External Identifier，而不等于本项目必须采用多 Identity Grain。

只有当外部系统的两个粒度都对应本项目真实、不可压缩的业务概念时，才构成 H2 证据。

---

## 4. H2 的最强论证

### H2-A：存在上层 Identity 与下层 Identity 的独立生命周期

**假设**：业务世界中确实存在两个粒度不同、且都需要稳定身份的对象。

**必要条件**：
1. 上层可独立存在；
2. 下层可独立存在；
3. 上层与下层之间有稳定关系；
4. 上层生命周期变化不必然改变下层 Identity；
5. 下层变化不必然改变上层 Identity；
6. 上层不能仅通过属性、分类或普通关系无损表达。

**状态**：这是 H2 最强、也是最严格的成立条件。

### H2-B：独立治理本身必须是“Identity 必要性”，而不是“Role 必要性”

例如品牌有自己的促销规则，并不足以证明品牌是 Identity Layer；促销规则可能只是对 Brand Role / Classification 的治理。

因此必须继续问：

> 如果删除具体下层对象，是否仍需要在系统中独立引用这个上层对象？

如果答案是“需要”，H2 证据增强。

### H2-C：稳定层间关系必须可验证

不能仅凭“看起来像 Product → SKU”就宣布 H2。

至少需要观察：
- 一个上层对应多个下层；
- 下层归属具有稳定规则；
- 关系本身具有业务意义；
- 关系不是一次性 ETL 映射；
- 关系不是单纯分类标签。

**状态**：当前本地证据不足。

---

## 5. H2 的最强攻击点

### H2-F1：把 Classification 升级成 Identity

Brand / Product Family / Product Line 等概念可能非常重要，但“重要”不等于“Identity”。

如果它们只是：
- 分类；
- 聚合维度；
- 报表维度；
- 权限维度；
- 促销规则维度；
- 组织治理维度；

那么 H2 是过度建模。

### H2-F2：把 Commercial Unit 升级成 Identity

Pack / Case / Each 可以拥有自己的条码、价格或采购关系，但这些差异可能属于 Commercial Unit / Packaging hierarchy，而非 Identity Grain。

H2 必须证明“Commercial Unit 本身就是不可压缩的业务 Identity”，而不能仅证明“Commercial Unit 有独立属性”。

### H2-F3：把 Inventory Holding / Batch / Location 升级成 Identity

Store / Warehouse / Batch / Lot 的存在不能自动产生新的 Identity Layer。

如果 H2 的第二层只是：

```text
Identity + Location
Identity + Batch
Identity + Packaging
```

则更可能是在把 Holding / Tracking / Commercial 语义错误提升为 Identity。

---

## 6. 十个必测业务案例的当前裁决边界

| Case | 当前最强解释 | 是否足以证明 H2 |
|---|---|---|
| 330ml vs 500ml | 可能是不同 Identity，也可能是同一上层概念下的不同 Variant / Commercial expression | 否 |
| each / pack / case | 更可能是 Commercial / Packaging relationship | 否 |
| 不同采购单位 | 更可能是 UOM / Commercial / Supplier context | 否 |
| Store / Warehouse | 更可能是 Holding / Location | 否 |
| Batch / Lot | 更可能是 Tracking | 否 |
| 不同 Recipe usage | 更可能是 Relationship / Transformation context | 否 |
| Product / Food / Material | A2 已禁止其作为竞争性 Canonical Identity Namespace | 否 |
| Sellable + Purchasable | Capability / Role | 否 |
| Sellable + Recipe input | Capability + Relationship | 否 |
| Role 随 Organization / Temporal 改变 | Role Scope | 否 |

特别注意：**330ml vs 500ml 是 D-IG 的压力测试案例，但不是 H2 的自动证明。**

---

## 7. 当前本地证据能证明什么、不能证明什么

### 能证明

1. `foods` 与 `material_archives` 当前承担不同的真相源角色；
2. `inventory` 与 `store_inventory` 的持有结构存在差异；
3. `purchase_request_item.food_id` 存在 namespace semantic drift；
4. `product` 存在 active-code residue；
5. 当前系统确实存在多个业务上下文引用相似对象的现实问题。

### 不能证明

1. Product / Food / Material 的最终 Identity Grain；
2. Brand / Family / Variant / SKU 是否构成 Identity 层级；
3. 330ml 与 500ml 是否必须属于不同 Identity；
4. each / pack / case 是否拥有独立 Identity；
5. H1 或 H2 哪一个最终正确；
6. 生产环境是否存在与本地不同的业务规则。

---

## 8. 能真正证伪 H1 的证据

以下任一证据成立，都会显著削弱 H1：

### Falsifier-H1-01
存在一个上层业务对象，在没有任何下层对象时仍需要被独立创建、引用、治理或停用。

### Falsifier-H1-02
上层对象与下层 Identity 的生命周期可以独立变化，而且这种关系不是简单 Classification / Relationship。

### Falsifier-H1-03
真实业务流程明确要求同时引用“上层 Identity”和“下层 Identity”，且二者承担不同的不可替代业务语义。

### Falsifier-H1-04
本地/生产数据中存在稳定的一对多 Identity hierarchy，并被多个 Domain 直接引用。

---

## 9. 能真正证伪 H2 的证据

以下证据会显著削弱 H2：

### Falsifier-H2-01
所有所谓“上层 Identity”都可以被唯一稳定的 Classification / Attribute / Relationship 表达。

### Falsifier-H2-02
所谓层间关系只是 External ID mapping、报表聚合或一次性 ETL 关系。

### Falsifier-H2-03
所有所谓第二 Identity Layer 都实际上对应 Commercial Unit、UOM、Holding、Tracking 或 Role。

### Falsifier-H2-04
业务 Owner 无法给出任何需要独立管理上层对象的真实流程，且生产/本地证据也无法观察到该层级。

---

## 10. 需要补充的最小证据集

D-IG 不需要马上做全库扫描。第一轮应只补足能够区分 H1/H2 的证据：

### E1 — 生命周期证据

针对 Brand / Family / Variant / SKU 等候选概念，确认是否存在独立 Create / Activate / Suspend / Retire / Governance 流程。

### E2 — 独立引用证据

确认业务流程是否在没有下层对象时仍需要引用上层对象。

### E3 — 独立变化证据

确认上层与下层是否可以分别变化而保持另一层 Identity 不变。

### E4 — 稳定关系证据

确认是否存在稳定、可验证的层间关系，而不是报表/ETL 映射。

### E5 — 生产证据

若生产环境可获得，只扫描能够验证 E1-E4 的最小范围；否则结论继续标记 provisional。

### E6 — Owner 决策事实

业务 Owner 需要回答的不是“你喜欢 H1 还是 H2”，而是：

> 是否存在两个业务上都必须保留、且不能通过 Attribute / Classification / Relationship 压缩为一个 Identity 的对象概念？

---

## 11. 临时结构试建规则

允许为 H1/H2 各建立最小语义草图，例如：

```text
H1:
Identity
 ├─ Commercial expression
 ├─ Quantity / UOM
 ├─ Holding
 └─ Tracking
```

```text
H2:
Upper Identity
      │
      └── Lower Identity
             ├─ Commercial expression
             ├─ Quantity / UOM
             ├─ Holding
             └─ Tracking
```

这些只是**比较工具**，不是 Canonical Model、Schema Proposal 或 Implementation Plan。

---

## 12. Current Decision Boundary

本分析不选择 H1/H2。

下一步应先完成 E1-E4 的最小证据收集，再进行一次 Owner-facing H1/H2 decision review。

在此之前：

- Identity Resolution Matrix = NOT STARTED；
- Canonical Business Item Model = BLOCKED；
- Schema / Migration = BLOCKED；
- Production-dependent conclusions = PROVISIONAL_PENDING_PRODUCTION_EVIDENCE。

最终 D-IG CONFIRMED 前仍必须满足 Charter §11，包括独立对抗性审查并留下正式审阅记录。
