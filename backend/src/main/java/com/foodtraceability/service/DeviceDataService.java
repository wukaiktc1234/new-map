package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.DeviceData;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 设备数据服务接口
 * 负责设备数据的CRUD操作和统计分析
 */
public interface DeviceDataService extends IService<DeviceData> {
    
    /**
     * 获取设备最新数据
     * @param deviceId 设备ID
     * @return 设备最新数据
     */
    DeviceData getLatestDeviceData(String deviceId);
    
    /**
     * 获取设备历史数据列表
     * @param deviceId 设备ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param page 页码
     * @param size 每页数量
     * @return 设备历史数据列表
     */
    List<DeviceData> getDeviceHistoryData(String deviceId, LocalDateTime startTime, LocalDateTime endTime, int page, int size);
    
    /**
     * 获取设备数据统计
     * @param deviceId 设备ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 设备数据统计
     */
    Map<String, Object> getDeviceDataStatistics(String deviceId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取设备连接状态统计
     * @param deviceType 设备类型（可选）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 设备连接状态统计
     */
    Map<String, Object> getConnectionStatusStatistics(String deviceType, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 清理过期设备数据
     * @param beforeTime 清理该时间之前的数据
     * @return 清理的数据条数
     */
    int cleanExpiredData(LocalDateTime beforeTime);
}