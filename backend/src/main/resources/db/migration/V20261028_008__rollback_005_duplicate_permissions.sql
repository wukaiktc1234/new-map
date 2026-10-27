-- =====================================================================
-- V20261028_008__rollback_005_duplicate_permissions.sql
-- 撤回 V20261028_005 造成的**重复权限行**（我用错了列做存在性判断）。
--
-- 事实（2026-10-28 复核）：hr:contract-template:view / :manage 这两条权限**本来就存在**：
--   permission_id 200/201，permission_id_str=PERM_HR_CONTRACT_TEMPLATE_VIEW/MANAGE，
--   permission_code = hr:contract-template:view/manage（创建于 2026-07-23）。
-- 005 用 `WHERE NOT EXISTS (permission_id_str='hr:contract-template:view')` 判断存在性 —— 查错了列
--   （冒号形态在 permission_code 上），于是插入了重复行 33504/33505 与 6 条 role_permissions。
-- 本迁移删除这些重复行，恢复原状；不改任何业务数据。
-- =====================================================================
DELETE FROM role_permissions WHERE permission_id IN ('hr:contract-template:view', 'hr:contract-template:manage');
DELETE FROM permissions WHERE permission_id_str IN ('hr:contract-template:view', 'hr:contract-template:manage');

COMMENT ON COLUMN permissions.permission_id_str IS 'role_permissions.permission_id 的 join 键（PERM_* 形态）';
