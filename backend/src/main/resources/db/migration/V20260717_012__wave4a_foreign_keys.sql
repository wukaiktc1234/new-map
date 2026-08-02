-- ============================================================
-- Wave 4-A：外键约束补齐（DM-029）
-- 关联问题：数据模型审查报告 §4.9 外键缺失、ERP 合规报告 §7.2
-- 修复内容：
--   仅对"业务上确实存在父子关系"且"字段类型匹配"的引用补齐 FK
--   审查范围：
--     - 引用 employees.employee_id 的字段（多数类型不匹配，跳过）
--     - 引用 users.user_id 的字段（审计日志类软引用，跳过）
--     - 引用 stores.store_id 的字段（store_id VARCHAR vs BIGINT 不匹配，跳过）
--     - 引用 departments.department_id 的字段（多数类型不匹配，跳过）
--     - 引用 products.product_id 的字段（类型匹配，补齐）
--     - 引用 suppliers.supplier_id 的字段（类型匹配，补齐）
--     - 追溯子系统内部父子关系（类型匹配，补齐）
-- 设计原则：
--   - 使用 DO 块保证幂等（检查 information_schema.table_constraints）
--   - 添加前预检孤儿记录，存在孤儿时跳过并 RAISE NOTICE（不静默清理）
--   - 不使用 NOT VALID：项目当前数据量小，预检通过即完整约束
--   - 约束命名：fk_{child_table}_{parent_table}_{field}
--   - 审计日志（audit_log.*）保留为软引用，不加强约束
-- ============================================================

-- ------------------------------------------------------
-- 1. 追溯子系统内部父子关系
-- ------------------------------------------------------

-- 1.1 trace_chain_nodes.trace_code_id → food_trace_codes.trace_code_id
--     追溯链节点必须关联一个有效的追溯码（强父子关系）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'trace_chain_nodes'
          AND constraint_name = 'fk_tcn_trace_code_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM trace_chain_nodes tcn
            LEFT JOIN food_trace_codes ftc ON tcn.trace_code_id = ftc.trace_code_id
            WHERE tcn.trace_code_id IS NOT NULL AND ftc.trace_code_id IS NULL
        ) THEN
            ALTER TABLE trace_chain_nodes
                ADD CONSTRAINT fk_tcn_trace_code_id
                FOREIGN KEY (trace_code_id) REFERENCES food_trace_codes(trace_code_id);
            RAISE NOTICE '已创建 fk_tcn_trace_code_id';
        ELSE
            RAISE NOTICE '跳过 fk_tcn_trace_code_id：trace_chain_nodes 存在孤儿 trace_code_id，需先清理';
        END IF;
    END IF;
END $$;

-- 1.2 food_trace_codes.purchase_stockin_id → purchase_stockins.stockin_id
--     追溯码可关联采购入库单（可空，存在则必须有效）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'food_trace_codes'
          AND constraint_name = 'fk_ftc_purchase_stockin_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM food_trace_codes ftc
            LEFT JOIN purchase_stockins ps ON ftc.purchase_stockin_id = ps.stockin_id
            WHERE ftc.purchase_stockin_id IS NOT NULL AND ps.stockin_id IS NULL
        ) THEN
            ALTER TABLE food_trace_codes
                ADD CONSTRAINT fk_ftc_purchase_stockin_id
                FOREIGN KEY (purchase_stockin_id) REFERENCES purchase_stockins(stockin_id);
            RAISE NOTICE '已创建 fk_ftc_purchase_stockin_id';
        ELSE
            RAISE NOTICE '跳过 fk_ftc_purchase_stockin_id：food_trace_codes 存在孤儿 purchase_stockin_id，需先清理';
        END IF;
    END IF;
END $$;

-- 1.3 food_trace_codes.supplier_id → suppliers.supplier_id
--     追溯码可关联供应商（可空，存在则必须有效）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'food_trace_codes'
          AND constraint_name = 'fk_ftc_supplier_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM food_trace_codes ftc
            LEFT JOIN suppliers s ON ftc.supplier_id = s.supplier_id
            WHERE ftc.supplier_id IS NOT NULL AND s.supplier_id IS NULL
        ) THEN
            ALTER TABLE food_trace_codes
                ADD CONSTRAINT fk_ftc_supplier_id
                FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id);
            RAISE NOTICE '已创建 fk_ftc_supplier_id';
        ELSE
            RAISE NOTICE '跳过 fk_ftc_supplier_id：food_trace_codes 存在孤儿 supplier_id，需先清理';
        END IF;
    END IF;
END $$;

-- 1.4 compliance_check_items.report_id → compliance_reports.report_id
--     合规检查项必须属于一个合规报告（强父子关系）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'compliance_check_items'
          AND constraint_name = 'fk_cci_report_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM compliance_check_items cci
            LEFT JOIN compliance_reports cr ON cci.report_id = cr.report_id
            WHERE cci.report_id IS NOT NULL AND cr.report_id IS NULL
        ) THEN
            ALTER TABLE compliance_check_items
                ADD CONSTRAINT fk_cci_report_id
                FOREIGN KEY (report_id) REFERENCES compliance_reports(report_id);
            RAISE NOTICE '已创建 fk_cci_report_id';
        ELSE
            RAISE NOTICE '跳过 fk_cci_report_id：compliance_check_items 存在孤儿 report_id，需先清理';
        END IF;
    END IF;
END $$;

-- 1.5 expiry_warning_records.rule_id → expiry_warning_rules.rule_id
--     预警记录必须关联一个有效的预警规则（强父子关系）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'expiry_warning_records'
          AND constraint_name = 'fk_ewr_rule_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM expiry_warning_records ewr
            LEFT JOIN expiry_warning_rules ewrule ON ewr.rule_id = ewrule.rule_id
            WHERE ewr.rule_id IS NOT NULL AND ewrule.rule_id IS NULL
        ) THEN
            ALTER TABLE expiry_warning_records
                ADD CONSTRAINT fk_ewr_rule_id
                FOREIGN KEY (rule_id) REFERENCES expiry_warning_rules(rule_id);
            RAISE NOTICE '已创建 fk_ewr_rule_id';
        ELSE
            RAISE NOTICE '跳过 fk_ewr_rule_id：expiry_warning_records 存在孤儿 rule_id，需先清理';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 2. 采购子系统内部父子关系
-- ------------------------------------------------------

-- 2.1 purchase_stockin_items.stockin_id → purchase_stockins.stockin_id
--     入库单明细必须属于一个入库单（强父子关系）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'purchase_stockin_items'
          AND constraint_name = 'fk_psi_stockin_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM purchase_stockin_items psi
            LEFT JOIN purchase_stockins ps ON psi.stockin_id = ps.stockin_id
            WHERE psi.stockin_id IS NOT NULL AND ps.stockin_id IS NULL
        ) THEN
            ALTER TABLE purchase_stockin_items
                ADD CONSTRAINT fk_psi_stockin_id
                FOREIGN KEY (stockin_id) REFERENCES purchase_stockins(stockin_id);
            RAISE NOTICE '已创建 fk_psi_stockin_id';
        ELSE
            RAISE NOTICE '跳过 fk_psi_stockin_id：purchase_stockin_items 存在孤儿 stockin_id，需先清理';
        END IF;
    END IF;
END $$;

-- 2.2 purchase_stockins.order_id → purchase_orders.order_id
--     入库单可关联采购订单（可空，存在则必须有效）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'purchase_stockins'
          AND constraint_name = 'fk_ps_order_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM purchase_stockins ps
            LEFT JOIN purchase_orders po ON ps.order_id = po.order_id
            WHERE ps.order_id IS NOT NULL AND po.order_id IS NULL
        ) THEN
            ALTER TABLE purchase_stockins
                ADD CONSTRAINT fk_ps_order_id
                FOREIGN KEY (order_id) REFERENCES purchase_orders(order_id);
            RAISE NOTICE '已创建 fk_ps_order_id';
        ELSE
            RAISE NOTICE '跳过 fk_ps_order_id：purchase_stockins 存在孤儿 order_id，需先清理';
        END IF;
    END IF;
END $$;

-- 2.3 purchase_stockins.supplier_id → suppliers.supplier_id
--     入库单可关联供应商（可空，存在则必须有效）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'purchase_stockins'
          AND constraint_name = 'fk_ps_supplier_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM purchase_stockins ps
            LEFT JOIN suppliers s ON ps.supplier_id = s.supplier_id
            WHERE ps.supplier_id IS NOT NULL AND s.supplier_id IS NULL
        ) THEN
            ALTER TABLE purchase_stockins
                ADD CONSTRAINT fk_ps_supplier_id
                FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id);
            RAISE NOTICE '已创建 fk_ps_supplier_id';
        ELSE
            RAISE NOTICE '跳过 fk_ps_supplier_id：purchase_stockins 存在孤儿 supplier_id，需先清理';
        END IF;
    END IF;
END $$;

-- 2.4 purchase_ledgers.purchase_stockin_id → purchase_stockins.stockin_id
--     进货台账可关联采购入库单（可空，存在则必须有效）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'purchase_ledgers'
          AND constraint_name = 'fk_pl_purchase_stockin_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM purchase_ledgers pl
            LEFT JOIN purchase_stockins ps ON pl.purchase_stockin_id = ps.stockin_id
            WHERE pl.purchase_stockin_id IS NOT NULL AND ps.stockin_id IS NULL
        ) THEN
            ALTER TABLE purchase_ledgers
                ADD CONSTRAINT fk_pl_purchase_stockin_id
                FOREIGN KEY (purchase_stockin_id) REFERENCES purchase_stockins(stockin_id);
            RAISE NOTICE '已创建 fk_pl_purchase_stockin_id';
        ELSE
            RAISE NOTICE '跳过 fk_pl_purchase_stockin_id：purchase_ledgers 存在孤儿 purchase_stockin_id，需先清理';
        END IF;
    END IF;
END $$;

-- 2.5 purchase_ledgers.supplier_id → suppliers.supplier_id
--     进货台账必须关联一个有效供应商（NOT NULL 字段，强约束）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'purchase_ledgers'
          AND constraint_name = 'fk_pl_supplier_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM purchase_ledgers pl
            LEFT JOIN suppliers s ON pl.supplier_id = s.supplier_id
            WHERE pl.supplier_id IS NOT NULL AND s.supplier_id IS NULL
        ) THEN
            ALTER TABLE purchase_ledgers
                ADD CONSTRAINT fk_pl_supplier_id
                FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id);
            RAISE NOTICE '已创建 fk_pl_supplier_id';
        ELSE
            RAISE NOTICE '跳过 fk_pl_supplier_id：purchase_ledgers 存在孤儿 supplier_id，需先清理';
        END IF;
    END IF;
END $$;

-- 2.6 certificate_managements.supplier_id → suppliers.supplier_id
--     索证索票可关联供应商（可空，存在则必须有效）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'certificate_managements'
          AND constraint_name = 'fk_cm_supplier_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM certificate_managements cm
            LEFT JOIN suppliers s ON cm.supplier_id = s.supplier_id
            WHERE cm.supplier_id IS NOT NULL AND s.supplier_id IS NULL
        ) THEN
            ALTER TABLE certificate_managements
                ADD CONSTRAINT fk_cm_supplier_id
                FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id);
            RAISE NOTICE '已创建 fk_cm_supplier_id';
        ELSE
            RAISE NOTICE '跳过 fk_cm_supplier_id：certificate_managements 存在孤儿 supplier_id，需先清理';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 3. 库存子系统父子关系
-- ------------------------------------------------------

-- 3.1 inventory.product_id → product.product_id
--     库存记录可关联产品（可空，存在则必须有效）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'inventory'
          AND constraint_name = 'fk_inv_product_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM inventory i
            LEFT JOIN product p ON i.product_id = p.product_id
            WHERE i.product_id IS NOT NULL AND p.product_id IS NULL
        ) THEN
            ALTER TABLE inventory
                ADD CONSTRAINT fk_inv_product_id
                FOREIGN KEY (product_id) REFERENCES product(product_id);
            RAISE NOTICE '已创建 fk_inv_product_id';
        ELSE
            RAISE NOTICE '跳过 fk_inv_product_id：inventory 存在孤儿 product_id，需先清理';
        END IF;
    END IF;
END $$;

-- ============================================================
-- 跳过说明（不在本脚本中创建，原因记录在 wave4-a-db-constraints-summary.md）
-- ============================================================
-- 1. 所有引用 employees.employee_id 的 FK：employees.employee_id 为 VARCHAR(50)，
--    但大多数引用方（如 audit_log.user_id, food_trace_codes.create_user_id）为 BIGINT，
--    类型不匹配无法创建 FK。需先在另一个 Wave 统一类型后再补齐。
-- 2. 所有引用 users.user_id 的 FK：user_id 为 BIGINT 类型匹配，但 audit_log.user_id
--    按设计为软引用（审计日志不应强约束，避免影响日志写入），保留软引用。
-- 3. 所有引用 stores.store_id 的 FK：stores.store_id 为 VARCHAR(32)，但大多数引用方
--    （如 orders.store_id, employees.store_id）为 BIGINT，类型不匹配无法创建 FK。
-- 4. 所有引用 departments.department_id 的 FK：departments.department_id 为 BIGINT，
--    但多数引用方（如 employees.department_id, users.department_id）为 VARCHAR(50)，
--    类型不匹配无法创建 FK。
-- 5. 引用 product.product_id 但字段为 VARCHAR 的引用（如 product.category_id），
--    类型不匹配无法创建 FK。
-- 6. purchase_orders.supplier_id 为 VARCHAR(50)，与 suppliers.supplier_id (BIGINT)
--    类型不匹配，无法创建 FK。
-- ============================================================
