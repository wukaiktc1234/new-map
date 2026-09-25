-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 更新现有职位编码，使其符合新规则
-- 格式：部门编码-职位类型缩写-序号

-- 更新人事专员编码（从 HR001 改为 HR-SPE-001）
UPDATE positions 
SET position_code = CONCAT(
    (SELECT dept_code FROM departments WHERE departments.id = positions.department_id),
    '-SPE-',
    SUBSTRING(position_code, -3)
)
WHERE position_name = '人事专员' AND position_code LIKE 'HR%';

-- 更新财务专员编码（从 FIN001 改为 FIN-SPE-001）
UPDATE positions 
SET position_code = CONCAT(
    (SELECT dept_code FROM departments WHERE departments.id = positions.department_id),
    '-SPE-',
    SUBSTRING(position_code, -3)
)
WHERE position_name = '财务专员' AND position_code LIKE 'FIN%';

-- 更新出纳编码（从 FIN002 改为 FIN-CAS-001）
UPDATE positions 
SET position_code = CONCAT(
    (SELECT dept_code FROM departments WHERE departments.id = positions.department_id),
    '-CAS-',
    SUBSTRING(position_code, -3)
)
WHERE position_name = '出纳' AND position_code LIKE 'FIN%';

-- 更新采购专员编码（从 PUR001 改为 PUR-SPE-001）
UPDATE positions 
SET position_code = CONCAT(
    (SELECT dept_code FROM departments WHERE departments.id = positions.department_id),
    '-SPE-',
    SUBSTRING(position_code, -3)
)
WHERE position_name = '采购专员' AND position_code LIKE 'PUR%';

-- 更新总经理编码（从 GM001 改为 GM-GM-001）
UPDATE positions 
SET position_code = CONCAT(
    (SELECT dept_code FROM departments WHERE departments.id = positions.department_id),
    '-GM-',
    SUBSTRING(position_code, -3)
)
WHERE position_name = '总经理' AND position_code LIKE 'GM%';
