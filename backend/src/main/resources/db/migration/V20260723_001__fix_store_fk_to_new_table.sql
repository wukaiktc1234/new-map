-- ============================================================
-- V20260723_001__fix_store_fk_to_new_table.sql
-- 修复门店相关外键指向：旧 stores 表已被 stores_new 替代，
-- 应用层（StoreNewService）统一写入 stores_new，故外键必须引用 stores_new.store_id。
-- ============================================================

DO $$
BEGIN
    -- ============================================================
    -- 1. employees.store_id -> stores_new.store_id
    -- ============================================================
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'employees' AND constraint_name = 'fk_employees_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        ALTER TABLE employees DROP CONSTRAINT fk_employees_store_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'employees' AND constraint_name = 'fk_employees_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'stores_new') THEN
        ALTER TABLE employees
            ADD CONSTRAINT fk_employees_store_id
            FOREIGN KEY (store_id) REFERENCES stores_new(store_id);
    END IF;

    -- ============================================================
    -- 2. users.store_id -> stores_new.store_id
    -- ============================================================
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'users' AND constraint_name = 'fk_users_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        ALTER TABLE users DROP CONSTRAINT fk_users_store_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'users' AND constraint_name = 'fk_users_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'stores_new') THEN
        ALTER TABLE users
            ADD CONSTRAINT fk_users_store_id
            FOREIGN KEY (store_id) REFERENCES stores_new(store_id);
    END IF;

    -- ============================================================
    -- 3. departments.store_id -> stores_new.store_id
    -- ============================================================
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'departments' AND constraint_name = 'fk_departments_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        ALTER TABLE departments DROP CONSTRAINT fk_departments_store_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'departments' AND constraint_name = 'fk_departments_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'stores_new') THEN
        ALTER TABLE departments
            ADD CONSTRAINT fk_departments_store_id
            FOREIGN KEY (store_id) REFERENCES stores_new(store_id);
    END IF;

    -- ============================================================
    -- 4. orders.store_id -> stores_new.store_id
    -- ============================================================
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'orders' AND constraint_name = 'fk_orders_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        ALTER TABLE orders DROP CONSTRAINT fk_orders_store_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'orders' AND constraint_name = 'fk_orders_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables WHERE table_name = 'stores_new'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'orders' AND column_name = 'store_id' AND data_type = 'bigint'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT fk_orders_store_id
            FOREIGN KEY (store_id) REFERENCES stores_new(store_id);
    END IF;

    -- ============================================================
    -- 5. purchase_orders.store_id -> stores_new.store_id
    -- ============================================================
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'purchase_orders' AND constraint_name = 'fk_purchase_orders_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) THEN
        ALTER TABLE purchase_orders DROP CONSTRAINT fk_purchase_orders_store_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'purchase_orders' AND constraint_name = 'fk_purchase_orders_store_id'
          AND constraint_type = 'FOREIGN KEY'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables WHERE table_name = 'stores_new'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'purchase_orders' AND column_name = 'store_id' AND data_type = 'bigint'
    ) THEN
        ALTER TABLE purchase_orders
            ADD CONSTRAINT fk_purchase_orders_store_id
            FOREIGN KEY (store_id) REFERENCES stores_new(store_id);
    END IF;
END $$;
