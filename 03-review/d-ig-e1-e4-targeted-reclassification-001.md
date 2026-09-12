# D-IG — E1–E4 Targeted Evidence Reclassification 001

> **Status**: ACTIVE_REFERENCE — TARGETED TEST RECLASSIFICATION
> **Decision ID**: D-IG
> **Purpose**: 根据 `d-ig-targeted-evidence-reconnaissance-report-001.md` 对 C-001/C-002 已执行 E1–E4 记录进行定向证据重定级；不新增 Identity Decision。
> **Production Evidence**: BLOCKED
> **Identity Decision**: NOT_MADE
> **Required Identity Object Set**: NOT_STARTED
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO

## 1. Precondition

E-definition alignment 已依据 Charter Amendment 002 完成。现有测试的定义目标无需整体重跑；但部分原始 evidence claim 被本地可复现证据推翻或显著收窄，因此必须进行 targeted reclassification / re-evaluation。

本记录不把本地代码/DB 自动升级为生产事实，也不代替 Business Owner / Product Owner 作业务真相裁决。

## 2. C-001 Food — targeted reclassification

### 2.1 E2 original claim: `food_trace_code.foodId -> foods`

**Prior status**: PASS evidence row.

**New status**: `CONTRADICTED — REMOVE AS STATED`.

Local database/entity evidence shows `food_trace_code.food_id` does not exist. The actual trace referent is `dish_id` (varchar) carrying a Food-code/natural-key value.

**Required replacement claim**:

- Traceability evidence may be used only as `dish_id = food_code` reference semantics;
- provenance and POS-origin stability are not equivalent to a numeric `foodId -> foods` FK/reference;
- the earlier field assertion must not remain in the E2 proof table.

**Effect on E2**: The prior trace row cannot support E2 as written. E2 must rely on the surviving direct-reference evidence, if any, after scope/provenance review; trace remains corroborative evidence only.

### 2.2 E2 original claim: `order_items.foodId -> foods`

**New status**: `PARTIAL / CONDITIONAL`.

Local code shows a Food-code-to-`foods.food_id` resolution attempt, but failed resolution can persist NULL and there is no DB-level FK. Local sample has 6/6 `order_items.food_id = NULL`.

**Required replacement claim**:

> A successfully resolved single-item order path may reference `foods.food_id`; the local sample does not demonstrate successful stored references.

This must not be written as an unconditional statement that Order/POS rows reference `foods`.

### 2.3 E4 Order/POS scope

**New status**: `SCOPE RESTRICTION REQUIRED`.

Positive E4 evidence is restricted to demonstrable single-item rows with a successfully resolved Food referent. Combo/package rows and NULL-reference rows are excluded from the positive claim. Menu-source behavior through the legacy `food` table must not be silently merged into the `foods` referent claim.

### 2.4 C-001 double-carrier finding

`food` and `foods` are both active technical carriers in the local runtime, with asymmetric failure semantics and different consumers. The governance proposition `foods = current truth source / food deprecated` is therefore `PARTIAL`, not unconditional.

This is a referent-reconciliation issue. No future canonical-table decision is made here.

## 3. C-002 Material — targeted reclassification

### 3.1 E2 purchase-request evidence

**New status**: `PARTIAL / BLOCKING FOR UNCONDITIONAL FORMULATION`.

Local database confirms 46/50 purchase-request values resolve to `material_archives`, while four records are anomalous. One value (`100`) remains an `UNRESOLVED_REFERENT`; two are empty strings and one is NULL with an orphan parent request.

The four records cannot be treated as Material merely because the column is named `food_id` or because nearby numeric IDs exist.

The E2 purchase-request line must therefore be rewritten as:

> Primary anchor-like Material reference path, strongly evidenced for the 46 resolvable records, with four unresolved/invalid referent cases that remain blocking for an unconditional population-level claim.

### 3.2 E2 six-path classification

The prior “six independent direct references” formulation is superseded by:

- primary anchor-like: `purchase_request_item.food_id`;
- anchor-like / mixed: manually created `purchase_order_items.material_id`;
- propagation: generated purchase-order items, receipt confirmation, inventory purchase path, trace code path;
- non-functional against observed local schema: `material_consumption`.

Propagation may corroborate stable referent continuity but must not be described as independently originated direct references.

### 3.3 Inventory dual-column conflict

**Status**: `CONFIRMED TECHNICAL CONFLICT / BUSINESS TRUTH UNRESOLVED`.

Local schema confirms `inventory.product_id -> product(product_id)` via DB FK; `material_id` has no FK. Local row evidence confirms a row where `material_id=1` identifies a Material object while `product_id=1` identifies a different Product object.

The observed ORM and write paths further show legacy mixed semantics, but the intended business truth remains an Owner decision.

**Required next action**:

- Engineering supplies the evidence package and provenance;
- Business Owner / Product Owner decides which referent is authoritative for the relevant inventory business behavior;
- until that decision, C-002 E2/E4 inventory evidence remains conditional and candidate-level blocking persists.

### 3.4 E4 consumption evidence

`material_consumption` is currently non-functional against the observed local schema because the ORM/entity mapping expects `product_id`-style fields while the table exposes `material_id` and no `product_id`. The local table is empty.

This does not itself invalidate Material Identity, but it prevents treating the consumption path as a fully evidenced live continuity path. FM-003 remains an explicitly named later-stage dependency.

## 4. E-stage result handling

This record distinguishes three outcomes:

- **Evidence-row contradicted** → remove/replace the specific evidence claim; do not silently preserve it.
- **Evidence-row narrowed/conditional** → retain only the precise supported proposition.
- **Test target itself changed** → rerun the affected E-stage.

For the current reconnaissance, E1/E3 targets remain aligned; C-001 E2/E4 and C-002 E2/E4 require evidence-table reclassification and candidate-level re-evaluation. A full re-run of every E-stage is not required solely because evidence rows were corrected.

## 5. Gate impact

The reconnaissance task is complete. The targeted reclassification record is now the formal bridge from local evidence to candidate-level review.

Current state remains:

```text
C-001 = CANDIDATE_PASS_NOT_YET_PROVEN
C-002 = CANDIDATE_PASS_NOT_YET_PROVEN

C-001: E2/E4 evidence scope requires repair
C-002: E2/E4 evidence scope requires repair + inventory business-truth decision

Required Identity Object Set = NOT_STARTED
H1/H2 = NOT_MADE
Identity Decision = NOT_MADE
Schema/Migration = NO AUTHORIZATION
Production Evidence = BLOCKED
```

No candidate-level PASS is implied by this reclassification.
