package com.foodtraceability.config;

import com.foodtraceability.entity.Position;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 数据库表结构验证配置
 * 在应用启动时验证数据库表结构与Java实体类是否一致
 */
@Configuration
public class DatabaseSchemaValidationConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseSchemaValidationConfig.class);

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    public DatabaseSchemaValidationConfig(JdbcTemplate jdbcTemplate, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    @PostConstruct
    public void validateDatabaseSchema() {
        logger.info("开始验证数据库表结构...");

        if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            logger.info("开发环境，跳过数据库表结构验证");
            return;
        }

        try {
            validatePositionsTable();
            logger.info("数据库表结构验证完成");
        } catch (Exception e) {
            logger.error("数据库表结构验证失败: {}", e.getMessage(), e);
            throw new RuntimeException("数据库表结构验证失败，请检查数据库配置", e);
        }
    }

    /**
     * 验证positions表结构
     */
    private void validatePositionsTable() {
        logger.info("验证positions表结构...");

        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
            "SELECT TABLE_NAME FROM information_schema.TABLES " +
            "WHERE TABLE_SCHEMA = CURRENT_SCHEMA() AND TABLE_NAME = 'positions'"
        );

        if (tables.isEmpty()) {
            logger.warn("positions表不存在，跳过验证");
            return;
        }

        // 检查level字段类型
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
            "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH " +
            "FROM information_schema.COLUMNS " +
            "WHERE TABLE_SCHEMA = CURRENT_SCHEMA() AND TABLE_NAME = 'positions' AND COLUMN_NAME = 'level'"
        );

        if (columns.isEmpty()) {
            throw new RuntimeException("positions表的level字段不存在");
        }

        Map<String, Object> column = columns.get(0);
        String dataType = (String) column.get("DATA_TYPE");
        logger.info("positions表的level字段类型: {}", dataType);

        // 验证level字段是否为varchar类型
        if (!"varchar".equalsIgnoreCase(dataType)) {
            logger.warn("positions表的level字段类型不是varchar，当前类型: {}", dataType);
        }

        // 检查数据一致性
        validatePositionsData();
    }

    /**
     * 验证positions表数据
     */
    private void validatePositionsData() {
        logger.info("验证positions表数据...");

        // 检查是否有无效的level值
        List<Map<String, Object>> invalidLevels = jdbcTemplate.queryForList(
            "SELECT position_id, position_name, level FROM positions " +
            "WHERE level IS NOT NULL " +
            "AND level NOT IN ('初级', '中级', '高级', '主管', '经理', '总监', '副总裁', '总裁', '专家') " +
            "AND level !~ '^[0-9]+$'"
        );

        if (!invalidLevels.isEmpty()) {
            logger.warn("发现{}条无效的level数据:", invalidLevels.size());
            for (Map<String, Object> row : invalidLevels) {
                logger.warn("  ID: {}, 职位: {}, Level: {}", 
                    row.get("position_id"), row.get("position_name"), row.get("level"));
            }
        }

        // 统计level分布
        Map<String, Object> stats = jdbcTemplate.queryForMap(
            "SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN level IN ('初级', '中级', '高级', '主管', '经理', '总监', '副总裁', '总裁', '专家') THEN 1 ELSE 0 END) as chinese_levels, " +
            "SUM(CASE WHEN level ~ '^[0-9]+$' THEN 1 ELSE 0 END) as numeric_levels, " +
            "SUM(CASE WHEN level IS NULL THEN 1 ELSE 0 END) as null_levels " +
            "FROM positions"
        );

        logger.info("positions表数据统计: 总数={}, 中文级别={}, 数字级别={}, NULL级别={}",
            stats.get("total"), stats.get("chinese_levels"), stats.get("numeric_levels"), stats.get("null_levels"));
    }
}
