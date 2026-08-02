-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 职级全局化改造 - 数据库迁移脚本
-- 日期: 2026-01-27
-- 目的: 将职级系统改造为全局可用的统一体系

-- 1. 为position_levels表添加coefficient字段（薪资系数）
ALTER TABLE position_levels 
ADD COLUMN coefficient DECIMAL(10, 4) COMMENT '薪资系数' AFTER salary_max;

-- 2. 为employees表添加position_level_id外键
ALTER TABLE employees 
ADD COLUMN position_level_id BIGINT COMMENT '职级ID' AFTER position_id,
ADD INDEX idx_position_level_id (position_level_id),
ADD CONSTRAINT fk_employee_position_level 
FOREIGN KEY (position_level_id) REFERENCES position_levels(id) ON DELETE SET NULL ON UPDATE CASCADE;

-- 3. 为positions表添加position_level_id外键
ALTER TABLE positions 
ADD COLUMN position_level_id BIGINT COMMENT '职级ID' AFTER level,
ADD INDEX idx_position_level_id (position_level_id),
ADD CONSTRAINT fk_position_position_level 
FOREIGN KEY (position_level_id) REFERENCES position_levels(id) ON DELETE SET NULL ON UPDATE CASCADE;

-- 4. 从job_levels表迁移数据到position_levels表
-- 将job_levels的数据合并到position_levels，创建一个新的等级类型"SALARY_LEVEL"
INSERT INTO position_levels (level_type_id, level_value, name, description, level_type, salary_min, salary_max, coefficient, sort_order, status, created_time, updated_time)
SELECT 
    NULL as level_type_id,
    jl.level as level_value,
    jl.level_name as name,
    jl.description as description,
    'SALARY_LEVEL' as level_type,
    NULL as salary_min,
    NULL as salary_max,
    jl.coefficient,
    jl.level as sort_order,
    'active' as status,
    jl.created_at as created_time,
    jl.updated_at as updated_time
FROM job_levels jl
WHERE jl.deleted = 0
ON DUPLICATE KEY UPDATE 
    coefficient = VALUES(coefficient),
    description = VALUES(description),
    updated_time = NOW();

-- 5. 更新employees表，将salary_level映射到position_level_id
-- 这里需要根据实际的salary_level值（如L1, L2等）映射到对应的position_level_id
-- 以下是一个示例映射，需要根据实际情况调整
UPDATE employees e
SET e.position_level_id = (
    SELECT pl.id 
    FROM position_levels pl 
    WHERE pl.level_type = 'SALARY_LEVEL' 
    AND pl.level_value = CAST(SUBSTRING(e.salary_level, 2) AS UNSIGNED)
)
WHERE e.salary_level IS NOT NULL 
AND e.salary_level != '';

-- 6. 创建职级变更历史表
CREATE TABLE IF NOT EXISTS position_level_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    entity_type VARCHAR(50) NOT NULL COMMENT '实体类型：employee/position',
    entity_id BIGINT NOT NULL COMMENT '实体ID',
    old_position_level_id BIGINT COMMENT '原职级ID',
    new_position_level_id BIGINT NOT NULL COMMENT '新职级ID',
    change_reason VARCHAR(500) COMMENT '变更原因',
    changed_by VARCHAR(100) COMMENT '变更人',
    change_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_change_time (change_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职级变更历史表';

-- 7. 为position_levels表添加唯一约束，确保同一等级类型下level_value唯一
ALTER TABLE position_levels 
ADD UNIQUE KEY uk_level_type_value (level_type, level_value);

-- 8. 更新position_levels表的状态字段，确保所有记录都有状态
UPDATE position_levels 
SET status = 'active' 
WHERE status IS NULL OR status = '';

-- 9. 创建视图，方便查询职级信息
CREATE OR REPLACE VIEW v_position_level_details AS
SELECT 
    pl.id,
    pl.level_type,
    pl.level_value,
    pl.name,
    pl.description,
    pl.salary_min,
    pl.salary_max,
    pl.coefficient,
    pl.sort_order,
    pl.status,
    plt.code as type_code,
    plt.name as type_name,
    COUNT(DISTINCT e.employee_id) as employee_count,
    COUNT(DISTINCT p.id) as position_count
FROM position_levels pl
LEFT JOIN position_level_types plt ON pl.level_type = plt.code
LEFT JOIN employees e ON pl.id = e.position_level_id AND e.deleted = 0
LEFT JOIN positions p ON pl.id = p.position_level_id AND p.deleted = 0
GROUP BY pl.id;

-- 10. 添加注释说明
ALTER TABLE position_levels 
MODIFY COLUMN coefficient DECIMAL(10, 4) COMMENT '薪资系数，用于计算员工薪资（系数/最高系数 * 部门基准薪资）';

-- 完成提示
SELECT '职级全局化改造数据库迁移完成！' AS message;