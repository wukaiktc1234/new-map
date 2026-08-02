package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.SupplierEvaluationCreateDTO;
import com.foodtraceability.entity.SupplierEvaluation;
import com.foodtraceability.service.SupplierEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 供应商评估控制器
 * 提供供应商评估的管理接口，支持手动创建和自动计算
 */
@Tag(name = "供应商评估管理", description = "供应商评估相关接口")
@RestController
@RequestMapping("/v1/supplier-evaluations")
public class SupplierEvaluationController {

    private static final Logger log = LoggerFactory.getLogger(SupplierEvaluationController.class);

    private final SupplierEvaluationService supplierEvaluationService;

    public SupplierEvaluationController(SupplierEvaluationService supplierEvaluationService) {
        this.supplierEvaluationService = supplierEvaluationService;
    }

    @Operation(summary = "手动创建供应商评估")
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<SupplierEvaluation> createEvaluation(@Valid @RequestBody SupplierEvaluationCreateDTO createDTO) {
        try {
            SupplierEvaluation evaluation = supplierEvaluationService.createEvaluation(createDTO);
            return Result.success(evaluation);
        } catch (Exception e) {
            log.error("创建供应商评估失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "创建评估失败");
        }
    }

    @Operation(summary = "自动计算评估结果")
    @PostMapping("/auto-calc")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<SupplierEvaluation> autoCalculate(
            @io.swagger.v3.oas.annotations.Parameter(description = "供应商ID") @RequestParam Long supplierId,
            @io.swagger.v3.oas.annotations.Parameter(description = "评估周期(yyyy-MM)") @RequestParam String period) {
        try {
            SupplierEvaluation evaluation = supplierEvaluationService.autoCalculateEvaluation(supplierId, period);
            return Result.success(evaluation);
        } catch (Exception e) {
            log.error("自动计算评估失败，供应商ID：{}，周期：{}", supplierId, period, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "自动计算评估失败");
        }
    }

    @Operation(summary = "获取供应商评估历史列表")
    @GetMapping("/supplier/{supplierId}")
    @PreAuthorize("isAuthenticated()")
    public Result<List<SupplierEvaluation>> getEvaluationsBySupplier(@PathVariable Long supplierId) {
        try {
            List<SupplierEvaluation> evaluations = supplierEvaluationService.getEvaluationsBySupplier(supplierId);
            return Result.success(evaluations);
        } catch (Exception e) {
            log.error("获取供应商评估历史失败，供应商ID：{}", supplierId, e);
            return Result.error("获取评估历史失败");
        }
    }

    @Operation(summary = "获取供应商最新一次评估结果")
    @GetMapping("/supplier/{supplierId}/latest")
    @PreAuthorize("isAuthenticated()")
    public Result<SupplierEvaluation> getLatestEvaluation(@PathVariable Long supplierId) {
        try {
            SupplierEvaluation evaluation = supplierEvaluationService.getLatestEvaluation(supplierId);
            if (evaluation == null) {
                return Result.error("该供应商暂无评估记录");
            }
            return Result.success(evaluation);
        } catch (Exception e) {
            log.error("获取供应商最新评估失败，供应商ID：{}", supplierId, e);
            return Result.error("获取最新评估失败");
        }
    }
}
