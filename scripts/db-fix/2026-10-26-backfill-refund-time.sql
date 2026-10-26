-- =====================================================================
-- 2026-10-26-backfill-refund-time.sql
-- N19-2③：把历史退款流水的 refund_time 回填为「退款单发生时刻」
--   口径：refund_time = COALESCE(r.complete_time, r.create_time)
--   范围：flow_category = 2（退款）且 refund_record_id 非空 且 refund_time IS NULL
--   守卫：
--     ① 写前统计「退款单的 complete_time 与 create_time 都为 NULL」的行数 → 若 >0 直接失败（不写任何行）
--     ② 写后复核「refund_time 仍为 NULL 且 refund_record_id 非空」的退款流水 = 0，否则 RAISE 回滚
--     ③ 打印影响行数
--   幂等：可重复执行（只处理 refund_time IS NULL 的行）
-- =====================================================================

\set ON_ERROR_STOP on

BEGIN;

-- ---------- 守卫①：写前统计无法确定时刻的行 ----------
DO $$
DECLARE
    bad bigint;
BEGIN
    SELECT count(*) INTO bad
      FROM fund_flows f
      JOIN order_refund_records r ON r.refund_id = f.refund_record_id
     WHERE f.deleted = 0
       AND f.flow_category = 2
       AND f.refund_time IS NULL
       AND COALESCE(r.complete_time, r.create_time) IS NULL;
    IF bad > 0 THEN
        RAISE EXCEPTION '守卫失败：存在 % 条退款流水的退款单 complete_time 与 create_time 均为 NULL，拒绝回填（请先修复退款单）', bad;
    END IF;
    RAISE NOTICE '守卫①通过：无「时刻不可判定」的退款单';
END $$;

-- ---------- 回填 ----------
DO $$
DECLARE
    affected bigint;
BEGIN
    UPDATE fund_flows f
       SET refund_time = COALESCE(r.complete_time, r.create_time)
      FROM order_refund_records r
     WHERE f.refund_record_id = r.refund_id
       AND f.refund_time IS NULL
       AND f.flow_category = 2;
    GET DIAGNOSTICS affected = ROW_COUNT;
    RAISE NOTICE '回填完成：影响行数 = %', affected;
END $$;

-- ---------- 守卫②：写后复核 ----------
DO $$
DECLARE
    left_null bigint;
    total_refund bigint;
BEGIN
    SELECT count(*) INTO left_null
      FROM fund_flows
     WHERE deleted = 0 AND flow_category = 2 AND refund_record_id IS NOT NULL AND refund_time IS NULL;
    IF left_null > 0 THEN
        RAISE EXCEPTION '守卫失败：回填后仍有 % 条退款流水 refund_time 为 NULL，回滚', left_null;
    END IF;
    SELECT count(*) INTO total_refund FROM fund_flows WHERE deleted = 0 AND flow_category = 2;
    RAISE NOTICE '守卫②通过：退款流水共 % 条，refund_time 全部有值', total_refund;
END $$;

COMMIT;

-- ---------- 结果核对 ----------
SELECT count(*) AS refund_flows,
       count(refund_time) AS with_time,
       count(*) FILTER (WHERE refund_time IS NULL) AS null_time
  FROM fund_flows WHERE deleted = 0 AND flow_category = 2;
