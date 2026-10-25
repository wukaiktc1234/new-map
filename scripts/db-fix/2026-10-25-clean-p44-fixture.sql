-- =====================================================================
-- scripts/db-fix/2026-10-25-clean-p44-fixture.sql
-- 目的：清掉 P4-4 验证夹具（remark LIKE 'P4-4 %' 的 DRAFT 调节表，含 statement 73）及其 items，
--       释放 account 25 + 今天的 cell，避免与 E2E 撞车。
--
-- 约束：
--   · 守卫：remark LIKE 'P4-4 %' 的行必须全部是 status='DRAFT'；任一行不是 → RAISE EXCEPTION 中止（不删任何数据）
--   · 单事务；先删 items 再删 statement；幂等（重复执行删 0 行）
--   · finance_audit_logs **不删**（RECON_AMBIGUITY_EXEMPTED / RECON_DISPOSITION_LOST 等审计必须保留）
-- =====================================================================
\set ON_ERROR_STOP on

DO $$
DECLARE bad_count integer; bad_list text;
BEGIN
    SELECT count(*) INTO bad_count
      FROM reconciliation_statements
     WHERE COALESCE(remark, '') LIKE 'P4-4 %' AND status <> 'DRAFT';
    IF bad_count > 0 THEN
        SELECT string_agg(statement_id || '(' || statement_no || ',' || status || ')', ', ') INTO bad_list
          FROM reconciliation_statements
         WHERE COALESCE(remark, '') LIKE 'P4-4 %' AND status <> 'DRAFT';
        RAISE EXCEPTION '守卫失败：remark LIKE P4-4-* 的行中存在非 DRAFT [%]，已中止，未删除任何数据', bad_list;
    END IF;
END $$;

\echo '===== BEFORE（P4-4 夹具：statements / items）====='
SELECT 'statements' AS kind, count(*) AS n FROM reconciliation_statements WHERE COALESCE(remark, '') LIKE 'P4-4 %'
UNION ALL
SELECT 'items', count(*) FROM reconciliation_items
 WHERE statement_id IN (SELECT statement_id FROM reconciliation_statements WHERE COALESCE(remark, '') LIKE 'P4-4 %');

BEGIN;

DELETE FROM reconciliation_items
 WHERE statement_id IN (
       SELECT statement_id FROM reconciliation_statements
        WHERE COALESCE(remark, '') LIKE 'P4-4 %' AND status = 'DRAFT');

DELETE FROM reconciliation_statements
 WHERE COALESCE(remark, '') LIKE 'P4-4 %' AND status = 'DRAFT';

COMMIT;

\echo '===== AFTER（验证 1：P4-4 夹具应均为 0；并确认 account 25 + 今天已释放）====='
SELECT 'statements' AS kind, count(*) AS n FROM reconciliation_statements WHERE COALESCE(remark, '') LIKE 'P4-4 %'
UNION ALL
SELECT 'items', count(*) FROM reconciliation_items
 WHERE statement_id IN (SELECT statement_id FROM reconciliation_statements WHERE COALESCE(remark, '') LIKE 'P4-4 %')
UNION ALL
SELECT 'acct25_today_statements', count(*) FROM reconciliation_statements
 WHERE account_id = 25 AND period_start = CURRENT_DATE AND period_end = CURRENT_DATE;

\echo '===== AFTER（验证 2：审计必须仍在 —— 不删审计）====='
SELECT operation_type, count(*) AS n FROM finance_audit_logs
 WHERE operation_type IN ('RECON_DISPOSITION_LOST','RECON_AMBIGUITY_EXEMPTED','RECON_AMBIGUITY_RESOLVED')
 GROUP BY operation_type ORDER BY operation_type;
