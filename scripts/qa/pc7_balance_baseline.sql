-- PC-7 基线校准（零容忍版：2026-10-25 已去掉白名单/豁免）
-- 说明：测试数据已物理清空重建，历史补账白名单（FFH% / system/backfill-%）已移除；
--       任何 balance_after 为 0 的普通流水都会由 consistency_check 的 A9 判红，
--       V3 比对不再豁免任何行（空库应为 N/A，无任何账户被标记）。
BEGIN;
SET TRANSACTION READ ONLY;

-- 1) 总览
WITH lf AS (
  SELECT account_id, balance_after,
         ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY flow_id DESC) rn
    FROM fund_flows WHERE deleted = 0
)
SELECT 'overview' AS section, count(*) AS accounts,
       count(lf.balance_after) AS with_flow,
       count(*) FILTER (WHERE lf.balance_after IS NOT NULL AND lf.balance_after <> b.balance) AS v3_mismatched,
       count(*) FILTER (WHERE lf.balance_after IS NULL) AS na_no_flow
  FROM bank_accounts b
  LEFT JOIN lf ON lf.account_id = b.account_id AND lf.rn = 1
 WHERE b.deleted = 0;

-- 2) 不一致清单（非空即需「快照 → 缓存」单向修复）
WITH lf AS (
  SELECT account_id, flow_no, balance_after,
         ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY flow_id DESC) rn
    FROM fund_flows WHERE deleted = 0
)
SELECT b.account_id, b.balance AS cache_fen, lf.balance_after AS snapshot_fen, lf.flow_no,
       lf.balance_after - b.balance AS diff_fen
  FROM bank_accounts b
  JOIN lf ON lf.account_id = b.account_id AND lf.rn = 1
 WHERE b.deleted = 0 AND lf.balance_after <> b.balance
 ORDER BY ABS(lf.balance_after - b.balance) DESC;

-- 3) 普通流水 balance_after = 0 清单（零容忍，与 A9 同口径，不豁免）
SELECT flow_id, flow_no, account_id, amount, balance_after, created_by, LEFT(COALESCE(remark,''),40) AS remark
  FROM fund_flows WHERE deleted = 0 AND balance_after = 0 ORDER BY flow_id;

COMMIT;