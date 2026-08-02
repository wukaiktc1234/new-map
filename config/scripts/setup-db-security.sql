-- ============================================================
-- 食品溯源系统 - 数据库安全配置SQL
-- 创建专用应用用户并限制权限
-- ============================================================

-- 注意: 以 postgres 超级用户身份执行

-- 1. 创建专用应用用户（如果不存在）
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'foodtrace_app') THEN
        CREATE ROLE foodtrace_app WITH LOGIN PASSWORD 'CHANGE_THIS_STRONG_PASSWORD_BEFORE_DEPLOY';
        RAISE NOTICE '用户 foodtrace_app 已创建，请立即修改密码';
    ELSE
        RAISE NOTICE '用户 foodtrace_app 已存在';
    END IF;
END
$$;

-- 2. 授予对业务表的CRUD权限
-- 用户表
GRANT SELECT, INSERT, UPDATE ON TABLE users TO foodtrace_app;
GRANT USAGE, SELECT ON SEQUENCE users_user_id_seq TO foodtrace_app;

-- 角色表
GRANT SELECT ON TABLE roles TO foodtrace_app;
GRANT SELECT ON TABLE user_roles TO foodtrace_app;

-- 权限表
GRANT SELECT ON TABLE permissions TO foodtrace_app;
GRANT SELECT ON TABLE role_permissions TO foodtrace_app;

-- 产品相关表
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE products TO foodtrace_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE categories TO foodtrace_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE product_images TO foodtrace_app;

-- 订单相关表
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE orders TO foodtrace_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE order_items TO foodtrace_app;
GRANT USAGE, SELECT ON SEQUENCE orders_order_id_seq TO foodtrace_app;

-- 库存相关表
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE inventory TO foodtrace_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE inventory_records TO foodtrace_app;

-- 厨房相关表
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE kitchen_orders TO foodtrace_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE kitchen_order_items TO foodtrace_app;

-- 财务相关表
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE finance_records TO foodtrace_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE finance_categories TO foodtrace_app;

-- 审计日志表（只读+插入）
GRANT SELECT, INSERT ON TABLE audit_log TO foodtrace_app;
GRANT SELECT, INSERT ON TABLE operation_log TO foodtrace_app;

-- 会话表
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE user_sessions TO foodtrace_app;

-- 3. 撤销危险权限
REVOKE ALL PRIVILEGES ON SCHEMA public FROM foodtrace_app;
REVOKE CREATE ON SCHEMA public FROM foodtrace_app;

-- 不允许DROP、TRUNCATE、ALTER等DDL操作
-- 应用用户不应有这些权限

-- 4. 设置行级安全策略（可选增强）

-- 5. 创建登录审计函数
CREATE OR REPLACE FUNCTION audit_login_func()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO audit_log (
        operation_type,
        table_name,
        record_id,
        old_data,
        new_data,
        operator_id,
        operation_time,
        ip_address
    ) VALUES (
        'DB_LOGIN',
        'users',
        NULL,
        NULL,
        json_build_object('user', current_user, 'time', now()),
        current_user,
        now(),
        inet_client_addr()
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 6. 显示授权结果
SELECT 
    grantee,
    privilege_type,
    table_name
FROM information_schema.role_table_grants 
WHERE grantee = 'foodtrace_app'
ORDER BY table_name, privilege_type;

RAISE NOTICE '数据库安全配置完成！请记住修改 foodtrace_app 的密码。';
