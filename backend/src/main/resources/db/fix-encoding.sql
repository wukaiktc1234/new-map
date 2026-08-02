-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 修复中文乱码数据脚本
-- 问题原因：数据在utf8字符集下插入，但被当作latin1读取后存储，导致乱码
-- 解决方案：将乱码数据转换回正确的UTF-8编码

USE food_traceability;

-- 1. 修复部门表(departments)的乱码数据
UPDATE departments
SET DEPT_NAME = CONVERT(CAST(CONVERT(DEPT_NAME USING latin1) AS BINARY) USING utf8mb4)
WHERE DELETED = 0;

-- 2. 修复员工表(employees)的乱码数据
UPDATE employees
SET employee_name = CONVERT(CAST(CONVERT(employee_name USING latin1) AS BINARY) USING utf8mb4)
WHERE employee_name IS NOT NULL AND employee_name != '';

-- 3. 验证修复结果
SELECT '部门表修复结果:' AS '===';
SELECT ID, DEPT_CODE, DEPT_NAME FROM departments WHERE DELETED = 0 LIMIT 10;

SELECT '员工表修复结果:' AS '===';
SELECT employee_id, employee_code, employee_name, gender FROM employees LIMIT 10;

SELECT '职位表数据(正常):' AS '===';
SELECT id, position_code, position_name FROM positions WHERE deleted = 0 LIMIT 10;
