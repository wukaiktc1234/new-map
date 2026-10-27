-- =====================================================================
-- V20261028_011__over_age_worker_permissions.sql
-- bug-㉑ 关联修复：OverAgeWorkerController 原用 hasAuthority('*')。
-- Spring 的 hasAuthority 是**精确字符串匹配**，'*' 不是通配符；
-- 而 permissions 目录里并不存在 permission_code='*' → 该接口现实中无人可调（永远 403）。
-- 正解（Lead 2026-10-28 裁定方案②）：补该模块真实权限码，绑 HR 角色。
-- =====================================================================

INSERT INTO permissions (permission_id_str, permission_code, permission_name, module, description, deleted, create_time)
SELECT 'PERM_HR_OVER_AGE_WORKER_VIEW', 'hr:over-age-worker:view', '超龄用工-查看', 'hr', '查看超龄劳动者信息（V2.0）', 0, now()
 WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'hr:over-age-worker:view');

INSERT INTO permissions (permission_id_str, permission_code, permission_name, module, description, deleted, create_time)
SELECT 'PERM_HR_OVER_AGE_WORKER_MANAGE', 'hr:over-age-worker:manage', '超龄用工-管理', 'hr', '新增/修改超龄劳动者信息（V2.0）', 0, now()
 WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'hr:over-age-worker:manage');

-- 绑定 HR 总监（默认给 HR 用；如需门店店长另议）
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_HR_DIRECTOR', p.permission_id_str, 0, now() FROM permissions p
 WHERE p.permission_code IN ('hr:over-age-worker:view','hr:over-age-worker:manage')
   AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id='ROLE_HR_DIRECTOR' AND rp.permission_id=p.permission_id_str);

SELECT p.permission_code, (SELECT count(*) FROM role_permissions rp WHERE rp.permission_id=p.permission_id_str) AS bindings
  FROM permissions p WHERE p.permission_code LIKE 'hr:over-age-worker:%';
