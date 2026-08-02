-- ============================================================
-- V20260731_003：补充采购订单主表缺失字段
-- 背景：
--   前端采购订单页展示/编辑的 purchase_type、contact_person、
--   contact_phone、paid_amount、budget_id、budget_status 等字段
--   在 purchase_orders 表中不存在，导致数据无法持久化。
-- 操作：
--   1. 添加上述列（IF NOT EXISTS 幂等）
--   2. 添加字段注释
-- 兼容性：
--   H2 环境 Flyway 禁用，由 PurchaseDatabaseInitializer 维护；
--   本脚本仅在 PostgreSQL 生产环境执行。
-- ============================================================

ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS purchase_type  VARCHAR(50);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS contact_person VARCHAR(100);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS contact_phone  VARCHAR(50);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS paid_amount     BIGINT DEFAULT 0;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS budget_id       VARCHAR(50);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS budget_status   VARCHAR(50);

COMMENT ON COLUMN purchase_orders.purchase_type  IS '采购类型（direct-直采，agency-代采等）';
COMMENT ON COLUMN purchase_orders.contact_person IS '联系人';
COMMENT ON COLUMN purchase_orders.contact_phone  IS '联系电话';
COMMENT ON COLUMN purchase_orders.paid_amount    IS '已付金额（单位：分）';
COMMENT ON COLUMN purchase_orders.budget_id      IS '预算ID';
COMMENT ON COLUMN purchase_orders.budget_status  IS '预算状态';
