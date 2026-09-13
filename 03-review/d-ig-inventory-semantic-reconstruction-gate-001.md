# D-IG — Inventory Semantic Reconstruction Gate 001

> **状态**: ACTIVE_REFERENCE — GATE / EXECUTION OVERRIDE
> **Decision ID**: D-IG
> **Scope**: 将 Inventory Semantic Reconstruction 纳入 C-002 后续执行门禁；仅调整执行顺序，不作业务/Identity/Schema 决策。
> **Production Evidence**: BLOCKED
> **Identity Decision**: NOT_MADE
> **Required Identity Object Set**: NOT_STARTED
> **H1/H2**: NOT_MADE
> **Schema Authorization**: NO
> **Migration Authorization**: NO

## 1. Trigger

`INVENTORY-BUSINESS-TRUTH-RECON-001` 已发现 Inventory 在对象边界、引用语义、历史演化、代码映射和数据现实之间存在跨层语义漂移。由于该漂移可能改变 C-002 E2/E4 的测试对象与正证明范围，必须先完成受治理的 Inventory Semantic Reconstruction。

## 2. Execution Effect

从本 Gate 生效起：

```text
C-001
→ 按既有 targeted repair plan 继续，但如发现 Inventory referent 会改变 C-001 E4 proposition，必须触发 targeted retest。

C-002
→ inventory-related E2/E4 re-evaluation 暂缓。
→ 4/50 purchase_request_item anomaly investigation 可以继续。
→ Inventory Semantic Reconstruction 为 C-002 inventory segment 的共同前置输入。
```

该 Gate 不回滚已完成证据、不重跑无关 E-stage，也不授权任何 Schema / Migration / Identity 行动。

## 3. Reconstruction Precedence

若本 Gate 与早期 targeted repair plan 对 C-002 inventory segment 的先后顺序存在冲突，以本 Gate 为准：

```text
Evidence / method validation
        ↓
Inventory Semantic Reconstruction
        ↓
Residual Owner Business Decision
        ↓
C-002 aligned E2/E4 re-evaluation
        ↓
Candidate-level adversarial re-review
```

## 4. Exit Gate

Inventory Semantic Reconstruction 只能通过以下出口之一结束：

1. `BOUNDARY_SUFFICIENTLY_RECONSTRUCTED` — Inventory 业务对象、holding object set、balance grain 及关键 referent 已足够明确；
2. `OWNER_DECISION_REQUIRED` — 证据已充分，但仍存在真正业务所有权命题需要 Owner 裁决；
3. `CANDIDATE_REENTRY_REQUIRED` — 发现新的 identity-grain candidate；
4. `DOMAIN_RECONSTRUCTION_PHASE_2` — 现有证据仍显示更大范围的 Inventory 语义域未能闭合；
5. `EVIDENCE_INSUFFICIENT` — 关键证据仍不足，保留命名缺口，不强行下结论。

任何出口均不等于 Identity PASS。

## 5. Safety Rules

- Engineering observation ≠ business truth。
- Historical migration shape ≠ current business meaning。
- Field/FK existence ≠ business object existence。
- Same-row dual reference ≠ business permission for dual identity。
- No new classification mechanism may be designed during reconstruction merely to remove uncertainty.
- `Product Inventory` remains business-unproven until the reconstruction produces direct business evidence or a formal Owner requirement.
- `Material Inventory` existing evidence may be retained, narrowed, or reclassified by the reconstruction; 100% local field population alone is not sufficient to settle the full Inventory domain semantics.

## 6. Required Downstream Revalidation

After reconstruction, explicitly revalidate:

- C-002 E2 direct-reference proposition and positive evidence set;
- C-002 E4 stable-referent scope across procurement, receipt, inventory, transaction, consumption and related paths;
- C-002 inventory evidence in Candidate-level Disposition;
- whether C-003/Product needs candidate re-entry or a Requirement-driven candidate;
- whether FM-003 or other boundary registers require update;
- whether existing `inventory.material_id` / `inventory.product_id` statements remain KEEP, NARROW, INVALIDATE, SUPERSEDE, or UNRESOLVED.

## 7. Non-Effect

This Gate does not:

- declare Inventory a canonical Identity;
- declare Material/Product/Food separation;
- select Q1/Q2/Q3 for Owner;
- create `inventory_type` or any other schema element;
- authorize migration or refactoring;
- change Production Evidence from BLOCKED.
