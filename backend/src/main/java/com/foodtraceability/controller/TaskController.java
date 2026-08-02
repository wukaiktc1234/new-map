package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.*;
import com.foodtraceability.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务管理控制器
 *
 * 提供任务发布、审核、子任务管理、模板管理的完整API：
 * - B-TASK-02: 发布与查询
 * - B-TASK-03: 审核
 * - B-TASK-04: 子任务 CRUD
 * - B-TASK-05: 模板 CRUD
 */
@RestController
@RequestMapping("/v1/tasks")
@Tag(name = "任务管理", description = "任务发布、审核、子任务接口")
public class TaskController {

    private final TaskService taskService;

    /**
     * 构造函数注入依赖
     */
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // ====== B-TASK-02: 发布与查询 ======

    /**
     * 发布新任务
     */
    @PostMapping
    @Operation(summary = "发布新任务")
    public Result<TaskDetailVO> createTask(
            @Valid @RequestBody TaskCreateDTO dto,
            @AuthenticationPrincipal String userId) {
        return Result.success(taskService.createTask(dto, userId));
    }

    /**
     * 我发布的任务列表
     */
    @GetMapping("/published")
    @Operation(summary = "获取我发布的任务列表")
    public Result<PageResult<TaskVO>> getPublishedTasks(
            TaskQueryDTO queryDTO,
            @AuthenticationPrincipal String userId) {
        return Result.success(taskService.getPublishedTasks(queryDTO, userId));
    }

    /**
     * 任务详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取任务详情")
    public Result<TaskDetailVO> getTaskDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal String userId) {
        return Result.success(taskService.getTaskDetail(id, userId));
    }

    // ====== B-TASK-03: 审核 ======

    /**
     * 审核任务
     */
    @PostMapping("/{id}/review")
    @Operation(summary = "审核任务")
    public Result<Void> reviewTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskReviewDTO dto,
            @AuthenticationPrincipal String userId) {
        taskService.reviewTask(id, dto, userId);
        return Result.success();
    }

    // ====== B-TASK-04: 子任务 CRUD ======

    /**
     * 创建子任务
     */
    @PostMapping("/{id}/sub-tasks")
    @Operation(summary = "创建子任务")
    public Result<SubTaskVO> createSubTask(
            @PathVariable Long id,
            @Valid @RequestBody SubTaskCreateDTO dto,
            @AuthenticationPrincipal String userId) {
        return Result.success(taskService.createSubTask(id, dto, userId));
    }

    /**
     * 更新子任务
     */
    @PutMapping("/sub-tasks/{subTaskId}")
    @Operation(summary = "更新子任务")
    public Result<SubTaskVO> updateSubTask(
            @PathVariable Long subTaskId,
            @Valid @RequestBody SubTaskUpdateDTO dto,
            @AuthenticationPrincipal String userId) {
        return Result.success(taskService.updateSubTask(subTaskId, dto, userId));
    }

    /**
     * 删除子任务（逻辑删除）
     */
    @DeleteMapping("/sub-tasks/{subTaskId}")
    @Operation(summary = "删除子任务")
    public Result<Void> deleteSubTask(
            @PathVariable Long subTaskId,
            @AuthenticationPrincipal String userId) {
        taskService.deleteSubTask(subTaskId, userId);
        return Result.success();
    }

    /**
     * 完成子任务
     */
    @PatchMapping("/sub-tasks/{subTaskId}/complete")
    @Operation(summary = "完成子任务")
    public Result<Void> completeSubTask(
            @PathVariable Long subTaskId,
            @AuthenticationPrincipal String userId) {
        taskService.completeSubTask(subTaskId, userId);
        return Result.success();
    }

    // ====== B-TASK-05: 模板 CRUD ======

    /**
     * 模板列表
     */
    @GetMapping("/templates")
    @Operation(summary = "获取任务模板列表")
    public Result<List<TaskTemplateVO>> getTemplates(
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal String userId) {
        return Result.success(taskService.getTemplates(category));
    }

    /**
     * 创建模板（管理员）
     */
    @PostMapping("/templates")
    @Operation(summary = "创建任务模板")
    public Result<TaskTemplateVO> createTemplate(
            @Valid @RequestBody TaskTemplateCreateDTO dto,
            @AuthenticationPrincipal String userId) {
        return Result.success(taskService.createTemplate(dto, userId));
    }

    /**
     * 更新模板
     */
    @PutMapping("/templates/{templateId}")
    @Operation(summary = "更新任务模板")
    public Result<TaskTemplateVO> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody TaskTemplateUpdateDTO dto,
            @AuthenticationPrincipal String userId) {
        return Result.success(taskService.updateTemplate(templateId, dto, userId));
    }

    /**
     * 删除模板（逻辑删除）
     */
    @DeleteMapping("/templates/{templateId}")
    @Operation(summary = "删除任务模板")
    public Result<Void> deleteTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal String userId) {
        taskService.deleteTemplate(templateId, userId);
        return Result.success();
    }
}
