# Stockable Capability 分析 (Phase 3)

> **核心问题**：STOCKABLE 是 Role、Capability、Profile、Type 还是 Contextual Property？
> **状态**：ANALYSIS, NOT CONFIRMED

---

## 1. 问题域

本分析必须回答：
1. STOCKABLE 应该是 Role、Capability、Profile、Type 还是 Contextual Property？
2. 库存能力的作用域应该是 GLOBAL、STORE-SCOPED、WAREHOUSE-SCOPED 还是 LOCATION-SCOPED？
3. 当前系统现实与目标模型之间的差距

**关键约束**：
- Current DB Model ≠ Target Inventory Model
- 不要因为当前实现就认定未来模型
- 必须从业务语义角度分析

---

## 2. 候选建模范式

### 2.1 范式 A：Stockable 作为 Type（类型）

```typescript
// Material 有一个类型字段，标识其是否可存储
interface Material {
  id: MaterialId;
  type: MaterialType;  // 'RAW_MATERIAL' | 'SEMI_FINISHED' | 'FINISHED_GOOD' | 'CONSUMABLE'
  stockable: boolean;  // 是否可存储
}
```

**优点**：
- 简单，查询快
- 类型是固定的，不会频繁变化

**缺点**：
- 类型是 Material 的固有属性，无法表达位置差异
- 无法表达"可乐在门店A可存储，在门店B不可存储"的差异
- 类型是二元的（stockable / not stockable），无法表达存储的具体能力

**适用场景**：简单业务，类型固定

### 2.2 范式 B：Stockable 作为 Role（角色）

```typescript
// Material 是主体，Stockable 是其扮演的角色之一
interface Material { id: MaterialId; ... }

interface StockableRole {
  materialId: MaterialId;
  locationId: LocationId;
  safetyStock: number;
  maxStock: number;
  shelfLife?: number;    // 保质期（天）
  storageTemp?: string;  // 存储温度
}

// 库存表 = StockableRole 的实例化
interface InventoryRecord {
  stockableRole: StockableRole;
  currentStock: number;
  batchNo: string;
  ...
}
```

**优点**：
- 角色携带业务语义
- 一个 Material 可以在不同 Location 扮演不同 StockableRole
- 角色可以有配置（安全库存、保质期等）

**缺点**：
- 查询链变长
- 需要 Role 表

**适用场景**：多仓库、多存储条件的中型系统

### 2.3 范式 C：Stockable 作为 Capability（能力声明）

```typescript
// Material 声明自己具备 stockable 能力，能力由具体模块实现
interface Material {
  id: MaterialId;
  capabilities: Capability[];
}

type Capability =
  | { type: 'stockable'; config: StockableConfig }
  | { type: 'purchasable'; config: PurchasableConfig }
  | { type: 'sellable'; config: SellableConfig }
  | { type: 'consumable'; config: ConsumableConfig };
```

**优点**：
- 高度灵活
- 能力可动态添加
- 能力可以携带配置

**缺点**：
- 过度抽象
- 当前系统无需此复杂度

**适用场景**：大型平台化系统

### 2.4 范式 D：Stockable 作为 Profile（配置文件）

```typescript
// Material 有一个库存配置文件，描述如何被库存管理
interface Material {
  id: MaterialId;
  inventoryProfile?: InventoryProfile;
}

interface InventoryProfile {
  safetyStock: number;
  maxStock: number;
  shelfLifeDays: number;
  storageTemp: string;
  reorderPoint: number;
  reorderQuantity: number;
}
```

**优点**：
- 配置集中管理
- 可以有默认配置和覆盖配置

**缺点**：
- Profile 是配置，不是能力声明
- 无法表达"可乐在门店A可存储，在门店B不可存储"的差异

**适用场景**：配置驱动的系统

### 2.5 范式 E：Stockable 作为 Contextual Property（上下文属性）

```typescript
// Material 在不同上下文中有不同的库存属性
interface Material {
  id: MaterialId;
  inventoryProperties: Map<LocationId, InventoryProperty>;
}

interface InventoryProperty {
  locationId: LocationId;
  isStockable: boolean;
  safetyStock: number;
  maxStock: number;
}
```

**优点**：
- 上下文感知
- 可以表达位置差异

**缺点**：
- 属性是数据，不是行为
- 无法表达能力的生命周期

**适用场景**：需要位置差异的系统

---

## 3. 候选作用域

### 3.1 GLOBAL（全局）

**定义**：Stockable 是 Material 的全局属性，所有位置共享同一配置。

```
Material (可乐) → STOCKABLE (全局) → 所有仓库/门店都可存储
```

**优点**：
- 简单，配置集中
- 查询快

**缺点**：
- 无法表达位置差异
- 无法表达"可乐在门店A可存储，在门店B不可存储"的差异

**适用场景**：所有位置的存储条件相同

### 3.2 STORE-SCOPED（门店级）

**定义**：Stockable 是 Material 在特定门店的属性，每个门店独立配置。

```
Material (可乐) → STOCKABLE (门店A) → 门店A可存储
             → STOCKABLE (门店B) → 门店B不可存储
```

**优点**：
- 可以表达门店差异
- 门店独立管理

**缺点**：
- 配置分散
- 查询需要关联门店

**适用场景**：门店有独立库存管理需求

### 3.3 WAREHOUSE-SCOPED（仓库级）

**定义**：Stockable 是 Material 在特定仓库的属性，每个仓库独立配置。

```
Material (可乐) → STOCKABLE (仓库A) → 仓库A可存储
             → STOCKABLE (仓库B) → 仓库B可存储
```

**优点**：
- 可以表达仓库差异
- 仓库独立管理

**缺点**：
- 配置分散
- 查询需要关联仓库

**适用场景**：仓库有独立存储条件（如冷藏库、常温库）

### 3.4 LOCATION-SCOPED（位置级）

**定义**：Stockable 是 Material 在特定位置（仓库或门店）的属性，每个位置独立配置。

```
Material (可乐) → STOCKABLE (位置A) → 位置A可存储
             → STOCKABLE (位置B) → 位置B不可存储
```

**优点**：
- 统一的位置模型
- 最灵活

**缺点**：
- 配置最分散
- 查询最复杂

**适用场景**：需要统一管理仓库和门店的库存

---

## 4. 业务案例验证

### 4.1 案例一：可乐在不同门店的库存能力

**场景**：
- 门店 A（大型门店）：可乐 Store A → Stockable，可以存储200罐
- 门店 B（小型门店）：可乐 Store B → 非 Stockable，不存储可乐

**问题**：这种情况是否应该允许？

**分析**：
- 门店 A 是大型门店，有冷藏柜，可以存储可乐
- 门店 B 是小型门店，没有冷藏柜，不存储可乐
- 这是合理的业务场景

**结论**：Stockable 应该是 **STORE-SCOPED** 或 **LOCATION-SCOPED**，不能是 GLOBAL。

### 4.2 案例二：可乐在不同仓库的库存能力

**场景**：
- 中央仓库：可乐 → Stockable，可以存储2000罐
- 冷藏仓库：可乐 → 非 Stockable，不存储可乐（冷藏仓库只存生鲜）

**问题**：这种情况是否应该允许？

**分析**：
- 中央仓库是常温仓库，可以存储可乐
- 冷藏仓库只存生鲜，不存储可乐
- 这是合理的业务场景

**结论**：Stockable 应该是 **WAREHOUSE-SCOPED** 或 **LOCATION-SCOPED**，不能是 GLOBAL。

### 4.3 案例三：鸡翅在不同位置的库存能力

**场景**：
- 中央仓库：鸡翅 → Stockable，可以存储500kg
- 冷藏仓库：鸡翅 → Stockable，可以存储100kg
- 门店 A：鸡翅 → Stockable，可以存储50kg
- 门店 B：鸡翅 → 非 Stockable，不存储鸡翅（小型门店不存储生鲜）

**问题**：这种情况是否应该允许？

**分析**：
- 鸡翅需要冷藏存储，不同位置的冷藏条件不同
- 不同位置的存储能力不同
- 这是合理的业务场景

**结论**：Stockable 应该是 **LOCATION-SCOPED**，不能是 GLOBAL。

### 4.4 案例四：餐盒在不同位置的库存能力

**场景**：
- 中央仓库：餐盒 → Stockable，可以存储10000个
- 门店 A：餐盒 → Stockable，可以存储500个
- 门店 B：餐盒 → Stockable，可以存储300个

**问题**：这种情况是否应该允许？

**分析**：
- 餐盒是耗材，所有位置都需要
- 不同位置的存储能力不同
- 这是合理的业务场景

**结论**：Stockable 应该是 **LOCATION-SCOPED**，不能是 GLOBAL。

---

## 5. 当前系统现实分析

### 5.1 表结构依赖图

```
material_archives (商品档案 - 主数据)
  │
  ├──► inventory (仓库库存 - 按 warehouse_id + material_id)
  │       ├── id (PK)
  │       ├── material_id (FK → material_archives)
  │       ├── warehouse_id
  │       ├── current_stock, locked_quantity
  │       ├── unit_cost, total_cost
  │       ├── batch_no, production_date, expiry_date
  │       └── min_safe_qty, max_stock_qty
  │
  ├──► store_inventory (门店库存 - 按 store_id + material_id)
  │       ├── id (PK)
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

### 5.2 关键发现

| 发现 | 证据 | 影响 |
|------|------|------|
| 库存管理的粒度是 `material_id` | `inventory.material_id`, `store_inventory.material_id` | 库存项 = material 的某位置实例 |
| 仓库库存和门店库存完全独立 | `inventory` 用 `warehouse_id`，`store_inventory` 用 `store_id`，两表无外键关系 | 存在双维度库存模型 |
| 库存配置分散在多表 | 安全库存在 `inventory.safety_stock` 和 `store_inventory.safety_stock`；最大库存在 `inventory.max_stock_qty` 和 `store_inventory.max_stock` | 配置不统一 |
| 无统一的位置模型 | `warehouse_id` 和 `store_id` 是两个独立字段 | 无法统一管理位置 |

### 5.3 当前系统的 Stockable 能力模型

**当前系统隐含的 Stockable 能力模型**：

```
Material (可乐)
  │
  ├──► inventory (仓库维度)
  │     ├── warehouse_id = W01
  │     ├── current_stock = 200
  │     ├── safety_stock = 50
  │     └── max_stock_qty = 500
  │
  └──► store_inventory (门店维度)
        ├── store_id = S01
        ├── current_stock = 20
        ├── safety_stock = 10
        └── max_stock = 100
```

**结论**：当前系统已经隐含了 **LOCATION-SCOPED** 的 Stockable 能力模型，但没有显式建模。

---

## 6. 推荐建模范式

### 6.1 推荐：Stockable 作为 Role（角色）

**理由**：

1. **系统复杂度匹配**：当前系统是中型餐饮 ERP，Role 范式的复杂度恰到好处
2. **已有建模基础**：`business-role-model.md` 已定义了 SELLABLE / PURCHASABLE / STOCKABLE / INGREDIENT 四种角色
3. **库存双维度**：`inventory` 和 `store_inventory` 实质上是 StockableRole 在不同位置的实例
4. **与采购模型衔接**：StockableRole 的上游是 PURCHASABLE，下游是 CONSUMABLE

**Role vs Capability vs Profile vs Type vs Contextual Property**：

| 范式 | 适用性 | 理由 |
|------|--------|------|
| Type | ❌ 不适用 | Type 是固有属性，无法表达位置差异 |
| Role | ✅ 推荐 | Role 携带业务语义，可以有配置，可以有多实例 |
| Capability | ⚠️ 过度抽象 | 当前系统无需此复杂度 |
| Profile | ⚠️ 不够灵活 | Profile 是配置，不是能力声明 |
| Contextual Property | ⚠️ 不够强 | 属性是数据，不是行为 |

### 6.2 推荐作用域：LOCATION-SCOPED（位置级）

**理由**：

1. **业务需求**：不同位置的存储条件不同（冷藏库 vs 常温库）
2. **当前现实**：系统已经隐含了位置级的库存能力模型
3. **灵活性**：位置级是最灵活的，可以退化为门店级或仓库级

**作用域选择**：

| 作用域 | 适用性 | 理由 |
|--------|--------|------|
| GLOBAL | ❌ 不适用 | 无法表达位置差异 |
| STORE-SCOPED | ⚠️ 部分适用 | 只能表达门店差异，无法表达仓库差异 |
| WAREHOUSE-SCOPED | ⚠️ 部分适用 | 只能表达仓库差异，无法表达门店差异 |
| LOCATION-SCOPED | ✅ 推荐 | 统一管理仓库和门店的库存能力 |

---

## 7. Stockable Role 的数据模型

### 7.1 推荐的 StockableRole 配置字段

| 字段 | 当前位置 | 语义 |
|------|----------|------|
| safety_stock | inventory / store_inventory | 安全库存 |
| max_stock / max_stock_qty | inventory / store_inventory | 最大库存 |
| shelf_life | material_archives (有字段但未用) | 保质期 |
| storage_condition | material_archives (有字段但未用) | 存储条件 |
| min_safe_qty | inventory | 仓库安全库存 |
| locked_quantity | inventory | 锁定数量 |

### 7.2 推荐的数据模型

```sql
-- 库存角色配置（LOCATION-SCOPED）
CREATE TABLE stockable_role_config (
    config_id       BIGINT PRIMARY KEY,
    material_id     BIGINT NOT NULL,       -- 关联 material_archives
    location_id     BIGINT NOT NULL,       -- 关联 location 表
    safety_stock    INTEGER DEFAULT 0,     -- 安全库存
    max_stock       INTEGER DEFAULT 0,     -- 最大库存
    shelf_life_days INTEGER,               -- 保质期（天）
    storage_temp    VARCHAR(50),           -- 存储温度
    reorder_point   INTEGER DEFAULT 0,     -- 再订货点
    reorder_qty     INTEGER DEFAULT 0,     -- 再订货量
    status          INTEGER DEFAULT 1,     -- 状态：1启用 0停用
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER NOT NULL DEFAULT 0,
    UNIQUE (material_id, location_id)
);

-- 统一位置模型
CREATE TABLE location (
    location_id     BIGINT PRIMARY KEY,
    location_type   VARCHAR(20) NOT NULL,  -- 'WAREHOUSE' | 'STORE'
    location_name   VARCHAR(100) NOT NULL,
    address         VARCHAR(200),
    contact_person  VARCHAR(50),
    contact_phone   VARCHAR(20),
    status          INTEGER DEFAULT 1,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER NOT NULL DEFAULT 0
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
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER NOT NULL DEFAULT 0,
    UNIQUE (material_id, location_id, batch_no)
);
```

### 7.3 与当前系统的映射

```
当前系统                          目标系统
─────────────────────────────────────────────────────────
inventory.warehouse_id      →    location (type='WAREHOUSE')
inventory.material_id       →    inventory_instance.material_id
inventory.current_stock     →    inventory_instance.current_stock
inventory.safety_stock      →    stockable_role_config.safety_stock

store_inventory.store_id    →    location (type='STORE')
store_inventory.material_id →    inventory_instance.material_id
store_inventory.current_stock →  inventory_instance.current_stock
store_inventory.safety_stock →   stockable_role_config.safety_stock
```

---

## 8. 业务案例验证（目标模型）

### 8.1 案例一：可乐在不同门店的库存能力（目标模型）

```sql
-- 位置定义
INSERT INTO location (location_id, location_type, location_name) VALUES
(1, 'STORE', '门店A（大型）'),
(2, 'STORE', '门店B（小型）');

-- 可乐的库存角色配置
INSERT INTO stockable_role_config (material_id, location_id, safety_stock, max_stock) VALUES
(1001, 1, 10, 200),  -- 门店A：可乐可存储，安全库存10，最大200
(1001, 2, 0, 0);     -- 门店B：可乐不可存储，安全库存0，最大0

-- 库存实例
INSERT INTO inventory_instance (material_id, location_id, current_stock) VALUES
(1001, 1, 50);       -- 门店A：当前库存50
-- 门店B：无库存记录（max_stock=0 表示不可存储）
```

**结论**：目标模型可以正确表达"可乐在门店A可存储，在门店B不可存储"。

### 8.2 案例二：可乐在不同仓库的库存能力（目标模型）

```sql
-- 位置定义
INSERT INTO location (location_id, location_type, location_name) VALUES
(1, 'WAREHOUSE', '中央仓库'),
(2, 'WAREHOUSE', '冷藏仓库');

-- 可乐的库存角色配置
INSERT INTO stockable_role_config (material_id, location_id, safety_stock, max_stock) VALUES
(1001, 1, 50, 2000),  -- 中央仓库：可乐可存储，安全库存50，最大2000
(1001, 2, 0, 0);      -- 冷藏仓库：可乐不可存储，安全库存0，最大0

-- 库存实例
INSERT INTO inventory_instance (material_id, location_id, current_stock) VALUES
(1001, 1, 500);       -- 中央仓库：当前库存500
-- 冷藏仓库：无库存记录（max_stock=0 表示不可存储）
```

**结论**：目标模型可以正确表达"可乐在中央仓库可存储，在冷藏仓库不可存储"。

### 8.3 案例三：鸡翅在不同位置的库存能力（目标模型）

```sql
-- 位置定义
INSERT INTO location (location_id, location_type, location_name) VALUES
(1, 'WAREHOUSE', '中央仓库'),
(2, 'WAREHOUSE', '冷藏仓库'),
(3, 'STORE', '门店A'),
(4, 'STORE', '门店B');

-- 鸡翅的库存角色配置
INSERT INTO stockable_role_config (material_id, location_id, safety_stock, max_stock) VALUES
(1002, 1, 100, 500),  -- 中央仓库：鸡翅可存储，安全库存100kg，最大500kg
(1002, 2, 20, 100),   -- 冷藏仓库：鸡翅可存储，安全库存20kg，最大100kg
(1002, 3, 10, 50),    -- 门店A：鸡翅可存储，安全库存10kg，最大50kg
(1002, 4, 0, 0);      -- 门店B：鸡翅不可存储，安全库存0，最大0

-- 库存实例
INSERT INTO inventory_instance (material_id, location_id, current_stock) VALUES
(1002, 1, 200),       -- 中央仓库：当前库存200kg
(1002, 2, 50),        -- 冷藏仓库：当前库存50kg
(1002, 3, 20);        -- 门店A：当前库存20kg
-- 门店B：无库存记录（max_stock=0 表示不可存储）
```

**结论**：目标模型可以正确表达"鸡翅在不同位置有不同的库存能力"。

---

## 9. 三维度分离

### 9.1 CURRENT REALITY（当前现实）

| 维度 | 现状 | 问题 |
|------|------|------|
| Stockable 能力模型 | 隐含在 inventory / store_inventory 表中 | 无显式建模 |
| 作用域 | 双维度（warehouse_id + store_id） | 无统一位置模型 |
| 配置管理 | 分散在多表 | 安全库存、保质期等配置不统一 |

### 9.2 TARGET BUSINESS SEMANTICS（目标业务语义）

| 维度 | 目标 | 理由 |
|------|------|------|
| Stockable 能力模型 | **显式 Role 建模** | 库存能力是 Material 的一个 Business Role |
| 作用域 | **LOCATION-SCOPED** | 统一管理仓库和门店的库存能力 |
| 配置管理 | **统一配置表** | 安全库存、保质期、存储条件统一管理 |

### 9.3 TARGET TECHNICAL MODEL（目标技术模型）

```
material_archives (Canonical Identity)
       │
       └── stockable_role_config (LOCATION-SCOPED Role Config)
              │
              ├── inventory_instance (仓库维度实例)
              └── inventory_instance (门店维度实例)
```

---

## 10. 遗留问题与技术债

### 10.1 当前系统的隐含能力模型

| 问题 | 位置 | 建议 |
|------|------|------|
| Stockable 能力无显式建模 | 无独立表 | 创建 stockable_role_config 表 |
| 位置模型不统一 | warehouse_id + store_id | 引入统一 location 表 |
| 配置分散在多表 | inventory / store_inventory / material_archives | 统一到 stockable_role_config |

### 10.2 迁移路径

| 阶段 | 任务 | 风险 |
|------|------|------|
| 短期 | 创建 stockable_role_config 表，从现有表回填数据 | 低 |
| 中期 | 引入统一 location 表，迁移 warehouse_id 和 store_id | 中 |
| 长期 | 合并 inventory 和 store_inventory，统一为 inventory_instance | 高 |

---

## 11. 推荐改进方向

### 11.1 短期（保持现有结构）

1. **创建 stockable_role_config 表**：将分散在 inventory / store_inventory 的预警阈值、存储条件统一管理
2. **从现有表回填数据**：将 inventory.safety_stock / store_inventory.safety_stock 回填到 stockable_role_config

### 11.2 中期（引入统一位置模型）

1. **创建 location 表**：将 warehouse 和 store 统一为 location
2. **迁移 warehouse_id 和 store_id**：将 inventory.warehouse_id 和 store_inventory.store_id 迁移为 location_id

### 11.3 长期（统一库存模型）

1. **合并 inventory 和 store_inventory**：统一为 inventory_instance 表
2. **引入统一的库存流水**：将 inventory_transactions 扩展为支持统一位置模型

---

## 12. 总结

| 问题 | 结论 |
|------|------|
| STOCKABLE 是什么？ | 是 Material 的一个 **Business Role**（角色），表示"可以被存储" |
| Stockable 作用域？ | **LOCATION-SCOPED**（位置级），统一管理仓库和门店的库存能力 |
| 推荐建模范式？ | **Role**（角色），携带业务语义，可以有配置，可以有多实例 |
| 当前系统现实？ | 已隐含位置级的库存能力模型，但没有显式建模 |
| 目标模型？ | 显式 Role 建模 + LOCATION-SCOPED 作用域 + 统一位置模型 |

---

**状态**：ANALYSIS, NOT CONFIRMED
**日期**：2026-09-10
**阶段**：Phase 3 - Stockable Capability Analysis
