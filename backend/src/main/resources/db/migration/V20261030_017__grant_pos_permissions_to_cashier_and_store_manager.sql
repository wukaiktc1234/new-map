-- V20261030_017__grant_pos_permissions_to_cashier_and_store_manager.sql
--
-- P0 fix (Lead 2026-10-30): POS permissions exist only in role_permissions for ROLE_CASHIER
-- partially, and not at all for ROLE_STORE_MANAGER. Runtime permission for POS users was
-- silently completed by the code constant POS_BASE_PERMISSIONS, which is a design-level
-- privilege escalation (any POS login gets those codes regardless of role config).
--
-- This migration moves the DB side to the intended state so that POS_BASE_PERMISSIONS can be
-- removed afterwards WITHOUT dropping POS capability:
--   ROLE_CASHIER       : 5 existing + 5 granted here  = 10 pos codes (full POS set)
--   ROLE_STORE_MANAGER : 0 existing + 7 granted here  =  7 pos codes (no cashier-only writes)
--
-- Cashier-only write codes NOT granted to store manager (business decision, pending product):
--   pos:member:consume / pos:member:recharge / pos:yolo:recognize
--
-- Idempotent: ON CONFLICT on uk_role_permissions_role_perm (role_id, permission_id).
-- Pure ASCII (Owner red line). Role-level assertion only, no global totals.

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT v.role_id, p.permission_id_str, 0, NOW()
  FROM (VALUES
    ('ROLE_CASHIER', 'pos:member:query'),
    ('ROLE_CASHIER', 'pos:menu:query'),
    ('ROLE_CASHIER', 'pos:order:create'),
    ('ROLE_CASHIER', 'pos:order:query'),
    ('ROLE_CASHIER', 'pos:yolo:recognize'),
    ('ROLE_STORE_MANAGER', 'pos:member:query'),
    ('ROLE_STORE_MANAGER', 'pos:menu:query'),
    ('ROLE_STORE_MANAGER', 'pos:order:create'),
    ('ROLE_STORE_MANAGER', 'pos:order:query'),
    ('ROLE_STORE_MANAGER', 'pos:yolo:recognize'),
    ('ROLE_STORE_MANAGER', 'pos:order:void'),
    ('ROLE_STORE_MANAGER', 'pos:shift:manage')
  ) AS v(role_id, permission_code)
  JOIN permissions p ON p.permission_code = v.permission_code AND p.deleted = 0
ON CONFLICT (role_id, permission_id) DO UPDATE SET deleted = 0;

DO $$
DECLARE
    v_cashier bigint;
    v_manager bigint;
BEGIN
    SELECT count(*) INTO v_cashier
      FROM role_permissions rp
      JOIN permissions p ON p.permission_id_str = rp.permission_id AND p.deleted = 0
     WHERE rp.role_id = 'ROLE_CASHIER' AND rp.deleted = 0
       AND p.permission_code LIKE 'pos:%';
    SELECT count(*) INTO v_manager
      FROM role_permissions rp
      JOIN permissions p ON p.permission_id_str = rp.permission_id AND p.deleted = 0
     WHERE rp.role_id = 'ROLE_STORE_MANAGER' AND rp.deleted = 0
       AND p.permission_code LIKE 'pos:%';
    IF v_cashier <> 10 THEN
        RAISE EXCEPTION '017 assertion failed: ROLE_CASHIER pos codes = %, expected 10', v_cashier;
    END IF;
    IF v_manager <> 7 THEN
        RAISE EXCEPTION '017 assertion failed: ROLE_STORE_MANAGER pos codes = %, expected 7', v_manager;
    END IF;
    RAISE NOTICE '017 OK: ROLE_CASHIER pos=% ROLE_STORE_MANAGER pos=%', v_cashier, v_manager;
END $$;
