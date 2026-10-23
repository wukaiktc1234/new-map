-- ============================================================
-- fund_flows 增加 order_id + 备注清洗（2026-10-24）
--
-- 背景
--   1) 幂等键此前塞在 remark 里：'POS销售入账：T20261024015[orderId=O1792772003471]'，
--      前端表格 tooltip 把技术串原样弹给业务人员；
--   2) 列表接口按单号搜索只能靠 remark like，语义脆弱。
--   本次：新增 order_id 列承载订单关联（可空），把 remark 清洗成人类可读的
--        'POS销售入账：<orderNumber>'，幂等与搜索都改走 order_id。
--
-- 幂等
--   全部 IF NOT EXISTS / 条件 UPDATE：ADD COLUMN、回填（order_id IS NULL）、清洗（remark LIKE '%[orderId=%'）、
--   建索引均只在需要时生效，可反复执行。
--
-- 用法：psql -h localhost -p 5432 -U postgres -d food_traceability -f <本文件>
-- ============================================================

-- 1) 新列：业务订单ID（varchar(64)，可空：非 POS 流水/历史行为空）
ALTER TABLE fund_flows ADD COLUMN IF NOT EXISTS order_id varchar(64);
COMMENT ON COLUMN fund_flows.order_id IS '业务订单ID（POS销售入账等；历史行由 remark 中的 [orderId=...] 回填）';

-- 2) 从旧 remark 回填 order_id（只处理 order_id 仍为空的行）
UPDATE fund_flows
SET order_id = substring(remark from '\[orderId=([^\]]+)\]')
WHERE order_id IS NULL
  AND remark LIKE '%[orderId=%';

-- 3) 清洗 remark：去掉 [orderId=...] 技术串，压缩多余空格
UPDATE fund_flows
SET remark = btrim(regexp_replace(regexp_replace(remark, '\s*\[orderId=[^\]]+\]', '', 'g'), '\s{2,}', ' ', 'g'))
WHERE remark LIKE '%[orderId=%';

-- 4) 索引：按订单反查流水；按账户+订单组合查询（对账/详情穿透）
CREATE INDEX IF NOT EXISTS idx_fund_flows_order
    ON fund_flows (order_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_fund_flows_account_order
    ON fund_flows (account_id, order_id) WHERE deleted = 0;

-- ------------------------------------------------------------
-- 自查（执行后应满足：残留 token = 0；POS 流水 order_id 覆盖率 = 100%）
-- ------------------------------------------------------------
-- SELECT count(*) AS token_left FROM fund_flows WHERE remark LIKE '%[orderId=%';
-- SELECT count(*) FILTER (WHERE order_id IS NULL) AS no_order_id, count(*) AS pos_flows
-- FROM fund_flows WHERE remark LIKE 'POS%入账%';
