SET client_encoding='UTF8';
BEGIN;
DO $$
DECLARE v_pwd TEXT; v_uid BIGINT; v_cnt INT;
BEGIN
  SELECT count(*) INTO v_cnt FROM users WHERE username='qa_auditor';
  IF v_cnt = 0 THEN
    SELECT password INTO v_pwd FROM users WHERE username='qa_admin' LIMIT 1;
    INSERT INTO users (username, password, name, status, roles, role, deleted, location_id, need_change_password)
    VALUES ('qa_auditor', v_pwd, 'QA 审计', 1, '["13"]'::jsonb, 'auditor', 0, NULL, false)
    RETURNING user_id INTO v_uid;
  ELSE
    SELECT user_id INTO v_uid FROM users WHERE username='qa_auditor';
    UPDATE users SET deleted=0, status=1 WHERE user_id=v_uid;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id=v_uid::text AND ur.role_id='ROLE_AUDITOR' AND ur.deleted=0) THEN
    INSERT INTO user_roles (user_id, role_id, deleted, create_time) VALUES (v_uid::text, 'ROLE_AUDITOR', 0, now());
  END IF;
END $$;
COMMIT;
\echo '=== 执行后回查（按鉴权真实 JOIN）==='
SELECT u.username, u.location_id, COALESCE(string_agg(r.role_code||'('||r.data_scope||')', ',' ORDER BY r.role_code),'(none)') AS resolved
  FROM users u LEFT JOIN user_roles ur ON ur.user_id=u.user_id::text AND ur.deleted=0
  LEFT JOIN roles r ON r.role_id_str=ur.role_id AND r.deleted=0
 WHERE u.username IN ('qa_auditor','qa_approver','qa_store_b','qa_admin') GROUP BY u.username, u.location_id ORDER BY u.username;