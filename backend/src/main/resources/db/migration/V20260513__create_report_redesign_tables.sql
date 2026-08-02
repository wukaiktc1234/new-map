-- ============================================================
-- 经营报表模块重构支撑表
-- 版本: V20260513__create_report_redesign_tables.sql
-- 说明: 报表用户配置、智能洞察规则、导出任务
-- ============================================================

-- ============================================================
-- 报表用户配置表 (report_configs)
-- 说明: 经营报表模块重构支撑表 - 用户报表配置
-- ============================================================
CREATE TABLE IF NOT EXISTS report_configs (
    config_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT NOT NULL,                    -- 用户ID
    report_type         INTEGER NOT NULL DEFAULT 1,         -- 1日报 2周报 3月报 4季报 5年报 6利润分析
    config_key          VARCHAR(50) NOT NULL,               -- 配置项key：date_range/store_ids/channel/compare_type/kpi_order
    config_value        TEXT,                               -- 配置值（JSON或简单字符串）
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT uk_report_config_user_type_key UNIQUE (user_id, report_type, config_key)
);

CREATE INDEX idx_report_configs_user_id ON report_configs(user_id) WHERE deleted = 0;
CREATE INDEX idx_report_configs_type ON report_configs(report_type) WHERE deleted = 0;

COMMENT ON TABLE report_configs IS '报表用户配置表';
COMMENT ON COLUMN report_configs.config_id IS '配置ID';
COMMENT ON COLUMN report_configs.user_id IS '用户ID';
COMMENT ON COLUMN report_configs.report_type IS '报表类型: 1日报 2周报 3月报 4季报 5年报 6利润分析';
COMMENT ON COLUMN report_configs.config_key IS '配置项key';
COMMENT ON COLUMN report_configs.config_value IS '配置值';

-- ============================================================
-- 智能洞察规则表 (report_insight_rules)
-- 说明: 经营报表智能洞察的规则引擎配置
-- ============================================================
CREATE TABLE IF NOT EXISTS report_insight_rules (
    rule_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rule_name           VARCHAR(100) NOT NULL,              -- 规则名称
    rule_type           INTEGER NOT NULL DEFAULT 1,         -- 1异常提醒 2增长亮点 3行动建议 4趋势预警
    report_scope        INTEGER NOT NULL DEFAULT 0,         -- 0全部 1日报 2周报 3月报 4季报 5年报 6利润
    metric_key          VARCHAR(50) NOT NULL,               -- 指标key：revenue/order_count/avg_check/profit/etc
    compare_target      INTEGER NOT NULL DEFAULT 1,         -- 1日均值 2上周同期 3上月同期 4去年同期 5目标值
    threshold_type      INTEGER NOT NULL DEFAULT 1,         -- 1绝对值 2百分比
    threshold_value     DECIMAL(10,4) NOT NULL,             -- 阈值
    operator            VARCHAR(10) NOT NULL DEFAULT '<',   -- 比较运算符：< <= > >= ==
    severity            INTEGER NOT NULL DEFAULT 1,         -- 1提示 2警告 3严重
    message_template    VARCHAR(500) NOT NULL,              -- 消息模板，支持占位符如${metricName} ${diffPercent}
    is_enabled          BOOLEAN NOT NULL DEFAULT true,      -- 是否启用
    priority            INTEGER NOT NULL DEFAULT 0,         -- 优先级（数字越大越优先）
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_insight_rules_type ON report_insight_rules(rule_type) WHERE deleted = 0;
CREATE INDEX idx_insight_rules_scope ON report_insight_rules(report_scope) WHERE deleted = 0;
CREATE INDEX idx_insight_rules_enabled ON report_insight_rules(is_enabled) WHERE deleted = 0;

COMMENT ON TABLE report_insight_rules IS '智能洞察规则表';
COMMENT ON COLUMN report_insight_rules.rule_id IS '规则ID';
COMMENT ON COLUMN report_insight_rules.rule_type IS '规则类型: 1异常提醒 2增长亮点 3行动建议 4趋势预警';
COMMENT ON COLUMN report_insight_rules.report_scope IS '适用范围: 0全部 1日报 2周报 3月报 4季报 5年报 6利润';
COMMENT ON COLUMN report_insight_rules.metric_key IS '指标key';
COMMENT ON COLUMN report_insight_rules.compare_target IS '对比目标: 1日均值 2上周同期 3上月同期 4去年同期 5目标值';
COMMENT ON COLUMN report_insight_rules.threshold_type IS '阈值类型: 1绝对值 2百分比';
COMMENT ON COLUMN report_insight_rules.threshold_value IS '阈值';
COMMENT ON COLUMN report_insight_rules.operator IS '比较运算符';
COMMENT ON COLUMN report_insight_rules.severity IS '严重级别: 1提示 2警告 3严重';
COMMENT ON COLUMN report_insight_rules.message_template IS '消息模板';
COMMENT ON COLUMN report_insight_rules.is_enabled IS '是否启用';
COMMENT ON COLUMN report_insight_rules.priority IS '优先级';

-- ============================================================
-- 报表导出任务表 (export_tasks)
-- 说明: 经营报表异步导出任务管理
-- ============================================================
CREATE TABLE IF NOT EXISTS export_tasks (
    task_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_no             VARCHAR(32) UNIQUE NOT NULL,        -- 任务编号 EXP20260513001
    task_type           INTEGER NOT NULL DEFAULT 1,         -- 1Excel 2PDF
    report_type         INTEGER NOT NULL,                   -- 1日报 2周报 3月报 4季报 5年报 6利润分析
    report_params_json  JSONB,                              -- 导出时的报表参数
    file_name           VARCHAR(200),                       -- 文件名
    file_path           VARCHAR(500),                       -- 文件存储路径
    file_size           BIGINT DEFAULT 0,                   -- 文件大小（字节）
    status              INTEGER NOT NULL DEFAULT 0,         -- 0待处理 1处理中 2成功 3失败
    error_message       TEXT,                               -- 错误信息
    row_count           INTEGER DEFAULT 0,                  -- 导出数据行数
    created_by          BIGINT NOT NULL,                    -- 创建人ID
    completed_time      TIMESTAMP,                          -- 完成时间
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_export_tasks_status ON export_tasks(status) WHERE deleted = 0;
CREATE INDEX idx_export_tasks_created_by ON export_tasks(created_by) WHERE deleted = 0;
CREATE INDEX idx_export_tasks_create_time ON export_tasks(create_time) WHERE deleted = 0;

COMMENT ON TABLE export_tasks IS '报表导出任务表';
COMMENT ON COLUMN export_tasks.task_id IS '任务ID';
COMMENT ON COLUMN export_tasks.task_no IS '任务编号';
COMMENT ON COLUMN export_tasks.task_type IS '任务类型: 1Excel 2PDF';
COMMENT ON COLUMN export_tasks.report_type IS '报表类型: 1日报 2周报 3月报 4季报 5年报 6利润分析';
COMMENT ON COLUMN export_tasks.report_params_json IS '报表参数JSON';
COMMENT ON COLUMN export_tasks.file_name IS '文件名';
COMMENT ON COLUMN export_tasks.file_path IS '文件存储路径';
COMMENT ON COLUMN export_tasks.file_size IS '文件大小（字节）';
COMMENT ON COLUMN export_tasks.status IS '状态: 0待处理 1处理中 2成功 3失败';
COMMENT ON COLUMN export_tasks.error_message IS '错误信息';
COMMENT ON COLUMN export_tasks.row_count IS '导出数据行数';
COMMENT ON COLUMN export_tasks.created_by IS '创建人ID';
COMMENT ON COLUMN export_tasks.completed_time IS '完成时间';
