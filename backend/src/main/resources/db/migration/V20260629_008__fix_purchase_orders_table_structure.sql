-- ============================================================
-- 修复采购订单表结构，使其与实体类对齐
--
-- 对应实体类：
--   com.foodtraceability.entity.PurchaseOrder       (@TableName("purchase_orders"))
--   com.foodtraceability.entity.PurchaseOrderItem   (@TableName("purchase_order_items"))
--
-- 对应 H2 开发环境建表逻辑：
--   PurchaseDatabaseInitializer.createPurchaseTables()
--   (H2 环境 Flyway 禁用，由 Initializer 建表；本脚本仅在 PostgreSQL 生产环境执行)
--
-- 背景：
--   V1.0.0.100__init_postgresql.sql 创建的 purchase_orders / purchase_order_items 表
--   字段与实体类严重不匹配（缺 10+ 字段、类型不一致）。本脚本仅添加缺失列，
--   不修改已有列的类型或名称，避免数据迁移风险。
--
-- 状态编码（数据库 INTEGER）：
--   order_status:   0草稿 1待审核 2已审核 3部分入库 4已完成 5已取消
--   payment_status: 0未付 1部分支付 2已支付
-- 前端语义字符串由 DataConverter 在 API 边界完成转换。
--
-- 金额单位：分（BIGINT），前端显示元
--
-- 已知遗留问题（不在本脚本处理）：
--   1. V1.0.0.100 中 supplier_id/order_id 等为 VARCHAR，实体使用 BIGINT；
--      类型转换风险存在但暂不处理，避免破坏现有数据。
--   2. V1.0.0.100 使用 created_at/updated_at，实体使用 create_time/update_time；
--      旧列保留，新增 create_time/update_time 列以匹配实体映射。
--   3. V1.0.0.100 的 status(VARCHAR) 与实体 order_status(INTEGER) 并存；
--      旧 status 列保留，新增 order_status 列以匹配实体。
-- ============================================================

-- ============================================================
-- 1. 采购订单主表 purchase_orders 修复
-- ============================================================

-- 1.1 添加缺失列（V1.0.0.100 已有的列不重复添加）
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS order_code        VARCHAR(50);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS warehouse_id      BIGINT;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS tax_amount         BIGINT DEFAULT 0;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS discount_amount    BIGINT DEFAULT 0;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS final_amount       BIGINT DEFAULT 0;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS order_status      INTEGER DEFAULT 0;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS payment_status    INTEGER DEFAULT 0;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS approval_user_id   BIGINT;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS approval_time      TIMESTAMP;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS approval_remark    VARCHAR(500);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS remark            VARCHAR(500);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS create_user_id    BIGINT;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS create_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS update_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 1.2 索引（CREATE INDEX IF NOT EXISTS 幂等）
CREATE INDEX IF NOT EXISTS idx_purchase_orders_order_code   ON purchase_orders (order_code);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_supplier_id  ON purchase_orders (supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_order_status  ON purchase_orders (order_status);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_create_time  ON purchase_orders (create_time);

-- 1.3 表/字段注释
COMMENT ON TABLE  purchase_orders              IS '采购订单表';
COMMENT ON COLUMN purchase_orders.order_id         IS '订单主键ID（自增）';
COMMENT ON COLUMN purchase_orders.order_code        IS '订单编号（如 PO202604250001）';
COMMENT ON COLUMN purchase_orders.supplier_id       IS '供应商ID';
COMMENT ON COLUMN purchase_orders.warehouse_id      IS '入库仓库ID';
COMMENT ON COLUMN purchase_orders.total_amount      IS '总金额（单位：分）';
COMMENT ON COLUMN purchase_orders.tax_amount        IS '税额（单位：分）';
COMMENT ON COLUMN purchase_orders.discount_amount   IS '折扣金额（单位：分）';
COMMENT ON COLUMN purchase_orders.final_amount      IS '实付金额（单位：分）';
COMMENT ON COLUMN purchase_orders.order_status      IS '订单状态：0草稿 1待审核 2已审核 3部分入库 4已完成 5已取消';
COMMENT ON COLUMN purchase_orders.payment_status    IS '付款状态：0未付 1部分支付 2已支付';
COMMENT ON COLUMN purchase_orders.expected_date     IS '期望到货日期';
COMMENT ON COLUMN purchase_orders.order_date        IS '下单日期';
COMMENT ON COLUMN purchase_orders.approval_user_id  IS '审批人ID';
COMMENT ON COLUMN purchase_orders.approval_time    IS '审批时间';
COMMENT ON COLUMN purchase_orders.approval_remark   IS '审批备注';
COMMENT ON COLUMN purchase_orders.remark            IS '订单备注';
COMMENT ON COLUMN purchase_orders.create_user_id    IS '创建人ID';
COMMENT ON COLUMN purchase_orders.create_time       IS '创建时间';
COMMENT ON COLUMN purchase_orders.update_time       IS '更新时间';
COMMENT ON COLUMN purchase_orders.deleted            IS '逻辑删除标记：0未删除 1已删除';

-- ============================================================
-- 2. 采购订单明细表 purchase_order_items 修复
-- ============================================================

-- 2.1 添加缺失列（V1.0.0.100 已有的列不重复添加）
-- 修复：补充 create_time 列（V1.0.0.100 仅创建了 created_at，实体映射 create_time）
ALTER TABLE purchase_order_items ADD COLUMN IF NOT EXISTS specification      VARCHAR(100);
ALTER TABLE purchase_order_items ADD COLUMN IF NOT EXISTS amount            BIGINT DEFAULT 0;
ALTER TABLE purchase_order_items ADD COLUMN IF NOT EXISTS tax_rate           DECIMAL(5,4);
ALTER TABLE purchase_order_items ADD COLUMN IF NOT EXISTS received_quantity  DECIMAL(12,3) DEFAULT 0;
ALTER TABLE purchase_order_items ADD COLUMN IF NOT EXISTS remark            VARCHAR(500);
ALTER TABLE purchase_order_items ADD COLUMN IF NOT EXISTS create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE purchase_order_items ADD COLUMN IF NOT EXISTS update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 2.2 索引
CREATE INDEX IF NOT EXISTS idx_purchase_order_items_order_id     ON purchase_order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_purchase_order_items_material_id  ON purchase_order_items (material_id);

-- 2.3 表/字段注释
COMMENT ON TABLE  purchase_order_items             IS '采购订单明细表';
COMMENT ON COLUMN purchase_order_items.item_id          IS '明细主键ID（自增）';
COMMENT ON COLUMN purchase_order_items.order_id         IS '关联的采购订单ID';
COMMENT ON COLUMN purchase_order_items.material_id      IS '物料ID（关联物料主数据）';
COMMENT ON COLUMN purchase_order_items.material_name   IS '物料名称';
COMMENT ON COLUMN purchase_order_items.specification    IS '规格';
COMMENT ON COLUMN purchase_order_items.unit            IS '单位';
COMMENT ON COLUMN purchase_order_items.quantity        IS '采购数量';
COMMENT ON COLUMN purchase_order_items.unit_price      IS '单价（单位：分）';
COMMENT ON COLUMN purchase_order_items.amount           IS '金额（单位：分）';
COMMENT ON COLUMN purchase_order_items.tax_rate        IS '税率';
COMMENT ON COLUMN purchase_order_items.received_quantity IS '已收货数量';
COMMENT ON COLUMN purchase_order_items.remark          IS '备注';
COMMENT ON COLUMN purchase_order_items.create_time     IS '创建时间';
COMMENT ON COLUMN purchase_order_items.update_time     IS '更新时间';
COMMENT ON COLUMN purchase_order_items.deleted          IS '逻辑删除标记：0未删除 1已删除';
