-- =====================================================================
-- V20261029_004__grant_pos_money_perms_to_cashier.sql
--
-- ①（Lead 2026-10-29 批准）：三个「中风险」码从 POS_BASE_PERMISSIONS 移除，改为**按角色授予**：
--   pos:member:recharge —— 会员充值（涉资金写入）
--   pos:member:consume  —— 会员消费扣余额（涉资金写入）
--   pos:shift:manage    —— 开班/交班（可开关钱箱，涉现金）
--   硬编码 = 任何能登 POS 的人都有，且后台撤不掉，违反最小权限。
-- =====================================================================

-- 前置：这三个 POS 码此前**只存在于代码硬编码**，权限目录里没有 → 必须先建目录行，否则无法授权/撤销。
INSERT INTO permissions (permission_id_str, permission_code, permission_name, status, deleted, create_time)
SELECT v.id_str, v.code, v.name, 1, 0, now()
  FROM (VALUES
        ('PERM_POS_MEMBER_RECHARGE', 'pos:member:recharge', 'POS 会员充值'),
        ('PERM_POS_MEMBER_CONSUME',  'pos:member:consume',  'POS 会员消费扣余额'),
        ('PERM_POS_SHIFT_MANAGE',    'pos:shift:manage',    'POS 开班交班')
       ) AS v(id_str, code, name)
 WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_code = v.code);

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_CASHIER', p.permission_id_str, 0, now()
  FROM permissions p
 WHERE p.permission_code IN ('pos:member:recharge','pos:member:consume','pos:shift:manage')
   AND NOT EXISTS (SELECT 1 FROM role_permissions rp
                    WHERE rp.role_id = 'ROLE_CASHIER'
                      AND rp.permission_id = p.permission_id_str
                      AND rp.deleted = 0);

-- 双断言：三个码必须都在 ROLE_CASHIER 上（少一个 → 收银台能力缺失，迁移失败）
DO $$
DECLARE have int;
DECLARE total int;
BEGIN
    SELECT count(DISTINCT p.permission_code) INTO have
      FROM role_permissions rp
      JOIN permissions p ON p.permission_id_str = rp.permission_id
     WHERE rp.role_id = 'ROLE_CASHIER' AND rp.deleted = 0
       AND p.permission_code IN ('pos:member:recharge','pos:member:consume','pos:shift:manage');
    IF have < 3 THEN
        RAISE EXCEPTION '迁移失败：ROLE_CASHIER 只拿到 % / 3 个涉资金权限码（撤销能力会丢）', have;
    END IF;

    -- 断言二：这三码不应再出现在权限目录的「基础集」里（基础集在代码中，这里校验 DB 侧确有授权可撤）
    SELECT count(*) INTO total
      FROM role_permissions rp
      JOIN permissions p ON p.permission_id_str = rp.permission_id
     WHERE rp.role_id = 'ROLE_CASHIER' AND rp.deleted = 0
       AND p.permission_code = 'pos:member:recharge';
    IF total = 0 THEN
        RAISE EXCEPTION '迁移失败：pos:member:recharge 未落库';
    END IF;
END $$;
