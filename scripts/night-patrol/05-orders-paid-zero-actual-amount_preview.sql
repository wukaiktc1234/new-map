/* =============================================================================
 * 文件    : 05-orders-paid-zero-actual-amount_preview.sql
 * 标题    : 已支付订单 actual_amount 恒为 0（DATA-006 的真正根因面）
 * 目标    : 定位 payment_status=2 但 actual_amount=0 的订单，并核对 finance_records 是否也同步记为 0 元收入
 * 预查影响: 25 行（全库 payment_status=2 且 deleted=0 的订单共 25 条，actual_amount 全部为 0.00；对应 finance_records 收入流水 32 条中有 10 条 amount=0）
 * 回滚    : 见 [ROLLBACK] 段：UPDATE orders SET actual_amount = 0 WHERE order_id IN (...)（原始值恒为 0，回滚即置回 0）
 * 幂等守卫: 变更语句带 coalesce(actual_amount,0)=0 条件，重复执行影响 0 行
 * 用法    : psql -f 05-orders-paid-zero-actual-amount_preview.sql     —— 默认**只跑 SELECT 预览**，不产生任何变更
 * 变更段  : 全部包在 [OWNER-EXECUTE] 注释块内，解除注释后才会生效，需 Owner 执行
 * 红线    : 本文件只读；不含任何物理移除语句、不含 DDL
 * 时点    : 2026-10-23（分支 ai-night-patrol-1，只读巡检产出）
 * ========================================================================== */

SET client_encoding = 'UTF8';

-- ============================ 1. 预览：25 行 ============================
SELECT o.order_id,
       o.order_code,
       o.payment_status,
       o.total_amount,
       o.final_amount,
       coalesce(o.actual_amount, 0) AS actual_amount,
       o.paid_amount,
       o.discount_amount,
       o.create_time::date AS create_date
FROM orders o
WHERE o.deleted = 0
  AND o.payment_status = 2
  AND coalesce(o.actual_amount, 0) = 0
ORDER BY o.create_time;

SELECT count(*) AS affected_rows
FROM orders o
WHERE o.deleted = 0
  AND o.payment_status = 2
  AND coalesce(o.actual_amount, 0) = 0;

-- ============================ 2. 与财务流水对账（0 元收入是否成对出现） ============================
SELECT o.order_code,
       o.final_amount,
       coalesce(o.actual_amount, 0) AS actual_amount,
       f.record_no,
       f.amount AS finance_amount,
       f.deleted AS finance_deleted
FROM orders o
LEFT JOIN finance_records f
       ON f.order_code = o.order_code
      AND f.record_type = '1'
      AND f.deleted = 0
WHERE o.deleted = 0
  AND o.payment_status = 2
  AND coalesce(o.actual_amount, 0) = 0
ORDER BY o.order_code;

-- ============================ 3. 金额口径核对：final_amount / total_amount / paid_amount 三列关系 ============================
-- 若三者一致而 actual_amount 单独为 0，说明是「写回漏字段」而不是「真 0 元订单」
SELECT count(*) AS rows_cnt,
       count(*) FILTER (WHERE coalesce(final_amount,0) = coalesce(total_amount,0)) AS final_eq_total,
       count(*) FILTER (WHERE coalesce(paid_amount,0) > 0) AS has_paid_amount,
       count(*) FILTER (WHERE coalesce(total_amount,0) = 0) AS real_zero_total
FROM orders
WHERE deleted = 0 AND payment_status = 2 AND coalesce(actual_amount, 0) = 0;

/* ============================ [OWNER-EXECUTE] 变更段 ============================
 * ⚠️ 这是**财务口径回填**，不是脏数据清理。必须先由 Owner 确认 canonical 取值列
 *    （候选：final_amount > total_amount > paid_amount，三列当前存在差异，不可自动选择）。
 *    下表仅为模板，确认后替换回填表达式再执行。
 *
 * UPDATE orders
 *    SET actual_amount = coalesce(final_amount, total_amount, 0),
 *        update_time = now()
 *  WHERE deleted = 0
 *    AND payment_status = 2
 *    AND coalesce(actual_amount, 0) = 0;
 * -- 预计影响：25 行
 * ================================================================================ */

/* ============================ [ROLLBACK] 回滚 ============================
 * UPDATE orders SET actual_amount = 0, update_time = now()
 *  WHERE deleted = 0 AND payment_status = 2
 *    AND order_code IN ('T20260915011', ... 第 1 段导出的 25 个 order_code ...);
 * -- 原始值恒为 0，按 order_code 白名单回滚最安全
 * ======================================================================== */
