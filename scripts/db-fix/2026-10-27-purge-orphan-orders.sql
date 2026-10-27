-- ============================================================================
-- 2026-10-27 清理「无退款、无缴款单、无 tag」的孤儿订单（**只准备，不执行**）
-- 来源：做 bug-③（orders.idempotency_key 不落库）实验时下的单，无退款 → 按 tag 无法定位
-- 判定：orders.deleted=0 ∧ 无 order_refund_records ∧ 无关联缴款单 ∧ 无 tag 痕迹
--       ∧ create_time 落在实验窗口（2026-10-27 16:40 ~ 17:10）
-- 安全：只 DELETE orders/order_items/order_payment_records/kitchen_order 中该订单的行；
--       不碰 fund_flows（本单无流水）；DELETE 段默认注释。
-- ============================================================================
\encoding UTF8
\pset pager off

\echo '===== [DRY-RUN] 命中明细 ====='
SELECT o.order_id, o.order_number, o.create_time,
       (SELECT count(*) FROM order_items i WHERE i.order_id = o.order_id) AS items,
       (SELECT count(*) FROM order_payment_records p WHERE p.order_id = o.order_id) AS payments,
       (SELECT count(*) FROM order_refund_records r WHERE r.order_id = o.order_id) AS refunds
  FROM orders o
 WHERE o.deleted = 0
   AND o.create_time >= '2026-10-27 16:40:00'
   AND o.create_time <  '2026-10-27 17:10:00'
   AND o.idempotency_key IS NULL
   AND NOT EXISTS (SELECT 1 FROM order_refund_records r WHERE r.order_id = o.order_id)
   AND NOT EXISTS (SELECT 1 FROM fund_flows f WHERE f.order_id = o.order_id)
   AND NOT EXISTS (SELECT 1 FROM deposit_slips d
                    WHERE d.create_time >= '2026-10-27 16:40:00'
                      AND d.create_time <  '2026-10-27 17:10:00')
 ORDER BY o.create_time;

\echo '===== 命中行数 ===== '
SELECT count(*) AS orphan_orders
  FROM orders o
 WHERE o.deleted = 0
   AND o.create_time >= '2026-10-27 16:40:00'
   AND o.create_time <  '2026-10-27 17:10:00'
   AND o.idempotency_key IS NULL
   AND NOT EXISTS (SELECT 1 FROM order_refund_records r WHERE r.order_id = o.order_id)
   AND NOT EXISTS (SELECT 1 FROM fund_flows f WHERE f.order_id = o.order_id);

\echo '===== [APPLY] 删除（默认注释；Owner 点头后取消注释） ====='
-- 备份建议：pg_dump --data-only --table=orders --table=order_items \
--   --table=order_payment_records --table=kitchen_order -f scripts/db-fix/backup/20261027-orphan-orders.sql
--
-- BEGIN;
-- CREATE TEMP TABLE victim AS
-- SELECT o.order_id FROM orders o
--  WHERE o.deleted = 0 AND o.idempotency_key IS NULL
--    AND o.create_time >= '2026-10-27 16:40:00' AND o.create_time < '2026-10-27 17:10:00'
--    AND NOT EXISTS (SELECT 1 FROM order_refund_records r WHERE r.order_id = o.order_id)
--    AND NOT EXISTS (SELECT 1 FROM fund_flows f WHERE f.order_id = o.order_id);
-- DELETE FROM kitchen_order        WHERE order_id IN (SELECT order_id FROM victim);
-- DELETE FROM order_payment_records WHERE order_id IN (SELECT order_id FROM victim);
-- DELETE FROM order_items          WHERE order_id IN (SELECT order_id FROM victim);
-- DELETE FROM orders               WHERE order_id IN (SELECT order_id FROM victim);
-- COMMIT;

\echo '===== 执行后复核（APPLY 后跑，应为 0） ====='
SELECT count(*) AS remaining_orphans FROM orders WHERE deleted = 0 AND idempotency_key IS NULL;