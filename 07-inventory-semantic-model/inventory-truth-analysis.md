# Inventory Truth Analysis (Phase 1)

## 核心问题：什么才是 Inventory 的 Canonical Truth？

> 库存到底是什么？

---

## 1. 五种库存概念的定义与比较

| 概念 | 定义 | 语义 | 粒度 | 时间维度 |
|------|------|------|------|----------|
| **Inventory Balance** | 某一时刻的库存余额 | 快照 (Snapshot) | 单仓库/单门店 | 当前时刻 |
| **Stock Ledger** | 库存变动的完整流水记录 | 事件流 (Event Stream) | 单笔交易 | 持续累积 |
| **Inventory Fact** | 库存的客观事实状态 | 真相 (Truth) | 物理实体 | 当前+历史 |
| **Stock Position** | 库存的业务可用头寸 | 计算值 (Derived) | 业务维度 | 当前时刻 |
| **Stock Movement** | 库存变动事件 | 变动 (Mutation) | 单笔交易 | 发生时刻 |

### 关键区别

```
Stock Ledger (流水)
    ↓ 聚合
Inventory Balance (余额)  ← 某一时刻的快照
    ↓ 扣减
Stock Position (头寸)     ← 可销售/可调拨的可用量
    ↓ 事件
Stock Movement (变动)     ← 单次出入库事件
    ↓ 总和
Inventory Fact (事实)     ← 所有维度的完整真相
```

---

## 2. 当前系统现实分析

### 2.1 三张库存表的职责

```
┌─────────────────────────────────────────────────────────────┐
│                    当前库存数据模型                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────┐      ┌──────────────────┐            │
│  │    inventory     │      │ store_inventory  │            │
│  │  (仓库库存)      │      │  (门店库存)      │            │
│  ├──────────────────┤      ├──────────────────┤            │
│  │ inventory_id     │      │ store_id         │            │
│  │ material_id      │      │ material_id      │            │
│  │ warehouse_id     │      │ current_stock    │            │
│  │ current_stock    │      │ unit             │            │
│  │ locked_quantity  │      │ unit_cost        │            │
│  │ unit_cost        │      │ total_cost       │            │
│  │ total_cost       │      │ safety_stock     │            │
│  │ batch_no         │      │ max_stock        │            │
│  │ min_safe_qty     │      └──────────────────┘            │
│  │ max_stock_qty    │                                       │
│  └──────────────────┘                                       │
│           │                                                  │
│           │ 无事务一致性                                     │
│           ▼                                                  │
│  ┌──────────────────────────────────────────┐               │
│  │        inventory_transactions            │               │
│  │           (库存流水)                      │               │
│  ├──────────────────────────────────────────┤               │
│  │ transaction_id                           │               │
│  │ material_id                              │               │
│  │ inventory_id (关联仓库库存)              │               │
│  │ warehouse_id                             │               │
│  │ transaction_type:                        │               │
│  │   1采购入库 2销售出库 3调拨出 4调拨入     │               │
│  │   5盘点盈 6盘点亏 7报损 8退货            │               │
│  │ quantity_change                           │               │
│  │ before_qty / after_qty                   │               │
│  │ unit_cost / total_cost                   │               │
│  └──────────────────────────────────────────┘               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 核心问题

| 问题 | 现状 | 风险 |
|------|------|------|
| 事务一致性 | inventory 和 store_inventory 独立更新 | 门店收货可能只更新一张表 |
| 数据同步 | 无同步机制 | 仓库与门店库存可能不一致 |
| 流水完整性 | inventory_transactions 仅关联 inventory_id | 门店库存变动可能未记录 |
| 成本核算 | 两套成本字段 (unit_cost) | 成本口径不统一 |

---

## 3. Inventory Truth 判定

### 3.1 什么是 Inventory Truth？

**Inventory Truth = Stock Ledger (库存台账)**

理由：
1. **不可变性**：流水记录一旦写入不可修改，是审计追溯的基础
2. **完整性**：流水包含所有库存变动事件，余额可通过流水重建
3. **因果性**：余额是流水的结果，不是独立的数据源

```
Stock Ledger (Truth Source)
    ↓ 重新计算
Inventory Balance (Derived View)
    ↓ 业务规则
Stock Position (Derived View)
```

### 3.2 当前系统的 Truth 判定

| 概念 | 当前系统中的对应 | 是否是 Truth | 原因 |
|------|------------------|--------------|------|
| Inventory Balance | inventory.current_stock | ❌ 否 | 可从流水重建，是 Derived |
| Store Balance | store_inventory.current_stock | ❌ 否 | 可从流水重建，是 Derived |
| Stock Ledger | inventory_transactions | ✅ 是 | 事件流，不可变 |
| Inventory Fact | 不存在 | ❌ 需构建 | 当前缺少完整视图 |
| Stock Position | 无对应 | ❌ 需构建 | 业务可用量计算缺失 |

### 3.3 回答核心问题

#### Q1: Inventory Truth 是什么？

**Inventory Truth = Inventory Fact (库存事实)**，它是 Stock Ledger 聚合后的完整视图，包含：
- 仓库库存事实
- 门店库存事实
- 跨仓库聚合事实

Stock Ledger 是 Truth Source，Inventory Fact 是 Truth View。

#### Q2: inventory.current_stock 和 store_inventory.current_stock 是否属于同一个 Truth？

**否。** 它们是两个独立的 Derived View，源自不同的 Stock Ledger：

```
仓库 Stock Ledger → inventory.current_stock (仓库维度)
门店 Stock Ledger → store_inventory.current_stock (门店维度)
```

但它们应该属于同一个 **Inventory Fact View**，该视图聚合所有维度的库存事实。

#### Q3: inventory_transactions 是否是 Movement Truth？

**部分是。** inventory_transactions 是仓库维度的 Movement Truth，但缺少门店维度的 Movement Truth。

完整 Movement Truth 应该是：
```
Movement Truth = inventory_transactions (仓库) + store_transactions (门店)
```

#### Q4: 聚合库存是否应该是 Aggregated View 而不是 Truth？

**是。** 聚合库存（如"总库存 = 仓库 + 门店"）是 Aggregated View，不是 Truth Source。

原因：
- 聚合值可从各维度的 Truth 重建
- 聚合逻辑可能随业务变化
- 聚合不应覆盖明细 Truth

---

## 4. 目标 Inventory Model 架构

### 4.1 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Inventory Model Layers                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Layer 1: Truth Source (事件层)                              │
│  ┌──────────────────────────────────────────────────┐       │
│  │ Stock Ledger (库存台账)                           │       │
│  │ - 仓库流水 (inventory_transactions)               │       │
│  │ - 门店流水 (store_transactions) [新增]            │       │
│  │ 特性: Append-only, Immutable, 审计可追溯           │       │
│  └──────────────────────────────────────────────────┘       │
│                           ↓                                 │
│  Layer 2: Truth View (事实层)                                │
│  ┌──────────────────────────────────────────────────┐       │
│  │ Inventory Fact (库存事实)                         │       │
│  │ - 仓库库存事实 (warehouse_inventory_fact)         │       │
│  │ - 门店库存事实 (store_inventory_fact)             │       │
│  │ - 聚合库存事实 (aggregated_inventory_fact)        │       │
│  │ 特性: 可重建, 可查询, 支持多维度                    │       │
│  └──────────────────────────────────────────────────┘       │
│                           ↓                                 │
│  Layer 3: Derived View (业务层)                              │
│  ┌──────────────────────────────────────────────────┐       │
│  │ Stock Position (库存头寸)                         │       │
│  │ - 可销售量 (available_to_sell)                    │       │
│  │ - 可调拨量 (available_to_transfer)                │       │
│  │ - 安全库存预警 (safety_stock_alert)               │       │
│  │ 特性: 业务规则驱动, 支持决策                       │       │
│  └──────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 数据流向

```
采购入库 ─┐
销售出库 ─┤
调拨出入 ─┼─→ Stock Ledger ─→ Inventory Fact ─→ Stock Position
盘点盈亏 ─┤                      ↓
报损退货 ─┘              ┌─────────────────┐
                          │ 业务系统查询    │
                          │ 报表统计        │
                          │ 决策支持        │
                          └─────────────────┘
```

---

## 5. 关键原则重申

| 原则 | 说明 |
|------|------|
| **Current DB Model ≠ Target Inventory Model** | 当前三表结构是历史产物，不是目标架构 |
| **不得建立两套库存 Truth** | Store Inventory + Warehouse Inventory 应统一到同一个 Truth 体系 |
| **聚合库存不能成为 Truth Source** | 聚合值必须可从明细重建 |
| **库存不等于商品数量** | 库存包含成本、批次、状态、位置等多维信息 |

---

## 6. 下一步行动

1. **Phase 2**: 设计 Stock Ledger 统一模型（合并仓库/门店流水）
2. **Phase 3**: 构建 Inventory Fact View（库存事实视图）
3. **Phase 4**: 实现 Stock Position 计算逻辑（业务可用量）
4. **Phase 5**: 制定数据迁移策略（从当前模型到目标模型）

---

## 附录: 术语表

| 术语 | 英文 | 定义 |
|------|------|------|
| 库存台账 | Stock Ledger | 记录所有库存变动事件的不可变流水 |
| 库存事实 | Inventory Fact | 基于流水计算的库存客观状态 |
| 库存头寸 | Stock Position | 基于业务规则计算的可用库存量 |
| 库存余额 | Inventory Balance | 某一时刻的库存数量快照 |
| 库存变动 | Stock Movement | 单次出入库事件 |
