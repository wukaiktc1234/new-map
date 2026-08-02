-- ============================================================
-- P2-1: 门店库存流水数量精度修复
-- store_inventory_log 的 before_stock/after_stock/change_quantity 原为 integer，
-- 无法记录小数数量（如 10.5kg）。改为 DECIMAL(12,3) 与 store_inventory.current_stock 一致。
-- 同时打通「采购入库 / 销售出库 / 调拨 / 退款回补」等主路径的门店库存流水写入。
-- ============================================================

ALTER TABLE store_inventory_log ALTER COLUMN before_stock TYPE DECIMAL(12,3);
ALTER TABLE store_inventory_log ALTER COLUMN after_stock TYPE DECIMAL(12,3);
ALTER TABLE store_inventory_log ALTER COLUMN change_quantity TYPE DECIMAL(12,3);
