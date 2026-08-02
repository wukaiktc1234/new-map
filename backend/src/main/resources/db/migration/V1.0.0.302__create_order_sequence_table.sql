-- 订单序列表（用于生成唯一订单号）
-- 执行此SQL创建表

CREATE TABLE IF NOT EXISTS order_sequence (
    sequence_key VARCHAR(50) PRIMARY KEY,
    sequence_value INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE order_sequence IS '订单号序列表';
COMMENT ON COLUMN order_sequence.sequence_key IS '序列键（如日期前缀）';
COMMENT ON COLUMN order_sequence.sequence_value IS '当前序列值';
