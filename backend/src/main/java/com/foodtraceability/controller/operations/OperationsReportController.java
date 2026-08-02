package com.foodtraceability.controller.operations;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.operations.KpiSummaryDTO;
import com.foodtraceability.dto.operations.OperationsReportQueryDTO;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.service.OperationsReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 经营报表控制器（管理层视角 - 精简版）
 * 提供统一报表查询、KPI汇总、导出等功能
 */
@RestController
@RequestMapping("/v1/operations-reports")
@Tag(name = "经营报表", description = "经营报表数据查询接口（管理层视角）")
public class OperationsReportController {

    private static final Logger logger = LoggerFactory.getLogger(OperationsReportController.class);

    private final OperationsReportService operationsReportService;

    public OperationsReportController(OperationsReportService operationsReportService) {
        this.operationsReportService = operationsReportService;
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
     * 统一报表数据查询
     * @param reportType 报表类型: daily/weekly/monthly/quarterly/yearly/profit
     * @param queryDTO 查询参数
     * @param authentication 认证信息
     * @return 报表数据
     */
    @GetMapping("/{reportType}")
    @PreAuthorize("hasAuthority('finance:view')")
    @Operation(summary = "获取报表数据", description = "统一报表数据查询接口，支持日报/周报/月报/季报/年报/利润分析")
    public Result<Map<String, Object>> getReportData(
            @Parameter(description = "报表类型: daily/weekly/monthly/quarterly/yearly/profit")
            @PathVariable String reportType,
            @Valid OperationsReportQueryDTO queryDTO,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        logger.info("获取报表数据，类型: {}, 用户: {}, 日期范围: {} ~ {}",
                reportType, userId, queryDTO.getStartDate(), queryDTO.getEndDate());
        Map<String, Object> data = operationsReportService.getReportData(reportType, queryDTO, userId);
        return Result.success(data);
    }

    /**
     * 获取KPI汇总数据（首屏快速加载）
     * @param queryDTO 查询参数
     * @param authentication 认证信息
     * @return KPI汇总数据
     */
    @GetMapping("/kpi-summary")
    @PreAuthorize("hasAuthority('finance:view')")
    @Operation(summary = "获取KPI汇总数据", description = "查询首屏KPI汇总数据，支持快速加载")
    public Result<KpiSummaryDTO> getKpiSummary(
            @Valid OperationsReportQueryDTO queryDTO,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        logger.info("获取KPI汇总数据，用户: {}, 日期范围: {} ~ {}", userId, queryDTO.getStartDate(), queryDTO.getEndDate());
        KpiSummaryDTO data = operationsReportService.getKpiSummary(queryDTO, userId);
        return Result.success(data);
    }

    /**
     * 创建导出任务
     * @param taskType 任务类型：1-Excel 2-PDF
     * @param reportType 报表类型：1日报 2周报 3月报 4季报 5年报 6利润分析
     * @param queryDTO 查询参数
     * @param authentication 认证信息
     * @return 导出任务信息
     */
    @PostMapping("/export")
    @PreAuthorize("hasAuthority('finance:edit')")
    @Operation(summary = "创建导出任务", description = "创建报表异步导出任务")
    public Result<Map<String, Object>> createExportTask(
            @Parameter(description = "任务类型：1-Excel 2-PDF") @RequestParam Integer taskType,
            @Parameter(description = "报表类型：1日报 2周报 3月报 4季报 5年报 6利润分析") @RequestParam Integer reportType,
            @Valid @RequestBody OperationsReportQueryDTO queryDTO,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        logger.info("创建导出任务，用户: {}, 任务类型: {}, 报表类型: {}", userId, taskType, reportType);
        Map<String, Object> data = operationsReportService.createExportTask(taskType, reportType, queryDTO, userId);
        return Result.success(data);
    }

    /**
     * 获取导出任务状态
     * @param taskId 任务ID
     * @param authentication 认证信息
     * @return 任务状态
     */
    @GetMapping("/export/{taskId}/status")
    @PreAuthorize("hasAuthority('finance:view')")
    @Operation(summary = "获取导出任务状态", description = "查询异步导出任务的处理状态")
    public Result<Map<String, Object>> getExportTaskStatus(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        logger.info("获取导出任务状态，任务ID: {}, 用户: {}", taskId, userId);
        Map<String, Object> data = operationsReportService.getExportTaskStatus(taskId, userId);
        return Result.success(data);
    }

    /**
     * 下载导出文件
     * @param taskId 任务ID
     * @param authentication 认证信息
     * @param response HTTP响应
     */
    @GetMapping("/export/{taskId}/download")
    @PreAuthorize("hasAuthority('finance:edit')")
    @Operation(summary = "下载导出文件", description = "下载已完成的报表导出文件")
    public void downloadExportFile(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            Authentication authentication,
            HttpServletResponse response) throws IOException {
        Long userId = getCurrentUserId(authentication);
        logger.info("下载导出文件，任务ID: {}, 用户: {}", taskId, userId);

        Map<String, Object> taskStatus = operationsReportService.getExportTaskStatus(taskId, userId);
        Integer status = (Integer) taskStatus.get("status");
        if (status == null || status != 2) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "任务未完成或不存在");
            return;
        }

        String fileName = (String) taskStatus.get("fileName");
        if (fileName == null) {
            fileName = "report_export.xlsx";
        }

        String filePath = (String) taskStatus.get("filePath");
        if (filePath == null || filePath.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件路径不存在");
            return;
        }

        java.nio.file.Path path = java.nio.file.Paths.get(filePath);
        if (!java.nio.file.Files.exists(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename="
                + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        response.setContentLengthLong(java.nio.file.Files.size(path));

        try (java.io.InputStream inputStream = java.nio.file.Files.newInputStream(path);
             java.io.OutputStream outputStream = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
    }
}
