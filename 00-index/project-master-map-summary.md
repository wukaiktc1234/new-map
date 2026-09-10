# Project Master Map Summary

## Overview

This report provides a comprehensive summary of the project's current architecture status, business foundation, and prioritized remediation items. It combines **PROJECT-MASTER-MAP-001** (Engineering Reality) and **PROJECT-MASTER-MAP-002** (Business Foundation & Dependency Reality) results.

---

## Core Statistics

### Engineering Reality (MAP-001)

| Metric | Count | Details |
|--------|-------|---------|
| **Core Objects** | 16 | 11 VERIFIED, 5 LEGACY |
| **Tables** | 100+ | 8 groups of duplicate tables |
| **APIs (Controllers)** | 154 | Across all domains |
| **Pages** | 200+ | Management 100+, POS 11, Kitchen 4, Employee 30+, MiniProgram 24, Mobile 1 |
| **Events/Jobs** | 47 | 22 Events, 10 Listeners, 5 Dispatchers, 6 Scheduled Tasks, 4 Marketing Dispatchers |
| **Legacy Components** | 36 | 8 tables, 8 services, 13 controllers, 7 interfaces |
| **Conflict Points** | 10 | Active conflicts requiring resolution |
| **Active Blockers** | 10 | BLOCKED_PRODUCT_DECISION |
| **Product Decision Blockers** | 10 | PD-006~010, PD-012~013, PD-033~035 |
| **Dead Code Items** | 5 | Major dead code identified |
| **Engineering-Ready WPs** | 4 | W0-WP-01~04 |

### Business Foundation (MAP-002)

| Metric | Count | Details |
|--------|-------|---------|
| **Foundation Objects** | 15 | 12 VERIFIED, 2 PARTIAL, 3 MISSING, 2 UNKNOWN |
| **ROOT Foundation** | 6 | Organization, Role, Permission, Category, AccountingSubject, BankAccount |
| **Missing Foundation** | 5 | Warehouse, Unit, PaymentMethod, Customer, Price |
| **Upstream Dependencies** | 16 | Core business capabilities traced |
| **Broken Chains** | 31 | High-risk issues identified |
| **Capability Readiness** | 9 | 5 READY, 4 PARTIAL |
| **Cross-Domain Risks** | 13 | 5 HIGH, 5 MEDIUM, 3 LOW |
| **Data Lineage Risks** | 13 | 5 HIGH, 5 MEDIUM, 3 LOW |

---

## Top 20 Highest-Value Remediation Items

The following items are prioritized based on impact, risk reduction, and alignment with product goals.

| # | Remediation Item | Category | Impact |
|---|------------------|----------|--------|
| 1 | Unify Order State Machine (3 → 1) | Data Consistency | High |
| 2 | Unify Amount Unit (分/元 → 分) | Data Consistency | High |
| 3 | Freeze `account_balance` Table | Data Integrity | Critical |
| 4 | Add `status` Field to `fund_flows` | Data Completeness | Medium |
| 5 | Add Publish Action Audit Log | Security & Compliance | High |
| 6 | Clean Up Legacy Database Tables | Tech Debt | Medium |
| 7 | Clean Up Legacy Service Implementations | Tech Debt | Medium |
| 8 | Clean Up Legacy Controllers | Tech Debt | Medium |
| 9 | Clean Up Dead Code | Code Quality | Low |
| 10 | Unify Voucher Status Mapping | Data Consistency | Medium |
| 11 | Resolve `food` + `foods` Dual-Write | Data Consistency | High |
| 12 | Resolve `inventory` + `store_inventory` Dual-Write | Data Consistency | High |
| 13 | Resolve Cost Dual-Write | Data Consistency | High |
| 14 | Resolve `finance_record` Metric Scope | Data Consistency | Medium |
| 15 | Add Migration for `role_stores` / `role_departments` | Data Completeness | Medium |
| 16 | Resolve Raw API Permission Ownership | Security | High |
| 17 | Resolve Appeal API Domain Ownership | Architecture | Medium |
| 18 | Resolve Supplier H5 External Authentication | Security | High |
| 19 | Resolve Generic Approval Permission Model | Security | High |
| 20 | Resolve Refund Approval Identity Attribution | Security | High |

---

## Business Foundation Insights

### Foundation Dependency Order

```
Organization (ROOT)
    ↓
Store / Department
    ↓
Master Data (Food, Material, Supplier, Employee)
    ↓
Configuration / Policy
    ↓
Capability (Order, Purchase, Inventory)
    ↓
Transaction
    ↓
Settlement
    ↓
Reporting
```

### Critical Missing Foundations

| Foundation | Impact | Downstream Affected |
|------------|--------|---------------------|
| **Warehouse** | Inventory Management cannot properly track locations | inventory, store_inventory, purchase_stockins |
| **Unit** | No standardized unit conversion | All quantity-based operations |
| **PaymentMethod** | Payment processing incomplete | payment, receipt, order_payment_records |
| **Customer** | Customer data fragmented | orders, receivables, members |
| **Price** | Pricing logic scattered | foods.sale_price, material_archives.reference_price |

### High-Risk Broken Chains

1. **AutoVoucherService Zero-Call** - Financial vouchers cannot auto-generate
2. **25 Raw APIs Without Permission** - Security vulnerability
3. **Order State Three-Table Coexistence** - State inconsistency risk
4. **food/foods Dual-Write** - Inventory/status corruption risk
5. **Amount Unit Dual-Track** - 100x calculation error risk

---

## Key Observations

### Engineering Reality (MAP-001)

1. **High Complexity**: 200+ pages, 154 APIs, 47 event/job components
2. **Significant Technical Debt**: 36 legacy components, 5 major dead code items
3. **Critical Data Integrity Issues**: Multiple dual-write scenarios, inconsistent units
4. **Security & Compliance Gaps**: Several permission and audit issues
5. **Blocked Progress**: 10 product decision blockers halting development

### Business Foundation (MAP-002)

1. **Foundation Gaps**: 5 critical foundations missing (Warehouse, Unit, PaymentMethod, Customer, Price)
2. **Upstream Dependency Issues**: 16 business capabilities traced, some with incomplete foundations
3. **Broken Chains**: 31 broken chain issues identified
4. **Capability Readiness**: 4 capabilities partially ready (Purchase, Finance, Device, Reporting)
5. **Cross-Domain Coordination**: 13 cross-domain risks identified
6. **Data Lineage**: 13 data lineage risks identified

---

## Recommended Next Steps

### Phase 1: Foundation Stabilization (MAP-002)

1. **Resolve Missing Foundations**: Address Warehouse, Unit, PaymentMethod, Customer, Price
2. **Fix Broken Chains**: Address AutoVoucherService, raw API permissions, order state machine
3. **Stabilize Cross-Domain Data Flow**: Resolve dual-write issues, amount unit conflicts

### Phase 2: Data Integrity (MAP-001)

4. **Resolve Product Decision Blockers**: Address PD-006~010, PD-012~013, PD-033~035
5. **Prioritize Data Integrity Fixes**: Focus on items #1, #2, #3, #11, #12, #13
6. **Security Hardening**: Address permission and audit issues (#5, #16, #17, #18, #19, #20)

### Phase 3: Technical Debt (MAP-001)

7. **Tech Debt Cleanup**: Schedule legacy cleanup (#6, #7, #8, #9) in upcoming sprints

---

## Document Structure

### MAP-001 (Engineering Reality)

| File | Content |
|------|---------|
| `project-master-map.md` | Main map summary |
| `business-object-map.md` | Business objects |
| `db-reality-map.md` | Database reality |
| `api-map.md` | API endpoints |
| `frontend-map.md` | Frontend clients |
| `event-job-map.md` | Events and jobs |
| `state-machine-map.md` | State machines |
| `permission-data-scope-map.md` | Permissions |
| `legacy-map.md` | Legacy components |
| `truth-conflict-map.md` | Truth sources and conflicts |
| `issue-lifecycle-map.md` | Issue lifecycle |

### MAP-002 (Business Foundation)

| File | Content |
|------|---------|
| `foundation-map.md` | Foundation objects |
| `upstream-dependency-map.md` | Upstream dependencies |
| `downstream-impact-map.md` | Downstream impact |
| `capability-readiness-map.md` | Capability readiness |
| `broken-chain-map.md` | Broken chains |
| `data-inheritance-map.md` | Data inheritance |
| `dialog-form-data-contract-map.md` | Dialog/Form contracts |
| `master-data-source-map.md` | Master data sources |
| `business-identity-propagation-map.md` | Identity propagation |
| `business-data-flow-map.md` | Business data flow |
| `data-design-map.md` | Data design |
| `data-lineage-map.md` | Data lineage |
| `cross-domain-coordination-matrix.md` | Cross-domain coordination |

---

*Report generated based on PROJECT-MASTER-MAP-001 and PROJECT-MASTER-MAP-002 analysis.*