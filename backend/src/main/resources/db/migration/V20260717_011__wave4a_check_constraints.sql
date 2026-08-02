-- ============================================================
-- Wave 4-A：CHECK 约束补齐（DM-031）
-- 关联问题：数据模型审查报告 §4.11 检查约束缺失、ERP 合规报告 §7.3
-- 修复内容：
--   1. 金额字段（BIGINT）>= 0
--   2. 数量字段（INTEGER）>= 0
--   3. 状态字段（INTEGER）值域约束
--   4. deleted 字段 IN (0,1)
--   5. version 字段 >= 0
--   6. email 字段格式校验（含 @）
--   7. phone 字段格式校验（中国手机号或空）
--   8. 关键时间逻辑约束（生产日期 < 过期日期等）
-- 设计原则：
--   - 使用 DO 块保证幂等（检查 information_schema.table_constraints）
--   - 添加前预检现有数据，违规时跳过并 RAISE NOTICE（不静默清理）
--   - 约束命名：chk_{table}_{field}_{语义}
--   - 仅对 BIGINT/INTEGER 类型字段添加金额/数量约束，DECIMAL 字段待类型统一后补充
-- 注意：
--   - DECIMAL 金额字段（如 purchase_orders.total_amount）暂不加 CHECK，
--     因为类型统一为 BIGINT 是另一个 Wave 的任务，提前加约束会与后续 ALTER TYPE 冲突
--   - VARCHAR 状态字段（如 employees.status='active'）暂不加 CHECK，
--     因为状态统一为 INTEGER 是另一个 Wave 的任务
-- ============================================================

-- ------------------------------------------------------
-- 1. 金额字段非负约束（BIGINT 类型）
-- ------------------------------------------------------

-- 1.1 voucher_header.total_debit >= 0, total_credit >= 0
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'voucher_header' AND constraint_name = 'chk_voucher_header_amounts_nonneg'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM voucher_header WHERE total_debit < 0 OR total_credit < 0) THEN
            ALTER TABLE voucher_header
                ADD CONSTRAINT chk_voucher_header_amounts_nonneg
                CHECK (total_debit >= 0 AND total_credit >= 0);
            RAISE NOTICE '已创建 chk_voucher_header_amounts_nonneg';
        ELSE
            RAISE NOTICE '跳过 chk_voucher_header_amounts_nonneg：voucher_header 存在负金额记录，需先清理';
        END IF;
    END IF;
END $$;

-- 1.2 members 余额/充值/消费金额 >= 0
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'members' AND constraint_name = 'chk_members_amounts_nonneg'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM members
            WHERE balance < 0 OR total_recharge < 0 OR total_consume < 0
        ) THEN
            ALTER TABLE members
                ADD CONSTRAINT chk_members_amounts_nonneg
                CHECK (balance >= 0 AND total_recharge >= 0 AND total_consume >= 0);
            RAISE NOTICE '已创建 chk_members_amounts_nonneg';
        ELSE
            RAISE NOTICE '跳过 chk_members_amounts_nonneg：members 存在负金额记录，需先清理';
        END IF;
    END IF;
END $$;

-- 1.3 members 积分非负
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'members' AND constraint_name = 'chk_members_points_nonneg'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM members
            WHERE points < 0 OR total_points_earned < 0 OR total_points_used < 0
        ) THEN
            ALTER TABLE members
                ADD CONSTRAINT chk_members_points_nonneg
                CHECK (points >= 0 AND total_points_earned >= 0 AND total_points_used >= 0);
            RAISE NOTICE '已创建 chk_members_points_nonneg';
        ELSE
            RAISE NOTICE '跳过 chk_members_points_nonneg：members 存在负积分记录，需先清理';
        END IF;
    END IF;
END $$;

-- 1.4 purchase_stockin_items.unit_price >= 0
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'purchase_stockin_items' AND column_name = 'unit_price'
        AND data_type = 'bigint'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE table_name = 'purchase_stockin_items' AND constraint_name = 'chk_psi_unit_price_nonneg'
        ) THEN
            IF NOT EXISTS (SELECT 1 FROM purchase_stockin_items WHERE unit_price < 0) THEN
                ALTER TABLE purchase_stockin_items
                    ADD CONSTRAINT chk_psi_unit_price_nonneg
                    CHECK (unit_price >= 0);
                RAISE NOTICE '已创建 chk_psi_unit_price_nonneg';
            ELSE
                RAISE NOTICE '跳过 chk_psi_unit_price_nonneg：purchase_stockin_items 存在负单价记录，需先清理';
            END IF;
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 2. 数量字段非负约束（INTEGER 类型）
-- ------------------------------------------------------

-- 2.1 inventory.current_stock >= 0, safety_stock >= 0
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'inventory' AND constraint_name = 'chk_inventory_stock_nonneg'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM inventory
            WHERE current_stock < 0 OR safety_stock < 0
        ) THEN
            ALTER TABLE inventory
                ADD CONSTRAINT chk_inventory_stock_nonneg
                CHECK (current_stock >= 0 AND safety_stock >= 0);
            RAISE NOTICE '已创建 chk_inventory_stock_nonneg';
        ELSE
            RAISE NOTICE '跳过 chk_inventory_stock_nonneg：inventory 存在负库存记录，需先清理';
        END IF;
    END IF;
END $$;

-- 2.2 members.order_count >= 0
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'members' AND constraint_name = 'chk_members_order_count_nonneg'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM members WHERE order_count < 0) THEN
            ALTER TABLE members
                ADD CONSTRAINT chk_members_order_count_nonneg
                CHECK (order_count >= 0);
            RAISE NOTICE '已创建 chk_members_order_count_nonneg';
        ELSE
            RAISE NOTICE '跳过 chk_members_order_count_nonneg：members 存在负订单数记录，需先清理';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 3. 状态字段值域约束（INTEGER 类型）
-- ------------------------------------------------------

-- 3.1 members.status IN (1,2,3,4)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'members' AND constraint_name = 'chk_members_status_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM members WHERE status NOT IN (1,2,3,4)) THEN
            ALTER TABLE members
                ADD CONSTRAINT chk_members_status_domain
                CHECK (status IN (1,2,3,4));
            RAISE NOTICE '已创建 chk_members_status_domain';
        ELSE
            RAISE NOTICE '跳过 chk_members_status_domain：members 存在非法 status 值，需先清理';
        END IF;
    END IF;
END $$;

-- 3.2 product.status IN (0,1) （SMALLINT）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'product' AND constraint_name = 'chk_product_status_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM product WHERE status NOT IN (0,1)) THEN
            ALTER TABLE product
                ADD CONSTRAINT chk_product_status_domain
                CHECK (status IN (0,1));
            RAISE NOTICE '已创建 chk_product_status_domain';
        ELSE
            RAISE NOTICE '跳过 chk_product_status_domain：product 存在非法 status 值，需先清理';
        END IF;
    END IF;
END $$;

-- 3.3 food_trace_codes.status IN (1,2,3,4,5), risk_level IN (1,2,3)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'food_trace_codes' AND constraint_name = 'chk_ftc_status_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM food_trace_codes WHERE status NOT IN (1,2,3,4,5)) THEN
            ALTER TABLE food_trace_codes
                ADD CONSTRAINT chk_ftc_status_domain
                CHECK (status IN (1,2,3,4,5));
            RAISE NOTICE '已创建 chk_ftc_status_domain';
        ELSE
            RAISE NOTICE '跳过 chk_ftc_status_domain：food_trace_codes 存在非法 status 值，需先清理';
        END IF;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'food_trace_codes' AND constraint_name = 'chk_ftc_risk_level_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM food_trace_codes WHERE risk_level NOT IN (1,2,3)) THEN
            ALTER TABLE food_trace_codes
                ADD CONSTRAINT chk_ftc_risk_level_domain
                CHECK (risk_level IN (1,2,3));
            RAISE NOTICE '已创建 chk_ftc_risk_level_domain';
        ELSE
            RAISE NOTICE '跳过 chk_ftc_risk_level_domain：food_trace_codes 存在非法 risk_level 值，需先清理';
        END IF;
    END IF;
END $$;

-- 3.4 certificate_managements.cert_status IN (1,2,3,4)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'certificate_managements' AND constraint_name = 'chk_cm_cert_status_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM certificate_managements WHERE cert_status NOT IN (1,2,3,4)) THEN
            ALTER TABLE certificate_managements
                ADD CONSTRAINT chk_cm_cert_status_domain
                CHECK (cert_status IN (1,2,3,4));
            RAISE NOTICE '已创建 chk_cm_cert_status_domain';
        ELSE
            RAISE NOTICE '跳过 chk_cm_cert_status_domain：certificate_managements 存在非法 cert_status 值，需先清理';
        END IF;
    END IF;
END $$;

-- 3.5 compliance_reports.status IN (1,2,3,4)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'compliance_reports' AND constraint_name = 'chk_cr_status_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM compliance_reports WHERE status NOT IN (1,2,3,4)) THEN
            ALTER TABLE compliance_reports
                ADD CONSTRAINT chk_cr_status_domain
                CHECK (status IN (1,2,3,4));
            RAISE NOTICE '已创建 chk_cr_status_domain';
        ELSE
            RAISE NOTICE '跳过 chk_cr_status_domain：compliance_reports 存在非法 status 值，需先清理';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 4. deleted 字段值域约束 IN (0,1)
--    覆盖核心业务表（不含 audit_log，该表已移除 deleted 字段）
-- ------------------------------------------------------

-- 4.1 employees.deleted IN (0,1)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'employees' AND constraint_name = 'chk_employees_deleted_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM employees WHERE deleted NOT IN (0,1)) THEN
            ALTER TABLE employees
                ADD CONSTRAINT chk_employees_deleted_domain
                CHECK (deleted IN (0,1));
            RAISE NOTICE '已创建 chk_employees_deleted_domain';
        ELSE
            RAISE NOTICE '跳过 chk_employees_deleted_domain：employees 存在非法 deleted 值，需先清理';
        END IF;
    END IF;
END $$;

-- 4.2 users.deleted IN (0,1)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'users' AND constraint_name = 'chk_users_deleted_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM users WHERE deleted NOT IN (0,1)) THEN
            ALTER TABLE users
                ADD CONSTRAINT chk_users_deleted_domain
                CHECK (deleted IN (0,1));
            RAISE NOTICE '已创建 chk_users_deleted_domain';
        ELSE
            RAISE NOTICE '跳过 chk_users_deleted_domain：users 存在非法 deleted 值，需先清理';
        END IF;
    END IF;
END $$;

-- 4.3 voucher_header.deleted IN (0,1)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'voucher_header' AND constraint_name = 'chk_voucher_header_deleted_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM voucher_header WHERE deleted NOT IN (0,1)) THEN
            ALTER TABLE voucher_header
                ADD CONSTRAINT chk_voucher_header_deleted_domain
                CHECK (deleted IN (0,1));
            RAISE NOTICE '已创建 chk_voucher_header_deleted_domain';
        ELSE
            RAISE NOTICE '跳过 chk_voucher_header_deleted_domain：voucher_header 存在非法 deleted 值，需先清理';
        END IF;
    END IF;
END $$;

-- 4.4 members.deleted IN (0,1)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'members' AND constraint_name = 'chk_members_deleted_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM members WHERE deleted NOT IN (0,1)) THEN
            ALTER TABLE members
                ADD CONSTRAINT chk_members_deleted_domain
                CHECK (deleted IN (0,1));
            RAISE NOTICE '已创建 chk_members_deleted_domain';
        ELSE
            RAISE NOTICE '跳过 chk_members_deleted_domain：members 存在非法 deleted 值，需先清理';
        END IF;
    END IF;
END $$;

-- 4.5 suppliers.deleted IN (0,1)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'suppliers' AND constraint_name = 'chk_suppliers_deleted_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM suppliers WHERE deleted NOT IN (0,1)) THEN
            ALTER TABLE suppliers
                ADD CONSTRAINT chk_suppliers_deleted_domain
                CHECK (deleted IN (0,1));
            RAISE NOTICE '已创建 chk_suppliers_deleted_domain';
        ELSE
            RAISE NOTICE '跳过 chk_suppliers_deleted_domain：suppliers 存在非法 deleted 值，需先清理';
        END IF;
    END IF;
END $$;

-- 4.6 product.deleted IN (0,1)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'product' AND constraint_name = 'chk_product_deleted_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM product WHERE deleted NOT IN (0,1)) THEN
            ALTER TABLE product
                ADD CONSTRAINT chk_product_deleted_domain
                CHECK (deleted IN (0,1));
            RAISE NOTICE '已创建 chk_product_deleted_domain';
        ELSE
            RAISE NOTICE '跳过 chk_product_deleted_domain：product 存在非法 deleted 值，需先清理';
        END IF;
    END IF;
END $$;

-- 4.7 inventory.deleted IN (0,1)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'inventory' AND constraint_name = 'chk_inventory_deleted_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM inventory WHERE deleted NOT IN (0,1)) THEN
            ALTER TABLE inventory
                ADD CONSTRAINT chk_inventory_deleted_domain
                CHECK (deleted IN (0,1));
            RAISE NOTICE '已创建 chk_inventory_deleted_domain';
        ELSE
            RAISE NOTICE '跳过 chk_inventory_deleted_domain：inventory 存在非法 deleted 值，需先清理';
        END IF;
    END IF;
END $$;

-- 4.8 purchase_orders.deleted IN (0,1)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'purchase_orders' AND constraint_name = 'chk_purchase_orders_deleted_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM purchase_orders WHERE deleted NOT IN (0,1)) THEN
            ALTER TABLE purchase_orders
                ADD CONSTRAINT chk_purchase_orders_deleted_domain
                CHECK (deleted IN (0,1));
            RAISE NOTICE '已创建 chk_purchase_orders_deleted_domain';
        ELSE
            RAISE NOTICE '跳过 chk_purchase_orders_deleted_domain：purchase_orders 存在非法 deleted 值，需先清理';
        END IF;
    END IF;
END $$;

-- 4.9 food_trace_codes.deleted IN (0,1)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'food_trace_codes' AND constraint_name = 'chk_ftc_deleted_domain'
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM food_trace_codes WHERE deleted NOT IN (0,1)) THEN
            ALTER TABLE food_trace_codes
                ADD CONSTRAINT chk_ftc_deleted_domain
                CHECK (deleted IN (0,1));
            RAISE NOTICE '已创建 chk_ftc_deleted_domain';
        ELSE
            RAISE NOTICE '跳过 chk_ftc_deleted_domain：food_trace_codes 存在非法 deleted 值，需先清理';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 5. version 字段非负约束 >= 0
-- ------------------------------------------------------

-- 5.1 voucher_header.version >= 0
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'voucher_header' AND column_name = 'version'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE table_name = 'voucher_header' AND constraint_name = 'chk_voucher_header_version_nonneg'
        ) THEN
            IF NOT EXISTS (SELECT 1 FROM voucher_header WHERE version < 0) THEN
                ALTER TABLE voucher_header
                    ADD CONSTRAINT chk_voucher_header_version_nonneg
                    CHECK (version >= 0);
                RAISE NOTICE '已创建 chk_voucher_header_version_nonneg';
            ELSE
                RAISE NOTICE '跳过 chk_voucher_header_version_nonneg：voucher_header 存在负 version 值，需先清理';
            END IF;
        END IF;
    END IF;
END $$;

-- 5.2 suppliers.version >= 0
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'suppliers' AND column_name = 'version'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE table_name = 'suppliers' AND constraint_name = 'chk_suppliers_version_nonneg'
        ) THEN
            IF NOT EXISTS (SELECT 1 FROM suppliers WHERE version < 0) THEN
                ALTER TABLE suppliers
                    ADD CONSTRAINT chk_suppliers_version_nonneg
                    CHECK (version >= 0);
                RAISE NOTICE '已创建 chk_suppliers_version_nonneg';
            ELSE
                RAISE NOTICE '跳过 chk_suppliers_version_nonneg：suppliers 存在负 version 值，需先清理';
            END IF;
        END IF;
    END IF;
END $$;

-- 5.3 purchase_orders.version >= 0
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'purchase_orders' AND column_name = 'version'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE table_name = 'purchase_orders' AND constraint_name = 'chk_purchase_orders_version_nonneg'
        ) THEN
            IF NOT EXISTS (SELECT 1 FROM purchase_orders WHERE version < 0) THEN
                ALTER TABLE purchase_orders
                    ADD CONSTRAINT chk_purchase_orders_version_nonneg
                    CHECK (version >= 0);
                RAISE NOTICE '已创建 chk_purchase_orders_version_nonneg';
            ELSE
                RAISE NOTICE '跳过 chk_purchase_orders_version_nonneg：purchase_orders 存在负 version 值，需先清理';
            END IF;
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 6. email 字段格式校验（含 @）
-- ------------------------------------------------------

-- 6.1 employees.email 格式校验
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'employees' AND constraint_name = 'chk_employees_email_format'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM employees
            WHERE email IS NOT NULL AND email != '' AND email NOT LIKE '%@%'
        ) THEN
            ALTER TABLE employees
                ADD CONSTRAINT chk_employees_email_format
                CHECK (email IS NULL OR email = '' OR email LIKE '%@%');
            RAISE NOTICE '已创建 chk_employees_email_format';
        ELSE
            RAISE NOTICE '跳过 chk_employees_email_format：employees 存在不含 @ 的 email，需先清理';
        END IF;
    END IF;
END $$;

-- 6.2 users.email 格式校验
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'users' AND constraint_name = 'chk_users_email_format'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM users
            WHERE email IS NOT NULL AND email != '' AND email NOT LIKE '%@%'
        ) THEN
            ALTER TABLE users
                ADD CONSTRAINT chk_users_email_format
                CHECK (email IS NULL OR email = '' OR email LIKE '%@%');
            RAISE NOTICE '已创建 chk_users_email_format';
        ELSE
            RAISE NOTICE '跳过 chk_users_email_format：users 存在不含 @ 的 email，需先清理';
        END IF;
    END IF;
END $$;

-- 6.3 members.email 格式校验
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'members' AND constraint_name = 'chk_members_email_format'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM members
            WHERE email IS NOT NULL AND email != '' AND email NOT LIKE '%@%'
        ) THEN
            ALTER TABLE members
                ADD CONSTRAINT chk_members_email_format
                CHECK (email IS NULL OR email = '' OR email LIKE '%@%');
            RAISE NOTICE '已创建 chk_members_email_format';
        ELSE
            RAISE NOTICE '跳过 chk_members_email_format：members 存在不含 @ 的 email，需先清理';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 7. phone 字段格式校验（中国手机号或空）
--    规则：11 位数字，以 1 开头，第二位 3-9
-- ------------------------------------------------------

-- 7.1 employees.phone 格式校验
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'employees' AND constraint_name = 'chk_employees_phone_format'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM employees
            WHERE phone IS NOT NULL AND phone != ''
            AND phone !~ '^1[3-9][0-9]{9}$'
        ) THEN
            ALTER TABLE employees
                ADD CONSTRAINT chk_employees_phone_format
                CHECK (phone IS NULL OR phone = '' OR phone ~ '^1[3-9][0-9]{9}$');
            RAISE NOTICE '已创建 chk_employees_phone_format';
        ELSE
            RAISE NOTICE '跳过 chk_employees_phone_format：employees 存在非法 phone 格式，需先清理';
        END IF;
    END IF;
END $$;

-- 7.2 users.phone 格式校验
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'users' AND constraint_name = 'chk_users_phone_format'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM users
            WHERE phone IS NOT NULL AND phone != ''
            AND phone !~ '^1[3-9][0-9]{9}$'
        ) THEN
            ALTER TABLE users
                ADD CONSTRAINT chk_users_phone_format
                CHECK (phone IS NULL OR phone = '' OR phone ~ '^1[3-9][0-9]{9}$');
            RAISE NOTICE '已创建 chk_users_phone_format';
        ELSE
            RAISE NOTICE '跳过 chk_users_phone_format：users 存在非法 phone 格式，需先清理';
        END IF;
    END IF;
END $$;

-- 7.3 members.phone 格式校验
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'members' AND constraint_name = 'chk_members_phone_format'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM members
            WHERE phone IS NOT NULL AND phone != ''
            AND phone !~ '^1[3-9][0-9]{9}$'
        ) THEN
            ALTER TABLE members
                ADD CONSTRAINT chk_members_phone_format
                CHECK (phone IS NULL OR phone = '' OR phone ~ '^1[3-9][0-9]{9}$');
            RAISE NOTICE '已创建 chk_members_phone_format';
        ELSE
            RAISE NOTICE '跳过 chk_members_phone_format：members 存在非法 phone 格式，需先清理';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 8. 关键时间逻辑约束
-- ------------------------------------------------------

-- 8.1 food_trace_codes: expiry_date > production_date（如两者都存在）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'food_trace_codes' AND constraint_name = 'chk_ftc_date_logic'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM food_trace_codes
            WHERE production_date IS NOT NULL AND expiry_date IS NOT NULL
            AND expiry_date <= production_date
        ) THEN
            ALTER TABLE food_trace_codes
                ADD CONSTRAINT chk_ftc_date_logic
                CHECK (
                    production_date IS NULL OR expiry_date IS NULL
                    OR expiry_date > production_date
                );
            RAISE NOTICE '已创建 chk_ftc_date_logic';
        ELSE
            RAISE NOTICE '跳过 chk_ftc_date_logic：food_trace_codes 存在 expiry_date <= production_date 的记录，需先清理';
        END IF;
    END IF;
END $$;

-- 8.2 certificate_managements: expiry_date >= issue_date（如两者都存在）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'certificate_managements' AND constraint_name = 'chk_cm_date_logic'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM certificate_managements
            WHERE issue_date IS NOT NULL AND expiry_date IS NOT NULL
            AND expiry_date < issue_date
        ) THEN
            ALTER TABLE certificate_managements
                ADD CONSTRAINT chk_cm_date_logic
                CHECK (
                    issue_date IS NULL OR expiry_date IS NULL
                    OR expiry_date >= issue_date
                );
            RAISE NOTICE '已创建 chk_cm_date_logic';
        ELSE
            RAISE NOTICE '跳过 chk_cm_date_logic：certificate_managements 存在 expiry_date < issue_date 的记录，需先清理';
        END IF;
    END IF;
END $$;

-- 8.3 voucher_header: total_debit = total_credit（借贷平衡）
--     仅约束已审核凭证，草稿状态允许临时不平衡
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'voucher_header' AND constraint_name = 'chk_voucher_header_balance'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM voucher_header
            WHERE status IN ('APPROVED', 'POSTED')
            AND total_debit != total_credit
        ) THEN
            ALTER TABLE voucher_header
                ADD CONSTRAINT chk_voucher_header_balance
                CHECK (
                    status NOT IN ('APPROVED', 'POSTED')
                    OR total_debit = total_credit
                );
            RAISE NOTICE '已创建 chk_voucher_header_balance（仅约束已审核凭证）';
        ELSE
            RAISE NOTICE '跳过 chk_voucher_header_balance：voucher_header 存在已审核但借贷不平衡的凭证，需先清理';
        END IF;
    END IF;
END $$;
