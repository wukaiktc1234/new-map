-- Flyway Migration Script
-- Version: V4
-- Description: 为sys_settings表添加索引
-- Author: System
-- Date: 2026-02-06

-- ============================================
-- 为sys_settings表添加索引以优化查询性能
-- ============================================

-- 为setting_key字段添加索引（用于按key查询）
CREATE INDEX IF NOT EXISTS idx_setting_key ON sys_settings(setting_key);

-- 为scope_type和scope_id字段添加复合索引（用于按范围查询）
CREATE INDEX IF NOT EXISTS idx_scope_type_id ON sys_settings(scope_type, scope_id);

-- 为setting_group字段添加索引（用于按分组查询）
CREATE INDEX IF NOT EXISTS idx_setting_group ON sys_settings(setting_group);

-- 为is_enabled字段添加索引（用于过滤启用的设置）
CREATE INDEX IF NOT EXISTS idx_is_enabled ON sys_settings(is_enabled);

-- 为scope_type字段添加索引（用于按类型查询）
CREATE INDEX IF NOT EXISTS idx_scope_type ON sys_settings(scope_type);
