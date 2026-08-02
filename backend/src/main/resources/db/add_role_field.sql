-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 添加role字段到users表
ALTER TABLE users ADD COLUMN role VARCHAR(50) COMMENT '用户角色（admin: 管理员, operator: 操作员, viewer: 查看者）' AFTER status;

-- 更新现有用户的角色
UPDATE users SET role = 'admin' WHERE username = 'admin';
UPDATE users SET role = 'operator' WHERE username = 'user';

-- 为role字段添加索引
CREATE INDEX idx_users_role ON users(role);