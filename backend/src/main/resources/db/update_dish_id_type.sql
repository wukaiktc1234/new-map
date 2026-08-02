-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 更新菜品配料关联表的dish_id字段类型为VARCHAR
USE food_traceability;

-- 删除dish_ingredient表（如果存在）
DROP TABLE IF EXISTS `dish_ingredient`;

-- 重新创建dish_ingredient表，dish_id改为VARCHAR类型
CREATE TABLE IF NOT EXISTS `dish_ingredient` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `dish_id` VARCHAR(50) NOT NULL COMMENT '菜品ID',
  `dish_name` VARCHAR(100) DEFAULT NULL COMMENT '菜品名称',
  `ingredient_id` VARCHAR(50) NOT NULL COMMENT '配料ID',
  `ingredient_name` VARCHAR(100) DEFAULT NULL COMMENT '配料名称',
  `ingredient_code` VARCHAR(50) DEFAULT NULL COMMENT '配料编码',
  `quantity` DECIMAL(10, 2) NOT NULL COMMENT '用量',
  `unit` VARCHAR(20) NOT NULL COMMENT '单位',
  `cost_price` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '成本价',
  `total_cost` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '总成本',
  `remark` TEXT COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `update_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_dish_ingredient` (`dish_id`, `ingredient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品配料关联表';
