-- ============================================================
-- Sprint 1 通知基础设施 - 数据库迁移脚本
-- 版本: V20260625_003__sprint1_notification_foundation.sql
-- 数据库: PostgreSQL 18（兼容 H2 MODE=PostgreSQL）
--
-- 变更内容:
-- 1. msg_send_record 表新增 notification_id 字段（SITE_MSG 渠道关联 notification 表）
-- 2. 新建 notification_dead_letter 表（死信持久化）
-- 3. 预注册 sprint1.test.event 测试模板（使用 {var} 语法）
-- 4. 更新 SMTP 配置为 QQ 企业邮箱
--
-- 对应文档:
-- - spec.md F-003（站内信统一分发）
-- - spec.md F-005（事务安全）
-- - plan.md 第 3.2 节（DDL）
-- - plan.md ADR-003（死信处理策略）
-- - plan.md ADR-005（SITE_MSG 不双写，notification_id 关联）
-- ============================================================

-- ----------------------------------------------------------
-- 1. msg_send_record 表新增 notification_id 字段
-- 用途: SITE_MSG 渠道发送后，关联到 notification 表的 ID
--       消除"双系统割裂"问题（spec F-003）
-- ----------------------------------------------------------
ALTER TABLE msg_send_record ADD COLUMN IF NOT EXISTS notification_id BIGINT;
COMMENT ON COLUMN msg_send_record.notification_id IS '关联notification表ID(SITE_MSG渠道专用,消除双系统割裂)';

-- 索引：按 notification_id 查询关联通知
CREATE INDEX IF NOT EXISTS idx_msg_send_record_notification_id
    ON msg_send_record(notification_id);

-- 说明：send_status 字段扩展状态码（无需 DDL 修改，SMALLINT 已支持）
-- 原有: 0=PENDING 1=SENDING 2=SUCCESS 3=FAILED 4=PARTIAL
-- 新增: 5=SKIPPED（SMS/WEBHOOK 渠道未实现时跳过，spec F-004）

-- ----------------------------------------------------------
-- 2. 新建 notification_dead_letter 表（死信持久化）
-- 用途: RabbitMQ 死信队列消息持久化到数据库，便于查询和重试
-- 对应: plan.md ADR-003（死信处理策略）
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_dead_letter (
    dead_letter_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    original_record_id BIGINT,
    template_id         BIGINT,
    template_code       VARCHAR(100),
    recipient           VARCHAR(500) NOT NULL,
    recipient_type      SMALLINT,
    subject             VARCHAR(500),
    content             TEXT,
    channel             VARCHAR(50) NOT NULL,
    send_status         SMALLINT NOT NULL DEFAULT 3,
    error_message       TEXT,
    retry_count         INTEGER NOT NULL DEFAULT 0,
    max_retry           INTEGER NOT NULL DEFAULT 3,
    original_message_id VARCHAR(100),
    biz_type            VARCHAR(100),
    biz_id              VARCHAR(100),
    dead_letter_reason  VARCHAR(500),
    dead_letter_time    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved            INTEGER NOT NULL DEFAULT 0,
    resolved_by         BIGINT,
    resolved_time       TIMESTAMP,
    resolve_remark      VARCHAR(500),
    version             BIGINT NOT NULL DEFAULT 0,
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE notification_dead_letter IS '通知死信表-持久化RabbitMQ死信队列消息便于查询重试';
COMMENT ON COLUMN notification_dead_letter.dead_letter_id IS '主键ID';
COMMENT ON COLUMN notification_dead_letter.original_record_id IS '原始msg_send_record.record_id';
COMMENT ON COLUMN notification_dead_letter.template_id IS '关联模板ID';
COMMENT ON COLUMN notification_dead_letter.template_code IS '关联模板编码';
COMMENT ON COLUMN notification_dead_letter.recipient IS '收件人(邮箱/用户ID/手机号)';
COMMENT ON COLUMN notification_dead_letter.recipient_type IS '收件人类型:1=EMAIL 2=USER_ID 3=PHONE';
COMMENT ON COLUMN notification_dead_letter.subject IS '发送主题';
COMMENT ON COLUMN notification_dead_letter.content IS '发送内容';
COMMENT ON COLUMN notification_dead_letter.channel IS '发送渠道:EMAIL/SITE_MSG/SMS/WEBHOOK';
COMMENT ON COLUMN notification_dead_letter.send_status IS '发送状态:3=FAILED';
COMMENT ON COLUMN notification_dead_letter.error_message IS '错误信息';
COMMENT ON COLUMN notification_dead_letter.retry_count IS '已重试次数';
COMMENT ON COLUMN notification_dead_letter.max_retry IS '最大重试次数';
COMMENT ON COLUMN notification_dead_letter.original_message_id IS 'RabbitMQ原始消息ID';
COMMENT ON COLUMN notification_dead_letter.biz_type IS '业务类型标识';
COMMENT ON COLUMN notification_dead_letter.biz_id IS '业务关联ID';
COMMENT ON COLUMN notification_dead_letter.dead_letter_reason IS '死信原因(超过最大重试次数等)';
COMMENT ON COLUMN notification_dead_letter.dead_letter_time IS '进入死信时间';
COMMENT ON COLUMN notification_dead_letter.resolved IS '是否已处理:0=未处理 1=已处理';
COMMENT ON COLUMN notification_dead_letter.resolved_by IS '处理人用户ID';
COMMENT ON COLUMN notification_dead_letter.resolved_time IS '处理时间';
COMMENT ON COLUMN notification_dead_letter.resolve_remark IS '处理备注';
COMMENT ON COLUMN notification_dead_letter.version IS '乐观锁版本号';
COMMENT ON COLUMN notification_dead_letter.deleted IS '逻辑删除标记:0=未删除 1=已删除';
COMMENT ON COLUMN notification_dead_letter.create_time IS '创建时间';
COMMENT ON COLUMN notification_dead_letter.update_time IS '更新时间';

-- 索引（命名符合 idx_{table}_{field} 规范）
CREATE INDEX IF NOT EXISTS idx_dead_letter_channel ON notification_dead_letter(channel);
CREATE INDEX IF NOT EXISTS idx_dead_letter_status ON notification_dead_letter(send_status);
CREATE INDEX IF NOT EXISTS idx_dead_letter_resolved ON notification_dead_letter(resolved);
CREATE INDEX IF NOT EXISTS idx_dead_letter_create_time ON notification_dead_letter(dead_letter_time);
CREATE INDEX IF NOT EXISTS idx_dead_letter_deleted ON notification_dead_letter(deleted);
CREATE INDEX IF NOT EXISTS idx_dead_letter_biz ON notification_dead_letter(biz_type, biz_id);

-- ----------------------------------------------------------
-- 3. 预注册 sprint1.test.event 测试模板
-- 用途: Sprint 1 验收场景 E1（手动触发测试事件）使用
-- 语法: {varName}（spec F-006 要求，与现有占位符模板独立）
-- 对应: plan.md ADR-002（模板渲染语法决策）
-- ----------------------------------------------------------
INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data, version, deleted, create_time, update_time)
VALUES (
    'sprint1.test.event',
    'Sprint1验收测试事件模板',
    '测试事件：{eventName}',
    '这是 Sprint 1 验收测试事件。事件名：{eventName}，操作人：{operatorName}',
    2,
    'SITE_MSG',
    1,
    '{"eventName":"事件名称","operatorName":"操作人名称"}'::jsonb,
    '{"eventName":"测试事件","operatorName":"系统管理员"}'::jsonb,
    0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
) ON CONFLICT (template_code) DO NOTHING;

-- ----------------------------------------------------------
-- 4. 更新 SMTP 配置为 QQ 企业邮箱
-- 用途: spec NC-001 决策（开发环境用 QQ 企业邮箱）
-- 对应: plan.md ADR-007
-- ----------------------------------------------------------
UPDATE notification_setting
SET setting_value = CASE setting_key
    WHEN 'smtp.host' THEN 'smtp.qq.com'
    WHEN 'smtp.port' THEN '587'
    WHEN 'smtp.username' THEN COALESCE(NULLIF(setting_value, ''), 'noreply@example.com')
    ELSE setting_value
END,
    update_time = CURRENT_TIMESTAMP
WHERE setting_key IN ('smtp.host', 'smtp.port', 'smtp.username');

-- 确保 SMTP 配置记录存在（如果之前没有）
INSERT INTO notification_setting (setting_key, setting_value, setting_group, description, is_encrypted, status)
SELECT 'smtp.host', 'smtp.qq.com', 'SMTP', 'SMTP服务器地址(QQ企业邮箱)', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM notification_setting WHERE setting_key = 'smtp.host');

INSERT INTO notification_setting (setting_key, setting_value, setting_group, description, is_encrypted, status)
SELECT 'smtp.port', '587', 'SMTP', 'SMTP服务器端口', 0, 1
WHERE NOT EXISTS (SELECT 1 FROM notification_setting WHERE setting_key = 'smtp.port');

-- ============================================================
-- 迁移完成
-- 验证: 启动应用后检查
--   1. SELECT column_name FROM information_schema.columns WHERE table_name = 'msg_send_record' AND column_name = 'notification_id';
--   2. SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'notification_dead_letter';
--   3. SELECT * FROM msg_template WHERE template_code = 'sprint1.test.event';
--   4. SELECT * FROM notification_setting WHERE setting_key IN ('smtp.host', 'smtp.port');
-- ============================================================
