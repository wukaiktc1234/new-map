-- ============================================================
-- 为 table_reservations 表添加 store_id 字段
-- 用于关联预约所属门店，支持按门店查证预约数据
-- ============================================================

ALTER TABLE table_reservations
    ADD COLUMN IF NOT EXISTS store_id BIGINT DEFAULT 1;

COMMENT ON COLUMN table_reservations.store_id IS '门店ID';

-- 为 store_id 创建索引，支持按门店查询
CREATE INDEX IF NOT EXISTS idx_table_reservations_store_id
    ON table_reservations (store_id);