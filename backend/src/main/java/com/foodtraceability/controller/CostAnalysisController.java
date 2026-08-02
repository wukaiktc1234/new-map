package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.product.CostAnalysisQueryDTO;
import com.foodtraceability.dto.product.CostAnalysisVO;
import com.foodtraceability.dto.product.CostTrendDataVO;
import com.foodtraceability.service.CostAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 成本分析控制器
 * 提供产品成本报表、盈利分析、预警功能
 * 权限要求：需要product:cost:view权限（只读）
 */
@RestController
@RequestMapping("/v1/product-center/cost-analysis")
@Tag(name = "成本分析", description = "产品成本报表、盈利分析、毛利统计接口")
@PreAuthorize("hasAuthority('product:cost:view')")
public class CostAnalysisController {

    private final CostAnalysisService costAnalysisService;

    public CostAnalysisController(CostAnalysisService costAnalysisService) {
        this.costAnalysisService = costAnalysisService;
    }

    /**
     * 分页查询成本分析数据
     */
    @GetMapping
    @Operation(summary = "成本分析列表", description = "分页查询产品的成本分析数据，含毛利率计算")
    public Result<Page<CostAnalysisVO>> queryAnalysis(CostAnalysisQueryDTO queryDto) {
        return Result.success(costAnalysisService.queryAnalysis(queryDto));
    }

    /**
     * 获取成本分析汇总统计
     */
    @GetMapping("/summary")
    @Operation(summary = "成本汇总", description = "获取产品成本的汇总统计数据")
    public Result<Map<String, Object>> getSummary(CostAnalysisQueryDTO queryDto) {
        return Result.success(costAnalysisService.getSummary(queryDto));
    }

    /**
     * 获取毛利率分布统计
     */
    @GetMapping("/profit-distribution")
    @Operation(summary = "毛利率分布", description = "获取产品毛利率的区间分布统计")
    public Result<Map<String, Integer>> getProfitRateDistribution(CostAnalysisQueryDTO queryDto) {
        return Result.success(costAnalysisService.getProfitRateDistribution(queryDto));
    }

    /**
     * 获取分类成本排名
     */
    @GetMapping("/category-ranking")
    @Operation(summary = "分类成本排名", description = "按销售额排名的分类成本统计")
    public Result<List<Map<String, Object>>> getCategoryCostRanking(
            @Parameter(description = "前N名") @RequestParam(defaultValue = "10") int topN) {
        return Result.success(costAnalysisService.getCategoryCostRanking(topN));
    }

    /**
     * 获取低毛利产品预警列表
     */
    @GetMapping("/low-profit-warning")
    @Operation(summary = "低毛利预警", description = "获取低于指定毛利率阈值的产品列表")
    public Result<List<CostAnalysisVO>> getLowProfitWarning(
            @Parameter(description = "毛利率阈值(%)") @RequestParam(defaultValue = "20") double threshold,
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "20") int limit) {
        return Result.success(costAnalysisService.getLowProfitWarning(threshold, limit));
    }

    /**
     * 获取成本趋势数据
     */
    @GetMapping("/trend")
    @Operation(summary = "成本趋势", description = "获取指定日期范围内的成本变化趋势，可按菜品筛选")
    public Result<List<CostTrendDataVO>> getTrend(
            @Parameter(description = "开始日期，格式 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期，格式 yyyy-MM-dd") @RequestParam(required = false) String endDate,
            @Parameter(description = "菜品ID（指定则查询单菜品趋势）") @RequestParam(required = false) Long dishId) {
        return Result.success(costAnalysisService.getTrend(startDate, endDate, dishId));
    }

    /**
     * 获取销售数据汇总（按商品聚合的 TOP N 销售额数据）
     * 数据来源：order_items 表，用于菜品成本分析页面"销售数据图表"
     */
    @GetMapping("/sales-summary")
    @Operation(summary = "销售数据汇总", description = "按商品聚合的 TOP N 销售额/销量数据，来源于订单明细表")
    public Result<List<Map<String, Object>>> getSalesSummary(
            @Parameter(description = "前N名，默认10，最大50") @RequestParam(defaultValue = "10") int topN) {
        return Result.success(costAnalysisService.getSalesSummary(topN));
    }
}
