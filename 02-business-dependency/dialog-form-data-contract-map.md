# Dialog / Form 数据契约地图 (Dialog-Form Data Contract Map)

> **项目**: 食品溯源系统 (Food Traceability System)
> **生成时间**: 2026-09-09
> **扫描范围**: frontend/src/views/**/components/*Dialog*.vue, *Form*.vue

---

## 一、核心 Dialog/Form 清单

| # | Dialog/Form | 所在模块 | 用途 | 关联父页面 |
|---|-------------|----------|------|-----------|
| 1 | PurchaseOrderFormDialog | purchase | 采购订单新建/编辑 | PurchaseOrder.vue |
| 2 | ArrivalFormDialog | purchase | 到货单新建/编辑 | PurchaseStockin.vue |
| 3 | ReceiptConfirmationFormDialog | receipt-confirmation | 收货确认单创建 | StoreReceiving.vue |
| 4 | RequestDetailDialog | purchase | 采购申请详情 | PurchaseRequest.vue, PurchaseOrder.vue |
| 5 | PaymentDialog | finance | 付款登记 | FinancePayable.vue |
| 6 | ReceiptDialog | finance | 收款登记 | FinanceReceivable.vue |
| 7 | VoucherFormDialog | finance | 凭证录入/编辑 | FinanceVoucher.vue |
| 8 | CostFormDialog | finance | 成本表单 | FinanceCost.vue |
| 9 | BudgetFormDialog | finance | 预算表单 | FinanceBudget.vue |
| 10 | EmployeeFormDialog | hr | 员工新建/编辑 | EmployeeList.vue |
| 11 | EmployeeTransferDialog | hr | 员工调岗 | EmployeeList.vue |
| 12 | OrgFormDialog | hr | 组织新建/编辑 | OrgManagement.vue |
| 13 | PositionFormDialog | hr | 岗位新建/编辑 | PositionList.vue |
| 14 | DishComboDialog | product | 套餐组合管理 | DishCombo.vue |
| 15 | CategoryDialog | product | 分类管理 | CategoryManagement.vue |
| 16 | PricingDialog | product | 菜品定价 | DishPricing.vue |
| 17 | SealFormDialog | seal | 签章管理 | SealManagement.vue |
| 18 | MemberDetailDialog | marketing | 会员详情 | MemberList.vue |
| 19 | SignConfirmDialog | purchase | 电子合同签署确认 | ElectronicContract.vue |
| 20 | HandheldEntryDialog | receipt-confirmation | 手持收货录入 | ReceivingMobile.vue |

---

## 二、核心 Dialog 数据契约详细分析

### 2.1 PurchaseOrderFormDialog — 采购订单表单

| 维度 | 详情 |
|------|------|
| **文件** | `views/purchase/components/PurchaseOrderFormDialog.vue` |
| **Parent Page** | PurchaseOrder.vue |
| **Open Trigger** | 父组件调用 `dialogRef.create(initialData?)` 或 `dialogRef.edit(id)` |
| **Inherited IDs** | 无（通过 initialData 传入） |
| **Inherited Values** | supplierId, supplierName, expectedDate, remark, items[]（可选从申请转单） |
| **Derived Values** | filteredMaterialOptions（按供应商过滤）, itemsTotalAmount（明细合计） |
| **API Prefill** | edit模式: `purchaseOrderApi.getById(id)` 加载详情 |
| **Defaults** | 无显式默认值（全部由 initialData 或用户输入） |
| **Editable Fields** | supplierId, expectedDate, remark, items[].materialId, quantity, unitPrice, specification, unit, remark |
| **Readonly Fields** | selectedSupplier.paymentTerms, selectedSupplier.settlementMethod, items[].amount（计算值） |
| **Hidden Fields** | 无 |
| **Submit Payload** | `{ supplierId, supplierName, expectedDate, remark, items[] }` |
| **Post-submit Refresh** | emit('success') → 父组件重新加载列表 |
| **Downstream Effects** | 后端可能按供应商自动拆分为多张订单 |

**关键数据契约**:
```
Props: { supplierOptions: SupplierInfo[] }
Expose: { create(initialData?), edit(id) }
Emits: { success: [] }
```

**⚠️ 特殊逻辑**: 供应商切换时的跨供应商选料检测 — 未开启允许跨供应商时，切换供应商会强制改回与明细一致。

---

### 2.2 ArrivalFormDialog — 到货单表单

| 维度 | 详情 |
|------|------|
| **文件** | `views/purchase/components/ArrivalFormDialog.vue` |
| **Parent Page** | PurchaseStockin.vue |
| **Open Trigger** | v-model 双向绑定 + props.mode |
| **Inherited IDs** | orderId（从父组件传入） |
| **Inherited Values** | 无（仅传 orderId，不传数据） |
| **Derived Values** | selectedOrder（根据 orderId 从 orderOptions 查找） |
| **API Prefill** | 无（orderOptions 由父组件加载） |
| **Defaults** | mode=create时: orderId=props.initialOrderId |
| **Editable Fields** | orderId, shipmentStatus, logisticsNo, logisticsCompany, transportMode, vehiclePlateNo, vehicleType, driverName, driverPhone, freightAmount, estimatedArrivalDate, remark |
| **Readonly Fields** | 仅edit模式: selectedOrder.orderNo, selectedOrder.supplierName |
| **Hidden Fields** | 无 |
| **Submit Payload** | create: `PurchaseArrivalFormData` / edit: `PurchaseArrivalUpdateForm`（仅物流字段） |
| **Post-submit Refresh** | emit('create'/'update') → 父组件处理 |
| **Downstream Effects** | 创建到货单后可发起收货确认 |

**关键数据契约**:
```
Props: { modelValue, mode, initialData?, initialOrderId?, orderOptions, orderOptionsLoading }
Emits: { 'update:modelValue', create: [PurchaseArrivalFormData], update: [PurchaseArrivalUpdateForm] }
```

**⚠️ 特殊功能**: 智能粘贴识别 — 自由文本自动解析为物流字段（parseLogisticsText）

---

### 2.3 ReceiptConfirmationFormDialog — 收货确认表单

| 维度 | 详情 |
|------|------|
| **文件** | `components/business/receipt-confirmation/ReceiptConfirmationFormDialog.vue` |
| **Parent Page** | StoreReceiving.vue |
| **Open Trigger** | v-model 双向绑定 |
| **Inherited IDs** | arrivalId（从 arrival 继承） |
| **Inherited Values** | arrival.items[] → formData.items[]（逐项继承） |
| **Derived Values** | totalConfirmed, totalRejected, totalAmount, partialCount |
| **API Prefill** | 无（数据从 props.arrival 直接继承） |
| **Defaults** | receiverType=props.receiverType, items[].accept='receive', quantity=expected-received |
| **Editable Fields** | items[].accept, items[].quantity, batchNo, productionDate, expiryDate, qualityCheckResult, qualityRemark, remark |
| **Readonly Fields** | items[].materialName, specification, unit, expectedQuantity, receivedQuantity, unitPrice |
| **Hidden Fields** | storeId, warehouseId（从 arrival 继承但不在表单中） |
| **Submit Payload** | `{ arrivalId, receiverType, storeId, warehouseId, items[], qualityCheckResult, ... }` |
| **Post-submit Refresh** | emit('success') → 父组件刷新 |
| **Downstream Effects** | 触发 `ReceiptConfirmationCompletedEvent` → 生成追溯码 + 库存变更 |

**关键数据契约**:
```
Props: { modelValue, arrival: PurchaseArrivalInfo, receiverType: ReceiverType }
Emits: { 'update:modelValue', success: [] }
```

**⚠️ Hidden Inheritance**: storeId 和 warehouseId 从 arrival 对象继承，但不在表单 UI 中显示，直接随提交数据发送到后端。

---

### 2.4 PaymentDialog — 付款登记表单

| 维度 | 详情 |
|------|------|
| **文件** | `views/finance/components/PaymentDialog.vue` |
| **Parent Page** | FinancePayable.vue |
| **Open Trigger** | v-model:visible 双向绑定 |
| **Inherited IDs** | payableId = Number(payable.id)（string→number 转换） |
| **Inherited Values** | supplierName, payableNo, orderNo, stockinNo, invoiceNo, dueDate, aging, amount, remainAmount |
| **Derived Values** | remainYuan（remainAmount元）, amountYuan（amount元）, needBankAccount, isModeA, needBankInfo |
| **API Prefill** | `supplierApi.getById(payable.supplierId)` → 推导 paymentMethod |
| | `bankAccountApi.getList()` → 银行账户列表 |
| **Defaults** | payAmount=remainYuan, paymentMethod=从供应商推导, paymentDate=当前日期 |
| **Editable Fields** | payAmount, paymentMethod, bankAccountId, paymentDate, remark, bankTransactionNo(A), bankTransactionTime(A), voucherType(A), voucherUrl(A) |
| **Readonly Fields** | payableNo, supplierName, orderNo, stockinNo, invoiceNo, dueDate, aging, amount, remainAmount |
| **Hidden Fields** | A模式字段（bankTransactionNo等）通过 v-if 控制 |
| **Submit Payload** | `PaymentFormData { payableId, paymentAmount(分), paymentMethod, bankAccountId(Number), paymentDate, remark, bankTransactionNo?, ... }` |
| **Post-submit Refresh** | emit('success') → 父组件刷新列表 |
| **Downstream Effects** | 触发四账联动：应付 + 银行账户 + 资金流水 + 会计凭证 |

**关键数据契约**:
```
Props: { visible, payable: FinancePayable, paymentMode?: 'A' | 'B' }
Emits: { 'update:visible', success: [] }
Amount Unit: 显示=元, API=分 (yuanToFen 转换)
```

**⚠️ Re-query Pattern**: 打开 Dialog 时重新查询供应商详情以推导付款方式，而非直接从 payable 对象继承。这保证了最新状态，但增加了 API 调用。

---

### 2.5 ReceiptDialog — 收款登记表单

| 维度 | 详情 |
|------|------|
| **文件** | `views/finance/components/ReceiptDialog.vue` |
| **Parent Page** | FinanceReceivable.vue |
| **Open Trigger** | v-model:visible 双向绑定 |
| **Inherited IDs** | receivableId = receivable.id（直接使用） |
| **Inherited Values** | customerName, receivableNo, orderNo, amount, remainAmount |
| **Derived Values** | remainYuan, needBankAccount |
| **API Prefill** | `bankAccountApi.getList()` → 银行账户列表 |
| **Defaults** | receiveAmount=remainYuan, receiptMethod='bank_transfer', receiptDate=当前日期 |
| **Editable Fields** | receiveAmount, receiptMethod, bankAccountId, receiptDate, remark |
| **Readonly Fields** | receivableNo, customerName, orderNo, amount, remainAmount |
| **Hidden Fields** | 无 |
| **Submit Payload** | `ReceiptFormData { receivableId, receiptAmount(分), receiptMethod, bankAccountId, receiptDate, remark }` |
| **Post-submit Refresh** | emit('success') → 父组件刷新列表 |
| **Downstream Effects** | 触发四账联动：应收 + 银行账户 + 资金流水 + 会计凭证 |

**关键数据契约**:
```
Props: { visible, receivable: FinanceReceivable }
Emits: { 'update:visible', success: [] }
Amount Unit: 显示=元, API=分 (yuanToFen 转换)
```

---

### 2.6 VoucherFormDialog — 凭证表单

| 维度 | 详情 |
|------|------|
| **文件** | `views/finance/components/VoucherFormDialog.vue` |
| **Parent Page** | FinanceVoucher.vue |
| **Open Trigger** | v-model 双向绑定 + props.voucherId |
| **Inherited IDs** | voucherId（编辑模式） |
| **Inherited Values** | 编辑模式: 全量从 API 加载 |
| **Derived Values** | debitTotal, creditTotal, balanceDiff, isBalanced |
| **API Prefill** | 编辑: `voucherApi.getById(id)` → fenToYuanNumber 转换 |
| | `subjectApi.getLeafSubjects()` → 科目列表 |
| **Defaults** | voucherType='general', voucherDate=当前日期, attachmentCount=1 |
| **Editable Fields** | voucherType, voucherDate, summary, attachmentCount, entries[].subjectId, debitAmount, creditAmount, summary, remark |
| **Readonly Fields** | 无（全部可编辑） |
| **Hidden Fields** | 无 |
| **Submit Payload** | `VoucherFormData { voucherType, voucherDate, summary, attachmentCount, entries[], remark }` — 金额: yuanToFen 转换 |
| **Post-submit Refresh** | emit('success') → 父组件刷新 |
| **Downstream Effects** | 凭证保存后可审批、过账 |

**关键数据契约**:
```
Props: { modelValue, voucherId?: string | null }
Emits: { 'update:modelValue', success: [] }
Amount Unit: 显示=元, API=分 (yuanToFen/fenToYuanNumber)
```

**⚠️ 金额双转换**: 编辑模式加载时 fen→元，提交时元→分。需确保全链路精度一致。

---

### 2.7 EmployeeFormDialog — 员工表单

| 维度 | 详情 |
|------|------|
| **文件** | `views/hr/components/EmployeeFormDialog.vue` |
| **Parent Page** | EmployeeList.vue |
| **Open Trigger** | v-model 双向绑定 + props.editData |
| **Inherited IDs** | 无（新增模式），editData.id（编辑模式） |
| **Inherited Values** | 编辑模式: 全量从 editData 继承 |
| **Derived Values** | filteredPositionOptions（按部门过滤）, isEditMode, dialogTitle, showStoreSelect, showWarehouseSelect |
| **API Prefill** | `storeArchiveApi` → 门店选项 |
| | `warehouseApi` → 仓库选项 |
| | `positionApi` → 岗位选项 |
| | `useDepartmentOptions` → 部门选项 |
| **Defaults** | status='active', employmentType='full_time', workLocationType='HEADQUARTERS', gender='other' |
| **Editable Fields** | name, employeeNo, gender, departmentId, positionId, phone, email, entryDate, birthday, education, emergencyContact, status, salary, employmentType, retirementDate, reemploymentDate, agreementNo, workLocationType, storeId, warehouseId |
| **Readonly Fields** | 无（全部可编辑） |
| **Hidden Fields** | storeId（仅workLocationType=STORE时显示）, warehouseId（仅workLocationType=WAREHOUSE时显示）, retirementDate/reemploymentDate/agreementNo（仅over_age时显示） |
| **Submit Payload** | `Employee { 全量字段 }` |
| **Post-submit Refresh** | submitHandler props 回调 → 父组件处理 |
| **Downstream Effects** | 触发 EmployeeCreatedEvent/EmployeeUpdatedEvent → HR事件监听 |

**关键数据契约**:
```
Props: { modelValue, editData: Employee | null, submitHandler?: (data) => Promise<void> }
Emits: { 'update:modelValue', submit: [Employee], close: [] }
```

---

### 2.8 ReceiptConfirmationFormDialog — 收货确认

（详见 2.3 节）

---

## 三、数据继承模式分类

### 3.1 Pattern A: Props 直接继承

父组件将数据通过 props 直接传入 Dialog，Dialog 初始化时复制到 formData。

```
父组件: <Dialog :data="selectedRow" />
Dialog: formData = { ...props.data }
```

**适用场景**: 编辑模式、详情展示
**示例**: EmployeeFormDialog(editData), PaymentDialog(payable), ReceiptDialog(receivable)

### 3.2 Pattern B: ID 继承 + API 重查

父组件只传入 ID，Dialog 通过 API 重新查询完整数据。

```
父组件: <Dialog :id="selectedRow.id" />
Dialog: const detail = await api.getById(props.id)
```

**适用场景**: 数据量大、需要最新状态
**示例**: PurchaseOrderFormDialog.edit(id), VoucherFormDialog(voucherId), RequestDetailDialog(bizId)

### 3.3 Pattern C: 初始值继承

父组件传入初始值，Dialog 在此基础上允许用户修改。

```
父组件: <Dialog :initial-data="{ supplierId: row.supplierId }" />
Dialog: Object.assign(formData, props.initialData)
```

**适用场景**: 从上游单据转单
**示例**: PurchaseOrderFormDialog.create(initialData), ArrivalFormDialog(initialOrderId)

### 3.4 Pattern D: Store 上下文继承

Dialog 从 Pinia Store 继承全局上下文（如当前门店、当前用户）。

```
Dialog: const orgContext = useOrgContextStore()
        formData.storeId = orgContext.currentStoreId
```

**适用场景**: 门店级数据操作
**示例**: 部分 HR 操作、库存操作

---

## 四、数据契约问题清单

### 4.1 Re-query Pattern (重新查询模式)

| Dialog | 重新查询内容 | 触发条件 | 风险 |
|--------|-------------|----------|------|
| PaymentDialog | 供应商详情 | 打开时 | 供应商信息可能已变更 |
| PurchaseOrderFormDialog | 物料档案列表 | 挂载时 | 物料列表可能已更新 |
| VoucherFormDialog | 凭证详情 | 编辑模式打开时 | 凭证可能已被审批 |
| RequestDetailDialog | 采购申请详情 | 打开时 | 申请状态可能已变更 |

**评估**: 这些重新查询是合理的 — 保证了 Dialog 操作时数据的最新性。但需注意：
1. 查询失败时的降级处理
2. 查询延迟导致的用户体验问题

### 4.2 金额单位转换 (Yuan/Fen Conversion)

| Dialog | 转换方向 | 转换函数 | 风险 |
|--------|---------|----------|------|
| PaymentDialog | 元→分 | `yuanToFen()` | 🟡 精度损失风险 |
| ReceiptDialog | 元→分 | `yuanToFen()` | 🟡 精度损失风险 |
| VoucherFormDialog | 分→元(加载) + 元→分(提交) | `fenToYuanNumber()` + `yuanToFen()` | 🟡 双重转换 |

**评估**: 全链路金额单位需保持一致。truth-conflict-map.md 已记录冲突1（金额单位双轨）。

### 4.3 Hidden Inheritance (隐藏继承)

| Dialog | 隐藏字段 | 继承来源 | 风险 |
|--------|---------|----------|------|
| ReceiptConfirmationFormDialog | storeId, warehouseId | arrival 对象 | 🟡 中 |
| PaymentDialog | A模式字段 | 条件控制 | 🟢 低 |

**评估**: Hidden Inheritance 容易被开发者忽略，建议在代码注释中明确标注。

### 4.4 缺失继承 (Missing Inheritance)

| 场景 | 期望继承 | 实际行为 | 建议 |
|------|---------|----------|------|
| 到货单→收货确认 | 物流信息 | 不继承 | 可接受，物流信息在到货单已记录 |
| 付款→再次付款 | 上次银行账户 | 不继承 | 可考虑继承默认账户 |
| 凭证编辑→新增 | 上次凭证类型 | 不继承 | 全新表单 |

---

## 五、Dialog 依赖关系图

```
┌─────────────────────────────────────────────────────────────────┐
│                    采购模块 Dialog 依赖                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  RequestDetailDialog ──── (只读详情，无下游)                      │
│                                                                 │
│  PurchaseOrderFormDialog ──→ (创建/编辑采购订单)                  │
│       ↑ initialData                                           │
│       │ (从采购申请转单)                                        │
│  RequestDetailDialog + 转单逻辑                                │
│                                                                 │
│  ArrivalFormDialog ──→ (创建/编辑到货单)                         │
│       ↑ initialOrderId                                        │
│  PurchaseOrderList                                              │
│                                                                 │
│  ReceiptConfirmationFormDialog ──→ (收货确认)                    │
│       ↑ arrival                                                │
│  StoreReceiving / ArrivaDetailDialog                            │
│                                                                 │
│  CrossSupplierTipDialog ──→ (供应商不一致提示)                   │
│       ↑ (被 PurchaseOrderFormDialog 内部调用)                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    财务模块 Dialog 依赖                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  PaymentDialog ──→ (付款登记)                                    │
│       ↑ payable                                                │
│  FinancePayableList                                             │
│                                                                 │
│  ReceiptDialog ──→ (收款登记)                                    │
│       ↑ receivable                                             │
│  FinanceReceivableList                                          │
│                                                                 │
│  VoucherFormDialog ──→ (凭证录入/编辑)                           │
│       ↑ voucherId                                              │
│  FinanceVoucherList                                             │
│                                                                 │
│  PaymentHistoryDialog ──→ (付款历史)                             │
│  ReceiptHistoryDialog ──→ (收款历史)                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    HR 模块 Dialog 依赖                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  EmployeeFormDialog ──→ (员工新建/编辑)                          │
│       ↑ editData                                               │
│  EmployeeList                                                   │
│                                                                 │
│  EmployeeTransferDialog ──→ (员工调岗)                           │
│       ↑ employee                                               │
│  EmployeeList                                                   │
│                                                                 │
│  OrgFormDialog ──→ (组织新建/编辑)                               │
│  PositionFormDialog ──→ (岗位新建/编辑)                          │
│                                                                 │
│  ContractCreateDialog / ContractChangeDialog / ...              │
│       ↑ (合同系列 Dialog，独立体系)                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 六、表单校验契约

### 6.1 Required Fields 校验规则

| Dialog | 字段 | 校验规则 | 触发方式 |
|--------|------|----------|----------|
| PurchaseOrderFormDialog | supplierId | required | change |
| PurchaseOrderFormDialog | expectedDate | required | change |
| PaymentDialog | payAmount | required + max=remainAmount | blur |
| PaymentDialog | bankAccountId | conditional required | change |
| PaymentDialog | paymentDate | required | change |
| ReceiptDialog | receiveAmount | required + max=remainAmount | blur |
| ReceiptDialog | bankAccountId | conditional required | change |
| EmployeeFormDialog | name | required + min=2, max=20 | blur |
| EmployeeFormDialog | employeeNo | required + pattern | blur |
| EmployeeFormDialog | departmentId | required | change |
| EmployeeFormDialog | positionId | required | change |
| EmployeeFormDialog | salary | required + range | blur |

### 6.2 Cross-field Validation

| Dialog | 校验逻辑 | 触发条件 |
|--------|---------|----------|
| PurchaseOrderFormDialog | 至少一条有效明细 | 提交时 |
| ReceiptConfirmationFormDialog | 数量 > 0 且 ≤ 剩余可收 | 提交时 |
| ReceiptConfirmationFormDialog | 拒收必须填写备注 | 提交时 |
| VoucherFormDialog | 借贷平衡 (debitTotal = creditTotal) | 实时计算 |

---

*生成时间: 2026-09-09*
*数据来源: 代码库静态分析*
