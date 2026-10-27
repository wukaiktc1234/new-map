-- ============================================================
-- 安全红线：此脚本仅用于测试环境，禁止在生产环境执行
-- 位于 src/test/resources/db/migration/（Flyway 主资源目录不含它）
-- ============================================================
-- 2026-10-27 修正（测试库隔离落地时暴露的两处 schema 漂移）：
--   ① 时间列名：活体是 create_time/update_time，但全新迁移出来的库是 created_at/updated_at
--      → 旧脚本在干净测试库上「列不存在」直接失败；
--   ② user_id 在全新迁移出来的库是 GENERATED ALWAYS AS IDENTITY → 不能显式插值
--      （活体是普通列，故旧脚本写死 999 在活体库能过、在测试库必炸）。
--   修法：DO 块探测实际形态（时间列名 + identity），两种库都能跑；保持幂等；
--         **不跳过 Flyway、不放宽任何断言**。

DO $mig$
DECLARE
  has_create_time boolean;
  uid_is_identity boolean;
  new_uid text;
BEGIN
  SELECT EXISTS (SELECT 1 FROM information_schema.columns
                  WHERE table_name = 'users' AND column_name = 'create_time') INTO has_create_time;
  SELECT is_identity = 'YES' FROM information_schema.columns
   WHERE table_name = 'users' AND column_name = 'user_id' INTO uid_is_identity;

  IF uid_is_identity THEN
    -- 让数据库生成 user_id（GENERATED ALWAYS），再按 username 取回
    IF has_create_time THEN
      INSERT INTO users (username, password, email, status, roles, create_time, update_time)
      VALUES ('test-admin', '$2a$10$N9qco8iKW2E...', 'test-admin@example.com', '1', '["ROLE_ADMIN"]', NOW(), NOW())
      ON CONFLICT DO NOTHING;
    ELSE
      INSERT INTO users (username, password, email, status, roles)
      VALUES ('test-admin', '$2a$10$N9qco8iKW2E...', 'test-admin@example.com', '1', '["ROLE_ADMIN"]')
      ON CONFLICT DO NOTHING;
    END IF;
  ELSE
    IF has_create_time THEN
      INSERT INTO users (user_id, username, password, email, status, roles, create_time, update_time)
      VALUES (999, 'test-admin', '$2a$10$N9qco8iKW2E...', 'test-admin@example.com', '1', '["ROLE_ADMIN"]', NOW(), NOW())
      ON CONFLICT DO NOTHING;
    ELSE
      INSERT INTO users (user_id, username, password, email, status, roles)
      VALUES (999, 'test-admin', '$2a$10$N9qco8iKW2E...', 'test-admin@example.com', '1', '["ROLE_ADMIN"]')
      ON CONFLICT DO NOTHING;
    END IF;
  END IF;

  SELECT user_id::text FROM users WHERE username = 'test-admin' LIMIT 1 INTO new_uid;
  IF new_uid IS NOT NULL THEN
    INSERT INTO user_roles (user_id, role_id, create_time)
    VALUES (new_uid, 'ROLE_ADMIN', NOW())
    ON CONFLICT DO NOTHING;
  END IF;
END $mig$;

SELECT '测试admin用户创建成功' AS result;