-- ============================================================================
-- 2026-10-27 E2E 历史 shift 遗留清理（**只准备，不执行**，等 Owner 点头）
-- 背景：E2E 长期只在用例里开/关班、从不清理 → pos_shifts 遗留 86 行（含 T-N194-* 等历史批次）
-- 判定：terminal_id LIKE 'T-%'（E2E 用的终端前缀）且「无任何缴款单 / 无任何订单」→ 纯垃圾班次
-- 安全前提：本文件只 DELETE pos_shifts；不动 orders/deposit_slips/fund_flows，也不碰非 T- 终端
-- ============================================================================
\encoding UTF8
\pset pager off
\echo '===== [DRY-RUN] 命中统计（只读） ====='

-- 1) 候选总量
SELECT count(*) AS total_t_shifts FROM pos_shifts WHERE deleted = 0 AND terminal_id LIKE 'T-%';

-- 2) 其中「有缴款单」的（**保留**，不能删）
SELECT count(*) AS with_slips
  FROM pos_shifts s
 WHERE s.deleted = 0 AND s.terminal_id LIKE 'T-%'
   AND EXISTS (SELECT 1 FROM deposit_slips d WHERE d.shift_id = s.shift_id AND d.deleted = 0);

-- 3) 其中「窗口内有订单」的（**保留**，订单要留证据）
SELECT count(*) AS with_orders
  FROM pos_shifts s
 WHERE s.deleted = 0 AND s.terminal_id LIKE 'T-%'
   AND EXISTS (SELECT 1 FROM orders o
                WHERE o.deleted = 0
                  AND o.create_time >= s.start_time
                  AND o.create_time <= COALESCE(s.end_time, now()));

-- 4) 真正可清（无缴款单、无订单）
SELECT count(*) AS purgeable_shifts
  FROM pos_shifts s
 WHERE s.deleted = 0 AND s.terminal_id LIKE 'T-%'
   AND NOT EXISTS (SELECT 1 FROM deposit_slips d WHERE d.shift_id = s.shift_id AND d.deleted = 0)
   AND NOT EXISTS (SELECT 1 FROM orders o
                    WHERE o.deleted = 0
                      AND o.create_time >= s.start_time
                      AND o.create_time <= COALESCE(s.end_time, now()));

-- 5) 明细（前 30 行，便于人工核对）
SELECT s.shift_id, s.terminal_id, s.employee_name, s.status,
       s.start_time, s.end_time, COALESCE(s.opening_cash, 0) AS opening_cash
  FROM pos_shifts s
 WHERE s.deleted = 0 AND s.terminal_id LIKE 'T-%'
   AND NOT EXISTS (SELECT 1 FROM deposit_slips d WHERE d.shift_id = s.shift_id AND d.deleted = 0)
   AND NOT EXISTS (SELECT 1 FROM orders o
                    WHERE o.deleted = 0
                      AND o.create_time >= s.start_time
                      AND o.create_time <= COALESCE(s.end_time, now()))
 ORDER BY s.shift_id
 LIMIT 30;

\echo '===== [APPLY] 真正的删除（默认注释掉；Owner 点头后取消注释执行） ====='
-- 备份建议：执行前先 pg_dump
--   pg_dump -h localhost -U postgres -d food_traceability --data-only --table=pos_shifts \
--     -f scripts/db-fix/backup/pos_shifts_20261027_pre_purge.sql
--
-- BEGIN;
-- DELETE FROM pos_shifts s
--  WHERE s.deleted = 0 AND s.terminal_id LIKE 'T-%'
--    AND NOT EXISTS (SELECT 1 FROM deposit_slips d WHERE d.shift_id = s.shift_id AND d.deleted = 0)
--    AND NOT EXISTS (SELECT 1 FROM orders o
--                     WHERE o.deleted = 0
--                       AND o.create_time >= s.start_time
--                       AND o.create_time <= COALESCE(s.end_time, now()));
-- COMMIT;

\echo '===== 执行后复核（APPLY 后跑一次，应为 0） ====='
SELECT count(*) AS remaining_t_shifts FROM pos_shifts WHERE deleted = 0 AND terminal_id LIKE 'T-%';