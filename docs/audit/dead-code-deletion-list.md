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

---

## 确认不删（有活引用）—— 2026-10-28 复核

以下三张曾被本清单列为候选删除，经 grep 全仓复核**存在活引用**，撤回删除建议。

| 表 | 活引用（文件:行） | 用途 | 若要删的前置条件 |
|----|------------------|------|------------------|
| `dish_ingredient` | `FoodTraceabilityServiceImpl:599-613`（按 ingredient_id 反查 dish_id）；`DishIngredientMapper:26/35/44` 三条 @Select | **召回影响面反查**：原料 → 用到它的菜品 → 受影响订单/批次 | 召回链路改造（改为直接从 order_items/recipe 反查），否则召回范围会算不全 |
| `role_departments` | `DataPermissionServiceImpl:546`；`SystemInitServiceImpl:333/343`；`RoleDepartmentMapper:24/32/47/58` | **数据权限**：角色可见部门范围 | 数据权限换实现（如改用 roles.data_scope 字段），否则权限范围失效 |
| `role_stores` | `DataPermissionServiceImpl:517`；`SystemInitServiceImpl:318/328`；`RoleStoreMapper:24/32/47/58`；调用方 5 处（OperationsDashboard:241 / LiveMonitor:280 / DecisionBoard:352 / OperationsReport:64 / PurchaseRequest:550） | **数据权限**：角色可见门店范围 | 同上；删了会「看到全部门店」 |

**判定原则**：删代码的收益是「少几张表」，代价是「召回算不全 / 数据权限失效」，**收益远小于代价**。

> **Owner 2026-10-28 拍板：撤销删除指令，保留以上三张表。**
> 保留依据（Owner 选定方案 B）：
> - `dish_ingredient` —— 支撑 **食品安全召回**（原料 → 菜品 → 订单/批次，召回影响面计算的核心关联）
> - `role_departments` / `role_stores` —— 支撑 **数据权限拦截**（门店/部门可见范围，防止越权查看其他门店数据）

## 误标注解移除记录（自动打标翻车）

| 日期 | 位置 | 误标的表 | 实际查的表 | 处置 |
|------|------|----------|------------|------|
| 2026-10-28 | `/store-inventory/stores` | inventory_log / store_inventory_log / v_inventory_summary | `stores`（存在） | 移除 3 条注解，实测 200 |
| 2026-10-28 | `/store-inventory/summary` | v_inventory_summary | 自写 SQL 查真实表 | 移除，实测 200 有数据 |
| 2026-10-28 | `/contracts/list` | contract_document | `employee_labor_contract` | 移除，403（权限）而非 501 |
| 2026-10-28 | `/salary/records` | salary_adjustment / salary_rule | `salary_record` | 移除 2 条，保留 salary_record |
| 2026-10-28 | `/operation-logs` | operation_logs | **`sys_operation_logs`（生产库已存在！）** | 移除，实测 200 |
