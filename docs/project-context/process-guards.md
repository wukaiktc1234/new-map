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

**v1 → v2 修订理由**（ENV-1 / ENV-2 / LEGACY-CLEANUP 三次实战暴露）：
- v1 的"全工作区 clean"在 467 真实改动 + 458 未跟踪环境下不可行
- 730 个行尾幻影被 v1 误判为"真实 dirty"（见 `docs/quality/workspace-wip-diagnosis-001.md` §1）
- v2 改为"开卡文件 clean + commit 隔离"，可执行且仍能拦截 ENV-2 类事故

**违规处置**：
  - 首次：本卡按 ENV 登记，不豁免
  - 连续两次：冻结新卡启动

**生效日期**：2026-09-25（v1：2026-09-24）
**关联**：ENV-1 / ENV-2 / ENV-3

## PG-002 — Repo 状态快照同步

**规则**：每次卡片收口或 push 后，developer 更新
  `docs/project-context/repo-state.md`

**理由**：跨会话/跨模型快速了解状态；减少重复 git 探索

**性质**：视图层，非权威；与 git 不一致时以 git 为准

**生效日期**：2026-09-25

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
