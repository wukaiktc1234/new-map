-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 职级全局化改造 - 数据库表结构修复脚本
-- 日期: 2026-01-27
-- 目的: 修复position_levels表缺失的字段

USE food_traceability;

-- 1. 检查并添加created_time字段
ALTER TABLE position_levels
ADD COLUMN IF NOT EXISTS created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

-- 2. 检查并添加updated_time字段
ALTER TABLE position_levels
ADD COLUMN IF NOT EXISTS updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- 3. 检查并添加coefficient字段
ALTER TABLE position_levels
ADD COLUMN IF NOT EXISTS coefficient DECIMAL(10, 4) COMMENT '薪资系数' AFTER salary_max;

-- 4. 检查并添加position_level_id字段到employees表
ALTER TABLE employees
ADD COLUMN IF NOT EXISTS position_level_id BIGINT COMMENT '职级ID' AFTER position_id;

-- 5. 检查并添加position_level_id字段到positions表
ALTER TABLE positions
ADD COLUMN IF NOT EXISTS position_level_id BIGINT COMMENT '职级ID' AFTER level;

-- 6. 检查并添加base_salary字段到departments表
ALTER TABLE departments
ADD COLUMN IF NOT EXISTS base_salary DECIMAL(10, 2) COMMENT '部门基础薪资';

-- 7. 添加索引
ALTER TABLE employees
ADD INDEX IF NOT EXISTS idx_position_level_id (position_level_id);

ALTER TABLE positions
ADD INDEX IF NOT EXISTS idx_position_level_id (position_level_id);

-- 8. 添加外键约束
ALTER TABLE employees
ADD CONSTRAINT IF NOT EXISTS fk_employee_position_level
FOREIGN KEY (position_level_id) REFERENCES position_levels(id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE positions
ADD CONSTRAINT IF NOT EXISTS fk_position_position_level
FOREIGN KEY (position_level_id) REFERENCES position_levels(id) ON DELETE SET NULL ON UPDATE CASCADE;

-- 验证表结构
DESCRIBE position_levels;
DESCRIBE employees;
DESCRIBE positions;
DESCRIBE departments;

SELECT '数据库表结构修复完成！' AS message;