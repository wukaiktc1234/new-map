-- =====================================================================
-- 2026-10-26-backfill-refund-record-id.sql
-- 目的：回填迁移前老样本退款流水的 fund_flows.refund_record_id（P4-5 缺口）
--
-- 背景：
--   flow_id=5530（flow_no=FF202610260016, order_id=O1792951870683, amount=1800 分）
--   于 2026-10-26 02:13:49 落库，**当时 fund_flows.refund_record_id 列尚未创建**，
--   因此该行的 refund_record_id 为 NULL。后续 A30/A31/A33 采用「refund_record_id IS NOT NULL」
--   口径收窄（防 category=2 被采购付款复用导致 A31 偏大）后，这条老数据被如实判红。
--   处理原则：**补数据，不放宽断言**。
--
-- 守卫（任何一条不满足即 RAISE EXCEPTION 中止，不写库）：
--   1) 目标流水存在、deleted=0、flow_category=2、refund_record_id IS NULL（幂等前提）
--   2) 该订单的退款记录**恰好 1 条**
--   3) 退款记录金额 == 流水金额（都是分，直比不 ×100）
--   4) 回填后：全库 flow_category=2 AND refund_record_id IS NULL 的行数必须 = 0
--
-- 幂等：UPDATE 带 refund_record_id IS NULL 条件，重复执行不会改写既有值
--       （第二次执行会在守卫 1) 处因 count<>1 中止，符合预期）
-- =====================================================================

\set ON_ERROR_STOP on

BEGIN;

DO $$
DECLARE
    v_flow_id      bigint := 5530;
    v_order_id     text   := 'O1792951870683';
    v_cnt          int;
    v_refund_id    bigint;
    v_refund_amt   numeric(12,2);
    v_flow_amt     bigint;
    v_null_after   int;
    v_flow_before  text;
BEGIN
    -- 0) 快照（回填前）
    SELECT format('flow_id=%s flow_no=%s order_id=%s amount=%s category=%s refund_record_id=%s',
                  flow_id, flow_no, order_id, amount, flow_category, COALESCE(refund_record_id::text, 'NULL'))
      INTO v_flow_before
      FROM fund_flows WHERE flow_id = v_flow_id;
    RAISE NOTICE '[BEFORE] %', v_flow_before;

    -- 1) 守卫：目标流水必须是「退款类 + 未回填」
    SELECT count(*) INTO v_cnt
      FROM fund_flows
     WHERE flow_id = v_flow_id AND deleted = 0 AND flow_category = 2 AND refund_record_id IS NULL;
    IF v_cnt <> 1 THEN
        RAISE EXCEPTION '守卫失败：flow_id=% 不存在 / 非退款类 / 已回填（count=%）', v_flow_id, v_cnt;
    END IF;

    SELECT amount INTO v_flow_amt FROM fund_flows WHERE flow_id = v_flow_id;

    -- 2) 守卫：该订单退款记录恰好 1 条
    SELECT count(*) INTO v_cnt
      FROM order_refund_records WHERE order_id = v_order_id AND deleted = 0;
    IF v_cnt <> 1 THEN
        RAISE EXCEPTION '守卫失败：订单 % 的退款记录数=%（期望恰好 1 条）', v_order_id, v_cnt;
    END IF;

    SELECT refund_id, refund_amount INTO v_refund_id, v_refund_amt
      FROM order_refund_records WHERE order_id = v_order_id AND deleted = 0;

    -- 3) 守卫：退款额（分）== 流水额（分）
    IF v_refund_amt::bigint <> v_flow_amt THEN
        RAISE EXCEPTION '守卫失败：退款记录 refund_id=% 金额=% 分 <> 流水金额=% 分',
                        v_refund_id, v_refund_amt::bigint, v_flow_amt;
    END IF;
    RAISE NOTICE '[GUARD] refund_record: refund_id=% refund_amount(分)=% == flow.amount(分)=%',
                 v_refund_id, v_refund_amt::bigint, v_flow_amt;

    -- 4) 回填（幂等：仅改 refund_record_id IS NULL 的行）
    UPDATE fund_flows
       SET refund_record_id = v_refund_id,
           update_time      = now()
     WHERE flow_id = v_flow_id
       AND refund_record_id IS NULL
       AND flow_category = 2;
    IF NOT FOUND THEN
        RAISE EXCEPTION '回填未命中（行可能被并发改写）';
    END IF;

    -- 5) 复核：全库不再有 category=2 且 refund_record_id 为空的行
    SELECT count(*) INTO v_null_after
      FROM fund_flows WHERE deleted = 0 AND flow_category = 2 AND refund_record_id IS NULL;
    IF v_null_after <> 0 THEN
        RAISE EXCEPTION '复核失败：仍有 % 行 flow_category=2 AND refund_record_id IS NULL', v_null_after;
    END IF;

    RAISE NOTICE '[AFTER ] flow_id=% ← refund_record_id=%', v_flow_id, v_refund_id;
    RAISE NOTICE '[CHECK ] flow_category=2 AND refund_record_id IS NULL 行数 = %', v_null_after;
END $$;

COMMIT;

-- 回填前/后对照（脚本自身输出，便于复制进报告）
\echo ''
\echo '-- 回填后目标行 --'
SELECT flow_id, flow_no, order_id, amount, flow_category, refund_record_id, update_time
  FROM fund_flows WHERE flow_id = 5530;

\echo ''
\echo '-- 全库残留（期望 0）--'
SELECT count(*) AS null_refund_record_id_rows
  FROM fund_flows WHERE deleted = 0 AND flow_category = 2 AND refund_record_id IS NULL;
