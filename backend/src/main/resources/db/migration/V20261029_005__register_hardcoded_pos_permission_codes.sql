-- =====================================================================
-- V20261029_005__register_hardcoded_pos_permission_codes.sql
--
-- ①（Lead 2026-10-29）：POS_BASE_PERMISSIONS（代码硬编码基础集）9 个码逐个核对目录，
--   实测只有 3 个在目录里（product:*:view），**6 个 pos:* 码目录里根本不存在** ——
--   即「代码声明了、目录没有」，任何人无法在后台看到/授予/审计它们（A46 只扫 hasAuthority 字面量，扫不到集合常量）。
--
-- 本迁移：把缺失的 6 个 pos:* 码登记进 permissions 目录（status=1）。
--   登记后它们**可被审计、可被授权、可被撤销**；代码里仍作为基础集无条件给，但至少不再「隐形」。
-- =====================================================================

INSERT INTO permissions (permission_id_str, permission_code, permission_name, status, deleted, create_time)
SELECT v.id_str, v.code, v.name, 1, 0, now()
  FROM (VALUES
        ('PERM_POS_ORDER_CREATE', 'pos:order:create',  'POS 下单'),
        ('PERM_POS_ORDER_PAY',    'pos:order:pay',     'POS 收款'),
        ('PERM_POS_ORDER_QUERY',  'pos:order:query',   'POS 订单查询'),
        ('PERM_POS_MEMBER_QUERY', 'pos:member:query',  'POS 会员查询'),
        ('PERM_POS_MENU_QUERY',   'pos:menu:query',    'POS 菜单查询'),
        ('PERM_POS_YOLO',         'pos:yolo:recognize','POS 菜品识别')
       ) AS v(id_str, code, name)
 WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_code = v.code);

-- 断言：9 个基础集码必须全部在目录里（少一个 → 迁移失败）
DO $$
DECLARE have int;
BEGIN
    SELECT count(*) INTO have
      FROM permissions p
     WHERE p.deleted = 0
       AND p.permission_code IN ('pos:order:create','pos:order:pay','pos:order:query',
                                 'pos:member:query','pos:menu:query','pos:yolo:recognize',
                                 'product:food:view','product:category:view','product:combo:view');
    IF have < 9 THEN
        RAISE EXCEPTION '迁移失败：基础集 9 码只有 % 个在 permissions 目录里', have;
    END IF;
END $$;
