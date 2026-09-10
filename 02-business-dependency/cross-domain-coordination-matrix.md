# 食品溯源系统 - 跨域协调矩阵

> 版本: 1.0 | 生成日期: 2026-09-09 | 基于: Project Master Map 静态分析

---

## 一、跨域协调总览

### 1.1 业务域定义

| 业务域 | 说明 | 主要职责 |
|--------|------|----------|
| **Management Core** | 管理后台 | 主数据管理、配置管理、报表管理 |
| **POS** | 收银终端 | 点餐、收银、订单处理 |
| **Kitchen** | 厨房终端 | 接单、出餐、状态更新 |
| **Employee** | 员工终端 | 排班、考勤、任务、审批 |
| **Mini Program** | 小程序 | 菜单浏览、在线点餐、会员服务 |
| **Receiving Mobile** | 收货移动端 | 收货确认、质检、入库 |
| **Inventory** | 库存管理 | 中央库存、门店库存、调拨 |
| **Finance** | 财务管理 | 凭证、应付、应收、资金 |

### 1.2 角色定义

| 角色 | 说明 | 权限级别 |
|------|------|----------|
| **OWNER** | 数据真相源，拥有完整CRUD权限 | 最高 |
| **WRITE** | 可写入数据，但非真相源 | 中 |
| **READ** | 只读访问 | 低 |
| **REFERENCE** | 仅引用ID，不读取完整数据 | 最低 |
| **SNAPSHOT** | 读取时创建快照，与源数据可能不一致 | 中 |
| **EVENT CONSUMER** | 通过事件接收数据变更 | 中 |
| **DEPENDENCY** | 强依赖，数据不可用则功能不可用 | 高 |

---

## 二、跨域协调矩阵

### 2.1 核心业务对象矩阵

| 业务对象 | Management Core | POS | Kitchen | Employee | Mini Program | Receiving Mobile | Inventory | Finance |
|----------|----------------|-----|---------|----------|--------------|------------------|-----------|---------|
| **Food (菜品)** | OWNER | READ | READ | READ | READ | - | READ | READ |
| **Material (物料)** | OWNER | - | - | READ | - | READ | READ | READ |
| **Order (订单)** | READ | OWNER | READ | READ | WRITE | - | EVENT CONSUMER | EVENT CONSUMER |
| **Inventory (库存)** | READ | READ | - | - | READ | WRITE | OWNER | READ |
| **StoreInventory (门店库存)** | READ | OWNER | - | - | READ | - | EVENT CONSUMER | READ |
| **Payable (应付)** | READ | - | - | - | - | EVENT CONSUMER | - | OWNER |
| **Receivable (应收)** | READ | EVENT CONSUMER | - | - | - | - | - | OWNER |
| **Voucher (凭证)** | READ | - | - | - | - | - | - | OWNER |
| **Employee (员工)** | OWNER | - | - | READ | - | READ | - | READ |
| **Supplier (供应商)** | OWNER | - | - | - | - | READ | READ | READ |
| **Store (门店)** | OWNER | READ | READ | READ | READ | READ | READ | READ |
| **Member (会员)** | OWNER | READ | - | - | WRITE | - | - | READ |
| **BankAccount (银行账户)** | READ | - | - | - | - | - | - | OWNER |
| **AccountingSubject (科目)** | READ | - | - | - | - | - | - | OWNER |

### 2.2 详细角色分析

#### Food (菜品)

| 业务域 | 角色 | 数据来源 | 写入操作 | 证据 |
|--------|------|----------|----------|------|
| Management Core | OWNER | foods表 | CRUD | FoodService.create/update/delete |
| POS | READ | DB: foods | - | 点餐时读取菜品信息 |
| Kitchen | READ | DB: foods | - | 显示菜品名称 |
| Employee | READ | DB: foods | - | 员工端查看菜品 |
| Mini Program | READ | API: /v1/foods | - | 菜单浏览 |
| Receiving Mobile | - | - | - | 不涉及菜品 |
| Inventory | READ | DB: foods | - | 库存关联 |
| Finance | READ | DB: foods | - | 成本计算 |

#### Order (订单)

| 业务域 | 角色 | 数据来源 | 写入操作 | 证据 |
|--------|------|----------|----------|------|
| Management Core | READ | DB: orders | 查询/统计 | OrderService查询 |
| POS | OWNER | DB: orders | CREATE | PosOrderCreateService.create |
| Kitchen | READ | API/Event | - | OrderCreatedEvent |
| Employee | READ | DB: orders | - | 订单查询 |
| Mini Program | WRITE | API: /v1/orders | CREATE | 小程序下单 |
| Receiving Mobile | - | - | - | 不涉及订单 |
| Inventory | EVENT CONSUMER | OrderCreatedEvent | - | 锁定原料 |
| Finance | EVENT CONSUMER | OrderCompletedEvent | - | 记录财务收入 |

#### Inventory (库存)

| 业务域 | 角色 | 数据来源 | 写入操作 | 证据 |
|--------|------|----------|----------|------|
| Management Core | READ | DB: inventory | 查询 | InventoryService查询 |
| POS | READ | DB: foods.stock | - | 可售判断 |
| Kitchen | - | - | - | 不涉及库存 |
| Employee | - | - | - | 不涉及库存 |
| Mini Program | READ | API | - | 库存显示 |
| Receiving Mobile | WRITE | API: /v1/inventory/in | UPDATE | 收货入库 |
| Inventory | OWNER | DB: inventory | CRUD | InventoryService |
| Finance | READ | DB: inventory | - | 库存报表 |

#### Payable (应付)

| 业务域 | 角色 | 数据来源 | 写入操作 | 证据 |
|--------|------|----------|----------|------|
| Management Core | READ | DB: payables | 查询 | PayableService查询 |
| POS | - | - | - | 不涉及应付 |
| Kitchen | - | - | - | 不涉及应付 |
| Employee | - | - | - | 不涉及应付 |
| Mini Program | - | - | - | 不涉及应付 |
| Receiving Mobile | EVENT CONSUMER | ReceiptConfirmationCompletedEvent | - | 生成应付 |
| Inventory | - | - | - | 不涉及应付 |
| Finance | OWNER | DB: payables | CRUD | PayableService |

#### Employee (员工)

| 业务域 | 角色 | 数据来源 | 写入操作 | 证据 |
|--------|------|----------|----------|------|
| Management Core | OWNER | DB: employees | CRUD | EmployeeService |
| POS | - | - | - | 不涉及员工管理 |
| Kitchen | - | - | - | 不涉及员工管理 |
| Employee | READ | DB: employees | - | 个人中心 |
| Mini Program | - | - | - | 不涉及员工 |
| Receiving Mobile | READ | DB: employees | - | 收货员信息 |
| Inventory | - | - | - | 不涉及员工 |
| Finance | READ | DB: employees | - | 报销关联 |

---

## 三、下游系统反向定义上游事实

### 3.1 反向依赖分析

| 下游系统 | 上游事实 | 反向定义方式 | 风险 |
|----------|----------|--------------|------|
| **POS** → Management Core | 菜品可售状态 | POS可售=foods.stock | 🟡 中 - 依赖库存准确性 |
| **Kitchen** → Management Core | 订单出餐状态 | KitchenDisplay更新order_status | 🟡 中 - 状态机复杂 |
| **Employee** → Management Core | 考勤打卡数据 | 打卡记录反推排班 | 🟢 低 - 有校验机制 |
| **Mini Program** → Management Core | 会员行为数据 | 浏览/下单行为反推偏好 | 🟢 低 - 只读分析 |
| **Receiving Mobile** → Management Core | 实际收货数量 | 收货确认反推采购订单 | 🟡 中 - 可能超收/欠收 |
| **Inventory** → Management Core | 实际库存数量 | 库存盘点反推理论库存 | 🔴 高 - 库存差异风险 |
| **Finance** → Management Core | 实际收支数据 | 资金流水反推业务发生额 | 🔴 高 - 财务对账风险 |

### 3.2 关键反向依赖链路

#### 3.2.1 POS 反向定义菜品可售

```
POS终端
  │
  ├── 读取 foods.stock (可售数量)
  │
  ├── 点餐时扣减库存
  │
  └── 反向影响: foods.stock 变为0 → 菜品不可售
```

**证据**:
- business-object-map.md §4: StoreInventory.Consumers 包含 OrderService
- truth-conflict-map.md §1: 可售口径=foods.stock

#### 3.2.2 Kitchen 反向定义订单状态

```
Kitchen终端
  │
  ├── 读取 OrderCreatedEvent
  │
  ├── 更新出餐状态
  │
  └── 反向影响: order_status 变为 PREPARING/COMPLETED
```

**证据**:
- event-job-map.md §1: OrderCreatedEvent
- business-object-map.md §5: Order.State 包含 PREPARING, COMPLETED

#### 3.2.3 Receiving Mobile 反向定义采购订单

```
Receiving Mobile
  │
  ├── 读取 purchase_arrivals
  │
  ├── 确认收货数量
  │
  └── 反向影响: 生成 payable (应付账款)
```

**证据**:
- data-inheritance-map.md §2.1: 链路3: 到货单 → 收货确认
- event-job-map.md §2: ReceiptConfirmationCompletedEvent

#### 3.2.4 Inventory 反向定义理论库存

```
Inventory模块
  │
  ├── 读取 inventory 表
  │
  ├── 库存盘点
  │
  └── 反向影响: 调整 inventory 数量 (盘盈/盘亏)
```

**证据**:
- business-object-map.md §3: Inventory.Write Path 包含 stockIn/stockOut/transfer
- truth-conflict-map.md §8: inventory + store_inventory 双写

#### 3.2.5 Finance 反向定义业务发生额

```
Finance模块
  │
  ├── 读取 payables/receivables
  │
  ├── 收付款确认
  │
  └── 反向影响: 生成 fund_flows (资金流水)
```

**证据**:
- business-object-map.md §12: FundFlow.Dependencies 包含 BankAccount, Voucher
- upstream-dependency-map.md §9: FundFlow 依赖 BankAccount + Voucher

---

## 四、跨终端数据流矩阵

### 4.1 数据流向总览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        跨终端数据流向图                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Management Core ────────────────────────────────────────────────────────── │
│       │                                                                     │
│       ├──→ POS: 菜品、价格、库存                                             │
│       │                                                                     │
│       ├──→ Kitchen: 订单、菜品                                              │
│       │                                                                     │
│       ├──→ Employee: 排班、任务                                             │
│       │                                                                     │
│       ├──→ MiniProgram: 菜品、活动                                          │
│       │                                                                     │
│       └──→ Receiving Mobile: 采购单、收货任务                                │
│                                                                             │
│  POS ────────────────────────────────────────────────────────────────────── │
│       │                                                                     │
│       ├──→ Kitchen: 订单、菜品                                              │
│       │                                                                     │
│       └──→ Finance: 订单、收款                                              │
│                                                                             │
│  Kitchen ────────────────────────────────────────────────────────────────── │
│       │                                                                     │
│       └──→ POS: 出餐状态                                                    │
│                                                                             │
│  Employee ───────────────────────────────────────────────────────────────── │
│       │                                                                     │
│       └──→ Management Core: 考勤、审批                                      │
│                                                                             │
│  MiniProgram ────────────────────────────────────────────────────────────── │
│       │                                                                     │
│       └──→ Management Core: 订单、会员                                      │
│                                                                             │
│  Receiving Mobile ───────────────────────────────────────────────────────── │
│       │                                                                     │
│       ├──→ Management Core: 收货结果                                        │
│       │                                                                     │
│       └──→ Inventory: 入库数据                                              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 详细数据流分析

#### 4.2.1 Management → POS

| 数据类型 | 数据内容 | 传输方式 | 频率 | 证据 |
|----------|----------|----------|------|------|
| 菜品 | Food信息、分类、价格 | DB: foods表 | 实时 | FoodService |
| 价格 | 菜品定价、套餐价格 | DB: foods.price | 实时 | PricingService |
| 库存 | 门店库存数量 | DB: store_inventory | 实时 | StoreInventoryService |
| 门店配置 | 营业时间、打印机配置 | DB: stores_new | 实时 | StoreNewService |

#### 4.2.2 POS → Kitchen

| 数据类型 | 数据内容 | 传输方式 | 频率 | 证据 |
|----------|----------|----------|------|------|
| 订单 | OrderCreatedEvent | RabbitMQ | 事件驱动 | event-job-map.md §1 |
| 菜品 | 菜品名称、数量 | API: /v1/orders | 实时 | KitchenOrderService |
| 特殊要求 | 口味、加料等 | 订单备注 | 实时 | order_items.remark |

#### 4.2.3 Kitchen → POS

| 数据类型 | 数据内容 | 传输方式 | 频率 | 证据 |
|----------|----------|----------|------|------|
| 出餐状态 | PREPARING/COMPLETED | WebSocket/轮询 | 实时 | KitchenDisplayService |
| 预计时间 | 出餐倒计时 | WebSocket | 实时 | KitchenOrderService |

#### 4.2.4 Management → Employee

| 数据类型 | 数据内容 | 传输方式 | 频率 | 证据 |
|----------|----------|----------|------|------|
| 排班 | 班次、时间、门店 | DB: schedule | 日更新 | ScheduleService |
| 任务 | 任务列表、截止时间 | DB: tasks | 实时 | TaskService |
| 通知 | 系统公告、审批提醒 | DB: notifications | 实时 | NotificationService |

#### 4.2.5 Employee → Management

| 数据类型 | 数据内容 | 传输方式 | 频率 | 证据 |
|----------|----------|----------|------|------|
| 考勤 | 打卡时间、位置 | API: /v1/attendance | 实时 | AttendanceRecordService |
| 审批 | 审批结果、意见 | API: /v1/approvals | 实时 | ApprovalService |
| 请假 | 请假申请、类型 | API: /v1/leaves | 实时 | LeaveService |

#### 4.2.6 Management → MiniProgram

| 数据类型 | 数据内容 | 传输方式 | 频率 | 证据 |
|----------|----------|----------|------|------|
| 菜品 | 菜单、图片、价格 | API: /v1/foods | 实时 | FoodService |
| 活动 | 优惠券、促销 | API: /v1/coupons | 实时 | CouponService |
| 会员 | 会员等级、积分 | API: /v1/members | 实时 | MemberService |

#### 4.2.7 MiniProgram → Management

| 数据类型 | 数据内容 | 传输方式 | 频率 | 证据 |
|----------|----------|----------|------|------|
| 订单 | 在线下单数据 | API: /v1/orders | 实时 | OrderService |
| 会员 | 注册、行为数据 | API: /v1/members | 实时 | MemberService |
| 反馈 | 评价、投诉 | API: /v1/appeals | 实时 | AppealService |

#### 4.2.8 Management → Receiving Mobile

| 数据类型 | 数据内容 | 传输方式 | 频率 | 证据 |
|----------|----------|----------|------|------|
| 采购单 | 采购订单信息 | API: /v1/purchase/* | 实时 | PurchaseOrderService |
| 收货任务 | 到货单、预期数量 | API: /v1/purchase/arrivals | 实时 | PurchaseArrivalService |
| 物料信息 | 物料名称、规格 | API: /v1/materials | 实时 | MaterialService |

#### 4.2.9 Receiving Mobile → Management

| 数据类型 | 数据内容 | 传输方式 | 频率 | 证据 |
|----------|----------|----------|------|------|
| 收货结果 | 实际收货数量、质量 | API: /v1/receipt-confirmations | 实时 | ReceiptConfirmationService |
| 质检数据 | 质检报告、合格率 | API | 实时 | ReceiptConfirmationService |
| 入库确认 | 入库单、批次号 | API | 实时 | PurchaseStockinService |

---

## 五、数据所有权矩阵

### 5.1 数据所有权分析

| 业务对象 | OWNER | READ下游 | WRITE下游 | 事件消费 |
|----------|-------|----------|-----------|----------|
| Food | Management Core | POS, Kitchen, Employee, MiniProgram, Inventory, Finance | - | - |
| Material | Management Core | Employee, Receiving Mobile, Inventory, Finance | - | - |
| Order | POS, MiniProgram | Management Core, Kitchen, Employee, Inventory, Finance | - | - |
| Inventory | Inventory模块 | Management Core, POS, MiniProgram, Finance | Receiving Mobile | OrderCreatedEvent |
| StoreInventory | POS | Management Core, MiniProgram | - | - |
| Payable | Finance | Management Core | Receiving Mobile | ReceiptConfirmationCompletedEvent |
| Receivable | Finance | Management Core | POS | OrderCompletedEvent |
| Voucher | Finance | Management Core | - | SalaryPaidEvent, StoreDailySettlementCompletedEvent |
| Employee | Management Core | Employee, Receiving Mobile, Finance | - | - |
| Supplier | Management Core | Receiving Mobile, Inventory, Finance | - | - |
| Store | Management Core | POS, Kitchen, Employee, MiniProgram, Receiving Mobile, Inventory, Finance | - | - |
| Member | Management Core | POS, MiniProgram | MiniProgram | - |
| BankAccount | Finance | Management Core | - | - |
| AccountingSubject | Finance | Management Core | - | - |

### 5.2 数据冲突风险点

| 冲突点 | 涉及对象 | 风险等级 | 缓解措施 |
|--------|----------|----------|----------|
| 金额单位双轨 | 所有财务对象 | 🔴 高 | 统一为「分」 |
| food/foods双写 | Food | 🔴 高 | 同一事务 |
| inventory/store_inventory双写 | Inventory, StoreInventory | 🟡 P1 | 同一事务 |
| 订单状态三套并存 | Order | 🟡 P1 | 维护映射表 |
| stores/stores_new双表 | Store | 🟡 P1 | 迁移到stores_new |

---

## 六、跨域协调风险矩阵

### 6.1 高风险协调点

| # | 协调点 | 风险描述 | 影响范围 | 缓解措施 |
|---|--------|----------|----------|----------|
| 1 | POS → Kitchen 订单传递 | 网络中断导致厨房无法接单 | 门店运营 | 本地缓存 + 重试机制 |
| 2 | Kitchen → POS 出餐状态 | 状态更新延迟导致超时取消 | 客户体验 | WebSocket实时推送 |
| 3 | Receiving Mobile → Inventory 入库 | 收货确认后库存未及时更新 | 库存准确性 | 同一事务 |
| 4 | Finance → Management 凭证 | 凭证生成失败导致财务数据不完整 | 财务报表 | 事件重试 + 补偿机制 |
| 5 | MiniProgram → Management 订单 | 在线下单后订单状态不同步 | 客户体验 | 实时同步机制 |

### 6.2 中风险协调点

| # | 协调点 | 风险描述 | 影响范围 | 缓解措施 |
|---|--------|----------|----------|----------|
| 6 | Management → Employee 排班 | 排班更新后员工未及时查看 | 考勤准确性 | 推送通知 |
| 7 | Employee → Management 考勤 | 打卡数据延迟上报 | 考勤统计 | 离线缓存 + 同步 |
| 8 | Management → MiniProgram 菜品 | 菜品下架后小程序仍显示 | 客户体验 | 定时刷新 |
| 9 | Inventory → Finance 库存报表 | 库存数据不一致导致报表错误 | 财务准确性 | 数据校验 |
| 10 | POS → Finance 收款 | 收款确认延迟导致账务不平 | 财务对账 | 实时同步 |

### 6.3 低风险协调点

| # | 协调点 | 风险描述 | 影响范围 | 缓解措施 |
|---|--------|----------|----------|----------|
| 11 | Management → Kitchen 菜品 | 菜品更新后厨房显示延迟 | 显示准确性 | 定时刷新 |
| 12 | Employee → Management 审批 | 审批结果延迟通知 | 流程效率 | 推送通知 |
| 13 | MiniProgram → Management 会员 | 会员行为数据延迟分析 | 分析准确性 | 批量同步 |

---

## 七、关键发现与建议

### 7.1 关键发现

1. **下游系统反向定义上游事实**：POS、Kitchen、Receiving Mobile、Inventory、Finance 等下游系统通过事件或API反向更新上游数据，形成复杂的双向依赖关系。

2. **数据所有权分散**：部分业务对象（如Order）在多个终端都有写入权限，需要严格的事务控制。

3. **事件驱动架构**：系统大量使用RabbitMQ事件进行跨域协调，但存在事件丢失风险。

4. **数据一致性挑战**：金额单位双轨、双表并存等历史问题增加了数据一致性风险。

### 7.2 建议

1. **建立数据所有权清单**：明确每个业务对象的唯一OWNER，其他终端只读或通过事件更新。

2. **加强事件可靠性**：实现事件持久化、重试机制、死信队列，确保事件不丢失。

3. **统一数据单位**：逐步统一金额单位为「分」，消除双轨问题。

4. **优化跨域缓存**：对于高频读取的跨域数据（如菜品、门店），建立分布式缓存机制。

5. **建立数据血缘追踪**：实现端到端的数据血缘追踪，便于问题排查和影响分析。

---

*生成时间: 2026-09-09*
*数据来源: Project Master Map 静态分析 + 代码库分析*