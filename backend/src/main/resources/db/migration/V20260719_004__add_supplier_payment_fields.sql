-- ============================================================
-- 为 suppliers 表增加结算方式与账期字段
-- 创建时间：2026-07-19
-- 用途：
--   1. 支持按供应商默认结算方式预填充付款单付款方式；
--   2. 支持按供应商账期自动计算应付账款到期日，替代硬编码 30 天。
-- 兼容 H2 (MODE=PostgreSQL) + PostgreSQL 18
-- ============================================================

ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS settlement_method VARCHAR(20) DEFAULT 'monthly';
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS payment_terms     INTEGER      DEFAULT 30;

COMMENT ON COLUMN suppliers.settlement_method IS '默认结算方式：monthly(月结)/immediate(现结)/prepaid(预付)/check(支票)/bank_transfer(银行转账)';
COMMENT ON COLUMN suppliers.payment_terms     IS '默认账期天数';

-- 为现有数据设置默认值，避免 NULL 导致付款方式/账期缺失
UPDATE suppliers SET settlement_method = COALESCE(settlement_method, 'monthly') WHERE settlement_method IS NULL;
UPDATE suppliers SET payment_terms = COALESCE(payment_terms, 30) WHERE payment_terms IS NULL;
