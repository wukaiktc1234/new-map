-- V20261030_013__add_missing_columns_payment.sql
--
-- P0「迁移体系与生产库脱钩」（Lead 2026-10-30）：生产 payment 比干净 Flyway 库多下列列，且无迁移。
-- 本迁移用 ADD COLUMN IF NOT EXISTS 补齐（幂等：主库已有列 → 不重复加）。
--
-- 列定义**逐个取自生产** information_schema.columns（data_type / length / precision / is_nullable / default）：
--   void_fund_flow_id|bigint
--   void_fund_flow_no|character varying(32)
--   void_remark|character varying(500)
--   void_time|timestamp without time zone
--   void_voucher_id|bigint
--   void_voucher_no|character varying(32)

ALTER TABLE payment ADD COLUMN IF NOT EXISTS void_fund_flow_id bigint;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS void_fund_flow_no character varying(32);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS void_remark character varying(500);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS void_time timestamp without time zone;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS void_voucher_id bigint;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS void_voucher_no character varying(32);

DO $$
DECLARE v_cnt bigint;
BEGIN
    SELECT count(*) INTO v_cnt FROM information_schema.columns
     WHERE table_schema='public' AND table_name='payment';
    IF v_cnt <> 35 THEN
        RAISE EXCEPTION 'V20261030_013__add_mis 断言失败：payment 列数=%（期望 35）', v_cnt;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public'
                   AND table_name='payment' AND column_name='void_fund_flow_id') THEN
        RAISE EXCEPTION 'V20261030_013__add_mis 断言失败：缺列 void_fund_flow_id';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public'
                   AND table_name='payment' AND column_name='void_time') THEN
        RAISE EXCEPTION 'V20261030_013__add_mis 断言失败：缺列 void_time';
    END IF;
    RAISE NOTICE 'V20261030_013__add_mis OK：payment 列数=%（含新增 6 列）', v_cnt;
END $$;
