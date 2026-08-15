-- ============================================================
-- Sprint-4 Release Execution | P0-FIN-002 缴税幂等（PD-001）
-- 创建 tax_record 表（原表在运行库中不存在，旧代码 createTaxRecord 直接 save 必失败）
-- 依据 PD-001 决策：幂等键 = 税种(tax_type) + 所属期(tax_period) + 缴税凭证号(voucher_no)
--   - voucher_no：PD-001 幂等键组成部分，原 schema 无凭证号字段，按决策落地字段
--   - deleted：幂等查询要求 deleted=0（与 training_study_record/account_balance 约定一致，默认 0）
--   - uk_tax_record_idempotent：DB 级幂等兜底（同键不新增；voucher_no 为 NULL 时 PG 允许重复，幂等不适用）
-- ============================================================
CREATE TABLE IF NOT EXISTS tax_record (
    id            BIGSERIAL PRIMARY KEY,
    tax_type      VARCHAR(50)  NOT NULL,
    tax_period    VARCHAR(20)  NOT NULL,
    taxable_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
    tax_rate      NUMERIC(5,2)  NOT NULL DEFAULT 0,
    tax_amount    NUMERIC(10,2) NOT NULL DEFAULT 0,
    paid_amount   NUMERIC(10,2) NOT NULL DEFAULT 0,
    tax_status    VARCHAR(20)  NOT NULL DEFAULT 'UNPAID',
    payment_date  TIMESTAMP,
    voucher_no    VARCHAR(64),
    description   VARCHAR(255),
    create_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by     VARCHAR(50),
    update_by     VARCHAR(50),
    deleted       SMALLINT NOT NULL DEFAULT 0
);

-- 幂等唯一约束：税种 + 所属期 + 缴税凭证号（PD-001 幂等键）
CREATE UNIQUE INDEX IF NOT EXISTS uk_tax_record_idempotent
    ON tax_record (tax_type, tax_period, voucher_no);

CREATE INDEX IF NOT EXISTS idx_tax_record_tax_type   ON tax_record (tax_type);
CREATE INDEX IF NOT EXISTS idx_tax_record_tax_period ON tax_record (tax_period);
CREATE INDEX IF NOT EXISTS idx_tax_record_tax_status ON tax_record (tax_status);
