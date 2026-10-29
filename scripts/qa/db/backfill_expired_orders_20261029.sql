-- =====================================================================
-- 历史死单回填（Owner 第 3 条要求）2026-10-29
--
-- 口径：只刷「15 分钟前创建、仍 order_status=0 且未支付」的死单；
--   **不回补 foods.stock**（历史账不回溯）；标记用 EXPIRED_AUTO_BACKFILL，
--   与作业关单的 EXPIRED_AUTO 区分（一眼能看出是脚本刷的还是任务关的）。
--
-- 执行方式：先跑 DRY RUN（注释掉的部分），确认 92 笔与金额，再执行 UPDATE。
-- =====================================================================

-- DRY RUN（只读）
SELECT count(*) AS will_update_rows,
       COALESCE(SUM(COALESCE(final_amount, total_amount, 0)), 0) AS amount_fen_total
  FROM orders
 WHERE order_status = 0
   AND COALESCE(payment_status, 0) <> 2
   AND deleted = 0
   AND create_time < now() - interval '15 minutes';

-- 清理前 countPendingExceptionOrders（全库口径）
SELECT count(*) AS pending_before
  FROM orders o
 WHERE o.shift_id IS NOT NULL AND o.deleted = 0
   AND COALESCE(o.payment_status, 0) <> 2
   AND COALESCE(o.cancel_reason, '') <> 'EXPIRED_AUTO'
   AND NOT EXISTS (SELECT 1 FROM exception_ops e WHERE e.order_id = o.order_id AND e.deleted = 0);

-- 执行：单条 UPDATE + 明确条件
UPDATE orders
   SET order_status  = 3,
       cancel_reason = 'EXPIRED_AUTO_BACKFILL',
       update_time   = now()
 WHERE order_status = 0
   AND COALESCE(payment_status, 0) <> 2
   AND deleted = 0
   AND create_time < now() - interval '15 minutes'
RETURNING order_id;

-- 清理后
SELECT count(*) AS pending_after
  FROM orders o
 WHERE o.shift_id IS NOT NULL AND o.deleted = 0
   AND COALESCE(o.payment_status, 0) <> 2
   AND COALESCE(o.cancel_reason, '') <> 'EXPIRED_AUTO'
   AND NOT EXISTS (SELECT 1 FROM exception_ops e WHERE e.order_id = o.order_id AND e.deleted = 0);
