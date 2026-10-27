-- =====================================================================
-- V20261028_005__contract_template_permissions.sql
-- A46 修复：ContractTemplateController 要求 hasAuthority('hr:contract-template:view|manage')，
-- 但 permissions 目录里没有这两条 → 除通配 *（SUPER_ADMIN）外**所有角色 403**，且不报错。
--
-- 约定（lesson）：@PreAuthorize 的字符串必须等于 permissions.permission_id_str，
-- 这样 role_permissions.permission_id（存 permission_id_str）才能 join 上并解析成 Authority。
-- permission_code 用 PERM_ 形态作为人类可读编码，不参与鉴权。
-- =====================================================================
INSERT INTO permissions (permission_id_str, permission_name, permission_code, resource_type, resource_path, module, status, deleted, create_time, update_time)
SELECT 'hr:contract-template:view', '合同模板-查看', 'PERM_HR_CONTRACT_TEMPLATE_VIEW', 'API', '/v1/hr/contract-template/**', 'hr', 1, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_id_str = 'hr:contract-template:view');

INSERT INTO permissions (permission_id_str, permission_name, permission_code, resource_type, resource_path, module, status, deleted, create_time, update_time)
SELECT 'hr:contract-template:manage', '合同模板-管理', 'PERM_HR_CONTRACT_TEMPLATE_MANAGE', 'API', '/v1/hr/contract-template/**', 'hr', 1, 0, now(), now()
 WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_id_str = 'hr:contract-template:manage');

-- 绑定：项目**没有 HR 专属角色用于本功能**（hr_director 是 HR 总监，符合语义）；
-- 按 Lead 要求「有 HR 角色就绑它」→ 绑 hr_director + ROLE_ADMIN + ROLE_OWNER（管理员兜底）。
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT r.role_id_str, p.permission_id_str, 0, now()
  FROM roles r
  JOIN permissions p ON p.permission_id_str IN ('hr:contract-template:view', 'hr:contract-template:manage')
 WHERE r.role_code IN ('hr_director', 'admin', 'owner')
   AND NOT EXISTS (SELECT 1 FROM role_permissions x
                    WHERE x.role_id = r.role_id_str AND x.permission_id = p.permission_id_str AND COALESCE(x.deleted,0) = 0);

COMMENT ON TABLE permissions IS '权限目录；permission_id_str 必须与 @PreAuthorize 中的权限串一致（A46 断言）';
