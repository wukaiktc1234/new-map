-- ============================================================
-- P1-USER-LOCATION-002 C3（D3 / U3 / U7）：员工归属分配权限种子
--
-- 背景：
--   1) hr:employee:assign-location 为 C3 新增权限码
--      （PUT /v1/employees/{id}/assign-location 的专属门槛，
--       与"改档案"的 hr:employee:manage 分离）；
--   2) system:user:assign-store / system:user:unassign-store
--      自 -001 起仅有 @RequiresPermission 注解，从未落库种子，
--      实际只有 admin 通配符 * 可用（U3 裁定：一并补齐种子行）；
--   3) U7 裁定：hr:employee:assign-location 授予 ROLE_HR_DIRECTOR
--      与 admin（admin 经 * 通配符自动覆盖）；
--      不授予 ROLE_STORE_MANAGER。
--
-- 幂等：所有 INSERT 均带 NOT EXISTS 守卫，可重复执行。
-- 回滚：向前修复（后续追加 DELETE 迁移删除本迁移的 3 行权限 + 1 行绑定），
--       本项目无 Flyway undo 约定。
--
-- 依赖：ROLE_HR_DIRECTOR 为人工创建角色（与 V20260728_011 同款假设，
--       该迁移已向同一角色绑定 3 条权限且线上生效）。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 权限种子（3 个权限码，同一迁移）
-- ------------------------------------------------------------
INSERT INTO permissions (permission_id_str, permission_name, permission_code, permission_type, module, status, create_time, update_time, deleted)
SELECT 'PERM_HR_EMP_ASSIGN_LOCATION', '员工归属分配', 'hr:employee:assign-location', 1, 'hr', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE permission_id_str = 'PERM_HR_EMP_ASSIGN_LOCATION'
);

INSERT INTO permissions (permission_id_str, permission_name, permission_code, permission_type, module, status, create_time, update_time, deleted)
SELECT 'PERM_SYS_USER_ASSIGN_STORE', '分配门店给用户', 'system:user:assign-store', 1, 'system', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE permission_id_str = 'PERM_SYS_USER_ASSIGN_STORE'
);

INSERT INTO permissions (permission_id_str, permission_name, permission_code, permission_type, module, status, create_time, update_time, deleted)
SELECT 'PERM_SYS_USER_UNASSIGN_STORE', '取消用户门店归属', 'system:user:unassign-store', 1, 'system', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE permission_id_str = 'PERM_SYS_USER_UNASSIGN_STORE'
);

-- ------------------------------------------------------------
-- 2. 角色绑定（U7：仅 ROLE_HR_DIRECTOR；admin 由 * 通配符覆盖）
--    注：role_id/permission_id 均存字符串 ID，
--        与 UserMapper.xml 的 role_id_str / permission_id_str join 对齐。
-- ------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_HR_DIRECTOR', 'PERM_HR_EMP_ASSIGN_LOCATION'
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions
    WHERE role_id = 'ROLE_HR_DIRECTOR' AND permission_id = 'PERM_HR_EMP_ASSIGN_LOCATION'
);
