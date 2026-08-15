-- 供应商档案扩展字段（与前端商品/供应商档案表单对齐）
-- 主要供应商来源：商品档案从供应商档案中选择主要供应商，此处补齐供应商可供应目录所需字段
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS short_name VARCHAR(50);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS unified_social_code VARCHAR(50);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS supplier_type VARCHAR(30);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS supplier_level VARCHAR(20);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS industry VARCHAR(50);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS registered_capital NUMERIC(15,2);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS establish_date DATE;
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS supply_categories VARCHAR(500);
COMMENT ON COLUMN suppliers.supply_categories IS '供货品类（逗号分隔，如：蔬菜,肉类,粮油）';
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS min_order_quantity NUMERIC(15,2);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS delivery_area VARCHAR(200);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS email VARCHAR(100);
COMMENT ON COLUMN suppliers.email IS '联系邮箱';

-- 收货确认明细：按商品记录各自质检结果（合格1 / 不合格2 / 部分合格3）
ALTER TABLE receipt_confirmation_items ADD COLUMN IF NOT EXISTS quality_check_result INTEGER;
COMMENT ON COLUMN receipt_confirmation_items.quality_check_result IS '质检结果（1-合格 2-不合格 3-部分合格）';