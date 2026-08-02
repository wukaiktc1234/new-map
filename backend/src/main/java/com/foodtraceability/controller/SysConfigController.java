package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.annotation.AuditLog;
import com.foodtraceability.annotation.OperationType;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.SysConfigBatchUpdateItemDTO;
import com.foodtraceability.entity.SysConfig;
import com.foodtraceability.entity.SysConfigHistory;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "系统配置管理", description = "系统配置的增删改查、缓存管理、变更历史")
@RestController
@RequestMapping("/v1/sys-config")
public class SysConfigController {

    private static final Logger logger = LoggerFactory.getLogger(SysConfigController.class);


    public SysConfigController(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    private final SysConfigService sysConfigService;

    @Operation(summary = "获取单个配置值")
    @GetMapping("/value/{configKey}")
    @PreAuthorize("isAuthenticated()")
    public Result<String> getValue(
            @Parameter(description = "配置键") @PathVariable String configKey) {
        try {
            String value = sysConfigService.getValue(configKey);
            return Result.success(value);
        } catch (Exception e) {
            logger.error("获取配置值失败: key={}", configKey, e);
            return Result.error("获取配置值失败");
        }
    }

    @Operation(summary = "按分组获取所有配置键值对")
    @GetMapping("/group/{groupName}/values")
    @PreAuthorize("hasAuthority('sys:config:query')")
    public Result<Map<String, String>> getGroupValues(
            @Parameter(description = "配置分组") @PathVariable String groupName) {
        Map<String, String> values = sysConfigService.getValuesByGroup(groupName);
        return Result.success(values != null ? values : new HashMap<>());
    }

    @Operation(summary = "获取单个配置详情(脱敏)")
    @GetMapping("/{configKey}")
    @PreAuthorize("isAuthenticated()")
    public Result<SysConfig> getConfig(
            @Parameter(description = "配置键") @PathVariable String configKey) {
        SysConfig config = sysConfigService.getConfig(configKey);
        if (config == null) {
            return Result.error(404, "配置项不存在");
        }
        return Result.success(config);
    }

    @Operation(summary = "分页查询配置列表")
    @GetMapping
    @PreAuthorize("hasAuthority('sys:config:query')")
    public Result<IPage<SysConfig>> getConfigList(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "配置键(模糊)") @RequestParam(required = false) String configKey,
            @Parameter(description = "配置名称(模糊)") @RequestParam(required = false) String configName,
            @Parameter(description = "配置分组") @RequestParam(required = false) String configGroup,
            @Parameter(description = "是否启用") @RequestParam(required = false) Integer isEnabled,
            @Parameter(description = "是否敏感") @RequestParam(required = false) Integer isSensitive) {

        try {
            if (current == null || current < 1) current = 1;
            if (size == null || size < 1 || size > 100) size = 20;

            Page<SysConfig> page = new Page<>(current, size);
            IPage<SysConfig> result = sysConfigService.getConfigPage(page,
                configKey, configName, configGroup, isEnabled, isSensitive);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("查询配置列表失败", e);
            return Result.error("查询配置列表失败");
        }
    }

    @Operation(summary = "获取所有配置分组列表")
    @GetMapping("/groups")
    @PreAuthorize("hasAuthority('sys:config:query')")
    public Result<List<String>> getGroups() {
        List<String> groups = sysConfigService.getAllGroups();
        return Result.success(groups);
    }

    @Operation(summary = "修改配置值")
    @PutMapping("/value/{configKey}")
    @PreAuthorize("hasAuthority('sys:config:edit')")
    @AuditLog(value = "修改系统配置", operationType = OperationType.UPDATE, module = "系统配置")
    public Result<Void> updateValue(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "配置键") @PathVariable String configKey,
            @RequestBody Map<String, String> body) {

        try {
            String value = body.get("value");
            if (value == null) return Result.error("配置值不能为空");

            boolean success = sysConfigService.updateValue(
                configKey, value, currentUser.getUserId(), currentUser.getUsername());
            if (success) return Result.success();
            return Result.error(404, "配置项不存在或更新失败");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalStateException | IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("修改配置失败: key={}", configKey, e);
            return Result.error("修改配置失败");
        }
    }

    @Operation(summary = "批量修改配置值")
    @PutMapping("/batch")
    @PreAuthorize("hasAuthority('sys:config:edit')")
    @AuditLog(value = "批量修改系统配置", operationType = OperationType.UPDATE, module = "系统配置")
    public Result<Map<String, Object>> batchUpdate(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestBody List<SysConfigBatchUpdateItemDTO> updates) {

        try {
            boolean success = sysConfigService.batchUpdate(updates,
                currentUser.getUserId(), currentUser.getUsername());
            if (success) {
                return Result.success(Map.of("updatedCount", updates.size()));
            }
            return Result.error("批量更新失败");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            logger.error("批量修改配置失败", e);
            return Result.error("批量修改配置失败");
        }
    }

    @Operation(summary = "重置为默认值")
    @PostMapping("/{configKey}/reset")
    @PreAuthorize("hasAuthority('sys:config:edit')")
    @AuditLog(value = "重置系统配置", operationType = OperationType.UPDATE, module = "系统配置")
    public Result<Void> resetToDefault(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "配置键") @PathVariable String configKey) {
        try {
            boolean success = sysConfigService.resetToDefault(
                configKey, currentUser.getUserId(), currentUser.getUsername());
            if (success) return Result.success();
            return Result.error(404, "配置项不存在或重置失败");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            logger.error("重置配置失败: key={}", configKey, e);
            return Result.error("重置配置失败");
        }
    }

    @Operation(summary = "重置整个分组为默认值")
    @PostMapping("/group/{groupName}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(value = "批量重置系统配置", operationType = OperationType.UPDATE, module = "系统配置")
    public Result<Void> resetGroupToDefault(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "配置分组") @PathVariable String groupName) {
        try {
            boolean success = sysConfigService.resetGroupToDefault(
                groupName, currentUser.getUserId(), currentUser.getUsername());
            if (success) return Result.success();
            return Result.error("该分组无配置可重置");
        } catch (Exception e) {
            logger.error("重置分组配置失败: group={}", groupName, e);
            return Result.error("重置失败");
        }
    }

    @Operation(summary = "启用配置")
    @PostMapping("/{configKey}/enable")
    @PreAuthorize("hasAuthority('sys:config:edit')")
    @AuditLog(value = "启用系统配置", operationType = OperationType.SYSTEM, module = "系统配置")
    public Result<Void> enableConfig(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "配置键") @PathVariable String configKey) {
        try {
            boolean success = sysConfigService.enableConfig(
                configKey, currentUser.getUserId(), currentUser.getUsername());
            if (success) return Result.success();
            return Result.error(404, "配置项不存在");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            logger.error("启用配置失败: key={}", configKey, e);
            return Result.error("启用配置失败");
        }
    }

    @Operation(summary = "禁用配置")
    @PostMapping("/{configKey}/disable")
    @PreAuthorize("hasAuthority('sys:config:edit')")
    @AuditLog(value = "禁用系统配置", operationType = OperationType.SYSTEM, module = "系统配置")
    public Result<Void> disableConfig(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "配置键") @PathVariable String configKey) {
        try {
            boolean success = sysConfigService.disableConfig(
                configKey, currentUser.getUserId(), currentUser.getUsername());
            if (success) return Result.success();
            return Result.error(404, "配置项不存在");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            logger.error("禁用配置失败: key={}", configKey, e);
            return Result.error("禁用配置失败");
        }
    }

    @Operation(summary = "查询配置变更历史")
    @GetMapping("/history")
    @PreAuthorize("hasAuthority('sys:config:query')")
    public Result<IPage<SysConfigHistory>> getHistory(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "配置ID") @RequestParam(required = false) Long configId,
            @Parameter(description = "操作人ID") @RequestParam(required = false) String operatorId,
            @Parameter(description = "变更类型") @RequestParam(required = false) String changeType,
            @Parameter(description = "开始时间") @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        try {
            if (current == null || current < 1) current = 1;
            if (size == null || size < 1 || size > 100) size = 20;
            Page<SysConfigHistory> page = new Page<>(current, size);
            IPage<SysConfigHistory> result = sysConfigService.getHistoryPage(
                page, configId, operatorId, changeType, startTime, endTime);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("查询配置历史失败", e);
            return Result.error("查询配置历史失败");
        }
    }

    @Operation(summary = "清除配置缓存")
    @DeleteMapping("/cache/{configKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> clearCache(
            @Parameter(description = "配置键") @PathVariable String configKey) {
        sysConfigService.clearCache(configKey);
        return Result.success();
    }

    @Operation(summary = "清除分组缓存")
    @DeleteMapping("/cache/group/{groupName}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> clearGroupCache(
            @Parameter(description = "配置分组") @PathVariable String groupName) {
        sysConfigService.clearCacheByGroup(groupName);
        return Result.success();
    }

    @Operation(summary = "清除全部配置缓存")
    @DeleteMapping("/cache/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> clearAllCache() {
        sysConfigService.clearAllCache();
        return Result.success();
    }

    @Operation(summary = "获取系统配置统计信息")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('sys:config:query')")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = sysConfigService.getSystemStats();
        return Result.success(stats);
    }
}
