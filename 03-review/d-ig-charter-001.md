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

在 §2 的结构判断未完成前，不得正式填写 §5.2 Identity Resolution Matrix。

D-IG 不以“先投票 H1 还是 H2”为唯一判定流程，而采用以下顺序：

1. E0：枚举候选业务对象 / 粒度集合；
2. E1–E4：逐一测试候选对象是否必须保留为 Identity；
3. 汇总得到 `Required Identity Object Set`；
4. 若该集合为空，则 H1；
5. 若该集合包含两个或以上不同抽象粒度且存在稳定层间关系，则 H2；
6. 只有完成上述判断后，才确定 H1/H2 的正式结构与 Identity Resolution Matrix。

H1/H2 判断阶段允许临时构建最小结构示例，用于比较表达能力，但不得将试建结构写成 Decision、Canonical Model 或 Schema Authorization。

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

# 4. Candidate Enumeration and Semantic Compression Test

## 4.1 Candidate Enumeration — E0

E0 的目标不是套用预设的 Brand / Family / Variant / SKU 名称，而是从当前业务证据中枚举**真实需要被业务表达、引用或治理的对象粒度**。

候选来源至少包括：

- `foods` 当前数据与业务引用；
- `material_archives` 当前数据与业务引用；
- `product` 的 active-code residue；
- 订单、采购、库存、Recipe、定价等业务路径中的对象引用；
- 已记录的跨 namespace 冲突；
- 业务 Owner 提供的业务对象清单。

通用行业对象名称（如 Brand / Product Family / Variant / SKU / Trade Item）可以作为**候选搜索词**，但不能因为名称存在就视为本项目已有业务对象。

E0 必须记录：

- Candidate Object；
- 来源；
- 所代表的抽象粒度；
- 已观察到的业务行为；
- 是否存在独立引用路径；
- 当前证据等级；
- 是否仍只是待验证假设。

## 4.2 Semantic Compression — Operational Definition

**语义压缩**定义为：

> 当一个候选上层对象 O 被表示为下层对象的属性时，若 O 的全部业务行为都可以在**不建立独立业务对象、不建立独立生命周期、不建立独立引用路径、不建立稳定对象间关系**的情况下，被无损翻译到下层对象，则 O 可被视为可压缩到非-Identity 语义。

反之，只要发现 O 至少存在一种业务行为，其正确表达**必须**依赖以下任一项：

- 独立生命周期；
- 独立业务引用 / 直接引用路径；
- 稳定、可验证的对象关系；
- 独立治理边界；
- 在不复制/重写下层 Identity 的情况下保持跨下层对象的一致语义；

则不能仅以属性压缩假定 O 非-Identity；该候选进入 `Required Identity Object Set` 候选集合，等待进一步验证。

> 注意：该定义不自动把 Role / Classification / Commercial Unit / UOM / Holding / Batch 升格为 Identity；这些仍需经过 A1 分离与 D-IG 测试。

---

# 5. Decision Output

## 5.1 Required Output

D-IG 最终必须产出：

1. Identity Hierarchy Decision（H1/H2）；
2. Identity Grain Definition；
3. Required Identity Object Set；
4. Identity-defining Attribute Matrix；
5. Non-identity Attribute Matrix；
6. Identity Resolution Rules；
7. 典型业务案例判定；
8. 与 A1/A2、D-QUANTITY、D-ROLE-SCOPE 的接口边界；
9. Provisional Conclusions Registry（如有）。

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

如果最终选择 H1：

> `Identity Layer` 列不适用，在矩阵中统一留空或删除该列。

如果最终选择 H2：

> `Identity Layer` 列必须引用最终 Decision 所定义的正式层名。

---

# 6. Evidence and Decision Tests

D-IG 每个关键结论必须通过至少一种可验证方法：

- 业务案例反例测试；
- 本地数据 / 代码路径验证；
- 业务规则访谈 / Owner 确认；
- Production Evidence 验证；
- 外部模型对标。

每个 E0–E4 结论必须标注证据来源：

- `LOCAL_CODE`
- `LOCAL_DATA`
- `BUSINESS_RULE_INTERVIEW`
- `PRODUCTION_DATA`
- `EXTERNAL_REFERENCE`

在 Production Evidence Gate-0 仍 BLOCKED 时：

- LOCAL_CODE / LOCAL_DATA / BUSINESS_RULE_INTERVIEW / EXTERNAL_REFERENCE 可以支持 provisional reasoning；
- 任何依赖生产数据才能确认的结论必须标记 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`；
- provisional conclusion 必须进入 Provisional Conclusions Registry；
- provisional conclusion 不得传播进入依赖它的 Schema / Migration Decision。

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
3. Required Identity Object Set 已明确，并说明为何集合为空、单层或多层；
4. Identity-defining attributes 已有明确判定；
5. Non-identity attributes 已有明确判定；
6. 关键 Business Cases 已逐项判定；
7. Quantity / UOM 边界已明确划出 D-IG 与 D-QUANTITY 的交接面；D-IG 不替 D-QUANTITY 做决定，但也不留未定义的空白；
8. Product / Food / Material 的处理符合 A2；
9. 所有重大反证问题均有可验证预测；
10. Local evidence 内部没有未解释冲突；
11. Production evidence：
    - 若可获得，则与 local evidence 的重大冲突已解释；
    - 若不可获得，则所有受影响结论均已标记 `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE`；
12. Identity Resolution Matrix 完整；
13. Decision Provenance 完整；
14. Independent adversarial review 已完成，且审阅意见已记录于对应的 `d-ig-adversarial-review-XXX.md` 或等价审计记录；
15. 所有 provisional conclusion 均已登记其禁止传播范围，并明确未进入依赖该结论的 Schema Design。

---

# 12. Governance Status

```text
D-IG = OPEN
Evidence Status = PARTIAL / LOCAL-VERIFIED, Production = BLOCKED
Execution Status = BLOCKED

Schema Authorization = NO
Migration Authorization = NO

First Agenda = E0 Candidate Enumeration → E1–E4 Identity Necessity Tests
Required Identity Object Set = NOT YET DETERMINED
```
