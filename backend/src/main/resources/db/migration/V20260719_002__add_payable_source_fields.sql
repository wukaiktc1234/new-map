ALTER TABLE payables ADD COLUMN IF NOT EXISTS stockin_id BIGINT;
ALTER TABLE payables ADD COLUMN IF NOT EXISTS stockin_no VARCHAR(32);
ALTER TABLE payables ADD COLUMN IF NOT EXISTS order_no VARCHAR(100);
CREATE INDEX IF NOT EXISTS idx_payables_stockin ON payables(stockin_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_payables_order_no ON payables(order_no) WHERE deleted = 0;
