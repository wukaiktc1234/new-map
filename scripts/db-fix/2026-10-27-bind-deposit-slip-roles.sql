\set ON_ERROR_STOP on
BEGIN;
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT r, p, 0, now() FROM (VALUES ('ROLE_STORE_MANAGER','finance:deposit-slip:receive'),
  ('ROLE_STORE_MANAGER','finance:deposit-slip:deposit'),
  ('ROLE_FINANCE_DIRECTOR','finance:deposit-slip:close'),
  ('ROLE_FINANCE_DIRECTOR','finance:deposit-slip:verify')) v(r,p)
 WHERE NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id=v.r AND rp.permission_id=v.p AND rp.deleted=0);
SELECT permission_id, string_agg(role_id, ', ' ORDER BY role_id) AS roles FROM role_permissions
 WHERE permission_id IN ('finance:deposit-slip:receive','finance:deposit-slip:deposit','finance:deposit-slip:close','finance:deposit-slip:verify') AND deleted=0
 GROUP BY 1 ORDER BY 1;
COMMIT;