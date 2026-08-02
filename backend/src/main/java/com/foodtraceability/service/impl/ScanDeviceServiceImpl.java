package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.ScanDevice;
import com.foodtraceability.entity.ScanRecord;
import com.foodtraceability.mapper.ScanDeviceMapper;
import com.foodtraceability.mapper.ScanRecordMapper;
import com.foodtraceability.service.ScanDeviceService;
import com.foodtraceability.entity.TraceCode;
import com.foodtraceability.service.TraceCodeService;
import com.foodtraceability.service.TrayService;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.Tray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 扫码设备服务实现类
 * @author example
 * @since 2026-01-08
 */
@Service
public class ScanDeviceServiceImpl extends ServiceImpl<ScanDeviceMapper, ScanDevice> implements ScanDeviceService {

    private static final Logger logger = LoggerFactory.getLogger(ScanDeviceServiceImpl.class);


    public ScanDeviceServiceImpl(ScanDeviceMapper scanDeviceMapper, ScanRecordMapper scanRecordMapper, TraceCodeService traceCodeService,
                                  @Lazy TrayService trayService) {
        this.scanDeviceMapper = scanDeviceMapper;
        this.scanRecordMapper = scanRecordMapper;
        this.traceCodeService = traceCodeService;
        this.trayService = trayService;
    }

    private final ScanDeviceMapper scanDeviceMapper;

    private final ScanRecordMapper scanRecordMapper;

    private final TraceCodeService traceCodeService;

    private final TrayService trayService;

    // 模拟设备连接状态
    private Map<Long, Boolean> deviceConnectionStatus = new HashMap<>();

    @Override
    public Map<String, Object> connectDevice(Long deviceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            ScanDevice device = getById(deviceId);
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
                case "BLUETOOTH":
                    // 蓝牙连接逻辑
                    logger.info("使用蓝牙连接");
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
            ScanDevice device = getById(deviceId);
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
            ScanDevice device = getById(deviceId);
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
            result.put("lastScanTime", device.getLastScanTime());
            result.put("todayScanCount", device.getTodayScanCount());
        } catch (Exception e) {
            logger.error("获取设备状态失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取设备状态失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> processScan(String scanCode, Long deviceId, String operator, 
                                          Long operatorId, String location, String businessType) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查设备状态
            ScanDevice device = getById(deviceId);
            if (device == null) {
                result.put("success", false);
                result.put("message", "设备不存在");
                return result;
            }

            boolean isConnected = deviceConnectionStatus.getOrDefault(deviceId, false);
            if (!isConnected) {
                result.put("success", false);
                result.put("message", "设备未连接");
                return result;
            }

            // 识别代码类型
            String codeType = identifyCodeType(scanCode);
            logger.info("处理扫码: {} (类型: {}), 设备: {}, 业务类型: {}", 
                       scanCode, codeType, device.getDeviceName(), businessType);

            // 处理不同业务类型的扫码
            String scanResult = "SUCCESS";
            String failureReason = null;
            boolean businessProcessed = false;

            if ("OUTBOUND".equals(businessType) && "TRACE_CODE".equals(codeType)) {
                // 出库扫码处理
                TraceCode traceCodeResult = traceCodeService.scanTraceCodeOutbound(
                        scanCode, operator, operatorId, location, "出库使用");
                if (traceCodeResult != null) {
                    businessProcessed = true;
                } else {
                    scanResult = "FAILED";
                    failureReason = "追溯码出库失败，可能已被使用或不存在";
                }
            } else if ("INBOUND".equals(businessType)) {
                // 入库扫码处理（可扩展）
                logger.info("入库扫码处理: {}", scanCode);
                businessProcessed = true;
            } else if ("CHECK".equals(businessType)) {
                // 盘点扫码处理（可扩展）
                logger.info("盘点扫码处理: {}", scanCode);
                businessProcessed = true;
            } else if ("SALES".equals(businessType)) {
                // 销售扫码处理（可扩展）
                logger.info("销售扫码处理: {}", scanCode);
                businessProcessed = true;
            } else if ("TRAY_KITCHEN_IN".equals(businessType) || "TRAY_KITCHEN_OUT".equals(businessType) || "TRAY_SERVE".equals(businessType)) {
                // 托盘扫码业务（5状态机）
                String operatorIdStr = operatorId != null ? String.valueOf(operatorId) : null;
                String deviceCode = device.getDeviceCode();
                if ("TRAY_KITCHEN_IN".equals(businessType)) {
                    Result<Tray> trayResult = trayService.scanKitchenIn(scanCode, deviceId, deviceCode, operatorIdStr, operator);
                    businessProcessed = trayResult.getCode() == 0;
                    if (!businessProcessed) {
                        scanResult = "FAILED";
                        failureReason = trayResult.getMessage();
                    }
                } else if ("TRAY_KITCHEN_OUT".equals(businessType)) {
                    Result<Tray> trayResult = trayService.scanKitchenOut(scanCode, deviceId, deviceCode, operatorIdStr, operator);
                    businessProcessed = trayResult.getCode() == 0;
                    if (!businessProcessed) {
                        scanResult = "FAILED";
                        failureReason = trayResult.getMessage();
                    }
                } else {
                    Result<?> serveResult = trayService.scanServe(scanCode, operatorIdStr, operator);
                    businessProcessed = serveResult.getCode() == 0;
                    if (!businessProcessed) {
                        scanResult = "FAILED";
                        failureReason = serveResult.getMessage();
                    }
                }
            }

            // 保存扫码记录
            ScanRecord scanRecord = new ScanRecord();
            scanRecord.setScanCode(scanCode);
            scanRecord.setCodeType(codeType);
            scanRecord.setDeviceId(deviceId);
            scanRecord.setDeviceCode(device.getDeviceCode());
            scanRecord.setScanTime(LocalDateTime.now());
            scanRecord.setOperator(operator);
            scanRecord.setOperatorId(operatorId);
            scanRecord.setScanLocation(location);
            scanRecord.setScanResult(scanResult);
            scanRecord.setFailureReason(failureReason);
            scanRecord.setBusinessType(businessType);
            scanRecord.setProcessStatus(businessProcessed ? "PROCESSED" : "ERROR");
            scanRecord.setCreateTime(LocalDateTime.now());
            scanRecord.setUpdateTime(LocalDateTime.now());
            scanRecord.setCreateBy(operator);
            scanRecord.setUpdateBy(operator);

            scanRecordMapper.insert(scanRecord);

            // 更新设备信息
            device.setLastScanTime(LocalDateTime.now());
            device.setTodayScanCount(device.getTodayScanCount() != null ? 
                                    device.getTodayScanCount() + 1 : 1);
            updateById(device);

            result.put("success", businessProcessed);
            result.put("message", businessProcessed ? "扫码处理成功" : "扫码处理失败");
            result.put("scanCode", scanCode);
            result.put("codeType", codeType);
            result.put("businessType", businessType);
            result.put("scanRecordId", scanRecord.getId());
            result.put("device", device);
        } catch (Exception e) {
            logger.error("处理扫码失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "处理扫码失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> saveScanRecord(ScanRecord record) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 设置默认值
            if (record.getScanTime() == null) {
                record.setScanTime(LocalDateTime.now());
            }
            if (record.getCreateTime() == null) {
                record.setCreateTime(LocalDateTime.now());
            }
            if (record.getUpdateTime() == null) {
                record.setUpdateTime(LocalDateTime.now());
            }

            // 保存记录
            scanRecordMapper.insert(record);

            result.put("success", true);
            result.put("message", "扫码记录保存成功");
            result.put("recordId", record.getId());
        } catch (Exception e) {
            logger.error("保存扫码记录失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "保存扫码记录失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> batchGetDeviceStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取所有设备
            List<ScanDevice> devices = list();
            List<Map<String, Object>> deviceStatusList = new ArrayList<>();

            for (ScanDevice device : devices) {
                Map<String, Object> deviceStatus = new HashMap<>();
                boolean isConnected = deviceConnectionStatus.getOrDefault(device.getId(), false);
                String status = isConnected ? "ONLINE" : "OFFLINE";
                
                deviceStatus.put("id", device.getId());
                deviceStatus.put("deviceCode", device.getDeviceCode());
                deviceStatus.put("deviceName", device.getDeviceName());
                deviceStatus.put("status", status);
                deviceStatus.put("isConnected", isConnected);
                deviceStatus.put("deviceType", device.getDeviceType());
                deviceStatus.put("location", device.getLocation());
                deviceStatus.put("lastScanTime", device.getLastScanTime());
                deviceStatus.put("todayScanCount", device.getTodayScanCount());
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
    public Map<String, Object> testScan(Long deviceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            ScanDevice device = getById(deviceId);
            if (device == null) {
                result.put("success", false);
                result.put("message", "设备不存在");
                return result;
            }

            boolean isConnected = deviceConnectionStatus.getOrDefault(deviceId, false);
            if (!isConnected) {
                result.put("success", false);
                result.put("message", "设备未连接");
                return result;
            }

            // 模拟测试扫描
            logger.info("测试设备扫描: {}", device.getDeviceName());
            String testCode = "TEST" + LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            result.put("success", true);
            result.put("message", "设备测试成功");
            result.put("device", device);
            result.put("testCode", testCode);
            result.put("timestamp", LocalDateTime.now());
        } catch (Exception e) {
            logger.error("测试设备扫描失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "测试设备扫描失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> resetDailyCount(Long deviceId) {
        Map<String, Object> result = new HashMap<>();
        try {
            ScanDevice device = getById(deviceId);
            if (device == null) {
                result.put("success", false);
                result.put("message", "设备不存在");
                return result;
            }

            // 重置今日扫描计数
            device.setTodayScanCount(0);
            updateById(device);

            result.put("success", true);
            result.put("message", "设备今日扫描计数已重置");
            result.put("device", device);
        } catch (Exception e) {
            logger.error("重置设备计数失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "重置设备计数失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 识别代码类型
     * @param code 代码
     * @return 代码类型
     */
    private String identifyCodeType(String code) {
        if (code == null) {
            return "UNKNOWN";
        }

        // 根据代码格式识别类型
        if (code.startsWith("TC")) {
            return "TRACE_CODE";
        } else if (code.startsWith("TRAY")) {
            return "TRAY_CODE";
        } else if (code.matches("^F\\d{5}$")) {
            return "PRODUCT_CODE";
        } else if (code.matches("^O\\d{8}$")) {
            return "ORDER_CODE";
        } else {
            return "UNKNOWN";
        }
    }
}
