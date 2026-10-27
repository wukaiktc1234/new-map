-- =====================================================================
-- 2026-10-27-grant-inventory-deduct.sql  （已修正 id 格式）
-- ⚠️ 关键：role_permissions.permission_id 必须等于 permissions.permission_id_str（形如 PERM_INVENTORY_DEDUCT），
--          **不是**权限码 inventory:deduct —— 用权限码插入的行装载时 join 不上，等于没授权（本轮踩到）。
-- 授权对象：ROLE_STORE_MANAGER（已具备，幂等跳过）+ ROLE_FINANCE_DIRECTOR（新增）
-- 不绑 ROLE_CASHIER（扣料由系统内部调用，不给收银员直调权限）
-- 回撤：DELETE FROM role_permissions WHERE permission_id='PERM_INVENTORY_DEDUCT'
--       AND role_id='ROLE_FINANCE_DIRECTOR';
-- =====================================================================
\set ON_ERROR_STOP on
BEGIN;
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT r.role_id, p.permission_id_str, 0, now()
  FROM (VALUES ('ROLE_STORE_MANAGER'), ('ROLE_FINANCE_DIRECTOR')) AS r(role_id)
 CROSS JOIN permissions p
 WHERE p.permission_code = 'inventory:deduct' AND p.deleted = 0 AND p.status = 1
   AND NOT EXISTS (
     SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.role_id AND rp.permission_id = p.permission_id_str AND rp.deleted = 0);
-- 清掉本轮误插的「权限码形态」冗余行
DELETE FROM role_permissions WHERE permission_id = 'inventory:deduct';
COMMIT;
SELECT rp.permission_id, string_agg(rp.role_id, ',' ORDER BY rp.role_id) AS roles
  FROM role_permissions rp
 WHERE rp.permission_id = (SELECT permission_id_str FROM permissions WHERE permission_code='inventory:deduct')
   AND rp.deleted = 0
 GROUP BY rp.permission_id;