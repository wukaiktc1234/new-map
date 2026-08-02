package com.foodtraceability.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * 数据源配置类，生产环境使用 PostgreSQL
 *
 * @author demo
 * @since 1.0.0
 */
@Configuration
public class DataSourceConfig {

    /**
     * 生产环境数据源
     */
    @Profile("prod")
    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
} 
