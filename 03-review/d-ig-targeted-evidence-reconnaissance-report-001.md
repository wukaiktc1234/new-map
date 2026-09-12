# D-IG — Targeted Evidence Reconnaissance Report 001

> **Status**: ACTIVE_REFERENCE — LOCAL REPRODUCED EVIDENCE REPORT
> **Decision ID**: D-IG
> **Scope**: C-001 Food / C-002 Material candidate-level blocking evidence repair
> **Evidence class**: LOCAL_REPRODUCED_EVIDENCE for database; LOCAL working-tree CODE EVIDENCE for source observations
> **Production Evidence**: BLOCKED
> **Identity Decision**: NOT_MADE
> **Required Identity Object Set**: NOT_STARTED
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO

## 1. Purpose and execution boundary

本报告落实 `d-ig-targeted-evidence-reconnaissance-task-001.md`。执行目标是把 candidate-level 外部反方提出的关键 referent / scope 攻击转换为可复现证据。

本报告不作 Business Owner / Product Owner 裁决，不创建 Identity Decision，不关闭 FM，不授权 Schema/Migration，也不发布 Candidate PASS。

远程治理仓库是正式治理包；本报告中的代码与数据库观察来自本地工作树和本地 PostgreSQL，不等同于生产环境。

## 2. Provenance

### 2.1 Remote governance package

- Repository: `wukaiktc1234/new-map`
- Governance inputs: Charter Amendment 002, Candidate-level Disposition 001, FM Boundary Register 001, E1-E4 test record 001.

### 2.2 Local engineering evidence

- Working tree: backend / frontend source tree with uncommitted modifications.
- Source observations below are `LOCAL_ONLY` unless separately reproduced in a committed evidence document.

### 2.3 Local database evidence

- PostgreSQL 18.3, database `food_traceability`.
- `flyway_schema_history` maximum observed version: `9.0.0`.
- Queries were read-only (`SELECT`, `information_schema`).
- Representative local row counts: `food=1`, `foods=1`, `product=1`, `order_items=6`, `purchase_request_item=50`, `purchase_order_items=84`, `inventory=11`, `inventory_transactions=27`, `material_archives=35`, `material_trace_code=18`, `material_consumption=0`, `dish_recipes=5`, `food_trace_code=0`.
- These counts are not production population estimates; ratios must not be extrapolated.

## 3. E-definition alignment

### E1
`ALIGNED — no full E1 rerun required.` The existing E1 target is consistent with Amendment 002: test whether the candidate-dependent behavior can be losslessly represented using A1 semantic layers without reintroducing an equivalent independently addressable object.

### E2
`TARGET ALIGNED; existing C-001/C-002 evidence mapping requires targeted reclassification/review.` The method now explicitly permits propagation as corroboration and requires only at least one direct stable reference. However, several prior field/target assertions are contradicted by local evidence and therefore cannot remain unchanged.

### E3
`ALIGNED — no full E3 rerun required.` Owner rules remain business-semantic input; engineering CRUD/API/permission evidence is not treated as sufficient by itself.

### E4
`TARGET ALIGNED; positive evidence set requires narrowing/re-derivation.` Amendment 002 prohibits using equal IDs, field names, or propagation continuity alone to prove common business referent.

**Ordering result**: E-definition alignment is complete enough to conduct this targeted reconnaissance. No finding below authorizes a candidate PASS. Any repaired evidence remains subject to candidate-level re-review.

## 4. C-001 Food findings

### C001-R1 — `food` / `foods` double carrier

**Status: CONFIRMED (local code evidence).**

`PosOrderCreateServiceImpl` POS order transaction performs stock deduction against both physical tables. The `food` deduction is a hard failure path; the `foods` deduction is warning-only. Refund/timeout paths also contain dual-carrier behavior. Startup data preparation includes `food <- foods` synchronization.

Observed division of responsibility:

| Carrier | Observed role |
|---|---|
| `food` | POS menu source; hard inventory deduction gate; legacy/IoT-oriented fields; read by several operational paths |
| `foods` | Current CRUD/master-data surface; pricing/recipe/product-center consumers; soft inventory deduction path |

**Governance statement assessment**: `foods = current truth source / food deprecated` is **PARTIAL**. It is supported for parts of master-data, pricing, and recipe behavior, but contradicted for operational POS menu/inventory hard-gate behavior. No evidence of a complete runtime canonicalization/consistency rule was found.

**Important boundary**: This report does not decide which table should become future canonical source.

### C001-R2 — actual `order_items.foodId` semantics

**Status: PARTIAL / CONTRADICTED as an unconditional claim.**

Local source inspection shows POS payloads originate from `food_code`; backend attempts a `foods.food_code -> foods.food_id` lookup, but on resolution failure logs a warning and may persist `NULL`. There is no database-level FK from `order_items.food_id` to `foods`.

Local data: 6 `order_items` rows were observed, all POS rows, all `product_type=1`, and all had `food_id=NULL`.

Therefore the prior E2 statement `order_items.foodId -> foods / Food` can remain only as a conditional code-path claim: a successfully resolved single-item POS order may reference `foods.food_id`; local stored data does not demonstrate successful references in the six-row sample.

### C001-R3 — `food_trace_code.foodId`

**Status: CONTRADICTED.**

The local database/entity has no `food_trace_code.food_id` column. The actual trace field is `dish_id` (varchar), which stores a Food code/natural-key value. Observed write paths include legacy `food` reads, kitchen JSON-derived values, and direct controller input.

The POS canonical kitchen JSON path does not provide an `id` field in the observed builder, weakening any claim that the trace path is a stable numeric `foodId -> foods` reference.

Required repair: rewrite the E2 traceability row to describe the actual `dish_id = food_code` referent and provenance, without claiming the nonexistent `food_id` field.

### C001-R4 — Recipe referent

**Status: CONFIRMED (local code + data).**

`dish_recipes.food_id` is written/read against the `foods` object. Local sample contained 5 rows, all `food_id` values matching the sole local `foods` row. The legacy `dish_recipe` table had zero rows and no observed backend writer; `dish_inventory` was also empty.

This is currently the cleanest `foods.food_id` referent evidence for C-001.

### C001-R5 — order-item polymorphism

**Status: CONFIRMED / PARTIAL.**

Schema/entity evidence shows `product_type` and `combo_id`. POS primary creation hard-codes `product_type=1` and does not populate `comboId`; management-side order processing contains a Food-vs-Combo branch. A downstream combo ingredient reader was observed reading `food_id` in the combo branch, but no local `product_type=2` row was available to validate the stored-data behavior.

Required E4 scope: positive Order/POS evidence must be limited to demonstrable single-item rows with a successfully resolved Food referent. Combo/package semantics remain FM-002 input and are not decided here.

### C001-R6 — safe E4 scope

For the current candidate re-review, the strongest safe C-001 E4 statements are:

- Recipe: `dish_recipes.food_id -> foods.food_id` is directly evidenced locally.
- Order/POS: only the successfully resolved single-item path may be used as positive referent evidence; local six-row sample does not provide stored successful references.
- Traceability: use the actual `dish_id = food_code` path and explicitly document its source/limitations.
- Exclude NULL order references, combo/package rows, and the legacy POS menu referent from any blanket claim that all Order/POS contexts identify `foods`.

## 5. C-002 Material findings

### C002-R1 — `purchase_request_item.food_id` anomalies

**Status: CONFIRMED facts + one UNRESOLVED_REFERENT.**

Local database sample: 50 rows total; 46 values match `material_archives`; 1 is `NULL`; 2 are empty strings; 1 is numeric `100` outside the local `material_archives` range (max observed id 35).

The 46 matched rows are consistent with Material referents. The four exceptional records are:

| Case | Stored value | Local result | Status |
|---|---|---|---|
| A1 | `100` | Not found in `material_archives`, `foods`, `product`, or legacy `food` id space; value propagates to a purchase-order item as `material_id=100` / `Debug Tomato` | **UNRESOLVED_REFERENT — BLOCKING** |
| A2 | empty string | Parent purchase request completed; no direct referent established | **UNRESOLVED_REFERENT — BLOCKING** |
| A3 | empty string | Parent request draft; no direct referent established | **UNRESOLVED_REFERENT — BLOCKING** |
| A4 | `NULL` | Parent request id itself has no matching purchase-request row | **UNRESOLVED_REFERENT — BLOCKING** |

The cause of A2-A4 being test-data artifacts is only an inference and is not treated as evidence.

The local results do not satisfy the requirement for production verification; production remains BLOCKED.

### C002-R2 — `inventory.material_id` / `product_id`

**Status: CONFIRMED technical conflict; business truth UNRESOLVED / BLOCKING.**

Schema evidence:

- `inventory` contains both `material_id` and `product_id`.
- `product_id` has a database-level FK to `product(product_id)`.
- `material_id` has no DB-level FK.
- The observed ORM `Inventory` model has no independent `product_id` field; `getProductId()/setProductId()` are aliases over the material-id property.

Data evidence:

- 11 inventory rows.
- `material_id` populated in all 11; 10 values match local `material_archives`, one is a local dangling value `999999`.
- One row contains `material_id=1` and `product_id=1`, while local `material_archives.id=1` and `product.id=1` identify different named objects. This is physical evidence of a mixed referent on the same inventory row.

Code evidence additionally shows several legacy inventory write paths using product-id values through the material-id property. This establishes mixed technical semantics in code; it does not determine the intended business truth.

Required governance disposition:

- Engineering provides technical referent, FK, write/read path, representative data, and provenance evidence.
- Business Owner / Product Owner decides the business truth after evidence is available.
- Until that decision, C-002 E2/E4 inventory segment remains conditional and the candidate-level blocking status remains.

### C002-R3 — E2 anchor/propagation classification

**Status: CONFIRMED.**

The six prior C-002 rows should not be described as six independent direct references. The strongest current classification is:

1. `purchase_request_item.food_id`: primary anchor-like path; frontend value originates from Material archive selection but service layer performs no archive validation.
2. `purchase_order_items.material_id`: mixed; manual-order path is anchor-like, generated-from-request path is propagation and can carry invalid/zero values.
3. `receipt_confirmation_items.materialId`: propagation.
4. `inventory.material_id`: propagation from upstream documents in the purchase path; legacy chains can inject product-id values into the material-id carrier.
5. `material_trace_code.materialId`: propagation; one purchase-stock-in listener arm writes null.
6. `material_consumption`: currently non-functional against the observed schema because entity/table columns do not align; local row count is zero.

Therefore the E2 statement should be narrowed to “at least one direct anchor exists, with downstream propagation/corroboration,” not “six independent direct references.”

### C002-R4 — `material_consumption` schema break

**Status: CONFIRMED local engineering evidence.**

The entity maps `product_id`-style fields while the observed database table exposes `material_id` and no `product_id`. The table is empty locally. This is an engineering consistency defect relevant to the current E4 consumption claim; it does not itself decide Material Identity.

## 6. Cross-cutting governance implications

1. C-001's prior `food_trace_code.foodId -> foods` evidence row cannot remain as written; the field does not exist locally.
2. C-001's prior blanket Order/POS referent claim must be narrowed because POS stores can contain NULL `food_id` and the POS menu source remains the legacy `food` table.
3. C-002's E2 six-path table must be reclassified from “independent direct references” to anchor + propagation + one non-functional path.
4. C-002 inventory evidence cannot be converted into a business-truth decision by the reconnaissance lane.
5. The local 4/50 anomalies are not evidence that Material is invalid; they are evidence that the purchase-request referent mapping is not clean enough for an unconditional E2 claim.

## 7. Evidence classes and limits

`LOCAL_REPRODUCED_EVIDENCE` applies to the local PostgreSQL observations in this report. `LOCAL_ONLY` applies to source observations from the uncommitted working tree unless independently reproduced in a committed engineering-evidence document.

Production evidence remains `BLOCKED`. None of the local row counts or proportions may be generalized to production.

The report does not establish:

- which of `food` / `foods` should become canonical;
- which of `inventory.material_id` / `product_id` is the intended business truth;
- any FM closure;
- any Identity, H1/H2, Required Set, Schema, or Migration decision.

## 8. Gate effect

This reconnaissance closes the **execution of the evidence-gathering task**, but it does **not** close the candidate-level gate.

Current candidate state remains:

```text
C-001 = CANDIDATE_PASS_NOT_YET_PROVEN
C-002 = CANDIDATE_PASS_NOT_YET_PROVEN

Required Identity Object Set = NOT_STARTED
H1/H2 = NOT_MADE
Identity Decision = NOT_MADE
Schema/Migration = NO AUTHORIZATION
Production Evidence = BLOCKED
```

Next action is controlled reclassification / targeted re-evaluation of the affected E2/E4 evidence, followed by candidate-level adversarial re-review. No candidate PASS may be inferred from this report alone.
