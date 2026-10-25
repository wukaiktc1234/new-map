-- =====================================================================
-- scripts/db-fix/20261025_statement13_reset.sql
-- Owner 指定：statement 13 重置为 DRAFT + is_balanced=false，并把两组同额歧义
-- item 的 reason 加 AMBIGUOUS: 前缀（幂等：已带前缀的不重复加）。
--
-- 约束（不得越界）：
--   · 不调类别（A/B/C/D 分布保持不动）
--   · 不重新 submit（只把 status 落回 DRAFT）
--   · 不删除审计（finance_audit_logs 中 220-249 的 RECON_AMBIGUITY_RESOLVED 保留，
--     由 lead 在 WORKER_LOG 标注作废）
--   · 只动 statement_id = 13，且只动 amount IN (10000, 45678) 的条目
-- =====================================================================
\set ON_ERROR_STOP on

\echo '===== BEFORE ====='
SELECT s.statement_id, s.status, s.is_balanced,
       (SELECT count(*) FROM reconciliation_items i WHERE i.statement_id = s.statement_id
          AND i.deleted = 0 AND i.category = 'A') AS a_cnt,
       (SELECT count(*) FROM reconciliation_items i WHERE i.statement_id = s.statement_id
          AND i.deleted = 0 AND i.category = 'B') AS b_cnt,
       (SELECT count(*) FROM reconciliation_items i WHERE i.statement_id = s.statement_id
          AND i.deleted = 0 AND i.category = 'C') AS c_cnt,
       (SELECT count(*) FROM reconciliation_items i WHERE i.statement_id = s.statement_id
          AND i.deleted = 0 AND i.category = 'D') AS d_cnt,
       (SELECT count(*) FROM reconciliation_items i WHERE i.statement_id = s.statement_id
          AND i.deleted = 0 AND i.reason LIKE 'AMBIGUOUS:%') AS ambiguous_cnt
  FROM reconciliation_statements s
 WHERE s.statement_id = 13;

BEGIN;

-- ① 状态回退：SUBMITTED → DRAFT，is_balanced 强制 false（不自动重新 submit）
UPDATE reconciliation_statements
   SET status = 'DRAFT',
       is_balanced = false,
       update_time = NOW(),
       version = version + 1
 WHERE statement_id = 13
   -- 幂等：已是 DRAFT 且 is_balanced=false 时不重复写（避免无意义 version 递增）
   AND (status <> 'DRAFT' OR is_balanced = true);

-- ② 同额歧义打标（幂等）：amount=10000（A10+C1）与 amount=45678（A2+C2）
--    两侧都要打，前缀只在缺失时添加；已带前缀的行不动。
UPDATE reconciliation_items
   SET reason = 'AMBIGUOUS: ' || COALESCE(reason, ''),
       update_time = NOW()
 WHERE statement_id = 13
   AND deleted = 0
   AND amount IN (10000, 45678)
   AND category IN ('A', 'B', 'C', 'D')
   AND (reason IS NULL OR reason NOT LIKE 'AMBIGUOUS:%');

COMMIT;

\echo '===== AFTER ====='
SELECT s.statement_id, s.status, s.is_balanced,
       (SELECT count(*) FROM reconciliation_items i WHERE i.statement_id = s.statement_id
          AND i.deleted = 0 AND i.category = 'A') AS a_cnt,
       (SELECT count(*) FROM reconciliation_items i WHERE i.statement_id = s.statement_id
          AND i.deleted = 0 AND i.category = 'B') AS b_cnt,
       (SELECT count(*) FROM reconciliation_items i WHERE i.statement_id = s.statement_id
          AND i.deleted = 0 AND i.category = 'C') AS c_cnt,
       (SELECT count(*) FROM reconciliation_items i WHERE i.statement_id = s.statement_id
          AND i.deleted = 0 AND i.category = 'D') AS d_cnt,
       (SELECT count(*) FROM reconciliation_items i WHERE i.statement_id = s.statement_id
          AND i.deleted = 0 AND i.reason LIKE 'AMBIGUOUS:%') AS ambiguous_cnt
  FROM reconciliation_statements s
 WHERE s.statement_id = 13;

\echo '===== AFTER：同额两组明细（应 10000:A10+C1、45678:A2+C2，全部带前缀）====='
SELECT amount, category, count(*) AS cnt, count(*) FILTER (WHERE reason LIKE 'AMBIGUOUS:%') AS prefixed
  FROM reconciliation_items
 WHERE statement_id = 13 AND deleted = 0 AND amount IN (10000, 45678)
 GROUP BY amount, category
 ORDER BY amount DESC, category;

\echo '===== 审计保留核对（220-249 不得被删）====='
SELECT count(*) AS audit_rows_220_249
  FROM finance_audit_logs
 WHERE operation_type = 'RECON_AMBIGUITY_RESOLVED'
   AND log_id BETWEEN 220 AND 249;
