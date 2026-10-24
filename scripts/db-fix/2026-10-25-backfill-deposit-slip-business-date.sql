-- ============================================================================
-- 2026-10-25-backfill-deposit-slip-business-date.sql
--
-- BL-8 历史回填：deposit_slips.business_date（业务日期 = 交班日，TD-36）
--
-- 口径：business_date = 该缴款单所属班次 pos_shifts.end_time 的日期（交班日）。
--   * 只回填 business_date IS NULL 的行，**不动**已有值（幂等，可反复执行）；
--   * 不按 created_at / submitted_at 猜日期，避免与交班日错位；
--   * shift_id 为空或 pos_shifts 无对应班次的行保持 NULL，并在末尾列出原因。
--
-- 幂等：UPDATE ... WHERE business_date IS NULL AND EXISTS(...)；重复执行 0 行变更。
--
-- 用法（单事务）：
--   psql -1 -v ON_ERROR_STOP=1 -h 127.0.0.1 -p 5432 -U postgres -d food_traceability \
--        -f scripts/db-fix/2026-10-25-backfill-deposit-slip-business-date.sql
-- ============================================================================

\echo '=== 回填前：business_date 分布 ==='
SELECT COALESCE(business_date::text, '(NULL)') AS business_date, count(*) AS rows
  FROM deposit_slips
 WHERE deleted = 0
 GROUP BY business_date
 ORDER BY business_date NULLS FIRST;

\echo '=== 回填前：NULL 行数 / 总数 ==='
SELECT count(*) FILTER (WHERE business_date IS NULL) AS null_rows,
       count(*)                                      AS total_rows
  FROM deposit_slips
 WHERE deleted = 0;

\echo '=== 回填前：无法归属的 NULL 行（shift 缺失）==='
SELECT d.slip_id, d.slip_no, d.shift_id,
       CASE WHEN d.shift_id IS NULL THEN 'shift_id 为空'
            ELSE 'pos_shifts 无该班次' END AS reason
  FROM deposit_slips d
 WHERE d.deleted = 0
   AND d.business_date IS NULL
   AND (d.shift_id IS NULL
        OR NOT EXISTS (SELECT 1 FROM pos_shifts s WHERE s.shift_id = d.shift_id))
 ORDER BY d.slip_id;

-- ---------------------------------------------------------------------------
-- 回填：交班日 = pos_shifts.end_time 的日期
-- ---------------------------------------------------------------------------
WITH updated AS (
    UPDATE deposit_slips d
       SET business_date = s.end_time::date,
           update_time   = CURRENT_TIMESTAMP
      FROM pos_shifts s
     WHERE s.shift_id = d.shift_id
       AND d.business_date IS NULL
       AND s.end_time IS NOT NULL
    RETURNING d.slip_id
)
SELECT count(*) AS backfilled_rows FROM updated;

\echo '=== 回填后：business_date 分布（按交班日）==='
SELECT COALESCE(business_date::text, '(NULL)') AS business_date, count(*) AS rows
  FROM deposit_slips
 WHERE deleted = 0
 GROUP BY business_date
 ORDER BY business_date NULLS FIRST;

\echo '=== 回填后：仍为 NULL 的行及原因 ==='
SELECT d.slip_id, d.slip_no, d.shift_id, d.status,
       CASE WHEN d.shift_id IS NULL THEN 'shift_id 为空'
            WHEN NOT EXISTS (SELECT 1 FROM pos_shifts s WHERE s.shift_id = d.shift_id) THEN 'pos_shifts 无该班次'
            WHEN (SELECT s.end_time FROM pos_shifts s WHERE s.shift_id = d.shift_id) IS NULL THEN 'pos_shifts.end_time 为空'
            ELSE '其它' END AS reason
  FROM deposit_slips d
 WHERE d.deleted = 0
   AND d.business_date IS NULL
 ORDER BY d.slip_id;

\echo '=== 回填后：与交班日不一致的行（应为 0，自检口径 TD-36）==='
SELECT count(*) AS mismatch_rows
  FROM deposit_slips d
  JOIN pos_shifts s ON s.shift_id = d.shift_id
 WHERE d.deleted = 0
   AND d.business_date IS NOT NULL
   AND d.business_date <> s.end_time::date;
