package com.foodtraceability.service;

import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.entity.HardwareConfig;

/**
 * 设备驱动兼容性检查服务接口
 * 用于检查设备驱动版本与系统的兼容性
 */
public interface DeviceDriverCompatibilityService {
    
    /**
     * 检查设备驱动版本兼容性
     * @param deviceType 设备类型
     * @param firmwareVersion 设备固件版本
     * @return 兼容性检查结果：true表示兼容，false表示不兼容
     */
    boolean checkCompatibility(String deviceType, String firmwareVersion);
    
    /**
     * 获取设备的最低兼容版本
     * @param deviceType 设备类型
     * @return 最低兼容版本
     */
    String getMinCompatibleVersion(String deviceType);
    
    /**
     * 获取设备的推荐兼容版本
     * @param deviceType 设备类型
     * @return 推荐兼容版本
     */
    String getRecommendedVersion(String deviceType);
    
    /**
     * 检查设备配置的兼容性
     * @param config 设备配置
     * @return 兼容性检查结果：true表示兼容，false表示不兼容
     */
    boolean checkConfigCompatibility(HardwareConfig config);
    
    /**
     * 检查设备状态的兼容性
     * @param status 设备状态
     * @return 兼容性检查结果：true表示兼容，false表示不兼容
     */
    boolean checkStatusCompatibility(DeviceStatus status);
}