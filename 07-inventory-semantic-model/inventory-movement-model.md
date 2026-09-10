# 库存变动模型分析 (Phase 10 - Inventory Movement Model)

> **核心问题**：库存变动（Inventory Movement）的语义层次是什么？
> **状态**：ANALYSIS, NOT CONFIRMED

---

## 1. 问题域

本分析必须回答：
1. 什么是 Inventory Movement？它与 Business Event、State Change、Derived Result 的关系是什么？
2. 当前系统的 transaction_type 枚举是否完整？是否需要扩展？
3. 如何建立标准的库存变动类型体系？

**关键约束**：
- 不要假设当前枚举就是最终枚举
- 必须分析 Movement 的语义层次
- 不要把库存简单等同于"商品数量"

---

## 2. 当前系统现实

### 2.1 现有 transaction_type 枚举

```java
// 当前系统的 inventory_transactions.transaction_type
1: 采购入库 (PURCHASE_RECEIPT)
2: 销售出库 (SALE)
3: 调拨出 (TRANSFER_OUT)
4: 调拨入 (TRANSFER_IN)
5: 盘点盈 (STOCKTAKE_GAIN)
6: 盘点亏 (STOCKTAKE_LOSS)
7: 报损 (WASTE)
8: 退货 (RETURN)
```

### 2.2 当前系统的数据模型

```
inventory_transactions (
  transaction_id BIGINT PRIMARY KEY,
  transaction_type INTEGER,           -- 1-8
  inventory_id BIGINT,                -- 关联库存实例
  material_id BIGINT,                 -- 冗余字段，便于查询
  warehouse_id BIGINT,                -- 仓库ID
  quantity_change DECIMAL(15,3),      -- 变动数量（正=增加，负=减少）
  before_qty DECIMAL(15,3),           -- 变动前数量
  after_qty DECIMAL(15,3),            -- 变动后数量
  unit_cost DECIMAL(12,2),            -- 单位成本
  total_cost DECIMAL(12,2),           -- 总成本
  reference_no VARCHAR(50),           -- 关联单据号
  reference_type VARCHAR(50),         -- 关联单据类型
  create_user_id BIGINT,              -- 操作人
  create_time TIMESTAMP,              -- 创建时间
  remark VARCHAR(500)                 -- 备注
)
```

### 2.3 当前系统的问题

| 问题 | 现状 | 风险 |
|------|------|------|
| 事务类型不完整 | 只有 8 种类型 | 无法支持复杂的库存变动场景 |
| 语义层次不清晰 | 所有类型平等对待 | 无法区分 Business Event 和 Inventory Movement |
| 缺少逆向操作 | 没有 REVERSAL 类型 | 无法支持取消/冲销操作 |
| 缺少状态变化 | 没有状态变化的记录 | 无法追踪库存状态变化 |

---

## 3. 语义层次分析

### 3.1 四个语义层次

```
┌─────────────────────────────────────────────────────────────┐
│                    Inventory Movement 语义层次               │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Layer 1: Business Event (业务事件层)                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ - 采购订单确认                                        │   │
│  │ - 销售订单创建                                        │   │
│  │ - 调拨申请审批                                        │   │
│  │ - 盘点任务创建                                        │   │
│  │ 特性：业务语义，触发库存变动                           │   │
│  └──────────────────────────────────────────────────────┘   │
│                           ↓                                 │
│  Layer 2: Inventory Movement (库存变动层)                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ - RECEIPT (入库)                                      │   │
│  │ - TRANSFER_IN (调拨入库)                              │   │
│  │ - TRANSFER_OUT (调拨出库)                             │   │
│  │ - SALE (销售出库)                                     │   │
│  │ - CONSUMPTION (消耗)                                  │   │
│  │ - WASTE (报损)                                        │   │
│  │ - ADJUSTMENT (调整)                                   │   │
│  │ - STOCKTAKE (盘点)                                    │   │
│  │ - RETURN (退货)                                       │   │
│  │ 特性：库存数量变动，可追踪                             │   │
│  └──────────────────────────────────────────────────────┘   │
│                           ↓                                 │
│  Layer 3: State Change (状态变化层)                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ - LOCKED (锁定)                                       │   │
│  │ - UNLOCKED (解锁)                                     │   │
│  │ - EXPIRED (过期)                                      │   │
│  │ - DAMAGED (损坏)                                      │   │
│  │ 特性：库存状态变化，不直接改变数量                     │   │
│  └──────────────────────────────────────────────────────┘   │
│                           ↓                                 │
│  Layer 4: Derived Result (派生结果层)                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ - AVAILABLE_STOCK (可用库存)                          │   │
│  │ - LOCKED_STOCK (锁定库存)                             │   │
│  │ - TOTAL_STOCK (总库存)                                │   │
│  │ - STOCK_VALUE (库存价值)                              │   │
│  │ 特性：从流水聚合计算，不是独立的变动                   │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 各层次的详细分析

#### Layer 1: Business Event (业务事件层)

**定义**：业务事件是触发库存变动的业务操作，本身不直接修改库存数量。

**典型事件**：
| 事件 | 描述 | 触发的 Movement |
|------|------|-----------------|
| 采购订单确认 | 供应商发货 | 无（等待收货） |
| 收货确认 | 收到货物 | RECEIPT |
| 销售订单创建 | 客户下单 | 无（等待发货） |
| 发货确认 | 发出货物 | SALE |
| 调拨申请 | 申请调拨 | 无（等待审批） |
| 调拨审批通过 | 审批通过 | TRANSFER_OUT + TRANSFER_IN |
| 盘点任务创建 | 创建盘点任务 | 无（等待盘点） |
| 盘点完成 | 完成盘点 | STOCKTAKE |

**关键特性**：
- 业务事件是 Movement 的触发器
- 一个 Business Event 可能触发多个 Movement
- Business Event 有独立的生命周期（创建→审批→完成）

#### Layer 2: Inventory Movement (库存变动层)

**定义**：Inventory Movement 是库存数量的直接变动，是库存流水的基本单位。

**标准 Movement 类型**：
| Movement | 描述 | 数量变化 | 方向 |
|----------|------|----------|------|
| RECEIPT | 采购入库 | + | 入库 |
| TRANSFER_IN | 调拨入库 | + | 入库 |
| TRANSFER_OUT | 调拨出库 | - | 出库 |
| SALE | 销售出库 | - | 出库 |
| CONSUMPTION | 消耗（厨房领料） | - | 出库 |
| WASTE | 报损 | - | 出库 |
| ADJUSTMENT | 调整（修正） | +/- | 双向 |
| STOCKTAKE | 盘点（盈亏） | +/- | 双向 |
| RETURN | 退货 | + | 入库 |

**关键特性**：
- Movement 是库存流水的基本单位
- 每个 Movement 都有明确的方向（入库/出库）
- Movement 是不可变的（一旦记录不可修改）

#### Layer 3: State Change (状态变化层)

**定义**：State Change 是库存状态的变化，不直接改变数量，但影响库存的可用性。

**典型状态变化**：
| 状态变化 | 描述 | 影响 |
|----------|------|------|
| LOCKED | 锁定库存 | 库存不可用 |
| UNLOCKED | 解锁库存 | 库存恢复可用 |
| EXPIRED | 过期 | 库存不可用，需要报损 |
| DAMAGED | 损坏 | 库存不可用，需要报损 |

**关键特性**：
- State Change 不直接改变数量
- State Change 影响库存的可用性
- State Change 可能触发 Movement（如 EXPIRED → WASTE）

#### Layer 4: Derived Result (派生结果层)

**定义**：Derived Result 是从流水聚合计算的结果，不是独立的变动。

**典型 Derived Result**：
| Derived Result | 计算方式 | 用途 |
|----------------|----------|------|
| AVAILABLE_STOCK | 当前库存 - 锁定库存 | 可销售/可调拨 |
| LOCKED_STOCK | SUM(锁定数量) | 已锁定但未出库 |
| TOTAL_STOCK | SUM(所有库存) | 总库存统计 |
| STOCK_VALUE | SUM(库存 × 单位成本) | 库存价值统计 |

**关键特性**：
- Derived Result 是计算结果，不是独立的变动
- Derived Result 可以从流水重建
- Derived Result 不应该存储为独立字段

---

## 4. 候选 Movement 类型分析

### 4.1 完整的 Movement 类型列表

```
┌─────────────────────────────────────────────────────────────┐
│              Inventory Movement Types                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  入库类 (Inbound)                                            │
│  ├── RECEIPT          采购入库                                │
│  ├── TRANSFER_IN      调拨入库                               │
│  ├── RETURN           退货入库                               │
│  ├── PRODUCTION_IN    生产入库（半成品/成品）                 │
│  └── ADJUSTMENT_IN    盘点盈余                               │
│                                                              │
│  出库类 (Outbound)                                           │
│  ├── SALE             销售出库                               │
│  ├── TRANSFER_OUT     调拨出库                               │
│  ├── CONSUMPTION      消耗（厨房领料）                       │
│  ├── WASTE            报损                                   │
│  └── ADJUSTMENT_OUT   盘点亏损                               │
│                                                              │
│  内部类 (Internal)                                           │
│  ├── LOCK             锁定（不改变数量）                     │
│  ├── UNLOCK           解锁（不改变数量）                     │
│  ├── EXPIRE           过期（不改变数量，触发报损）            │
│  └── DAMAGE           损坏（不改变数量，触发报损）            │
│                                                              │
│  逆向类 (Reversal)                                           │
│  ├── REVERSAL         冲销（撤销之前的 Movement）            │
│  └── CANCEL           取消（撤销未完成的 Movement）          │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 当前系统与目标系统的映射

| 当前 transaction_type | 目标 Movement Type | 分析 |
|----------------------|-------------------|------|
| 1: 采购入库 | RECEIPT | ✅ 直接映射 |
| 2: 销售出库 | SALE | ✅ 直接映射 |
| 3: 调拨出 | TRANSFER_OUT | ✅ 直接映射 |
| 4: 调拨入 | TRANSFER_IN | ✅ 直接映射 |
| 5: 盘点盈 | STOCKTAKE (ADJUSTMENT_IN) | ⚠️ 需要细化 |
| 6: 盘点亏 | STOCKTAKE (ADJUSTMENT_OUT) | ⚠️ 需要细化 |
| 7: 报损 | WASTE | ✅ 直接映射 |
| 8: 退货 | RETURN | ✅ 直接映射 |

**缺失的类型**：
- CONSUMPTION (消耗) - 厨房领料
- LOCK / UNLOCK (锁定/解锁)
- EXPIRE / DAMAGE (过期/损坏)
- REVERSAL / CANCEL (冲销/取消)
- PRODUCTION_IN (生产入库)

---

## 5. 深度分析：哪些是 Business Event？哪些是 Movement？

### 5.1 Business Event vs Inventory Movement

| 概念 | Business Event | Inventory Movement |
|------|----------------|-------------------|
| **定义** | 触发库存变动的业务操作 | 库存数量的直接变动 |
| **粒度** | 业务级（订单、任务） | 库存级（单笔出入库） |
| **数量** | 一个 Event 可能触发多个 Movement | 一个 Movement 只记录一次变动 |
| **生命周期** | 创建→审批→完成→关闭 | 记录→不可修改 |
| **可逆性** | 可以取消/撤销 | 不可以修改，只能冲销 |

**示例**：
```
Business Event: 采购订单 PO-001
  │
  ├── Movement 1: RECEIPT (入库 100 瓶)
  │     ├── material_id = 可乐330ml
  │     ├── quantity_change = +100
  │     ├── before_qty = 200
  │     └── after_qty = 300
  │
  └── Movement 2: RECEIPT (入库 50 瓶)
        ├── material_id = 矿泉水500ml
        ├── quantity_change = +50
        ├── before_qty = 100
        └── after_qty = 150

结论：一个 Business Event (PO-001) 触发了两个 Inventory Movement
```

### 5.2 Inventory Movement vs State Change

| 概念 | Inventory Movement | State Change |
|------|-------------------|--------------|
| **定义** | 库存数量的直接变动 | 库存状态的变化 |
| **数量影响** | 直接改变数量 | 不直接改变数量 |
| **可追踪性** | 有独立的流水记录 | 可能没有独立记录 |
| **触发关系** | 可能触发 State Change | 可能触发 Movement |

**示例**：
```
State Change: 过期 (EXPIRE)
  │
  ├── 不直接改变数量
  │     └── 库存数量不变，但状态变为"不可用"
  │
  └── 可能触发 Movement
        └── WASTE (报损)
              ├── quantity_change = -100
              ├── before_qty = 100
              └── after_qty = 0

结论：State Change (EXPIRE) 不直接改变数量，但可能触发 Movement (WASTE)
```

### 5.3 Inventory Movement vs Derived Result

| 概念 | Inventory Movement | Derived Result |
|------|-------------------|----------------|
| **定义** | 库存数量的直接变动 | 从流水聚合计算的结果 |
| **数据来源** | 直接记录 | 从流水计算 |
| **可修改性** | 不可修改（只能冲销） | 可以重新计算 |
| **存储方式** | 独立的流水记录 | 可以存储为物化视图 |

**示例**：
```
Derived Result: AVAILABLE_STOCK (可用库存)
  │
  ├── 计算方式
  │     └── AVAILABLE_STOCK = current_stock - locked_quantity
  │
  ├── 数据来源
  │     ├── current_stock = 从流水聚合
  │     └── locked_quantity = 从流水聚合
  │
  └── 存储方式
        └── 可以存储为物化视图，但逻辑上是计算结果

结论：Derived Result 是从流水计算的结果，不是独立的变动
```

---

## 6. Movement 类型的语义分析

### 6.1 RECEIPT (采购入库)

```
Business Event: 采购订单收货确认
  │
  ├── 触发条件
  │     ├── 采购订单已审批
  │     ├── 货物已收到
  │     └── 质检已通过
  │
  ├── Movement 语义
  │     ├── 数量变化: +N
  │     ├── 成本变化: +N × unit_cost
  │     └── 状态变化: 无
  │
  └── 关联数据
        ├── reference_no = 采购订单号
        ├── reference_type = 'PURCHASE_ORDER'
        └── unit_cost = 采购单价
```

### 6.2 SALE (销售出库)

```
Business Event: 销售订单发货确认
  │
  ├── 触发条件
  │     ├── 销售订单已确认
  │     ├── 货物已拣货
  │     └── 货物已发货
  │
  ├── Movement 语义
  │     ├── 数量变化: -N
  │     ├── 成本变化: -N × unit_cost (成本结转)
  │     └── 收入变化: +N × sale_price (销售收入)
  │
  └── 关联数据
        ├── reference_no = 销售订单号
        ├── reference_type = 'SALES_ORDER'
        └── unit_cost = 销售成本
```

### 6.3 CONSUMPTION (消耗)

```
Business Event: 厨房领料
  │
  ├── 触发条件
  │     ├── 菜品订单已创建
  │     ├── 厨房已接单
  │     └── 领料操作已执行
  │
  ├── Movement 语义
  │     ├── 数量变化: -N (按配方比例)
  │     ├── 成本变化: -N × unit_cost
  │     └── 关联菜品: dish_id
  │
  └── 关联数据
        ├── reference_no = 菜品订单号
        ├── reference_type = 'DISH_ORDER'
        └── unit_cost = 领料成本
```

### 6.4 WASTE (报损)

```
Business Event: 报损申请审批
  │
  ├── 触发条件
  │     ├── 报损申请已提交
  │     ├── 报损原因已说明
  │     └── 报损申请已审批
  │
  ├── Movement 语义
  │     ├── 数量变化: -N
  │     ├── 成本变化: -N × unit_cost
  │     └── 损失确认: +N × unit_cost
  │
  └── 关联数据
        ├── reference_no = 报损单号
        ├── reference_type = 'WASTE_ORDER'
        └── waste_reason = 报损原因
```

### 6.5 ADJUSTMENT (调整)

```
Business Event: 盘点调整
  │
  ├── 触发条件
  │     ├── 盘点任务已完成
  │     ├── 差异已确认
  │     └── 调整申请已审批
  │
  ├── Movement 语义
  │     ├── 数量变化: +/-N (差异量)
  │     ├── 成本变化: +/-N × unit_cost
  │     └── 差异原因: 已记录
  │
  └── 关联数据
        ├── reference_no = 盘点单号
        ├── reference_type = 'STOCKTAKE'
        └── difference_reason = 差异原因
```

### 6.6 TRANSFER (调拨)

```
Business Event: 调拨申请审批
  │
  ├── 触发条件
  │     ├── 调拨申请已提交
  │     ├── 调拨原因已说明
  │     └── 调拨申请已审批
  │
  ├── Movement 语义
  │     ├── 源仓库: TRANSFER_OUT (数量变化: -N)
  │     ├── 目标仓库: TRANSFER_IN (数量变化: +N)
  │     └── 成本传递: 保持一致
  │
  └── 关联数据
        ├── reference_no = 调拨单号
        ├── reference_type = 'TRANSFER_ORDER'
        └── transfer_reason = 调拨原因
```

---

## 7. 推荐的 Movement 类型体系

### 7.1 标准 Movement Types

```typescript
// 推荐的 Inventory Movement Types
enum InventoryMovementType {
  // 入库类
  RECEIPT = 'RECEIPT',                    // 采购入库
  TRANSFER_IN = 'TRANSFER_IN',            // 调拨入库
  RETURN = 'RETURN',                      // 退货入库
  PRODUCTION_IN = 'PRODUCTION_IN',        // 生产入库
  ADJUSTMENT_IN = 'ADJUSTMENT_IN',        // 盘点盈余
  
  // 出库类
  SALE = 'SALE',                          // 销售出库
  TRANSFER_OUT = 'TRANSFER_OUT',          // 调拨出库
  CONSUMPTION = 'CONSUMPTION',            // 消耗（厨房领料）
  WASTE = 'WASTE',                        // 报损
  ADJUSTMENT_OUT = 'ADJUSTMENT_OUT',      // 盘点亏损
  
  // 内部类（不改变数量）
  LOCK = 'LOCK',                          // 锁定
  UNLOCK = 'UNLOCK',                      // 解锁
  EXPIRE = 'EXPIRE',                      // 过期（触发报损）
  DAMAGE = 'DAMAGE',                      // 损坏（触发报损）
  
  // 逆向类
  REVERSAL = 'REVERSAL',                  // 冲销
  CANCEL = 'CANCEL'                       // 取消
}
```

### 7.2 Movement 的属性定义

```typescript
interface InventoryMovement {
  // 基础信息
  movementId: string;
  movementType: InventoryMovementType;
  
  // 库存信息
  materialId: string;
  locationId: string;
  batchNo?: string;
  
  // 数量信息
  quantityChange: number;  // 正=入库，负=出库
  beforeQty: number;
  afterQty: number;
  
  // 成本信息
  unitCost: number;
  totalCost: number;
  
  // 关联信息
  referenceNo: string;
  referenceType: string;
  
  // 元数据
  createdAt: Date;
  createdBy: string;
  remark?: string;
  
  // 冲销信息
  reversedBy?: string;
  reversedAt?: Date;
}
```

### 7.3 Movement 的方向定义

```typescript
// Movement 方向定义
const MovementDirection = {
  // 入库方向
  INBOUND: ['RECEIPT', 'TRANSFER_IN', 'RETURN', 'PRODUCTION_IN', 'ADJUSTMENT_IN'],
  
  // 出库方向
  OUTBOUND: ['SALE', 'TRANSFER_OUT', 'CONSUMPTION', 'WASTE', 'ADJUSTMENT_OUT'],
  
  // 内部（不改变数量）
  INTERNAL: ['LOCK', 'UNLOCK', 'EXPIRE', 'DAMAGE'],
  
  // 逆向
  REVERSAL: ['REVERSAL', 'CANCEL']
};
```

---

## 8. 当前系统与目标系统的差距分析

### 8.1 缺失的 Movement Types

| 缺失类型 | 描述 | 影响 | 建议 |
|----------|------|------|------|
| CONSUMPTION | 厨房领料消耗 | 无法追踪厨房消耗 | 新增 CONSUMPTION 类型 |
| LOCK / UNLOCK | 锁定/解锁库存 | 无法支持库存锁定 | 新增 LOCK/UNLOCK 类型 |
| EXPIRE / DAMAGE | 过期/损坏 | 无法追踪状态变化 | 新增 EXPIRE/DAMAGE 类型 |
| REVERSAL / CANCEL | 冲销/取消 | 无法支持逆向操作 | 新增 REVERSAL/CANCEL 类型 |
| PRODUCTION_IN | 生产入库 | 无法支持生产入库 | 新增 PRODUCTION_IN 类型 |

### 8.2 语义层次缺失

| 缺失层次 | 描述 | 影响 | 建议 |
|----------|------|------|------|
| Business Event | 业务事件层 | 无法区分业务事件和库存变动 | 引入 Business Event 概念 |
| State Change | 状态变化层 | 无法追踪库存状态变化 | 引入 State Change 概念 |
| Derived Result | 派生结果层 | 派生结果可能被误认为独立变动 | 明确 Derived Result 的定义 |

### 8.3 数据模型差距

| 差距 | 现状 | 目标 | 建议 |
|------|------|------|------|
| 事务类型枚举 | 只有 8 种 | 16+ 种 | 扩展枚举 |
| 状态变化记录 | 无独立记录 | 独立记录 | 新增状态变化表 |
| 冲销记录 | 无支持 | 支持冲销 | 新增冲销字段 |
| Business Event 关联 | 无支持 | 支持关联 | 新增业务事件表 |

---

## 9. 推荐的数据模型

### 9.1 扩展后的 inventory_transactions 表

```sql
-- 扩展后的库存变动表
CREATE TABLE inventory_transactions (
  transaction_id BIGINT PRIMARY KEY,
  
  -- 变动类型（扩展为字符串）
  movement_type VARCHAR(50) NOT NULL,  -- 'RECEIPT', 'SALE', 'CONSUMPTION', etc.
  
  -- 库存信息
  inventory_id BIGINT NOT NULL,
  material_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  batch_no VARCHAR(50),
  
  -- 数量信息
  quantity_change DECIMAL(15,3) NOT NULL,
  before_qty DECIMAL(15,3) NOT NULL,
  after_qty DECIMAL(15,3) NOT NULL,
  
  -- 成本信息
  unit_cost DECIMAL(12,2),
  total_cost DECIMAL(12,2),
  
  -- 关联信息
  reference_no VARCHAR(50),
  reference_type VARCHAR(50),
  
  -- 冲销信息
  reversed_by BIGINT,  -- 冲销此记录的 transaction_id
  reversal_of BIGINT,  -- 此记录冲销的 transaction_id
  
  -- 状态变化信息
  state_change VARCHAR(50),  -- 'LOCK', 'UNLOCK', 'EXPIRE', 'DAMAGE'
  
  -- 元数据
  create_user_id BIGINT NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  remark VARCHAR(500),
  deleted INTEGER NOT NULL DEFAULT 0
);
```

### 9.2 新增状态变化表

```sql
-- 库存状态变化表
CREATE TABLE inventory_state_changes (
  state_change_id BIGINT PRIMARY KEY,
  
  -- 库存信息
  inventory_id BIGINT NOT NULL,
  material_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  
  -- 状态变化
  state_type VARCHAR(50) NOT NULL,  -- 'LOCK', 'UNLOCK', 'EXPIRE', 'DAMAGE'
  previous_state VARCHAR(50),
  new_state VARCHAR(50),
  
  -- 关联信息
  reference_no VARCHAR(50),
  reference_type VARCHAR(50),
  
  -- 元数据
  create_user_id BIGINT NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  remark VARCHAR(500),
  deleted INTEGER NOT NULL DEFAULT 0
);
```

### 9.3 新增业务事件表

```sql
-- 业务事件表
CREATE TABLE business_events (
  event_id BIGINT PRIMARY KEY,
  
  -- 事件信息
  event_type VARCHAR(50) NOT NULL,  -- 'PURCHASE_ORDER', 'SALES_ORDER', etc.
  event_no VARCHAR(50) NOT NULL,
  event_status VARCHAR(50) NOT NULL,  -- 'CREATED', 'APPROVED', 'COMPLETED', etc.
  
  -- 关联信息
  reference_no VARCHAR(50),
  reference_type VARCHAR(50),
  
  -- 元数据
  create_user_id BIGINT NOT NULL,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  remark VARCHAR(500),
  deleted INTEGER NOT NULL DEFAULT 0,
  
  UNIQUE(event_type, event_no)
);
```

---

## 10. 决策矩阵

| 评估维度 | 当前系统 | 扩展方案 | 推荐 |
|----------|----------|----------|------|
| Movement 类型完整性 | ❌ 只有 8 种 | ✅ 16+ 种 | 扩展 |
| 语义层次清晰度 | ❌ 所有类型平等 | ✅ 四层语义 | 引入语义层次 |
| 逆向操作支持 | ❌ 不支持 | ✅ 支持冲销 | 新增冲销 |
| 状态变化追踪 | ❌ 不支持 | ✅ 支持状态变化 | 新增状态表 |
| Business Event 关联 | ❌ 不支持 | ✅ 支持关联 | 新增事件表 |
| 开发成本 | - | ⚠️ 中等 | 短期保持 |
| 维护成本 | ❌ 高 | ✅ 低 | 扩展 |

---

## 11. 结论

### 11.1 核心结论

1. **Inventory Movement 是库存数量的直接变动**
   - Movement 是库存流水的基本单位
   - Movement 是不可变的（只能冲销，不能修改）
   - Movement 有明确的方向（入库/出库）

2. **Movement 与 Business Event、State Change、Derived Result 是不同层次的概念**
   - Business Event 是 Movement 的触发器
   - State Change 不直接改变数量，但可能触发 Movement
   - Derived Result 是从流水计算的结果，不是独立的变动

3. **当前系统的 transaction_type 枚举不完整**
   - 缺少 CONSUMPTION（消耗）
   - 缺少 LOCK/UNLOCK（锁定/解锁）
   - 缺少 EXPIRE/DAMAGE（过期/损坏）
   - 缺少 REVERSAL/CANCEL（冲销/取消）

4. **必须建立标准的 Movement 类型体系**
   - 定义 16+ 种标准 Movement Types
   - 建立四层语义模型
   - 支持逆向操作和状态变化

### 11.2 关键原则

| 原则 | 说明 |
|------|------|
| **库存不等于商品数量** | 库存包含数量、成本、状态、位置等多维信息 |
| **Movement 是不可变的** | 一旦记录不可修改，只能冲销 |
| **语义层次必须清晰** | 区分 Business Event、Movement、State Change、Derived Result |
| **不要假设枚举是最终的** | Movement Types 应该是可扩展的 |

### 11.3 与当前系统的差距

| 差距 | 影响 | 建议 |
|------|------|------|
| 事务类型不完整 | 无法支持复杂场景 | 扩展枚举到 16+ 种 |
| 语义层次不清晰 | 无法区分不同概念 | 引入四层语义模型 |
| 缺少逆向操作 | 无法支持取消/冲销 | 新增 REVERSAL/CANCEL |
| 缺少状态变化记录 | 无法追踪状态变化 | 新增状态变化表 |
| 缺少 Business Event 关联 | 无法追踪业务事件 | 新增业务事件表 |

### 11.4 迁移路径

**短期（保持现状）**：
- 承认当前系统的限制
- 通过应用层模拟缺失的 Movement Types

**中期（引入语义层次）**：
- 扩展 inventory_transactions 表，支持字符串类型的 movement_type
- 新增 inventory_state_changes 表，记录状态变化
- 新增 business_events 表，关联业务事件

**长期（统一 Movement 模型）**：
- 废弃整数类型的 transaction_type
- 使用字符串类型的 movement_type
- 建立完整的 Movement 类型体系

---

## 12. 附录

### 12.1 术语表

| 术语 | 英文 | 定义 |
|------|------|------|
| 库存变动 | Inventory Movement | 库存数量的直接变动，是库存流水的基本单位 |
| 业务事件 | Business Event | 触发库存变动的业务操作 |
| 状态变化 | State Change | 库存状态的变化，不直接改变数量 |
| 派生结果 | Derived Result | 从流水聚合计算的结果，不是独立的变动 |
| 冲销 | Reversal | 撤销之前的 Movement，通过记录反向 Movement 实现 |

### 12.2 相关文件

| 文件 | 说明 |
|------|------|
| `inventory-truth-analysis.md` | 库存真相分析 (Phase 1) |
| `stockable-object-analysis.md` | 可存储对象分析 (Phase 2) |
| `stockable-capability-analysis.md` | 可存储能力分析 (Phase 3) |
| `inventory-location-model.md` | 库存位置模型分析 (Phase 4) |
| `store-vs-warehouse-analysis.md` | 门店库存与仓储库存分析 (Phase 5) |
| `partial-consumption-analysis.md` | 部分消耗分析 (Phase 9) |

### 12.3 当前系统 transaction_type 枚举

```java
// 当前系统的 transaction_type
public static final Integer PURCHASE_RECEIPT = 1;      // 采购入库
public static final Integer SALE = 2;                   // 销售出库
public static final Integer TRANSFER_OUT = 3;           // 调拨出
public static final Integer TRANSFER_IN = 4;            // 调拨入
public static final Integer STOCKTAKE_GAIN = 5;         // 盘点盈
public static final Integer STOCKTAKE_LOSS = 6;         // 盘点亏
public static final Integer WASTE = 7;                  // 报损
public static final Integer RETURN = 8;                 // 退货
```

---

**状态**：ANALYSIS, NOT CONFIRMED
**日期**：2026-09-10
**阶段**：Phase 10 - Inventory Movement Model Analysis
