# 缺表收敛明细（基线 → 当前）

基线 59 张 → 当前缺 59 张 ｜ 已补齐 **0** 张 ｜ 仍缺 **59** 张

判据：补齐 = 该表已存在于 `information_schema.tables`；『有写入』只认 Mapper 实例调用（2026-10-28 更正）。

## 已补齐（0 张）

| 表名 | 实体 | 分类 |
|---|---|---|

## 仍缺（59 张）

| 表名 | 实体 | 分类 |
|---|---|---|
| `category_operation_log` | `CategoryOperationLog.java` | 外围功能先下线（无写入无调用） |
| `contract_document` | `ContractDocument.java` | 外围功能先下线（无写入无调用） |
| `cost_allocation` | `CostAllocation.java` | 外围功能先下线（无写入无调用） |
| `cost_allocation_detail` | `CostAllocationDetail.java` | 外围功能先下线（无写入无调用） |
| `cost_allocation_rule` | `CostAllocationRule.java` | 外围功能先下线（无写入无调用） |
| `data_change_history` | `DataChangeHistory.java` | 外围功能先下线（无写入无调用） |
| `data_link_event` | `DataLinkEvent.java` | 外围功能先下线（无写入无调用） |
| `department_permissions` | `DepartmentPermission.java` | 外围功能先下线（无写入无调用） |
| `dish_ingredient` | `DishIngredient.java` | 核心业务必做（有写入） |
| `file_attachment` | `FileAttachment.java` | 核心业务必做（有写入） |
| `finance_account` | `FinanceAccount.java` | 外围功能先下线（前端半成品调用） |
| `finance_approval` | `FinanceApproval.java` | 核心业务必做（有写入） |
| `finance_record_audit_log` | `FinanceRecordAuditLog.java` | 外围功能先下线（无写入无调用） |
| `finance_report` | `FinanceReport.java` | 外围功能先下线（前端半成品调用） |
| `finance_report_detail` | `FinanceReportDetail.java` | 外围功能先下线（无写入无调用） |
| `food_trace` | `FoodTrace.java` | 实体指向空表（登记，不修） |
| `hardware_config` | `HardwareConfig.java` | 核心业务必做（有写入） |
| `hardware_config_version` | `HardwareConfigVersion.java` | 核心业务必做（有写入） |
| `hardware_operation_log` | `HardwareOperationLog.java` | 核心业务必做（有写入） |
| `inventory_category` | `InventoryCategory.java` | 外围功能先下线（无写入无调用） |
| `inventory_code` | `InventoryCode.java` | 核心业务必做（有写入） |
| `inventory_log` | `InventoryLog.java` | 废弃表直接删 |
| `inventory_transactions` | `InventoryTransaction.java` | 外围功能先下线（无写入无调用） |
| `inventory_transfer_detail` | `InventoryTransferDetail.java` | 外围功能先下线（无写入无调用） |
| `inventory_unit` | `InventoryUnit.java` | 外围功能先下线（无写入无调用） |
| `invitation_expense_record` | `InvitationExpenseRecord.java` | 外围功能先下线（无写入无调用） |
| `invitation_reminder_log` | `InvitationReminderLog.java` | 外围功能先下线（无写入无调用） |
| `ocr_record` | `OcrRecord.java` | 外围功能先下线（无写入无调用） |
| `operation_logs` | `OperationLog.java` | 核心业务必做（有写入） |
| `operation_type` | `OperationType.java` | 废弃表直接删 |
| `plan_item` | `PlanItem.java` | 外围功能先下线（前端半成品调用） |
| `print_format_template` | `PrintFormatTemplate.java` | 核心业务必做（有写入） |
| `receivable` | `Receivable.java` | 核心业务必做（有写入+前端调用） |
| `registration_code_log` | `RegistrationCodeLog.java` | 核心业务必做（有写入） |
| `requisition_requests` | `RequisitionRequestEntity.java` | 外围功能先下线（无写入无调用） |
| `role_departments` | `RoleDepartment.java` | 核心业务必做（有写入） |
| `role_stores` | `RoleStore.java` | 核心业务必做（有写入） |
| `salary_adjustment` | `SalaryAdjustment.java` | 外围功能先下线（无写入无调用） |
| `salary_record` | `SalaryRecord.java` | 外围功能先下线（无写入无调用） |
| `salary_rule` | `SalaryRule.java` | 外围功能先下线（无写入无调用） |
| `sales_order` | `SalesOrder.java` | 核心业务必做（有写入+前端调用） |
| `sales_order_detail` | `SalesOrderDetail.java` | 外围功能先下线（无写入无调用） |
| `scan_device` | `ScanDevice.java` | 外围功能先下线（无写入无调用） |
| `self_purchase` | `SelfPurchase.java` | 外围功能先下线（无写入无调用） |
| `self_purchase_item` | `SelfPurchaseItem.java` | 核心业务必做（有写入） |
| `store_inventory_log` | `StoreInventoryLog.java` | 废弃表直接删 |
| `swap_request_details` | `SwapRequestDetailEntity.java` | 外围功能先下线（无写入无调用） |
| `sys_devices` | `SysDevice.java` | 外围功能先下线（无写入无调用） |
| `sys_permissions` | `SysPermission.java` | 废弃表直接删 |
| `sys_print_templates` | `SysPrintTemplate.java` | 外围功能先下线（无写入无调用） |
| `sys_roles` | `SysRole.java` | 废弃表直接删 |
| `sys_stores` | `SysStore.java` | 废弃表直接删 |
| `trace_stage` | `TraceStage.java` | 外围功能先下线（无写入无调用） |
| `traceability_code` | `TraceabilityCode.java` | 外围功能先下线（无写入无调用） |
| `travel_requests` | `TravelRequestEntity.java` | 外围功能先下线（无写入无调用） |
| `user_sessions` | `UserSession.java` | 核心业务必做（有写入） |
| `v_inventory_summary` | `InventorySummary.java` | 废弃表直接删 |
| `weighing_device` | `WeighingDevice.java` | 外围功能先下线（无写入无调用） |
| `weighing_record` | `WeighingRecord.java` | 核心业务必做（有写入） |

