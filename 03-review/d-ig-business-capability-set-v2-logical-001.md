# D-IG Capability Set v2 — Logical Governance Record 001

- Decision: D-IG
- Status: ACTIVE_REFERENCE
- Physical capability set: `03-review/d-ig-business-capability-set-001.yaml` (V1 baseline)
- Revision record: `03-review/d-ig-business-capability-set-review-001.md`
- Owner Review record: `03-review/d-ig-business-capability-owner-review-001.md`
- Logical v2 status: `ESTABLISHED_FROM_V1_PLUS_REVIEW_001`
- Adversarial review status: `COMPLETED_PASSED`
- Owner completeness review: `COMPLETED`
- Requirement-driven status: `REQUIREMENT_DRIVEN_INCOMPLETE`
- Production evidence status: `BLOCKED`
- Pre-Screen authorization: `READY`
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

This record is authoritative for the fact that the reviewed v2 composition exists for governance sequencing. It does **not** replace the V1 YAML as the physical baseline and does **not** authorize Schema, Migration, or Identity decisions.

The completed Owner Review is an additional governance input to sequencing. It confirms the completeness of the business-capability boundary within the reviewed evidence scope and records Owner system-boundary/business-endpoint decisions, but it does not convert oral/unlocated supplemental inputs into `DECLARED` capabilities or change implementation evidence.

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

The Owner Review confirms additional system-level business capabilities and boundaries, but because the corresponding Owner statements are not yet backed by locatable requirement records, they remain `ORAL_UNLOCATED_PENDING_OWNER_RECORD` supplemental inputs and do not change the 37 IMPLEMENTED count in this logical v2 record.

## 4. Owner review basis and merge method

Owner completeness review used the three records in a deterministic order:

1. Start from the untouched V1 physical baseline YAML.
2. Apply every mandatory disposition in `D-IG Business Capability Set Review-001`.
3. The resulting composition is Capability Set v2 (LOGICAL) as defined by this record.
4. For any ambiguity in the composition, this logical v2 record §2 is authoritative for the v2 governance composition.
5. Apply the completed Owner Review as a governance/provenance input for boundary completeness and business-semantic conclusions; do not treat oral/unlocated supplemental statements as `DECLARED` or `IMPLEMENTED` evidence.

The Owner review must not be interpreted as requirement confirmation where requirement-driven source records remain unavailable. `REQUIREMENT_DRIVEN_INCOMPLETE` remains in force.

### 4.1 Owner handoff basis

The BUSINESS_OWNER completeness review is based on these records together:

1. `03-review/d-ig-business-capability-set-001.yaml` — V1 physical baseline and original evidence-bearing capability/boundary records.
2. `03-review/d-ig-business-capability-set-review-001.md` — mandatory review dispositions applied to V1.
3. `03-review/d-ig-business-capability-set-v2-logical-001.md` — deterministic v2 composition, count definitions, and merge rule.
4. `03-review/d-ig-business-capability-owner-review-001.md` — completed Owner completeness review and business-boundary decisions.

The review scope is **completeness of the business capability boundary represented by the current evidence package**, not confirmation of unavailable requirement-driven source records and not an Identity decision.

## 5. Sequencing state

```yaml
D-IG: OPEN
Capability_Set: V2_LOGICAL_ESTABLISHED
Adversarial_Review: COMPLETED_PASSED
Owner_Review: COMPLETED
Requirement_Driven: INCOMPLETE
Production_Evidence: BLOCKED
Pre_Screen: READY
E1_E4: BLOCKED_PENDING_PRE_SCREEN
Schema: BLOCKED
Migration: BLOCKED
Identity_Decision: NOT_MADE
```

## 6. Physical v2 plan

A physical V2 YAML remains intentionally deferred to the local byte-preserving patch operation recorded by `03-review/d-ig-business-capability-set-v2-patch.py`. The planned patch must modify only the agreed metadata/count fields and the three split capability blocks plus removal of `UNKNOWN-008`, while preserving all unrelated V1 evidence text byte-for-byte.

The completed Owner Review does not by itself justify changing physical V1 evidence records or inserting unlocated Owner statements into the YAML as `DECLARED`/`IMPLEMENTED`.

## 7. Non-effects

- The distinction between UNKNOWN capability checks and UNKNOWN business-capability boundaries is a governance bookkeeping distinction only.
- Registering an UNKNOWN capability check does not imply that the capability exists in production or that it is required.
- Splitting IMPLEMENTED capabilities and registering evidence-gap checks do not promote or reject any E0 Identity candidate.
- Owner boundary confirmation does not constitute implementation evidence.
- `C-007`, `C-008`, `C-012`, `C-013`, and all other E0 candidates remain governed by the E0 Candidate Enumeration and subsequent Identity tests, not by Capability Set counts.

## 8. Next governance gate

The next authorized activity is **Identity Necessity Pre-Screen execution** using:

1. the E0 Candidate Object List;
2. the established Capability Set v2 logical composition and its confirmed Owner boundary;
3. locatable test cases;
4. A1/A2 confirmed semantic boundaries.

Pre-Screen must preserve `PENDING_CAPABILITY_CONFIRMATION` whenever a conclusion depends on an unresolved UNKNOWN capability check/boundary, and must preserve `PROVISIONAL_NEEDS_PRODUCTION` when a conclusion depends on blocked production evidence.

Pre-Screen must not make an Identity decision, decide H1/H2, authorize Schema, or authorize Migration.
