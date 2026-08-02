-- 修复food_category表status列类型（PostgreSQL兼容）
ALTER TABLE food_category ALTER COLUMN status TYPE VARCHAR(20);
ALTER TABLE food_category ALTER COLUMN status SET DEFAULT 'active';
