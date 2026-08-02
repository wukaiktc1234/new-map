package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.SysSetting;
import com.foodtraceability.service.SysSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 系统设置Controller
 */
@RestController
@RequestMapping("/v1/settings")
@Tag(name = "系统设置管理", description = "系统设置相关接口")
public class SysSettingController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SysSettingController.class);
    private final SysSettingService sysSettingService;

    /**
     * 根据分组获取设置列表
     */
    @GetMapping("/groups/{group}")
    @Operation(summary = "根据分组获取设置列表", description = "获取指定分组下的所有设置项")
    public Result<List<SysSetting>> getSettingsByGroup(@Parameter(description = "设置分组") @PathVariable String group) {
        return sysSettingService.getSettingsByGroup(group);
    }

    /**
     * 获取设置值
     */
    @GetMapping("/values/{key}")
    @Operation(summary = "获取设置值", description = "根据设置键获取值，支持分层查找")
    public Result<String> getSettingValue(@Parameter(description = "设置键") @PathVariable String key, @Parameter(description = "目标类型") @RequestParam(required = false) String targetType, @Parameter(description = "目标ID") @RequestParam(required = false) String targetId) {
        return sysSettingService.getSettingValue(key, targetType, targetId);
    }

    /**
     * 批量获取设置值
     */
    @PostMapping("/values/batch")
    @Operation(summary = "批量获取设置值", description = "批量获取多个设置项的值")
    public Result<Map<String, String>> getSettingsBatch(@Parameter(description = "设置键列表") @RequestBody List<String> keys, @Parameter(description = "目标类型") @RequestParam(required = false) String targetType, @Parameter(description = "目标ID") @RequestParam(required = false) String targetId) {
        return sysSettingService.getSettingsBatch(keys, targetType, targetId);
    }

    /**
     * 设置全局配置
     */
    @PostMapping("/global/{key}")
    @Operation(summary = "设置全局配置", description = "设置全局级别的配置值")
    public Result<Void> setGlobalSetting(@Parameter(description = "设置键") @PathVariable String key, @Parameter(description = "设置值") @RequestBody String value, @Parameter(description = "当前用户") @RequestHeader(value = "X-User-Id", required = false) String currentUser) {
        log.info("【SysSettingController】设置全局配置: key={}, value={}, currentUser={}", key, value, currentUser);
        if (currentUser == null || currentUser.isEmpty()) {
            log.warn("【SysSettingController】当前用户为空，使用默认值");
            currentUser = "system";
        }
        try {
            // 处理JSON字符串（去掉外层引号）
            String processedValue = value;
            if (value != null && value.startsWith("\"") && value.endsWith("\"")) {
                try {
                    // 尝试解析JSON字符串
                    processedValue = value.substring(1, value.length() - 1);
                    // 处理转义字符
                    processedValue = processedValue.replace("\\\"", "\"").replace("\\\\", "\\");
                } catch (Exception e) {
                    log.warn("处理JSON字符串失败，使用原始值: {}", value);
                }
            }
            Result<Void> result = sysSettingService.setGlobalSetting(key, processedValue, currentUser);
            log.info("【SysSettingController】设置全局配置结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("【SysSettingController】设置全局配置失败: key={}, value={}, currentUser={}", key, value, currentUser, e);
            return Result.error("设置失败: " + e.getMessage());
        }
    }

    /**
     * 设置门店配置
     */
    @PostMapping("/store/{key}")
    @Operation(summary = "设置门店配置", description = "设置门店级别的配置值")
    public Result<Void> setStoreSetting(@Parameter(description = "设置键") @PathVariable String key, @Parameter(description = "门店ID") @RequestParam Long storeId, @Parameter(description = "设置值") @RequestBody String value, @Parameter(description = "当前用户") @RequestHeader(value = "X-User-Id", required = false) String currentUser) {
        return sysSettingService.setStoreSetting(key, value, storeId, currentUser != null ? currentUser : "system");
    }

    /**
     * 设置用户配置
     */
    @PostMapping("/user/{key}")
    @Operation(summary = "设置用户配置", description = "设置用户级别的配置值")
    public Result<Void> setUserSetting(@Parameter(description = "设置键") @PathVariable String key, @Parameter(description = "用户ID") @RequestParam String userId, @Parameter(description = "设置值") @RequestBody String value, @Parameter(description = "当前用户") @RequestHeader(value = "X-User-Id", required = false) String currentUser) {
        return sysSettingService.setUserSetting(key, value, userId, currentUser != null ? currentUser : "system");
    }

    /**
     * 初始化默认设置
     */
    @PostMapping("/init")
    @Operation(summary = "初始化默认设置", description = "初始化系统的默认设置项")
    public Result<Void> initDefaultSettings() {
        return sysSettingService.initDefaultSettings();
    }

    public SysSettingController(final SysSettingService sysSettingService) {
        this.sysSettingService = sysSettingService;
    }
}
