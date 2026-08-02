-- ============================================
-- V20260607__create_appeal_tables.sql
-- 申诉系统数据库迁移脚本
-- 包含：申诉主表、附件表、处理日志表
-- ============================================

-- ============================================
-- 1. 申诉主表 (appeals)
-- ============================================
CREATE TABLE appeals (
    appeal_id          VARCHAR(32)    NOT NULL,
    type               VARCHAR(20)    NOT NULL,  -- penalty / complaint
    anonymous_flag     BOOLEAN        NOT NULL DEFAULT FALSE,
    employee_id        VARCHAR(32)    NOT NULL,
    target_decision_id VARCHAR(32),              -- 关联处罚单号（仅penalty模式）
    status             VARCHAR(20)    NOT NULL DEFAULT 'pending',
    title              VARCHAR(200)   NOT NULL,
    description        TEXT           NOT NULL,
    expected_result    TEXT,
    create_time        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_appeals PRIMARY KEY (appeal_id)
);

COMMENT ON TABLE appeals IS '申诉主表：处罚申诉与投诉举报';
COMMENT ON COLUMN appeals.appeal_id IS '申诉ID（雪花算法）';
COMMENT ON COLUMN appeals.type IS '类型：penalty=处罚申诉 / complaint=投诉举报';
COMMENT ON COLUMN appeals.anonymous_flag IS '是否匿名';
COMMENT ON COLUMN appeals.employee_id IS '申诉人员工ID';
COMMENT ON COLUMN appeals.target_decision_id IS '关联处罚单号';
COMMENT ON COLUMN appeals.status IS '状态：pending/processing/resolved/closed/withdrawn';

-- 索引
CREATE INDEX IF NOT EXISTS idx_appeals_employee_id ON appeals (employee_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_appeals_status ON appeals (status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_appeals_type ON appeals (type) WHERE deleted = 0;

-- ============================================
-- 2. 申诉附件表 (appeal_attachments)
-- ============================================
CREATE TABLE appeal_attachments (
    attachment_id      VARCHAR(32)    NOT NULL,
    appeal_id          VARCHAR(32)    NOT NULL,
    file_name          VARCHAR(200)   NOT NULL,
    file_url           VARCHAR(500)   NOT NULL,
    file_type          VARCHAR(50),
    file_size          BIGINT,
    create_time        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_appeal_attachments PRIMARY KEY (attachment_id)
);

COMMENT ON TABLE appeal_attachments IS '申诉附件表';
COMMENT ON COLUMN appeal_attachments.attachment_id IS '附件ID';
COMMENT ON COLUMN appeal_attachments.appeal_id IS '关联申诉ID（外键→appeals）';
COMMENT ON COLUMN appeal_attachments.file_size IS '文件大小（字节）';

-- 外键索引
CREATE INDEX idx_appeal_attachments_appeal_id ON appeal_attachments (appeal_id) WHERE deleted = 0;

-- ============================================
-- 3. 申诉处理日志表 (appeal_process_logs)
-- ============================================
CREATE TABLE appeal_process_logs (
    log_id             VARCHAR(32)    NOT NULL,
    appeal_id          VARCHAR(32)    NOT NULL,
    action             VARCHAR(50)    NOT NULL,  -- submit/accept/process/resolve/close/withdraw/reject
    operator_id        VARCHAR(32),
    operator_name      VARCHAR(100),
    comment            TEXT,
    create_time        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_appeal_process_logs PRIMARY KEY (log_id)
);

COMMENT ON TABLE appeal_process_logs IS '申诉处理日志表：记录每次状态变更';
COMMENT ON COLUMN appeal_process_logs.action IS '操作类型';
COMMENT ON COLUMN appeal_process_logs.operator_name IS '操作人姓名';

-- 索引
CREATE INDEX IF NOT EXISTS idx_appeal_process_logs_appeal_id ON appeal_process_logs (appeal_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_appeal_process_logs_create_time ON appeal_process_logs (create_time) WHERE deleted = 0;
