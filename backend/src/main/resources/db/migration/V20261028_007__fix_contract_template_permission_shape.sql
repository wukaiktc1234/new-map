-- =====================================================================
-- V20261028_007__fix_contract_template_permission_shape.sql
-- 修正 V20261028_005 的形态错误（自查发现）：
--   运行时授予的 authority 字符串来自 permissions.permission_code（见 UserMapper.xml
--   getUserRolePermissions：JOIN 用 permission_id_str，但 SELECT 的是 PERMISSION_CODE）。
--   005 把 'hr:contract-template:view' 写进了 permission_id_str、把 PERM_ 形态写进了 permission_code，
--   结果运行时授予的是 'PERM_HR_CONTRACT_TEMPLATE_VIEW'，@PreAuthorize('hr:contract-template:view') 仍然不匹配。
-- 正确形态：
--   permission_id_str = PERM_HR_CONTRACT_TEMPLATE_VIEW / PERM_HR_CONTRACT_TEMPLATE_MANAGE（供 role_permissions join）
--   permission_code   = hr:contract-template:view / hr:contract-template:manage（供 hasAuthority 匹配）
-- =====================================================================
UPDATE permissions SET permission_id_str = 'PERM_HR_CONTRACT_TEMPLATE_VIEW', permission_code = 'hr:contract-template:view'
 WHERE permission_id_str = 'hr:contract-template:view';
UPDATE permissions SET permission_id_str = 'PERM_HR_CONTRACT_TEMPLATE_MANAGE', permission_code = 'hr:contract-template:manage'
 WHERE permission_id_str = 'hr:contract-template:manage';

-- 反向同步 role_permissions.permission_id（005 里存的是冒号形态）
UPDATE role_permissions SET permission_id = 'PERM_HR_CONTRACT_TEMPLATE_VIEW'
 WHERE permission_id = 'hr:contract-template:view';
UPDATE role_permissions SET permission_id = 'PERM_HR_CONTRACT_TEMPLATE_MANAGE'
 WHERE permission_id = 'hr:contract-template:manage';

COMMENT ON COLUMN permissions.permission_code IS '授予给 Spring Security 的 authority 字符串（@PreAuthorize 匹配这个）；permission_id_str 仅供 role_permissions join';
