-- ============================================================
-- Wave 4-A：索引补齐
-- 关联问题：数据模型审查报告 §4.12 索引缺失、ERP 合规报告 §7.4
-- 修复内容：
--   1. 为 V20260717_012 新增 FK 引用列补齐索引（FK 性能保障）
--   2. 为高频列表查询添加组合索引（deleted + status + create_time）
--   3. 为高频范围查询添加组合索引（warning_level + expiry_date 等）
-- 设计原则：
--   - 使用 CREATE INDEX IF NOT EXISTS 保证幂等
--   - 使用 DO 块预检列存在性，列不存在时跳过并 RAISE NOTICE
--   - 不使用 CONCURRENTLY：Flyway 默认在事务内执行，CONCURRENTLY 不支持事务
--     项目当前数据量小，普通 CREATE INDEX 锁表时间可接受
--     生产环境大数据量升级时，应在维护窗口手动使用 CREATE INDEX CONCURRENTLY
--   - 避免冗余索引：仅添加比现有单列索引提供明显价值的组合索引
--   - 索引命名：idx_{table}_{semantic} 或 idx_{table}_{field1}_{field2}
-- ============================================================

-- ------------------------------------------------------
-- 1. FK 引用列索引补齐（PostgreSQL 不自动为 FK 引用列创建索引）
-- ------------------------------------------------------

-- 1.1 food_trace_codes.purchase_stockin_id 索引
--     V20260717_012 新增 FK fk_ftc_purchase_stockin_id 的引用列
--     其他 FK 引用列已在原建表脚本中创建索引（详见末尾说明）
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'food_trace_codes' AND column_name = 'purchase_stockin_id'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE tablename = 'food_trace_codes' AND indexname = 'idx_ftc_purchase_stockin_id'
        ) THEN
            CREATE INDEX idx_ftc_purchase_stockin_id ON food_trace_codes(purchase_stockin_id);
            RAISE NOTICE '已创建 idx_ftc_purchase_stockin_id';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 2. 高频列表查询组合索引（deleted + status + create_time）
--    典型场景：列表页 WHERE deleted=0 AND status=? ORDER BY create_time DESC LIMIT 20
-- ------------------------------------------------------

-- 2.1 food_trace_codes: (deleted, status, create_time)
--     现有 idx_trace_code_status_risk (status, risk_level) 不含 deleted/create_time
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'food_trace_codes' AND indexname = 'idx_ftc_deleted_status_create_time'
    ) THEN
        CREATE INDEX idx_ftc_deleted_status_create_time
            ON food_trace_codes(deleted, status, create_time DESC);
        RAISE NOTICE '已创建 idx_ftc_deleted_status_create_time';
    END IF;
END $$;

-- 2.2 purchase_stockins: (deleted, status, create_time)
--     现有 status / create_time 均为单列索引，组合索引可避免回表
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'purchase_stockins' AND indexname = 'idx_ps_deleted_status_create_time'
    ) THEN
        CREATE INDEX idx_ps_deleted_status_create_time
            ON purchase_stockins(deleted, status, create_time DESC);
        RAISE NOTICE '已创建 idx_ps_deleted_status_create_time';
    END IF;
END $$;

-- 2.3 purchase_orders: (deleted, order_status, create_time)
--     现有 order_status / create_time 均为单列索引
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'purchase_orders' AND indexname = 'idx_po_deleted_status_create_time'
    ) THEN
        CREATE INDEX idx_po_deleted_status_create_time
            ON purchase_orders(deleted, order_status, create_time DESC);
        RAISE NOTICE '已创建 idx_po_deleted_status_create_time';
    END IF;
END $$;

-- 2.4 compliance_reports: (deleted, status, inspect_time)
--     现有 status / inspect_time 均为单列索引
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'compliance_reports' AND indexname = 'idx_cr_deleted_status_time'
    ) THEN
        CREATE INDEX idx_cr_deleted_status_time
            ON compliance_reports(deleted, status, inspect_time DESC);
        RAISE NOTICE '已创建 idx_cr_deleted_status_time';
    END IF;
END $$;

-- 2.5 certificate_managements: (deleted, cert_status, expiry_date)
--     现有 cert_status / expiry_date 均为单列索引
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'certificate_managements' AND indexname = 'idx_cm_deleted_status_expiry'
    ) THEN
        CREATE INDEX idx_cm_deleted_status_expiry
            ON certificate_managements(deleted, cert_status, expiry_date);
        RAISE NOTICE '已创建 idx_cm_deleted_status_expiry';
    END IF;
END $$;

-- 2.6 expiry_warning_records: (deleted, warning_level, expiry_date)
--     现有 warning_level / expiry_date 均为单列索引
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'expiry_warning_records' AND indexname = 'idx_ewr_deleted_level_expiry'
    ) THEN
        CREATE INDEX idx_ewr_deleted_level_expiry
            ON expiry_warning_records(deleted, warning_level, expiry_date);
        RAISE NOTICE '已创建 idx_ewr_deleted_level_expiry';
    END IF;
END $$;

-- 2.7 voucher_header: (deleted, status, create_time)
--     现有索引不含 status + create_time + deleted 组合
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'voucher_header' AND indexname = 'idx_vh_deleted_status_create_time'
    ) THEN
        CREATE INDEX idx_vh_deleted_status_create_time
            ON voucher_header(deleted, status, create_time DESC);
        RAISE NOTICE '已创建 idx_vh_deleted_status_create_time';
    END IF;
END $$;

-- ------------------------------------------------------
-- 3. 高频筛选组合索引（deleted + status）
--    典型场景：下拉框/统计 WHERE deleted=0 AND status=?
--    仅添加现有单列 status 索引无法高效支持 deleted 过滤的表
-- ------------------------------------------------------

-- 3.1 employees: (deleted, status)
--     现有 idx_employees_status 单列，员工列表高频查询
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'employees' AND indexname = 'idx_emp_deleted_status'
    ) THEN
        CREATE INDEX idx_emp_deleted_status ON employees(deleted, status);
        RAISE NOTICE '已创建 idx_emp_deleted_status';
    END IF;
END $$;

-- 3.2 users: (deleted, status)
--     现有 idx_users_status 单列，用户列表高频查询
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'users' AND indexname = 'idx_users_deleted_status'
    ) THEN
        CREATE INDEX idx_users_deleted_status ON users(deleted, status);
        RAISE NOTICE '已创建 idx_users_deleted_status';
    END IF;
END $$;

-- ============================================================
-- 已有索引说明（无需重复创建）
-- ============================================================
-- 以下 FK 引用列已在原建表脚本中创建索引，本脚本不重复创建：
--
-- trace_chain_nodes.trace_code_id:    V11.0.0 idx_chain_node_trace_code
-- food_trace_codes.supplier_id:       V11.0.0 idx_trace_code_supplier
-- compliance_check_items.report_id:   V11.0.0 idx_item_report
-- expiry_warning_records.rule_id:     V11.0.0 idx_record_rule
-- purchase_stockin_items.stockin_id:  V20260629_009 idx_purchase_stockin_items_stockin_id
-- purchase_stockins.order_id:         V20260629_009 idx_purchase_stockins_order_id
-- purchase_stockins.supplier_id:      V20260629_009 idx_purchase_stockins_supplier_id
-- purchase_ledgers.purchase_stockin_id: V11.0.0 idx_ledger_stockin
-- purchase_ledgers.supplier_id:       V11.0.0 idx_ledger_supplier
-- certificate_managements.supplier_id: V11.0.0 idx_cert_supplier
-- inventory.product_id:               V1.0.0.100 idx_inventory_product_id
-- ============================================================
-- 跳过的冗余索引候选（已有等效或更优索引）：
--
-- trace_chain_nodes (deleted, trace_code_id):
--   现有 idx_chain_node_trace_code (trace_code_id) 已能支持 FK 和查询
--
-- purchase_stockin_items (deleted, stockin_id):
--   现有 idx_purchase_stockin_items_stockin_id 单列已足够
--
-- compliance_check_items (deleted, report_id):
--   现有 idx_item_report 单列已足够
--
-- inventory (deleted, store_id, product_id):
--   现有 idx_inventory_store_id + idx_inventory_product_id 已覆盖
-- ============================================================
