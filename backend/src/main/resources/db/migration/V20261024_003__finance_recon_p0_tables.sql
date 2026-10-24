-- ============================================================================
-- V20261024_003__finance_recon_p0_tables.sql
--
-- 财务对账（finance reconciliation）· P0 结构迁移：§5.1–§5.7 共 7 张表
--
-- 唯一权威定义：docs/design/finance-reconciliation-design.md §5.1–§5.7
--   §5.1 import_batches   导入批次
--   §5.2 import_rows      行级明细与回执
--   §5.3 bank_transactions 外部流水事实表（含 3 个唯一索引 + 3 个普通索引）
--   §5.4 recon_runs       对账批次
--   §5.5 recon_items      勾稽主体（含 matched_since）
--   §5.6 recon_differences 差异池
--   §5.7 deposit_slips    现金交款单
--   §5.10 两张调节表**不在本迁移范围**（待 TD-24 裁决后由后续版本落地）
--
-- 【金额单位硬规则】数据库一律「分」(BIGINT)，恒正数、禁止元混入；
--   元 → 分的换算只允许在 Java 侧一处实现（FinanceMoney#yuanToFen），本迁移只建列不造数。
--
-- 【通用列口径】按文档逐表落地：
--   * §5.1 / §5.3 / §5.7 文档明确列出 create_time/update_time/deleted/version → 四列齐全；
--   * §5.2 / §5.4 / §5.5 / §5.6 文档只列 create_time/update_time → 不擅自加 deleted/version
--     （P0 为只读查询；后续若需软删，另起迁移补列，保持「只增不改」）。
--
-- 幂等：全部 CREATE TABLE/INDEX IF NOT EXISTS；重复执行不报错、不覆盖已存在对象。
-- 失败信息可读：末尾 DO 块逐项自检（7 张表 + 3 个唯一索引），缺一即 RAISE EXCEPTION 说明缺什么。
--
-- 回退：DROP TABLE 七张表（逆序）；P0 无数据写入，回退无数据损失。
-- ============================================================================

-- ---------------------------------------------------------------------------
-- §5.1 import_batches（导入批次）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS import_batches (
    batch_id          BIGSERIAL     PRIMARY KEY,
    batch_no          VARCHAR(32)   NOT NULL,
    source_type       VARCHAR(16)   NOT NULL,
    account_id        BIGINT        NOT NULL,
    file_name         VARCHAR(255)  NOT NULL,
    file_sha256       VARCHAR(64)   NOT NULL,
    encoding_used     VARCHAR(16),
    period_start      DATE,
    period_end        DATE,
    total_rows        INTEGER       NOT NULL DEFAULT 0,
    ok_rows           INTEGER       NOT NULL DEFAULT 0,
    dup_rows          INTEGER       NOT NULL DEFAULT 0,
    error_rows        INTEGER       NOT NULL DEFAULT 0,
    status            VARCHAR(16)   NOT NULL DEFAULT 'UPLOADED',
    error_summary     TEXT,
    create_user_id    BIGINT        NOT NULL,
    created_by        VARCHAR(64)   NOT NULL,
    create_time       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           INTEGER       NOT NULL DEFAULT 0,
    version           INTEGER       NOT NULL DEFAULT 0,
    CONSTRAINT uk_import_batches_batch_no UNIQUE (batch_no)
);

COMMENT ON TABLE import_batches IS '对账-P0：导入批次（§5.1）；ok_rows+dup_rows+error_rows = total_rows';
COMMENT ON COLUMN import_batches.account_id IS '账户ID → bank_accounts.account_id（导入必须指定账户）';
COMMENT ON COLUMN import_batches.file_sha256 IS '原始文件 SHA-256（重复上传提示用）';
COMMENT ON COLUMN import_batches.status IS 'UPLOADED/PARSED_OK/PARSED_FAILED/COMMITTED/PARTIAL/FAILED/CANCELLED';

CREATE INDEX IF NOT EXISTS idx_import_batches_account ON import_batches (account_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_import_batches_status ON import_batches (status);

-- ---------------------------------------------------------------------------
-- §5.2 import_rows（行级明细与回执）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS import_rows (
    row_id                  BIGSERIAL     PRIMARY KEY,
    batch_id                BIGINT        NOT NULL,
    row_no                  INTEGER       NOT NULL,
    raw_json                JSONB,
    status                  VARCHAR(16)   NOT NULL,
    error_code              VARCHAR(32),
    error_message           VARCHAR(255),
    duplicate_of_row_no     INTEGER,
    matched_transaction_id  BIGINT,
    transaction_id          BIGINT,
    create_time             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE import_rows IS '对账-P0：导入行级明细（§5.2）；raw_json 为脱敏后的原始行（TD-18）';
COMMENT ON COLUMN import_rows.row_no IS '文件内行号（含偏移，便于对照原文）';
COMMENT ON COLUMN import_rows.status IS 'OK/DUPLICATE/ERROR/NEEDS_REVIEW';
COMMENT ON COLUMN import_rows.matched_transaction_id IS '与库内既有流水重复时指向 bank_transactions.transaction_id';

CREATE INDEX IF NOT EXISTS idx_import_rows_batch ON import_rows (batch_id, row_no);
CREATE INDEX IF NOT EXISTS idx_import_rows_status ON import_rows (status);
CREATE INDEX IF NOT EXISTS idx_import_rows_txn ON import_rows (transaction_id);

-- ---------------------------------------------------------------------------
-- §5.3 bank_transactions（外部流水事实表 —— 权威定义）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bank_transactions (
    transaction_id        BIGSERIAL     PRIMARY KEY,
    batch_id              BIGINT        NOT NULL,
    account_id            BIGINT        NOT NULL,
    source_type           VARCHAR(16)   NOT NULL,
    external_txn_id       VARCHAR(64),
    merchant_order_no     VARCHAR(64),
    direction             SMALLINT      NOT NULL,
    is_refund             BOOLEAN       NOT NULL DEFAULT FALSE,
    amount                BIGINT        NOT NULL,
    fee                   BIGINT,
    txn_time              TIMESTAMP     NOT NULL,
    business_date         DATE          NOT NULL,
    balance_snapshot      BIGINT,
    counterparty_name     VARCHAR(100),
    counterparty_account  VARCHAR(64),
    summary               VARCHAR(255),
    status_raw            VARCHAR(64),
    settle_status         VARCHAR(16),
    dedupe_basis          VARCHAR(16)   NOT NULL,
    raw_hash              VARCHAR(64)   NOT NULL,
    match_status          VARCHAR(16)   NOT NULL DEFAULT 'UNMATCHED',
    matched_flow_id       BIGINT,
    needs_review          BOOLEAN       NOT NULL DEFAULT FALSE,
    create_user_id        BIGINT        NOT NULL,
    create_time           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               INTEGER       NOT NULL DEFAULT 0,
    version               INTEGER       NOT NULL DEFAULT 0
);

COMMENT ON TABLE bank_transactions IS '对账-P0：外部流水事实表（§5.3 权威定义）；按账户+业务日勾稽';
COMMENT ON COLUMN bank_transactions.direction IS '1 收入 / 2 支出（与 fund_flows.flow_direction 同口径）';
COMMENT ON COLUMN bank_transactions.amount IS '金额（分，恒正数；方向由 direction 表达，禁止元混入）';
COMMENT ON COLUMN bank_transactions.fee IS '渠道手续费（分，独立字段，不并入 amount —— 裁决 6）';
COMMENT ON COLUMN bank_transactions.balance_snapshot IS '银行回单余额（分，可空，用于连续性校验 V4）';
COMMENT ON COLUMN bank_transactions.dedupe_basis IS 'EXTERNAL_ID / COMPOSITE（TD-12）';
COMMENT ON COLUMN bank_transactions.match_status IS 'UNMATCHED/MATCHED/AUTO_MATCHED/MANUAL_MATCHED/EXCEPTION（与 recon_items.subject_status 同枚举）';
COMMENT ON COLUMN bank_transactions.matched_flow_id IS '勾稽到的 fund_flows.flow_id（逻辑引用，不建外键）';

-- 主幂等键（方案 C，TD-12）：同账户同来源同渠道号唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_bank_txn_external
    ON bank_transactions (account_id, source_type, external_txn_id)
    WHERE external_txn_id IS NOT NULL AND deleted = 0;

-- 兜底幂等：无渠道号时按「账户+时间+金额+对方账号」判重
CREATE UNIQUE INDEX IF NOT EXISTS uk_bank_txn_composite
    ON bank_transactions (account_id, txn_time, amount, counterparty_account)
    WHERE dedupe_basis = 'COMPOSITE' AND deleted = 0;

-- 勾稽与列表
CREATE INDEX IF NOT EXISTS idx_bank_txn_account_date ON bank_transactions (account_id, business_date);
CREATE INDEX IF NOT EXISTS idx_bank_txn_match_status ON bank_transactions (match_status);
CREATE INDEX IF NOT EXISTS idx_bank_txn_batch        ON bank_transactions (batch_id);

-- ---------------------------------------------------------------------------
-- §5.4 recon_runs（对账批次）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recon_runs (
    run_id            BIGSERIAL   PRIMARY KEY,
    run_key           VARCHAR(64) NOT NULL,
    account_id        BIGINT      NOT NULL,
    store_id          BIGINT,
    business_date     DATE,
    status            VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    internal_count    INTEGER     NOT NULL DEFAULT 0,
    internal_amount   BIGINT      NOT NULL DEFAULT 0,
    external_count    INTEGER     NOT NULL DEFAULT 0,
    external_amount   BIGINT      NOT NULL DEFAULT 0,
    matched_count     INTEGER     NOT NULL DEFAULT 0,
    matched_amount    BIGINT      NOT NULL DEFAULT 0,
    difference_count  INTEGER     NOT NULL DEFAULT 0,
    rebuild_of        BIGINT,
    started_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at       TIMESTAMP,
    operator_id       BIGINT,
    create_time       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_recon_runs_run_key UNIQUE (run_key)
);

COMMENT ON TABLE recon_runs IS '对账-P0：对账批次（§5.4）；run_key = RECON-<account_id>-<yyyyMMdd>';
COMMENT ON COLUMN recon_runs.internal_amount IS '内部侧合计（分）';
COMMENT ON COLUMN recon_runs.external_amount IS '外部侧合计（分）';
COMMENT ON COLUMN recon_runs.matched_amount IS '已勾稽合计（分；仅 AUTO + MANUAL —— 裁决 7）';
COMMENT ON COLUMN recon_runs.operator_id IS '触发人；定时任务为 0';

CREATE INDEX IF NOT EXISTS idx_recon_runs_account_date ON recon_runs (account_id, business_date);

-- ---------------------------------------------------------------------------
-- §5.5 recon_items（勾稽主体）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recon_items (
    item_id                  BIGSERIAL     PRIMARY KEY,
    run_id                   BIGINT        NOT NULL,
    side                     VARCHAR(8)    NOT NULL,
    internal_ref             VARCHAR(64),
    external_ref             VARCHAR(64),
    payment_methods          VARCHAR(16),
    amount                   BIGINT        NOT NULL,
    txn_time                 TIMESTAMP,
    counterparty_name        VARCHAR(100),
    subject_status           VARCHAR(16)   NOT NULL,
    matched_since            TIMESTAMP,
    match_level              VARCHAR(8),
    match_group              VARCHAR(64),
    suggested_external_ref   VARCHAR(64),
    confidence               NUMERIC(4,3),
    create_time              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE recon_items IS '对账-P0：勾稽主体（§5.5）；同一主体在 (run_id, side, 内部号, 外部号) 上唯一';
COMMENT ON COLUMN recon_items.side IS 'INTERNAL / EXTERNAL';
COMMENT ON COLUMN recon_items.internal_ref IS '内部侧 flow_no（SALE 流水号）';
COMMENT ON COLUMN recon_items.external_ref IS '外部侧 external_txn_id 或 bank_transactions.transaction_id';
COMMENT ON COLUMN recon_items.amount IS '金额（分）';
COMMENT ON COLUMN recon_items.subject_status IS '五态（设计 §2.3）';
COMMENT ON COLUMN recon_items.matched_since IS '进入 MATCHED（待人工确认）的时刻；NULL 表示非 MATCHED（AC-42 超期统计）';

-- 防同一主体重复落 item（expr index：NULL 归一为空串）
CREATE UNIQUE INDEX IF NOT EXISTS uk_recon_items_subject
    ON recon_items (run_id, side, COALESCE(internal_ref, ''), COALESCE(external_ref, ''));

CREATE INDEX IF NOT EXISTS idx_recon_items_run_status ON recon_items (run_id, subject_status);
CREATE INDEX IF NOT EXISTS idx_recon_items_matched_since ON recon_items (matched_since);

-- ---------------------------------------------------------------------------
-- §5.6 recon_differences（差异池）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recon_differences (
    difference_id       BIGSERIAL     PRIMARY KEY,
    run_id              BIGINT        NOT NULL,
    difference_type     VARCHAR(24)   NOT NULL,
    internal_ref        VARCHAR(64),
    external_ref        VARCHAR(64),
    amount_internal     BIGINT,
    amount_external     BIGINT,
    diff_amount         BIGINT,
    status              VARCHAR(16)   NOT NULL DEFAULT 'OPEN',
    resolution          VARCHAR(16),
    resolution_remark   VARCHAR(500),
    reverse_flow_no     VARCHAR(32),
    assignee_id         BIGINT,
    assigned_at         TIMESTAMP,
    resolution_by       BIGINT,
    resolution_time     TIMESTAMP,
    reopen_of           BIGINT,
    create_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE recon_differences IS '对账-P0：差异池（§5.6）；七类差异枚举见设计 §2.4';
COMMENT ON COLUMN recon_differences.amount_internal IS '内部金额（分）';
COMMENT ON COLUMN recon_differences.amount_external IS '外部金额（分）';
COMMENT ON COLUMN recon_differences.diff_amount IS '差额（分）= amount_external − amount_internal';
COMMENT ON COLUMN recon_differences.reverse_flow_no IS '冲正产生的反向流水号 → fund_flows.flow_no（P1 才写）';

CREATE UNIQUE INDEX IF NOT EXISTS uk_recon_diff_subject
    ON recon_differences (run_id, difference_type, internal_ref, external_ref);
CREATE INDEX IF NOT EXISTS idx_recon_diff_status_assignee ON recon_differences (status, assignee_id);

-- ---------------------------------------------------------------------------
-- §5.7 deposit_slips（现金交款单）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS deposit_slips (
    slip_id                BIGSERIAL     PRIMARY KEY,
    slip_no                VARCHAR(32)   NOT NULL,
    store_id               BIGINT        NOT NULL,
    shift_id               BIGINT,
    cashier_employee_id    BIGINT,
    cashier_name           VARCHAR(50),
    collector_user_id      BIGINT,
    collector_name         VARCHAR(50),
    expected_amount        BIGINT        NOT NULL DEFAULT 0,
    actual_amount          BIGINT        NOT NULL DEFAULT 0,
    difference_amount      BIGINT        NOT NULL DEFAULT 0,
    cash_account_id        BIGINT,
    bank_account_id        BIGINT,
    deposit_bank_txn_id    BIGINT,
    deposit_ref_no         VARCHAR(64),
    status                 VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
    submitted_at           TIMESTAMP,
    received_at            TIMESTAMP,
    deposited_at           TIMESTAMP,
    verified_at            TIMESTAMP,
    closed_at              TIMESTAMP,
    reopen_of              BIGINT,
    remark                 VARCHAR(500),
    create_user_id         BIGINT        NOT NULL,
    created_by             VARCHAR(64)   NOT NULL,
    create_time            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                INTEGER       NOT NULL DEFAULT 0,
    deleted                INTEGER       NOT NULL DEFAULT 0,
    CONSTRAINT uk_deposit_slips_slip_no UNIQUE (slip_no),
    CONSTRAINT uk_deposit_slips_shift   UNIQUE (shift_id)
);

COMMENT ON TABLE deposit_slips IS '对账-P0：现金交款单（§5.7）；一班次一单（TD-33）';
COMMENT ON COLUMN deposit_slips.expected_amount IS '应交金额（分；§3.2 重算权威，P1 才写）';
COMMENT ON COLUMN deposit_slips.actual_amount IS '实交金额（分，点钞）';
COMMENT ON COLUMN deposit_slips.difference_amount IS '差额（分）= actual − expected（>0 长款，<0 短款）';
COMMENT ON COLUMN deposit_slips.deposit_bank_txn_id IS '存行后的 bank_transactions.transaction_id（P1 回填）';

CREATE INDEX IF NOT EXISTS idx_deposit_slips_store_date ON deposit_slips (store_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_deposit_slips_status ON deposit_slips (status);

-- ---------------------------------------------------------------------------
-- 迁移自检：7 张表 + bank_transactions 的 3 个关键索引，缺一即失败并说明
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_missing text := '';
    v_tbl     text;
    v_idx     text;
BEGIN
    FOREACH v_tbl IN ARRAY ARRAY[
        'import_batches', 'import_rows', 'bank_transactions', 'recon_runs',
        'recon_items', 'recon_differences', 'deposit_slips'
    ] LOOP
        IF to_regclass(v_tbl) IS NULL THEN
            v_missing := v_missing || v_tbl || '(表) ';
        END IF;
    END LOOP;

    FOREACH v_idx IN ARRAY ARRAY[
        'uk_bank_txn_external', 'uk_bank_txn_composite',
        'idx_bank_txn_account_date', 'idx_bank_txn_match_status', 'idx_bank_txn_batch'
    ] LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = current_schema() AND indexname = v_idx) THEN
            v_missing := v_missing || v_idx || '(索引) ';
        END IF;
    END LOOP;

    IF v_missing <> '' THEN
        RAISE EXCEPTION USING
            MESSAGE = 'P0 财务对账结构自检失败，缺少：' || v_missing,
            HINT = '请检查本迁移是否被部分执行（手工 psql 请用 -1 单事务执行），或库里已有同名对象类型冲突。';
    END IF;

    RAISE NOTICE 'P0 财务对账结构就绪：7 张表 + bank_transactions 5 个索引已存在。';
END
$$;
