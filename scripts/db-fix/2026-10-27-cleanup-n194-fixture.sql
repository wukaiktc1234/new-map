-- ============================================================
-- scripts/db-fix/2026-10-27-cleanup-n194-fixture.sql
--
-- 目的：清理 N19-4「POS 开班备用金」验证过程中为让 generate 跑通而插入的**测试夹具**。
--       **不补任何资金流水**（补 SALE 流水等于把假单洗成真单，故 A1 只能靠删夹具回到 0）。
--
-- 夹具清单（均由 N19-4 验证产生）：
--   orders                order_id='N194-ORD-369' / order_number='N194-NO-369'，2000 分，payment_status=2
--   order_payment_records payment_id=1217（order_id='N194-ORD-369'，payment_method=1 现金，2000 分）
--   kitchen_order         kitchen_order_id='KON194-ORD-369'（订单派生厨房单，随订单删除）
--   deposit_slips         slip_id=301 / slip_no='DS202610270005'（shift_id=369，由夹具订单 generate 产生）
--
-- 保留：pos_shifts.shift_id=369（status=completed，仅作历史；删单后无任何悬空引用）
--
-- 幂等：所有 DELETE 按精确业务键匹配，重复执行结果 0 行。
-- 用法：
--   正式执行  psql -f scripts/db-fix/2026-10-27-cleanup-n194-fixture.sql
--   试跑     先把末尾 COMMIT; 改为 ROLLBACK; 再执行
-- ============================================================
BEGIN;

-- ---------- 0) 清理前计数 ----------
SELECT 'before: orders' AS step, count(*) AS rows
  FROM orders WHERE order_id = 'N194-ORD-369'
UNION ALL SELECT 'before: order_payment_records',
       count(*) FROM order_payment_records WHERE order_id = 'N194-ORD-369' OR payment_id = 1217
UNION ALL SELECT 'before: kitchen_order',
       count(*) FROM kitchen_order WHERE order_id = 'N194-ORD-369'
UNION ALL SELECT 'before: deposit_slips',
       count(*) FROM deposit_slips WHERE shift_id = 369 OR slip_no = 'DS202610270005'
UNION ALL SELECT 'before: fund_flows(slip)',
       count(*) FROM fund_flows WHERE remark LIKE '%DS202610270005%'
UNION ALL SELECT 'before: refund_shift_attribution',
       count(*) FROM refund_shift_attribution
 WHERE slip_id IN (SELECT slip_id FROM deposit_slips WHERE shift_id = 369 OR slip_no = 'DS202610270005');

-- ---------- 1) 缴款单侧：转账流水 → 归属表 → 缴款单本体 ----------
-- 当前应为 0 行（DRAFT 单据不产生转账流水）；语句保留以应对环境差异
WITH d AS (
    DELETE FROM fund_flows WHERE remark LIKE '%DS202610270005%' RETURNING 1
) SELECT 'deleted: fund_flows(slip)' AS step, count(*) AS rows FROM d;

WITH d AS (
    DELETE FROM refund_shift_attribution
     WHERE slip_id IN (SELECT slip_id FROM deposit_slips WHERE shift_id = 369 OR slip_no = 'DS202610270005')
    RETURNING 1
) SELECT 'deleted: refund_shift_attribution' AS step, count(*) AS rows FROM d;

WITH d AS (
    DELETE FROM deposit_slips WHERE shift_id = 369 OR slip_no = 'DS202610270005' RETURNING 1
) SELECT 'deleted: deposit_slips' AS step, count(*) AS rows FROM d;

-- ---------- 2) 支付记录 ----------
WITH d AS (
    DELETE FROM order_payment_records WHERE order_id = 'N194-ORD-369' OR payment_id = 1217 RETURNING 1
) SELECT 'deleted: order_payment_records' AS step, count(*) AS rows FROM d;

-- ---------- 3) 订单派生行（厨房单等） ----------
WITH d AS (
    DELETE FROM kitchen_order WHERE order_id = 'N194-ORD-369' RETURNING 1
) SELECT 'deleted: kitchen_order' AS step, count(*) AS rows FROM d;

-- ---------- 4) 订单本体 ----------
WITH d AS (
    DELETE FROM orders WHERE order_id = 'N194-ORD-369' RETURNING 1
) SELECT 'deleted: orders' AS step, count(*) AS rows FROM d;

-- ---------- 5) 清理后残留检查（全部应为 0） ----------
SELECT 'after: orders' AS step, count(*) AS rows
  FROM orders WHERE order_id = 'N194-ORD-369'
UNION ALL SELECT 'after: order_payment_records',
       count(*) FROM order_payment_records WHERE order_id = 'N194-ORD-369' OR payment_id = 1217
UNION ALL SELECT 'after: kitchen_order',
       count(*) FROM kitchen_order WHERE order_id = 'N194-ORD-369'
UNION ALL SELECT 'after: deposit_slips(shift 369)',
       count(*) FROM deposit_slips WHERE shift_id = 369
UNION ALL SELECT 'after: fund_flows(slip)',
       count(*) FROM fund_flows WHERE remark LIKE '%DS202610270005%'
UNION ALL SELECT 'after: pos_shifts.shift_id=369 (保留，应为 1)',
       count(*) FROM pos_shifts WHERE shift_id = 369;

COMMIT;
