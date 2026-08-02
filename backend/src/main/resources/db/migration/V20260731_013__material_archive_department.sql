-- ============================================================
-- 商品档案增加"使用部门"维度（物资按部门过滤）
-- department_id = NULL 表示通用物料（所有部门可见）
-- ============================================================

ALTER TABLE material_archives ADD COLUMN IF NOT EXISTS department_id BIGINT;

COMMENT ON COLUMN material_archives.department_id IS '使用部门ID（NULL=通用物料，所有部门可见）';
