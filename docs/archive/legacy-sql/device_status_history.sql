-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
CREATE TABLE IF NOT EXISTS `device_status_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `device_id` BIGINT NOT NULL COMMENT '设备ID',
    `device_name` VARCHAR(100) COMMENT '设备名称',
    `device_type` VARCHAR(50) NOT NULL COMMENT '设备类型',
    `store_id` BIGINT COMMENT '门店ID',
    `status_before` VARCHAR(50) COMMENT '变化前状态',
    `status_after` VARCHAR(50) NOT NULL COMMENT '变化后状态',
    `change_reason` VARCHAR(200) COMMENT '状态变化原因',
    `operator` VARCHAR(100) COMMENT '操作人',
    `change_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '状态变化时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_device_id` (`device_id`),
    KEY `idx_device_type` (`device_type`),
    KEY `idx_store_id` (`store_id`),
    KEY `idx_change_time` (`change_time`),
    KEY `idx_status_after` (`status_after`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备状态历史记录表';
