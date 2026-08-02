-- ============================================================
-- V20260723_004: 为库存流水表增加仓库字段
-- ============================================================
-- 背景：
--   仓库收货确认时，InventoryService.recordTransaction 会记录 warehouse_id，
--   但旧版 inventory_transactions 表（V1.0.0.100）未包含该字段，导致仓库收货入库失败。
--   新增 warehouse_id 字段后，仓库维度库存变动可追溯。
--
-- 处理策略：
--   1. 新增 warehouse_id 字段（可空）
--   2. 创建索引优化按仓库查询
--   3. 添加字段注释
-- ============================================================

ALTER TABLE inventory_transactions ADD COLUMN IF NOT EXISTS warehouse_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_inventory_transactions_warehouse_id ON inventory_transactions (warehouse_id);

COMMENT ON COLUMN inventory_transactions.warehouse_id IS '仓库ID，记录库存变动发生的仓库';
