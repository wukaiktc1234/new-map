package com.foodtraceability.config;


import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 限流配置类
 * 使用Resilience4j实现API限流功能
 */
@Configuration
public class RateLimiterConfiguration {

    /**
     * 配置默认的限流规则
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimiterRegistry rateLimiterRegistry() {
        // 使用默认配置创建限流注册表
        return RateLimiterRegistry.ofDefaults();
    }
}
