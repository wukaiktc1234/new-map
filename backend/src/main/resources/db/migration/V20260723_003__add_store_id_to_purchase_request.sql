-- ============================================================
-- V20260723_003: 为采购申请表增加所属门店字段
-- ============================================================
-- 背景：
--   采购申请列表需要按数据范围过滤。角色 store_manager/team_leader 的 data_scope 为 store，
--   但 purchase_request 表此前只有 department_id/applicant_id，无法按门店隔离。
--   新增 store_id 字段后，店长可查看本门店员工提交的所有采购申请。
--
-- 处理策略：
--   1. 新增 store_id 字段（可空）
--   2. 创建索引优化按门店查询
--   3. 添加字段注释
-- ============================================================

ALTER TABLE purchase_request ADD COLUMN IF NOT EXISTS store_id VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_purchase_request_store_id ON purchase_request (store_id);

COMMENT ON COLUMN purchase_request.store_id IS '申请所属门店ID，用于店长/门店经理按门店隔离数据';
