-- ============================================================
-- 为采购退货主表增加备注字段
-- 创建时间：2026-07-20
-- 兼容 H2 (MODE=PostgreSQL) + PostgreSQL 18
-- ============================================================

ALTER TABLE purchase_returns ADD COLUMN IF NOT EXISTS remark TEXT;

COMMENT ON COLUMN purchase_returns.remark IS '备注';
