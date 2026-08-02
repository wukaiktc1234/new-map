package com.foodtraceability.controller.operations;

import com.foodtraceability.common.Result;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.ReportConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 报表配置控制器
 * 提供报表用户配置的增删改查接口
 */
@RestController
@RequestMapping("/v1/operations-reports/config")
@Tag(name = "报表配置", description = "报表用户配置管理接口")
@PreAuthorize("hasAuthority('finance:view')")
public class ReportConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ReportConfigController.class);

    private final ReportConfigService reportConfigService;

    public ReportConfigController(ReportConfigService reportConfigService) {
        this.reportConfigService = reportConfigService;
    }

    /**
     * 获取当前登录用户ID
     * @param authentication 认证信息
     * @return 用户ID
     */
    private Long getCurrentUserId(Authentication authentication) {
        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        return Long.valueOf(user.getUserId());
    }

    /**
     * 获取当前用户的报表配置
     * @param reportType 报表类型：1日报 2周报 3月报 4季报 5年报 6利润分析
     * @param authentication 认证信息
     * @return 配置Map
     */
    @GetMapping("/{reportType}")
    @Operation(summary = "获取用户报表配置", description = "获取当前用户指定报表类型的配置")
    public Result<Map<String, String>> getUserConfig(
            @Parameter(description = "报表类型：1日报 2周报 3月报 4季报 5年报 6利润分析") @PathVariable Integer reportType,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        logger.info("获取用户报表配置，用户: {}, 报表类型: {}", userId, reportType);
        Map<String, String> config = reportConfigService.getUserConfig(userId, reportType);
        return Result.success(config);
    }

    /**
     * 保存报表配置
     * @param reportType 报表类型
     * @param configKey 配置项key
     * @param configValue 配置值
     * @param authentication 认证信息
     * @return 操作结果
     */
    @PostMapping("/{reportType}")
    @PreAuthorize("hasAuthority('finance:report:config')")
    @Operation(summary = "保存报表配置", description = "保存或更新指定配置项")
    public Result<Void> saveConfig(
            @Parameter(description = "报表类型：1日报 2周报 3月报 4季报 5年报 6利润分析") @PathVariable Integer reportType,
            @Parameter(description = "配置项key") @RequestParam String configKey,
            @Parameter(description = "配置值") @RequestParam String configValue,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        logger.info("保存报表配置，用户: {}, 报表类型: {}, key: {}", userId, reportType, configKey);
        reportConfigService.saveConfig(userId, reportType, configKey, configValue);
        return Result.success();
    }

    /**
     * 批量保存报表配置
     * @param reportType 报表类型
     * @param configs 配置Map
     * @param authentication 认证信息
     * @return 操作结果
     */
    @PostMapping("/{reportType}/batch")
    @PreAuthorize("hasAuthority('finance:report:config')")
    @Operation(summary = "批量保存报表配置", description = "批量保存多个配置项")
    public Result<Void> batchSaveConfig(
            @Parameter(description = "报表类型：1日报 2周报 3月报 4季报 5年报 6利润分析") @PathVariable Integer reportType,
            @RequestBody Map<String, String> configs,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        logger.info("批量保存报表配置，用户: {}, 报表类型: {}, 数量: {}", userId, reportType, configs.size());
        reportConfigService.batchSaveConfig(userId, reportType, configs);
        return Result.success();
    }

    /**
     * 获取默认配置
     * @param reportType 报表类型
     * @return 默认配置Map
     */
    @GetMapping("/{reportType}/default")
    @Operation(summary = "获取默认配置", description = "获取指定报表类型的默认配置")
    public Result<Map<String, String>> getDefaultConfig(
            @Parameter(description = "报表类型：1日报 2周报 3月报 4季报 5年报 6利润分析") @PathVariable Integer reportType) {
        logger.info("获取默认配置，报表类型: {}", reportType);
        Map<String, String> config = reportConfigService.getDefaultConfig(reportType);
        return Result.success(config);
    }

    /**
     * 删除报表配置
     * @param reportType 报表类型
     * @param configKey 配置项key
     * @param authentication 认证信息
     * @return 操作结果
     */
    @DeleteMapping("/{reportType}")
    @PreAuthorize("hasAuthority('finance:report:config')")
    @Operation(summary = "删除报表配置", description = "删除指定配置项")
    public Result<Void> deleteConfig(
            @Parameter(description = "报表类型：1日报 2周报 3月报 4季报 5年报 6利润分析") @PathVariable Integer reportType,
            @Parameter(description = "配置项key") @RequestParam String configKey,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        logger.info("删除报表配置，用户: {}, 报表类型: {}, key: {}", userId, reportType, configKey);
        reportConfigService.deleteConfig(userId, reportType, configKey);
        return Result.success();
    }
}
