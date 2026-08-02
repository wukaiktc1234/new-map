-- ============================================================
-- 为 employees 表添加 employment_type 列
-- 版本: V20260712_001
-- 说明: Employee 实体包含 employmentType 字段（@TableField("employment_type")），
--       但数据库缺少该列，导致 QueryWrapper<Employee> 生成 SELECT * 时出错。
--       注意：V4.0.0 迁移添加的是 employee_type（INTEGER），而实体需要
--       employment_type（VARCHAR），两者不同。
-- ============================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'employment_type'
    ) THEN
        ALTER TABLE employees ADD COLUMN employment_type VARCHAR(20) DEFAULT 'full-time';
        COMMENT ON COLUMN employees.employment_type IS '用工类型（full-time: 全职, part-time: 兼职）';
    END IF;
END $$;