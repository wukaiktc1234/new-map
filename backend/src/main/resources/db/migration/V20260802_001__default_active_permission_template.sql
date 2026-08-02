-- ============================================================
-- 默认激活权限模板（集中式单店模式）
-- 修复: 预设权限模板未生效（无默认激活 + 无服务端持久化）
-- ============================================================

INSERT INTO system_config (tenant_id, config_key, config_value, config_type, description, encrypted, deleted)
VALUES (0, 'permission.active_template', 'centralized-single', 'permission', '当前激活的权限模板（4 种模式编码）', 0, 0)
ON CONFLICT (tenant_id, config_key) DO NOTHING;
