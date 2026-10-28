-- =====================================================================
-- V20261028_021__dining_table_compat_view_columns.sql
--
-- 起因（P0-A）：GET /v1/pos/tables → HTTP 200 + code=500
--   栈首行：PSQLException 字段 "id" 不存在
--   查证：**dining_table 是 VIEW（不是表）**，定义 = SELECT ... FROM dining_tables（16 列）。
--     information_schema 实测：dining_table → VIEW ｜ dining_tables → BASE TABLE
--     之前尝试 ALTER 时报错：`ADD COLUMN cannot be performed on relation "dining_table"`（视图不支持）
--
-- 处置：**重建兼容视图**，在原有列基础上补出实体 DiningTable 需要的列（id/table_number/capacity/
--   area/guest_count/seated_at/created_at/updated_at），全部由真实列派生，**不动基表结构**、不动历史数据。
--   这样：
--     · 实体不改（保住 API JSON 契约：status 仍是 'available'/'occupied' 字符串）
--     · 前端不改
--     · 数据仍只有一份真相（dining_tables）
--
-- 派生规则（写明，避免以后当成真实列去写）：
--   id            = table_id
--   table_number  = table_code
--   capacity      = seats_count
--   area          = area_id::text     （视图里 area 是字符串，真实列 area_id 是 bigint）
--   guest_count   = min_people
--   seated_at     = NULL::timestamp   （基表无此语义列；实体该字段当前只读不写）
--   created_at    = create_time
--   updated_at    = update_time
--   status        = CASE status WHEN 1 THEN 'available' WHEN 2 THEN 'occupied' ELSE status::text END
-- =====================================================================

DROP VIEW IF EXISTS dining_table;

CREATE VIEW dining_table AS
SELECT
    d.table_id                AS id,
    d.table_code              AS table_number,
    d.table_name              AS table_name,
    d.seats_count             AS capacity,
    d.area_id::text           AS area,
    CASE d.status
        WHEN 1 THEN 'available'
        WHEN 2 THEN 'occupied'
        ELSE d.status::text
    END                       AS status,
    d.qr_code                 AS qr_code,
    d.current_order_id::text  AS current_order_id,
    d.min_people              AS guest_count,
    NULL::timestamp without time zone AS seated_at,
    d.create_time             AS created_at,
    d.update_time             AS updated_at
  FROM dining_tables d
 WHERE d.deleted = 0;

COMMENT ON VIEW dining_table IS '桌子兼容视图（P0-A 2026-10-28 重建）：由 dining_tables 派生，供实体 DiningTable 使用；列语义见 V20261028_021 迁移头注释';
