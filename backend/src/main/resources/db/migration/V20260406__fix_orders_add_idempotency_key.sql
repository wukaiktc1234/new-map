-- ============================================
-- BUG#1修复：orders表添加idempotency_key列（PostgreSQL兼容）
-- 问题描述：Order实体类有idempotencyKey字段，
--          但数据库缺少IDEMPOTENCY_KEY列导致订单创建报500错误
-- 执行时间：2026-04-06
-- ============================================

-- 添加幂等性键字段
ALTER TABLE orders
ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128) DEFAULT NULL;

COMMENT ON COLUMN orders.idempotency_key IS '幂等性键';
