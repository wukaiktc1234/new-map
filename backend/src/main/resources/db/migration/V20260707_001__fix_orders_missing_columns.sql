-- =====================================================
-- 修复生产环境 orders 表缺失列
-- 版本: V20260707_001
-- 说明: 兼容旧 orders 表结构，补齐订单核心字段
-- =====================================================

-- 订单主表核心金额/状态字段
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_code VARCHAR(32);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_type INTEGER DEFAULT 1;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_source INTEGER DEFAULT 1;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_id BIGINT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_name VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(20);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS table_id BIGINT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS table_name VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS dining_people_count INTEGER;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_status INTEGER DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status INTEGER DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS total_amount BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS discount_amount BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS coupon_amount BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS points_amount BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_fee BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS packaging_fee BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS final_amount BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS paid_amount BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS refund_amount BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS points_earned INTEGER DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS remark TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancel_reason TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_address TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS expected_time TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS actual_delivery_time TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cashier_user_id BIGINT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS create_user_id BIGINT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS deleted INTEGER DEFAULT 0;

-- 如果旧表使用 order_number 而非 order_code，则同步数据（兼容新库无该列的情况）
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'order_number'
    ) THEN
        UPDATE orders SET order_code = order_number WHERE order_code IS NULL AND order_number IS NOT NULL;
    END IF;
END $$;

-- 确保 order_code 唯一约束存在
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'uk_orders_order_code'
    ) THEN
        CREATE UNIQUE INDEX uk_orders_order_code ON orders(order_code);
    END IF;
END $$;
