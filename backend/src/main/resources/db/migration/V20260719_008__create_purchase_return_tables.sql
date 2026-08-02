-- ============================================================
-- 采购退货模块表结构
-- 创建时间：2026-07-19
-- 说明：
--   1. 支持退货单关联原采购入库单，并记录多物料退货明细。
--   2. 退货审批通过后生成红字应付单（payables 金额为负），
--      通过 related_invoice_id 关联原正向应付单。
--   3. 库存扣减按总量执行，批次号仅作为辅助信息记录。
-- 兼容 H2 (MODE=PostgreSQL) + PostgreSQL 18
-- ============================================================

-- 采购退货单主表
CREATE TABLE IF NOT EXISTS purchase_returns (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    return_no VARCHAR(32) NOT NULL UNIQUE,
    stockin_id BIGINT,
    stockin_no VARCHAR(32),
    order_id BIGINT,
    order_no VARCHAR(100),
    supplier_id BIGINT,
    supplier_name VARCHAR(200),
    warehouse_id BIGINT,
    return_date DATE NOT NULL DEFAULT CURRENT_DATE,
    total_quantity DECIMAL(18,3) NOT NULL DEFAULT 0,
    total_amount BIGINT NOT NULL DEFAULT 0,
    refund_method VARCHAR(20) DEFAULT 'offset',
    status VARCHAR(20) DEFAULT 'pending',
    approval_remark TEXT,
    related_payable_id BIGINT,
    created_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

COMMENT ON TABLE purchase_returns IS '采购退货单主表';
COMMENT ON COLUMN purchase_returns.return_no IS '退货单号';
COMMENT ON COLUMN purchase_returns.stockin_id IS '关联原入库单ID';
COMMENT ON COLUMN purchase_returns.refund_method IS '退款方式：offset(冲抵) / cash(现金退款)';
COMMENT ON COLUMN purchase_returns.status IS '状态：pending(待审批) / approved(已通过) / rejected(已驳回) / completed(已完成)';
COMMENT ON COLUMN purchase_returns.related_payable_id IS '关联生成的红字应付单ID';

CREATE INDEX IF NOT EXISTS idx_purchase_returns_stockin_id ON purchase_returns(stockin_id);
CREATE INDEX IF NOT EXISTS idx_purchase_returns_supplier_id ON purchase_returns(supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_returns_status ON purchase_returns(status);

-- 采购退货单明细表
CREATE TABLE IF NOT EXISTS purchase_return_items (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    return_id BIGINT NOT NULL,
    stockin_item_id BIGINT,
    material_id BIGINT,
    material_name VARCHAR(200),
    specification VARCHAR(500),
    unit VARCHAR(50),
    quantity DECIMAL(18,3) NOT NULL DEFAULT 0,
    unit_price BIGINT NOT NULL DEFAULT 0,
    total_amount BIGINT NOT NULL DEFAULT 0,
    return_reason VARCHAR(500),
    batch_no VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

COMMENT ON TABLE purchase_return_items IS '采购退货单明细表';
COMMENT ON COLUMN purchase_return_items.stockin_item_id IS '关联原入库单明细ID';
COMMENT ON COLUMN purchase_return_items.batch_no IS '入库批次号（辅助）';

CREATE INDEX IF NOT EXISTS idx_purchase_return_items_return_id ON purchase_return_items(return_id);
CREATE INDEX IF NOT EXISTS idx_purchase_return_items_material_id ON purchase_return_items(material_id);

-- 应付账款表增加红字单关联字段
ALTER TABLE payables ADD COLUMN IF NOT EXISTS related_invoice_id BIGINT;
ALTER TABLE payables ADD COLUMN IF NOT EXISTS invoice_type VARCHAR(20) DEFAULT 'blue';

COMMENT ON COLUMN payables.related_invoice_id IS '关联原正向应付单ID（红字单使用）';
COMMENT ON COLUMN payables.invoice_type IS '单据类型：blue(蓝字正向) / red(红字退货)';

CREATE INDEX IF NOT EXISTS idx_payables_related_invoice_id ON payables(related_invoice_id);
CREATE INDEX IF NOT EXISTS idx_payables_invoice_type ON payables(invoice_type);

-- 供应商表增加可抵扣退货余额字段（单位：分）
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS return_credit_balance BIGINT DEFAULT 0;

COMMENT ON COLUMN suppliers.return_credit_balance IS '可抵扣退货余额（单位：分）';
