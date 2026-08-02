-- ============================================================
-- 迁移脚本：为 employees 表添加 health_certificate_expiry_date 字段
-- 说明：用于 HR 系统同步健康证到期日期，修复 6.1.30 健康证→人事管理（存储端占位）
-- ============================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'health_certificate_expiry_date'
    ) THEN
        ALTER TABLE employees ADD COLUMN health_certificate_expiry_date DATE;
        COMMENT ON COLUMN employees.health_certificate_expiry_date IS '健康证到期日期（由HR系统同步维护）';
    END IF;
END $$;
