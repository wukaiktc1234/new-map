-- ============================================================
-- 设备状态日志表
-- 对应实体类: com.foodtraceability.entity.DeviceStatusLog
-- @TableName("device_status_logs")
--
-- 注意: V20260630_002 中创建的 device_status_history 表字段结构
-- 与 DeviceStatusLog 实体类不匹配（id vs log_id，且缺少 old_status/
-- new_status/event_type/message/extra_data 等字段），
-- 因此单独创建 device_status_logs 表。
-- ============================================================

CREATE TABLE IF NOT EXISTS device_status_logs (
    -- 日志ID（主键，自增）
    log_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- 设备ID
    device_id           BIGINT,

    -- 原状态
    old_status          INTEGER,

    -- 新状态
    new_status          INTEGER,

    -- 事件类型：1上线 2离线 3故障 4恢复 5配置变更
    event_type          INTEGER,

    -- 日志消息
    message             VARCHAR(500),

    -- 额外数据JSON
    extra_data          TEXT,

    -- 创建时间
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_device_status_logs_device_id ON device_status_logs(device_id);
CREATE INDEX IF NOT EXISTS idx_device_status_logs_create_time ON device_status_logs(create_time);
CREATE INDEX IF NOT EXISTS idx_device_status_logs_event_type ON device_status_logs(event_type);

-- 添加表注释
COMMENT ON TABLE device_status_logs IS '设备状态日志表（设备上下线、故障、恢复等状态变更记录）';
COMMENT ON COLUMN device_status_logs.log_id IS '日志ID';
COMMENT ON COLUMN device_status_logs.device_id IS '设备ID';
COMMENT ON COLUMN device_status_logs.old_status IS '变更前状态';
COMMENT ON COLUMN device_status_logs.new_status IS '变更后状态';
COMMENT ON COLUMN device_status_logs.event_type IS '事件类型：1上线 2离线 3故障 4恢复 5配置变更';
COMMENT ON COLUMN device_status_logs.message IS '日志消息';
COMMENT ON COLUMN device_status_logs.extra_data IS '额外数据（JSON格式）';
COMMENT ON COLUMN device_status_logs.create_time IS '创建时间';
