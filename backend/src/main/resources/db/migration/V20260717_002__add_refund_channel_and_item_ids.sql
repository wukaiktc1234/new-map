-- ============================================================
-- 退款链路修复：补充支付渠道状态与退款明细字段
-- 关联问题：DF-022（退款未实际执行）/ DF-024（部分退款不回补库存）
-- 修复内容：
--   1. 添加 payment_channel_status 字段（支付渠道状态）
--      0=未提交 1=待渠道处理 2=渠道处理成功 3=渠道处理失败
--      用于退款记录持久化方案：approveRefund 时标记为"待渠道处理"，
--      后续接入真实支付 SDK 时再实现实际退款
--   2. 添加 payment_channel_submit_time 字段（支付渠道提交时间）
--   3. 添加 refund_item_ids 字段（退款明细ID列表，CSV格式）
--      用于部分退款精确回补库存：存储 OrderItemNew.itemId 列表
-- ============================================================

-- 1. 支付渠道状态字段
ALTER TABLE order_refund_records ADD COLUMN IF NOT EXISTS payment_channel_status INTEGER DEFAULT 0;
COMMENT ON COLUMN order_refund_records.payment_channel_status IS '支付渠道状态: 0未提交 1待渠道处理 2渠道处理成功 3渠道处理失败';

-- 2. 支付渠道提交时间
ALTER TABLE order_refund_records ADD COLUMN IF NOT EXISTS payment_channel_submit_time TIMESTAMP;
COMMENT ON COLUMN order_refund_records.payment_channel_submit_time IS '支付渠道提交时间（退款提交到支付渠道的时间）';

-- 3. 退款明细ID列表（CSV格式）
ALTER TABLE order_refund_records ADD COLUMN IF NOT EXISTS refund_item_ids VARCHAR(2000);
COMMENT ON COLUMN order_refund_records.refund_item_ids IS '退款明细ID列表（CSV格式，部分退款时使用，匹配 OrderItemNew.itemId）';
