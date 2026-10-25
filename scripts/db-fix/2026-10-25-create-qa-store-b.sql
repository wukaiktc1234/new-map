-- P3-2 前置：建 qa_store_b（ROLE_STORE_MANAGER 角色 id=9，绑定 B 店 location_id=2），幂等
SET client_encoding='UTF8';
BEGIN;
DO $$
DECLARE v_exists INT; v_pwd TEXT; v_uid BIGINT;
BEGIN
  SELECT count(*) INTO v_exists FROM users WHERE username = 'qa_store_b';
  IF v_exists = 0 THEN
    SELECT password INTO v_pwd FROM users WHERE username = 'qa_admin' LIMIT 1;
    INSERT INTO users (username, password, name, status, roles, role, deleted, location_id, need_change_password)
    VALUES ('qa_store_b', v_pwd, 'QA 店长B', 1, '["9"]'::jsonb, 'store_manager', 0, 2, false)
    RETURNING user_id INTO v_uid;
    INSERT INTO user_roles (user_id, role_id) VALUES (v_uid, 9)
    ON CONFLICT DO NOTHING;
  ELSE
    SELECT user_id INTO v_uid FROM users WHERE username = 'qa_store_b';
    UPDATE users SET location_id = 2, roles = '["9"]'::jsonb, deleted = 0, status = 1 WHERE user_id = v_uid;
  END IF;
END $$;
COMMIT;
SELECT user_id, username, location_id, roles::text, (SELECT count(*) FROM user_roles ur WHERE ur.user_id = u.user_id) AS user_role_rows
  FROM users u WHERE username IN ('qa_store_b','qa_admin') ORDER BY username;