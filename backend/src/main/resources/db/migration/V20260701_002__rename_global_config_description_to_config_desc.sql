-- ============================================================
-- C13: 统一 global_config 字段名为 config_desc
-- ============================================================
-- 问题背景：
--   实体 GlobalConfig.java 使用 @TableField("config_desc")
--   H2 环境 BaseDatabaseInitializer.createGlobalConfigTable() 已使用 config_desc
--   但 PostgreSQL 生产环境 V1.0.0.100__init_postgresql.sql 使用 description
--   导致 PostgreSQL 环境下 GlobalConfigMapper 查询 config_desc 列时报错
--
-- 修复方案：
--   将 PostgreSQL 中 global_config.description 重命名为 config_desc
--   使用 information_schema 条件判断，保证脚本可重复执行（幂等）
-- ============================================================

DO $$
BEGIN
    -- 仅当 description 列存在时执行重命名，避免重复执行报错
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'global_config' AND column_name = 'description'
    ) THEN
        ALTER TABLE global_config RENAME COLUMN description TO config_desc;
    END IF;
END $$;
