# 22 张「有写入代码但两边都无表」清单（Owner 要求逐条贴出）

数据来源：`backend/src/main/java` 全量扫描 `xxxMapper.insert/update/delete/save/remove`（159 张有写入的表）∩ 生产库缺失表；
前端引用 = 扫 `frontend/src`、`frontend-pos/src`、`frontend-kitchen/src` 下 .ts/.vue/.js。

| 表名 | 写入代码位置(文件:行) | 写入点数 | 推测业务用途 | 前端引用 | 分类 |
|------|----------------------|----------|--------------|----------|------|
| `sales_order` | `SalesOrderServiceImpl.java:82/92` 等 | 8 | 销售订单（与 order_new 并存的旧模型） | frontend/src/api/dashboard/index.ts | **像废弃**（已被 order_new/orders 取代，但仪表盘仍读） |
| `role_departments` | `SystemInitServiceImpl.java:333` | 1 | 角色-部门数据权限范围 | - | **像没做完**（数据权限初始化用，无页面） |
| `role_stores` | `SystemInitServiceImpl.java:318` | 1 | 角色-门店数据权限范围 | - | **像没做完**（同上） |
| `hardware_config` | `DeviceHeartbeatServiceImpl.java:210`、`HardwareConfigServiceImpl.java:152` | 5 | 硬件设备配置（打印机/秤） | - | **像没做完**（设备管理页在，但配置表没建） |
| `file_attachment` | `FileCleanupScheduler.java:65`、`FileAttachmentServiceImpl.java:111` | 5 | 附件元数据 | - | **像没做完**（上传入口有，表没有 → 已 501） |
| `dish_ingredient` | 无直接写入（仅读取/映射） | 0 | 菜品-原料关联（旧模型） | - | **像废弃**（已由 dish_recipe_new 取代） |
| `hardware_config_version` | `HardwareConfigVersionServiceImpl.java:82/152` | 2 | 硬件配置版本历史 | - | **像没做完** |
| `inventory_code` | `HardwareDeviceServiceImpl.java:85`、`SalesOrderServiceImpl.java:304` | 2 | 库存编码（扫码用） | - | **不确定**（两处不同业务都写它，可能是共用码表） |
| `material_usage_record` | `InventoryDeductionServiceImpl.java:72/216` | 2 | 原料使用记录（出餐扣料） | - | **像没做完**（后厨扫码写它 → 已 501） |
| `operation_logs` | `FinanceRecordOperationAspect.java:174`、`OperationLogServiceImpl.java:31` | 4 | 通用操作日志 | - | **像没做完**（日志页有，表没有） |
| `over_age_worker` | `OverAgeWorkerServiceImpl.java:58/66` | 3 | 超龄用工登记 | frontend/src/api/hr/over-age-worker.ts | **像没做完**（**前端有页面**） |
| `print_format_template` | `PrintFormatTemplateServiceImpl.java:88/141` | 2 | 打印格式模板 | - | **像没做完** |
| `scan_device` | 无直接写入（仅读取） | 0 | 扫码设备注册 | - | **不确定** |
| `scan_record` | `ScanDeviceServiceImpl.java:269/308` | 2 | 扫码记录 | frontend-kitchen/src/views/Home.vue | **像没做完**（**前端在用**） |
| `self_purchase_item` | `SelfPurchaseServiceImpl.java:67/129` | 2 | 自采明细 | - | **像没做完**（主表 self_purchase 也缺） |
| `self_purchase` | 无直接写入（主表由 Service 组装） | 0 | 自采单主表 | - | **像没做完** |
| `supplier_evaluations` | `SupplierEvaluationServiceImpl.java:66/141` | 3 | 供应商评估 | - | **像没做完** |
| `locker_slot` | `TakeoutLockerServiceImpl.java:201/245` | 4 | 取餐柜格口 | - | **像没做完** |
| `takeout_locker` | 无直接写入（主表） | 0 | 取餐柜 | - | **像没做完** |
| `trace_code_log` | `TraceCodeServiceImpl.java:106/285` | 2 | 追溯码操作日志 | - | **像没做完** |
| `weighing_device` | 无直接写入（主表） | 0 | 称重设备 | - | **像没做完** |
| `weighing_record` | `WeighingDeviceServiceImpl.java:197` | 1 | 称重记录 | - | **像没做完** |

## 分类汇总

| 分类 | 张数 | 表 |
|------|------|-----|
| **像废弃** | 2 | `sales_order`、`dish_ingredient` |
| **像没做完** | 17 | `role_departments`、`role_stores`、`hardware_config`、`file_attachment`、`hardware_config_version`、`material_usage_record`、`operation_logs`、`over_age_worker`、`print_format_template`、`scan_record`、`self_purchase`、`self_purchase_item`、`supplier_evaluations`、`takeout_locker`、`locker_slot`、`trace_code_log`、`weighing_record` |
| **不确定** | 3 | `inventory_code`（两业务共用）、`scan_device`、`weighing_device`（仅读取） |

## 需要产品/你拍板的三类动作

1. **像废弃（2 张）**：确认后删实体+Mapper+Service 引用（`dish_ingredient` 已被 dish_recipe_new 取代；`sales_order` 若仪表盘还在读，需先把仪表盘改到新表再删）；
2. **像没做完（17 张）**：确认是否立项；不立项则**下线入口**（现在已 501）；立项则按实体/设计补表；
3. **不确定（3 张）**：`inventory_code` 需确认两个业务是否共用同一码表；`scan_device`/`weighing_device` 仅读取 → 确认写入方是谁。

## 前端有页面的（优先给产品看）

- `over_age_worker` → `frontend/src/api/hr/over-age-worker.ts`（超龄用工页）
- `scan_record` → `frontend-kitchen/src/views/Home.vue`（后厨主页扫码）
- `sales_order` → `frontend/src/api/dashboard/index.ts`（仪表盘读销售单）

