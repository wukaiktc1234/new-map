-- ============================================================
-- 修复 admin 用户 roles 字段（无效JSON）和 SUPER_ADMIN 角色 data_scope
-- 问题：
--   1. admin 用户的 roles 字段为无效 JSON '[" 1\]'，导致 JSON 解析失败
--      PermissionVerifyServiceImpl.loadDataScopeFromDatabase() 抛异常，返回 "self"
--      使 admin 用户的数据权限被限制为 "self"，无法查看门店数据
--   2. SUPER_ADMIN 角色的 data_scope 错误为 "self"，应为 "all"
--      导致管理员无法访问全部门店数据
-- 影响：
--   - 运营总览页面统计全部显示0（activeStores=0, todayOrderCount=0, todayRevenue=0）
--   - 数据权限服务返回空列表，OperationsDashboardDataService 限制到 NO_ACCESS_STORE_ID=-1
-- ============================================================

-- 兼容新库：确保 roles 表存在 data_scope 字段
ALTER TABLE roles ADD COLUMN IF NOT EXISTS data_scope VARCHAR(20);

-- 步骤1: 修复 admin 用户的 roles 字段（无效JSON → 有效JSON数组）
-- 原值: '[" 1\]'  无效JSON，Jackson 解析抛 JsonParseException
-- 新值: '["1"]'   有效JSON，包含角色ID=1（SUPER_ADMIN）
UPDATE users
SET roles = '["1"]'::jsonb
WHERE username = 'admin'
  AND (
      roles IS NULL
      OR roles::text = '[" 1\]'
      OR roles::text NOT LIKE '%1%'
  );

-- 步骤2: 修复 SUPER_ADMIN 角色 data_scope（self → all）
-- 超级管理员应具有全部数据权限，而非仅本人数据
UPDATE roles
SET data_scope = 'all'
WHERE role_code = 'SUPER_ADMIN'
  AND (data_scope IS NULL OR data_scope = 'self' OR data_scope != 'all');

-- 步骤3: 验证修复结果（用于诊断，不修改数据）
-- 修复后: admin 用户的 roles 应为 ["1"]，SUPER_ADMIN 的 data_scope 应为 all
-- 注意：PostgreSQL 18 禁止在 JOIN 条件中使用返回集合的函数，改用 LATERAL 展开
SELECT
    'AFTER_FIX' AS check_point,
    u.username,
    u.roles::text AS roles_json,
    r.role_code,
    r.data_scope
FROM users u
LEFT JOIN LATERAL jsonb_array_elements_text(u.roles::jsonb) AS role_id_text ON true
LEFT JOIN roles r ON r.role_id::text = role_id_text
WHERE u.username = 'admin';
