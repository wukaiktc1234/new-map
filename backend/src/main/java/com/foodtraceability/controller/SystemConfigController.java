package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/v1/system-config")
@Tag(name = "系统配置管理")
public class SystemConfigController {
    private final SystemConfigService systemConfigService;

    /**
     * 获取税务平台配置
     */
    @GetMapping("/tax-platform")
    @Operation(summary = "获取税务平台配置")
    @PreAuthorize("hasAuthority(\'system:config\') or hasAuthority(\'*\')")
    public Result<Map<String, String>> getTaxPlatformConfig() {
        return Result.success(systemConfigService.getTaxPlatformConfig());
    }

    /**
     * 保存税务平台配置
     */
    @PostMapping("/tax-platform")
    @Operation(summary = "保存税务平台配置")
    @PreAuthorize("hasAuthority(\'system:config\') or hasAuthority(\'*\')")
    public Result<Void> saveTaxPlatformConfig(@RequestBody Map<String, String> config) {
        systemConfigService.saveTaxPlatformConfig(config);
        return Result.success();
    }

    /**
     * 获取税务平台配置状态
     */
    @GetMapping("/tax-platform/status")
    @Operation(summary = "获取税务平台配置状态")
    @PreAuthorize("hasAuthority(\'system:config\') or hasAuthority(\'*\')")
    public Result<Map<String, Object>> getTaxPlatformStatus() {
        return Result.success(systemConfigService.getTaxPlatformStatus());
    }

    /**
     * 获取当前激活的权限模板（4 种模式：centralized-single/standard-chain/large-chain/custom）
     * 无配置时默认返回 centralized-single（集中式单店模式，当前推进模式）
     */
    @GetMapping("/permission-template")
    @Operation(summary = "获取当前激活的权限模板")
    @PreAuthorize("hasAuthority(\'system:config\') or hasAuthority(\'*\')")
    public Result<Map<String, String>> getActivePermissionTemplate() {
        Map<String, String> result = new HashMap<>();
        String code = systemConfigService.getConfigValue("permission.active_template");
        if (code == null || code.isEmpty()) {
            code = "centralized-single";
        }
        result.put("templateCode", code);
        return Result.success(result);
    }

    /**
     * 保存当前激活的权限模板（由前端权限模板页"应用模板"时同步调用，实现多终端一致）
     */
    @PutMapping("/permission-template")
    @Operation(summary = "保存当前激活的权限模板")
    @PreAuthorize("hasAuthority(\'system:config\') or hasAuthority(\'*\')")
    public Result<Void> saveActivePermissionTemplate(@RequestBody Map<String, String> body) {
        String code = body.get("templateCode");
        if (code == null || code.isEmpty()) {
            return Result.error("templateCode 不能为空");
        }
        systemConfigService.setConfigValue("permission.active_template", code, "permission", "当前激活的权限模板（4 种模式编码）", false);
        return Result.success();
    }

    /**
     * 测试税务平台连接
     */
    @PostMapping("/tax-platform/test")
    @Operation(summary = "测试税务平台连接")
    @PreAuthorize("hasAuthority(\'system:config\') or hasAuthority(\'*\')")
    public Result<Map<String, Object>> testTaxPlatformConnection() {
        Map<String, Object> result = systemConfigService.getTaxPlatformStatus();
        result.put("testTime", System.currentTimeMillis());
        return Result.success(result);
    }

    public SystemConfigController(final SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }
}
