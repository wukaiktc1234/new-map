# D-IG — E1–E4 Identity Necessity Testing Record 001

> **状态**: ACTIVE_REFERENCE — TEST RECORD
> **Decision ID**: D-IG
> **执行阶段**: E1–E4
> **执行顺序**: C-001 Food → C-002 Material → Food/Material boundary review → re-screen remaining candidates if warranted
> **Identity Decision**: NOT_MADE
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO
> **Production Evidence**: BLOCKED
> **Requirement-Driven Status**: REQUIREMENT_DRIVEN_INCOMPLETE

## 1. Purpose

本记录只负责执行 D-IG Pre-Screen 之后的 E1–E4 测试，不直接确认最终 Identity。

E1–E4 的目标是分别检验候选对象是否满足：

- E1：不可替代表达测试
- E2：独立直接引用测试
- E3：独立生命周期测试
- E4：稳定语义一致性测试

测试结论必须来自可定位证据。代码实现存在、不存在、命名习惯或工程便利性本身，不得直接替代业务语义判断。

## 2. Source Boundary

### 2.1 Formal method sources

- `03-review/d-ig-prescreen-rule-001.md`
- `03-review/d-ig-e0-prescreen-result-001.yaml`
- `03-review/d-ig-e0-candidate-enumeration-001.yaml`
- `03-review/d-ig-business-capability-set-v2-logical-001.md`
- `03-review/business-item-canonical-independent-review.md`

### 2.2 Engineering evidence sources currently referenced

- `01-engineering-reality/business-object-map.md`
- `01-engineering-reality/db-reality-map.md`
- `02-business-dependency/business-data-flow-map.md`
- `02-business-dependency/business-identity-propagation-map.md`
- other evidence documents only when they are explicitly cited per test case

### 2.3 Evidence limitations

- Production evidence remains `BLOCKED`.
- Conclusions derived from local/documented evidence must remain local/documentary conclusions.
- Requirement-driven source status remains incomplete.
- No test result in this record may be promoted to a production-verified conclusion without the required evidence.

## 3. Candidate Queue

| Candidate | Name | Pre-Screen | Test Status | Order |
|---|---|---|---|---|
| C-001 | Food / Menu Item | NEEDS_TESTING | IN_PROGRESS | 1 |
| C-002 | Material / Procurement Material | NEEDS_TESTING | QUEUED | 2 |

The Pre-Screen result explicitly places C-001 and C-002 in `NEEDS_TESTING`; it does not make an Identity Decision. See `03-review/d-ig-e0-prescreen-result-001.yaml`.

## 4. Test Record Rules

For each candidate and each E-stage, the executor must record:

1. the specific business capability / operation being tested;
2. the observed loss if the candidate is removed;
3. the attempted semantic compensation using the A1 semantic layers;
4. the exact evidence references;
5. the result as `PASS`, `FAIL`, `INCONCLUSIVE`, or `BLOCKED`;
6. what the result does and does not establish.

A single `PASS` or `FAIL` in one E-stage does not by itself constitute the final Identity decision.

## 5. C-001 — Food / Menu Item

### 5.1 Candidate grounding

Current E0 evidence records the following local/documented facts:

- `foods` is documented as the current Food truth source.
- `Food` entity has create/update/delete paths and read paths.
- `order_items.foodId` directly references the Food object in the documented order path.
- Documented consumers include `OrderService`, `InventoryService`, and `MenuService`.
- Legacy `food` table is documented separately as deprecated.

Primary evidence: `01-engineering-reality/business-object-map.md §1`; E0 C-001 in `03-review/d-ig-e0-candidate-enumeration-001.yaml`.

### 5.2 E1 — Inexpressibility / Non-Substitutability

**Question**

> If Food is removed, can the confirmed Food-dependent business behavior be represented losslessly by the existing A1 semantic layers and their combinations, without introducing an equivalent independent object under another name?

**Tested confirmed capability**

`CAP-IMPL-016 — Create orders`

**Observed dependency**

The documented order path uses `order_items.foodId` as a direct reference to the Food object. Removing Food therefore removes the stable referenced object required by the documented order-item path.

**Compensation attempts**

| A1 semantic-layer combination | Result | Failure point |
|---|---|---|
| Classification + Role + Domain | LOSS NOT COMPENSATED | Can describe what category/role/domain is being sold, but cannot identify the particular stable order-item reference target. |
| Commercial Unit + Quantity + UOM | LOSS NOT COMPENSATED | Can describe transaction unit and amount, but cannot identify the stable sellable object referenced by the order item. |
| Classification + Commercial Unit + Role | LOSS NOT COMPENSATED | Can describe the commercial meaning, but still lacks a stable object reference anchor for the order item. |
| Domain + Commercial Unit + Temporal | LOSS NOT COMPENSATED | Can describe contextual/time-dependent meaning, but cannot replace the stable object reference required by the documented path. |

**Minimal counterfactual**

To remove `Food` while preserving the confirmed order capability, the order item would still need to reference some independently addressable stable object carrying the same referential role. Such a replacement would be an equivalent independent object under another name rather than a pure A1 semantic-layer substitution.

**Evidence**

- `01-engineering-reality/business-object-map.md §1` — `foods` truth source and Food API/read/write paths; `order_items.foodId`; consumers.
- `03-review/d-ig-e0-prescreen-result-001.yaml` — C-001 Pre-Screen removal effect and compensation attempts.
- `03-review/d-ig-prescreen-rule-001.md §4 / §4.1` — required necessity and compensation method.

**Result**: `PASS`

**What this proves**

Under the current documented `CAP-IMPL-016` evidence, Food cannot be removed and replaced solely by the A1 semantic layers without losing a stable reference required by the order capability or introducing an equivalent independent object.

**What this does not prove**

- It does not prove Food is the final Canonical Identity name.
- It does not prove Food and Material are separate or non-overlapping business identities.
- It does not resolve the Food/Material boundary issues FM-001–FM-004.
- It does not authorize H1/H2, Schema, Migration, or Canonical Table creation.
- It does not convert local/documented evidence into production verification.

### 5.3 E2 — Independent Direct Reference

**Question**

> Do confirmed business capabilities directly reference Food as a stable object, rather than merely carrying Food-like attributes?

**Initial evidence to test**

- `order_items.foodId`
- Food CRUD paths
- documented consumers of Food
- any additional confirmed capability that directly references `food_id`

**Current documented signal**

The existing evidence package explicitly records `order_items.foodId` as a direct reference and Food CRUD/read paths as an independently addressable object path.

**Status**: `INCONCLUSIVE_PENDING_EVIDENCE_CONSISTENCY_CHECK`

The existence of direct references is not by itself enough to settle whether those references represent an independently required business Identity; the business semantics and replacement test must still be completed.

### 5.4 E3 — Independent Lifecycle

**Question**

> Does Food have a business lifecycle independent from transactions that consume or reference it?

**Initial documented evidence**

- Food has documented create/update/delete operations.
- Food has documented states: `ACTIVE`, `INACTIVE`, `DISCONTINUED`.
- Food has dedicated permissions and API paths.

**Status**: `INCONCLUSIVE_PENDING_BUSINESS_LIFECYCLE_VALIDATION`

The engineering object lifecycle is evidence of an independent technical lifecycle, but the test must still verify whether this corresponds to an independent business lifecycle rather than merely a technical aggregate boundary.

### 5.5 E4 — Stable Semantic Consistency

**Question**

> Across confirmed capabilities, does the referenced Food concept preserve a stable business meaning, such that removing the object would require uncontrolled duplication, rewriting, or inconsistent interpretation?

**Initial evidence to test**

- order/sales references
- menu references
- inventory-related usage
- recipe relationship usage
- naming/code/state semantics

**Status**: `INCONCLUSIVE_PENDING_CROSS_CAPABILITY_CHECK`

### 5.6 C-001 provisional test state

`C-001 = E1 PASS; E2 INCONCLUSIVE; E3 INCONCLUSIVE; E4 INCONCLUSIVE`

No final Identity conclusion is authorized at this point.

## 6. Food / Material Boundary Issue Register

This register is intentionally kept separate from the C-001 test conclusion.

| Issue ID | Boundary question | Current status | Rule |
|---|---|---|---|
| FM-001 | 半成品（如自制酱料、预制面团）属于 Food、Material，还是两者在不同业务上下文中分别存在？ | OPEN | 不在 C-001 E1–E4 中提前裁定 |
| FM-002 | 套餐中的组成项如何引用？ | OPEN | 先完成 Food 独立测试，再单独处理 |
| FM-003 | 配方里的原料与采购物料是否为同一业务对象？ | OPEN | 不因数据库字段同名而预设等价 |
| FM-004 | Food 与 Material 是否存在合法的一对一、多对一、多对多或上下文分域关系？ | OPEN | 需要 E2/E3/E4 后再分析 |

这些问题是边界问题，不得倒灌成 C-001 或 C-002 的预设结论。

## 7. C-002 — Material / Procurement Material

### 7.1 Candidate grounding

C-002 当前 E0 证据记录：

- `material_archives` 为文档记录的 Material truth source；
- `purchase_order_items.material_id` 直接引用 Material；
- `inventory.material_id`、`material_trace_code.materialId`、`material_consumption.materialId` 等路径传播 Material reference；
- 当前证据还记录了独立 Material CRUD/read paths。

Primary evidence: `01-engineering-reality/business-object-map.md §2`; E0 C-002 in `03-review/d-ig-e0-candidate-enumeration-001.yaml`.

### 7.2 Execution State

`QUEUED — START AFTER C-001`

C-002 必须独立执行 E1–E4；不得因为 C-001 的结论自动复制到 C-002。

## 8. Decision Safety Gates

Only the following transitions are allowed after this test record:

`E1–E4 evidence → candidate-level conclusion → Required Identity Object Set → H1/H2 → Identity Decision`

The following transitions remain prohibited:

- E1–E4 → direct Schema design
- E1–E4 → migration design
- engineering object name → confirmed Identity
- local evidence → production verification
- Food result → automatic Material result
- Material result → retroactive rewriting of Food result without explicit boundary review

## 9. Next Execution Step

The next execution is C-001 E2 evidence completion, followed by E3 and E4. After C-001 is completed, execute C-002 independently, then process FM-001 through FM-004 as a separate Food/Material boundary review.

## 10. Governance State

- D-IG: `OPEN`
- E0: `COMPLETED_PENDING_E1_E4`
- E1–E4: `IN_PROGRESS — C-001`
- C-001 E1: `PASS`
- C-001 E2: `INCONCLUSIVE`
- C-001 E3: `INCONCLUSIVE`
- C-001 E4: `INCONCLUSIVE`
- C-002: `QUEUED`
- Identity Decision: `NOT_MADE`
- H1/H2: `NOT_MADE`
- Schema: `BLOCKED`
- Migration: `BLOCKED`
- Production evidence: `BLOCKED`
- Requirement-driven status: `REQUIREMENT_DRIVEN_INCOMPLETE`
