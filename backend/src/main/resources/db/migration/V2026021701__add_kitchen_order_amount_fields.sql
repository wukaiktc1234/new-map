-- 添加kitchen_order表缺失的字段（PostgreSQL兼容）
-- 执行时间: 2026-02-17
-- 注意: V1.0.0.100__init_postgresql.sql 已包含这些字段，此处使用 IF NOT EXISTS 确保幂等

-- 添加订单总金额字段
ALTER TABLE kitchen_order
ADD COLUMN IF NOT EXISTS total_amount DECIMAL(12,2) DEFAULT 0.00;

-- 添加支付方式字段
ALTER TABLE kitchen_order
ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50) DEFAULT '现金';
