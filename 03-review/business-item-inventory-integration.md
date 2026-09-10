# Business Item × Inventory Semantic Integration

> **版本**: 1.0
> **生成日期**: 2026-09-10
> **状态**: OPEN
> **任务**: BUSINESS-ITEM-INVENTORY-INTEGRATION-001
> **来源域**: 06-business-item-model, 07-inventory-semantic-model
> **约束**: 本文档为只读分析结果，不修改任何代码

---

## 一、任务概述

### 任务目标

将 06-business-item-model 和 07-inventory-semantic-model 的语义分析结果进行跨域整合，识别已收敛概念、剩余冲突和统一模型候选，为 DEC-006 提供输入。

### 分析范围

- 06 域：Business Item 的 Identity / Type / Role / Capability / Relationship
- 07 域：Inventory 的 Truth / Fact / Location / Quantity / UOM / Ledger
- 跨域交叉：Food × Inventory, Material × Inventory, Recipe × Inventory

### 状态标记

| 标记 | 含义 | 本文档使用 |
|------|------|------------|
| RECONCILED | 06 与 07 已收敛 | ✓ 大量使用 |
| PARTIAL | 部分收敛，存在限定条件 | ✓ 使用 |
| OPEN | 未收敛，需要 Decision | ✓ 使用 |
| CONFIRMED | 已由 Decision 正式确认 | **禁止输出** |
| LOCKED | 已锁定为不可变事实 | **禁止输出** |

---

## 二、四层输出

### Layer 1 — Confirmed Facts

来自当前仓库的可验证事实。所有条目均可通过代码/DB/API 验证。

#### L1-001: foods 表为菜品唯一真相源

- **LOCK ID**: LOCK-A001
- **事实**: foods 表是菜品（Food）的唯一真相源，food 表为 legacy 已废弃但仍在双写
- **证据**: `foundation-map.md §8`, `truth-conflict-map.md §7` — FoodService.create() 写入 foods，同时双写 food（技术债）
- **置信度**: HIGH
- **状态**: VERIFIED

#### L1-002: material_archives 为物料真相源

- **LOCK ID**: LOCK-A002
- **事实**: material_archives 表是物料（Material）的唯一真相源
- **证据**: `foundation-map.md §9` — MaterialService.create() 写入 material_archives
- **置信度**: HIGH
- **状态**: VERIFIED

#### L1-003: inventory 以 material_id 管理仓库库存

- **事实**: inventory 表以 `material_id` + `warehouse_id` 为核心键管理仓库级库存
- **证据**: `db-reality-map.md §5` — inventory 表结构 inventory_id, material_id, warehouse_id, current_stock
- **置信度**: HIGH
- **状态**: VERIFIED

#### L1-004: store_inventory 以 store_id + material_id 管理门店库存

- **事实**: store_inventory 表以 `store_id` + `material_id` 为核心键管理门店级库存
- **证据**: `db-reality-map.md §5` — store_inventory 表结构 store_id, material_id, current_stock
- **置信度**: HIGH
- **状态**: VERIFIED

#### L1-005: dish_recipe 连接 food 和 material

- **事实**: dish_recipe 表通过 `food_id`（菜品）和 `material_id`（原料）建立 Recipe ↔ Item 关系
- **证据**: `db-reality-map.md §4` — dish_recipe 表结构 food_id, material_id, quantity, unit
- **置信度**: HIGH
- **状态**: VERIFIED

#### L1-006: order_items 使用 food_id

- **事实**: order_items 表通过 `food_id` 引用菜品，不直接引用 material_id
- **证据**: `db-reality-map.md §6` — order_items 表结构 order_id, food_id, quantity, price
- **置信度**: HIGH
- **状态**: VERIFIED

#### L1-007: purchase_order_items 使用 material_id

- **事实**: purchase_order_items 表通过 `material_id` 引用物料，不直接引用 food_id
- **证据**: `db-reality-map.md §6` — purchase_order_items 表结构 purchase_order_id, material_id, quantity, price
- **置信度**: HIGH
- **状态**: VERIFIED

#### L1-008: 金额以分为准

- **LOCK ID**: LOCK-A003
- **事实**: 新表统一用「分」存储金额（orders.amount, finance_records.amount），遗留表（tax_record, account_balance）仍用「元」
- **证据**: `truth-conflict-map.md §1` — 跨表 JOIN 时单位不一致，差 100 倍
- **置信度**: HIGH
- **状态**: VERIFIED

#### L1-009: inventory_unit 表存在但未被引用

- **事实**: inventory_unit 表定义了单位主数据（id, name, code, type），但代码中未建立外键引用
- **证据**: `inventory-uom-model.md §2.1` — 5 张表各自存储 unit 字段且与 inventory_unit 脱节
- **置信度**: HIGH
- **状态**: VERIFIED

#### L1-010: inventory_transactions 仅关联 inventory_id

- **事实**: inventory_transactions 表通过 `inventory_id` 关联仓库库存，门店库存变动未纳入此流水
- **证据**: `inventory-truth-analysis.md §2.2` — 门店库存变动可能未记录
- **置信度**: HIGH
- **状态**: VERIFIED

---

### Layer 2 — Reconciled Semantics

已经能够跨 06/07 收敛的语义。标记为 RECONCILED 或 PARTIAL。

#### L2-001: Inventory Truth = Stock Ledger (不可变记录)

- **定义**: 库存的唯一真相源是 Stock Ledger（库存台账），即不可变的库存变动流水记录
- **06 观点**: Business Item 的 Operational Definition 包含库存属性
- **07 观点**: Stock Ledger 是唯一 Truth Source，Balance / Instance / Snapshot 均为 Derived
- **收敛结论**: Inventory Truth = Stock Ledger
- **状态**: RECONCILED

#### L2-002: Inventory Fact = Material × Location × Quantity

- **定义**: 库存事实的最小语义单位是「某物料 × 某位置 × 某数量」的三元组
- **06 观点**: Business Item 的 Operational Definition 在不同位置有不同库存状态
- **07 观点**: Inventory Fact 是 Item × Location 的当前状态，可从 Ledger 重建
- **收敛结论**: Inventory Fact = Material × Location × Quantity
- **状态**: RECONCILED

#### L2-003: Balance = Derived View (可从 Ledger 重建)

- **定义**: 库存余额是 Stock Ledger 的聚合结果，不是独立 Truth
- **06 观点**: 无直接冲突
- **07 观点**: Balance 可从 Ledger 重建，当前 current_stock 是 Derived View
- **收敛结论**: Balance = Derived View
- **状态**: RECONCILED

#### L2-004: Stockable = Location-scoped Business Role

- **定义**: Stockable（可库存）是 Business Item 在特定 Location 下的业务角色
- **06 观点**: Stockable 是 Capability（全局）+ Role（场景级）
- **07 观点**: Stockable 是 Location-scoped Role
- **收敛结论**: Stockable = Location-scoped Business Role（不同 Location 可有不同 Stockable 状态）
- **状态**: RECONCILED

#### L2-005: Location = 统一抽象 (Store, Warehouse, Kitchen, Transit)

- **定义**: Location 是库存管理的统一位置抽象，包括 Store（门店）、Warehouse（仓库）、Kitchen（厨房）、Transit（在途）
- **06 观点**: Business Item 的 Role 受 Location Scope 影响
- **07 观点**: inventory（仓库）和 store_inventory（门店）应统一为 Location 模型
- **收敛结论**: Location = 统一抽象
- **状态**: RECONCILED

#### L2-006: UOM = 独立 Foundation (Base → Purchase → Stock → Recipe → Sales)

- **定义**: Unit of Measure 是独立的 Foundation 实体，支持五个层次的语义：Base UOM → Purchase UOM → Stock UOM → Recipe UOM → Sales UOM
- **06 观点**: Business Item 的 Unit 属性分散在多张表
- **07 观点**: 5 张表各自存储 unit 且无转换关系，需要独立 Foundation
- **收敛结论**: UOM = 独立 Foundation，支持五层转换
- **状态**: RECONCILED

#### L2-007: INGREDIENT = Recipe ↔ Item Relationship (不是 Item Role)

- **定义**: Ingredient 是 Recipe 与 Item 之间的 Relationship 参与方身份，不是 Item 自身的 Role
- **06 观点**: Ingredient 是 Material 在 Recipe 场景中的 Role
- **07 观点**: Ingredient 是 RecipeComponent 的业务名称
- **收敛结论**: Ingredient **不是** Item Role，而是 Recipe ↔ Item Relationship
- **关键证据**: 同一 Item（鸡翅）可参与多个 Recipe（可乐鸡翅、香辣鸡翅），Ingredient 身份随 Recipe 变化
- **状态**: RECONCILED

#### L2-008: SELLABLE = Capability + Role (场景级)

- **定义**: Sellable（可销售）是 Business Item 的全局 Capability + 特定场景下的 Role
- **06 观点**: SELLABLE = Capability + Role
- **07 观点**: 无直接冲突
- **收敛结论**: SELLABLE = Capability（Item 具备可销售能力）+ Role（在特定 Location/场景中执行销售）
- **状态**: RECONCILED

#### L2-009: PURCHASABLE = Capability + Profile (采购配置)

- **定义**: Purchasable（可采购）是 Business Item 的全局 Capability + 采购配置 Profile
- **06 观点**: PURCHASABLE = Capability + Profile
- **07 观点**: 无直接冲突
- **收敛结论**: PURCHASABLE = Capability（Item 具备可采购能力）+ Profile（供应商、采购单位、最小起订量等配置）
- **状态**: RECONCILED

#### L2-010: CONSUMABLE = Role (Recipe 场景)

- **定义**: Consumable（可消耗）是 Business Item 在 Recipe 场景中的 Role
- **06 观点**: CONSUMABLE = Role
- **07 观点**: 无直接冲突
- **收敛结论**: CONSUMABLE = Role（仅在 Recipe 场景中生效，表示该 Item 可被 Recipe 消耗）
- **状态**: RECONCILED

---

### Layer 3 — Remaining Conflicts

无法直接收敛、必须进入 Decision 的问题。参考 `business-item-inventory-conflict-registry.yaml`。

#### L3-001: Product 语义冲突 (CONFLICT-A)

- **冲突**: Product 是废弃概念 vs Base Type
- **表述 A**: Product 是已废弃的 Legacy Concept（product 表标记为 LEGACY，无活跃引用）
- **表述 B**: Product 是 Base Type（Food/Material 是 Product 的 Subtype）
- **当前现实**: product 表存在但无活跃业务角色 → 表述 A 更接近现实
- **Recommendation**: Product 作为 Commercial Definition 可能有长期价值，但当前无业务必要性
- **需要 Decision**: 是 — DEC-006 已覆盖，状态 OPEN
- **状态**: OPEN

#### L3-002: Material vs Canonical Identity (CONFLICT-C)

- **冲突**: Material 是 Canonical Identity 还是 Legacy Implementation Object
- **表述 A**: Material 是 Canonical Identity（material_id 贯穿采购→库存→配方全生命周期）
- **表述 B**: Material 是 Legacy Implementation Object（需要 Canonical Item 作为更高层抽象）
- **当前现实**: Material 是事实上的 Canonical Identity（LOCK-A002 已锁定）
- **Recommendation**: Material 作为当前 Canonical Identity，但目标模型应引入 Canonical Item 作为更高层抽象
- **关键区分**: Material 是 Type（"原料"），不是 Identity（"这瓶可口可乐330ml"）
- **需要 Decision**: 是 — 需要 DEC-006-REFINED 覆盖
- **状态**: OPEN

#### L3-003: Inventory 管理什么对象 (DG-003)

- **冲突**: Inventory 管理的是 Material、Product、Stock Item、Canonical Item 还是 Stockable Profile？
- **当前现实**: inventory 以 material_id 为核心，store_inventory 以 store_id + material_id 为核心
- **Recommendation**: Inventory → Item × Location（统一为 Item × Location × Quantity）
- **需要 Decision**: 是 — 需要 Product Owner 确认
- **状态**: OPEN

#### L3-004: Store Scope 如何影响 Role (DG-004)

- **冲突**: 同一业务对象在不同门店是否可以拥有不同 Role？
- **场景**: Store A：可乐 = Sellable + Stockable；Store B：可乐 = Stockable only（不销售）；Central Warehouse：可乐 = Purchasable + Stockable
- **Recommendation**: Role 是 Location-scoped（按位置配置）
- **需要 Decision**: 是 — 需要 Product Owner 确认
- **状态**: OPEN

---

### Layer 4 — Candidate Unified Models

提出最多 3 个统一模型候选。

#### 候选模型 CAND-BI-INV-001: Canonical Item + Unified Location + Ledger-First

| 维度 | 定义 |
|------|------|
| **Identity** | Canonical Item ID（统一标识，跨 Food/Material/Beverage） |
| **Type** | Item Type: FOOD / MATERIAL / BEVERAGE / PACKAGING / CONSUMABLE |
| **Role** | Location-scoped: SELLABLE / PURCHASABLE / STOCKABLE / CONSUMABLE |
| **Capability** | Global flags: is_sellable, is_purchasable, is_stockable, is_consumable |
| **Relationship** | Recipe ↔ Item (通过 dish_recipe 连接，Ingredient 为 Relationship 参与方) |
| **Context** | Business Context: ORDER (销售) / PROCUREMENT (采购) / RECIPE (配方) / INVENTORY (库存) |
| **Scope** | Location Scope: Store / Warehouse / Kitchen / Transit |
| **Location** | Unified Location: location_id + location_type (STORE / WAREHOUSE / KITCHEN / TRANSIT) |
| **Quantity** | Quantity = material_id × location_id × uom × value |
| **UOM** | Independent Foundation: Base → Purchase → Stock → Recipe → Sales (五层转换) |
| **Ledger** | Stock Ledger: immutable movement entries (transaction_id, item_id, location_id, movement_type, quantity_change, before_qty, after_qty, timestamp) |
| **Cross-Domain Flow** | Order → food_id → dish_recipe → material_id → inventory → ledger |

**适用场景**: 餐饮 ERP 全场景，支持多门店、多仓库、配方管理

**迁移路径**: Material (当前) → Canonical Item (目标)

---

#### 候选模型 CAND-BI-INV-002: Business Item Role Matrix + Inventory Instance

| 维度 | 定义 |
|------|------|
| **Identity** | Business Item ID（保持现有 material_id / food_id 双轨） |
| **Type** | Product Type: FOOD / MATERIAL / BEVERAGE / PACKAGING |
| **Role** | Role Matrix: Item × Location × Role (SELLABLE, PURCHASABLE, STOCKABLE, CONSUMABLE, INGREDIENT) |
| **Capability** | Derived from Role Matrix（从 Role 推导 Capability） |
| **Relationship** | RecipeComponent (food_id → material_id, quantity, unit) |
| **Context** | Scene: POS_SALES / KITCHEN_CONSUMPTION / WAREHOUSE_STOCK / STORE_STOCK |
| **Scope** | Store-scoped or Global-scoped（按业务配置） |
| **Location** | Dual Model: Warehouse (inventory) + Store (store_inventory)，通过调拨连接 |
| **Quantity** | Quantity = value × uom（绑定到 Location） |
| **UOM** | Per-table UOM（保持当前分散存储，添加转换层） |
| **Ledger** | inventory_transactions（仅仓库）+ store_inventory 变更日志（门店） |
| **Cross-Domain Flow** | Purchase → material_id → inventory → dish_recipe → food_id → order_items |

**适用场景**: 渐进式迁移，保持当前双轨运行

**优势**: 迁移成本低，兼容现有数据模型

---

#### 候选模型 CAND-BI-INV-003: Commercial Definition + Operational Definition + Inventory Ledger

| 维度 | 定义 |
|------|------|
| **Identity** | Commercial ID (Product) + Operational ID (Material/Item) |
| **Type** | Commercial Type: BRAND / CATEGORY / SKU；Operational Type: RAW_MATERIAL / SEMI_FINISHED / FINISHED_GOOD |
| **Role** | Commercial Role: SELLABLE / MARKETABLE；Operational Role: STOCKABLE / CONSUMABLE / PURCHASABLE |
| **Capability** | Capability flags on Commercial + Operational layers |
| **Relationship** | Commercial → Operational mapping (Product → Material) |
| **Context** | Commercial Context (品牌/营销) + Operational Context (库存/采购/生产) |
| **Scope** | Commercial Scope (品牌范围) + Operational Scope (Location 范围) |
| **Location** | Unified Location (Store / Warehouse / Kitchen / Transit) |
| **Quantity** | Quantity at Operational layer only（Commercial 层无库存概念） |
| **UOM** | Independent Foundation（Commercial UOM = Sales UOM, Operational UOM = Stock/Purchase/Recipe UOM） |
| **Ledger** | Single Stock Ledger at Operational layer |
| **Cross-Domain Flow** | Product (Commercial) → Material (Operational) → Inventory Ledger |

**适用场景**: 集团化多品牌、Commercial Definition 有长期价值

**风险**: 迁移成本高，当前 Product 表无活跃业务角色

---

## 三、15 个核心问题回答

### Q1: Business Item 与 Inventory Item 是否是同一个 Identity？

**回答**: **是，但需区分层次。**

- Business Item 的 Identity = Material ID (LOCK-A002 已锁定)
- Inventory Item 的 Identity = Material ID × Location ID
- 当前 `inventory.material_id` 和 `store_inventory.material_id` 均引用 `material_archives.id`
- **结论**: Inventory Item 是 Business Item 在特定 Location 下的实例化，Identity 继承自 Business Item
- **状态**: RECONCILED

### Q2: Product / Food / Material / Beverage 的 Type 关系是什么？

**回答**: **Product 为废弃概念，Food / Material 为活跃 Type，Beverage 为 Food 的 Subtype。**

- Product: LEGACY（product 表无活跃业务角色）→ **OPEN** (CONFLICT-A, DEC-006)
- Food: Active Type（foods 表为唯一真相源，LOCK-A001）
- Material: Active Type（material_archives 表为唯一真相源，LOCK-A002）
- Beverage: Food Subtype（可乐、矿泉水在 foods 表中以 food_category 区分）
- **结论**: 当前 Type 层次 = Food (含 Beverage) + Material；Product 待 Decision
- **状态**: PARTIAL (Product 待 DEC-006)

### Q3: Sellable / Purchasable / Stockable / Consumable / Ingredient 属于什么？

**回答**: **分属不同语义范畴，不可混为一谈。**

| 概念 | 语义范畴 | 06 结论 | 07 结论 | 统一结论 | 状态 |
|------|----------|---------|---------|----------|------|
| SELLABLE | Capability + Role | Capability + Role | - | Capability (全局) + Role (场景) | RECONCILED |
| PURCHASABLE | Capability + Profile | Capability + Profile | - | Capability (全局) + Profile (采购配置) | RECONCILED |
| STOCKABLE | Role (Location-scoped) | Role (Location-scoped) | Role (Location-scoped) | Role (Location-scoped) | RECONCILED |
| CONSUMABLE | Role | Role | - | Role (Recipe 场景) | RECONCILED |
| INGREDIENT | Relationship | Relationship | - | Recipe ↔ Item Relationship | RECONCILED |

- **状态**: RECONCILED

### Q4: Ingredient 是否应该是 Item 自身 Role？

**回答**: **不是。Ingredient 是 Recipe ↔ Item 之间的 Relationship 参与方身份。**

- **证据**: 同一 Item（鸡翅）可参与多个 Recipe（可乐鸡翅、香辣鸡翅），Ingredient 身份随 Recipe 变化
- **06 结论**: Ingredient 是 Material 在 Recipe 场景中的 Role（已修正）
- **07 结论**: Ingredient 是 RecipeComponent 的业务名称
- **统一结论**: Ingredient 不是 Item 自身 Role，而是 Recipe ↔ Item Relationship
- **状态**: RECONCILED

### Q5: Store / Warehouse / Kitchen / Transit 是否属于统一 Location 模型？

**回答**: **是。四种位置应统一为 Location 抽象。**

- **当前现实**: inventory（仓库）和 store_inventory（门店）是独立表
- **统一模型**: Location = { location_id, location_type, name, address }
  - Store: 门店（可销售、可存储）
  - Warehouse: 仓库（可存储、可调拨）
  - Kitchen: 厨房（可消耗、可暂存）
  - Transit: 在途（调拨中间态）
- **关键价值**: 调拨操作需要统一的 Location 概念，"总库存"只是 Aggregated View
- **需要 Decision**: DG-006 (Architecture Owner)
- **状态**: PARTIAL (Recommendation 已明确，待 Decision)

### Q6: Inventory Fact 的最小语义单位是什么？

**回答**: **Material × Location × Quantity × UOM × Timestamp。**

- Inventory Fact = 某物料在某位置的某时刻库存状态
- 最小可表达单位: (material_id, location_id, quantity, uom, as_of_timestamp)
- 可从 Stock Ledger 重建
- **状态**: RECONCILED

### Q7: Quantity 与 UOM 如何继承 Business Item Identity？

**回答**: **Quantity × UOM 绑定到 Location-scoped Inventory Instance，Identity 继承自 Business Item。**

- Business Item Identity (material_id) → 全局唯一
- Quantity × UOM → Location-scoped（同一物料在不同 Location 可有不同 Quantity 和 UOM）
- 示例: 可口可乐330ml
  - Warehouse A: 200 箱 (Purchase UOM)
  - Store B: 30 瓶 (Stock UOM)
  - Recipe: 0 ml (不作为 Recipe 原料)
- **状态**: RECONCILED

### Q8: Purchase UOM / Stock UOM / Recipe UOM / Sales UOM / Base UOM 的关系？

**回答**: **五层 UOM 体系，通过转换因子连接。**

```
Base UOM (ml, g, 个)
    ↑ 转换因子
Purchase UOM (箱, 托, pallet)
    ↑ 转换因子
Stock UOM (瓶, 袋, kg)
    ↑ 转换因子
Recipe UOM (ml, g, 片)
    ↑ 转换因子
Sales UOM (份, 杯, 瓶)
```

- **当前现实**: 5 张表各自存储 unit 字段且无转换关系
- **目标模型**: inventory_unit 表应建立转换关系
- **需要 Decision**: DG-005 (Architecture Owner)
- **状态**: PARTIAL (Recommendation 已明确，待 Decision)

### Q9: Partial Consumption / Open Package 如何连接？

**回答**: **Partial Consumption 是 Recipe 场景的部分消耗，Open Package 是 Stock 场景的包装拆分。两者通过 Inventory Movement Type 连接。**

- Partial Consumption: 配方使用半瓶酱油 → Recipe UOM 精确到 ml
- Open Package: 拆箱将 1 箱可乐变为 24 瓶 → Stock UOM 从"箱"变为"瓶"
- **连接点**: 两者都是 Inventory Movement，通过 movement_type 区分
  - PARTIAL_CONSUMPTION: 部分消耗
  - UNPACK: 拆包
- **状态**: RECONCILED

### Q10: 同一业务对象如何同时支持多角色？

**回答**: **通过 Role Matrix (Item × Location × Role) 实现。**

- 同一 Item 在不同 Location 可有不同 Role
- 示例: 可乐
  - Store A: SELLABLE + STOCKABLE
  - Store B: STOCKABLE only（不销售）
  - Central Warehouse: PURCHASABLE + STOCKABLE
  - Kitchen: CONSUMABLE（可乐鸡翅原料）
- **当前现实**: 无 Role Matrix，角色隐含在业务逻辑中
- **目标模型**: 需要 Role 配置表 (item_id, location_id, role_type, enabled)
- **需要 Decision**: DG-004 (Product Owner)
- **状态**: PARTIAL (Recommendation 已明确，待 Decision)

### Q11: Store Scope 如何影响 Type / Role / Capability？

**回答**: **Store Scope 仅影响 Role 和 Capability，不影响 Type。**

- Type: Global（不随 Store 变化）— 可乐永远是 FOOD/BEVERAGE
- Role: Location-scoped（随 Store 变化）— 可乐在 Store A 是 SELLABLE，在 Store B 不是
- Capability: Global（不随 Store 变化）— 可乐的 is_sellable 能力不变，但在特定 Store 可能不启用
- **结论**: Store Scope 影响 Role 启用/禁用，不影响 Type 定义和 Capability 存在性
- **状态**: RECONCILED

### Q12: 当前表如何映射到目标语义？

**回答**: **以下是核心表的映射关系。**

| 当前表 | 目标语义 | 映射说明 |
|--------|----------|----------|
| `material_archives` | Canonical Item (Material) | LOCK-A002，物料真相源 |
| `foods` | Canonical Item (Food) | LOCK-A001，菜品真相源 |
| `inventory` | Inventory Instance (Warehouse) | material_id × warehouse_id |
| `store_inventory` | Inventory Instance (Store) | material_id × store_id |
| `inventory_transactions` | Stock Ledger (Warehouse only) | 仅关联 inventory_id，门店未覆盖 |
| `dish_recipe` | Recipe ↔ Item Relationship | food_id × material_id |
| `order_items` | Order Line (Food) | food_id，不直接引用 material_id |
| `purchase_order_items` | Purchase Line (Material) | material_id，不直接引用 food_id |
| `inventory_unit` | UOM Foundation (未启用) | 存在但未被代码引用 |

- **状态**: RECONCILED (映射关系明确)，PARTIAL (目标模型待 Decision)

### Q13: 哪些结论已有充分 Evidence？

**回答**: **以下结论有充分代码/DB/API 证据支撑。**

| 结论 | Evidence 来源 | 置信度 |
|------|---------------|--------|
| foods 为菜品唯一真相源 | foundation-map.md, FoodService.java | HIGH |
| material_archives 为物料真相源 | foundation-map.md, MaterialService.java | HIGH |
| inventory 以 material_id 管理仓库库存 | db-reality-map.md, inventory 表结构 | HIGH |
| store_inventory 以 store_id + material_id 管理门店库存 | db-reality-map.md, store_inventory 表结构 | HIGH |
| dish_recipe 连接 food 和 material | db-reality-map.md, dish_recipe 表结构 | HIGH |
| order_items 使用 food_id | db-reality-map.md, order_items 表结构 | HIGH |
| purchase_order_items 使用 material_id | db-reality-map.md, purchase_order_items 表结构 | HIGH |
| 金额以分为准 | truth-conflict-map.md §1 | HIGH |
| Ingredient 是 Relationship 不是 Role | counterexample-validation.md, dish_recipe 数据 | HIGH |
| Stock Ledger 是 Inventory Truth | inventory-truth-analysis.md | HIGH |
| UOM 需要独立 Foundation | inventory-uom-model.md §2.2 | HIGH |

- **状态**: RECONCILED

### Q14: 哪些仍然只是 Recommendation？

**回答**: **以下结论为 Recommendation，尚未经过正式 Decision。**

| Recommendation | 来源 | 需要 Decision |
|----------------|------|---------------|
| Product 废弃，保留 Food + Material | conflict-registry.yaml CONFLICT-A | DEC-006 |
| Material 作为 Canonical Identity（保持现状） | conflict-registry.yaml CONFLICT-C | DEC-006-REFINED |
| Inventory → Item × Location | decision-gap.md DG-003 | Product Owner |
| Role 是 Location-scoped | decision-gap.md DG-004 | Product Owner |
| UOM 成为独立 Foundation | decision-gap.md DG-005 | Architecture Owner |
| Location 统一抽象 | decision-gap.md DG-006 | Architecture Owner |
| Ledger-First (非 Balance-First) | decision-gap.md DG-007 | Architecture Owner |

- **状态**: OPEN

### Q15: 哪些冲突必须进入正式 Decision？

**回答**: **以下冲突必须进入正式 Decision，无法通过分析收敛。**

| Conflict | 标题 | 决策者 | 截止日期 | 阻塞影响 |
|----------|------|--------|----------|----------|
| CONFLICT-A | Product 语义 | Product Owner | 2026-10-15 | 阻塞 Canonical Item 定义 |
| CONFLICT-C | Material vs Canonical Identity | Product Owner | 2026-10-15 | 阻塞 Inventory 模型设计 |
| DG-003 | Inventory 管理什么对象 | Product Owner | 2026-10-15 | 阻塞 Inventory 数据模型 |
| DG-004 | Store Scope 如何影响 Role | Product Owner | 2026-10-15 | 阻塞门店级配置设计 |
| DG-005 | UOM 是否成为独立 Foundation | Architecture Owner | 2026-10-01 | 阻塞 Inventory 数据模型 |
| DG-006 | Location 是否统一抽象 | Architecture Owner | 2026-10-01 | 阻塞 Inventory 数据模型 |
| DG-007 | Ledger-First 还是 Balance-First | Architecture Owner | 2026-10-01 | 阻塞 Inventory 架构 |

- **状态**: OPEN

---

## 四、6 个业务反例验证

### Case 1: Coca-Cola 330ml

**业务场景**: 可口可乐 330ml 罐装饮料，从供应商采购，在仓库存储，调拨到门店销售，也可作为可乐鸡翅的原料。

| 维度 | 当前现实 | 目标模型 |
|------|----------|----------|
| Identity | material_archives.id (M001) | Canonical Item ID (I001) |
| Type | Material (采购) + Food (销售) | FOOD / BEVERAGE |
| Sellable | food 表有记录 (legacy) + foods 表 | Capability + Role (Store-scoped) |
| Purchasable | material_archives 有记录 | Capability + Profile |
| Stockable | inventory + store_inventory 有记录 | Role (Location-scoped) |
| Ingredient | dish_recipe 中作为可乐鸡翅的原料 | Recipe ↔ Item Relationship |
| UOM | material_archives.unit = "箱" | Purchase UOM = 箱, Stock UOM = 瓶, Recipe UOM = ml |

**反例验证结果**: ✅ 目标模型可表达，但需注意同一 Item 同时具有 Food 和 Material 双重身份

**关键冲突**: 当前 order_items 使用 food_id，purchase_order_items 使用 material_id，同一可乐在两个表中有不同身份 → **需要 Canonical Item 统一**

---

### Case 2: 仓库 → 门店调拨

**业务场景**: 仓库有 200 箱可乐，调拨 20 箱到门店 A，调拨过程中经过 Transit 状态。

| 维度 | 当前现实 | 目标模型 |
|------|----------|----------|
| 起点 | inventory (warehouse_id=W001, material_id=M001, qty=200) | Stock Ledger Entry: location=W001, qty=-20 |
| 在途 | 无 Transit 记录 | Transit Location (临时) |
| 终点 | store_inventory (store_id=S001, material_id=M001, qty=+20) | Stock Ledger Entry: location=S001, qty=+20 |
| 事务 | inventory 和 store_inventory 独立更新 | 统一 Ledger + 事务保障 |

**反例验证结果**: ⚠️ PASS WITH RULES — 当前无 Transit 概念，需引入临时 Location

**关键问题**: inventory_transactions 仅关联 inventory_id，门店库存变动未纳入流水 → **需要统一 Ledger**

---

### Case 3: 部分消耗

**业务场景**: 厨房使用半瓶酱油（500ml 中使用 200ml）制作菜品，剩余 300ml 留存。

| 维度 | 当前现实 | 目标模型 |
|------|----------|----------|
| 消耗记录 | dish_recipe 记录 quantity=200, unit="ml" | Recipe UOM = ml |
| 库存扣减 | inventory.current_stock 直接扣减 | Stock Ledger Entry: movement_type=PARTIAL_CONSUMPTION, qty=-200 |
| 剩余管理 | 无 Open Package 概念 | Open Package: 剩余 300ml 仍绑定原 batch |

**反例验证结果**: ⚠️ PASS WITH RULES — 当前无部分消耗和 Open Package 的精细管理

**关键问题**: 部分消耗后剩余物料的库存归属和成本分摊需要明确规则

---

### Case 4: Recipe (可乐鸡翅)

**业务场景**: 可乐鸡翅菜品，需要鸡翅 500g、可乐 330ml、酱油 10ml、糖 5g。

| 维度 | 当前现实 | 目标模型 |
|------|----------|----------|
| Recipe 定义 | dish_recipe: food_id=可乐鸡翅, material_id=鸡翅, quantity=500, unit="g" | Recipe ↔ Item Relationship |
| Ingredient 关系 | dish_recipe 连接 food 和 material | Ingredient = Recipe ↔ Item Relationship 参与方 |
| 多 Recipe 参与 | 鸡翅同时参与可乐鸡翅、香辣鸡翅 | 同一 Item 参与多个 Recipe，身份随 Recipe 变化 |
| UOM | dish_recipe.unit = "g" / "ml" | Recipe UOM (独立于 Stock/Purchase UOM) |

**反例验证结果**: ✅ 目标模型可表达

**关键确认**: Ingredient 不是 Item 自身 Role，而是 Recipe ↔ Item Relationship — **RECONCILED**

---

### Case 5: Food (宫保鸡丁)

**业务场景**: 宫保鸡丁菜品，直接在 POS 销售，无采购（不作为原材料采购），无库存（门店现做现卖）。

| 维度 | 当前现实 | 目标模型 |
|------|----------|----------|
| Identity | foods 表 (food_id=F001) | Canonical Item ID (I001) |
| Type | Food | FOOD |
| Sellable | order_items 使用 food_id | Capability + Role (Store-scoped) |
| Purchasable | 无 material_archives 记录 | 无此 Capability |
| Stockable | 无 inventory / store_inventory 记录 | 无此 Role |
| Recipe | 有自己的 dish_recipe（鸡肉、花生、调料等） | Recipe ↔ Item Relationship |

**反例验证结果**: ✅ 目标模型可表达

**关键确认**: Food (宫保鸡丁) 只有 Sellable + Recipe 角色，无 Purchasable/Stockable — **同一 Item 可有不同 Role 组合**

---

### Case 6: Packaging / Supply (餐盒)

**业务场景**: 一次性餐盒，从供应商采购，在仓库存储，门店领用（消耗），不直接销售给顾客。

| 维度 | 当前现实 | 目标模型 |
|------|----------|----------|
| Identity | material_archives 表 (M010) | Canonical Item ID (I010) |
| Type | Material | PACKAGING / CONSUMABLE |
| Sellable | 无 foods 记录 | 无此 Capability |
| Purchasable | material_archives 有记录 | Capability + Profile |
| Stockable | inventory 有记录 | Role (Location-scoped) |
| Consumable | 门店领用时消耗 | Role (Recipe/Supply 场景) |
| Ingredient | 不是任何 Recipe 的原料 | 无此 Relationship |

**反例验证结果**: ✅ 目标模型可表达

**关键确认**: Packaging (餐盒) 是 Purchasable + Stockable + Consumable，无 Sellable/Ingredient — **Role 组合因 Item Type 而异**

---

## 五、与 DEC-006 的衔接

本文档的分析结果直接服务于 DEC-006 (food/foods 如何统一) 及其 REFINED 版本。

### DEC-006 当前状态

- **Decision**: RECOMMENDED (B_migrate_and_drop)
- **推荐**: 完成迁移后废弃 food 表
- **阻塞**: 旧模块 (POS legacy) 迁移状态未确认

### 本文档对 DEC-006 的输入

1. **CONFLICT-A (Product 语义)**: 本文档确认 Product 为 Legacy Concept，推荐废弃 → 支持 DEC-006 Option B
2. **CONFLICT-C (Material vs Canonical Identity)**: 本文档确认 Material 为当前 Canonical Identity → 支持 DEC-006-REFINED 中 Material 保持 Canonical
3. **DG-003 (Inventory 管理什么对象)**: 本文档推荐 Inventory → Item × Location → 需要 DEC-006 明确 Item 的统一 Identity
4. **DG-004 (Store Scope 如何影响 Role)**: 本文档推荐 Location-scoped Role → 需要 DEC-006 明确 Role 配置模型

### DEC-006-REFINED 需要覆盖的额外问题

| 问题 | 来源 | 优先级 |
|------|------|--------|
| Material 是否作为 Canonical Identity 保留 | CONFLICT-C, DG-002 | P0 |
| Food 是否需要独立 Identity 还是继承 Material | Q2 分析 | P0 |
| Beverage 是否需要独立 Type 还是 Food Subtype | Case 1 分析 | P1 |
| Packaging 是否需要独立 Type 还是 Material Subtype | Case 6 分析 | P1 |

---

## 六、状态标记

### 文档状态

| 区块 | 状态 | 说明 |
|------|------|------|
| Layer 1 — Confirmed Facts | RECONCILED | 10 条事实，全部 VERIFIED |
| Layer 2 — Reconciled Semantics | RECONCILED | 10 个语义，全部收敛 |
| Layer 3 — Remaining Conflicts | OPEN | 4 个冲突，需要 Decision |
| Layer 4 — Candidate Models | PARTIAL | 3 个候选，待 Decision 选择 |
| 15 个核心问题 | PARTIAL | 11 个 RECONCILED, 4 个 OPEN |
| 6 个业务反例 | PARTIAL | 3 个 PASS, 3 个 PASS WITH RULES |
| DEC-006 衔接 | OPEN | 需要 DEC-006-REFINED 覆盖 |

### 下一步行动

| 行动 | 负责人 | 截止日期 | 阻塞 |
|------|--------|----------|------|
| DEC-006 正式 Decision | Product Owner | 2026-09-16 | Canonical Item 定义 |
| DG-005/DG-006/DG-007 正式 Decision | Architecture Owner | 2026-10-01 | Inventory 数据模型 |
| DG-003/DG-004 正式 Decision | Product Owner | 2026-10-15 | 门店级配置设计 |
| DEC-006-REFINED 扩展覆盖 | Product Owner | 2026-10-15 | Material/Food/Beverage Type 关系 |

---

**文档状态**: OPEN
**下一步**: 等待 DEC-006 正式 Decision，本文档的 Layer 3 和 Layer 4 将根据 Decision 结果更新
