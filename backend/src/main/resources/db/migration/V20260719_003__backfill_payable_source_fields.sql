-- 回填历史应付账款的来源字段（stockin_id / stockin_no / order_no）
-- 背景：payables 表近期新增来源字段，历史记录这三个字段为 NULL，导致应付账款列表的采购订单号/入库单号显示空白。
-- 策略：根据 payables.purchase_order_id 反查 purchase_stockins，按创建时间取该订单下第一个入库单回填；
--       同时根据 purchase_order_id 反查 purchase_orders 回填 order_no。
-- 注意：本脚本仅适用于 PostgreSQL；H2 开发环境由 FinanceDatabaseInitializer 在启动时自动回填。

UPDATE payables p
SET
    stockin_id = sub.stockin_id,
    stockin_no = sub.stockin_code,
    order_no   = COALESCE(sub.order_code, p.order_no)
FROM (
    SELECT DISTINCT ON (ps.order_id)
        ps.order_id,
        ps.stockin_id,
        ps.stockin_code,
        po.order_code
    FROM purchase_stockins ps
    LEFT JOIN purchase_orders po ON po.order_id = ps.order_id
    WHERE ps.order_id IS NOT NULL
    ORDER BY ps.order_id, ps.create_time ASC
) sub
WHERE p.stockin_id IS NULL
  AND p.purchase_order_id IS NOT NULL
  AND p.purchase_order_id = sub.order_id;
