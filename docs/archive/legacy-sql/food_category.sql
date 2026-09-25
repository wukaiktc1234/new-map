-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 创建菜品分类表（PostgreSQL兼容版本）
-- 注意：V1.0.0__init_postgresql.sql 中已有 food_category 表定义

CREATE TABLE IF NOT EXISTS food_category (
    id             VARCHAR(50) PRIMARY KEY,
    category_name  VARCHAR(50) NOT NULL,
    category_code  VARCHAR(50),
    description    VARCHAR(200),
    sort_order     INTEGER DEFAULT 0,
    status         VARCHAR(20) DEFAULT 'active',
    create_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by      VARCHAR(50),
    update_by      VARCHAR(50)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_food_category_name ON food_category(category_name);
CREATE INDEX IF NOT EXISTS idx_food_category_status ON food_category(status);

COMMENT ON TABLE food_category IS '菜品分类表';
COMMENT ON COLUMN food_category.id IS '分类ID';
COMMENT ON COLUMN food_category.category_name IS '分类名称';

-- 初始化默认分类数据
INSERT INTO food_category (id, category_name, category_code, description, sort_order, status)
VALUES
    ('1', '热菜', 'hot_dish', '热菜类菜品', 1, 'active'),
    ('2', '海鲜', 'seafood', '海鲜类菜品', 2, 'active'),
    ('3', '汤品', 'soup', '汤品类', 3, 'active'),
    ('4', '素菜', 'vegetable', '素菜类', 4, 'active'),
    ('5', '主食', 'staple', '主食类', 5, 'active')
ON CONFLICT (id) DO NOTHING;
