package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.DeviceStatusHistory;
import com.foodtraceability.service.DeviceStatusHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 设备状态历史记录控制器
 * 提供设备状态历史记录的查询API
 */
@RestController
@RequestMapping("/v1/device-status-history")
@Tag(name = "设备状态历史记录", description = "提供设备状态历史记录的查询API")
public class DeviceStatusHistoryController {


    public DeviceStatusHistoryController(DeviceStatusHistoryService deviceStatusHistoryService) {
        this.deviceStatusHistoryService = deviceStatusHistoryService;
    }

    private final DeviceStatusHistoryService deviceStatusHistoryService;

    /**
     * 获取过去7天的设备状态历史记录
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 历史记录列表
     */
    @GetMapping("/last-7-days")
    @Operation(summary = "获取过去7天的设备状态历史记录")
    @PreAuthorize("hasAuthority('device:status-history:view') or hasAuthority('*')")
    public Result<List<DeviceStatusHistory>> getLast7DaysHistory(
            @Parameter(description = "设备类型") @RequestParam String deviceType,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId) {
        List<DeviceStatusHistory> historyList = deviceStatusHistoryService.getLast7DaysHistory(deviceType, storeId != null ? storeId : 1L);
        return Result.success(historyList);
    }

    /**
     * 获取过去24小时的设备状态历史记录
     * @param deviceType 设备类型
     * @param storeId 门店ID
     * @return 历史记录列表
     */
    @GetMapping("/last-24-hours")
    @Operation(summary = "获取过去24小时的设备状态历史记录")
    @PreAuthorize("hasAuthority('device:status-history:view') or hasAuthority('*')")
    public Result<List<DeviceStatusHistory>> getLast24HoursHistory(
            @Parameter(description = "设备类型") @RequestParam String deviceType,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId) {
        List<DeviceStatusHistory> historyList = deviceStatusHistoryService.getLast24HoursHistory(deviceType, storeId != null ? storeId : 1L);
        return Result.success(historyList);
    }

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
    @GetMapping
    @Operation(summary = "按设备类型查询设备状态历史记录")
    @PreAuthorize("hasAuthority('device:status-history:view') or hasAuthority('*')")
    public Result<IPage<DeviceStatusHistory>> getHistoryByDeviceType(
            @Parameter(description = "设备类型") @RequestParam(required = false) String deviceType,
            @Parameter(description = "开始时间") @RequestParam(required = false) Date startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) Date endTime,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum) {
        IPage<DeviceStatusHistory> page = deviceStatusHistoryService.getHistoryByDeviceType(
                deviceType, startTime, endTime, storeId != null ? storeId : 1L, pageSize, pageNum);
        return Result.success(page);
    }

    /**
     * 查询指定设备的状态历史记录（后端真分页）
     * @param deviceId 设备ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageSize 每页大小
     * @param pageNum 页码
     * @return 分页结果
     */
    @GetMapping("/device/{deviceId}")
    @Operation(summary = "查询指定设备的状态历史记录")
    @PreAuthorize("hasAuthority('device:status-history:view') or hasAuthority('*')")
    public Result<IPage<DeviceStatusHistory>> getHistoryByDeviceId(
            @Parameter(description = "设备ID") @PathVariable Long deviceId,
            @Parameter(description = "开始时间") @RequestParam(required = false) Date startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) Date endTime,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum) {
        IPage<DeviceStatusHistory> page = deviceStatusHistoryService.getHistoryByDeviceId(
                deviceId, startTime, endTime, pageSize, pageNum);
        return Result.success(page);
    }

    /**
     * 统计设备状态变化次数
     * @param deviceType 设备类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param storeId 门店ID
     * @return 状态变化次数
     */
    @GetMapping("/status-changes-count")
    @Operation(summary = "统计设备状态变化次数")
    @PreAuthorize("hasAuthority('device:status-history:view') or hasAuthority('*')")
    public Result<Integer> countStatusChanges(
            @Parameter(description = "设备类型") @RequestParam String deviceType,
            @Parameter(description = "开始时间") @RequestParam(required = false) Date startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) Date endTime,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId) {
        int count = deviceStatusHistoryService.countStatusChanges(
                deviceType, startTime, endTime, storeId != null ? storeId : 1L);
        return Result.success(count);
    }
}
