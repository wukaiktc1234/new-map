-- 2026-10-25 P2 开工前置：清理 P1 测试残留
-- 范围（仅测试期内产生的对账数据）：recon_runs/recon_items/recon_differences/reconciliation_statements/reconciliation_items
-- **绝不动** fund_flows / bank_accounts / bank_transactions / deposit_slips；不碰 FF202609080010 / FF202609080028
-- 幂等：重复执行删除 0 行
BEGIN;

-- 0) 清理前快照（写进同一事务的临时表，便于事后核对）
CREATE TEMP TABLE _pre AS
SELECT now() AS at,
       (SELECT count(*) FROM recon_runs) AS runs,
       (SELECT count(*) FROM recon_items) AS items,
       (SELECT count(*) FROM recon_differences) AS diffs,
       (SELECT count(*) FROM reconciliation_statements) AS stmts,
       (SELECT count(*) FROM reconciliation_items) AS stmt_items,
       (SELECT count(*) FROM fund_flows) AS flows,
       (SELECT count(*) FROM bank_transactions) AS bank_txns;

-- 1) 删除顺序：子表 → 父表（FK 级联已存在，但显式删更可控）
DELETE FROM reconciliation_items;
DELETE FROM reconciliation_statements;
DELETE FROM recon_differences WHERE run_id >= 3;
DELETE FROM recon_items WHERE run_id >= 3;
DELETE FROM recon_runs WHERE run_id >= 3;

-- 2) 清理后核对（flows / bank_txns 必须与清理前一致）
SELECT p.runs AS runs_before, (SELECT count(*) FROM recon_runs) AS runs_after,
       p.items AS items_before, (SELECT count(*) FROM recon_items) AS items_after,
       p.diffs AS diffs_before, (SELECT count(*) FROM recon_differences) AS diffs_after,
       p.stmts AS stmts_before, (SELECT count(*) FROM reconciliation_statements) AS stmts_after,
       p.stmt_items AS stmt_items_before, (SELECT count(*) FROM reconciliation_items) AS stmt_items_after,
       p.flows AS flows_before, (SELECT count(*) FROM fund_flows) AS flows_after,
       p.bank_txns AS bank_txns_before, (SELECT count(*) FROM bank_transactions) AS bank_txns_after
  FROM _pre p;

COMMIT;