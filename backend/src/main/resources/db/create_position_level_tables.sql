-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 职位等级管理系统数据库迁移脚本
-- 创建职位等级类型表和职位等级表，并更新positions表

-- 步骤1: 创建职位等级类型表
CREATE TABLE IF NOT EXISTS position_level_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '等级类型ID',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '等级类型编码',
    name VARCHAR(100) NOT NULL COMMENT '等级类型名称',
    description TEXT COMMENT '等级类型描述',
    sort_order INT DEFAULT 0 COMMENT '排序优先级',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态（active: 启用, inactive: 禁用）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：1-已删除，0-未删除',
    INDEX idx_position_level_types_code (code),
    INDEX idx_position_level_types_status (status),
    INDEX idx_position_level_types_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位等级类型表';

-- 步骤2: 创建职位等级表
CREATE TABLE IF NOT EXISTS position_levels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '等级ID',
    level_type_id BIGINT NOT NULL COMMENT '所属等级类型ID',
    code VARCHAR(50) NOT NULL COMMENT '等级编码',
    name VARCHAR(100) NOT NULL COMMENT '等级名称',
    description TEXT COMMENT '等级描述',
    level_value INT NOT NULL COMMENT '等级数值（用于排序和比较）',
    salary_min BIGINT DEFAULT 0 COMMENT '工资范围下限',
    salary_max BIGINT DEFAULT 0 COMMENT '工资范围上限',
    sort_order INT DEFAULT 0 COMMENT '排序优先级',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态（active: 启用, inactive: 禁用）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：1-已删除，0-未删除',
    INDEX idx_position_levels_level_type_id (level_type_id),
    INDEX idx_position_levels_code (code),
    INDEX idx_position_levels_status (status),
    INDEX idx_position_levels_deleted (deleted),
    CONSTRAINT fk_position_levels_level_type_id FOREIGN KEY (level_type_id) REFERENCES position_level_types(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位等级表';

-- 步骤3: 更新positions表，添加level_id字段（如果不存在）
-- 检查并添加level_id字段
SET @has_level_id = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'food_traceability' AND table_name = 'positions' AND column_name = 'level_id');
SET @sql_add_level_id = IF(@has_level_id = 0, 'ALTER TABLE positions ADD COLUMN level_id BIGINT COMMENT ''职位等级ID'' AFTER level;', 'SELECT ''level_id column already exists'';');
PREPARE stmt_add_level_id FROM @sql_add_level_id;
EXECUTE stmt_add_level_id;
DEALLOCATE PREPARE stmt_add_level_id;

-- 检查并添加level_type字段（如果不存在，有些情况下可能已经存在）
SET @has_level_type = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'food_traceability' AND table_name = 'positions' AND column_name = 'level_type');
SET @sql_add_level_type = IF(@has_level_type = 0, 'ALTER TABLE positions ADD COLUMN level_type VARCHAR(50) COMMENT ''职位等级类型'' AFTER level_id;', 'SELECT ''level_type column already exists'';');
PREPARE stmt_add_level_type FROM @sql_add_level_type;
EXECUTE stmt_add_level_type;
DEALLOCATE PREPARE stmt_add_level_type;

-- 添加外键约束（如果不存在）
SET @has_fk = (SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = 'food_traceability' AND table_name = 'positions' AND constraint_name = 'fk_positions_level_id');
SET @sql_add_fk = IF(@has_fk = 0, 'ALTER TABLE positions ADD CONSTRAINT fk_positions_level_id FOREIGN KEY (level_id) REFERENCES position_levels(id) ON DELETE SET NULL;', 'SELECT ''foreign key fk_positions_level_id already exists'';');
PREPARE stmt_add_fk FROM @sql_add_fk;
EXECUTE stmt_add_fk;
DEALLOCATE PREPARE stmt_add_fk;

-- 步骤4: 为position_levels表添加唯一约束，确保同一类型下等级编码唯一
ALTER TABLE position_levels
ADD UNIQUE INDEX uk_position_levels_type_code (level_type_id, code);

-- 步骤5: 插入初始数据
-- 插入等级类型数据
INSERT INTO position_level_types (code, name, description, sort_order, status) VALUES
('MANAGEMENT', '管理岗', '企业管理层职位等级体系', 1, 'active'),
('PROFESSIONAL', '专业岗', '专业技术人员职位等级体系', 2, 'active'),
('OPERATIONAL', '操作岗', '基层操作岗位职位等级体系', 3, 'active');

-- 插入管理岗等级数据
INSERT INTO position_levels (level_type_id, code, name, description, level_value, salary_min, salary_max, sort_order, status) VALUES
-- 管理岗等级
(1, 'M0', '总经理', '企业最高管理层', 1, 50000, 150000, 1, 'active'),
(1, 'M1', '副总经理', '企业高级管理层', 2, 30000, 80000, 2, 'active'),
(1, 'M2', '部门总监', '部门负责人', 3, 20000, 50000, 3, 'active'),
(1, 'M3', '部门经理', '部门中层管理', 4, 15000, 30000, 4, 'active'),
(1, 'M4', '主管', '基层管理人员', 5, 10000, 20000, 5, 'active'),

-- 专业岗等级
(2, 'P0', '资深专家', '行业资深专家', 1, 30000, 80000, 1, 'active'),
(2, 'P1', '高级工程师', '高级专业技术人员', 2, 20000, 50000, 2, 'active'),
(2, 'P2', '工程师', '中级专业技术人员', 3, 15000, 30000, 3, 'active'),
(2, 'P3', '助理工程师', '初级专业技术人员', 4, 10000, 20000, 4, 'active'),

-- 操作岗等级
(3, 'O0', '高级技师', '高级技术操作人员', 1, 15000, 30000, 1, 'active'),
(3, 'O1', '技师', '中级技术操作人员', 2, 10000, 20000, 2, 'active'),
(3, 'O2', '高级工', '熟练操作人员', 3, 8000, 15000, 3, 'active'),
(3, 'O3', '中级工', '一般操作人员', 4, 6000, 12000, 4, 'active'),
(3, 'O4', '初级工', '新入职操作人员', 5, 4000, 8000, 5, 'active');

-- 步骤6: 更新现有positions表中的level_type字段，根据level值设置默认等级类型
UPDATE positions SET level_type = 'MANAGEMENT' WHERE level IN ('1', '2', '3');
UPDATE positions SET level_type = 'PROFESSIONAL' WHERE level IN ('4', '5');
UPDATE positions SET level_type = 'OPERATIONAL' WHERE level IN ('6', '7', '8');