# 夜间自动巡检 · 雷达 2：实体/表映射漂移审计报告

| 项 | 值 |
|----|----|
| 分支 | `ai-night-patrol-1`（只读巡检，未执行任何写操作） |
| 采样时点 | 2026-10-23 |
| 数据源 | PostgreSQL 18 `food_traceability`（psql SELECT，只读）、`backend/src/main/java` 全量扫描、`backend/src/main/resources/mapper/**/*.xml`、`db/migration` 222 个脚本 |
| 库内对象 | 316 = 312 张 BASE TABLE + 4 个 VIEW（`asset_master` / `dining_table` / `v_kitchen_order_full` / `v_users_store_compat`） |
| 实体声明 | 368 处有效 `@TableName`（371 处文本命中，其中 3 处是注释），覆盖 368 个实体类 / 362 个目标表名 |
| 关联卡 | TECH-DEBT-001（单/复数双表）、DATA-004、DATA-006 |
| 权限边界 | 未删表、未改 `@TableName`、未改任何 Java / migration；产出仅为报告 + 只读 SQL 脚本 |

---

## 0. 结论摘要

1. **A 类（`@TableName` 指向不存在的表/视图）：67 张**。其中 **40 张的 Mapper 已被 Controller 可达链路注入** —— 一旦对应接口被调用即 `relation ... does not exist` 500。另外 27 张对应实体完全孤立（Mapper 存在但未接线）。
2. **A 类根因不是笔误，是缺 schema**：67 张里 **64 张在 222 个 Flyway 脚本里从来没有 `CREATE TABLE`**，实体是先写后无表；剩下 3 张（`store_inventory_log` / `inventory_log` / `inventory_transactions`）有迁移、`flyway_schema_history.success = t`，但库里对象不存在，实际数据躺在手工建的 `*_legacy` 表里。
3. **B 类（库里有对象、全仓代码零引用）：5 张**（全是 `*_legacy` / backup），另有 7 张只在 migration 文本里出现。
4. **C 类（单复数并存）：18 对**，其中 **6 对两侧都有可用的 Mapper 且都在业务链路上被使用**，是本雷达最危险的一类 —— 典型：`food_trace_code`(14 行) vs `food_trace_codes`(0 行)、`dish_recipe`(0 行) vs `dish_recipes`(12 行)、`food_category`(0 行) vs `food_categories`(9 行)、`recharge_plan`(496 行) vs `recharge_plans`(0 行)。**代码写/读的那一侧恰好是空表**。
5. **`STATUS.md` TECH-DEBT-001 的描述有 3 处与实测不符**（见 §5），其中 `tray/trays` 双表**不存在**（库里只有 `tray`）。
6. 全库 312 张表里 **199 张为 0 行**（63.8%），是这轮 schema 漂移的土壤；`dining_tables` 0 行（DATA-004 仍未解决）。

---

## 1. 方法与判定口径

```text
① information_schema.tables  → 312 表 + 4 视图（仅 public schema，无第二 schema）
② 逐表 count(*)             → 精确行数（不依赖 reltuples，全部 312 张实算）
③ grep ^@TableName           → 368 处有效声明，按文件解析出 实体类 → 目标表名
④ mapper/**/*.xml 手写表名   → 91 个被 XML 直接引用的表（其余走 MyBatis-Plus 注解）
⑤ Mapper 接口 → BaseMapper<X> + import 解析 → 确定每个 Mapper 真正落在哪张表
⑥ Controller 可达性 BFS      → 从 268 个 @RestController/@Controller 出发，按 token 引用 +
                               interface→implements 边扩展 7 层，判定 Mapper 是否在调用链上
```

可达性 BFS 的口径说明：`visited = 2376 / 2766` 个 java 文件（起点 269 个 Controller）。它给出的是**上界**（注释里的类名也算引用边），所以「不可达」结论偏保守可信，「可达」结论只会多不会少。已用已知活链路自校准：`TrayMapper`(REACH) / `FinanceRecordMapper`(REACH) / `FoodMapper`(REACH) / `CostRecordMapper`(REACH) 均判定为可达，与 STATUS.md 记录的 POS→KDS→成本链路一致。

---

## 2. A 类：`@TableName` 指向不存在的表 / 视图（67 张）

`是` = 该表对应的 Mapper 已被 Controller 可达链路注入（调用即炸）；`否` = 实体/Mapper 孤立。

| # | 缺失表 | 实体类 | 声明位置 | 已接线 | 首个注入点 | Owner WIP |
|---|--------|--------|----------|--------|-----------|-----------|
| 1 | `budget` | `Budget` | `backend\src\main\java\com\foodtraceability\entity\Budget.java:14` | **是** | `dataservice\finance\impl\BudgetDataServiceImpl.java:28` | — |
| 2 | `category_operation_log` | `CategoryOperationLog` | `backend\src\main\java\com\foodtraceability\entity\CategoryOperationLog.java:11` | **是** | — | — |
| 3 | `contract_document` | `ContractDocument` | `backend\src\main\java\com\foodtraceability\entity\ContractDocument.java:23` | **是** | — | — |
| 4 | `contract_template` | `ContractTemplate` | `backend\src\main\java\com\foodtraceability\entity\ContractTemplate.java:14` | **是** | — | — |
| 5 | `coupon` | `Coupon` | `backend\src\main\java\com\foodtraceability\entity\Coupon.java:15` | **是** | `service\impl\CouponServiceImpl.java:26` | — |
| 6 | `dish_ingredient` | `DishIngredient` | `backend\src\main\java\com\foodtraceability\entity\DishIngredient.java:13` | **是** | `service\trace\impl\FoodTraceabilityServiceImpl.java:49` | — |
| 7 | `file_attachment` | `FileAttachment` | `backend\src\main\java\com\foodtraceability\entity\FileAttachment.java:7` | **是** | `config\FileCleanupScheduler.java:42` | — |
| 8 | `finance_approval` | `FinanceApproval` | `backend\src\main\java\com\foodtraceability\entity\finance\FinanceApproval.java:12` | **是** | `service\impl\FinanceApprovalServiceImpl.java:39` | — |
| 9 | `hardware_config` | `HardwareConfig` | `backend\src\main\java\com\foodtraceability\entity\HardwareConfig.java:9` | **是** | `service\impl\DeviceHeartbeatServiceImpl.java:43` | — |
| 10 | `hardware_config_version` | `HardwareConfigVersion` | `backend\src\main\java\com\foodtraceability\entity\HardwareConfigVersion.java:9` | **是** | `service\impl\HardwareConfigVersionServiceImpl.java:37` | — |
| 11 | `hardware_operation_log` | `HardwareOperationLog` | `backend\src\main\java\com\foodtraceability\entity\HardwareOperationLog.java:11` | **是** | `service\impl\HardwareOperationLogServiceImpl.java:25` | — |
| 12 | `inventory_category` | `InventoryCategory` | `backend\src\main\java\com\foodtraceability\entity\InventoryCategory.java:11` | **是** | — | — |
| 13 | `inventory_code` | `InventoryCode` | `backend\src\main\java\com\foodtraceability\entity\InventoryCode.java:7` | **是** | `service\impl\HardwareDeviceServiceImpl.java:41` | — |
| 14 | `inventory_log` | `InventoryLog` | `backend\src\main\java\com\foodtraceability\entity\InventoryLog.java:15` | **是** | `service\impl\InventoryLogServiceImpl.java:24` | — |
| 15 | `inventory_unit` | `InventoryUnit` | `backend\src\main\java\com\foodtraceability\entity\InventoryUnit.java:11` | **是** | — | — |
| 16 | `locker_slot` | `LockerSlot` | `backend\src\main\java\com\foodtraceability\entity\LockerSlot.java:11` | **是** | `service\impl\TakeoutLockerServiceImpl.java:40` | — |
| 17 | `material_usage_record` | `MaterialUsageRecord` | `backend\src\main\java\com\foodtraceability\entity\MaterialUsageRecord.java:9` | **是** | `controller\kitchen\KitchenScanController.java:42` | — |
| 18 | `over_age_worker` | `OverAgeWorker` | `backend\src\main\java\com\foodtraceability\entity\OverAgeWorker.java:17` | **是** | `service\impl\OverAgeWorkerServiceImpl.java:23` | — |
| 19 | `plan_item` | `PlanItem` | `backend\src\main\java\com\foodtraceability\entity\PlanItem.java:17` | **是** | — | — |
| 20 | `print_format_template` | `PrintFormatTemplate` | `backend\src\main\java\com\foodtraceability\entity\PrintFormatTemplate.java:13` | **是** | `service\impl\PrintFormatTemplateServiceImpl.java:28` | — |
| 21 | `receivable` | `Receivable` | `backend\src\main\java\com\foodtraceability\entity\Receivable.java:13` | **是** | `dataservice\finance\impl\ReceivableDataServiceImpl.java:28` | — |
| 22 | `requisition_requests` | `RequisitionRequestEntity` | `backend\src\main\java\com\foodtraceability\entity\approval\RequisitionRequestEntity.java:16` | **是** | `service\approval\impl\EmployeeApprovalServiceImpl.java:79` | — |
| 23 | `role_departments` | `RoleDepartment` | `backend\src\main\java\com\foodtraceability\entity\RoleDepartment.java:16` | **是** | `service\impl\DataPermissionServiceImpl.java:62` | — |
| 24 | `role_stores` | `RoleStore` | `backend\src\main\java\com\foodtraceability\entity\RoleStore.java:16` | **是** | `service\impl\DataPermissionServiceImpl.java:60` | — |
| 25 | `salary_adjustment` | `SalaryAdjustment` | `backend\src\main\java\com\foodtraceability\entity\SalaryAdjustment.java:16` | **是** | — | — |
| 26 | `salary_record` | `SalaryRecord` | `backend\src\main\java\com\foodtraceability\entity\SalaryRecord.java:16` | **是** | — | — |
| 27 | `salary_rule` | `SalaryRule` | `backend\src\main\java\com\foodtraceability\entity\SalaryRule.java:16` | **是** | — | — |
| 28 | `scan_device` | `ScanDevice` | `backend\src\main\java\com\foodtraceability\entity\ScanDevice.java:11` | **是** | `service\impl\ScanDeviceServiceImpl.java:47` | — |
| 29 | `scan_record` | `ScanRecord` | `backend\src\main\java\com\foodtraceability\entity\ScanRecord.java:11` | **是** | `service\impl\ScanDeviceServiceImpl.java:49` | — |
| 30 | `self_purchase` | `SelfPurchase` | `backend\src\main\java\com\foodtraceability\entity\SelfPurchase.java:14` | **是** | `service\impl\SelfPurchaseServiceImpl.java:36` | — |
| 31 | `self_purchase_item` | `SelfPurchaseItem` | `backend\src\main\java\com\foodtraceability\entity\SelfPurchaseItem.java:13` | **是** | `service\impl\SelfPurchaseServiceImpl.java:38` | — |
| 32 | `store_inventory_log` | `StoreInventoryLog` | `backend\src\main\java\com\foodtraceability\entity\StoreInventoryLog.java:12` | **是** | `service\impl\StoreInventoryLogServiceImpl.java:45` | — |
| 33 | `supplier_evaluations` | `SupplierEvaluation` | `backend\src\main\java\com\foodtraceability\entity\SupplierEvaluation.java:14` | **是** | `service\impl\SupplierEvaluationServiceImpl.java:33` | — |
| 34 | `swap_request_details` | `SwapRequestDetailEntity` | `backend\src\main\java\com\foodtraceability\entity\approval\SwapRequestDetailEntity.java:16` | **是** | `service\approval\impl\EmployeeApprovalServiceImpl.java:76` | — |
| 35 | `takeout_locker` | `TakeoutLocker` | `backend\src\main\java\com\foodtraceability\entity\TakeoutLocker.java:11` | **是** | `service\impl\TakeoutLockerServiceImpl.java:38` | — |
| 36 | `trace_code_log` | `TraceCodeLog` | `backend\src\main\java\com\foodtraceability\entity\TraceCodeLog.java:12` | **是** | `service\impl\TraceCodeServiceImpl.java:38` | — |
| 37 | `travel_requests` | `TravelRequestEntity` | `backend\src\main\java\com\foodtraceability\entity\approval\TravelRequestEntity.java:16` | **是** | `service\approval\impl\EmployeeApprovalServiceImpl.java:77` | — |
| 38 | `v_inventory_summary` | `InventorySummary` | `backend\src\main\java\com\foodtraceability\entity\InventorySummary.java:11` | **是** | `service\impl\InventorySummaryServiceImpl.java:19` | — |
| 39 | `weighing_device` | `WeighingDevice` | `backend\src\main\java\com\foodtraceability\entity\WeighingDevice.java:11` | **是** | `service\impl\WeighingDeviceServiceImpl.java:35` | — |
| 40 | `weighing_record` | `WeighingRecord` | `backend\src\main\java\com\foodtraceability\entity\WeighingRecord.java:11` | **是** | `service\impl\WeighingDeviceServiceImpl.java:37` | — |
| 41 | `cost_allocation` | `CostAllocation` | `backend\src\main\java\com\foodtraceability\entity\CostAllocation.java:13` | 否 | — | — |
| 42 | `cost_allocation_detail` | `CostAllocationDetail` | `backend\src\main\java\com\foodtraceability\entity\CostAllocationDetail.java:13` | 否 | — | — |
| 43 | `cost_allocation_rule` | `CostAllocationRule` | `backend\src\main\java\com\foodtraceability\entity\CostAllocationRule.java:12` | 否 | — | — |
| 44 | `data_change_history` | `DataChangeHistory` | `backend\src\main\java\com\foodtraceability\entity\DataChangeHistory.java:14` | 否 | — | — |
| 45 | `data_link_event` | `DataLinkEvent` | `backend\src\main\java\com\foodtraceability\entity\DataLinkEvent.java:14` | 否 | — | — |
| 46 | `department_permissions` | `DepartmentPermission` | `backend\src\main\java\com\foodtraceability\entity\DepartmentPermission.java:14` | 否 | — | — |
| 47 | `finance_account` | `FinanceAccount` | `backend\src\main\java\com\foodtraceability\entity\finance\FinanceAccount.java:8` | 否 | — | — |
| 48 | `finance_record_audit_log` | `FinanceRecordAuditLog` | `backend\src\main\java\com\foodtraceability\entity\finance\FinanceRecordAuditLog.java:15` | 否 | — | — |
| 49 | `finance_report` | `FinanceReport` | `backend\src\main\java\com\foodtraceability\entity\finance\FinanceReport.java:12` | 否 | — | — |
| 50 | `finance_report_detail` | `FinanceReportDetail` | `backend\src\main\java\com\foodtraceability\entity\finance\FinanceReportDetail.java:9` | 否 | — | — |
| 51 | `food_trace` | `FoodTrace` | `backend\src\main\java\com\foodtraceability\entity\FoodTrace.java:13` | 否 | — | — |
| 52 | `inventory_transactions` | `InventoryTransaction` | `backend\src\main\java\com\foodtraceability\entity\InventoryTransaction.java:15` | 否 | — | — |
| 53 | `inventory_transfer_detail` | `InventoryTransferDetail` | `backend\src\main\java\com\foodtraceability\entity\InventoryTransferDetail.java:12` | 否 | — | — |
| 54 | `invitation_expense_record` | `InvitationExpenseRecord` | `backend\src\main\java\com\foodtraceability\entity\InvitationExpenseRecord.java:16` | 否 | — | — |
| 55 | `invitation_reminder_log` | `InvitationReminderLog` | `backend\src\main\java\com\foodtraceability\entity\InvitationReminderLog.java:15` | 否 | — | — |
| 56 | `ocr_record` | `OcrRecord` | `backend\src\main\java\com\foodtraceability\entity\OcrRecord.java:15` | 否 | — | — |
| 57 | `operation_logs` | `OperationLog` | `backend\src\main\java\com\foodtraceability\entity\OperationLog.java:12` | 否 | — | — |
| 58 | `operation_type` | `OperationType` | `backend\src\main\java\com\foodtraceability\entity\OperationType.java:12` | 否 | — | — |
| 59 | `registration_code_log` | `RegistrationCodeLog` | `backend\src\main\java\com\foodtraceability\entity\RegistrationCodeLog.java:11` | 否 | — | — |
| 60 | `sys_devices` | `SysDevice` | `backend\src\main\java\com\foodtraceability\entity\SysDevice.java:10` | 否 | — | — |
| 61 | `sys_permissions` | `SysPermission` | `backend\src\main\java\com\foodtraceability\entity\SysPermission.java:10` | 否 | — | — |
| 62 | `sys_print_templates` | `SysPrintTemplate` | `backend\src\main\java\com\foodtraceability\entity\SysPrintTemplate.java:10` | 否 | — | — |
| 63 | `sys_roles` | `SysRole` | `backend\src\main\java\com\foodtraceability\entity\SysRole.java:10` | 否 | — | — |
| 64 | `sys_stores` | `SysStore` | `backend\src\main\java\com\foodtraceability\entity\SysStore.java:10` | 否 | — | — |
| 65 | `trace_stage` | `TraceStage` | `backend\src\main\java\com\foodtraceability\entity\TraceStage.java:12` | 否 | — | — |
| 66 | `traceability_code` | `TraceabilityCode` | `backend\src\main\java\com\foodtraceability\entity\TraceabilityCode.java:7` | 否 | — | — |
| 67 | `user_sessions` | `UserSession` | `backend\src\main\java\com\foodtraceability\entity\UserSession.java:9` | 否 | — | — |

### 2.1 真雷 Top 6（已接线 + 在核心链路）

| 缺失表 | 证据 file:line | 为什么会炸 |
|--------|----------------|-----------|
| `material_usage_record` | `backend\src\main\java\com\foodtraceability\controller\kitchen\KitchenScanController.java:42` 直接注入 `MaterialUsageRecordMapper` | **Controller 直接持有 Mapper**，后厨扫码类接口一进方法就查表 → 500 |
| `dish_ingredient` | `backend\src\main\java\com\foodtraceability\service\trace\impl\FoodTraceabilityServiceImpl.java:49` 注入 `DishIngredientMapper` | 追溯主服务依赖它；同文件 `:29` 又继承 `ServiceImpl<TraceCodeMapper, TraceCode>`（见 C 类雷 1） |
| `file_attachment` | `backend\src\main\java\com\foodtraceability\config\FileCleanupScheduler.java:42` 注入 `FileAttachmentMapper` | **定时任务**，启动即跑 → 每次清理周期必然异常刷日志 |
| `hardware_config` | `backend\src\main\java\com\foodtraceability\service\impl\DeviceHeartbeatServiceImpl.java:43` 注入 `HardwareConfigMapper` | 设备心跳链路，设备一上线即触发 |
| `budget` / `receivable` | `dataservice\finance\impl\BudgetDataServiceImpl.java:28`、`dataservice\finance\impl\ReceivableDataServiceImpl.java:28` | 财务预算/应收看板，`budget` 已有 `budgets` 真表（见 C 类） |
| `store_inventory_log` | `backend\src\main\java\com\foodtraceability\service\impl\StoreInventoryLogServiceImpl.java:45` | 有 Flyway 迁移、有 `store_inventory_log_legacy`(49 行) 真数据，但实体指向的表名不存在 |

### 2.2 根因：64/67 从来没有建表迁移

- 在 222 个 `db/migration/*.sql` 中检索 `CREATE TABLE <name>`：**64 张缺失表零命中**。这批实体是「先写实体、后补 schema」流程的残渣（`entity/` 下 19 个文件当前还带 Owner WIP，见 §6）。
- 只有 3 张有建表迁移却依旧不存在：

| 表 | 建表迁移 | 迁移在 history 里 | 库内现状 |
|----|----------|-------------------|----------|
| `inventory_log` | `V20260629_020__create_inventory_logs_table.sql` | 20260629.020 `create inventory logs table` → `success = t` | 表不存在；存在 `inventory_log_legacy`(0 行) |
| `store_inventory_log` | `V20260704_001__create_missing_entity_tables.sql` | 20260704.001 `create missing entity tables` → `success = t` | 表不存在；存在 `store_inventory_log_legacy`(**49 行**) |
| `inventory_transactions` | `V1.0.0.100__init_postgresql.sql` + `V20260723_005__rebuild_inventory_transactions_table.sql` | 20260723.005 → `success = t` | 表不存在；存在 `inventory_transactions_legacy`(**31 行**) |

- **`*_legacy` 这 4 张表在 222 个迁移里全都找不到建表或改名语句**（唯一 `_legacy` 相关迁移是 `V20260707__create_legacy_order_tables.sql`，只建 `orders_legacy` / `order_items_legacy`）。结论：`inventory_legacy`(15) / `inventory_log_legacy`(0) / `inventory_transactions_legacy`(31) / `store_inventory_legacy`(18) / `store_inventory_log_legacy`(49) 是**手工 DDL 的产物**，属 PG-003「禁止手写 schema」的历史欠账；Flyway 记录的 `success=t` 与库内真实对象不一致，下次 `flyway migrate` 不会自愈。
- `flyway_schema_history` 223 行 **`success = false` 为 0 行**，max(version)=`999`；说明这不是「迁移失败」，而是「迁移记录与物理对象脱节」。

---

## 3. B 类：库里有对象但零代码引用

判定：在 `backend/src/main/java`（2766 个 java）与 `resources/mapper`（191 个 XML）中搜索标识符，出现次数 = 0，且无 `@TableName` 指向。

### 3.1 完全零引用（连 migration 都没提过）—— 5 张

| 表 | 行数 | 备注 |
|----|------|------|
| `flyway_schema_history_backup` | 205 | Flyway 卫生异常遗留备份（ENV-14 相邻），**不碰** |
| `inventory_legacy` | 15 | 无迁移来源，手工表 |
| `inventory_log_legacy` | 0 | 无迁移来源，手工表 |
| `inventory_transactions_legacy` | 31 | 无迁移来源，手工表（**有数据**） |
| `store_inventory_legacy` | 18 | 无迁移来源，手工表（**有数据**） |

### 3.2 只在 migration 文本里出现、无 java/XML 引用 —— 7 张

`device_status_log`(0 行) / `flyway_schema_history`(223，框架表) / `inventory_warning_record`(0) / `invitation_codes`(0) / `marketing_message_template`(6) / `salary_records`(0) / `v_users_store_compat`(视图)

> 注意：`device_status_log`、`inventory_warning_record` 属 STATUS.md §4 明确标注「单数 legacy 表不碰」的范围，这里只登记不动。

### 3.3 有 java 字面量引用但无实体、无 XML（半死表）—— 6 张

`asset_master`(视图) / `cost_record`(0) / `order_sequence`(10) / `rfm_calculation_log`(0) / `store_inventory_log_legacy`(**49 行，被 java 裸 SQL 引用**) / `v_kitchen_order_full`(视图)

其中 `store_inventory_log_legacy` 是**唯一一张「有实体表名不存在、数据落在 legacy 表、代码用裸 SQL 读 legacy」的表**，是 A 类 3 号根因的活证据。

---


> **口径说明（与脚本 07 对齐）**：`scripts/night-patrol/07-schema-drift-singular-plural_verify.sql` 的 `B-UNCLAIMED` 用的是「无 `@TableName` 认领」这一**宽口径**，实测 = 21 个对象；本 §3 的 B 类是更严的「连裸 SQL / XML 都没引用」= 5 张。两套数字不矛盾，只是阈值不同，请勿混用。
## 4. C 类：单/复数并存（18 对）

`rows` 为 `count(*)` 实测；`Mapper` 后的 `usedIn` 是引用该 Mapper 的 java 文件数（不含 Mapper 自身）。

| 单数表 | rows | Mapper | 复数表 | rows | Mapper |
|--------|------|--------|--------|------|--------|
| `accounting_subject` | 102 | **无 Mapper** | `accounting_subjects` | 2 | `AccountingSubjectMapper`(usedIn=6) |
| `combo_ingredient` | 1 | `ComboIngredientMapper`(usedIn=0) | `combo_ingredients` | 2 | `ComboIngredientNewMapper`(usedIn=7) |
| `cost_record` | 0 | **无 Mapper** | `cost_records` | 33 | `CostRecordMapper`(usedIn=3) |
| `device_status_log` | 0 | **无 Mapper** | `device_status_logs` | 4 | `DeviceStatusLogMapper`(usedIn=2) |
| `dining_tables` | 0 | `DiningTableNewMapper`(usedIn=4) | `dining_table` | 视图 | `DiningTableMapper`(usedIn=1) |
| `dish_combo` | 1 | `DishComboMapper`(usedIn=1) | `dish_combos` | 1 | `DishComboNewMapper`(usedIn=6) |
| `dish_recipe` | 0 | `DishRecipeMapper`(usedIn=4) | `dish_recipes` | 12 | `DishRecipeNewMapper`(usedIn=4) |
| `finance_invoice` | 0 | **无 Mapper** | `finance_invoices` | 8 | `FinanceInvoiceMapper`(usedIn=1) |
| `finance_voucher` | 0 | **无 Mapper** | `finance_vouchers` | 42 | `FinanceVoucherMapper`(usedIn=8) |
| `finance_voucher_detail` | 0 | **无 Mapper** | `finance_voucher_details` | 84 | `FinanceVoucherDetailMapper`(usedIn=4) |
| `food` | 8 | `FoodMapper`(usedIn=11) `ProductMapper`(usedIn=6) | `foods` | 8 | `FoodNewMapper`(usedIn=16) |
| `food_category` | 0 | `FoodCategoryMapper`(usedIn=1) | `food_categories` | 9 | `FoodCategoryNewMapper`(usedIn=4) |
| `food_trace_code` | 14 | `FoodTraceCodeMapper`(usedIn=4) | `food_trace_codes` | 0 | `TraceCodeMapper`(usedIn=3) |
| `inventory_warning_record` | 0 | **无 Mapper** | `inventory_warning_records` | 0 | `InventoryWarningRecordMapper`(usedIn=1) |
| `inventory_warning_rule` | 0 | **无 Mapper** | `inventory_warning_rules` | 0 | `InventoryWarningRuleMapper`(usedIn=2) |
| `member_level` | 4 | `MemberLevelBaseMapper`(usedIn=1) | `member_levels` | 4 | `MemberLevelMapper`(usedIn=1) |
| `recharge_plan` | 496 | `RechargePlanBaseMapper`(usedIn=1) | `recharge_plans` | 0 | `RechargePlanMapper`(usedIn=2) |
| `recharge_record` | 0 | `RechargeRecordBaseMapper`(usedIn=2) | `recharge_records` | 0 | `RechargeRecordMapper`(usedIn=4) |

### 4.1 高危 C 类 6 项（代码读的那一侧是空表 / 双写）

**雷 1｜追溯码：`food_trace_code`(14 行) ⟷ `food_trace_codes`(0 行)**
- `TraceCodeMapper` → `food_trace_codes`（空），被 3 处使用：
  - `backend\src\main\java\com\foodtraceability\service\trace\impl\FoodTraceabilityServiceImpl.java:29`：`extends ServiceImpl<TraceCodeMapper, TraceCode>` —— **追溯主服务的 save/update 写进空表**
  - `backend\src\main\java\com\foodtraceability\service\impl\TraceCodeServiceImpl.java:26`、`:36`
  - `backend\src\main\java\com\foodtraceability\service\trace\impl\SupplierTraceServiceImpl.java:12`
- 真正的 14 条追溯码在 `food_trace_code`，由 `FoodTraceCodeMapper`（`backend\src\main\java\com\foodtraceability\mapper\FoodTraceCodeMapper.java`）读。
- 风险：同一次追溯写成两条数据路径，读路径写路径不一致时表现为「刚生成的追溯码查不到」。

**雷 2｜BOM：`dish_recipe`(0 行) ⟷ `dish_recipes`(12 行)**
- `DishRecipeMapper` → `dish_recipe`（空），用在**出餐扣料链路**：
  - `backend\src\main\java\com\foodtraceability\service\impl\InventoryDeductionServiceImpl.java:23` `private final DishRecipeMapper dishRecipeMapper;`
  - `backend\src\main\java\com\foodtraceability\service\impl\KitchenScanServiceImpl.java:34` 同上
  - `:212` / `:378` 构造器注入
- `DishRecipeNewMapper` → `dish_recipes`（12 行），用在 `FoodServiceImpl.java:62` / `RecipeServiceImpl.java:33` / `OrderNewServiceImpl.java:60`。
- 风险：**OPS-BOM-003「无 BOM 硬拒出餐」可能对有效菜品误判**（校验方读空表）。这也解释了为什么 BOM 数据要重复维护。

**雷 3｜菜品分类：`food_category`(0 行) ⟷ `food_categories`(9 行)**
- `FoodCategoryMapper` → `food_category`（空），使用点 `backend\src\main\java\com\foodtraceability\service\impl\PosApiServiceImpl.java:42`（**:16 / :42 / :48** 三处）—— **POS 端分类查询走空表**。
- `FoodCategoryNewMapper` → `food_categories`（9 行），4 处使用（`CategoryServiceImpl.java:33` / `CostAnalysisServiceImpl.java:37` / `DishCostServiceImpl.java:55`）。

**雷 4｜充值套餐：`recharge_plan`(496 行) ⟷ `recharge_plans`(0 行)**
- 真数据 496 行在单数表 `recharge_plan`，由 `RechargePlanBaseMapper` 读（`service\impl\RechargeServiceImpl.java:35`）。
- `mapper\marketing\RechargePlanMapper` → `recharge_plans`（空），被 `service\impl\marketing\RechargeRecordServiceImpl.java:47` 使用 —— **充值记录校验套餐会读空表**（「套餐不存在」类误报）。

**雷 5｜菜品主数据双写：`food`(8 行) ⟷ `foods`(8 行)**
- `FoodMapper`(usedIn=11) 与 `FoodNewMapper`(usedIn=16) **都在用，两侧都有数据且行数相同**。
- `backend\src\main\java\com\foodtraceability\service\impl\FoodServiceImpl.java:130` 注释已自证：「POS 下单扣减走 legacy food（FoodMapper.deductStock），行缺失时报误导性『库存不足』」；同文件 `:62-:71` 又同时注入 `FoodNewMapper` 与 `FoodMapper`。
- 风险：库存扣减/回滚分表进行，`foods` 与 `food` 的库存量必然漂移，这是「库存不足」误报的根因面。

**雷 6｜总账科目：`accounting_subject`(102 行) 无 Mapper**
- 102 行真实科目数据（全库第 9 大表）在 `accounting_subject`，但**没有任何 Mapper 指向它**（实体 `entity\AccountingSubject.java:16` 存在、无对应 Mapper 接口）。
- 代码只认 `accounting_subjects`（2 行，`mapper\finance\AccountingSubjectMapper.java`）。
- 风险：科目表 102 行数据不可达，凭证科目的下拉/校验只能看到 2 条。

### 4.2 安全死表（0 行 + 无 Mapper，可判死刑但本轮不动）

`cost_record` / `finance_invoice` / `finance_voucher` / `finance_voucher_detail` / `device_status_log` / `inventory_warning_record` / `inventory_warning_rule`

> `cost_record` 对应 STATUS.md 里「库内并存 cost_record(空)/cost_records(实体实际用)」—— 与实测一致，且**无任何 Mapper 指向 cost_record**，是最干净的一张可下线表。

---

## 5. `STATUS.md` TECH-DEBT-001 描述与实测的 3 处偏差（建议更正）

| STATUS.md 原描述 | 实测 | 结论 |
|------------------|------|------|
| 「库内并存 `tray` / `trays`」 | 库里**只有 `tray`（13 行）**，`trays` 不存在 | 双表不存在，删掉这条 |
| 「`dish_recipe` / `dish_recipes`」 | 成立，且**两侧都有 Mapper 在跑**（见雷 2） | 严重度应从「清理遗留空表」升级为「出餐扣料读空表」 |
| 「`order_material_requirement`」列为双表之一 | 只有单数一张（0 行），**不是双表**；真正的问题是它 21 单 `material_consumed=1` 却 **0 行明细** | 属性应改为「数据未落库」而非「表名漂移」 |

补充：`dining_table` 确为**视图**（`dining_tables` 为 0 行表），`DiningTable.java:8` → `dining_table`(视图)、`DiningTableNew.java:11` → `dining_tables`(表)，两者都可达 —— 这是「表/视图同名不同物」的第 19 类漂移，**视图上做 INSERT/UPDATE 会直接报错**，需在桌台卡（DATA-004）里一并处理。

---

## 6. 建议（按性价比排序，均未执行）

| 优先级 | 动作 | 涉及文件（Owner WIP 状态需先核对） |
|--------|------|-----------------------------------|
| P0 | 把 §4.1 雷 1-5 的 Mapper 目标表改到有数据的一侧（改 `@TableName` 或统一 Mapper），并补一条 Flyway 把空表数据 `INSERT ... SELECT` 回填 | 实体/Mapper 均在 `entity/`、`mapper/` 下；**`git status` 显示 `entity/` 19 个、`mapper/` 9 个文件带 Owner WIP，动前逐个确认** |
| P0 | A 类 40 张已接线缺失表：要么补 `CREATE TABLE` 迁移，要么把 Mapper 从链路摘掉 | 迁移只增不改；`db/migration` 本轮零改动 |
| P1 | `file_attachment` / `hardware_config` 两张**定时任务/心跳**链路上的缺失表优先处理（异常是持续刷的，不是偶发） | `config\FileCleanupScheduler.java:42`、`service\impl\DeviceHeartbeatServiceImpl.java:43` |
| P1 | 更正 `docs/STATUS.md:39` TECH-DEBT-001 的 3 处描述（本轮未改，属治理文档历史面，需 Owner 裁决） | — |
| P2 | 手工 `*_legacy` 5 张表纳入 PG-003 欠账清单，禁止再手工改名 | — |
| P2 | 199 张空表做一次「是否保留」裁决，一次性收敛 schema | — |


### 6.1 建议改动文件的 Owner WIP 实测（`git status --porcelain`，只读）

Lead 红线：工作区有 Owner 未提交 WIP（`backend/src/main/java` 下 226 个路径变更）。**下列建议触及的文件实测全部干净（输出为空）**，可以安全改动；`entity/` 目录整体有 19 个 WIP 文件、`mapper/` 有 9 个，动目录内其它文件前仍需逐个确认。

| 文件 | git status |
|------|------------|
| `backend/src/main/java/com/foodtraceability/entity/DishRecipe.java` | 干净 |
| `backend/src/main/java/com/foodtraceability/entity/FoodCategory.java` | 干净 |
| `backend/src/main/java/com/foodtraceability/entity/TraceCode.java` | 干净 |
| `backend/src/main/java/com/foodtraceability/entity/marketing/RechargePlan.java` | 干净 |
| `backend/src/main/java/com/foodtraceability/entity/Food.java` | 干净 |
| `backend/src/main/java/com/foodtraceability/service/impl/InventoryDeductionServiceImpl.java` | 干净 |
| `backend/src/main/java/com/foodtraceability/service/impl/KitchenScanServiceImpl.java` | 干净 |
| `backend/src/main/java/com/foodtraceability/service/impl/PosApiServiceImpl.java` | 干净 |
| `backend/src/main/java/com/foodtraceability/service/trace/impl/FoodTraceabilityServiceImpl.java` | 干净 |
| `backend/src/main/java/com/foodtraceability/controller/kitchen/KitchenScanController.java` | 干净 |
| `backend/src/main/java/com/foodtraceability/config/FileCleanupScheduler.java` | 干净 |

> 注意：`service/trace/` 与 `controller/kitchen/` 虽然文件本身干净，但同目录有其它 WIP 文件；提交时请按 PG-001 精确 `git add <文件>`，不要带目录。
> 本轮**不做**：不删表、不建表、不改 `@TableName`、不改任何 Java、不新增 migration。

---

## 7. 局限与未覆盖

1. 可达性 BFS 是**静态近似**：不解析 Spring 条件装配（`@ConditionalOnProperty`）、不解析 `@Profile`，也不覆盖 `ApplicationContext.getBean()` 反射取 Bean 的路径 → 结论「已接线」偏多、「未接线」偏少。
2. Mapper XML 手写表名只统计了 `FROM/JOIN/UPDATE/INSERT INTO` 四种前缀；子查询别名、`${}` 动态表名未统计（`usedIn` 同理）。
3. 未验证运行时实际异常：`logs/` 下最新日志时间为 2026-09-08，早于 `V20261015_*` 迁移，无法作为当前构建的证据；本报告所有结论均来自**库现状 + 代码静态链路**。
4. `dish_recipe`/`food_category` 等空表若在运行时被 `selectList` 调用不会报错（返回空列表），所以「读空表」是**静默错**，不会出现在错误日志里 —— 这也是它一直没被发现的原因。
---

## 8. 配套脚本与实测复核（已跑通）

| 脚本 | 用途 | 实测结果 |
|------|------|----------|
| `scripts/night-patrol/07-schema-drift-singular-plural_verify.sql` | A/B/C 三类漂移一键复核（纯只读） | `psql -f` 退出码 0；输出 `a_missing = 67`、`a_view_target = 1`（`dining_table`）、`b_unclaimed = 21`；C 类 18 对两侧精确行数与本报告 §4 完全一致 |

脚本 07 的 C 类实测输出（可复跑对比）：

```text
accounting_subject|102      accounting_subjects|2
cost_record|0              cost_records|33
dish_recipe|0              dish_recipes|12
food|8                     foods|8
food_category|0            food_categories|9
food_trace_code|14         food_trace_codes|0
member_level|4             member_levels|4
recharge_plan|496          recharge_plans|0
finance_voucher|0          finance_vouchers|42
finance_voucher_detail|0   finance_voucher_details|84
```

回归用法：每晚或每次改实体后跑一次 07，`a_missing` 只应下降不应上升。
