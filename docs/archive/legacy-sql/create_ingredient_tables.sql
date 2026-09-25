-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 创建配料表
CREATE TABLE IF NOT EXISTS ingredient (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '配料ID，主键',
    ingredient_code VARCHAR(20) NOT NULL COMMENT '配料编码，格式为I+00000',
    ingredient_name VARCHAR(100) NOT NULL COMMENT '配料名称',
    category_id BIGINT COMMENT '配料分类ID',
    category_name VARCHAR(50) COMMENT '配料分类名称',
    specification VARCHAR(100) COMMENT '规格',
    unit VARCHAR(20) COMMENT '单位（如：kg, g, ml, l等）',
    cost_price DECIMAL(10,2) DEFAULT 0.00 COMMENT '成本价（元）',
    supplier_id BIGINT COMMENT '供应商ID',
    supplier_name VARCHAR(100) COMMENT '供应商名称',
    stock_quantity DECIMAL(10,2) DEFAULT 0.00 COMMENT '库存数量',
    min_stock DECIMAL(10,2) DEFAULT 0.00 COMMENT '最低库存',
    max_stock DECIMAL(10,2) DEFAULT 0.00 COMMENT '最高库存',
    remark TEXT COMMENT '备注',
    status TINYINT DEFAULT 1 COMMENT '状态（1-启用，0-禁用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记（0-正常，1-删除）',
    PRIMARY KEY (id),
    UNIQUE KEY ingredient_code (ingredient_code),
    KEY idx_ingredient_name (ingredient_name),
    KEY idx_ingredient_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配料表';

-- 创建配料分类表
CREATE TABLE IF NOT EXISTS ingredient_category (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '配料分类ID，主键',
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID（0表示顶级分类）',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态（1-启用，0-禁用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记（0-正常，1-删除）',
    PRIMARY KEY (id),
    KEY idx_ingredient_category_parent (parent_id),
    KEY idx_ingredient_category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配料分类表';

-- 创建菜品-配料关联表
CREATE TABLE IF NOT EXISTS dish_ingredient (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID，主键',
    dish_id BIGINT NOT NULL COMMENT '菜品ID',
    dish_name VARCHAR(100) COMMENT '菜品名称',
    ingredient_id BIGINT NOT NULL COMMENT '配料ID',
    ingredient_name VARCHAR(100) COMMENT '配料名称',
    ingredient_code VARCHAR(20) COMMENT '配料编码',
    quantity DECIMAL(10,2) NOT NULL COMMENT '用量',
    unit VARCHAR(20) COMMENT '单位',
    cost_price DECIMAL(10,2) DEFAULT 0.00 COMMENT '配料成本价（元）',
    total_cost DECIMAL(10,2) DEFAULT 0.00 COMMENT '总成本（元）',
    remark TEXT COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(50) COMMENT '创建人',
    update_by VARCHAR(50) COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记（0-正常，1-删除）',
    PRIMARY KEY (id),
    KEY idx_dish_ingredient_dish (dish_id),
    KEY idx_dish_ingredient_ingredient (ingredient_id),
    CONSTRAINT fk_dish_ingredient_dish FOREIGN KEY (dish_id) REFERENCES food(id) ON DELETE CASCADE,
    CONSTRAINT fk_dish_ingredient_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredient(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品-配料关联表';

-- 插入配料分类测试数据
INSERT INTO ingredient_category (category_name, parent_id, sort_order) VALUES
('Vegetables', 0, 1),
('Meat', 0, 2),
('Seasonings', 0, 3),
('Grains and Oils', 0, 4),
('Soy Products', 0, 5);

-- 插入配料测试数据
INSERT INTO ingredient (ingredient_code, ingredient_name, category_id, category_name, specification, unit, cost_price, supplier_name, stock_quantity, min_stock, max_stock) VALUES
('I00001', 'Potato', 1, 'Vegetables', 'Fresh', 'kg', 3.50, 'Vegetable Supplier A', 100.00, 20.00, 200.00),
('I00002', 'Beef', 2, 'Meat', 'Premium', 'kg', 45.00, 'Meat Supplier B', 50.00, 10.00, 100.00),
('I00003', 'Soy Sauce', 3, 'Seasonings', '500ml', 'bottle', 8.50, 'Seasoning Supplier C', 30.00, 5.00, 50.00),
('I00004', 'Rice', 4, 'Grains and Oils', 'Premium', 'kg', 6.80, 'Grain Supplier D', 200.00, 50.00, 300.00),
('I00005', 'Tofu', 5, 'Soy Products', 'Fresh', 'piece', 2.50, 'Soy Product Supplier E', 80.00, 15.00, 150.00);