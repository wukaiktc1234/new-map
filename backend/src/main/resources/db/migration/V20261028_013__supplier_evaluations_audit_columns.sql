-- =====================================================================
-- V20261028_013__supplier_evaluations_audit_columns.sql
-- 起因：实体 SupplierEvaluation 有 create_time / update_time / deleted 三列（@TableField 显式声明），
--       但 V20261028_010 建表时漏了 → 查询报「字段 create_time 不存在」→ /v1/supplier-evaluations/* 500。
-- 修法：补三列（含默认值），并对齐实体类型。
-- =====================================================================

ALTER TABLE supplier_evaluations ADD COLUMN IF NOT EXISTS create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE supplier_evaluations ADD COLUMN IF NOT EXISTS update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE supplier_evaluations ADD COLUMN IF NOT EXISTS deleted integer NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_se_supplier_period_time ON supplier_evaluations (supplier_id, evaluation_period DESC);
