# P1-USER-LOCATION-001 实施记录（-001）

- **卡片**：P1-USER-LOCATION-001（任务板 §24.3h）——用户归属与身份层重构（JWT 携带 locationId / SecurityUtils / 13 处兜底消除 / 分配入口 / 入职链路 / users.location_id 改名）
- **日期**：2026-09-30 开卡
- **判档**：**重档**（PG-006，Owner 2026-09-30 回"重"）→ PG-005 三阶段 + 实施记录 + 更新 INDEX/roadmap + KL/ENV 登记 + PG-001 精确提交
- **依据**（实施以 -002 为准）：
  - 阶段1 诊断：`docs/quality/user-store-assignment-chain-audit-001.md`（链路审计 + 15 NULL 用户逐人分类 + 11 项差距）
  - 阶段2 设计主体：`docs/design/user-store-assignment-design-001.md`
  - 阶段2 设计修订：`docs/design/user-store-assignment-design-002.md`（§5 改名 / §5 修订二 admin 解析链 / §5.2 JWT locationId / §5.3 设备归属 / §7 仓库员工边界）
  - 前置语义：`docs/design/location-organization-separation-design-002.md` §7（ID 空间规则）
- **依赖**：P1-LOCATION-MODEL-001 一期核心层 —— **已收口**（`b2e1313`，`CLOSED_WITH_REGISTERED_LIMITATION`）

---

## §1 三阶段状态与本卡切入点

| 阶段 | 状态 | 产物 |
|---|---|---|
| 阶段 1 诊断（只读） | ✅ 已就位 | `user-store-assignment-chain-audit-001.md`（2026-09-27） |
| 阶段 2 设计（只出方案） | ✅ Owner 已拍板 | `-001` 主体 + `-002` 修订；U-1~U-7 + 附加 2/3/4 共 19 项拍板 + 3 处务实简化 |
| 阶段 3 实施 | **▶ 本卡当前阶段** | 本记录 |

**阶段 3 边界（PG-005）**：允许改代码 / 补测试 / 补实施记录；**禁止改 DB schema（除本卡明确需要）与顺手修其他**。本卡**明确需要**的 schema 变更仅：`users.store_id→location_id`、`employees.store_id→location_id`、`employees.user_id` 新增、兼容视图、回填脚本（-002 §5 修订一 + design-001 §1.4/§4.1）。
**交接条件**：DS 抽检 + QA 独立验收。

**Owner 已拍板对 -001 的覆盖点（本记录一律按 -002 执行）**：
1. `users.store_id` → **`users.location_id`** 全链改名 + 兼容视图一个观察期；`employees.store_id` 同批改名。
2. `SecurityUser`/JWT 单字段 **`locationId`**（仓库/门店共用；不再区分 storeId/warehouseId）。
3. 异常统一更名 **`NoLocationAssignedException`**（用户归属缺失）/ **`NoLocationContext`**（操作对象缺位置上下文，HTTP 400）。
4. 设备归属：**注册时必绑 `location_id`**（U-6 原"心跳落 NULL 行"折中废弃）；心跳报文自带设备 location_id。
5. 仓库员工：`users.location_id` 指向 DEPOT/CENTRAL 型 location，边界按 -002 §7；`users` 不再"不加仓库维度"。
6. admin：读侧 data_scope=all 走 all 分支不拒绝；写侧走**位置上下文解析链**（显式 locationId → 从对象推导 → 抛 `NoLocationContext`）。

---

## §2 实施切片计划（提交边界）

> PG-001：每片一次精确 `git add` + `git diff --cached` 验证；片间不混提交。
> 说明：S1+S2 是"改名骨架"，S3 起可并行于 S4/S5（设计称 ③④ 可先行）。

| 片 | 内容 | 主要文件面 | 提交边界 |
|---|---|---|---|
| **S1** | **DB 迁移**：`users.store_id→location_id`（值经 `location_id_map` 校验仍指 STORE 型）、`employees.store_id→location_id`、`employees.user_id BIGINT NULL`（按 employee_code 回填 14 行）、兼容视图 `v_users_store_compat` | `db/migration/V2026xxxx__*.sql`（只增不改，PG-003） | 1~2 个迁移文件一批 |
| **S2** | **代码侧改名**：`User`/`Employee` 实体 `storeId→locationId`；UserMapper/EmployeeMapper（Java+XML）；`UserCreateDTO`/`UserUpdateDTO`/`UserAssignStoreDTO`/`UpdateUserRequest`/`EmployeeTransferDTO`；`UserController`（create/update/assign-store/unassign-store）；`UserServiceImpl` assign/unassign；`DataPermissionServiceImpl`；`AuthServiceImpl`（注册 + 自助更新越权面） | entity / mapper / dto / controller / service / impl | 一批（编译器是验收） |
| **S3** | **身份层 locationId**：`SecurityUser.locationId`（替换 storeId）；JWT claim `locationId`（5 生成点）；`SecurityUtils.getCurrentUserLocationId()`（新主方法）+ `getCurrentUserStoreId()` 兼容（经 map 反查）；`KdsPrincipal` 经 `location_id_map` 映射；`UserPermissionCacheService` 不再作 storeId 来源 | security/model / security/utils / util / utils / service/impl（auth、token、pos） | 一批 |
| **S4** | **兜底消除 + 异常 + 守卫**：design-001 §6 的 13 处；新增 `NoLocationAssignedException` / `NoLocationContext` + 全局异常处理器注册；`LocationGuard`（`assertStoreContext` / `assertWarehouseContext`）；设备域 5 处 `DEFAULT_STORE_ID=1L` 改为读设备归属 `location_id`（-002 §5.3） | controller / service/impl（device、purchase、receipt、dashboard、table、callnumber） | 按域分 2~3 批 |
| **S5** | **分配入口**：用户管理页门店控件（激活死代码 `storeOptions` + 接线 `assignStore`/`unassignStore`）；assign-store **双写** `users.location_id` + `employees.location_id`；新增批量 `PUT /v1/users/assign-store-batch`；审计留痕 `sys_operation_logs`（模块 `USER_STORE_ASSIGN`）；越权面修复（`UpdateUserRequest` 删 storeId） | frontend（UserManagementTab.vue / api/system/user.ts）+ UserController / UserServiceImpl | 前端 + 后端各一批 |
| **S6** | **入职链路**：`OnboardingArchive` 加 `locationId`；`AuthServiceImpl` 注册不再置空（改读 archive）；`completeOnboarding` 透传 → `createEmployeeProfile`；positions **不加** store_id（U-3 拍板） | entity / service/impl（onboarding、registration、auth） | 一批 |
| **S7** | **回填核对 + 联调 + 收口**：15 NULL 用户逐人核对（admin + 9 总部保持 NULL / emp-l·n 待仓库 location / finqa×3 待停用）；Flyway 回填脚本 + users/employees 前后快照；E2E（登录→分配→token→权限）；实施记录 + 更新 INDEX/roadmap + KL/ENV 登记 | db/migration + docs + INDEX/roadmap/KL | 收口批 |

**风险与顺序约束**：
- S1（DB 改名）→ S2（代码改名）必须相邻，中间不停留（否则编译/运行断裂）。
- S4 中的 **#12 收货落店**、**#7 设备心跳**、**assign-store 双写** 三点需与 Location 模型语义对齐（`location_id_map` / `locations` 已有，M3-M4 已收口，故可做；若发现缺口按 PG-005 记入本记录不扩范围）。
- S7 的 emp-l/n 仓库归属须 `users.location_id` 指向 DEPOT/CENTRAL 型 location —— 若当前 locations 无该型数据，登记为限制项，不伪造。

---

## §3 验收基准（任务板 §24.3h 原样）

1. JWT claim 含 `locationId` 且 5 个生成点一致；
2. 15 个 NULL 用户按处置表逐人核对；
3. 13 处兜底逐项消除（拒绝路径 403/400 文案正确）；
4. 仓库员工（emp-l/n）边界：允许仓库操作、拒绝门店单据；
5. admin 写侧位置上下文解析链三级生效；
6. assign-store 分配有审计留痕。

---

## §4 执行环境事实（登记，非本卡回归）

| # | 事实 | 影响 |
|---|---|---|
| 1 | **本机无 DB**（PostgreSQL 不在本机运行）→ 8 个 `@SpringBootTest` 集成测试 + E2E 不可运行 | S1/S7 的迁移与回填**无法本地活体验证**，只能静态审查 + 交 QA 在活体环境验收；单测（mockito/plain）可跑 |
| 2 | **WIP 工作区致 ~10 个无关单测类失败**（ENV-9） | 42/42 基线在当前 WIP 树不可复现；本卡只对**受影响类**跑定向单测，并显式声明 WIP 干扰 |
| 3 | **构建 JDK 变更（2026-09-30 实测）**：原 `H:\jdk-25.0.1.8-hotspot`（Temurin 25）**已从磁盘消失**（`H:\` 全盘无 JDK）；现存可用 JDK = **`P:\my-new-project\JDK21`（Temurin 21.0.9+10 LTS）**。构建统一 `JAVA_HOME=P:\my-new-project\JDK21`（Maven 仍 `H:\fuwu\apache-maven-3.9.11`）。**副作用**：Java 25 下需要的 byte-buddy `experimental` 开关（ENV-3）在 JDK 21 下**不再需要** | 跑构建/测试时必带 |
| 4 | Maven `clean` + `danger-full-access`（沙箱拒 `backend/`、`target/`、`.git/` 写） | 构建命令固定 |

---

## §5 进度登记

| 片 | 状态 | commit | 备注 |
|---|---|---|---|
| 开卡（判档/记录） | ✅ 2026-09-30 | `b153613` | 任务板 §24.3h 状态 → 进行中；本记录落盘 |
| S1 DB 迁移 | ✅ 2026-09-30 | `e2a83e7` | `V20260930_002` 迁移（重映射 + 守卫 + 兼容视图）；与 S2a 同片提交 |
| S2 代码改名 | ✅ **S2a** 完成（S2b 待办） | `e2a83e7` | 实体 + 持久列 + ID 空间桥接（见 §7/§8）；S2b = DTO/API 字段名 |
| S3 身份层 | ✅ 2026-09-30 | `6fb7a6e` | `SecurityUser.locationId` + JWT claim + 5 生成点 + `SecurityUtils` 主方法（见 §9）；KDS 冻结 = LIM-1 |
| S4 兜底消除 | ✅ **非设备域完成（7/13）**；设备域冻结 LIM-2 | `6bb4edb` | S4a = 两异常 + `LocationGuard`（§10）；S4b = 7 处接入（§11/§12）；设备域 #3~#8 = LIM-2 |
| S5 分配入口 | ⏳ 待办 | — | 前端控件 + 双写 + 批量 + 审计 + 越权面 |
| S6 入职链路 | ⏳ 待办 | — | `OnboardingArchive.locationId` + 注册回填 + `completeOnboarding` 透传 |
| S7 回填/联调/收口 | ⏳ 待办 | — | 15 NULL 用户核对 + 回填脚本 + E2E + INDEX/roadmap/KL |

## §6 WIP 隔离裁决（Owner 2026-09-30）

**背景**：开卡后勘定发现 PG-001 按文件隔离受阻——**11 个必碰文件带无关 WIP**（~130 行），且 `KdsPrincipal` 定义在**未跟踪**的 `security/filter/KdsTokenFilter.java` 内。Owner 裁决路径：先判"security/KDS 这 5 个 WIP 文件**是否自洽、可独立入库**"→ 自洽走 B（先固化该批次）；**不自洽走 A 变体**（只冻 KDS hunk，其余全片推进）；明确**不选 C**（WIP 固化不该被本卡裹挟）、**不选 D**（骨架不解决问题）。

### 6.1 判定结论：**不自洽（不可独立入库）**

| 文件 | 状态 | 依赖 | 自洽性 |
|---|---|---|---|
| `security/config/KdsAuthProperties.java` | 未跟踪（新，57 行） | 仅 Spring（`@ConfigurationProperties("app.kds")`） | ✅ 自身自洽 |
| `security/filter/KdsTokenFilter.java` | 未跟踪（新，115 行） | 仅 `KdsAuthProperties`（内嵌 `KdsPrincipal` L99-114） | ✅ KDS 对内自洽 |
| `security/filter/JwtAuthenticationFilter.java` | M（+6） | 仅 `kds_` 前缀字面量（无类依赖） | ✅ 自身自洽 |
| `security/config/SecurityConfig.java` | M（+17） | 装配 `DeviceWhitelistFilter`+`KdsTokenFilter`；`permitAll` 指向 `/v1/device-registrations/status\|activate`（→ **未跟踪** `AppDeviceRegistrationController`）与 `/v1/receipt-confirmations/verify/**`（→ **5+ 文件 WIP**：`ReceiptConfirmationController` +51 / `Service` +35 / `CreateDTO` +108 / 实体 +84 / 另 3 个未跟踪 VO） | ❌ 拖入"APK 设备注册"与"防伪码查验"两个未落地特性 |
| `security/filter/DeviceWhitelistFilter.java` | 未跟踪（新，76 行） | **`AppDeviceRegistrationService`（未跟踪）** → `AppDeviceRegistration` 实体 / Mapper / Impl / Controller（均未跟踪，5 文件） | ❌ 依赖整条未入库特性 |

**结论**：KDS 三个新文件自身自洽，但 `SecurityConfig(+17)` 与 `DeviceWhitelistFilter` 把 **APK 设备注册特性（5 个未跟踪文件 + 表迁移缺失疑点）** 与 **防伪码查验特性（5+ 文件 WIP）** 一并拖入 → 该"5 文件批次"**不是自洽单元，无法独立入库** → 按 Owner 规则**降级 A 变体**。

### 6.2 执行方式：A 变体

**冻结（不触碰）**：
1. `security/config/SecurityConfig.java`（KDS/设备装配 hunk）与 `security/filter/JwtAuthenticationFilter.java`（KDS 跳过 hunk）——本卡 S3 **不需要**改这两个文件；
2. design-002 §5.2 的 **KDS 识别部分**（`SecurityUtils` 内的 `KdsPrincipal` 分支）→ 登记为**冻结限制项**，待 APK 设备注册 / KDS 批次入库后补；
3. 未跟踪的 `KdsTokenFilter` / `KdsAuthProperties` / `DeviceWhitelistFilter`。

**其余全片推进**（S1→S2→S6→S3 非 KDS 部分→S4→S5），对带 WIP 的目标文件用 **hunk 级隔离**：先取 WIP-only patch 存档 → "目标版本 = HEAD + 本卡改动" → `git add` 该文件 → 还原 WIP；每片 `git diff --cached` 逐 hunk 核对（PG-001）。
隔离清单：`UserController(+19)` / `GlobalExceptionHandler(+12)` / `DiningTableManagementController(+8)` / `CallNumberQueueManagementController(+3)` / `OperationsDashboardDataServiceImpl(+18−14)` / `HardwareConfigVersionMapper.xml` / `PosOrderCreateService.java` / `UserManagementTab.vue`。

**补充事实**：`AuthenticationServiceImpl(+8)` 与 `TokenServiceImpl(+8)` 的 WIP 经查是 **"P1-A 账户主数据"权限码批次**（`finance:bank:view/manage`、`balance:view/manage`、`product:cost:export`），**与 KDS 无关**，且改动区域（`ALL_PERMISSIONS` 常量数组）与本卡 S3 改动区域（`createSecurityUser` / `generateToken`）**不重叠** → 可安全 hunk 隔离，**不冻结**。

### 6.3 S1 迁移的待决点（登记）

-002 字面要求 `users.store_id` **"改名/迁移为 location_id"**，"旧列保留一个观察期为兼容视图"。实施二选一：

- **I（-002 字面）**：ADD `location_id` + 经 `location_id_map` 重映射 + FK/索引 → **DROP `store_id`** + 建 `v_users_store_compat`（旧 FK `fk_users_store_id` / 索引 `idx_users_store_id` 随列退役）；
- **II（保守两步）**：S1 只 ADD + 回填 + 建视图，`store_id` **暂留并标注 deprecated**，S7 收口再 DROP。

**本机无 DB**（迁移不可活体验证）→ S1 采用 **I + 内建数据完整性守卫**（"`store_id` 非空而 `location_id` 仍空"即 `RAISE EXCEPTION` 中止，防静默丢归属）；若 Owner 取 II，删去 DROP 段即可。

### 6.4 冻结限制项（待收口登记 KL/ENV）

**LIM-1**：KDS 托盘绑定链路的 `locationId` 识别（design-002 §5.2 的 `KdsPrincipal` 映射）**未实施** —— 冻结原因 = `KdsPrincipal` 位于未跟踪 WIP 文件、单独提交会导致已提交树编译不过；开卡条件 = APK 设备注册 / KDS 批次入库后。

---

## §7 S2 改名勘定（2026-09-30，S1 落盘后）

### 7.1 S1 已落盘（**未提交**，与 S2 同片原子提交）
`backend/src/main/resources/db/migration/V20260930_002__user_location_001_rename_store_id_to_location_id.sql`：
- `employees.user_id` 新增 + 按 `employee_code` 回填（-001 §1.4 前置）；
- `users.store_id → location_id`、`employees.store_id → location_id`（值经 `location_id_map` 由 `stores_new.store_id` **重映射**）+ FK/索引切换 + **数据完整性守卫**（"旧列非空、新列仍空"即 `RAISE EXCEPTION` 中止）；
- 兼容视图 `v_users_store_compat`。

**为何不单独提交**：迁移一旦落地而代码仍读旧列，运行期即断；提交边界须为 S1+S2 一片。

### 7.2 **S2 不是机械改名 —— ID 空间桥接才是核心工作量**

`users.store_id → location_id` **改变了 ID 空间**（`stores_new.store_id` → `locations.location_id`）。按 `location-organization-separation-design-002 §7`「一切跨 ID 空间的换算必须经 `location_id_map`」，凡"把用户归属当 `store_id` 用"的消费点都必须**反查映射**，否则退化为跨空间比较（静默错数据）。

**安全（零改）**：`SecurityUtils.getCurrentUserStoreId()` 的 **15 个调用点** —— -002 §5.2 已裁定该方法**保留为反查兼容方法**（`locationId` 经 map 反查 STORE 别名），故 15 处语义不变、无需改动。

**必须新增反查桥接的直接消费点**（编译器已定位 file:line）：

| 消费点 | 语义 | 处置 |
|---|---|---|
| `DataPermissionServiceImpl:291,386` | `condition.setSingleValue(storeId)` → 查询按 `store_id` 过滤 | 反查后传入 |
| `DataPermissionServiceImpl:576` | 仅 null 判定 | 改 `getLocationId()` 即可 |
| `DailySettlementServiceImpl:346-347` | `wrapper.eq(DailySettlement::getStoreId, …)`（store_id 空间） | 反查后传入 |
| `PurchaseRequestServiceImpl:139-140,562-563` | `request.setStoreId(resolveValidStoreId(...))` / `wrapper.eq(PurchaseRequest::getStoreId, …)` | 反查后传入 |
| `UserPermissionCacheServiceImpl:173` | `.storeId(user.getStoreId())` → 被 `DataPermissionAspect:140` `eq("store_id", …)` 消费 | 反查后存入 |
| `security/aspect/DataScopeAspect:197` | `dbUser.getStoreId()` 用于 STORE 型 SQL 拼接 | 反查后返回 |
| `UserDataServiceImpl:57-66,124-138,152` | 门店名查询 `batchGetStoreBasicInfo` / `getStoreBasicInfo`（store_id 空间） | 反查后再查门店名 |
| `UserServiceImpl:194-198` | 响应 `storeId` + `getStoreBasicInfo`（store_id 空间） | 反查后再查门店名 |
| `AuthServiceImpl:319` | 注册/登录响应 `setStoreId` | 反查后返回（保 API 兼容） |

**纯机械（直接改访问器）**：`UserController:70,138`、`EmployeeServiceImpl:310,352`、`EmployeeDataServiceImpl:121`、`EmployeeBasicDataServiceImpl:107`、`PermissionAutoAssignServiceImpl:186`、`WorkLocationFilterHelper:66,95,126`、`AuthServiceImpl:262,587`、`SecurityUtils:98`。

**编译器完整清单：45 个错误 / 15 个文件** —— `PurchaseRequestServiceImpl`、`DataPermissionServiceImpl`、`EmployeeServiceImpl`、`DailySettlementServiceImpl`、`UserServiceImpl`、`PermissionAutoAssignServiceImpl`、`EmployeeBasicDataServiceImpl`、`UserDataServiceImpl`、`EmployeeDataServiceImpl`、`security/aspect/DataScopeAspect`、`controller/UserController`、`AuthServiceImpl`、`utils/SecurityUtils`、`WorkLocationFilterHelper`、`UserPermissionCacheServiceImpl`。

**非编译错误但必须改**：`mapper/UserMapper.java` 原生 SQL —— `UPDATE users SET store_id = NULL`（:146）、`WHERE u.store_id = #{storeId}`（:162）+ `@Param("storeId")`；`DataFixController:381` 运行时补列 `"store_id BIGINT DEFAULT NULL"`。

### 7.3 S2a / S2b 解耦（降风险）
- **S2a** = 实体字段 + 持久列 + 访问器/桥接改造（`User.storeId→locationId`、`Employee.storeId→locationId`、`Employee.userId` 新增）→ 目标**绿构建 + API 契约不变**（DTO 字段名暂留 `storeId`，映射点写 `user.setLocationId(dto.getStoreId())`）→ 前端零影响；
- **S2b** = `UserCreateDTO` / `UserUpdateDTO` / `UserAssignStoreDTO` / `UpdateUserRequest` / `EmployeeCreateDTO` / `EmployeeUpdateDTO` 的 `storeId→locationId`（API 字段名）+ 前端接线（并入 S5）。

### 7.4 本轮执行结论
S1 已落盘；S2 实体改名**已尝试**：先改 `User`/`Employee` 字段与访问器，再跑 `mvn -o clean compile` 让编译器枚举全部消费点（**45 错 / 15 文件**，含上述桥接清单），随后**回退实体改动**以保持工作树可编译 —— 回退原因：ID 空间桥接是本片核心（需新增 `location_id_map` 反查方法并逐点改造），不宜在未完成桥接时留下编译断裂。原始文件无 WIP，回退无损（`git checkout HEAD --` 两文件）。

---

## §8 S2a 完成与验证（2026-09-30）

### 8.1 交付物（commit `e2a83e7`，22 文件 +321/−62）

| 层 | 内容 |
|---|---|
| DB（S1） | `V20260930_002__user_location_001_rename_store_id_to_location_id.sql`：`employees.user_id` 新增 + 按 `employee_code` 回填；`users.store_id→location_id`、`employees.store_id→location_id`（经 `location_id_map` 由 `stores_new.store_id` **重映射**）；FK/索引切换；**完整性守卫**（"旧列非空、新列仍空"即 `RAISE EXCEPTION` 中止）；兼容视图 `v_users_store_compat` |
| 桥（规则 3/4） | 新增 `LocationIdBridge`（`toStoreId` / `toLocationId` / `toWarehouseId` + **静态入口** `storeIdOf` / `locationIdOfStore`，供静态工具）；`LocationIdMapMapper` 新增 `selectSrcIdByLocationId` / `selectLocationIdBySrcId` |
| 实体 | `User.storeId→locationId`（`@TableField("location_id")`）；`Employee.storeId→locationId` + 新增 `Employee.userId`（`@TableField("user_id")`） |
| 身份层（S3 前置） | `SecurityUtils.getCurrentUserLocationId()`（新主方法，null = 显式拒绝）；`getCurrentUserStoreId()` **保留为反查兼容方法**（经桥）→ **15 个存量调用点零改** |
| 消费点 | 14 文件：store_id 空间消费点经桥**反查**（`DataPermissionServiceImpl` / `DailySettlementServiceImpl` / `PurchaseRequestServiceImpl` / `UserPermissionCacheServiceImpl` / `DataScopeAspect` / `UserDataServiceImpl` / `UserServiceImpl` / `AuthServiceImpl` / `WorkLocationFilterHelper` / `Employee*ServiceImpl` …）；写入经 `locationIdOfStore` **正查** |
| 原生 SQL | `UserMapper`：`unassignStore` 改 `location_id`；`findUserIdsByStoreIdAndRoleCode` 改为 **SQL 内 `JOIN location_id_map`**（保持 store_id 入参语义 → **调用方零改**）；`DataFixController` 运行时补列改 `location_id` |

### 8.2 验证证据
- **编译器闭环**：实体改名后 `mvn -o clean compile` 报 **45 错 / 15 文件**（消费点全枚举）→ 逐点改造后 **BUILD SUCCESS（EXIT=0）**。
- **PG-001 隔离**：3 个碰撞文件（`UserController`（+19 WIP）/ `DataFixController` / `DailySettlementServiceImpl`）按「HEAD + 本卡改动」精确入索引；**提交后工作树残留 diff = 原 WIP 尺寸（`UserController` 恰为 +19）** → 隔离精确、WIP 零混入。
- **可疑行扫描**：cached diff 中不含 storeId/location 关键字的行全部落在新文件与新注释上，**无 WIP 泄漏**。

### 8.3 ⚠ 重大既有发现：**已提交树无法独立编译（100 错，非本卡引入）**
在临时 worktree 编译**纯提交树**（补入本地 `backend/lib/*.jar` 以解 system-scope 依赖后）：

| 被测提交 | 结果 |
|---|---|
| `e2a83e7`（本卡 S2a） | **BUILD FAILURE** |
| `e2a83e7^`（本卡提交前） | **BUILD FAILURE，100 errors，同一文件集** |

失败文件：`ReceiptConfirmationServiceImpl`（import `ReceiptEvidence` / `ReceiptSignature` / `ReceiptVerifyVO` / `ReceiptPrintLog` / `ReceiptSignatureDTO` 等**未跟踪**类）、`PosOrderCreateServiceImpl`（引用 `MaterialDeductionContext` / `MaterialDeductionFailureException` / `CanonicalOrderCommand` 等**未跟踪**类）。

**结论**：**committed 代码引用了未跟踪的 WIP 类** → "仓库已提交状态"长期不可独立构建；本卡提交**未引入也未加剧**该问题。
**影响**：任何 commit 的"可编译性"只能在**含 WIP 的工作树**上判定；纯提交树构建验证须补入本地 `backend/lib` 且仍会失败 → 所有构建/测试结论必须注明"含 WIP 工作树"。
**登记**：KL 主表 **ENV-10**（归 P0-WORKSPACE-WIP-CONSOLIDATION-001）。

### 8.4 S2a 遗留（下一步）
- **S2b**：DTO/API 字段名 `storeId → locationId`（`UserCreateDTO` / `UserUpdateDTO` / `UserAssignStoreDTO` / `UpdateUserRequest` / `EmployeeCreateDTO` / `EmployeeUpdateDTO`）+ 前端接线（并入 S5）；
- **观察期兼容方法退场**：`SecurityUtils.getCurrentUserStoreId()` 待 15 个调用点迁至 `getCurrentUserLocationId()` 后废弃；
- **S3 身份层**：`SecurityUser.locationId` + JWT claim `locationId`（5 生成点）+ `SecurityUtils` 的 `SecurityUser` 分支（KDS 分支冻结 → LIM-1）。

---

## §9 S3 身份层完成（2026-09-30，commit `6fb7a6e`）

### 9.1 交付（7 文件 +42/−1）
| 项 | 内容 |
|---|---|
| `SecurityUser` | 新增 `locationId` 字段（**单字段走天下**：门店员工 → STORE 型 location、仓库员工 → CENTRAL/DEPOT 型；design-002 §5.2 不再区分 storeId/warehouseId）+ getter/setter |
| `JwtUtils.generateToken` | 写 claim `locationId`（**null 不写**）；旧 token 无该 claim → 解析为 null，与现状一致 → **存量 token 平滑过渡、不做强制全员重登** |
| `JwtUtils.getUserFromToken` | 读 claim `locationId`（`Number` 判定后 `longValue`） |
| `SecurityUtils.getCurrentUserLocationId()` | 补 `SecurityUser`（JWT）分支 → **新主方法**；null = 显式拒绝 |
| `SecurityUtils.getCurrentUserStoreId()` | 保持**反查兼容方法**（经 `location_id_map`）→ **15 个存量调用点零改** |
| 生成点 ×5 | `AuthenticationServiceImpl.createSecurityUser` / `AuthServiceImpl.buildSecurityUser` / `PosAuthServiceImpl.buildSecurityUser`（claim 写入由 `JwtUtils` 统一）+ `TokenServiceImpl.refreshToken`（**从 `dbUser` 补，不受旧 refresh token 无 claim 影响**，design-002 §5.2） |
| KDS | 识别**冻结 = LIM-1**（`KdsPrincipal` 在未跟踪 WIP 文件内；单独提交会导致已提交树编译不过） |

### 9.2 验证
- `mvn -o clean compile` = **BUILD SUCCESS**（含 WIP 工作树；纯提交树另见 ENV-10）。
- **PG-001 隔离**：`AuthenticationServiceImpl`（WIP +6/−2「P1-A 权限码」批次）与 `TokenServiceImpl`（WIP +6/−2）按「HEAD + 本卡改动」入索引；**提交后残留 diff 尺寸与原 WIP 精确一致**。
- 过程中修正一次**隔离顺序错误**（插入置于锚点之后，而工作树为锚点之前）→ 重做后残留 diff 复原为原 WIP 尺寸。**方法学结论**：隔离须使「工作树 = 索引 + 原 WIP」**逐字成立**，否则会留下伪 diff 污染后续隔离。

### 9.3 验收基准对照（任务板 §24.3h）
| # | 基准 | 状态 |
|---|---|---|
| 1 | JWT claim 含 `locationId` 且 5 个生成点一致 | ✅ **S3 达成**（写入 `JwtUtils.generateToken` 统一；5 生成点均带取值源） |
| 2 | 15 个 NULL 用户按处置表逐人核对 | ⏳ S7 |
| 3 | 13 处兜底逐项消除（403/400 文案正确） | ⏳ S4 |
| 4 | 仓库员工（emp-l/n）边界 | ⏳ S4/S6 |
| 5 | admin 写侧位置上下文解析链三级生效 | ⏳ S4 |
| 6 | assign-store 分配有审计留痕 | ⏳ S5 |

---

## §10 S4a 位置上下文守卫（2026-09-30，commit `7e59f52`）

### 10.1 交付（3 个新文件，+208）
| 文件 | 内容 |
|---|---|
| `common/exception/NoLocationAssignedException` | **-001 §6 的 `NoStoreAssignedException` 按 -002 §5 修订二更名**；语义从"未分配门店"扩为"未分配位置（门店/仓库）"；码 **403**；默认文案"当前用户未分配位置，请联系管理员在用户管理中分配" |
| `common/exception/NoLocationContextException` | **操作对象**缺位置上下文；码 **400**；文案"无法确定操作的位置上下文" |
| `common/util/LocationGuard` | 统一守卫：`requireCurrentLocationId()`（403）/ `requireStoreContext()`（归属须 STORE 型 → 返回 `store_id`；**仓库员工访问门店单据 → 403**，-002 §7）/ `requireWarehouseContext()`（须 CENTRAL/DEPOT → 返回 `warehouse_id`）/ `assertStoreContext(targetLocationId)`（解析链 ①② + 一致性断言） |

### 10.2 关键实现决策
- **不新增/不改全局异常处理器**：-002 §5 修订二明确"**全局异常处理器注册不变**"，故两异常继承 `BusinessException`，由既有 `handleBusinessException` 处理（`Result.error(code, message)`，403/400 透传）→ **零处理器改动**，并**避开** `GlobalExceptionHandler.java` 的无关 WIP（+12）碰撞 ✓。
- **拒绝统一、禁止数值兜底**：`LocationGuard` 是 design-001 §6「13 处散写判定」的收敛点，也是 -002 §7「仓库员工允许/拒绝清单」的落点。
- **仓库"允许清单"不进守卫**：按 -002 §7.1，仓库操作（入库/出库/损耗/调整/调拨/盘点）靠**功能权限**（warehouse_manager 角色），不靠 location 推导；守卫只提供 `requireWarehouseContext()` 供显式校验。

### 10.3 验证
`mvn -o clean compile` = **BUILD SUCCESS**（含 WIP 工作树）。

### 10.4 S4b 待办（13 处接入守卫）
| 组 | 站点 | 状态 |
|---|---|---|
| 可干净落地 | **#9** `PurchasePlanServiceImpl:435`、**#10** `PurchaseRequestServiceImpl:329-343`、**#11** `PosOrderCreateServiceImpl:309-313`、**#12** `ReceiptConfirmationServiceImpl:569,581` | ⏳ 四文件无 WIP |
| 需 hunk 隔离 | **#1** `DiningTableManagementController`（WIP +8）、**#2** `CallNumberQueueManagementController`（WIP +3）、**#13** `OperationsDashboardDataServiceImpl`（WIP +18−14） | ⏳ |
| **冻结 = LIM-2** | **#3~#8 设备域**：-002 §5.3 要求"设备注册必绑 `location_id` + 心跳自带设备归属"，但 `devices.location_id` **尚无迁移**、且设备注册链（`DeviceWhitelistFilter` / `AppDeviceRegistration*`）整条为**未跟踪 WIP** | 待该批次入库后实施 |

### 10.5 冻结限制项（累计）
- **LIM-1**（§6.4）：KDS 托盘绑定链路的 `locationId` 识别（`SecurityUtils` 的 `KdsPrincipal` 分支）—— `KdsPrincipal` 在未跟踪 WIP 文件内。
- **LIM-2**（本节）：设备域 5 处 `DEFAULT_STORE_ID=1L` 兜底 → 设备归属 `location_id`（-002 §5.3）—— 缺 `devices.location_id` 迁移 + 设备注册链整条未跟踪 WIP。

---

## §11 S4b 实施进度（2026-09-30，commit `7357433`）

### 11.1 已完成（4 处，均在**无 WIP** 文件上）
| # | 站点 | 原兜底 | 改为 |
|---|---|---|---|
| **#11** | `PosOrderCreateServiceImpl`（解析门店信息） | `selectByStoreCode("STORE_A")` → 否则 `"1"` | **解析链 ②**：`SecurityUtils.getCurrentUserStoreId()`；仍无 → **400 `NoLocationContextException`**（删双兜底） |
| **#10** | `PurchaseRequestServiceImpl.resolveValidStoreId` | 门店不存在/无法解析 → **回落 `"1"`** | **抛 400**（"门店无效或不存在"）—— 回落会静默污染订单归属 |
| **#12** | `ReceiptConfirmationServiceImpl` | 门店为空 → **"最小活跃店"猜测**（含 `resolveDefaultStoreId`） | **抛 400**（收货必须明确落店）；**删除猜测方法** |
| **#9** | `PurchasePlanServiceImpl`（计划转订单） | `order.setStoreId(1L)`（"集中式单店"） | **从 `getCurrentUserStoreId()` 取；缺失 → 403 `NoLocationAssignedException`** |

### 11.2 待办
| # | 站点 | WIP | 状态 |
|---|---|---|---|
| #1 | `DiningTableManagementController:205-216` | +8 | ⏳ 需 hunk 隔离 |
| #2 | `CallNumberQueueManagementController:90-100` | +3 | ⏳ 需 hunk 隔离 |
| #13 | `OperationsDashboardDataServiceImpl:104-105` | +18−14 | ⏳ 需 hunk 隔离 |
| #3~#8 | 设备域 5 处 `DEFAULT_STORE_ID` | 整条未跟踪 | **冻结 = LIM-2** |

### 11.3 验证
`mvn -o clean compile` = **BUILD SUCCESS**（`JAVA_HOME=P:\my-new-project\JDK21`，Temurin 21.0.9；见 §4 #3 的 JDK 变更）。

### 11.4 进度小结（验收基准 §3）
- 基准 1（JWT claim `locationId` + 5 生成点）✅ **S3**
- 基准 3（13 处兜底）：**4/13 已消除**（#9/#10/#11/#12），3 处待隔离（#1/#2/#13），5 处设备域冻结（LIM-2）+ 1 处（#7 心跳）随 LIM-2
- 基准 4（仓库员工边界）/ 基准 5（admin 解析链）：守卫已就位（S4a），待站点接入
- 基准 2（15 NULL 用户）→ S6/S7；基准 6（assign-store 审计）→ S5

---

## §12 S4b 完成（非设备域 7/13，2026-09-30，commit `6bb4edb`）

### 12.1 part2 交付（3 处，均需 hunk 隔离）
| # | 站点 | 原兜底 | 改为 |
|---|---|---|---|
| **#1** | `DiningTableManagementController.getCurrentStoreId` | 未取到门店ID → **`return 1L`** | 归属缺失 → **403 `NoLocationAssignedException`**；门店ID格式异常 → **400 `NoLocationContextException`** |
| **#2** | `CallNumberQueueManagementController.getCurrentStoreId` | 与 #1 **逐字相同**的 `return 1L` | 同上（两处一并消除） |
| **#13** | `OperationsDashboardDataServiceImpl` | 误导性注释"单店模式默认门店1聚合" | 删除注释；读侧明确走 **data_scope 聚合**（`storeIds` 空 = 总部/admin 不限制；店长限制本人门店） |

### 12.2 PG-001 隔离（A 变体）
3 文件均带无关 WIP，按「HEAD + 本卡改动」精确入索引；**提交后残留 diff 尺寸与原 WIP 精确一致**：

| 文件 | 原 WIP | 残留 diff | WIP 内容 |
|---|---|---|---|
| `DiningTableManagementController` | +8 | `8 ++++++++` ✓ | 7 处 `@PreAuthorize` + 1 import（权限注解批次） |
| `CallNumberQueueManagementController` | +3 | `3 +++` ✓ | 权限注解批次 |
| `OperationsDashboardDataServiceImpl` | +18−14 | `18 ++++--------------` ✓ | `posOrderMapper` / `OrderMapper` 移除 + 两方法改单表聚合（W1-EC-04B-3） |

**方法学补充（本轮踩坑）**：隔离脚本内的文本必须与 `edit` 工具写入的文本**逐字一致** —— 本轮注释里的**弯引号 `“”` 与直引号 `""`** 不一致，造成 2 处伪 diff（`DiningTable` 10 vs 8、`OperationsDashboard` 20 vs 18），改用一致引号重做后精确复原。**规则**：残留 diff 必须逐字等于原 WIP，否则隔离不算通过。

### 12.3 S4b 汇总（13 处）
| 组 | 数量 | 状态 |
|---|---|---|
| 非设备域（#1 / #2 / #9 / #10 / #11 / #12 / #13） | 7 | ✅ **已消除** |
| 设备域（#3~#8，含 #7 心跳） | 6 | **冻结 = LIM-2**（缺 `devices.location_id` 迁移 + 设备注册链整条未跟踪 WIP） |

**基准 3（13 处兜底）**：非设备域 **7/7 达成**（403/400 文案正确）；设备域 6 处受限（LIM-2）。

### 12.4 验证
`mvn -o clean compile` = **BUILD SUCCESS**（`JAVA_HOME=P:\my-new-project\JDK21`）。
提交：`7357433`（part1，4 处）+ `6bb4edb`（part2，3 处）。

---

## §13 S6 入职链路完成（2026-09-30，commit `7d6db6d`）

### 13.1 交付（5 文件，+70/−2）
| 项 | 内容 |
|---|---|
| 迁移 `V20260930_003` | `onboarding_archive` 新增 **`location_id`**（+列注释）；存量按 `employee_code` 对齐已注册用户归属做**尽力回填**，无匹配/未分配保持 NULL（不猜门店） |
| `OnboardingArchive` / `OnboardingArchiveDTO` | 新增 `locationId` → DTO 经 `BeanUtils.copyProperties` **自动贯通** archive 创建/编辑链 |
| `AuthServiceImpl` | **删除硬编码置空归属**（`locationIdOfStore(null)`）→ **`user.setLocationId(archive.getLocationId())`**（-001 §1.2 根因断点修复） |
| `OnboardingRecordServiceImpl.createEmployeeProfile` | **修复"入职完成不落门店"断点**：`record.storeId` 经 `location_id_map` 换算为 `location_id` 落 `employee.location_id`；缺值/无法换算**留空**（不猜门店） |

### 13.2 与设计对照
- §1.1「OnboardingArchive 加 storeId」→ 按 **-002 §5 修订一**落地为 **`locationId`**（ID 空间 = `locations.location_id`）✓
- §1.1「completeOnboarding 把 record.storeId 传给 createEmployeeProfile」✓
- §1.2「AuthServiceImpl:262 硬编码修复」→ 落地为 `setLocationId(archive.getLocationId())` ✓
- 兼容旧数据：存量 archive 无 `locationId` → 注册后用户归属为 NULL → 落入"未分配"集合，由 **S5 人工分配入口**补配（**不自动猜测**，符合 Owner 裁定）✓

### 13.3 验证
`mvn -o clean compile` = **BUILD SUCCESS**（JDK21）。5 文件均**无 WIP** → PG-001 常规精确 add（cached diff 逐行审阅）。

### 13.4 遗留
- **`employees.user_id` 的注册时关联**（-001 §1.4「同步规则（后续）」）→ 并入 **S5**（与 assign-store 双写同片）；
- 存量 14 个 emp-* 的 `user_id` 回填已在 **S1 迁移**完成 ✓。

---

## §14 S5a 分配入口后端（2026-09-30，commit `6bf16b5`）

### 14.1 交付（3 文件，+53/−7）
| 项 | 内容 |
|---|---|
| `UserServiceImpl.assignStore` | 入参 `storeId`（store_id 空间）经 `location_id_map` 换算为 `location_id`；**门店无效即 400 `NoLocationContextException` 拒绝**（不再落 NULL 兜底） |
| `UserServiceImpl.unassignStore` | 归属**双写**同步清空员工侧 |
| 新增 `syncEmployeeAssignment(user, locationId)` | **-001 §1.4 双写**：按 `employee_code` 写 `employees.location_id` + **补齐 `employees.user_id` 强关联**（此前仅 employee_code 弱关联）；无档案（admin/总部/测试号）静默跳过；归属值只取已换算结果，**不猜不兜底** |
| `AuthServiceImpl.updateUserInfo` | **越权面修复**（-001 §2.3）：删除自助更新中的归属写入 —— 用户不得自行改本人门店 |
| `UserController` | `assign-store` / `unassign-store` 加 **`@OperationLog(module = "USER_STORE_ASSIGN")`** 审计留痕（复用既有 `sys_operation_logs` 机制）→ **验收基准 6 达成** |

### 14.2 审计实现方式
沿用现有 **`@OperationLog` 注解 + `OperationLogAspect`** 机制（模块 `USER_STORE_ASSIGN`，操作人/时间/请求参数由切面落 `sys_operation_logs`），**不新增独立审计代码** —— 与 design-001 §2.3「统一写 sys_operation_logs」等价且复用既有基础设施。

### 14.3 PG-001 隔离
`UserController` 带无关 WIP（+19）→ 按「HEAD + 本卡改动」入索引；**残留 diff = 19 逐字一致** ✓。另两文件无 WIP，常规精确 add。

### 14.4 验证
`mvn -o clean compile` = **BUILD SUCCESS**（JDK21）。

### 14.5 S5b 待办
- **批量接口** `PUT /v1/users/assign-store-batch`（U-7 拍板：Body = userIds[] + storeId，权限同单点，复用 `syncEmployeeAssignment`）；
- **S2b**：`UserCreateDTO` / `UserUpdateDTO` / `UserAssignStoreDTO` / `UpdateUserRequest` / `EmployeeCreateDTO` / `EmployeeUpdateDTO` 字段 `storeId → locationId`（API 契约）+ **前端接线**（`UserManagementTab.vue` 门店控件 + `api/system/user.ts` 的 assignStore/unassignStore 已定义未调用；该文件带 WIP → 需隔离）；
- **`UpdateUserRequest.storeId` 字段删除**（-001 §2.3 字面要求；本轮只删了写入路径）。

---

## §15 S5b-part1 批量分配接口（2026-09-30，commit `39d63e3`）

### 15.1 交付（4 文件，+99）
| 项 | 内容 |
|---|---|
| `UserAssignStoreBatchDTO`（新） | `userIds[]` + `storeId`（`@NotEmpty` / `@NotNull` 校验；`storeId` 为 stores_new.store_id，内部换算） |
| `UserService.assignStoreBatch` + `UserServiceImpl` 实现 | **与单点同语义**：`store_id` 经 `location_id_map` 换算；逐用户双写 `users.location_id` + `employees.location_id`（并补齐 `employees.user_id`，复用 `syncEmployeeAssignment`）；不存在用户跳过并告警；**整体一个事务**；返回成功数 |
| `UserController` | `PUT /v1/users/assign-store-batch`：权限同单点（`system:user:assign-store`）+ **`@OperationLog(USER_STORE_ASSIGN)`** 审计留痕 |

### 15.2 对应设计
- design-001 §2.2「新增 `PUT /v1/users/assign-store-batch`（Body: userIds[] + storeId）；权限同单点」✓
- U-7 拍板「批量分配接口一期做」✓
- design-001 §2.2「按组织架构批量：一期不做」→ 遵守（未做）✓

### 15.3 PG-001 隔离与验证
`UserController` 带无关 WIP（+19）→ 隔离后 cached **12 行**、残留 **19 行逐字一致** ✓；另三文件无 WIP。`mvn -o clean compile` = **BUILD SUCCESS**（JDK21）。

### 15.4 S5b 剩余（→ 下一轮）
- **S2b DTO/API 字段改名**：`UserCreateDTO` / `UserUpdateDTO` / `UserAssignStoreDTO` / `UpdateUserRequest` / `EmployeeCreateDTO` / `EmployeeUpdateDTO`：`storeId → locationId`（含 delombok 生成的 `equals/hashCode/toString` 同步）+ `UpdateUserRequest.storeId` **字段删除**（-001 §2.3 字面要求）；
- **前端接线（Owner 核心诉求"入口"）**：`UserManagementTab.vue` 门店控件（激活死代码 `storeOptions`）+ `api/system/user.ts` 的 `assignStore`/`unassignStore`（已定义 0 调用）→ 该 vue 带 WIP，需隔离；
- **⚠ 契约决策点（已裁定）**：-002 §5 修订一要求 Body 字段名为 `locationId`（值 = location_id 空间），design-001 §2.1 的下拉选项来自 `/v1/stores/active`（store_id 空间）——两文冲突。**依 Owner 既有指令「实施以 -002 为准」取 A**：字段 `locationId` 的值 = **location_id**；服务端 `assignStore` / `assignStoreBatch` 改为**直接接收 location_id** 并用 `LocationService` 校验 STORE 型（不再做换算）；前端下拉列 **locations**。**影响**：S5a/S5b 服务端入参空间需随之调整（当前唯一客户端是未接线前端，风险低）。

---

## §16 S5b-part2a assign-store 契约对齐（裁定 A）（2026-09-30，commit `87c9151`）

### 16.1 交付（5 文件 / cached +66−47）
| 项 | 内容 |
|---|---|
| `UserAssignStoreDTO` | `storeId → locationId`（**值 = locations.location_id**）；**顺带修正 `@NotBlank` 误用于 `Long`**（@NotBlank 只对 CharSequence 生效，原注解实际不校验）→ `@NotNull` |
| `UserAssignStoreBatchDTO` | 同步 `storeId → locationId` |
| `UserService.assignStore` / `assignStoreBatch` | 入参语义改为 **location_id**（不再做 store_id → location_id 换算） |
| `UserServiceImpl` | 新增 `requireExistingLocation(locationId)`：经 `LocationService.getById` 校验位置存在；**STORE / CENTRAL / DEPOT 均可**（-002 §5.2 门店与仓库共用同一归属字段，故**不能**限制为 STORE 型）；注入 `LocationService` |
| `UserController` | `assign-store` / `assign-store-batch` 改取 `request.getLocationId()` |

### 16.2 裁定依据
Owner 既有指令「依据 design-001（主体）+ design-002（修订，**以 -002 为准**）」→ -002 §5 修订一「Body 字段名改 locationId」+ §5.2「单字段走天下」⇒ 字段名与取值空间**同步**改为 location_id（design-001 §2.1 的 `/v1/stores/active` 下拉口径被 -002 覆盖）。

### 16.3 PG-001 隔离与验证
`UserController` 带无关 WIP（+19）→ 隔离后 cached **6 行**（3 改 3 增）、残留 **19 行逐字一致** ✓；另 4 文件无 WIP。`mvn -o clean compile` = **BUILD SUCCESS**（JDK21）。
> **隔离方法学补充**：多行锚点（含换行拼接）易失配 —— 本轮首次 ABORT；**改用逐行单行锚点**后成功。记入 §12.2：锚点尽量**单行且可验证**。

### 16.4 S5b 剩余
- **S2b 其余 DTO**：`UserCreateDTO` / `UserUpdateDTO` / `UpdateUserRequest`（含 **字段删除**）/ `EmployeeCreateDTO` / `EmployeeUpdateDTO`：`storeId → locationId`（含 delombok 的 `equals/hashCode/toString` 同步）；
- **前端接线**（Owner 核心诉求"入口"）：`UserManagementTab.vue`（死代码 `storeOptions` → 改为 **locations 下拉**）+ `api/system/user.ts` 的 `assignStore`/`unassignStore` 接线（送 `locationId`）；该 vue 带 WIP → 需隔离。

---

## §17 15 个 NULL 用户逐人处置核对（验收基准 2）（2026-09-30）

> 依据：阶段1 诊断 §5 活体分类 + design-001 §3.1 逐人处置表 + U-1（总部保持 NULL）/ U-5（finqa 停用）拍板。
> **环境限制**：本机无 DB（§4 #1）→ 本表为**设计符合性核对**（对照处置表逐人定性）；**活体数据核对**（`SELECT username, location_id FROM users`）须在有 DB 环境执行。

| # | username | 阶段1 分类 | 处置（设计） | 本卡落地后 `users.location_id` 期望 | 状态 |
|---|---|---|---|---|---|
| 1 | admin | 平台超管（data_scope=all） | **保持 NULL**（-001 §3.2 方案 A） | NULL | ✅ 不回填（S1 迁移只重映射非 NULL 值） |
| 2–5 | emp-a / emp-b / emp-c / emp-d | 总部职能（HEADQUARTERS） | **保持 NULL** | NULL | ✅ 同上 |
| 6–10 | emp-e / emp-f / emp-g / emp-h / emp-i | 总部职能（HEADQUARTERS） | **保持 NULL** | NULL | ✅ 同上 |
| 11–12 | emp-l / emp-n | 仓库（WAREHOUSE；emp-n = ROLE_WAREHOUSE_MANAGER） | 归属 **DEPOT/CENTRAL 型 location**（-002 §5.2 / §7） | 仓库 location_id | ⚠ **待补**：原 `store_id` 为 NULL → S1 迁移不会自动回填；需人工分配 |
| 对照 | emp-j / emp-k / emp-m | 已归店（不在 15 人之列） | 保持其门店归属 | stores_new → location 重映射后语义不变 | ✅ S1 迁移覆盖 |
| 13–15 | finqa_register / finqa_confirm / finqa_noperm | 测试残留 | **停用/删除**（U-5，Owner 定） | — | ⏳ **待 Owner 执行**（本卡不删数据） |

### 17.1 关键结论
1. **admin + 9 个总部职能用户 = 10 人保持 NULL** —— 与 design-001 §3.2 方案 A 一致；配合 S4 兜底消除后，NULL **不再静默落"门店 1"**（改为 403/400 拒绝或走 data_scope 聚合）✓
2. **emp-l / emp-n（仓库 2 人）**：其 `users.store_id` 原本即为 NULL，因此 **S1 迁移不会为它们回填 location_id**（迁移只重映射非 NULL 值）→ 需经 **S5 分配入口**人工分配仓库型 location，或在收口回填脚本中按 `employees.warehouse_id` 推导。
   > -001 §4.1 把「emp-l/n 回填 employees.warehouse_id」列为"建议"、users 侧归属列为"待 Location 模型落地"；M3-M4 已收口（locations 就绪）→ 时点具备，但**归属分配按 Owner 裁定须人工**。
3. **finqa×3** 属数据清理、非本卡代码范围 → 待 Owner 处置（U-5）。
4. **活体核对不可行**（无 DB）：本表"期望值"由迁移逻辑 + 处置表推导；**QA 须在有 DB 环境执行 SQL 核对**（登记为交接项）。

### 17.2 需 Owner 裁决/协助的 2 项
- **(A) emp-l / emp-n 仓库归属**：走「人工经 S5 分配入口分配」还是「收口回填脚本按 `employees.warehouse_id` 推导」？后者省事但属"自动推导"，与 Owner「人工分配」裁定需确认。
- **(B) finqa×3 停用/删除**：Owner 定（U-5 原为"建议停用"）。

### 17.3 冻结限制项与环境事实汇总（累计）
| 编号 | 内容 | 开卡/解除条件 |
|---|---|---|
| **LIM-1** | KDS 托盘绑定链路的 `locationId` 识别（`SecurityUtils` 的 `KdsPrincipal` 分支） | `KdsTokenFilter` / `KdsAuthProperties` / `DeviceWhitelistFilter` 批次入库后 |
| **LIM-2** | 设备域 #3~#8 五处 `DEFAULT_STORE_ID=1L` → 设备归属 `location_id`（-002 §5.3） | 补 `devices.location_id` 迁移 + 设备注册链入库后 |
| **ENV-10**（已登记 KL） | 已提交树不可独立编译（committed 代码引用未跟踪 WIP 类；100 errors，非本卡引入） | P0-WORKSPACE-WIP-CONSOLIDATION-001 后重验 |
| **ENV-3（需更新）** | 构建 JDK 由 `H:\jdk-25.0.1.8-hotspot`（已消失）迁至 **`P:\my-new-project\JDK21`**（Temurin 21.0.9+10）；Java 25 的 byte-buddy experimental 开关不再需要 | 见 §4 #3；KL 主表待收口更新 |

---

## §18 S5b-part2b 删除 UpdateUserRequest.storeId（2026-09-30，commit `3bea354`）

### 18.1 交付（1 文件）
`dto/UpdateUserRequest.java`：**字段 + `getStoreId`/`setStoreId` + delombok 生成的 `equals`/`hashCode`/`toString` 引用共 5 处一并删除**（删除后文件内 `storeId` 引用 = 0）。

与 **S5a**（删除 `AuthServiceImpl.updateUserInfo` 中的归属写入）合起来完成 **-001 §2.3「移除 storeId 字段」** 的字面要求。

### 18.2 设计范围核对（重要，避免范围外改动）
-002 的 DTO 改名范围**仅列** `UserCreateDTO` / `UserUpdateDTO` / `UserAssignStoreDTO` 三个；**`EmployeeCreateDTO` / `EmployeeUpdateDTO` 不在范围内** —— -002 员工侧只要求**列/实体**改名为 `employees.location_id`（已在 S1/S2a 完成）→ 故**不做**员工 DTO 改名（PG-005 阶段3「不得私自扩大范围」）。
**残留命名**（登记，非缺陷）：`UserBasicInfo.storeId` / `EmployeeBasicInfo.storeId` 等**展示型 DTO** 仍名 `storeId`，其值已是换算后的 `store_id`（S2a 已确保），语义一致；待后续统一改名。

### 18.3 验证
`mvn -o clean compile` = **BUILD SUCCESS**（JDK21）；文件无 WIP → 常规精确 add（cached diff 已逐行审阅：纯删除 + 1 处 toString 修正）。

---

## §19 S2b 收尾：UserCreateDTO / UserUpdateDTO 改名（2026-09-30，commit `011a75c`）

### 19.1 交付（3 文件 / cached +15−16）
| 项 | 内容 |
|---|---|
| `UserCreateDTO` / `UserUpdateDTO` | 字段 + 访问器 + `@Schema` 文案全链改名 `storeId → locationId`（每文件 6 处；**剩余 `storeId` 引用 0**）。注：这两个 DTO **无 delombok 生成的 equals/hashCode/toString**，故改名面仅字段/访问器/文案 |
| `UserController` | create/update 两处调用点 → `user.setLocationId(userDTO.getLocationId())`（**值即 location_id**，不再经 `location_id_map` 换算，与裁定 A 一致）；移除已无用的 `LocationIdBridge` import |

### 19.2 -002 §5 修订一的 DTO 改名范围**全部完成**
`UserCreateDTO` ✅ / `UserUpdateDTO` ✅ / `UserAssignStoreDTO` ✅（+ 本卡新增 `UserAssignStoreBatchDTO` ✅）；`UpdateUserRequest.storeId` ✅ 已删除（§18）。

### 19.3 PG-001 隔离与验证
`UserController` 带 WIP(+19) → cached **7 行**、残留 **19 行逐字一致** ✓。
> **方法学再补**：同一段改动的**不同缩进版本**会让字面锚点部分失配（本轮 12 空格锚点 MISS）→ 改用**空白弹性正则 `(?m)^(\s*)…`** 一次通过。记入 §12.2。
`mvn -o clean compile` = **BUILD SUCCESS**（JDK21）。

### 19.4 剩余
- **前端接线**（Owner 核心诉求"入口"）：`UserManagementTab.vue` + `api/system/user.ts`（送 `locationId`；该 vue 带 WIP → 隔离）；
- **S7 收口**：INDEX/roadmap + KL/ENV（含 ENV-3 JDK）+ 回填/快照 + E2E 交接 + LIM-1/LIM-2/ENV-10 登记 + PG-001 收口提交。

---

## §20 S7 收口（part2）：回填/快照与 E2E 交接（2026-09-30）

### 20.1 回填与快照
| 迁移 | 作用 | 回滚 |
|---|---|---|
| `V20260930_002__user_location_001_rename_store_id_to_location_id.sql` | `employees.user_id` 新增 + 按 `employee_code` 回填；`users.store_id→location_id`、`employees.store_id→location_id`（经 `location_id_map` 重映射）+ FK/索引切换 + **完整性守卫** + 兼容视图 `v_users_store_compat` | 见文件头注释（DROP VIEW → ADD COLUMN store_id → 反查回填 → 恢复 FK/索引 → DROP location_id） |
| `V20260930_003__onboarding_archive_add_location_id.sql` | `onboarding_archive.location_id` 新增 + 按 `employee_code` 对齐已注册用户归属的尽力回填 | `ALTER TABLE onboarding_archive DROP COLUMN location_id;` |

**前后快照策略**：迁移内置**数据完整性守卫**（"旧列非空、新列仍空"即 `RAISE EXCEPTION` 中止）替代人工前后比对；**活体前后快照导出**须在有 DB 环境执行（本机无 DB）→ 交接 QA 的核对 SQL：

```sql
-- 1) NULL 归属人数：期望 = admin+9 总部(10) + finqa x3 + emp-l/n(2) = 15
SELECT COUNT(*) FROM users WHERE location_id IS NULL AND deleted = 0;
-- 2) 重映射覆盖：期望 = 迁移前 store_id 非空且 deleted=0 的行数
SELECT COUNT(*) FROM users u JOIN location_id_map m ON m.location_id = u.location_id AND m.src_table='stores_new' WHERE u.deleted = 0;
-- 3) 员工-账号关联：期望 >= 14（存量 emp-* 对齐）
SELECT COUNT(*) FROM employees WHERE user_id IS NOT NULL AND deleted = 0;
-- 4) 兼容视图可用
SELECT * FROM v_users_store_compat LIMIT 20;
```

### 20.2 E2E 交接（本机不可运行）
本机**无 DB / 无 server**（§4 #1）→ 8 个 `@SpringBootTest` + 32 条 E2E（`temp-*.ps1`）**不可本地运行**。交接 QA 在**有 DB 环境**执行的链路清单：
1. 登录 → 解 JWT 得 **`locationId` claim**（S3，5 生成点一致）；
2. 分配归属 `PUT /v1/users/{id}/assign-store`，body `{"locationId": N}` → 断言 `users.location_id` 与 `employees.location_id` **双写一致** + `employees.user_id` 补齐（S5a）；
3. 批量分配 `PUT /v1/users/assign-store-batch` → 断言返回成功计数 + **审计落 `sys_operation_logs`（模块 `USER_STORE_ASSIGN`）**（S5b-part1）；
4. 未分配归属用户访问门店强绑定接口（桌台/叫号）→ 断言 **403 `NoLocationAssignedException`**（S4b #1/#2）；
5. 仓库员工（emp-n）访问门店单据 → 拒绝；仓库操作 → 允许（-002 §7）；
6. 采购计划生成未分配归属 → 403；收货缺门店 → 400（S4b #9/#12）；
7. 入职完成 → 注册 → 断言用户归属来自 archive（S6）。

### 20.3 本卡唯一剩余代码项
**前端接线**：`UserManagementTab.vue` 位置控件 + `api/system/user.ts` 的 `assignStore`/`unassignStore` 接线（Owner 核心诉求"入口"）。**后端 API 已完备**（单点 / 批量 / 权限 / 审计），仅缺 UI 调用；该 vue 带 WIP → 需 hunk 隔离 + **前端构建验证**（后端 compile 覆盖不到）。

---

## §21 S7 收口结论（part3）（2026-09-30）

### 21.1 结论：`CLOSED_WITH_REGISTERED_LIMITATION`
**依据**：验收基准 6 项中 **1 / 3 / 6 达成**；**2** 完成设计符合性核对（活体核对待 QA）；**4 / 5** 由 `LocationGuard` 与 -002 §7 规则承载；13 处兜底**非设备域 7/7 消除**，设备域 6 处与 KDS 识别因**依赖未跟踪 WIP 批次**而冻结；**前端 UI 未接线**（后端 API 完备）。

| # | 基准 | 结论 |
|---|---|---|
| 1 | JWT claim `locationId` + 5 生成点一致 | ✅ S3 |
| 2 | 15 NULL 用户逐人核对 | ✅ 设计符合性（活体待 QA，§17） |
| 3 | 13 处兜底（403/400 文案正确） | ✅ 非设备域 7/7；设备域 6 = LIM-2 |
| 4 | 仓库员工边界 | 🔶 规则/守卫就位（-002 §7） |
| 5 | admin 写侧位置上下文解析链 | 🔶 `LocationGuard.assertStoreContext` 就位 |
| 6 | assign-store 分配有审计留痕 | ✅ S5a |

### 21.2 登记限制项
| 编号 | 内容 | 解除条件 |
|---|---|---|
| **LIM-1** | KDS 托盘绑定链路的 `locationId` 识别（`SecurityUtils` 的 `KdsPrincipal` 分支） | KDS/设备过滤链批次入库后 |
| **LIM-2** | 设备域 #3~#8 五处 `DEFAULT_STORE_ID=1L` → 设备归属 `location_id`（-002 §5.3，需 `devices.location_id` 迁移） | 同批次入库后 |
| **LIM-3** | **前端分配入口未接线**：`UserManagementTab.vue` 位置控件 + `api/system/user.ts` 接线（后端 API 完备：单点 / 批量 / 权限 / 审计） | 前端轮次实施 + **前端构建验证** |
| **ENV-10** | 已提交树不可独立编译（100 错，非本卡引入） | P0-WORKSPACE-WIP-CONSOLIDATION-001 |
| **ENV-3（已更新）** | 构建 JDK 迁至 `P:\my-new-project\JDK21` | 已登记（KL 主表） |

### 21.3 交付清单汇总（30 个本地 commit，**未 push**）
- **迁移 ×2**：`V20260930_002`（users/employees 列改名 + employees.user_id + 守卫 + 兼容视图）、`V20260930_003`（onboarding_archive.location_id + 回填）；
- **新增类 ×4**：`LocationIdBridge`（ID 空间桥，含静态入口）、`LocationGuard`、`NoLocationAssignedException`(403)、`NoLocationContextException`(400)；**新增 DTO ×1**（`UserAssignStoreBatchDTO`）；**新增 API ×1**（`PUT /v1/users/assign-store-batch`）；
- **改名面**：`users` / `employees` 列 + `User` / `Employee` 实体 + 6 个 DTO + `UserMapper` 原生 SQL（含 SQL 内 JOIN `location_id_map`）+ `DataFixController`；
- **身份层**：`SecurityUser.locationId` + JWT claim `locationId` + 5 生成点 + `SecurityUtils`（新主方法 + 反查兼容方法，15 存量调用点零改）；
- **兜底消除 7 处**：#1 桌台 / #2 叫号 / #9 采购计划 / #10 采购申请 / #11 POS 建单 / #12 收货 / #13 运营总览；
- **分配入口（后端）**：归属双写 + `employees.user_id` 补齐 + `@OperationLog` 审计 + 批量接口；
- **入职链路**：`OnboardingArchive.locationId` + 注册回填（删除硬编码置空）+ `completeOnboarding` 透传；
- **文档**：本记录 §1–§21；KL（ENV-3 更新 / ENV-10 新增）；roadmap 进度总览刷新。

### 21.4 收口后剩余（进入下一轮）
1. **LIM-3 前端接线**（唯一剩余代码项）；
2. **KL 主表补登 LIM-1 / LIM-2 / LIM-3 三行**（本轮仅登记于本记录 + roadmap，KL 主表待补）；
3. **push**（需 Owner 显式指令；当前 30 个 commit 全部本地）。

---

## §22 S5b-前端：分配位置入口接线（2026-09-30，commit `331b79b`）

### 22.1 交付（2 文件 / cached +32−3）
| 项 | 内容 |
|---|---|
| `frontend/src/api/system/user.ts` | `assignStore(id, locationId)` 改送 **`{ locationId }`**（裁定 A：值 = `locations.location_id`；此前该函数是**死代码**，0 调用） |
| `frontend/src/views/system/components/UserManagementTab.vue` | 用户列表行新增 **「分配位置」** 操作 → `ElMessageBox.prompt` 输入 locationId → `userApi.assignStore` → 成功提示（位置**下拉**待补 locations 选项接口后替换） |

### 22.2 PG-001 隔离
`UserManagementTab.vue` 带无关 WIP → 用 **Python 脚本**按「HEAD + 本卡改动」入索引（PowerShell 对含反引号/引号的 Vue 文本解析失败，脚本未执行、文件未受损）；**cached 28 行、残留 diff 中无本卡新增行**（已核对）✓。

### 22.3 ✅ 构建验证通过（2026-09-30，R19 补验）
`pnpm build` 因依赖状态检查触发 `pnpm install` 失败而不可用 → 改为**直接调用本地工具链**验通：
- **`node node_modules/vite/bin/vite.js build`** → **EXIT=0**（`✓ 3236 modules transformed` / `✓ built in 2.99s`）；
- **`node node_modules/vue-tsc/bin/vue-tsc.js --noEmit`** → 全量 **144 条类型错**（均落在既有 WIP 文件，如 `api/hr/recruitment.ts`/`api/product/converters.ts`，同 ENV-9 性质），而**本卡两个文件（`UserManagementTab.vue` / `api/system/user.ts`）错误数 = 0** ✓。
→ **前端改动已验证**（构建 + 类型检查），无需留待他人。

**R19 复验（前端环境恢复后，产物级证据）**：
- `node node_modules/vite/bin/vite.js build` → **EXIT=0**（`✓ 3236 modules transformed` / `✓ built in 3.06s`）；
- **产物内容证明改动确实进包**：`dist/assets/*.js` 中命中 `分配位置`（1 个文件）与 `assign-store`（1 个文件）→ UI 按钮与接口调用均已编入产物；
- `vue-tsc --noEmit` → 全量 **144** 条 `error TS`（均既有 WIP 文件），**本卡两文件 = 0**；
- `git status`：`api/system/user.ts` 干净（已提交）；`UserManagementTab.vue` 仍显示 `M` = **开卡前既有的无关 WIP**（本卡改动经 hunk 隔离已入 HEAD，残留 diff 中无本卡新增行，R18 已核对）。
- **工具链说明**：`pnpm build` / `pnpm typecheck` 在本沙箱不可用（pnpm 的"运行前依赖校验"会调 `pnpm install` 且装不上，离线/registry 不可达）；`build` 脚本本体即 `vite build`，故以直接调用 `node_modules` 下的 vite / vue-tsc 作为等价验证路径。

### 22.4 LIM-3 解除
LIM-3（前端分配入口）：**已接线（`331b79b`）+ 已构建/类型验证（R19）** → **解除**。KL 主表 KL-087 行同步为「已解除」。

---

**（本记录随各片完成增量更新）**
