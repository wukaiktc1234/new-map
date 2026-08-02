-- ============================================================
-- 生产环境订单子表 schema 修复脚本
-- 版本: V20260707_005
-- 说明: 对齐 order_items/order_payment_records/order_refund_records 与实体定义
-- ============================================================

-- ------------------------------------------------------
-- 1. 修复 order_items 表字段，与 OrderItemNew 实体对齐
-- ------------------------------------------------------

-- 主键列重命名: order_item_id -> item_id
-- 幂等保护：仅在源列存在时才重命名（避免重复执行报错）
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'order_items' AND column_name = 'order_item_id'
    ) THEN
        ALTER TABLE order_items RENAME COLUMN order_item_id TO item_id;
    END IF;
END $$;

-- 商品名称重命名: food_name -> product_name
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'order_items' AND column_name = 'food_name'
    ) THEN
        ALTER TABLE order_items RENAME COLUMN food_name TO product_name;
    END IF;
END $$;

-- 金额重命名并转换为分(int8)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'order_items' AND column_name = 'subtotal_amount'
    ) THEN
        ALTER TABLE order_items RENAME COLUMN subtotal_amount TO amount;
    END IF;
END $$;
-- 幂等保护：仅当列类型非 bigint 时才执行 *100 转换
-- 若已是 bigint（已转为分），跳过避免溢出
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'order_items' AND column_name = 'amount'
          AND data_type != 'bigint'
    ) THEN
        ALTER TABLE order_items ALTER COLUMN amount TYPE BIGINT USING COALESCE((amount * 100)::bigint, 0);
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'order_items' AND column_name = 'unit_price'
          AND data_type != 'bigint'
    ) THEN
        ALTER TABLE order_items ALTER COLUMN unit_price TYPE BIGINT USING COALESCE((unit_price * 100)::bigint, 0);
    END IF;
END $$;

-- 补齐实体所需字段
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_type INTEGER DEFAULT 1;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS combo_id VARCHAR(32);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS discount_amount BIGINT DEFAULT 0;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS remark VARCHAR(500);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS kitchen_status INTEGER DEFAULT 0;

-- 为存量数据设置合理的 product_type（有 food_id 视为单品）
UPDATE order_items SET product_type = 1 WHERE product_type IS NULL;

-- 重建外键约束（order_id 类型已与 orders.order_id 一致为 VARCHAR）
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS fk_order_items_order_id;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_order_id FOREIGN KEY (order_id) REFERENCES orders(order_id);

-- 确保索引存在
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);

-- ------------------------------------------------------
-- 2. 修复 order_payment_records 表 order_id 类型
-- ------------------------------------------------------
ALTER TABLE order_payment_records ALTER COLUMN order_id TYPE VARCHAR(32);

-- ------------------------------------------------------
-- 3. 修复 order_refund_records 表 order_id 类型
-- ------------------------------------------------------
ALTER TABLE order_refund_records ALTER COLUMN order_id TYPE VARCHAR(32);
