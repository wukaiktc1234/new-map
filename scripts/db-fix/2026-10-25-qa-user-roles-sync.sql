SET client_encoding='UTF8';
INSERT INTO user_roles (user_id, role_id, deleted, create_time)
SELECT u.user_id::text, '9', 0, now() FROM users u WHERE u.username='qa_store_b'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.user_id::text AND ur.role_id = '9');
-- 同时补 users.roles 与 user_roles 一致性（A6 观测项，qa_* 账号历史遗留空 user_roles）
INSERT INTO user_roles (user_id, role_id, deleted, create_time)
SELECT u.user_id::text, r.role_id::text, 0, now()
  FROM users u
  CROSS JOIN LATERAL jsonb_array_elements_text(CASE WHEN u.roles IS NULL OR u.roles::text = '' THEN '[]'::jsonb ELSE u.roles::jsonb END) AS j(rid)
  JOIN roles r ON r.role_id::text = trim(both '"' from j.rid)
 WHERE u.username LIKE 'qa_%' AND u.deleted = 0
   AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.user_id::text AND ur.role_id = r.role_id::text);
SELECT u.username, u.roles::text AS users_roles, COALESCE(string_agg(ur.role_id, ',' ORDER BY ur.role_id), '') AS user_roles
  FROM users u LEFT JOIN user_roles ur ON ur.user_id = u.user_id::text AND ur.deleted = 0
 WHERE u.username LIKE 'qa_%' GROUP BY u.username, u.roles ORDER BY u.username;