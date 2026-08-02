package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.Notification;
import com.foodtraceability.mapper.NotificationMapper;
import com.foodtraceability.service.NotificationDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 通知数据服务实现类
 * 缓存层已由 Spring Cache（ConcurrentMapCacheManager）通过 @Cacheable/@CacheEvict 注解托管
 */
@Service
public class NotificationDataServiceImpl implements NotificationDataService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDataServiceImpl.class);

    private final NotificationMapper notificationMapper;

    public NotificationDataServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    @Cacheable(value = "notificationBasicInfo", key = "#id", unless = "#result == null")
    public Notification getNotificationById(Long id) {
        if (id == null) {
            return null;
        }
        return notificationMapper.selectById(id);
    }

    @Override
    public Map<Long, Notification> batchGetNotifications(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashMap<>();
        }

        // 直接批量查询数据库
        List<Notification> dbResults = notificationMapper.selectBatchIds(ids);
        Map<Long, Notification> result = new HashMap<>();
        for (Notification notification : dbResults) {
            result.put(notification.getId(), notification);
        }

        return result;
    }

    @Override
    public List<Notification> getNotificationsByUserId(Long userId, Integer isRead) {
        if (userId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead);
        }
        wrapper.orderByDesc(Notification::getCreateTime);
        return notificationMapper.selectList(wrapper);
    }

    @Override
    public long countUnreadByUserId(Long userId) {
        if (userId == null) {
            return 0;
        }
        // 直接查询数据库（不再缓存未读数，保证实时准确性）
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        wrapper.eq(Notification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper);
    }

    @Override
    public long countByUserId(Long userId, Integer isRead) {
        if (userId == null) {
            return 0;
        }
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead);
        }
        return notificationMapper.selectCount(wrapper);
    }

    @Override
    @CacheEvict(value = "notificationBasicInfo", key = "#id")
    public void clearNotificationCache(Long id) {
        // 缓存清理由 @CacheEvict 注解自动处理
    }

    @Override
    public void clearNotificationBatchCache(List<Long> ids) {
        // 批量缓存清理：Spring Cache 不支持批量 evict，由业务层在更新时逐个调用 clearNotificationCache
        // 此处保留空实现以维持接口契约
    }

    @Override
    public void clearUserNotificationCache(Long userId) {
        // 未读数已改为直接查询数据库，无需缓存清理
    }

    @Override
    @CacheEvict(value = "notificationBasicInfo", allEntries = true)
    public void clearAllNotificationCache() {
        // 缓存清理由 @CacheEvict(allEntries=true) 注解自动处理
    }
}
