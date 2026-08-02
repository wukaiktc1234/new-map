package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.AuditLog;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "审计日志管理", description = "审计日志查询、导出、统计接口")
@RestController
@RequestMapping("/v1/audit-logs")
public class AuditLogController {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogController.class);
    private static final int MAX_STRING_LENGTH = 200;


    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    private final AuditLogService auditLogService;

    @Operation(summary = "分页查询审计日志")
    @GetMapping
    @PreAuthorize("hasAuthority('audit:log:query')")
    public Result<IPage<AuditLog>> getAuditLogs(
            @AuthenticationPrincipal SecurityUser currentUser,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "操作类型: LOGIN/CREATE/UPDATE/DELETE/EXPORT/VIEW/AUTH/SYSTEM/SECURITY") 
                @RequestParam(required = false) String operationType,
            @Parameter(description = "模块") @RequestParam(required = false) String module,
            @Parameter(description = "风险等级: LOW/MEDIUM/HIGH/CRITICAL") 
                @RequestParam(required = false) String riskLevel,
            @Parameter(description = "IP地址") @RequestParam(required = false) String ip,
            @Parameter(description = "是否包含敏感数据") @RequestParam(required = false) Integer sensitiveFlag,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        try {
            if (current == null || current < 1) current = 1;
            if (size == null || size < 1 || size > 100) size = 20;
            
            Page<AuditLog> page = new Page<>(current, size);
            
            IPage<AuditLog> result = auditLogService.getAuditLogPage(
                page, truncate(userId), truncate(username), truncate(operationType), 
                truncate(module), truncate(riskLevel), truncate(ip), 
                sensitiveFlag, startTime, endTime
            );
            
            return Result.success(result);
        } catch (Exception e) {
            logger.error("查询审计日志失败", e);
            return Result.error("查询审计日志失败");
        }
    }

    @Operation(summary = "获取审计日志统计信息")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('audit:log:query')")
    public Result<Map<String, Object>> getStatistics(
            @Parameter(description = "开始时间(默认7天前)") 
                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束时间(默认当前)") 
                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        
        Map<String, Object> stats = auditLogService.getStatistics(startDate, endDate);
        return Result.success(stats);
    }

    @Operation(summary = "导出审计日志(CSV)")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('audit:log:export')")
    public void exportAuditLogs(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpServletResponse response) throws Exception {
        
        Map<String, Object> params = new HashMap<>();
        params.put("operationType", truncate(operationType));
        params.put("riskLevel", truncate(riskLevel));
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        
        auditLogService.exportToCsv(params, response);
    }

    @Operation(summary = "归档历史日志（管理员）")
    @PostMapping("/archive")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('audit:log:archive')")
    public Result<Map<String, Integer>> archiveLogs(
            @Parameter(description = "归档保留天数前的数据(默认90天)") @RequestParam(defaultValue = "90") int retentionDays) {
        
        if (retentionDays < 30) {
            return Result.error("归档保留天数不能少于30天");
        }
        
        int archived = auditLogService.archiveLogs(retentionDays);
        logger.info("管理员归档审计日志: 保留{}天, 归档{}条(原表数据保留不变)", retentionDays, archived);
        
        return Result.success(Map.of(
            "archivedCount", archived,
            "retentionDays", retentionDays
        ));
    }

    @Operation(summary = "说明：审计日志不可删除（法规要求）")
    @DeleteMapping("/clean")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> cleanExpiredLogsDeprecated() {
        // 审计日志仅允许 INSERT，禁止 UPDATE 和 DELETE（会计法/网络安全法/数据安全法要求）。
        // 此接口已废弃，保留端点以兼容旧前端调用，但不再执行任何删除操作。
        return Result.error("审计日志不可删除（会计法/网络安全法要求），如需归档请使用 POST /v1/audit-logs/archive");
    }

    private String truncate(String value) {
        if (value == null) return null;
        if (value.length() > MAX_STRING_LENGTH) {
            return value.substring(0, MAX_STRING_LENGTH);
        }
        return value;
    }
}
