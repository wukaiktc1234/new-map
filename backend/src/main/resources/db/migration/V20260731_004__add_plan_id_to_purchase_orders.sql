-- ============================================================
-- V20260731_004：采购订单表新增 plan_id 字段
-- 背景：
--   LK-PURCHASE-04 数据链路修复：采购订单需关联采购计划
--   （purchase_plan.plan_id），以支持"订单全部入库后回写计划
--   状态为已完成"的链路闭环。
-- 操作：
--   1. 添加 plan_id 列（IF NOT EXISTS 幂等）
--   2. 添加字段注释
--   3. 创建 plan_id 索引（用于按计划查询订单列表）
-- 兼容性：
--   H2 环境 Flyway 禁用，由 PurchaseDatabaseInitializer 维护；
--   本脚本仅在 PostgreSQL 生产环境执行。
-- ============================================================

ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS plan_id BIGINT;

COMMENT ON COLUMN purchase_orders.plan_id IS '关联采购计划ID（purchase_plan.plan_id）';

CREATE INDEX IF NOT EXISTS idx_purchase_orders_plan_id ON purchase_orders (plan_id);
