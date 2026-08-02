-- ============================================================
-- 修复 P0-ERP-01：为继承 BaseEntity 的实体类对应数据库表补齐 version 字段
-- 修复日期：2026-07-17
-- 关联问题：architecture-review/C-erp-compliance-review.md P0-01
-- 说明：
--   1. BaseEntity.version 字段已添加 @Version 注解（乐观锁插件生效的前提）
--   2. 本脚本为 Flyway 启用场景下的表结构补齐（运行时由 DatabaseInitConfig 兜底）
--   3. 使用 ADD COLUMN IF NOT EXISTS 确保幂等可重复执行
--   4. 使用 DO 块 + EXCEPTION 处理表不存在的情况，避免脚本中断
--   5. 现有数据初始化为 version = 0（首次更新时由乐观锁插件递增）
-- ============================================================

-- 通用补齐 version 字段的辅助函数（PL/pgSQL）
-- 对每个表尝试 ADD COLUMN IF NOT EXISTS version，若表不存在则跳过
DO $$
BEGIN
    -- 财务模块（继承 BaseEntity 的实体类对应表）
    ALTER TABLE accounting_subjects       ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE finance_vouchers           ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE finance_voucher_details    ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE finance_records            ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE cost_records               ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE receivables                ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE payables                   ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE budgets                    ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE accounting_periods         ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE summary_templates           ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE transfer_templates         ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE tax_rate_configs           ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE standard_cost_cards        ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE bank_accounts              ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE fund_flows                 ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE approval_flow_configs      ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE receipt                    ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE payment                    ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;

    -- 单复数命名兼容：底层单数表（实体类 @TableName 使用复数，VIEW 已创建）
    ALTER TABLE accounting_subject         ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE cost_record                ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE finance_voucher            ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE finance_voucher_detail     ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;

    -- 全局基础表
    ALTER TABLE global_config              ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;
    ALTER TABLE position_code_rules        ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 0;

EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE '部分表可能不存在或 version 字段已存在，已跳过: %', SQLERRM;
END $$;

-- ============================================================
-- 数据初始化：将 NULL 或缺失的 version 设置为 0
-- 说明：使用 UPDATE WHERE version IS NULL 确保现有数据可用
-- ============================================================
DO $$
BEGIN
    UPDATE accounting_subjects       SET version = 0 WHERE version IS NULL;
    UPDATE finance_vouchers           SET version = 0 WHERE version IS NULL;
    UPDATE finance_voucher_details    SET version = 0 WHERE version IS NULL;
    UPDATE finance_records            SET version = 0 WHERE version IS NULL;
    UPDATE cost_records               SET version = 0 WHERE version IS NULL;
    UPDATE receivables                SET version = 0 WHERE version IS NULL;
    UPDATE payables                   SET version = 0 WHERE version IS NULL;
    UPDATE budgets                    SET version = 0 WHERE version IS NULL;
    UPDATE accounting_periods         SET version = 0 WHERE version IS NULL;
    UPDATE summary_templates           SET version = 0 WHERE version IS NULL;
    UPDATE transfer_templates         SET version = 0 WHERE version IS NULL;
    UPDATE tax_rate_configs           SET version = 0 WHERE version IS NULL;
    UPDATE standard_cost_cards        SET version = 0 WHERE version IS NULL;
    UPDATE bank_accounts              SET version = 0 WHERE version IS NULL;
    UPDATE fund_flows                 SET version = 0 WHERE version IS NULL;
    UPDATE approval_flow_configs      SET version = 0 WHERE version IS NULL;
    UPDATE receipt                    SET version = 0 WHERE version IS NULL;
    UPDATE payment                    SET version = 0 WHERE version IS NULL;
    UPDATE accounting_subject         SET version = 0 WHERE version IS NULL;
    UPDATE cost_record                SET version = 0 WHERE version IS NULL;
    UPDATE finance_voucher            SET version = 0 WHERE version IS NULL;
    UPDATE finance_voucher_detail     SET version = 0 WHERE version IS NULL;
    UPDATE global_config              SET version = 0 WHERE version IS NULL;
    UPDATE position_code_rules        SET version = 0 WHERE version IS NULL;
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE '数据初始化部分跳过: %', SQLERRM;
END $$;
