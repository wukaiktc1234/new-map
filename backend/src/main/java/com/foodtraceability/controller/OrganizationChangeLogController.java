package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.OrganizationChangeLog;
import com.foodtraceability.service.OrganizationChangeLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织架构变更日志控制器
 */
@Tag(name = "组织架构变更日志", description = "组织架构变更日志相关接口")
@RestController
@RequestMapping("/v1/organization/changes")
public class OrganizationChangeLogController {


    public OrganizationChangeLogController(OrganizationChangeLogService organizationChangeLogService) {
        this.organizationChangeLogService = organizationChangeLogService;
    }

    private final OrganizationChangeLogService organizationChangeLogService;

    /**
     * 获取变更日志列表
     */
    @Operation(summary = "获取变更日志列表", description = "获取所有变更日志")
    @GetMapping
    public Result<List<OrganizationChangeLog>> getChangeLogs() {
        List<OrganizationChangeLog> logs = organizationChangeLogService.list();
        return Result.success(logs);
    }

    /**
     * 根据变更类型获取日志
     */
    @Operation(summary = "根据变更类型获取日志", description = "根据变更类型获取变更日志")
    @GetMapping("/type/{changeType}")
    public Result<List<OrganizationChangeLog>> getLogsByChangeType(
            @Parameter(description = "变更类型：1-部门变更，2-职位变更，3-员工变更") @PathVariable Integer changeType) {
        List<OrganizationChangeLog> logs = organizationChangeLogService.getLogsByChangeType(changeType);
        return Result.success(logs);
    }

    /**
     * 根据变更对象ID获取日志
     */
    @Operation(summary = "根据变更对象获取日志", description = "根据变更对象ID获取变更日志")
    @GetMapping("/object/{objectId}")
    public Result<List<OrganizationChangeLog>> getLogsByObjectId(
            @Parameter(description = "变更对象ID") @PathVariable Long objectId) {
        List<OrganizationChangeLog> logs = organizationChangeLogService.getLogsByObjectId(objectId);
        return Result.success(logs);
    }

    /**
     * 获取最近的变更日志
     */
    @Operation(summary = "获取最近的变更日志", description = "获取最近的变更日志")
    @GetMapping("/recent")
    public Result<List<OrganizationChangeLog>> getRecentLogs(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") Integer limit) {
        List<OrganizationChangeLog> logs = organizationChangeLogService.getRecentLogs(limit);
        return Result.success(logs);
    }

    /**
     * 获取变更统计信息
     */
    @Operation(summary = "获取变更统计信息", description = "获取变更统计信息")
    @GetMapping("/statistics")
    public Result<Object> getChangeStatistics() {
        Object statistics = organizationChangeLogService.getChangeStatistics();
        return Result.success(statistics);
    }

    /**
     * 记录部门变更日志
     */
    @Operation(summary = "记录部门变更日志", description = "记录部门变更日志")
    @PostMapping("/department")
    public Result<Long> recordDepartmentChange(
            @Parameter(description = "部门ID") @RequestParam Long departmentId,
            @Parameter(description = "部门名称") @RequestParam String departmentName,
            @Parameter(description = "变更前状态") @RequestParam String beforeChange,
            @Parameter(description = "变更后状态") @RequestParam String afterChange,
            @Parameter(description = "变更原因") @RequestParam String changeReason,
            @Parameter(description = "操作人") @RequestParam String operator) {
        Long logId = organizationChangeLogService.recordDepartmentChange(
                departmentId, departmentName, beforeChange, afterChange, changeReason, operator);
        return Result.success(logId);
    }

    /**
     * 记录职位变更日志
     */
    @Operation(summary = "记录职位变更日志", description = "记录职位变更日志")
    @PostMapping("/position")
    public Result<Long> recordPositionChange(
            @Parameter(description = "职位ID") @RequestParam Long positionId,
            @Parameter(description = "职位名称") @RequestParam String positionName,
            @Parameter(description = "变更前状态") @RequestParam String beforeChange,
            @Parameter(description = "变更后状态") @RequestParam String afterChange,
            @Parameter(description = "变更原因") @RequestParam String changeReason,
            @Parameter(description = "操作人") @RequestParam String operator) {
        Long logId = organizationChangeLogService.recordPositionChange(
                positionId, positionName, beforeChange, afterChange, changeReason, operator);
        return Result.success(logId);
    }

    /**
     * 记录员工变更日志
     */
    @Operation(summary = "记录员工变更日志", description = "记录员工变更日志")
    @PostMapping("/employee")
    public Result<Long> recordEmployeeChange(
            @Parameter(description = "员工ID") @RequestParam Long employeeId,
            @Parameter(description = "员工姓名") @RequestParam String employeeName,
            @Parameter(description = "变更前状态") @RequestParam String beforeChange,
            @Parameter(description = "变更后状态") @RequestParam String afterChange,
            @Parameter(description = "变更原因") @RequestParam String changeReason,
            @Parameter(description = "操作人") @RequestParam String operator) {
        Long logId = organizationChangeLogService.recordEmployeeChange(
                employeeId, employeeName, beforeChange, afterChange, changeReason, operator);
        return Result.success(logId);
    }
}
