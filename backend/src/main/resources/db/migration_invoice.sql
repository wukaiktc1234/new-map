-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 添加电子发票表缺失字段
ALTER TABLE electronic_invoice 
ADD COLUMN IF NOT EXISTS payee VARCHAR(50) COMMENT '收款人',
ADD COLUMN IF NOT EXISTS checker VARCHAR(50) COMMENT '复核人',
ADD COLUMN IF NOT EXISTS issuer VARCHAR(50) COMMENT '开票人';
