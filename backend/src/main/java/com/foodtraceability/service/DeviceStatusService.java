package com.foodtraceability.service;

import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.entity.HardwareConfig;

public interface DeviceStatusService {
    
    /**
     * 获取设备状态（简单状态）
     * @param deviceType 设备类型
     * @return 设备状态
     */
    Boolean getDeviceStatus(String deviceType);
    
    /**
     * 获取设备详细状态
     * @param deviceType 设备类型
     * @return 设备详细状态
     */
    DeviceStatus getDeviceDetailedStatus(String deviceType);
    
    /**
     * 异步检测设备状态
     * @param deviceType 设备类型
     */
    void asyncCheckDeviceStatus(String deviceType);
    
    /**
     * 异步检测所有设备状态
     */
    void asyncCheckAllDeviceStatus();
    
    /**
     * 从缓存或数据库获取设备配置
     * @param deviceType 设备类型
     * @return 设备配置
     */
    HardwareConfig getHardwareConfig(String deviceType);
    
    /**
     * 创建默认设备配置
     * @param deviceType 设备类型
     * @return 默认设备配置
     */
    HardwareConfig createDefaultHardwareConfig(String deviceType);
    
    /**
     * 清除设备状态缓存
     * @param deviceType 设备类型
     */
    void clearDeviceStatusCache(String deviceType);
    
    /**
     * 获取所有设备状态
     * @return 所有设备状态，key为设备类型，value为设备状态
     */
    java.util.Map<String, DeviceStatus> getAllDeviceStatus();

    /**
     * 获取所有设备配置
     * @param storeId 门店ID
     * @return 所有设备配置
     */
    java.util.List<HardwareConfig> getAllDevices(Long storeId);
}