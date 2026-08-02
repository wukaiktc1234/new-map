-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 食品溯源系统 - 数据初始化脚本（清理版）
-- 仅保留必要的系统数据，不包含业务模拟数据和测试账号

-- 插入系统基础数据
-- 1. 部门数据（仅保留必要的部门）
INSERT INTO DEPARTMENTS (DEPT_CODE, DEPT_NAME, PARENT_ID, LEVEL, SORT_ORDER, STATUS, DESCRIPTION) VALUES
('DEPT001', '总经办', 0, 1, 1, 1, '公司最高管理部门，负责战略决策和整体运营'),
('DEPT008', '行政部', 0, 1, 8, 1, '负责公司行政事务、人力资源管理和后勤保障');

-- 2. 仓库数据（仅保留一个示例仓库）
INSERT INTO WAREHOUSE (NAME, CODE, TYPE, LOCATION, MANAGER, STATUS) VALUES
('北京主仓库', 'WH-BJ-001', 'main', '北京市朝阳区建国路88号', '系统管理员', 1);

-- 3. 用户数据（仅保留系统管理员）
-- 密码使用BCrypt加密，原始密码为：123456
INSERT INTO USERS (USERNAME, PASSWORD, EMAIL, PHONE, NAME, AVATAR, STATUS, DEPARTMENT_ID, IS_LOCKED, NEED_CHANGE_PASSWORD) VALUES
-- 系统管理员
('admin', '$2a$10$e1cV8Z3t6Y7u8i9o0p1q2r3s4t5u6v7w8x9y0z1A2B3C4D5E6F7G8H9I0J', 'admin@foodtrace.com', '13800138000', '管理员', 'https://picsum.photos/200/200', 1, 1, 0, 0);

-- 4. 角色数据（仅保留基础角色）
INSERT INTO ROLES (ROLE_CODE, ROLE_NAME, DESCRIPTION, ROLE_TYPE, LEVEL, STATUS, IS_SYSTEM) VALUES
('admin', '管理员', '系统管理员，拥有所有权限', 1, 1, 1, 1);

-- 5. 用户角色关联数据
INSERT INTO USER_ROLES (USER_ID, ROLE_ID, CREATED_BY) VALUES
(1, 1, 1);  -- admin拥有admin角色

-- 6. 角色权限关系数据
-- 为admin角色分配所有权限
INSERT INTO ROLE_PERMISSIONS (ROLE_ID, PERMISSION_ID, CREATED_BY) VALUES
(1, 1, 1),  -- admin拥有系统管理菜单权限
(1, 2, 1),  -- admin拥有用户管理菜单权限
(1, 3, 1),  -- admin拥有角色管理菜单权限
(1, 4, 1),  -- admin拥有权限管理菜单权限
(1, 5, 1),  -- admin拥有部门管理菜单权限
(1, 6, 1),  -- admin拥有促销管理菜单权限
(1, 7, 1),  -- admin拥有促销活动管理菜单权限
(1, 8, 1),  -- admin拥有添加活动按钮权限
(1, 9, 1),  -- admin拥有编辑活动按钮权限
(1, 10, 1);  -- admin拥有删除活动按钮权限

-- 数据初始化完成
SELECT '数据初始化完成！仅保留admin系统管理员账号' AS message;