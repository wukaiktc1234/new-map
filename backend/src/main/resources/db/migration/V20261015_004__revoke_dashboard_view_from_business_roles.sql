-- ============================================================
-- P1-DASHBOARD-FILTER-001 前置：撤销误授的 operations:dashboard:view（Owner 裁决 B）
--
-- 背景：
--   2026-10-22 19:55:09，13 个业务角色被批量授予 operations:dashboard:view
--   （当时为排查「今日销售/会员增长 403」而执行的临时方案 A）。
--   Owner 随后裁决改走方案 B（前端按权限隐藏卡片），并明确指出：
--   **DashboardController 未做门店级数据过滤，放开该权限等于让店长/收银员看到全公司流水，属严重越权。**
--   故本迁移撤销这批授予，仅保留原有合法持有者：
--     ROLE_ADMIN / ROLE_OWNER / '1'（admin 悬空 id）/ ROLE_OPS_DIRECTOR
--
-- 关联：board §24.18a P1-DASHBOARD-FILTER-001（补门店过滤后再按需重新授权）
--
-- 幂等：DELETE 重复执行无副作用。
-- 回退：重新 INSERT 即可（见 board §24.18a 记录的角色清单）。
-- ============================================================

DELETE FROM role_permissions rp
USING permissions p
WHERE rp.permission_id = p.permission_id_str
  AND p.permission_code = 'operations:dashboard:view'
  AND rp.role_id IN (
      'ROLE_STORE_MANAGER',
      'ROLE_CASHIER',
      'ROLE_POS_OPERATOR',
      'ROLE_CHEF',
      'ROLE_KITCHEN',
      'ROLE_TEAM_LEADER',
      'ROLE_REGION_MANAGER',
      'ROLE_DEPARTMENT_MANAGER',
      'ROLE_WAREHOUSE_MANAGER',
      'ROLE_PURCHASE_MANAGER',
      'ROLE_HR_DIRECTOR',
      'ROLE_FINANCE_DIRECTOR',
      'ROLE_AUDITOR'
  );

-- 自检（执行后人工核对，期望只余 4 个合法持有者）
-- SELECT rp.role_id FROM role_permissions rp
--   JOIN permissions p ON p.permission_id_str = rp.permission_id
--  WHERE p.permission_code = 'operations:dashboard:view' AND rp.deleted = 0
--  ORDER BY rp.role_id;
