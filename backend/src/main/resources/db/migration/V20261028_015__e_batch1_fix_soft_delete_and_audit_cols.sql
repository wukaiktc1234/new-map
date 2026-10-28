-- =====================================================================
-- V20261028_015__e_batch1_fix_soft_delete_and_audit_cols.sql
--
-- 起因：V20261028_014 建 hardware_config / hardware_config_version 时按实体字段建列
--       （实体无 deleted / update_time），但 Owner 新表标准要求：必须有 deleted 软删 + create_time/update_time 审计列。
-- 处置：**补列**（表对齐标准），不放宽标准。
--
-- ⚠ 已知缺口（如实记录）：实体 HardwareConfig / HardwareConfigVersion 目前没有 deleted / updateTime 字段，
--   因此这两列**当前不会被代码使用**（查询不会带 deleted=0 过滤）；
--   补齐实体字段 + 查询过滤属代码侧改造，登记在 docs/audit/tech-debt-schema.md。
--
-- 时间列使用 timestamp without time zone（单时区假设，见 tech-debt-schema.md P0-1）。
-- =====================================================================

ALTER TABLE hardware_config ADD COLUMN IF NOT EXISTS deleted integer NOT NULL DEFAULT 0;
ALTER TABLE hardware_config_version ADD COLUMN IF NOT EXISTS deleted integer NOT NULL DEFAULT 0;
ALTER TABLE hardware_config_version ADD COLUMN IF NOT EXISTS update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_hc_deleted ON hardware_config (deleted);
CREATE INDEX IF NOT EXISTS idx_hcv_deleted ON hardware_config_version (deleted);
