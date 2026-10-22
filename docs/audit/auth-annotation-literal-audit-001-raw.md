# 全仓注解权限字面量核对报告（N32 扫描器产出）

> 扫描面：`backend/src/main/java`（全部 `.java`）
> 比对基准：活库 `roles.role_code` ∪ `roles.role_id_str` → 19 个归一化角色码；`permissions.permission_code` → 132 个权限码
> 分类：A=角色幻码（可近邻纠正）/ B=权限码幻码 / C=完全未定义 / VALID=库中存在

## 1. 总览

| 分类 | 出现次数 | 说明 |
|------|---------|------|
| **VALID** | 1461 | 有效（库中存在） |
| **A** | 12 | A 类：角色幻码（可用近邻匹配纠正标识） |
| **B** | 1031 | B 类：权限码幻码（permissions 表无此码） |
| **C** | 22 | C 类：完全未定义（角色与权限均无，需设计决策） |
| **合计** | 2526 | |

## 2. 位置错配（A 卡归一化也覆盖不到的那一类）

| 标记 | 次数 | 含义 |
|------|------|------|
| `ROLE_IN_AUTHORITY_POSITION` | 46 | ROLE_ 前缀角色码写在 hasAuthority/hasAnyAuthority；前缀对，但角色必须真实存在 |

## 3. A 类：角色幻码（可用近邻匹配纠正标识）

| 字面量 | 次数 | 近邻建议 | 涉及文件 |
|--------|------|----------|----------|
| `ROLE_MANAGER` | 12 | store_manager, region_manager, finance_manager | KitchenOrderController.java, PosApiController.java, PosCouponController.java, PosOrderController.java, PosOrderPaymentServiceImpl.java, YoloProductRecognitionController.java |

<details><summary>逐条位置（12 条）</summary>

```
com/foodtraceability/controller/kitchen/KitchenOrderController.java:436  PreAuthorize  hasAnyAuthority('ROLE_MANAGER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/kitchen/KitchenOrderController.java:453  PreAuthorize  hasAnyAuthority('ROLE_MANAGER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/kitchen/KitchenOrderController.java:469  PreAuthorize  hasAnyAuthority('ROLE_MANAGER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/kitchen/KitchenOrderController.java:482  PreAuthorize  hasAnyAuthority('ROLE_MANAGER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/kitchen/KitchenOrderController.java:495  PreAuthorize  hasAnyAuthority('ROLE_MANAGER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/pos/PosApiController.java:26  PreAuthorize  hasAnyAuthority('ROLE_MANAGER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/pos/PosCouponController.java:29  PreAuthorize  hasAnyAuthority('ROLE_MANAGER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/pos/PosOrderController.java:29  PreAuthorize  hasAnyAuthority('ROLE_MANAGER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/pos/YoloProductRecognitionController.java:24  PreAuthorize  hasAnyAuthority('ROLE_MANAGER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:104  PreAuthorize  hasAnyAuthority('ROLE_MANAGER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:280  PreAuthorize  hasAnyAuthority('ROLE_MANAGER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:699  PreAuthorize  hasAnyAuthority('ROLE_MANAGER')  [ROLE_IN_AUTHORITY_POSITION]
```
</details>

## 4. B 类：权限码幻码（permissions 表无此码）

| 字面量 | 次数 | 近邻建议 | 涉及文件 |
|--------|------|----------|----------|
| `trace:query` | 40 | finance:cost:query, warehouse:query | FoodTraceCodeController.java, InspectionController.java, MaterialTemplateController.java, MaterialTraceCodeController.java, PurchaseLedgerController.java, RecallController.java… |
| `schedule:view:self` | 25 | — | ScheduleAttendanceSyncController.java, ScheduleCalendarController.java, ScheduleConflictController.java, ScheduleEngineController.java, ScheduleExportController.java, ScheduleNotificationController.java… |
| `system:role:read` | 22 | — | RoleController.java |
| `system:user:read` | 22 | system:user:assign-store, system:user:unassign-store | UserController.java |
| `system:permission:read` | 21 | system:user:assign-store, system:user:unassign-store | PermissionController.java, UserPermissionOverrideController.java |
| `trace:update` | 21 | finance:cost:update | FoodTraceCodeController.java, InspectionController.java, MaterialTemplateController.java, MaterialTraceCodeController.java, PurchaseLedgerController.java, RecallController.java… |
| `member:view` | 17 | user:view, order:view, employee:view | MarketingMemberController.java, MemberController.java, MemberLevelController.java, PointsController.java |
| `inventory:update` | 16 | inventory:query, inventory:approve, finance:cost:update | InventoryAdjustController.java, InventoryCategoryController.java, InventoryCheckController.java, InventoryLocationController.java, InventoryLossController.java, InventoryOutboundController.java… |
| `system:permission:manage` | 16 | hr:position:manage | PermissionTemplateController.java, UserPermissionOverrideController.java |
| `order:queue` | 15 | order:view, order:manage, order:add | CallNumberController.java, PosController.java |
| `member:manage` | 14 | hr:manage, order:manage, hr:employee:manage | MarketingMemberController.java, MemberController.java, MemberLevelController.java |
| `inventory:create` | 13 | inventory:approve, inventory:query, finance:payment:create | InventoryAdjustController.java, InventoryCategoryController.java, InventoryCheckController.java, InventoryController.java, InventoryLocationController.java, InventoryLossController.java… |
| `member:analysis:view` | 13 | hr:analytics:view | CustomerAnalysisController.java, MarketingAnalysisController.java |
| `device:communication:manage` | 10 | hr:organization:manage, hr:position:manage | DeviceCommunicationTestController.java |
| `system:aimodel:manage` | 10 | order:manage | AIModelConfigController.java |
| `trace:recall` | 10 | — | RecallController.java, SupplierTraceController.java, TraceCodeController.java |
| `hardware:config:manage` | 9 | hr:contract:manage, hr:onboarding:manage, hr:archive:manage | HardwareConfigController.java |
| `hr:knowledge-base:manage` | 9 | hr:knowledge:manage, hr:knowledge:view, hr:knowledge:delete | KnowledgeBaseController.java |
| `inventory:delete` | 9 | inventory:query, inventory:approve, asset:delete | InventoryAdjustController.java, InventoryCategoryController.java, InventoryCheckController.java, InventoryLossController.java, InventoryOutboundController.java, InventoryTransferController.java… |
| `schedule:plan:edit` | 9 | kitchen:edit | ScheduleConflictController.java, ScheduleNotificationController.java, SchedulePlanController.java, ScheduleSwapController.java |
| `scheduler:task:query` | 9 | — | ScheduledTaskController.java |
| `sys:config:edit` | 9 | — | SysConfigController.java, SysSettingController.java |
| `coupon:manage` | 8 | hr:position:manage, hr:contract:manage, order:manage | CouponController.java |
| `device:alert:manage` | 8 | finance:asset:manage, order:manage, asset:manage | DeviceAlertController.java |
| `device:locker:manage` | 8 | order:manage, finance:asset:manage | TakeoutLockerController.java |
| `device:print-task:manage` | 8 | — | PrintTaskController.java |
| `hardware:label-printer:manage` | 8 | hr:recruitment:manage | LabelPrinterController.java |
| `schedule:template:manage` | 8 | hr:employee:manage, hr:contract-template:manage, hr:department:manage | ScheduleTemplateController.java |
| `sys:config:query` | 8 | — | SysConfigController.java, SysSettingController.java |
| `device:alert:view` | 7 | finance:asset:view | DeviceAlertController.java |
| `device:manage:manage` | 7 | — | DeviceController.java |
| `finance:report:view` | 7 | finance:asset:view, finance:view, finance:invoice:view | ReportController.java |
| `hr:knowledge-base:view` | 7 | hr:knowledge:view, hr:knowledge:manage, hr:knowledge:delete | KnowledgeBaseController.java |
| `member:points:manage` | 7 | hr:position:manage, hr:contract:manage, hr:onboarding:manage | PointsController.java |
| `operations:dashboard:view` | 7 | hr:onboarding:view | DashboardController.java |
| `operations:strategy:manage` | 7 | hr:contract:manage | PromotionController.java |
| `schedule:shift:configure` | 7 | — | ScheduleShiftTypeController.java |
| `scheduler:task:edit` | 7 | — | ScheduledTaskController.java |
| `trace:create` | 7 | finance:cost:create, purchase:order:create, product:recipe:create | FoodTraceCodeController.java, InspectionController.java, MaterialTemplateController.java, MaterialTraceCodeController.java, PurchaseLedgerController.java, RecallController.java… |
| `traceability:quality:manage` | 7 | — | QualityController.java |
| `backup:query` | 6 | — | DataBackupController.java |
| `coupon:view` | 6 | kitchen:view, user:view | CouponController.java |
| `device:management:view` | 6 | hr:department:view, finance:asset:view | DeviceManagementController.java |
| `device:scan:manage` | 6 | finance:asset:manage, order:manage | ScanDeviceController.java |
| `device:simulator:manage` | 6 | hr:salary:manage, finance:asset:manage | DeviceSimulatorController.java |
| `finance:tax:view` | 6 | finance:view, finance:asset:view, finance:invoice:view | TaxCalculationController.java, TaxRecordController.java |
| `hardware:epson-test:manage` | 6 | hr:department:manage, hr:position:manage, hr:employee:manage | EpsonPrinterTestController.java |
| `permission:assign` | 6 | — | PermissionAutoAssignController.java |
| `product:view` | 6 | product:food:view, product:cost:view, product:combo:view | ProductSalesStatsController.java |
| `purchase:trace:view` | 6 | purchase:order:view, purchase:request:view, purchase:arrival:view | ProcurementTraceController.java |
| `system:config` | 6 | — | SystemConfigController.java |
| `system:database:delete` | 6 | — | DatabaseCleanupController.java, SystemDataController.java |
| `traceability:quality:view` | 6 | — | QualityController.java |
| `alert:query` | 5 | finance:cost:query, warehouse:query, inventory:query | AlertController.java |
| `device:manage:view` | 5 | finance:manual-invoice:view, finance:asset:view, order:manage | DeviceController.java |
| `device:management:manage` | 5 | hr:department:manage, finance:asset:manage | DeviceManagementController.java |
| `device:print-template:manage` | 5 | hr:contract-template:manage | PrintTemplateController.java |
| `device:status-history:view` | 5 | — | DeviceStatusHistoryController.java |
| `device:tts:manage` | 5 | finance:asset:manage, order:manage | TTSController.java |
| `device:weighing:manage` | 5 | — | WeighingDeviceController.java |
| `esign:manage` | 5 | order:manage, hr:position:manage, hr:manage | ElectronicContractController.java |
| `finance:auto-voucher:update` | 5 | finance:manual-invoice:update, finance:invoice:update, finance:cost:update | AutoVoucherController.java |
| `finance:auto-voucher:view` | 5 | finance:manual-invoice:view, finance:invoice:view, finance:asset:view | AutoVoucherController.java |
| `finance:bank:manage` | 5 | finance:asset:manage, hr:attendance:manage | BankAccountController.java |
| `finance:period:query` | 5 | finance:cost:query, finance:payment:query, finance:payable:query | AccountingPeriodController.java |
| `finance:voucher:approve` | 5 | finance:payable:approve, finance:invoice:view, finance:settlement:approve | VoucherController.java |
| `hardware:config:view` | 5 | hr:contract:view | HardwareConfigController.java |
| `hardware:print-format:manage` | 5 | hr:recruitment:manage, hr:contract:manage, hr:approval:manage | PrintFormatTemplateController.java |
| `inventory:deduct` | 5 | inventory:query, inventory:approve | InventoryController.java, InventoryDeductionController.java |
| `inventory:execute` | 5 | inventory:query, inventory:approve | InventoryAdjustController.java, InventoryCheckController.java, InventoryLossController.java, InventoryOutboundController.java, InventoryTransferController.java |
| `purchase:supplier:manage` | 5 | asset:manage, purchase:order:view, hr:salary:manage | SupplierPortalController.java |
| `system:stats:view` | 5 | — | ApiStatsController.java |
| `device:print-template:view` | 4 | hr:contract-template:view | PrintTemplateController.java |
| `finance:approval-flow-config:manage` | 4 | hr:approval:manage | ApprovalFlowConfigController.java |
| `finance:receivable:query` | 4 | finance:payable:query, finance:cost:query, finance:payment:query | ReceiptController.java, ReceivableController.java |
| `finance:reimbursement:view` | 4 | finance:asset:view, finance:settlement:approve, finance:payment:void | InvoiceReimbursementController.java |
| `finance:report:config` | 4 | finance:payment:confirm, finance:payment:void, finance:asset:manage | ReportConfigController.java, ReportInsightRuleController.java |
| `member:level:manage` | 4 | hr:knowledge:manage, hr:archive:manage | MemberLevelController.java |
| `notification:template:query` | 4 | — | NotificationController.java |
| `operations:strategy:view` | 4 | product:category:view, hr:contract:view | PromotionController.java |
| `purchase:category:view` | 4 | product:category:view, purchase:order:view, purchase:arrival:view | MaterialCategoryController.java |
| `recharge-plan:manage` | 4 | hr:recruitment:manage, hr:employee:manage, order:manage | RechargePlanController.java |
| `schedule:entry:edit` | 4 | — | ScheduleAttendanceSyncController.java, SchedulePlanController.java, ScheduleSwapController.java |
| `seal:manage` | 4 | asset:manage, hr:salary:manage, hr:manage | SealController.java |
| `seal:view` | 4 | user:view, asset:view, hr:salary:view | SealController.java |
| `store:table:view` | 4 | order:view, hr:salary:view | DiningTableManagementController.java |
| `system:dict:query` | 4 | — | SysDictController.java |
| `system:dict:update` | 4 | — | SysDictController.java |
| `traceability:expiry-alert:view` | 4 | — | ExpiryAlertController.java |
| `warehouse:inventory:view` | 4 | warehouse:query, asset:inventory | StoreInventoryController.java |
| `device:locker:view` | 3 | finance:invoice:view | TakeoutLockerController.java |
| `device:print-task:view` | 3 | — | PrintTaskController.java |
| `finance:approval-flow-config:view` | 3 | finance:manual-invoice:view | ApprovalFlowConfigController.java |
| `finance:approval:view` | 3 | hr:approval:view, finance:view, finance:asset:view | FinanceApprovalController.java |
| `finance:bank:query` | 3 | finance:payment:query, finance:cost:query, finance:payable:query | BankAccountController.java |
| `finance:bank:view` | 3 | finance:view, finance:asset:view, finance:invoice:view | BankAccountController.java |
| `finance:receivable:approve` | 3 | finance:payable:approve, finance:settlement:approve, finance:payable:update | ReceiptController.java, ReceivableController.java |
| `finance:tax:create` | 3 | finance:cost:create, finance:payment:create, finance:payable:create | TaxCalculationController.java, TaxRecordController.java |
| `finance:warning:view` | 3 | finance:view, finance:invoice:view, finance:asset:view | FinanceWarningController.java |
| `hardware:print-format:view` | 3 | hr:recruitment:view, hr:contract:view | PrintFormatTemplateController.java |
| `hardware:view` | 3 | order:view, hr:archive:view, purchase:order:view | CameraController.java, HardwareConfigVersionController.java |
| `member:analysis:manage` | 3 | hr:salary:manage, hr:approval:manage | CustomerAnalysisController.java, MarketingAnalysisController.java |
| `member:recharge:manage` | 3 | hr:recruitment:manage, hr:department:manage, hr:archive:manage | RechargeRecordController.java |
| `notification:message:send` | 3 | — | NotificationController.java |
| `notification:template:edit` | 3 | — | NotificationController.java |
| `recharge:view` | 3 | order:view, hr:archive:view, kitchen:view | RechargeFinanceController.java, RechargeStatsController.java |
| `refund:manage` | 3 | hr:manage, order:manage, hr:attendance:manage | RefundController.java |
| `signature:manage` | 3 | order:manage, asset:manage, hr:manage | ElectronicSignatureController.java |
| `signature:view` | 3 | finance:view, kitchen:view, user:view | ElectronicSignatureController.java |
| `store:table:manage` | 3 | order:manage, hr:salary:manage, hr:archive:manage | DiningTableManagementController.java |
| `trace:delete` | 3 | asset:delete, hr:contract:delete, hr:attendance:delete | InspectionController.java, MaterialTemplateController.java, RecallController.java |
| `warehouse:overview:view` | 3 | warehouse:query | StoreInventoryController.java |
| `alert:edit` | 2 | user:edit, finance:edit | AlertController.java |
| `audit:log:query` | 2 | inventory:query | AuditLogController.java |
| `backup:restore` | 2 | — | DataBackupController.java |
| `device:scan:view` | 2 | kitchen:view | ScanDeviceController.java |
| `device:weighing:view` | 2 | — | WeighingDeviceController.java |
| `esign:view` | 2 | kitchen:view, finance:view, user:view | ElectronicContractController.java |
| `finance:approval:approve` | 2 | finance:payable:approve, finance:settlement:approve, hr:approval:view | FinanceApprovalController.java |
| `finance:audit-log:view` | 2 | finance:asset:view, finance:invoice:view, finance:view | FinanceAuditLogController.java |
| `finance:auto-voucher:create` | 2 | finance:manual-invoice:create, finance:invoice:create, finance:cost:create | AutoVoucherController.java |
| `finance:auto-voucher:delete` | 2 | finance:manual-invoice:delete, finance:invoice:delete, finance:payment:delete | AutoVoucherController.java |
| `finance:bank:update` | 2 | finance:cost:update, finance:payable:update, finance:invoice:update | BankAccountController.java |
| `finance:budget:query` | 2 | finance:cost:query, finance:payment:query, finance:payable:query | BudgetController.java |
| `finance:budget:update` | 2 | finance:cost:update, finance:payable:update, finance:invoice:update | BudgetController.java |
| `finance:invoice:verify` | 2 | finance:manual-invoice:verify, finance:invoice:void, finance:invoice:view | InvoiceVerifyController.java |
| `finance:period:update` | 2 | finance:cost:update, finance:payable:update, finance:invoice:update | AccountingPeriodController.java |
| `finance:record:view` | 2 | finance:view, finance:asset:view, finance:invoice:view | RecordController.java |
| `finance:reimbursement:approve` | 2 | finance:settlement:approve, finance:payable:approve, finance:settlement:create | InvoiceReimbursementController.java |
| `finance:reimbursement:create` | 2 | finance:settlement:create, finance:payment:create, finance:cost:create | InvoiceReimbursementController.java |
| `finance:tax:delete` | 2 | finance:payment:delete, finance:payable:delete, finance:invoice:delete | TaxRecordController.java |
| `finance:voucher:query` | 2 | finance:cost:query, finance:payment:query, finance:payable:query | VoucherController.java |
| `hardware:capture` | 2 | — | CameraController.java, HardwareDeviceController.java |
| `hardware:device:manage` | 2 | hr:department:manage, hr:attendance:manage, hr:archive:manage | HardwareDeviceController.java |
| `hardware:epson-test:view` | 2 | hr:department:view, hr:position:view, hr:employee:view | EpsonPrinterTestController.java |
| `hardware:label-printer:view` | 2 | — | LabelPrinterController.java |
| `inventory:lock` | 2 | inventory:approve, inventory:query, asset:inventory | InventoryController.java |
| `label:template:print` | 2 | — | LabelTemplateController.java |
| `label:template:view` | 2 | hr:contract-template:view, hr:employee:view, employee:view | LabelTemplateController.java |
| `member:alert:manage` | 2 | hr:salary:manage, asset:manage, hr:department:manage | AnomalyAlertController.java |
| `member:level:view` | 2 | employee:view | MemberLevelController.java |
| `member:points:view` | 2 | — | PointsController.java |
| `member:recharge:view` | 2 | hr:recruitment:view | RechargeRecordController.java |
| `notification:deadletter:query` | 2 | — | NotificationController.java |
| `notification:deadletter:retry` | 2 | — | NotificationController.java |
| `notification:message:query` | 2 | — | NotificationController.java |
| `notification:statistics:query` | 2 | — | NotificationController.java |
| `operations:dashboard:manage` | 2 | hr:onboarding:manage | DashboardController.java |
| `permission:view` | 2 | hr:position:view | PermissionAutoAssignController.java |
| `product:food:import` | 2 | product:food:update, product:food:create, product:food:view | FoodController.java |
| `purchase:archive:edit` | 2 | purchase:request:edit, purchase:arrival:view, purchase:arrival:confirm | PurchaseArchiveController.java |
| `purchase:archive:view` | 2 | purchase:arrival:view, purchase:order:view, hr:archive:view | PurchaseArchiveController.java |
| `purchase:category:edit` | 2 | purchase:request:edit, product:category:update, product:category:delete | MaterialCategoryController.java |
| `purchase:order:freeze` | 2 | purchase:order:create, purchase:order:generate, purchase:order:view | PurchaseOrderController.java |
| `purchase:return:view` | 2 | purchase:order:view, purchase:request:view, purchase:arrival:view | PurchaseReturnController.java |
| `purchase:stockin:print` | 2 | purchase:request:edit, purchase:request:create, purchase:request:approve | PurchaseStockinScanController.java |
| `purchase:stockin:scan` | 2 | purchase:request:create, purchase:order:create, purchase:order:confirm | PurchaseStockinScanController.java |
| `purchase:stockin:view` | 2 | purchase:request:view, purchase:order:view, purchase:arrival:view | PurchaseStockinScanController.java |
| `recharge-plan:view` | 2 | kitchen:view | RechargePlanController.java |
| `schedule:plan:create` | 2 | finance:payment:create | ScheduleEngineController.java, SchedulePlanController.java |
| `scheduler:task:delete` | 2 | hr:training:delete, hr:contract:delete | ScheduledTaskController.java |
| `store:queue:view` | 2 | order:view | CallNumberQueueManagementController.java |
| `system:dict:create` | 2 | — | SysDictController.java |
| `system:dict:delete` | 2 | asset:delete | SysDictController.java |
| `system:init:view` | 2 | asset:view | CompanyInitController.java |
| `system:permission:create` | 2 | system:user:assign-store, system:user:unassign-store | PermissionController.java |
| `system:permission:delete` | 2 | system:user:assign-store, hr:position:delete, system:user:unassign-store | PermissionController.java |
| `system:permission:update` | 2 | system:user:assign-store, system:user:unassign-store | PermissionController.java |
| `system:permission:update-sort` | 2 | system:user:assign-store, system:user:unassign-store | PermissionController.java |
| `system:permission:update-status` | 2 | — | PermissionController.java |
| `system:policy:update` | 2 | — | PasswordPolicyController.java |
| `system:policy:view` | 2 | employee:view | PasswordPolicyController.java |
| `system:role:assign-permissions` | 2 | system:user:assign-store, system:user:unassign-store | RoleController.java |
| `system:role:create` | 2 | — | RoleController.java |
| `system:role:delete` | 2 | asset:delete, hr:employee:delete, hr:approval:delete | RoleController.java |
| `system:role:set-data-scope` | 2 | — | RoleController.java |
| `system:role:update` | 2 | — | RoleController.java |
| `system:role:update-status` | 2 | — | RoleController.java |
| `system:user:assign-roles` | 2 | system:user:assign-store, system:user:unassign-store | UserController.java |
| `system:user:create` | 2 | system:user:assign-store, system:user:unassign-store | UserController.java |
| `system:user:reset-password` | 2 | system:user:assign-store, system:user:unassign-store | UserController.java |
| `system:user:unassign-roles` | 2 | system:user:unassign-store, system:user:assign-store | UserController.java |
| `system:user:update` | 2 | system:user:unassign-store, system:user:assign-store | UserController.java |
| `system:user:update-status` | 2 | system:user:unassign-store, system:user:assign-store | UserController.java |
| `traceability:expiry-alert:manage` | 2 | — | ExpiryAlertController.java |
| `warehouse:update` | 2 | warehouse:query, finance:cost:update | WarehouseController.java |
| `admin:user:export` | 1 | — | DataExportController.java |
| `alert:delete` | 1 | asset:delete, hr:salary:delete, hr:training:delete | AlertController.java |
| `audit:log:archive` | 1 | — | AuditLogController.java |
| `audit:log:export` | 1 | — | AuditLogController.java |
| `backup:config` | 1 | — | DataBackupController.java |
| `backup:create` | 1 | — | DataBackupController.java |
| `backup:delete` | 1 | asset:delete | DataBackupController.java |
| `backup:download` | 1 | — | DataBackupController.java |
| `device:communication:view` | 1 | hr:organization:view | DeviceCommunicationTestController.java |
| `device:sdk-printer:manage` | 1 | — | SdkPrinterController.java |
| `device:sdk-printer:view` | 1 | — | SdkPrinterController.java |
| `device:simulator:view` | 1 | — | DeviceSimulatorController.java |
| `device:tts:view` | 1 | finance:asset:view | TTSController.java |
| `esign:delete` | 1 | hr:position:delete, asset:delete, hr:recruitment:delete | ElectronicContractController.java |
| `file:query` | 1 | finance:payable:query, finance:cost:query, inventory:query | FileAttachmentController.java |
| `finance:auto-voucher:export` | 1 | finance:manual-invoice:update, finance:manual-invoice:delete, finance:manual-invoice:create | AutoVoucherController.java |
| `finance:bank:create` | 1 | finance:payment:create, finance:cost:create, finance:payable:create | BankAccountController.java |
| `finance:bank:delete` | 1 | finance:payment:delete, finance:payable:delete, finance:invoice:delete | BankAccountController.java |
| `finance:bank:reconcile` | 1 | finance:payment:create, finance:payment:confirm, finance:asset:view | BankAccountController.java |
| `finance:budget:create` | 1 | finance:cost:create, finance:payment:create, finance:payable:create | BudgetController.java |
| `finance:period:approve` | 1 | finance:payable:approve, finance:settlement:approve, inventory:approve | AccountingPeriodController.java |
| `finance:period:create` | 1 | finance:cost:create, finance:payment:create, finance:payable:create | AccountingPeriodController.java |
| `finance:receipt:delete` | 1 | finance:payment:delete, finance:invoice:delete, finance:payable:delete | ReceiptController.java |
| `finance:receipt:edit` | 1 | finance:payment:edit, finance:edit, finance:cost:update | ReceiptController.java |
| `finance:receivable:create` | 1 | finance:payable:create, finance:invoice:create, finance:cost:create | ReceivableController.java |
| `finance:receivable:delete` | 1 | finance:payable:delete, finance:invoice:delete, finance:payment:delete | ReceivableController.java |
| `finance:receivable:update` | 1 | finance:payable:update, finance:invoice:update, finance:cost:update | ReceivableController.java |
| `finance:record:approve` | 1 | finance:payable:approve, finance:settlement:approve, inventory:approve | RecordController.java |
| `finance:record:create` | 1 | finance:cost:create, finance:payment:create, finance:payable:create | RecordController.java |
| `finance:record:update` | 1 | finance:cost:update, finance:payable:update, finance:invoice:update | RecordController.java |
| `finance:reimbursement:pay` | 1 | finance:settlement:query, finance:settlement:create, finance:settlement:approve | InvoiceReimbursementController.java |
| `finance:statistics:view` | 1 | finance:asset:view, finance:invoice:view, finance:view | FinanceStatisticsController.java |
| `finance:tax:update` | 1 | finance:cost:update, finance:payable:update, finance:invoice:update | TaxRecordController.java |
| `finance:voucher:create` | 1 | finance:invoice:create, finance:cost:create, finance:manual-invoice:create | VoucherController.java |
| `finance:voucher:update` | 1 | finance:invoice:update, finance:cost:update, finance:manual-invoice:update | VoucherController.java |
| `finance:warning:create` | 1 | finance:payment:create, finance:invoice:create, finance:manual-invoice:create | FinanceWarningController.java |
| `finance:warning:delete` | 1 | finance:payment:delete, finance:invoice:delete, finance:manual-invoice:delete | FinanceWarningController.java |
| `finance:warning:process` | 1 | finance:invoice:issue, finance:payable:approve, finance:invoice:void | FinanceWarningController.java |
| `hardware:delete` | 1 | hr:approval:delete, asset:delete, hr:knowledge:delete | HardwareConfigVersionController.java |
| `hardware:device:view` | 1 | finance:invoice:view | HardwareDeviceController.java |
| `hardware:edit` | 1 | — | HardwareConfigVersionController.java |
| `hardware:init:manage` | 1 | hr:training:manage, hr:recruitment:manage, hr:onboarding:manage | HardwareInitController.java |
| `hardware:print` | 1 | — | HardwareDeviceController.java |
| `hardware:scan` | 1 | — | HardwareDeviceController.java |
| `label:template:create` | 1 | finance:settlement:create, finance:payment:create | LabelTemplateController.java |
| `label:template:delete` | 1 | hr:employee:delete, hr:contract-template:delete, hr:department:delete | LabelTemplateController.java |
| `label:template:update` | 1 | — | LabelTemplateController.java |
| `loss-outbound:approve` | 1 | — | LossOutboundController.java |
| `loss-outbound:create` | 1 | product:food:create, product:combo:create | LossOutboundController.java |
| `loss-outbound:list` | 1 | — | LossOutboundController.java |
| `loss-outbound:view` | 1 | — | LossOutboundController.java |
| `member:alert:view` | 1 | hr:salary:view, employee:view | AnomalyAlertController.java |
| `member:export` | 1 | — | MemberController.java |
| `member:recharge:export` | 1 | — | RechargeRecordController.java |
| `notification:deadletter:delete` | 1 | — | NotificationController.java |
| `notification:event:test` | 1 | — | NotificationController.java |
| `notification:setting:edit` | 1 | — | NotificationController.java |
| `notification:setting:query` | 1 | finance:settlement:query | NotificationController.java |
| `notification:template:create` | 1 | — | NotificationController.java |
| `notification:template:delete` | 1 | hr:contract-template:delete | NotificationController.java |
| `order:create` | 1 | purchase:order:create, order:manage, order:add | PosController.java |
| `order:pay` | 1 | order:add, order:manage, order:view | PosController.java |
| `order:refund` | 1 | order:add, order:view | PosController.java |
| `other-inbound:create` | 1 | — | OtherInboundController.java |
| `other-inbound:list` | 1 | — | OtherInboundController.java |
| `other-inbound:view` | 1 | hr:onboarding:view, kitchen:view | OtherInboundController.java |
| `product:analysis:export` | 1 | — | ProductAnalysisController.java |
| `product:analysis:view` | 1 | product:cost:view, hr:analytics:view, product:category:view | ProductAnalysisController.java |
| `product:cost:export` | 1 | product:cost:view, product:combo:create, product:category:update | DishCostController.java |
| `product:food:export` | 1 | product:food:update, product:food:delete, product:food:create | FoodController.java |
| `purchase:archive:create` | 1 | purchase:order:create, purchase:request:create, purchase:order:generate | PurchaseArchiveController.java |
| `purchase:archive:delete` | 1 | purchase:request:delete, purchase:order:create, purchase:arrival:view | PurchaseArchiveController.java |
| `purchase:category:create` | 1 | product:category:create, purchase:order:create, purchase:request:create | MaterialCategoryController.java |
| `purchase:category:delete` | 1 | product:category:delete, purchase:request:delete, product:category:update | MaterialCategoryController.java |
| `purchase:order:terminate` | 1 | purchase:order:generate, purchase:order:create, purchase:order:view | PurchaseOrderController.java |
| `purchase:return:approve` | 1 | purchase:request:approve | PurchaseReturnController.java |
| `purchase:return:confirm` | 1 | purchase:order:confirm, purchase:arrival:confirm, purchase:order:create | PurchaseReturnController.java |
| `purchase:return:create` | 1 | purchase:order:create, purchase:request:create, purchase:order:generate | PurchaseReturnController.java |
| `purchase:return:delete` | 1 | purchase:request:delete, purchase:request:create, purchase:request:view | PurchaseReturnController.java |
| `purchase:return:edit` | 1 | purchase:request:edit, purchase:request:view, purchase:request:delete | PurchaseReturnController.java |
| `purchase:stockin:cancel` | 1 | purchase:request:delete, purchase:request:create | PurchaseStockinScanController.java |
| `purchase:stockin:complete` | 1 | purchase:request:delete, purchase:request:create, purchase:order:create | PurchaseStockinScanController.java |
| `purchase:stockin:intervene` | 1 | purchase:order:view, purchase:order:generate, purchase:request:approve | PurchaseStockinScanController.java |
| `recharge-settings:manage` | 1 | hr:training:manage, hr:position:manage, asset:manage | RechargeSettingsController.java |
| `recharge-settings:view` | 1 | — | RechargeSettingsController.java |
| `refund:view` | 1 | order:view, hr:attendance:view, trace_code:view | RefundController.java |
| `schedule:plan:delete` | 1 | finance:payment:delete, hr:salary:delete, hr:department:delete | SchedulePlanController.java |
| `scheduler:task:create` | 1 | — | ScheduledTaskController.java |
| `scheduler:task:trigger` | 1 | — | ScheduledTaskController.java |
| `seal:delete` | 1 | asset:delete, hr:salary:delete, hr:employee:delete | SealController.java |
| `signature:delete` | 1 | asset:delete | ElectronicSignatureController.java |
| `system:init:manage` | 1 | asset:manage, hr:training:manage | CompanyInitController.java |
| `system:log:view` | 1 | employee:view, asset:view | OperationLogController.java |
| `system:policy:create` | 1 | — | PasswordPolicyController.java |
| `system:policy:delete` | 1 | hr:employee:delete, asset:delete | PasswordPolicyController.java |
| `warehouse:create` | 1 | warehouse:query, finance:cost:create | WarehouseController.java |
| `warehouse:delete` | 1 | asset:delete, warehouse:query | WarehouseController.java |
| `warehouse:inbound:manage` | 1 | warehouse:query | StoreInventoryController.java |
| `warehouse:location:delete` | 1 | hr:position:delete, product:category:delete | InventoryLocationController.java |
| `warehouse:outbound:manage` | 1 | hr:position:manage, warehouse:query | StoreInventoryController.java |

<details><summary>逐条位置（1031 条）</summary>

```
com/foodtraceability/controller/AIModelConfigController.java:52  PreAuthorize  hasAuthority('system:aimodel:manage')
com/foodtraceability/controller/AIModelConfigController.java:61  PreAuthorize  hasAuthority('system:aimodel:manage')
com/foodtraceability/controller/AIModelConfigController.java:69  PreAuthorize  hasAuthority('system:aimodel:manage')
com/foodtraceability/controller/AIModelConfigController.java:82  PreAuthorize  hasAuthority('system:aimodel:manage')
com/foodtraceability/controller/AIModelConfigController.java:97  PreAuthorize  hasAuthority('system:aimodel:manage')
com/foodtraceability/controller/AIModelConfigController.java:110  PreAuthorize  hasAuthority('system:aimodel:manage')
com/foodtraceability/controller/AIModelConfigController.java:125  PreAuthorize  hasAuthority('system:aimodel:manage')
com/foodtraceability/controller/AIModelConfigController.java:142  PreAuthorize  hasAuthority('system:aimodel:manage')
com/foodtraceability/controller/AIModelConfigController.java:157  PreAuthorize  hasAuthority('system:aimodel:manage')
com/foodtraceability/controller/AIModelConfigController.java:179  PreAuthorize  hasAuthority('system:aimodel:manage')
com/foodtraceability/controller/AlertController.java:42  PreAuthorize  hasAuthority('alert:query')
com/foodtraceability/controller/AlertController.java:63  PreAuthorize  hasAuthority('alert:query')
com/foodtraceability/controller/AlertController.java:75  PreAuthorize  hasAuthority('alert:query')
com/foodtraceability/controller/AlertController.java:85  PreAuthorize  hasAuthority('alert:query')
com/foodtraceability/controller/AlertController.java:97  PreAuthorize  hasAuthority('alert:edit')
com/foodtraceability/controller/AlertController.java:117  PreAuthorize  hasAuthority('alert:edit')
com/foodtraceability/controller/AlertController.java:138  PreAuthorize  hasAuthority('alert:delete')
com/foodtraceability/controller/AlertController.java:154  PreAuthorize  hasAuthority('alert:query')
com/foodtraceability/controller/AuditLogController.java:41  PreAuthorize  hasAuthority('audit:log:query')
com/foodtraceability/controller/AuditLogController.java:81  PreAuthorize  hasAuthority('audit:log:query')
com/foodtraceability/controller/AuditLogController.java:94  PreAuthorize  hasAuthority('audit:log:export')
com/foodtraceability/controller/AuditLogController.java:114  PreAuthorize  hasAuthority('audit:log:archive')
com/foodtraceability/controller/CallNumberQueueManagementController.java:54  PreAuthorize  hasAuthority('store:queue:view')
com/foodtraceability/controller/CallNumberQueueManagementController.java:79  PreAuthorize  hasAuthority('store:queue:view')
com/foodtraceability/controller/CameraController.java:30  PreAuthorize  hasAuthority('hardware:capture')
com/foodtraceability/controller/CameraController.java:42  PreAuthorize  hasAuthority('hardware:view')
com/foodtraceability/controller/CompanyInitController.java:42  PreAuthorize  hasAuthority('system:init:view')
com/foodtraceability/controller/CompanyInitController.java:58  PreAuthorize  hasAuthority('system:init:view')
com/foodtraceability/controller/CompanyInitController.java:74  PreAuthorize  hasAuthority('system:init:manage')
com/foodtraceability/controller/CouponController.java:43  PreAuthorize  hasAuthority('coupon:manage')
com/foodtraceability/controller/CouponController.java:55  PreAuthorize  hasAuthority('coupon:view')
com/foodtraceability/controller/CouponController.java:66  PreAuthorize  hasAuthority('coupon:manage')
com/foodtraceability/controller/CouponController.java:87  PreAuthorize  hasAuthority('coupon:manage')
com/foodtraceability/controller/CouponController.java:109  PreAuthorize  hasAuthority('coupon:view')
com/foodtraceability/controller/CouponController.java:125  PreAuthorize  hasAuthority('coupon:view')
com/foodtraceability/controller/CouponController.java:141  PreAuthorize  hasAuthority('coupon:manage')
com/foodtraceability/controller/CouponController.java:158  PreAuthorize  hasAuthority('coupon:manage')
com/foodtraceability/controller/CouponController.java:176  PreAuthorize  hasAuthority('coupon:view')
com/foodtraceability/controller/CouponController.java:188  PreAuthorize  hasAuthority('coupon:view')
com/foodtraceability/controller/CouponController.java:196  PreAuthorize  hasAuthority('coupon:view')
com/foodtraceability/controller/CouponController.java:205  PreAuthorize  hasAuthority('coupon:manage')
com/foodtraceability/controller/CouponController.java:218  PreAuthorize  hasAuthority('coupon:manage')
com/foodtraceability/controller/CouponController.java:230  PreAuthorize  hasAuthority('coupon:manage')
com/foodtraceability/controller/CustomerAnalysisController.java:36  PreAuthorize  hasAuthority('member:analysis:view')
com/foodtraceability/controller/CustomerAnalysisController.java:54  PreAuthorize  hasAuthority('member:analysis:manage')
com/foodtraceability/controller/CustomerAnalysisController.java:70  PreAuthorize  hasAuthority('member:analysis:view')
com/foodtraceability/controller/CustomerAnalysisController.java:86  PreAuthorize  hasAuthority('member:analysis:view')
com/foodtraceability/controller/CustomerAnalysisController.java:103  PreAuthorize  hasAuthority('member:analysis:view')
com/foodtraceability/controller/CustomerAnalysisController.java:120  PreAuthorize  hasAuthority('member:analysis:view')
com/foodtraceability/controller/CustomerAnalysisController.java:137  PreAuthorize  hasAuthority('member:analysis:view')
com/foodtraceability/controller/CustomerAnalysisController.java:154  PreAuthorize  hasAuthority('member:analysis:view')
com/foodtraceability/controller/DashboardController.java:33  PreAuthorize  hasAuthority('operations:dashboard:view')
com/foodtraceability/controller/DashboardController.java:48  PreAuthorize  hasAuthority('operations:dashboard:view')
com/foodtraceability/controller/DashboardController.java:63  PreAuthorize  hasAuthority('operations:dashboard:view')
com/foodtraceability/controller/DashboardController.java:79  PreAuthorize  hasAuthority('operations:dashboard:view')
com/foodtraceability/controller/DashboardController.java:95  PreAuthorize  hasAuthority('operations:dashboard:view')
com/foodtraceability/controller/DashboardController.java:111  PreAuthorize  hasAuthority('operations:dashboard:manage')
com/foodtraceability/controller/DashboardController.java:128  PreAuthorize  hasAuthority('operations:dashboard:view')
com/foodtraceability/controller/DashboardController.java:149  PreAuthorize  hasAuthority('operations:dashboard:view')
com/foodtraceability/controller/DashboardController.java:166  PreAuthorize  hasAuthority('operations:dashboard:manage')
com/foodtraceability/controller/DeviceManagementController.java:58  PreAuthorize  hasAuthority('device:management:view')
com/foodtraceability/controller/DeviceManagementController.java:72  PreAuthorize  hasAuthority('device:management:view')
com/foodtraceability/controller/DeviceManagementController.java:85  PreAuthorize  hasAuthority('device:management:manage')
com/foodtraceability/controller/DeviceManagementController.java:98  PreAuthorize  hasAuthority('device:management:manage')
com/foodtraceability/controller/DeviceManagementController.java:175  PreAuthorize  hasAuthority('device:management:view')
com/foodtraceability/controller/DeviceManagementController.java:187  PreAuthorize  hasAuthority('device:management:manage')
com/foodtraceability/controller/DeviceManagementController.java:205  PreAuthorize  hasAuthority('device:management:manage')
com/foodtraceability/controller/DeviceManagementController.java:219  PreAuthorize  hasAuthority('device:management:manage')
com/foodtraceability/controller/DeviceManagementController.java:233  PreAuthorize  hasAuthority('device:management:view')
com/foodtraceability/controller/DeviceManagementController.java:250  PreAuthorize  hasAuthority('device:management:view')
com/foodtraceability/controller/DeviceManagementController.java:264  PreAuthorize  hasAuthority('device:management:view')
com/foodtraceability/controller/DeviceStatusHistoryController.java:40  PreAuthorize  hasAuthority('device:status-history:view')
com/foodtraceability/controller/DeviceStatusHistoryController.java:56  PreAuthorize  hasAuthority('device:status-history:view')
com/foodtraceability/controller/DeviceStatusHistoryController.java:76  PreAuthorize  hasAuthority('device:status-history:view')
com/foodtraceability/controller/DeviceStatusHistoryController.java:100  PreAuthorize  hasAuthority('device:status-history:view')
com/foodtraceability/controller/DeviceStatusHistoryController.java:122  PreAuthorize  hasAuthority('device:status-history:view')
com/foodtraceability/controller/DiningTableManagementController.java:58  PreAuthorize  hasAuthority('store:table:view')
com/foodtraceability/controller/DiningTableManagementController.java:82  PreAuthorize  hasAuthority('store:table:view')
com/foodtraceability/controller/DiningTableManagementController.java:94  PreAuthorize  hasAuthority('store:table:manage')
com/foodtraceability/controller/DiningTableManagementController.java:122  PreAuthorize  hasAuthority('store:table:manage')
com/foodtraceability/controller/DiningTableManagementController.java:151  PreAuthorize  hasAuthority('store:table:manage')
com/foodtraceability/controller/DiningTableManagementController.java:177  PreAuthorize  hasAuthority('store:table:view')
com/foodtraceability/controller/DiningTableManagementController.java:190  PreAuthorize  hasAuthority('store:table:view')
com/foodtraceability/controller/DishCostController.java:148  PreAuthorize  hasAuthority('product:cost:export')
com/foodtraceability/controller/ElectronicContractController.java:38  PreAuthorize  hasAuthority('esign:view')
com/foodtraceability/controller/ElectronicContractController.java:54  PreAuthorize  hasAuthority('esign:manage')
com/foodtraceability/controller/ElectronicContractController.java:61  PreAuthorize  hasAuthority('esign:manage')
com/foodtraceability/controller/ElectronicContractController.java:68  PreAuthorize  hasAuthority('esign:view')
com/foodtraceability/controller/ElectronicContractController.java:75  PreAuthorize  hasAuthority('esign:delete')
com/foodtraceability/controller/ElectronicContractController.java:83  PreAuthorize  hasAuthority('esign:manage')
com/foodtraceability/controller/ElectronicContractController.java:91  PreAuthorize  hasAuthority('esign:manage')
com/foodtraceability/controller/ElectronicContractController.java:141  PreAuthorize  hasAuthority('esign:manage')
com/foodtraceability/controller/ElectronicSignatureController.java:61  PreAuthorize  hasAuthority('signature:view')
com/foodtraceability/controller/ElectronicSignatureController.java:74  PreAuthorize  hasAuthority('signature:manage')
com/foodtraceability/controller/ElectronicSignatureController.java:90  PreAuthorize  hasAuthority('signature:delete')
com/foodtraceability/controller/ElectronicSignatureController.java:100  PreAuthorize  hasAuthority('signature:view')
com/foodtraceability/controller/ElectronicSignatureController.java:110  PreAuthorize  hasAuthority('signature:view')
com/foodtraceability/controller/ElectronicSignatureController.java:120  PreAuthorize  hasAuthority('signature:manage')
com/foodtraceability/controller/ElectronicSignatureController.java:132  PreAuthorize  hasAuthority('signature:manage')
com/foodtraceability/controller/ExpiryAlertController.java:39  PreAuthorize  hasAuthority('traceability:expiry-alert:view')
com/foodtraceability/controller/ExpiryAlertController.java:48  PreAuthorize  hasAuthority('traceability:expiry-alert:view')
com/foodtraceability/controller/ExpiryAlertController.java:57  PreAuthorize  hasAuthority('traceability:expiry-alert:view')
com/foodtraceability/controller/ExpiryAlertController.java:66  PreAuthorize  hasAuthority('traceability:expiry-alert:manage')
com/foodtraceability/controller/ExpiryAlertController.java:78  PreAuthorize  hasAuthority('traceability:expiry-alert:manage')
com/foodtraceability/controller/ExpiryAlertController.java:90  PreAuthorize  hasAuthority('traceability:expiry-alert:view')
com/foodtraceability/controller/FoodController.java:162  PreAuthorize  hasAuthority('product:food:import')
com/foodtraceability/controller/FoodController.java:172  PreAuthorize  hasAuthority('product:food:export')
com/foodtraceability/controller/FoodController.java:182  PreAuthorize  hasAuthority('product:food:import')
com/foodtraceability/controller/FoodTraceCodeController.java:38  PreAuthorize  hasAuthority('trace:create')
com/foodtraceability/controller/FoodTraceCodeController.java:50  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/FoodTraceCodeController.java:62  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/FoodTraceCodeController.java:74  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/FoodTraceCodeController.java:107  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/FoodTraceCodeController.java:116  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/FoodTraceCodeController.java:130  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/FoodTraceCodeController.java:145  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/FoodTraceCodeController.java:158  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/FoodTraceCodeController.java:171  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/FoodTraceCodeController.java:185  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/FoodTraceCodeController.java:199  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/FoodTraceCodeController.java:208  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/FoodTraceCodeController.java:217  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/InspectionController.java:46  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/InspectionController.java:55  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/InspectionController.java:64  PreAuthorize  hasAuthority('trace:create')
com/foodtraceability/controller/InspectionController.java:75  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/InspectionController.java:85  PreAuthorize  hasAuthority('trace:delete')
com/foodtraceability/controller/InspectionController.java:94  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/InspectionController.java:103  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/InspectionController.java:112  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/InspectionController.java:121  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/InventoryAdjustController.java:98  PreAuthorize  hasAuthority('inventory:create')
com/foodtraceability/controller/InventoryAdjustController.java:153  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventoryAdjustController.java:173  PreAuthorize  hasAuthority('inventory:delete')
com/foodtraceability/controller/InventoryAdjustController.java:208  PreAuthorize  hasAuthority('inventory:execute')
com/foodtraceability/controller/InventoryCategoryController.java:38  PreAuthorize  hasAuthority('inventory:create')
com/foodtraceability/controller/InventoryCategoryController.java:54  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventoryCategoryController.java:89  PreAuthorize  hasAuthority('inventory:delete')
com/foodtraceability/controller/InventoryCheckController.java:35  PreAuthorize  hasAuthority('inventory:create')
com/foodtraceability/controller/InventoryCheckController.java:89  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventoryCheckController.java:107  PreAuthorize  hasAuthority('inventory:delete')
com/foodtraceability/controller/InventoryCheckController.java:165  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventoryCheckController.java:190  PreAuthorize  hasAuthority('inventory:execute')
com/foodtraceability/controller/InventoryController.java:107  PreAuthorize  hasAuthority('inventory:lock')
com/foodtraceability/controller/InventoryController.java:122  PreAuthorize  hasAuthority('inventory:lock')
com/foodtraceability/controller/InventoryController.java:139  PreAuthorize  hasAuthority('inventory:deduct')
com/foodtraceability/controller/InventoryController.java:156  PreAuthorize  hasAuthority('inventory:create')
com/foodtraceability/controller/InventoryDeductionController.java:21  PreAuthorize  hasAuthority('inventory:deduct')
com/foodtraceability/controller/InventoryDeductionController.java:34  PreAuthorize  hasAuthority('inventory:deduct')
com/foodtraceability/controller/InventoryDeductionController.java:47  PreAuthorize  hasAuthority('inventory:deduct')
com/foodtraceability/controller/InventoryDeductionController.java:60  PreAuthorize  hasAuthority('inventory:deduct')
com/foodtraceability/controller/InventoryLocationController.java:77  PreAuthorize  hasAuthority('inventory:create')
com/foodtraceability/controller/InventoryLocationController.java:92  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventoryLocationController.java:109  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventoryLocationController.java:127  PreAuthorize  hasAuthority('warehouse:location:delete')
com/foodtraceability/controller/InventoryLossController.java:40  PreAuthorize  hasAuthority('inventory:create')
com/foodtraceability/controller/InventoryLossController.java:62  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventoryLossController.java:103  PreAuthorize  hasAuthority('inventory:delete')
com/foodtraceability/controller/InventoryLossController.java:164  PreAuthorize  hasAuthority('inventory:execute')
com/foodtraceability/controller/InventoryOutboundController.java:98  PreAuthorize  hasAuthority('inventory:create')
com/foodtraceability/controller/InventoryOutboundController.java:137  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventoryOutboundController.java:157  PreAuthorize  hasAuthority('inventory:delete')
com/foodtraceability/controller/InventoryOutboundController.java:192  PreAuthorize  hasAuthority('inventory:execute')
com/foodtraceability/controller/InventorySettingController.java:62  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventorySettingController.java:71  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventoryTransferController.java:43  PreAuthorize  hasAuthority('inventory:create')
com/foodtraceability/controller/InventoryTransferController.java:66  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventoryTransferController.java:108  PreAuthorize  hasAuthority('inventory:delete')
com/foodtraceability/controller/InventoryTransferController.java:169  PreAuthorize  hasAuthority('inventory:execute')
com/foodtraceability/controller/InventoryUnitController.java:38  PreAuthorize  hasAuthority('inventory:create')
com/foodtraceability/controller/InventoryUnitController.java:54  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventoryUnitController.java:89  PreAuthorize  hasAuthority('inventory:delete')
com/foodtraceability/controller/InventoryWarningController.java:43  PreAuthorize  hasAuthority('inventory:create')
com/foodtraceability/controller/InventoryWarningController.java:69  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventoryWarningController.java:115  PreAuthorize  hasAuthority('inventory:delete')
com/foodtraceability/controller/InventoryWarningController.java:155  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventoryWarningController.java:175  PreAuthorize  hasAuthority('inventory:create')
com/foodtraceability/controller/InventoryWarningController.java:208  PreAuthorize  hasAuthority('inventory:create')
com/foodtraceability/controller/InventoryWarningRuleController.java:39  PreAuthorize  hasAuthority('inventory:create')
com/foodtraceability/controller/InventoryWarningRuleController.java:81  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/InventoryWarningRuleController.java:108  PreAuthorize  hasAuthority('inventory:delete')
com/foodtraceability/controller/InventoryWarningRuleController.java:149  PreAuthorize  hasAuthority('inventory:update')
com/foodtraceability/controller/KnowledgeBaseController.java:58  PreAuthorize  hasAuthority('hr:knowledge-base:view')
com/foodtraceability/controller/KnowledgeBaseController.java:73  PreAuthorize  hasAuthority('hr:knowledge-base:view')
com/foodtraceability/controller/KnowledgeBaseController.java:89  PreAuthorize  hasAuthority('hr:knowledge-base:manage')
com/foodtraceability/controller/KnowledgeBaseController.java:104  PreAuthorize  hasAuthority('hr:knowledge-base:manage')
com/foodtraceability/controller/KnowledgeBaseController.java:121  PreAuthorize  hasAuthority('hr:knowledge-base:manage')
com/foodtraceability/controller/KnowledgeBaseController.java:141  PreAuthorize  hasAuthority('hr:knowledge-base:manage')
com/foodtraceability/controller/KnowledgeBaseController.java:157  PreAuthorize  hasAuthority('hr:knowledge-base:manage')
com/foodtraceability/controller/KnowledgeBaseController.java:177  PreAuthorize  hasAuthority('hr:knowledge-base:manage')
com/foodtraceability/controller/KnowledgeBaseController.java:194  PreAuthorize  hasAuthority('hr:knowledge-base:manage')
com/foodtraceability/controller/KnowledgeBaseController.java:211  PreAuthorize  hasAuthority('hr:knowledge-base:manage')
com/foodtraceability/controller/KnowledgeBaseController.java:228  PreAuthorize  hasAuthority('hr:knowledge-base:view')
com/foodtraceability/controller/KnowledgeBaseController.java:249  PreAuthorize  hasAuthority('hr:knowledge-base:view')
com/foodtraceability/controller/KnowledgeBaseController.java:269  PreAuthorize  hasAuthority('hr:knowledge-base:view')
com/foodtraceability/controller/KnowledgeBaseController.java:285  PreAuthorize  hasAuthority('hr:knowledge-base:view')
com/foodtraceability/controller/KnowledgeBaseController.java:301  PreAuthorize  hasAuthority('hr:knowledge-base:view')
com/foodtraceability/controller/KnowledgeBaseController.java:317  PreAuthorize  hasAuthority('hr:knowledge-base:manage')
com/foodtraceability/controller/LabelTemplateController.java:72  PreAuthorize  hasAuthority('label:template:view')
com/foodtraceability/controller/LabelTemplateController.java:104  PreAuthorize  hasAuthority('label:template:view')
com/foodtraceability/controller/LabelTemplateController.java:137  PreAuthorize  hasAuthority('label:template:create')
com/foodtraceability/controller/LabelTemplateController.java:213  PreAuthorize  hasAuthority('label:template:update')
com/foodtraceability/controller/LabelTemplateController.java:278  PreAuthorize  hasAuthority('label:template:delete')
com/foodtraceability/controller/LabelTemplateController.java:889  PreAuthorize  hasAuthority('label:template:print')
com/foodtraceability/controller/LabelTemplateController.java:1039  PreAuthorize  hasAuthority('label:template:print')
com/foodtraceability/controller/LossOutboundController.java:29  PreAuthorize  hasAuthority('loss-outbound:list')
com/foodtraceability/controller/LossOutboundController.java:43  PreAuthorize  hasAuthority('loss-outbound:view')
com/foodtraceability/controller/LossOutboundController.java:54  PreAuthorize  hasAuthority('loss-outbound:create')
com/foodtraceability/controller/LossOutboundController.java:64  PreAuthorize  hasAuthority('loss-outbound:approve')
com/foodtraceability/controller/MarketingAnalysisController.java:31  PreAuthorize  hasAuthority('member:analysis:view')
com/foodtraceability/controller/MarketingAnalysisController.java:41  PreAuthorize  hasAuthority('member:analysis:manage')
com/foodtraceability/controller/MarketingAnalysisController.java:49  PreAuthorize  hasAuthority('member:analysis:view')
com/foodtraceability/controller/MarketingAnalysisController.java:59  PreAuthorize  hasAuthority('member:analysis:view')
com/foodtraceability/controller/MarketingAnalysisController.java:69  PreAuthorize  hasAuthority('member:analysis:view')
com/foodtraceability/controller/MarketingAnalysisController.java:78  PreAuthorize  hasAuthority('member:analysis:view')
com/foodtraceability/controller/MarketingAnalysisController.java:89  PreAuthorize  hasAuthority('member:analysis:view')
com/foodtraceability/controller/MarketingAnalysisController.java:100  PreAuthorize  hasAuthority('member:analysis:manage')
com/foodtraceability/controller/MarketingMemberController.java:33  PreAuthorize  hasAuthority('member:manage')
com/foodtraceability/controller/MarketingMemberController.java:45  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/MarketingMemberController.java:56  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/MarketingMemberController.java:64  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/MarketingMemberController.java:75  PreAuthorize  hasAuthority('member:manage')
com/foodtraceability/controller/MarketingMemberController.java:88  PreAuthorize  hasAuthority('member:manage')
com/foodtraceability/controller/MarketingMemberController.java:100  PreAuthorize  hasAuthority('member:manage')
com/foodtraceability/controller/MarketingMemberController.java:108  PreAuthorize  hasAuthority('member:manage')
com/foodtraceability/controller/MarketingMemberController.java:124  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/MarketingMemberController.java:139  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/MaterialTemplateController.java:31  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/MaterialTemplateController.java:42  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/MaterialTemplateController.java:69  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/MaterialTemplateController.java:76  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/MaterialTemplateController.java:83  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/MaterialTemplateController.java:90  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/MaterialTemplateController.java:97  PreAuthorize  hasAuthority('trace:create')
com/foodtraceability/controller/MaterialTemplateController.java:106  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/MaterialTemplateController.java:115  PreAuthorize  hasAuthority('trace:delete')
com/foodtraceability/controller/MaterialTemplateController.java:122  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/MaterialTraceCodeController.java:37  PreAuthorize  hasAuthority('trace:create')
com/foodtraceability/controller/MaterialTraceCodeController.java:49  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/MaterialTraceCodeController.java:61  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/MaterialTraceCodeController.java:94  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/MaterialTraceCodeController.java:103  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/MaterialTraceCodeController.java:119  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/MaterialTraceCodeController.java:135  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/MaterialTraceCodeController.java:149  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/MaterialTraceCodeController.java:157  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/MaterialTraceCodeController.java:166  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/MaterialTraceCodeController.java:178  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/MaterialTraceCodeController.java:187  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/MaterialTraceCodeController.java:201  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/MaterialTraceCodeController.java:215  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/MaterialTraceCodeController.java:233  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/NotificationController.java:83  PreAuthorize  hasAuthority('notification:template:query')
com/foodtraceability/controller/NotificationController.java:108  PreAuthorize  hasAuthority('notification:template:query')
com/foodtraceability/controller/NotificationController.java:120  PreAuthorize  hasAuthority('notification:template:create')
com/foodtraceability/controller/NotificationController.java:139  PreAuthorize  hasAuthority('notification:template:edit')
com/foodtraceability/controller/NotificationController.java:160  PreAuthorize  hasAuthority('notification:template:delete')
com/foodtraceability/controller/NotificationController.java:177  PreAuthorize  hasAuthority('notification:template:edit')
com/foodtraceability/controller/NotificationController.java:197  PreAuthorize  hasAuthority('notification:template:edit')
com/foodtraceability/controller/NotificationController.java:217  PreAuthorize  hasAuthority('notification:template:query')
com/foodtraceability/controller/NotificationController.java:244  PreAuthorize  hasAuthority('notification:message:send')
com/foodtraceability/controller/NotificationController.java:263  PreAuthorize  hasAuthority('notification:message:send')
com/foodtraceability/controller/NotificationController.java:282  PreAuthorize  hasAuthority('notification:message:query')
com/foodtraceability/controller/NotificationController.java:311  PreAuthorize  hasAuthority('notification:message:query')
com/foodtraceability/controller/NotificationController.java:323  PreAuthorize  hasAuthority('notification:message:send')
com/foodtraceability/controller/NotificationController.java:345  PreAuthorize  hasAuthority('notification:setting:query')
com/foodtraceability/controller/NotificationController.java:354  PreAuthorize  hasAuthority('notification:setting:edit')
com/foodtraceability/controller/NotificationController.java:371  PreAuthorize  hasAuthority('notification:statistics:query')
com/foodtraceability/controller/NotificationController.java:395  PreAuthorize  hasAuthority('notification:statistics:query')
com/foodtraceability/controller/NotificationController.java:418  PreAuthorize  hasAuthority('notification:template:query')
com/foodtraceability/controller/NotificationController.java:609  PreAuthorize  hasAuthority('notification:event:test')
com/foodtraceability/controller/NotificationController.java:647  PreAuthorize  hasAuthority('notification:deadletter:query')
com/foodtraceability/controller/NotificationController.java:663  PreAuthorize  hasAuthority('notification:deadletter:query')
com/foodtraceability/controller/NotificationController.java:683  PreAuthorize  hasAuthority('notification:deadletter:retry')
com/foodtraceability/controller/NotificationController.java:706  PreAuthorize  hasAuthority('notification:deadletter:retry')
com/foodtraceability/controller/NotificationController.java:730  PreAuthorize  hasAuthority('notification:deadletter:delete')
com/foodtraceability/controller/OperationLogController.java:31  PreAuthorize  hasAuthority('system:log:view')
com/foodtraceability/controller/OtherInboundController.java:29  PreAuthorize  hasAuthority('other-inbound:list')
com/foodtraceability/controller/OtherInboundController.java:42  PreAuthorize  hasAuthority('other-inbound:create')
com/foodtraceability/controller/OtherInboundController.java:51  PreAuthorize  hasAuthority('other-inbound:view')
com/foodtraceability/controller/PasswordPolicyController.java:31  PreAuthorize  hasAuthority('system:policy:view')
com/foodtraceability/controller/PasswordPolicyController.java:39  PreAuthorize  hasAuthority('system:policy:view')
com/foodtraceability/controller/PasswordPolicyController.java:47  PreAuthorize  hasAuthority('system:policy:create')
com/foodtraceability/controller/PasswordPolicyController.java:56  PreAuthorize  hasAuthority('system:policy:update')
com/foodtraceability/controller/PasswordPolicyController.java:71  PreAuthorize  hasAuthority('system:policy:update')
com/foodtraceability/controller/PasswordPolicyController.java:79  PreAuthorize  hasAuthority('system:policy:delete')
com/foodtraceability/controller/PermissionAutoAssignController.java:37  PreAuthorize  hasAuthority('permission:assign')
com/foodtraceability/controller/PermissionAutoAssignController.java:49  PreAuthorize  hasAuthority('permission:assign')
com/foodtraceability/controller/PermissionAutoAssignController.java:66  PreAuthorize  hasAuthority('permission:assign')
com/foodtraceability/controller/PermissionAutoAssignController.java:82  PreAuthorize  hasAuthority('permission:assign')
com/foodtraceability/controller/PermissionAutoAssignController.java:97  PreAuthorize  hasAuthority('permission:view')
com/foodtraceability/controller/PermissionAutoAssignController.java:109  PreAuthorize  hasAuthority('permission:view')
com/foodtraceability/controller/PermissionAutoAssignController.java:121  PreAuthorize  hasAuthority('permission:assign')
com/foodtraceability/controller/PermissionAutoAssignController.java:134  PreAuthorize  hasAuthority('permission:assign')
com/foodtraceability/controller/PermissionController.java:63  RequiresPermission  @RequiresPermission('system:permission:read')
com/foodtraceability/controller/PermissionController.java:64  PreAuthorize  hasAuthority('system:permission:read')
com/foodtraceability/controller/PermissionController.java:81  RequiresPermission  @RequiresPermission('system:permission:read')
com/foodtraceability/controller/PermissionController.java:82  PreAuthorize  hasAuthority('system:permission:read')
com/foodtraceability/controller/PermissionController.java:99  RequiresPermission  @RequiresPermission('system:permission:read')
com/foodtraceability/controller/PermissionController.java:100  PreAuthorize  hasAuthority('system:permission:read')
com/foodtraceability/controller/PermissionController.java:120  RequiresPermission  @RequiresPermission('system:permission:read')
com/foodtraceability/controller/PermissionController.java:121  PreAuthorize  hasAuthority('system:permission:read')
com/foodtraceability/controller/PermissionController.java:153  RequiresPermission  @RequiresPermission('system:permission:read')
com/foodtraceability/controller/PermissionController.java:154  PreAuthorize  hasAuthority('system:permission:read')
com/foodtraceability/controller/PermissionController.java:188  RequiresPermission  @RequiresPermission('system:permission:read')
com/foodtraceability/controller/PermissionController.java:189  PreAuthorize  hasAuthority('system:permission:read')
com/foodtraceability/controller/PermissionController.java:207  RequiresPermission  @RequiresPermission('system:permission:create')
com/foodtraceability/controller/PermissionController.java:208  PreAuthorize  hasAuthority('system:permission:create')
com/foodtraceability/controller/PermissionController.java:230  RequiresPermission  @RequiresPermission('system:permission:update')
com/foodtraceability/controller/PermissionController.java:231  PreAuthorize  hasAuthority('system:permission:update')
com/foodtraceability/controller/PermissionController.java:256  RequiresPermission  @RequiresPermission('system:permission:delete')
com/foodtraceability/controller/PermissionController.java:257  PreAuthorize  hasAuthority('system:permission:delete')
com/foodtraceability/controller/PermissionController.java:279  RequiresPermission  @RequiresPermission('system:permission:update-status')
com/foodtraceability/controller/PermissionController.java:280  PreAuthorize  hasAuthority('system:permission:update-status')
com/foodtraceability/controller/PermissionController.java:307  RequiresPermission  @RequiresPermission('system:permission:update-sort')
com/foodtraceability/controller/PermissionController.java:308  PreAuthorize  hasAuthority('system:permission:update-sort')
com/foodtraceability/controller/PermissionController.java:365  RequiresPermission  @RequiresPermission('system:permission:read')
com/foodtraceability/controller/PermissionController.java:366  PreAuthorize  hasAuthority('system:permission:read')
com/foodtraceability/controller/PermissionController.java:382  RequiresPermission  @RequiresPermission('system:permission:read')
com/foodtraceability/controller/PermissionController.java:383  PreAuthorize  hasAuthority('system:permission:read')
com/foodtraceability/controller/PermissionController.java:400  RequiresPermission  @RequiresPermission('system:permission:read')
com/foodtraceability/controller/PermissionController.java:401  PreAuthorize  hasAuthority('system:permission:read')
com/foodtraceability/controller/PermissionTemplateController.java:45  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/PermissionTemplateController.java:53  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/PermissionTemplateController.java:61  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/PermissionTemplateController.java:74  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/PermissionTemplateController.java:87  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/PermissionTemplateController.java:99  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/PermissionTemplateController.java:112  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/PermissionTemplateController.java:127  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/PointsController.java:35  PreAuthorize  hasAuthority('member:points:manage')
com/foodtraceability/controller/PointsController.java:35  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/PointsController.java:48  PreAuthorize  hasAuthority('member:points:manage')
com/foodtraceability/controller/PointsController.java:48  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/PointsController.java:60  PreAuthorize  hasAuthority('member:points:manage')
com/foodtraceability/controller/PointsController.java:77  PreAuthorize  hasAuthority('member:points:manage')
com/foodtraceability/controller/PointsController.java:89  PreAuthorize  hasAuthority('member:points:view')
com/foodtraceability/controller/PointsController.java:99  PreAuthorize  hasAuthority('member:points:view')
com/foodtraceability/controller/PointsController.java:108  PreAuthorize  hasAuthority('member:points:manage')
com/foodtraceability/controller/PointsController.java:115  PreAuthorize  hasAuthority('member:points:manage')
com/foodtraceability/controller/PointsController.java:128  PreAuthorize  hasAuthority('member:points:manage')
com/foodtraceability/controller/ProductAnalysisController.java:25  PreAuthorize  hasAuthority('product:analysis:view')
com/foodtraceability/controller/ProductAnalysisController.java:61  PreAuthorize  hasAuthority('product:analysis:export')
com/foodtraceability/controller/ProductSalesStatsController.java:34  PreAuthorize  hasAuthority('product:view')
com/foodtraceability/controller/ProductSalesStatsController.java:52  PreAuthorize  hasAuthority('product:view')
com/foodtraceability/controller/ProductSalesStatsController.java:67  PreAuthorize  hasAuthority('product:view')
com/foodtraceability/controller/ProductSalesStatsController.java:82  PreAuthorize  hasAuthority('product:view')
com/foodtraceability/controller/ProductSalesStatsController.java:95  PreAuthorize  hasAuthority('product:view')
com/foodtraceability/controller/ProductSalesStatsController.java:109  PreAuthorize  hasAuthority('product:view')
com/foodtraceability/controller/PromotionController.java:33  PreAuthorize  hasAuthority('operations:strategy:manage')
com/foodtraceability/controller/PromotionController.java:45  PreAuthorize  hasAuthority('operations:strategy:manage')
com/foodtraceability/controller/PromotionController.java:58  PreAuthorize  hasAuthority('operations:strategy:view')
com/foodtraceability/controller/PromotionController.java:66  PreAuthorize  hasAuthority('operations:strategy:view')
com/foodtraceability/controller/PromotionController.java:74  PreAuthorize  hasAuthority('operations:strategy:manage')
com/foodtraceability/controller/PromotionController.java:86  PreAuthorize  hasAuthority('operations:strategy:manage')
com/foodtraceability/controller/PromotionController.java:98  PreAuthorize  hasAuthority('operations:strategy:manage')
com/foodtraceability/controller/PromotionController.java:110  PreAuthorize  hasAuthority('operations:strategy:manage')
com/foodtraceability/controller/PromotionController.java:122  PreAuthorize  hasAuthority('operations:strategy:view')
com/foodtraceability/controller/PromotionController.java:133  PreAuthorize  hasAuthority('operations:strategy:view')
com/foodtraceability/controller/PromotionController.java:141  PreAuthorize  hasAuthority('operations:strategy:manage')
com/foodtraceability/controller/PurchaseLedgerController.java:29  PreAuthorize  hasAuthority('trace:create')
com/foodtraceability/controller/PurchaseLedgerController.java:37  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/PurchaseLedgerController.java:45  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/PurchaseLedgerController.java:53  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/PurchaseOrderController.java:205  PreAuthorize  hasAuthority('purchase:order:terminate')
com/foodtraceability/controller/PurchaseOrderController.java:219  PreAuthorize  hasAuthority('purchase:order:freeze')
com/foodtraceability/controller/PurchaseOrderController.java:233  PreAuthorize  hasAuthority('purchase:order:freeze')
com/foodtraceability/controller/PurchaseReturnController.java:31  PreAuthorize  hasAuthority('purchase:return:view')
com/foodtraceability/controller/PurchaseReturnController.java:47  PreAuthorize  hasAuthority('purchase:return:create')
com/foodtraceability/controller/PurchaseReturnController.java:55  PreAuthorize  hasAuthority('purchase:return:edit')
com/foodtraceability/controller/PurchaseReturnController.java:64  PreAuthorize  hasAuthority('purchase:return:view')
com/foodtraceability/controller/PurchaseReturnController.java:71  PreAuthorize  hasAuthority('purchase:return:delete')
com/foodtraceability/controller/PurchaseReturnController.java:79  PreAuthorize  hasAuthority('purchase:return:approve')
com/foodtraceability/controller/PurchaseReturnController.java:88  PreAuthorize  hasAuthority('purchase:return:confirm')
com/foodtraceability/controller/PurchaseStockinScanController.java:32  PreAuthorize  hasAuthority('purchase:stockin:scan')
com/foodtraceability/controller/PurchaseStockinScanController.java:40  PreAuthorize  hasAuthority('purchase:stockin:scan')
com/foodtraceability/controller/PurchaseStockinScanController.java:48  PreAuthorize  hasAuthority('purchase:stockin:view')
com/foodtraceability/controller/PurchaseStockinScanController.java:59  PreAuthorize  hasAuthority('purchase:stockin:intervene')
com/foodtraceability/controller/PurchaseStockinScanController.java:67  PreAuthorize  hasAuthority('purchase:stockin:view')
com/foodtraceability/controller/PurchaseStockinScanController.java:75  PreAuthorize  hasAuthority('purchase:stockin:cancel')
com/foodtraceability/controller/PurchaseStockinScanController.java:85  PreAuthorize  hasAuthority('purchase:stockin:complete')
com/foodtraceability/controller/PurchaseStockinScanController.java:93  PreAuthorize  hasAuthority('purchase:stockin:print')
com/foodtraceability/controller/PurchaseStockinScanController.java:106  PreAuthorize  hasAuthority('purchase:stockin:print')
com/foodtraceability/controller/QualityController.java:49  PreAuthorize  hasAuthority('traceability:quality:view')
com/foodtraceability/controller/QualityController.java:58  PreAuthorize  hasAuthority('traceability:quality:view')
com/foodtraceability/controller/QualityController.java:67  PreAuthorize  hasAuthority('traceability:quality:manage')
com/foodtraceability/controller/QualityController.java:78  PreAuthorize  hasAuthority('traceability:quality:manage')
com/foodtraceability/controller/QualityController.java:87  PreAuthorize  hasAuthority('traceability:quality:manage')
com/foodtraceability/controller/QualityController.java:96  PreAuthorize  hasAuthority('traceability:quality:view')
com/foodtraceability/controller/QualityController.java:107  PreAuthorize  hasAuthority('traceability:quality:manage')
com/foodtraceability/controller/QualityController.java:122  PreAuthorize  hasAuthority('traceability:quality:view')
com/foodtraceability/controller/QualityController.java:133  PreAuthorize  hasAuthority('traceability:quality:view')
com/foodtraceability/controller/QualityController.java:142  PreAuthorize  hasAuthority('traceability:quality:view')
com/foodtraceability/controller/QualityController.java:151  PreAuthorize  hasAuthority('traceability:quality:manage')
com/foodtraceability/controller/QualityController.java:160  PreAuthorize  hasAuthority('traceability:quality:manage')
com/foodtraceability/controller/QualityController.java:170  PreAuthorize  hasAuthority('traceability:quality:manage')
com/foodtraceability/controller/RecallController.java:54  PreAuthorize  hasAuthority('trace:recall')
com/foodtraceability/controller/RecallController.java:63  PreAuthorize  hasAuthority('trace:recall')
com/foodtraceability/controller/RecallController.java:74  PreAuthorize  hasAuthority('trace:recall')
com/foodtraceability/controller/RecallController.java:83  PreAuthorize  hasAuthority('trace:recall')
com/foodtraceability/controller/RecallController.java:92  PreAuthorize  hasAuthority('trace:recall')
com/foodtraceability/controller/RecallController.java:101  PreAuthorize  hasAuthority('trace:recall')
com/foodtraceability/controller/RecallController.java:110  PreAuthorize  hasAuthority('trace:recall')
com/foodtraceability/controller/RecallController.java:121  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/RecallController.java:130  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/RecallController.java:139  PreAuthorize  hasAuthority('trace:create')
com/foodtraceability/controller/RecallController.java:150  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/RecallController.java:159  PreAuthorize  hasAuthority('trace:delete')
com/foodtraceability/controller/RoleController.java:52  RequiresPermission  @RequiresPermission('system:role:create')
com/foodtraceability/controller/RoleController.java:53  PreAuthorize  hasAuthority('system:role:create')
com/foodtraceability/controller/RoleController.java:69  RequiresPermission  @RequiresPermission('system:role:update')
com/foodtraceability/controller/RoleController.java:70  PreAuthorize  hasAuthority('system:role:update')
com/foodtraceability/controller/RoleController.java:87  RequiresPermission  @RequiresPermission('system:role:delete')
com/foodtraceability/controller/RoleController.java:88  PreAuthorize  hasAuthority('system:role:delete')
com/foodtraceability/controller/RoleController.java:107  RequiresPermission  @RequiresPermission('system:role:read')
com/foodtraceability/controller/RoleController.java:108  PreAuthorize  hasAuthority('system:role:read')
com/foodtraceability/controller/RoleController.java:127  RequiresPermission  @RequiresPermission('system:role:read')
com/foodtraceability/controller/RoleController.java:128  PreAuthorize  hasAuthority('system:role:read')
com/foodtraceability/controller/RoleController.java:147  RequiresPermission  @RequiresPermission('system:role:read')
com/foodtraceability/controller/RoleController.java:148  PreAuthorize  hasAuthority('system:role:read')
com/foodtraceability/controller/RoleController.java:171  RequiresPermission  @RequiresPermission('system:role:read')
com/foodtraceability/controller/RoleController.java:172  PreAuthorize  hasAuthority('system:role:read')
com/foodtraceability/controller/RoleController.java:188  RequiresPermission  @RequiresPermission('system:role:read')
com/foodtraceability/controller/RoleController.java:189  PreAuthorize  hasAuthority('system:role:read')
com/foodtraceability/controller/RoleController.java:205  RequiresPermission  @RequiresPermission('system:role:read')
com/foodtraceability/controller/RoleController.java:206  PreAuthorize  hasAuthority('system:role:read')
com/foodtraceability/controller/RoleController.java:222  RequiresPermission  @RequiresPermission('system:role:read')
com/foodtraceability/controller/RoleController.java:223  PreAuthorize  hasAuthority('system:role:read')
com/foodtraceability/controller/RoleController.java:239  RequiresPermission  @RequiresPermission('system:role:update-status')
com/foodtraceability/controller/RoleController.java:240  PreAuthorize  hasAuthority('system:role:update-status')
com/foodtraceability/controller/RoleController.java:284  RequiresPermission  @RequiresPermission('system:role:assign-permissions')
com/foodtraceability/controller/RoleController.java:285  PreAuthorize  hasAuthority('system:role:assign-permissions')
com/foodtraceability/controller/RoleController.java:309  RequiresPermission  @RequiresPermission('system:role:read')
com/foodtraceability/controller/RoleController.java:310  PreAuthorize  hasAuthority('system:role:read')
com/foodtraceability/controller/RoleController.java:326  RequiresPermission  @RequiresPermission('system:role:set-data-scope')
com/foodtraceability/controller/RoleController.java:327  PreAuthorize  hasAuthority('system:role:set-data-scope')
com/foodtraceability/controller/RoleController.java:351  RequiresPermission  @RequiresPermission('system:role:read')
com/foodtraceability/controller/RoleController.java:352  PreAuthorize  hasAuthority('system:role:read')
com/foodtraceability/controller/RoleController.java:371  RequiresPermission  @RequiresPermission('system:role:read')
com/foodtraceability/controller/RoleController.java:372  PreAuthorize  hasAuthority('system:role:read')
com/foodtraceability/controller/RoleController.java:388  RequiresPermission  @RequiresPermission('system:role:read')
com/foodtraceability/controller/RoleController.java:389  PreAuthorize  hasAuthority('system:role:read')
com/foodtraceability/controller/ScheduledTaskController.java:54  PreAuthorize  hasAuthority('scheduler:task:query')
com/foodtraceability/controller/ScheduledTaskController.java:90  PreAuthorize  hasAuthority('scheduler:task:query')
com/foodtraceability/controller/ScheduledTaskController.java:109  PreAuthorize  hasAuthority('scheduler:task:create')
com/foodtraceability/controller/ScheduledTaskController.java:131  PreAuthorize  hasAuthority('scheduler:task:edit')
com/foodtraceability/controller/ScheduledTaskController.java:157  PreAuthorize  hasAuthority('scheduler:task:delete')
com/foodtraceability/controller/ScheduledTaskController.java:181  PreAuthorize  hasAuthority('scheduler:task:edit')
com/foodtraceability/controller/ScheduledTaskController.java:204  PreAuthorize  hasAuthority('scheduler:task:edit')
com/foodtraceability/controller/ScheduledTaskController.java:227  PreAuthorize  hasAuthority('scheduler:task:edit')
com/foodtraceability/controller/ScheduledTaskController.java:250  PreAuthorize  hasAuthority('scheduler:task:edit')
com/foodtraceability/controller/ScheduledTaskController.java:273  PreAuthorize  hasAuthority('scheduler:task:trigger')
com/foodtraceability/controller/ScheduledTaskController.java:302  PreAuthorize  hasAuthority('scheduler:task:query')
com/foodtraceability/controller/ScheduledTaskController.java:332  PreAuthorize  hasAuthority('scheduler:task:query')
com/foodtraceability/controller/ScheduledTaskController.java:357  PreAuthorize  hasAuthority('scheduler:task:query')
com/foodtraceability/controller/ScheduledTaskController.java:372  PreAuthorize  hasAuthority('scheduler:task:query')
com/foodtraceability/controller/ScheduledTaskController.java:387  PreAuthorize  hasAuthority('scheduler:task:edit')
com/foodtraceability/controller/ScheduledTaskController.java:413  PreAuthorize  hasAuthority('scheduler:task:edit')
com/foodtraceability/controller/ScheduledTaskController.java:439  PreAuthorize  hasAuthority('scheduler:task:delete')
com/foodtraceability/controller/ScheduledTaskController.java:464  PreAuthorize  hasAuthority('scheduler:task:query')
com/foodtraceability/controller/ScheduledTaskController.java:479  PreAuthorize  hasAuthority('scheduler:task:query')
com/foodtraceability/controller/ScheduledTaskController.java:498  PreAuthorize  hasAuthority('scheduler:task:query')
com/foodtraceability/controller/StoreInventoryController.java:53  PreAuthorize  hasAuthority('warehouse:inventory:view')
com/foodtraceability/controller/StoreInventoryController.java:53  PreAuthorize  hasAuthority('warehouse:overview:view')
com/foodtraceability/controller/StoreInventoryController.java:61  PreAuthorize  hasAuthority('warehouse:inventory:view')
com/foodtraceability/controller/StoreInventoryController.java:99  PreAuthorize  hasAuthority('warehouse:overview:view')
com/foodtraceability/controller/StoreInventoryController.java:99  PreAuthorize  hasAuthority('warehouse:inventory:view')
com/foodtraceability/controller/StoreInventoryController.java:115  PreAuthorize  hasAuthority('warehouse:inbound:manage')
com/foodtraceability/controller/StoreInventoryController.java:115  PreAuthorize  hasAuthority('warehouse:outbound:manage')
com/foodtraceability/controller/StoreInventoryController.java:165  PreAuthorize  hasAuthority('warehouse:inventory:view')
com/foodtraceability/controller/StoreInventoryController.java:165  PreAuthorize  hasAuthority('warehouse:overview:view')
com/foodtraceability/controller/SupplierTraceController.java:44  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/SupplierTraceController.java:53  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/SupplierTraceController.java:62  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/SupplierTraceController.java:71  PreAuthorize  hasAuthority('trace:recall')
com/foodtraceability/controller/SupplierTraceController.java:80  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/SupplierTraceController.java:89  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/SysConfigController.java:58  PreAuthorize  hasAuthority('sys:config:query')
com/foodtraceability/controller/SysConfigController.java:79  PreAuthorize  hasAuthority('sys:config:query')
com/foodtraceability/controller/SysConfigController.java:106  PreAuthorize  hasAuthority('sys:config:query')
com/foodtraceability/controller/SysConfigController.java:114  PreAuthorize  hasAuthority('sys:config:edit')
com/foodtraceability/controller/SysConfigController.java:141  PreAuthorize  hasAuthority('sys:config:edit')
com/foodtraceability/controller/SysConfigController.java:164  PreAuthorize  hasAuthority('sys:config:edit')
com/foodtraceability/controller/SysConfigController.java:202  PreAuthorize  hasAuthority('sys:config:edit')
com/foodtraceability/controller/SysConfigController.java:222  PreAuthorize  hasAuthority('sys:config:edit')
com/foodtraceability/controller/SysConfigController.java:242  PreAuthorize  hasAuthority('sys:config:query')
com/foodtraceability/controller/SysConfigController.java:295  PreAuthorize  hasAuthority('sys:config:query')
com/foodtraceability/controller/SysDictController.java:50  PreAuthorize  hasAuthority('system:dict:create')
com/foodtraceability/controller/SysDictController.java:63  PreAuthorize  hasAuthority('system:dict:update')
com/foodtraceability/controller/SysDictController.java:80  PreAuthorize  hasAuthority('system:dict:delete')
com/foodtraceability/controller/SysDictController.java:120  PreAuthorize  hasAuthority('system:dict:query')
com/foodtraceability/controller/SysDictController.java:140  PreAuthorize  hasAuthority('system:dict:query')
com/foodtraceability/controller/SysDictController.java:148  PreAuthorize  hasAuthority('system:dict:update')
com/foodtraceability/controller/SysDictController.java:162  PreAuthorize  hasAuthority('system:dict:update')
com/foodtraceability/controller/SysDictController.java:178  PreAuthorize  hasAuthority('system:dict:create')
com/foodtraceability/controller/SysDictController.java:191  PreAuthorize  hasAuthority('system:dict:update')
com/foodtraceability/controller/SysDictController.java:208  PreAuthorize  hasAuthority('system:dict:delete')
com/foodtraceability/controller/SysDictController.java:235  PreAuthorize  hasAuthority('system:dict:query')
com/foodtraceability/controller/SysDictController.java:293  PreAuthorize  hasAuthority('system:dict:query')
com/foodtraceability/controller/SysSettingController.java:29  PreAuthorize  hasAuthority('sys:config:query')
com/foodtraceability/controller/SysSettingController.java:39  PreAuthorize  hasAuthority('sys:config:query')
com/foodtraceability/controller/SysSettingController.java:49  PreAuthorize  hasAuthority('sys:config:query')
com/foodtraceability/controller/SysSettingController.java:59  PreAuthorize  hasAuthority('sys:config:edit')
com/foodtraceability/controller/SysSettingController.java:93  PreAuthorize  hasAuthority('sys:config:edit')
com/foodtraceability/controller/SysSettingController.java:103  PreAuthorize  hasAuthority('sys:config:edit')
com/foodtraceability/controller/SysSettingController.java:113  PreAuthorize  hasAuthority('sys:config:edit')
com/foodtraceability/controller/SystemConfigController.java:27  PreAuthorize  hasAuthority('system:config')
com/foodtraceability/controller/SystemConfigController.java:37  PreAuthorize  hasAuthority('system:config')
com/foodtraceability/controller/SystemConfigController.java:48  PreAuthorize  hasAuthority('system:config')
com/foodtraceability/controller/SystemConfigController.java:59  PreAuthorize  hasAuthority('system:config')
com/foodtraceability/controller/SystemConfigController.java:75  PreAuthorize  hasAuthority('system:config')
com/foodtraceability/controller/SystemConfigController.java:90  PreAuthorize  hasAuthority('system:config')
com/foodtraceability/controller/TraceCodeController.java:32  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/TraceCodeController.java:41  PreAuthorize  hasAuthority('trace:create')
com/foodtraceability/controller/TraceCodeController.java:51  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/TraceCodeController.java:60  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/TraceCodeController.java:69  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/TraceCodeController.java:78  PreAuthorize  hasAuthority('trace:recall')
com/foodtraceability/controller/TraceCodeController.java:93  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/TraceCodeController.java:102  PreAuthorize  hasAuthority('trace:recall')
com/foodtraceability/controller/TraceCodeController.java:114  PreAuthorize  hasAuthority('trace:query')
com/foodtraceability/controller/TraceCodeController.java:127  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/TraceCodeController.java:141  PreAuthorize  hasAuthority('trace:update')
com/foodtraceability/controller/UserController.java:63  RequiresPermission  @RequiresPermission('system:user:create')
com/foodtraceability/controller/UserController.java:64  PreAuthorize  hasAuthority('system:user:create')
com/foodtraceability/controller/UserController.java:83  RequiresPermission  @RequiresPermission('system:user:read')
com/foodtraceability/controller/UserController.java:84  PreAuthorize  hasAuthority('system:user:read')
com/foodtraceability/controller/UserController.java:105  RequiresPermission  @RequiresPermission('system:user:read')
com/foodtraceability/controller/UserController.java:106  PreAuthorize  hasAuthority('system:user:read')
com/foodtraceability/controller/UserController.java:117  RequiresPermission  @RequiresPermission('system:user:update')
com/foodtraceability/controller/UserController.java:118  PreAuthorize  hasAuthority('system:user:update')
com/foodtraceability/controller/UserController.java:156  RequiresPermission  @RequiresPermission('system:user:reset-password')
com/foodtraceability/controller/UserController.java:157  PreAuthorize  hasAuthority('system:user:reset-password')
com/foodtraceability/controller/UserController.java:170  RequiresPermission  @RequiresPermission('system:user:update-status')
com/foodtraceability/controller/UserController.java:171  PreAuthorize  hasAuthority('system:user:update-status')
com/foodtraceability/controller/UserController.java:222  RequiresPermission  @RequiresPermission('system:user:read')
com/foodtraceability/controller/UserController.java:223  PreAuthorize  hasAuthority('system:user:read')
com/foodtraceability/controller/UserController.java:234  RequiresPermission  @RequiresPermission('system:user:read')
com/foodtraceability/controller/UserController.java:235  PreAuthorize  hasAuthority('system:user:read')
com/foodtraceability/controller/UserController.java:243  RequiresPermission  @RequiresPermission('system:user:read')
com/foodtraceability/controller/UserController.java:244  PreAuthorize  hasAuthority('system:user:read')
com/foodtraceability/controller/UserController.java:289  RequiresPermission  @RequiresPermission('system:user:assign-roles')
com/foodtraceability/controller/UserController.java:290  PreAuthorize  hasAuthority('system:user:assign-roles')
com/foodtraceability/controller/UserController.java:303  RequiresPermission  @RequiresPermission('system:user:unassign-roles')
com/foodtraceability/controller/UserController.java:304  PreAuthorize  hasAuthority('system:user:unassign-roles')
com/foodtraceability/controller/UserController.java:315  RequiresPermission  @RequiresPermission('system:user:read')
com/foodtraceability/controller/UserController.java:316  PreAuthorize  hasAuthority('system:user:read')
com/foodtraceability/controller/UserController.java:324  RequiresPermission  @RequiresPermission('system:user:read')
com/foodtraceability/controller/UserController.java:325  PreAuthorize  hasAuthority('system:user:read')
com/foodtraceability/controller/UserController.java:333  RequiresPermission  @RequiresPermission('system:user:read')
com/foodtraceability/controller/UserController.java:334  PreAuthorize  hasAuthority('system:user:read')
com/foodtraceability/controller/UserController.java:343  RequiresPermission  @RequiresPermission('system:user:read')
com/foodtraceability/controller/UserController.java:344  PreAuthorize  hasAuthority('system:user:read')
com/foodtraceability/controller/UserController.java:353  RequiresPermission  @RequiresPermission('system:user:read')
com/foodtraceability/controller/UserController.java:354  PreAuthorize  hasAuthority('system:user:read')
com/foodtraceability/controller/UserController.java:362  RequiresPermission  @RequiresPermission('system:user:read')
com/foodtraceability/controller/UserController.java:363  PreAuthorize  hasAuthority('system:user:read')
com/foodtraceability/controller/UserPermissionOverrideController.java:44  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/UserPermissionOverrideController.java:44  PreAuthorize  hasAuthority('system:permission:read')
com/foodtraceability/controller/UserPermissionOverrideController.java:71  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/UserPermissionOverrideController.java:71  PreAuthorize  hasAuthority('system:permission:read')
com/foodtraceability/controller/UserPermissionOverrideController.java:91  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/UserPermissionOverrideController.java:91  PreAuthorize  hasAuthority('system:permission:read')
com/foodtraceability/controller/UserPermissionOverrideController.java:108  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/UserPermissionOverrideController.java:134  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/UserPermissionOverrideController.java:155  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/UserPermissionOverrideController.java:175  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/UserPermissionOverrideController.java:200  PreAuthorize  hasAuthority('system:permission:manage')
com/foodtraceability/controller/WarehouseController.java:38  PreAuthorize  hasAuthority('warehouse:create')
com/foodtraceability/controller/WarehouseController.java:53  PreAuthorize  hasAuthority('warehouse:update')
com/foodtraceability/controller/WarehouseController.java:126  PreAuthorize  hasAuthority('warehouse:update')
com/foodtraceability/controller/WarehouseController.java:143  PreAuthorize  hasAuthority('warehouse:delete')
com/foodtraceability/controller/admin/ReportInsightRuleController.java:27  PreAuthorize  hasAuthority('finance:report:config')
com/foodtraceability/controller/device/DeviceAlertController.java:46  PreAuthorize  hasAuthority('device:alert:view')
com/foodtraceability/controller/device/DeviceAlertController.java:57  PreAuthorize  hasAuthority('device:alert:manage')
com/foodtraceability/controller/device/DeviceAlertController.java:65  PreAuthorize  hasAuthority('device:alert:manage')
com/foodtraceability/controller/device/DeviceAlertController.java:74  PreAuthorize  hasAuthority('device:alert:manage')
com/foodtraceability/controller/device/DeviceAlertController.java:85  PreAuthorize  hasAuthority('device:alert:view')
com/foodtraceability/controller/device/DeviceAlertController.java:112  PreAuthorize  hasAuthority('device:alert:view')
com/foodtraceability/controller/device/DeviceAlertController.java:121  PreAuthorize  hasAuthority('device:alert:view')
com/foodtraceability/controller/device/DeviceAlertController.java:131  PreAuthorize  hasAuthority('device:alert:manage')
com/foodtraceability/controller/device/DeviceAlertController.java:142  PreAuthorize  hasAuthority('device:alert:manage')
com/foodtraceability/controller/device/DeviceAlertController.java:160  PreAuthorize  hasAuthority('device:alert:view')
com/foodtraceability/controller/device/DeviceAlertController.java:170  PreAuthorize  hasAuthority('device:alert:manage')
com/foodtraceability/controller/device/DeviceAlertController.java:178  PreAuthorize  hasAuthority('device:alert:view')
com/foodtraceability/controller/device/DeviceAlertController.java:186  PreAuthorize  hasAuthority('device:alert:view')
com/foodtraceability/controller/device/DeviceAlertController.java:196  PreAuthorize  hasAuthority('device:alert:manage')
com/foodtraceability/controller/device/DeviceAlertController.java:206  PreAuthorize  hasAuthority('device:alert:manage')
com/foodtraceability/controller/device/DeviceController.java:40  PreAuthorize  hasAuthority('device:manage:manage')
com/foodtraceability/controller/device/DeviceController.java:48  PreAuthorize  hasAuthority('device:manage:manage')
com/foodtraceability/controller/device/DeviceController.java:58  PreAuthorize  hasAuthority('device:manage:manage')
com/foodtraceability/controller/device/DeviceController.java:67  PreAuthorize  hasAuthority('device:manage:view')
com/foodtraceability/controller/device/DeviceController.java:76  PreAuthorize  hasAuthority('device:manage:view')
com/foodtraceability/controller/device/DeviceController.java:101  PreAuthorize  hasAuthority('device:manage:view')
com/foodtraceability/controller/device/DeviceController.java:117  PreAuthorize  hasAuthority('device:manage:view')
com/foodtraceability/controller/device/DeviceController.java:125  PreAuthorize  hasAuthority('device:manage:view')
com/foodtraceability/controller/device/DeviceController.java:134  PreAuthorize  hasAuthority('device:manage:manage')
com/foodtraceability/controller/device/DeviceController.java:145  PreAuthorize  hasAuthority('device:manage:manage')
com/foodtraceability/controller/device/DeviceController.java:154  PreAuthorize  hasAuthority('device:manage:manage')
com/foodtraceability/controller/device/DeviceController.java:164  PreAuthorize  hasAuthority('device:manage:manage')
com/foodtraceability/controller/device/PrintTaskController.java:42  PreAuthorize  hasAuthority('device:print-task:manage')
com/foodtraceability/controller/device/PrintTaskController.java:50  PreAuthorize  hasAuthority('device:print-task:manage')
com/foodtraceability/controller/device/PrintTaskController.java:58  PreAuthorize  hasAuthority('device:print-task:view')
com/foodtraceability/controller/device/PrintTaskController.java:67  PreAuthorize  hasAuthority('device:print-task:view')
com/foodtraceability/controller/device/PrintTaskController.java:92  PreAuthorize  hasAuthority('device:print-task:view')
com/foodtraceability/controller/device/PrintTaskController.java:101  PreAuthorize  hasAuthority('device:print-task:manage')
com/foodtraceability/controller/device/PrintTaskController.java:110  PreAuthorize  hasAuthority('device:print-task:manage')
com/foodtraceability/controller/device/PrintTaskController.java:119  PreAuthorize  hasAuthority('device:print-task:manage')
com/foodtraceability/controller/device/PrintTaskController.java:130  PreAuthorize  hasAuthority('device:print-task:manage')
com/foodtraceability/controller/device/PrintTaskController.java:139  PreAuthorize  hasAuthority('device:print-task:manage')
com/foodtraceability/controller/device/PrintTaskController.java:148  PreAuthorize  hasAuthority('device:print-task:manage')
com/foodtraceability/controller/device/PrintTemplateController.java:41  PreAuthorize  hasAuthority('device:print-template:manage')
com/foodtraceability/controller/device/PrintTemplateController.java:49  PreAuthorize  hasAuthority('device:print-template:manage')
com/foodtraceability/controller/device/PrintTemplateController.java:59  PreAuthorize  hasAuthority('device:print-template:manage')
com/foodtraceability/controller/device/PrintTemplateController.java:68  PreAuthorize  hasAuthority('device:print-template:view')
com/foodtraceability/controller/device/PrintTemplateController.java:77  PreAuthorize  hasAuthority('device:print-template:view')
com/foodtraceability/controller/device/PrintTemplateController.java:90  PreAuthorize  hasAuthority('device:print-template:view')
com/foodtraceability/controller/device/PrintTemplateController.java:99  PreAuthorize  hasAuthority('device:print-template:view')
com/foodtraceability/controller/device/PrintTemplateController.java:108  PreAuthorize  hasAuthority('device:print-template:manage')
com/foodtraceability/controller/device/PrintTemplateController.java:117  PreAuthorize  hasAuthority('device:print-template:manage')
com/foodtraceability/controller/finance/AccountingPeriodController.java:31  PreAuthorize  hasAuthority('finance:period:create')
com/foodtraceability/controller/finance/AccountingPeriodController.java:38  PreAuthorize  hasAuthority('finance:period:query')
com/foodtraceability/controller/finance/AccountingPeriodController.java:45  PreAuthorize  hasAuthority('finance:period:query')
com/foodtraceability/controller/finance/AccountingPeriodController.java:56  PreAuthorize  hasAuthority('finance:period:update')
com/foodtraceability/controller/finance/AccountingPeriodController.java:63  PreAuthorize  hasAuthority('finance:period:update')
com/foodtraceability/controller/finance/AccountingPeriodController.java:70  PreAuthorize  hasAuthority('finance:period:query')
com/foodtraceability/controller/finance/AccountingPeriodController.java:77  PreAuthorize  hasAuthority('finance:period:query')
com/foodtraceability/controller/finance/AccountingPeriodController.java:84  PreAuthorize  hasAuthority('finance:period:approve')
com/foodtraceability/controller/finance/AccountingPeriodController.java:91  PreAuthorize  hasAuthority('finance:period:query')
com/foodtraceability/controller/finance/ApprovalFlowConfigController.java:30  PreAuthorize  hasAuthority('finance:approval-flow-config:manage')
com/foodtraceability/controller/finance/ApprovalFlowConfigController.java:37  PreAuthorize  hasAuthority('finance:approval-flow-config:manage')
com/foodtraceability/controller/finance/ApprovalFlowConfigController.java:44  PreAuthorize  hasAuthority('finance:approval-flow-config:manage')
com/foodtraceability/controller/finance/ApprovalFlowConfigController.java:51  PreAuthorize  hasAuthority('finance:approval-flow-config:view')
com/foodtraceability/controller/finance/ApprovalFlowConfigController.java:58  PreAuthorize  hasAuthority('finance:approval-flow-config:view')
com/foodtraceability/controller/finance/ApprovalFlowConfigController.java:65  PreAuthorize  hasAuthority('finance:approval-flow-config:view')
com/foodtraceability/controller/finance/ApprovalFlowConfigController.java:72  PreAuthorize  hasAuthority('finance:approval-flow-config:manage')
com/foodtraceability/controller/finance/AutoVoucherController.java:76  PreAuthorize  hasAuthority('finance:auto-voucher:view')
com/foodtraceability/controller/finance/AutoVoucherController.java:86  PreAuthorize  hasAuthority('finance:auto-voucher:create')
com/foodtraceability/controller/finance/AutoVoucherController.java:97  PreAuthorize  hasAuthority('finance:auto-voucher:view')
com/foodtraceability/controller/finance/AutoVoucherController.java:111  PreAuthorize  hasAuthority('finance:auto-voucher:update')
com/foodtraceability/controller/finance/AutoVoucherController.java:124  PreAuthorize  hasAuthority('finance:auto-voucher:delete')
com/foodtraceability/controller/finance/AutoVoucherController.java:136  PreAuthorize  hasAuthority('finance:auto-voucher:update')
com/foodtraceability/controller/finance/AutoVoucherController.java:152  PreAuthorize  hasAuthority('finance:auto-voucher:view')
com/foodtraceability/controller/finance/AutoVoucherController.java:168  PreAuthorize  hasAuthority('finance:auto-voucher:view')
com/foodtraceability/controller/finance/AutoVoucherController.java:179  PreAuthorize  hasAuthority('finance:auto-voucher:update')
com/foodtraceability/controller/finance/AutoVoucherController.java:190  PreAuthorize  hasAuthority('finance:auto-voucher:create')
com/foodtraceability/controller/finance/AutoVoucherController.java:201  PreAuthorize  hasAuthority('finance:auto-voucher:export')
com/foodtraceability/controller/finance/AutoVoucherController.java:218  PreAuthorize  hasAuthority('finance:auto-voucher:view')
com/foodtraceability/controller/finance/AutoVoucherController.java:230  PreAuthorize  hasAuthority('finance:auto-voucher:update')
com/foodtraceability/controller/finance/AutoVoucherController.java:242  PreAuthorize  hasAuthority('finance:auto-voucher:update')
com/foodtraceability/controller/finance/AutoVoucherController.java:254  PreAuthorize  hasAuthority('finance:auto-voucher:delete')
com/foodtraceability/controller/finance/BankAccountController.java:35  PreAuthorize  hasAuthority('finance:bank:manage')
com/foodtraceability/controller/finance/BankAccountController.java:35  PreAuthorize  hasAuthority('finance:bank:create')
com/foodtraceability/controller/finance/BankAccountController.java:42  PreAuthorize  hasAuthority('finance:bank:manage')
com/foodtraceability/controller/finance/BankAccountController.java:42  PreAuthorize  hasAuthority('finance:bank:update')
com/foodtraceability/controller/finance/BankAccountController.java:49  PreAuthorize  hasAuthority('finance:bank:manage')
com/foodtraceability/controller/finance/BankAccountController.java:49  PreAuthorize  hasAuthority('finance:bank:update')
com/foodtraceability/controller/finance/BankAccountController.java:56  PreAuthorize  hasAuthority('finance:bank:manage')
com/foodtraceability/controller/finance/BankAccountController.java:56  PreAuthorize  hasAuthority('finance:bank:delete')
com/foodtraceability/controller/finance/BankAccountController.java:63  PreAuthorize  hasAuthority('finance:bank:view')
com/foodtraceability/controller/finance/BankAccountController.java:63  PreAuthorize  hasAuthority('finance:bank:query')
com/foodtraceability/controller/finance/BankAccountController.java:70  PreAuthorize  hasAuthority('finance:bank:view')
com/foodtraceability/controller/finance/BankAccountController.java:70  PreAuthorize  hasAuthority('finance:bank:query')
com/foodtraceability/controller/finance/BankAccountController.java:77  PreAuthorize  hasAuthority('finance:bank:manage')
com/foodtraceability/controller/finance/BankAccountController.java:77  PreAuthorize  hasAuthority('finance:bank:reconcile')
com/foodtraceability/controller/finance/BankAccountController.java:86  PreAuthorize  hasAuthority('finance:bank:view')
com/foodtraceability/controller/finance/BankAccountController.java:86  PreAuthorize  hasAuthority('finance:bank:query')
com/foodtraceability/controller/finance/BudgetController.java:30  PreAuthorize  hasAuthority('finance:budget:create')
com/foodtraceability/controller/finance/BudgetController.java:37  PreAuthorize  hasAuthority('finance:budget:update')
com/foodtraceability/controller/finance/BudgetController.java:45  PreAuthorize  hasAuthority('finance:budget:query')
com/foodtraceability/controller/finance/BudgetController.java:52  PreAuthorize  hasAuthority('finance:budget:query')
com/foodtraceability/controller/finance/BudgetController.java:59  PreAuthorize  hasAuthority('finance:budget:update')
com/foodtraceability/controller/finance/FinanceApprovalController.java:59  PreAuthorize  hasAuthority('finance:approval:view')
com/foodtraceability/controller/finance/FinanceApprovalController.java:85  PreAuthorize  hasAuthority('finance:approval:view')
com/foodtraceability/controller/finance/FinanceApprovalController.java:100  PreAuthorize  hasAuthority('finance:approval:view')
com/foodtraceability/controller/finance/FinanceApprovalController.java:118  PreAuthorize  hasAuthority('finance:approval:approve')
com/foodtraceability/controller/finance/FinanceApprovalController.java:141  PreAuthorize  hasAuthority('finance:approval:approve')
com/foodtraceability/controller/finance/FinanceAuditLogController.java:29  PreAuthorize  hasAuthority('finance:audit-log:view')
com/foodtraceability/controller/finance/FinanceAuditLogController.java:36  PreAuthorize  hasAuthority('finance:audit-log:view')
com/foodtraceability/controller/finance/FinanceStatisticsController.java:46  PreAuthorize  hasAuthority('finance:statistics:view')
com/foodtraceability/controller/finance/FinanceWarningController.java:54  PreAuthorize  hasAuthority('finance:warning:view')
com/foodtraceability/controller/finance/FinanceWarningController.java:68  PreAuthorize  hasAuthority('finance:warning:view')
com/foodtraceability/controller/finance/FinanceWarningController.java:85  PreAuthorize  hasAuthority('finance:warning:view')
com/foodtraceability/controller/finance/FinanceWarningController.java:100  PreAuthorize  hasAuthority('finance:warning:create')
com/foodtraceability/controller/finance/FinanceWarningController.java:114  PreAuthorize  hasAuthority('finance:warning:process')
com/foodtraceability/controller/finance/FinanceWarningController.java:130  PreAuthorize  hasAuthority('finance:warning:delete')
com/foodtraceability/controller/finance/InvoiceReimbursementController.java:64  PreAuthorize  hasAuthority('finance:reimbursement:create')
com/foodtraceability/controller/finance/InvoiceReimbursementController.java:86  PreAuthorize  hasAuthority('finance:reimbursement:view')
com/foodtraceability/controller/finance/InvoiceReimbursementController.java:99  PreAuthorize  hasAuthority('finance:reimbursement:view')
com/foodtraceability/controller/finance/InvoiceReimbursementController.java:115  PreAuthorize  hasAuthority('finance:reimbursement:create')
com/foodtraceability/controller/finance/InvoiceReimbursementController.java:138  PreAuthorize  hasAuthority('finance:reimbursement:approve')
com/foodtraceability/controller/finance/InvoiceReimbursementController.java:159  PreAuthorize  hasAuthority('finance:reimbursement:approve')
com/foodtraceability/controller/finance/InvoiceReimbursementController.java:179  PreAuthorize  hasAuthority('finance:reimbursement:pay')
com/foodtraceability/controller/finance/InvoiceReimbursementController.java:198  PreAuthorize  hasAuthority('finance:reimbursement:view')
com/foodtraceability/controller/finance/InvoiceReimbursementController.java:212  PreAuthorize  hasAuthority('finance:reimbursement:view')
com/foodtraceability/controller/finance/InvoiceVerifyController.java:28  PreAuthorize  hasAuthority('finance:invoice:verify')
com/foodtraceability/controller/finance/InvoiceVerifyController.java:52  PreAuthorize  hasAuthority('finance:invoice:verify')
com/foodtraceability/controller/finance/ReceiptController.java:32  PreAuthorize  hasAuthority('finance:receivable:approve')
com/foodtraceability/controller/finance/ReceiptController.java:40  PreAuthorize  hasAuthority('finance:receivable:query')
com/foodtraceability/controller/finance/ReceiptController.java:48  PreAuthorize  hasAuthority('finance:receivable:query')
com/foodtraceability/controller/finance/ReceiptController.java:56  PreAuthorize  hasAuthority('finance:receipt:edit')
com/foodtraceability/controller/finance/ReceiptController.java:64  PreAuthorize  hasAuthority('finance:receipt:delete')
com/foodtraceability/controller/finance/ReceivableController.java:30  PreAuthorize  hasAuthority('finance:receivable:create')
com/foodtraceability/controller/finance/ReceivableController.java:37  PreAuthorize  hasAuthority('finance:receivable:update')
com/foodtraceability/controller/finance/ReceivableController.java:45  PreAuthorize  hasAuthority('finance:receivable:delete')
com/foodtraceability/controller/finance/ReceivableController.java:52  PreAuthorize  hasAuthority('finance:receivable:query')
com/foodtraceability/controller/finance/ReceivableController.java:59  PreAuthorize  hasAuthority('finance:receivable:query')
com/foodtraceability/controller/finance/ReceivableController.java:66  PreAuthorize  hasAuthority('finance:receivable:approve')
com/foodtraceability/controller/finance/ReceivableController.java:73  PreAuthorize  hasAuthority('finance:receivable:approve')
com/foodtraceability/controller/finance/RecordController.java:30  PreAuthorize  hasAuthority('finance:record:create')
com/foodtraceability/controller/finance/RecordController.java:37  PreAuthorize  hasAuthority('finance:record:update')
com/foodtraceability/controller/finance/RecordController.java:45  PreAuthorize  hasAuthority('finance:record:view')
com/foodtraceability/controller/finance/RecordController.java:52  PreAuthorize  hasAuthority('finance:record:view')
com/foodtraceability/controller/finance/RecordController.java:59  PreAuthorize  hasAuthority('finance:record:approve')
com/foodtraceability/controller/finance/ReportController.java:32  PreAuthorize  hasAuthority('finance:report:view')
com/foodtraceability/controller/finance/ReportController.java:41  PreAuthorize  hasAuthority('finance:report:view')
com/foodtraceability/controller/finance/ReportController.java:50  PreAuthorize  hasAuthority('finance:report:view')
com/foodtraceability/controller/finance/ReportController.java:57  PreAuthorize  hasAuthority('finance:report:view')
com/foodtraceability/controller/finance/ReportController.java:64  PreAuthorize  hasAuthority('finance:report:view')
com/foodtraceability/controller/finance/ReportController.java:71  PreAuthorize  hasAuthority('finance:report:view')
com/foodtraceability/controller/finance/ReportController.java:78  PreAuthorize  hasAuthority('finance:report:view')
com/foodtraceability/controller/finance/TaxCalculationController.java:47  PreAuthorize  hasAuthority('finance:tax:view')
com/foodtraceability/controller/finance/TaxCalculationController.java:74  PreAuthorize  hasAuthority('finance:tax:view')
com/foodtraceability/controller/finance/TaxCalculationController.java:90  PreAuthorize  hasAuthority('finance:tax:view')
com/foodtraceability/controller/finance/TaxCalculationController.java:104  PreAuthorize  hasAuthority('finance:tax:create')
com/foodtraceability/controller/finance/TaxCalculationController.java:130  PreAuthorize  hasAuthority('finance:tax:create')
com/foodtraceability/controller/finance/TaxCalculationController.java:144  PreAuthorize  hasAuthority('finance:tax:view')
com/foodtraceability/controller/finance/TaxRecordController.java:45  PreAuthorize  hasAuthority('finance:tax:view')
com/foodtraceability/controller/finance/TaxRecordController.java:63  PreAuthorize  hasAuthority('finance:tax:view')
com/foodtraceability/controller/finance/TaxRecordController.java:78  PreAuthorize  hasAuthority('finance:tax:create')
com/foodtraceability/controller/finance/TaxRecordController.java:94  PreAuthorize  hasAuthority('finance:tax:update')
com/foodtraceability/controller/finance/TaxRecordController.java:108  PreAuthorize  hasAuthority('finance:tax:delete')
com/foodtraceability/controller/finance/TaxRecordController.java:122  PreAuthorize  hasAuthority('finance:tax:delete')
com/foodtraceability/controller/finance/VoucherController.java:30  PreAuthorize  hasAuthority('finance:voucher:create')
com/foodtraceability/controller/finance/VoucherController.java:37  PreAuthorize  hasAuthority('finance:voucher:update')
com/foodtraceability/controller/finance/VoucherController.java:45  PreAuthorize  hasAuthority('finance:voucher:query')
com/foodtraceability/controller/finance/VoucherController.java:52  PreAuthorize  hasAuthority('finance:voucher:query')
com/foodtraceability/controller/finance/VoucherController.java:59  PreAuthorize  hasAuthority('finance:voucher:approve')
com/foodtraceability/controller/finance/VoucherController.java:66  PreAuthorize  hasAuthority('finance:voucher:approve')
com/foodtraceability/controller/finance/VoucherController.java:73  PreAuthorize  hasAuthority('finance:voucher:approve')
com/foodtraceability/controller/finance/VoucherController.java:80  PreAuthorize  hasAuthority('finance:voucher:approve')
com/foodtraceability/controller/finance/VoucherController.java:87  PreAuthorize  hasAuthority('finance:voucher:approve')
com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:37  PreAuthorize  hasAuthority('device:communication:manage')
com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:54  PreAuthorize  hasAuthority('device:communication:manage')
com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:71  PreAuthorize  hasAuthority('device:communication:manage')
com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:88  PreAuthorize  hasAuthority('device:communication:manage')
com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:105  PreAuthorize  hasAuthority('device:communication:manage')
com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:122  PreAuthorize  hasAuthority('device:communication:manage')
com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:139  PreAuthorize  hasAuthority('device:communication:manage')
com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:156  PreAuthorize  hasAuthority('device:communication:manage')
com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:173  PreAuthorize  hasAuthority('device:communication:manage')
com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:190  PreAuthorize  hasAuthority('device:communication:manage')
com/foodtraceability/controller/hardware/DeviceCommunicationTestController.java:207  PreAuthorize  hasAuthority('device:communication:view')
com/foodtraceability/controller/hardware/DeviceSimulatorController.java:48  PreAuthorize  hasAuthority('device:simulator:manage')
com/foodtraceability/controller/hardware/DeviceSimulatorController.java:71  PreAuthorize  hasAuthority('device:simulator:manage')
com/foodtraceability/controller/hardware/DeviceSimulatorController.java:93  PreAuthorize  hasAuthority('device:simulator:view')
com/foodtraceability/controller/hardware/DeviceSimulatorController.java:112  PreAuthorize  hasAuthority('device:simulator:manage')
com/foodtraceability/controller/hardware/DeviceSimulatorController.java:136  PreAuthorize  hasAuthority('device:simulator:manage')
com/foodtraceability/controller/hardware/DeviceSimulatorController.java:160  PreAuthorize  hasAuthority('device:simulator:manage')
com/foodtraceability/controller/hardware/DeviceSimulatorController.java:184  PreAuthorize  hasAuthority('device:simulator:manage')
com/foodtraceability/controller/hardware/EpsonPrinterTestController.java:62  PreAuthorize  hasAuthority('hardware:epson-test:view')
com/foodtraceability/controller/hardware/EpsonPrinterTestController.java:105  PreAuthorize  hasAuthority('hardware:epson-test:view')
com/foodtraceability/controller/hardware/EpsonPrinterTestController.java:222  PreAuthorize  hasAuthority('hardware:epson-test:manage')
com/foodtraceability/controller/hardware/EpsonPrinterTestController.java:307  PreAuthorize  hasAuthority('hardware:epson-test:manage')
com/foodtraceability/controller/hardware/EpsonPrinterTestController.java:325  PreAuthorize  hasAuthority('hardware:epson-test:manage')
com/foodtraceability/controller/hardware/EpsonPrinterTestController.java:352  PreAuthorize  hasAuthority('hardware:epson-test:manage')
com/foodtraceability/controller/hardware/EpsonPrinterTestController.java:381  PreAuthorize  hasAuthority('hardware:epson-test:manage')
com/foodtraceability/controller/hardware/EpsonPrinterTestController.java:530  PreAuthorize  hasAuthority('hardware:epson-test:manage')
com/foodtraceability/controller/hardware/HardwareConfigController.java:57  PreAuthorize  hasAuthority('hardware:config:view')
com/foodtraceability/controller/hardware/HardwareConfigController.java:67  PreAuthorize  hasAuthority('hardware:config:view')
com/foodtraceability/controller/hardware/HardwareConfigController.java:75  PreAuthorize  hasAuthority('hardware:config:manage')
com/foodtraceability/controller/hardware/HardwareConfigController.java:83  PreAuthorize  hasAuthority('hardware:config:manage')
com/foodtraceability/controller/hardware/HardwareConfigController.java:92  PreAuthorize  hasAuthority('hardware:config:manage')
com/foodtraceability/controller/hardware/HardwareConfigController.java:100  PreAuthorize  hasAuthority('hardware:config:view')
com/foodtraceability/controller/hardware/HardwareConfigController.java:111  PreAuthorize  hasAuthority('hardware:config:manage')
com/foodtraceability/controller/hardware/HardwareConfigController.java:124  PreAuthorize  hasAuthority('hardware:config:manage')
com/foodtraceability/controller/hardware/HardwareConfigController.java:139  PreAuthorize  hasAuthority('hardware:config:manage')
com/foodtraceability/controller/hardware/HardwareConfigController.java:151  PreAuthorize  hasAuthority('hardware:config:manage')
com/foodtraceability/controller/hardware/HardwareConfigController.java:164  PreAuthorize  hasAuthority('hardware:config:view')
com/foodtraceability/controller/hardware/HardwareConfigController.java:174  PreAuthorize  hasAuthority('hardware:config:manage')
com/foodtraceability/controller/hardware/HardwareConfigController.java:181  PreAuthorize  hasAuthority('hardware:config:manage')
com/foodtraceability/controller/hardware/HardwareConfigController.java:188  PreAuthorize  hasAuthority('hardware:config:view')
com/foodtraceability/controller/hardware/HardwareConfigVersionController.java:45  PreAuthorize  hasAuthority('hardware:view')
com/foodtraceability/controller/hardware/HardwareConfigVersionController.java:57  PreAuthorize  hasAuthority('hardware:view')
com/foodtraceability/controller/hardware/HardwareConfigVersionController.java:70  PreAuthorize  hasAuthority('hardware:edit')
com/foodtraceability/controller/hardware/HardwareConfigVersionController.java:82  PreAuthorize  hasAuthority('hardware:delete')
com/foodtraceability/controller/hardware/HardwareDeviceController.java:44  PreAuthorize  hasAuthority('hardware:scan')
com/foodtraceability/controller/hardware/HardwareDeviceController.java:51  PreAuthorize  hasAuthority('hardware:print')
com/foodtraceability/controller/hardware/HardwareDeviceController.java:61  PreAuthorize  hasAuthority('hardware:capture')
com/foodtraceability/controller/hardware/HardwareDeviceController.java:71  PreAuthorize  hasAuthority('hardware:device:manage')
com/foodtraceability/controller/hardware/HardwareDeviceController.java:78  PreAuthorize  hasAuthority('hardware:device:manage')
com/foodtraceability/controller/hardware/HardwareDeviceController.java:85  PreAuthorize  hasAuthority('hardware:device:view')
com/foodtraceability/controller/hardware/HardwareInitController.java:42  PreAuthorize  hasAuthority('hardware:init:manage')
com/foodtraceability/controller/hardware/LabelPrinterController.java:69  PreAuthorize  hasAuthority('hardware:label-printer:view')
com/foodtraceability/controller/hardware/LabelPrinterController.java:164  PreAuthorize  hasAuthority('hardware:label-printer:manage')
com/foodtraceability/controller/hardware/LabelPrinterController.java:211  PreAuthorize  hasAuthority('hardware:label-printer:manage')
com/foodtraceability/controller/hardware/LabelPrinterController.java:248  PreAuthorize  hasAuthority('hardware:label-printer:manage')
com/foodtraceability/controller/hardware/LabelPrinterController.java:288  PreAuthorize  hasAuthority('hardware:label-printer:manage')
com/foodtraceability/controller/hardware/LabelPrinterController.java:635  PreAuthorize  hasAuthority('hardware:label-printer:manage')
com/foodtraceability/controller/hardware/LabelPrinterController.java:671  PreAuthorize  hasAuthority('hardware:label-printer:view')
com/foodtraceability/controller/hardware/LabelPrinterController.java:687  PreAuthorize  hasAuthority('hardware:label-printer:manage')
com/foodtraceability/controller/hardware/LabelPrinterController.java:720  PreAuthorize  hasAuthority('hardware:label-printer:manage')
com/foodtraceability/controller/hardware/LabelPrinterController.java:743  PreAuthorize  hasAuthority('hardware:label-printer:manage')
com/foodtraceability/controller/hardware/PrintFormatTemplateController.java:46  PreAuthorize  hasAuthority('hardware:print-format:view')
com/foodtraceability/controller/hardware/PrintFormatTemplateController.java:64  PreAuthorize  hasAuthority('hardware:print-format:view')
com/foodtraceability/controller/hardware/PrintFormatTemplateController.java:80  PreAuthorize  hasAuthority('hardware:print-format:view')
com/foodtraceability/controller/hardware/PrintFormatTemplateController.java:94  PreAuthorize  hasAuthority('hardware:print-format:manage')
com/foodtraceability/controller/hardware/PrintFormatTemplateController.java:109  PreAuthorize  hasAuthority('hardware:print-format:manage')
com/foodtraceability/controller/hardware/PrintFormatTemplateController.java:125  PreAuthorize  hasAuthority('hardware:print-format:manage')
com/foodtraceability/controller/hardware/PrintFormatTemplateController.java:142  PreAuthorize  hasAuthority('hardware:print-format:manage')
com/foodtraceability/controller/hardware/PrintFormatTemplateController.java:160  PreAuthorize  hasAuthority('hardware:print-format:manage')
com/foodtraceability/controller/hardware/ScanDeviceController.java:43  PreAuthorize  hasAuthority('device:scan:manage')
com/foodtraceability/controller/hardware/ScanDeviceController.java:55  PreAuthorize  hasAuthority('device:scan:manage')
com/foodtraceability/controller/hardware/ScanDeviceController.java:67  PreAuthorize  hasAuthority('device:scan:view')
com/foodtraceability/controller/hardware/ScanDeviceController.java:84  PreAuthorize  hasAuthority('device:scan:manage')
com/foodtraceability/controller/hardware/ScanDeviceController.java:102  PreAuthorize  hasAuthority('device:scan:manage')
com/foodtraceability/controller/hardware/ScanDeviceController.java:113  PreAuthorize  hasAuthority('device:scan:view')
com/foodtraceability/controller/hardware/ScanDeviceController.java:125  PreAuthorize  hasAuthority('device:scan:manage')
com/foodtraceability/controller/hardware/ScanDeviceController.java:137  PreAuthorize  hasAuthority('device:scan:manage')
com/foodtraceability/controller/hardware/SdkPrinterController.java:35  PreAuthorize  hasAuthority('device:sdk-printer:manage')
com/foodtraceability/controller/hardware/SdkPrinterController.java:48  PreAuthorize  hasAuthority('device:sdk-printer:view')
com/foodtraceability/controller/hardware/TTSController.java:45  PreAuthorize  hasAuthority('device:tts:manage')
com/foodtraceability/controller/hardware/TTSController.java:101  PreAuthorize  hasAuthority('device:tts:manage')
com/foodtraceability/controller/hardware/TTSController.java:109  PreAuthorize  hasAuthority('device:tts:manage')
com/foodtraceability/controller/hardware/TTSController.java:117  PreAuthorize  hasAuthority('device:tts:manage')
com/foodtraceability/controller/hardware/TTSController.java:125  PreAuthorize  hasAuthority('device:tts:manage')
com/foodtraceability/controller/hardware/TTSController.java:169  PreAuthorize  hasAuthority('device:tts:view')
com/foodtraceability/controller/hardware/TakeoutLockerController.java:42  PreAuthorize  hasAuthority('device:locker:manage')
com/foodtraceability/controller/hardware/TakeoutLockerController.java:54  PreAuthorize  hasAuthority('device:locker:manage')
com/foodtraceability/controller/hardware/TakeoutLockerController.java:66  PreAuthorize  hasAuthority('device:locker:view')
com/foodtraceability/controller/hardware/TakeoutLockerController.java:82  PreAuthorize  hasAuthority('device:locker:manage')
com/foodtraceability/controller/hardware/TakeoutLockerController.java:101  PreAuthorize  hasAuthority('device:locker:manage')
com/foodtraceability/controller/hardware/TakeoutLockerController.java:116  PreAuthorize  hasAuthority('device:locker:manage')
com/foodtraceability/controller/hardware/TakeoutLockerController.java:128  PreAuthorize  hasAuthority('device:locker:view')
com/foodtraceability/controller/hardware/TakeoutLockerController.java:140  PreAuthorize  hasAuthority('device:locker:view')
com/foodtraceability/controller/hardware/TakeoutLockerController.java:153  PreAuthorize  hasAuthority('device:locker:manage')
com/foodtraceability/controller/hardware/TakeoutLockerController.java:166  PreAuthorize  hasAuthority('device:locker:manage')
com/foodtraceability/controller/hardware/TakeoutLockerController.java:178  PreAuthorize  hasAuthority('device:locker:manage')
com/foodtraceability/controller/hardware/WeighingDeviceController.java:42  PreAuthorize  hasAuthority('device:weighing:manage')
com/foodtraceability/controller/hardware/WeighingDeviceController.java:54  PreAuthorize  hasAuthority('device:weighing:manage')
com/foodtraceability/controller/hardware/WeighingDeviceController.java:66  PreAuthorize  hasAuthority('device:weighing:view')
com/foodtraceability/controller/hardware/WeighingDeviceController.java:78  PreAuthorize  hasAuthority('device:weighing:manage')
com/foodtraceability/controller/hardware/WeighingDeviceController.java:90  PreAuthorize  hasAuthority('device:weighing:manage')
com/foodtraceability/controller/hardware/WeighingDeviceController.java:101  PreAuthorize  hasAuthority('device:weighing:view')
com/foodtraceability/controller/hardware/WeighingDeviceController.java:114  PreAuthorize  hasAuthority('device:weighing:manage')
com/foodtraceability/controller/integration/FileAttachmentController.java:132  PreAuthorize  hasAuthority('file:query')
com/foodtraceability/controller/maintenance/ApiStatsController.java:48  PreAuthorize  hasAuthority('system:stats:view')
com/foodtraceability/controller/maintenance/ApiStatsController.java:104  PreAuthorize  hasAuthority('system:stats:view')
com/foodtraceability/controller/maintenance/ApiStatsController.java:140  PreAuthorize  hasAuthority('system:stats:view')
com/foodtraceability/controller/maintenance/ApiStatsController.java:170  PreAuthorize  hasAuthority('system:stats:view')
com/foodtraceability/controller/maintenance/ApiStatsController.java:191  PreAuthorize  hasAuthority('system:stats:view')
com/foodtraceability/controller/maintenance/DataBackupController.java:62  PreAuthorize  hasAuthority('backup:create')
com/foodtraceability/controller/maintenance/DataBackupController.java:90  PreAuthorize  hasAuthority('backup:query')
com/foodtraceability/controller/maintenance/DataBackupController.java:139  PreAuthorize  hasAuthority('backup:query')
com/foodtraceability/controller/maintenance/DataBackupController.java:161  PreAuthorize  hasAuthority('backup:delete')
com/foodtraceability/controller/maintenance/DataBackupController.java:188  PreAuthorize  hasAuthority('backup:download')
com/foodtraceability/controller/maintenance/DataBackupController.java:222  PreAuthorize  hasAuthority('backup:restore')
com/foodtraceability/controller/maintenance/DataBackupController.java:253  PreAuthorize  hasAuthority('backup:query')
com/foodtraceability/controller/maintenance/DataBackupController.java:289  PreAuthorize  hasAuthority('backup:restore')
com/foodtraceability/controller/maintenance/DataBackupController.java:317  PreAuthorize  hasAuthority('backup:query')
com/foodtraceability/controller/maintenance/DataBackupController.java:337  PreAuthorize  hasAuthority('backup:query')
com/foodtraceability/controller/maintenance/DataBackupController.java:355  PreAuthorize  hasAuthority('backup:query')
com/foodtraceability/controller/maintenance/DataBackupController.java:373  PreAuthorize  hasAuthority('backup:config')
com/foodtraceability/controller/maintenance/DataExportController.java:36  PreAuthorize  hasAuthority('admin:user:export')
com/foodtraceability/controller/maintenance/DatabaseCleanupController.java:28  RequiresPermission  @RequiresPermission('system:database:delete')
com/foodtraceability/controller/maintenance/DatabaseCleanupController.java:29  PreAuthorize  hasAuthority('system:database:delete')
com/foodtraceability/controller/maintenance/DatabaseCleanupController.java:54  RequiresPermission  @RequiresPermission('system:database:delete')
com/foodtraceability/controller/maintenance/DatabaseCleanupController.java:55  PreAuthorize  hasAuthority('system:database:delete')
com/foodtraceability/controller/maintenance/SystemDataController.java:27  RequiresPermission  @RequiresPermission('system:database:delete')
com/foodtraceability/controller/maintenance/SystemDataController.java:28  PreAuthorize  hasAuthority('system:database:delete')
com/foodtraceability/controller/marketing/AnomalyAlertController.java:50  PreAuthorize  hasAuthority('member:alert:view')
com/foodtraceability/controller/marketing/AnomalyAlertController.java:72  PreAuthorize  hasAuthority('member:alert:manage')
com/foodtraceability/controller/marketing/AnomalyAlertController.java:92  PreAuthorize  hasAuthority('member:alert:manage')
com/foodtraceability/controller/marketing/MemberLevelController.java:35  PreAuthorize  hasAuthority('member:level:view')
com/foodtraceability/controller/marketing/MemberLevelController.java:35  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/marketing/MemberLevelController.java:59  PreAuthorize  hasAuthority('member:level:view')
com/foodtraceability/controller/marketing/MemberLevelController.java:59  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/marketing/MemberLevelController.java:73  PreAuthorize  hasAuthority('member:level:manage')
com/foodtraceability/controller/marketing/MemberLevelController.java:73  PreAuthorize  hasAuthority('member:manage')
com/foodtraceability/controller/marketing/MemberLevelController.java:96  PreAuthorize  hasAuthority('member:level:manage')
com/foodtraceability/controller/marketing/MemberLevelController.java:96  PreAuthorize  hasAuthority('member:manage')
com/foodtraceability/controller/marketing/MemberLevelController.java:112  PreAuthorize  hasAuthority('member:level:manage')
com/foodtraceability/controller/marketing/MemberLevelController.java:112  PreAuthorize  hasAuthority('member:manage')
com/foodtraceability/controller/marketing/MemberLevelController.java:134  PreAuthorize  hasAuthority('member:level:manage')
com/foodtraceability/controller/marketing/MemberLevelController.java:134  PreAuthorize  hasAuthority('member:manage')
com/foodtraceability/controller/marketing/RechargeFinanceController.java:42  PreAuthorize  hasAuthority('recharge:view')
com/foodtraceability/controller/marketing/RechargeFinanceController.java:68  PreAuthorize  hasAuthority('recharge:view')
com/foodtraceability/controller/marketing/RechargePlanController.java:54  PreAuthorize  hasAuthority('recharge-plan:view')
com/foodtraceability/controller/marketing/RechargePlanController.java:76  PreAuthorize  hasAuthority('recharge-plan:view')
com/foodtraceability/controller/marketing/RechargePlanController.java:95  PreAuthorize  hasAuthority('recharge-plan:manage')
com/foodtraceability/controller/marketing/RechargePlanController.java:112  PreAuthorize  hasAuthority('recharge-plan:manage')
com/foodtraceability/controller/marketing/RechargePlanController.java:131  PreAuthorize  hasAuthority('recharge-plan:manage')
com/foodtraceability/controller/marketing/RechargePlanController.java:149  PreAuthorize  hasAuthority('recharge-plan:manage')
com/foodtraceability/controller/marketing/RechargeRecordController.java:56  PreAuthorize  hasAuthority('member:recharge:manage')
com/foodtraceability/controller/marketing/RechargeRecordController.java:74  PreAuthorize  hasAuthority('member:recharge:export')
com/foodtraceability/controller/marketing/RechargeRecordController.java:106  PreAuthorize  hasAuthority('member:recharge:view')
com/foodtraceability/controller/marketing/RechargeRecordController.java:142  PreAuthorize  hasAuthority('member:recharge:view')
com/foodtraceability/controller/marketing/RechargeRecordController.java:163  PreAuthorize  hasAuthority('member:recharge:manage')
com/foodtraceability/controller/marketing/RechargeRecordController.java:183  PreAuthorize  hasAuthority('member:recharge:manage')
com/foodtraceability/controller/marketing/RechargeSettingsController.java:39  PreAuthorize  hasAuthority('recharge-settings:view')
com/foodtraceability/controller/marketing/RechargeSettingsController.java:54  PreAuthorize  hasAuthority('recharge-settings:manage')
com/foodtraceability/controller/marketing/RechargeStatsController.java:37  PreAuthorize  hasAuthority('recharge:view')
com/foodtraceability/controller/marketing/RefundController.java:51  PreAuthorize  hasAuthority('refund:view')
com/foodtraceability/controller/marketing/RefundController.java:73  PreAuthorize  hasAuthority('refund:manage')
com/foodtraceability/controller/marketing/RefundController.java:90  PreAuthorize  hasAuthority('refund:manage')
com/foodtraceability/controller/marketing/RefundController.java:109  PreAuthorize  hasAuthority('refund:manage')
com/foodtraceability/controller/operations/ReportConfigController.java:71  PreAuthorize  hasAuthority('finance:report:config')
com/foodtraceability/controller/operations/ReportConfigController.java:92  PreAuthorize  hasAuthority('finance:report:config')
com/foodtraceability/controller/operations/ReportConfigController.java:126  PreAuthorize  hasAuthority('finance:report:config')
com/foodtraceability/controller/pos/CallNumberController.java:61  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/pos/CallNumberController.java:72  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/pos/CallNumberController.java:83  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/pos/CallNumberController.java:94  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/pos/CallNumberController.java:102  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/pos/CallNumberController.java:113  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/pos/CallNumberController.java:123  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/pos/CallNumberController.java:130  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/pos/MemberController.java:51  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/pos/MemberController.java:58  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/pos/MemberController.java:69  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/pos/MemberController.java:80  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/pos/MemberController.java:91  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/pos/MemberController.java:102  PreAuthorize  hasAuthority('member:export')
com/foodtraceability/controller/pos/MemberController.java:102  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/pos/MemberController.java:118  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/pos/MemberController.java:134  PreAuthorize  hasAuthority('member:view')
com/foodtraceability/controller/pos/MemberController.java:146  PreAuthorize  hasAuthority('member:manage')
com/foodtraceability/controller/pos/MemberController.java:157  PreAuthorize  hasAuthority('member:manage')
com/foodtraceability/controller/pos/MemberController.java:168  PreAuthorize  hasAuthority('member:manage')
com/foodtraceability/controller/pos/MemberController.java:179  PreAuthorize  hasAuthority('member:manage')
com/foodtraceability/controller/pos/MemberController.java:190  PreAuthorize  hasAuthority('member:manage')
com/foodtraceability/controller/pos/PosController.java:41  PreAuthorize  hasAuthority('order:create')
com/foodtraceability/controller/pos/PosController.java:54  PreAuthorize  hasAuthority('order:pay')
com/foodtraceability/controller/pos/PosController.java:68  PreAuthorize  hasAuthority('order:refund')
com/foodtraceability/controller/pos/PosController.java:103  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/pos/PosController.java:115  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/pos/PosController.java:123  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/pos/PosController.java:132  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/pos/PosController.java:141  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/pos/PosController.java:150  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/pos/PosController.java:158  PreAuthorize  hasAuthority('order:queue')
com/foodtraceability/controller/purchase/MaterialCategoryController.java:52  PreAuthorize  hasAuthority('purchase:category:view')
com/foodtraceability/controller/purchase/MaterialCategoryController.java:78  PreAuthorize  hasAuthority('purchase:category:view')
com/foodtraceability/controller/purchase/MaterialCategoryController.java:94  PreAuthorize  hasAuthority('purchase:category:view')
com/foodtraceability/controller/purchase/MaterialCategoryController.java:111  PreAuthorize  hasAuthority('purchase:category:view')
com/foodtraceability/controller/purchase/MaterialCategoryController.java:132  PreAuthorize  hasAuthority('purchase:category:create')
com/foodtraceability/controller/purchase/MaterialCategoryController.java:152  PreAuthorize  hasAuthority('purchase:category:edit')
com/foodtraceability/controller/purchase/MaterialCategoryController.java:173  PreAuthorize  hasAuthority('purchase:category:delete')
com/foodtraceability/controller/purchase/MaterialCategoryController.java:194  PreAuthorize  hasAuthority('purchase:category:edit')
com/foodtraceability/controller/purchase/ProcurementTraceController.java:43  PreAuthorize  hasAuthority('purchase:trace:view')
com/foodtraceability/controller/purchase/ProcurementTraceController.java:60  PreAuthorize  hasAuthority('purchase:trace:view')
com/foodtraceability/controller/purchase/ProcurementTraceController.java:80  PreAuthorize  hasAuthority('purchase:trace:view')
com/foodtraceability/controller/purchase/ProcurementTraceController.java:97  PreAuthorize  hasAuthority('purchase:trace:view')
com/foodtraceability/controller/purchase/ProcurementTraceController.java:117  PreAuthorize  hasAuthority('purchase:trace:view')
com/foodtraceability/controller/purchase/ProcurementTraceController.java:134  PreAuthorize  hasAuthority('purchase:trace:view')
com/foodtraceability/controller/purchase/PurchaseArchiveController.java:52  PreAuthorize  hasAuthority('purchase:archive:view')
com/foodtraceability/controller/purchase/PurchaseArchiveController.java:86  PreAuthorize  hasAuthority('purchase:archive:view')
com/foodtraceability/controller/purchase/PurchaseArchiveController.java:108  PreAuthorize  hasAuthority('purchase:archive:create')
com/foodtraceability/controller/purchase/PurchaseArchiveController.java:127  PreAuthorize  hasAuthority('purchase:archive:edit')
com/foodtraceability/controller/purchase/PurchaseArchiveController.java:146  PreAuthorize  hasAuthority('purchase:archive:delete')
com/foodtraceability/controller/purchase/PurchaseArchiveController.java:166  PreAuthorize  hasAuthority('purchase:archive:edit')
com/foodtraceability/controller/schedule/ScheduleAttendanceSyncController.java:49  PreAuthorize  hasAuthority('schedule:entry:edit')
com/foodtraceability/controller/schedule/ScheduleAttendanceSyncController.java:66  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleAttendanceSyncController.java:83  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleAttendanceSyncController.java:104  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleCalendarController.java:55  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleCalendarController.java:77  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleCalendarController.java:101  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleConflictController.java:49  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleConflictController.java:72  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleConflictController.java:88  PreAuthorize  hasAuthority('schedule:plan:edit')
com/foodtraceability/controller/schedule/ScheduleConflictController.java:104  PreAuthorize  hasAuthority('schedule:plan:edit')
com/foodtraceability/controller/schedule/ScheduleEngineController.java:52  PreAuthorize  hasAuthority('schedule:plan:create')
com/foodtraceability/controller/schedule/ScheduleEngineController.java:69  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleEngineController.java:87  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleEngineController.java:104  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleExportController.java:56  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleExportController.java:69  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleExportController.java:84  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleExportController.java:103  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleNotificationController.java:54  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleNotificationController.java:85  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleNotificationController.java:101  PreAuthorize  hasAuthority('schedule:plan:edit')
com/foodtraceability/controller/schedule/ScheduleNotificationController.java:118  PreAuthorize  hasAuthority('schedule:plan:edit')
com/foodtraceability/controller/schedule/SchedulePlanController.java:44  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/SchedulePlanController.java:57  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/SchedulePlanController.java:74  PreAuthorize  hasAuthority('schedule:plan:create')
com/foodtraceability/controller/schedule/SchedulePlanController.java:93  PreAuthorize  hasAuthority('schedule:plan:edit')
com/foodtraceability/controller/schedule/SchedulePlanController.java:112  PreAuthorize  hasAuthority('schedule:plan:delete')
com/foodtraceability/controller/schedule/SchedulePlanController.java:130  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/SchedulePlanController.java:143  PreAuthorize  hasAuthority('schedule:plan:edit')
com/foodtraceability/controller/schedule/SchedulePlanController.java:161  PreAuthorize  hasAuthority('schedule:plan:edit')
com/foodtraceability/controller/schedule/SchedulePlanController.java:193  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/SchedulePlanController.java:212  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/SchedulePlanController.java:242  PreAuthorize  hasAuthority('schedule:entry:edit')
com/foodtraceability/controller/schedule/ScheduleShiftTypeController.java:38  PreAuthorize  hasAuthority('schedule:shift:configure')
com/foodtraceability/controller/schedule/ScheduleShiftTypeController.java:50  PreAuthorize  hasAuthority('schedule:shift:configure')
com/foodtraceability/controller/schedule/ScheduleShiftTypeController.java:62  PreAuthorize  hasAuthority('schedule:shift:configure')
com/foodtraceability/controller/schedule/ScheduleShiftTypeController.java:77  PreAuthorize  hasAuthority('schedule:shift:configure')
com/foodtraceability/controller/schedule/ScheduleShiftTypeController.java:89  PreAuthorize  hasAuthority('schedule:shift:configure')
com/foodtraceability/controller/schedule/ScheduleShiftTypeController.java:106  PreAuthorize  hasAuthority('schedule:shift:configure')
com/foodtraceability/controller/schedule/ScheduleShiftTypeController.java:122  PreAuthorize  hasAuthority('schedule:shift:configure')
com/foodtraceability/controller/schedule/ScheduleSwapController.java:59  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleSwapController.java:86  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleSwapController.java:103  PreAuthorize  hasAuthority('schedule:view:self')
com/foodtraceability/controller/schedule/ScheduleSwapController.java:122  PreAuthorize  hasAuthority('schedule:entry:edit')
com/foodtraceability/controller/schedule/ScheduleSwapController.java:137  PreAuthorize  hasAuthority('schedule:plan:edit')
com/foodtraceability/controller/schedule/ScheduleSwapController.java:155  PreAuthorize  hasAuthority('schedule:plan:edit')
com/foodtraceability/controller/schedule/ScheduleSwapController.java:176  PreAuthorize  hasAuthority('schedule:entry:edit')
com/foodtraceability/controller/schedule/ScheduleTemplateController.java:38  PreAuthorize  hasAuthority('schedule:template:manage')
com/foodtraceability/controller/schedule/ScheduleTemplateController.java:51  PreAuthorize  hasAuthority('schedule:template:manage')
com/foodtraceability/controller/schedule/ScheduleTemplateController.java:67  PreAuthorize  hasAuthority('schedule:template:manage')
com/foodtraceability/controller/schedule/ScheduleTemplateController.java:80  PreAuthorize  hasAuthority('schedule:template:manage')
com/foodtraceability/controller/schedule/ScheduleTemplateController.java:99  PreAuthorize  hasAuthority('schedule:template:manage')
com/foodtraceability/controller/schedule/ScheduleTemplateController.java:117  PreAuthorize  hasAuthority('schedule:template:manage')
com/foodtraceability/controller/schedule/ScheduleTemplateController.java:135  PreAuthorize  hasAuthority('schedule:template:manage')
com/foodtraceability/controller/schedule/ScheduleTemplateController.java:152  PreAuthorize  hasAuthority('schedule:template:manage')
com/foodtraceability/controller/seal/SealController.java:53  PreAuthorize  hasAuthority('seal:view')
com/foodtraceability/controller/seal/SealController.java:83  PreAuthorize  hasAuthority('seal:view')
com/foodtraceability/controller/seal/SealController.java:103  PreAuthorize  hasAuthority('seal:view')
com/foodtraceability/controller/seal/SealController.java:125  PreAuthorize  hasAuthority('seal:manage')
com/foodtraceability/controller/seal/SealController.java:152  PreAuthorize  hasAuthority('seal:manage')
com/foodtraceability/controller/seal/SealController.java:171  PreAuthorize  hasAuthority('seal:view')
com/foodtraceability/controller/seal/SealController.java:187  PreAuthorize  hasAuthority('seal:manage')
com/foodtraceability/controller/seal/SealController.java:206  PreAuthorize  hasAuthority('seal:delete')
com/foodtraceability/controller/seal/SealController.java:223  PreAuthorize  hasAuthority('seal:manage')
com/foodtraceability/controller/supplierportal/SupplierPortalController.java:162  PreAuthorize  hasAuthority('purchase:supplier:manage')
com/foodtraceability/controller/supplierportal/SupplierPortalController.java:182  PreAuthorize  hasAuthority('purchase:supplier:manage')
com/foodtraceability/controller/supplierportal/SupplierPortalController.java:203  PreAuthorize  hasAuthority('purchase:supplier:manage')
com/foodtraceability/controller/supplierportal/SupplierPortalController.java:223  PreAuthorize  hasAuthority('purchase:supplier:manage')
com/foodtraceability/controller/supplierportal/SupplierPortalController.java:242  PreAuthorize  hasAuthority('purchase:supplier:manage')
```
</details>

## 5. C 类：完全未定义（角色与权限均无，需设计决策）

| 字面量 | 次数 | 近邻建议 | 涉及文件 |
|--------|------|----------|----------|
| `ROLE_CASHIER` | 6 | — | PosApiController.java, PosCouponController.java, PosOrderController.java, PosOrderPaymentServiceImpl.java, YoloProductRecognitionController.java |
| `ROLE_POS_OPERATOR` | 6 | — | PosApiController.java, PosCouponController.java, PosOrderController.java, PosOrderPaymentServiceImpl.java, YoloProductRecognitionController.java |
| `ROLE_CHEF` | 5 | — | KitchenOrderController.java |
| `ROLE_KITCHEN` | 5 | — | KitchenOrderController.java |

<details><summary>逐条位置（22 条）</summary>

```
com/foodtraceability/controller/kitchen/KitchenOrderController.java:436  PreAuthorize  hasAnyAuthority('ROLE_CHEF')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/kitchen/KitchenOrderController.java:436  PreAuthorize  hasAnyAuthority('ROLE_KITCHEN')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/kitchen/KitchenOrderController.java:453  PreAuthorize  hasAnyAuthority('ROLE_CHEF')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/kitchen/KitchenOrderController.java:453  PreAuthorize  hasAnyAuthority('ROLE_KITCHEN')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/kitchen/KitchenOrderController.java:469  PreAuthorize  hasAnyAuthority('ROLE_CHEF')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/kitchen/KitchenOrderController.java:469  PreAuthorize  hasAnyAuthority('ROLE_KITCHEN')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/kitchen/KitchenOrderController.java:482  PreAuthorize  hasAnyAuthority('ROLE_CHEF')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/kitchen/KitchenOrderController.java:482  PreAuthorize  hasAnyAuthority('ROLE_KITCHEN')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/kitchen/KitchenOrderController.java:495  PreAuthorize  hasAnyAuthority('ROLE_CHEF')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/kitchen/KitchenOrderController.java:495  PreAuthorize  hasAnyAuthority('ROLE_KITCHEN')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/pos/PosApiController.java:26  PreAuthorize  hasAnyAuthority('ROLE_CASHIER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/pos/PosApiController.java:26  PreAuthorize  hasAnyAuthority('ROLE_POS_OPERATOR')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/pos/PosCouponController.java:29  PreAuthorize  hasAnyAuthority('ROLE_CASHIER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/pos/PosCouponController.java:29  PreAuthorize  hasAnyAuthority('ROLE_POS_OPERATOR')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/pos/PosOrderController.java:29  PreAuthorize  hasAnyAuthority('ROLE_CASHIER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/pos/PosOrderController.java:29  PreAuthorize  hasAnyAuthority('ROLE_POS_OPERATOR')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/pos/YoloProductRecognitionController.java:24  PreAuthorize  hasAnyAuthority('ROLE_CASHIER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/controller/pos/YoloProductRecognitionController.java:24  PreAuthorize  hasAnyAuthority('ROLE_POS_OPERATOR')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:104  PreAuthorize  hasAnyAuthority('ROLE_CASHIER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:104  PreAuthorize  hasAnyAuthority('ROLE_POS_OPERATOR')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:699  PreAuthorize  hasAnyAuthority('ROLE_CASHIER')  [ROLE_IN_AUTHORITY_POSITION]
com/foodtraceability/service/impl/PosOrderPaymentServiceImpl.java:699  PreAuthorize  hasAnyAuthority('ROLE_POS_OPERATOR')  [ROLE_IN_AUTHORITY_POSITION]
```
</details>

## 6. 有效字面量 TOP 20（确认扫描不是在空转）

| 字面量 | 次数 |
|--------|------|
| `admin` | 284 |
| `hr_director` | 203 |
| `store_manager` | 111 |
| `ADMIN` | 91 |
| `purchase_manager` | 63 |
| `inventory:query` | 57 |
| `OWNER` | 51 |
| `order:view` | 44 |
| `order:manage` | 43 |
| `finance:view` | 27 |
| `kitchen:edit` | 26 |
| `finance:edit` | 24 |
| `finance_director` | 24 |
| `asset:view` | 23 |
| `hr:health-certificate:manage` | 22 |
| `asset:manage` | 20 |
| `STORE_MANAGER` | 19 |
| `warehouse_manager` | 18 |
| `ROLE_ADMIN` | 16 |
| `hr:health-certificate:view` | 15 |
