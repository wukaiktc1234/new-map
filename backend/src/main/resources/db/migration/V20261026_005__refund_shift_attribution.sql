-- =====================================================================
-- V20261026_005__refund_shift_attribution.sql
-- N19-2 第一步（**只建表，不写业务逻辑**）
--   退款单 → 班次（shift）归属表：用于现金户对账时判断某笔退款落在哪个班次/哪张缴款单。
--   本轮不填数据、不写任何读写逻辑。
-- =====================================================================

CREATE TABLE IF NOT EXISTS refund_shift_attribution (
    id               bigint generated always as identity PRIMARY KEY,
    -- 一个退款单只归属一次（UNIQUE 兜底幂等）
    refund_record_id bigint       NOT NULL UNIQUE,
    shift_id         bigint       NOT NULL,
    slip_id          bigint,
    operator_id      bigint,
    reason           varchar(200),
    confirmed_at     timestamptz,
    update_time      timestamptz,
    version          int          DEFAULT 0
);

COMMENT ON TABLE  refund_shift_attribution                    IS 'N19-2：退款单 → 班次归属（现金户对账用）';
COMMENT ON COLUMN refund_shift_attribution.refund_record_id   IS '退款单号（order_refund_records.refund_id），唯一';
COMMENT ON COLUMN refund_shift_attribution.shift_id           IS '归属班次（pos_shifts.shift_id）';
COMMENT ON COLUMN refund_shift_attribution.slip_id            IS '关联缴款单（deposit_slips.slip_id），可空';
COMMENT ON COLUMN refund_shift_attribution.reason             IS '人工归属原因（自动归属时可为空）';

CREATE INDEX IF NOT EXISTS idx_refund_shift_attribution_shift
    ON refund_shift_attribution (shift_id);
