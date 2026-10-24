-- PC-7 基线校准：bank_accounts.balance（缓存） vs 最新「非补账豁免」fund_flows.balance_after（快照真值）
-- 只读（SET TRANSACTION READ ONLY）。
--
-- 豁免规则（白名单式，唯一口径 —— Owner 2026-10-24 裁决，禁止宽泛豁免）：
--   仅当该行同时属于下列之一时豁免：
--     ① flow_no LIKE 'FFH%'                 （历史补账流水，名称前缀即标记）
--     ② created_by LIKE 'system/backfill-%'  （补账脚本写入的审计名）
--   反例（**不得豁免**）：普通流水 balance_after=0 —— 它仍参与 V3 比对；
--     若它恰好是某账户最新一笔流水，V3 必须报红（真实不一致），不准放宽、不准改数据。
--   注：历史补账行按设计写 balance_after=0（补账不回写余额），豁免它们是为了避免
--     把「设计如此」误判为「账户余额漂移」；这与「普通行写 0」是两个完全不同的性质。
BEGIN;
SET TRANSACTION READ ONLY;

-- 1) 总览：账户数 / 有非豁免流水 / V3 不一致 / 全补账(N/A)
WITH lf AS (
  SELECT account_id, balance_after,
         ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY flow_id DESC) rn
    FROM fund_flows
   WHERE deleted = 0
     AND NOT (flow_no LIKE 'FFH%' OR COALESCE(created_by,'') LIKE 'system/backfill-%')
)
SELECT 'overview' AS section,
       count(*) AS accounts,
       count(lf.balance_after) AS with_nonexempt_flow,
       count(*) FILTER (WHERE lf.balance_after IS NOT NULL AND lf.balance_after <> b.balance) AS v3_mismatched,
       count(*) FILTER (WHERE lf.balance_after IS NULL) AS na_all_backfill
  FROM bank_accounts b
  LEFT JOIN lf ON lf.account_id = b.account_id AND lf.rn = 1
 WHERE b.deleted = 0;

-- 2) V3 不一致清单（非空即需按「快照 → 缓存」单向修复；N/A 仅说明该账户全为补账行）
WITH lf AS (
  SELECT account_id, flow_no, balance_after,
         ROW_NUMBER() OVER (PARTITION BY account_id ORDER BY flow_id DESC) rn
    FROM fund_flows
   WHERE deleted = 0
     AND NOT (flow_no LIKE 'FFH%' OR COALESCE(created_by,'') LIKE 'system/backfill-%')
)
SELECT b.account_id, b.account_name, b.scope, b.store_id, b.account_type,
       b.balance AS cache_fen, lf.balance_after AS snapshot_fen, lf.flow_no,
       lf.balance_after - b.balance AS diff_fen,
       CASE WHEN lf.balance_after IS NULL THEN 'N/A(全补账)' ELSE 'DRIFT' END AS verdict
  FROM bank_accounts b
  LEFT JOIN lf ON lf.account_id = b.account_id AND lf.rn = 1
 WHERE b.deleted = 0
   AND (lf.balance_after IS NULL OR lf.balance_after <> b.balance)
 ORDER BY ABS(COALESCE(lf.balance_after, 0) - b.balance) DESC, b.account_id;

-- 3) 豁免行统计（应全部 balance_after = 0；非 0 说明补账写入异常）
SELECT 'exempt_rows' AS section, count(*) AS rows,
       count(*) FILTER (WHERE balance_after <> 0) AS exempt_non_zero
  FROM fund_flows
 WHERE deleted = 0
   AND (flow_no LIKE 'FFH%' OR COALESCE(created_by,'') LIKE 'system/backfill-%');

-- 4) 普通行 balance_after = 0 清单（不豁免！只报告，不改数据；若成为最新流水会让 V3 报红）
--    已知历史白名单（精确 flow_no，禁止模糊放宽）：FF202609080010 / FF202609080028
--    白名单只用于「标注已知项」，不改变 A8 的判定口径；未知异常仍会在 A8 报红。
SELECT flow_id, flow_no, account_id, amount, balance_after, created_by,
       LEFT(COALESCE(remark,''),40) AS remark,
       CASE WHEN flow_no IN ('FF202609080010','FF202609080028')
            THEN 'KNOWN_HISTORICAL(待P2)' ELSE 'UNKNOWN' END AS triage
  FROM fund_flows
 WHERE deleted = 0
   AND balance_after = 0
   AND NOT (flow_no LIKE 'FFH%' OR COALESCE(created_by,'') LIKE 'system/backfill-%')
 ORDER BY triage, flow_id;

COMMIT;