# 门店库存与仓储库存分析 (Phase 5 - Store vs Warehouse)

## 1. 问题域

本分析回答核心问题：**门店库存与仓储库存应该是什么关系？**

关键问题：
- Inventory Fact 是否应该 = Item × Location × Quantity？
- Warehouse/Management View 是否只是 Aggregate Inventory View？
- "总库存 = 仓库 + 门店" 中的 160 是否只是计算结果？

---

## 2. 当前系统现实

### 2.1 表结构

```
inventory (仓库库存)
├── inventory_id (PK)
├── material_id (FK → material_archives)
├── warehouse_id (FK → warehouse)
├── current_stock (当前库存)
├── locked_quantity (锁定库存)
├── unit_cost (单位成本)
├── total_cost (总成本)
├── batch_no (批次号)
├── production_date (生产日期)
├── expiry_date (保质期)
├── min_safe_qty (安全库存)
└── max_stock_qty (最大库存)

store_inventory (门店库存)
├── id (PK)
├── store_id (FK → stores)
├── material_id (FK → material_archives)
├── current_stock (当前库存)
├── unit_cost (单位成本)
├── total_cost (总成本)
├── safety_stock (安全库存)
└── max_stock (最大库存)
```

### 2.2 关键差异

| 维度 | inventory (仓库) | store_inventory (门店) |
|------|------------------|------------------------|
| 位置维度 | warehouse_id | store_id |
| 批次管理 | ✅ batch_no, production_date, expiry_date | ❌ 无 |
| 成本核算 | ✅ unit_cost, total_cost | ✅ unit_cost, total_cost |
| 预警阈值 | min_safe_qty, max_stock_qty | safety_stock, max_stock |
| 事务一致性 | 独立 | 独立 |
| 流水表 | inventory_transactions | store_inventory_log |

### 2.3 数据流

```
采购入库
    │
    ├──► inventory (仓库库存增加)
    │
    └──► (门店收货时)
         └──► store_inventory (门店库存增加)
         └──► inventory (仓库库存减少)

销售出库
    │
    └──► store_inventory (门店库存减少)

调拨
    │
    ├──► inventory (源仓库库存减少)
    └──► inventory (目标仓库库存增加)
    │
    ├──► store_inventory (源门店库存减少)
    └──► store_inventory (目标门店库存增加)
```

---

## 3. 核心问题分析

### 3.1 Inventory Fact 是否应该 = Item × Location × Quantity？

**当前实现**：
- `inventory`: Item × Warehouse → Quantity
- `store_inventory`: Item × Store → Quantity

**问题**：两个表的 Location 维度不统一（warehouse_id vs store_id），导致：
1. 无法统一查询"某物料在所有位置的库存"
2. 调拨操作需要跨表事务
3. 库存汇总需要 UNION ALL

**推荐**：**是的，Inventory Fact 应该 = Item × Location × Quantity**

**理由**：
1. **语义清晰**：库存是"物料在某个位置的数量"，Location 是统一维度
2. **查询统一**：`SELECT * FROM inventory_facts WHERE material_id = ?` 可以查所有位置
3. **事务一致**：调拨操作在同一表内完成，原子性有保证

### 3.2 Warehouse/Management View 是否只是 Aggregate Inventory View？

**当前实现**：
- 仓储管理页面显示"总库存"（UNION ALL 合并两个表）
- 门店库存页面只显示门店维度

**问题**：
1. "总库存"是计算结果，不是独立的 Truth Source
2. 聚合查询性能差（UNION ALL）
3. 无法支持实时汇总

**推荐**：**是的，Warehouse/Management View 只是 Aggregate Inventory View**

**理由**：
1. **Truth Source 只有一个**：`inventory_facts` 表（统一库存表）
2. **View 是派生数据**：总库存 = SUM(inventory_facts WHERE material_id = ?)
3. **View 可以缓存**：通过物化视图或缓存优化性能

### 3.3 "总库存 = 仓库 + 门店" 中的 160 是否只是计算结果？

**示例**：
```
物料A：
  仓库库存 = 100
  门店库存 = 60
  总库存 = 160（计算结果）
```

**问题**：160 是否应该存储在某个表中？

**推荐**：**160 只是计算结果，不应该存储**

**理由**：
1. **避免双 Truth Source**：如果存储 160，那么 inventory_facts 表的 SUM 与存储值可能不一致
2. **实时计算**：160 可以通过 `SUM(current_stock) WHERE material_id = ?` 实时计算
3. **性能优化**：如果查询频繁，可以通过物化视图或缓存优化，但逻辑上仍是计算结果

---

## 4. 双库存模型的风险

### 4.1 事务不一致

**场景**：门店收货确认
```
BEGIN TRANSACTION;
  -- 1. 仓库库存减少
  UPDATE inventory 
  SET current_stock = current_stock - 10
  WHERE material_id = 1 AND warehouse_id = 100;
  
  -- 2. 门店库存增加（如果失败）
  UPDATE store_inventory 
  SET current_stock = current_stock + 10
  WHERE material_id = 1 AND store_id = 'S001';
  
  -- 如果步骤2失败，步骤1已提交，数据不一致
COMMIT;
```

**风险**：仓库库存已减少，但门店库存未增加，物料"丢失"。

### 4.2 成本核算割裂

**场景**：采购入库
```
采购单价 = 10元/kg

仓库库存：
  unit_cost = 10
  total_cost = 1000 (100kg)

门店调拨后：
  门店库存 unit_cost = ? (未定义)
  门店库存 total_cost = ? (未定义)
```

**风险**：成本传递逻辑未定义，可能导致成本计算错误。

### 4.3 库存汇总性能

**当前实现**：
```sql
SELECT 
  material_id,
  SUM(CASE WHEN source = 'warehouse' THEN current_stock ELSE 0 END) as warehouse_stock,
  SUM(CASE WHEN source = 'store' THEN current_stock ELSE 0 END) as store_stock,
  SUM(current_stock) as total_stock
FROM (
  SELECT material_id, current_stock, 'warehouse' as source FROM inventory
  UNION ALL
  SELECT material_id, current_stock, 'store' as source FROM store_inventory
) combined
GROUP BY material_id;
```

**问题**：
1. UNION ALL 性能差（全表扫描）
2. 无法支持实时汇总（每次查询都重新计算）
3. 无法支持分页（需要先聚合再分页）

---

## 5. 推荐模型：统一 Inventory Fact

### 5.1 核心思想

**一个 Truth Source，多个 View**

```
┌─────────────────────────────────────────────────────┐
│              Truth Source: inventory_facts           │
│                                                      │
│  material_id × location_id → quantity + metadata    │
│                                                      │
├─────────────────────────────────────────────────────┤
│                                                      │
│  View 1: 仓库库存 View                              │
│  SELECT * FROM inventory_facts                       │
│  WHERE location_id IN (SELECT id FROM locations      │
│                         WHERE type = 'WAREHOUSE')    │
│                                                      │
│  View 2: 门店库存 View                              │
│  SELECT * FROM inventory_facts                       │
│  WHERE location_id IN (SELECT id FROM locations      │
│                         WHERE type = 'STORE')        │
│                                                      │
│  View 3: 总库存 View（聚合）                        │
│  SELECT material_id, SUM(current_stock) as total    │
│  FROM inventory_facts                                │
│  GROUP BY material_id                                │
│                                                      │
└─────────────────────────────────────────────────────┘
```

### 5.2 表结构设计

```sql
-- Location 表（统一位置）
CREATE TABLE inventory_locations (
    location_id BIGINT PRIMARY KEY,
    location_type VARCHAR(20) NOT NULL,  -- 'WAREHOUSE', 'STORE', 'KITCHEN', 'TRANSIT'
    location_code VARCHAR(50) NOT NULL,
    location_name VARCHAR(100) NOT NULL,
    -- 类型特定字段
    warehouse_id BIGINT,  -- 仅 WAREHOUSE 类型
    store_id VARCHAR(64), -- 仅 STORE 类型
    status INTEGER DEFAULT 1,
    UNIQUE(location_code)
);

-- 统一库存事实表
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
    safety_stock DECIMAL(15,3),
    max_stock DECIMAL(15,3),
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    UNIQUE(material_id, location_id, batch_no)
);

-- 索引
CREATE INDEX idx_inventory_facts_material ON inventory_facts(material_id);
CREATE INDEX idx_inventory_facts_location ON inventory_facts(location_id);
CREATE INDEX idx_inventory_facts_batch ON inventory_facts(batch_no);
```

### 5.3 查询示例

**查询某物料在所有位置的库存**：
```sql
SELECT 
  l.location_name,
  l.location_type,
  f.current_stock,
  f.unit_cost,
  f.total_cost
FROM inventory_facts f
JOIN inventory_locations l ON f.location_id = l.location_id
WHERE f.material_id = 1
  AND f.deleted = 0;
```

**查询某物料的总库存**：
```sql
SELECT 
  SUM(f.current_stock) as total_stock,
  SUM(f.total_cost) as total_cost
FROM inventory_facts f
WHERE f.material_id = 1
  AND f.deleted = 0;
```

**调拨操作**：
```sql
BEGIN TRANSACTION;
  -- 源 Location 库存减少
  UPDATE inventory_facts 
  SET current_stock = current_stock - 10,
      total_cost = total_cost - (unit_cost * 10)
  WHERE material_id = 1 AND location_id = 100 AND deleted = 0;
  
  -- 目标 Location 库存增加（如果不存在则插入）
  INSERT INTO inventory_facts (material_id, location_id, current_stock, unit_cost, total_cost)
  VALUES (1, 200, 10, 10.00, 100.00)
  ON CONFLICT (material_id, location_id, batch_no) 
  DO UPDATE SET 
    current_stock = inventory_facts.current_stock + 10,
    total_cost = inventory_facts.total_cost + 100.00;
  
  -- 记录调拨流水
  INSERT INTO inventory_transactions (material_id, from_location_id, to_location_id, quantity)
  VALUES (1, 100, 200, 10);
COMMIT;
```

---

## 6. 迁移策略

### 6.1 短期（保持现状）

**目标**：承认双表模型的现实，通过应用层保证一致性

**措施**：
1. 调拨操作在同一事务中更新两个表
2. 定期对账（inventory vs store_inventory）
3. 库存汇总使用物化视图优化性能

**风险**：事务一致性依赖应用层，容易出错

### 6.2 中期（引入 Location 抽象）

**目标**：创建统一的 Location 表，逐步迁移

**步骤**：
1. 创建 `inventory_locations` 表，支持 WAREHOUSE 和 STORE 类型
2. 为 `inventory` 表添加 `location_id` 字段，关联 `inventory_locations`
3. 为 `store_inventory` 表添加 `location_id` 字段，关联 `inventory_locations`
4. 双写：新数据写入 `inventory_facts`，旧数据保留在原表

**风险**：双写期间数据一致性需要保证

### 6.3 长期（统一 Inventory Fact）

**目标**：废弃 `inventory` 和 `store_inventory` 表，使用统一的 `inventory_facts` 表

**步骤**：
1. 数据迁移：将 `inventory` 和 `store_inventory` 的数据迁移到 `inventory_facts`
2. 代码迁移：修改所有查询和更新逻辑，使用 `inventory_facts`
3. 废弃旧表：标记 `inventory` 和 `store_inventory` 为 @Deprecated

**风险**：数据迁移可能丢失历史数据，需要充分测试

---

## 7. 决策矩阵

| 评估维度 | 当前双表模型 | 统一 Inventory Fact | 推荐 |
|----------|--------------|---------------------|------|
| 事务一致性 | ❌ 独立表，无保证 | ✅ 单表事务 | 统一模型 |
| 成本核算 | ❌ 割裂，未定义传递逻辑 | ✅ 统一成本字段 | 统一模型 |
| 查询性能 | ⚠️ UNION ALL | ✅ 单表查询 | 统一模型 |
| 扩展性 | ❌ 新类型需新建表 | ✅ 只需添加 Location 记录 | 统一模型 |
| 开发成本 | - | ⚠️ 需要迁移 | 短期保持 |
| 维护成本 | ❌ 高（两套逻辑） | ✅ 低（一套逻辑） | 统一模型 |

---

## 8. 结论

### 8.1 核心结论

1. **Inventory Fact = Material × Location × Quantity**
   - Location 是统一维度，支持 WAREHOUSE、STORE、KITCHEN、TRANSIT 等类型
   - 每个 (Material, Location, Batch) 组合是独立的库存事实

2. **Warehouse/Management View 是 Aggregate Inventory View**
   - View 是派生数据，不是独立的 Truth Source
   - 总库存 = SUM(inventory_facts WHERE material_id = ?)

3. **"总库存 = 仓库 + 门店" 中的 160 是计算结果**
   - 160 不应该存储在任何表中
   - 160 可以通过实时查询或物化视图获取

4. **双表模型是技术债，需要逐步偿还**
   - 短期：通过应用层保证一致性
   - 中期：引入 Location 抽象，双写过渡
   - 长期：统一到 inventory_facts 表

### 8.2 关键原则

1. **不得因为存在 Store Inventory + Warehouse Inventory，就建立两套库存 Truth**
   - Truth Source 只有一个：inventory_facts 表
   - View 是派生数据，可以缓存但逻辑上是计算结果

2. **聚合库存不能成为第二 Truth Source**
   - 总库存 = SUM(inventory_facts)
   - 不应该存储"总库存"字段

3. **它们应该是同一 Inventory Model + 不同 Inventory Location**
   - 库存模型统一：Material × Location → Quantity
   - Location 类型不同：WAREHOUSE、STORE、KITCHEN、TRANSIT

---

## 9. 附录：相关文件

| 文件 | 说明 |
|------|------|
| `V20260629_016__create_inventory_locations_table.sql` | 库位表建表脚本 |
| `V20260704_004__create_store_inventory_table.sql` | 门店库存表建表脚本 |
| `inventory-stockable-model.md` | 库存可存储模型分析 |
| `system-truth-source-map.md` | 系统真相源映射 |
| `system-conflict-map.md` | 系统冲突映射 |
| `data-chain-fix-handover.md` | 数据链路修复交接文档 |
