# 部署缺口分级清单（库=food_traceability_test）

合计 85 处：真活 51 ｜ 半活 9 ｜ 死代码 25

| 表名 | 实体 | Mapper | Service | Controller入口 | 分级 | 建议 |
|------|------|--------|---------|----------------|------|------|
| `app_device_registrations` | AppDeviceRegistration | AppDeviceRegistrationMapper | AppDeviceRegistrationServiceImpl | AppDeviceRegistrationController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `budget` | Budget | BudgetMapper | BudgetDataServiceImpl, BudgetServiceImpl | BudgetController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `category_operation_log` | CategoryOperationLog | CategoryOperationLogMapper | CategoryOperationLogServiceImpl | CategoryOperationLogController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `contract_document` | ContractDocument | ContractDocumentMapper | ContractDocumentServiceImpl | EmployeeLaborContractController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `contract_template` | ContractTemplate | ContractTemplateMapper | ContractTemplateServiceImpl | ContractTemplateController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `coupon` | Coupon | CouponMapper | CouponServiceImpl, CouponTemplateServiceImpl | CouponController, PosCouponController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `dish_ingredient` | DishIngredient | DishIngredientMapper | FoodTraceabilityServiceImpl | TraceCodeController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `file_attachment` | FileAttachment | FileAttachmentMapper | FileAttachmentServiceImpl, FileChunkServiceImpl | FileAttachmentController, FileChunkController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `finance_audit_logs` | FinanceAuditLog | FinanceAuditLogMapper | FinanceAuditLogServiceImpl | FinanceAuditLogController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `finance_invoices` | FinanceInvoice | FinanceInvoiceMapper | InvoiceServiceImpl | InvoiceController, ManualInvoiceController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `finance_voucher_details` | FinanceVoucherDetail | FinanceVoucherDetailMapper | AccountingPeriodServiceImpl, VoucherServiceImpl | AccountingPeriodController, VoucherController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `hardware_config` | HardwareConfig | HardwareConfigMapper | DeviceHeartbeatServiceImpl, DeviceStatusServiceImpl | DeviceManagementController, HardwareConfigController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `hardware_config_version` | HardwareConfigVersion | HardwareConfigVersionMapper | HardwareConfigVersionServiceImpl | HardwareConfigVersionController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `inventory_category` | InventoryCategory | InventoryCategoryMapper | InventoryCategoryServiceImpl | InventoryCategoryController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `inventory_code` | InventoryCode | InventoryCodeMapper | HardwareDeviceServiceImpl, SalesOrderServiceImpl | DeviceManagementController, HardwareConfigController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `inventory_unit` | InventoryUnit | InventoryUnitMapper | InventoryUnitServiceImpl | InventoryUnitController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `inventory_warning_rules` | InventoryWarningRule | InventoryWarningRuleMapper | InventoryWarningRuleServiceImpl | InventoryWarningRuleController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `invoice_reimbursement` | InvoiceReimbursement | InvoiceReimbursementMapper | FinanceApprovalServiceImpl, InvoiceReimbursementServiceImpl | FinanceApprovalController, InvoiceReimbursementController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `invoice_reimbursement_approval_record` | InvoiceReimbursementApprovalRecord | InvoiceReimbursementApprovalRecordMapper | InvoiceReimbursementServiceImpl | FinanceApprovalController, InvoiceReimbursementController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `invoice_reimbursement_item` | InvoiceReimbursementItem | InvoiceReimbursementItemMapper | InvoiceReimbursementServiceImpl | FinanceApprovalController, InvoiceReimbursementController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `locker_slot` | LockerSlot | LockerSlotMapper | TakeoutLockerServiceImpl | TakeoutLockerController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `material_template` | MaterialTemplate | MaterialTemplateMapper | MaterialTemplateServiceImpl, OCRServiceImpl | MaterialTemplateController, OCRController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `material_usage_record` | MaterialUsageRecord | MaterialUsageRecordMapper | InventoryDeductionServiceImpl, KitchenScanController | InventoryDeductionController, KitchenScanController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `operation_logs` | OperationLog | OperationLogMapper | CategoryOperationLogMapper, CategoryOperationLogServiceImpl | CategoryOperationLogController, HealthCertificateController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `over_age_worker` | OverAgeWorker | OverAgeWorkerMapper | OverAgeWorkerServiceImpl | OverAgeWorkerController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `plan_item` | PlanItem | PlanItemMapper | PlanItemServiceImpl, PurchasePlanItemMapper | PlanItemController, PurchasePlanController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `print_format_template` | PrintFormatTemplate | PrintFormatTemplateMapper | PrintFormatTemplateServiceImpl | PrintFormatTemplateController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `profit_statements` | ProfitStatement | ProfitStatementMapper | FinancialReportServiceImpl | ProfitController, ReportController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `receipt_print_log` | ReceiptPrintLog | ReceiptPrintLogMapper | ReceiptConfirmationServiceImpl | ReceiptConfirmationController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `receivable` | Receivable | ReceivableMapper | FinanceStatisticsServiceImpl, FinancialReportServiceImpl | FinanceStatisticsController, ProfitController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `receivables` | Receivable | ReceivableMapper | FinanceStatisticsServiceImpl, FinancialReportServiceImpl | FinanceStatisticsController, ProfitController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `role_departments` | RoleDepartment | RoleDepartmentMapper | DataPermissionServiceImpl, SystemInitServiceImpl | CompanyInitController, SystemInitController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `role_stores` | RoleStore | RoleStoreMapper | DataPermissionServiceImpl, SystemInitServiceImpl | CompanyInitController, SystemInitController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `salary_adjustment` | SalaryAdjustment | SalaryAdjustmentMapper | SalaryAdjustmentServiceImpl | SalaryController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `salary_record` | SalaryRecord | SalaryRecordMapper | SalaryRecordServiceImpl | SalaryBatchController, SalaryController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `salary_rule` | SalaryRule | SalaryRuleMapper | SalaryRuleServiceImpl | SalaryController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `sales_order` | SalesOrder | SalesOrderMapper | DashboardServiceImpl, SalesOrderServiceImpl | DashboardController, OperationsDashboardController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `scan_device` | ScanDevice | ScanDeviceMapper | ScanDeviceServiceImpl | ScanDeviceController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `scan_record` | ScanRecord | ScanRecordMapper | ScanDeviceServiceImpl, TraceScanRecordMapper | ScanDeviceController, TrayController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `self_purchase` | SelfPurchase | SelfPurchaseMapper | SelfPurchaseServiceImpl | SelfPurchaseController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `self_purchase_item` | SelfPurchaseItem | SelfPurchaseItemMapper | SelfPurchaseServiceImpl | SelfPurchaseController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `standard_cost_cards` | StandardCostCard | StandardCostCardMapper | StandardCostCardServiceImpl | StandardCostCardController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `summary_templates` | SummaryTemplate | SummaryTemplateMapper | SummaryTemplateServiceImpl | SummaryTemplateController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `supplier_evaluations` | SupplierEvaluation | SupplierEvaluationMapper | SupplierEvaluationServiceImpl | SupplierEvaluationController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `takeout_locker` | TakeoutLocker | TakeoutLockerMapper | TakeoutLockerServiceImpl | TakeoutLockerController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `tax_rate_configs` | TaxRateConfig | TaxRateConfigMapper | TaxRateConfigServiceImpl | TaxRateConfigController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `trace_code_log` | TraceCodeLog | TraceCodeLogMapper | TraceCodeServiceImpl | FoodTraceCodeController, MaterialTraceCodeController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `transfer_templates` | TransferTemplate | TransferTemplateMapper | TransferTemplateServiceImpl | TransferTemplateController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `v_inventory_summary` | InventorySummary | InventorySummaryMapper | InventorySummaryServiceImpl | StoreInventoryController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `weighing_device` | WeighingDevice | WeighingDeviceMapper | WeighingDeviceServiceImpl | WeighingDeviceController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `weighing_record` | WeighingRecord | WeighingRecordMapper | WeighingDeviceServiceImpl | WeighingDeviceController | 真活 | 补迁移（先贴生产库 \d 结构，逐列照抄） |
| `data_change_history` | DataChangeHistory | DataChangeHistoryMapper | DataChangeHistoryServiceImpl | - | 半活 | 补迁移或确认弃用（Service 无 Controller 入口、无调度） |
| `department_permissions` | DepartmentPermission | DepartmentPermissionMapper | DepartmentPermissionServiceImpl | - | 半活 | 补迁移或确认弃用（Service 无 Controller 入口、无调度） |
| `finance_account` | FinanceAccount | FinanceAccountMapper | FinanceAccountServiceImpl | - | 半活 | 补迁移或确认弃用（Service 无 Controller 入口、无调度） |
| `finance_approval` | FinanceApproval | FinanceApprovalMapper | FinanceApprovalServiceImpl | - | 半活 | 补迁移或确认弃用（Service 无 Controller 入口、无调度） |
| `finance_report_detail` | FinanceReportDetail | FinanceReportDetailMapper | FinanceReportDetailServiceImpl | - | 半活 | 补迁移或确认弃用（Service 无 Controller 入口、无调度） |
| `hardware_operation_log` | HardwareOperationLog | HardwareOperationLogMapper | HardwareOperationLogServiceImpl | - | 半活 | 补迁移或确认弃用（Service 无 Controller 入口、无调度） |
| `inventory_movement` | InventoryMovement | InventoryMovementMapper | InventoryMovementServiceImpl | - | 半活 | 补迁移或确认弃用（Service 无 Controller 入口、无调度） |
| `registration_code_log` | RegistrationCodeLog | RegistrationCodeLogMapper | RegistrationCodeServiceImpl | - | 半活 | 补迁移或确认弃用（Service 无 Controller 入口、无调度） |
| `user_sessions` | UserSession | UserSessionMapper | UserSessionServiceImpl | - | 半活 | 补迁移或确认弃用（Service 无 Controller 入口、无调度） |
| `cost_allocation` | CostAllocation | CostAllocationMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `cost_allocation_detail` | CostAllocationDetail | CostAllocationDetailMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `cost_allocation_rule` | CostAllocationRule | CostAllocationRuleMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `cost_allocation_rules` | CostAllocationRule | CostAllocationRuleMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `data_link_event` | DataLinkEvent | DataLinkEventMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `finance_record_audit_log` | FinanceRecordAuditLog | - | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `finance_record_operation_log` | FinanceRecordOperationLog | FinanceRecordOperationLogMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `finance_report` | FinanceReport | FinanceReportMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `food_trace` | FoodTrace | FoodTraceMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `inventory_transfer_detail` | InventoryTransferDetail | - | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `invitation_expense_record` | InvitationExpenseRecord | InvitationExpenseRecordMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `invitation_reminder_log` | InvitationReminderLog | InvitationReminderLogMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `ocr_record` | OcrRecord | OcrRecordMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `operation_type` | OperationType | OperationTypeMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `requisition_requests` | RequisitionRequestEntity | - | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `sales_order_detail` | SalesOrderDetail | - | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `swap_request_details` | SwapRequestDetailEntity | - | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `sys_devices` | SysDevice | SysDeviceMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `sys_permissions` | SysPermission | SysPermissionMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `sys_print_templates` | SysPrintTemplate | SysPrintTemplateMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `sys_roles` | SysRole | SysRoleMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `sys_stores` | SysStore | SysStoreMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `trace_stage` | TraceStage | TraceStageMapper | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `traceability_code` | TraceabilityCode | - | - | - | 死代码 | 确认不建表；进死代码删除清单 |
| `travel_requests` | TravelRequestEntity | - | - | - | 死代码 | 确认不建表；进死代码删除清单 |
