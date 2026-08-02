package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.annotation.AuditLog;
import com.foodtraceability.annotation.OperationType;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.AlertEntity;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 告警管理控制器
 * 提供告警的查询、确认、解决等API接口
 */
@Tag(name = "告警管理", description = "系统告警的查询、确认、解决、统计")
@RestController
@RequestMapping("/v1/alerts")
public class AlertController {

    private static final Logger logger = LoggerFactory.getLogger(AlertController.class);

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @Operation(summary = "分页查询告警列表")
    @GetMapping
    @PreAuthorize("hasAuthority('alert:query')")
    public Result<IPage<AlertEntity>> getAlertPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "告警级别") @RequestParam(required = false) String severity,
            @Parameter(description = "告警状态") @RequestParam(required = false) String status,
            @Parameter(description = "告警来源") @RequestParam(required = false) String source) {
        try {
            if (current == null || current < 1) current = 1;
            if (size == null || size < 1 || size > 100) size = 20;
            Page<AlertEntity> page = new Page<>(current, size);
            IPage<AlertEntity> result = alertService.getAlertPage(page, severity, status, source);
            return Result.success(result);
        } catch (Exception e) {
            logger.error("查询告警列表失败", e);
            return Result.error("查询告警列表失败");
        }
    }

    @Operation(summary = "获取所有告警")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('alert:query')")
    public Result<List<AlertEntity>> getAllAlerts() {
        try {
            return Result.success(alertService.getAllAlerts());
        } catch (Exception e) {
            logger.error("获取所有告警失败", e);
            return Result.error("获取所有告警失败");
        }
    }

    @Operation(summary = "根据ID获取告警")
    @GetMapping("/{alertId}")
    @PreAuthorize("hasAuthority('alert:query')")
    public Result<AlertEntity> getAlertById(
            @Parameter(description = "告警ID") @PathVariable Long alertId) {
        return alertService.getAlertById(alertId)
                .map(Result::success)
                .orElse(Result.error(404, "告警不存在"));
    }

    @Operation(summary = "获取活跃告警列表")
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('alert:query')")
    public Result<List<AlertEntity>> getActiveAlerts() {
        try {
            return Result.success(alertService.getActiveAlerts());
        } catch (Exception e) {
            logger.error("获取活跃告警失败", e);
            return Result.error("获取活跃告警失败");
        }
    }

    @Operation(summary = "确认告警")
    @PutMapping("/{alertId}/acknowledge")
    @PreAuthorize("hasAuthority('alert:edit')")
    @AuditLog(value = "确认告警", operationType = OperationType.UPDATE, module = "告警管理")
    public Result<AlertEntity> acknowledgeAlert(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "告警ID") @PathVariable Long alertId) {
        try {
            String acknowledgedBy = currentUser.getUsername();
            AlertEntity alert = alertService.acknowledgeAlert(alertId, acknowledgedBy);
            if (alert == null) {
                return Result.error(404, "告警不存在");
            }
            return Result.success(alert);
        } catch (Exception e) {
            logger.error("确认告警失败: alertId={}", alertId, e);
            return Result.error("确认告警失败");
        }
    }

    @Operation(summary = "解决告警")
    @PutMapping("/{alertId}/resolve")
    @PreAuthorize("hasAuthority('alert:edit')")
    @AuditLog(value = "解决告警", operationType = OperationType.UPDATE, module = "告警管理")
    public Result<AlertEntity> resolveAlert(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "告警ID") @PathVariable Long alertId,
            @Parameter(description = "解决描述") @RequestParam String resolveDescription) {
        try {
            String resolvedBy = currentUser.getUsername();
            AlertEntity alert = alertService.resolveAlert(alertId, resolvedBy, resolveDescription);
            if (alert == null) {
                return Result.error(404, "告警不存在");
            }
            return Result.success(alert);
        } catch (Exception e) {
            logger.error("解决告警失败: alertId={}", alertId, e);
            return Result.error("解决告警失败");
        }
    }

    @Operation(summary = "删除告警（逻辑删除）")
    @DeleteMapping("/{alertId}")
    @PreAuthorize("hasAuthority('alert:delete')")
    @AuditLog(value = "删除告警", operationType = OperationType.DELETE, module = "告警管理")
    public Result<Void> deleteAlert(
            @Parameter(description = "告警ID") @PathVariable Long alertId) {
        try {
            boolean success = alertService.deleteAlert(alertId);
            if (success) return Result.success();
            return Result.error(404, "告警不存在或删除失败");
        } catch (Exception e) {
            logger.error("删除告警失败: alertId={}", alertId, e);
            return Result.error("删除告警失败");
        }
    }

    @Operation(summary = "获取告警统计数据")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('alert:query')")
    public Result<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("activeCount", alertService.countByStatus("ACTIVE"));
            stats.put("acknowledgedCount", alertService.countByStatus("ACKNOWLEDGED"));
            stats.put("resolvedCount", alertService.countByStatus("RESOLVED"));
            stats.put("errorCount", alertService.countBySeverity("ERROR"));
            stats.put("warningCount", alertService.countBySeverity("WARNING"));
            stats.put("infoCount", alertService.countBySeverity("INFO"));
            return Result.success(stats);
        } catch (Exception e) {
            logger.error("获取告警统计失败", e);
            return Result.error("获取告警统计失败");
        }
    }
}
