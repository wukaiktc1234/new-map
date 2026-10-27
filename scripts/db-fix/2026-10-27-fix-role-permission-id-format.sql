-- =====================================================================
-- 2026-10-27-fix-role-permission-id-format.sql
-- P0：修正 role_permissions.permission_id 的「权限码形态」孤儿行
--   规则（写死）：role_permissions.permission_id 必须等于 permissions.permission_id_str（形如 PERM_XXX）。
--     用权限码（finance:deposit-slip:receive）插入的行，在 UserMapper.getUserRolePermissions 的
--     join（rp.PERMISSION_ID = p.permission_id_str）上匹配不上 → 静默失效（接口 403）。
--   本脚本：① 补真授权（STORE_MANAGER 的 receive/deposit，原本只有孤儿行）
--           ② 删除全部孤儿行（6 行，已备份 .verify-tmp/backup-orphan-role-permissions.csv）
--   幂等；回撤见文件末尾注释。
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
-- ① 补真授权：STORE_MANAGER 的 receive / deposit
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT r.role_id, p.permission_id_str, 0, now()
  FROM (VALUES ('ROLE_STORE_MANAGER')) AS r(role_id)
 CROSS JOIN permissions p
 WHERE p.permission_code IN ('finance:deposit-slip:receive','finance:deposit-slip:deposit')
   AND p.deleted = 0 AND p.status = 1
   AND NOT EXISTS (SELECT 1 FROM role_permissions rp
                    WHERE rp.role_id = r.role_id AND rp.permission_id = p.permission_id_str
                      AND COALESCE(rp.deleted,0) = 0);
-- ② 删除孤儿行（permission_id 不等于任何 permission_id_str）
DELETE FROM role_permissions rp
 WHERE COALESCE(rp.deleted,0) = 0
   AND NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_id_str = rp.permission_id);
-- 审计留痕
INSERT INTO finance_audit_logs (operator_id, operator_name, operation_type, module, target_id, target_type,
                                operation_desc, operation_data, create_time, created_by, updated_by, version)
VALUES (0, 'fund-500', 'PERM_ID_FORMAT_FIX', 'SYSTEM', 0, 'ROLE_PERMISSION',
        'P0 修正：补 STORE_MANAGER 的 receive/deposit 真授权（PERM_ 形态），删除 6 行权限码形态孤儿授权',
        '{"added":["ROLE_STORE_MANAGER/PERM_FINANCE_DEPOSIT_SLIP_RECEIVE","ROLE_STORE_MANAGER/PERM_FINANCE_DEPOSIT_SLIP_DEPOSIT"],"deletedOrphans":6,"backup":".verify-tmp/backup-orphan-role-permissions.csv","rule":"role_permissions.permission_id = permissions.permission_id_str"}',
        now(), 'fund-500', 'fund-500', 0);
COMMIT;
SELECT count(*) AS orphans_left
  FROM role_permissions rp
 WHERE COALESCE(rp.deleted,0) = 0
   AND NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_id_str = rp.permission_id);
SELECT p.permission_code, rp.permission_id
  FROM role_permissions rp JOIN permissions p ON p.permission_id_str = rp.permission_id
 WHERE rp.role_id = 'ROLE_STORE_MANAGER' AND COALESCE(rp.deleted,0)=0
   AND p.permission_code LIKE 'finance:deposit-slip:%'
 ORDER BY p.permission_code;
-- 回撤：
--   DELETE FROM role_permissions WHERE role_id='ROLE_STORE_MANAGER'
--     AND permission_id IN ('PERM_FINANCE_DEPOSIT_SLIP_RECEIVE','PERM_FINANCE_DEPOSIT_SLIP_DEPOSIT');
--   （孤儿行按 .verify-tmp/backup-orphan-role-permissions.csv 恢复）
