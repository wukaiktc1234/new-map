-- ============================================================
-- 商品档案扩展列（#2 设计确认）
-- 新增：条码 / 产地 / 保质期 / 存储条件（食品业务需要，持久化）
-- 移除方案：采购价/零售价/会员价/初始库存 不落库（归属其他模块）；
--           库存预警值 由 store_inventory.safety_stock 承担
-- ============================================================

ALTER TABLE material_archives ADD COLUMN IF NOT EXISTS barcode VARCHAR(100);
ALTER TABLE material_archives ADD COLUMN IF NOT EXISTS origin VARCHAR(200);
ALTER TABLE material_archives ADD COLUMN IF NOT EXISTS shelf_life VARCHAR(100);
ALTER TABLE material_archives ADD COLUMN IF NOT EXISTS storage_condition VARCHAR(100);

COMMENT ON COLUMN material_archives.barcode IS '条码';
COMMENT ON COLUMN material_archives.origin IS '产地';
COMMENT ON COLUMN material_archives.shelf_life IS '保质期（如：12个月）';
COMMENT ON COLUMN material_archives.storage_condition IS '存储条件（如：冷藏/冷冻/常温）';
