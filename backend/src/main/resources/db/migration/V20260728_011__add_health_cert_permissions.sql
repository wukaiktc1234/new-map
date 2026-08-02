-- 为相关角色补充健康证查看/管理权限
-- 修复一线员工、店长、HR总监访问健康证接口 403 的问题

-- 给普通员工添加健康证查看权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'ROLE_EMPLOYEE', 'PERM_HR_HEALTH_CERT_VIEW'
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_EMPLOYEE' AND permission_id = 'PERM_HR_HEALTH_CERT_VIEW'
);

-- 给店长添加健康证查看权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'ROLE_STORE_MANAGER', 'PERM_HR_HEALTH_CERT_VIEW'
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_STORE_MANAGER' AND permission_id = 'PERM_HR_HEALTH_CERT_VIEW'
);

-- 给 HR 总监添加健康证查看/管理/删除权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'ROLE_HR_DIRECTOR', 'PERM_HR_HEALTH_CERT_VIEW'
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_HR_DIRECTOR' AND permission_id = 'PERM_HR_HEALTH_CERT_VIEW'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'ROLE_HR_DIRECTOR', 'PERM_HR_HEALTH_CERT_MANAGE'
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_HR_DIRECTOR' AND permission_id = 'PERM_HR_HEALTH_CERT_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT 'ROLE_HR_DIRECTOR', 'PERM_HR_HEALTH_CERT_DELETE'
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_HR_DIRECTOR' AND permission_id = 'PERM_HR_HEALTH_CERT_DELETE'
);
