-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 为各部门添加职位数据

-- 厨房部职位 (DEPARTMENT_ID = 2)
INSERT INTO POSITIONS (POSITION_CODE, POSITION_NAME, DEPARTMENT_ID, LEVEL, STATUS, DESCRIPTION, CREATED_BY) VALUES
('KITCHEN001', '主厨', 2, '1', 1, '负责厨房整体管理和菜品研发', 'system'),
('KITCHEN002', '副主厨', 2, '2', 1, '协助主厨管理厨房事务', 'system'),
('KITCHEN003', '厨师', 2, '3', 1, '负责菜品制作', 'system'),
('KITCHEN004', '帮厨', 2, '4', 1, '协助厨师进行食材处理和准备工作', 'system'),
('KITCHEN005', '洗碗工', 2, '5', 1, '负责餐具和厨房用具的清洗消毒', 'system');

-- 前厅部职位 (DEPARTMENT_ID = 3)
INSERT INTO POSITIONS (POSITION_CODE, POSITION_NAME, DEPARTMENT_ID, LEVEL, STATUS, DESCRIPTION, CREATED_BY) VALUES
('FRONT001', '前厅经理', 3, '1', 1, '负责前厅整体运营管理', 'system'),
('FRONT002', '领班', 3, '2', 1, '负责服务员团队管理和日常运营', 'system'),
('FRONT003', '服务员', 3, '3', 1, '负责餐厅服务和客户接待', 'system'),
('FRONT004', '迎宾员', 3, '4', 1, '负责客户迎送和座位安排', 'system'),
('FRONT005', '收银员', 3, '5', 1, '负责收银和账单处理', 'system');

-- 采购部职位 (DEPARTMENT_ID = 4)
INSERT INTO POSITIONS (POSITION_CODE, POSITION_NAME, DEPARTMENT_ID, LEVEL, STATUS, DESCRIPTION, CREATED_BY) VALUES
('PURCHASE001', '采购经理', 4, '1', 1, '负责采购部门整体管理和供应商管理', 'system'),
('PURCHASE002', '采购员', 4, '2', 1, '负责食材和物资的采购', 'system'),
('PURCHASE003', '采购助理', 4, '3', 1, '协助采购员处理采购事务', 'system');

-- 仓储部职位 (DEPARTMENT_ID = 5)
INSERT INTO POSITIONS (POSITION_CODE, POSITION_NAME, DEPARTMENT_ID, LEVEL, STATUS, DESCRIPTION, CREATED_BY) VALUES
('WAREHOUSE001', '仓储经理', 5, '1', 1, '负责仓库整体管理和库存控制', 'system'),
('WAREHOUSE002', '仓管员', 5, '2', 1, '负责物资入库、出库和库存盘点', 'system'),
('WAREHOUSE003', '搬运工', 5, '3', 1, '负责物资搬运和装卸', 'system');

-- 财务部职位 (DEPARTMENT_ID = 6)
INSERT INTO POSITIONS (POSITION_CODE, POSITION_NAME, DEPARTMENT_ID, LEVEL, STATUS, DESCRIPTION, CREATED_BY) VALUES
('FINANCE001', '财务经理', 6, '1', 1, '负责财务部门整体管理和财务决策', 'system'),
('FINANCE002', '会计', 6, '2', 1, '负责会计核算和账务处理', 'system'),
('FINANCE003', '出纳', 6, '3', 1, '负责现金管理和日常收支', 'system'),
('FINANCE004', '财务专员', 6, '4', 1, '协助处理财务事务', 'system');

-- 人事部职位 (DEPARTMENT_ID = 7)
INSERT INTO POSITIONS (POSITION_CODE, POSITION_NAME, DEPARTMENT_ID, LEVEL, STATUS, DESCRIPTION, CREATED_BY) VALUES
('HR001', '人事经理', 7, '1', 1, '负责人事部门整体管理和人力资源规划', 'system'),
('HR002', '人事专员', 7, '2', 1, '负责招聘、培训、考勤等人事事务', 'system'),
('HR003', '薪酬专员', 7, '3', 1, '负责薪酬核算和福利管理', 'system'),
('HR004', '培训专员', 7, '4', 1, '负责员工培训和发展', 'system');

-- 市场部职位 (DEPARTMENT_ID = 8)
INSERT INTO POSITIONS (POSITION_CODE, POSITION_NAME, DEPARTMENT_ID, LEVEL, STATUS, DESCRIPTION, CREATED_BY) VALUES
('MARKET001', '市场经理', 8, '1', 1, '负责市场部门整体管理和市场策略制定', 'system'),
('MARKET002', '市场专员', 8, '2', 1, '负责市场推广和品牌建设', 'system'),
('MARKET003', '策划专员', 8, '3', 1, '负责活动策划和执行', 'system'),
('MARKET004', '新媒体运营', 8, '4', 1, '负责新媒体平台运营', 'system');

-- 客户服务部职位 (DEPARTMENT_ID = 9)
INSERT INTO POSITIONS (POSITION_CODE, POSITION_NAME, DEPARTMENT_ID, LEVEL, STATUS, DESCRIPTION, CREATED_BY) VALUES
('CS001', '客服经理', 9, '1', 1, '负责客服部门整体管理和客户关系维护', 'system'),
('CS002', '客服专员', 9, '2', 1, '负责客户咨询和投诉处理', 'system'),
('CS003', '售后专员', 9, '3', 1, '负责售后服务和问题解决', 'system');

-- 行政部职位 (DEPARTMENT_ID = 10)
INSERT INTO POSITIONS (POSITION_CODE, POSITION_NAME, DEPARTMENT_ID, LEVEL, STATUS, DESCRIPTION, CREATED_BY) VALUES
('ADMIN001', '行政经理', 10, '1', 1, '负责行政部门整体管理和后勤保障', 'system'),
('ADMIN002', '行政专员', 10, '2', 1, '负责行政事务和办公管理', 'system'),
('ADMIN003', '后勤专员', 10, '3', 1, '负责后勤保障和物资管理', 'system'),
('ADMIN004', '前台文员', 10, '4', 1, '负责前台接待和电话接听', 'system');
