-- ============================================================
-- V20260717_034: 迁移后数据完整性校验
-- ============================================================
-- 背景：
--   在 V20260717_005（字段类型修复）、V20260717_030（ID 类型统一）、
--   V20260717_033（事件发件箱表）等迁移完成后，执行此脚本对关键字段类型
--   与表结构进行校验，确保所有迁移均已正确生效。
--
-- 使用方式：
--   本脚本仅包含 SELECT 查询，Flyway 执行后会记录到 flyway_schema_history。
--   人工核查时也可直接复制其中的查询到 psql 中运行。
--   - 查询返回空结果集 = 该项校验通过
--   - 查询返回有结果 = 该项存在问题，需排查未完成的迁移
--
-- 幂等性：
--   纯 SELECT 语句，天然幂等，可重复执行。
-- ============================================================

-- 1. 检查金额字段是否已转为 BIGINT
--    如有结果返回，说明金额字段未完全转换（仍存在 DECIMAL/NUMERIC 类型）
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE column_name IN ('total_amount', 'debit_amount', 'credit_amount')
AND data_type != 'bigint'
-- 如果有结果，说明金额字段未完全转换
;

-- 2. 检查状态字段是否已转为 INTEGER
--    如有结果返回，说明状态字段未完全转换（仍存在 VARCHAR 类型）
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE (table_name = 'employees' OR table_name = 'users')
AND column_name = 'status'
AND data_type != 'integer'
-- 如果有结果，说明状态字段未完全转换
;

-- 3. 检查 ID 字段是否已转为 BIGINT
--    如有结果返回，说明 ID 字段未完全转换（仍存在 VARCHAR 类型）
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE column_name IN ('store_id', 'employee_id', 'supplier_id')
AND data_type != 'bigint'
AND table_name NOT IN ('information_schema.columns')
-- 如果有结果，说明 ID 字段未完全转换
;

-- 4. 检查 event_outbox 表是否创建
--    outbox_exists = true 表示 V20260717_033 已成功执行
SELECT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_name = 'event_outbox'
) as outbox_exists;
