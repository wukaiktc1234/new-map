-- ============================================================
-- 到货单质检/入库确认支持（7.1 主闭环断点修复）
-- 新增：质检结果/备注/确认时间
-- ============================================================

ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS quality_check_result INTEGER DEFAULT 0;
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS quality_check_remark VARCHAR(500);
ALTER TABLE purchase_arrivals ADD COLUMN IF NOT EXISTS confirm_time TIMESTAMP;

COMMENT ON COLUMN purchase_arrivals.quality_check_result IS '质检结果：0待检 1通过 2失败';
COMMENT ON COLUMN purchase_arrivals.quality_check_remark IS '质检备注';
COMMENT ON COLUMN purchase_arrivals.confirm_time IS '入库确认时间';
