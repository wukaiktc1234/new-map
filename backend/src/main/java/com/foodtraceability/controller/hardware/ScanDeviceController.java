package com.foodtraceability.controller.hardware;

import com.foodtraceability.entity.ScanRecord;
import com.foodtraceability.service.ScanDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 扫码设备控制器
 * @author example
 * @since 2026-01-08
 *
 * @deprecated 已废弃，将在下个版本移除，由 DeviceDriver 体系替代。
 *             新系统使用 {@link com.foodtraceability.driver.ScannerDriver} 驱动实现类，
 *             通过 {@link com.foodtraceability.driver.DeviceDriverManager} 统一管理设备连接、
 *             扫码处理和记录管理。旧控制器的硬件集成逻辑应下沉为 DeviceDriver 实现类。
 *             参见 docs/audit/MANAGEMENT_PRODUCT_REVIEW.md 第 4.2 节"代码冗余"。
 */
@Deprecated
@RestController
@RequestMapping("/v1/scan")
@Tag(name = "扫码设备管理", description = "扫码枪设备连接、扫描和记录管理接口")
public class ScanDeviceController {


    public ScanDeviceController(ScanDeviceService scanDeviceService) {
        this.scanDeviceService = scanDeviceService;
    }

    private final ScanDeviceService scanDeviceService;

    /**
     * 连接扫码设备
     * @param deviceId 设备ID
     * @return 连接结果
     */
    @PostMapping("/device/{deviceId}/connect")
    @Operation(summary = "连接设备")
    @PreAuthorize("hasAuthority('device:scan:manage') or hasAuthority('*')")
    public Map<String, Object> connectDevice(@PathVariable Long deviceId) {
        return scanDeviceService.connectDevice(deviceId);
    }

    /**
     * 断开设备连接
     * @param deviceId 设备ID
     * @return 断开结果
     */
    @PostMapping("/device/{deviceId}/disconnect")
    @Operation(summary = "断开设备连接")
    @PreAuthorize("hasAuthority('device:scan:manage') or hasAuthority('*')")
    public Map<String, Object> disconnectDevice(@PathVariable Long deviceId) {
        return scanDeviceService.disconnectDevice(deviceId);
    }

    /**
     * 获取设备状态
     * @param deviceId 设备ID
     * @return 设备状态
     */
    @GetMapping("/device/{deviceId}/status")
    @Operation(summary = "获取设备状态")
    @PreAuthorize("hasAuthority('device:scan:view') or hasAuthority('*')")
    public Map<String, Object> getDeviceStatus(@PathVariable Long deviceId) {
        return scanDeviceService.getDeviceStatus(deviceId);
    }

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
    @PostMapping("/process")
    @Operation(summary = "处理扫描")
    @PreAuthorize("hasAuthority('device:scan:manage') or hasAuthority('*')")
    public Map<String, Object> processScan(@RequestParam String scanCode,
                                          @RequestParam Long deviceId,
                                          @RequestParam String operator,
                                          @RequestParam Long operatorId,
                                          @RequestParam String location,
                                          @RequestParam String businessType) {
        return scanDeviceService.processScan(scanCode, deviceId, operator, 
                                           operatorId, location, businessType);
    }

    /**
     * 保存扫码记录
     * @param record 扫码记录
     * @return 保存结果
     */
    @PostMapping("/record")
    @Operation(summary = "保存扫描记录")
    @PreAuthorize("hasAuthority('device:scan:manage') or hasAuthority('*')")
    public Map<String, Object> saveScanRecord(@RequestBody ScanRecord record) {
        return scanDeviceService.saveScanRecord(record);
    }

    /**
     * 批量获取设备状态
     * @return 设备状态列表
     */
    @GetMapping("/devices/status")
    @Operation(summary = "批量获取设备状态")
    @PreAuthorize("hasAuthority('device:scan:view') or hasAuthority('*')")
    public Map<String, Object> batchGetDeviceStatus() {
        return scanDeviceService.batchGetDeviceStatus();
    }

    /**
     * 测试设备扫描
     * @param deviceId 设备ID
     * @return 测试结果
     */
    @PostMapping("/device/{deviceId}/test")
    @Operation(summary = "测试扫描")
    @PreAuthorize("hasAuthority('device:scan:manage') or hasAuthority('*')")
    public Map<String, Object> testScan(@PathVariable Long deviceId) {
        return scanDeviceService.testScan(deviceId);
    }

    /**
     * 重置设备今日扫描计数
     * @param deviceId 设备ID
     * @return 重置结果
     */
    @PostMapping("/device/{deviceId}/reset-count")
    @Operation(summary = "重置每日计数")
    @PreAuthorize("hasAuthority('device:scan:manage') or hasAuthority('*')")
    public Map<String, Object> resetDailyCount(@PathVariable Long deviceId) {
        return scanDeviceService.resetDailyCount(deviceId);
    }
}
