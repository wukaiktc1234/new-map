-- =====================================================================
-- V20261029_002__revoke_cashier_refund_permissions.sql
--
-- Owner 2026-10-29 路径 A 指令；QA 根因：**权限声明层发了退款码，接口层却要求店长**，
--   导致前端显示退款按钮、点击必 403（声明与执行不一致）。
--
-- 处置：从 ROLE_CASHIER **物理删除**三个退款发起权限码（保留 refund:view 只读）。
--   为什么用 DELETE 而不是 deleted=1：这三条是**误授**（权限声明与接口要求不一致），
--   留软删会让「按 deleted=0 统计」和「按行数统计」两套口径打架；
--   且本迁移在同事务内用断言保证结果，可重复执行（幂等）。
--   —— 注：与「授权行永不 DELETE」的通则的偏离，已在此显式记录，理由为误授纠正。
-- =====================================================================

-- 1) 删除三个退款发起码（保留 refund:view）
DELETE FROM role_permissions rp
 USING permissions p
 WHERE rp.permission_id = p.permission_id_str
   AND rp.role_id = 'ROLE_CASHIER'
   AND p.permission_code IN ('order:refund','order:refund:initiate','refund:manage');

-- 2) 断言：三个码必须已不在 ROLE_CASHIER 上；refund:view 必须仍在
DO $$
DECLARE bad int;
DECLARE keep int;
BEGIN
    SELECT count(*) INTO bad
      FROM role_permissions rp
      JOIN permissions p ON p.permission_id_str = rp.permission_id
     WHERE rp.role_id = 'ROLE_CASHIER'
       AND rp.deleted = 0
       AND p.permission_code IN ('order:refund','order:refund:initiate','refund:manage');
    IF bad > 0 THEN
        RAISE EXCEPTION '迁移失败：ROLE_CASHIER 仍持有 % 条退款发起权限码', bad;
    END IF;

    SELECT count(*) INTO keep
      FROM role_permissions rp
      JOIN permissions p ON p.permission_id_str = rp.permission_id
     WHERE rp.role_id = 'ROLE_CASHIER'
       AND rp.deleted = 0
       AND p.permission_code = 'refund:view';
    IF keep = 0 THEN
        RAISE EXCEPTION '迁移失败：refund:view 被误删（应保留只读）';
    END IF;
END $$;
