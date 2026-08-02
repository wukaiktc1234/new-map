package com.foodtraceability.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 事务配置类
 * 注意：由于项目使用MyBatis-Plus，事务管理由MyBatis-Plus自动处理
 * 本配置类仅用于启用事务注解支持
 */
@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    // 不需要手动注入PlatformTransactionManager
    // Spring Boot会自动为MyBatis-Plus配置合适的事务管理器

}
