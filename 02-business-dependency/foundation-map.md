# Foundation 对象地图 - Food Traceability System

> 版本: 1.0 | 生成日期: 2026-09-09 | 基于: Project Master Map 静态分析

---

## 一、Foundation 对象总览

| # | Foundation 对象 | Truth Source | Status | Confidence |
|---|----------------|--------------|--------|------------|
| 1 | Organization | departments | VERIFIED | HIGH |
| 2 | Store | stores_new | VERIFIED | HIGH |
| 3 | Employee | employees | VERIFIED | HIGH |
| 4 | Role | roles | VERIFIED | HIGH |
| 5 | Permission | permissions | VERIFIED | HIGH |
| 6 | Supplier | suppliers | VERIFIED | HIGH |
| 7 | Material | material_archives | VERIFIED | HIGH |
| 8 | Food (Product) | foods | VERIFIED | HIGH |
| 9 | Category | food_categories, material_categories | VERIFIED | MEDIUM |
| 10 | Warehouse | inventory.warehouse_id (引用) | PARTIAL | LOW |
| 11 | AccountingSubject | accounting_subjects | VERIFIED | HIGH |
| 12 | BankAccount | bank_accounts | VERIFIED | MEDIUM |
| 13 | Member | members | VERIFIED | MEDIUM |
| 14 | MemberLevel | member_level | VERIFIED | MEDIUM |
| 15 | Device | devices | VERIFIED | LOW |

---

## 二、Foundation 对象详细地图

### 1. Organization (组织架构)

| 属性 | 描述 |
|------|------|
| **Truth Source** | `departments` 表 |
| **Owner** | HR 模块 |
| **Creation Entry** | `DepartmentService.create()`, `DepartmentCreatedEvent` |
| **Lifecycle** | CREATE → UPDATE → DELETE (事件驱动) |
| **Scope** | 全局 (跨门店) |
| **Dependencies** | 无 (ROOT Foundation) |
| **Consumers** | `EmployeeService`, `StoreService`, `DataScopeAspect`, `FinanceService` |
| **Required Capabilities** | 部门 CRUD, 部门树查询, 部门层级管理 |
| **Optional Capabilities** | 部门预算关联, 部门绩效统计 |
| **Evidence** | db-reality-map.md §2, project-master-registry.yaml §database_tables.organization, event-job-map.md §5 |
| **Confidence** | HIGH - 有完整 Migration、Service、Event 支撑 |

**依赖链**:
```
Organization (ROOT)
  └─► Store (依赖 Organization)
  └─► Employee (依赖 Organization + Position)
  └─► Department (自引用 parent_id)
```

---

### 2. Store (门店)

| 属性 | 描述 |
|------|------|
| **Truth Source** | `stores_new` 表 |
| **Owner** | Store 模块 |
| **Creation Entry** | `StoreService.create()` |
| **Lifecycle** | CREATE → UPDATE → DEACTIVATE |
| **Scope** | 门店级 |
| **Dependencies** | `Organization` (部门), `Area` (区域 - 推断) |
| **Consumers** | `OrderService`, `InventoryService`, `StoreInventoryService`, `DataPermissionAspect` |
| **Required Capabilities** | 门店 CRUD, 门店状态管理, 门店经营范围配置 |
| **Optional Capabilities** | 门店日结, 门店公告, 门店证件管理 |
| **Evidence** | db-reality-map.md §2 (stores_new), truth-conflict-map.md §冲突4, permission-data-scope-map.md §2 |
| **Confidence** | HIGH - 新表为唯一真相源 |

**依赖链**:
```
Organization → Store (ROOT Foundation)
  └─► StoreInventory (依赖 Store + Food)
  └─► Order (依赖 Store)
  └─► DataScope (依赖 Store)
  └─► Member (依赖 Store)
```

---

### 3. Employee (员工)

| 属性 | 描述 |
|------|------|
| **Truth Source** | `employees` 表 |
| **Owner** | HR 模块 |
| **Creation Entry** | `EmployeeService.create()`, `EmployeeCreatedEvent` |
| **Lifecycle** | ONBOARD → ACTIVE → TRANSFER → RESIGN |
| **Scope** | 部门级 |
| **Dependencies** | `Organization` (部门), `Position` (职位), `User` (用户账号) |
| **Consumers** | `InvoiceReimbursementService`, `SalaryService`, `AttendanceService` |
| **Required Capabilities** | 员工 CRUD, 入职/离职/调岗, 考勤管理, 薪资管理 |
| **Optional Capabilities** | 健康证管理, 培训管理 |
| **Evidence** | db-reality-map.md §3, event-job-map.md §5, api-map.md §7 |
| **Confidence** | HIGH - 有完整 HR 事件体系 |

**依赖链**:
```
Organization → Employee (ROOT Foundation)
  └─► User (Employee 关联 User)
  └─► InvoiceReimbursement (依赖 Employee)
  └─► Salary (依赖 Employee)
```

---

### 4. Role (角色)

| 属性 | 描述 |
|------|------|
| **Truth Source** | `roles` 表 |
| **Owner** | System 模块 |
| **Creation Entry** | `RoleService.create()` |
| **Lifecycle** | CREATE → UPDATE → DEACTIVATE |
| **Scope** | 全局 |
| **Dependencies** | 无 (ROOT Foundation) |
| **Consumers** | `PermissionService`, `DataScopeAspect`, `DomainVisibleRoles`, `UserRoles` |
| **Required Capabilities** | 角色 CRUD, 角色状态管理 |
| **Optional Capabilities** | 角色数据范围配置, 角色权限分配 |
| **Evidence** | db-reality-map.md §1, permission-data-scope-map.md §2, project-master-registry.yaml §permissions |
| **Confidence** | HIGH - 16 基线角色已定义 |

**依赖链**:
```
Role (ROOT Foundation)
  └─► Permission (Role-Permission 多对多)
  └─► DataScope (Role 关联门店/部门)
  └─► UserRoles (User-Role 多对多)
```

---

### 5. Permission (权限)

| 属性 | 描述 |
|------|------|
| **Truth Source** | `permissions` 表 |
| **Owner** | System 模块 |
| **Creation Entry** | `PermissionService.create()` |
| **Lifecycle** | CREATE → UPDATE → DEACTIVATE |
| **Scope** | 全局 |
| **Dependencies** | 无 (ROOT Foundation) |
| **Consumers** | `PermissionAspect`, `PermissionVerifyService`, `PreAuthorize` |
| **Required Capabilities** | 权限 CRUD, 权限树管理 |
| **Optional Capabilities** | 权限模板, 用户权限覆盖 |
| **Evidence** | db-reality-map.md §1, permission-data-scope-map.md §3, api-map.md §1 |
| **Confidence** | HIGH - 180+ 权限码已定义 |

**依赖链**:
```
Permission (ROOT Foundation)
  └─► RolePermission (Role-Permission 关联)
  └─► UserPermission (User-Permission 直接授权)
  └─► PermissionTemplate (权限模板)
```

---

### 6. Supplier (供应商)

| 属性 | 描述 |
|------|------|
| **Truth Source** | `suppliers` 表 |
| **Owner** | Procurement 模块 |
| **Creation Entry** | `SupplierService.create()` |
| **Lifecycle** | CREATE → APPROVE → ACTIVE → SUSPEND → DEACTIVATE |
| **Scope** | 全局 |
| **Dependencies** | 无 (ROOT Foundation) |
| **Consumers** | `PurchaseOrderService`, `PayableService`, `PaymentService`, `MaterialTraceCodeService` |
| **Required Capabilities** | 供应商 CRUD, 供应商状态管理, 供应商 H5 门户 |
| **Optional Capabilities** | 供应商报价, 供应商合同, 供应商协同 |
| **Evidence** | db-reality-map.md §6, business-object-map.md §6-7, permission-data-scope-map.md §7.2 |
| **Confidence** | HIGH - 有完整采购链支撑 |

**依赖链**:
```
Supplier (ROOT Foundation)
  └─► PurchaseOrder (依赖 Supplier + Material)
  └─► Payable (依赖 Supplier)
  └─► MaterialTraceCode (依赖 Supplier + Material)
  └─► Payment (依赖 Supplier)
```

---

### 7. Material (物料)

| 属性 | 描述 |
|------|------|
| **Truth Source** | `material_archives` 表 |
| **Owner** | Procurement 模块 |
| **Creation Entry** | `MaterialService.create()`, `MaterialCreatedEvent` |
| **Lifecycle** | CREATE → UPDATE → DEACTIVATE |
| **Scope** | 全局 |
| **Dependencies** | `Category` (物料分类), `Supplier` (供应商 - 可选) |
| **Consumers** | `InventoryService`, `TraceabilityService`, `ProductionService`, `PurchaseOrderService` |
| **Required Capabilities** | 物料 CRUD, 物料分类管理, 物料追溯码绑定 |
| **Optional Capabilities** | 物料批次管理, 物料保质期管理 |
| **Evidence** | db-reality-map.md §5-6, business-object-map.md §2, truth-conflict-map.md §2 |
| **Confidence** | HIGH - material_archives 为唯一真相源 |

**依赖链**:
```
Category → Material (ROOT Foundation)
  └─► Inventory (依赖 Material + Warehouse)
  └─► PurchaseOrder (依赖 Material + Supplier)
  └─► MaterialTraceCode (依赖 Material + Supplier)
  └─► DishRecipe (依赖 Material + Food)
```

---

### 8. Food / Product (菜品/产品)

| 属性 | 描述 |
|------|------|
| **Truth Source** | `foods` 表 |
| **Owner** | Product 模块 |
| **Creation Entry** | `FoodService.create()`, `FoodCreatedEvent` |
| **Lifecycle** | CREATE → UPDATE → DISCONTINUE |
| **Scope** | 门店级 (门店经营范围) |
| **Dependencies** | `Category` (菜品分类), `Supplier` (供应商 - 可选), `Material` (物料 - 通过 DishRecipe) |
| **Consumers** | `OrderService`, `InventoryService`, `MenuService`, `StoreInventoryService` |
| **Required Capabilities** | 菜品 CRUD, 菜品分类管理, 菜品配方管理 |
| **Optional Capabilities** | 菜品图片, 菜品标签, 套餐组合 |
| **Evidence** | db-reality-map.md §4, business-object-map.md §1, truth-conflict-map.md §7 |
| **Confidence** | HIGH - foods 为唯一真相源 (food 为 legacy) |

**依赖链**:
```
Category → Food (ROOT Foundation)
  └─► OrderItem (依赖 Food + Order)
  └─► StoreInventory (依赖 Food + Store)
  └─► Menu (依赖 Food + Store)
  └─► DishRecipe (依赖 Food + Material)
  └─► DishCombo (依赖 Food)
```

---

### 9. Category (分类)

| 属性 | 描述 |
|------|------|
| **Truth Source** | `food_categories`, `material_categories` 表 |
| **Owner** | Product / Procurement 模块 |
| **Creation Entry** | `FoodCategoryService.create()`, `MaterialCategoryService.create()` |
| **Lifecycle** | CREATE → UPDATE → DEACTIVATE |
| **Scope** | 全局 |
| **Dependencies** | 无 (ROOT Foundation) |
| **Consumers** | `FoodService`, `MaterialService`, `InventoryService` |
| **Required Capabilities** | 分类 CRUD, 分类树管理 |
| **Optional Capabilities** | 分类排序, 分类层级限制 |
| **Evidence** | db-reality-map.md §4, business-object-map.md §1-2 |
| **Confidence** | MEDIUM - food_categories 和 material_categories 分离 |

**依赖链**:
```
Category (ROOT Foundation)
  └─► Food (依赖 Category)
  └─► Material (依赖 Category)
  └─► Inventory (依赖 Category - 推断)
```

---

### 10. Warehouse (仓库)

| 属性 | 描述 |
|------|------|
| **Truth Source** | 无独立表 - 通过 `inventory.warehouse_id` 引用 |
| **Owner** | Inventory 模块 |
| **Creation Entry** | 无独立创建入口 |
| **Lifecycle** | UNKNOWN |
| **Scope** | 中央仓级别 |
| **Dependencies** | `Organization` (推断) |
| **Consumers** | `InventoryService`, `PurchaseStockinService` |
| **Required Capabilities** | 仓库标识 (ID) |
| **Optional Capabilities** | UNKNOWN |
| **Evidence** | db-reality-map.md §5 (inventory 表有 warehouse_id), project-master-registry.yaml §inventory |
| **Confidence** | LOW - 无独立表，可能是硬编码或配置项 |

**依赖链**:
```
Warehouse (MISSING FOUNDATION)
  └─► Inventory (依赖 Material + Warehouse)
```

---

### 11. AccountingSubject (会计科目)

| 属性 | 描述 |
|------|------|
| **Truth Source** | `accounting_subjects` 表 |
| **Owner** | Finance 模块 |
| **Creation Entry** | `AccountingSubjectService.create()`, `AccountingSubjectCreatedEvent` |
| **Lifecycle** | CREATE → UPDATE → DEACTIVATE |
| **Scope** | 全局 |
| **Dependencies** | 无 (ROOT Foundation) |
| **Consumers** | `VoucherService`, `FinanceReportService`, `TaxService`, `FundFlowService` |
| **Required Capabilities** | 科目 CRUD, 科目树管理, 科目类型 (资产/负债/权益/收入/费用) |
| **Optional Capabilities** | 科目余额自动计算 |
| **Evidence** | db-reality-map.md §7, business-object-map.md §9, truth-conflict-map.md §3 |
| **Confidence** | HIGH - PD-031 已裁决为唯一真相源 |

**依赖链**:
```
AccountingSubject (ROOT Foundation)
  └─► Voucher (依赖 AccountingSubject)
  └─► FinanceRecord (依赖 AccountingSubject)
  └─► FundFlow (依赖 AccountingSubject - 推断)
```

---

### 12. BankAccount (银行账户)

| 属性 | 描述 |
|------|------|
| **Truth Source** | `bank_accounts` 表 |
| **Owner** | Finance 模块 |
| **Creation Entry** | `BankAccountService.create()` |
| **Lifecycle** | CREATE → UPDATE → DEACTIVATE |
| **Scope** | 门店级 / 公司级 |
| **Dependencies** | `Store` (推断), `Organization` (推断) |
| **Consumers** | `FundFlowService`, `PaymentService`, `ReceiptService` |
| **Required Capabilities** | 账户 CRUD, 账户状态管理 |
| **Optional Capabilities** | 账户余额查询 |
| **Evidence** | db-reality-map.md §7 (bank_accounts), business-object-map.md §12-13 |
| **Confidence** | MEDIUM - 有表但详细结构未知 |

**依赖链**:
```
BankAccount (ROOT Foundation)
  └─► FundFlow (依赖 BankAccount)
  └─► Payment (依赖 BankAccount)
  └─► Receipt (依赖 BankAccount)
```

---

### 13. Member (会员)

| 属性 | 描述 |
|------|------|
| **Truth Source** | `members` 表 |
| **Owner** | Marketing 模块 |
| **Creation Entry** | `MemberService.create()` |
| **Lifecycle** | REGISTER → ACTIVE → SUSPEND → DELETE |
| **Scope** | 门店级 |
| **Dependencies** | `MemberLevel` (会员等级), `Store` (推断) |
| **Consumers** | `OrderService`, `PointsService`, `CouponService`, `RechargeService` |
| **Required Capabilities** | 会员 CRUD, 会员积分管理, 会员等级管理 |
| **Optional Capabilities** | 优惠券管理, 充值管理, RFM 模型 |
| **Evidence** | db-reality-map.md §9, project-master-registry.yaml §marketing |
| **Confidence** | MEDIUM - 有完整营销体系支撑 |

**依赖链**:
```
MemberLevel → Member (ROOT Foundation)
  └─► Order (依赖 Member - 可选)
  └─► PointsLog (依赖 Member)
  └─► MemberCoupon (依赖 Member)
  └─► RechargeRecord (依赖 Member)
```

---

### 14. Device (设备)

| 属性 | 描述 |
|------|------|
| **Truth Source** | `devices` 表 |
| **Owner** | Device 模块 |
| **Creation Entry** | `DeviceService.register()` |
| **Lifecycle** | REGISTER → ACTIVE → MAINTENANCE → DEACTIVATE |
| **Scope** | 门店级 |
| **Dependencies** | `Store` (推断) |
| **Consumers** | `OrderService` (POS/Kitchen), `TraceabilityService` |
| **Required Capabilities** | 设备注册, 设备状态管理 |
| **Optional Capabilities** | 设备模板, 设备状态日志 |
| **Evidence** | db-reality-map.md §11, api-map.md §8 |
| **Confidence** | LOW - 详细结构未知 |

**依赖链**:
```
Device (ROOT Foundation)
  └─► DeviceStatusLog (依赖 Device)
  └─► DeviceTemplate (依赖 Device - 推断)
```

---

### 15. Unit (单位)

| 属性 | 描述 |
|------|------|
| **Truth Source** | 无独立表 - 可能存储在字段枚举或配置中 |
| **Owner** | Product 模块 |
| **Creation Entry** | UNKNOWN |
| **Lifecycle** | UNKNOWN |
| **Scope** | 全局 |
| **Dependencies** | 无 (ROOT Foundation) |
| **Consumers** | `MaterialService`, `FoodService`, `InventoryService` |
| **Required Capabilities** | 单位标识 |
| **Optional Capabilities** | 单位换算 |
| **Evidence** | 未发现独立表 - 可能是枚举值 |
| **Confidence** | LOW - 无明确证据 |

---

### 16. Tax (税)

| 属性 | 描述 |
|------|------|
| **Truth Source** | `tax_record` 表 |
| **Owner** | Finance 模块 |
| **Creation Entry** | `TaxRecordService.create()`, `TaxRecordCreatedEvent` |
| **Lifecycle** | CREATE → FILE → PAY |
| **Scope** | 全局 |
| **Dependencies** | `Voucher`, `AccountingSubject` |
| **Consumers** | `FinanceReportService`, `TaxDeclarationService` |
| **Required Capabilities** | 税务记录 CRUD |
| **Optional Capabilities** | 税务申报, 税种管理 |
| **Evidence** | db-reality-map.md §7, business-object-map.md §15, truth-conflict-map.md §1 |
| **Confidence** | MEDIUM - 存在金额单位冲突 (元 vs 分) |

---

### 17. PaymentMethod (支付方式)

| 属性 | 描述 |
|------|------|
| **Truth Source** | 无独立表 - 可能存储在 `orders` 或 `payment` 表字段中 |
| **Owner** | Finance 模块 |
| **Creation Entry** | UNKNOWN |
| **Lifecycle** | UNKNOWN |
| **Scope** | 全局 |
| **Dependencies** | 无 (ROOT Foundation) |
| **Consumers** | `OrderService`, `PaymentService`, `ReceiptService` |
| **Required Capabilities** | 支付方式标识 |
| **Optional Capabilities** | UNKNOWN |
| **Evidence** | 未发现独立表 - 可能是枚举值 (现金/微信/支付宝/会员支付) |
| **Confidence** | LOW - 无明确证据 |

---

### 18. Customer (客户)

| 属性 | 描述 |
|------|------|
| **Truth Source** | 无独立表 - 可能通过 `members` 表或 `orders.customer_id` 引用 |
| **Owner** | Marketing 模块 |
| **Creation Entry** | UNKNOWN |
| **Lifecycle** | UNKNOWN |
| **Scope** | 门店级 |
| **Dependencies** | `Store` (推断) |
| **Consumers** | `OrderService`, `ReceivableService`, `ReceiptService` |
| **Required Capabilities** | 客户标识 |
| **Optional Capabilities** | UNKNOWN |
| **Evidence** | business-object-map.md §5 (Order.Dependencies 包含 Customer), §8 (Receivable.Dependencies 包含 Customer) |
| **Confidence** | LOW - 可能与 Member 合并 |

---

### 19. Price (价格)

| 属性 | 描述 |
|------|------|
| **Truth Source** | 无独立表 - 存储在 `foods.price`, `material_archives.price` 等字段中 |
| **Owner** | Product 模块 |
| **Creation Entry** | 随对象创建 |
| **Lifecycle** | 随对象生命周期 |
| **Scope** | 门店级 |
| **Dependencies** | 依赖所属对象 (Food, Material) |
| **Consumers** | `OrderService`, `InventoryService` |
| **Required Capabilities** | 价格存储 |
| **Optional Capabilities** | 价格历史, 价格策略 |
| **Evidence** | 未发现独立表 - 分散存储在各业务表中 |
| **Confidence** | LOW - 无独立管理能力 |

---

## 三、Foundation 对象依赖关系图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Foundation 对象依赖全景图                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐    │
│  │ Organization│   │    Role     │   │ Permission  │   │  Category   │    │
│  │   (ROOT)    │   │   (ROOT)   │   │   (ROOT)   │   │   (ROOT)   │    │
│  └──────┬──────┘   └──────┬──────┘   └──────┬──────┘   └──────┬──────┘    │
│         │                 │                 │                 │            │
│         ▼                 │                 │                 │            │
│  ┌─────────────┐          │                 │                 │            │
│  │    Store    │          │                 │                 │            │
│  └──────┬──────┘          │                 │                 │            │
│         │                 │                 │                 │            │
│         ▼                 ▼                 ▼                 ▼            │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                           应用层 Foundation                         │  │
│  ├─────────────────────────────────────────────────────────────────────┤  │
│  │                                                                     │  │
│  │  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐              │  │
│  │  │  Employee   │   │  Supplier   │   │   Member    │              │  │
│  │  └──────┬──────┘   └──────┬──────┘   └──────┬──────┘              │  │
│  │         │                 │                 │                      │  │
│  │         ▼                 ▼                 ▼                      │  │
│  │  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐              │  │
│  │  │   Material  │   │    Food     │   │ BankAccount │              │  │
│  │  └──────┬──────┘   └──────┬──────┘   └──────┬──────┘              │  │
│  │         │                 │                 │                      │  │
│  │         ▼                 ▼                 ▼                      │  │
│  │  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐              │  │
│  │  │ Accounting  │   │  Warehouse  │   │   Device    │              │  │
│  │  │  Subject    │   │  (MISSING)  │   │             │              │  │
│  │  └─────────────┘   └─────────────┘   └─────────────┘              │  │
│  │                                                                     │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 四、Foundation 对象统计

### 4.1 按状态统计

| 状态 | 数量 | 对象 |
|------|------|------|
| VERIFIED | 12 | Organization, Store, Employee, Role, Permission, Supplier, Material, Food, Category, AccountingSubject, BankAccount, Member |
| PARTIAL | 2 | Device, Tax |
| MISSING | 3 | Warehouse, Unit, PaymentMethod |
| UNKNOWN | 2 | Customer, Price |

### 4.2 按依赖层级统计

| 层级 | 数量 | 对象 |
|------|------|------|
| ROOT (无依赖) | 6 | Organization, Role, Permission, Category, AccountingSubject, BankAccount |
| Level 1 (依赖 ROOT) | 4 | Store, Employee, Supplier, Member |
| Level 2 (依赖 Level 1) | 3 | Material, Food, Device |
| MISSING | 2 | Warehouse, Unit |
| UNKNOWN | 4 | Customer, Price, Tax, PaymentMethod |

### 4.3 按 Owner 统计

| Owner | 数量 | 对象 |
|-------|------|------|
| System 模块 | 2 | Role, Permission |
| HR 模块 | 2 | Organization, Employee |
| Store 模块 | 1 | Store |
| Procurement 模块 | 2 | Supplier, Material |
| Product 模块 | 2 | Food, Category |
| Finance 模块 | 3 | AccountingSubject, BankAccount, Tax |
| Marketing 模块 | 1 | Member |
| Device 模块 | 1 | Device |
| Inventory 模块 | 1 | Warehouse |
| UNKNOWN | 4 | Unit, PaymentMethod, Customer, Price |

---

## 五、关键发现

### 5.1 缺失的 Foundation 对象

| 缺失对象 | 影响范围 | 严重程度 | 说明 |
|----------|----------|----------|------|
| Warehouse | 库存管理 | HIGH | 无独立表，通过 inventory.warehouse_id 引用，可能是硬编码 |
| Unit | 物料/菜品管理 | MEDIUM | 无独立表，可能是枚举值 |
| PaymentMethod | 支付流程 | MEDIUM | 无独立表，可能是枚举值 |
| Customer | 订单/财务 | LOW | 可能与 Member 合并 |
| Price | 价格管理 | LOW | 分散存储在各业务表中 |

### 5.2 Legacy 冲突的 Foundation 对象

| 对象 | 真相源 | Legacy | 冲突说明 |
|------|--------|--------|----------|
| Store | stores_new | stores | 新旧表并存，字段结构不同 |
| Food | foods | food | 新旧表并存，双写风险 |
| Category | food_categories | food_category | 新旧表并存 |
| AccountingSubject | accounting_subjects | account_balance | 余额双轨 |

### 5.3 需要 Product Decision 的 Foundation 对象

| 对象 | 编号 | 决策项 | 状态 |
|------|------|--------|------|
| Warehouse | PD-XXX | Warehouse 管理模式 (独立表 vs 配置项) | 待决 |
| Unit | PD-XXX | Unit 管理模式 (独立表 vs 枚举) | 待决 |
| PaymentMethod | PD-XXX | PaymentMethod 管理模式 (独立表 vs 枚举) | 待决 |
| Customer | PD-XXX | Customer 与 Member 的关系 | 待决 |

---

## 六、建议行动项

### 6.1 高优先级 (P0)

1. **确认 Warehouse 管理模式**
   - 当前通过 `inventory.warehouse_id` 引用，无独立表
   - 需确认是否需要独立的仓库管理
   - 建议：创建 `warehouses` 表或确认为配置项

2. **确认 Unit 管理模式**
   - 当前无独立表，可能是枚举值
   - 需确认是否需要动态单位管理
   - 建议：创建 `units` 表或确认为枚举

3. **确认 PaymentMethod 管理模式**
   - 当前无独立表，可能是枚举值
   - 需确认是否需要动态支付方式管理
   - 建议：创建 `payment_methods` 表或确认为枚举

### 6.2 中优先级 (P1)

4. **确认 Customer 与 Member 的关系**
   - 当前 Customer 无独立表，可能与 Member 合并
   - 需确认是否需要独立的客户管理
   - 建议：明确 Customer 与 Member 的边界

5. **清理 Legacy 冲突**
   - `stores` → `stores_new` 迁移
   - `food` → `foods` 迁移
   - `food_category` → `food_categories` 迁移
   - `account_balance` → `accounting_subjects.balance` 迁移

### 6.3 低优先级 (P2)

6. **统一 Price 管理**
   - 当前价格分散存储在各业务表中
   - 可考虑创建 `price_list` 或 `price_policy` 表
   - 建议：暂不处理，保持现状

7. **统一 Tax 管理**
   - 当前存在金额单位冲突 (元 vs 分)
   - 需在所有跨表查询处添加单位转换
   - 建议：逐步统一为「分」

---

*生成时间: 2026-09-09*
*数据来源: Project Master Map 静态分析*
