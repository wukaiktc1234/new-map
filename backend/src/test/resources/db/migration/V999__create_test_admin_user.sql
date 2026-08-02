-- ============================================================
-- 安全红线：此脚本仅用于测试环境，禁止在生产环境执行
-- 已从 src/main/resources/db/migration/ 移动到 src/test/resources/db/migration/
-- Flyway 仅在主资源目录扫描迁移脚本，此文件不会在生产环境被自动执行
-- ============================================================

-- 创建测试专用的admin用户
-- ID使用999，避免与生产admin（ID=1）冲突
-- 密码使用BCrypt加密：Test@123456 -> $2a$10$N9qco8iKW2E...（这是标准的BCrypt加密）

-- 1. 插入测试admin用户
INSERT INTO users (
  id,
  username,
  password,
  email,
  status,
  roles,
  created_at,
  updated_at
) VALUES (
  999,                                    -- 使用大数字ID，避免与生产用户冲突
  'test-admin',                            -- 测试专用用户名
  '$2a$10$N9qco8iKW2E...',  -- 密码：Test@123456的BCrypt加密值
  'test-admin@example.com',                -- 测试专用邮箱
  '1',                                     -- 状态：启用
  '["ROLE_ADMIN"]',                        -- 角色：超级管理员
  NOW(),                                   -- 创建时间
  NOW()                                    -- 更新时间
);

-- 2. 为测试admin用户添加角色关联
INSERT INTO user_roles (user_id, role_id, created_at)
VALUES (999, 'ROLE_ADMIN', NOW());

-- 3. 为测试admin用户添加所有权限
-- 获取所有权限ID并关联
INSERT INTO sys_role_permissions (role_id, permission_id, created_at)
SELECT 'ROLE_ADMIN', id, NOW()
FROM sys_permissions
WHERE status = '1';

-- 4. 验证插入结果
SELECT '测试admin用户创建成功' AS result;
