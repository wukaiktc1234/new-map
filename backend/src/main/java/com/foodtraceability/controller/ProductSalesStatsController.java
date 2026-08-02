package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.ProductSalesStats;
import com.foodtraceability.entity.SalesTrendData;
import com.foodtraceability.entity.TopProductData;
import com.foodtraceability.service.ProductSalesStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/sales-stats")
@Tag(name = "销售统计管理")
public class ProductSalesStatsController {
    

    public ProductSalesStatsController(ProductSalesStatsService salesStatsService) {
        this.salesStatsService = salesStatsService;
    }

    private final ProductSalesStatsService salesStatsService;
    
    @GetMapping("/product/page")
    @Operation(summary = "分页查询产品销售统计数据")
    @PreAuthorize("hasAuthority('product:view') or hasAuthority('*')")
    public Result<IPage<ProductSalesStats>> getSalesStatsPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "产品名称") @RequestParam(required = false) String productName,
            @Parameter(description = "产品分类") @RequestParam(required = false) String category,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            IPage<ProductSalesStats> statsPage = salesStatsService.getSalesStatsPage(page, size, productName, category, startDate, endDate);
            return Result.success(statsPage, "获取销售统计数据成功");
        } catch (Exception e) {
            return Result.error(500, "获取销售统计数据失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/trend")
    @Operation(summary = "获取销售趋势数据")
    @PreAuthorize("hasAuthority('product:view') or hasAuthority('*')")
    public Result<SalesTrendData> getSalesTrend(
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "趋势类型: day, week, month") @RequestParam(required = false, defaultValue = "day") String type) {
        try {
            SalesTrendData trendData = salesStatsService.getSalesTrend(startDate, endDate, type);
            return Result.success(trendData, "获取销售趋势成功");
        } catch (Exception e) {
            return Result.error(500, "获取销售趋势失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/top-products")
    @Operation(summary = "获取热销产品排行榜")
    @PreAuthorize("hasAuthority('product:view') or hasAuthority('*')")
    public Result<List<TopProductData>> getTopProducts(
            @Parameter(description = "TOP数量") @RequestParam(required = false, defaultValue = "5") int topN,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            List<TopProductData> topProducts = salesStatsService.getTopProducts(topN, startDate, endDate);
            return Result.success(topProducts, "获取热销产品成功");
        } catch (Exception e) {
            return Result.error(500, "获取热销产品失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/overview")
    @Operation(summary = "获取销售统计概览")
    @PreAuthorize("hasAuthority('product:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getSalesOverview(
            @Parameter(description = "统计日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            Map<String, Object> overview = salesStatsService.getSalesOverview(date);
            return Result.success(overview, "获取销售概览成功");
        } catch (Exception e) {
            return Result.error(500, "获取销售概览失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/category-stats")
    @Operation(summary = "按分类获取销售统计")
    @PreAuthorize("hasAuthority('product:view') or hasAuthority('*')")
    public Result<List<Map<String, Object>>> getSalesStatsByCategory(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            List<Map<String, Object>> categoryStats = salesStatsService.getSalesStatsByCategory(startDate, endDate);
            return Result.success(categoryStats, "获取分类统计成功");
        } catch (Exception e) {
            return Result.error(500, "获取分类统计失败：" + e.getMessage());
        }
    }
    
    @GetMapping("/growth-rate")
    @Operation(summary = "获取销售增长率数据")
    @PreAuthorize("hasAuthority('product:view') or hasAuthority('*')")
    public Result<List<Map<String, Object>>> getSalesGrowthRate(
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            List<Map<String, Object>> growthData = salesStatsService.getSalesGrowthRate(startDate, endDate);
            return Result.success(growthData, "获取增长率数据成功");
        } catch (Exception e) {
            return Result.error(500, "获取增长率数据失败：" + e.getMessage());
        }
    }
}
