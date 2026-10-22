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
| **P1-AUTH-CHAIN-001**（**本轮收口：A+B 完成，C 待启动**） | **N30 已修**（A 卡 `RoleCodeNormalizer` 剥前缀 + 三处比较面归一，15 单测）；**N31 已修**（B 卡四片 239 处幻码 + Flyway 补 286 码入目录到 418 + admin 全局兜底 `AdminPermissionCatalog` + 补建 4 角色 + 前端字典）；活体已验：admin 5 个 API 全 200；**C 卡（废弃 `users.roles` 鉴权、彻底单向化）未启动** | 下轮开 C 卡；另 `getUserRoleNames` 仍返回中文名（N28），前端靠字典兼容 |
| **P1-DASHBOARD-FILTER-001**（**新增，P1 越权风险**） | 卡面已立（§24.18a）：`DashboardController` 全端点仅校验 `operations:dashboard:view`，**无门店级数据过滤**；本轮以「撤销 13 个业务角色误授 + 前端按权限隐藏卡片」规避，根因未修 | Owner 放行后实施：4 端点接数据权限过滤 + 修 `DashboardServiceImpl:114` 的 500 + 完成后按角色重新授权 |
| **P1-APK-DEVICE-001** | 卡号/优先级已确认（Owner 2026-10-13）：**P1 / 整特性一次入库**；三项 `.gitignore` 已执行（`48750ce` + `c13cb2a`）；5 个生成模板文件已排除（§24.3y），候选 **59** | Owner 放行后整特性入库，含 4 项卡面待补：guards.ts APK 四块 / request.ts X-Device-Id + FormData / index.ts 三条路由 / SecurityConfig 3 条 permitAll 与 `meta.requireAuth` 口径核对 |
| **§24.3s（N15 order:add）** | 实施 CLOSED；应急补桥已 Live 通过（`user_roles` id=2329，`/me` 返回 12 码） | **已收口**（原「被 P1-AUTH-CHAIN-001 阻塞」已解除） |
| **P1-SEC-009** | QA 渗透验证未关闭（**发布门禁前必须关闭**）；执行窗口已裁决（§24.3x）：**排在 P1-APK-DEVICE-001 入库后一起跑** | 覆盖 device-whitelist / verify-code / `usesCleartextTraffic="true"`；Owner 安排窗口 |
| **P0-SEC-001** | 第二阶段 71 Controller 审计，**暂缓**（Owner 2026-10-12） | Owner 放行后恢复 |
| **P0-WORKSPACE-WIP-CONSOLIDATION-001** | **暂缓**（GOV-003 分类表为其输入） | Owner 裁决五批入库 |

## 3. 挂起项（只列 13）

| 项 | 类型 | 说明 |
|----|------|------|
| **TRAY-005** 托盘门店归属管理缺失 | 新增（2026-10-23，待裁决） | ① 库内 2 个 `store_id=NULL` 托盘永久不可用（KDS 被门店校验拒绝、管理端无分配入口）→ 建议加「托盘分配门店」功能，**不建议**"KDS 首次绑定自动认领"（静默改写归属）② 若连锁门店间会调拨托盘，硬隔离不可行 → 建议独立「托盘调拨」业务动作（换店 + 审计），而非直接改 `store_id` |
| **TRAY-006** 手动流转轨生产收敛 | 新增（2026-10-23，待裁决） | 新增手动轨 `POST /v1/tray/manual-transition` 当前仅需 `kitchen:edit`。生产建议加开关 `app.tray.manual-override-enabled`（dev 开 / prod 关），或收敛到 admin。已内置：理由必填、写 `tray_scan_record(scan_type=MANUAL)`、跃迁白名单、**禁止手动置 served**（承载扣料+追溯码+成本） |
| **DATA-006** 历史 0 元财务流水 | 新增（2026-10-23，待执行） | `finance_records` 有 10 条 `amount=0` 的「订单收入」流水（`SZ202609170001`/`SZ202609210001`/`SZ202609250002`/`SZ202609260002`/`SZ202609300002`/`SZ202609300005`/`SZ202609300008`/`SZ202609300011`/`SZ202610230005`/`SZ202610230007`），均产生于事件金额修复前。建议软删（`deleted=1`）避免污染报表 |
| **CODE-007** 死代码 `recordOrderCost` | 新增（2026-10-23） | `OrderCompletedEventListener.recordOrderCost` 依赖恒为 null 的 `event.materialCost`（成本由 lifecycle 同步写 `cost_records` 以避免重复入账）。建议下轮删除，防止将来有人填 `materialCost` 造成成本重复 |
| **TECH-DEBT-001** 单/复数双表清理 | 新增（2026-10-23） | 库内并存 `cost_record`(空) / `cost_records`(实体实际用)、`tray` / `trays`、`dish_recipe` / `dish_recipes`、`order_material_requirement` 等；`dining_table` 实为**视图**而非表。需逐一核对实体 `@TableName` 与真实对象，清理遗留空表，避免再次"查对表名却查空" |
| **ARCH-EVENT-002** 事件 outbox | 新增（2026-10-23） | `OrderCompletedEvent` 现已在事务内发布（AFTER_COMMIT 能触发），但仍是"发出去不管"：进程崩溃/异步失败即丢事件（财务收入、应收、成本联动丢失且无痕）。建议加 `event_outbox` 表：事务内落库 + 异步投递 + 重试 |
| **OPS-BOM-003** BOM 缺失可视化 | 新增（2026-10-23） | 已裁决「无 BOM 硬拒出餐」（P0 严格版）。现仅抛 `FOOD_NO_RECIPE` 错误，缺运维入口：应提供"缺配方菜品清单"报表/告警，让厨师长主动补 BOM，而不是等出餐失败才发现 |
| **DATA-004** 桌台基线数据为空 | 新增（2026-10-23） | `dining_tables` 当前 0 行；锁桌/解锁语义已修复（列类型 varchar(50)，含依赖视图 `dining_table` 重建）并经 SQL 事务验证，但无真实桌台可做端到端验证。需先导入门店桌台配置，或走桌台管理 API 建桌后再复验 |
| Live 验证批次 A/B/C + N15 Live | 等 Owner 执行 | Agent 不跑 Live 验证 |
| **ENV-14** `flyway_schema_history` 卫生异常（20260922.001 checksum=NULL + 缺 V999） | 已裁决（2026-10-13） | 无阻断（Flyway 容忍）；**不 repair**；**顺手修，不另开卡**——等下一个碰 `flyway_schema_history` 的卡（如未来某次 migration 卡）一并处理 |
| **P1-SEC-002** | **部分收口（验收依赖 P1-APK-DEVICE-001）** | 卡面已提交（`eec8fb9`），但 `/verify-code`、`/device-activation` 路由条目属该卡簇 → 提交面无该路由，验收标准（未登录不重定向）不可执行；Live 验证须待该卡入库 |
| 被忽略 `*.md`（非 production 面）——**实测 13 个**（不是 15） | 等 Owner 逐条裁 | 8 清（release-* / sprint-* / 方案 / 里程碑）+ 2 留（debug-*）+ 5 待裁（oic-1 / oic-2 / product-decision-* / frontend-improvement-backlog；同族不一致：`oic-be-task-board.md` 已 tracked）；4 条通配无对应文件为误伤根源。逐条清单已登记 KL 追加 |
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

