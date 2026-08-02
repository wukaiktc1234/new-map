package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.entity.DeviceStatusHistory;

import java.util.Date;
import java.util.List;

/**
 * 设备状态历史记录服务接口
 */
public interface DeviceStatusHistoryService extends IService<DeviceStatusHistory> {

    /**
     * 记录设备状态到历史记录
     * @param status 设备状态
     */
    void recordDeviceStatus(DeviceStatus status);

    /**
     * 按设备类型查询设备状态历史记录（后端真分页）
     * @param deviceType 设备类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param storeId 门店ID
     * @param pageSize 每页大小
     * @param pageNum 页码
     * @return 分页结果
     */
    IPage<DeviceStatusHistory> getHistoryByDeviceType(String deviceType, Date startTime, Date endTime, Long storeId, Integer pageSize, Integer pageNum);

    /**
     * 查询指定设备的状态历史记录（后端真分页）
     * @param deviceId 设备ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageSize 每页大小
     * @param pageNum 页码
     * @return 分页结果
     */
    IPage<DeviceStatusHistory> getHistoryByDeviceId(Long deviceId, Date startTime, Date endTime, Integer pageSize, Integer pageNum);
    
    /**
     * 清理指定时间之前的历史记录
     * @param beforeTime 指定时间
     * @return 清理结果
     */
    boolean cleanHistoryBeforeTime(Date beforeTime);
    
    /**
     * 统计设备状态变化次数
     * @param deviceType 设备类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param storeId 门店ID
     * @return 变化次数
     */
    int countStatusChanges(String deviceType, Date startTime, Date endTime, Long storeId);
    
    /**
     * 获取过去7天的设备状态历史记录
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 历史记录列表
     */
    List<DeviceStatusHistory> getLast7DaysHistory(String deviceType, Long storeId);
    
    /**
     * 获取过去24小时的设备状态历史记录
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 历史记录列表
     */
    List<DeviceStatusHistory> getLast24HoursHistory(String deviceType, Long storeId);
}
