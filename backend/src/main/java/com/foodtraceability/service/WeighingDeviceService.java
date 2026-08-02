package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.WeighingDevice;
import com.foodtraceability.entity.WeighingRecord;
import java.util.Map;

/**
 * 称重设备服务接口
 * @author example
 * @since 2026-01-08
 */
public interface WeighingDeviceService extends IService<WeighingDevice> {

    /**
     * 连接称重设备
     * @param deviceId 设备ID
     * @return 连接结果
     */
    Map<String, Object> connectDevice(Long deviceId);

    /**
     * 断开设备连接
     * @param deviceId 设备ID
     * @return 断开结果
     */
    Map<String, Object> disconnectDevice(Long deviceId);

    /**
     * 获取设备状态
     * @param deviceId 设备ID
     * @return 设备状态
     */
    Map<String, Object> getDeviceStatus(Long deviceId);

    /**
     * 读取称重数据
     * @param deviceId 设备ID
     * @return 称重数据
     */
    Map<String, Object> readWeight(Long deviceId);

    /**
     * 保存称重记录
     * @param record 称重记录
     * @return 保存结果
     */
    Map<String, Object> saveWeighingRecord(WeighingRecord record);

    /**
     * 批量获取设备状态
     * @return 设备状态列表
     */
    Map<String, Object> batchGetDeviceStatus();

    /**
     * 校准设备
     * @param deviceId 设备ID
     * @param calibrationValue 校准值
     * @return 校准结果
     */
    Map<String, Object> calibrateDevice(Long deviceId, Double calibrationValue);
}
