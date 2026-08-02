-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 创建财务收支记录表
CREATE TABLE IF NOT EXISTS `finance_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type` VARCHAR(20) NOT NULL COMMENT '收支类型：INCOME(收入)、EXPENSE(支出)',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '金额',
  `category` VARCHAR(50) NOT NULL COMMENT '类别',
  `business_id` BIGINT DEFAULT NULL COMMENT '关联业务ID',
  `business_type` VARCHAR(20) DEFAULT NULL COMMENT '业务类型：ORDER(订单)、PURCHASE(采购)、INVENTORY(库存)、SALARY(薪资)',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `record_date` DATETIME NOT NULL COMMENT '记录日期',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE(有效)、INACTIVE(无效)',
  PRIMARY KEY (`id`),
  INDEX `idx_type` (`type`),
  INDEX `idx_category` (`category`),
  INDEX `idx_record_date` (`record_date`),
  INDEX `idx_business_type` (`business_type`),
  INDEX `idx_business_id` (`business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务收支记录表';

-- 创建财务统计数据表
CREATE TABLE IF NOT EXISTS `finance_statistics` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `statistics_date` DATE NOT NULL COMMENT '统计日期',
  `income_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '当日收入金额',
  `expense_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '当日支出金额',
  `profit_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '当日利润金额',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_statistics_date` (`statistics_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财务统计数据表';

-- 创建税务记录表
CREATE TABLE IF NOT EXISTS `tax_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tax_type` VARCHAR(50) NOT NULL COMMENT '税种类型：VAT(增值税)、ENTERPRISE_INCOME_TAX(企业所得税)、PERSONAL_INCOME_TAX(个人所得税)、OTHER(其他)',
  `tax_period` VARCHAR(20) NOT NULL COMMENT '纳税期间：如202511(2025年11月)',
  `taxable_amount` DECIMAL(10,2) NOT NULL COMMENT '应纳税所得额',
  `tax_rate` DECIMAL(5,2) NOT NULL COMMENT '税率',
  `tax_amount` DECIMAL(10,2) NOT NULL COMMENT '应纳税额',
  `paid_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '已纳税额',
  `tax_status` VARCHAR(20) DEFAULT 'UNPAID' COMMENT '纳税状态：UNPAID(未缴纳)、PAID(已缴纳)、OVERDUE(逾期)',
  `payment_date` DATETIME DEFAULT NULL COMMENT '缴纳日期',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`),
  INDEX `idx_tax_type` (`tax_type`),
  INDEX `idx_tax_period` (`tax_period`),
  INDEX `idx_tax_status` (`tax_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='税务记录表';