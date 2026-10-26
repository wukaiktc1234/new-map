\set ON_ERROR_STOP on
SET client_encoding='UTF8';
BEGIN;
-- 守卫：只改「POS 即时成功退款」特征行（complete_time 非空 + method=1 + type=1 + status=2）
UPDATE order_refund_records
   SET refund_status = 3, update_time = now()
 WHERE refund_status = 2 AND refund_type = 1 AND refund_method = 1
   AND complete_time IS NOT NULL
   AND abs(extract(epoch FROM (complete_time - create_time))) = 0;
\echo '--- AFTER ---'
SELECT refund_status, count(*) FROM order_refund_records GROUP BY 1 ORDER BY 1;
COMMIT;