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

E3 的 Owner business rules 已单独记录于 `03-review/d-ig-e3-owner-decision-001.md`；E3 复评记录为 PASS。该 Owner 输入不自动推出最终 Identity。fileciteturn171file0 fileciteturn173file0

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

Capability Set v2 remains logically established but requirement-driven status remains incomplete and production evidence remains blocked. fileciteturn172file0

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

The underlying E-stage record documents Food as the current documented Food truth source, direct order reference `order_items.foodId`, a separate traceability reference path, independent Food API/CRUD evidence, and cross-capability semantic consistency within the evidenced set. fileciteturn163file0

### 5.2 Required attacks

| Attack ID | Attack target | Question | Status |
|---|---|---|---|
| C001-CL-01 | Cross-stage composition | Can E1/E2/E3/E4 all pass while the overall claim “Food is an independent candidate object” still fails? | PENDING_EXECUTION |
| C001-CL-02 | E1 circularity | Does “order_items needs a stable object” secretly assume that object must be Identity rather than merely an implementation target? | PENDING_EXECUTION |
| C001-CL-03 | E2 reference semantics | Are `order_items.foodId` and `food_trace_code.foodId` genuinely independent business references, or only one fact propagated twice? | PENDING_EXECUTION |
| C001-CL-04 | E3 lifecycle inflation | Do Owner rules prove independent lifecycle, or merely define behavior for an existing record? | PENDING_EXECUTION |
| C001-CL-05 | E4 semantic split | Could Order, Traceability, Recipe, or Menu refer to different business concepts despite shared naming/reference? | PENDING_EXECUTION |
| C001-CL-06 | Missing context | Does an untested confirmed capability create a candidate-level blocking gap? | PENDING_EXECUTION |
| C001-CL-07 | Food/Material leakage | Does Food PASS implicitly rely on an unresolved Food/Material boundary? | PENDING_EXECUTION |
| C001-CL-08 | Evidence provenance | Are any positive premises dependent only on LOCAL_ONLY evidence? | PENDING_EXECUTION |

## 6. C-002 Material — adversarial review workspace

### 6.1 Current candidate-level input

`E1 PASS / E2 PASS / E3 PASS / E4 PASS`

The underlying E-stage record documents `material_archives` as the Material truth source and repeated `materialId` references across procurement, receipt, inventory, traceability and consumption, together with independent Material CRUD/Read/API/permission evidence. fileciteturn163file0

### 6.2 Required attacks

| Attack ID | Attack target | Question | Status |
|---|---|---|---|
| C002-CL-01 | Cross-stage composition | Can all four E-stage PASS results coexist with failure of the overall candidate claim? | PENDING_EXECUTION |
| C002-CL-02 | E1 circularity | Does “stable material reference is necessary” already assume an independent Material Identity? | PENDING_EXECUTION |
| C002-CL-03 | E2 reference semantics | Are procurement/receipt/inventory/traceability/consumption references distinct business evidence or propagation of one technical identifier? | PENDING_EXECUTION |
| C002-CL-04 | E3 lifecycle inflation | Does Material’s confirmed inactive rule prove Identity, or only a governed master-data record? | PENDING_EXECUTION |
| C002-CL-05 | E4 semantic split | Could supplier material, stock material, consumed material and trace material be related-but-distinct concepts? | PENDING_EXECUTION |
| C002-CL-06 | Legacy label attack | Could `product_id` represent a genuine second business object rather than legacy/wrong semantic labeling? | PENDING_EXECUTION |
| C002-CL-07 | Food/Material leakage | Does Material PASS rely on Food having already been conceptually separated? | PENDING_EXECUTION |
| C002-CL-08 | Evidence provenance | Are any positive premises dependent only on LOCAL_ONLY or historical evidence? | PENDING_EXECUTION |

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
C-001_candidate_level: PENDING_CANDIDATE_ADVERSARIAL_REVIEW
C-002_candidate_level: PENDING_CANDIDATE_ADVERSARIAL_REVIEW
Required_Identity_Object_Set: NOT_STARTED
FM-001_to_FM-004: OPEN
H1_H2: NOT_MADE
Identity_Decision: NOT_MADE
Schema: BLOCKED
Migration: BLOCKED
Production_Evidence: BLOCKED
Requirement_Driven: INCOMPLETE
```