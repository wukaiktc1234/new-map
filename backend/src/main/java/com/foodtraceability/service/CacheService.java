package com.foodtraceability.service;

import com.foodtraceability.common.Result;
import org.springframework.scheduling.annotation.Scheduled;

public interface CacheService {

    void put(String key, String value, long expireMinutes);

    String get(String key);

    void delete(String key);

    @Scheduled(fixedRate = 300000)
    void cleanupExpiredCache();

    int getMemoryCacheSize();

    Result<Void> clearCache(String cacheType);
}
