-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 食品追溯模块权限初始化脚本
-- 执行此脚本后需要重新登录系统

-- 1. 添加食品追溯模块菜单权限
INSERT INTO permissions (
    PERMISSION_CODE, PERMISSION_NAME, PERMISSION_TYPE, MODULE, 
    PARENT_ID, LEVEL, SORT_ORDER, STATUS, ICON, PATH, 
    IS_HIDDEN, IS_CACHE, CREATED_AT, UPDATED_AT, DELETED, ENABLED
) VALUES (
    'traceability', '食品追溯', 1, 'traceability',
    0, 1, 5, 1, 'el-icon-location', '/traceability',
    0, 1, NOW(), NOW(), 0, 1
);

-- 获取刚插入的ID (假设为 @traceability_id)
SET @traceability_id = LAST_INSERT_ID();

-- 2. 添加追溯中心子菜单
INSERT INTO permissions (
    PERMISSION_CODE, PERMISSION_NAME, PERMISSION_TYPE, MODULE, 
    PARENT_ID, LEVEL, SORT_ORDER, STATUS, ICON, PATH, 
    IS_HIDDEN, IS_CACHE, CREATED_AT, UPDATED_AT, DELETED, ENABLED
) VALUES (
    'traceability:view', '追溯中心', 1, 'traceability',
    @traceability_id, 2, 1, 1, 'el-icon-search', '/traceability/query',
    0, 1, NOW(), NOW(), 0, 1
);
SET @view_id = LAST_INSERT_ID();

-- 3. 添加追溯查询页面
INSERT INTO permissions (
    PERMISSION_CODE, PERMISSION_NAME, PERMISSION_TYPE, MODULE, 
    PARENT_ID, LEVEL, SORT_ORDER, STATUS, ICON, PATH, COMPONENT,
    IS_HIDDEN, IS_CACHE, CREATED_AT, UPDATED_AT, DELETED, ENABLED
) VALUES (
    'traceability:query', '追溯查询', 1, 'traceability',
    @view_id, 3, 1, 1, 'el-icon-search', '/traceability/query/summary', 'FoodsafetyTraceability',
    0, 1, NOW(), NOW(), 0, 1
);

-- 4. 添加供应商追溯页面
INSERT INTO permissions (
    PERMISSION_CODE, PERMISSION_NAME, PERMISSION_TYPE, MODULE, 
    PARENT_ID, LEVEL, SORT_ORDER, STATUS, ICON, PATH, COMPONENT,
    IS_HIDDEN, IS_CACHE, CREATED_AT, UPDATED_AT, DELETED, ENABLED
) VALUES (
    'traceability:supplier', '供应商追溯', 1, 'traceability',
    @view_id, 3, 2, 1, 'el-icon-office-building', '/traceability/query/supplier', 'FoodSafetySupplier',
    0, 1, NOW(), NOW(), 0, 1
);

-- 5. 添加质量追溯页面
INSERT INTO permissions (
    PERMISSION_CODE, PERMISSION_NAME, PERMISSION_TYPE, MODULE, 
    PARENT_ID, LEVEL, SORT_ORDER, STATUS, ICON, PATH, COMPONENT,
    IS_HIDDEN, IS_CACHE, CREATED_AT, UPDATED_AT, DELETED, ENABLED
) VALUES (
    'traceability:quality', '质量追溯', 1, 'traceability',
    @traceability_id, 2, 2, 1, 'el-icon-quality', '/traceability/quality', 'FoodsafetyQuality',
    0, 1, NOW(), NOW(), 0, 1
);

-- 6. 添加检验记录页面
INSERT INTO permissions (
    PERMISSION_CODE, PERMISSION_NAME, PERMISSION_TYPE, MODULE, 
    PARENT_ID, LEVEL, SORT_ORDER, STATUS, ICON, PATH, COMPONENT,
    IS_HIDDEN, IS_CACHE, CREATED_AT, UPDATED_AT, DELETED, ENABLED
) VALUES (
    'traceability:inspection', '检验记录', 1, 'traceability',
    @traceability_id, 2, 3, 1, 'el-icon-document', '/traceability/inspection', 'FoodSafetyInspection',
    0, 1, NOW(), NOW(), 0, 1
);

-- 7. 添加原料追溯码页面
INSERT INTO permissions (
    PERMISSION_CODE, PERMISSION_NAME, PERMISSION_TYPE, MODULE, 
    PARENT_ID, LEVEL, SORT_ORDER, STATUS, ICON, PATH, COMPONENT,
    IS_HIDDEN, IS_CACHE, CREATED_AT, UPDATED_AT, DELETED, ENABLED
) VALUES (
    'traceability:material', '原料追溯码', 1, 'traceability',
    @traceability_id, 2, 4, 1, 'el-icon-tickets', '/traceability/material-code', 'TraceCode/MaterialTraceCode',
    0, 1, NOW(), NOW(), 0, 1
);

-- 8. 添加标签模板设计页面
INSERT INTO permissions (
    PERMISSION_CODE, PERMISSION_NAME, PERMISSION_TYPE, MODULE, 
    PARENT_ID, LEVEL, SORT_ORDER, STATUS, ICON, PATH, COMPONENT,
    IS_HIDDEN, IS_CACHE, CREATED_AT, UPDATED_AT, DELETED, ENABLED
) VALUES (
    'traceability:manage', '标签模板设计', 1, 'traceability',
    @traceability_id, 2, 5, 1, 'el-icon-postcard', '/traceability/label-template', 'LabelTemplateDesigner',
    0, 1, NOW(), NOW(), 0, 1
);

-- 9. 为超级管理员角色添加权限
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT 1, ID, NOW() FROM permissions WHERE MODULE = 'traceability';

-- 查询结果确认
SELECT ID, PERMISSION_CODE, PERMISSION_NAME, LEVEL, PATH FROM permissions WHERE MODULE = 'traceability' ORDER BY LEVEL, SORT_ORDER;
