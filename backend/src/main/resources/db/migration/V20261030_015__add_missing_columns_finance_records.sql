-- V20261030_015__add_missing_columns_finance_records.sql
--
-- P0「迁移体系与生产库脱钩」（Lead 2026-10-30）：补齐 finance_records 的 17 列。
-- 全部 ADD COLUMN IF NOT EXISTS（幂等）：主库那 17 列本来就存在 → no-op；干净库补齐。
--
-- 列定义逐个取自生产 information_schema.columns：
--   account_subject_id     | bigint
--   approval_status        | integer DEFAULT 0
--   approve_time           | timestamp without time zone
--   approve_user_id        | bigint
--   business_date          | date
--   counterparty_name      | character varying(100)
--   counterparty_type      | integer
--   create_user_id         | bigint
--   created_by             | character varying(100)
--   invoice_id             | bigint
--   payment_method         | integer
--   record_category        | integer NOT NULL
--   record_no              | character varying(32) NOT NULL
--   record_type            | integer NOT NULL
--   remark                 | text
--   updated_by             | character varying(100)
--   voucher_id             | bigint
--
-- ⚠ 三个 NOT NULL 列（record_category / record_no / record_type）走「先可空 → 断言无 NULL → SET NOT NULL」：
--   「有行但无此列」的环境 → 显式报错要求人工按业务语义回填，**禁止用固定值批量填充**
--   （生产实测 106 行该 3 列全非空，值域 record_category=101/205、record_type=1/2 —— 属业务码，不可由迁移臆造）。
--
-- ⚠ 断言只断言「17 列齐备」，**不断言总列数**：主库 32 列、干净库 41 列（含 9 个历史残留列），
--   写死任何总数都会在其中一边失败；总列数只在 NOTICE 里打印。

ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS account_subject_id bigint;
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS approval_status integer DEFAULT 0;
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS approve_time timestamp without time zone;
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS approve_user_id bigint;
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS business_date date;
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS counterparty_name character varying(100);
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS counterparty_type integer;
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS create_user_id bigint;
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS created_by character varying(100);
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS invoice_id bigint;
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS payment_method integer;
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS record_category integer;
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS record_no character varying(32);
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS record_type integer;
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS remark text;
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS updated_by character varying(100);
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS voucher_id bigint;

-- 单一 DO 块：NOT NULL 三段式 + 列齐备断言（合并成一段，减少 $$ 定界符数量，降低出错面）
DO $$
DECLARE
    n_null bigint;
    missing text;
    v_cnt bigint;
BEGIN
    -- ① NOT NULL 三段式：断言无 NULL → SET NOT NULL（不自己回填）
    SELECT count(*) INTO n_null FROM finance_records
     WHERE record_category IS NULL OR record_no IS NULL OR record_type IS NULL;
    IF n_null > 0 THEN
        RAISE EXCEPTION '需人工回填：finance_records 有 % 行缺 record_category/record_no/record_type，请按业务语义回填后重跑（禁止固定值批量填充）', n_null;
    END IF;
    ALTER TABLE finance_records ALTER COLUMN record_category SET NOT NULL;
    ALTER TABLE finance_records ALTER COLUMN record_no SET NOT NULL;
    ALTER TABLE finance_records ALTER COLUMN record_type SET NOT NULL;

    -- ② 环境无关断言：17 列必须齐备（不断言总列数）
    SELECT string_agg(c, ', ') INTO missing
      FROM unnest(ARRAY[
            'account_subject_id','approval_status','approve_time','approve_user_id','business_date',
            'counterparty_name','counterparty_type','create_user_id','created_by','invoice_id',
            'payment_method','record_category','record_no','record_type','remark',
            'updated_by','voucher_id']) AS c
     WHERE NOT EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_schema = 'public' AND table_name = 'finance_records'
                          AND column_name = c);
    IF missing IS NOT NULL THEN
        RAISE EXCEPTION '015 断言失败：finance_records 缺列 %', missing;
    END IF;

    SELECT count(*) INTO v_cnt FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'finance_records';
    RAISE NOTICE '015 OK：finance_records 17 列齐备（当前总列数=%）', v_cnt;
END $$;
