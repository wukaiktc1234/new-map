-- ============================================================
-- 2026-10-24-cleanup-zero-amount-finance-records.sql
-- DATA-006：清理 10 条 0 元「订单收入」财务流水（软删除，可回滚）
--
-- 【背景】
--   finance_records 里有 10 条 amount=0、record_type=1（收入）、record_category=101（订单收入）
--   的历史流水，remark 形如「订单收入 - T20260915012」；它们没有 voucher_id、
--   审批状态 0（未审批），是订单收入落账链路的早期缺陷产物（金额取不到时写了 0 元）。
--   0 元流水会污染财务报表的收入笔数/均值，且无法通过审批流，属于纯噪音数据。
--
-- 【处理】
--   只软删这 10 条（按 record_no 精确指定），不物理删除：
--     deleted = 1
--     remark 追加 ' [CLEANUP-20261024-zero-amount]'（保留原始 remark 便于追溯）
--   不触碰任何其它 finance_records 行；不改凭证、不改订单。
--
-- 【安全】
--   1) 先建备份表 finance_records_backup_20261024_zero_amount（只备份这 10 行，含原 remark）；
--      表已存在则跳过（可复放）。
--   2) 幂等：UPDATE 带 AND deleted = 0 守卫，重复执行 0 行；remark 标签用 NOT LIKE 二次防抖。
--   3) 执行前后打印分布；结束时断言「amount=0 且 deleted=0 的订单收入」= 0。
--   回滚：见文件末尾注释（用备份表还原 deleted 与 remark）。
--
-- 用法：psql -h localhost -U postgres -d food_traceability -f 本文件
-- ============================================================

\set ON_ERROR_STOP on

BEGIN;

-- ---------- 0) 备份这 10 行 ----------
DO $$
DECLARE
    v_rows bigint;
BEGIN
    IF to_regclass('public.finance_records_backup_20261024_zero_amount') IS NULL THEN
        CREATE TABLE public.finance_records_backup_20261024_zero_amount AS
        SELECT * FROM public.finance_records
         WHERE record_no IN ('SZ202609170001','SZ202609210001','SZ202609250002','SZ202609260002',
                             'SZ202609300002','SZ202609300005','SZ202609300008','SZ202609300011',
                             'SZ202610230005','SZ202610230007');
        SELECT COUNT(*) INTO v_rows FROM public.finance_records_backup_20261024_zero_amount;
        RAISE NOTICE '[ok] 已备份 % 行 → finance_records_backup_20261024_zero_amount', v_rows;
    ELSE
        SELECT COUNT(*) INTO v_rows FROM public.finance_records_backup_20261024_zero_amount;
        RAISE NOTICE '[skip] 备份表已存在（% 行），不覆盖', v_rows;
    END IF;
END $$;

-- ---------- 1) BEFORE 快照 ----------
\echo '== BEFORE：目标 10 条现状 =='
SELECT record_id, record_no, amount, deleted, voucher_id, remark
  FROM public.finance_records
 WHERE record_no IN ('SZ202609170001','SZ202609210001','SZ202609250002','SZ202609260002',
                     'SZ202609300002','SZ202609300005','SZ202609300008','SZ202609300011',
                     'SZ202610230005','SZ202610230007')
 ORDER BY record_no;

\echo '== BEFORE：全表 0 元订单收入（deleted=0）=='
SELECT count(*) AS zero_amount_order_income_rows
  FROM public.finance_records
 WHERE amount = 0 AND deleted = 0 AND record_type = 1 AND record_category = 101;

-- ---------- 2) 软删 ----------
UPDATE public.finance_records
   SET deleted     = 1,
       remark      = COALESCE(remark, '') || ' [CLEANUP-20261024-zero-amount]',
       update_time = CURRENT_TIMESTAMP
 WHERE record_no IN ('SZ202609170001','SZ202609210001','SZ202609250002','SZ202609260002',
                     'SZ202609300002','SZ202609300005','SZ202609300008','SZ202609300011',
                     'SZ202610230005','SZ202610230007')
   AND deleted = 0
   AND COALESCE(remark, '') NOT LIKE '%[CLEANUP-20261024-zero-amount]%';

-- ---------- 3) AFTER 快照 + 断言 ----------
\echo '== AFTER：目标 10 条 =='
SELECT record_id, record_no, amount, deleted, left(remark, 60) AS remark_head
  FROM public.finance_records
 WHERE record_no IN ('SZ202609170001','SZ202609210001','SZ202609250002','SZ202609260002',
                     'SZ202609300002','SZ202609300005','SZ202609300008','SZ202609300011',
                     'SZ202610230005','SZ202610230007')
 ORDER BY record_no;

SELECT CASE WHEN count(*) = 0
            THEN 'PASS: 0 元订单收入已清零（deleted=0 口径）'
            ELSE 'FAIL: 仍有 ' || count(*) || ' 条 0 元订单收入未清理' END AS assertion,
       (SELECT count(*) FROM public.finance_records
         WHERE record_no IN ('SZ202609170001','SZ202609210001','SZ202609250002','SZ202609260002',
                             'SZ202609300002','SZ202609300005','SZ202609300008','SZ202609300011',
                             'SZ202610230005','SZ202610230007')
           AND deleted = 1) AS soft_deleted_target_rows
  FROM public.finance_records
 WHERE amount = 0 AND deleted = 0 AND record_type = 1 AND record_category = 101;

COMMIT;

-- 回滚（如需）：
--   UPDATE finance_records f
--      SET deleted = b.deleted,
--          remark  = b.remark,
--          update_time = CURRENT_TIMESTAMP
--     FROM finance_records_backup_20261024_zero_amount b
--    WHERE f.record_id = b.record_id;
