-- ============================================================
-- fund_flows 统计索引（模块 1.2 追加，2026-10-23）
--
-- 背景：财务大盘/交班对账按「账户 + 业务日期 + 收支方向」聚合，fund_flows 原先只有单列索引
--       （account / date / direction 各一个），组合统计仍需多次 BitmapOr 或顺扫。
-- 做法：加一个与查询口径一致的部分组合索引（deleted = 0）；幂等（IF NOT EXISTS），可反复执行。
-- 用法：psql -h localhost -p 5432 -U postgres -d food_traceability -f <本文件>
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_fund_flows_account_date_direction
    ON fund_flows (account_id, business_date, flow_direction)
    WHERE deleted = 0;

-- 自查：
-- SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'fund_flows' ORDER BY indexname;
