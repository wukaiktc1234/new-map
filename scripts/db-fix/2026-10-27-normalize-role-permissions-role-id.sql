-- =====================================================================
-- 2026-10-27-normalize-role-permissions-role-id.sql
-- P0 后半段：role_permissions.role_id 的 legacy 数字形态（'1'）归一为 roles.role_id_str
--   背景：A45 断言发现 418 行 role_id='1'，而 roles 表里 role_id=1 的 role_id_str='ROLE_SUPER_ADMIN'；
--        UserMapper.getUserRolePermissions 的 join 用 role_id_str → 这 418 行**全部被丢弃**（超管授权静默失效）。
--   做法：① 备份（.verify-tmp/backup-role1-permissions.csv，418 行）
--         ② 剔除与 ROLE_SUPER_ADMIN 已有行冲突的重复（本轮实测 0 行）
--         ③ 其余 UPDATE role_id='1' → 'ROLE_SUPER_ADMIN'
--   幂等（再跑无 '1' 行）；回撤：按备份 CSV 恢复 role_id='1'。
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
DELETE FROM role_permissions rp
 WHERE rp.role_id = '1' AND COALESCE(rp.deleted,0) = 0
   AND EXISTS (SELECT 1 FROM role_permissions x
                WHERE x.role_id = 'ROLE_SUPER_ADMIN' AND x.permission_id = rp.permission_id
                  AND COALESCE(x.deleted,0) = 0);
UPDATE role_permissions SET role_id = 'ROLE_SUPER_ADMIN' WHERE role_id = '1';
INSERT INTO finance_audit_logs (operator_id, operator_name, operation_type, module, target_id, target_type,
                                operation_desc, operation_data, create_time, created_by, updated_by, version)
VALUES (0,'fund-500','PERM_ROLE_ID_NORMALIZE','SYSTEM',0,'ROLE_PERMISSION',
 'A45 发现：role_permissions.role_id=1（数字形态）418 行无法被 join 解析 → 归一为 ROLE_SUPER_ADMIN',
 '{"updated":418,"deletedDuplicates":0,"from":"1","to":"ROLE_SUPER_ADMIN","backup":".verify-tmp/backup-role1-permissions.csv"}',
 now(),'fund-500','fund-500',0);
COMMIT;
SELECT count(*) AS unresolved FROM role_permissions rp
 WHERE COALESCE(rp.deleted,0)=0
   AND ( NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_id_str=rp.permission_id)
         OR NOT EXISTS (SELECT 1 FROM roles r WHERE r.role_id_str=rp.role_id) );
SELECT role_id, count(*) FROM role_permissions WHERE COALESCE(deleted,0)=0 GROUP BY role_id ORDER BY 2 DESC LIMIT 5;