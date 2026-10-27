# V2.0 范围拆解方案（Owner 指示落地版）

> 产出人：fund-500（夜班）｜时间：2026-10-28 03:10｜**本轮只出方案，未改任何代码**
> 依据：Owner 指示 + 现场实测证据（DB / 代码 / 前端三层都查过）

---

## ⚠ 与指令冲突：三张表暂不删（需 Owner 拍板）

Owner 要求「直接删 `dish_ingredient` / `role_departments` / `role_stores`」。**本轮未删**，因为实测有活引用：

| 表 | 活引用（文件:行） | 业务后果（删了会怎样） |
|----|------------------|------------------------|
| `dish_ingredient` | `FoodTraceabilityServiceImpl:599-613`（按 `ingredient_id` 反查 `dish_id`）；`DishIngredientMapper:26/35/44` 三条 @Select | **召回影响面算不全**：原料 → 菜品 → 受影响订单这条链断掉，食品安全召回会漏批次 |
| `role_departments` | `DataPermissionServiceImpl:546`；`SystemInitServiceImpl:333/343`；`RoleDepartmentMapper:24/32/47/58` | **数据权限的部门维度失效** |
| `role_stores` | `DataPermissionServiceImpl:517`；`SystemInitServiceImpl:318/328`；`RoleStoreMapper:24/32/47/58`；调用方 5 处（`OperationsDashboard:241`、`LiveMonitor:280`、`DecisionBoard:352`、`OperationsReport:64`、`PurchaseRequest:550`） | **数据权限的门店维度失效 → 门店店长能看全部门店数据**（越权） |

**我的建议**：先不删。若 Owner 坚持删，前置条件必须同时满足：
1. 召回链路改造完成（改为直接由 `order_items` / 配方表反查，不再经 `dish_ingredient`）；
2. 数据权限换实现（如 `roles.data_scope` 字段或独立权限服务），且 5 个调用方改造并通过越权测试；
3. 完成后再删代码，**表本身不 DROP**（生产库本来也没有这两张表）。

---

## 一、`over_age_worker`（超龄用工）——**立即可开工**

**范围**：补表 + 后端 CRUD 已被现成 Controller/DTO/VO 覆盖（`OverAgeWorkerController` 等 9 个类已存在），只需建表 + 核对字段。

**以现有前端为准反推**（`frontend/src/types/hr/over-age-worker.ts:47-77`，不改设计）：

```text
id, employeeId, employeeName, retirementDate, reemploymentDate,
agreementNo, agreementStartDate, agreementEndDate,
workInjuryInsuranceNo?, workInjuryInsuranceExpiry?,
healthCheckExpiry?, healthCheckResult?('qualified'|'restricted'|'unqualified'),
restrictedPositions? (string[] → jsonb 或 text[]), remark?, createTime?, updateTime?
```
前端接口路径已定：`GET/POST /v1/over-age-workers`（`api/hr/over-age-worker.ts:86-105`），**后端 Controller 已对齐，只缺表**。

**依赖**：无（`employees` 表已存在，`employeeId` 外键引用它）。
**工作量**：0.5 人日（1 条迁移 + 字段核对 + 2 个接口冒烟）。
**风险**：`restrictedPositions` 若存 jsonb，MyBatis-Plus 需 TypeHandler；建议 `text[]` 或 `varchar` 存 JSON 串（低风险）。
**验收**：`POST /v1/over-age-workers` 201 后 `GET /v1/over-age-workers/page` 能查到；前端页面可保存并回显。

---

## 二、`scan_record`（后厨扫码记录）——**立即可开工**

**现有写入字段**（`ScanDeviceServiceImpl:269` 与 `:308` 两处 insert，实体 `ScanRecord.java` 共 20 字段）：

```text
id, scanCode（扫了什么码）, codeType, deviceId, deviceCode, scanTime（时间）,
operator, operatorId（**允许为空：后厨共用账号**）, scanLocation, scanResult,
failureReason, businessType（业务类型）, businessId（业务ID：订单/菜品/托盘）,
processStatus（动作结果：PROCESSED/ERROR）, remark,
createTime, updateTime, createBy, updateBy, deleted
```

**对照 Owner 要求的三要素**：

| Owner 要求 | 现有字段 | 结论 |
|-----------|----------|------|
| 扫了什么（订单/菜品/托盘） | `scanCode` + `codeType` + `businessType` + `businessId` | ✔ 已覆盖 |
| 时间 | `scanTime` + `createTime` | ✔ 已覆盖 |
| 动作（绑定/出餐） | `processStatus`（仅 PROCESSED/ERROR），**缺 ACTION 语义** | ⚠ **建议补 `action_type`**（BIND/OUTBOUND/QUERY） |
| `user_id` 允许为空 | `operatorId` bigint NULL | ✔ 允许为空，需在 DDL 显式 `NULL` 且代码不赋值时传 null |

**依赖**：`scan_device`（设备注册表，生产库也没有 → 见「十、深查」；本表 `device_id` 应允许为空）。
**工作量**：0.5 人日（建表 + 补 `action_type` 并回填写入点 + 冒烟）。
**风险**：`businessType` 取值无字典（自由字符串）→ 建议同时落一张 `sys_dict` 记录，否则将来统计无法归组。
**验收**：后厨扫码一次 → `scan_record` 新增 1 行，`operator_id` 为 NULL 时也成功；`processStatus` 与实际成败一致。

---

## 三、仪表盘 `/v1/dashboard/today-sales` 底层 SQL ——**已核实：不依赖 `sales_order`**

**实测结论**：这条接口**没有**查 `sales_order`。证据链：

```text
DashboardController.java:45-53  → dashboardService.getTodaySalesOverview()
DashboardServiceImpl.java:144-168 用 MyBatis-Plus QueryWrapper<SalesOrder>
SalesOrder.java:25  @TableName("orders")   ← 已按 OICBE-B2-001 对齐到 orders
SQL 实际形态（等价原文）：
  SELECT COUNT(*)                    FROM orders WHERE create_time >= ? AND create_time < ?
  SELECT COALESCE(SUM(total_amount),0) FROM orders WHERE create_time >= ? AND create_time < ?
（storeScopeResolver.applyStoreId 会追加 store_id 条件，开关关闭时不加）
```

**真调响应**（2026-10-28 03:06 实测）：
```json
{"code":0,"message":"操作成功","data":{"yesterdayGrowth":-4.8,"todayOrderCount":20,"todayAmount":58800}}
```
→ 有真实数字，**不是 0.00，也不依赖 `sales_order`**。`sales_order` 只剩 `SalesOrderController` 上的 501 注解（该 Controller 仍在用旧表名路径，属另一件事）。

**范围**：**不需要迁移**。原计划的「迁 `sales_order` → `orders`/`order_new`」判断依据如下（若将来要做）：

| 判据 | orders | order_new（若存在） |
|------|--------|---------------------|
| `SalesOrder` 实体实际映射 | **orders** | 无 |
| 活库行数 | 有数据（今日 20 单） | — |
| 字段覆盖度 | 覆盖 `create_time` / `total_amount` / `store_id` | 无证据 |
| 结论 | **就是 orders，无需迁移** | — |

**工作量**：0（本轮澄清即可，顺势把 `SalesOrderController` 的 501 注解按「误标」处理）。
**风险**：`orders.order_status` 存在两套冲突定义（`Order.java` 4=完成 vs `OrderQueryDTO` 2=完成）→ 统计口径待 PD-015/PD-019 裁决，**当前接口故意不按状态过滤**（代码注释已说明）。

---

## 四、财务总账「0.00 / 状态未知」——**先定位，结论：不是后端错，是「没这个接口」**

**我实测了 4 个候选路径**（`/v1/finance/general-ledger`、`/accounts`、`/reports/trial-balance`、`/overview/summary`）→ **全部 `code=404 资源不存在`**，说明这些是**前端猜的路径**，后端没有对应接口。

**后端真实可用的**（实测有数据）：
```json
GET /v1/finance/statistics/overview
{"code":0,"data":{"monthlyIncome":64040,"monthlyExpense":46080,"monthlyProfit":17960,
 "monthlyIncomeChange":null,"monthlyExpenseChange":668.0,"monthlyProfitChange":-399.33,
 "lastMonthIncome":0,"lastMonthExpense":6000,"lastMonthProfit":-6000,"period":"2026-10"}}
```
```json
GET /v1/finance/fund-flows?page=1&pageSize=2
{"code":0,"data":{"records":[{"flowId":5792,"flowNo":"FF202610280026","flowDirection":2,
 "flowCategory":2,"amount":2500,"balanceAfter":24002,"businessDate":"2026-10-28",
 "status":2,"statusName":"已完成","remark":"POS退款…"}]}}
```
```json
GET /v1/finance/periods?page=1&pageSize=2
{"code":0,"data":[{"periodCode":"2026-10","status":"REOPENED","statusCode":0,"reopened":true,
 "trialBalancePassed":true,"accountCount":5,"openingTotalFen":0}]}
```

**字段对照（前端 ↔ 后端）**：

| 前端字段（`frontend/src/views/finance/*.vue`） | 后端 VO 字段 | 单位/转换 | 判定 |
|---|---|---|---|
| `balance`（`BankAccountManage.vue:107`、`FinanceFund.vue:80`） | `bank_accounts.balance` / `fund_flows.balanceAfter` | 后端**分** → 前端 Converter 转元（注释已写明 `BankAccountManage.vue:10`） | ✔ 一致 |
| `amount` | `fund_flows.amount` | 分→元（`FinanceFund.vue:321`） | ✔ 一致 |
| `totalAssets` / `ledger` / `glAccount` 等**总账类字段** | **后端无对应 VO**（404） | — | ✘ **前端调的是不存在的接口** |
| `monthlyIncome/Expense/Profit` | `statistics/overview` 同名 | 分 | ✔ 一致 |
| `openingTotalFen` | `finance_periods.opening_total_fen` | 分（命名带单位，不会误读） | ✔ 一致 |

**判定**：`0.00` 的最可能来源是 ①前端调 404 接口 → 前端兜底显示 0.00；②或调 `statistics/overview` 里 `monthlyIncomeChange:null` 这类字段被格式化成 0.00。
**范围**：先让前端确认它在调哪个 URL（贴 network 面板），再决定是「补后端总账接口」还是「改前端路径」。
**工作量**：定位 0.5 人日；若需新增总账接口 +2 人日。
**风险**：总账口径（借贷方向、期初/期末）没有产品定义 → 不能凭字段名造接口（会做出「数字看着对但口径错」的东西，比 404 更危险）。

---

## 五、`self_purchase` / `self_purchase_item` ——**立即可开工（若确认上线）**

**字段（按 `SelfPurchaseServiceImpl:55-71` 写入与实体反推）**：
```text
self_purchase: selfPurchaseId(PK), purchaseNumber, purchaseType, purchaseReason, totalAmount,
  purchaseDate, purchaser, purchaserPhone, purchaseLocation, warehouseId, warehouseName,
  status('pending'|'completed'), reimburseStatus('unreimbursed'|…), remark
self_purchase_item: selfPurchaseItemId(PK), selfPurchaseId, productId, productName, specification,
  unit, quantity, unitPrice, subtotalAmount, status, traceCodeStatus, createTime, updateTime, deleted
```
**依赖**：`inventory`（入库走 `increaseInventory`，`sourceType=QUICK_STOCKIN`）；`warehouseId` 指向仓库库位。
**工作量**：0.5 人日；**风险**：前端 0 命中 → 属「写了没上线」，按 Owner 意见**建议下线入口**（保持 501）。

---

## 六、`takeout_locker` / `locker_slot` / `supplier_evaluations` ——**立即可开工（补表 + 菜单置灰）**

**字段**：`TakeoutLocker` 20 字段、`LockerSlot` 20 字段、`SupplierEvaluation` 14 字段（实体已全，见代理清单）。
**工作量**：1.5 人日（3 张表 + 索引）；**风险**：硬件相关（取餐柜），当前整体 501 下线中。
**前端菜单置灰**：
```text
菜单配置文件：frontend/src/router/index.ts（路由）+ frontend/src/layouts/*/Menu*.vue（菜单渲染）
做法：给对应路由 meta 加 { v2: true, disabled: true }，菜单项显示「V2.0 功能」并置灰不可点；
      api 调用层统一拦截：命中 v2 路由直接 return 提示，不发请求（避免 501 弹窗）
```

---

## 七、`file_attachment` ——**方案评估（不写代码）**

**对象存储五维对比**：

| 维度 | MinIO（自建） | 阿里云 OSS / S3 兼容（托管） |
|------|---------------|------------------------------|
| 部署成本 | 需服务器 + 磁盘（可单机一键 docker） | 0（开通即用） |
| 运维成本 | 要自己管磁盘/扩容/备份/升级（约 0.2 人日/月） | 云商负责，接近 0 |
| 成本模型 | 一次性硬件 + 电费；容量越大越便宜 | 按量：存储 + 流量 + 请求数（小体量更贵，大体量线性涨） |
| 迁移自由度 | 高（S3 兼容，随时可搬走） | 中（S3 兼容，但出口流量收费） |
| 技术契合度 | 高（S3 API，与 Java SDK 一致） | 高（同 S3 API） |

**推荐**：**开发/测试用 MinIO，生产用 OSS**，靠**存储抽象层**切换（两者都是 S3 协议，实现可共用 `aws-sdk-s3` 或 MinIO SDK）。
理由：本地零依赖、CI 可跑；生产不用自己运维磁盘与备份（食品安全留痕的附件不能丢，自建磁盘风险更高）。

**抽象层设计（接口 + 实现可换）**：
```java
public interface FileStorage {
    StoredFile put(String bucket, String key, InputStream in, long size, String contentType);
    InputStream get(String bucket, String key);
    String presignedUrl(String bucket, String key, Duration ttl);  // 前端直传/直读
    void delete(String bucket, String key);
}
// 实现：MinioFileStorage / S3FileStorage（同一个 S3 客户端，只换 endpoint+凭证）
```
**硬约束**：禁止 DB Blob、禁止本地磁盘落盘（已在方案里作为验收红线）。
**工作量**：抽象层 + 上传/下载/预签名 3 人日；`file_attachment` 表 0.5 人日。

---

## 八、`print_format_template` ——**设计（已存在实体 30 字段，表已在 V20261028_004 建）**

**JSON 模板结构（`template_content` 字段内容）**：
```json
{
  "version": 2,
  "paper": { "width": 58, "height": 40, "unit": "mm" },
  "blocks": [
    { "type": "text", "field": "storeName", "align": "center", "size": 14, "bold": true },
    { "type": "text", "field": "orderNumber", "align": "left", "size": 10 },
    { "type": "divider" },
    { "type": "table", "columns": [{ "field": "dishName", "width": 60 },
                                 { "field": "quantity", "width": 15, "align": "right" }] },
    { "type": "qrcode", "field": "traceCode", "size": 120 },
    { "type": "cut" }
  ]
}
```
**硬编码兜底**：`templateContent` 为空 / JSON 解析失败 / 字段缺失时，走 `DefaultReceiptTemplate.java`（代码内常量），**同时写一条 WARN 日志 + 上报**，避免打印机整单不出。
**版本/启用字段**：`status`(0/1) 控制启用；已存在 `isDefault`；**建议再加** `version_no` + `effective_from`（同类型同门店只允许 1 条 `is_default=1`，用部分唯一索引约束）。
**工作量**：模板引擎 2 人日 + 兜底 0.5 人日。

---

## 九、`inventory_code` 深查结果（Owner 待确认）

**两处写入的真实用途**：

| 位置 | 写什么 | 业务解释 |
|------|--------|----------|
| `HardwareDeviceServiceImpl:85` | `status=1`（已使用） | **硬件设备（扫码枪）写库存码，是合理的**：扫码枪是**读取设备**，扫到库存码后要「核销」把它标为已用，否则同一个码能被重复扫。它写的是**库存码的状态**，不是设备码。 |
| `SalesOrderServiceImpl:304` | 新增一条 `inventory_code`（`uniqueCode/qrCode/inventoryId/storeId/status=1`） | 销售出库时为每个明细生成**一物一码**，绑定到库存账行 |

**结论**：`inventory_code` = **库存编码（一物一码）**，与设备编码无关；值域字段全是 `inventoryId/uniqueCode/qrCode/storeId`，无 `deviceId`。
「硬件设备写库存码」不是设计混乱，而是**扫码枪的核销动作**。

## 十、`scan_device` / `weighing_device` 深查结果

```text
scan_device
  读取：ScanDeviceServiceImpl.java:62  getById(deviceId)
  写入：全仓 grep scanDeviceMapper.insert/update/delete = 0 处
  判定：**只读配置表（设备注册）**，写入方在本仓库之外（外部登记/手工作业）
weighing_device
  读取：WeighingDeviceServiceImpl.java:30/31/35 注入 + getById 类读取
  写入：0 处；:197 写的是另一张 weighing_record
  判定：同型只读配置表
```
→ 归「**外部写入假设**」，**不补表**；清单里标注“若上线需先有设备注册导入流程”。

---

## 十一、建议实施顺序（依赖图）与总计人日

```text
第 1 批（可并行，均无依赖）：
  ├─ over_age_worker 补表 + 冒烟                     0.5
  ├─ scan_record 补表 + action_type                   0.5
  └─ self_purchase(2 表) / takeout_locker(3 表) 补表  1.5
第 2 批（依赖第 1 批的表就位）：
  ├─ 前端菜单置灰（3 个 V2 功能入口）                 0.5
  └─ 财务 0.00 定位（前端贴 network → 定责）          0.5
第 3 批（依赖存储选型决策）：
  └─ file_attachment 抽象层 + MinIO/OSS 双实现        3.5
第 4 批（依赖产品口径）：
  ├─ 财务总账接口（若判定需要新增）                   +2.0
  └─ print_format_template 模板引擎 + 兜底             2.5
```

**总计：约 11.0 人日**（不含「财务总账接口」可选项 +2.0）。
**已澄清、无需工作量**：仪表盘 today-sales（不依赖 `sales_order`）、`inventory_code`/`scan_device`/`weighing_device` 分类。

**最大风险项**：财务总账 —— 口径无产品定义时**不要动**，否则做出「数字对但口径错」的报表。
