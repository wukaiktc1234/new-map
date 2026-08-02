-- ============================================================
-- 为 table_reservations 表添加 remark 字段
-- 用于存储预约备注/特殊要求
-- ============================================================

ALTER TABLE table_reservations
    ADD COLUMN IF NOT EXISTS remark VARCHAR(500) DEFAULT '';

COMMENT ON COLUMN table_reservations.remark IS '预约备注/特殊要求';