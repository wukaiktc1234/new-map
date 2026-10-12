-- ============================================================
-- N15（board §24.3s）：订单权限码种子 + 角色绑定
--
-- 背景（证据，2026-10-12 活库实测 food_traceability@localhost:5432）：
--   1) 活库 permissions 中 permission_code LIKE 'order%' = 0 行
--      → order:view / order:manage 仅有 @PreAuthorize 注解引用
--        （75 处，10 个 Controller），从未落库种子；
--   2) 活库无 '*' 通配符（permissions 0 行）→ admin 不能经 '*' 兜底，
--      必须显式绑定；
--   3) 活库 admin 用户持 role_id '1'（roles 表无 '1' 行，悬空角色 ID，
--      经 UserMapper.xml 字符串 join 生效）→ '1' 必须在绑定集内，
--      否则 admin 在 order 端点 403。
--
-- 绑定集（5 角色 × 2 码 = 10 行，证据见 board §24.3s 卡）：
--   '1'（admin 实际持有）、ROLE_ADMIN、ROLE_OWNER、
--   ROLE_OPS_DIRECTOR（活库 0 权限，需补齐）、ROLE_STORE_MANAGER。
--
-- 命名：permission_id_str 沿用 PERM_* 约定（同 V20261009_001）；
--       permission_name 与 PermissionInitServiceImpl 注册表一致
--       （order:manage=订单管理 / order:view=查看订单）。
--
-- 幂等：所有 INSERT 均带 NOT EXISTS 守卫，可重复执行。
-- 回退：
--   DELETE FROM role_permissions WHERE permission_id IN ('PERM_ORDER_MANAGE','PERM_ORDER_VIEW');
--   DELETE FROM permissions WHERE permission_id_str IN ('PERM_ORDER_MANAGE','PERM_ORDER_VIEW');
--
-- 范围外观察（不在本迁移处理，报告 Owner）：
--   OrderNewController:38 引用第 3 个码 'order:add'，N15 卡范围为 2 码。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 权限种子（2 个权限码）
-- ------------------------------------------------------------
INSERT INTO permissions (permission_id_str, permission_name, permission_code, permission_type, module, status, create_time, update_time, deleted)
SELECT 'PERM_ORDER_MANAGE', '订单管理', 'order:manage', 1, 'order', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE permission_id_str = 'PERM_ORDER_MANAGE'
);

INSERT INTO permissions (permission_id_str, permission_name, permission_code, permission_type, module, status, create_time, update_time, deleted)
SELECT 'PERM_ORDER_VIEW', '查看订单', 'order:view', 1, 'order', 1, NOW(), NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE permission_id_str = 'PERM_ORDER_VIEW'
);

-- ------------------------------------------------------------
-- 2. 角色绑定（5 角色 × 2 码 = 10 行）
--    注：role_id/permission_id 均存字符串 ID，
--        与 UserMapper.xml 的 role_id_str / permission_id_str join 对齐。
-- ------------------------------------------------------------
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT '1', 'PERM_ORDER_MANAGE', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = '1' AND permission_id = 'PERM_ORDER_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT '1', 'PERM_ORDER_VIEW', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = '1' AND permission_id = 'PERM_ORDER_VIEW'
);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_ADMIN', 'PERM_ORDER_MANAGE', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_ADMIN' AND permission_id = 'PERM_ORDER_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_ADMIN', 'PERM_ORDER_VIEW', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_ADMIN' AND permission_id = 'PERM_ORDER_VIEW'
);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_OWNER', 'PERM_ORDER_MANAGE', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_OWNER' AND permission_id = 'PERM_ORDER_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_OWNER', 'PERM_ORDER_VIEW', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_OWNER' AND permission_id = 'PERM_ORDER_VIEW'
);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_OPS_DIRECTOR', 'PERM_ORDER_MANAGE', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_OPS_DIRECTOR' AND permission_id = 'PERM_ORDER_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_OPS_DIRECTOR', 'PERM_ORDER_VIEW', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_OPS_DIRECTOR' AND permission_id = 'PERM_ORDER_VIEW'
);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_STORE_MANAGER', 'PERM_ORDER_MANAGE', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_STORE_MANAGER' AND permission_id = 'PERM_ORDER_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_STORE_MANAGER', 'PERM_ORDER_VIEW', 0, NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions WHERE role_id = 'ROLE_STORE_MANAGER' AND permission_id = 'PERM_ORDER_VIEW'
);
