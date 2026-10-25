-- =====================================================================
-- V20261026_003__fund_flows_refund_record_id.sql
-- P4-5 R1：退款流水配对键从 order_id 改为 **order_refund_records.refund_id**
--   原因：同一订单可以有多笔「部分退款」，按 order_id 建唯一会把第二笔挡死；
--        退款单号才是退款动作的天然主键。
-- =====================================================================

-- 1) 新列（退款单号）
ALTER TABLE fund_flows ADD COLUMN IF NOT EXISTS refund_record_id bigint;

-- 2) 退役按订单的唯一索引
DROP INDEX IF EXISTS uk_fund_flows_refund_order;

-- 3) 按退款单建唯一（退款类别=2 的未删除行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_fund_flows_refund_record
    ON fund_flows (refund_record_id)
    WHERE flow_category = 2 AND deleted = 0;

-- 4) 普通索引（配对查询/回溯）
CREATE INDEX IF NOT EXISTS idx_fund_flows_refund_record
    ON fund_flows (refund_record_id)
    WHERE deleted = 0;
