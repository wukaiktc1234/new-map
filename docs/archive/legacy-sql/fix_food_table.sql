-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 修复food表结构（PostgreSQL兼容版本）
-- 删除food_id字段，因为Food实体类使用food_code作为主键

-- 1. 删除food_id字段（如果存在）
-- 注意：PG不支持 DROP COLUMN IF EXISTS 在所有旧版本中，使用DO块安全处理
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'food' AND column_name = 'food_id'
    ) THEN
        ALTER TABLE food DROP COLUMN food_id;
    END IF;
END $$;

-- 2. 修改列定义确保类型正确（PG使用ALTER COLUMN TYPE）
DO $$
BEGIN
    ALTER TABLE food ALTER COLUMN food_code SET NOT NULL;
EXCEPTION WHEN OTHERS THEN
    -- 如果约束冲突则忽略（可能已有数据或主键已存在）
    NULL;
END $$;

-- 3. 确保其他字段类型正确
ALTER TABLE food ALTER COLUMN food_name TYPE VARCHAR(100) USING food_name::VARCHAR(100);
ALTER TABLE food ALTER COLUMN food_category TYPE VARCHAR(50) USING COALESCE(food_category::VARCHAR(50), '');
ALTER TABLE food ALTER COLUMN food_price TYPE DECIMAL(12,2) USING COALESCE(food_price::DECIMAL(12,2), 0.00);
ALTER TABLE food ALTER COLUMN cost_price TYPE DECIMAL(12,2) USING COALESCE(cost_price::DECIMAL(12,2), 0.00);
ALTER TABLE food ALTER COLUMN food_desc TYPE VARCHAR(500) USING COALESCE(food_desc::VARCHAR(500), '');
ALTER TABLE food ALTER COLUMN food_image TYPE VARCHAR(500) USING COALESCE(food_image::VARCHAR(500), '');
ALTER TABLE food ALTER COLUMN food_status TYPE VARCHAR(20) USING COALESCE(food_status::VARCHAR(20), 'active');
ALTER TABLE food ALTER COLUMN batch_number TYPE VARCHAR(100) USING COALESCE(batch_number::VARCHAR(100), '');
ALTER TABLE food ALTER COLUMN trace_code TYPE VARCHAR(100) USING COALESCE(trace_code::VARCHAR(100), '');
ALTER TABLE food ALTER COLUMN manufacturer TYPE VARCHAR(200) USING COALESCE(manufacturer::VARCHAR(200), '');

-- 4. 确保deleted字段为INTEGER类型
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'food' AND column_name = 'deleted' AND data_type IN ('smallint', 'tinyint')
    ) THEN
        ALTER TABLE food ALTER COLUMN deleted TYPE INTEGER USING deleted::INTEGER;
    END IF;
END $$;

-- 5. 插入测试数据（如有冲突则跳过）
INSERT INTO food (food_code, food_name, food_category, food_price, cost_price, food_desc, food_status, created_at, updated_at, deleted)
VALUES
    ('F001', '宫保鸡丁', '热菜', 28.00, 15.00, '经典川菜，鸡肉配花生', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('F002', '红烧肉', '热菜', 35.00, 20.00, '软糯可口，肥而不腻', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('F003', '麻婆豆腐', '热菜', 18.00, 8.00, '麻辣鲜香，豆腐嫩滑', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('F004', '鱼香肉丝', '热菜', 25.00, 12.00, '酸甜可口，肉丝嫩滑', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('F005', '酸辣汤', '汤品', 12.00, 5.00, '开胃暖身，酸辣适中', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('F006', '米饭', '主食', 2.00, 0.50, '香软可口，东北大米', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('F007', '可乐', '饮品', 5.00, 2.00, '冰爽可口', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    ('F008', '蛋炒饭', '主食', 15.00, 7.00, '香气四溢，粒粒分明', 'active', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
ON CONFLICT (food_code) DO NOTHING;
