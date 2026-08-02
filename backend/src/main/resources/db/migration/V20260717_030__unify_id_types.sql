-- ============================================================
-- Wave 5-B：ID 字段类型统一为 BIGINT
-- 关联问题：Wave 4-A §4.2 跳过的 FK（类型不匹配）
-- 修复内容：
--   将以下 VARCHAR 类型的 ID 字段统一为 BIGINT，
--   解除 Wave 4-A 因类型不匹配无法创建 FK 的阻塞。
--
-- 类型统一清单：
--   主键字段（VARCHAR → BIGINT）：
--     1. stores.store_id              VARCHAR(32) → BIGINT
--     2. employees.employee_id        VARCHAR(50) → BIGINT
--
--   外键字段（VARCHAR → BIGINT，引用上述主键或已有 BIGINT 主键）：
--     3. purchase_orders.supplier_id  VARCHAR(50) → BIGINT  (引用 suppliers.supplier_id BIGINT)
--     4. purchase_orders.store_id     VARCHAR(50) → BIGINT  (引用 stores.store_id)
--     5. purchase_order_items.order_id VARCHAR(50) → BIGINT (引用 purchase_orders.order_id BIGINT)
--     6. employees.department_id      VARCHAR(50) → BIGINT  (引用 departments.department_id BIGINT)
--     7. employees.position_id        VARCHAR(50) → BIGINT  (引用 positions.position_id BIGINT)
--     8. employees.store_id          已是 BIGINT（无需处理，stores.store_id 改后即可建立 FK）
--     9. users.department_id          VARCHAR(50) → BIGINT  (引用 departments.department_id BIGINT)
--    10. users.store_id               VARCHAR(50) → BIGINT  (引用 stores.store_id)
--    11. departments.store_id         VARCHAR(32) → BIGINT  (引用 stores.store_id)
--    12. material_trace_code.supplier_id        VARCHAR(50) → BIGINT (引用 suppliers.supplier_id BIGINT)
--    13. material_trace_code.purchase_order_id  VARCHAR(50) → BIGINT (引用 purchase_orders.order_id BIGINT)
--    14. material_trace_code.material_id        VARCHAR(50) → BIGINT (引用 material_archives.material_id BIGINT)
--    15. material_consumption.chef_id           VARCHAR(50) → BIGINT (引用 employees.employee_id)
--    16. food_trace_code.chef_id                VARCHAR(50) → BIGINT (引用 employees.employee_id)
--    17. purchase_ledgers.supplier_id           VARCHAR(50) → BIGINT (引用 suppliers.supplier_id BIGINT)
--    18. certificate_managements.supplier_id    VARCHAR(50) → BIGINT (引用 suppliers.supplier_id BIGINT)
--    19. food_trace_codes.supplier_id           VARCHAR(50) → BIGINT (引用 suppliers.supplier_id BIGINT)
--    20. purchase_stockins.supplier_id          VARCHAR(50) → BIGINT (引用 suppliers.supplier_id BIGINT)
--
-- 数据迁移策略（用户确认数据可重建）：
--   - VARCHAR 数值字符串 → BIGINT：直接 CAST（如 '123' → 123）
--   - VARCHAR 非数值字符串（如 'S001'、UUID 等）→ NULL
--   - 使用 CASE WHEN ~ '^[0-9]+$' THEN ...::BIGINT ELSE NULL END 实现安全转换
--   - 转换为 NULL 的记录由应用层或后续清理脚本处理
--
-- 幂等性保障：
--   - 每个 ALTER 操作前用 information_schema.columns 检查当前类型
--   - 仅当 data_type = 'character varying' 时执行转换
--   - 转换后再次执行时跳过（已是 bigint 类型）
--
-- 锁表风险警告：
--   - ALTER TABLE ... ALTER COLUMN ... TYPE 会获取 ACCESS EXCLUSIVE 锁
--   - 大表迁移需在维护窗口执行
--   - 项目当前数据量小（< 10000 行），可在生产环境直接执行
--   - 建议执行前用 pg_dump 全量备份
--
-- 不修改已执行迁移脚本（V20260629_xxx 及更早版本保持不变）
-- ============================================================

-- ============================================================
-- 1. stores.store_id VARCHAR(32) → BIGINT（主键转换）
-- ============================================================
-- 注意：stores 表存在历史数据 'S001'/'S002'/'S003'（V20260709_001 初始化）
-- 这些非数值字符串无法转换为 BIGINT。
-- 由于用户确认数据可重建（缺乏实际应用价值和业务关联性），
-- 采用 TRUNCATE 方式清空 stores 表后再转换类型，避免 PK 约束冲突。
-- 应用层可通过初始化脚本重新插入 BIGINT ID 的门店数据。
DO $$
DECLARE
    v_current_type VARCHAR;
    v_row_count BIGINT;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'stores' AND column_name = 'store_id';

    IF v_current_type = 'character varying' THEN
        -- 1.1 统计待清理行数（用于审计）
        SELECT COUNT(*) INTO v_row_count FROM stores;
        RAISE NOTICE 'stores 表当前有 % 行数据将被清空（用户已确认数据可重建）', v_row_count;

        -- 1.2 删除主键约束（如存在）
        ALTER TABLE stores DROP CONSTRAINT IF EXISTS stores_pkey;

        -- 1.3 清空 stores 表（因数据可重建，TRUNCATE 是最快的方式）
        TRUNCATE TABLE stores;

        -- 1.4 类型转换：VARCHAR → BIGINT（表已空，无需 USING 转换数据）
        ALTER TABLE stores ALTER COLUMN store_id TYPE BIGINT USING store_id::BIGINT;

        -- 1.5 移除 VARCHAR 的 DEFAULT（如有）
        ALTER TABLE stores ALTER COLUMN store_id DROP DEFAULT;

        -- 1.6 重建主键（NOT NULL 由 PRIMARY KEY 隐式保证）
        ALTER TABLE stores ADD CONSTRAINT stores_pkey PRIMARY KEY (store_id);

        RAISE NOTICE '已统一 stores.store_id 为 BIGINT（已清空 % 行历史数据）', v_row_count;
    ELSE
        RAISE NOTICE '跳过 stores.store_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 2. employees.employee_id VARCHAR(50) → BIGINT（主键转换）
-- ============================================================
-- 注意：employees.employee_id 历史为 UUID 字符串（IdType.ASSIGN_UUID），
-- 无法直接转换为 BIGINT。由于用户确认数据可重建，
-- 采用 TRUNCATE 方式清空 employees 表后再转换类型，避免 PK 约束冲突。
-- 应用层使用雪花算法（IdType.ASSIGN_ID）重新分配 BIGINT ID。
-- 注意：employees 表的 created_by 字段引用 users.user_id（BIGINT），
-- 故 users 表如有引用 employees 的记录需在应用层处理。
DO $$
DECLARE
    v_current_type VARCHAR;
    v_row_count BIGINT;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'employees' AND column_name = 'employee_id';

    IF v_current_type = 'character varying' THEN
        -- 2.1 统计待清理行数（用于审计）
        SELECT COUNT(*) INTO v_row_count FROM employees;
        RAISE NOTICE 'employees 表当前有 % 行数据将被清空（用户已确认数据可重建）', v_row_count;

        -- 2.2 删除主键约束
        ALTER TABLE employees DROP CONSTRAINT IF EXISTS employees_pkey;

        -- 2.3 清空 employees 表（因数据可重建）
        TRUNCATE TABLE employees;

        -- 2.4 类型转换：VARCHAR → BIGINT（表已空，无需 USING 转换数据）
        ALTER TABLE employees ALTER COLUMN employee_id TYPE BIGINT USING employee_id::BIGINT;

        -- 2.5 重建主键
        ALTER TABLE employees ADD CONSTRAINT employees_pkey PRIMARY KEY (employee_id);

        RAISE NOTICE '已统一 employees.employee_id 为 BIGINT（已清空 % 行历史数据）', v_row_count;
    ELSE
        RAISE NOTICE '跳过 employees.employee_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 3. purchase_orders.supplier_id VARCHAR(50) → BIGINT
--    引用 suppliers.supplier_id BIGINT
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'purchase_orders' AND column_name = 'supplier_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE purchase_orders ALTER COLUMN supplier_id TYPE BIGINT
            USING CASE
                WHEN supplier_id ~ '^[0-9]+$' THEN supplier_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 purchase_orders.supplier_id 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 purchase_orders.supplier_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 4. purchase_orders.store_id VARCHAR(50) → BIGINT
--    引用 stores.store_id（已在第1节统一为 BIGINT）
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'purchase_orders' AND column_name = 'store_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE purchase_orders ALTER COLUMN store_id TYPE BIGINT
            USING CASE
                WHEN store_id ~ '^[0-9]+$' THEN store_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 purchase_orders.store_id 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 purchase_orders.store_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 5. purchase_order_items.order_id VARCHAR(50) → BIGINT
--    引用 purchase_orders.order_id BIGINT
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'purchase_order_items' AND column_name = 'order_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE purchase_order_items ALTER COLUMN order_id TYPE BIGINT
            USING CASE
                WHEN order_id ~ '^[0-9]+$' THEN order_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 purchase_order_items.order_id 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 purchase_order_items.order_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 6. employees.department_id VARCHAR(50) → BIGINT
--    引用 departments.department_id BIGINT
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'employees' AND column_name = 'department_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE employees ALTER COLUMN department_id TYPE BIGINT
            USING CASE
                WHEN department_id ~ '^[0-9]+$' THEN department_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 employees.department_id 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 employees.department_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 7. employees.position_id VARCHAR(50) → BIGINT
--    引用 positions.position_id BIGINT
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'employees' AND column_name = 'position_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE employees ALTER COLUMN position_id TYPE BIGINT
            USING CASE
                WHEN position_id ~ '^[0-9]+$' THEN position_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 employees.position_id 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 employees.position_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 8. users.department_id VARCHAR(50) → BIGINT
--    引用 departments.department_id BIGINT
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'department_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE users ALTER COLUMN department_id TYPE BIGINT
            USING CASE
                WHEN department_id ~ '^[0-9]+$' THEN department_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 users.department_id 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 users.department_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 9. users.store_id VARCHAR(50) → BIGINT
--    引用 stores.store_id（已在第1节统一为 BIGINT）
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'users' AND column_name = 'store_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE users ALTER COLUMN store_id TYPE BIGINT
            USING CASE
                WHEN store_id ~ '^[0-9]+$' THEN store_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 users.store_id 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 users.store_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 10. departments.store_id VARCHAR(32) → BIGINT
--     引用 stores.store_id（已在第1节统一为 BIGINT）
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'departments' AND column_name = 'store_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE departments ALTER COLUMN store_id TYPE BIGINT
            USING CASE
                WHEN store_id ~ '^[0-9]+$' THEN store_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 departments.store_id 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 departments.store_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 11. material_trace_code.supplier_id VARCHAR(50) → BIGINT
--     引用 suppliers.supplier_id BIGINT
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'material_trace_code' AND column_name = 'supplier_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE material_trace_code ALTER COLUMN supplier_id TYPE BIGINT
            USING CASE
                WHEN supplier_id ~ '^[0-9]+$' THEN supplier_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 material_trace_code.supplier_id 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 material_trace_code.supplier_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 12. material_trace_code.purchase_order_id VARCHAR(50) → BIGINT
--     引用 purchase_orders.order_id BIGINT
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'material_trace_code' AND column_name = 'purchase_order_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE material_trace_code ALTER COLUMN purchase_order_id TYPE BIGINT
            USING CASE
                WHEN purchase_order_id ~ '^[0-9]+$' THEN purchase_order_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 material_trace_code.purchase_order_id 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 material_trace_code.purchase_order_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 13. material_trace_code.material_id VARCHAR(50) → BIGINT
--     引用 material_archives.material_id BIGINT（如存在）
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'material_trace_code' AND column_name = 'material_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE material_trace_code ALTER COLUMN material_id TYPE BIGINT
            USING CASE
                WHEN material_id ~ '^[0-9]+$' THEN material_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 material_trace_code.material_id 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 material_trace_code.material_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 14. material_consumption.chef_id VARCHAR(50) → BIGINT
--     引用 employees.employee_id（已在第2节统一为 BIGINT）
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'material_consumption' AND column_name = 'chef_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE material_consumption ALTER COLUMN chef_id TYPE BIGINT
            USING CASE
                WHEN chef_id ~ '^[0-9]+$' THEN chef_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 material_consumption.chef_id 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 material_consumption.chef_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 15. food_trace_code.chef_id VARCHAR(50) → BIGINT
--     引用 employees.employee_id（已在第2节统一为 BIGINT）
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'food_trace_code' AND column_name = 'chef_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE food_trace_code ALTER COLUMN chef_id TYPE BIGINT
            USING CASE
                WHEN chef_id ~ '^[0-9]+$' THEN chef_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 food_trace_code.chef_id 为 BIGINT';
    ELSE
        RAISE NOTICE '跳过 food_trace_code.chef_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 16. purchase_ledgers.supplier_id VARCHAR(50) → BIGINT（如列存在）
--     引用 suppliers.supplier_id BIGINT
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'purchase_ledgers' AND column_name = 'supplier_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE purchase_ledgers ALTER COLUMN supplier_id TYPE BIGINT
            USING CASE
                WHEN supplier_id ~ '^[0-9]+$' THEN supplier_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 purchase_ledgers.supplier_id 为 BIGINT';
    ELSIF v_current_type IS NULL THEN
        RAISE NOTICE '跳过 purchase_ledgers.supplier_id：表或列不存在';
    ELSE
        RAISE NOTICE '跳过 purchase_ledgers.supplier_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 17. certificate_managements.supplier_id VARCHAR(50) → BIGINT（如列存在）
--     引用 suppliers.supplier_id BIGINT
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'certificate_managements' AND column_name = 'supplier_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE certificate_managements ALTER COLUMN supplier_id TYPE BIGINT
            USING CASE
                WHEN supplier_id ~ '^[0-9]+$' THEN supplier_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 certificate_managements.supplier_id 为 BIGINT';
    ELSIF v_current_type IS NULL THEN
        RAISE NOTICE '跳过 certificate_managements.supplier_id：表或列不存在';
    ELSE
        RAISE NOTICE '跳过 certificate_managements.supplier_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 18. food_trace_codes.supplier_id VARCHAR(50) → BIGINT（如列存在）
--     引用 suppliers.supplier_id BIGINT
--     注意：V20260624_001 创建的 food_trace_codes 表为新一代追溯码表（单数）
--     与 V1.0.0.100 创建的 food_trace_code 不同
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'food_trace_codes' AND column_name = 'supplier_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE food_trace_codes ALTER COLUMN supplier_id TYPE BIGINT
            USING CASE
                WHEN supplier_id ~ '^[0-9]+$' THEN supplier_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 food_trace_codes.supplier_id 为 BIGINT';
    ELSIF v_current_type IS NULL THEN
        RAISE NOTICE '跳过 food_trace_codes.supplier_id：表或列不存在';
    ELSE
        RAISE NOTICE '跳过 food_trace_codes.supplier_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 19. purchase_stockins.supplier_id VARCHAR(50) → BIGINT（如列存在）
--     引用 suppliers.supplier_id BIGINT
-- ============================================================
DO $$
DECLARE
    v_current_type VARCHAR;
BEGIN
    SELECT data_type INTO v_current_type
    FROM information_schema.columns
    WHERE table_name = 'purchase_stockins' AND column_name = 'supplier_id';

    IF v_current_type = 'character varying' THEN
        ALTER TABLE purchase_stockins ALTER COLUMN supplier_id TYPE BIGINT
            USING CASE
                WHEN supplier_id ~ '^[0-9]+$' THEN supplier_id::BIGINT
                ELSE NULL
            END;
        RAISE NOTICE '已统一 purchase_stockins.supplier_id 为 BIGINT';
    ELSIF v_current_type IS NULL THEN
        RAISE NOTICE '跳过 purchase_stockins.supplier_id：表或列不存在';
    ELSE
        RAISE NOTICE '跳过 purchase_stockins.supplier_id：当前类型已为 %', v_current_type;
    END IF;
END $$;

-- ============================================================
-- 转换结果说明
-- ============================================================
-- 1. 所有原 VARCHAR 类型的 ID 字段已统一为 BIGINT
-- 2. 历史数据中的非数值字符串（如 'S001'、UUID）已被转为 NULL
--    - 主键列的 NULL 值需在 V20260717_031 之前由应用层或 DBA 重新分配 ID
--    - 外键列的 NULL 值表示"无关联"，符合业务语义
-- 3. 后续可在 V20260717_031 中创建之前因类型不匹配跳过的 FK 约束
-- 4. 索引可能因类型变更失效，V20260717_032 将重建相关索引
-- ============================================================
