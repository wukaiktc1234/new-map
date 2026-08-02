-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 修复角色代码大小写问题
-- 将大写的角色代码改为小写，以匹配控制器的权限检查

-- 1. 更新角色表中的角色代码为小写
UPDATE ROLES SET ROLE_CODE = 'admin' WHERE ROLE_CODE = 'ADMIN';
UPDATE ROLES SET ROLE_CODE = 'user' WHERE ROLE_CODE = 'USER';
UPDATE ROLES SET ROLE_CODE = 'hr' WHERE ROLE_CODE = 'HR';
UPDATE ROLES SET ROLE_CODE = 'store_manager' WHERE ROLE_CODE = 'STORE_MANAGER';

-- 2. 检查修复结果
SELECT ID, ROLE_CODE, ROLE_NAME, DESCRIPTION FROM ROLES WHERE ROLE_CODE IN ('admin', 'user', 'hr', 'store_manager');