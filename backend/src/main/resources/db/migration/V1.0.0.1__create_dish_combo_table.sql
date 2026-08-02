-- Flyway Migration: V1.0.0.1__create_dish_combo_table.sql
-- Description: 创建套餐表（PostgreSQL兼容）
-- 注意: V1.0.0__init_postgresql.sql 中已包含 dish_combo 表定义
--       此迁移作为补充，确保在独立执行时也能正确建表

CREATE TABLE IF NOT EXISTS dish_combo (
    combo_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    combo_code VARCHAR(50),
    combo_name VARCHAR(100) NOT NULL,
    price      DECIMAL(10, 2),
    dishes     TEXT,
    description VARCHAR(500),
    status     VARCHAR(20) DEFAULT 'active',
    image_url  VARCHAR(500),
    priority   INTEGER DEFAULT 0,
    people_count INTEGER DEFAULT 1,
    combo_type VARCHAR(20) DEFAULT 'regular',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted    INTEGER DEFAULT 0,
    version    INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_dish_combo_combo_code ON dish_combo(combo_code);
CREATE INDEX IF NOT EXISTS idx_dish_combo_status ON dish_combo(status);

COMMENT ON TABLE dish_combo IS '套餐表';
COMMENT ON COLUMN dish_combo.combo_code IS '套餐编码';
COMMENT ON COLUMN dish_combo.combo_name IS '套餐名称';
