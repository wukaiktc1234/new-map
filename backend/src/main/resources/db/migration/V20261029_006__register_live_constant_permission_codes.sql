-- =====================================================================
-- V20261029_006__register_live_constant_permission_codes.sql
--
-- ①（Lead 2026-10-29）：TokenServiceImpl / UserDetailsServiceImpl 的权限常量表**仍在运行**
--   （TokenServiceImpl 被 AuthServiceImpl 注入并调用 refreshToken；UserDetailsServiceImpl 被
--    SecurityConfig 用于 setUserDetailsService）。这两张表里的码会被**真的写进 token**，
--   但其中 35 个在 permissions 目录里不存在 → 「token 里有权限、后台授不了、审计看不到」的隐形授权。
--
-- 本迁移：批量登记这 35 个码（去重后实测 = 35，且全部出现在这两个活类中；
--   AuthenticationServiceImpl 独有的缺失码 = 0，故本批登记与该类是否死代码无关）。
-- =====================================================================

INSERT INTO permissions (permission_id_str, permission_code, permission_name, status, deleted, create_time)
SELECT v.id_str, v.code, v.name, 1, 0, now()
  FROM (VALUES
        ('PERM_FINANCE_BUDGET_APPROVE', 'finance:budget:approve', 'finance:budget:approve'),
        ('PERM_FINANCE_BUDGET_DELETE', 'finance:budget:delete', 'finance:budget:delete'),
        ('PERM_FINANCE_COST_APPROVE', 'finance:cost:approve', 'finance:cost:approve'),
        ('PERM_FINANCE_COST_DELETE', 'finance:cost:delete', 'finance:cost:delete'),
        ('PERM_PURCHASE_CONTRACT_APPROVE', 'purchase:contract:approve', 'purchase:contract:approve'),
        ('PERM_PURCHASE_CONTRACT_CREATE', 'purchase:contract:create', 'purchase:contract:create'),
        ('PERM_PURCHASE_CONTRACT_DELETE', 'purchase:contract:delete', 'purchase:contract:delete'),
        ('PERM_PURCHASE_CONTRACT_EDIT', 'purchase:contract:edit', 'purchase:contract:edit'),
        ('PERM_PURCHASE_CONTRACT_VIEW', 'purchase:contract:view', 'purchase:contract:view'),
        ('PERM_PURCHASE_MATERIAL_REQUEST_APPROVE', 'purchase:material-request:approve', 'purchase:material-request:approve'),
        ('PERM_PURCHASE_MATERIAL_REQUEST_CREATE', 'purchase:material-request:create', 'purchase:material-request:create'),
        ('PERM_PURCHASE_MATERIAL_REQUEST_DELETE', 'purchase:material-request:delete', 'purchase:material-request:delete'),
        ('PERM_PURCHASE_MATERIAL_REQUEST_EDIT', 'purchase:material-request:edit', 'purchase:material-request:edit'),
        ('PERM_PURCHASE_MATERIAL_REQUEST_VIEW', 'purchase:material-request:view', 'purchase:material-request:view'),
        ('PERM_PURCHASE_ORDER_APPROVE', 'purchase:order:approve', 'purchase:order:approve'),
        ('PERM_PURCHASE_ORDER_DELETE', 'purchase:order:delete', 'purchase:order:delete'),
        ('PERM_PURCHASE_ORDER_EDIT', 'purchase:order:edit', 'purchase:order:edit'),
        ('PERM_PURCHASE_PLAN_APPROVE', 'purchase:plan:approve', 'purchase:plan:approve'),
        ('PERM_PURCHASE_PLAN_CREATE', 'purchase:plan:create', 'purchase:plan:create'),
        ('PERM_PURCHASE_PLAN_DELETE', 'purchase:plan:delete', 'purchase:plan:delete'),
        ('PERM_PURCHASE_PLAN_EDIT', 'purchase:plan:edit', 'purchase:plan:edit'),
        ('PERM_PURCHASE_PLAN_VIEW', 'purchase:plan:view', 'purchase:plan:view'),
        ('PERM_PURCHASE_SETTLEMENT_APPROVE', 'purchase:settlement:approve', 'purchase:settlement:approve'),
        ('PERM_PURCHASE_SETTLEMENT_CREATE', 'purchase:settlement:create', 'purchase:settlement:create'),
        ('PERM_PURCHASE_SETTLEMENT_DELETE', 'purchase:settlement:delete', 'purchase:settlement:delete'),
        ('PERM_PURCHASE_SETTLEMENT_EDIT', 'purchase:settlement:edit', 'purchase:settlement:edit'),
        ('PERM_PURCHASE_SETTLEMENT_VIEW', 'purchase:settlement:view', 'purchase:settlement:view'),
        ('PERM_PURCHASE_STOCKIN_CREATE', 'purchase:stockin:create', 'purchase:stockin:create'),
        ('PERM_PURCHASE_STOCKIN_DELETE', 'purchase:stockin:delete', 'purchase:stockin:delete'),
        ('PERM_PURCHASE_STOCKIN_EDIT', 'purchase:stockin:edit', 'purchase:stockin:edit'),
        ('PERM_PURCHASE_SUPPLIER_CREATE', 'purchase:supplier:create', 'purchase:supplier:create'),
        ('PERM_PURCHASE_SUPPLIER_DELETE', 'purchase:supplier:delete', 'purchase:supplier:delete'),
        ('PERM_PURCHASE_SUPPLIER_EDIT', 'purchase:supplier:edit', 'purchase:supplier:edit'),
        ('PERM_PURCHASE_SUPPLIER_VIEW', 'purchase:supplier:view', 'purchase:supplier:view'),
        ('PERM_TRACE_EXPORT', 'trace:export', 'trace:export')
       ) AS v(id_str, code, name)
 WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_code = v.code);

-- 断言：本批 35 码必须全部在目录中（少一个 → 迁移失败）
DO $$
DECLARE have int;
BEGIN
    SELECT count(*) INTO have FROM permissions p
     WHERE p.deleted = 0 AND p.permission_code IN (
        'finance:budget:approve',
        'finance:budget:delete',
        'finance:cost:approve',
        'finance:cost:delete',
        'purchase:contract:approve',
        'purchase:contract:create',
        'purchase:contract:delete',
        'purchase:contract:edit',
        'purchase:contract:view',
        'purchase:material-request:approve',
        'purchase:material-request:create',
        'purchase:material-request:delete',
        'purchase:material-request:edit',
        'purchase:material-request:view',
        'purchase:order:approve',
        'purchase:order:delete',
        'purchase:order:edit',
        'purchase:plan:approve',
        'purchase:plan:create',
        'purchase:plan:delete',
        'purchase:plan:edit',
        'purchase:plan:view',
        'purchase:settlement:approve',
        'purchase:settlement:create',
        'purchase:settlement:delete',
        'purchase:settlement:edit',
        'purchase:settlement:view',
        'purchase:stockin:create',
        'purchase:stockin:delete',
        'purchase:stockin:edit',
        'purchase:supplier:create',
        'purchase:supplier:delete',
        'purchase:supplier:edit',
        'purchase:supplier:view',
        'trace:export'
     );
    IF have < 35 THEN
        RAISE EXCEPTION '迁移失败：本批 35 码只有 % 个在目录中', have;
    END IF;
END $$;
