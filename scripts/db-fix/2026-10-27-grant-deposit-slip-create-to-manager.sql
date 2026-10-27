-- =====================================================================
-- 2026-10-27-grant-deposit-slip-create-to-manager.sql
-- NEW-38 同族第 3 处：finance:deposit-slip:create 缺 ROLE_STORE_MANAGER
--   业务依据：关班后生成缴款单是店长/收银主管的标准动作；出纳只负责清点（receive）。
--   规则同 P0：role_permissions.permission_id 必须 = permissions.permission_id_str（PERM_XXX）。
--   幂等；回撤：DELETE FROM role_permissions WHERE permission_id='PERM_FINANCE_DEPOSIT_SLIP_CREATE'
--              AND role_id='ROLE_STORE_MANAGER';
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_STORE_MANAGER', p.permission_id_str, 0, now()
  FROM permissions p
 WHERE p.permission_code = 'finance:deposit-slip:create' AND p.deleted = 0 AND p.status = 1
   AND NOT EXISTS (SELECT 1 FROM role_permissions rp
                    WHERE rp.role_id = 'ROLE_STORE_MANAGER'
                      AND rp.permission_id = p.permission_id_str AND COALESCE(rp.deleted,0) = 0);
INSERT INTO finance_audit_logs (operator_id, operator_name, operation_type, module, target_id, target_type,
                                operation_desc, operation_data, create_time, created_by, updated_by, version)
VALUES (0,'fund-500','PERM_GRANT','SYSTEM',0,'ROLE_PERMISSION',
 'NEW-38③ 补齐：finance:deposit-slip:create 补绑 ROLE_STORE_MANAGER（关班出单是店长标准动作）',
 '{"permission":"finance:deposit-slip:create","permissionIdStr":"PERM_FINANCE_DEPOSIT_SLIP_CREATE","roles":["ROLE_STORE_MANAGER"],"reason":"NEW-38 同族第3处"}',
 now(),'fund-500','fund-500',0);
COMMIT;
SELECT rp.permission_id, string_agg(rp.role_id, ',' ORDER BY rp.role_id) AS roles
  FROM role_permissions rp WHERE rp.permission_id='PERM_FINANCE_DEPOSIT_SLIP_CREATE' AND COALESCE(rp.deleted,0)=0
 GROUP BY rp.permission_id;