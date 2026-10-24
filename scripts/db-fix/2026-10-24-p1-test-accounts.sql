-- 2026-10-24 P1 验收测试账号（**仅测试环境**）：qa_teller（出纳）+ qa_approver（财务经理，非出纳审核人）
-- 用途：AC-44（V8 三条用例）、AC-35（四眼）需要三个不同身份的账号；活库没有 ROLE_TELLER 用户。
-- 做法：复制 qa_admin 的 bcrypt 口令（同一口令 Admin@123456），仅授予所需角色；**不授予任何超管角色**。
-- 幂等：按 username 判重；带备份表与回滚。生产环境执行前请先确认是否允许存在测试账号。
BEGIN;

CREATE TABLE IF NOT EXISTS users_backup_20261024_p1_test AS
SELECT user_id, username, password, status, roles, deleted FROM users WHERE username IN ('qa_teller','qa_approver');

-- 1) 账号（不存在则建）
INSERT INTO users (user_id, username, password, name, status, deleted, roles, create_time, update_time)
SELECT (SELECT COALESCE(MAX(user_id),0)+10 FROM users), 'qa_teller',
       (SELECT password FROM users WHERE username='qa_admin'), 'QA出纳（测试）', 1, 0, '[]', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='qa_teller');

INSERT INTO users (user_id, username, password, name, status, deleted, roles, create_time, update_time)
SELECT (SELECT COALESCE(MAX(user_id),0)+11 FROM users), 'qa_approver',
       (SELECT password FROM users WHERE username='qa_admin'), 'QA财务经理（测试）', 1, 0, '[]', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='qa_approver');

-- 2) 角色绑定（user_roles.role_id 存 role_id_str）
INSERT INTO user_roles (user_id, role_id, deleted, create_time)
SELECT u.user_id::text, 'ROLE_TELLER', 0, now() FROM users u
WHERE u.username='qa_teller'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id::text=u.user_id::text AND ur.role_id='ROLE_TELLER' AND COALESCE(ur.deleted,0)=0);

INSERT INTO user_roles (user_id, role_id, deleted, create_time)
SELECT u.user_id::text, 'ROLE_FINANCE_MANAGER', 0, now() FROM users u
WHERE u.username='qa_approver'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id::text=u.user_id::text AND ur.role_id='ROLE_FINANCE_MANAGER' AND COALESCE(ur.deleted,0)=0);

-- 3) 审计留痕
INSERT INTO permission_assignment_log (employee_id, operation_type, old_roles, new_roles, operator, operated_at, remark)
SELECT '-', 'TEST_ACCOUNT_SEED', NULL, x.username || '=>' || x.role_id_str, 'night-handover', now(), 'P1 验收测试账号（AC-44/AC-35）'
FROM (VALUES ('qa_teller','ROLE_TELLER'), ('qa_approver','ROLE_FINANCE_MANAGER')) x(username, role_id_str)
WHERE NOT EXISTS (SELECT 1 FROM permission_assignment_log l WHERE l.operation_type='TEST_ACCOUNT_SEED' AND l.new_roles = x.username || '=>' || x.role_id_str);

COMMIT;

-- 验证
SELECT u.user_id, u.username, u.status,
       (SELECT string_agg(ur.role_id, ',' ORDER BY ur.role_id) FROM user_roles ur WHERE ur.user_id::text=u.user_id::text AND COALESCE(ur.deleted,0)=0) AS roles
FROM users u WHERE u.username IN ('qa_teller','qa_approver') ORDER BY u.username;

-- 回滚：DELETE FROM user_roles WHERE user_id::text IN (SELECT user_id::text FROM users WHERE username IN ('qa_teller','qa_approver'));
--       UPDATE users SET deleted=1 WHERE username IN ('qa_teller','qa_approver');
-- ============ P2 前置补充（2026-10-25）：第二个出纳 qa_teller2（用于 AC-44②「另一出纳审核也拒」）============
INSERT INTO users (user_id, username, password, name, status, deleted, roles, create_time, update_time)
SELECT (SELECT COALESCE(MAX(user_id),0)+12 FROM users), 'qa_teller2',
       (SELECT password FROM users WHERE username='qa_admin'), 'QA-teller2', 1, 0, '[]', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='qa_teller2');

INSERT INTO user_roles (user_id, role_id, deleted, create_time)
SELECT u.user_id::text, 'ROLE_TELLER', 0, now() FROM users u
WHERE u.username='qa_teller2'
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id::text=u.user_id::text AND ur.role_id='ROLE_TELLER' AND COALESCE(ur.deleted,0)=0);

INSERT INTO permission_assignment_log (employee_id, operation_type, old_roles, new_roles, operator, operated_at, remark)
SELECT '-', 'TEST_ACCOUNT_SEED', NULL, 'qa_teller2=>ROLE_TELLER', 'night-handover', now(), 'P2 前置：AC-44② 第二出纳'
WHERE NOT EXISTS (SELECT 1 FROM permission_assignment_log WHERE operation_type='TEST_ACCOUNT_SEED' AND new_roles='qa_teller2=>ROLE_TELLER');
