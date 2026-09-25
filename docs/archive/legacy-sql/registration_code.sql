-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 创建注册码表
CREATE TABLE `registration_code` (
  `id` varchar(36) NOT NULL DEFAULT UUID() COMMENT '主键ID',
  `code` varchar(50) NOT NULL COMMENT '注册码',
  `type` varchar(20) NOT NULL DEFAULT 'INTERNAL' COMMENT '注册码类型（INTERNAL：内部员工，EXTERNAL：外部合作）',
  `validity_start` datetime NOT NULL COMMENT '有效期开始时间',
  `validity_end` datetime NOT NULL COMMENT '有效期结束时间',
  `status` varchar(20) NOT NULL DEFAULT 'UNUSED' COMMENT '状态（UNUSED：未使用，USED：已使用，EXPIRED：已过期，INVALID：无效）',
  `created_by` varchar(36) DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status` (`status`),
  KEY `idx_validity` (`validity_start`,`validity_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='注册码表';

-- 创建注册码日志表
CREATE TABLE `registration_code_log` (
  `id` varchar(36) NOT NULL DEFAULT UUID() COMMENT '主键ID',
  `code` varchar(50) NOT NULL COMMENT '注册码',
  `user_id` varchar(36) DEFAULT NULL COMMENT '用户ID',
  `username` varchar(50) DEFAULT NULL COMMENT '用户名',
  `use_time` datetime DEFAULT NULL COMMENT '使用时间',
  `status` varchar(20) NOT NULL COMMENT '使用状态（SUCCESS：成功，FAILED：失败）',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_code` (`code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_use_time` (`use_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='注册码使用日志表';