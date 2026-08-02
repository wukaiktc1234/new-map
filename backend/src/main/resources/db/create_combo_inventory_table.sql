-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 创建套餐库存产品关联表
CREATE TABLE IF NOT EXISTS `combo_inventory` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `combo_id` BIGINT NOT NULL COMMENT '套餐ID',
  `inventory_id` BIGINT NOT NULL COMMENT '库存产品ID',
  `inventory_name` VARCHAR(100) NOT NULL COMMENT '库存产品名称',
  `quantity` DECIMAL(10, 2) NOT NULL COMMENT '使用数量',
  `unit` VARCHAR(20) NOT NULL COMMENT '计量单位',
  `remark` TEXT COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) COMMENT '创建人',
  `update_by` VARCHAR(50) COMMENT '更新人',
  `deleted` INT DEFAULT 0 COMMENT '删除标记（0：未删除，1：已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_combo_id` (`combo_id`),
  KEY `idx_inventory_id` (`inventory_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐库存产品关联表';
