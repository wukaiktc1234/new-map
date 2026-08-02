-- ============================================================
-- V20260723_005: 重建库存流水表以匹配新版实体
-- ============================================================
-- 背景：
--   旧版 inventory_transactions 表（V1.0.0.100）字段与实体 InventoryTransaction
--   严重不一致：缺少 warehouse_id / quantity_change / before_qty / after_qty /
--   unit_cost / total_cost / reference_no / reference_type / create_user_id / remark 等字段，
--   且主键、字段类型均不兼容，导致仓库/门店收货确认时记录库存流水失败。
--
-- 处理策略：
--   1. 若表仍保持旧版结构（存在 id 列），则删除旧表及索引
--   2. 按新版实体创建表结构
--   3. 创建规范索引
--   4. 本迁移会丢失历史流水数据；测试环境无价值流水，生产环境需单独评估
-- ============================================================

-- 删除旧版索引（若存在）
DROP INDEX IF EXISTS idx_inventory_transactions_inventory_id;
DROP INDEX IF EXISTS idx_inventory_transactions_transaction_type;
DROP INDEX IF EXISTS idx_inventory_transactions_business_id;
DROP INDEX IF EXISTS idx_inventory_transactions_created_at;
DROP INDEX IF EXISTS idx_inventory_transactions_create_time;
DROP INDEX IF EXISTS idx_inventory_transactions_warehouse_id;
DROP INDEX IF EXISTS idx_inventory_transactions_material_id;
DROP INDEX IF EXISTS idx_inventory_transactions_reference_no;

-- 若表为旧版结构（存在 id 列），直接删除旧表
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'inventory_transactions'
          AND column_name = 'id'
          AND table_schema = CURRENT_SCHEMA()
    ) THEN
        DROP TABLE inventory_transactions;
    END IF;
END $$;

-- 创建新版库存流水表（与 InventoryTransaction 实体一致）
CREATE TABLE IF NOT EXISTS inventory_transactions (
    transaction_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_type INT,
    inventory_id     BIGINT,
    material_id      BIGINT,
    warehouse_id     BIGINT,
    quantity_change  DECIMAL(12,2),
    before_qty       DECIMAL(12,2),
    after_qty        DECIMAL(12,2),
    unit_cost        BIGINT,
    total_cost       BIGINT,
    reference_no     VARCHAR(100),
    reference_type   VARCHAR(50),
    create_user_id   BIGINT,
    create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark           VARCHAR(500),
    deleted          INT NOT NULL DEFAULT 0
);

-- 规范索引
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_inventory_id     ON inventory_transactions (inventory_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_material_id      ON inventory_transactions (material_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_warehouse_id     ON inventory_transactions (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_transaction_type ON inventory_transactions (transaction_type);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_reference_no     ON inventory_transactions (reference_no);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_create_time      ON inventory_transactions (create_time);

-- 字段注释
COMMENT ON COLUMN inventory_transactions.transaction_id   IS '变动记录ID（自增主键）';
COMMENT ON COLUMN inventory_transactions.transaction_type IS '变动类型（1:采购入库 2:销售出库 3:调拨出 4:调拨入 5:盘点盈 6:盘点亏 7:报损 8:退货）';
COMMENT ON COLUMN inventory_transactions.inventory_id     IS '库存ID';
COMMENT ON COLUMN inventory_transactions.material_id      IS '物料ID';
COMMENT ON COLUMN inventory_transactions.warehouse_id     IS '仓库ID';
COMMENT ON COLUMN inventory_transactions.quantity_change  IS '变动数量（正数为增加，负数为减少）';
COMMENT ON COLUMN inventory_transactions.before_qty       IS '变动前数量';
COMMENT ON COLUMN inventory_transactions.after_qty        IS '变动后数量';
COMMENT ON COLUMN inventory_transactions.unit_cost        IS '单位成本（分）';
COMMENT ON COLUMN inventory_transactions.total_cost       IS '总成本（分）';
COMMENT ON COLUMN inventory_transactions.reference_no     IS '关联单据号';
COMMENT ON COLUMN inventory_transactions.reference_type   IS '关联单据类型';
COMMENT ON COLUMN inventory_transactions.create_user_id   IS '操作人ID';
COMMENT ON COLUMN inventory_transactions.create_time      IS '创建时间';
COMMENT ON COLUMN inventory_transactions.remark           IS '备注';
COMMENT ON COLUMN inventory_transactions.deleted          IS '逻辑删除标记（0:未删除 1:已删除）';
