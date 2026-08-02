-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 餐饮企业部门数据初始化脚本
-- 清空现有部门数据（保留表结构）
DELETE FROM departments;

-- 插入餐饮企业部门数据

-- Level 1: 顶级部门
INSERT INTO departments (id, dept_name, dept_code, parent_id, level, sort_order, status, description, deleted) VALUES
(1, '总经办', 'GM', 0, 1, 0, 1, '总经理办公室，负责企业整体管理和决策', 0);

-- Level 2: 一级部门
INSERT INTO departments (id, dept_name, dept_code, parent_id, level, sort_order, status, description, deleted) VALUES
(2, '厨房部', 'KITCHEN', 1, 2, 1, 1, '负责菜品制作和厨房管理', 0),
(3, '前厅部', 'FRONT_HALL', 1, 2, 2, 1, '负责餐厅前厅服务和客户接待', 0),
(4, '采购部', 'PURCHASE', 1, 2, 3, 1, '负责食材和物资采购', 0),
(5, '仓储部', 'WAREHOUSE', 1, 2, 4, 1, '负责食材和物资仓储管理', 0),
(6, '财务部', 'FINANCE', 1, 2, 5, 1, '负责财务管理和成本控制', 0),
(7, '人事部', 'HR', 1, 2, 6, 1, '负责人力资源管理和员工培训', 0),
(8, '市场部', 'MARKETING', 1, 2, 7, 1, '负责市场推广和品牌建设', 0),
(9, '客户服务部', 'CS', 1, 2, 8, 1, '负责客户关系管理和投诉处理', 0),
(10, '行政部', 'ADMIN', 1, 2, 9, 1, '负责行政管理和后勤保障', 0);

-- Level 3: 厨房部下属部门
INSERT INTO departments (id, dept_name, dept_code, parent_id, level, sort_order, status, description, deleted) VALUES
(11, '中餐厨房', 'KITCHEN_CN', 2, 3, 1, 1, '负责中式菜品制作', 0),
(12, '西餐厨房', 'KITCHEN_WEST', 2, 3, 2, 1, '负责西式菜品制作', 0),
(13, '点心厨房', 'KITCHEN_DESSERT', 2, 3, 3, 1, '负责点心和甜品制作', 0),
(14, '冷菜厨房', 'KITCHEN_COLD', 2, 3, 4, 1, '负责冷菜和凉菜制作', 0),
(15, '洗碗间', 'KITCHEN_DISHWASH', 2, 3, 5, 1, '负责餐具清洗和消毒', 0);

-- Level 3: 前厅部下属部门
INSERT INTO departments (id, dept_name, dept_code, parent_id, level, sort_order, status, description, deleted) VALUES
(16, '服务员团队', 'SERVICE_WAITER', 3, 3, 1, 1, '负责餐厅服务', 0),
(17, '收银团队', 'SERVICE_CASHIER', 3, 3, 2, 1, '负责收银和账单管理', 0),
(18, '迎宾团队', 'SERVICE_HOST', 3, 3, 3, 1, '负责迎宾和座位安排', 0),
(19, '传菜团队', 'SERVICE_RUNNER', 3, 3, 4, 1, '负责菜品传递', 0),
(20, '保洁团队', 'SERVICE_CLEAN', 3, 3, 5, 1, '负责餐厅清洁卫生', 0);

-- Level 3: 采购部下属部门
INSERT INTO departments (id, dept_name, dept_code, parent_id, level, sort_order, status, description, deleted) VALUES
(21, '食材采购组', 'PUR_FOOD', 4, 3, 1, 1, '负责食材采购', 0),
(22, '酒水采购组', 'PUR_DRINK', 4, 3, 2, 1, '负责酒水饮料采购', 0),
(23, '物资采购组', 'PUR_SUPPLY', 4, 3, 3, 1, '负责日常物资采购', 0);

-- Level 3: 仓储部下属部门
INSERT INTO departments (id, dept_name, dept_code, parent_id, level, sort_order, status, description, deleted) VALUES
(24, '冷藏库', 'WH_COLD', 5, 3, 1, 1, '负责冷藏食材存储', 0),
(25, '冷冻库', 'WH_FREEZE', 5, 3, 2, 1, '负责冷冻食材存储', 0),
(26, '干货库', 'WH_DRY', 5, 3, 3, 1, '负责干货调料存储', 0),
(27, '酒水库', 'WH_DRINK', 5, 3, 4, 1, '负责酒水饮料存储', 0);

-- Level 3: 财务部下属部门
INSERT INTO departments (id, dept_name, dept_code, parent_id, level, sort_order, status, description, deleted) VALUES
(28, '成本核算组', 'FIN_COST', 6, 3, 1, 1, '负责成本核算和控制', 0),
(29, '收银管理组', 'FIN_CASH', 6, 3, 2, 1, '负责收银管理', 0),
(30, '税务管理组', 'FIN_TAX', 6, 3, 3, 1, '负责税务申报和管理', 0);

-- Level 3: 人事部下属部门
INSERT INTO departments (id, dept_name, dept_code, parent_id, level, sort_order, status, description, deleted) VALUES
(31, '招聘培训组', 'HR_RECRUIT', 7, 3, 1, 1, '负责员工招聘和培训', 0),
(32, '绩效考核组', 'HR_PERF', 7, 3, 2, 1, '负责绩效考核和管理', 0),
(33, '薪酬福利组', 'HR_COMP', 7, 3, 3, 1, '负责薪酬和福利管理', 0);

-- Level 3: 市场部下属部门
INSERT INTO departments (id, dept_name, dept_code, parent_id, level, sort_order, status, description, deleted) VALUES
(34, '品牌推广组', 'MKT_BRAND', 8, 3, 1, 1, '负责品牌建设和推广', 0),
(35, '活动策划组', 'MKT_EVENT', 8, 3, 2, 1, '负责活动策划和执行', 0),
(36, '新媒体运营组', 'MKT_MEDIA', 8, 3, 3, 1, '负责新媒体运营', 0);

-- Level 3: 客户服务部下属部门
INSERT INTO departments (id, dept_name, dept_code, parent_id, level, sort_order, status, description, deleted) VALUES
(37, '会员管理组', 'CS_MEMBER', 9, 3, 1, 1, '负责会员管理', 0),
(38, '投诉处理组', 'CS_COMPLAINT', 9, 3, 2, 1, '负责投诉处理', 0),
(39, '客户回访组', 'CS_FEEDBACK', 9, 3, 3, 1, '负责客户回访和反馈', 0);

-- Level 3: 行政部下属部门
INSERT INTO departments (id, dept_name, dept_code, parent_id, level, sort_order, status, description, deleted) VALUES
(40, '后勤保障组', 'ADM_LOGISTICS', 10, 3, 1, 1, '负责后勤保障', 0),
(41, '安全管理组', 'ADM_SECURITY', 10, 3, 2, 1, '负责安全管理', 0),
(42, '设备维护组', 'ADM_EQUIP', 10, 3, 3, 1, '负责设备维护', 0);

-- 重置自增ID
ALTER TABLE departments AUTO_INCREMENT = 43;

-- 显示插入结果
SELECT '部门数据初始化完成' AS message;
SELECT COUNT(*) AS total_departments FROM departments WHERE deleted = 0;
