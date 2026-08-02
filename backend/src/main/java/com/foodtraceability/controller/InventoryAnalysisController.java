package com.foodtraceability.controller;

import com.foodtraceability.service.InventoryAnalysisService;
import com.foodtraceability.dto.InventoryAnalysisVO;
import com.foodtraceability.dto.InventoryAnalysisQueryDTO;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.common.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 库存分析控制器
 * 提供库存数据分析的API接口
 */
@RestController
@RequestMapping("/v1/analytics/inventory")
public class InventoryAnalysisController {

    private final InventoryAnalysisService inventoryAnalysisService;

    public InventoryAnalysisController(InventoryAnalysisService inventoryAnalysisService) {
        this.inventoryAnalysisService = inventoryAnalysisService;
    }

    /**
     * 获取库存概览
     * @return 库存概览数据
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getInventoryOverview() {
        try {
            Map<String, Object> data = inventoryAnalysisService.getInventoryOverview();
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取库存概览失败: " + e.getMessage());
        }
    }

    /**
     * 生成库存分析报表
     * @param date 日期
     * @return 库存分析报表
     */
    @PostMapping("/report")
    public Result<InventoryAnalysisVO> generateInventoryReport(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            InventoryAnalysisVO vo = inventoryAnalysisService.generateInventoryReport(date);
            return Result.success(vo, "报表生成成功");
        } catch (Exception e) {
            return Result.error("生成库存报表失败: " + e.getMessage());
        }
    }

    /**
     * 获取周转率分析
     * @param startDate 开始日期（可选，默认近30天）
     * @param endDate 结束日期（可选，默认今天）
     * @return 周转率分析数据
     */
    @GetMapping("/turnover-analysis")
    public Result<Map<String, Object>> getInventoryTurnoverAnalysis(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            // 未指定日期时默认查询近30天
            LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
            LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);
            Map<String, Object> data = inventoryAnalysisService.getInventoryTurnoverAnalysis(effectiveStart, effectiveEnd);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取周转率分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取ABC分类
     * @return ABC分类数据
     */
    @GetMapping("/abc-classification")
    public Result<List<Map<String, Object>>> getABCClassification() {
        try {
            List<Map<String, Object>> data = inventoryAnalysisService.getABCClassification();
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取ABC分类失败: " + e.getMessage());
        }
    }

    /**
     * 获取报损分析
     * @param startDate 开始日期（可选，默认近30天）
     * @param endDate 结束日期（可选，默认今天）
     * @return 报损分析数据
     */
    @GetMapping("/waste-analysis")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<Map<String, Object>> getWasteAnalysis(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            // 未指定日期时默认查询近30天
            LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
            LocalDate effectiveStart = startDate != null ? startDate : effectiveEnd.minusDays(30);
            Map<String, Object> data = inventoryAnalysisService.getWasteAnalysis(effectiveStart, effectiveEnd);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取报损分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取缺货风险列表
     * @return 缺货风险物料列表
     */
    @GetMapping("/stockout-risk")
    public Result<List<Map<String, Object>>> getStockoutRiskList() {
        try {
            List<Map<String, Object>> data = inventoryAnalysisService.getStockoutRiskList();
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取缺货风险列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取各仓库库存价值分布
     * @return 各仓库库存价值
     */
    @GetMapping("/warehouse-value")
    public Result<List<Map<String, Object>>> getInventoryValueByWarehouse() {
        try {
            List<Map<String, Object>> data = inventoryAnalysisService.getInventoryValueByWarehouse();
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取仓库库存价值失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询库存报表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @PostMapping("/reports/query")
    public Result<PageResult<InventoryAnalysisVO>> queryReports(@RequestBody InventoryAnalysisQueryDTO queryDTO) {
        try {
            PageResult<InventoryAnalysisVO> result = inventoryAnalysisService.queryInventoryReports(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询库存报表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取报表详情
     * @param reportId 报表ID
     * @return 报表详情
     */
    @GetMapping("/reports/{reportId}")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<InventoryAnalysisVO> getReportById(@PathVariable Long reportId) {
        try {
            InventoryAnalysisVO vo = inventoryAnalysisService.getReportById(reportId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error("获取报表详情失败: " + e.getMessage());
        }
    }
}
