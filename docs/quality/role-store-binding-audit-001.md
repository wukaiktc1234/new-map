# 角色/门店绑定排查报告 001（role-store-binding-audit-001）

- **日期**：2026-09-26
- **方式**：只读（docs 检索 + 代码核实 + DB 查询 + 1 个活体探测）；未改代码、未改 DB、未建卡
- **背景**：用户报告"登录账号角色自动带出门店/仓库"相关问题（到货登记的门店/仓库来源）

## 一、历史记录查找结果（docs/ 检索）

**找到 4 条直接相关历史记录**：

| # | 位置 | 关键内容 |
|---|------|----------|
| H1 | `docs/data-chain-fix-tracking.md:377` | **采购申请"OA 化"**：创建表单移除部门选择，提示"申请人/所属部门由系统自动带出，不可修改"；后端 `PurchaseRequestServiceImpl.create` 已强制绑定（applicantId/Name、departmentId/Name、**storeId 从 JWT 取**，前端伪造无效） |
| H2 | `docs/business-mode-design.md:52` | **数据权限设计**：`DataPermissionAspect` 按当前用户 storeId 给 queryWrapper 加 `store_id` 过滤（**admin 除外**，全量可见） |
| H3 | `docs/architecture/02-business-dependency/broken-chain-map.md:768`（BC-026） | **role_stores 表存在但无 Flyway 迁移**——"数据范围绑定"链路断裂（与 KL-082 黑箱清单吻合：role_stores/role_departments 本地 DB 不存在） |
| H4 | `docs/招聘流程表.md:9` | "门店ID（storeId）由后端 `SecurityUtils.getCurrentUserStoreId()` 自动注入，前端无需传入"（HR 场景） |

**未找到**：任何"到货登记时按角色自动带出门店"的历史记录；"admin 账号门店选项"的专项讨论亦未找到（仅 business-mode-design.md 提到 admin 全量可见的通用例外）。

## 二、当前行为核实（代码 + DB + 活体）

### 2.1 门店/仓库的决定机制（分环节）

| 环节 | 门店/仓库来源 | 核实证据 |
|------|---------------|----------|
| **到货登记** | **item.planned_store_id / planned_warehouse_id**（订单明细计划字段）→ 无 planned_store 时 receiver_type 兜底 STORE + warehouse 兜底 1（代码注释"集中式单店"）；**与登录账号无关** | `PurchaseArrivalServiceImpl` ReceiverKey（L771-782）+ `arrival.setWarehouseId(key.warehouseId ?? 1)`；audit 实测：order 72 item.planned_store_id='1' |
| 采购申请 | DTO.storeId（前端传）→ `resolveValidStoreId` 校验 stores_new，**无效回落默认门店 1**；注释称"storeId 从 JWT 取"（H1），但实际 create 用的仍是 DTO 值+回落逻辑 | `PurchaseRequestServiceImpl.resolveValidStoreId`（L329-343） |
| POS 下单 | 请求 DTO.tableNumber/storeId（前端传） | `PosOrderCreateServiceImpl.createOrder` |
| 库存数据权限 | `DataPermissionAspect`：permissionInfo.storeId 非空 → queryWrapper 加 `store_id=?` 过滤；**admin（isAdmin）→ 不过滤（全量）**；普通用户 storeId 为空 → 也不过滤 | `DataPermissionAspect.java:113-164` |
| KDS 设备 | **唯一真正绑定门店的通道**：token 格式 `kds_{storeId}_{secret}`，principal=storeId，TrayController bind-order 校验 KDS 门店与托盘门店一致 | `KdsTokenFilter.java:23-24` + TrayController |
| 调拨/报表 | 未逐环节核（本轮范围外）；DataPermissionAspect 覆盖面内同理生效 | — |

### 2.2 数据模型与 admin 定义

| 项 | 现状 |
|----|------|
| 用户-门店绑定 | `users.store_id` 列存在（User 实体 storeId）；**实测 admin/emp-b/emp-c 全部为空** |
| 角色-门店绑定 | `role_stores` 表**本地 DB 不存在**（BC-026：无 migration；KL-082 黑箱清单成员）——数据范围绑定链路本就断裂 |
| 权限缓存 storeId | `UserPermissionCacheServiceImpl:173` `storeId = user.getStoreId()`——数据源即 users.store_id（空 → null） |
| JWT 主体 | `SecurityUser` **无 storeId 字段**（SecurityUtils 注释自认），`getCurrentUserStoreId()` 在 JWT 链路**恒返回 null**（仅当 principal 为 User 实体才有值） |
| admin 定义 | permissionInfo.isAdmin → DataPermissionAspect 跳过过滤；admin users.store_id 为空 |

### 2.3 活体探测（审计申请 PR20260926002）

- `POST /v1/purchase/requests/{rid}/generate-order`（转单预览端点）→ code=0，回显 status=**completed**（申请状态在生成订单后被置 completed——补充审计报告未记录的状态值）、storeId='1' 回显
- 勘误：此前 `procurement-chain-flow-audit-001.md` 议题 1 写"无从申请生成订单的端点"**不准确**——`POST /{requestId}/generate-order` 存在（转单预览），但 `createOrder` 仍要求 items 手工传（半自动结论维持：预览与正式创建是两个调用，items 仍需客户端二次组装）

### 2.4 admin 账号实际行为总结

| 环节 | admin 看到什么 |
|------|----------------|
| 到货登记 | 门店/仓库来自订单 item planned 字段（非账号）；未规划时仓库兜底 1、门店空 |
| 采购申请 | storeId 由前端传 + 校验回落 1；admin 的 JWT 无门店可带 |
| POS 下单 | 查询/写入均无门店强制（admin 无过滤 + 无绑定） |
| 库存查询/报表 | DataPermissionAspect 对 admin 不过滤 → **全量数据可见** |
| KDS | 需专用 kds_ token（带 storeId）才受门店隔离 |

## 三、影响面评估

### 3.1 设计层面的问题（不只是 admin 的问题）

| # | 问题 | 影响环节 |
|---|------|----------|
| P1 | **JWT 链路门店隔离名存实亡**：SecurityUser 无 storeId → `getCurrentUserStoreId()` 恒 null → DataPermissionAspect 对**所有 JWT 用户**（含普通角色）都不过滤门店——普通用户与 admin 一样全量可见。H2 描述的"按用户 storeId 过滤"实际未生效 | 库存/报表/订单等所有被 Aspect 覆盖的查询 |
| P2 | **role_stores/role_departments 无 migration 且本地不存在**（BC-026/KL-082）：即使代码要按角色-门店过滤也没有数据表支撑 | 数据范围绑定的根基 |
| P3 | **到货门店取决于订单 item planned 字段**，而该字段来自前端手工传值（小写还会炸 CHECK，见 procurement audit A2）——与账号无关，但"传错店/漏传"无校验拦截 | 采购到货/收货 |
| P4 | users.store_id 空且无管理端点维护（待核实用户管理是否可编辑）——即使修复 P1 也无数据可用 | 全部依赖 users.store_id 的环节 |

### 3.2 admin vs 普通角色用户

| 用户类型 | 受影响环节 | 说明 |
|----------|------------|------|
| admin | 无隔离（全量可见）——符合 H2 设计 | 不受影响（设计内例外） |
| 普通角色（JWT 用户） | **与 admin 完全相同的行为**（本应按门店过滤但 storeId 恒 null）→ 受影响：**能看到/操作全部门店数据** | P1 的实际受害者是数据隔离预期，而非 admin |
| KDS 设备 | 正常门店隔离（kds_ token 唯一生效的绑定） | 不受影响 |

## 四、建议方向（不实施）

1. **P1（先决）**：确认数据权限的产品预期——若需门店隔离，先补 role_stores/role_departments Flyway（BC-026/KL-082 消化）+ users.store_id 数据维护端点，再让 SecurityUser/JWT 携带 storeId
2. **A2 联动**：到货 planned_receiver_type 大小写归一（procurement audit A2 修复卡内一并处理）
3. **文档**：business-mode-design.md H2 描述与实现现状的差距应标注（本轮不改原文件，遵指令）
4. 本报告发现与 KL-082（黑箱表）强关联：role_stores/role_departments/sys_roles 等的 Flyway 补齐应在 P0-FLYWAY-COVERAGE-001 中一并规划
