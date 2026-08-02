-- =====================================================
-- 库存设置表迁移脚本（PostgreSQL兼容）
-- 版本：V3.1.0
-- 说明：将库存设置从静态HashMap迁移到数据库持久化存储
-- =====================================================

-- 创建库存设置表
CREATE TABLE IF NOT EXISTS inventory_settings (
    setting_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    setting_type VARCHAR(20) NOT NULL DEFAULT 'string',
    description VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

-- 创建唯一索引（仅对未删除记录生效，使用部分索引）
CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_settings_key
    ON inventory_settings(setting_key) WHERE deleted = 0;

-- 插入默认库存设置数据（PostgreSQL使用INSERT ON CONFLICT）
INSERT INTO inventory_settings (setting_key, setting_value, setting_type, description)
VALUES
    ('warningThreshold', '10', 'integer', '库存预警阈值'),
    ('autoGeneratePurchaseOrder', 'false', 'boolean', '是否自动生成采购订单'),
    ('checkFrequency', 'monthly', 'string', '盘点频率'),
    ('remark', '默认库存设置', 'string', '备注')
ON CONFLICT (setting_key) WHERE deleted = 0 DO NOTHING;
