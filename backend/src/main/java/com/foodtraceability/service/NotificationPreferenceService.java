package com.foodtraceability.service;

import com.foodtraceability.entity.NotificationUserPreference;
import java.util.List;

/**
 * 用户通知偏好服务接口
 * 管理用户对不同类型通知的接收偏好设置
 */
public interface NotificationPreferenceService {

    List<NotificationUserPreference> getUserPreferences(Long userId);

    NotificationUserPreference getPreference(Long userId, String notificationType, String channel);

    NotificationUserPreference saveOrUpdatePreference(Long userId, String notificationType,
                                                       String channel, Integer isEnabled, String frequency);

    boolean isNotificationEnabled(Long userId, String notificationType, String channel);

    void initUserPreferences(Long userId);

    List<NotificationUserPreference> batchUpdatePreferences(Long userId, List<NotificationUserPreference> preferences);
}
