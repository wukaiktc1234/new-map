-- ============================================================
-- 日结对账功能增强 - 支付方式明细字段扩展（管理端版）
-- 版本: v2.1 (4种支付方式：现金/微信/支付宝/会员卡)
-- 执行前请备份数据！
-- ============================================================

BEGIN;

-- -----------------------------------------------------------
-- 表1: daily_settlements（日结主表）- 新增6个字段
-- -----------------------------------------------------------

ALTER TABLE daily_settlements
  ADD COLUMN IF NOT EXISTS payment_breakdown JSONB DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS total_discount_amount BIGINT DEFAULT 0,
  ADD COLUMN IF NOT EXISTS refund_amount BIGINT DEFAULT 0,
  ADD COLUMN IF NOT EXISTS refund_count INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS cancelled_amount BIGINT DEFAULT 0,
  ADD COLUMN IF NOT EXISTS cancelled_count INTEGER DEFAULT 0;

COMMENT ON COLUMN daily_settlements.payment_breakdown IS '支付方式明细JSONB（cash/wechat/alipay/memberBalance）';
COMMENT ON COLUMN daily_settlements.total_discount_amount IS '优惠总金额（分，从订单DISCOUNT_AMOUNT聚合）';
COMMENT ON COLUMN daily_settlements.refund_amount IS '退款总金额（分）';
COMMENT ON COLUMN daily_settlements.refund_count IS '退款总笔数';
COMMENT ON COLUMN daily_settlements.cancelled_amount IS '作废订单总金额（分）';
COMMENT ON COLUMN daily_settlements.cancelled_count IS '作废订单总笔数';

-- 创建 JSONB 索引（加速查询）
CREATE INDEX IF NOT EXISTS idx_daily_settlements_payment_breakdown
  ON daily_settlements USING GIN (payment_breakdown);

-- -----------------------------------------------------------
-- 表2: daily_settlement_shifts（班次明细表）- 新增3个字段（仅用于展示）
-- -----------------------------------------------------------

ALTER TABLE daily_settlement_shifts
  ADD COLUMN IF NOT EXISTS payment_breakdown JSONB DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS cashier_id VARCHAR(32) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS cashier_name VARCHAR(50) DEFAULT NULL;

COMMENT ON COLUMN daily_settlement_shifts.payment_breakdown IS '该班次支付方式明细JSONB';
COMMENT ON COLUMN daily_settlement_shifts.cashier_id IS '收银员ID（仅用于展示）';
COMMENT ON COLUMN daily_settlement_shifts.cashier_name IS '收银员姓名（仅用于展示）';

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_daily_settlement_shifts_payment_breakdown
  ON daily_settlement_shifts USING GIN (payment_breakdown);

CREATE INDEX IF NOT EXISTS idx_daily_settlement_shifts_cashier_id
  ON daily_settlement_shifts (cashier_id);

COMMIT;
