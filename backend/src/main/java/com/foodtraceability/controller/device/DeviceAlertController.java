package com.foodtraceability.controller.device;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.DeviceAlertQueryDTO;
import com.foodtraceability.dto.DeviceAlertVO;
import com.foodtraceability.entity.DeviceAlert;
import com.foodtraceability.service.device.DeviceAlertService;
import com.foodtraceability.service.device.DeviceMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 设备告警控制器
 * 提供告警查询、处理、监控等API
 */
@RestController
@RequestMapping("/v1/device-alerts")
@Tag(name = "设备告警管理", description = "设备告警相关接口")
public class DeviceAlertController {

    private static final Logger log = LoggerFactory.getLogger(DeviceAlertController.class);

    private final DeviceAlertService deviceAlertService;
    private final DeviceMonitorService deviceMonitorService;

    public DeviceAlertController(DeviceAlertService deviceAlertService,
                                DeviceMonitorService deviceMonitorService) {
        this.deviceAlertService = deviceAlertService;
        this.deviceMonitorService = deviceMonitorService;
    }

    // ==================== 告警查询接口 ====================

    @GetMapping("/{alertId}")
    @Operation(summary = "查询告警详情")
    @PreAuthorize("hasAuthority('device:alert:view') or hasAuthority('*')")
    public Result<DeviceAlertVO> getAlertById(
            @Parameter(description = "告警ID") @PathVariable Long alertId) {
        log.info("查询告警: alertId={}", alertId);
        return deviceAlertService.getAlertById(alertId);
    }

    // ==================== 告警基础CRUD接口 ====================

    @PostMapping
    @Operation(summary = "创建设备告警")
    @PreAuthorize("hasAuthority('device:alert:manage') or hasAuthority('*')")
    public Result<DeviceAlertVO> createAlert(@RequestBody DeviceAlert alert) {
        log.info("创建设备告警: deviceId={}, alertType={}", alert.getDeviceId(), alert.getAlertType());
        return deviceAlertService.save(alert);
    }

    @DeleteMapping("/{alertId}")
    @Operation(summary = "删除告警")
    @PreAuthorize("hasAuthority('device:alert:manage') or hasAuthority('*')")
    public Result<Void> deleteAlert(
            @Parameter(description = "告警ID") @PathVariable Long alertId) {
        log.info("删除告警: alertId={}", alertId);
        return deviceAlertService.removeById(alertId);
    }

    @PutMapping("/{alertId}")
    @Operation(summary = "更新告警信息")
    @PreAuthorize("hasAuthority('device:alert:manage') or hasAuthority('*')")
    public Result<DeviceAlertVO> updateAlert(
            @Parameter(description = "告警ID") @PathVariable Long alertId,
            @RequestBody DeviceAlert alert) {
        alert.setAlertId(alertId);
        log.info("更新告警: alertId={}", alertId);
        return deviceAlertService.updateById(alert);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询告警列表")
    @PreAuthorize("hasAuthority('device:alert:view') or hasAuthority('*')")
    public Result<IPage<DeviceAlertVO>> getAlertPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "设备ID") @RequestParam(required = false) Long deviceId,
            @Parameter(description = "告警类型") @RequestParam(required = false) Integer alertType,
            @Parameter(description = "告警级别") @RequestParam(required = false) Integer alertLevel,
            @Parameter(description = "是否已处理") @RequestParam(required = false) Boolean isHandled,
            @Parameter(description = "告警状态：0未处理 1处理中 2已解决 3已忽略") @RequestParam(required = false) Integer alertStatus,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {

        DeviceAlertQueryDTO queryDto = new DeviceAlertQueryDTO();
        queryDto.setDeviceId(deviceId);
        queryDto.setAlertType(alertType);
        queryDto.setAlertLevel(alertLevel);
        queryDto.setIsHandled(isHandled);
        queryDto.setAlertStatus(alertStatus);
        queryDto.setStartTime(startTime);
        queryDto.setEndTime(endTime);

        Page<?> pageParam = new Page<>(page, size);
        return deviceAlertService.getAlertPage(pageParam, queryDto);
    }

    @GetMapping("/device/{deviceId}/unhandled")
    @Operation(summary = "查询设备的未处理告警")
    @PreAuthorize("hasAuthority('device:alert:view') or hasAuthority('*')")
    public Result<List<DeviceAlertVO>> getUnhandledAlertsByDevice(
            @Parameter(description = "设备ID") @PathVariable Long deviceId) {
        log.info("查询未处理告警: deviceId={}", deviceId);
        return deviceAlertService.getUnhandledAlertsByDevice(deviceId);
    }

    @GetMapping("/unhandled")
    @Operation(summary = "查询所有未处理告警")
    @PreAuthorize("hasAuthority('device:alert:view') or hasAuthority('*')")
    public Result<List<DeviceAlertVO>> getAllUnhandledAlerts() {
        log.info("查询所有未处理告警");
        return deviceAlertService.getAllUnhandledAlerts();
    }

    // ==================== 告警处理接口 ====================

    @PutMapping("/{alertId}/handle")
    @Operation(summary = "处理告警")
    @PreAuthorize("hasAuthority('device:alert:manage') or hasAuthority('*')")
    public Result<Void> handleAlert(
            @Parameter(description = "告警ID") @PathVariable Long alertId,
            @RequestBody Map<String, String> request) {
        String handleResult = request.getOrDefault("handleResult", "已处理");
        log.info("处理告警: alertId={}, result={}", alertId, handleResult);
        return deviceAlertService.handleAlert(alertId, handleResult);
    }

    @PutMapping("/batch-handle")
    @Operation(summary = "批量处理告警")
    @PreAuthorize("hasAuthority('device:alert:manage') or hasAuthority('*')")
    public Result<Void> batchHandleAlerts(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> alertIds = (List<Long>) request.get("alertIds");
        String handleResult = (String) request.getOrDefault("handleResult", "批量处理完成");

        if (alertIds == null || alertIds.isEmpty()) {
            return Result.error("告警ID列表不能为空");
        }

        log.info("批量处理告警: 数量={}", alertIds.size());
        return deviceAlertService.batchHandleAlerts(alertIds, handleResult);
    }

    // ==================== 统计接口 ====================

    @GetMapping("/statistics")
    @Operation(summary = "获取告警统计信息")
    @PreAuthorize("hasAuthority('device:alert:view') or hasAuthority('*')")
    public Result<Object> getAlertStatistics() {
        log.info("获取告警统计信息");
        return deviceAlertService.getAlertStatistics();
    }

    // ==================== 监控接口 ====================

    @PostMapping("/monitor/check")
    @Operation(summary = "手动触发设备状态检查")
    @PreAuthorize("hasAuthority('device:alert:manage') or hasAuthority('*')")
    public Result<Map<String, Object>> checkDeviceStatus() {
        log.info("手动触发设备状态检查");
        return deviceMonitorService.checkDeviceStatus();
    }

    @GetMapping("/monitor/statistics")
    @Operation(summary = "获取设备统计信息")
    @PreAuthorize("hasAuthority('device:alert:view') or hasAuthority('*')")
    public Result<Map<String, Long>> getDeviceStatistics() {
        log.info("获取设备统计信息");
        return deviceMonitorService.getDeviceStatistics();
    }

    @GetMapping("/monitor/recent-changes")
    @Operation(summary = "获取最近的状态变更记录")
    @PreAuthorize("hasAuthority('device:alert:view') or hasAuthority('*')")
    public Result<List<Map<String, Object>>> getRecentStatusChanges(
            @Parameter(description = "最近N分钟") @RequestParam(defaultValue = "60") int minutes,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "50") int limit) {
        log.info("查询最近状态变更: 分钟={}, 限制={}", minutes, limit);
        return deviceMonitorService.getRecentStatusChanges(minutes, limit);
    }

    @PostMapping("/monitor/online/{deviceId}")
    @Operation(summary = "模拟设备上线（测试用）", description = "仅供开发环境使用，生产环境应禁用此接口或通过网关屏蔽")
    @PreAuthorize("hasAuthority('device:alert:manage') or hasAuthority('*')")
    public Result<Void> handleDeviceOnline(
            @Parameter(description = "设备ID") @PathVariable Long deviceId) {
        // 安全提示：此接口仅供开发环境模拟设备上线事件使用，生产环境应通过网关或权限策略禁用。
        log.info("设备上线事件: deviceId={}", deviceId);
        return deviceMonitorService.handleDeviceOnline(deviceId);
    }

    @PostMapping("/monitor/offline/{deviceId}")
    @Operation(summary = "模拟设备离线（测试用）", description = "仅供开发环境使用，生产环境应禁用此接口或通过网关屏蔽")
    @PreAuthorize("hasAuthority('device:alert:manage') or hasAuthority('*')")
    public Result<Void> handleDeviceOffline(
            @Parameter(description = "设备ID") @PathVariable Long deviceId) {
        // 安全提示：此接口仅供开发环境模拟设备离线事件使用，生产环境应通过网关或权限策略禁用。
        log.info("设备离线事件: deviceId={}", deviceId);
        return deviceMonitorService.handleDeviceOffline(deviceId);
    }
}
