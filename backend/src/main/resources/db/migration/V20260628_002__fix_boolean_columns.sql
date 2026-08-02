-- ============================================================
-- 修复 users 表中 Boolean 字段的类型
-- is_locked 和 need_change_password 原为 SMALLINT，
-- 但 Java 实体映射为 Boolean 类型导致 PostgreSQL 类型不匹配
-- ============================================================

-- H2 兼容模式不支持 ALTER COLUMN ... TYPE BOOLEAN USING，
-- 需要先删除再添加列

-- 备份 is_locked 数据
ALTER TABLE users ADD COLUMN is_locked_new BOOLEAN DEFAULT FALSE;
UPDATE users SET is_locked_new = CASE WHEN is_locked = 1 THEN TRUE ELSE FALSE END;
ALTER TABLE users DROP COLUMN is_locked;
ALTER TABLE users RENAME COLUMN is_locked_new TO is_locked;

-- 备份 need_change_password 数据
ALTER TABLE users ADD COLUMN need_change_password_new BOOLEAN DEFAULT FALSE;
UPDATE users SET need_change_password_new = CASE WHEN need_change_password = 1 THEN TRUE ELSE FALSE END;
ALTER TABLE users DROP COLUMN need_change_password;
ALTER TABLE users RENAME COLUMN need_change_password_new TO need_change_password;
