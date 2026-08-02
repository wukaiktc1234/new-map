-- 添加设备连接配置字段（PostgreSQL兼容）
-- 原脚本使用MySQL存储过程语法，现重写为PostgreSQL兼容

-- 添加 connection_config 字段
ALTER TABLE devices ADD COLUMN IF NOT EXISTS connection_config JSONB;
COMMENT ON COLUMN devices.connection_config IS '连接配置（IP地址、端口、驱动等）';

-- 添加 supported_functions 字段
ALTER TABLE devices ADD COLUMN IF NOT EXISTS supported_functions JSONB;
COMMENT ON COLUMN devices.supported_functions IS '支持功能：["order","dish","receipt","label"]';

-- 添加 last_online_time 字段
ALTER TABLE devices ADD COLUMN IF NOT EXISTS last_online_time TIMESTAMP;
COMMENT ON COLUMN devices.last_online_time IS '最后在线时间';
