-- ============================================================
-- 盘点表建表脚本
-- 对应实体: InventoryCheck (inventory_checks)、InventoryCheckItem (inventory_check_items)
-- 对应初始化器: InventoryDatabaseInitializer.createInventoryChecksTable() / createInventoryCheckItemsTable()
--
-- 注意：H2 开发环境由 InventoryDatabaseInitializer 建表，
--       本脚本仅在生产环境（PostgreSQL）执行。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 盘点单主表
-- 用途：记录仓库的盘点操作
-- 状态流转：0 待执行 → 1 盘点中 → 2 已完成 → 3 已取消
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_checks (
    check_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    check_code         VARCHAR(50)  NOT NULL,
    warehouse_id       BIGINT,
    check_type         INTEGER,
    check_status       INTEGER      DEFAULT 0,
    check_date         DATE,
    complete_date      DATE,
    create_user_id    BIGINT,
    approve_user_id   BIGINT,
    approve_time       TIMESTAMP,
    remark             VARCHAR(500),
    create_time        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE  inventory_checks IS '盘点单主表';
COMMENT ON COLUMN inventory_checks.check_id IS '盘点单ID（主键，自增）';
COMMENT ON COLUMN inventory_checks.check_code IS '盘点单号（唯一）';
COMMENT ON COLUMN inventory_checks.warehouse_id IS '仓库ID';
COMMENT ON COLUMN inventory_checks.check_type IS '盘点类型：1全盘 2部分盘';
COMMENT ON COLUMN inventory_checks.check_status IS '盘点状态：0待执行 1盘点中 2已完成 3已取消';
COMMENT ON COLUMN inventory_checks.check_date IS '盘点日期';
COMMENT ON COLUMN inventory_checks.complete_date IS '完成日期';
COMMENT ON COLUMN inventory_checks.create_user_id IS '创建人ID';
COMMENT ON COLUMN inventory_checks.approve_user_id IS '审批人ID';
COMMENT ON COLUMN inventory_checks.approve_time IS '审批时间';
COMMENT ON COLUMN inventory_checks.remark IS '备注';
COMMENT ON COLUMN inventory_checks.create_time IS '创建时间';
COMMENT ON COLUMN inventory_checks.update_time IS '更新时间';
COMMENT ON COLUMN inventory_checks.deleted IS '逻辑删除：0未删除 1已删除';

-- 唯一索引：盘点单号
CREATE UNIQUE INDEX IF NOT EXISTS uk_inventory_checks_check_code ON inventory_checks (check_code);
-- 普通索引：按仓库/状态/日期查询
CREATE INDEX IF NOT EXISTS idx_inventory_checks_warehouse_id ON inventory_checks (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_checks_check_status ON inventory_checks (check_status);
CREATE INDEX IF NOT EXISTS idx_inventory_checks_check_date  ON inventory_checks (check_date);

-- ------------------------------------------------------------
-- 2. 盘点明细表
-- 用途：记录每个物料在盘点时的账面数量、实际数量和差异
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_check_items (
    check_item_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    check_id       BIGINT       NOT NULL,
    inventory_id   BIGINT,
    book_qty       DECIMAL(12, 2),
    actual_qty     DECIMAL(12, 2),
    diff_qty       DECIMAL(12, 2),
    diff_amount    BIGINT,
    reason         VARCHAR(500),
    create_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE  inventory_check_items IS '盘点明细表';
COMMENT ON COLUMN inventory_check_items.check_item_id IS '盘点明细ID（主键，自增）';
COMMENT ON COLUMN inventory_check_items.check_id IS '所属盘点单ID';
COMMENT ON COLUMN inventory_check_items.inventory_id IS '库存记录ID';
COMMENT ON COLUMN inventory_check_items.book_qty IS '账面数量';
COMMENT ON COLUMN inventory_check_items.actual_qty IS '实际数量';
COMMENT ON COLUMN inventory_check_items.diff_qty IS '差异量（实际 - 账面）';
COMMENT ON COLUMN inventory_check_items.diff_amount IS '差异金额（分）';
COMMENT ON COLUMN inventory_check_items.reason IS '差异原因';
COMMENT ON COLUMN inventory_check_items.create_time IS '创建时间';
COMMENT ON COLUMN inventory_check_items.update_time IS '更新时间';
COMMENT ON COLUMN inventory_check_items.deleted IS '逻辑删除：0未删除 1已删除';

-- 普通索引：按盘点单/库存ID查询
CREATE INDEX IF NOT EXISTS idx_inventory_check_items_check_id      ON inventory_check_items (check_id);
CREATE INDEX IF NOT EXISTS idx_inventory_check_items_inventory_id  ON inventory_check_items (inventory_id);
