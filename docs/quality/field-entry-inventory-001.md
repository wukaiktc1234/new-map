# FIELD-ENTRY-INVENTORY-001 —— 关键字段入口扫描清单

> **卡片**：关键字段入口扫描（产出清单，不改代码）
> **判档**：轻档（只读扫描 + 写文档；不触发 PG-005 三段式）
> **扫描日期**：2026-09-30　**HEAD**：`42ca33c`（origin/master = `ff61f85`）
> **产出边界**：本文件只做**登记**，不修任何不一致，不改任何代码。
> **扫描字段**：`location_id / locationId`、`store_id / storeId`、`warehouse_id / warehouseId`
> **扫描范围**：`backend/src/main/java` + `backend/src/main/resources/mapper`；`frontend/src`（.vue + .ts）
> **范围外客户端**：`frontend-design-demo / frontend-pos / frontend-kitchen / employee-frontend`（命中情况单列第 5 节登记）

---

## 0. 口径与 ID 空间定义

| 代号 | 空间 | 真值来源 | 说明 |
|---|---|---|---|
| **LOC** | `locations.location_id` | `locations` 表 | design-002 后的**唯一身份空间**；`users.location_id`、`employees.location_id`、`inventory.location_id` 属此空间 |
| **STORE** | `stores_new.store_id` | `stores_new` / `stores` 表 | 旧身份空间；仍是 **41 张业务表**的归属列 |
| **WH** | `warehouses.warehouse_id` | `warehouses` 表 | 仓储空间；17 张表使用 |
| **CODE** | `locations.location_code` | `locations` 表 | 字符串「第四空间」，被某些 Mapper 当作 `store_id` 使用（见 §1.4 异常项） |

**「是否经桥」判定口径**：只有经 `LocationIdBridge`（`backend/src/main/java/com/foodtraceability/common/util/LocationIdBridge.java:38-70`）或 `location_id_map` JOIN 的换算才算「经桥」。数值直传、字段别名直赋、`transient` 字段读写一律记 **❌（未换算 / 违规）**。

**桥本体（合法换算入口）**

| 方法 | file:line |
|---|---|
| `SRC_STORES_NEW = "stores_new"` | LocationIdBridge.java:23 |
| `SRC_WAREHOUSES = "warehouses"` | LocationIdBridge.java:26 |
| `toStoreId(locationId)` | LocationIdBridge.java:38-42 |
| `toLocationId(storeId)` | LocationIdBridge.java:46-50 |
| `toWarehouseId(locationId)` | LocationIdBridge.java:54-58 |
| `static storeIdOf(locationId)` | LocationIdBridge.java:62 |
| `static locationIdOfStore(storeId)` | LocationIdBridge.java:68 |
| 桥 SQL（`selectSrcIdByLocationId` / `selectLocationIdBySrcId`） | mapper/LocationIdMapMapper.java:19,25 |
| `location_id_map` DDL（`UNIQUE(src_table, src_id)`） | db/migration/V20260927_001__create_locations_and_location_id_map.sql:38-47 |

**收窄规则应用**：backend java 原始命中 2164 行 / 435 文件，frontend 658 行 / 127 文件。按卡片约定收窄为「非注释、非测试」代码行，并把同文件连续行合并为一行（写作 `:a-b`）。getter/setter 样板行不逐条列出，只在「契约声明」中按字段声明行登记。

---

## 1. 表 1 —— 字段读写点清单

### 1.1 身份与上下文层（LOC 空间入口）

| 字段 | file:line | 操作 | ID 空间 | 是否经桥 |
|---|---|---|---|---|
| locationId | utils/SecurityUtils.java:125-131 | 读（当前用户归属） | LOC | — 原生 LOC |
| storeId | utils/SecurityUtils.java:124-131 `getCurrentUserStoreId()` | 读（兼容访问器） | LOC→STORE | ✅ `storeIdOf` |
| storeId | utils/SecurityUtils.java:181-184 `hasStorePermission()` | 读（权限判定） | STORE（经桥） | ✅ 间接 |
| locationId | common/util/LocationGuard.java:56 | 读 `getCurrentUserLocationId()` | LOC | — |
| storeId | common/util/LocationGuard.java:82 `requireStoreContext()` | 读（换算） | LOC→STORE | ✅ `toStoreId` |
| warehouseId | common/util/LocationGuard.java:109 `requireWarehouseContext()` | 读（换算） | LOC→WH | ✅ `toWarehouseId` |
| locationId | mapper/UserMapper.java:146 | **写**（`UPDATE users SET location_id = NULL`） | LOC | — |
| locationId | mapper/UserMapper.java:162 | 读（`JOIN location_id_map … src_table='stores_new'`） | LOC↔STORE | ✅ SQL 级桥 |
| locationId | mapper/LocationMapper.java:21,29,38 | 读（`JOIN location_id_map`） | LOC↔旧空间 | ✅ SQL 级桥 |
| storeId | security/service/impl/UserPermissionCacheServiceImpl.java:174 | 读→缓存字段（`storeIdOf(user.getLocationId())`） | LOC→STORE | ✅ |
| storeId | service/impl/DataPermissionServiceImpl.java:292,387,576 | 读（数据权限） | LOC→STORE | ✅ |
| storeId | service/impl/UserDataServiceImpl.java:139,153 | 读（门店名展示） | LOC→STORE | ✅ |
| storeId | service/impl/UserServiceImpl.java:282 | 读（`store-info` 响应） | LOC→STORE | ✅ |
| storeId | service/impl/EmployeeBasicDataServiceImpl.java:108 | 读（员工→门店名） | LOC→STORE | ✅ |
| storeId | service/impl/EmployeeDataServiceImpl.java:122 | 读 | LOC→STORE | ✅ |
| storeId | **15 处** `SecurityUtils.getCurrentUserStoreId()` 直接调用点（12 文件）：controller/CallNumberQueueManagementController.java:95；controller/DiningTableManagementController.java:211；controller/StoreInventoryController.java:76；service/finance/impl/BankAccountServiceImpl.java:234；service/impl/DeviceStatusServiceImpl.java:178；service/impl/schedule/SchedulePlanServiceImpl.java:69,548；service/impl/PosOrderCreateServiceImpl.java:314；service/impl/RecruitmentRequirementServiceImpl.java:162；service/impl/SalesOrderServiceImpl.java:268,329；service/impl/StockForecastServiceImpl.java:53,190；service/impl/StoreInventoryLogServiceImpl.java:111；service/purchase/impl/PurchasePlanServiceImpl.java:440 | 读（身份） | STORE（经桥回退） | ✅ 但属**冻结兼容层** |
| storeId | service/impl/schedule/SchedulePlanServiceImpl.java:150,264 | 读（私有包装 `getCurrentUserStoreId()`） | STORE | ✅ 间接 |
| locationId | utils/SecurityUtils.java:125 外部调用点 | 读 | LOC | — |

> **量化事实**：`getCurrentUserStoreId()`（旧空间兼容访问器）外部调用点 **15 处 / 12 文件**；`getCurrentUserLocationId()` 外部调用点 **1 处**（LocationGuard.java:56）。身份层实际仍以 STORE 口径消费。

### 1.2 实体与 DTO/VO 契约层

| 字段 | file:line | 操作 | ID 空间 | 是否经桥 |
|---|---|---|---|---|
| locationId | entity/User.java:101-102 `@TableField("location_id")` | 声明 | LOC | — |
| locationId | entity/Employee.java:107-110 `@TableField("location_id")` | 声明 | LOC | — |
| warehouseId | entity/Employee.java:129-131 `@TableField("warehouse_id")` | 声明 | **WH（与 location_id 并存）** | ❌ 双空间并存 |
| workLocationType | entity/Employee.java:122-124 | 声明（STORE/WAREHOUSE/HEADQUARTERS 判别列） | 类型判别 | — |
| locationId | entity/Inventory.java:27-28 | 声明 | LOC | — |
| storeId | entity/Inventory.java:122-124 `@Deprecated transient Long storeId`（注释：「禁止直接当查询条件，须经 location_id_map —— 宪法 §三.3」） | 声明（非映射） | STORE | ❌ 不映射 |
| warehouseId | entity/Inventory.java:126-128 `@Deprecated transient Long warehouseId`（「已并入 location_id，S4 收编后移除」） | 声明（非映射） | WH | ❌ 不映射 |
| locationId + warehouseId | entity/InventoryLocation.java:21,26-27 | 声明（**同表双空间列**） | LOC + WH | ❌ |
| storeId | entity/RoleStore.java:16（`@TableName("role_stores")`） | 声明 | STORE | ❌ |
| storeId | entity/DeviceAlert.java:105-108 `transient Long storeId` | 声明（非映射） | STORE | ❌ |
| locationId | dto/UserAssignStoreDTO.java:21 | 声明（入参） | LOC | — ✅ 已迁 |
| locationId | dto/UserAssignStoreBatchDTO.java:27 | 声明（入参） | LOC | — ✅ 已迁 |
| locationId | dto/UserCreateDTO.java:49 / dto/UserUpdateDTO.java:36 | 声明（入参） | LOC | — ✅ 已迁 |
| locationId | dto/OnboardingArchiveDTO.java:54 | 声明 | LOC | — |
| locationId | dto/PurchaseStockinItemDTO.java:86 | 声明 | LOC | — |
| storeId | dto/UserInfoResponse.java:33 | 声明（**出参仍是旧空间**） | STORE | ❌ |
| storeId | dto/UserBasicInfo.java:34,186 | 声明（出参） | STORE | ❌ |
| storeId | dto/EmployeeCreateDTO.java:36 / dto/EmployeeUpdateDTO.java:36 | 声明（入参） | STORE | ❌ **未迁** |
| warehouseId | dto/EmployeeCreateDTO.java:39 / dto/EmployeeUpdateDTO.java:39 | 声明（入参） | WH | ❌ **未迁** |
| storeId | dto/EmployeeBasicInfo.java:32,53 | 声明（出参） | STORE | ❌ |
| storeId | dto/PermissionAssignmentDTO.java:32（String） | 声明 | STORE | ❌ |
| storeId | dto/SettlementQueryDTO.java:16（String） | 声明 | STORE | ❌ |
| storeId | dto/StoreBasicInfo.java:10,179（String） | 声明 | STORE | ❌ |
| storeId | dto/CanonicalOrderCommand.java:38（String） | 声明 | STORE | ❌ |
| storeId | dto/OrderRequestDTO.java:53（String） | 声明 | STORE | ❌ |
| storeId | dto/PurchaseRequestDTO.java:35 / PurchaseRequestCreateDTO.java:34（String） | 声明 | STORE | ❌ |
| storeId | dto/QuickStockInDTO.java:42（String） | 声明 | STORE | ❌ |
| storeId | dto/MaterialScanConsumeDTO.java:30 / MaterialTraceCodeGenerateDTO.java:52（String） | 声明 | STORE | ❌ |
| storeId | dto/ReceiptConfirmationQueryDTO.java:24 | 声明 | STORE | ❌ |
| storeId | dto/KitchenOrderCreateDTO.java:24 / KitchenOrderFullDTO.java:56 | 声明 | STORE | ❌ |
| storeId | dto/DeviceCreateDTO.java:69 / DeviceQueryDTO.java:27 / DeviceUpdateDTO.java:44 / DeviceVO.java:49 | 声明 | STORE | ❌ |
| storeId | dto/PrintTemplateCreateDTO.java:44 / PrintTemplateUpdateDTO.java:39 / PrintTemplateVO.java:40 | 声明 | STORE | ❌ |
| storeId | dto/PreMakeRequestDTO.java:29 / FoodTraceCodeGenerateDTO.java:33 | 声明 | STORE | ❌ |
| warehouseId | dto/InventoryIncreaseDTO.java:21 **+ locationId:24** | 声明（**同一 DTO 双空间**） | WH + LOC | ❌ |
| warehouseId | dto/InventoryDecreaseDTO.java:21 / InventoryAdjustCreateDTO.java:23（String） / InventoryCheckCreateDTO.java:17 / InventoryLocationCreateDTO.java:20 / LossOutboundCreateDTO.java:16 / OtherInboundCreateDTO.java:20 / InventoryWarningUpdateDTO.java:22 / InventoryWarningRuleCreateDTO.java:20 / InventoryWarningRuleUpdateDTO.java:17 | 声明 | WH | ❌ |
| warehouseId | dto/PurchaseOrderCreateDTO.java:29 / PurchaseOrderUpdateDTO.java:29 / PurchaseOrderCreateFromWarningDTO.java:16 / PurchaseStockinCreateDTO.java:31 / PurchaseReturnVO.java:42 / PurchaseSuggestionVO.java:20 / WarehouseVO.java:14 / ProcurementTraceInfoVO.java:136 / ProcurementTraceMaterialItemVO.java:97 | 声明 | WH | ❌ |
| warehouseId | dto/MaterialTraceCodeGenerateDTO.java:48（String） | 声明 | WH | ❌ |

### 1.3 Mapper / SQL 层

| 字段 | file:line | 操作 | ID 空间 | 是否经桥 |
|---|---|---|---|---|
| store_id | mapper/RoleStoreMapper.java:24,32,47,58（`SELECT store_id FROM role_stores`） | 读 | STORE | ❌ |
| store_id | resources/mapper/RoleStoreMapper.xml:7,10（`INSERT INTO role_stores (role_id, store_id …)`） | **写** | STORE | ❌ |
| store_id | mapper/SysStoreMapper.java:21,29（`sys_stores` + `sys_user_stores us … us.is_default = 1`） | 读 | STORE（第三套旧机制） | ❌ **全库无调用点 → 死代码** |
| location_id | resources/mapper/InventoryMapper.xml:8,30,36-37,55 | 读/筛 | LOC | — ✅ |
| location_id | resources/mapper/InventorySummaryMapper.xml:20,22（`JOIN locations l ON l.location_id = i.location_id`） | 读 | LOC | — ✅ |
| location_id | mapper/InventoryMapper.java:29-30（`@Param("locationId")`，注释「统一账按 location_id 维度」） | 读 | LOC | — ✅ |
| store_id | resources/mapper/DailySettlementMapper.xml:8,30,39-40,75,87 | 读/筛 | STORE | ❌ |
| store_id | resources/mapper/OrderMapper.xml:19,37,54,71 | 读/筛 | STORE | ❌ |
| store_id | resources/mapper/SalesOrderMapper.xml:37 | 读（resultMap） | STORE | ❌ |
| store_id | resources/mapper/PurchaseStockinMapper.xml:25 | 读（resultMap） | STORE | ❌ |
| store_id | resources/mapper/CallNumberQueueNewMapper.xml:9 | 筛 | STORE | ❌ |
| store_id | resources/mapper/DiningTableNewMapper.xml:9 | 筛 | STORE | ❌ |
| store_id | resources/mapper/HardwareConfigMapper.xml:8-9 | 筛 | STORE | ❌ |
| store_id | resources/mapper/HardwareConfigVersionMapper.xml:21,54（`store_id AS "storeId"`） | 读/别名 | STORE | ❌ |
| store_id | resources/mapper/schedule/SwapRequestMapper.xml:79（`sp.store_id`） | 筛 | STORE | ❌ |
| warehouse_id | resources/mapper/WarehouseMapper.xml:7,23 | 读 | WH | — |
| warehouse_id | resources/mapper/InventoryOutboundMapper.xml:13-14；InventoryAdjustMapper.xml:13-14；InventoryLossMapper.xml:10-11；InventoryCheckMapper.xml:9,43-44；InventoryWarningMapper.xml:10-11,26；LossOutboundMapper.xml:13,31；OtherInboundMapper.xml:14；TraceCodeMapper.xml:19 | 读/筛 | WH | ❌ |
| location_id / warehouse_id | **resources/mapper/InventoryLogMapper.xml:9**（`m.location_id AS warehouse_id`）与 **:26-27**（`AND m.location_id = #{warehouseId}`） | 读 + 筛 | **LOC 列被当作 WH 参数比较** | ❌ **无桥、命名跨空间** |
| location_code / store_id | **resources/mapper/StoreInventoryLogMapper.xml:10**（`l.location_code AS store_id`）与 **:23-24**（`AND l.location_code = #{storeId}`） | 读 + 筛 | **CODE 被当作 STORE** | ❌ **第四空间混入** |
| location_id | resources/mapper/StoreInventoryLogMapper.xml:21（`JOIN locations l ON l.location_id = m.location_id`） | 读 | LOC | — ✅ |
| store_id | aspect/DataPermissionAspect.java:140,164（`queryWrapper.eq("store_id", storeId)`） | 筛（数据权限） | STORE | ❌ 直接字符串列名 |
| store_id | LambdaQueryWrapper 过滤点（代表）：controller/FoodTraceCodeController.java:96；controller/kitchen/KitchenOrderController.java:80；dataservice/impl/CallNumberQueueDataServiceImpl.java:97；dataservice/impl/DiningTableDataServiceImpl.java:81；dataservice/impl/OperationsDashboardDataServiceImpl.java:389,412,421；dataservice/impl/schedule/SchedulePlanDataServiceImpl.java:82；service/device/impl/PrintTemplateServiceImpl.java:160,189,214,298；service/finance/impl/BankAccountServiceImpl.java:249；service/impl/AssetMasterNewServiceImpl.java:62；service/impl/CallNumberQueueNewServiceImpl.java:169；service/impl/DeviceStatusHistoryServiceImpl.java:98,169,198,228；service/impl/DiningTableNewServiceImpl.java:165；service/impl/OrderNewServiceImpl.java:1949；service/impl/RecruitmentQuotaServiceImpl.java:353；service/impl/RecruitmentRequirementServiceImpl.java:279；service/impl/schedule/SchedulePlanServiceImpl.java:86,563；service/impl/schedule/ScheduleShiftTypeServiceImpl.java:184,203；service/impl/schedule/ScheduleTemplateServiceImpl.java:193 | 筛 | STORE | ❌ |
| warehouse_id | service/impl/InventoryCheckServiceImpl.java:54；service/impl/InventoryLocationServiceImpl.java:127,187 | 筛 | WH | ❌ |
| locationId | service/impl/InventoryServiceImpl.java:556（`Inventory::getLocationId`） | 筛 | LOC | — ✅ |

### 1.4 服务层写入点（含兜底与静默丢失）

| 字段 | file:line | 操作 | ID 空间 | 是否经桥 |
|---|---|---|---|---|
| locationId | service/impl/UserServiceImpl.java:176,204 | **写**（用户分配/创建） | LOC | — |
| locationId | service/impl/UserServiceImpl.java:254 | **写**（user→employee 正向同步） | LOC | — |
| locationId | service/impl/EmployeeServiceImpl.java:311（create） | **写** `LocationIdBridge.locationIdOfStore(dto.getStoreId())` | STORE→LOC | ✅ 但**仅门店方向** |
| locationId | service/impl/EmployeeServiceImpl.java:353（update） | **写** 同上 | STORE→LOC | ✅ 但**仅门店方向** |
| warehouseId | service/impl/EmployeeServiceImpl.java:312,354（`employee.setWarehouseId(dto.getWarehouseId())`） | **写** | WH | ❌ **无 仓库→location 正向桥**（缺口① 实锤） |
| locationId | service/impl/InventoryServiceImpl.java:432,586 | **写**（库存与流水） | LOC | — ✅ |
| storeId | controller/AssetController.java:301；service/impl/AssetMasterServiceImpl.java:150 | **写** | STORE | ❌ |
| storeId | service/impl/CallNumberQueueNewServiceImpl.java:53 | **写** | STORE | ❌ |
| storeId | service/impl/DailySettlementServiceImpl.java:540 | **写** | STORE | ❌ |
| storeId | service/impl/SystemInitServiceImpl.java:324 | **写**（初始化） | STORE | ❌ |
| storeId | service/impl/PermissionAutoAssignServiceImpl.java:112,134 | **写**（权限分配） | STORE | ❌ |
| storeId | service/impl/PurchaseArrivalServiceImpl.java:655 | **写** | STORE | ❌ |
| storeId | service/impl/SalesOrderServiceImpl.java:299 | **写** | STORE | ❌ |
| storeId | service/impl/TrayServiceImpl.java:500 | **写** | STORE | ❌ |
| storeId | service/impl/schedule/SchedulePlanServiceImpl.java:160 | **写** | STORE | ❌ |
| storeId | controller/device/DeviceController.java:89,109；controller/StoreManagementSettlementController.java:153；controller/InventoryController.java:51（warehouseId） | **写**（查询对象填充） | STORE / WH | ❌ |
| storeId 兜底 | service/impl/HardwareConfigServiceImpl.java:95（`storeId != null ? storeId : DeviceConstants.DEFAULT_STORE_ID`） | **写**（常量兜底） | STORE | ❌ **硬编码默认门店** |
| warehouseId 兜底 | service/impl/PurchaseArrivalServiceImpl.java:656（`key.warehouseId != null ? key.warehouseId : 1L`） | **写**（字面量 `1L` 兜底） | WH | ❌ **硬编码默认仓库** |
| warehouseId | service/impl/InventoryAdjustServiceImpl.java:341,356；InventoryLossServiceImpl.java:149；InventoryOutboundServiceImpl.java:293；SelfPurchaseServiceImpl.java:106；TraceCodeServiceImpl.java:67；service/purchase/impl/MaterialArchiveServiceImpl.java:388 | **写** | WH | ❌ |
| **静默丢失** | service/purchase/impl/MaterialArchiveServiceImpl.java:388（`inventory.setWarehouseId(...)` 打在 `Inventory` 的 `transient` 字段上，见 entity/Inventory.java:126-128） | **写→不落库** | WH | ❌ **新建库存行丢失归属** |
| **静默丢失** | config/InventoryWarningScheduler.java:188（`record.setWarehouseId(inventory.getWarehouseId())`，读的是加载后恒为 null 的 transient 字段） | **写 null** | WH | ❌ |
| **违规查询** | service/impl/ProcurementTraceServiceImpl.java:443（`wrapper.eq(Inventory::getWarehouseId, warehouseId)`） | 筛（把 **非映射 transient 字段**当查询条件） | WH | ❌ **违反 entity/Inventory.java:122 明示的「宪法 §三.3」** |

### 1.5 前端字段读写点（非 UI 入口部分）

| 字段 | file:line | 操作 | ID 空间 | 是否经桥 |
|---|---|---|---|---|
| storeId | frontend/src/composables/useStoreOptions.ts:44-46（`storeArchiveApi.getActiveStores()`） | 读 | STORE | ❌ |
| storeId | frontend/src/composables/useStoreOptions.ts:63（`s.storeId === numId`） | 读（取名） | STORE | ❌ |
| locationId | frontend/src/composables/useLocationOptions.ts:38-39（`locationApi.listActive()`） | 读 | LOC | — ✅ |
| locationId | frontend/src/api/location.ts:28（`GET /v1/locations`） | 读 | LOC | — ✅ |
| storeId | frontend/src/api/store-ops/store-archive.ts:232,269,304,318,327（`/v1/stores`、`/v1/stores/active`） | 读/写 | STORE | ❌ |
| warehouseId | frontend/src/api/warehouse/warehouse.ts:165,189,216（`/v1/warehouses`、`/v1/warehouses/active`） | 读/写 | WH | ❌ |
| locationId | frontend/src/api/system/user.ts:198-199（`PUT /v1/users/{id}/assign-store` body `{locationId}`） | **写** | LOC | — ✅ 已迁 |
| locationId | frontend/src/api/system/user.ts:206-207（`DELETE /v1/users/{id}/assign-store`） | **写**（解绑） | LOC | — ✅ |
| storeId / warehouseId | frontend/src/views/hr/HREmployee.vue:170-171（后端→本地转换读） | 读 | STORE + WH | ❌ |
| storeId / warehouseId | frontend/src/views/hr/HREmployee.vue:198-199（本地对象持有） | 读/展示 | STORE + WH | ❌ |
| storeId / warehouseId | frontend/src/views/hr/HREmployee.vue:252-253（本地→后端 DTO 写，按 `workLocationType` 二选一） | **写** | STORE + WH | ❌ |
| storeId / warehouseId | frontend/src/views/hr/components/EmployeeDetailDialog.vue:91-92 | 展示 | STORE + WH | ❌ |
| storeId | frontend/src/api/{asset/asset.ts, auth.ts, device/history.ts, device/index.ts, finance/bank-account.ts, finance/converters.ts, finance/tax-calculation.ts, operations/strategy.ts, order/converters.ts, order/order.ts, order/reservation.ts, purchase/arrival.ts, purchase/order.ts, receipt-confirmation.ts, schedule/converters.ts, schedule/plans.ts, schedule/shift-types.ts, schedule/templates.ts, store-ops/certificate.ts, store-ops/converters.ts, store-ops/queue.ts, store-ops/settlement.ts, store-ops/store-archive.ts, store-ops/store-inventory.ts, store-ops/table.ts, system/sys-setting.ts, tray.ts} | 读/写 | STORE | ❌（26 个 API 模块仍按旧空间收发） |
| warehouseId | frontend/src/api/warehouse/*.ts（adjust / check / location / loss / outbound / warning / stockin / transfer） | 读/写 | WH | ❌ |

---

## 2. 表 2 —— UI 入口清单（重点）

> 表 2 共 **36 行 = 33 个字段绑定入口 + 3 个补录行**（① 已迁移的「分配归属位置」对话框：绑定名 `assignLocationId` 不含字段 token，纯 token 扫描会漏；② 用户新建/编辑表单：应有权属字段但 UI 缺项；③ 员工「归属类型」判别下拉：非 ID 字段但决定落库空间）。补录理由见 §6.3。

### 2.1 身份类 UI 入口（决定「某人归属哪个位置」）

| UI 位置 | file:line | 字段 | 数据源 API | 提交写什么 | 副作用（双写/审计） |
|---|---|---|---|---|---|
| 系统管理 → 用户管理 → 行操作「分配位置」→ 对话框「分配归属位置」 | frontend/src/views/system/components/UserManagementTab.vue:729（按钮）/ :742（对话框）/ :748-762（下拉，`:value="opt.locationId"`） | **locationId** | `GET /v1/locations`（useLocationOptions.ts:38 → api/location.ts:28） | `PUT /v1/users/{id}/assign-store` body `{locationId}`（api/system/user.ts:198-199；controller/UserController.java:182-190） | 后端 UserServiceImpl.java:176,204 写 `users.location_id`；:254 **双写** `employees.location_id`（user→employee 正向同步）；无独立审计事件 |
| 用户管理 → 新建/编辑用户表单 | UserManagementTab.vue:788-916（表单区，**无位置字段**） | — | — | `POST/PUT /v1/users`（dto/UserCreateDTO.java:49 有 `locationId`，前端不填） | 创建时**不分配归属**；后端 DTO 支持但 UI 缺项 |
| 员工管理 → 新增/编辑员工 → 「工作归属」区 → **归属门店** | frontend/src/views/hr/components/EmployeeFormDialog.vue:474-477（`:value="String(store.storeId)"`） | **storeId** | `GET /v1/stores/active`（EmployeeFormDialog.vue:270-276 → api/store-ops/store-archive.ts:304） | `POST/PUT /v1/employees` body `storeId`（HREmployee.vue:252 → dto/EmployeeCreateDTO.java:36 / EmployeeUpdateDTO.java:36） | 后端 EmployeeServiceImpl.java:311/353 经桥写 `employees.location_id`；**无审计事件、无权限门槛**（002 卡 Q1 裁决要求的 `hr:employee:assign-location` / `EMPLOYEE_LOCATION_ASSIGN` 尚未存在） |
| 员工管理 → 「工作归属」区 → **归属仓库** | EmployeeFormDialog.vue:481-484（`:value="wh.warehouseId"`） | **warehouseId** | `GET /v1/warehouses/active`（EmployeeFormDialog.vue:271-276 → api/warehouse/warehouse.ts:216） | 同上 body `warehouseId`（HREmployee.vue:253 → dto/EmployeeCreateDTO.java:39） | 只写 `employees.warehouse_id`（EmployeeServiceImpl.java:312/354）；**`employees.location_id` 保持 NULL**（缺口①） |
| 员工管理 → 「工作归属」区 → **归属类型** | EmployeeFormDialog.vue:467-469（`workLocationType`：STORE/WAREHOUSE/HEADQUARTERS） | 类型判别 | 本地常量 | body `workLocationType` | 切换时清空另一侧字段（EmployeeFormDialog.vue:317-318）→ 归属互斥，但**落库空间随类型而变** |
| 财务 → 银行账户管理 → 表单「门店ID」 | frontend/src/views/finance/BankAccountManage.vue:418（**`el-input` 自由文本「请输入门店ID」**） | **storeId（字符串手输）** | **无数据源**（无下拉、无校验） | `POST /v1/finance/bank-accounts` body `storeId`（api/finance/bank-account.ts:75） | 后端 BankAccountServiceImpl.java:234,249 按 `users` 当前 storeId 校验/过滤 → 手输值可与真实归属不一致 |

### 2.2 门店空间（STORE）业务 UI 入口 —— 15 个

| UI 位置 | file:line | 字段 | 数据源 API | 提交写什么 | 副作用 |
|---|---|---|---|---|---|
| 设备 → 设备告警 → 筛选「门店」 | frontend/src/views/device/DeviceAlerts.vue:393-403 | storeId | `useStoreOptions` → `/v1/stores/active` | 查询参数（只读） | 无 |
| 设备 → 设备列表 → 筛选「门店」 | frontend/src/views/device/DeviceList.vue:535-546 | storeId | 同上 | 查询参数 | 无 |
| 设备 → 设备列表 → 新增/编辑表单「请选择所属门店」 | DeviceList.vue:719-720 | storeId | 同上 | `POST /v1/devices` body `storeId`（api/device/index.ts:232 → dto/DeviceCreateDTO.java:69） | 写 `devices.store_id`；HardwareConfigServiceImpl.java:95 有默认门店兜底 |
| 设备 → 状态历史 → 筛选「门店」 | frontend/src/views/device/DeviceStatusHistory.vue:301-313 | storeId | 同上 | 查询参数 | 无 |
| 运营 → 决策看板 → 门店切换器 | frontend/src/views/operations/DecisionBoard.vue:409-420 | storeId | 同上 | 查询参数（图表聚合） | 无 |
| 订单 → 订单查询 → 筛选「门店」 | frontend/src/views/order/OrderQuery.vue:498-510（含「全部门店」空值项） | storeId | 同上 | 查询参数 | 无 |
| 订单 → 预订管理 → 筛选「门店」 | frontend/src/views/order/OrderReservation.vue:576-587 | storeId | 同上 | 查询参数 | 无 |
| 订单 → 预订管理 → 新建预订表单「选择门店」 | OrderReservation.vue:772-777 | storeId | 同上 | `POST /v1/reservations` body `storeId`（api/order/reservation.ts:62） | 写订单表 store_id 空间 |
| 门店运营 → 排班管理 → 筛选「门店」 | frontend/src/views/store-ops/ShiftManagement.vue:525-537 | storeId | 同上 | 查询参数 | 无 |
| 门店运营 → 排班管理 → 新建排班表单「门店」 | ShiftManagement.vue:750-759 | storeId | 同上 | `POST /v1/schedule/plans` body `storeId`（api/schedule/plans.ts:93-94） | 写 `schedule_plans.store_id`；后端 SchedulePlanServiceImpl.java:150,264 从当前用户 storeId 反推 |
| 门店运营 → 日结 → 筛选「门店」 | frontend/src/views/store-ops/StoreDailySettlement.vue:337-349 | storeId | 同上 | 查询参数 | 无 |
| 门店运营 → 日结 → 新建日结表单「门店」 | StoreDailySettlement.vue:628-636 | storeId | 同上 | `POST /v1/store-management/settlements` body `storeId`（api/store-ops/settlement.ts:105-106） | 写 `daily_settlements.store_id`（DailySettlementServiceImpl.java:540） |
| 门店运营 → 门店库存 → 筛选「门店」 | frontend/src/views/store-ops/StoreInventory.vue:592-603 | storeId | 同上 | 查询参数 | 无 |
| 门店运营 → 排队历史 → 筛选「门店」 | frontend/src/views/store-ops/StoreQueueHistory.vue:318-329 | storeId | 同上 | 查询参数 | 无 |
| 门店运营 → 门店招聘 → 筛选「门店」 | frontend/src/views/store-ops/StoreRecruitment.vue:394-406 | storeId | 同上 | 查询参数 | 无 |

> **口径统一性**：这 15 个入口的选项数据源全部收敛到 **同一个 composable**（`useStoreOptions.ts` → `storeArchiveApi.getActiveStores()` → `/v1/stores/active`），彼此口径一致；但它们与身份层（`/v1/locations`）**不同口径**。

### 2.3 仓库空间（WH）业务 UI 入口 —— 15 个

| UI 位置 | file:line | 字段 | 数据源 API | 提交写什么 | 副作用 |
|---|---|---|---|---|---|
| 商品 → 库存管理 → 筛选「仓库」 | frontend/src/views/product/Inventory.vue:280-290 | warehouseId | `warehouseApi.getActiveList()`（Inventory.vue:119 → `/v1/warehouses/active`） | 查询参数 | 无 |
| 商品 → 库存管理 → 入库表单「请选择仓库」 | product/Inventory.vue:370-371 | warehouseId | 同上 | `inventoryApi.increase()`（Inventory.vue:234 → dto/InventoryIncreaseDTO.java:21 `warehouseId` **+ :24 `locationId`**） | **同一 DTO 双空间字段**；`warehouseId` 落到 `Inventory` 时命中 transient（entity/Inventory.java:126-128）→ 归属可能不落库 |
| 仓储 → 库存调整 → 筛选「仓库」 | frontend/src/views/warehouse/InventoryAdjust.vue:424-425 | warehouseId | `warehouseApi.getActiveList()` | 查询参数 | 无 |
| 仓储 → 库存调整 → 新建调整表单 | InventoryAdjust.vue:485-486 | warehouseId | 同上 | `POST /v1/inventory/adjustments`（api/warehouse/inventory-adjust.ts:298） | 写 `inventory_adjust.warehouse_id`；:341,356 派生增减流水 |
| 仓储 → 库存盘点 → 新建盘点表单 | frontend/src/views/warehouse/InventoryCheck.vue:699-700 | warehouseId | 同上 | `POST /v1/inventory-checks`（api/warehouse/inventory-check.ts:219） | 写 `inventory_checks.warehouse_id` |
| 仓储 → 库存盘点 → 盘点计划表单 | InventoryCheck.vue:787-788 | warehouseId | 同上 | 计划接口 | 写计划表 warehouse_id |
| 仓储 → 库存库位 → 筛选「所属仓库」 | frontend/src/views/warehouse/InventoryLocation.vue:354-355 | warehouseId | 同上 | 查询参数 | 无 |
| 仓储 → 库存库位 → 新建库位表单 | InventoryLocation.vue:416-417 | warehouseId | 同上 | `POST /v1/inventory-locations`（api/warehouse/inventory-location.ts:205） | **双写**：`inventory_locations` 同时有 `location_id` 与 `warehouse_id`（entity/InventoryLocation.java:21,26-27） |
| 仓储 → 库存损耗 → 筛选「仓库」 | frontend/src/views/warehouse/InventoryLoss.vue:271-272 | warehouseId | 同上 | 查询参数 | 无 |
| 仓储 → 库存损耗 → 新建损耗表单 | InventoryLoss.vue:316-317 | warehouseId | 同上 | `POST /v1/inventory/losses`（api/warehouse/inventory-loss.ts:177） | 写 `inventory_losses.warehouse_id`；:149 派生出库 |
| 仓储 → 库存出库 → 筛选「仓库」 | frontend/src/views/warehouse/InventoryOutbound.vue:477-478 | warehouseId | 同上 | 查询参数 | 无 |
| 仓储 → 库存出库 → 新建出库表单 | InventoryOutbound.vue:553-554 | warehouseId | 同上 | `POST /v1/inventory/outbounds`（api/warehouse/inventory-outbound.ts:262） | 写 `inventory_outbound.warehouse_id`；:293 派生扣减 |
| 仓储 → 库存预警 → 筛选「仓库」 | frontend/src/views/warehouse/InventoryWarning.vue:201-202 | warehouseId | 同上 | 查询参数 | 无 |
| 仓储 → 仓库库存（warehouse 版） → 筛选「仓库」 | frontend/src/views/warehouse/StoreInventory.vue:524-535 | warehouseId | 同上 | 查询参数 | 无 |
| 仓储 → 仓库总览 → 筛选「仓库」 | frontend/src/views/warehouse/WarehouseOverview.vue:667-678 | warehouseId | 同上 | 查询参数 | 无 |

> **口径统一性**：仓库下拉**没有统一 composable**（`frontend/src/composables/` 只有 `useStoreOptions.ts` 与 `useLocationOptions.ts`），15 个入口分布在 9 个文件里各自 `warehouseApi.getActiveList()` 自行加载（如 InventoryAdjust.vue:38、InventoryCheck.vue:40、InventoryLocation.vue:41、InventoryLoss.vue:33、InventoryOutbound.vue:41、InventoryWarning.vue:35、warehouse/StoreInventory.vue:145、WarehouseOverview.vue:66、product/Inventory.vue:19）。口径一致但**无收敛点**，未来收编需逐点改。

---

## 3. 同一字段的 UI 入口数量与口径一致性（问题 1）

| 字段 | UI 入口数 | 口径 |
|---|---|---|
| **归属身份**（人 → 位置） | **2 套互不相通**：用户管理 1 个（`/v1/locations`，LOC）；员工管理 3 个（归属类型 + 归属门店 `/v1/stores/active` + 归属仓库 `/v1/warehouses/active`，STORE/WH） | **不一致**：同名概念（「归属」）在两个页面走两个不同 ID 空间、两个不同 API、两套不同 DTO 字段名 |
| **storeId**（业务归属） | **15 个**（§2.2）+ 1 个自由文本输入（BankAccountManage.vue:418） | **内部一致**（全部经 `useStoreOptions` → `/v1/stores/active`），**外部不一致**（与身份层 LOC 不同空间）；自由文本输入是口径破口 |
| **warehouseId** | **15 个**（§2.3）+ 员工管理 1 个（EmployeeFormDialog.vue:481-484） | **内部一致**（全部 `/v1/warehouses/active`，但无 composable）；与 `inventory.location_id` 已迁 LOC 的事实冲突 |
| **locationId** | **1 个**（UserManagementTab.vue:748-762） | 唯一按新空间设计的入口；`useLocationOptions` 全库仅此 1 个消费者 |

**结论**：`locationId` 只有 1 个 UI 入口，`storeId` 有 16 个，`warehouseId` 有 16 个（合计 33 个字段绑定入口 + 3 个补录行 = 36 行）。UI 层的空间分布与后端「身份已迁 LOC、业务仍 STORE/WH」的迁移状态**完全镜像**，即迁移只完成了 36 个入口中的 1 个。

---

## 4. 仍使用旧空间（stores_new / warehouses）而系统身份已迁 locations 的位置（问题 2）

### 4.1 数据库层（Flyway 211 个迁移解析所得）

- **store_id 列存在于 43 张表**，扣除已被 `V20260930_002__user_location_001_rename_store_id_to_location_id.sql` 删除的 `users`、`employees` → **实际 41 张**：`asset_masters_enhanced, bank_accounts, call_number_queues, call_number_records, compliance_reports, cost_record, daily_settlements, device_data, device_status_history, device_template, devices, employee_approvals, finance_invoice, finance_records, finance_voucher, invitation_codes, job_offers, kitchen_order, onboarding_records, open_close_store_records, print_templates, purchase_arrivals, purchase_orders, receipt_confirmations, recruitment_quotas, recruitment_requirements, salary_records, schedule_plans, schedule_rules, schedule_shift_types, schedule_templates, shift_records, store_announcements, store_inventory, store_inventory_log, stores, stores_new, tray, tray_scan_record`（+ `role_stores`、`sys_stores`、`sys_user_stores` 见 §4.4）
- **location_id 列存在于 7 张表**：`locations, location_id_map, inventory_locations, purchase_stockin_items, inventory, users, employees`
- **warehouse_id 列存在于 17 张表**：`food_trace_codes, inventory_adjust, inventory_checks, inventory_locations, inventory_log, inventory_losses, inventory_outbound, inventory_transactions, inventory_warning, loss_outbound, other_inbound, purchase_arrivals, purchase_returns, purchase_stockins, receipt_confirmations, warehouses`（+ `employees.warehouse_id`，V20260721_001:18）

### 4.2 后端仍按旧空间读写的代码（代表点，全量见 §1.3/§1.4）

- 数据权限切面直接按 `store_id` 列过滤：aspect/DataPermissionAspect.java:140,164
- 15 处身份消费仍取 storeId：见 §1.1 末行清单
- 24 处业务写入写 store_id：见 §1.4
- 13 个 XML mapper 的 store_id 过滤：见 §1.3

### 4.3 旧空间与已迁空间**同时存在**的表/实体（真正的双空间点）

| 位置 | file:line | 说明 |
|---|---|---|
| `employees` | entity/Employee.java:107-110（location_id）+ :129-131（warehouse_id）；migration V20260721_001:18 | 同一行归属分裂到两个空间，由 `work_location_type` 判别 |
| `inventory_locations` | entity/InventoryLocation.java:21,26-27 | 同表同时有 location_id 与 warehouse_id |
| `InventoryIncreaseDTO` | dto/InventoryIncreaseDTO.java:21,24 | 同一入参 DTO 同时有 warehouseId 与 locationId |
| `Inventory` 实体 | entity/Inventory.java:27-28（真列 location_id）+ :122-128（transient storeId/warehouseId） | 迁移后保留的过渡字段，注释已声明「S4 收编后移除」但仍有活跃调用点（§1.4 三条） |
| `role_stores` | entity/RoleStore.java:16；mapper/RoleStoreMapper.java:24,32,47,58；mapper/RoleStoreMapper.xml:7,10 | 权限模型仍按 store_id 分配 |

### 4.4 无 Flyway 定义的旧空间表（schema 漂移，PG-003 相关）

| 表 | 代码引用 | Flyway 命中 |
|---|---|---|
| `role_stores` | RoleStore.java:16；RoleStoreMapper.java:24,32,47,58；RoleStoreMapper.xml:7,10 | **0 个迁移** |
| `sys_stores` / `sys_user_stores` | mapper/SysStoreMapper.java:21,29 | **0 个迁移**；且 `SysStoreMapper` / `selectStoresByUserId` / `selectDefaultStoreByUserId` 全库**无调用点 → 死代码**（第三套用户→门店机制残留） |
| `inventory_movement` | resources/mapper/InventoryLogMapper.xml:22（`FROM inventory_movement`）；resources/mapper/StoreInventoryLogMapper.xml:20 | **0 个迁移**，但它是库存流水读模型的实际来源表 |

---

## 5. 范围外客户端登记（单列一节，不纳入整改口径）

| 客户端 | 命中文件数 | 命中行数 | 最近提交 | 性质 | 代表点 |
|---|---|---|---|---|---|
| `frontend-design-demo` | 2 | 7 | 无 git 记录 | 演示/静态 | src/demos/demo4-permission/PermissionPage.vue:50,183,298（`storeId` 作「门店编码」展示，注释自述「用户以 store_id 直绑（DSN-08）」） |
| `frontend-pos` | 3 | 7 | 2026-09-27 | **真实 API 客户端** | src/api/kitchen.ts:60,137,140-141（`/v1/kitchen/orders/statistics/{storeId}`）；src/api/posApi.ts:559-562；src/types/kitchen.ts:62 |
| `frontend-kitchen` | 1 | 11 | 2026-09-27 | **真实 API 客户端** | src/api/kitchenOrderApi.ts:13,46,65,71-72,75-76,124-125,136-137（`/v1/kitchen/pending-orders?storeId`、`/v1/kitchen-order/store-active/{storeId}` 等 6 个端点按 storeId 路径/查询） |
| `employee-frontend` | 8 | 22 | 2026-08-15 | 混合（含 mock） | src/views/TaskPublishPage.vue:129,151,159-170,213,231,254（`storeId: 'store-001'` 字符串假 ID）；src/views/TaskPublishPage/components/TaskAssigneeSection.vue:16 |
| `frontend-design-v3` | 1 | 3 | — | mock 数据 | src/data/inventory.js:54,55,80 |
| `frontend-design-v2` / `frontend-design-v4` / `frontend-demo-clean` / `miniprogram` | 0 | 0 | — | 无命中 | — |

> **风险提示（仅登记）**：`frontend-pos` 与 `frontend-kitchen` 是**会真实打到后端 store_id 端点**的客户端。若后端做 store→location 收编，这两个客户端不在本次扫描主范围内，属于**未登记的下游断点**。

---

## 6. 已知不一致 vs 新发现不一致（问题 3）

### 6.1 已知不一致（此前卡片已登记，本次复核确认，未新增结论）

| 编号 | 内容 | 复核证据 |
|---|---|---|
| K1 | 员工管理前端仍用旧两套空间（`/v1/stores/active` + `/v1/warehouses/active`），用户管理已迁 `/v1/locations` | §2.1 各行；commit `500c154`（用户侧）；EmployeeFormDialog.vue:270-276 |
| K2 | 员工选「仓库」不落 `employees.location_id`；后端只从 storeId 推导 | EmployeeServiceImpl.java:311,353（仅 `locationIdOfStore`）；:312,354（warehouseId 直存）；无 warehouse→location 正向桥入口 |
| K3 | 无 employee→user 反向同步（只有 user→employee 正向） | UserServiceImpl.java:254 存在；反向无对应写入点 |
| K4 | `role_stores.store_id` 属旧空间且无 Flyway 定义 | §4.4 |
| K5 | `sys_stores` / `sys_user_stores` 是第三套旧机制且为死代码 | §4.4 |
| K6 | 改名不是机械改名而是 ID 空间桥接；S2b（DTO/API 字段改名 + 前端接线）被推迟 | p1-user-location-001-implementation-record-001.md §7.2/§7.3；本次 §1.2 契约表证实 S2b 仅对 users 完成 |

### 6.2 新发现的不一致（本次扫描首次登记，未在任何卡片中出现）

| 编号 | 内容 | 证据 | 严重度提示 |
|---|---|---|---|
| **N1** | `InventoryLogMapper.xml` 把 **LOC 列起成 WH 名**：`m.location_id AS warehouse_id`，并用 `AND m.location_id = #{warehouseId}` 过滤 —— 前端传 warehouseId，后端拿去比 location_id，**无桥** | resources/mapper/InventoryLogMapper.xml:9,26-27 | 跨空间直接比较，违反 design-002 §7 规则 3/4 |
| **N2** | `StoreInventoryLogMapper.xml` 把 **location_code（字符串）起成 store_id** 并用作过滤条件 —— 引入「第四空间」CODE | resources/mapper/StoreInventoryLogMapper.xml:10,23-24 | 前端 storeId 语义在此变成字符串编码 |
| **N3** | `Inventory` 的 `transient storeId/warehouseId` 已声明「禁止当查询条件、S4 后移除」，但仍有 **3 处活跃调用**：查询条件、写入、读回 | entity/Inventory.java:122-128；ProcurementTraceServiceImpl.java:443；MaterialArchiveServiceImpl.java:388；config/InventoryWarningScheduler.java:188 | 后两处是**静默数据丢失**（写不进 / 读出恒 null） |
| **N4** | 硬编码兜底 ID：默认仓库 `1L`、默认门店常量 | PurchaseArrivalServiceImpl.java:656；HardwareConfigServiceImpl.java:95（`DeviceConstants.DEFAULT_STORE_ID`） | 兜底会造出「归属到不存在/默认位置」的数据 |
| **N5** | 银行账户「门店ID」是**自由文本输入**，无下拉、无校验，而服务端按当前用户 storeId 做权限过滤 | BankAccountManage.vue:418；api/finance/bank-account.ts:75；BankAccountServiceImpl.java:234,249 | 唯一无数据源的归属入口 |
| **N6** | 员工表单 `warehouseId` 被前端类型化为 **string**，后端 DTO 为 `Long` | EmployeeFormDialog.vue:102（`{warehouseId: string …}`）、:34-35；dto/EmployeeCreateDTO.java:39 | 类型口径不一致（storeId 同样 `String(store.storeId)`，:476） |
| **N7** | 用户创建/编辑表单**不提供归属字段**，尽管 DTO 支持 | UserManagementTab.vue:788-916；dto/UserCreateDTO.java:49 | 新用户必须二次操作才有归属 |
| **N8** | `UserInfoResponse.java:33` / `UserBasicInfo.java:34` 等**出参契约仍返回 storeId**（经桥换算得出），前端 auth 层消费 | dto/UserInfoResponse.java:33；api/auth.ts | 前端仍能看到 storeId，助长旧空间复用 |
| **N9** | `inventory_movement` 是库存流水读模型的实际表，但**无任何 Flyway 定义** | InventoryLogMapper.xml:22；StoreInventoryLogMapper.xml:20 | 与 role_stores/sys_stores 同属 schema 漂移（PG-003） |
| **N10** | 仓库下拉无统一 composable（15 个入口分布在 9 个文件各自加载），门店下拉有（15 处收敛） | composables/ 目录仅 useStoreOptions.ts + useLocationOptions.ts；§2.3 各行 | 收编成本不对称 |
| **N11** | `frontend-pos` / `frontend-kitchen` 两个真实客户端按 storeId 打后端端点，不在主扫描范围 | §5 | 后端收编时的未登记下游 |
| **N12** | 身份访问器使用比例失衡：旧访问器 15 处调用，新访问器 1 处 | §1.1 | 「已迁」在消费层尚未成立 |

### 6.3 方法学注记（为什么这类不一致容易漏）

1. **纯 token 扫描会漏 UI 入口**：用户管理的位置下拉绑定名是 `assignLocationId`（不含 `locationId` token 的 `v-model` 形式在 `:748` 才命中），选项 `:value="opt.locationId"` 在 `:760`。若只 grep `store_id|location_id` 会完全漏掉这个**唯一的新空间入口**。
2. **入口收敛度决定风险**：门店有 `useStoreOptions` 单点，仓库没有；因此「改一处」只对门店成立，对仓库要改 13 处。
3. **DTO 字段名是空间口径的真正载体**：`UserAssignStoreDTO.locationId` vs `EmployeeCreateDTO.storeId` —— 两个「分配归属」接口名字都含 store，但字段空间不同（UserController.java:182-190 与 EmployeeController.java:150,163）。
4. **transient 兼容字段是隐形漏洞源**：编译通过、运行不报错、数据静默丢失（N3）。

---

## 7. 复现方法与原始数据

- 分类脚本：`tmp/scan001/scan.py`（token 正则 `\b(location_id|locationId|store_id|storeId|warehouse_id|warehouseId)\b`，跳过注释行，标签 BRIDGE / GUARD / IDENTITY / DDL-MAP / DECLARE / WRITE / READ / SQL / FILTER / UI-BIND / LOG）
- 分类结果：`tmp/scan001/cls_backend-java.tsv`（2164 行 / 435 文件）、`tmp/scan001/cls_backend-xml.tsv`（54 行 / 22 文件）、`tmp/scan001/cls_frontend.tsv`（658 行 / 127 文件）
- UI 上下文抽取：`tmp/scan001/ui_ctx.py` → `tmp/scan001/ui_ctx.txt`（33 个 UI-BIND 的绑定行 + 紧随的 `el-option/:key/:value` + 选项源赋值）
- API 映射：`tmp/scan001/ui_api.py` → `tmp/scan001/ui_api.txt`（每个 UI 文件的 `@/api` import 与 `xxxOptions.value =` 赋值点）
- Schema 真值：解析 `backend/src/main/resources/db/migration/` 全部 211 个迁移得到的列→表映射（注意：`V20260930_002` 用 ADD+DROP 而非 RENAME，朴素解析会把 users/employees 误列入 store_id，已人工修正）
- `tmp/` 已被 `.gitignore:110` 忽略，不进入版本库。

---

## 8. 边界与后续（不在本卡执行）

- 本卡**未修改任何代码**，未修复任何上表不一致，仅登记。
- 建议（不执行）：表 2 的 36 行入口应作为 P1-USER-LOCATION-002 阶段 2 的**收编清单基线**；§2.1 身份类 6 行是 002 卡缺口①②的直接落点；§6.2 的 N1/N2/N3/N9 属新缺口，需 Owner 判定是否并入 002 或另开卡。
- 后续任何卡做字段改动前，应先引用本文件对应行，避免「每卡各自 grep、结果不归档」的复发（p1-user-location-001-implementation-record-001.md §7 记录的根因）。

---

## 9. Owner 分流裁决落地（2026-09-30，关闭 §8 的待判定项）

Owner 收下本清单并明确「**不再普查**」，指定本文件为 **P1-USER-LOCATION-002 阶段 2 的输入**；§6.2 的 12 项新发现据此定档分流（完整登记见任务板 §24.3o）：

| 条目 | 去向 |
|---|---|
| K1–K3、缺口①②③、**N3、N4（仅非冻结一处）、N6、N7、N8、N12** | **P1-USER-LOCATION-002**（任务板 §24.3m）——阶段 2 设计已产出：[../design/p1-user-location-002-design-001.md](../design/p1-user-location-002-design-001.md) |
| **N11** | 已另开**轻档**扫描卡并产出：[p1-pos-kitchen-storeid-downstream-scan-001.md](p1-pos-kitchen-storeid-downstream-scan-001.md)（任务板 §24.3n）→ **§5 的「范围外客户端登记 + 风险提示」段落由该卡接管**，后续引用应指向它 |
| **N1 / N2** | 库存收编卡（M5 / P1-LOCATION-MODEL-001 M5-M7） |
| **N5** | 财务域卡（随 P1-FIN-* 批次） |
| **N9** | **P0-FLYWAY-COVERAGE-002**（任务板 §24.3b，68 张黑箱表 → 新增第 69 张候选） |
| **N10** | 员工侧由 002 设计 D2 消化；其余 8 文件的仓库下拉不卡 |
| **§4.4**（`role_stores` / `sys_stores` / `sys_user_stores`） | **P0-ROLE-STORE-SCHEMA-001**（任务板 §24.3c） |
| **§4.1**（41 张 `store_id` 表） | **P1-LOCATION-MODEL-001 M5-M7**（任务板 §24.3g） |

> 本卡判档 = 轻档，故 §9 只是**把 Owner 裁决写进数据源文档**（避免裁决只存在于会话里），未新增扫描结论、未改任何数字口径（36 行入口 / 4 个 ID 空间 / K1–K6 / N1–N12 均不变）。
