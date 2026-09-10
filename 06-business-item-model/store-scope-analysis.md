# 门店范围 / 角色分析（Store Scope & Role）

> **文档类型**: 门店维度业务角色分析  
> **生成日期**: 2026-09-09  
> **状态**: ANALYSIS  
> **范围**: 门店级角色配置、Store-scoped Role、经营范围与库存分离  
> **关联决策**: PD-022 (千店千面), PADR-002 (门店经营范围), PD-030 (库存扣减分层)

---

## 一、现状事实（代码审计）

### 1.1 门店库存表（store_inventory）

```sql
CREATE TABLE store_inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id VARCHAR NOT NULL,        -- 门店ID
    material_id BIGINT NOT NULL,      -- 物料ID
    material_name VARCHAR,            -- 物料名称（冗余）
    current_stock DECIMAL(10,2),      -- 当前库存
    unit VARCHAR(32),                 -- 单位
    safety_stock DECIMAL(10,2),       -- 安全库存
    max_stock DECIMAL(10,2),          -- 最大库存
    unit_cost BIGINT,                 -- 单位成本（分）
    total_cost BIGINT,                -- 总成本（分）
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT,
    version INT,                      -- 乐观锁
    PRIMARY KEY (store_id, material_id)
);
```

**关键事实**:
- `store_inventory` 以 `store_id + material_id` 为唯一键
- 同一 `material_id` 在不同门店有独立的库存记录
- 门店库存和仓库库存（`inventory`）是**两个独立维度**

### 1.2 门店库存与仓库库存的关系

```
┌─────────────────────────────────────────────────────────┐
│                    库存双维度模型                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  material_archives (物料)                                │
│       │                                                 │
│       ├── inventory (仓库库存)                           │
│       │   material_id + warehouse_id                     │
│       │   仓库级: 批次管理、成本核算                      │
│       │                                                 │
│       └── store_inventory (门店库存)                     │
│           material_id + store_id                         │
│           门店级: 日常消耗、补货需求                       │
│                                                         │
│  两个维度独立管理，通过调拨/补货关联                       │
└─────────────────────────────────────────────────────────┘
```

### 1.3 当前门店级业务对象

| 业务对象 | 表 | 门店维度字段 | 说明 |
|----------|-----|-------------|------|
| 门店库存 | store_inventory | store_id | 门店物料库存 |
| 门店订单 | orders | store_id | 门店销售订单 |
| 门店员工 | users | store_id | 员工绑定门店 |
| 门店资产 | asset_master | store_id | 门店固定资产 |
| 门店日结 | (待建) | store_id | 门店日结对账 |

### 1.4 门店经营范围现状

根据 `system-truth-source-map.md:37`:

> **门店经营范围**: 全局统一菜单，无门店维度。  
> **PADR-002**: 正式概念，演进实现。  
> **状态**: 不存在（演进 PADR-002）

**当前现实**:
- 菜品（foods）是全局统一的，所有门店共享同一份菜单
- 不存在"门店 A 卖可乐，门店 B 不卖可乐"的配置
- `foods.status` 控制全局发布状态，不控制门店级可见性

---

## 二、核心问题分析

### 2.1 同一个 Canonical Item 在不同门店是否可以拥有不同 Role？

#### 场景分析

| 场景 | 门店 A | 门店 B | 是否应该允许？ |
|------|--------|--------|---------------|
| 可乐 | Sellable + Stockable | Purchasable + Stockable（不销售） | **是** — 酒店房间迷你吧只存储不销售 |
| 生鲜蔬菜 | Stockable | 不采购（中央厨房统一配送） | **是** — 不同门店采购策略不同 |
| 餐盒 | Stockable | 不使用（只做堂食） | **是** — 外卖店和堂食店需求不同 |
| 特色菜品 | Sellable | 不上架（区域口味差异） | **是** — 千店千面的核心需求 |

**结论**: **应该允许。** 同一物料在不同门店可以有不同的业务角色。

#### 角色组合示例

| 门店类型 | 可乐角色 | 生鲜角色 | 餐盒角色 |
|----------|----------|----------|----------|
| 堂食店 | Sellable + Stockable | Stockable + Ingredient | Stockable |
| 外卖店 | Sellable + Stockable | Stockable + Ingredient | Stockable + Consumable |
| 酒店迷你吧 | Stockable（不销售） | 不采购 | 不使用 |
| 中央厨房配送店 | Stockable（接收配送） | Stockable（接收配送） | Stockable |

---

### 2.2 Role 是 Global 还是 Store-scoped？

#### 选项 A：Global Role（全局角色）

```
canonical_item: 可乐
  roles: [SELLABLE, STOCKABLE, PURCHASABLE]  ← 全局定义
  
所有门店: 可乐 = SELLABLE + STOCKABLE + PURCHASABLE
```

**优点**:
- 简单，角色定义一次
- 全局一致

**缺点**:
- 无法支持"门店 B 不卖可乐"的场景
- 违背 PD-022 千店千面方向

#### 选项 B：Store-scoped Role（门店级角色）

```
canonical_item: 可乐
  global_roles: [STOCKABLE, PURCHASABLE]  ← 全局基础角色
  
store_role:
  store_a: [SELLABLE]  ← 门店 A 额外标记为可销售
  store_b: []          ← 门店 B 不标记（不销售）
```

**优点**:
- 支持门店差异化
- 符合 PD-022 千店千面方向
- 全局角色 + 门店覆盖，灵活度高

**缺点**:
- 复杂度增加
- 需要维护 store_role 表

#### 选项 C：混合模式（推荐）

```
canonical_item: 可乐
  global_type: MATERIAL           ← 全局类型（不可变）
  
item_store_config:
  store_a: {
    is_sellable: true,            ← 门店级销售配置
    is_purchasable: true,
    is_stockable: true,
    sell_price: 3.00,             ← 门店级价格
    stock_unit: '瓶'
  }
  store_b: {
    is_sellable: false,           ← 门店 B 不销售
    is_purchasable: true,
    is_stockable: true
  }
```

**优点**:
- 全局类型不变（可乐永远是"物料"）
- 门店级配置灵活（卖不卖、怎么卖）
- 价格、单位等可门店级定制

**缺点**:
- 需要新增 item_store_config 表
- 查询逻辑复杂度增加

---

### 2.3 门店经营范围与库存的分离

根据 `system-conflict-map.md:C-06`:

> **C-06 门店经营范围**: 库存 ≠ 经营范围；不把库存当替代物。  
> **裁决**: PADR-002（D2）

**核心原则**:

```
门店经营范围 ≠ 门店库存
```

| 概念 | 定义 | 管理方式 |
|------|------|----------|
| **门店经营范围** | 门店**可以**经营哪些菜品/物料 | 门店级配置（is_sellable, is_purchasable） |
| **门店库存** | 门店**实际**有多少库存 | store_inventory 表（实时数量） |

**分离的理由**:
1. 经营范围是**配置**，库存是**事实** — 两者生命周期不同
2. 经营范围变更不影响库存数量（如"下架可乐"≠"清空可乐库存"）
3. 经营范围是**门店级**，库存是**门店+物料级** — 维度不同

---

## 三、建议的领域模型

### 3.1 Item Store Config（门店级物料配置）

```sql
CREATE TABLE item_store_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id VARCHAR NOT NULL,           -- 门店ID
    material_id BIGINT NOT NULL,         -- 物料ID
    is_sellable BOOLEAN DEFAULT FALSE,   -- 是否可销售
    is_purchasable BOOLEAN DEFAULT TRUE, -- 是否可采购
    is_stockable BOOLEAN DEFAULT TRUE,   -- 是否可存储
    sell_price BIGINT,                   -- 门店级销售价（分）
    purchase_price BIGINT,               -- 门店级采购价（分）
    stock_unit VARCHAR(20),              -- 门店库存单位
    sell_unit VARCHAR(20),               -- 门店销售单位
    min_stock DECIMAL(10,2),             -- 门店最低库存
    max_stock DECIMAL(10,2),             -- 门店最高库存
    status INT DEFAULT 1,               -- 状态
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(store_id, material_id)
);
```

**数据示例**:

| store_id | material_id | is_sellable | is_purchasable | sell_price | stock_unit |
|----------|-------------|-------------|----------------|------------|------------|
| STORE_A | 可乐(1) | TRUE | TRUE | 300 | 瓶 |
| STORE_B | 可乐(1) | FALSE | TRUE | NULL | 箱 |
| STORE_A | 生鲜(2) | FALSE | TRUE | NULL | 斤 |
| STORE_B | 生鲜(2) | FALSE | FALSE | NULL | NULL |

### 3.2 与现有表的关系

```
┌─────────────────────────────────────────────────────────┐
│                    门店级业务模型                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  canonical_item (物料)                                   │
│       │                                                 │
│       ├── item_store_config (门店级配置)                 │
│       │   store_id + material_id                         │
│       │   is_sellable / is_purchasable / is_stockable    │
│       │                                                 │
│       ├── store_inventory (门店库存)                     │
│       │   store_id + material_id                         │
│       │   current_stock / unit / unit_cost               │
│       │                                                 │
│       └── orders → order_items (门店订单)                │
│           store_id                                       │
│           material_id (通过 food 关联)                   │
│                                                         │
│  item_store_config 控制"能不能做"                        │
│  store_inventory 控制"有多少"                            │
│  orders 控制"做了多少"                                   │
└─────────────────────────────────────────────────────────┘
```

---

## 四、门店级角色的实现方案

### 4.1 方案 A：扩展 store_inventory 表（最小改动）

在 `store_inventory` 表中增加角色字段：

```sql
ALTER TABLE store_inventory ADD COLUMN is_sellable BOOLEAN DEFAULT FALSE;
ALTER TABLE store_inventory ADD COLUMN is_purchasable BOOLEAN DEFAULT TRUE;
ALTER TABLE store_inventory ADD COLUMN sell_unit VARCHAR(20);
```

**优点**: 改动最小，store_inventory 已经有 store_id + material_id
**缺点**: store_inventory 是库存事实表，不应承载配置职责

### 4.2 方案 B：新增 item_store_config 表（推荐）

创建独立的门店级配置表，store_inventory 保持纯粹的库存事实。

**优点**: 职责分离清晰，扩展性好
**缺点**: 需要新增表，查询需要 JOIN

### 4.3 方案 C：复用 foods 表 + store_id 维度（不推荐）

在 foods 表中增加 store_id 字段，使每个门店有自己的菜品副本。

**优点**: 不需要新表
**缺点**: 数据冗余严重，foods 表膨胀，违背 PD-022 方向

---

## 五、与现有决策的关系

### 5.1 PD-022 千店千面

> **决策**: 集团统一 + 门店差异化为方向；集团发布 ≠ 门店销售。  
> **状态**: 已决（方向确认），实现=演进

本分析的 `item_store_config` 表是 PD-022 的**实现载体**：
- 集团统一: `canonical_item` 定义物料基础信息
- 门店差异化: `item_store_config` 定义门店级配置

### 5.2 PADR-002 门店经营范围

> **决策**: 门店经营范围是正式概念，演进实现。  
> **状态**: 已决概念，实现方案=演进

本分析的 `is_sellable / is_purchasable / is_stockable` 字段是 PADR-002 的**数据模型**。

### 5.3 PD-030 库存扣减分层

> **决策**: 菜品层可售量=下单即扣，物料层实物=出餐按BOM扣。  
> **状态**: 已决

本分析的 `store_inventory` 和 `item_store_config` 分别对应：
- `store_inventory.current_stock` = 门店物料实物量（出餐按BOM扣）
- `item_store_config.is_sellable` + `foods.stock` = 门店可售量（下单即扣）

---

## 六、查询场景分析

### 6.1 POS 收银：查询门店可售菜品

```sql
-- 当前: 全局查询
SELECT * FROM foods WHERE status = 1;

-- 目标: 门店级查询
SELECT f.* FROM foods f
JOIN item_store_config isc ON f.material_id = isc.material_id
WHERE isc.store_id = 'STORE_A'
  AND isc.is_sellable = TRUE
  AND f.status = 1;
```

### 6.2 采购：查询门店可采购物料

```sql
SELECT ma.* FROM material_archives ma
JOIN item_store_config isc ON ma.material_id = isc.material_id
WHERE isc.store_id = 'STORE_A'
  AND isc.is_purchasable = TRUE
  AND ma.status = 1;
```

### 6.3 库存：查询门店库存（仅可存储物料）

```sql
SELECT si.*, ma.material_name, ma.material_code
FROM store_inventory si
JOIN item_store_config isc ON si.store_id = isc.store_id AND si.material_id = isc.material_id
JOIN material_archives ma ON si.material_id = ma.material_id
WHERE si.store_id = 'STORE_A'
  AND isc.is_stockable = TRUE
  AND si.current_stock > 0;
```

---

## 七、决策建议

### 7.1 短期方案（0-3 个月）

| 动作 | 说明 | 优先级 |
|------|------|--------|
| 新增 item_store_config 表 | 门店级物料配置（is_sellable, is_purchasable, is_stockable） | P1 |
| POS 查询改造 | POS 收银查询改为门店级过滤 | P1 |
| 采购查询改造 | 采购列表改为门店级过滤 | P2 |

### 7.2 中期方案（3-6 个月）

| 动作 | 说明 | 优先级 |
|------|------|--------|
| 门店级价格配置 | item_store_config 增加 sell_price, purchase_price | P2 |
| 门店级单位配置 | item_store_config 增加 stock_unit, sell_unit | P2 |
| 门店级库存预警 | item_store_config 增加 min_stock, max_stock | P3 |

### 7.3 长期方案（6-12 个月）

| 动作 | 说明 |
|------|------|
| 门店级菜单管理 | 支持门店自定义菜品上架/下架 |
| 门店级促销配置 | 不同门店不同促销策略 |
| 区域级配置继承 | 区域默认配置 + 门店覆盖 |

---

## 八、总结

| 问题 | 结论 |
|------|------|
| 同一物料在不同门店是否可以有不同 Role？ | **是。** 这是千店千面的核心需求 |
| Role 是 Global 还是 Store-scoped？ | **混合模式。** 全局类型（Global）+ 门店配置（Store-scoped） |
| 门店经营范围与库存是否分离？ | **是。** 经营范围=配置，库存=事实，两者独立管理 |
| 应该新增 item_store_config 表吗？ | **是。** 它是 PD-022/PADR-002 的数据模型载体 |

---

*文档结束*
