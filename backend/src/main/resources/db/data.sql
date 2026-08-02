-- 食品溯源系统 - 数据初始化脚本
-- 仅保留必要的系统基础数据，不包含业务模拟数据
-- 测试账号统一密码：Admin@123（与 admin 一致，方便测试）

-- ============================================
-- 1. 部门数据（仅保留必要的部门）
-- ============================================
INSERT INTO DEPARTMENTS (DEPT_CODE, DEPT_NAME, PARENT_ID, LEVEL, SORT_ORDER, STATUS, DESCRIPTION) VALUES
('DEPT001', '总经办', 0, 1, 1, 1, '公司最高管理部门，负责战略决策和整体运营'),
('DEPT008', '行政部', 0, 1, 8, 1, '负责公司行政事务、人力资源管理和后勤保障');

-- ============================================
-- 2. 仓库数据（仅保留一个示例仓库）
-- ============================================
INSERT INTO WAREHOUSE (NAME, CODE, TYPE, LOCATION, MANAGER, STATUS) VALUES
('北京主仓库', 'WH-BJ-001', 'main', '北京市朝阳区建国路88号', '系统管理员', 1);

-- ============================================
-- 3. 角色数据（基础角色定义）
-- 包含系统管理员、普通用户及各业务角色
-- ============================================
INSERT INTO ROLES (ROLE_CODE, ROLE_NAME, DESCRIPTION, ROLE_TYPE, LEVEL, STATUS, IS_SYSTEM) VALUES
('admin', '系统管理员', '系统管理员，拥有所有权限', 1, 1, 1, 1),
('user', '普通用户', '普通用户，拥有基础操作权限', 2, 2, 1, 1),
('store_manager', '店长', '门店管理者，负责门店日常运营管理', 2, 2, 1, 0),
('purchaser', '采购员', '采购管理人员，负责采购申请、订单、收货等', 2, 3, 1, 0),
('finance', '财务', '财务管理人员，负责财务记录、对账、结算等', 2, 3, 1, 0),
('chef', '厨师', '厨房工作人员，负责厨房订单处理、菜品制作', 2, 4, 1, 0),
('warehouse', '仓管员', '仓库管理员，负责库存管理、出入库操作', 2, 3, 1, 0);

-- ============================================
-- 4. 用户数据（多角色测试账号）
-- 密码统一为 Admin@123（BCrypt强度12轮哈希）
-- 用途：系统功能测试和权限验证
-- ============================================
INSERT INTO USERS (USERNAME, PASSWORD, EMAIL, PHONE, NAME, AVATAR, STATUS, DEPARTMENT_ID, IS_LOCKED, NEED_CHANGE_PASSWORD) VALUES
-- 系统管理员（ID=1）
('admin', '$2a$12$LX6JRAyUkCTBBGxYZ6uBNuH4vF/PGuet6Es9QS5ilNPMk.vjUDScW', 'admin@foodtrace.com', '13800138000', '系统管理员', 'https://picsum.photos/200/200', 1, 1, 0, 0),
-- 店长（ID=2）
('store_manager', '$2a$12$LX6JRAyUkCTBBGxYZ6uBNuH4vF/PGuet6Es9QS5ilNPMk.vjUDScW', 'store.manager@foodtrace.com', '13800138001', '店长测试账号', 'https://picsum.photos/200/200?random=1', 1, 1, 0, 0),
-- 采购员（ID=3）
('purchaser', '$2a$12$LX6JRAyUkCTBBGxYZ6uBNuH4vF/PGuet6Es9QS5ilNPMk.vjUDScW', 'purchaser@foodtrace.com', '13800138002', '采购员测试账号', 'https://picsum.photos/200/200?random=2', 1, 2, 0, 0),
-- 财务（ID=4）
('finance', '$2a$12$LX6JRAyUkCTBBGxYZ6uBNuH4vF/PGuet6Es9QS5ilNPMk.vjUDScW', 'finance@foodtrace.com', '13800138003', '财务测试账号', 'https://picsum.photos/200/200?random=3', 1, 2, 0, 0),
-- 厨师（ID=5）
('chef', '$2a$12$LX6JRAyUkCTBBGxYZ6uBNuH4vF/PGuet6Es9QS5ilNPMk.vjUDScW', 'chef@foodtrace.com', '13800138004', '厨师测试账号', 'https://picsum.photos/200/200?random=4', 1, 2, 0, 0),
-- 仓管员（ID=6）
('warehouse', '$2a$12$LX6JRAyUkCTBBGxYZ6uBNuH4vF/PGuet6Es9QS5ilNPMk.vjUDScW', 'warehouse@foodtrace.com', '13800138005', '仓管员测试账号', 'https://picsum.photos/200/200?random=5', 1, 2, 0, 0),
-- 普通用户（ID=7）
('user', '$2a$12$LX6JRAyUkCTBBGxYZ6uBNuH4vF/PGuet6Es9QS5ilNPMk.vjUDScW', 'user@foodtrace.com', '13800138006', '普通用户测试账号', 'https://picsum.photos/200/200?random=6', 1, 2, 0, 0);

-- ============================================
-- 5. 用户角色关联数据
-- 每个测试账号关联对应角色，用于权限验证
-- ============================================
INSERT INTO USER_ROLES (USER_ID, ROLE_ID, CREATED_BY) VALUES
(1, 1, 1),  -- admin → admin角色
(2, 3, 1),  -- store_manager → store_manager角色
(3, 4, 1),  -- purchaser → purchaser角色
(4, 5, 1),  -- finance → finance角色
(5, 6, 1),  -- chef → chef角色
(6, 7, 1),  -- warehouse → warehouse角色
(7, 2, 1);  -- user → user角色
