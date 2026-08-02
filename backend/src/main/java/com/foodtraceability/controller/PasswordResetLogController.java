package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PasswordResetLogQueryRequest;
import com.foodtraceability.entity.PasswordResetLog;
import com.foodtraceability.service.PasswordResetLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 密码重置操作日志控制器
 */
@RestController
@RequestMapping("/v1/auth/password-reset-log")
@Tag(name = "密码重置日志管理", description = "密码重置操作日志查询和监控")
public class PasswordResetLogController {


    public PasswordResetLogController(PasswordResetLogService passwordResetLogService) {
        this.passwordResetLogService = passwordResetLogService;
    }

    private final PasswordResetLogService passwordResetLogService;

    /**
     * 分页查询密码重置操作日志
     */
    @PostMapping("/list")
    @Operation(summary = "分页查询密码重置操作日志")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getPageList(@RequestBody PasswordResetLogQueryRequest request) {
        return Result.success(passwordResetLogService.getPageList(request));
    }

    /**
     * 查询用户的最近操作记录
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "查询用户的最近操作记录")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getUserRecentOperations(@PathVariable Long userId, @RequestParam(defaultValue = "10") int limit) {
        List<PasswordResetLog> logs = passwordResetLogService.getUserRecentOperations(userId, limit);
        return Result.success(logs);
    }

    /**
     * 查询异常操作记录
     */
    @GetMapping("/abnormal")
    @Operation(summary = "查询异常操作记录")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getAbnormalOperations(@RequestParam LocalDateTime startTime, @RequestParam LocalDateTime endTime) {
        List<PasswordResetLog> logs = passwordResetLogService.getAbnormalOperations(startTime, endTime);
        return Result.success(logs);
    }

    /**
     * 获取密码重置统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取密码重置统计信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> getStatistics(@RequestParam LocalDateTime startTime, @RequestParam LocalDateTime endTime) {
        Map<String, Object> statistics = new HashMap<>();
        
        // 统计异常操作数量
        List<PasswordResetLog> abnormalLogs = passwordResetLogService.getAbnormalOperations(startTime, endTime);
        statistics.put("abnormalCount", abnormalLogs.size());
        
        // 统计各类异常类型
        Map<String, Long> abnormalTypeCount = new HashMap<>();
        for (PasswordResetLog log : abnormalLogs) {
            String type = log.getAbnormalType();
            abnormalTypeCount.put(type, abnormalTypeCount.getOrDefault(type, 0L) + 1);
        }
        statistics.put("abnormalTypeCount", abnormalTypeCount);
        
        return Result.success(statistics);
    }

    /**
     * 检测用户是否存在异常行为
     */
    @GetMapping("/detect-abnormal/{userId}")
    @Operation(summary = "检测用户是否存在异常行为")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> detectAbnormalBehavior(@PathVariable Long userId, 
                                             @RequestParam String operationType,
                                             @RequestParam String clientIp) {
        boolean isAbnormal = passwordResetLogService.detectAbnormalBehavior(userId, operationType, clientIp);
        return Result.success(isAbnormal);
    }
}
