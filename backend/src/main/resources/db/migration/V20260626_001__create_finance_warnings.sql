-- ============================================================
-- 财务预警表 (finance_warnings)
-- 版本: V20260626_001
-- 说明: 创建财务预警表，用于记录和处理财务异常预警
-- 日期: 2026-06-26
-- ============================================================

CREATE TABLE IF NOT EXISTS finance_warnings (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    warning_type        VARCHAR(50),                          -- 预警类型：BUDGET_OVER/RECEIVABLE_OVERDUE/PAYABLE_OVERDUE/CASH_FLOW等
    warning_level       VARCHAR(20),                          -- 预警级别：INFO/WARNING/ERROR/CRITICAL
    title               VARCHAR(200),                         -- 预警标题
    content             TEXT,                                 -- 预警内容
    business_id         VARCHAR(50),                          -- 关联业务ID
    business_type       VARCHAR(50),                          -- 关联业务类型
    warning_date        TIMESTAMP,                            -- 预警发生时间
    status              VARCHAR(20) DEFAULT 'UNHANDLED',      -- 状态：UNHANDLED/HANDLING/HANDLED/IGNORED
    handler             VARCHAR(100),                         -- 处理人
    handle_time         TIMESTAMP,                            -- 处理时间
    handle_result       TEXT,                                 -- 处理结果
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by           VARCHAR(100),                         -- 创建人
    update_by           VARCHAR(100),                         -- 更新人
    deleted             INTEGER NOT NULL DEFAULT 0
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_finance_warnings_type ON finance_warnings(warning_type) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_warnings_level ON finance_warnings(warning_level) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_warnings_status ON finance_warnings(status) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_warnings_date ON finance_warnings(warning_date) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_finance_warnings_business ON finance_warnings(business_id, business_type) WHERE deleted = 0;

-- 注释
COMMENT ON TABLE finance_warnings IS '财务预警表';
COMMENT ON COLUMN finance_warnings.id IS '主键ID';
COMMENT ON COLUMN finance_warnings.warning_type IS '预警类型：BUDGET_OVER/RECEIVABLE_OVERDUE/PAYABLE_OVERDUE/CASH_FLOW等';
COMMENT ON COLUMN finance_warnings.warning_level IS '预警级别：INFO/WARNING/ERROR/CRITICAL';
COMMENT ON COLUMN finance_warnings.title IS '预警标题';
COMMENT ON COLUMN finance_warnings.content IS '预警内容';
COMMENT ON COLUMN finance_warnings.business_id IS '关联业务ID';
COMMENT ON COLUMN finance_warnings.business_type IS '关联业务类型';
COMMENT ON COLUMN finance_warnings.warning_date IS '预警发生时间';
COMMENT ON COLUMN finance_warnings.status IS '状态：UNHANDLED-未处理/HANDLING-处理中/HANDLED-已处理/IGNORED-已忽略';
COMMENT ON COLUMN finance_warnings.handler IS '处理人';
COMMENT ON COLUMN finance_warnings.handle_time IS '处理时间';
COMMENT ON COLUMN finance_warnings.handle_result IS '处理结果';
