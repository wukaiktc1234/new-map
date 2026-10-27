-- =====================================================================
-- 2026-10-27-grant-deposit-slip-reject-to-finance-manager.sql
-- NEW-38 同族第 4 处（矩阵核查发现）：reject 只绑 FINANCE_DIRECTOR + TELLER，缺 ROLE_FINANCE_MANAGER。
--   幂等；回撤：DELETE FROM role_permissions WHERE permission_id='PERM_FINANCE_DEPOSIT_SLIP_REJECT'
--              AND role_id='ROLE_FINANCE_MANAGER';
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_FINANCE_MANAGER', p.permission_id_str, 0, now()
  FROM permissions p
 WHERE p.permission_code = 'finance:deposit-slip:reject' AND p.deleted = 0 AND p.status = 1
   AND NOT EXISTS (SELECT 1 FROM role_permissions rp
                    WHERE rp.role_id = 'ROLE_FINANCE_MANAGER'
                      AND rp.permission_id = p.permission_id_str AND COALESCE(rp.deleted,0) = 0);
INSERT INTO finance_audit_logs (operator_id, operator_name, operation_type, module, target_id, target_type,
                                operation_desc, operation_data, create_time, created_by, updated_by, version)
VALUES (0,'fund-500','PERM_GRANT','SYSTEM',0,'ROLE_PERMISSION',
 'NEW-38④ 补齐：finance:deposit-slip:reject 补绑 ROLE_FINANCE_MANAGER（财务经理为驳回主力）',
 '{"permission":"finance:deposit-slip:reject","permissionIdStr":"PERM_FINANCE_DEPOSIT_SLIP_REJECT","roles":["ROLE_FINANCE_MANAGER"]}',
 now(),'fund-500','fund-500',0);
COMMIT;