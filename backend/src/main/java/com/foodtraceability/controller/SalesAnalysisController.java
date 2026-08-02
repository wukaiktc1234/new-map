package com.foodtraceability.controller;

import com.foodtraceability.service.SalesAnalysisService;
import com.foodtraceability.dto.SalesAnalysisVO;
import com.foodtraceability.dto.SalesAnalysisQueryDTO;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.common.Result;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 销售分析控制器
 * 提供销售数据分析的API接口
 */
@RestController
@RequestMapping("/v1/analytics/sales")
public class SalesAnalysisController {

    private final SalesAnalysisService salesAnalysisService;

    public SalesAnalysisController(SalesAnalysisService salesAnalysisService) {
        this.salesAnalysisService = salesAnalysisService;
    }

    /**
     * 生成日报
     * @param date 日期（yyyy-MM-dd）
     * @return 销售分析报表
     */
    @PostMapping("/report/daily")
    public Result<SalesAnalysisVO> generateDailyReport(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            SalesAnalysisVO vo = salesAnalysisService.generateDailySalesReport(date);
            return Result.success(vo, "日报生成成功");
        } catch (Exception e) {
            return Result.error("生成日报失败: " + e.getMessage());
        }
    }

    /**
     * 生成周报
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 销售分析报表
     */
    @PostMapping("/report/weekly")
    public Result<SalesAnalysisVO> generateWeeklyReport(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            SalesAnalysisVO vo = salesAnalysisService.generateWeeklySalesReport(startDate, endDate);
            return Result.success(vo, "周报生成成功");
        } catch (Exception e) {
            return Result.error("生成周报失败: " + e.getMessage());
        }
    }

    /**
     * 获取销售趋势
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param granularity 粒度（day/week/month）
     * @return 销售趋势数据
     */
    @GetMapping("/trend")
    public Result<Map<String, Object>> getSalesTrend(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "day") String granularity) {
        try {
            Map<String, Object> data = salesAnalysisService.getSalesTrend(startDate, endDate, granularity);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取销售趋势失败: " + e.getMessage());
        }
    }

    /**
     * 获取品类销售占比
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 品类占比数据
     */
    @GetMapping("/category-analysis")
    public Result<Map<String, Object>> getCategorySalesAnalysis(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            Map<String, Object> data = salesAnalysisService.getCategorySalesAnalysis(startDate, endDate);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取品类销售占比失败: " + e.getMessage());
        }
    }

    /**
     * 获取热销菜品TOP N
     * @param limit 数量限制
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 热销菜品列表
     */
    @GetMapping("/top-foods")
    public Result<List<Map<String, Object>>> getTopSellingFoods(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            List<Map<String, Object>> data = salesAnalysisService.getTopSellingFoods(limit, startDate, endDate);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取热销菜品失败: " + e.getMessage());
        }
    }

    /**
     * 获取时段分布
     * @param date 日期
     * @return 时段分布数据
     */
    @GetMapping("/hourly-distribution")
    public Result<Map<String, Object>> getHourlyDistribution(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            Map<String, Object> data = salesAnalysisService.getHourlyDistribution(date);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取时段分布失败: " + e.getMessage());
        }
    }

    /**
     * 获取渠道对比
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 渠道对比数据
     */
    @GetMapping("/channel-comparison")
    public Result<Map<String, Object>> getChannelComparison(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            Map<String, Object> data = salesAnalysisService.getChannelComparison(startDate, endDate);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取渠道对比失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询销售报表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @PostMapping("/reports/query")
    public Result<PageResult<SalesAnalysisVO>> queryReports(@RequestBody SalesAnalysisQueryDTO queryDTO) {
        try {
            PageResult<SalesAnalysisVO> result = salesAnalysisService.querySalesReports(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询销售报表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取报表详情
     * @param reportId 报表ID
     * @return 报表详情
     */
    @GetMapping("/reports/{reportId}")
    public Result<SalesAnalysisVO> getReportById(@PathVariable Long reportId) {
        try {
            SalesAnalysisVO vo = salesAnalysisService.getReportById(reportId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error("获取报表详情失败: " + e.getMessage());
        }
    }
}
