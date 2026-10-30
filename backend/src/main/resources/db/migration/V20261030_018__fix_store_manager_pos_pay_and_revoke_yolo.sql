-- V20261030_018__fix_store_manager_pos_pay_and_revoke_yolo.sql
--
-- Corrects V20261030_017 (already applied, success=true, must not be edited).
-- 017 granted pos:yolo:recognize to ROLE_STORE_MANAGER but NOT pos:order:pay.
-- Product decision (Lead 2026-10-30): store manager must be able to take payments (pos:order:pay) and
-- pos:yolo:recognize is cashier-only. Target set for store manager (7 codes):
--   pos:order:create / pos:order:pay / pos:order:query / pos:order:void
--   pos:shift:manage / pos:member:query / pos:menu:query
-- Effect: 7 stays 7, but composition becomes correct and survives removal of the code
-- constant POS_BASE_PERMISSIONS (which currently supplies pos:order:pay).
--
-- Pure ASCII. Idempotent. Asserts composition, not only counts.

-- 1) grant pos:order:pay
INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_STORE_MANAGER', p.permission_id_str, 0, NOW()
  FROM permissions p
 WHERE p.permission_code = 'pos:order:pay' AND p.deleted = 0
ON CONFLICT (role_id, permission_id) DO UPDATE SET deleted = 0;

-- 2) revoke pos:yolo:recognize (cashier-only)
UPDATE role_permissions rp
   SET deleted = 1
  FROM permissions p
 WHERE p.permission_id_str = rp.permission_id
   AND rp.role_id = 'ROLE_STORE_MANAGER'
   AND p.permission_code = 'pos:yolo:recognize'
   AND rp.deleted = 0;

DO $$
DECLARE
    v_mgr bigint;
    v_has_pay int;
    v_has_yolo int;
    v_cashier bigint;
BEGIN
    SELECT count(*) INTO v_mgr
      FROM role_permissions rp
      JOIN permissions p ON p.permission_id_str = rp.permission_id AND p.deleted = 0
     WHERE rp.role_id = 'ROLE_STORE_MANAGER' AND rp.deleted = 0
       AND p.permission_code LIKE 'pos:%';
    SELECT count(*) INTO v_has_pay
      FROM role_permissions rp
      JOIN permissions p ON p.permission_id_str = rp.permission_id AND p.deleted = 0
     WHERE rp.role_id = 'ROLE_STORE_MANAGER' AND rp.deleted = 0
       AND p.permission_code = 'pos:order:pay';
    SELECT count(*) INTO v_has_yolo
      FROM role_permissions rp
      JOIN permissions p ON p.permission_id_str = rp.permission_id AND p.deleted = 0
     WHERE rp.role_id = 'ROLE_STORE_MANAGER' AND rp.deleted = 0
       AND p.permission_code = 'pos:yolo:recognize';
    SELECT count(*) INTO v_cashier
      FROM role_permissions rp
      JOIN permissions p ON p.permission_id_str = rp.permission_id AND p.deleted = 0
     WHERE rp.role_id = 'ROLE_CASHIER' AND rp.deleted = 0
       AND p.permission_code LIKE 'pos:%';
    IF v_mgr <> 7 THEN
        RAISE EXCEPTION '018 assertion failed: ROLE_STORE_MANAGER pos codes = %, expected 7', v_mgr;
    END IF;
    IF v_has_pay <> 1 THEN
        RAISE EXCEPTION '018 assertion failed: ROLE_STORE_MANAGER missing pos:order:pay';
    END IF;
    IF v_has_yolo <> 0 THEN
        RAISE EXCEPTION '018 assertion failed: ROLE_STORE_MANAGER still has pos:yolo:recognize';
    END IF;
    IF v_cashier <> 10 THEN
        RAISE EXCEPTION '018 assertion failed: ROLE_CASHIER pos codes = %, expected 10', v_cashier;
    END IF;
    RAISE NOTICE '018 OK: store_manager=% (pay=1 yolo=0) cashier=%', v_mgr, v_cashier;
END $$;
