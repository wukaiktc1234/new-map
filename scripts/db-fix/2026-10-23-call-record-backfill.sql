-- 2026-10-23 叫号历史回填 v2（可复放）
-- v1 的缺陷：只修「已存在的 call_record 行」，没有给「完全没有历史行」的订单补行。
-- 实测活库：70 个有叫号号的订单里，23 个在 call_record 里根本没有对应行 → 历史页永远查不到。
-- 本脚本按 orders 补行，create_time 取订单创建时间（保证落在正确业务日），状态按订单真实终态映射，
-- 不臆造「呼叫/取餐」时间：已完成订单用 order.update_time 作为取餐时间，其余保持待取餐。

INSERT INTO call_record (order_id, order_number, table_number, order_type, item_count,
                         status, call_count, first_call_time, last_call_time, pick_time,
                         create_time, update_time, call_number)
SELECT o.order_id,
       COALESCE(o.order_number, o.order_code),
       o.table_name,
       o.order_type::text,
       1,
       CASE WHEN o.order_status = 2 THEN 'picked' ELSE 'pending' END,
       0,
       NULL,
       NULL,
       CASE WHEN o.order_status = 2 THEN o.update_time ELSE NULL END,
       COALESCE(o.create_time, NOW()),
       NOW(),
       o.call_number
FROM orders o
WHERE o.call_number IS NOT NULL
  AND o.call_number <> ''
  AND NOT EXISTS (SELECT 1 FROM call_record c WHERE c.order_id = o.order_id);

-- 历史行与订单终态对齐（可复放）：已完成但仍显示待取餐/已呼叫的老行 → 标记已取餐
UPDATE call_record c
SET status = 'picked',
    pick_time = COALESCE(c.pick_time, o.update_time),
    update_time = NOW()
FROM orders o
WHERE c.order_id = o.order_id
  AND o.order_status = 2
  AND c.status IN ('pending', 'called')
  AND c.pick_time IS NULL;

-- 诊断：仍无历史行的订单（应为 0）
-- SELECT COUNT(*) FROM orders o WHERE o.call_number <> '' AND NOT EXISTS (SELECT 1 FROM call_record c WHERE c.order_id = o.order_id);
