-- 为label_template表添加version字段用于乐观锁（PostgreSQL兼容）
ALTER TABLE label_template ADD COLUMN IF NOT EXISTS version INT DEFAULT 0;

COMMENT ON COLUMN label_template.version IS '乐观锁版本号';

-- 为现有数据设置初始版本号
UPDATE label_template SET version = 0 WHERE version IS NULL;
