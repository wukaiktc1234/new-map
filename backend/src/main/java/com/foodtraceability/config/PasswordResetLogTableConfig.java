package com.foodtraceability.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 密码重置日志表配置
 * 应用启动时自动创建password_reset_log表
 */
@Component
@Order(20)
public class PasswordResetLogTableConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetLogTableConfig.class);


    public PasswordResetLogTableConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            createPasswordResetLogTable();
            log.info("密码重置日志表初始化完成");
        } catch (Exception e) {
            log.error("密码重置日志表初始化失败", e);
        }
    }

    private void createPasswordResetLogTable() {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS password_reset_log (
                id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                user_id BIGINT NOT NULL,
                username VARCHAR(50) NOT NULL,
                operation_type VARCHAR(50) NOT NULL,
                operation_status VARCHAR(20) NOT NULL,
                operation_detail VARCHAR(255),
                code_type VARCHAR(20),
                client_ip VARCHAR(50),
                client_location VARCHAR(100),
                device_type VARCHAR(50),
                browser VARCHAR(100),
                os VARCHAR(50),
                failure_reason VARCHAR(255),
                error_count INT DEFAULT 0,
                is_abnormal SMALLINT DEFAULT 0,
                abnormal_type VARCHAR(50),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;

        jdbcTemplate.execute(createTableSql);
        log.info("密码重置日志表创建完成或已存在");
    }
}
