-- 为采购入库单增加作废相关字段（作废非撤销，保留原记录用于溯源）
ALTER TABLE purchase_stockins ADD COLUMN IF NOT EXISTS void_time TIMESTAMP DEFAULT NULL;
ALTER TABLE purchase_stockins ADD COLUMN IF NOT EXISTS void_remark VARCHAR(500) DEFAULT NULL;
ALTER TABLE purchase_stockins ADD COLUMN IF NOT EXISTS void_by BIGINT DEFAULT NULL;
