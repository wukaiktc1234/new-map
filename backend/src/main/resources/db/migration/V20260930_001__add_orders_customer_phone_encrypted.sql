-- =====================================================
-- 新增 orders.customer_phone_encrypted（顾客电话可还原密文）
-- 版本: V20260930_001
-- 说明: P1-POS-CUSTOMER-PHONE-500-001（A1 专用字段·可还原）
--       customer_phone 存脱敏值（varchar(20) 放得下，修复 POS 下单带电话 500）；
--       customer_phone_encrypted 存 AES 密文（ENC_PHONE:<base64>，可还原，供授权回拨/隐私分级）。
--       只增不改（PG-003）；不回填存量数据（回滚 = 代码 revert + 下方 DROP）。
-- 回滚（手动，不入库执行）:
--   ALTER TABLE orders DROP COLUMN IF EXISTS customer_phone_encrypted;
-- =====================================================

ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_phone_encrypted VARCHAR(255);

COMMENT ON COLUMN orders.customer_phone_encrypted IS '顾客电话密文（ENC_PHONE:<base64>，AES/CBC IV+密文 Base64，可还原），供授权回拨/隐私分级使用';
