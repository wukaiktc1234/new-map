-- 2026-10-24 处置孤儿超管账号 test-admin（user_id=999 / user_roles.user_id 为 varchar）
-- 事实：status=1 启用 + user_roles 真实持有 ROLE_ADMIN；全仓 0 处引用（C 卡后仍是可登录超管入口）→ 停用 + 撤角色。
-- 注意类型：users.user_id bigint，user_roles.user_id varchar → 关联/过滤必须显式 ::text。
-- 幂等：可反复执行；带备份表与回滚语句。

BEGIN;

CREATE TABLE IF NOT EXISTS users_backup_20261024_test_admin AS
SELECT user_id, username, status, update_time FROM users WHERE user_id = 999;

CREATE TABLE IF NOT EXISTS user_roles_backup_20261024_test_admin AS
SELECT * FROM user_roles WHERE user_id::text = '999';

UPDATE users SET status = 0, update_time = now() WHERE user_id = 999 AND status <> 0;

UPDATE user_roles SET deleted = 1 WHERE user_id::text = '999' AND deleted = 0;

COMMIT;

SELECT 'after' AS phase, u.user_id, u.username, u.status,
       (SELECT count(*) FROM user_roles ur WHERE ur.user_id::text = u.user_id::text AND ur.deleted = 0) AS live_roles
FROM users u WHERE u.user_id = 999;

-- 回滚：
-- UPDATE users SET status = 1 WHERE user_id = 999;
-- UPDATE user_roles SET deleted = 0 WHERE user_id::text = '999';
