# Business Requirements Baseline — PROJECT-MASTER-REVIEW-001

> **版本**: 1.0  
> **生成日期**: 2026-09-09  
> **适用范围**: 食品溯源系统 (Food Traceability System) 业务需求基线  
> **数据来源**: 项目地图文件交叉验证 + 代码库静态分析

---

## 一、文档说明

本文档基于以下项目事实和已确认设计，提取"以后系统必须满足什么"的业务需求基线：

- **项目地图文件**: master-data-source-map.md, business-identity-propagation-map.md, truth-conflict-map.md, foundation-map.md, cross-domain-coordination-matrix.md, permission-data-scope-map.md, event-job-map.md
- **产品决策 (Product Decisions)**: PADR 系列裁决、PD-031/PD-033 等
- **代码库静态分析**: 154 Controllers, 293 Services, 326 Entities, 302 Mappers

---

## 二、业务需求基线

### 2.1 Master Data 需求

```yaml
REQ-MASTER-001:
  title: 供应商业务身份必须来源于 Canonical Supplier Master
  description: |
    所有业务域引用供应商时，必须通过 `suppliers` 表作为唯一真相源 (Truth Source)。
    - 采购订单 (`purchase_orders.supplierId`) 使用 REFERENCE 类型引用
    - 应付账款 (`payables.supplierId`) 使用 REFERENCE 类型引用
    - 物料主供应商 (`material_archives.supplierId`) 使用 REFERENCE 类型引用
    - 追溯码关联 (`material_trace_code.supplierId`) 使用 REFERENCE 类型引用
    
    快照字段 (`purchase_orders.supplierName`, `payables.supplierName`) 仅记录创建时刻的供应商名称，
    不随供应商改名更新，用于历史交易追溯。
  category: MASTER
  priority: P0
  source:
    - master-data-source-map.md §2.1
    - business-identity-propagation-map.md §2.1
    - foundation-map.md §6
  evidence: |
    Truth Source 裁决: suppliers 表为唯一真相源 (master-data-source-map.md)
    引用关系: 7+ 表引用 supplierId (business-identity-propagation-map.md §2.1)
    写入路径: SupplierService → SupplierRepository → suppliers 表 (master-data-source-map.md §5.1)
    已知风险: SNAPSHOT 传播 (supplierName) 和 Re-query Pattern (PaymentDialog) (business-identity-propagation-map.md)
  status: VERIFIED

REQ-MASTER-002:
  title: 商品业务身份必须来源于 Canonical Product/Food Master
  description: |
    所有业务域引用菜品/产品时，必须通过 `foods` 表作为唯一真相源。
    - 订单行项目 (`order_items.foodId`) 使用 REFERENCE 类型引用
    - 门店库存 (`store_inventory.foodId`) 使用 REFERENCE 类型引用
    - 菜品配方 (`dish_recipes.foodId`) 使用 REFERENCE 类型引用
    - 成品溯源码 (`food_trace_code.foodId`) 使用 REFERENCE 类型引用
    
    注意: `food` 旧表仍被双写，为过渡方案，待迁移完成后废弃。
  category: MASTER
  priority: P0
  source:
    - master-data-source-map.md §2.2
    - truth-conflict-map.md §冲突7
    - foundation-map.md §8
  evidence: |
    Truth Source 裁决: PADR §九-A 明确 foods 为唯一真相源 (truth-conflict-map.md §一)
    已知冲突: food/foods 双写 — 下单/退款/超时同时写两表 (truth-conflict-map.md §冲突7)
    状态: ⚠️ 技术债 — foods 为真相源，food 为 legacy 兼容 (truth-conflict-map.md)
  status: VERIFIED (存在双写技术债)

REQ-MASTER-003:
  title: 物料业务身份必须来源于 Canonical Material Master
  description: |
    所有业务域引用物料时，必须通过 `material_archives` 表作为唯一真相源。
    - 采购订单 (`purchase_orders.materialId`) 使用 REFERENCE 类型引用
    - 库存 (`inventory.materialId`) 使用 REFERENCE 类型引用
    - 物料追溯码 (`material_trace_code.materialId`) 使用 REFERENCE 类型引用
    - 物料消耗 (`material_consumption.materialId`) 使用 REFERENCE 类型引用
    
    注意: `product_id` 列名实际指向物料，为遗留语义冲突，新代码应使用 `material_id` 语义。
  category: MASTER
  priority: P0
  source:
    - master-data-source-map.md §2.3
    - business-identity-propagation-map.md §2.3
    - truth-conflict-map.md §冲突2
  evidence: |
    Truth Source 裁决: 内部裁决 material_archives 为唯一真相源 (master-data-source-map.md)
    语义冲突: product_id vs material_id — 列名实际指向物料 (truth-conflict-map.md §冲突2)
    决策: material_id 为真相语义；product_id 列名暂保留兼容 (truth-conflict-map.md)
  status: VERIFIED (存在语义冲突技术债)

REQ-MASTER-004:
  title: 员工业务身份必须来源于 Canonical Employee Master
  description: |
    所有业务域引用员工时，必须通过 `employees` 表作为唯一真相源。
    - 用户关联 (`users.employeeId`) 使用 REFERENCE 类型引用
    - 考勤记录 (`attendance_record.employeeId`) 使用 REFERENCE 类型引用
    - 薪资记录 (`salary_records.employeeId`) 使用 REFERENCE 类型引用
    - 入职档案 (`onboarding_archive.employeeId`) 使用 REFERENCE 类型引用
    - 审批记录 (`approval_record.approverId`) 使用 REFERENCE 类型引用
    
    快照字段 (`approval_record.approverName`) 仅记录创建时刻的审批人姓名。
  category: MASTER
  priority: P0
  source:
    - master-data-source-map.md §2.4
    - business-identity-propagation-map.md §2.6
    - foundation-map.md §3
  evidence: |
    Truth Source 裁决: employees 表为唯一真相源 (master-data-source-map.md)
    生命周期: ONBOARD → ACTIVE → TRANSFER → RESIGN (foundation-map.md §3)
    事件体系: 8 个 HR 领域事件 (event-job-map.md §5)
    状态: ✅ 无冲突，REFERENCE 一致性良好
  status: VERIFIED

REQ-MASTER-005:
  title: 门店业务身份必须来源于 Canonical Store Master
  description: |
    所有业务域引用门店时，必须通过 `stores_new` 表作为唯一真相源。
    - 订单归属 (`orders.storeId`) 使用 REFERENCE 类型引用
    - 门店库存 (`store_inventory.storeId`) 使用 REFERENCE 类型引用
    - 用户归属 (`users.storeId`) 使用 REFERENCE 类型引用
    - 员工归属 (`employees.storeId`) 使用 REFERENCE 类型引用
    - 调拨关联 (`inventory_transfer.fromStoreId/toStoreId`) 使用 REFERENCE 类型引用
    
    注意: `stores` 旧表仍被部分查询使用，为遗留兼容。
  category: MASTER
  priority: P0
  source:
    - master-data-source-map.md §2.5
    - business-identity-propagation-map.md §2.4
    - truth-conflict-map.md §冲突4
    - foundation-map.md §2
  evidence: |
    Truth Source 裁决: truth-conflict-map.md 冲突4 已裁决 stores_new 为唯一真相源
    已知冲突: stores/stores_new 双表并存 (truth-conflict-map.md §冲突4)
    状态: ⚠️ 迁移中 — 新代码必须查询 stores_new (truth-conflict-map.md)
  status: VERIFIED (存在双表并存技术债)
```

---

### 2.2 Data 需求

```yaml
REQ-DATA-001:
  title: 跨域不得重新生成 Canonical Business Identity
  description: |
    下游系统在消费上游业务身份时，不得重新生成或创建新的身份标识。
    - 必须通过 REFERENCE 类型引用上游 Canonical Source 的 ID
    - 不得复制上游身份到下游表创建新的身份标识
    - Snapshot 字段仅记录创建时刻的属性值，不作为身份引用
    
    违反此原则的案例:
    - `purchase_orders.product_id` 实际指向物料而非产品 (WRONG IDENTITY)
    - `warehouseId` 无独立 Canonical Source (IDENTITY LOSS)
  category: DATA
  priority: P0
  source:
    - business-identity-propagation-map.md §三
    - truth-conflict-map.md §冲突2
  evidence: |
    Identity 问题分类: Identity Loss, Wrong Identity, Legacy Identity (business-identity-propagation-map.md §三)
    冲突案例: product_id 语义错误，实际指向物料 (truth-conflict-map.md §冲突2)
    仓库案例: warehouseId 无独立 Canonical Source (business-identity-propagation-map.md §2.5)
  status: VERIFIED

REQ-DATA-002:
  title: Dialog/Form 必须继承正确的 Canonical Identity
  description: |
    前端 Dialog/Form 在创建或编辑业务对象时，必须继承正确的 Canonical Identity:
    - 关联字段必须引用 Canonical Source 的 ID
    - 不得使用 Legacy 或 Duplicate 的身份标识
    - Hidden Inheritance 模式必须有明确的继承来源说明
    
    已确认的继承模式:
    - 采购订单继承 supplierId (正确)
    - 采购订单继承 materialId (正确)
    - 收货确认继承 storeId/warehouseId (Hidden Inheritance)
  category: DATA
  priority: P1
  source:
    - business-identity-propagation-map.md §3.4
    - dialog-form-data-contract-map.md
  evidence: |
    隐式继承: 收货确认中 storeId/warehouseId 从 arrival 继承但不在表单中 (business-identity-propagation-map.md §3.4)
    已通过代码处理，属于 Hidden Inheritance 模式 (business-identity-propagation-map.md)
  status: VERIFIED

REQ-DATA-003:
  title: 历史交易 Snapshot 必须有明确的数据语义
  description: |
    所有 Snapshot 字段必须满足以下要求:
    - 明确标记为 SNAPSHOT 类型
    - 记录创建时刻的属性值 (如供应商名称、门店名称)
    - 不随源数据更新而更新
    - 用于历史交易追溯和审计
    
    已确认的 Snapshot 字段:
    - `purchase_orders.supplierName`: 供应商名称快照
    - `payables.supplierName`: 应付单供应商名称快照
    - `order_items.foodName`: 订单行菜品名称快照
    - `orders.storeName`: 订单门店名称快照
    - `approval_record.approverName`: 审批人姓名快照
  category: DATA
  priority: P1
  source:
    - master-data-source-map.md §3
    - business-identity-propagation-map.md §2.1, §2.4, §2.6
  evidence: |
    Snapshot 类型定义: 创建时快照，不随源数据更新 (master-data-source-map.md §4)
    传播路径: 明确标记为 SNAPSHOT 类型 (business-identity-propagation-map.md §2.1)
  status: VERIFIED

REQ-DATA-004:
  title: 金额单位必须统一（分或元）
  description: |
    系统内所有金额字段必须统一单位:
    - 新表统一用「分」存储金额
    - 遗留表 (`tax_record`, `account_balance`) 仍用「元」
    - 跨表 JOIN/汇总时必须显式转换单位
    
    已知冲突:
    - `orders.amount` vs `tax_record.amount`: 分 vs 元，差 100 倍
    - `finance_records.amount` 注释混乱: 有标「分」有标「元」
    
    决策: 新表以「分」为准；读遗留表时需显式转换
  category: DATA
  priority: P0
  source:
    - truth-conflict-map.md §冲突1
    - master-data-source-map.md §6.2
  evidence: |
    冲突定义: 新表用「分」，遗留表用「元」 (truth-conflict-map.md §冲突1)
    风险: 跨表 JOIN 金额差 100 倍 (truth-conflict-map.md §冲突1)
    决策: 新表以「分」为准 (truth-conflict-map.md §冲突1)
    状态: ⚠️ 持续关注 — 需在所有跨表查询处添加单位转换注释
  status: VERIFIED (存在双轨技术债)

REQ-DATA-005:
  title: 状态机必须统一（不能三套并存）
  description: |
    订单状态机存在三套并存:
    - `order_status`: 订单状态
    - `payment_status`: 支付状态
    - legacy 状态: 遗留状态字段
    
    已确认决策: 三套状态机并存，每个字段有独立生命周期
    
    凭证状态机存在映射冲突:
    - 后端存储 0-3 (枚举)
    - 前端展示 1-4 (序号)
    - 映射关系固定但不直观
    
    已确认决策: 后端以 0-3 为准；前端映射层单独处理
  category: DATA
  priority: P0
  source:
    - truth-conflict-map.md §冲突5, §冲突6
    - business-identity-propagation-map.md §2.7
  evidence: |
    订单状态机: 三套并存 (PADR 裁决) (truth-conflict-map.md §冲突5)
    凭证状态机: 后端 0-3 vs 前端 1-4 (truth-conflict-map.md §冲突6)
    状态: ✅ 已裁决 — 代码中需明确区分使用哪个状态字段
  status: VERIFIED (已裁决，需代码遵循)
```

---

### 2.3 Core 需求

```yaml
REQ-CORE-001:
  title: 客户端是业务入口，不等于 Truth Owner
  description: |
    所有终端 (POS, Kitchen, Employee, MiniProgram, Receiving Mobile) 作为业务入口，
    但不拥有数据真相权:
    - 客户端可写入数据，但不是 Truth Owner
    - Truth Owner 由跨域协调矩阵明确定义
    - 客户端写入必须通过 Service 层
    
    数据所有权矩阵:
    - Management Core: Food, Material, Employee, Supplier, Store, Member
    - POS: Order, StoreInventory
    - Finance: Payable, Receivable, Voucher, BankAccount, AccountingSubject
    - Inventory 模块: Inventory
  category: CORE
  priority: P0
  source:
    - cross-domain-coordination-matrix.md §5.1
    - foundation-map.md §2, §3, §6, §8
  evidence: |
    数据所有权矩阵: 明确每个业务对象的唯一 OWNER (cross-domain-coordination-matrix.md §5.1)
    角色定义: OWNER 拥有完整 CRUD 权限，其他角色为 READ/WRITE/EVENT CONSUMER (cross-domain-coordination-matrix.md §1.2)
  status: VERIFIED

REQ-CORE-002:
  title: Truth Source 必须唯一
  description: |
    每个业务域必须有且只有一个 Truth Source:
    - 菜品: `foods` 表
    - 物料: `material_archives` 表
    - 供应商: `suppliers` 表
    - 员工: `employees` 表
    - 门店: `stores_new` 表
    - 库存: `inventory` 表 (中央仓)
    - 门店库存: `store_inventory` 表
    - 订单: `orders` 表
    - 凭证: `finance_vouchers` 表
    - 应付: `payables` 表
    - 应收: `receivables` 表
    - 科目: `accounting_subjects` 表
    - 财务流水: `finance_records` 表
    - 资金流水: `fund_flows` 表
    
    Legacy Source 仅用于只读兼容，不得作为新业务的写入目标。
  category: CORE
  priority: P0
  source:
    - truth-conflict-map.md §一
    - master-data-source-map.md §一
    - foundation-map.md §一
  evidence: |
    真相源裁决表: 14 个业务域的 Truth Source 已明确裁决 (truth-conflict-map.md §一)
    主数据源总览: 15 个主数据的 Canonical Source 已验证 (master-data-source-map.md §一)
  status: VERIFIED

REQ-CORE-003:
  title: 写入操作必须通过 Service 层
  description: |
    所有数据写入操作必须通过 Service 层:
    - Controller 不得直接操作 Repository/Mapper
    - Service 层负责业务逻辑校验和事务管理
    - 事件发布必须在 Service 层完成
    
    写入路径模式:
    ```
    Controller → Service → Repository/Mapper → Database
                                    ↓
                              Domain Event
    ```
  category: CORE
  priority: P0
  source:
    - master-data-source-map.md §5 (写入路径)
    - project-master-map.md §一
  evidence: |
    写入路径: 所有主数据写入均经过 Service 层 (master-data-source-map.md §5)
    代码规模: 293 Services 对应 302 Mappers，Service 层覆盖完整 (project-master-map.md §二)
  status: VERIFIED
```

---

### 2.4 Security 需求

```yaml
REQ-SEC-001:
  title: 所有 API 必须有权限保护
  description: |
    所有 API 端点必须有权限保护:
    - 8 层安全过滤器链 (DeviceWhitelist → RateLimit → JWT → MFA → Spring Security → Permission → DataScope → DataPermission)
    - 180+ 权限码覆盖所有业务域
    - @RequiresPermission / @RequiresPermissions / @RequiresRole / @RequiresDataScope 注解强制校验
    - DataPermissionAspect 自动向 QueryWrapper 添加数据过滤
    
    公开端点白名单 (SecurityConfig):
    - `/v1/auth/**`: 登录/注册
    - `/v1/public/**`: 公开 API
    - `/v1/foods/**`: 菜品查询 (GET)
    - `/v1/suppliers/list`: 供应商列表
    - `/v1/kitchen/**`: 厨房查询 (GET)
    - `/v1/orders/**`: 订单查询 (GET)
    - `/v1/mp/**`: 小程序端
    
    权限缺口 (BLOCKED_PRODUCT_DECISION):
    - PD-006: 25 裸接口方法无权限注解
    - PD-008: 6 供应商 H5 端点无认证
  category: SECURITY
  priority: P0
  source:
    - permission-data-scope-map.md §六, §七
    - project-master-map.md §四
  evidence: |
    安全链: 8 层安全过滤器链 (permission-data-scope-map.md §六)
    权限码: 180+ 权限码已定义 (permission-data-scope-map.md §十)
    数据范围: 8 级数据范围 (ALL → NONE) (permission-data-scope-map.md §4.1)
    权限缺口: PD-006/PD-008 待产品决策 (permission-data-scope-map.md §七)
  status: VERIFIED (存在权限缺口)

REQ-SEC-002:
  title: 敏感操作必须有审计日志
  description: |
    所有敏感操作必须有审计日志:
    - @AuditLog 注解标记敏感操作
    - 操作人、操作时间、操作内容、操作结果必须记录
    - 审计日志不可删除、不可修改
    
    已确认的审计缺失:
    - 发布操作无 @AuditLog 审计
    - 退款审批身份归因不明确 (PD-010)
  category: SECURITY
  priority: P1
  source:
    - permission-data-scope-map.md §7.1
  evidence: |
    审计缺失: 发布操作无 @AuditLog (permission-data-scope-map.md §7.1)
    身份归因: 退款审批身份归因不明确 (PD-010) (permission-data-scope-map.md §7.1)
  status: VERIFIED (存在审计缺口)
```

---

### 2.5 Cross-Domain 需求

```yaml
REQ-CROSS-001:
  title: 跨域数据流必须明确所有权
  description: |
    跨域数据流必须明确所有权:
    - 每个业务对象必须有唯一 OWNER (Truth Owner)
    - 下游系统只能 READ 或通过事件 EVENT CONSUMER
    - WRITE 下游必须通过 Service 层而非直接写入
    
    已定义的数据所有权:
    - Food: Management Core (OWNER) → POS/Kitchen/Employee/MiniProgram/Inventory/Finance (READ)
    - Order: POS/MiniProgram (OWNER) → Management Core/Kitchen/Employee/Inventory/Finance (READ/EVENT CONSUMER)
    - Inventory: Inventory 模块 (OWNER) → Management Core/POS/MiniProgram/Finance (READ)
    - Payable: Finance (OWNER) → Management Core (READ) / Receiving Mobile (EVENT CONSUMER)
    
    反向依赖风险:
    - Inventory → Management: 库存盘点反推理论库存 (高风险)
    - Finance → Management: 资金流水反推业务发生额 (高风险)
  category: CROSS_DOMAIN
  priority: P0
  source:
    - cross-domain-coordination-matrix.md §2.1, §3, §5
  evidence: |
    数据所有权矩阵: 15 个业务对象的 OWNER 已明确 (cross-domain-coordination-matrix.md §5.1)
    反向依赖: 7 个反向依赖链路已分析 (cross-domain-coordination-matrix.md §3.1)
    风险等级: 5 个高风险协调点 (cross-domain-coordination-matrix.md §6.1)
  status: VERIFIED

REQ-CROSS-002:
  title: 事件驱动必须保证最终一致性
  description: |
    事件驱动架构必须保证最终一致性:
    - 22 个领域事件定义完整
    - 10 个事件监听器覆盖所有事件
    - 事件发布必须在同一事务内完成
    - 事件消费失败必须有补偿机制
    
    已确认的事件流转:
    - OrderCreatedEvent → OrderCreatedEventListener → 锁定原料
    - OrderCompletedEvent → OrderCompletedEventListener → 记录财务收入
    - OrderRefundEvent → OrderRefundEventListener → 生成退款支出
    - PurchaseStockInEvent → PurchaseStockInEventListener → 生成追溯码
    - ReceiptConfirmationCompletedEvent → ReceiptConfirmationEventListener → 生成追溯码
    - SalaryPaidEvent → SalaryPaidEventListener → 生成凭证
    - StoreDailySettlementCompletedEvent → StoreDailySettlementEventListener → 生成凭证
    - InvoiceReimbursementApprovedEvent → InvoiceReimbursementApprovedEventListener → 生成凭证
    
    已知风险:
    - inventory + store_inventory 双写事务边界不一致 (truth-conflict-map.md §冲突8)
    - 成本双写: persistOrderCost (主事务) + recordOrderCost (异步) (truth-conflict-map.md §冲突9)
    - 事件丢失风险 (cross-domain-coordination-matrix.md §7.1)
  category: CROSS_DOMAIN
  priority: P0
  source:
    - event-job-map.md
    - truth-conflict-map.md §冲突8, §冲突9
    - cross-domain-coordination-matrix.md §7.1
  evidence: |
    事件定义: 22 个领域事件 (event-job-map.md §一)
    事件监听器: 10 个监听器 (event-job-map.md §二)
    事件流转: 8 条事件流转链路 (event-job-map.md §六)
    双写风险: inventory/store_inventory 双写事务边界 (truth-conflict-map.md §冲突8)
    异步风险: 成本双写异步失败需补偿 (truth-conflict-map.md §冲突9)
  status: VERIFIED (存在事务一致性风险)
```

---

## 三、需求覆盖度矩阵

### 3.1 按类别统计

| 类别 | 需求数 | P0 | P1 | 状态 |
|------|--------|-----|-----|------|
| MASTER | 5 | 5 | 0 | VERIFIED (存在技术债) |
| DATA | 5 | 3 | 2 | VERIFIED (存在双轨/双表) |
| CORE | 3 | 3 | 0 | VERIFIED |
| SECURITY | 2 | 1 | 1 | VERIFIED (存在缺口) |
| CROSS_DOMAIN | 2 | 2 | 0 | VERIFIED (存在风险) |
| **合计** | **17** | **14** | **3** | |

### 3.2 按优先级统计

| 优先级 | 需求数 | 需求列表 |
|--------|--------|----------|
| P0 | 14 | REQ-MASTER-001~005, REQ-DATA-001/004/005, REQ-CORE-001~003, REQ-SEC-001, REQ-CROSS-001/002 |
| P1 | 3 | REQ-DATA-002/003, REQ-SEC-002 |

### 3.3 按验证状态统计

| 状态 | 需求数 | 说明 |
|------|--------|------|
| VERIFIED | 17 | 所有需求已通过代码库静态分析验证 |
| 存在技术债 | 5 | REQ-MASTER-002/003/005, REQ-DATA-004/005 |
| 存在缺口 | 2 | REQ-SEC-001/002 |
| 存在风险 | 2 | REQ-CROSS-001/002 |

---

## 四、关联产品决策项

### 4.1 已决策 (Resolved)

| 编号 | 决策项 | 决策结论 | 来源 |
|------|--------|----------|------|
| PD-031 | 科目余额双轨 | `accounting_subjects.balance` 为唯一真相源 | truth-conflict-map.md §冲突3 |
| PADR §九-A | 菜品/订单真相源 | `foods` 和 `orders` 为唯一真相源 | truth-conflict-map.md §一 |
| PADR 裁决 | 订单状态机 | 三套状态机并存，每个字段有独立生命周期 | truth-conflict-map.md §冲突5 |

### 4.2 待决策 (Pending)

| 编号 | 决策项 | 状态 | 影响需求 |
|------|--------|------|----------|
| PD-033 | 反向链空白 | ❌ 待决 | REQ-CROSS-001 |
| PD-006 | 裸接口权限 | ❌ 待决 | REQ-SEC-001 |
| PD-008 | 供应商 H5 认证 | ❌ 待决 | REQ-SEC-001 |
| PD-010 | 退款审批身份归因 | ❌ 待决 | REQ-SEC-002 |

---

## 五、技术债务清单

| 编号 | 技术债务 | 涉及需求 | 风险等级 | 缓解措施 |
|------|----------|----------|----------|----------|
| TD-001 | food/foods 双写 | REQ-MASTER-002 | 🔴 P0 | 同一事务，待旧模块迁移后废弃 food 表 |
| TD-002 | stores/stores_new 双表 | REQ-MASTER-005 | 🔴 P0 | 完成迁移废弃旧表 |
| TD-003 | product_id vs material_id 语义冲突 | REQ-MASTER-003 | 🔴 P0 | 新代码一律使用 material_id 语义 |
| TD-004 | 金额单位双轨 (分/元) | REQ-DATA-004 | 🔴 P0 | 统一为「分」，跨表查询添加转换 |
| TD-005 | 订单三套状态机并存 | REQ-DATA-005 | 🟡 P1 | 已裁决并存，代码明确区分使用 |
| TD-006 | 25 裸接口无权限注解 | REQ-SEC-001 | 🔴 P0 | PD-006 待决后补充 |
| TD-007 | 6 供应商 H5 端点无认证 | REQ-SEC-001 | 🔴 P0 | PD-008 待决后补充 |
| TD-008 | 发布操作无审计日志 | REQ-SEC-002 | 🟡 P1 | 补充 @AuditLog 注解 |
| TD-009 | inventory/store_inventory 双写事务 | REQ-CROSS-002 | 🟡 P1 | 确保同一事务 |
| TD-010 | 成本异步写入补偿 | REQ-CROSS-002 | 🟡 P1 | 监控异步任务成功率 |

---

## 六、维护说明

1. 本文件随 PADR 裁决更新而更新
2. 新增需求需同时更新「需求覆盖度矩阵」
3. 需求状态变更（待决→已决）需更新「关联产品决策项」
4. 技术债务变更需更新「技术债务清单」

---

*生成时间: 2026-09-09*  
*数据来源: 项目地图文件交叉验证 + 代码库静态分析*  
*维护者: opencode*
