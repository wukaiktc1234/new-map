# Stockable Object 分析 (Phase 2)

> **核心问题**：Inventory 真正管理的是哪一个概念？
> **状态**：ANALYSIS, NOT CONFIRMED

---

## 1. 问题域

本分析必须回答：
1. Inventory 真正管理的是哪一个概念？
2. Material、Product、Food、Canonical Item、Stockable Item、Inventory Profile、Stockable Role 之间的关系是什么？
3. 通过业务案例验证候选概念的正确性

**关键约束**：
- Current DB Model ≠ Target Inventory Model
- 不要因为当前实现就认定未来模型
- 必须从业务语义角度分析

---

## 2. 候选概念定义

| 候选概念 | 定义 | 典型代表 | 系统中的位置 |
|----------|------|----------|-------------|
| **Material** | 可采购、可存储的物理实体，以 material_id 为唯一标识 | 土豆、鸡翅、可乐、餐盒 | `material_archives` |
| **Product** | 可销售的商品（含成品和原料），历史概念 | 可乐、薯片、矿泉水 | `product`（已废弃） |
| **Food** | 门店销售的菜品/食品，可以是成品或预包装品 | 宫保鸡丁、可乐、薯片 | `foods` |
| **Canonical Item** | 跨所有业务域的唯一标识，"这个东西是什么"的最终答案 | "可口可乐330ml"这一个东西 | 无（理想模型） |
| **Stockable Item** | 具备库存能力的物料实例，Material 在库存上下文中的角色 | 仓库A的可乐200罐 | `inventory` |
| **Inventory Profile** | 描述一个 Item 如何被库存管理的配置 | 安全库存、保质期、存储温度 | 分散在多表 |
| **Stockable Role** | Material 在库存管理上下文中的角色配置 | 可乐作为库存项的能力声明 | 无独立建模 |

---

## 3. 业务案例深度分析

### 3.1 鸡翅（Chicken Wing）

**物理身份**：生鲜禽肉部位，需冷藏/冷冻保存。

**业务角色矩阵**：

| 角色 | 业务场景 | 系统表 | 关键数据 |
|------|----------|--------|----------|
| PURCHASABLE | 从供应商采购 | `purchase_order_items` | material_id, unit_price=12.00/斤 |
| STOCKABLE | 仓库冷藏存储 | `inventory` | material_id, warehouse_id, current_stock=50kg |
| STORE-STOCKABLE | 门店冷藏存储 | `store_inventory` | material_id, store_id, current_stock=10kg |
| INGREDIENT | 宫保鸡丁、可乐鸡翅的原料 | `dish_recipe` | ingredient_id=M002, quantity=0.3kg |
| SELLABLE | 通常不直接销售 | `foods` | 通常不出现 |

**关键洞察**：
- 鸡翅是**生鲜原料**，采购后入库，作为菜品原料消耗
- 不直接面向顾客销售（除非是熟食店场景）
- 保质期短（冷藏3-5天，冷冻3-6个月），需要批次管理
- 库存管理的核心是**原料**维度，不是成品维度

**鸡翅的库存管理本质**：
```
Material (鸡翅) → STOCKABLE Role → Inventory Instance (仓库/门店)
                                    ↓
                        当菜品原料消耗时扣减
```

### 3.2 可乐（Cola）

**物理身份**：碳酸饮料液体，装在铝罐/塑料瓶中，有保质期（通常12个月）。

**业务角色矩阵**：

| 角色 | 业务场景 | 系统表 | 关键数据 |
|------|----------|--------|----------|
| SELLABLE | 门店POS销售 | `foods` | food_id, price=3.00 |
| PURCHASABLE | 从供应商采购 | `purchase_order_items` | material_id, unit_price=2.50 |
| STOCKABLE | 仓库存储 | `inventory` | material_id, warehouse_id, current_stock=200 |
| STORE-STOCKABLE | 门店库存 | `store_inventory` | material_id, store_id, current_stock=20 |
| INGREDIENT | 可乐鸡翅的原料 | `dish_recipe` | ingredient_id=M001, quantity=0.2L |

**关键洞察**：可乐是**预包装标准品**。它具备：
- 独立的 SKU（可口可乐330ml）
- 独立的包装（铝罐/塑料瓶）
- 标准的规格（330ml/罐）
- 可直接销售（不需要加工）
- 可长期存储（有保质期）
- 可作为原料（可乐鸡翅）

**可乐为什么能库存？**
1. **物理形态**：预包装、有标准规格、可堆叠
2. **存储条件**：常温保存，无需特殊设备
3. **保质期**：12个月，足够长的存储窗口
4. **需求模式**：相对稳定，可预测
5. **采购模式**：批量采购，需要库存缓冲

**可乐的库存管理本质**：
```
Material (可乐) → SELLABLE Role  → foods (门店销售)
             → STOCKABLE Role → Inventory Instance (仓库/门店)
             → INGREDIENT Role → dish_recipe (可乐鸡翅)
```

### 3.3 大米（Rice）

**物理身份**：谷物类食材，袋装/散装。

**业务角色矩阵**：

| 角色 | 业务场景 | 系统表 | 关键数据 |
|------|----------|--------|----------|
| PURCHASABLE | 从供应商采购 | `purchase_order_items` | material_id, unit_price=3.00/斤 |
| STOCKABLE | 仓库存储 | `inventory` | material_id, warehouse_id, current_stock=500kg |
| STORE-STOCKABLE | 门店库存 | `store_inventory` | material_id, store_id, current_stock=50kg |
| INGREDIENT | 米饭、粥等菜品的原料 | `dish_recipe` | ingredient_id=M003, quantity=0.2kg |
| SELLABLE | 通常不直接销售 | `foods` | 通常不出现 |

**关键洞察**：
- 大米是**基础食材**，采购后入库，作为菜品原料消耗
- 保质期较长（6-12个月），存储条件简单（干燥常温）
- 库存管理的核心是**原料**维度

**大米的库存管理本质**：
```
Material (大米) → STOCKABLE Role → Inventory Instance (仓库/门店)
                                    ↓
                        当菜品原料消耗时扣减
```

### 3.4 酱油（Soy Sauce）

**物理身份**：调味品，瓶装。

**业务角色矩阵**：

| 角色 | 业务场景 | 系统表 | 关键数据 |
|------|----------|--------|----------|
| PURCHASABLE | 从供应商采购 | `purchase_order_items` | material_id, unit_price=8.00/瓶 |
| STOCKABLE | 仓库存储 | `inventory` | material_id, warehouse_id, current_stock=100瓶 |
| STORE-STOCKABLE | 门店库存 | `store_inventory` | material_id, store_id, current_stock=20瓶 |
| INGREDIENT | 各类菜品的调味原料 | `dish_recipe` | ingredient_id=M004, quantity=0.02L |
| SELLABLE | 通常不直接销售 | `foods` | 通常不出现 |

**关键洞察**：
- 酱油是**调味品**，采购后入库，作为菜品原料消耗
- 保质期较长（12-24个月），存储条件简单
- 库存管理的核心是**原料**维度

**酱油的库存管理本质**：
```
Material (酱油) → STOCKABLE Role → Inventory Instance (仓库/门店)
                                    ↓
                        当菜品原料消耗时扣减
```

### 3.5 矿泉水（Mineral Water）

**物理身份**：天然饮用水，塑料瓶包装。

**业务角色矩阵**：

| 角色 | 业务场景 | 系统表 | 关键数据 |
|------|----------|--------|----------|
| SELLABLE | 门店销售 | `foods` | food_id, price=2.00 |
| PURCHASABLE | 供应商采购 | `purchase_order_items` | material_id, unit_price=1.50 |
| STOCKABLE | 仓库存储 | `inventory` | material_id, warehouse_id, current_stock=200 |
| STORE-STOCKABLE | 门店库存 | `store_inventory` | material_id, store_id, current_stock=30 |
| INGREDIENT | 几乎不作为原料 | `dish_recipe` | 极少使用 |

**关键洞察**：
- 矿泉水是**预包装标准品**，与可乐类似
- 可直接销售，可长期存储
- 几乎不作为菜品原料

**矿泉水的库存管理本质**：
```
Material (矿泉水) → SELLABLE Role  → foods (门店销售)
                  → STOCKABLE Role → Inventory Instance (仓库/门店)
```

### 3.6 预包装薯片（Packaged Chips）

**物理身份**：休闲食品，袋装。

**业务角色矩阵**：

| 角色 | 业务场景 | 系统表 | 关键数据 |
|------|----------|--------|----------|
| SELLABLE | 门店销售 | `foods` | food_id, price=6.00 |
| PURCHASABLE | 供应商采购 | `purchase_order_items` | material_id, unit_price=4.50 |
| STOCKABLE | 仓库存储 | `inventory` | material_id, warehouse_id, current_stock=80 |
| STORE-STOCKABLE | 门店库存 | `store_inventory` | material_id, store_id, current_stock=10 |
| INGREDIENT | 通常不作为原料 | `dish_recipe` | 通常不出现 |

**关键洞察**：
- 薯片是**预包装标准品**，与可乐、矿泉水类似
- 可直接销售，可长期存储
- 通常不作为菜品原料

**薯片的库存管理本质**：
```
Material (薯片) → SELLABLE Role  → foods (门店销售)
                → STOCKABLE Role → Inventory Instance (仓库/门店)
```

### 3.7 餐盒（Takeout Box）

**物理身份**：包装耗材，可采购、可库存、可消耗。

**业务角色矩阵**：

| 角色 | 业务场景 | 系统表 | 关键数据 |
|------|----------|--------|----------|
| PURCHASABLE | 从供应商采购 | `purchase_order_items` | material_id, unit_price=0.50/个 |
| STOCKABLE | 仓库存储 | `inventory` | material_id, warehouse_id, current_stock=1000个 |
| STORE-STOCKABLE | 门店库存 | `store_inventory` | material_id, store_id, current_stock=200个 |
| CONSUMABLE | 外卖打包时消耗 | `material_consumption` | material_id, quantity=1 |
| SELLABLE | 不直接销售 | `foods` | 不出现 |

**关键洞察**：
- 餐盒是**耗材**，采购后入库，外卖打包时消耗
- 不直接销售给顾客
- 保质期无限制（塑料/纸质制品）
- 库存管理的核心是**消耗品**维度

**餐盒的库存管理本质**：
```
Material (餐盒) → STOCKABLE Role → Inventory Instance (仓库/门店)
                                    ↓
                        当外卖打包时消耗扣减
```

### 3.8 宫保鸡丁（Kung Pao Chicken）

**物理身份**：由鸡胸肉、花生、辣椒、花椒等原料现场烹饪的成品菜品。

**业务角色矩阵**：

| 角色 | 业务场景 | 系统表 | 关键数据 |
|------|----------|--------|----------|
| SELLABLE | 门店销售 | `foods` | food_id, price=28.00 |
| PURCHASABLE | 几乎不采购成品 | `purchase_order_items` | 通常不出现 |
| STOCKABLE | 通常不存储成品 | `inventory` | 通常不出现 |
| INGREDIENT | 几乎不作为其他菜品原料 | `dish_recipe` | 通常不出现 |

**关键洞察**：
- 宫保鸡丁是**现做菜品**，不是库存管理的对象
- 它的原料（鸡胸肉、花生、辣椒）才是库存管理的对象
- Recipe/BOM 是连接成品和原料库存的桥梁

**宫保鸡丁的库存管理本质**：
```
Material (鸡胸肉) → STOCKABLE Role → Inventory Instance
Material (花生)   → STOCKABLE Role → Inventory Instance
Material (辣椒)   → STOCKABLE Role → Inventory Instance
                        ↓
              当宫保鸡丁被制作时，原料扣减
```

### 3.9 可乐鸡翅（Cola Chicken Wings）

**物理身份**：由鸡翅和可乐现场烹饪的成品菜品。

**业务角色矩阵**：

| 角色 | 业务场景 | 系统表 | 关键数据 |
|------|----------|--------|----------|
| SELLABLE | 门店销售 | `foods` | food_id, price=32.00 |
| PURCHASABLE | 几乎不采购成品 | `purchase_order_items` | 通常不出现 |
| STOCKABLE | 通常不存储成品 | `inventory` | 通常不出现 |
| INGREDIENT | 几乎不作为其他菜品原料 | `dish_recipe` | 通常不出现 |

**关键洞察**：
- 可乐鸡翅是**现做菜品**，不是库存管理的对象
- 它的原料（鸡翅、可乐）才是库存管理的对象
- 可乐鸡翅这个菜品本身不入库，但它的原料都入库

**可乐鸡翅的库存管理本质**：
```
Material (鸡翅)  → STOCKABLE Role → Inventory Instance
Material (可乐)  → STOCKABLE Role → Inventory Instance
                        ↓
              当可乐鸡翅被制作时，原料扣减
```

---

## 4. 案例汇总分析

### 4.1 角色激活矩阵

| 物品 | SELLABLE | PURCHASABLE | STOCKABLE | INGREDIENT | CONSUMABLE |
|------|----------|-------------|-----------|------------|------------|
| 鸡翅 | ❌ | ✅ | ✅ | ✅ | ❌ |
| 可乐 | ✅ | ✅ | ✅ | ✅ | ❌ |
| 大米 | ❌ | ✅ | ✅ | ✅ | ❌ |
| 酱油 | ❌ | ✅ | ✅ | ✅ | ❌ |
| 矿泉水 | ✅ | ✅ | ✅ | ❌ | ❌ |
| 薯片 | ✅ | ✅ | ✅ | ❌ | ❌ |
| 餐盒 | ❌ | ✅ | ✅ | ❌ | ✅ |
| 宫保鸡丁 | ✅ | ❌ | ❌ | ❌ | ❌ |
| 可乐鸡翅 | ✅ | ❌ | ❌ | ❌ | ❌ |

### 4.2 关键发现

**发现 1：Inventory 管理的对象是 Material，不是 Food**

| 证据 | 说明 |
|------|------|
| 宫保鸡丁不入库 | 它是成品菜品，不采购、不库存 |
| 宫保鸡丁的原料入库 | 鸡胸肉、花生、辣椒都是 Material |
| 可乐鸡翅不入库 | 它是成品菜品，不采购、不库存 |
| 可乐鸡翅的原料入库 | 鸡翅、可乐都是 Material |

**结论**：Inventory 管理的是 **Material**（原料/物料），不是 **Food**（成品菜品）。

**发现 2：Stockable Item 是 Material 在特定 Location 的实例**

| 场景 | Material | Location | Stockable Item |
|------|----------|----------|----------------|
| 仓库可乐 | 可口可乐330ml | 仓库A | 仓库A有200罐可乐 |
| 门店可乐 | 可口可乐330ml | 门店B | 门店B有20罐可乐 |
| 仓库鸡翅 | 鸡翅 | 仓库A | 仓库A有50kg鸡翅 |
| 门店鸡翅 | 鸡翅 | 门店B | 门店B有10kg鸡翅 |

**结论**：Stockable Item = Material + Location，是库存管理的实际粒度。

**发现 3：Food 可以同时是 Material**

| 物品 | 作为 Food | 作为 Material | 说明 |
|------|-----------|---------------|------|
| 可乐 | foods 表有记录 | material_archives 有记录 | 预包装品同时是 Food 和 Material |
| 矿泉水 | foods 表有记录 | material_archives 有记录 | 预包装品同时是 Food 和 Material |
| 薯片 | foods 表有记录 | material_archives 有记录 | 预包装品同时是 Food 和 Material |
| 鸡翅 | foods 表通常无记录 | material_archives 有记录 | 生鲜原料只是 Material |
| 大米 | foods 表通常无记录 | material_archives 有记录 | 基础食材只是 Material |

**结论**：预包装品（可乐、矿泉水、薯片）同时具备 Food 和 Material 双重身份。

**发现 4：Cooked Dishes 不是 Inventory 的管理对象**

| 物品 | 作为 Food | 作为 Material | 作为 Inventory |
|------|-----------|---------------|----------------|
| 宫保鸡丁 | foods 表有记录 | material_archives 无记录 | 不入库 |
| 可乐鸡翅 | foods 表有记录 | material_archives 无记录 | 不入库 |

**结论**：Cooked Dishes（现做菜品）只在 foods 表中存在，不参与库存管理。

---

## 5. 候选概念深度比较

### 5.1 Material vs Product

| 维度 | Material | Product |
|------|----------|---------|
| 定义 | 可采购、可存储的物理实体 | 可销售的商品（历史概念） |
| 当前系统位置 | `material_archives` | `product`（已废弃） |
| 活跃度 | 所有采购/库存表都使用 | 已被历史淘汰 |
| 语义 | 强调"可存储、可采购" | 强调"可销售" |
| 覆盖范围 | 包含原料、耗材、预包装品 | 只包含可销售品 |

**结论**：Material 是当前系统的 Canonical Identity，Product 已被历史淘汰。

### 5.2 Material vs Food

| 维度 | Material | Food |
|------|----------|------|
| 定义 | 可采购、可存储的物理实体 | 门店销售的菜品/食品 |
| 当前系统位置 | `material_archives` | `foods` |
| 覆盖范围 | 原料、耗材、预包装品 | 成品菜品、预包装品 |
| 库存管理 | ✅ 是 | ❌ 否（成品菜品不入库） |
| 采购管理 | ✅ 是 | ❌ 否（成品菜品不采购） |

**结论**：Material 是 Inventory 的管理对象，Food 不是（成品菜品不入库）。

### 5.3 Material vs Canonical Item

| 维度 | Material | Canonical Item |
|------|----------|----------------|
| 定义 | 可采购、可存储的物理实体 | 跨所有业务域的唯一标识 |
| 当前系统位置 | `material_archives` | 无（理想模型） |
| 实现状态 | ✅ 已实现 | ❌ 未实现 |
| 语义 | 强调"可存储、可采购" | 强调"唯一身份" |

**结论**：Material 是当前系统的 Canonical Identity，Canonical Item 是目标模型的理想形态。

### 5.4 Material vs Stockable Item

| 维度 | Material | Stockable Item |
|------|----------|----------------|
| 定义 | 可采购、可存储的物理实体 | 在特定位置持有数量的库存实例 |
| 当前系统位置 | `material_archives` | `inventory` |
| 粒度 | 一个 Material | 一个 (Material, Location) 组合 |
| 实例化 | 一次创建 | 多次实例化（不同位置） |

**结论**：Material 是 Stockable Item 的 Canonical Identity，Stockable Item 是 Material 在特定 Location 的实例。

### 5.5 Material vs Inventory Profile

| 维度 | Material | Inventory Profile |
|------|----------|-------------------|
| 定义 | 可采购、可存储的物理实体 | 描述一个 Item 如何被库存管理的配置 |
| 当前系统位置 | `material_archives` | 分散在多表 |
| 关系 | 主体 | 配置 |
| 包含内容 | 名称、编码、规格等 | 安全库存、保质期、存储条件等 |

**结论**：Inventory Profile 是 Material 的库存配置，可以作为 Stockable Role 的属性存在。

### 5.6 Material vs Stockable Role

| 维度 | Material | Stockable Role |
|------|----------|----------------|
| 定义 | 可采购、可存储的物理实体 | Material 在库存管理上下文中的角色 |
| 当前系统位置 | `material_archives` | 无独立建模 |
| 关系 | 主体 | 角色 |
| 可选性 | 必须存在 | 可选（不是所有 Material 都需要库存） |

**结论**：Stockable Role 是 Material 的一个 Business Role，表示"可以被存储"。

---

## 6. 推荐模型：三层架构

### 6.1 模型结构

```
Layer 1: Canonical Identity (标识层)
  └── material_archives (material_id)
        "这是什么？" → 一袋5kg装的东北大米

Layer 2: Stockable Role (能力层)
  └── stockable_config (material_id, role_type)
        "它能被存储吗？" → 能，需干燥环境，保质期180天

Layer 3: Inventory Instance (实例层)
  ├── inventory (material_id, warehouse_id)  → 仓库中的具体库存
  └── store_inventory (material_id, store_id) → 门店中的具体库存
        "它现在在哪里？有多少？" → 仓库A有200袋，门店B有10袋
```

### 6.2 与业务流程的映射

```
                    material_archives (Canonical Identity)
                         │
    ┌────────────────────┼────────────────────┐
    ▼                    ▼                    ▼
 [采购场景]          [库存场景]           [销售场景]
 PURCHASABLE        STOCKABLE            SELLABLE
    │                    │                    │
 purchase_order    inventory/            foods/
 _items             store_inventory      sales_order
    │                    │                    │
    └──────── material_id ────────────────────┘
              (Canonical Identity)
```

### 6.3 可乐的完整角色图

```
Canonical Identity: 可口可乐330ml (M001)
  │
  ├── SELLABLE Role
  │     ├── foods: food_id=F001, price=3.00
  │     └── sales_order_items: food_id=F001
  │
  ├── PURCHASABLE Role
  │     ├── material_archives: material_id=M001
  │     └── purchase_order_items: material_id=M001, unit_price=2.50
  │
  ├── STOCKABLE Role
  │     ├── inventory: material_id=M001, warehouse_id=W01, current_stock=200
  │     └── store_inventory: material_id=M001, store_id=S01, current_stock=20
  │
  └── INGREDIENT Role
        └── dish_recipe: ingredient_id=M001, quantity=0.2L (可乐鸡翅)
```

---

## 7. Inventory 的语义重新定义

### 7.1 当前定义

**Inventory = Material 在特定 Location 的数量实例**

```
inventory(material_id, warehouse_id) → {
    current_stock,        -- 可用数量
    safety_stock,         -- 安全库存
    cost_price,           -- 成本价
    stock_value,          -- 库存价值
    inventory_type        -- 库存类型（不明确）
}
```

### 7.2 目标定义

**Inventory Instance = Stockable Role 在特定 Location 的运行时状态**

```
inventory_instance {
    material_id:          -- 物料标识（Canonical Identity）
    location_id:          -- 位置标识（仓库/门店）
    location_type:        -- 位置类型（WAREHOUSE/STORE）
    current_stock:        -- 当前数量
    locked_quantity:      -- 锁定数量
    unit_cost:            -- 单位成本
    total_cost:           -- 总成本
    batch_no:             -- 批次号
    production_date:      -- 生产日期
    expiry_date:          -- 过期日期
    safety_stock:         -- 安全库存
    max_stock:            -- 最大库存
}
```

### 7.3 库存流水的语义

`inventory_transactions.transaction_type` 编码的业务语义：

| type | 操作 | 来源 | 目标 | material_id 变化 |
|------|------|------|------|-----------------|
| 1 | 采购入库 | 采购订单 | 仓库库存 | + |
| 2 | 销售出库 | 仓库库存/门店库存 | 门店销售 | - |
| 3 | 调拨出 | 源仓库/门店 | - | - |
| 4 | 调拨入 | - | 目标仓库/门店 | + |
| 5 | 盘点盈 | - | 库存 | + |
| 6 | 盘点亏 | 库存 | - | - |
| 7 | 报损 | 库存 | 报损记录 | - |
| 8 | 退货 | 客户退回 | 库存 | + |

**关键洞察**：流水表同时记录了 `inventory_id` 和 `material_id`。`inventory_id` 是外键关联到具体库存实例，`material_id` 是冗余字段便于跨仓库聚合查询。这说明库存流水的主语是 **"material 在某个 location 的数量变动"**。

---

## 8. 三维度分离

### 8.1 CURRENT REALITY（当前现实）

| 维度 | 现状 | 问题 |
|------|------|------|
| 库存粒度 | `material_id` | 遗留字段 `product_id` 仍存在 |
| 位置模型 | `warehouse_id` + `store_id` | 双维度库存，无统一位置模型 |
| 成本模型 | `cost_price` / `unit_cost` | 仓库和门店成本独立维护 |
| 配置模型 | 分散在多表 | 安全库存、保质期等配置不统一 |

### 8.2 TARGET BUSINESS SEMANTICS（目标业务语义）

| 维度 | 目标 | 理由 |
|------|------|------|
| 库存粒度 | **Stockable Role Instance** | 库存管理的是 Material 的库存角色实例 |
| 位置模型 | **统一 Location** | warehouse 和 store 统一为 Location + LocationType |
| 成本模型 | **统一成本核算** | 入库成本 → 库存成本 → 出库成本，统一核算规则 |
| 配置模型 | **Stockable Role Config** | 安全库存、保质期、存储条件统一管理 |

### 8.3 TARGET TECHNICAL MODEL（目标技术模型）

```sql
-- 统一位置模型
CREATE TABLE location (
    location_id     BIGINT PRIMARY KEY,
    location_type   VARCHAR(20) NOT NULL,  -- 'WAREHOUSE' | 'STORE'
    location_name   VARCHAR(100) NOT NULL,
    ...
);

-- 库存角色配置
CREATE TABLE stockable_config (
    config_id       BIGINT PRIMARY KEY,
    material_id     BIGINT NOT NULL,       -- 关联 material_archives
    location_id     BIGINT,                -- 可选：特定位置配置
    safety_stock    INTEGER DEFAULT 0,
    max_stock       INTEGER DEFAULT 0,
    shelf_life_days INTEGER,               -- 保质期（天）
    storage_temp    VARCHAR(50),           -- 存储温度
    ...
);

-- 统一库存实例
CREATE TABLE inventory_instance (
    instance_id     BIGINT PRIMARY KEY,
    material_id     BIGINT NOT NULL,
    location_id     BIGINT NOT NULL,
    current_stock   INTEGER NOT NULL DEFAULT 0,
    locked_quantity INTEGER DEFAULT 0,
    unit_cost       DECIMAL(12,2) DEFAULT 0,
    total_cost      DECIMAL(14,4) DEFAULT 0,
    batch_no        VARCHAR(50),
    production_date DATE,
    expiry_date     DATE,
    UNIQUE (material_id, location_id, batch_no)
);
```

---

## 9. 遗留问题与技术债

### 9.1 命名不一致

| 问题 | 位置 | 建议 |
|------|------|------|
| `material_archives` 叫"商品档案"而非"物料档案" | 表注释 | 统一为"物料主数据"或接受"商品档案"作为采购视角的名称 |
| `inventory.product_id` 仍使用 product_id | V1.0.0.100 遗留 | 应迁移为 material_id |
| `Inventory.java` 中 `getProductId()` 返回 `materialId` | 兼容性方法 | 应标记 @Deprecated |
| `inventory.product_name` 是遗留字段 | V1.0.0.100 遗留 | 应迁移为 material_name |

### 9.2 双维度库存的同步问题

- `inventory` 和 `store_inventory` 是独立表，无事务一致性保证
- 门店收货确认时需要同时更新两个表
- 缺少库存同步机制（如：门店申请 → 仓库调拨 → 双表联动）

### 9.3 成本核算的割裂

- `inventory.cost_price` 与 `store_inventory.unit_cost` 独立维护
- 采购入库单有 `unit_price`，入库后更新 `inventory.cost_price`，但门店调拨时成本如何传递未定义
- 系统中未见明确的库存成本核算规则（如加权平均法、先进先出法）

---

## 10. 推荐改进方向

### 10.1 短期（保持现有结构）

1. **统一 material_id 口径**：确保所有新增表和字段使用 `material_id`，不使用 `product_id`
2. **清理 Inventory.java 的兼容性方法**：标记 `getProductId()` 为 @Deprecated
3. **修复 inventory.product_id**：在后续迁移中添加 `material_id` 列并回填数据

### 10.2 中期（引入 Stockable Role 配置）

1. **创建 `stockable_config` 表**：将分散在 inventory / store_inventory 的预警阈值、存储条件统一管理
2. **标准化库存预警**：基于 stockable_config 统一预警规则

### 10.3 长期（统一库存模型）

1. **引入统一 Location 模型**：warehouse_id 和 store_id 统一为 location_id + location_type
2. **合并 inventory 和 store_inventory**：统一为 inventory_instance 表
3. **引入 Material 的 Canonical Identity**：建立跨域的统一身份标识

---

## 11. 总结

| 问题 | 结论 |
|------|------|
| Inventory 管理的是什么？ | **Material**（物料/商品），以 material_id 为 canonical identity |
| 可乐为什么能库存？ | 预包装标准品，有独立 SKU、标准规格、保质期长、需求稳定 |
| 宫保鸡丁为什么不能库存？ | 现做现卖的成品，保质期极短，不采购成品，只采购原料 |
| 预包装商品处于什么位置？ | **Material + SELLABLE Role**，同时承担采购和销售双重身份 |
| Stockable Item 是什么？ | Material 在特定 Location 的库存实例 |
| 库存的核心抽象是什么？ | **Material + Location → Inventory Instance**（物料 + 位置 → 库存实例） |
| 仓库库存和门店库存的关系？ | 同一 Material 在不同 Location 类型的 Stockable Role 实例化 |

---

**状态**：ANALYSIS, NOT CONFIRMED
**日期**：2026-09-10
**阶段**：Phase 2 - Stockable Object Analysis
