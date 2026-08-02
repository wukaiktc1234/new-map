package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.SysConfig;
import com.foodtraceability.entity.SysConfigHistory;
import com.foodtraceability.dto.SysConfigBatchUpdateItemDTO;

import java.util.List;
import java.util.Map;

public interface SysConfigService {

    String getValue(String configKey);

    <T> T getValue(String configKey, Class<T> type);

    Map<String, String> getValuesByGroup(String group);

    SysConfig getConfig(String configKey);

    IPage<SysConfig> getConfigPage(Page<SysConfig> page, String configKey, String configName,
                                    String configGroup, Integer isEnabled, Integer isSensitive);

    List<SysConfig> getConfigsByGroup(String group);

    List<String> getAllGroups();

    boolean updateValue(String configKey, String value, String userId, String username);

    boolean batchUpdate(List<SysConfigBatchUpdateItemDTO> updates, String userId, String username);

    boolean resetToDefault(String configKey, String userId, String username);

    boolean resetGroupToDefault(String group, String userId, String username);

    boolean enableConfig(String configKey, String userId, String username);

    boolean disableConfig(String configKey, String userId, String username);

    IPage<SysConfigHistory> getHistoryPage(Page<SysConfigHistory> page, Long configId,
                                           String operatorId, String changeType,
                                           java.time.LocalDateTime startTime,
                                           java.time.LocalDateTime endTime);

    void clearCache(String configKey);

    void clearCacheByGroup(String group);

    void clearAllCache();

    Map<String, Object> getSystemStats();
}
