-- ============================================
-- 为门店库存表增加成本字段
-- 实现门店级别的成本核算，让库存成本真正流动起来
-- ============================================

-- 门店库存表增加单位成本和总成本字段
ALTER TABLE store_inventory ADD COLUMN IF NOT EXISTS unit_cost BIGINT DEFAULT 0;
COMMENT ON COLUMN store_inventory.unit_cost IS '单位成本（分）';

ALTER TABLE store_inventory ADD COLUMN IF NOT EXISTS total_cost BIGINT DEFAULT 0;
COMMENT ON COLUMN store_inventory.total_cost IS '总成本（分）';

-- 门店库存日志表增加成本字段
ALTER TABLE store_inventory_log ADD COLUMN IF NOT EXISTS unit_cost BIGINT DEFAULT 0;
COMMENT ON COLUMN store_inventory_log.unit_cost IS '单位成本（分）';

ALTER TABLE store_inventory_log ADD COLUMN IF NOT EXISTS total_cost_change BIGINT DEFAULT 0;
COMMENT ON COLUMN store_inventory_log.total_cost_change IS '成本变动（分，正数增加，负数减少）';
