-- ============================================================
-- 采购入库单子模块建表脚本
-- 对应实体: PurchaseStockin (purchase_stockins) / PurchaseStockinItem (purchase_stockin_items)
-- 对应前端 API: /v1/purchase/stockins
--
-- 状态编码（数据库 INTEGER）：
--   status:               0=待入库 1=已入库 2=部分入库
--   stockin_type:         1=正常入库 2=退货入库 3=赠品入库
--   quality_check_result: 1=合格 2=不合格 3=待检
-- 金额单位：数据库存储分（BIGINT），前端显示元（number）
--
-- 注意：H2 开发环境由 PurchaseDatabaseInitializer.createPurchaseStockinTable /
--       createPurchaseStockinItemTable 建表，本脚本仅在生产环境（PostgreSQL）执行。
-- ============================================================

-- 1. 采购入库单主表
CREATE TABLE IF NOT EXISTS purchase_stockins (
    stockin_id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    stockin_code          VARCHAR(50),
    order_id              BIGINT,
    supplier_id           BIGINT,
    warehouse_id          BIGINT,
    stockin_date          DATE,
    stockin_type          INTEGER,
    total_quantity        DECIMAL(12,3),
    total_amount          BIGINT         DEFAULT 0,
    quality_check_result  INTEGER,
    quality_check_user_id BIGINT,
    quality_check_time    TIMESTAMP,
    quality_remark        VARCHAR(500),
    status                INTEGER        NOT NULL DEFAULT 0,
    create_user_id        BIGINT,
    create_time           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               INTEGER        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  purchase_stockins IS '采购入库单主表';
COMMENT ON COLUMN purchase_stockins.stockin_id IS '入库单主键ID（自增）';
COMMENT ON COLUMN purchase_stockins.stockin_code IS '入库单编号（如 SI20260425001）';
COMMENT ON COLUMN purchase_stockins.order_id IS '关联采购订单ID';
COMMENT ON COLUMN purchase_stockins.supplier_id IS '供应商ID';
COMMENT ON COLUMN purchase_stockins.warehouse_id IS '入库仓库ID';
COMMENT ON COLUMN purchase_stockins.stockin_date IS '入库日期';
COMMENT ON COLUMN purchase_stockins.stockin_type IS '入库类型：1正常入库 2退货入库 3赠品入库';
COMMENT ON COLUMN purchase_stockins.total_quantity IS '总数量';
COMMENT ON COLUMN purchase_stockins.total_amount IS '总金额（单位：分）';
COMMENT ON COLUMN purchase_stockins.quality_check_result IS '质检结果：1合格 2不合格 3待检';
COMMENT ON COLUMN purchase_stockins.quality_check_user_id IS '质检人ID';
COMMENT ON COLUMN purchase_stockins.quality_check_time IS '质检时间';
COMMENT ON COLUMN purchase_stockins.quality_remark IS '质检备注';
COMMENT ON COLUMN purchase_stockins.status IS '状态：0待入库 1已入库 2部分入库';
COMMENT ON COLUMN purchase_stockins.create_user_id IS '创建人ID';
COMMENT ON COLUMN purchase_stockins.deleted IS '逻辑删除：0未删除 1已删除';

-- 索引
CREATE INDEX IF NOT EXISTS idx_purchase_stockins_stockin_code ON purchase_stockins (stockin_code);
CREATE INDEX IF NOT EXISTS idx_purchase_stockins_order_id     ON purchase_stockins (order_id);
CREATE INDEX IF NOT EXISTS idx_purchase_stockins_supplier_id  ON purchase_stockins (supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_stockins_status      ON purchase_stockins (status);
CREATE INDEX IF NOT EXISTS idx_purchase_stockins_create_time  ON purchase_stockins (create_time);


-- 2. 采购入库单明细表
CREATE TABLE IF NOT EXISTS purchase_stockin_items (
    stockin_item_id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    stockin_id       BIGINT         NOT NULL,
    order_item_id    BIGINT,
    material_id      BIGINT,
    material_name    VARCHAR(200)   NOT NULL,
    batch_no         VARCHAR(100),
    production_date  DATE,
    expiry_date      DATE,
    actual_quantity  DECIMAL(12,3) NOT NULL,
    unit             VARCHAR(20)   NOT NULL,
    unit_price       BIGINT        NOT NULL DEFAULT 0,
    amount           BIGINT        NOT NULL DEFAULT 0,
    location_id      BIGINT,
    remark           VARCHAR(500),
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER       NOT NULL DEFAULT 0
);

COMMENT ON TABLE  purchase_stockin_items IS '采购入库单明细表';
COMMENT ON COLUMN purchase_stockin_items.stockin_id IS '入库单ID（关联 purchase_stockins.stockin_id）';
COMMENT ON COLUMN purchase_stockin_items.order_item_id IS '关联采购订单明细ID';
COMMENT ON COLUMN purchase_stockin_items.material_id IS '物料ID';
COMMENT ON COLUMN purchase_stockin_items.material_name IS '物料名称';
COMMENT ON COLUMN purchase_stockin_items.batch_no IS '批次号';
COMMENT ON COLUMN purchase_stockin_items.production_date IS '生产日期';
COMMENT ON COLUMN purchase_stockin_items.expiry_date IS '过期日期';
COMMENT ON COLUMN purchase_stockin_items.actual_quantity IS '实际入库数量';
COMMENT ON COLUMN purchase_stockin_items.unit IS '单位（kg/斤/袋等）';
COMMENT ON COLUMN purchase_stockin_items.unit_price IS '单价（单位：分）';
COMMENT ON COLUMN purchase_stockin_items.amount IS '金额（单位：分）';
COMMENT ON COLUMN purchase_stockin_items.location_id IS '库位ID';
COMMENT ON COLUMN purchase_stockin_items.deleted IS '逻辑删除：0未删除 1已删除';

-- 索引
CREATE INDEX IF NOT EXISTS idx_purchase_stockin_items_stockin_id   ON purchase_stockin_items (stockin_id);
CREATE INDEX IF NOT EXISTS idx_purchase_stockin_items_order_item_id ON purchase_stockin_items (order_item_id);
CREATE INDEX IF NOT EXISTS idx_purchase_stockin_items_material_id   ON purchase_stockin_items (material_id);
