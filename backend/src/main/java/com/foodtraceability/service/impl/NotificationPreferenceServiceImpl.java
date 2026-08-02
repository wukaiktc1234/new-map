package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.NotificationUserPreference;
import com.foodtraceability.mapper.NotificationUserPreferenceMapper;
import com.foodtraceability.service.NotificationPreferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户通知偏好服务实现类
 */
@Service
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPreferenceServiceImpl.class);

    private static final String[][] DEFAULT_PREFERENCES = {
            {"REJECT_AUDIT", "SYSTEM"}, {"REJECT_AUDIT", "EMAIL"},
            {"APPROVE_AUDIT", "SYSTEM"}, {"APPROVE_AUDIT", "EMAIL"},
            {"PENDING_AUDIT", "SYSTEM"}, {"PENDING_AUDIT", "EMAIL"},
            {"INVENTORY_WARNING", "SYSTEM"}, {"INVENTORY_WARNING", "EMAIL"},
            {"CONTRACT_EXPIRY", "SYSTEM"}, {"CONTRACT_EXPIRY", "EMAIL"},
            {"HEALTH_CERT_EXPIRY", "SYSTEM"}, {"HEALTH_CERT_EXPIRY", "EMAIL"},
            {"TASK_ASSIGN", "SYSTEM"}, {"TASK_ASSIGN", "EMAIL"},
            {"ORDER_STATUS", "SYSTEM"},
            {"SYSTEM", "SYSTEM"},
    };

    private final NotificationUserPreferenceMapper preferenceMapper;

    public NotificationPreferenceServiceImpl(NotificationUserPreferenceMapper preferenceMapper) {
        this.preferenceMapper = preferenceMapper;
    }

    @Override
    public List<NotificationUserPreference> getUserPreferences(Long userId) {
        return preferenceMapper.selectByUserId(userId);
    }

    @Override
    public NotificationUserPreference getPreference(Long userId, String notificationType, String channel) {
        return preferenceMapper.selectByUserAndTypeAndChannel(userId, notificationType, channel);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationUserPreference saveOrUpdatePreference(Long userId, String notificationType,
                                                              String channel, Integer isEnabled, String frequency) {
        NotificationUserPreference existing = preferenceMapper.selectByUserAndTypeAndChannel(
                userId, notificationType, channel);

        if (existing != null) {
            existing.setIsEnabled(isEnabled);
            if (frequency != null) {
                existing.setFrequency(frequency);
            }
            existing.setUpdateTime(LocalDateTime.now());
            preferenceMapper.updateById(existing);
            return existing;
        }

        try {
            NotificationUserPreference pref = new NotificationUserPreference();
            pref.setUserId(userId);
            pref.setNotificationType(notificationType);
            pref.setChannel(channel);
            pref.setIsEnabled(isEnabled);
            pref.setFrequency(frequency != null ? frequency : "REALTIME");
            pref.setCreateTime(LocalDateTime.now());
            pref.setUpdateTime(LocalDateTime.now());
            preferenceMapper.insert(pref);
            return pref;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            logger.info("偏好记录并发插入冲突，回退到更新: userId={}, type={}, channel={}", userId, notificationType, channel);
            NotificationUserPreference existingAfterConflict = preferenceMapper.selectByUserAndTypeAndChannel(
                    userId, notificationType, channel);
            if (existingAfterConflict != null) {
                existingAfterConflict.setIsEnabled(isEnabled);
                if (frequency != null) {
                    existingAfterConflict.setFrequency(frequency);
                }
                existingAfterConflict.setUpdateTime(LocalDateTime.now());
                preferenceMapper.updateById(existingAfterConflict);
                return existingAfterConflict;
            }
            throw e;
        }
    }

    @Override
    public boolean isNotificationEnabled(Long userId, String notificationType, String channel) {
        NotificationUserPreference pref = preferenceMapper.selectByUserAndTypeAndChannel(
                userId, notificationType, channel);
        if (pref == null) {
            return true;
        }
        return pref.getIsEnabled() != null && pref.getIsEnabled() == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initUserPreferences(Long userId) {
        List<NotificationUserPreference> existing = preferenceMapper.selectByUserId(userId);
        if (!existing.isEmpty()) {
            return;
        }

        List<NotificationUserPreference> prefs = new ArrayList<>();
        for (String[] def : DEFAULT_PREFERENCES) {
            NotificationUserPreference pref = new NotificationUserPreference();
            pref.setUserId(userId);
            pref.setNotificationType(def[0]);
            pref.setChannel(def[1]);
            pref.setIsEnabled(1);
            pref.setFrequency("REALTIME");
            pref.setCreateTime(LocalDateTime.now());
            pref.setUpdateTime(LocalDateTime.now());
            prefs.add(pref);
        }

        for (NotificationUserPreference pref : prefs) {
            preferenceMapper.insert(pref);
        }
        logger.info("初始化用户通知偏好: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<NotificationUserPreference> batchUpdatePreferences(Long userId, List<NotificationUserPreference> preferences) {
        List<NotificationUserPreference> result = new ArrayList<>();
        for (NotificationUserPreference pref : preferences) {
            NotificationUserPreference updated = saveOrUpdatePreference(
                    userId, pref.getNotificationType(), pref.getChannel(),
                    pref.getIsEnabled(), pref.getFrequency());
            result.add(updated);
        }
        return result;
    }
}
