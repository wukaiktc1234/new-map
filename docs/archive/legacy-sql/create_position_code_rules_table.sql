-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 职位编码规则表
CREATE TABLE IF NOT EXISTS `position_code_rules` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '规则ID',
  `keyword` VARCHAR(100) NOT NULL COMMENT '职位名称关键词',
  `code` VARCHAR(20) NOT NULL COMMENT '职位编码',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `created_by` VARCHAR(50) DEFAULT 'system' COMMENT '创建人',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
  INDEX `idx_keyword` (`keyword`),
  INDEX `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位编码规则表';
