-- ============================================================
-- 收货确认单补充：货物条形码（扫码识别货物用）
-- ============================================================

ALTER TABLE receipt_confirmations ADD COLUMN IF NOT EXISTS barcode VARCHAR(64);
COMMENT ON COLUMN receipt_confirmations.barcode IS '货物条形码（商品码/外箱条码）';
