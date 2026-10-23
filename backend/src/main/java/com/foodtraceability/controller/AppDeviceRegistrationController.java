package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.AppDeviceRegistration;
import com.foodtraceability.service.AppDeviceRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * APK 设备激活白名单控制器
 * 管理端发放激活码，APK 设备凭码绑定后才可访问业务接口
 */
@RestController
@RequestMapping("/v1/device-registrations")
@Tag(name = "APK 设备白名单", description = "激活码发放、设备激活、白名单管理")
public class AppDeviceRegistrationController {

    private final AppDeviceRegistrationService appDeviceRegistrationService;

    public AppDeviceRegistrationController(AppDeviceRegistrationService appDeviceRegistrationService) {
        this.appDeviceRegistrationService = appDeviceRegistrationService;
    }

    /**
     * 管理端：生成一次性激活码
     */
    @PostMapping("/codes")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "生成一次性激活码")
    public Result<AppDeviceRegistration> generateActivationCode() {
        return Result.success(appDeviceRegistrationService.generateActivationCode());
    }

    /**
     * APK：查询设备当前状态（无需登录，仅凭设备标识）
     */
    @GetMapping("/status")
    @Operation(summary = "查询设备白名单状态")
    public Result<String> getDeviceStatus(
            @Parameter(description = "设备唯一标识") @RequestParam String deviceId) {
        return Result.success(appDeviceRegistrationService.getDeviceStatus(deviceId));
    }

    /**
     * APK：凭激活码激活设备（无需登录，激活码即凭证）
     */
    @PostMapping("/activate")
    @Operation(summary = "设备激活")
    public Result<AppDeviceRegistration> activate(
            @Parameter(description = "激活码") @RequestParam String code,
            @Parameter(description = "设备唯一标识") @RequestParam String deviceId,
            @Parameter(description = "设备名称（可选）") @RequestParam(required = false) String deviceName) {
        return Result.success(appDeviceRegistrationService.activate(code, deviceId, deviceName));
    }

    /**
     * 管理端：分页查询设备白名单
     */
    @GetMapping
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "分页查询设备白名单")
    public Result<Page<AppDeviceRegistration>> getPage(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        return Result.success(appDeviceRegistrationService.getPage(current, size, keyword));
    }

    /**
     * 管理端：禁用设备（踢出白名单）
     */
    @PostMapping("/{registrationId}/disable")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "禁用设备")
    public Result<Void> disable(@Parameter(description = "登记ID") @PathVariable Long registrationId) {
        appDeviceRegistrationService.disable(registrationId);
        return Result.success();
    }

    /**
     * 管理端：恢复启用设备
     */
    @PostMapping("/{registrationId}/enable")
    @PreAuthorize("hasRole('admin')")
    @Operation(summary = "恢复启用设备")
    public Result<Void> enable(@Parameter(description = "登记ID") @PathVariable Long registrationId) {
        appDeviceRegistrationService.enable(registrationId);
        return Result.success();
    }
}
