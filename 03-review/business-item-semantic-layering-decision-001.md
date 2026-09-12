# Business Item Semantic Layering — A1/A2 Decision 001

> **状态**: CONFIRMED  
> **Decision IDs**: DEC-BI-001 (A1), DEC-BI-002 (A2)  
> **Confirmed by**: BUSINESS_OWNER  
> **Confirmed at**: 2026-09-12  
> **Confirmation basis**: Owner approval of A1/A2 Owner Decision Draft 004  
> **性质**: Business Semantic Principle  
> **禁止事项**: 本 Decision 不授权数据库 Schema、Migration、Canonical Table Creation、Identity Migration、Legacy 删除或历史数据清洗。

---

# 1. Decision Purpose

当前系统存在多个主数据空间、多个数量持有位置，以及 `food_id`、`material_id`、`product_id` 的跨领域使用与语义漂移。

本 Decision 只解决两项业务语义边界：

1. Identity 是否必须与其他业务语义分离；
2. Product / Food / Material 是否继续作为竞争性的 Canonical Identity Namespace。

本 Decision 不选择最终 Identity Grain；Identity Grain 由独立 Decision `D-IG` 确定。

---

# 2. A1 — Identity 与其他语义概念分离

## 2.1 Confirmed Principle

确认：

> **Business Identity 与 Quantity、Commercial、Inventory Holding、Tracking、Transformation 等语义概念必须保持区分。**

下游业务模型必须能够分别表达：

- Identity
- Classification
- Capability / Role
- Domain
- Organization / Location
- Temporal Validity
- Quantity / UOM
- Conversion
- Commercial Unit
- Inventory Holding
- Tracking
- Transformation / Recipe

不得仅因为实现方便而默认将上述概念合并成单一 Identity。

## 2.2 Identity Stability

以下变化本身不构成创建新 Identity 的充分条件：

- Location 改变；
- Quantity 改变；
- Inventory Holding 改变；
- Batch / Lot 改变；
- Recipe Relationship 改变；
- Business Domain 改变。

Commercial Unit 改变也不得在没有 Identity Grain Decision 的情况下被默认解释为新的 Identity。

## 2.3 Identity Grain Independence

Identity Grain 是独立 Decision：

**D-IG — Identity Grain Decision**

D-IG 必须明确：

- 是否存在多个 Identity 层级；
- 每个层级的语义；
- 什么属性定义每个层级的 Identity；
- 什么属性不定义 Identity。

A1 不预先选择 Brand、Product Family、Variant、SKU、Trade Item、Inventory Unit 中任何一个作为 Identity Grain。

但正式 Schema Design 开始前，Identity Grain 必须由 D-IG 显式确定，不得由表结构、主键、字段名称、SKU 编号或旧表名称隐含决定。

## 2.4 Quantity / UOM / Conversion / Commercial Unit

确认：

> **Identity ≠ UOM ≠ Conversion ≠ Commercial Unit**

UOM 表示数量计量单位；Conversion 表示数量表达之间的换算；Commercial Unit 表示商业交易/包装单位。

本 Decision 不确定 Base UOM、Transaction UOM、Commercial Unit 与具体 Identity Grain 的最终归属；相关问题由 D-IG / D-QUANTITY 后续确定。

## 2.5 Inventory Holding

确认：

> **Inventory Holding ≠ Business Identity**

同一个 Business Identity 可以在不同 Location / Organization 中形成多个 Inventory Holdings。

Inventory / Store Inventory 的最终关系、Location Model、Holding 粒度不在 A1 中确定。

## 2.6 Tracking

Tracking 用于表达 Batch、Lot、Serial、Expiry 等追踪信息。

确认：

> Tracking 信息本身不等同于 Business Identity。

Batch 是否成为 Inventory Holding 的强制粒度，由后续 Decision 确定。

## 2.7 Transformation / Recipe

确认：

> Recipe / BOM / Production Transformation 与 Commercial Unit / Packaging Conversion 是不同语义。

例如：

`24 cans = 1 case`

属于数量/商业转换。

而：

`beef + bread + lettuce → burger`

属于 Transformation / Recipe。

---

# 3. A1 Testable Constraints

### A1-C1 — Location Independence

必须存在至少一个合法的 Location 变更操作，使从 Location A 转移到 Location B 后，Business Identity 数量不增加，且业务语义保持正确。

### A1-C2 — Quantity Independence

必须存在至少一个合法的 Quantity 变化操作，使 Quantity 改变后 Business Identity 数量不增加。

### A1-C3 — Tracking Independence

必须存在至少一个合法的 Batch / Lot 变化场景，使 Tracking value 改变后 Business Identity 数量不增加。

### A1-C4 — Transformation Independence

必须存在至少一个合法的 Recipe / Transformation Relationship，使 Recipe Relationship 增减后 Business Identity 数量不增加。

### A1-C5 — Commercial Unit Independence

必须存在至少一个合法的 Commercial Unit 变化场景，使 Commercial Unit 发生变化时，至少存在一种合法业务表达方式，使 Business Identity 数量不增加。

具体哪些 Commercial Unit 变化属于 Identity 变化的例外，由 D-IG 单独决定。

---

# 4. A2 — Product / Food / Material Namespace

## 4.1 Confirmed Principle

确认：

> **Product、Food、Material 不作为三个互相竞争的 Canonical Identity Namespace。**

不得建立长期依赖三套平级 Canonical Identity 并通过大量 cross-reference 推导“同一个现实对象”。

## 4.2 What A2 Does Not Decide

A2 不确认：

- Product = Identity；
- Product = Classification；
- Product = Role；
- Material = Identity；
- Food = Identity；
- Food = Material；
- Material = Product。

也不确认：

- `product` = Canonical Product Master；
- `foods` = Universal Business Identity；
- `material_archives` = Universal Business Identity。

这些由后续业务语义 Decision 与 D-IG 确定。

## 4.3 Multi-role Principle

确认：

> 一个现实业务对象可以在不同业务场景中承担多个业务职责，而不因此产生多个 Canonical Identity。

一个对象可以被采购、销售、库存管理、Recipe 使用；多个业务职责不等于多个 Identity。

## 4.4 Namespace Equality Rule

确认：

> **数字 ID 相同不构成业务对象相同的证据。**

例如：

```text
material_id = 1
product_id  = 1
```

不能仅因数值相同就推断两个对象相同；必须依据明确的 Identity Relationship。

## 4.5 Current Evidence Boundary

`purchase_request_item.food_id` 当前本地 46/50 条记录实际匹配 Material ID。

本 Decision 仅认定：

> 该事实证明当前系统存在跨 namespace 引用或语义漂移。

它不证明 Food 与 Material 必然共享 Identity，也不证明它们必然拥有不同 Identity，更不授权字段改名或数据迁移。

---

# 5. A2 Testable Constraints

### A2-C1

任何业务系统不得仅通过 Product ID / Material ID / Food ID 的数字相等判断两个业务对象相同。

### A2-C2

如果多个业务域引用同一现实业务对象，必须存在可解释的 Identity Relationship。

### A2-C3

一个对象承担多个业务职责时，Role / Domain 的变化不得单独成为创建新 Canonical Identity 的理由。

### A2-C4

任何新的 Canonical Identity Model 不得通过 `product`、`foods`、`material_archives` 任一现有表名直接推导 Identity 语义。

---

# 6. Engineering Boundary

以下事项在 A1/A2 Confirmation 后仍然需要后续 Decision：

- Identity Grain；
- Product / Food / Material 最终语义；
- Commercial Unit Schema；
- Inventory Holding Schema；
- Batch / Tracking granularity；
- Recipe Identity boundary；
- Product / Food / Material namespace migration。

因此：

> **A1/A2 CONFIRMED ≠ Schema Authorization**

> **A1/A2 CONFIRMED ≠ Identity Grain Decision**

> **A1/A2 CONFIRMED ≠ Migration Authorization**

---

# 7. Downstream Decisions

Owner Confirmation 后的直接下游 Decision：

1. `D-IG — Identity Grain Decision`
2. `D-QUANTITY — Quantity Semantics Decision`
3. `D-ROLE-SCOPE — Role Scope Decision`
4. Canonical Business Item Model

---

# 8. Governance Record

```text
A1 = CONFIRMED
A2 = CONFIRMED

Decision IDs:
- DEC-BI-001 = A1
- DEC-BI-002 = A2

Confirmed by: BUSINESS_OWNER
Confirmed at: 2026-09-12

D-IG = OPEN
D-QUANTITY = OPEN
D-ROLE-SCOPE = OPEN

Schema Authorization = NO
Identity Migration Authorization = NO
Canonical Table Authorization = NO
```
