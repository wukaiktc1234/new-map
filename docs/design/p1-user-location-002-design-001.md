# P1-USER-LOCATION-002 阶段 2 设计（方案，不改代码）

> **卡**：P1-USER-LOCATION-002（任务板 [production-remediation-task-board.md](../../production-remediation-task-board.md) **§24.3m**；编号更正：原登记 §24.3j 与 P1-INVENTORY-LEGACY-WRITEPATH-001 撞号）
> **判档**：**重档**（Owner 2026-09-30）
> **阶段**：**阶段 2 = 只出方案**（PG-005 三阶段）。本文件 = 设计文档 + 改动清单 + 回滚方案 + 验收基准定稿。**全程未改任何代码 / DB / 配置。**
> **上游**：阶段 1 诊断 [docs/quality/p1-user-location-002-diagnosis-001.md](p1-user-location-002-diagnosis-001.md)（含 Owner 四问四答裁决）
> **输入**：[docs/quality/field-entry-inventory-001.md](field-entry-inventory-001.md)（字段入口清单，Owner 指定为阶段 2 输入）
> **交接条件**：**Owner 拍板本设计 → 才进入阶段 3 实施**。阶段 2 内不得提前实施、不得顺手修。

---

## 0. 范围（Owner 2026-09-30 收窄，逐字对齐）

### 0.1 在范围内

| 组 | 条目 | 来源 |
|---|---|---|
| 已知不一致 | **K1 / K2 / K3** | inventory §6.1 |
| 002 三缺口 | **缺口①**（员工选仓库 `location_id` 不落库）/ **缺口②**（员工前端仍用旧两套空间）/ **缺口③**（无 employee→user 反向同步） | 诊断 §2/§3/§4 |
| 新发现 | **N3**（`Inventory` transient 字段违规用法 3 处）/ **N4**（硬编码兜底）/ **N6**（前端类型 string vs 后端 Long）/ **N7**（用户表单无归属字段）/ **N8 + N12**（出参契约仍返回 storeId；旧访问器 15 处 vs 新访问器 1 处，**同源，合并处理**） | inventory §6.2 |

### 0.2 明确不在范围内（已分流，不得在 002 内顺手做）

| 条目 | 去向 | 理由 |
|---|---|---|
| **N1 / N2**（`InventoryLogMapper.xml` 跨空间比较、`StoreInventoryLogMapper.xml` 把 location_code 当 store_id） | 库存收编卡（M5 或 P1-LOCATION-MODEL-001 M5-M7） | 属库存流水读模型收编，与用户归属不同域 |
| **N5**（银行账户「门店ID」自由文本） | 财务域卡 | 唯一无数据源入口，需财务侧数据源设计 |
| **N9**（`inventory_movement` 无 Flyway 定义） | **P0-FLYWAY-COVERAGE-002**（任务板 §24.3b，68 张黑箱表 / KL-082） | 属 schema 漂移（PG-003），非本卡 |
| **§4.4**（`role_stores` / `sys_stores` / `sys_user_stores`） | **P0-ROLE-STORE-SCHEMA-001**（任务板 §24.3c） | 已有卡覆盖 |
| **§4.1 的 41 张 `store_id` 表** | **P1-LOCATION-MODEL-001 M5-M7**（任务板 §24.3g） | 表级收编是另一条主线 |
| **N11**（POS / KDS 客户端下游断点） | 已另开轻档扫描卡并产出 [p1-pos-kitchen-storeid-downstream-scan-001.md](p1-pos-kitchen-storeid-downstream-scan-001.md) | Owner 明确「独立不阻塞 002」 |
| **KL-085 / KL-086 冻结项**（KdsPrincipal、设备域 `DEFAULT_STORE_ID`） | 维持冻结 | 见 §2 D5 与 §7 |

---

## 1. 设计不变量（所有决策不得违反）

| # | 不变量 | 出处 |
|---|---|---|
| I-1 | `location_id` 是唯一外键；旧空间只作别名/兼容 | design-002 §7 规则 1 |
| I-2 | 跨 ID 空间换算**必须**经 `location_id_map`（桥），严禁假设数值相等 | design-002 §7 规则 3/4 |
| I-3 | `null` = **显式拒绝**（403/400 或空结果），**禁止数值兜底** | [SecurityUtils.java:93-94](../../backend/src/main/java/com/foodtraceability/utils/SecurityUtils.java#L93)、[LocationIdBridge.java:38-40](../../backend/src/main/java/com/foodtraceability/common/util/LocationIdBridge.java#L38) |
| I-4 | 无 schema 变更优先；确需 DB 变更只允许 **seed 迁移**（Flyway 单一来源） | PG-003 |
| I-5 | 带既有无关 WIP 的文件须 **hunk 级隔离**（残留 diff 逐字等于原 WIP） | PG-001，诊断 §6 |
| I-6 | 冻结项（KL-085 / KL-086）不在本卡解冻 | KL 表 |

---

## 2. 设计决策（D1–D9）

### D1 —— 缺口①：给桥补「正向 warehouse→location」入口，员工归属改由 `locationId` 驱动

**问题**：[EmployeeServiceImpl.java:311](../../backend/src/main/java/com/foodtraceability/service/impl/EmployeeServiceImpl.java#L311)（create）/ :353（update）只做 `locationIdOfStore(dto.getStoreId())`；选仓库时 `storeId=null` → 桥返回 null → `employees.location_id` 恒 NULL。桥侧只有反向 `toWarehouseId`（:54-59），**没有正向入口**。

**方案**：

1. **桥补正向入口**（[LocationIdBridge.java](../../backend/src/main/java/com/foodtraceability/common/util/LocationIdBridge.java)）
   - 新增实例方法 `Long toLocationId(String srcTable, Long srcId)`（泛化，内部走 `locationIdMapMapper.selectLocationIdBySrcId`）；
   - 现有 `toLocationId(Long storeId)`（:46）保留为 `toLocationId(SRC_STORES_NEW, storeId)` 的委托，**不改签名**（零调用点冲击）；
   - 新增静态入口 `static Long locationIdOfWarehouse(Long warehouseId)`，与 `locationIdOfStore`（:68）对称，非 Spring 上下文返回 null。
2. **DTO 增 `locationId` 字段**（[EmployeeCreateDTO.java](../../backend/src/main/java/com/foodtraceability/dto/EmployeeCreateDTO.java)、[EmployeeUpdateDTO.java](../../backend/src/main/java/com/foodtraceability/dto/EmployeeUpdateDTO.java)）：`storeId`（:36）/ `warehouseId`（:39）/ `workLocationType`（:33）**保留但标 `@Deprecated`**（观察期兼容，前端不再发送）。
3. **服务层归属解析优先级**（EmployeeServiceImpl 抽一个私有方法 `resolveLocationId(dto)`）：
   `dto.locationId` 非空 → 直接用（校验存在于 `locations`）；
   否则 `dto.storeId` 非空 → `locationIdOfStore`；
   否则 `dto.warehouseId` 非空 → `locationIdOfWarehouse`（**新路径**）；
   全空 → `null`（不落库，不兜底）。
4. **`workLocationType` 不再作为写入依据**，改为从 `locations.location_type` 推导（与 Owner Q2 裁决一致）。

**数据前提（阶段 2 已核实）**：`location_id_map` **已含 warehouses 映射行**——[V20260927_001:66-77](../../backend/src/main/resources/db/migration/V20260927_001__create_locations_and_location_id_map.sql#L66) 把真实仓写入 `locations` 并登记 `src_table='warehouses'`。**但 Owner「全清」决策排除了 8 个 E2E/TEST 仓库**（同文件 :6 注释）→ 这 8 个仓的 `warehouseId` 经新桥**返回 null** → 按 I-3 显式拒绝（见 §7 未决 U4）。

**取舍**：不新增 `employees` 列、不加迁移（`employees.location_id` 已由 [V20260930_002:3](../../backend/src/main/resources/db/migration/V20260930_002__user_location_001_rename_store_id_to_location_id.sql#L3) 从 `employees.store_id` 改名而来，可空）。

**影响文件**：`LocationIdBridge.java`、`EmployeeCreateDTO.java`、`EmployeeUpdateDTO.java`、`EmployeeServiceImpl.java`。

---

### D2 —— 缺口②：员工表单合并成**一个位置下拉**（Owner Q2 裁决落地）

**现状**：[EmployeeFormDialog.vue:271-273](../../frontend/src/views/hr/components/EmployeeFormDialog.vue#L271) 拉 `/v1/stores/active` + `/v1/warehouses`（旧两套空间）；:467-484 是「归属类型」+ 条件显示的两个下拉；选项类型混用（:476 `String(store.storeId)`、:35 `warehouseOptions: {warehouseId: string}`）。

**方案**：

1. 数据源换成 **`GET /v1/locations`**（[LocationController.java:32](../../backend/src/main/java/com/foodtraceability/controller/LocationController.java#L32)，已支持 `locationType` 过滤，返回 `locationId / locationCode / locationName / locationType / storageType`）——复用已有 composable `useLocationOptions`（inventory §6.3 记录：门店侧已收敛、仓库侧无统一 composable，本决策顺带把员工侧并入 locations 面）。
2. **一个下拉 + 类型标签**：option label = `locationName（locationType 中文标签）`，`:value="opt.locationId"`；「归属类型」从表单控件降级为**只读展示**，值由所选 `location.locationType` 推导（不再让用户先选类型再选实体）。
3. 提交载荷改 `locationId`（`api/hr/employee.ts` 同步）；`storeId` / `warehouseId` 停止发送（后端字段保留，见 D1-2）。
4. **N6 在本决策内自然消解**：`{storeId: string}` / `{warehouseId: string}`（:34-35）随两个旧下拉一起删除，新字段 `locationId?: number` 与后端 `Long` 对齐。

**不做**：不删 `/v1/stores/active`、`/v1/warehouses` 端点（其他消费方未清点，属 §0.2 分流范围）。

**WIP 约束**：`EmployeeFormDialog.vue` 带既有无关 WIP（诊断 §6）→ 阶段 3 须 hunk 级隔离。

**影响文件**：`EmployeeFormDialog.vue`、`api/hr/employee.ts`。

---

### D3 —— 缺口③：employee → user 反向同步（含权限门槛与审计）

**现状**：只有 user→employee 单向双写（[UserServiceImpl.java:168-258](../../backend/src/main/java/com/foodtraceability/service/impl/UserServiceImpl.java#L168) `syncEmployeeAssignment`，关联键 `employee_code`，顺带补 `employees.user_id`）；[EmployeeServiceImpl.java:334-365](../../backend/src/main/java/com/foodtraceability/service/impl/EmployeeServiceImpl.java#L334) 全程不触碰 `users`。

**方案**：

1. **新增反向同步方法**（落在 `EmployeeServiceImpl`，命名 `syncUserAssignmentFromEmployee`），在 `updateEmployee` 归属发生变化时调用：
   - 关联键沿用既有惯例 **`employees.user_id`（已由 V20260930_002 :21 建列并回填）优先，`employee_code` 兜底**；
   - 写入 `users.location_id = 解析出的 locationId`；
   - **不创建用户**：找不到对应用户 → 只写审计、不动 `users`（避免员工档案造账号）。
2. **防回写循环**：正向 sync 与反向 sync **各自只写对向表一次**，不在被调用方法内再触发对向同步；实现上以「入口决定方向」为硬约束（用户入口→正向；员工入口→反向），并在两处方法注释互指。
3. **权限门槛**（Owner Q1 约束①③）：`EmployeeController` 的归属变更须加
   `@PreAuthorize("hasAuthority('hr:employee:assign-location') or hasAuthority('*')")` + `@RequiresPermission(value="hr:employee:assign-location", action="assign-location")`，样板 = [UserController.java:184-186](../../backend/src/main/java/com/foodtraceability/controller/UserController.java#L184)。
   现状对照：[EmployeeController.java:164](../../backend/src/main/java/com/foodtraceability/controller/EmployeeController.java#L164) 仅 `hasAnyRole('admin','hr')`。
   **拆分要求**：`PUT /v1/employees/{id}` 现同时承载「改档案」与「改归属」→ 阶段 3 须把归属变更拆为**独立端点** `PUT /v1/employees/{id}/assign-location`（与用户侧 `PUT /v1/users/{id}/assign-store` 对称），使权限门槛只作用于归属，不收紧普通档案编辑。
4. **审计**（Owner Q1 约束②）：`@OperationLog(module = "EMPLOYEE_LOCATION_ASSIGN", type = UPDATE, desc = "员工归属变更（含反向同步 users.location_id）")`；审计 desc 须含**旧值 → 新值**（现 `USER_STORE_ASSIGN` 审计未记旧值，本卡补齐员工侧）。
5. **权限码发放面（5 面，实测 grep 得出）**：
   `EmployeeController.java`（注解）、[AdminPermissions.java:98](../../backend/src/main/java/com/foodtraceability/security/constants/AdminPermissions.java#L98) 附近、[db/init-data.sql:100-103](../../backend/src/main/resources/db/init-data.sql#L100) 附近、[frontend/src/utils/permissions.ts:369-372](../../frontend/src/utils/permissions.ts#L369)、**新增 Flyway seed 迁移**（见 §4）。
   ⚠ 命名口径事实：前端注册表用 `hr:employee:edit`，DB seed 用 `hr:employee:update`（同一动作两套码名）→ 新码 `hr:employee:assign-location` 须在 5 面**逐字一致**，不沿用 edit/update 分歧。

**影响文件**：`EmployeeServiceImpl.java`、`EmployeeController.java`、`AdminPermissions.java`、`db/init-data.sql`、`frontend/src/utils/permissions.ts`、新 seed 迁移。

---

### D4 —— N3：清除 `Inventory` transient 字段的 3 处违规用法

事实核对（阶段 2 只读核实）：`inventory` 表**同时存在** `warehouse_id` / `store_id`（[V1.0.0.100:425-431](../../backend/src/main/resources/db/migration/V1.0.0.100__init_postgresql.sql#L425)）与 `location_id`（[V20260816_001:13](../../backend/src/main/resources/db/migration/V20260816_001__align_inventory_material_columns.sql#L13)）；实体 [Inventory.java:28](../../backend/src/main/java/com/foodtraceability/entity/Inventory.java#L28) 映射 `locationId`，[ :122-128](../../backend/src/main/java/com/foodtraceability/entity/Inventory.java#L122) 的 `storeId` / `warehouseId` 是 `@Deprecated transient`（不落映射）。

| 站点 | 现状 | 方案 |
|---|---|---|
| [ProcurementTraceServiceImpl.java:443](../../backend/src/main/java/com/foodtraceability/service/impl/ProcurementTraceServiceImpl.java#L443) `wrapper.eq(Inventory::getWarehouseId, warehouseId)` | 用 transient 字段当查询条件 → MyBatis-Plus 要么解析不到列（运行期异常）要么打到**已停写的旧列**（恒空结果） | 改 `wrapper.eq(Inventory::getLocationId, resolveLocationIdOfWarehouse(warehouseId))`；调用方入参 `warehouseId` 保留（API 不变），换算在服务层 |
| [MaterialArchiveServiceImpl.java:388](../../backend/src/main/java/com/foodtraceability/service/impl/MaterialArchiveServiceImpl.java#L388) `inventory.setWarehouseId(warehouseId)` | 写 transient → **新建库存记录 `location_id` 恒空**（静默丢失） | 改 `inventory.setLocationId(bridge.locationIdOfWarehouse(warehouseId))`；解析为 null → 按 I-3 拒绝建档并给业务提示 |
| [InventoryWarningScheduler.java:188](../../backend/src/main/java/com/foodtraceability/config/InventoryWarningScheduler.java#L188) `record.setWarehouseId(inventory.getWarehouseId())` | 读 transient → `inventory_warning_records.warehouse_id`（真实列，实体 :39）**恒 NULL** | 改 `record.setWarehouseId(bridge.toWarehouseId(inventory.getLocationId()))`（保持该列语义不变，不加新列） |

**阶段 3 前置核实项**：MyBatis-Plus 对 `transient` 字段的实际处理（异常 vs 打到旧列）——用一条最小查询实测后再定第 1 行的错误语义描述。

**不做**：不删 `Inventory` 的 transient 字段（仍有编译依赖），不删 `inventory.warehouse_id` / `store_id` 列（属 §0.2 表级收编）。

---

### D5 —— N4：只处理**非冻结**的那一处兜底

| 站点 | 处置 | 理由 |
|---|---|---|
| [PurchaseArrivalServiceImpl.java:656](../../backend/src/main/java/com/foodtraceability/service/purchase/impl/PurchaseArrivalServiceImpl.java#L656) `key.warehouseId != null ? key.warehouseId : 1L` | **纳入 002**：去掉 `1L`，未指定收货仓库 → 显式校验失败（业务异常，提示「未指定收货仓库」）；若上游只给门店，则经桥 `locationIdOfStore` 推导 | 纯业务兜底，无 WIP 依赖，改动局部 |
| [HardwareConfigServiceImpl.java:95](../../backend/src/main/java/com/foodtraceability/service/impl/HardwareConfigServiceImpl.java#L95)（及 :138）、`DeviceStatusServiceImpl` 6 处 `DeviceConstants.DEFAULT_STORE_ID` | **不纳入 002**，维持 **KL-086 冻结** | 解冻前置是 `devices.location_id` 迁移 + 设备注册链（`DeviceWhitelistFilter` / `AppDeviceRegistration*`）**未跟踪 WIP 入库**（KL-086 原文）；本卡动它 = 撞冻结项（违反 I-6） |

**登记**：`DEFAULT_STORE_ID` 实测使用点 = 8 行（`DeviceStatusServiceImpl` 183/184/190/195/198/233 + `HardwareConfigServiceImpl` 95/138），KL-086 记「5 处」为**低估**；本卡只登记计数差异，不改 KL-086 结论（重档阶段 2 不修 KL）。

---

### D6 —— N7：用户创建/编辑表单补归属入口

**现状**：[UserManagementTab.vue:788-916](../../frontend/src/views/system/components/UserManagementTab.vue#L788) 的创建/编辑表单**没有归属字段**，尽管 [UserCreateDTO.java:49](../../backend/src/main/java/com/foodtraceability/dto/UserCreateDTO.java#L49) 支持；归属只能事后经「分配位置」（KL-087 已解除的入口）。

**方案**：创建表单加**可选**位置下拉（复用 `useLocationOptions`，与 D2 同一数据源）；编辑仍走既有 `PUT /v1/users/{id}/assign-store` 入口（保持权限门 + 审计，不新开编辑内联改归属）。

**取舍**：创建时给归属 = 少一次二次操作；不给 = 新用户登录即 `NoLocationContextException`（POS 下单直接失败，见 N11 扫描卡 A1）。**推荐纳入**。

**WIP 约束**：`UserManagementTab.vue` 带既有无关 WIP → hunk 级隔离。

---

### D7 —— N8：出参契约「加新留旧 + 标废弃」，不删

**现状**：[UserInfoResponse.java:33](../../backend/src/main/java/com/foodtraceability/dto/UserInfoResponse.java#L33) `storeId`（经桥换算得出，另带 `storeName`）、[UserBasicInfo.java:34](../../backend/src/main/java/com/foodtraceability/dto/UserBasicInfo.java#L34) `storeId`；前端 [api/auth.ts:29](../../frontend/src/api/auth.ts#L29) `storeId?: string | number` 消费。

**方案**：两个 DTO **新增 `locationId`**（直取 `users.location_id`，不经桥）；`storeId` / `storeName` 保留并加 `@Deprecated` + 注释「观察期兼容，取值经 location_id_map 反查」；前端 `auth.ts` 类型加 `locationId`，消费点改读 `locationId`。

**阶段 3 前置清点**：前端 `userInfo.storeId` / `user.storeId` 消费点全量 grep 清单（阶段 2 未清点，禁止在阶段 3 未清点前删除字段）。

**取舍**：删除 `storeId` 会同时打断未清点的消费点 → 不在本卡做（观察期结束、消费点归零后另卡删除）。

---

### D8 —— N12：15 个旧访问器调用点**不迁移**，改为「登记面 + 收敛 1 处冗余 + 立编码约束」

**实测分类**（`SecurityUtils.getCurrentUserStoreId()` 共 **15 个调用行 / 12 个文件**；`SchedulePlanServiceImpl` 另有私有包装 :547-548 被 :150/:264 使用，不计入 15）：

| 调用点 | 下游写入/过滤的列空间 | 能否改 `getCurrentUserLocationId()` |
|---|---|---|
| [PosOrderCreateServiceImpl.java:314](../../backend/src/main/java/com/foodtraceability/service/impl/PosOrderCreateServiceImpl.java#L314) | 订单表 `store_id` | **否**（表未收编） |
| [CallNumberQueueManagementController.java:95](../../backend/src/main/java/com/foodtraceability/controller/CallNumberQueueManagementController.java#L95) | 叫号队列 `store_id` | 否 |
| [DiningTableManagementController.java:211](../../backend/src/main/java/com/foodtraceability/controller/DiningTableManagementController.java#L211) | 桌台 `store_id` | 否 |
| [PurchasePlanServiceImpl.java:440](../../backend/src/main/java/com/foodtraceability/service/purchase/impl/PurchasePlanServiceImpl.java#L440) | `order.setStoreId(...)` | 否 |
| [StoreInventoryController.java:76](../../backend/src/main/java/com/foodtraceability/controller/StoreInventoryController.java#L76) | 先取 storeId 再 `resolveLocationIdByStoreId`（:83） | **是**（冗余往返，见下） |
| [StoreInventoryLogServiceImpl.java:111](../../backend/src/main/java/com/foodtraceability/service/impl/StoreInventoryLogServiceImpl.java#L111) | 流水按 store 维度（且涉 N2 的 CODE 空间） | 否（属 N1/N2 分流） |
| [BankAccountServiceImpl.java:234](../../backend/src/main/java/com/foodtraceability/service/finance/impl/BankAccountServiceImpl.java#L234) | 财务 `store_id` | 否（属 N5 分流） |
| [DeviceStatusServiceImpl.java:178](../../backend/src/main/java/com/foodtraceability/service/impl/DeviceStatusServiceImpl.java#L178) | 设备 `store_id` | 否（KL-086 冻结） |
| [SchedulePlanServiceImpl.java:69](../../backend/src/main/java/com/foodtraceability/service/impl/schedule/SchedulePlanServiceImpl.java#L69) | 排班 `store_id` | 否 |
| [RecruitmentRequirementServiceImpl.java:162](../../backend/src/main/java/com/foodtraceability/service/impl/RecruitmentRequirementServiceImpl.java#L162) | `requirement.setStoreId(String)` | 否 |
| [SalesOrderServiceImpl.java:268](../../backend/src/main/java/com/foodtraceability/service/impl/SalesOrderServiceImpl.java#L268) | 取 storeId 后 `resolveLocationIdByStoreId` 换算成 locationId | **是**（冗余往返） |
| [SalesOrderServiceImpl.java:329](../../backend/src/main/java/com/foodtraceability/service/impl/SalesOrderServiceImpl.java#L329) | `voucher.setStoreId(...)` | 否 |
| [StockForecastServiceImpl.java:53](../../backend/src/main/java/com/foodtraceability/service/impl/StockForecastServiceImpl.java#L53) / [:190](../../backend/src/main/java/com/foodtraceability/service/impl/StockForecastServiceImpl.java#L190) | 库存预测按 store 维度 | 否 |

**结论（本卡最重要的范围判断）**：**15 点中 13 点的目标表仍在 `store_id` 空间**（属 §0.2 的 41 张表收编，即 P1-LOCATION-MODEL-001 M5-M7）。把它们改成 `getCurrentUserLocationId()` 会把 **location_id 写进 store_id 列**，制造新错误。因此 002 对 N12 只做三件事：

1. **立登记面**：把上表作为「桥消费清单」定稿（本文件 §2 D8 表即产物），后续每张表收编时按行销账；
2. **收敛 2 处冗余往返**（`StoreInventoryController:76→83`、`SalesOrderServiceImpl:268`）：直接取 `getCurrentUserLocationId()`，省掉「桥反查 store → 再经 map 正查 location」的两次 DB 往返（语义等价，风险低）；
3. **编码约束**：`SecurityUtils.getCurrentUserStoreId()`（:124）补 `@Deprecated` 与「新增代码一律用 `getCurrentUserLocationId()`」的 Javadoc 断言（:120 已有措辞，缺 `@Deprecated` 标记）。

**可选（Owner 可砍）**：第 2 项若被认为超出「收尾补丁」定性，可整项移出 002，只保留 1、3。

---

### D9 —— 阶段 3 提交切分（每提交独立可回滚）

| 提交 | 内容 | 依赖 |
|---|---|---|
| **C1** | 桥正向入口 + 单测（D1-1） | 无 |
| **C2** | 员工 DTO 加 `locationId` + `EmployeeServiceImpl` 归属解析（D1-2/3）+ 单测 | C1 |
| **C3** | 权限码 5 面 + 独立端点 `PUT /v1/employees/{id}/assign-location` + 审计（D3-3/4/5）+ seed 迁移 | C2 |
| **C4** | 反向同步 `syncUserAssignmentFromEmployee`（D3-1/2）+ 单测 | C2、C3 |
| **C5** | 前端员工表单合并位置下拉（D2，含 N6 消解） | C2、C3 |
| **C6** | 用户创建表单加归属（D6） | 无（可与 C5 并行） |
| **C7** | N3 三处（D4） | 无 |
| **C8** | N4 一处（D5，仅 `PurchaseArrivalServiceImpl`） | 无 |
| **C9** | N8 出参加 `locationId` + 前端消费切换（D7） | 无 |
| **C10** | N12 冗余收敛 + `@Deprecated`（D8-2/3） | 无 |

> C1–C4 是缺口①②③主干；C5–C10 各自独立，任一被 Owner 砍掉不影响其余。

---

## 3. 改动清单（文件级）

| 文件 | 改什么 | WIP 状态（诊断 §6） | 提交 |
|---|---|---|---|
| `backend/.../common/util/LocationIdBridge.java` | +`toLocationId(srcTable, srcId)`、+`locationIdOfWarehouse` | 干净 | C1 |
| `backend/.../dto/EmployeeCreateDTO.java` / `EmployeeUpdateDTO.java` | +`locationId`；`storeId`/`warehouseId`/`workLocationType` 标 `@Deprecated` | 干净 | C2 |
| `backend/.../service/impl/EmployeeServiceImpl.java` | +`resolveLocationId(dto)`；create/update 归属写入改走它；+`syncUserAssignmentFromEmployee` | 干净 | C2、C4 |
| `backend/.../controller/EmployeeController.java` | +`PUT /{id}/assign-location`（权限码 + 审计）；`PUT /{id}` 移除归属语义 | 干净 | C3 |
| `backend/.../security/constants/AdminPermissions.java` | +`hr:employee:assign-location` | 干净 | C3 |
| `backend/src/main/resources/db/init-data.sql` | +权限行（`PERM_HR_EMP_ASSIGN_LOCATION`）+ 角色绑定 | 干净 | C3 |
| `backend/src/main/resources/db/migration/V<date>_001__seed_hr_employee_assign_location.sql`（**新文件**） | 幂等 seed（样板 [V20260728_011](../../backend/src/main/resources/db/migration/V20260728_011__add_health_cert_permissions.sql)） | 新文件 | C3 |
| `frontend/src/utils/permissions.ts` | +`hr:employee:assign-location`（:369-372 同组） | 干净 | C3 |
| `frontend/src/views/hr/components/EmployeeFormDialog.vue` | 两个旧下拉 → 一个 locations 下拉；类型标签；载荷 `locationId`；删 `storeId/warehouseId` 类型 | **带无关 WIP** → hunk 隔离 | C5 |
| `frontend/src/api/hr/employee.ts` | 载荷字段改 `locationId` | 干净 | C5 |
| `frontend/src/views/system/components/UserManagementTab.vue` | 创建表单 +可选位置下拉 | **带无关 WIP** → hunk 隔离 | C6 |
| `backend/.../service/impl/ProcurementTraceServiceImpl.java` :443 | 查询条件改 `location_id`（经桥） | 待阶段 3 预检 | C7 |
| `backend/.../service/impl/MaterialArchiveServiceImpl.java` :388 | 写 `location_id`（经桥） | 待阶段 3 预检 | C7 |
| `backend/.../config/InventoryWarningScheduler.java` :188 | 读回改经桥 `toWarehouseId` | 待阶段 3 预检 | C7 |
| `backend/.../service/purchase/impl/PurchaseArrivalServiceImpl.java` :656 | 删 `1L` 兜底 → 显式校验 | 待阶段 3 预检 | C8 |
| `backend/.../dto/UserInfoResponse.java` / `UserBasicInfo.java` | +`locationId`；`storeId` 标 `@Deprecated` | 干净 | C9 |
| `frontend/src/api/auth.ts` :29 | +`locationId`，消费切换 | 干净 | C9 |
| `backend/.../utils/SecurityUtils.java` :124 | 补 `@Deprecated` + 约束 Javadoc | 干净 | C10 |
| `backend/.../controller/StoreInventoryController.java` :76-83、`SalesOrderServiceImpl.java` :268 | 冗余往返收敛（**可选**） | 待阶段 3 预检 | C10 |

**合计**：19 个文件（17 改 + 1 新迁移 + 1 前端注册表）；**无表结构变更**。

---

## 4. 数据库影响（阶段 2 核实结论）

| 判定 | 结论 | 证据 |
|---|---|---|
| 需要新表？ | **否** | `locations` / `location_id_map` 已由 V20260927_001 建 |
| 需要新列 / 改名？ | **否** | `employees.location_id`、`employees.user_id`、`users.location_id` 均已存在（[V20260930_002:21,37](../../backend/src/main/resources/db/migration/V20260930_002__user_location_001_rename_store_id_to_location_id.sql#L21) 及 :3 注释） |
| 需要 seed 迁移？ | **是（1 条）** | 新权限码须入库；PG-003 要求 Flyway 单一来源 → 只改 `init-data.sql` 不满足 |
| ⚠ 附带事实（登记，不在本卡修） | -001 S5a 的 `system:user:assign-store` **未落任何 DB seed**：`init-data.sql` 与全部 Flyway 迁移 grep 均 0 命中 → 现只有 `hasAuthority('*')`（admin）可用 | 实测：`grep -rn "assign-store" backend/src/main/resources/db/` → 0 命中 |

→ 该事实构成 **U3 未决**（见 §7）：是否在 C3 的同一 seed 迁移里一并补齐 `assign-store` / `unassign-store`。

---

## 5. 回滚方案

| 层 | 回滚方式 | 说明 |
|---|---|---|
| 代码 | **按 C1–C10 逐提交 `git revert`**（每提交独立可 revert，切分依据见 D9） | 精确 `git add`（PG-001）保证每提交只含本卡 hunk，revert 不会带走 WIP |
| 数据库 | 无 schema 变更 → **无结构回滚**；seed 迁移回滚 = **forward-fix**（追加一条 `DELETE FROM role_permissions/permissions WHERE permission_id='PERM_HR_EMP_ASSIGN_LOCATION'` 迁移） | 实测本仓 `db/migration/` **无 undo/revert 命名惯例**（`grep -ci "undo\|revert"` = 0）→ 不引入 Flyway undo，沿用 forward-fix |
| 前端 | C5 / C6 各自 revert；旧端点 `/v1/stores/active`、`/v1/warehouses` **未删** → 回滚后表单可直接恢复 | D2「不做」项正是为回滚留的退路 |
| 权限 | 若 C3 已发放权限而 C4 被 revert → 权限存在但无反向同步 = 行为退回 -001 现状（不产生错误） | 顺序依赖已在 D9 标注 |
| 数据修复 | 若阶段 3 后发现「仓库员工 `location_id` 已写但写错」→ 提供一次性 `UPDATE ... FROM location_id_map` 脚本（**只写不跑，交 Owner**） | 参照 V20260930_002 的守卫式回填写法（:13-15 校验非 0 即 RAISE EXCEPTION） |

---

## 6. 验收基准（定稿，阶段 3 照此执行）

**A. 单元 / 集成**

| # | 断言 | 覆盖决策 |
|---|---|---|
| A1 | `locationIdOfWarehouse(真实仓 id)` 返回对应 `location_id`；`locationIdOfWarehouse(null)` 返回 null；**8 个 E2E/TEST 仓 id 返回 null** | D1 |
| A2 | 员工 create/update 选仓库 → `employees.location_id` 非空且 = 该仓 location；选门店 → = 该店 location；两者皆空 → 列为 null 且**不抛兜底异常** | D1 |
| A3 | 改员工归属 → 对应用户 `users.location_id` 同步变化；无对应用户 → `users` 不变且审计有记录 | D3 |
| A4 | 无 `hr:employee:assign-location` 的 HR 账号调 `PUT /v1/employees/{id}/assign-location` → **403**；有权限 → 200 且 `operation_log` 出现 `EMPLOYEE_LOCATION_ASSIGN` 且 desc 含旧值→新值 | D3 |
| A5 | 反向/正向 sync 不产生循环（单次调用只写对向表一次，可用调用计数断言） | D3 |
| A6 | `grep -rn "Inventory::getWarehouseId" backend/src/main` → **0 命中**；`grep -n "setWarehouseId" MaterialArchiveServiceImpl` → 0 命中；`inventory_warning_records.warehouse_id` 新记录非全 NULL | D4 |
| A7 | `grep -n ": 1L" PurchaseArrivalServiceImpl` 该行为 0 命中；未指定收货仓库 → 业务异常而非默认仓 1 | D5 |
| A8 | `UserInfoResponse` / `UserBasicInfo` JSON 出参含 `locationId`；`storeId` 仍在且值 = 桥换算结果 | D7 |
| A9 | `grep -c "DEFAULT_STORE_ID" backend/src/main/java` 计数**不变**（证明未撞 KL-086 冻结） | D5 / I-6 |

**B. 前端**

| # | 断言 |
|---|---|
| B1 | `EmployeeFormDialog.vue` 中 `getActiveStores()` / `warehouseApi.getActiveList()` 调用 **0 命中**；出现 `useLocationOptions` / `/v1/locations` |
| B2 | 提交载荷含 `locationId`（number），不再出现 `storeId` / `warehouseId` |
| B3 | 用户创建表单有可选位置下拉；编辑仍走 `assign-store` 入口（`api/system/user.ts:199,207` 不变） |
| B4 | `vite build` EXIT=0；`vue-tsc` 对**本卡改动文件** 0 错误（全量残留错误须逐条属既有 WIP，同 ENV-9） |

**C. 手工冒烟（须写明「含 WIP 工作树」，ENV-9 / ENV-10）**

1. HR 账号 → 员工管理 → 选一个**仓库型**位置 → 保存；
2. 用该员工账号登录 → 解 JWT → `locationId` claim **非空**；
3. 该账号访问仓库上下文接口 → `LocationGuard.requireWarehouseContext()`（[LocationGuard.java:109](../../backend/src/main/java/com/foodtraceability/security/guard/LocationGuard.java#L109)）**通过**（此前因 `location_id` 恒 NULL 无法通过）；
4. 同一账号在 POS 下单 → 不再 `NoLocationContextException`（对照 N11 扫描卡 A1）。

**D. 边界自检（阶段 3 收尾必查）**

- `git status --short` 增量 = 本卡 19 个文件（+1 新迁移），**不得**出现 §0.2 分流条目涉及的文件；
- 两个带 WIP 的前端文件：残留 diff 逐字等于原 WIP（PG-001 / I-5）。

---

## 7. 未决项（须 Owner 拍板后才进阶段 3）

| # | 问题 | 推荐 | 影响 |
|---|---|---|---|
| **U1** | 反向同步冲突语义：用户已有不同 `location_id` 时——覆盖 / 拒绝 / 覆盖但提示？ | **覆盖 + 审计旧值**（Owner Q1 已裁「同一件事」） | D3-1 |
| **U2** | 员工无对应用户时：只审计 / 报错 / 自动建账号？ | **只审计，不建账号** | D3-1 |
| **U3** | `-001` 的 `assign-store` 权限码未入库（§4 事实）：是否在同一 seed 迁移补齐？ | **补齐**（否则用户侧分配入口对非 admin 仍不可用，本卡员工侧却可用 → 口径不一致） | C3 范围 +1 权限码 |
| **U4** | 8 个 E2E/TEST 仓（未迁 `locations`）的员工归属怎么办？ | **显式拒绝 + 提示「该仓库未纳入位置体系，待 M7 退役」**（符合 I-3） | D1 数据前提 |
| **U5** | `PUT /v1/employees/{id}` 拆出独立归属端点（D3-3）——是否接受 API 面新增？ | **接受**（否则权限门槛会误伤普通档案编辑） | C3 |
| **U6** | D8-2 冗余往返收敛（2 处）是否纳入 002？ | **纳入**（可砍，砍后 N12 仅剩登记面） | C10 |
| **U7** | `hr:employee:assign-location` 授予哪些角色？（现有 HR 角色族：`ROLE_HR_DIRECTOR` 等，见 [init-data.sql:193](../../backend/src/main/resources/db/init-data.sql#L193)） | `ROLE_HR_DIRECTOR` + `admin`（`*` 已覆盖 admin）；`ROLE_STORE_MANAGER` **不授**（跨店改归属风险） | C3 seed 内容 |

---

## 8. 阶段边界声明

- 阶段 2 **完成**：全程只读（grep / read / 迁移文件核对），**未改任何代码、配置、DB**；未提前实施任何 D1–D8；未顺手修 N1/N2/N5/N9/N11 与 KL-085/KL-086。
- 本文件是**方案**，不是实施记录；实施记录在阶段 3 产出（重档要求）。
- **交接条件：Owner 对 §7 七项拍板 → 放行阶段 3（按 D9 提交切分实施）。**
