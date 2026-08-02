-- 托盘管理表
CREATE TABLE IF NOT EXISTS tray (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tray_code VARCHAR(50) NOT NULL UNIQUE,
    tray_name VARCHAR(50),
    tray_type VARCHAR(20) DEFAULT 'standard',
    status VARCHAR(20) DEFAULT 'idle',
    current_order_id VARCHAR(50),
    current_kitchen_order_id BIGINT,
    bind_time TIMESTAMP,
    last_use_time TIMESTAMP,
    use_count INTEGER DEFAULT 0,
    store_id BIGINT,
    store_name VARCHAR(100),
    remark VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(50),
    update_by VARCHAR(50),
    deleted SMALLINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_tray_code ON tray(tray_code);
CREATE INDEX IF NOT EXISTS idx_tray_status ON tray(status);
CREATE INDEX IF NOT EXISTS idx_tray_current_order ON tray(current_order_id);

COMMENT ON TABLE tray IS '托盘管理表';
COMMENT ON COLUMN tray.id IS '主键ID';
COMMENT ON COLUMN tray.tray_code IS '托盘码（底部二维码）';
COMMENT ON COLUMN tray.tray_name IS '托盘名称';
COMMENT ON COLUMN tray.tray_type IS '托盘类型：standard-标准, large-大号, small-小号';
COMMENT ON COLUMN tray.status IS '状态：idle-空闲, in_use-使用中, cleaning-清洁中, damaged-损坏';
COMMENT ON COLUMN tray.current_order_id IS '当前绑定订单ID';
COMMENT ON COLUMN tray.current_kitchen_order_id IS '当前绑定后厨订单ID';
COMMENT ON COLUMN tray.bind_time IS '绑定时间';
COMMENT ON COLUMN tray.last_use_time IS '最后使用时间';
COMMENT ON COLUMN tray.use_count IS '使用次数';
COMMENT ON COLUMN tray.store_id IS '所属门店ID';
COMMENT ON COLUMN tray.store_name IS '所属门店名称';
COMMENT ON COLUMN tray.remark IS '备注';
