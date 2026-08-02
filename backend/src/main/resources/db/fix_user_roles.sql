-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 为用户ID=12分配管理员角色
-- 执行时机：修复用户权限问题

-- 1. 首先查看当前用户ID=12的角色关联情况
SELECT * FROM user_roles WHERE user_id = 12;

-- 2. 查看roles表中已有的角色
SELECT * FROM roles;

-- 3. 如果用户ID=12没有关联角色，添加ADMIN角色关联
INSERT INTO user_roles (id, user_id, role_id, created_at)
SELECT
    COALESCE((SELECT MAX(id) FROM user_roles) + 1, 1),
    12,
    (SELECT id FROM roles WHERE role_code = 'ADMIN' LIMIT 1),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 12);

-- 4. 或者添加SUPER_ADMIN角色关联
INSERT INTO user_roles (id, user_id, role_id, created_at)
SELECT
    COALESCE((SELECT MAX(id) FROM user_roles) + 1, 1),
    12,
    (SELECT id FROM roles WHERE role_code = 'SUPER_ADMIN' LIMIT 1),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = 12);

-- 5. 验证结果
SELECT u.id, u.username, r.role_code, r.role_name
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN roles r ON ur.role_id = r.id
WHERE u.id = 12;
