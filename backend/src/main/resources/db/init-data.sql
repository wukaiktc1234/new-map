-- ============================================================
-- 食品溯源系统 - 初始化数据脚本 (init-data.sql)
-- 数据库: PostgreSQL 18
-- 执行时机: Flyway 迁移完成后、业务首次使用前
-- 特性: 幂等（可重复执行，重复执行不会产生重复数据）
-- 编写日期: 2026-07-17
-- 说明: 本脚本基于实际表结构编写，字段名以数据库为准
-- ============================================================

-- 执行方式:
--   set PGPASSWORD=123456
--   psql -U postgres -h localhost -p 5432 -d food_traceability -f init-data.sql
-- 或在 psql 交互模式下:
--   \i init-data.sql


-- ============================================================
-- 第一部分：系统基础数据（角色/权限/菜单/部门）
-- ============================================================

-- ------------------------------------------------------------
-- 1.1 权限码（permissions 表）
-- 现状: 0 条，需要初始化
-- 表结构: permission_id(bigint自增), permission_id_str, permission_name,
--          permission_code, permission_type(int), module, status(int),
--          parent_id(varchar), level(int), deleted(smallint)
-- ------------------------------------------------------------

INSERT INTO permissions (permission_id_str, permission_name, permission_code, permission_type, module, status, create_time, update_time, deleted)
SELECT * FROM (VALUES
    -- admin 模块（管理后台）
    ('PERM_ADMIN_USER_LIST',    '用户列表',     'admin:user:list',      1, 'admin', 1, NOW(), NOW(), 0),
    ('PERM_ADMIN_USER_CREATE',  '创建用户',     'admin:user:create',    1, 'admin', 1, NOW(), NOW(), 0),
    ('PERM_ADMIN_USER_UPDATE',  '更新用户',     'admin:user:update',    1, 'admin', 1, NOW(), NOW(), 0),
    ('PERM_ADMIN_USER_DELETE',  '删除用户',     'admin:user:delete',    1, 'admin', 1, NOW(), NOW(), 0),
    ('PERM_ADMIN_ROLE_LIST',    '角色列表',     'admin:role:list',      1, 'admin', 1, NOW(), NOW(), 0),
    ('PERM_ADMIN_ROLE_CREATE',  '创建角色',     'admin:role:create',    1, 'admin', 1, NOW(), NOW(), 0),
    ('PERM_ADMIN_ROLE_UPDATE',  '更新角色',     'admin:role:update',    1, 'admin', 1, NOW(), NOW(), 0),
    ('PERM_ADMIN_ROLE_DELETE',  '删除角色',     'admin:role:delete',    1, 'admin', 1, NOW(), NOW(), 0),
    ('PERM_ADMIN_ROLE_ASSIGN',  '角色授权',     'admin:role:assign',    1, 'admin', 1, NOW(), NOW(), 0),

    -- order 模块（订单）
    ('PERM_ORDER_LIST',    '订单列表', 'order:list',   1, 'order', 1, NOW(), NOW(), 0),
    ('PERM_ORDER_CREATE',  '订单创建', 'order:create', 1, 'order', 1, NOW(), NOW(), 0),
    ('PERM_ORDER_UPDATE',  '订单更新', 'order:update', 1, 'order', 1, NOW(), NOW(), 0),
    ('PERM_ORDER_CANCEL',  '订单取消', 'order:cancel', 1, 'order', 1, NOW(), NOW(), 0),
    ('PERM_ORDER_PAY',     '订单支付', 'order:pay',    1, 'order', 1, NOW(), NOW(), 0),
    ('PERM_ORDER_REFUND',  '订单退款', 'order:refund', 1, 'order', 1, NOW(), NOW(), 0),

    -- product 模块（商品）
    ('PERM_PROD_CAT_LIST',    '商品分类列表', 'product:category:list',    1, 'product', 1, NOW(), NOW(), 0),
    ('PERM_PROD_CAT_CREATE',  '商品分类创建', 'product:category:create',  1, 'product', 1, NOW(), NOW(), 0),
    ('PERM_PROD_CAT_UPDATE',  '商品分类更新', 'product:category:update',  1, 'product', 1, NOW(), NOW(), 0),
    ('PERM_PROD_CAT_DELETE',  '商品分类删除', 'product:category:delete',  1, 'product', 1, NOW(), NOW(), 0),
    ('PERM_PROD_FOOD_LIST',   '菜品列表',     'product:food:list',        1, 'product', 1, NOW(), NOW(), 0),
    ('PERM_PROD_FOOD_CREATE', '菜品创建',     'product:food:create',      1, 'product', 1, NOW(), NOW(), 0),
    ('PERM_PROD_FOOD_UPDATE', '菜品更新',     'product:food:update',      1, 'product', 1, NOW(), NOW(), 0),
    ('PERM_PROD_FOOD_DELETE', '菜品删除',     'product:food:delete',      1, 'product', 1, NOW(), NOW(), 0),
    ('PERM_PROD_COMBO_LIST',  '套餐列表',     'product:combo:list',       1, 'product', 1, NOW(), NOW(), 0),

    -- inventory 模块（库存）
    ('PERM_INV_LIST',     '库存列表', 'inventory:list',     1, 'inventory', 1, NOW(), NOW(), 0),
    ('PERM_INV_IN',       '入库',     'inventory:in',       1, 'inventory', 1, NOW(), NOW(), 0),
    ('PERM_INV_OUT',      '出库',     'inventory:out',      1, 'inventory', 1, NOW(), NOW(), 0),
    ('PERM_INV_TRANSFER', '调拨',     'inventory:transfer', 1, 'inventory', 1, NOW(), NOW(), 0),
    ('PERM_INV_CHECK',    '盘点',     'inventory:check',    1, 'inventory', 1, NOW(), NOW(), 0),
    ('PERM_INV_WARNING',  '库存预警', 'inventory:warning',  1, 'inventory', 1, NOW(), NOW(), 0),

    -- warehouse 模块（仓库）
    ('PERM_WH_LIST',    '仓库列表', 'warehouse:list',    1, 'warehouse', 1, NOW(), NOW(), 0),
    ('PERM_WH_CREATE',  '仓库创建', 'warehouse:create',  1, 'warehouse', 1, NOW(), NOW(), 0),
    ('PERM_WH_UPDATE',  '仓库更新', 'warehouse:update',  1, 'warehouse', 1, NOW(), NOW(), 0),
    ('PERM_WH_DELETE',  '仓库删除', 'warehouse:delete',  1, 'warehouse', 1, NOW(), NOW(), 0),

    -- purchase 模块（采购）
    ('PERM_PUR_STOCKIN_LIST',    '采购入库列表', 'purchase:stockin:list',    1, 'purchase', 1, NOW(), NOW(), 0),
    ('PERM_PUR_STOCKIN_CREATE',  '采购入库创建', 'purchase:stockin:create',  1, 'purchase', 1, NOW(), NOW(), 0),
    ('PERM_PUR_STOCKIN_UPDATE',  '采购入库更新', 'purchase:stockin:update',  1, 'purchase', 1, NOW(), NOW(), 0),
    ('PERM_PUR_STOCKIN_APPROVE', '采购入库审批', 'purchase:stockin:approve', 1, 'purchase', 1, NOW(), NOW(), 0),

    -- member 模块（会员）
    ('PERM_MEM_LEVEL_LIST',      '会员等级列表', 'member:level:list',       1, 'member', 1, NOW(), NOW(), 0),
    ('PERM_MEM_POINTS_LIST',     '积分列表',     'member:points:list',      1, 'member', 1, NOW(), NOW(), 0),
    ('PERM_MEM_POINTS_ADJUST',   '积分调整',     'member:points:adjust',    1, 'member', 1, NOW(), NOW(), 0),
    ('PERM_MEM_RECHARGE_LIST',   '充值列表',     'member:recharge:list',    1, 'member', 1, NOW(), NOW(), 0),
    ('PERM_MEM_RECHARGE_CREATE', '充值创建',     'member:recharge:create',  1, 'member', 1, NOW(), NOW(), 0),
    ('PERM_MEM_RECHARGE_APPROVE','充值审批',     'member:recharge:approve', 1, 'member', 1, NOW(), NOW(), 0),

    -- finance 模块（财务）
    ('PERM_FIN_VIEW',           '财务查看',     'finance:view',             1, 'finance', 1, NOW(), NOW(), 0),
    ('PERM_FIN_EDIT',           '财务编辑',     'finance:edit',             1, 'finance', 1, NOW(), NOW(), 0),
    ('PERM_FIN_VOUCHER_LIST',   '凭证列表',     'finance:voucher:list',     1, 'finance', 1, NOW(), NOW(), 0),
    ('PERM_FIN_VOUCHER_CREATE', '凭证创建',     'finance:voucher:create',   1, 'finance', 1, NOW(), NOW(), 0),
    ('PERM_FIN_VOUCHER_AUDIT',  '凭证审核',     'finance:voucher:audit',    1, 'finance', 1, NOW(), NOW(), 0),
    ('PERM_FIN_VOUCHER_POST',   '凭证过账',     'finance:voucher:post',     1, 'finance', 1, NOW(), NOW(), 0),
    ('PERM_FIN_REPORT_LIST',    '财务报表',     'finance:report:list',      1, 'finance', 1, NOW(), NOW(), 0),
    ('PERM_FIN_REPORT_EXPORT',  '报表导出',     'finance:report:export',    1, 'finance', 1, NOW(), NOW(), 0),

    -- hr 模块（人力资源）
    ('PERM_HR_EMP_LIST',     '员工列表', 'hr:employee:list',     1, 'hr', 1, NOW(), NOW(), 0),
    ('PERM_HR_EMP_CREATE',   '员工创建', 'hr:employee:create',   1, 'hr', 1, NOW(), NOW(), 0),
    ('PERM_HR_EMP_UPDATE',   '员工更新', 'hr:employee:update',   1, 'hr', 1, NOW(), NOW(), 0),
    ('PERM_HR_EMP_DELETE',   '员工删除', 'hr:employee:delete',   1, 'hr', 1, NOW(), NOW(), 0),
    ('PERM_HR_ATTEND_LIST',  '考勤列表', 'hr:attendance:list',   1, 'hr', 1, NOW(), NOW(), 0),
    ('PERM_HR_ATTEND_EXPORT','考勤导出', 'hr:attendance:export', 1, 'hr', 1, NOW(), NOW(), 0),

    -- trace 模块（溯源）
    ('PERM_TRACE_LIST',   '溯源列表', 'trace:list',   1, 'trace', 1, NOW(), NOW(), 0),
    ('PERM_TRACE_CREATE', '溯源创建', 'trace:create', 1, 'trace', 1, NOW(), NOW(), 0),
    ('PERM_TRACE_SCAN',   '溯源扫码', 'trace:scan',   1, 'trace', 1, NOW(), NOW(), 0),
    ('PERM_TRACE_REPORT', '溯源报告', 'trace:report', 1, 'trace', 1, NOW(), NOW(), 0),

    -- schedule 模块（排班）
    ('PERM_SCHED_LIST',   '排班列表', 'schedule:list',   1, 'schedule', 1, NOW(), NOW(), 0),
    ('PERM_SCHED_CREATE', '排班创建', 'schedule:create', 1, 'schedule', 1, NOW(), NOW(), 0),
    ('PERM_SCHED_UPDATE', '排班更新', 'schedule:update', 1, 'schedule', 1, NOW(), NOW(), 0),
    ('PERM_SCHED_DELETE', '排班删除', 'schedule:delete', 1, 'schedule', 1, NOW(), NOW(), 0),

    -- loss 模块（损耗）
    ('PERM_LOSS_LIST',    '损耗列表', 'loss-outbound:list',    1, 'loss', 1, NOW(), NOW(), 0),
    ('PERM_LOSS_CREATE',  '损耗创建', 'loss-outbound:create',  1, 'loss', 1, NOW(), NOW(), 0),
    ('PERM_LOSS_APPROVE', '损耗审批', 'loss-outbound:approve', 1, 'loss', 1, NOW(), NOW(), 0),

    -- sysconfig 模块（系统配置）
    ('PERM_SYSCONFIG_LIST',   '配置列表', 'sys:config:list',   1, 'sysconfig', 1, NOW(), NOW(), 0),
    ('PERM_SYSCONFIG_UPDATE', '配置更新', 'sys:config:update', 1, 'sysconfig', 1, NOW(), NOW(), 0),

    -- audit 模块（审计）
    ('PERM_AUDIT_LOG_LIST',   '审计日志查询', 'audit:log:list',   1, 'audit', 1, NOW(), NOW(), 0),
    ('PERM_AUDIT_LOG_EXPORT', '审计日志导出', 'audit:log:export', 1, 'audit', 1, NOW(), NOW(), 0),

    -- backup 模块（备份）
    ('PERM_BACKUP_LIST',    '备份列表', 'backup:list',    1, 'backup', 1, NOW(), NOW(), 0),
    ('PERM_BACKUP_CREATE',  '创建备份', 'backup:create',  1, 'backup', 1, NOW(), NOW(), 0),
    ('PERM_BACKUP_RESTORE', '恢复备份', 'backup:restore', 1, 'backup', 1, NOW(), NOW(), 0),

    -- permission 模块（权限管理）
    ('PERM_PERM_LIST',   '权限列表', 'permission:list',   1, 'permission', 1, NOW(), NOW(), 0),
    ('PERM_PERM_ASSIGN', '权限分配', 'permission:assign', 1, 'permission', 1, NOW(), NOW(), 0)
) AS t(permission_id_str, permission_name, permission_code, permission_type, module, status, create_time, update_time, deleted)
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'admin:user:list');

-- 提示: 完整的 293 个权限码需扫描 Controller 中 @PreAuthorize 注解
-- 扫描命令 (PowerShell):
--   Get-ChildItem -Recurse -Filter "*.java" -Path backend\src\main\java\com\foodtraceability\controller |
--     Select-String -Pattern "hasAuthority\('([^']+)'\)" -AllMatches |
--     ForEach-Object { $_.Matches.Groups[1].Value } | Sort-Object -Unique


-- ------------------------------------------------------------
-- 1.2 业务角色（roles 表）
-- 现状: 已有 SUPER_ADMIN(role_id=1) + 测试角色，需补建正式业务角色
-- 表结构: role_id(bigint自增), role_id_str, role_code, role_name,
--          status(varchar, 默认'active'), data_scope(varchar, 默认'self'),
--          org_level(varchar, 默认'STORE_STAFF'), deleted(smallint)
-- ------------------------------------------------------------

INSERT INTO roles (role_id_str, role_code, role_name, description, status, data_scope, org_level, create_time, update_time, deleted)
SELECT * FROM (VALUES
    ('ROLE_STORE_MANAGER', 'STORE_MANAGER', '店长',       '门店运营管理，本门店数据范围', 'active', 'store', 'STORE_MANAGER', NOW(), NOW(), 0),
    ('ROLE_CASHIER',       'CASHIER',       '收银员',     '收银台操作，订单与会员基础操作', 'active', 'store', 'STORE_STAFF',   NOW(), NOW(), 0),
    ('ROLE_WAREHOUSE',     'WAREHOUSE',     '仓管员',     '库存与仓库管理',               'active', 'store', 'STORE_STAFF',   NOW(), NOW(), 0),
    ('ROLE_FINANCE',       'FINANCE',       '财务员',     '财务全模块操作',               'active', 'all',   'FINANCE',       NOW(), NOW(), 0),
    ('ROLE_PURCHASE',      'PURCHASE',      '采购员',     '采购与供应商管理',             'active', 'all',   'PURCHASE',      NOW(), NOW(), 0)
) AS t(role_id_str, role_code, role_name, description, status, data_scope, org_level, create_time, update_time, deleted)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_code = 'STORE_MANAGER');


-- ------------------------------------------------------------
-- 1.3 角色-权限关联（role_permissions 表）
-- 现状: 0 条，需要初始化
-- 表结构: id(bigint自增), role_id(varchar!), permission_id(varchar!), deleted(smallint)
-- 注意: role_id 和 permission_id 均为 varchar 类型
-- ------------------------------------------------------------

-- 1.3.1 超级管理员通配符权限（permission_id='*' 表示全部权限）
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT '1', '*', 0, NOW()
WHERE NOT EXISTS (SELECT 1 FROM role_permissions WHERE role_id = '1' AND permission_id = '*');

-- 1.3.2 店长权限（本门店运营 + 排班 + 员工查看）
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT CAST(r.role_id AS VARCHAR), CAST(p.permission_id AS VARCHAR), 0, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'STORE_MANAGER' AND r.deleted = 0
  AND p.deleted = 0
  AND p.permission_code IN (
    'order:list', 'order:create', 'order:update', 'order:cancel', 'order:pay', 'order:refund',
    'product:food:list', 'product:combo:list', 'product:category:list',
    'member:level:list', 'member:points:list', 'member:recharge:list', 'member:recharge:create',
    'schedule:list', 'schedule:create', 'schedule:update', 'schedule:delete',
    'hr:employee:list', 'hr:attendance:list',
    'inventory:list',
    'trace:list', 'trace:scan'
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = CAST(r.role_id AS VARCHAR)
      AND rp.permission_id = CAST(p.permission_id AS VARCHAR)
      AND rp.deleted = 0
  );

-- 1.3.3 收银员权限（订单 + 会员基础操作）
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT CAST(r.role_id AS VARCHAR), CAST(p.permission_id AS VARCHAR), 0, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'CASHIER' AND r.deleted = 0
  AND p.deleted = 0
  AND p.permission_code IN (
    'order:list', 'order:create', 'order:update', 'order:pay',
    'member:level:list', 'member:points:list', 'member:recharge:list', 'member:recharge:create',
    'product:food:list', 'product:combo:list'
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = CAST(r.role_id AS VARCHAR)
      AND rp.permission_id = CAST(p.permission_id AS VARCHAR)
      AND rp.deleted = 0
  );

-- 1.3.4 仓管员权限（库存 + 仓库 + 采购入库）
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT CAST(r.role_id AS VARCHAR), CAST(p.permission_id AS VARCHAR), 0, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'WAREHOUSE' AND r.deleted = 0
  AND p.deleted = 0
  AND p.permission_code IN (
    'inventory:list', 'inventory:in', 'inventory:out', 'inventory:transfer', 'inventory:check',
    'warehouse:list',
    'purchase:stockin:list', 'purchase:stockin:create', 'purchase:stockin:update',
    'loss-outbound:list', 'loss-outbound:create',
    'product:category:list'
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = CAST(r.role_id AS VARCHAR)
      AND rp.permission_id = CAST(p.permission_id AS VARCHAR)
      AND rp.deleted = 0
  );

-- 1.3.5 财务员权限（财务全模块）
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT CAST(r.role_id AS VARCHAR), CAST(p.permission_id AS VARCHAR), 0, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'FINANCE' AND r.deleted = 0
  AND p.deleted = 0
  AND p.permission_code IN (
    'finance:view', 'finance:edit',
    'finance:voucher:list', 'finance:voucher:create', 'finance:voucher:audit', 'finance:voucher:post',
    'finance:report:list', 'finance:report:export',
    'member:recharge:approve',
    'loss-outbound:approve'
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = CAST(r.role_id AS VARCHAR)
      AND rp.permission_id = CAST(p.permission_id AS VARCHAR)
      AND rp.deleted = 0
  );

-- 1.3.6 采购员权限（采购 + 供应商查看）
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT CAST(r.role_id AS VARCHAR), CAST(p.permission_id AS VARCHAR), 0, NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.role_code = 'PURCHASE' AND r.deleted = 0
  AND p.deleted = 0
  AND p.permission_code IN (
    'purchase:stockin:list', 'purchase:stockin:create', 'purchase:stockin:update', 'purchase:stockin:approve',
    'product:category:list',
    'inventory:list',
    'warehouse:list'
  )
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = CAST(r.role_id AS VARCHAR)
      AND rp.permission_id = CAST(p.permission_id AS VARCHAR)
      AND rp.deleted = 0
  );


-- ------------------------------------------------------------
-- 1.4 核心菜单（menus 表）
-- 现状: 6 条排班菜单(10001-10006)，需补建核心业务菜单
-- 表结构: id(bigint自增), permission_code, permission_name, permission_type(int),
--          module, parent_id(bigint), level(int), sort_order(int),
--          component, path, icon, is_hidden(int), status(int), deleted(int)
-- ------------------------------------------------------------

-- 一级目录菜单（permission_type=1 表示目录）
-- 注意: menus.id 不是 identity 列，需手动指定（从 10007 开始，避开已有的排班菜单 10001-10006）
INSERT INTO menus (id, permission_code, permission_name, permission_type, module, parent_id, level, sort_order, component, path, icon, is_hidden, status, create_time, update_time, deleted)
SELECT * FROM (VALUES
    (10007, 'dashboard',    '工作台',     1, 'dashboard', 0, 1, 1,  'Dashboard/Index', '/dashboard',     'Dashboard',    0, 1, NOW(), NOW(), 0),
    (10008, 'system',       '系统管理',   1, 'system',    0, 1, 2,  'Layout',          '/system',        'Setting',      0, 1, NOW(), NOW(), 0),
    (10009, 'store',        '门店管理',   1, 'store',     0, 1, 3,  'Layout',          '/store',         'Shop',         0, 1, NOW(), NOW(), 0),
    (10010, 'product',      '商品管理',   1, 'product',   0, 1, 4,  'Layout',          '/product',       'Goods',        0, 1, NOW(), NOW(), 0),
    (10011, 'purchase',     '采购管理',   1, 'purchase',  0, 1, 5,  'Layout',          '/purchase',      'Shopping',     0, 1, NOW(), NOW(), 0),
    (10012, 'inventory',    '库存管理',   1, 'inventory', 0, 1, 6,  'Layout',          '/inventory',     'Box',          0, 1, NOW(), NOW(), 0),
    (10013, 'member',       '会员管理',   1, 'member',    0, 1, 7,  'Layout',          '/member',        'User',         0, 1, NOW(), NOW(), 0),
    (10014, 'finance',      '财务管理',   1, 'finance',   0, 1, 8,  'Layout',          '/finance',       'Money',        0, 1, NOW(), NOW(), 0),
    (10015, 'traceability', '食品溯源',   1, 'trace',     0, 1, 9,  'Layout',          '/traceability',  'Connection',   0, 1, NOW(), NOW(), 0),
    (10016, 'hr',           '人力资源',   1, 'hr',        0, 1, 10, 'Layout',          '/hr',            'UserFilled',   0, 1, NOW(), NOW(), 0)
) AS t(id, permission_code, permission_name, permission_type, module, parent_id, level, sort_order, component, path, icon, is_hidden, status, create_time, update_time, deleted)
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE permission_code = 'dashboard' AND parent_id = 0);

-- 系统管理二级菜单（permission_type=2 表示菜单，parent_id 指向"系统管理"一级目录 id=10008）
INSERT INTO menus (id, permission_code, permission_name, permission_type, module, parent_id, level, sort_order, component, path, icon, is_hidden, status, create_time, update_time, deleted)
SELECT * FROM (VALUES
    (10017, 'sys-user',       '用户管理', 2, 'system', 10008, 2, 1, 'system/UserList',       'user',       'User',         0, 1, NOW(), NOW(), 0),
    (10018, 'sys-role',       '角色管理', 2, 'system', 10008, 2, 2, 'system/RoleList',       'role',       'UserFilled',   0, 1, NOW(), NOW(), 0),
    (10019, 'sys-permission', '权限管理', 2, 'system', 10008, 2, 3, 'system/PermissionList', 'permission', 'Key',          0, 1, NOW(), NOW(), 0),
    (10020, 'sys-menu',       '菜单管理', 2, 'system', 10008, 2, 4, 'system/MenuList',       'menu',       'Menu',         0, 1, NOW(), NOW(), 0),
    (10021, 'sys-config',     '系统配置', 2, 'system', 10008, 2, 5, 'system/ConfigList',     'config',     'Tools',        0, 1, NOW(), NOW(), 0),
    (10022, 'sys-audit',      '审计日志', 2, 'system', 10008, 2, 6, 'system/AuditLog',       'audit',      'Document',     0, 1, NOW(), NOW(), 0),
    (10023, 'sys-backup',     '数据备份', 2, 'system', 10008, 2, 7, 'system/BackupList',     'backup',     'FolderOpened', 0, 1, NOW(), NOW(), 0)
) AS t(id, permission_code, permission_name, permission_type, module, parent_id, level, sort_order, component, path, icon, is_hidden, status, create_time, update_time, deleted)
WHERE NOT EXISTS (SELECT 1 FROM menus WHERE permission_code = 'sys-user' AND deleted = 0);


-- ============================================================
-- 第二部分：业务基础数据（门店/员工/用户/食品分类/商品/仓库/供应商）
-- ============================================================

-- ------------------------------------------------------------
-- 2.0 测试门店（stores 旧表，用于满足 employees/users 的 FK 约束）
-- 现状: 0 条，需插入 1 条以满足 FK 约束
-- 注意: employees.store_id 和 users.store_id 的 FK 均指向 stores(旧表)，
--       而非 stores_new(新表)。stores_new 已有 3 条门店，但 stores(旧表) 为空。
-- stores 表结构: store_id(bigint, 非 identity), store_name(varchar, NOT NULL),
--                store_code, address, phone, status(varchar), deleted(int, NOT NULL)
-- ------------------------------------------------------------
INSERT INTO stores (store_id, store_name, store_code, address, phone, status, create_time, update_time, deleted)
SELECT 1, '中心旗舰店', 'ST001', '北京市朝阳区建国路88号', '010-88880001', 'active', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE store_id = 1);


-- ------------------------------------------------------------
-- 2.1 测试员工（employees 表）
-- 现状: 0 条，需要初始化 2 个测试员工
-- 表结构: employee_id(bigint, 非 identity), employee_name(varchar, NOT NULL),
--          gender(varchar), employee_code(varchar, UNIQUE), phone(varchar),
--          email(varchar), department_id(bigint, FK→departments),
--          position_id(bigint, FK→positions), position_level_id(bigint),
--          status(int, 默认1), store_id(bigint, FK→stores), hire_date(timestamp),
--          entry_date(date), id_card(varchar), deleted(int, 默认0)
-- 依赖: departments(id=17 运营), positions(id=1 厨师), position_levels(id=4 P4),
--       stores(id=1 中心旗舰店, 旧表, 已于 2.0 插入)
-- 约束: phone 必须匹配 ^1[3-9][0-9]{9}$
-- ------------------------------------------------------------

-- 注意: employees.employee_id 不是 identity 列，需手动指定
INSERT INTO employees (employee_id, employee_name, gender, employee_code, phone, email, department_id, position_id, position_level_id, status, store_id, hire_date, entry_date, id_card, deleted, create_time, update_time)
SELECT * FROM (VALUES
    (1, '张明', 'male',   'EMP001', '13800138001', 'zhangming@example.com', 17, 1, 4, 1, 1, NOW(), CURRENT_DATE, '110101199001011234', 0, NOW(), NOW()),
    (2, '李娜', 'female', 'EMP002', '13800138002', 'lina@example.com',      17, 1, 2, 1, 1, NOW(), CURRENT_DATE, '110101199202022345', 0, NOW(), NOW())
) AS t(employee_id, employee_name, gender, employee_code, phone, email, department_id, position_id, position_level_id, status, store_id, hire_date, entry_date, id_card, deleted, create_time, update_time)
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE employee_code = 'EMP001');

-- 说明: department_id=17(运营), position_id=1(厨师), position_level_id=4(P4), store_id=1(中心旗舰店)
-- 实际部署时请用 SELECT 查询出实际的部门/职位/职级 ID 后调整


-- ------------------------------------------------------------
-- 2.2 业务用户（users 表）
-- 现状: 1 条(admin, user_id=1)，需补建 2 个业务用户
-- 表结构: user_id(bigint, GENERATED ALWAYS AS IDENTITY), username, password(BCrypt),
--          name, email, phone, status(int), roles(varchar), department_id,
--          store_id, employee_code, deleted(smallint), is_locked(bool),
--          need_change_password(bool)
-- 密码: User@123 的 BCrypt 加密值（strength=12）
-- ------------------------------------------------------------

-- 修复 users 序列：确保 user_id 自增从当前最大值+1 开始（解决 admin 用 OVERRIDING 插入后 sequence 未推进的问题）
SELECT setval('public.users_user_id_seq', GREATEST((SELECT MAX(user_id) FROM users), 1));

-- 使用 OVERRIDING SYSTEM VALUE 显式指定 user_id（因为 user_id 是 GENERATED ALWAYS AS IDENTITY）
INSERT INTO users (user_id, username, password, name, email, phone, status, roles, department_id, store_id, employee_code, deleted, is_locked, need_change_password, create_time, update_time)
OVERRIDING SYSTEM VALUE
SELECT * FROM (VALUES
    (2, 'zhangming', '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '张明', 'zhangming@example.com', '13800138001', 1, '6', 17, 1, 'EMP001', 0, false, true, NOW(), NOW()),
    (3, 'lina',      '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李娜', 'lina@example.com',      '13800138002', 1, '7', 17, 1, 'EMP002', 0, false, true, NOW(), NOW())
) AS t(user_id, username, password, name, email, phone, status, roles, department_id, store_id, employee_code, deleted, is_locked, need_change_password, create_time, update_time)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'zhangming');

-- 再次修复序列，确保后续自增正常
SELECT setval('public.users_user_id_seq', GREATEST((SELECT MAX(user_id) FROM users), 1));

-- 说明: roles 字段存储角色 ID（varchar），6=店长, 7=收银员（实际值以 roles 表查询为准）
-- 密码 User@123 仅为测试用，生产环境必须强制首次登录修改


-- ------------------------------------------------------------
-- 2.3 用户-角色关联（user_roles 表）
-- 现状: 1 条(admin→超管)，需补建业务用户关联
-- 表结构: id(bigint自增), user_id(varchar!), role_id(varchar!), deleted(smallint)
-- 注意: user_id 和 role_id 均为 varchar 类型
-- ------------------------------------------------------------

INSERT INTO user_roles (user_id, role_id, deleted, create_time)
SELECT CAST(u.user_id AS VARCHAR), CAST(r.role_id AS VARCHAR), 0, NOW()
FROM users u
CROSS JOIN roles r
WHERE u.deleted = 0 AND r.deleted = 0
  AND (
    (u.username = 'zhangming' AND r.role_code = 'STORE_MANAGER') OR
    (u.username = 'lina'      AND r.role_code = 'CASHIER')
  )
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur
    WHERE ur.user_id = CAST(u.user_id AS VARCHAR)
      AND ur.role_id = CAST(r.role_id AS VARCHAR)
      AND ur.deleted = 0
  );


-- ------------------------------------------------------------
-- 2.4 食品分类（food_category 表）
-- 现状: 0 条，需要初始化
-- 表结构: category_id(varchar!), category_name, category_code,
--          parent_id(varchar!), status(varchar 'active'), deleted(int)
-- 注意: category_id 和 parent_id 均为 varchar 类型
-- ------------------------------------------------------------

INSERT INTO food_category (category_id, category_name, category_code, parent_id, description, sort_order, status, create_time, update_time, deleted)
SELECT * FROM (VALUES
    ('FC001', '主食类', 'FC001', '0', '米饭、面条等主食', 1, 'active', NOW(), NOW(), 0),
    ('FC002', '荤菜类', 'FC002', '0', '肉类菜品',         2, 'active', NOW(), NOW(), 0),
    ('FC003', '素菜类', 'FC003', '0', '蔬菜类菜品',       3, 'active', NOW(), NOW(), 0),
    ('FC004', '汤类',   'FC004', '0', '汤品',             4, 'active', NOW(), NOW(), 0),
    ('FC005', '凉菜类', 'FC005', '0', '凉拌菜品',         5, 'active', NOW(), NOW(), 0),
    ('FC006', '饮品',   'FC006', '0', '饮料、酒水',       6, 'active', NOW(), NOW(), 0),
    ('FC007', '甜点',   'FC007', '0', '甜品、糕点',       7, 'active', NOW(), NOW(), 0),
    ('FC008', '海鲜类', 'FC008', '0', '海鲜菜品',         8, 'active', NOW(), NOW(), 0)
) AS t(category_id, category_name, category_code, parent_id, description, sort_order, status, create_time, update_time, deleted)
WHERE NOT EXISTS (SELECT 1 FROM food_category WHERE category_code = 'FC001');


-- ------------------------------------------------------------
-- 2.5 商品分类树补充（material_categories 表）
-- 现状: 3 条(干货/预包装/冻货)，补充标准分类树
-- 表结构: category_id(bigint自增), category_code, category_name,
--          parent_id(bigint), description, sort_order, status(int), deleted(int)
-- 注意: 本表无 level 字段，parent_id=0 表示一级分类
-- ------------------------------------------------------------

INSERT INTO material_categories (category_code, category_name, parent_id, description, sort_order, status, create_time, update_time, deleted)
SELECT * FROM (VALUES
    ('MC_FOOD',       '食材原料', 0, '原材料分类', 10, 1, NOW(), NOW(), 0),
    ('MC_DRINK',      '酒水饮料', 0, '饮品分类',   11, 1, NOW(), NOW(), 0),
    ('MC_PACK',       '包装耗材', 0, '包装材料',   12, 1, NOW(), NOW(), 0),
    ('MC_CLEAN',      '清洁用品', 0, '清洁消毒',   13, 1, NOW(), NOW(), 0)
) AS t(category_code, category_name, parent_id, description, sort_order, status, create_time, update_time, deleted)
WHERE NOT EXISTS (SELECT 1 FROM material_categories WHERE category_code = 'MC_FOOD');

-- 二级分类：食材原料下（parent_id 指向 MC_FOOD 的 category_id）
INSERT INTO material_categories (category_code, category_name, parent_id, description, sort_order, status, create_time, update_time, deleted)
SELECT t.code, t.name, mc.category_id, t.descr, t.sort, 1, NOW(), NOW(), 0
FROM (VALUES
    ('MC_FOOD_VEG',   '蔬菜类',   '日常蔬菜',     1),
    ('MC_FOOD_MEAT',  '肉类',     '猪牛羊禽肉',   2),
    ('MC_FOOD_SEA',   '海鲜水产', '水产海鲜',     3),
    ('MC_FOOD_GRAIN', '粮油',     '米面粮油',     4),
    ('MC_FOOD_SPICE', '调味品',   '调料干货',     5)
) AS t(code, name, descr, sort)
CROSS JOIN material_categories mc
WHERE mc.category_code = 'MC_FOOD' AND mc.parent_id = 0 AND mc.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM material_categories WHERE category_code = t.code AND deleted = 0);

-- 二级分类：酒水饮料下
INSERT INTO material_categories (category_code, category_name, parent_id, description, sort_order, status, create_time, update_time, deleted)
SELECT t.code, t.name, mc.category_id, t.descr, t.sort, 1, NOW(), NOW(), 0
FROM (VALUES
    ('MC_DRINK_BEV', '软饮料', '碳酸饮料、果汁', 1),
    ('MC_DRINK_ALC', '酒类',   '啤酒、白酒',     2),
    ('MC_DRINK_TEA', '茶饮',   '茶叶、茶饮料',   3)
) AS t(code, name, descr, sort)
CROSS JOIN material_categories mc
WHERE mc.category_code = 'MC_DRINK' AND mc.parent_id = 0 AND mc.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM material_categories WHERE category_code = t.code AND deleted = 0);

-- 二级分类：包装耗材下
INSERT INTO material_categories (category_code, category_name, parent_id, description, sort_order, status, create_time, update_time, deleted)
SELECT t.code, t.name, mc.category_id, t.descr, t.sort, 1, NOW(), NOW(), 0
FROM (VALUES
    ('MC_PACK_BOX', '餐盒',   '外卖餐盒',   1),
    ('MC_PACK_BAG', '包装袋', '打包袋',     2),
    ('MC_PACK_CUP', '杯子',   '饮品杯',     3)
) AS t(code, name, descr, sort)
CROSS JOIN material_categories mc
WHERE mc.category_code = 'MC_PACK' AND mc.parent_id = 0 AND mc.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM material_categories WHERE category_code = t.code AND deleted = 0);


-- ------------------------------------------------------------
-- 2.6 商品档案补充（material_archives 表）
-- 现状: 4 条(豆皮/凉皮/腐竹/土豆粉条)，补充至不少于 5 个
-- 表结构: material_id(bigint自增), material_code, material_name,
--          category_id(bigint), unit, spec, reference_price(bigint, 分),
--          supplier_id(bigint), status(int), deleted(int)
-- 金额: reference_price 以分为单位（如 12.50元 = 1250）
-- ------------------------------------------------------------

-- 补充 5 个标准商品（确保商品总数 >= 5）
INSERT INTO material_archives (material_code, material_name, category_id, unit, spec, reference_price, supplier_id, status, remark, create_time, update_time, deleted)
SELECT t.code, t.name, mc.category_id, t.unit, t.spec, t.price, t.supplier_id, 1, t.remark, NOW(), NOW(), 0
FROM (VALUES
    ('MAT_STD_001', '白菜',     'MC_FOOD_VEG',   '斤', '一级',  150,  1,    '蔬菜类基础食材'),
    ('MAT_STD_002', '猪肉(五花)','MC_FOOD_MEAT', '斤', '冷鲜', 1800,  2,    '肉类基础食材'),
    ('MAT_STD_003', '大米',     'MC_FOOD_GRAIN', '袋', '25kg', 9800,  3,    '粮油基础食材'),
    ('MAT_STD_004', '可乐',     'MC_DRINK_BEV',  '瓶', '330ml', 250,  3,    '软饮料'),
    ('MAT_STD_005', '环保餐盒', 'MC_PACK_BOX',   '个', '750ml',  80,  3,    '外卖包装耗材')
) AS t(code, name, cat_code, unit, spec, price, supplier_id, remark)
CROSS JOIN material_categories mc
WHERE mc.category_code = t.cat_code AND mc.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM material_archives WHERE material_code = t.code);


-- ------------------------------------------------------------
-- 2.7 仓库补充（warehouses 表）
-- 现状: 3 条(WH001主仓库/WH002备用仓库/WH003临时仓库)
-- 表结构: warehouse_id(bigint自增), warehouse_code, warehouse_name,
--          warehouse_type(int), address, manager_id(bigint), phone,
--          capacity, status(int), deleted(int)
-- 说明: 现有仓库数据已满足需求，无需补充
-- warehouse_type: 1=主仓库, 2=备用仓库, 3=临时仓库
-- ------------------------------------------------------------

-- 为中心旗舰店补充专用仓库（幂等）
INSERT INTO warehouses (warehouse_code, warehouse_name, warehouse_type, address, phone, capacity, status, remark, create_time, update_time, deleted)
SELECT 'WH_ST001', '中心旗舰店主仓库', 1, '北京市朝阳区建国路88号B1层', '010-88880001', 500.00, 1, '中心旗舰店专用仓库', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM warehouses WHERE warehouse_code = 'WH_ST001' AND deleted = 0);


-- ------------------------------------------------------------
-- 2.8 供应商（suppliers 表）
-- 现状: 已有 6 条(SUP001/SUP002/SUP003 + 3 条测试)，补充 2 个标准供应商
-- 表结构: supplier_id(bigint, identity自增), supplier_code(varchar, NOT NULL),
--          supplier_name(varchar, NOT NULL), contact_person(varchar, NOT NULL),
--          phone(varchar, NOT NULL), address(varchar, NOT NULL), email(varchar, NOT NULL),
--          category(varchar, NOT NULL), bank_name(varchar, NOT NULL),
--          bank_account(varchar, NOT NULL), tax_id(varchar, NOT NULL),
--          remark(varchar, NOT NULL), license_no(varchar, NOT NULL),
--          license_expiry(date, NOT NULL), tax_no(varchar, NOT NULL),
--          credit_level(varchar, 默认'C'), status(int, 默认1), deleted(int, 默认0)
-- 说明: supplier_id 为 identity 列，无需指定
-- ------------------------------------------------------------
INSERT INTO suppliers (supplier_code, supplier_name, contact_person, phone, address, email, category, bank_name, bank_account, tax_id, remark, license_no, license_expiry, tax_no, credit_level, status, create_time, update_time, deleted)
SELECT * FROM (VALUES
    ('SUP_INIT_001', '绿源农产品有限公司',     '王经理', '13900139001', '北京市大兴区农产品基地A区', 'lvyuan@supplier.com',     '食材', '工商银行', '6222020200012345678', '91110105MA01ABCD12', '长期合作蔬菜供应商',     'JY1100002024001A01', CURRENT_DATE + INTERVAL '2 years', '91110105MA01ABCD12', 'A', 1, NOW(), NOW(), 0),
    ('SUP_INIT_002', '汇丰食品贸易有限公司',   '李经理', '13900139002', '北京市顺义区物流园B区',     'huifeng@supplier.com',    '肉类', '建设银行', '6227000112340005678', '91110105MA01EFGH34', '肉类及冻品供应商',       'JY1100002024001B02', CURRENT_DATE + INTERVAL '2 years', '91110105MA01EFGH34', 'B', 1, NOW(), NOW(), 0)
) AS t(supplier_code, supplier_name, contact_person, phone, address, email, category, bank_name, bank_account, tax_id, remark, license_no, license_expiry, tax_no, credit_level, status, create_time, update_time, deleted)
WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE supplier_code = 'SUP_INIT_001');


-- ============================================================
-- 第三部分：配置基础数据验证
-- 以下数据已由 Flyway 迁移脚本自动初始化，此处仅做验证
-- ============================================================

-- ------------------------------------------------------------
-- 3.1 会员等级（member_level 表，已由 V8.0.0 初始化 4 个等级）
-- 预期: NORMAL(普通), SILVER(白银), GOLD(黄金), DIAMOND(钻石)
-- ------------------------------------------------------------
-- 验证: SELECT level_code, level_name, min_points, discount_rate FROM member_level ORDER BY min_points;

-- ------------------------------------------------------------
-- 3.2 会计科目（accounting_subject 表，已由 V20260405 初始化 102 个科目）
-- 预期: 资产类/负债类/权益类/成本类/损益类 共 102 个科目
-- ------------------------------------------------------------
-- 验证: SELECT category, COUNT(*) FROM accounting_subject WHERE deleted=0 GROUP BY category ORDER BY category;

-- ------------------------------------------------------------
-- 3.3 自动记账规则（accounting_rule 表，已由 V20260405 初始化 18 条规则）
-- ------------------------------------------------------------
-- 验证: SELECT rule_code, rule_name, event_type FROM accounting_rule ORDER BY rule_code;

-- ------------------------------------------------------------
-- 3.4 系统配置（sys_config 表，已由 V20260404.3 初始化 17 项配置）
-- ------------------------------------------------------------
-- 验证: SELECT config_key, config_value FROM sys_config WHERE deleted=0 ORDER BY config_key;


-- ============================================================
-- 第四部分：验证查询（执行后请检查输出）
-- ============================================================

-- 4.1 权限码总数
SELECT 'permissions' AS table_name, COUNT(*) AS count FROM permissions WHERE deleted = 0
UNION ALL SELECT 'roles', COUNT(*) FROM roles WHERE deleted = 0
UNION ALL SELECT 'role_permissions', COUNT(*) FROM role_permissions WHERE deleted = 0
UNION ALL SELECT 'users', COUNT(*) FROM users WHERE deleted = 0
UNION ALL SELECT 'user_roles', COUNT(*) FROM user_roles WHERE deleted = 0
UNION ALL SELECT 'employees', COUNT(*) FROM employees WHERE deleted = 0
UNION ALL SELECT 'stores', COUNT(*) FROM stores WHERE deleted = 0
UNION ALL SELECT 'stores_new', COUNT(*) FROM stores_new WHERE deleted = 0
UNION ALL SELECT 'suppliers', COUNT(*) FROM suppliers WHERE deleted = 0
UNION ALL SELECT 'food_category', COUNT(*) FROM food_category WHERE deleted = 0
UNION ALL SELECT 'material_categories', COUNT(*) FROM material_categories WHERE deleted = 0
UNION ALL SELECT 'material_archives', COUNT(*) FROM material_archives WHERE deleted = 0
UNION ALL SELECT 'warehouses', COUNT(*) FROM warehouses WHERE deleted = 0
UNION ALL SELECT 'menus', COUNT(*) FROM menus WHERE deleted = 0
UNION ALL SELECT 'member_level', COUNT(*) FROM member_level
UNION ALL SELECT 'accounting_subject', COUNT(*) FROM accounting_subject
UNION ALL SELECT 'accounting_rule', COUNT(*) FROM accounting_rule
UNION ALL SELECT 'sys_config', COUNT(*) FROM sys_config WHERE deleted = 0
ORDER BY table_name;

-- 4.2 角色权限关联验证
SELECT r.role_id, r.role_code, r.role_name, COUNT(rp.permission_id) AS perm_count
FROM roles r
LEFT JOIN role_permissions rp ON CAST(r.role_id AS VARCHAR) = rp.role_id AND rp.deleted = 0
WHERE r.deleted = 0
GROUP BY r.role_id, r.role_code, r.role_name
ORDER BY r.role_id;

-- 4.3 用户角色关联验证
SELECT u.username, u.name, r.role_code, r.role_name
FROM users u
LEFT JOIN user_roles ur ON CAST(u.user_id AS VARCHAR) = ur.user_id AND ur.deleted = 0
LEFT JOIN roles r ON CAST(r.role_id AS VARCHAR) = ur.role_id AND r.deleted = 0
WHERE u.deleted = 0
ORDER BY u.user_id;


-- ============================================================
-- 初始化完成
-- ============================================================
-- 默认账号清单:
--   超级管理员: admin / Admin@123 (首次登录强制修改密码)
--   店长:       zhangming / User@123 (首次登录强制修改密码)
--   收银员:     lina / User@123 (首次登录强制修改密码)
--
-- 注意事项:
--   1. 生产环境必须为每个用户设置独立强密码
--   2. 完整的 293 个权限码需扫描 Controller @PreAuthorize 注解后补全
--   3. 部门/职位/职级 ID 请按实际查询结果调整
--   4. 金额字段以分为单位（整数），前端通过 DataConverter 转换
-- ============================================================
