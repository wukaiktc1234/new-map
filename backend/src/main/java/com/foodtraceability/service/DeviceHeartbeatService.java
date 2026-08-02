package com.foodtraceability.service;

import com.foodtraceability.entity.HardwareConfig;

/**
 * 设备心跳检测服务接口
 * 用于定时检查设备连接状态，及时发现设备断开连接的情况
 */
public interface DeviceHeartbeatService {
    
    /**
     * 启动设备心跳检测
     */
    void startHeartbeatDetection();
    
    /**
     * 停止设备心跳检测
     */
    void stopHeartbeatDetection();
    
    /**
     * 检查单个设备的心跳状态
     * @param config 设备配置
     * @return 设备是否在线
     */
    boolean checkDeviceHeartbeat(HardwareConfig config);
    
    /**
     * 检查所有设备的心跳状态
     */
    void checkAllDevicesHeartbeat();
    
    /**
     * 添加设备到心跳检测列表
     * @param config 设备配置
     */
    void addDeviceToHeartbeatList(HardwareConfig config);
    
    /**
     * 从心跳检测列表中移除设备
     * @param config 设备配置
     */
    void removeDeviceFromHeartbeatList(HardwareConfig config);
    
    /**
     * 获取心跳检测状态
     * @return 心跳检测是否正在运行
     */
    boolean isHeartbeatRunning();
}