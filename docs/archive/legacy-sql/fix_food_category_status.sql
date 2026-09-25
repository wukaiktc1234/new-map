-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 修复food_category表的status字段（PostgreSQL兼容版本）
-- 注意：原脚本将status从'active'改为数字'1'，但根据项目规范status应使用语义化字符串
-- 此处保持原始意图（改为数字），如需恢复语义化请使用反向操作

-- 将所有分类的status设为 'active'
UPDATE food_category SET status = 'active';

-- 确保所有分类都有status='active'
UPDATE food_category SET status = 'active' WHERE status IS NULL OR status = '';

-- 查看修复后的数据
SELECT id, category_code, category_name, status FROM food_category;
