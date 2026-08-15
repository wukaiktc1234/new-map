-- ============================================================
-- V20260802_004: 采购订单终止/冻结状态支持（状态 8=已终止 9=已冻结）
-- 背景:
--   异常场景处置定案（docs/order-termination-design.md，2026-08-02）：
--   终止 = 不可逆收尾（合作终止/资金断裂/质量异常等），
--   冻结 = 可逆暂停（案件查扣/监管冻结，止付止收货，解冻后恢复原执行状态）。
-- 设计:
--   1. frozen_from_status: 冻结前状态快照，解冻时恢复原状态；
--   2. freeze_reason/freeze_time/freeze_user_id: 冻结留痕；
--   3. terminate_reason/terminate_time/terminate_user_id: 终止留痕。
-- ============================================================

ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS frozen_from_status INT;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS freeze_reason      VARCHAR(500);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS freeze_time        TIMESTAMP;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS freeze_user_id     BIGINT;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS terminate_reason   VARCHAR(500);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS terminate_time     TIMESTAMP;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS terminate_user_id  BIGINT;

COMMENT ON COLUMN purchase_orders.frozen_from_status IS '冻结前状态快照（解冻时恢复，状态码 0-7）';
COMMENT ON COLUMN purchase_orders.freeze_reason      IS '冻结原因';
COMMENT ON COLUMN purchase_orders.freeze_time        IS '冻结时间';
COMMENT ON COLUMN purchase_orders.freeze_user_id     IS '冻结操作人ID';
COMMENT ON COLUMN purchase_orders.terminate_reason   IS '终止原因';
COMMENT ON COLUMN purchase_orders.terminate_time     IS '终止时间';
COMMENT ON COLUMN purchase_orders.terminate_user_id  IS '终止操作人ID';
