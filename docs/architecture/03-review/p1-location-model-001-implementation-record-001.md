# P1-LOCATION-MODEL-001 实施记录：M1-M2 核心层（implementation-record-001）

- **日期**：2026-09-27
- **性质**：PG-005 阶段 3 实施（Owner 已拍板 D-1~D-7 + 授权启动；设计冻结，依据 -002）
- **本批范围**：仅 M1-M2（locations + location_id_map 建表迁移 + 只读核心层）。**M3-M7（库存单表合并、单据切换、departments 挂接、旧表退役）未动，users.store_id 与 JWT 未触碰（Owner 禁令）。**

---

## 0. 开工前置（PG-001）

### 文件白名单（全部新建，零存量文件修改）

| # | 文件 |
|---|---|
| 1 | `backend/src/main/resources/db/migration/V20260927_001__create_locations_and_location_id_map.sql` |
| 2 | `backend/src/main/java/com/foodtraceability/entity/Location.java` |
| 3 | `backend/src/main/java/com/foodtraceability/entity/LocationIdMap.java` |
| 4 | `backend/src/main/java/com/foodtraceability/mapper/LocationMapper.java` |
| 5 | `backend/src/main/java/com/foodtraceability/mapper/LocationIdMapMapper.java` |
| 6 | `backend/src/main/java/com/foodtraceability/service/LocationService.java` |
| 7 | `backend/src/main/java/com/foodtraceability/service/impl/LocationServiceImpl.java` |
| 8 | `backend/src/main/java/com/foodtraceability/controller/LocationController.java` |

禁改清单（本批违反即停）：`entity/User*`、`entity/Employee*`、`security/**`、`utils/SecurityUtils`、`entity/StoreInventory*`、`entity/Inventory*`、`InventoryLogMapper.*`（24.3f 卡文件）、全部前端。

### 与 P1-INVENTORY-CONSUMPTION-STATS-001（§24.3f）文件重叠确认

24.3f 文件范围 = `InventoryLogMapper.xml` + `InventoryLogConsumptionStatsIntegrationTest` + 其实施记录（commit `9187286` 已见对应修复提交）。本批 8 个文件与上述**零重叠**。✅

### 回归清单（add-only，回归面=应用启动 + 旧接口抽查）

1. `mvn compile` BUILD SUCCESS
2. 应用启动成功且 Flyway 迁移成功（无既有迁移被破坏）
3. 旧接口抽查：`/v1/stores/active`、`/v1/warehouses` 域不受影响（本批未改任何存量文件，抽查确认）

### 验收命令与基准

| 项 | 命令 / 断言 | 基准 |
|---|---|---|
| 编译 | `mvn compile -DskipTests` | BUILD SUCCESS |
| 迁移 | 启动后查 `flyway_schema_history` | `20260927.001 success=t` |
| 数据 | psql 查 locations / location_id_map | 5 行 / 5 行（3 STORE + 2 仓） |
| API | `GET /v1/locations`、`/by-store/{id}`、`/by-warehouse/{id}` | code=0 且映射正确 |
| 回滚演练 | DOWN→验证→UP 重放→验证 | 表删除干净后可完整重建 |

### 回滚点

- **代码**：本批全部为新文件，回滚 = revert 本 commit（或删除 8 个文件）；
- **DB**：`DROP TABLE IF EXISTS location_id_map; DROP TABLE IF EXISTS locations;`（flyway_schema_history 中 20260927.001 记录需同步删除或保留——保留时应用重启不会重放，人工重放脚本即可，见演练）；
- **演练结果**：见 §3.4。

---

## 1. 实施

### 1.1 M1：建表（V20260927_001）

- `locations`：location_id IDENTITY PK / location_code UNIQUE / location_type（CHECK: STORE/CENTRAL/DEPOT/TRANSIT）/ **storage_type**（物理属性列，吸收 warehouse_type 冷冻常温口径，D-3 拍板）/ status / deleted，唯一约束与索引按 -001 §1.1。
- `location_id_map`：(src_table, src_id) UNIQUE → location_id（规则 3/4 的强制桥）。

### 1.2 M2：数据迁移（Owner"全清"口径）

- `stores_new`（deleted=0，3 行）→ STORE 型；`warehouses` 仅 `WH_A`/`WH_B`（WH_A→CENTRAL、WH_B→DEPOT，storage_type 按 2冷/3冻/4常温 映射）；8 个 E2E/TEST 仓不迁移（留在 warehouses 表，M7 退役）。
- **实施修正一处**：活体 `stores_new.manager_id` 为 **varchar**（与建表 DDL bigint 漂移），首次应用失败（SQL State 42804 类型不匹配，Flyway 事务回滚干净、未留半成品）。修正为 `CASE WHEN manager_id ~ '^[0-9]+$' THEN manager_id::bigint ELSE NULL END` 安全转型后成功。该漂移已记入 -002 §4.1 类型统一注意事项。

### 1.3 核心层（只读）

- `LocationService.resolveByStoreId / resolveByWarehouseId`：规则 3/4 的标准换算入口（经 location_id_map JOIN），**未找到返回 null，调用方决定拒绝语义，禁止数值兜底**（接口 javadoc 明示）。
- `LocationController`：GET `/v1/locations`（可按 locationType 过滤）、`/{locationId}`、`/by-store/{storeId}`、`/by-warehouse/{warehouseId}`，类级 @PreAuthorize 沿用 /v1/stores 同款角色集。

## 2. 验证证据（2026-09-27 21:58–22:02，活体）

| 项 | 结果 |
|---|---|
| 编译 | ✅ BUILD SUCCESS（mvn compile，JAVA_HOME=JDK21） |
| 迁移 | ✅ `flyway_schema_history`: `20260927.001 \| create locations and location id map \| t` |
| 数据 | ✅ locations 5 行（1-3 STORE_A/STORE_B/DS-MSB9DM2J；5=WH_A CENTRAL、4=WH_B DEPOT/NORMAL）；map 5 行（stores_new 1/2/3→1/2/3，warehouses 1→5、2→4） |
| API | ✅ `/v1/locations` 返回 5 行；`/by-store/1`→locationId=1 STORE_A；`/by-warehouse/1`→locationId=5 WH_A CENTRAL |
| 回归 | ✅ `/v1/stores/active` 原样返回（旧门店接口不受影响）；应用启动无任何既有 Bean 受影响 |

## 3. 回滚演练（2026-09-27 22:02）

1. DOWN：`DROP TABLE location_id_map; DROP TABLE locations;` → `to_regclass('locations') IS NULL` = t ✅
2. UP 重放（同一脚本经 psql 执行）：INSERT 3+3+2+2，locations=5 / map=5 ✅
3. API 复验：`/by-store/1` 正常 ✅
4. 结论：回滚路径可用；正式回滚时须同步处理 flyway_schema_history 记录（删记录或保留+人工重放，两种口径均演练可行）。

## 4. 本批未做（后续批次）

- M3/M4：store_inventory + inventory 合并、流水合并、污染数据全清
- M5：单据表（stockins/arrivals/transfers）location_id 切换
- M6：departments.location_id 挂接
- 前端 Location 选择器 / API 兼容层
- users.store_id / JWT / 员工归属（P1-USER-LOCATION-001，等本卡核心层——本批即其依赖）

## 5. 交接

M1-M2 完成 → 暂停等 Owner 审（commit 号见 git log）→ Owner 确认后再排 M3-M4。
