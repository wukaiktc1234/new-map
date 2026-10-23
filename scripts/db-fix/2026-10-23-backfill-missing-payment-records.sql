-- ============================================================
-- 2026-10-23-backfill-missing-payment-records.sql
-- 目的（C5）：历史单「未知支付方式」安全回填
--   现象：部分订单 payment_status=2（已支付），但 order_payment_records 里
--         没有任何支付记录 → POS 交班/资金拆解出现「各方式合计 ≠ 总额」的差额，
--         以前被塞进 unrecorded 桶（PosAuthServiceImpl.countShiftOrders 兜底）。
--   实测：4 笔（2026-08-08 / 2026-09-09 测试夹具），合计 333000 分 = 3330.00 元。
--
-- 原则：不做臆测。
--   1) 只插入 payment_method = 0，语义「未记录」。绝不猜测成现金/微信等具体渠道。
--   2) order_payment_records.payment_method 是 integer NOT NULL，无 CHECK 约束，
--      0 可以落库；该表枚举口径为 1现金 2微信 3支付宝 4银行卡 5积分 6混合支付，
--      0 不占用任何渠道值，展示层已把 0 映射为「未记录」
--      （OrderNewServiceImpl.getPaymentMethodName: case 0 -> "未记录"）。
--      注：orders.payment_method 另有旧口径（0-微信 1-支付宝 2-现金 3-银行卡），
--      但那是 orders 表自己的列，本脚本不读、不写该列，避免两套枚举混用。
--   3) 金额取 final_amount，为 0/空时取 paid_amount；两者都为 0 的订单跳过
--      （0 元记录没有对账价值，且会污染笔数）。
--   4) payment_time 取 orders.payment_time，为空时退 create_time（不臆造时间）。
--   5) remark 标明来源，便于审计与二次核对。
--
-- 可重复执行：插入前 NOT EXISTS 校验（同订单已有未删除记录就不再插），
--             重复跑插入 0 行。
-- 用法：psql -h localhost -U postgres -d food_traceability -f 本文件
-- 回滚：
--   DELETE FROM order_payment_records
--    WHERE payment_method = 0
--      AND remark LIKE '历史数据回填（支付方式未知）%';
-- ============================================================

BEGIN;

-- 回填前快照：待回填订单
SELECT o.order_id,
       o.order_number,
       COALESCE(NULLIF(o.final_amount, 0), o.paid_amount) AS amount_cents,
       COALESCE(o.payment_time, o.create_time)            AS pay_time
  FROM orders o
 WHERE o.payment_status = 2
   AND COALESCE(o.deleted, 0) = 0
   AND COALESCE(NULLIF(o.final_amount, 0), o.paid_amount, 0) > 0
   AND NOT EXISTS (
         SELECT 1 FROM order_payment_records r
          WHERE r.order_id = o.order_id
            AND COALESCE(r.deleted, 0) = 0)
 ORDER BY o.create_time;

WITH missing AS (
    SELECT o.order_id,
           COALESCE(NULLIF(o.final_amount, 0), o.paid_amount) AS amount_cents,
           COALESCE(o.payment_time, o.create_time)            AS pay_time
      FROM orders o
     WHERE o.payment_status = 2
       AND COALESCE(o.deleted, 0) = 0
       AND COALESCE(NULLIF(o.final_amount, 0), o.paid_amount, 0) > 0
       AND NOT EXISTS (
             SELECT 1 FROM order_payment_records r
              WHERE r.order_id = o.order_id
                AND COALESCE(r.deleted, 0) = 0)
)
INSERT INTO order_payment_records (order_id, payment_method, payment_amount, payment_time, operator_id, remark)
SELECT m.order_id,
       0,
       m.amount_cents,
       COALESCE(m.pay_time, CURRENT_TIMESTAMP),
       NULL,
       '历史数据回填（支付方式未知）：payment_status=2 但无支付记录，金额取 final_amount/paid_amount，渠道不做猜测'
  FROM missing m;

-- 回填后核对：仍有「已支付但无记录」的订单应为 0 行
SELECT COUNT(*) AS still_missing
  FROM orders o
 WHERE o.payment_status = 2
   AND COALESCE(o.deleted, 0) = 0
   AND NOT EXISTS (
         SELECT 1 FROM order_payment_records r
          WHERE r.order_id = o.order_id
            AND COALESCE(r.deleted, 0) = 0);

COMMIT;
