-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 设备运行数据表
CREATE TABLE IF NOT EXISTS device_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    device_id VARCHAR(100) NOT NULL COMMENT '设备ID',
    device_type VARCHAR(50) NOT NULL COMMENT '设备类型：SCANNER-扫码枪、PRINTER-打印机、CAMERA-摄像头',
    device_name VARCHAR(100) COMMENT '设备名称',
    connection_status TINYINT DEFAULT 0 COMMENT '连接状态：0-断开，1-连接',
    response_time BIGINT COMMENT '响应时间（毫秒）',
    error_count INT DEFAULT 0 COMMENT '错误次数',
    operation_count INT DEFAULT 0 COMMENT '操作次数',
    temperature DOUBLE COMMENT '设备温度（摄氏度）',
    load DOUBLE COMMENT '设备负载（百分比）',
    signal_strength INT COMMENT '信号强度（百分比）',
    collect_time DATETIME NOT NULL COMMENT '数据采集时间',
    status_details TEXT COMMENT '设备状态详情',
    store_id BIGINT COMMENT '门店ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    -- 索引设计
    INDEX idx_device_id (device_id),
    INDEX idx_collect_time (collect_time),
    INDEX idx_device_type (device_type),
    INDEX idx_store_id (store_id),
    INDEX idx_device_time (device_id, collect_time),
    INDEX idx_type_time (device_type, collect_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备运行数据表';

-- 设备数据统计视图（可选）
CREATE VIEW IF NOT EXISTS v_device_data_statistics AS
SELECT 
    device_id,
    device_type,
    COUNT(*) AS total_records,
    SUM(CASE WHEN connection_status = 1 THEN 1 ELSE 0 END) AS connected_count,
    AVG(response_time) AS avg_response_time,
    SUM(error_count) AS total_error_count,
    SUM(operation_count) AS total_operation_count,
    MAX(collect_time) AS last_collect_time
FROM device_data
GROUP BY device_id, device_type;
