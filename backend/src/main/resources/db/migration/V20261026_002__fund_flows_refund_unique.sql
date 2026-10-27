-- =====================================================================
-- V20261026_002__fund_flows_refund_unique.sql
-- P4-5 退款侧勾稽（TD-31）：退款流水唯一性兜底
--   同一 order_id 只允许一笔退款流水（flow_category = 2，退款专用类别）
--   应用层「先查后写」是第一道；本索引是并发兜底。
-- 执行前已校验：category=2 现网 0 行、无重复 order_id、无 NULL order_id（不会因存量数据建索引失败）。
-- =====================================================================
-- 从零建库修复（bug-⑦/⑧ 同族，第 4 处）：本索引引用 fund_flows.order_id，
-- 但该列是历史演进出来的、任何迁移都没创建它 → 新库跑到这里报「字段 order_id 不存在」。
-- 处置：先确保列存在（与活库一致 bigint），再建索引；活库为 IF NOT EXISTS 无操作。
ALTER TABLE fund_flows ADD COLUMN IF NOT EXISTS order_id bigint;

CREATE UNIQUE INDEX IF NOT EXISTS uk_fund_flows_refund_order
    ON fund_flows (order_id)
    WHERE flow_category = 2 AND deleted = 0;
