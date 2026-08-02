-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 设备驱动版本表
CREATE TABLE IF NOT EXISTS device_driver_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    driver_name VARCHAR(100) NOT NULL COMMENT '驱动名称',
    version VARCHAR(20) NOT NULL COMMENT '驱动版本号',
    supported_device_type VARCHAR(50) NOT NULL COMMENT '支持的设备类型：SCANNER-扫码枪、PRINTER-打印机、CAMERA-摄像头',
    supported_device_models TEXT COMMENT '支持的设备型号，JSON格式',
    driver_file_path VARCHAR(255) NOT NULL COMMENT '驱动文件路径',
    file_size BIGINT COMMENT '驱动文件大小（字节）',
    file_md5 VARCHAR(32) COMMENT '驱动文件MD5值',
    release_date DATETIME NOT NULL COMMENT '发布日期',
    release_notes TEXT COMMENT '发布说明',
    is_default TINYINT DEFAULT 0 COMMENT '是否为默认版本：0-否，1-是',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(50) COMMENT '创建人',
    updated_by VARCHAR(50) COMMENT '更新人',
    -- 索引设计
    INDEX idx_supported_device_type (supported_device_type),
    INDEX idx_version (version),
    INDEX idx_release_date (release_date),
    INDEX idx_is_default (is_default),
    INDEX idx_status (status),
    UNIQUE INDEX uk_driver_version (driver_name, version) -- 确保驱动名称和版本号的组合唯一
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备驱动版本表';
