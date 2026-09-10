# 主数据源地图 (Master Data Source Map)

> **项目**: 食品溯源系统 (Food Traceability System)
> **生成时间**: 2026-09-09
> **数据来源**: 代码库静态分析 + 地图文件交叉验证

---

## 一、主数据源总览

| # | 主数据 | Canonical Source | 状态 | Legacy Source | Duplicate Source |
|---|--------|-----------------|------|--------------|-----------------|
| 1 | 供应商信息 | `suppliers` 表 | ✅ VERIFIED | 无 | 无 |
| 2 | 商品/菜品信息 | `foods` 表 | ✅ VERIFIED | `food` 表 (废弃) | 无 |
| 3 | 物料信息 | `material_archives` 表 | ✅ VERIFIED | 无 | 无 |
| 4 | 员工信息 | `employees` 表 | ✅ VERIFIED | 无 | 无 |
| 5 | 门店信息 | `stores_new` 表 | ✅ VERIFIED | `stores` 表 (废弃) | 无 |
| 6 | 仓库信息 | 未发现独立主表 | ⚠️ 待确认 | 无 | 无 |
| 7 | 账户信息 | `bank_accounts` 表 | ✅ VERIFIED | 无 | 无 |
| 8 | 会员信息 | `members` 表 | ✅ VERIFIED | 无 | 无 |
| 9 | 部门信息 | `departments` 表 | ✅ VERIFIED | 无 | 无 |
| 10 | 岗位信息 | `positions` 表 | ✅ VERIFIED | 无 | 无 |
| 11 | 订单信息 | `orders` 表 (V6) | ✅ VERIFIED | `orders_legacy` 表 | `sales_order` (虚实体) |
| 12 | 库存信息 | `inventory` 表 | ✅ VERIFIED | 无 | 无 |
| 13 | 门店库存 | `store_inventory` 表 | ✅ VERIFIED | 无 | 无 |
| 14 | 凭证信息 | `finance_vouchers` 表 | ✅ VERIFIED | `voucher_header` 表 (死体系) | 无 |
| 15 | 会计科目 | `accounting_subjects` 表 | ✅ VERIFIED | `account_balance` (只读) | 无 |

---

## 二、各主数据详细分析

### 2.1 供应商信息 (Supplier)

| 维度 | 详情 |
|------|------|
| **Canonical Source** | `suppliers` 表 |
| **DB Table** | `suppliers` |
| **Entity** | `Supplier` |
| **API** | `GET /api/v1/suppliers`, `POST /api/v1/suppliers`, `PUT /api/v1/suppliers/:id` |
| **Frontend** | `src/views/purchase/SupplierArchive.vue` |
| **Truth Source** | ✅ 已裁决 — business-object-map.md |
| **Alternate Source** | 无 |
| **Legacy Source** | 无 |
| **Snapshot Source** | `purchase_orders.supplierName` (快照) |
| **Derived Source** | `payables.supplierName` (衍生) |
| **Duplicate Source** | 无 |
| **Known Issues** | 无 |
| **Consumers** | PurchaseOrderService, PaymentService, TraceabilityService |

**引用关系分析**:

| 引用表 | 引用字段 | 引用类型 | 说明 |
|--------|---------|----------|------|
| `purchase_orders` | `supplierId` | REFERENCE | 外键引用 |
| `purchase_orders` | `supplierName` | SNAPSHOT | 下单时快照，供应商改名不影响历史订单 |
| `purchase_request` | `supplierId` | REFERENCE | 外键引用 |
| `material_archives` | `supplierId` | REFERENCE | 物料主供应商 |
| `payables` | `supplierId` | REFERENCE | 外键引用 |
| `payables` | `supplierName` | SNAPSHOT | 应付单快照 |
| `material_trace_code` | `supplierId` | REFERENCE | 追溯码关联 |

---

### 2.2 商品/菜品信息 (Food)

| 维度 | 详情 |
|------|------|
| **Canonical Source** | `foods` 表 |
| **DB Table** | `foods` |
| **Entity** | `Food` |
| **API** | `GET /api/v1/foods`, `POST /api/v1/foods`, `PUT /api/v1/foods/:id` |
| **Frontend** | `src/views/product/FoodManagement.vue`, `src/views/product/FoodForm.vue` |
| **Truth Source** | ✅ 已裁决 — PADR §九-A |
| **Alternate Source** | 无 |
| **Legacy Source** | `food` 表 (⚠️ 双写中) |
| **Snapshot Source** | `order_items.foodName` (快照) |
| **Derived Source** | `store_inventory.foodId` (引用) |
| **Duplicate Source** | 无 |
| **Known Issues** | ⚠️ food/foods 双写 — 下单/退款/超时同时写两表 (truth-conflict-map.md 冲突7) |
| **Consumers** | OrderService, InventoryService, MenuService |

**引用关系分析**:

| 引用表 | 引用字段 | 引用类型 | 说明 |
|--------|---------|----------|------|
| `order_items` | `foodId` | REFERENCE | 外键引用 |
| `order_items` | `foodName` | SNAPSHOT | 下单时快照 |
| `store_inventory` | `foodId` | REFERENCE | 门店库存关联 |
| `dish_recipes` | `foodId` | REFERENCE | 菜品配方 |
| `dish_combos` | `foodId` | REFERENCE | 套餐组合 |
| `food_trace_code` | `foodId` | REFERENCE | 成品溯源码 |
| `food` (旧表) | `id` | ⚠️ DUPLICATE | 旧表仍被双写 |

---

### 2.3 物料信息 (Material)

| 维度 | 详情 |
|------|------|
| **Canonical Source** | `material_archives` 表 |
| **DB Table** | `material_archives` |
| **Entity** | `Material` |
| **API** | `GET /api/v1/materials`, `POST /api/v1/materials`, `PUT /api/v1/materials/:id` |
| **Frontend** | `src/views/purchase/PurchaseArchive.vue` |
| **Truth Source** | ✅ 已裁决 — 内部裁决 |
| **Alternate Source** | 无 |
| **Legacy Source** | 无 |
| **Snapshot Source** | `purchase_orders.items[].materialName` (快照) |
| **Derived Source** | `inventory.materialId` (引用) |
| **Duplicate Source** | 无 |
| **Known Issues** | ⚠️ product_id vs material_id 列名冲突 (truth-conflict-map.md 冲突2) |
| **Consumers** | InventoryService, TraceabilityService, ProductionService |

**引用关系分析**:

| 引用表 | 引用字段 | 引用类型 | 说明 |
|--------|---------|----------|------|
| `purchase_orders` | `materialId` | REFERENCE | 采购订单关联 |
| `purchase_orders` | `product_id` | ⚠️ Legacy | 遗留列名，实际指向物料 |
| `inventory` | `materialId` | REFERENCE | 库存关联 |
| `material_trace_code` | `materialId` | REFERENCE | 物料追溯码 |
| `material_consumption` | `materialId` | REFERENCE | 物料消耗记录 |

---

### 2.4 员工信息 (Employee)

| 维度 | 详情 |
|------|------|
| **Canonical Source** | `employees` 表 |
| **DB Table** | `employees` |
| **Entity** | `Employee` |
| **API** | `GET /api/v1/employees`, `POST /api/v1/employees`, `PUT /api/v1/employees/:id` |
| **Frontend** | `src/views/hr/EmployeeList.vue` |
| **Truth Source** | ✅ VERIFIED |
| **Alternate Source** | 无 |
| **Legacy Source** | 无 |
| **Snapshot Source** | `approval_record.approverName` (快照) |
| **Derived Source** | `users.employeeId` (关联) |
| **Duplicate Source** | 无 |
| **Known Issues** | 无 |
| **Consumers** | HREventListener, AttendanceService, SalaryService |

**引用关系分析**:

| 引用表 | 引用字段 | 引用类型 | 说明 |
|--------|---------|----------|------|
| `users` | `employeeId` | REFERENCE | 用户-员工关联 |
| `attendance_record` | `employeeId` | REFERENCE | 考勤记录 |
| `salary_records` | `employeeId` | REFERENCE | 薪资记录 |
| `onboarding_archive` | `employeeId` | REFERENCE | 入职档案 |
| `approval_record` | `approverId` | REFERENCE | 审批记录 |
| `approval_record` | `approverName` | SNAPSHOT | 审批人姓名快照 |

---

### 2.5 门店信息 (Store)

| 维度 | 详情 |
|------|------|
| **Canonical Source** | `stores_new` 表 |
| **DB Table** | `stores_new` |
| **Entity** | `Store` |
| **API** | `GET /api/v1/stores`, `POST /api/v1/stores`, `PUT /api/v1/stores/:id` |
| **Frontend** | `src/views/store-ops/StoreArchive.vue` |
| **Truth Source** | ✅ 已裁决 — truth-conflict-map.md 冲突4 |
| **Alternate Source** | 无 |
| **Legacy Source** | `stores` 表 (⚠️ 仍被部分查询使用) |
| **Snapshot Source** | `orders.storeName` (快照) |
| **Derived Source** | `users.storeId` (关联) |
| **Duplicate Source** | 无 |
| **Known Issues** | ⚠️ stores/stores_new 双表并存 (truth-conflict-map.md 冲突4) |
| **Consumers** | StoreService, OrderService, InventoryService |

**引用关系分析**:

| 引用表 | 引用字段 | 引用类型 | 说明 |
|--------|---------|----------|------|
| `users` | `storeId` | REFERENCE | 用户归属门店 |
| `orders` | `storeId` | REFERENCE | 订单归属门店 |
| `orders` | `storeName` | SNAPSHOT | 订单门店名称快照 |
| `store_inventory` | `storeId` | REFERENCE | 门店库存 |
| `employees` | `storeId` | REFERENCE | 员工归属门店 |
| `inventory_transfer` | `fromStoreId/toStoreId` | REFERENCE | 调拨关联 |
| `stores` (旧表) | `id` | ⚠️ DUPLICATE | 旧表仍被部分代码查询 |

---

### 2.6 仓库信息 (Warehouse)

| 维度 | 详情 |
|------|------|
| **Canonical Source** | ⚠️ 未发现独立主表，可能复用 `stores_new` 或独立表 |
| **DB Table** | 待确认（前端有 warehouseApi 调用） |
| **Entity** | `Warehouse` |
| **API** | `GET /api/v1/warehouses` (前端 warehouseApi) |
| **Frontend** | EmployeeFormDialog 中有 warehouseId 字段 |
| **Truth Source** | ⚠️ 待确认 |
| **Known Issues** | 未在 db-reality-map.md 中列出独立仓库表 |

**引用关系分析**:

| 引用表 | 引用字段 | 引用类型 | 说明 |
|--------|---------|----------|------|
| `inventory` | `warehouseId` | REFERENCE | 库存归属仓库 |
| `employees` | `warehouseId` | REFERENCE | 员工归属仓库 |
| `inventory_locations` | `warehouseId` | REFERENCE | 仓位关联 |

---

### 2.7 账户信息 (Bank Account)

| 维度 | 详情 |
|------|------|
| **Canonical Source** | `bank_accounts` 表 |
| **DB Table** | `bank_accounts` |
| **Entity** | `BankAccount` |
| **API** | `GET /api/v1/bank-accounts` |
| **Frontend** | `src/views/finance/BankAccountManage.vue` |
| **Truth Source** | ✅ VERIFIED |
| **Alternate Source** | 无 |
| **Legacy Source** | 无 |
| **Snapshot Source** | `fund_flows.bankAccountId` (引用) |
| **Derived Source** | 无 |
| **Duplicate Source** | 无 |
| **Known Issues** | 无 |
| **Consumers** | PaymentService, ReceiptService, FundFlowService |

**引用关系分析**:

| 引用表 | 引用字段 | 引用类型 | 说明 |
|--------|---------|----------|------|
| `payment` | `bankAccountId` | REFERENCE | 付款账户 |
| `receipt` | `bankAccountId` | REFERENCE | 收款账户 |
| `fund_flows` | `bankAccountId` | REFERENCE | 资金流水关联 |

---

### 2.8 会员信息 (Member)

| 维度 | 详情 |
|------|------|
| **Canonical Source** | `members` 表 |
| **DB Table** | `members` |
| **Entity** | `Member` |
| **API** | `GET /api/v1/members`, `POST /api/v1/members` |
| **Frontend** | `src/views/marketing/MemberList.vue` |
| **Truth Source** | ✅ VERIFIED |
| **Alternate Source** | 无 |
| **Legacy Source** | 无 |
| **Snapshot Source** | 无 |
| **Derived Source** | `member_level` (等级), `member_points_log` (积分) |
| **Duplicate Source** | 无 |
| **Known Issues** | 无 |
| **Consumers** | OrderService, MarketingService |

---

## 三、Reference / Snapshot / Derived / Duplicate 判定矩阵

### 3.1 供应商 (Supplier) 引用链

| 引用位置 | 字段 | 判定 | 依据 |
|----------|------|------|------|
| `purchase_orders.supplierId` | supplierId | **REFERENCE** | 外键引用 suppliers.id |
| `purchase_orders.supplierName` | supplierName | **SNAPSHOT** | 下单时快照，不随供应商改名更新 |
| `payables.supplierId` | supplierId | **REFERENCE** | 外键引用 suppliers.id |
| `payables.supplierName` | supplierName | **SNAPSHOT** | 应付单创建时快照 |
| `material_archives.supplierId` | supplierId | **REFERENCE** | 物料主供应商引用 |
| `material_trace_code.supplierId` | supplierId | **REFERENCE** | 追溯码关联 |

### 3.2 商品 (Food) 引用链

| 引用位置 | 字段 | 判定 | 依据 |
|----------|------|------|------|
| `order_items.foodId` | foodId | **REFERENCE** | 外键引用 foods.id |
| `order_items.foodName` | foodName | **SNAPSHOT** | 下单时快照 |
| `store_inventory.foodId` | foodId | **REFERENCE** | 门店库存关联 |
| `dish_recipes.foodId` | foodId | **REFERENCE** | 菜品配方关联 |
| `food_trace_code.foodId` | foodId | **REFERENCE** | 成品溯源码关联 |
| `food.id` (旧表) | id | **DUPLICATE** | 旧表与新表并存 |

### 3.3 物料 (Material) 引用链

| 引用位置 | 字段 | 判定 | 依据 |
|----------|------|------|------|
| `purchase_orders.materialId` | materialId | **REFERENCE** | 外键引用 material_archives.id |
| `purchase_orders.product_id` | product_id | **OVERRIDDEN** | 遗留列名，语义已废弃，实际指向物料 |
| `inventory.materialId` | materialId | **REFERENCE** | 库存关联 |
| `material_trace_code.materialId` | materialId | **REFERENCE** | 追溯码关联 |

### 3.4 员工 (Employee) 引用链

| 引用位置 | 字段 | 判定 | 依据 |
|----------|------|------|------|
| `users.employeeId` | employeeId | **REFERENCE** | 用户-员工关联 |
| `attendance_record.employeeId` | employeeId | **REFERENCE** | 考勤记录 |
| `salary_records.employeeId` | employeeId | **REFERENCE** | 薪资记录 |
| `approval_record.approverId` | approverId | **REFERENCE** | 审批人引用 |
| `approval_record.approverName` | approverName | **SNAPSHOT** | 审批人姓名快照 |

### 3.5 门店 (Store) 引用链

| 引用位置 | 字段 | 判定 | 依据 |
|----------|------|------|------|
| `users.storeId` | storeId | **REFERENCE** | 用户归属门店 |
| `orders.storeId` | storeId | **REFERENCE** | 订单归属门店 |
| `orders.storeName` | storeName | **SNAPSHOT** | 订单门店名称快照 |
| `store_inventory.storeId` | storeId | **REFERENCE** | 门店库存 |
| `stores.id` (旧表) | id | **DUPLICATE** | 旧表与新表并存 |

---

## 四、数据源类型说明

| 类型 | 定义 | 示例 |
|------|------|------|
| **Canonical Source** | 唯一真相源，所有读写以此为准 | `suppliers`, `foods`, `material_archives` |
| **Alternate Source** | 备用数据源，用于容灾或特殊场景 | 无（当前系统未使用） |
| **Legacy Source** | 遗留数据源，已被新源替代但仍在使用 | `food`(旧), `stores`(旧), `orders_legacy` |
| **Snapshot Source** | 创建时快照，不随源数据更新 | `purchase_orders.supplierName`, `order_items.foodName` |
| **Derived Source** | 从 Canonical Source 衍生计算 | `accounting_subjects.balance`（从流水计算） |
| **Duplicate Source** | 与 Canonical Source 重复的数据 | `food`/`foods`, `stores`/`stores_new` |

---

## 五、Master Data 写入路径

### 5.1 供应商信息写入路径

```
SupplierArchive.vue
  → supplierApi.create/update
    → SupplierService.create/update
      → SupplierRepository.save
        → INSERT/UPDATE suppliers
          → SupplierCreatedEvent / SupplierUpdatedEvent
```

### 5.2 商品信息写入路径

```
FoodManagement.vue
  → foodApi.create/update
    → FoodService.create/update
      → FoodRepository.save
        → INSERT/UPDATE foods
          → 同时 INSERT/UPDATE food (⚠️ 双写)
          → FoodCreatedEvent / FoodUpdatedEvent
```

### 5.3 物料信息写入路径

```
PurchaseArchive.vue
  → materialArchiveApi.create/update
    → MaterialService.create/update
      → MaterialRepository.save
        → INSERT/UPDATE material_archives
          → MaterialCreatedEvent / MaterialUpdatedEvent
```

### 5.4 员工信息写入路径

```
EmployeeFormDialog
  → submitHandler(props)
    → EmployeeService.create/update
      → EmployeeRepository.save
        → INSERT/UPDATE employees
          → EmployeeCreatedEvent / EmployeeUpdatedEvent
            → HREventListener → 更新员工计数
```

### 5.5 门店信息写入路径

```
StoreArchive.vue
  → storeArchiveApi.create/update
    → StoreService.create/update
      → StoreRepository.save
        → INSERT/UPDATE stores_new
```

---

## 六、数据源一致性风险

### 6.1 高风险：Dual-Write (双写)

| 双写对 | 写入触发 | 一致性风险 | 状态 |
|--------|---------|-----------|------|
| `food` + `foods` | 下单/退款/超时 | 🔴 高 — 可能导致库存/状态错乱 | ⚠️ 技术债 |
| `inventory` + `store_inventory` | 采购入库/调拨/收货 | 🟡 中 — 需确保同一事务 | ⚠️ 持续关注 |

### 6.2 高风险：Amount Unit Conflict (金额单位冲突)

| 冲突点 | 新表单位 | Legacy表单位 | 风险 |
|--------|---------|-------------|------|
| `orders.amount` vs `tax_record.amount` | 分 | 元 | 🔴 跨表 JOIN 差100倍 |
| `finance_records.amount` 注释 | 分 | 标注混乱 | 🟡 开发者误解 |

### 6.3 中风险：Status Machine Conflict (状态机冲突)

| 冲突点 | 涉及表 | 严重程度 |
|--------|--------|----------|
| 订单三套状态机 | `order_status`, `payment_status`, `legacy status` | 🔴 高 |
| 凭证前后端错位 | `finance_vouchers.status` (0-3) vs 前端 (1-4) | 🟡 中 |

### 6.4 中风险：Schema Conflict (Schema 冲突)

| 冲突点 | 详情 | 严重程度 |
|--------|------|----------|
| schema.sql vs V1.0.0.100 | 表结构定义不一致 | 🟡 中 |
| V20260717_030 TRUNCATE | 迁移脚本清空生产数据 | 🔴 高 |
| V6.0.0 覆盖 V1.0.0.100 | 同名表重新定义 | 🟡 中 |

---

## 七、主数据依赖图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        主数据依赖全景                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────┐     ┌──────────┐     ┌──────────┐                   │
│  │suppliers │────→│material_ │────→│inventory │                   │
│  │          │     │archives  │     │          │                   │
│  └────┬─────┘     └──────────┘     └────┬─────┘                   │
│       │                                  │                         │
│       │         ┌──────────┐            │                         │
│       ├────────→│purchase_ │────────────┤                         │
│       │         │orders    │            │                         │
│       │         └────┬─────┘            │                         │
│       │              │                  │                         │
│       │         ┌────▼─────┐     ┌──────▼──────┐                 │
│       │         │ orders   │────→│store_       │                 │
│       │         │          │     │inventory    │                 │
│       │         └────┬─────┘     └─────────────┘                 │
│       │              │                                            │
│  ┌────▼─────┐   ┌────▼─────┐     ┌──────────┐                   │
│  │payables  │   │receivables│    │ bank_    │                    │
│  │          │   │           │    │ accounts │                    │
│  └────┬─────┘   └────┬─────┘    └────┬─────┘                    │
│       │              │               │                            │
│       ▼              ▼               ▼                            │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐                     │
│  │ payment  │   │ receipt  │   │fund_flows│                     │
│  └──────────┘   └──────────┘   └──────────┘                     │
│       │              │               │                            │
│       └──────────────┼───────────────┘                            │
│                      ▼                                            │
│              ┌──────────────┐                                     │
│              │finance_      │                                     │
│              │vouchers      │                                     │
│              └──────────────┘                                     │
│                                                                     │
│  ┌──────────┐     ┌──────────┐     ┌──────────┐                  │
│  │employees │────→│attendance│     │salary_   │                  │
│  │          │     │_record   │     │records   │                  │
│  └────┬─────┘     └──────────┘     └──────────┘                  │
│       │                                                            │
│  ┌────▼─────┐     ┌──────────┐                                   │
│  │departments│    │positions │                                    │
│  └──────────┘     └──────────┘                                    │
│                                                                     │
│  ┌──────────┐     ┌──────────┐     ┌──────────┐                  │
│  │stores_new│────→│store_    │     │users     │                  │
│  │          │     │inventory │     │          │                   │
│  └──────────┘     └──────────┘     └──────────┘                  │
│                                                                     │
│  ┌──────────┐                                                      │
│  │ members  │──→ member_level / member_points_log / member_coupon  │
│  └──────────┘                                                      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 八、主数据治理建议

| # | 建议 | 优先级 | 涉及主数据 |
|---|------|--------|-----------|
| 1 | 完成 food/foods 双写清理，废弃 food 表写入 | P0 | 商品 |
| 2 | 完成 stores/stores_new 迁移，废弃 stores 表 | P0 | 门店 |
| 3 | 统一金额单位（全链路分），修正 tax_record/account_balance | P0 | 全局 |
| 4 | 修正 product_id → material_id 列名语义 | P1 | 物料 |
| 5 | 为仓库信息建立独立 Canonical Source 文档 | P1 | 仓库 |
| 6 | 凭证状态码前后端统一 | P2 | 凭证 |
| 7 | 订单状态机统一（或明确映射关系） | P2 | 订单 |
| 8 | 清理遗留表：orders_legacy, voucher_header, sales_order | P3 | 全局 |

---

*生成时间: 2026-09-09*
*数据来源: 代码库静态分析 + 地图文件交叉验证*
