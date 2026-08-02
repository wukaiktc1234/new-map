-- ============================================================
-- Wave 5-B：类型变更后重建相关索引
-- 关联问题：V20260717_030 类型变更可能导致索引失效
-- 前置依赖：V20260717_030__unify_id_types.sql + V20260717_031__add_missing_foreign_keys.sql
--
-- 背景：
--   PostgreSQL 在 ALTER COLUMN TYPE 时会自动重建依赖于该列的索引，
--   但以下场景可能需要显式处理：
--   1. 部分索引可能因类型变更失效
--   2. 类型变更后查询性能可能下降（BIGINT 索引 vs VARCHAR 索引选择性不同）
--   3. 某些组合索引可能需要重建以适应新类型
--
-- 处理策略：
--   1. 显式 REINDEX 关键表（仅在数据量小的情况下安全）
--   2. 重建受影响的组合索引
--   3. 为新增 FK 添加索引（FK 引用列需索引以提升 JOIN 性能）
--
-- 锁表风险警告：
--   - REINDEX 会获取 SHARE 锁，阻塞写入但不阻塞读取
--   - 大表建议在维护窗口使用 REINDEX CONCURRENTLY
--   - 项目当前数据量小，可直接 REINDEX
-- ============================================================

-- ============================================================
-- 1. 为新增 FK 引用列添加索引（V20260717_031 新增的 FK）
-- ============================================================

-- 1.1 orders.store_id 索引（如不存在）
--     V20260629_003 已创建 idx_orders_store_id，此处幂等补齐
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_orders_store_id'
    ) THEN
        CREATE INDEX idx_orders_store_id ON orders(store_id);
        RAISE NOTICE '已创建 idx_orders_store_id';
    ELSE
        RAISE NOTICE '跳过 idx_orders_store_id：已存在';
    END IF;
END $$;

-- 1.2 employees.store_id 索引（如不存在）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_employees_store_id'
    ) THEN
        CREATE INDEX idx_employees_store_id ON employees(store_id);
        RAISE NOTICE '已创建 idx_employees_store_id';
    ELSE
        RAISE NOTICE '跳过 idx_employees_store_id：已存在';
    END IF;
END $$;

-- 1.3 users.store_id 索引（如不存在）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_users_store_id'
    ) THEN
        CREATE INDEX idx_users_store_id ON users(store_id);
        RAISE NOTICE '已创建 idx_users_store_id';
    ELSE
        RAISE NOTICE '跳过 idx_users_store_id：已存在';
    END IF;
END $$;

-- 1.4 departments.store_id 索引（如不存在）
--     V20260701_004 已创建 idx_departments_store_id，此处幂等补齐
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_departments_store_id'
    ) THEN
        CREATE INDEX idx_departments_store_id ON departments(store_id);
        RAISE NOTICE '已创建 idx_departments_store_id';
    ELSE
        RAISE NOTICE '跳过 idx_departments_store_id：已存在';
    END IF;
END $$;

-- 1.5 purchase_orders.store_id 索引（如不存在）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_purchase_orders_store_id'
    ) THEN
        CREATE INDEX idx_purchase_orders_store_id ON purchase_orders(store_id);
        RAISE NOTICE '已创建 idx_purchase_orders_store_id';
    ELSE
        RAISE NOTICE '跳过 idx_purchase_orders_store_id：已存在';
    END IF;
END $$;

-- 1.6 material_consumption.chef_id 索引（如不存在）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_material_consumption_chef_id'
    ) THEN
        CREATE INDEX idx_material_consumption_chef_id ON material_consumption(chef_id);
        RAISE NOTICE '已创建 idx_material_consumption_chef_id';
    ELSE
        RAISE NOTICE '跳过 idx_material_consumption_chef_id：已存在';
    END IF;
END $$;

-- 1.7 food_trace_code.chef_id 索引（如不存在）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_food_trace_code_chef_id'
    ) THEN
        CREATE INDEX idx_food_trace_code_chef_id ON food_trace_code(chef_id);
        RAISE NOTICE '已创建 idx_food_trace_code_chef_id';
    ELSE
        RAISE NOTICE '跳过 idx_food_trace_code_chef_id：已存在';
    END IF;
END $$;

-- 1.8 material_trace_code.supplier_id 索引（如不存在）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_material_trace_code_supplier_id'
    ) THEN
        CREATE INDEX idx_material_trace_code_supplier_id ON material_trace_code(supplier_id);
        RAISE NOTICE '已创建 idx_material_trace_code_supplier_id';
    ELSE
        RAISE NOTICE '跳过 idx_material_trace_code_supplier_id：已存在';
    END IF;
END $$;

-- 1.9 material_trace_code.purchase_order_id 索引（如不存在）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_material_trace_code_purchase_order_id'
    ) THEN
        CREATE INDEX idx_material_trace_code_purchase_order_id ON material_trace_code(purchase_order_id);
        RAISE NOTICE '已创建 idx_material_trace_code_purchase_order_id';
    ELSE
        RAISE NOTICE '跳过 idx_material_trace_code_purchase_order_id：已存在';
    END IF;
END $$;

-- ============================================================
-- 2. 类型变更后的索引状态检查与重建
-- ============================================================
-- PostgreSQL 在 ALTER COLUMN TYPE 时会自动重建依赖索引，
-- 但为了确保性能，对关键表执行 REINDEX（数据量小，安全）

-- 2.1 REINDEX stores 表（store_id 主键索引 + idx_stores_status + idx_stores_company_id）
-- 注意：REINDEX TABLE 在事务中执行，会锁表直至完成
DO $$
BEGIN
    -- 仅在表存在时执行（防御性检查）
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'stores') THEN
        -- REINDEX 不能在 DO 块内直接执行（REINDEX 是 utility command）
        -- 此处仅记录提示，实际 REINDEX 在 DO 块外执行
        RAISE NOTICE 'stores 表存在，建议执行 REINDEX TABLE stores;';
    END IF;
END $$;

-- 实际执行 REINDEX（幂等：REINDEX 总是安全的，重复执行无副作用）
-- 注意：REINDEX 在 PostgreSQL 13+ 支持 CONCURRENTLY，但需在事务外执行
-- Flyway 事务内执行，故使用普通 REINDEX
REINDEX TABLE stores;
REINDEX TABLE employees;
REINDEX TABLE purchase_orders;
REINDEX TABLE purchase_order_items;
REINDEX TABLE users;
REINDEX TABLE departments;
REINDEX TABLE material_trace_code;
REINDEX TABLE material_consumption;
REINDEX TABLE food_trace_code;

-- ============================================================
-- 3. 收集统计信息（提升查询计划质量）
-- ============================================================
-- 类型变更后，PostgreSQL 的统计信息可能过时，影响查询计划
-- 执行 ANALYZE 重新收集统计信息
ANALYZE stores;
ANALYZE employees;
ANALYZE purchase_orders;
ANALYZE purchase_order_items;
ANALYZE users;
ANALYZE departments;
ANALYZE material_trace_code;
ANALYZE material_consumption;
ANALYZE food_trace_code;

-- ============================================================
-- 完成说明
-- ============================================================
-- 1. 已为 V20260717_031 新增的 FK 引用列添加索引（9 个）
-- 2. 已对 9 个类型变更的表执行 REINDEX（确保索引健康）
-- 3. 已对 9 个表执行 ANALYZE（更新统计信息）
-- 4. 后续查询性能应回到正常水平，无需额外优化
-- ============================================================
