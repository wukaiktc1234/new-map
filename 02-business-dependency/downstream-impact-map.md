# 食品溯源系统 - Downstream 影响地图

> 最后更新：2026-09-09

---

## 一、Foundation/Master 影响总览

| Foundation/Master | 真实消费者 | 影响范围 | 严重程度 |
|-------------------|------------|----------|----------|
| **Food (菜品)** | OrderService, InventoryService, MenuService, POS前端, 小程序 | 订单、库存、菜单、溯源 | 🔴 高 |
| **Material (物料)** | InventoryService, TraceabilityService, ProductionService, 采购模块 | 库存、溯源、采购 | 🔴 高 |
| **Store (门店)** | OrderService, InventoryService, FinanceService, 所有终端 | 全业务流程 | 🔴 高 |
| **Supplier (供应商)** | PurchaseService, PayableService, TraceabilityService | 采购、应付、溯源 | 🔴 高 |
| **AccountingSubject (科目)** | VoucherService, FinanceReportService, TaxService | 财务核心 | 🔴 高 |
| **Employee (员工)** | HR模块, 薪资模块, 报销模块 | 人力资源 | 🟡 中 |
| **Customer (客户)** | OrderService, ReceivableService, MemberService | 订单、应收、会员 | 🟡 中 |
| **Category (分类)** | FoodService, MaterialService, FoodCategoryService | 产品管理 | 🟡 中 |
| **Warehouse (仓库)** | InventoryService, PurchaseService | 库存、采购 | 🟡 中 |
| **Permission (权限)** | 所有业务模块 | 全系统安全 | 🔴 高 |
| **DataScope (数据范围)** | 所有业务查询 | 数据隔离 | 🔴 高 |

---

## 二、核心链路影响分析

### 2.1 菜品 (Food) 链路

```
Foundation: Food (foods表)
    │
    ├──→ Capability: 订单管理 (OrderService)
    │       │
    │       ├──→ Transaction: POS点餐 (frontend-pos)
    │       │       └──→ Settlement: 日结对账 (DailySettlementTask)
    │       │               └──→ Reporting: 营业报表
    │       │
    │       ├──→ Transaction: 小程序点餐 (miniprogram)
    │       │       └──→ Settlement: 在线支付结算
    │       │
    │       └──→ Transaction: 外卖订单
    │               └──→ Settlement: 配送结算
    │
    ├──→ Capability: 库存管理 (InventoryService)
    │       ├──→ Transaction: 出库 (stockOut)
    │       └──→ Transaction: 调拨 (transfer)
    │
    └──→ Capability: 溯源管理 (TraceabilityService)
            └──→ Transaction: 溯源查询 (小程序扫码)
```

**停用影响**：
- POS终端无法点餐 → 门店停业
- 小程序无法下单 → 线上业务中断
- 库存无法扣减 → 财务数据错误
- 溯源链路断裂 → 合规风险

### 2.2 物料 (Material) 链路

```
Foundation: Material (material_archives表)
    │
    ├──→ Capability: 库存管理 (InventoryService)
    │       ├──→ Transaction: 入库 (stockIn)
    │       │       └──→ Settlement: 采购结算
    │       │               └──→ Reporting: 库存报表
    │       │
    │       └──→ Transaction: 出库 (stockOut)
    │               └──→ Settlement: 生产领料
    │
    ├──→ Capability: 采购管理 (PurchaseService)
    │       ├──→ Transaction: 采购申请 (purchase_request)
    │       ├──→ Transaction: 采购订单 (purchase_orders)
    │       └──→ Transaction: 收货确认 (purchase_arrivals)
    │               └──→ Settlement: 应付账款 (Payable)
    │                       └──→ Reporting: 采购报表
    │
    └──→ Capability: 溯源管理 (TraceabilityService)
            └──→ Transaction: 物料追溯码 (MaterialTraceCode)
                    └──→ Reporting: 溯源报告
```

**停用影响**：
- 采购无法执行 → 原材料断供
- 库存无法管理 → 生产停滞
- 溯源链路断裂 → 食品安全合规风险

### 2.3 门店 (Store) 链路

```
Foundation: Store (stores_new表)
    │
    ├──→ Capability: 订单管理
    │       ├──→ Transaction: 门店订单
    │       │       └──→ Settlement: 门店日结
    │       │               └──→ Reporting: 门店报表
    │       │
    │       └──→ Transaction: 桌位管理 (dining_tables)
    │
    ├──→ Capability: 库存管理
    │       ├──→ Transaction: 门店库存 (store_inventory)
    │       └──→ Transaction: 调拨到店
    │
    ├──→ Capability: 财务管理
    │       ├──→ Transaction: 门店收款
    │       └──→ Transaction: 门店付款
    │
    ├──→ Capability: 人力资源
    │       ├──→ Transaction: 员工排班
    │       └──→ Transaction: 考勤管理
    │
    └──→ DataScope: 数据范围过滤
            └──→ 所有查询的 store_id 过滤
```

**停用影响**：
- 门店无法营业 → 收入损失
- 数据范围失效 → 数据泄露风险
- 员工管理混乱 → 运营中断

### 2.4 供应商 (Supplier) 链路

```
Foundation: Supplier (suppliers表)
    │
    ├──→ Capability: 采购管理
    │       ├──→ Transaction: 采购订单
    │       │       └──→ Settlement: 应付账款
    │       │               └──→ Payment: 付款
    │       │                       └──→ FundFlow: 资金流水
    │       │
    │       └──→ Transaction: 收货确认
    │               └──→ Inventory: 入库
    │
    ├──→ Capability: 溯源管理
    │       └──→ Transaction: 供应商溯源码
    │
    └──→ Capability: 合同管理
            └──→ Transaction: 供应商合同 (H5端点)
```

**停用影响**：
- 采购链路断裂 → 原材料断供
- 应付无法结算 → 财务风险
- 溯源链路断裂 → 合规风险

### 2.5 会计科目 (AccountingSubject) 链路

```
Foundation: AccountingSubject (accounting_subjects表)
    │
    ├──→ Capability: 凭证管理
    │       ├──→ Transaction: 凭证创建
    │       ├──→ Transaction: 凭证过账
    │       └──→ Transaction: 凭证反过账
    │
    ├──→ Capability: 财务报表
    │       ├──→ Transaction: 资产负债表
    │       ├──→ Transaction: 利润表
    │       └──→ Transaction: 现金流量表
    │
    └──→ Capability: 税务管理
            └──→ Transaction: 税务申报
```

**停用影响**：
- 财务核算无法进行 → 财务瘫痪
- 报表无法生成 → 合规风险
- 税务无法申报 → 法律风险

---

## 三、事件驱动影响分析

### 3.1 订单事件链

| 事件 | 生产者 | 消费者 | 影响 |
|------|--------|--------|------|
| OrderCreatedEvent | OrderService | InventoryService, KitchenDisplay, Notification | 库存扣减、厨房显示、通知 |
| OrderCompletedEvent | OrderService | FinanceService, Settlement, Notification | 财务入账、日结、通知 |
| OrderRefundEvent | OrderService | FinanceService, InventoryService, Notification | 退款处理、库存回滚、通知 |

**事件中断影响**：
- OrderCreatedEvent 中断 → 库存不扣减 → 超卖
- OrderCompletedEvent 中断 → 财务不入账 → 账务不平
- OrderRefundEvent 中断 → 退款不处理 → 客户投诉

### 3.2 采购事件链

| 事件 | 生产者 | 消费者 | 影响 |
|------|--------|--------|------|
| PurchaseStockInEvent | PurchaseService | InventoryService, FinanceService, Notification | 库存增加、应付生成、通知 |
| ReceiptConfirmationCompletedEvent | PurchaseService | FinanceService, PayableService, Notification | 应付确认、通知 |

**事件中断影响**：
- PurchaseStockInEvent 中断 → 库存不增加 → 生产断料
- ReceiptConfirmationCompletedEvent 中断 → 应付不确认 → 付款延迟

### 3.3 财务事件链

| 事件 | 生产者 | 消费者 | 影响 |
|------|--------|--------|------|
| VoucherPostedEvent | VoucherService | ReportService, Notification | 报表更新、通知 |
| PaymentCompletedEvent | PaymentService | PayableService, Notification | 应付核销、通知 |
| BudgetExceededEvent | BudgetService | Notification, ReportService | 预算告警、报表 |

**事件中断影响**：
- VoucherPostedEvent 中断 → 报表数据不准确
- PaymentCompletedEvent 中断 → 应付未核销 → 重复付款风险
- BudgetExceededEvent 中断 → 预算超支无告警

---

## 四、数据继承影响分析

### 4.1 数据流向图

```
Food ─────────┐
              │
Material ─────┼──→ Order ───→ FinanceRecord ───→ FundFlow
              │     │
Store ────────┘     │
                    │
Supplier ───────────┼──→ Purchase ───→ Payable ───→ Payment ───→ FundFlow
                    │
AccountingSubject ──┼──→ Voucher ───→ FinanceRecord
                    │
Employee ───────────┼──→ Salary ───→ Payment ───→ FundFlow
                    │
Customer ───────────┼──→ Receivable ───→ Receipt ───→ FundFlow
```

### 4.2 数据继承中断点

| 中断点 | 上游 | 下游 | 影响 |
|--------|------|------|------|
| Food → Order | FoodService | OrderService | 订单无法创建 |
| Material → Inventory | MaterialService | InventoryService | 库存无法管理 |
| Store → Order | StoreService | OrderService | 门店订单无法生成 |
| Supplier → Purchase | SupplierService | PurchaseService | 采购无法执行 |
| Order → FinanceRecord | OrderService | FinanceService | 财务无法入账 |
| Purchase → Payable | PurchaseService | PayableService | 应付无法生成 |
| Payable → Payment | PayableService | PaymentService | 付款无法执行 |

---

## 五、权限依赖影响分析

### 5.1 权限依赖链

```
User → Role → Permission → DataScope
  │      │       │            │
  │      │       │            └──→ 查询过滤 (store_id, department_id)
  │      │       │
  │      │       └──→ 接口访问控制 (@RequiresPermission)
  │      │
  │      └──→ 角色绑定 (@RequiresRole)
  │
  └──→ 用户认证 (JWT)
```

### 5.2 权限中断影响

| 权限组件 | 中断影响 |
|----------|----------|
| Role 中断 | 用户无法访问任何业务功能 |
| Permission 中断 | 特定接口无法访问 |
| DataScope 中断 | 数据泄露或数据隔离失效 |
| JWT 中断 | 用户无法登录 |

---

## 六、前端终端影响分析

### 6.1 终端依赖矩阵

| 终端 | 依赖的 Foundation | 依赖的 Capability | 影响 |
|------|-------------------|-------------------|------|
| POS终端 | Food, Store, Order | 订单管理, 收银 | 门店无法收银 |
| Kitchen终端 | Food, Order | 订单管理, 厨房显示 | 厨房无法接单 |
| Employee终端 | Employee, Store | HR模块, 排班 | 员工无法自助 |
| MiniProgram | Food, Order, Customer | 订单管理, 溯源 | 小程序无法使用 |
| Receiving终端 | Material, Supplier | 采购管理, 收货 | 收货无法执行 |
| 管理后台 | 所有 | 所有 | 系统管理瘫痪 |

### 6.2 终端中断影响

| 终端 | 中断场景 | 业务影响 |
|------|----------|----------|
| POS终端 | Food不可用 | 门店无法点餐 → 停业 |
| POS终端 | Store不可用 | 门店无法识别 → 数据混乱 |
| Kitchen终端 | Order不可用 | 厨房无法接单 → 出餐延迟 |
| MiniProgram | Food不可用 | 小程序无法展示菜品 → 客户流失 |
| Receiving终端 | Material不可用 | 收货无法执行 → 库存断供 |

---

## 七、调度任务影响分析

### 7.1 调度任务依赖

| 调度任务 | 依赖的 Foundation | 依赖的 Capability | 影响 |
|----------|-------------------|-------------------|------|
| DailySettlementTask | Store, Order | 日结对账 | 门店日结失败 |
| MonthlyClosingScheduler | AccountingSubject, Voucher | 月末结账 | 财务月结失败 |
| BudgetCheckScheduler | Department, Budget | 预算检查 | 预算超支无告警 |
| OrderTimeoutTask | Order | 订单超时取消 | 超时订单无法取消 |
| AutoBackupScheduler | 所有表 | 数据备份 | 数据丢失风险 |

### 7.2 调度任务中断影响

| 调度任务 | 中断影响 |
|----------|----------|
| DailySettlementTask | 门店日结失败 → 财务数据不准确 |
| MonthlyClosingScheduler | 月度报表无法生成 → 合规风险 |
| BudgetCheckScheduler | 预算超支无告警 → 财务风险 |
| OrderTimeoutTask | 超时订单堆积 → 库存锁定 |
| AutoBackupScheduler | 数据无法备份 → 数据丢失风险 |

---

## 八、影响评估汇总

### 8.1 高影响 Foundation (停用导致系统瘫痪)

| Foundation | 影响范围 | 恢复时间 | 业务损失 |
|------------|----------|----------|----------|
| Food | 订单、库存、溯源 | 4-8小时 | 门店停业、客户流失 |
| Material | 库存、采购、溯源 | 4-8小时 | 生产停滞、断供 |
| Store | 全业务流程 | 8-16小时 | 全面停业 |
| Supplier | 采购、应付 | 4-8小时 | 原材料断供 |
| AccountingSubject | 财务核算 | 8-16小时 | 财务瘫痪 |
| Permission | 全系统安全 | 1-2小时 | 安全漏洞 |

### 8.2 中影响 Foundation (停用导致部分功能异常)

| Foundation | 影响范围 | 恢复时间 | 业务损失 |
|------------|----------|----------|----------|
| Employee | HR、薪资 | 2-4小时 | 员工管理混乱 |
| Customer | 订单、应收 | 2-4小时 | 客户服务中断 |
| Category | 产品管理 | 1-2小时 | 产品分类混乱 |
| Warehouse | 库存、采购 | 2-4小时 | 仓储管理异常 |

### 8.3 低影响 Foundation (停用影响有限)

| Foundation | 影响范围 | 恢复时间 | 业务损失 |
|------------|----------|----------|----------|
| 遗留表 (food, product等) | 无 | 0 | 无 |
| 废弃服务 | 无 | 0 | 无 |

---

## 九、关键发现

### 9.1 高风险发现

1. **Food 双表并存**：`food` 和 `foods` 表并存，可能导致数据不一致
2. **Order 状态冲突**：三套订单状态机并存，状态转换可能不一致
3. **权限缺口**：25个裸接口方法无权限保护
4. **事件链断裂**：AutoVoucherService 零调用点，财务自动凭证未接入

### 9.2 中风险发现

1. **财务双表并存**：`finance_record` 和 `finance_records` 表并存
2. **凭证状态错位**：前后端状态码不一致
3. **供应商H5无认证**：6个外部端点无JWT认证

### 9.3 低风险发现

1. **遗留代码**：8个Legacy服务实现仍存在
2. **死代码**：DatabaseFixConfig 包含死方法
3. **隐藏Fallback**：6个降级机制可能掩盖问题

---

*生成时间：2026-09-09*
*数据来源：代码库静态分析 + 业务对象地图 + API地图 + 状态机地图*
