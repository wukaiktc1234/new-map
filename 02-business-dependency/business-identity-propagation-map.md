# Business Identity Propagation Map

> **项目**: 食品溯源系统 (Food Traceability System)
> **生成时间**: 2026-09-09
> **数据来源**: 代码库静态分析 + 地图文件交叉验证

---

## 一、Identity 传播总览

| Identity | Truth Source | 传播范围 | 冲突类型 | 严重程度 |
|----------|--------------|----------|----------|----------|
| supplierId | `suppliers` 表 | 7+ 表 | SNAPSHOT + REFERENCE 混用 | 🟡 中 |
| productId | 语义冲突 | 多表 | Legacy Identity / Wrong Identity | 🔴 高 |
| materialId | `material_archives` 表 | 5+ 表 | REFERENCE 为主 | 🟢 低 |
| storeId | `stores_new` 表 | 6+ 表 | DUPLICATE (stores 双表) | 🟡 中 |
| warehouseId | ⚠️ 未定义主源 | 3 表 | Identity Loss (无 Canonical Source) | 🔴 高 |
| employeeId | `employees` 表 | 5+ 表 | SNAPSHOT (approverName) | 🟢 低 |
| orderId | `orders` 表 | 多表 | 三套状态机并存 | 🔴 高 |
| orderCode | `orders` 表 | 下游表 | SNAPSHOT 传递 | 🟢 低 |
| accountId | `bank_accounts` 表 | 3 表 | REFERENCE 为主 | 🟢 低 |

---

## 二、各 Identity 详细传播路径

### 2.1 supplierId 传播链

```
┌─────────────────────────────────────────────────────────────────────┐
│                        supplierId 传播图                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  suppliers (Truth Source)                                           │
│       │                                                             │
│       ├──→ purchase_orders.supplierId (REFERENCE)                  │
│       │         └──→ purchase_orders.supplierName (SNAPSHOT)        │
│       │                                                             │
│       ├──→ purchase_request.supplierId (REFERENCE)                 │
│       │                                                             │
│       ├──→ material_archives.supplierId (REFERENCE)                │
│       │                                                             │
│       ├──→ payables.supplierId (REFERENCE)                         │
│       │         └──→ payables.supplierName (SNAPSHOT)              │
│       │                                                             │
│       ├──→ material_trace_code.supplierId (REFERENCE)              │
│       │                                                             │
│       └──→ [Implicit] PaymentDialog → supplierApi.getById()        │
│                 └──→ mapSettlementToPaymentMethod()                │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

| 传播位置 | 字段 | 传播类型 | 传播方式 | 风险 |
|----------|------|----------|----------|------|
| `purchase_orders.supplierId` | supplierId | REFERENCE | Explicit | ✅ 低 |
| `purchase_orders.supplierName` | supplierName | SNAPSHOT | Explicit | ⚠️ 中 - 供应商改名不影响历史 |
| `purchase_request.supplierId` | supplierId | REFERENCE | Explicit | ✅ 低 |
| `material_archives.supplierId` | supplierId | REFERENCE | Explicit | ✅ 低 |
| `payables.supplierId` | supplierId | REFERENCE | Explicit | ✅ 低 |
| `payables.supplierName` | supplierName | SNAPSHOT | Explicit | ⚠️ 中 - 应付单快照 |
| `material_trace_code.supplierId` | supplierId | REFERENCE | Explicit | ✅ 低 |
| PaymentDialog | supplierId | DERIVED | Re-query | ⚠️ 中 - Re-query Pattern |

**关键发现**:
1. ⚠️ **SNAPSHOT 传播**: `purchase_orders.supplierName` 和 `payables.supplierName` 为创建时快照，供应商改名后历史记录仍保留旧名
2. ⚠️ **Re-query Pattern**: PaymentDialog 通过 `supplierApi.getById(payable.supplierId)` 重新查询，可能导致页面打开后供应商信息变化
3. ✅ **REFERENCE 一致性**: 核心引用关系使用 REFERENCE 类型，保证引用完整性

---

### 2.2 productId 传播链 (WRONG IDENTITY)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        productId 传播图                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ⚠️ 语义冲突: product_id 实际指向物料，而非产品                      │
│                                                                     │
│  material_archives (Truth Source - 物料)                            │
│       │                                                             │
│       ├──→ purchase_orders.materialId (REFERENCE - 正确)            │
│       │                                                             │
│       └──→ purchase_orders.product_id (⚠️ Legacy Identity)         │
│                 └──→ 实际指向 material_archives.id                  │
│                                                                     │
│  foods (Truth Source - 菜品)                                        │
│       │                                                             │
│       ├──→ order_items.foodId (REFERENCE - 正确)                    │
│       │                                                             │
│       └──→ food (旧表) - DUPLICATE                                  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

| 传播位置 | 字段 | 传播类型 | 传播方式 | 风险 |
|----------|------|----------|----------|------|
| `purchase_orders.materialId` | materialId | REFERENCE | Explicit | ✅ 正确语义 |
| `purchase_orders.product_id` | product_id | OVERRIDDEN | Legacy | 🔴 错误语义 - 实际指向物料 |
| `order_items.foodId` | foodId | REFERENCE | Explicit | ✅ 正确语义 |

**关键发现**:
1. 🔴 **Wrong Identity**: `product_id` 列名语义错误，实际指向 `material_archives.id` 而非产品
2. 🔴 **Legacy Identity**: 新代码应使用 `material_id` 语义，`product_id` 为技术债
3. ✅ **裁决**: `material_id` 为真相语义；`product_id` 列名暂保留兼容

---

### 2.3 materialId 传播链

```
┌─────────────────────────────────────────────────────────────────────┐
│                        materialId 传播图                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  material_archives (Truth Source)                                   │
│       │                                                             │
│       ├──→ purchase_orders.materialId (REFERENCE)                  │
│       │                                                             │
│       ├──→ inventory.materialId (REFERENCE)                        │
│       │                                                             │
│       ├──→ material_trace_code.materialId (REFERENCE)              │
│       │                                                             │
│       ├──→ material_consumption.materialId (REFERENCE)             │
│       │                                                             │
│       └──→ purchase_orders.product_id (⚠️ Legacy - OVERRIDDEN)    │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

| 传播位置 | 字段 | 传播类型 | 传播方式 | 风险 |
|----------|------|----------|----------|------|
| `purchase_orders.materialId` | materialId | REFERENCE | Explicit | ✅ 低 |
| `inventory.materialId` | materialId | REFERENCE | Explicit | ✅ 低 |
| `material_trace_code.materialId` | materialId | REFERENCE | Explicit | ✅ 低 |
| `material_consumption.materialId` | materialId | REFERENCE | Explicit | ✅ 低 |
| `purchase_orders.product_id` | product_id | OVERRIDDEN | Legacy | 🔴 高 - 语义冲突 |

**关键发现**:
1. ✅ **REFERENCE 一致性**: 核心传播链使用 REFERENCE 类型
2. ⚠️ **Legacy Column**: `product_id` 列名保留但语义已废弃

---

### 2.4 storeId 传播链

```
┌─────────────────────────────────────────────────────────────────────┐
│                        storeId 传播图                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  stores_new (Truth Source)                                          │
│       │                                                             │
│       ├──→ orders.storeId (REFERENCE)                              │
│       │         └──→ orders.storeName (SNAPSHOT)                   │
│       │                                                             │
│       ├──→ store_inventory.storeId (REFERENCE)                     │
│       │                                                             │
│       ├──→ users.storeId (REFERENCE)                               │
│       │                                                             │
│       ├──→ employees.storeId (REFERENCE)                           │
│       │                                                             │
│       ├──→ inventory_transfer.fromStoreId/toStoreId (REFERENCE)    │
│       │                                                             │
│       └──→ stores (旧表) - DUPLICATE                               │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

| 传播位置 | 字段 | 传播类型 | 传播方式 | 风险 |
|----------|------|----------|----------|------|
| `orders.storeId` | storeId | REFERENCE | Explicit | ✅ 低 |
| `orders.storeName` | storeName | SNAPSHOT | Explicit | ⚠️ 中 - 门店改名不影响历史 |
| `store_inventory.storeId` | storeId | REFERENCE | Explicit | ✅ 低 |
| `users.storeId` | storeId | REFERENCE | Explicit | ✅ 低 |
| `employees.storeId` | storeId | REFERENCE | Explicit | ✅ 低 |
| `stores.id` (旧表) | id | DUPLICATE | Legacy | 🔴 高 - 双表并存 |

**关键发现**:
1. 🔴 **Duplicate Identity**: `stores` 和 `stores_new` 双表并存，可能导致读错表
2. ✅ **裁决**: `stores_new` 为唯一真相源；`stores` 仅用于兼容旧查询
3. ⚠️ **SNAPSHOT**: `orders.storeName` 为创建时快照

---

### 2.5 warehouseId 传播链 (IDENTITY LOSS)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        warehouseId 传播图                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ⚠️ Identity Loss: 未发现独立 Canonical Source                      │
│                                                                     │
│  [Unknown Source] - 可能复用 stores_new 或独立表                    │
│       │                                                             │
│       ├──→ inventory.warehouseId (REFERENCE)                       │
│       │                                                             │
│       ├──→ employees.warehouseId (REFERENCE)                       │
│       │                                                             │
│       └──→ inventory_locations.warehouseId (REFERENCE)             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

| 传播位置 | 字段 | 传播类型 | 传播方式 | 风险 |
|----------|------|----------|----------|------|
| `inventory.warehouseId` | warehouseId | REFERENCE | Explicit | ⚠️ 中 - 引用目标不明 |
| `employees.warehouseId` | warehouseId | REFERENCE | Explicit | ⚠️ 中 - 引用目标不明 |
| `inventory_locations.warehouseId` | warehouseId | REFERENCE | Explicit | ⚠️ 中 - 引用目标不明 |

**关键发现**:
1. 🔴 **Identity Loss**: 仓库信息无独立 Canonical Source，未在 db-reality-map.md 中列出
2. ⚠️ **Reference Target Unknown**: warehouseId 引用目标不明确
3. 📋 **待确认**: 需要为仓库信息建立独立 Canonical Source 文档

---

### 2.6 employeeId 传播链

```
┌─────────────────────────────────────────────────────────────────────┐
│                        employeeId 传播图                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  employees (Truth Source)                                           │
│       │                                                             │
│       ├──→ users.employeeId (REFERENCE)                            │
│       │                                                             │
│       ├──→ attendance_record.employeeId (REFERENCE)                │
│       │                                                             │
│       ├──→ salary_records.employeeId (REFERENCE)                   │
│       │                                                             │
│       ├──→ onboarding_archive.employeeId (REFERENCE)               │
│       │                                                             │
│       └──→ approval_record.approverId (REFERENCE)                  │
│                 └──→ approval_record.approverName (SNAPSHOT)       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

| 传播位置 | 字段 | 传播类型 | 传播方式 | 风险 |
|----------|------|----------|----------|------|
| `users.employeeId` | employeeId | REFERENCE | Explicit | ✅ 低 |
| `attendance_record.employeeId` | employeeId | REFERENCE | Explicit | ✅ 低 |
| `salary_records.employeeId` | employeeId | REFERENCE | Explicit | ✅ 低 |
| `onboarding_archive.employeeId` | employeeId | REFERENCE | Explicit | ✅ 低 |
| `approval_record.approverId` | approverId | REFERENCE | Explicit | ✅ 低 |
| `approval_record.approverName` | approverName | SNAPSHOT | Explicit | ⚠️ 中 - 审批人姓名快照 |

**关键发现**:
1. ✅ **REFERENCE 一致性**: 核心传播链使用 REFERENCE 类型
2. ⚠️ **SNAPSHOT**: `approval_record.approverName` 为创建时快照

---

### 2.7 orderId 传播链

```
┌─────────────────────────────────────────────────────────────────────┐
│                        orderId 传播图                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  orders (Truth Source)                                              │
│       │                                                             │
│       ├──→ order_items.orderId (REFERENCE)                         │
│       │                                                             │
│       ├──→ order_payment_records.orderId (REFERENCE)               │
│       │                                                             │
│       ├──→ [状态机冲突] order_status / payment_status / legacy      │
│       │                                                             │
│       └──→ [Legacy] orders_legacy - DUPLICATE                      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

| 传播位置 | 字段 | 传播类型 | 传播方式 | 风险 |
|----------|------|----------|----------|------|
| `order_items.orderId` | orderId | REFERENCE | Explicit | ✅ 低 |
| `order_payment_records.orderId` | orderId | REFERENCE | Explicit | ✅ 低 |
| `orders.order_status` | order_status | STATE | Explicit | 🔴 高 - 三套状态机 |
| `orders.payment_status` | payment_status | STATE | Explicit | 🔴 高 - 三套状态机 |
| `orders.legacy status` | legacy | STATE | Legacy | 🔴 高 - 三套状态机 |

**关键发现**:
1. 🔴 **三套状态机并存**: `order_status`, `payment_status`, legacy 三套并存
2. 🔴 **状态映射不完整**: `sales_order` 状态与 `order_status` 完全无映射
3. ✅ **裁决**: 三套状态机并存，每个字段有独立生命周期

---

### 2.8 orderCode 传播链

```
┌─────────────────────────────────────────────────────────────────────┐
│                        orderCode 传播图                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  orders (Truth Source)                                              │
│       │                                                             │
│       └──→ 下游表 (SNAPSHOT)                                        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

| 传播位置 | 字段 | 传播类型 | 传播方式 | 风险 |
|----------|------|----------|----------|------|
| orders.orderCode | orderCode | REFERENCE | Explicit | ✅ 低 |

**关键发现**:
1. ✅ **低风险**: orderCode 为主要业务标识，传播链简单

---

### 2.9 accountId 传播链

```
┌─────────────────────────────────────────────────────────────────────┐
│                        accountId 传播图                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  bank_accounts (Truth Source)                                       │
│       │                                                             │
│       ├──→ payment.bankAccountId (REFERENCE)                       │
│       │                                                             │
│       ├──→ receipt.bankAccountId (REFERENCE)                       │
│       │                                                             │
│       └──→ fund_flows.bankAccountId (REFERENCE)                    │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

| 传播位置 | 字段 | 传播类型 | 传播方式 | 风险 |
|----------|------|----------|----------|------|
| `payment.bankAccountId` | bankAccountId | REFERENCE | Explicit | ✅ 低 |
| `receipt.bankAccountId` | bankAccountId | REFERENCE | Explicit | ✅ 低 |
| `fund_flows.bankAccountId` | bankAccountId | REFERENCE | Explicit | ✅ 低 |

**关键发现**:
1. ✅ **REFERENCE 一致性**: 核心传播链使用 REFERENCE 类型，无冲突

---

## 三、Identity 问题分类

### 3.1 Identity Loss (身份丢失)

| Identity | 问题描述 | 严重程度 | 影响范围 |
|----------|----------|----------|----------|
| warehouseId | 未发现独立 Canonical Source | 🔴 高 | 库存、员工、仓位 |

**修复建议**: 建立独立的仓库主数据表，明确 warehouseId 的 Canonical Source

### 3.2 Identity Re-generation (身份重新生成)

| Identity | 问题描述 | 严重程度 | 影响范围 |
|----------|----------|----------|----------|
| supplierId | PaymentDialog 通过 Re-query 重新查询供应商信息 | 🟡 中 | 付款登记 |

**修复建议**: 可接受的 Re-query Pattern，但需注意页面打开后供应商信息可能变化

### 3.3 Wrong Identity (身份错误)

| Identity | 问题描述 | 严重程度 | 影响范围 |
|----------|----------|----------|----------|
| productId | `product_id` 列名实际指向物料而非产品 | 🔴 高 | 采购订单 |

**修复建议**: 新代码一律使用 `material_id` 语义，`product_id` 列名保留兼容

### 3.4 Implicit Identity (隐式身份)

| Identity | 问题描述 | 严重程度 | 影响范围 |
|----------|----------|----------|----------|
| storeId/warehouseId | 收货确认中 storeId/warehouseId 从 arrival 继承但不在表单中 | 🟡 中 | 收货确认 |

**修复建议**: 已通过代码处理，属于 Hidden Inheritance 模式

### 3.5 Legacy Identity (遗留身份)

| Identity | 问题描述 | 严重程度 | 影响范围 |
|----------|----------|----------|----------|
| product_id | 遗留列名，语义已废弃 | 🔴 高 | 采购订单 |
| stores | 旧门店表与新表并存 | 🔴 高 | 门店相关 |

**修复建议**: 完成 stores/stores_new 迁移，废弃 stores 表

### 3.6 Duplicate Identity (重复身份)

| Identity | 问题描述 | 严重程度 | 影响范围 |
|----------|----------|----------|----------|
| storeId | `stores` 和 `stores_new` 双表并存 | 🔴 高 | 门店相关 |

**修复建议**: 完成迁移，废弃旧表

---

## 四、Identity 传播风险矩阵

| 风险等级 | Identity | 问题类型 | 涉及表 | 缓解措施 |
|----------|----------|----------|--------|----------|
| 🔴 P0 | productId | Wrong Identity | purchase_orders.product_id | 新代码使用 material_id |
| 🔴 P0 | storeId | Duplicate Identity | stores/stores_new | 完成迁移废弃旧表 |
| 🔴 P0 | warehouseId | Identity Loss | 无 Canonical Source | 建立独立仓库主数据 |
| 🔴 P0 | orderId | 三套状态机 | orders 多状态字段 | 统一或建立映射表 |
| 🟡 P1 | supplierId | SNAPSHOT 传播 | purchase_orders.supplierName | 可接受，记录快照 |
| 🟡 P1 | supplierId | Re-query Pattern | PaymentDialog | 可接受，保证最新状态 |
| 🟡 P1 | employeeId | SNAPSHOT | approval_record.approverName | 可接受，记录快照 |

---

## 五、Identity 传播治理建议

| # | 建议 | 优先级 | 涉及 Identity |
|---|------|--------|---------------|
| 1 | 完成 stores/stores_new 迁移，废弃 stores 表 | P0 | storeId |
| 2 | 建立独立的仓库主数据表，明确 warehouseId Canonical Source | P0 | warehouseId |
| 3 | 新代码一律使用 material_id 语义，逐步清理 product_id | P0 | productId |
| 4 | 订单状态机统一或建立明确映射表 | P0 | orderId |
| 5 | 修正所有跨表查询中的金额单位转换注释 | P1 | 全局 |
| 6 | 为 SNAPSHOT 字段添加创建时间戳，便于追溯 | P2 | supplierName, storeName |

---

*生成时间: 2026-09-09*
*数据来源: 代码库静态分析 + 地图文件交叉验证*
