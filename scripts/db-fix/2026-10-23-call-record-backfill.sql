-- 2026-10-23 叫号历史数据修补（可复放）
-- 背景：早期 call_record 行由「叫号屏同步」产生，部分行缺 call_number（叫号号上线前的历史残渣）；
--       另有少量行状态与时间戳不一致（picked 无 pick_time）。本脚本先按 orders 回填，再清理无主的空号行。

-- 1) 从 orders 回填叫号号（仅当历史行缺号且订单已有号）
UPDATE call_record c
SET call_number = o.call_number,
    update_time = NOW()
FROM orders o
WHERE c.order_id = o.order_id
  AND (c.call_number IS NULL OR c.call_number = '')
  AND o.call_number IS NOT NULL
  AND o.call_number <> '';

-- 2) picked 但缺 pick_time：用 update_time 兜底（不臆造业务时间，只是补一致性）
UPDATE call_record
SET pick_time = update_time
WHERE status = 'picked' AND pick_time IS NULL;

-- 3) called 但缺 first_call_time：用 last_call_time 兜底
UPDATE call_record
SET first_call_time = last_call_time
WHERE status IN ('called', 'picked') AND first_call_time IS NULL AND last_call_time IS NOT NULL;

-- 4) 清理孤立的空号历史行（既无叫号号、也无对应订单）——典型「假数据」
DELETE FROM call_record c
WHERE (c.call_number IS NULL OR c.call_number = '')
  AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.order_id = c.order_id);
