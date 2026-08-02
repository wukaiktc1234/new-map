package com.foodtraceability.service.device;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.DeviceAlertVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 设备监控服务接口
 * 提供设备状态监控、离线检测、告警规则检查等功能
 */
public interface DeviceMonitorService {

    /**
     * 执行设备状态检查（定时任务调用）
     * @return 检查结果，包含发现的告警数量
     */
    Result<Map<String, Object>> checkDeviceStatus();

    /**
     * 检查指定设备的离线状态
     * @param deviceId 设备ID
     * @return 是否离线
     */
    Result<Boolean> checkDeviceOffline(Long deviceId);

    /**
     * 获取所有需要检查的设备列表
     * @return 设备列表（在线状态的设备）
     */
    Result<List<Long>> getDevicesForCheck();

    /**
     * 处理设备上线事件
     * @param deviceId 设备ID
     * @return 处理结果
     */
    Result<Void> handleDeviceOnline(Long deviceId);

    /**
     * 处理设备离线事件
     * @param deviceId 设备ID
     * @return 处理结果
     */
    Result<Void> handleDeviceOffline(Long deviceId);

    /**
     * 获取设备统计信息
     * @return 统计信息（在线数、离线数、故障数等）
     */
    Result<Map<String, Long>> getDeviceStatistics();

    /**
     * 获取最近N分钟的设备状态变更记录
     * @param minutes 分钟数
     * @param limit 限制条数
     * @return 状态变更记录
     */
    Result<List<Map<String, Object>>> getRecentStatusChanges(int minutes, int limit);
}
