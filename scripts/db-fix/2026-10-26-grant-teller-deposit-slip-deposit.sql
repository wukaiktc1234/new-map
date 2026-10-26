-- =====================================================================
-- 2026-10-26-grant-teller-deposit-slip-deposit.sql
-- N19-3a②：给 ROLE_TELLER 补 finance:deposit-slip:deposit（存行）。
--   只补 deposit；**不补 create / submit**（create 走系统自动、submit 属 CASHIER）。
--   幂等；回撤：UPDATE role_permissions SET deleted=1 WHERE role_id='ROLE_TELLER'
--         AND permission_id='finance:deposit-slip:deposit';
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_TELLER', 'finance:deposit-slip:deposit', 0, now()
 WHERE NOT EXISTS (
   SELECT 1 FROM role_permissions rp
    WHERE rp.role_id='ROLE_TELLER' AND rp.permission_id='finance:deposit-slip:deposit' AND rp.deleted=0);
SELECT permission_id, deleted FROM role_permissions
 WHERE role_id='ROLE_TELLER' AND permission_id LIKE 'finance:deposit-slip%' ORDER BY 1;
COMMIT;
