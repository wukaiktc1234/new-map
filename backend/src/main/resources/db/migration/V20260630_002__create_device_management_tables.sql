-- ============================================================
-- 设备管理模块补齐表结构
--
-- 背景：
--   后端设备管理有 11 个实体类，但 BaseDatabaseInitializer.createDeviceTables()
--   仅创建了 devices 与 device_status_log 两张表。本脚本补齐剩余 10 张表
--   （DeviceStatus.java 无 @TableName 注解，为普通 POJO，不需要建表）。
--
-- 字段对齐原则：
--   1. 表名严格遵循实体类 @TableName 注解（注意复数：device_driver_configs / print_tasks / print_templates）
--   2. 列名优先使用 @TableField 显式指定的名称；无 @TableField 时依赖
--      MyBatis-Plus map-underscore-to-camel-case 自动映射（createdAt→created_at 等）
--   3. 类型映射：String→VARCHAR/TEXT, Long→BIGINT, Integer→INT/SMALLINT,
--      LocalDateTime→TIMESTAMP, Boolean→BOOLEAN, Double→DOUBLE PRECISION
--   4. 每张表均包含 deleted 字段（SMALLINT NOT NULL DEFAULT 0），
--      实体未声明 @TableLogic 的表该字段不参与逻辑删除，仅占位以备扩展
--   5. 时间字段：实体使用 createdAt/updatedAt 的表建 created_at/updated_at 列；
--      实体使用 createTime/updateTime 的表建 create_time/update_time 列
--
-- 对应实体:
--   device_alerts                 → DeviceAlert.java
--   device_status_history         → DeviceStatusHistory.java
--   device_template               → DeviceTemplate.java
--   device_data                   → DeviceData.java
--   device_driver_version         → DeviceDriverVersion.java
--   device_driver_configs         → DeviceDriverConfig.java
--   device_workflow               → DeviceWorkflow.java
--   device_workflow_execution_log → DeviceWorkflowExecutionLog.java
--   print_tasks                   → PrintTask.java
--   print_templates               → PrintTemplate.java
--
-- 对应 H2 初始化: BaseDatabaseInitializer.createDeviceManagementTables
-- ============================================================

-- ============================================================
-- 1. device_alerts 设备告警表
-- ============================================================
CREATE TABLE IF NOT EXISTS device_alerts (
    alert_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id             BIGINT,
    alert_type            INT,
    alert_level           INT,
    alert_message         VARCHAR(500),
    is_handled            BOOLEAN DEFAULT FALSE,
    handle_time           TIMESTAMP,
    handle_result         VARCHAR(500),
    handle_user_id        BIGINT,
    create_time           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               SMALLINT NOT NULL DEFAULT 0,
    device_type           INT,
    alert_status          INT,
    trigger_time          TIMESTAMP,
    resolve_time          TIMESTAMP,
    device_status_snapshot VARCHAR(1000),
    device_name           VARCHAR(200)
);
COMMENT ON TABLE device_alerts IS '设备告警表';
COMMENT ON COLUMN device_alerts.alert_id IS '告警ID';
COMMENT ON COLUMN device_alerts.device_id IS '设备ID';
COMMENT ON COLUMN device_alerts.alert_type IS '告警类型：1离线超时 2纸张缺 3碳带缺 4故障 5维护提醒';
COMMENT ON COLUMN device_alerts.alert_level IS '告警级别：1信息 2警告 3严重 4紧急';
COMMENT ON COLUMN device_alerts.alert_message IS '告警消息';
COMMENT ON COLUMN device_alerts.is_handled IS '是否已处理';
COMMENT ON COLUMN device_alerts.handle_time IS '处理时间';
COMMENT ON COLUMN device_alerts.handle_result IS '处理结果';
COMMENT ON COLUMN device_alerts.handle_user_id IS '处理人ID';
COMMENT ON COLUMN device_alerts.device_type IS '设备类型（冗余字段）';
COMMENT ON COLUMN device_alerts.alert_status IS '告警状态：0未处理 1处理中 2已解决 3已忽略';
COMMENT ON COLUMN device_alerts.trigger_time IS '触发时间';
COMMENT ON COLUMN device_alerts.resolve_time IS '解决时间';
COMMENT ON COLUMN device_alerts.device_status_snapshot IS '设备状态快照';
COMMENT ON COLUMN device_alerts.device_name IS '设备名称（冗余）';
CREATE INDEX IF NOT EXISTS idx_device_alerts_device_id ON device_alerts(device_id);
CREATE INDEX IF NOT EXISTS idx_device_alerts_alert_status ON device_alerts(alert_status);
CREATE INDEX IF NOT EXISTS idx_device_alerts_trigger_time ON device_alerts(trigger_time);

-- ============================================================
-- 2. device_status_history 设备状态历史记录表
--    实体无 @TableField，依赖驼峰转下划线映射；使用 created_at 列
-- ============================================================
CREATE TABLE IF NOT EXISTS device_status_history (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_type       VARCHAR(50),
    device_id         BIGINT,
    device_name       VARCHAR(200),
    device_model      VARCHAR(100),
    online            BOOLEAN DEFAULT FALSE,
    response_time     BIGINT,
    firmware_version  VARCHAR(100),
    connection_type   VARCHAR(50),
    ip_address        VARCHAR(50),
    port              VARCHAR(20),
    check_time        TIMESTAMP,
    details           VARCHAR(1000),
    error_message     VARCHAR(1000),
    store_id          BIGINT,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT NOT NULL DEFAULT 0
);
COMMENT ON TABLE device_status_history IS '设备状态历史记录表';
COMMENT ON COLUMN device_status_history.device_type IS '设备类型';
COMMENT ON COLUMN device_status_history.device_id IS '设备ID';
COMMENT ON COLUMN device_status_history.device_name IS '设备名称';
COMMENT ON COLUMN device_status_history.device_model IS '设备型号';
COMMENT ON COLUMN device_status_history.online IS '设备在线状态';
COMMENT ON COLUMN device_status_history.response_time IS '设备响应时间（毫秒）';
COMMENT ON COLUMN device_status_history.firmware_version IS '设备固件版本';
COMMENT ON COLUMN device_status_history.connection_type IS '连接类型';
COMMENT ON COLUMN device_status_history.ip_address IS '网络IP地址';
COMMENT ON COLUMN device_status_history.port IS '网络端口';
COMMENT ON COLUMN device_status_history.check_time IS '检测时间';
COMMENT ON COLUMN device_status_history.details IS '检测结果详情';
COMMENT ON COLUMN device_status_history.error_message IS '错误信息';
COMMENT ON COLUMN device_status_history.store_id IS '门店ID';
COMMENT ON COLUMN device_status_history.created_at IS '创建时间';
CREATE INDEX IF NOT EXISTS idx_device_status_history_device_id ON device_status_history(device_id);
CREATE INDEX IF NOT EXISTS idx_device_status_history_check_time ON device_status_history(check_time);

-- ============================================================
-- 3. device_template 设备模板表
--    实体无 @TableField，依赖驼峰转下划线映射；使用 create_time/update_time 列
--    DeviceTemplateMapper.xml 使用 SELECT * ORDER BY create_time，确认列名
-- ============================================================
CREATE TABLE IF NOT EXISTS device_template (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id       BIGINT,
    template_type   VARCHAR(50),
    template_name   VARCHAR(100),
    template_data   TEXT,
    is_default      INT DEFAULT 0,
    store_id        BIGINT,
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted         SMALLINT NOT NULL DEFAULT 0
);
COMMENT ON TABLE device_template IS '设备模板表';
COMMENT ON COLUMN device_template.device_id IS '关联设备ID';
COMMENT ON COLUMN device_template.template_type IS '模板类型：PRINT_FORMAT/LAYOUT_DESIGN';
COMMENT ON COLUMN device_template.template_name IS '模板名称';
COMMENT ON COLUMN device_template.template_data IS '模板数据（JSON）';
COMMENT ON COLUMN device_template.is_default IS '是否为默认模板：0否 1是';
COMMENT ON COLUMN device_template.store_id IS '门店ID';
COMMENT ON COLUMN device_template.created_by IS '创建人';
COMMENT ON COLUMN device_template.updated_by IS '更新人';
CREATE INDEX IF NOT EXISTS idx_device_template_device_id ON device_template(device_id);
CREATE INDEX IF NOT EXISTS idx_device_template_template_type ON device_template(template_type);

-- ============================================================
-- 4. device_data 设备运行数据表
--    实体无 @TableField；deviceId 为 String 类型；使用 created_at/updated_at 列
-- ============================================================
CREATE TABLE IF NOT EXISTS device_data (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    device_id         VARCHAR(100),
    device_type       VARCHAR(50),
    device_name       VARCHAR(100),
    connection_status INT,
    response_time     BIGINT,
    error_count       INT DEFAULT 0,
    operation_count   INT DEFAULT 0,
    temperature       DOUBLE PRECISION,
    load              DOUBLE PRECISION,
    signal_strength   INT,
    collect_time      TIMESTAMP,
    status_details    TEXT,
    store_id          BIGINT,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted           SMALLINT NOT NULL DEFAULT 0
);
COMMENT ON TABLE device_data IS '设备运行数据表';
COMMENT ON COLUMN device_data.device_id IS '设备ID';
COMMENT ON COLUMN device_data.device_type IS '设备类型：SCANNER/PRINTER/CAMERA';
COMMENT ON COLUMN device_data.device_name IS '设备名称';
COMMENT ON COLUMN device_data.connection_status IS '连接状态：0断开 1连接';
COMMENT ON COLUMN device_data.response_time IS '响应时间（毫秒）';
COMMENT ON COLUMN device_data.error_count IS '错误次数';
COMMENT ON COLUMN device_data.operation_count IS '操作次数';
COMMENT ON COLUMN device_data.temperature IS '设备温度（摄氏度）';
COMMENT ON COLUMN device_data.load IS '设备负载（百分比）';
COMMENT ON COLUMN device_data.signal_strength IS '信号强度（百分比）';
COMMENT ON COLUMN device_data.collect_time IS '数据采集时间';
COMMENT ON COLUMN device_data.status_details IS '设备状态详情';
COMMENT ON COLUMN device_data.store_id IS '门店ID';
CREATE INDEX IF NOT EXISTS idx_device_data_device_id ON device_data(device_id);
CREATE INDEX IF NOT EXISTS idx_device_data_collect_time ON device_data(collect_time);

-- ============================================================
-- 5. device_driver_version 设备驱动版本表
--    实体无 @TableField；使用 created_at/updated_at 列
-- ============================================================
CREATE TABLE IF NOT EXISTS device_driver_version (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    driver_name            VARCHAR(100),
    version                VARCHAR(50),
    supported_device_type  VARCHAR(50),
    supported_device_models TEXT,
    driver_file_path       VARCHAR(500),
    file_size              BIGINT,
    file_md5               VARCHAR(64),
    release_date           TIMESTAMP,
    release_notes           TEXT,
    is_default             INT DEFAULT 0,
    status                 INT DEFAULT 1,
    created_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by             VARCHAR(50),
    updated_by             VARCHAR(50),
    deleted                SMALLINT NOT NULL DEFAULT 0
);
COMMENT ON TABLE device_driver_version IS '设备驱动版本表';
COMMENT ON COLUMN device_driver_version.driver_name IS '驱动名称';
COMMENT ON COLUMN device_driver_version.version IS '驱动版本号';
COMMENT ON COLUMN device_driver_version.supported_device_type IS '支持的设备类型';
COMMENT ON COLUMN device_driver_version.supported_device_models IS '支持的设备型号（JSON）';
COMMENT ON COLUMN device_driver_version.driver_file_path IS '驱动文件路径';
COMMENT ON COLUMN device_driver_version.file_size IS '驱动文件大小（字节）';
COMMENT ON COLUMN device_driver_version.file_md5 IS '驱动文件MD5值';
COMMENT ON COLUMN device_driver_version.release_date IS '发布日期';
COMMENT ON COLUMN device_driver_version.release_notes IS '发布说明';
COMMENT ON COLUMN device_driver_version.is_default IS '是否为默认版本：0否 1是';
COMMENT ON COLUMN device_driver_version.status IS '状态：0禁用 1启用';
COMMENT ON COLUMN device_driver_version.created_by IS '创建人';
COMMENT ON COLUMN device_driver_version.updated_by IS '更新人';
CREATE INDEX IF NOT EXISTS idx_device_driver_version_supported_device_type ON device_driver_version(supported_device_type);
CREATE INDEX IF NOT EXISTS idx_device_driver_version_version ON device_driver_version(version);
CREATE UNIQUE INDEX IF NOT EXISTS uk_device_driver_version_name_version ON device_driver_version(driver_name, version);

-- ============================================================
-- 6. device_driver_configs 驱动配置表（注意表名复数）
--    实体有 @TableField；使用 create_time/update_time/deleted 列
-- ============================================================
CREATE TABLE IF NOT EXISTS device_driver_configs (
    config_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    driver_type         VARCHAR(100),
    driver_class_name   VARCHAR(255),
    default_params      TEXT,
    supported_commands  TEXT,
    description         VARCHAR(500),
    is_enabled          BOOLEAN DEFAULT TRUE,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT NOT NULL DEFAULT 0
);
COMMENT ON TABLE device_driver_configs IS '驱动配置表';
COMMENT ON COLUMN device_driver_configs.config_id IS '配置ID';
COMMENT ON COLUMN device_driver_configs.driver_type IS '驱动类型：printer_tspl/printer_wsd/scanner_generic/scale_xiaomi';
COMMENT ON COLUMN device_driver_configs.driver_class_name IS '驱动类全限定名';
COMMENT ON COLUMN device_driver_configs.default_params IS '默认参数（JSON）';
COMMENT ON COLUMN device_driver_configs.supported_commands IS '支持的命令列表（JSON）';
COMMENT ON COLUMN device_driver_configs.description IS '描述';
COMMENT ON COLUMN device_driver_configs.is_enabled IS '是否启用';
CREATE UNIQUE INDEX IF NOT EXISTS uk_device_driver_configs_driver_type ON device_driver_configs(driver_type);
CREATE INDEX IF NOT EXISTS idx_device_driver_configs_is_enabled ON device_driver_configs(is_enabled);

-- ============================================================
-- 7. device_workflow 设备协同工作流表
--    实体无 @TableField；使用 created_at/updated_at 列
-- ============================================================
CREATE TABLE IF NOT EXISTS device_workflow (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workflow_name        VARCHAR(100),
    workflow_desc        VARCHAR(500),
    trigger_device_type  VARCHAR(50),
    trigger_event_type   VARCHAR(50),
    trigger_condition    TEXT,
    action_config        TEXT,
    status               INT DEFAULT 1,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by           VARCHAR(50),
    updated_by           VARCHAR(50),
    deleted              SMALLINT NOT NULL DEFAULT 0
);
COMMENT ON TABLE device_workflow IS '设备协同工作流表';
COMMENT ON COLUMN device_workflow.workflow_name IS '工作流名称';
COMMENT ON COLUMN device_workflow.workflow_desc IS '工作流描述';
COMMENT ON COLUMN device_workflow.trigger_device_type IS '触发设备类型';
COMMENT ON COLUMN device_workflow.trigger_event_type IS '触发事件类型';
COMMENT ON COLUMN device_workflow.trigger_condition IS '触发条件（JSON）';
COMMENT ON COLUMN device_workflow.action_config IS '动作配置（JSON）';
COMMENT ON COLUMN device_workflow.status IS '状态：0禁用 1启用';
COMMENT ON COLUMN device_workflow.created_by IS '创建人';
COMMENT ON COLUMN device_workflow.updated_by IS '更新人';
CREATE INDEX IF NOT EXISTS idx_device_workflow_trigger_device_type ON device_workflow(trigger_device_type);
CREATE INDEX IF NOT EXISTS idx_device_workflow_status ON device_workflow(status);

-- ============================================================
-- 8. device_workflow_execution_log 设备协同工作流执行日志表
--    实体无 @TableField；triggerDeviceId 为 String 类型；使用 created_at/updated_at 列
-- ============================================================
CREATE TABLE IF NOT EXISTS device_workflow_execution_log (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    workflow_id         BIGINT,
    workflow_name       VARCHAR(100),
    trigger_device_id   VARCHAR(100),
    trigger_device_type VARCHAR(50),
    trigger_event_type  VARCHAR(50),
    trigger_event_data  TEXT,
    execution_status    INT,
    execution_result    TEXT,
    execution_time      BIGINT,
    error_message       TEXT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT NOT NULL DEFAULT 0
);
COMMENT ON TABLE device_workflow_execution_log IS '设备协同工作流执行日志表';
COMMENT ON COLUMN device_workflow_execution_log.workflow_id IS '工作流ID';
COMMENT ON COLUMN device_workflow_execution_log.workflow_name IS '工作流名称';
COMMENT ON COLUMN device_workflow_execution_log.trigger_device_id IS '触发设备ID';
COMMENT ON COLUMN device_workflow_execution_log.trigger_device_type IS '触发设备类型';
COMMENT ON COLUMN device_workflow_execution_log.trigger_event_type IS '触发事件类型';
COMMENT ON COLUMN device_workflow_execution_log.trigger_event_data IS '触发事件数据（JSON）';
COMMENT ON COLUMN device_workflow_execution_log.execution_status IS '执行状态：0失败 1成功';
COMMENT ON COLUMN device_workflow_execution_log.execution_result IS '执行结果（JSON）';
COMMENT ON COLUMN device_workflow_execution_log.execution_time IS '执行耗时（毫秒）';
COMMENT ON COLUMN device_workflow_execution_log.error_message IS '错误信息';
CREATE INDEX IF NOT EXISTS idx_device_workflow_execution_log_workflow_id ON device_workflow_execution_log(workflow_id);
CREATE INDEX IF NOT EXISTS idx_device_workflow_execution_log_created_at ON device_workflow_execution_log(created_at);

-- ============================================================
-- 9. print_tasks 打印任务表（注意表名复数）
--    实体有 @TableField；使用 create_time/update_time/deleted 列
-- ============================================================
CREATE TABLE IF NOT EXISTS print_tasks (
    task_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_code        VARCHAR(100),
    device_id        BIGINT,
    task_type        INT,
    content_json     TEXT,
    print_status     INT DEFAULT 0,
    retry_count      INT DEFAULT 0,
    max_retry       INT DEFAULT 3,
    error_message    VARCHAR(1000),
    create_user_id   BIGINT,
    print_time       TIMESTAMP,
    complete_time    TIMESTAMP,
    create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          SMALLINT NOT NULL DEFAULT 0,
    file_path        VARCHAR(500),
    traceability_code VARCHAR(100),
    product_name     VARCHAR(200),
    device_type      INT
);
COMMENT ON TABLE print_tasks IS '打印任务表';
COMMENT ON COLUMN print_tasks.task_id IS '任务ID';
COMMENT ON COLUMN print_tasks.task_code IS '任务编号';
COMMENT ON COLUMN print_tasks.device_id IS '目标打印机ID';
COMMENT ON COLUMN print_tasks.task_type IS '任务类型：1小票 2标签 3报表 4厨房单';
COMMENT ON COLUMN print_tasks.content_json IS '打印内容（JSON/HTML/template）';
COMMENT ON COLUMN print_tasks.print_status IS '打印状态：0待打印 1打印中 2已完成 3失败';
COMMENT ON COLUMN print_tasks.retry_count IS '已重试次数';
COMMENT ON COLUMN print_tasks.max_retry IS '最大重试次数';
COMMENT ON COLUMN print_tasks.error_message IS '错误信息';
COMMENT ON COLUMN print_tasks.create_user_id IS '创建用户ID';
COMMENT ON COLUMN print_tasks.print_time IS '打印时间';
COMMENT ON COLUMN print_tasks.complete_time IS '完成时间';
COMMENT ON COLUMN print_tasks.file_path IS '文件路径';
COMMENT ON COLUMN print_tasks.traceability_code IS '追溯码';
COMMENT ON COLUMN print_tasks.product_name IS '产品名称';
COMMENT ON COLUMN print_tasks.device_type IS '设备类型（冗余字段）';
CREATE UNIQUE INDEX IF NOT EXISTS uk_print_tasks_task_code ON print_tasks(task_code);
CREATE INDEX IF NOT EXISTS idx_print_tasks_device_id ON print_tasks(device_id);
CREATE INDEX IF NOT EXISTS idx_print_tasks_print_status ON print_tasks(print_status);

-- ============================================================
-- 10. print_templates 打印模板表（注意表名复数）
--     实体有 @TableField；使用 create_time/update_time/deleted 列
-- ============================================================
CREATE TABLE IF NOT EXISTS print_templates (
    template_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_name    VARCHAR(100),
    template_type    INT,
    template_content TEXT,
    page_width       INT,
    page_height      INT,
    is_default       BOOLEAN DEFAULT FALSE,
    is_enabled       BOOLEAN DEFAULT TRUE,
    store_id         BIGINT,
    remark           VARCHAR(500),
    create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          SMALLINT NOT NULL DEFAULT 0
);
COMMENT ON TABLE print_templates IS '打印模板表';
COMMENT ON COLUMN print_templates.template_id IS '模板ID';
COMMENT ON COLUMN print_templates.template_name IS '模板名称';
COMMENT ON COLUMN print_templates.template_type IS '模板类型：1小票模板 2标签模板 3报表模板';
COMMENT ON COLUMN print_templates.template_content IS '模板内容（支持变量占位符）';
COMMENT ON COLUMN print_templates.page_width IS '纸宽（mm）';
COMMENT ON COLUMN print_templates.page_height IS '纸高（mm）';
COMMENT ON COLUMN print_templates.is_default IS '是否默认模板';
COMMENT ON COLUMN print_templates.is_enabled IS '是否启用';
COMMENT ON COLUMN print_templates.store_id IS '所属门店ID（null表示全局模板）';
COMMENT ON COLUMN print_templates.remark IS '备注';
CREATE INDEX IF NOT EXISTS idx_print_templates_template_type ON print_templates(template_type);
CREATE INDEX IF NOT EXISTS idx_print_templates_store_id ON print_templates(store_id);
CREATE INDEX IF NOT EXISTS idx_print_templates_is_default ON print_templates(is_default);
