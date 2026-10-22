-- ============================================================
-- 权限树 500 修复：permissions.parent_id 脏值归一（非数字 -> 数字 id / NULL）
--
-- 现场（2026-10-15 活体验证）：
--   GET /api/v1/permissions/tree-nodes -> 500
--   根因：Permission.parentId 为 Long，而 permissions.parent_id 是 varchar；
--         存在 1 行脏值：permission_id=32599（finance:payment:confirm）
--         parent_id = 'PERM_FINANCE_VIEW'（写成了 permission_id_str 而非数字 id）
--         -> JDBC 反序列化抛
--            DataIntegrityViolationException: 不良的类型值 long : PERM_FINANCE_VIEW
--   该行 create_time = 2026-09-05 15:37:17，属**历史数据**，与 2026-10-15 的权限补码迁移无关
--   （V20261015_001 插入的 286 行 parent_id 全为 NULL）。
--
-- 本迁移（幂等，可重复执行）：
--   1) 非数字 parent_id 且能在 permission_id_str 里找到对应行 -> 改写为对应数字 permission_id
--   2) 非数字 parent_id 且找不到对应行 -> 置 NULL（避免继续打穿权限树）
--
-- 回退：无（数据归一化不可逆；如需还原该行，执行
--   UPDATE permissions SET parent_id = 'PERM_FINANCE_VIEW' WHERE permission_id = 32599;
--   但还原即恢复 500 故障，不建议）。
-- ============================================================

-- 1. 可解析：'PERM_XXX' -> 对应 permission_id
UPDATE permissions AS child
SET parent_id = parent.permission_id::text,
    update_time = NOW()
FROM permissions AS parent
WHERE child.deleted = 0
  AND child.parent_id IS NOT NULL
  AND child.parent_id !~ '^[0-9]+$'
  AND parent.deleted = 0
  AND parent.permission_id_str = child.parent_id;

-- 2. 不可解析：置 NULL（成为根节点，不影响权限判定，仅影响树层级展示）
UPDATE permissions AS child
SET parent_id = NULL,
    update_time = NOW()
WHERE child.deleted = 0
  AND child.parent_id IS NOT NULL
  AND child.parent_id !~ '^[0-9]+$'
  AND NOT EXISTS (
      SELECT 1 FROM permissions p2
      WHERE p2.permission_id_str = child.parent_id AND p2.deleted = 0
  );

-- 3. 自检（执行后人工核对，期望 non_numeric = 0）
-- SELECT count(*) AS non_numeric FROM permissions
--  WHERE deleted = 0 AND parent_id IS NOT NULL AND parent_id !~ '^[0-9]+$';
-- SELECT permission_id, permission_code, parent_id FROM permissions WHERE permission_id = 32599;
