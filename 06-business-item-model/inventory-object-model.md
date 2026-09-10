# Inventory 对象模型分析 (Phase 17)

> **核心问题**：Inventory 管理的到底是什么？Material？Product？Stock Item？Canonical Item？
> **状态**：ANALYSIS, NOT CONFIRMED

---

## 1. 问题域

本分析必须回答：
1. `inventory` 表管理的粒度到底是什么？
2. "可乐为什么既能卖又能库存？矿泉水为什么既能卖又能库存？宫保鸡丁为什么通常不能像可乐一样库存？"
3. 预包装商品（薯片、瓶装水）处于什么位置？
4. Stockable 是 Item 的属性、Role 还是 Capability？

**关键约束**：不得为了迁就现有数据库表结构定义最终业务模型。

---

## 2. 候选概念定义

| 候选概念 | 定义 | 典型代表 | 系统中的位置 |
|----------|------|----------|-------------|
| **Material** | 可采购、可存储的物理实体 | 土豆、鸡翅、可乐、餐盒 | `material_archives` |
| **Product** | 可销售的商品（含成品和原料） | 可乐、薯片、矿泉水 | `product`（已废弃） |
| **Stock Item** | 在特定位置持有数量的库存实例 | 仓库A的可乐200罐 | `inventory` |
| **Canonical Item** | 跨所有业务域的唯一标识 | "可口可乐330ml"这一个东西 | 无（理想模型） |
| **Stock Role** | Material 在库存管理上下文中的角色 | 可乐作为库存项 | 无独立建模 |
| **Inventory Profile** | 描述一个 Item 如何被库存管理的配置 | 安全库存、保质期、存储温度 | 分散在多表 |

---

## 3. 业务案例深度分析

### 3.1 可乐（Cola）—— 为什么既能卖又能库存？

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

### 3.2 矿泉水（Mineral Water）—— 为什么既能卖又能库存？

**物理身份**：天然饮用水，塑料瓶包装。

**业务角色矩阵**：

| 角色 | 业务场景 | 系统表 | 关键数据 |
|------|----------|--------|----------|
| SELLABLE | 门店销售 | `foods` | food_id, price=2.00 |
| PURCHASABLE | 供应商采购 | `purchase_order_items` | material_id, unit_price=1.50 |
| STOCKABLE | 仓库存储 | `inventory` | material_id, warehouse_id, current_stock=200 |
| STORE-STOCKABLE | 门店库存 | `store_inventory` | material_id, store_id, current_stock=30 |
| INGREDIENT | 几乎不作为原料 | `dish_recipe` | 极少使用 |

**矿泉水为什么能库存？**
与可乐相同：预包装标准品，常温存储，保质期长（通常24个月），需求稳定。

**关键区别**：矿泉水几乎不作为菜品原料（可乐鸡翅是例外），所以它的 INGREDIENT 角色通常不激活。

### 3.3 宫保鸡丁（Kung Pao Chicken）—— 为什么通常不能像可乐一样库存？

**物理身份**：由鸡胸肉、花生、辣椒、花椒等原料现场烹饪的成品菜品。

**业务角色矩阵**：

| 角色 | 业务场景 | 系统表 | 关键数据 |
|------|----------|--------|----------|
| SELLABLE | 门店销售 | `foods` | food_id, price=28.00 |
| PURCHASABLE | 几乎不采购成品 | `purchase_order_items` | 通常不出现 |
| STOCKABLE | 通常不存储成品 | `inventory` | 通常不出现 |
| INGREDIENT | 几乎不作为其他菜品原料 | `dish_recipe` | 通常不出现 |

**宫保鸡丁为什么不能库存？**
1. **物理形态**：现做现卖的成品，非预包装
2. **保质期**：极短（几小时内），无法长期存储
3. **存储条件**：需要冷藏/保温，成本高
4. **需求模式**：波动大，难以精确预测
5. **采购模式**：不采购成品，只采购原料

**但宫保鸡丁的原料可以库存**：
- 鸡胸肉 → `material_archives` → `inventory`（冷藏）
- 花生 → `material_archives` → `inventory`（常温）
- 辣椒 → `material_archives` → `inventory`（常温）

**关键洞察**：宫保鸡丁作为一个**成品菜品**，它的库存管理粒度不是"宫保鸡丁"本身，而是它的**原料**。Recipe/BOM 是连接成品和原料库存的桥梁。

### 3.4 预包装商品（薯片、瓶装水、袋装大米）—— 处于什么位置？

**预包装商品的共同特征**：
- 有独立的 SKU
- 有标准的包装规格
- 有明确的保质期
- 可直接销售
- 可批量存储
- 通常不作为菜品原料

**预包装商品在模型中的位置**：

| 维度 | 预包装商品 | 现做菜品 | 原料 |
|------|-----------|---------|------|
| 物理形态 | 预包装、标准规格 | 现场烹饪 | 散装/预包装 |
| 可直接销售 | ✅ 是 | ✅ 是 | ❌ 否（通常） |
| 可存储 | ✅ 是 | ❌ 否（通常） | ✅ 是 |
| 可作为原料 | ❌ 否（通常） | ❌ 否 | ✅ 是 |
| 保质期 | 中等（月级） | 极短（小时级） | 中等（天-月级） |

**预包装商品 = Material + SELLABLE Role**

这是餐饮ERP的核心特征：同一个预包装商品（可乐）同时承担 Material 和 Food 的双重身份。

---

## 4. 当前系统现实分析

### 4.1 表结构依赖图

```
material_archives (商品档案 - 主数据)
  │
  ├──► inventory (仓库库存 - 按 warehouse_id + material_id)
  │       ├── id (PK)
  │       ├── product_id (遗留字段，实际引用 material_id)
  │       ├── warehouse_id
  │       ├── current_stock, safety_stock
  │       ├── cost_price, stock_value
  │       └── inventory_type
  │
  ├──► store_inventory (门店库存 - 按 store_id + material_id)
  │       ├── store_id
  │       ├── material_id (FK → material_archives)
  │       ├── current_stock, unit_cost, total_cost
  │       └── safety_stock, max_stock
  │
  └──► inventory_transactions (库存流水)
          ├── transaction_id (PK)
          ├── material_id (冗余，便于查询)
          ├── inventory_id (FK → inventory)
          ├── warehouse_id
          ├── quantity_change, before_qty, after_qty
          └── unit_cost, total_cost
```

### 4.2 关键发现

| 发现 | 证据 | 影响 |
|------|------|------|
| 库存管理的粒度是 `material_id` | `inventory.product_id`（遗留）实际引用 material_id | 库存项 = material 的某位置实例 |
| `inventory` 表有 `product_id` 但无 `material_id` | V1.0.0.100 遗留，Java 实体有兼容性 getter | 历史遗留，语义混乱 |
| 仓库库存和门店库存完全独立 | `inventory` 用 `warehouse_id`，`store_inventory` 用 `store_id`，两表无外键关系 | 存在双维度库存模型 |
| `material_archives` 是采购和库存的共享主数据 | 所有活跃采购表和库存表都引用 `material_id` | Material 是跨域的 Canonical Identity |

### 4.3 从 V1.0.0.100 到当前的演进路径

```
V1.0.0.100:
  inventory (product_id, warehouse_id, store_id)  ← 单表管理所有库存
  ↓
V20260704_004:
  store_inventory (store_id, material_id)  ← 门店库存独立建表
  ↓
V20260723_005:
  inventory_transactions (material_id, inventory_id, warehouse_id)  ← 流水表重建
  ↓
V20260816_001:
  inventory 补齐 material_name, specification, location_id, locked_quantity 等 11 列
  ← 物料口径对齐
```

**结论**：系统从 product-centric 演进为 material-centric，但演进过程中产生了遗留字段和命名不一致。

### 4.4 inventory 表的语义困境

`inventory` 表的当前结构：

```sql
CREATE TABLE inventory (
    id                  BIGSERIAL       PRIMARY KEY,
    product_id          BIGINT           DEFAULT NULL,  -- 遗留字段，实际引用 material_id
    product_name        VARCHAR(200)     DEFAULT NULL,  -- 遗留字段
    warehouse_id        BIGINT           DEFAULT NULL,
    store_id            BIGINT           DEFAULT NULL,  -- 同时有 warehouse_id 和 store_id
    current_stock       INTEGER          NOT NULL DEFAULT 0,
    safety_stock        INTEGER          DEFAULT 0,
    unit                VARCHAR(20)      DEFAULT '件',
    cost_price          DECIMAL(12,2)    DEFAULT 0,
    stock_value         DECIMAL(14,4)    DEFAULT 0,
    inventory_type      INTEGER          DEFAULT 1,
    ...
);
```

**问题**：
1. `product_id` 是遗留字段，实际引用 `material_archives.material_id`，但列名仍叫 `product_id`
2. 同时有 `warehouse_id` 和 `store_id`，但 `store_inventory` 已独立建表
3. `inventory_type` 的语义不明确（1=仓库库存？2=门店库存？）
4. `product_name` 是冗余字段，应为 `material_name`

---

## 5. 候选概念深度比较

### 5.1 Material

**定义**：可采购、可存储的物理实体，以 `material_id` 为唯一标识。

**支持证据**：
- 所有活跃库存表都使用 `material_id`（或等价的 `product_id` 遗留字段）
- `material_archives` 是采购和库存的共享主数据
- 采购流程：`purchase_order_items.material_id` → `material_archives.material_id`
- 库存流程：`inventory.product_id`（实际=material_id）→ `material_archives.material_id`

**反对证据**：
- `material_archives` 叫"商品档案"而非"物料档案"，语义含糊
- Material 在不同业务场景中承担不同角色（采购、库存、原料），但 Material 本身不区分这些角色
- Material 无法表达"可乐在门店A可销售，在门店B不可销售"的差异

**结论**：Material 是当前系统的 Canonical Identity，但语义需要澄清。

### 5.2 Product

**定义**：可销售的商品（含成品和原料）。

**支持证据**：
- `inventory.product_id` 仍使用 product_id
- `Inventory.java` 中 `getProductId()` 返回 `materialId`

**反对证据**：
- `product` 表已被标记为废弃
- 当前所有活跃采购表和库存表都使用 `material_id`
- Product 无法区分"可销售"和"可采购"

**结论**：Product 已被历史淘汰，不应作为 Inventory 的管理对象。

### 5.3 Stock Item

**定义**：在特定位置持有数量的库存实例，粒度为 (material, location, batch)。

**支持证据**：
- `inventory` 表的 (product_id, warehouse_id) 组合实际上就是 Stock Item
- `store_inventory` 表的 (store_id, material_id) 组合也是 Stock Item
- 库存流水 `inventory_transactions` 同时记录 `inventory_id` 和 `material_id`

**反对证据**：
- 系统中无独立的 StockItem 实体
- 库存直接用 material_id + warehouse_id 作为逻辑主键
- Stock Item 的概念未被显式建模

**结论**：Stock Item 是库存管理的正确粒度，但当前未被显式建模。

### 5.4 Canonical Item

**定义**：跨所有业务域的唯一标识，是"这个东西是什么"的最终答案。

**支持证据**：
- `business-role-model.md` 和 `type-vs-role-analysis.md` 都推荐 Canonical Identity + Role 模型
- 同一个"可乐"在 foods、material_archives、inventory 中存在三个独立表示，应统一
- Canonical Item 是解决身份碎片化的根本方案

**反对证据**：
- 系统中无此实体
- 引入 Canonical Item 需要重构所有业务流程
- 迁移复杂度高

**结论**：Canonical Item 是目标模型的理想形态，但当前未实现。

### 5.5 Stock Role

**定义**：Material 在库存管理上下文中的角色，表示"可以被存储"。

**支持证据**：
- `business-role-model.md` 定义了 STOCKABLE 作为业务角色
- `type-vs-role-analysis.md` 推荐 Type 和 Role 分离
- Stock Role 可以携带库存特定的配置（安全库存、保质期、存储条件）

**反对证据**：
- 系统中无独立的 StockRole 实体
- 库存配置分散在 inventory、store_inventory、material_archives 多表中

**结论**：Stock Role 是目标模型中库存角色的正确抽象，但当前未被显式建模。

### 5.6 Inventory Profile

**定义**：描述一个 Item 如何被库存管理的配置，包括存储条件、保质期、安全库存等。

**支持证据**：
- 库存管理需要多种配置：安全库存、最大库存、保质期、存储温度
- 这些配置当前分散在 inventory、store_inventory、material_archives 多表中
- Inventory Profile 可以将这些配置统一管理

**反对证据**：
- 系统中无独立的 InventoryProfile 实体
- Profile 的概念过于宽泛，与 Stock Role 有重叠

**结论**：Inventory Profile 是 Stock Role 的配置层，可以作为 Stock Role 的属性存在。

---

## 6. 推荐模型：三层架构

### 6.1 模型结构

```
Layer 1: Canonical Identity (标识层)
  └── material_archives (material_id)
        "这是什么？" → 一袋5kg装的东北大米

Layer 2: Stock Role (能力层)
  └── stock_role_config (material_id, role_type)
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
Canonical Identity: 可口可乐330ml (C001)
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

**Inventory Instance = Stock Role 在特定 Location 的运行时状态**

```
inventory_instance {
    canonical_item_id:    -- 统一身份标识
    stock_role_id:        -- 库存角色标识
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

## 8. 业务案例验证

### 8.1 案例一：可乐的库存管理

**当前系统**：
- `material_archives`: material_id=M001, material_name="可口可乐330ml"
- `inventory`: product_id=M001(遗留), warehouse_id=W01, current_stock=200
- `store_inventory`: material_id=M001, store_id=S01, current_stock=20
- `inventory_transactions`: material_id=M001, inventory_id=I001, transaction_type=1, quantity_change=+200

**目标模型**：
- `canonical_item`: id=C001, name="可口可乐330ml"
- `stock_role`: canonical_item_id=C001, role_type=STOCKABLE
- `inventory_instance`: stock_role_id=SR001, location_id=W01, current_stock=200
- `inventory_instance`: stock_role_id=SR001, location_id=S01, current_stock=20

### 8.2 案例二：宫保鸡丁的库存管理

**当前系统**：
- `foods`: food_id=F001, food_name="宫保鸡丁"
- `inventory`: 不存储成品宫保鸡丁
- `material_archives`: material_id=M002(鸡胸肉), M003(花生), M004(辣椒)
- `inventory`: 鸡胸肉50kg, 花生20kg, 辣椒10kg

**目标模型**：
- `canonical_item`: id=C002, name="宫保鸡丁"
- `stock_role`: 无（宫保鸡丁不作为库存项）
- `recipe`: canonical_item_id=C002, ingredient_id=C003(鸡胸肉), quantity=0.3kg

### 8.3 案例三：预包装薯片的库存管理

**当前系统**：
- `material_archives`: material_id=M005, material_name="乐事薯片104g"
- `inventory`: product_id=M005(遗留), warehouse_id=W01, current_stock=80
- `store_inventory`: material_id=M005, store_id=S01, current_stock=10
- `dish_recipe`: 通常不作为原料

**目标模型**：
- `canonical_item`: id=C005, name="乐事薯片104g"
- `stock_role`: canonical_item_id=C005, role_type=STOCKABLE
- `inventory_instance`: stock_role_id=SR005, location_id=W01, current_stock=80
- `ingredient_role`: 无（薯片不作为菜品原料）

---

## 9. 三维度分离

### 9.1 CURRENT REALITY（当前现实）

| 维度 | 现状 | 问题 |
|------|------|------|
| 库存粒度 | `material_id` | 遗留字段 `product_id` 仍存在 |
| 位置模型 | `warehouse_id` + `store_id` | 双维度库存，无统一位置模型 |
| 成本模型 | `cost_price` / `unit_cost` | 仓库和门店成本独立维护 |
| 配置模型 | 分散在多表 | 安全库存、保质期等配置不统一 |

### 9.2 TARGET BUSINESS SEMANTICS（目标业务语义）

| 维度 | 目标 | 理由 |
|------|------|------|
| 库存粒度 | **Stock Role Instance** | 库存管理的是 Material 的库存角色实例 |
| 位置模型 | **统一 Location** | warehouse 和 store 统一为 Location + LocationType |
| 成本模型 | **统一成本核算** | 入库成本 → 库存成本 → 出库成本，统一核算规则 |
| 配置模型 | **Stock Role Config** | 安全库存、保质期、存储条件统一管理 |

### 9.3 TARGET TECHNICAL MODEL（目标技术模型）

```sql
-- 统一位置模型
CREATE TABLE location (
    location_id     BIGINT PRIMARY KEY,
    location_type   VARCHAR(20) NOT NULL,  -- 'WAREHOUSE' | 'STORE'
    location_name   VARCHAR(100) NOT NULL,
    ...
);

-- 库存角色配置
CREATE TABLE stock_role_config (
    config_id       BIGINT PRIMARY KEY,
    canonical_item_id BIGINT NOT NULL,  -- 关联 canonical_item
    location_id     BIGINT,            -- 可选：特定位置配置
    safety_stock    INTEGER DEFAULT 0,
    max_stock       INTEGER DEFAULT 0,
    shelf_life_days INTEGER,           -- 保质期（天）
    storage_temp    VARCHAR(50),       -- 存储温度
    ...
);

-- 统一库存实例
CREATE TABLE inventory_instance (
    instance_id     BIGINT PRIMARY KEY,
    canonical_item_id BIGINT NOT NULL,
    location_id     BIGINT NOT NULL,
    current_stock   INTEGER NOT NULL DEFAULT 0,
    locked_quantity INTEGER DEFAULT 0,
    unit_cost       DECIMAL(12,2) DEFAULT 0,
    total_cost      DECIMAL(14,4) DEFAULT 0,
    batch_no        VARCHAR(50),
    production_date DATE,
    expiry_date     DATE,
    UNIQUE (canonical_item_id, location_id, batch_no)
);
```

---

## 10. 遗留问题与技术债

### 10.1 命名不一致

| 问题 | 位置 | 建议 |
|------|------|------|
| `material_archives` 叫"商品档案"而非"物料档案" | 表注释 | 统一为"物料主数据"或接受"商品档案"作为采购视角的名称 |
| `inventory.product_id` 仍使用 product_id | V1.0.0.100 遗留 | 应迁移为 material_id |
| `Inventory.java` 中 `getProductId()` 返回 `materialId` | 兼容性方法 | 应标记 @Deprecated |
| `inventory.product_name` 是遗留字段 | V1.0.0.100 遗留 | 应迁移为 material_name |

### 10.2 双维度库存的同步问题

- `inventory` 和 `store_inventory` 是独立表，无事务一致性保证
- 门店收货确认时需要同时更新两个表
- 缺少库存同步机制（如：门店申请 → 仓库调拨 → 双表联动）

### 10.3 成本核算的割裂

- `inventory.cost_price` 与 `store_inventory.unit_cost` 独立维护
- 采购入库单有 `unit_price`，入库后更新 `inventory.cost_price`，但门店调拨时成本如何传递未定义
- 系统中未见明确的库存成本核算规则（如加权平均法、先进先出法）

---

## 11. 推荐改进方向

### 11.1 短期（保持现有结构）

1. **统一 material_id 口径**：确保所有新增表和字段使用 `material_id`，不使用 `product_id`
2. **清理 Inventory.java 的兼容性方法**：标记 `getProductId()` 为 @Deprecated
3. **修复 inventory.product_id**：在后续迁移中添加 `material_id` 列并回填数据

### 11.2 中期（引入 Stock Role 配置）

1. **创建 `stock_role_config` 表**：将分散在 inventory / store_inventory 的预警阈值、存储条件统一管理
2. **标准化库存预警**：基于 stock_role_config 统一预警规则

### 11.3 长期（统一库存模型）

1. **引入统一 Location 模型**：warehouse_id 和 store_id 统一为 location_id + location_type
2. **合并 inventory 和 store_inventory**：统一为 inventory_instance 表
3. **引入 Canonical Item**：建立跨域的统一身份标识

---

## 12. 总结

| 问题 | 结论 |
|------|------|
| Inventory 管理的到底是什么？ | **Material**（物料/商品），以 material_id 为 canonical identity |
| 可乐为什么能库存？ | 预包装标准品，有独立 SKU、标准规格、保质期长、需求稳定 |
| 宫保鸡丁为什么不能库存？ | 现做现卖的成品，保质期极短，不采购成品，只采购原料 |
| 预包装商品处于什么位置？ | **Material + SELLABLE Role**，同时承担采购和销售双重身份 |
| Stockable 是什么？ | 是 Material 的一个 **Business Role**，表示"可以被存储" |
| 库存的核心抽象是什么？ | **Material + Location → Inventory Instance**（物料 + 位置 → 库存实例） |
| 仓库库存和门店库存的关系？ | 同一 Material 在不同 Location 类型的 Stock Role 实例化 |

---

**状态**：ANALYSIS, NOT CONFIRMED
**日期**：2026-09-09
**作者**：AI 架构总控
