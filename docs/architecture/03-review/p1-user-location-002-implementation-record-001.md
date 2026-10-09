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
| 阶段 3 实施 | **▶ 进行中：C1、C2、C2b 已提交 + Windows 侧交验通过（定向 25/25，全量 = 基线 58 零回归）；C3–C10 未开始** | 本记录 |

**阶段 3 边界（PG-005）**：允许改代码 / 补测试 / 补实施记录；**禁止跨阶段顺手修、禁止扩大范围**。本卡 I-4 已定：**无 schema 变更**，唯一允许的 DB 动作是 C3 的 1 条 seed 迁移（3 个权限码）。

---

## §2 已实施提交（逐提交可独立 revert）

| 提交 | 内容 | 文件面 | 依据 | 验收状态 |
|---|---|---|---|---|
| `aff2568` **C1** | 桥补正向入口：`toLocationId(String srcTable, Long srcId)`；旧 `toLocationId(Long storeId)` 保留签名、委托 `SRC_STORES_NEW`；新增 `static locationIdOfWarehouse` | `common/util/LocationIdBridge.java` + 新测试 `LocationIdBridgeTest`（6 例） | 设计 §2 D1-1、§6 A1 | ⏸ 待 Windows 侧（ENV-11） |
| `ff4cba3` **C2** | 员工 DTO 加 `locationId`（唯一归属入参），`storeId`/`warehouseId`/`workLocationType` 标 `@Deprecated` 保留；`EmployeeServiceImpl.resolveLocationId` 优先级链 + 存在性校验；create/update 两处写入改走它；新增 `LocationMapper` 构造注入 | `dto/EmployeeCreateDTO.java`、`dto/EmployeeUpdateDTO.java`、`service/impl/EmployeeServiceImpl.java` + 新测试 `EmployeeServiceImplLocationResolveTest`（9 例） | 设计 §2 D1-2 / D1-3、§3 改动清单、§6 A2 | ✅ Windows 侧交验通过（2026-10-09） |
| `acb9aa8` **C2b** | D1-4 落点：`workLocationType` 显式推导——新增 `mapLocationTypeToWorkLocationType`（U9 映射）+ `deriveWorkLocationType`；创建路径无 locationId → 显式 `HEADQUARTERS`（U10，不走 DB DEFAULT）；更新路径未表达归属意图 → 不 set（I-3）；`TRANSIT`/词表外 → 400 | `service/impl/EmployeeServiceImpl.java` + `EmployeeServiceImplLocationResolveTest`（9→19 例，+10 C2b 用例） | 设计 §2 D1-4、§7 U9/U10、F-1/F-2/F-3 裁决（§3.4） | ✅ 编译 + 定向测试 25/25 绿（2026-10-09，JDK21 + Maven 3.9.11） |

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

## §4 未开始提交（C2b、C3–C10）

> **推进顺序（Owner 第四批）**：C1+C2 Windows 交验 ✅ → **C2b ✅（`acb9aa8`）→ C3 → C4 → C5…**。

| 提交 | 状态 | 前置 |
|---|---|---|
| **C2b** 承载 D1-4：`resolveLocationId` 内按 U9 映射显式 `set workLocationType`；`TRANSIT`/词表外 → 400；无 `locationId` → 显式 `HEADQUARTERS`（不走 DB DEFAULT）+ 单测 | ✅ 已提交 `acb9aa8` + 交验通过（定向 25/25 绿，2026-10-09） | C1–C2 交验通过 ✅ |
| C3 权限码 5 面 + `PUT /v1/employees/{id}/assign-location` + 审计 + seed 迁移（U3/U7 已裁） | 未开始 | C1–C2 交验通过（或 Owner 两天未验 → 直接开工） |
| C4 反向同步 `syncUserAssignmentFromEmployee`（U1/U2 已裁） | 未开始 | C2、C3 |
| C5 前端员工表单合并位置下拉（D2，N6 消解） | 未开始 | **C2b（硬前置）**、C3 |
| C6 用户创建表单加归属（D6） | 未开始 | 无 |
| C7 N3 三处（D4） | 未开始 | 无 |
| C8 N4 一处（D5，仅 `PurchaseArrivalServiceImpl:656`） | 未开始 | 无（设备域 KL-086 冻结不动） |
| C9 N8 出参加 `locationId` + 前端消费切换（D7） | 未开始 | 无 |
| C10 N12 冗余收敛 2 处 + `@Deprecated`（D8-2/3，U6 已裁纳入） | 未开始 | 无 |

**§24.3p（POS 未分配位置引导）不在本卡内**，且其前置交集 SQL 未跑 → 未实施（Owner 已裁：等 SQL 结果再实施）。

---

## §5 验收状态（Windows 侧交验，2026-10-09 完成；ENV-11 已解除：本会话在 Windows 侧，JDK21 + Maven 3.9.11 可用）

| 验收项 | 设计出处 | 状态 | 执行方式（实际） |
|---|---|---|---|
| 编译通过 | §6 | ✅ 通过（`mvn test` 全构建 compile + testCompile 成功） | `mvn -f backend/pom.xml test …`（JDK21 Temurin 21.0.9，Maven 3.9.11） |
| `LocationIdBridgeTest` 6 例通过 | §6 A1 | ✅ 6/6 | `mvn test -Dtest=LocationIdBridgeTest,…` |
| `EmployeeServiceImplLocationResolveTest` 通过（C2b 后 19 例） | §6 A2 | ✅ 19/19（C2 9 例 + C2b 10 例） | 同上（定向批次 **Tests run: 25, Failures: 0, Errors: 0**） |
| 仓库员工归属不再恒 NULL（端到端） | §6 A2 / 诊断缺口① | ⏸ 待 Owner 手工 | 员工表单选仓库 → 查 `employees.location_id` 非 NULL（emp-l/emp-n 待 UI 分配） |
| 既有测试无回归 | §6 | ✅ 354 tests / 58 失败 = 基线 58（27F+31E，全为 ENV-9 既有 WIP，无一涉及本卡改动面） | `mvn test` 全量（C1+C2 批次）；C2b 批次以定向 25/25 复核，全量将于 C5 后复跑 |

**原登记的本侧未能自验风险点 → 已实跑解除**：
1. `EmployeeCodeGenerator` Mockito mock —— ✅ 实跑通过；
2. `ServiceImpl.baseMapper` 反射注入 —— ✅ 实跑通过；
3. `createEmployee` `this.save` 走 mock `EmployeeMapper.insert` —— ✅ 断言全部成立。

**回退预案（若后续发现问题）**：按提交粒度 revert `acb9aa8`（C2b）→ `ff4cba3`（C2）→ `aff2568`（C1），三者独立。

---

## §6 边界自检

- 本阶段改动面（C1–C2b 合计）：**3 个主文件 + 2 个测试文件**（C2b 复用 C2 的 2 个文件，未新增文件），未触碰其他 WIP 基线文件。
- **无 schema 变更**、无迁移文件新增（C3 才引入 seed 迁移）。
- **未解冻** KL-085（`KdsPrincipal`）、KL-086（设备域 `DEFAULT_STORE_ID`）。
- **未实施 §24.3p 任何内容**（含 B1 语义修正）——该卡独立，且前置 SQL 未跑。
- D1-4 已完成（C2b `acb9aa8`）：F-1/F-2/F-3 登记 → Owner 第四批裁决（U9/U10）→ C1+C2 交验通过 → **裁决后实施，无擅自补做**；§3.5 数据回填仍未实施（独立决策）。
- 提交方式：PG-001 精确 `git add` 逐文件 + `git diff --cached --stat` 核对（C2b：2 files / +218 −5）。
