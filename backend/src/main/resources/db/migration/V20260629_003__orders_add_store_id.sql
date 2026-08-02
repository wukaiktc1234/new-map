-- ============================================================
-- 订单表增加门店ID字段
-- 修复 Critical 问题：orders 表无 store_id，订单无法关联门店
-- 关联 stores_new.store_id（BIGINT）
-- ============================================================

-- 1. 订单主表增加门店ID（nullable 以兼容历史数据，新订单必填由应用层保证）
ALTER TABLE orders ADD COLUMN IF NOT EXISTS store_id BIGINT;
COMMENT ON COLUMN orders.store_id IS '门店ID（关联 stores_new.store_id），NULL 表示历史订单未关联门店';

-- 2. 门店维度索引（支持按门店查询订单的常见场景）
CREATE INDEX IF NOT EXISTS idx_orders_store_id ON orders(store_id);

-- 3. 组合索引：门店 + 订单状态（运营报表按门店统计订单状态常用）
CREATE INDEX IF NOT EXISTS idx_orders_store_status ON orders(store_id, order_status);

-- 4. 组合索引：门店 + 创建时间（按门店查询今日订单常用）
CREATE INDEX IF NOT EXISTS idx_orders_store_create_time ON orders(store_id, create_time);
