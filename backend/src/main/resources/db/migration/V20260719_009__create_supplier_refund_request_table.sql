-- ============================================================
-- 供应商退款申请单表
-- 创建时间：2026-07-20
-- 用途：
--   1. 采购退货选择现金退款时生成实际资金退款申请；
--   2. 记录退款金额、状态及到账确认，支持独立审批流。
-- 兼容 H2 (MODE=PostgreSQL) + PostgreSQL 18
-- ============================================================

CREATE TABLE IF NOT EXISTS supplier_refund_requests (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    return_id BIGINT NOT NULL,
    supplier_id BIGINT,
    supplier_name VARCHAR(200),
    amount BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) DEFAULT 'pending',
    remark TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INTEGER NOT NULL DEFAULT 0
);

COMMENT ON TABLE supplier_refund_requests IS '供应商退款申请单';
COMMENT ON COLUMN supplier_refund_requests.return_id IS '关联采购退货单ID';
COMMENT ON COLUMN supplier_refund_requests.amount IS '退款金额（单位：分）';
COMMENT ON COLUMN supplier_refund_requests.status IS '状态：pending(待处理) / completed(已到账) / rejected(已驳回)';

CREATE INDEX IF NOT EXISTS idx_supplier_refund_requests_return_id ON supplier_refund_requests(return_id);
CREATE INDEX IF NOT EXISTS idx_supplier_refund_requests_supplier_id ON supplier_refund_requests(supplier_id);
CREATE INDEX IF NOT EXISTS idx_supplier_refund_requests_status ON supplier_refund_requests(status);
