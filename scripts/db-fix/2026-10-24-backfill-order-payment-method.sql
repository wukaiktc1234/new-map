-- ============================================================
-- orders.payment_method 回填（2026-10-24，可复放）
--
-- 背景
--   POS 支付主口径在 order_payment_records.payment_method（**新编码**）：
--     1现金 / 2微信支付 / 3支付宝 / 4银行卡 / 5余额支付
--   而 orders.payment_method 沿用**旧编码**（与 orders_legacy 同源）：
--     0微信支付 / 1支付宝 / 2现金 / 3银行卡 / 4余额支付
--   两套编码数字含义完全不同（1 在新编码是现金、在旧编码是支付宝），混用会把支付方式显示错。
--
-- 本脚本做什么
--   对「已支付（payment_status=2）且 orders.payment_method 为空」的订单，
--   取该订单最近一条支付记录的新编码，按映射回填成旧编码写入 orders.payment_method：
--     新1现金     → 旧2现金
--     新2微信支付 → 旧0微信支付
--     新3支付宝   → 旧1支付宝
--     新4银行卡   → 旧3银行卡
--     新5余额支付 → 旧4余额支付
--
-- 幂等
--   仅命中 payment_method IS NULL 的行；UPDATE 自带 CASE 映射，重复执行影响 0 行。
--
-- 用法：psql -h localhost -p 5432 -U postgres -d food_traceability -f <本文件>
-- ============================================================

WITH latest_record AS (
    SELECT DISTINCT ON (r.order_id)
           r.order_id,
           r.payment_method AS new_code
    FROM order_payment_records r
    WHERE r.deleted = 0
      AND r.payment_method IS NOT NULL
    ORDER BY r.order_id, r.payment_time DESC NULLS LAST, r.payment_id DESC
)
UPDATE orders o
SET payment_method = CASE l.new_code
                         WHEN 1 THEN 2   -- 现金
                         WHEN 2 THEN 0   -- 微信支付
                         WHEN 3 THEN 1   -- 支付宝
                         WHEN 4 THEN 3   -- 银行卡
                         WHEN 5 THEN 4   -- 余额支付
                     END,
    update_time = NOW()
FROM latest_record l
WHERE o.order_id = l.order_id
  AND o.payment_method IS NULL
  AND o.payment_status = 2
  AND o.deleted = 0
  AND l.new_code IN (1, 2, 3, 4, 5);

-- 自查：
-- SELECT count(*) FILTER (WHERE payment_method IS NULL) AS still_null,
--        count(*) AS paid_total
-- FROM orders WHERE payment_status = 2 AND deleted = 0;
