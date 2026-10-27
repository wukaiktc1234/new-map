-- ============================================================================
-- 2026-10-27 账户余额校准（一次性修复/复核工具，口径 A）
-- 起因：按 tag 清理 fund_flows 后未同步 bank_accounts.balance → 巡检 A7 判红
--   （实测：账户 20 余额 19002 / 链尾 balance_after 29002 / delta -10000 → A7 accounts=1；已修平）
-- 口径 A（Owner 裁定）：bank_accounts.balance 是【派生值】，fund_flows.balance_after 链才是事实源
--   ⇒ balance := 该账户最新未删流水的 balance_after；无流水则置 0（需人工确认是否合理）
-- 注：e2e_cleanup.py 已内建同一逻辑（--apply 时同事务执行）；本文件用于一次性修复与事后复核。
-- ⚠ 反例（踩过的坑）：不要写 UPDATE ... FROM LATERAL (SELECT ... ORDER BY flow_id DESC)，
--    每个账户会匹配多行 → UPDATE 取值不确定（实测 UPDATE 1 行但 A7 仍为 1）。必须相关子查询 + LIMIT 1。
-- ============================================================================
\encoding UTF8
\pset pager off

\echo '===== [DRY-RUN] 需要校准的账户 ====='
SELECT b.account_id, b.account_name, b.balance AS current_balance,
       (SELECT f.balance_after FROM fund_flows f
         WHERE f.account_id = b.account_id AND f.deleted = 0
         ORDER BY f.flow_id DESC LIMIT 1) AS latest_flow_snapshot,
       b.balance - (SELECT f.balance_after FROM fund_flows f
                     WHERE f.account_id = b.account_id AND f.deleted = 0
                     ORDER BY f.flow_id DESC LIMIT 1) AS delta
  FROM bank_accounts b
 WHERE b.deleted = 0
   AND (SELECT f.balance_after FROM fund_flows f
         WHERE f.account_id = b.account_id AND f.deleted = 0
         ORDER BY f.flow_id DESC LIMIT 1) IS NOT NULL
   AND (SELECT f.balance_after FROM fund_flows f
         WHERE f.account_id = b.account_id AND f.deleted = 0
         ORDER BY f.flow_id DESC LIMIT 1) <> b.balance;

\echo '===== [APPLY] 校准（默认注释；Owner 点头后取消注释） ====='
-- 备份：pg_dump --data-only --table=bank_accounts -f scripts/db-fix/backup/bank_accounts_20261027.sql
-- BEGIN;
-- UPDATE bank_accounts b
--    SET balance = (SELECT f.balance_after FROM fund_flows f
--                    WHERE f.account_id = b.account_id AND f.deleted = 0
--                    ORDER BY f.flow_id DESC LIMIT 1)
--  WHERE b.deleted = 0
--    AND (SELECT f.balance_after FROM fund_flows f
--          WHERE f.account_id = b.account_id AND f.deleted = 0
--          ORDER BY f.flow_id DESC LIMIT 1) IS NOT NULL
--    AND (SELECT f.balance_after FROM fund_flows f
--          WHERE f.account_id = b.account_id AND f.deleted = 0
--          ORDER BY f.flow_id DESC LIMIT 1) <> b.balance;
-- COMMIT;

\echo '===== 执行后复核（A7 必须为 0） ====='
SELECT count(*) AS a7_mismatch
  FROM bank_accounts b
 WHERE b.deleted = 0
   AND (SELECT f.balance_after FROM fund_flows f
         WHERE f.account_id = b.account_id AND f.deleted = 0
         ORDER BY f.flow_id DESC LIMIT 1) IS NOT NULL
   AND (SELECT f.balance_after FROM fund_flows f
         WHERE f.account_id = b.account_id AND f.deleted = 0
         ORDER BY f.flow_id DESC LIMIT 1) <> b.balance;