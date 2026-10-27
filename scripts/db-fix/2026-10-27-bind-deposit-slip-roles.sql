\set ON_ERROR_STOP on
-- ⚠️ 已修正（P0）：role_permissions.permission_id 必须 = permissions.permission_id_str（PERM_XXX）。
--   旧版用权限码（finance:deposit-slip:receive）插入 → join 不上 = 静默失效（qa_store_b 曾 403）。
BEGIN;
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT v.r, p.permission_id_str, 0, now()
  FROM (VALUES ('ROLE_STORE_MANAGER','finance:deposit-slip:receive'),
               ('ROLE_STORE_MANAGER','finance:deposit-slip:deposit'),
               ('ROLE_FINANCE_DIRECTOR','finance:deposit-slip:close'),
               ('ROLE_FINANCE_DIRECTOR','finance:deposit-slip:verify')) v(r, code)
  JOIN permissions p ON p.permission_code = v.code AND p.deleted = 0 AND p.status = 1
 WHERE NOT EXISTS (SELECT 1 FROM role_permissions rp
                    WHERE rp.role_id = v.r AND rp.permission_id = p.permission_id_str AND COALESCE(rp.deleted,0)=0);
COMMIT;
SELECT p.permission_code, rp.permission_id, string_agg(rp.role_id, ',' ORDER BY rp.role_id) AS roles
  FROM role_permissions rp JOIN permissions p ON p.permission_id_str = rp.permission_id
 WHERE p.permission_code LIKE 'finance:deposit-slip:%' AND COALESCE(rp.deleted,0)=0
 GROUP BY p.permission_code, rp.permission_id ORDER BY 1;
