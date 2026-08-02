-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 配料管理相关表创建脚本（PostgreSQL兼容版本）

-- 创建配料表
CREATE TABLE IF NOT EXISTS ingredients (
    ingredient_id          VARCHAR(50) PRIMARY KEY,
    ingredient_code        VARCHAR(50) NOT NULL,
    ingredient_name        VARCHAR(100) NOT NULL,
    category_id            VARCHAR(50),
    category_name          VARCHAR(100),
    specification          VARCHAR(100),
    unit                   VARCHAR(20) NOT NULL,
    cost_price             DECIMAL(10, 2) DEFAULT 0.00,
    supplier_id            VARCHAR(50),
    supplier_name          VARCHAR(100),
    current_stock_quantity DECIMAL(10, 2) DEFAULT 0.00,
    min_stock_quantity     DECIMAL(10, 2) DEFAULT 0.00,
    shelf_life_days        INTEGER,
    storage_conditions      VARCHAR(200),
    ingredient_status      INTEGER DEFAULT 1,
    remarks                TEXT,
    create_time            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by              VARCHAR(50),
    update_by              VARCHAR(50),
    deleted                INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ingredients_ingredient_code ON ingredients(ingredient_code);
CREATE INDEX IF NOT EXISTS idx_ingredients_category_id ON ingredients(category_id);
CREATE INDEX IF NOT EXISTS idx_ingredients_supplier_id ON ingredients(supplier_id);
CREATE INDEX IF NOT EXISTS idx_ingredients_status ON ingredients(ingredient_status);

COMMENT ON TABLE ingredients IS '配料表';
COMMENT ON COLUMN ingredients.ingredient_id IS '配料ID';
COMMENT ON COLUMN ingredients.ingredient_code IS '配料编码';
COMMENT ON COLUMN ingredients.ingredient_name IS '配料名称';

-- 创建配料分类表
CREATE TABLE IF NOT EXISTS ingredient_categories (
    category_id       VARCHAR(50) PRIMARY KEY,
    category_name     VARCHAR(100) NOT NULL,
    parent_id         VARCHAR(50),
    category_level    INTEGER DEFAULT 1,
    sort_order        INTEGER DEFAULT 0,
    category_status   INTEGER DEFAULT 1,
    remarks           TEXT,
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by         VARCHAR(50),
    update_by         VARCHAR(50),
    deleted           INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ingredient_categories_parent_id ON ingredient_categories(parent_id);

COMMENT ON TABLE ingredient_categories IS '配料分类表';

-- 创建菜品配料关联表
CREATE TABLE IF NOT EXISTS dish_ingredient (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dish_id          VARCHAR(50) NOT NULL,
    dish_name        VARCHAR(100),
    ingredient_id    VARCHAR(50) NOT NULL,
    ingredient_name  VARCHAR(100),
    ingredient_code  VARCHAR(50),
    quantity         DECIMAL(10, 2) NOT NULL,
    unit             VARCHAR(20) NOT NULL,
    cost_price       DECIMAL(10, 2) DEFAULT 0.00,
    total_cost       DECIMAL(10, 2) DEFAULT 0.00,
    remark           TEXT,
    create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by        VARCHAR(50),
    update_by        VARCHAR(50),
    deleted          INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_dish_ingredient_dish_id ON dish_ingredient(dish_id);
CREATE INDEX IF NOT EXISTS idx_dish_ingredient_ingredient_id ON dish_ingredient(ingredient_id);

COMMENT ON TABLE dish_ingredient IS '菜品配料关联表';

-- 插入默认配料分类数据
INSERT INTO ingredient_categories (category_id, category_name, parent_id, category_level, sort_order, category_status, create_by)
VALUES
    ('IC001', '蔬菜类', NULL, 1, 1, 1, 'system'),
    ('IC002', '肉类', NULL, 1, 2, 1, 'system'),
    ('IC003', '水产类', NULL, 1, 3, 1, 'system'),
    ('IC004', '调味品类', NULL, 1, 4, 1, 'system'),
    ('IC005', '粮油类', NULL, 1, 5, 1, 'system'),
    ('IC006', '干货类', NULL, 1, 6, 1, 'system')
ON CONFLICT (category_id) DO NOTHING;

-- 插入默认配料数据
INSERT INTO ingredients (ingredient_id, ingredient_code, ingredient_name, category_id, category_name, specification, unit, cost_price, current_stock_quantity, min_stock_quantity, ingredient_status, create_by)
VALUES
    ('I00001', 'P+00001', '土豆', 'IC001', '蔬菜类', '500g/个', 'kg', 3.50, 100.00, 10.00, 1, 'system'),
    ('I00002', 'P+00002', '白菜', 'IC001', '蔬菜类', '500g/颗', 'kg', 2.00, 50.00, 5.00, 1, 'system'),
    ('I00003', 'P+00003', '猪肉', 'IC002', '肉类', '五花肉', 'kg', 28.00, 30.00, 5.00, 1, 'system'),
    ('I00004', 'P+00004', '鸡肉', 'IC002', '肉类', '鸡胸肉', 'kg', 18.00, 40.00, 5.00, 1, 'system'),
    ('I00005', 'P+00005', '鱼', 'IC003', '水产类', '草鱼', 'kg', 15.00, 20.00, 3.00, 1, 'system'),
    ('I00006', 'P+00006', '盐', 'IC004', '调味品类', '食用盐', 'kg', 2.50, 200.00, 20.00, 1, 'system'),
    ('I00007', 'P+00007', '油', 'IC005', '粮油类', '花生油', 'kg', 12.00, 50.00, 10.00, 1, 'system'),
    ('I00008', 'P+00008', '香菇', 'IC006', '干货类', '干香菇', 'kg', 80.00, 15.00, 2.00, 1, 'system')
ON CONFLICT (ingredient_id) DO NOTHING;
