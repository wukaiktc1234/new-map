-- ============================================================
-- orders.payment_method 编码单轨化（2026-10-24，可复放，带备份）
--
-- 背景
--   本系统曾存在两套支付方式编码：
--     新编码 order_payment_records.payment_method：0未记录 1现金 2微信 3支付宝 4银行卡 5余额  ← 唯一权威
--     旧编码 orders.payment_method（源自 orders_legacy）：0微信 1支付宝 2现金 3银行卡 4余额
--   两套编码数字含义不同（1 在新编码是现金、在旧编码是支付宝），历史翻译代码反复出错。
--   本脚本把 orders.payment_method 全量迁移到**新编码**，此后只存在一套编码。
--
--   注意 0 的语义：新编码里 0 =「未记录」（历史补账单，渠道不可考），是合法值；
--   不能用默认值/兜底把它伪装成「现金」（曾有 4 单 ORD-C01/C02/C03、DEV-L206-TEST-001 被写成 1，已纠正）。
--
-- 执行顺序（每一步都幂等）
--   0) 备份表 orders_payment_method_backup_20261024(order_id PK, old_value, new_value, source, migrated_at)
--   1) 先把取值约束放宽到 0..5（必须先做：否则回填 0 会被旧约束 1..5 拦下）
--   2) 权威来源：有支付记录的订单 → 最新一条记录的 payment_method（0..5）覆盖，含纠正历史误映射
--   3) 无有效记录的订单 → 旧编码映射 0→2微信 / 1→3支付宝 / 2→1现金 / 3→4银行卡 / 4→5余额
--   4) 列默认值改为 NULL；列注释写明 0=未记录
--   5) 统计输出
--
-- 幂等：
--   · 第 2 步以「现值 != 记录值」为触发条件，并用 upsert 维护备份行，重复执行 UPDATE 0 行；
--   · 第 3 步以「备份表无该 order_id」为守卫，已映射过的订单不会被二次映射（否则 1→3 会越迁越错）；
--   · 约束「先删后建」，可反复执行。
--   空值：payment_method IS NULL 的订单保持 NULL，不瞎填。
--
-- 用法：psql -h localhost -p 5432 -U postgres -d food_traceability -f <本文件>
-- ============================================================

-- ---------- 0) 备份表 ----------
CREATE TABLE IF NOT EXISTS orders_payment_method_backup_20261024 (
    order_id    varchar(32) PRIMARY KEY,
    old_value   integer,
    new_value   integer,
    source      varchar(16) NOT NULL,          -- record = 按支付记录回填；legacy-map = 按旧编码映射
    migrated_at timestamp   NOT NULL DEFAULT NOW()
);

-- ---------- 1) 放宽取值约束到 0..5（0=未记录）；先删后建，可复放 ----------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_orders_payment_method_range'
          AND conrelid = 'orders'::regclass
    ) THEN
        ALTER TABLE orders DROP CONSTRAINT ck_orders_payment_method_range;
    END IF;
    ALTER TABLE orders
        ADD CONSTRAINT ck_orders_payment_method_range
        CHECK (payment_method IS NULL OR payment_method BETWEEN 0 AND 5);
END $$;

-- ---------- 2) 权威来源：order_payment_records 最新一条（合法新编码 0..5，其中 0=未记录） ----------
-- 用「现值 != 记录值」触发（而不是「备份表里没有」），确保早期被误映射成现金的 4 单能被纠正。
WITH latest_record AS (
    SELECT DISTINCT ON (r.order_id)
           r.order_id,
           r.payment_method AS new_value
    FROM order_payment_records r
    WHERE r.deleted = 0
      AND r.payment_method BETWEEN 0 AND 5
    ORDER BY r.order_id, r.payment_time DESC NULLS LAST, r.payment_id DESC
)
INSERT INTO orders_payment_method_backup_20261024 (order_id, old_value, new_value, source)
SELECT o.order_id, o.payment_method, l.new_value, 'record'
FROM orders o
JOIN latest_record l ON l.order_id = o.order_id
WHERE o.deleted = 0
  AND o.payment_method IS DISTINCT FROM l.new_value
ON CONFLICT (order_id) DO UPDATE
    SET old_value   = EXCLUDED.old_value,
        new_value   = EXCLUDED.new_value,
        source      = EXCLUDED.source,
        migrated_at = NOW();

UPDATE orders o
SET payment_method = b.new_value,
    update_time    = NOW()
FROM orders_payment_method_backup_20261024 b
WHERE b.order_id = o.order_id
  AND b.source = 'record'
  AND o.payment_method IS DISTINCT FROM b.new_value;

-- ---------- 3) 无有效记录：旧编码 → 新编码 ----------
WITH legacy_target AS (
    SELECT o.order_id, o.payment_method AS old_value,
           CASE o.payment_method
               WHEN 0 THEN 2   -- 微信
               WHEN 1 THEN 3   -- 支付宝
               WHEN 2 THEN 1   -- 现金
               WHEN 3 THEN 4   -- 银行卡
               WHEN 4 THEN 5   -- 余额
           END AS new_value
    FROM orders o
    WHERE o.deleted = 0
      AND o.payment_method IN (0, 1, 2, 3, 4)
      AND NOT EXISTS (
            SELECT 1 FROM order_payment_records r
            WHERE r.order_id = o.order_id
              AND r.deleted = 0
              AND r.payment_method BETWEEN 0 AND 5)
      AND NOT EXISTS (SELECT 1 FROM orders_payment_method_backup_20261024 b WHERE b.order_id = o.order_id)
)
INSERT INTO orders_payment_method_backup_20261024 (order_id, old_value, new_value, source)
SELECT order_id, old_value, new_value, 'legacy-map'
FROM legacy_target
WHERE new_value IS NOT NULL;

UPDATE orders o
SET payment_method = b.new_value,
    update_time    = NOW()
FROM orders_payment_method_backup_20261024 b
WHERE b.order_id = o.order_id
  AND b.source = 'legacy-map'
  AND o.payment_method IS DISTINCT FROM b.new_value;

-- ---------- 4) 列默认值与注释 ----------
-- 默认值改为 NULL：新单未支付时不该「默认成现金」；已支付时必须由 payNewOrder / quick-pay
-- 显式写入真实支付方式（0..5，0=未记录）。
ALTER TABLE orders ALTER COLUMN payment_method DROP DEFAULT;

COMMENT ON COLUMN orders.payment_method IS
    '支付方式：0未记录（历史补账，渠道不可考） 1现金 2微信 3支付宝 4银行卡 5余额；'
    '与 order_payment_records.payment_method 同一套编码（唯一真相源），禁止旧编码 0/2 混用；'
    '未支付订单为 NULL（不再默认成现金）';

-- ---------- 5) 统计输出 ----------
SELECT
    (SELECT count(*) FROM orders WHERE deleted = 0)                                              AS total_orders,
    (SELECT count(*) FROM orders_payment_method_backup_20261024 WHERE source = 'record')          AS backfilled_from_record,
    (SELECT count(*) FROM orders_payment_method_backup_20261024 WHERE source = 'legacy-map')      AS mapped_from_legacy,
    (SELECT count(*) FROM orders WHERE deleted = 0 AND payment_method IS NULL)                    AS null_values,
    (SELECT count(*) FROM orders WHERE deleted = 0 AND payment_method = 0)                        AS unrecorded_rows;
