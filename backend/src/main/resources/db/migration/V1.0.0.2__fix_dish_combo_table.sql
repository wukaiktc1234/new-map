-- Flyway Migration: V1.0.0.2__fix_dish_combo_table.sql
-- Description: 修复dish_combo表结构以匹配实体类（PostgreSQL兼容）
-- 原脚本使用MySQL动态SQL（SET/PREPARE/EXECUTE），已改写为PG兼容的DO块

DO $$
BEGIN
    -- 添加combo_code列（如果不存在）
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'dish_combo' AND column_name = 'combo_code'
    ) THEN
        ALTER TABLE dish_combo ADD COLUMN combo_code VARCHAR(50);
        COMMENT ON COLUMN dish_combo.combo_code IS '套餐编码';
    END IF;

    -- 重命名name列为combo_name（如果name列存在且combo_name不存在）
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'dish_combo' AND column_name = 'name'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'dish_combo' AND column_name = 'combo_name'
    ) THEN
        ALTER TABLE dish_combo RENAME COLUMN name TO combo_name;
    END IF;

    -- 删除dishes列（如果存在）
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'dish_combo' AND column_name = 'dishes'
    ) THEN
        ALTER TABLE dish_combo DROP COLUMN dishes;
    END IF;

    -- 删除priority列（如果存在）
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'dish_combo' AND column_name = 'priority'
    ) THEN
        ALTER TABLE dish_combo DROP COLUMN priority;
    END IF;

    -- 删除people_count列（如果存在）
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'dish_combo' AND column_name = 'people_count'
    ) THEN
        ALTER TABLE dish_combo DROP COLUMN people_count;
    END IF;

    -- 删除combo_type列（如果存在）
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'dish_combo' AND column_name = 'combo_type'
    ) THEN
        ALTER TABLE dish_combo DROP COLUMN combo_type;
    END IF;

    -- 删除version列（如果存在）
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'dish_combo' AND column_name = 'version'
    ) THEN
        ALTER TABLE dish_combo DROP COLUMN version;
    END IF;
END $$;
