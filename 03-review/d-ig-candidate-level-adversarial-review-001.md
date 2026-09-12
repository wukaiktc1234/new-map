# D-IG — Candidate-level Adversarial Review 001

> **状态**: ACTIVE_REFERENCE — GOVERNANCE TEST RECORD
> **Decision ID**: D-IG
> **适用候选**: C-001 Food / C-002 Material
> **阶段**: Candidate-level review, after E1–E4
> **Identity Decision**: NOT_MADE
> **Required Identity Object Set**: NOT_STARTED
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO
> **Production Evidence**: BLOCKED
> **Requirement-Driven Status**: REQUIREMENT_DRIVEN_INCOMPLETE

## 1. Purpose

本记录用于执行 C-001 Food 与 C-002 Material 的 **candidate-level adversarial review**。

本阶段不是重新执行 E1–E4，也不是最终 Identity Decision。其目的，是在四个 E-stage 均通过后，继续尝试寻找能够推翻候选级结论的跨阶段逻辑漏洞、隐藏前提、证据边界问题以及候选间关系泄漏。

当前 E-stage 基线来自 `03-review/d-ig-e1-e4-testing-001.md` 与 `03-review/d-ig-e3-reevaluation-001.md`：

```text
C-001 Food
E1 PASS / E2 PASS / E3 PASS / E4 PASS

C-002 Material
E1 PASS / E2 PASS / E3 PASS / E4 PASS
```

E3 的 Owner business rules 已单独记录于 `03-review/d-ig-e3-owner-decision-001.md`；E3 复评记录为 PASS。该 Owner 输入不自动推出最终 Identity。

## 2. Candidate-level gate

Candidate-level conclusion 必须遵守既有 E3 Gate 所固化的逻辑：

```text
PASS iff E1 = PASS and E2 = PASS and E3 = PASS and E4 = PASS
FAIL iff 任一 E-stage = FAIL
INCONCLUSIVE iff 未满足 PASS/FAIL 且至少一个 E-stage = INCONCLUSIVE
BLOCKED iff 必要前置条件或证据状态明确阻断判定
```

但本记录进一步要求：即使满足四个 E-stage PASS 条件，也必须先完成本候选级反方审查，才可写入 `candidate-level conclusion = PASS`。四个 PASS 不得被视为自动通关。

## 3. Source boundary

### 3.1 Primary governance sources

- `03-review/d-ig-e1-e4-testing-001.md`
- `03-review/d-ig-e3-owner-rule-gate-001.md`
- `03-review/d-ig-e3-owner-decision-001.md`
- `03-review/d-ig-e3-reevaluation-001.md`
- `03-review/d-ig-e0-prescreen-result-001.yaml`
- `03-review/d-ig-e0-candidate-enumeration-001.yaml`
- `03-review/d-ig-business-capability-set-v2-logical-001.md`
- `03-review/business-item-canonical-independent-review.md`

### 3.2 Evidence discipline

- Production evidence remains `BLOCKED`.
- Local-only observations remain local-only unless reproduced in a locatable submitted repository evidence document.
- Engineering facts must not be silently upgraded to business facts.
- Owner business rules may define business meaning, but do not by themselves prove engineering implementation.
- Lack of evidence must not be converted into evidence of non-existence.
- Existing `PASS` results are inputs to attack, not conclusions that the reviewer is required to defend.

Capability Set v2 remains logically established but requirement-driven status remains incomplete and production evidence remains blocked.

## 4. Review dimensions

The executor must attempt attacks across at least these dimensions:

1. **Cross-stage composition** — each E passes individually, but the combined claim still may not hold.
2. **E1 circularity / compression** — the inability to replace a reference target may already presuppose Identity.
3. **E2 reference semantics** — technical foreign-key/ID linkage may not equal business Identity.
4. **E3 lifecycle inflation** — Owner-defined state semantics may establish lifecycle without necessarily establishing Identity.
5. **E4 semantic consistency** — apparently shared references across capabilities may conceal distinct contexts or roles.
6. **Candidate-level sufficiency** — check whether a cross-stage property is missing from E1–E4.
7. **Candidate-to-candidate leakage** — ensure Food and Material are not treated as separate or same Identity merely because one candidate passed.
8. **Evidence provenance** — distinguish repository evidence, documented local evidence, historical evidence, and unresolved gaps.
9. **Boundary interaction** — determine whether FM-001–FM-004 can safely remain later-stage issues or whether any one already blocks candidate validity.
10. **Status separation** — candidate PASS must not be upgraded into Required Identity Object Set, H1/H2, Schema, Migration, or Identity Decision.

## 5. C-001 Food — adversarial review workspace

### 5.1 Current candidate-level input

`E1 PASS / E2 PASS / E3 PASS / E4 PASS`

The underlying E-stage record documents Food as the current documented Food truth source, direct order reference `order_items.foodId`, a separate traceability reference path, independent Food API/CRUD evidence, and cross-capability semantic consistency within the evidenced set.

### 5.2 Required attacks

| Attack ID | Attack target | Question | Status |
|---|---|---|---|
| C001-CL-01 | Cross-stage composition | Can E1/E2/E3/E4 all pass while the overall claim “Food is an independent candidate object” still fails? | PENDING_EXTERNAL_REVIEW |
| C001-CL-02 | E1 circularity | Does “order_items needs a stable object” secretly assume that object must be Identity rather than merely an implementation target? | INTERNAL_NONFATAL_CHECKED |
| C001-CL-03 | E2 reference semantics | Are `order_items.foodId` and `food_trace_code.foodId` genuinely independent business references, or only one fact propagated twice? | INTERNAL_NONFATAL_CHECKED |
| C001-CL-04 | E3 lifecycle inflation | Do Owner rules prove independent lifecycle, or merely define behavior for an existing record? | INTERNAL_NONFATAL_CHECKED |
| C001-CL-05 | E4 semantic split | Could Order, Traceability, Recipe, or Menu refer to different business concepts despite shared naming/reference? | INTERNAL_NONFATAL_CHECKED |
| C001-CL-06 | Missing context | Does an untested confirmed capability create a candidate-level blocking gap? | INTERNAL_NONFATAL_CHECKED |
| C001-CL-07 | Food/Material leakage | Does Food PASS implicitly rely on an unresolved Food/Material boundary? | INTERNAL_NONFATAL_CHECKED |
| C001-CL-08 | Evidence provenance | Are any positive premises dependent only on LOCAL_ONLY evidence? | INTERNAL_NONFATAL_CHECKED |

### 5.3 Internal executor findings — C-001

This section records the executor's own first-pass adversarial review. It is an independent review lane inside this record, not the external-model verdict.

#### C001-CL-02 — E1 circularity

**Attack:** The E1 argument might be circular if “the order item needs an independently addressable object” is simply another way of assuming that Food must be an Identity.

**Finding:** No fatal circularity identified in the current E1 formulation. The test premise is narrower: preserve the confirmed business behavior without introducing an equivalent independent object under another name. It does not require the replacement object to be called Identity. The E1 test therefore establishes non-compressibility of the stable referential role, not the final name or ontology of that object.

**Disposition:** `NON_FATAL / WORDING-SAFE`; external review still required.

#### C001-CL-03 — E2 reference semantics

**Attack:** `order_items.foodId` and `food_trace_code.foodId` might be only one fact propagated twice, so the second path should not be treated as an independent proof source.

**Finding:** The attack does not defeat E2. E2 requires evidence that a confirmed business capability directly references the candidate as a stable object. A single strong direct-reference path is sufficient to satisfy that criterion. The traceability path is corroborating evidence of propagation and semantic persistence; it is not required to be statistically or causally independent from the order path.

**Disposition:** `NON_FATAL`; record should not imply that E2 PASS depends on two fully independent reference sources.

#### C001-CL-04 — E3 lifecycle inflation

**Attack:** Owner-defined state rules may define behavior for an existing master record without proving Identity.

**Finding:** Correct as a limitation, but not fatal at candidate level. E3 is not used alone as Identity proof. The combined candidate claim depends on E1/E2/E3/E4 together. Owner rules establish business lifecycle semantics needed for E3; engineering evidence establishes an independently addressable object surface. The candidate review must continue to ensure the combined claim does not contain hidden circularity.

**Disposition:** `NON_FATAL`; preserve explicit “E3 alone does not decide Identity” boundary.

#### C001-CL-05 — E4 semantic split

**Attack:** Order, Traceability, Recipe, Menu and other contexts could refer to different concepts despite shared naming or entry points.

**Finding:** No evidence-supported semantic split currently defeats the candidate. Order and traceability explicitly use the same Food referent in the submitted evidence; recipe behavior is attached to the Food path, while missing exact-field evidence is intentionally not upgraded. Menu/POS/other contexts with insufficient direct-field evidence are excluded from positive proof rather than assumed equivalent. This is an evidence limitation, not a demonstrated contradiction.

**Disposition:** `NON_FATAL / SCOPE_LIMITATION`; external review should still attempt stronger counterexamples.

#### C001-CL-06 — Missing confirmed capability context

**Attack:** A confirmed capability not directly tested could reveal a different object grain and block candidate validity.

**Finding:** Current Capability Set v2 is the established logical governance input, with Owner completeness review complete. The remaining requirement-driven incompleteness and UNKNOWN capability checks are not, by themselves, evidence that Food has a contradictory identity grain. No specific confirmed capability has been located that necessarily invalidates Food's candidate claim.

**Disposition:** `NON_FATAL_PENDING_NO_SPECIFIC_COUNTEREVIDENCE`; if a concrete capability dependency is later identified, reopen.

#### C001-CL-07 — Food/Material boundary leakage

**Attack:** Food candidate PASS might secretly depend on already deciding Food/Material separation.

**Finding:** Candidate validity does not require deciding the final relationship between Food and Material. The current evidence can establish Food's own necessity properties without deciding whether some real-world or contextual items can participate in both domains. FM-001–FM-004 therefore remain a separate later-stage boundary review unless a concrete dependency is shown.

**Disposition:** `LATER_STAGE / NON_FATAL_CURRENTLY`.

#### C001-CL-08 — Evidence provenance

**Attack:** Positive Food conclusions may depend only on LOCAL_ONLY evidence.

**Finding:** No fatal sole-local dependency identified for the core E1/E2/E3 candidate claim. The core order reference and main Food object evidence are represented in submitted governance documents. Some traceability/recipe detail is local-only or incompletely exposed, but those details are corroborative or scope-limited rather than the sole basis of PASS.

**Disposition:** `NON_FATAL`; candidate remains documentary/local-evidence-based and not production verified.

### 5.4 Internal preliminary synthesis — C-001

Current executor review has not found a demonstrated attack that invalidates the C-001 candidate claim. The strongest remaining question is **candidate-level sufficiency** itself: whether E1–E4 collectively cover all necessary cross-stage conditions without a missing required test. This remains open to external adversarial review.

**Internal lane provisional assessment:** `CANDIDATE_PASS_SURVIVES_INTERNAL_FIRST_PASS`

This is not the final candidate-level verdict.

## 6. C-002 Material — adversarial review workspace

### 6.1 Current candidate-level input

`E1 PASS / E2 PASS / E3 PASS / E4 PASS`

The underlying E-stage record documents `material_archives` as the Material truth source and repeated `materialId` references across procurement, receipt, inventory, traceability and consumption, together with independent Material CRUD/Read/API/permission evidence.

### 6.2 Required attacks

| Attack ID | Attack target | Question | Status |
|---|---|---|---|
| C002-CL-01 | Cross-stage composition | Can all four E-stage PASS results coexist with failure of the overall candidate claim? | PENDING_EXTERNAL_REVIEW |
| C002-CL-02 | E1 circularity | Does “stable material reference is necessary” already assume an independent Material Identity? | INTERNAL_NONFATAL_CHECKED |
| C002-CL-03 | E2 reference semantics | Are procurement/receipt/inventory/traceability/consumption references distinct business evidence or propagation of one technical identifier? | INTERNAL_NONFATAL_CHECKED |
| C002-CL-04 | E3 lifecycle inflation | Does Material’s confirmed inactive rule prove Identity, or only a governed master-data record? | INTERNAL_NONFATAL_CHECKED |
| C002-CL-05 | E4 semantic split | Could supplier material, stock material, consumed material and trace material be related-but-distinct concepts? | INTERNAL_NONFATAL_CHECKED |
| C002-CL-06 | Legacy label attack | Could `product_id` represent a genuine second business object rather than legacy/wrong semantic labeling? | INTERNAL_NONFATAL_CHECKED / LATER_STAGE_BOUNDARY |
| C002-CL-07 | Food/Material leakage | Does Material PASS rely on Food having already been conceptually separated? | INTERNAL_NONFATAL_CHECKED |
| C002-CL-08 | Evidence provenance | Are any positive premises dependent only on LOCAL_ONLY or historical evidence? | INTERNAL_NONFATAL_CHECKED |

### 6.3 Internal executor findings — C-002

#### C002-CL-02 — E1 circularity

**Finding:** No fatal circularity identified. The Material E1 test is about whether cross-stage referential behavior can be preserved without an equivalent independently addressable object. It does not require the replacement to retain the name “Material.”

**Disposition:** `NON_FATAL`; external review required.

#### C002-CL-03 — E2 reference semantics

**Attack:** All materialId occurrences may be propagation of one technical identifier rather than multiple independent business proofs.

**Finding:** This does not defeat E2. Repeated propagation is in fact evidence that a stable referential meaning survives multiple business stages. E2 need not require independently-originated references in every stage. A single direct business reference is sufficient; multiple propagation paths strengthen the conclusion that the same referent persists across contexts.

**Disposition:** `NON_FATAL`.

#### C002-CL-04 — E3 lifecycle inflation

**Finding:** Owner rules establish the business consequences of Material INACTIVE and historical-reference preservation. That supports independent lifecycle semantics, but E3 alone is not an Identity proof. No contradiction with the combined E1/E2/E4 claim has been found.

**Disposition:** `NON_FATAL`.

#### C002-CL-05 — E4 semantic split

**Attack:** Supplier material, received material, stocked material, consumed material and traceability material could be related but distinct concepts.

**Finding:** Current submitted propagation evidence keeps the same `materialId` tied to the same `material_archives` truth source across procurement, receipt, inventory, traceability and consumption. No evidence-supported semantic split was located within the tested paths. The conclusion remains limited to evidenced paths; it does not claim every possible material-related context is already resolved.

**Disposition:** `NON_FATAL / SCOPE_LIMITATION`.

#### C002-CL-06 — Legacy `product_id` attack

**Attack:** `product_id` might represent a real second business object, which would undermine the claim that Material is the stable referent.

**Finding:** This is a real architectural/data conflict worth preserving, but it does not presently invalidate the Material candidate claim. The independent review evidence reports concrete local cases where `product_id=1` and `material_id=1` point to different objects. That demonstrates a technical semantic conflict, not that Material is not independently necessary. The issue belongs to the Product/Material boundary and later normalization/repair analysis unless new evidence shows Material references are actually semantically misclassified.

**Disposition:** `LATER_STAGE_BOUNDARY / NON_FATAL_CURRENTLY`.

#### C002-CL-07 — Food/Material boundary leakage

**Finding:** Material candidate necessity can be tested on procurement/inventory/traceability/consumption behavior without deciding whether some physical item can appear in Food and Material contexts. FM-001–FM-004 remain separate unless a concrete cross-candidate contradiction affects Material's own necessity claim.

**Disposition:** `LATER_STAGE / NON_FATAL_CURRENTLY`.

#### C002-CL-08 — Evidence provenance

**Finding:** Core Material reference and propagation claims are represented in submitted engineering/governance maps. Local instance evidence strengthens the picture but is not the sole basis for the candidate claim. Production verification remains blocked.

**Disposition:** `NON_FATAL`.

### 6.4 Internal preliminary synthesis — C-002

Current executor review has not found a demonstrated attack that invalidates the C-002 candidate claim. The strongest remaining question is again candidate-level sufficiency and whether the current E1–E4 method requires an additional cross-stage test.

**Internal lane provisional assessment:** `CANDIDATE_PASS_SURVIVES_INTERNAL_FIRST_PASS`

This is not the final candidate-level verdict.

## 7. Candidate-level cross-checks

The following are mandatory review questions before either candidate may be marked PASS:

### 7.1 Cross-stage consistency

Can the same candidate satisfy the meaning implied by E1, E2, E3 and E4 simultaneously without introducing a hidden new object, hidden role change, or context-specific redefinition?

### 7.2 Identity inflation check

The reviewer must not use any of the following as automatic proof:

- stable technical ID;
- foreign-key/reference existence;
- CRUD/API existence;
- Owner-defined lifecycle state;
- repeated usage across multiple tables.

Each may be evidence, but none alone is an Identity conclusion.

### 7.3 Candidate-level sufficiency check

Determine whether E1–E4 collectively cover the necessary dimensions for candidate validity, or whether a missing cross-stage test is required.

A newly identified test should be classified as:

- **blocking** — candidate cannot be concluded without it;
- **targeted repair** — must be addressed before PASS but does not invalidate prior E-stage results;
- **later-stage issue** — may safely remain for Required Identity Object Set / FM boundary / H1/H2;
- **non-blocking enhancement** — improves assurance but is not needed for candidate conclusion.

### 7.4 Food/Material boundary leakage

FM-001–FM-004 remain a separate boundary register. The reviewer must identify whether any specific FM issue is already logically necessary for candidate validity. If not, it remains later-stage. If yes, the review must name the exact dependency rather than broadly reopening the entire boundary.

### 7.5 Production evidence separation

Production evidence remains blocked. A candidate PASS may only be documentary/local-evidence-based at this stage and must not be described as production verified.

## 8. Final verdict rule

No final verdict is pre-filled in this record.

The executor must select exactly one verdict for each candidate:

```text
CANDIDATE_PASS_SURVIVES
CANDIDATE_PASS_NOT_YET_PROVEN
CANDIDATE_PASS_IS_INVALID
CANDIDATE_REVIEW_BLOCKED
```

A verdict must distinguish:

- an attack that actually defeats candidate validity;
- an unresolved evidence gap that prevents conclusion;
- a non-blocking improvement suggestion;
- a later-stage relationship issue that does not invalidate the candidate itself.

## 9. Governance safety

Regardless of review outcome:

- no candidate-level result may directly create a Canonical Identity decision;
- no candidate-level result may authorize Schema or Migration;
- no candidate-level result may close FM-001–FM-004 without the dedicated boundary review;
- no local evidence may be promoted to production verification;
- an adversarial reviewer may invalidate or block a prior claim, but may not create a Business Owner decision.

## 10. Current state

```yaml
D-IG: OPEN
C-001_E1_E4: PASS_SET
C-002_E1_E4: PASS_SET
C-001_internal_adversarial_lane: PROVISIONAL_SURVIVES
C-002_internal_adversarial_lane: PROVISIONAL_SURVIVES
C-001_candidate_level: PENDING_EXTERNAL_ADVERSARIAL_REVIEW
C-002_candidate_level: PENDING_EXTERNAL_ADVERSARIAL_REVIEW
Required_Identity_Object_Set: NOT_STARTED
FM-001_to_FM-004: OPEN
H1_H2: NOT_MADE
Identity_Decision: NOT_MADE
Schema: BLOCKED
Migration: BLOCKED
Production_Evidence: BLOCKED
Requirement_Driven: INCOMPLETE
```
