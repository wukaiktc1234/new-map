package com.foodtraceability.service;

import com.foodtraceability.entity.Notification;

import java.util.List;
import java.util.Map;

/**
 * 通知数据服务接口
 * 提供通知的缓存优先数据访问，遵循Controller→Service→DataService→Mapper分层
 */
public interface NotificationDataService {

    Notification getNotificationById(Long id);

    Map<Long, Notification> batchGetNotifications(List<Long> ids);

    List<Notification> getNotificationsByUserId(Long userId, Integer isRead);

    long countUnreadByUserId(Long userId);

    long countByUserId(Long userId, Integer isRead);

    void clearNotificationCache(Long id);

    void clearNotificationBatchCache(List<Long> ids);

    void clearUserNotificationCache(Long userId);

    void clearAllNotificationCache();
}
