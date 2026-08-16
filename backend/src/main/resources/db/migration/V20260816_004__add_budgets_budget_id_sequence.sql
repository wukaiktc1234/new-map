-- ============================================================
-- V20260816_003 补充修复：budgets.budget_id 自增语义（BIGSERIAL 惯例）
-- 背景：V20260816_003 将 budget_id VARCHAR(32) → BIGINT 并对齐 PK，
--       但未补自增默认值——实体 finance/Budget.budgetId 为 IdType.AUTO，
--       MyBatis-Plus 插入时 INSERT 不含 budget_id，依赖 DB 默认值；
--       无默认值 → NOT NULL violation（冒烟实测：null value in column "budget_id"）
-- 处置：CREATE SEQUENCE + SET DEFAULT nextval（等价 BIGSERIAL，与既有表
--       account_balance.id BIGSERIAL PRIMARY KEY 同惯例）；幂等 IF NOT EXISTS
-- 影响面：仅 budgets.budget_id 默认值；无存量数据（表行数=0）
-- 归属：PD-016（OICBE-B1-003）budget_id 类型对齐的必要完成面
-- ============================================================

CREATE SEQUENCE IF NOT EXISTS budgets_budget_id_seq;

ALTER TABLE budgets ALTER COLUMN budget_id SET DEFAULT nextval('budgets_budget_id_seq');

COMMENT ON COLUMN budgets.budget_id IS '预算ID（实体 Long IdType.AUTO，BIGINT 自增，对齐 BIGSERIAL 惯例）';
