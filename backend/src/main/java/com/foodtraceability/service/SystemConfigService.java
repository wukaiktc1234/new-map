package com.foodtraceability.service;

import com.foodtraceability.dto.SystemConfigDTO;
import com.foodtraceability.entity.SystemConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService {

    /**
     * 获取税务平台配置
     */
    Map<String, String> getTaxPlatformConfig();

    /**
     * 保存税务平台配置
     */
    void saveTaxPlatformConfig(Map<String, String> config);

    /**
     * 获取配置值
     */
    String getConfigValue(String configKey);

    /**
     * 设置配置值
     */
    void setConfigValue(String configKey, String configValue, String configType, String description, Boolean encrypted);

    /**
     * 获取所有配置
     */
    List<SystemConfig> getAllConfigs();

    /**
     * 删除配置
     */
    void deleteConfig(Long id);

    /**
     * 检查税务平台是否已配置
     */
    boolean isTaxPlatformConfigured();

    /**
     * 获取税务平台配置状态
     */
    Map<String, Object> getTaxPlatformStatus();
}
