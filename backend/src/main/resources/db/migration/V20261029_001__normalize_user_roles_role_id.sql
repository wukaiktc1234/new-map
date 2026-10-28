-- =====================================================================
-- V20261029_001__normalize_user_roles_role_id.sql
--
-- 起因（R3 三.2）：user_roles.role_id 混存两种形态 —— 数字型（'1'）与 ROLE_* 型（'ROLE_CASHIER'）。
-- 实测分桶：numeric=1 ｜ ROLE_*=23 ｜ 合计 24（唯一数字行：user_id=1(admin) role_id='1'）。
--
-- 影响：授权判定按 role_id 字符串比对，'1' 与 'ROLE_SUPER_ADMIN' 永远不相等 →
--       admin 在 user_roles 上的授权对不上任何权限码；JWT 里角色会变成 ROLE_1。
--
-- 映射规则（只映射能唯一确定的，不猜）：
--   roles 表里数字 id → role_id_str 的对应关系显式写死（当前只有 1=ROLE_SUPER_ADMIN），
--   因为 roles.id 与 role_id_str 不是稳定同源（历史插入顺序不同库可能不同），**不写动态子查询**。
-- 若将来出现其它数字型（非 '1'），本迁移**不会**处理它们，会在下面的校验里被查出来。
-- =====================================================================

UPDATE user_roles SET role_id = 'ROLE_SUPER_ADMIN' WHERE role_id = '1';

-- 校验（迁移后应为 0 行）：数字型残留
DO $$
DECLARE leftover int;
BEGIN
    SELECT count(*) INTO leftover FROM user_roles WHERE role_id ~ '^[0-9]+$';
    IF leftover > 0 THEN
        RAISE EXCEPTION '迁移后仍有 % 行数字型 role_id，需人工确认映射规则', leftover;
    END IF;
END $$;
