-- ============================================
-- 任务系统表结构
-- 包含：任务主表、子任务表、任务模板表
-- 版本: V20260608
-- ============================================

-- ============================================
-- 任务主表
-- ============================================
CREATE TABLE tasks (
    task_id                VARCHAR(32)    NOT NULL,
    ref_no                 VARCHAR(32)    NOT NULL,
    category               VARCHAR(20)    NOT NULL,  -- daily/training/business_trip/inventory/assessment
    title                  VARCHAR(200)   NOT NULL,
    description            TEXT,
    publisher_id           VARCHAR(32)    NOT NULL,
    priority               VARCHAR(10)    NOT NULL DEFAULT 'medium',  -- high/medium/low
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
COMMENT ON COLUMN tasks.task_id IS '任务主键ID';
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

-- 索引
CREATE INDEX idx_tasks_publisher_id ON tasks (publisher_id) WHERE deleted = 0;
CREATE INDEX idx_tasks_status ON tasks (status) WHERE deleted = 0;
CREATE INDEX idx_tasks_category ON tasks (category) WHERE deleted = 0;
CREATE INDEX idx_tasks_assignee_ids ON tasks USING GIN (assignee_ids) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_tasks_ref_no ON tasks (ref_no) WHERE deleted = 0;

-- ============================================
-- 子任务表
-- ============================================
CREATE TABLE task_sub_tasks (
    sub_task_id        VARCHAR(32)    NOT NULL,
    task_id            VARCHAR(32)    NOT NULL,
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
COMMENT ON COLUMN task_sub_tasks.sub_task_id IS '子任务主键ID';
COMMENT ON COLUMN task_sub_tasks.task_id IS '所属任务ID';
COMMENT ON COLUMN task_sub_tasks.title IS '子任务标题';
COMMENT ON COLUMN task_sub_tasks.description IS '子任务描述';
COMMENT ON COLUMN task_sub_tasks.sort_order IS '排序序号';
COMMENT ON COLUMN task_sub_tasks.status IS '子任务状态：pending/in_progress/completed';
COMMENT ON COLUMN task_sub_tasks.completed_at IS '完成时间';

-- 索引
CREATE INDEX idx_task_sub_tasks_task_id ON task_sub_tasks (task_id) WHERE deleted = 0;

-- ============================================
-- 任务模板表
-- ============================================
CREATE TABLE IF NOT EXISTS task_templates (
    template_id        VARCHAR(32)    NOT NULL,
    category           VARCHAR(20)    NOT NULL,
    name               VARCHAR(100)   NOT NULL,
    description        TEXT,
    default_title      VARCHAR(200),
    default_description TEXT,
    stages_config      JSONB,
    is_system          BOOLEAN        NOT NULL DEFAULT FALSE,
    create_time        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_templates PRIMARY KEY (template_id)
);

COMMENT ON TABLE task_templates IS '任务模板表：预定义的任务模板，用于快速创建任务';
COMMENT ON COLUMN task_templates.template_id IS '模板主键ID';
COMMENT ON COLUMN task_templates.category IS '适用类别：daily/training/business_trip/inventory/assessment';
COMMENT ON COLUMN task_templates.name IS '模板名称';
COMMENT ON COLUMN task_templates.description IS '模板描述';
COMMENT ON COLUMN task_templates.default_title IS '默认标题模板';
COMMENT ON COLUMN task_templates.default_description IS '默认描述模板';
COMMENT ON COLUMN task_templates.stages_config IS '默认工作流阶段配置（JSON）';
COMMENT ON COLUMN task_templates.is_system IS '是否系统内置模板';

-- 索引
CREATE INDEX IF NOT EXISTS idx_task_templates_category ON task_templates (category) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_task_templates_is_system ON task_templates (is_system) WHERE deleted = 0;
