# D-IG Capability Set v2 — Logical Governance Record 001

- Decision: D-IG
- Status: ACTIVE_REFERENCE
- Physical capability set: `03-review/d-ig-business-capability-set-001.yaml` (V1 baseline)
- Revision record: `03-review/d-ig-business-capability-set-review-001.md`
- Logical v2 status: `ESTABLISHED_FROM_V1_PLUS_REVIEW_001`
- Adversarial review status: `READY_FOR_FINAL_QUICK_CHECK`
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
- `CAP-IMPL-026`: 1 -> 2 IMPLEMENTED capabilities:
  - `Record receipts`
  - `Record fund flows`
- `UNKNOWN-008` is removed from Capability Set UNKNOWN boundaries and preserved only as E0 candidate implementation status.

### 2.1 UNKNOWN capability checks

The term **UNKNOWN capability check** is a distinct classification from **UNKNOWN business-capability boundary**.

- An **UNKNOWN business-capability boundary** is an existing entry in the V1 `unknown_boundaries` collection whose business-capability boundary itself is known to be relevant but whose implementation/owner status remains unresolved.
- An **UNKNOWN capability check** is a newly registered evidence-gap check for a capability boundary that was not represented in the V1 capability collections and therefore must not be counted as an existing UNKNOWN boundary merely by being discovered during Review-001.
- `unknown_boundary_count` counts only the former collection.
- `unknown_capability_check_count` counts only the latter collection.
- Neither count is an IMPLEMENTED capability count and neither count makes an Identity decision.

V2 registers these **new** UNKNOWN capability checks:

| Check ID | Capability boundary | Classification |
|---|---|---|
| `UCC-001` | Recipe | `UNKNOWN_REQUIRES_BEHAVIOR_PATH` |
| `UCC-002` | Pricing | `UNKNOWN_REQUIRES_OWNER_AND_BEHAVIOR_EVIDENCE` |
| `UCC-003` | Inventory Count / Adjustment | `UNKNOWN_REQUIRES_BEHAVIOR_PATH` |
| `UCC-004` | Inventory Transaction Query / Trace | `UNKNOWN_REQUIRES_BEHAVIOR_PATH` |
| `UCC-005` | Member Points / Loyalty Points | `EVIDENCE_CHECK_REQUIRED` |

These five checks are v2 evidence-gap records only. They are not backfilled into the V1 `unknown_boundaries` collection and therefore do not change `unknown_boundary_count`.

## 3. Counts

```text
IMPLEMENTED = 32 - 3 + 2 + 4 + 2 = 37

Potential future IMPLEMENTED after independent member-points evidence = 37 + 1 = 38

unknown_boundary_count = 7
unknown_capability_check_count = 5
```

The five UNKNOWN capability checks are newly registered evidence-gap checks, not additional V1 UNKNOWN boundaries.

Count changes are bookkeeping effects of the Review-001 split dispositions and explicit evidence-gap registration only; they do not create or modify Identity conclusions.

## 4. Owner review basis and merge method

Owner completeness review uses the three records in a deterministic order:

1. Start from the untouched V1 physical baseline YAML.
2. Apply every mandatory disposition in `D-IG Business Capability Set Review-001`.
3. The resulting composition is Capability Set v2 (LOGICAL) as defined by this record.
4. For any ambiguity in the composition, this logical v2 record §2 is authoritative for the v2 governance composition.

The Owner review must not be interpreted as requirement confirmation where requirement-driven source records remain unavailable. `REQUIREMENT_DRIVEN_INCOMPLETE` remains in force.

## 5. Sequencing state

```yaml
D-IG: OPEN
Capability_Set: V2_LOGICAL_ESTABLISHED_PENDING_FINAL_QUICK_CHECK
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

A physical V2 YAML is intentionally deferred to the local byte-preserving patch operation recorded by `03-review/d-ig-business-capability-set-v2-patch.py`. The planned patch must modify only the agreed metadata/count fields and the three split capability blocks plus removal of `UNKNOWN-008`, while preserving all unrelated V1 evidence text byte-for-byte.

## 7. Non-effects

- The distinction between UNKNOWN capability checks and UNKNOWN business-capability boundaries is a governance bookkeeping distinction only.
- Registering an UNKNOWN capability check does not imply that the capability exists in production or that it is required.
- Splitting IMPLEMENTED capabilities and registering evidence-gap checks do not promote or reject any E0 Identity candidate.
- `C-007`, `C-008`, `C-012`, `C-013`, and all other E0 candidates remain governed by the E0 Candidate Enumeration and subsequent Identity tests, not by Capability Set counts.
