-- ============================================================
-- 采购计划子模块建表脚本
-- 对应前端: types/purchase-plan.ts (PurchasePlanInfo + PurchasePlanItem)
-- 对应前端 API: api/purchase/plan.ts (/v1/purchase/plans)
--
-- 状态编码（数据库 INTEGER）：
--   0=草稿 1=待审批 2=已审批 3=执行中 4=已完成 5=已拒绝
-- 前端状态字符串：'draft' 'pending' 'approved' 'executing' 'completed' 'rejected'
-- 通过 DataConverter 在 API 边界转换
--
-- 金额单位：数据库存储分（BIGINT），前端显示元（number）
-- ============================================================

-- 1. 采购计划主表
CREATE TABLE IF NOT EXISTS purchase_plan (
    plan_id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plan_no         VARCHAR(32)    NOT NULL,
    plan_date       DATE           NOT NULL,
    department_id   BIGINT,
    department_name VARCHAR(100),
    total_amount    BIGINT         NOT NULL DEFAULT 0,
    item_count      INTEGER        NOT NULL DEFAULT 0,
    creator_id      BIGINT,
    creator_name    VARCHAR(100),
    status          INTEGER        NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    approve_by      VARCHAR(100),
    approve_time    TIMESTAMP,
    reject_reason   VARCHAR(500),
    create_time     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER        NOT NULL DEFAULT 0,
    CONSTRAINT uk_purchase_plan_plan_no UNIQUE (plan_no)
);

COMMENT ON TABLE  purchase_plan IS '采购计划主表';
COMMENT ON COLUMN purchase_plan.plan_id IS '计划ID（主键，自增）';
COMMENT ON COLUMN purchase_plan.plan_no IS '计划编号（业务唯一，格式 PL+yyyyMMdd+3位序号）';
COMMENT ON COLUMN purchase_plan.plan_date IS '计划日期';
COMMENT ON COLUMN purchase_plan.department_id IS '部门ID（关联 departments）';
COMMENT ON COLUMN purchase_plan.department_name IS '部门名称（冗余存储，避免 JOIN）';
COMMENT ON COLUMN purchase_plan.total_amount IS '总金额（单位：分）';
COMMENT ON COLUMN purchase_plan.item_count IS '物料项数';
COMMENT ON COLUMN purchase_plan.creator_id IS '创建人ID';
COMMENT ON COLUMN purchase_plan.creator_name IS '创建人名称（冗余）';
COMMENT ON COLUMN purchase_plan.status IS '状态：0草稿 1待审批 2已审批 3执行中 4已完成 5已拒绝';
COMMENT ON COLUMN purchase_plan.approve_by IS '审批人';
COMMENT ON COLUMN purchase_plan.approve_time IS '审批时间';
COMMENT ON COLUMN purchase_plan.reject_reason IS '拒绝原因';
COMMENT ON COLUMN purchase_plan.deleted IS '逻辑删除：0未删除 1已删除';

-- 索引
CREATE INDEX idx_purchase_plan_plan_date     ON purchase_plan (plan_date);
CREATE INDEX idx_purchase_plan_department_id ON purchase_plan (department_id);
CREATE INDEX idx_purchase_plan_status        ON purchase_plan (status);
CREATE INDEX idx_purchase_plan_creator_id    ON purchase_plan (creator_id);
CREATE INDEX idx_purchase_plan_create_time    ON purchase_plan (create_time);


-- 2. 采购计划明细表
CREATE TABLE IF NOT EXISTS purchase_plan_item (
    item_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plan_id          BIGINT         NOT NULL,
    material_id      VARCHAR(50),
    material_name    VARCHAR(200)   NOT NULL,
    specification    VARCHAR(100),
    quantity         DECIMAL(12,3) NOT NULL,
    unit             VARCHAR(20)   NOT NULL,
    estimated_price  BIGINT        NOT NULL DEFAULT 0,
    is_temp_material SMALLINT      NOT NULL DEFAULT 0,
    supplier_id      BIGINT,
    supplier_name    VARCHAR(200),
    remark           VARCHAR(500),
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER       NOT NULL DEFAULT 0
);

COMMENT ON TABLE  purchase_plan_item IS '采购计划明细表';
COMMENT ON COLUMN purchase_plan_item.plan_id IS '采购计划ID（关联 purchase_plan.plan_id）';
COMMENT ON COLUMN purchase_plan_item.material_id IS '物料ID（关联 material_archives，临时物料为空）';
COMMENT ON COLUMN purchase_plan_item.material_name IS '物料名称';
COMMENT ON COLUMN purchase_plan_item.specification IS '规格型号';
COMMENT ON COLUMN purchase_plan_item.quantity IS '数量（DECIMAL 支持小数，如 1.5 kg）';
COMMENT ON COLUMN purchase_plan_item.unit IS '单位（kg/斤/袋等）';
COMMENT ON COLUMN purchase_plan_item.estimated_price IS '预估单价（单位：分）';
COMMENT ON COLUMN purchase_plan_item.is_temp_material IS '是否临时物料：0否 1是';
COMMENT ON COLUMN purchase_plan_item.supplier_id IS '建议供应商ID（关联 suppliers）';
COMMENT ON COLUMN purchase_plan_item.supplier_name IS '建议供应商名称（冗余）';
COMMENT ON COLUMN purchase_plan_item.deleted IS '逻辑删除：0未删除 1已删除';

-- 索引
CREATE INDEX idx_purchase_plan_item_plan_id      ON purchase_plan_item (plan_id);
CREATE INDEX idx_purchase_plan_item_material_id  ON purchase_plan_item (material_id);
CREATE INDEX idx_purchase_plan_item_supplier_id  ON purchase_plan_item (supplier_id);
