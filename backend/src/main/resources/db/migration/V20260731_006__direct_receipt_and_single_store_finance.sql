-- ============================================================
-- SR-1: 收货确认单支持「无单直收」（单店模式）
--  - receipt_confirmations 新增 receipt_source 区分到货确认/无单直收
--  - arrival_id / order_id 允许为空（直收单无上游采购单据）
--  - receipt_confirmation_items 的 arrival_item_id / order_item_id 允许为空
-- SR-5: centralized-single 模式核心域增加 finance（单店也需要财务）
-- ============================================================

-- SR-1: receipt_confirmations 支持直收
ALTER TABLE receipt_confirmations ALTER COLUMN arrival_id DROP NOT NULL;
ALTER TABLE receipt_confirmations ALTER COLUMN order_id DROP NOT NULL;
ALTER TABLE receipt_confirmations ADD COLUMN IF NOT EXISTS receipt_source VARCHAR(20) NOT NULL DEFAULT 'arrival';
COMMENT ON COLUMN receipt_confirmations.receipt_source IS '收货来源：arrival-到货确认 / direct-无单直收';
COMMENT ON COLUMN receipt_confirmations.arrival_id IS '关联到货单ID（无单直收可为空）';
COMMENT ON COLUMN receipt_confirmations.order_id IS '关联采购订单ID（无单直收可为空）';

-- SR-1: receipt_confirmation_items 支持直收明细
ALTER TABLE receipt_confirmation_items ALTER COLUMN arrival_item_id DROP NOT NULL;
ALTER TABLE receipt_confirmation_items ALTER COLUMN order_item_id DROP NOT NULL;
COMMENT ON COLUMN receipt_confirmation_items.arrival_item_id IS '关联到货明细ID（无单直收可为空）';
COMMENT ON COLUMN receipt_confirmation_items.order_item_id IS '关联采购订单明细ID（无单直收可为空）';

-- SR-5: centralized-single 模板核心域增加 finance（admin/owner/finance_director）
UPDATE permission_templates
SET role_config = '{"admin":["workspace","store-ops","product","order","operations","member","traceability","device","system","finance"],"owner":["workspace","store-ops","product","order","operations","member","traceability","device","system","finance"],"hr_director":["workspace","hr"],"finance_director":["workspace","finance"],"ops_director":["workspace","store-ops","order","operations"],"employee":["workspace"]}',
    update_time = CURRENT_TIMESTAMP
WHERE code = 'centralized-single';