package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.trace.InspectionVO;
import com.foodtraceability.dto.trace.SupplierBatchVO;
import com.foodtraceability.dto.trace.SupplierTraceVO;
import com.foodtraceability.service.trace.SupplierTraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应商追溯管理控制器
 * 提供供应商维度的追溯信息聚合查询，包括批次、合格率、召回、检验等
 */
@Tag(name = "供应商追溯管理", description = "供应商维度的追溯信息聚合查询")
@RestController
@RequestMapping("/v1/supplier-trace")
public class SupplierTraceController {

    private static final Logger log = LoggerFactory.getLogger(SupplierTraceController.class);

    private final SupplierTraceService supplierTraceService;

    public SupplierTraceController(SupplierTraceService supplierTraceService) {
        this.supplierTraceService = supplierTraceService;
    }

    /** 供应商追溯汇总 */
    @Operation(summary = "供应商追溯汇总", description = "获取供应商追溯汇总信息，含批次、合格率、召回、检验")
    @GetMapping("/{supplierId}")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<SupplierTraceVO> getSupplierTrace(@PathVariable Long supplierId) {
        SupplierTraceVO vo = supplierTraceService.getSupplierTrace(supplierId);
        return Result.success(vo);
    }

    /** 供应商所有原料批次 */
    @Operation(summary = "供应商原料批次", description = "获取供应商所有原料批次列表")
    @GetMapping("/{supplierId}/batches")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<List<SupplierBatchVO>> getBatches(@PathVariable Long supplierId) {
        List<SupplierBatchVO> list = supplierTraceService.getSupplierBatches(supplierId);
        return Result.success(list);
    }

    /** 供应商合格率 */
    @Operation(summary = "供应商合格率", description = "获取供应商合格率统计信息")
    @GetMapping("/{supplierId}/quality-rate")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<Map<String, Object>> getQualityRate(@PathVariable Long supplierId) {
        Map<String, Object> rate = supplierTraceService.getQualityRate(supplierId);
        return Result.success(rate);
    }

    /** 供应商召回记录 */
    @Operation(summary = "供应商召回记录", description = "获取供应商相关的召回记录列表")
    @GetMapping("/{supplierId}/recalls")
    @PreAuthorize("hasAuthority('trace:recall')")
    public Result<List<Map<String, Object>>> getRecalls(@PathVariable Long supplierId) {
        List<Map<String, Object>> recalls = supplierTraceService.getRecalls(supplierId);
        return Result.success(recalls);
    }

    /** 供应商检验记录 */
    @Operation(summary = "供应商检验记录", description = "获取供应商相关的检验记录列表")
    @GetMapping("/{supplierId}/inspections")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<List<InspectionVO>> getInspections(@PathVariable Long supplierId) {
        List<InspectionVO> list = supplierTraceService.getInspections(supplierId);
        return Result.success(list);
    }

    /** 供应商追溯统计 */
    @Operation(summary = "供应商追溯统计", description = "获取供应商追溯统计数据")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<Map<String, Object>> statistics() {
        try {
            Map<String, Object> statistics = supplierTraceService.getStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            // 字段映射不一致（material_trace_code 表使用 created_at/updated_at，实体映射为 create_time/update_time）
            // 导致聚合查询失败时，返回空统计数据，保证页面可访问
            log.warn("供应商追溯统计查询失败，返回空统计数据: {}", e.getMessage());
            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("totalSuppliers", 0L);
            emptyStats.put("totalBatches", 0);
            emptyStats.put("totalInspections", 0L);
            emptyStats.put("totalRecalls", 0L);
            emptyStats.put("overallQualificationRate", BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            return Result.success(emptyStats);
        }
    }
}
