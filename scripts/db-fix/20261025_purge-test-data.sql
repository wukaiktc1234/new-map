-- 2026-10-25 P3 前置：物理清空测试数据（非软删）
-- 备份：scripts/db-fix/backup/20261025-pre-purge.sql（pg_dump --data-only，14 张表，1.84 MB）
-- 保留：bank_accounts（账户配置）、roles/permissions/user_roles（权限主数据）、users（测试账号 qa_* 保留）
-- 顺序：子表 → 父表；单事务；可用备份恢复
BEGIN;
SET client_encoding = 'UTF8';

-- 1) 调节表
DELETE FROM reconciliation_items;
DELETE FROM reconciliation_statements;

-- 2) 勾稽
DELETE FROM recon_differences;
DELETE FROM recon_items;
DELETE FROM recon_runs;

-- 3) 导入与外部流水
DELETE FROM import_rows;
DELETE FROM import_batches;
DELETE FROM bank_transactions;

-- 4) 缴款单
DELETE FROM deposit_slips;

-- 5) 资金流水（全部为测试期数据）
DELETE FROM fund_flows;

-- 6) 审计：仅清测试期专用类型
DELETE FROM finance_audit_logs
 WHERE operation_type IN ('FIXTURE_FLOW_REVERSED','RECON_AMBIGUITY_RESOLVED')
    OR operation_type LIKE 'DEPOSIT_SLIP_%';

-- 7) 订单与班次（子表先删）
DELETE FROM order_payment_records;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM pos_shifts;

-- 8) 号段重置（拍板 3，2026-10-25）：清库后必须重置流水号段，否则 A24「号段取值 − 落库条数」会留下恒定残差
--    （清库前已产生的历史残差 456 即由此而来）。仅开发/测试环境适用；生产不清库，A24 残差 = 失败/软删次数，无需清零。
DELETE FROM order_sequence WHERE sequence_key LIKE 'FLOW%';
--    注：缴款单号段 SLIPyyyyMMdd 不随清库重置（缴款单号对外可追溯，重置会与已导出单据重号）

COMMIT;

-- 清空后计数
SELECT 'reconciliation_items' t, count(*) n FROM reconciliation_items
UNION ALL SELECT 'reconciliation_statements', count(*) FROM reconciliation_statements
UNION ALL SELECT 'recon_differences', count(*) FROM recon_differences
UNION ALL SELECT 'recon_items', count(*) FROM recon_items
UNION ALL SELECT 'recon_runs', count(*) FROM recon_runs
UNION ALL SELECT 'import_rows', count(*) FROM import_rows
UNION ALL SELECT 'import_batches', count(*) FROM import_batches
UNION ALL SELECT 'bank_transactions', count(*) FROM bank_transactions
UNION ALL SELECT 'deposit_slips', count(*) FROM deposit_slips
UNION ALL SELECT 'fund_flows', count(*) FROM fund_flows
UNION ALL SELECT 'pos_shifts', count(*) FROM pos_shifts
UNION ALL SELECT 'orders', count(*) FROM orders
UNION ALL SELECT 'order_payment_records', count(*) FROM order_payment_records
UNION ALL SELECT 'order_items', count(*) FROM order_items
UNION ALL SELECT 'finance_audit_logs', count(*) FROM finance_audit_logs
UNION ALL SELECT 'bank_accounts(保留)', count(*) FROM bank_accounts
ORDER BY 1;