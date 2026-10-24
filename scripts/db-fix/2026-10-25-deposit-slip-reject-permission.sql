-- 2026-10-25 P3 前置：新增 finance:deposit-slip:reject（Owner 拍板 1）并绑定 TELLER + FINANCE_DIRECTOR
-- 幂等；同时写 permission_assignment_log 留痕（日志行数 = 新增绑定数）
BEGIN;

-- 1) 权限码本体（字段形态对齐既有 finance:deposit-slip:* 行）
-- permission_id 是 GENERATED ALWAYS 标识列，不能显式给值（P2 建 ROLE_TELLER 时踩过同一个坑）
INSERT INTO permissions (permission_id_str, permission_name, permission_code,
                         resource_type, resource_path, parent_id, description, sort_order, status,
                         create_time, update_time, deleted, permission_type, module, level,
                         icon, path, component, redirect, is_hidden, is_cache)
SELECT 'PERM_FINANCE_DEPOSIT_SLIP_REJECT', '现金交款单退回', 'finance:deposit-slip:reject',
       '', '', '', 'SL9 退回；P2 阶段暂挂 :close，P3 前置独立成码', 0, 1,
       now(), now(), 0, 1, 'finance', 1,
       '', '', '', '', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code='finance:deposit-slip:reject');

-- 2) 绑定 TELLER + FINANCE_DIRECTOR（只插缺失）
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT r, p.permission_id_str, 0, now()
FROM (VALUES ('ROLE_TELLER'), ('ROLE_FINANCE_DIRECTOR')) v(r)
CROSS JOIN permissions p
WHERE p.permission_code='finance:deposit-slip:reject' AND p.deleted=0
  AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id=v.r AND rp.permission_id=p.permission_id_str AND rp.deleted=0);

-- 3) 留痕
INSERT INTO permission_assignment_log (employee_id, operation_type, old_roles, new_roles, operator, operated_at, remark)
SELECT '-', 'ROLE_PERMISSION_BIND', NULL, r, 'night-handover', now(), 'permission=finance:deposit-slip:reject (PERM_FINANCE_DEPOSIT_SLIP_REJECT)'
FROM (VALUES ('ROLE_TELLER'), ('ROLE_FINANCE_DIRECTOR')) v(r)
WHERE NOT EXISTS (SELECT 1 FROM permission_assignment_log l
                  WHERE l.operation_type='ROLE_PERMISSION_BIND' AND l.new_roles=v.r
                    AND l.remark LIKE 'permission=finance:deposit-slip:reject%');

COMMIT;

-- 验证 1：权限码存在
SELECT permission_id, permission_id_str, permission_code FROM permissions WHERE permission_code='finance:deposit-slip:reject';
-- 验证 2：两角色持有
SELECT rp.role_id, r.role_name FROM role_permissions rp
JOIN roles r ON r.role_id_str=rp.role_id
JOIN permissions p ON p.permission_id_str=rp.permission_id
WHERE p.permission_code='finance:deposit-slip:reject' AND rp.deleted=0 ORDER BY 1;