-- Flyway Migration: V1.0.0.3__restore_dish_combo_columns.sql
-- Description: 恢复dish_combo表中被误删的列（PostgreSQL兼容）
-- 使用 ADD COLUMN IF NOT EXISTS 避免 DO 块，适配 ScriptUtils 按分号拆分

ALTER TABLE dish_combo ADD COLUMN IF NOT EXISTS combo_type VARCHAR(20) DEFAULT 'regular';
COMMENT ON COLUMN dish_combo.combo_type IS '套餐类型：regular-普通套餐，special-特色套餐，family-家庭套餐';

ALTER TABLE dish_combo ADD COLUMN IF NOT EXISTS people_count INTEGER DEFAULT 1;
COMMENT ON COLUMN dish_combo.people_count IS '适用人数';
