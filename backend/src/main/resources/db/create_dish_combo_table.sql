-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 检查并创建dish_combo表（PostgreSQL兼容版本）
-- 注意：此为历史遗留脚本，权威定义见 V1.0.0__init_postgresql.sql 中的 dish_combo 表
-- 和 V6.0.0__create_order_product_tables.sql 中的 dish_combos 表

-- 创建套餐表（如不存在）
CREATE TABLE IF NOT EXISTS dish_combo (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    combo_code          VARCHAR(50),
    name                VARCHAR(100) NOT NULL,
    price               DECIMAL(10, 2) NOT NULL,
    dishes              TEXT,
    description         VARCHAR(500),
    status              VARCHAR(20) DEFAULT 'active',
    image_url           VARCHAR(255),
    priority            INTEGER DEFAULT 0,
    people_count        INTEGER DEFAULT 1,
    combo_type          VARCHAR(20) DEFAULT 'regular',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER DEFAULT 0,
    version             INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_dish_combo_status ON dish_combo(status);
CREATE INDEX IF NOT EXISTS idx_dish_combo_combo_type ON dish_combo(combo_type);
CREATE INDEX IF NOT EXISTS idx_dish_combo_priority ON dish_combo(priority);

COMMENT ON TABLE dish_combo IS '套餐表';
COMMENT ON COLUMN dish_combo.id IS '套餐ID';
COMMENT ON COLUMN dish_combo.name IS '套餐名称';
COMMENT ON COLUMN dish_combo.price IS '套餐价格';
COMMENT ON COLUMN dish_combo.dishes IS '包含菜品，JSON格式存储菜品ID列表';
COMMENT ON COLUMN dish_combo.status IS '套餐状态：active-启用，inactive-停用';

-- 插入示例数据（如有冲突则跳过）
INSERT INTO dish_combo (name, price, dishes, description, status, image_url, priority, people_count, combo_type)
VALUES
    ('单人套餐A', 38.00, '[{"dishId": 1, "dishName": "宫保鸡丁", "quantity": 1}, {"dishId": 2, "dishName": "米饭", "quantity": 1}]', '包含宫保鸡丁和米饭', 'active', NULL, 1, 1, 'regular'),
    ('双人套餐B', 68.00, '[{"dishId": 1, "dishName": "宫保鸡丁", "quantity": 1}, {"dishId": 3, "dishName": "红烧肉", "quantity": 1}, {"dishId": 2, "dishName": "米饭", "quantity": 2}]', '包含宫保鸡丁、红烧肉和两份米饭', 'active', NULL, 2, 2, 'regular'),
    ('家庭套餐C', 128.00, '[{"dishId": 1, "dishName": "宫保鸡丁", "quantity": 1}, {"dishId": 3, "dishName": "红烧肉", "quantity": 1}, {"dishId": 4, "dishName": "清蒸鱼", "quantity": 1}, {"dishId": 5, "dishName": "麻婆豆腐", "quantity": 1}, {"dishId": 2, "dishName": "米饭", "quantity": 4}]', '适合3-4人享用的家庭套餐', 'active', NULL, 3, 4, 'family')
ON CONFLICT DO NOTHING;
