package com.foodtraceability.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TraceCodeSystemConfig {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TraceCodeSystemConfig.class);
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("===== 检查并添加追溯码系统字段 =====");
        addColumnIfNotExists("material_trace_code", "available_quantity", "DECIMAL(10,3)", "quantity");
        addColumnIfNotExists("material_trace_code", "locked_quantity", "DECIMAL(10,3) DEFAULT 0", "available_quantity");
        addColumnIfNotExists("material_trace_code", "trace_type", "VARCHAR(20) DEFAULT \'batch\'", "status");
        addColumnIfNotExists("material_trace_code", "bind_dish_id", "VARCHAR(50)", "trace_type");
        addColumnIfNotExists("material_trace_code", "bind_dish_name", "VARCHAR(100)", "bind_dish_id");
        addColumnIfNotExists("kitchen_order", "material_locked", "SMALLINT DEFAULT 0", null);
        addColumnIfNotExists("kitchen_order", "material_lock_time", "TIMESTAMP", "material_locked");
        log.info("===== 追溯码系统字段检查完成 =====");
    }

    private void addColumnIfNotExists(String tableName, String columnName, String columnDef, String afterColumn) {
        try {
            String checkSql = "SELECT COUNT(*) FROM information_schema.columns " + "WHERE table_schema = CURRENT_SCHEMA() AND table_name = ? AND column_name = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName, columnName);
            if (count != null && count == 0) {
                String alterSql;
                alterSql = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnDef);
                jdbcTemplate.execute(alterSql);
                log.info("添加列成功: {}.{}", tableName, columnName);
            }
        } catch (Exception e) {
            log.warn("添加列 {} 失败: {}", columnName, e.getMessage());
        }
    }

    public TraceCodeSystemConfig(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
