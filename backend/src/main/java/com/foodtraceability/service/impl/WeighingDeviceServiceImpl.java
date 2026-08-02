package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.WeighingDevice;
import com.foodtraceability.entity.WeighingRecord;
import com.foodtraceability.mapper.WeighingDeviceMapper;
import com.foodtraceability.mapper.WeighingRecordMapper;
import com.foodtraceability.service.WeighingDeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 称重设备服务实现类
 * @author example
 * @since 2026-01-08
 */
@Service
public class WeighingDeviceServiceImpl extends ServiceImpl<WeighingDeviceMapper, WeighingDevice> implements WeighingDeviceService {

    private static final Logger logger = LoggerFactory.getLogger(WeighingDeviceServiceImpl.class);


    public WeighingDeviceServiceImpl(WeighingDeviceMapper weighingDeviceMapper, WeighingRecordMapper weighingRecordMapper) {
        this.weighingDeviceMapper = weighingDeviceMapper;
        this.weighingRecordMapper = weighingRecordMapper;
    }

    private final WeighingDeviceMapper weighingDeviceMapper;

    private final WeighingRecordMapper weighingRecordMapper;

    // 模拟设备连接状态
    private Map<Long, Boolean> deviceConnectionStatus = new HashMap<>();

    @Override
    public Map<String, Object> connectDevice(Long deviceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            WeighingDevice device = getById(deviceId);
            if (device == null) {
                result.put("success", false);
                result.put("message", "设备不存在");
                return result;
            }

            // 模拟设备连接
            logger.info("连接设备: {} - {}", device.getDeviceCode(), device.getDeviceName());
            
            // 根据连接类型执行不同的连接逻辑
            String connectionType = device.getConnectionType();
            switch (connectionType) {
                case "TCP":
                    // 网络连接逻辑
                    logger.info("使用TCP连接: {}:{}", device.getDeviceIp(), device.getDevicePort());
                    break;
                case "SERIAL":
                    // 串口连接逻辑
                    logger.info("使用串口连接: {}", device.getSerialConfig());
                    break;
                case "USB":
                    // USB连接逻辑
                    logger.info("使用USB连接");
                    break;
                default:
                    result.put("success", false);
                    result.put("message", "不支持的连接类型");
                    return result;
            }

            // 模拟连接成功
            deviceConnectionStatus.put(deviceId, true);
            device.setStatus("ONLINE");
            updateById(device);

            result.put("success", true);
            result.put("message", "设备连接成功");
            result.put("device", device);
        } catch (Exception e) {
            logger.error("连接设备失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "连接设备失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> disconnectDevice(Long deviceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            WeighingDevice device = getById(deviceId);
            if (device == null) {
                result.put("success", false);
                result.put("message", "设备不存在");
                return result;
            }

            // 模拟设备断开
            logger.info("断开设备连接: {} - {}", device.getDeviceCode(), device.getDeviceName());
            deviceConnectionStatus.put(deviceId, false);
            device.setStatus("OFFLINE");
            updateById(device);

            result.put("success", true);
            result.put("message", "设备断开成功");
        } catch (Exception e) {
            logger.error("断开设备连接失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "断开设备连接失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getDeviceStatus(Long deviceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            WeighingDevice device = getById(deviceId);
            if (device == null) {
                result.put("success", false);
                result.put("message", "设备不存在");
                return result;
            }

            // 检查连接状态
            boolean isConnected = deviceConnectionStatus.getOrDefault(deviceId, false);
            String status = isConnected ? "ONLINE" : "OFFLINE";

            result.put("success", true);
            result.put("status", status);
            result.put("device", device);
            result.put("isConnected", isConnected);
        } catch (Exception e) {
            logger.error("获取设备状态失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取设备状态失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> readWeight(Long deviceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            WeighingDevice device = getById(deviceId);
            if (device == null) {
                result.put("success", false);
                result.put("message", "设备不存在");
                return result;
            }

            // 检查设备连接状态
            boolean isConnected = deviceConnectionStatus.getOrDefault(deviceId, false);
            if (!isConnected) {
                result.put("success", false);
                result.put("message", "设备未连接");
                return result;
            }

            // 模拟读取称重数据
            logger.info("读取设备称重数据: {} - {}", device.getDeviceCode(), device.getDeviceName());
            
            // 生成随机称重数据（实际应从设备读取）
            double weight = Math.round((Math.random() * 10000 + 100) * 10) / 10.0;
            String unit = "g";

            result.put("success", true);
            result.put("weight", weight);
            result.put("unit", unit);
            result.put("deviceId", deviceId);
            result.put("deviceCode", device.getDeviceCode());
            result.put("timestamp", LocalDateTime.now());
        } catch (Exception e) {
            logger.error("读取称重数据失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "读取称重数据失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> saveWeighingRecord(WeighingRecord record) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 设置称重时间
            if (record.getWeighTime() == null) {
                record.setWeighTime(LocalDateTime.now());
            }

            // 保存记录
            weighingRecordMapper.insert(record);

            result.put("success", true);
            result.put("message", "称重记录保存成功");
            result.put("recordId", record.getId());
        } catch (Exception e) {
            logger.error("保存称重记录失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "保存称重记录失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> batchGetDeviceStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取所有设备
            List<WeighingDevice> devices = list();
            List<Map<String, Object>> deviceStatusList = new ArrayList<>();

            for (WeighingDevice device : devices) {
                Map<String, Object> deviceStatus = new HashMap<>();
                deviceStatus.put("id", device.getId());
                deviceStatus.put("deviceCode", device.getDeviceCode());
                deviceStatus.put("deviceName", device.getDeviceName());
                deviceStatus.put("status", device.getStatus());
                deviceStatus.put("isConnected", deviceConnectionStatus.getOrDefault(device.getId(), false));
                deviceStatus.put("deviceType", device.getDeviceType());
                deviceStatus.put("location", device.getLocation());
                deviceStatusList.add(deviceStatus);
            }

            result.put("success", true);
            result.put("devices", deviceStatusList);
            result.put("total", deviceStatusList.size());
        } catch (Exception e) {
            logger.error("批量获取设备状态失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "批量获取设备状态失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> calibrateDevice(Long deviceId, Double calibrationValue) {
        Map<String, Object> result = new HashMap<>();
        try {
            WeighingDevice device = getById(deviceId);
            if (device == null) {
                result.put("success", false);
                result.put("message", "设备不存在");
                return result;
            }

            // 检查设备连接状态
            boolean isConnected = deviceConnectionStatus.getOrDefault(deviceId, false);
            if (!isConnected) {
                result.put("success", false);
                result.put("message", "设备未连接");
                return result;
            }

            // 模拟设备校准
            logger.info("校准设备: {} - {}, 校准值: {}", device.getDeviceCode(), device.getDeviceName(), calibrationValue);

            result.put("success", true);
            result.put("message", "设备校准成功");
            result.put("calibrationValue", calibrationValue);
            result.put("deviceId", deviceId);
        } catch (Exception e) {
            logger.error("校准设备失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "校准设备失败: " + e.getMessage());
        }
        return result;
    }
}
