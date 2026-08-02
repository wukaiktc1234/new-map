package com.foodtraceability.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.PostConstruct;

/**
 * 职位-角色权限映射表初始化配置
 */
@Configuration
public class PositionRoleMappingTableConfig {

    private static final Logger log = LoggerFactory.getLogger(PositionRoleMappingTableConfig.class);


    public PositionRoleMappingTableConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        createPositionRoleMappingTable();
        createEmployeeDataScopeTable();
        createPermissionAssignmentLogTable();
    }

    private void createPositionRoleMappingTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS position_role_mapping (" +
                "id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY," +
                "position_id BIGINT NOT NULL," +
                "role_id BIGINT NOT NULL," +
                "is_primary BOOLEAN DEFAULT FALSE," +
                "priority INT DEFAULT 0," +
                "status BOOLEAN DEFAULT TRUE," +
                "description VARCHAR(500)," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "created_by VARCHAR(36)," +
                "updated_by VARCHAR(36)," +
                "deleted SMALLINT DEFAULT 0," +
                "PRIMARY KEY (id)" +
                ")");
            log.info("职位-角色映射表初始化完成");
        } catch (Exception e) {
            log.warn("职位-角色映射表已存在或创建失败: {}", e.getMessage());
        }
    }

    private void createEmployeeDataScopeTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS employee_data_scope (" +
                "id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY," +
                "employee_id VARCHAR(36) NOT NULL," +
                "scope_type VARCHAR(20) NOT NULL," +
                "scope_id VARCHAR(50) NOT NULL," +
                "scope_name VARCHAR(100)," +
                "source VARCHAR(50) DEFAULT 'manual'," +
                "status BOOLEAN DEFAULT TRUE," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "created_by VARCHAR(36)," +
                "updated_by VARCHAR(36)," +
                "deleted SMALLINT DEFAULT 0," +
                "PRIMARY KEY (id)" +
                ")");
            log.info("员工数据权限表初始化完成");
        } catch (Exception e) {
            log.warn("员工数据权限表已存在或创建失败: {}", e.getMessage());
        }
    }

    private void createPermissionAssignmentLogTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS permission_assignment_log (" +
                "id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY," +
                "employee_id VARCHAR(36) NOT NULL," +
                "operation_type VARCHAR(50) NOT NULL," +
                "old_position_id VARCHAR(36)," +
                "new_position_id VARCHAR(36)," +
                "old_roles TEXT," +
                "new_roles TEXT," +
                "old_data_scopes TEXT," +
                "new_data_scopes TEXT," +
                "operator VARCHAR(36)," +
                "operated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "remark TEXT," +
                "PRIMARY KEY (id)" +
                ")");
            log.info("权限分配日志表初始化完成");
        } catch (Exception e) {
            log.warn("权限分配日志表已存在或创建失败: {}", e.getMessage());
        }
    }
}
