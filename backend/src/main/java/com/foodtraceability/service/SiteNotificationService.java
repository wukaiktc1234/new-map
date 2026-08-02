package com.foodtraceability.service;

import com.foodtraceability.entity.Notification;

import java.util.List;
import java.util.Map;

/**
 * 站内通知服务接口
 * 提供站内通知的CRUD、已读管理、WebSocket推送等功能
 */
public interface SiteNotificationService {

    Notification createNotification(Long userId, String title, String content,
                                     String type, String priority,
                                     Long businessId, String businessType,
                                     Long senderId, String senderName);

    Notification createNotification(Long userId, String title, String content,
                                     String type, String priority,
                                     Long businessId, String businessType,
                                     Long senderId, String senderName,
                                     String extraData);

    List<Notification> getNotificationsByUserId(Long userId, Integer isRead, Integer page, Integer size);

    long countUnreadByUserId(Long userId);

    boolean markAsRead(Long notificationId, Long userId);

    boolean markAllAsRead(Long userId);

    boolean deleteNotification(Long notificationId, Long userId);

    void pushNotificationToUser(Long userId, Notification notification);

    void sendBusinessNotification(String templateCode, Long userId, Map<String, Object> variables,
                                   String bizType, String bizId, String priority);
}
