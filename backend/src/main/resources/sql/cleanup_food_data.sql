-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 清理food表的历史数据（PostgreSQL兼容版本）
-- 使用前请确认：此操作将删除food表中的所有数据
-- 建议先备份数据：pg_dump -t food > food_backup_$(date +%Y%m%d).sql

-- 1. 查看当前数据量
SELECT COUNT(*) AS current_count FROM food;

-- 2. 查看最大编码（food表主键为food_code，不是food_id）
SELECT MAX(food_code) AS max_code FROM food;

-- 3. 清空food表（保留表结构）
TRUNCATE TABLE food RESTART IDENTITY;

-- 4. 验证清理结果
SELECT COUNT(*) AS count_after_cleanup FROM food;
