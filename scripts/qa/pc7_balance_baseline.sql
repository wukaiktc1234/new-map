-- PC-7 基线校准：bank_accounts.balance（缓存） vs 最新非补账 fund_flows.balance_after（快照真值）
-- 只读（SET TRANSACTION READ ONLY）。V3 例外规则：历史补账行（FFH% / remark 含 [历史补账）balance_after 按设计为 0，必须排除。
BEGIN;
SET TRANSACTION READ ONLY;

-- 1) 总览
WITH last_flow AS (
  SELECT account_id, balance_after,
         ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY flow_id DESC) rn
  FROM fund_flows
  WHERE deleted = 0
    AND flow_no NOT LIKE 'FFH%'
    AND COALESCE(remark,'') NOT LIKE '%[历史补账%'
)
SELECT 'overview' AS section,
       count(*) AS accounts,
       count(lf.balance_after) AS accounts_with_real_flow,
       count(*) FILTER (WHERE lf.balance_after IS NOT NULL AND lf.balance_after <> b.balance) AS mismatched,
       count(*) FILTER (WHERE lf.balance_after IS NULL) AS na_all_backfill
FROM bank_accounts b
LEFT JOIN last_flow lf ON lf.account_id = b.account_id AND lf.rn = 1
WHERE b.deleted = 0;

-- 2) 漂移清单（非 0 即需按 V3 单向修复：快照 → 缓存）
WITH last_flow AS (
  SELECT account_id, balance_after,
         ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY flow_id DESC) rn
  FROM fund_flows
  WHERE deleted = 0
    AND flow_no NOT LIKE 'FFH%'
    AND COALESCE(remark,'') NOT LIKE '%[历史补账%'
)
SELECT b.account_id, b.account_name, b.scope, b.store_id, b.account_type,
       b.balance AS cache_fen, lf.balance_after AS snapshot_fen,
       lf.balance_after - b.balance AS diff_fen,
       CASE WHEN lf.balance_after IS NULL THEN 'N/A(全补账)' ELSE 'DRIFT' END AS verdict
FROM bank_accounts b
LEFT JOIN last_flow lf ON lf.account_id = b.account_id AND lf.rn = 1
WHERE b.deleted = 0
  AND (lf.balance_after IS NULL OR lf.balance_after <> b.balance)
ORDER BY ABS(COALESCE(lf.balance_after, 0) - b.balance) DESC, b.account_id;

-- 3) 历史补账行统计（应全部 balance_after = 0）
SELECT 'backfill_rows' AS section, count(*) AS rows,
       count(*) FILTER (WHERE balance_after <> 0) AS non_zero_backfill
FROM fund_flows
WHERE deleted = 0 AND (flow_no LIKE 'FFH%' OR COALESCE(remark,'') LIKE '%[历史补账%');

COMMIT;
