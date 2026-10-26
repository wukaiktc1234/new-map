\set ON_ERROR_STOP on
BEGIN;
ALTER TABLE reconciliation_statements DROP CONSTRAINT IF EXISTS uk_recon_statements_period;
ALTER TABLE reconciliation_statements DROP CONSTRAINT IF EXISTS uk_recon_statements_no;
DROP INDEX IF EXISTS uk_recon_statements_period;
DROP INDEX IF EXISTS uk_recon_statements_no;
CREATE UNIQUE INDEX uk_recon_statements_period_live ON reconciliation_statements (account_id, period_start, period_end) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_recon_statements_no_live ON reconciliation_statements (statement_no) WHERE deleted = 0;
\echo '--- AFTER ---'
SELECT indexname FROM pg_indexes WHERE indexname LIKE 'uk_recon_statements%' ORDER BY 1;
COMMIT;