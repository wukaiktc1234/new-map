-- =====================================================================
-- scripts/db-fix/2026-10-25-clean-p43-unit-statements.sql
-- 目的：清掉 P4-3 单测级验证建的夹具调节表（statement_id 47/48，statement_no P43-TEST-01）
--       及其 reconciliation_items，避免 ts-ops 的 E2E 撞到。
--
-- 约束：
--   · **只删 47/48**，且必须同时满足 statement_no LIKE 'P43-TEST-%' 且 status='DRAFT'；
--     任一不满足 → RAISE EXCEPTION 中止（防止 id 被复用后误删真实业务表）。
--   · 幂等：重复执行删 0 行，不报错。
--   · finance_audit_logs 里的 RECON_AMBIGUITY_EXEMPTED 审计 **保留不删**（审计不可删）。
-- =====================================================================
\set ON_ERROR_STOP on

-- ---------- 守卫：47/48 必须是 P43-TEST-% 的 DRAFT ----------
DO $$
DECLARE
    bad_count integer;
    bad_list  text;
BEGIN
    SELECT count(*) INTO bad_count
      FROM reconciliation_statements
     WHERE statement_id IN (47, 48)
       AND (statement_no NOT LIKE 'P43-TEST-%' OR status <> 'DRAFT');

    IF bad_count > 0 THEN
        SELECT string_agg(statement_id || '(' || statement_no || ',' || status || ')', ', ')
          INTO bad_list
          FROM reconciliation_statements
         WHERE statement_id IN (47, 48)
           AND (statement_no NOT LIKE 'P43-TEST-%' OR status <> 'DRAFT');
        RAISE EXCEPTION '守卫失败：statement 47/48 中存在非 P43-TEST-* 或非 DRAFT 的行 [%]，已中止，未删除任何数据', bad_list;
    END IF;
END $$;

\echo '===== BEFORE ====='
SELECT 'statements' AS kind, count(*) AS n FROM reconciliation_statements
 WHERE statement_id IN (47, 48)
UNION ALL
SELECT 'items', count(*) FROM reconciliation_items
 WHERE statement_id IN (47, 48);

BEGIN;

-- 先删 items（仅限 47/48 且父表行满足守卫条件）
DELETE FROM reconciliation_items
 WHERE statement_id IN (
       SELECT statement_id FROM reconciliation_statements
        WHERE statement_id IN (47, 48)
          AND statement_no LIKE 'P43-TEST-%'
          AND status = 'DRAFT');

-- 再删 statements 本体
DELETE FROM reconciliation_statements
 WHERE statement_id IN (47, 48)
   AND statement_no LIKE 'P43-TEST-%'
   AND status = 'DRAFT';

COMMIT;

\echo '===== AFTER（验证 1：47/48 的 statements/items 应均为 0）====='
SELECT 'statements' AS kind, count(*) AS n FROM reconciliation_statements
 WHERE statement_id IN (47, 48)
UNION ALL
SELECT 'items', count(*) FROM reconciliation_items
 WHERE statement_id IN (47, 48);

\echo '===== AFTER（验证 2：EXEMPTED 审计必须仍在，审计不可删）====='
SELECT 'RECON_AMBIGUITY_EXEMPTED 审计行数' AS kind, count(*) AS n
  FROM finance_audit_logs
 WHERE operation_type = 'RECON_AMBIGUITY_EXEMPTED';
