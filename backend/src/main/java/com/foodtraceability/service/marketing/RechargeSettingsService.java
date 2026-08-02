package com.foodtraceability.service.marketing;

import com.foodtraceability.dto.marketing.RechargeSettingsDTO;

/**
 * 储值系统设置服务接口
 * 单行配置：所有设置以JSON形式存储在 recharge_system_settings 表
 * 固定ID为 RECHARGE_SETTINGS_001
 */
public interface RechargeSettingsService {

    /**
     * 获取系统设置
     * 如果数据库中不存在，返回默认设置
     *
     * @return 系统设置DTO
     */
    RechargeSettingsDTO getSettings();

    /**
     * 更新系统设置
     * 将整个DTO序列化为JSON存储到 config_json 字段
     *
     * @param settingsDTO 设置DTO
     */
    void updateSettings(RechargeSettingsDTO settingsDTO);
}
