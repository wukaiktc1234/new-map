-- =====================================================================
-- 2026-10-27-purge-untagged-statements.sql   （**仅起草，未执行**；等 Owner 裁定）
-- 目的：清理本轮 SF 场景造的调节表 statement 130~135（remark 裸写 "e2e-stmt generate"，无 [E2E:*] tag），
--       它们是 ts-ops/SF 场景的试验数据，不是真实业务。
-- 判定三条件（全部命中才删）：
--   ① remark NOT LIKE '%[E2E:*%'（无 tag）
--   ② 没有关联的 recon_run 处于 FINALIZED（否则对账结论会被一并抹掉）
--   ③ 期间落在实验窗口：**create_time** 在 2026-10-27 00:00 ~ 2026-10-28 00:00（这批 period_start 是 10-04~10-09 的长窗口，不能拿 period_start 卡）
-- 现状（DRY 会打印）：
--   statement 120~135 共 16 条，remark 全部是 'e2e-stmt generate'（无 tag）；本轮点名 130~135。
--   注意：120~129 属更早一批，**本脚本默认不删**（只处理 130~135，其余等指令）。
-- =====================================================================
\set ON_ERROR_STOP on

-- ========== DRY（只读，可反复执行）==========
\echo '=== DRY：满足三条件的 statement（默认范围 130~135）==='
SELECT s.statement_id, s.statement_no, s.store_id, s.account_id, s.status,
       s.period_start, s.period_end, s.create_time, s.remark
  FROM reconciliation_statements s
 WHERE s.statement_id BETWEEN 130 AND 135
   AND s.deleted = 0
   AND COALESCE(s.remark, '') NOT LIKE '%[E2E:%'                            -- ① 无 E2E tag
   AND NOT EXISTS (                                                          -- ② 无 FINALIZED run 关联
         SELECT 1 FROM recon_runs r
          WHERE r.account_id = s.account_id
            AND r.status = 'FINALIZED'
            AND r.business_date BETWEEN s.period_start AND COALESCE(s.period_end, s.period_start)
       )
   AND s.create_time >= TIMESTAMP '2026-10-27 00:00:00'                      -- ③ 实验窗口
   AND s.create_time <  TIMESTAMP '2026-10-28 00:00:00'
 ORDER BY s.statement_id;

\echo '=== DRY：这些 statement 的明细条数（删前必看）==='
SELECT s.statement_id, s.statement_no,
       (SELECT count(*) FROM reconciliation_items i WHERE i.statement_id = s.statement_id) AS items
  FROM reconciliation_statements s
 WHERE s.statement_id BETWEEN 130 AND 135
 ORDER BY s.statement_id;

-- ========== 备份（执行前必须跑）==========
-- \copy (SELECT * FROM reconciliation_statements WHERE statement_id BETWEEN 130 AND 135) TO 'P:/my-new-project/.verify-tmp/backup-statements-130-135.csv' WITH (FORMAT csv, HEADER true)
-- \copy (SELECT * FROM reconciliation_items WHERE statement_id BETWEEN 130 AND 135) TO 'P:/my-new-project/.verify-tmp/backup-statement-items-130-135.csv' WITH (FORMAT csv, HEADER true)

-- ========== DELETE（**默认注释，Owner 批准后取消注释**）==========
-- BEGIN;
-- DELETE FROM reconciliation_items i
--  WHERE i.statement_id IN (
--        SELECT s.statement_id FROM reconciliation_statements s
--         WHERE s.statement_id BETWEEN 130 AND 135
--           AND s.deleted = 0
--           AND COALESCE(s.remark,'') NOT LIKE '%[E2E:%'
--           AND NOT EXISTS (SELECT 1 FROM recon_runs r
--                            WHERE r.account_id = s.account_id AND r.status='FINALIZED'
--                              AND r.business_date BETWEEN s.period_start AND COALESCE(s.period_end, s.period_start))
--           AND s.create_time >= TIMESTAMP '2026-10-27 00:00:00'
--           AND s.create_time <  TIMESTAMP '2026-10-28 00:00:00'
-- DELETE FROM reconciliation_statements s
--  WHERE s.statement_id BETWEEN 130 AND 135
--    AND s.deleted = 0
--    AND COALESCE(s.remark,'') NOT LIKE '%[E2E:%'
--    AND NOT EXISTS (SELECT 1 FROM recon_runs r
--                     WHERE r.account_id = s.account_id AND r.status='FINALIZED'
--                       AND r.business_date BETWEEN s.period_start AND COALESCE(s.period_end, s.period_start))
--    AND s.create_time >= TIMESTAMP '2026-10-27 00:00:00'
--    AND s.create_time <  TIMESTAMP '2026-10-28 00:00:00'
--    AND s.period_start >= DATE '2026-10-27';
-- INSERT INTO finance_audit_logs (operator_id, operator_name, operation_type, module, target_id, target_type,
--                                 operation_desc, operation_data, create_time, created_by, updated_by, version)
-- VALUES (0,'fund-500','DATA_PURGE','RECON_STATEMENT',0,'RECONCILIATION_STATEMENT',
--  '清理无 tag 的 SF 实验调节表 130~135（remark=e2e-stmt generate）',
--  '{"purgedStatements":"130-135","backup":".verify-tmp/backup-statements-130-135.csv"}',
--  now(),'fund-500','fund-500',0);
-- COMMIT;
-- ========== 回撤：按 backup CSV 重新 INSERT（statement 主键自增，需显式写 statement_id）==========
