-- 门店运营模块架构优化 - 数据库迁移脚本
-- 新增4张核心业务表：待办任务、日结对账、班次明细、招聘审批记录
-- 版本：V20260511__create_store_ops_optimization_tables.sql
-- 作者：SDD Pipeline Phase 4 (Implement)
-- 日期：2026-05-11

-- ══════════════════════════════════════════════════════════════
-- 1. 待办任务表（pending_tasks）
-- 用途：存储所有类型的待办事项，支持联动跳转和批量操作
-- ══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS pending_tasks (
    task_id          VARCHAR(32) NOT NULL,
    task_type        VARCHAR(30) NOT NULL,
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    priority         INTEGER NOT NULL DEFAULT 2,
    assignee_id      VARCHAR(32),
    assignee_role    VARCHAR(20),
    source_type      VARCHAR(30),
    source_id        VARCHAR(32),
    redirect_url     VARCHAR(500),
    status           VARCHAR(20) NOT NULL DEFAULT 'pending',
    due_date         TIMESTAMP,
    completed_at     TIMESTAMP,
    created_by       VARCHAR(32) NOT NULL,
    create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER NOT NULL DEFAULT 0,
    version          BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_pending_tasks PRIMARY KEY (task_id)
);

COMMENT ON TABLE pending_tasks IS '待办任务表 - 支持联动跳转的智能任务中心';
COMMENT ON COLUMN pending_tasks.task_id IS '任务ID（雪花算法生成）';
COMMENT ON COLUMN pending_tasks.task_type IS '任务类型：settlement_confirm/certificate_expiry/recruitment_approval/inventory_warning/device_fault/revenue_alert';
COMMENT ON COLUMN pending_tasks.priority IS '优先级（数字编码）：1=低 2=中 3=高 4=紧急';
COMMENT ON COLUMN pending_tasks.status IS '状态：pending/completed/expired/cancelled';
COMMENT ON COLUMN pending_tasks.redirect_url IS '联动跳转URL（点击立即处理时跳转的目标页面）';

CREATE INDEX IF NOT EXISTS idx_pending_tasks_assignee ON pending_tasks(assignee_id, status);
CREATE INDEX IF NOT EXISTS idx_pending_tasks_type_status ON pending_tasks(task_type, status);
CREATE INDEX IF NOT EXISTS idx_pending_tasks_due_date ON pending_tasks(due_date) WHERE status = 'pending' AND deleted = 0;
CREATE INDEX IF NOT EXISTS idx_pending_tasks_created_by ON pending_tasks(created_by);
CREATE INDEX IF NOT EXISTS idx_pending_tasks_deleted ON pending_tasks(deleted);

-- ══════════════════════════════════════════════════════════════
-- 2. 日结对账主表（daily_settlements）
-- 用途：记录每日门店营收对账数据，支持权限控制（敏感字段隐藏）
-- ══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS daily_settlements (
    settlement_id    VARCHAR(32) NOT NULL,
    store_id         VARCHAR(32) NOT NULL,
    settlement_date  DATE NOT NULL,
    total_revenue    BIGINT NOT NULL DEFAULT 0,
    total_cost       BIGINT NOT NULL DEFAULT 0,
    net_profit       BIGINT NOT NULL DEFAULT 0,
    gross_profit_rate DECIMAL(5,2),
    order_count      INTEGER NOT NULL DEFAULT 0,
    avg_order_value  BIGINT NOT NULL DEFAULT 0,
    table_usage_rate DECIMAL(5,2),
    difference_amount BIGINT NOT NULL DEFAULT 0,
    auditor_id       VARCHAR(32),
    auditor_name     VARCHAR(50),
    status           VARCHAR(20) NOT NULL DEFAULT 'pending',
    remark           TEXT,
    create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER NOT NULL DEFAULT 0,
    version          BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_daily_settlements PRIMARY KEY (settlement_id)
);

COMMENT ON TABLE daily_settlements IS '日结对账主表 - 记录门店每日营收对账数据，支持敏感字段权限控制';
COMMENT ON COLUMN daily_settlements.settlement_id IS '对账ID（雪花算法）';
COMMENT ON COLUMN daily_settlements.total_revenue IS '总营收（单位：分）';
COMMENT ON COLUMN daily_settlements.total_cost IS '总成本（单位：分）[敏感字段-仅总部可见]';
COMMENT ON COLUMN daily_settlements.net_profit IS '净利润（单位：分）[敏感字段-仅总部可见]';
COMMENT ON COLUMN daily_settlements.gross_profit_rate IS '毛利率（百分比）[敏感字段-仅总部可见]';
COMMENT ON COLUMN daily_settlements.difference_amount IS '对账差异金额（单位：分）';
COMMENT ON COLUMN daily_settlements.status IS '状态：pending/confirmed/abnormal';

CREATE INDEX IF NOT EXISTS idx_daily_settlements_store_date ON daily_settlements(store_id, settlement_date);
CREATE INDEX IF NOT EXISTS idx_daily_settlements_status ON daily_settlements(status);
CREATE INDEX IF NOT EXISTS idx_daily_settlements_auditor ON daily_settlements(auditor_id);
CREATE INDEX IF NOT EXISTS idx_daily_settlements_deleted ON daily_settlements(deleted);

-- ══════════════════════════════════════════════════════════════
-- 3. 对账班次明细子表（daily_settlement_shifts）
-- 用途：记录每个班次（早/晚/通宵）的详细营收数据
-- ══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS daily_settlement_shifts (
    shift_id         VARCHAR(32) NOT NULL,
    settlement_id    VARCHAR(32) NOT NULL,
    shift_type       VARCHAR(20) NOT NULL,
    start_time       TIME NOT NULL,
    end_time         TIME NOT NULL,
    revenue          BIGINT NOT NULL DEFAULT 0,
    order_count      INTEGER NOT NULL DEFAULT 0,
    cashier_id       VARCHAR(32),
    cashier_name     VARCHAR(50),
    create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER NOT NULL DEFAULT 0,
    version          BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_daily_settlement_shifts PRIMARY KEY (shift_id),

    CONSTRAINT fk_shifts_settlement
        FOREIGN KEY (settlement_id)
        REFERENCES daily_settlements(settlement_id)
        ON DELETE CASCADE
);

COMMENT ON TABLE daily_settlement_shifts IS '对账班次明细子表 - 存储早班/晚班/通宵班的详细营收数据';
COMMENT ON COLUMN daily_settlement_shifts.shift_id IS '班次ID（雪花算法）';
COMMENT ON COLUMN daily_settlement_shifts.shift_type IS '班次类型：morning/evening/overnight';
COMMENT ON COLUMN daily_settlement_shifts.revenue IS '该班次营收（单位：分）';

CREATE INDEX IF NOT EXISTS idx_shifts_settlement ON daily_settlement_shifts(settlement_id);
CREATE INDEX IF NOT EXISTS idx_shifts_type ON daily_settlement_shifts(shift_type);
CREATE INDEX IF NOT EXISTS idx_shifts_cashier ON daily_settlement_shifts(cashier_id);
CREATE INDEX IF NOT EXISTS idx_shifts_deleted ON daily_settlement_shifts(deleted);

-- ══════════════════════════════════════════════════════════════
-- 4. 招聘审批记录表（recruitment_approvals）
-- 用途：记录门店自主招聘的三级审批流程（门店→区域→总部）
-- ══════════════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS recruitment_approvals (
    approval_id      VARCHAR(32) NOT NULL,
    post_id          VARCHAR(32) NOT NULL,
    applicant_name   VARCHAR(100) NOT NULL,
    phone            VARCHAR(20),
    proposed_salary  BIGINT,
    interview_score  INTEGER,
    interviewer_id   VARCHAR(32),
    interviewer_comment TEXT,
    current_level    SMALLINT NOT NULL DEFAULT 1,
    approver_id      VARCHAR(32),
    approval_status  VARCHAR(20) NOT NULL DEFAULT 'pending',
    rejection_reason TEXT,
    approved_at      TIMESTAMP,
    final_decision   VARCHAR(20),
    hired_at         TIMESTAMP,
    employee_id      VARCHAR(32),
    create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER NOT NULL DEFAULT 0,
    version          BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_recruitment_approvals PRIMARY KEY (approval_id)
);

COMMENT ON TABLE recruitment_approvals IS '招聘审批记录表 - 支持三级审批流程的状态机实现';
COMMENT ON COLUMN recruitment_approvals.approval_id IS '审批ID（雪花算法）';
COMMENT ON COLUMN recruitment_approvals.proposed_salary IS '建议薪资（单位：分/月，符合项目金额规范）';
COMMENT ON COLUMN recruitment_approvals.interview_score IS '面试评分（1-100）';
COMMENT ON COLUMN recruitment_approvals.current_level IS '当前审批层级：1=门店提交 2=区域审核 3=总部终审';
COMMENT ON COLUMN recruitment_approvals.approval_status IS '审批状态：pending/regional_review/hq_review/approved_by_regional/rejected_by_regional/final_approved/final_rejected/hired/ended';

CREATE INDEX IF NOT EXISTS idx_approvals_post ON recruitment_approvals(post_id);
CREATE INDEX IF NOT EXISTS idx_approvals_status ON recruitment_approvals(approval_status);
CREATE INDEX IF NOT EXISTS idx_approvals_current_level ON recruitment_approvals(current_level, approval_status);
CREATE INDEX IF NOT EXISTS idx_approvals_approver ON recruitment_approvals(approver_id);
CREATE INDEX IF NOT EXISTS idx_approvals_interviewer ON recruitment_approvals(interviewer_id);
CREATE INDEX IF NOT EXISTS idx_approvals_deleted ON recruitment_approvals(deleted);

-- ══════════════════════════════════════════════════════════════
-- 5. 初始化数据（可选 - 根据需要添加测试数据）
-- ══════════════════════════════════════════════════════════════

-- 示例：插入一些初始待办任务类型枚举值说明（仅注释参考，不插入数据）
-- task_type 可选值:
--   - settlement_confirm: 日结对账确认
--   - certificate_expiry: 证件到期提醒
--   - recruitment_approval: 招聘审批请求
--   - inventory_warning: 库存预警通知
--   - device_fault: 设备故障报警
--   - revenue_alert: 营收异常预警

-- priority 可选值:
--   - high: 紧急（红色标记）
--   - medium: 一般（黄色标记）
--   - low: 低优先级（蓝色标记）

-- shift_type 可选值:
--   - morning: 早班 (06:00-14:00)
--   - evening: 晚班 (14:00-22:00)
--   - overnight: 通宵班 (22:00-06:00)

-- approval_status 可选值（状态机）:
--   - pending: 待提交（初始状态）
--   - regional_review: 区域审核中
--   - hq_review: 总部终审中
--   - approved_by_regional: 区域已通过（等待终审）
--   - rejected_by_regional: 区域已驳回
--   - final_approved: 总部终审通过
--   - final_rejected: 总部终审驳回
--   - hired: 已入职（终态-成功）
--   - ended: 已结束（终态-失败）

-- ══════════════════════════════════════════════════════════════
-- 迁移脚本完成
-- 验证命令（在psql中执行）:
--   \dt pending_tasks
--   \dt daily_settlements
--   \dt daily_settlement_shifts
--   \dt recruitment_approvals
-- ══════════════════════════════════════════════════════════════
