# Business Data Flow Map

> **项目**: 食品溯源系统 (Food Traceability System)
> **生成时间**: 2026-09-09
> **数据来源**: 代码库静态分析 + 地图文件交叉验证

---

## 一、核心业务链数据流总览

```
┌─────────────────────────────────────────────────────────────────────┐
│                        食品溯源系统 - 核心数据流                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐     │
│  │ 采购链路  │───→│ 库存链路  │───→│ 订单链路  │───→│ 财务链路  │     │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘     │
│       │               │               │               │            │
│       ▼               ▼               ▼               ▼            │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐     │
│  │ 溯源链路  │    │ HR链路   │    │ 营销链路  │    │ 资产链路  │     │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘     │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 二、核心业务链详细数据流

### 2.1 采购链路 (Procurement Chain)

```
Business Event: 采购申请创建
      │
      ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Write: purchase_request                                             │
│ Owner: PurchaseRequestService                                       │
│ Truth Source: purchase_request                                      │
│ Write Source: PurchaseRequestFormDialog                             │
├─────────────────────────────────────────────────────────────────────┤
│ Field Transformation:                                               │
│   - supplierId: 从物料主供应商继承                                    │
│   - items[].materialId: 直接复制                                     │
│   - items[].quantity: 可编辑                                         │
├─────────────────────────────────────────────────────────────────────┤
│ Unit: 金额单位 - 新表分，legacy元                                    │
│ Identity: supplierId → suppliers, materialId → material_archives    │
│ State: 0:草稿 → 1:待审 → 2:已批 → 3:已取消                          │
│ Event: PurchaseRequestCreatedEvent                                  │
│ Consumer: PurchaseOrderService                                      │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼ (转单操作)
┌─────────────────────────────────────────────────────────────────────┐
│ Write: purchase_orders                                              │
│ Owner: PurchaseOrderService                                         │
│ Truth Source: purchase_orders                                       │
│ Write Source: PurchaseOrderFormDialog                               │
├─────────────────────────────────────────────────────────────────────┤
│ Field Transformation:                                               │
│   - supplierId: 从 purchase_request 继承                           │
│   - supplierName: SNAPSHOT - 下单时快照                             │
│   - items[].materialId: 从申请明细继承                              │
│   - items[].materialName: SNAPSHOT                                  │
│   - product_id: Legacy 列名，实际指向物料                            │
├─────────────────────────────────────────────────────────────────────┤
│ Unit: 金额单位 - 分                                                 │
│ Identity: supplierId → suppliers, materialId → material_archives    │
│ State: 0:待确认 → 1:已确认 → 2:已完成 → 3:已取消                    │
│ Event: PurchaseOrderCreatedEvent                                    │
│ Consumer: PurchaseArrivalService, PayableService                    │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼ (到货操作)
┌─────────────────────────────────────────────────────────────────────┐
│ Write: purchase_arrivals                                            │
│ Owner: PurchaseArrivalService                                       │
│ Truth Source: purchase_arrivals                                     │
│ Write Source: ArrivalFormDialog                                     │
├─────────────────────────────────────────────────────────────────────┤
│ Field Transformation:                                               │
│   - orderId: 从 purchase_orders 继承                               │
│   - supplierName: 从 purchase_orders 继承                          │
│   - 物流信息: 全部新建                                              │
├─────────────────────────────────────────────────────────────────────┤
│ Unit: 数量/金额单位 - 分                                            │
│ Identity: orderId → orders, supplierId → suppliers                  │
│ State: 待到货 → 已到货                                              │
│ Event: PurchaseArrivalCreatedEvent                                  │
│ Consumer: ReceiptConfirmationService                                │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼ (收货确认)
┌─────────────────────────────────────────────────────────────────────┐
│ Write: receipt_confirmation + inventory/store_inventory             │
│ Owner: ReceiptConfirmationService                                   │
│ Truth Source: receipt_confirmation                                  │
│ Write Source: ReceiptConfirmationFormDialog                         │
├─────────────────────────────────────────────────────────────────────┤
│ Field Transformation:                                               │
│   - arrivalId: 从 purchase_arrivals 继承                           │
│   - items[].materialId: 逐项继承                                   │
│   - storeId: Hidden Inheritance - 从 arrival 继承                  │
│   - warehouseId: Hidden Inheritance - 从 arrival 继承              │
│   - batchNo/productionDate/expiryDate: 用户输入                     │
├─────────────────────────────────────────────────────────────────────┤
│ Unit: 数量单位                                                     │
│ Identity: arrivalId → purchase_arrivals, materialId → material_archives│
│ State: 0:待收 → 1:已收 → 2:已拒                                    │
│ Event: ReceiptConfirmationCompletedEvent                            │
│ Consumer: InventoryService, TraceabilityService                     │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼ (库存变更)
┌─────────────────────────────────────────────────────────────────────┐
│ Write: inventory + store_inventory (双写)                           │
│ Owner: InventoryService                                             │
│ Truth Source: inventory / store_inventory                           │
│ Write Source: ReceiptConfirmationCompletedEvent                     │
├─────────────────────────────────────────────────────────────────────┤
│ Field Transformation:                                               │
│   - materialId: 从 receipt_confirmation 继承                       │
│   - warehouseId/storeId: 从 receipt_confirmation 继承              │
│   - quantity: 变更数量                                              │
├─────────────────────────────────────────────────────────────────────┤
│ Unit: 数量单位                                                     │
│ Identity: materialId → material_archives                            │
│ State: NORMAL / LOW / OUT_OF_STOCK                                  │
│ Event: StockInEvent, LowStockEvent                                  │
│ Consumer: OrderService, ProductionService                           │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 订单链路 (Order Chain)

```
Business Event: 订单创建
      │
      ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Write: orders                                                       │
│ Owner: OrderService                                                 │
│ Truth Source: orders                                                │
│ Write Source: POS / Management Backend                              │
├─────────────────────────────────────────────────────────────────────┤
│ Field Transformation:                                               │
│   - storeId: 从 orgContext.currentStoreId 继承                     │
│   - storeName: SNAPSHOT - 下单时快照                                │
│   - items[].foodId: 从 foods 继承                                  │
│   - items[].foodName: SNAPSHOT                                      │
│   - amount: 金额计算 (分)                                           │
├─────────────────────────────────────────────────────────────────────┤
│ Unit: 金额单位 - 分                                                 │
│ Identity: storeId → stores_new, foodId → foods                      │
│ State: order_status (0-6) / payment_status (0-3) / legacy (-1-7)   │
│ Event: OrderCreatedEvent                                            │
│ Consumer: InventoryService, FinanceService, TraceabilityService     │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼ (订单完成)
┌─────────────────────────────────────────────────────────────────────┐
│ Write: orders + finance_records + fund_flows                        │
│ Owner: OrderService + FinanceService                                │
│ Truth Source: orders, finance_records, fund_flows                   │
│ Write Source: OrderCompletedEvent → OrderCompletedEventListener     │
├─────────────────────────────────────────────────────────────────────┤
│ Field Transformation:                                               │
│   - orderId: 从 orders 继承                                        │
│   - amount: 从 orders.amount 继承                                  │
│   - accountingSubjectId: 从 accounting_subjects 继承                │
├─────────────────────────────────────────────────────────────────────┤
│ Unit: 金额单位 - 分                                                 │
│ Identity: orderId → orders, subjectId → accounting_subjects         │
│ State: order_status: 2:已完成, payment_status: 2:已支付             │
│ Event: OrderCompletedEvent                                          │
│ Consumer: FinanceReportService, TaxService                          │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.3 财务链路 (Finance Chain)

```
Business Event: 应付账款创建
      │
      ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Write: payables                                                     │
│ Owner: PayableService                                               │
│ Truth Source: payables                                              │
│ Write Source: PurchaseSettlement                                    │
├─────────────────────────────────────────────────────────────────────┤
│ Field Transformation:                                               │
│   - supplierId: 从 purchase_orders 继承                            │
│   - supplierName: SNAPSHOT - 应付单创建时快照                       │
│   - amount: 从 purchase_orders 计算                                │
├─────────────────────────────────────────────────────────────────────┤
│ Unit: 金额单位 - 分                                                 │
│ Identity: supplierId → suppliers                                    │
│ State: PENDING / PARTIAL / SETTLED / OVERDUE                        │
│ Event: PayableCreatedEvent                                          │
│ Consumer: PaymentService                                            │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼ (付款登记)
┌─────────────────────────────────────────────────────────────────────┐
│ Write: payment + bank_accounts + fund_flows                         │
│ Owner: PaymentService                                               │
│ Truth Source: payment, bank_accounts, fund_flows                    │
│ Write Source: PaymentDialog                                         │
├─────────────────────────────────────────────────────────────────────┤
│ Field Transformation:                                               │
│   - payableId: Number(payable.id) string→number                    │
│   - supplierId: 从 payable 重新查询 supplier                       │
│   - paymentMethod: 从 supplier.settlementMethod 推导               │
│   - bankAccountId: 用户选择                                         │
│   - paymentAmount: 用户输入                                         │
├─────────────────────────────────────────────────────────────────────┤
│ Unit: 金额单位 - 分                                                 │
│ Identity: payableId → payables, supplierId → suppliers              │
│ State: PENDING → CONFIRMED / REJECTED                               │
│ Event: PaymentCreatedEvent, PaymentCompletedEvent                   │
│ Consumer: PayableService, FinanceReportService                      │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼ (凭证生成)
┌─────────────────────────────────────────────────────────────────────┐
│ Write: finance_vouchers                                             │
│ Owner: VoucherService                                               │
│ Truth Source: finance_vouchers                                      │
│ Write Source: SalaryPaidEventListener / StoreDailySettlementListener│
├─────────────────────────────────────────────────────────────────────┤
│ Field Transformation:                                               │
│   - voucherType: 用户输入/系统生成                                  │
│   - voucherDate: 用户输入/系统默认                                  │
│   - entries[].subjectId: 从 accounting_subjects 继承                │
│   - entries[].debitAmount/creditAmount: 金额计算                    │
├─────────────────────────────────────────────────────────────────────┤
│ Unit: 金额单位 - 分 (后端 0-3 vs 前端 1-4)                         │
│ Identity: subjectId → accounting_subjects                           │
│ State: 0:DRAFT → 1:APPROVED → 2:POSTED → 3:VOID                   │
│ Event: VoucherCreatedEvent, VoucherPostedEvent                      │
│ Consumer: FinanceReportService, TaxService                          │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.4 溯源链路 (Traceability Chain)

```
Business Event: 追溯码生成
      │
      ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Write: material_trace_code                                          │
│ Owner: TraceabilityService                                          │
│ Truth Source: material_trace_code                                   │
│ Write Source: PurchaseStockInEventListener / ReceiptConfirmationListener│
├─────────────────────────────────────────────────────────────────────┤
│ Field Transformation:                                               │
│   - materialId: 从 purchase_arrivals/receipt_confirmation 继承     │
│   - supplierId: 从 purchase_orders 继承                            │
│   - batchNo: 从 receipt_confirmation 继承                          │
├─────────────────────────────────────────────────────────────────────┤
│ Unit: N/A                                                           │
│ Identity: materialId → material_archives, supplierId → suppliers    │
│ State: ACTIVE / USED / EXPIRED                                      │
│ Event: TraceCodeGeneratedEvent                                      │
│ Consumer: TraceabilityService                                       │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼ (成品追溯码)
┌─────────────────────────────────────────────────────────────────────┐
│ Write: food_trace_code                                              │
│ Owner: TraceabilityService                                          │
│ Truth Source: food_trace_code                                       │
│ Write Source: 订单完成时生成                                        │
├─────────────────────────────────────────────────────────────────────┤
│ Field Transformation:                                               │
│   - foodId: 从 orders.items[].foodId 继承                          │
│   - materialTraceCodeId: 从 material_trace_code 继承               │
├─────────────────────────────────────────────────────────────────────┤
│ Unit: N/A                                                           │
│ Identity: foodId → foods, materialTraceCodeId → material_trace_code │
│ State: ACTIVE / USED                                                │
│ Event: TraceCodeGeneratedEvent                                      │
│ Consumer: TraceabilityService, Consumer                             │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.5 HR链路 (HR Chain)

```
Business Event: 员工创建
      │
      ▼
┌─────────────────────────────────────────────────────────────────────┐
│ Write: employees                                                    │
│ Owner: EmployeeService                                              │
│ Truth Source: employees                                             │
│ Write Source: EmployeeFormDialog                                    │
├─────────────────────────────────────────────────────────────────────┤
│ Field Transformation:                                               │
│   - name/employeeNo: 用户输入                                      │
│   - departmentId: 从 departments 继承                               │
│   - positionId: 从 positions 继承                                   │
│   - storeId/warehouseId: 按工作地点类型选择                         │
│   - salary: 用户输入                                                │
├─────────────────────────────────────────────────────────────────────┤
│ Unit: 薪资单位 - 元                                                 │
│ Identity: departmentId → departments, positionId → positions        │
│ State: active / inactive                                            │
│ Event: EmployeeCreatedEvent                                         │
│ Consumer: HREventListener, AttendanceService                        │
└─────────────────────────────────────────────────────────────────────┘
      │
      ▼ (HR事件监听)
┌─────────────────────────────────────────────────────────────────────┐
│ Write: employees (更新员工计数)                                      │
│ Owner: HREventListener                                              │
│ Truth Source: employees                                             │
│ Write Source: EmployeeCreatedEvent / EmployeeDeletedEvent           │
├─────────────────────────────────────────────────────────────────────┤
│ Field Transformation:                                               │
│   - departmentId: 从 event 继承                                    │
│   - employeeCount: 计数更新                                         │
├─────────────────────────────────────────────────────────────────────┤
│ Unit: 计数单位                                                     │
│ Identity: departmentId → departments                                │
│ State: N/A                                                          │
│ Event: HREventListener                                              │
│ Consumer: DepartmentService                                         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 三、事件驱动数据流

### 3.1 事件流转矩阵

| 事件 | 触发源 | 监听器 | 下游操作 | 涉及表 |
|------|--------|--------|----------|--------|
| OrderCreatedEvent | OrderService | OrderCreatedEventListener | 锁定原料 | inventory, material_trace_code |
| OrderCompletedEvent | OrderService | OrderCompletedEventListener | 记录财务收入 | finance_records, fund_flows |
| OrderRefundEvent | OrderService | OrderRefundEventListener | 生成退款支出 | finance_records, fund_flows |
| PurchaseStockInEvent | InventoryService | PurchaseStockInEventListener | 生成追溯码 | material_trace_code |
| ReceiptConfirmationCompletedEvent | ReceiptConfirmationService | ReceiptConfirmationEventListener | 生成追溯码 | material_trace_code |
| StoreDailySettlementCompletedEvent | StoreSettlementService | StoreDailySettlementEventListener | 生成凭证 | finance_vouchers |
| SalaryPaidEvent | SalaryService | SalaryPaidEventListener | 生成凭证 | finance_vouchers |
| InvoiceReimbursementApprovedEvent | InvoiceReimbursementService | InvoiceReimbursementApprovedEventListener | 生成凭证 | finance_vouchers |
| VoucherPostedEvent | VoucherService | - | 更新科目余额 | accounting_subjects |
| PaymentCompletedEvent | PaymentService | - | 更新应付状态 | payables |
| BudgetExceededEvent | BudgetCheckScheduler | - | 预警通知 | - |

### 3.2 事件流转详细图

```
订单领域:
  OrderCreatedEvent ──► OrderCreatedEventListener ──► 锁定原料
    └──► inventory (减库存) + material_trace_code (生成追溯码)

  OrderCompletedEvent ──► OrderCompletedEventListener ──► 记录财务收入
    └──► finance_records (收入记录) + fund_flows (资金流入)

  OrderRefundEvent ──► OrderRefundEventListener ──► 生成退款支出
    └──► finance_records (退款记录) + fund_flows (资金流出)

采购/库存领域:
  PurchaseStockInEvent ──► PurchaseStockInEventListener ──► 生成追溯码
    └──► material_trace_code (原材料追溯码)

  ReceiptConfirmationCompletedEvent ──► ReceiptConfirmationEventListener ──► 生成追溯码
    └──► material_trace_code (原材料追溯码)

门店/薪资领域:
  StoreDailySettlementCompletedEvent ──► StoreDailySettlementEventListener ──► 生成凭证
    └──► finance_vouchers (门店日结凭证)

  SalaryPaidEvent ──► SalaryPaidEventListener ──► 生成凭证
    └──► finance_vouchers (薪资发放凭证)

财务报销领域:
  InvoiceReimbursementApprovedEvent ──► InvoiceReimbursementApprovedEventListener ──► 生成凭证
    └──► finance_vouchers (报销凭证)

财务领域:
  VoucherPostedEvent ──► 更新 accounting_subjects.balance
  PaymentCompletedEvent ──► 更新 payables.status
```

---

## 四、数据流治理建议

### 4.1 高风险数据流

| 数据流 | 风险描述 | 严重程度 | 缓解措施 |
|--------|----------|----------|----------|
| 双写 food + foods | 下单/退款/超时同时写两表 | 🔴 P0 | 同一事务写入 |
| 双写 inventory + store_inventory | 采购入库/调拨双写 | 🟡 P1 | 确保同一事务 |
| 金额单位跨表 | 新表分，legacy元 | 🔴 P0 | 全链路统一单位 |
| 订单状态三套并存 | 状态流转逻辑分散 | 🔴 P0 | 统一状态机 |

### 4.2 中风险数据流

| 数据流 | 风险描述 | 严重程度 | 缓解措施 |
|--------|----------|----------|----------|
| PaymentDialog re-query | 页面打开后供应商信息可能变化 | 🟡 P1 | 可接受，保证最新状态 |
| 凭证前后端状态码错位 | 后端0-3 vs 前端1-4 | 🟡 P1 | 已在adapter层封装 |
| 成本双写 | persistOrderCost (主事务) + recordOrderCost (异步) | 🟡 P1 | 监控异步任务成功率 |

---

## 五、数据流治理建议总结

| # | 建议 | 优先级 | 涉及链路 |
|---|------|--------|----------|
| 1 | 完成 food/foods 双写清理，废弃 food 表写入 | P0 | 订单链路 |
| 2 | 确保 inventory/store_inventory 双写在同一事务 | P0 | 采购/库存链路 |
| 3 | 统一金额单位（全链路分），修正 tax_record/account_balance | P0 | 财务链路 |
| 4 | 订单状态机统一或建立明确映射表 | P0 | 订单链路 |
| 5 | 修正 product_id → material_id 列名语义 | P1 | 采购链路 |
| 6 | 为仓库信息建立独立 Canonical Source | P1 | 库存链路 |
| 7 | 凭证状态码前后端统一 | P2 | 财务链路 |

---

*生成时间: 2026-09-09*
*数据来源: 代码库静态分析 + 地图文件交叉验证*
