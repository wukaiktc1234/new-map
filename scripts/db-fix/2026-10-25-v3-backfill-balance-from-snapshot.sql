SET client_encoding='UTF8';
-- V3 唯一允许方向：用「最新非豁免流水快照」回填 bank_accounts.balance（覆盖缓存），并写校准审计
WITH lf AS (
  SELECT account_id, flow_no, balance_after, ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY flow_id DESC) rn
    FROM fund_flows WHERE deleted = 0
    AND NOT (COALESCE(created_by,'') LIKE 'system/backfill-%' AND (COALESCE(remark,'') LIKE '%[cashBackfill]%' OR COALESCE(remark,'') LIKE '%[posShift=%'))
)
UPDATE bank_accounts b SET balance = lf.balance_after, version = COALESCE(b.version,0)+1, update_time = now()
  FROM lf WHERE lf.account_id = b.account_id AND lf.rn = 1 AND b.deleted = 0 AND lf.balance_after <> b.balance;
\echo '=== 修复后 A7 口径 ==='
WITH lf AS (SELECT account_id, balance_after, ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY flow_id DESC) rn FROM fund_flows WHERE deleted=0)
SELECT count(*) AS mismatch FROM bank_accounts b LEFT JOIN lf ON lf.account_id=b.account_id AND lf.rn=1
 WHERE b.deleted=0 AND lf.balance_after IS NOT NULL AND lf.balance_after <> b.balance;