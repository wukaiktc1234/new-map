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
| 阶段 3 实施 | **✅ 完成：C1–C10 全部已提交 + 全部已收口（C10/C7/C6/C9 均收口 2026-10-09/12，§7/§7.5/§7.6/§7.7）** | 本记录 |

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
| C9 N8 出参加 `locationId` + 前端消费切换（D7） | **已提交 + 已收口（2026-10-12，`803e5ae`）**——2 DTO +`locationId`（新位置空间，直取 `users.location_id` 不经桥）+ `storeId`/`storeName` `@Deprecated`；前端 `auth.ts` +`locationId`；消费点清点 6 处全旧空间/显示 → 0 处切换（D7 取舍）；`permission.ts` 透传一行延后（P0-SEC-006 WIP 耦合）。前置核实 + 交验 + 纪律事件见 **§7.7** | 无 |
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

> **2026-10-12 判档复议（Owner 指令「并行开卡：N15 + N17，后者优先」）**：N15 = **P1 ✅**（Owner 确认）→ **已开卡 board §24.3s**；N16 = P3→**P2**（「设计没落地」，设计债，非普通死代码；待 Owner 开卡指令）；N17 = P1→**P0（生产事故级）**（「如果生产在跑，这是生产事故级」；如确认非生产运行降紧急 P1）→ **已开卡 board §24.3r（优先）**。详见 §7.9/§7.9b。

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

### §7.7 C9 出参契约加新留旧（D7）：前置核实、交验、纪律事件与延后登记（2026-10-12）

- 范围（D7 裁决，不猜）：两个出参 DTO（`UserInfoResponse` = `/v1/auth/me` 契约，`UserBasicInfo` = `/v1/users/basic-info` 契约）**加新留旧 + 标废弃，不删**——新增 `locationId`（新位置空间 `locations.location_id`，**直取 `users.location_id`，不经桥**）；`storeId`/`storeName` 保留 + `@Deprecated` + 观察期兼容注释；前端 `auth.ts` 类型加 `locationId`。删除 `storeId` 待观察期结束、消费点归零后另卡做。
- 前置核实（逐项实测，不猜）：
  - 构建点 2 处：`AuthServiceImpl.getUserInfo:312-325`（`updateUserInfo:606` 委托 `getUserInfo` → 同一改动覆盖）+ `UserDataServiceImpl.convertToBasicInfo:144-158`（全仓 grep = `UserBasicInfo` 唯一构建点；全参构造器唯一外部调用点 = 内部 builder `build()`，已同步加参）。
  - **消费点全量清点（D7 阶段 3 前置：未清点禁止删字段）——6 处用户契约消费全部为旧门店空间或门店名显示，0 处需要新位置空间 → 全部保持原样**：
    1. `stores/permission.ts:408-409` `accessibleStores = [userInfo.storeId]`（旧门店空间）；
    2. `stores/permission.ts:496-497`（**Owner WIP**）`/me`→`info` 映射 `storeId`/`storeName`（旧门店空间）；
    3. `views/store-ops/StoreReceiving.vue:29` `userInfo?.storeName` 显示；
    4. `views/employee/SelfServicePortal.vue:48` `userInfo?.storeName` 显示；
    5. `views/personal/PersonalCenter.vue:234` `userInfo?.storeName` 显示；
    6. `views/store-ops/ReceivingMobile.vue:648` `me?.storeName` 显示（直连 `/me`）。
  - `types/index.ts` 旧 `UserInfo`（`storeId?: string`）非 `/me` 契约类型、无本卡消费点 → 不属 D7 范围；`types/auth.ts`/`utils/auth.ts` 的 `UserInfo`/`UserInfoResponse` 无 `storeId` 字段 → 不适用。
  - 0 测试类引用 `UserInfoResponse`/`UserBasicInfo`（`grep backend/src/test` 实证）。
  - WIP 盘点：4 个后端文件 `git status` 干净（直接 add）；`api/auth.ts` 含 Owner WIP 8 行（`UserInfo` +`storeId`/`storeName`/`departmentId`/`department`，P0-SEC-006 `/me` 授权链配套）→ index 手术。
- 改动（`803e5ae`，5 文件 +57/−7）：
  1. `UserInfoResponse`：+`locationId` 字段/getter/setter/equals/hashCode/toString；`storeId`/`storeName` 加 `@Deprecated` +「观察期兼容，取值经 `location_id_map` 反查；新消费点应读 `locationId`」注释；`@Schema` 描述同步标「已废弃」。
  2. `UserBasicInfo`：+`locationId` 字段/全参构造器加参/getter/setter/builder 全链；同款 `@Deprecated` + 注释。
  3. `AuthServiceImpl.getUserInfo`：+`response.setLocationId(user.getLocationId())`（直取，不经桥；紧邻保留的 `storeIdOf` 反查行，注释标明加新留旧）。
  4. `UserDataServiceImpl.convertToBasicInfo`：+`.locationId(user.getLocationId())`（同注）。
  5. `api/auth.ts`：`UserInfo` +`locationId?: number`（HEAD 基线 +2 行；C9-only blob `472f0c2`，blob diff = 仅 2 行插入）。
- 交验（命令 + 结果，不猜）：
  - `mvn -f backend/pom.xml test-compile` → **BUILD SUCCESS**（17.9s，`c9-compile.log`）——主 + 测试全编译，构造器签名变更零外部调用点实证。
  - `vue-tsc --noEmit`（frontend）→ **144 个既有错误全在其它 WIP 文件（recruitment/converters/stockin/plan 等），C9 文件（`api/auth.ts`、`stores/permission.ts`）0 错误**（`c9-vue-tsc.log`）。
  - 暂存面自洽：`git diff --cached --stat` = 5 文件 +57/−7；auth.ts 暂存 blob diff = HEAD 基线仅 +2 行（Owner WIP 0 行混入）。
- 纪律事件（如实登记）：
  - **Owner WIP 保护**：`api/auth.ts` 工作区 WIP（8 行）经 index 手术（`git show HEAD:` 基线 + 应用 C9 2 行 → `git hash-object -w` `472f0c2` + `git update-index --cacheinfo`）隔离，只暂存 C9 2 行；提交后复核工作区 diff = 8 行 WIP 原样在位（`M frontend/src/api/auth.ts`）。
  - **延后登记（一行跟进，非本卡欠账）**：`stores/permission.ts` `/me→info` 映射透传 `locationId`（`info` 加 `locationId: me.locationId ?? undefined`）——该映射整段属 P0-SEC-006 Owner WIP（HEAD 版本基于 `jwtPayload` 解码，`locationId` 不在 JWT 载荷；WIP 版本改读 `me` 但未透传）→ 与 WIP 强耦合，WIP 提交后一行即可落地，已登记于任务板 C9 行。
  - **live 验证（待 Owner，ENV-11）**：`GET /v1/auth/me` 与 `GET /v1/users/basic-info/{userId}` 返回体应含 `locationId`（= `users.location_id`），`storeId`/`storeName` 值与 C9 前完全一致（观察期兼容；消费点 0 切换，零行为变化）。
- **回退**：`git revert 803e5ae` 独立（纯加字段 + 标注，`storeId`/`storeName` 原值原语义不动，回退不影响任何已提交切片）。
- 结论：C9 收口；**C1–C10 全部收口，本卡实施阶段 3 完成**（live 验证项挂 Owner ENV-11）。

### §7.8 全量自然复跑（最终门，2026-10-12，C9 提交后）

- 命令：`mvn -f backend\pom.xml test`（JDK21 Temurin 21.0.9 + Maven 3.9.11；`c9-full-mvn.log`）。
- 结果：**Tests run: 381, Failures: 18, Errors: 44, Skipped: 1**（62 失败；BUILD FAILURE = 既有环境漂移，非本卡）。
- 与 C5 基线全量（`c5-mvn-full.log`，380 tests / 74 失败 / 18 失败类）逐类对比：**新增失败类 = 0**；2 类转绿（`MaterialDeductionAudit*`，环境漂移向好）；+1 测试 = C7 `InventoryTransientFieldMeasureTest`（@Disabled 档案，= Skipped:1）——非 C9 引入。
- 本卡全部测试类绿：`LocationIdBridgeTest` 6 + `EmployeeAssignLocationEndpointTest` 4 + `EmployeeServiceImplAssignLocationTest` 10 + `EmployeeServiceImplLocationResolveTest` 21 + `InventoryServiceImplLegacyBridgeTest` 7 + `InventoryServiceImplLocationStockTest` 9 + `SalesOrderServiceImplTest` 7 + `PurchaseArrivalReceiverKeyNormalizationTest` 5 + `FoodLegacySyncTest` 5 + `PosOrderCreateServiceFoodIdMapTest` 5 + `PosOrderEnsureLegacyRowTest` 3 + `PurchaseOrderSupplierBindingTest` 2 + `PurchaseReturnServiceImplTest` 5 + `PurchaseStockinServiceImplTest` 8 + `InventoryMovementServiceImplTest` 9 + `InventoryTransferServiceImplTest` 4 + `OrderNewServiceImplDeductTest` 28 + `OrderNewServiceImplOrderNumberA1Test` 3。
- 62 个失败全部落在 C5 基线既有漂移类（finance 系 / `SysSetting*` / `SysDict*` / `Position*Generator` / `OrderManagement`+`SystemSettings*` 集成 context 加载失败等），与本卡代码路径无交集（C9 触碰的 `AuthServiceImpl`/`UserDataServiceImpl`/2 个 DTO 0 测试引用，§7.7 实证）。

### §7.9 三点全量对比 + SEC-006 补提交 + N15/N16/N17 判档（2026-10-12 Owner 澄清指令）

**背景（Owner 三件澄清）**：① 中间四片提交时是否各跑了全量？没跑则补跑 C6 提交点全量对比 C5；② 74→62 差 12 逐类核对；③ permission.ts 一行（P0-SEC-006 WIP）要么现在合要么排期。

**披露 A——中间片提交时交验 = 定向 only（未跑全量）**：
- C8：`c8-mvn-targeted.log` 5/5 + `c8patch-mvn-targeted.log` 5/5；
- C10：`c10-baseline-mvn.log` 7/7 + `c10-verify-mvn.log` 7/7；
- C7：`c7-mvn.log` 定向批 50/50；
- C6：纯前端，`vue-tsc` 改动文件 0 错误（无后端改动，无 mvn）。

**披露 B——已提交树不能独立构建（仓库级事实）**：
- `git status --porcelain -uall -- backend/src` = **39 个未跟踪源文件**（从未提交），被已提交代码引用（`MaterialDeductionContext`、`ReceiptSignatureDTO` 等）。
- 干净 worktree `dd6ddcc`（补 `backend/lib` 3 个 system jar 后）→ **36 编译错误**（缺类）→ 已提交树不可编译。
- 当前 39 个未跟踪文件 overlay 到 `dd6ddcc` → 仍 **19 编译错误**（未跟踪文件与当前 tracked 状态耦合，时代错位）。
- → 「提交点全量」唯一可行配置 = **主工作树（当前 WIP/未跟踪环境不动）+ 卡文件回退到目标提交点**（回退集 = `git diff --name-only <point> HEAD -- backend/src`：C5 点 15 文件、C6 点 4 文件；备份 + `hash-object` 校验恢复 15/15 + 4/4 OK；`git checkout <point> --` 会污染 index，须 `git reset --` 复原）。

**披露 C——原 C5 基线是 PG 宕机下跑的**：`c5-mvn-full.log` 含 **35 处 Connection refused**；`c9-full-mvn.log` 0 处。C5 的 74 失败含环境伪影。

**三点全量对比（均 PG 在线、0 refused、同 WIP/未跟踪环境、仅卡代码不同）**：

| 端点 | 卡代码 | run | 失败测试 | 失败类 | skipped | refused |
|---|---|---|---|---|---|---|
| C5 提交点 `563c3e8` | C1–C5 | 380 | 62（18F+44E） | 16 | 0 | 0 |
| C6 提交点 `dd6ddcc` | +C8/C10/C7/C6 | 381 | 62（18F+44E） | 16 | 1 | 0 |
| C9 提交点 `803e5ae` | +C9（=C1–C10） | 381 | 62（18F+44E） | 16 | 1 | 0 |

- 日志：`c5point-full-mvn.log` / `c6point-full-mvn.log` / `c9-full-mvn.log`。
- **16 失败类与逐类 F/E 计数三点一致**：InventoryLogConsumptionStats 2F / InventoryLogFilter 1F / OrderManagement 9E / SystemSettingsDatabase 13E / SystemSettingsIntegration 8E / CostRecord 1F / InvoiceReimbursement$PayTest 1F / Payable 1F / Payment 3E / FoodTracePrompt 4E / SysDict$EdgeCase 1F / SysDict$UpdateDictItem 1E / PositionSorting 5E / SysSettingService 4F / FoodCodeGenerator 3F / PositionCodeGenerator 4F+1E。
- +1 run/+1 skipped = C7 `InventoryTransientFieldMeasureTest`（@Disabled，§7.8 已登记）。
- **结论：C8/C10/C7/C6/C9 引入 0 新增失败类、0 新增失败测试**（三点链覆盖中间片，取代此前仅两端点证据）。

**74→62 差 12 逐类核对（全为环境伪影）**：
- 「2 类转绿」= `MaterialDeductionAuditIntegrationTest` 4E→绿（−4）+ `MaterialDeductionAuditSelfFailureIntegrationTest` 1E→绿（−1）：C5 中为 `Failed to load ApplicationContext`（PG 宕机），PG 在线即绿。
- `InventoryLogConsumptionStatsIntegrationTest` 6E→2F（−4）：C5 context 加载失败 → PG 在线后 context 通过、剩 2 断言失败（数据漂移：expected -30 actual -40.4000 等）。
- `InventoryLogFilterIntegrationTest` 4E→1F（−3）：C5 context 加载失败 → 剩 1 断言失败（expected 2 actual 4）。
- 其余 14 类两端点计数完全一致。5+4+3 = 12 全部归因。
- 注：InventoryLog 两类残留 2F/1F 为**数据漂移**（fixture 期望 vs 活库数据），属 §24.3e/§24.3f 卡范围，与本卡无关。

**SEC-006 补提交（Owner 指令「要么现在合」→ 合）**：
- git 事实：`git log --all -- frontend/src/stores/permission.ts` = 仅 `dd33ee3`（init，2026-08-02）→ 任务板 L1981「已提交」声称与 git 不符；修复一直活在未提交 WIP（mtime 2026-09-15）。QA 复审（2026-08-10）当时基于工作树代码评审。
- 补提交 **`64e5d95`**（2 文件 +100/−67）：`permission.ts`（/me 唯一授权源 + /me 失败登出 fail-closed + mapRoles fail-closed 不降级 EMPLOYEE + 移除 atob 本地解码）+ `api/auth.ts`（Owner WIP `storeId/storeName/departmentId/department` 4 字段 + **保留 C9 `locationId` 防回退**——WIP 版本早于 C9、缺该字段，提交前合并字段集）。
- 范围纪律：仅 permission.ts + api/auth.ts（与 board 声称范围一致）；`request.ts` token 静默续期 WIP = P1-SEC-004 关联特性，**未纳入**，另行排期（KL-013 = **已关闭 2026-08-11**，版本差注意项见 §7.9b 重审；终验 = Owner Live）。
- 依赖自洽：permission.ts WIP 全部 import 目标（`types/permission.ts`/`utils/storage.ts`/`api/system/permission-template.ts`/`api/auth.ts`）干净或随提交包含；导出面与 HEAD 一致（枚举/store/return 块相同）；`getCurrentUser()` 在 HEAD auth.ts 已存在。
- 交验：`vue-tsc --noEmit` 本卡面 0 错误（144 处预存 WIP 错误，计数与基线一致）。
- **C9 跟进落地 `c588476`**（+3 行）：`permission.ts` UserInfo 接口 +`locationId?: number` + `/me→info` 映射 +`locationId: me.locationId` → §7.7 延后登记销账，D7 前端消费链（/me → authApi → permissionStore.userInfo.locationId）全链可达。vue-tsc 144 = 基线，卡面 0 错误。

**N15/N16/N17 判档（Owner 指令；只判档，不实施）**：
- **N15 = P1（功能阻断，fail-closed 方向，无越权）**：活库实测 2026-10-12 `permissions` 表 `permission_code LIKE 'order%'` = **0 行** → `order:manage`/`order:view` 不存在、无角色可持有 → SalesOrderController 端点仅 admin `*` 通配可达，**非 admin 用户全部 403**。与 U3（`system:user:assign-store` 未落 seed）同根因家族。修复路径（另卡）：seed 迁移补权限码 + 角色绑定（C3 同款），或端点换用现有码。
- **N16 = P3（dead code 清理，无运行时影响）**：`LocationGuard` git grep 实测 0 外部调用点（仅类声明/logger/构造器自引用）。修复路径（另卡）：删类，或按设计「收敛到守卫」落地（设计债）。
- **N17 = P1（运行时必炸链 + 每小时静默失败）**：活库实测 2026-10-12 `inventory_warning%` 表 = `inventory_warning` / `inventory_warning_rule` / `inventory_warning_record` / `inventory_warning_rules`，**无复数 `inventory_warning_records`**、无 Flyway 创建迁移；`InventoryWarningScheduler` `@Scheduled(cron 每小时)`（:47）→ `createWarningRecord` insert（:205）必中 `relation does not exist` → 被外层 `catch`（:79-81）吞 → **预警记录永不落库，且首个失败后该周期剩余规则被跳过**。修复路径（另卡）：Flyway 迁移建 `inventory_warning_records`（单数表列集不同，不可改读单数表）。

**N15/N16/N17 判档复议 + 开卡（2026-10-12，Owner 指令「并行开卡：N15 + N17，后者优先」）**：
- **N15 = P1 ✅（Owner 确认）** → **已开卡 board §24.3s P1-ORDER-PERMISSION-SEED-001**（PENDING 未启动）。影响面复核大于初判：~10 控制器 ~50 端点引用该两码（SalesOrder 8 + OrderNew 16 + SalesAnalysis 9 + DiningTable 8 + DecisionBoard 5 + LiveMonitor 4 + CallNumber 4 + OrderTrends 2 + Pos 2）；代码侧已有现成幂等入口 `POST /v1/permissions/init`（`PermissionController:331`，admin，`initPermissions()` 含 order:manage/order:view，:139/:143）**从未被调用**。
- **N16 = P3→P2（Owner：「设计没落地」，设计债，非普通死代码）** → 本轮指令仅 N15+N17，**不开卡**，待 Owner 指令。
- **N17 = P1→P0（生产事故级）（Owner：「每小时必炸 + 静默功能失效，如果生产在跑，这是生产事故级」；如 Owner 确认非生产运行降紧急 P1）** → **已开卡 board §24.3r P0-INVENTORY-WARNING-TABLE-001（优先）**（PENDING 未启动；实施要点 = 1 个 Flyway 建表迁移，列集 = 实体 12 字段，调度器代码零改动）。

**7.9b P0-SEC-006 治理重审（Owner 指令：「列 P0-SEC-006 真实提交点」「基于真实提交点，重审之前所有"依赖 P0-SEC-006 已收口"的结论（KL-013 首当其冲）」）**：

- **真实提交点（Owner 指定命令原始输出，2026-10-12 执行）**：
  - `git log --all -- frontend/src/stores/permission.ts` = 恰 3 条：`dd33ee3`（init，2026-08-02，atob 版）→ **`64e5d95`**（SEC-006 补提交，2026-10-12 15:48）→ `c588476`（C9 跟进，2026-10-12 15:49）。
  - `git log --all --oneline | grep -i "SEC-006"` = 单条命中：`64e5d95 fix(P0-SEC-006): 权限源统一 /me + fail-closed 补提交 …`。
  - **结论：P0-SEC-006 真实提交点 = `64e5d95`（2026-10-12）——全史唯一实质提交；此前 `permission.ts` 仅有 init `dd33ee3`**。2026-08-10（QA 复审）至 2026-10-12 之间一切「已收口」声称均基于工作树 WIP（mtime 2026-09-15），提交锚此前不存在。
- **时间线对账**：2026-08-10 QA 复审 PASS_WITH_LIMITATION（工作树代码评审；同次实测「0 parseJWTPayload 残留」代码级 → /me 版当时已存在于工作树）→ 2026-08-10 KL-007 关闭（P1-SEC-009 后端渗透 14 用例）→ 2026-08-11 KL-013 关闭（浏览器冒烟 5/5）→ 2026-09-15 文件最后写入（WIP mtime；无版本控制，delta 内容不可证明）→ 2026-10-12 提交 `64e5d95`。
- **依赖结论逐项重审**：

| 结论 | 证据 | 重审判定 |
|---|---|---|
| KL-007（L-2 渗透）已关闭 2026-08-10 | P1-SEC-009 活体渗透 14 用例（后端 HTTP 级）+「0 parseJWTPayload 残留」代码级（与提交内容一致） | **维持**——证据为后端侧 + 代码级，不依赖前端提交状态 |
| KL-013（L-5 登录联调）已关闭 2026-08-11 | `browser-smoke-sprint3.md` §一.1 5 项 PASS（登录→/me→路由→按钮渲染→401 登出） | **维持（附条件）**——证据链完整，但关闭证据（08-11）**早于文件最后写入（09-15）**，最终提交内容（`64e5d95`）从未 Live 验证 → **终验 = Owner Live（登录→/me→路由跳转→按钮渲染全链路）；如发现差异则重开 KL-013** |
| REG-SEC-004 / final gate「0 parseJWTPayload 残留」（2026-08-10） | 代码级 grep（与提交内容一致） | **维持** |
| release-readiness-report L22「KL-013 4/4 闭环」（2026-08-11） | 同 KL-013 | **维持（附条件）**——同版本差注意项 |
| remediation-roadmap L113「P0-SEC-006 → L-2/L-5」（2026-08-10 历史快照） | KL 状态见上行 | **维持**（KL 现行状态以上行为准） |
| Board L1981「L-5 登录联调未执行 → KL-013」（2026-08-10 快照行） | KL-013 已于 2026-08-11 关闭 | **更正**——L1981 已改注（L-2/L-5 均已关闭 + 版本差注意项） |
| 本记录/board §24.3q 初稿「KL-013 仍开放」 | §24.3q 初稿（2026-10-12 当日上午） | **更正**——Agent 误记：仅看 L1981 快照行、未查 KL 登记最新状态；board ③ 行、L698 问题卡更正戳均已改 |
- **治理教训（登记）**：「已收口」声称须同时锚定 (a) 证据面（QA/冒烟/渗透工件）与 (b) 提交面（git log）。2026-08-10~10-12 期间只有 (a) 无 (b)，故「P0-SEC-006 已收口」对 git 面不成立；后续收口声称重审一律先跑 `git log --all -- <file>` + `git log --all --oneline | grep -i <卡号>`。

> **2026-10-12 扩展（Owner 三件追问）**：P0-SEC 全系列提交面核查证实「已提交」声称不符范围远大于 SEC-006 单点——第一阶段 7 卡（SEC-003/004/005、P1-SEC-001/002/003/004）对应文件（guards.ts/router/index.ts/permissions.ts/AdminPermissions/UserDetailsServiceImpl/request.ts）全史仅 init + 工作树 ` M`（backend 209 + frontend 215 = 424 文件 WIP）→ **GOV-INCIDENT-003**（`production-governance-incidents.md`）+ 治理规则 **PG-001 收口双向锚定**（证据面 + 提交面，仅工作树评审 ≠ 提交完成；board 第一阶段表 L1980-1987 已逐行更正戳 + 表下证据注）。

### §7.10 Live 验证标准（Owner 执行，2026-10-12 指令「Live 验证标准，你写完我跑」）

> **通用纪律（全批次适用）**：
> - 前置：PG 已起（`localhost:5432`，db=`food_traceability`）；后端运行中（Owner 常用 profile）；前端运行中（vite dev/preview）；
> - 任一项 **FAIL** → **原始输出原样贴出**（HTTP 状态码 + 响应体 / psql 输出 / 浏览器 Network + console）→ 按文末「失败处理映射」重开对应卡/KL；
> - 每项 **PASS** → 记录实测值（时间戳 + 关键字段值）回写本节对应行。
> - 禁止为通过验证直接改库（PG-002 纪律）；数据一律走 API。

#### 批次 A —— P1-USER-LOCATION-002（C3/C5/C9，提交面已干净：`ddb7d08`/`e97db65`/`563c3e8`/`803e5ae`/`c588476`）

**V-A1（C3 A4，`PUT /v1/employees/{id}/assign-location`）**

| # | 操作 | PASS 标准 |
|---|------|-----------|
| 1 | 无 `hr:employee:assign-location` 权限用户（非 HR 总监/非 admin）调用，body `{"locationId": <有效位置>}` | **403**，响应含「权限不足: hr:employee:assign-location」（PermissionAspect 链） |
| 2 | `ROLE_HR_DIRECTOR`（或 admin）同调用 | **200**，`Result<Employee>` 返回详情（部门/岗位名称填充） |
| 3 | `SELECT module, desc, create_time FROM operation_log WHERE module='EMPLOYEE_LOCATION_ASSIGN' ORDER BY create_time DESC LIMIT 3;` | 有新行，`desc` 含**旧值 → 新值**（如「locationId 2 → 500」）+ users 侧同步片段（C4，如「未找到对应用户」/「未重复写」/新值） |
| 4 | 边界：`{"locationId": <TRANSIT 位置>}` / `{"locationId": 999999}` | 均 **400**（TRANSIT 禁作归属 U9 / 位置不存在） |
| 5 | 员工不存在 id | **404** |

**V-A2（C5 UI，HREmployee 归属表单）**

| # | 操作 | PASS 标准 |
|---|------|-----------|
| 1 | HR 员工列表 → 新建/编辑员工 → 「归属位置」下拉 | 单一下拉，选项 = `locationName（门店/中央仓/仓储仓）` 类型标签（数据源 `GET /v1/locations`） |
| 2 | 下拉内容 | **无 TRANSIT** 选项 |
| 3 | 选门店提交 → `SELECT location_id, work_location_type FROM employees WHERE id=<该员工>;` | `location_id` 非 NULL，`work_location_type='STORE'`（C2b 推导） |
| 4 | 不选位置提交 | `work_location_type='HEADQUARTERS'`（U10） |
| 5 | 列表「工作归属」列 | 显示位置名称（locations 晚到时响应式刷新，不停在回退值） |

**V-A3（C9，/me + basic-info 出参契约）**

| # | 操作 | PASS 标准 |
|---|------|-----------|
| 1 | `GET /v1/auth/me` | 200，含 `locationId`（number，= `users.location_id`）；`storeId`/`storeName` **仍在**（观察期兼容） |
| 2 | `GET /v1/users/basic-info/{userId}` | 同上两点 |
| 3 | 对比 | `storeId`/`storeName` 值与 C9 前完全一致（**零行为变化**，消费点 0 切换） |
| 4 | 前端已登录态（C9 跟进 `c588476` 消费链） | `permissionStore.userInfo.locationId` 可取（console 或 DevTools） |

> **V-A3 判据细化（2026-10-12 追加，代码级锚定；原表行 1-3 表述保留作历史，执行以本块为准）**
>
> 代码级事实（HEAD 证据）：
> - `/me` → `AuthServiceImpl:322-326`：`locationId` = `users.location_id` **直取**；`storeId` = `LocationIdBridge.storeIdOf(locationId)`（`location_id_map` 反查，`src_table='stores_new'`，无映射/归属 NULL → null）；`storeName` = `User.storeName`——该字段 `@TableField(exist = false)`（`User.java:108`），默认 `selectById` 不填充 → **`/me` 的 `storeName` 恒为 null（代码级常量，非故障）**。
> - `basic-info/{userId}` → `UserDataServiceImpl:123-156`：`locationId`/`storeId` 同上；`storeName` = 反查 storeId 能解析时取 **`stores_new.store_name`**（经 `storeDataService.getStoreBasicInfo`），否则 null——**两端点 `storeName` 语义不同（/me 恒 null；basic-info 真实派生）**。
> - Jackson：`WebConfig:84` `Include.ALWAYS` → null 字段在 JSON 中显式出现（如 `"locationId": null`），**不因 null 省略**。
> - 活库锚点（food_traceability）：locations `1=测试门店A(STORE)` / `2=测试门店B(STORE)` / `3=DeepStore(STORE)` / `4=仓库B(DEPOT)` / `5=仓库A(CENTRAL)`；`location_id_map`：`stores_new` 1→loc1、2→loc2、3→loc3，`warehouses` 1→loc5、2→loc4；`users` 现 `admin`/`test-admin` 的 `location_id` 均为 NULL。
>
> 细化 PASS 判据（U = 被测登录用户）：
>
> | # | 操作 | PASS 标准 |
> |---|------|-----------|
> | 1 | `GET /v1/auth/me` | 200；`locationId` == `SELECT location_id FROM users WHERE username=U`（NULL → `"locationId": null`）；`storeId` == `SELECT src_id FROM location_id_map WHERE src_table='stores_new' AND location_id=(SELECT location_id FROM users WHERE username=U)`（无行 → null）；`storeName` = null（代码级常量） |
> | 2 | `GET /v1/users/basic-info/{userId}` | `locationId`/`storeId` 同上；`storeName` == `SELECT store_name FROM stores_new WHERE store_id=(反查 src_id)`（门店不存在 → null） |
> | 3 | 路径验证（先设归属再验）：`PUT /v1/users/{U}/assign-store` body `{"locationId": 1}`（需 `system:user:assign-store`；C3 已 seed 该码但**无角色绑定**——U7 裁定，当前仅 **admin** 经 `*` 通配可执行，建议 U = admin 自身，用自己的 token 调）→ 复验 1/2 | 设后：`locationId=1`、`storeId=1`；`/me` `storeName=null`；basic-info `storeName=<stores_new store_id=1 的 store_name>`；再改 `{"locationId": 4}`（仓库，无 stores_new 映射）→ `locationId=4`、`storeId=null`（显式拒绝语义，无数值兜底）；验证完可 `DELETE /v1/users/{U}/assign-store` 还原（同权限门） |
> | 4 | 前端已登录态（C9 跟进 `c588476` 消费链） | `permissionStore.userInfo.locationId` 可取（console 或 DevTools） |
>
> 原表行 3「与 C9 前完全一致」**废弃**（无 C9 前基线快照，不可执行）；零行为变化主张改由本块代码级锚定承载（契约以 HEAD 代码为准，观测值应与代码一致，偏离即 FAIL）。

#### 批次 B —— P0-SEC-006 L-5 全链路（KL-013 终验；提交点 `64e5d95` + `c588476`）

**V-B1（登录 → /me → 路由跳转 → 按钮渲染，全链路）**

| # | 操作 | PASS 标准 |
|---|------|-----------|
| 1 | 干净会话（清 cookie 或隐身）→ 登录 admin/Admin@123 | 成功，**跳转 /home**（路由守卫放行） |
| 2 | Network 面板看 `GET /v1/auth/me` | 200，含 `locationId`（权限源 = /me，非本地 JWT 解析） |
| 3 | 进入带权限按钮的业务页（如 /finance/ledger 或任一有权限码按钮的页面） | 页面渲染正常，权限按钮按码渲染（有码可见/无码隐藏，与 08-11 冒烟基线一致） |
| 4 | 已登录会话内篡改 token → 刷新/触发请求 | 401 弹框（标题「登录已过期」，无「留在本页」）→ 确认 → 登出跳 /login（**fail-closed**） |

- **全 PASS** → KL-013 附条件终验完成，条件条款关闭（回写 KL 登记 + release-readiness L22）。
- **任一项 FAIL** → **重开 KL-013**，原始输出原样贴出 + 核对运行面 `permission.ts` 实际内容 vs `64e5d95` 提交内容（`git show 64e5d95 -- frontend/src/stores/permission.ts`）。

#### 批次 C —— N17 失败证据（Owner 跑 SQL；卡 §24.3r PENDING 待放行）

- 事实基线：`InventoryWarningScheduler` + `InventoryWarningRecord` 实体均**自 init `dd33ee3`（2026-08-02）存在** → **失败窗口 ≈ 2026-08-02 后第一次应用启动起**（复数表从未建，每小时 insert 必中 `relation does not exist`，被 :79-81 catch 吞）。
- 参考 SQL（Owner 执行，只读）：

```sql
-- 1) 单数/复数表存在性（期望：单数有、复数 NULL）
SELECT to_regclass('inventory_warning_record')  AS singular,
       to_regclass('inventory_warning_records') AS plural_expected_null;
-- 2) 迁移时间线（V20260629_021 安装时间；§24.3r 建表迁移 20261012.001 当前应不存在）
SELECT version, description, installed_on FROM flyway_schema_history
WHERE version IN ('20260629.021','20261012.001') ORDER BY installed_rank;
-- 3) 失败起始时间锚点 = 2026-08-02 后第一次应用启动（应用日志 grep：
--    "inventory_warning_records" / "relation \"inventory_warning_records\" does not exist"，
--    频率应 ≈ 每小时 1 次；最早一条日志时间 = 失败起始实证）
```

- **证据齐备标准**：复数表 `to_regclass` = NULL + 应用日志存在 `relation "inventory_warning_records" does not exist`（频率 ≈ 每小时）+ 日志最早时间 = 失败起始时间实证。

> **批次 C 状态更新（2026-10-12 追加，N17 已收口 `9aaa1f0`）**：本批 SQL 的期望值**已变化**——
> - SQL#1：`to_regclass('inventory_warning_records')` 现返回**非 NULL**（表已建，Flyway rank 214 `20261012.001`，2026-10-12 20:47:02.923 success=t）；
> - SQL#2：`20261012.001` **已存在**于 `flyway_schema_history`（rank 214）；
> - 失败窗口**已闭合**：下一小时整点调度周期起 insert 成功落库（可 `SELECT count(*) FROM inventory_warning_records;` 验证 ≥0 且随规则命中增长）；
> - **历史失败窗口证据（仍有效）**：仅 SQL#3 日志 grep 部分——`relation "inventory_warning_records" does not exist` 的最早一条时间 = 失败起始实证（2026-08-02 后首次应用启动起，至 2026-10-12 20:47 修复）。若应用日志已被滚动清理，则历史起点不可实证，按「失败窗口 ≈ init `dd33ee3` 后」记档即可。

#### 失败处理映射

| 失败项 | 动作 |
|---|---|
| V-A1/V-A2 | 重开 C3/C5 对应卡面复核（提交内容 vs 工作树 hunk 级核对，PG-001 协议） |
| V-A3 | 重开 C9（出参契约） |
| V-B1 | **重开 KL-013** + 按 §7.9b 重审协议处理依赖结论 |
| V-C | §24.3r（N17，P0）放行实施；失败起始时间回写 §24.3r 卡 |

### §7.11 C3 迁移 P0 缺陷发现与修复（2026-10-12，N17 实施过程中发现，**收口后追加记载**）

**发现（P0，治理级）**：C3 迁移 `V20261009_001__seed_hr_employee_assign_location.sql` 自提交起即含 **PG 42601 语法错误（原 :50）**——`INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)` 的 `SELECT` 提供 **2 个值**对应 **4 列**（缺 `deleted`/`create_time` 值）→ **该迁移从未在任何库应用成功**（发现时活库 `flyway_schema_history` 无 `20261009.001` 行）。

**发现路径（证据链）**：
1. N17 独立 Flyway runner 应用 `20261012.001` 前，Flyway 先报 `20261009.001` 失败（42601）→ 定位 :50 列值不匹配；
2. `mvn test` run#0（C3 修复前）`FoodTracePromptServiceTest`（默认 profile → 活库 → Flyway 随上下文加载运行）context 加载失败 4E——**同根因**：Flyway 在 context 加载期撞上坏迁移；
3. 修复后 run#1：该类 context 加载通过、4 测试 PASS → 62→58 的**全部 4 个 delta 即此**（非新增失败，是既有失败转绿；run#1/run#2/run#3 三次 58 逐测试集合相等，`Compare-Object` 空）。

**影响面（如实登记）**：
- C3 的 3 个权限码（`hr:employee:assign-location` / `system:user:assign-store` / `system:user:unassign-store`）+ 1 条绑定（`ROLE_HR_DIRECTOR → hr:employee:assign-location`）**在修复前从未在活库生效** → 本卡 V-A1 行 1/2（403/200 权限门验证）在修复前**不可能按预期行为出现**（非 HR 角色调 assign-location = 码不存在 → PermissionAspect 链行为与「403 权限不足」文案依赖码已 seed 的前提）；
- 不触及 N15/N17（不同迁移、不同表）；不触及任何已提交代码行为（纯 seed 数据面）。

**修复与验证**：
- 提交 **`697773e`**（1 文件，1+/1−，:50 补齐 4 列值 `SELECT 'ROLE_HR_DIRECTOR', 'PERM_HR_EMP_ASSIGN_LOCATION', 0, NOW()`）；
- 应用：Flyway rank **213** `20261009.001` success=t（2026-10-12 20:47:02.923）；
- 活库复核：3 权限行 + 1 绑定行在位（`role_permissions` join `permissions` 查询，`deleted=0`）；`system:user:assign-store`/`system:user:unassign-store` = **seed 在位但无角色绑定**（U7 裁定：仅 admin 经 `*` 使用）；
- 回归：mvn test run#1（修复后首次）58 F+E，其中 `FoodTracePromptServiceTest` 4 PASS（run#0 时 4E）。

**V-A1 前置条件更新**：V-A1 行 1/2 现可有效执行（权限码 + 绑定已在活库，rank 213）；行 3 审计日志验证不受影响。

**治理注**：C3 卡此前已 ACCEPTED（C1–C10 收口，§7.7/§7.8），其「seed 落库」证据面当时锚定于**迁移文件存在 + 编译/定向测试绿**，未含「活库 flyway 应用成功」一项——本次发现即该缺口。**PG-001 双向锚定规则扩展建议**：凡 seed/建表类迁移，收口证据面须含 `flyway_schema_history` 应用成功行（rank + success=t + installed_on），否则「已落库」声称不成立。
