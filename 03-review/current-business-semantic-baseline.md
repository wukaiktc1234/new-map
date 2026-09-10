# Current Business Semantic Baseline

## 一、Baseline 概述
- 生成日期：2026-09-10
- 任务：PROJECT-KNOWLEDGE-BASE-BASELINE-RECONCILIATION-001
- 状态：BASELINE-RECONCILIATION = PASS_WITH_OPEN_DECISIONS

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

### dish_recipe 连接 food 和 material
- Evidence Status: VERIFIED

### order_items 使用 food_id
- Evidence Status: VERIFIED

### purchase_order_items 使用 material_id
- Evidence Status: VERIFIED

### product 表标记为 LEGACY
- Evidence Status: VERIFIED

## 三、B. Current Reconciled Semantics（已收敛语义）

### L2-001: Inventory Truth = Stock Ledger
- 状态：RECONCILED
- 来源：07-inventory-semantic-model/inventory-truth-analysis.md

### L2-002: Inventory Fact = Material × Location × Quantity
- 状态：RECONCILED
- 注意：这是 Current Implementation Fact，不是 Target Model

### L2-003: Balance = Derived View
- 状态：RECONCILED

### L2-004: Stockable = Location-scoped Business Role
- 状态：RECONCILED

### L2-005: Location = 统一抽象
- 状态：RECONCILED

### L2-006: UOM = 独立 Foundation
- 状态：RECONCILED

### L2-007: INGREDIENT = Recipe ↔ Item Relationship
- 状态：RECONCILED

### L2-008: SELLABLE = Capability + Role
- 状态：RECONCILED

### L2-009: PURCHASABLE = Capability + Profile
- 状态：RECONCILED

### L2-010: CONSUMABLE = Role (三种消费场景)
- 状态：RECONCILED

## 四、C. Open Decisions（开放决策）

### Product Decisions
- PD-CANONICAL-001: Canonical Business Item 定义 → OPEN
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

## 八、Document Lifecycle Summary

### 当前可直接作为任务输入的文档
- 03-review/current-business-semantic-baseline.md (本文件)
- 03-review/business-item-canonical-semantic-reassessment.md
- 03-review/business-item-canonical-decision-boundary.md
- 03-review/business-item-canonical-conflict-registry.yaml
- 03-review/business-item-inventory-integration.md
- 01-engineering-reality/* (所有工程现实文件)
- 02-business-dependency/* (所有业务依赖文件)
- 05-governance/* (所有治理文件)

### 仍需回查的 Reference
- 06-business-item-model/* (历史分析，仍有证据价值)
- 07-inventory-semantic-model/* (历史分析，仍有证据价值)

### 已经不应作为当前依据的文档
- 06-business-item-model/recommendation.md (已被替代)
- 06-business-item-model/dec-006-refined-decision-pack.md (历史推荐)
- 07-inventory-semantic-model/inventory-semantic-recommendation.md (已被替代)
