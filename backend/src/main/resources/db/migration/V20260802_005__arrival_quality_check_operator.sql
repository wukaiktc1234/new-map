-- ============================================================
-- 到货单质检补充：质检人 / 质检时间
-- ============================================================

ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS quality_check_by BIGINT;
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS quality_check_time TIMESTAMP;

COMMENT ON COLUMN purchase_arrivals.quality_check_by IS '质检人ID（users.user_id）';
COMMENT ON COLUMN purchase_arrivals.quality_check_time IS '质检时间';
