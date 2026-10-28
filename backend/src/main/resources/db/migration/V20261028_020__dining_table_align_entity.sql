-- =====================================================================
-- V20261028_020__dining_table_align_entity.sql（2026-10-28 重写，view-safe）
--
-- 起因（P0-A）：GET /v1/pos/tables → HTTP 200 + code=500，
--   栈首行：PSQLException 字段 "id" 不存在。
--   查证：**dining_table 是 VIEW**（由 dining_tables 派生），实体 DiningTable 打在该视图上。
--
-- 为什么重写：首版在本文件里 COMMENT ON COLUMN dining_table.table_id ——
--   视图重建后不再有 table_id 列（改名为 id），导致**应用启动时 Flyway 直接失败**：
--   `错误: 关系 "dining_table" 的 "table_id" 字段不存在`。
--   教训：**注释要打在基表（BASE TABLE）上，不要打在视图列上** —— 视图列名会随视图定义变。
--
-- 本版：只给 **dining_tables（基表）** 加注释，登记真实列的语义，避免下次再猜。
-- =====================================================================

COMMENT ON COLUMN dining_tables.table_id IS '桌台主键（实体 DiningTable.id 经兼容视图映射到此）';
COMMENT ON COLUMN dining_tables.table_code IS '桌台编码（实体 DiningTable.tableNumber 映射到此）';
COMMENT ON COLUMN dining_tables.seats_count IS '座位数（实体 DiningTable.capacity 映射到此）';
COMMENT ON COLUMN dining_tables.area_id IS '区域ID（视图里 area 为其 text 形式，bigint）';
COMMENT ON COLUMN dining_tables.status IS '状态（integer：1 可用 / 2 占用；视图转成 available/occupied 字符串）';
COMMENT ON COLUMN dining_tables.current_order_id IS '当前订单ID（varchar）';
COMMENT ON COLUMN dining_tables.create_time IS '创建时间（实体 DiningTable.createdAt 映射到此）';
COMMENT ON COLUMN dining_tables.update_time IS '更新时间（实体 DiningTable.updatedAt 映射到此）';
