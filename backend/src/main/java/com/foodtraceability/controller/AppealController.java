package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.*;
import com.foodtraceability.service.AppealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 申诉管理控制器
 *
 * 提供处罚申诉与投诉举报的完整API：
 * - 创建申诉、查询列表/详情、撤回申诉、处理申诉
 */
@RestController
@RequestMapping("/v1/appeals")
@Tag(name = "申诉管理", description = "处罚申诉与投诉举报接口")
public class AppealController {

    private final AppealService appealService;

    /**
     * 构造函数注入依赖
     */
    public AppealController(AppealService appealService) {
        this.appealService = appealService;
    }

    /**
     * 创建申诉
     */
    @PostMapping
    @Operation(summary = "创建申诉")
    public Result<AppealDetailVO> createAppeal(
            @Valid @RequestBody AppealCreateDTO dto,
            @AuthenticationPrincipal String userId) {
        // TODO: 从SecurityContext获取完整的UserInfo，当前先用userId占位
        return Result.success(appealService.createAppeal(dto, userId));
    }

    /**
     * 获取我的申诉列表（分页）
     */
    @GetMapping
    @Operation(summary = "获取我的申诉列表")
    public Result<com.foodtraceability.dto.PageResult<AppealVO>> getMyAppeals(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal String userId) {
        return Result.success(appealService.getMyAppeals(userId, status, current, size));
    }

    /**
     * 获取申诉详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取申诉详情")
    public Result<AppealDetailVO> getAppealDetail(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        return Result.success(appealService.getAppealDetail(id, userId));
    }

    /**
     * 撤回申诉
     */
    @PutMapping("/{id}/withdraw")
    @Operation(summary = "撤回申诉")
    public Result<Void> withdrawAppeal(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        appealService.withdrawAppeal(id, userId);
        return Result.success();
    }

    /**
     * 处理申诉（审核人操作）
     */
    @PostMapping("/{id}/process")
    @Operation(summary = "处理申诉")
    public Result<Void> processAppeal(
            @PathVariable String id,
            @Valid @RequestBody AppealProcessDTO dto,
            @AuthenticationPrincipal String operatorId) {
        appealService.processAppeal(id, dto, operatorId);
        return Result.success();
    }
}
