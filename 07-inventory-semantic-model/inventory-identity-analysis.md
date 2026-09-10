# Inventory Identity Analysis (Phase 18 + Phase 20)

> **核心问题**：Inventory Identity 到底是什么？各业务域如何关联到同一条库存记录？
> **状态**：ANALYSIS, NOT CONFIRMED

---

## 1. 问题域

本分析必须回答：
1. 食材、饮品、预包装品、原料、Canonical Item 各自在销售/采购/库存/配方/直接库存中的角色矩阵
2. `itemId`, `materialId`, `productId`, `foodId`, `inventoryId`, `stockItemId` 之间的关系
3. Identity Loss、Identity Re-generation、Wrong Identity、Duplicate Identity、Implicit Identity 的识别
4. Inventory 是否应该直接保存 Canonical Item Identity？还是通过 Material / Product / Stockable Profile 间接关联？

**关键约束**：
- Current DB Model ≠ Target Inventory Model
- 不要因为当前实现就认定未来模型
- 必须从业务语义角度分析

---

## 2. 角色矩阵（Role Matrix）

### 2.1 概念定义

| 概念 | 语义 | 对应业务域 | 当前系统位置 |
|------|------|-----------|-------------|
| **Food（菜品）** | 门店销售的现做菜品或预包装食品，面向终端顾客 | 销售域 | `foods` |
| **Beverage（饮品）** | 液态饮品，预包装，可直接销售，可作为菜品原料 | 销售+原料域 | `foods`（饮料类）+ `material_archives` |
| **Packaged Product（预包装品）** | 预包装标准品，有独立 SKU，可直接销售 | 销售域 | `foods` + `material_archives` |
| **Material（物料）** | 可采购、可存储的物理实体，以 `material_id` 为唯一标识 | 采购+库存域 | `material_archives` |
| **Canonical Item（规范标识）** | 跨所有业务域的唯一标识，"这个东西是什么"的最终答案 | 全域 | 无（理想模型） |

### 2.2 角色激活矩阵

| 概念 | Sellable | Purchasable | Stockable | Recipe Component | 直接 Inventory |
|------|----------|-------------|-----------|------------------|----------------|
| **Food** | ✅ 核心角色 | ❌ 通常不采购成品 | ❌ 现做菜品不入库 | ⚠️ 可作为其他菜品原料（极少见） | ❌ 成品不入库 |
| **Beverage** | ✅ 核心角色 | ✅ 供应商采购 | ✅ 仓库+门店库存 | ✅ 可作为菜品原料（可乐鸡翅） | ✅ material_id 管理 |
| **Packaged Product** | ✅ 核心角色 | ✅ 供应商采购 | ✅ 仓库+门店库存 | ❌ 通常不作为原料 | ✅ material_id 管理 |
| **Material** | ⚠️ 部分可售（预包装品） | ✅ 核心角色 | ✅ 核心角色 | ✅ 核心角色（原料/配方成分） | ✅ 核心角色 |
| **Canonical Item** | ⚠️ 取决于具体 Item 的角色 | ⚠️ 取决于具体 Item 的角色 | ⚠️ 取决于具体 Item 的角色 | ⚠️ 取决于具体 Item 的角色 | ⚠️ 取决于具体 Item 的角色 |

### 2.3 每一个结果的解释

#### Food（菜品）

| 角色 | 判定 | 理由 |
|------|------|------|
| **Sellable** | ✅ | Food 的核心定义就是"门店销售的菜品"，`foods.food_id` 是销售域的主标识 |
| **Purchasable** | ❌ | 现做菜品（宫保鸡丁、可乐鸡翅）不会从供应商采购成品；预包装食品另算（那是 Material） |
| **Stockable** | ❌ | 现做菜品保质期极短（小时级），不经过库存管理流程 |
| **Recipe Component** | ⚠️ | 极少数场景下菜品可作为其他菜品的原料（如：米饭作为蛋炒饭的原料），但 `dish_recipe.ingredient_id` 指向 `material_archives`，不是 `foods` |
| **Direct Inventory** | ❌ | 成品菜品不入库，只在制作时扣减原料库存 |

**关键洞察**：Food 作为独立概念，其库存管理是通过**原料的 Material Identity** 间接实现的。Food 本身不直接参与库存管理。

#### Beverage（饮品）

| 角色 | 判定 | 理由 |
|------|------|------|
| **Sellable** | ✅ | 可乐、矿泉水等饮品直接面向顾客销售，`foods.food_id` 是销售标识 |
| **Purchasable** | ✅ | 从供应商批量采购，`purchase_order_items.material_id` 是采购标识 |
| **Stockable** | ✅ | 仓库和门店都有库存，`inventory.material_id` / `store_inventory.material_id` |
| **Recipe Component** | ✅ | 可乐可作为可乐鸡翅的原料，`dish_recipe.ingredient_id = material_id` |
| **Direct Inventory** | ✅ | 以 `material_id` 为核心管理库存，`inventory` 表和 `store_inventory` 表都记录 |

**关键洞察**：饮品是**双重身份**的典型——既是 Sellable（foods），又是 Material（material_archives）。同一个物理实体在两个域中各有一个 Identity。

#### Packaged Product（预包装品）

| 角色 | 判定 | 理由 |
|------|------|------|
| **Sellable** | ✅ | 预包装薯片直接面向顾客销售 |
| **Purchasable** | ✅ | 从供应商批量采购 |
| **Stockable** | ✅ | 仓库和门店都有库存 |
| **Recipe Component** | ❌ | 预包装品通常不作为菜品原料（直接销售，不加工） |
| **Direct Inventory** | ✅ | 以 `material_id` 管理库存 |

**关键洞察**：预包装品与饮品类似，也是双重身份，但通常不参与 Recipe。

#### Material（物料）

| 角色 | 判定 | 理由 |
|------|------|------|
| **Sellable** | ⚠️ | 取决于具体物料：预包装品可售（可乐、薯片），生鲜原料通常不直接销售（鸡翅、大米） |
| **Purchasable** | ✅ | Material 的核心定义就是"可采购的物理实体" |
| **Stockable** | ✅ | Material 的核心定义就是"可存储的物理实体" |
| **Recipe Component** | ✅ | Material 是配方的原料，`dish_recipe.ingredient_id = material_id` |
| **Direct Inventory** | ✅ | `material_id` 是库存管理的核心标识，所有库存表都以 `material_id` 为外键 |

**关键洞察**：Material 是当前系统的 **Canonical Identity**，贯穿采购→库存→配方的全生命周期。

#### Canonical Item（规范标识）

| 角色 | 判定 | 理由 |
|------|------|------|
| **Sellable** | ⚠️ | 取决于具体 Item 在销售域是否被激活为 SELLABLE Role |
| **Purchasable** | ⚠️ | 取决于具体 Item 在采购域是否被激活为 PURCHASABLE Role |
| **Stockable** | ⚠️ | 取决于具体 Item 在库存域是否被激活为 STOCKABLE Role |
| **Recipe Component** | ⚠️ | 取决于具体 Item 在配方域是否被激活为 INGREDIENT Role |
| **Direct Inventory** | ⚠️ | 取决于具体 Item 在库存域是否被激活为 STOCKABLE Role |

**关键洞察**：Canonical Item 本身不携带任何角色，它是"这个东西是什么"的唯一答案。所有角色都是通过 Business Role 层动态赋予的。

---

## 3. 真实业务案例验证

### 3.1 可乐（Cola）

```
物理实体: 可口可乐330ml 铝罐
Canonical Identity: material_id = M001

角色激活:
  ├── SELLABLE     → foods.food_id = F001 (门店销售，price=3.00)
  ├── PURCHASABLE  → purchase_order_items.material_id = M001 (采购，unit_price=2.50)
  ├── STOCKABLE    → inventory.material_id = M001 (仓库库存 200 罐)
  │                  store_inventory.material_id = M001 (门店库存 20 罐)
  └── INGREDIENT   → dish_recipe.ingredient_id = M001 (可乐鸡翅原料，quantity=0.2L)

Identity 链路:
  F001 (food_id) ←→ M001 (material_id)    [隐式映射，无物理外键]
  M001 (material_id) → inventory           [物理外键]
  M001 (material_id) → store_inventory     [物理外键]
  M001 (material_id) → dish_recipe         [物理外键]
```

### 3.2 鸡翅（Chicken Wing）

```
物理实体: 鸡翅中段，生鲜
Canonical Identity: material_id = M002

角色激活:
  ├── SELLABLE     → 通常不直接销售（除非熟食店场景）
  ├── PURCHASABLE  → purchase_order_items.material_id = M002 (采购，unit_price=12.00/斤)
  ├── STOCKABLE    → inventory.material_id = M002 (仓库冷藏库存 50kg)
  │                  store_inventory.material_id = M002 (门店冷藏库存 10kg)
  └── INGREDIENT   → dish_recipe.ingredient_id = M002 (宫保鸡丁原料 0.3kg)
                                                   (可乐鸡翅原料 0.5kg)

Identity 链路:
  M002 (material_id) → inventory           [物理外键]
  M002 (material_id) → store_inventory     [物理外键]
  M002 (material_id) → dish_recipe         [物理外键]
```

### 3.3 大米（Rice）

```
物理实体: 东北大米，袋装
Canonical Identity: material_id = M003

角色激活:
  ├── SELLABLE     → 通常不直接销售（除非袋装米零售场景）
  ├── PURCHASABLE  → purchase_order_items.material_id = M003 (采购，unit_price=3.00/斤)
  ├── STOCKABLE    → inventory.material_id = M003 (仓库库存 500kg)
  │                  store_inventory.material_id = M003 (门店库存 50kg)
  └── INGREDIENT   → dish_recipe.ingredient_id = M003 (米饭原料 0.2kg/碗)

Identity 链路:
  M003 (material_id) → inventory           [物理外键]
  M003 (material_id) → store_inventory     [物理外键]
  M003 (material_id) → dish_recipe         [物理外键]
```

### 3.4 酱油（Soy Sauce）

```
物理实体: 生抽酱油，瓶装
Canonical Identity: material_id = M004

角色激活:
  ├── SELLABLE     → 通常不直接销售
  ├── PURCHASABLE  → purchase_order_items.material_id = M004 (采购，unit_price=8.00/瓶)
  ├── STOCKABLE    → inventory.material_id = M004 (仓库库存 100 瓶)
  │                  store_inventory.material_id = M004 (门店库存 20 瓶)
  └── INGREDIENT   → dish_recipe.ingredient_id = M004 (调味原料 0.02L/份)

Identity 链路:
  M004 (material_id) → inventory           [物理外键]
  M004 (material_id) → store_inventory     [物理外键]
  M004 (material_id) → dish_recipe         [物理外键]
```

### 3.5 矿泉水（Mineral Water）

```
物理实体: 天然矿泉水，塑料瓶
Canonical Identity: material_id = M005

角色激活:
  ├── SELLABLE     → foods.food_id = F005 (门店销售，price=2.00)
  ├── PURCHASABLE  → purchase_order_items.material_id = M005 (采购，unit_price=1.50)
  ├── STOCKABLE    → inventory.material_id = M005 (仓库库存 200 瓶)
  │                  store_inventory.material_id = M005 (门店库存 30 瓶)
  └── INGREDIENT   → 几乎不作为原料

Identity 链路:
  F005 (food_id) ←→ M005 (material_id)    [隐式映射，无物理外键]
  M005 (material_id) → inventory           [物理外键]
  M005 (material_id) → store_inventory     [物理外键]
```

### 3.6 预包装薯片（Packaged Chips）

```
物理实体: 乐事薯片，袋装
Canonical Identity: material_id = M006

角色激活:
  ├── SELLABLE     → foods.food_id = F006 (门店销售，price=6.00)
  ├── PURCHASABLE  → purchase_order_items.material_id = M006 (采购，unit_price=4.50)
  ├── STOCKABLE    → inventory.material_id = M006 (仓库库存 80 袋)
  │                  store_inventory.material_id = M006 (门店库存 10 袋)
  └── INGREDIENT   → 通常不作为原料

Identity 链路:
  F006 (food_id) ←→ M006 (material_id)    [隐式映射，无物理外键]
  M006 (material_id) → inventory           [物理外键]
  M006 (material_id) → store_inventory     [物理外键]
```

### 3.7 餐盒（Takeout Box）

```
物理实体: 一次性餐盒，塑料/纸质
Canonical Identity: material_id = M007

角色激活:
  ├── SELLABLE     → ❌ 不直接销售
  ├── PURCHASABLE  → purchase_order_items.material_id = M007 (采购，unit_price=0.50/个)
  ├── STOCKABLE    → inventory.material_id = M007 (仓库库存 1000 个)
  │                  store_inventory.material_id = M007 (门店库存 200 个)
  ├── RECIPE       → ❌ 不是菜品原料
  └── CONSUMABLE   → 外卖打包时消耗 (material_consumption)

Identity 链路:
  M007 (material_id) → inventory           [物理外键]
  M007 (material_id) → store_inventory     [物理外键]
  M007 (material_id) → material_consumption [物理外键]
```

### 3.8 宫保鸡丁（Kung Pao Chicken）

```
物理实体: 由鸡胸肉、花生、辣椒、花椒等原料现场烹饪的成品菜品
Canonical Identity: food_id = F008 (仅存在于 foods 表)

角色激活:
  ├── SELLABLE     → foods.food_id = F008 (门店销售，price=28.00)
  ├── PURCHASABLE  → ❌ 不采购成品
  ├── STOCKABLE    → ❌ 不存储成品
  └── INGREDIENT   → 极少作为其他菜品原料

原料库存管理:
  Material (鸡胸肉, M010) → STOCKABLE → inventory (仓库冷藏)
  Material (花生, M011)   → STOCKABLE → inventory (仓库常温)
  Material (辣椒, M012)   → STOCKABLE → inventory (仓库常温)
  Material (花椒, M013)   → STOCKABLE → inventory (仓库常温)

Identity 链路:
  F008 (food_id) → dish_recipe.dish_id    [物理外键]
  M010 (ingredient_id) → material_archives [物理外键]
  M010 → inventory                         [物理外键]
  M011 → inventory                         [物理外键]
  M012 → inventory                         [物理外键]
  M013 → inventory                         [物理外键]
```

### 3.9 可乐鸡翅（Cola Chicken Wings）

```
物理实体: 由鸡翅和可乐现场烹饪的成品菜品
Canonical Identity: food_id = F009 (仅存在于 foods 表)

角色激活:
  ├── SELLABLE     → foods.food_id = F009 (门店销售，price=32.00)
  ├── PURCHASABLE  → ❌ 不采购成品
  ├── STOCKABLE    → ❌ 不存储成品
  └── INGREDIENT   → 极少作为其他菜品原料

原料库存管理:
  Material (鸡翅, M002)  → STOCKABLE → inventory (仓库冷藏)
  Material (可乐, M001)  → STOCKABLE → inventory (仓库常温)

Identity 链路:
  F009 (food_id) → dish_recipe.dish_id    [物理外键]
  M001 (ingredient_id) → material_archives [物理外键]
  M002 (ingredient_id) → material_archives [物理外键]
  M001 → inventory                         [物理外键]
  M002 → inventory                         [物理外键]
```

### 3.10 案例汇总

| 物品 | Sellable | Purchasable | Stockable | Recipe Component | 直接 Inventory | Canonical Identity |
|------|----------|-------------|-----------|------------------|----------------|-------------------|
| 可乐 | ✅ | ✅ | ✅ | ✅ | ✅ | material_id=M001 + food_id=F001 |
| 鸡翅 | ❌ | ✅ | ✅ | ✅ | ✅ | material_id=M002 |
| 大米 | ❌ | ✅ | ✅ | ✅ | ✅ | material_id=M003 |
| 酱油 | ❌ | ✅ | ✅ | ✅ | ✅ | material_id=M004 |
| 矿泉水 | ✅ | ✅ | ✅ | ❌ | ✅ | material_id=M005 + food_id=F005 |
| 预包装薯片 | ✅ | ✅ | ✅ | ❌ | ✅ | material_id=M006 + food_id=F006 |
| 餐盒 | ❌ | ✅ | ✅ | ❌ | ✅ | material_id=M007 |
| 宫保鸡丁 | ✅ | ❌ | ❌ | ❌ | ❌ | food_id=F008 (原料用 material_id) |
| 可乐鸡翅 | ✅ | ❌ | ❌ | ❌ | ❌ | food_id=F009 (原料用 material_id) |

---

## 4. Identity 字段关系分析

### 4.1 六种 Identity 字段定义

| 字段 | 定义 | 当前系统位置 | 活跃度 |
|------|------|-------------|--------|
| **itemId** | 通用 Item 标识，系统中未使用 | 无 | ❌ 不存在 |
| **materialId** | 物料主数据标识，贯穿采购→库存→配方 | `material_archives.material_id` | ✅ 核心 Identity |
| **productId** | 商品标识，历史遗留 | `product.product_id` | ⚠️ 已废弃，列名保留 |
| **foodId** | 菜品/食品标识，门店销售域 | `foods.food_id` | ✅ 销售域核心 Identity |
| **inventoryId** | 库存实例标识，特定位置的库存记录 | `inventory.inventory_id` | ✅ 库存实例标识 |
| **stockItemId** | 库存项标识，系统中未使用 | 无 | ❌ 不存在 |

### 4.2 Identity 字段之间的关系图

```
┌─────────────────────────────────────────────────────────────────┐
│                    Identity 关系全景图                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐                                               │
│  │ material_id  │ ← 当前系统的 Canonical Identity               │
│  │ (M001~M999)  │                                               │
│  └──────┬───────┘                                               │
│         │                                                       │
│         ├────────────────────┬──────────────────────┐            │
│         ▼                    ▼                      ▼            │
│  ┌──────────────┐   ┌──────────────┐       ┌──────────────┐    │
│  │ inventory_id │   │  food_id     │       │  product_id  │    │
│  │ (实例标识)    │   │ (销售标识)    │       │ (遗留标识)    │    │
│  └──────────────┘   └──────────────┘       └──────────────┘    │
│         │                    │                      │            │
│         │            ┌───────┴───────┐              │            │
│         │            ▼               ▼              │            │
│         │    ┌──────────────┐ ┌──────────────┐     │            │
│         │    │ sale_order   │ │ dish_recipe  │     │            │
│         │    │ _items       │ │ (bridge)     │     │            │
│         │    │ .food_id     │ │ .dish_id     │     │            │
│         │    └──────────────┘ │ .ingredient  │     │            │
│         │                     │  _id=M001    │     │            │
│         │                     └──────────────┘     │            │
│         │                                          │            │
│  ┌──────┴──────────────────────────────┐           │            │
│  │         库存域 (Inventory)           │           │            │
│  │                                     │           │            │
│  │  inventory.material_id = M001       │           │            │
│  │  inventory.warehouse_id = W01       │           │            │
│  │  inventory.inventory_id = I001      │           │            │
│  │                                     │           │            │
│  │  store_inventory.material_id = M001 │           │            │
│  │  store_inventory.store_id = S01     │           │            │
│  └─────────────────────────────────────┘           │            │
│                                                    │            │
│  ┌─────────────────────────────────────────────────┘            │
│  │  采购域 (Procurement)                                        │
│  │                                                              │
│  │  purchase_order_items.material_id = M001                     │
│  │  purchase_order_items.product_id = M001 ← 语义冲突！          │
│  └──────────────────────────────────────────────────────────────┘
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.3 Identity 字段在各表中的使用

| 表 | ID 字段 | 外键指向 | 状态 |
|----|---------|---------|------|
| `material_archives` | `material_id` (PK) | — | ✅ Canonical Identity |
| `inventory` | `inventory_id` (PK), `material_id` (FK) | `material_archives.material_id` | ✅ 活跃 |
| `store_inventory` | `id` (PK), `material_id` (FK) | `material_archives.material_id` | ✅ 活跃 |
| `inventory_transactions` | `transaction_id` (PK), `material_id`, `inventory_id` (FK) | `material_archives.material_id`, `inventory.inventory_id` | ✅ 活跃 |
| `purchase_order_items` | `material_id` (FK), `product_id` (FK) | `material_archives.material_id`, `product.product_id` | ⚠️ 双口径 |
| `foods` | `food_id` (PK) | — | ✅ 销售域 Identity |
| `dish_recipe` | `dish_id` (FK), `ingredient_id` (FK) | `foods.food_id`, `material_archives.material_id` | ✅ 桥接域 |
| `order_items` | `food_id` (FK) | `foods.food_id` | ✅ 销售域 |
| `product` | `product_id` (PK) | — | ⚠️ 已废弃 |

---

## 5. Identity 问题识别

### 5.1 Identity Loss（身份丢失）

**定义**：当一个业务实体在跨域流转时，其原始 Identity 丢失，无法追溯。

| 问题 | 位置 | 描述 | 严重度 |
|------|------|------|--------|
| **food_id → material_id 无物理映射** | `foods` ↔ `material_archives` | 预包装品（可乐）同时存在于 foods 和 material_archives，但两表之间**无外键、无映射表**。food_id 和 material_id 是两个独立 Identity，系统无法从 food_id 自动找到 material_id | 🔴 P0 |
| **dish_recipe 用 ingredient_id 指向 material_id** | `dish_recipe.ingredient_id` | 配方表用 `ingredient_id` 字段名，但实际指向 `material_archives.material_id`。命名不一致导致语义模糊 | 🟡 P1 |
| **inventory 同表混用 product_id + material_id** | `inventory` 表 | V1.0.0.100 建表时用 `product_id`，后期补了 `material_id` 列。`getProductId()` 返回 `materialId`，是别名而非真实 product_id | 🔴 P0 |

**案例验证**：
```
可乐: food_id=F001, material_id=M001
问题: 如果只知道 food_id=F001，系统能否自动找到 M001 对应的库存？
答案: 不能。需要业务代码手动映射，无数据层面的关联。
```

### 5.2 Identity Re-generation（身份再生）

**定义**：当一个已有 Identity 的实体被重新创建时，产生新的 Identity，但语义上是同一个东西。

| 问题 | 位置 | 描述 | 严重度 |
|------|------|------|--------|
| **同一商品在 foods 和 material_archives 各有一条记录** | `foods` + `material_archives` | 预包装品（可乐）在两个表中各有一个 Identity（food_id 和 material_id），但描述的是同一个物理实体 | 🟡 P1 |
| **v_inventory_summary 以 material_id AS product_id 输出** | `v_inventory_summary` | 视图将 `material_id` 别名为 `product_id` 输出，造成 Identity 混淆 | 🟡 P1 |
| **store_inventory_log 用 product_id 而 store_inventory 用 material_id** | `store_inventory_log` vs `store_inventory` | 同一模块的日志表和主表引用不同 Identity | 🟡 P1 |

**案例验证**：
```
可乐: 
  foods 表: food_id=F001, food_name='可乐', price=3.00
  material_archives 表: material_id=M001, material_name='可口可乐330ml', reference_price=2.50
  问题: 这是两个独立的 Identity，还是同一个东西的两个视角？
  答案: 语义上是同一个东西，但系统中是两个独立 Identity。
```

### 5.3 Wrong Identity（错误身份）

**定义**：系统使用了错误的 Identity 字段来标识一个实体。

| 问题 | 位置 | 描述 | 严重度 |
|------|------|------|--------|
| **OtherInboundService 把 productId 当 materialId 写** | `OtherInboundService.java:89/108` | `inventory.setMaterialId(productId)` — 变量名是 productId，但写入的是 materialId 字段 | 🔴 P0 |
| **purchase_order_items.product_id 存 material_id 值** | `purchase_order_items` | product_id 列实际存储的是 material_id 的值，列名与值语义不一致 | 🔴 P0 |
| **PurchaseStockinEventListener 用 productId 写 material_trace_codes** | `PurchaseStockinEventListener.java:73` | productId 变量实际是 material_id | 🟡 P1 |

**案例验证**：
```
采购入库:
  purchase_order_items.product_id = 1 (实际是 material_id=1 的值)
  inventory.setMaterialId(1) (从 productId 取值)
  问题: 如果 product 表中 product_id=1 对应的是"洗衣液"，而 material_archives 中 material_id=1 对应的是"可乐"，
        那么这个入库操作到底入库的是什么？
  答案: 当前系统中 product_id 的值与 material_id 的值恰好一致（同一条记录），
        但如果未来 product 表被清理，这个映射就会断裂。
```

### 5.4 Duplicate Identity（重复身份）

**定义**：同一个物理实体在系统中被赋予了多个 Identity，且这些 Identity 之间没有明确的关联关系。

| 问题 | 位置 | 描述 | 严重度 |
|------|------|------|--------|
| **可乐同时有 food_id 和 material_id** | `foods` + `material_archives` | 两个 Identity 独立存在，无外键关联 | 🟡 P1 |
| **鸡翅在 foods 和 material_archives 都可能有记录** | `foods` + `material_archives` | 如果鸡翅同时作为菜品销售和作为原料，会有两个 Identity | 🟡 P1 |
| **四套对象之间无任何物理外键互引** | `foods` / `material_archives` / `product` / `inventory` | 全库唯一指向 product 的物理外键 = `inventory.product_id → product` | 🔴 P0 |

**案例验证**：
```
可乐:
  foods 表: food_id=F001
  material_archives 表: material_id=M001
  问题: 两个 Identity 之间如何关联？
  答案: 只能通过业务代码手动映射（如 food_name 匹配），无数据层面的保证。
```

### 5.5 Implicit Identity（隐式身份）

**定义**：系统中存在隐含的 Identity 映射关系，没有显式建模。

| 问题 | 位置 | 描述 | 严重度 |
|------|------|------|--------|
| **dish_recipe 桥接 food_id ↔ material_id** | `dish_recipe` | `dish_id` 指向 `foods.food_id`，`ingredient_id` 指向 `material_archives.material_id`。这是唯一的跨域桥接表 | 🟡 P1 |
| **预包装品的 food ↔ material 映射** | 无物理表 | 预包装品（可乐）同时存在于 foods 和 material_archives，但映射关系隐含在业务逻辑中 | 🔴 P0 |
| **Inventory.getProductId() = getMaterialId()** | `Inventory.java:382-388` | 代码层的隐式映射，getProductId() 实际返回 materialId | 🟡 P1 |

**案例验证**：
```
可乐鸡翅的原料消耗:
  当可乐鸡翅被制作时:
    dish_recipe.dish_id = F009 (可乐鸡翅)
    dish_recipe.ingredient_id = M001 (可乐)
    dish_recipe.ingredient_id = M002 (鸡翅)
  
  库存扣减:
    inventory.material_id = M001, quantity_change = -0.2L
    inventory.material_id = M002, quantity_change = -0.5kg
  
  问题: dish_recipe 通过 ingredient_id 隐式桥接了 food_id 和 material_id，
        但这个桥接关系没有在数据模型中显式表达。
  答案: dish_recipe 是隐式 Identity 桥接，是当前系统中唯一的跨域关联机制。
```

---

## 6. Identity 问题全景表

| # | 问题类型 | 位置 | 描述 | 严重度 | 影响范围 |
|---|----------|------|------|--------|---------|
| 1 | Identity Loss | `foods` ↔ `material_archives` | 预包装品的 food_id 与 material_id 无物理映射 | 🔴 P0 | 销售→库存→采购全链路 |
| 2 | Wrong Identity | `purchase_order_items.product_id` | product_id 列实际存 material_id 值 | 🔴 P0 | 采购链路 |
| 3 | Identity Loss | `inventory.product_id` | 同表混用 product_id + material_id 双口径 | 🔴 P0 | 库存核心 |
| 4 | Wrong Identity | `OtherInboundService` | 把 productId 当 materialId 写 | 🔴 P0 | 其他入库 |
| 5 | Duplicate Identity | `foods` + `material_archives` | 同一商品两个独立 Identity | 🟡 P1 | 预包装品 |
| 6 | Implicit Identity | `dish_recipe` | ingredient_id 隐式桥接 food_id ↔ material_id | 🟡 P1 | 配方域 |
| 7 | Identity Re-generation | `v_inventory_summary` | material_id AS product_id 输出 | 🟡 P1 | 查询视图 |
| 8 | Identity Re-generation | `store_inventory_log` vs `store_inventory` | 日志表与主表 Identity 不一致 | 🟡 P1 | 门店库存日志 |
| 9 | Identity Loss | 全库 | 四套对象之间无物理外键互引 | 🔴 P0 | 全域 |

---

## 7. 核心决策：Inventory Identity 的关联方式

### 7.1 两种候选方案

#### 方案 A：Inventory 直接保存 Canonical Item Identity

```sql
-- 方案 A: 直接保存 Canonical Item Identity
CREATE TABLE inventory_instance (
    instance_id         BIGINT PRIMARY KEY,
    canonical_item_id   BIGINT NOT NULL,  -- 直接关联 Canonical Item
    location_id         BIGINT NOT NULL,
    current_stock       INTEGER NOT NULL DEFAULT 0,
    ...
    FOREIGN KEY (canonical_item_id) REFERENCES canonical_item(id)
);
```

**优点**：
- 简单直接，查询链路短
- 无需通过中间表间接关联
- 语义清晰：库存就是管理 Canonical Item 的

**缺点**：
- 需要先建立 Canonical Item 表（当前不存在）
- 需要将 material_id、food_id、product_id 统一映射到 canonical_item_id
- 迁移成本高

#### 方案 B：Inventory 通过 Material 间接关联

```sql
-- 方案 B: 通过 Material 间接关联（当前系统现实）
CREATE TABLE inventory_instance (
    instance_id         BIGINT PRIMARY KEY,
    material_id         BIGINT NOT NULL,  -- 关联 material_archives
    location_id         BIGINT NOT NULL,
    current_stock       INTEGER NOT NULL DEFAULT 0,
    ...
    FOREIGN KEY (material_id) REFERENCES material_archives(material_id)
);

-- Material 表作为事实上的 Canonical Identity
CREATE TABLE material_archives (
    material_id         BIGINT PRIMARY KEY,  -- 事实上的 Canonical Identity
    material_code       VARCHAR(50),
    material_name       VARCHAR(100),
    ...
);

-- Food 与 Material 的映射通过业务逻辑实现
-- dish_recipe 桥接 food_id ↔ material_id
```

**优点**：
- 与当前系统现实一致
- material_archives 已经承担了 Canonical Identity 的职责
- 迁移成本低

**缺点**：
- material_archives 的语义是"物料"，不是"规范标识"
- 预包装品的 food_id 与 material_id 仍然没有物理映射
- 语义上不够纯粹

### 7.2 推荐方案：方案 B（渐进式）

**理由**：

1. **当前系统现实**：material_archives 已经是事实上的 Canonical Identity，所有活跃表都以 `material_id` 为外键
2. **迁移成本**：方案 A 需要新建 Canonical Item 表并迁移所有数据，成本过高
3. **渐进式改进**：可以先在 material_archives 上建立 food_id ↔ material_id 的映射，再逐步收敛

**具体步骤**：

```
Phase 1: 在 material_archives 上添加 food_id 字段（可选）
  ALTER TABLE material_archives ADD COLUMN food_id BIGINT;
  -- 将预包装品的 food_id 回填

Phase 2: 建立显式映射表（可选）
  CREATE TABLE item_identity_mapping (
      material_id   BIGINT PRIMARY KEY,
      food_id       BIGINT,
      product_id    BIGINT,  -- 遗留
      UNIQUE (food_id)
  );

Phase 3: 统一 material_id 口径
  -- 所有新代码使用 material_id，不使用 product_id
  -- 标记 product_id 为 @Deprecated
```

### 7.3 最终推荐：Inventory 直接保存 material_id，通过映射表间接关联其他 Identity

```sql
-- 推荐的 Inventory Identity 架构

-- 1. 物料主数据（事实上的 Canonical Identity）
CREATE TABLE material_archives (
    material_id     BIGINT PRIMARY KEY,
    material_code   VARCHAR(50) NOT NULL,
    material_name   VARCHAR(100) NOT NULL,
    ...
);

-- 2. Identity 映射表（可选，用于跨域关联）
CREATE TABLE item_identity_map (
    material_id     BIGINT PRIMARY KEY,       -- 物料标识（Canonical）
    food_id         BIGINT,                   -- 销售标识（可选）
    product_id      BIGINT,                   -- 遗留标识（可选）
    FOREIGN KEY (material_id) REFERENCES material_archives(material_id),
    FOREIGN KEY (food_id) REFERENCES foods(food_id),
    UNIQUE (food_id)
);

-- 3. 库存实例（直接使用 material_id）
CREATE TABLE inventory_instance (
    instance_id     BIGINT PRIMARY KEY,
    material_id     BIGINT NOT NULL,          -- 直接关联物料
    location_id     BIGINT NOT NULL,
    current_stock   INTEGER NOT NULL DEFAULT 0,
    ...
    FOREIGN KEY (material_id) REFERENCES material_archives(material_id)
);

-- 4. 库存流水（直接使用 material_id）
CREATE TABLE inventory_ledger (
    ledger_id       BIGINT PRIMARY KEY,
    material_id     BIGINT NOT NULL,          -- 直接关联物料
    instance_id     BIGINT NOT NULL,          -- 关联库存实例
    ...
    FOREIGN KEY (material_id) REFERENCES material_archives(material_id),
    FOREIGN KEY (instance_id) REFERENCES inventory_instance(instance_id)
);
```

---

## 8. Identity 流转全景

### 8.1 采购→库存 流转

```
采购订单:
  purchase_order_items.material_id = M001  ← 直接使用 material_id
  purchase_order_items.product_id = M001   ← 遗留字段（值=material_id）

入库确认:
  purchase_stockin_items.material_id = M001  ← 直接使用 material_id

库存增加:
  inventory.material_id = M001              ← 直接使用 material_id
  inventory_transactions.material_id = M001  ← 直接使用 material_id

结论: 采购→库存链路中，material_id 是唯一活跃的 Identity。
```

### 8.2 销售→配方→库存 流转

```
销售下单:
  order_items.food_id = F001               ← 使用 food_id

菜品制作:
  dish_recipe.dish_id = F001               ← food_id（菜品标识）
  dish_recipe.ingredient_id = M001         ← material_id（原料标识）

库存扣减:
  inventory.material_id = M001             ← 直接使用 material_id
  inventory_transactions.material_id = M001 ← 直接使用 material_id

结论: 销售→配方→库存链路中，food_id 和 material_id 通过 dish_recipe 桥接。
      dish_recipe 是唯一的跨域 Identity 桥接表。
```

### 8.3 完整 Identity 流转图

```
                    ┌─────────────────────┐
                    │   Canonical Identity │
                    │   material_id = M001 │
                    └──────────┬──────────┘
                               │
           ┌───────────────────┼───────────────────┐
           ▼                   ▼                   ▼
    ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
    │  采购域       │   │  库存域       │   │  配方域       │
    │              │   │              │   │              │
    │ PO_items     │   │ inventory    │   │ dish_recipe  │
    │ .material_id │   │ .material_id │   │ .ingredient  │
    │ = M001       │   │ = M001       │   │  _id = M001  │
    └──────────────┘   └──────────────┘   └──────┬───────┘
                                                  │
                                                  │ 桥接
                                                  ▼
                                          ┌──────────────┐
                                          │   销售域       │
                                          │              │
                                          │ foods        │
                                          │ .food_id     │
                                          │ = F001       │
                                          │              │
                                          │ order_items  │
                                          │ .food_id     │
                                          │ = F001       │
                                          └──────────────┘
```

---

## 9. 关键原则

| # | 原则 | 说明 |
|---|------|------|
| 1 | **material_id 是当前系统的 Canonical Identity** | 所有活跃表都以 `material_id` 为外键，这是事实 |
| 2 | **food_id 是销售域的独立 Identity** | 门店销售使用 `food_id`，不使用 `material_id` |
| 3 | **dish_recipe 是唯一的跨域桥接表** | 通过 `dish_id`（→food_id）和 `ingredient_id`（→material_id）桥接两个域 |
| 4 | **product_id 已被历史淘汰** | 列名保留兼容，但值实际是 material_id |
| 5 | **预包装品存在 Identity Duplicate** | 同一物理实体在 foods 和 material_archives 各有一个 Identity，无物理映射 |
| 6 | **Inventory 应直接保存 material_id** | 不需要通过 Canonical Item 间接关联，material_id 已承担此职责 |
| 7 | **同一 Item 的多角色不等于多 Identity** | 可乐的 Sellable/Purchasable/Stockable/Recipe 角色都指向同一个 material_id=M001 |

---

## 10. 总结

| 问题 | 结论 |
|------|------|
| **Inventory 是否应该直接保存 Canonical Item Identity？** | **否**。当前系统中 `material_id` 已经是事实上的 Canonical Identity，Inventory 直接保存 `material_id` 即可。未来如果需要建立真正的 Canonical Item 表，可以通过 `item_identity_map` 映射表间接关联。 |
| **还是通过 Material / Product / Stockable Profile 间接关联？** | **通过 Material 直接关联**。`material_archives.material_id` 是当前系统的 Canonical Identity，所有库存表都直接使用它。Product 已废弃，Stockable Profile 是配置不是 Identity。 |
| **food_id 与 material_id 如何关联？** | 通过 `dish_recipe` 桥接表间接关联。预包装品需要建立显式的 `item_identity_map` 映射。 |
| **Inventory Identity 的最佳实践？** | Inventory 直接保存 `material_id`，通过 `item_identity_map` 映射表关联其他域的 Identity（food_id、product_id）。 |

---

## 11. 下一步行动

| # | 任务 | 优先级 | 说明 |
|---|------|--------|------|
| 1 | 建立 `item_identity_map` 映射表 | P1 | 解决预包装品的 food_id ↔ material_id 映射问题 |
| 2 | 统一 `material_id` 口径 | P0 | 所有新代码使用 `material_id`，标记 `product_id` 为 @Deprecated |
| 3 | 修复 `inventory.product_id` 双口径 | P0 | 清理遗留字段，确保 `material_id` 为唯一业务口径 |
| 4 | 审计 `dish_recipe.ingredient_id` 数据源 | P1 | 确认 `ingredient_id` 全部指向 `material_archives.material_id` |
| 5 | 修复 `OtherInboundService` 的 Wrong Identity | P0 | 修正 `productId` 变量实际是 `materialId` 的问题 |

---

**状态**：ANALYSIS, NOT CONFIRMED
**日期**：2026-09-10
**阶段**：Phase 18 + Phase 20 - Inventory Identity Analysis
