-- ============================================================
-- P1-LOCATION-MODEL-001 M3-M4 统一账本迁移（草案 DRAFT）
-- ⚠️ 本文件当前位于 m3m4-preflight/（不参与 Flyway）。
-- ⚠️ S8（代码层就绪）完成后，移入 resources/db/migration/ 并在停机窗口应用。
--      （后端运行中 Flyway 随启动自动应用——提前入目录会导致表更名打断在线系统）
-- 设计依据：location-organization-separation-design-001.md §1.3/§1.4 + -002.md
-- 数据裁定：见 implementation-record-002-m3m4.md §2（裁定日志）
-- 回滚：DOWN 段落（见文件尾）+ backups/m3m4-predump-20260927.dump
-- ============================================================

-- ---------- 1. legacy 更名（观察期保留，M7 退役） ----------
ALTER TABLE inventory            RENAME TO inventory_legacy;
ALTER TABLE store_inventory      RENAME TO store_inventory_legacy;
ALTER TABLE store_inventory_log  RENAME TO store_inventory_log_legacy;
ALTER TABLE inventory_transactions RENAME TO inventory_transactions_legacy;
ALTER TABLE inventory_log        RENAME TO inventory_log_legacy;

-- ---------- 2. 统一库存账（-001 §1.3） ----------
CREATE TABLE inventory (
    inventory_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    location_id     BIGINT NOT NULL REFERENCES locations(location_id),
    material_id     BIGINT NOT NULL,
    material_name   VARCHAR(200),
    material_code   VARCHAR(64),
    material_category_id   BIGINT,
    material_category_name VARCHAR(100),
    specification   VARCHAR(200),
    quantity        NUMERIC(14,4) NOT NULL DEFAULT 0,
    unit            VARCHAR(20),
    safety_stock    NUMERIC(14,4),                    -- 吸收 T1 门店安全线
    max_stock       NUMERIC(14,4),
    production_date DATE,
    min_safe_qty    NUMERIC(14,4),                    -- 吸收 T2（行为保留列，矩阵 §7）
    locked_quantity NUMERIC(14,4) NOT NULL DEFAULT 0, -- 吸收 T2 锁定语义
    min_safe_qty    NUMERIC(14,4),                    -- 吸收 T2 预警阈值（getLowStockList 现状迁移）
    status          SMALLINT NOT NULL DEFAULT 1,      -- 吸收 T2（预警调度器维护 1/2/3/4）
    expiry_date     DATE,                             -- 吸收 T2 临期/过期语义
    batch_no        VARCHAR(64),                      -- 可空，不进唯一键（D-3 单行化）
    unit_cost       NUMERIC(14,4),
    total_cost      NUMERIC(16,4),
    version         BIGINT NOT NULL DEFAULT 0,
    deleted         SMALLINT NOT NULL DEFAULT 0,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_inventory_location_material UNIQUE (location_id, material_id)
);
CREATE INDEX idx_inventory_location ON inventory(location_id);
CREATE INDEX idx_inventory_material ON inventory(material_id);
CREATE INDEX idx_inventory_expiry   ON inventory(expiry_date);

-- ---------- 3. 统一流水（-001 §1.4，T3+T4a+T4b → 一张） ----------
CREATE TABLE inventory_movement (
    movement_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    location_id   BIGINT NOT NULL,
    material_id   BIGINT NOT NULL,
    change_qty    NUMERIC(14,4) NOT NULL,
    balance_after NUMERIC(14,4),
    movement_type VARCHAR(30) NOT NULL,   -- IN / OUT / TRANSFER_OUT / TRANSFER_IN / ADJUST ...
    source_type   VARCHAR(40) NOT NULL,   -- PURCHASE_STOCKIN / RECEIPT_CONFIRM / KDS_DEDUCT / ... / MIGRATED_WAREHOUSE_LEGACY / MIGRATED_STORE_LEGACY
    source_ref    VARCHAR(64)  NOT NULL,
    operator_id   BIGINT,
    unit_cost     NUMERIC(14,4),
    total_cost    NUMERIC(16,4),
    remark        VARCHAR(500),
    create_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_movement_source CHECK (length(source_type) > 0 AND length(source_ref) > 0)
);
CREATE INDEX idx_mv_location_time ON inventory_movement(location_id, create_time);
CREATE INDEX idx_mv_source        ON inventory_movement(source_type, source_ref);

-- ---------- 4. 数据迁移（裁定日志见 implementation-record-002 §2） ----------
-- 4a. T2 仓库账 → 统一账（13 行：经 map；剔除 NULL 仓行与 999999 虚账；门店账起账不迁）
INSERT INTO inventory (location_id, material_id, material_name, material_code,
                       material_category_id, material_category_name, specification,
                       quantity, unit, safety_stock, max_stock,
                       min_safe_qty, locked_quantity, status, expiry_date, production_date,
                       batch_no, unit_cost, version)
SELECT m.location_id, i.material_id, i.material_name, i.material_code,
       i.material_category_id, i.material_category_name, i.specification,
       i.current_stock, i.unit, i.safety_stock, i.max_stock_qty,
       i.min_safe_qty, i.locked_quantity, i.status, i.expiry_date, i.production_date,
       i.batch_no, i.unit_cost, i.version
FROM inventory_legacy i
JOIN location_id_map m ON m.src_table = 'warehouses' AND m.src_id = i.warehouse_id
WHERE i.deleted = 0
  AND i.material_id <> 999999;

-- 4b. T4a 仓库流水 → 统一流水（15 行：wh1/wh2 经 map；584 污染 / 测试仓 / NULL 剔除）
INSERT INTO inventory_movement (location_id, material_id, change_qty, balance_after,
                                movement_type, source_type, source_ref,
                                operator_id, unit_cost, total_cost, create_time)
SELECT m.location_id, t.material_id, t.quantity_change, t.after_qty,
       CASE WHEN t.transaction_type = 1 THEN 'IN' ELSE 'OUT' END,
       'MIGRATED_WAREHOUSE_LEGACY',
       COALESCE(t.reference_no, 'TXN-' || t.transaction_id),
       t.create_user_id, t.unit_cost, t.total_cost, t.create_time
FROM inventory_transactions_legacy t
JOIN location_id_map m ON m.src_table = 'warehouses' AND m.src_id = t.warehouse_id
WHERE t.deleted = 0;

-- 4c. T3 门店流水：全部不迁（D-4 门店账全清起账；Q2 待批复项数据随账清除）
-- 4d. T4b inventory_log：0 行，无迁移
-- （对照断言见实施记录 §3；执行后校验：inventory=13 行 / movement=15 行）

-- ---------- 5. 应用后校验（停机窗口内执行） ----------
-- SELECT count(*) FROM inventory;                 -- 预期 13
-- SELECT count(*) FROM inventory_movement;        -- 预期 15
-- SELECT count(*) FROM inventory i LEFT JOIN locations l ON l.location_id=i.location_id WHERE l.id IS NULL;  -- 预期 0

-- ---------- DOWN（回滚段，仅演练/异常时经 Owner 批准执行） ----------
-- DROP TABLE inventory_movement;
-- DROP TABLE inventory;
-- ALTER TABLE inventory_log_legacy          RENAME TO inventory_log;
-- ALTER TABLE inventory_transactions_legacy RENAME TO inventory_transactions;
-- ALTER TABLE store_inventory_log_legacy    RENAME TO store_inventory_log;
-- ALTER TABLE store_inventory_legacy        RENAME TO store_inventory;
-- ALTER TABLE inventory_legacy              RENAME TO inventory;
