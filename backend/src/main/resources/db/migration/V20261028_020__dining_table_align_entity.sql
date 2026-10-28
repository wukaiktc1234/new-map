-- =====================================================================
-- V20261028_020__dining_table_align_entity.sql
--
-- 起因（P0-A，2026-10-28）：GET /v1/pos/tables 返回 HTTP 200 + 业务 code=500，
--   异常栈首行：org.postgresql.util.PSQLException: 错误: 字段 "id" 不存在
--   SQL: SELECT id,table_number,...,created_at FROM dining_table
--
-- 定性：实体 DiningTable（@TableName("dining_table")）的列名（id / table_number / capacity /
--   area / qr_code / guest_count / seated_at / created_at / updated_at）与实际表
--   dining_table（table_id / table_code / seats_count / area_id / ...）**完全不一致** → 查询必然失败。
--
-- 处置：**以真实表为准，给实体加 @TableField 显式列映射**（不改表结构、不动历史数据）。
--   表当前 0 行，且没有其它实体指向 dining_table（只有 DiningTable.java），改动面无风险。
--   本迁移只做「列语义登记」：把真实表列的用途写进库注释，避免下次再猜。
--
-- 时间列：timestamp without time zone（单时区假设，见 tech-debt-schema.md P0-1）。
-- =====================================================================

COMMENT ON COLUMN dining_table.table_id IS '桌台主键（实体 DiningTable.id 映射到此）';
COMMENT ON COLUMN dining_table.table_code IS '桌台编码（实体 DiningTable.tableNumber 映射到此）';
COMMENT ON COLUMN dining_table.seats_count IS '座位数（实体 DiningTable.capacity 映射到此）';
COMMENT ON COLUMN dining_table.area_id IS '区域ID（实体 DiningTable.area 映射到此，bigint）';
COMMENT ON COLUMN dining_table.status IS '状态（integer：1 可用 / 其它见产品定义）';
COMMENT ON COLUMN dining_table.current_order_id IS '当前订单ID（varchar）';
COMMENT ON COLUMN dining_table.create_time IS '创建时间（实体 createdAt 映射到此）';
COMMENT ON COLUMN dining_table.update_time IS '更新时间（实体 updatedAt 映射到此）';
