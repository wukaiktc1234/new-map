-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 食品溯源系统 - 将数字ID迁移到极简编码
-- 迁移方案：将现有的长数字ID替换为D00001格式的简单编码

-- 步骤1: 创建临时映射表，存储旧ID到新编码的映射
CREATE TABLE IF NOT EXISTS food_id_mapping (
    old_food_id VARCHAR(50) PRIMARY KEY,
    new_food_id VARCHAR(10) NOT NULL,
    food_name VARCHAR(100),
    migrated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 步骤2: 为现有数据生成新的极简编码
-- 假设从D83044开始（基于用户提到的编码）
INSERT INTO food_id_mapping (old_food_id, new_food_id, food_name)
SELECT 
    food_id,
    CONCAT('D', LPAD(83044 + ROW_NUMBER() OVER (ORDER BY create_time), 5, '0')),
    food_name
FROM food 
WHERE food_id NOT IN (SELECT old_food_id FROM food_id_mapping);

-- 步骤3: 更新food_trace表中的外键引用
UPDATE food_trace ft
JOIN food_id_mapping fim ON ft.food_id = fim.old_food_id
SET ft.food_id = fim.new_food_id;

-- 步骤4: 更新order_items表中的外键引用  
UPDATE order_items oi
JOIN food_id_mapping fim ON oi.food_id = fim.old_food_id
SET oi.food_id = fim.new_food_id;

-- 步骤5: 更新food表的主键（这是最复杂的步骤，需要按顺序执行）

-- 5.1 首先处理被引用的记录，暂时设置为NULL
UPDATE food_trace SET food_id = NULL WHERE food_id LIKE 'D%';
UPDATE order_items SET food_id = NULL WHERE food_id LIKE 'D%';

-- 5.2 执行主键更新
-- 注意：这需要禁用外键检查，执行更新，然后重新启用

-- 5.3 更新food表的主键
UPDATE food f
JOIN food_id_mapping fim ON f.food_id = fim.old_food_id
SET f.food_id = fim.new_food_id;

-- 5.4 重新建立外键关系
UPDATE food_trace ft
JOIN food_id_mapping fim ON ft.trace_id LIKE CONCAT(fim.old_food_id, '%')
SET ft.food_id = fim.new_food_id;

UPDATE order_items oi
JOIN food_id_mapping fim ON oi.order_item_id LIKE CONCAT(fim.old_food_id, '%')
SET oi.food_id = fim.new_food_id;

-- 步骤6: 验证迁移结果
SELECT 
    fim.old_food_id as "原ID",
    fim.new_food_id as "新编码",
    fim.food_name as "食品名称",
    COUNT(ft.trace_id) as "溯源记录数",
    COUNT(oi.order_item_id) as "订单记录数"
FROM food_id_mapping fim
LEFT JOIN food_trace ft ON fim.new_food_id = ft.food_id
LEFT JOIN order_items oi ON fim.new_food_id = oi.food_id
GROUP BY fim.old_food_id, fim.new_food_id, fim.food_name
ORDER BY fim.new_food_id;

-- 步骤7: 清理临时表（在确认迁移成功后执行）
-- DROP TABLE food_id_mapping;