# 库存位置模型分析 (Phase 4 - Inventory Location Model)

## 1. 问题域

本分析回答核心问题：**Store、Warehouse、Kitchen、Transit、Other Location 之间的关系是什么？**

库存位置（Inventory Location）是库存事实（Inventory Fact）的维度之一。不同的建模方式会直接影响：
- 库存表结构
- 查询语义
- 业务流程设计
- 数据一致性保证

---

## 2. 候选模型对比

### 2.1 Model A: Store = Warehouse（门店就是仓库）

```
┌─────────────────────────────────────────────────────┐
│                   Inventory Location                 │
│                                                      │
│  warehouse_id (PK)                                   │
│  ├── type = WAREHOUSE (中央仓/区域仓)                │
│  └── type = STORE (门店作为仓库)                     │
│                                                      │
│  统一使用 inventory 表                               │
│  inventory(material_id, warehouse_id) → quantity     │
└─────────────────────────────────────────────────────┘
```

**假设**：门店和仓库是同一种 Location，只是 type 不同。

**优点**：
- 单表管理所有库存，查询简单
- 天然支持调拨（同表内 warehouse_id 变更）
- 事务一致性容易保证

**缺点**：
- 门店库存的业务语义（如 safety_stock、max_stock）与仓库库存不同，混在同一表会导致字段冗余或语义模糊
- 门店库存可能需要额外字段（如 store_id 关联、门店特有的成本核算）
- 门店和仓库的操作权限、审批流程不同，单表难以区分

**适用场景**：简单的进销存系统，门店和仓库无显著差异。

---

### 2.2 Model B: Store ≠ Warehouse，但都是 Inventory Location

```
┌─────────────────────────────────────────────────────┐
│              Inventory Location (抽象层)             │
│                                                      │
│  location_id (PK)                                   │
│  location_type (ENUM: WAREHOUSE, STORE, KITCHEN...) │
│  location_code, location_name                       │
│                                                      │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌──────────────┐    ┌──────────────┐               │
│  │   Warehouse  │    │    Store     │               │
│  │  (继承/关联)  │    │  (继承/关联)  │               │
│  └──────────────┘    └──────────────┘               │
│                                                      │
│  Inventory Fact = Item × Location × Quantity         │
│  inventory(material_id, location_id) → quantity     │
└─────────────────────────────────────────────────────┘
```

**假设**：Store 和 Warehouse 是不同类型的 Location，但共享相同的 Inventory Fact 模型。

**优点**：
- 统一的 Location 抽象，支持扩展（Kitchen、Transit 等）
- Inventory Fact 模型统一：`Item × Location → Quantity`
- 调拨操作 = Location A 的 inventory 减少 + Location B 的 inventory 增加

**缺点**：
- 需要 Location 表，增加查询 JOIN
- 门店和仓库的库存属性差异（如 safety_stock）需要在 Inventory Fact 中兼容
- 需要 Location Type 枚举，扩展性受限

**适用场景**：中型系统，需要支持多种 Location 类型，但业务逻辑相对统一。

---

### 2.3 Model C: Inventory Location 枚举（STORE, WAREHOUSE, KITCHEN, TRANSIT, OTHER）

```
┌─────────────────────────────────────────────────────┐
│              Inventory Location (枚举)               │
│                                                      │
│  location_type: ENUM('STORE', 'WAREHOUSE',          │
│                       'KITCHEN', 'TRANSIT', 'OTHER')│
│                                                      │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌──────────────┐    ┌──────────────┐               │
│  │ inventory    │    │store_inventory│               │
│  │ (WAREHOUSE)  │    │   (STORE)    │               │
│  └──────────────┘    └──────────────┘               │
│                                                      │
│  ┌──────────────┐    ┌──────────────┐               │
│  │kitchen_inv   │    │transit_inv   │               │
│  │  (KITCHEN)   │    │  (TRANSIT)   │               │
│  └──────────────┘    └──────────────┘               │
│                                                      │
│  每种 Location Type 一张表                           │
└─────────────────────────────────────────────────────┘
```

**假设**：每种 Location Type 有独立的表和业务逻辑。

**优点**：
- 每种 Location 的字段可以独立设计，适应不同业务需求
- 查询性能高（单表查询，无 JOIN）
- 权限控制粒度细

**缺点**：
- 表数量多，维护成本高
- 调拨操作需要跨表事务，复杂度高
- 库存聚合查询需要 UNION ALL，性能差
- 新增 Location Type 需要新建表和代码

**适用场景**：大型系统，不同 Location 类型的业务逻辑差异显著。

---

## 3. 当前系统实现分析

### 3.1 现有表结构

```
inventory (仓库库存)
├── inventory_id (PK)
├── material_id (FK → material_archives)
├── warehouse_id (FK → warehouse)
├── current_stock, locked_quantity
├── unit_cost, total_cost
├── batch_no, production_date, expiry_date
└── min_safe_qty, max_stock_qty

store_inventory (门店库存)
├── id (PK)
├── store_id (FK → stores)
├── material_id (FK → material_archives)
├── current_stock
├── unit_cost, total_cost
├── safety_stock, max_stock
└── (无 batch_no, production_date, expiry_date)

inventory_locations (库位表)
├── location_id (PK)
├── warehouse_id (FK → warehouse)
├── location_code, location_name
├── location_type (INTEGER)
├── max_capacity, current_quantity
└── status
```

### 3.2 当前实现属于 Model C 的变体

当前系统实际上采用了 **Model C 的变体**：
- `inventory` 表对应 WAREHOUSE 类型
- `store_inventory` 表对应 STORE 类型
- `inventory_locations` 表对应仓库内的库位（子位置）

**问题**：
1. `inventory` 和 `store_inventory` 是两张独立的表，无事务一致性
2. `inventory_locations` 是仓库内的库位，不是独立的 Location Type
3. 缺少 KITCHEN、TRANSIT、OTHER 类型的表

### 3.3 当前实现的矛盾

| 矛盾点 | 现状 | 问题 |
|--------|------|------|
| 事务一致性 | `inventory` 和 `store_inventory` 独立更新 | 调拨操作可能部分成功，数据不一致 |
| 成本核算 | 两个表各自维护 `unit_cost` / `total_cost` | 成本传递逻辑未定义 |
| 库存汇总 | 使用 `UNION ALL` 合并两个表 | 聚合查询性能差，无法支持实时汇总 |
| 扩展性 | 新增 Location Type 需要新建表 | 开发成本高，代码复用性差 |

---

## 4. 推荐模型：Model B 的增强版

### 4.1 核心思想

**统一 Inventory Fact 模型，Location 作为维度**：

```
Inventory Fact = Material × Location → Quantity + Metadata
```

**Location 抽象层**：
```sql
CREATE TABLE inventory_locations (
    location_id BIGINT PRIMARY KEY,
    location_type VARCHAR(20) NOT NULL,  -- 'WAREHOUSE', 'STORE', 'KITCHEN', 'TRANSIT'
    location_code VARCHAR(50) NOT NULL,
    location_name VARCHAR(100) NOT NULL,
    -- 类型特定字段（可选）
    warehouse_id BIGINT,  -- 仅当 location_type = 'WAREHOUSE' 时使用
    store_id VARCHAR(64), -- 仅当 location_type = 'STORE' 时使用
    ...
);
```

**统一 Inventory Fact 表**：
```sql
CREATE TABLE inventory_facts (
    fact_id BIGINT PRIMARY KEY,
    material_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    current_stock DECIMAL(15,3) DEFAULT 0,
    locked_quantity DECIMAL(15,3) DEFAULT 0,
    unit_cost DECIMAL(12,2),
    total_cost DECIMAL(12,2),
    batch_no VARCHAR(50),
    production_date DATE,
    expiry_date DATE,
    -- 类型特定字段（可选）
    safety_stock DECIMAL(15,3),  -- 门店/仓库通用
    max_stock DECIMAL(15,3),     -- 门店/仓库通用
    UNIQUE(material_id, location_id, batch_no)
);
```

### 4.2 与当前系统的兼容

| 当前表 | 目标表 | 迁移策略 |
|--------|--------|----------|
| `inventory` | `inventory_facts` | 添加 `location_id`，关联 `inventory_locations` |
| `store_inventory` | `inventory_facts` | 添加 `location_id`，关联 `inventory_locations` |
| `inventory_locations` | `inventory_locations` | 扩展 `location_type` 枚举 |

### 4.3 调拨操作的语义

```
调拨 = 从源 Location 转移到目标 Location

BEGIN TRANSACTION;
  -- 1. 源 Location 库存减少
  UPDATE inventory_facts 
  SET current_stock = current_stock - 10
  WHERE material_id = 1 AND location_id = 100;
  
  -- 2. 目标 Location 库存增加
  UPDATE inventory_facts 
  SET current_stock = current_stock + 10
  WHERE material_id = 1 AND location_id = 200;
  
  -- 3. 记录调拨流水
  INSERT INTO inventory_transactions (material_id, from_location_id, to_location_id, quantity)
  VALUES (1, 100, 200, 10);
COMMIT;
```

---

## 5. 业务语义合理性分析

### 5.1 餐饮 ERP 的典型场景

```
中央仓库 (WAREHOUSE)
    │
    ├──► 区域仓库 (WAREHOUSE)
    │        │
    │        └──► 门店 (STORE)
    │                 │
    │                 └──► 厨房 (KITCHEN)
    │
    └──► 调拨中 (TRANSIT)
```

### 5.2 各 Location 的业务语义

| Location Type | 业务语义 | 关键字段 | 操作特点 |
|---------------|----------|----------|----------|
| WAREHOUSE | 采购入库、库存调拨 | batch_no, production_date, expiry_date | 批次管理、保质期追踪 |
| STORE | 门店收货、销售消耗 | safety_stock, max_stock | 预警阈值、补货触发 |
| KITCHEN | 领料消耗、菜品制作 | 无独立库存（实时消耗） | 可能不需要库存表 |
| TRANSIT | 调拨在途 | 预计到达时间 | 锁定库存，不可用 |

### 5.3 推荐的 Location Type 定义

```typescript
enum LocationType {
  WAREHOUSE = 'WAREHOUSE',    // 仓库：采购入库、批次管理
  STORE = 'STORE',            // 门店：销售消耗、补货触发
  KITCHEN = 'KITCHEN',        // 厨房：领料消耗（可能不需要库存表）
  TRANSIT = 'TRANSIT',        // 调拨在途：锁定库存
  OTHER = 'OTHER'             // 其他：报损、退货等临时位置
}
```

---

## 6. 决策矩阵

| 评估维度 | Model A | Model B | Model C | 当前实现 |
|----------|---------|---------|---------|----------|
| 事务一致性 | ✅ 天然支持 | ✅ 单表事务 | ❌ 跨表事务 | ❌ 独立表 |
| 查询性能 | ✅ 单表查询 | ⚠️ 需要 JOIN | ✅ 单表查询 | ⚠️ UNION ALL |
| 扩展性 | ⚠️ 字段冗余 | ✅ 支持新类型 | ⚠️ 需新建表 | ❌ 需新建表 |
| 业务语义 | ❌ 混淆语义 | ✅ 统一模型 | ✅ 独立设计 | ⚠️ 部分统一 |
| 开发成本 | ✅ 低 | ⚠️ 中等 | ❌ 高 | - |
| 维护成本 | ✅ 低 | ⚠️ 中等 | ❌ 高 | ❌ 高 |

---

## 7. 结论

### 7.1 推荐模型

**Model B 的增强版**：统一 Inventory Fact 模型 + Location 抽象层

**理由**：
1. **语义统一**：Inventory Fact = Material × Location → Quantity，所有 Location 类型共享同一模型
2. **事务一致**：单表事务，调拨操作原子性有保证
3. **扩展友好**：新增 Location Type 只需在 `inventory_locations` 表中添加记录
4. **性能可控**：单表查询 + 索引，聚合查询可通过物化视图优化

### 7.2 与当前系统的差距

| 差距 | 影响 | 建议 |
|------|------|------|
| `inventory` 和 `store_inventory` 独立 | 事务不一致 | 迁移到统一表 |
| `inventory_locations` 只支持仓库内库位 | 无法扩展 | 扩展为通用 Location 表 |
| 缺少 TRANSIT 类型 | 调拨在途无法追踪 | 新增 TRANSIT 类型 |

### 7.3 迁移路径

**短期（保持现状）**：
- 承认双表模型的现实
- 通过应用层保证事务一致性（如：调拨操作在同一事务中更新两个表）

**中期（引入 Location 抽象）**：
- 创建统一的 `inventory_locations` 表
- 逐步迁移 `inventory` 和 `store_inventory` 到统一表

**长期（统一 Inventory Fact）**：
- 废弃 `inventory` 和 `store_inventory` 表
- 使用统一的 `inventory_facts` 表

---

## 8. 附录：相关文件

| 文件 | 说明 |
|------|------|
| `V20260629_016__create_inventory_locations_table.sql` | 库位表建表脚本 |
| `V20260704_004__create_store_inventory_table.sql` | 门店库存表建表脚本 |
| `inventory-stockable-model.md` | 库存可存储模型分析 |
| `system-truth-source-map.md` | 系统真相源映射 |
