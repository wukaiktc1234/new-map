\set ON_ERROR_STOP on
BEGIN;
WITH cash AS (
  SELECT f.flow_id, f.amount, f.refund_time, ba.store_id
    FROM fund_flows f JOIN bank_accounts ba ON ba.account_id=f.account_id AND ba.deleted=0
   WHERE f.deleted=0 AND f.status=2 AND f.flow_category=2 AND f.flow_direction=2
     AND f.refund_time IS NOT NULL AND ba.account_type=2
), calc AS (
  SELECT s.slip_id, s.expected_amount, s.actual_amount,
         COALESCE((SELECT sum(c.amount) FROM cash c, pos_shifts ps
                    WHERE ps.shift_id = s.shift_id AND c.store_id IS NOT DISTINCT FROM s.store_id
                      AND c.refund_time >= (ps.start_time AT TIME ZONE 'Asia/Shanghai' + make_interval(mins => 10))
                      AND c.refund_time <= (ps.end_time   AT TIME ZONE 'Asia/Shanghai' - make_interval(mins => 10))),0)
       + COALESCE((SELECT sum(f2.amount) FROM refund_shift_attribution a
                     JOIN fund_flows f2 ON f2.refund_record_id = a.refund_record_id AND f2.deleted=0 AND f2.flow_category=2
                     JOIN deposit_slips ds ON ds.slip_id = a.slip_id
                    WHERE ds.shift_id = s.shift_id AND ds.store_id IS NOT DISTINCT FROM s.store_id),0) AS comp
    FROM deposit_slips s WHERE s.deleted=0 AND s.slip_id = 231
)
UPDATE deposit_slips d SET refund_component_fen = c.comp,
       difference_net_fen = (c.actual_amount - COALESCE(c.reserve_fen, 0)) - (c.expected_amount - c.comp)
  FROM calc c WHERE d.slip_id = c.slip_id;
SELECT slip_id, refund_component_fen, difference_net_fen FROM deposit_slips WHERE slip_id=231;
COMMIT;