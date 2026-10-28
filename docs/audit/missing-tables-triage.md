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

---

## 交叉过滤：59 张里「曾建后丢」只有 3 张，其余 56 张**从未建过**

判据：`backend/src/{main,test}/resources/db/migration/*.sql` 里的 `CREATE TABLE <表名>`。

| 性质 | 数量 | 含义 | 处置方向 |
|---|---|---|---|
| **曾建后丢**（迁移里有 CREATE TABLE，生产库没了） | **3** | `inventory_log`、`inventory_transactions`、`store_inventory_log` | **部署缺陷**：环境漏跑/回滚了迁移，需要补迁移或核对部署 |
| **从未建过**（迁移里没有 CREATE TABLE） | **56** | 其余全部 | **未上线功能**或死代码，按功能分类处置（见上表） |

且与既有文档对齐：`docs/audit/missing-table-writers.md` 的 **22 张 = 有写入代码且两边都无表**（lead 说的 ~20 张）；
本清单 59 张 = 实体 `@TableName` 全量 vs 生产库（含 22 张 + 37 张无写入的实体）。

## ⚠ 单列一档：代码在写不存在的表（真 bug，不是「废弃」）

这 3 张**不能按废弃处理**：写入路径在运行时必然失败或报错，属真 bug。

| 表名 | 是否曾建过 | 写入/读取点 | 当前后果 | 证据 |
|---|---|---|---|---|
| `inventory_log` | 曾建后丢 | `MaterialTraceCodeServiceImpl.java:692`；控制器 `InventoryLogController` / `InventoryConsumptionController` | **501**（`@UnfinishedFeature` 拦截）；写后门已关（宪法 §III.7），只剩查询 | `InventoryLogController.java:38-39`、`InventoryConsumptionController.java:36-37` |
| `store_inventory_log` | 曾建后丢 | `MaterialTraceCodeServiceImpl.java:692`；`StoreInventoryController` 曾引用 | 曾因误标注解 → **501**；现已按「本端点只查 stores」修正 | `StoreInventoryController.java:54` 注释 |
| `food_trace` | **从未建过** | `InventoryDeductionServiceImpl.java:59` | 写入落空/异常（该路径未挂 `@UnfinishedFeature`）→ 需确认是 500 还是静默 | `InventoryDeductionServiceImpl.java:59` |
| `inventory_transactions`（参考） | 曾建后丢 | 旧写路径已迁走 | 历史上「表已更名 → **500**」，现改为 location 维度委派 | `InventoryServiceImpl.java:154` 注释 |

结论：
- **曾建后丢的 3 张**属**部署缺口**（不是「没上线」），应优先核对部署是否漏跑迁移；
- **`food_trace` 从未建过但有写入** → 是**未上线功能的残留写入**，风险是 500/静默；建议 @UnfinishedFeature 或删代码引用；
- 其余 56 张「从未建过」按上表三档处置即可。

---

## 更正（2026-10-28，fund-500 复核后）：`food_trace` 不是「代码在写不存在的表」

**上一版判据有误**：我用「文件里提到实体类名 + 出现 `.update/.insert/.delete` 调用」做启发式匹配，
`InventoryDeductionServiceImpl` 里同时出现了别的 Mapper 的写入调用 → 被误判成 `food_trace` 写入点。

复核证据（命令与原文）：
```bash
# 1) 全仓搜 Mapper 变量（唯一命中就是接口声明，没有任何调用方）
grep -rn 'foodTraceMapper' backend/src                 → 1 hit: FoodTraceMapper.java:24 (interface 声明)
grep -rn 'FoodTraceMapper'  backend/src                 → 1 hit: 同上（连 @Autowired 注入都没有）
# 2) 我当时写的「写入点」原文（InventoryDeductionServiceImpl.java:55-72）
59: materialTraceCodeMapper.updateById(traceCodeEntity);   ← 写 material_trace_code，不是 food_trace
72: materialUsageRecordMapper.insert(usageRecord);         ← 写 material_usage_record
157: food_trace_code 相关写入                                ← 不是 food_trace
```
**结论**：`food_trace` **没有任何写入/调用方**（`FoodTraceMapper` 甚至未被注入），属「实体指向空表」。

| 表名 | 更正后分类 | 处置 |
|---|---|---|
| `food_trace` | **实体指向空表（登记，不修）** | **不建表**（建出来永远为空）；建议后续删实体/Mapper（死代码） |

另外 3 张仍维持「真 bug」判定，但依据已改为**实际 Mapper 调用**：
```
inventory_log          : @UnfinishedFeature 拦截 → 501（控制器注解，非 Mapper 调用）
store_inventory_log    : 同上 + StoreInventoryController:54 注释说明曾误标 501
inventory_transactions : 旧写路径已迁走；InventoryServiceImpl:154 注释记录历史 500
```
⚠ 判据教训（写进本文件防复发）：**判定「有写入」必须以「Mapper 实例调用」为准**（`xxxMapper.insert/update/delete`），
不能用「文件里出现实体类名」代替 —— 否则一个文件里混用多个 Mapper 就会把没被调用过的表也算成写入。
