package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.LoginLogQueryRequest;
import com.foodtraceability.entity.LoginLog;
import com.foodtraceability.service.LoginLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "登录日志管理", description = "登录日志查询接口")
@RestController
@RequestMapping("/v1/login-logs")
public class LoginLogController {


    public LoginLogController(LoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    private final LoginLogService loginLogService;

    @Operation(summary = "分页查询登录日志")
    @PostMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Result<IPage<LoginLog>> getLoginLogs(@RequestBody LoginLogQueryRequest request) {
        IPage<LoginLog> logs = loginLogService.getLoginLogs(request);
        return Result.success(logs);
    }

    @Operation(summary = "获取登录日志详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Result<LoginLog> getLoginLogById(@PathVariable Long id) {
        LoginLog log = loginLogService.getLoginLogById(id);
        return Result.success(log);
    }

    @Operation(summary = "导出登录日志（Excel）")
    @PostMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Result<String> exportLoginLogs(@RequestBody LoginLogQueryRequest request) {
        return Result.success("导出功能开发中");
    }
}