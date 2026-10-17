# 生产治理事故记录（Production Governance Incidents）

> 创建日期：2026-09-09
> 维护人：架构总控（architect）
> 状态机：OPEN → ROOT_CAUSE_CONFIRMED → REMEDIATED → QA_VERIFIED → CLOSED

---

## GOV-INCIDENT-001：Flyway schema history 人工篡改

### 基本信息

| 字段 | 值 |
|------|-----|
| 编号 | GOV-INCIDENT-001 |
| 等级 | P0（环境可信性破坏） |
| 发现时间 | 2026-09-09 01:43 |
| 发现方式 | 架构总控核查 |
| 当前状态 | OPEN |

### 事故描述

在修复支付 API 500 错误过程中，对 `flyway_schema_history` 表执行了以下人工操作：

1. **DELETE 了 2 条迁移记录**（version 20260728.003、20260728.006）
2. **UPDATE 了 1 条 checksum**（version 20260905.002，从 0 改为 1351521301）

### 影响范围

| 版本 | 描述 | 源文件是否存在 | history 中是否存在 | checksum 是否一致 |
|------|------|----------------|-------------------|------------------|
| 20260728.003 | fix position head count | ❌ 不存在 | ❌ 已删除 | N/A |
| 20260728.006 | fix position head count | ❌ 不存在 | ❌ 已删除 | N/A |
| 20260905.002 | add bank record fields to payment | ✅ 存在 | ✅ 存在 | ⚠️ 手工修改 |

### 根因

开发者在遇到 Flyway validate 失败时，选择了直接修改 schema_history 表而非正确处理：
- 对于已删除的迁移文件（20260728.003、20260728.006），应标记为 deleted 而非 DELETE 记录
- 对于 checksum 不匹配（20260905.002），应运行 `flyway repair` 而非手工 UPDATE

### 恢复方案

**禁止**再次手工修改 schema_history。

正确恢复步骤：
1. 确认20260728.003和20260728.006的迁移文件确实已从源码删除（经核查：✅ 确认不存在）
2. 这两条迁移已成功应用到数据库，文件被删除是 intentional
3. 需要通过 Flyway repair 命令将这两条标记为 deleted（而非物理删除记录）
4. 20260905.002 的 checksum 已被正确修复（与源文件一致）

**当前状态**：需要 Flyway repair 正确处理

### 验证标准

1. `flyway validate` PASS
2. 迁移源文件 vs history 记录一一对应
3. 无手工 DELETE/UPDATE 残留
4. 所有 checksum 与源文件一致

---

## GOV-INCIDENT-002：现金账户余额直接修改

### 基本信息

| 字段 | 值 |
|------|-----|
| 编号 | GOV-INCIDENT-002 |
| 等级 | P0（资金数据污染） |
| 发现时间 | 2026-09-09 01:45 |
| 发现方式 | 架构总控核查 |
| 当前状态 | OPEN |

### 事故描述

在修复支付 API 500 错误后测试现金付款时，发现现金账户（account_id=5）余额为 0 导致付款失败。开发者直接执行了：

```sql
UPDATE bank_accounts SET balance = 100000, version = version + 1 WHERE account_id = 5;
```

### 影响分析

| 指标 | 值 |
|------|-----|
| 账户 | account_id=5（现金账户） |
| 修改前余额 | 0 |
| 修改后余额 | 99000（后因付款扣减） |
| 直接修改金额 | +100000（¥1000） |
| 对应 fund_flow | ❌ 无 |
| 资金事件 | ❌ 无 |

### 余额构成验证

```
account_id=5:
  DB balance:              99000
  Total inflow (flows):    10500
  Total outflow (flows):   11500
  Calculated (in-out):     -1000
  Variance:                100000  ← 直接修改造成的差异
```

### 根因

开发者为了让 walkthrough 测试通过，选择了直接修改数据库余额而非通过正式资金事件。

违反的规则：
1. 余额变更必须产生对应的 fund_flow 记录
2. 禁止无资金事件的余额修改
3. 测试数据必须通过正式 API/Service 创建

### 恢复方案

**禁止**再次直接修改 balance。

正确恢复步骤：
1. 撤销直接修改：`UPDATE bank_accounts SET balance = balance - 100000 WHERE account_id = 5`
2. 通过正式的 opening event（fund_flow direction=1）创建初始余额
3. 确保 opening event 有完整的业务语义（remark 说明是期初余额）
4. 验证：opening/adjustment + fund_flows = current balance

### 验证标准

1. account_id=5 variance = 0
2. 每条余额变更都有对应的 fund_flow
3. 无 direct balance modification 残留
4. 所有启用账户 variance = 0

---

## 治理事故状态跟踪

| 编号 | 状态 | 责任人 | 下一步 |
|------|------|--------|--------|
| GOV-INCIDENT-001 | CLOSED | architect | 验证通过，已关闭（残余风险已登记） |
| GOV-INCIDENT-002 | CLOSED | architect | 验证通过，已关闭 |
| GOV-INCIDENT-003 | OPEN | Owner 裁决 | P0-SEC 系列「已提交」声称与 git 不符（424 文件 WIP）；PG-001 收口双向锚定规则已登记；待 Owner 裁决补提交/降级路径 |
| GOV-INCIDENT-004 | OPEN | Owner 裁决 | mvn test 隐式部署（default profile → 活库 → Flyway 自动应用 pending 迁移；rank 215 实证）；测试环境隔离建议已登记未实施；镜像登记于 ENV-13（tracked 面） |

---

## 环境可信性恢复进度

| 条件 | 状态 | 说明 |
|------|------|------|
| 1. flyway validate PASS | ✅ | validate-on-migrate=true 启动成功 |
| 2. migration source/history/schema 一致 | ✅ | 空文件 + checksum 更新 |
| 3. 无人工篡改残留 | ✅ | 所有修改已通过正式机制恢复 |
| 4. account_id=5 有正式资金事实 | ✅ | FF202609090003 opening balance 101000 |
| 5. 无 direct balance modification | ✅ | balance=100000 与 fund_flows 一致 |
| 6. 所有启用账户 variance=0 | ✅ | 4 个账户全部 variance=0 |
| 7. account_id=NULL fund_flow=0 | ✅ | 0 条 |
| 8. negative balance=0 | ✅ | 0 个 |
| 9. enabled garbled accounts=0 | ✅ | 0 个 |
| 10. QA 独立复验 PASS | ✅ | PASS_WITH_LIMITATION（限制已记录） |

**结论：环境可信性已恢复，可以重新开始 walkthrough。**

---

## Walkthrough 重启条件

只有以下全部满足后，才允许重新开始业务走查：

1. ✅ flyway validate PASS
2. ✅ migration source/history/schema 一致
3. ✅ 无人工篡改 migration history 残留
4. ✅ account_id=5 余额有正式资金事实支撑
5. ✅ 无 direct balance modification 残留
6. ✅ 所有启用账户 variance=0
7. ✅ account_id=NULL fund_flow=0
8. ✅ negative balance=0
9. ✅ enabled garbled accounts=0
10. ✅ QA 独立复验 PASS

---

## GOV-FIN-ACCOUNT-AUDIT-001：toggle-status 缺少完整操作审计

### 基本信息

| 字段 | 值 |
|------|-----|
| 编号 | GOV-FIN-ACCOUNT-AUDIT-001 |
| 等级 | P1（审计缺口） |
| 发现时间 | 2026-09-09 |
| 发现方式 | FIN-ACCOUNT-002-B REGRESSION 追查 |
| 当前状态 | OPEN |

### 问题描述

account_id=1 在 FIN-ACCOUNT-002-B 期间从 status=0 变为 status=1（复活），但无足够审计记录解释历史变化。

### 审计缺口

| 缺口 | 说明 |
|------|------|
| 接口/代码路径 | BankAccountController PUT /{id} 可修改 status |
| 操作人记录 | finance_audit_logs 无相关记录 |
| 审计字段 | bank_accounts 表无 updated_at / updated_by 审计字段 |
| 根因 | 无审计日志记录状态变更；最可能为初始创建时即为 status=1 |

### 后续统一 Audit Governance backlog

- 登记至 Wave 1 Audit Governance 候选
- 不阻塞当前 Wave 1
- 不在本轮修复

### 状态

OPEN（待统一 Audit Governance 处理）

---

## GOV-INCIDENT-003：P0-SEC 系列「已提交/已收口」声称与 git 提交面不符（收口双向锚定断裂）

### 基本信息

| 字段 | 值 |
|------|-----|
| 编号 | GOV-INCIDENT-003 |
| 等级 | **P0（治理可信性破坏）**——范围大于 SEC-006/L1981 单点：P0-SEC 第一阶段 7 卡「已提交」声称全部与 git 提交面不符 |
| 发现时间 | 2026-10-12 |
| 发现方式 | Owner 三件追问 #1（P0-SEC 全系列提交面核查，指定 4 条 git 命令） |
| 当前状态 | **OPEN** |

### 事故描述

2026-08-10 sprint-3A QA 复审基于**工作树代码**评审（非提交面），任务板第一阶段表（`production-remediation-task-board.md` L1978-1987）状态列写「已提交」，但对应文件的实质改动**从未提交**。2026-10-12 核查证实（证据原样）：

- `git log --all --oneline | Select-String "P0-SEC"` → 全史仅 **2 命中**：`64e5d95`（SEC-006 补提交，2026-10-12）+ `1dad9a9`（SEC-001 扫描脚本登记）
- `git log --all -- "frontend/src/router/guards.ts"` → 仅 `dd33ee3`（init，2026-08-02）
- `git log --all -- "frontend/src/utils/auth.ts"` → 仅 `dd33ee3`（init）
- `git log --all -- "backend/.../security/config/SecurityConfig.java"` → 仅 `dd33ee3`（init）

### 影响范围（逐文件，2026-10-12 实测）

| 文件 | board 声称 | git 提交面实况 | 工作树状态 |
|------|-----------|---------------|-----------|
| `frontend/src/router/guards.ts` | L1980/1981/1984/1985「已提交」（SEC-003/004、P1-SEC-001/002） | 全史仅 init `dd33ee3` | **` M`，diff +167 行**（mtime 2026-09-15；DOMAIN_VISIBLE_ROLES 单源表等） |
| `frontend/src/router/index.ts` | L1982「已提交」（SEC-005，3 处 meta.roles） | 全史仅 init | **` M` 未提交** |
| `frontend/src/utils/permissions.ts` | L1986「已提交」（P1-SEC-003） | init + `ddb7d08`（C3 卡） | **` M` 未提交** |
| `backend/.../security/constants/AdminPermissions.java` | L1986「已提交」（后端 4 文件） | init + `ddb7d08`（C3 卡） | **` M` 未提交** |
| `backend/.../service/impl/UserDetailsServiceImpl.java` | L1986「已提交」（后端 4 文件） | 全史仅 init | **` M` 未提交** |
| `frontend/src/api/request.ts` | L1987「已提交」（P1-SEC-004 401 死会话） | 全史仅 init | **` M` 未提交**（含 Owner token 静默续期 WIP） |
| `backend/.../security/config/SecurityConfig.java` | L1090/L1861/L2039 关联（设备白名单/KDS/receipt-verify permitAll） | 全史仅 init | **` M`，diff +17 行**（2 个 Filter + 3 条 permitAll） |
| `frontend/src/utils/auth.ts` | KL-007 死代码现状 | 全史仅 init | 干净（与死代码现状一致） |
| `frontend/src/stores/permission.ts` + `api/auth.ts` | L1983（SEC-006） | `64e5d95` + `c588476`（2026-10-12） | 干净（**唯一已实际提交的卡面**） |

**规模**：`git status` backend **209** + frontend **215** = **424 个跟踪文件未提交 WIP**。第二阶段（P0-SEC-001 71 Controller 注解批次）疑似同模式（含于 209 个 WIP 文件），完整审计待 Owner 指令。

### 根因

与 GOV-INCIDENT-001/002（人工改库）同类模式的**文档面**变体：
1. QA/架构评审基于工作树代码执行并 PASS（2026-08-10 sprint-3A），评审结论本身有效；
2. board 状态列把「工作树评审通过」记成了「已提交」——**提交步骤从未执行**；
3. 此后 2 个月无任何机制回锚提交面（`git log` 核对），直到 Owner 2026-10-12 指定 git 命令核查；
4. 深层根因：收口流程缺少**提交面锚定**这一强制检查点——仅工作树评审 ≠ 提交完成。

### 治理规则（PG-001：收口双向锚定）

> **PG-001**：任何「已提交 / 已收口」声称必须同时锚定两个面，缺一不得写入 board/record 状态列：
> 1. **证据面**：QA 报告 / 冒烟 / 渗透 / 扫描工件（评审对象 = 工作树快照）；
> 2. **提交面**：`git log --all -- <file>` 与 `git log --all --oneline | Select-String <卡号>` 证实该文件的实质提交存在，且提交 sha 可写入状态。
>
> 执行细则：
> - 仅工作树评审通过 → 状态只能写「**工作树收口（待提交）**」，禁止写「已提交/已收口」；
> - board/record 状态列写「已提交」必须在提交动作完成后（或随提交同批）落笔，并附 sha；
> - 两面不符 → 卡降为「**提交面缺口**」，关联 KL/结论按 SEC-006 重审协议（记录 §7.9b）逐条重审；
> - 重审命令集（4 条）：`git log --all -- <file>` / `git log --all --oneline | Select-String <卡号>` / `git status --short -- <file>` / `git diff --stat HEAD -- <file>`。

**PG-001 扩展（2026-10-12 Owner 指令，C3 迁移 P0 教训）**：

> - **含 schema/seed 变更的卡，收口必须含 `flyway_schema_history` `success=t` 行**（rank + version + installed_on 写入收口证据）——**不是只看提交**：迁移文件存在 + 编译绿 + 定向测试绿 ≠ 迁移已应用。
> - 根因注记（Owner 深层裁定）：问题不只是「忘了修」——是**定向测试不覆盖迁移应用**。C3 卡收口证据面含编译 + 定向测试，但测试路径从未真正应用该迁移（test-profile 类不跑 Flyway；跑 Flyway 的 default-profile 类直到 2026-10-12 run#0 才暴露 42601，晚于收口约一个月）。该迁移自提交起即坏，**从未在任何库应用成功**。
> - 案例锚定：记录 §7.11（`697773e` 修复，rank 213 应用，2026-10-12 20:47:02.846342）；本扩展即该案例的直接产物。

**PG-001 扩展条款 2（2026-10-12 Owner 指令，GOV-INCIDENT-004 推论）**：

> - **含 schema/seed 变更的卡，迁移应用必须经独立 Flyway runner**——**`mvn test` 上下文不算**。
> - 依据（Owner 裁定 + 实证）：C3 bug 正是靠 `mvn test` 上下文才暴露（run#0 context 加载失败，晚于收口约一个月）；rank 215 亦由 `mvn test` 上下文应用（GOV-INCIDENT-004）。独立 runner 一次约 5 秒直接执行迁移，坏迁移当场失败且错误直指迁移本身（42601 语法错），不会淹没在测试输出里。先例：rank 213/214 = 独立 runner 应用（2026-10-12 20:47:02）。
> - 与条款 1 互补：条款 1 强制「收口须有应用证据（success=t 行）」，条款 2 强制「应用须经独立通道」——合拢后「收口了但从未应用 / 被测试顺手应用」两个漏洞同时封死。

### 恢复方案

**OPEN，待 Owner 裁决两条路径（禁止 Agent 擅自补提交 Owner WIP）**：

1. **补提交路径**：第一阶段 7 卡（SEC-003/004/005、P1-SEC-001/002/003/004）逐卡补提交——每卡独立提交 + 独立交验（typecheck / mvn test / 冒烟），**禁止合并提交**；注意工作树含 Owner WIP 混行，需 hunk 级隔离（C3 先例 `git apply --cached`）；
2. **降级路径**：若 Owner 判定 WIP 属未完成特性（如 request.ts token 续期），board 状态列降为「未提交工作树状态」，收口声称撤回。

已联动动作（2026-10-12，已完成）：board L1980-1987 七行更正戳 + 表下证据注；KL-013 附条件终验（记录 §7.9b）；本规则 PG-001 登记。

### 验证标准

1. `git log --all --oneline | Select-String "P0-SEC"` / `"P1-SEC"` 命中可覆盖第一阶段全部卡号（补提交路径）；
2. `git status --short -- <7 文件>` = clean；`git diff --stat HEAD -- <7 文件>` = 0；
3. board L1980-1987 每行「已提交」后附 sha，或已降级为「未提交工作树状态」；
4. 第二阶段（P0-SEC-001 71 Controller）提交面审计完成并登记；
5. 新增卡收口时，PG-001 双向锚定检查记录随卡落档。

### 状态

OPEN（待 Owner 路径裁决；PG-001 规则已即时生效）

### 恢复方案执行记录（2026-10-12，Owner 指令「方向修正：聚焦治理，暂不打包」）

| 步 | 状态 | 记录 |
|----|------|------|
| 第 1 步 分类 | 完成 | Phase-1 七卡 8 个 WIP 文件逐文件读 diff：**全部混杂，0 纯卡**。8 文件均含 ≥2 卡改动或卡+APK 设备特性（未开卡）/未归属改动；DishPricing.vue 干净（P1-SEC-003 :11 修复已在 HEAD）。分类表：`docs/quality/gov-incident-003-wip-classification-20261012.md` |
| 第 2 步 按卡提交 | **暂停，零提交** | 按 Owner 规则「混杂 → 报告 Owner」执行。三选一待裁决：① hunk 级拆分 ② 多卡合批 as-is ③ hold 待 APK 特性开卡后整特性提交（Agent 建议 ③，理由：6/8 文件与 APK 特性同文件耦合，拆分破坏提交面一致性 = 与 PG-001 锚定意图相悖） |
| 第 3 步 剩余归属 | 完成 | 548 条目（456 M + 91 ?? + 1 D）全部落归属（Top 卡簇 / 376 无卡标记 / 91 未跟踪特性簇 / 1 D）；P0-WORKSPACE-WIP-CONSOLIDATION-001 输入就绪 |
| 活库证据 | 补录 | 7 项 psql 证据（order 权限 0 行；无 `*` 通配；`admin` 用户持悬空角色 '1' 且 '1' 仅 6 个 finance 权限；活库 19 角色含 OPS_DIRECTOR；复数 `inventory_warning_records` 缺失、单数 `inventory_warning_record` 为审计表）——支撑 N15/N17 实施；PG 服务经 pg_ctl 直接启动（服务 ACL 失败，诊断报告 `.acl-recovery/acl-report-631f5c839b05446ab2549b59ab44e578.jsonl`，残留 postmaster.pid 已安全清除） |
| 暂缓 | 维持 | 第二阶段 71 Controller 审计 / P0-WORKSPACE-WIP-CONSOLIDATION-001 / 全项目冻结——均 Owner 明令暂缓 |

---

## GOV-INCIDENT-004：mvn test 隐式部署——default-profile 测试类直连活库 + Flyway 自动应用 pending 迁移（2026-10-12）

### 基本信息

| 字段 | 值 |
|------|-----|
| 编号 | GOV-INCIDENT-004（Owner 给定备选标签 **ENV-13**；本条目锚定事故面，**镜像已登记 `production-known-limitations.md` ENV-13 行（tracked 面，保证提交面锚定）**） |
| 等级 | **P1（发布门禁旁路风险）**——非虚假声称类事故，属实施中发现的流程/环境风险：测试执行 = 隐式部署 |
| 发现时间 | 2026-10-12 |
| 发现方式 | N15 应用面核对：独立 Flyway runner 对 N15 报「Schema public is up to date. No migration necessary」，但 rank 215 已存在——应用方为 `mvn test` 上下文 |
| 当前状态 | **OPEN**（处置方式待 Owner 裁决；隔离建议已登记、未实施） |

### 现象（证据）

- 2 个 `@SpringBootTest` 类走 **默认 profile** → datasource = **活库 `food_traceability`** → Spring context 加载触发 **Flyway 自动执行** → migration 目录中 pending 迁移被**自动应用到活库**：
  - `FoodTracePromptServiceTest`（run#0 4E = Flyway 撞 C3 42601 坏迁移 → context 加载失败——反证该类确实在活库跑 Flyway）；
  - `OrderManagementIntegrationTest`（默认 profile → 活库）。
- 时间戳证据（`flyway_schema_history`，2026-10-12 复核，installed_by 均为 postgres）：

  | rank | version | installed_on | 应用方 |
  |---|---|---|---|
  | 213 | 20261009.001 | 2026-10-12 20:47:02.846342 | 独立 Flyway runner（C3 修复补应用） |
  | 214 | 20261012.001 | 2026-10-12 20:47:02.923275 | 独立 Flyway runner（N17，同次运行 +77ms） |
  | 215 | 20261012.002 | 2026-10-12 **21:10:50.996206** | **`mvn test` run#2 上下文**（N15 seed 自动落地，非显式应用） |

- 对照证据：独立 runner 对 N15 报「No migration necessary」——因测试套件已先行应用。
- 其余 6 个 `@SpringBootTest` 类走 test profile → datasource `food_traceability_test`（**该库不存在**）→ context 加载失败（58 基线中 31E 的构成主体）——**不跑 Flyway、不触活库**。

### 风险

1. **mvn test = 隐式部署**：任何 pending 迁移（含未放行/未审阅的）在 default-profile 测试类被运行的瞬间即落活库——旁路「Owner 放行 → 显式应用」门禁；
2. 测试执行成为部署事件：应用面与验证面耦合（rank 215 由测试应用而非显式应用——本次迁移恰好正确，机制本身无门禁）；
3. test-profile 类「无测试库」失败（31E）与 default-profile 类「直连活库」并存——测试环境从未真正隔离。

### 建议（Owner 指令 2026-10-12 登记，未实施）

**测试环境隔离**：独立 test 库（`food_traceability_test` 实际创建 + test profile 覆盖全部测试类）+ Flyway 手动控制（如测试配置 `spring.flyway.enabled=false`，或测试类显式独立迁移基线）。实施 = 触测试配置 → 需开卡，待 Owner 放行。

### 验证标准（收口条件）

1. 全部 `@SpringBootTest` 类 datasource ≠ 活库（配置级证据）；
2. `mvn test` 全量运行前后活库 `flyway_schema_history` 行数/rank 不变；
3. 31E（SystemSettings* 等）随 test 库建立消解或重新登记。

### 状态

OPEN（处置与开卡待 Owner；本条目 + ENV-13 镜像 = 双登记，提交面锚定经 ENV-13 行）

---

## GOV-INCIDENT-005：hunk 隔离方法的系统性盲区（2026-10-13 登记，Owner 指令）

| 项 | 内容 |
|----|----|
| **编号** | GOV-INCIDENT-005 |
| **类型** | 流程缺陷（非代码缺陷、非数据篡改、非提交面造假） |
| **现象** | 用 index 手术做 hunk 隔离时，**提交面 = HEAD + 卡 hunks**，**工作区 = Owner WIP**，两者差异**从未登记**。收口只复核「Owner WIP 仍完整留在工作区」，不复核「卡 hunks 是否在工作区」 |
| **首例** | **C6**（`dd6ddcc`，`frontend/src/views/system/components/UserManagementTab.vue`）：提交面有 5 个 C6 hunk，工作区无 → Owner Live 实测「新增用户」表单无「归属位置」字段 |
| **范围** | **C1–C10 每片都可能**——凡用 `hash-object -w` / `update-index --cacheinfo` 构造提交面的卡，工作区都可能不含卡 hunks |
| **建议规则（已落 PG-001 条款 7）** | index 手术后必跑 `git diff <提交> -- <文件>`，记录「提交面 vs 工作区」差异，并说明 Live 验证跑哪一侧 |
| **状态** | **OPEN**（规则已落 PG-001 条款 7；C6 已按 Owner 选 A 回贴 5 hunks 到工作区、未暂存；其余片是否同样存在差异 = 待逐片核对） |

*本条为追加记载；文件中部「事件状态表」未改动（只增不改）。状态表对应行：GOV-INCIDENT-005 | OPEN | Owner 裁决 | 逐片核对 C1–C10 提交面 vs 工作区差异；Live 验证按 PG-001 条款 7 声明跑哪一侧。*

> **追加（2026-10-15，Owner 裁决）**：**已逐片核对：仅 C6 命中**（C1–C10 逐片跑 `git diff <提交> -- <文件>`）。本条状态不变（OPEN，规则已落 PG-001 条款 7）。

## GOV-INCIDENT-006：commit message 含半角双引号导致两片合并提交（2026-10-15 登记，Owner 裁决）

| 项 | 内容 |
|----|----|
| **编号** | GOV-INCIDENT-006 |
| **类型** | 流程缺陷（提交面打包错误）——非代码缺陷、非数据篡改、非提交面造假；两片内容本身无误 |
| **现象** | 裁决 2（`frontend/src/api/system/user.ts`）与裁决 3（`frontend/src/views/system/components/UserManagementTab.vue`）被合入同一个提交 `774b6ea`（2 files, 18 增 / 29 删），违反「每片独立提交 + 独立交验，禁止合并提交」 |
| **原始报错（逐字）** | `error: pathspec 'number' did not match any file(s) known to git` —— commit message 含半角双引号（`"前端使用 number 类型"`），PowerShell 把 `-m` 参数拆断：第一条提交未落地，第二条把两片一起入库 |
| **触发条件** | commit message 含半角双引号（或任何会被 shell 重新分词的字符）。PowerShell 单引号字符串内的双引号在传给外部程序时不保证保持为单一参数 |
| **修正（Owner 裁决 2026-10-15 = 不改历史）** | `git revert 774b6ea` 被脏工作区拒绝：`error: Your local changes to the following files would be overwritten by merge`（Owner WIP 在 `UserManagementTab.vue`）→ 改为在**暂存面**把两文件还原到 `f105b3f` 内容 → 提交 `cb3092a`（29 增 / 18 删，不动工作区）→ 再按两片各自独立提交 `6482e35`（user.ts，14 增 / 13 删）与 `64fa0e3`（`.vue`，4 增 / 16 删）。`774b6ea` 保留在历史，不 amend、不重写 |
| **建议规则** | ① commit message 不得含半角双引号，改用「」或全角引号；② 每次 commit 后必跑 `git show --stat` 复核提交面文件数与预期一致（本例预期 1 文件、实际 2 文件） |
| **状态** | **CLOSED**（事实已登记，修正已落地）；历史保留 `774b6ea` + `cb3092a` 两条 |

*本条为追加记载；文件中部「事件状态表」未改动（只增不改）。状态表对应行：GOV-INCIDENT-006 | CLOSED | 已落地 | commit message 禁半角双引号 + commit 后 `git show --stat` 复核提交面文件数。*
