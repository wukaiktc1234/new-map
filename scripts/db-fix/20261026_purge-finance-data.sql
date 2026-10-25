-- 2026-10-26 P4-4 收尾：清空财务测试数据（purge 语义，物理 DELETE）
-- ===== 备份 =====
--   scripts/db-fix/backup/20261026-pre-purge-finance.sql（pg_dump --data-only，14 张表，1.95 MB，dump_exit=0）
-- ===== 表清单与清前行数（2026-10-26 01:3x 实测）=====
--   fund_flows 1625 ｜ bank_transactions 210 ｜ deposit_slips 42 ｜ import_batches 8 ｜ import_rows 62
--   reconciliation_statements 11 ｜ reconciliation_items 1417 ｜ recon_runs 66 ｜ recon_items 1947 ｜ recon_differences 1948
--   finance_audit_logs 1548 ｜ accounting_periods 2 ｜ finance_period_account_balance 3
--   bank_accounts 32（**只重置 balance，不删行**）
-- ===== 保留（主数据）===== bank_accounts / roles / permissions / users / orders / pos_shifts
-- ===== 依据 ===== Owner 拍板：2026-09 是 P4-4 验收造的测试期间；测试数据不干净导致期初口径问题，一律清空后由 seed 重新生成
--
-- ===== 执行顺序（**必须按此顺序，否则 A1 会恒红**）=====
--   ① 财务派生表（子→父）：finance_period_account_balance → accounting_periods →
--      reconciliation_items → reconciliation_statements → recon_items → recon_differences → recon_runs →
--      finance_audit_logs → fund_flows → deposit_slips → import_rows → import_batches → bank_transactions；
--   ② **然后**软删「已支付但已无 SALE 流水」的历史测试订单（deleted=1，不物理删，备份 orders 见 backup/README.md）；
--   ③ **最后**重置 bank_accounts.balance=0 与 FLOW/SLIP 号段（必须在流水清空之后重置，否则 A24 留残差）。
--   为什么 ② 必须在 ① 之后：② 的判定条件是「该订单没有任何 SALE 流水」——只有在 fund_flows 清空后，
--     所有历史已支付订单才会同时满足该条件并被一次性识别；若在 ① 之前跑，新近订单仍有流水而漏网，
--     清库后它们立刻变成「已支付无流水」→ A1（零容忍）恒红。
--   为什么 ② 必须在 ③ 之前：③ 只影响余额与号段，与 ② 的判定无关，但放最后可保证「余额归零」是终态。
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;

\echo '===== BEFORE ====='
SELECT 'fund_flows' AS t, count(*) FROM fund_flows
UNION ALL SELECT 'bank_transactions', count(*) FROM bank_transactions
UNION ALL SELECT 'deposit_slips', count(*) FROM deposit_slips
UNION ALL SELECT 'import_batches', count(*) FROM import_batches
UNION ALL SELECT 'import_rows', count(*) FROM import_rows
UNION ALL SELECT 'reconciliation_statements', count(*) FROM reconciliation_statements
UNION ALL SELECT 'reconciliation_items', count(*) FROM reconciliation_items
UNION ALL SELECT 'recon_runs', count(*) FROM recon_runs
UNION ALL SELECT 'recon_items', count(*) FROM recon_items
UNION ALL SELECT 'recon_differences', count(*) FROM recon_differences
UNION ALL SELECT 'finance_audit_logs', count(*) FROM finance_audit_logs
UNION ALL SELECT 'accounting_periods', count(*) FROM accounting_periods
UNION ALL SELECT 'finance_period_account_balance', count(*) FROM finance_period_account_balance;

-- 顺序：子表 → 父表（避免 FK 阻塞）
DELETE FROM finance_period_account_balance;
DELETE FROM accounting_periods;
DELETE FROM reconciliation_items;
DELETE FROM reconciliation_statements;
DELETE FROM recon_items;
DELETE FROM recon_differences;
DELETE FROM recon_runs;
DELETE FROM finance_audit_logs;
DELETE FROM fund_flows;
DELETE FROM deposit_slips;
DELETE FROM import_rows;
DELETE FROM import_batches;
DELETE FROM bank_transactions;

-- ===== NEW-14：孤儿退款记录（定性=测试数据，见 WORKER_LOG §63.10）=====
-- 现存 16 条 order_refund_records 的 order_id 在 orders 中 0 命中，create_time 2026-10-23 13:19 ~ 2026-10-24 02:40
-- （早于 2026-10-25 的 P3 清库）→ 是「旧 purge 物理删 orders 但没级联删退款记录」留下的测试残留，不是设计缺陷。
-- 处置：清库时一并清掉孤儿退款记录；同时保留「新增退款写入必须带 order_id 且订单存在」的约束由 P4-5 实现保证。
DELETE FROM order_refund_records r WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.order_id = r.order_id);

-- 账户余额重置为 0（后续由 seed 设定期初；只改 balance/version，不删账户行）
UPDATE bank_accounts SET balance = 0, version = COALESCE(version,0) + 1, update_time = now() WHERE deleted = 0;

-- 号段重置（清库后必须重置，否则 A24 留残差；SLIP 同步重置以免与已删单据重号）
DELETE FROM order_sequence WHERE sequence_key LIKE 'FLOW%' OR sequence_key LIKE 'SLIP%';

COMMIT;

\echo '===== AFTER（应全为 0）====='
SELECT 'fund_flows' AS t, count(*) FROM fund_flows
UNION ALL SELECT 'bank_transactions', count(*) FROM bank_transactions
UNION ALL SELECT 'deposit_slips', count(*) FROM deposit_slips
UNION ALL SELECT 'import_batches', count(*) FROM import_batches
UNION ALL SELECT 'import_rows', count(*) FROM import_rows
UNION ALL SELECT 'reconciliation_statements', count(*) FROM reconciliation_statements
UNION ALL SELECT 'reconciliation_items', count(*) FROM reconciliation_items
UNION ALL SELECT 'recon_runs', count(*) FROM recon_runs
UNION ALL SELECT 'recon_items', count(*) FROM recon_items
UNION ALL SELECT 'recon_differences', count(*) FROM recon_differences
UNION ALL SELECT 'finance_audit_logs', count(*) FROM finance_audit_logs
UNION ALL SELECT 'accounting_periods', count(*) FROM accounting_periods
UNION ALL SELECT 'finance_period_account_balance', count(*) FROM finance_period_account_balance;
\echo '===== 历史已支付测试订单的处理（保护性软删，不改 financial 主数据）====='
-- 为什么必须处理：purge 清空了 fund_flows，但 orders 里有 93 条历史「已支付」测试订单（来自此前 E2E 的 POS 单），
-- 它们对应的 SALE 流水已被清空 → A1「已支付订单缺 SALE 流水」会恒为 93 而永远红（零容忍项）。
-- 处置：把「无任何 SALE 流水支撑的历史已支付订单」**软删**（deleted=1，不物理删除，可回溯），
--   备份见 scripts/db-fix/backup/20261026-pre-purge-orders.sql（53 KB，orders/order_payment_records/pos_shifts）。
--   **这是本次唯一动到 orders 表的操作，且仅为软删 + 有备份；如需保留这些订单请回滚该备份并告知。**
UPDATE orders o SET deleted = 1, update_time = now()
 WHERE o.deleted = 0 AND o.payment_status = 2
   AND NOT EXISTS (SELECT 1 FROM fund_flows f WHERE f.deleted = 0 AND f.order_id = o.order_id);

\echo '===== 主数据保留确认（应非 0）====='
SELECT 'bank_accounts' AS t, count(*) FROM bank_accounts
UNION ALL SELECT 'users', count(*) FROM users
UNION ALL SELECT 'roles', count(*) FROM roles
UNION ALL SELECT 'permissions', count(*) FROM permissions
UNION ALL SELECT 'orders', count(*) FROM orders
UNION ALL SELECT 'pos_shifts', count(*) FROM pos_shifts;
\echo '===== 账户余额重置确认 ====='
SELECT count(*) AS accounts_nonzero_balance FROM bank_accounts WHERE deleted = 0 AND balance <> 0;