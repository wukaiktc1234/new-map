-- DEF-3 修复 (P0-MOCK-001)：notification 表补 business_id / business_type 列
-- 背景：实体 Notification 已映射 @TableField("business_id") / @TableField("business_type")，
--       且写侧有 3 处真实消费（NotificationEventBus / SiteMsgChannelSender / NotificationScheduleService），
--       但 V20260425 建表时未包含此两列，导致 MyBatis-Plus 生成 SQL 引用不存在的列（接口 500）。
-- 方案：架构裁决(2026-08-10) 方案A —— 版本化 migration 补列，正式模型保留 businessId/businessType。
-- 兼容性：IF NOT EXISTS 保证可重复执行；既有数据行两列保持 NULL，不破坏现有数据。
ALTER TABLE notification ADD COLUMN IF NOT EXISTS business_id BIGINT NULL;
ALTER TABLE notification ADD COLUMN IF NOT EXISTS business_type VARCHAR(50) NULL;

COMMENT ON COLUMN notification.business_id IS '业务ID（关联业务单据，如采购单/入库单/盘点单等）';
COMMENT ON COLUMN notification.business_type IS '业务类型（如 PURCHASE_ORDER / RECEIPT / INVENTORY_CHECK 等）';
