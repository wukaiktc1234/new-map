# 缺表分类清单（实体有 @TableName、信息模式里不存在）

生成时间：2026-10-28 13:35 ｜ 口径：`backend/**/entity/*.java` 的 @TableName vs `information_schema.tables`

判据说明：
- 写入点：`backend/**/*.java` 中同时提到该实体类/Mapper 且出现 `.insert/.update/.delete/.save/.updateById/.deleteById` 的文件（取首个命中行）；XML Mapper 里 `<insert/update/delete>` 或 `INSERT INTO <table>` 亦计为写入。
- 前端调用：`frontend/src`、`frontend-pos/src` 里出现表名 / kebab 形式（`a_b` → `a-b`）的文件。
- **核心业务必做**：有写入点，且（前端有调用 或 业务明显需要）；**外围功能先下线**：无写入点 / 未闭环；**废弃表直接删**：被新模型取代或仅视图。

## 核心业务必做（28 张）

| 表名 | 实体 | 写入点 | 前端调用 | 判据 |
|---|---|---|---|---|
| `contract_document` | `com\foodtraceability\entity\ContractDocument.java` | `com\foodtraceability\service\impl\ContractDocumentServiceImpl.java:152` | 无 | 有写入(Java 1/XML 0)，前端未直接调用 |
| `department_permissions` | `com\foodtraceability\entity\DepartmentPermission.java` | `com\foodtraceability\service\impl\DepartmentPermissionServiceImpl.java:72` | 无 | 有写入(Java 1/XML 0)，前端未直接调用 |
| `dish_ingredient` | `com\foodtraceability\entity\DishIngredient.java` | `com\foodtraceability\service\trace\impl\FoodTraceabilityServiceImpl.java:410` | 无 | 有写入(Java 1/XML 0)，前端未直接调用 |
| `file_attachment` | `com\foodtraceability\entity\FileAttachment.java` | `com\foodtraceability\service\impl\FileAttachmentServiceImpl.java:111` | 无 | 有写入(Java 2/XML 1)，前端未直接调用 |
| `finance_approval` | `com\foodtraceability\entity\finance\FinanceApproval.java` | `com\foodtraceability\service\impl\FinanceApprovalServiceImpl.java:61` | 无 | 有写入(Java 2/XML 0)，前端未直接调用 |
| `finance_report` | `com\foodtraceability\entity\finance\FinanceReport.java` | `com\foodtraceability\service\impl\FinanceReportDetailServiceImpl.java:93` | `types\common-types.ts:7` | 有写入(Java 1/XML 0) + 前端有调用(2) |
| `finance_report_detail` | `com\foodtraceability\entity\finance\FinanceReportDetail.java` | `com\foodtraceability\service\impl\FinanceReportDetailServiceImpl.java:93` | 无 | 有写入(Java 1/XML 0)，前端未直接调用 |
| `hardware_config` | `com\foodtraceability\entity\HardwareConfig.java` | `com\foodtraceability\controller\LabelTemplateController.java:185` | 无 | 有写入(Java 6/XML 0)，前端未直接调用 |
| `hardware_config_version` | `com\foodtraceability\entity\HardwareConfigVersion.java` | `com\foodtraceability\service\impl\HardwareConfigServiceImpl.java:152` | 无 | 有写入(Java 2/XML 0)，前端未直接调用 |
| `hardware_operation_log` | `com\foodtraceability\entity\HardwareOperationLog.java` | `com\foodtraceability\service\impl\HardwareConfigServiceImpl.java:152` | 无 | 有写入(Java 2/XML 0)，前端未直接调用 |
| `inventory_code` | `com\foodtraceability\entity\InventoryCode.java` | `com\foodtraceability\config\DatabaseFixConfig.java:626` | 无 | 有写入(Java 3/XML 1)，前端未直接调用 |
| `operation_logs` | `com\foodtraceability\entity\OperationLog.java` | `com\foodtraceability\aspect\FinanceRecordOperationAspect.java:174` | 无 | 有写入(Java 10/XML 0)，前端未直接调用 |
| `plan_item` | `com\foodtraceability\entity\PlanItem.java` | `com\foodtraceability\service\purchase\impl\PurchasePlanServiceImpl.java:192` | `views\purchase\PurchasePlan.vue:1008` | 有写入(Java 1/XML 0) + 前端有调用(1) |
| `receivable` | `com\foodtraceability\entity\Receivable.java` | `com\foodtraceability\service\impl\PosOrderPaymentServiceImpl.java:216` | `router\index.ts:113` | 有写入(Java 3/XML 0) + 前端有调用(13) |
| `registration_code_log` | `com\foodtraceability\entity\RegistrationCodeLog.java` | `com\foodtraceability\service\impl\RegistrationCodeServiceImpl.java:92` | 无 | 有写入(Java 1/XML 0)，前端未直接调用 |
| `requisition_requests` | `com\foodtraceability\entity\approval\RequisitionRequestEntity.java` | `com\foodtraceability\service\approval\impl\EmployeeApprovalServiceImpl.java:259` | 无 | 有写入(Java 1/XML 0)，前端未直接调用 |
| `role_departments` | `com\foodtraceability\entity\RoleDepartment.java` | `com\foodtraceability\service\impl\SystemInitServiceImpl.java:89` | 无 | 有写入(Java 1/XML 1)，前端未直接调用 |
| `role_stores` | `com\foodtraceability\entity\RoleStore.java` | `com\foodtraceability\service\impl\SystemInitServiceImpl.java:89` | 无 | 有写入(Java 1/XML 1)，前端未直接调用 |
| `sales_order` | `com\foodtraceability\entity\SalesOrder.java` | `com\foodtraceability\service\impl\SalesOrderServiceImpl.java:82` | `api\dashboard\index.ts:44` | 有写入(Java 1/XML 1) + 前端有调用(1) |
| `sales_order_detail` | `com\foodtraceability\entity\SalesOrderDetail.java` | `com\foodtraceability\service\impl\SalesOrderServiceImpl.java:82` | 无 | 有写入(Java 1/XML 1)，前端未直接调用 |
| `scan_device` | `com\foodtraceability\entity\ScanDevice.java` | `com\foodtraceability\service\impl\ScanDeviceServiceImpl.java:273` | 无 | 有写入(Java 2/XML 0)，前端未直接调用 |
| `self_purchase` | `com\foodtraceability\entity\SelfPurchase.java` | `com\foodtraceability\service\impl\SelfPurchaseServiceImpl.java:67` | 无 | 有写入(Java 1/XML 0)，前端未直接调用 |
| `self_purchase_item` | `com\foodtraceability\entity\SelfPurchaseItem.java` | `com\foodtraceability\service\impl\SelfPurchaseServiceImpl.java:67` | 无 | 有写入(Java 1/XML 0)，前端未直接调用 |
| `swap_request_details` | `com\foodtraceability\entity\approval\SwapRequestDetailEntity.java` | `com\foodtraceability\service\approval\impl\EmployeeApprovalServiceImpl.java:259` | 无 | 有写入(Java 1/XML 0)，前端未直接调用 |
| `travel_requests` | `com\foodtraceability\entity\approval\TravelRequestEntity.java` | `com\foodtraceability\service\approval\impl\EmployeeApprovalServiceImpl.java:259` | 无 | 有写入(Java 1/XML 0)，前端未直接调用 |
| `user_sessions` | `com\foodtraceability\entity\UserSession.java` | `com\foodtraceability\service\impl\UserSessionServiceImpl.java:54` | 无 | 有写入(Java 1/XML 0)，前端未直接调用 |
| `weighing_device` | `com\foodtraceability\entity\WeighingDevice.java` | `com\foodtraceability\service\impl\WeighingDeviceServiceImpl.java:197` | 无 | 有写入(Java 1/XML 0)，前端未直接调用 |
| `weighing_record` | `com\foodtraceability\entity\WeighingRecord.java` | `com\foodtraceability\service\impl\WeighingDeviceServiceImpl.java:197` | 无 | 有写入(Java 1/XML 0)，前端未直接调用 |

## 外围功能先下线（23 张）

| 表名 | 实体 | 写入点 | 前端调用 | 判据 |
|---|---|---|---|---|
| `category_operation_log` | `com\foodtraceability\entity\CategoryOperationLog.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `cost_allocation` | `com\foodtraceability\entity\CostAllocation.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `cost_allocation_detail` | `com\foodtraceability\entity\CostAllocationDetail.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `cost_allocation_rule` | `com\foodtraceability\entity\CostAllocationRule.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `data_change_history` | `com\foodtraceability\entity\DataChangeHistory.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `data_link_event` | `com\foodtraceability\entity\DataLinkEvent.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `finance_account` | `com\foodtraceability\entity\finance\FinanceAccount.java` | 无 | `types\finance.ts:8` | 前端有调用(1) 但后端无写入 → 未闭环 |
| `finance_record_audit_log` | `com\foodtraceability\entity\finance\FinanceRecordAuditLog.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `inventory_category` | `com\foodtraceability\entity\InventoryCategory.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `inventory_transactions` | `com\foodtraceability\entity\InventoryTransaction.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `inventory_transfer_detail` | `com\foodtraceability\entity\InventoryTransferDetail.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `inventory_unit` | `com\foodtraceability\entity\InventoryUnit.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `invitation_expense_record` | `com\foodtraceability\entity\InvitationExpenseRecord.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `invitation_reminder_log` | `com\foodtraceability\entity\InvitationReminderLog.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `ocr_record` | `com\foodtraceability\entity\OcrRecord.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `print_format_template` | `com\foodtraceability\entity\PrintFormatTemplate.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `salary_adjustment` | `com\foodtraceability\entity\SalaryAdjustment.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `salary_record` | `com\foodtraceability\entity\SalaryRecord.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `salary_rule` | `com\foodtraceability\entity\SalaryRule.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `sys_devices` | `com\foodtraceability\entity\SysDevice.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `sys_print_templates` | `com\foodtraceability\entity\SysPrintTemplate.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `trace_stage` | `com\foodtraceability\entity\TraceStage.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |
| `traceability_code` | `com\foodtraceability\entity\TraceabilityCode.java` | 无 | 无 | 无写入、无前端调用（未上线/死代码） |

## 废弃表直接删（8 张）

| 表名 | 实体 | 写入点 | 前端调用 | 判据 |
|---|---|---|---|---|
| `food_trace` | `com\foodtraceability\entity\FoodTrace.java` | `com\foodtraceability\service\impl\InventoryDeductionServiceImpl.java:59` | `router\index.ts:187` | 被新模型取代 / 视图 / 实体死代码 |
| `inventory_log` | `com\foodtraceability\entity\InventoryLog.java` | `com\foodtraceability\service\impl\MaterialTraceCodeServiceImpl.java:692` | `views\warehouse\StoreInventory.vue:33` | 被新模型取代 / 视图 / 实体死代码 |
| `operation_type` | `com\foodtraceability\entity\OperationType.java` | `com\foodtraceability\aspect\FinanceRecordOperationAspect.java:174` | 无 | 被新模型取代 / 视图 / 实体死代码 |
| `store_inventory_log` | `com\foodtraceability\entity\StoreInventoryLog.java` | `com\foodtraceability\service\impl\MaterialTraceCodeServiceImpl.java:692` | 无 | 被新模型取代 / 视图 / 实体死代码 |
| `sys_permissions` | `com\foodtraceability\entity\SysPermission.java` | 无 | 无 | 被新模型取代 / 视图 / 实体死代码 |
| `sys_roles` | `com\foodtraceability\entity\SysRole.java` | 无 | 无 | 被新模型取代 / 视图 / 实体死代码 |
| `sys_stores` | `com\foodtraceability\entity\SysStore.java` | 无 | 无 | 被新模型取代 / 视图 / 实体死代码 |
| `v_inventory_summary` | `com\foodtraceability\entity\InventorySummary.java` | 无 | 无 | 被新模型取代 / 视图 / 实体死代码 |


---

## 口径说明与注意事项

1. **数量差异**：lead 提到「~20 张」，本次按「实体 @TableName vs information_schema」全量扫描得到 **59 张**。
   差别来源：本清单把「实体存在但表不存在」全算了进来，包含一部分**从未建过表**的未上线功能；
   如果只要「曾经建过、后来丢了」的表，需要再和迁移脚本 `db/migration/*.sql` 的 CREATE TABLE 交叉过滤（本清单未做）。
2. **「废弃表直接删」档里有 3 张表代码仍在引用**（`food_trace`、`inventory_log`、`store_inventory_log`）：
   表不存在 → 这些写入路径**运行时必然报错或落空**，删表前必须先把引用清掉（或确认走的是新表）。
   表中「写入点」列给的就是这些引用位置，供核对。
3. 判据是**静态扫描**（文件:行），不等于运行时证据；执行前建议按分类抽样跑一次接口确认。
