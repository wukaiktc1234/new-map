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
| 3 | JDK 25 + byte-buddy 1.14.10 → mockito 单测须 `JAVA_TOOL_OPTIONS=-Dnet.bytebuddy.experimental=true`（ENV-3） | 跑命令时必带 |
| 4 | Maven `clean` + `danger-full-access`（沙箱拒 `backend/`、`target/`、`.git/` 写） | 构建命令固定 |

---

## §5 进度登记

| 片 | 状态 | commit | 备注 |
|---|---|---|---|
| 开卡（判档/记录） | ✅ 2026-09-30 | 待提交 | 任务板 §24.3h 状态 → 进行中；本记录落盘 |
| S1 DB 迁移 | ⏳ 待办 | — | 已并行勘定迁移规范与受影响面 |
| S2 代码改名 | ⏳ 待办 | — | |
| S3 身份层 | ⏳ 待办 | — | |
| S4 兜底消除 | ⏳ 待办 | — | |
| S5 分配入口 | ⏳ 待办 | — | |
| S6 入职链路 | ⏳ 待办 | — | |
| S7 回填/联调/收口 | ⏳ 待办 | — | |

**（本记录随各片完成增量更新；收口时补 §6 验收证据 + 限制项登记）**
