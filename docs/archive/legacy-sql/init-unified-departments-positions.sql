-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 部门和职位数据统一初始化脚本
-- 适用于食品溯源系统
-- 部门：总经办、人事处、财务室、采购办
-- 职位：总经理、人事专员、财务专员、出纳、采购专员

-- 清空现有部门和职位数据（谨慎使用）
-- TRUNCATE TABLE positions;
-- TRUNCATE TABLE departments;

-- 插入部门数据
INSERT INTO departments (dept_name, dept_code, parent_id, level, status, description, sort_order, deleted) VALUES
-- 顶级部门
('总经办', 'GM', 0, 1, 1, '总经理办公室，负责企业整体管理和决策', 1, 0),
('人事处', 'HR', 0, 1, 1, '人事处，负责人力资源管理和员工培训', 2, 0),
('财务室', 'FIN', 0, 1, 1, '财务室，负责财务管理和成本控制', 3, 0),
('采购办', 'PUR', 0, 1, 1, '采购办，负责原材料采购和供应商管理', 4, 0);

-- 插入职位数据
INSERT INTO positions (position_code, position_name, department_id, level, status, description, employee_count, created_by, deleted) VALUES
-- 总经办职位
('GM-GM-001', '总经理', 1, '1', 1, '负责公司整体管理和战略决策', 0, 'system', 0),

-- 人事处职位
('HR-SPE-001', '人事专员', 2, '2', 1, '负责人力资源管理和员工培训', 0, 'system', 0),

-- 财务室职位
('FIN-SPE-001', '财务专员', 3, '2', 1, '负责财务核算和成本控制', 0, 'system', 0),
('FIN-CAS-001', '出纳', 3, '3', 1, '负责现金管理和日常收支', 0, 'system', 0),

-- 采购办职位
('PUR-SPE-001', '采购专员', 4, '2', 1, '负责原材料采购和供应商管理', 0, 'system', 0);
