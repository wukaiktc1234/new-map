# P1-USER-LOCATION-002 实施记录（-001）

- **卡片**：P1-USER-LOCATION-002（任务板 §24.3m）——关键字段入口收敛二期：桥正向入口 / 员工归属 locationId 驱动 / 反向同步 / N3·N4·N7·N8·N12
- **日期**：2026-09-30 阶段 3 开工
- **判档**：**重档**（PG-006）→ PG-005 三阶段 + 本实施记录 + 更新 INDEX/roadmap + KL/ENV 登记 + PG-001 精确提交
- **依据**：
  - 阶段 1 诊断：`docs/quality/p1-user-location-002-diagnosis-001.md`（含放行记录 + U1–U8 裁决记录）
  - 阶段 2 设计：`docs/design/p1-user-location-002-design-001.md`（D1–D9；§7 U1–U8 全部已裁）
  - 上游语义：`docs/design/location-organization-separation-design-002.md` §7（ID 空间规则 3/4）
- **阶段 3 放行方式（Owner 裁决）**：**分批交验** —— 先做 **C1–C2**，交 Owner Windows 侧编译 + 单测验证，通过后再继续 C3–C10。

---

## §1 三阶段状态

| 阶段 | 状态 | 产物 |
|---|---|---|
| 阶段 1 诊断（只读） | ✅ 完成 | `p1-user-location-002-diagnosis-001.md` |
| 阶段 2 设计（只出方案，禁改代码） | ✅ 完成（U1–U8 全裁） | `p1-user-location-002-design-001.md` |
| 阶段 3 实施 | **▶ 进行中：C1–C5、C8、C10、C7、C6 已提交（C10/C7/C6 均已收口 2026-10-09，§7/§7.5/§7.6）；C9 未开始** | 本记录 |

**阶段 3 边界（PG-005）**：允许改代码 / 补测试 / 补实施记录；**禁止跨阶段顺手修、禁止扩大范围**。本卡 I-4 已定：**无 schema 变更**，唯一允许的 DB 动作是 C3 的 1 条 seed 迁移（3 个权限码）。

---

## §2 已实施提交（逐提交可独立 revert）

| 提交 | 内容 | 文件面 | 依据 | 验收状态 |
|---|---|---|---|---|
| `aff2568` **C1** | 桥补正向入口：`toLocationId(String srcTable, Long srcId)`；旧 `toLocationId(Long storeId)` 保留签名、委托 `SRC_STORES_NEW`；新增 `static locationIdOfWarehouse` | `common/util/LocationIdBridge.java` + 新测试 `LocationIdBridgeTest`（6 例） | 设计 §2 D1-1、§6 A1 | ⏸ 待 Windows 侧（ENV-11） |
| `ff4cba3` **C2** | 员工 DTO 加 `locationId`（唯一归属入参），`storeId`/`warehouseId`/`workLocationType` 标 `@Deprecated` 保留；`EmployeeServiceImpl.resolveLocationId` 优先级链 + 存在性校验；create/update 两处写入改走它；新增 `LocationMapper` 构造注入 | `dto/EmployeeCreateDTO.java`、`dto/EmployeeUpdateDTO.java`、`service/impl/EmployeeServiceImpl.java` + 新测试 `EmployeeServiceImplLocationResolveTest`（9 例） | 设计 §2 D1-2 / D1-3、§3 改动清单、§6 A2 | ✅ Windows 侧交验通过（2026-10-09） |
| `acb9aa8` **C2b** | D1-4 落点：`workLocationType` 显式推导——新增 `mapLocationTypeToWorkLocationType`（U9 映射）+ `deriveWorkLocationType`；创建路径无 locationId → 显式 `HEADQUARTERS`（U10，不走 DB DEFAULT）；更新路径未表达归属意图 → 不 set（I-3）；`TRANSIT`/词表外 → 400 | `service/impl/EmployeeServiceImpl.java` + `EmployeeServiceImplLocationResolveTest`（9→19 例，+10 C2b 用例） | 设计 §2 D1-4、§7 U9/U10、F-1/F-2/F-3 裁决（§3.4） | ✅ 编译 + 定向测试 25/25 绿（2026-10-09，JDK21 + Maven 3.9.11） |
| `ddb7d08` **C3** | 员工归属分配端点 `PUT /v1/employees/{id}/assign-location`（D3-2）：新 DTO `EmployeeAssignLocationDTO`（`locationId` `@NotNull`）；`assignEmployeeLocation`（id 解析 → 404 → `resolveLocationId` → `deriveWorkLocationType` → 只写归属两列 → 审计 → 详情）；权限码 `hr:employee:assign-location` **5 面逐字一致**（AdminPermissions / init-data.sql / seed 迁移 / permissions.ts / `@PreAuthorize`+`@RequiresPermission`）；审计模块 `EMPLOYEE_LOCATION_ASSIGN`（controller `@OperationLog` + 服务级动态行 desc 含旧值→新值）；seed 迁移 `V20261009_001`（3 权限码，仅绑 `ROLE_HR_DIRECTOR`） | 4 新文件（DTO / 迁移 / 2 测试类）+ 7 修改（controller / 接口 / impl / AdminPermissions +2 行 / init-data.sql / permissions.ts +1 行 / C2b 测试 6 参构造） | 设计 §2 D3-2/D3-3/D3-4、§7 U3/U7、§6 A4 | ✅ 编译 + 定向测试 29/29 绿（2026-10-09）；A4 live HTTP 部分待 Owner（ENV-11） |
| `e97db65` **C4** | 反向同步 `syncUserAssignmentFromEmployee`（D3-1，impl 私有方法，不上接口）：关联键 `employees.user_id` 优先 / `employee_code` 兜底（`selectOne` + `LIMIT 1` 防多行异常）；U1 last-write-wins（审计 desc 记 users 侧旧 → 新）；U2 无用户不动 users、不建号、不报错；**部分更新**仅 id+locationId+updatedTime（防 `password` 覆盖）；调用点 `updateEmployee`（locationId 非空）+ `assignEmployeeLocation`；`logLocationAssignAudit` 4→5 参（syncDesc）；构造 6→7 参（注入 `UserMapper` 而非 UserService，避免与 `UserServiceImpl` 成环）；D3-2 防写回环：双向 javadoc 互参（入口决定方向） | 4 文件（impl + `UserServiceImpl` 注释 + 2 测试文件），+231/−7 | 设计 §2 D3-1/D3-2、§7 U1/U2 | ✅ 编译 + 定向测试 35/35 绿（2026-10-09） |
| `563c3e8` **C5** | 前端员工表单归属收敛为单一 locations 下拉（D2，N6 消解）：`EmployeeFormDialog` 两旧下拉（归属类型 + 门店/仓库实体）→ 单一 locations 下拉（`useLocationOptions`，`GET /v1/locations`）；TRANSIT 前端先行过滤（U9，后端 C2b 亦 400）；「归属类型」降级只读展示（由所选 locationType 推导）；载荷仅发 `locationId`（N6：number ↔ 后端 Long），`workLocationType`/`storeId`/`warehouseId` 停止发送（后端 C2b U9/U10 推导为权威来源）；`HREmployee` 本地接口 +locationId/−storeId/−warehouseId，展示名按 locationId 响应式解析（回退总部 U10），`toBackendPayload` 仅发 locationId，StatusTag 经 `displayWorkLocationName` | 2 文件（`EmployeeFormDialog.vue` + `HREmployee.vue`），+60/−77 | 设计 §2 D2（N6 消解）、§7 U9/U10 | ✅ vue-tsc 改动文件 0 错误（2026-10-09）；live UI 待 Owner（ENV-11） |

### C1 落地事实（与设计的差异登记）

- 设计写"新增实例方法 `toLocationId(srcTable, srcId)`"，实现按此执行；**实测 `toLocationId` 的外部调用点 = 0**（`grep` 全后端），故"零调用点冲击"由推测变为事实。
- 空入参（`srcTable` 空/`srcId` null）→ 返回 null 且**不触库**；单测用 `verify(never())` 锁死该行为。
- 静态字段 `LocationIdBridge.instance` 构造即自注册 → 测试内显式构造桥并在 `@AfterEach` 用 `ReflectionTestUtils` 清空，防静态状态泄漏到其它测试类。

### C2 落地事实

- `resolveLocationId` 优先级：`locationId` → `storeId`（桥）→ `warehouseId`（桥，**缺口①新路径**）→ `null`。
- `locationId` 存在性校验用 `LocationMapper.selectById`：`null` 或 `deleted=1` → `BusinessException(400, "归属位置不存在或已删除：…")`。
  - **注意**：`Location` 实体**没有** `@TableLogic`（`entity/Location.java` 无该注解），故 `selectById` 会返回逻辑删除行 → 必须显式判 `deleted`。此为实施期核实到的事实，设计未写。
  - `status=0`（停用位置）**只记 warn，不拒绝**：拒绝与否属后续卡决策，本卡不扩权。
- 返回 `null` 时 MyBatis-Plus `updateById` 默认跳过该列 → "请求未表达归属意图"不会被误清空（与改前行为一致）。
- 构造签名新增第 5 参 `LocationMapper`：**实测全仓无 `new EmployeeServiceImpl(` 手工调用点（0 处）**，仅 Spring 装配受影响。
- 旧入参 getter 已标 `@Deprecated` → 在 `createEmployee`/`updateEmployee` 上加 `@SuppressWarnings("deprecation")` 抑制观察期兼容告警（`maven-compiler-plugin` 无 `-Werror`，pom :510-521 已核实）。

### C2b 落地事实

- **两个新方法**（`service/impl/EmployeeServiceImpl.java`，`resolveLocationId` 之后）：
  - `static mapLocationTypeToWorkLocationType(String locationType)` —— U9 映射：`STORE→STORE`、`CENTRAL/DEPOT→WAREHOUSE`；`TRANSIT`/词表外（含 null）→ `BusinessException(400, "该位置类型不可作为员工归属：…")`。静态 + 包级可见，直接可单测。
  - `private deriveWorkLocationType(resolvedLocationId, locationIdParam, storeId, warehouseId)` —— locationId 路径主键回查走 U9 映射；桥路径（`locationIdParam == null`）**按来源推导**：`storeId→STORE`、`warehouseId→WAREHOUSE`，**不查库**（与 C2 桥分支"不做存在性校验"语义一致）。
- **创建路径**：`locationId == null → 显式 "HEADQUARTERS"`（U10：§3.2 总部职能 NULL 场景，非 DB DEFAULT 的意外结果）；非空 → `deriveWorkLocationType`。
- **更新路径**：`locationId == null → 不 set`（null → MyBatis-Plus `updateById` 跳过该列，存量值不被清空，I-3）；非空 → `deriveWorkLocationType`。
- **DTO `workLocationType` 字段保留**（`@Deprecated`），但后端已成为权威来源：客户端传值不再影响落库（C5 停发的前置条件成立）。
- **locationId 路径多一次 `selectById`**：`resolveLocationId` 已查过一次存在性，`deriveWorkLocationType` 再查一次取 `location_type`。同事务内两次主键查询，代价可忽略；换来的是方法边界清晰（映射方法只依赖入参，静态可测）。
- **单测 9→19**（+10）：STORE/CENTRAL/DEPOT 三分支映射、TRANSIT 400、词表外 400（`FACTORY` 造词）、创建无归属→HEADQUARTERS、桥 storeId→STORE / 桥 warehouseId→WAREHOUSE（均带 `verify(locationMapper, never())` 锁"不查库"）、更新路径 STORE 推导（`ArgumentCaptor` 抓 `updateById` 实参）、更新无归属→`workLocationType == null`（I-3）。
- **验收（Windows 侧，2026-10-09）**：`mvn test -Dtest=LocationIdBridgeTest,EmployeeServiceImplLocationResolveTest` → **Tests run: 25, Failures: 0, Errors: 0**（6 + 19）。JDK21（Temurin 21.0.9）+ Maven 3.9.11。
- **§3.5 数据侧待核项未动**（回填属独立决策）；SQL-2a/2b 已核 = 0 行（无 TRANSIT 脏数据、无存量不一致），C2b 数据前置干净。

### C3 落地事实

- **权限码 5 面**（`hr:employee:assign-location` 逐字一致，`EmployeeAssignLocationEndpointTest` 锁死）：
  1. `AdminPermissions.ALL`（后端注册面，+注释 +2 行）；
  2. `init-data.sql` 权限行 `PERM_HR_EMP_ASSIGN_LOCATION` + §1.3.7 幂等绑定块（`ROLE_HR_DIRECTOR`，`ON CONFLICT DO NOTHING`）；
  3. seed 迁移 `V20261009_001__seed_hr_employee_assign_location.sql`（U3：3 码同一迁移——`hr:employee:assign-location` + `system:user:assign-store` + `system:user:unassign-store`；U7：仅绑 `ROLE_HR_DIRECTOR`，**不**含 `ROLE_STORE_MANAGER`；假设 `ROLE_HR_DIRECTOR` 已存在——`V20260728_011` 先例，迁移内不 CREATE）；
  4. `frontend/src/utils/permissions.ts` 权限注册（+1 行）；
  5. `EmployeeController.assignLocation`：`@PreAuthorize("hasAuthority('hr:employee:assign-location') or hasAuthority('*')")` + `@RequiresPermission("hr:employee:assign-location")`（`PermissionAspect` 既有链 → `BusinessException(403, "权限不足: …")`）。
- **端点契约**（与 `PUT /v1/users/{id}/assign-store` 对称）：请求体 `EmployeeAssignLocationDTO { locationId: Long @NotNull }`（`@NotNull` 落字段级，测试用 `getDeclaredField` 断言）；响应 `Result<Employee>`（详情，部门/岗位名称填充）；员工不存在 → 404；id 非法 → 400。
- **服务流**（`EmployeeServiceImpl.assignEmployeeLocation`，`@Transactional(rollbackFor=Exception)`）：id 解析（失败→400）→ `getById`（null→404）→ `resolveLocationId(locationId, null, null)`（C2 链：不存在/已删除→400）→ `deriveWorkLocationType`（C2b U9：`TRANSIT`/词表外→400）→ 只写 `location_id` + `work_location_type` 两列（附 `updatedTime`/`updatedBy`）→ 审计 → `getEmployeeDetail`。**其余档案字段一律不动**（D3-2 与 `updateEmployee`「改档案」职责分离）；归属入参走 `locationId` 唯一入口，不传 `storeId`/`warehouseId`。
- **审计 D3-4 双层**：
  - controller：`@OperationLog(module = "EMPLOYEE_LOCATION_ASSIGN", type = UPDATE, desc = "员工归属变更（含反向同步 users.location_id）")`——静态注解行（A4 grep 点）；
  - 服务级：`logLocationAssignAudit` 动态补行，`desc = "员工归属分配：locationId 旧 → 新；workLocationType 旧 → 新"`（静态注解 desc 无法携带旧值；参照 `TaxRecordServiceImpl#logIdempotencyHit` :119-143「审计失败仅告警不阻断」先例，单测锁死）；操作人取 `SecurityContextHolder`（测试线程无上下文 → 容忍 null，不阻断）。
  - `users.location_id` 反向同步为 **C4 接入点**（D3-1），接入后本审计行文案保持。
- **构造 5→6 参**（新增 `OperationLogService`）：`EmployeeServiceImplLocationResolveTest` 同步修 6 参构造 + `@Mock OperationLogService`；全仓无其他 `new EmployeeServiceImpl(` 手工调用点（C2 落地事实延续）。
- **单测 +10（19→29）**：
  - `EmployeeServiceImplAssignLocationTest` 6 例：STORE 位置成功（`ArgumentCaptor` 断言 `updateById` 恰写两列 + 审计行 `desc` 含旧→新 + module `EMPLOYEE_LOCATION_ASSIGN`）；`TRANSIT`→400 且不写不审计；未知位置→400；员工 404；**审计失败不阻断主流程**（`updateById` 照常）；id `"abc"`→400。
  - `EmployeeAssignLocationEndpointTest` 4 例：端点注解五断言（`@RequestMapping("/v1/employees")` + `PUT /{id}/assign-location` + `@RequiresPermission` + `@PreAuthorize` 含码 + `@OperationLog` 模块）；DTO `@NotNull` 字段级；`AdminPermissions.ALL` 含码；**5 面文件面一致性**（`init-data.sql` 权限行 + 1.3.7 绑定、迁移文件 3 码 + `ROLE_HR_DIRECTOR` 正 / `ROLE_STORE_MANAGER` 负、`permissions.ts` 含码）。
- **交验（Windows 侧，2026-10-09）**：`mvn test -Dtest=EmployeeServiceImplAssignLocationTest,EmployeeAssignLocationEndpointTest,EmployeeServiceImplLocationResolveTest` → **Tests run: 29, Failures: 0, Errors: 0**（6+4+19），`MVN_EXIT=0`。JDK21（Temurin 21.0.9）+ Maven 3.9.11。
- **A4 live 部分**（无权限 403 / 有权限 200 + `operation_log` 出现 `EMPLOYEE_LOCATION_ASSIGN` 行且 desc 含旧→新）：403 强制链 = `PermissionAspect` 既有机制（与 `assign-store` 同码先例）；200 + 审计行路径已被单测锁死；live HTTP 归 Owner（ENV-11 清单，与 C2b 交验模式一致）。
- **提交精度**：工作树含 Owner WIP（`AdminPermissions.java` / `permissions.ts` 中 P0-SEC-001-D 等混行），本提交用 **hunk 级 `git apply --cached`** 精确暂存（两文件各只进 C3 行，`apply --check` 预检通过），`git diff --cached --stat` 核对 **11 files / +607 −2**；Owner WIP 全部保持未暂存。

### C4 落地事实

- **方法落点**：`EmployeeServiceImpl` **私有方法** `syncUserAssignmentFromEmployee(Long employeeId, Long newLocationId)`（设计 §2 D3-1「在 EmployeeServiceImpl 内」；未加 `EmployeeService` 接口）。返回 users 侧审计 desc 片段；任何分支**不抛错**（主流程不中断），失败语义以「未动」片段入审计。
- **关联键**（D3-1）：`employees.user_id` 优先（`userMapper.selectById`）；为 null 时 `employee_code` 兜底（`selectOne(eq("employee_code", ...)).last("LIMIT 1")`——PG 下 selectOne 命中多行会抛 TooManyResults，`LIMIT 1` 防护；与正向同步 `employee_code` 先例一致）。
- **U2 边界**：无对应用户 → debug 日志 + 返回「未找到对应用户，users 未动（U2）」，与 `userMapper` **零交互**（单测 `verifyNoInteractions` 锁死）；员工侧写入 + 审计照常。
- **U1 last-write-wins**：`Objects.equals(user.locationId, newLocationId)` → 跳过写入、返回「未重复写」（no-op 不静默，审计 desc 如实记录）；否则**部分更新**——`new User()` 只 set `id` / `locationId` / `updatedTime`，**绝不整实体写**（`User` 含 `password` 字段）。
- **注入选择**：构造第 7 参 `UserMapper`（**非** `UserService`）——`UserServiceImpl` 已依赖 `EmployeeService`（`syncEmployeeAssignment` 调 `employeeService.updateById`），反向注入服务即成环；Mapper 注入无依赖环。
- **D3-2 防写回环**：入口决定方向——用户入口（assignStore / unassignStore）→ 正向同步写 employees 表；员工入口（`updateEmployee` 归属变更 / `assignEmployeeLocation`）→ 反向同步写 users 表。每个方向各写对侧表一次，被调方法内不再反向触发（反向同步方法自身不写 employees 表）。双向 javadoc 互参（本方法 ↔ `UserServiceImpl#syncEmployeeAssignment`）。
- **`updateEmployee` 调用守卫**：仅 `locationId != null` 时调用（表达了归属意图才同步；null = 未表达 = 不动 users，与 C2 I-3「未表达不 set」语义一致）。
- **`logLocationAssignAudit` 4→5 参**：`String syncDesc`，null/空白安全追加（`；<users 侧旧 → 新 / 未重复写 / 未动>`）；C3 审计行原文案保留（C3 落地事实「接入后本审计行文案保持」兑现）。
- **单测 +6（29→35）**：
  - `EmployeeServiceImplAssignLocationTest` 6→10：userId=9 + user.locationId=12 → 写 7（captor 断言部分 User id=9/locationId=7 + 审计 desc 含 `12 → 7`）；userId=null + code=E001 → `selectOne` 兜底路径写 44 号用户；无用户 → `verifyNoInteractions(userMapper)` + 不抛 + 审计 desc 含「未找到对应用户」；用户已是新归属 → `never().updateById` + 审计 desc 含「未重复写」。
  - `EmployeeServiceImplLocationResolveTest` 19→21：`updateEmployee` 归属变更（userId=9，2→500）→ 反向同步写 500（U1 覆盖用户既有归属）；`updateEmployee` 未表达归属 → `verifyNoInteractions(userMapper)`。
  - 既有 29 例全绿：其 Employee fixture 的 userId/employeeCode 均为 null → 新路径走 U2 片段、零 userMapper 交互；desc 断言均为 `contains`，追加片段不破坏。
- **交验（Windows 侧，2026-10-09）**：`mvn test -Dtest=EmployeeServiceImplAssignLocationTest,EmployeeServiceImplLocationResolveTest,EmployeeAssignLocationEndpointTest` → **Tests run: 35, Failures: 0, Errors: 0**（10+21+4），`MVN_EXIT=0`。JDK21（Temurin 21.0.9）+ Maven 3.9.11。
- **提交精度**：4 files / +231 −7；`UserServiceImpl` 仅 javadoc 注释（Owner WIP 行不在该 hunk，无冲突）；Owner WIP 保持未暂存。
- **回退**：无 schema 变更（纯 Java），`git revert e97db65` 独立，不影响 C3/C2b/C2/C1。

### C5 落地事实

- **单一数据源**：`GET /v1/locations`（复用既有 composable `useLocationOptions(true)`，与门店侧 / `UserManagementTab` 同源），替代两旧数据源 `/v1/stores/active` + `/v1/warehouses`。**两旧端点保留未删**（D2「不做」项 = 回滚退路；其他消费方未盘点）。
- **单下拉 + 类型标签**：option 标签 = `locationName（locationType 中文标签）`（`STORE→门店 / CENTRAL→中央仓 / DEPOT→仓储仓`），`:value="opt.locationId"`（number ↔ 后端 Long，**N6 消解**）；「归属类型」从下拉降级为**只读展示**（`el-input readonly`，值由所选 `locationType` 推导；未选 →「总部（未选位置 = 总部职能）」）——「先选类型再选实体」的交互不复存在。
- **TRANSIT 过滤**：`employeeLocationOptions = locationOptions.filter(l => l.locationType !== 'TRANSIT')`（U9：TRANSIT 禁止作员工归属——前端第一道防线；后端 C2b `mapLocationTypeToWorkLocationType` 对 TRANSIT 亦 400，双保险）。
- **载荷收敛**：`toBackendPayload` 仅发 `locationId`；`workLocationType`/`storeId`/`warehouseId` 停止发送——后端 C2b U9/U10 显式推导为权威来源（C5 硬前置已满足：`acb9aa8` 交验通过）。`api/hr/employee.ts` 的 payload 为透传 spread，**无需改码**（收敛在 `HREmployee.vue` 载荷构造侧完成，与 D2 改动清单一致）。
- **展示名响应式**：`fromBackendEmployee` 是快照转换器——若列表先于 locations 加载完成，快照名会停在回退值 → 模板级 `displayWorkLocationName(row)`：`locationNameById(row.locationId)`（读 `locationOptions` Ref，响应式，加载完成后自动刷新）→ 回退快照名 →「总部」；快照转换同时保留（`workLocationName`）作兜底。
- **`types/hr.ts` 未改**：`Employee` 接口带 `[key: string]: unknown` 索引签名，`locationId` 加入无需类型变更（与 D2 清单一致）。
- **验证（Windows 侧，2026-10-09）**：`vue-tsc --noEmit` → 两改动文件 **0 错误**（全项目 144 条均在其它 WIP 文件 = 既有，无一涉及本卡）；功能代码对 `storeId`/`warehouseId` 引用 = 0（仅 2 处 C5 说明注释）；**`vite build` 通过**（Owner 指定命令 `node node_modules/vite/bin/vite.js build`，`VITE_EXIT=0`，`✓ built in 3.99s`；告警仅 `INEFFECTIVE_DYNAMIC_IMPORT`（`src/utils/device-id.ts`，既有 WIP）+ chunk 体积提示，均与 C5 两文件无关）。
- **提交精度（hunk 级隔离，沿用 C3 先例）**：工作树含 Owner WIP 混入两文件（`submitHandler` 机制 / `:teleported` 翻转 / `:lock-scroll` 移除 / 统计 error toast）→ 脚本从全量 diff 抽取 C5 hunk（dialog 11 + HREmployee 8，混合 hunk 按 `locationId` 行拆分），`git apply --cached` 应用（`--check` 预检通过）；`git diff --cached --stat` 核对 **2 files / +60 −77**；Owner WIP 全部保持未暂存；**暂存态自洽核对**：`submitHandler` 0 处 / `emit('submit')` 1 处（HEAD 原 handleSubmit）/ `@submit` 1 处——提交后的文件不依赖任何未提交 WIP。
- **回退**：纯前端，`git revert 563c3e8` 独立；旧端点未删 → 回滚后表单可直接恢复（D2 回滚路径兑现）。
- **live 验证（待 Owner，ENV-11）**：员工表单「归属位置」下拉显示 locations（含类型标签，无 TRANSIT）；选门店提交 → `employees.location_id` 非 NULL 且 `work_location_type='STORE'`（C2b 推导生效）；不选提交 → `HEADQUARTERS`（U10）；列表「工作归属」列显示位置名称。

---

## §3 实施期新发现（F-1 / F-2 / F-3 —— **已全部裁决，2026-09-30 第四批**）

> 裁决前一律未擅自补做（PG-005 不扩范围）。裁决结果见 §3.4。

### F-1 —— 设计 D1-4 在 D9 提交切分中**没有落点**

设计 §2 D1 第 4 条：「`workLocationType` 不再作为写入依据，改为从 `locations.location_type` 推导」。但 §2 D9 的 C1–C10 中，**没有任何提交承载 D1-4**（C2 只列 D1-2/D1-3）。本记录按 PG-005 **不擅自补做**，登记为缺口。

### F-2 —— 两侧词表被 DB CHECK 硬约束，推导需要映射裁决

| 侧 | 取值域 | 出处 |
|---|---|---|
| `locations.location_type` | `STORE / CENTRAL / DEPOT / TRANSIT` | `V20260927_001__create_locations_and_location_id_map.sql` :17、:29（CHECK）、:33（注释） |
| `employees.work_location_type` | `STORE / WAREHOUSE / HEADQUARTERS`，**DEFAULT `'HEADQUARTERS'`** | `V20260721_001__purchase_arrival_restructuring.sql` :16-17、:20 |

两套词表不重合，"从 `location_type` 推导"必须显式映射，而映射本身是业务语义决策：
- `CENTRAL`（中央仓，采购默认落点）→ `WAREHOUSE` 还是 `HEADQUARTERS`？
- `TRANSIT`（调拨在途虚拟位，一期未启用）→ 映射到什么？还是禁止作为员工归属？

**不猜、不自行定义**（I-3 + PG-005 不扩范围）。

### F-3 —— C5 的隐性数据范围隐患（同一裁决面）

`work_location_type` 有 DB `DEFAULT 'HEADQUARTERS'`，而 MyBatis-Plus 默认写入策略跳过 null 字段 → **传 null 即落 `'HEADQUARTERS'`**。
`common/util/WorkLocationFilterHelper.java` :74 / :103 注释明示「HEADQUARTERS 不限制，可查看全部」，且 :61 `workLocationType == null` 时直接 `return`（不加过滤）。

→ **C5（前端员工表单停发 `workLocationType`）一旦落地，新建员工默认落入"全量可见"数据作用域。** 该隐患在改前已存在（不传即默认），C5 会把它变成普遍状态。

### §3.4 裁决结果（2026-09-30 第四批 · Owner 原话已登记进设计 §7 U9 / U10）

- **F-2 → U9 已裁**：`STORE→STORE`、`CENTRAL→WAREHOUSE`、`DEPOT→WAREHOUSE`；**`TRANSIT` 禁止作员工归属 → 显式拒绝**（词表外值同处理，不猜）。
  Owner 补条：**无 `locationId` → 显式 `HEADQUARTERS`**（对应 design-001 §3.2 总部职能 NULL 场景，也是 `WorkLocationFilterHelper` "不限制"分支的语义承载）。
- **F-3 → U10 已裁**：**`resolveLocationId` 里永远显式 `set workLocationType`——有 `locationId` 走 U9 映射；无 `locationId` → `HEADQUARTERS`；不走 DB DEFAULT。**
- **F-1 已补落点**：新增提交 **C2b**（设计 D9 已插入，依赖 C2；**C5 的硬前置**）。

**裁决后的实际语义收窄效果（如实说明，不夸大）**：
- 有 `locationId` 的员工 → `work_location_type` 由位置类型推导，**不再由客户端传值决定** → 门店/仓库员工不会再落进"全量可见"。这是 F-3 隐患被真正修掉的部分。
- 无 `locationId` 的员工 → 仍落 `HEADQUARTERS` = 全量可见，但**从"DB 默认值的意外结果"变成"显式且有业务语义的选择"**（总部职能）。范围本身未收窄，收窄的是**来源的可解释性**。

### §3.5 裁决引出的数据侧待核项（未实施，登记）

映射生效后，存量数据可能出现 `location_id` 与 `work_location_type` 不一致的行（历史上该字段由前端传值决定）。需核实行数再决定是否回填（回填属独立决策，不在 C2b 内）：

```sql
-- 存量不一致行数（映射口径 = U9）
SELECT e.employee_id, e.employee_code, e.name, e.location_id,
       l.location_type AS actual_type, e.work_location_type AS stored_type
FROM employees e
JOIN locations l ON l.location_id = e.location_id
WHERE e.deleted = 0
  AND NOT (
        (l.location_type = 'STORE'   AND e.work_location_type = 'STORE')
     OR (l.location_type IN ('CENTRAL','DEPOT') AND e.work_location_type = 'WAREHOUSE')
  )
ORDER BY e.employee_code;
```

判读：非 0 行 → 交 Owner 决定「回填」或「保留并登记为已知不一致」；**不自行回填**。`TRANSIT` 型位置若出现在 `employees.location_id` 上 → 属 C2b 上线前必须清零的脏数据（C2b 只拒绝新写入，不改存量）。

---

## §4 未开始提交（C4–C10）

> **推进顺序（Owner 第四批）**：C1+C2 Windows 交验 ✅ → **C2b ✅（`acb9aa8`）→ C3 ✅（`ddb7d08`）→ C4 ✅（`e97db65`）→ C5 ✅（`563c3e8`）→ C6…**。

| 提交 | 状态 | 前置 |
|---|---|---|
| **C2b** 承载 D1-4：`resolveLocationId` 内按 U9 映射显式 `set workLocationType`；`TRANSIT`/词表外 → 400；无 `locationId` → 显式 `HEADQUARTERS`（不走 DB DEFAULT）+ 单测 | ✅ 已提交 `acb9aa8` + 交验通过（定向 25/25 绿，2026-10-09） | C1–C2 交验通过 ✅ |
| **C3** 权限码 5 面 + `PUT /v1/employees/{id}/assign-location` + 审计 + seed 迁移（U3/U7 已裁） | ✅ 已提交 `ddb7d08` + 交验通过（定向 29/29 绿，2026-10-09；A4 live HTTP 待 Owner ENV-11） | C1–C2 交验通过 ✅ |
| C4 反向同步 `syncUserAssignmentFromEmployee`（U1/U2 已裁） | ✅ 已提交 `e97db65` + 交验通过（定向 35/35 绿，2026-10-09） | C2、C3 ✅ |
| C5 前端员工表单合并位置下拉（D2，N6 消解） | ✅ 已提交 `563c3e8`（vue-tsc 改动文件 0 错误，2026-10-09；live UI 待 Owner ENV-11） | C2b（硬前置）✅、C3 ✅ |
| C6 用户创建表单加归属（D6） | **已提交 + 已收口（2026-10-09，`dd6ddcc`）**——创建表单可选位置下拉（复用 `useLocationOptions`，单一数据源 `/v1/locations`）；纯前端 2 文件 +35/−0；vue-tsc 改动文件 0 错误；编辑不加内联入口（仍走 `assign-store`）。前置实测 + 交验 + 纪律事件见 **§7.6** | 无 |
| C7 N3 三处（D4） | **已提交 + 已收口（2026-10-09，`32d493b`）**——3 处全修 + 桥反查 + 实测测试 `@Disabled` 档案化；定向 50/50 绿 + A6 grep 过；N17 表缺失明确划出范围。前置实测 `c7pre-mvn-measure.log`（§7.4）；收口详情 **§7.5** | `32d493b` |
| C8 N4 一处（D5，仅 `PurchaseArrivalServiceImpl:656`） | ✅ 已提交 `0e160d9` + 补丁 `014247b`（**裁决 4：判断顺序改 receiver_type 优先**；定向 5/5 绿，A7/A9 过，2026-10-09） | 无（设备域 KL-086 冻结不动） |
| C9 N8 出参加 `locationId` + 前端消费切换（D7） | 未开始 | 无 |
| C10 N12 冗余收敛 2 处 + `@Deprecated`（D8-2/3，U6 已裁纳入） | ✅ 已提交 `fd87808` + docs `a515b69`（定向 7/7 绿前后一致；**已收口**：2026-10-09 Owner 裁决 = 权限模型有洞、行为变更不可达 → 不回退，见 §7） | 无 |

**§24.3p（POS 未分配位置引导）不在本卡内**，且其前置交集 SQL 未跑 → 未实施（Owner 已裁：等 SQL 结果再实施）。

---

## §5 验收状态（Windows 侧交验，2026-10-09 完成；ENV-11 已解除：本会话在 Windows 侧，JDK21 + Maven 3.9.11 可用）

| 验收项 | 设计出处 | 状态 | 执行方式（实际） |
|---|---|---|---|
| 编译通过 | §6 | ✅ 通过（`mvn test` 全构建 compile + testCompile 成功） | `mvn -f backend/pom.xml test …`（JDK21 Temurin 21.0.9，Maven 3.9.11） |
| `LocationIdBridgeTest` 6 例通过 | §6 A1 | ✅ 6/6 | `mvn test -Dtest=LocationIdBridgeTest,…` |
| `EmployeeServiceImplLocationResolveTest` 通过（C2b 后 19 例） | §6 A2 | ✅ 19/19（C2 9 例 + C2b 10 例） | 同上（定向批次 **Tests run: 25, Failures: 0, Errors: 0**） |
| 仓库员工归属不再恒 NULL（端到端） | §6 A2 / 诊断缺口① | ⏸ 待 Owner 手工 | 员工表单选仓库 → 查 `employees.location_id` 非 NULL（emp-l/emp-n 待 UI 分配） |
| `EmployeeServiceImplAssignLocationTest` + `EmployeeAssignLocationEndpointTest` 通过（C3，10 例） | §6 A4 | ✅ 10/10 | `mvn test -Dtest=EmployeeServiceImplAssignLocationTest,EmployeeAssignLocationEndpointTest,EmployeeServiceImplLocationResolveTest`（定向批次 **Tests run: 29, Failures: 0, Errors: 0**） |
| 三个测试类定向 35 例（C4：Assign 10 + Resolve 21 + Endpoint 4） | §2 D3-1/D3-2、§7 U1/U2 | ✅ 35/35 | `mvn test -Dtest=EmployeeServiceImplAssignLocationTest,EmployeeServiceImplLocationResolveTest,EmployeeAssignLocationEndpointTest`（定向批次 **Tests run: 35, Failures: 0, Errors: 0**，`MVN_EXIT=0`） |
| A4 live：无权限 403 / 有权限 200 + `operation_log` 出现 `EMPLOYEE_LOCATION_ASSIGN` 行且 desc 含旧值→新值 | §6 A4 | ⏸ 待 Owner live（ENV-11） | 单测已锁：403 强制链 = `PermissionAspect` 既有机制（`assign-store` 同码先例）；200 + 审计行 = 服务级单测断言 |
| C5 前端类型检查 + 构建 | 设计 §2 D2 | ✅ 改动文件 0 错误（全项目 144 条 = 既有 WIP，无一涉及本卡）；vite build `VITE_EXIT=0`（`✓ built in 3.99s`，仅既有 WIP 告警） | `vue-tsc --noEmit` + `node node_modules/vite/bin/vite.js build`（frontend，2026-10-09） |
| C5 live：表单单一 locations 下拉（无 TRANSIT）+ 提交后 `employees.location_id`/`work_location_type` 按 C2b 推导 + 列表显示位置名 | 设计 §2 D2、§7 U9/U10 | ⏸ 待 Owner live（ENV-11） | 前端代码路径已锁：payload 仅 `locationId`（grep 0 残留）；推导权威 = 后端 C2b（35/35 绿） |
| 既有测试无回归 | §6 | ✅ **本卡改动面全量复跑全绿 41/41**（`LocationIdBridgeTest` 6 + `EmployeeServiceImplAssignLocationTest` 10 + `EmployeeServiceImplLocationResolveTest` 21 + `EmployeeAssignLocationEndpointTest` 4，各 `Failures: 0, Errors: 0`）；全量 380 tests / 74 失败（15F+59E）= 18 个其它测试类（integration / finance / sysdict / code generator / sorting），无一涉及本卡改动面 | `mvn test` 全量（C5 批次，2026-10-09，`MVN_EXIT=1` 由其它类失败导致）；**354→380 的 +26 = 本卡 C2b/C3/C4 提交的测试**（`acb9aa8`：LocationResolveTest 9→19 **+10**；`ddb7d08`：EndpointTest 4 + AssignLocationTest 6 新建 **+10**；`e97db65`：AssignLocationTest 6→10 + LocationResolveTest 19→21 **+6**；10+10+6=26 与差值精确吻合；基线 354 已含 C1 +6 / C2 +9）；**失败增量 58→74**：`backend/src/test` 工作树仅 1 个 WIP 改动（`PaymentServiceImplTest.java`）+ 环境/DB 状态漂移（10 个 integration 类全 Errors，与 Owner 并发 DB 操作时段吻合）；C1–C4 测试类两轮均绿（旧批次原始日志已清理，逐类对比不可行，如实登记）；C2b 批次定向 25/25；C3 批次定向 29/29；C4 批次定向 35/35 |

| C8 A7：`: 1L` 0 命中 + 未指定收货仓库 → 业务异常不默认仓库 1（裁决 4 后：receiver_type 优先——STORE→store_id / WAREHOUSE→warehouse_id / 空或未知 → 400） | §6 A7 | ✅ `grep ': 1L'` 0 命中；ReceiverKey 5 例全绿（裁决 4 仅改判断顺序，未动 ReceiverKey 归一化） | `mvn test -Dtest=PurchaseArrivalReceiverKeyNormalizationTest`（定向批次 **Tests run: 5, Failures: 0, Errors: 0**，`MVN_EXIT=0`，`c8patch-mvn-targeted.log`，2026-10-09） |
| C8 A9：`DEFAULT_STORE_ID` 计数不变（KL-086 未撞） | §6 A9 | ✅ 9 行不变（`DeviceConstants:1` + `DeviceStatusServiceImpl:6` + `HardwareConfigServiceImpl:2`） | `git grep -c DEFAULT_STORE_ID backend/src/main/java`（2026-10-09） |
| C10 冗余收敛：2 处不再「反查 storeId → 正查 locationId」+ `@Deprecated` 编码约束 | 设计 §2 D8-2/3（U6） | ✅ `SalesOrderServiceImpl` 中 `resolveLocationIdByStoreId` **0 命中**；`StoreInventoryController` 仅余管理员/显式参数路径（非冗余，保留）；`@Deprecated` 位于 `:126`（`getCurrentUserStoreId` 定义 `:127`）；真实调用点 **15→14**；定向 7/7 绿（改动前基线 / 改动后） | `mvn test -Dtest=SalesOrderServiceImplTest`（**Tests run: 7, Failures: 0, Errors: 0**，`MVN_EXIT=0`，`c10-baseline-mvn.log` / `c10-verify-mvn.log`）+ `git grep`（2026-10-09） |

**原登记的本侧未能自验风险点 → 已实跑解除**：
1. `EmployeeCodeGenerator` Mockito mock —— ✅ 实跑通过；
2. `ServiceImpl.baseMapper` 反射注入 —— ✅ 实跑通过；
3. `createEmployee` `this.save` 走 mock `EmployeeMapper.insert` —— ✅ 断言全部成立。

**回退预案（若后续发现问题）**：按提交粒度 revert `563c3e8`（C5，纯前端）→ `e97db65`（C4，纯 Java）→ `ddb7d08`（C3）→ `acb9aa8`（C2b）→ `ff4cba3`（C2）→ `aff2568`（C1），六者独立（C3 seed 迁移为幂等 INSERT，无 schema 变更，随提交整体回退；C5 旧端点未删，回滚后表单可直接恢复）。

---

## §6 边界自检

- 本阶段改动面（C1–C5 合计）：**C1–C2b 3 主文件 + 2 测试文件；C3 新增 DTO + seed 迁移 + 2 测试类；C4 impl 私有方法 + 构造 +2 处调用点 + `UserServiceImpl` D3-2 互参注释 + 2 测试文件；C5 前端 2 文件（`EmployeeFormDialog.vue` + `HREmployee.vue`，+60/−77）**，修改 controller/接口/impl/`AdminPermissions`（+2 行）/`init-data.sql`/`permissions.ts`（+1 行）/C2b 测试，未触碰其他 WIP 基线文件（Owner WIP 混行保持未暂存）。
- **无 schema 变更**；C3 引入 1 条幂等 seed 迁移（`V20261009_001`：3 权限码 + 1 角色绑定，无表结构变更）。
- **未解冻** KL-085（`KdsPrincipal`）、KL-086（设备域 `DEFAULT_STORE_ID`）。
- **未实施 §24.3p 任何内容**（含 B1 语义修正）——该卡独立，且前置 SQL 未跑。
- D1-4 已完成（C2b `acb9aa8`）：F-1/F-2/F-3 登记 → Owner 第四批裁决（U9/U10）→ C1+C2 交验通过 → **裁决后实施，无擅自补做**；§3.5 数据回填仍未实施（独立决策）。
- **C10（`fd87808`）**：改动面 = 3 文件（`SecurityUtils` `@Deprecated`+Javadoc+`hasStorePermission` 抑制；`StoreInventoryController` 门店权限分支直取 `getCurrentUserLocationId()`；`SalesOrderServiceImpl` 出库扣减 locationId 直取上下文 + 方法 Javadoc 同步）；**行为差异（证据登记）**：`deductInventoryForOrder` 对 WAREHOUSE 型位置用户旧 = 无 store 别名 → 恒抛「未分配门店」，新 = 直取位置正常扣减（移除冗余间接的产物；设计 D8「语义等价」对 STORE 用户严格成立）；`StoreInventoryController` 管理员/显式 `storeId` 参数路径行为不变；未新增测试（现有测试 0 覆盖这两处——`SalesOrderServiceImplTest` 7/7 绿前后一致）；D8-1 桥消费清单 15→14（StoreInventoryController 行销账，设计 §2 D8 表为定稿登记面）；14 个存量调用点今后编译出 deprecation 告警 = 预期信号。
- **C8（`0e160d9` + 补丁 `014247b`）**：改动面 = `PurchaseArrivalServiceImpl` 单文件 2 处（`buildAndSaveArrival` 创建期校验 + `increaseInventoryForArrival` 确认期解析）；**裁决 3**：12 单存量 `receiver_type=STORE + warehouse_id=1` **不纠正** = **N14**（任务板 §24.3o；物料混装非门店入货逻辑形态，声明 STORE 是标错）；**裁决 4**：判断顺序改 **receiver_type 优先**（1) 空 → 400（防御，ReceiverKey 入口已按 A2 归一）2) STORE → store_id 必填 3) WAREHOUSE → warehouse_id 必填 4) 未知 → 400；`warehouse_id` 仅 WAREHOUSE 接收方落库——U11-B 未覆盖的顺序变更，Owner 显式裁；无 schema 变更、无数据迁移、无新权限/端点；未新增测试（A7/A9 = grep 断言 + 既有 5 例定向绿）。
- 提交方式：PG-001 精确 `git add` 逐文件 + `git diff --cached --stat` 核对（C2b：2 files / +218 −5；C3：11 files / +607 −2，其中 `AdminPermissions.java` / `permissions.ts` 两混合文件用 hunk 级 `git apply --cached` 精确暂存；C4：4 files / +231 −7；C5：2 files / +60 −77，两混合文件用 hunk 级 `git apply --cached` 精确暂存 + 暂存态自洽核对）。

---

## §7 C10 收口（2026-10-09 Owner 裁决）+ C7 前置实测

### §7.1 Owner 裁决（原文要点）

> C10 收口，但定性改写：不是"顺带修复 vs 越权"——是**"权限模型有洞，行为变更不可达"**。`order:manage` 不在 DB、无角色持有、端点实际仅 admin 可达 → **C10 变更无害（不可达）→ 不回退**。

- **C10 状态**：`fd87808`（code）+ `a515b69`（docs）→ **已收口**（2026-10-09）。
- **行为差异定性**：`deductInventoryForOrder` 对 WAREHOUSE 型位置用户「旧恒抛 → 新可扣减」的差异，因权限门（`order:manage`/`order:view` 活库无行、无角色持有、`user_permissions`=0 行）使端点实际仅 `*` 通配（admin 系角色）可达 → **不可达 → 无害**。

### §7.2 五步追溯证据清单（Owner 指定 5 步，2026-10-09 交付）

1. **调用链**：`SalesOrderServiceImpl:266`（private 定义）← 唯一调用点 `:143`（`completeOrder` `:128`，preparing→completed）← `SalesOrderController:78-83` `POST /v1/sales/order/{id}/complete`。
2. **上游业务动作**：旧版销售订单完成（`SalesOrder`/`sales_order`，store_id 空间）——非 OrderNew（POS/core）；`sales-order-orders-mapping.md:183`、`project-map.md:318`（扣减**门店**库存）。
3. **设计文档操作者语义**：`LocationGuard.java:21-22`（门店单据 → 仓库员工被拒绝）vs `:23-24`（出库列于仓库单据 → 功能权限）vs `project-map.md:318`/`零信任全链路测试方案.md:321`（销售出库归**门店**口径）——**两处证据张力，仅登记不裁**。
4. **守卫调用**：`grep locationGuard\.` backend/src/main → **0 命中**；SalesOrderServiceImpl 内 require*/assert* → 0 命中；S4b 7 处全为直接抛异常；002 设计 :304 引用的 `security/guard/LocationGuard.java` 不存在 → **该动作无任何 location/store/warehouse 守卫**。
5. **权限门**：`SalesOrderController:80` `@PreAuthorize("order:manage" or "*")`；`SecurityUser:58-69` `*` 仅 admin 系角色；`AdminPermissions:76`/`UserDetailsServiceImpl:62` 不含 `order:manage`/`order:view`（仅 `purchase:order:%`）；活库 `permissions` 无该二行、`role_permissions` 368 行无角色持有、`user_permissions` 0 行、`PermissionInitServiceImpl:139/143` 有创建代码但活库未落 → **非 admin 角色当前 403，端点实际仅 admin 可达**。

### §7.3 追溯挖出的三个新发现（已登记任务板 §24.3o，均另立卡）

| 编号 | 内容 | 证据锚点 |
|---|---|---|
| **N15** | SalesOrderController 权限模型失效（`order:manage`/`order:view` 活库无行、无角色持有、端点实际仅 `*` 通配可达） | 追溯步骤 5 全链 + 活库 psql 实测 |
| **N16** | LocationGuard 孤儿类（0 调用点；S4b 7 处全直接抛异常；设计"收敛到守卫"从未落地；设计 :304 引用文件不存在） | 追溯步骤 4 全链 |
| **N17** | InventoryWarningRecord 表名错位（实体 `@TableName("inventory_warning_records")` 复数——活库无此表、无 Flyway 迁移创建；活库仅单数 `inventory_warning_record`（动作日志表，列集不同）与旧表 `inventory_warning`）→ 调度器 `createWarningRecord` 全链运行时必中 `relation does not exist`，被 `:75` catch 吞 → 每小时静默失败 | `InventoryWarningRecord.java:15`；`InventoryWarningScheduler.java:173/:196`；grep migration 0 命中；psql 实测 |

### §7.4 C7 前置实测（D4「阶段 3 前置核实项」，2026-10-09 landed）

- **实测方式**：`InventoryTransientFieldMeasureTest`（MP 3.5.5 `MybatisConfiguration` + `MapperBuilderAssistant` + `TableInfoHelper.initTableInfo` 离线初始化 TableInfo，**无 Spring 上下文、无 DB**）→ `mvn test` **MVN_EXIT=0**。
- **MEASURE 输出**（`c7pre-mvn-measure.log:52-54`）：
  1. `MEASURE_MAPPED_FIELDS` = locationId→location_id, materialId→material_id, …, version→version —— **`warehouseId`/`storeId` 不在映射清单**（MP 3.5.5 对 `transient` 字段整体跳过）；
  2. `wrapper.eq(Inventory::getWarehouseId, …)` → **`MybatisPlusException: can not find lambda cache for this property [warehouseId] of entity [com.foodtraceability.entity.Inventory]`**（解析即抛，非"打到旧列/恒空"）；
  3. 同异常经 SQL 生成路径复现。
- **三处严重度更新（Owner 已确认）**：
  | 处 | 路径 | 改前登记 | **实测后** |
  |---|---|---|---|
  | `ProcurementTraceServiceImpl:443` | 查询 | 违规用法 | **必炸**（`fillInventoryInfo` 在 `buildTraceInfoByOrderId:258` 主链、无 try/catch → 带 warehouseId 的采购单溯源查询运行时 500） |
  | `MaterialArchiveServiceImpl:388` | 写入（新记录） | 违规用法 | **静默丢失**（新库存记录 `location_id` 恒 NULL，违反 (location_id, material_id) 唯一口径，全链路 location 查询漏行） |
  | `InventoryWarningScheduler:188` | 读回（+ N17 表缺失） | 违规用法 | **双重问题**（:188 恒 null + 目标表活库缺失 → 全链在 :173 即炸，:188 当前实为死代码） |
- **实测测试文件处置（Owner 2026-10-09）**：**保留**，加 `@Disabled`（非单元测试，档案性证据），随 C7 提交。
- **活库交叉证据**：`inventory` 表 25 列仅含 `location_id`（**无** warehouse_id/store_id 列）——即便 transient 映射成功，写入也会 PG 报错；旧 DDL（V1.0.0.100:429/447）带 warehouse_id 的建表已被 M3-M4 重建取代。

### §7.5 C7 收口（2026-10-09，`32d493b`）

**提交**：`32d493b`（7 文件 +129/−9；代码 6 + 实测测试 1；代码提交与文档提交分离）。

**修法差异（Owner 开工令「先想清楚 3 处的修法差异」→ 按严重度三分法落地）**：

| 处 | 定性（§7.4） | 修法 | 未映射仓库时语义 |
|---|---|---|---|
| `ProcurementTraceServiceImpl.fillInventoryInfo:443`（读/查询） | 必炸 → 500 | `locationService.resolveByWarehouseId` 解析 + `eq(Inventory::getLocationId, …)`（S6b 同款）；:454 VO `setWarehouseId` 原读 transient 恒 null → 改填入参 warehouseId（本方法正是按该仓库过滤，语义等价） | **早退不填充**（读侧不抛，与 :432 空值守卫同风格） |
| `MaterialArchiveServiceImpl.ensureInventoryForMaterial:388`（写） | 静默丢失 + location_id 恒 NULL | 解析后 `setLocationId(location.getLocationId())`，**删除** `setWarehouseId(warehouseId)`；日志补 locationId | **log.warn + 跳过初始化**（写侧不产生 location_id=NULL 的孤儿记录） |
| `InventoryWarningScheduler.createWarningRecord:188`（读回） | 双重问题（当前死代码，异常先于 :173 触发） | **桥反查**（新增）：`locationService.resolveWarehouseIdByLocationId(inventory.getLocationId())` 落旧空间 `warehouse_id` 列 | **保持 null**（禁止数值兜底；位置未映射 warehouses 来源时不猜） |

**新增桥反查（对称于 C1 正向桥）**：
- `LocationMapper.selectWarehouseIdByLocation(locationId)` — `SELECT m.src_id FROM location_id_map m WHERE m.location_id = ? AND m.src_table = 'warehouses'`；
- `LocationService.resolveWarehouseIdByLocationId(locationId)` + `LocationServiceImpl` 实现 — null 入 → null 出，否则走 mapper；
- 唯一消费点 = 站点 3（站点 1/2 均为正向 warehouse→location，走既有 `resolveByWarehouseId`，零新增路径）。

**范围外（如实登记，不实施）**：N17（`inventory_warning_records` 表活库缺失，§7.3）——C7 不建表、不改 `@TableName`、无 schema 变更；仅保证表可用后 `warehouse_id` 读回正确。N15/N16 同属未启动。

**交验证据**：
- 编译 + 定向批（含全量 test-compile，构造器变更影响面 = 仅 Spring 装配；全仓无 3 个改动类的 `new XxxImpl(` 手工调用点）：`LocationIdBridgeTest` 6 + `EmployeeServiceImplLocationResolveTest` 21 + `InventoryServiceImplLegacyBridgeTest` 7 + `InventoryServiceImplLocationStockTest` 9 + `SalesOrderServiceImplTest` 7 = **50/50 绿，BUILD SUCCESS**（`c7-mvn.log`）；
- **A6 grep（更新版，适配活库现实）**：main 源码 `Inventory::getWarehouseId` / `Inventory::getStoreId` **代码命中 0**（唯一文本命中 = `ProcurementTraceServiceImpl:442` C7 说明注释）；`MaterialArchiveServiceImpl` 无 `Inventory.setWarehouseId`（剩余 `getWarehouseId` 均为 Warehouse 实体合法用法）；`InventoryWarningScheduler` 无 `inventory.getWarehouseId()` 代码命中；实测测试 `@Disabled` 随本提交入档；
- **纪律事件（如实登记）**：`MaterialArchiveServiceImpl` 工作区含 **Owner WIP barcode hunk（:232-234**，`queryDTO.getBarcode()` 查询过滤，非 C7 范围）→ 直接 `git add` 会混入提交 → 以 **index 手术**（`git show HEAD:` 基线 + 行级剔除该 hunk → `git hash-object -w` + `git update-index --cacheinfo`）使暂存版 = HEAD + 仅 C7 hunks；提交后复核：该文件 `git diff` 仅剩 barcode hunk 未暂存，Owner WIP 完整保留。

### §7.6 C6 收口（2026-10-09，`dd6ddcc`）

**提交**：`dd6ddcc`（2 文件 +35/−0；**纯前端，零后端改动**；代码提交与文档提交分离）。

**前置核实（D6 前提，逐项实测）**：
- 后端已就绪：`UserCreateDTO:49` 已有 `locationId`（Long）；`UserController.createUser:74` `user.setLocationId(userDTO.getLocationId())` → 直落 `users.location_id` → **C6 = 纯前端**。
- `UserManagementTab.vue` 已 import `useLocationOptions`（:17）并实例化（:456，「分配位置」对话框用）→ 复用同一实例，无新增 composable。
- 前端 `UserCreateDTO`（`api/system/user.ts:61`）**缺** `locationId` → 本卡补上。

**改动面（2 文件 +35/−0）**：

| 文件 | 改动 |
|---|---|
| `frontend/src/api/system/user.ts` | `UserCreateDTO` +`locationId?: number`（`locations.location_id`，可选） |
| `frontend/src/views/system/components/UserManagementTab.vue`（5 hunks，仅创建路径） | ① `UserFormData` +`locationId: number \| null`；② `formData` 初始化 `null`；③ `handleCreate` 重置 + **按需预载**（`locationOptions` 为空才 `loadLocations()`，与「分配位置」对话框共享同一实例）；④ `handleSubmit` create 载荷 +`locationId: formData.locationId ?? undefined`（未选不发 → 后端 null，不强制归属）；⑤ 模板：组织信息行后插入**创建模式专属**（`v-if="dialogType === 'create'"`）**可选**「归属位置」下拉（filterable/clearable，标签 = `locationName（locationType）`，与分配对话框同构；无 required 规则） |

**范围（按 D6 严格执行）**：
- **编辑表单不加内联位置入口**——编辑仍走既有 `PUT /v1/users/{id}/assign-store`（-001 权限门 + 审计不变）；
- 「分配位置」对话框、用户列表列、后端代码一律未动；无 schema 变更。

**交验证据**：
- `vue-tsc --noEmit`（全项目）：144 个既有错误全部在其它 WIP 文件（recruitment/converters/stockin/…），**本卡 2 个改动文件 0 类型错误**（`c6-vue-tsc.log`，2026-10-09）；
- 暂存面自洽核对：`git diff --cached --stat` = **2 files / +35 −0**；blob 级 diff（HEAD vs C6-only `4612b7a`）逐 hunk 核对 = 仅上述 5 hunks，无其它差异。

**纪律事件（如实登记）**：`UserManagementTab.vue` 工作区含 Owner WIP（`:teleported` ×4、批量导入/导出/删除按钮禁用 + tooltip、注释位移，39+/23−）→ 整文件 `git add` 会混入 → 以 **index 手术**（`git show HEAD:` 基线 + 应用 C6 5 hunks → `git hash-object -w` `4612b7a` + `git update-index --cacheinfo`）使暂存版 = HEAD + 仅 C6 hunks；提交后复核：Owner WIP 全部 hunk 仍完整留在工作区未提交（`git diff` 复核 6 个 WIP hunk 在位）。

**live 验证（待 Owner，ENV-11）**：用户管理 →「新增用户」→ 出现「归属位置」下拉（locations 数据、带类型标签）→ 选中提交 → `users.location_id` 非 NULL；不选提交 → NULL（不强制归属）；「编辑」对话框**无**「归属位置」字段（改归属仍走行内「分配位置」）。

**回退**：`git revert dd6ddcc` 独立（纯前端；后端字段本就就绪，回退不影响任何已提交切片）。
