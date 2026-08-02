-- ============================================================
-- 系统配置管理模块 - sys_config + sys_config_history
-- 版本: V20260404__create_sys_config_table
-- 数据库: PostgreSQL 18
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_config (
    config_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    config_key          VARCHAR(128) NOT NULL,
    config_name         VARCHAR(200) NOT NULL,
    config_group        VARCHAR(50)  NOT NULL DEFAULT 'general',
    config_category     VARCHAR(50)  DEFAULT '',
    value_type          VARCHAR(20)  NOT NULL DEFAULT 'STRING',
    config_value        TEXT         DEFAULT '',
    default_value       TEXT         DEFAULT '',
    is_sensitive        SMALLINT     NOT NULL DEFAULT 0,
    is_encrypted        SMALLINT     NOT NULL DEFAULT 0,
    is_enabled          SMALLINT     NOT NULL DEFAULT 1,
    is_readonly         SMALLINT     NOT NULL DEFAULT 0,
    sort_order          INT          DEFAULT 0,
    description         VARCHAR(500) DEFAULT '',
    validation_rules    VARCHAR(500) DEFAULT '',
    options             VARCHAR(1000) DEFAULT '',
    create_user_id      VARCHAR(64)  DEFAULT '',
    create_username     VARCHAR(100) DEFAULT '',
    update_user_id      VARCHAR(64)  DEFAULT '',
    update_username     VARCHAR(100) DEFAULT '',
    version             INT          NOT NULL DEFAULT 1,
    deleted             SMALLINT     NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP    NULL,

    CONSTRAINT uk_sys_config_key UNIQUE (config_key, deleted)
);

COMMENT ON TABLE sys_config IS '系统配置表';
COMMENT ON COLUMN sys_config.config_id IS '配置ID';
COMMENT ON COLUMN sys_config.config_key IS '配置键(唯一标识)';
COMMENT ON COLUMN sys_config.config_name IS '配置名称';
COMMENT ON COLUMN sys_config.config_group IS '配置分组: system/business/security/storage/notification/general';
COMMENT ON COLUMN sys_config.config_category IS '配置子分类';
COMMENT ON COLUMN sys_config.value_type IS '值类型: STRING/INTEGER/LONG/DOUBLE/BOOLEAN/JSON/TEXT';
COMMENT ON COLUMN sys_config.config_value IS '配置值';
COMMENT ON COLUMN sys_config.default_value IS '默认值';
COMMENT ON COLUMN sys_config.is_sensitive IS '是否敏感: 0否,1是';
COMMENT ON COLUMN sys_config.is_encrypted IS '是否已加密: 0否,1是';
COMMENT ON COLUMN sys_config.is_enabled IS '是否启用: 0禁用,1启用';
COMMENT ON COLUMN sys_config.is_readonly IS '是否只读: 0可编辑,1只读(系统关键配置)';
COMMENT ON COLUMN sys_config.sort_order IS '排序序号';
COMMENT ON COLUMN sys_config.description IS '配置描述';
COMMENT ON COLUMN sys_config.validation_rules IS '校验规则(JSON格式)';
COMMENT ON COLUMN sys_config.options IS '可选值列表(JSON数组)';
COMMENT ON COLUMN sys_config.version IS '乐观锁版本号';
COMMENT ON COLUMN sys_config.deleted IS '逻辑删除: 0未删除,1已删除';

CREATE INDEX IF NOT EXISTS idx_sys_config_group ON sys_config(config_group, config_category, deleted);
CREATE INDEX IF NOT EXISTS idx_sys_config_key_search ON sys_config(config_key, deleted);
CREATE INDEX IF NOT EXISTS idx_sys_config_name_search ON sys_config(config_name, deleted);
CREATE INDEX IF NOT EXISTS idx_sys_config_enabled ON sys_config(is_enabled, deleted);
CREATE INDEX IF NOT EXISTS idx_sys_config_created ON sys_config(created_at);

CREATE TABLE IF NOT EXISTS sys_config_history (
    history_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    config_id           BIGINT       NOT NULL,
    config_key          VARCHAR(128) NOT NULL,
    old_value           TEXT         DEFAULT '',
    new_value           TEXT         DEFAULT '',
    change_type         VARCHAR(20)  NOT NULL DEFAULT 'UPDATE',
    change_reason       VARCHAR(500) DEFAULT '',
    operator_id         VARCHAR(64)  DEFAULT '',
    operator_name       VARCHAR(100) DEFAULT '',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_config_history IS '系统配置变更历史表';
COMMENT ON COLUMN sys_config_history.history_id IS '历史记录ID';
COMMENT ON COLUMN sys_config_history.config_id IS '关联配置ID';
COMMENT ON COLUMN sys_config_history.config_key IS '配置键';
COMMENT ON COLUMN sys_config_history.old_value IS '变更前值';
COMMENT ON COLUMN sys_config_history.new_value IS '变更后值';
COMMENT ON COLUMN sys_config_history.change_type IS '变更类型: CREATE/UPDATE/RESET/ENABLE/DISABLE';
COMMENT ON COLUMN sys_config_history.change_reason IS '变更原因';
COMMENT ON COLUMN sys_config_history.operator_id IS '操作人ID';
COMMENT ON COLUMN sys_config_history.operator_name IS '操作人姓名';

CREATE INDEX IF NOT EXISTS idx_config_history_config ON sys_config_history(config_id, created_at);
CREATE INDEX IF NOT EXISTS idx_config_history_operator ON sys_config_history(operator_id, created_at);
CREATE INDEX IF NOT EXISTS idx_config_history_time ON sys_config_history(created_at);

INSERT INTO sys_config (config_key, config_name, config_group, value_type, default_value, is_readonly, description, sort_order) VALUES
('system.name', '系统名称', 'system', 'STRING', '食品溯源管理系统', 1, '系统显示名称', 1),
('system.version', '系统版本', 'system', 'STRING', '1.0.0', 1, '当前系统版本号', 2),
('system.logo_url', '系统Logo地址', 'system', 'STRING', '', 0, '系统Logo图片URL', 3),
('security.password.min_length', '密码最小长度', 'security', 'INTEGER', '8', 1, '用户密码最小字符数', 10),
('security.password.max_length', '密码最大长度', 'security', 'INTEGER', '32', 1, '用户密码最大字符数', 11),
('security.login.max_attempts', '登录最大尝试次数', 'security', 'INTEGER', '5', 0, '连续失败次数上限后锁定', 20),
('security.login.lock_duration_minutes', '登录锁定时长(分钟)', 'security', 'INTEGER', '30', 0, '账户锁定持续时间(分钟)', 21),
('upload.max_file_size_mb', '上传文件大小限制(MB)', 'storage', 'INTEGER', '50', 0, '单个上传文件最大大小(MB)', 30),
('upload.allowed_extensions', '允许的文件扩展名', 'storage', 'TEXT', 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,csv,txt', 0, '允许上传的文件扩展名(逗号分隔)', 31),
('storage.user_quota_mb', '用户存储配额(MB)', 'storage', 'INTEGER', '500', 0, '每个用户的最大存储空间(MB)', 40),
('audit.log.retention_days', '审计日志保留天数', 'system', 'INTEGER', '180', 0, '审计日志保留天数(超期自动清理)', 50),
('file.cleanup.logical_delete_days', '文件逻辑删除天数', 'system', 'INTEGER', '180', 0, '过期文件逻辑删除阈值(天)', 51),
('file.cleanup.physical_delete_days', '文件物理清理延迟天数', 'system', 'INTEGER', '30', 0, '已删文件物理清理延迟(天)', 52),
('notification.email.enabled', '邮件通知开关', 'notification', 'BOOLEAN', 'false', 0, '是否启用邮件通知', 60),
('notification.email.smtp_host', 'SMTP服务器地址', 'notification', 'STRING', '', 1, '邮件服务器地址(敏感)', 70),
('general.date_format', '日期显示格式', 'general', 'STRING', 'yyyy-MM-dd HH:mm:ss', 0, '系统默认日期时间格式', 80),
('general.page_size', '默认分页大小', 'general', 'INTEGER', '20', 0, '列表默认每页条数', 81);

UPDATE sys_config SET is_sensitive = 1 WHERE config_key IN ('notification.email.smtp_host');
