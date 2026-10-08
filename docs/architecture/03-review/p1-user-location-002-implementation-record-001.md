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
| 阶段 3 实施 | **▶ 进行中：C1、C2 已提交；C3–C10 未开始** | 本记录 |

**阶段 3 边界（PG-005）**：允许改代码 / 补测试 / 补实施记录；**禁止跨阶段顺手修、禁止扩大范围**。本卡 I-4 已定：**无 schema 变更**，唯一允许的 DB 动作是 C3 的 1 条 seed 迁移（3 个权限码）。

---

## §2 已实施提交（逐提交可独立 revert）

| 提交 | 内容 | 文件面 | 依据 | 验收状态 |
|---|---|---|---|---|
| `aff2568` **C1** | 桥补正向入口：`toLocationId(String srcTable, Long srcId)`；旧 `toLocationId(Long storeId)` 保留签名、委托 `SRC_STORES_NEW`；新增 `static locationIdOfWarehouse` | `common/util/LocationIdBridge.java` + 新测试 `LocationIdBridgeTest`（6 例） | 设计 §2 D1-1、§6 A1 | ⏸ 待 Windows 侧（ENV-11） |
| `ff4cba3` **C2** | 员工 DTO 加 `locationId`（唯一归属入参），`storeId`/`warehouseId`/`workLocationType` 标 `@Deprecated` 保留；`EmployeeServiceImpl.resolveLocationId` 优先级链 + 存在性校验；create/update 两处写入改走它；新增 `LocationMapper` 构造注入 | `dto/EmployeeCreateDTO.java`、`dto/EmployeeUpdateDTO.java`、`service/impl/EmployeeServiceImpl.java` + 新测试 `EmployeeServiceImplLocationResolveTest`（9 例） | 设计 §2 D1-2 / D1-3、§3 改动清单、§6 A2 | ⏸ 待 Windows 侧（ENV-11） |

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

---

## §3 实施期新发现（需 Owner 裁决，**阻塞 C5**）

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

**建议处置方向**（供 Owner 拍板，本卡未实施）：
1. 定 F-2 映射表，并在 `resolveLocationId` 同批推导写入 `work_location_type`；或
2. 把 `WorkLocationFilterHelper` 的判据从 `employees.work_location_type` 换成 `locations.location_type`（更彻底，但触及数据范围语义，属独立卡）；或
3. 明确 `work_location_type` 进入退役路径（先停止消费，再 DROP）。

**C5 放行前需先裁 F-2 + F-3。**

---

## §4 未开始提交（C3–C10）

| 提交 | 状态 | 前置 |
|---|---|---|
| C3 权限码 5 面 + `PUT /v1/employees/{id}/assign-location` + 审计 + seed 迁移（U3/U7 已裁） | 未开始 | C1–C2 交验通过 |
| C4 反向同步 `syncUserAssignmentFromEmployee`（U1/U2 已裁） | 未开始 | C2、C3 |
| C5 前端员工表单合并位置下拉（D2，N6 消解） | 未开始 | **F-2 + F-3 裁决** |
| C6 用户创建表单加归属（D6） | 未开始 | 无 |
| C7 N3 三处（D4） | 未开始 | 无 |
| C8 N4 一处（D5，仅 `PurchaseArrivalServiceImpl:656`） | 未开始 | 无（设备域 KL-086 冻结不动） |
| C9 N8 出参加 `locationId` + 前端消费切换（D7） | 未开始 | 无 |
| C10 N12 冗余收敛 2 处 + `@Deprecated`（D8-2/3，U6 已裁纳入） | 未开始 | 无 |

**§24.3p（POS 未分配位置引导）不在本卡内**，且其前置交集 SQL 未跑 → 未实施（Owner 已裁：等 SQL 结果再实施）。

---

## §5 验收状态（ENV-11：本 Linux 会话无 `java`/`mvn`/`psql`/`docker`）

| 验收项 | 设计出处 | 状态 | Owner 侧执行方式 |
|---|---|---|---|
| 编译通过 | §6 | ⏸ 待执行 | `cd backend && mvnd -q compile` |
| `LocationIdBridgeTest` 6 例通过 | §6 A1 | ⏸ 待执行 | `mvnd test -Dtest=LocationIdBridgeTest` |
| `EmployeeServiceImplLocationResolveTest` 9 例通过 | §6 A2 | ⏸ 待执行 | `mvnd test -Dtest=EmployeeServiceImplLocationResolveTest` |
| 仓库员工归属不再恒 NULL（端到端） | §6 A2 / 诊断缺口① | ⏸ 待执行 | 手工：员工表单选仓库 → 查 `employees.location_id` 非 NULL |
| 既有测试无回归 | §6 | ⏸ 待执行 | `mvnd test`（关注 `EmployeeServiceImpl` 构造签名变更影响面） |

**本侧未能自验的编译期风险点（如实登记，不掩饰）**：
1. `EmployeeCodeGenerator` 为具体类（非 final，:17）→ Mockito 可 mock，但未实跑确认；
2. `ServiceImpl.baseMapper` 反射注入沿用仓内既有做法（`AccountingSubjectServiceTest` :42-47 同法），未实跑确认；
3. `createEmployee` 内 `this.save(employee)` 走 mock 的 `EmployeeMapper.insert`（返回 0）→ 断言只针对返回对象上的 `locationId`，不依赖 DB 写入结果。

若上述任一项在 Windows 侧失败，**按提交粒度 revert `ff4cba3`（C2）即可**，C1 独立不受影响。

---

## §6 边界自检

- 本阶段仅改动 **3 个主文件 + 2 个新测试文件**（见 §2），未触碰其他 679 条 WIP 基线文件；每次提交后 `git status --short | wc -l` 回到 **679**。
- **无 schema 变更**、无迁移文件新增（C3 才引入 seed 迁移）。
- **未解冻** KL-085（`KdsPrincipal`）、KL-086（设备域 `DEFAULT_STORE_ID`）。
- **未实施 §24.3p 任何内容**（含 B1 语义修正）——该卡独立，且前置 SQL 未跑。
- 未做 D1-4（F-1/F-2/F-3 登记，不擅自补做）。
- 提交方式：PG-001 精确 `git add` 逐文件 + `git diff --cached --stat` 核对。
