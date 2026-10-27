\set ON_ERROR_STOP on
SET client_encoding='UTF8';
BEGIN;
-- 回填 2 张 N19-2 之前的老缴款单：按 A39② 口径（窗口内非边界自动计入 ∪ 人工归属）重算 refund_component_fen，并同步 difference_net_fen
WITH cash AS (
  SELECT f.flow_id, f.amount, f.refund_time, ba.store_id
    FROM fund_flows f JOIN bank_accounts ba ON ba.account_id=f.account_id AND ba.deleted=0
   WHERE f.deleted=0 AND f.status=2 AND f.flow_category=2 AND f.flow_direction=2
     AND f.refund_time IS NOT NULL AND ba.account_type=2
), calc AS (
  SELECT s.slip_id,
         COALESCE((SELECT sum(c.amount) FROM cash c JOIN pos_shifts ps ON ps.shift_id=s.shift_id
                    WHERE c.store_id = s.store_id
                      AND c.refund_time >= (ps.start_time AT TIME ZONE 'Asia/Shanghai' + make_interval(mins => 10))
                      AND c.refund_time <= (ps.end_time   AT TIME ZONE 'Asia/Shanghai' - make_interval(mins => 10))
                  ),0)
       + COALESCE((SELECT sum(f2.amount) FROM refund_shift_attribution a
                     JOIN fund_flows f2 ON f2.refund_record_id = a.refund_record_id AND f2.deleted=0 AND f2.flow_category=2
                    WHERE a.shift_id = s.shift_id),0) AS comp
    FROM deposit_slips s WHERE s.deleted=0 AND s.slip_id IN (231,232)
)
UPDATE deposit_slips d
   SET refund_component_fen = c.comp,
       difference_net_fen = (d.actual_amount - COALESCE(d.reserve_fen, 0)) - (d.expected_amount - c.comp)
  FROM calc c WHERE d.slip_id = c.slip_id;
SELECT slip_id, expected_amount, actual_amount, refund_component_fen, difference_net_fen FROM deposit_slips WHERE slip_id IN (231,232) ORDER BY 1;
COMMIT;