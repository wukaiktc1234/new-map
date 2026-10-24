# STATUS.md — 单一真相源（轻档，只写文档）

> **本文件是唯一状态源。** 其他入口（`docs/INDEX.md` / `remediation-roadmap.md` / `production-remediation-task-board.md` / `production-known-limitations.md` / `docs/project-context/repo-state.md`）只放指向本文件的指针，不各自维护状态。
> **同步规则（PG-002 扩展）**：每次卡片收口或 push 后必改本文件；其他入口不改状态。
> **性质**：视图层快照，非权威；与 git 不一致时以 git 为准。快照时点：2026-10-15。

## 1. 当前 HEAD 与同步状态

| 项 | 值 |
|----|----|
| 分支 | `master`（单分支工作流） |
| HEAD | **上次同步时点：2026-10-15**（不写具体哈希——任何提交都会使其过时）→ 以 `git rev-parse HEAD` 为准 |
| 远程 | `origin` = https://github.com/wukaiktc1234/new-map.git；`origin/master` 以 `git rev-parse origin/master` 为准 |
| 同步 | **2026-10-15 Owner 指令 push 已执行**：本轮（N30 → N31 收口）全部提交已推送，**ahead=0 / behind=0**，以 `git rev-list --count origin/master..HEAD` 实测为准；push 仅由 Owner 显式指令触发；`.git/hooks` 实测无 `post-commit`（PG-001 规则 6） |
| 工作区 | **547 条目 = 456 M + 90 ?? + 1 D**（Owner WIP，留盘不暂存；本轮全程未变） |
| **会话收口（2026-10-23）** | POS/KDS 主链路全线闭环并实测通过：点单 → 支付 → 后厨接单 → 开始制作 → 完成制作（出餐）→ 扣料 → 追溯码 → 成本入账 → 财务收入（幂等防重）。**E2E 断言三端一致**（`order_status=2` / `payment_status=2` / `kds=completed` / `material_consumed=1`）。本轮共 20+ 提交，最新见 `git log`。收口快照：orders 82 / kitchen_order 83 / food_trace_code 14 / cost_records 33 / finance_records 32 / tray 13 / dining_tables 0 |
| 现场修复轮（2026-10-23） | POS/KDS 全链路收口：`§24.19a N33` POS 登录闸门 → `§24.20a N34` 后厨 401 定性 + 三处链路缺陷 + E2E 脚本 → `§24.21a N35` dish_items canonical + 门店归属 → `§24.22a N36` KDS 前端凭证接线 → `§24.23a N37` 状态机闭环（OrderLifecycleService / 日结口径 / 状态码归一 / cost_record）。**主链路已实测闭环**：点单→支付→后厨→出餐→扣料→追溯→成本入账。迁移 V004–V007 已用 psql 落库，`flyway_schema_history` 待下次启动统一登记 |

## 2. 活跃卡（只列 7）

| 卡 | 状态 | 下一个动作 |
|----|------|-----------|
| **P1-AUTH-CHAIN-001**（**C 卡已完成（2026-10-24）**） | **C 卡已完成（2026-10-24）**：`users.roles` 彻底退出鉴权链路，**13 文件 + 3 单测 + 篡改验证**（把 `qa_admin` 的 `users.roles` 改成 `["1"]` 后 roles/权限不变）；审计脚本 `scripts/db-fix/2026-10-24-audit-users-roles-divergence.sql`；`getUserRoleNames` 改返回 role_code、新增 `getUserRoleDisplayNames`；admin 用户名特判全系统仅剩 `PosAuthServiceImpl` 一处（已加注释）。前情：N30（`RoleCodeNormalizer` 剥前缀）+ N31（B 卡 239 处幻码 + Flyway 补 286 码到 418 + `AdminPermissionCatalog` 兜底）均已修，活体 admin 5 API 200 | 影响面仅 **3 个 FINQA 测试账号**（其 `users.roles` 非 JSON 字面量，旧解析本就失败，**未掉权**）；待跟进：读中文角色名的消费方切到 `getUserRoleDisplayNames` |
| **P1-DASHBOARD-FILTER-001**（**R8 已实现：开关式，默认关闭**） | **已实现（开关 `app.dashboard.store-scope.enabled`，默认关闭，Owner 放行后置 true 生效）**。① 配置：`DashboardStoreScopeProperties`（`@ConfigurationProperties(prefix="app.dashboard.store-scope")`，字段默认 `false`）+ `application.yml` 同名配置项（支持 `APP_DASHBOARD_STORE_SCOPE_ENABLED` 覆盖）；② 解析：`DashboardStoreScopeResolver`，链路 JWT `locationId` → `users.location_id` → `employees.location_id`（`employees.user_id` 关联）→ `LocationIdBridge` 反查 `stores_new.store_id`（严禁数值兜底）；admin → 全量；解析不到 → empty（=不限门店，打 WARN）；③ 注入：`DashboardServiceImpl` 5 个入口按**真实列**追加条件（`orders.store_id`、`inventory.location_id`；**members 表无门店列、finance-summary 仍是 0 占位**，两处已在代码注释登记为不可过滤）；④ 单测 `DashboardStoreScopeTest` **6/6 绿**（开关关=不加条件 / 开店=含 store_id=2 / admin=不加 / 无绑定=不加+WARN / employees 回落 / 无门店映射仅位置维度生效）；⑤ 活体 A/B 已验（见右列）。**⚠ 开启前必须确认**：`users.location_id` / `employees.location_id` **数据是否齐全——缺绑定会被判为「不限门店」（与现状一致），仍可能越权**；另需确认会员增长与财务概况两处无门店维度。⑥ 原卡「修 `DashboardServiceImpl:114` 的 500」一并收口：`getInventoryAlerts` 查的是幻列 `inventory.warning_status`（`information_schema` 实测不存在）→ 每次 BadSqlGrammarException 被 catch 吞成常量 0；现按 `quantity/safety_stock/max_stock` 真实列计算，端点由 0/0/0 变为真实 **4/4/0** | **Owner 已裁决（2026-10-24）：暂不开启 `enabled=true`**——开关保持默认关，且不加任何「location 为空→回退/跳过」兜底；待业务侧完成 **90% 以上用户归属绑定**后再开（覆盖率现状与依据见 §3 的 R8 行）。开启后再按角色重新授予 `operations:dashboard:view`。活体证据（2026-10-24 02:0x，:8089 实例）：开关 **true** + `vcashier01`（location 1 → store 1）→ `todayOrderCount=125 / todayAmount=227080`（与 SQL 真值 125/227080 一致）、`inventory-alerts warningCount=0`（location 1 真值 0）；同为 true 的 `qa_admin`（无绑定）→ 139/252280 全量 + WARN 日志；开关 **false** 时两人均为全量 139/252280（行为零变化）；5 个端点 + `overview/{1,3,4,5}` 全部 HTTP 200 |
| **P1-APK-DEVICE-001** | 卡号/优先级已确认（Owner 2026-10-13）：**P1 / 整特性一次入库**；三项 `.gitignore` 已执行（`48750ce` + `c13cb2a`）；5 个生成模板文件已排除（§24.3y），候选 **59**；**本次保留 `DeviceActivationPage` / `DeviceWhitelistPage` / `VerifyCodePage` 三个组件并加注，等接线**（孤儿页清理未动它们） | Owner 放行后整特性入库，含 4 项卡面待补：guards.ts APK 四块 / request.ts X-Device-Id + FormData / index.ts 三条路由 / SecurityConfig 3 条 permitAll 与 `meta.requireAuth` 口径核对 |
| **§24.3s（N15 order:add）** | 实施 CLOSED；应急补桥已 Live 通过（`user_roles` id=2329，`/me` 返回 12 码） | **已收口**（原「被 P1-AUTH-CHAIN-001 阻塞」已解除） |
| **P1-SEC-009** | QA 渗透验证未关闭（**发布门禁前必须关闭**）；执行窗口已裁决（§24.3x）：**排在 P1-APK-DEVICE-001 入库后一起跑** | 覆盖 device-whitelist / verify-code / `usesCleartextTraffic="true"`；Owner 安排窗口。**本次保留 `DeviceActivationPage` / `DeviceWhitelistPage` / `VerifyCodePage` 三个组件并加注，等接线**（与 P1-APK-DEVICE-001 同一批） |
| **P0-SEC-001** | 第二阶段 71 Controller 审计，**暂缓**（Owner 2026-10-12） | Owner 放行后恢复 |
| **P0-WORKSPACE-WIP-CONSOLIDATION-001** | **暂缓**（GOV-003 分类表为其输入） | Owner 裁决五批入库 |

## 3. 挂起项（只列 16）

| 项 | 类型 | 说明 |
|----|------|------|
| **TRAY-005** 托盘门店归属管理缺失 | 新增（2026-10-23，待裁决） | ① 库内 2 个 `store_id=NULL` 托盘永久不可用（KDS 被门店校验拒绝、管理端无分配入口）→ 建议加「托盘分配门店」功能，**不建议**"KDS 首次绑定自动认领"（静默改写归属）② 若连锁门店间会调拨托盘，硬隔离不可行 → 建议独立「托盘调拨」业务动作（换店 + 审计），而非直接改 `store_id` |
| **TRAY-006** 手动流转轨生产收敛 | 新增（2026-10-23，待裁决） | 新增手动轨 `POST /v1/tray/manual-transition` 当前仅需 `kitchen:edit`。生产建议加开关 `app.tray.manual-override-enabled`（dev 开 / prod 关），或收敛到 admin。已内置：理由必填、写 `tray_scan_record(scan_type=MANUAL)`、跃迁白名单、**禁止手动置 served**（承载扣料+追溯码+成本） |
| **DATA-006** 历史 0 元财务流水 | **已执行（2026-10-24）** | `finance_records` 有 10 条 `amount=0` 的「订单收入」流水（`SZ202609170001`/`SZ202609210001`/`SZ202609250002`/`SZ202609260002`/`SZ202609300002`/`SZ202609300005`/`SZ202609300008`/`SZ202609300011`/`SZ202610230005`/`SZ202610230007`），均产生于事件金额修复前。**已于 2026-10-24 软删（`deleted=1`）10 条**，不再污染报表 |
| **CODE-007** 死代码 `recordOrderCost` | **已清理（2026-10-24）** | `OrderCompletedEventListener.recordOrderCost` 依赖恒为 null 的 `event.materialCost`（成本由 lifecycle 同步写 `cost_records` 以避免重复入账）。**已清理（2026-10-24）**：`OrderCompletedEventListener` 的 `recordOrderCost` 及 `costRecordService` 字段/构造参数已删（本轮死代码清理 14 方法 + 2 字段 / 12 文件 / 约 −176 行）。**注意**：`OrderCompletedEvent` 的成本字段仍在（事件契约，未删） |
| **TECH-DEBT-001** 单/复数双表清理 | 新增（2026-10-23）· **未做（2026-10-24 复核仍挂）** | 库内并存 `cost_record`(空) / `cost_records`(实体实际用)、`tray` / `trays`、`dish_recipe` / `dish_recipes`、`order_material_requirement` 等；`dining_table` 实为**视图**而非表。需逐一核对实体 `@TableName` 与真实对象，清理遗留空表，避免再次"查对表名却查空" |
| **DATA-007** 扫码确认入库不落统一库存账 | 新增（2026-10-24，P1 建议） | 事实：`MaterialTraceCodeServiceImpl.updateInventoryAfterConfirm` 只 `new Inventory` + 写日志，**未调用** `InventoryService.increaseStockAtLocation`；`createPurchaseStockinRecord` 绕过 `PurchaseStockInEvent` → `cost_records` 不写。后果：扫码入库与采购入库两套账，库存与成本双口径。建议：并入统一库存服务 + 走事件链路 |
| **DATA-008** 孤儿管理员 `test-admin` 处置记录 | **已执行（2026-10-24）** | `test-admin`(`user_id=999`) **停用并撤销 `ROLE_ADMIN`**；脚本 `scripts/db-fix/2026-10-24-disable-orphan-test-admin.sql`（**含备份表与回滚段**）。留痕：该账号此前属越权面（admin 权限 + 无业务归属） |
| **ARCH-EVENT-002** 事件 outbox | 新增（2026-10-23） | `OrderCompletedEvent` 现已在事务内发布（AFTER_COMMIT 能触发），但仍是"发出去不管"：进程崩溃/异步失败即丢事件（财务收入、应收、成本联动丢失且无痕）。建议加 `event_outbox` 表：事务内落库 + 异步投递 + 重试 |
| **OPS-BOM-003** BOM 缺失可视化 | 新增（2026-10-23） | 已裁决「无 BOM 硬拒出餐」（P0 严格版）。现仅抛 `FOOD_NO_RECIPE` 错误，缺运维入口：应提供"缺配方菜品清单"报表/告警，让厨师长主动补 BOM，而不是等出餐失败才发现 |
| **DATA-004** 桌台基线数据为空 | 新增（2026-10-23） | `dining_tables` 当前 0 行；锁桌/解锁语义已修复（列类型 varchar(50)，含依赖视图 `dining_table` 重建）并经 SQL 事务验证，但无真实桌台可做端到端验证。需先导入门店桌台配置，或走桌台管理 API 建桌后再复验 |
| Live 验证批次 A/B/C + N15 Live | 等 Owner 执行 | Agent 不跑 Live 验证 |
| **ENV-14** `flyway_schema_history` 卫生异常（20260922.001 checksum=NULL + 缺 V999） | 已裁决（2026-10-13） | 无阻断（Flyway 容忍）；**不 repair**；**顺手修，不另开卡**——等下一个碰 `flyway_schema_history` 的卡（如未来某次 migration 卡）一并处理 |
| **P1-SEC-002** | **部分收口（验收依赖 P1-APK-DEVICE-001）** | 卡面已提交（`eec8fb9`），但 `/verify-code`、`/device-activation` 路由条目属该卡簇 → 提交面无该路由，验收标准（未登录不重定向）不可执行；Live 验证须待该卡入库 |
| 被忽略 `*.md`（非 production 面）——**实测 13 个**（不是 15） | 等 Owner 逐条裁 | 8 清（release-* / sprint-* / 方案 / 里程碑）+ 2 留（debug-*）+ 5 待裁（oic-1 / oic-2 / product-decision-* / frontend-improvement-backlog；同族不一致：`oic-be-task-board.md` 已 tracked）；4 条通配无对应文件为误伤根源。逐条清单已登记 KL 追加 |
| **R8 / P1-DASHBOARD-FILTER-001** 看板门店隔离**暂不开启** | Owner 裁决（2026-10-24）+ 待业务侧数据达标 | **多门店看板隔离待业务侧完成 90% 以上的用户归属绑定后，再开启。** 开关保持默认关（`app.dashboard.store-scope.enabled=false`）；**禁止**加「location 为空→回退默认门店 / 跳过过滤」兜底；测试账号与总部职能账号无 location 属正常，不造假补齐。依据：启用用户 location 覆盖 23.81%（21/5）、在职员工 21.43%（14/3）、悬空 location 0、交叉兜底 0、近 30 天 617 单 100% 可按门店限定（`scripts/qa/r8_location_coverage.sql` / `.py`） |
| **Owner push**（**第一优先级**；commit 数以 `git rev-list --count origin/master..HEAD` 为准） | 持续项 | 2026-10-13 已 push 64 commit（`671d87f` → 当时 HEAD），现 ahead=0；本文件之后的 commit 仍需 Owner push；无 auto-push hook（PG-001 规则 6） |

## 4. 禁区（不该碰）

- **Live 验证环境口径（Owner 选 C：混合快照，2026-10-13）**：= tracked-only worktree + **13 个必要未跟踪类**（最小可编译集）；纯 tracked 树编译不过（ENV-10）。结论须注明「混合快照（tracked + 13 WIP 类）」，**不得声称纯 tracked**。
- **Owner WIP**：456 M + 90 ?? + 1 D 不暂存（与第 1 节同一口径）；PG-001 精确 `git add <文件>`，禁 `git add .` / `-A` / 目录通配。
- **`flyway_schema_history`**：禁 `repair`（历史篡改需 Owner 逐次批准）。
- **`inventory_warning` / `inventory_warning_rule`（单数 legacy 表）**：不碰；现行是复数 `inventory_warning_records`（rank 214）。
- **治理文档历史面**：任务板 / KL / incidents / 实施记录 = **只增不改**。
- **hunk 隔离（index 手术）**：PG-001 **条款 7**（2026-10-13，GOV-INCIDENT-005）——凡用 index 手术做 hunk 隔离的卡，收口前必须登记「提交面 vs 工作区差异」并说明 Live 验证跑哪一侧。C6 首例（`dd6ddcc` 提交面有 5 hunks、工作区无 → 2026-10-13 已回贴到工作区，**未暂存**）。
- **KL-085 / KL-086 冻结项**：不解冻；`DEFAULT_STORE_ID` 8 行使用点维持冻结。
- **`.gitignore` ENV-4 残留专用段**：不批量改，逐条经 Owner 裁决。
- **未跟踪特性簇**（APK 设备 / P1-A 银行 / 原料扣减 / Receipt）：属 Owner WIP，不自行提交。
- **手写 schema 脚本**：禁止（PG-003）；表结构变更一律 Flyway（只增不改）。
- **含 schema/seed 的卡**：收口必须含 `flyway_schema_history` `success=t` 行，且迁移经**独立 Flyway runner**（`mvn test` 上下文不算，PG-001 v2 条款 2/3）。

## 5. 新会话第一步

1. 读本文件。
2. 读 `docs/project-context/process-guards.md`（PG-001 v2 / PG-002 / PG-003 / PG-006 判档）。
3. 读 `production-governance-incidents.md`（GOV-INCIDENT-001–006）。
4. 读任务板**尾部**最新追加段（当前 §24.10a–§24.24a）——只增不改，读尾部而非中部。
5. 跑 git 事实核对（HEAD / ahead-behind / status），不依赖本文件快照。

## 6. 夜间自主巡航（2026-10-23，分支 `ai-night-patrol-1`）· 只增不改追加段

> 性质：Owner 休息期间的无人值守扫描结果。**本节不改动上方任何既有结论**；与上文冲突处以本节实测为准，并已在上文对应条目下留指针（若未留指针，以本节为准）。
> 详细日志：`NIGHT_PATROL_LOG.md`；雷达报告：`docs/night-patrol/radar2-schema-drift-report.md` / `radar3-dirty-data-report.md`。

### 6.1 已修复并提交（8 个提交，全部 `mvn -o compile` BUILD SUCCESS）

| 批次 | 提交 | 内容 |
|------|------|------|
| 1 | `b746e66` | 雷达4：7 文件 9 处空安全/边界（Integer 拆箱 NPE×3、IpUtils 内网判定 AIOOBE+NFE、JwtUtil 无 exp NPE×2、DesensitizeUtils switch null、ImageUploadUtils file null、FinanceAssetController assetId null→500 与 months=12.0→400 误报） |
| 2 | `d1c1f9d` | 雷达1：6 文件 7 处假成功 + **CODE-007 已删除**（`OrderCompletedEventListener.recordOrderCost` 死代码） |
| 3 | `20adc43` | 雷达1：7 文件 10 处无日志静默吞异常（JsonbListTypeHandler 追溯码解析静默清空、VoucherNoGenerator 凭证重号、PermissionInitRunner 只进 System.err 等） |
| 4 | `947cf80` | 雷达2+3：2 份审计报告 + 7 个只读 SQL 脚本（剥离注释后零写语句） |
| 5 | `5491cda`+`7dfd3fa` | 新增 `scripts/check-table-name-exists.py` 自动化闸门 + 69 项漂移基线 |
| 6 | `0ded263` | 雷达4 第二批：5 文件 6 处（Budget/Payable/Receivable/AccountingSubject/FoodTraceability 拆箱 NPE 与编号解析崩溃） |

**未入库说明**：工作区 448 M / 91 ?? 的 Owner WIP 全程未暂存；每批次均用 `git diff --cached --numstat` 复核仅含本批文件。带 WIP 的同族缺陷一律「只报不改」。

### 6.2 对既有条目的实测更正（TECH-DEBT-001 / DATA-006）

| 原记载 | 实测更正 |
|--------|----------|
| TECH-DEBT-001「`tray` / `trays` 双表并存」 | **`trays` 表根本不存在**（`information_schema` 实测），只有 `tray` 13 行 |
| TECH-DEBT-001「`order_material_requirement` 属双表残留」 | 不是双表：该表**存在但全表 0 行**，且 21 张 `material_consumed=1` 的厨单对应扣料明细**从未落库** |
| TECH-DEBT-001 未提及 `food_trace_codes` / `dish_recipe` / `food_category` / `recharge_plans` | 这 4 对是**代码读写的是空表**（0 行），真数据在单数/复数另一侧（14 / 12 / 9 / 496 行）——严重度高于原记载 |
| DATA-006「10 条 0 元流水建议软删」 | **不建议直接软删**：10 条对应订单均已支付且真实存在；同时 `orders` 里 **25 条已支付订单 `actual_amount` 全为 0.00**（`final_amount=total_amount` 25/25、真 0 元订单 0 条）→ 软删只是掩盖金额未写回的缺口 |

### 6.3 新增待裁决项（建议 Owner 优先看这 4 条）

| 编号 | 问题 | 建议 |
|------|------|------|
| **SCHEMA-DRIFT-001** | `@TableName` 指向活库不存在对象 **65 处 + 单复数错侧 4 处**；其中 40 处 Mapper 已被 Controller 链路注入 → 查询返回空、写入静默失败，**日志里没有任何痕迹** | 按 `scripts/check-table-name-exists.py` 的清单逐条定：修 @TableName / 补 Flyway / 或登记基线。基线已建（69 项），**基线外新增即 exit 1**，可先接进收口流程防扩散 |
| **SCHEMA-DRIFT-002** | `DiningTable` 实体 `@TableName("dining_table")` 指向的是**视图**（可读不可写）→ 任何 save/update 必失败 | 确认是否应改为写基表；同时呼应 DATA-004（`dining_tables` 0 行） |
| **FIN-AMOUNT-001** | 25 条已支付订单 `actual_amount=0.00` | 先修金额写回，再决定 DATA-006 是否软删；`scripts/night-patrol/05-*` 已备好对账三段 |
| **DATA-007** | `cost_records.remark` 含字面 `" - null"` 25 行；`food_trace_code.dish_name` 空 8/14 行 | `scripts/night-patrol/02-*` / `03-*` 已备（默认只预览，变更段封在 `[OWNER-EXECUTE]` 注释内） |

### 6.4 明确「不敢修」的项（红线或需 Owner 决策）

- `OrderLifecycleServiceImpl.java:116-119` javadoc 仍描述已删除的 `recordOrderCost`（**核心状态机红线**，仅注释问题，留 Owner）。
- `interceptor/PermissionInterceptor.java:123` `permission.getStatus() == 1` 无判空 —— 鉴权链路 NPE，但「null 视为禁用」属授权语义，**权限底层红线不碰**。
- `service/impl/MaterialTraceCodeServiceImpl.java:441-453` 组装 `Inventory` 后**从未落库**，只打「更新门店库存」日志 —— 比吞异常更彻底的假成功，属功能缺口（`InventoryService` 无对应写入方法），需 Owner 定接口。
- `scheduler/MonthlyClosingScheduler.java:67-122` 成本结转/折旧金额写死 `0L`、账龄更新空 TODO，日志却打印「月末结账任务完成」；`scheduler/impl/DefaultTaskExecutor.java:90-133` 用 `Thread.sleep(100)` 模拟备份却返回 success —— 均为结构性假成功，接真实服务属新增依赖，需立卡。
- 带 Owner WIP 的 6 处同族缺陷（`PosOrderNumberGenerator:136-141` 序号归零→重复 order_code、`PurchaseOrderServiceImpl:242-249` 静默 null 供应商、`PosOrderQueryServiceImpl:226-233` 详情转换失败仍 success、`OrderTimeoutTask:102,134-136` private 方法上的 @Transactional 失效、`BankAccountServiceImpl:303`、`SchemaFixMigration:71-74` debug 降级）—— **只报不改**，待 Owner WIP 落定后处理。

### 6.5 独立复核（reviewer 只读复核 8 个提交）新增的 4 项

| 编号 | 问题 | 关键证据 |
|------|------|---------|
| **SEC-JWT-003**（**建议优先**） | `security/utils/JwtUtils.java:451,541` `claims.getExpiration().getTime()` 无判空 → 无 exp 的 token 直接 NPE；:451 的 catch 只捕 `JwtException`，NPE 冒到注销接口 → 500 | 这才是**真实鉴权链**用的类：`JwtAuthenticationFilter:118` / `WebSocketConfig:113` / `TokenServiceImpl:86`。批次 1 修的 `util/JwtUtil` 全仓零调用方 |
| **SEC-JWT-004** | `security/utils/JwtUtils.validateToken:267` 只靠 JJWT `parseSignedClaims`，**无 exp 的 token 在真实链路仍被判有效** → 「无 exp 视为过期」政策未在真实链路落地 | 实际风险限于持密钥伪造者（token 生成 :197/:245 恒带 exp），评 P2 |
| **ARCH-EVENT-003** | `OrderCreatedEventListener:25-26` 用裸 `@EventListener` + `@Async`（DF-011 只落地到 OrderCompleted / PurchaseStockIn）→ 主事务回滚时原料锁定仍会执行 | 预存在设计缺口，非本轮引入。改 AFTER_COMMIT 前须确认发布点事务上下文，否则事件在无事务上下文发布时监听器**完全不触发**、原料锁定静默失效 |
| **ARCH-EVENT-004** | `PurchaseStockInEventListener:50-51` 在 AFTER_COMMIT 阶段抛异常，**没有事务可回滚** → 可能「追溯码已提交 + 采购成本失败」半成功 | 批次 2 提交说明里的原子性承诺**撤回**（代码无回归，但一致性未保证）。建议与既有 **ARCH-EVENT-002（事件 outbox）** 一并立卡 |

> **更正声明（只增不改）**：`d1c1f9d` 提交说明中「监听器均为 @Async+AFTER_COMMIT+REQUIRES_NEW 不影响主事务」**不成立**，以本节为准。实测：只有 `OrderCompletedEventListener:75-77` 三件套齐全；`OrderCreatedEventListener` 为 `@Async + @EventListener`（但其外层 `:35-40` catch 捕获后不重抛，故无异步未捕获异常回归）；`PurchaseStockInEventListener` 缺 `REQUIRES_NEW`。


## 7. 2026-10-23 白天值守轮（12:00–13:30）· 只增不改追加段

### 7.1 本轮最高成就（一句话）

**POS 主链路彻底闭环且体验对标真实门店**：支付入口统一（消灭 quick-pay 后门）、叫号号与订单号解耦（A001/B023 式）、退款不再要求「已完成」、POS 图片与时间显示修复、两个前端类型检查双双归零。

### 7.2 已完成的验证（全部可复跑，数字为实测）

| 类别 | 命令 | 结果 |
|---|---|---|
| 单元测试 | `mvn -o -B test -Dtest=<8 个新增 Mockito 类>` | **57 例，0 失败** |
| 核心链路 E2E | `python scripts/e2e_core_chain.py --password 123456 --tray auto` | **PASS=12 / FAIL=0 / SKIP=1** |
| 页面自检 | `python scripts/e2e_wip_pages.py` | **PASS=11 / FAIL=0** |
| 管理端类型 | `frontend: npx vue-tsc --noEmit` | **0 error** |
| POS 类型 | `frontend-pos: npx vue-tsc --noEmit` | **0 error** |
| 后端编译 | `mvn -o -B compile`（JDK21） | BUILD SUCCESS |

### 7.3 本轮交付（提交见 `git log`）

1. **统一 POS 支付入口**：`order/pay` 新表优先（分口径金额校验、写 `order_payment_records`、回写 `payment_status=2`），旧表兜底；`quick-pay` 标 `@Deprecated`。
2. **退款前置放宽**：`payment_status=2 且 paid>refund` 即可退（不再要求已完成），写 `order_refund_records` + 回写 `order_status=5`；POS 前端 `canRefund()` 同步放宽。
3. **叫号号段**：`orders.call_number` / `call_record.call_number`，格式 `A001`，堂食 A / 外卖 B / 自提 C，按门店+自然日重置，原子取号 + CAS 幂等，小票/叫号屏/大屏/顾客端全改叫号号。
4. **POS 图片与时间**：`utils/image.ts#getFullImageUrl`（补 `/api` 上下文前缀、回环 host 统一）、`utils/format.ts`（YYYY-MM-DD HH:mm:ss，去 T 与微秒），13 处本地实现收敛。
5. **手动流转理由非必填**：未填落默认「收银员手动流转」，审计仍写。
6. **类型收尾**：`InventoryCreateDTO.creatorId`、`Department.parentId→number`、`MaterialArchiveFormData` 五字段可选；管理端 `vue-tsc` 49→0。

### 7.4 遗留风险全量清单（挂起，不做静默处理）

| # | 风险 | 级别 | 建议动作 |
|---|---|---|---|
| R1 | `OrderNewServiceImpl.createPosQuickOrder` 建单不即取号（禁改类） | 低 | POS 前端未调用；需 Owner 授权后在收尾插 `ensureCallNumber` |
| R2 | POS 支付仍「新表优先 + 旧表兜底」双轨 | 中 | 下轮下线 `orders_legacy` 支付写入 |
| R3 | `orders` 无 openid 列 | 中 | 顾客端「我的订单」对新表订单不可用，需加列或建映射 |
| R4 | 混合期 POS 列表只显示新表 | 低 | 历史单需回查旧表，按业务需要补开关 |
| R5 | `InventoryCreateDTO.creatorId` 前端已传，后端列待复核 | 低 | 核对 `inventory_check` 表是否有 creator_id |
| R6 | `Department.parentId` 语义（0=顶级）待产品确认 | 低 | 与 HR 模块确认后再统一其他调用点 |
| R7 | P1-AUTH-CHAIN-001 C 卡未启动 | 高 | 见 §2 |
| R8 | P1-DASHBOARD-FILTER-001 看板无门店级过滤（越权） | 高（**代码已收口，风险降为数据归属**） | **Owner 裁决（2026-10-24）**：① **暂不开启** `app.dashboard.store-scope.enabled`；② **绝对不加**「location 为空的用户回退默认门店 / 跳过过滤」这类兜底（跳过=没修越权；回退默认门店在多店场景会串数据）；③ 保持现状（开关默认关），**多门店看板隔离待业务侧完成 90% 以上的用户归属绑定后，再开启**；④ 测试账号与总部职能账号没有 location 是合理的，不强行造假数据补齐。**只读排查依据**（`scripts/qa/r8_location_coverage.sql` / `.py`，可复跑）：启用用户 location 覆盖 **23.81%（21 启用 / 5 有值）**；在职员工覆盖 **21.43%（14 / 3）**；悬空/失效 location **0**；交叉兜底为 **0**（不能靠 employees 补齐）；近 30 天订单 **617 单 100% 可经 stores_new→location 限定**。**结论：代码已实现（开关式，默认关），风险点是数据归属未达标，不是代码缺陷** |
| R9 | 托盘绑定 E2E 恒 SKIP | 低 | 跑前先释放一张托盘 |
| R10 | 收编页面未做浏览器级联调（签名画板/扫码桥/离线重传） | 中 | 真机过一遍；接口闭环已由 11 项自检覆盖 |

> 详细改动与证据见 `WORKER_LOG.md` §11（WIP 收编）、§12（主链路闭环）、§13（体验缺陷修复轮 + 归档）。