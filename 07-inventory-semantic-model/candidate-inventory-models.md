# 候选库存模型比较 (Phase 22 - Candidate Inventory Models)

> **核心问题**：哪种库存模型最适合餐饮ERP的业务需求？
> **状态**：ANALYSIS, NOT CONFIRMED

---

## 1. 问题域

本分析必须回答：
1. 哪种库存模型最准确地表达餐饮ERP的业务语义？
2. 各模型在关键业务场景下的表现差异是什么？
3. 权重：Business Correctness > Long-term Model Integrity > Cross-Domain Consistency > Migration Cost

**关键约束**：
- Current DB Model ≠ Target Inventory Model
- 不要因为当前实现就认定未来模型
- 必须使用真实业务案例验证
- 必须证明为什么正确、在哪里不正确

---

## 2. 候选模型定义

### 2.1 Model A: Inventory → Material（当前实现）

**核心思想**：库存管理的实体是 Material（物料），以 `material_id` 为唯一标识。

```
┌─────────────────────────────────────────────────────────┐
│                    Model A: Material-Based               │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Material (material_archives)                           │
│    ├── material_id (PK)                                 │
│    ├── material_name                                    │
│    ├── unit (基础单位)                                   │
│    └── is_raw_material, is_sellable, ...                │
│                                                         │
│  Inventory (inventory / store_inventory)                │
│    ├── material_id (FK → Material)                      │
│    ├── warehouse_id / store_id                          │
│    ├── current_stock                                    │
│    └── unit_cost                                        │
│                                                         │
│  Stock Ledger (inventory_transactions)                  │
│    ├── material_id (FK → Material)                      │
│    ├── transaction_type                                 │
│    └── quantity_change                                  │
│                                                         │
│  Identity: material_id 是 Canonical Identity             │
│  Location: inventory + store_inventory 双表             │
│  UOM: 各表独立存储 unit 字段                             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**优点**：
- 当前系统已实现，迁移成本最低
- material_id 在所有活跃表中已作为外键
- 简单直观，适合当前业务规模

**缺点**：
- Material 无法表达多角色（可乐既是原料又是可售品）
- 门店/仓库双表维护成本高
- UOM 不统一，缺少转换机制
- 缺少 Canonical Item 概念

---

### 2.2 Model B: Inventory → Canonical Item

**核心思想**：库存管理的实体是 Canonical Item（规范标识），跨所有业务域唯一标识"这个东西是什么"。

```
┌─────────────────────────────────────────────────────────┐
│                Model B: Canonical Item-Based             │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Canonical Item                                         │
│    ├── item_id (PK)                                     │
│    ├── item_name                                        │
│    ├── item_type (RAW/SEMI/FINISHED/PACKAGED)          │
│    ├── primary_uom_id (FK → UOM)                       │
│    └── capabilities:                                    │
│        ├── is_sellable                                  │
│        ├── is_purchasable                               │
│        ├── is_stockable                                 │
│        └── is_recipe_component                          │
│                                                         │
│  Inventory Fact (unified)                               │
│    ├── item_id (FK → Canonical Item)                    │
│    ├── location_id (FK → Location)                      │
│    ├── quantity                                         │
│    ├── uom_id (FK → UOM)                               │
│    └── batch_id                                         │
│                                                         │
│  Stock Ledger (unified)                                 │
│    ├── item_id (FK → Canonical Item)                    │
│    ├── location_id                                      │
│    ├── movement_type                                    │
│    └── quantity                                         │
│                                                         │
│  Item Identity Map                                      │
│    ├── item_id (FK → Canonical Item)                    │
│    ├── domain (SALES/PURCHASE/RECIPE)                   │
│    └── domain_id (food_id / material_id)               │
│                                                         │
│  UOM + UOM Conversion                                   │
│    ├── uom_id, uom_name, category                       │
│    └── from_uom, to_uom, factor                        │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**优点**：
- 真正的跨域 Canonical Identity
- 多角色支持（Capability Model）
- 统一库存表，Location 维度化
- UOM 标准化

**缺点**：
- 需要 Item Identity Map 解决当前 food_id/material_id 分裂
- 迁移成本中等
- 需要重新设计所有库存相关查询

---

### 2.3 Model C: Inventory → Stockable Profile

**核心思想**：库存管理的实体是 Stockable Profile（库存配置），描述一个 Item 如何被库存管理。

```
┌─────────────────────────────────────────────────────────┐
│              Model C: Stockable Profile-Based            │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Stockable Profile                                      │
│    ├── profile_id (PK)                                  │
│    ├── item_id (FK → Canonical Item)                    │
│    ├── location_type (WAREHOUSE/STORE/KITCHEN)          │
│    ├── stocking_uom_id                                  │
│    ├── costing_method (FIFO/WEIGHTED_AVG/FIXED)        │
│    ├── min_stock_qty                                    │
│    ├── max_stock_qty                                    │
│    ├── reorder_point                                    │
│    ├── lead_time_days                                   │
│    ├── shelf_life_days                                  │
│    ├── storage_temp_range                               │
│    └── batch_required (boolean)                         │
│                                                         │
│  Stock Ledger (unified)                                 │
│    ├── profile_id (FK → Stockable Profile)              │
│    ├── movement_type                                    │
│    ├── quantity                                         │
│    └── cost                                             │
│                                                         │
│  Inventory Balance (derived from ledger)                │
│    ├── profile_id                                       │
│    ├── quantity_on_hand                                 │
│    ├── quantity_reserved                                │
│    └── quantity_in_transit                              │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**优点**：
- 库存配置与库存事实分离
- 支持不同 Location 的不同库存策略
- 天然支持多仓库、多门店的差异化管理
- Costing Method 可配置

**缺点**：
- Profile 概念增加了抽象层
- 迁移成本较高
- 当前业务可能不需要如此细粒度的配置

---

### 2.4 Model D: Inventory → Item + Location + Stock Ledger

**核心思想**：库存管理由三个正交维度组合：Item（是什么）+ Location（在哪里）+ Stock Ledger（变动历史）。

```
┌─────────────────────────────────────────────────────────┐
│           Model D: Item + Location + Stock Ledger        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Item (Canonical Identity)                              │
│    ├── item_id (PK)                                     │
│    ├── item_name, item_code                             │
│    ├── item_type                                        │
│    └── primary_uom_id                                   │
│                                                         │
│  Location (Canonical Identity)                          │
│    ├── location_id (PK)                                 │
│    ├── location_type (WAREHOUSE/STORE/KITCHEN)          │
│    ├── location_name, location_code                     │
│    └── parent_location_id (hierarchy)                   │
│                                                         │
│  Stock Ledger (Append-Only, Immutable)                  │
│    ├── entry_id (PK)                                    │
│    ├── item_id (FK → Item)                              │
│    ├── location_id (FK → Location)                      │
│    ├── movement_type                                    │
│    ├── quantity (positive=in, negative=out)             │
│    ├── uom_id                                           │
│    ├── unit_cost                                        │
│    ├── batch_id                                         │
│    ├── reference_type (PO/ORDER/TRANSFER/ADJUSTMENT)    │
│    ├── reference_id                                     │
│    ├── created_at                                       │
│    └── created_by                                       │
│                                                         │
│  Inventory Position (Derived View)                      │
│    ├── item_id                                          │
│    ├── location_id                                      │
│    ├── quantity_on_hand (SUM of positive entries)       │
│    ├── quantity_reserved (pending outbound)             │
│    ├── quantity_in_transit (pending receipt)            │
│    └── last_movement_at                                 │
│                                                         │
│  Item Identity Map (Optional Bridge)                    │
│    ├── item_id                                          │
│    ├── domain                                           │
│    └── domain_id                                        │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**优点**：
- 最清晰的三正交维度设计
- Stock Ledger 是唯一 Truth Source
- 天然支持所有 Location 类型
- 历史数据完整，支持任意时间点查询
- 最强的可扩展性

**缺点**：
- 迁移成本最高
- 需要重构所有库存查询为 Ledger 聚合
- 当前系统需要全面改造

---

## 3. 多维度比较矩阵

### 3.1 比较维度定义

| 维度 | 权重 | 说明 |
|------|------|------|
| **Business Correctness** | 🔴 最高 | 模型是否准确表达业务语义 |
| **Long-term Model Integrity** | 🟠 高 | 模型是否支持未来业务扩展 |
| **Cross-Domain Consistency** | 🟡 中高 | 模型是否统一所有业务域的库存语义 |
| **Migration Cost** | 🟢 中 | 从当前模型迁移的难度和成本 |

### 3.2 逐维度评分

#### Business Correctness（业务正确性）

| 模型 | 评分 | 说明 |
|------|------|------|
| **Model A** | ⭐⭐ | Material 是事实上的 Canonical Identity，但无法表达多角色。可乐既是原料又是可售品，Material 模型需要额外规则处理 |
| **Model B** | ⭐⭐⭐⭐ | Canonical Item + Capability Model 完美表达多角色。Item Identity Map 解决 food_id/material_id 分裂 |
| **Model C** | ⭐⭐⭐⭐ | Stockable Profile 在库存配置层面最精确，但 Profile 不是 Identity，需要配合 Canonical Item |
| **Model D** | ⭐⭐⭐⭐⭐ | Item + Location + Stock Ledger 三正交维度是最纯粹的业务表达。Stock Ledger 作为唯一 Truth Source 无歧义 |

**详细分析**：

| 业务场景 | Model A | Model B | Model C | Model D |
|----------|---------|---------|---------|---------|
| 可乐作为原料消耗 | ⚠️ 需要 Recipe 桥接 | ✅ Capability 标记 | ✅ Profile 配置 | ✅ Ledger 自然支持 |
| 可乐作为可售品销售 | ✅ material_id 管理 | ✅ Capability 标记 | ⚠️ 需要额外 Sellable Profile | ✅ Ledger 自然支持 |
| 鸡翅采购入库 | ✅ material_id | ✅ item_id | ✅ Profile 配置 | ✅ Ledger |
| 盘点盈亏 | ⚠️ 双表维护 | ✅ 统一表 | ✅ 统一表 | ✅ Ledger 记录 |
| 门店调拨 | ⚠️ 双表，需同步 | ✅ 统一表，location 变更 | ✅ Profile 维度 | ✅ Ledger 两端记录 |

---

#### Long-term Model Integrity（长期模型完整性）

| 模型 | 评分 | 说明 |
|------|------|------|
| **Model A** | ⭐⭐ | 当前实现的天花板。双表、UOM 不统一、Identity Duplicate 问题无法根本解决 |
| **Model B** | ⭐⭐⭐⭐ | Canonical Item + Identity Map 提供了长期演进的基础。支持未来新增 Item Type |
| **Model C** | ⭐⭐⭐ | Stockable Profile 配置灵活，但 Identity 层仍需要 Canonical Item |
| **Model D** | ⭐⭐⭐⭐⭐ | 三正交维度是理论上最完整的模型。支持任意 Location 层级、任意 Item Type、任意 Movement Type |

**详细分析**：

| 扩展场景 | Model A | Model B | Model C | Model D |
|----------|---------|---------|---------|---------|
| 新增中央厨房 Location | ⚠️ 需要第三张表 | ✅ Location 维度 | ✅ Profile 扩展 | ✅ Location 直接扩展 |
| 新增冷链食材 | ⚠️ 无存储温度字段 | ✅ Item 属性扩展 | ✅ Profile 配置 | ✅ Ledger 元数据扩展 |
| 新增移动 POS | ⚠️ 双表维护 | ✅ 统一表 | ✅ Profile | ✅ Ledger |
| 支持多币种 | ❌ 无支持 | ⚠️ 需扩展 | ⚠️ 需扩展 | ✅ Ledger 元数据扩展 |

---

#### Cross-Domain Consistency（跨域一致性）

| 模型 | 评分 | 说明 |
|------|------|------|
| **Model A** | ⭐⭐ | inventory/store_inventory 双表，采购/销售/库存各用不同 Identity |
| **Model B** | ⭐⭐⭐⭐ | Canonical Item 统一 Identity，Item Identity Map 桥接各域 |
| **Model C** | ⭐⭐⭐ | Stockable Profile 统一库存配置，但 Identity 仍需 Canonical Item |
| **Model D** | ⭐⭐⭐⭐⭐ | Item 统一 Identity，Stock Ledger 统一所有变动记录 |

**详细分析**：

| 跨域场景 | Model A | Model B | Model C | Model D |
|----------|---------|---------|---------|---------|
| 采购→库存→销售 | ⚠️ material_id→material_id→food_id 断裂 | ✅ item_id 贯穿 | ⚠️ 需要桥接 | ✅ item_id 贯穿 |
| 调拨（仓库→门店） | ❌ 跨表，无事务 | ✅ 同表 location 变更 | ✅ Profile 变更 | ✅ Ledger 两端记录 |
| 配方→库存扣减 | ⚠️ dish_recipe 桥接 | ✅ item_id 桥接 | ✅ Profile 扣减 | ✅ Ledger 扣减 |
| 盘点→成本调整 | ⚠️ 双表盘点 | ✅ 统一盘点 | ✅ Profile 盘点 | ✅ Ledger 调整 |

---

#### Migration Cost（迁移成本）

| 模型 | 评分 | 说明 |
|------|------|------|
| **Model A** | ⭐⭐⭐⭐⭐ | 当前已实现，迁移成本为零（但长期成本高） |
| **Model B** | ⭐⭐⭐ | 需要：1) 创建 Canonical Item 表 2) 创建 Item Identity Map 3) 统一 inventory/store_inventory 4) 迁移所有外键 |
| **Model C** | ⭐⭐ | 需要：Model B 的全部工作 + 创建 Stockable Profile 表 + 重构库存配置逻辑 |
| **Model D** | ⭐ | 需要：全面重构库存模型，所有查询重写，迁移成本最高 |

---

## 4. 综合评分

| 模型 | Business Correctness | Long-term Integrity | Cross-Domain Consistency | Migration Cost | **加权总分** |
|------|---------------------|---------------------|--------------------------|----------------|-------------|
| **Model A** | 2 × 4 = 8 | 2 × 3 = 6 | 2 × 2 = 4 | 5 × 1 = 5 | **23** |
| **Model B** | 4 × 4 = 16 | 4 × 3 = 12 | 4 × 2 = 8 | 3 × 1 = 3 | **39** |
| **Model C** | 4 × 4 = 16 | 3 × 3 = 9 | 3 × 2 = 6 | 2 × 1 = 2 | **33** |
| **Model D** | 5 × 4 = 20 | 5 × 3 = 15 | 5 × 2 = 10 | 1 × 1 = 1 | **46** |

> 权重：Business Correctness(4) > Long-term Model Integrity(3) > Cross-Domain Consistency(2) > Migration Cost(1)

---

## 5. 推荐模型：Model D

### 5.1 推荐理由

| 理由 | 说明 |
|------|------|
| **业务正确性最高** | Item + Location + Stock Ledger 三正交维度是最纯粹的业务表达 |
| **长期完整性最强** | 支持任意 Location 层级、任意 Item Type、任意 Movement Type |
| **跨域一致性最好** | Item 统一 Identity，Stock Ledger 统一所有变动记录 |
| **Truth Source 明确** | Stock Ledger 是唯一不可变的 Truth Source，余额从 Ledger 重建 |

### 5.2 迁移策略

由于 Model D 迁移成本最高，建议分阶段迁移：

```
Phase 1: 基础设施 (低风险)
├── 创建 Canonical Item 表
├── 创建 Item Identity Map 表
├── 创建 Location 表（统一 warehouse/store）
├── 创建 UOM + UOM Conversion 表
└── 创建 Stock Ledger 表（统一所有变动）

Phase 2: 数据迁移 (中风险)
├── material_archives → canonical_item
├── foods → sellable_item + item_identity_map
├── inventory + store_inventory → inventory_fact (Derived View)
└── inventory_transactions → stock_ledger

Phase 3: 业务重构 (高风险)
├── 所有库存查询重写为 Ledger 聚合
├── 采购/销售/调拨流程重构
├── 盘点流程重构
└── 成本核算流程重构
```

### 5.3 Model D 的详细设计

#### 5.3.1 Item 表

```sql
CREATE TABLE canonical_item (
    item_id       VARCHAR(32) PRIMARY KEY,
    item_code     VARCHAR(64) NOT NULL UNIQUE,
    item_name     VARCHAR(256) NOT NULL,
    item_type     ENUM('RAW','SEMI_FINISHED','FINISHED','PACKAGED','SERVICE') NOT NULL,
    primary_uom_id VARCHAR(32) NOT NULL,
    status        ENUM('ACTIVE','INACTIVE','DISCONTINUED') DEFAULT 'ACTIVE',
    is_sellable   BOOLEAN DEFAULT FALSE,
    is_purchasable BOOLEAN DEFAULT FALSE,
    is_stockable  BOOLEAN DEFAULT FALSE,
    is_recipe_component BOOLEAN DEFAULT FALSE,
    created_at    DATETIME NOT NULL,
    updated_at    DATETIME NOT NULL
);
```

#### 5.3.2 Location 表

```sql
CREATE TABLE inventory_location (
    location_id      VARCHAR(32) PRIMARY KEY,
    location_code    VARCHAR(64) NOT NULL UNIQUE,
    location_name    VARCHAR(256) NOT NULL,
    location_type    ENUM('WAREHOUSE','STORE','KITCHEN','TRANSIT','OTHER') NOT NULL,
    parent_location_id VARCHAR(32),
    status           ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE',
    created_at       DATETIME NOT NULL,
    updated_at       DATETIME NOT NULL
);
```

#### 5.3.3 Stock Ledger 表

```sql
CREATE TABLE stock_ledger (
    entry_id        VARCHAR(32) PRIMARY KEY,
    item_id         VARCHAR(32) NOT NULL,
    location_id     VARCHAR(32) NOT NULL,
    movement_type   ENUM(
        'PURCHASE_RECEIPT','PURCHASE_RETURN',
        'SALES_CONSUMPTION','SALES_RETURN',
        'TRANSFER_OUT','TRANSFER_IN',
        'KITCHEN_CONSUMPTION',
        'STOCKTAKE_GAIN','STOCKTAKE_LOSS',
        'DAMAGE','OPEN_PACKAGE',
        'ADJUSTMENT_IN','ADJUSTMENT_OUT'
    ) NOT NULL,
    quantity        DECIMAL(12,4) NOT NULL,
    uom_id          VARCHAR(32) NOT NULL,
    unit_cost       DECIMAL(12,4),
    batch_id        VARCHAR(64),
    reference_type  VARCHAR(32),
    reference_id    VARCHAR(32),
    metadata        JSON,
    created_at      DATETIME NOT NULL,
    created_by      VARCHAR(32),
    
    INDEX idx_item_location (item_id, location_id),
    INDEX idx_movement_type (movement_type),
    INDEX idx_created_at (created_at)
);
```

#### 5.3.4 Inventory Position（Derived View）

```sql
CREATE VIEW inventory_position AS
SELECT 
    item_id,
    location_id,
    SUM(CASE WHEN quantity > 0 THEN quantity ELSE 0 END) AS quantity_on_hand,
    SUM(CASE WHEN quantity < 0 THEN ABS(quantity) ELSE 0 END) AS quantity_consumed,
    SUM(quantity) AS net_quantity,
    MAX(created_at) AS last_movement_at
FROM stock_ledger
GROUP BY item_id, location_id;
```

---

## 6. 模型风险分析

### 6.1 Model D 的风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 迁移成本高 | 高 | 分阶段迁移，Phase 1 低风险 |
| 查询性能下降（Ledger 聚合） | 中 | 物化视图 + 定期聚合 |
| 团队学习曲线 | 中 | 培训 + 文档 |
| 业务流程重构 | 高 | 先在新模型中验证，再切换 |

### 6.2 Model A 不推荐的风险

| 风险 | 影响 | 说明 |
|------|------|------|
| 双表维护成本 | 高 | inventory + store_inventory 同步问题 |
| Identity Duplicate | 高 | foods ↔ material_archives 无映射 |
| UOM 不统一 | 中 | 各表独立存储 unit 字段 |
| 无统一 Truth Source | 极高 | 门店库存无流水记录 |

---

## 7. 其他模型的适用场景

| 模型 | 适用场景 | 不适用场景 |
|------|---------|-----------|
| **Model A** | 简单进销存、单门店系统 | 多门店、多仓库、复杂配方 |
| **Model B** | 需要 Canonical Identity 但不需要细粒度库存配置 | 需要差异化库存策略 |
| **Model C** | 需要细粒度库存配置但 Identity 层已解决 | Identity 层未解决 |
| **Model D** | **餐饮ERP全场景** | 简单进销存（过度设计） |

---

## 8. 关键原则重申

| # | 原则 | 说明 |
|---|------|------|
| 1 | **Business Correctness 最高优先** | 模型必须准确表达业务语义 |
| 2 | **Long-term Model Integrity 次高优先** | 模型必须支持未来扩展 |
| 3 | **Migration Cost 最低优先** | 不能因为迁移成本放弃正确模型 |
| 4 | **Stock Ledger 是 Truth Source** | 余额从 Ledger 重建，不独立存储 |
| 5 | **Item + Location + Stock Ledger 三正交** | 最纯粹的库存模型设计 |

---

## 9. 下一步行动

| # | 任务 | 优先级 | 依赖 |
|---|------|--------|------|
| 1 | Phase 23: 反例验证 Model D | P0 | 本分析 |
| 2 | Model D 详细技术设计 | P1 | Phase 23 验证通过 |
| 3 | 迁移策略制定 | P1 | Model D 确认 |

---

**状态**：ANALYSIS, NOT CONFIRMED
**日期**：2026-09-10
**阶段**：Phase 22 - Candidate Inventory Models
