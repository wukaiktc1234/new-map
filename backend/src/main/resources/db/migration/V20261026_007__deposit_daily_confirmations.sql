-- =====================================================================
-- V20261026_007__deposit_daily_confirmations.sql
-- NEW-34 存行日汇总「人工确认」：只记录确认状态，**不锁定数据**。
--   · 允许重复确认（覆盖式追加，历史行全部保留）
--   · 确认后数据变化 → daily-summary 返回 confirmedStale=true + 上次快照，不自动失效
-- =====================================================================

CREATE TABLE IF NOT EXISTS deposit_daily_confirmations (
    id                      bigserial PRIMARY KEY,
    business_date           date        NOT NULL,
    account_id              bigint,
    confirmed_by            bigint,
    confirmed_by_name       varchar(100),
    confirmed_at            timestamptz NOT NULL DEFAULT now(),
    total_cash_counted_fen  bigint      NOT NULL DEFAULT 0,
    bank_inflow_fen         bigint,
    unmatched_count         integer     NOT NULL DEFAULT 0,
    snapshot_hash           varchar(64),
    snapshot_json           jsonb,
    create_time             timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_deposit_daily_confirmations_date
    ON deposit_daily_confirmations (business_date, account_id, confirmed_at DESC);

COMMENT ON TABLE deposit_daily_confirmations IS 'NEW-34：存行日汇总人工确认记录（覆盖式追加，保留历史；不锁定业务数据）';
