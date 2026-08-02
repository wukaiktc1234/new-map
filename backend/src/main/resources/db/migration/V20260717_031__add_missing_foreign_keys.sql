-- ============================================================
-- Wave 5-B：补齐 Wave 4-A 因类型不匹配跳过的外键约束
-- 关联问题：Wave 4-A §4.2 跳过的 FK
-- 前置依赖：V20260717_030__unify_id_types.sql 必须先执行
--
-- 修复内容：
--   V20260717_030 已将 stores.store_id、employees.employee_id、
--   purchase_orders.supplier_id 等字段统一为 BIGINT。
--   本脚本补齐 Wave 4-A 因类型不匹配跳过的 FK 约束。
--
-- 跳过清单（来自 wave4-a-db-constraints-summary.md §4.2）：
--   1. 引用 employees.employee_id 的 FK（类型不匹配）
--   2. 引用 stores.store_id 的 FK（类型不匹配）
--   3. 引用 departments.department_id 的 FK（类型不匹配）
--   4. purchase_orders.supplier_id → suppliers.supplier_id（类型不匹配）
--   注：审计日志 audit_log.user_id 按设计保留软引用，不强约束
--
-- 设计原则：
--   - 使用 DO 块保证幂等（检查 information_schema.table_constraints）
--   - 添加前预检孤儿记录，存在孤儿时跳过并 RAISE NOTICE
--   - 不使用 NOT VALID：项目当前数据量小，预检通过即完整约束
--   - 约束命名：fk_{child_table}_{parent_table}_{field} 或 fk_{child}_{field}
--   - 审计日志（audit_log.*）保留为软引用
--
-- 数据可重建说明：
--   用户已确认数据可重建，V20260717_030 中非数值字符串已转为 NULL。
--   NULL 的外键值不构成孤儿（外键允许 NULL），故不会阻塞 FK 创建。
--   非 NULL 但不存在于父表的值才构成孤儿（如已删除的父记录 ID）。
-- ============================================================

-- ------------------------------------------------------
-- 1. 引用 stores.store_id 的 FK
-- ------------------------------------------------------

-- 1.1 orders.store_id → stores.store_id
--     订单关联门店（可空，存在则必须有效）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'orders' AND constraint_name = 'fk_orders_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM orders o
            LEFT JOIN stores s ON o.store_id = s.store_id
            WHERE o.store_id IS NOT NULL AND s.store_id IS NULL
        ) THEN
            ALTER TABLE orders
                ADD CONSTRAINT fk_orders_store_id
                FOREIGN KEY (store_id) REFERENCES stores(store_id);
            RAISE NOTICE '已创建 fk_orders_store_id';
        ELSE
            RAISE NOTICE '跳过 fk_orders_store_id：orders 存在孤儿 store_id，需先清理';
        END IF;
    END IF;
END $$;

-- 1.2 employees.store_id → stores.store_id
--     员工所属门店（可空，存在则必须有效）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'employees' AND constraint_name = 'fk_employees_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM employees e
            LEFT JOIN stores s ON e.store_id = s.store_id
            WHERE e.store_id IS NOT NULL AND s.store_id IS NULL
        ) THEN
            ALTER TABLE employees
                ADD CONSTRAINT fk_employees_store_id
                FOREIGN KEY (store_id) REFERENCES stores(store_id);
            RAISE NOTICE '已创建 fk_employees_store_id';
        ELSE
            RAISE NOTICE '跳过 fk_employees_store_id：employees 存在孤儿 store_id，需先清理';
        END IF;
    END IF;
END $$;

-- 1.3 users.store_id → stores.store_id
--     用户所属门店（可空）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'users' AND constraint_name = 'fk_users_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM users u
            LEFT JOIN stores s ON u.store_id = s.store_id
            WHERE u.store_id IS NOT NULL AND s.store_id IS NULL
        ) THEN
            ALTER TABLE users
                ADD CONSTRAINT fk_users_store_id
                FOREIGN KEY (store_id) REFERENCES stores(store_id);
            RAISE NOTICE '已创建 fk_users_store_id';
        ELSE
            RAISE NOTICE '跳过 fk_users_store_id：users 存在孤儿 store_id，需先清理';
        END IF;
    END IF;
END $$;

-- 1.4 departments.store_id → stores.store_id
--     部门所属门店（可空）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'departments' AND constraint_name = 'fk_departments_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM departments d
            LEFT JOIN stores s ON d.store_id = s.store_id
            WHERE d.store_id IS NOT NULL AND s.store_id IS NULL
        ) THEN
            ALTER TABLE departments
                ADD CONSTRAINT fk_departments_store_id
                FOREIGN KEY (store_id) REFERENCES stores(store_id);
            RAISE NOTICE '已创建 fk_departments_store_id';
        ELSE
            RAISE NOTICE '跳过 fk_departments_store_id：departments 存在孤儿 store_id，需先清理';
        END IF;
    END IF;
END $$;

-- 1.5 purchase_orders.store_id → stores.store_id
--     采购订单关联门店（可空）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'purchase_orders' AND constraint_name = 'fk_purchase_orders_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM purchase_orders po
            LEFT JOIN stores s ON po.store_id = s.store_id
            WHERE po.store_id IS NOT NULL AND s.store_id IS NULL
        ) THEN
            ALTER TABLE purchase_orders
                ADD CONSTRAINT fk_purchase_orders_store_id
                FOREIGN KEY (store_id) REFERENCES stores(store_id);
            RAISE NOTICE '已创建 fk_purchase_orders_store_id';
        ELSE
            RAISE NOTICE '跳过 fk_purchase_orders_store_id：purchase_orders 存在孤儿 store_id，需先清理';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 2. 引用 employees.employee_id 的 FK
-- ------------------------------------------------------

-- 2.1 material_consumption.chef_id → employees.employee_id
--     物料消耗记录关联厨师（可空）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'material_consumption' AND constraint_name = 'fk_mc_chef_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM material_consumption mc
            LEFT JOIN employees e ON mc.chef_id = e.employee_id
            WHERE mc.chef_id IS NOT NULL AND e.employee_id IS NULL
        ) THEN
            ALTER TABLE material_consumption
                ADD CONSTRAINT fk_mc_chef_id
                FOREIGN KEY (chef_id) REFERENCES employees(employee_id);
            RAISE NOTICE '已创建 fk_mc_chef_id';
        ELSE
            RAISE NOTICE '跳过 fk_mc_chef_id：material_consumption 存在孤儿 chef_id，需先清理';
        END IF;
    END IF;
END $$;

-- 2.2 food_trace_code.chef_id → employees.employee_id
--     食品追溯码关联厨师（可空）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'food_trace_code' AND constraint_name = 'fk_ftc_chef_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM food_trace_code ftc
            LEFT JOIN employees e ON ftc.chef_id = e.employee_id
            WHERE ftc.chef_id IS NOT NULL AND e.employee_id IS NULL
        ) THEN
            ALTER TABLE food_trace_code
                ADD CONSTRAINT fk_ftc_chef_id
                FOREIGN KEY (chef_id) REFERENCES employees(employee_id);
            RAISE NOTICE '已创建 fk_ftc_chef_id';
        ELSE
            RAISE NOTICE '跳过 fk_ftc_chef_id：food_trace_code 存在孤儿 chef_id，需先清理';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 3. 引用 departments.department_id 的 FK（employees/users/positions）
-- ------------------------------------------------------

-- 3.1 employees.department_id → departments.department_id
--     员工所属部门（可空，存在则必须有效）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'employees' AND constraint_name = 'fk_employees_department_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM employees e
            LEFT JOIN departments d ON e.department_id = d.department_id
            WHERE e.department_id IS NOT NULL AND d.department_id IS NULL
        ) THEN
            ALTER TABLE employees
                ADD CONSTRAINT fk_employees_department_id
                FOREIGN KEY (department_id) REFERENCES departments(department_id);
            RAISE NOTICE '已创建 fk_employees_department_id';
        ELSE
            RAISE NOTICE '跳过 fk_employees_department_id：employees 存在孤儿 department_id，需先清理';
        END IF;
    END IF;
END $$;

-- 3.2 users.department_id → departments.department_id
--     用户所属部门（可空）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'users' AND constraint_name = 'fk_users_department_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM users u
            LEFT JOIN departments d ON u.department_id = d.department_id
            WHERE u.department_id IS NOT NULL AND d.department_id IS NULL
        ) THEN
            ALTER TABLE users
                ADD CONSTRAINT fk_users_department_id
                FOREIGN KEY (department_id) REFERENCES departments(department_id);
            RAISE NOTICE '已创建 fk_users_department_id';
        ELSE
            RAISE NOTICE '跳过 fk_users_department_id：users 存在孤儿 department_id，需先清理';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 4. 引用 positions.position_id 的 FK
-- ------------------------------------------------------

-- 4.1 employees.position_id → positions.position_id
--     员工所属职位（可空，存在则必须有效）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'employees' AND constraint_name = 'fk_employees_position_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM employees e
            LEFT JOIN positions p ON e.position_id = p.position_id
            WHERE e.position_id IS NOT NULL AND p.position_id IS NULL
        ) THEN
            ALTER TABLE employees
                ADD CONSTRAINT fk_employees_position_id
                FOREIGN KEY (position_id) REFERENCES positions(position_id);
            RAISE NOTICE '已创建 fk_employees_position_id';
        ELSE
            RAISE NOTICE '跳过 fk_employees_position_id：employees 存在孤儿 position_id，需先清理';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 5. 引用 suppliers.supplier_id 的 FK（purchase_orders 等）
-- ------------------------------------------------------

-- 5.1 purchase_orders.supplier_id → suppliers.supplier_id
--     采购订单关联供应商（可空，存在则必须有效）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'purchase_orders' AND constraint_name = 'fk_purchase_orders_supplier_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM purchase_orders po
            LEFT JOIN suppliers s ON po.supplier_id = s.supplier_id
            WHERE po.supplier_id IS NOT NULL AND s.supplier_id IS NULL
        ) THEN
            ALTER TABLE purchase_orders
                ADD CONSTRAINT fk_purchase_orders_supplier_id
                FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id);
            RAISE NOTICE '已创建 fk_purchase_orders_supplier_id';
        ELSE
            RAISE NOTICE '跳过 fk_purchase_orders_supplier_id：purchase_orders 存在孤儿 supplier_id，需先清理';
        END IF;
    END IF;
END $$;

-- 5.2 material_trace_code.supplier_id → suppliers.supplier_id
--     物料追溯码关联供应商（可空）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'material_trace_code' AND constraint_name = 'fk_mtc_supplier_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM material_trace_code mtc
            LEFT JOIN suppliers s ON mtc.supplier_id = s.supplier_id
            WHERE mtc.supplier_id IS NOT NULL AND s.supplier_id IS NULL
        ) THEN
            ALTER TABLE material_trace_code
                ADD CONSTRAINT fk_mtc_supplier_id
                FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id);
            RAISE NOTICE '已创建 fk_mtc_supplier_id';
        ELSE
            RAISE NOTICE '跳过 fk_mtc_supplier_id：material_trace_code 存在孤儿 supplier_id，需先清理';
        END IF;
    END IF;
END $$;

-- ------------------------------------------------------
-- 6. 引用 purchase_orders.order_id 的 FK
-- ------------------------------------------------------

-- 6.1 purchase_order_items.order_id → purchase_orders.order_id
--     采购订单明细必须属于一个采购订单（强父子关系）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'purchase_order_items' AND constraint_name = 'fk_poi_order_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM purchase_order_items poi
            LEFT JOIN purchase_orders po ON poi.order_id = po.order_id
            WHERE poi.order_id IS NOT NULL AND po.order_id IS NULL
        ) THEN
            ALTER TABLE purchase_order_items
                ADD CONSTRAINT fk_poi_order_id
                FOREIGN KEY (order_id) REFERENCES purchase_orders(order_id);
            RAISE NOTICE '已创建 fk_poi_order_id';
        ELSE
            RAISE NOTICE '跳过 fk_poi_order_id：purchase_order_items 存在孤儿 order_id，需先清理';
        END IF;
    END IF;
END $$;

-- 6.2 material_trace_code.purchase_order_id → purchase_orders.order_id
--     物料追溯码关联采购订单（可空）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'material_trace_code' AND constraint_name = 'fk_mtc_purchase_order_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM material_trace_code mtc
            LEFT JOIN purchase_orders po ON mtc.purchase_order_id = po.order_id
            WHERE mtc.purchase_order_id IS NOT NULL AND po.order_id IS NULL
        ) THEN
            ALTER TABLE material_trace_code
                ADD CONSTRAINT fk_mtc_purchase_order_id
                FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(order_id);
            RAISE NOTICE '已创建 fk_mtc_purchase_order_id';
        ELSE
            RAISE NOTICE '跳过 fk_mtc_purchase_order_id：material_trace_code 存在孤儿 purchase_order_id，需先清理';
        END IF;
    END IF;
END $$;

-- ============================================================
-- 跳过说明（不在本脚本中创建）
-- ============================================================
-- 1. audit_log.user_id → users.user_id（保留软引用，符合审计日志设计原则）
-- 2. food_trace_codes.create_user_id → employees.employee_id
--    （追溯码创建人为软引用，避免删除员工导致追溯链断裂）
-- 3. audit_log.business_key 等审计字段（保留软引用）
-- 4. food_trace_code.order_id → orders.order_id
--    （food_trace_code 为旧表，order_id 字段历史复杂，留待后续 Wave 处理）
-- ============================================================
