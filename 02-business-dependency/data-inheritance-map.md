# 数据继承关系地图 (Data Inheritance Map)

> **项目**: 食品溯源系统 (Food Traceability System)
> **生成时间**: 2026-09-09
> **数据来源**: 代码库静态分析 + 地图文件交叉验证

---

## 一、数据继承链路总览

```
Page → Row (List Selection) → Route Params/Query → Props → Store → Dialog/Form → API Payload → Service → DB
                                                                                                      ↓
                                                                                               Event → Listener → Downstream
```

### 继承层级说明

| 层级 | 继承载体 | 数据传递方式 | 典型场景 |
|------|----------|-------------|----------|
| L1 Page | 列表页组件 | 用户选择行 | PurchaseOrder.vue 选中一行采购单 |
| L2 Route | `this.$route.params/query` | URL 参数 | `/purchase/orders/:id` |
| L3 Props | `defineProps` | 父子组件传参 | `<PaymentDialog :payable="row" />` |
| L4 Store | Pinia Store | 全局状态 | `useOrgContextStore().currentStoreId` |
| L5 Dialog/Form | `reactive/ref` | 表单状态 | `formData.supplierId` |
| L6 API Payload | `yuanToFen()` + Converter | 数据转换 | 元→分, string→number |
| L7 Service | `@Service` | 业务逻辑 | `PaymentService.register()` |
| L8 DB | `INSERT/UPDATE` | 持久化 | `payment` 表 |

---

## 二、核心继承链路详细分析

### 2.1 采购链路继承关系

#### 链路 1: 采购申请 → 采购订单

```
PurchaseRequest.vue (列表页)
  → 选中行 (PurchaseRequest)
    → props.bizId (RequestDetailDialog)
      → purchaseRequestApi.getById(bizId)  ← ⚠️ 重新查询，非继承
        → 展示详情
          → 转单操作
            → PurchaseOrderFormDialog.create(initialData)
              → Object.assign(formData, initialData)  ← Explicit Inheritance
                → 提交 purchaseOrderApi.create(submitData)
```

| 继承字段 | 来源 | 类型 | 继承方式 | 备注 |
|----------|------|------|----------|------|
| supplierId | RequestItem → OrderForm | REFERENCE | Explicit | 从申请明细的物料主供应商继承 |
| items[].materialId | RequestItem → OrderItem | REFERENCE | Explicit | 直接复制物料ID |
| items[].quantity | RequestItem → OrderItem | COPY | Explicit | 数量可编辑 |
| expectedDate | Request → OrderForm | COPY | Explicit | 可修改 |
| remark | Request → OrderForm | COPY | Explicit | 可修改 |

#### 链路 2: 采购订单 → 到货单

```
PurchaseOrder.vue (列表页)
  → 选中行 (PurchaseOrderInfo)
    → props: { initialOrderId, orderOptions }
      → ArrivalFormDialog
        → watch(modelValue) 初始化 formData
          → formData.orderId = props.initialOrderId  ← Explicit Inheritance
            → 提交 emit('create', formData)
```

| 继承字段 | 来源 | 类型 | 继承方式 | 备注 |
|----------|------|------|----------|------|
| orderId | OrderList → ArrivalForm | REFERENCE | Explicit | 仅传ID，不传数据 |
| supplierName | OrderList → ArrivalForm | DISPLAY | Explicit | 展示用，不提交 |
| totalAmount | OrderList → ArrivalForm | DISPLAY | Explicit | 展示用，不提交 |
| 物流信息 | 用户输入 | NEW | N/A | 全部新建 |

#### 链路 3: 到货单 → 收货确认

```
StoreReceiving.vue (到货列表)
  → 选中到货单 (PurchaseArrivalInfo)
    → props.arrival
      → ReceiptConfirmationFormDialog
        → resetForm() 从 arrival 继承
          → formData.arrivalId = arrival.arrivalId  ← Explicit
          → formData.items = arrival.items.map(buildInitialItem)  ← Explicit
            → 每项: expectedQuantity, receivedQuantity, materialId 等
              → 提交 receiptConfirmationApi.create(submitData)
```

| 继承字段 | 来源 | 类型 | 继承方式 | 备注 |
|----------|------|------|----------|------|
| arrivalId | Arrival → ConfirmForm | REFERENCE | Explicit | 必须继承 |
| items[].materialId | ArrivalItem → ConfirmItem | REFERENCE | Explicit | 逐项继承 |
| items[].expectedQuantity | ArrivalItem → ConfirmItem | COPY | Explicit | 只读展示 |
| items[].receivedQuantity | ArrivalItem → ConfirmItem | COPY | Explicit | 只读展示 |
| items[].unitPrice | ArrivalItem → ConfirmItem | COPY | Explicit | 只读展示 |
| storeId | Arrival → ConfirmForm | REFERENCE | Implicit | 通过 receiverType 推断 |
| warehouseId | Arrival → ConfirmForm | REFERENCE | Implicit | 通过 receiverType 推断 |
| batchNo | 用户输入 | NEW | N/A | 整单批次 |
| productionDate | 用户输入 | NEW | N/A | 整单生产日期 |
| expiryDate | 用户输入 | NEW | N/A | 整单有效期 |

---

### 2.2 财务链路继承关系

#### 链路 4: 应付账款 → 付款登记

```
FinancePayable.vue (列表页)
  → 选中行 (FinancePayable)
    → props: { payable, visible }
      → PaymentDialog
        → watch(visible) 初始化
          → resetForm(): formData.payAmount = remainYuan  ← Implicit
          → loadSupplierInfo(): 根据 payable.supplierId 查询供应商  ← ⚠️ 重新查询
            → supplierApi.getById(payable.supplierId)
              → mapSettlementToPaymentMethod(supplier.settlementMethod)
                → initialPaymentMethod.value = paymentMethod
          → 提交 paymentApi.register(submitData)
```

| 继承字段 | 来源 | 类型 | 继承方式 | 备注 |
|----------|------|------|----------|------|
| payableId | PayableList → PaymentForm | REFERENCE | Explicit | `Number(payable.id)` string→number |
| supplierName | Payable → PaymentForm | DISPLAY | Explicit | 只读展示 |
| payableNo | Payable → PaymentForm | DISPLAY | Explicit | 只读展示 |
| remainAmount | Payable → PaymentForm | SNAPSHOT | Implicit | 作为默认付款金额 |
| amount | Payable → PaymentForm | DISPLAY | Explicit | 只读展示 |
| paymentMethod | Supplier → PaymentForm | DERIVED | ⚠️ Re-query | 从供应商结算方式推导 |
| bankAccountId | 用户选择 | NEW | N/A | 用户手动选择 |
| paymentAmount | 用户输入 | EDITABLE | N/A | 可修改，不能超过 remainAmount |

**⚠️ 关键发现**: PaymentDialog 对 supplierId 的处理是「页面显示 supplierName → Dialog 通过 supplierId 重新查询 supplier 详情 → 推导 paymentMethod」。这是一个典型的 **Re-query Pattern**，可能导致：
1. 供应商信息在页面打开后被修改，Dialog 查询到的是最新状态而非打开时的状态
2. 增加了一次 API 调用，但保证了 paymentMethod 的准确性

#### 链路 5: 应收账款 → 收款登记

```
FinanceReceivable.vue (列表页)
  → 选中行 (FinanceReceivable)
    → props: { receivable, visible }
      → ReceiptDialog
        → watch(visible) 初始化
          → resetForm(): formData.receiveAmount = remainYuan  ← Implicit
          → 提交 receiptApi.register(submitData)
```

| 继承字段 | 来源 | 类型 | 继承方式 | 备注 |
|----------|------|------|----------|------|
| receivableId | ReceivableList → ReceiptForm | REFERENCE | Explicit | 直接使用 props.receivable.id |
| customerName | Receivable → ReceiptForm | DISPLAY | Explicit | 只读展示 |
| receivableNo | Receivable → ReceiptForm | DISPLAY | Explicit | 只读展示 |
| remainAmount | Receivable → ReceiptForm | SNAPSHOT | Implicit | 作为默认收款金额 |
| receiptAmount | 用户输入 | EDITABLE | N/A | 可修改，不能超过 remainAmount |
| bankAccountId | 用户选择 | NEW | N/A | 用户手动选择 |

#### 链路 6: 凭证录入/编辑

```
VoucherFormDialog
  → 新增模式: 无继承，全手动
  → 编辑模式: voucherApi.getById(voucherId)
    → fenToYuanNumber(e.debitAmount)  ← 分→元转换
    → fenToYuanNumber(e.creditAmount)  ← 分→元转换
      → 提交 voucherApi.create/update
```

| 继承字段 | 来源 | 类型 | 继承方式 | 备注 |
|----------|------|------|----------|------|
| voucherId | 编辑模式传入 | REFERENCE | Explicit | 通过 props 传入 |
| voucherType | API查询结果 | COPY | Explicit | 可编辑 |
| voucherDate | API查询结果 | COPY | Explicit | 可编辑 |
| entries[].subjectId | API查询结果 | REFERENCE | Explicit | 可编辑 |
| entries[].debitAmount | API查询结果 | SNAPSHOT | ⚠️ 分→元 | 显示为元，提交为分 |
| entries[].creditAmount | API查询结果 | SNAPSHOT | ⚠️ 分→元 | 显示为元，提交为分 |

---

### 2.3 HR 链路继承关系

#### 链路 7: 员工列表 → 员工表单

```
EmployeeList.vue (列表页)
  → 选中行 (Employee)
    → props: { modelValue, editData }
      → EmployeeFormDialog
        → watch(modelValue) 初始化
          → fillEditData(props.editData)  ← Explicit Inheritance
            → formData.name = data.name
            → formData.employeeNo = data.employeeNo
            → ... (全部字段逐个赋值)
              → 提交 emit('submit', formData)
```

| 继承字段 | 来源 | 类型 | 继承方式 | 备注 |
|----------|------|------|----------|------|
| name | EditData → Form | COPY | Explicit | 可编辑 |
| employeeNo | EditData → Form | COPY | Explicit | 可编辑 |
| departmentId | EditData → Form | REFERENCE | Explicit | 可编辑，触发岗位过滤 |
| positionId | EditData → Form | REFERENCE | Explicit | 可编辑 |
| storeId | EditData → Form | REFERENCE | Explicit | 可编辑，按工作地点类型显示 |
| warehouseId | EditData → Form | REFERENCE | Explicit | 可编辑，按工作地点类型显示 |
| salary | EditData → Form | COPY | Explicit | 可编辑 |

**设计特点**: EmployeeFormDialog 采用「全量继承 + 全量可编辑」模式，无只读字段。继承后所有字段均可修改。

---

## 三、继承类型分类

### 3.1 Explicit Inheritance (显式继承)

数据通过 props/vuex 显式传递，代码中有明确的赋值语句。

| 场景 | 示例 | 证据 |
|------|------|------|
| 到货单继承采购订单ID | `formData.orderId = props.initialOrderId` | ArrivalFormDialog.vue:179 |
| 付款继承应付ID | `payableId: Number(props.payable.id)` | PaymentDialog.vue:251 |
| 收货确认继承到货明细 | `items: arrival.items.map(buildInitialItem)` | ReceiptConfirmationFormDialog.vue:83 |
| 员工编辑继承全量数据 | `fillEditData(props.editData)` | EmployeeFormDialog.vue:177 |

### 3.2 Implicit Inheritance (隐式继承)

数据通过计算属性/watch/默认值隐式继承，无显式赋值语句。

| 场景 | 示例 | 证据 |
|------|------|------|
| 付款金额默认=未付金额 | `formData.payAmount = remainYuan.value` | PaymentDialog.vue:225 |
| 收款金额默认=未收金额 | `formData.receiveAmount = remainYuan.value` | ReceiptDialog.vue:143 |
| 门店上下文继承 | `useOrgContextStore().currentStoreId` | org-context.ts:21 |
| 部门选项自动过滤岗位 | `filteredPositionOptions` computed | EmployeeFormDialog.vue:106 |

### 3.3 Hidden Inheritance (隐藏继承)

数据通过间接机制传递，开发者容易忽略。

| 场景 | 示例 | 风险 |
|------|------|------|
| 金额单位转换 | 元→分通过 `yuanToFen()` | ⚠️ 100倍误差风险 |
| 供应商结算方式→付款方式 | `mapSettlementToPaymentMethod()` | ⚠️ 间接推导，逻辑分散 |
| 岗位按部门过滤 | `filteredPositionOptions` | 部门变更后岗位选项变化 |
| 收货确认的 storeId/warehouseId | 从 arrival 继承但字段不在表单中 | Hidden Inheritance |

### 3.4 Missing Inheritance (缺失继承)

应该继承但未继承的数据。

| 场景 | 问题 | 影响 |
|------|------|------|
| 采购订单→到货单: 供应商信息 | 仅传 orderId，不传 supplierId | 需要通过 orderId 反查 |
| 到货单→收货确认: 物流信息 | 物流信息不继承到收货确认 | 需要重新填写或查看 |
| 应付→付款: 银行账户 | 不继承上次付款的银行账户 | 每次需重新选择 |
| 员工编辑: 原部门名称 | 只继承 departmentId，不继承 departmentName | 需要反查 |

### 3.5 Incorrect Inheritance (错误继承)

继承了不应该继承的数据或继承逻辑有误。

| 场景 | 问题 | 证据 |
|------|------|------|
| 金额单位双轨 | 新表用分，legacy 用元，跨表 JOIN 差100倍 | truth-conflict-map.md 冲突1 |
| product_id vs material_id | 遗留列名 product_id 实际指向物料 | truth-conflict-map.md 冲突2 |
| 订单状态三套并存 | order_status/payment_status/legacy 三套状态机 | state-machine-map.md |

---

## 四、数据继承风险矩阵

### 4.1 高风险继承点

| # | 链路 | 风险描述 | 严重程度 | 缓解措施 |
|---|------|----------|----------|----------|
| 1 | 金额 元↔分转换 | PayableDialog/ReceiptDialog/VoucherDialog 中 yuanToFen/fenToYuan 转换 | 🔴 P0 | 已有转换函数，但需确保全链路一致 |
| 2 | 订单状态三套并存 | 不同状态机间映射不完整 | 🔴 P0 | 已裁决并存，需维护映射表 |
| 3 | food/foods 双写 | 下单同时写两表，可能不一致 | 🔴 P0 | 同一事务写入 |
| 4 | inventory/store_inventory 双写 | 采购入库/调拨双写 | 🟡 P1 | 需确保同一事务 |

### 4.2 中风险继承点

| # | 链路 | 风险描述 | 严重程度 | 缓解措施 |
|---|------|----------|----------|----------|
| 5 | PaymentDialog re-query supplier | 页面打开后供应商信息可能变化 | 🟡 P1 | 可接受，保证最新状态 |
| 6 | ReceiptConfirm继承storeId/warehouseId | Hidden Inheritance，不在表单中 | 🟡 P1 | 已通过代码处理 |
| 7 | 凭证前后端状态码错位 | 后端0-3 vs 前端1-4 | 🟡 P1 | 已在adapter层封装 |
| 8 | stores/stores_new双表 | 可能读错表 | 🟡 P1 | 已裁决用 stores_new |

### 4.3 低风险继承点

| # | 链路 | 风险描述 | 严重程度 | 缓解措施 |
|---|------|----------|----------|----------|
| 9 | 部门→岗位过滤 | 部门变更后岗位选项变化 | 🟢 P2 | 已通过computed实现 |
| 10 | 供应商→物料过滤 | 跨供应商选料逻辑 | 🟢 P2 | 已有CrossSupplierTipDialog |

---

## 五、继承链路数据流图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        采购链路数据流                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  material_archives ──┐                                              │
│                      ├──→ PurchaseRequestForm                       │
│  suppliers ──────────┘         │                                    │
│                               ↓                                    │
│  purchase_request ─────→ RequestDetailDialog                       │
│                               │                                    │
│                               ↓ (转单)                              │
│  material_archives ──┐                                              │
│                      ├──→ PurchaseOrderFormDialog                  │
│  suppliers ──────────┘         │                                    │
│                               ↓                                    │
│  purchase_orders ─────→ 到货单列表                                   │
│                               │                                    │
│                               ↓                                    │
│  purchase_arrivals ───→ ArrivalFormDialog                          │
│                               │                                    │
│                               ↓                                    │
│  receipt_confirmation → ReceiptConfirmationFormDialog              │
│                               │                                    │
│                               ↓                                    │
│  inventory / store_inventory ──→ 库存变更                           │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                        财务链路数据流                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  purchase_orders ─────→ PurchaseSettlement                         │
│                               │                                    │
│                               ↓                                    │
│  payables ────────────→ PaymentDialog                              │
│                               │                                    │
│                    ┌──────────┼──────────┐                         │
│                    ↓          ↓          ↓                         │
│               payment   bank_accounts  fund_flows                  │
│                               │                                    │
│                               ↓                                    │
│  accounting_subjects ─→ finance_vouchers                           │
│                               │                                    │
│                               ↓                                    │
│  finance_records ─────→ 财务报表                                    │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 六、跨终端继承关系

### 6.1 管理后台 → POS 终端

| 继承数据 | 管理后台写入 | POS 读取 | 继承方式 |
|----------|-------------|----------|----------|
| 菜品信息 | FoodManagement.vue | POS 点餐 | DB: foods 表 |
| 价格信息 | DishPricing.vue | POS 收银 | DB: foods.price |
| 库存信息 | InventoryStockin.vue | POS 可售判断 | DB: foods.stock |
| 门店配置 | StoreArchive.vue | POS 启动加载 | DB: stores_new |

### 6.2 管理后台 → 员工终端

| 继承数据 | 管理后台写入 | 员工终端读取 | 继承方式 |
|----------|-------------|-------------|----------|
| 员工信息 | EmployeeFormDialog | 个人中心 | DB: employees |
| 排班信息 | ShiftManagement | 排班查询 | DB: schedule |
| 考勤规则 | SystemConfig | 打卡校验 | Redis 缓存 |

### 6.3 管理后台 → 小程序

| 继承数据 | 管理后台写入 | 小程序读取 | 继承方式 |
|----------|-------------|-----------|----------|
| 菜品菜单 | FoodManagement | 菜单浏览 | API: /v1/foods |
| 会员信息 | MemberList | 会员中心 | API: /v1/members |
| 优惠券 | CouponTemplate | 领券中心 | API: /v1/coupons |

---

## 七、Pinia Store 继承关系

### 7.1 orgContext Store

```typescript
// org-context.ts
interface OrgContextState {
  currentStoreId: string | null  // 当前门店
  currentOrgId: string | null    // 当前组织
  version: number                // 版本号（变更触发重载）
}
```

**继承链**: `orgContext.currentStoreId` → 被所有门店相关下拉继承 → 通过 `version` 变更触发重载

### 7.2 permission Store

```typescript
// permission.ts
interface PermissionState {
  token: string
  userInfo: UserInfo
  permissions: string[]
  domains: string[]
}
```

**继承链**: `permissionStore.permissions` → 路由守卫检查 → 控制菜单可见性 → 控制按钮权限

### 7.3 tab Store

```typescript
// tab.ts - 管理已打开的标签页
```

**继承链**: 路由切换 → tab store 记录 → 保持标签页状态

---

## 八、localStorage/sessionStorage 继承

| 存储位置 | 数据 | 继承场景 | 风险 |
|----------|------|----------|------|
| localStorage | token | 页面刷新后恢复登录状态 | 🟢 低 |
| localStorage | theme | 主题偏好持久化 | 🟢 低 |
| sessionStorage | currentStoreId | 标签页内门店上下文 | 🟡 中 - 关闭标签页丢失 |
| sessionStorage | lastQueryParams | 列表页搜索条件恢复 | 🟡 中 |

---

## 九、Watch/Effect 继承关系

| 监听目标 | 触发动作 | 继承场景 | 代码位置 |
|----------|----------|----------|----------|
| `props.modelValue` | 初始化表单 | Dialog 打开时继承数据 | 多个 Dialog |
| `props.visible` | 加载数据 | Dialog 打开时异步加载 | PaymentDialog, ReceiptDialog |
| `formData.supplierId` | 过滤物料选项 | 供应商变更后刷新物料列表 | PurchaseOrderFormDialog |
| `formData.workLocationType` | 显示/隐藏门店/仓库选择 | 工作地点类型变更 | EmployeeFormDialog |
| `orgContext.version` | 重载下拉选项 | 门店/组织切换后刷新 | useStoreOptions, useDepartmentOptions |

---

## 十、Default Values 继承

| 表单字段 | 默认值来源 | 默认值 | 继承方式 |
|----------|-----------|--------|----------|
| PaymentDialog.payAmount | remainAmount | 未付金额 | Implicit |
| ReceiptDialog.receiveAmount | remainAmount | 未收金额 | Implicit |
| PaymentDialog.paymentDate | new Date() | 当前日期 | Default |
| PaymentDialog.paymentMethod | supplier.settlementMethod | 从供应商推导 | DERIVED |
| VoucherFormDialog.voucherType | 硬编码 | 'general' | Default |
| VoucherFormDialog.voucherDate | new Date() | 当前日期 | Default |
| EmployeeFormDialog.status | 硬编码 | 'active' | Default |
| EmployeeFormDialog.employmentType | 硬编码 | 'full_time' | Default |
| EmployeeFormDialog.workLocationType | 硬编码 | 'HEADQUARTERS' | Default |

---

## 十一、Hidden Fields (隐藏字段)

| Dialog/Form | 隐藏字段 | 隐藏原因 | 风险 |
|-------------|---------|----------|------|
| PaymentDialog | A模式: bankTransactionNo, bankTransactionTime, voucherType, voucherUrl | 仅A模式显示 | 🟢 低 - v-if控制 |
| ReceiptConfirmationFormDialog | storeId, warehouseId | 从arrival继承，不在表单中 | 🟡 中 - Hidden Inheritance |
| PurchaseOrderFormDialog | items[].purchaseOrderId | 空值，提交后由后端填充 | 🟢 低 |

---

*生成时间: 2026-09-09*
*数据来源: 代码库静态分析 + 地图文件交叉验证*
