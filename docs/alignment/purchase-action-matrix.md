# 采购链路操作矩阵

> 范围：采购申请、采购计划、采购订单、到货登记、采购结算  
> 用途：明确各页面/状态下可执行的操作、触发的 API、请求参数及数据刷新策略  
> 生成日期：2026-07-30

---

## 符号说明

- `✓`：当前代码中已提供的操作
- `−`：当前代码中未提供的操作
- `[待修复]`：操作存在但实现不完整、API 缺失或与状态/权限不匹配

---

## 1. 采购申请操作矩阵

### 1.1 页面：PurchaseRequest.vue

| 操作 | 入口位置 | 触发条件（状态） | 调用的 API | 请求方法/路径 | 关键参数 | 成功后的数据刷新 |
|-----|---------|----------------|-----------|-------------|---------|----------------|
| 新增申请 | 页面头部 `+ 新增申请` | 始终可见 | `purchaseRequestApi.create` | `POST /v1/purchase/requests` | `{ title, departmentId, departmentName, priority, expectedDate, description, items[] }` | `refresh()` + `loadStatistics()` |
| 编辑 | 操作列 | `status === 'draft' \|\| status === 'rejected'` | `purchaseRequestApi.update` | `PUT /v1/purchase/requests/{id}` | `{ requestId, title, departmentId, departmentName, priority, expectedDate, description, items[] }` | `refresh()` + `loadStatistics()` |
| 提交审批 | 操作列 | `status === 'draft'` | `purchaseRequestApi.submit` | `POST /v1/purchase/requests/{id}/submit` | 无 | `refresh()` + `loadStatistics()` |
| 批量提交 | 工具栏 | 选中行中存在 `draft` | `purchaseRequestApi.submit`（循环） | `POST /v1/purchase/requests/{id}/submit` | 无 | `refresh()` + `loadStatistics()` |
| 审批 | 操作列 | `status === 'pending'` | `purchaseRequestApi.approve` | `POST /v1/purchase/requests/{id}/approve` | `{ status: 'approved' \| 'rejected', remark }` | `refresh()` + `loadStatistics()` |
| 生成采购订单 | 操作列 | `status === 'approved'` | `purchaseRequestApi.generateOrder` | `POST /v1/purchase/requests/{id}/generate-order` | 无 | `refresh()` + `loadStatistics()` |
| 删除 | 操作列 | `status === 'draft'` | `purchaseRequestApi.delete` | `DELETE /v1/purchase/requests/{id}` | 无 | `refresh()` + `loadStatistics()` |
| 查看详情 | 操作列 `详情` | 始终可见 | `purchaseRequestApi.getById` | `GET /v1/purchase/requests/{id}` | 无 | 详情弹窗赋值 |
| 查询 | 工具栏 `查询` | — | `purchaseRequestApi.getList` | `GET /v1/purchase/requests/page` | `{ requestNo, applicantName, departmentName, status, startDate, endDate, page, size }` | `refresh()` |
| 重置 | 工具栏 `重置` | — | — | — | 清空查询表单 | `refresh()` |

### 1.2 操作问题

- `[已修复]` 删除操作原仅在 `draft` 状态显示，现已在 `PurchaseRequest.vue` 操作列对 `draft` / `rejected` 状态均显示删除按钮。
- `[待修复]` `cancelled` 状态无任何操作入口，包括查看详情理论上应可用。
- `[已修复]` 审批弹窗 `approveForm` 已补充 `remark` 校验：选择拒绝时 `remark` 必填。

---

## 2. 采购计划操作矩阵

### 2.1 页面：PurchasePlan.vue

| 操作 | 入口位置 | 触发条件（状态） | 调用的 API | 请求方法/路径 | 关键参数 | 成功后的数据刷新 |
|-----|---------|----------------|-----------|-------------|---------|----------------|
| 新增计划 | 页面头部 `+ 新增计划` | 始终可见 | `purchasePlanApi.create` | `POST /v1/purchase/plans` | `{ departmentId, planDate, remark, items[] }` | `refresh()` |
| 编辑 | 操作列 | `status === 'draft'` | `purchasePlanApi.update` | `PUT /v1/purchase/plans/{id}` | `{ departmentId, planDate, remark, items[] }` | `refresh()` |
| 提交审批 | 操作列 | `status === 'draft'` | `purchasePlanApi.submit` | `PUT /v1/purchase/plans/{id}/submit` | 无 | `refresh()` |
| 删除 | 操作列 | `status === 'draft' \|\| status === 'rejected'` | `purchasePlanApi.delete` | `DELETE /v1/purchase/plans/{id}` | 无 | `refresh()` |
| 查看详情 | 操作列 `详情` | 始终可见 | `purchasePlanApi.getById` | `GET /v1/purchase/plans/{id}` | 无 | 详情弹窗赋值 |
| 生成计划 | 工具栏 `生成计划` | 始终可见 | 无真实 API | — | — | 提示「[待后端配合] 计划生成功能开发中」`[待后端配合]` |
| 导出 | 工具栏 `导出` | 始终可见 | 无真实 API | — | — | 提示「[待后端配合] 导出功能开发中」`[待后端配合]` |
| 查询 | 工具栏 `查询` | — | `purchasePlanApi.getList` | `GET /v1/purchase/plans` | `{ planNo, keyword, status, startDate, endDate, page, size }` | `refresh()` |
| 重置 | 工具栏 `重置` | — | — | — | 清空查询表单 | `refresh()` |

### 2.2 操作问题

- `[已修复]` 采购计划页面已增加“审批通过/拒绝”按钮（`v-if="row.status === 'pending'"`），调用 `purchasePlanApi.approve` / `reject`。
- `[待修复]` `approved` / `executing` / `completed` 状态无任何操作，状态机存在断点。
- `[待后端配合]` “生成计划”与“导出”为占位功能，未接入真实 API，已改为明确的「[待后端配合] 功能开发中」提示。

---

## 3. 采购订单操作矩阵

### 3.1 页面：PurchaseOrder.vue

| 操作 | 入口位置 | 触发条件（状态） | 调用的 API | 请求方法/路径 | 关键参数 | 成功后的数据刷新 |
|-----|---------|----------------|-----------|-------------|---------|----------------|
| 新建订单 | 页面头部 `+ 新建订单` | 始终可见 | `purchaseOrderApi.create` | `POST /v1/purchase/orders` | `{ supplierId, expectedDate, remark, items[] }` | `refresh()` |
| 从申请生成 | 页面头部 `从申请生成` | 始终可见 | `purchaseRequestApi.generateOrder` | `POST /v1/purchase/requests/{id}/generate-order` | `requestId` | `refresh()` |
| 编辑 | 操作列 | `status === 'draft'` | `purchaseOrderApi.update` | `PUT /v1/purchase/orders/{id}` | `{ supplierId, expectedDate, remark, items[] }` | `refresh()` |
| 提交审批 | 操作列 | `status === 'draft'` | `purchaseOrderApi.submit` + `approvalWorkflowApi.submitApproval` | `PUT /v1/purchase/orders/{id}/submit` + `POST /v1/approval-workflow/submit` | `businessType='purchase_order', businessId` | `refresh()` |
| 审批 | 操作列 | `status === 'pending'` | `purchaseOrderApi.approve` / `approvalWorkflowApi` | `PUT /v1/purchase/orders/{id}/approve` | 可选 `approvalRemark` | `refresh()` |
| 确认下单 | 详情页 | `status === 'approved'` | `purchaseOrderApi.confirmOrder` | `PUT /v1/purchase/orders/{id}/confirm` | 无 | `refresh()` + 关闭详情 |
| 收货 | 操作列 | `status === 'ordered'` | 无 API，页面跳转 | 跳转 `/purchase/stockin?orderId=...` | 无 | — |
| 链路追溯 | 操作列 | 始终可见 | 无 | 打开 `PurchaseTraceDialog` | `orderNo` | 弹窗展示 |
| 删除 | 操作列 | `status === 'draft'` | `purchaseOrderApi.delete` | `DELETE /v1/purchase/orders/{id}` | 无 | `refresh()` |
| 终止 | 操作列 | `approved \|\| ordered \|\| shipped` | `purchaseOrderApi.cancel` | `PUT /v1/purchase/orders/{id}/cancel?reason=...` | `reason` | `refresh()` |
| 详情审批通过 | 详情页 | `status === 'pending'` | `approvalWorkflowApi` | 弹窗审批 | — | `refresh()` |
| 详情审批拒绝 | 详情页 | `status === 'pending'` | `purchaseOrderApi.reject` / `approvalWorkflowApi.reject` | `PUT /v1/purchase/orders/{id}/reject?reason=...` | `reason` | `refresh()` |
| 查询 | 工具栏 `查询` | — | `purchaseOrderApi.getList` | `GET /v1/purchase/orders` | `{ orderNo, status, paymentStatus, startDate, endDate, mineOnly, page, size }` | `refresh()` |
| 重置 | 工具栏 `重置` | — | — | — | 清空查询表单 | `refresh()` |

### 3.2 操作问题

- `[待修复]` 操作列显示“收货”按钮，但实际为页面跳转，订单状态变更依赖到货登记，交互语义不清晰。
- `[待修复]` `shipped` 状态显示“终止”按钮，但后端无 `shipped` 状态，条件判断基于前端近似映射。
- `[待修复]` 详情页“确认下单”仅在详情页可用，列表操作列未提供该入口。
- `[待修复]` 从申请生成订单后，列表高亮逻辑依赖 `requestNo` 与订单 `requestNo` 字段，但后端当前未返回 `requestNo`。

---

## 4. 到货登记操作矩阵

### 4.1 页面：PurchaseStockin.vue

| 操作 | 入口位置 | 触发条件（状态） | 调用的 API | 请求方法/路径 | 关键参数 | 成功后的数据刷新 |
|-----|---------|----------------|-----------|-------------|---------|----------------|
| 新增到货单 | 页面头部 `+ 新增` | 始终可见 | `purchaseArrivalApi.createFromOrder` | `POST /v1/purchase/arrivals` | `{ orderId, shipmentStatus, logisticsNo, logisticsCompany, transportMode, vehiclePlateNo, vehicleType, driverName, driverPhone, freightAmount, estimatedArrivalDate, remark }` | `refresh()` + 高亮最近操作 |
| 编辑物流信息 | 操作列（推断） | `canEditLogistics(row)`：pending/receiving/partial_received | `purchaseArrivalApi.update` | `PUT /v1/purchase/arrivals/{id}` | `{ shipmentStatus, logisticsNo, logisticsCompany, transportMode, vehiclePlateNo, vehicleType, driverName, driverPhone, freightAmount, estimatedArrivalDate, actualArrivalDate, remark }` | `refresh()` |
| 关闭到货单 | 操作列（推断） | `canClose(row)`：仅 `pending` | `purchaseArrivalApi.close` | `PUT /v1/purchase/arrivals/{id}/close` | `{ closeReason, remark }` | `refresh()` |
| 查看详情 | 操作列/行点击 | 始终可见 | `purchaseArrivalApi.getById` | `GET /v1/purchase/arrivals/{id}` | 无 | 打开详情弹窗 |
| 查询 | 工具栏 `查询` | — | `purchaseArrivalApi.getList` | `GET /v1/purchase/arrivals` | `{ arrivalCode, orderCode, supplierId, status, receiverType, overdueOnly, estimatedStartDate, estimatedEndDate, createUserId, page, size }` | `refresh()` |
| 重置 | 工具栏 `重置` | — | — | — | 清空查询表单 | `refresh()` |
| 我的单据 | 查询表单 checkbox | — | 附加 `createUserId` | `GET /v1/purchase/arrivals` | `createUserId = currentUserId` | `refresh()` |

### 4.2 子组件操作

| 子组件 | 操作 | 触发条件 | 调用的 API | 备注 |
|-------|-----|---------|-----------|------|
| `ArrivalFormDialog.vue` | 创建/编辑到货单 | `formDialogMode` | `purchaseArrivalApi.createFromOrder` / `purchaseArrivalApi.update` | 关联采购订单选择 |
| `ArrivalCloseDialog.vue` | 关闭到货单 | `status === 'pending'` | `purchaseArrivalApi.close` | 需填写关闭原因 |
| `ArrivalDetailDialog.vue` | 查看详情 | 始终 | `purchaseArrivalApi.getById` | 只读 |

### 4.3 操作问题

- `[待修复]` 操作列完整代码未在片段中展示，编辑/关闭按钮的显隐条件依赖组件内局部函数，需统一梳理。
- `[待修复]` 到货登记缺少“收货确认”操作入口，无法将 `pending` 推进到 `received`。
- `[待修复]` 到货单创建后是否应反写采购订单状态，代码中未体现。

---

## 5. 采购结算操作矩阵

### 5.1 页面：PurchaseSettlement.vue

| 操作 | 入口位置 | 触发条件（状态） | 调用的 API | 请求方法/路径 | 关键参数 | 成功后的数据刷新 |
|-----|---------|----------------|-----------|-------------|---------|----------------|
| 新增结算 | 页面头部 `+ 新增结算` | 始终可见 | `purchaseSettlementApi.create` | `POST /v1/purchase/settlements` | `{ orderId, supplierId, supplierName, totalAmount, dueDate, paymentMethod, remark }` | `fetchData()` |
| 编辑 | 操作列 | `status === 'pending' \|\| status === 'partial'` | `purchaseSettlementApi.update` | `PUT /v1/purchase/settlements/{id}` | `{ totalAmount, dueDate, paymentMethod, remark }` | `fetchData()` |
| 付款 | 操作列 | `status === 'pending' \|\| partial \|\| finance_reviewing` | `purchaseSettlementApi.settle` | `POST /v1/purchase/settlements/{id}/settle?action=pay&voucherNo=...` | `voucherNo` | `fetchData()` |
| 标记完成 | 操作列 | `status === 'partial' && unpaidAmount === 0` | `purchaseSettlementApi.settle` | `POST /v1/purchase/settlements/{id}/settle?action=complete` | 无 | `fetchData()` |
| 付款记录 | 操作列 | `status === 'partial' \|\| status === 'completed'` | 无真实 API | — | — | 弹窗显示「[待后端配合] 付款记录查询功能开发中」`[待后端配合]` |
| 批量结算 | 工具栏 | 选中行数 > 0 | 无真实 API | — | — | 提示「[待后端配合] 批量结算功能开发中」`[待后端配合]` |
| 删除 | 当前页面未提供 | API 中存在 | `purchaseSettlementApi.delete` | `DELETE /v1/purchase/settlements/{id}` | 无 | — |
| 申请发票 | 表单中 | 手动选择发票状态 | `purchaseSettlementApi.applyInvoice` | `POST /v1/purchase/settlements/{id}/invoice/apply` | 无 | 未与表单联动 `[待修复]` |
| 确认收票 | 表单中 | 手动选择发票状态 | `purchaseSettlementApi.receiveInvoice` | `POST /v1/purchase/settlements/{id}/invoice/receive?invoiceNo=...` | `invoiceNo` | 未与表单联动 `[待修复]` |
| 查询 | 工具栏 `查询` | — | `purchaseSettlementApi.getList` | `GET /v1/purchase/settlements` | `{ settlementNo, supplierId, status, startDate, endDate, keyword, page, size }` | `fetchData()` |
| 重置 | 工具栏 `重置` | — | — | — | 清空查询表单 | `fetchData()` |

### 5.2 操作问题

- `[待后端配合]` 付款记录功能为 TODO，未调用真实 API，已改为弹窗显示「[待后端配合] 付款记录查询功能开发中」。
- `[待后端配合]` 批量结算为占位功能，已改为提示「[待后端配合] 批量结算功能开发中」。
- `[待修复]` 发票状态在表单中由用户手动下拉选择（0/1/2），未与 `applyInvoice` / `receiveInvoice` API 联动。
- `[待修复]` 结算表单使用 `orderIds` 数组，但后端 `toCreateDTO` 仅取 `orderIds[0]`，多选逻辑未实现。
- `[待修复]` 结算删除按钮在页面中未找到，但 API 已提供。

---

## 6. 公共操作对比

| 操作 | 采购申请 | 采购计划 | 采购订单 | 到货登记 | 采购结算 |
|-----|---------|---------|---------|---------|---------|
| 新增 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 编辑 | ✓ | ✓ | ✓ | ✓ | ✓ |
| 删除 | ✓ | ✓ | ✓ | ✓（API 有，页面未展示） | ✓（API 有，页面未展示） |
| 提交审批 | ✓ | ✓ | ✓ | − | − |
| 审批通过 | ✓ | API 有，页面无 | ✓ | − | − |
| 审批拒绝 | ✓ | API 有，页面无 | ✓ | − | − |
| 生成下游单据 | 生成订单 | − | 跳转收货 | 收货确认（缺失） | 付款 |
| 批量操作 | 批量提交 | − | 从申请批量生成 | − | 批量结算（占位） |
| 导出 | − | 占位 | − | − | − |

---

## 7. 数据刷新策略对比

| 页面 | 列表刷新方法 | 统计刷新方法 | 一致性 |
|-----|------------|------------|-------|
| 采购申请 | `refresh()`（useCrudTable） | `loadStatistics()` | 每次操作后两者都刷新 ✓ |
| 采购计划 | `refresh()`（useCrudTable） | 无独立统计接口，统计为前端计算 | 刷新列表即可 |
| 采购订单 | `refresh()`（useCrudTable） | 无独立统计接口，统计为前端计算 | 刷新列表即可 |
| 到货登记 | `refresh()`（useCrudTable） | 无独立统计接口，统计为前端计算 | 刷新列表即可 |
| 采购结算 | `fetchData()`（自定义） | 无独立统计接口，统计为前端计算 | 刷新列表即可 |

### 数据刷新问题

- `[待修复]` 采购结算使用自定义 `fetchData()`，其他页面使用 `useCrudTable`，分页/加载状态管理不统一。
- `[待修复]` 采购申请统计与列表分别刷新，存在两次请求，可优化为后端聚合一次返回。
- `[待修复]` 到货登记创建成功后仅高亮首条记录，若批量创建多条，其余记录无高亮反馈。

---

## 8. API 路径一致性

| 操作 | 采购申请 | 采购计划 | 采购订单 | 到货登记 | 采购结算 |
|-----|---------|---------|---------|---------|---------|
| 列表 | `GET /v1/purchase/requests/page` | `GET /v1/purchase/plans` | `GET /v1/purchase/orders` | `GET /v1/purchase/arrivals` | `GET /v1/purchase/settlements` |
| 详情 | `GET /v1/purchase/requests/{id}` | `GET /v1/purchase/plans/{id}` | `GET /v1/purchase/orders/{id}` | `GET /v1/purchase/arrivals/{id}` | `GET /v1/purchase/settlements/{id}` |
| 创建 | `POST /v1/purchase/requests` | `POST /v1/purchase/plans` | `POST /v1/purchase/orders` | `POST /v1/purchase/arrivals` | `POST /v1/purchase/settlements` |
| 更新 | `PUT /v1/purchase/requests/{id}` | `PUT /v1/purchase/plans/{id}` | `PUT /v1/purchase/orders/{id}` | `PUT /v1/purchase/arrivals/{id}` | `PUT /v1/purchase/settlements/{id}` |
| 删除 | `DELETE /v1/purchase/requests/{id}` | `DELETE /v1/purchase/plans/{id}` | `DELETE /v1/purchase/orders/{id}` | − | `DELETE /v1/purchase/settlements/{id}` |
| 提交审批 | `POST /v1/purchase/requests/{id}/submit` | `PUT /v1/purchase/plans/{id}/submit` | `PUT /v1/purchase/orders/{id}/submit` | − | − |
| 审批通过 | `POST /v1/purchase/requests/{id}/approve` | `PUT /v1/purchase/plans/{id}/approve` | `PUT /v1/purchase/orders/{id}/approve` | − | − |
| 审批拒绝 | 同 approve（status=rejected） | `PUT /v1/purchase/plans/{id}/reject` | `PUT /v1/purchase/orders/{id}/reject` | − | − |
| 特殊动作 | `POST /v1/purchase/requests/{id}/generate-order` | − | `PUT /v1/purchase/orders/{id}/confirm` | `PUT /v1/purchase/arrivals/{id}/close` | `POST /v1/purchase/settlements/{id}/settle` |

### API 一致性问题

- `[待修复]` 提交审批请求方法不一致：采购申请用 `POST`，采购计划/订单用 `PUT`。
- `[待修复]` 审批通过请求方法不一致：采购申请用 `POST`，采购计划/订单用 `PUT`。
- `[待修复]` 采购申请生成订单接口路径为 `/generate-order`，其他下游生成动作命名不统一。
- `[待修复]` 到货登记无删除 API 实现（API 文件中未提供），但业务上可能需要。

---

## 9. [待修复] 汇总

1. `[已修复]` 采购申请 `rejected` 状态删除入口缺失（`PurchaseRequest.vue` 操作列已对 `draft` / `rejected` 显示删除）。
2. `[已修复]` 采购计划 `pending` 状态审批通过/拒绝按钮缺失（`PurchasePlan.vue` 操作列已增加并调用 `approve` / `reject`）。
3. `[待修复]` 采购计划 `approved`/`executing`/`completed` 状态无操作入口，状态机断点。
4. `[待后端配合]` 采购计划“生成计划”与“导出”为占位功能，已改为明确「[待后端配合] 功能开发中」提示。
5. `[待修复]` 采购订单列表操作列缺少“确认下单”入口。
6. `[待修复]` 采购订单“收货”按钮语义为跳转，状态变更关系不清晰。
7. `[待修复]` 到货登记缺少收货确认操作，无法将到货单推进到 `received`。
8. `[待修复]` 到货登记操作列完整条件未在代码片段中完整展示。
9. `[待后端配合]` 采购结算付款记录、批量结算为占位功能，已改为明确「[待后端配合] 功能开发中」提示。
10. `[待修复]` 采购结算发票状态手动选择与发票 API 未联动。
11. `[待修复]` 采购结算表单 `orderIds` 多选与后端单订单字段不匹配。
12. `[待修复]` 采购结算删除按钮在页面中未找到。
13. `[待修复]` API 请求方法在提交审批、审批通过等操作上不一致（POST vs PUT）。
14. `[待修复]` 各页面数据刷新策略不统一（`useCrudTable.refresh()` vs `fetchData()`）。
