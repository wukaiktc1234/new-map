# 跨层覆盖率审计（Service → Mapper → 表 → 上游 Controller）

| 表 | Service | Mapper | 读写 | 上游 Controller 入口 | 501 覆盖 | 判定 |
|----|---------|--------|------|----------------------|----------|------|
| `sales_order` | DashboardServiceImpl | SalesOrderMapper | 有写入 | DashboardController, OperationsDashboardController | 0/2 | **需人工确认**（多入口，未全覆盖） |
| `role_departments` | DataPermissionServiceImpl | RoleDepartmentMapper | 只有读取 | - | - | 无 HTTP 入口（内部调用） |
| `role_stores` | DataPermissionServiceImpl | RoleStoreMapper | 只有读取 | - | - | 无 HTTP 入口（内部调用） |
| `hardware_config` | DeviceHeartbeatServiceImpl | HardwareConfigMapper | 有写入 | - | - | 无 HTTP 入口（内部调用） |
| `hardware_config` | DeviceStatusServiceImpl | HardwareConfigMapper | 有写入 | DeviceManagementController, HardwareConfigController | 2/2 | 已覆盖 |
| `file_attachment` | FileAttachmentServiceImpl | FileAttachmentMapper | 有写入 | FileAttachmentController | 1/1 | 已覆盖 |
| `file_attachment` | FileChunkServiceImpl | FileAttachmentMapper | 有写入 | FileChunkController | 0/1 | **需人工确认**（入口未注解） |
| `dish_ingredient` | FoodTraceabilityServiceImpl | DishIngredientMapper | 有写入 | TraceCodeController | 0/1 | **需人工确认**（入口未注解） |
| `hardware_config` | HardwareConfigServiceImpl | HardwareConfigMapper | 有写入 | DeviceManagementController, HardwareConfigController | 2/2 | 已覆盖 |
| `hardware_config` | HardwareConfigVersionServiceImpl | HardwareConfigMapper | 有写入 | HardwareConfigVersionController | 0/1 | **需人工确认**（入口未注解） |
| `hardware_config_version` | HardwareConfigVersionServiceImpl | HardwareConfigVersionMapper | 有写入 | HardwareConfigVersionController | 1/1 | 已覆盖 |
| `inventory_code` | HardwareDeviceServiceImpl | InventoryCodeMapper | 有写入 | DeviceManagementController, HardwareConfigController, HardwareDeviceController | 0/3 | **需人工确认**（多入口，未全覆盖） |
| `material_usage_record` | InventoryDeductionServiceImpl | MaterialUsageRecordMapper | 有写入 | InventoryDeductionController | 0/1 | **需人工确认**（入口未注解） |
| `inventory_log` | InventoryLogServiceImpl | InventoryLogMapper | 只有读取 | InventoryConsumptionController, InventoryLogController, StoreInventoryController | 3/3 | 已覆盖 |
| `v_inventory_summary` | InventorySummaryServiceImpl | InventorySummaryMapper | 只有读取 | StoreInventoryController | 1/1 | 已覆盖 |
| `material_usage_record` | KitchenScanServiceImpl | MaterialUsageRecordMapper | 有写入 | KitchenScanController | 1/1 | 已覆盖 |
| `operation_logs` | OperationLogServiceImpl | OperationLogMapper | 有写入 | CategoryOperationLogController, HealthCertificateController, OperationLogController, StoreHealthCertificateController | 4/4 | 已覆盖 |
| `over_age_worker` | OverAgeWorkerServiceImpl | OverAgeWorkerMapper | 有写入 | OverAgeWorkerController | 1/1 | 已覆盖 |
| `operation_logs` | PosOrderPaymentServiceImpl | OperationLogMapper | 有写入 | - | - | 无 HTTP 入口（内部调用） |
| `print_format_template` | PrintFormatTemplateServiceImpl | PrintFormatTemplateMapper | 有写入 | PrintFormatTemplateController | 1/1 | 已覆盖 |
| `inventory_code` | SalesOrderServiceImpl | InventoryCodeMapper | 有写入 | SalesOrderController | 0/1 | **需人工确认**（入口未注解） |
| `sales_order` | SalesOrderServiceImpl | SalesOrderMapper | 有写入 | SalesOrderController | 1/1 | 已覆盖 |
| `scan_device` | ScanDeviceServiceImpl | ScanDeviceMapper | 有写入 | ScanDeviceController | 1/1 | 已覆盖 |
| `scan_record` | ScanDeviceServiceImpl | ScanRecordMapper | 有写入 | ScanDeviceController | 0/1 | **需人工确认**（入口未注解） |
| `self_purchase_item` | SelfPurchaseServiceImpl | SelfPurchaseItemMapper | 有写入 | SelfPurchaseController | 0/1 | **需人工确认**（入口未注解） |
| `self_purchase` | SelfPurchaseServiceImpl | SelfPurchaseMapper | 有写入 | SelfPurchaseController | 1/1 | 已覆盖 |
| `store_inventory_log` | StoreInventoryLogServiceImpl | StoreInventoryLogMapper | 只有读取 | StoreInventoryController | 1/1 | 已覆盖 |
| `supplier_evaluations` | SupplierEvaluationServiceImpl | SupplierEvaluationMapper | 有写入 | SupplierEvaluationController | 1/1 | 已覆盖 |
| `role_departments` | SystemInitServiceImpl | RoleDepartmentMapper | 有写入 | CompanyInitController, SystemInitController | 0/2 | **需人工确认**（多入口，未全覆盖） |
| `role_stores` | SystemInitServiceImpl | RoleStoreMapper | 有写入 | CompanyInitController, SystemInitController | 0/2 | **需人工确认**（多入口，未全覆盖） |
| `locker_slot` | TakeoutLockerServiceImpl | LockerSlotMapper | 有写入 | TakeoutLockerController | 0/1 | **需人工确认**（入口未注解） |
| `takeout_locker` | TakeoutLockerServiceImpl | TakeoutLockerMapper | 有写入 | TakeoutLockerController | 1/1 | 已覆盖 |
| `trace_code_log` | TraceCodeServiceImpl | TraceCodeLogMapper | 有写入 | FoodTraceCodeController, MaterialTraceCodeController, QuickStockInController | 0/3 | **需人工确认**（多入口，未全覆盖） |
| `weighing_device` | WeighingDeviceServiceImpl | WeighingDeviceMapper | 有写入 | WeighingDeviceController | 1/1 | 已覆盖 |
| `weighing_record` | WeighingDeviceServiceImpl | WeighingRecordMapper | 有写入 | WeighingDeviceController | 0/1 | **需人工确认**（入口未注解） |

合计 35 条跨层引用；**需人工确认 14 条**。

## 读/写分级（Lead ② 要求）

口径：从 `*ServiceImpl` 的字段注入提取 Mapper 类型 → 实体 → 表；再在 service 源码里找 `insert/update/delete/save*/remove*` 与 `select/count/page/list/findBy*`。

| 分级 | 张数 | 表 |
|------|------|-----|
| **有写入** | 22 | sales_order, role_departments, role_stores, hardware_config, file_attachment, dish_ingredient, hardware_config_version, inventory_code, material_usage_record, operation_logs, over_age_worker, print_format_template, scan_device, scan_record, self_purchase_item, self_purchase, supplier_evaluations, locker_slot, takeout_locker, trace_code_log, weighing_device, weighing_record |
| **只有读取** | 3 | inventory_log, v_inventory_summary, store_inventory_log |
| 读写皆无 | 0（跨层可见范围内） | — |

处置建议：
- **有写入（22 张）**：功能半成品 → 交产品定「做 or 下线」，优先处理；
- **只有读取（3 张）**：标「**外部写入假设**」——本服务只查不写，说明写入方在别处（如视图/报表/外部系统）；
  这几张**不该由本服务建表**，需先确认写入方是谁（`v_inventory_summary` 名字是视图，很可能是别的表上的视图）。

## 需人工确认的 14 条（不猜，列候选入口）

| 表 | Service | 上游 Controller 候选 | 覆盖情况 |
|----|---------|----------------------|----------|
| `sales_order` | DashboardServiceImpl | DashboardController, OperationsDashboardController | 0/2 未覆盖 |
| `file_attachment` | FileChunkServiceImpl | FileChunkController | 0/1 未覆盖 |
| `dish_ingredient` | FoodTraceabilityServiceImpl | TraceCodeController | 0/1 未覆盖 |
| `hardware_config` | HardwareConfigVersionServiceImpl | HardwareConfigVersionController | 0/1 未覆盖 |
| `inventory_code` | HardwareDeviceServiceImpl / SalesOrderServiceImpl | DeviceManagementController, HardwareConfigController, HardwareDeviceController / SalesOrderController | 0/多 未覆盖 |
| `material_usage_record` | InventoryDeductionServiceImpl | InventoryDeductionController | 0/1 未覆盖 |
| `scan_record` | ScanDeviceServiceImpl | ScanDeviceController | 0/1 未覆盖 |
| `self_purchase_item` | SelfPurchaseServiceImpl | SelfPurchaseController | 0/1 未覆盖 |
| `role_departments` / `role_stores` | SystemInitServiceImpl | CompanyInitController, SystemInitController | 0/2 未覆盖 |
| `locker_slot` | TakeoutLockerServiceImpl | TakeoutLockerController | 0/1 未覆盖 |
| `trace_code_log` | TraceCodeServiceImpl | FoodTraceCodeController, MaterialTraceCodeController, QuickStockInController | 0/3 未覆盖 |
| `weighing_record` | WeighingDeviceServiceImpl | WeighingDeviceController | 0/1 未覆盖 |

**判定难点（需 Lead 定）**：这些 Controller 大多是「混合用途」——同一个 Controller 既有查真实表的正常端点，
也有走缺表的端点（如 `DeviceManagementController`）。给类加注解会误伤正常功能，只能**方法级逐个挑**；
而方法级的挑选需要「该端点确实写/读这张缺表」的证据，脚本给不出唯一结论，故列为需人工确认。

## 实测判定（Lead ① 要求：证据决定，不靠拍板）

判据：真调一次 → 500 且日志含 `关系 "X" 不存在` → **加注解**；200/400/403/404 → **不加**；500 但非缺表 → **另立 bug**。

| 端点 | 改前返回 | 是否缺表 | 处置 |
|------|----------|----------|------|
| `GET /v1/self-purchase/page` | 500「系统繁忙」，日志 `关系 "self_purchase" 不存在`（SelfPurchaseServiceImpl:153） | **是** | **已加注解 → 复测 501** ✔ |
| `GET /v1/self-purchase/{id}` | 同上（同链） | **是** | 一并覆盖 → 501 ✔ |
| `GET /v1/sales/order/page` | 501 | 已覆盖 | 不加 |
| `GET /v1/hardware/version/1` | 501 | 已覆盖 | 不加 |
| `GET /v1/hardware/devices` | 501 | 已覆盖 | 不加 |
| `GET /v1/dashboard/today-sales` | 200 code=0 有数据 | 否 | **不加**（不碰缺表） |
| `GET /v1/dashboard/inventory-alerts` | 200 code=0 | 否 | 不加 |
| `GET /v1/dashboard/member-growth` | 200 code=0 | 否 | 不加 |
| `GET /v1/dashboard/finance-summary` | 200 code=0 | 否 | 不加 |
| `GET /v1/dashboard/overview/1` | 200 code=0 | 否 | 不加（**DashboardServiceImpl 虽有 SalesOrderMapper 字段，但该端点未使用**） |
| `GET /v1/operations/dashboard/*` | 403 权限 | 无法判定 | 不加（鉴权先拦） |
| `GET /v1/trace-codes/page`、`/recall-query` | 403 权限 | 无法判定 | 不加 |
| `GET /v1/hardware/devices/{id}` | 403 权限 | 无法判定 | 不加 |
| `GET /v1/hardware/device/status` | 400 缺参数 | 否 | 不加 |
| `GET /v1/scan/devices/status` | 403 权限 | 无法判定 | 不加 |
| `GET /v1/weighing/devices`、`/weighing/records` | 404 资源不存在 | 否（路径未注册） | 未测（路径需确认） |
| `GET /v1/takeout-lockers`、`/supplier-evaluations/page`、`/print-format-templates` | 404 | 否 | 未测（路径需确认） |
| `GET /v1/inventory-deduction/deduction-record/FTC-1` | 500「未找到扣减记录」 | **未判定**（业务码，非缺表） | 另立观察项，不加 |
| `POST /v1/files/chunk/init|merge`、`POST /v1/trace-codes/{id}/chain-nodes`、`POST /v1/scan/process` 等写接口 | — | — | **未测（需构造含文件/业务主键的报文，且会产生写入副作用）** |

**方法级 vs 文件级的教训**：上一轮我用「文件里存在 `@UnfinishedFeature(table=X)`」判覆盖 —— `SelfPurchaseController` 有注解（标在 `/inbound/{id}` 上），
但 `/{page}`、`/{id}` 并未被覆盖，实测仍是 500。**文件级检查会高估覆盖率**，必须按方法级或按实测判定。

