-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 创建套餐配料关联表（PostgreSQL兼容版本）
-- 注意：V6.0.0__create_order_product_tables.sql 中已有 combo_ingredients 表定义
--       此文件为历史遗留脚本

CREATE TABLE IF NOT EXISTS combo_ingredient (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    combo_id       BIGINT NOT NULL,
    ingredient_id  BIGINT NOT NULL,
    quantity       DECIMAL(10, 2) NOT NULL,
    unit           VARCHAR(20) NOT NULL,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted        INTEGER DEFAULT 0,
    version        INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_combo_ingredient_combo_id ON combo_ingredient(combo_id);
CREATE INDEX IF NOT EXISTS idx_combo_ingredient_ingredient_id ON combo_ingredient(ingredient_id);

COMMENT ON TABLE combo_ingredient IS '套餐配料关联表';
COMMENT ON COLUMN combo_ingredient.id IS '关联ID';
COMMENT ON COLUMN combo_ingredient.combo_id IS '套餐ID';
COMMENT ON COLUMN combo_ingredient.ingredient_id IS '配料ID';
COMMENT ON COLUMN combo_ingredient.quantity IS '配料用量';
