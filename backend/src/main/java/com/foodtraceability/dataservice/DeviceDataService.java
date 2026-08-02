package com.foodtraceability.dataservice;

import com.foodtraceability.dto.DeviceVO;
import java.util.List;
import java.util.Map;

/**
 * 设备数据服务接口
 * 提供设备数据的缓存和批量查询功能
 */
public interface DeviceDataService {

    /**
     * 批量获取设备基本信息
     * @param deviceIds 设备ID列表
     * @return 设备ID到基本信息的映射
     */
    Map<Long, DeviceVO> batchGetDeviceBasicInfo(List<Long> deviceIds);

    /**
     * 获取单个设备基本信息
     * @param deviceId 设备ID
     * @return 设备基本信息
     */
    DeviceVO getDeviceBasicInfo(Long deviceId);

    /**
     * 清除指定设备的缓存
     * @param deviceId 设备ID
     */
    void clearDeviceCache(Long deviceId);

    /**
     * 批量清除设备缓存
     * @param deviceIds 设备ID列表
     */
    void clearDeviceBatchCache(List<Long> deviceIds);

    /**
     * 清除所有设备缓存
     */
    void clearAllDeviceCache();
}
