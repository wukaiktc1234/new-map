-- ============================================================
-- 审批工作流模块：审批流程定义 + 审批记录
-- 兼容 H2 (MODE=PostgreSQL) 与 PostgreSQL 18
-- nodes / conditions 使用 TEXT 存储 JSON 字符串（避免 H2 不支持 JSONB）
-- ============================================================

-- Part 1: 审批流程定义表（approval_workflows）
-- 存储按 业务类型 + 权限模板 配置的审批流程定义
CREATE TABLE IF NOT EXISTS approval_workflows (
    workflow_id VARCHAR(32) PRIMARY KEY,
    workflow_name VARCHAR(100) NOT NULL,
    business_type VARCHAR(50) NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    nodes TEXT,
    conditions TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_approval_workflows_business_type ON approval_workflows(business_type);
CREATE INDEX IF NOT EXISTS idx_approval_workflows_template_code ON approval_workflows(template_code);
CREATE INDEX IF NOT EXISTS idx_approval_workflows_enabled ON approval_workflows(enabled);
CREATE INDEX IF NOT EXISTS idx_approval_workflows_deleted ON approval_workflows(deleted);

COMMENT ON TABLE approval_workflows IS '审批流程定义表';
COMMENT ON COLUMN approval_workflows.business_type IS '业务类型: purchase_request/purchase_order/contract/payment 等';
COMMENT ON COLUMN approval_workflows.template_code IS '权限模板编码: enterprise-chain/standard-chain/centralized-single/custom';
COMMENT ON COLUMN approval_workflows.enabled IS '是否启用';
COMMENT ON COLUMN approval_workflows.nodes IS '审批节点列表(JSON字符串)';
COMMENT ON COLUMN approval_workflows.conditions IS '条件分支列表(JSON字符串)';

-- Part 2: 审批记录表（approval_audit_logs）
-- 记录每次审批操作（提交/通过/驳回/撤回）的详细日志
CREATE TABLE IF NOT EXISTS approval_audit_logs (
    log_id VARCHAR(32) PRIMARY KEY,
    workflow_id VARCHAR(32),
    business_id VARCHAR(32) NOT NULL,
    business_type VARCHAR(50) NOT NULL,
    node_id VARCHAR(32),
    node_name VARCHAR(100),
    approver_type VARCHAR(30),
    approver_name VARCHAR(50),
    action VARCHAR(20) NOT NULL,
    opinion VARCHAR(1000),
    operate_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_approval_audit_logs_business ON approval_audit_logs(business_id, business_type);
CREATE INDEX IF NOT EXISTS idx_approval_audit_logs_workflow ON approval_audit_logs(workflow_id);
CREATE INDEX IF NOT EXISTS idx_approval_audit_logs_action ON approval_audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_approval_audit_logs_deleted ON approval_audit_logs(deleted);

COMMENT ON TABLE approval_audit_logs IS '审批记录表';
COMMENT ON COLUMN approval_audit_logs.action IS '操作类型: submit-提交 approve-通过 reject-驳回 withdraw-撤回';
COMMENT ON COLUMN approval_audit_logs.approver_type IS '审批人类型: role/superior/form_field/specific_user';
COMMENT ON COLUMN approval_audit_logs.opinion IS '审批意见';
