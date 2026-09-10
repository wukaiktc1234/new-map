# Inventory Model Stress Points (Phase 22 Extension)

> **核心问题**：库存模型在哪些地方被迫"打补丁"？这些补丁是孤立问题还是系统性模型缺陷的症状？
> **状态**：ANALYSIS, NOT CONFIRMED
> **约束**：使用真实业务案例验证每个压力点，证明为什么正确、在哪里不正确
> **依赖**：Phase 21 Model Stress Points (business-item-model/reconciliation)，Phase 18/20 Inventory Identity Analysis

---

## 一、分析范围

本分析是 Phase 21 `model-stress-points.md` 的 **库存域专项扩展**。Phase 21 识别了跨域的 6 种压力点类型，本分析将这些压力点**聚焦到库存模型**，回答：

1. 库存模型本身在哪些地方被迫打补丁？
2. 库存模型与采购、销售、配方、财务的接口处有哪些应力集中？
3. 库存特有的 MODEL_STRESS_POINT 是什么？

---

## 二、压力点识别方法论（库存域适配）

### 2.1 六种压力点类型的库存域定义

| 类型 | 库存域定义 | 信号 |
|------|-----------|------|
| **Identity Stress** | 库存记录引用的 material_id 与其他域的 food_id 无法自然关联 | 预包装品同时有 food_id 和 material_id，无物理映射 |
| **Role Conflict** | 同一 material 必须同时扮演 Warehouse-Stockable 和 Store-Stockable 角色，但两套库存表结构不同 | inventory 与 store_inventory 字段不一致 |
| **Scope Mismatch** | 库存配置（安全库存、最大库存）需要门店级别覆盖，但当前模型只支持全局配置 | 安全库存无法按门店差异化 |
| **Relationship Complexity** | 库存变动与采购入库、销售出库、调拨、配方消耗之间的跨域关系无法自然表达 | inventory_transactions 缺少门店维度流水 |
| **Data Duplication** | 同一 material 的名称、单位、成本在 inventory 和 store_inventory 中重复存储且不一致 | 单位字段在多表中不一致 |
| **Type Ambiguity** | inventory 表同时承担仓库库存和在途库存的语义，store_inventory 同时承担门店库存和消耗品语义 | transaction_type 编码含义模糊 |

---

## 三、Identity Stress: 库存记录的双重身份引用

### 3.1 压力描述

库存表（`inventory` 和 `store_inventory`）通过 `material_id` 引用 `material_archives`，但当预包装品（可乐）同时具有 `food_id` 时，库存系统**无法直接回答**："这个 material_id 对应的销售价是多少？"

```
当前库存记录：
inventory: material_id=M001, warehouse_id=W01, current_stock=200
    ↓ 引用
material_archives: material_id=M001, material_name="可口可乐330ml"
    ↓ 无物理映射
foods: food_id=F001, food_name="可乐", price=3.00
```

### 3.2 补丁代码

```java
// 查询库存时，需要额外查询 foods 表获取售价
public StockInfo getStockWithPrice(Long materialId) {
    Inventory inv = inventoryMapper.selectByMaterialId(materialId);
    
    // 补丁：通过编码匹配查找对应的 food_id
    String foodCode = materialIdToFoodCode(materialId);  // 不可靠的映射
    Food food = foodMapper.selectByCode(foodCode);
    
    // 如果 foods 中没有记录呢？
    // 如果编码不一致呢？
    return new StockInfo(inv, food != null ? food.getPrice() : null);
}
```

### 3.3 业务案例验证

**案例：可乐的库存与销售联动**

```
场景：门店 POS 售出 1 瓶可乐

步骤 1：POS 系统扣减
  order_items.food_id = F001, quantity = 1
  → 需要知道 F001 对应的 material_id = M001

步骤 2：库存扣减
  inventory.material_id = M001, current_stock -= 1
  → 需要知道 M001 对应的库存位置

问题：
  - 从 F001 → M001 的映射在哪里？
  - 当前系统：无物理外键，依赖业务代码手动映射
  - 风险：映射断裂时，库存扣减失败或扣错
```

### 3.4 根因分析

库存模型假设 `material_id` 是唯一的 Identity，但销售域使用 `food_id`。两者的映射关系**未在数据模型中显式表达**，导致：
1. 库存扣减需要跨域映射逻辑
2. 映射关系散落在业务代码中
3. 数据一致性无法保证

### 3.5 影响评估

| 受影响的场景 | 影响程度 | 具体表现 |
|-------------|----------|----------|
| POS 销售扣减库存 | 🔴 严重 | food_id → material_id 映射不可靠 |
| 成本核算 | 🔴 严重 | 无法自动从销售追溯到原料成本 |
| 库存报表 | 🟡 中等 | 跨域报表需要复杂的 JOIN 逻辑 |
| 新增预包装品 | 🟡 中等 | 需要同时在 foods 和 material_archives 中维护 |

---

## 四、Role Conflict: Warehouse-Stockable vs Store-Stockable 的结构分裂

### 4.1 压力描述

同一个 material（可乐）必须同时在 `inventory`（仓库库存）和 `store_inventory`（门店库存）中存在，但两张表的**字段结构完全不同**。

```
inventory 表:
  ├── inventory_id (PK)
  ├── material_id (FK)          ← 注意：V1.0.0.100 遗留用 product_id
  ├── warehouse_id
  ├── current_stock
  ├── locked_quantity
  ├── unit_cost
  ├── total_cost
  ├── batch_no
  ├── min_safe_qty
  └── max_stock_qty

store_inventory 表:
  ├── id (PK)
  ├── material_id (FK)
  ├── store_id
  ├── current_stock
  ├── unit                      ← inventory 无此字段
  ├── unit_cost
  ├── total_cost
  ├── safety_stock              ← inventory 叫 min_safe_qty
  └── max_stock                 ← inventory 叫 max_stock_qty
```

### 4.2 补丁代码

```java
// 统一查询所有库存时，需要合并两张表的结果
public List<StockSummary> getAllStock(Long materialId) {
    List<StockSummary> result = new ArrayList<>();
    
    // 查仓库库存
    Inventory wh = inventoryMapper.selectByMaterialId(materialId);
    if (wh != null) {
        result.add(new StockSummary("WAREHOUSE", wh.getWarehouseId(),
            wh.getCurrentStock(), wh.getUnitCost()));
    }
    
    // 查门店库存（注意：字段名不同！）
    List<StoreInventory> stores = storeInventoryMapper.selectByMaterialId(materialId);
    for (StoreInventory si : stores) {
        result.add(new StockSummary("STORE", si.getStoreId(),
            si.getCurrentStock(), si.getUnitCost()));
    }
    
    // 问题：两张表的字段名不一致，需要手动映射
    // safety_stock vs min_safe_qty
    // max_stock vs max_stock_qty
    return result;
}
```

### 4.3 业务案例验证

**案例：门店调拨流程**

```
场景：仓库向门店A调拨 100 罐可乐

当前流程：
  步骤1：更新 inventory 表（仓库库存减少）
    UPDATE inventory SET current_stock = current_stock - 100
    WHERE material_id = M001 AND warehouse_id = W01;

  步骤2：更新 store_inventory 表（门店库存增加）
    UPDATE store_inventory SET current_stock = current_stock + 100
    WHERE material_id = M001 AND store_id = S01;

问题：
  - 两个 UPDATE 不在同一个事务中
  - 如果步骤2失败，步骤1已提交 → 库存数据不一致
  - 没有 in-transit 状态表示调拨在途
  - 调拨成本如何传递未定义
```

### 4.4 根因分析

**根因 1**：仓库库存和门店库存使用**不同的表结构**
- `inventory` 使用 `product_id`（遗留）+ `warehouse_id`
- `store_inventory` 使用 `material_id` + `store_id`
- 字段名不一致（`min_safe_qty` vs `safety_stock`）

**根因 2**：缺少**统一的位置模型**
- 仓库和门店应该是同一种位置类型的不同实例
- 但当前模型将它们建模为两个独立的概念

**根因 3**：缺少**在途库存**概念
- 调拨过程中，物料处于"在途"状态
- 当前模型无法表达这个中间状态

### 4.5 影响评估

| 受影响的场景 | 影响程度 | 具体表现 |
|-------------|----------|----------|
| 门店调拨 | 🔴 严重 | 无事务一致性保证，无在途状态 |
| 库存盘点 | 🔴 严重 | 仓库和门店库存独立盘点，无法统一 |
| 成本核算 | 🟡 中等 | 仓库和门店成本独立维护，调拨成本传递未定义 |
| 库存报表 | 🟡 中等 | 需要合并两张表的数据，字段名不一致 |
| 安全库存预警 | 🟡 中等 | 两张表的安全库存字段名不同 |

---

## 五、Scope Mismatch: 全局库存配置 vs 门店差异化

### 5.1 压力描述

库存配置（安全库存、最大库存、存储条件）需要支持**门店级别**的差异化，但当前模型只支持**全局配置**或**仓库级配置**。

```
理想场景：
  可乐的安全库存：
    集团标准: 50 罐
    门店A (商圈): 80 罐
    门店B (学校): 30 罐

当前现实：
  inventory.min_safe_qty = 50    ← 仓库级，全局统一
  store_inventory.safety_stock = ??  ← 门店级，但字段含义不明确
```

### 5.2 补丁代码

```sql
-- 门店安全库存的补丁：在 store_inventory 中硬编码
UPDATE store_inventory 
SET safety_stock = 80 
WHERE material_id = M001 AND store_id = S01;  -- 门店A

UPDATE store_inventory 
SET safety_stock = 30 
WHERE material_id = M001 AND store_id = S02;  -- 门店B

-- 问题：
-- 1. 每个 (material, store) 组合需要单独配置
-- 2. 没有继承机制：修改集团标准时，不会自动更新门店
-- 3. 配置散落在多条记录中，无法统一管理
```

### 5.3 业务案例验证

**案例：连锁餐厅的安全库存管理**

| 配置项 | 集团标准 | 门店A (商圈) | 门店B (学校) | 门店C (社区) |
|--------|---------|-------------|-------------|-------------|
| 可乐安全库存 | 50 | 80 | 30 | 50 |
| 大米安全库存 | 100 | 120 | 80 | 100 |
| 鸡翅安全库存 | 20 | 30 | 15 | 20 |
| 酱油安全库存 | 10 | 15 | 10 | 10 |

**问题**：
1. 每个门店的每个 material 都需要单独配置
2. 修改集团标准时，无法自动传播到门店
3. 没有"继承 + 覆盖"的机制

### 5.4 根因分析

当前模型缺少**Profile + Scope**的覆盖机制：
- 每个 (material, location) 组合直接存储配置值
- 没有"默认值 + 覆盖值"的层级关系
- 修改默认值时无法自动传播

### 5.5 影响评估

| 受影响的场景 | 影响程度 | 具体表现 |
|-------------|----------|----------|
| 安全库存预警 | 🔴 严重 | 无法统一管理门店差异化配置 |
| 库存补货 | 🟡 中等 | 补货策略无法按门店差异化 |
| 配置维护 | 🔴 严重 | 每个门店需要单独配置，工作量大 |

---

## 六、Relationship Complexity: 库存变动与多业务链的隐式关联

### 6.1 压力描述

库存变动（`inventory_transactions`）只记录了仓库维度的流水，缺少**门店维度**的流水记录。门店的库存变动（销售扣减、消耗扣减）**没有对应的事务流水表**。

```
当前系统：
  inventory_transactions: 记录仓库库存变动
    ├── type=1: 采购入库
    ├── type=2: 销售出库（？门店销售如何记录？）
    ├── type=3: 调拨出
    ├── type=4: 调拨入
    ├── type=5: 盘点盈
    ├── type=6: 盘点亏
    ├── type=7: 报损
    └── type=8: 退货

  store_inventory: 门店库存
    ├── 当前库存数量
    ├── 无事务流水表
    └── 库存变动无法追溯
```

### 6.2 补丁代码

```java
// 门店销售扣减库存时，没有事务流水记录
public void deductStoreStock(Long materialId, Long storeId, int quantity) {
    // 直接更新 store_inventory
    storeInventoryMapper.deductStock(storeId, materialId, quantity);
    
    // 没有记录事务流水！
    // 问题：无法追溯是什么操作扣减了门店库存
    // 问题：无法审计门店库存变动历史
    // 问题：无法从流水重建门店库存
}

// 如果需要追溯门店库存变动，只能通过对比前后快照
// 这不是真正的事件溯源
```

### 6.3 业务案例验证

**案例：门店库存盘点差异**

```
场景：门店A盘点发现可乐库存差异

  理论库存 = 期初 + 入库 - 销售 - 消耗
  实际库存 = 盘点数量
  差异 = 实际库存 - 理论库存

问题：
  - "入库"从哪里来？store_inventory 没有入库流水
  - "销售"从哪里来？order_items 只记录 food_id，不记录 material_id
  - "消耗"从哪里来？material_consumption 记录了消耗，但不在 store_inventory 流水中
  - 无法自动计算理论库存，只能手动对账
```

### 6.4 根因分析

**根因 1**：门店库存缺少**事务流水表**
- `inventory_transactions` 只关联 `inventory_id`（仓库库存）
- 门店库存变动没有对应的流水记录
- 库存变动无法追溯

**根因 2**：销售域与库存域的**接口不完整**
- `order_items` 使用 `food_id`，不直接关联 `material_id`
- 门店销售扣减库存需要手动映射 food_id → material_id
- 映射关系散落在业务代码中

### 6.5 影响评估

| 受影响的场景 | 影响程度 | 具体表现 |
|-------------|----------|----------|
| 门店库存追溯 | 🔴 严重 | 无法追溯门店库存变动历史 |
| 门店盘点 | 🔴 严重 | 无法自动计算理论库存 |
| 成本核算 | 🟡 中等 | 门店消耗成本无法自动追溯 |
| 数据一致性 | 🔴 严重 | 门店库存变动无审计记录 |

---

## 七、Data Duplication: 库存数据在多处重复存储

### 7.1 压力描述

同一 material 的库存相关信息（名称、单位、成本、安全库存）在 `inventory` 和 `store_inventory` 中**重复存储**，且**字段名不一致**。

```
inventory 表中的 material 信息：
  material_id = M001
  current_stock = 200
  unit_cost = 2.50
  min_safe_qty = 50          ← 安全库存（仓库级）
  max_stock_qty = 500        ← 最大库存

store_inventory 表中的 material 信息：
  material_id = M001
  current_stock = 20
  unit_cost = 2.50           ← 与 inventory.unit_cost 可能不一致！
  safety_stock = 50          ← 安全库存（门店级），字段名不同！
  max_stock = 500            ← 最大库存，字段名不同！

  unit = "罐"                ← inventory 表没有 unit 字段！
```

### 7.2 业务案例验证

**案例：调拨后成本不一致**

```
场景：仓库向门店A调拨可乐

  仓库库存成本: unit_cost = 2.50 (加权平均)
  门店库存成本: unit_cost = 2.50 (初始值)

  调拨后：
    仓库库存减少，成本按比例减少
    门店库存增加，成本 = 调拨成本

  问题：
    - 调拨成本如何确定？是仓库成本还是市场价？
    - 如果门店之前有库存，新调拨的成本如何与旧库存合并？
    - 加权平均法在调拨场景下如何计算？
```

### 7.3 根因分析

**根因 1**：缺少**统一的 Inventory Profile**
- 每张表独立管理自己的配置字段
- 无全局的配置字典和继承机制

**根因 2**：缺少**成本传递规则**
- 调拨时成本如何从仓库传递到门店未定义
- 门店成本与仓库成本的合并规则未定义

### 7.4 影响评估

| 受影响的场景 | 影响程度 | 具体表现 |
|-------------|----------|----------|
| 成本核算 | 🔴 严重 | 仓库和门店成本不一致 |
| 数据一致性 | 🟡 中等 | 字段名不一致，维护困难 |
| 配置管理 | 🟡 中等 | 安全库存字段名不同 |

---

## 八、Type Ambiguity: inventory_transactions 的语义过载

### 8.1 压力描述

`inventory_transactions` 表的 `transaction_type` 字段承担了**多种完全不同的语义**，且缺少门店维度的流水。

```
transaction_type 编码：
  1 = 采购入库    → 仓库库存增加
  2 = 销售出库    → 仓库库存减少（？门店销售呢？）
  3 = 调拨出      → 仓库库存减少
  4 = 调拨入      → 仓库库存增加（？门店调拨呢？）
  5 = 盘点盈      → 库存增加
  6 = 盘点亏      → 库存减少
  7 = 报损        → 库存减少
  8 = 退货        → 库存增加

问题：
  - type=2 "销售出库" 是仓库维度的还是门店维度的？
  - 门店销售扣减库存时，记录在哪张表？
  - 门店调拨入库时，记录在哪张表？
```

### 8.2 补丁代码

```java
// 门店销售扣减库存时，需要决定记录在哪张表
public void processSaleDeduction(Long materialId, Long storeId, int quantity) {
    // 方案1：记录在 inventory_transactions（但这是仓库维度的！）
    InventoryTransaction tx = new InventoryTransaction();
    tx.setMaterialId(materialId);
    tx.setWarehouseId(null);  // 门店没有 warehouse_id
    tx.setType(2);  // 销售出库
    tx.setQuantityChange(-quantity);
    inventoryTransactionMapper.insert(tx);
    // 问题：inventory_transactions 的 warehouse_id 不允许为 null

    // 方案2：不记录流水，直接更新 store_inventory
    storeInventoryMapper.deductStock(storeId, materialId, quantity);
    // 问题：没有事务流水，无法追溯

    // 方案3：创建单独的门店流水表（补丁）
    // 问题：又一个补丁表，与 inventory_transactions 的关系不明确
}
```

### 8.3 根因分析

`inventory_transactions` 表在设计时只考虑了**仓库维度**的库存变动，没有考虑**门店维度**的库存变动。导致：
1. 门店库存变动没有对应的流水表
2. transaction_type 的语义不明确（仓库？门店？）
3. 调拨操作需要同时更新两张表，无事务一致性

### 8.4 影响评估

| 受影响的场景 | 影响程度 | 具体表现 |
|-------------|----------|----------|
| 门店库存追溯 | 🔴 严重 | 门店库存变动无流水记录 |
| 库存审计 | 🔴 严重 | 门店库存变动无法审计 |
| 成本核算 | 🟡 中等 | 门店消耗成本无法自动追溯 |

---

## 九、Inventory 特有的压力点交叉分析

### 9.1 压力点关系图

```
                    ┌─────────────────────┐
                    │   Identity Stress    │
                    │ (material ↔ food)    │
                    └──────────┬──────────┘
                               │
                    ┌──────────┼──────────┐
                    ▼          ▼          ▼
            ┌──────────┐ ┌──────────┐ ┌──────────┐
            │   Role   │ │Relation- │ │   Type   │
            │ Conflict │ │ ship     │ │Ambiguity │
            │(双表结构) │ │Complexity│ │(语义过载)│
            └────┬─────┘ └────┬─────┘ └────┬─────┘
                 │            │            │
                 └────────────┼────────────┘
                              ▼
                    ┌─────────────────────┐
                    │  Data Duplication    │
                    │ (字段名不一致)        │
                    └──────────┬──────────┘
                               │
                    ┌──────────┼──────────┐
                    ▼          ▼          ▼
            ┌──────────┐ ┌──────────┐ ┌──────────┐
            │  Scope   │ │Inventory │ │  在途    │
            │ Mismatch │ │ 双表问题  │ │  缺失    │
            │(全局vs门店)│ │(无事务)  │ │          │
            └──────────┘ └──────────┘ └──────────┘
```

### 9.2 根因追溯

| 压力点 | 表层症状 | 深层根因 | 修复优先级 |
|--------|---------|---------|-----------|
| Identity Stress | food_id ↔ material_id 无映射 | 缺少 Canonical Item 映射表 | P0 |
| Role Conflict | inventory 与 store_inventory 结构不同 | 缺少统一位置模型 | P0 |
| Scope Mismatch | 安全库存无法按门店差异化 | 缺少 Profile + Scope 覆盖 | P1 |
| Relationship Complexity | 门店库存变动无流水 | 缺少门店维度事务流水 | P0 |
| Data Duplication | 字段名不一致，成本不一致 | 缺少统一 Inventory Profile | P1 |
| Type Ambiguity | transaction_type 语义过载 | 库存流水缺少位置维度 | P1 |
| 在途缺失 | 调拨无中间状态 | 缺少 in-transit 概念 | P2 |

### 9.3 系统性结论

**核心发现**：库存域的所有压力点都指向同一个根因——**库存模型缺少统一的位置抽象和身份关联**。

```
当前模型：
  inventory (仓库) ←──无事务──→ store_inventory (门店)
  inventory_transactions ←──只记录仓库──→ 门店无流水
  material_id ←──无映射──→ food_id

目标模型：
  Canonical Item → material_id (统一身份)
  Location → warehouse_id / store_id (统一位置)
  Inventory Instance → (canonical_item_id, location_id) (统一库存)
  Inventory Ledger → 所有库存变动的统一流水 (统一流水)
```

---

## 十、修复路径建议

### 10.1 修复优先级矩阵

| 优先级 | 压力点 | 修复策略 | 预期收益 |
|--------|--------|---------|---------|
| **P0** | Identity Stress | 建立 item_identity_map 映射表 | 消除身份分裂 |
| **P0** | Role Conflict | 统一 inventory_instance 表 | 消除双表问题 |
| **P0** | Relationship Complexity | 建立统一 inventory_ledger 流水表 | 完整追溯链 |
| **P1** | Scope Mismatch | 引入 stockable_profile + scope | 支持门店差异 |
| **P1** | Data Duplication | 统一字段命名和成本模型 | 消除数据冗余 |
| **P1** | Type Ambiguity | 在 transaction_type 中增加 location_type 维度 | 明确语义 |
| **P2** | 在途缺失 | 引入 in-transit 状态 | 完善调拨流程 |

### 10.2 渐进式修复路径

```
Phase 1: 建立身份映射 (2周)
├── 创建 item_identity_map 表
├── 回填预包装品的 food_id ↔ material_id 映射
├── 验证 POS 销售 → 库存扣减链路
└── 标记 Inventory.getProductId() 为 @Deprecated

Phase 2: 统一库存表结构 (3周)
├── 创建 inventory_instance 统一库存表
├── 统一字段命名 (safety_stock / max_stock)
├── 迁移 inventory + store_inventory → inventory_instance
└── 验证事务一致性

Phase 3: 统一库存流水 (3周)
├── 创建 inventory_ledger 统一流水表
├── 增加 location_type 维度
├── 迁移 inventory_transactions → inventory_ledger
├── 补充门店维度流水
└── 验证完整追溯链

Phase 4: 引入 Profile 层 (2周)
├── 创建 stockable_profile 表
├── 支持门店级别配置覆盖
├── 统一成本核算规则
└── 验证门店差异化配置
```

---

## 十一、总结

### 11.1 核心发现

1. **库存域有 7 个压力点**，其中 3 个 P0、3 个 P1、1 个 P2
2. **所有压力点都指向同一个根因**：库存模型缺少统一的位置抽象和身份关联
3. **最严重的压力点**：Identity Stress（food_id ↔ material_id 无映射）、Role Conflict（双表结构）、Relationship Complexity（门店库存无流水）
4. **修复的核心**：统一位置模型 + 统一流水表 + 身份映射

### 11.2 修复原则

1. **先统一身份，再统一结构**：item_identity_map 是所有修复的基础
2. **先补充流水，再统一表结构**：inventory_ledger 比 inventory_instance 更紧迫
3. **渐进式迁移**：先建立新表，再迁移数据，最后切换读写

### 11.3 风险提示

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 数据迁移复杂度高 | 业务中断 | 分阶段迁移，保留回滚能力 |
| 流水表数据量大 | 性能下降 | 分区表，定期归档 |
| 门店流水缺失 | 无法追溯 | 先补充流水，再切换读写 |
| 成本模型变更 | 财务影响 | 保持成本计算逻辑不变，只统一数据结构 |

---

**状态**：ANALYSIS, NOT CONFIRMED
**日期**：2026-09-10
**阶段**：Phase 22 Extension - Inventory Model Stress Points
**依赖**：Phase 21 Model Stress Points, Phase 18/20 Inventory Identity Analysis
