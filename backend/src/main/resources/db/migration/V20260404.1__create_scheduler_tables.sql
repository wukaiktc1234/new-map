-- ============================================================
-- 定时任务调度模块 - 数据库初始化脚本
-- 版本: V20260404__create_scheduler_tables.sql
-- 说明: 创建定时任务调度相关表（3张表 + 7个索引 + 7条内置种子数据）
-- ============================================================

-- ==================== 表1: scheduled_task（任务定义表）====================
CREATE TABLE IF NOT EXISTS scheduled_task (
    task_id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_name            VARCHAR(100) NOT NULL,
    task_code            VARCHAR(100) NOT NULL UNIQUE,
    description          VARCHAR(500),
    task_group           VARCHAR(50) DEFAULT 'DEFAULT',
    job_handler         VARCHAR(200) NOT NULL,
    cron_expression      VARCHAR(100),
    interval_seconds     INTEGER,
    task_type            SMALLINT NOT NULL DEFAULT 1,
    status               SMALLINT NOT NULL DEFAULT 0,
    execution_status     SMALLINT DEFAULT 0,
    concurrent_policy    SMALLINT DEFAULT 0,
    max_retry_count      INTEGER DEFAULT 3,
    retry_interval_sec   INTEGER DEFAULT 30,
    timeout_seconds      INTEGER DEFAULT 300,
    misfire_policy       SMALLINT DEFAULT 1,
    last_execution_time  TIMESTAMP,
    next_execution_time  TIMESTAMP,
    last_execution_msg   TEXT,
    is_builtin           SMALLINT NOT NULL DEFAULT 0,
    version              BIGINT NOT NULL DEFAULT 1,
    deleted              SMALLINT NOT NULL DEFAULT 0,
    create_time          TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time          TIMESTAMP NOT NULL DEFAULT NOW(),
    create_user_id       BIGINT,
    create_username      VARCHAR(50),
    update_user_id       BIGINT,
    update_username      VARCHAR(50)
);

COMMENT ON TABLE scheduled_task IS '定时任务定义表';
COMMENT ON COLUMN scheduled_task.task_id IS '任务ID，主键自增';
COMMENT ON COLUMN scheduled_task.task_name IS '任务名称';
COMMENT ON COLUMN scheduled_task.task_code IS '任务编码，唯一标识';
COMMENT ON COLUMN scheduled_task.description IS '任务描述';
COMMENT ON COLUMN scheduled_task.task_group IS '任务分组';
COMMENT ON COLUMN scheduled_task.job_handler IS '任务执行处理器类名/方法名';
COMMENT ON COLUMN scheduled_task.cron_expression IS 'CRON表达式（task_type=1时使用）';
COMMENT ON COLUMN scheduled_task.interval_seconds IS '固定间隔秒数（task_type=2时使用）';
COMMENT ON COLUMN scheduled_task.task_type IS '任务类型：1=CRON 2=FIXED_RATE 3=ONE_TIME';
COMMENT ON COLUMN scheduled_task.status IS '任务状态：0=CREATED 1=ENABLED 2=PAUSED 3=DISABLED 4=ERROR';
COMMENT ON COLUMN scheduled_task.execution_status IS '执行状态：0=IDLE 1=PENDING 2=RUNNING';
COMMENT ON COLUMN scheduled_task.concurrent_policy IS '并发策略：0=FORBID禁止 1=ALLOW允许 2=DISCARD丢弃';
COMMENT ON COLUMN scheduled_task.max_retry_count IS '最大重试次数';
COMMENT ON COLUMN scheduled_task.retry_interval_sec IS '重试间隔秒数';
COMMENT ON COLUMN scheduled_task.timeout_seconds IS '超时时间（秒）';
COMMENT ON COLUMN scheduled_task.misfire_policy IS '错失触发策略：1=立即执行 2=跳过 3=仅一次';
COMMENT ON COLUMN scheduled_task.last_execution_time IS '上次执行时间';
COMMENT ON COLUMN scheduled_task.next_execution_time IS '下次执行时间';
COMMENT ON COLUMN scheduled_task.last_execution_msg IS '上次执行消息';
COMMENT ON COLUMN scheduled_task.is_builtin IS '是否内置任务：0=否 1=是';
COMMENT ON COLUMN scheduled_task.version IS '乐观锁版本号';
COMMENT ON COLUMN scheduled_task.deleted IS '逻辑删除标记：0=未删除 1=已删除';
COMMENT ON COLUMN scheduled_task.created_at IS '创建时间';
COMMENT ON COLUMN scheduled_task.updated_at IS '更新时间';
COMMENT ON COLUMN scheduled_task.create_user_id IS '创建用户ID';
COMMENT ON COLUMN scheduled_task.create_username IS '创建用户名';
COMMENT ON COLUMN scheduled_task.update_user_id IS '更新用户ID';
COMMENT ON COLUMN scheduled_task.update_username IS '更新用户名';

-- ==================== 表2: task_execution_log（执行日志表）====================
CREATE TABLE IF NOT EXISTS task_execution_log (
    log_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id            BIGINT NOT NULL,
    task_name          VARCHAR(100),
    task_code          VARCHAR(100),
    trigger_type       SMALLINT DEFAULT 1,
    execution_status   SMALLINT NOT NULL DEFAULT 0,
    start_time         TIMESTAMP NOT NULL,
    end_time           TIMESTAMP,
    duration_ms        BIGINT,
    error_message      TEXT,
    result_data        JSONB,
    retry_count        INTEGER DEFAULT 0,
    create_time        TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE task_execution_log IS '任务执行日志表';
COMMENT ON COLUMN task_execution_log.log_id IS '日志ID，主键自增';
COMMENT ON COLUMN task_execution_log.task_id IS '关联任务ID';
COMMENT ON COLUMN task_execution_log.task_name IS '任务名称（冗余存储）';
COMMENT ON COLUMN task_execution_log.task_code IS '任务编码（冗余存储）';
COMMENT ON COLUMN task_execution_log.trigger_type IS '触发类型：1=SCHEDULED定时 2=MANUAL手动 3=RETRY重试 4=COMPENSATE补偿';
COMMENT ON COLUMN task_execution_log.execution_status IS '执行状态：0=RUNNING 1=SUCCESS 2=FAILED 3=TIMEOUT 4=CANCELLED 5=PARTIAL';
COMMENT ON COLUMN task_execution_log.start_time IS '开始时间';
COMMENT ON COLUMN task_execution_log.end_time IS '结束时间';
COMMENT ON COLUMN task_execution_log.duration_ms IS '执行耗时（毫秒）';
COMMENT ON COLUMN task_execution_log.error_message IS '错误信息';
COMMENT ON COLUMN task_execution_log.result_data IS '结果数据（JSON格式）';
COMMENT ON COLUMN task_execution_log.retry_count IS '重试次数';
COMMENT ON COLUMN task_execution_log.create_time IS '创建时间';

-- ==================== 表3: task_execution_log_detail（执行详情表）====================
CREATE TABLE IF NOT EXISTS task_execution_log_detail (
    detail_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    log_id       BIGINT NOT NULL,
    step_name    VARCHAR(100) NOT NULL,
    step_status  SMALLINT NOT NULL DEFAULT 0,
    start_time   TIMESTAMP NOT NULL,
    end_time       TIMESTAMP,
    duration_ms  BIGINT,
    message      TEXT,
    create_time  TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE task_execution_log_detail IS '任务执行详情表（步骤级别）';
COMMENT ON COLUMN task_execution_log_detail.detail_id IS '详情ID，主键自增';
COMMENT ON COLUMN task_execution_log_detail.log_id IS '关联日志ID';
COMMENT ON COLUMN task_execution_log_detail.step_name IS '步骤名称';
COMMENT ON COLUMN task_execution_log_detail.step_status IS '步骤状态：0=PENDING 1=RUNNING 2=SUCCESS 3=FAILED 4=SKIPPED';
COMMENT ON COLUMN task_execution_log_detail.start_time IS '开始时间';
COMMENT ON COLUMN task_execution_log_detail.end_time IS '结束时间';
COMMENT ON COLUMN task_execution_log_detail.duration_ms IS '耗时（毫秒）';
COMMENT ON COLUMN task_execution_log_detail.message IS '步骤消息/备注';
COMMENT ON COLUMN task_execution_log_detail.create_time IS '创建时间';

-- ==================== 索引创建 ====================

-- scheduled_task 表索引
CREATE INDEX IF NOT EXISTS idx_scheduled_task_status ON scheduled_task(status);
CREATE INDEX IF NOT EXISTS idx_scheduled_task_task_group ON scheduled_task(task_group);
CREATE INDEX IF NOT EXISTS uk_scheduled_task_code ON scheduled_task(task_code);
CREATE INDEX IF NOT EXISTS idx_scheduled_task_execution_status ON scheduled_task(execution_status);

-- task_execution_log 表索引
CREATE INDEX IF NOT EXISTS idx_task_execution_log_task_id ON task_execution_log(task_id);
CREATE INDEX IF NOT EXISTS idx_task_execution_log_start_time ON task_execution_log(start_time);

-- task_execution_log_detail 表索引
CREATE INDEX IF NOT EXISTS idx_task_execution_log_detail_log_id ON task_execution_log_detail(log_id);

-- ==================== 内置任务种子数据（7条）====================
INSERT INTO scheduled_task (task_name, task_code, description, task_group, job_handler, cron_expression, interval_seconds, task_type, status, execution_status, concurrent_policy, max_retry_count, retry_interval_sec, timeout_seconds, misfire_policy, is_builtin, version, deleted)
VALUES
    ('日志清理任务', 'log-cleanup', '定期清理系统运行日志和操作日志，保留最近90天数据', 'SYSTEM', 'com.example.demo.scheduler.handler.LogCleanupHandler', '0 0 2 * * ?', NULL, 1, 1, 0, 0, 3, 30, 300, 1, 1, 1, 0),

    ('文件清理任务', 'file-cleanup', '清理临时上传文件和过期附件', 'SYSTEM', 'com.example.demo.scheduler.handler.FileCleanupHandler', '0 0 3 * * ?', NULL, 1, 1, 0, 0, 3, 30, 600, 1, 1, 1, 0),

    ('数据库备份任务', 'db-backup', '自动备份数据库，每周日凌晨4点执行', 'SYSTEM', 'com.example.demo.scheduler.handler.DatabaseBackupHandler', '0 0 4 * * SUN', NULL, 1, 1, 0, 0, 3, 60, 1800, 1, 1, 1, 0),

    ('缓存预热任务', 'cache-warmup', '预热常用缓存数据，提升系统响应速度', 'SYSTEM', 'com.example.demo.scheduler.handler.CacheWarmupHandler', '0 0 5 * * ?', NULL, 1, 1, 0, 0, 2, 30, 120, 1, 1, 1, 0),

    ('报表生成任务', 'report-generate', '每周一凌晨6点自动生成经营报表', 'REPORT', 'com.example.demo.scheduler.handler.ReportGenerateHandler', '0 0 6 * * MON', NULL, 1, 1, 0, 0, 3, 60, 600, 1, 1, 1, 0),

    ('证书过期检查任务', 'cert-check', '每日检查健康证、营业执照等证书过期情况', 'COMPLIANCE', 'com.example.demo.scheduler.handler.CertificateCheckHandler', '0 0 6 * * ?', NULL, 1, 1, 0, 0, 2, 30, 300, 1, 1, 1, 0),

    ('会话清理任务', 'session-cleanup', '每小时清理过期的用户会话和登录令牌', 'SECURITY', 'com.example.demo.scheduler.handler.SessionCleanupHandler', NULL, 3600, 2, 1, 0, 0, 1, 10, 120, 1, 1, 1, 0)
ON CONFLICT (task_code) DO NOTHING;
