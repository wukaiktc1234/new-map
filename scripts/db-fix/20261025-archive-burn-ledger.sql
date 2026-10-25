-- ============================================================================
-- 20261025-archive-burn-ledger.sql
--
-- BL-5 烧号账 90 天归档（**逻辑归档，不物理删除**）
--
-- 口径：create_time < now() - interval '90 days' 且 archived_flag = false 的行
--        置 archived_flag = true、archived_time = now()。
--       A25 只统计 archived_flag=false 的行，因此归档后活跃洞数自然收敛。
--
-- 幂等：WHERE archived_flag = false，重复执行只影响尚未归档的行（第二次 0 行）。
--
-- 用法：
--   psql -1 -v ON_ERROR_STOP=1 -h 127.0.0.1 -p 5432 -U postgres -d food_traceability \
--        -f scripts/db-fix/20261025-archive-burn-ledger.sql
-- ============================================================================

\echo '=== 归档前：活跃 / 已归档 ==='
SELECT count(*) FILTER (WHERE archived_flag = false) AS active_rows,
       count(*) FILTER (WHERE archived_flag = true)  AS archived_rows,
       count(*)                                      AS total_rows
  FROM fund_flow_no_burn_ledger;

\echo '=== 归档前：待归档（超过 90 天且仍活跃）==='
SELECT count(*) AS due_rows
  FROM fund_flow_no_burn_ledger
 WHERE archived_flag = false
   AND create_time < now() - interval '90 days';

WITH archived AS (
    UPDATE fund_flow_no_burn_ledger
       SET archived_flag = true,
           archived_time = now()
     WHERE archived_flag = false
       AND create_time < now() - interval '90 days'
    RETURNING ledger_id
)
SELECT count(*) AS archived_in_this_run FROM archived;

\echo '=== 归档后：活跃 / 已归档 ==='
SELECT count(*) FILTER (WHERE archived_flag = false) AS active_rows,
       count(*) FILTER (WHERE archived_flag = true)  AS archived_rows,
       count(*)                                      AS total_rows
  FROM fund_flow_no_burn_ledger;
