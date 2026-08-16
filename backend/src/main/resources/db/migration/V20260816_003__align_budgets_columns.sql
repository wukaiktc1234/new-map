-- ============================================================
-- PD-016 裁决落地（OICBE-B1-003 / KL-055）：migration 为真相源，三步收敛
-- 架构裁决 2026-08-16：① 实体字段差异清单 → ② 影响接口扫描 → ③ migration 收敛对齐
--   补实体 9 列 + budget_id 类型对齐；不直接删字段（表-only 采购预算语义列保留）；
--   剩余差异登记任务池/差异表（见 oic-be-task-board.md §四·1）
-- 三向核对（2026-08-16 information_schema 只读实测）：
--   - 实体：entity/finance/Budget.java @TableName("budgets")，budget_id Long(IdType.AUTO)
--     + 9 列实体-only：budget_year/budget_month/budget_type/category_id/actual_amount/
--       variance/variance_rate/responsible_dept_id/remark（表中实测缺失）
--   - 真实表：budgets 15 列（V20260704_001 旧采购预算结构），行数=0（类型变更安全）
--   - budget_id 类型漂移：VARCHAR(32) PK vs 实体 Long（本脚本对齐为 BIGINT）
-- 类型推断（按实体字段 + 既有表惯例标注）：
--   budget_year INTEGER / budget_month INTEGER / budget_type INTEGER / category_id INTEGER
--   actual_amount BIGINT（实体 Long，单位分）/ variance BIGINT（单位分）
--   variance_rate NUMERIC(10,4)（BigDecimal，差异率%，实现保留 4 位小数）
--   responsible_dept_id BIGINT / remark VARCHAR(500)（DTO Size max=500）
-- 幂等：ADD COLUMN IF NOT EXISTS；budget_id 变更需 drop PK → ALTER TYPE → 重建 PK
--   （显式语句：PK 约束名按 PostgreSQL 内联 PRIMARY KEY 默认名 budgets_pkey）
-- 不删字段：budget_name/used_amount/remaining_amount/budget_period/status/create_by/update_by
--   保留不删（采购预算语义列，差异登记任务池）
-- ============================================================

-- 1. 补实体-only 9 列（结构护栏 BudgetServiceImpl.BUDGET_ENTITY_MAPPED_COLUMNS 校验面，补齐后自动放行）
ALTER TABLE budgets
    ADD COLUMN IF NOT EXISTS budget_year          INTEGER,
    ADD COLUMN IF NOT EXISTS budget_month         INTEGER,
    ADD COLUMN IF NOT EXISTS budget_type          INTEGER,
    ADD COLUMN IF NOT EXISTS category_id          INTEGER,
    ADD COLUMN IF NOT EXISTS actual_amount        BIGINT,
    ADD COLUMN IF NOT EXISTS variance             BIGINT,
    ADD COLUMN IF NOT EXISTS variance_rate        NUMERIC(10,4),
    ADD COLUMN IF NOT EXISTS responsible_dept_id  BIGINT,
    ADD COLUMN IF NOT EXISTS remark               VARCHAR(500);

COMMENT ON COLUMN budgets.budget_year IS '预算年份（财务预算实体）';
COMMENT ON COLUMN budgets.budget_month IS '预算月份（1-12，NULL=年度预算）';
COMMENT ON COLUMN budgets.budget_type IS '预算类型: 1-收入预算 2-成本预算 3-费用预算 4-利润预算 5-现金流预算';
COMMENT ON COLUMN budgets.category_id IS '细分科目/类别ID';
COMMENT ON COLUMN budgets.actual_amount IS '实际金额（单位：分）';
COMMENT ON COLUMN budgets.variance IS '差异=预算-实际（单位：分）';
COMMENT ON COLUMN budgets.variance_rate IS '差异率%（保留 4 位小数）';
COMMENT ON COLUMN budgets.responsible_dept_id IS '责任部门ID';
COMMENT ON COLUMN budgets.remark IS '备注';

-- 2. budget_id 类型对齐：VARCHAR(32) → BIGINT（表行数=0，无存量转换风险；
--    drop PK → ALTER TYPE → 重建 PK，PK 约束名 budgets_pkey）
ALTER TABLE budgets DROP CONSTRAINT IF EXISTS budgets_pkey;
ALTER TABLE budgets ALTER COLUMN budget_id TYPE BIGINT USING budget_id::bigint;
ALTER TABLE budgets ADD CONSTRAINT budgets_pkey PRIMARY KEY (budget_id);

COMMENT ON COLUMN budgets.budget_id IS '预算ID（实体 Long IdType.AUTO，类型对齐 BIGINT）';
