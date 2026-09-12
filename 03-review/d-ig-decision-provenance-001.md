# D-IG Decision Provenance 001

> **Status**: ACTIVE REFERENCE  
> **Decision**: D-IG  
> **Decision Status**: OPEN  
> **Owner**: BUSINESS_OWNER  
> **Started**: 2026-09-12

## 1. Provenance

| Field | Value |
|---|---|
| Decision ID | D-IG |
| Origin | A1/A2 Confirmed Semantic Layering |
| Source Documents | `03-review/business-item-semantic-layering-decision-001.md`; `03-review/current-business-semantic-baseline.md` |
| Upstream Decisions | DEC-BI-001 (A1); DEC-BI-002 (A2) |
| Evidence | Local code/DB/API evidence; Production Evidence Gate-0 currently BLOCKED |
| Owner | BUSINESS_OWNER |
| Status | OPEN |

## 2. Scope

D-IG determines Identity Grain and, if required, Identity Layer hierarchy.

It does not determine:

- database table names;
- primary-key strategy;
- migration execution;
- legacy table deletion;
- canonical storage architecture;
- complete Quantity/UOM semantics.

## 3. First Decision

The first D-IG agenda item is:

> H1 — Single Identity Grain vs H2 — Multiple Identity Layers.

No Identity Resolution Matrix row may be treated as decided before this question is answered.

## 4. Evidence Rule

Local evidence may support provisional conclusions. Production-dependent conclusions must remain `PROVISIONAL_PENDING_PRODUCTION_EVIDENCE` until Production Evidence becomes available and conflicts are resolved.

## 5. Governance

D-IG may not enter `CONFIRMED` until the Decision Gate in the Charter is satisfied, including provenance completeness and independent adversarial review.
