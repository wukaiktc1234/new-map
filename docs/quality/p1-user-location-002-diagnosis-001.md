# P1-USER-LOCATION-002 阶段 1 诊断报告（diagnosis-001）

> 卡片：**P1-USER-LOCATION-002**（P1-USER-LOCATION-001 收尾补丁卡 · 重档 · 任务板 **§24.3m**；编号更正：原登记 §24.3j 与 P1-INVENTORY-LEGACY-WRITEPATH-001 撞号）
> 日期：2026-09-30 ｜ 方法：**只读静态核验**（代码 + 设计文档 + git），零代码改动、零 DB 改动、零顺手修
> 环境注记：所有结论基于**含 WIP 工作树**（ENV-10：已提交树不可独立编译）；构建 JDK = `P:\my-new-project\JDK21`（ENV-3 更新口径）

---

## 1. 背景与卡定位

P1-USER-LOCATION-001 已收口 `CLOSED_WITH_REGISTERED_LIMITATION`（`ff61f85`，已 push）。收尾核验对比**两个"归属位置"入口**（用户管理 vs 员工管理）时发现 **3 个真缺口**（§2–§4 逐一给证据）。

**设计依据**（判定"缺口"的标尺）：
- [user-store-assignment-design-002.md](../design/user-store-assignment-design-002.md) §5 修订一：`employees.store_id → employees.location_id` 同批改名，"**归属统一**，-001 §1.4 双写规则简化为**单列单写**"；
- 同文 §5.2：**仓库员工与门店员工使用同一字段 locationId**——仓库员工的 location_id 指向 DEPOT/CENTRAL 型 location，"JWT 不区分 storeId/warehouseId，一个 claim 走天下"；
- 同文 §7：仓库员工操作边界（允许/拒绝清单）依赖"本人 location 归属"判定；
- [location-organization-separation-design-002.md](../design/location-organization-separation-design-002.md) §7 规则 1：**location_id 是库存、调拨、员工归属的唯一外键**。

---

## 2. 缺口 ①：员工选"仓库"归属时，`location_id` 不落库（后端）

**证据链**：

| 位置 | 事实 |
|---|---|
| [EmployeeServiceImpl.java:311](../../backend/src/main/java/com/foodtraceability/service/impl/EmployeeServiceImpl.java#L311)（create）/ :353（update） | `employee.setLocationId(LocationIdBridge.locationIdOfStore(dto.getStoreId()))` —— `location_id` **只从 storeId 推导** |
| [EmployeeUpdateDTO.java:33-39](../../backend/src/main/java/com/foodtraceability/dto/EmployeeUpdateDTO.java#L33) | 字段为 `workLocationType` / `storeId` / `warehouseId`，**无 locationId 字段**；选仓库时 `storeId=null`、仅 `warehouseId` 有值 |
| [LocationIdBridge.java:68-71](../../backend/src/main/java/com/foodtraceability/common/util/LocationIdBridge.java#L68) | `locationIdOfStore(null)` → **null**（桥对 null 不做数值兜底，语义正确） |
| [LocationIdBridge.java:54-59](../../backend/src/main/java/com/foodtraceability/common/util/LocationIdBridge.java#L54) | 桥只有**反向** `toWarehouseId`（location→warehouse），**没有正向 warehouse→location 入口** |

**影响**：
1. 仓库员工 `employees.location_id` **恒 NULL** —— 直接违背 -002 §5.2"仓库员工 location_id 指向 DEPOT/CENTRAL location"；
2. **emp-l / emp-n 无法经员工管理入口闭环**（Owner 已裁决"人工分配"，但员工管理这条入口写不进归属）；
3. 仓库员工 JWT `locationId` 为 NULL → §7 仓库员工操作边界（`NoLocationAssignedException` 判定链）对其**无法生效**。

**关联事实（对阶段 2 有利）**：`location_id_map` 兼容视图（迁移 `V20260930_002`）**已含 warehouses 来源映射**（`LocationIdBridge.SRC_WAREHOUSES`，:26）——映射数据已在，缺的只是正向写入口与 DTO 字段。

---

## 3. 缺口 ②：员工管理前端仍用旧两套空间（前端）

**证据链**：

| 位置 | 事实 |
|---|---|
| [EmployeeFormDialog.vue:271-273](../../frontend/src/views/hr/components/EmployeeFormDialog.vue#L271) | `storeArchiveApi.getActiveStores()`（`/v1/stores/active`）+ `warehouseApi.getActiveList()`（`/v1/warehouses`）——**旧两套空间** |
| 同文件 :467-484 | 表单形态 = "归属类型"下拉（HEADQUARTERS/STORE/WAREHOUSE）+ 条件显示"归属门店"/"归属仓库"两个下拉 |
| 对照：用户管理 | 已改**新空间** locations 下拉（commit `500c154`，[api/location.ts:28](../../frontend/src/api/location.ts#L28) `GET /v1/locations`） |

**影响**：同一个"归属"概念在两个入口分别落在 **stores_new/warehouses ID 空间** 与 **locations ID 空间**，口径不一致；违背 design-002 §7 规则 1（location_id 唯一外键）。两入口可对同一人写出互相矛盾的归属值。

---

## 4. 缺口 ③：无 employee → user 反向同步（后端）

**证据链**：

| 位置 | 事实 |
|---|---|
| [UserServiceImpl.java:168-258](../../backend/src/main/java/com/foodtraceability/service/impl/UserServiceImpl.java#L168) | `assignStore` / `assignStoreBatch` / `unassignStore` → `syncEmployeeAssignment`：**仅 user→employee 单向双写**（关联键 `employee_code`，顺带补齐 `employees.user_id`） |
| [EmployeeServiceImpl.java:334-365](../../backend/src/main/java/com/foodtraceability/service/impl/EmployeeServiceImpl.java#L334) | `updateEmployee` 全程只写 `employees` 表，**不触碰 `users` 表** |

**影响**：从员工管理改归属 → `users.location_id` 不变 → **JWT claim / 数据权限（LocationGuard、DataPermission）不同步**；员工档案与账号权限分叉，且分叉无提示。

---

## 5. 权限门现状事实（Owner 约束的落点参照）

| 入口 | 现状 |
|---|---|
| 用户管理 `PUT /v1/users/{id}/assign-store`（[UserController.java:182-186](../../backend/src/main/java/com/foodtraceability/controller/UserController.java#L182)） | 权限码 `system:user:assign-store` + `@OperationLog(module="USER_STORE_ASSIGN")` 审计（P1-USER-LOCATION-001 S5a 交付） |
| 员工管理 `PUT /v1/employees/{id}`（[EmployeeController.java:163-165](../../backend/src/main/java/com/foodtraceability/controller/EmployeeController.java#L163)） | **仅角色门** `hasAnyRole('admin','hr')`，无细粒度权限码、无归属审计 |

**Owner 约束（2026-09-30 裁决，见 §7）**：新增权限码 `hr:employee:assign-location`；员工归属变更落审计（`EMPLOYEE_LOCATION_ASSIGN`）；HR 改归属=变相权限变更，须权限门槛。

---

## 6. PG-001 预检（WIP 碰撞登记）

| 文件 | WIP 状态 | 实施时要求 |
|---|---|---|
| `EmployeeServiceImpl.java` / `UserServiceImpl.java` / `LocationIdBridge.java` / `EmployeeController.java` / `LocationController.java` / `api/location.ts` | **干净** | 直接改 |
| `EmployeeFormDialog.vue` / `UserManagementTab.vue` | **带既有无关 WIP（M）** | **hunk 级隔离**（残留 diff 须逐字等于原 WIP） |

环境事实沿用：ENV-9（WIP 致 ~10 无关单测类失败）、ENV-10（纯提交树不可编译）——本卡所有构建/测试结论须注明"含 WIP 工作树"。

---

## 7. Owner 裁决记录（2026-09-30，本会话四问四答）

| 问 | 裁决 |
|---|---|
| **Q1 归属语义** | **A 同一件事**，附 3 约束：① 新增权限码 `hr:employee:assign-location`；② 改员工归属落审计（`EMPLOYEE_LOCATION_ASSIGN`）；③ HR 改归属=变相权限变更，需权限门槛 |
| **Q2 下拉形态** | **合并成一个位置下拉**（`/v1/locations`）；"归属类型"退化为展示字段（从 `location.type` 推导）；一个下拉带类型标签 |
| **Q3 push `fc2ad96`** | 裁决 **push**；**环境阻断**：本会话 Linux 环境无 GitHub 凭据（无 credential helper / `~/.git-credentials` / gh CLI），须 Owner 在 Windows 机执行或提供凭据后补推 |
| **判档** | **重档**；定性 = P1-USER-LOCATION-001 的**收尾补丁**（非新领域）；编号采 **P1-USER-LOCATION-002**（理由：`-R1` 在本仓是 QA 复验惯例，本卡新增范围——新权限码 + 反向同步 + UI 合并——属新卡而非复验） |

---

## 8. 触点清单（阶段 2 设计输入，**不含方案**）

- **后端**：`EmployeeServiceImpl`（create/update 归属写入）、`EmployeeCreateDTO` / `EmployeeUpdateDTO`（字段形态）、`EmployeeController`（权限门 + 审计注解）、`UserServiceImpl`（现有单向 sync 参照）、`LocationIdBridge`（正向 warehouse→location 入口缺失）、`LocationController`（下拉数据源，已有 `by-store` / `by-warehouse` 端点）、权限码注册面（KL-014 曾登记 4 文件清单不一致，新增码须对齐）、审计面（`@OperationLog` 模式）。
- **前端**：`EmployeeFormDialog.vue`（带 WIP）、`api/hr/employee.ts`（载荷字段）、`UserManagementTab.vue`（对照基准，带 WIP）。
- **数据**：`location_id_map`（warehouses 映射已在）、`employees.location_id`（可空）、`users.location_id`；**预判无新表/无 schema 变更**（阶段 2 核实）。

---

## 9. 阶段边界声明

阶段 1（诊断）**完成**：全程只读；发现的问题未顺手修；未改代码/DB。
**交接条件：Owner 审本报告 → 放行阶段 2（设计）。**

**放行记录（2026-09-30）**：Owner 收下字段入口清单（`docs/quality/field-entry-inventory-001.md`）并明确「不再普查」，指定其为**本卡阶段 2 的输入** → 据此视为**放行阶段 2**；同时给出收窄范围（K1–K3 + 缺口①②③ + N3 + N4 + N6/N7 + N8/N12；排除 N1/N2/N5/N9/N10 与 §4.1/§4.4）。阶段 2 已产出 = [../design/p1-user-location-002-design-001.md](../design/p1-user-location-002-design-001.md)（§8 触点清单已在其中逐条落为方案）。**下一交接：Owner 拍板该设计 §7 的 U1–U7 → 阶段 3 实施。**

**裁决记录（2026-09-30 第二批）**：**已裁 4 项** —— U1 **后写覆盖**（审计记旧值）/ U2 **只写员工表**（不动 `users`、不建账号）/ U4 **显式拒绝**（8 个未迁 `locations` 的 E2E/TEST 仓不猜不兜底）/ U7 授 **`ROLE_HR_DIRECTOR` + admin，不授 `ROLE_STORE_MANAGER`**；四项已落地进设计 §7 与 D1 / D3-1 / C3。**仍待裁 3 项**：U3（`system:user:assign-store` seed 是否补齐）/ U5（是否接受新端点 `PUT /v1/employees/{id}/assign-location`）/ U6（2 处冗余往返收敛是否纳入）→ **此三项 = 阶段 3 交接条件**。**U8 已裁 = 归 B**：「未分配位置账号在 POS 下单撞墙」**不进 002、不加代码兜底**，另开卡 **§24.3p P1-UNASSIGNED-LOCATION-GUIDANCE-001**（任务板）。
