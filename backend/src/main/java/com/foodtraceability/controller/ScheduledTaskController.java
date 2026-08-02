package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.annotation.AuditLog;
import com.foodtraceability.annotation.OperationType;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.ScheduledTaskCreateDTO;
import com.foodtraceability.dto.ScheduledTaskQueryDTO;
import com.foodtraceability.dto.ScheduledTaskUpdateDTO;
import com.foodtraceability.entity.ScheduledTask;
import com.foodtraceability.entity.TaskExecutionLog;
import com.foodtraceability.entity.TaskExecutionLogDetail;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.ScheduledTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务管理 Controller
 * 提供任务的完整生命周期管理 REST API（20个端点）
 */
@Tag(name = "定时任务管理", description = "定时任务的增删改查、生命周期控制、执行日志查询、统计仪表盘")
@RestController
@RequestMapping("/v1/scheduler")
public class ScheduledTaskController {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskController.class);


    public ScheduledTaskController(ScheduledTaskService scheduledTaskService) {
        this.scheduledTaskService = scheduledTaskService;
    }

    private final ScheduledTaskService scheduledTaskService;

    // ==================== 端点1: 分页列表 ====================

    @Operation(summary = "分页查询任务列表")
    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('scheduler:task:query')")
    public Result<IPage<ScheduledTask>> getTaskList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "任务名称(模糊)") @RequestParam(required = false) String taskName,
            @Parameter(description = "任务编码(模糊)") @RequestParam(required = false) String taskCode,
            @Parameter(description = "任务分组") @RequestParam(required = false) String taskGroup,
            @Parameter(description = "任务状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "任务类型") @RequestParam(required = false) Integer taskType,
            @Parameter(description = "是否内置") @RequestParam(required = false) Integer isBuiltin) {

        try {
            if (current == null || current < 1) current = 1;
            if (size == null || size < 1 || size > 100) size = 20;

            Page<ScheduledTask> page = new Page<>(current, size);
            ScheduledTaskQueryDTO query = new ScheduledTaskQueryDTO();
            query.setTaskName(taskName);
            query.setTaskCode(taskCode);
            query.setTaskGroup(taskGroup);
            query.setStatus(status);
            query.setTaskType(taskType);
            query.setIsBuiltin(isBuiltin);

            IPage<ScheduledTask> result = scheduledTaskService.page(page, query);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("查询任务列表失败", e);
            return Result.error("查询任务列表失败");
        }
    }

    // ==================== 端点2: 任务详情 ====================

    @Operation(summary = "获取任务详情")
    @GetMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('scheduler:task:query')")
    public Result<ScheduledTask> getTaskDetail(
            @Parameter(description = "任务ID") @PathVariable Long id) {
        try {
            ScheduledTask task = scheduledTaskService.getById(id);
            if (task == null) {
                return Result.error(404, "任务不存在");
            }
            return Result.success(task);
        } catch (Exception e) {
            logger.error("获取任务详情失败: id={}", id, e);
            return Result.error("获取任务详情失败");
        }
    }

    // ==================== 端点3: 创建任务 ====================

    @Operation(summary = "创建新任务")
    @PostMapping("/tasks")
    @PreAuthorize("hasAuthority('scheduler:task:create')")
    @AuditLog(value = "创建定时任务", operationType = OperationType.CREATE, module = "定时任务")
    public Result<ScheduledTask> createTask(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Valid @RequestBody ScheduledTaskCreateDTO createDTO) {

        try {
            ScheduledTask task = scheduledTaskService.create(createDTO,
                Long.valueOf(currentUser.getUserId()), currentUser.getUsername());
            return Result.success(task);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("创建任务失败", e);
            return Result.error("创建任务失败");
        }
    }

    // ==================== 端点4: 更新任务 ====================

    @Operation(summary = "更新任务信息")
    @PutMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('scheduler:task:edit')")
    @AuditLog(value = "修改定时任务", operationType = OperationType.UPDATE, module = "定时任务")
    public Result<Void> updateTask(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "任务ID") @PathVariable Long id,
            @Valid @RequestBody ScheduledTaskUpdateDTO updateDTO) {

        try {
            boolean success = scheduledTaskService.update(id, updateDTO,
                Long.valueOf(currentUser.getUserId()), currentUser.getUsername());
            if (success) return Result.success();
            return Result.error(404, "任务不存在或更新失败");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("更新任务失败: id={}", id, e);
            return Result.error("更新任务失败");
        }
    }

    // ==================== 端点5: 删除任务 ====================

    @Operation(summary = "删除任务（逻辑删除）")
    @DeleteMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('scheduler:task:delete')")
    @AuditLog(value = "删除定时任务", operationType = OperationType.DELETE, module = "定时任务")
    public Result<Void> deleteTask(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "任务ID") @PathVariable Long id) {

        try {
            boolean success = scheduledTaskService.delete(id);
            if (success) return Result.success();
            return Result.error(404, "任务不存在或删除失败");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("删除任务失败: id={}", id, e);
            return Result.error("删除任务失败");
        }
    }

    // ==================== 端点6: 启用任务 ====================

    @Operation(summary = "启用任务")
    @PutMapping("/tasks/{id}/enable")
    @PreAuthorize("hasAuthority('scheduler:task:edit')")
    @AuditLog(value = "启用定时任务", operationType = OperationType.SYSTEM, module = "定时任务")
    public Result<Void> enableTask(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "任务ID") @PathVariable Long id) {

        try {
            boolean success = scheduledTaskService.enable(id,
                Long.valueOf(currentUser.getUserId()), currentUser.getUsername());
            if (success) return Result.success();
            return Result.error(404, "任务不存在");
        } catch (IllegalStateException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("启用任务失败: id={}", id, e);
            return Result.error("启用任务失败");
        }
    }

    // ==================== 端点7: 禁用任务 ====================

    @Operation(summary = "禁用任务")
    @PutMapping("/tasks/{id}/disable")
    @PreAuthorize("hasAuthority('scheduler:task:edit')")
    @AuditLog(value = "禁用定时任务", operationType = OperationType.SYSTEM, module = "定时任务")
    public Result<Void> disableTask(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "任务ID") @PathVariable Long id) {

        try {
            boolean success = scheduledTaskService.disable(id,
                Long.valueOf(currentUser.getUserId()), currentUser.getUsername());
            if (success) return Result.success();
            return Result.error(404, "任务不存在");
        } catch (IllegalStateException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("禁用任务失败: id={}", id, e);
            return Result.error("禁用任务失败");
        }
    }

    // ==================== 端点8: 暂停任务 ====================

    @Operation(summary = "暂停任务")
    @PutMapping("/tasks/{id}/pause")
    @PreAuthorize("hasAuthority('scheduler:task:edit')")
    @AuditLog(value = "暂停定时任务", operationType = OperationType.SYSTEM, module = "定时任务")
    public Result<Void> pauseTask(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "任务ID") @PathVariable Long id) {

        try {
            boolean success = scheduledTaskService.pause(id,
                Long.valueOf(currentUser.getUserId()), currentUser.getUsername());
            if (success) return Result.success();
            return Result.error(404, "任务不存在");
        } catch (IllegalStateException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("暂停任务失败: id={}", id, e);
            return Result.error("暂停任务失败");
        }
    }

    // ==================== 端点9: 恢复任务 ====================

    @Operation(summary = "恢复任务")
    @PutMapping("/tasks/{id}/resume")
    @PreAuthorize("hasAuthority('scheduler:task:edit')")
    @AuditLog(value = "恢复定时任务", operationType = OperationType.SYSTEM, module = "定时任务")
    public Result<Void> resumeTask(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "任务ID") @PathVariable Long id) {

        try {
            boolean success = scheduledTaskService.resume(id,
                Long.valueOf(currentUser.getUserId()), currentUser.getUsername());
            if (success) return Result.success();
            return Result.error(404, "任务不存在");
        } catch (IllegalStateException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("恢复任务失败: id={}", id, e);
            return Result.error("恢复任务失败");
        }
    }

    // ==================== 端点10: 立即触发 ====================

    @Operation(summary = "立即触发任务执行")
    @PostMapping("/tasks/{id}/trigger")
    @PreAuthorize("hasAuthority('scheduler:task:trigger')")
    @AuditLog(value = "手动触发定时任务", operationType = OperationType.SYSTEM, module = "定时任务")
    public Result<Map<String, Object>> triggerNow(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "任务ID") @PathVariable Long id) {

        try {
            ScheduledTask task = scheduledTaskService.getById(id);
            if (task == null) {
                return Result.error(404, "任务不存在");
            }
            // Phase 2将实现实际的触发逻辑，Phase 1仅返回确认信息
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", id);
            result.put("taskCode", task.getTaskCode());
            result.put("message", "触发请求已接收，任务将在调度引擎中排队执行");
            result.put("triggerTime", LocalDateTime.now().toString());
            logger.info("手动触发任务: taskId={}, operator={}", id, currentUser.getUsername());
            return Result.success(result);
        } catch (Exception e) {
            logger.error("触发任务失败: id={}", id, e);
            return Result.error("触发任务失败");
        }
    }

    // ==================== 端点11: 执行日志列表 ====================

    @Operation(summary = "获取任务执行日志")
    @GetMapping("/tasks/{id}/logs")
    @PreAuthorize("hasAuthority('scheduler:task:query')")
    public Result<IPage<TaskExecutionLog>> getExecutionLogs(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "触发类型") @RequestParam(required = false) Integer triggerType,
            @Parameter(description = "执行状态") @RequestParam(required = false) Integer executionStatus,
            @Parameter(description = "开始时间") @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        try {
            if (current == null || current < 1) current = 1;
            if (size == null || size < 1 || size > 100) size = 20;

            Page<TaskExecutionLog> page = new Page<>(current, size);
            IPage<TaskExecutionLog> result = scheduledTaskService.getExecutionLogs(page, id,
                triggerType, executionStatus, startTime, endTime);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("获取执行日志失败: taskId={}", id, e);
            return Result.error("获取执行日志失败");
        }
    }

    // ==================== 端点12: 日志详情 ====================

    @Operation(summary = "获取执行日志详情（含步骤）")
    @GetMapping("/logs/{logId}")
    @PreAuthorize("hasAuthority('scheduler:task:query')")
    public Result<Map<String, Object>> getLogDetail(
            @Parameter(description = "日志ID") @PathVariable Long logId) {

        try {
            TaskExecutionLog log = scheduledTaskService.getExecutionLogById(logId);
            if (log == null) {
                return Result.error(404, "日志记录不存在");
            }
            List<TaskExecutionLogDetail> details = scheduledTaskService.getExecutionDetails(logId);

            Map<String, Object> result = new HashMap<>();
            result.put("log", log);
            result.put("details", details != null ? details : List.of());
            return Result.success(result);
        } catch (Exception e) {
            logger.error("获取日志详情失败: logId={}", logId, e);
            return Result.error("获取日志详情失败");
        }
    }

    // ==================== 端点13: 统计数据 ====================

    @Operation(summary = "获取任务统计数据")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('scheduler:task:query')")
    public Result<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = scheduledTaskService.getStatistics();
            return Result.success(stats != null ? stats : new HashMap<>());
        } catch (Exception e) {
            logger.error("获取统计数据失败", e);
            return Result.error("获取统计数据失败");
        }
    }

    // ==================== 端点14: 仪表盘数据 ====================

    @Operation(summary = "获取仪表盘聚合数据")
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('scheduler:task:query')")
    public Result<Map<String, Object>> getDashboardData() {
        try {
            Map<String, Object> dashboard = scheduledTaskService.getDashboardData();
            return Result.success(dashboard != null ? dashboard : new HashMap<>());
        } catch (Exception e) {
            logger.error("获取仪表盘数据失败", e);
            return Result.error("获取仪表盘数据失败");
        }
    }

    // ==================== 端点15: 批量启用 ====================

    @Operation(summary = "批量启用任务")
    @PutMapping("/tasks/batch-enable")
    @PreAuthorize("hasAuthority('scheduler:task:edit')")
    @AuditLog(value = "批量启用定时任务", operationType = OperationType.SYSTEM, module = "定时任务")
    public Result<Map<String, Object>> batchEnable(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestBody List<Long> taskIds) {

        try {
            if (taskIds == null || taskIds.isEmpty()) {
                return Result.error(400, "任务ID列表不能为空");
            }
            if (taskIds.size() > 50) {
                return Result.error(400, "单次批量操作不能超过50条");
            }
            int count = scheduledTaskService.batchEnable(taskIds,
                Long.valueOf(currentUser.getUserId()), currentUser.getUsername());
            return Result.success(Map.of("requestedCount", taskIds.size(), "successCount", count));
        } catch (Exception e) {
            logger.error("批量启用任务失败", e);
            return Result.error("批量启用任务失败");
        }
    }

    // ==================== 端点16: 批量禁用 ====================

    @Operation(summary = "批量禁用任务")
    @PutMapping("/tasks/batch-disable")
    @PreAuthorize("hasAuthority('scheduler:task:edit')")
    @AuditLog(value = "批量禁用定时任务", operationType = OperationType.SYSTEM, module = "定时任务")
    public Result<Map<String, Object>> batchDisable(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestBody List<Long> taskIds) {

        try {
            if (taskIds == null || taskIds.isEmpty()) {
                return Result.error(400, "任务ID列表不能为空");
            }
            if (taskIds.size() > 50) {
                return Result.error(400, "单次批量操作不能超过50条");
            }
            int count = scheduledTaskService.batchDisable(taskIds,
                Long.valueOf(currentUser.getUserId()), currentUser.getUsername());
            return Result.success(Map.of("requestedCount", taskIds.size(), "successCount", count));
        } catch (Exception e) {
            logger.error("批量禁用任务失败", e);
            return Result.error("批量禁用任务失败");
        }
    }

    // ==================== 端点17: 批量删除 ====================

    @Operation(summary = "批量删除任务（逻辑删除）")
    @DeleteMapping("/tasks/batch-delete")
    @PreAuthorize("hasAuthority('scheduler:task:delete')")
    @AuditLog(value = "批量删除定时任务", operationType = OperationType.DELETE, module = "定时任务")
    public Result<Map<String, Object>> batchDelete(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestBody List<Long> taskIds) {

        try {
            if (taskIds == null || taskIds.isEmpty()) {
                return Result.error(400, "任务ID列表不能为空");
            }
            if (taskIds.size() > 50) {
                return Result.error(400, "单次批量操作不能超过50条");
            }
            int count = scheduledTaskService.batchDelete(taskIds);
            return Result.success(Map.of("requestedCount", taskIds.size(), "successCount", count));
        } catch (Exception e) {
            logger.error("批量删除任务失败", e);
            return Result.error("批量删除任务失败");
        }
    }

    // ==================== 端点18: 任务分组列表 ====================

    @Operation(summary = "获取所有任务分组")
    @GetMapping("/task-groups")
    @PreAuthorize("hasAuthority('scheduler:task:query')")
    public Result<List<String>> getTaskGroups() {
        try {
            List<String> groups = scheduledTaskService.getTaskGroups();
            return Result.success(groups != null ? groups : List.of());
        } catch (Exception e) {
            logger.error("获取任务分组失败", e);
            return Result.error("获取任务分组失败");
        }
    }

    // ==================== 端点19: 按编码查任务 ====================

    @Operation(summary = "按编码查询任务")
    @GetMapping("/tasks/code/{code}")
    @PreAuthorize("hasAuthority('scheduler:task:query')")
    public Result<ScheduledTask> getTaskByCode(
            @Parameter(description = "任务编码") @PathVariable String code) {
        try {
            ScheduledTask task = scheduledTaskService.getByTaskCode(code);
            if (task == null) {
                return Result.error(404, "任务编码不存在: " + code);
            }
            return Result.success(task);
        } catch (Exception e) {
            logger.error("按编码查询任务失败: code={}", code, e);
            return Result.error("按编码查询任务失败");
        }
    }

    // ==================== 端点20: 内置任务列表 ====================

    @Operation(summary = "获取所有内置任务列表")
    @GetMapping("/tasks/builtin")
    @PreAuthorize("hasAuthority('scheduler:task:query')")
    public Result<List<ScheduledTask>> getBuiltinTasks() {
        try {
            List<ScheduledTask> tasks = scheduledTaskService.getBuiltinTasks();
            return Result.success(tasks != null ? tasks : List.of());
        } catch (Exception e) {
            logger.error("获取内置任务列表失败", e);
            return Result.error("获取内置任务列表失败");
        }
    }
}
