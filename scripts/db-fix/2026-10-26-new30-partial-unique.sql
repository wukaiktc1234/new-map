\set ON_ERROR_STOP on
BEGIN;
ALTER TABLE reconciliation_items DROP CONSTRAINT IF EXISTS uk_recon_items_statement_subject;
DROP INDEX IF EXISTS uk_recon_items_statement_subject;
CREATE UNIQUE INDEX uk_recon_items_statement_subject_live ON reconciliation_items
  (statement_id, category, COALESCE(flow_id,0), COALESCE(transaction_id,0)) WHERE deleted = 0;
\echo '--- AFTER ---'
SELECT indexname FROM pg_indexes WHERE tablename='reconciliation_items' AND indexname LIKE 'uk_%' ORDER BY 1;
COMMIT;