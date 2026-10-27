-- =====================================================================
-- 2026-10-27-grant-deposit-slip-submit-to-managers.sql
-- NEW-38 补齐：deposit-slip:submit 原先**只**绑 ROLE_CASHIER（POS 侧），
--   后台（店长/财务）无人能提交 → 流程死锁（实测 vcashier01 200，qa_teller/qa_store_b/qa_admin 全 403）。
--   语义：**补齐而非放开** —— 收银员在 POS 侧提交，店长/财务在后台补提交。
--   规则同 P0：role_permissions.permission_id 必须 = permissions.permission_id_str（PERM_XXX）。
--   幂等；回撤：DELETE FROM role_permissions
--     WHERE permission_id='PERM_FINANCE_DEPOSIT_SLIP_SUBMIT'
--       AND role_id IN ('ROLE_STORE_MANAGER','ROLE_FINANCE_DIRECTOR');
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT r.role_id, p.permission_id_str, 0, now()
  FROM (VALUES ('ROLE_STORE_MANAGER'), ('ROLE_FINANCE_DIRECTOR')) AS r(role_id)
 CROSS JOIN permissions p
 WHERE p.permission_code = 'finance:deposit-slip:submit' AND p.deleted = 0 AND p.status = 1
   AND NOT EXISTS (SELECT 1 FROM role_permissions rp
                    WHERE rp.role_id = r.role_id AND rp.permission_id = p.permission_id_str
                      AND COALESCE(rp.deleted,0) = 0);
INSERT INTO finance_audit_logs (operator_id, operator_name, operation_type, module, target_id, target_type,
                                operation_desc, operation_data, create_time, created_by, updated_by, version)
VALUES (0,'fund-500','PERM_GRANT','SYSTEM',0,'ROLE_PERMISSION',
 'NEW-38 补齐：finance:deposit-slip:submit 补绑 ROLE_STORE_MANAGER + ROLE_FINANCE_DIRECTOR（原仅 ROLE_CASHIER，后台无人能提交）',
 '{"permission":"finance:deposit-slip:submit","permissionIdStr":"PERM_FINANCE_DEPOSIT_SLIP_SUBMIT","roles":["ROLE_STORE_MANAGER","ROLE_FINANCE_DIRECTOR"],"reason":"NEW-38 流程死锁：submit 仅绑定 CASHIER，后台角色无提交权限"}',
 now(),'fund-500','fund-500',0);
COMMIT;
SELECT rp.permission_id, string_agg(rp.role_id, ',' ORDER BY rp.role_id) AS roles
  FROM role_permissions rp WHERE rp.permission_id='PERM_FINANCE_DEPOSIT_SLIP_SUBMIT' AND COALESCE(rp.deleted,0)=0
 GROUP BY rp.permission_id;