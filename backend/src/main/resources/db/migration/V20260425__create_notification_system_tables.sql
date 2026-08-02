-- 消息与通知系统增强迁移脚本
-- 补全notification表规范字段，新增alert持久化表、用户通知偏好表

-- 创建notification表（V20260404未创建此表，此处补充）
CREATE TABLE IF NOT EXISTS notification (
    notification_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            BIGINT NOT NULL,
    type               VARCHAR(50) NOT NULL,
    title              VARCHAR(200),
    content            TEXT,
    is_read            INTEGER NOT NULL DEFAULT 0,
    read_time          TIMESTAMP,
    create_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE notification IS '通知消息表';
COMMENT ON COLUMN notification.user_id IS '接收用户ID';
COMMENT ON COLUMN notification.type IS '通知类型';
COMMENT ON COLUMN notification.is_read IS '是否已读：0未读 1已读';

-- 1. 补全notification表缺失的规范字段
ALTER TABLE notification ADD COLUMN IF NOT EXISTS update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS deleted INTEGER NOT NULL DEFAULT 0;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL';
ALTER TABLE notification ADD COLUMN IF NOT EXISTS sender_id BIGINT;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS sender_name VARCHAR(100);
ALTER TABLE notification ADD COLUMN IF NOT EXISTS extra_data VARCHAR(2000);

-- 扩展notification.type枚举值，增加更多通知类型
-- 原有: REJECT_AUDIT, SYSTEM
-- 新增: APPROVE_AUDIT, PENDING_AUDIT, INVENTORY_WARNING, CONTRACT_EXPIRY,
--       HEALTH_CERT_EXPIRY, TASK_ASSIGN, TASK_COMPLETE, ORDER_STATUS

-- 为notification表添加索引
CREATE INDEX IF NOT EXISTS idx_notification_user_id ON notification(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_type ON notification(type);
CREATE INDEX IF NOT EXISTS idx_notification_is_read ON notification(is_read);
CREATE INDEX IF NOT EXISTS idx_notification_create_time ON notification(create_time);
CREATE INDEX IF NOT EXISTS idx_notification_deleted ON notification(deleted);

-- 2. 创建alert持久化表（替代内存HashMap存储）
CREATE TABLE IF NOT EXISTS alert (
    alert_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    alert_name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    alert_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    alert_value DOUBLE PRECISION,
    threshold_value DOUBLE PRECISION,
    source VARCHAR(100),
    source_type VARCHAR(50),
    source_id VARCHAR(100),
    alert_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolve_time TIMESTAMP,
    acknowledge_time TIMESTAMP,
    acknowledged_by VARCHAR(100),
    resolved_by VARCHAR(100),
    resolve_description VARCHAR(500),
    assigned_to BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_alert_status ON alert(alert_status);
CREATE INDEX IF NOT EXISTS idx_alert_severity ON alert(severity);
CREATE INDEX IF NOT EXISTS idx_alert_source ON alert(source);
CREATE INDEX IF NOT EXISTS idx_alert_create_time ON alert(alert_time);
CREATE INDEX IF NOT EXISTS idx_alert_deleted ON alert(deleted);

-- 3. 创建用户通知偏好表（替代原NotificationSetting的用户偏好功能）
CREATE TABLE IF NOT EXISTS notification_user_preference (
    preference_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    is_enabled INTEGER NOT NULL DEFAULT 1,
    frequency VARCHAR(20) NOT NULL DEFAULT 'REALTIME',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_pref_type_channel UNIQUE (user_id, notification_type, channel)
);

CREATE INDEX IF NOT EXISTS idx_user_pref_user_id ON notification_user_preference(user_id);
CREATE INDEX IF NOT EXISTS idx_user_pref_type ON notification_user_preference(notification_type);

-- 4. 保持原notification_setting表名不变（NotificationSettingEntity仍映射此表）
-- 用户通知偏好使用独立的notification_user_preference表

-- 5. 插入默认通知模板（新增业务场景模板）
INSERT INTO msg_template (template_code, template_name, subject_pattern, content_pattern, template_type, channel, status, variables, example_data, version, deleted, create_time, update_time)
VALUES
('inventory-warning', '库存预警通知', '【库存预警】${materialName}库存不足', '尊敬的用户，${materialName}当前库存为${currentStock}${unit}，已低于预警阈值${threshold}${unit}，请及时补货。仓库：${warehouseName}', 2, 'SITE_MSG', 1, '{"materialName":"物料名称","currentStock":"当前库存","unit":"单位","threshold":"预警阈值","warehouseName":"仓库名称"}', '{"materialName":"食用油","currentStock":"5","unit":"升","threshold":"10","warehouseName":"主仓库"}', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('contract-expiry', '合同到期提醒', '【合同到期】${employeeName}的劳动合同即将到期', '${employeeName}的劳动合同将于${expiryDate}到期，合同编号：${contractNo}，请及时处理续签事宜。', 2, 'SITE_MSG', 1, '{"employeeName":"员工姓名","expiryDate":"到期日期","contractNo":"合同编号"}', '{"employeeName":"张三","expiryDate":"2026-05-30","contractNo":"HT-2024-001"}', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('health-cert-expiry', '健康证到期提醒', '【健康证到期】${employeeName}的健康证即将到期', '${employeeName}的健康证将于${expiryDate}到期，证件编号：${certNo}，请及时安排体检换证。', 2, 'SITE_MSG', 1, '{"employeeName":"员工姓名","expiryDate":"到期日期","certNo":"证件编号"}', '{"employeeName":"李四","expiryDate":"2026-06-15","certNo":"JKZ-2025-123"}', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('pending-audit', '待审核提醒', '【待审核】${bizTypeName}待您审核', '您有一条${bizTypeName}待审核，提交人：${submitterName}，提交时间：${submitTime}，请及时处理。', 2, 'SITE_MSG', 1, '{"bizTypeName":"业务类型名称","submitterName":"提交人","submitTime":"提交时间"}', '{"bizTypeName":"采购申请","submitterName":"王五","submitTime":"2026-04-25 10:00"}', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('task-assign', '任务分配通知', '【新任务】${taskName}', '您被分配了一个新任务：${taskName}，任务类型：${taskType}，截止时间：${deadline}，请及时处理。', 2, 'SITE_MSG', 1, '{"taskName":"任务名称","taskType":"任务类型","deadline":"截止时间"}', '{"taskName":"盘点任务","taskType":"库存盘点","deadline":"2026-04-30"}', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (template_code) DO NOTHING;
