-- =====================================================================
-- V20261029_003__grant_member_manage_to_cashier.sql
--
-- ①（Lead 2026-10-29）：`member:manage` 从 POS_BASE_PERMISSIONS 硬编码基础集移除，
--   改为**按角色授予**。理由：manage 是写权限且涉及资金（会员充值/消费扣余额），
--   硬编码 = 任何能登 POS 的人都能动余额，且后台撤不掉（违反最小权限）。
--
-- 迁移内容：给 ROLE_CASHIER 显式授予 member:manage（能力不变，授权来源搬到 DB）。
-- =====================================================================

INSERT INTO role_permissions (role_id, permission_id, deleted, create_time)
SELECT 'ROLE_CASHIER', p.permission_id_str, 0, now()
  FROM permissions p
 WHERE p.permission_code = 'member:manage'
   AND NOT EXISTS (SELECT 1 FROM role_permissions rp
                    WHERE rp.role_id = 'ROLE_CASHIER'
                      AND rp.permission_id = p.permission_id_str
                      AND rp.deleted = 0);

-- 双断言：① ROLE_CASHIER 确有该码；② 该码可被撤销（存在于 role_permissions 即可被 DELETE 掉）
DO $$
DECLARE have int;
BEGIN
    SELECT count(*) INTO have
      FROM role_permissions rp
      JOIN permissions p ON p.permission_id_str = rp.permission_id
     WHERE rp.role_id = 'ROLE_CASHIER' AND rp.deleted = 0
       AND p.permission_code = 'member:manage';
    IF have = 0 THEN
        RAISE EXCEPTION '迁移失败：ROLE_CASHIER 未获得 member:manage（能力会丢）';
    END IF;
END $$;
