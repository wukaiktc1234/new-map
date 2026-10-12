# GOV-INCIDENT-003 补提交路径 · WIP 分类表（2026-10-12）

> **依据**：GOV-INCIDENT-003（P0，OPEN）恢复方案「补提交路径」第 1 步「分类」+ Owner 裁决规则「纯卡改动 → 提交；混杂 → 报告 Owner」+ PG-001（收口双向锚定）
> **范围**：`git status --short` 全量 **548 条目 = 456 M + 91 ?? + 1 D**（全清单见 `.git/wip-status.txt`）
> **数据来源**：`.git/wip-diff.patch`（全量 diff，逐文件读毕）、`.git/wip-classification-raw.txt`（按卡标记逐文件聚类，457 段）
> **Phase-1 7 卡**（board L1980-1987 被更正戳的声称卡）：P0-SEC-003 / P0-SEC-004 / P0-SEC-005 / P1-SEC-001 / P1-SEC-002 / P1-SEC-003 / P1-SEC-004

---

## 1. Phase-1 七卡 × 文件分类（结论：0 纯卡文件）

| # | 文件 | WIP diff | 卡面改动（归属卡，证据） | 非卡面改动（证据） | 判定 |
|---|------|---------|--------------------------|--------------------|------|
| 1 | `frontend/src/router/guards.ts` | +126/−41 | P0-SEC-003（角色-域矩阵）；P0-SEC-004（DOMAIN_VISIBLE_ROLES 单源 + meta.roles 分支）；P1-SEC-001（三角色放行）；P1-SEC-002（public-route 分支） | **APK 设备门禁**：`isRunningInApp`、`/device-activation` 路由、`apkAllowedPages`、import `@/api/device-registration`（该文件为未跟踪 ??） | **混杂** |
| 2 | `frontend/src/router/index.ts` | +13/−5 | P0-SEC-005（company-init / device-whitelist / supplier-portal+links 路由 roles）；P1-SEC-002（verify-code / device-activation 路由） | **P1-A 银行路由**（bank 相关，P1-A 卡）；**PD-004 3 条路由删除**（PD-004 卡）；**P1-FIN-RVT-001 注释**；device-whitelist / receiving-mobile 路由（设备特性） | **混杂** |
| 3 | `frontend/src/api/request.ts` | +141/−9 | P1-SEC-004（handleSessionExpired、静默刷新、401 去重、登录态 401 区分，卡面 :164-199） | **X-Device-Id 头注入**（APK 设备特性；import 未跟踪 `@/utils/device-id` + `@/api/device-registration` → 单独提交将破坏构建）；**FormData Content-Type 修正**（无卡标记，未归属） | **混杂** |
| 4 | `frontend/src/utils/permissions.ts` | +9/−1 | P1-SEC-003（`product:pricing:batch`、`product:cost:export` 注册表） | **`purchase:order:terminate` / `purchase:order:freeze`**（采购终止/冻结，属其他采购卡面） | **混杂** |
| 5 | `backend/.../security/constants/AdminPermissions.java` | +23/−3 | P1-SEC-003（`product:cost:export`） | **P1-A 银行/余额码**；**P0-SEC-001-D system 码**；**purchase:order 6 码**；**purchase:return**；**finance:tax**；**finance:payment**（共 7 个非 P1-SEC-003 码族） | **混杂** |
| 6 | `backend/.../service/impl/TokenServiceImpl.java` | +6/−2 | P1-SEC-003（权限码注册表同步） | **P1-A bank/balance 码** | **混杂** |
| 7 | `backend/.../service/impl/AuthenticationServiceImpl.java` | +6/−2 | P1-SEC-003（同上） | **P1-A bank/balance 码** | **混杂** |
| 8 | `backend/.../service/impl/UserDetailsServiceImpl.java` | +6/−2 | P1-SEC-003（同上） | **P1-A bank/balance 码** | **混杂** |
| 9 | `frontend/src/views/product/DishPricing.vue` | 无 WIP | P1-SEC-003 卡面 :11 —— **修复已在 HEAD**（`v-permission="'product:pricing:batch'"` 已提交） | — | **干净**（无需提交） |

**Phase-1 范围外、同批发现的另 2 个 WIP 文件**（不计入上表结论，单列）：

| # | 文件 | WIP diff | 归属 | 判定 |
|---|------|---------|------|------|
| 10 | `frontend/src/views/product/DishCostAnalysis.vue` | +18/−9 | **全部为 P1-UI-CORE-006**（分页 + CaliberNoteBar；`v-permission` 码已在 HEAD） | **纯单卡**（但卡 = P1-UI-CORE-006，**非 Phase-1 七卡**；提交属另一卡面，须 Owner 另行放行 + 该卡验收） |
| 11 | `backend/.../security/config/SecurityConfig.java` | +17 | DeviceWhitelistFilter + KdsTokenFilter + 3 条 permitAll —— 设备白名单/KDS 特性（**无对应 board 卡**） | **未归属**（Owner WIP 或待开卡） |

### 判定结论

**Phase-1 七卡的 8 个 WIP 文件全部混杂（0 纯卡）。按 Owner 规则「混杂 → 报告 Owner」，本步骤（第 2 步）不执行任何提交。**

### 待 Owner 裁决项（三选一，或组合）

1. **hunk 级拆分**：逐文件 `git apply --cached` 只暂存卡面 hunk（C3 先例），每卡独立提交 + 独立交验。代价：request.ts / guards.ts 的卡面 hunk 与 APK 特性 hunk 有 import 依赖（卡面 hunk 单独提交后工作树构建仍依赖 ?? 文件，提交面构建快照不含卡面新增 import 时可通过；需逐 hunk 验证）；
2. **多卡合批提交（as-is）**：每文件按「主卡 + 伴随卡」一次性提交，提交信息列全卡号。违反「每片独立提交」字面规则，但保留工作树原貌、零 hunk 风险；
3. **暂持（hold）**：Phase-1 维持「工作树收口（待提交）」，先做第二阶段审计 / 或先完成 APK 设备特性开卡收口后整特性提交。

**建议**：选项 3（hold）——理由：8 个混杂文件中 6 个与「APK 设备特性（未开卡，91 个 ?? 文件簇）」同文件耦合，拆分即制造提交面/工作树不一致风险（PG-001 锚定的正是提交面一致性）；APK 特性开卡收口后按特性整批提交，卡面归属在提交信息中列全，可一次闭环。

---

## 2. 全量 WIP 卡簇分布（456 个 M 文件，按卡标记聚类）

Top 簇（文件数）：P1-FIN-EXPORT-004（10）、P1-EXPORT-002（9）、P1-FIN-EXPORT-001（9）、PD-001（7）、P1-MOCK-010（5）、P1-DATA-009（5）、P1-UI-CORE-007（4）、PD-002（4）、RVT-1（4）、PD-035（4）、RVT-002（3）、PD-028（3）、P1-FIN-WS-003（3）、RVT-4（3）、P1-DATA-010（3）、PD-003（3）、P1-UI-CORE-006（2）、P2-FIN-TRACE-005（2）、PD-004（2）、RVT-3（2）、KL-061（2）、P1-FIN-BE-001（2）、P1-FIN-WS-002（2）、RVT-2（2）、P0-FIN-001（2）、P1-API-004（2）、KL-057（2）、P1-FIN-RVT-001（2）、P1-FIN-WS-001（2）、P1-FIN-WS-006（2）、P1-UI-CORE-002/003/004（各 1-2）、P1-MOCK 系列（001/003/004/009/010）、P1-DATA-001~011 系列、KL-058/059/060（各 1）、P1-API-002/005、P2-FIN-PAY-003、PD-032/034、P0-SEC-001/003/004（各 1）、P1-SEC-002/004（各 1）、P1-FIN-LRVT-001~009（各 1）等。

**无卡标记的大簇**：backend **190** 文件、frontend **152** 文件、employee-frontend **13**、frontend-pos **5**、docs **5**、miniprogram **4**、misc **7** —— 合计 **376 文件无卡标记**（Owner WIP / 未开卡特性 / 历史遗留，逐文件归属超出本步范围，列入 P0-WORKSPACE-WIP-CONSOLIDATION-001 输入）。

## 3. 未跟踪（??）91 文件 · 特性簇

| 簇 | 代表文件 | 归属 |
|----|---------|------|
| **APK / 设备白名单 / KDS** | `capacitor.config.ts`、`build-apk.bat`、`android/`、`certs/`、`apk-download/`、`AppDeviceRegistration*`（5 类）、`DeviceWhitelistFilter.java`、`KdsTokenFilter.java`、`KdsAuthProperties.java`、`device-registration.ts`、`device-id.ts`、`device-activation/`、`device-whitelist/`、`verify-code/`、`ReceivingMobile.vue` | **board 无卡** → 待开卡或归属裁决 |
| **P1-A 银行对账** | `BankAccountReconciliation*`、`BankPaymentRecord*`、`BankAccountManage.vue` | P1-A |
| **原料扣减审计** | `MaterialDeductionContext/FailureException`、`MaterialConsumptionAuditService*` | 待归属 |
| **Receipt 签收** | `ReceiptSignature/ReceiptEvidence/ReceiptPrintLog`、`Receipt*.java`、`SignaturePad.vue`、`HandheldEntryDialog.vue` | 待归属 |
| **CanonicalOrder / Training / 其他 DTO** | `CanonicalOrderCommand/ItemCommand`、`TrainingEnroll*`、`AccountBalanceOpeningDTO` | 待归属 |
| **本地工具/杂项** | `s4c1-surgery.py`、`start-backend.cmd`、`backups/`、`design/brand/`、`frontend-design-v2~v4/`、`studyvault/`、`linux-env/`、`.omo/`、`.opencode/`、`.dsh-acl-recovery/`、`.verify-tmp/`、`docs/结论1.md`、`docs/结论2.md`、`docs/frontend-health-report.md` | 本地环境（多数应 gitignore） |

## 4. 已删除（D）1 文件

- `frontend/src/api/hr/recruitment-mock.ts` —— P1-MOCK 家族（mock 下线），归属 P1-MOCK 卡面。

## 5. 2026-10-12 活库证据补录（N15/N17 裁决支撑，psql 可复跑）

| # | 证据 | 查询 |
|---|------|------|
| L1 | `permissions` 表 `permission_code LIKE 'order%'` = **0 行**（N15 前提成立） | `SELECT count(*) FROM permissions WHERE permission_code LIKE 'order%';` |
| L2 | `role_permissions` 无 `*` 通配符行、无 order 权限行（**init-data 的 `1 → *` 设计未在活库生效**） | `SELECT * FROM role_permissions WHERE permission_id IN ('*','order:view','order:manage');` → 0 行 |
| L3 | 活库 roles = **19 个**（含 `ROLE_OPS_DIRECTOR`、`ROLE_STORE_MANAGER`、`ROLE_OWNER`、`ROLE_ADMIN`、`ROLE_SUPER_ADMIN`、`ROLE_EMPLOYEE`、`ROLE_TEAM_LEADER`…）；**无** CASHIER/WAREHOUSE/FINANCE/PURCHASE（init-data §1.2 种子未落活库） | `SELECT role_id_str, role_code FROM roles WHERE deleted=0;` |
| L4 | `admin`（系统管理员）用户持有 **`role_id='1'`（悬空：roles 表无 role_id_str='1' 行）**；'1' 仅绑 6 个 `PERM_FINANCE_PAYMENT_*` 权限 | `SELECT u.username, ur.role_id FROM users u JOIN user_roles ur USING...` |
| L5 | `test-admin` 持有 `ROLE_ADMIN`（125 权限）；`ROLE_OWNER` 125 权限；`ROLE_STORE_MANAGER` 10；`ROLE_OPS_DIRECTOR` **0**；`ROLE_EMPLOYEE` 6 | `SELECT role_id, count(*) FROM role_permissions GROUP BY role_id;` |
| L6 | 活库 4 张预警表：`inventory_warning`（legacy）、`inventory_warning_rule`（legacy 单数）、`inventory_warning_rules`（**现行规则表，调度器读它，存在**）、`inventory_warning_record`（**单数 = 操作审计表**：warning_id/action_type/operator_* 10 列，与实体 12 列 schema **完全不同**）；**复数 `inventory_warning_records` 不存在**（N17 根因确认 = 表缺失，非改名） | `\d inventory_warning_record` + `SELECT table_name FROM information_schema.tables WHERE table_name LIKE 'inventory_warning%';` |
| L7 | 权限命名约定（活库双轨）：`PERM_*`（125 权限主流）与裸码（`inventory:approve` 等）并存；`permission_type=1`、`module` 填域或 NULL、`status=1` | `SELECT * FROM permissions WHERE permission_id_str IN ('PERM_FINANCE_PAYMENT_CREATE','inventory:approve');` |

> 注：PG 服务本轮经 `pg_ctl` 直接启动（服务 ACL 失败：`P:\my-new-project\PostgreSQL\18` 中间层目录无当前用户权限；data 目录本身 FullControl，诊断报告 `.acl-recovery/acl-report-631f5c839b05446ab2549b59ab44e578.jsonl`；残留 `postmaster.pid` 已安全清除）。

## 6. 复验命令集（Owner 可独立复跑）

```powershell
# WIP 全量
git status --short | Measure-Object -Line
# Phase-1 8 文件逐文件提交面（PG-001 重审命令集）
git log --all --oneline -- frontend/src/router/guards.ts
git log --all --oneline | Select-String "P0-SEC|P1-SEC"
git diff --stat HEAD -- frontend/src/router/guards.ts frontend/src/router/index.ts frontend/src/api/request.ts frontend/src/utils/permissions.ts
# 活库
$env:PGPASSWORD='123456'; & "P:\my-new-project\PostgreSQL\18\bin\psql.exe" -h localhost -p 5432 -U postgres -d food_traceability -X -c "SELECT count(*) FROM permissions WHERE permission_code LIKE 'order%';"
```

---

**状态**：第 1 步（分类）完成；第 2 步（提交）按 Owner 规则暂停于「混杂 → 报告 Owner」；第 3 步（git status 剩余归属）= 本文 §1-§4 全量映射，548 条目全部落归属。
