-- ============================================================
-- 组织架构变更日志表 (organization_change_logs)
-- ------------------------------------------------------------
-- 背景：OrganizationChangeLogServiceImpl 通过 baseMapper.insert(log)
-- 向 organization_change_logs 表写入数据，但此前缺少建表迁移脚本，
-- 导致运行时抛出 Table doesn't exist 异常。
-- 本脚本依据 OrganizationChangeLog 实体类字段定义创建对应表结构。
-- ============================================================

CREATE TABLE IF NOT EXISTS organization_change_logs (
    id              BIGINT       PRIMARY KEY,
    change_type     INTEGER,
    object_id       BIGINT,
    object_name     VARCHAR(200),
    before_change   TEXT,
    after_change    TEXT,
    change_reason   VARCHAR(500),
    operator        VARCHAR(100),
    operate_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status          INTEGER,
    remark          VARCHAR(500),
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER      NOT NULL DEFAULT 0
);

-- 索引：按变更对象查询（object_id 维度，实体未定义 object_type，故单列索引）
CREATE INDEX IF NOT EXISTS idx_org_change_logs_object   ON organization_change_logs (object_id);
-- 索引：按操作人查询（实体字段为 operator）
CREATE INDEX IF NOT EXISTS idx_org_change_logs_operator ON organization_change_logs (operator);
-- 索引：按操作时间查询
CREATE INDEX IF NOT EXISTS idx_org_change_logs_time     ON organization_change_logs (operate_time);
-- 索引：按变更类型查询（服务层 getLogsByChangeType 高频使用）
CREATE INDEX IF NOT EXISTS idx_org_change_logs_type     ON organization_change_logs (change_type);

COMMENT ON TABLE  organization_change_logs IS '组织架构变更日志表';
COMMENT ON COLUMN organization_change_logs.id IS '变更ID（雪花算法生成）';
COMMENT ON COLUMN organization_change_logs.change_type IS '变更类型：1-部门变更, 2-职位变更, 3-员工变更';
COMMENT ON COLUMN organization_change_logs.object_id IS '变更对象ID';
COMMENT ON COLUMN organization_change_logs.object_name IS '变更对象名称';
COMMENT ON COLUMN organization_change_logs.before_change IS '变更前数据(JSON)';
COMMENT ON COLUMN organization_change_logs.after_change IS '变更后数据(JSON)';
COMMENT ON COLUMN organization_change_logs.change_reason IS '变更原因';
COMMENT ON COLUMN organization_change_logs.operator IS '操作人';
COMMENT ON COLUMN organization_change_logs.operate_time IS '操作时间';
COMMENT ON COLUMN organization_change_logs.status IS '变更状态：1-成功, 0-失败';
COMMENT ON COLUMN organization_change_logs.remark IS '备注';
COMMENT ON COLUMN organization_change_logs.deleted IS '逻辑删除：0-未删除, 1-已删除';
