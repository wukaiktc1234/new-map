-- ============================================================
-- SR-9: centralized-single 模式按角色开放财务/采购
-- 决策（2026-07-31）：单店模式下 admin/owner（老板/管理员）显示「财务+采购」，
-- 店长及以下角色不显示（避免员工掌控财务与采购业务）。
-- 菜单最终可见性由 visibleRoles + 角色域矩阵双重过滤。
-- ============================================================

UPDATE permission_templates
SET role_config = '{"admin":["workspace","store-ops","product","order","operations","member","traceability","device","system","finance","purchase"],"owner":["workspace","store-ops","product","order","operations","member","traceability","device","system","finance","purchase"],"hr_director":["workspace","hr"],"finance_director":["workspace","finance"],"ops_director":["workspace","store-ops","order","operations"],"employee":["workspace"]}',
    update_time = CURRENT_TIMESTAMP
WHERE code = 'centralized-single';
