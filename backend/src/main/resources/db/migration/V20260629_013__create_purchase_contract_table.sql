-- ============================================================
-- 采购合同子模块建表脚本
-- 对应实体: PurchaseContract (purchase_contract)
-- 对应前端 API: /v1/purchase/contracts
--
-- 状态字段（VARCHAR(20)，字符串）：
--   status: draft=草稿 signed=已签署 terminated=已终止
-- 金额单位：数据库存储元（DECIMAL(12,2)），与实体 BigDecimal 字段对齐
--   注：本模块金额未采用"分"约定，遵循实体类 BigDecimal 定义
--
-- 注意：H2 开发环境由 PurchaseDatabaseInitializer.createPurchaseContractTable 建表，
--       本脚本仅在生产环境（PostgreSQL）执行。
-- ============================================================

-- 采购合同主表
CREATE TABLE IF NOT EXISTS purchase_contract (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_no     VARCHAR(50),
    contract_name   VARCHAR(200),
    supplier_id     BIGINT,
    supplier_name   VARCHAR(200),
    contract_type   VARCHAR(50),
    contract_amount DECIMAL(12,2),
    start_date      TIMESTAMP,
    end_date        TIMESTAMP,
    status          VARCHAR(20),
    signatory       VARCHAR(100),
    sign_date       TIMESTAMP,
    attachment_url  VARCHAR(500),
    remark          VARCHAR(500),
    created_by      BIGINT,
    create_time     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    update_time     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  purchase_contract IS '采购合同主表';
COMMENT ON COLUMN purchase_contract.id IS '合同主键ID（自增）';
COMMENT ON COLUMN purchase_contract.contract_no IS '合同编号（如 PC20260629001）';
COMMENT ON COLUMN purchase_contract.contract_name IS '合同名称';
COMMENT ON COLUMN purchase_contract.supplier_id IS '供应商ID';
COMMENT ON COLUMN purchase_contract.supplier_name IS '供应商名称（冗余字段，前端展示用）';
COMMENT ON COLUMN purchase_contract.contract_type IS '合同类型';
COMMENT ON COLUMN purchase_contract.contract_amount IS '合同金额（单位：元，DECIMAL(12,2)）';
COMMENT ON COLUMN purchase_contract.start_date IS '合同开始日期';
COMMENT ON COLUMN purchase_contract.end_date IS '合同结束日期';
COMMENT ON COLUMN purchase_contract.status IS '状态：draft草稿 signed已签署 terminated已终止';
COMMENT ON COLUMN purchase_contract.signatory IS '签署人';
COMMENT ON COLUMN purchase_contract.sign_date IS '签署日期';
COMMENT ON COLUMN purchase_contract.attachment_url IS '附件URL';
COMMENT ON COLUMN purchase_contract.remark IS '备注';
COMMENT ON COLUMN purchase_contract.created_by IS '创建人ID';
COMMENT ON COLUMN purchase_contract.create_time IS '创建时间';
COMMENT ON COLUMN purchase_contract.updated_by IS '更新人ID';
COMMENT ON COLUMN purchase_contract.update_time IS '更新时间';
COMMENT ON COLUMN purchase_contract.deleted IS '逻辑删除：0未删除 1已删除';

-- 索引
CREATE INDEX IF NOT EXISTS idx_purchase_contract_supplier_id ON purchase_contract (supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_contract_status      ON purchase_contract (status);

-- 唯一索引（合同编号）
CREATE UNIQUE INDEX IF NOT EXISTS uk_purchase_contract_contract_no ON purchase_contract (contract_no);
