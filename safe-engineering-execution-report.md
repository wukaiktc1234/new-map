# Safe Engineering Execution Report

> **任务**: SAFE-ENGINEERING-EXECUTION-PHASE-001
> **版本**: 1.0
> **日期**: 2026-09-10
> **状态**: IN_PROGRESS — Investigation Complete, Awaiting Production Discovery (C1 Cancelled)
> **治理角色**: 架构总控 / 项目治理负责人

---

## 一、Phase Overview

### 1.1 Phase Definition

Safe Engineering Execution Phase 是项目进入正式整改前的"侦察阶段"。在此阶段：

- **允许**: 调查、生成差异报告、生成 Engineering Cards、Bug Fixes (type errors, null safety, exception paths, API contract issues)
- **禁止**: 任何涉及业务语义的架构决策、Schema Migration、新 Canonical 表、Identity/Inventory Truth 重构、Product/Food/Material merge/split、BOM/recipe restructuring
- **产出**: Safe Engineering Execution Report (本文档) + Investigation Evidence Files + Engineering Cards

### 1.2 Investigation Tasks Completed

| Task ID | Task Name | Status | Evidence Files |
|---------|-----------|--------|----------------|
| A1 | purchase_request_item.food_id caller path | ✅ COMPLETED | — (inline evidence) |
| A2 | dish_recipe vs dish_recipes schema | ✅ COMPLETED | — (inline evidence) |
| A3 | inventory_transactions caller/writer/reader | ✅ COMPLETED | — (inline evidence) |
| A4 | inventory vs store_inventory差异 | ✅ COMPLETED | — (inline evidence) |
| B1 | Legacy tables (product, sales_order, orders_legacy, voucher_header) | ✅ COMPLETED | — (inline evidence) |
| C1 | Production environment discovery | ❌ CANCELLED | — |

### 1.3 Key Findings Summary

- **6 tables** investigated with full caller path analysis
- **4 legacy tables** verified active (product, orders_legacy, voucher_header, sales_order virtual wrapper)
- **0 safe deletions** — all legacy tables have active runtime callers
- **3 blocking semantic decisions** identified (food_id naming, dual-table confusion, FK absence)
- **Production environment** not yet discovered — Evidence Gate-0 remains BLOCKED

---

## 二、COMPLETED — Safe Without Semantic Decision

### 2.1 A1: purchase_request_item.food_id

**Status**: `SAFE_WITHOUT_SEMANTIC_DECISION` (for analysis); `BLOCKED_BY_BUSINESS_SEMANTIC_DECISION` (for field rename)

**Schema Evidence**:
- Table: `purchase_request_item`
- Column: `food_id VARCHAR(32)` — nullable, no FK constraint
- Created in: V1.0.0.100 (initial schema)
- Index: 1 index on `food_id`

**Entity/DTO Mapping**:
- `PurchaseRequestItem.java`: `food_id` → `foodId` (VARCHAR)
- `PurchaseRequestItemDTO.java`: `foodId` field mapped
- `PurchaseRequestVO.java`: `foodId` field mapped

**Full Caller Path (18 usages)**:
1. **Writer (1)**: `PurchaseRequestServiceImpl.createPurchaseRequest()` — writes `food_id`
2. **Readers (17)**: Various query, detail, and statistics methods that read `food_id`

**Analysis**: `food_id` is semantically ambiguous — it references a "food" but is used in a "purchase request" context where `material_id` is the standard. The field is actively used across 18 code locations. Renaming requires business semantic decision on whether purchase requests reference Food or Material.

**Action**: Generate Engineering Card for field rename decision. No code change allowed without PD-CANONICAL-001 confirmation.

---

### 2.2 A2: dish_recipe vs dish_recipes

**Status**: `EVIDENCE_FOUND` — Two distinct tables with active callers

**Schema Evidence**:

| Aspect | dish_recipe (V1.0.0.100) | dish_recipes (V6.0.0) |
|--------|--------------------------|------------------------|
| PK Type | VARCHAR(32) | BIGINT |
| FK Fields | `dish_id`, `material_id` | `food_id`, `material_id` |
| Soft-delete | Yes (`deleted` field) | No |
| Extra Fields | — | `loss_rate`, `unit_cost`, `subtotal_cost`, `recipe_status` |
| Status Field | — | `recipe_status` |

**Active Callers**:
- `DishRecipeService` → `dish_recipe` (legacy)
- `RecipeBomService` → `dish_recipes` (new)
- Both services have active callers throughout the codebase

**Analysis**: Two coexisting recipe tables with different schemas, different ID types, different FK naming, and different active callers. This creates:
1. Data duplication risk (same recipe in both tables)
2. Schema confusion (which is the source of truth?)
3. Migration complexity (consolidation requires careful orchestration)

**Action**: Generate Engineering Card for table consolidation decision. No merge allowed without PD-CANONICAL-003 (Food semantics) confirmation.

---

### 2.3 A3: inventory_transactions

**Status**: `EVIDENCE_FOUND` — Schema rebuilt, active callers, no FK constraints

**Schema Evidence** (V20260723_005 — current definitive):
- PK: BIGINT (rebuilt)
- `material_id` references `material_archives` (BIGINT)
- `transaction_type` NOT NULL
- `warehouse_id` added
- Soft-delete via `deleted` field
- **No FK constraints on table** (dangling reference risk)

**Active Callers**:
- **6+ Writers**: purchase receipt, inventory adjustment, store-transfer, BOM consumption, auto-generation, legacy stock import
- **10+ Readers**: various query and reporting methods

**Analysis**:
1. No FK constraints → dangling reference risk if material is deleted
2. `material_id` references `material_archives` → consistent with LOCK-A002
3. Soft-delete pattern consistent with other tables
4. Schema rebuilt at V20260723_005 suggests prior issues

**Action**: Generate Engineering Card for FK constraint addition. Safe to add FK constraints (no semantic change).

---

### 2.4 A4: inventory vs store_inventory

**Status**: `EVIDENCE_FOUND` — Two distinct tables with bridging service

**Schema Comparison**:

| Aspect | inventory (central) | store_inventory (store-level) |
|--------|---------------------|-------------------------------|
| Identity Key | `material_id` | `material_id` + `store_id` |
| Version Field | Yes (optimistic locking) | No |
| Soft-delete | `deleted` field | `status` field |
| Location | Central warehouse | Store-specific |

**Bridging Service**: `InventoryTransferService` — transfers between inventory and store_inventory

**Analysis**:
1. Two-tier inventory model: central + store
2. Different soft-delete patterns (`deleted` vs `status`)
3. Bridging service exists but schema differs
4. No unified inventory view across locations

**Action**:差异报告 generated. No merge/choice of canonical allowed without AD-INVENTORY-001/002 confirmation.

---

## 三、BLOCKED — Requires Business Semantic Decision

### 3.1 PD-CANONICAL-001 (Canonical Business Identity Semantics)

**Status**: RECOMMENDED (NOT CONFIRMED)
**Blocks**: PD-CANONICAL-002~006
**Impact on Safe Engineering**: Any field rename, table consolidation, or identity migration is blocked

### 3.2 PD-CANONICAL-003 (Food Semantics)

**Status**: OPEN
**Impact on Safe Engineering**: dish_recipe/dish_recipes consolidation blocked

### 3.3 AD-INVENTORY-001/002 (Inventory Truth/Object)

**Status**: OPEN
**Impact on Safe Engineering**: inventory/store_inventory merge or choice blocked

---

## 四、EVIDENCE_FOUND — Requires Engineering Card

### 4.1 FK Absence on inventory_transactions

**Issue**: `inventory_transactions` has `material_id` referencing `material_archives` but no FK constraint defined in schema.
**Risk**: Dangling references if material is deleted.
**Action**: Add FK constraint — `SAFE_WITHOUT_SEMANTIC_DECISION` (pure schema hardening).
**Engineering Card**: ENG-CARD-001

### 4.2 Dual-Table Recipe Confusion

**Issue**: `dish_recipe` and `dish_recipes` coexist with different schemas and active callers.
**Risk**: Data inconsistency, developer confusion, migration complexity.
**Action**: Table consolidation decision required — `BLOCKED_BY_BUSINESS_SEMANTIC_DECISION`.
**Engineering Card**: ENG-CARD-002

### 4.3 purchase_request_item.food_id Semantic Mismatch

**Issue**: `food_id` field used in purchase request context where `material_id` is standard.
**Risk**: Semantic confusion, cross-module reference errors.
**Action**: Field rename decision required — `BLOCKED_BY_BUSINESS_SEMANTIC_DECISION`.
**Engineering Card**: ENG-CARD-003

### 4.4 inventory vs store_inventory Schema Divergence

**Issue**: Different soft-delete patterns (`deleted` vs `status`), different identity keys, no unified view.
**Risk**: Inconsistent inventory state across locations.
**Action**: Unified inventory model decision required — `BLOCKED_BY_BUSINESS_SEMANTIC_DECISION`.
**Engineering Card**: ENG-CARD-004

---

## 五、PRODUCTION_ACCESS — Environment Discovery

**Status**: CANCELLED — Production environment not yet discovered.

**Required for Evidence Gate-0**:
1. Production DB connection string
2. Network/tunnel method (VPN, SSH tunnel, etc.)
3. DBA/owner contact
4. Environment variables needed
5. No mutations — read-only access only

**Impact**: Cannot validate production data against local schema. All evidence is `LIVE_LOCAL_INSTANCE_EVIDENCE` only.

---

## 六、RISK Assessment

### 6.1 High Risk

| Risk | Impact | Mitigation |
|------|--------|------------|
| Production data unknown | Cannot validate schema assumptions | Cancelled C1 — requires manual setup |
| PD-CANONICAL-001 unconfirmed | All semantic decisions blocked | Product Owner confirmation required |
| Legacy tables active | Cannot safely delete | 5-way proof required (no runtime caller, no test dep, no data dep, no external dep, no migration dep) |

### 6.2 Medium Risk

| Risk | Impact | Mitigation |
|------|--------|------------|
| Dual recipe tables | Data inconsistency | ENG-CARD-002 generated |
| FK absence | Dangling references | ENG-CARD-001 generated |
| Semantic mismatch | Cross-module errors | ENG-CARD-003 generated |

### 6.3 Low Risk

| Risk | Impact | Mitigation |
|------|--------|------------|
| inventory/store_inventory divergence | Inconsistent views | ENG-CARD-004 generated |
| Legacy table investigation | No deletion without proof | Investigation complete, no deletion attempted |

---

## 七、ENGINEERING CARDS Generated

| Card ID | Title | Status | Blocked By |
|---------|-------|--------|------------|
| ENG-CARD-001 | Add FK constraint to inventory_transactions | SAFE_WITHOUT_SEMANTIC_DECISION | None |
| ENG-CARD-002 | Consolidate dish_recipe / dish_recipes tables | BLOCKED_BY_BUSINESS_SEMANTIC_DECISION | PD-CANONICAL-003 |
| ENG-CARD-003 | Rename purchase_request_item.food_id | BLOCKED_BY_BUSINESS_SEMANTIC_DECISION | PD-CANONICAL-001 |
| ENG-CARD-004 | Unify inventory / store_inventory model | BLOCKED_BY_BUSINESS_SEMANTIC_DECISION | AD-INVENTORY-001/002 |

---

## 八、Safe Engineering Phase Exit Criteria

### 8.1 Completed

- [x] Investigation tasks A1-A4, B1 completed
- [x] Evidence collected for 6 tables
- [x] 4 Engineering Cards generated
- [x] Legacy table deletion safety verified (all active callers mapped)

### 8.2 Pending

- [ ] Production environment discovery (C1 cancelled — requires manual setup)
- [ ] PD-CANONICAL-001 confirmation (Product Owner)
- [ ] PD-CANONICAL-003 confirmation (Product Owner)
- [ ] AD-INVENTORY-001/002 confirmation (Architecture Owner)

### 8.3 Next Phase Prerequisites

Before entering "Full Remediation Phase":
1. PD-CANONICAL-001 confirmed → PD-CANONICAL-002~006 can proceed
2. PD-CANONICAL-003 confirmed → ENG-CARD-002 can proceed
3. AD-INVENTORY-001/002 confirmed → ENG-CARD-004 can proceed
4. Production environment discovered → Evidence Gate-0 can be cleared

---

## 九、Document Status

| Document | Status | Notes |
|----------|--------|-------|
| current-business-semantic-baseline.md | CURRENT_BASELINE | v2.0 |
| business-item-canonical-decision-boundary.md | CURRENT_BASELINE | v2.0 |
| PD-CANONICAL-001-Decision.yaml | ACTIVE_REFERENCE | v2.0, scope-corrected |
| PD-CANONICAL-001-decision-record.md | ACTIVE_REFERENCE | v2.0, scope-corrected |
| safe-engineering-execution-report.md | ACTIVE_REFERENCE | 本文档 |

---

**文档状态**: ✅ SAFE_ENGINEERING_EXECUTION_REPORT  
**下一步**: 等待 Product Owner 确认 PD-CANONICAL-001，或手动设置生产环境发现 (C1)
