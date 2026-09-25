-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 设备模板表
-- 用于存储每个设备的打印格式和版式设计模板

CREATE TABLE IF NOT EXISTS `device_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` BIGINT NOT NULL COMMENT '关联设备ID（hardware_config表的主键）',
  `template_type` VARCHAR(50) NOT NULL COMMENT '模板类型：PRINT_FORMAT（打印格式）、LAYOUT_DESIGN（版式设计）',
  `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `template_data` TEXT NOT NULL COMMENT '模板数据（JSON格式）',
  `is_default` TINYINT(1) DEFAULT 0 COMMENT '是否为默认模板：0-否，1-是',
  `store_id` BIGINT DEFAULT NULL COMMENT '门店ID',
  `created_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `updated_by` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_template_type` (`template_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备模板表';

-- 插入一些默认模板数据（可选）
INSERT INTO `device_template` (`device_id`, `template_type`, `template_name`, `template_data`, `is_default`, `store_id`, `created_by`) VALUES
(0, 'PRINT_FORMAT', '默认打印格式', '{"title":"食品追溯标签","titleFont":"Microsoft YaHei","titleSize":24,"titleAlign":"center","showProductName":true,"showTraceabilityCode":true,"showDate":true,"showQrCode":true,"paperWidth":"80","printDensity":3,"feedLines":5}', 1, 1, 'system'),
(0, 'LAYOUT_DESIGN', '默认版式设计', '{"width":80,"height":60,"marginTop":5,"marginBottom":5,"marginLeft":5,"marginRight":5,"titlePosition":"top","qrPosition":"right","infoLayout":"vertical","borderStyle":"solid","borderWidth":1}', 1, 1, 'system');