-- ============================================================================
-- V20261024_004__finance_recon_p1_statements.sql
--
-- 财务对账 · P1：§5.10 调节表两表（银行存款余额调节表）
--   §5.10.1 reconciliation_statements（主表）
--   §5.10.2 reconciliation_items（未达账项明细）
-- 唯一权威定义：docs/design/finance-reconciliation-design.md §5.10.1 / §5.10.2
--
-- 【范围锁（P1）】
--   * 本迁移**只新增**这两张表；§5.1–§5.7 七张 P0 表不改结构/不加列/不改索引；
--   * 两表只承载「核对与披露」，**不承载任何调账动作**（§4.3 硬规则一）：
--     不写 fund_flows、不改 bank_accounts.balance、不冲正、不做缴款单/内部转账（P2 范围）；
--   * 差异处理 REVERSE 仅在 recon_differences 标记「待 P2 执行」，本迁移不涉及。
--
-- 【金额单位】一律 BIGINT「分」，恒正数；列注释逐列写明。
--
-- 【幂等】CREATE TABLE/INDEX IF NOT EXISTS + DO 块判存在；重复执行 0 变更。
-- 【回退】DROP TABLE reconciliation_items; DROP TABLE reconciliation_statements;
-- ============================================================================

-- ---------------------------------------------------------------------------
-- §5.10.1 reconciliation_statements（银行存款余额调节表主表）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reconciliation_statements (
    statement_id            BIGSERIAL     PRIMARY KEY,
    statement_no            VARCHAR(32)   NOT NULL,
    account_id              BIGINT        NOT NULL,
    store_id                BIGINT,
    period_start            DATE          NOT NULL,
    period_end              DATE          NOT NULL,
    book_balance            BIGINT        NOT NULL DEFAULT 0,
    bank_balance            BIGINT        NOT NULL DEFAULT 0,
    uncleared_in_credit     BIGINT        NOT NULL DEFAULT 0,
    uncleared_in_debit      BIGINT        NOT NULL DEFAULT 0,
    uncleared_out_credit    BIGINT        NOT NULL DEFAULT 0,
    uncleared_out_debit     BIGINT        NOT NULL DEFAULT 0,
    adjusted_book_balance   BIGINT        NOT NULL DEFAULT 0,
    adjusted_bank_balance   BIGINT        NOT NULL DEFAULT 0,
    is_balanced             BOOLEAN       NOT NULL DEFAULT FALSE,
    status                  VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    submitted_by            BIGINT,
    submitted_at            TIMESTAMP,
    approved_by             BIGINT,
    approved_at             TIMESTAMP,
    remark                  VARCHAR(500),
    create_user_id          BIGINT        NOT NULL,
    created_by              VARCHAR(64)   NOT NULL,
    create_time             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                 INTEGER       NOT NULL DEFAULT 0,
    deleted                 INTEGER       NOT NULL DEFAULT 0,
    CONSTRAINT uk_recon_statements_no UNIQUE (statement_no),
    CONSTRAINT uk_recon_statements_period UNIQUE (account_id, period_start, period_end),
    CONSTRAINT ck_recon_statements_status CHECK (status IN ('DRAFT','SUBMITTED','APPROVED'))
);

COMMENT ON TABLE reconciliation_statements IS '对账-P1：银行存款余额调节表（§5.10.1）；只承载核对与披露，不承载调账动作';
COMMENT ON COLUMN reconciliation_statements.statement_no IS '调节表编号 BS<yyyyMM><NNNN>，如 BS2026100001';
COMMENT ON COLUMN reconciliation_statements.account_id IS '账户ID → bank_accounts.account_id（一账户一张表）';
COMMENT ON COLUMN reconciliation_statements.store_id IS '账户归属门店；scope=GLOBAL 的总部账户为 NULL';
COMMENT ON COLUMN reconciliation_statements.book_balance IS '企业账面余额（分）：期间末 fund_flows.balance_after 快照（TD-23）';
COMMENT ON COLUMN reconciliation_statements.bank_balance IS '银行对账单余额（分）：bank_transactions.balance_snapshot 最新一笔';
COMMENT ON COLUMN reconciliation_statements.uncleared_in_credit IS '未达 A：企业已收、银行未收（分，合计）';
COMMENT ON COLUMN reconciliation_statements.uncleared_in_debit IS '未达 B：企业已付、银行未付（分，合计）';
COMMENT ON COLUMN reconciliation_statements.uncleared_out_credit IS '未达 C：银行已收、企业未收（分，合计）';
COMMENT ON COLUMN reconciliation_statements.uncleared_out_debit IS '未达 D：银行已付、企业未付（分，合计）';
COMMENT ON COLUMN reconciliation_statements.adjusted_book_balance IS '调节后企业余额（分）= book_balance + C − D';
COMMENT ON COLUMN reconciliation_statements.adjusted_bank_balance IS '调节后银行余额（分）= bank_balance + A − B';
COMMENT ON COLUMN reconciliation_statements.is_balanced IS '生成时计算：adjusted_book_balance = adjusted_bank_balance；false 时禁止 SUBMIT/APPROVED（§4.3 V5）';
COMMENT ON COLUMN reconciliation_statements.status IS 'DRAFT / SUBMITTED / APPROVED（APPROVED 后锁定，改动需重开新表）';
COMMENT ON COLUMN reconciliation_statements.approved_by IS '审核人；硬门禁 §4.3 V8：approved_by ≠ submitted_by 且不得持有 ROLE_TELLER';

CREATE INDEX IF NOT EXISTS idx_recon_statements_account_period
    ON reconciliation_statements (account_id, period_start);

-- ---------------------------------------------------------------------------
-- §5.10.2 reconciliation_items（未达账项明细）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reconciliation_items (
    item_id                 BIGSERIAL     PRIMARY KEY,
    statement_id            BIGINT        NOT NULL,
    category                VARCHAR(4)    NOT NULL,
    flow_id                 BIGINT,
    transaction_id          BIGINT,
    amount                  BIGINT        NOT NULL,
    business_date           DATE          NOT NULL,
    period_start            DATE,
    period_end              DATE,
    owner_user_id           BIGINT,
    owner_name              VARCHAR(50),
    reason                  VARCHAR(500)  NOT NULL,
    cleared_flag            BOOLEAN       NOT NULL DEFAULT FALSE,
    cleared_statement_id    BIGINT,
    create_time             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 INTEGER       NOT NULL DEFAULT 0
);

COMMENT ON TABLE reconciliation_items IS '对账-P1：未达账项明细（§5.10.2）；四类 A/B（内部侧）/ C/D（外部侧）';
COMMENT ON COLUMN reconciliation_items.category IS 'A 企业已收银行未收 / B 企业已付银行未付 / C 银行已收企业未收 / D 银行已付企业未付';
COMMENT ON COLUMN reconciliation_items.flow_id IS '内部侧引用 → fund_flows.flow_id（A/B 类必填；逻辑引用，不建外键）';
COMMENT ON COLUMN reconciliation_items.transaction_id IS '外部侧引用 → bank_transactions.transaction_id（C/D 类必填）';
COMMENT ON COLUMN reconciliation_items.amount IS '该笔未达金额（分，正数；方向由 category 决定）';
COMMENT ON COLUMN reconciliation_items.business_date IS '业务发生日（A/B 取 fund_flows.business_date；C/D 取 bank_transactions.txn_time 的日期）';
COMMENT ON COLUMN reconciliation_items.period_start IS '归属期间起（冗余自 statement，便于跨期追溯与索引）';
COMMENT ON COLUMN reconciliation_items.period_end IS '归属期间止（冗余自 statement）';
COMMENT ON COLUMN reconciliation_items.owner_user_id IS '挂账人（认领该未达项的责任人：出纳或主管）';
COMMENT ON COLUMN reconciliation_items.reason IS '未达原因（必填，如「月末在途」「银行已扣手续费未回单」）';
COMMENT ON COLUMN reconciliation_items.cleared_flag IS '是否已在下期核销（下期命中同一 flow_id/transaction_id 时置 true）';
COMMENT ON COLUMN reconciliation_items.cleared_statement_id IS '在下一期哪张调节表被核销（可空）';

-- 悬空未达项闸门：A/B 必须挂内部引用，C/D 必须挂外部引用
ALTER TABLE reconciliation_items DROP CONSTRAINT IF EXISTS ck_recon_items_ref_by_category;
ALTER TABLE reconciliation_items
    ADD CONSTRAINT ck_recon_items_ref_by_category
    CHECK (
        (category IN ('A','B') AND flow_id IS NOT NULL)
        OR (category IN ('C','D') AND transaction_id IS NOT NULL)
    );

-- V7 双保险的 DB 侧：同一张表内同一笔不重复挂账（NULL 归 0 后参与唯一）
CREATE UNIQUE INDEX IF NOT EXISTS uk_recon_items_statement_subject
    ON reconciliation_items (statement_id, category, COALESCE(flow_id, 0), COALESCE(transaction_id, 0));

-- 明细随主表删除（P1 唯一硬外键：statement_id）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname = 'fk_recon_items_statement'
           AND conrelid = 'reconciliation_items'::regclass
    ) THEN
        ALTER TABLE reconciliation_items
            ADD CONSTRAINT fk_recon_items_statement
            FOREIGN KEY (statement_id)
            REFERENCES reconciliation_statements (statement_id)
            ON DELETE CASCADE;
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_recon_items_statement    ON reconciliation_items (statement_id);
CREATE INDEX IF NOT EXISTS idx_recon_items_flow         ON reconciliation_items (flow_id);
CREATE INDEX IF NOT EXISTS idx_recon_items_transaction  ON reconciliation_items (transaction_id);

-- ---------------------------------------------------------------------------
-- 迁移自检：两表 + 3 个关键约束 + 5 个索引，缺一即失败并说明
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_missing text := '';
    v_tbl text;
    v_con text;
    v_idx text;
BEGIN
    FOREACH v_tbl IN ARRAY ARRAY['reconciliation_statements', 'reconciliation_items'] LOOP
        IF to_regclass(v_tbl) IS NULL THEN
            v_missing := v_missing || v_tbl || '(表) ';
        END IF;
    END LOOP;

    FOREACH v_con IN ARRAY ARRAY[
        'uk_recon_statements_period', 'ck_recon_statements_status',
        'ck_recon_items_ref_by_category', 'fk_recon_items_statement'
    ] LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = v_con) THEN
            v_missing := v_missing || v_con || '(约束) ';
        END IF;
    END LOOP;

    FOREACH v_idx IN ARRAY ARRAY[
        'idx_recon_statements_account_period', 'uk_recon_items_statement_subject',
        'idx_recon_items_statement', 'idx_recon_items_flow', 'idx_recon_items_transaction'
    ] LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = current_schema() AND indexname = v_idx) THEN
            v_missing := v_missing || v_idx || '(索引) ';
        END IF;
    END LOOP;

    IF v_missing <> '' THEN
        RAISE EXCEPTION USING
            MESSAGE = 'P1 调节表结构自检失败，缺少：' || v_missing,
            HINT = '请检查本迁移是否被部分执行；手工 psql 请用 -1 单事务执行。';
    END IF;

    RAISE NOTICE 'P1 调节表结构就绪：reconciliation_statements / reconciliation_items + 4 约束 + 5 索引。';
END
$$;
