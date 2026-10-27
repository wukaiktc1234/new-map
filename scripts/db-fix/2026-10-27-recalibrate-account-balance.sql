-- ============================================================================
-- 2026-10-27 现金户余额校准（**只准备，不执行**）—— 起因：按 tag 清理 fund_flows 后余额未同步
-- 现象：巡检 A7 FAIL accounts=1
--   账户 20「门店1现金账户」balance = 19002，最新未删流水 balance_after = 29002（差 10000 = 被删流水净额）
-- 根因：e2e_cleanup.py 删除 fund_flows 行，但 bank_accounts.balance 仍包含这些行的金额效果
--       → 账户余额与「最新流水快照」不一致 → A7 判红（这正是 A7 存在的意义，报得对）
-- 校准口径（二选一，本文件采用 A，B 供参考）：
--   A) 余额 := 该账户最新未删流水的 balance_after（账实相符；流水是唯一事实源）
--   B) 余额 := 余额 − 被删流水对余额的净影响（需要知道每行 delta 与方向，易错）
-- 安全：仅对「存在未删流水的账户」做；UPDATE 段默认注释，必须 Owner 点头。
-- ============================================================================
\encoding UTF8
\pset pager off

\echo '===== [DRY-RUN] 需要校准的账户 ====='
SELECT b.account_id, b.account_name, b.balance AS current_balance,
       lf.balance_after AS latest_flow_snapshot,
       (b.balance - lf.balance_after) AS delta
  FROM bank_accounts b
  JOIN LATERAL (
        SELECT balance_after FROM fund_flows f
         WHERE f.account_id = b.account_id AND f.deleted = 0
         ORDER BY f.flow_id DESC LIMIT 1
       ) lf ON true
 WHERE b.deleted = 0
   AND lf.balance_after IS NOT NULL
   AND lf.balance_after <> b.balance;

\echo '===== [APPLY] 校准（默认注释；Owner 点头后取消注释并先备份） ====='
-- 备份：pg_dump --data-only --table=bank_accounts -f scripts/db-fix/backup/bank_accounts_20261027.sql
-- BEGIN;
-- UPDATE bank_accounts b
--    SET balance = lf.balance_after
--   FROM LATERAL (
--         SELECT f.account_id, f.balance_after FROM fund_flows f
--          WHERE f.deleted = 0
--          ORDER BY f.flow_id DESC
--       ) lf
--  WHERE lf.account_id = b.account_id
--    AND lf.balance_after IS NOT NULL
--    AND lf.balance_after <> b.balance
--    AND b.deleted = 0;
-- COMMIT;

\echo '===== 执行后复核（A7 应为 0） ====='
SELECT count(*) AS a7_mismatch
  FROM bank_accounts b
  JOIN LATERAL (
        SELECT balance_after FROM fund_flows f
         WHERE f.account_id = b.account_id AND f.deleted = 0
         ORDER BY f.flow_id DESC LIMIT 1
       ) lf ON true
 WHERE b.deleted = 0 AND lf.balance_after IS NOT NULL AND lf.balance_after <> b.balance;