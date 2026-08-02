package com.foodtraceability.service;

import com.foodtraceability.entity.NotificationSettingEntity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface NotificationSettingService extends IService<NotificationSettingEntity> {

    List<NotificationSettingEntity> getUserNotificationSettings(Long userId);

    NotificationSettingEntity getUserNotificationSettingByType(Long userId, String notificationType);

    boolean saveOrUpdateNotificationSetting(NotificationSettingEntity setting);

    boolean batchSaveOrUpdateNotificationSettings(List<NotificationSettingEntity> settings);

    boolean deleteNotificationSetting(Long settingId);

    boolean isNotificationEnabled(Long userId, String notificationType);

    boolean initUserNotificationSettings(Long userId);
}
