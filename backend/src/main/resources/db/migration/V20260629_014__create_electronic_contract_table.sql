-- ============================================================
-- 电子合同子模块建表脚本
-- 对应实体: ElectronicContract (electronic_contract)
-- 对应前端 API: /v1/purchase/electronic-contracts
--
-- 状态字段（VARCHAR(20)，字符串）：
--   status: draft=草稿 pending_sign=待签署 signed=已签署 expired=已过期 cancelled=已取消
--
-- 注意：H2 开发环境由 PurchaseDatabaseInitializer.createElectronicContractTable 建表，
--       本脚本仅在生产环境（PostgreSQL）执行。
-- ============================================================

-- 电子合同主表
CREATE TABLE IF NOT EXISTS electronic_contract (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contract_no     VARCHAR(50),
    contract_name   VARCHAR(200),
    supplier_id     BIGINT,
    supplier_name   VARCHAR(200),
    contract_type   VARCHAR(50),
    status          VARCHAR(20),
    sign_url        VARCHAR(500),
    view_url        VARCHAR(500),
    sign_date       TIMESTAMP,
    expire_date     TIMESTAMP,
    remark          VARCHAR(500),
    created_by      BIGINT,
    create_time     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    update_time     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  electronic_contract IS '电子合同主表';
COMMENT ON COLUMN electronic_contract.id IS '电子合同主键ID（自增）';
COMMENT ON COLUMN electronic_contract.contract_no IS '电子合同编号（如 EC20260629001）';
COMMENT ON COLUMN electronic_contract.contract_name IS '合同名称';
COMMENT ON COLUMN electronic_contract.supplier_id IS '供应商ID';
COMMENT ON COLUMN electronic_contract.supplier_name IS '供应商名称（冗余字段，前端展示用）';
COMMENT ON COLUMN electronic_contract.contract_type IS '合同类型';
COMMENT ON COLUMN electronic_contract.status IS '状态：draft草稿 pending_sign待签署 signed已签署 expired已过期 cancelled已取消';
COMMENT ON COLUMN electronic_contract.sign_url IS '签署链接';
COMMENT ON COLUMN electronic_contract.view_url IS '查看链接';
COMMENT ON COLUMN electronic_contract.sign_date IS '签署日期';
COMMENT ON COLUMN electronic_contract.expire_date IS '到期日期';
COMMENT ON COLUMN electronic_contract.remark IS '备注';
COMMENT ON COLUMN electronic_contract.created_by IS '创建人ID';
COMMENT ON COLUMN electronic_contract.create_time IS '创建时间';
COMMENT ON COLUMN electronic_contract.updated_by IS '更新人ID';
COMMENT ON COLUMN electronic_contract.update_time IS '更新时间';
COMMENT ON COLUMN electronic_contract.deleted IS '逻辑删除：0未删除 1已删除';

-- 索引
CREATE INDEX IF NOT EXISTS idx_electronic_contract_supplier_id ON electronic_contract (supplier_id);
CREATE INDEX IF NOT EXISTS idx_electronic_contract_status      ON electronic_contract (status);

-- 唯一索引（电子合同编号）
CREATE UNIQUE INDEX IF NOT EXISTS uk_electronic_contract_contract_no ON electronic_contract (contract_no);
