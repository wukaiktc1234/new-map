-- V20261030_016__add_missing_trace_code_columns.sql
--
-- P0 drift fix (Lead 2026-10-30): 3 columns of material_trace_code exist ONLY in production.
-- They are created at startup by config/TraceCodeSystemConfig (an undocumented DDL runner),
-- therefore they never entered flyway_schema_history -> schema drift source.
--
-- Verification performed before writing this migration:
--   the other 4 columns used by TraceCodeSystemConfig (locked_quantity / trace_type /
--   material_locked / material_lock_time) ALREADY have flyway records -> not repeated here.
--
-- Column definitions taken from PRODUCTION information_schema.columns (NOT from the Java
-- string literals in TraceCodeSystemConfig, which declare numeric(10,3) / varchar(100) and
-- therefore do NOT match production - recorded as P1 tech debt):
--   available_quantity | numeric(12,2) | nullable | no default
--   bind_dish_id       | character varying(50)  | nullable
--   bind_dish_name     | character varying(200) | nullable
--
-- Rule learned tonight: the single source of truth for a migration is the PRODUCTION
-- database, not the DDL strings embedded in Java code.
--
-- Pure ASCII only (Owner red line). Asserts column existence only, never a total count.

ALTER TABLE material_trace_code ADD COLUMN IF NOT EXISTS available_quantity numeric(12,2);
ALTER TABLE material_trace_code ADD COLUMN IF NOT EXISTS bind_dish_id character varying(50);
ALTER TABLE material_trace_code ADD COLUMN IF NOT EXISTS bind_dish_name character varying(200);

DO $$
DECLARE
    missing text;
    v_cnt bigint;
BEGIN
    SELECT string_agg(c, ', ') INTO missing
      FROM unnest(ARRAY['available_quantity', 'bind_dish_id', 'bind_dish_name']) AS c
     WHERE NOT EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_schema = 'public' AND table_name = 'material_trace_code'
                          AND column_name = c);
    IF missing IS NOT NULL THEN
        RAISE EXCEPTION '016 assertion failed: material_trace_code missing columns %', missing;
    END IF;

    SELECT count(*) INTO v_cnt FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'material_trace_code';
    RAISE NOTICE '016 OK: 3 trace-code columns present (current total columns = %)', v_cnt;
END $$;
