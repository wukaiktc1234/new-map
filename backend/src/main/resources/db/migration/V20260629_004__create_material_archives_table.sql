-- ============================================================
-- 商品档案表 (material_archives)
-- 采购物料/商品的基础信息维护
-- 对应前端: /v1/purchase/archives
-- ============================================================

CREATE TABLE IF NOT EXISTS material_archives (
    -- 主键：自增 BIGINT
    material_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    -- 业务编码（唯一）
    material_code   VARCHAR(50)   NOT NULL,

    -- 商品名称
    material_name   VARCHAR(200)  NOT NULL,

    -- 分类ID（关联 material_categories.category_id，下个子模块补建后加外键）
    category_id     BIGINT,

    -- 单位（如：斤、袋、箱、瓶）
    unit            VARCHAR(50)   NOT NULL,

    -- 规格型号（如：10kg/箱）
    spec            VARCHAR(200),

    -- 参考价（分）—— 后端和数据库以分为单位（整数），前端以元为单位
    reference_price BIGINT        NOT NULL DEFAULT 0,

    -- 主供应商ID（关联 suppliers.supplier_id）
    supplier_id     BIGINT,

    -- 状态：1启用 0停用（数字编码，与项目规范一致）
    status          INTEGER       NOT NULL DEFAULT 1,

    -- 备注
    remark          VARCHAR(500),

    -- 必备字段（项目规则第八节强制）
    create_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER       NOT NULL DEFAULT 0,

    -- 约束
    CONSTRAINT uk_material_archives_code UNIQUE (material_code)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_material_archives_name       ON material_archives (material_name);
CREATE INDEX IF NOT EXISTS idx_material_archives_category   ON material_archives (category_id);
CREATE INDEX IF NOT EXISTS idx_material_archives_supplier   ON material_archives (supplier_id);
CREATE INDEX IF NOT EXISTS idx_material_archives_status     ON material_archives (status);

-- 表与字段注释
COMMENT ON TABLE  material_archives                  IS '商品档案表';
COMMENT ON COLUMN material_archives.material_id      IS '商品ID（自增主键）';
COMMENT ON COLUMN material_archives.material_code    IS '商品编码（业务唯一）';
COMMENT ON COLUMN material_archives.material_name    IS '商品名称';
COMMENT ON COLUMN material_archives.category_id      IS '分类ID（关联 material_categories，下个子模块补建）';
COMMENT ON COLUMN material_archives.unit             IS '单位（斤/袋/箱/瓶等）';
COMMENT ON COLUMN material_archives.spec             IS '规格型号';
COMMENT ON COLUMN material_archives.reference_price  IS '参考价（分）';
COMMENT ON COLUMN material_archives.supplier_id      IS '主供应商ID（关联 suppliers.supplier_id）';
COMMENT ON COLUMN material_archives.status           IS '状态: 1启用 0停用';
COMMENT ON COLUMN material_archives.remark           IS '备注';
COMMENT ON COLUMN material_archives.create_time      IS '创建时间';
COMMENT ON COLUMN material_archives.update_time      IS '更新时间';
COMMENT ON COLUMN material_archives.deleted          IS '逻辑删除: 0未删除 1已删除';
