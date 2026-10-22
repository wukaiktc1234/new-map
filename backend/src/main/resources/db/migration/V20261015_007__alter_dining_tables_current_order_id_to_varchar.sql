-- ============================================================
-- 现场修复（2026-10-23）：dining_tables.current_order_id 类型对齐订单号（bigint → varchar(50)）
--
-- 背景：
--   canonical 订单表 orders.order_id 为 varchar（形如 O1792694767233），
--   而 dining_tables.current_order_id 一直是 bigint，导致：
--     · DiningTableNewMapper.lockTable(tableId, orderId)   —— current_order_id = 'O...' → 类型冲突
--     · DiningTableNewMapper.unlockTableByOrderId(orderId) —— 同上（此前仅靠 ::text 兜底避免 500）
--   实测该列全表为 NULL → 桌台锁定/解锁从未真正生效。
--
-- 关键前置：public.dining_table（单数）是一个**视图**，直接依赖 dining_tables.current_order_id，
--   因此必须先 DROP VIEW → ALTER COLUMN → 原样重建 VIEW（仅该列类型随基表变为 varchar）。
--
-- 注意：dining_table（单数）不是表而是视图；实体 DiningTable 走的是这条视图路径。
--       orders_legacy / dining_table 之类的遗留对象不在本迁移清理范围。
--
-- 幂等：列已是 varchar 时跳过整段；视图按定义重建。
-- 回退：DROP VIEW + ALTER COLUMN ... TYPE bigint USING NULLIF(current_order_id,'')::bigint + 重建视图
-- ============================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
         WHERE table_name = 'dining_tables'
           AND column_name = 'current_order_id'
           AND data_type <> 'character varying'
    ) THEN
        -- 1) 拆除依赖视图（DiningTable 实体使用的 dining_table 视图）
        DROP VIEW IF EXISTS public.dining_table;

        -- 2) 基表列类型对齐订单号
        ALTER TABLE dining_tables
            ALTER COLUMN current_order_id TYPE varchar(50)
            USING current_order_id::text;

        -- 3) 原样重建视图（列顺序/来源与拆除前完全一致）
        CREATE VIEW public.dining_table AS
            SELECT table_id,
                   table_code,
                   table_name,
                   area_id,
                   table_type,
                   seats_count,
                   min_people,
                   max_people,
                   status,
                   current_order_id,
                   qr_code,
                   sort_order,
                   create_time,
                   update_time,
                   deleted,
                   store_id
              FROM dining_tables;
    END IF;
END $$;

COMMENT ON COLUMN dining_tables.current_order_id IS '当前占用订单号（varchar，对齐 orders.order_id；锁桌/解锁使用）';

CREATE INDEX IF NOT EXISTS idx_dining_tables_current_order_id
    ON dining_tables (current_order_id);
