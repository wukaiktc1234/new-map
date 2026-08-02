package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.SysConfigBatchUpdateItemDTO;
import com.foodtraceability.entity.SysConfig;
import com.foodtraceability.entity.SysConfigHistory;
import com.foodtraceability.mapper.SysConfigHistoryMapper;
import com.foodtraceability.mapper.SysConfigMapper;
import com.foodtraceability.security.utils.AesEncryptionUtil;
import com.foodtraceability.service.SysConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SysConfigServiceImpl implements SysConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SysConfigServiceImpl.class);
    private static final long CACHE_TTL_MINUTES = 30;
    private static final int MAX_CONFIG_VALUE_LENGTH = 10000;
    private static final String MASKED_VALUE = "******";

    // 内存配置缓存：configKey -> 缓存条目（带TTL）
    private final ConcurrentHashMap<String, ConfigCacheEntry> configCache = new ConcurrentHashMap<>();

    public SysConfigServiceImpl(SysConfigMapper sysConfigMapper, SysConfigHistoryMapper historyMapper) {
        this.sysConfigMapper = sysConfigMapper;
        this.historyMapper = historyMapper;
    }

    private final SysConfigMapper sysConfigMapper;

    private final SysConfigHistoryMapper historyMapper;

    /**
     * 配置缓存条目，记录值和过期时间
     */
    private static class ConfigCacheEntry {
        final String value;
        final long expireTimeMillis;

        ConfigCacheEntry(String value, long expireTimeMillis) {
            this.value = value;
            this.expireTimeMillis = expireTimeMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTimeMillis;
        }
    }

    @Override
    public String getValue(String configKey) {
        if (configKey == null || configKey.isEmpty()) return null;

        String cached = getFromCache(configKey);
        if (cached != null) {
            if (MASKED_VALUE.equals(cached)) return MASKED_VALUE;
            return cached;
        }

        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey)
               .eq(SysConfig::getIsEnabled, 1)
               .last("LIMIT 1");
        SysConfig config = sysConfigMapper.selectOne(wrapper);

        if (config == null) {
            setCache(configKey, "", 5);
            return null;
        }

        String value = decryptIfNeeded(config);

        if (config.getIsSensitive() != null && config.getIsSensitive() == 1) {
            setCache(configKey, MASKED_VALUE, CACHE_TTL_MINUTES);
            return MASKED_VALUE;
        }

        setCache(configKey, value != null ? value : "", CACHE_TTL_MINUTES);
        return value;
    }

    @Override
    public <T> T getValue(String configKey, Class<T> type) {
        String value = getValue(configKey);
        if (value == null || value.isEmpty() || MASKED_VALUE.equals(value)) return null;

        if (type == String.class) {
            return type.cast(value);
        } else if (type == Integer.class) {
            try { return type.cast(Integer.parseInt(value)); } catch (Exception e) { return null; }
        } else if (type == Long.class) {
            try { return type.cast(Long.parseLong(value)); } catch (Exception e) { return null; }
        } else if (type == Double.class) {
            try { return type.cast(Double.parseDouble(value)); } catch (Exception e) { return null; }
        } else if (type == Boolean.class) {
            return type.cast(Boolean.parseBoolean(value));
        }
        return null;
    }

    @Override
    public Map<String, String> getValuesByGroup(String group) {
        List<SysConfig> configs = getConfigsByGroup(group);
        Map<String, String> result = new LinkedHashMap<>();
        if (configs != null) {
            for (SysConfig c : configs) {
                if (c.getIsSensitive() != null && c.getIsSensitive() == 1) {
                    result.put(c.getConfigKey(), MASKED_VALUE);
                } else {
                    result.put(c.getConfigKey(), decryptIfNeeded(c));
                }
            }
        }
        return result;
    }

    @Override
    public SysConfig getConfig(String configKey) {
        if (configKey == null || configKey.isEmpty()) return null;
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey)
               .last("LIMIT 1");
        SysConfig config = sysConfigMapper.selectOne(wrapper);
        if (config != null && config.getIsSensitive() != null && config.getIsSensitive() == 1) {
            config.setConfigValue(MASKED_VALUE);
        }
        return config;
    }

    @Override
    public IPage<SysConfig> getConfigPage(Page<SysConfig> page, String configKey, String configName,
                                            String configGroup, Integer isEnabled, Integer isSensitive) {
        IPage<SysConfig> result = sysConfigMapper.selectConfigPage(page,
            truncate(configKey), truncate(configName),
            configGroup, isEnabled, isSensitive);

        if (result != null && result.getRecords() != null) {
            for (SysConfig c : result.getRecords()) {
                if (c.getIsSensitive() != null && c.getIsSensitive() == 1) {
                    c.setConfigValue(MASKED_VALUE);
                }
            }
        }
        return result;
    }

    @Override
    public List<SysConfig> getConfigsByGroup(String group) {
        return sysConfigMapper.selectByGroup(group != null ? group : "general");
    }

    @Override
    public List<String> getAllGroups() {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysConfig::getConfigGroup)
               .groupBy(SysConfig::getConfigGroup)
               .orderByAsc(SysConfig::getConfigGroup);
        return sysConfigMapper.selectList(wrapper).stream()
            .map(SysConfig::getConfigGroup)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateValue(String configKey, String value, String userId, String username) {
        if (configKey == null || configKey.isEmpty()) return false;

        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey)
               .last("LIMIT 1");
        SysConfig config = sysConfigMapper.selectOne(wrapper);

        if (config == null) {
            logger.warn("配置项不存在: key={}", configKey);
            return false;
        }

        if (config.getIsReadonly() != null && config.getIsReadonly() == 1) {
            logger.warn("尝试修改只读配置: key={}, user={}", configKey, userId);
            throw new SecurityException("该配置为系统关键配置，不允许修改");
        }

        if (config.getIsEnabled() == null || !config.getIsEnabled().equals(1)) {
            throw new IllegalStateException("该配置已禁用，请先启用后再修改");
        }

        String oldValue = decryptIfNeeded(config);
        if (value.length() > MAX_CONFIG_VALUE_LENGTH) {
            throw new IllegalArgumentException("配置值过长(最大" + MAX_CONFIG_VALUE_LENGTH + "字符)");
        }

        String encryptedValue = value;
        if (config.getIsSensitive() != null && config.getIsSensitive() == 1) {
            encryptedValue = encryptValue(value);
            config.setIsEncrypted(1);
        }

        config.setConfigValue(encryptedValue);
        config.setUpdateUserId(userId != null ? userId : "");
        config.setUpdateUsername(username != null ? username : "");
        config.setUpdatedAt(LocalDateTime.now());

        int rows = sysConfigMapper.updateById(config);
        if (rows > 0) {
            saveHistory(config, "UPDATE", oldValue, value, userId, username, "手动修改");
            clearCache(configKey);
            logger.info("配置更新: key={}, user={}", configKey, userId);
        }
        return rows > 0;
    }

    @Override
    public boolean batchUpdate(List<SysConfigBatchUpdateItemDTO> updates, String userId, String username) {
        if (updates == null || updates.isEmpty()) return false;
        if (updates.size() > 50) throw new IllegalArgumentException("单次批量更新不能超过50条");

        int successCount = 0;
        List<Map<String, String>> failures = new ArrayList<>();

        for (SysConfigBatchUpdateItemDTO item : updates) {
            String configKey = item.getConfigKey();
            String value = item.getValue();
            if (configKey != null && value != null) {
                try {
                    if (updateValue(configKey, value, userId, username)) {
                        successCount++;
                    }
                } catch (SecurityException | IllegalStateException e) {
                    logger.warn("批量更新跳过: key={}, reason={}", configKey, e.getMessage());
                    Map<String, String> fail = new HashMap<>();
                    fail.put("configKey", configKey);
                    fail.put("reason", e.getMessage() != null ? e.getMessage() : "权限或状态异常");
                    failures.add(fail);
                }
            }
        }
        if (!failures.isEmpty()) {
            logger.warn("批量更新部分失败: 成功{}/总{}, 失败详情={}", successCount, updates.size(), failures);
        }
        logger.info("批量配置更新: 成功{}/{}", successCount, updates.size());
        return successCount > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetToDefault(String configKey, String userId, String username) {
        if (configKey == null || configKey.isEmpty()) return false;

        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey)
               .last("LIMIT 1");
        SysConfig config = sysConfigMapper.selectOne(wrapper);

        if (config == null) return false;
        if (config.getIsReadonly() != null && config.getIsReadonly() == 1) {
            throw new SecurityException("该配置为系统关键配置，不允许重置");
        }

        String oldValue = decryptIfNeeded(config);
        String defaultValue = config.getDefaultValue() != null ? config.getDefaultValue() : "";

        String encryptedDefault = defaultValue;
        if (config.getIsSensitive() != null && config.getIsSensitive() == 1) {
            encryptedDefault = encryptValue(defaultValue);
            config.setIsEncrypted(1);
        }

        config.setConfigValue(encryptedDefault);
        config.setUpdateUserId(userId != null ? userId : "");
        config.setUpdateUsername(username != null ? username : "");
        config.setUpdatedAt(LocalDateTime.now());

        int rows = sysConfigMapper.updateById(config);
        if (rows > 0) {
            saveHistory(config, "RESET", oldValue, defaultValue, userId, username, "重置为默认值");
            clearCache(configKey);
            logger.info("配置重置为默认值: key={}, user={}", configKey, userId);
        }
        return rows > 0;
    }

    @Override
    public boolean resetGroupToDefault(String group, String userId, String username) {
        List<SysConfig> configs = getConfigsByGroup(group);
        if (configs == null || configs.isEmpty()) return false;

        int count = 0;
        for (SysConfig c : configs) {
            if (c.getIsReadonly() == null || c.getIsReadonly() != 1) {
                try {
                    if (resetToDefault(c.getConfigKey(), userId, username)) count++;
                } catch (Exception e) {
                    logger.warn("重置跳过: key={}, reason={}", c.getConfigKey(), e.getMessage());
                }
            }
        }
        logger.info("分组配置重置: group={}, reset={}/{}", group, count, configs.size());
        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableConfig(String configKey, String userId, String username) {
        return setEnabledStatus(configKey, 1, userId, username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableConfig(String configKey, String userId, String username) {
        return setEnabledStatus(configKey, 0, userId, username);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean setEnabledStatus(String configKey, int enabled, String userId, String username) {
        if (configKey == null || configKey.isEmpty()) return false;

        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, configKey).last("LIMIT 1");
        SysConfig config = sysConfigMapper.selectOne(wrapper);
        if (config == null) return false;

        if (enabled == 0 && config.getIsReadonly() != null && config.getIsReadonly() == 1) {
            throw new SecurityException("该配置为系统关键配置，不允许禁用");
        }

        int current = config.getIsEnabled() != null ? config.getIsEnabled() : 1;
        if (current == enabled) return true;

        config.setIsEnabled(enabled);
        config.setUpdateUserId(userId != null ? userId : "");
        config.setUpdateUsername(username != null ? username : "");
        config.setUpdatedAt(LocalDateTime.now());

        int rows = sysConfigMapper.updateById(config);
        if (rows > 0) {
            String changeType = enabled == 1 ? "ENABLE" : "DISABLE";
            saveHistory(config, changeType,
                current == 1 ? "启用" : "禁用",
                enabled == 1 ? "启用" : "禁用",
                userId, username, enabled == 1 ? "启用配置" : "禁用配置");
            clearCache(configKey);
        }
        return rows > 0;
    }

    @Override
    public IPage<SysConfigHistory> getHistoryPage(Page<SysConfigHistory> page, Long configId,
                                                    String operatorId, String changeType,
                                                    LocalDateTime startTime, LocalDateTime endTime) {
        return historyMapper.selectHistoryPage(page, configId, operatorId, changeType, startTime, endTime);
    }

    @Override
    public void clearCache(String configKey) {
        if (configKey != null) {
            configCache.remove(configKey);
        }
    }

    @Override
    public void clearCacheByGroup(String group) {
        if (group == null) return;
        List<SysConfig> configs = getConfigsByGroup(group);
        if (configs != null) {
            for (SysConfig c : configs) {
                configCache.remove(c.getConfigKey());
            }
        }
    }

    @Override
    public void clearAllCache() {
        int count = configCache.size();
        configCache.clear();
        logger.info("清除全部配置缓存: 删除{}个key", count);
    }

    @Override
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();

        long total = sysConfigMapper.selectCount(null);

        LambdaQueryWrapper<SysConfig> enabledWrapper = new LambdaQueryWrapper<>();
        enabledWrapper.eq(SysConfig::getIsEnabled, 1);
        long enabled = sysConfigMapper.selectCount(enabledWrapper);

        LambdaQueryWrapper<SysConfig> sensitiveWrapper = new LambdaQueryWrapper<>();
        sensitiveWrapper.eq(SysConfig::getIsSensitive, 1);
        long sensitive = sysConfigMapper.selectCount(sensitiveWrapper);

        LambdaQueryWrapper<SysConfig> readonlyWrapper = new LambdaQueryWrapper<>();
        readonlyWrapper.eq(SysConfig::getIsReadonly, 1);
        long readonly = sysConfigMapper.selectCount(readonlyWrapper);

        stats.put("totalConfigs", total);
        stats.put("enabledConfigs", enabled);
        stats.put("disabledConfigs", total - enabled);
        stats.put("sensitiveConfigs", sensitive);
        stats.put("readonlyConfigs", readonly);
        stats.put("editableConfigs", total - readonly);
        stats.put("groups", getAllGroups());

        long historyTotal = 0;
        try {
            historyTotal = historyMapper.selectCount(null);
        } catch (Exception e) {
            logger.debug("获取配置历史总数统计失败: {}", e.getMessage());
        }
        stats.put("totalHistoryRecords", historyTotal);

        return stats;
    }

    private void saveHistory(SysConfig config, String changeType, String oldValue,
                               String newValue, String operatorId, String operatorName, String reason) {
        try {
            SysConfigHistory history = new SysConfigHistory();
            history.setConfigId(config.getConfigId());
            history.setConfigKey(config.getConfigKey());
            history.setOldValue(maskIfSensitive(oldValue, config));
            history.setNewValue(maskIfSensitive(newValue, config));
            history.setChangeType(changeType);
            history.setChangeReason(reason);
            history.setOperatorId(operatorId != null ? operatorId : "");
            history.setOperatorName(operatorName != null ? operatorName : "");
            history.setCreatedAt(LocalDateTime.now());
            historyMapper.insert(history);
        } catch (Exception e) {
            logger.error("保存配置变更历史失败: key={}, error={}", config.getConfigKey(), e.getMessage());
        }
    }

    private String maskIfSensitive(String value, SysConfig config) {
        if (config != null && config.getIsSensitive() != null && config.getIsSensitive() == 1) {
            return MASKED_VALUE;
        }
        return value != null ? value : "";
    }

    private String decryptIfNeeded(SysConfig config) {
        if (config == null || config.getConfigValue() == null) return null;
        if (config.getIsEncrypted() != null && config.getIsEncrypted() == 1) {
            try {
                return AesEncryptionUtil.decrypt(config.getConfigValue());
            } catch (Exception e) {
                logger.warn("解密配置失败: key={}, error={}", config.getConfigKey(), e.getMessage());
                return "[DECRYPT_FAILED]";
            }
        }
        return config.getConfigValue();
    }

    private String encryptValue(String plainValue) {
        try {
            return AesEncryptionUtil.encrypt(plainValue);
        } catch (Exception e) {
            logger.error("加密配置值失败: error={}", e.getMessage());
            throw new RuntimeException("配置值加密失败，请检查加密服务状态: " + e.getMessage(), e);
        }
    }

    private String getFromCache(String configKey) {
        ConfigCacheEntry entry = configCache.get(configKey);
        if (entry == null) {
            return null;
        }
        // 检查是否过期
        if (entry.isExpired()) {
            configCache.remove(configKey, entry);
            return null;
        }
        // __NULL__ 表示空值缓存
        if ("__NULL__".equals(entry.value)) return "";
        return entry.value;
    }

    private void setCache(String configKey, String value, long ttlMinutes) {
        String cacheVal = (value != null && !value.isEmpty()) ? value : "__NULL__";
        long expireTimeMillis = System.currentTimeMillis() + ttlMinutes * 60 * 1000;
        configCache.put(configKey, new ConfigCacheEntry(cacheVal, expireTimeMillis));
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() > 50 ? value.substring(0, 50) : value;
    }
}
