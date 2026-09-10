# 库存状态模型分析 (Phase 14 - Inventory State Model)

## 核心问题：库存状态的语义层次

> ON_HAND, AVAILABLE, RESERVED, DAMAGED, EXPIRED, IN_TRANSIT, BLOCKED 这些概念到底属于 Quantity Dimension、State、Location、Movement 哪个层级？

**关键约束**：
- 不能把所有东西做成一个 status 字段
- 必须分析哪些是 Inventory Fact 的属性？
- 必须分析哪些是 Movement 的结果？
- 必须分析哪些是 Derived View？

---

## 1. 当前系统现实

### 1.1 现有字段分析

```
┌─────────────────────────────────────────────────────────────────┐
│                     当前系统库存状态相关字段                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  inventory 表                                                   │
│  ├── current_stock (BigDecimal)                                 │
│  │   └── 语义: 当前库存数量 (On-hand Quantity)                   │
│  ├── locked_quantity (BigDecimal)                               │
│  │   └── 语义: 锁定数量 (Reserved Quantity)                     │
│  ├── min_safe_qty (BigDecimal)                                  │
│  │   └── 语义: 安全库存阈值 (Configuration)                     │
│  └── max_stock_qty (BigDecimal)                                 │
│      └── 语义: 最大库存容量 (Configuration)                     │
│                                                                 │
│  inventory_transactions 表                                      │
│  └── transaction_type (INTEGER)                                 │
│      └── 语义: 变动类型 (Movement Type)                         │
│                                                                 │
│  ❌ 缺失字段:                                                    │
│  ├── 无 inventory_status 字段                                    │
│  ├── 无 condition_state 字段                                     │
│  └── 无 transit_quantity 字段                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 当前系统的隐含状态

| 概念 | 当前实现 | 存储位置 | 问题 |
|------|----------|----------|------|
| **On-hand (在手)** | current_stock | inventory 表 | ✅ 已实现 |
| **Available (可用)** | 需计算: current_stock - locked_quantity | 无存储 | ⚠️ 需实时计算 |
| **Reserved (预留)** | locked_quantity | inventory 表 | ⚠️ 无 unlock 流水 |
| **Damaged (损坏)** | ❌ 无 | 无 | ❌ 缺失 |
| **Expired (过期)** | ❌ 无 | 无 | ❌ 缺失 |
| **In-transit (在途)** | ❌ 无 | 无 | ❌ 缺失 |
| **Blocked (冻结)** | ❌ 无 | 无 | ❌ 缺失 |

---

## 2. 状态语义分类框架

### 2.1 四个语义层级

```
┌─────────────────────────────────────────────────────────────────┐
│                    库存状态语义层级                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Layer 1: Quantity Dimension (数量维度)                         │
│  ┌──────────────────────────────────────────────────────┐      │
│  │ - On-hand Quantity (在手数量)                         │      │
│  │ - Available Quantity (可用数量)                       │      │
│  │ - Reserved Quantity (预留数量)                        │      │
│  │ - In-transit Quantity (在途数量)                      │      │
│  │ 特性: 描述库存数量的不同视角                           │      │
│  └──────────────────────────────────────────────────────┘      │
│                           ↓                                     │
│  Layer 2: Condition State (状态层)                              │
│  ┌──────────────────────────────────────────────────────┐      │
│  │ - GOOD (正常)                                         │      │
│  │ - DAMAGED (损坏)                                      │      │
│  │ - EXPIRED (过期)                                      │      │
│  │ - BLOCKED (冻结)                                      │      │
│  │ 特性: 描述库存的可用性状态                             │      │
│  └──────────────────────────────────────────────────────┘      │
│                           ↓                                     │
│  Layer 3: Movement Result (变动结果层)                          │
│  ┌──────────────────────────────────────────────────────┐      │
│  │ - RECEIPT (入库)                                      │      │
│  │ - SALE (出库)                                         │      │
│  │ - TRANSFER (调拨)                                     │      │
│  │ - WASTE (报损)                                        │      │
│  │ 特性: 库存数量变动的结果                               │      │
│  └──────────────────────────────────────────────────────┘      │
│                           ↓                                     │
│  Layer 4: Derived View (派生视图层)                             │
│  ┌──────────────────────────────────────────────────────┐      │
│  │ - USABLE_STOCK (可使用库存)                           │      │
│  │ - UNUSABLE_STOCK (不可使用库存)                       │      │
│  │ - TOTAL_STOCK (总库存)                                │      │
│  │ 特性: 从多个维度聚合计算的视图                         │      │
│  └──────────────────────────────────────────────────────┘      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 各层级详细分析

#### Layer 1: Quantity Dimension (数量维度)

**定义**：描述库存数量的不同视角，是同一库存事实的不同投影。

| Quantity Dimension | 计算方式 | 是否存储 | 说明 |
|-------------------|----------|----------|------|
| **On-hand** | current_stock | ✅ 存储 | 当前实际持有的库存数量 |
| **Available** | current_stock - locked_quantity | ❌ 不存储 | 可供销售/调拨的库存数量 |
| **Reserved** | locked_quantity | ✅ 存储 | 已被订单预留的库存数量 |
| **In-transit** | 从采购单/调拨单计算 | ❌ 不存储 | 在途但未入库的库存数量 |

**关键特性**：
- Quantity Dimension 是同一库存事实的不同视角
- 不是独立的"状态"，而是数量的不同计算方式
- 大部分是 Derived View，可从 Truth 重建

#### Layer 2: Condition State (状态层)

**定义**：描述库存的可用性状态，影响库存是否可以被使用。

| Condition State | 描述 | 影响 | 触发条件 |
|-----------------|------|------|----------|
| **GOOD** | 正常可用 | 可销售、可调拨 | 入库时默认状态 |
| **DAMAGED** | 损坏不可用 | 不可销售、需报损 | 检验发现损坏 |
| **EXPIRED** | 过期不可用 | 不可销售、需报损 | 系统检测过期 |
| **BLOCKED** | 冻结不可用 | 不可销售、不可调拨 | 质检/审计冻结 |

**关键特性**：
- Condition State 是库存的属性，影响可用性
- 状态变化不直接改变数量，但影响库存是否可用
- 状态变化可能触发 Movement（如 EXPIRED → WASTE）

#### Layer 3: Movement Result (变动结果层)

**定义**：库存数量变动的结果，是 Movement 的直接产出。

| Movement | 数量影响 | 状态影响 | 说明 |
|----------|----------|----------|------|
| **RECEIPT** | On-hand +N | 无 | 入库增加在手数量 |
| **SALE** | On-hand -N, Reserved -N | 无 | 出库减少在手和预留 |
| **TRANSFER** | 源 -N, 目标 +N | 无 | 调拨不改变总量 |
| **WASTE** | On-hand -N | 无 | 报损减少在手数量 |

**关键特性**：
- Movement Result 是 Movement 的直接产出
- 不是独立的"状态"，而是 Movement 的结果
- 应该通过 Movement 流水记录，而不是存储为状态

#### Layer 4: Derived View (派生视图层)

**定义**：从多个维度聚合计算的视图，不是独立的存储。

| Derived View | 计算方式 | 说明 |
|--------------|----------|------|
| **USABLE_STOCK** | SUM(On-hand WHERE State=GOOD) | 可使用的库存总量 |
| **UNUSABLE_STOCK** | SUM(On-hand WHERE State IN (DAMAGED, EXPIRED, BLOCKED)) | 不可使用的库存总量 |
| **TOTAL_STOCK** | SUM(On-hand) | 库存总量 |

**关键特性**：
- Derived View 是计算结果，不是独立的存储
- 可以从 Truth 重建
- 不应该存储为独立字段

---

## 3. 深度分析：各状态概念的归属

### 3.1 ON_HAND (在手数量)

```
┌─────────────────────────────────────────────────────────────────┐
│                     ON_HAND 语义分析                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  概念: 当前实际持有的库存数量                                     │
│                                                                 │
│  语义层级: Quantity Dimension                                    │
│                                                                 │
│  当前实现:                                                       │
│  ├── inventory.current_stock                                     │
│  ├── 分类: Derived (可从流水重建)                                │
│  └── 存储: ✅ 已存储                                             │
│                                                                 │
│  推荐:                                                           │
│  ├── 保持 current_stock 作为 Derived 缓存                       │
│  ├── 确保所有变更都通过 inventory_transactions 记录              │
│  └── 定期从流水重建校验                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 AVAILABLE (可用数量)

```
┌─────────────────────────────────────────────────────────────────┐
│                     AVAILABLE 语义分析                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  概念: 可供销售/调拨的库存数量                                    │
│                                                                 │
│  语义层级: Quantity Dimension (View)                            │
│                                                                 │
│  计算方式:                                                       │
│  ├── AVAILABLE = current_stock - locked_quantity                │
│  └── 或: AVAILABLE = On-hand WHERE State = GOOD                 │
│                                                                 │
│  当前实现:                                                       │
│  ├── ❌ 无存储字段                                               │
│  ├── 需要实时计算                                                │
│  └── 存储: ❌ 未存储                                             │
│                                                                 │
│  推荐:                                                           │
│  ├── 不存储为独立字段                                            │
│  ├── 作为 View 实时计算                                          │
│  └── 或存储为物化视图用于查询优化                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.3 RESERVED (预留数量)

```
┌─────────────────────────────────────────────────────────────────┐
│                     RESERVED 语义分析                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  概念: 已被订单预留的库存数量                                     │
│                                                                 │
│  语义层级: Quantity Dimension (Operational State)               │
│                                                                 │
│  当前实现:                                                       │
│  ├── inventory.locked_quantity                                   │
│  ├── 分类: Operational State (可变)                             │
│  ├── 问题: 无对应的 unlock 流水记录                              │
│  └── 存储: ✅ 已存储                                             │
│                                                                 │
│  推荐:                                                           │
│  ├── 保持 locked_quantity 作为 Operational State                │
│  ├── 新增 LOCK/UNLOCK Movement 类型                             │
│  ├── locked_quantity 的增减必须有流水记录                        │
│  └── 定期从流水重建校验                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.4 DAMAGED (损坏)

```
┌─────────────────────────────────────────────────────────────────┐
│                     DAMAGED 语义分析                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  概念: 库存因损坏而不可使用                                       │
│                                                                 │
│  语义层级: Condition State                                       │
│                                                                 │
│  当前实现:                                                       │
│  ├── ❌ 无存储字段                                               │
│  ├── 无状态变化记录                                              │
│  └── 存储: ❌ 未存储                                             │
│                                                                 │
│  推荐:                                                           │
│  ├── 方案 A: 新增 condition_state 字段                          │
│  │   └── inventory.condition_state = 'GOOD' | 'DAMAGED' | ...  │
│  ├── 方案 B: 通过 Movement 实现                                 │
│  │   └── DAMAGED → WASTE Movement (直接报损)                    │
│  └── 建议: 餐饮场景下，方案 B 更简洁                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.5 EXPIRED (过期)

```
┌─────────────────────────────────────────────────────────────────┐
│                     EXPIRED 语义分析                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  概念: 库存因过期而不可使用                                       │
│                                                                 │
│  语义层级: Condition State                                       │
│                                                                 │
│  当前实现:                                                       │
│  ├── ❌ 无存储字段                                               │
│  ├── 有 expiry_date 字段 (可计算)                               │
│  └── 存储: ❌ 未存储                                             │
│                                                                 │
│  推荐:                                                           │
│  ├── 不存储为独立状态字段                                        │
│  ├── 通过 expiry_date 计算是否过期                              │
│  ├── 过期后触发 WASTE Movement                                  │
│  └── 定期扫描过期库存并生成报损单                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.6 IN_TRANSIT (在途)

```
┌─────────────────────────────────────────────────────────────────┐
│                     IN_TRANSIT 语义分析                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  概念: 已下单但未入库的库存                                       │
│                                                                 │
│  语义层级: Quantity Dimension (View)                            │
│                                                                 │
│  当前实现:                                                       │
│  ├── ❌ 无存储字段                                               │
│  ├── 需从采购单/调拨单计算                                       │
│  └── 存储: ❌ 未存储                                             │
│                                                                 │
│  推荐:                                                           │
│  ├── 不存储为独立字段                                            │
│  ├── 作为 View 从采购单/调拨单计算                              │
│  ├── 或引入 TRANSIT Location Type                               │
│  └── 建议: 简单场景下，作为 View 计算即可                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.7 BLOCKED (冻结)

```
┌─────────────────────────────────────────────────────────────────┐
│                     BLOCKED 语义分析                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  概念: 库存因质检/审计等原因被冻结                                │
│                                                                 │
│  语义层级: Condition State                                       │
│                                                                 │
│  当前实现:                                                       │
│  ├── ❌ 无存储字段                                               │
│  ├── 无状态变化记录                                              │
│  └── 存储: ❌ 未存储                                             │
│                                                                 │
│  推荐:                                                           │
│  ├── 方案 A: 新增 condition_state 字段                          │
│  ├── 方案 B: 通过 locked_quantity 模拟                          │
│  │   └── BLOCKED = locked_quantity (但语义不同)                 │
│  └── 建议: 餐饮场景下，BLOCKED 需求不强，可暂不实现             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. 推荐的库存状态模型

### 4.1 核心原则

| 原则 | 说明 |
|------|------|
| **Quantity Dimension 不是 Status** | On-hand、Available、Reserved 是数量的不同视角，不是独立状态 |
| **Condition State 影响可用性** | GOOD、DAMAGED、EXPIRED、BLOCKED 影响库存是否可用 |
| **Movement Result 不存储** | 库存变动的结果通过流水记录，不存储为状态 |
| **Derived View 不存储** | 派生视图可从 Truth 重建，不存储为独立字段 |

### 4.2 推荐的库存状态模型

```
┌─────────────────────────────────────────────────────────────────┐
│                  推荐的库存状态模型                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Quantity Dimension (数量维度) - 不是 Status                    │
│  ├── On-hand: current_stock (Derived)                          │
│  ├── Available: current_stock - locked_quantity (View)          │
│  ├── Reserved: locked_quantity (Operational State)             │
│  └── In-transit: 从采购单/调拨单计算 (View)                    │
│                                                                 │
│  Condition State (状态层) - 影响可用性                          │
│  ├── condition_state: 'GOOD' | 'DAMAGED' | 'EXPIRED' | 'BLOCKED'│
│  ├── 默认值: 'GOOD'                                             │
│  └── 状态变化: 通过 Movement 记录                              │
│                                                                 │
│  Movement Result (变动结果层) - 通过流水记录                    │
│  ├── 入库: RECEIPT → On-hand +N                                │
│  ├── 出库: SALE → On-hand -N, Reserved -N                      │
│  ├── 调拨: TRANSFER → 源 -N, 目标 +N                           │
│  └── 报损: WASTE → On-hand -N                                  │
│                                                                 │
│  Derived View (派生视图层) - 不存储                            │
│  ├── USABLE_STOCK: On-hand WHERE State = GOOD                  │
│  ├── UNUSABLE_STOCK: On-hand WHERE State IN (DAMAGED, EXPIRED) │
│  └── TOTAL_STOCK: On-hand                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.3 数据模型建议

```sql
-- 推荐的 inventory 表结构
CREATE TABLE inventory (
    inventory_id BIGINT PRIMARY KEY,
    material_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    
    -- Quantity Dimension
    current_stock DECIMAL(15,3) DEFAULT 0,  -- On-hand (Derived)
    locked_quantity DECIMAL(15,3) DEFAULT 0, -- Reserved (Operational State)
    
    -- Condition State (可选，餐饮场景下可能不需要)
    condition_state VARCHAR(20) DEFAULT 'GOOD',  -- 'GOOD', 'DAMAGED', 'EXPIRED', 'BLOCKED'
    
    -- Configuration
    min_safe_qty DECIMAL(15,3),
    max_stock_qty DECIMAL(15,3),
    
    -- Batch/Lot (Phase 15 分析)
    batch_no VARCHAR(50),
    production_date DATE,
    expiry_date DATE,
    
    -- Cost
    unit_cost DECIMAL(12,2),
    total_cost DECIMAL(12,2),
    
    -- Metadata
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INTEGER DEFAULT 0,
    
    UNIQUE(material_id, warehouse_id, batch_no)
);
```

---

## 5. 餐饮 ERP 场景分析

### 5.1 餐饮业务的特点

| 特点 | 影响 | 建议 |
|------|------|------|
| **周转快** | 库存状态变化频繁 | 状态模型应简洁 |
| **保质期短** | 过期是常态 | 过期处理应自动化 |
| **损耗高** | 损坏/过期常见 | 损耗处理应便捷 |
| **批次管理** | 需要追踪批次 | 批次是核心维度 |
| **整箱采购** | 需要拆包 | 拆包是常见操作 |

### 5.2 餐饮场景下的状态需求

| 状态概念 | 餐饮需求 | 当前实现 | 建议 |
|----------|----------|----------|------|
| **On-hand** | ✅ 必须 | ✅ current_stock | 保持 |
| **Available** | ✅ 必须 | ⚠️ 需计算 | 作为 View |
| **Reserved** | ✅ 必须 | ⚠️ locked_quantity | 保持，补充流水 |
| **Damaged** | ⚠️ 视业务 | ❌ 缺失 | 通过 WASTE 实现 |
| **Expired** | ✅ 必须 | ⚠️ 有 expiry_date | 通过 WASTE 实现 |
| **In-transit** | ⚠️ 视业务 | ❌ 缺失 | 作为 View 计算 |
| **Blocked** | ❌ 不需要 | ❌ 缺失 | 暂不实现 |

### 5.3 推荐的餐饮库存状态方案

```
┌─────────────────────────────────────────────────────────────────┐
│              餐饮 ERP 推荐的库存状态方案                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  核心状态 (必须实现):                                            │
│  ├── On-hand: current_stock (Derived)                          │
│  ├── Available: current_stock - locked_quantity (View)          │
│  └── Reserved: locked_quantity (Operational State)             │
│                                                                 │
│  条件状态 (通过 Movement 实现):                                 │
│  ├── Damaged → WASTE Movement                                  │
│  ├── Expired → WASTE Movement (定期扫描)                       │
│  └── Blocked → 暂不实现                                        │
│                                                                 │
│  在途状态 (通过 View 计算):                                     │
│  └── In-transit → 从采购单/调拨单计算                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. 决策矩阵

| 状态概念 | 语义层级 | 当前实现 | 建议 | 理由 |
|----------|----------|----------|------|------|
| **On-hand** | Quantity Dimension (Derived) | ✅ current_stock | 保持 | 核心数量 |
| **Available** | Quantity Dimension (View) | ❌ 需计算 | 不存储 | 可实时计算 |
| **Reserved** | Quantity Dimension (Operational) | ⚠️ locked_quantity | 保持，补充流水 | 核心数量 |
| **Damaged** | Condition State | ❌ 缺失 | 通过 WASTE 实现 | 餐饮场景下损耗直接报损 |
| **Expired** | Condition State | ⚠️ 有 expiry_date | 通过 WASTE 实现 | 过期直接报损 |
| **In-transit** | Quantity Dimension (View) | ❌ 缺失 | 作为 View 计算 | 简单场景不需要独立存储 |
| **Blocked** | Condition State | ❌ 缺失 | 暂不实现 | 餐饮场景下需求不强 |

---

## 7. 结论

### 7.1 核心结论

| 结论 | 说明 |
|------|------|
| **Quantity Dimension 不是 Status** | On-hand、Available、Reserved 是数量的不同视角，不是独立状态 |
| **Condition State 影响可用性** | GOOD、DAMAGED、EXPIRED、BLOCKED 影响库存是否可用 |
| **Movement Result 不存储** | 库存变动的结果通过流水记录，不存储为状态 |
| **Derived View 不存储** | 派生视图可从 Truth 重建，不存储为独立字段 |

### 7.2 餐饮场景建议

| 建议 | 说明 |
|------|------|
| **简化状态模型** | 餐饮场景下，状态模型应简洁 |
| **通过 Movement 实现** | Damaged、Expired 通过 WASTE Movement 实现 |
| **不存储 Condition State** | 餐饮场景下，condition_state 字段可能不需要 |
| **保留核心数量** | On-hand、Available、Reserved 是核心数量 |

### 7.3 迁移路径

**短期（保持现状）**：
- 承认 locked_quantity 是 Operational State
- 通过应用层模拟 Available 计算

**中期（补充流水）**：
- 新增 LOCK/UNLOCK Movement 类型
- locked_quantity 的增减必须有流水记录

**长期（可选优化）**：
- 如果业务需要，新增 condition_state 字段
- 实现 Damaged、Expired、Blocked 的状态管理

---

**状态**：ANALYSIS, NOT CONFIRMED
**日期**：2026-09-10
**阶段**：Phase 14 - Inventory State Model Analysis