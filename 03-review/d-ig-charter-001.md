# D-IG — Identity Grain Decision Charter 001

> **状态**: OPEN — DECISION CHARTER  
> **Decision ID**: D-IG  
> **依赖**: DEC-BI-001 (A1), DEC-BI-002 (A2)  
> **启动日期**: 2026-09-12  
> **性质**: Business Semantic Decision  
> **禁止事项**: 本 Charter 不授权 Schema、Migration、Canonical Table Creation 或 Identity 数据迁移。

---

# 1. Purpose

D-IG 的目标是确定业务 Identity 的粒度与层级结构，为后续 Canonical Business Item Model 提供唯一、显式、可追溯的 Identity Grain 定义。

D-IG 不负责直接选择数据库表，也不负责迁移现有 `product`、`foods`、`material_archives` 数据。

---

# 2. Identity Hierarchy Question — FIRST DECISION

D-IG 第一项必须回答：

> **业务世界是否需要多个合法的 Identity 层级？**

## H1 — Single Identity Grain

整个业务模型使用一个 Identity Grain。

如果选择 H1，必须明确该 Grain 的名称、边界、Identity-defining attributes，以及非 Identity attributes。

## H2 — Multiple Identity Layers

业务模型需要两个或多个合法 Identity 层级。

如果选择 H2，必须明确：

- 层级数量；
- 每层名称；
- 每层语义边界；
- 层间关系；
- 哪些属性定义各层 Identity；
- 哪些层级允许跨 Domain / Organization / Temporal 使用。

### 2.1 Mandatory Sequence

在 §2 未完成前，不得正式填写 §5.2 Identity Resolution Matrix。

D-IG Kickoff 的顺序必须为：

1. H1 vs H2；
2. 若 H1，确定单层名称与边界；
3. 若 H2，确定层级数量、层名、层间关系；
4. 完成后，才进入 Identity Resolution Matrix。

在 H1/H2 判断阶段，允许临时构建最小结构示例，用于比较两种模型的表达能力；但不得把临时试建结构写成 Decision、Canonical Model 或 Schema Authorization。

---

# 3. Decision Inputs

D-IG 必须同时使用以下输入：

1. 已确认的 A1/A2；
2. 本地代码 / DB / API Evidence；
3. Production Evidence（若可获得）；
4. 业务需求：销售、采购、库存、Recipe、定价、门店运营、追踪；
5. 既有历史模型，仅作为参考，不自动继承其结论；
6. 外部行业模型，仅作为参照，不作为本项目 Decision 的替代者。

Production Evidence Gate-0 当前为 BLOCKED，因此任何仅依赖 Production Evidence 的结论必须标记 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`。

---

# 4. Identity Grain Candidates vs Business Semantic Domains

## 4.1 Identity Grain Candidates

候选粒度层级包括但不限于：

- Brand
- Product Family
- Product Variant
- SKU
- Trade Item
- Inventory Unit
- Other candidate grain

这些只是候选，不构成预先选择。

## 4.2 Business Semantic Domain Candidates

以下属于待决业务语义领域，不直接构成 Identity Grain：

- Product
- Food
- Material
- Beverage
- Dish
- Other

D-IG 必须分析这些语义与 Identity 的关系，但不得仅因名称或现有表结构把它们当作 Identity Grain。

---

# 5. Decision Output

## 5.1 Required Output

D-IG 最终必须产出：

1. Identity Hierarchy Decision（H1/H2）；
2. Identity Grain Definition；
3. Identity-defining Attribute Matrix；
4. Non-identity Attribute Matrix；
5. Identity Resolution Rules；
6. 典型业务案例判定；
7. 与 A1/A2、D-QUANTITY、D-ROLE-SCOPE 的接口边界；
8. Provisional Conclusions Registry（如有）。

## 5.2 Identity Resolution Matrix

矩阵至少包含：

| Attribute / Dimension | Identity Impact | Scope | Identity Layer | Evidence | Decision Status |
|---|---|---|---|---|---|
| Brand | TBD | TBD | TBD | TBD | OPEN |
| Product Family | TBD | TBD | TBD | TBD | OPEN |
| Variant / Formula | TBD | TBD | TBD | TBD | OPEN |
| Size / Capacity | TBD | TBD | TBD | TBD | OPEN |
| Packaging | TBD | TBD | TBD | TBD | OPEN |
| Commercial Unit | TBD | TBD | TBD | TBD | OPEN |
| Base / Context UOM | TBD | TBD | TBD | TBD | OPEN |
| Supplier-specific code | TBD | TBD | TBD | TBD | OPEN |
| Batch / Lot | TBD | TBD | TBD | TBD | OPEN |
| Expiry / Production Date | TBD | TBD | TBD | TBD | OPEN |
| Location | TBD | Holding / Location | TBD | TBD | OPEN |
| Quantity | TBD | Quantity | TBD | TBD | OPEN |
| Recipe Relationship | TBD | Transformation | TBD | TBD | OPEN |

**Scope** 的取值必须引用 A1 已定义的语义概念，不得重新发明一套平行 Scope Ontology。

如果 §2 选择 H1 — Single Identity Grain：

> `Identity Layer` 列不适用，在矩阵中统一留空或删除该列。

如果 §2 选择 H2 — Multiple Identity Layers：

> `Identity Layer` 列必须引用 §2 所定义的正式层名。

---

# 6. Decision Tests

D-IG 每个关键结论必须通过至少一种可验证方法：

- 业务案例反例测试；
- 本地数据 / 代码路径验证；
- Production Evidence 验证；
- 业务 Owner 访谈 / 决策确认；
- 外部模型对标。

每个关键反证问题必须产出可验证预测：

> 如果该反证成立，应在数据、代码或业务规则中观察到什么？

如果预测未被观察到，该反证不能继续作为已成立结论。

---

# 7. Business Case Set

至少覆盖：

1. 同品牌不同容量：330ml vs 500ml；
2. 同商品不同包装层级：each vs pack vs case；
3. 同物品不同采购单位；
4. 同物品跨 Store / Warehouse；
5. 同物品不同 Batch / Lot；
6. 同物品不同 Recipe usage；
7. Food / Material / Product 跨域引用；
8. 可销售同时可采购；
9. 可销售同时作为 Recipe 输入；
10. 同物品在不同组织/时间范围承担不同 Role。

所有案例必须明确：

- Identity 是否相同；
- 如果不同，属于哪个 Identity Layer；
- 哪个属性造成差异；
- Evidence 来源；
- 是否存在 provisional 状态。

---

# 8. A1/A2 Interface

D-IG 必须遵守：

- A1 已确认的 Identity 与 Quantity / Commercial / Inventory / Tracking / Transformation 分离；
- A2 已确认 Product / Food / Material 不作为三个竞争性 Canonical Identity Namespace；
- 不得通过数字 ID 相等推导 Identity Equality；
- 不得以现有表名直接推导 Identity Grain。

---

# 9. Provisional Conclusion Governance

如果 Production Evidence 不可获得，D-IG 可以确认不依赖 Production Evidence 的部分，但必须：

1. 明确标记 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`；
2. 在 Provisional Conclusions Registry 登记证据缺口；
3. 指定需要生产证据复核的具体结论；
4. 明确禁止 provisional conclusion 进入依赖它的 Schema Design；
5. Production Evidence 到位后重新验证。

Provisional 不等于 Confirmed，也不等于 Invalidated。

---

# 10. D-IG 与后续 Decision 的边界

### D-QUANTITY

负责 Quantity / UOM / Conversion 的完整语义。

D-IG 只需要明确 Identity 与 Quantity/UOM 的交接面，不替 D-QUANTITY 做数量语义决定。

### D-ROLE-SCOPE

负责 Role 在 Domain / Organization / Temporal 上的适用范围。

D-IG 只需要确保 Identity Resolution 不隐含 Role Scope。

### Canonical Business Item Model

只有 D-IG、D-QUANTITY、D-ROLE-SCOPE 等前置 Decision 达到要求后，才进入 Canonical Model。

---

# 11. Decision Gate

D-IG 进入 CONFIRMED 前至少满足：

1. H1/H2 已明确；
2. Identity Grain / Layers 已定义；
3. Identity-defining attributes 已有明确判定；
4. Non-identity attributes 已有明确判定；
5. 关键 Business Cases 已逐项判定；
6. Quantity / UOM 边界已明确划出 D-IG 与 D-QUANTITY 的交接面；D-IG 不替 D-QUANTITY 做决定，但也不留未定义的空白；
7. Product / Food / Material 的处理符合 A2；
8. 所有重大反证问题均有可验证预测；
9. Local evidence 内部没有未解释冲突；
10. Production evidence：
    - 若可获得，则与 local evidence 的重大冲突已解释；
    - 若不可获得，则所有受影响结论均已标记 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`；
11. Identity Resolution Matrix 完整；
12. Decision Provenance 完整；
13. Independent adversarial review 已完成，且审阅意见已记录于 `d-ig-adversarial-review-XXX.md` 或等价的独立审阅记录中；
14. 所有 provisional conclusion 均已登记其禁止传播范围，并明确未进入依赖该结论的 Schema Design。

---

# 12. Governance Status

```text
D-IG = OPEN
Evidence Status = PARTIAL / LOCAL-VERIFIED, Production = BLOCKED
Execution Status = BLOCKED

Schema Authorization = NO
Migration Authorization = NO

First Agenda = H1 vs H2
```
