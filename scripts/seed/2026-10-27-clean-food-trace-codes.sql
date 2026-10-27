-- 清理 E2E 追溯码（按 tag 幂等清理）
\set ON_ERROR_STOP on
BEGIN;
DELETE FROM food_trace_codes WHERE trace_code LIKE 'TC-E2E-SEED-%' OR extra_info LIKE '%[E2E:trace-seed]%';
COMMIT;
SELECT count(*) AS remaining FROM food_trace_codes WHERE trace_code LIKE 'TC-E2E-SEED-%';