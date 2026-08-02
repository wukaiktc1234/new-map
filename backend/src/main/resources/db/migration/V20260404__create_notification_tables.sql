-- ============================================================
-- 消息通知模块 - Phase 1 基础框架
-- 创建消息模板表、发送记录表、通知设置表
-- 版本: V20260404__create_notification_tables.sql
-- 数据库: PostgreSQL 18
-- ============================================================

-- ----------------------------------------------------------
-- 表1: msg_template（消息模板表）
-- 用于存储邮件、站内消息、短信等通知模板
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS msg_template (
    template_id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_code     VARCHAR(100) NOT NULL UNIQUE,
    template_name     VARCHAR(200) NOT NULL,
    subject_pattern   VARCHAR(500) NOT NULL,
    content_pattern   TEXT NOT NULL,
    template_type     SMALLINT NOT NULL DEFAULT 1,   -- 1=EMAIL 2=SITE_MSG 3=SMS 4=WEBHOOK
    channel           VARCHAR(50) DEFAULT 'EMAIL',
    status            SMALLINT NOT NULL DEFAULT 1,   -- 0=DISABLED 1=ENABLED 2=DRAFT
    variables         JSONB,                         -- 模板变量定义
    example_data      JSONB,                         -- 示例数据
    version           BIGINT NOT NULL DEFAULT 1,
    deleted           SMALLINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time       TIMESTAMP NOT NULL DEFAULT NOW(),
    create_user_id    BIGINT,
    create_username   VARCHAR(50),
    update_user_id    BIGINT,
    update_username   VARCHAR(50)
);

COMMENT ON TABLE msg_template IS '消息模板表-存储各类通知模板定义';
COMMENT ON COLUMN msg_template.template_id IS '主键ID';
COMMENT ON COLUMN msg_template.template_code IS '模板编码(唯一标识)';
COMMENT ON COLUMN msg_template.template_name IS '模板名称';
COMMENT ON COLUMN msg_template.subject_pattern IS '主题模板(支持变量语法)';
COMMENT ON COLUMN msg_template.content_pattern IS '内容模板(支持变量语法)';
COMMENT ON COLUMN msg_template.template_type IS '模板类型:1=EMAIL 2=SITE_MSG 3=SMS 4=WEBHOOK';
COMMENT ON COLUMN msg_template.channel IS '发送渠道';
COMMENT ON COLUMN msg_template.status IS '状态:0=DISABLED 1=ENABLED 2=DRAFT';
COMMENT ON COLUMN msg_template.variables IS '变量定义(JSONB格式)';
COMMENT ON COLUMN msg_template.example_data IS '示例数据(JSONB格式)';
COMMENT ON COLUMN msg_template.version IS '乐观锁版本号';
COMMENT ON COLUMN msg_template.deleted IS '逻辑删除标记:0=未删除 1=已删除';
COMMENT ON COLUMN msg_template.create_time IS '创建时间';
COMMENT ON COLUMN msg_template.update_time IS '更新时间';

-- 索引
CREATE INDEX IF NOT EXISTS uk_msg_template_code ON msg_template(template_code);
CREATE INDEX IF NOT EXISTS idx_msg_template_type ON msg_template(template_type);
CREATE INDEX IF NOT EXISTS idx_msg_template_status ON msg_template(status);
CREATE INDEX IF NOT EXISTS idx_msg_template_channel ON msg_template(channel);

-- ----------------------------------------------------------
-- 表2: msg_send_record（消息发送记录表）
-- 记录所有消息的发送状态和详细信息
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS msg_send_record (
    record_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    template_id       BIGINT,
    template_code     VARCHAR(100),
    template_name     VARCHAR(200),
    recipient         VARCHAR(500) NOT NULL,          -- 收件人(邮箱/用户ID/手机号)
    recipient_type    SMALLINT NOT NULL DEFAULT 1,   -- 1=EMAIL 2=USER_ID 3=PHONE
    subject           VARCHAR(500),
    content           TEXT,
    channel           VARCHAR(50) NOT NULL,
    send_status       SMALLINT NOT NULL DEFAULT 0,   -- 0=PENDING 1=SENDING 2=SUCCESS 3=FAILED 4=PARTIAL
    error_message     TEXT,
    retry_count       INTEGER DEFAULT 0,
    max_retry         INTEGER DEFAULT 3,
    send_time         TIMESTAMP,
    finish_time       TIMESTAMP,
    trigger_type      SMALLINT DEFAULT 1,            -- 1=MANUAL 2=SYSTEM 3=SCHEDULED 4=EVENT
    biz_type          VARCHAR(100),                  -- 业务类型标识
    biz_id            VARCHAR(100),                  -- 业务关联ID
    version           BIGINT NOT NULL DEFAULT 1,
    deleted           SMALLINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL DEFAULT NOW(),
    create_user_id    BIGINT,
    create_username   VARCHAR(50)
);

COMMENT ON TABLE msg_send_record IS '消息发送记录表-追踪所有消息发送状态';
COMMENT ON COLUMN msg_send_record.record_id IS '主键ID';
COMMENT ON COLUMN msg_send_record.template_id IS '关联模板ID';
COMMENT ON COLUMN msg_send_record.template_code IS '关联模板编码';
COMMENT ON COLUMN msg_send_record.template_name IS '关联模板名称';
COMMENT ON COLUMN msg_send_record.recipient IS '收件人地址/ID';
COMMENT ON COLUMN msg_send_record.recipient_type IS '收件人类型:1=EMAIL 2=USER_ID 3=PHONE';
COMMENT ON COLUMN msg_send_record.subject IS '实际发送主题';
COMMENT ON COLUMN msg_send_record.content IS '实际发送内容';
COMMENT ON COLUMN msg_send_record.channel IS '发送渠道';
COMMENT ON COLUMN msg_send_record.send_status IS '发送状态:0=PENDING 1=SENDING 2=SUCCESS 3=FAILED 4=PARTIAL';
COMMENT ON COLUMN msg_send_record.error_message IS '错误信息';
COMMENT ON COLUMN msg_send_record.retry_count IS '已重试次数';
COMMENT ON COLUMN msg_send_record.max_retry IS '最大重试次数';
COMMENT ON COLUMN msg_send_record.send_time IS '开始发送时间';
COMMENT ON COLUMN msg_send_record.finish_time IS '完成时间';
COMMENT ON COLUMN msg_send_record.trigger_type IS '触发类型:1=MANUAL 2=SYSTEM 3=SCHEDULED 4=EVENT';
COMMENT ON COLUMN msg_send_record.biz_type IS '业务类型标识';
COMMENT ON COLUMN msg_send_record.biz_id IS '业务关联ID';
COMMENT ON COLUMN msg_send_record.version IS '乐观锁版本号';
COMMENT ON COLUMN msg_send_record.deleted IS '逻辑删除标记:0=未删除 1=已删除';
COMMENT ON COLUMN msg_send_record.create_time IS '创建时间';

-- 索引
CREATE INDEX IF NOT EXISTS idx_msg_send_record_template ON msg_send_record(template_id);
CREATE INDEX IF NOT EXISTS idx_msg_send_record_recipient ON msg_send_record(recipient);
CREATE INDEX IF NOT EXISTS idx_msg_send_record_status ON msg_send_record(send_status);
CREATE INDEX IF NOT EXISTS idx_msg_send_record_channel ON msg_send_record(channel);
CREATE INDEX IF NOT EXISTS idx_msg_send_record_biz ON msg_send_record(biz_type, biz_id);
CREATE INDEX IF NOT EXISTS idx_msg_send_record_create_time ON msg_send_record(create_time);

-- ----------------------------------------------------------
-- 表3: notification_setting（通知设置表）
-- 存储SMTP等通知通道配置信息
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_setting (
    setting_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    setting_key       VARCHAR(100) NOT NULL UNIQUE,
    setting_value     TEXT,
    setting_group     VARCHAR(50) DEFAULT 'GENERAL',
    description       VARCHAR(500),
    is_encrypted      SMALLINT NOT NULL DEFAULT 0,
    status            SMALLINT NOT NULL DEFAULT 1,
    version           BIGINT NOT NULL DEFAULT 1,
    deleted           SMALLINT NOT NULL DEFAULT 0,
    create_time       TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time       TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE notification_setting IS '通知设置表-存储SMTP等通道配置';
COMMENT ON COLUMN notification_setting.setting_id IS '主键ID';
COMMENT ON COLUMN notification_setting.setting_key IS '设置键(唯一)';
COMMENT ON COLUMN notification_setting.setting_value IS '设置值';
COMMENT ON COLUMN notification_setting.setting_group IS '设置分组';
COMMENT ON COLUMN notification_setting.description IS '描述说明';
COMMENT ON COLUMN notification_setting.is_encrypted IS '是否加密存储:0=否 1=是';
COMMENT ON COLUMN notification_setting.status IS '状态:0=DISABLED 1=ENABLED';
COMMENT ON COLUMN notification_setting.version IS '乐观锁版本号';
COMMENT ON COLUMN notification_setting.deleted IS '逻辑删除标记:0=未删除 1=已删除';
COMMENT ON COLUMN notification_setting.create_time IS '创建时间';
COMMENT ON COLUMN notification_setting.update_time IS '更新时间';

-- 索引
CREATE INDEX IF NOT EXISTS uk_notification_setting_key ON notification_setting(setting_key);
CREATE INDEX IF NOT EXISTS idx_notification_setting_group ON notification_setting(setting_group);

-- ============================================================
-- 种子数据：消息模板（4条）
-- ============================================================

INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data, create_user_id, create_username) VALUES
('welcome-email', '用户注册欢迎邮件', '欢迎加入食品溯源系统 - ${userName}', '<h3>尊敬的 ${userName}：</h3><p>欢迎注册食品溯源系统！您的账号已成功创建。</p><p>登录名：${userEmail}</p><p>注册时间：${registerTime}</p><p>请妥善保管您的账户信息。</p>', 1, 'EMAIL', 1, '["userName", "userEmail", "registerTime"]'::jsonb, '{"userName": "张三", "userEmail": "zhangsan@example.com", "registerTime": "2026-04-04 10:00:00"}'::jsonb, 1, 'system'),

('password-reset', '密码重置邮件', '【食品溯源系统】密码重置验证码', '<h3>尊敬的用户：</h3><p>您正在申请重置密码，验证码如下：</p><h2 style="color:#1890ff;text-align:center;">${verifyCode}</h2><p>验证码有效时间为 ${expireMinutes} 分钟，请尽快完成重置操作。</p><p>如果这不是您本人的操作，请忽略此邮件。</p>', 1, 'EMAIL', 1, '["verifyCode", "expireMinutes"]'::jsonb, '{"verifyCode": "123456", "expireMinutes": "15"}'::jsonb, 1, 'system'),

('order-notification', '订单状态变更通知', '订单${orderNo}状态更新：${orderStatus}', '<p>您好 ${userName}，</p><p>您的订单 <strong>${orderNo}</strong> 状态已更新为：<strong>${orderStatus}</strong></p><p>更新时间：${updateTime}</p><p>备注：${remark}</p>', 2, 'SITE_MSG', 1, '["userName", "orderNo", "orderStatus", "updateTime", "remark"]'::jsonb, '{"userName": "李四", "orderNo": "ORD20260404001", "orderStatus": "已发货", "updateTime": "2026-04-04 14:30:00", "remark": "已通过快递发出"}'::jsonb, 1, 'system'),

('system-alert', '系统告警通知', '【系统告警】${alertLevel}: ${alertTitle}', '<h3 style="color:red;">系统告警通知</h3><p><strong>告警级别：</strong>${alertLevel}</p><p><strong>告警标题：</strong>${alertTitle}</p><p><strong>告警内容：</strong>${alertContent}</p><p><strong>发生时间：</strong>${alertTime}</p><p><strong>影响范围：</strong>${affectScope}</p><p>请及时处理！</p>', 1, 'EMAIL', 1, '["alertLevel", "alertTitle", "alertContent", "alertTime", "affectScope"]'::jsonb, '{"alertLevel": "严重", "alertTitle": "库存预警", "alertContent": "食材A库存低于安全阈值", "alertTime": "2026-04-04 09:00:00", "affectScope": "厨房A区"}'::jsonb, 1, 'system');

-- ============================================================
-- 种子数据：通知设置（6条SMTP配置）
-- ============================================================

INSERT INTO notification_setting (setting_key, setting_value, setting_group, description, is_encrypted, status) VALUES
('smtp.host', 'smtp.example.com', 'SMTP', 'SMTP服务器地址', 0, 1),
('smtp.port', '587', 'SMTP', 'SMTP服务器端口', 0, 1),
('smtp.username', 'noreply@example.com', 'SMTP', 'SMTP认证用户名', 0, 1),
('smtp.password', 'ENC(encrypted_password_placeholder)', 'SMTP', 'SMTP认证密码(加密存储)', 1, 1),
('smtp.from_address', 'noreply@example.com', 'SMTP', '发件人邮箱地址', 0, 1),
('smtp.enable_ssl', 'true', 'SMTP', '是否启用SSL/TLS加密', 0, 1);
