package com.foodtraceability.service.impl;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存服务实现（基于内存 ConcurrentHashMap）
 * 已移除 Redis 依赖，仅使用本地内存缓存
 */
@Service
public class CacheServiceImpl implements CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheServiceImpl.class);

    private final ConcurrentHashMap<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();

    private static class CacheEntry {
        String value;
        long expireTime;

        CacheEntry(String value, long expireMinutes) {
            this.value = value;
            this.expireTime = System.currentTimeMillis() + expireMinutes * 60 * 1000;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    @Override
    public void put(String key, String value, long expireMinutes) {
        memoryCache.put(key, new CacheEntry(value, expireMinutes));
    }

    @Override
    public String get(String key) {
        CacheEntry entry = memoryCache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }
        if (entry != null) {
            memoryCache.remove(key);
        }
        return null;
    }

    @Override
    public void delete(String key) {
        memoryCache.remove(key);
    }

    @Override
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredCache() {
        int removedCount = 0;
        Iterator<Map.Entry<String, CacheEntry>> iterator = memoryCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CacheEntry> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removedCount++;
            }
        }
        if (removedCount > 0) {
            logger.debug("清理过期缓存: {} 条", removedCount);
        }
    }

    @Override
    public int getMemoryCacheSize() {
        return memoryCache.size();
    }

    @Override
    public Result<Void> clearCache(String cacheType) {
        try {
            logger.info("清理缓存: cacheType={}", cacheType);

            if (cacheType == null || cacheType.isEmpty() || "all".equalsIgnoreCase(cacheType)) {
                memoryCache.clear();
                logger.info("已清理所有缓存");
                return Result.success(null, "缓存清理成功");
            }

            String prefix = cacheType + ":";
            memoryCache.keySet().removeIf(key -> key.startsWith(prefix));

            logger.info("已清理{}类型缓存", cacheType);
            return Result.success(null, "缓存清理成功");
        } catch (Exception e) {
            logger.error("清理缓存失败", e);
            return Result.error(500, "清理缓存失败：" + e.getMessage());
        }
    }
}
