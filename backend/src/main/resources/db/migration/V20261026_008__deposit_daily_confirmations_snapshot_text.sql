-- =====================================================================
-- V20261026_008__deposit_daily_confirmations_snapshot_text.sql
-- NEW-34 修正：snapshot_json 由 jsonb 改为 text ——
--   实体字段是 String，MyBatis 以 varchar 绑定；jsonb 列直接插会报「字段类型为 jsonb，但表达式为 character varying」。
--   快照只用于「原样回显 + 哈希比对」，用 text 存储更稳（不依赖 JSON 规范化顺序）。
-- =====================================================================

ALTER TABLE deposit_daily_confirmations
    ALTER COLUMN snapshot_json TYPE text USING snapshot_json::text;
