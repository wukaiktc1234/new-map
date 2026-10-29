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
 WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_code = v.code)
ON CONFLICT DO NOTHING;

-- 断言：**只断言本迁移负责的 6 个 pos:* 码**（Lead 2026-10-29 裁定）。
--   为什么不再断言 9 码：迁移不能依赖别人的迁移 —— product:*:view 三码由其他迁移负责，
--   在干净库/换顺序时它们可能还没创建，断言 9 会误报失败（已实际炸过一次）。
DO $$
DECLARE have int;
BEGIN
    SELECT count(*) INTO have
      FROM permissions p
     WHERE p.deleted = 0
       AND p.permission_code IN ('pos:order:create','pos:order:pay','pos:order:query',
                                 'pos:member:query','pos:menu:query','pos:yolo:recognize');
    IF have < 6 THEN
        RAISE EXCEPTION '迁移失败：本迁移应登记 6 个 pos:* 码，实际只有 % 个在目录里', have;
    END IF;
END $$;
