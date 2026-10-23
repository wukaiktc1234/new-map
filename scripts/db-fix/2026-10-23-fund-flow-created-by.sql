-- ============================================================
-- 2026-10-23-fund-flow-created-by.sql
-- 不变式 [d] 收口：fund_flows.created_by 空值补齐（Owner/lead 2026-10-23 要求）
--
-- 背景：
--   check_finance_invariants.py 的 [d] 判定只看 create_user_id（已为 0 空值），
--   但同一张表里 created_by 仍有 149 行（含软删共 152 行）为 NULL——
--   早期流水由系统补账/迁移/直连 SQL 写入，没有写操作人标识。
--   审计追踪上这属于「不知道谁记的账」，统一补成 'system'（系统写入），
--   而不是留 NULL 让下游把他当成未知用户。
--
-- 规则：
--   1) 只补 NULL，已有真实值（用户名/补账标记如 'system-backfill-20261023'）一律不动；
--   2) 不区分 deleted：软删除行也补，避免历史导出/审计报表出现空操作人；
--   3) 幂等：第二次执行 UPDATE 0 行。
--
-- 用法：psql -h localhost -U postgres -d food_traceability -f 本文件
-- 回滚：无法精确还原（原值即 NULL）——
--       UPDATE fund_flows SET created_by = NULL WHERE created_by = 'system';
--       （注意会连带清掉历史上本来就是 'system' 的行，建议依赖 fund_flows_backup_20261023_hist 快照核对后再执行）
-- ============================================================

\set ON_ERROR_STOP on

BEGIN;

-- BEFORE 快照
SELECT 'BEFORE' AS phase,
       COUNT(*)                                  AS total_rows,
       COUNT(*) FILTER (WHERE created_by IS NULL) AS created_by_null,
       COUNT(*) FILTER (WHERE created_by IS NOT NULL) AS created_by_set
  FROM public.fund_flows;

UPDATE public.fund_flows
   SET created_by  = 'system',
       update_time = CURRENT_TIMESTAMP
 WHERE created_by IS NULL;

-- AFTER 快照 + 断言
SELECT 'AFTER' AS phase,
       COUNT(*)                                  AS total_rows,
       COUNT(*) FILTER (WHERE created_by IS NULL) AS created_by_null,
       COUNT(*) FILTER (WHERE created_by = 'system') AS created_by_system,
       CASE WHEN COUNT(*) FILTER (WHERE created_by IS NULL) = 0
            THEN 'PASS: created_by 无 NULL 残留'
            ELSE 'FAIL: 仍有 NULL 残留' END       AS assertion
  FROM public.fund_flows;

COMMIT;
