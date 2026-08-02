-- ============================================================
-- 创建召回记录表（recall_record）
-- 关联问题：P0-DF-032/033/034（召回管理订单级追溯缺失）
-- 业务背景：
--   食品安全法第二十九条要求食品生产者发现其生产的食品不符合
--   食品安全标准或者有证据证明可能危害人体健康的，应当立即
--   召回，并记录召回情况。本表用于留痕每一次召回操作的审计信息。
-- 表用途：
--   1. 记录召回操作的发起人、原因、时间
--   2. 快照召回时的受影响订单数、客户数、追溯码数（防止后续数据变动影响追溯审计）
--   3. 关联批次号和追溯码，支持反向追溯查询
-- 设计要点：
--   - 采用逻辑删除（deleted=0/1），保留召回历史可追溯
--   - 通过 batch_no 关联 food_trace_codes 表实现批次级追溯
--   - 通过 trace_code 字段支持单码召回场景
--   - 快照字段（affected_*_count）确保审计数据稳定性
-- ============================================================

CREATE TABLE IF NOT EXISTS recall_record (
    recall_id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    batch_no                   VARCHAR(64)  NOT NULL,
    trace_code                 VARCHAR(64),
    recall_reason              VARCHAR(500) NOT NULL,
    initiator_id               BIGINT       NOT NULL,
    initiator_name             VARCHAR(100),
    recall_time                TIMESTAMP    NOT NULL,
    affected_order_count       INTEGER      NOT NULL DEFAULT 0,
    affected_customer_count    INTEGER      NOT NULL DEFAULT 0,
    affected_trace_code_count  INTEGER      NOT NULL DEFAULT 0,
    status                     INTEGER      NOT NULL DEFAULT 1,
    remark                     VARCHAR(500),
    create_time                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                    INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE  recall_record IS '召回记录表（审计留痕，符合食品安全法召回记录要求）';
COMMENT ON COLUMN recall_record.recall_id IS '召回记录ID（主键，自增）';
COMMENT ON COLUMN recall_record.batch_no IS '关联批次号（订单级召回依据）';
COMMENT ON COLUMN recall_record.trace_code IS '关联追溯码（单码反向追溯召回时填写，可选）';
COMMENT ON COLUMN recall_record.recall_reason IS '召回原因';
COMMENT ON COLUMN recall_record.initiator_id IS '发起人ID';
COMMENT ON COLUMN recall_record.initiator_name IS '发起人姓名';
COMMENT ON COLUMN recall_record.recall_time IS '召回发起时间';
COMMENT ON COLUMN recall_record.affected_order_count IS '受影响订单数（执行召回时快照）';
COMMENT ON COLUMN recall_record.affected_customer_count IS '受影响客户数（执行召回时快照）';
COMMENT ON COLUMN recall_record.affected_trace_code_count IS '受影响追溯码数（执行召回时快照）';
COMMENT ON COLUMN recall_record.status IS '召回状态：1-进行中 2-已完成 3-已取消';
COMMENT ON COLUMN recall_record.remark IS '备注';
COMMENT ON COLUMN recall_record.create_time IS '创建时间';
COMMENT ON COLUMN recall_record.update_time IS '更新时间';
COMMENT ON COLUMN recall_record.deleted IS '逻辑删除标记：0-正常 1-删除';

-- 索引：按批次号查询召回记录
CREATE INDEX IF NOT EXISTS idx_recall_record_batch_no ON recall_record (batch_no);
-- 索引：按发起人查询召回记录
CREATE INDEX IF NOT EXISTS idx_recall_record_initiator_id ON recall_record (initiator_id);
-- 索引：按召回时间倒序查询
CREATE INDEX IF NOT EXISTS idx_recall_record_recall_time ON recall_record (recall_time);
-- 索引：按状态筛选召回记录
CREATE INDEX IF NOT EXISTS idx_recall_record_status ON recall_record (status);
