# 部署缺口审计（库=food_traceability_test）

表不存在：活引用 61 个实体，死代码 24 个

| 表 | 实体 | 活引用处（前 3） |
|----|------|------------------|
| `app_device_registrations` | AppDeviceRegistration | AppDeviceRegistrationServiceImpl.java（共 1 处） |
| `budget` | Budget | BudgetCheckScheduler.java, BudgetServiceImpl.java, BudgetDataServiceImpl.java（共 3 处） |
| `category_operation_log` | CategoryOperationLog | CategoryOperationLogServiceImpl.java（共 1 处） |
| `contract_document` | ContractDocument | ContractDocumentServiceImpl.java（共 1 处） |
| `contract_template` | ContractTemplate | ContractTemplateServiceImpl.java（共 1 处） |
| `coupon` | Coupon | MemberCouponMapper.java, CouponServiceImpl.java, CouponTemplateServiceImpl.java（共 3 处） |
| `data_change_history` | DataChangeHistory | DataChangeHistoryServiceImpl.java（共 1 处） |
| `department_permissions` | DepartmentPermission | DepartmentPermissionServiceImpl.java（共 1 处） |
| `dish_ingredient` | DishIngredient | FoodTraceabilityServiceImpl.java（共 1 处） |
| `file_attachment` | FileAttachment | FileCleanupScheduler.java, FileAttachmentServiceImpl.java, FileChunkServiceImpl.java（共 3 处） |
| `finance_account` | FinanceAccount | FinanceAccountServiceImpl.java（共 1 处） |
| `finance_approval` | FinanceApproval | FinanceApprovalServiceImpl.java（共 1 处） |
| `finance_audit_logs` | FinanceAuditLog | FinanceAuditLogServiceImpl.java（共 1 处） |
| `finance_invoices` | FinanceInvoice | InvoiceServiceImpl.java（共 1 处） |
| `finance_record_operation_log` | FinanceRecordOperationLog | FinanceRecordOperationAspect.java（共 1 处） |
| `finance_report_detail` | FinanceReportDetail | FinanceReportDetailServiceImpl.java（共 1 处） |
| `finance_voucher_details` | FinanceVoucherDetail | AccountingPeriodServiceImpl.java, VoucherServiceImpl.java（共 2 处） |
| `hardware_config` | HardwareConfig | DeviceHeartbeatServiceImpl.java, DeviceStatusServiceImpl.java, HardwareConfigServiceImpl.java（共 4 处） |
| `hardware_config_version` | HardwareConfigVersion | HardwareConfigVersionServiceImpl.java（共 1 处） |
| `hardware_operation_log` | HardwareOperationLog | HardwareOperationLogServiceImpl.java（共 1 处） |
| `inventory_category` | InventoryCategory | InventoryCategoryServiceImpl.java（共 1 处） |
| `inventory_code` | InventoryCode | HardwareDeviceServiceImpl.java, SalesOrderServiceImpl.java（共 2 处） |
| `inventory_movement` | InventoryMovement | InventoryMovementServiceImpl.java（共 1 处） |
| `inventory_unit` | InventoryUnit | InventoryUnitServiceImpl.java（共 1 处） |
| `inventory_warning_rules` | InventoryWarningRule | InventoryWarningScheduler.java, InventoryWarningRuleServiceImpl.java（共 2 处） |
| `invoice_reimbursement` | InvoiceReimbursement | FinanceApprovalServiceImpl.java, InvoiceReimbursementServiceImpl.java（共 2 处） |
| `invoice_reimbursement_approval_record` | InvoiceReimbursementApprovalRecord | InvoiceReimbursementServiceImpl.java（共 1 处） |
| `invoice_reimbursement_item` | InvoiceReimbursementItem | InvoiceReimbursementServiceImpl.java（共 1 处） |
| `locker_slot` | LockerSlot | TakeoutLockerServiceImpl.java（共 1 处） |
| `material_template` | MaterialTemplate | MaterialTemplateServiceImpl.java, OCRServiceImpl.java（共 2 处） |
| `material_usage_record` | MaterialUsageRecord | InventoryDeductionServiceImpl.java, KitchenScanServiceImpl.java, KitchenScanController.java（共 3 处） |
| `operation_logs` | OperationLog | FinanceRecordOperationAspect.java, CategoryOperationLogMapper.java, FinanceRecordOperationLogMapper.java（共 9 处） |
| `over_age_worker` | OverAgeWorker | OverAgeWorkerServiceImpl.java（共 1 处） |
| `plan_item` | PlanItem | PurchasePlanItemMapper.java, PlanItemServiceImpl.java, PurchasePlanServiceImpl.java（共 3 处） |
| `print_format_template` | PrintFormatTemplate | PrintFormatTemplateServiceImpl.java（共 1 处） |
| `profit_statements` | ProfitStatement | FinancialReportServiceImpl.java（共 1 处） |
| `receipt_print_log` | ReceiptPrintLog | ReceiptConfirmationServiceImpl.java（共 1 处） |
| `receivable` | Receivable | FinanceStatisticsServiceImpl.java, FinancialReportServiceImpl.java, ReceiptServiceImpl.java（共 5 处） |
| `receivables` | Receivable | FinanceStatisticsServiceImpl.java, FinancialReportServiceImpl.java, ReceiptServiceImpl.java（共 5 处） |
| `registration_code_log` | RegistrationCodeLog | RegistrationCodeServiceImpl.java（共 1 处） |
| `role_departments` | RoleDepartment | DataPermissionServiceImpl.java, SystemInitServiceImpl.java（共 2 处） |
| `role_stores` | RoleStore | DataPermissionServiceImpl.java, SystemInitServiceImpl.java（共 2 处） |
| `salary_adjustment` | SalaryAdjustment | SalaryAdjustmentServiceImpl.java（共 1 处） |
| `salary_record` | SalaryRecord | SalaryRecordServiceImpl.java（共 1 处） |
| `salary_rule` | SalaryRule | SalaryRuleServiceImpl.java（共 1 处） |
| `sales_order` | SalesOrder | DashboardServiceImpl.java, SalesOrderServiceImpl.java（共 2 处） |
| `scan_device` | ScanDevice | ScanDeviceServiceImpl.java（共 1 处） |
| `scan_record` | ScanRecord | TraceScanRecordMapper.java, TrayScanRecordMapper.java, ScanDeviceServiceImpl.java（共 4 处） |
| `self_purchase` | SelfPurchase | SelfPurchaseServiceImpl.java（共 1 处） |
| `self_purchase_item` | SelfPurchaseItem | SelfPurchaseServiceImpl.java（共 1 处） |
| `standard_cost_cards` | StandardCostCard | StandardCostCardServiceImpl.java（共 1 处） |
| `summary_templates` | SummaryTemplate | SummaryTemplateServiceImpl.java（共 1 处） |
| `supplier_evaluations` | SupplierEvaluation | SupplierEvaluationServiceImpl.java（共 1 处） |
| `takeout_locker` | TakeoutLocker | TakeoutLockerServiceImpl.java（共 1 处） |
| `tax_rate_configs` | TaxRateConfig | TaxRateConfigServiceImpl.java（共 1 处） |
| `trace_code_log` | TraceCodeLog | TraceCodeServiceImpl.java（共 1 处） |
| `transfer_templates` | TransferTemplate | TransferTemplateServiceImpl.java（共 1 处） |
| `user_sessions` | UserSession | UserSessionServiceImpl.java（共 1 处） |
| `v_inventory_summary` | InventorySummary | InventorySummaryServiceImpl.java（共 1 处） |
| `weighing_device` | WeighingDevice | WeighingDeviceServiceImpl.java（共 1 处） |
| `weighing_record` | WeighingRecord | WeighingDeviceServiceImpl.java（共 1 处） |

[BLOCK] 部署缺口 61 处：从零建的库会缺表，发版前必须补迁移
