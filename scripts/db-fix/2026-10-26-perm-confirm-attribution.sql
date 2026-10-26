\set ON_ERROR_STOP on
SET client_encoding='UTF8';
BEGIN;
INSERT INTO permissions (permission_id_str, permission_name, permission_code, resource_type, description, status, create_time, deleted)
SELECT 'finance:deposit-slip:confirm-attribution', '缴款单-人工确认退款归属',
       'finance:deposit-slip:confirm-attribution', 'API',
       '人工确认退款归属到班次（账务裁决，收银员不可终审）— NEW-19', 1, now(), 0
 WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_id_str='finance:deposit-slip:confirm-attribution');
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT r.role_id_str, 'finance:deposit-slip:confirm-attribution', 0, now()
  FROM (SELECT role_id_str FROM roles WHERE role_id_str IN ('ROLE_FINANCE_DIRECTOR','ROLE_STORE_MANAGER') AND deleted=0) r
 WHERE NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.permission_id='finance:deposit-slip:confirm-attribution' AND rp.role_id=r.role_id_str AND rp.deleted=0);
UPDATE role_permissions rp SET deleted=1
 WHERE rp.deleted=0 AND NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_id_str = rp.permission_id);
\echo '--- AFTER ---'
SELECT (SELECT count(*) FROM role_permissions WHERE deleted=0) AS bindings,
       (SELECT count(*) FROM role_permissions rp2 WHERE rp2.deleted=0 AND NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_id_str=rp2.permission_id)) AS orphans_left,
       (SELECT count(*) FROM role_permissions rp3 WHERE rp3.permission_id='finance:deposit-slip:confirm-attribution' AND rp3.deleted=0) AS new_perm_roles;
COMMIT;