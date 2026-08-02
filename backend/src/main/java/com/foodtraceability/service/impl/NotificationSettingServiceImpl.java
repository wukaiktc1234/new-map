package com.foodtraceability.service.impl;

import com.foodtraceability.entity.NotificationSettingEntity;
import com.foodtraceability.mapper.NotificationSettingMapper;
import com.foodtraceability.service.NotificationSettingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationSettingServiceImpl extends ServiceImpl<NotificationSettingMapper, NotificationSettingEntity> implements NotificationSettingService {


    public NotificationSettingServiceImpl(NotificationSettingMapper notificationSettingMapper) {
        this.notificationSettingMapper = notificationSettingMapper;
    }

    private final NotificationSettingMapper notificationSettingMapper;

    @Override
    public List<NotificationSettingEntity> getUserNotificationSettings(Long userId) {
        LambdaQueryWrapper<NotificationSettingEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationSettingEntity::getSettingGroup, "USER_" + userId);
        return this.list(wrapper);
    }

    @Override
    public NotificationSettingEntity getUserNotificationSettingByType(Long userId, String notificationType) {
        LambdaQueryWrapper<NotificationSettingEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NotificationSettingEntity::getSettingKey, notificationType)
               .eq(NotificationSettingEntity::getSettingGroup, "USER_" + userId);
        return this.getOne(wrapper);
    }

    @Override
    public boolean saveOrUpdateNotificationSetting(NotificationSettingEntity setting) {
        LocalDateTime now = LocalDateTime.now();
        if (setting.getSettingId() == null) {
            setting.setCreateTime(now);
        }
        setting.setUpdateTime(now);
        return this.saveOrUpdate(setting);
    }

    @Override
    public boolean batchSaveOrUpdateNotificationSettings(List<NotificationSettingEntity> settings) {
        LocalDateTime now = LocalDateTime.now();
        for (NotificationSettingEntity setting : settings) {
            if (setting.getSettingId() == null) {
                setting.setCreateTime(now);
            }
            setting.setUpdateTime(now);
        }
        return this.saveOrUpdateBatch(settings);
    }

    @Override
    public boolean deleteNotificationSetting(Long settingId) {
        return this.removeById(settingId);
    }

    @Override
    public boolean isNotificationEnabled(Long userId, String notificationType) {
        NotificationSettingEntity setting = this.getUserNotificationSettingByType(userId, notificationType);
        if (setting == null) {
            return true;
        }
        return setting.getStatus() != null && setting.getStatus() == 1;
    }

    @Override
    public boolean initUserNotificationSettings(Long userId) {
        List<NotificationSettingEntity> settings = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        String group = "USER_" + userId;

        String[] types = {"REJECT_AUDIT", "APPROVE_AUDIT", "AUTO_APPROVE", "PENDING_AUDIT", "TIMEOUT_AUDIT"};
        String[] methods = {"SYSTEM", "EMAIL"};

        for (String type : types) {
            for (String method : methods) {
                NotificationSettingEntity setting = new NotificationSettingEntity();
                setting.setSettingKey(type + "_" + method);
                setting.setSettingValue("enabled");
                setting.setSettingGroup(group);
                setting.setDescription(type + "-" + method);
                setting.setIsEncrypted(0);
                setting.setStatus(1);
                setting.setCreateTime(now);
                setting.setUpdateTime(now);
                settings.add(setting);
            }
        }
        return this.saveBatch(settings);
    }
}
