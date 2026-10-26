\set ON_ERROR_STOP on
BEGIN;
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_TELLER','finance:deposit-slip:receive',0,now()
 WHERE NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id='ROLE_TELLER' AND rp.permission_id='finance:deposit-slip:receive' AND rp.deleted=0);
SELECT permission_id FROM role_permissions WHERE role_id='ROLE_TELLER' AND deleted=0 AND permission_id LIKE 'finance:deposit-slip%' ORDER BY 1;
COMMIT;