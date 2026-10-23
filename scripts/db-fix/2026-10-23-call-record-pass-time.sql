-- 2026-10-23 叫号过号留痕（可复放）
ALTER TABLE call_record ADD COLUMN IF NOT EXISTS pass_time timestamp without time zone;
CREATE INDEX IF NOT EXISTS idx_call_record_status_create ON call_record (status, create_time DESC);
