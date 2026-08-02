-- ============================================================
-- 修复 stores_new 表相关问题（C10/C11/H8/H9/H12）
-- 1. manager_id 由 BIGINT 改为 VARCHAR(32)，对齐 employees.employee_id（雪花算法字符串）
-- 2. 新增 department_id 列（H8：门店归属部门）
-- 3. 新增 org_id 列（H9：门店纳入组织架构树）
-- 4. store_code 唯一性约束改为部分唯一索引（仅未删除记录，H12）
-- ============================================================

-- 1. manager_id 类型变更：BIGINT -> VARCHAR(32)
-- PostgreSQL 可自动将 BIGINT 隐式转换为 VARCHAR
ALTER TABLE stores_new ALTER COLUMN manager_id TYPE VARCHAR(32);

-- 2. 新增 department_id 列（H8）
ALTER TABLE stores_new ADD COLUMN IF NOT EXISTS department_id VARCHAR(32);

-- 3. 新增 org_id 列（H9）
ALTER TABLE stores_new ADD COLUMN IF NOT EXISTS org_id VARCHAR(32);

-- 4. store_code 唯一性约束（H12）
-- 先删除 V9.0.0 中由列级 UNIQUE 创建的约束（PostgreSQL 默认命名 {table}_{column}_key）
ALTER TABLE stores_new DROP CONSTRAINT IF EXISTS stores_new_store_code_key;
-- 创建部分唯一索引：仅对未删除记录强制 store_code 唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_stores_new_store_code
    ON stores_new(store_code) WHERE deleted = 0;

-- 5. 新增索引
CREATE INDEX IF NOT EXISTS idx_stores_new_department_id ON stores_new(department_id);
CREATE INDEX IF NOT EXISTS idx_stores_new_org_id ON stores_new(org_id);
