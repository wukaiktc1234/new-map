package com.foodtraceability.service;

import com.foodtraceability.dto.operations.ReportConfigDTO;
import com.foodtraceability.entity.report.ReportConfig;

import java.util.List;
import java.util.Map;

/**
 * 报表配置服务接口
 * 提供报表用户配置的增删改查
 */
public interface ReportConfigService {

    /**
     * 获取当前用户的报表配置
     * @param userId 用户ID
     * @param reportType 报表类型
     * @return 配置Map
     */
    Map<String, String> getUserConfig(Long userId, Integer reportType);

    /**
     * 保存或更新报表配置
     * @param userId 用户ID
     * @param reportType 报表类型
     * @param configKey 配置项key
     * @param configValue 配置值
     */
    void saveConfig(Long userId, Integer reportType, String configKey, String configValue);

    /**
     * 批量保存配置
     * @param userId 用户ID
     * @param reportType 报表类型
     * @param configs 配置Map
     */
    void batchSaveConfig(Long userId, Integer reportType, Map<String, String> configs);

    /**
     * 获取默认配置
     * @param reportType 报表类型
     * @return 默认配置Map
     */
    Map<String, String> getDefaultConfig(Integer reportType);

    /**
     * 删除配置
     * @param userId 用户ID
     * @param reportType 报表类型
     * @param configKey 配置项key
     */
    void deleteConfig(Long userId, Integer reportType, String configKey);
}
