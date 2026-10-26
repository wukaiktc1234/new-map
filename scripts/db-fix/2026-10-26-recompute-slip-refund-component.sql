-- =====================================================================
-- 2026-10-26-recompute-slip-refund-component.sql
-- N19-2④ 缺陷修复配套：按**唯一公式**（与巡检 A39 完全同口径）重算全部缴款单的
--   refund_component_fen 与 difference_net_fen。幂等，可重复执行。
--   期望 = Σ 同门店现金户的现金退款流水，满足
--     (a) refund_time 落在班次 [start+N, end-N]（N=10 分钟边界带，A39 的 SHIFT_BOUNDARY_MINUTES），或
--     (b) refund_shift_attribution 把该 refund_record_id 归属到该班次
--   difference_net_fen = actual_amount - (expected_amount - refund_component_fen)
-- 守卫：写后复核 A39 恒等式违规数必须为 0，否则 RAISE 回滚；打印影响行数。
-- =====================================================================

\set ON_ERROR_STOP on
\set SHIFT_BOUNDARY_MINUTES 10

BEGIN;

WITH slip AS (
    SELECT s.slip_id, s.store_id, s.shift_id, s.expected_amount, s.actual_amount, s.create_time,
           COALESCE(s.refund_component_fen, 0) AS rcf
      FROM deposit_slips s
     WHERE s.deleted = 0
), cash_refund AS (
    SELECT f.flow_id, f.amount, f.refund_record_id, f.refund_time, ba.store_id AS store_id
      FROM fund_flows f
      JOIN bank_accounts ba ON ba.account_id = f.account_id AND ba.deleted = 0
     WHERE f.deleted = 0 AND f.status = 2 AND f.flow_category = 2 AND f.flow_direction = 2
       AND f.refund_record_id IS NOT NULL AND ba.account_type = 2
), expected AS (
    SELECT s.slip_id, COALESCE(SUM(f.amount), 0) AS amt
      FROM slip s
      JOIN cash_refund f
        ON (f.store_id IS NOT DISTINCT FROM s.store_id)
       AND f.refund_time IS NOT NULL
       AND (f.refund_time AT TIME ZONE 'Asia/Shanghai') <= s.create_time
       AND (
             EXISTS (SELECT 1 FROM pos_shifts ps
                      WHERE ps.shift_id = s.shift_id AND ps.deleted = 0
                        AND ps.start_time IS NOT NULL AND ps.end_time IS NOT NULL
                        AND f.refund_time IS NOT NULL
                        AND (f.refund_time AT TIME ZONE 'Asia/Shanghai')
                            BETWEEN ps.start_time + make_interval(mins => :'SHIFT_BOUNDARY_MINUTES'::int)
                                AND ps.end_time   - make_interval(mins => :'SHIFT_BOUNDARY_MINUTES'::int))
             OR
             EXISTS (SELECT 1 FROM refund_shift_attribution a
                      WHERE a.refund_record_id = f.refund_record_id AND a.shift_id = s.shift_id)
           )
     GROUP BY s.slip_id
)
-- 用标量子查询：即使该缴款单「期望退款 = 0」（expected 里没有行）也必须被写成 0，
-- 否则历史脏值会残留（首版用 UPDATE ... FROM 漏了这类行，实测被判守卫拦下）。
UPDATE deposit_slips s
   SET refund_component_fen = COALESCE((SELECT e.amt FROM expected e WHERE e.slip_id = s.slip_id), 0),
       difference_net_fen   = s.actual_amount
                              - (s.expected_amount - COALESCE((SELECT e.amt FROM expected e WHERE e.slip_id = s.slip_id), 0)),
       update_time          = NOW()
 WHERE s.deleted = 0
   AND (COALESCE(s.refund_component_fen, 0) <> COALESCE((SELECT e.amt FROM expected e WHERE e.slip_id = s.slip_id), 0)
        OR s.difference_net_fen IS DISTINCT FROM s.actual_amount
             - (s.expected_amount - COALESCE((SELECT e.amt FROM expected e WHERE e.slip_id = s.slip_id), 0)));

DO $$
DECLARE affected bigint;
BEGIN
    GET DIAGNOSTICS affected = ROW_COUNT;
    RAISE NOTICE '重算完成：影响行数 = %', affected;
END $$;

-- 守卫：按 A39 同口径复核，违规必须为 0
WITH slip AS (
    SELECT s.slip_id, s.store_id, s.shift_id, s.expected_amount, s.actual_amount, s.create_time,
           COALESCE(s.refund_component_fen, 0) AS rcf, s.difference_net_fen
      FROM deposit_slips s WHERE s.deleted = 0
), cash_refund AS (
    SELECT f.amount, f.refund_record_id, f.refund_time, ba.store_id AS store_id
      FROM fund_flows f JOIN bank_accounts ba ON ba.account_id = f.account_id AND ba.deleted = 0
     WHERE f.deleted = 0 AND f.status = 2 AND f.flow_category = 2 AND f.flow_direction = 2
       AND f.refund_record_id IS NOT NULL AND ba.account_type = 2
), expected AS (
    SELECT s.slip_id, COALESCE(SUM(f.amount), 0) AS amt
      FROM slip s JOIN cash_refund f
        ON (f.store_id IS NOT DISTINCT FROM s.store_id)
       AND f.refund_time IS NOT NULL
       AND (f.refund_time AT TIME ZONE 'Asia/Shanghai') <= s.create_time
       AND (
             EXISTS (SELECT 1 FROM pos_shifts ps
                      WHERE ps.shift_id = s.shift_id AND ps.deleted = 0
                        AND ps.start_time IS NOT NULL AND ps.end_time IS NOT NULL
                        AND f.refund_time IS NOT NULL
                        AND (f.refund_time AT TIME ZONE 'Asia/Shanghai')
                            BETWEEN ps.start_time + make_interval(mins => :'SHIFT_BOUNDARY_MINUTES'::int)
                                AND ps.end_time   - make_interval(mins => :'SHIFT_BOUNDARY_MINUTES'::int))
             OR EXISTS (SELECT 1 FROM refund_shift_attribution a
                         WHERE a.refund_record_id = f.refund_record_id AND a.shift_id = s.shift_id))
     GROUP BY s.slip_id
)
SELECT count(*) AS a39_violations_after
  FROM slip s LEFT JOIN expected e ON e.slip_id = s.slip_id
 WHERE s.difference_net_fen IS NOT NULL
   AND (s.difference_net_fen <> s.actual_amount - (s.expected_amount - s.rcf)
        OR s.rcf <> COALESCE(e.amt, 0))
\gset

\if :a39_violations_after
\echo '守卫失败：A39 仍存在违规，回滚'
ROLLBACK;
\else
COMMIT;
\endif
