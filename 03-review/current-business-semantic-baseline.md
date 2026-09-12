# Current Business Semantic Baseline

> **任务**: PROJECT-KNOWLEDGE-BASE-CURRENT-BASELINE-FINALIZATION-001
> **版本**: 3.0
> **日期**: 2026-09-12
> **状态**: CURRENT BASELINE

## 一、Governance Decision Baseline

### DEC-BI-001: A1 — Identity 与其他业务语义分离
- Decision Status: **CONFIRMED**
- Evidence Status: **PARTIAL**（基于当前本地证据；生产环境证据仍不可得）
- Owner: BUSINESS_OWNER
- Confirmed at: 2026-09-12
- Source: `03-review/business-item-semantic-layering-decision-001.md`

### DEC-BI-002: A2 — Product / Food / Material 不作为竞争性 Canonical Identity Namespace
- Decision Status: **CONFIRMED**
- Evidence Status: **PARTIAL**（基于当前本地证据；生产环境证据仍不可得）
- Owner: BUSINESS_OWNER
- Confirmed at: 2026-09-12
- Source: `03-review/business-item-semantic-layering-decision-001.md`

### D-IG: Identity Grain Decision
- Decision Status: **OPEN**
- Execution Status: **BLOCKED**
- First Agenda: **H1 Single Identity Grain vs H2 Multiple Identity Layers**
- Source: `03-review/d-ig-charter-001.md`

## 二、A. Confirmed Current Reality（当前代码/DB/API事实）

### LOCK-A001: foods 为菜品唯一真相源
- Evidence Status: VERIFIED
- Decision Status: LOCKED

### LOCK-A002: material_archives 为物料真相源
- Evidence Status: VERIFIED
- Decision Status: LOCKED

### LOCK-A003: 金额以分为准
- Evidence Status: PARTIAL
- Decision Status: LOCKED

### inventory 以 material_id 管理库存
- Evidence Status: VERIFIED
- 类型：Current Implementation Fact

### store_inventory 以 store_id + material_id 管理门店库存
- Evidence Status: VERIFIED
- 类型：Current Implementation Fact

### dish_recipe / dish_recipes
- 当前实现存在两个相近命名对象；其字段与数据语义仍需按独立 Evidence 复核
- 不在本 Baseline 中继续写成已确认的统一 food↔material 关系

### order_items 使用 food_id
- Evidence Status: VERIFIED

### purchase_order_items 使用 material_id
- Evidence Status: VERIFIED

### product 表
- 当前应视为 legacy / active-code-residue 候选，不得仅凭名称赋予 Canonical Identity 地位

## 三、B. Current Reconciled Semantics（已收敛语义）

### L2-001: Ledger = Inventory Event Record / Balance = Derived State
- 状态：RECONCILED
- 注意：Ledger-first vs Balance-first 仍属于 AD-INVENTORY-001 = OPEN

### L2-002: Inventory Fact = Material × Location × Quantity
- 状态：RECONCILED
- 类型：CURRENT IMPLEMENTATION FACT（不得写成 Target Canonical Inventory Fact）

### L2-003: Balance = Derived View
- 状态：RECONCILED

### L2-004: Stockable = Business Item Capability
- 状态：RECONCILED
- 注意：Location-specific stocking configuration 属于 Scope / Profile / Context 层

### L2-005: Location = 统一语义抽象方向
- 状态：RECONCILED
- 注意：Store / Warehouse / Kitchen 可作为 Location Types
- 注意：Transit 当前 Reassessment 方向为 Inventory State，而非 Physical Location
- 正式数据模型：AD-LOCATION-001 = OPEN

### L2-006: 多 UOM / Context-specific UOM / Conversion = 已识别的业务语义需求
- 状态：RECONCILED
- 注意："UOM 是否独立 Foundation" 仍属于 AD-UOM-001 = OPEN
- 禁止把 Base → Purchase → Stock → Recipe → Sales 写成固定 Ontology

### L2-007: INGREDIENT = Recipe ↔ Business Item Relationship
- 状态：RECONCILED SEMANTIC INVARIANT
- 注意：不再作为未解决 Product Decision

### L2-008: SELLABLE = Capability
- 状态：RECONCILED
- 注意：Sales Role / Sales Profile 属于 Context-specific 表现

### L2-009: PURCHASABLE = Capability
- 状态：RECONCILED
- 注意：Procurement Profile 是配置，不是 Capability 的一部分

### L2-010: CONSUMABLE = Capability
- 状态：RECONCILED
- 注意：Consumption Context（Recipe / Supply / Operational）属于 Context / Consumption Event

## 四、C. Open Decisions（开放决策）

### Identity / Business Item Decisions
- D-IG: Identity Grain → OPEN
- D-QUANTITY: Quantity Semantics → OPEN
- D-ROLE-SCOPE: Role Scope → OPEN

### Product Decisions
- PD-CANONICAL-001: Canonical Business Item 定义 → SUPERSEDED by A1/A2 + D-IG sequence
- PD-CANONICAL-002: Product 语义 → OPEN
- PD-CANONICAL-003: Food 语义 → OPEN
- PD-CANONICAL-004: Material 语义 → OPEN
- PD-CANONICAL-005: Ingredient 定义 → RECONCILED
- PD-CANONICAL-006: Consumable 定义 → OPEN

### Architecture Decisions
- AD-IDENTITY-001: Identity Storage → OPEN
- AD-LOCATION-001: Location Model → OPEN
- AD-UOM-001: UOM Foundation → OPEN
- AD-INVENTORY-001: Inventory Truth → OPEN
- AD-INVENTORY-002: Inventory Object → OPEN

## 五、D. Active Recommendations（当前最新推荐）

### From BUSINESS-ITEM-CANONICAL-SEMANTIC-REASSESSMENT-001
- 推荐 Model C: Hybrid Domain Model
- 状态：RECOMMENDATION, NOT CONFIRMED

### From INVENTORY-SEMANTIC-MODEL-001
- 推荐 Model D: Item + Location + Stock Ledger
- 状态：RECOMMENDATION, NOT CONFIRMED

## 六、E. Superseded Conclusions（已被替代的旧结论）

### From 06-business-item-model/recommendation.md
- 旧推荐：Model C: Hybrid Domain Model
- 状态：SUPERSEDED by BUSINESS-ITEM-CANONICAL-SEMANTIC-REASSESSMENT-001

### From 06-business-item-model/dec-006-refined-decision-pack.md
- 旧推荐：废弃 Product，保留 Food + Material
- 状态：HISTORICAL (DEC-006-REFINED 是历史推荐)

### From 07-inventory-semantic-model/inventory-semantic-recommendation.md
- 旧推荐：Model C-Hybrid
- 状态：SUPERSEDED by BUSINESS-ITEM-CANONICAL-SEMANTIC-REASSESSMENT-001

## 七、F. Historical References（历史追溯）

### DEC-006 (Historical Decision Package)
- 路径：04-decision-history/open-product-decisions.md
- 状态：HISTORICAL

### DEC-006-REFINED (Historical Recommendation)
- 路径：06-business-item-model/dec-006-refined-decision-pack.md
- 状态：HISTORICAL

## 八、G. Active Decision Sequence

```text
A1/A2 = CONFIRMED
      ↓
D-IG = OPEN
      ↓
D-QUANTITY = OPEN
      ↓
D-ROLE-SCOPE = OPEN
      ↓
Canonical Business Item Model
      ↓
Schema / Migration Decisions
```

Production Evidence Gate-0 remains BLOCKED. Production-dependent conclusions must remain provisional until production evidence is available.

## 九、Document Lifecycle Summary

### 当前可直接作为任务输入的文档
- `03-review/current-business-semantic-baseline.md` (本文件)
- `03-review/business-item-semantic-layering-decision-001.md`
- `03-review/d-ig-charter-001.md`
- `03-review/d-ig-decision-provenance-001.md`
- `03-review/business-item-canonical-semantic-reassessment.md`
- `03-review/business-item-canonical-decision-boundary.md`
- `03-review/business-item-canonical-conflict-registry.yaml`
- `03-review/business-item-inventory-integration.md`
- `01-engineering-reality/*`
- `02-business-dependency/*`
- `05-governance/*`

### 仍需回查的 Reference
- `06-business-item-model/*`
- `07-inventory-semantic-model/*`

### 已经不应作为当前依据的文档
- `06-business-item-model/recommendation.md`
- `06-business-item-model/dec-006-refined-decision-pack.md`
- `07-inventory-semantic-model/inventory-semantic-recommendation.md`
