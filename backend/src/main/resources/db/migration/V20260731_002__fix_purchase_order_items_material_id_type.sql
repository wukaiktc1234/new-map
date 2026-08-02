-- ============================================================
-- V20260731_002：统一 purchase_order_items.material_id 为 BIGINT
-- 背景：
--   实体 PurchaseOrderItem.materialId 为 Long，而数据库表
--   purchase_order_items.material_id 初始为 VARCHAR(50)。
--   这导致按物料 ID 查询采购追溯链路时抛出
--   "操作符不存在: character varying = bigint"。
-- 操作：
--   1. 检查当前字段类型，仅在仍为 character varying 时执行转换
--   2. 使用安全 CAST：数值字符串转 BIGINT，非数值字符串转 NULL
--   3. 重建 material_id 索引
-- 兼容性：
--   本脚本使用 H2 / PostgreSQL 兼容语法，Flyway 仅执行一次。
-- ============================================================

DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'purchase_order_items' AND column_name = 'material_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE purchase_order_items ALTER COLUMN material_id TYPE BIGINT
            USING CASE
                WHEN material_id ~ '^[0-9]+$' THEN material_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 purchase_order_items.material_id 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 purchase_order_items.material_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- 重建索引
DROP INDEX IF EXISTS idx_purchase_order_items_material_id;
CREATE INDEX IF NOT EXISTS idx_purchase_order_items_material_id ON purchase_order_items (material_id);
