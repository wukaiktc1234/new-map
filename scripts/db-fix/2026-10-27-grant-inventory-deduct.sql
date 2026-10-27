-- =====================================================================
-- 2026-10-27-grant-inventory-deduct.sql
-- inventory:deduct 正式授权（此前 0 行绑定 → 所有账号 403，接口形同虚设）
--   绑 ROLE_STORE_MANAGER + ROLE_FINANCE_DIRECTOR；
--   **不绑 ROLE_CASHIER**（扣料应由系统内部调用，不给收银员直调权限）。
--   幂等；回撤：DELETE FROM role_permissions WHERE permission_id='inventory:deduct'
--             AND role_id IN ('ROLE_STORE_MANAGER','ROLE_FINANCE_DIRECTOR');
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT r.role_id, 'inventory:deduct', 0, now()
  FROM (VALUES ('ROLE_STORE_MANAGER'), ('ROLE_FINANCE_DIRECTOR')) AS r(role_id)
 WHERE NOT EXISTS (
   SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.role_id AND rp.permission_id = 'inventory:deduct' AND rp.deleted = 0);
COMMIT;
SELECT role_id, permission_id, deleted
  FROM role_permissions
 WHERE permission_id = 'inventory:deduct'
 ORDER BY role_id;