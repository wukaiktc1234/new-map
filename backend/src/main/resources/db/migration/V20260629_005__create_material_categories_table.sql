-- ============================================================
-- 商品分类表（支持树形结构）
-- 创建时间：2026-06-29
-- 说明：
--   1. 替代旧 entity 引用的 material_category（单数）表（实际不存在）
--   2. 支持 parent_id 自关联实现树形结构
--   3. category_code 业务唯一编码（如 VGE、MEAT）
--   4. 与 material_archives.category_id 关联（应用层校验，不加外键约束避免删除复杂性）
--   5. status 字段统一使用 INTEGER（1启用 0停用），符合项目数据一致性规范
-- ============================================================

CREATE TABLE IF NOT EXISTS material_categories (
    category_id     BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_code   VARCHAR(50)  NOT NULL,
    category_name   VARCHAR(200) NOT NULL,
    parent_id       BIGINT,
    description     VARCHAR(500),
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    status          INTEGER      NOT NULL DEFAULT 1,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT uk_material_categories_code UNIQUE (category_code)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_material_categories_parent  ON material_categories(parent_id);
CREATE INDEX IF NOT EXISTS idx_material_categories_status   ON material_categories(status);
CREATE INDEX IF NOT EXISTS idx_material_categories_parent_status ON material_categories(parent_id, status);

-- 表注释
COMMENT ON TABLE  material_categories               IS '商品分类表（支持树形结构）';
COMMENT ON COLUMN material_categories.category_id  IS '分类ID（主键，自增）';
COMMENT ON COLUMN material_categories.category_code IS '分类编码（业务唯一，如 VGE/MEAT）';
COMMENT ON COLUMN material_categories.category_name IS '分类名称';
COMMENT ON COLUMN material_categories.parent_id     IS '父分类ID（NULL 表示顶级分类）';
COMMENT ON COLUMN material_categories.description   IS '分类描述';
COMMENT ON COLUMN material_categories.sort_order     IS '排序值（升序，越小越靠前）';
COMMENT ON COLUMN material_categories.status        IS '状态：1启用 0停用';
COMMENT ON COLUMN material_categories.create_time  IS '创建时间';
COMMENT ON COLUMN material_categories.update_time   IS '更新时间';
COMMENT ON COLUMN material_categories.deleted       IS '逻辑删除：0未删除 1已删除';
