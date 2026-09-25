-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 创建员工表
CREATE TABLE `employees` (
  `employee_id` varchar(36) NOT NULL COMMENT '员工ID',
  `employee_code` varchar(50) NOT NULL COMMENT '员工编码（唯一）',
  `employee_name` varchar(100) NOT NULL COMMENT '员工姓名',
  `gender` varchar(10) DEFAULT NULL COMMENT '员工性别（male: 男, female: 女）',
  `phone` varchar(20) DEFAULT NULL COMMENT '员工手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '员工邮箱',
  `department_id` varchar(36) DEFAULT NULL COMMENT '所属部门ID',
  `position_id` varchar(36) DEFAULT NULL COMMENT '所属职位ID',
  `store_id` varchar(36) DEFAULT NULL COMMENT '所属门店ID',
  `hire_date` datetime DEFAULT NULL COMMENT '入职日期',
  `resign_date` datetime DEFAULT NULL COMMENT '离职日期',
  `status` varchar(20) DEFAULT 'active' COMMENT '员工状态（active: 在职, inactive: 离职, probation: 试用期）',
  `salary_level` varchar(10) DEFAULT NULL COMMENT '薪资级别',
  `base_salary` decimal(10,2) DEFAULT NULL COMMENT '基本薪资',
  `address` varchar(255) DEFAULT NULL COMMENT '员工地址',
  `id_card` varchar(50) DEFAULT NULL COMMENT '身份证号',
  `bank_card` varchar(50) DEFAULT NULL COMMENT '银行卡号',
  `emergency_contact` varchar(100) DEFAULT NULL COMMENT '紧急联系人',
  `emergency_phone` varchar(20) DEFAULT NULL COMMENT '紧急联系电话',
  `photo_url` varchar(255) DEFAULT NULL COMMENT '员工照片URL',
  `remark` varchar(500) DEFAULT NULL COMMENT '员工备注',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` varchar(36) DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(36) DEFAULT NULL COMMENT '更新人',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除标记（0: 未删除, 1: 已删除）',
  PRIMARY KEY (`employee_id`),
  UNIQUE KEY `uk_employee_code` (`employee_code`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_position_id` (`position_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';
