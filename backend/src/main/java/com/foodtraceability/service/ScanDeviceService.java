package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.ScanDevice;
import com.foodtraceability.entity.ScanRecord;
import java.util.Map;

/**
 * 扫码设备服务接口
 * @author example
 * @since 2026-01-08
 */
public interface ScanDeviceService extends IService<ScanDevice> {

    /**
     * 连接扫码设备
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
     * 处理扫码事件
     * @param scanCode 扫描的代码
     * @param deviceId 设备ID
     * @param operator 操作人
     * @param operatorId 操作人ID
     * @param location 扫码位置
     * @param businessType 业务类型
     * @return 扫码处理结果
     */
    Map<String, Object> processScan(String scanCode, Long deviceId, String operator, 
                                   Long operatorId, String location, String businessType);

    /**
     * 保存扫码记录
     * @param record 扫码记录
     * @return 保存结果
     */
    Map<String, Object> saveScanRecord(ScanRecord record);

    /**
     * 批量获取设备状态
     * @return 设备状态列表
     */
    Map<String, Object> batchGetDeviceStatus();

    /**
     * 测试设备扫描
     * @param deviceId 设备ID
     * @return 测试结果
     */
    Map<String, Object> testScan(Long deviceId);

    /**
     * 重置设备今日扫描计数
     * @param deviceId 设备ID
     * @return 重置结果
     */
    Map<String, Object> resetDailyCount(Long deviceId);
}
