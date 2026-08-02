-- ============================================================
-- V20260717_033: 创建事件发件箱表（Outbox Pattern / DF-038）
-- ============================================================
-- 背景：
--   业务事件（如 PurchaseStockInEvent、OrderCompletedEvent）目前通过 Spring
--   ApplicationEvent 发布，如果服务重启或线程池满，事件会丢失。
--   引入 Outbox Pattern：业务事务中同表写入事件记录，由后台轮询器异步投递，
--   保证事件至少被投递一次（at-least-once）。
--
-- 表性质：
--   队列/日志类基础设施数据（非业务核心数据）。事件处理完成后可物理删除
--   或归档，故不包含 deleted 逻辑删除字段。
--
-- 幂等性：
--   CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，二次执行安全。
--   COMMENT ON 语句本身幂等。
-- ============================================================

CREATE TABLE IF NOT EXISTS event_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    event_id VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64),
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    error_message TEXT
);

CREATE INDEX IF NOT EXISTS idx_event_outbox_status_created
    ON event_outbox (status, created_at);

COMMENT ON TABLE event_outbox IS '事件发件箱表（Outbox Pattern）';
COMMENT ON COLUMN event_outbox.event_type IS '事件类型（如 PurchaseStockInEvent）';
COMMENT ON COLUMN event_outbox.event_id IS '事件唯一 ID';
COMMENT ON COLUMN event_outbox.aggregate_id IS '聚合根 ID（如订单号/采购单号）';
COMMENT ON COLUMN event_outbox.payload IS '事件 JSON 负载';
COMMENT ON COLUMN event_outbox.status IS '状态（PENDING/PROCESSED/FAILED）';
COMMENT ON COLUMN event_outbox.retry_count IS '重试次数';
COMMENT ON COLUMN event_outbox.processed_at IS '处理完成时间';
COMMENT ON COLUMN event_outbox.error_message IS '错误信息';
