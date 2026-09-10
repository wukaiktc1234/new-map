# Phase 7 - Stockable 能力分析

> **核心问题**: STOCKABLE 是 Role、Capability、Profile 还是 State?
> **状态**: ANALYSIS
> **关联文档**: concept-taxonomy.md, inventory-stockable-model.md, store-scope-analysis.md, type-vs-role-analysis.md

---

## 1. 问题域

### 1.1 核心问题

1. Coca-Cola 在 Store A → Stockable, Store B → Non-Stockable — 这意味着什么?
2. Central Warehouse → Stockable, Store → Stockable — 同一物料在不同位置的库存能力是否相同?
3. 不同 Scope（Global / Store / Warehouse / Location）下是否可以不同?

### 1.2 分析原则

- **CURRENT REALITY**: 当前数据库表结构和代码实现
- **TARGET BUSINESS SEMANTICS**: 目标业务语义模型
- **TARGET TECHNICAL MODEL**: 目标技术实现模型
- **严格区分**: Type, Role, Capability, Relationship, State, Profile — 不得混淆

---

## 2. 当前现实 (CURRENT REALITY)

### 2.1 库存相关表结构

```
inventory (仓库库存):
├── inventory_id (PK)
├── material_id (FK → material_archives)
├── warehouse_id
├── current_stock, locked_quantity
├── unit_cost, total_cost
├── batch_no, production_date, expiry_date
└── min_safe_qty, max_stock_qty

store_inventory (门店库存):
├── id (PK)
├── store_id
├── material_id (FK → material_archives)
├── current_stock, unit_cost, total_cost
└── safety_stock, max_stock

inventory_transactions (库存流水):
├── transaction_id (PK)
├── material_id (冗余)
├── inventory_id (FK → inventory)
├── warehouse_id
├── quantity_change, before_qty, after_qty
├── unit_cost, total_cost
└── transaction_type: 1采购入库 2销售出库 3调拨出 4调拨入 5盘点盈 6盘点亏 7报损 8退货
```

### 2.2 关键事实

| 事实 | 证据 | 影响 |
|------|------|------|
| 库存管理粒度是 `material_id` | `inventory.material_id`, `store_inventory.material_id` | 库存项 = material 的某位置实例 |
| 仓库库存和门店库存完全独立 | `inventory` 用 `warehouse_id`，`store_inventory` 用 `store_id`，两表无外键 | 存在双维度库存模型 |
| 门店经营范围目前全局统一 | 不存在门店级菜品配置 | 无法区分"门店 A 卖可乐，门店 B 不卖可乐" |
| 无 `material_stockable_config` 表 | 安全库存分散在 inventory / store_inventory | 没有独立的 Stockable 能力配置 |

### 2.3 当前系统的隐含假设

当前系统隐含假设：**只要在 material_archives 中创建了物料，它就是 Stockable 的。** 没有任何机制来禁用某个物料在特定位置的库存能力。

---

## 3. Stockable 的本质是什么？

### 3.1 候选模型分析

#### 模型 A: Stockable 作为 TYPE

```
Material ← TYPE(Stockable) → "这个东西是可存储的"
```

**支持证据**:
- concept-taxonomy.md 中将 Stockable 分类为 CAPABILITY + SCOPE
- 业务上，可乐是"可存储的"，宫保鸡丁是"不可存储的"

**反对证据**:
- 如果 Stockable 是 Type，那么同一物料在不同门店不能有不同的 Stockable 状态
- 可乐在 Store A 可能是 Stockable（有库存），在 Store B 可能不是（不采购）
- Type 是固有的、不可变的；但 Stockable 是可以按门店配置的

**结论**: ❌ Stockable 不是 Type

#### 模型 B: Stockable 作为 ROLE

```
Material ← ROLE(Stockable) → "这个东西在库存场景中扮演什么角色"
```

**支持证据**:
- `type-vs-role-analysis.md` 已经确认: Type 和 Role 应该分离
- Role 可以按门店独立配置（千店千面）
- business-role-model.md 将 STOCKABLE 定义为 Business Role
- inventory-stockable-model.md 推荐: Stockable 是 Material 的 Business Role

**反对证据**:
- Role 通常描述"做什么"（如 INGREDIENT = 在 Recipe 中作为原料）
- 但 Stockable 描述的是"能被存储"，更像是一个能力声明

**结论**: ⚠️ 接近但不精确 — Role 捕获了"场景行为"，但 Stockable 更像是一种"能力声明"

#### 模型 C: Stockable 作为 CAPABILITY

```
Material ← CAPABILITY(Stockable) → "这个东西具备被存储的能力"
```

**支持证据**:
- concept-taxonomy.md 将 Stockable 分类为 CAPABILITY
- 能力可以按门店配置：同一物料在不同门店可以有不同的能力
- 能力是一种声明，不一定是持续的行为

**反对证据**:
- 如果是纯 Capability，那么需要一个"实例化"来表示"正在被库存管理"
- 当前的 inventory / store_inventory 就是 Capability 的实例化

**结论**: ✅ Stockable 的核心是 CAPABILITY，但需要 Role 来承载配置数据

#### 模型 D: Stockable 作为 STATE

```
Material ← STATE(Stockable) → "这个东西当前正在被存储"
```

**支持证据**:
- inventory 表记录了当前库存状态（current_stock）
- 可以有状态转换：从"在库"到"已出库"

**反对证据**:
- State 是临时的、可变的；Stockable 是持久的能力声明
- 一个物料的 Stockable 能力不因库存为零而消失
- 状态是实例级的，能力是定义级的

**结论**: ❌ Stockable 不是 State

#### 模型 E: Stockable 作为 PROFILE

```
Material ← PROFILE(StockableConfig) → "这个东西的存储配置信息"
```

**支持证据**:
- 库存配置数据（安全库存、最大库存、保质期）确实是 Profile
- 当前分散在 inventory / store_inventory / material_archives 中

**反对证据**:
- Profile 描述"是什么样子"，不是"能做什么"
- 安全库存、最大库存是 Stockable 能力的**配置参数**，不是能力本身

**结论**: ⚠️ Profile 是 Stockable 的配置数据，不是 Stockable 本身

### 3.2 综合结论

**Stockable = CAPABILITY（能力声明）+ ROLE（场景角色）+ PROFILE（配置数据）**

```
Stockable 能力模型:
┌─────────────────────────────────────────────────────────┐
│                    Material (Canonical Item)              │
│                                                         │
│  capability声明:                                         │
│    stockable: boolean  ← "我具备被存储的能力"              │
│                                                         │
│  role配置 (per scope):                                   │
│    ├── Global Scope:                                    │
│    │   role_type: STOCKABLE                              │
│    │   config: { shelf_life, storage_condition }        │
│    │                                                    │
│    ├── Warehouse Scope:                                 │
│    │   role_type: STOCKABLE                              │
│    │   config: { min_safe_qty, max_stock_qty }          │
│    │                                                    │
│    └── Store Scope:                                     │
│        role_type: STOCKABLE                              │
│        config: { safety_stock, max_stock }              │
│                                                         │
│  实例数据 (Inventory Instance):                           │
│    ├── inventory(material_id, warehouse_id)              │
│    └── store_inventory(material_id, store_id)            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**关键区分**:

| 层次 | 概念 | 说明 | 示例 |
|------|------|------|------|
| **Capability** | stockable = true/false | "我能不能被存储" | 可乐: true, 宫保鸡丁: false |
| **Role Config** | StockableRole per scope | "在这个范围内怎么存储" | Store A: safety_stock=10, Store B: safety_stock=5 |
| **Instance** | inventory / store_inventory | "实际有多少库存" | Store A: current_stock=100 |

---

## 4. 范围维度分析: Global / Store / Warehouse / Location

### 4.1 四层范围模型

```
┌─────────────────────────────────────────────────────────────┐
│  Layer 1: Global Scope (全局)                               │
│  "可乐能不能被库存管理？"                                     │
│  答案: stockable = true (全局声明)                           │
│                                                             │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Layer 2: Warehouse Scope (仓库)                        │ │
│  │  "可乐在中央仓库怎么管理？"                               │ │
│  │  答案: safety_stock=50, max_stock=500, shelf_life=180d  │ │
│  │                                                         │ │
│  │  ┌─────────────────────────────────────────────────────┐ │ │
│  │  │  Layer 3: Store Scope (门店)                        │ │ │
│  │  │  "可乐在 Store A 怎么管理？"                         │ │ │
│  │  │  答案: safety_stock=10, max_stock=50, stockable=true │ │ │
│  │  │                                                     │ │ │
│  │  │  "可乐在 Store B 怎么管理？"                         │ │ │
│  │  │  答案: stockable=false (门店 B 不存储可乐)            │ │ │
│  │  └─────────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 业务案例验证

#### 案例 1: Coca-Cola 在不同门店的 Stockable 差异

**场景**:
- Store A（堂食店）: 可乐作为饮品直接销售，需要库存
- Store B（酒店迷你吧）: 可乐只作为赠品存储，不独立销售

**分析**:

| 维度 | Store A | Store B | 说明 |
|------|---------|---------|------|
| Global Capability | stockable=true | stockable=true | 可乐全局可存储 |
| Store Config | is_stockable=true | is_stockable=true | 两个门店都存储 |
| Stock Role | SELLABLE + STOCKABLE | STOCKABLE (不销售) | 角色不同 |
| Safety Stock | 10瓶 | 2瓶 | 配置不同 |
| Max Stock | 50瓶 | 5瓶 | 配置不同 |

**结论**: 同一物料在不同门店可以有不同的 Stockable 配置，但 Capability（能力声明）是全局的。

#### 案例 2: Central Warehouse vs Store 的 Stockable

**场景**:
- Central Warehouse: 大批量存储所有采购物料
- Store: 只存储当天需要的少量物料

**分析**:

| 维度 | Central Warehouse | Store | 说明 |
|------|-------------------|-------|------|
| Global Capability | stockable=true | stockable=true | 大米全局可存储 |
| Scope Config | safety_stock=200kg | safety_stock=5kg | 配置差异巨大 |
| Batch Management | 严格（先进先出） | 宽松 | 管理粒度不同 |
| Cost Calculation | 加权平均法 | 简化 | 核算方式不同 |

**结论**: Warehouse 和 Store 是同一 Capability 的不同 Scope 配置。

#### 案例 3: 同一物料在不同 Location 的 Stockable

**场景**:
- Warehouse A（常温库）: 存储大米、酱油等常温物料
- Warehouse B（冷藏库）: 存储生鲜、乳制品等冷藏物料

**分析**:

| 物料 | Warehouse A (常温) | Warehouse B (冷藏) |
|------|--------------------|--------------------|
| 大米 | Stockable ✅ | Stockable ❌ (不适合冷藏) |
| 生鲜蔬菜 | Stockable ❌ (不适合常温) | Stockable ✅ |
| 可乐 | Stockable ✅ | Stockable ✅ (均可) |

**结论**: Location 的物理属性（温度、湿度）会影响 Stockable 的适用性。

---

## 5. Stockable 与 Type/Role/Capability 的严格区分

### 5.1 分类矩阵

| 概念 | 分类 | 定义 | 判断标准 | 示例 |
|------|------|------|----------|------|
| **Food** | TYPE | 可直接销售的食品 | 与生俱来，不可变 | 宫保鸡丁 = Food |
| **Material** | TYPE | 可采购/存储的物料 | 与生俱来，不可变 | 大米 = Material |
| **SELLABLE** | ROLE | 在销售场景中可卖 | 可按门店配置 | 可乐在门店A可卖 |
| **PURCHASABLE** | ROLE | 在采购场景中可买 | 可按门店配置 | 可乐可从供应商采购 |
| **STOCKABLE** | CAPABILITY + ROLE | 能被存储 + 在库存场景中管理 | Capability全局，Role按Scope配置 | 可乐能被库存管理 |
| **INGREDIENT** | ROLE | 在 Recipe 中作为原料 | 可按 Recipe 配置 | 可乐在可乐鸡翅中 |
| **CONSUMABLE** | CAPABILITY | 能被消耗 | 与生俱来 | 大米能被消耗 |

### 5.2 关键区别: Capability vs Role

**Capability（能力）**= "我**能**做什么"
- 是全局声明
- 是布尔值（能/不能）
- 变更频率低
- 示例: 可乐 stockable = true

**Role（角色）**= "我在这个场景中**做**什么"
- 是场景级配置
- 是配置集（包含多个参数）
- 变更频率中
- 示例: 可乐在 Store A 的 StockableRole = { safety_stock: 10, max_stock: 50 }

### 5.3 关键区别: Stockable vs Stock Item

| 概念 | 层次 | 说明 | 示例 |
|------|------|------|------|
| **Stockable** | Capability/Role | "能被库存管理"的声明和配置 | 可乐 stockable=true |
| **Stock Item** | Entity/Instance | "正在被库存管理"的具体记录 | 可乐在仓库A有100瓶 |

---

## 6. 真实案例验证

### 案例 A: 可乐的完整 Stockable 生命周期

```
1. 创建物料
   material_archives: 可乐, material_id=M001
   capability: stockable = true

2. 配置 Global Stockable Role
   stockable_config: shelf_life=180天, storage_condition=常温

3. 配置 Warehouse Stockable Role
   warehouse_stockable: warehouse_id=W01, safety_stock=50, max_stock=500

4. 配置 Store Stockable Role
   store_stockable: store_id=STORE_A, safety_stock=10, max_stock=50
   store_stockable: store_id=STORE_B, stockable=false (不存储)

5. 实例化库存
   inventory: material_id=M001, warehouse_id=W01, current_stock=200
   store_inventory: store_id=STORE_A, material_id=M001, current_stock=20

6. 库存变动
   inventory_transactions: type=1(采购入库), quantity=+100
   inventory_transactions: type=4(调拨入), quantity=+20 (从仓库到门店)
```

### 案例 B: 宫保鸡丁的 Stockable 分析

```
1. 创建物料
   foods: 宫保鸡丁, food_id=F001

2. Capability 声明
   stockable = false ← 现做现卖，不库存

3. Role 配置
   无需 Stockable Role 配置

4. 实例数据
   inventory: 无记录
   store_inventory: 无记录

5. 验证
   即使宫保鸡丁的 stock 字段有值，那也是"可售量"（下单即扣），不是实物库存
```

### 案例 C: 餐盒的多 Scope Stockable

```
1. 创建物料
   material_archives: 一次性餐盒, material_id=M010

2. Capability 声明
   stockable = true

3. 多 Scope 配置
   Warehouse: safety_stock=1000个, max_stock=10000个
   Store_A (外卖店): safety_stock=200个, max_stock=500个
   Store_B (堂食店): stockable=false (不需要餐盒)

4. 库存变动
   Warehouse: 采购入库 +5000
   Store_A: 调拨入 +300
   Store_B: 无
```

---

## 7. 目标技术模型 (TARGET TECHNICAL MODEL)

### 7.1 Stockable 能力模型

```sql
-- 全局 Stockable 能力声明
CREATE TABLE material_capability (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    capability_type VARCHAR(50) NOT NULL,  -- STOCKABLE, PURCHASABLE, SELLABLE, CONSUMABLE
    enabled BOOLEAN DEFAULT TRUE,
    config JSON,                           -- 能力特定配置
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(material_id, capability_type)
);

-- Scope 级 Stockable Role 配置
CREATE TABLE stockable_role_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    material_id BIGINT NOT NULL,
    scope_type VARCHAR(20) NOT NULL,       -- GLOBAL, WAREHOUSE, STORE, LOCATION
    scope_id VARCHAR(50),                  -- warehouse_id / store_id / location_id
    enabled BOOLEAN DEFAULT TRUE,
    safety_stock DECIMAL(10,2),
    max_stock DECIMAL(10,2),
    shelf_life_days INT,
    storage_condition VARCHAR(100),
    cost_method VARCHAR(20) DEFAULT 'WEIGHTED_AVG',  -- WEIGHTED_AVG, FIFO, LIFO
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(material_id, scope_type, scope_id)
);
```

### 7.2 与现有表的关系

```
material_archives (Canonical Item)
    │
    ├── material_capability (能力声明)
    │   material_id + capability_type = 'STOCKABLE'
    │   enabled = true/false
    │
    ├── stockable_role_config (Scope 配置)
    │   material_id + scope_type + scope_id
    │   safety_stock, max_stock, shelf_life
    │
    ├── inventory (仓库实例)
    │   material_id + warehouse_id
    │   current_stock = 实际库存量
    │
    └── store_inventory (门店实例)
        material_id + store_id
        current_stock = 实际库存量
```

### 7.3 查询模式

```sql
-- 查询门店 A 的可库存物料
SELECT ma.*, src.safety_stock, src.max_stock
FROM material_archives ma
JOIN material_capability mc ON ma.material_id = mc.material_id
JOIN stockable_role_config src ON ma.material_id = src.material_id
WHERE mc.capability_type = 'STOCKABLE'
  AND mc.enabled = TRUE
  AND src.scope_type = 'STORE'
  AND src.scope_id = 'STORE_A'
  AND src.enabled = TRUE;

-- 查询门店 A 的实际库存
SELECT si.*, ma.material_name
FROM store_inventory si
JOIN material_archives ma ON si.material_id = ma.material_id
WHERE si.store_id = 'STORE_A'
  AND si.current_stock > 0;
```

---

## 8. 与现有决策的关系

### 8.1 与 PD-022 (千店千面) 的关系

PD-022 要求支持门店差异化。Stockable 的 Scope 配置是 PD-022 的核心实现:
- 全局声明: 可乐 stockable=true
- 门店配置: Store A stockable=true, Store B stockable=false

### 8.2 与 PD-030 (库存扣减分层) 的关系

PD-030 区分了"可售量"和"实物量":
- `item_store_config.is_stockable` + `store_inventory.current_stock` = 实物量
- `foods.stock` + `item_store_config.is_sellable` = 可售量

### 8.3 与 PADR-002 (门店经营范围) 的关系

PADR-002 定义了门店经营范围。Stockable 的 Store Scope 配置是 PADR-002 的数据模型:
- `stockable_role_config` where scope_type='STORE' + enabled=true = 门店可存储物料

---

## 9. 结论

### 9.1 回答核心问题

| 问题 | 结论 |
|------|------|
| STOCKABLE 是 Role、Capability、Profile 还是 State? | **Capability（能力声明）+ Role（Scope 级配置）+ Profile（配置数据）** |
| Coca-Cola 在 Store A → Stockable, Store B → Non-Stockable? | **全局 Capability=true，Store Scope Role 可以 enabled=false** |
| Central Warehouse vs Store 的 Stockable? | **同一 Capability 的不同 Scope 配置** |
| 不同 Scope 下是否可以不同? | **是。Capability 全局统一，Role 配置按 Scope 独立** |

### 9.2 分层总结

| 层次 | 概念 | 变更频率 | 示例 |
|------|------|----------|------|
| **Capability** | stockable=true/false | 极低（物料创建时确定） | 可乐能被库存管理 |
| **Role Config** | safety_stock, max_stock 等 | 中（业务变化时调整） | Store A: safety_stock=10 |
| **Instance** | inventory.current_stock | 高（每次出入库变动） | Store A: current_stock=20 |

### 9.3 为什么不能把 Stockable 混为其他概念

1. **不是 Type**: Type 是固有的（可乐永远是 Material），Stockable 是可配置的（可乐在 Store B 可以不 Stockable）
2. **不是 State**: State 是临时的（库存为零不意味着不可存储），Stockable 是持久的能力
3. **不是纯 Profile**: Profile 描述"是什么样子"，Stockable 声明"能做什么"
4. **不是纯 Role**: Role 描述"做什么"，Stockable 首先声明"能做什么"，然后在 Role 中配置"怎么做"

---

**分析完成时间**: 2026-09-09
**分析结论**: Stockable = CAPABILITY（全局能力声明）+ ROLE（Scope 级配置）+ PROFILE（配置数据）
**推荐行动**: 新增 material_capability 表和 stockable_role_config 表，实现 Scope 级 Stockable 配置
