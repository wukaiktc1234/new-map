-- =====================================================================
-- 2026-10-26-grant-confirm-attribution.sql
-- 给 ROLE_FINANCE_DIRECTOR 绑定 finance:deposit-slip:confirm-attribution（N19-2④ 端点）
--   背景：权限码在 permissions 里已存在，但**没有任何角色绑定** → 该端点对所有真实账号恒 403，
--         实测（qa_admin / qa_teller / vcashier01）均 403。本脚本按 lead 的 receive 授权脚本同款写法补齐。
--   幂等：已存在则不插入。回撤：UPDATE role_permissions SET deleted=1 WHERE role_id='ROLE_FINANCE_DIRECTOR'
--         AND permission_id='finance:deposit-slip:confirm-attribution';
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_FINANCE_DIRECTOR', 'finance:deposit-slip:confirm-attribution', 0, now()
 WHERE NOT EXISTS (
   SELECT 1 FROM role_permissions rp
    WHERE rp.role_id='ROLE_FINANCE_DIRECTOR'
      AND rp.permission_id='finance:deposit-slip:confirm-attribution'
      AND rp.deleted=0);
SELECT role_id, permission_id, deleted FROM role_permissions
 WHERE permission_id='finance:deposit-slip:confirm-attribution' ORDER BY role_id;
COMMIT;
