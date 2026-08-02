-- ============================================================
-- 数据备份与恢复模块 - 数据库表结构
-- 版本: V20260404.2
-- 说明: 创建备份记录表和恢复日志表
-- ============================================================

-- 表1: 数据备份记录表
CREATE TABLE IF NOT EXISTS data_backup_record (
    backup_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    backup_name        VARCHAR(200) NOT NULL,
    backup_type        SMALLINT NOT NULL DEFAULT 1,   -- 1=FULL全量 2=INCREMENTAL增量 3=SCHEMA_ONLY仅结构
    backup_method      SMALLINT NOT NULL DEFAULT 1,   -- 1=PG_DUMP 2=PG_RESTORE 3=CUSTOM自定义
    file_path          VARCHAR(1000),                  -- 备份文件存储路径
    file_size_bytes    BIGINT DEFAULT 0,               -- 备份文件大小(字节)
    file_checksum      VARCHAR(128),                   -- SHA256校验和
    status             SMALLINT NOT NULL DEFAULT 0,    -- 0=PENDING待处理 1=IN_PROGRESS进行中 2=SUCCESS成功 3=FAILED失败 4=EXPIRED已过期 5=DELETING删除中
    tables_included    TEXT,                           -- 包含的表列表(JSON数组)
    rows_affected      BIGINT,                         -- 影响行数
    duration_ms        BIGINT,                         -- 执行耗时(毫秒)
    error_message      TEXT,                           -- 错误信息
    triggered_by       VARCHAR(100),                   -- 触发者: MANUAL/SCHEDULED/SYSTEM
    trigger_user_id    BIGINT,                         -- 触发用户ID
    trigger_username   VARCHAR(50),                    -- 触发用户名
    storage_location   VARCHAR(200) DEFAULT 'local',   -- 存储位置: local/s3/oss
    retention_days     INTEGER DEFAULT 30,             -- 保留天数
    expires_at         TIMESTAMP,                      -- 过期时间
    version            BIGINT NOT NULL DEFAULT 1,      -- 乐观锁版本号
    deleted            SMALLINT NOT NULL DEFAULT 0,    -- 逻辑删除: 0未删除 1已删除
    create_time        TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time        TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE data_backup_record IS '数据备份记录表 - 存储所有数据库备份操作记录';
COMMENT ON COLUMN data_backup_record.backup_id IS '备份记录主键';
COMMENT ON COLUMN data_backup_record.backup_name IS '备份名称';
COMMENT ON COLUMN data_backup_record.backup_type IS '备份类型: 1=FULL全量 2=INCREMENTAL增量 3=SCHEMA_ONLY仅结构';
COMMENT ON COLUMN data_backup_record.backup_method IS '备份方式: 1=PG_DUMP 2=PG_RESTORE 3=CUSTOM自定义';
COMMENT ON COLUMN data_backup_record.file_path IS '备份文件存储路径';
COMMENT ON COLUMN data_backup_record.file_size_bytes IS '备份文件大小(字节)';
COMMENT ON COLUMN data_backup_record.file_checksum IS 'SHA256文件校验和';
COMMENT ON COLUMN data_backup_record.status IS '状态: 0=PENDING 1=IN_PROGRESS 2=SUCCESS 3=FAILED 4=EXPIRED 5=DELETING';
COMMENT ON COLUMN data_backup_record.tables_included IS '包含的表列表(JSON数组格式)';
COMMENT ON COLUMN data_backup_record.rows_affected IS '影响行数';
COMMENT ON COLUMN data_backup_record.duration_ms IS '执行耗时(毫秒)';
COMMENT ON COLUMN data_backup_record.error_message IS '错误信息';
COMMENT ON COLUMN data_backup_record.triggered_by IS '触发方式: MANUAL手动/SCHEDULED定时/SYSTEM系统';
COMMENT ON COLUMN data_backup_record.trigger_user_id IS '触发用户ID';
COMMENT ON COLUMN data_backup_record.trigger_username IS '触发用户名';
COMMENT ON COLUMN data_backup_record.storage_location IS '存储位置: local本地/s3/oss对象存储';
COMMENT ON COLUMN data_backup_record.retention_days IS '保留天数';
COMMENT ON COLUMN data_backup_record.expires_at IS '过期时间';
COMMENT ON COLUMN data_backup_record.version IS '乐观锁版本号';
COMMENT ON COLUMN data_backup_record.deleted IS '逻辑删除标记: 0未删除 1已删除';
COMMENT ON COLUMN data_backup_record.create_time IS '创建时间';
COMMENT ON COLUMN data_backup_record.update_time IS '更新时间';

-- 索引: 备份记录表
CREATE UNIQUE INDEX IF NOT EXISTS uk_data_backup_record_backup_id ON data_backup_record(backup_id);
CREATE INDEX IF NOT EXISTS idx_data_backup_record_status ON data_backup_record(status);
CREATE INDEX IF NOT EXISTS idx_data_backup_record_backup_type ON data_backup_record(backup_type);
CREATE INDEX IF NOT EXISTS idx_data_backup_record_trigger_user_id ON data_backup_record(trigger_user_id);
CREATE INDEX IF NOT EXISTS idx_data_backup_record_create_time ON data_backup_record(create_time);
CREATE INDEX IF NOT EXISTS idx_data_backup_record_expires_at ON data_backup_record(expires_at);

-- 表2: 恢复日志表
CREATE TABLE IF NOT EXISTS backup_restore_log (
    log_id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    backup_id          BIGINT NOT NULL,
    backup_name        VARCHAR(200),
    restore_status     SMALLINT NOT NULL DEFAULT 0,    -- 0=PENDING待处理 1=IN_PROGRESS进行中 2=SUCCESS成功 3=FAILED失败 4=ROLLED_BACK已回滚
    restore_mode       SMALLINT DEFAULT 1,             -- 1=FULL_OVERWRITE全覆盖 2=SELECTIVE选择性 3=PREVIEW_ONLY预览
    target_tables      TEXT,                           -- 目标表(选择性恢复时使用，JSON数组)
    pre_restore_snapshot VARCHAR(500),                 -- 恢复前自动快照路径
    error_message      TEXT,                           -- 错误信息
    duration_ms        BIGINT,                         -- 执行耗时(毫秒)
    confirmed_by       VARCHAR(50),                    -- 确认人
    confirm_time       TIMESTAMP,                      -- 确认时间
    rollback_sql       TEXT,                           -- 回滚SQL(如果生成)
    create_user_id     BIGINT,                         -- 操作用户ID
    create_username    VARCHAR(50),                    -- 操作用户名
    create_time        TIMESTAMP NOT NULL DEFAULT NOW(),
    finish_time        TIMESTAMP                       -- 完成时间
);

COMMENT ON TABLE backup_restore_log IS '恢复日志表 - 记录所有数据恢复操作的详细日志';
COMMENT ON COLUMN backup_restore_log.log_id IS '恢复日志主键';
COMMENT ON COLUMN backup_restore_log.backup_id IS '关联的备份记录ID';
COMMENT ON COLUMN backup_restore_log.backup_name IS '关联的备份名称';
COMMENT ON COLUMN backup_restore_log.restore_status IS '恢复状态: 0=PENDING 1=IN_PROGRESS 2=SUCCESS 3=FAILED 4=ROLLED_BACK';
COMMENT ON COLUMN backup_restore_log.restore_mode IS '恢复模式: 1=FULL_OVERWRITE全覆盖 2=SELECTIVE选择性 3=PREVIEW_ONLY预览';
COMMENT ON COLUMN backup_restore_log.target_tables IS '目标表列表(选择性恢复时使用，JSON数组)';
COMMENT ON COLUMN backup_restore_log.pre_restore_snapshot IS '恢复前自动快照路径';
COMMENT ON COLUMN backup_restore_log.error_message IS '错误信息';
COMMENT ON COLUMN backup_restore_log.duration_ms IS '执行耗时(毫秒)';
COMMENT ON COLUMN backup_restore_log.confirmed_by IS '确认人用户名';
COMMENT ON COLUMN backup_restore_log.confirm_time IS '确认时间';
COMMENT ON COLUMN backup_restore_log.rollback_sql IS '回滚SQL语句(如果生成)';
COMMENT ON COLUMN backup_restore_log.create_user_id IS '操作用户ID';
COMMENT ON COLUMN backup_restore_log.create_username IS '操作用户名';
COMMENT ON COLUMN backup_restore_log.create_time IS '创建时间';
COMMENT ON COLUMN backup_restore_log.finish_time IS '完成时间';

-- 索引: 恢复日志表
CREATE UNIQUE INDEX IF NOT EXISTS uk_backup_restore_log_log_id ON backup_restore_log(log_id);
CREATE INDEX IF NOT EXISTS idx_backup_restore_log_backup_id ON backup_restore_log(backup_id);
CREATE INDEX IF NOT EXISTS idx_backup_restore_log_restore_status ON backup_restore_log(restore_status);
CREATE INDEX IF NOT EXISTS idx_backup_restore_log_create_user_id ON backup_restore_log(create_user_id);
CREATE INDEX IF NOT EXISTS idx_backup_restore_log_create_time ON backup_restore_log(create_time);

-- 外键约束: 恢复日志 -> 备份记录
ALTER TABLE backup_restore_log
    ADD CONSTRAINT fk_restore_log_backup_record
    FOREIGN KEY (backup_id) REFERENCES data_backup_record(backup_id)
    ON DELETE RESTRICT ON UPDATE CASCADE;
