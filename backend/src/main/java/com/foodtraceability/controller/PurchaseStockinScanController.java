package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.MaterialTraceCode;
import com.foodtraceability.service.PurchaseStockinScanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购入库扫描控制器
 * 支持扫码确认入库、剥离模式打印、人工介入处理
 */
@RestController
@RequestMapping("/v1/purchase/stockin/scan")
@Tag(name = "采购入库扫描管理", description = "扫码确认入库、标签打印、人工介入处理")
public class PurchaseStockinScanController {
    

    public PurchaseStockinScanController(PurchaseStockinScanService scanService) {
        this.scanService = scanService;
    }

    private final PurchaseStockinScanService scanService;

    @PostMapping("/confirm")
    @Operation(summary = "扫描追溯码确认入库")
    @PreAuthorize("hasAuthority('purchase:stockin:scan') or hasAuthority('*')")
    public Result<ScanConfirmResult> scanConfirm(@RequestBody PurchaseStockinScanDTO scanDTO) {
        ScanConfirmResult result = scanService.scanConfirmStockin(scanDTO);
        return Result.success(result);
    }
    
    @PostMapping("/batch-confirm")
    @Operation(summary = "批量扫描确认入库")
    @PreAuthorize("hasAuthority('purchase:stockin:scan')")
    public Result<List<ScanConfirmResult>> batchScanConfirm(@RequestBody List<PurchaseStockinScanDTO> scanDTOs) {
        List<ScanConfirmResult> results = scanService.batchScanConfirm(scanDTOs);
        return Result.success(results);
    }
    
    @GetMapping("/progress/{stockinId}")
    @Operation(summary = "获取入库进度")
    @PreAuthorize("hasAuthority('purchase:stockin:view')")
    public Result<StockinProgressDTO> getProgress(@PathVariable Long stockinId) {
        StockinProgressDTO progress = scanService.getStockinProgress(stockinId);
        if (progress == null) {
            return Result.error("入库单不存在");
        }
        return Result.success(progress);
    }
    
    @PostMapping("/intervention/handle")
    @Operation(summary = "处理人工介入")
    @PreAuthorize("hasAuthority('purchase:stockin:intervene') or hasAuthority('*')")
    public Result<ScanConfirmResult> handleIntervention(@RequestBody ManualInterventionDTO intervention) {
        ScanConfirmResult result = scanService.handleManualIntervention(intervention);
        return Result.success(result);
    }
    
    @GetMapping("/intervention/pending/{stockinId}")
    @Operation(summary = "获取待处理的介入任务")
    @PreAuthorize("hasAuthority('purchase:stockin:view')")
    public Result<List<ManualInterventionDTO>> getPendingInterventions(@PathVariable Long stockinId) {
        List<ManualInterventionDTO> interventions = scanService.getPendingInterventions(stockinId);
        return Result.success(interventions);
    }
    
    @PostMapping("/cancel/{stockinId}")
    @Operation(summary = "取消入库单")
    @PreAuthorize("hasAuthority('purchase:stockin:cancel') or hasAuthority('*')")
    public Result<Boolean> cancelStockin(
            @PathVariable Long stockinId,
            @RequestParam String reason) {
        boolean result = scanService.cancelStockin(stockinId, reason);
        return Result.success(result);
    }
    
    @PostMapping("/complete/{stockinId}")
    @Operation(summary = "完成入库单")
    @PreAuthorize("hasAuthority('purchase:stockin:complete')")
    public Result<Boolean> completeStockin(@PathVariable Long stockinId) {
        boolean result = scanService.completeStockin(stockinId);
        return Result.success(result);
    }
    
    @PostMapping("/generate-labels/{stockinId}")
    @Operation(summary = "生成追溯码并打印标签")
    @PreAuthorize("hasAuthority('purchase:stockin:print')")
    public Result<List<MaterialTraceCode>> generateAndPrintLabels(
            @PathVariable Long stockinId,
            @RequestBody(required = false) LabelPrintConfigDTO printConfig) {
        if (printConfig == null) {
            printConfig = LabelPrintConfigDTO.peelModeConfig();
        }
        List<MaterialTraceCode> codes = scanService.generateAndPrintLabels(stockinId, printConfig);
        return Result.success(codes);
    }
    
    @PostMapping("/print-peel-mode/{stockinId}")
    @Operation(summary = "剥离模式打印标签（即打即贴）")
    @PreAuthorize("hasAuthority('purchase:stockin:print')")
    public Result<List<MaterialTraceCode>> printWithPeelMode(
            @PathVariable Long stockinId,
            @RequestBody(required = false) LabelPrintConfigDTO printConfig) {
        if (printConfig == null) {
            printConfig = LabelPrintConfigDTO.peelModeConfig();
        } else {
            printConfig.setPeelModeEnabled(true);
        }
        List<MaterialTraceCode> codes = scanService.generateAndPrintLabels(stockinId, printConfig);
        return Result.success(codes);
    }
}
