-- =====================================================================
-- V20261026_004__finance_cash_recon_columns.sql
-- N19-2 第一步（**只加字段与索引，不写任何业务逻辑**）
--   现金户对账口径所需的结构位：缴款单拆分字段 + 退款流水的时间戳。
--   本轮不填空值、不写配对、不动 PosRefundFlowRecorder。
-- =====================================================================

-- 1) deposit_slips：退款构成（分）与净差异（分）
--    refund_component_fen：本次存行里属于"退款/冲回"的金额，参与现金户勾稽扣减
--    difference_net_fen  ：扣除退款构成后的净差异（可正可负，未确认时允许 NULL）
ALTER TABLE deposit_slips
    ADD COLUMN IF NOT EXISTS refund_component_fen bigint NOT NULL DEFAULT 0;

ALTER TABLE deposit_slips
    ADD COLUMN IF NOT EXISTS difference_net_fen bigint;

COMMENT ON COLUMN deposit_slips.refund_component_fen IS 'N19-2：本次存行中属于退款/冲回的金额（分），默认 0';
COMMENT ON COLUMN deposit_slips.difference_net_fen  IS 'N19-2：扣除退款构成后的净差异（分），未确认时为 NULL';

-- 2) fund_flows：退款发生时间（业务日期是"哪一天"，这里是"哪一刻"）
ALTER TABLE fund_flows
    ADD COLUMN IF NOT EXISTS refund_time timestamptz;

COMMENT ON COLUMN fund_flows.refund_time IS 'N19-2：退款流水的时间戳（仅 flow_category=2 有意义，用于配对/归属判定）';

-- 3) 退款流水按 (account_id, refund_time) 的检索索引（只覆盖退款类别未删除行）
CREATE INDEX IF NOT EXISTS idx_fund_flows_refund_time
    ON fund_flows (account_id, refund_time)
    WHERE flow_category = 2 AND deleted = 0;
