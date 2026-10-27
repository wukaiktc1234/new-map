# 501 覆盖率核对（缺表 58 张）

| 类别 | 数量 |
|------|------|
| 已覆盖（501） | 30 |
| **未覆盖（仍 500）** | **2** |
| 无入口（HTTP 打不到） | 33 |

## 未覆盖清单（需补注解）

| 表 | Controller |
|----|------------|
| `material_usage_record` | KitchenScanController |
| `operation_logs` | OperationLogController |

## 无入口清单（无需注解）

| 表 | 实体 |
|----|------|
| `cost_allocation` | CostAllocation |
| `cost_allocation_detail` | CostAllocationDetail |
| `cost_allocation_rule` | CostAllocationRule |
| `data_link_event` | DataLinkEvent |
| `dish_ingredient` | DishIngredient |
| `finance_record_audit_log` | FinanceRecordAuditLog |
| `finance_report` | FinanceReport |
| `food_trace` | FoodTrace |
| `inventory_code` | InventoryCode |
| `inventory_transactions` | InventoryTransaction |
| `inventory_transfer_detail` | InventoryTransferDetail |
| `invitation_expense_record` | InvitationExpenseRecord |
| `invitation_reminder_log` | InvitationReminderLog |
| `locker_slot` | LockerSlot |
| `ocr_record` | OcrRecord |
| `operation_type` | OperationType |
| `requisition_requests` | RequisitionRequestEntity |
| `role_departments` | RoleDepartment |
| `role_stores` | RoleStore |
| `sales_order_detail` | SalesOrderDetail |
| `scan_record` | ScanRecord |
| `self_purchase_item` | SelfPurchaseItem |
| `swap_request_details` | SwapRequestDetailEntity |
| `sys_devices` | SysDevice |
| `sys_permissions` | SysPermission |
| `sys_print_templates` | SysPrintTemplate |
| `sys_roles` | SysRole |
| `sys_stores` | SysStore |
| `trace_code_log` | TraceCodeLog |
| `trace_stage` | TraceStage |
| `traceability_code` | TraceabilityCode |
| `travel_requests` | TravelRequestEntity |
| `weighing_record` | WeighingRecord |
