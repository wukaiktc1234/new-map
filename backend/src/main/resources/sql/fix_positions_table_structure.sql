-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 修复 positions 表结构，添加缺失的字段

-- 添加 level_id 字段
ALTER TABLE positions ADD COLUMN level_id BIGINT COMMENT '职级ID' AFTER level;

-- 添加 level_type 字段
ALTER TABLE positions ADD COLUMN level_type TINYINT COMMENT '职级类型' AFTER level_id;

-- 添加 sort_order 字段
ALTER TABLE positions ADD COLUMN sort_order INT DEFAULT 0 COMMENT '排序' AFTER level_type;
