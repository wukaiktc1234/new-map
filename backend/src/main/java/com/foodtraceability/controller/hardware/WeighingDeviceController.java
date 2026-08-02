package com.foodtraceability.controller.hardware;

import com.foodtraceability.entity.WeighingRecord;
import com.foodtraceability.service.WeighingDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 称重设备控制器
 * @author example
 * @since 2026-01-08
 * @deprecated 已废弃，将在下个版本移除，由 DeviceDriver 体系替代。
 *             新系统使用 {@link com.foodtraceability.driver.ScaleDriver} 驱动实现类，
 *             通过 {@link com.foodtraceability.driver.DeviceDriverManager} 统一管理设备连接、
 *             数据读取和校准操作。旧控制器的硬件集成逻辑应下沉为 DeviceDriver 实现类。
 *             参见 docs/audit/MANAGEMENT_PRODUCT_REVIEW.md 第 4.2 节"代码冗余"。
 */
@Deprecated
@RestController
@RequestMapping("/v1/weighing")
@Tag(name = "称重设备管理", description = "电子秤设备连接、称重和校准管理接口")
public class WeighingDeviceController {


    public WeighingDeviceController(WeighingDeviceService weighingDeviceService) {
        this.weighingDeviceService = weighingDeviceService;
    }

    private final WeighingDeviceService weighingDeviceService;

    /**
     * 连接称重设备
     * @param deviceId 设备ID
     * @return 连接结果
     */
    @PostMapping("/device/{deviceId}/connect")
    @Operation(summary = "连接设备")
    @PreAuthorize("hasAuthority('device:weighing:manage') or hasAuthority('*')")
    public Map<String, Object> connectDevice(@PathVariable Long deviceId) {
        return weighingDeviceService.connectDevice(deviceId);
    }

    /**
     * 断开设备连接
     * @param deviceId 设备ID
     * @return 断开结果
     */
    @PostMapping("/device/{deviceId}/disconnect")
    @Operation(summary = "断开设备连接")
    @PreAuthorize("hasAuthority('device:weighing:manage') or hasAuthority('*')")
    public Map<String, Object> disconnectDevice(@PathVariable Long deviceId) {
        return weighingDeviceService.disconnectDevice(deviceId);
    }

    /**
     * 获取设备状态
     * @param deviceId 设备ID
     * @return 设备状态
     */
    @GetMapping("/device/{deviceId}/status")
    @Operation(summary = "获取设备状态")
    @PreAuthorize("hasAuthority('device:weighing:view') or hasAuthority('*')")
    public Map<String, Object> getDeviceStatus(@PathVariable Long deviceId) {
        return weighingDeviceService.getDeviceStatus(deviceId);
    }

    /**
     * 读取称重数据
     * @param deviceId 设备ID
     * @return 称重数据
     */
    @GetMapping("/device/{deviceId}/weight")
    @Operation(summary = "读取重量")
    @PreAuthorize("hasAuthority('device:weighing:manage') or hasAuthority('*')")
    public Map<String, Object> readWeight(@PathVariable Long deviceId) {
        return weighingDeviceService.readWeight(deviceId);
    }

    /**
     * 保存称重记录
     * @param record 称重记录
     * @return 保存结果
     */
    @PostMapping("/record")
    @Operation(summary = "保存称重记录")
    @PreAuthorize("hasAuthority('device:weighing:manage') or hasAuthority('*')")
    public Map<String, Object> saveWeighingRecord(@RequestBody WeighingRecord record) {
        return weighingDeviceService.saveWeighingRecord(record);
    }

    /**
     * 批量获取设备状态
     * @return 设备状态列表
     */
    @GetMapping("/devices/status")
    @Operation(summary = "批量获取设备状态")
    @PreAuthorize("hasAuthority('device:weighing:view') or hasAuthority('*')")
    public Map<String, Object> batchGetDeviceStatus() {
        return weighingDeviceService.batchGetDeviceStatus();
    }

    /**
     * 校准设备
     * @param deviceId 设备ID
     * @param calibrationValue 校准值
     * @return 校准结果
     */
    @PostMapping("/device/{deviceId}/calibrate")
    @Operation(summary = "校准设备")
    @PreAuthorize("hasAuthority('device:weighing:manage') or hasAuthority('*')")
    public Map<String, Object> calibrateDevice(@PathVariable Long deviceId, 
                                              @RequestParam Double calibrationValue) {
        return weighingDeviceService.calibrateDevice(deviceId, calibrationValue);
    }
}
