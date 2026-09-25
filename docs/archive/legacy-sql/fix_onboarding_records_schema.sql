-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 修复入职记录表schema，允许resume_id为NULL
-- 这样可以直接创建入职记录而不需要关联简历

ALTER TABLE onboarding_records MODIFY COLUMN resume_id VARCHAR(36) NULL COMMENT '简历ID';
ALTER TABLE onboarding_records MODIFY COLUMN requirement_id VARCHAR(36) NULL COMMENT '招聘需求ID';
