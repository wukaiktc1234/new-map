-- ============================================================
-- T1 修复（断点#1）：material_trace_code 列类型与实体对齐
-- 实体 MaterialTraceCode.materialId/supplierId/purchaseOrderId 均为 String，
-- 但 DB 列为 bigint（material_id 无 FK；supplier_id/purchase_order_id 有 FK），
-- 导致原料追溯码 INSERT 一直失败（含采购入库与直收两个来源）。
-- 表当前为空（0 行），安全调整；追溯码按证据独立存储，供应商/采购单为自由引用，
-- 删除不兼容的 FK 后列改为 VARCHAR，与实体设计一致。
-- ============================================================

ALTER TABLE material_trace_code DROP CONSTRAINT IF EXISTS fk_mtc_supplier_id;
ALTER TABLE material_trace_code DROP CONSTRAINT IF EXISTS fk_mtc_purchase_order_id;

ALTER TABLE material_trace_code ALTER COLUMN material_id TYPE VARCHAR(64);
ALTER TABLE material_trace_code ALTER COLUMN supplier_id TYPE VARCHAR(64);
ALTER TABLE material_trace_code ALTER COLUMN purchase_order_id TYPE VARCHAR(64);

COMMENT ON COLUMN material_trace_code.material_id IS '物料ID（与实体 String 对齐，原 bigint 导致插入失败）';
