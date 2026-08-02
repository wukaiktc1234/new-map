package com.foodtraceability.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Schema修复迁移组件
 * 在应用启动时自动检测并添加缺失的列
 */
@Component
public class SchemaFixMigration {
    private static final Logger logger = LoggerFactory.getLogger(SchemaFixMigration.class);


    public SchemaFixMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final JdbcTemplate jdbcTemplate;

    /**
     * 启动时执行Schema修复
     */
    @PostConstruct
    public void fixMissingColumns() {
        try {
            // 仅当orders表存在时才修复其字段（DatabaseFixConfig会负责创建orders表）
            if (tableExists("orders")) {
                // 检查orders表是否有idempotency_key列（信息架构中列名统一按大写比较，兼容 H2/PostgreSQL）
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=CURRENT_SCHEMA() AND UPPER(TABLE_NAME)=UPPER('orders') AND UPPER(COLUMN_NAME)=UPPER('idempotency_key')",
                    Integer.class);

                if (count != null && count == 0) {
                    logger.info("检测到orders表缺少idempotency_key列，正在添加...");
                    jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN idempotency_key VARCHAR(128) DEFAULT NULL");
                    logger.info("idempotency_key列添加成功");
                } else {
                    logger.info("orders表idempotency_key列已存在，跳过");
                }

                // BUG#5修复: 检查并修复CONTACT_PHONE字段长度（至少支持11位手机号）
                fixContactPhoneLength();
            } else {
                logger.info("orders表尚未创建，跳过相关字段修复");
            }

            // 修复scheduled_task表缺少task_code列
            fixScheduledTaskMissingColumns();

            // 创建POS班次表
            createPosShiftsTable();

        } catch (Exception e) {
            logger.error("SchemaFixMigration执行失败", e);
        }
    }

    /**
     * 检查表是否存在
     */
    private boolean tableExists(String tableName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=CURRENT_SCHEMA() AND UPPER(TABLE_NAME)=UPPER(?)",
                Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.debug("检查表是否存在失败: {} - {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * 创建POS班次表（pos_shifts）
     * 用于管理收银终端的班次信息
     */
    private void createPosShiftsTable() {
        try {
            // 检查表是否已存在
            Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'pos_shifts'",
                Integer.class);

            if (tableCount != null && tableCount == 0) {
                logger.info("检测到pos_shifts表不存在，正在创建...");
                
                jdbcTemplate.execute(
                    "CREATE TABLE pos_shifts (" +
                    "    shift_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "    terminal_id       VARCHAR(32) NOT NULL DEFAULT ''," +
                    "    employee_id       VARCHAR(20) NOT NULL," +
                    "    employee_name     VARCHAR(50) NOT NULL DEFAULT ''," +
                    "    shift_type        VARCHAR(20) NOT NULL DEFAULT 'day'," +
                    "    start_time        TIMESTAMP NOT NULL," +
                    "    end_time          TIMESTAMP," +
                    "    opening_cash      DECIMAL(10,2) NOT NULL DEFAULT 0.00," +
                    "    closing_cash      DECIMAL(10,2)," +
                    "    total_orders      INT NOT NULL DEFAULT 0," +
                    "    total_amount      DECIMAL(12,2) NOT NULL DEFAULT 0.00," +
                    "    status            VARCHAR(20) NOT NULL DEFAULT 'active'," +
                    "    handover_remark   TEXT," +
                    "    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "    deleted           SMALLINT NOT NULL DEFAULT 0" +
                    ")"
                );
                
                // 创建索引
                jdbcTemplate.execute(
                    "CREATE INDEX idx_pos_shifts_terminal ON pos_shifts(terminal_id)"
                );
                jdbcTemplate.execute(
                    "CREATE INDEX idx_pos_shifts_employee ON pos_shifts(employee_id)"
                );
                jdbcTemplate.execute(
                    "CREATE INDEX idx_pos_shifts_status ON pos_shifts(status)"
                );
                
                logger.info("pos_shifts表创建成功");
            } else {
                logger.info("pos_shifts表已存在，跳过创建");
            }
        } catch (Exception e) {
            logger.error("创建pos_shifts表失败", e);
        }
    }

    /**
     * 修复CONTACT_PHONE字段长度不足问题
     */
    private void fixContactPhoneLength() {
        try {
            Integer charLength = jdbcTemplate.queryForObject(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=CURRENT_SCHEMA() AND UPPER(TABLE_NAME)=UPPER('orders') AND UPPER(COLUMN_NAME)=UPPER('contact_phone')",
                Integer.class);

            if (charLength != null && charLength < 20) {
                logger.warn("检测到orders表contact_phone字段长度为{}，不足20，正在修改...", charLength);
                jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN contact_phone TYPE VARCHAR(20)");
                logger.info("contact_phone字段长度已修改为VARCHAR(20)");
            } else {
                logger.info("orders表contact_phone字段长度正常({})，跳过", charLength);
            }
        } catch (Exception e) {
            logger.error("修复contact_phone字段长度失败", e);
        }
    }

    /**
     * 修复scheduled_task表缺少task_code列
     * 实体类ScheduledTask定义了task_code字段，但数据库可能缺少该列
     */
    private void fixScheduledTaskMissingColumns() {
        try {
            // 检查task_code列是否存在（信息架构中列名统一按大写比较，兼容 H2/PostgreSQL）
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=CURRENT_SCHEMA() AND UPPER(TABLE_NAME)=UPPER('scheduled_task') AND UPPER(COLUMN_NAME)=UPPER('task_code')",
                Integer.class);

            if (count != null && count == 0) {
                logger.warn("检测到scheduled_task表缺少task_code列，正在添加...");
                jdbcTemplate.execute(
                    "ALTER TABLE scheduled_task ADD COLUMN task_code VARCHAR(64) DEFAULT NULL"
                );
                logger.info("scheduled_task表task_code列添加成功");
            } else {
                logger.info("scheduled_task表task_code列已存在，跳过");
            }
        } catch (Exception e) {
            logger.error("修复scheduled_task表task_code列失败", e);
        }
    }
}
