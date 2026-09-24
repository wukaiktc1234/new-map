# Process Guards

## PG-001 — 开卡前工作区清洁检查

**规则**：
在开始任何新任务卡（Task Card）的实施前，developer 必须确认：
  1. 当前 git 工作区 clean（无 uncommitted 改动）
  2. 或现有 WIP 已提交到独立分支
  3. 或现有 WIP 已 git stash 并在新卡结束后恢复

**理由**：
  ENV-1（FOODID 卡）与 ENV-2（combo 卡）同源——
  developer 在开始新卡时未清理工作区 WIP，导致：
  - 单卡 diff 无法用 git 隔离
  - WIP 被误提交进新卡的 commit
  - 回滚时波及其他批次

**违规处置**：
  - 首次：本卡按 ENV 登记，不豁免
  - 连续两次：冻结新卡启动，直到工作区清洁

**执行方式**：
  任务卡模板中增加前置声明：
  "工作区状态：CLEAN / 已提交到独立分支 / 已 stash"
  若为后者，需列出 stash 内容与其他卡的任务 ID

**生效日期**：2026-09-24
**关联**：ENV-1 / ENV-2
