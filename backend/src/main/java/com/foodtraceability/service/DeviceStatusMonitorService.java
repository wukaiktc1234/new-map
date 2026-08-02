package com.foodtraceability.service;

import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.entity.HardwareConfig;

import java.util.List;
import java.util.Map;

/**
 * 设备状态监控服务接口
 * 负责监控设备状态变化并实时推送
 */
public interface DeviceStatusMonitorService {

    /**
     * 启动设备状态监控
     */
    void startMonitoring();

    /**
     * 停止设备状态监控
     */
    void stopMonitoring();

    /**
     * 添加设备到监控列表
     * @param deviceId 设备ID
     */
    void addDeviceToMonitor(Long deviceId);

    /**
     * 从监控列表中移除设备
     * @param deviceId 设备ID
     */
    void removeDeviceFromMonitor(Long deviceId);

    /**
     * 获取设备当前状态
     * @param deviceId 设备ID
     * @return 设备状态
     */
    DeviceStatus getDeviceStatus(Long deviceId);

    /**
     * 获取所有设备状态
     * @return 设备状态映射
     */
    Map<Long, DeviceStatus> getAllDeviceStatus();

    /**
     * 手动刷新设备状态
     * @param deviceId 设备ID
     * @return 设备状态
     */
    DeviceStatus refreshDeviceStatus(Long deviceId);

    /**
     * 批量刷新设备状态
     * @param deviceIds 设备ID列表
     * @return 设备状态映射
     */
    Map<Long, DeviceStatus> batchRefreshDeviceStatus(List<Long> deviceIds);

    /**
     * 测试设备连接
     * @param config 设备配置
     * @return 测试结果
     */
    boolean testDeviceConnection(HardwareConfig config);

    /**
     * 获取监控状态
     * @return 是否正在监控
     */
    boolean isMonitoring();

    /**
     * 获取监控设备数量
     * @return 监控设备数量
     */
    int getMonitoredDeviceCount();
}
