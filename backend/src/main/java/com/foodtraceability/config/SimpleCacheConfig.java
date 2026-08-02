package com.foodtraceability.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地缓存配置（基于 ConcurrentMapCacheManager）
 * 已移除 Redis 依赖，使用 Spring 内置的 ConcurrentMap 作为本地缓存
 */
@Configuration
@EnableCaching
public class SimpleCacheConfig {

    @Bean
    public ConcurrentMapCacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
