package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.MaterialTraceCodeGenerateDTO;
import com.foodtraceability.entity.MaterialTraceCode;
import com.foodtraceability.service.MaterialTraceCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 原料追溯码控制器
 */
@RestController
@RequestMapping("/v1/material-trace-code")
@Tag(name = "原料追溯码管理", description = "原料追溯码的生成、查询、状态管理等接口")
public class MaterialTraceCodeController {


    public MaterialTraceCodeController(MaterialTraceCodeService materialTraceCodeService) {
        this.materialTraceCodeService = materialTraceCodeService;
    }

    private final MaterialTraceCodeService materialTraceCodeService;

    @PostMapping("/generate")
    @Operation(summary = "批量生成原料追溯码")
    @PreAuthorize("hasAuthority('trace:create')")
    public Result<List<MaterialTraceCode>> generate(@RequestBody MaterialTraceCodeGenerateDTO dto) {
        try {
            List<MaterialTraceCode> codes = materialTraceCodeService.generateBatch(dto);
            return Result.success(codes);
        } catch (Exception e) {
            return Result.error("生成原料追溯码失败: " + e.getMessage());
        }
    }

    @GetMapping("/query/{traceCode}")
    @Operation(summary = "根据追溯码查询")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<MaterialTraceCode> queryByTraceCode(
            @Parameter(description = "追溯码") @PathVariable String traceCode) {
        MaterialTraceCode code = materialTraceCodeService.getByTraceCode(traceCode);
        if (code == null) {
            return Result.error("追溯码不存在");
        }
        return Result.success(code);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询原料追溯码列表")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<IPage<MaterialTraceCode>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "物料ID") @RequestParam(required = false) String materialId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "存储位置") @RequestParam(required = false) String storageLocation,
            @Parameter(description = "批次号") @RequestParam(required = false) String batchNumber) {

        Page<MaterialTraceCode> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MaterialTraceCode> wrapper = new LambdaQueryWrapper<>();

        if (materialId != null && !materialId.isEmpty()) {
            wrapper.eq(MaterialTraceCode::getMaterialId, materialId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(MaterialTraceCode::getStatus, status);
        }
        if (storageLocation != null && !storageLocation.isEmpty()) {
            wrapper.eq(MaterialTraceCode::getStorageLocation, storageLocation);
        }
        if (batchNumber != null && !batchNumber.isEmpty()) {
            wrapper.eq(MaterialTraceCode::getBatchNumber, batchNumber);
        }

        wrapper.orderByDesc(MaterialTraceCode::getCreateTime);

        IPage<MaterialTraceCode> result = materialTraceCodeService.page(pageParam, wrapper);
        return Result.success(result);
    }

    @GetMapping("/stockin/{stockinId}")
    @Operation(summary = "根据采购入库单ID查询")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<List<MaterialTraceCode>> listByStockinId(
            @Parameter(description = "采购入库单ID") @PathVariable Long stockinId) {
        List<MaterialTraceCode> codes = materialTraceCodeService.getByStockinId(stockinId);
        return Result.success(codes);
    }

    @PutMapping("/{traceCodeId}/status")
    @Operation(summary = "更新追溯码状态")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Boolean> updateStatus(
            @Parameter(description = "追溯码ID") @PathVariable String traceCodeId,
            @Parameter(description = "新状态") @RequestParam String status,
            @Parameter(description = "操作人") @RequestParam(required = false) String operatorName,
            @Parameter(description = "原因") @RequestParam(required = false) String reason) {
        try {
            boolean result = materialTraceCodeService.updateStatus(traceCodeId, status, operatorName, reason);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("更新状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/pick")
    @Operation(summary = "领用追溯码")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Boolean> pickTraceCode(
            @Parameter(description = "追溯码") @RequestParam String traceCode,
            @Parameter(description = "使用人ID") @RequestParam Long usedById,
            @Parameter(description = "使用人姓名") @RequestParam String usedByName,
            @Parameter(description = "使用目的") @RequestParam(required = false) String usagePurpose) {
        try {
            boolean result = materialTraceCodeService.pickTraceCode(traceCode, usedById, usedByName, usagePurpose);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("领用失败: " + e.getMessage());
        }
    }

    @PostMapping("/return")
    @Operation(summary = "退回追溯码")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Boolean> returnTraceCode(
            @Parameter(description = "追溯码") @RequestParam String traceCode,
            @Parameter(description = "退回原因") @RequestParam String reason) {
        try {
            boolean result = materialTraceCodeService.returnTraceCode(traceCode, reason);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("退回失败: " + e.getMessage());
        }
    }

    @GetMapping("/expired")
    @Operation(summary = "获取过期追溯码")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<List<MaterialTraceCode>> getExpiredCodes() {
        List<MaterialTraceCode> codes = materialTraceCodeService.getExpiredCodes();
        return Result.success(codes);
    }

    @GetMapping("/expiring-soon")
    @Operation(summary = "获取即将过期追溯码")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<List<MaterialTraceCode>> getExpiringSoonCodes(
            @Parameter(description = "天数") @RequestParam(defaultValue = "7") Integer days) {
        List<MaterialTraceCode> codes = materialTraceCodeService.getExpiringSoonCodes(days);
        return Result.success(codes);
    }

    @PostMapping("/mark-expired")
    @Operation(summary = "标记过期追溯码")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Integer> markExpiredCodes() {
        try {
            int count = materialTraceCodeService.markExpiredCodes();
            return Result.success(count);
        } catch (Exception e) {
            return Result.error("标记过期失败: " + e.getMessage());
        }
    }

    @GetMapping("/qr/{traceCodeId}")
    @Operation(summary = "生成二维码URL")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<String> generateQrCode(
            @Parameter(description = "追溯码ID") @PathVariable String traceCodeId) {
        String qrCodeUrl = materialTraceCodeService.generateQrCode(traceCodeId);
        return Result.success(qrCodeUrl);
    }

    @PostMapping("/print")
    @Operation(summary = "批量打印追溯码")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Boolean> batchPrint(
            @RequestBody List<String> traceCodeIds,
            @Parameter(description = "打印机ID") @RequestParam(required = false) Long printerId) {
        try {
            boolean result = materialTraceCodeService.batchPrint(traceCodeIds, printerId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("打印失败: " + e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "统计各状态数量")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<Map<String, Integer>> statistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("pending", materialTraceCodeService.countByStatus("pending"));
        stats.put("in_stock", materialTraceCodeService.countByStatus("in_stock"));
        stats.put("picked", materialTraceCodeService.countByStatus("picked"));
        stats.put("used", materialTraceCodeService.countByStatus("used"));
        stats.put("expired", materialTraceCodeService.countByStatus("expired"));
        stats.put("returned", materialTraceCodeService.countByStatus("returned"));
        return Result.success(stats);
    }

    @PostMapping("/confirm-stockin")
    @Operation(summary = "扫码确认入库", description = "将待入库状态的追溯码确认为已入库，同时更新库存和创建采购入库记录")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Boolean> confirmStockin(
            @Parameter(description = "追溯码") @RequestParam String traceCode,
            @Parameter(description = "操作人") @RequestParam(required = false) String operatorName) {
        try {
            boolean result = materialTraceCodeService.confirmStockin(traceCode, operatorName);
            if (result) {
                return Result.success(true);
            } else {
                return Result.error("确认入库失败：追溯码不存在或状态不正确");
            }
        } catch (Exception e) {
            return Result.error("确认入库失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch-confirm-stockin")
    @Operation(summary = "批量扫码确认入库")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Integer> batchConfirmStockin(
            @RequestBody List<String> traceCodes,
            @Parameter(description = "操作人") @RequestParam(required = false) String operatorName) {
        try {
            int successCount = materialTraceCodeService.batchConfirmStockin(traceCodes, operatorName);
            return Result.success(successCount);
        } catch (Exception e) {
            return Result.error("批量确认入库失败: " + e.getMessage());
        }
    }
}
