-- =====================================================================
-- V20261026_001__finance_period_close.sql
-- P4-4：期末结账 / 反结账（表 accounting_periods 已存在，不新建期间表）
--   · 追加 reopen_time / reopen_user_id / reopen_reason
--   · 新建 finance_period_account_balance（逐账户快照，结账时写入）
--   · 种子：当前自然月 2026-10（ON CONFLICT DO NOTHING，需 period_code 唯一）
-- =====================================================================

-- 1) 反结账字段
ALTER TABLE accounting_periods ADD COLUMN IF NOT EXISTS reopen_time   timestamp without time zone;
ALTER TABLE accounting_periods ADD COLUMN IF NOT EXISTS reopen_user_id bigint;
ALTER TABLE accounting_periods ADD COLUMN IF NOT EXISTS reopen_reason  varchar(500);

-- 2) period_code 唯一约束（已存在则跳过；结账/反结账都按 period_code 定位）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
          JOIN pg_class t ON t.oid = c.conrelid
         WHERE t.relname = 'accounting_periods' AND c.contype = 'u'
           AND pg_get_constraintdef(c.oid) LIKE '%(period_code)%'
    ) AND NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE tablename = 'accounting_periods' AND indexdef LIKE '%UNIQUE%period_code%'
    ) THEN
        ALTER TABLE accounting_periods ADD CONSTRAINT uk_accounting_periods_code UNIQUE (period_code);
    END IF;
END $$;

-- 3) 逐账户快照表
CREATE TABLE IF NOT EXISTS finance_period_account_balance (
    period_code       varchar(32)  NOT NULL,
    account_id        bigint       NOT NULL,
    opening_fen       bigint       NOT NULL DEFAULT 0,
    income_fen        bigint       NOT NULL DEFAULT 0,
    expense_fen       bigint       NOT NULL DEFAULT 0,
    closing_fen       bigint       NOT NULL DEFAULT 0,
    txn_count         bigint       NOT NULL DEFAULT 0,
    last_balance_after bigint,
    create_time       timestamp without time zone NOT NULL DEFAULT now(),
    create_user_id    bigint,
    CONSTRAINT pk_finance_period_account_balance PRIMARY KEY (period_code, account_id)
);

CREATE INDEX IF NOT EXISTS idx_fpab_period ON finance_period_account_balance (period_code);
CREATE INDEX IF NOT EXISTS idx_fpab_account ON finance_period_account_balance (account_id);

-- 4) 种子：当前自然月
INSERT INTO accounting_periods
    (period_code, period_name, period_type, start_date, end_date, status, is_closed,
     trial_balance_passed, create_time, update_time, created_by, deleted, version)
VALUES ('2026-10', '2026年10月', 1, '2026-10-01', '2026-10-31', 0, false,
        false, now(), now(), 'system', 0, 0)
ON CONFLICT (period_code) DO NOTHING;

INSERT INTO accounting_periods
    (period_code, period_name, period_type, start_date, end_date, status, is_closed,
     trial_balance_passed, create_time, update_time, created_by, deleted, version)
VALUES ('2026-09', '2026年9月', 1, '2026-09-01', '2026-09-30', 0, false,
        false, now(), now(), 'system', 0, 0)
ON CONFLICT (period_code) DO NOTHING;
