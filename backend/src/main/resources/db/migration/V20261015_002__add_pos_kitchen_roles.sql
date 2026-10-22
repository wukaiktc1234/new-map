-- ============================================================
-- N31（board §24.15a）：补建 4 个业务角色（Owner 裁决 4）
--
-- 背景（N32 扫描器实测，2026-10-15）：
--   POS / 厨房共 22 处注解使用 hasAnyAuthority('ROLE_CASHIER' / 'ROLE_POS_OPERATOR'
--   / 'ROLE_CHEF' / 'ROLE_KITCHEN')，而 roles 表既无对应 role_code 也无 role_id_str
--   -> 任何用户的 authority 里都不会出现这 4 个字符串 -> 相关端点永远 403。
--   报告：docs/audit/auth-annotation-literal-audit-001.md §6（C 类）
--
-- 本迁移：
--   1) 建 4 个角色（role_id_str 与注解写法完全一致：ROLE_CASHIER / ROLE_POS_OPERATOR
--      / ROLE_CHEF / ROLE_KITCHEN；role_code 为小写规范码）
--   2) 按模块前缀授予最小必要权限码（未登记的角色/模块一律不授）
--
-- 幂等：role_code / role_id_str 双守卫，可重复执行。
-- 回退：
--   DELETE FROM role_permissions WHERE role_id IN ('ROLE_CASHIER','ROLE_POS_OPERATOR','ROLE_CHEF','ROLE_KITCHEN');
--   DELETE FROM roles WHERE role_code IN ('cashier','pos_operator','chef','kitchen');
-- 注：roles.role_id 为 GENERATED ALWAYS AS IDENTITY -> 不显式赋值。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 4 个角色
-- ------------------------------------------------------------
INSERT INTO roles (role_id_str, role_name, role_code, description, status, data_scope, org_level,
                   deleted, version, create_time, update_time)
SELECT 'ROLE_CASHIER', '收银员', 'cashier', 'POS 收银员（前台收款）', 'active', 'store', 'STORE_STAFF',
       0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.role_code = 'cashier' OR r.role_id_str = 'ROLE_CASHIER');

INSERT INTO roles (role_id_str, role_name, role_code, description, status, data_scope, org_level,
                   deleted, version, create_time, update_time)
SELECT 'ROLE_POS_OPERATOR', 'POS操作员', 'pos_operator', 'POS 点单/终端操作员', 'active', 'store', 'STORE_STAFF',
       0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.role_code = 'pos_operator' OR r.role_id_str = 'ROLE_POS_OPERATOR');

INSERT INTO roles (role_id_str, role_name, role_code, description, status, data_scope, org_level,
                   deleted, version, create_time, update_time)
SELECT 'ROLE_CHEF', '厨师', 'chef', '后厨厨师（出品）', 'active', 'store', 'STORE_STAFF',
       0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.role_code = 'chef' OR r.role_id_str = 'ROLE_CHEF');

INSERT INTO roles (role_id_str, role_name, role_code, description, status, data_scope, org_level,
                   deleted, version, create_time, update_time)
SELECT 'ROLE_KITCHEN', '后厨人员', 'kitchen', '后厨通用人员', 'active', 'store', 'STORE_STAFF',
       0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles r WHERE r.role_code = 'kitchen' OR r.role_id_str = 'ROLE_KITCHEN');

-- ------------------------------------------------------------
-- 2. 最小必要权限（按模块前缀）
--    cashier      <- order, member, coupon, refund, store
--    pos_operator <- order, member, coupon
--    chef         <- kitchen
--    kitchen      <- kitchen
-- ------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT v.role_id, p.permission_id_str, 0, NOW()
FROM (VALUES
    ('ROLE_CASHIER',      'order'),
    ('ROLE_CASHIER',      'member'),
    ('ROLE_CASHIER',      'coupon'),
    ('ROLE_CASHIER',      'refund'),
    ('ROLE_CASHIER',      'store'),
    ('ROLE_POS_OPERATOR', 'order'),
    ('ROLE_POS_OPERATOR', 'member'),
    ('ROLE_POS_OPERATOR', 'coupon'),
    ('ROLE_CHEF',         'kitchen'),
    ('ROLE_KITCHEN',      'kitchen')
) AS v(role_id, module_prefix)
JOIN permissions p ON p.permission_code LIKE v.module_prefix || ':%'
WHERE p.deleted = 0 AND p.status = 1
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp
                  WHERE rp.role_id = v.role_id
                    AND rp.permission_id = p.permission_id_str
                    AND rp.deleted = 0);

-- ------------------------------------------------------------
-- 3. 自检（执行后人工核对）
-- ------------------------------------------------------------
-- SELECT role_id_str, role_code, role_name FROM roles WHERE role_code IN ('cashier','pos_operator','chef','kitchen');
-- SELECT rp.role_id, count(*) FROM role_permissions rp
--  WHERE rp.role_id IN ('ROLE_CASHIER','ROLE_POS_OPERATOR','ROLE_CHEF','ROLE_KITCHEN') AND rp.deleted = 0
--  GROUP BY 1 ORDER BY 1;
