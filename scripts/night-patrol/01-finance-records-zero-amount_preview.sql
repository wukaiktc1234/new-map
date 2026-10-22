/* =============================================================================
 * 文件    : 01-finance-records-zero-amount_preview.sql
 * 标题    : DATA-006 历史 0 元「订单收入」流水
 * 目标    : 定位 finance_records 中 record_type='1'（订单收入）、amount=0 且 deleted=0 的历史脏流水，并核对关联订单是否真实存在
 * 预查影响: 10 行（2026-10-23 psql 实测：10 行全部 deleted=0，10 条 order_code 在 orders 中全部命中；全库 32 条收入流水里恰好这 10 条为 0 元）
 * 回滚    : 见文件末尾 [ROLLBACK] 段：UPDATE finance_records SET deleted=0 WHERE record_no IN (10 个单号) AND deleted=1（按单号精确回滚，不依赖 id）
 * 幂等守卫: 变更语句带 deleted = 0 AND amount = 0 双条件，重复执行不重复影响
 * 用法    : psql -f 01-finance-records-zero-amount_preview.sql     —— 默认**只跑 SELECT 预览**，不产生任何变更
 * 变更段  : 全部包在 [OWNER-EXECUTE] 注释块内，解除注释后才会生效，需 Owner 执行
 * 红线    : 本文件只读 + 软标记；不含任何物理移除语句、不含 DDL
 * 时点    : 2026-10-23（分支 ai-night-patrol-1，只读巡检产出）
 * ========================================================================== */

SET client_encoding = 'UTF8';

-- ============================ 1. 预览：待处理 10 行 ============================
SELECT record_id,
       record_no,
       record_type,
       record_category,
       amount,
       deleted,
       order_code,
       business_date,
       create_time::date AS create_date
FROM finance_records
WHERE record_no IN ('SZ202609170001','SZ202609210001','SZ202609250002','SZ202609260002',
                    'SZ202609300002','SZ202609300005','SZ202609300008','SZ202609300011',
                    'SZ202610230005','SZ202610230007')
  AND record_type = '1'
ORDER BY record_no;

-- 影响行数（应恒为 10；变化即说明有人动过数据，请重新评估）
SELECT count(*) AS affected_rows
FROM finance_records
WHERE record_no IN ('SZ202609170001','SZ202609210001','SZ202609250002','SZ202609260002',
                    'SZ202609300002','SZ202609300005','SZ202609300008','SZ202609300011',
                    'SZ202610230005','SZ202610230007')
  AND record_type = '1'
  AND amount = 0
  AND deleted = 0;

-- ============================ 2. 关联订单核实 ============================
-- 预期 10 行全命中、payment_status=2（已支付）；若某单号查不到，说明该流水是纯脏记录
SELECT f.record_no,
       f.order_code,
       o.order_id,
       o.order_status,
       o.payment_status,
       o.total_amount,
       o.final_amount,
       o.actual_amount,
       o.deleted AS order_deleted
FROM finance_records f
LEFT JOIN orders o ON o.order_code = f.order_code
WHERE f.record_no IN ('SZ202609170001','SZ202609210001','SZ202609250002','SZ202609260002',
                      'SZ202609300002','SZ202609300005','SZ202609300008','SZ202609300011',
                      'SZ202610230005','SZ202610230007')
ORDER BY f.record_no;

-- 关联订单缺失计数（预期 0）
SELECT count(*) AS orders_missing
FROM finance_records f
WHERE f.record_no IN ('SZ202609170001','SZ202609210001','SZ202609250002','SZ202609260002',
                      'SZ202609300002','SZ202609300005','SZ202609300008','SZ202609300011',
                      'SZ202610230005','SZ202610230007')
  AND NOT EXISTS (SELECT 1 FROM orders o WHERE o.order_code = f.order_code);

-- ============================ 3. 前置阻断检查 ============================
-- 结论：这 10 条流水对应的 order.actual_amount 也是 0.00（全库 25 条已支付订单 actual_amount 全为 0）。
-- 也就是说：软删流水**不会**修正报表总额，只会让「0 元收入」痕迹消失。先执行下面这条确认：
SELECT count(*) AS paid_orders,
       count(*) FILTER (WHERE coalesce(actual_amount, 0) = 0) AS paid_with_zero_actual
FROM orders
WHERE deleted = 0
  AND payment_status = 2;

/* ============================ [OWNER-EXECUTE] 变更段 ============================
 * ⚠️ 执行前必须先决策：是把这 10 条软删（口径上「作废」），还是回填 amount（从 orders.final_amount）。
 *    两者选其一，不要同时执行。下方默认给出「软删」版本，需 Owner 解除注释后执行。
 *
 * UPDATE finance_records
 *    SET deleted = 1,
 *        update_time = now(),
 *        remark = COALESCE(NULLIF(btrim(remark), ''), '') || ' [night-patrol: DATA-006 0元流水作废]'
 *  WHERE record_no IN ('SZ202609170001','SZ202609210001','SZ202609250002','SZ202609260002',
 *                      'SZ202609300002','SZ202609300005','SZ202609300008','SZ202609300011',
 *                      'SZ202610230005','SZ202610230007')
 *    AND record_type = '1'
 *    AND amount = 0
 *    AND deleted = 0;
 * -- 预计影响：10 行
 * ================================================================================ */

/* ============================ [ROLLBACK] 回滚 ============================
 * UPDATE finance_records
 *    SET deleted = 0,
 *        update_time = now()
 *  WHERE record_no IN ('SZ202609170001','SZ202609210001','SZ202609250002','SZ202609260002',
 *                      'SZ202609300002','SZ202609300005','SZ202609300008','SZ202609300011',
 *                      'SZ202610230005','SZ202610230007')
 *    AND record_type = '1'
 *    AND deleted = 1;
 * -- 按单号回滚，与变更段一一对应；不依赖 record_id（避免序列漂移）
 * ======================================================================== */

-- ============================ 4. 变更后复核（Owner 执行后跑） ============================
SELECT count(*) FILTER (WHERE deleted = 0) AS still_open,
       count(*) FILTER (WHERE deleted = 1) AS done
FROM finance_records
WHERE record_no IN ('SZ202609170001','SZ202609210001','SZ202609250002','SZ202609260002',
                    'SZ202609300002','SZ202609300005','SZ202609300008','SZ202609300011',
                    'SZ202610230005','SZ202610230007')
  AND record_type = '1';
