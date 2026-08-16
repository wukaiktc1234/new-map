-- ============================================================
-- RM-B2-实施卡1：inventory 表对齐 Inventory 实体物料口径 11 列
-- 来源：docs/quality/rm-b1-001-inventory-alignment-plan.md §四（架构评审 §十 事项 1/2 批准方案 A）
-- 类型/默认值以 information_schema 实测为准（readiness §2.1，与真实表手工补列完全匹配）
-- 幂等：ADD COLUMN IF NOT EXISTS —— 对已手工补列的真实表为 no-op，对新环境为补齐动作
-- 无 backfill：列可空/常量默认值，存量数据零迁移
-- 本文件不包含索引（idx_inventory_batch_no 归实施卡 3，架构 §十 事项 3/7）
-- ============================================================

ALTER TABLE inventory
    ADD COLUMN IF NOT EXISTS material_name    VARCHAR(200),
    ADD COLUMN IF NOT EXISTS specification    VARCHAR(200),
    ADD COLUMN IF NOT EXISTS location_id      BIGINT,
    ADD COLUMN IF NOT EXISTS locked_quantity  NUMERIC(12,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS batch_no         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS production_date  DATE,
    ADD COLUMN IF NOT EXISTS expiry_date      DATE,
    ADD COLUMN IF NOT EXISTS unit_cost        BIGINT,
    ADD COLUMN IF NOT EXISTS total_cost       BIGINT,
    ADD COLUMN IF NOT EXISTS min_safe_qty     NUMERIC(12,2),
    ADD COLUMN IF NOT EXISTS max_stock_qty    NUMERIC(12,2);

COMMENT ON COLUMN inventory.material_name   IS '物料名称（冗余展示，与 material_archives 对齐）';
COMMENT ON COLUMN inventory.specification   IS '规格型号';
COMMENT ON COLUMN inventory.location_id     IS '所属库位ID';
COMMENT ON COLUMN inventory.locked_quantity IS '锁定数量（订单预留，默认 0）';
COMMENT ON COLUMN inventory.batch_no        IS '当前批次号';
COMMENT ON COLUMN inventory.production_date IS '生产日期';
COMMENT ON COLUMN inventory.expiry_date     IS '有效期至';
COMMENT ON COLUMN inventory.unit_cost       IS '单位成本（分）';
COMMENT ON COLUMN inventory.total_cost      IS '总成本（分）';
COMMENT ON COLUMN inventory.min_safe_qty    IS '安全库存量';
COMMENT ON COLUMN inventory.max_stock_qty   IS '最大库存量';
