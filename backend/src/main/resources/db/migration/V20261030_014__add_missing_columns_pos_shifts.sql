-- V20261030_014__add_missing_columns_pos_shifts.sql
--
-- P0 migration-system / production-schema decoupling (Lead 2026-10-30).
-- Adds the 2 missing columns of pos_shifts with ADD COLUMN IF NOT EXISTS (idempotent).
-- NOTE (production facts measured 2026-10-30): pos_shifts already has 16 columns and
--      create_time / update_time ALREADY EXIST (45 rows). So both ADD statements are no-ops;
--      they are kept only for idempotency on environments lacking these columns.
--
-- Column definitions taken from production information_schema.columns:
--   create_time | timestamp without time zone | NOT NULL | DEFAULT CURRENT_TIMESTAMP
--   update_time | timestamp without time zone | NOT NULL | DEFAULT CURRENT_TIMESTAMP
--
-- WARNING: this migration MUST NOT assert a fixed total column count.
-- NOTE: an earlier revision asserted a fixed total column count (18), which failed
-- NOTE: on production (16 columns). Asserts below check only column existence.
-- It only asserts that the 2 columns exist; the total count is printed via NOTICE.
--
-- Owner red line: this file must stay pure ASCII (no CJK characters).

ALTER TABLE pos_shifts ADD COLUMN IF NOT EXISTS create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE pos_shifts ADD COLUMN IF NOT EXISTS update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL;

DO $$
DECLARE
    missing text;
    v_cnt bigint;
BEGIN
    SELECT string_agg(c, ', ') INTO missing
      FROM unnest(ARRAY['create_time', 'update_time']) AS c
     WHERE NOT EXISTS (SELECT 1 FROM information_schema.columns
                        WHERE table_schema = 'public' AND table_name = 'pos_shifts'
                          AND column_name = c);
    IF missing IS NOT NULL THEN
        RAISE EXCEPTION '014 assertion failed: pos_shifts missing columns %', missing;
    END IF;

    SELECT count(*) INTO v_cnt FROM information_schema.columns
     WHERE table_schema = 'public' AND table_name = 'pos_shifts';
    RAISE NOTICE '014 OK: pos_shifts has both columns (current total columns = %)', v_cnt;
END $$;
