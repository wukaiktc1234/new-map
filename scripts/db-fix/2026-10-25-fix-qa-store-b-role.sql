SET client_encoding='UTF8';
BEGIN;
-- 清理我上一轮误插的数字 role_id 死行（鉴权按 role_id_str 关联，数字行永不命中）
DELETE FROM user_roles WHERE role_id IN ('4','9','13','323') AND user_id IN (SELECT user_id::text FROM users WHERE username LIKE 'qa_%');
-- qa_store_b 补正确的 ROLE_STORE_MANAGER
INSERT INTO user_roles (user_id, role_id, deleted, create_time)
SELECT u.user_id::text, 'ROLE_STORE_MANAGER', 0, now() FROM users u WHERE u.username='qa_store_b'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id=u.user_id::text AND ur.role_id='ROLE_STORE_MANAGER');
COMMIT;
SELECT u.username, COALESCE(string_agg(r.role_code, ',' ORDER BY r.role_code), '(none)') AS resolved_roles
  FROM users u LEFT JOIN user_roles ur ON ur.user_id=u.user_id::text AND ur.deleted=0
  LEFT JOIN roles r ON r.role_id_str=ur.role_id AND r.deleted=0
 WHERE u.username LIKE 'qa_%' GROUP BY u.username ORDER BY u.username;