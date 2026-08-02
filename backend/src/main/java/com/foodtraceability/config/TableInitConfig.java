package com.foodtraceability.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class TableInitConfig {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TableInitConfig.class);
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        createDiningTable();
        // 已废弃：member 表（单数）完全孤立，前端使用的是 members 表（复数，marketing 模块）
        // createMemberTable();
        createComboIngredientTable();
    }

    private void createDiningTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS dining_table (
                    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    table_number VARCHAR(20) NOT NULL UNIQUE,
                    table_name VARCHAR(50),
                    capacity INT DEFAULT 4,
                    area VARCHAR(50),
                    status VARCHAR(20) DEFAULT \'available\',
                    qr_code VARCHAR(100) UNIQUE,
                    current_order_id BIGINT,
                    guest_count INT,
                    seated_at TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            // 已清理：不再自动插入 10 条示例桌台数据，由门店运营人员在系统中手动维护
            log.info("桌台表初始化完成");
        } catch (Exception e) {
            log.error("创建桌台表失败", e);
        }
    }

    private void createMemberTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS member (
                    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    member_no VARCHAR(20) NOT NULL UNIQUE,
                    name VARCHAR(50),
                    phone VARCHAR(20) UNIQUE,
                    gender VARCHAR(10),
                    birthday DATE,
                    level INT DEFAULT 1,
                    level_name VARCHAR(20) DEFAULT \'普通会员\',
                    balance DECIMAL(10,2) DEFAULT 0,
                    points INT DEFAULT 0,
                    total_spent DECIMAL(10,2) DEFAULT 0,
                    order_count INT DEFAULT 0,
                    status VARCHAR(20) DEFAULT \'active\',
                    registered_at TIMESTAMP,
                    last_visit_at TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            log.info("会员表初始化完成");
        } catch (Exception e) {
            log.error("创建会员表失败", e);
        }
    }

    private void createComboIngredientTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS combo_ingredient (
                    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                    combo_id BIGINT NOT NULL,
                    food_id VARCHAR(50) NOT NULL,
                    quantity DECIMAL(10,2) DEFAULT 1,
                    unit VARCHAR(20),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    deleted INT DEFAULT 0
                )
                """);
            log.info("套餐食材表初始化完成");
        } catch (Exception e) {
            log.error("创建套餐食材表失败", e);
        }
    }

    public TableInitConfig(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
