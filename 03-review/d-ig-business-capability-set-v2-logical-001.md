# D-IG Capability Set v2 — Logical Governance Record 001

- Decision: D-IG
- Status: ACTIVE_REFERENCE
- Physical capability set: `03-review/d-ig-business-capability-set-001.yaml` (V1 baseline)
- Revision record: `03-review/d-ig-business-capability-set-review-001.md`
- Logical v2 status: `ESTABLISHED_FROM_V1_PLUS_REVIEW_001`
- Adversarial review status: `PENDING`
- Owner completeness review: `BLOCKED_PENDING_ADVERSARIAL_REVIEW`
- Requirement-driven status: `REQUIREMENT_DRIVEN_INCOMPLETE`
- Production evidence status: `BLOCKED`
- Pre-Screen authorization: `BLOCKED`
- Identity decision: `NOT_MADE`
- H1/H2 decision: `NOT_MADE`
- Schema authorization: `NO`
- Migration authorization: `NO`

## 1. Definition

For the current governance step, **Capability Set v2 (LOGICAL)** is the deterministic governance composition of:

```text
V1 Capability Set
+ D-IG Business Capability Set Review-001 mandatory dispositions
= Capability Set v2 (LOGICAL)
```

This record is authoritative for the fact that the reviewed v2 composition exists for governance sequencing. It does **not** replace the V1 YAML as the physical baseline and does **not** authorize schema, migration, Pre-Screen, E1-E4, or Identity decisions.

## 2. Mandatory v2 composition

The logical v2 composition applies exactly these Review-001 dispositions:

- `CAP-IMPL-030`: 1 -> 2 IMPLEMENTED capabilities:
  - `Maintain employee attendance records`
  - `Process employee salary records`
- `CAP-IMPL-031`: 1 -> 4 IMPLEMENTED capabilities:
  - `Maintain member records`
  - `Manage member levels`
  - `Manage member coupon operations`
  - `Process member recharge operations`
- Points remain `EVIDENCE_CHECK_REQUIRED` and are not counted in v2 until an independent business behavior path is evidenced.
- `CAP-IMPL-026`: 1 -> 2 IMPLEMENTED capabilities:
  - `Record receipts`
  - `Record fund flows`
- `UNKNOWN-008` is removed from Capability Set UNKNOWN boundaries and preserved only as E0 candidate implementation status.
- Recipe, Pricing, Inventory Count/Adjustment, and Inventory Transaction Query/Trace remain explicit UNKNOWN capability checks and are not promoted to IMPLEMENTED.

## 3. Counts

```text
IMPLEMENTED = 32 - 3 + 2 + 4 + 2 = 37

Potential future IMPLEMENTED after independent points evidence = 37 + 1 = 38

UNKNOWN business-capability boundaries = 7
```

Count changes are bookkeeping effects of the Review-001 split dispositions only; they do not create or modify Identity conclusions.

## 4. Owner review basis

The next completeness review may use this logical v2 record together with:

1. the untouched V1 physical baseline YAML; and
2. the complete Review-001 remediation record.

The Owner review must not be interpreted as requirement confirmation where requirement-driven source records remain unavailable. `REQUIREMENT_DRIVEN_INCOMPLETE` remains in force.

## 5. Sequencing state

```yaml
D-IG: OPEN
Capability_Set: V2_LOGICAL_ESTABLISHED_PENDING_ADVERSARIAL_REVIEW
Owner_Review: BLOCKED_PENDING_ADVERSARIAL_REVIEW
Requirement_Driven: INCOMPLETE
Production_Evidence: BLOCKED
Pre_Screen: BLOCKED
E1_E4: BLOCKED
Schema: BLOCKED
Migration: BLOCKED
Identity_Decision: NOT_MADE
```

## 6. Physical v2 plan

A physical V2 YAML is intentionally deferred to a local byte-preserving patch operation. The planned patch must modify only the agreed metadata/count fields and the three split capability blocks plus removal of `UNKNOWN-008`, while preserving all unrelated V1 evidence text byte-for-byte.
