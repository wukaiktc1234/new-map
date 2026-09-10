# Inventory Quantity Model (Phase 6)

## 核心问题：库存数量的语义分类

> 哪些数量是 Truth？哪些是 Derived？哪些是 Operational State？哪些是 View？

---

## 1. 数量语义分类框架

| 类别 | 定义 | 特性 | 示例 |
|------|------|------|------|
| **Truth** | 不可变的业务事实 | 一旦写入不可修改，是审计基础 | 流水中的 quantity_change |
| **Derived** | 由 Truth 计算得出 | 可随时从 Truth 重建 | current_stock = SUM(quantity_change) |
| **Operational State** | 业务操作的中间状态 | 可变，反映当前业务状态 | locked_quantity |
| **View** | 多维度展示的投影 | 无独立存储，实时计算 | available_quantity |

---

## 2. 当前系统数量字段分析

### 2.1 inventory 表（仓库库存）

```
┌─────────────────────────────────────────────────────────────────┐
│                     inventory 表数量字段                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  current_stock (BigDecimal)                                     │
│  ├── 语义: 当前库存数量                                          │
│  ├── 分类: Derived (可从 inventory_transactions 重建)            │
│  ├── 问题: 与 locked_quantity 共同存储，存在更新竞争              │
│  └── 风险: 并发更新可能导致数据不一致                            │
│                                                                 │
│  locked_quantity (BigDecimal)                                   │
│  ├── 语义: 锁定数量（订单预留）                                  │
│  ├── 分类: Operational State                                    │
│  ├── 问题: 无对应的 unlock 事务记录                              │
│  └── 风险: 异常情况下锁定数量无法自动释放                        │
│                                                                 │
│  min_safe_qty (BigDecimal)                                      │
│  ├── 语义: 安全库存阈值                                         │
│  ├── 分类: Configuration (配置值)                               │
│  └── 用于: 库存预警判断                                          │
│                                                                 │
│  max_stock_qty (BigDecimal)                                     │
│  ├── 语义: 最大库存容量                                         │
│  ├── 分类: Configuration (配置值)                               │
│  └── 用于: 入库上限控制                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 store_inventory 表（门店库存）

```
┌─────────────────────────────────────────────────────────────────┐
│                   store_inventory 表数量字段                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  current_stock (DECIMAL(15,3))                                  │
│  ├── 语义: 门店当前库存数量                                      │
│  ├── 分类: Derived (但无对应的门店流水表)                        │
│  ├── 问题: 无法从流水重建，只能手动盘点校正                      │
│  └── 风险: 数据孤岛，与仓库库存无同步机制                        │
│                                                                 │
│  safety_stock (DECIMAL(15,3))                                   │
│  ├── 语义: 门店安全库存阈值                                     │
│  ├── 分类: Configuration                                        │
│  └── 用于: 门店补货预警                                         │
│                                                                 │
│  max_stock (DECIMAL(15,3))                                      │
│  ├── 语义: 门店最大库存容量                                     │
│  ├── 分类: Configuration                                        │
│  └── 用于: 门店收货上限                                         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 inventory_transactions 表（库存流水）

```
┌─────────────────────────────────────────────────────────────────┐
│                inventory_transactions 表数量字段                  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  quantity_change (DECIMAL(12,2))                                │
│  ├── 语义: 变动数量（正数入库，负数出库）                        │
│  ├── 分类: ★ TRUTH ★                                           │
│  ├── 特性: 一旦写入不可修改                                      │
│  └── 用途: 所有库存数量的计算基础                                │
│                                                                 │
│  before_qty (DECIMAL(12,2))                                     │
│  ├── 语义: 变动前数量                                           │
│  ├── 分类: Derived (快照冗余，便于审计)                          │
│  └── 用途: 变动前后对比展示                                      │
│                                                                 │
│  after_qty (DECIMAL(12,2))                                      │
│  ├── 语义: 变动后数量                                           │
│  ├── 分类: Derived (快照冗余，便于审计)                          │
│  └── 用途: 变动前后对比展示                                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 数量语义判定矩阵

| 数量概念 | 当前存储位置 | 语义分类 | 能否从 Truth 重建 | 问题 |
|----------|--------------|----------|-------------------|------|
| **Quantity (变动数量)** | inventory_transactions.quantity_change | **Truth** | N/A (本身就是 Truth) | 无 |
| **Current Stock (当前库存)** | inventory.current_stock | **Derived** | ✅ 可以 | 与 locked_quantity 竞争更新 |
| **Locked Quantity (锁定数量)** | inventory.locked_quantity | **Operational State** | ❌ 不可以 | 无对应 unlock 流水 |
| **Available Quantity (可用数量)** | 不存在（需计算） | **View** | ✅ 可以 | current_stock - locked_quantity |
| **Reserved Quantity (预留数量)** | 不存在（需计算） | **View** | ✅ 可以 | locked_quantity 的别名 |
| **On-hand Quantity (在手数量)** | inventory.current_stock | **Derived** | ✅ 可以 | 与 Current Stock 同义 |
| **In-transit Quantity (在途数量)** | 不存在 | **View** | ✅ 可以 | 需从采购单/调拨单计算 |

---

## 4. 关键发现：两个独立 Truth 问题

### 4.1 问题描述

```
⚠️ 严重违反：一个业务事实存在两个独立数量 Truth

┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│  事实：某物料在某仓库有 100 单位库存                             │
│                                                                 │
│  存储位置 1: inventory.current_stock = 100                       │
│  存储位置 2: inventory_transactions (SUM) = 100                  │
│                                                                 │
│  问题：如果只更新 current_stock 而不写流水，                      │
│        或者流水被清理，两个 Truth 将不一致                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 当前系统现状

| 操作 | 是否写流水 | 是否更新 current_stock | 一致性风险 |
|------|------------|------------------------|------------|
| 采购入库 | ✅ 是 | ✅ 是 | 低 |
| 销售出库 | ✅ 是 | ✅ 是 | 低 |
| 调拨出/入 | ✅ 是 | ✅ 是 | 低 |
| 盘点盈亏 | ✅ 是 | ✅ 是 | 低 |
| 报损 | ✅ 是 | ✅ 是 | 低 |
| 门店收货 | ❌ 否（仅更新 store_inventory） | ❌ 否 | **高** |
| 手动调整 | ⚠️ 不确定 | ⚠️ 不确定 | **高** |

### 4.3 门店库存的 Truth 缺失

```
┌─────────────────────────────────────────────────────────────────┐
│                     门店库存 Truth 缺失                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  store_inventory.current_stock                                   │
│       │                                                         │
│       ├── 无对应的 store_inventory_transactions 表              │
│       ├── 无法从流水重建门店库存                                 │
│       └── 只能通过盘点校正                                       │
│                                                                 │
│  结论：门店库存 current_stock 是唯一的数量来源                    │
│        因此它既是 Truth 又是 Derived（语义矛盾）                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. 推荐的 Canonical Quantity Model

### 5.1 单一 Truth 原则

```
┌─────────────────────────────────────────────────────────────────┐
│                  Single Truth Principle                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ✅ 正确做法：                                                   │
│                                                                 │
│  inventory_transactions.quantity_change (TRUTH)                  │
│       │                                                         │
│       ↓ SUM by material_id + warehouse_id                       │
│                                                                 │
│  current_stock (DERIVED - 可选缓存)                             │
│       │                                                         │
│       ↓ - locked_quantity                                       │
│                                                                 │
│  available_quantity (VIEW - 实时计算)                           │
│                                                                 │
│                                                                 │
│  ❌ 错误做法：                                                   │
│                                                                 │
│  inventory.current_stock = 100  ← 直接更新                      │
│  inventory_transactions      ← 不写流水或补写流水               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 Quantity 语义层次

```
                    ┌─────────────────┐
                    │   Truth Layer   │
                    │  (不可变事实)   │
                    ├─────────────────┤
                    │ quantity_change │
                    │ before_qty      │
                    │ after_qty       │
                    └────────┬────────┘
                             │ SUM/Materialize
                             ↓
                    ┌─────────────────┐
                    │  Derived Layer  │
                    │  (可重建缓存)   │
                    ├─────────────────┤
                    │ current_stock   │
                    │ on_hand_qty     │
                    └────────┬────────┘
                             │ - locked_quantity
                             ↓
                    ┌─────────────────┐
                    │ Operational Layer│
                    │  (业务状态)     │
                    ├─────────────────┤
                    │ locked_quantity │
                    │ reserved_qty    │
                    └────────┬────────┘
                             │ Subtract
                             ↓
                    ┌─────────────────┐
                    │    View Layer   │
                    │  (展示投影)     │
                    ├─────────────────┤
                    │ available_qty   │
                    │ in_transit_qty  │
                    │ display_qty     │
                    └─────────────────┘
```

---

## 6. 当前系统数量字段完整性检查

| 需求场景 | 是否需要 | 当前状态 | 缺失风险 |
|----------|----------|----------|----------|
| 基础库存查询 | ✅ | ✅ current_stock | 低 |
| 订单预留/释放 | ✅ | ⚠️ locked_quantity (无 unlock 流水) | **中** |
| 在途库存统计 | ⚠️ 视业务 | ❌ 缺失 | 需从采购单计算 |
| 批次库存管理 | ⚠️ 视业务 | ✅ batch_no | 低 |
| 效期管理 | ⚠️ 视业务 | ✅ expiry_date | 低 |
| 多仓库库存 | ✅ | ✅ warehouse_id | 低 |
| 门店库存同步 | ✅ | ⚠️ store_inventory 独立 | **高** |

---

## 7. 结论与建议

### 7.1 核心结论

| 结论 | 说明 |
|------|------|
| **Truth** | inventory_transactions.quantity_change |
| **Derived** | current_stock, on_hand_qty (可从 Truth 重建) |
| **Operational State** | locked_quantity (业务状态，需单独管理) |
| **View** | available_quantity, in_transit_qty (实时计算) |

### 7.2 改进建议

1. **统一 Truth 源**：所有库存数量变更必须通过 inventory_transactions 记录
2. **门店流水表**：为 store_inventory 建立对应的 store_inventory_transactions 表
3. **锁定释放流水**：locked_quantity 的增加/减少都应有对应的流水记录
4. **current_stock 缓存化**：明确 current_stock 为 Derived 缓存，定期从流水重建校验
