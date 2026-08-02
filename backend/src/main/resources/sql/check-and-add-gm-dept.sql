-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 检查并修复部门数据
-- 1. 检查现有部门数据
SELECT 'Current departments count:' as info;
SELECT COUNT(*) as dept_count FROM departments WHERE deleted = 0;

-- 2. 显示现有部门数据
SELECT 'Existing departments:' as info;
SELECT id, dept_name, dept_code, parent_id, level, status FROM departments WHERE deleted = 0 ORDER BY level, sort_order LIMIT 10;

-- 3. 添加总经办部门（如果不存在）
INSERT INTO departments (dept_name, dept_code, parent_id, level, status, description, sort_order, deleted)
SELECT '总经办', 'GM', 0, 1, 1, '总经办，负责企业整体管理和决策', 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM departments WHERE dept_code = 'GM' AND deleted = 0
);

-- 4. 更新总经办部门信息（如果已存在）
UPDATE departments 
SET 
    dept_name = '总经办',
    dept_code = 'GM',
    parent_id = 0,
    level = 1,
    status = 1,
    description = '总经办，负责企业整体管理和决策',
    sort_order = 0,
    deleted = 0
WHERE dept_code = 'GM';

-- 5. 验证总经办部门已添加
SELECT 'General Manager Office department:' as info;
SELECT id, dept_name, dept_code, parent_id, level, status FROM departments WHERE dept_code = 'GM' AND deleted = 0;

-- 6. 显示所有顶级部门
SELECT 'All root departments (parent_id = 0):' as info;
SELECT id, dept_name, dept_code, parent_id, level, status, sort_order FROM departments WHERE parent_id = 0 AND deleted = 0 ORDER BY sort_order;
