-- ⚠️ 已修正（P0）：必须用 permission_id_str（PERM_FINANCE_DEPOSIT_SLIP_RECEIVE），不是权限码
\set ON_ERROR_STOP on
BEGIN;
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_TELLER', p.permission_id_str, 0, now()
  FROM permissions p
 WHERE p.permission_code = 'finance:deposit-slip:receive' AND p.deleted = 0 AND p.status = 1
   AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id='ROLE_TELLER'
                    AND rp.permission_id = p.permission_id_str AND COALESCE(rp.deleted,0)=0);
COMMIT;
