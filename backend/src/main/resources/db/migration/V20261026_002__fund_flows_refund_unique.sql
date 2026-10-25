-- =====================================================================
-- V20261026_002__fund_flows_refund_unique.sql
-- P4-5 退款侧勾稽（TD-31）：退款流水唯一性兜底
--   同一 order_id 只允许一笔退款流水（flow_category = 2，退款专用类别）
--   应用层「先查后写」是第一道；本索引是并发兜底。
-- 执行前已校验：category=2 现网 0 行、无重复 order_id、无 NULL order_id（不会因存量数据建索引失败）。
-- =====================================================================
CREATE UNIQUE INDEX IF NOT EXISTS uk_fund_flows_refund_order
    ON fund_flows (order_id)
    WHERE flow_category = 2 AND deleted = 0;
