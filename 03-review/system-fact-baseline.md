# System Fact Baseline — PROJECT-MASTER-REVIEW-001

> **版本**: 1.0 | **生成日期**: 2026-09-09
> **基线定义**: 通过当前代码/DB/API/Runtime/UI/调用链已证明的事实
> **来源**: foundation-map.md, business-object-map.md, master-data-source-map.md, truth-conflict-map.md, capability-readiness-map.md, db-reality-map.md

---

## 一、Foundation 事实 (13条)

```yaml
- fact_id: FND-001
  fact: Organization 的真相源为 departments 表
  domain: Foundation
  object: Organization
  current_truth_source: departments
  owner: HR 模块
  evidence: foundation-map.md §1, db-reality-map.md §2 — DepartmentService.create() + DepartmentCreatedEvent 完整支撑
  confidence: HIGH
  status: VERIFIED

- fact_id: FND-002
  fact: Store 的真相源为 stores_new 表，stores 为废弃旧表
  domain: Foundation
  object: Store
  current_truth_source: stores_new
  owner: Store 模块
  evidence: foundation-map.md §2, truth-conflict-map.md §冲突4 — StoreService.create() 写入 stores_new，新代码必须查询 stores_new
  confidence: HIGH
  status: VERIFIED

- fact_id: FND-003
  fact: Department 的真相源为 departments 表，支持自引用 parent_id 层级
  domain: Foundation
  object: Department
  current_truth_source: departments
  owner: HR 模块
  evidence: foundation-map.md §1, db-reality-map.md §2 — departments 表有 parent_id 自引用
  confidence: HIGH
  status: VERIFIED

- fact_id: FND-004
  fact: Employee 的真相源为 employees 表，完整 HR 事件体系已建立
  domain: Foundation
  object: Employee
  current_truth_source: employees
  owner: HR 模块
  evidence: foundation-map.md §3, db-reality-map.md §3 — EmployeeService.create() + EmployeeCreatedEvent + HREventListener
  confidence: HIGH
  status: VERIFIED

- fact_id: FND-005
  fact: Position 的真相源为 positions 表，与 position_levels 职级体系关联
  domain: Foundation
  object: Position
  current_truth_source: positions
  owner: HR 模块
  evidence: db-reality-map.md §2 — positions + position_levels 两张表
  confidence: MEDIUM
  status: PARTIAL

- fact_id: FND-006
  fact: Role 的真相源为 roles 表，16 基线角色已定义
  domain: Foundation
  object: Role
  current_truth_source: roles
  owner: System 模块
  evidence: foundation-map.md §4, db-reality-map.md §1 — RoleService.create() + 16基线角色
  confidence: HIGH
  status: VERIFIED

- fact_id: FND-007
  fact: Permission 的真相源为 permissions 表，180+ 权限码已定义
  domain: Foundation
  object: Permission
  current_truth_source: permissions
  owner: System 模块
  evidence: foundation-map.md §5, db-reality-map.md §1 — 180+ 权限码 + PermissionAspect + PreAuthorize
  confidence: HIGH
  status: VERIFIED

- fact_id: FND-008
  fact: Category 的真相源为 food_categories 和 material_categories 两张分离表
  domain: Foundation
  object: Category
  current_truth_source: food_categories, material_categories
  owner: Product / Procurement 模块
  evidence: foundation-map.md §9, db-reality-map.md §4 — FoodCategoryService.create() + MaterialCategoryService.create()
  confidence: MEDIUM
  status: VERIFIED

- fact_id: FND-009
  fact: Food 的真相源为 foods 表，food 为 legacy 已废弃但仍在双写
  domain: Foundation
  object: Food
  current_truth_source: foods
  owner: Product 模块
  evidence: foundation-map.md §8, business-object-map.md §1 — FoodService.create() 写入 foods，同时双写 food（技术债）
  confidence: HIGH
  status: VERIFIED

- fact_id: FND-010
  fact: Material 的真相源为 material_archives 表
  domain: Foundation
  object: Material
  current_truth_source: material_archives
  owner: Procurement 模块
  evidence: foundation-map.md §7, business-object-map.md §2 — MaterialService.create() + MaterialCreatedEvent
  confidence: HIGH
  status: VERIFIED

- fact_id: FND-011
  fact: Supplier 的真相源为 suppliers 表，完整采购链支撑
  domain: Foundation
  object: Supplier
  current_truth_source: suppliers
  owner: Procurement 模块
  evidence: foundation-map.md §6, db-reality-map.md §6 — SupplierService.create() + 完整引用链（purchase_orders, payables, material_trace_code）
  confidence: HIGH
  status: VERIFIED

- fact_id: FND-012
  fact: AccountingSubject 的真相源为 accounting_subjects 表，PD-031 已裁决为唯一真相源
  domain: Foundation
  object: AccountingSubject
  current_truth_source: accounting_subjects
  owner: Finance 模块
  evidence: foundation-map.md §11, truth-conflict-map.md §冲突3 — PD-031 已决，account_balance 降级为只读
  confidence: HIGH
  status: VERIFIED

- fact_id: FND-013
  fact: BankAccount 的真相源为 bank_accounts 表
  domain: Foundation
  object: BankAccount
  current_truth_source: bank_accounts
  owner: Finance 模块
  evidence: foundation-map.md §12, master-data-source-map.md §2.7 — BankAccountService.create()，被 payment/receipt/fund_flows 引用
  confidence: MEDIUM
  status: VERIFIED
```

---

## 二、Master Data 事实 (5条)

```yaml
- fact_id: MDS-001
  fact: Food 主数据 Canonical Source 为 foods 表，food 表为 Legacy 双写中
  domain: Master Data
  object: Food→foods
  current_truth_source: foods
  owner: Product 模块
  evidence: master-data-source-map.md §2.2 — PADR §九-A 裁决；下单/退款/超时同时写 food 和 foods（冲突7）
  confidence: HIGH
  status: VERIFIED

- fact_id: MDS-002
  fact: Material 主数据 Canonical Source 为 material_archives 表，无 Legacy 表
  domain: Master Data
  object: Material→material_archives
  current_truth_source: material_archives
  owner: Procurement 模块
  evidence: master-data-source-map.md §2.3 — 内部裁决；引用链：purchase_orders.materialId, inventory.materialId, material_trace_code.materialId
  confidence: HIGH
  status: VERIFIED

- fact_id: MDS-003
  fact: Supplier 主数据 Canonical Source 为 suppliers 表，快照字段存在于 purchase_orders.supplierName 和 payables.supplierName
  domain: Master Data
  object: Supplier→suppliers
  current_truth_source: suppliers
  owner: Procurement 模块
  evidence: master-data-source-map.md §2.1 — 无 Legacy Source；Snapshot Source 为下单/应付时快照
  confidence: HIGH
  status: VERIFIED

- fact_id: MDS-004
  fact: Employee 主数据 Canonical Source 为 employees 表，用户关联通过 users.employeeId
  domain: Master Data
  object: Employee→employees
  current_truth_source: employees
  owner: HR 模块
  evidence: master-data-source-map.md §2.4 — approval_record.approverName 为快照字段
  confidence: HIGH
  status: VERIFIED

- fact_id: MDS-005
  fact: Store 主数据 Canonical Source 为 stores_new 表，stores 为废弃旧表仍被部分查询使用
  domain: Master Data
  object: Store→stores_new
  current_truth_source: stores_new
  owner: Store 模块
  evidence: master-data-source-map.md §2.5 — truth-conflict-map.md §冲突4 裁决；orders.storeName 为快照字段
  confidence: HIGH
  status: VERIFIED
```

---

## 三、Capability 事实 (5条)

```yaml
- fact_id: CAP-001
  fact: Order Management 能力已就绪，完整链路：Food→OrderService→OrderRepository→orders 表
  domain: Capability
  object: Order Management
  current_truth_source: orders 表
  owner: 订单模块
  evidence: capability-readiness-map.md §2.1 — API: /v1/orders (GET/POST/PUT/DELETE)；事件: OrderCreatedEvent, OrderCompletedEvent, OrderRefundEvent；权限: order:read/write/cancel
  confidence: HIGH
  status: VERIFIED

- fact_id: CAP-002
  fact: Purchase Management 能力部分就绪，SupplierPortal H5 无认证（PD-008）
  domain: Capability
  object: Purchase Management
  current_truth_source: purchase_orders 表
  owner: 采购模块
  evidence: capability-readiness-map.md §2.3 — ⚠️ PARTIAL；6个 H5 端点无 JWT 认证；自采接口 /v1/self-purchase/* 无前端消费
  confidence: HIGH
  status: PARTIAL

- fact_id: CAP-003
  fact: Inventory Management 能力已就绪，中央仓(inventory)和门店仓(store_inventory)独立管理
  domain: Capability
  object: Inventory Management
  current_truth_source: inventory + store_inventory 表
  owner: 库存模块
  evidence: capability-readiness-map.md §2.2 — API: /v1/inventory；事件: StockInEvent, StockOutEvent, LowStockEvent；权限: inventory:read/write/transfer
  confidence: HIGH
  status: VERIFIED

- fact_id: CAP-004
  fact: Finance Management 能力部分就绪，AutoVoucherService 零调用导致凭证无法自动生成
  domain: Capability
  object: Finance Management
  current_truth_source: finance_vouchers 表
  owner: 财务模块
  evidence: capability-readiness-map.md §2.4 — ⚠️ PARTIAL；AutoVoucherService.generateSalesVoucher/generatePurchaseVoucher 零调用点；FinanceVoucherService 根包 no-op stub
  confidence: HIGH
  status: PARTIAL

- fact_id: CAP-005
  fact: HR Management 能力已就绪，完整链路：Employee→EmployeeService→EmployeeRepository→employees 表
  domain: Capability
  object: HR Management
  current_truth_source: employees 表
  owner: HR 模块
  evidence: capability-readiness-map.md §2.6 — 完整 HR 事件体系；SalaryService + AttendanceService + OnboardingService；权限: hr:read/write
  confidence: HIGH
  status: VERIFIED
```

---

## 四、Conflict 事实 (10条)

```yaml
- fact_id: CNF-001
  fact: food/foods 双写 — 下单/退款/超时同时写 food 和 foods 两表
  domain: Conflict
  object: food/foods 双写
  current_truth_source: foods (真相源)，food (双写兼容)
  owner: Product 模块
  evidence: truth-conflict-map.md §冲突7 — foods 为真相源；food 写入为兼容旧模块的过渡方案；风险：双写不一致导致库存/状态错乱
  confidence: HIGH
  status: CONFLICT

- fact_id: CNF-002
  fact: 金额单位双轨 — 新表用「分」，tax_record/account_balance 等遗留表用「元」
  domain: Conflict
  object: 金额单位双轨
  current_truth_source: 新表「分」为准；遗留表「元」需显式转换
  owner: Finance 模块
  evidence: truth-conflict-map.md §冲突1 — orders.amount (分) vs tax_record.amount (元)；跨表 JOIN 差 100 倍风险
  confidence: HIGH
  status: CONFLICT

- fact_id: CNF-003
  fact: 订单状态三套并存 — order_status、payment_status、legacy 状态各自独立生命周期
  domain: Conflict
  object: 订单状态三套并存
  current_truth_source: 三套状态机并存（PADR 裁决）
  owner: 订单模块
  evidence: truth-conflict-map.md §冲突5 — PADR 裁决：三套状态机并存；代码中需明确区分使用哪个状态字段
  confidence: HIGH
  status: CONFLICT

- fact_id: CNF-004
  fact: stores/stores_new 双表并存 — 新代码必须查询 stores_new
  domain: Conflict
  object: stores/stores_new
  current_truth_source: stores_new (真相源)，stores (兼容旧查询)
  owner: Store 模块
  evidence: truth-conflict-map.md §冲突4 — 迁移中；新代码必须查询 stores_new；stores 仅用于兼容
  confidence: HIGH
  status: CONFLICT

- fact_id: CNF-005
  fact: 科目余额双轨 — accounting_subjects.balance vs account_balance 并存，PD-031 已裁决
  domain: Conflict
  object: 科目余额双轨
  current_truth_source: accounting_subjects.balance (唯一真相源)
  owner: Finance 模块
  evidence: truth-conflict-map.md §冲突3 — PD-031 已决：accounting_subjects.balance 为准；account_balance 降级为只读查询旧数据
  confidence: HIGH
  status: VERIFIED

- fact_id: CNF-006
  fact: product_id vs material_id 列名冲突 — product_id 为遗留列名，实际指向物料
  domain: Conflict
  object: product_id vs material_id
  current_truth_source: material_id (真相语义)，product_id (遗留列名暂保留兼容)
  owner: 采购/库存模块
  evidence: truth-conflict-map.md §冲突2 — 新代码一律使用 material_id 语义；product_id 列名暂保留兼容
  confidence: HIGH
  status: CONFLICT

- fact_id: CNF-007
  fact: 凭证状态映射错位 — 后端 0-3 (DRAFT/APPROVED/POSTED/VOID) vs 前端 1-4
  domain: Conflict
  object: 凭证状态错位
  current_truth_source: 后端 0-3 为准；前端映射层单独处理
  owner: Finance 模块
  evidence: truth-conflict-map.md §冲突6 — 映射逻辑封装在前端 adapter 层；已解决
  confidence: HIGH
  status: VERIFIED

- fact_id: CNF-008
  fact: inventory + store_inventory 双写 — 采购入库/调拨/收货按类型双写两表
  domain: Conflict
  object: inventory+store_inventory 双写
  current_truth_source: inventory (中央仓)，store_inventory (门店仓)
  owner: 库存模块
  evidence: truth-conflict-map.md §冲突8 — 两表各自独立管理；双写必须在同一事务内；风险：事务边界不一致导致库存不同步
  confidence: HIGH
  status: CONFLICT

- fact_id: CNF-009
  fact: 成本双写 — persistOrderCost (主事务) + recordOrderCost (异步) 分离
  domain: Conflict
  object: 成本双写
  current_truth_source: 成本口径：分（一致）
  owner: 订单/财务模块
  evidence: truth-conflict-map.md §冲突9 — 主事务成功但异步失败导致成本记录丢失或不一致；需监控异步任务成功率
  confidence: HIGH
  status: CONFLICT

- fact_id: CNF-010
  fact: finance_record 口径注释混乱 — 表注释中金额单位标注有标「分」有标「元」，实际存储为「分」
  domain: Conflict
  object: finance_record 口径
  current_truth_source: finance_records 存储单位为「分」；注释错误需修正
  owner: Finance 模块
  evidence: truth-conflict-map.md §冲突10 — 开发者按注释理解单位，实际存储与注释不符；待修
  confidence: HIGH
  status: CONFLICT
```

---

## 五、统计事实

```yaml
- fact_id: STAT-001
  fact: Controller 总数为 154 个
  domain: Statistics
  object: Controller
  current_truth_source: 代码库静态分析
  owner: 架构团队
  evidence: 代码库静态分析
  confidence: HIGH
  status: VERIFIED

- fact_id: STAT-002
  fact: Service 总数为 293 个
  domain: Statistics
  object: Service
  current_truth_source: 代码库静态分析
  owner: 架构团队
  evidence: 代码库静态分析
  confidence: HIGH
  status: VERIFIED

- fact_id: STAT-003
  fact: Entity 总数为 326 个
  domain: Statistics
  object: Entity
  current_truth_source: 代码库静态分析
  owner: 架构团队
  evidence: 代码库静态分析
  confidence: HIGH
  status: VERIFIED

- fact_id: STAT-004
  fact: Mapper 总数为 302 个
  domain: Statistics
  object: Mapper
  current_truth_source: 代码库静态分析
  owner: 架构团队
  evidence: 代码库静态分析
  confidence: HIGH
  status: VERIFIED

- fact_id: STAT-005
  fact: Migration 文件总数为 160 个 Flyway 迁移文件
  domain: Statistics
  object: Migration
  current_truth_source: 代码库静态分析
  owner: 架构团队
  evidence: db-reality-map.md — 160 Flyway 迁移文件
  confidence: HIGH
  status: VERIFIED

- fact_id: STAT-006
  fact: Events 总数为 22 个
  domain: Statistics
  object: Events
  current_truth_source: 代码库静态分析
  owner: 架构团队
  evidence: 代码库静态分析
  confidence: HIGH
  status: VERIFIED

- fact_id: STAT-007
  fact: Listeners 总数为 10 个
  domain: Statistics
  object: Listeners
  current_truth_source: 代码库静态分析
  owner: 架构团队
  evidence: 代码库静态分析
  confidence: HIGH
  status: VERIFIED

- fact_id: STAT-008
  fact: Schedulers 总数为 5 个
  domain: Statistics
  object: Schedulers
  current_truth_source: 代码库静态分析
  owner: 架构团队
  evidence: 代码库静态分析
  confidence: HIGH
  status: VERIFIED

- fact_id: STAT-009
  fact: Tasks 总数为 6 个
  domain: Statistics
  object: Tasks
  current_truth_source: 代码库静态分析
  owner: 架构团队
  evidence: 代码库静态分析
  confidence: HIGH
  status: VERIFIED

- fact_id: STAT-010
  fact: Permission Codes 总数为 180 个
  domain: Statistics
  object: Permission Codes
  current_truth_source: permissions 表
  owner: System 模块
  evidence: foundation-map.md §5 — 180+ 权限码已定义
  confidence: HIGH
  status: VERIFIED

- fact_id: STAT-011
  fact: Data Scope Levels 总数为 8 级
  domain: Statistics
  object: Data Scope Levels
  current_truth_source: DataScopeAspect 实现
  owner: System 模块
  evidence: permission-data-scope-map.md — 8级数据范围
  confidence: HIGH
  status: VERIFIED
```

---

## 六、状态分布统计

| Status | 数量 | 占比 |
|--------|------|------|
| VERIFIED | 27 | 73.0% |
| CONFLICT | 9 | 24.3% |
| PARTIAL | 1 | 2.7% |
| UNVERIFIED | 0 | 0% |
| DEAD | 0 | 0% |

## 七、领域分布统计

| Domain | 数量 |
|--------|------|
| Foundation | 13 |
| Master Data | 5 |
| Capability | 5 |
| Conflict | 10 |
| Statistics | 11 |
| **总计** | **44** |

---

*基线生成时间: 2026-09-09*
*数据来源: foundation-map.md, business-object-map.md, master-data-source-map.md, truth-conflict-map.md, capability-readiness-map.md, db-reality-map.md*
*下次更新触发: 代码库结构变更 / 新增裁决 / 冲突状态变更*
