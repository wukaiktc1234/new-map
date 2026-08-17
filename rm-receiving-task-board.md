# RM 任务池（手持收货域专项轨 — Receiving Mobile）

> **阶段定位**：多客户端基线审计第 5 项（2026-08-15，`docs/quality/client-audit-receiving-mobile.md` + `multi-client-audit-summary.md` §3.5/§四 建议 E 级）→ RM 独立轨 ACTIVE（架构裁决 2026-08-15，`remediation-roadmap.md` §5.5 D-2/D-3/D-4）。
> **定位**：RM =「手持专项轨」，覆盖 `frontend/` 内**手持收货域**——Capacitor 壳（`capacitor.config.ts` + `frontend/android/`）+ `ReceivingMobile.vue` + `receipt-confirmation.ts` + APK 分发脚本（`scripts/apk-server.cjs`）；**与 OIC-1 管理端批次隔离**（不进 OIC-1 任务池、不进 OIC-2 多客户端轨）；独立编号 `RM-*`。
> **治理输入**：`docs/quality/client-audit-receiving-mobile.md`（前端 27 项 P0×3 / P1×12 / P2×12 + 后端依赖 8 项 RM-BE-001~008）+ 全仓实体-表映射扫描（ETM-106/039/041 关联，`docs/quality/entity-table-mapping-audit.md`）。
> **维护者**：planner（会话3）。developer 完成后将修改证据回写本文件对应卡；qa 验收结论输出至 `docs/quality/`。
> **后端联动**：RM-BE-*（后端依赖 8 项）归**后端整改池**，随 **ETM-106 专项（RM-B1-001）** 与 KL-054/055/ETM 同批窗口治理（Entity-Table Consistency Remediation 家族，`production-known-limitations.md`）。**RM-BE-007 已由架构裁决（2026-08-15）确认为权限面问题（非业务语义）→ 从 PD 候选语义中移除，不涉及 PD 决策，直接按 RM 后端池 P1 整改登记（见 §4.2）**。
> **批次节奏（D-4）**：每批 2~3 卡：planner 放卡 → developer → qa（PASS/FAIL）→ 部署 → observe；后端专项（D-2）执行窗口：**管理端 Batch 2 observe 收口（2026-08-16 12:55）后开工**。

---

## 〇、RM 红线（全程有效，每张卡必读）

| # | 红线 |
|---|---|
| R-1 | **后端观察窗口约束（D-2，最高优先级）**：**2026-08-16 12:55（管理端 Batch 2 observe 收口）前不动后端**——后端专项（ETM-106）窗口内**仅出 migration 方案、不落库、不改码**；窗口后按 D-2 开工，分批迁移+验收 |
| R-2 | **不改变业务规则**；业务语义类决策前不实现：**PD-012（自动签名存证，RM-DATA-001/002）/ PD-013（APK 分发模式，RM-CAP-004/005）待评估**——相关卡标记 BLOCKED/待决策，不写实现方案（BLOCKED 不纳入批次通过数）；与规则无关部分（失败提示/禁用入口）允许先行 |
| R-3 | **不创建 Sprint 编号、不进 Release Gate**；RM 为发布后独立治理编排（同 OIC 轨道口径） |
| R-4 | **开发中不得顺手扩大范围**：发现同源问题只登记本文件「后续候选占位区」，不得顺手修改 |
| R-5 | 已封存治理文件、OIC-1 管理端批次、OIC-2 多客户端轨**不触碰** |
| R-6 | 标准流程强制：developer 完成 → qa 独立验收（`docs/quality/`）→ 部署 → observe；开发不得自验，QA 不改代码 |

## 一、批次状态

| 批次 | 范围 | 状态 | 说明 |
|---|---|---|---|
| **Batch 1（3 卡，2026-08-15 排卡）** | **RM-B1-001 + RM-B1-002 + RM-B1-003** | **QA 通过（PASS×2 + PWL×1，无 FAIL），2026-08-15** | D-4 首批：ETM-106 后端整改启动（窗口内先出 migration 方案，**不落库**）/ server.url 配置化 / cleartext 关闭规划；RM-B1-002/003 为前端/壳域可先行卡（不依赖后端窗口），RM-B1-001 执行窗口 08-16 12:55 后。qa 独立验收（`docs/quality/rm-b1-qa-report.md`）：001 PASS（含架构评审通过）/ 002 **PASS_WITH_LIMITATION**（L-1 构建门禁由发布负责人 08-16 12:55 收口后执行并回填，不豁免；L-2 untracked 治理项）/ 003 PASS；下一步：Batch 2+ 实施卡（执行窗口 08-16 12:55 后） |
| Batch 2+（后续候选） | 见 §四 占位区（RM-DATA/RM-SILENT/RM-API/RM-PERM/RM-CAP/RM-BE 剩余项）+ §三-B 实施卡（RM-B2-实施卡1/2/3 已拆占位卡） | 实施卡 1 已闭环；**实施卡 3 已放卡并开发完成（2026-08-17，待 qa 验收）** | 实施卡 1（inventory 补列落库）**QA PASS 已闭环**（2026-08-16，REG-ETM-106 转正由 regression 执行）；实施卡 2（生产实测）核心已由 **G-3 先行覆盖**（qa 责任项已执行）；**实施卡 3（idx_inventory_batch_no 索引落库 + 产品口径列收敛治理候选）2026-08-17 放卡并开发完成**（观察收口条件已满足：管理端 Batch 2 observe 收口 08-16 12:55 已过、OIC-2 Batch 2 observe 收口 08-17 18:00 已完成、RM 轨无进行中 observe 检查点；与 OIC-BE 阶段二批次并行放卡，详见实施卡 3 状态字段）；同源只登记、不顺手扩展（红线 R-4） |

## 二、流程（每批强制，developer → qa → deploy → observe）

1. **planner** 排卡（编号 `RM-B{n}-xxx`；验收标准写入任务卡；PD 待评估项标注 BLOCKED/待决策）
2. **developer** 按卡实现（前端/壳域：`frontend/` 手持域文件；后端专项卡：仅方案文档，窗口后落库拆后续卡），提交修改证据：**文件 diff + 构建 EXIT=0 + 自测说明**（回写本文件对应卡）
3. **qa** 独立验收（功能/边界/回归面），输出 PASS / FAIL / PASS_WITH_LIMITATION 至 `docs/quality/`；FAIL 时生成 `-R{n}` 返回开发
4. **部署**：验收通过后由发布负责人执行（APK 构建/分发或静态服务；后端不动、DB 不动——窗口内严禁后端/DB 变更）
5. **observe**：部署后观察（行为对齐、无回归反馈）；不回写既有回归基线、不进 Release Gate；验收中发现与基线相关行为，以 regression 意见为准；后端专项完成后随 regression → Release Gate 链路评估

---

## 三、Batch 1 任务卡（2026-08-15 排卡）

---

### 任务卡 1/3：RM-B1-001

| 字段 | 内容 |
|---|---|
| **编号** | `RM-B1-001`（QA FAIL 复验时为 `RM-B1-001-R{n}`） |
| **优先级** | **P0**（ETM-106 为实体-表映射扫描 C 类唯一 P0；多客户端审计 RM-BE-001） |
| **来源审计项** | **RM-BE-001**（`docs/quality/client-audit-receiving-mobile.md` L18/L59）+ **ETM-106**（`docs/quality/entity-table-mapping-audit.md` L166，C 类唯一 P0）+ 架构裁决 D-2（`remediation-roadmap.md` §5.5：Entity-Table Consistency Remediation 专项） |
| **文件** | `backend/.../service/impl/ReceiptConfirmationServiceImpl.java:706-716`（WAREHOUSE 分支）+ `InventoryServiceImpl.java:196-271` + `entity/Inventory.java:28-165` + `db/migration/V1.0.0.100__init_postgresql.sql:425-444` + `V20260730_001__add_chain_linkage_columns.sql:48-54` |
| **现状** | WAREHOUSE（仓库）收货确认 → `increaseInventory` 以 Inventory 实体全字段写 `inventory` 表：实体/XML 为物料口径 20 列（material_name/specification/batch_no/locked_quantity/unit_cost/total_cost 等），表为产品口径 18 列（仅 V20260730_001 补 6 列）→ 新建库存 INSERT 必含 locked_quantity（恒 ZERO）+ batch_no（手持端必传）+ unit_cost + total_cost；UPDATE 含 batch_no/unit_cost/total_cost → **列不存在 → SQL 异常 → @Transactional 整体回滚 → 500 → 仓库手持收货确认全线失败**（门店 STORE 分支走 store_inventory 表已对齐，幸免） |
| **风险** | 手持端「仓库入库」场景不可用（库存写入断流）；Inventory 被 17 个文件引用（核心库存链路）——数据迁移风险高，方案决策权归 developer 口径确认 + 架构裁决，禁止 auditor/planner 决策 |
| **修复目标** | **Entity-Table Consistency Remediation 专项首卡**：产出 inventory 表对齐 migration 方案（补列 vs 实体对齐三选一口径）+ 表结构核对单（实体 ↔ migration ↔ 真实表三向核对，协议规则 6 门禁）——**窗口内（2026-08-16 12:55 前）只出方案不落库**；执行窗口 08-16 12:55 后开工，分批迁移+验收（不全仓一次性改） |
| **执行方式** | 后端专项（D-2）；本卡 = **方案卡**：developer 产出 migration 方案文档 + 表结构核对单（information_schema 只读核对）；**不包含实际落库/代码修改**（migration 文件零新增/零修改、后端代码零改动）；方案经架构评审通过后，落库/改码拆 Batch 2+ 实施卡 |
| **验收标准（qa 独立验收用，可验证、可断言）** | ① migration 方案文档产出（补列 vs 实体对齐的口径选择、逐列差异清单、风险与回滚预案）并经**架构评审通过**；② **表结构核对单**：Inventory 实体列 ↔ migration 声明 ↔ 真实表（information_schema 只读）三向差异完整列出（含 batch_no/locked_quantity/unit_cost/total_cost/material_name/specification 等漂移列）；③ **明确不落库**：git diff 无 backend 代码与 migration 文件改动（本卡产物仅方案/文档）；④ **执行窗口合规**：08-16 12:55 前仅方案、不执行任何落库动作；⑤ 方案注明分批迁移+验收计划（不全仓一次性改）；⑥ 验收结论注明：本卡不含实际落库，落库实施拆 Batch 2+ 卡 |
| **红线** | 窗口内不动后端（R-1：不落库、不改码）；不猜测业务规则；方案决策权=developer 口径确认 + 架构裁决；不创建 Sprint 编号、不进 Release Gate |
| **提交证据要求（developer）** | 方案文档路径 + 核对单 + 架构评审结论 + git diff 空（backend/migration 零改动）证明 |
| **不扩展声明** | 发现其它表漂移（ETM-039/041 等 RM-BE-002/003）**只登记 §四 后续候选区**，不得在本卡内一并处置（分批迁移） |
| **状态** | **QA PASS + 架构评审通过（2026-08-15，`docs/quality/rm-b1-qa-report.md` §一；方案文档 `docs/quality/rm-b1-001-inventory-alignment-plan.md` §十 事项 1~7 全数批准）**——验收标准①~⑥全过，无 FAIL、不生成 -R1；落库/改码按架构 §十 事项 7 拆 Batch 2+ 实施卡（§三-B） |
| | **产出**：① 方案文档 `docs/quality/rm-b1-001-inventory-alignment-plan.md`（口径选择+逐列差异清单+风险与回滚预案+分批迁移/验收计划）；② 三向核对单（文档 §三） |
| | **三向核对结论（information_schema 只读实测，本地 PG18，2026-08-15）**：实体 24 映射列 ↔ migration 链最终态 24 列 ↔ 真实表 35 列。**漂移 11 列**：material_name/specification/location_id/locked_quantity/batch_no/production_date/expiry_date/unit_cost/total_cost/min_safe_qty/max_stock_qty = **真实表已手工补列、migration 文件零声明**（方向修正：审计原「实体有表无」实为「表有、migration 无」→ 新环境按 migration 重建即缺列 → 仓库收货 500）；时间列 create_time/update_time 三向一致（V20260717_020 已重命名，非漂移）；产品口径 11 列表有实体未映射（保留不删） |
| | **口径**：推荐**方案 A 补列（表对齐实体，ADD COLUMN IF NOT EXISTS ×11，类型/默认值以真实表实测为准）**；方案 B（实体对齐表）/C（新表重建）否决（理由：业务必存数据不可丢/改动面/先例/可逆性，文档 §二）。**待架构评审事项**：口径确认、列类型默认值、batch_no 索引、产品口径列保留策略（文档 §七） |
| | **窗口合规**：零 DDL/DML（仅 information_schema/pg_constraint/flyway_schema_history/count 只读 SELECT）；**git diff 空**（migration 目录/Inventory 实体/mapper/InventoryServiceImpl/InventoryIncreaseDTO 零改动；ReceiptConfirmationServiceImpl.java 的 M 为会前既有未提交改动，本卡未触碰，见 §五 OBS-3） |
| | **BLOCKED 确认**：无 BLOCKED_PRODUCT_RULE（补列为 schema 事实对齐，locked_quantity 语义已有 lockInventory 实现，非新规则） |
| | **落库/改码拆 Batch 2+ 实施卡**（文档 §六 A/B/C 三卡，不全仓一次性改） |

---

### 任务卡 2/3：RM-B1-002

| 字段 | 内容 |
|---|---|
| **编号** | `RM-B1-002`（QA FAIL 复验时为 `RM-B1-002-R{n}`） |
| **优先级** | **P0**（沿用审计级别 RM-CAP-001） |
| **来源审计项** | **RM-CAP-001**（server.url 硬编码，`docs/quality/client-audit-receiving-mobile.md` L19/L76） |
| **文件** | `frontend/capacitor.config.ts`（L25-29）+ `frontend/android/.../MainActivity.java`（L660-663，错误视图文案）+ 构建/打包配置（server.url 注入点） |
| **现状** | `server.url: 'http://192.168.0.106:3005'` 硬编码局域网 IP；错误视图文案「请检查网络连接（需与系统在同一局域网）」，重试仅 loadUrl 同地址——换网络段/后端换机器 → APK 冷启动加载失败、错误视图无法恢复（除非重新构建 APK） |
| **风险** | 生产 APK 无可用性保障，发布即绑定单台开发机（RM-CAP-001） |
| **修复目标** | 服务器地址**外置化**（构建参数注入/环境配置），去除硬编码 IP；APK 内置可配置地址（多候选或域名方案由 developer 按简单优先并注明） |
| **执行方式** | 仅 `frontend/` 手持域改动（capacitor 配置 + android 构建配置）；server.url 由**构建参数注入**（构建命令可传参，产物含注入值，无硬编码 IP）；错误视图文案对齐可配置地址语义；**不涉及签名/分发改造**（PD-013 决策前不触碰 RM-CAP-004/005 范围） |
| **验收标准（qa 独立验收用，可验证、可断言）** | ① grep `192.168.0.106` 0 命中（如保留默认示例值须显式标注占位不可用，qa 以 grep 断言）；② server.url 由构建参数/环境注入：以不同参数构建两次，产物含对应注入值（可断言产物差异）；③ 错误视图文案不再宣称绑定开发机语义（如「需与系统在同一局域网」口径对齐为可配置地址提示）；④ APK 构建 EXIT=0；⑤ 无后端/DB 改动；⑥ 验收结论注明：PD-013（APK 分发模式）决策前，本卡不涉及签名/release 分发改造 |
| **红线** | 仅 `frontend/` 手持域改动；不改后端/DB；PD-013 决策前不涉及签名/分发；不创建 Sprint 编号、不进 Release Gate；开发中不得顺手扩大范围 |
| **提交证据要求（developer）** | 文件 diff + APK/构建 EXIT=0 + 自测说明（双参数构建产物对比、错误视图文案验证） |
| **不扩展声明** | 开发中发现同源问题（如 apk-server.cjs 代理 RM-CAP-003、版本双源 RM-DATA-004 等）**只登记 §四 后续候选区，不得顺手修改** |
| **状态** | **QA PASS_WITH_LIMITATION（2026-08-15，`docs/quality/rm-b1-qa-report.md` §二）**——功能/数据/边界验收项全过（QA 独立四态断言 + tsc 隔离检查），无 FAIL、不生成 -R1。**两条限制登记（不豁免）**： |
| | **L-1 部署门禁**：构建 EXIT=0 + 双参数产物 diff 断言按构建约束未执行——**2026-08-16 12:55 收口后由发布负责人执行并回填**：两次 `cap sync android`（注入 A/B）产物 capacitor.config.json server.url 字段 diff + assembleDebug EXIT=0；本放行不豁免该门禁 |
| | **L-1 门禁回填（2026-08-16 13:54，developer 执行）——已闭环**：① 构建 EXIT=0：注入 A `http://192.168.1.100:3005` → `npm run build` **EXIT=0**（built in 5.09s）→ `cap sync android` **EXIT=0**；注入 B `http://192.168.1.200:3005` → `npm run build` **EXIT=0**（4.80s）→ `cap sync android` **EXIT=0**；`gradlew assembleDebug` **EXIT=0**（BUILD SUCCESSFUL in 2s，217 tasks，含 13:22 APK 增量复核）；② 双参数产物 diff 断言 **PASS**：A 产物 capacitor.config.json `server.url=http://192.168.1.100:3005`，B 产物 `=http://192.168.1.200:3005`，**A≠B 且除 server.url 外 357 字段级完全一致**（存档 `Temp\opencode\rm-b1-002-capjson-A/B.json`）——resolveServerUrl 读 CAP_SERVER_URL 注入真实生效（QA 四态断言 + 真实 sync 产物双重实证）；③ 现场核验：app-debug.apk 13:22:27 存在（13,426,739 B，上次构建成功属实）；capacitor.config.json 当前=B 值（与 13:22 APK 一致）；MainActivity.java L661 文案正确（grep「同一局域网」0 命中）；④ **:3003 服务保护确认**：dist 于 13:21 被本门禁构建覆盖——核对结论：**13:21 构建产物与 08-15 部署基线（OIC-1 Batch 2 产物，12:08/12:50，357 文件/8,534,757 B，`dist-backup-20260815-batch2-before.zip`）357/357 SHA256 完全一致**（覆盖=内容幂等，无实质差异）；构建前/后 :3003（PID 5940）HTTP 200、/api 代理 UP，无需恢复基线；⑤ 异常如实报告：无 FAIL、无 BLOCKED；L-2（untracked）仍待发布负责人/架构治理决策（本会话未擅改 git 跟踪状态）；本次未修改任何源码（壳三文件保持 QA 验收版本），无 git 提交产生 |
| | **L-2 untracked 治理项**：`capacitor.config.ts` / `build-apk.bat` / `frontend/android/`（含 MainActivity.java）**未纳入 git 跟踪**——改动面无法 git 审计、回滚基线不可追溯；发布基线确认以内容核验为准，登记 §五 治理项 |
| | **修改文件（3）**：① `frontend/capacitor.config.ts`：server.url 改 `resolveServerUrl()`——构建参数 `CAP_SERVER_URL` 环境变量注入（`npx cap sync android` 时 @capacitor/cli 以 Node 加载 config，注入值写入 android assets 的 capacitor.config.json）；未注入/格式非法时回退**显式标注「不可用」的占位默认值** `http://127.0.0.1:3005` + console.warn（fail-safe，不产出假可用地址）；**cleartext/allowMixedContent 原样保留**（与 RM-B1-003 隔离）；② `frontend/build-apk.bat`：移除硬编码 IP 与 PowerShell in-place 改写 config 的 hack（旧 3004/3005 端口不一致随之消除）；地址由环境变量 CAP_SERVER_URL 或第一参数注入，**未注入则报错退出**（不产出地址不可用 APK）；③ `frontend/android/.../MainActivity.java` L661：错误视图文案改「请检查网络连接，或确认服务器地址配置正确」，不再宣称「需与系统在同一局域网」 |
| | **验证证据（构建约束：未执行构建/双构建，以逻辑验证替代并说明）**：① grep `192.168.0.106` 手持域（capacitor.config.ts / build-apk.bat / frontend/android/）**0 命中**；② 注入逻辑 node 断言：注入 A `http://192.168.1.50:3005` 与注入 B `https://rm.example.com:443` → 产物值不同（A≠B 可断言差异），未注入/非法格式 → 回退占位；③ tsc --noEmit 隔离检查 **exit 0**（本卡改动类型干净；既有 `android.androidScheme`（原 L32）为 Capacitor 8 无效键报错，非本卡引入，登记 §五 OBS-4）；④ 双构建产物差异断言留待部署阶段（两次 cap sync 产物 capacitor.config.json server.url 字段 diff） |
| | **范围声明**：无后端/DB 改动；PD-013 决策前未涉及签名/release 分发改造；未执行任何构建/部署（frontend/dist 与 :3003 静态服务零影响） |
| | **同源登记**：frontend 非手持域残留 `192.168.0.106`（.env.development:3003/docs 运维文档/gen-cert.mjs/h5-server.cjs 注释/setup-firewall.bat/HandheldEntryDialog.vue 提示文本）→ §五 RM-B1-002-OBS-2，grep 验收范围请 qa 按手持域界定 |

---

### 任务卡 3/3：RM-B1-003

| 字段 | 内容 |
|---|---|
| **编号** | `RM-B1-003`（QA FAIL 复验时为 `RM-B1-003-R{n}`） |
| **优先级** | **P0**（沿用审计级别 RM-CAP-002） |
| **来源审计项** | **RM-CAP-002**（cleartext 三重明文放行，`docs/quality/client-audit-receiving-mobile.md` L20/L77） |
| **文件** | `frontend/capacitor.config.ts`（L27, L33）+ `frontend/android/app/src/main/AndroidManifest.xml`（L10）——**本卡为规划卡，不修改上述文件** |
| **现状** | `cleartext: true` + `allowMixedContent: true` + `android:usesCleartextTraffic="true"` 三重明文放行，全部流量走 `http://`；页面、JWT token、签名图像（base64）、留证照片、业务数据（收货数量/单价/应付金额）均**明文局域网传输**，可被同网段嗅探/中间人篡改 |
| **风险** | 凭证泄露与业务数据篡改（收货确认=库存+应付资金链路）——**生产接入前必须关闭** |
| **修复目标** | **规划卡**：前置条件=真实 HTTPS 环境（证书/服务端 https 化），生产接入前必须关闭 cleartext；输出关闭规划文档（前置条件清单 + 关闭步骤 + 验证方法）；**不强制立即关闭**（当前无 HTTPS 环境，立即关闭会导致 APK 全断） |
| **执行方式** | 本卡**仅产出规划文档**（存放位置由 developer 执行时确定并回写本卡）：cleartext 关闭前置条件清单（HTTPS 环境就绪、证书方案、server.url https 化——与 RM-B1-002 联动）、关闭步骤（移除三重放行、https 地址接线）、验证方法（QA 可执行：HTTPS 环境就绪后配置检查 + 抓包断言无明文）；实施拆 Batch 2+ 卡（前置条件达成后） |
| **验收标准（qa 独立验收用，可验证、可断言）** | ① 规划文档产出：**前置条件清单**完整（真实 HTTPS 环境、证书、server.url https 化、与 RM-B1-002 联动关系）；② 关闭步骤与验证方法文档化（QA 可执行：配置检查断言 + 抓包断言无明文流量）；③ **本卡不修改配置**：git diff 无 capacitor.config.ts / AndroidManifest.xml 改动；④ 明确「不强制立即关闭」口径与理由（无 HTTPS 环境时立即关闭将导致 APK 全断）；⑤ 无后端/DB 改动 |
| **红线** | 本卡仅规划文档，不实施配置修改；不强制立即关闭；不改后端/DB；不创建 Sprint 编号、不进 Release Gate |
| **提交证据要求（developer）** | 规划文档路径 + git diff 空（配置零改动）证明 |
| **不扩展声明** | 实施（移除 cleartext/allowMixedContent）拆 Batch 2+ 实施卡（前置条件达成后）；开发中发现同源问题只登记 §四 后续候选区 |
| **状态** | **QA PASS（2026-08-15，`docs/quality/rm-b1-qa-report.md` §三）**——验收标准①~⑤全过（规划文档 P-1~P-6 完整、关闭步骤与验证方法 QA 可执行、配置零改动 QA 读文件实证、「不强制立即关闭」口径与理由充分），无 FAIL、不生成 -R1；实施拆 Batch 2+ 卡（前置条件达成后） |
| | **产出**：规划文档 `docs/quality/rm-b1-003-cleartext-close-plan.md` —— ① 前置条件清单 P-1~P-6（真实 HTTPS 环境、证书方案[正式证书 vs 自签+pinning，架构裁决]、server.url https 化（与 RM-B1-002 构建参数注入联动：HTTPS 就绪仅需注入 https 地址）、后端 API 传输面一致、错误视图兼容、QA 抓包环境）；② 关闭步骤（移除三重放行 3 处 + https 地址接线 + network_security_config 可选过渡白名单）；③ QA 验证方法 V-1~V-5（配置 grep 断言 + 抓包断言无明文流量 + 功能回归 + 反向断言）；④ 「不强制立即关闭」口径与理由：APK 远程加载模式无 HTTPS 环境立即关闭 → 冷启动即全断且不可热恢复；⑤ 实施拆 Batch 2+ 三卡 |
| | **零改动声明**：`capacitor.config.ts` / `AndroidManifest.xml` 未修改（git diff 空）；RM-B1-002 对 capacitor.config.ts 的改动仅 server.url 注入化，未触碰 cleartext/allowMixedContent（两卡改动面正交）；无后端/DB 改动 |

---

## 三-B、Batch 2+ 实施卡（占位卡，TODO——架构评审 §十 事项 7 已批准口径，2026-08-15 拆卡）

> **来源**：RM-B1-001 方案文档 §六 A/B/C 分批迁移计划（`docs/quality/rm-b1-001-inventory-alignment-plan.md`）+ §十 架构评审（事项 1/2/3 批准、事项 4 产品列保留、事项 6 前置门禁、事项 7 拆卡）+ qa 独立实测新发现（OBS-4/5/6 手工索引漂移，`rm-b1-qa-report.md` §1.4）。
> **执行窗口**：**2026-08-16 12:55（管理端 Batch 2 observe 收口）后**，随后端专项（D-2/ETM-106）窗口开工（红线 R-1：窗口内不动后端）。
> **批约束（D-4）**：每批 2~3 卡——**实施卡 1/2 首批**（落库+实测；实施卡 1 已 QA PASS 闭环 2026-08-16，实施卡 2 核心由 G-3 先行覆盖），**实施卡 3 下一批排期（2026-08-16）——与 OIC-BE 阶段二批次并行候选（2026-08-17：状态更新为「待放卡」，观察收口条件已满足，见实施卡 3 状态字段）**。
> **全部实施卡前置门禁（架构 §十 事项 6）**：开工前必须确认 `ReceiptConfirmationServiceImpl.java` 工作区未提交改动（+396/-12，OBS-3）的**基线归属**（发布负责人确认：提交/暂存/与发布基线关系）；**未确认前不得改码**。

### 实施卡 1：RM-B2-实施卡1（inventory 补列 migration 落库 + 端到端冒烟）

| 字段 | 内容 |
|---|---|
| **编号** | `RM-B2-实施卡1`（来源：方案 §六 A + §四 脚本 + §十 事项 1/2/7） |
| **优先级** | **P0**（ETM-106 补列，仓库收货 500 根因） |
| **文件** | 新增 `backend/src/main/resources/db/migration/V20260816_001__align_inventory_material_columns.sql`（§四 建议脚本：`ADD COLUMN IF NOT EXISTS` ×11 + COMMENT，类型/默认值以 information_schema 实测为准）；关联 `ReceiptConfirmationServiceImpl.java` / `InventoryServiceImpl.java` / `Inventory.java`（改动与否以落库后冒烟结论为准） |
| **问题** | inventory 表 11 物料口径列 migration 零声明（OBS-1）——新环境 Flyway 全量重建即缺列 → 仓库 WAREHOUSE 收货确认 INSERT/UPDATE 必失败 → 500 回滚 |
| **风险** | 生产库表结构 ≠ dev 实测（类型漂移残留）；加列锁表；Flyway 版本冲突——缓解：落库前先跑核对单只读实测、additive 可逆、按命名规范递增 + out-of-order 兜底 |
| **修复目标** | 方案 A 补列 migration 落库（幂等，真实表 no-op）→ dev 库 Flyway 执行 → 仓库 WAREHOUSE 收货确认端到端冒烟 |
| **执行方式** | 后端专项（D-2），08-16 12:55 后开工；**前置门禁**：OBS-3 基线归属确认后方可改码（§十 事项 6）；先 dev 落库 → 冒烟（WAREHOUSE 收货确认 + STORE 分支回归 + selectInventoryPage 回归 + inventory_transactions 写入核对）；不全仓一次性改（分批） |
| **验收标准（qa 独立验收用）** | ① information_schema 断言 11 列存在且类型匹配（实体 ↔ migration ↔ 真实表三向核对，协议规则 6）；② 冒烟 EXIT=0（仓库收货确认全链路成功）；③ STORE 分支无回归；④ migration 文件随 Flyway 版本链正确（无冲突）；⑤ 风险/回滚预案可执行 |
| **状态** | **QA PASS（2026-08-16，`docs/quality/rm-b2-impl1-qa-report.md`）**——验收标准①~⑤全过（qa 独立执行：psql 只读断言 + Flyway checksum 独立计算 -1564733106 与库内一致 + HTTP 实测 + git 核验），无 FAIL、不生成 -R1；**G-3 生产独立实测：已执行（通过）**（readiness §4.2 G-3，qa 责任项：information_schema 11 列断言 + OBS-4/5/6 索引核对 + 数据抽查，0 DDL/DML）；无 BLOCKED_PRODUCT_RULE（补列为 schema 事实对齐，locked_quantity 语义已有 lockInventory 实现）；**REV 标注**：本卡为 RM-B2 轨后端改动（V20260816_001 落库）——**回归基线转正由 regression 执行**（候选条目：新环境 Flyway 全量重建后 11 列存在 / WAREHOUSE+STORE 3 单链路 HTTP 200 / selectInventoryPage+batchNo 筛选无 500，见 qa 报告 §六 REG-候选A/B/C） |
| | **① migration 文件（diff 摘要）**：新增 `backend/src/main/resources/db/migration/V20260816_001__align_inventory_material_columns.sql`（33 行）——`ALTER TABLE inventory ADD COLUMN IF NOT EXISTS ×11`（material_name varchar(200)/specification varchar(200)/location_id bigint/locked_quantity numeric(12,2) DEFAULT 0/batch_no varchar(100)/production_date date/expiry_date date/unit_cost bigint/total_cost bigint/min_safe_qty numeric(12,2)/max_stock_qty numeric(12,2)）+ `COMMENT ON COLUMN ×11`（与方案 §四 批准脚本逐字一致，类型/默认值以 readiness §2.1 实测为准）；**不含索引**（idx_inventory_batch_no 归实施卡 3）；仅新增 1 文件，其余文件零改动（ReceiptConfirmationServiceImpl.java 等未触碰） |
| | **② flyway 落库记录**：`mvn org.flywaydb:flyway-maven-plugin:9.22.3:migrate`（JDK25 显式 JAVA_HOME + filesystem 定位 migration 目录；未重启后端服务，PID 14196 自 08-14 11:04:22 未变）——落库前：**188 条 / success=188/188 / 最新 20260811.001（rank 188，checksum -1527782362）**；落库后：**189 条 / rank 189 = 20260816.001（description=align inventory material columns，success=t，checksum -1564733106，installed 2026-08-16 13:22:59）**，`Successfully applied 1 migration ... now at version v20260816.001`（BUILD SUCCESS）；11 个 ADD COLUMN 全部触发「已存在」WARNING（no-op 符合预期——真实表已手工补列）；validate 通过 189 migrations |
| | **③ 端到端冒烟（真实 HTTP :8081，admin 登录，逐项结果）**：① WAREHOUSE INSERT 链路：`POST /api/v1/receipt-confirmations`（arrival 7，material 22×100，batchNo=SMOKE-WH-20260816）→ **HTTP 200**，confirmationId=21/RC202608160001，inventory 新行 inv=35（stock=100，batch_no/unit_cost=300/total_cost=30000/locked_quantity=0 全落库）——**不再 500**；② WAREHOUSE UPDATE 路径：arrival 8（material 22×50）→ HTTP 200，confirmationId=23/RC202608160003，inv=35 更新（100→150，batch_no→SMOKE-WH-UPDATE-20260816，total_cost→45000，乐观锁 version 0→1）；③ STORE 分支回归：arrival 18（material 20×1+21×2）→ HTTP 200，confirmationId=22/RC202608160002，store_inventory 正确写入（20:+1/21:+2，unit_cost/total_cost 正确），inventory 表零污染（仅 WAREHOUSE 冒烟新增 1 行）；④ selectInventoryPage 回归：`GET /api/v1/inventory?warehouseId=1` → HTTP 200，total=9，含 inv=35 全 11 列可查（batch/unitCost/totalCost/locked）——无 500；⑤ inventory_transactions 写入核对：tx 26（RC202608160001：+100.00，before 0.00→after 100.00，unit_cost 300/total_cost 30000，reference_type=receipt_confirmation）、tx 27（RC202608160003：+50.00，100.00→150.00）——16 列全对齐 |
| | **④ 实测数据（qa G-3 复核输入）**：information_schema 断言 **11 列存在且类型匹配**（material_name/specification varchar(200)、location_id/unit_cost/total_cost bigint、locked_quantity/min_safe_qty/max_stock_qty numeric(12,2) 且 locked_quantity DEFAULT 0、batch_no varchar(100)、production_date/expiry_date date）——实体 ↔ migration ↔ 真实表三向一致（协议规则 6）；**11 条 COMMENT 已补齐**（落库前 pg_description 全空，落库后 11/11 有注释，RM-B2-RC-OBS-3 覆盖）；**数据抽查**：inventory 共 10 行（冒烟前）→ 落库后 11 行，batch_no 填充率 30%（3/10）、unit_cost 填充率 70%（7/10）、total_cost 70%、material_name 60%、locked_quantity 100%（恒 0）；**索引只读核对（OBS-4/5/6，不新增）**：idx_inventory_batch_no / idx_inventory_expiry_date / idx_inventory_location_id 三手工索引均存在（btree），与 qa 实测一致 |
| | **⑤ 提交**：`852b53f` — `feat: RM-B2-实施卡1 inventory 补列 migration 落库（V20260816_001，11 列+COMMENT，rank 189，checksum -1564733106；仓库收货确认端到端冒烟 3 单 HTTP 200，不再 500；STORE 回归/查询面/transactions 核对通过）`（1 file changed, 33 insertions；仅 1 文件精确 add，git add . 未使用；提交后 `git log` 复核 HEAD=852b53f） |
| | **⑥ 异常报告（如实登记，不掩盖）**：首次 migrate 触发 `FlywayValidateException`（库中已应用 `20260728.003/20260728.006` 本地文件缺失）——**既有事实**（应用配置 `ignore-missing-migrations: true` 下的历史状态，非本卡引入），按应用一致配置 `-Dflyway.ignoreMigrationPatterns=*:missing` 重试成功；另 Flyway 9.22.3 提示 PostgreSQL 18.3 未测试（既有告警）；`2026022701` 8 位版本号 Out of Order 显示为既有怪癖（§五-A 已登记），本次 migrate 在 `out-of-order: true` 下正常执行。**均未掩盖、无 checksum 冲突**（新 migration checksum -1564733106 与库内一致） |

### 实施卡 2：RM-B2-实施卡2（生产/测试库 information_schema 实测 + 数据抽查——**范围扩展：含索引核对**）

| 字段 | 内容 |
|---|---|
| **编号** | `RM-B2-实施卡2`（来源：方案 §六 B + qa 新发现 OBS-4/5/6，`rm-b1-qa-report.md` §1.4 建议） |
| **优先级** | **P0**（生产库落库前置核对） |
| **文件** | 生产/测试库（只读核对：information_schema.columns / pg_indexes / 数据抽查 SELECT；无 DDL/DML） |
| **问题** | 生产库表结构可能与 dev 实测不一致（手工补列不完全一致/类型不同）；真实表存在 3 个 migration 未声明的手工索引（OBS-4/5/6，与 11 列漂移同源）——落库前必须实测锁定基线 |
| **风险** | 未核对直接落库 → 补列脚本对生产非全 no-op、类型漂移残留、索引与 migration 声明不符 |
| **修复目标** | 生产/测试库 information_schema 实测（复用方案 §三 核对单）+ 数据抽查（batch_no/unit_cost 填充率、历史行成本完整性）+ **索引核对（idx_inventory_batch_no / idx_inventory_expiry_date / idx_inventory_location_id 存在性与 migration 声明差异清单——OBS-4/5/6 落实）** |
| **执行方式** | 后端专项（D-2），08-16 12:55 后开工；只读实测不落库；输出生产基线核对结论供实施卡 1 落库参考 |
| **验收标准（qa 独立验收用）** | ① 三向核对单在生产基线无新漂移（或差异全部登记）；② 索引核对清单输出（3 手工索引存在性、与架构 §十 事项 3 批准索引一致性）；③ 数据抽查通过；④ 无任何 DDL/DML |
| **状态** | **TODO（占位卡）**——执行窗口 08-16 12:55 后，首批与实施卡 1 同批（qa 提示：实施卡 B 核对范围已由 qa 扩展至索引核对，原方案 §六 B 计划范围更新，`rm-b1-qa-report.md` §1.4/§四） |

### 实施卡 3：RM-B2-实施卡3（idx_inventory_batch_no 索引落库 + 产品口径列收敛治理候选）

| 字段 | 内容 |
|---|---|
| **编号** | `RM-B2-实施卡3`（来源：方案 §六 C + §十 事项 3/4） |
| **优先级** | **P1**（索引活跃路径；产品列收敛为治理类不阻塞） |
| **文件** | migration（`CREATE INDEX IF NOT EXISTS idx_inventory_batch_no ...` 幂等覆盖真实表现状——OBS-4）；产品口径 11 列收敛策略（治理评审产出，不删列） |
| **问题** | idx_inventory_batch_no 架构已批准建（§十 事项 3，selectInventoryPage 按批次筛选活跃路径）但真实表已手工存在（OBS-4，migration 零声明）→ 落库用幂等覆盖；产品口径 11 列与物料口径并存（OBS-2），收敛策略待治理评审（§十 事项 4 批准保留不删） |
| **风险** | 索引缺失拖慢批次筛选查询；双口径并存误导后续开发（治理类） |
| **修复目标** | idx_inventory_batch_no 随 migration 落库（幂等）；产品口径列收敛治理候选（随治理评审结论推进） |
| **执行方式** | 后端专项（D-2）后续批；**下一批排期（2026-08-16，与 OIC-BE 阶段二批次并行候选）**——索引落库独立成卡（`CREATE INDEX IF NOT EXISTS` 幂等 migration），产品列收敛随治理评审结论推进；由 planner 按 2~3 卡约束排 |
| **验收标准（qa 独立验收用）** | ① pg_indexes 断言 idx_inventory_batch_no 存在且与架构批准口径一致（幂等覆盖后无重复索引）；② 产品列收敛按治理评审结论执行并验收 |
| **状态** | **开发完成（2026-08-17，developer 修改证据已回写，待 qa 独立验收）**——索引落库+治理候选登记按卡面执行完毕（详见下方证据块），由 qa 独立验收（R-6：开发不自验） |
| | **① migration 文件（diff 摘要）**：新增 `backend/src/main/resources/db/migration/V20260817_001__add_inventory_batch_no_index.sql`（9 行，含注释头）——核心语句 `CREATE INDEX IF NOT EXISTS idx_inventory_batch_no ON inventory (batch_no);` 幂等（对已手工存在索引的真实库为 no-op，对新环境为补齐）；**仅此 1 个索引**（OBS-5/6 的 expiry_date/location_id 索引不在本卡范围，只登记不处理——分批原则）；版本号 V20260817_001 避让既有（最新 V20260816.004）；不删任何列/索引；不触碰业务代码 |
| | **② flyway 落库记录**：`mvn org.flywaydb:flyway-maven-plugin:9.22.3:migrate`（backend 目录，JDK25 显式 JAVA_HOME，filesystem 定位 migration 目录；未重启后端服务）——落库前 **192 条 / success=192/192**；落库后 **193 条 / success=193/193、fail=0**；**rank 193 = 20260817.001**（description=add inventory batch no index，**checksum -916957038**，success=t，installed 2026-08-17 23:49:58）——`Successfully applied 1 migration ... now at version v20260817.001`（**BUILD SUCCESS**）；执行中触发 `关系 "idx_inventory_batch_no" 已经存在` WARNING（SQL State 42P07）——**no-op 符合预期**（真实表已手工存在该索引，OBS-4 幂等覆盖）；PostgreSQL 18.3 未测试告警为既有提示 |
| | **③ 索引断言（pg_indexes / pg_class 只读）**：`idx_inventory_batch_no` 存在（btree，`CREATE INDEX idx_inventory_batch_no ON public.inventory USING btree (batch_no)`）；**无重复索引**（batch_no 列索引 count=1，幂等覆盖成立）；`indisvalid=t / indisunique=f / indisprimary=f`——有效、非唯一、非主键，**与架构批准口径（§十 事项 3）一致**；EXPLAIN `enable_seqscan=off` 下走 **Index Scan using idx_inventory_batch_no**（Index Cond: batch_no=...）——索引生效；常规 EXPLAIN 为 Seq Scan 属优化器对小表（11 行）正常选择 |
| | **④ selectInventoryPage 按批次筛选抽查（只读 HTTP :8081）**：`GET /api/v1/inventory?batchNo=SMOKE-WH-UPDATE-20260816` → **HTTP 200**（code=0，total=1，命中 inv=35 batchNo=SMOKE-WH-UPDATE-20260816 quantity=150）——查询面无 500，不受影响 |
| | **⑤ 产品口径列收敛治理候选登记（RM-B1-001-OBS-2）**：§五 登记行已更新——**保留不删已批准**（架构 §十 事项 4），收敛策略**待架构评审**（治理类，不阻塞发布），本卡**仅登记不实施收敛**（详见 §五 更新行） |
| | **⑥ 不触碰声明**：零业务代码改动（未触碰任何 .java/mapper/前端/壳文件）；未删除任何列/索引；OBS-5/6（expiry_date/location_id 手工索引）仅登记 §五，未落库、未处理；未触碰 RM-B1-002 壳文件与其它轨文件 |
| | **⑦ 提交**：`ad55771` — `feat: RM-B2-实施卡3 idx_inventory_batch_no 索引落库 + 产品口径列收敛治理候选登记`（2 files changed, 276 insertions；migration 1 文件 + 任务板精确 add，git add . 未使用；提交后 `git log` 复核 HEAD=ad55771；migration 文件落库后内容未再改动——checksum 已固化） |

---

---
## 四、后续批次占位区（只列编号+来源+等级，不拆全卡；由架构/planner 按序放卡，每批 2~3 卡）

> 规则：以下为手持收货域审计其余发现占位登记（P0 已全数进入 Batch 1）；**不拆全卡、不开发**。放卡顺序建议（`multi-client-audit-summary.md` §五.2 建议 E 级）：前端可先行项（假成功移除/失败透传，同 OIC-1 模式）→ 后端依赖批（RM-BE-*/ETM 同批，随 RM-B1-001 专项窗口）。**PD 待评估项（RM-DATA-001/002、RM-CAP-004/005）决策前不实现（BLOCKED，不纳入批次通过数）**。

### 4.1 前端/壳域剩余项（批次 2+）

| 编号（审计） | 等级 | 说明（一句话） |
|---|---|---|
| RM-DATA-001 / RM-DATA-002 | P1 | 签名假成功展示 + canvas 程序生成签名图像（**BLOCKED：待 PD-012 自动签名存证决策，决策前不实现**；与规则无关部分如失败提示可先行） |
| RM-CAP-004 | P1 | APK 分发模式：app-debug.apk 分发 + release 无 signingConfig（**BLOCKED：待 PD-013 APK 分发模式决策，决策前不实现**） |
| RM-DATA-003 / RM-SILENT-003 | P1 | 设备身份漂移（ANDROID_ID vs fts-随机ID）+ 激活 catch 行为与注释不符（叠加 RM-BE-002/ETM-041）——冷启动激活死锁链 |
| RM-SILENT-001 / RM-SILENT-002 | P1 | 待收货/历史列表 catch 后置空 → 假空数据（失败透传 + 错误横幅 + 重试入口，同 OIC-1 FE-001/010 模式） |
| RM-PERM-001 | P1 | 路由域矩阵缺 warehouse_manager → 仓库手持收货功能不可达（守卫补角色或精确 meta.roles） |
| RM-CAP-003 | P1 | apk-server.cjs：0.0.0.0 无鉴权监听 + /api 纯转发 + API_TARGET 默认 localhost + 局域网开放 APK 下载 |
| RM-DATA-004 / RM-SILENT-004/005/006 / RM-API-001 / RM-API-002 | P2 | 版本双源 / 门店名占位回退 / OCR 故障无提示 / 供应商筛选静默消失 / DTO 契约冗余 / as never 类型掩盖 |
| RM-CAP-005 / RM-CAP-006 | P2 | DIAG_MODE 手工开关无强制门禁 / RECORD_AUDIO 等未使用权限（最小权限偏离） |

### 4.2 后端依赖剩余项（RM-BE-*，后端整改池，随 ETM 专项窗口）

| 编号（审计） | 等级 | 说明（一句话） |
|---|---|---|
| RM-BE-002 | P1 | `app_device_registrations` 无建表（ETM-041）→ APK 冷启动设备状态查询 500 → 卡激活页 |
| RM-BE-003 | P1 | `receipt_print_log` 无建表（ETM-039）→ recordPrint 500 + 防伪查验打印历史查询 500 |
| RM-BE-004 | P1 | 工作归属过滤兜底缺失（employee 未关联即不过滤 → 全量数据可见，越权面） |
| RM-BE-005 / RM-BE-006 / RM-BE-008 | P2 | quick-stock-in 无 @PreAuthorize（KL-045 关联）/ /v1/ocr 僵尸豁免 / STORE 缺 storeId 静默回退默认门店 |
| **RM-BE-007** | **P1** | 防伪码查验免登录信息面（`/v1` 防伪查验接口未登录即返回打印历史，含 operatorName/sourceIp，来源 `docs/quality/client-audit-receiving-mobile.md`）——**架构 2026-08-15 确认：权限面问题而非业务语义问题，归 RM 后端池 P1，非 PD 项**（原「PD 候选」语义已移除，直接登记整改项） |

---

## 五、开发中发现区（planner/developer/qa 登记；仅记录，不修改）

| 登记编号 | 文件 | 说明 | 处置 |
|---|---|---|---|
| RM-B1-001-OBS-1 | `inventory` 表（真实库） | 11 列（material_name/specification/location_id/locked_quantity/batch_no/production_date/expiry_date/unit_cost/total_cost/min_safe_qty/max_stock_qty）为**手工 ALTER 漂移，migration 零声明**——治理缺陷：手工改库未沉淀 migration；新环境 Flyway 全量重建即缺列 → 仓库收货 500 | auditor 复核；随 RM-B1-001 实施卡（Batch 2+）修复 |
| RM-B1-001-OBS-2 | `inventory` 表 | 产品口径 11 列（product_id/product_name/warehouse_name/store_id/store_name/safety_stock/cost_price/stock_value/warning_level/inventory_type/last_update_time）与物料口径并存、实体未映射 → 双口径治理候选 | **治理候选登记（实施卡 3，2026-08-17）**：**保留不删已批准**（架构 §十 事项 4）；**收敛策略待架构评审**（治理类不阻塞）；本卡仅登记、**不实施收敛**；后续治理卡随架构评审结论推进 |
| RM-B1-001-OBS-3 | `ReceiptConfirmationServiceImpl.java` | 工作区存在**会前未提交改动**（git diff +396/-12，签名/打印/证据功能开发中内容）——与本卡无关，但发布基线需确认该文件未完成开发流 | 登记不处置；发布负责人确认基线 |
| RM-B1-002-OBS-1 | `frontend/capacitor.config.ts` L32 | `android.androidScheme` 为**无效键**（Capacitor 8 类型定义 androidScheme 仅存在于 server 接口，android 块内被静默忽略）→ tsc 报错源；既有代码类型偏差 | Batch 2+ 清理（删除 android 块重复键） |
| RM-B1-002-OBS-2 | frontend 非手持域 | `192.168.0.106` 残留于 `.env.development`(:3003 H5 dev)、`docs/` 运维文档、`scripts/gen-cert.mjs:22`(证书 SAN 回退 IP)、`scripts/h5-server.cjs:88`(注释)、`scripts/setup-firewall.bat:53`、`HandheldEntryDialog.vue:99`(提示文本示例)——均为 :3003 Web/文档域，非 APK server.url 链（RM-CAP-001 范围=capacitor.config.ts/build-apk.bat/android/） | 登记不修（R-4 不扩大范围）；qa grep 验收按手持域界定 |
| RM-B1-002-OBS-3 | `frontend/build-apk.bat`（旧版） | 旧默认地址端口 3004 与 capacitor.config.ts 3005 不一致（正则替换依赖精确匹配 3004 URL → 旧链路实际静默失效）——已随本卡注入化消除 | 已消除，存档登记 |
| **RM-B1-001-OBS-4** | `inventory` 表（真实库，qa 独立 pg_indexes 实测，`rm-b1-qa-report.md` §1.4） | **手工索引 `idx_inventory_batch_no` 漂移**：真实表存在、migration 零声明——与 11 列漂移（OBS-1）同源（手工 ALTER 未沉淀 migration）；**与架构已批准索引（§十 事项 3）一致**——`CREATE INDEX IF NOT EXISTS` 幂等可覆盖 | auditor 复核；纳入实施卡 2 生产库核对范围（索引核对）+ 实施卡 3 落库（幂等覆盖） |
| **RM-B1-001-OBS-5** | `inventory` 表（真实库，qa 独立 pg_indexes 实测） | **手工索引 `idx_inventory_expiry_date` 漂移**：真实表存在、migration 零声明——与 11 列漂移同源；是否承载活跃业务路径待 auditor 复核 | auditor 复核；纳入实施卡 2 生产库核对范围 |
| **RM-B1-001-OBS-6** | `inventory` 表（真实库，qa 独立 pg_indexes 实测） | **手工索引 `idx_inventory_location_id` 漂移**：真实表存在、migration 零声明——与 11 列漂移同源 | auditor 复核；纳入实施卡 2 生产库核对范围 |
| **RM-B1-002-L-2**（qa LIMITATION，`rm-b1-qa-report.md` §2.3 L-2） | `frontend/capacitor.config.ts` / `frontend/build-apk.bat` / `frontend/android/`（含 `MainActivity.java`） | **手持域三文件（目录）未纳入 git 跟踪（untracked）**——qa 验收 ⑥ 无法以 git diff 审计本卡改动面，回滚基线不可追溯；发布基线确认时以内容核验替代 | 登记治理项：建议纳入版本控制（提交/跟踪决策由发布负责人/架构确认）；L-1 门禁（构建断言）不豁免、另行执行回填 |

---

## 五-A、G-1 / G-2 提交固化登记（2026-08-15 用户指令②，发布负责人决策=提交入库；developer 执行）

> **来源**：`docs/quality/rm-b2-readiness-check.md` §4.2 门禁 G-1（OBS-3 基线归属书面确认）与 G-2（8 个 untracked migration 入库决策）——用户 2026-08-15 指令②按「提交入库」决策闭环。全程红线合规：不改库（flyway_schema_history 只读 SELECT）、不动后端服务（:8081 零触碰）、不改任何代码行为；git 操作全部显式路径、提交前核对暂存区。

### G-1 闭环：ReceiptConfirmationServiceImpl.java 精确提交（OBS-3 基线归属=提交入库）

- **文件隔离确认**：提交前暂存区为空（`git diff --cached` 空、`git status` 第一列无任何条目）；仅 `git add` 单一路径 `backend/src/main/java/com/foodtraceability/service/impl/ReceiptConfirmationServiceImpl.java`，提交前 `git status --short` 复核暂存区仅该文件（1 项）。
- **提交**：`4fc4f64` — `feat: 收货确认签名存证/打印留痕/证据链/防伪查验改动固化为发布基线（G-1 OBS-3，+396/-12 已随生产上线）`（1 file changed, 396 insertions(+), 12 deletions(-)）。
- **改动类别摘要**（与 readiness §1.2 一致）：签名存证（addSignature/listSignatures）/ 打印留痕（recordPrint，上限 10 + ReceiptPrintLog + 来源 IP）/ 证据链（saveEvidence/validateEvidence + 拒收必携照片）/ 防伪查验（verifyByCode + 16 位 SecureRandom 防伪码 + arrival 一对一拦截 + enrichArrivalInfo）/ 查询增强（确认时间区间 + 供应商名过滤）/ 字段扩展（barcode/issueType/qualityCheckResult/全拒置 REJECTED）/ 状态机语义变更（到货单恒 RECEIVED + STORE 缺 storeId 回退默认门店——RM-B2-RC-OBS-2 登记提示，非本提交引入）。
- **影响范围**：纯 git 提交，零代码行为变化；生产基线由「工作树基线（依赖部署报告记录）」升级为「git commit 基线（`4fc4f64`）」，可追溯/可审计/可回滚。
- **风险**：无（已上线内容固化；git 侧基线缺位风险消除）。**G-1 门禁状态：已闭环**。

### G-2 闭环：8 个已执行 migration 审查五结论 + 精确提交

| 审查项 | 结论 | 证据 |
|---|---|---|
| **① 文件内容** | 逐文件读取完成，版本号/变更概要齐全，与 schema history 描述一致 | 建表 3：`receipt_confirmation_signatures`（V20260802_006）、`receipt_evidence`（V20260804_001）、`tax_record`（V20260811_001，含 uk_tax_record_idempotent 幂等唯一键）；改列 5：`purchase_orders` 冻结/终止 7 列（V20260802_004）、`purchase_arrivals` 质检 2 列（V20260802_005）、`receipt_confirmations.barcode`（V20260803_001）、`suppliers` 档案 11 列 + `receipt_confirmation_items.quality_check_result`（V20260807_001）、`notification` business_id/business_type（V20260810_001）；索引 6（含唯一索引 1） |
| **② 版本号连续性** | 顺序连续、无重复、无跳号；与规划 V20260816_001 无冲突 | flyway_schema_history 实测：rank 178~180（20260802.001~003）→ **181~188（20260802.004→005→006→20260803.001→20260804.001→20260807.001→20260810.001→20260811.001）** 递增连续；最新=20260811.001；V20260816_001（20260816.001）晚于其排序，可安全新增 |
| **③ checksum** | **8/8 文件与库内 checksum 完全一致**（双重验证） | ① `mvn -o org.flywaydb:flyway-maven-plugin:9.22.3:info`（JDK25 显式 JAVA_HOME，filesystem 定位同目录文件）**BUILD SUCCESS**——validate 通过、零 mismatch；② Flyway 9.22.3 官方 `ChecksumCalculator.calculate(StringResource)` 逐文件计算：-2039616926 / 810604458 / 1199710019 / -952453818 / -1600369269 / 1521818078 / -2048075820 / -1527782362 —— 与 flyway_schema_history.checksum（rank 181~188）**逐条相等** |
| **④ 生产执行状态** | 8 条全部 success=t（已执行） | psql 只读：`SELECT installed_rank, version, checksum, success, installed_on FROM flyway_schema_history WHERE installed_rank BETWEEN 175 AND 188` → rank 181~188 全部 success=t；本次提交为**固化已执行内容，无任何新执行**（未跑 migrate，仅 git add/commit） |
| **⑤ Flyway 冲突风险** | **无 validate 失败风险** | 文件内容与已执行 checksum 一致 → 新环境按 git 全量重建后 validate 通过；无任何不一致需停下报告 |
| **审查结论** | **通过（8/8）** | 可入库；提交后新环境重建不再缺 8 个迁移（OBS-1 风险面：receipt_signature/receipt_evidence/receipt_barcode/notification/tax_record 等表建表/补列可还原） |

- **精确提交**：`git add` 仅上述 **8 个文件显式路径**（无通配符/目录）；提交前 `git status --short` 复核暂存区仅这 8 个文件（8 项 A，无混入）。
- **提交**：`b9a4aeb` — `chore: 8 个已执行 migration 入库固化（rank 181~188，G-2 OBS-1：随生产工作树构建应用、success=t、无新执行；checksum 8/8 与 flyway_schema_history 一致）`（8 files changed, 160 insertions）。
- **影响范围**：纯 git 入库，零 DDL/DML、零行为变化；红线 R-1（窗口内不动后端/DB）合规（psql 仅只读 SELECT、mvn flyway:info 只读、未重启 :8081）。
- **风险**：无（checksum 全匹配）。**G-2 门禁状态：已闭环**。

> **既有版本号怪癖登记（不处置）**：schema history 存在旧版本 `2026022701`（8 位数字，rank<175，success=t），Flyway 按版本数值比较视其高于 20260811.001 → `flyway:info` 显示 `Schema version: 2026022701`、目标 8 迁移状态显示 `Out of Order`——既有命名事实，非本次 8 文件引入，与 `out-of-order: true` 应用配置自洽，不影响 validate/应用；V20260816_001 入库后同样显示 Out of Order（既有行为）。仅登记。

---

*初始创建：2026-08-15。本文件为 RM 手持收货域专项轨任务池，登记来源 `docs/quality/client-audit-receiving-mobile.md` + `multi-client-audit-summary.md` + `remediation-roadmap.md` §5.5（架构裁决 D-1~D-4）。Batch 1 QA 验收回写：2026-08-15（`docs/quality/rm-b1-qa-report.md`：PASS×2 + PWL×1；OBS-4/5/6 与 L-2 新登记；Batch 2+ 实施卡拆卡 §三-B）。§五-A G-1/G-2 提交固化登记回写：2026-08-15（用户指令②，commit `4fc4f64` / `b9a4aeb`；红线合规：仅 git 提交，未落库、未动后端服务）。未修改任何代码功能。*
*RM-B2 实施卡 1 QA 验收回写：2026-08-16（`docs/quality/rm-b2-impl1-qa-report.md`——验收①~⑤全过 **PASS**、**G-3 生产独立实测已执行（通过）**（readiness §4.2 G-3，qa 责任项）；状态：开发完成 → **QA PASS**；**REV 标注：回归基线转正由 regression 执行**（REG-ETM 系/RM 轨条目，候选见 qa 报告 §六）。planner 仅登记与状态流转，未写代码、未做正确性判断）。*
*RM-B2 实施卡 3 排期登记回写：2026-08-16（planner——实施卡 3 状态更新为「下一批排期（2026-08-16）」：① idx_inventory_batch_no 索引落库（`CREATE INDEX IF NOT EXISTS` 幂等覆盖 OBS-4，架构 §十 事项 3 已批准）；② 产品口径列收敛治理候选登记（RM-B1-001-OBS-2，§十 事项 4 批准保留不删）；**与 OIC-BE 阶段二批次并行候选**。仅登记排期，未写代码、未做正确性判断）。*
*主机重启事件注记（2026-08-16 用户通知，planner 登记）：主机重启，观察窗口中断（服务停止-恢复）。按既有先例处置——**中断如实登记、不重置计时、收口时间不变**；RM 轨当前无进行中 observe 检查点（Batch 2+ 实施卡未进入 observe），本注记仅登记事件；若 RM 轨相关服务受重启影响，恢复后按 `docs/quality/observation-log.md` 恢复基线检查（R-1~R-6）执行后再继续相应流程。仅登记事件，未写代码、未做正确性判断。*
*RM-B2 实施卡 3 待放卡确认回写：2026-08-17（planner——实施卡 3 状态由「下一批排期（2026-08-16）」更新为「**待放卡**」：观察收口条件已满足（管理端 Batch 2 observe 收口 08-16 12:55 已过、OIC-2 Batch 2 observe 收口 08-17 18:00 已完成、RM 轨无进行中 observe 检查点——主机重启事件已注记登记不阻塞放卡）；与 OIC-BE 阶段二批次并行放卡；卡面要点（idx_inventory_batch_no 索引落库幂等 + 产品口径列收敛治理候选）保持。仅登记与状态流转，未写代码、未做正确性判断、历史条目零修改）。*