-- ============================================================
-- [DEPRECATED] 本文件为参考性 SQL，不再被项目使用
-- 项目实际使用 *DatabaseInitializer 模式（Java 代码建表）
-- 活跃迁移脚本位于 db/migration/V*.sql（Flyway 管理）
-- H2 环境使用 db/schema.sql + db/data.sql（spring.sql.init）
-- 财务模块使用 db/finance_tables.sql（FinanceDatabaseInitializer）
-- 请勿作为数据库初始化脚本引用
-- 标注时间：2026-06-30
-- ============================================================
-- 创建职位表
CREATE TABLE IF NOT EXISTS positions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '职位ID',
    position_code VARCHAR(50) NOT NULL UNIQUE COMMENT '职位编码',
    position_name VARCHAR(100) NOT NULL COMMENT '职位名称',
    department_id BIGINT COMMENT '所属部门ID',
    level VARCHAR(20) COMMENT '职位级别',
    description TEXT COMMENT '职位描述',
    employee_count INT DEFAULT 0 COMMENT '员工数量',
    status VARCHAR(20) DEFAULT 'active' COMMENT '职位状态（active: 启用, inactive: 禁用）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by VARCHAR(36) COMMENT '创建人',
    updated_by VARCHAR(36) COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：1-已删除，0-未删除',
    INDEX idx_positions_position_code (position_code),
    INDEX idx_positions_department_id (department_id),
    INDEX idx_positions_status (status),
    INDEX idx_positions_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位表';
