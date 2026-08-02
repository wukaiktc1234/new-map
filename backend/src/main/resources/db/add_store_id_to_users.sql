-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 添加门店关联字段到用户表
USE food_traceability;

-- 添加store_id字段到users表
ALTER TABLE users 
ADD COLUMN store_id VARCHAR(50) COMMENT '所属门店ID' AFTER department_id;

-- 添加索引以提高查询性能
CREATE INDEX idx_users_store_id ON users(store_id);
