-- ============================================================
-- 物资需求提报子模块建表脚本
-- 对应前端: types/material-request.ts (MaterialRequestInfo + MaterialRequestItem)
-- 对应前端 API: api/purchase/material-request.ts (/v1/purchase/material-requests)
--
-- 状态编码（数据库 INTEGER）：
--   0=草稿 1=待审核 2=已审核 3=已驳回 4=已转采购申请
-- 前端状态字符串：'draft' 'pending' 'approved' 'rejected' 'converted'
-- 通过 DataConverter 在 API 边界转换
--
-- 金额单位：数据库存储分（BIGINT），前端显示元（number）
--
-- 主键：request_id / item_id 使用 BIGINT GENERATED ALWAYS AS IDENTITY
-- 与实体 MaterialRequest / MaterialRequestItem 的 Long 主键对齐
-- ============================================================

-- 1. 物资需求提报主表
CREATE TABLE IF NOT EXISTS material_request (
    request_id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_no           VARCHAR(32)    NOT NULL,
    title                VARCHAR(100)  NOT NULL,
    store_name           VARCHAR(100)  NOT NULL,
    applicant_id         BIGINT,
    applicant_name       VARCHAR(50),
    expected_date        DATE,
    status               INTEGER       NOT NULL DEFAULT 0,
    total_amount         BIGINT        NOT NULL DEFAULT 0,
    converted_request_no VARCHAR(50),
    remark               VARCHAR(500),
    create_time          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              INTEGER       NOT NULL DEFAULT 0,
    CONSTRAINT uk_material_request_request_no UNIQUE (request_no)
);

COMMENT ON TABLE  material_request IS '物资需求提报主表';
COMMENT ON COLUMN material_request.request_id IS '提报ID（主键，自增）';
COMMENT ON COLUMN material_request.request_no IS '提报单号（业务唯一，格式 MR+yyyyMMdd+4位序号）';
COMMENT ON COLUMN material_request.title IS '需求标题';
COMMENT ON COLUMN material_request.store_name IS '提报门店名称';
COMMENT ON COLUMN material_request.applicant_id IS '申请人ID';
COMMENT ON COLUMN material_request.applicant_name IS '申请人姓名（冗余存储）';
COMMENT ON COLUMN material_request.expected_date IS '期望到货日期';
COMMENT ON COLUMN material_request.status IS '状态：0草稿 1待审核 2已审核 3已驳回 4已转采购申请';
COMMENT ON COLUMN material_request.total_amount IS '总金额（单位：分）';
COMMENT ON COLUMN material_request.converted_request_no IS '转换后的采购申请单号（未转换为空）';
COMMENT ON COLUMN material_request.remark IS '备注（驳回时附加驳回原因）';
COMMENT ON COLUMN material_request.deleted IS '逻辑删除：0未删除 1已删除';

-- 索引
CREATE INDEX IF NOT EXISTS idx_material_request_request_no    ON material_request (request_no);
CREATE INDEX IF NOT EXISTS idx_material_request_status         ON material_request (status);
CREATE INDEX IF NOT EXISTS idx_material_request_store_name      ON material_request (store_name);
CREATE INDEX IF NOT EXISTS idx_material_request_applicant_id    ON material_request (applicant_id);
CREATE INDEX IF NOT EXISTS idx_material_request_create_time     ON material_request (create_time);


-- 2. 物资需求提报明细表
CREATE TABLE IF NOT EXISTS material_request_item (
    item_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    request_id       BIGINT         NOT NULL,
    material_id      BIGINT,
    material_name    VARCHAR(200)   NOT NULL,
    specification    VARCHAR(100),
    quantity         DECIMAL(12,3) NOT NULL,
    unit             VARCHAR(20)   NOT NULL,
    estimated_price  BIGINT        NOT NULL DEFAULT 0,
    subtotal_amount  BIGINT        NOT NULL DEFAULT 0,
    remark           VARCHAR(500),
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          INTEGER       NOT NULL DEFAULT 0
);

COMMENT ON TABLE  material_request_item IS '物资需求提报明细表';
COMMENT ON COLUMN material_request_item.item_id IS '明细ID（主键，自增）';
COMMENT ON COLUMN material_request_item.request_id IS '提报ID（关联 material_request.request_id）';
COMMENT ON COLUMN material_request_item.material_id IS '物料ID（关联 material_archives，临时物料为空）';
COMMENT ON COLUMN material_request_item.material_name IS '物料名称';
COMMENT ON COLUMN material_request_item.specification IS '规格型号';
COMMENT ON COLUMN material_request_item.quantity IS '数量（DECIMAL 支持小数，如 1.5 kg）';
COMMENT ON COLUMN material_request_item.unit IS '单位（kg/斤/袋等）';
COMMENT ON COLUMN material_request_item.estimated_price IS '预估单价（单位：分）';
COMMENT ON COLUMN material_request_item.subtotal_amount IS '小计金额（单位：分，quantity × estimated_price）';
COMMENT ON COLUMN material_request_item.remark IS '备注';
COMMENT ON COLUMN material_request_item.deleted IS '逻辑删除：0未删除 1已删除';

-- 索引
CREATE INDEX IF NOT EXISTS idx_material_request_item_request_id   ON material_request_item (request_id);
CREATE INDEX IF NOT EXISTS idx_material_request_item_material_id  ON material_request_item (material_id);
