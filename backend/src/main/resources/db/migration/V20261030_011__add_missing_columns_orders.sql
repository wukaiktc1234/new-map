-- V20261030_011__add_missing_columns_orders.sql
--
-- P0「迁移体系与生产库脱钩」（Lead 2026-10-30）：生产 orders 比干净 Flyway 库多下列列，且无迁移。
-- 本迁移用 ADD COLUMN IF NOT EXISTS 补齐（幂等：主库已有列 → 不重复加）。
--
-- 列定义**逐个取自生产** information_schema.columns（data_type / length / precision / is_nullable / default）：
--   call_number|character varying(8)
--   cashier_user_id|bigint
--   coupon_amount|bigint DEFAULT 0
--   create_user_id|bigint
--   customer_id|bigint
--   customer_name|character varying(50)
--   customer_phone|character varying(20)
--   customer_phone_encrypted|character varying(255)
--   delivery_fee|bigint DEFAULT 0
--   dining_people_count|integer
--   expected_time|timestamp without time zone
--   final_amount|bigint DEFAULT 0
--   order_code|character varying(32)
--   packaging_fee|bigint DEFAULT 0
--   paid_amount|bigint DEFAULT 0
--   payment_status|integer DEFAULT 0
--   points_amount|bigint DEFAULT 0
--   points_earned|integer DEFAULT 0
--   remark|text
--   shift_id|bigint
--   store_id|bigint
--   table_id|bigint
--   table_name|character varying(50)
--   total_amount|bigint DEFAULT 0
--   ★ 红线1：final_amount / total_amount / paid_amount / coupon_amount / points_amount 必须是 bigint（**分**），不是 numeric（元）
--   ★ 红线2：payment_status 必须是 integer

ALTER TABLE orders ADD COLUMN IF NOT EXISTS call_number character varying(8);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cashier_user_id bigint;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS coupon_amount bigint DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS create_user_id bigint;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_id bigint;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_name character varying(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_phone character varying(20);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_phone_encrypted character varying(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_fee bigint DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS dining_people_count integer;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS expected_time timestamp without time zone;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS final_amount bigint DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_code character varying(32);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS packaging_fee bigint DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS paid_amount bigint DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status integer DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS points_amount bigint DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS points_earned integer DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS remark text;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shift_id bigint;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS store_id bigint;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS table_id bigint;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS table_name character varying(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS total_amount bigint DEFAULT 0;

DO $$
DECLARE v_cnt bigint;
BEGIN
    SELECT count(*) INTO v_cnt FROM information_schema.columns
     WHERE table_schema='public' AND table_name='orders';
    IF v_cnt <> 54 THEN
        RAISE EXCEPTION 'V20261030_011__add_mis 断言失败：orders 列数=%（期望 54）', v_cnt;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public'
                   AND table_name='orders' AND column_name='final_amount') THEN
        RAISE EXCEPTION 'V20261030_011__add_mis 断言失败：缺列 final_amount';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public'
                   AND table_name='orders' AND column_name='total_amount') THEN
        RAISE EXCEPTION 'V20261030_011__add_mis 断言失败：缺列 total_amount';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public'
                   AND table_name='orders' AND column_name='paid_amount') THEN
        RAISE EXCEPTION 'V20261030_011__add_mis 断言失败：缺列 paid_amount';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public'
                   AND table_name='orders' AND column_name='payment_status') THEN
        RAISE EXCEPTION 'V20261030_011__add_mis 断言失败：缺列 payment_status';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public'
                   AND table_name='orders' AND column_name='shift_id') THEN
        RAISE EXCEPTION 'V20261030_011__add_mis 断言失败：缺列 shift_id';
    END IF;
    RAISE NOTICE 'V20261030_011__add_mis OK：orders 列数=%（含新增 24 列）', v_cnt;
END $$;
