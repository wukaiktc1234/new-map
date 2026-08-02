-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 为food表添加菜品编码字段（PostgreSQL兼容版本）
-- 注意：PG不支持AFTER子句，新列默认添加到表末尾

-- 为food表添加菜品编码字段
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'food' AND column_name = 'food_code'
    ) THEN
        ALTER TABLE food ADD COLUMN food_code VARCHAR(10);
    END IF;
END $$;

-- 创建索引（如果不存在）
CREATE INDEX IF NOT EXISTS idx_food_code ON food(food_code);

-- 为现有数据生成菜品编码（仅对NULL值更新）
UPDATE food SET food_code = 'F' || LPAD(CAST(row_number() OVER (ORDER BY food_code)::TEXT), 5, '0')
WHERE food_code IS NULL;

-- 如果上面没有生成成功（空表等情况），使用序列方式
-- 注意：PG不使用用户变量和AUTO_INCREMENT，改用序列或ROW_NUMBER()
