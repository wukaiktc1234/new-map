-- ============================================================
-- 迁移脚本: V20260625_004__sprint1_5_key_unification.sql
-- 阶段: Sprint 1.5 - Phase 4 阶段 2
-- 任务: T-006 / T-007 / T-008
-- 日期: 2026-06-25
-- ============================================================
--
-- 【Sprint 1.5 说明】
-- 本次 Sprint 目标：PostgreSQL 开发环境对接 + 20 个 UUID 表主键统一化。
-- 本脚本负责其中 16 张表的主键类型统一化迁移（剩余 4 张表由其他脚本处理）。
-- 采用 DROP + CREATE 方式（开发环境数据可清空）。
--
-- 【混合主键方案说明】
-- 根据 ADR 决策，采用混合主键方案：
--   1. 对内表（10 张）：BIGINT GENERATED ALWAYS AS IDENTITY
--      适用：内部系统表、日志表、配置表（不对外暴露 ID）
--      对应 Java: @TableId(type = IdType.AUTO)
--   2. 对外表（6 张）：VARCHAR(32) 雪花算法
--      适用：对外暴露 ID 的业务表（招聘、健康证、发票等）
--      对应 Java: @TableId(type = IdType.ASSIGN_ID)
--
-- 【H2 + PostgreSQL 双环境兼容说明】
-- - 开发环境: H2 内存数据库（MODE=PostgreSQL 兼容模式）
-- - 生产环境: PostgreSQL 18
-- - 不使用 PG 特有语法（如 GIN 索引、JSONB 操作符）
-- - 使用 GENERATED ALWAYS AS IDENTITY（H2 在 MODE=PostgreSQL 下支持）
-- - 部分索引 WHERE deleted = 0（H2 与 PG 均支持）
-- - JSONB 类型（H2 作为 JSON 别名支持）
--
-- 【关键变更点】
-- 1. task_sub_tasks.task_id: VARCHAR(32) → BIGINT（关联 tasks.task_id）
-- 2. health_certificate.last_expense_id: VARCHAR(36) → BIGINT（关联 health_certificate_expense.id）
-- 3. 对外表之间外键字段统一为 VARCHAR(32)（与外表主键类型一致）
--    - resumes.requirement_id
--    - interviews.resume_id / requirement_id
--    - onboarding_records.resume_id / requirement_id
--    - health_certificate_expense.health_certificate_id
--    - invoice.health_certificate_id
--
-- 【注意事项】
-- - 原 tasks 表的 idx_tasks_assignee_ids USING GIN 索引在 H2 中不支持，已移除
-- - schedule_rules 表原预置数据（rule_sys_001 等）因主键类型变更（VARCHAR→BIGINT）无法保留，
--   需由应用层初始化逻辑重新插入（见脚本末尾说明）
-- - 招聘模块 4 表保留原 created_at/updated_at 字段名以兼容现有代码，仅补全 deleted 字段
-- - schedule_plans.template_id（VARCHAR(32)）仍引用 schedule_templates.template_id（现已改为 BIGINT），
--   存在类型不匹配，因 schedule_plans 表不在本次迁移范围，暂不处理（软外键关系，数据库层面不会报错）
-- ============================================================


-- ============================================================
-- T-007: 10 对内表 DROP + CREATE DDL
-- 主键: BIGINT GENERATED ALWAYS AS IDENTITY
-- ============================================================


-- ------------------------------------------------------------
-- 1. task_sub_tasks（子任务表）
--    依赖: tasks（先 DROP 子表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS task_sub_tasks CASCADE;

CREATE TABLE task_sub_tasks (
    sub_task_id        BIGINT         GENERATED ALWAYS AS IDENTITY,
    task_id            BIGINT         NOT NULL,
    title              VARCHAR(200)   NOT NULL,
    description        TEXT,
    sort_order         INTEGER        NOT NULL DEFAULT 0,
    status             VARCHAR(20)    NOT NULL DEFAULT 'pending',
    completed_at       TIMESTAMP,
    create_time        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_sub_tasks PRIMARY KEY (sub_task_id)
);

COMMENT ON TABLE task_sub_tasks IS '子任务表：任务的细分步骤项';
COMMENT ON COLUMN task_sub_tasks.sub_task_id IS '子任务主键ID（BIGINT GENERATED ALWAYS AS IDENTITY）';
COMMENT ON COLUMN task_sub_tasks.task_id IS '所属任务ID（外键→tasks.task_id，BIGINT）';
COMMENT ON COLUMN task_sub_tasks.title IS '子任务标题';
COMMENT ON COLUMN task_sub_tasks.description IS '子任务描述';
COMMENT ON COLUMN task_sub_tasks.sort_order IS '排序序号';
COMMENT ON COLUMN task_sub_tasks.status IS '子任务状态：pending/in_progress/completed';
COMMENT ON COLUMN task_sub_tasks.completed_at IS '完成时间';

CREATE INDEX idx_task_sub_tasks_task_id ON task_sub_tasks (task_id) WHERE deleted = 0;


-- ------------------------------------------------------------
-- 2. tasks（任务主表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS tasks CASCADE;

CREATE TABLE tasks (
    task_id                BIGINT         GENERATED ALWAYS AS IDENTITY,
    ref_no                 VARCHAR(32)    NOT NULL,
    category               VARCHAR(20)    NOT NULL,
    title                  VARCHAR(200)   NOT NULL,
    description            TEXT,
    publisher_id           VARCHAR(32)    NOT NULL,
    priority               VARCHAR(10)    NOT NULL DEFAULT 'medium',
    deadline               TIMESTAMP,
    status                 VARCHAR(20)    NOT NULL DEFAULT 'draft',
    current_stage          VARCHAR(30),
    stages_config          JSONB,
    sub_tasks_count        INTEGER        NOT NULL DEFAULT 0,
    completed_sub_tasks    INTEGER        NOT NULL DEFAULT 0,
    assignee_ids           JSONB,
    progress               INTEGER        NOT NULL DEFAULT 0,
    create_time            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_tasks PRIMARY KEY (task_id)
);

COMMENT ON TABLE tasks IS '任务主表：存储所有任务的基本信息、状态和工作流阶段';
COMMENT ON COLUMN tasks.task_id IS '任务主键ID（BIGINT GENERATED ALWAYS AS IDENTITY）';
COMMENT ON COLUMN tasks.ref_no IS '任务编号（对外展示）';
COMMENT ON COLUMN tasks.category IS '任务类别：daily/training/business_trip/inventory/assessment';
COMMENT ON COLUMN tasks.title IS '任务标题';
COMMENT ON COLUMN tasks.description IS '任务详细描述';
COMMENT ON COLUMN tasks.publisher_id IS '发布人ID';
COMMENT ON COLUMN tasks.priority IS '优先级：high/medium/low';
COMMENT ON COLUMN tasks.deadline IS '截止时间';
COMMENT ON COLUMN tasks.status IS '任务状态：draft/pending/in_progress/reviewing/completed/overdue/rejected';
COMMENT ON COLUMN tasks.current_stage IS '当前工作流阶段key';
COMMENT ON COLUMN tasks.stages_config IS '工作流阶段配置（JSON）';
COMMENT ON COLUMN tasks.sub_tasks_count IS '子任务总数';
COMMENT ON COLUMN tasks.completed_sub_tasks IS '已完成子任务数';
COMMENT ON COLUMN tasks.assignee_ids IS '接收人ID列表（JSON数组）';
COMMENT ON COLUMN tasks.progress IS '完成进度（0-100）';

CREATE INDEX idx_tasks_publisher_id ON tasks (publisher_id) WHERE deleted = 0;
CREATE INDEX idx_tasks_status ON tasks (status) WHERE deleted = 0;
CREATE INDEX idx_tasks_category ON tasks (category) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_tasks_ref_no ON tasks (ref_no) WHERE deleted = 0;
-- 注意：原 idx_tasks_assignee_ids USING GIN 索引在 H2 中不支持，已移除


-- ------------------------------------------------------------
-- 3. task_templates（任务模板表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS task_templates CASCADE;

CREATE TABLE task_templates (
    template_id          BIGINT         GENERATED ALWAYS AS IDENTITY,
    category             VARCHAR(20)    NOT NULL,
    name                 VARCHAR(100)   NOT NULL,
    description          TEXT,
    default_title        VARCHAR(200),
    default_description  TEXT,
    stages_config        JSONB,
    is_system            BOOLEAN        NOT NULL DEFAULT FALSE,
    create_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_templates PRIMARY KEY (template_id)
);

COMMENT ON TABLE task_templates IS '任务模板表：预定义的任务模板，用于快速创建任务';
COMMENT ON COLUMN task_templates.template_id IS '模板主键ID（BIGINT GENERATED ALWAYS AS IDENTITY）';
COMMENT ON COLUMN task_templates.category IS '适用类别：daily/training/business_trip/inventory/assessment';
COMMENT ON COLUMN task_templates.name IS '模板名称';
COMMENT ON COLUMN task_templates.description IS '模板描述';
COMMENT ON COLUMN task_templates.default_title IS '默认标题模板';
COMMENT ON COLUMN task_templates.default_description IS '默认描述模板';
COMMENT ON COLUMN task_templates.stages_config IS '默认工作流阶段配置（JSON）';
COMMENT ON COLUMN task_templates.is_system IS '是否系统内置模板';

CREATE INDEX idx_task_templates_category ON task_templates (category) WHERE deleted = 0;
CREATE INDEX idx_task_templates_is_system ON task_templates (is_system) WHERE deleted = 0;


-- ------------------------------------------------------------
-- 4. appeal_process_logs（申诉处理日志表）
--    依赖: appeals（不在本次迁移范围）
--    补全: update_time 字段
-- ------------------------------------------------------------
DROP TABLE IF EXISTS appeal_process_logs CASCADE;

CREATE TABLE appeal_process_logs (
    log_id               BIGINT         GENERATED ALWAYS AS IDENTITY,
    appeal_id            VARCHAR(32)    NOT NULL,
    action               VARCHAR(50)    NOT NULL,
    operator_id          VARCHAR(32),
    operator_name        VARCHAR(100),
    comment              TEXT,
    create_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_appeal_process_logs PRIMARY KEY (log_id)
);

COMMENT ON TABLE appeal_process_logs IS '申诉处理日志表：记录每次状态变更';
COMMENT ON COLUMN appeal_process_logs.log_id IS '日志主键ID（BIGINT GENERATED ALWAYS AS IDENTITY）';
COMMENT ON COLUMN appeal_process_logs.appeal_id IS '关联申诉ID（外键→appeals.appeal_id）';
COMMENT ON COLUMN appeal_process_logs.action IS '操作类型：submit/accept/process/resolve/close/withdraw/reject';
COMMENT ON COLUMN appeal_process_logs.operator_id IS '操作人ID';
COMMENT ON COLUMN appeal_process_logs.operator_name IS '操作人姓名';
COMMENT ON COLUMN appeal_process_logs.comment IS '操作备注';

CREATE INDEX idx_appeal_process_logs_appeal_id ON appeal_process_logs (appeal_id) WHERE deleted = 0;
CREATE INDEX idx_appeal_process_logs_create_time ON appeal_process_logs (create_time) WHERE deleted = 0;


-- ------------------------------------------------------------
-- 5. appeal_attachments（申诉附件表）
--    依赖: appeals（不在本次迁移范围）
--    补全: update_time 字段
-- ------------------------------------------------------------
DROP TABLE IF EXISTS appeal_attachments CASCADE;

CREATE TABLE appeal_attachments (
    attachment_id        BIGINT         GENERATED ALWAYS AS IDENTITY,
    appeal_id            VARCHAR(32)    NOT NULL,
    file_name            VARCHAR(200)   NOT NULL,
    file_url             VARCHAR(500)   NOT NULL,
    file_type            VARCHAR(50),
    file_size            BIGINT,
    create_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_appeal_attachments PRIMARY KEY (attachment_id)
);

COMMENT ON TABLE appeal_attachments IS '申诉附件表';
COMMENT ON COLUMN appeal_attachments.attachment_id IS '附件主键ID（BIGINT GENERATED ALWAYS AS IDENTITY）';
COMMENT ON COLUMN appeal_attachments.appeal_id IS '关联申诉ID（外键→appeals.appeal_id）';
COMMENT ON COLUMN appeal_attachments.file_name IS '文件名';
COMMENT ON COLUMN appeal_attachments.file_url IS '文件URL';
COMMENT ON COLUMN appeal_attachments.file_type IS '文件类型';
COMMENT ON COLUMN appeal_attachments.file_size IS '文件大小（字节）';

CREATE INDEX idx_appeal_attachments_appeal_id ON appeal_attachments (appeal_id) WHERE deleted = 0;


-- ------------------------------------------------------------
-- 6. notification_logs（通知记录表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS notification_logs CASCADE;

CREATE TABLE notification_logs (
    log_id               BIGINT         GENERATED ALWAYS AS IDENTITY,
    business_type        VARCHAR(50)    NOT NULL,
    business_id          VARCHAR(32)    NOT NULL,
    title                VARCHAR(200)   NOT NULL,
    content              TEXT           NOT NULL,
    receiver_ids         JSONB          NOT NULL DEFAULT '[]',
    receiver_names       JSONB          DEFAULT '[]',
    channel              VARCHAR(20)    NOT NULL DEFAULT 'in_app',
    send_status          VARCHAR(20)    NOT NULL DEFAULT 'pending',
    send_time            TIMESTAMP,
    retry_count          INTEGER        DEFAULT 0,
    error_message        VARCHAR(500),
    create_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_notification_logs PRIMARY KEY (log_id)
);

COMMENT ON TABLE notification_logs IS '通知记录表';
COMMENT ON COLUMN notification_logs.log_id IS '日志主键ID（BIGINT GENERATED ALWAYS AS IDENTITY）';
COMMENT ON COLUMN notification_logs.business_type IS '业务类型: schedule_published-排班发布 schedule_withdrawn-排班撤回 swap_approved-换班通过 swap_rejected-换班拒绝 daily_reminder-明日提醒';
COMMENT ON COLUMN notification_logs.business_id IS '业务ID(方案ID/换班申请ID等)';
COMMENT ON COLUMN notification_logs.title IS '通知标题';
COMMENT ON COLUMN notification_logs.content IS '通知内容(支持模板变量)';
COMMENT ON COLUMN notification_logs.receiver_ids IS '接收人用户ID数组(JSON)';
COMMENT ON COLUMN notification_logs.receiver_names IS '接收人姓名数组(JSON,冗余)';
COMMENT ON COLUMN notification_logs.channel IS '发送渠道: in_app-站内信(本期) sms-短信 email-邮件 wechat_work-企微(预留)';
COMMENT ON COLUMN notification_logs.send_status IS '发送状态: pending-待发送 sending-发送中 success-成功 failed-失败';
COMMENT ON COLUMN notification_logs.send_time IS '实际发送时间';
COMMENT ON COLUMN notification_logs.retry_count IS '重试次数';
COMMENT ON COLUMN notification_logs.error_message IS '错误信息(失败时记录)';

CREATE INDEX idx_notification_logs_business ON notification_logs (business_type, business_id);
CREATE INDEX idx_notification_logs_status ON notification_logs (send_status);
CREATE INDEX idx_notification_logs_create_time ON notification_logs (create_time);
CREATE INDEX idx_notification_logs_deleted ON notification_logs (deleted);

ALTER TABLE notification_logs ADD CONSTRAINT chk_notification_business_type
    CHECK (business_type IN ('schedule_published', 'schedule_withdrawn', 'swap_approved', 'swap_rejected', 'daily_reminder'));
ALTER TABLE notification_logs ADD CONSTRAINT chk_notification_channel
    CHECK (channel IN ('in_app', 'sms', 'email', 'wechat_work'));
ALTER TABLE notification_logs ADD CONSTRAINT chk_notification_send_status
    CHECK (send_status IN ('pending', 'sending', 'success', 'failed'));


-- ------------------------------------------------------------
-- 7. schedule_rules（排班规则配置表）
--    注意: 原预置数据 rule_sys_001 等因主键类型变更无法保留
-- ------------------------------------------------------------
DROP TABLE IF EXISTS schedule_rules CASCADE;

CREATE TABLE schedule_rules (
    rule_id              BIGINT         GENERATED ALWAYS AS IDENTITY,
    rule_code            VARCHAR(50)    NOT NULL,
    rule_name            VARCHAR(100)   NOT NULL,
    description          TEXT,
    store_id             BIGINT         NOT NULL,
    category             VARCHAR(30)    NOT NULL,
    parameters           JSONB          NOT NULL DEFAULT '{}',
    priority             INTEGER        NOT NULL DEFAULT 100,
    status               VARCHAR(20)    NOT NULL DEFAULT 'active',
    is_system            BOOLEAN        NOT NULL DEFAULT FALSE,
    create_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_schedule_rules PRIMARY KEY (rule_id),
    CONSTRAINT uk_schedule_rules_rule_code UNIQUE (store_id, rule_code)
);

COMMENT ON TABLE schedule_rules IS '排班规则配置表';
COMMENT ON COLUMN schedule_rules.rule_id IS '规则主键ID（BIGINT GENERATED ALWAYS AS IDENTITY）';
COMMENT ON COLUMN schedule_rules.rule_code IS '规则编码: consecutive_days/rest_time/weekly_rest/duplicate_shift/on_leave_check/resigned_check/staff_shortage';
COMMENT ON COLUMN schedule_rules.rule_name IS '规则名称';
COMMENT ON COLUMN schedule_rules.description IS '规则描述';
COMMENT ON COLUMN schedule_rules.store_id IS '门店ID(规则按门店配置)';
COMMENT ON COLUMN schedule_rules.category IS '规则分类: hard_constraint-硬约束(必须满足) soft_constraint-软约束(建议满足)';
COMMENT ON COLUMN schedule_rules.parameters IS '规则参数(JSON): 根据 rule_code 不同而不同';
COMMENT ON COLUMN schedule_rules.priority IS '优先级(数值越小越优先)';
COMMENT ON COLUMN schedule_rules.status IS '状态: active-启用 inactive-停用';
COMMENT ON COLUMN schedule_rules.is_system IS '是否系统预置规则(不可删除)';

CREATE INDEX idx_schedule_rules_store ON schedule_rules (store_id);
CREATE INDEX idx_schedule_rules_store_category ON schedule_rules (store_id, category);
CREATE INDEX idx_schedule_rules_store_status ON schedule_rules (store_id, status);
CREATE INDEX idx_schedule_rules_priority ON schedule_rules (priority);
CREATE INDEX idx_schedule_rules_deleted ON schedule_rules (deleted);

ALTER TABLE schedule_rules ADD CONSTRAINT chk_schedule_rules_category
    CHECK (category IN ('hard_constraint', 'soft_constraint'));
ALTER TABLE schedule_rules ADD CONSTRAINT chk_schedule_rules_status
    CHECK (status IN ('active', 'inactive'));


-- ------------------------------------------------------------
-- 8. schedule_shift_types（班次类型配置表）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS schedule_shift_types CASCADE;

CREATE TABLE schedule_shift_types (
    shift_type_id        BIGINT         GENERATED ALWAYS AS IDENTITY,
    shift_code           VARCHAR(30)    NOT NULL,
    shift_name           VARCHAR(50)    NOT NULL,
    store_id             BIGINT         NOT NULL,
    start_time           TIME           NOT NULL,
    end_time             TIME           NOT NULL,
    color                VARCHAR(20)    DEFAULT '#409EFF',
    icon                 VARCHAR(50),
    duration_minutes     INTEGER        NOT NULL,
    is_rest              BOOLEAN        NOT NULL DEFAULT FALSE,
    sort_order           INTEGER        DEFAULT 0,
    status               VARCHAR(20)    NOT NULL DEFAULT 'active',
    create_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_schedule_shift_types PRIMARY KEY (shift_type_id),
    CONSTRAINT uk_schedule_shift_types_shift_code UNIQUE (store_id, shift_code)
);

COMMENT ON TABLE schedule_shift_types IS '班次类型配置表';
COMMENT ON COLUMN schedule_shift_types.shift_type_id IS '班次类型主键ID（BIGINT GENERATED ALWAYS AS IDENTITY）';
COMMENT ON COLUMN schedule_shift_types.shift_code IS '班次编码: morning-早班 noon-中班 evening-晚班 night_off-休息';
COMMENT ON COLUMN schedule_shift_types.shift_name IS '班次显示名称(如"早班(A)")';
COMMENT ON COLUMN schedule_shift_types.store_id IS '门店ID(班次按门店配置)';
COMMENT ON COLUMN schedule_shift_types.start_time IS '开始时间(HH:mm格式)';
COMMENT ON COLUMN schedule_shift_types.end_time IS '结束时间(HH:mm格式,跨夜用次日时间如01:00)';
COMMENT ON COLUMN schedule_shift_types.color IS '颜色标识(CSS颜色值或--fts-*变量名)';
COMMENT ON COLUMN schedule_shift_types.icon IS '图标(可选)';
COMMENT ON COLUMN schedule_shift_types.duration_minutes IS '持续时长(分钟,自动计算)';
COMMENT ON COLUMN schedule_shift_types.is_rest IS '是否休息班次(休息不计工时)';
COMMENT ON COLUMN schedule_shift_types.sort_order IS '显示顺序';
COMMENT ON COLUMN schedule_shift_types.status IS '状态: active-启用 inactive-停用';

CREATE INDEX idx_schedule_shift_types_store ON schedule_shift_types (store_id);
CREATE INDEX idx_schedule_shift_types_store_status ON schedule_shift_types (store_id, status);
CREATE INDEX idx_schedule_shift_types_sort ON schedule_shift_types (store_id, sort_order);
CREATE INDEX idx_schedule_shift_types_deleted ON schedule_shift_types (deleted);

ALTER TABLE schedule_shift_types ADD CONSTRAINT chk_schedule_shift_types_status
    CHECK (status IN ('active', 'inactive'));


-- ------------------------------------------------------------
-- 9. schedule_templates（排班模板表）
--    注意: schedule_plans.template_id 仍为 VARCHAR(32)，存在类型不匹配（软外键）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS schedule_templates CASCADE;

CREATE TABLE schedule_templates (
    template_id          BIGINT         GENERATED ALWAYS AS IDENTITY,
    template_name        VARCHAR(100)   NOT NULL,
    description          TEXT,
    store_id             BIGINT         NOT NULL,
    demand_matrix        JSONB          NOT NULL DEFAULT '{}',
    enabled_rule_ids     JSONB          DEFAULT '[]',
    is_default           BOOLEAN        NOT NULL DEFAULT FALSE,
    status               VARCHAR(20)    NOT NULL DEFAULT 'active',
    use_count            INTEGER        DEFAULT 0,
    create_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_schedule_templates PRIMARY KEY (template_id)
);

COMMENT ON TABLE schedule_templates IS '排班模板表';
COMMENT ON COLUMN schedule_templates.template_id IS '模板主键ID（BIGINT GENERATED ALWAYS AS IDENTITY）';
COMMENT ON COLUMN schedule_templates.template_name IS '模板名称';
COMMENT ON COLUMN schedule_templates.description IS '模板描述';
COMMENT ON COLUMN schedule_templates.store_id IS '门店ID(模板按门店隔离)';
COMMENT ON COLUMN schedule_templates.demand_matrix IS '时段需求矩阵(JSON): {"weekday":{"morning":N,"noon":N,"evening":N},"weekend":{...},"holiday":{...}}';
COMMENT ON COLUMN schedule_templates.enabled_rule_ids IS '启用的规则ID列表(JSON数组)';
COMMENT ON COLUMN schedule_templates.is_default IS '是否默认模板(每个门店仅一个)';
COMMENT ON COLUMN schedule_templates.status IS '状态: active-启用 inactive-停用';
COMMENT ON COLUMN schedule_templates.use_count IS '被使用次数';

CREATE INDEX idx_schedule_templates_store ON schedule_templates (store_id);
CREATE INDEX idx_schedule_templates_store_status ON schedule_templates (store_id, status);
CREATE INDEX idx_schedule_templates_default ON schedule_templates (store_id, is_default);
CREATE INDEX idx_schedule_templates_deleted ON schedule_templates (deleted);

ALTER TABLE schedule_templates ADD CONSTRAINT chk_schedule_templates_status
    CHECK (status IN ('active', 'inactive'));


-- ------------------------------------------------------------
-- 10. health_certificate_expense（健康证报销表）
--     规范化: create_time/update_time 从 date 改为 TIMESTAMP NOT NULL
--     补全: deleted 字段
-- ------------------------------------------------------------
DROP TABLE IF EXISTS health_certificate_expense CASCADE;

CREATE TABLE health_certificate_expense (
    id                       BIGINT         GENERATED ALWAYS AS IDENTITY,
    health_certificate_id    VARCHAR(32)    NOT NULL,
    employee_id              VARCHAR(36)    NOT NULL,
    employee_name            VARCHAR(100)   NOT NULL,
    store                    VARCHAR(100)   NOT NULL,
    amount                   NUMERIC(10,2)  NOT NULL,
    invoice_type             VARCHAR(50),
    invoice_number           VARCHAR(50),
    invoice_date             DATE,
    invoice_attachments      TEXT,
    reason                   VARCHAR(500)   NOT NULL,
    note                     VARCHAR(500),
    status                   VARCHAR(20)    NOT NULL,
    apply_date               DATE           NOT NULL,
    approve_date             DATE,
    reimburse_date           DATE,
    reject_reason            VARCHAR(500),
    expense_type             VARCHAR(20)    NOT NULL,
    approval_history         TEXT,
    creator                  VARCHAR(100),
    updater                  VARCHAR(100),
    create_time              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_health_certificate_expense PRIMARY KEY (id)
);

COMMENT ON TABLE health_certificate_expense IS '健康证报销表';
COMMENT ON COLUMN health_certificate_expense.id IS '报销主键ID（BIGINT GENERATED ALWAYS AS IDENTITY）';
COMMENT ON COLUMN health_certificate_expense.health_certificate_id IS '关联健康证ID（外键→health_certificate.id，VARCHAR(32)）';
COMMENT ON COLUMN health_certificate_expense.employee_id IS '员工ID';
COMMENT ON COLUMN health_certificate_expense.employee_name IS '员工姓名';
COMMENT ON COLUMN health_certificate_expense.store IS '所属门店';
COMMENT ON COLUMN health_certificate_expense.amount IS '报销金额';
COMMENT ON COLUMN health_certificate_expense.invoice_type IS '发票类型';
COMMENT ON COLUMN health_certificate_expense.invoice_number IS '发票号码';
COMMENT ON COLUMN health_certificate_expense.invoice_date IS '发票日期';
COMMENT ON COLUMN health_certificate_expense.invoice_attachments IS '发票附件路径列表';
COMMENT ON COLUMN health_certificate_expense.reason IS '报销事由';
COMMENT ON COLUMN health_certificate_expense.note IS '备注';
COMMENT ON COLUMN health_certificate_expense.status IS '报销状态: pending-待审批 approved-已审批 reimbursed-已报销 rejected-已拒绝';
COMMENT ON COLUMN health_certificate_expense.apply_date IS '申请日期';
COMMENT ON COLUMN health_certificate_expense.approve_date IS '审批日期';
COMMENT ON COLUMN health_certificate_expense.reimburse_date IS '报销日期';
COMMENT ON COLUMN health_certificate_expense.reject_reason IS '拒绝原因';
COMMENT ON COLUMN health_certificate_expense.expense_type IS '报销类型: new-新办 renewal-续期';
COMMENT ON COLUMN health_certificate_expense.approval_history IS '审批历史(JSON格式)';
COMMENT ON COLUMN health_certificate_expense.creator IS '创建人';
COMMENT ON COLUMN health_certificate_expense.updater IS '最后更新人';

CREATE INDEX idx_health_certificate_expense_health_certificate_id ON health_certificate_expense (health_certificate_id);
CREATE INDEX idx_health_certificate_expense_employee_id ON health_certificate_expense (employee_id);
CREATE INDEX idx_health_certificate_expense_store ON health_certificate_expense (store);
CREATE INDEX idx_health_certificate_expense_status ON health_certificate_expense (status);
CREATE INDEX idx_health_certificate_expense_apply_date ON health_certificate_expense (apply_date);


-- ============================================================
-- T-008: 6 对外表 DROP + CREATE DDL
-- 主键: VARCHAR(32) 雪花算法
-- ============================================================


-- ------------------------------------------------------------
-- 1. recruitment_requirements（招聘需求表）
--    保留: created_at/updated_at 字段（兼容现有代码）
--    补全: deleted 字段
-- ------------------------------------------------------------
DROP TABLE IF EXISTS recruitment_requirements CASCADE;

CREATE TABLE recruitment_requirements (
    id                   VARCHAR(32)    NOT NULL,
    requirement_code     VARCHAR(50)    NOT NULL,
    position_name        VARCHAR(100)   NOT NULL,
    department_id        VARCHAR(36),
    department_name      VARCHAR(100),
    store_id             VARCHAR(36),
    store_name           VARCHAR(100),
    position_id          VARCHAR(36),
    requirement_num      INTEGER        NOT NULL DEFAULT 1,
    type                 VARCHAR(20)    NOT NULL DEFAULT 'hr',
    status               VARCHAR(20)    NOT NULL DEFAULT 'open',
    approval_status      VARCHAR(20)    DEFAULT 'approved',
    description          TEXT,
    requirements         TEXT,
    salary_range         VARCHAR(50),
    work_location        VARCHAR(200),
    apply_deadline       TIMESTAMP,
    created_by           VARCHAR(36),
    created_by_name      VARCHAR(50),
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_recruitment_requirements PRIMARY KEY (id)
);

COMMENT ON TABLE recruitment_requirements IS '招聘需求表';
COMMENT ON COLUMN recruitment_requirements.id IS '主键ID（VARCHAR(32) 雪花算法）';
COMMENT ON COLUMN recruitment_requirements.requirement_code IS '需求编号';
COMMENT ON COLUMN recruitment_requirements.position_name IS '职位名称';
COMMENT ON COLUMN recruitment_requirements.department_id IS '部门ID';
COMMENT ON COLUMN recruitment_requirements.department_name IS '部门名称';
COMMENT ON COLUMN recruitment_requirements.store_id IS '门店ID';
COMMENT ON COLUMN recruitment_requirements.store_name IS '门店名称';
COMMENT ON COLUMN recruitment_requirements.position_id IS '职位ID';
COMMENT ON COLUMN recruitment_requirements.requirement_num IS '需求人数';
COMMENT ON COLUMN recruitment_requirements.type IS '需求类型: hr-人事发布 store-门店提报';
COMMENT ON COLUMN recruitment_requirements.status IS '状态: open-进行中 closed-已关闭 filled-已招满';
COMMENT ON COLUMN recruitment_requirements.approval_status IS '审批状态: pending-待审批 approved-已通过 rejected-已拒绝';
COMMENT ON COLUMN recruitment_requirements.description IS '职位描述';
COMMENT ON COLUMN recruitment_requirements.requirements IS '任职要求';
COMMENT ON COLUMN recruitment_requirements.salary_range IS '薪资范围';
COMMENT ON COLUMN recruitment_requirements.work_location IS '工作地点';
COMMENT ON COLUMN recruitment_requirements.apply_deadline IS '申请截止时间';
COMMENT ON COLUMN recruitment_requirements.created_by IS '创建人ID';
COMMENT ON COLUMN recruitment_requirements.created_by_name IS '创建人姓名';
COMMENT ON COLUMN recruitment_requirements.created_at IS '创建时间';
COMMENT ON COLUMN recruitment_requirements.updated_at IS '更新时间';

CREATE UNIQUE INDEX uk_recruitment_requirements_requirement_code ON recruitment_requirements (requirement_code);
CREATE INDEX idx_recruitment_requirements_department_id ON recruitment_requirements (department_id);
CREATE INDEX idx_recruitment_requirements_store_id ON recruitment_requirements (store_id);
CREATE INDEX idx_recruitment_requirements_status ON recruitment_requirements (status);
CREATE INDEX idx_recruitment_requirements_type ON recruitment_requirements (type);


-- ------------------------------------------------------------
-- 2. resumes（简历表）
--    保留: created_at/updated_at 字段（兼容现有代码）
--    补全: deleted 字段
--    外键类型统一: requirement_id VARCHAR(36) → VARCHAR(32)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS resumes CASCADE;

CREATE TABLE resumes (
    id                   VARCHAR(32)    NOT NULL,
    resume_code          VARCHAR(50)    NOT NULL,
    requirement_id       VARCHAR(32),
    candidate_name       VARCHAR(50)    NOT NULL,
    gender               VARCHAR(10),
    phone                VARCHAR(20)    NOT NULL,
    email                VARCHAR(100),
    age                  INTEGER,
    education            VARCHAR(50),
    work_experience      INTEGER,
    current_salary       VARCHAR(50),
    expected_salary      VARCHAR(50),
    skills               TEXT,
    experience           TEXT,
    education_detail     TEXT,
    self_introduction    TEXT,
    attachment_url       VARCHAR(500),
    status               VARCHAR(20)    NOT NULL DEFAULT 'pending',
    source               VARCHAR(20)    DEFAULT 'online',
    created_by           VARCHAR(36),
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_resumes PRIMARY KEY (id)
);

COMMENT ON TABLE resumes IS '简历表';
COMMENT ON COLUMN resumes.id IS '主键ID（VARCHAR(32) 雪花算法）';
COMMENT ON COLUMN resumes.resume_code IS '简历编号';
COMMENT ON COLUMN resumes.requirement_id IS '关联的招聘需求ID（外键→recruitment_requirements.id）';
COMMENT ON COLUMN resumes.candidate_name IS '候选人姓名';
COMMENT ON COLUMN resumes.gender IS '性别';
COMMENT ON COLUMN resumes.phone IS '联系电话';
COMMENT ON COLUMN resumes.email IS '邮箱';
COMMENT ON COLUMN resumes.age IS '年龄';
COMMENT ON COLUMN resumes.education IS '学历';
COMMENT ON COLUMN resumes.work_experience IS '工作年限';
COMMENT ON COLUMN resumes.current_salary IS '当前薪资';
COMMENT ON COLUMN resumes.expected_salary IS '期望薪资';
COMMENT ON COLUMN resumes.skills IS '技能描述';
COMMENT ON COLUMN resumes.experience IS '工作经历';
COMMENT ON COLUMN resumes.education_detail IS '教育经历';
COMMENT ON COLUMN resumes.self_introduction IS '自我介绍';
COMMENT ON COLUMN resumes.attachment_url IS '简历附件URL';
COMMENT ON COLUMN resumes.status IS '状态: pending-待处理 reviewing-筛选中 interviewed-已面试 offered-已发offer rejected-已拒绝 hired-已录用';
COMMENT ON COLUMN resumes.source IS '来源: online-在线投递 referral-内部推荐 headhunter-猎头';
COMMENT ON COLUMN resumes.created_by IS '创建人ID';
COMMENT ON COLUMN resumes.created_at IS '创建时间';
COMMENT ON COLUMN resumes.updated_at IS '更新时间';

CREATE UNIQUE INDEX uk_resumes_resume_code ON resumes (resume_code);
CREATE INDEX idx_resumes_requirement_id ON resumes (requirement_id);
CREATE INDEX idx_resumes_phone ON resumes (phone);
CREATE INDEX idx_resumes_status ON resumes (status);


-- ------------------------------------------------------------
-- 3. interviews（面试记录表）
--    保留: created_at/updated_at 字段（兼容现有代码）
--    补全: deleted 字段
--    外键类型统一: resume_id/requirement_id VARCHAR(36) → VARCHAR(32)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS interviews CASCADE;

CREATE TABLE interviews (
    id                   VARCHAR(32)    NOT NULL,
    interview_code       VARCHAR(50)    NOT NULL,
    resume_id            VARCHAR(32)    NOT NULL,
    requirement_id       VARCHAR(32),
    interview_round      VARCHAR(20)    NOT NULL,
    interview_type       VARCHAR(20)    DEFAULT 'offline',
    interview_date       TIMESTAMP      NOT NULL,
    interview_location   VARCHAR(200),
    interviewer_id       VARCHAR(36),
    interviewer_name     VARCHAR(50),
    status               VARCHAR(20)    NOT NULL DEFAULT 'scheduled',
    result               VARCHAR(20),
    score                INTEGER,
    feedback             TEXT,
    notes                TEXT,
    created_by           VARCHAR(36),
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_interviews PRIMARY KEY (id)
);

COMMENT ON TABLE interviews IS '面试记录表';
COMMENT ON COLUMN interviews.id IS '主键ID（VARCHAR(32) 雪花算法）';
COMMENT ON COLUMN interviews.interview_code IS '面试编号';
COMMENT ON COLUMN interviews.resume_id IS '简历ID（外键→resumes.id）';
COMMENT ON COLUMN interviews.requirement_id IS '招聘需求ID（外键→recruitment_requirements.id）';
COMMENT ON COLUMN interviews.interview_round IS '面试轮次: first-初试 second-复试 final-终试';
COMMENT ON COLUMN interviews.interview_type IS '面试类型: online-线上 offline-线下';
COMMENT ON COLUMN interviews.interview_date IS '面试时间';
COMMENT ON COLUMN interviews.interview_location IS '面试地点';
COMMENT ON COLUMN interviews.interviewer_id IS '面试官ID';
COMMENT ON COLUMN interviews.interviewer_name IS '面试官姓名';
COMMENT ON COLUMN interviews.status IS '状态: scheduled-已安排 completed-已完成 cancelled-已取消';
COMMENT ON COLUMN interviews.result IS '面试结果: pass-通过 fail-未通过 pending-待定';
COMMENT ON COLUMN interviews.score IS '面试分数';
COMMENT ON COLUMN interviews.feedback IS '面试反馈';
COMMENT ON COLUMN interviews.notes IS '备注';
COMMENT ON COLUMN interviews.created_by IS '创建人ID';
COMMENT ON COLUMN interviews.created_at IS '创建时间';
COMMENT ON COLUMN interviews.updated_at IS '更新时间';

CREATE UNIQUE INDEX uk_interviews_interview_code ON interviews (interview_code);
CREATE INDEX idx_interviews_resume_id ON interviews (resume_id);
CREATE INDEX idx_interviews_requirement_id ON interviews (requirement_id);
CREATE INDEX idx_interviews_interviewer_id ON interviews (interviewer_id);
CREATE INDEX idx_interviews_status ON interviews (status);


-- ------------------------------------------------------------
-- 4. onboarding_records（入职记录表）
--    保留: created_at/updated_at 字段（兼容现有代码）
--    补全: deleted 字段
--    外键类型统一: resume_id/requirement_id VARCHAR(36) → VARCHAR(32)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS onboarding_records CASCADE;

CREATE TABLE onboarding_records (
    id                        VARCHAR(32)    NOT NULL,
    onboarding_code           VARCHAR(50)    NOT NULL,
    resume_id                 VARCHAR(32)    NOT NULL,
    requirement_id            VARCHAR(32),
    employee_id               VARCHAR(36),
    employee_code             VARCHAR(50),
    candidate_name            VARCHAR(50)    NOT NULL,
    phone                     VARCHAR(20)    NOT NULL,
    department_id             VARCHAR(36),
    department_name           VARCHAR(100),
    store_id                  VARCHAR(36),
    store_name                VARCHAR(100),
    position_id               VARCHAR(36),
    position_name             VARCHAR(100),
    hire_date                 DATE           NOT NULL,
    probation_start           DATE,
    probation_end             DATE,
    probation_months          INTEGER,
    salary                    NUMERIC(10,2),
    registration_code         VARCHAR(50),
    registration_code_status  VARCHAR(20)    DEFAULT 'UNUSED',
    status                    VARCHAR(20)    NOT NULL DEFAULT 'pending',
    notes                     TEXT,
    created_by                VARCHAR(36),
    created_by_name           VARCHAR(50),
    created_at                TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                   INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_onboarding_records PRIMARY KEY (id)
);

COMMENT ON TABLE onboarding_records IS '入职记录表';
COMMENT ON COLUMN onboarding_records.id IS '主键ID（VARCHAR(32) 雪花算法）';
COMMENT ON COLUMN onboarding_records.onboarding_code IS '入职编号';
COMMENT ON COLUMN onboarding_records.resume_id IS '简历ID（外键→resumes.id）';
COMMENT ON COLUMN onboarding_records.requirement_id IS '招聘需求ID（外键→recruitment_requirements.id）';
COMMENT ON COLUMN onboarding_records.employee_id IS '员工ID';
COMMENT ON COLUMN onboarding_records.employee_code IS '员工编码';
COMMENT ON COLUMN onboarding_records.candidate_name IS '候选人姓名';
COMMENT ON COLUMN onboarding_records.phone IS '联系电话';
COMMENT ON COLUMN onboarding_records.department_id IS '部门ID';
COMMENT ON COLUMN onboarding_records.department_name IS '部门名称';
COMMENT ON COLUMN onboarding_records.store_id IS '门店ID';
COMMENT ON COLUMN onboarding_records.store_name IS '门店名称';
COMMENT ON COLUMN onboarding_records.position_id IS '职位ID';
COMMENT ON COLUMN onboarding_records.position_name IS '职位名称';
COMMENT ON COLUMN onboarding_records.hire_date IS '入职日期';
COMMENT ON COLUMN onboarding_records.probation_start IS '试用期开始日期';
COMMENT ON COLUMN onboarding_records.probation_end IS '试用期结束日期';
COMMENT ON COLUMN onboarding_records.probation_months IS '试用期月数';
COMMENT ON COLUMN onboarding_records.salary IS '薪资';
COMMENT ON COLUMN onboarding_records.registration_code IS '注册码';
COMMENT ON COLUMN onboarding_records.registration_code_status IS '注册码状态: UNUSED-未使用 USED-已使用 EXPIRED-已过期';
COMMENT ON COLUMN onboarding_records.status IS '状态: pending-待入职 onboarding-入职中 completed-已完成 cancelled-已取消';
COMMENT ON COLUMN onboarding_records.notes IS '备注';
COMMENT ON COLUMN onboarding_records.created_by IS '创建人ID';
COMMENT ON COLUMN onboarding_records.created_by_name IS '创建人姓名';
COMMENT ON COLUMN onboarding_records.created_at IS '创建时间';
COMMENT ON COLUMN onboarding_records.updated_at IS '更新时间';

CREATE UNIQUE INDEX uk_onboarding_records_onboarding_code ON onboarding_records (onboarding_code);
CREATE INDEX idx_onboarding_records_resume_id ON onboarding_records (resume_id);
CREATE INDEX idx_onboarding_records_employee_id ON onboarding_records (employee_id);
CREATE INDEX idx_onboarding_records_registration_code ON onboarding_records (registration_code);
CREATE INDEX idx_onboarding_records_status ON onboarding_records (status);


-- ------------------------------------------------------------
-- 5. health_certificate（健康证表）
--    关键变更: last_expense_id VARCHAR(36) → BIGINT
--    补全: create_time / update_time / deleted 三字段
-- ------------------------------------------------------------
DROP TABLE IF EXISTS health_certificate CASCADE;

CREATE TABLE health_certificate (
    id                       VARCHAR(32)    NOT NULL,
    employee_id              VARCHAR(36)    NOT NULL,
    employee_name            VARCHAR(100)   NOT NULL,
    store                    VARCHAR(100)   NOT NULL,
    certificate_number       VARCHAR(50)    NOT NULL,
    issue_date               DATE           NOT NULL,
    expiry_date              DATE           NOT NULL,
    status                   VARCHAR(20)    NOT NULL,
    issuer                   VARCHAR(100)   NOT NULL,
    note                     VARCHAR(500),
    expense_status           VARCHAR(20),
    approval_status          VARCHAR(20)    DEFAULT 'pending',
    approval_by              VARCHAR(100),
    approval_date            DATE,
    reject_reason            VARCHAR(500),
    submission_date          DATE,
    submitted_by             VARCHAR(100),
    certificate_image        VARCHAR(255),
    last_expense_id          BIGINT,
    operator                 VARCHAR(100),
    operation_time           DATE,
    expiry_days              INTEGER,
    create_time              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_health_certificate PRIMARY KEY (id)
);

COMMENT ON TABLE health_certificate IS '健康证表';
COMMENT ON COLUMN health_certificate.id IS '健康证主键ID（VARCHAR(32) 雪花算法）';
COMMENT ON COLUMN health_certificate.employee_id IS '员工ID';
COMMENT ON COLUMN health_certificate.employee_name IS '员工姓名';
COMMENT ON COLUMN health_certificate.store IS '所属门店';
COMMENT ON COLUMN health_certificate.certificate_number IS '健康证号';
COMMENT ON COLUMN health_certificate.issue_date IS '签发日期';
COMMENT ON COLUMN health_certificate.expiry_date IS '到期日期';
COMMENT ON COLUMN health_certificate.status IS '状态: valid-有效 expiring-即将过期 expired-已过期';
COMMENT ON COLUMN health_certificate.issuer IS '签发机构';
COMMENT ON COLUMN health_certificate.note IS '备注';
COMMENT ON COLUMN health_certificate.expense_status IS '报销状态: pending-待审批 approved-已审批 reimbursed-已报销 rejected-已拒绝';
COMMENT ON COLUMN health_certificate.approval_status IS '审核状态: pending-待审核 approved-已通过 rejected-已拒绝 draft-草稿';
COMMENT ON COLUMN health_certificate.approval_by IS '审核人';
COMMENT ON COLUMN health_certificate.approval_date IS '审核日期';
COMMENT ON COLUMN health_certificate.reject_reason IS '拒绝原因';
COMMENT ON COLUMN health_certificate.submission_date IS '提交日期';
COMMENT ON COLUMN health_certificate.submitted_by IS '提交人';
COMMENT ON COLUMN health_certificate.certificate_image IS '健康证图片路径';
COMMENT ON COLUMN health_certificate.last_expense_id IS '最后一次报销ID（外键→health_certificate_expense.id，BIGINT）';
COMMENT ON COLUMN health_certificate.operator IS '操作人';
COMMENT ON COLUMN health_certificate.operation_time IS '操作时间';
COMMENT ON COLUMN health_certificate.expiry_days IS '剩余天数';

CREATE INDEX idx_health_certificate_employee_id ON health_certificate (employee_id);
CREATE INDEX idx_health_certificate_store ON health_certificate (store);
CREATE INDEX idx_health_certificate_status ON health_certificate (status);
CREATE INDEX idx_health_certificate_expiry_date ON health_certificate (expiry_date);
CREATE INDEX idx_health_certificate_approval_status ON health_certificate (approval_status);


-- ------------------------------------------------------------
-- 6. invoice（发票表）
--    字段来源: Invoice.java Entity 反推
--    补全: create_time / update_time / deleted 三字段
--    外键类型统一: health_certificate_id VARCHAR(36) → VARCHAR(32)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS invoice CASCADE;

CREATE TABLE invoice (
    id                       VARCHAR(32)    NOT NULL,
    health_certificate_id    VARCHAR(32),
    invoice_number           VARCHAR(50),
    invoice_amount           NUMERIC(10,2),
    invoice_date             DATE,
    invoice_type             VARCHAR(50),
    invoice_image            VARCHAR(255),
    approval_status          VARCHAR(20),
    approved_by              VARCHAR(100),
    approved_date            DATE,
    reject_reason            VARCHAR(500),
    operator                 VARCHAR(100),
    operation_time           DATE,
    create_time              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_invoice PRIMARY KEY (id)
);

COMMENT ON TABLE invoice IS '发票表：存储健康证报销相关的发票信息';
COMMENT ON COLUMN invoice.id IS '发票主键ID（VARCHAR(32) 雪花算法）';
COMMENT ON COLUMN invoice.health_certificate_id IS '健康证ID（外键→health_certificate.id）';
COMMENT ON COLUMN invoice.invoice_number IS '发票号码';
COMMENT ON COLUMN invoice.invoice_amount IS '发票金额';
COMMENT ON COLUMN invoice.invoice_date IS '发票日期';
COMMENT ON COLUMN invoice.invoice_type IS '发票类型: 增值税普通发票/增值税专用发票/其他发票';
COMMENT ON COLUMN invoice.invoice_image IS '发票图片路径';
COMMENT ON COLUMN invoice.approval_status IS '发票审核状态: pending-待审核 approved-已通过 rejected-已拒绝';
COMMENT ON COLUMN invoice.approved_by IS '审核人';
COMMENT ON COLUMN invoice.approved_date IS '审核日期';
COMMENT ON COLUMN invoice.reject_reason IS '拒绝原因';
COMMENT ON COLUMN invoice.operator IS '操作人';
COMMENT ON COLUMN invoice.operation_time IS '操作时间';

CREATE INDEX idx_invoice_health_certificate_id ON invoice (health_certificate_id);
CREATE INDEX idx_invoice_invoice_number ON invoice (invoice_number);
CREATE INDEX idx_invoice_approval_status ON invoice (approval_status);


-- ============================================================
-- Part C: 关联外键字段类型调整（3 处 String → BIGINT）
-- ============================================================
-- 说明：以下 3 张表不在重建清单内（主键保持原类型），但存在引用对内表主键的外键字段，
-- 需同步调整为 BIGINT 以保证外键类型与主键类型一致。
-- ============================================================

-- 1. schedule_plans.template_id: VARCHAR(32) → BIGINT（关联 schedule_templates.template_id）
--    schedule_plans 表本身是对外表（plan_id VARCHAR(32) 雪花），仅 template_id 字段需调整
ALTER TABLE schedule_plans ALTER COLUMN template_id SET DATA TYPE BIGINT USING NULL;

COMMENT ON COLUMN schedule_plans.template_id IS '使用的模板ID（外键→schedule_templates.template_id，BIGINT）';


-- ============================================================
-- 迁移后说明
-- ============================================================
-- 1. schedule_rules 表原预置数据（rule_sys_001 ~ rule_sys_006）因主键类型从 VARCHAR(32)
--    变更为 BIGINT GENERATED ALWAYS AS IDENTITY，无法保留原字符串 ID。
--    建议由应用层初始化逻辑（如 DataInitializer）重新插入预置规则，
--    插入时不指定 rule_id，由数据库自动生成 BIGINT ID。
--
-- 2. 招聘模块 4 表（recruitment_requirements / resumes / interviews / onboarding_records）
--    保留原 created_at/updated_at 字段名以兼容现有 Java 代码。建议后续统一为
--    create_time/update_time 以符合项目规范。
--
-- 3. 本脚本采用 DROP + CREATE 方式，会清空所有数据。仅适用于开发环境。
--    生产环境迁移需另行编写数据迁移脚本。
-- ============================================================
