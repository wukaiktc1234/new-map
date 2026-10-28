# E 批 2/3：实体字段 → @TableField → DDL 列 三列对照（2026-10-28）

说明：`@TableField` 列为空表示**实体未加注解，走 MyBatis-Plus 驼峰转下划线默认映射**（等价列名已在第三列给出）。

## `self_purchase`（@TableName = `self_purchase`，实体 19 字段 / DDL 20 列）

| # | 实体字段 | @TableField 名 | DDL 列名 | 一致 |
|---|----------|----------------|----------|------|
| 1 | `selfPurchaseId` | *(默认映射)* | `self_purchase_id` | ✔ |
| 2 | `purchaseNumber` | *(默认映射)* | `purchase_number` | ✔ |
| 3 | `purchaseType` | *(默认映射)* | `purchase_type` | ✔ |
| 4 | `purchaseReason` | *(默认映射)* | `purchase_reason` | ✔ |
| 5 | `totalAmount` | *(默认映射)* | `total_amount` | ✔ |
| 6 | `purchaseDate` | *(默认映射)* | `purchase_date` | ✔ |
| 7 | `purchaser` | *(默认映射)* | `purchaser` | ✔ |
| 8 | `purchaserPhone` | *(默认映射)* | `purchaser_phone` | ✔ |
| 9 | `purchaseLocation` | *(默认映射)* | `purchase_location` | ✔ |
| 10 | `warehouseId` | *(默认映射)* | `warehouse_id` | ✔ |
| 11 | `warehouseName` | *(默认映射)* | `warehouse_name` | ✔ |
| 12 | `status` | *(默认映射)* | `status` | ✔ |
| 13 | `reimburseStatus` | *(默认映射)* | `reimburse_status` | ✔ |
| 14 | `remark` | *(默认映射)* | `remark` | ✔ |
| 15 | `createTime` | *(默认映射)* | `create_time` | ✔ |
| 16 | `updateTime` | *(默认映射)* | `update_time` | ✔ |
| 17 | `createBy` | *(默认映射)* | `create_by` | ✔ |
| 18 | `updateBy` | *(默认映射)* | `update_by` | ✔ |
| 19 | `deleted` | *(默认映射)* | `deleted` | ✔ |

→ 实体→DDL：19/19 命中；DDL 多出列：`total_amount_fen`

## `self_purchase_item`（@TableName = `self_purchase_item`，实体 14 字段 / DDL 16 列）

| # | 实体字段 | @TableField 名 | DDL 列名 | 一致 |
|---|----------|----------------|----------|------|
| 1 | `selfPurchaseItemId` | *(默认映射)* | `self_purchase_item_id` | ✔ |
| 2 | `selfPurchaseId` | *(默认映射)* | `self_purchase_id` | ✔ |
| 3 | `productId` | *(默认映射)* | `product_id` | ✔ |
| 4 | `productName` | *(默认映射)* | `product_name` | ✔ |
| 5 | `specification` | *(默认映射)* | `specification` | ✔ |
| 6 | `unit` | *(默认映射)* | `unit` | ✔ |
| 7 | `quantity` | *(默认映射)* | `quantity` | ✔ |
| 8 | `unitPrice` | *(默认映射)* | `unit_price` | ✔ |
| 9 | `subtotalAmount` | *(默认映射)* | `subtotal_amount` | ✔ |
| 10 | `status` | *(默认映射)* | `status` | ✔ |
| 11 | `traceCodeStatus` | *(默认映射)* | `trace_code_status` | ✔ |
| 12 | `createTime` | *(默认映射)* | `create_time` | ✔ |
| 13 | `updateTime` | *(默认映射)* | `update_time` | ✔ |
| 14 | `deleted` | *(默认映射)* | `deleted` | ✔ |

→ 实体→DDL：14/14 命中；DDL 多出列：`unit_price_fen`, `subtotal_amount_fen`

## `inventory_code`

> **已知缺口（实体暂无该字段，查询不带 deleted 过滤）**：本表 DDL 按新标准建了
> `create_time` / `update_time` / `deleted` 三列，但实体 `InventoryCode` **没有对应字段**。
> **不为此给实体加字段**（加字段会改变 Mapper 行为）；当前这三列不会被读写。
（@TableName = `inventory_code`，实体 10 字段 / DDL 13 列）

| # | 实体字段 | @TableField 名 | DDL 列名 | 一致 |
|---|----------|----------------|----------|------|
| 1 | `id` | *(默认映射)* | `id` | ✔ |
| 2 | `inventoryId` | *(默认映射)* | `inventory_id` | ✔ |
| 3 | `codeType` | *(默认映射)* | `code_type` | ✔ |
| 4 | `uniqueCode` | *(默认映射)* | `unique_code` | ✔ |
| 5 | `batchNo` | *(默认映射)* | `batch_no` | ✔ |
| 6 | `qrCode` | *(默认映射)* | `qr_code` | ✔ |
| 7 | `status` | *(默认映射)* | `status` | ✔ |
| 8 | `storeId` | *(默认映射)* | `store_id` | ✔ |
| 9 | `createdBy` | *(默认映射)* | `created_by` | ✔ |
| 10 | `updatedBy` | *(默认映射)* | `updated_by` | ✔ |

→ 实体→DDL：10/10 命中；DDL 多出列：`create_time`, `update_time`, `deleted`

## `plan_item`（@TableName = `plan_item`，实体 14 字段 / DDL 14 列）

| # | 实体字段 | @TableField 名 | DDL 列名 | 一致 |
|---|----------|----------------|----------|------|
| 1 | `id` | *(默认映射)* | `id` | ✔ |
| 2 | `title` | *(默认映射)* | `title` | ✔ |
| 3 | `description` | *(默认映射)* | `description` | ✔ |
| 4 | `type` | *(默认映射)* | `type` | ✔ |
| 5 | `priority` | *(默认映射)* | `priority` | ✔ |
| 6 | `status` | *(默认映射)* | `status` | ✔ |
| 7 | `assignee` | *(默认映射)* | `assignee` | ✔ |
| 8 | `startTime` | *(默认映射)* | `start_time` | ✔ |
| 9 | `endTime` | *(默认映射)* | `end_time` | ✔ |
| 10 | `completeTime` | *(默认映射)* | `complete_time` | ✔ |
| 11 | `remark` | *(默认映射)* | `remark` | ✔ |
| 12 | `createTime` | *(默认映射)* | `create_time` | ✔ |
| 13 | `updateTime` | *(默认映射)* | `update_time` | ✔ |
| 14 | `deleted` | *(默认映射)* | `deleted` | ✔ |

→ 实体→DDL：14/14 命中；DDL 多出列：无

## `receivable`（@TableName = `receivable`，实体 18 字段 / DDL 22 列）

| # | 实体字段 | @TableField 名 | DDL 列名 | 一致 |
|---|----------|----------------|----------|------|
| 1 | `id` | `id` | `id` | ✔ |
| 2 | `orderId` | *(默认映射)* | `order_id` | ✔ |
| 3 | `orderNo` | *(默认映射)* | `order_no` | ✔ |
| 4 | `customerId` | *(默认映射)* | `customer_id` | ✔ |
| 5 | `customerName` | *(默认映射)* | `customer_name` | ✔ |
| 6 | `customerType` | *(默认映射)* | `customer_type` | ✔ |
| 7 | `amount` | *(默认映射)* | `amount` | ✔ |
| 8 | `receivedAmount` | *(默认映射)* | `received_amount` | ✔ |
| 9 | `remainingAmount` | *(默认映射)* | `remaining_amount` | ✔ |
| 10 | `invoiceId` | *(默认映射)* | `invoice_id` | ✔ |
| 11 | `invoiceNo` | *(默认映射)* | `invoice_no` | ✔ |
| 12 | `status` | *(默认映射)* | `status` | ✔ |
| 13 | `dueDate` | *(默认映射)* | `due_date` | ✔ |
| 14 | `overdueDays` | *(默认映射)* | `overdue_days` | ✔ |
| 15 | `lastReceivedDate` | *(默认映射)* | `last_received_date` | ✔ |
| 16 | `createTime` | *(默认映射)* | `create_time` | ✔ |
| 17 | `updateTime` | *(默认映射)* | `update_time` | ✔ |
| 18 | `remark` | *(默认映射)* | `remark` | ✔ |

→ 实体→DDL：18/18 命中；DDL 多出列：`amount_fen`, `received_amount_fen`, `remaining_amount_fen`, `deleted`

## `requisition_requests`（@TableName = `requisition_requests`，实体 18 字段 / DDL 20 列）

| # | 实体字段 | @TableField 名 | DDL 列名 | 一致 |
|---|----------|----------------|----------|------|
| 1 | `id` | *(默认映射)* | `id` | ✔ |
| 2 | `approvalId` | *(默认映射)* | `approval_id` | ✔ |
| 3 | `employeeId` | *(默认映射)* | `employee_id` | ✔ |
| 4 | `itemName` | *(默认映射)* | `item_name` | ✔ |
| 5 | `categoryId` | *(默认映射)* | `category_id` | ✔ |
| 6 | `quantity` | *(默认映射)* | `quantity` | ✔ |
| 7 | `unit` | *(默认映射)* | `unit` | ✔ |
| 8 | `purpose` | *(默认映射)* | `purpose` | ✔ |
| 9 | `supplierPreference` | *(默认映射)* | `supplier_preference` | ✔ |
| 10 | `urgencyLevel` | *(默认映射)* | `urgency_level` | ✔ |
| 11 | `expectedDate` | *(默认映射)* | `expected_date` | ✔ |
| 12 | `departmentBudget` | *(默认映射)* | `department_budget` | ✔ |
| 13 | `estimatedCost` | *(默认映射)* | `estimated_cost` | ✔ |
| 14 | `createdTime` | *(默认映射)* | `created_time` | ✔ |
| 15 | `updatedTime` | *(默认映射)* | `updated_time` | ✔ |
| 16 | `deleted` | *(默认映射)* | `deleted` | ✔ |
| 17 | `createdBy` | *(默认映射)* | `created_by` | ✔ |
| 18 | `updatedBy` | *(默认映射)* | `updated_by` | ✔ |

→ 实体→DDL：18/18 命中；DDL 多出列：`department_budget_fen`, `estimated_cost_fen`

## `swap_request_details`（@TableName = `swap_request_details`，实体 18 字段 / DDL 18 列）

| # | 实体字段 | @TableField 名 | DDL 列名 | 一致 |
|---|----------|----------------|----------|------|
| 1 | `id` | *(默认映射)* | `id` | ✔ |
| 2 | `approvalId` | *(默认映射)* | `approval_id` | ✔ |
| 3 | `initiatorId` | *(默认映射)* | `initiator_id` | ✔ |
| 4 | `targetEmployeeId` | *(默认映射)* | `target_employee_id` | ✔ |
| 5 | `targetEmployeeName` | *(默认映射)* | `target_employee_name` | ✔ |
| 6 | `originalDate` | *(默认映射)* | `original_date` | ✔ |
| 7 | `originalShiftType` | *(默认映射)* | `original_shift_type` | ✔ |
| 8 | `targetDate` | *(默认映射)* | `target_date` | ✔ |
| 9 | `targetShiftType` | *(默认映射)* | `target_shift_type` | ✔ |
| 10 | `partnerConfirmed` | *(默认映射)* | `partner_confirmed` | ✔ |
| 11 | `confirmTime` | *(默认映射)* | `confirm_time` | ✔ |
| 12 | `coveragePlan` | *(默认映射)* | `coverage_plan` | ✔ |
| 13 | `reason` | *(默认映射)* | `reason` | ✔ |
| 14 | `createdTime` | *(默认映射)* | `created_time` | ✔ |
| 15 | `updatedTime` | *(默认映射)* | `updated_time` | ✔ |
| 16 | `deleted` | *(默认映射)* | `deleted` | ✔ |
| 17 | `createdBy` | *(默认映射)* | `created_by` | ✔ |
| 18 | `updatedBy` | *(默认映射)* | `updated_by` | ✔ |

→ 实体→DDL：18/18 命中；DDL 多出列：无

## `travel_requests`（@TableName = `travel_requests`，实体 21 字段 / DDL 25 列）

| # | 实体字段 | @TableField 名 | DDL 列名 | 一致 |
|---|----------|----------------|----------|------|
| 1 | `id` | *(默认映射)* | `id` | ✔ |
| 2 | `approvalId` | *(默认映射)* | `approval_id` | ✔ |
| 3 | `employeeId` | *(默认映射)* | `employee_id` | ✔ |
| 4 | `destination` | *(默认映射)* | `destination` | ✔ |
| 5 | `travelPurpose` | *(默认映射)* | `travel_purpose` | ✔ |
| 6 | `startDate` | *(默认映射)* | `start_date` | ✔ |
| 7 | `endDate` | *(默认映射)* | `end_date` | ✔ |
| 8 | `days` | *(默认映射)* | `days` | ✔ |
| 9 | `estimatedBudget` | *(默认映射)* | `estimated_budget` | ✔ |
| 10 | `actualAmount` | *(默认映射)* | `actual_amount` | ✔ |
| 11 | `advancePayment` | *(默认映射)* | `advance_payment` | ✔ |
| 12 | `transportType` | *(默认映射)* | `transport_type` | ✔ |
| 13 | `hotelRequired` | *(默认映射)* | `hotel_required` | ✔ |
| 14 | `hotelCity` | *(默认映射)* | `hotel_city` | ✔ |
| 15 | `relatedTaskId` | *(默认映射)* | `related_task_id` | ✔ |
| 16 | `dailyAllowance` | *(默认映射)* | `daily_allowance` | ✔ |
| 17 | `createdTime` | *(默认映射)* | `created_time` | ✔ |
| 18 | `updatedTime` | *(默认映射)* | `updated_time` | ✔ |
| 19 | `deleted` | *(默认映射)* | `deleted` | ✔ |
| 20 | `createdBy` | *(默认映射)* | `created_by` | ✔ |
| 21 | `updatedBy` | *(默认映射)* | `updated_by` | ✔ |

→ 实体→DDL：21/21 命中；DDL 多出列：`estimated_budget_fen`, `actual_amount_fen`, `advance_payment_fen`, `daily_allowance_fen`

## `weighing_record`（@TableName = `weighing_record`，实体 17 字段 / DDL 17 列）

| # | 实体字段 | @TableField 名 | DDL 列名 | 一致 |
|---|----------|----------------|----------|------|
| 1 | `id` | `id` | `id` | ✔ |
| 2 | `deviceId` | `device_id` | `device_id` | ✔ |
| 3 | `deviceCode` | `device_code` | `device_code` | ✔ |
| 4 | `weighTime` | `weigh_time` | `weigh_time` | ✔ |
| 5 | `weightValue` | `weight_value` | `weight_value` | ✔ |
| 6 | `unit` | `unit` | `unit` | ✔ |
| 7 | `businessType` | `business_type` | `business_type` | ✔ |
| 8 | `businessId` | `business_id` | `business_id` | ✔ |
| 9 | `productId` | `product_id` | `product_id` | ✔ |
| 10 | `productName` | `product_name` | `product_name` | ✔ |
| 11 | `operator` | `operator` | `operator` | ✔ |
| 12 | `remark` | `remark` | `remark` | ✔ |
| 13 | `createTime` | `create_time` | `create_time` | ✔ |
| 14 | `updateTime` | `update_time` | `update_time` | ✔ |
| 15 | `createBy` | `create_by` | `create_by` | ✔ |
| 16 | `updateBy` | `update_by` | `update_by` | ✔ |
| 17 | `deleted` | `deleted` | `deleted` | ✔ |

→ 实体→DDL：17/17 命中；DDL 多出列：无

