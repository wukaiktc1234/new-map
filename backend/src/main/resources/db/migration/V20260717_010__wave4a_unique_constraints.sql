-- ============================================================
-- Wave 4-A：唯一约束补齐（DM-026）
-- 关联问题：数据模型审查报告 §4.10 唯一约束缺失或不合理
-- 修复内容：
--   1. 补齐业务键的唯一约束（部分唯一索引 WHERE deleted=0）
--   2. 对已存在全表 UNIQUE 的字段（employees.employee_code、users.username），
--      保留全表 UNIQUE（比"按未删除范围"更严格，已满足要求），仅追加注释说明
--   3. 对供应商营业执照号/税号、采购入库单号、员工手机号/邮箱/身份证号、
--      用户手机号/邮箱等关键字段补齐部分唯一索引
-- 设计原则：
--   - 使用 CREATE UNIQUE INDEX IF NOT EXISTS 保证幂等
--   - 使用 WHERE deleted=0 部分索引，允许已删除记录复用业务键
--   - 使用 DO 块预检重复数据，重复时跳过并 RAISE NOTICE（不静默清理）
--   - 所有 NULL 值不参与唯一性约束（PostgreSQL 标准行为）
-- 注意：
--   - food_trace_codes.trace_code 已有 inline UNIQUE（V11.0.0）
--   - voucher_header.voucher_no 已有 inline UNIQUE + 部分唯一索引（V20260717_006）
--   - members.member_no 已有 inline UNIQUE + 部分唯一索引 uk_members_member_no（V8.0.0）
--   - product_batches 表在代码库中不存在，跳过
-- ============================================================

-- ------------------------------------------------------
-- 1. suppliers.license_no 部分唯一索引（营业执照号）
-- ------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'suppliers' AND column_name = 'license_no'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE tablename = 'suppliers' AND indexname = 'uk_suppliers_license_no'
        ) THEN
            IF NOT EXISTS (
                SELECT 1 FROM suppliers
                WHERE deleted = 0 AND license_no IS NOT NULL AND license_no != ''
                GROUP BY license_no HAVING COUNT(*) > 1
            ) THEN
                CREATE UNIQUE INDEX uk_suppliers_license_no
                    ON suppliers(license_no)
                    WHERE deleted = 0 AND license_no IS NOT NULL AND license_no != '';
                RAISE NOTICE '已创建 uk_suppliers_license_no';
            ELSE
                RAISE NOTICE '跳过 uk_suppliers_license_no：suppliers 表存在重复的 license_no（deleted=0），需先清理数据';
            END IF;
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 2. suppliers.tax_no 部分唯一索引（税号）
-- ------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'suppliers' AND column_name = 'tax_no'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE tablename = 'suppliers' AND indexname = 'uk_suppliers_tax_no'
        ) THEN
            IF NOT EXISTS (
                SELECT 1 FROM suppliers
                WHERE deleted = 0 AND tax_no IS NOT NULL AND tax_no != ''
                GROUP BY tax_no HAVING COUNT(*) > 1
            ) THEN
                CREATE UNIQUE INDEX uk_suppliers_tax_no
                    ON suppliers(tax_no)
                    WHERE deleted = 0 AND tax_no IS NOT NULL AND tax_no != '';
                RAISE NOTICE '已创建 uk_suppliers_tax_no';
            ELSE
                RAISE NOTICE '跳过 uk_suppliers_tax_no：suppliers 表存在重复的 tax_no（deleted=0），需先清理数据';
            END IF;
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 3. purchase_stockins.stockin_code 部分唯一索引（入库单号）
-- ------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'purchase_stockins' AND column_name = 'stockin_code'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE tablename = 'purchase_stockins' AND indexname = 'uk_purchase_stockins_stockin_code'
        ) THEN
            IF NOT EXISTS (
                SELECT 1 FROM purchase_stockins
                WHERE deleted = 0 AND stockin_code IS NOT NULL AND stockin_code != ''
                GROUP BY stockin_code HAVING COUNT(*) > 1
            ) THEN
                CREATE UNIQUE INDEX uk_purchase_stockins_stockin_code
                    ON purchase_stockins(stockin_code)
                    WHERE deleted = 0 AND stockin_code IS NOT NULL AND stockin_code != '';
                RAISE NOTICE '已创建 uk_purchase_stockins_stockin_code';
            ELSE
                RAISE NOTICE '跳过 uk_purchase_stockins_stockin_code：purchase_stockins 表存在重复的 stockin_code（deleted=0），需先清理数据';
            END IF;
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 4. employees.phone 部分唯一索引（员工手机号）
-- ------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'employees' AND indexname = 'uk_employees_phone'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM employees
            WHERE deleted = 0 AND phone IS NOT NULL AND phone != ''
            GROUP BY phone HAVING COUNT(*) > 1
        ) THEN
            CREATE UNIQUE INDEX uk_employees_phone
                ON employees(phone)
                WHERE deleted = 0 AND phone IS NOT NULL AND phone != '';
            RAISE NOTICE '已创建 uk_employees_phone';
        ELSE
            RAISE NOTICE '跳过 uk_employees_phone：employees 表存在重复的 phone（deleted=0），需先清理数据';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 5. employees.email 部分唯一索引（员工邮箱）
-- ------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'employees' AND indexname = 'uk_employees_email'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM employees
            WHERE deleted = 0 AND email IS NOT NULL AND email != ''
            GROUP BY email HAVING COUNT(*) > 1
        ) THEN
            CREATE UNIQUE INDEX uk_employees_email
                ON employees(email)
                WHERE deleted = 0 AND email IS NOT NULL AND email != '';
            RAISE NOTICE '已创建 uk_employees_email';
        ELSE
            RAISE NOTICE '跳过 uk_employees_email：employees 表存在重复的 email（deleted=0），需先清理数据';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 6. employees.id_card 部分唯一索引（员工身份证号）
-- ------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'employees' AND column_name = 'id_card'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE tablename = 'employees' AND indexname = 'uk_employees_id_card'
        ) THEN
            IF NOT EXISTS (
                SELECT 1 FROM employees
                WHERE deleted = 0 AND id_card IS NOT NULL AND id_card != ''
                GROUP BY id_card HAVING COUNT(*) > 1
            ) THEN
                CREATE UNIQUE INDEX uk_employees_id_card
                    ON employees(id_card)
                    WHERE deleted = 0 AND id_card IS NOT NULL AND id_card != '';
                RAISE NOTICE '已创建 uk_employees_id_card';
            ELSE
                RAISE NOTICE '跳过 uk_employees_id_card：employees 表存在重复的 id_card（deleted=0），需先清理数据';
            END IF;
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 7. users.phone 部分唯一索引（用户手机号）
-- ------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'users' AND indexname = 'uk_users_phone'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM users
            WHERE deleted = 0 AND phone IS NOT NULL AND phone != ''
            GROUP BY phone HAVING COUNT(*) > 1
        ) THEN
            CREATE UNIQUE INDEX uk_users_phone
                ON users(phone)
                WHERE deleted = 0 AND phone IS NOT NULL AND phone != '';
            RAISE NOTICE '已创建 uk_users_phone';
        ELSE
            RAISE NOTICE '跳过 uk_users_phone：users 表存在重复的 phone（deleted=0），需先清理数据';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 8. users.email 部分唯一索引（用户邮箱）
-- ------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'users' AND indexname = 'uk_users_email'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM users
            WHERE deleted = 0 AND email IS NOT NULL AND email != ''
            GROUP BY email HAVING COUNT(*) > 1
        ) THEN
            CREATE UNIQUE INDEX uk_users_email
                ON users(email)
                WHERE deleted = 0 AND email IS NOT NULL AND email != '';
            RAISE NOTICE '已创建 uk_users_email';
        ELSE
            RAISE NOTICE '跳过 uk_users_email：users 表存在重复的 email（deleted=0），需先清理数据';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 9. 说明：已存在的 UNIQUE 约束（无需修改）
-- ------------------------------------------------------
-- 以下字段已有 UNIQUE 约束，满足"必须唯一"要求：
--
-- food_trace_codes.trace_code：
--   V11.0.0 line 10 inline UNIQUE NOT NULL
--
-- food_trace_code.trace_code（单数表）：
--   V1.0.0.100 line 714 CONSTRAINT uk_food_trace_code_trace_code UNIQUE
--
-- voucher_header.voucher_no：
--   V20260405 line 392 inline UNIQUE NOT NULL
--   V20260717_006 line 54 部分唯一索引 uk_voucher_header_voucher_no WHERE deleted=0
--
-- employees.employee_code：
--   V1.0.0.100 line 144 CONSTRAINT uk_employees_employee_code UNIQUE
--   说明：全表 UNIQUE 比"按未删除范围"更严格，已满足要求
--   如需允许已删除记录复用 employee_code，需先 DROP CONSTRAINT 再创建部分索引
--   本次修复保留现有约束，避免破坏数据完整性
--
-- users.username：
--   V1.0.0.100 line 191 CONSTRAINT uk_users_username UNIQUE
--   说明：全表 UNIQUE 比"按未删除范围"更严格，已满足要求
--   同上，保留现有约束
--
-- members.member_no（任务写的 member_code 实为 member_no）：
--   V8.0.0 line 55 inline UNIQUE NOT NULL
--   V8.0.0 line 102 部分唯一索引 uk_members_member_no WHERE deleted=0
--
-- product_batches.batch_no：
--   该表在代码库中不存在，跳过
