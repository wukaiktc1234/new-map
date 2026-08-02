-- ============================================================
-- 迁移脚本: V20260516__create_schedule_management_tables.sql
-- 功能: 排班管理模块 - 新增7张业务表
-- 作者: SDD Pipeline Auto-Generate
-- 日期: 2026-05-16
-- 依赖: stores表(必须存在), employees表(必须存在)
-- ============================================================

-- ============================================================
-- Part 1: 排班方案主表 (schedule_plans)
-- 存储排班方案的元数据和状态信息
-- ============================================================
CREATE TABLE IF NOT EXISTS schedule_plans (
    -- 主键: 使用雪花算法生成分布式唯一ID
    plan_id VARCHAR(32) PRIMARY KEY,

    -- 基本信息
    plan_name VARCHAR(100) NOT NULL,            -- 方案名称(如"2026年5月第3周排班")

    -- 关联门店
    store_id BIGINT NOT NULL,                   -- 门店ID(数据隔离)

    -- 排班周期
    start_date DATE NOT NULL,                   -- 周期起始日期
    end_date DATE NOT NULL,                     -- 周期结束日期

    -- 状态(语义化字符串, ADR-001决策)
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
                                                -- draft-草稿 published-已发布 executing-执行中 archived-已归档

    -- 版本控制(乐观锁, ADR-004决策)
    version INTEGER NOT NULL DEFAULT 1,          -- 版本号(每次发布+1)

    -- 模板关联
    template_id VARCHAR(32),                     -- 使用的模板ID(可选)

    -- 统计字段(冗余,避免联表计算)
    employee_count INTEGER DEFAULT 0,            -- 涉及员工总数
    total_work_hours INTEGER DEFAULT 0,          -- 总工时(分钟)

    -- 发布信息
    publisher_id BIGINT,                         -- 发布人用户ID
    publisher_name VARCHAR(50),                  -- 发布人姓名(冗余)
    publish_time TIMESTAMP,                      -- 发布时间

    -- 撤回信息
    withdrawer_id BIGINT,                        -- 撤回人用户ID
    withdrawer_name VARCHAR(50),                 -- 撤回人姓名
    withdraw_time TIMESTAMP,                     -- 撤回时间
    withdraw_reason VARCHAR(500),                -- 撤回原因

    -- 考勤同步信息
    sync_status VARCHAR(20) DEFAULT 'not_synced',
                                                -- not_synced-未 synced-已同步 failed-失败
    sync_to_attendance_time TIMESTAMP,           -- 最后同步时间
    sync_message VARCHAR(200),                   -- 同步结果说明

    -- 必备字段(遵循项目规范)
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_schedule_plans_store ON schedule_plans(store_id);
CREATE INDEX IF NOT EXISTS idx_schedule_plans_status ON schedule_plans(store_id, status);
CREATE INDEX IF NOT EXISTS idx_schedule_plans_date ON schedule_plans(store_id, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_schedule_plans_template ON schedule_plans(template_id);
CREATE INDEX IF NOT EXISTS idx_schedule_plans_deleted ON schedule_plans(deleted);

-- 注释
COMMENT ON TABLE schedule_plans IS '排班方案主表';
COMMENT ON COLUMN schedule_plans.plan_name IS '方案名称';
COMMENT ON COLUMN schedule_plans.store_id IS '门店ID(数据隔离)';
COMMENT ON COLUMN schedule_plans.start_date IS '周期起始日期';
COMMENT ON COLUMN schedule_plans.end_date IS '周期结束日期';
COMMENT ON COLUMN schedule_plans.status IS '状态: draft-草稿 published-已发布 executing-执行中 archived-已归档';
COMMENT ON COLUMN schedule_plans.version IS '版本号(乐观锁,每次发布+1)';
COMMENT ON COLUMN schedule_plans.employee_count IS '涉及员工总数(冗余)';
COMMENT ON COLUMN schedule_plans.total_work_hours IS '总工时(单位:分钟)';
COMMENT ON COLUMN schedule_plans.sync_status IS '考勤同步状态: not_synced-未同步 synced-已同步 failed-失败';

-- 状态检查约束
ALTER TABLE schedule_plans ADD CONSTRAINT chk_plan_status
    CHECK (status IN ('draft', 'published', 'executing', 'archived'));
ALTER TABLE schedule_plans ADD CONSTRAINT chk_sync_status
    CHECK (sync_status IN ('not_synced', 'synced', 'failed'));

-- ============================================================
-- Part 2: 排班条目明细表 (schedule_entries)
-- 存储"谁在哪天哪个班次"的最小排班单元
-- ============================================================
CREATE TABLE IF NOT EXISTS schedule_entries (
    -- 主键
    entry_id VARCHAR(32) PRIMARY KEY,

    -- 关联方案
    plan_id VARCHAR(32) NOT NULL,                -- 所属排班方案ID

    -- 员工信息
    employee_id BIGINT NOT NULL,                 -- 员工ID
    employee_name VARCHAR(50) NOT NULL,          -- 员工姓名(冗余,避免联表)
    position_name VARCHAR(50),                  -- 岗位名称(冗余)

    -- 日期和班次
    work_date DATE NOT NULL,                    -- 工作日期
    shift_type VARCHAR(30) NOT NULL,            -- 班次类型编码(morning/noon/evening/night_off)
    shift_name VARCHAR(50) NOT NULL,            -- 班次显示名称(如"早班(A)")

    -- 班次时间段(冗余自shift_types,发布快照)
    start_time TIME NOT NULL,                   -- 班次开始时间(HH:mm)
    end_time TIME NOT NULL,                     -- 班次结束时间(HH:mm)

    -- 来源标识
    source VARCHAR(20) NOT NULL DEFAULT 'auto',
                                                -- auto-自动生成 manual-手动编辑 swap-换班产生

    -- 冲突标记(冗余,加速查询)
    conflict_level VARCHAR(10),                 -- error/warning/info/null
    conflict_message VARCHAR(200),              -- 冲突描述

    -- 排序
    sort_order INTEGER DEFAULT 0,               -- 显示顺序

    -- 必备字段
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,

    -- 外键约束
    CONSTRAINT fk_entry_plan FOREIGN KEY (plan_id)
        REFERENCES schedule_plans(plan_id) ON DELETE CASCADE
);

-- 索引(关键性能索引, ADR-002决策)
CREATE INDEX IF NOT EXISTS idx_entries_plan_employee_date
    ON schedule_entries(plan_id, employee_id, work_date);
CREATE INDEX IF NOT EXISTS idx_entries_plan_date_shift
    ON schedule_entries(plan_id, work_date, shift_type);
CREATE INDEX IF NOT EXISTS idx_entries_employee_date
    ON schedule_entries(employee_id, work_date);
CREATE INDEX IF NOT EXISTS idx_entries_plan_conflict
    ON schedule_entries(plan_id, conflict_level);
CREATE INDEX IF NOT EXISTS idx_entries_deleted ON schedule_entries(deleted);

-- 注释
COMMENT ON TABLE schedule_entries IS '排班条目明细表';
COMMENT ON COLUMN schedule_entries.plan_id IS '所属排班方案ID';
COMMENT ON COLUMN schedule_entries.employee_id IS '员工ID';
COMMENT ON COLUMN schedule_entries.work_date IS '工作日期';
COMMENT ON COLUMN schedule_entries.shift_type IS '班次类型编码: morning-早班 noon-中班 evening-晚班 night_off-休息';
COMMENT ON COLUMN schedule_entries.source IS '来源: auto-自动生成 manual-手动编辑 swap-换班产生';
COMMENT ON COLUMN schedule_entries.conflict_level IS '冲突等级: error-严重 warning-警告 info-提示 null-无';

-- 来源检查约束
ALTER TABLE schedule_entries ADD CONSTRAINT chk_entry_source
    CHECK (source IN ('auto', 'manual', 'swap'));
ALTER TABLE schedule_entries ADD CONSTRAINT chk_conflict_level
    CHECK (conflict_level IN ('error', 'warning', 'info') OR conflict_level IS NULL);

-- 唯一约束: 同一方案同一员工同一天只能有一条记录(一人一天一班)
CREATE UNIQUE INDEX IF NOT EXISTS uk_entry_unique
    ON schedule_entries(plan_id, employee_id, work_date) WHERE deleted = 0;

-- ============================================================
-- Part 3: 排班模板表 (schedule_templates)
-- 存储可复用的排班配置(需求量+规则组合)
-- ============================================================
CREATE TABLE IF NOT EXISTS schedule_templates (
    -- 主键
    template_id VARCHAR(32) PRIMARY KEY,

    -- 基本信息
    template_name VARCHAR(100) NOT NULL,         -- 模板名称
    description TEXT,                            -- 模板描述

    -- 门店归属
    store_id BIGINT NOT NULL,                    -- 门店ID(模板按门店隔离)

    -- 时段需求矩阵(JSONB, ADR-002决策)
    demand_matrix JSONB NOT NULL DEFAULT '{}',
                                                -- {"weekday":{"morning":3,"noon":4,"evening":2},...}

    -- 启用的规则ID列表(JSONB)
    enabled_rule_ids JSONB DEFAULT '[]',        -- ["rule_001","rule_002",...]

    -- 状态
    is_default BOOLEAN NOT NULL DEFAULT FALSE,   -- 是否默认模板
    status VARCHAR(20) NOT NULL DEFAULT 'active',
                                                -- active-启用 inactive-停用

    -- 使用统计
    use_count INTEGER DEFAULT 0,                 -- 被使用次数

    -- 必备字段
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_templates_store ON schedule_templates(store_id);
CREATE INDEX IF NOT EXISTS idx_templates_store_status ON schedule_templates(store_id, status);
CREATE INDEX IF NOT EXISTS idx_templates_default ON schedule_templates(store_id, is_default);
CREATE INDEX IF NOT EXISTS idx_templates_deleted ON schedule_templates(deleted);

-- 注释
COMMENT ON TABLE schedule_templates IS '排班模板表';
COMMENT ON COLUMN schedule_templates.template_name IS '模板名称';
COMMENT ON COLUMN schedule_templates.demand_matrix IS '时段需求矩阵(JSON): {"weekday":{"morning":N,"noon":N,"evening":N},"weekend":{...},"holiday":{...}}';
COMMENT ON COLUMN schedule_templates.enabled_rule_ids IS '启用的规则ID列表(JSON数组)';
COMMENT ON COLUMN schedule_templates.is_default IS '是否默认模板(每个门店仅一个)';
COMMENT ON COLUMN schedule_templates.use_count IS '被使用次数';

-- 状态检查约束
ALTER TABLE schedule_templates ADD CONSTRAINT chk_template_status
    CHECK (status IN ('active', 'inactive'));

-- ============================================================
-- Part 4: 换班申请表 (swap_requests)
-- 存储员工换班请求和审批信息
-- ============================================================
CREATE TABLE IF NOT EXISTS swap_requests (
    -- 主键
    request_id VARCHAR(32) PRIMARY KEY,

    -- 关联方案
    plan_id VARCHAR(32) NOT NULL,                -- 所属排班方案ID

    -- 发起人信息
    initiator_id BIGINT NOT NULL,                -- 发起人用户ID
    initiator_name VARCHAR(50) NOT NULL,         -- 发起人姓名(冗余)

    -- 发起人的班次(换出)
    initiator_entry_id VARCHAR(32) NOT NULL,     -- 发起人的排班条目ID
    initiator_work_date DATE NOT NULL,           -- 换出的日期
    initiator_shift_type VARCHAR(30) NOT NULL,   -- 换出的班次类型
    initiator_shift_name VARCHAR(50) NOT NULL,   -- 换出的班次名称

    -- 目标人信息
    target_employee_id BIGINT NOT NULL,          -- 换班目标员工ID
    target_employee_name VARCHAR(50) NOT NULL,   -- 目标员工姓名(冗余)

    -- 目标人的班次(换入)
    target_entry_id VARCHAR(32) NOT NULL,        -- 目标人的排班条目ID
    target_work_date DATE NOT NULL,              -- 换入的日期
    target_shift_type VARCHAR(30) NOT NULL,      -- 换入的班次类型
    target_shift_name VARCHAR(50) NOT NULL,      -- 换入的班次名称

    -- 申请状态(语义化字符串, ADR-001决策)
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
                                                -- pending-待审批 approved-通过 rejected-拒绝 cancelled-已取消

    -- 申请内容
    reason VARCHAR(500),                         -- 换班原因(选填)

    -- 审批信息
    approver_id BIGINT,                          -- 审批人用户ID
    approver_name VARCHAR(50),                   -- 审批人姓名
    approve_time TIMESTAMP,                      -- 审批时间
    reject_reason VARCHAR(500),                  -- 拒绝原因

    -- 必备字段
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,

    -- 外键约束
    CONSTRAINT fk_swap_plan FOREIGN KEY (plan_id)
        REFERENCES schedule_plans(plan_id) ON DELETE CASCADE
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_swap_requests_plan ON swap_requests(plan_id);
CREATE INDEX IF NOT EXISTS idx_swap_requests_initiator ON swap_requests(initiator_id, status);
CREATE INDEX IF NOT EXISTS idx_swap_requests_target ON swap_requests(target_employee_id, status);
CREATE INDEX IF NOT EXISTS idx_swap_requests_status ON swap_requests(status);
CREATE INDEX IF NOT EXISTS idx_swap_requests_create_time ON swap_requests(create_time);
CREATE INDEX IF NOT EXISTS idx_swap_requests_deleted ON swap_requests(deleted);

-- 注释
COMMENT ON TABLE swap_requests IS '换班申请表';
COMMENT ON COLUMN swap_requests.initiator_id IS '发起人用户ID';
COMMENT ON COLUMN swap_requests.target_employee_id IS '换班目标员工ID';
COMMENT ON COLUMN swap_requests.status IS '状态: pending-待审批 approved-通过 rejected-拒绝 cancelled-已取消';
COMMENT ON COLUMN swap_requests.reject_reason IS '拒绝原因(拒绝时必填)';

-- 状态检查约束
ALTER TABLE swap_requests ADD CONSTRAINT chk_swap_status
    CHECK (status IN ('pending', 'approved', 'rejected', 'cancelled'));

-- ============================================================
-- Part 5: 班次类型配置表 (schedule_shift_types)
-- 定义门店使用的班次类型(如早班/中班/晚班/休息)
-- ============================================================
CREATE TABLE IF NOT EXISTS schedule_shift_types (
    -- 主键
    shift_type_id VARCHAR(32) PRIMARY KEY,

    -- 基本信息
    shift_code VARCHAR(30) NOT NULL,             -- 班次编码(内部标识,如morning/noon/evening/night_off)
    shift_name VARCHAR(50) NOT NULL,             -- 班次显示名称(如"早班(A)")

    -- 门店归属
    store_id BIGINT NOT NULL,                    -- 门店ID(班次按门店配置)

    -- 时间段
    start_time TIME NOT NULL,                    -- 开始时间(HH:mm格式)
    end_time TIME NOT NULL,                      -- 结束时间(HH:mm格式,跨夜用次日时间如01:00)

    -- 显示属性
    color VARCHAR(20) DEFAULT '#409EFF',         -- 颜色标识(CSS颜色值或CSS变量名)
    icon VARCHAR(50),                            -- 图标(可选)

    -- 计算属性(冗余,便于统计)
    duration_minutes INTEGER NOT NULL,           -- 持续时长(分钟,自动计算)

    -- 是否为休息班次
    is_rest BOOLEAN NOT NULL DEFAULT FALSE,      -- 是否休息(休息不计工时)

    -- 排序和状态
    sort_order INTEGER DEFAULT 0,                -- 显示顺序
    status VARCHAR(20) NOT NULL DEFAULT 'active',
                                                -- active-启用 inactive-停用

    -- 必备字段
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,

    -- 唯一约束: 门店内班次编码唯一
    CONSTRAINT uk_shift_code UNIQUE (store_id, shift_code)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_shift_types_store ON schedule_shift_types(store_id);
CREATE INDEX IF NOT EXISTS idx_shift_types_store_status ON schedule_shift_types(store_id, status);
CREATE INDEX IF NOT EXISTS idx_shift_types_sort ON schedule_shift_types(store_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_shift_types_deleted ON schedule_shift_types(deleted);

-- 注释
COMMENT ON TABLE schedule_shift_types IS '班次类型配置表';
COMMENT ON COLUMN schedule_shift_types.shift_code IS '班次编码: morning-早班 noon-中班 evening-晚班 night_off-休息';
COMMENT ON COLUMN schedule_shift_types.shift_name IS '班次显示名称(如"早班(A)")';
COMMENT ON COLUMN schedule_shift_types.start_time IS '开始时间(HH:mm格式)';
COMMENT ON COLUMN schedule_shift_types.end_time IS '结束时间(HH:mm格式,跨夜用次日时间如01:00)';
COMMENT ON COLUMN schedule_shift_types.color IS '颜色标识(CSS颜色值或--fts-*变量名)';
COMMENT ON COLUMN schedule_shift_types.duration_minutes IS '持续时长(分钟,自动计算)';
COMMENT ON COLUMN schedule_shift_types.is_rest IS '是否休息班次(休息不计工时)';

-- 状态检查约束
ALTER TABLE schedule_shift_types ADD CONSTRAINT chk_shift_status
    CHECK (status IN ('active', 'inactive'));

-- ============================================================
-- Part 6: 排班规则配置表 (schedule_rules)
-- 定义冲突检测使用的业务规则
-- ============================================================
CREATE TABLE IF NOT EXISTS schedule_rules (
    -- 主键
    rule_id VARCHAR(32) PRIMARY KEY,

    -- 基本信息
    rule_code VARCHAR(50) NOT NULL,              -- 规则编码(唯一标识)
    rule_name VARCHAR(100) NOT NULL,             -- 规则名称
    description TEXT,                            -- 规则描述

    -- 门店归属
    store_id BIGINT NOT NULL,                    -- 门店ID(规则按门店配置)

    -- 规则分类
    category VARCHAR(30) NOT NULL,               -- 规则分类: hard_constraint-硬约束 soft_constraint-软约束

    -- 规则参数(JSONB,灵活配置不同规则的不同参数)
    parameters JSONB NOT NULL DEFAULT '{}',
                                                -- 示例: {"max_consecutive_days":6,"min_rest_hours":10,"min_weekly_rest_days":1}

    -- 优先级(数值越小优先级越高)
    priority INTEGER NOT NULL DEFAULT 100,

    -- 状态
    status VARCHAR(20) NOT NULL DEFAULT 'active',
                                                -- active-启用 inactive-停用

    -- 是否为系统预置规则(不可删除)
    is_system BOOLEAN NOT NULL DEFAULT FALSE,    -- 系统预置规则

    -- 必备字段
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,

    -- 唯一约束: 门店内规则编码唯一
    CONSTRAINT uk_rule_code UNIQUE (store_id, rule_code)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_rules_store ON schedule_rules(store_id);
CREATE INDEX IF NOT EXISTS idx_rules_store_category ON schedule_rules(store_id, category);
CREATE INDEX IF NOT EXISTS idx_rules_store_status ON schedule_rules(store_id, status);
CREATE INDEX IF NOT EXISTS idx_rules_priority ON schedule_rules(priority);
CREATE INDEX IF NOT EXISTS idx_rules_deleted ON schedule_rules(deleted);

-- 注释
COMMENT ON TABLE schedule_rules IS '排班规则配置表';
COMMENT ON COLUMN schedule_rules.rule_code IS '规则编码: consecutive_days/rest_time/weekly_rest/duplicate_shift/on_leave_check/resigned_check/staff_shortage';
COMMENT ON COLUMN schedule_rules.category IS '规则分类: hard_constraint-硬约束(必须满足) soft_constraint-软约束(建议满足)';
COMMENT ON COLUMN schedule_rules.parameters IS '规则参数(JSON): 根据rule_code不同而不同';
COMMENT ON COLUMN schedule_rules.priority IS '优先级(数值越小越优先)';

-- 状态检查约束
ALTER TABLE schedule_rules ADD CONSTRAINT chk_rule_category
    CHECK (category IN ('hard_constraint', 'soft_constraint'));
ALTER TABLE schedule_rules ADD CONSTRAINT chk_rule_status
    CHECK (status IN ('active', 'inactive'));

-- ============================================================
-- Part 7: 通知记录表 (notification_logs)
-- 记录排班生命周期中的通知发送情况
-- ============================================================
CREATE TABLE IF NOT EXISTS notification_logs (
    -- 主键
    log_id VARCHAR(32) PRIMARY KEY,

    -- 关联业务
    business_type VARCHAR(50) NOT NULL,          -- 业务类型: schedule_published/schedule_withdrawn/swap_approved/swap_rejected/daily_reminder
    business_id VARCHAR(32) NOT NULL,            -- 业务ID(方案ID/换班申请ID等)

    -- 通知内容
    title VARCHAR(200) NOT NULL,                 -- 通知标题
    content TEXT NOT NULL,                       -- 通知内容(支持模板变量)

    -- 接收人列表(JSONB)
    receiver_ids JSONB NOT NULL DEFAULT '[]',    -- 接收人用户ID数组 [101,102,103]
    receiver_names JSONB DEFAULT '[]',           -- 接收人姓名数组(冗余)

    -- 发送渠道
    channel VARCHAR(20) NOT NULL DEFAULT 'in_app',
                                                -- in_app-站内信(本期实现) sms-短信 email-邮件 wechat_work-企微(预留)

    -- 发送状态
    send_status VARCHAR(20) NOT NULL DEFAULT 'pending',
                                                -- pending-待发送 sending-发送中 success-成功 failed-失败
    send_time TIMESTAMP,                         -- 实际发送时间
    retry_count INTEGER DEFAULT 0,               -- 重试次数
    error_message VARCHAR(500),                  -- 错误信息(失败时记录)

    -- 必备字段
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_notification_logs_business ON notification_logs(business_type, business_id);
CREATE INDEX IF NOT EXISTS idx_notification_logs_status ON notification_logs(send_status);
CREATE INDEX IF NOT EXISTS idx_notification_logs_create_time ON notification_logs(create_time);
CREATE INDEX IF NOT EXISTS idx_notification_logs_deleted ON notification_logs(deleted);

-- 注释
COMMENT ON TABLE notification_logs IS '通知记录表';
COMMENT ON COLUMN notification_logs.business_type IS '业务类型: schedule_published-排班发布 schedule_withdrawn-排班撤回 swap_approved-换班通过 swap_rejected-换班拒绝 daily_reminder-明日提醒';
COMMENT ON COLUMN notification_logs.receiver_ids IS '接收人用户ID数组(JSON)';
COMMENT ON COLUMN notification_logs.channel IS '发送渠道: in_app-站内信(本期) sms-短信 email-邮件 wechat_work-企微(预留)';
COMMENT ON COLUMN notification_logs.send_status IS '发送状态: pending-待发送 sending-发送中 success-成功 failed-失败';

-- 状态检查约束
ALTER TABLE notification_logs ADD CONSTRAINT chk_business_type
    CHECK (business_type IN ('schedule_published', 'schedule_withdrawn', 'swap_approved', 'swap_rejected', 'daily_reminder'));
ALTER TABLE notification_logs ADD CONSTRAINT chk_channel
    CHECK (channel IN ('in_app', 'sms', 'email', 'wechat_work'));
ALTER TABLE notification_logs ADD CONSTRAINT chk_send_status
    CHECK (send_status IN ('pending', 'sending', 'success', 'failed'));

-- ============================================================
-- Part 8: 菜单基础表（初始化）& 排班管理菜单数据
-- ============================================================

-- 创建菜单基础表（首次迁移时表不存在）
CREATE TABLE IF NOT EXISTS menus (
    id              BIGINT NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    permission_type INTEGER NOT NULL DEFAULT 1,
    module          VARCHAR(50),
    parent_id       BIGINT NOT NULL DEFAULT 0,
    level           INTEGER NOT NULL DEFAULT 1,
    sort_order      INTEGER NOT NULL DEFAULT 0,
    component       VARCHAR(200),
    path            VARCHAR(200),
    icon            VARCHAR(50),
    is_hidden       INTEGER NOT NULL DEFAULT 0,
    status          INTEGER NOT NULL DEFAULT 1,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT pk_menus PRIMARY KEY (id),
    CONSTRAINT uk_menus_permission_code UNIQUE (permission_code)
);

COMMENT ON TABLE menus IS '菜单权限表 - 存储系统菜单和权限树';
COMMENT ON COLUMN menus.id IS '菜单ID';
COMMENT ON COLUMN menus.permission_code IS '权限编码（唯一）';
COMMENT ON COLUMN menus.permission_name IS '权限/菜单名称';
COMMENT ON COLUMN menus.permission_type IS '类型：1=菜单 2=按钮';
COMMENT ON COLUMN menus.module IS '所属模块';
COMMENT ON COLUMN menus.parent_id IS '父菜单ID（0=根菜单）';
COMMENT ON COLUMN menus.level IS '层级';
COMMENT ON COLUMN menus.sort_order IS '排序号';
COMMENT ON COLUMN menus.component IS '组件路径';
COMMENT ON COLUMN menus.path IS '路由路径';
COMMENT ON COLUMN menus.icon IS '图标';
COMMENT ON COLUMN menus.is_hidden IS '是否隐藏：0=显示 1=隐藏';
COMMENT ON COLUMN menus.status IS '状态：1=启用 0=禁用';

INSERT INTO menus (id, permission_code, permission_name, permission_type, module, parent_id, level, sort_order, component, path, icon, is_hidden, status)
VALUES
    (10001, 'schedule:index', '排班管理', 1, 'schedule', 0, 1, 15, NULL, '/schedule', 'Calendar', 0, 1),
    (10002, 'schedule:plan:list', '排班方案', 1, 'schedule', 10001, 2, 1, NULL, '/schedule/plans', 'Document', 0, 1),
    (10003, 'schedule:calendar', '排班日历', 1, 'schedule', 10001, 2, 2, NULL, '/schedule/calendar', 'Timer', 0, 1),
    (10004, 'schedule:swap', '换班管理', 1, 'schedule', 10001, 2, 3, NULL, '/schedule/swap', 'Switch', 0, 1),
    (10005, 'schedule:template', '排班模板', 1, 'schedule', 10001, 2, 4, NULL, '/schedule/templates', 'Files', 0, 1),
    (10006, 'schedule:shift-config', '班次配置', 1, 'schedule', 10001, 2, 5, NULL, '/schedule/shift-types', 'Setting', 0, 1)
ON CONFLICT (permission_code) DO NOTHING;

-- ============================================================
-- Part 9: 初始化预置排班规则（全局预置，store_id=0）
-- ============================================================
INSERT INTO schedule_rules (rule_id, rule_code, rule_name, description, store_id, category, parameters, priority, status, is_system, create_time, update_time, deleted)
VALUES
    ('rule_sys_001', 'consecutive_days', '连续工作天数限制', '防止员工连续工作超过指定天数', 0, 'hard_constraint', '{"max_consecutive_days":6}', 10, 'active', true, NOW(), NOW(), 0),
    ('rule_sys_002', 'rest_time', '最小休息间隔', '两次班次之间必须有足够的休息时间', 0, 'hard_constraint', '{"min_rest_hours":10}', 20, 'active', true, NOW(), NOW(), 0),
    ('rule_sys_003', 'weekly_rest', '每周最少休息天数', '确保员工每周至少休息一天', 0, 'hard_constraint', '{"min_weekly_rest_days":1}', 30, 'active', true, NOW(), NOW(), 0),
    ('rule_sys_004', 'duplicate_shift', '禁止一天多班', '同一员工同一天不能安排多个班次', 0, 'hard_constraint', '{}', 5, 'active', true, NOW(), NOW(), 0),
    ('rule_sys_005', 'on_leave_check', '员工请假检查', '排班时检查员工是否在该时段请假', 0, 'soft_constraint', '{}', 50, 'active', true, NOW(), NOW(), 0),
    ('rule_sys_006', 'staff_shortage', '缺员预警', '某时段实际排班人数低于需求量时警告', 0, 'soft_constraint', '{}', 60, 'active', true, NOW(), NOW(), 0)
ON CONFLICT (rule_id) DO NOTHING;
-- 注意: store_id=0 表示全局预置规则,实际使用时需要复制到各门店
