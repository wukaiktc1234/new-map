-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 门店-部门-职位关联优化脚本
-- 为departments和positions表添加store_id字段，实现完整的数据关联和级联更新

-- 步骤1: 为departments表添加store_id字段
ALTER TABLE departments 
ADD COLUMN store_id VARCHAR(50) COMMENT '所属门店ID' AFTER description;

-- 为departments表的store_id字段创建索引
CREATE INDEX idx_departments_store_id ON departments(store_id);

-- 步骤2: 为positions表添加store_id字段
ALTER TABLE positions 
ADD COLUMN store_id VARCHAR(50) COMMENT '所属门店ID' AFTER department_id;

-- 为positions表的store_id字段创建索引
CREATE INDEX idx_positions_store_id ON positions(store_id);

-- 步骤3: 为stores表添加外键约束说明（注释说明，不实际创建外键以保持灵活性）
-- 注意：由于使用逻辑删除，不建议创建外键约束，通过应用层维护数据一致性

-- 步骤4: 创建门店-部门-职位关联视图（用于快速查询）
CREATE OR REPLACE VIEW v_store_dept_position AS
SELECT 
    s.store_id,
    s.store_name,
    s.store_code,
    s.status AS store_status,
    d.ID AS dept_id,
    d.DEPT_NAME,
    d.DEPT_CODE,
    d.STATUS AS dept_status,
    p.id AS position_id,
    p.position_name,
    p.position_code,
    p.status AS position_status,
    COUNT(DISTINCT e.employee_id) AS employee_count
FROM stores s
LEFT JOIN departments d ON s.store_id = d.store_id AND d.deleted = 0
LEFT JOIN positions p ON d.ID = p.department_id AND p.deleted = 0
LEFT JOIN employees e ON p.id = e.position_id AND e.deleted = 0
WHERE s.deleted = 0
GROUP BY s.store_id, d.ID, p.id;

-- 步骤5: 创建门店部门统计视图
CREATE OR REPLACE VIEW v_store_dept_stats AS
SELECT 
    s.store_id,
    s.store_name,
    s.store_code,
    COUNT(DISTINCT d.ID) AS dept_count,
    COUNT(DISTINCT p.id) AS position_count,
    COUNT(DISTINCT e.employee_id) AS employee_count
FROM stores s
LEFT JOIN departments d ON s.store_id = d.store_id AND d.deleted = 0
LEFT JOIN positions p ON d.ID = p.department_id AND p.deleted = 0
LEFT JOIN employees e ON p.id = e.position_id AND e.deleted = 0
WHERE s.deleted = 0
GROUP BY s.store_id;

-- 步骤6: 为现有数据设置默认门店关联（可选）
-- 将所有未关联门店的部门和职位关联到默认门店（总店）
UPDATE departments d
LEFT JOIN stores s ON d.store_id IS NULL
SET d.store_id = 'STORE001'
WHERE s.store_id = 'STORE001' AND d.store_id IS NULL;

UPDATE positions p
LEFT JOIN stores s ON p.store_id IS NULL
SET p.store_id = 'STORE001'
WHERE s.store_id = 'STORE001' AND p.store_id IS NULL;

-- 步骤7: 添加注释说明
ALTER TABLE departments COMMENT = '部门表（支持门店关联）';
ALTER TABLE positions COMMENT = '职位表（支持门店关联）';
