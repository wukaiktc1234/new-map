-- =====================================================================
-- V20261026_006__deposit_slips_reserve.sql
-- 差异1 备用金：缴款单记录班次备用金（元→分）与「计现额」。
--   pos_shifts.opening_cash 是 numeric(10,2) 元；这里按分落库（MoneyYuanToFen 换算）。
--   cash_counted_fen = actual_amount − reserve_fen（计现额，备用金从实收里扣掉）
-- =====================================================================

ALTER TABLE deposit_slips
    ADD COLUMN IF NOT EXISTS reserve_fen bigint NOT NULL DEFAULT 0;

ALTER TABLE deposit_slips
    ADD COLUMN IF NOT EXISTS cash_counted_fen bigint;

COMMENT ON COLUMN deposit_slips.reserve_fen      IS '差异1：班次备用金（分，= pos_shifts.opening_cash 元×100）';
COMMENT ON COLUMN deposit_slips.cash_counted_fen IS '差异1：计现额（分）= actual_amount − reserve_fen';
