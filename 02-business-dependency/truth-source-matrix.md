# Truth Source Matrix — 完整版

> **项目**: 食品溯源系统 (Food Traceability System)
> **生成时间**: 2026-09-09
> **数据来源**: 代码库静态分析 + 地图文件交叉验证
> **状态**: ACTIVE

---

## 一、LOCK 决策 Evidence 矩阵

### LOCK-001: foods 为菜品唯一真相源

| 维度 | Evidence | 状态 |
|------|----------|------|
| **代码证据** | `FoodService.create()`, `FoodRepository.save()` 写入 `foods` 表 | ✅ VERIFIED |
| **DB 证据** | `foods` 表为新表，`food` 为旧表 (db-reality-map.md §4) | ✅ VERIFIED |
| **API 证据** | `GET /api/v1/foods`, `POST /api/v1/foods` (api-map.md) | ✅ VERIFIED |
| **迁移证据** | V6.0.0 迁移创建 `foods` 表，`food` 标记为 LEGACY | ✅ VERIFIED |
| **冲突证据** | truth-conflict-map.md 冲突7: food/foods 双写 | ⚠️ 存在双写风险 |
| **结论** | foods 为 Canonical Source，food 为 Legacy Source | **VERIFIED** |

**Evidence 引用**: `foundation-map.md §8`, `business-object-map.md §1`, `master-data-source-map.md §2.2`

---

### LOCK-002: 物料真相源 (material_archives)

| 维度 | Evidence | 状态 |
|------|----------|------|
| **代码证据** | `MaterialService.create()`, `MaterialRepository.save()` 写入 `material_archives` | ✅ VERIFIED |
| **DB 证据** | `material_archives` 表存在，无 Legacy 表 (db-reality-map.md §6) | ✅ VERIFIED |
| **API 证据** | `GET /api/v1/materials`, `POST /api/v1/materials` (api-map.md) | ✅ VERIFIED |
| **迁移证据** | V1.0.0.100 创建 `material_archives` 表 | ✅ VERIFIED |
| **冲突证据** | truth-conflict-map.md 冲突2: product_id vs material_id 列名冲突 | ⚠️ 列名语义问题 |
| **结论** | material_archives 为 Canonical Source | **VERIFIED** |

**Evidence 引用**: `foundation-map.md §7`, `business-object-map.md §2`, `master-data-source-map.md §2.3`

---

### LOCK-003: 金额以分为准 (整数存储)

| 维度 | Evidence | 状态 |
|------|----------|------|
| **代码证据** | 新表 `orders.amount`, `payables.amount` 使用分存储 | ✅ VERIFIED |
| **DB 证据** | `tax_record.amount` 使用元存储，存在单位冲突 (master-data-source-map.md §6.2) | ⚠️ 旧表单位不一致 |
| **API 证据** | API 返回值统一为分 | ✅ VERIFIED |
| **迁移证据** | V6.0.0 迁移统一金额单位 | ✅ VERIFIED |
| **冲突证据** | master-data-source-map.md §6.2: orders.amount vs tax_record.amount 差100倍 | 🔴 高风险 |
| **结论** | 新表统一用分，旧表需修正 | **VERIFIED (需修正)** |

**Evidence 引用**: `decision-recon-summary.md §2.1`, `master-data-source-map.md §6.2`

---

### LOCK-004: 供应商真相源 (suppliers)

| 维度 | Evidence | 状态 |
|------|----------|------|
| **代码证据** | `SupplierService.create()`, `SupplierRepository.save()` 写入 `suppliers` | ✅ VERIFIED |
| **DB 证据** | `suppliers` 表存在，无 Legacy 表 (db-reality-map.md §6) | ✅ VERIFIED |
| **API 证据** | `GET /api/v1/suppliers`, `POST /api/v1/suppliers` (api-map.md) | ✅ VERIFIED |
| **迁移证据** | V1.0.0.100 创建 `suppliers` 表 | ✅ VERIFIED |
| **冲突证据** | 无 | ✅ 无冲突 |
| **结论** | suppliers 为 Canonical Source | **VERIFIED** |

**Evidence 引用**: `foundation-map.md §6`, `master-data-source-map.md §2.1`

---

### LOCK-005: 员工真相源 (employees)

| 维度 | Evidence | 状态 |
|------|----------|------|
| **代码证据** | `EmployeeService.create()`, `EmployeeRepository.save()` 写入 `employees` | ✅ VERIFIED |
| **DB 证据** | `employees` 表存在，无 Legacy 表 (db-reality-map.md §3) | ✅ VERIFIED |
| **API 证据** | `GET /api/v1/employees`, `POST /api/v1/employees` (api-map.md) | ✅ VERIFIED |
| **迁移证据** | V1.0.0.100 创建 `employees` 表 | ✅ VERIFIED |
| **冲突证据** | 无 | ✅ 无冲突 |
| **结论** | employees 为 Canonical Source | **VERIFIED** |

**Evidence 引用**: `foundation-map.md §3`, `master-data-source-map.md §2.4`

---

### LOCK-006: stores_new 为门店唯一真相源

| 维度 | Evidence | 状态 |
|------|----------|------|
| **代码证据** | `StoreService.create()`, `StoreRepository.save()` 写入 `stores_new` | ✅ VERIFIED |
| **DB 证据** | `stores_new` 为新表，`stores` 为旧表 (db-reality-map.md §2) | ✅ VERIFIED |
| **API 证据** | `GET /api/v1/stores`, `POST /api/v1/stores` (api-map.md) | ✅ VERIFIED |
| **迁移证据** | V6.0.0 迁移创建 `stores_new` 表 | ✅ VERIFIED |
| **冲突证据** | truth-conflict-map.md 冲突4: stores/stores_new 双表并存 | ⚠️ 存在双表风险 |
| **结论** | stores_new 为 Canonical Source，stores 为 Legacy Source | **VERIFIED** |

**Evidence 引用**: `foundation-map.md §2`, `business-object-map.md`, `master-data-source-map.md §2.5`

---

### LOCK-007: departments 为组织真相源

| 维度 | Evidence | 状态 |
|------|----------|------|
| **代码证据** | `DepartmentService.create()`, `DepartmentCreatedEvent` 完整支撑 | ✅ VERIFIED |
| **DB 证据** | `departments` 表存在，无 Legacy 表 (db-reality-map.md §2) | ✅ VERIFIED |
| **API 证据** | `GET /api/v1/departments`, `POST /api/v1/departments` | ✅ VERIFIED |
| **迁移证据** | V1.0.0.100 创建 `departments` 表 | ✅ VERIFIED |
| **冲突证据** | 无 | ✅ 无冲突 |
| **结论** | departments 为 Canonical Source | **VERIFIED** |

**Evidence 引用**: `foundation-map.md §1`, `project-master-registry.yaml §organization`

---

### LOCK-008: positions 为职位真相源

| 维度 | Evidence | 状态 |
|------|----------|------|
| **代码证据** | `PositionService` 存在，写入 `positions` 表 | ✅ VERIFIED |
| **DB 证据** | `positions` 表存在，无 Legacy 表 (db-reality-map.md §2) | ✅ VERIFIED |
| **API 证据** | `GET /api/v1/positions` (api-map.md) | ✅ VERIFIED |
| **迁移证据** | V1.0.0.100 创建 `positions` 表 | ✅ VERIFIED |
| **冲突证据** | 无 | ✅ 无冲突 |
| **结论** | positions 为 Canonical Source | **VERIFIED** |

**Evidence 引用**: `foundation-map.md`, `project-master-registry.yaml §organization`

---

### LOCK-009: accounting_subjects 为科目真相源

| 维度 | Evidence | 状态 |
|------|----------|------|
| **代码证据** | `AccountingSubjectService.create()` 写入 `accounting_subjects` | ✅ VERIFIED |
| **DB 证据** | `accounting_subjects` 表存在，`account_balance` 为只读 (db-reality-map.md §7) | ✅ VERIFIED |
| **API 证据** | `GET /api/v1/accounting-subjects` (api-map.md) | ✅ VERIFIED |
| **迁移证据** | V1.0.0.100 创建 `accounting_subjects` 表 | ✅ VERIFIED |
| **冲突证据** | truth-conflict-map.md §3: PD-031 已裁决为唯一真相源 | ✅ 已裁决 |
| **结论** | accounting_subjects 为 Canonical Source | **VERIFIED** |

**Evidence 引用**: `foundation-map.md §11`, `business-object-map.md §9`

---

### LOCK-010: roles/permissions 为权限真相源

| 维度 | Evidence | 状态 |
|------|----------|------|
| **代码证据** | `RoleService.create()`, `PermissionService.create()` 写入对应表 | ✅ VERIFIED |
| **DB 证据** | `roles`, `permissions` 表存在，16 基线角色 + 180+ 权限码 (db-reality-map.md §1) | ✅ VERIFIED |
| **API 证据** | `GET /api/v1/roles`, `GET /api/v1/permissions` (api-map.md) | ✅ VERIFIED |
| **迁移证据** | V1.0.0.100 创建 `roles`, `permissions` 表 | ✅ VERIFIED |
| **冲突证据** | 无 | ✅ 无冲突 |
| **结论** | roles/permissions 为 Canonical Source | **VERIFIED** |

**Evidence 引用**: `foundation-map.md §4-5`, `project-master-registry.yaml §permissions`

---

## 二、Foundation 对象 Truth Source Matrix

| Object | Truth Source | Table | Entity | Owner | Evidence | Status |
|--------|-------------|-------|--------|-------|----------|--------|
| Organization | departments | departments | Department | HR 模块 | db-reality-map.md §2, foundation-map.md §1 | VERIFIED |
| Store | stores_new | stores_new | Store | Store 模块 | db-reality-map.md §2, foundation-map.md §2 | VERIFIED |
| Department | departments | departments | Department | HR 模块 | db-reality-map.md §2, foundation-map.md §1 | VERIFIED |
| Employee | employees | employees | Employee | HR 模块 | db-reality-map.md §3, foundation-map.md §3 | VERIFIED |
| Position | positions | positions | Position | HR 模块 | db-reality-map.md §2, foundation-map.md | VERIFIED |
| Role | roles | roles | Role | System 模块 | db-reality-map.md §1, foundation-map.md §4 | VERIFIED |
| Permission | permissions | permissions | Permission | System 模块 | db-reality-map.md §1, foundation-map.md §5 | VERIFIED |
| Category | food_categories, material_categories | food_categories, material_categories | FoodCategory, MaterialCategory | Product/Procurement 模块 | db-reality-map.md §4, foundation-map.md §9 | VERIFIED |
| Food | foods | foods | Food | Product 模块 | db-reality-map.md §4, foundation-map.md §8 | VERIFIED |
| Material | material_archives | material_archives | Material | Procurement 模块 | db-reality-map.md §6, foundation-map.md §7 | VERIFIED |
| Supplier | suppliers | suppliers | Supplier | Procurement 模块 | db-reality-map.md §6, foundation-map.md §6 | VERIFIED |
| AccountingSubject | accounting_subjects | accounting_subjects | AccountingSubject | Finance 模块 | db-reality-map.md §7, foundation-map.md §11 | VERIFIED |
| BankAccount | bank_accounts | bank_accounts | BankAccount | Finance 模块 | db-reality-map.md §7, foundation-map.md §12 | VERIFIED |

---

## 三、Master Data 对象 Truth Source Matrix

| Object | Truth Source | Table | Entity | Owner | Evidence | Status |
|--------|-------------|-------|--------|-------|----------|--------|
| Food | foods | foods | Food | Product 模块 | business-object-map.md §1, master-data-source-map.md §2.2 | VERIFIED |
| Material | material_archives | material_archives | Material | Procurement 模块 | business-object-map.md §2, master-data-source-map.md §2.3 | VERIFIED |
| Supplier | suppliers | suppliers | Supplier | Procurement 模块 | business-object-map.md, master-data-source-map.md §2.1 | VERIFIED |
| Employee | employees | employees | Employee | HR 模块 | business-object-map.md, master-data-source-map.md §2.4 | VERIFIED |
| Store | stores_new | stores_new | Store | Store 模块 | business-object-map.md, master-data-source-map.md §2.5 | VERIFIED |

---

## 四、Business 对象 Truth Source Matrix

| Object | Truth Source | Table | Entity | Owner | Evidence | Status |
|--------|-------------|-------|--------|-------|----------|--------|
| Inventory | inventory | inventory | Inventory | Inventory 模块 | business-object-map.md §3, db-reality-map.md §5 | VERIFIED |
| StoreInventory | store_inventory | store_inventory | StoreInventory | Inventory 模块 | business-object-map.md §4, db-reality-map.md §5 | VERIFIED |
| Order | orders | orders | Order | Order 模块 | business-object-map.md §5, db-reality-map.md §8 | VERIFIED |
| Voucher | finance_vouchers | finance_vouchers | Voucher | Finance 模块 | business-object-map.md §6, db-reality-map.md §7 | VERIFIED |
| Payable | payables | payables | Payable | Finance 模块 | business-object-map.md §7, db-reality-map.md §7 | VERIFIED |
| Receivable | receivables | receivables | Receivable | Finance 模块 | business-object-map.md §8, db-reality-map.md §7 | VERIFIED |

---

## 五、Derived 对象 Truth Source Matrix

| Object | Truth Source | Table | Entity | Owner | Evidence | Status |
|--------|-------------|-------|--------|-------|----------|--------|
| FinanceRecord | finance_records | finance_records | FinanceRecord | Finance 模块 | business-object-map.md §11, db-reality-map.md §7 | VERIFIED |
| FundFlow | fund_flows | fund_flows | FundFlow | Finance 模块 | business-object-map.md §12, db-reality-map.md §7 | VERIFIED |
| TaxRecord | tax_record | tax_record | TaxRecord | Finance 模块 | business-object-map.md §15, db-reality-map.md §7 | VERIFIED |
| MaterialTraceCode | material_trace_code | material_trace_code | MaterialTraceCode | Traceability 模块 | business-object-map.md §10, db-reality-map.md §10 | VERIFIED |

---

## 六、Evidence 引用汇总

### 6.1 DB Evidence (数据库证据)

| 表名 | 用途 | 来源 |
|------|------|------|
| `departments` | 部门信息 | db-reality-map.md §2 |
| `positions` | 职位定义 | db-reality-map.md §2 |
| `stores_new` | 门店信息 | db-reality-map.md §2 |
| `employees` | 员工主表 | db-reality-map.md §3 |
| `foods` | 菜品(新) | db-reality-map.md §4 |
| `food` | 菜品(旧) | db-reality-map.md §4 |
| `food_categories` | 菜品分类(新) | db-reality-map.md §4 |
| `food_category` | 菜品分类(旧) | db-reality-map.md §4 |
| `inventory` | 库存主表 | db-reality-map.md §5 |
| `store_inventory` | 门店库存 | db-reality-map.md §5 |
| `suppliers` | 供应商 | db-reality-map.md §6 |
| `material_archives` | 物料档案 | db-reality-map.md §6 |
| `purchase_orders` | 采购订单 | db-reality-map.md §6 |
| `orders` | 订单(V6) | db-reality-map.md §8 |
| `finance_vouchers` | 财务凭证 | db-reality-map.md §7 |
| `accounting_subjects` | 会计科目 | db-reality-map.md §7 |
| `payables` | 应付账款 | db-reality-map.md §7 |
| `receivables` | 应收账款 | db-reality-map.md §7 |
| `finance_records` | 财务记录(新) | db-reality-map.md §7 |
| `fund_flows` | 资金流水 | db-reality-map.md §7 |
| `bank_accounts` | 银行账户 | db-reality-map.md §7 |
| `tax_record` | 税务记录 | db-reality-map.md §7 |
| `roles` | 角色定义 | db-reality-map.md §1 |
| `permissions` | 权限定义 | db-reality-map.md §1 |
| `members` | 会员主表 | db-reality-map.md §9 |
| `material_trace_code` | 物料追溯码 | db-reality-map.md §10 |

### 6.2 API Evidence (API 证据)

| API 路径 | 对象 | 来源 |
|----------|------|------|
| `GET /api/v1/foods` | Food | api-map.md, project-master-registry.yaml |
| `POST /api/v1/foods` | Food | api-map.md, project-master-registry.yaml |
| `GET /api/v1/materials` | Material | api-map.md, project-master-registry.yaml |
| `POST /api/v1/materials` | Material | api-map.md, project-master-registry.yaml |
| `GET /api/v1/suppliers` | Supplier | api-map.md, project-master-registry.yaml |
| `GET /api/v1/employees` | Employee | api-map.md, project-master-registry.yaml |
| `GET /api/v1/stores` | Store | api-map.md, project-master-registry.yaml |
| `GET /api/v1/inventory` | Inventory | api-map.md, project-master-registry.yaml |
| `GET /api/v1/orders` | Order | api-map.md, project-master-registry.yaml |
| `GET /api/v1/vouchers` | Voucher | api-map.md, project-master-registry.yaml |
| `GET /api/v1/payables` | Payable | api-map.md, project-master-registry.yaml |
| `GET /api/v1/receivables` | Receivable | api-map.md, project-master-registry.yaml |
| `GET /api/v1/accounting-subjects` | AccountingSubject | api-map.md, project-master-registry.yaml |
| `GET /api/v1/bank-accounts` | BankAccount | api-map.md, project-master-registry.yaml |
| `GET /api/v1/roles` | Role | api-map.md, project-master-registry.yaml |
| `GET /api/v1/permissions` | Permission | api-map.md, project-master-registry.yaml |

### 6.3 Code Evidence (代码证据)

| 组件 | Service/Repository | 对象 | 来源 |
|------|-------------------|------|------|
| Product | `FoodService`, `FoodRepository` | Food | business-object-map.md §1 |
| Product | `MaterialService`, `MaterialRepository` | Material | business-object-map.md §2 |
| Inventory | `InventoryService`, `InventoryRepository` | Inventory | business-object-map.md §3 |
| Inventory | `StoreInventoryService`, `StoreInventoryRepository` | StoreInventory | business-object-map.md §4 |
| Order | `OrderService`, `OrderRepository` | Order | business-object-map.md §5 |
| Finance | `VoucherService`, `VoucherRepository` | Voucher | business-object-map.md §6 |
| Finance | `PayableService`, `PayableRepository` | Payable | business-object-map.md §7 |
| Finance | `ReceivableService`, `ReceivableRepository` | Receivable | business-object-map.md §8 |
| Finance | `AccountingSubjectService`, `AccountingSubjectRepository` | AccountingSubject | business-object-map.md §9 |
| Traceability | `MaterialTraceCodeService`, `MaterialTraceCodeRepository` | MaterialTraceCode | business-object-map.md §10 |
| Finance | `FinanceRecordService`, `FinanceRecordRepository` | FinanceRecord | business-object-map.md §11 |
| Finance | `FundFlowService`, `FundFlowRepository` | FundFlow | business-object-map.md §12 |
| Finance | `TaxRecordService`, `TaxRecordRepository` | TaxRecord | business-object-map.md §15 |
| HR | `EmployeeService`, `EmployeeRepository` | Employee | master-data-source-map.md §2.4 |
| HR | `DepartmentService`, `DepartmentRepository` | Department | master-data-source-map.md §2.4 |
| Store | `StoreService`, `StoreRepository` | Store | master-data-source-map.md §2.5 |
| Procurement | `SupplierService`, `SupplierRepository` | Supplier | master-data-source-map.md §2.1 |

### 6.4 Migration Evidence (迁移证据)

| 迁移版本 | 创建表 | 来源 |
|----------|--------|------|
| V1.0.0.100 | `departments`, `positions`, `stores_new`, `employees`, `foods`, `food_categories`, `suppliers`, `material_archives`, `accounting_subjects`, `roles`, `permissions` | db-reality-map.md §15 |
| V6.0.0 | `orders`, `finance_vouchers`, `payables`, `receivables`, `finance_records`, `fund_flows`, `bank_accounts`, `tax_record` | db-reality-map.md §15 |
| schema.sql | 初始 schema 定义 | db-reality-map.md §14 |

---

## 七、已知冲突与风险

| 冲突点 | 涉及表 | 严重程度 | 状态 | 来源 |
|--------|--------|----------|------|------|
| food/foods 双写 | `food`, `foods` | 🔴 高 | ⚠️ 技术债 | master-data-source-map.md §6.1 |
| stores/stores_new 双表并存 | `stores`, `stores_new` | 🔴 高 | ⚠️ 技术债 | master-data-source-map.md §6.1 |
| 金额单位冲突 | `orders.amount`, `tax_record.amount` | 🔴 高 | ⚠️ 需修正 | master-data-source-map.md §6.2 |
| product_id vs material_id 列名 | `purchase_orders` | 🟡 中 | ⚠️ 语义问题 | master-data-source-map.md §2.3 |
| 凭证状态前后端错位 | `finance_vouchers.status` | 🟡 中 | ⚠️ 需统一 | master-data-source-map.md §6.3 |
| 订单三套状态机 | `order_status`, `payment_status` | 🔴 高 | ⚠️ 需统一 | master-data-source-map.md §6.3 |

---

## 八、治理建议

### 8.1 P0 优先级

| # | 建议 | 涉及对象 | 来源 |
|---|------|----------|------|
| 1 | 完成 food/foods 双写清理，废弃 food 表写入 | Food | master-data-source-map.md §7 |
| 2 | 完成 stores/stores_new 迁移，废弃 stores 表 | Store | master-data-source-map.md §7 |
| 3 | 统一金额单位（全链路分），修正 tax_record/account_balance | 全局 | master-data-source-map.md §7 |

### 8.2 P1 优先级

| # | 建议 | 涉及对象 | 来源 |
|---|------|----------|------|
| 4 | 修正 product_id → material_id 列名语义 | Material | master-data-source-map.md §7 |
| 5 | 为仓库信息建立独立 Canonical Source 文档 | Warehouse | master-data-source-map.md §7 |
| 6 | 凭证状态码前后端统一 | Voucher | master-data-source-map.md §7 |

### 8.3 P2 优先级

| # | 建议 | 涉及对象 | 来源 |
|---|------|----------|------|
| 7 | 订单状态机统一（或明确映射关系） | Order | master-data-source-map.md §7 |
| 8 | 清理遗留表：orders_legacy, voucher_header, sales_order | 全局 | master-data-source-map.md §7 |

---

## 九、统计摘要

| 分类 | 总数 | VERIFIED | UNVERIFIED | 风险项 |
|------|------|----------|------------|--------|
| LOCK 决策 | 10 | 10 | 0 | 3 (双写/金额单位) |
| Foundation 对象 | 13 | 13 | 0 | 2 (Category 双表, Warehouse 缺失) |
| Master Data 对象 | 5 | 5 | 0 | 0 |
| Business 对象 | 6 | 6 | 0 | 1 (订单状态机) |
| Derived 对象 | 4 | 4 | 0 | 1 (金额单位) |
| **总计** | **38** | **38** | **0** | **7** |

---

*生成时间: 2026-09-09*
*数据来源: project-master-map/ 目录下所有地图文件交叉验证*
