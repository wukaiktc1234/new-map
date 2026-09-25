-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 添加成本价字段到食品表（PostgreSQL兼容版本）

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'food' AND column_name = 'cost_price'
    ) THEN
        ALTER TABLE food ADD COLUMN cost_price DECIMAL(10,2) DEFAULT 0.00;
        COMMENT ON COLUMN food.cost_price IS '成本价（元）';
    END IF;
END $$;

-- 更新现有数据，设置默认成本价为售价的70%
UPDATE food SET cost_price = food_price * 0.7
WHERE (cost_price IS NULL OR cost_price = 0) AND food_price IS NOT NULL AND food_price > 0;
