# placeholder

| 表名 | Controller 类#方法 + 路径 | 生产库有表 | 影响面 | 建议 |
|------|---------------------------|-----------|--------|------|
| `budget` | BudgetController （PUT /v1/finance/budgets/{id}） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `category_operation_log` | CategoryOperationLogController （GET /v1/product-center/categories/operation-logs/category/{categoryId}） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `contract_document` | EmployeeLaborContractController （GET /v1/contracts/list） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `contract_template` | ContractTemplateController （PUT /v1/hr/contract-template/{id}） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `coupon` | PosCouponController （POST /v1/pos/coupons/verify） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `file_attachment` | FileAttachmentController （POST /v1/files/upload） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `hardware_config` | DeviceManagementController （GET /v1/device-management/config） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `hardware_config` | HardwareConfigController （GET /v1/hardware/devices） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `hardware_config_version` | HardwareConfigVersionController （GET /v1/hardware/version/{versionId}） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `inventory_category` | InventoryCategoryController （PUT /v1/inventory/category/{id}） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `inventory_log` | InventoryConsumptionController （GET /v1/inventory/consumptions/page） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `inventory_log` | InventoryLogController （GET /v1/inventory/logs/{id}） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `inventory_log` | StoreInventoryController （GET /v1/store-inventory/stores） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `inventory_unit` | InventoryUnitController （PUT /v1/inventory/unit/{id}） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `material_usage_record` | KitchenScanController （POST /v1/kitchen/scan） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `operation_logs` | CategoryOperationLogController （GET /v1/product-center/categories/operation-logs/category/{categoryId}） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `operation_logs` | HealthCertificateController （GET /v1/hr/health-certificate/list） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `operation_logs` | StoreHealthCertificateController （GET /v1/store/health-certificate/list） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `over_age_worker` | OverAgeWorkerController （GET /v1/over-age-workers/page） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `plan_item` | PlanItemController （PUT /v1/plan-items/{id}） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `print_format_template` | PrintFormatTemplateController （GET /v1/print-format/default） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `receivable` | ReceivableController （PUT /v1/finance/receivables/{id}） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `salary_adjustment` | SalaryController （GET /v1/salary/records） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `salary_record` | SalaryBatchController （POST /v1/salary/batches/generate） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `salary_record` | SalaryController （GET /v1/salary/records） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `salary_rule` | SalaryController （GET /v1/salary/records） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `sales_order` | SalesOrderController （GET /v1/sales/order/page） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `scan_device` | ScanDeviceController （POST /v1/scan/device/{deviceId}/connect） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `self_purchase` | SelfPurchaseController （POST /v1/self-purchase/inbound/{selfPurchaseId}） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `store_inventory_log` | StoreInventoryController （GET /v1/store-inventory/stores） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `supplier_evaluations` | SupplierEvaluationController （POST /v1/supplier-evaluations/auto-calc） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `takeout_locker` | TakeoutLockerController （POST /v1/takeout-locker/locker/{lockerId}/connect） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `v_inventory_summary` | StoreInventoryController （GET /v1/store-inventory/stores） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |
| `weighing_device` | WeighingDeviceController （POST /v1/weighing/device/{deviceId}/connect） | 否 | 用户/后台可见 | 补表 或 下线入口 或 标未完成 |

合计：**28 张表**缺失，涉及 **29 个 Controller**、**34 条接口路径**。

## 实证（3 条，真调 / 真查）

### 证据 1：`GET /v1/product-center/categories/operation-logs/category/{categoryId}`
```
HTTP 200  body: {"code":500,"message":"获取分类操作日志失败","data":null,"success":false}
底层 SQL（对生产库直查同表）：
  psql: 错误:  关系 "category_operation_log" 不存在
  LINE 1: SELECT count(*) FROM category_operation_log
```

### 证据 2：`POST /v1/pos/coupons/verify`（coupon 表缺失）
```
psql: 错误:  关系 "coupon" 不存在
  LINE 1: SELECT count(*) FROM coupon
```

### 证据 3：`POST /v1/files/upload`（file_attachment 表缺失）
```
psql: 错误:  关系 "file_attachment" 不存在
  LINE 1: SELECT count(*) FROM file_attachment
```

补充观察（也说明这些入口确实存在且被路由到）：
```
GET  /v1/device-management/config  → {"code":400,"message":"缺少必填参数：deviceType"}  ← 入口活着，参数校验先拦
GET  /v1/contracts/list            → {"code":403,"message":"权限不足"}                    ← 入口活着，鉴权先拦
→ 说明这些接口不是「路径没注册」，而是**进了业务层就会撞上缺表**。
```

## 汇总

**生产环境共有 34 个接口路径因缺表不可用，涉及 28 张表、29 个 Controller。**
（脚本口径：`scripts/qa/broken_endpoints_prod.py`，判据 = 表在生产库不存在 且 该表实体/服务出现在带 `@*Mapping` 的 Controller 里）

## 说明与局限（如实标注）

- 「是否被前端引用」：本仓库内无前端工程与路由清单，**无法从后端侧确认**，故统一标注「未知（需前端路由清单核对）」，不做猜测。
- 影响面一栏按 Controller 所在一级路径粗分（`/v1/pos/**` 用户可见；`/v1/finance/**`、`/v1/hr/**` 后台可见），属**推断**，需业务确认。
- 建议三选一：**补表**（有生产结构可抄的 6 张优先）、**下线入口**（半成品功能）、**标未完成**（无生产结构且无调度）。

## P0 处置（2026-10-28，Lead 裁定：先让 500 变 501，不补表不下线）

做法：`@UnfinishedFeature(table="…")` 标注入口方法 + `UnfinishedFeatureInterceptor` 在进入业务前短路，
返回 `code=501`「该功能尚未上线（表结构未建立），请联系产品确认」。**不改 SQL、不补表、不删代码**，删注解即可恢复。

### 实测（改前 → 改后）

| 接口 | 改前 | 改后 |
|------|------|------|
| `GET /v1/product-center/categories/operation-logs/category/1` | code=500 获取分类操作日志失败 | **code=501 功能未上线** |
| `POST /v1/pos/coupons/verify` | code=500（coupon 表不存在） | **code=501** |
| `POST /v1/files/upload` | code=500（file_attachment 表不存在） | **code=501** |
| `GET /v1/device-management/config?deviceType=1` | code=400 缺参数 → 业务层撞缺表 | **code=501** |

### 未受影响抽查（不涉及缺表的接口）

| 接口 | 结果 |
|------|------|
| `GET /v1/promotions/statistics/effect` | code=0，返回统计结构 ✔ |
| `GET /v1/finance/budgets` | code=0，返回空分页 ✔（该接口查的是真实存在的表） |
| `GET /v1/orders/999999` | code=403 权限（业务前鉴权拦截，未受影响） |

### 前端调用面（Lead ② 要求，已能查的部分）

| 接口路径 | 前端有调用? | 命中文件 |
|----------|-------------|----------|
| `/v1/pos/coupons` | **前端有调用** | frontend-pos/src/api/posApi.ts |
| `/v1/finance/budgets` | **前端有调用** | frontend/src/api/finance/budget.ts |
| `/v1/hr/contract-template` | **前端有调用** | frontend/src/router/index.ts、views/hr/ContractDashboard.vue 等 5 处 |
| `/v1/files/upload` | 前端未见调用（未覆盖动态拼接） | - |
| `/v1/device-management/config` | 前端未见调用（未覆盖动态拼接） | - |
| `/v1/product-center/categories/operation-logs` | 前端未见调用（未覆盖动态拼接） | - |

搜索范围：`frontend/src`、`frontend-pos/src`、`frontend-kitchen/src`、`employee-frontend/src` 下 `.js/.ts/.vue/.jsx/.tsx`。

## 影响面粗分（交 Owner 排优先级）

| 一级路径 | 接口数 | 可见面 |
|----------|--------|--------|
| `/v1/pos/*` | 1 | **收银员可见**（coupons/verify） |
| `/v1/finance/*` | 1 | 财务后台（budgets） |
| `/v1/hr/*` | 2 | HR 后台（contract-template 等） |
| `/v1/product-center/*`、`/v1/device-management/*`、`/v1/files/*`、`/v1/inventory/*`、`/v1/weighing/*` 等 | 30 | 后台/设备侧 |

## 已修复（2026-10-28，反推建表后）

| 表 | 接口 | 状态 | 说明 |
|----|------|------|------|
| `coupon` | `POST /v1/pos/coupons/verify` | **已修复** | V20261028_004 建表；实测 200 code=0（空表返回 valid:false「请输入券码」） |
| `budget` | `GET /v1/finance/budgets` | **已修复** | V20261028_004 建表；实测 200 code=0 空分页 |
| `contract_template` | `GET /v1/hr/contract-template/*` | **已修复（表）/ 权限已补** | V20261028_004 建表 + V20261028_005 补 `hr:contract-template:view/manage` 权限并绑 hr_director/admin/owner；表 count(*)=0 正常 |

> 这三张由**实体反推**生成（生产库原无结构），字段以实体为准；业务需录入数据后才有内容，空表返回空结构属正常。

## 维护提示

本清单里凡标注「已修复」的行**遇到即删**，不要留过时结论；剩余仍为 501 的 25 条等产品逐条定「做 or 下线」。
