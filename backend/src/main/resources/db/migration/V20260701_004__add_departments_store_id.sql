-- ============================================================
-- V20260701_004: departments 表新增 store_id 字段
-- ============================================================
-- 修复 H7: HR 的 departments 表缺少 store_id，无法建立部门与门店的关联。
-- 新增 store_id VARCHAR(32) 列及索引 idx_departments_store_id。
-- 本脚本在生产环境（PostgreSQL）中通过 Flyway 执行；
-- H2 开发环境由 BaseDatabaseInitializer.createDepartmentsTable() 处理。
-- 使用 IF NOT EXISTS 语法实现幂等，避免重复执行报错。
-- ============================================================

ALTER TABLE departments ADD COLUMN IF NOT EXISTS store_id VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_departments_store_id ON departments(store_id);
