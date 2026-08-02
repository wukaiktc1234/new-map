-- ============================================================
-- 付款单表增加来源字段（采购订单号、入库单号）
-- 创建日期：2026-07-19
-- 说明：补齐采购→入库→应付→付款链路上的合理联动字段，使付款记录
--       也能追溯到上游采购订单与入库单，便于对账与查询。
-- ============================================================

ALTER TABLE payment ADD COLUMN IF NOT EXISTS stockin_id BIGINT;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS stockin_no VARCHAR(32);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS order_no VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_payment_stockin_id ON payment(stockin_id);
CREATE INDEX IF NOT EXISTS idx_payment_order_no ON payment(order_no);

-- 回填历史付款单缺失的来源字段：根据关联的应付账款（payables）带出
UPDATE payment p
SET
    stockin_id = py.stockin_id,
    stockin_no = py.stockin_no,
    order_no   = py.order_no
FROM payables py
WHERE p.payable_id = py.payable_id
  AND p.stockin_id IS NULL
  AND py.stockin_id IS NOT NULL;
