# 食品溯源系统 - 数据血缘图

> 版本: 1.0 | 生成日期: 2026-09-09 | 基于: Project Master Map 静态分析

---

## 一、数据血缘总览

### 1.1 数据血缘定义

数据血缘（Data Lineage）描述数据从创建到消费的完整流转路径，包括：
- **数据起源**：数据的创建者和创建位置
- **数据流转**：数据经过的处理和转换
- **数据消费**：数据的最终使用者和用途
- **数据所有权**：数据的所有者和权限控制
- **数据快照**：数据在特定时间点的状态

### 1.2 核心业务链路

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        核心业务数据血缘链路                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  采购链路:                                                                   │
│  Material → PurchaseRequest → PurchaseOrder → PurchaseArrival              │
│    → ReceiptConfirmation → Inventory → Payable → Payment → FundFlow        │
│                                                                             │
│  销售链路:                                                                   │
│  Food → Order → FinanceRecord → FundFlow                                   │
│    → Receivable → Receipt → FundFlow                                        │
│                                                                             │
│  追溯链路:                                                                   │
│  Material → MaterialTraceCode → TraceabilityService                        │
│    → Food → FoodTraceCode → TraceScanRecord                                │
│                                                                             │
│  HR链路:                                                                     │
│  Employee → Attendance → Salary → FinanceRecord → FundFlow                 │
│    → InvoiceReimbursement → Payment → FundFlow                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、核心业务对象血缘追踪

### 2.1 Food (菜品) 血缘

#### 创建血缘

```
创建者: Management Core (FoodService.create)
  │
  ├── 写入: foods表 (OWNER)
  │
  ├── 写入: food表 (兼容旧模块, 冲突7)
  │
  └── 发布事件: FoodCreatedEvent
        │
        └── 消费者: MenuService, CacheService
```

#### 流转血缘

```
Management Core
  │
  ├──→ POS终端: 读取 foods表 (READ)
  │     │
  │     └──→ 点餐时读取菜品信息
  │
  ├──→ Kitchen终端: 读取 foods表 (READ)
  │     │
  │     └──→ 显示菜品名称
  │
  ├──→ Employee终端: 读取 foods表 (READ)
  │     │
  │     └──→ 员工端查看菜品
  │
  ├──→ MiniProgram: 读取 API: /v1/foods (READ)
  │     │
  │     └──→ 菜单浏览
  │
  ├──→ Inventory模块: 读取 foods表 (READ)
  │     │
  │     └──→ 库存关联
  │
  └──→ Finance模块: 读取 foods表 (READ)
        │
        └──→ 成本计算
```

#### 冲突点

| 冲突 | 描述 | 风险 | 缓解措施 |
|------|------|------|----------|
| food/foods双写 | 下单同时写两表 | 🔴 高 | 同一事务 |

---

### 2.2 Material (物料) 血缘

#### 创建血缘

```
创建者: Management Core (MaterialService.create)
  │
  ├── 写入: material_archives表 (OWNER)
  │
  └── 发布事件: MaterialCreatedEvent
        │
        └── 消费者: CacheService
```

#### 流转血缘

```
Management Core
  │
  ├──→ Employee终端: 读取 material_archives表 (READ)
  │     │
  │     └──→ 员工端查看物料
  │
  ├──→ Receiving Mobile: 读取 API: /v1/materials (READ)
  │     │
  │     └──→ 收货时查看物料信息
  │
  ├──→ Inventory模块: 读取 material_archives表 (READ)
  │     │
  │     └──→ 库存管理
  │
  └──→ Finance模块: 读取 material_archives表 (READ)
        │
        └──→ 采购成本计算
```

---

### 2.3 Order (订单) 血缘

#### 创建血缘

```
创建者: POS终端 (PosOrderCreateService.create) 或 MiniProgram (OrderService.create)
  │
  ├── 写入: orders表 (OWNER)
  │
  ├── 写入: order_items表 (明细)
  │
  ├── 写入: food表 (兼容旧模块, 冲突7)
  │
  └── 发布事件: OrderCreatedEvent
        │
        ├── 消费者: OrderCreatedEventListener (锁定原料)
        ├── 消费者: KitchenDisplay (厨房显示)
        └── 消费者: NotificationService (通知)
```

#### 流转血缘

```
POS终端 / MiniProgram
  │
  ├──→ Management Core: 读取 orders表 (READ)
  │     │
  │     └──→ 订单查询、统计
  │
  ├──→ Kitchen终端: 读取 API/Event (READ)
  │     │
  │     └──→ 显示订单详情
  │
  ├──→ Employee终端: 读取 orders表 (READ)
  │     │
  │     └──→ 订单查询
  │
  ├──→ Inventory模块: 事件消费 OrderCreatedEvent (EVENT CONSUMER)
  │     │
  │     └──→ 锁定原料库存
  │
  └──→ Finance模块: 事件消费 OrderCompletedEvent (EVENT CONSUMER)
        │
        └──→ 记录财务收入
```

#### 冲突点

| 冲突 | 描述 | 风险 | 缓解措施 |
|------|------|------|----------|
| 订单状态三套并存 | order_status/payment_status/legacy | 🟡 P1 | 维护映射表 |

---

### 2.4 Inventory (库存) 血缘

#### 创建血缘

```
创建者: Inventory模块 (InventoryService.stockIn)
  │
  ├── 写入: inventory表 (OWNER)
  │
  └── 发布事件: StockInEvent
        │
        └── 消费者: InventoryWarningService
```

#### 流转血缘

```
Inventory模块
  │
  ├──→ Management Core: 读取 inventory表 (READ)
  │     │
  │     └──→ 库存查询、报表
  │
  ├──→ POS终端: 读取 foods.stock (READ)
  │     │
  │     └──→ 可售判断
  │
  ├──→ MiniProgram: 读取 API (READ)
  │     │
  │     └──→ 库存显示
  │
  ├──→ Receiving Mobile: 写入 API: /v1/inventory/in (WRITE)
  │     │
  │     └──→ 收货入库
  │
  └──→ Finance模块: 读取 inventory表 (READ)
        │
        └──→ 库存报表
```

#### 冲突点

| 冲突 | 描述 | 风险 | 缓解措施 |
|------|------|------|----------|
| inventory/store_inventory双写 | 采购入库/调拨双写 | 🟡 P1 | 同一事务 |

---

### 2.5 Payable (应付) 血缘

#### 创建血缘

```
创建者: Finance模块 (PayableService.create)
  │
  ├── 写入: payables表 (OWNER)
  │
  └── 发布事件: PayableCreatedEvent
```

#### 流转血缘

```
Receiving Mobile
  │
  └──→ 事件消费 ReceiptConfirmationCompletedEvent (EVENT CONSUMER)
        │
        └──→ 生成应付账款
              │
              └──→ Finance模块
                    │
                    ├──→ Management Core: 读取 payables表 (READ)
                    │     │
                    │     └──→ 应付查询
                    │
                    └──→ PaymentService: 写入 payment表 (WRITE)
                          │
                          └──→ 付款确认
```

---

### 2.6 Receivable (应收) 血缘

#### 创建血缘

```
创建者: Finance模块 (ReceivableService.create)
  │
  ├── 写入: receivables表 (OWNER)
  │
  └── 发布事件: ReceivableCreatedEvent
```

#### 流转血缘

```
POS终端 / MiniProgram
  │
  └──→ 事件消费 OrderCompletedEvent (EVENT CONSUMER)
        │
        └──→ 生成应收账款
              │
              └──→ Finance模块
                    │
                    ├──→ Management Core: 读取 receivables表 (READ)
                    │     │
                    │     └──→ 应收查询
                    │
                    └──→ ReceiptService: 写入 receipt表 (WRITE)
                          │
                          └──→ 收款确认
```

---

### 2.7 Voucher (凭证) 血缘

#### 创建血缘

```
创建者: Finance模块 (VoucherService.create)
  │
  ├── 写入: finance_vouchers表 (OWNER)
  │
  └── 发布事件: VoucherCreatedEvent
```

#### 流转血缘

```
Finance模块
  │
  ├──→ Management Core: 读取 finance_vouchers表 (READ)
  │     │
  │     └──→ 凭证查询、报表
  │
  ├──→ 事件消费: SalaryPaidEvent (EVENT CONSUMER)
  │     │
  │     └──→ 生成薪资凭证
  │
  ├──→ 事件消费: StoreDailySettlementCompletedEvent (EVENT CONSUMER)
  │     │
  │     └──→ 生成日结凭证
  │
  └──→ 事件消费: InvoiceReimbursementApprovedEvent (EVENT CONSUMER)
        │
        └──→ 生成报销凭证
```

#### 冲突点

| 冲突 | 描述 | 风险 | 缓解措施 |
|------|------|------|----------|
| 凭证状态映射 | 后端0-3 vs 前端1-4 | 🟡 P1 | adapter层封装 |

---

### 2.8 Employee (员工) 血缘

#### 创建血缘

```
创建者: Management Core (EmployeeService.create)
  │
  ├── 写入: employees表 (OWNER)
  │
  └── 发布事件: EmployeeCreatedEvent
        │
        └── 消费者: HREventListener (更新员工计数)
```

#### 流转血缘

```
Management Core
  │
  ├──→ Employee终端: 读取 employees表 (READ)
  │     │
  │     └──→ 个人中心
  │
  ├──→ Receiving Mobile: 读取 employees表 (READ)
  │     │
  │     └──→ 收货员信息
  │
  └──→ Finance模块: 读取 employees表 (READ)
        │
        └──→ 报销关联
```

---

## 三、数据所有权详细分析

### 3.1 数据所有权矩阵

| 业务对象 | OWNER | 创建API | 写入权限 | 读取权限 |
|----------|-------|---------|----------|----------|
| Food | Management Core | FoodService.create | Management Core | 所有终端 |
| Material | Management Core | MaterialService.create | Management Core | Management, Employee, Receiving, Inventory, Finance |
| Order | POS, MiniProgram | PosOrderCreateService.create, OrderService.create | POS, MiniProgram | Management, Kitchen, Employee, Inventory, Finance |
| Inventory | Inventory模块 | InventoryService.stockIn | Inventory, Receiving Mobile | Management, POS, MiniProgram, Finance |
| StoreInventory | POS | StoreInventoryService.stockIn | POS | Management, MiniProgram |
| Payable | Finance模块 | PayableService.create | Finance | Management, Receiving Mobile |
| Receivable | Finance模块 | ReceivableService.create | Finance | Management, POS |
| Voucher | Finance模块 | VoucherService.create | Finance | Management |
| Employee | Management Core | EmployeeService.create | Management Core | Employee, Receiving, Finance |
| Supplier | Management Core | SupplierService.create | Management Core | Receiving, Inventory, Finance |
| Store | Management Core | StoreNewService.create | Management Core | 所有终端 |
| Member | Management Core | MemberService.create | Management Core, MiniProgram | POS, MiniProgram |
| BankAccount | Finance模块 | FinanceAccountService.create | Finance | Management |
| AccountingSubject | Finance模块 | AccountingSubjectService.create | Finance | Management |

### 3.2 数据读写权限详细分析

#### READ 权限分析

| 业务对象 | 读取终端 | 读取方式 | 读取频率 | 数据一致性要求 |
|----------|----------|----------|----------|----------------|
| Food | POS | DB直读 | 高频 | 强一致 |
| Food | Kitchen | DB直读 | 中频 | 强一致 |
| Food | Employee | DB直读 | 低频 | 最终一致 |
| Food | MiniProgram | API | 高频 | 最终一致 |
| Order | Management | DB直读 | 中频 | 强一致 |
| Order | Kitchen | API/Event | 高频 | 强一致 |
| Order | Employee | DB直读 | 低频 | 最终一致 |
| Inventory | Management | DB直读 | 中频 | 强一致 |
| Inventory | POS | DB直读 | 高频 | 强一致 |
| Inventory | MiniProgram | API | 中频 | 最终一致 |
| Payable | Management | DB直读 | 低频 | 最终一致 |
| Payable | Receiving Mobile | API | 低频 | 最终一致 |
| Receivable | Management | DB直读 | 低频 | 最终一致 |
| Receivable | POS | API | 中频 | 最终一致 |
| Voucher | Management | DB直读 | 低频 | 最终一致 |
| Employee | Employee | DB直读 | 低频 | 最终一致 |
| Employee | Receiving Mobile | DB直读 | 低频 | 最终一致 |
| Employee | Finance | DB直读 | 低频 | 最终一致 |

#### WRITE 权限分析

| 业务对象 | 写入终端 | 写入方式 | 写入频率 | 事务要求 |
|----------|----------|----------|----------|----------|
| Food | Management Core | Service.create/update | 低频 | 本地事务 |
| Material | Management Core | Service.create/update | 低频 | 本地事务 |
| Order | POS | Service.create | 高频 | 本地事务 |
| Order | MiniProgram | Service.create | 中频 | 本地事务 |
| Inventory | Inventory模块 | Service.stockIn/stockOut | 中频 | 本地事务 |
| Inventory | Receiving Mobile | Service.stockIn | 中频 | 分布式事务 |
| StoreInventory | POS | Service.stockIn/stockOut | 高频 | 本地事务 |
| Payable | Finance模块 | Service.create | 低频 | 本地事务 |
| Receivable | Finance模块 | Service.create | 低频 | 本地事务 |
| Voucher | Finance模块 | Service.create | 低频 | 本地事务 |
| Employee | Management Core | Service.create/update | 低频 | 本地事务 |
| Supplier | Management Core | Service.create/update | 低频 | 本地事务 |
| Store | Management Core | Service.create/update | 低频 | 本地事务 |
| Member | Management Core | Service.create/update | 低频 | 本地事务 |
| Member | MiniProgram | Service.create/update | 中频 | 本地事务 |
| BankAccount | Finance模块 | Service.create | 低频 | 本地事务 |
| AccountingSubject | Finance模块 | Service.create | 低频 | 本地事务 |

---

## 四、数据快照点分析

### 4.1 快照点定义

数据快照（Snapshot）是指在特定时间点对数据状态的记录，用于：
- 审计追踪
- 数据恢复
- 报表生成
- 冲突解决

### 4.2 核心业务对象快照点

| 业务对象 | 快照点 | 快照内容 | 快照时机 | 证据 |
|----------|--------|----------|----------|------|
| Order | 订单创建 | 完整订单数据 | OrderCreatedEvent | event-job-map.md §1 |
| Order | 订单完成 | 完整订单数据 | OrderCompletedEvent | event-job-map.md §1 |
| Order | 订单退款 | 完整订单数据 | OrderRefundEvent | event-job-map.md §1 |
| Inventory | 库存变更 | 变更前后库存 | StockInEvent/StockOutEvent | business-object-map.md §3 |
| Payable | 应付生成 | 完整应付数据 | PayableCreatedEvent | business-object-map.md §7 |
| Payable | 应付核销 | 核销前后状态 | PayableSettledEvent | business-object-map.md §7 |
| Receivable | 应收生成 | 完整应收数据 | ReceivableCreatedEvent | business-object-map.md §8 |
| Receivable | 应收核销 | 核销前后状态 | ReceivableCollectedEvent | business-object-map.md §8 |
| Voucher | 凭证过账 | 完整凭证数据 | VoucherPostedEvent | event-job-map.md §7 |
| Employee | 员工创建 | 完整员工数据 | EmployeeCreatedEvent | event-job-map.md §5 |
| Employee | 员工离职 | 离职信息 | EmployeeResignedEvent | event-job-map.md §5 |

### 4.3 快照存储位置

| 快照类型 | 存储位置 | 保留策略 | 查询方式 |
|----------|----------|----------|----------|
| 订单快照 | orders表历史字段 | 永久 | 按时间范围查询 |
| 库存快照 | inventory_log表 | 按策略清理 | 按物料/时间查询 |
| 凭证快照 | finance_vouchers表 | 永久 | 按期间/类型查询 |
| 员工快照 | employees表历史字段 | 永久 | 按员工/时间查询 |

---

## 五、数据冲突与不一致分析

### 5.1 已识别冲突点

| 冲突ID | 冲突描述 | 涉及对象 | 风险等级 | 当前状态 | 缓解措施 |
|--------|----------|----------|----------|----------|----------|
| C-001 | 金额单位双轨 (分 vs 元) | 所有财务对象 | 🔴 高 | ⚠️ 持续关注 | 统一为「分」 |
| C-002 | product_id vs material_id | Material相关表 | 🟡 中 | ⚠️ 技术债 | 使用material_id语义 |
| C-003 | 科目余额双轨 | AccountingSubject | 🟡 中 | ✅ 已裁决 | accounting_subjects为准 |
| C-004 | stores vs stores_new | Store | 🟡 中 | ⚠️ 迁移中 | 使用stores_new |
| C-005 | 订单状态三套并存 | Order | 🟡 中 | ✅ 已裁决 | 三套并存，维护映射 |
| C-006 | 凭证状态映射 | Voucher | 🟢 低 | ✅ 已解决 | adapter层封装 |
| C-007 | food + foods双写 | Food | 🔴 高 | ⚠️ 技术债 | 同一事务 |
| C-008 | inventory + store_inventory双写 | Inventory | 🟡 P1 | ⚠️ 持续关注 | 同一事务 |
| C-009 | 成本双写 | OrderCost | 🟡 P1 | ⚠️ 持续关注 | 补偿机制 |
| C-010 | finance_record口径 | FinanceRecord | 🟡 P1 | ⚠️ 待修 | 修正注释 |

### 5.2 冲突影响分析

#### C-001: 金额单位双轨

```
影响范围:
  │
  ├──→ 订单金额: orders表 (分) vs 旧订单 (元)
  │
  ├──→ 应付金额: payables表 (分) vs account_balance (元)
  │
  ├──→ 应收金额: receivables表 (分) vs account_balance (元)
  │
  └──→ 财务流水: finance_records表 (分) vs tax_record (元)

风险:
  - 跨表JOIN时金额差100倍
  - 报表统计口径错误
  - 财务对账失败

缓解措施:
  - 新表统一用「分」
  - 读遗留表时显式转换
  - 逐步迁移遗留表
```

#### C-007: food + foods双写

```
影响范围:
  │
  ├──→ 下单: 同时写food和foods表
  │
  ├──→ 退款: 同时更新food和foods表
  │
  └──→ 超时: 同时更新food和foods表

风险:
  - 双写不一致导致库存/状态错乱
  - 写入顺序依赖
  - 事务边界不一致

缓解措施:
  - 同一事务内双写
  - 逐步废弃food表写入
  - 建立数据校验机制
```

#### C-008: inventory + store_inventory双写

```
影响范围:
  │
  ├──→ 采购入库: 按类型双写inventory或store_inventory
  │
  ├──→ 调拨: 从inventory到store_inventory
  │
  └──→ 收货: 按receiverType双写

风险:
  - 双写事务边界不一致
  - 中央仓/门店仓库存不同步
  - 库存查询口径混乱

缓解措施:
  - 两表各自独立管理
  - 双写必须在同一事务内
  - 建立库存同步机制
```

---

## 六、数据血缘追踪图

### 6.1 采购链路血缘

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        采购链路数据血缘                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Material (ROOT) ──► PurchaseRequest ──► PurchaseOrder                     │
│       │                                      │                              │
│       │                                      ▼                              │
│       │                               PurchaseArrival                       │
│       │                                      │                              │
│       │                                      ▼                              │
│       │                          ReceiptConfirmation                        │
│       │                                      │                              │
│       │                                      ▼                              │
│       │                               Inventory ──► Payable                 │
│       │                                                │                    │
│       │                                                ▼                    │
│       │                                         Payment ──► FundFlow        │
│       │                                                  │                  │
│       └──────────────────────────────────────────────────┘                  │
│                                                                             │
│  MaterialTraceCode ──► TraceabilityService                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 销售链路血缘

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        销售链路数据血缘                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Category (ROOT) ──► Food ──► Order ──► FinanceRecord ──► FundFlow         │
│                            │                                                │
│                            ├──► Receivable ──► Receipt ──► FundFlow         │
│                            │                                                │
│                            └──► MaterialTraceCode ──► TraceabilityService  │
│                                                                             │
│  Store ──► StoreInventory ──► Order                                        │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.3 财务链路血缘

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        财务链路数据血缘                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  AccountingSubject (ROOT) ──► Voucher ──► FinanceRecord                    │
│                                      │                                      │
│                                      ├──► TaxRecord ──► TaxDeclaration     │
│                                      │                                      │
│                                      └──► FundFlow ──► CashFlowReport      │
│                                                                             │
│  BankAccount (ROOT) ──► FundFlow                                           │
│                                                                             │
│  SalaryPaidEvent ──► Voucher                                               │
│  StoreDailySettlementCompletedEvent ──► Voucher                            │
│  InvoiceReimbursementApprovedEvent ──► Voucher                             │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.4 HR链路血缘

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        HR链路数据血缘                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Organization (ROOT) ──► Department ──► Employee                           │
│                                │              │                             │
│                                │              ├──► Attendance ──► Salary    │
│                                │              │                             │
│                                │              └──► InvoiceReimbursement     │
│                                │                        │                   │
│                                │                        ▼                   │
│                                │                 Payment ──► FundFlow       │
│                                │                                            │
│                                └──► Position                               │
│                                                                             │
│  EmployeeCreatedEvent ──► HREventListener ──► 更新员工计数                  │
│  EmployeeResignedEvent ──► HREventListener ──► 更新员工计数                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 七、数据血缘风险矩阵

### 7.1 高风险血缘点

| # | 血缘点 | 风险描述 | 影响范围 | 缓解措施 |
|---|--------|----------|----------|----------|
| 1 | Order → FinanceRecord | 订单完成后财务记录可能丢失 | 财务报表 | 事件重试机制 |
| 2 | PurchaseStockin → Inventory | 入库后库存可能未更新 | 库存准确性 | 同一事务 |
| 3 | ReceiptConfirmation → Payable | 收货确认后应付可能未生成 | 财务对账 | 事件补偿机制 |
| 4 | SalaryPaid → Voucher | 薪资发放后凭证可能未生成 | 财务报表 | 事件重试机制 |
| 5 | StoreDailySettlement → Voucher | 日结后凭证可能未生成 | 财务报表 | 事件重试机制 |

### 7.2 中风险血缘点

| # | 血缘点 | 风险描述 | 影响范围 | 缓解措施 |
|---|--------|----------|----------|----------|
| 6 | Food → Order | 菜品下架后订单仍可创建 | 数据一致性 | 状态校验 |
| 7 | Material → Inventory | 物料停用后库存仍可操作 | 数据一致性 | 状态校验 |
| 8 | Employee → Attendance | 员工离职后考勤仍可记录 | 数据一致性 | 状态校验 |
| 9 | Member → Order | 会员注销后订单仍可查询 | 数据完整性 | 软删除 |
| 10 | Store → Order | 门店关闭后订单仍可创建 | 数据一致性 | 状态校验 |

### 7.3 低风险血缘点

| # | 血缘点 | 风险描述 | 影响范围 | 缓解措施 |
|---|--------|----------|----------|----------|
| 11 | Category → Food | 分类停用后菜品仍可显示 | 显示准确性 | 状态过滤 |
| 12 | AccountingSubject → Voucher | 科目停用后凭证仍可查询 | 查询准确性 | 状态过滤 |
| 13 | BankAccount → FundFlow | 账户注销后流水仍可查询 | 查询准确性 | 状态过滤 |

---

## 八、数据血缘监控建议

### 8.1 监控指标

| 监控类型 | 监控指标 | 告警阈值 | 处理方式 |
|----------|----------|----------|----------|
| 事件丢失 | 事件投递失败率 | >0.1% | 立即排查 |
| 事件延迟 | 事件处理延迟 | >5秒 | 监控告警 |
| 数据不一致 | 跨表数据差异 | >0 | 定时校验 |
| 事务失败 | 分布式事务失败率 | >0.01% | 立即排查 |
| 快照过期 | 快照数据缺失 | >0 | 补偿生成 |

### 8.2 监控实现建议

1. **事件监控**：在RabbitMQ层面监控事件投递和消费情况，建立事件追踪链路。

2. **数据一致性监控**：定时校验关键业务对象的数据一致性，如订单状态、库存数量、应付余额等。

3. **事务监控**：监控分布式事务的执行情况，对失败事务进行补偿处理。

4. **快照监控**：监控关键业务对象的快照生成情况，确保快照数据完整。

5. **血缘追踪**：建立端到端的数据血缘追踪系统，便于问题排查和影响分析。

---

## 九、总结

### 9.1 关键发现

1. **数据所有权分散**：部分业务对象（如Order）在多个终端都有写入权限，需要严格的事务控制。

2. **事件驱动架构**：系统大量使用RabbitMQ事件进行跨域协调，但存在事件丢失风险。

3. **数据一致性挑战**：金额单位双轨、双表并存等历史问题增加了数据一致性风险。

4. **快照机制不完善**：部分业务对象缺少完整的快照机制，影响审计和问题排查。

### 9.2 改进建议

1. **建立数据所有权清单**：明确每个业务对象的唯一OWNER，其他终端只读或通过事件更新。

2. **加强事件可靠性**：实现事件持久化、重试机制、死信队列，确保事件不丢失。

3. **统一数据单位**：逐步统一金额单位为「分」，消除双轨问题。

4. **完善快照机制**：为关键业务对象建立完整的快照机制，支持审计和问题排查。

5. **建立血缘追踪系统**：实现端到端的数据血缘追踪，便于问题排查和影响分析。

---

*生成时间: 2026-09-09*
*数据来源: Project Master Map 静态分析 + 代码库分析*