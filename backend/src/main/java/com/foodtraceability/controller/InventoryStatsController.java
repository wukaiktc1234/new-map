package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.InventoryStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 库存统计控制器
 * 处理库存统计相关的HTTP请求
 */
@RestController
@RequestMapping("/v1/inventory/stats")
@Tag(name = "库存统计管理")
public class InventoryStatsController {


    public InventoryStatsController(InventoryStatsService inventoryStatsService) {
        this.inventoryStatsService = inventoryStatsService;
    }

    private final InventoryStatsService inventoryStatsService;

    /**
     * 获取库存统计概览
     */
    @GetMapping("/overview")
    @Operation(summary = "获取库存统计概览")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<Map<String, Object>> getStatsOverview() {
        try {
            Map<String, Object> overview = inventoryStatsService.getStatsOverview();
            return Result.success(overview, "获取库存统计概览成功");
        } catch (Exception e) {
            return Result.error(500, "获取库存统计概览失败：" + e.getMessage());
        }
    }

    /**
     * 获取库存趋势统计
     */
    @GetMapping("/trend")
    @Operation(summary = "获取库存趋势统计")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<Map<String, Object>>> getStatsTrend(
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "统计类型: day, week, month") @RequestParam(required = false, defaultValue = "day") String type) {
        try {
            List<Map<String, Object>> trendData = inventoryStatsService.getStatsTrend(startTime, endTime, type);
            return Result.success(trendData, "获取库存趋势统计成功");
        } catch (Exception e) {
            return Result.error(500, "获取库存趋势统计失败：" + e.getMessage());
        }
    }

    /**
     * 获取库存分类统计
     */
    @GetMapping("/category")
    @Operation(summary = "获取库存分类统计")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<Map<String, Object>>> getStatsCategory(
            @Parameter(description = "仓库ID") @RequestParam(required = false) Long warehouseId) {
        try {
            List<Map<String, Object>> categoryData = inventoryStatsService.getStatsCategory(warehouseId);
            return Result.success(categoryData, "获取库存分类统计成功");
        } catch (Exception e) {
            return Result.error(500, "获取库存分类统计失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取消耗趋势统计
     */
    @GetMapping("/consumption-trend")
    @Operation(summary = "获取消耗趋势统计")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<List<Map<String, Object>>> getConsumptionTrend(
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "统计类型: day, week, month") @RequestParam(required = false, defaultValue = "day") String type) {
        try {
            List<Map<String, Object>> trendData = inventoryStatsService.getConsumptionTrend(startTime, endTime, type);
            return Result.success(trendData, "获取消耗趋势统计成功");
        } catch (Exception e) {
            return Result.error(500, "获取消耗趋势统计失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取库存同比/环比数据
     */
    @GetMapping("/comparison")
    @Operation(summary = "获取库存同比/环比数据")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<Map<String, Object>> getStatsComparison() {
        try {
            Map<String, Object> comparisonData = inventoryStatsService.getStatsComparison();
            return Result.success(comparisonData, "获取库存同比/环比数据成功");
        } catch (Exception e) {
            return Result.error(500, "获取库存同比/环比数据失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取预警历史趋势
     */
    @GetMapping("/warning-trend")
    @Operation(summary = "获取预警历史趋势")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<List<Map<String, Object>>> getWarningHistoryTrend(
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "统计类型: day, week, month") @RequestParam(required = false, defaultValue = "day") String type) {
        try {
            List<Map<String, Object>> trendData = inventoryStatsService.getWarningHistoryTrend(startTime, endTime, type);
            return Result.success(trendData, "获取预警历史趋势成功");
        } catch (Exception e) {
            return Result.error(500, "获取预警历史趋势失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取预警级别分布
     */
    @GetMapping("/warning-level-distribution")
    @Operation(summary = "获取预警级别分布")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<Map<String, Object>> getWarningLevelDistribution() {
        try {
            Map<String, Object> distributionData = inventoryStatsService.getWarningLevelDistribution();
            return Result.success(distributionData, "获取预警级别分布成功");
        } catch (Exception e) {
            return Result.error(500, "获取预警级别分布失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取预警状态分布
     */
    @GetMapping("/warning-status-distribution")
    @Operation(summary = "获取预警状态分布")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('inventory:query')")
    public Result<Map<String, Object>> getWarningStatusDistribution() {
        try {
            Map<String, Object> distributionData = inventoryStatsService.getWarningStatusDistribution();
            return Result.success(distributionData, "获取预警状态分布成功");
        } catch (Exception e) {
            return Result.error(500, "获取预警状态分布失败：" + e.getMessage());
        }
    }
}