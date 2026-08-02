-- ============================================================
-- 付款单表增加凭证号、流水号字段
-- 创建日期：2026-07-19
-- 说明：付款单四账联动后，需在付款单上冗余保存关联的会计凭证号
--       与资金流水号，使付款历史能够直接展示，无需额外查询。
-- ============================================================

ALTER TABLE payment ADD COLUMN IF NOT EXISTS voucher_no VARCHAR(32);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS fund_flow_no VARCHAR(32);

-- 回填历史数据：根据已保存的 voucher_id / fund_flow_id 带出编号
UPDATE payment p
SET voucher_no = (SELECT voucher_no FROM finance_vouchers v WHERE v.voucher_id = p.voucher_id)
WHERE p.voucher_id IS NOT NULL
  AND p.voucher_no IS NULL;

UPDATE payment p
SET fund_flow_no = (SELECT flow_no FROM fund_flows f WHERE f.flow_id = p.fund_flow_id)
WHERE p.fund_flow_id IS NOT NULL
  AND p.fund_flow_no IS NULL;
