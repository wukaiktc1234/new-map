-- 叫号记录表
CREATE TABLE IF NOT EXISTS call_record (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    order_number VARCHAR(50) NOT NULL,
    table_number VARCHAR(20),
    order_type VARCHAR(20),
    item_count INTEGER DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    call_count INTEGER DEFAULT 0,
    first_call_time TIMESTAMP,
    last_call_time TIMESTAMP,
    pick_time TIMESTAMP,
    wait_seconds INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_call_record_order_id ON call_record(order_id);
CREATE INDEX IF NOT EXISTS idx_call_record_order_number ON call_record(order_number);
CREATE INDEX IF NOT EXISTS idx_call_record_status ON call_record(status);
CREATE INDEX IF NOT EXISTS idx_call_record_create_time ON call_record(create_time);

COMMENT ON TABLE call_record IS '叫号记录表';
COMMENT ON COLUMN call_record.id IS '主键ID';
COMMENT ON COLUMN call_record.order_id IS '订单ID';
COMMENT ON COLUMN call_record.order_number IS '订单号';
COMMENT ON COLUMN call_record.table_number IS '桌号';
COMMENT ON COLUMN call_record.order_type IS '订单类型';
COMMENT ON COLUMN call_record.item_count IS '餐品数量';
COMMENT ON COLUMN call_record.status IS '状态: pending-待叫号, called-已叫号, picked-已取餐';
COMMENT ON COLUMN call_record.call_count IS '叫号次数';
COMMENT ON COLUMN call_record.first_call_time IS '首次叫号时间';
COMMENT ON COLUMN call_record.last_call_time IS '最后叫号时间';
COMMENT ON COLUMN call_record.pick_time IS '取餐时间';
COMMENT ON COLUMN call_record.wait_seconds IS '等待秒数';
