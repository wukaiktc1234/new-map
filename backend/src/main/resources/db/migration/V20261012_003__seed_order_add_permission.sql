-- ============================================================
-- N15 扩展子项（board §24.3s，Owner 指令 2026-10-12「order:add 缺口并入 N15 卡范围，不另开」）：
-- order:add 权限码种子 + 角色绑定（镜像 order:manage 的 5 角色，Owner 已确认）
--
-- 背景（证据，2026-10-12）：
--   1) OrderNewController:38 引用 'order:add'（链 order:add or order:manage or *）——
--      全仓唯一引用端点；
--   2) 代码权限树已定义该码：PermissionInitServiceImpl:140
--      createPermission(…, "order:add", "新增订单", 2, "order", 20L, 1)；
--   3) 活库 permissions 无该码（V20261012_002 后仅 order:manage / order:view 两码）；
--   4) 功能影响：:38 有 order:manage 兜底 → N15 后 5 角色已可创建订单；
--      本迁移补的是 seed 完整性（权限树 / 菜单 / 审计面），非功能阻塞。
--
-- permission_type 取值说明（如实登记）：取 1，镜像种子面既有约定——
--   活库 deleted=0 的 131 行 permissions 全部 permission_type=1（含 V20261009_001 / V20261012_002 种子行）；
--   代码树的 0/1/2 是树自身层级语义（Mapper 中 'menu'/'button'/'data' 字符串查询属遗留面），
--   取 2 将成为活库唯一 type=2 行，引入新不一致，故不取。
--
-- permission_name '新增订单' 与 PermissionInitServiceImpl:140 注册表一致。
-- 绑定集（5 角色 × 1 码 = 5 行，Owner 确认镜像 V20261012_002 的 5 角色）：
--   '1'（admin 实际持有的悬空角色 ID）、ROLE_ADMIN、ROLE_OWNER、
--   ROLE_OPS_DIRECTOR、ROLE_STORE_MANAGER。
--
-- 幂等：所有 INSERT 均带 NOT EXISTS 守卫，可重复执行。
-- 回退：
--   DELETE FROM role_permissions WHERE permission_id IN ('PERM_ORDER_ADD');
--   DELETE FROM permissions WHERE permission_id_str = 'PERM_ORDER_ADD';
-- ============================================================

-- ------------------------------------------------------------
-- 1. 权限种子（1 个权限码）
-- ------------------------------------------------------------
INSERT INTO permissions (permission_id_str, permission_name, permission_code, permission_type, module, status, create_time, update_time, deleted)
SELECT 'PERM_ORDER_ADD', '新增订单', 'order:add', 1, 'order', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE permission_id_str = 'PERM_ORDER_ADD'
);

-- ------------------------------------------------------------
-- 2. 角色绑定（5 角色 × 1 码 = 5 行）
--    注：role_id/permission_id 均存字符串 ID，
--        与 UserMapper.xml 的 role_id_str / permission_id_str join 对齐。
-- ------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT '1', 'PERM_ORDER_ADD', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = '1' AND permission_id = 'PERM_ORDER_ADD'
);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_ADMIN', 'PERM_ORDER_ADD', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_ADMIN' AND permission_id = 'PERM_ORDER_ADD'
);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_OWNER', 'PERM_ORDER_ADD', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_OWNER' AND permission_id = 'PERM_ORDER_ADD'
);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_OPS_DIRECTOR', 'PERM_ORDER_ADD', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_OPS_DIRECTOR' AND permission_id = 'PERM_ORDER_ADD'
);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_STORE_MANAGER', 'PERM_ORDER_ADD', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_STORE_MANAGER' AND permission_id = 'PERM_ORDER_ADD'
);
