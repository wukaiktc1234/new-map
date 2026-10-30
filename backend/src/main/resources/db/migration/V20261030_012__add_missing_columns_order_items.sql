-- V20261030_012__add_missing_columns_order_items.sql
--
-- P0「迁移体系与生产库脱钩」（Lead 2026-10-30）：补齐 order_items 的 8 个列。
-- 全部使用 ADD COLUMN IF NOT EXISTS（幂等）：主库那 8 列本来就存在 → 执行即 no-op；干净库 12 → 20 列。
--
-- 列定义逐个取自生产 information_schema.columns（类型/长度/precision/is_nullable/default）：
--   amount          | bigint DEFAULT 0
--   combo_id        | character varying(32)
--   discount_amount | bigint DEFAULT 0
--   item_id         | character varying(32) NOT NULL
--   kitchen_status  | integer DEFAULT 0
--   product_name    | character varying(100) NOT NULL
--   product_type    | integer DEFAULT 1
--   remark          | character varying(500)
--
-- ⚠ 两个 NOT NULL 列（item_id / product_name）走「先可空 → 断言无 NULL → SET NOT NULL」：
--   干净库 0 行 → 直接过；主库列已存在且非空 → 直接过；
--   「有行但无此列」的环境 → 显式报错要求人工回填，**禁止用固定值批量填充**（财务/业务字段不许造假）。
--
-- ⚠ 断言只断言「8 列齐备」，**不断言总列数**：主库 17 列、干净库 20 列（含 3 个 legacy 残留列），
--   写死任何总数都会在其中一边失败；总列数只在 NOTICE 里打印，便于人工看差异。

ALTER TABLE order_items ADD COLUMN IF NOT EXISTS amount bigint DEFAULT 0;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS combo_id character varying(32);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS discount_amount bigint DEFAULT 0;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS item_id character varying(32);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS kitchen_status integer DEFAULT 0;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_name character varying(100);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_type integer DEFAULT 1;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS remark character varying(500);

-- ① NOT NULL 三段式：先可空（上面已建）→ 断言无 NULL → SET NOT NULL
DO $$
DECLARE n_null bigint;
BEGIN
    SELECT count(*) INTO n_null FROM order_items WHERE item_id IS NULL OR product_name IS NULL;
    IF n_null > 0 THEN
        RAISE EXCEPTION '需人工回填：order_items 有 % 行缺 item_id/product_name，请按业务语义回填后重跑（禁止固定值批量填充）', n_null;
    END IF;
    ALTER TABLE order_items ALTER COLUMN item_id SET NOT NULL;
    ALTER TABLE order_items ALTER COLUMN product_name SET NOT NULL;
END $$;

-- ② 环境无关断言：8 列必须齐备（不断言总列数）
DO $$
DECLARE missing text;
BEGIN
    SELECT string_agg(c, ', ') INTO missing
      FROM unnest(ARRAY['amount','combo_id','discount_amount','item_id','kitchen_status',
                        'product_name','product_type','remark']) AS c
     WHERE NOT EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_schema='public' AND table_name='order_items' AND column_name = c);
    IF missing IS NOT NULL THEN
        RAISE EXCEPTION '012 断言失败：order_items 缺列 %', missing;
    END IF;
    RAISE NOTICE '012 OK：order_items 8 列齐备（当前总列数=%）',
        (SELECT count(*) FROM information_schema.columns
          WHERE table_schema='public' AND table_name='order_items');
END $$;
