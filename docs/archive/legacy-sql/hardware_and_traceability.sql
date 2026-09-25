-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 硬件设备配置表
CREATE TABLE IF NOT EXISTS hardware_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    device_type VARCHAR(50) COMMENT '设备类型：SCANNER-扫码枪、PRINTER-打印机、CAMERA-摄像头',
    device_name VARCHAR(100) COMMENT '设备名称',
    device_model VARCHAR(100) COMMENT '设备型号',
    connection_type VARCHAR(50) COMMENT '连接类型：SERIAL-串口、NETWORK-网络、BLUETOOTH-蓝牙',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    port VARCHAR(50) COMMENT '端口号，支持WSD协议字符串端口',
    baud_rate VARCHAR(20) COMMENT '波特率',
    api_key VARCHAR(100) COMMENT 'API密钥',
    config_json TEXT COMMENT '配置JSON',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用、1-启用',
    remark VARCHAR(500) COMMENT '备注',
    store_id BIGINT COMMENT '门店ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(50) COMMENT '创建人',
    updated_by VARCHAR(50) COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除、1-已删除',
    INDEX idx_store_device (store_id, device_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='硬件设备配置表';

-- 自有库存编码表
CREATE TABLE IF NOT EXISTS inventory_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    inventory_id BIGINT COMMENT '库存ID',
    code_type VARCHAR(50) COMMENT '编码类型：BARCODE-条形码、QR-二维码、CUSTOM-自定义',
    unique_code VARCHAR(100) UNIQUE COMMENT '唯一编码',
    batch_no VARCHAR(50) COMMENT '批次号',
    qr_code TEXT COMMENT '二维码数据',
    status TINYINT DEFAULT 1 COMMENT '状态：0-未使用、1-已使用、2-已过期',
    store_id BIGINT COMMENT '门店ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(50) COMMENT '创建人',
    updated_by VARCHAR(50) COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除、1-已删除',
    INDEX idx_inventory (inventory_id),
    INDEX idx_code (unique_code),
    INDEX idx_batch (batch_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自有库存编码表';

-- 食品追溯码表
CREATE TABLE IF NOT EXISTS traceability_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    code_type VARCHAR(50) COMMENT '编码类型：QR-二维码、BARCODE-条形码、DIGITAL-数字码',
    unique_code VARCHAR(100) UNIQUE COMMENT '唯一编码',
    product_name VARCHAR(200) COMMENT '产品名称',
    inventory_id BIGINT COMMENT '库存ID',
    batch_no VARCHAR(50) COMMENT '批次号',
    order_no VARCHAR(50) COMMENT '订单号',
    status TINYINT DEFAULT 1 COMMENT '状态：0-未制作、1-制作中、2-已完成、3-已过期',
    production_time TIMESTAMP NULL COMMENT '制作时间',
    expiry_time TIMESTAMP NULL COMMENT '过期时间',
    store_id BIGINT COMMENT '门店ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(50) COMMENT '创建人',
    updated_by VARCHAR(50) COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0-未删除、1-已删除',
    INDEX idx_inventory (inventory_id),
    INDEX idx_code (unique_code),
    INDEX idx_order (order_no),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食品追溯码表';

-- 插入默认硬件配置（示例数据）
INSERT INTO hardware_config (device_type, device_name, connection_type, status, remark, created_by, updated_by) VALUES
('SCANNER', '默认扫码枪', 'SERIAL', 1, '系统默认配置', 'system', 'system'),
('PRINTER', '默认热敏打印机', 'NETWORK', 1, '系统默认配置', 'system', 'system'),
('CAMERA', '默认摄像头', 'NETWORK', 1, '系统默认配置', 'system', 'system');