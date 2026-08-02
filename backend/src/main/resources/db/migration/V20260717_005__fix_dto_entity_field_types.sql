-- ============================================================
-- V20260717_005: 修复 DTO/Entity 字段类型不符合规范问题
-- ============================================================
-- 背景：
--   - 项目规范要求：金额字段统一使用 BIGINT（分），状态字段统一使用 INTEGER（1/0/2）
--   - purchase_request.total_amount 原为 DECIMAL(12,2)（元），需改为 BIGINT（分）
--   - employees.status 原为 VARCHAR(20)（'active'/'inactive'/'probation'），需改为 INTEGER（1/0/2）
--   - users.status 原为 VARCHAR(20)（'1'/'0'），需改为 INTEGER（1/0）
--   - finance_voucher.debit_amount/credit_amount 原为 DECIMAL(15,2)（元），需改为 BIGINT（分）
--   - finance_voucher_detail.debit_amount/credit_amount 原为 DECIMAL(15,2)（元），需改为 BIGINT（分）
--
-- 处理策略（非破坏性 + 幂等保护）：
--   1. 使用 ALTER TABLE ... ALTER COLUMN ... TYPE ... USING 进行类型转换与数据迁移
--   2. 金额字段：DECIMAL 元 × 100 → BIGINT 分（使用 ROUND 避免精度丢失）
--   3. employees.status 字符串 → INTEGER：active→1, inactive→0, probation→2
--   4. users.status 字符串 → INTEGER：'1'→1, '0'→0（直接 CAST）
--   5. 同步更新列的 DEFAULT 值
--   6. 本脚本仅在 PostgreSQL 生产环境执行。
--
-- 幂等性保护（DF-039 修复）：
--   每个 ALTER 操作均用 DO 块包裹，先通过 information_schema.columns 检查列类型
--   是否已经是目标类型，如果是则跳过。这样即使脚本被二次执行（如 flyway repair 后），
--   也不会对已经是 BIGINT（分）的数据再次 ×100 导致数据放大 100 倍。
--   COMMENT ON COLUMN 语句本身幂等，无需包裹。
-- ============================================================

-- 1. purchase_request.total_amount: DECIMAL(12,2) → BIGINT（元转分）
DO $$
BEGIN
    -- 只有当列类型不是 BIGINT 时才执行转换（幂等保护，避免二次 ×100 放大）
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'purchase_request'
        AND column_name = 'total_amount'
        AND data_type != 'bigint'
    ) THEN
        ALTER TABLE purchase_request
            ALTER COLUMN total_amount TYPE BIGINT
            USING ROUND(total_amount * 100);
        ALTER TABLE purchase_request
            ALTER COLUMN total_amount SET DEFAULT 0;
        RAISE NOTICE '已转换 purchase_request.total_amount 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 purchase_request.total_amount：已是 BIGINT';
    END IF;
END $$;

COMMENT ON COLUMN purchase_request.total_amount IS '总金额（单位：分，1元=100分）';

-- 2. employees.status: VARCHAR(20) → INTEGER（语义字符串转数字编码）
--    映射：active→1（在职）, inactive→0（离职）, probation→2（试用期）
DO $$
BEGIN
    -- 只有当列类型不是 INTEGER 时才执行转换（幂等保护）
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees'
        AND column_name = 'status'
        AND data_type != 'integer'
    ) THEN
        -- 必须先 DROP DEFAULT，否则旧默认值（'active'）无法隐式转换为 INTEGER
        ALTER TABLE employees ALTER COLUMN status DROP DEFAULT;
        ALTER TABLE employees
            ALTER COLUMN status TYPE INTEGER
            USING CASE
                WHEN status = 'active' THEN 1
                WHEN status = 'inactive' THEN 0
                WHEN status = 'probation' THEN 2
                WHEN status IS NULL THEN 1
                ELSE 1
            END;
        ALTER TABLE employees
            ALTER COLUMN status SET DEFAULT 1;
        RAISE NOTICE '已转换 employees.status 为 INTEGER';
    ELSE
        RAISE NOTICE '跳过 employees.status：已是 INTEGER';
    END IF;
END $$;

COMMENT ON COLUMN employees.status IS '员工状态（1:在职, 0:离职, 2:试用期）';

-- 3. users.status: VARCHAR(20) → INTEGER（字符串数字转 INTEGER）
--    映射：'1'→1（正常）, '0'→0（禁用）
DO $$
BEGIN
    -- 只有当列类型不是 INTEGER 时才执行转换（幂等保护）
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users'
        AND column_name = 'status'
        AND data_type != 'integer'
    ) THEN
        -- 必须先 DROP DEFAULT，否则旧默认值（'1'::character varying）无法隐式转换为 INTEGER
        ALTER TABLE users ALTER COLUMN status DROP DEFAULT;
        ALTER TABLE users
            ALTER COLUMN status TYPE INTEGER
            USING CASE
                WHEN status = '1' THEN 1
                WHEN status = '0' THEN 0
                WHEN status IS NULL THEN 1
                ELSE 1
            END;
        ALTER TABLE users
            ALTER COLUMN status SET DEFAULT 1;
        RAISE NOTICE '已转换 users.status 为 INTEGER';
    ELSE
        RAISE NOTICE '跳过 users.status：已是 INTEGER';
    END IF;
END $$;

COMMENT ON COLUMN users.status IS '用户状态（1:正常, 0:禁用）';

-- 4. finance_voucher.debit_amount: DECIMAL(15,2) → BIGINT（元转分）
DO $$
BEGIN
    -- 只有当列类型不是 BIGINT 时才执行转换（幂等保护，避免二次 ×100 放大）
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'finance_voucher'
        AND column_name = 'debit_amount'
        AND data_type != 'bigint'
    ) THEN
        ALTER TABLE finance_voucher
            ALTER COLUMN debit_amount TYPE BIGINT
            USING ROUND(debit_amount * 100);
        ALTER TABLE finance_voucher
            ALTER COLUMN debit_amount SET DEFAULT 0;
        RAISE NOTICE '已转换 finance_voucher.debit_amount 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 finance_voucher.debit_amount：已是 BIGINT';
    END IF;
END $$;

COMMENT ON COLUMN finance_voucher.debit_amount IS '借方金额（单位：分，1元=100分）';

-- 5. finance_voucher.credit_amount: DECIMAL(15,2) → BIGINT（元转分）
DO $$
BEGIN
    -- 只有当列类型不是 BIGINT 时才执行转换（幂等保护，避免二次 ×100 放大）
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'finance_voucher'
        AND column_name = 'credit_amount'
        AND data_type != 'bigint'
    ) THEN
        ALTER TABLE finance_voucher
            ALTER COLUMN credit_amount TYPE BIGINT
            USING ROUND(credit_amount * 100);
        ALTER TABLE finance_voucher
            ALTER COLUMN credit_amount SET DEFAULT 0;
        RAISE NOTICE '已转换 finance_voucher.credit_amount 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 finance_voucher.credit_amount：已是 BIGINT';
    END IF;
END $$;

COMMENT ON COLUMN finance_voucher.credit_amount IS '贷方金额（单位：分，1元=100分）';

-- 6. finance_voucher_detail.debit_amount: DECIMAL(15,2) → BIGINT（元转分）
DO $$
BEGIN
    -- 只有当列类型不是 BIGINT 时才执行转换（幂等保护，避免二次 ×100 放大）
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'finance_voucher_detail'
        AND column_name = 'debit_amount'
        AND data_type != 'bigint'
    ) THEN
        ALTER TABLE finance_voucher_detail
            ALTER COLUMN debit_amount TYPE BIGINT
            USING ROUND(debit_amount * 100);
        ALTER TABLE finance_voucher_detail
            ALTER COLUMN debit_amount SET DEFAULT 0;
        RAISE NOTICE '已转换 finance_voucher_detail.debit_amount 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 finance_voucher_detail.debit_amount：已是 BIGINT';
    END IF;
END $$;

COMMENT ON COLUMN finance_voucher_detail.debit_amount IS '借方金额（单位：分，1元=100分）';

-- 7. finance_voucher_detail.credit_amount: DECIMAL(15,2) → BIGINT（元转分）
DO $$
BEGIN
    -- 只有当列类型不是 BIGINT 时才执行转换（幂等保护，避免二次 ×100 放大）
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'finance_voucher_detail'
        AND column_name = 'credit_amount'
        AND data_type != 'bigint'
    ) THEN
        ALTER TABLE finance_voucher_detail
            ALTER COLUMN credit_amount TYPE BIGINT
            USING ROUND(credit_amount * 100);
        ALTER TABLE finance_voucher_detail
            ALTER COLUMN credit_amount SET DEFAULT 0;
        RAISE NOTICE '已转换 finance_voucher_detail.credit_amount 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 finance_voucher_detail.credit_amount：已是 BIGINT';
    END IF;
END $$;

COMMENT ON COLUMN finance_voucher_detail.credit_amount IS '贷方金额（单位：分，1元=100分）';
