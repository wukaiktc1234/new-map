-- ============================================================
-- RM-B2-实施卡3：idx_inventory_batch_no 索引落库
-- 来源：docs/quality/rm-b1-001-inventory-alignment-plan.md §七/§十 事项 3（架构已批准）
--      + qa 实测 OBS-4（真实表已手工存在该索引，migration 零声明，rm-b1-qa-report.md §1.4）
-- 幂等：CREATE INDEX IF NOT EXISTS —— 对已手工存在索引的真实表为 no-op（NOTICE），对新环境为补齐动作
-- 仅此一个索引；idx_inventory_expiry_date / idx_inventory_location_id（OBS-5/6）不在本卡范围，只登记不处理
-- 不删任何列/索引；不触碰业务代码
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_inventory_batch_no ON inventory (batch_no);