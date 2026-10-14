# Process Guards

## PG-001 — 开卡前隔离检查（v2）

**规则**（v2，替代 v1"全工作区 clean"）：
1. 开卡前必须清除行尾幻影
   （校验：`git status` 的 M 数 = `git diff --numstat` 真实改动数；若有偏差，对幻影文件 `git checkout -- <file>` 消除）
2. 开卡时需声明：本卡预计改动的文件清单
3. 本卡 commit 时必须精确 `git add <指定文件>`
   （**禁止** `git add .` / `git add -A` / 目录通配）
4. commit 前必须 `git diff --cached` 检查 staged 内容，确认仅含本卡文件
5. 若发现本卡目标文件与其他批次 WIP **真实内容重叠** → 仍是 BLOCKED
   （仅行尾幻影重叠不算；真实重叠须 Owner 裁决，不得带重叠开工）
6. **禁静默 auto-push 类 hook**：仓库不得存在 commit 后自动 `git push` 的 hook（如 `.git/hooks/post-commit`）；
   commit 与 push 必须分离，push 仅由 Owner 显式指令触发（关联 ENV-8）

**v1 → v2 修订理由**（ENV-1 / ENV-2 / LEGACY-CLEANUP 三次实战暴露）：
- v1 的"全工作区 clean"在 467 真实改动 + 458 未跟踪环境下不可行
- 730 个行尾幻影被 v1 误判为"真实 dirty"（见 `docs/quality/workspace-wip-diagnosis-001.md` §1）
- v2 改为"开卡文件 clean + commit 隔离"，可执行且仍能拦截 ENV-2 类事故

**违规处置**：
  - 首次：本卡按 ENV 登记，不豁免
  - 连续两次：冻结新卡启动

**生效日期**：2026-09-25（v1：2026-09-24）
**关联**：ENV-1 / ENV-2 / ENV-3 / ENV-8

**收口双向锚定 + schema/seed 扩展（2026-10-12 追加，GOV-INCIDENT-003）**：
1. 任何「已提交/已收口」声称必须同时锚定**证据面 + 提交面**（规则全文：`production-governance-incidents.md` GOV-INCIDENT-003 §治理规则）；
2. **含 schema/seed 变更的卡，收口必须含 `flyway_schema_history` `success=t` 行**（不是只看提交；Owner 裁定根因：定向测试不覆盖迁移应用；扩展块：同上 §治理规则之后）；
3. **含 schema/seed 变更的卡，迁移应用必须经独立 Flyway runner**——`mvn test` 上下文不算（条款 2，2026-10-12 Owner 指令，GOV-INCIDENT-004 推论；先例 = rank 213/214 独立 runner 应用）。

## PG-002 — Repo 状态快照同步

**规则**：每次卡片收口或 push 后，developer 更新
  `docs/project-context/repo-state.md`

**理由**：跨会话/跨模型快速了解状态；减少重复 git 探索

**性质**：视图层，非权威；与 git 不一致时以 git 为准

**生效日期**：2026-09-25

**扩展（2026-10-13，Owner 指令：单一真相源，只增不改）**：
1. 单一真相源 = `docs/STATUS.md`（轻档）。每次卡片收口或 push 后**必改 `docs/STATUS.md`**。
2. 五处入口（`docs/INDEX.md` / `remediation-roadmap.md` / `production-remediation-task-board.md` / `production-known-limitations.md` / `docs/project-context/repo-state.md`）只保留指向 STATUS.md 的指针，**不改状态**；其历史行仍按只增不改。
3. 分歧处理：入口文档与 STATUS.md 不一致 → 以 STATUS.md 为准；STATUS.md 与 git 不一致 → 以 git 为准（视图层性质不变）。
4. `repo-state.md` 角色收窄为**历史快照**（不再作当前状态源）；原"更新 repo-state.md"的同步动作改为"更新 STATUS.md"。

**生效日期（扩展）**：2026-10-13

## PG-003 — Schema 单一真相源（Flyway）

**规则**：
  1. 所有表结构变更一律通过新增 Flyway migration（`backend/src/main/resources/db/migration/V*.sql`，只增不改）
  2. **禁止**新增任何手写 schema 脚本（非 Flyway 位置的建表/改表 SQL）；遗留脚本已于 2026-09-25 归档至 `docs/archive/legacy-sql/`
  3. 实体 `@TableId` / `@TableField` 必须与 Flyway 定义对齐；涉 schema/实体的卡片，收口前运行 `scripts/check-schema-alignment.py`
  4. 新增错位 → 当卡修复或单独立卡，不得留盲

**理由**：
  遗留 schema 脚本与 Flyway 并存冲突（food_category：遗留 `id` vs Flyway `category_id`），
  直接导致实体错位与 `/v1/pos/api/menu` 500（P1-POS-MENU-500-001）。

**违规处置**：
  - 手写脚本入库 → 当卡按 ENV 登记，脚本移除
  - 实体错位新引入 → 当卡修复，不豁免

**执行细节**：见 `docs/project-context/schema-governance.md`

**生效日期**：2026-09-25
**关联**：P1-POS-MENU-500-001 / P0-SCHEMA-SINGLE-SOURCE-001

## PG-004 — 文档批量编辑后必须复核

**规则**：
  任何对治理/任务/报告类文档的批量修改（脚本替换、多文件回写、python/sed 批量编辑），
  落盘后必须复核实际结果：
  1. 抽查至少一处修改点的前后文（防止前缀/锚点替换把旧文本残留拼接进新文本）
  2. 核对变更统计（git diff --stat 行数与预期一致）
  3. 涉及表格单元格的替换，须确认单元格边界完整（无错位 `|`、无跨单元格残留）

**理由**：
  2026-09-25 P1-COMBO-LEGACY-CLEANUP-001 收口时发现：前一轮 QA 回写使用前缀替换，
  旧长单元格尾部残留拼接进新文本（QA 文本后跟半截 BLOCKED 旧内容 + 错位 `|`），
  直至下一轮收口才被发现修复。批量编辑的静默损伤若无复核步骤将持续潜伏。

**违规处置**：
  - 批量编辑未复核即提交 → 发现后按 ENV 登记
  - 已造成文档损伤 → 修复并在此登记案例

**生效日期**：2026-09-25
**关联**：P1-COMBO-LEGACY-CLEANUP-001 收口轮损伤修复案例

## PG-005 — 三阶段强制安全边界（诊断 → 设计 → 实施）

**规则**：所有整改类工作（含进行中卡片工作）分三阶段执行，每阶段有独立允许/禁止边界与交接条件：

**阶段 1 — 诊断（只读）**
- 允许：读代码 / 读 DB / 读文档 / 写诊断报告
- 禁止：改代码 / 改 DB / 建卡 / 顺手修
- 交接条件：Owner 审过诊断报告

**阶段 2 — 设计（只出方案）**
- 允许：写设计文档 / 出改动清单 / 出回滚方案
- 禁止：改代码 / 改 DB
- 交接条件：Owner 拍板方案

**阶段 3 — 实施**
- 允许：改代码 / 补测试 / 补实施记录
- 禁止：改 DB schema（除本卡明确需要）/ 顺手修其他
- 交接条件：DS 抽检 + QA 独立验收

**跨阶段禁止**：
- 阶段 1 发现的问题不得顺手修 → 记入报告
- 阶段 2 出的方案不得提前实施 → 等 Owner
- 阶段 3 不得私自扩大范围 → 发现问题登记

**生效日期**：2026-09-26
**关联**：PG-001（commit 隔离）/ PG-003（schema 单一真相源）

## PG-005.1 — 纯 bug 豁免（Owner 批准机制）

**规则**：当同时满足以下三条时，允许跳过"设计阶段"直接进入实施：

1. 诊断报告明确修复方向唯一（无备选方案）；
2. 不涉及产品决策；
3. 不涉及架构选择（不新增/删除表、不改变接口契约、不改变业务语义）。

**豁免需 Owner 显式批准，不得由执行者自行判断。**

**理由**：PG-005 三阶段强制对"修复方向唯一"的纯 bug 卡是形式主义。
补本豁免，避免为纯 bug 写形式化设计文档。

**生效日期**：2026-09-26

## PG-006 — 判档分流（重档 / 轻档）

**规则**：开卡前，GPT 用一句话问 Owner："这是重档还是轻档？"，Owner 回一个字（重 / 轻），据此分流：

- **重档** = 走现有完整流程：PG-005 三阶段（诊断→设计→实施）+ 实施记录 + 更新 INDEX/roadmap + KL/ENV 登记 + PG-001 精确提交。
- **轻档** = 诊断→改→测→提交；一句 commit message 说清证据即可；**不写实施记录、不更 INDEX/roadmap**。

**性质**：给治理加一个"分流开关"，**不是再加一层治理**——重档不省、轻档不赘。判档只决定"流程走多深"，不影响 PG-001 的 commit 精确隔离（重/轻两档都必须精确 `git add` + `git diff --cached`）。

**生效日期**：2026-09-30
**关联**：PG-001（commit 隔离，两档都适用）/ PG-005（三阶段，仅重档强制）
