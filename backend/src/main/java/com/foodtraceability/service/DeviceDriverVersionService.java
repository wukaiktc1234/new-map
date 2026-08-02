package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.DeviceDriverVersion;

import java.util.List;

/**
 * 设备驱动版本服务接口
 * 负责设备驱动版本的CRUD操作和版本管理
 */
public interface DeviceDriverVersionService extends IService<DeviceDriverVersion> {
    
    /**
     * 获取指定设备类型的最新驱动版本
     * @param deviceType 设备类型
     * @return 最新驱动版本
     */
    DeviceDriverVersion getLatestVersion(String deviceType);
    
    /**
     * 获取指定设备类型的所有驱动版本
     * @param deviceType 设备类型
     * @return 驱动版本列表
     */
    List<DeviceDriverVersion> getVersionsByDeviceType(String deviceType);
    
    /**
     * 获取指定设备类型的默认驱动版本
     * @param deviceType 设备类型
     * @return 默认驱动版本
     */
    DeviceDriverVersion getDefaultVersion(String deviceType);
    
    /**
     * 设置默认驱动版本
     * @param id 驱动版本ID
     * @return 是否设置成功
     */
    boolean setDefaultVersion(Long id);
    
    /**
     * 比较两个版本号的大小
     * @param version1 版本号1
     * @param version2 版本号2
     * @return 1-版本1大于版本2，0-版本1等于版本2，-1-版本1小于版本2
     */
    int compareVersions(String version1, String version2);
    
    /**
     * 检查驱动版本是否兼容
     * @param deviceType 设备类型
     * @param firmwareVersion 设备固件版本
     * @param driverVersion 驱动版本
     * @return 是否兼容
     */
    boolean checkCompatibility(String deviceType, String firmwareVersion, String driverVersion);
}