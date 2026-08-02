package com.foodtraceability.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/**
 * 缓存错误处理配置（通用）
 *
 * 当缓存操作失败时，使缓存操作静默失败而非抛出异常，
 * 应用降级为直接执行业务方法（缓存未命中）。
 * 这保证了缓存不可用时核心业务功能仍可正常使用。
 */
@Configuration
public class CacheErrorHandlerConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(@NonNull RuntimeException e,
                                            @NonNull org.springframework.cache.Cache cache,
                                            @NonNull Object key) {
                // 缓存 GET 失败时静默降级（视为缓存未命中，直接执行方法）
            }

            @Override
            public void handleCachePutError(@NonNull RuntimeException e,
                                            @NonNull org.springframework.cache.Cache cache,
                                            @NonNull Object key, Object value) {
                // 缓存 PUT 失败时静默降级（结果不缓存，但不影响业务返回值）
            }

            @Override
            public void handleCacheEvictError(@NonNull RuntimeException e,
                                              @NonNull org.springframework.cache.Cache cache,
                                              @NonNull Object key) {
                // 缓存 EVICT 失败时静默降级（缓存未清除，但不影响业务操作）
            }

            @Override
            public void handleCacheClearError(@NonNull RuntimeException e,
                                              @NonNull org.springframework.cache.Cache cache) {
                // 缓存 CLEAR 失败时静默降级
            }
        };
    }
}
