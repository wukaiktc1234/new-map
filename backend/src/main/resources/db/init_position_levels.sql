-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- Position Level Management Database Migration Script
-- Create position level types table and position levels table

-- Step 1: Create position level types table
CREATE TABLE IF NOT EXISTS position_level_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Level Type ID',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT 'Level Type Code',
    name VARCHAR(100) NOT NULL COMMENT 'Level Type Name',
    description TEXT COMMENT 'Level Type Description',
    sort_order INT DEFAULT 0 COMMENT 'Sort Order',
    status VARCHAR(20) DEFAULT 'active' COMMENT 'Status (active: enabled, inactive: disabled)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated At',
    deleted TINYINT DEFAULT 0 COMMENT 'Soft Delete: 1-deleted, 0-not deleted',
    INDEX idx_position_level_types_code (code),
    INDEX idx_position_level_types_status (status),
    INDEX idx_position_level_types_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Position Level Types Table';

-- Step 2: Create position levels table
CREATE TABLE IF NOT EXISTS position_levels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Level ID',
    level_type_id BIGINT NOT NULL COMMENT 'Level Type ID',
    code VARCHAR(50) NOT NULL COMMENT 'Level Code',
    name VARCHAR(100) NOT NULL COMMENT 'Level Name',
    description TEXT COMMENT 'Level Description',
    level_value INT NOT NULL COMMENT 'Level Value (for sorting and comparison)',
    salary_min BIGINT DEFAULT 0 COMMENT 'Salary Range Min',
    salary_max BIGINT DEFAULT 0 COMMENT 'Salary Range Max',
    sort_order INT DEFAULT 0 COMMENT 'Sort Order',
    status VARCHAR(20) DEFAULT 'active' COMMENT 'Status (active: enabled, inactive: disabled)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Created At',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated At',
    deleted TINYINT DEFAULT 0 COMMENT 'Soft Delete: 1-deleted, 0-not deleted',
    INDEX idx_position_levels_level_type_id (level_type_id),
    INDEX idx_position_levels_code (code),
    INDEX idx_position_levels_status (status),
    INDEX idx_position_levels_deleted (deleted),
    CONSTRAINT fk_position_levels_level_type_id FOREIGN KEY (level_type_id) REFERENCES position_level_types(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Position Levels Table';

-- Step 5: Add unique constraint to position_levels table
ALTER TABLE position_levels
ADD UNIQUE INDEX uk_position_levels_type_code (level_type_id, code);

-- Step 6: Insert initial data
-- Insert level type data
INSERT INTO position_level_types (code, name, description, sort_order, status) VALUES
('MANAGEMENT', 'Management', 'Enterprise management level system', 1, 'active'),
('PROFESSIONAL', 'Professional', 'Professional technical level system', 2, 'active'),
('OPERATIONAL', 'Operational', 'Operational level system', 3, 'active');

-- Insert management level data
INSERT INTO position_levels (level_type_id, code, name, description, level_value, salary_min, salary_max, sort_order, status) VALUES
-- Management levels
(1, 'M0', 'General Manager', 'Enterprise top management', 1, 50000, 150000, 1, 'active'),
(1, 'M1', 'Deputy General Manager', 'Enterprise senior management', 2, 30000, 80000, 2, 'active'),
(1, 'M2', 'Department Director', 'Department head', 3, 20000, 50000, 3, 'active'),
(1, 'M3', 'Department Manager', 'Department middle management', 4, 15000, 30000, 4, 'active'),
(1, 'M4', 'Supervisor', 'Frontline management', 5, 10000, 20000, 5, 'active'),

-- Professional levels
(2, 'P0', 'Senior Expert', 'Industry senior expert', 1, 30000, 80000, 1, 'active'),
(2, 'P1', 'Senior Engineer', 'Senior technical staff', 2, 20000, 50000, 2, 'active'),
(2, 'P2', 'Engineer', 'Mid-level technical staff', 3, 15000, 30000, 3, 'active'),
(2, 'P3', 'Assistant Engineer', 'Junior technical staff', 4, 10000, 20000, 4, 'active'),

-- Operational levels
(3, 'O0', 'Senior Technician', 'Senior technical operator', 1, 15000, 30000, 1, 'active'),
(3, 'O1', 'Technician', 'Mid-level technical operator', 2, 10000, 20000, 2, 'active'),
(3, 'O2', 'Senior Worker', 'Skilled operator', 3, 8000, 15000, 3, 'active'),
(3, 'O3', 'Mid Worker', 'General operator', 4, 6000, 12000, 4, 'active'),
(3, 'O4', 'Junior Worker', 'New operator', 5, 4000, 8000, 5, 'active');
