\set ON_ERROR_STOP on
BEGIN;
UPDATE deposit_slips
   SET difference_net_fen = actual_amount - (expected_amount - COALESCE(refund_component_fen,0))
 WHERE difference_net_fen IS NULL AND deleted = 0
   AND COALESCE(refund_component_fen,0) = 0;
SELECT count(*) AS remaining_null FROM deposit_slips WHERE difference_net_fen IS NULL AND deleted=0;
COMMIT;