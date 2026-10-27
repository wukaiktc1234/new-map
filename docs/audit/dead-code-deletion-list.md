# 死代码 25 张：建议删除清单（本轮只出清单，不删代码）

判据与前置条件（Lead 要求）：
- 每张表标注「生产库是否存在 / 行数」；**有数据的一律保留表、只标死代码**；
- 每张表标注删除前置条件：确认无 XML/反射引用、无报表 SQL 引用、无 `@Select` 硬编码表名；
- 本轮**不动任何代码、不动任何表**；删除需单独一轮。

数据来源：`information_schema` + `count(*)`（生产库 food_traceability，2026-10-28）

| 表 | 实体 | 生产库有表 | 行数 | 结论 | 删除前置条件 / 备注 |
|----|------|-----------|------|------|--------------------|
| `cost_allocation` | CostAllocation | 否 | 0 | 可删（无表无引用） | 确认无 XML/反射/@Select 硬编码 |
| `cost_allocation_detail` | CostAllocationDetail | 否 | 0 | 可删 | 同上 |
| `cost_allocation_rule` | CostAllocationRule | 否 | 0 | 可删（与复数表重名） | 同上 + 确认复数表承接 |
| `cost_allocation_rules` | CostAllocationRule(s) | 是（13 列） | 0 | **保留表**、只标死代码 | 无 |
| `data_link_event` | DataLinkEvent | 否 | 0 | 可删 | 确认无 MQ/事件总线引用 |
| `finance_record_audit_log` | FinanceRecordAuditLog | 否 | 0 | 可删 | 确认是否应改由 operation_logs 承接 |
| `finance_record_operation_log` | FinanceRecordOperationLog | 是（9 列） | **108** | **保留表**、只标死代码 | 有历史数据 |
| `finance_report` | FinanceReport | 否 | 0 | 可删 | 确认无报表 SQL 引用 |
| `food_trace` | FoodTrace | 否 | 0 | 可删 | 确认已由 trace 相关表取代 |
| `inventory_transfer_detail` | InventoryTransferDetail | 否 | 0 | 可删 | 确认调拨功能是否废弃 |
| `invitation_expense_record` | InvitationExpenseRecord | 否 | 0 | 可删 | 确认邀约模块是否上线 |
| `invitation_reminder_log` | InvitationReminderLog | 否 | 0 | 可删 | 同上 |
| `ocr_record` | OcrRecord | 否 | 0 | 可删 | 确认 OCR 模块是否上线 |
| `operation_type` | OperationType | 否 | 0 | 可删 | 确认已由字典/枚举取代 |
| `requisition_requests` | RequisitionRequest | 否 | 0 | 可删 | 确认是否已由 material_request 取代 |
| `sales_order_detail` | SalesOrderDetail | 否 | 0 | 可删 | 确认已由 order_items 取代 |
| `swap_request_details` | SwapRequestDetail | 否 | 0 | 可删 | 确认换班是否改为 form_json 存储 |
| `sys_devices` | SysDevice | 否 | 0 | **已由 devices 取代** | 核对实体映射是否改指向 devices |
| `sys_permissions` | SysPermission | 否 | 0 | **已由 permissions 取代（431 行）** | 同上 |
| `sys_print_templates` | SysPrintTemplate | 否 | 0 | **已由 print_format_template 取代** | 需确认哪张是现役 |
| `sys_roles` | SysRole | 否 | 0 | **已由 roles 取代（24 行）** | 同上 |
| `sys_stores` | SysStore | 否 | 0 | **已由 stores 取代（2 行）** | 同上 |
| `trace_stage` | TraceStage | 否 | 0 | 可删 | 确认已由 trace_code_log 取代 |
| `traceability_code` | TraceabilityCode | 否 | 0 | 可删 | 同上 |
| `travel_requests` | TravelRequestEntity | 否 | 0 | 可删（出差审批走 form_json） | 确认审批详情是否需要独立表 |

## sys_* 被取代的逐条复核

| 死表 | 现役表（生产库） | 现役行数 | 映射关系说明 |
|------|-----------------|---------|--------------|
| `sys_roles` | `roles` | 24 | 现役表列为 role_id/role_code/role_id_str；`SysRole` 实体仍映射旧名 `sys_roles` |
| `sys_permissions` | `permissions` | 431 | 现役表以 permission_id_str 为语义主键；`SysPermission` 实体映射旧名 |
| `sys_stores` | `stores` | 2 | 门店现役表为 `stores` |
| `sys_devices` | `devices` | — | 设备现役表为 `devices` |

**结论**：这 4 张属「历史遗留命名，已被现役表取代」；**不删实体、不删表**，先由 Lead 确认「实体映射是否改指向现役表」（那是另一次改动）。

## 统计

- 25 张里生产库存在 **2 张**：`cost_allocation_rules`（0 行）、`finance_record_operation_log`（**108 行**）→ 均**保留表**；
- 其余 **23 张生产库不存在** → 无历史数据，可进入删除流程（仍需满足前置条件）；
- 其中 **4 张 `sys_*`** 属「已被现役表取代」，不按删除处理，等映射决策；
- `travel_requests` 需产品确认：出差审批当前存 form_json，是否需要独立明细表。
