# 采购链路字段四维对照表

> 范围：采购申请、采购计划、采购订单、到货登记、采购结算  
> 用途：为后续代码改造提供字段命名、类型、状态、金额/ID 转换依据  
> 生成日期：2026-07-30

---

## 1. 单据主档字段对照

| 业务语义 | 采购申请 `PurchaseRequest` | 采购计划 `PurchasePlanInfo` | 采购订单 `PurchaseOrderInfo` | 到货登记 `PurchaseArrivalInfo` | 采购结算 `PurchaseSettlementInfo` |
|---------|---------------------------|----------------------------|-----------------------------|--------------------------------|----------------------------------|
| 单据ID | `requestId` (string/UUID) | `planId` (string) | `purchaseOrderId` (string) | `arrivalId` (string) | `settlementId` (string) |
| 单据编号 | `requestNo` | `planNo` | `orderNo` | `arrivalCode` | `settlementNo` |
| 标题/主题 | `title` | — | — | — | — |
| 来源单据ID | — | — | `requestId` | `orderId` | `orderId` |
| 来源单据编号 | — | — | `requestNo` | `orderCode` | `orderNo` |
| 供应商ID | — | 明细 `supplierId` | `supplierId` | `supplierId` | `supplierId` |
| 供应商名称 | — | 明细 `supplierName` | `supplierName` | `supplierName` | `supplierName` |
| 申请部门ID | `departmentId` | `departmentId` | — | — | — |
| 申请部门名称 | `departmentName` | `departmentName` | — | — | — |
| 申请人ID | `applicantId` | — (`creatorId`) | — (`createBy`) | — | — |
| 申请人名称 | `applicantName` | — (`creatorName`) | — (`createBy`) | — | `createBy` |
| 负责人/创建人 | — | `creatorId` / `creatorName` | `createBy` | — | `createBy` |
| 总金额（元） | `totalAmount` | `totalAmount` | `totalAmount` | `totalAmount` | `totalAmount` |
| 已付金额 | — | — | `paidAmount` | — | `paidAmount` |
| 未付金额 | — | — | — | — | `unpaidAmount` |
| 单据状态 | `status` (string) | `status` (string) | `status` (string) | `status` (string) | `status` (string) |
| 付款状态 | — | — | `paymentStatus` | — | — |
| 优先级 | `priority` | — | `priority` `[待修复：后端未返回]` | — | — |
| 采购类型 | — | — | `purchaseType` `[待修复：后端未返回]` | — | — |
| 来源类型 | — | — | `sourceType` `[待修复：后端未返回]` | — | — |
| 合同ID/编号 | — | — | `contractId` / `contractNo` `[待修复：后端未返回]` | — | — |
| 预算ID/状态 | `budgetId` / `budgetStatus` | — | `budgetId` / `budgetStatus` `[待修复：后端未返回]` | — | — |
| 计划/下单/结算日期 | `expectedDate` | `planDate` | `orderDate` / `expectedDate` | `estimatedArrivalDate` / `actualArrivalDate` | `dueDate` / `createTime` |
| 联系人/电话 | — | — | `contactPerson` / `contactPhone` `[待修复：后端未返回]` | `driverName` / `driverPhone` | — |
| 收货仓库ID | — | — | `warehouseId` | `warehouseId` | — |
| 收货门店ID | — | — | — | `storeId` | — |
| 收货方类型 | 明细 `plannedReceiverType` | — | 明细 `plannedReceiverType` | `receiverType` | — |
| 拒绝/关闭原因 | `rejectReason` | `rejectReason` | `rejectReason` | `closeReason` | — |
| 审批人/时间 | `approvedBy` / `approvedTime` | `approveBy` / `approveTime` `[待修复：命名不统一]` | `updateBy` / `updateTime` `[待修复：后端无审批字段]` | — | — |
| 备注 | `description` / `remark` | `remark` | `remark` | `remark` | `remark` |
| 创建时间 | `createTime` | `createTime` | `createTime` | `createTime` | `createTime` |
| 更新时间 | `updateTime` | `updateTime` | `updateTime` | `updateTime` | `updateTime` |
| 创建人 | `createBy` | — | `createBy` | — | `createBy` |
| 删除审计 | `deletedBy` / `deletedTime` | — | `deletedBy` / `deletedTime` `[待修复：后端未返回]` | — | — |

### 主档字段不一致点

1. **申请人 vs 创建人/负责人**  
   - 采购申请使用 `applicantId` / `applicantName`；采购计划使用 `creatorId` / `creatorName`；采购订单使用 `createBy`。  
   - `[待修复]` 建议统一为 `createBy` / `creatorName` 或按业务语义统一。

2. **审批字段命名**  
   - 采购申请：`approvedBy` / `approvedTime`；采购计划：`approveBy` / `approveTime`。  
   - `[待修复]` 建议统一为 `approvedBy` / `approvedTime`。

3. **采购订单大量字段后端未返回**  
   - `requestId/requestNo/contractId/contractNo/sourceType/priority/purchaseType/contactPerson/contactPhone/paidAmount/budgetId/budgetStatus/updateBy/deletedTime/deletedBy` 当前在 `api/purchase/order.ts` 中被硬编码为空字符串或 0。  
   - `[待修复]` 需后端补充这些字段，或前端调整类型为可选并移除硬编码默认值。

4. **金额字段**  
   - 采购申请：`totalAmount` 后端为 Long 分，前端转元（`/ 100`）。  
   - 采购计划/订单/到货/结算：同样使用 Long 分 ↔ 元转换，但分别由 `plan.ts`、`order.ts`、`arrival.ts`、`settlement.ts` 各自处理，存在重复代码。  
   - `[待修复]` 建议统一调用 `utils/money` 中的 `fenToYuan` / `yuanToFen`。

---

## 2. 单据明细字段对照

| 业务语义 | 采购申请明细 `PurchaseRequestItem` | 采购计划明细 `PurchasePlanItem` | 采购订单明细 `PurchaseOrderItemInfo` | 到货单明细 `PurchaseArrivalItem` |
|---------|-----------------------------------|--------------------------------|-------------------------------------|----------------------------------|
| 明细ID | `itemId` (string) | — | `id` (number) | `arrivalItemId` (string) |
| 物料ID | `foodId` | `materialId` | `productId` | `materialId` |
| 物料名称 | `foodName` | `materialName` | `productName` | `materialName` |
| 物料编码 | `foodCode` | — | — | — |
| 规格 | `specification` | `specification` | `specification` | `specification` |
| 数量 | `quantity` | `quantity` | `quantity` | `expectedQuantity` / `receivedQuantity` |
| 单位 | `unit` | `unit` | `unit` | `unit` |
| 单价（元） | `estimatedPrice` | `estimatedPrice` | `unitPrice` | `unitPrice` |
| 金额（元） | `subtotalAmount` | 计算：`quantity * estimatedPrice` | `amount` | `amount` |
| 已到货/已收数量 | — | — | `receivedQuantity` | `receivedQuantity` |
| 备注 | `remark` | `remark` | `remark` | `remark` |
| 是否临时物料 | — | `isTempMaterial` | — | — |
| 供应商ID/名称 | 明细 `plannedReceiverType` 等 | `supplierId` / `supplierName` | — | — |
| 计划收货方类型 | `plannedReceiverType` | — | `plannedReceiverType` | — |
| 计划收货门店ID | `plannedStoreId` | — | `plannedStoreId` | — |
| 计划收货仓库ID | `plannedWarehouseId` | — | `plannedWarehouseId` | — |
| 关联订单明细ID | — | — | — | `orderItemId` |

### 明细字段不一致点

1. **物料字段命名严重不统一**  
   - 采购申请：`foodId` / `foodName` / `foodCode`（食品语义）。  
   - 采购计划/到货：`materialId` / `materialName`（物资语义）。  
   - 采购订单：`productId` / `productName`（产品语义）。  
   - `[待修复]` 建议采购链路统一使用 `materialId` / `materialName` / `materialCode`，避免同一实体在不同单据中名称不同。

2. **单价字段命名**  
   - 采购申请/计划使用 `estimatedPrice`；采购订单/到货使用 `unitPrice`。  
   - `[待修复]` 建议统一为 `unitPrice`，或在采购申请/计划阶段使用 `estimatedUnitPrice` 以区分预算价与实际价。

3. **金额字段**  
   - 采购申请：`subtotalAmount`；采购订单/到货：`amount`；采购计划：前端计算无独立字段。  
   - `[待修复]` 建议统一为 `amount` 或 `subtotalAmount`。

---

## 3. 状态字段对照

| 单据 | 前端状态值 | 中文标签 | 后端编码 | 说明 |
|-----|-----------|---------|---------|------|
| 采购申请 | `draft` | 草稿 | 语义字符串 | 无数字编码 |
| 采购申请 | `pending` | 待审批 | 语义字符串 | 无数字编码 |
| 采购申请 | `approved` | 已审批 | 语义字符串 | 无数字编码 |
| 采购申请 | `rejected` | 已拒绝 | 语义字符串 | 无数字编码 |
| 采购申请 | `completed` | 已完成 | 语义字符串 | 生成订单后自动置为 completed |
| 采购申请 | `cancelled` | 已取消 | 语义字符串 | 类型中存在但页面未展示操作入口 |
| 采购计划 | `draft` | 草稿 | 0 | — |
| 采购计划 | `pending` | 待审批 | 1 | — |
| 采购计划 | `approved` | 已审批 | 2 | — |
| 采购计划 | `executing` | 执行中 | 3 | — |
| 采购计划 | `completed` | 已完成 | 4 | — |
| 采购计划 | `rejected` | 已拒绝 | 5 | — |
| 采购订单 | `draft` | 草稿 | 0 | — |
| 采购订单 | `pending` | 待审批 | 1 | — |
| 采购订单 | `approved` | 已审批 | 2 | 后端为“已审核” |
| 采购订单 | `ordered` | 已下单 | 6 | 后端无独立状态，映射为 6 |
| 采购订单 | `shipped` | 已发货 | 2 `[待修复]` | 后端无此状态，映射到 approved |
| 采购订单 | `received` | 已收货 | 4 `[待修复]` | 后端无此状态，映射到 completed |
| 采购订单 | `partial_received` | 部分到货 | 3 | 后端为“部分入库” |
| 采购订单 | `completed` | 已完成 | 4 | — |
| 采购订单 | `rejected` | 已拒绝 | 5 `[待修复]` | 后端无此状态，映射到 cancelled |
| 采购订单 | `terminated` | 已终止 | 5 `[待修复]` | 后端无此状态，映射到 cancelled |
| 采购订单 | `cancelled` | 已取消 | 5 | — |
| 到货登记 | `pending` | 待收货 | 0 | — |
| 到货登记 | `receiving` | 收货中 | 1 | — |
| 到货登记 | `partial_received` | 部分收货 | 2 | — |
| 到货登记 | `received` | 已收货 | 3 | — |
| 到货登记 | `closed` | 已关闭 | 4 | — |
| 采购结算 | `pending` | 待付款 | 0 | 类型注释为“待结算”，页面显示“待结算” |
| 采购结算 | `partial` | 部分付款 | 1 | 类型注释为“部分结算”，页面显示“部分结算” |
| 采购结算 | `finance_reviewing` | 财务审核中 | 2 | — |
| 采购结算 | `completed` | 已完成 | 3 | 类型注释为“已结算”，页面显示“已结算” |
| 采购结算 | `overdue` | 已逾期 | 4 | — |

### 状态不一致点

1. **采购申请状态为语义字符串，其余单据为数字编码**  
   - `[待修复]` 建议统一：要么所有单据前后端均使用语义字符串，要么均使用数字编码 + DataConverter 转换。

2. **采购订单前后端状态数量不匹配**  
   - 前端定义 11 个状态，后端实体仅支持 7 个（0-6）。`shipped`/`received`/`rejected`/`terminated` 被就近映射到 `approved`/`completed`/`cancelled`。  
   - `[待修复]` 需明确业务状态机：扩展后端状态码，或前端删减/合并状态。

3. **标签文字不一致**  
   - 采购订单 `approved` 在 `PurchaseOrderStatusOptions` 中显示为“已审核”，但在 `converters.ts` 中显示为“已审批”。  
   - 采购结算 `pending` 在类型注释中为“待付款”，在页面选项中为“待结算”。  
   - `[待修复]` 统一中文标签来源，避免同一状态在不同组件显示不同文案。

---

## 4. 金额与 ID 转换规则

### 4.1 金额转换

| 单据 | 后端存储 | 前端显示/表单 | 转换位置 | 转换方式 |
|-----|---------|--------------|---------|---------|
| 采购申请 | `totalAmount` Long 分 | `totalAmount` number 元 | `api/purchase/request.ts` | `/ 100` |
| 采购申请明细 | `estimatedPrice` / `subtotalAmount` BigDecimal 元 | 同上 元 | 直接序列化为 number | 无转换 |
| 采购计划 | `totalAmount` Long 分 | `totalAmount` number 元 | `api/purchase/plan.ts` | `fenToYuanNumber` |
| 采购计划明细 | `estimatedPrice` 元 | `estimatedPrice` number 元 | 直接序列化为 number | 无转换 |
| 采购订单 | `totalAmount` Long 分 | `totalAmount` number 元 | `api/purchase/order.ts` | `fenToYuanNumber` |
| 采购订单明细 | `unitPrice` / `amount` Long 分 | 元 | `api/purchase/order.ts` | `fenToYuanNumber` |
| 到货登记 | `totalAmount` / `freightAmount` Long 分 | 元 | `api/purchase/arrival.ts` | `fenToYuanNumber` |
| 到货登记明细 | `amount` / `unitPrice` Long 分 | 元 | `api/purchase/arrival.ts` | `fenToYuanNumber` |
| 采购结算 | `totalAmount` / `paidAmount` / `unpaidAmount` Long 分 | 元 | `api/purchase/settlement.ts` | `fenToYuanNumber` |

### 金额转换不一致点

1. **采购申请总金额与其他单据转换方式不同**  
   - 采购申请使用 `/ 100`，其余使用 `utils/money` 的 `fenToYuanNumber`。  
   - `[待修复]` 统一使用 `utils/money` 工具函数。

2. **采购申请明细金额未做元分转换**  
   - 注释说明明细 `estimatedPrice` / `subtotalAmount` 为 BigDecimal 元，但与其他单据明细（Long 分）不一致。  
   - `[待修复]` 需确认后端实际存储单位，避免前端汇总与后端不一致。

### 4.2 ID 转换

| 单据 | 后端类型 | 前端类型 | 转换方式 |
|-----|---------|---------|---------|
| 采购申请 | UUID string | string | 无转换 |
| 采购计划 | number / BigInt / Long | string | `String()` |
| 采购订单 | number (Long) | string | `String()` |
| 采购订单明细 | number | number | 无转换 |
| 到货登记 | number | string | `String()` |
| 到货登记明细 | number | string | `String()` |
| 采购结算 | number | string | `String()` |

### ID 不一致点

1. **采购申请使用 UUID，其余单据使用数字ID**  
   - `[待修复]` 建议统一主键策略：对外暴露均使用字符串，内部存储按需使用 UUID 或雪花ID。

---

## 5. 通用字段与扩展字段

### 5.1 时间字段

| 字段 | 类型 | 说明 |
|-----|------|------|
| `createTime` | ISO 8601 string / `yyyy-MM-dd HH:mm:ss` | 创建时间 |
| `updateTime` | ISO 8601 string / `yyyy-MM-dd HH:mm:ss` | 更新时间 |
| `approvedTime` / `approveTime` | ISO 8601 string | 审批时间（命名待统一） |
| `deletedTime` | ISO 8601 string | 删除时间（部分单据缺失） |
| `expectedDate` / `planDate` / `dueDate` | `yyyy-MM-dd` | 业务日期 |

### 5.2 冗余字段

| 字段 | 位置 | 说明 |
|-----|------|------|
| `requestNo` / `orderNo` / `orderCode` | 订单/到货/结算 | 来源单据编号冗余，便于列表展示 |
| `supplierName` | 订单/到货/结算 | 供应商名称冗余 |
| `departmentName` | 申请/计划 | 部门名称冗余 |
| `applicantName` | 申请 | 申请人名称冗余 |

---

## 6. 字段命名规范建议

| 层级 | 当前问题 | 建议 |
|-----|---------|------|
| 数据库 | 表名/字段名使用 snake_case | 保持 `purchase_request`、`request_no`、`total_amount` |
| Java 实体 | 驼峰命名 | `requestNo`、`totalAmount` |
| 前端类型 | 驼峰命名 | `requestNo`、`totalAmount` |
| 采购链路物料 | `food` / `product` / `material` 混用 | 统一为 `materialId` / `materialName` / `materialCode` |
| 申请人/创建人 | `applicant` / `creator` / `createBy` 混用 | 统一为 `creatorId` / `creatorName`，申请人语义在申请单中额外使用 `applicantId` / `applicantName` |
| 审批时间 | `approvedTime` / `approveTime` | 统一为 `approvedBy` / `approvedTime` |
| 金额 | 部分分、部分元 | 后端统一存分，前端统一显示元，API 层统一转换 |

---

## 7. 备注清单（[待修复]汇总）

1. `[待修复]` 采购订单类型 `PurchaseOrderInfo` 中大量字段后端未返回，当前被硬编码为空/0。
2. `[待修复]` 采购订单前端 11 个状态与后端 7 个状态不匹配，`shipped`/`received`/`rejected`/`terminated` 为近似映射。
3. `[待修复]` 采购链路物料字段命名不统一：`foodId/foodName`、`productId/productName`、`materialId/materialName`。
4. `[待修复]` 单价/金额字段命名不统一：`estimatedPrice` / `unitPrice` / `subtotalAmount` / `amount`。
5. `[待修复]` 采购申请状态使用语义字符串，其余单据使用数字编码，策略不一致。
6. `[待修复]` 采购申请总金额使用 `/ 100`，其余使用 `utils/money`，转换方式不一致。
7. `[待修复]` 采购申请明细金额单位为元（BigDecimal），其余单据明细为分（Long），需确认后端实际单位。
8. `[待修复]` 审批字段命名：`approvedBy` / `approvedTime`（申请） vs `approveBy` / `approveTime`（计划）。
9. `[待修复]` 采购订单 `approved` 中文标签在状态选项和 `converters.ts` 中不一致（已审核 vs 已审批）。
10. `[待修复]` 采购结算 `pending` 中文标签不一致（待付款 vs 待结算）。
