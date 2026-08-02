-- 门店桌台与叫号模块 - 数据库迁移脚本
-- 为dining_tables添加store_id列，为call_number_queues添加缺失索引
-- 版本：V20260516__create_store_table_queue_tables.sql

-- ══════════════════════════════════════════════════════════════
-- 1. dining_tables 表变更：添加 store_id 列
-- ══════════════════════════════════════════════════════════════

ALTER TABLE dining_tables ADD COLUMN IF NOT EXISTS store_id BIGINT;
COMMENT ON COLUMN dining_tables.store_id IS '门店ID';

CREATE INDEX IF NOT EXISTS idx_dining_tables_store_id ON dining_tables(store_id);
CREATE INDEX IF NOT EXISTS idx_dining_tables_store_code ON dining_tables(store_id, table_code);
CREATE INDEX IF NOT EXISTS idx_dining_tables_status ON dining_tables(status);
CREATE INDEX IF NOT EXISTS idx_dining_tables_deleted ON dining_tables(deleted);

-- ══════════════════════════════════════════════════════════════
-- 2. call_number_queues 表：添加缺失索引
-- ══════════════════════════════════════════════════════════════

CREATE INDEX IF NOT EXISTS idx_call_number_queues_store_id ON call_number_queues(store_id);
CREATE INDEX IF NOT EXISTS idx_call_number_queues_status ON call_number_queues(store_id, status);
CREATE INDEX IF NOT EXISTS idx_call_number_queues_create_time ON call_number_queues(store_id, create_time);
CREATE INDEX IF NOT EXISTS idx_call_number_queues_deleted ON call_number_queues(deleted);
