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
- The uploaded local engineering audit contains code-level observations from a larger local workspace; the GitHub repository at this stage is treated as the submitted governance/evidence package. Local workspace observations therefore remain `LOCAL_ONLY` unless reproduced by a locatable repository evidence document.

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

#### 5.2A E1 — Adversarial Counter-Refutation

**Attack objective**

Attempt to prove that the `E1 = PASS` result is false by reconstructing the order item's Food dependency from A1 semantic layers only, without an independently addressable object.

**Required and expanded compensation attempts**

| Proposed compensation | Attack result | Reason |
|---|---|---|
| Classification + Role + Domain | FAIL TO COMPENSATE | Describes a class/role/domain but does not provide the stable referential identity represented by `order_items.foodId`. |
| Commercial Unit + Quantity + UOM | FAIL TO COMPENSATE | Quantity/UOM are transaction semantics, not the stable target of the item reference. |
| Classification + Commercial Unit + Role | FAIL TO COMPENSATE | Still lacks the stable referenced object; two items with identical values could not be distinguished by a semantic-layer tuple alone. |
| Domain + Commercial Unit + Temporal | FAIL TO COMPENSATE | Temporal context does not create the required stable reference target. |
| Classification + Domain + Temporal | FAIL TO COMPENSATE | Adds context but still cannot serve as the independently addressable order-item target. |
| Role + Commercial Unit + Temporal | FAIL TO COMPENSATE | Same deficiency: no stable object identity is created by the layer combination. |
| All A1 layers combined | FAIL TO COMPENSATE | The combination becomes a descriptive tuple. To use it as a stable reusable referent, the system must introduce an independently addressable object/registry entry carrying that tuple, which is an equivalent independent object rather than pure compression. |
| Use `foodName` / other Food attributes as the reference | FAIL TO COMPENSATE | The documented order path explicitly treats `foodName` as snapshot data while `foodId` is the reference. Snapshot cannot substitute for the reference relationship. |

**Adversarial conclusion**

No evidence-supported A1-only lossless substitution was found. Every successful-looking reconstruction requires an independently addressable stable target, which violates the "without equivalent independent object" condition.

**Result remains**: `PASS`

**Scope limitation**: this adversarial result attacks the current E1 logic; it does not establish the canonical naming of that independent object, nor the Food/Material boundary.

### 5.3 E2 — Independent Direct Reference

**Question**

> Do confirmed business capabilities directly reference Food as a stable object, rather than merely carrying Food-like attributes?

#### 5.3.1 Direct-reference evidence table

| Confirmed capability / path | Business operation | Field / path | Target object | Evidence | Reference type | Semantic role | Evidence strength |
|---|---|---|---|---|---|---|---|
| `BP-01` POS point-order / `CAP-IMPL-016` | Create order | `order_items.foodId` | `foods` / Food | `02-business-dependency/business-data-flow-map.md` 订单链；`02-business-dependency/business-identity-propagation-map.md` 2.2 | `DIRECT_OBJECT_REFERENCE` | Order line identifies the sellable Food object | STRONG |
| `BP-34` food trace-code path | Generate/query finished-food trace code | `food_trace_code.foodId` | `foods` / Food | `02-business-dependency/business-data-flow-map.md` 溯源链；local audit `Owner结果.txt` BP-34 | `DIRECT_OBJECT_REFERENCE` | Finished-food trace record identifies the Food referenced by the originating order item | STRONG / LOCAL_ONLY for exact code path |
| `BP-16` recipe maintenance | Maintain recipe attached to Food | FoodController `recipes` payload → `dish_recipes` | Food-associated recipe target | local audit `Owner结果.txt` BP-16 / UCC-001 | `INDIRECT` | Recipe data is maintained through Food endpoint and persisted to recipe records; exact FK field is not exposed in the governance package | MEDIUM |
| Store inventory / Food dependency | Read store inventory by Food | `findByFood()` path | Food | `01-engineering-reality/business-object-map.md` StoreInventory section | `ATTRIBUTE_ONLY` | Governance package shows a Food lookup dependency, but does not expose the exact physical reference field | MEDIUM / exact-field evidence absent |

#### 5.3.2 Candidate paths explicitly not promoted to direct-reference evidence

The following are not counted as direct Food references because the current evidence package does not provide a concrete field/target pair:

- `MenuService` as a consumer of Food: consumer relationship is documented, but no exact direct-reference field is exposed in the current governance package.
- Promotion: local audit records promotion rules as not wired into order evaluation; no Food direct-reference evidence was established.
- Delivery/takeout: Owner boundary is confirmed, but the local audit states there is no delivery/rider/dispatch behavior path; no Food direct-reference field is established.
- Kitchen: kitchen collaboration is evidenced around order execution, but a separate direct Food field was not located in the current evidence package.

Where the exact reference cannot be located, this record deliberately uses `NO_EVIDENCE` in the test narrative rather than inferring from names or consumer lists.

**E2 test reasoning**

Two independent behavior paths explicitly preserve the same object-reference semantics: `order_items.foodId → foods` and `food_trace_code.foodId → foods`. This is materially stronger than merely having a Food-like attribute on transactions. The propagation map explicitly marks `order_items.foodId` as `REFERENCE`, and the data-flow map explicitly maps `foodId → foods` in the order path.

**Result**: `PASS`

**What this proves**

- At least one confirmed business capability directly references Food as a stable object.
- A second traceability behavior path independently carries the same Food reference semantics.
- The observed reference semantics are not reducible to a snapshot-only `foodName` attribute in the documented order path.

**What this does not prove**

- It does not prove every Food consumer has an independently documented direct reference.
- It does not settle whether StoreInventory's Food dependency is a direct FK or a derived lookup because the exact field is not exposed in the current package.
- It does not establish the final Identity name or Food/Material separation.

### 5.4 E3 — Independent Lifecycle

**Question**

> Does Food have a business lifecycle independent from transactions that consume or reference it?

**Initial documented evidence**

- Food has documented create/update/delete operations.
- Food has documented states: `ACTIVE`, `INACTIVE`, `DISCONTINUED`.
- Food has dedicated permissions and API paths.

#### 5.4.1 Adversarial lifecycle review

**Position A — status is a business lifecycle**

Evidence supporting this possibility:

- Food has dedicated state vocabulary (`ACTIVE`, `INACTIVE`, `DISCONTINUED`) rather than only CRUD verbs.
- State is part of the Food object model and accompanies a dedicated Food API/permission surface.
- A `DISCONTINUED` state is semantically more specific than a technical delete flag and could represent a durable business fact about a Food item.

**Position B — status is only a technical state flag**

Evidence supporting this possibility:

- The current evidence package shows the existence of state values, but does not establish the business event that causes each transition.
- No current evidence in this record proves what `INACTIVE` does to sales eligibility, menu exposure, ordering, inventory behavior, or downstream references.
- No current evidence establishes whether `DISCONTINUED` can coexist with historical order references or what historical reconstruction rule applies.
- The current evidence package does not show an explicit owner/business rule stating why state must exist independently of technical CRUD.

#### 5.4.2 Required counterfactual checks

| Question | Current evidence | Result |
|---|---|---|
| Food never referenced by an order — can it be created? | Food create path exists, but the business precondition is not stated | INCONCLUSIVE |
| Existing historical orders — can Food become `DISCONTINUED`? | State exists, but transition rule is not documented | INCONCLUSIVE |
| After `DISCONTINUED`, how are historical orders interpreted? | `foodName` snapshot is documented, but no explicit historical-lifecycle rule is documented | INCONCLUSIVE |
| Does `INACTIVE` affect selling? | No business-effect evidence located | INCONCLUSIVE |
| Does a state change have independent business meaning? | Possible from vocabulary, not established by a locatable business rule | INCONCLUSIVE |
| If the state field were removed, what business behavior would be lost? | Not established without the transition/effect rules above | INCONCLUSIVE |

**Result**: `INCONCLUSIVE`

**Reason**

The engineering evidence establishes an independent technical state surface, but the required business-lifecycle consequences remain unproven. A status field cannot be promoted to E3 PASS merely because it has business-sounding labels.

**What this proves**

- Food has an independent technical lifecycle surface.
- The current evidence is insufficient to prove the corresponding business lifecycle semantics.

**What this does not prove**

- It does not prove that Food lacks an independent business lifecycle.
- It does not authorize removal of Food lifecycle states.

### 5.5 E4 — Stable Semantic Consistency

**Question**

> Across confirmed capabilities, does the referenced Food concept preserve a stable business meaning, such that removing the object would require uncontrolled duplication, rewriting, or inconsistent interpretation?

#### 5.5.1 Cross-capability semantic mapping

| Capability / context | Observed Food meaning | Evidence status | Semantic relationship |
|---|---|---|---|
| Order / POS sale (`BP-01`, `CAP-IMPL-016`) | Sellable Food object identified by `foodId` | VERIFIED in submitted map; local code path also reported STRONG | Core sellable referent |
| Traceability (`BP-34`) | Finished Food associated with `food_trace_code.foodId` | VERIFIED in data-flow map; local path STRONG | Same Food referent carried into traceability context |
| Recipe (`BP-16`, UCC-001) | Food is the target/context under which recipe rows are maintained and cost is calculated | CLEAR behavior path in local audit; exact FK semantics not fully exposed | Same business referent viewed through recipe/cost role |
| Store inventory / Food dependency | Food is a lookup/subject of store-inventory behavior | Object-map evidence exists; exact field not exposed | Potential same referent, but exact reference mechanism is not fully evidenced |
| Menu | Food is documented as a MenuService consumer | Consumer relation only | Cannot prove a distinct object; insufficient direct-field evidence |
| Promotion | Promotion behavior exists, but local audit says rule evaluation is not wired into order path | PARTIAL | Not used to establish semantic consistency |
| Delivery | Owner boundary is confirmed, but local audit reports no delivery behavior path | BLOCKED/NO_BEHAVIOR_PATH_FOUND locally | Not used to establish semantic consistency |
| Kitchen | Kitchen collaborates with order execution | Partial | No separate Food identity contradiction established, but direct Food field not located |

#### 5.5.2 Consistency attack

Potential contradiction paths considered:

1. **Food = menu-only presentation object** — rejected by current order and traceability direct-reference evidence, which use Food as a persisted stable target.
2. **Food = inventory-only object** — rejected by order direct-reference evidence, where Food is the order-line target before inventory consequences.
3. **Food = recipe-only object** — rejected by direct order and traceability references.
4. **Food changes into a different object in traceability** — not supported; the trace path explicitly carries `foodId → foods`.
5. **Promotion / delivery / kitchen require a semantically different object** — no evidence was found proving such a split; these contexts therefore remain out of the positive E4 proof set rather than being treated as evidence of equivalence.

**Result**: `PASS`

**Basis for PASS**

The currently evidenced cross-capability paths preserve one stable referent (`foods` / Food) across ordering and traceability, with recipe behavior attached to the same Food entry point. No evidence-supported semantic contradiction or object substitution was found. Where evidence is missing, the path is excluded from the positive proof rather than guessed into equivalence.

**Scope limitation**

E4 PASS is limited to the evidenced capability set above. It does not mean every system capability has been checked, and it does not resolve the Food/Material boundary issues.

### 5.6 C-001 current test state

`C-001 = E1 PASS; E2 PASS; E3 INCONCLUSIVE; E4 PASS`

`C-001 = NOT COMPLETE FOR CANDIDATE-LEVEL CONCLUSION`

No final Identity conclusion is authorized at this point because E3 business lifecycle semantics remain unresolved and the Food/Material boundary register remains open.

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

The next execution is C-001 completion of the unresolved E3 business-lifecycle evidence, followed by C-002 independent E1–E4 execution, then process FM-001 through FM-004 as a separate Food/Material boundary review.

## 10. Governance State

- D-IG: `OPEN`
- E0: `COMPLETED_PENDING_E1_E4`
- E1–E4: `IN_PROGRESS — C-001`
- C-001 E1: `PASS`
- C-001 E2: `PASS`
- C-001 E3: `INCONCLUSIVE`
- C-001 E4: `PASS`
- C-002: `QUEUED`
- Identity Decision: `NOT_MADE`
- H1/H2: `NOT_MADE`
- Schema: `BLOCKED`
- Migration: `BLOCKED`
- Production evidence: `BLOCKED`
- Requirement-driven status: `REQUIREMENT_DRIVEN_INCOMPLETE`
