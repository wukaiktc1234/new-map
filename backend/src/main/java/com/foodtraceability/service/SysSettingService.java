package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.SysSetting;
import com.foodtraceability.common.Result;

import java.util.List;
import java.util.Map;

/**
 * 系统设置Service接口
 */
public interface SysSettingService extends IService<SysSetting> {

    /**
     * 根据分组获取设置列表
     */
    Result<List<SysSetting>> getSettingsByGroup(String group);

    /**
     * 获取设置值（自动按优先级查找：user > store > global）
     */
    Result<String> getSettingValue(String key, String targetType, String targetId);

    /**
     * 设置全局配置
     */
    Result<Void> setGlobalSetting(String key, String value, String currentUser);

    /**
     * 设置门店配置
     */
    Result<Void> setStoreSetting(String key, String value, Long storeId, String currentUser);

    /**
     * 设置用户配置
     */
    Result<Void> setUserSetting(String key, String value, String userId, String currentUser);

    /**
     * 批量获取设置
     */
    Result<Map<String, String>> getSettingsBatch(List<String> keys, String targetType, String targetId);

    /**
     * 初始化默认设置
     */
    Result<Void> initDefaultSettings();
}
