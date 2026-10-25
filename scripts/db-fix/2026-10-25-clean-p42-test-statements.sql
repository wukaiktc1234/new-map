SET client_encoding='UTF8';
BEGIN;
-- 清理 P4-2 造数产生的测试调节表（38/39 及其 items），恢复 A19 绿；仅删测试数据，不动真实业务表
DELETE FROM reconciliation_items WHERE statement_id IN (38, 39);
DELETE FROM reconciliation_statements WHERE statement_id IN (38, 39);
COMMIT;
\echo '=== 清理后 A19/A7/A9 口径 ==='
SELECT count(*) AS a19_hit FROM reconciliation_statements s WHERE s.is_balanced = true AND EXISTS (
  SELECT 1 FROM reconciliation_items i1 JOIN reconciliation_items i2 ON i1.statement_id=i2.statement_id AND i1.amount=i2.amount
   WHERE i1.statement_id=s.statement_id AND i1.category IN ('A','B') AND i2.category IN ('C','D'));
SELECT count(*) AS statements_left FROM reconciliation_statements;