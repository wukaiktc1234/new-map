-- ============================================================
-- V20260701_003: 重新启用 order_items 外键约束
-- ============================================================
-- 修复 H2: order_items.order_id → orders.order_id 的外键约束
--          在 V6.0.0 迁移脚本中被注释掉，导致可能出现孤儿订单明细记录。
-- 本脚本在生产环境（PostgreSQL）中通过 Flyway 执行；
-- H2 开发环境由 DatabaseFixConfig.initializeOrdersTables() 处理。
-- 使用 DO $$ 块实现幂等，避免重复创建约束报错。
-- ============================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'order_items'
          AND constraint_name = 'fk_order_items_order_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        ALTER TABLE order_items
            ADD CONSTRAINT fk_order_items_order_id
            FOREIGN KEY (order_id) REFERENCES orders(order_id);
    END IF;
END $$;
