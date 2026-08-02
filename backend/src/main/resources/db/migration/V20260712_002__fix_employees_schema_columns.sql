-- ============================================================
-- 修复 employees 表字段名与 Entity 实体映射不一致
-- 版本: V20260712_002
-- 说明: V1.0.0.100 创建 employees 表时使用 created_at，但
--       Employee 实体使用 @TableField("created_time") 和
--       @TableField("updated_time")，导致 SQL 查询报错
--       "column created_time does not exist"。
--       修复措施：
--       1. Employee.java 已将 @TableField 改为 created_at/updated_at
--       2. 本脚本为 employees 表添加 updated_at/created_by/updated_by
--          （created_at 已存在，无需添加）
-- ============================================================

DO $$
BEGIN
    -- 添加 updated_at 字段（如果不存在）
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE employees ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
        COMMENT ON COLUMN employees.updated_at IS '更新时间';
    END IF;

    -- 添加 created_by 字段（如果不存在）
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'created_by'
    ) THEN
        ALTER TABLE employees ADD COLUMN created_by VARCHAR(50);
        COMMENT ON COLUMN employees.created_by IS '创建人';
    END IF;

    -- 添加 updated_by 字段（如果不存在）
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'updated_by'
    ) THEN
        ALTER TABLE employees ADD COLUMN updated_by VARCHAR(50);
        COMMENT ON COLUMN employees.updated_by IS '更新人';
    END IF;
END $$;