package com.foodtraceability.controller.device;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.Device;
import com.foodtraceability.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设备管理控制器
 * 提供设备CRUD、状态管理、心跳等API
 */
@RestController
@RequestMapping("/v1/devices")
@Tag(name = "设备管理", description = "设备管理相关接口（新系统）")
public class DeviceController {

    private static final Logger log = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    @Operation(summary = "创建设备")
    @PreAuthorize("hasAuthority('device:manage:manage') or hasAuthority('*')")
    public Result<DeviceVO> createDevice(@Valid @RequestBody DeviceCreateDTO dto) {
        log.info("创建设备请求: deviceCode={}, deviceName={}", dto.getDeviceCode(), dto.getDeviceName());
        return deviceService.createDevice(dto);
    }

    @PutMapping("/{deviceId}")
    @Operation(summary = "更新设备")
    @PreAuthorize("hasAuthority('device:manage:manage') or hasAuthority('*')")
    public Result<DeviceVO> updateDevice(
            @Parameter(description = "设备ID") @PathVariable Long deviceId,
            @Valid @RequestBody DeviceUpdateDTO dto) {
        log.info("更新设备请求: deviceId={}", deviceId);
        return deviceService.updateDevice(deviceId, dto);
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "删除设备")
    @PreAuthorize("hasAuthority('device:manage:manage') or hasAuthority('*')")
    public Result<Void> deleteDevice(
            @Parameter(description = "设备ID") @PathVariable Long deviceId) {
        log.info("删除设备请求: deviceId={}", deviceId);
        return deviceService.deleteDevice(deviceId);
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "查询设备详情")
    @PreAuthorize("hasAuthority('device:manage:view') or hasAuthority('*')")
    public Result<DeviceVO> getDeviceById(
            @Parameter(description = "设备ID") @PathVariable Long deviceId) {
        log.info("查询设备详情: deviceId={}", deviceId);
        return deviceService.getDeviceById(deviceId);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询设备列表")
    @PreAuthorize("hasAuthority('device:manage:view') or hasAuthority('*')")
    public Result<IPage<DeviceVO>> getDevicePage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "设备类型") @RequestParam(required = false) Integer deviceType,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "连接类型") @RequestParam(required = false) Integer connectionType,
            @Parameter(description = "设备名称(模糊)") @RequestParam(required = false) String deviceName,
            @Parameter(description = "设备编号(模糊)") @RequestParam(required = false) String deviceCode) {

        DeviceQueryDTO queryDto = new DeviceQueryDTO();
        queryDto.setDeviceType(deviceType);
        queryDto.setStoreId(storeId);
        queryDto.setStatus(status);
        queryDto.setConnectionType(connectionType);
        queryDto.setDeviceName(deviceName);
        queryDto.setDeviceCode(deviceCode);

        Page<Device> pageParam = new Page<>(page, size);
        return deviceService.getDevicePage(pageParam, queryDto);
    }

    @GetMapping("/list")
    @Operation(summary = "查询设备列表")
    @PreAuthorize("hasAuthority('device:manage:view') or hasAuthority('*')")
    public Result<List<DeviceVO>> getDeviceList(
            @Parameter(description = "设备类型") @RequestParam(required = false) Integer deviceType,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {

        DeviceQueryDTO queryDto = new DeviceQueryDTO();
        queryDto.setDeviceType(deviceType);
        queryDto.setStoreId(storeId);
        queryDto.setStatus(status);

        return deviceService.getDeviceList(queryDto);
    }

    @GetMapping("/online")
    @Operation(summary = "查询在线设备列表")
    @PreAuthorize("hasAuthority('device:manage:view') or hasAuthority('*')")
    public Result<List<DeviceVO>> getOnlineDevices() {
        log.info("查询在线设备列表");
        return deviceService.getOnlineDevices();
    }

    @GetMapping("/type/{deviceType}")
    @Operation(summary = "按类型查询设备")
    @PreAuthorize("hasAuthority('device:manage:view') or hasAuthority('*')")
    public Result<List<DeviceVO>> getDevicesByType(
            @Parameter(description = "设备类型") @PathVariable Integer deviceType) {
        log.info("按类型查询设备: deviceType={}", deviceType);
        return deviceService.getDevicesByType(deviceType);
    }

    @PutMapping("/{deviceId}/status")
    @Operation(summary = "更新设备状态")
    @PreAuthorize("hasAuthority('device:manage:manage') or hasAuthority('*')")
    public Result<Void> updateDeviceStatus(
            @Parameter(description = "设备ID") @PathVariable Long deviceId,
            @RequestBody Map<String, Integer> request) {
        Integer status = request.get("status");
        log.info("更新设备状态: deviceId={}, status={}", deviceId, status);
        return deviceService.updateDeviceStatus(deviceId, status);
    }

    @PostMapping("/{deviceId}/heartbeat")
    @Operation(summary = "更新设备心跳")
    @PreAuthorize("hasAuthority('device:manage:manage') or hasAuthority('*')")
    public Result<Void> updateHeartbeat(
            @Parameter(description = "设备ID") @PathVariable Long deviceId) {
        log.debug("设备心跳: deviceId={}", deviceId);
        return deviceService.updateHeartbeat(deviceId);
    }

    @PutMapping("/{deviceId}/toggle")
    @Operation(summary = "启用/停用设备")
    @PreAuthorize("hasAuthority('device:manage:manage') or hasAuthority('*')")
    public Result<Void> toggleDeviceEnabled(
            @Parameter(description = "设备ID") @PathVariable Long deviceId,
            @RequestParam boolean enabled) {
        log.info("{}设备: deviceId={}", enabled ? "启用" : "停用", deviceId);
        return deviceService.toggleDeviceEnabled(deviceId, enabled);
    }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试设备连接")
    @PreAuthorize("hasAuthority('device:manage:manage') or hasAuthority('*')")
    public Result<Void> testDevice(@Parameter(description = "设备ID") @PathVariable Long id) {
        log.info("测试设备连接，ID：{}", id);
        deviceService.updateHeartbeat(id);
        return Result.success();
    }
}
