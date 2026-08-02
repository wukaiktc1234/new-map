-- ============================================================
-- 采购结算单子模块建表脚本
-- 对应实体: PurchaseSettlement (purchase_settlements)
-- 对应前端 API: /v1/purchase/settlements
--
-- 状态编码（数据库 INTEGER）：
--   status:         0=待结算 1=部分结算 2=财务审核中 3=已完成 4=已逾期
--   invoice_status: 0=未开票 1=已开票 2=已收票
-- 金额单位：数据库存储分（BIGINT），前端显示元（number）
--
-- 注意：H2 开发环境由 PurchaseDatabaseInitializer.createPurchaseSettlementTable 建表，
--       本脚本仅在生产环境（PostgreSQL）执行。
-- ============================================================

-- 采购结算单主表
CREATE TABLE IF NOT EXISTS purchase_settlements (
    settlement_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    settlement_no   VARCHAR(50)    NOT NULL,
    order_id        BIGINT,
    order_no        VARCHAR(50),
    supplier_id     BIGINT,
    supplier_name   VARCHAR(200),
    total_amount    BIGINT         NOT NULL DEFAULT 0,
    paid_amount     BIGINT         NOT NULL DEFAULT 0,
    unpaid_amount   BIGINT         NOT NULL DEFAULT 0,
    due_date        DATE,
    status          INTEGER        NOT NULL DEFAULT 0,
    payment_method  VARCHAR(50),
    invoice_no      VARCHAR(100),
    invoice_status  INTEGER        NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    create_user_id  BIGINT,
    create_time     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  purchase_settlements IS '采购结算单主表';
COMMENT ON COLUMN purchase_settlements.settlement_id IS '结算单主键ID（自增）';
COMMENT ON COLUMN purchase_settlements.settlement_no IS '结算单编号（如 STL20260629001）';
COMMENT ON COLUMN purchase_settlements.order_id IS '关联采购订单ID';
COMMENT ON COLUMN purchase_settlements.order_no IS '采购订单编号（冗余字段，前端展示用）';
COMMENT ON COLUMN purchase_settlements.supplier_id IS '供应商ID';
COMMENT ON COLUMN purchase_settlements.supplier_name IS '供应商名称（冗余字段，前端展示用）';
COMMENT ON COLUMN purchase_settlements.total_amount IS '总金额（单位：分）';
COMMENT ON COLUMN purchase_settlements.paid_amount IS '已付金额（单位：分）';
COMMENT ON COLUMN purchase_settlements.unpaid_amount IS '未付金额（单位：分）';
COMMENT ON COLUMN purchase_settlements.due_date IS '到期日期';
COMMENT ON COLUMN purchase_settlements.status IS '状态：0待结算 1部分结算 2财务审核中 3已完成 4逾期';
COMMENT ON COLUMN purchase_settlements.payment_method IS '付款方式';
COMMENT ON COLUMN purchase_settlements.invoice_no IS '发票号';
COMMENT ON COLUMN purchase_settlements.invoice_status IS '发票状态：0未开票 1已开票 2已收票';
COMMENT ON COLUMN purchase_settlements.remark IS '备注';
COMMENT ON COLUMN purchase_settlements.create_user_id IS '创建人ID';
COMMENT ON COLUMN purchase_settlements.create_time IS '创建时间';
COMMENT ON COLUMN purchase_settlements.update_time IS '更新时间';
COMMENT ON COLUMN purchase_settlements.deleted IS '逻辑删除：0未删除 1已删除';

-- 索引
CREATE INDEX IF NOT EXISTS idx_purchase_settlement_order_id    ON purchase_settlements (order_id);
CREATE INDEX IF NOT EXISTS idx_purchase_settlement_supplier_id ON purchase_settlements (supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_settlement_status      ON purchase_settlements (status);
CREATE INDEX IF NOT EXISTS idx_purchase_settlement_due_date     ON purchase_settlements (due_date);

-- 唯一索引（结算单编号）
CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_settlement_settlement_no ON purchase_settlements (settlement_no);
