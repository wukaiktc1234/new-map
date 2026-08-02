package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.TaskQueryDTO;
import com.foodtraceability.dto.store.operation.vo.PendingTaskVO;
import com.foodtraceability.service.PendingTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * 待办事项管理控制器
 * 提供智能任务中心的RESTful API
 * 支持任务列表查询、详情查看、完成操作、批量处理等功能
 */
@RestController
@RequestMapping("/v1/store-management/tasks")
@Tag(name = "待办事项管理", description = "智能任务中心")
public class StoreManagementTaskController {

    private final PendingTaskService pendingTaskService;

    /**
     * 构造函数注入
     *
     * @param pendingTaskService 待办任务服务
     */
    public StoreManagementTaskController(PendingTaskService pendingTaskService) {
        this.pendingTaskService = pendingTaskService;
    }

    /**
     * 分页查询待办任务列表
     * 支持按任务类型、状态、优先级、来源类型等条件筛选
     */
    @GetMapping
    @Operation(summary = "获取任务列表", description = "分页查询当前用户的待办任务列表")
    @PreAuthorize("isAuthenticated()")
    public Result<IPage<PendingTaskVO>> getTaskList(TaskQueryDTO query, Principal principal) {
        IPage<PendingTaskVO> taskPage = pendingTaskService.getTasksByAssignee(principal.getName(), query);
        return Result.success(taskPage);
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取任务详情", description = "根据ID获取任务的完整信息")
    @PreAuthorize("isAuthenticated()")
    public Result<PendingTaskVO> getTaskDetail(@PathVariable String id) {
        PendingTaskVO task = pendingTaskService.getTaskDetail(id);
        return Result.success(task);
    }

    /**
     * 完成任务
     * 将任务状态更新为completed，记录完成时间
     */
    @PutMapping("/{id}/complete")
    @Operation(summary = "完成任务", description = "将指定任务标记为已完成")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> completeTask(@PathVariable String id, Principal principal) {
        pendingTaskService.completeTask(id, principal.getName());
        return Result.success();
    }

    /**
     * 批量完成任务
     * 单次最多处理50个任务
     */
    @PutMapping("/batch-complete")
    @Operation(summary = "批量完成任务", description = "一次性完成多个任务，最多50个")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> batchComplete(@RequestBody List<String> taskIds, Principal principal) {
        pendingTaskService.batchCompleteTasks(taskIds, principal.getName());
        return Result.success();
    }

    /**
     * 获取未读任务数量
     * 统计pending和in_progress状态的任务数量
     */
    @GetMapping("/unread-count")
    @Operation(summary = "获取未读数量", description = "获取当前用户未处理的任务数量")
    @PreAuthorize("isAuthenticated()")
    public Result<Integer> getUnreadCount(Principal principal) {
        int count = pendingTaskService.getUnreadCount(principal.getName());
        return Result.success(count);
    }

    /**
     * 获取任务联动跳转URL
     * 用于前端"立即处理"按钮点击后跳转到目标业务页面
     */
    @GetMapping("/{id}/redirect-url")
    @Operation(summary = "获取跳转URL", description = "获取任务的联动跳转链接")
    @PreAuthorize("isAuthenticated()")
    public Result<String> getRedirectUrl(@PathVariable String id) {
        String redirectUrl = pendingTaskService.getRedirectUrl(id);
        return Result.success(redirectUrl);
    }
}
