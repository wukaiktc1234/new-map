-- ============================================================
-- 安全红线：此脚本仅用于测试环境，禁止在生产环境执行
-- 已从 src/main/resources/db/migration/ 移动到 src/test/resources/db/migration/
-- Flyway 仅在主资源目录扫描迁移脚本，此文件不会在生产环境被自动执行
-- ============================================================
-- S9a-2 幂等化（Owner 裁定：test resources → 改幂等，ON CONFLICT DO NOTHING；纳入 S9a-2）：
-- 1. 对齐活体 users schema：PK user_id / UNIQUE username /
--    NOT NULL 仅 (user_id, username, password)；
--    （旧版 `id`/`created_at`/`updated_at` 列在活体表不存在，原脚本不可执行）
-- 2. 对齐活体 user_roles schema：create_time（旧版 created_at 不存在）；
--    UQ(user_id, role_id)；user_id 为 varchar
-- 3. 移除原版第 3 条 sys_role_permissions 授权语句：该表在活体库不存在（schema 漂移），
--    既不可执行也无法幂等化；授权职责归应用初始化（role_permissions/permissions 初始化逻辑）
-- 4. 全部语句幂等：ON CONFLICT DO NOTHING（重复执行不报错、不产生重复行）

-- 1. 插入测试admin用户
-- ID使用999，避免与生产admin（user_id=1）冲突
-- 密码使用BCrypt加密：Test@123456 -> $2a$10$N9qco8iKW2E...（这是标准的BCrypt加密）
INSERT INTO users (
  user_id,
  username,
  password,
  email,
  status,
  roles,
  create_time,
  update_time
) VALUES (
  999,                                    -- 使用大数字ID，避免与生产用户冲突
  'test-admin',                          -- 测试专用用户名
  '$2a$10$N9qco8iKW2E...',               -- 密码：Test@123456的BCrypt加密值
  'test-admin@example.com',              -- 测试专用邮箱
  1,                                     -- 状态：启用
  '["ROLE_ADMIN"]',                      -- 角色：超级管理员
  NOW(),                                 -- 创建时间
  NOW()                                  -- 更新时间
)
ON CONFLICT DO NOTHING;

-- 2. 为测试admin用户添加角色关联（UQ(user_id, role_id)，已存在则跳过）
INSERT INTO user_roles (user_id, role_id, create_time)
VALUES ('999', 'ROLE_ADMIN', NOW())
ON CONFLICT DO NOTHING;

-- 3. 验证插入结果（SELECT 不影响幂等性）
SELECT '测试admin用户创建成功' AS result;
