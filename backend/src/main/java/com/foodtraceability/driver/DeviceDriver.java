package com.foodtraceability.driver;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.DeviceStatus;

import java.util.Map;

/**
 * 设备驱动抽象接口
 * 定义设备驱动的统一接口
 */
public interface DeviceDriver {
    
    /**
     * 初始化设备驱动
     * @param config 设备配置
     * @return 初始化结果
     */
    boolean init(HardwareConfig config);
    
    /**
     * 连接设备
     * @return 连接结果
     */
    boolean connect();
    
    /**
     * 断开设备连接
     */
    void disconnect();
    
    /**
     * 检查设备连接状态
     * @return 连接状态
     */
    boolean isConnected();
    
    /**
     * 获取设备状态
     * @return 设备状态
     */
    DeviceStatus getDeviceStatus();
    
    /**
     * 执行设备操作
     * @param operationType 操作类型
     * @param params 操作参数
     * @return 操作结果
     */
    Map<String, Object> executeOperation(String operationType, Map<String, Object> params);
    
    /**
     * 获取设备驱动名称
     * @return 驱动名称
     */
    String getDriverName();
    
    /**
     * 获取设备驱动版本
     * @return 驱动版本
     */
    String getDriverVersion();
    
    /**
     * 获取支持的设备类型
     * @return 设备类型
     */
    String getSupportedDeviceType();
    
    /**
     * 检查设备是否支持此驱动
     * @param config 设备配置
     * @return 是否支持
     */
    boolean isSupported(HardwareConfig config);
    
    /**
     * 关闭驱动，释放资源
     */
    void close();
}