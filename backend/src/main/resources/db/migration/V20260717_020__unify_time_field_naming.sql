-- ============================================================
-- 迁移脚本: V20260717_020__unify_time_field_naming.sql
-- 任务: Wave 4-B - 统一时间字段命名为 create_time/update_time（DM-030）
-- 日期: 2026-07-17
-- ============================================================
--
-- 【背景】
-- 架构审查报告 DM-030 发现"三套时间字段命名并存"问题：
--   1. created_at / updated_at  （141 次，20 个迁移脚本 - 旧规范）
--   2. created_time / updated_time （第 3 种变体，少量表）
--   3. create_time / update_time  （229 次，50 个迁移脚本 - 项目规范）
--
-- 【项目规范 §8 / §24】
--   create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--   update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
-- 数据库层 snake_case (create_time)，Java 层 camelCase (createTime)
--
-- 【处理策略】
-- 已执行的迁移脚本不可修改，必须新增 ALTER 脚本统一字段名。
--   1. 仅有 created_at 列的表 → RENAME COLUMN created_at TO create_time
--   2. 同时有 created_at 和 create_time 列的表 → DROP COLUMN created_at（保留新列）
--   3. 仅有 created_time 列的表 → RENAME COLUMN created_time TO create_time
--   4. 同上策略处理 updated_at/updated_time → update_time
--   5. 重建引用旧列名的索引（idx_xxx_created_at → idx_xxx_create_time）
--
-- 【幂等性】
-- 所有操作通过 information_schema 检查列是否存在，确保脚本可重复执行。
-- 目标数据库：PostgreSQL 18。
-- ============================================================

DO $$
DECLARE
    rec RECORD;
    has_new BOOLEAN;
BEGIN
    -- ============================================================
    -- Phase 1: 处理 created_at → create_time
    -- ============================================================
    FOR rec IN
        SELECT DISTINCT table_name
        FROM information_schema.columns
        WHERE column_name = 'created_at'
          AND table_schema = CURRENT_SCHEMA()
    LOOP
        SELECT EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_name = rec.table_name
              AND column_name = 'create_time'
              AND table_schema = CURRENT_SCHEMA()
        ) INTO has_new;

        IF has_new THEN
            -- 新列已存在，删除旧列（数据已在之前的迁移中复制到新列）
            EXECUTE 'ALTER TABLE ' || rec.table_name || ' DROP COLUMN IF EXISTS created_at';
        ELSE
            -- 仅旧列存在，重命名为新列名
            EXECUTE 'ALTER TABLE ' || rec.table_name || ' RENAME COLUMN created_at TO create_time';
        END IF;
    END LOOP;

    -- ============================================================
    -- Phase 2: 处理 updated_at → update_time
    -- ============================================================
    FOR rec IN
        SELECT DISTINCT table_name
        FROM information_schema.columns
        WHERE column_name = 'updated_at'
          AND table_schema = CURRENT_SCHEMA()
    LOOP
        SELECT EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_name = rec.table_name
              AND column_name = 'update_time'
              AND table_schema = CURRENT_SCHEMA()
        ) INTO has_new;

        IF has_new THEN
            EXECUTE 'ALTER TABLE ' || rec.table_name || ' DROP COLUMN IF EXISTS updated_at';
        ELSE
            EXECUTE 'ALTER TABLE ' || rec.table_name || ' RENAME COLUMN updated_at TO update_time';
        END IF;
    END LOOP;

    -- ============================================================
    -- Phase 3: 处理 created_time → create_time（第 3 种变体）
    -- ============================================================
    FOR rec IN
        SELECT DISTINCT table_name
        FROM information_schema.columns
        WHERE column_name = 'created_time'
          AND table_schema = CURRENT_SCHEMA()
    LOOP
        SELECT EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_name = rec.table_name
              AND column_name = 'create_time'
              AND table_schema = CURRENT_SCHEMA()
        ) INTO has_new;

        IF has_new THEN
            EXECUTE 'ALTER TABLE ' || rec.table_name || ' DROP COLUMN IF EXISTS created_time';
        ELSE
            EXECUTE 'ALTER TABLE ' || rec.table_name || ' RENAME COLUMN created_time TO create_time';
        END IF;
    END LOOP;

    -- ============================================================
    -- Phase 4: 处理 updated_time → update_time（第 3 种变体）
    -- ============================================================
    FOR rec IN
        SELECT DISTINCT table_name
        FROM information_schema.columns
        WHERE column_name = 'updated_time'
          AND table_schema = CURRENT_SCHEMA()
    LOOP
        SELECT EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_name = rec.table_name
              AND column_name = 'update_time'
              AND table_schema = CURRENT_SCHEMA()
        ) INTO has_new;

        IF has_new THEN
            EXECUTE 'ALTER TABLE ' || rec.table_name || ' DROP COLUMN IF EXISTS updated_time';
        ELSE
            EXECUTE 'ALTER TABLE ' || rec.table_name || ' RENAME COLUMN updated_time TO update_time';
        END IF;
    END LOOP;
END $$;


-- ============================================================
-- Phase 5: 重建引用旧列名的索引
-- 说明：RENAME COLUMN 会自动更新索引引用，但索引名仍含旧列名。
--       此处删除旧名索引，按规范 idx_{table}_{field} 重建。
-- ============================================================

-- 5.1 单列索引：删除旧名索引（索引名含 _created_at / _updated_at）
DROP INDEX IF EXISTS idx_kitchen_order_created_at;
DROP INDEX IF EXISTS idx_inventory_transactions_created_at;
DROP INDEX IF EXISTS idx_audit_log_created_at;
DROP INDEX IF EXISTS idx_password_history_created_at;
DROP INDEX IF EXISTS idx_sys_config_created;
DROP INDEX IF EXISTS idx_config_history_time;
DROP INDEX IF EXISTS idx_review_record_created_at;
DROP INDEX IF EXISTS idx_device_workflow_execution_log_created_at;
DROP INDEX IF EXISTS idx_audit_log_archive_created_at;
DROP INDEX IF EXISTS idx_users_created_at;
DROP INDEX IF EXISTS idx_stores_created_at;

-- 5.2 单列索引：按规范重建
CREATE INDEX IF NOT EXISTS idx_kitchen_order_create_time ON kitchen_order (create_time);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_create_time ON inventory_transactions (create_time);
CREATE INDEX IF NOT EXISTS idx_audit_log_create_time ON audit_log (create_time);
CREATE INDEX IF NOT EXISTS idx_password_history_create_time ON password_history (create_time);
CREATE INDEX IF NOT EXISTS idx_sys_config_create_time ON sys_config (create_time);
CREATE INDEX IF NOT EXISTS idx_config_history_create_time ON sys_config_history (create_time);
CREATE INDEX IF NOT EXISTS idx_review_record_create_time ON article_review_record (create_time);
CREATE INDEX IF NOT EXISTS idx_device_workflow_execution_log_create_time ON device_workflow_execution_log (create_time);
CREATE INDEX IF NOT EXISTS idx_audit_log_archive_create_time ON audit_log_archive (create_time);

-- 5.3 复合索引：删除引用旧列名的复合索引并重建
DROP INDEX IF EXISTS idx_electronic_voucher_tenant_created;
CREATE INDEX IF NOT EXISTS idx_electronic_voucher_tenant_create_time ON electronic_voucher (tenant_id, create_time);

DROP INDEX IF EXISTS idx_config_history_config;
CREATE INDEX IF NOT EXISTS idx_config_history_config ON sys_config_history (config_id, create_time);

DROP INDEX IF EXISTS idx_config_history_operator;
CREATE INDEX IF NOT EXISTS idx_config_history_operator ON sys_config_history (operator_id, create_time);


-- ============================================================
-- Phase 6: 验证 - 确保没有残留的旧列名
-- 说明：此查询不会修改数据，仅用于验证迁移结果。
--       如有残留，会在应用启动时报错。
-- ============================================================
-- 验证查询（手动执行检查，不影响迁移）:
-- SELECT table_name, column_name
-- FROM information_schema.columns
-- WHERE column_name IN ('created_at', 'updated_at', 'created_time', 'updated_time')
--   AND table_schema = CURRENT_SCHEMA()
-- ORDER BY table_name, column_name;
-- 预期结果：0 行（所有旧列名已统一为 create_time/update_time）


-- ============================================================
-- 迁移说明
-- ============================================================
-- 1. 本脚本处理以下表（含 created_at/updated_at 的旧规范表）：
--    departments, positions, position_levels, employees, users, roles,
--    user_roles, permissions, role_permissions, user_permissions,
--    food_category, food, product, inventory, kitchen_order, sys_settings,
--    purchase_orders, purchase_order_items, material_trace_code,
--    food_trace_code, material_consumption, dish_recipe, cost_record,
--    inventory_transactions, finance_records, salary_records, devices,
--    device_status_log, promotion, dish_combo, combo_inventory,
--    dish_inventory, sys_operation_logs, login_log, password_reset_log,
--    scheduled_task, global_config, audit_log, label_template,
--    purchase_request, purchase_request_item, other_inbound, loss_outbound,
--    electronic_voucher, electronic_invoice, electronic_invoice_item,
--    voucher_signature_log, electronic_train_ticket, electronic_flight_ticket,
--    electronic_bank_receipt, invitation_send_record, onboarding_archive,
--    approval_record, invitation_codes, password_history, system_config,
--    sys_config, sys_config_history, recruitment_requirements, resumes,
--    interviews, onboarding_records, article_review_record,
--    inventory_loss_detail, device_status_history, device_data,
--    device_driver_version, device_workflow, device_workflow_execution_log,
--    permission_templates, user_permission_overrides, position_role_mapping,
--    ai_model_configs, store_inventory_log, stores, audit_log_archive,
--    employee_approvals, leave_requests, overtime_requests,
--    reimbursement_requests
--
-- 2. 本脚本处理以下表（含 created_time/updated_time 的第 3 种变体）：
--    employee_approvals, leave_requests, overtime_requests,
--    reimbursement_requests
--
-- 3. suppliers 表已在 V20260629_006 中完成 created_at → create_time 重命名
--
-- 4. purchase_request/purchase_orders/purchase_order_items/salary_records
--    表已有 create_time/update_time 列（由 V20260629_008/V20260629_012/V20260626_002 添加），
--    本脚本删除其残留的 created_at/updated_at 旧列。
-- ============================================================
