-- ============================================================
-- users.roles 与 user_roles 偏差审计（2026-10-24，只读，不改任何数据）
--
-- 背景（P1-AUTH-CHAIN-001 C 卡）
--   鉴权唯一真相源已切换为：user_roles → roles(role_id_str/role_code) → role_permissions → permissions。
--   users.roles（JSON/数组文本列，历史存数字角色ID 或 ROLE_XXX 文本）自本卡起**只作历史展示字段**，
--   任何鉴权/授权判定都不再读取它。
--
--   本脚本只做体检，供 Owner 决定是否清洗 users.roles：
--   ⚠ 不执行任何 UPDATE / DELETE，不修改 users.roles 内容。
--
-- 输出
--   [A] users.roles 有值、但 user_roles 没有任何行 → 切换真相源后**实际无授权**（重点，需人工补授权）
--   [B] user_roles 已授角色、但 users.roles 为空 → 仅展示不一致，不影响权限
--   [C] 两张表都没有角色 → 登录后无任何角色（fail-closed）
--   [D] 汇总行数
--
-- 用法：psql -h localhost -p 5432 -U postgres -d food_traceability -f <本文件>
-- ============================================================

\pset pager off

-- [A] 只写 users.roles、user_roles 无行（切换后掉权风险）
SELECT '[A] users.roles 有值 / user_roles 无行（切换后实际无授权）' AS section,
       u.user_id, u.username, u.status, u.roles AS users_roles_raw
FROM users u
WHERE u.deleted = 0
  AND u.roles IS NOT NULL AND u.roles <> '' AND u.roles <> '[]'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.user_id::text AND ur.deleted = 0)
ORDER BY u.user_id;

-- [B] user_roles 有授权、users.roles 缺失（展示不一致，不影响权限）
SELECT '[B] user_roles 已授权 / users.roles 为空（不影响权限）' AS section,
       u.user_id, u.username, u.roles AS users_roles_raw,
       string_agg(r.role_id_str || '(' || r.role_name || ')', '、' ORDER BY r.role_id) AS granted_roles
FROM users u
JOIN user_roles ur ON ur.user_id = u.user_id::text AND ur.deleted = 0
JOIN roles r ON r.role_id_str = ur.role_id AND r.deleted = 0
WHERE u.deleted = 0
  AND (u.roles IS NULL OR u.roles = '' OR u.roles = '[]')
GROUP BY u.user_id, u.username, u.roles
ORDER BY u.user_id;

-- [C] 两张表都没有角色
SELECT '[C] 无任何角色（users.roles 与 user_roles 都空）' AS section,
       u.user_id, u.username, u.status, u.roles AS users_roles_raw
FROM users u
WHERE u.deleted = 0
  AND (u.roles IS NULL OR u.roles = '' OR u.roles = '[]')
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.user_id::text AND ur.deleted = 0)
ORDER BY u.user_id;

-- [D] 汇总
SELECT '[D] 汇总' AS section,
       (SELECT count(*) FROM users WHERE deleted = 0) AS total_users,
       (SELECT count(*) FROM users u WHERE u.deleted = 0
          AND u.roles IS NOT NULL AND u.roles <> '' AND u.roles <> '[]'
          AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.user_id::text AND ur.deleted = 0)) AS only_users_roles,
       (SELECT count(DISTINCT u.user_id) FROM users u
          JOIN user_roles ur ON ur.user_id = u.user_id::text AND ur.deleted = 0
        WHERE u.deleted = 0 AND (u.roles IS NULL OR u.roles = '' OR u.roles = '[]')) AS only_user_roles,
       (SELECT count(*) FROM users u WHERE u.deleted = 0
          AND (u.roles IS NULL OR u.roles = '' OR u.roles = '[]')
          AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.user_id::text AND ur.deleted = 0)) AS no_roles_at_all;
