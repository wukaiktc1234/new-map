-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 健康证表
CREATE TABLE IF NOT EXISTS `health_certificate` (
  `id` varchar(36) NOT NULL COMMENT '健康证ID',
  `employee_id` varchar(36) NOT NULL COMMENT '员工ID',
  `employee_name` varchar(100) NOT NULL COMMENT '员工姓名',
  `store` varchar(100) NOT NULL COMMENT '所属门店',
  `certificate_number` varchar(50) NOT NULL COMMENT '健康证号',
  `issue_date` date NOT NULL COMMENT '签发日期',
  `expiry_date` date NOT NULL COMMENT '到期日期',
  `status` varchar(20) NOT NULL COMMENT '状态（valid: 有效, expiring: 即将过期, expired: 已过期）',
  `issuer` varchar(100) NOT NULL COMMENT '签发机构',
  `note` varchar(500) DEFAULT NULL COMMENT '备注',
  `expense_status` varchar(20) DEFAULT NULL COMMENT '报销状态（pending: 待审批, approved: 已审批, reimbursed: 已报销, rejected: 已拒绝）',
  `approval_status` varchar(20) DEFAULT 'pending' COMMENT '审核状态（pending: 待审核, approved: 已通过, rejected: 已拒绝, draft: 草稿）',
  `approval_by` varchar(100) DEFAULT NULL COMMENT '审核人',
  `approval_date` date DEFAULT NULL COMMENT '审核日期',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因',
  `submission_date` date DEFAULT NULL COMMENT '提交日期',
  `submitted_by` varchar(100) DEFAULT NULL COMMENT '提交人',
  `certificate_image` varchar(255) DEFAULT NULL COMMENT '健康证图片路径',
  `last_expense_id` varchar(36) DEFAULT NULL COMMENT '最后一次报销ID',
  `operator` varchar(100) DEFAULT NULL COMMENT '操作人',
  `operation_time` date DEFAULT NULL COMMENT '操作时间',
  `expiry_days` int DEFAULT NULL COMMENT '剩余天数',
  PRIMARY KEY (`id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_store` (`store`),
  KEY `idx_status` (`status`),
  KEY `idx_expiry_date` (`expiry_date`),
  KEY `idx_approval_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='健康证表';

-- 健康证报销表
CREATE TABLE IF NOT EXISTS `health_certificate_expense` (
  `id` varchar(36) NOT NULL COMMENT '报销ID',
  `health_certificate_id` varchar(36) NOT NULL COMMENT '健康证ID',
  `employee_id` varchar(36) NOT NULL COMMENT '员工ID',
  `employee_name` varchar(100) NOT NULL COMMENT '员工姓名',
  `store` varchar(100) NOT NULL COMMENT '所属门店',
  `amount` decimal(10,2) NOT NULL COMMENT '报销金额',
  `invoice_type` varchar(50) DEFAULT NULL COMMENT '发票类型',
  `invoice_number` varchar(50) DEFAULT NULL COMMENT '发票号码',
  `invoice_date` date DEFAULT NULL COMMENT '发票日期',
  `invoice_attachments` text COMMENT '发票附件路径列表',
  `reason` varchar(500) NOT NULL COMMENT '报销事由',
  `note` varchar(500) DEFAULT NULL COMMENT '备注',
  `status` varchar(20) NOT NULL COMMENT '报销状态（pending: 待审批, approved: 已审批, reimbursed: 已报销, rejected: 已拒绝）',
  `apply_date` date NOT NULL COMMENT '申请日期',
  `approve_date` date DEFAULT NULL COMMENT '审批日期',
  `reimburse_date` date DEFAULT NULL COMMENT '报销日期',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因',
  `expense_type` varchar(20) NOT NULL COMMENT '报销类型（new: 新办, renewal: 续期）',
  `approval_history` text COMMENT '审批历史（JSON格式）',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建人',
  `create_time` date DEFAULT NULL COMMENT '创建时间',
  `updater` varchar(100) DEFAULT NULL COMMENT '最后更新人',
  `update_time` date DEFAULT NULL COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_health_certificate_id` (`health_certificate_id`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_store` (`store`),
  KEY `idx_status` (`status`),
  KEY `idx_apply_date` (`apply_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='健康证报销表';
