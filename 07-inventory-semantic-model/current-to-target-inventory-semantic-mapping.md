# 当前→目标库存语义映射 (Phase 21 - Current-to-Target Inventory Semantic Mapping)

> **核心问题**：当前数据库表结构中的每个字段，映射到目标业务语义概念的哪个部分？
> **状态**：ANALYSIS, NOT CONFIRMED

---

## 1. 问题域

本分析必须回答：
1. 当前每张表的每个字段，对应的业务语义是什么？
2. 哪些字段在目标模型中直接保留？哪些需要重构？哪些废弃？
3. 当前表之间的关系，在目标语义中如何重新表达？
4. 映射中的语义断层（Semantic Gap）在哪里？

**关键约束**：
- Current DB Model ≠ Target Inventory Model
- 不要因为当前实现就认定未来模型
- 必须从业务语义角度分析
- 这是业务语义映射，不是数据库迁移计划

---

## 2. 当前表结构概览

### 2.1 表清单

| # | 当前表 | 核心职责 | 当前角色 |
|---|--------|----------|----------|
| 1 | `inventory` | 仓库维度库存余额 | 仓库库存主表 |
| 2 | `store_inventory` | 门店维度库存余额 | 门店库存主表 |
| 3 | `material_archives` | 物料主数据 | 采购+库存的 Canonical Identity |
| 4 | `foods` | 菜品/食品主数据 | 销售域主标识 |
| 5 | `product` | 商品主数据（已废弃） | 历史遗留 |
| 6 | `dish_recipe` | 菜品配方（原料组成） | 跨域桥接表 |
| 7 | `purchase_order_items` | 采购订单行项 | 采购域 |
| 8 | `order_items` | 销售订单行项 | 销售域 |
| 9 | `inventory_transactions` | 库存变动流水 | 仓库维度流水 |
| 10 | `inventory_unit` | 库存单位定义 | 辅助表 |

---

## 3. 逐表语义映射

### 3.1 inventory（仓库库存表）

| 当前字段 | 当前语义 | 目标语义映射 | 目标模型对应 | 迁移判定 |
|----------|----------|-------------|-------------|----------|
| `inventory_id` | 仓库库存记录ID | 库存事实ID（仓库维度） | `inventory_fact.warehouse_fact_id` | 重构：语义不变，ID体系重建 |
| `material_id` | 物料ID | Canonical Item Identity（通过material_archives） | `inventory_fact.item_id` | 保留：material_id 是当前系统的 Canonical Identity |
| `warehouse_id` | 仓库ID | Location Identity（仓库维度） | `inventory_fact.location_id` | 保留：语义一致 |
| `current_stock` | 当前库存数量 | **Derived Value**（从 Stock Ledger 聚合） | `inventory_fact.quantity` | 重构：不再独立存储，从流水重建 |
| `locked_quantity` | 锁定数量 | Stock Position（业务可用量的一部分） | `stock_position.reserved_qty` | 重构：移入 Stock Position 计算层 |
| `unit_cost` | 单位成本 | Costing Method Result（成本核算结果） | `inventory_cost.current_unit_cost` | 重构：移入 Costing 模型 |
| `total_cost` | 总成本 | Derived Value（quantity × unit_cost） | 不独立存储 | 废弃：可计算得出 |
| `batch_no` | 批次号 | Batch/Lot Identity | `inventory_fact.batch_id` | 保留：批次管理需要 |
| `min_safe_qty` | 最小安全库存 | Stock Position 配置 | `stock_position_config.min_qty` | 重构：移入配置层 |
| `max_stock_qty` | 最大库存 | Stock Position 配置 | `stock_position_config.max_qty` | 重构：移入配置层 |

**语义断层分析**：

| 断层 | 影响 | 说明 |
|------|------|------|
| `current_stock` 是 Derived 而非 Truth | 高 | 目标模型中库存余额必须从 Stock Ledger 重建，不能独立存储 |
| `unit_cost` 混合了成本和库存 | 中 | 目标模型中 Costing 是独立域，与库存数量解耦 |
| `locked_quantity` 语义模糊 | 中 | 是"已锁定待出库"还是"已分配未扣减"？目标模型需要精确语义 |

---

### 3.2 store_inventory（门店库存表）

| 当前字段 | 当前语义 | 目标语义映射 | 目标模型对应 | 迁移判定 |
|----------|----------|-------------|-------------|----------|
| `store_id` | 门店ID | Location Identity（门店维度） | `inventory_fact.location_id` | 保留：语义一致 |
| `material_id` | 物料ID | Canonical Item Identity | `inventory_fact.item_id` | 保留 |
| `current_stock` | 当前库存数量 | **Derived Value**（从门店流水聚合） | `inventory_fact.quantity` | 重构：同 inventory |
| `unit` | 库存单位 | UOM Identity | `inventory_fact.uom_id` | 保留：但需要 UOM 标准化 |
| `unit_cost` | 单位成本 | Costing Method Result | `inventory_cost.current_unit_cost` | 重构：同 inventory |
| `total_cost` | 总成本 | Derived Value | 不独立存储 | 废弃 |
| `safety_stock` | 安全库存 | Stock Position 配置 | `stock_position_config.min_qty` | 重构 |
| `max_stock` | 最大库存 | Stock Position 配置 | `stock_position_config.max_qty` | 重构 |

**语义断层分析**：

| 断层 | 影响 | 说明 |
|------|------|------|
| 门店库存无 `batch_no` | 高 | 门店是否需要批次管理？当前实现缺失，目标模型需要决策 |
| 门店库存无 `locked_quantity` | 中 | 门店是否存在"锁定但未出库"的场景？ |
| 与 `inventory` 表结构不一致 | 高 | 两表字段不统一，增加了维护成本和语义歧义 |

---

### 3.3 material_archives（物料主数据）

| 当前字段 | 当前语义 | 目标语义映射 | 目标模型对应 | 迁移判定 |
|----------|----------|-------------|-------------|----------|
| `material_id` | 物料唯一标识 | **Canonical Item Identity** | `canonical_item.item_id` | 保留：当前系统的事实标准 |
| `material_name` | 物料名称 | Item Display Name | `canonical_item.name` | 保留 |
| `material_code` | 物料编码 | Item Code / SKU | `canonical_item.code` | 保留 |
| `category_id` | 物料分类 | Item Category | `canonical_item.category_id` | 保留 |
| `unit` | 基础单位 | Primary UOM | `canonical_item.primary_uom_id` | 保留：但需要 UOM 标准化 |
| `unit_price` | 参考价格 | Cost Reference / Last Purchase Price | `canonical_item.reference_cost` | 重构：语义从"价格"变为"参考成本" |
| `supplier_id` | 供应商ID | Supplier Reference | `canonical_item.preferred_supplier_id` | 保留：但可选 |
| `status` | 状态 | Item Status | `canonical_item.status` | 保留 |
| `is_raw_material` | 是否原材料 | Item Type Classification | `canonical_item.item_type` | 重构：枚举值扩展 |
| `is_sellable` | 是否可售 | Item Capability Flag | `item_capability.is_sellable` | 重构：移入 Capability 模型 |
| `is_purchasable` | 是否可采购 | Item Capability Flag | `item_capability.is_purchasable` | 重构：移入 Capability 模型 |

**语义断层分析**：

| 断层 | 影响 | 说明 |
|------|------|------|
| `is_raw_material` 二元分类不足 | 高 | 可乐既是原材料又是预包装品，二元分类无法表达多角色 |
| `unit_price` 语义混淆 | 中 | 是"最近采购价"还是"标准成本"？不同语义影响成本核算 |
| 缺少 Item Type 体系 | 高 | 目标模型需要完整的 Item Type 分类（Raw/Finished/Packaged/Service） |

---

### 3.4 foods（菜品/食品表）

| 当前字段 | 当前语义 | 目标语义映射 | 目标模型对应 | 迁移判定 |
|----------|----------|-------------|-------------|----------|
| `food_id` | 菜品唯一标识 | Sellable Item Identity | `sellable_item.sellable_item_id` | 保留：销售域主标识 |
| `food_name` | 菜品名称 | Sellable Item Display Name | `sellable_item.name` | 保留 |
| `food_code` | 菜品编码 | Sellable Item Code | `sellable_item.code` | 保留 |
| `price` | 售价 | Sellable Item Price | `sellable_item.price` | 保留 |
| `category_id` | 菜品分类 | Sellable Item Category | `sellable_item.category_id` | 保留 |
| `status` | 状态 | Sellable Item Status | `sellable_item.status` | 保留 |

**语义断层分析**：

| 断层 | 影响 | 说明 |
|------|------|------|
| `food_id` 与 `material_id` 无显式映射 | **极高** | 预包装品（可乐、薯片）在两个表中各有一个 Identity，无物理映射 |
| `foods` 不区分现做vs预包装 | 高 | 现做菜品（宫保鸡丁）和预包装品（可乐）语义完全不同，但混在同一表 |
| `foods` 不包含成本信息 | 中 | 销售域不关心成本，但需要知道 Recipe 成本（通过 dish_recipe） |

---

### 3.5 product（商品表 - 已废弃）

| 当前字段 | 当前语义 | 目标语义映射 | 目标模型对应 | 迁移判定 |
|----------|----------|-------------|-------------|----------|
| `product_id` | 商品ID | **已废弃**：值实际是 material_id | `canonical_item.item_id` | 废弃：用 material_id 替代 |
| `product_name` | 商品名称 | 与 material_name 重复 | `canonical_item.name` | 废弃 |

**结论**：`product` 表是历史遗留，其 `product_id` 在活跃代码中实际是 `material_id`。目标模型中完全废弃，由 `canonical_item` 替代。

---

### 3.6 dish_recipe（菜品配方表）

| 当前字段 | 当前语义 | 目标语义映射 | 目标模型对应 | 迁移判定 |
|----------|----------|-------------|-------------|----------|
| `dish_id` | 菜品ID | Sellable Item Identity | `recipe.sellable_item_id` | 保留 |
| `ingredient_id` | 原料ID | Canonical Item Identity | `recipe.ingredient_item_id` | 保留 |
| `quantity` | 用量 | Recipe Quantity | `recipe.quantity` | 保留 |
| `unit` | 单位 | UOM Identity | `recipe.uom_id` | 保留：需要 UOM 标准化 |

**语义断层分析**：

| 断层 | 影响 | 说明 |
|------|------|------|
| `dish_id` 指向 `foods.food_id` | 高 | 是跨域桥接：销售域（food）→ 库存域（material） |
| 配方不含成本信息 | 中 | 需要从 `material_archives.unit_price` 计算 |
| 配方不含替代料逻辑 | 中 | 目标模型是否需要支持替代料？ |

---

### 3.7 purchase_order_items（采购订单行项）

| 当前字段 | 当前语义 | 目标语义映射 | 目标模型对应 | 迁移判定 |
|----------|----------|-------------|-------------|----------|
| `order_id` | 采购订单ID | Purchase Order Identity | `purchase_order.order_id` | 保留 |
| `material_id` | 物料ID | Canonical Item Identity | `purchase_order_item.item_id` | 保留 |
| `quantity` | 采购数量 | Purchase Quantity | `purchase_order_item.quantity` | 保留 |
| `unit_price` | 采购单价 | Purchase Unit Price | `purchase_order_item.unit_price` | 保留 |
| `received_quantity` | 已收货数量 | Receipt Quantity（收货追踪） | `receipt_item.received_qty` | 重构：移入收货域 |

**语义断层分析**：

| 断层 | 影响 | 说明 |
|------|------|------|
| `received_quantity` 混在采购行项 | 中 | 目标模型中收货是独立事件，不应存储在采购订单行项 |
| 采购单位与库存单位可能不同 | 高 | 采购"箱"，库存"瓶"，需要 UOM 转换 |

---

### 3.8 order_items（销售订单行项）

| 当前字段 | 当前语义 | 目标语义映射 | 目标模型对应 | 迁移判定 |
|----------|----------|-------------|-------------|----------|
| `order_id` | 销售订单ID | Sales Order Identity | `sales_order.order_id` | 保留 |
| `food_id` | 菜品ID | Sellable Item Identity | `sales_order_item.sellable_item_id` | 保留 |
| `quantity` | 销售数量 | Sales Quantity | `sales_order_item.quantity` | 保留 |
| `unit_price` | 售价 | Sales Unit Price | `sales_order_item.unit_price` | 保留 |

**语义断层分析**：

| 断层 | 影响 | 说明 |
|------|------|------|
| `food_id` 不直接关联库存 | 高 | 销售域使用 `food_id`，库存域使用 `material_id`，需要 Recipe 桥接 |
| 销售出库的触发点 | 高 | POS 销售如何触发库存扣减？通过 Recipe → Material → Inventory |

---

### 3.9 inventory_transactions（库存变动流水）

| 当前字段 | 当前语义 | 目标语义映射 | 目标模型对应 | 迁移判定 |
|----------|----------|-------------|-------------|----------|
| `transaction_id` | 流水ID | Stock Ledger Entry ID | `stock_ledger.entry_id` | 保留 |
| `material_id` | 物料ID | Canonical Item Identity | `stock_ledger.item_id` | 保留 |
| `inventory_id` | 关联仓库库存 | Warehouse Fact Reference | `stock_ledger.warehouse_fact_id` | 重构：移除直接关联 |
| `warehouse_id` | 仓库ID | Location Identity | `stock_ledger.location_id` | 保留 |
| `transaction_type` | 交易类型（枚举） | Movement Type | `stock_ledger.movement_type` | 保留：枚举值扩展 |
| `quantity_change` | 数量变动 | Movement Quantity | `stock_ledger.quantity` | 保留：正数入库，负数出库 |
| `before_qty` | 变动前数量 | **Derived**（可从流水重建） | 不独立存储 | 废弃：违反 Ledger 不可变原则 |
| `after_qty` | 变动后数量 | **Derived**（可从流水重建） | 不独立存储 | 废弃：同上 |
| `unit_cost` | 单位成本 | Movement Cost | `stock_ledger.unit_cost` | 保留：每笔流水的成本快照 |
| `total_cost` | 总成本 | **Derived**（quantity × unit_cost） | 不独立存储 | 废弃 |
| `remark` | 备注 | Movement Metadata | `stock_ledger.metadata` | 保留 |

**语义断层分析**：

| 断层 | 影响 | 说明 |
|------|------|------|
| `before_qty` / `after_qty` 违反 Ledger 原则 | 高 | 目标模型中 Ledger 是 append-only，不应存储可变的"当前状态" |
| 仅覆盖仓库维度 | **极高** | 门店库存变动无流水记录，违反 Truth 一致性 |
| `transaction_type` 枚举不足 | 中 | 缺少"厨房消耗"、"POS销售扣减"、"拆箱"等类型 |

---

### 3.10 inventory_unit（库存单位表）

| 当前字段 | 当前语义 | 目标语义映射 | 目标模型对应 | 迁移判定 |
|----------|----------|-------------|-------------|----------|
| `unit_id` | 单位ID | UOM Identity | `uom.uom_id` | 保留 |
| `unit_name` | 单位名称 | UOM Display Name | `uom.name` | 保留 |
| `conversion_factor` | 换算系数 | UOM Conversion Factor | `uom_conversion.factor` | 重构：移入转换表 |

**语义断层分析**：

| 断层 | 影响 | 说明 |
|------|------|------|
| 换算系数存储在单位表 | 中 | 目标模型中 UOM Conversion 是独立的 N×N 关系表 |
| 缺少 UOM Category | 中 | "kg"和"g"可以互换，"kg"和"个"不能互换，需要 Category 约束 |

---

## 4. 跨表语义关系映射

### 4.1 Identity 关系映射

```
当前系统：                          目标语义：
material_archives.material_id  →   canonical_item.item_id (主标识)
foods.food_id                  →   sellable_item.sellable_item_id (销售域标识)
product.product_id             →   (废弃，值=material_id)
inventory.material_id          →   inventory_fact.item_id (关联)
store_inventory.material_id    →   inventory_fact.item_id (关联)
purchase_order_items.material_id → purchase_order_item.item_id (关联)
order_items.food_id            →   sales_order_item.sellable_item_id (关联)
```

### 4.2 跨域桥接映射

| 当前桥接方式 | 目标桥接方式 | 说明 |
|-------------|-------------|------|
| `dish_recipe.dish_id` → `foods.food_id` | `recipe.sellable_item_id` → `sellable_item.item_id` | 保留：菜品→原料的桥接 |
| `dish_recipe.ingredient_id` → `material_archives.material_id` | `recipe.ingredient_item_id` → `canonical_item.item_id` | 保留：原料→规范标识的桥接 |
| `order_items.food_id` → `foods.food_id` | 通过 Recipe 桥接 → 库存扣减 | 保留：销售→库存的间接桥接 |
| **预包装品：foods ↔ material_archives** | **item_identity_map** | **新增：解决 Identity Duplicate** |

### 4.3 库存事实关系映射

| 当前关系 | 目标关系 | 说明 |
|---------|---------|------|
| `inventory(material_id, warehouse_id)` | `inventory_fact(item_id, location_id)` | 统一模型 |
| `store_inventory(material_id, store_id)` | `inventory_fact(item_id, location_id)` | 统一模型 |
| `inventory_transactions` → `inventory` | `stock_ledger` → `inventory_fact` (Derived) | Ledger 是 Truth，Fact 是 View |

---

## 5. 语义断层总结

### 5.1 高优先级断层

| # | 断层 | 当前状态 | 目标状态 | 影响 |
|---|------|---------|---------|------|
| 1 | **仓库/门店双表** | `inventory` + `store_inventory` 独立 | 统一 `inventory_fact` + Location 维度 | 极高：两套库存 Truth |
| 2 | **门店无流水** | 门店库存变动无记录 | 统一 `stock_ledger` 覆盖所有 Location | 极高：无法审计门店库存 |
| 3 | **预包装品 Identity Duplicate** | `foods` 和 `material_archives` 各有 Identity | `item_identity_map` 映射表 | 高：同一物理实体两个身份 |
| 4 | **`current_stock` 是 Derived** | 独立存储，可能与流水不一致 | 从 Stock Ledger 重建 | 高：数据一致性风险 |
| 5 | **`before_qty`/`after_qty` 违反 Ledger** | 流水中存储可变状态 | Ledger append-only，状态从流水计算 | 高：违反不可变原则 |

### 5.2 中优先级断层

| # | 断层 | 当前状态 | 目标状态 | 影响 |
|---|------|---------|---------|------|
| 6 | **UOM 标准化** | 单位存储在各表，无统一转换 | `uom` + `uom_conversion` 独立模型 | 中 |
| 7 | **Costing 混在库存表** | `unit_cost` 在 `inventory`/`store_inventory` | Costing 独立域 | 中 |
| 8 | **Item Type 分类不足** | `is_raw_material` 二元 | 完整 Item Type 体系 | 中 |
| 9 | **`received_quantity` 混在采购行** | 采购行项包含收货追踪 | 收货是独立事件 | 中 |

---

## 6. 映射矩阵总览

### 6.1 表级映射

| 当前表 | 目标模型 | 映射类型 | 迁移判定 |
|--------|----------|----------|----------|
| `inventory` | `inventory_fact` + `stock_position` | **重构** | 余额→事实+计算层 |
| `store_inventory` | `inventory_fact` + `stock_position` | **重构** | 合并到统一模型 |
| `material_archives` | `canonical_item` | **保留+扩展** | Identity 不变，增加 Capability |
| `foods` | `sellable_item` | **保留** | 销售域标识不变 |
| `product` | `canonical_item` | **废弃** | 值=material_id，列名保留兼容 |
| `dish_recipe` | `recipe` | **保留** | 跨域桥接不变 |
| `purchase_order_items` | `purchase_order_item` | **保留+拆分** | 收货追踪移出 |
| `order_items` | `sales_order_item` | **保留** | 通过 Recipe 桥接库存 |
| `inventory_transactions` | `stock_ledger` | **重构** | 仅覆盖仓库→覆盖全 Location |
| `inventory_unit` | `uom` + `uom_conversion` | **重构** | 单位模型标准化 |

### 6.2 字段级映射统计

| 迁移判定 | 数量 | 说明 |
|----------|------|------|
| **保留** | ~35 | 语义一致，直接迁移 |
| **重构** | ~20 | 语义需要重新表达 |
| **废弃** | ~8 | 目标模型中不需要 |
| **新增** | ~15 | 当前系统缺失，目标模型需要 |

---

## 7. 关键原则重申

| # | 原则 | 说明 |
|---|------|------|
| 1 | **Current DB Model ≠ Target Inventory Model** | 当前表结构是历史产物，不是目标架构 |
| 2 | **不得为了迁就现有表结构定义最终业务模型** | 目标模型由业务语义驱动 |
| 3 | **material_id 是当前系统的事实 Canonical Identity** | 所有活跃表都以它为外键 |
| 4 | **food_id 是销售域的独立 Identity** | 需要通过 item_identity_map 关联 |
| 5 | **库存余额是 Derived，不是 Truth** | 必须从 Stock Ledger 重建 |
| 6 | **门店库存必须有流水** | 统一 Truth 体系 |
| 7 | **这是业务语义映射，不是数据库迁移计划** | 不涉及具体的 DDL/DML |

---

## 8. 下一步行动

| # | 任务 | 优先级 | 依赖 |
|---|------|--------|------|
| 1 | Phase 22: 候选库存模型比较 | P0 | 本分析 |
| 2 | Phase 23: 反例验证 | P0 | Phase 22 |
| 3 | 建立 `item_identity_map` 映射表设计 | P1 | Phase 21 |
| 4 | 统一 `stock_ledger` 覆盖全 Location | P1 | Phase 21 |

---

**状态**：ANALYSIS, NOT CONFIRMED
**日期**：2026-09-10
**阶段**：Phase 21 - Current-to-Target Inventory Semantic Mapping
