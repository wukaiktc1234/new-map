package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.ProductAnalysisQueryDTO;
import com.foodtraceability.dto.ProductAnalysisVO;
import com.foodtraceability.service.ProductAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 产品分析控制器
 * 提供产品（菜品/套餐）数据分析的API接口
 * 权限要求：需要product:analysis:view权限
 */
@RestController
@RequestMapping("/v1/analytics/product")
@Tag(name = "产品分析", description = "产品数据分析、报表生成、套餐表现等接口")
@PreAuthorize("hasAuthority('product:analysis:view') or hasAnyRole('ADMIN', 'admin')")
public class ProductAnalysisController {

    private final ProductAnalysisService productAnalysisService;

    public ProductAnalysisController(ProductAnalysisService productAnalysisService) {
        this.productAnalysisService = productAnalysisService;
    }

    /**
     * 获取菜品盈利能力分析
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 盈利能力分析数据
     */
    @GetMapping("/profitability")
    @Operation(summary = "盈利能力分析", description = "获取指定时间范围内的菜品盈利能力分析数据")
    public Result<Map<String, Object>> getProductProfitabilityAnalysis(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            Map<String, Object> data = productAnalysisService.getProductProfitabilityAnalysis(startDate, endDate);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取盈利能力分析失败: " + e.getMessage());
        }
    }

    /**
     * 生成产品分析报表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 产品分析报表
     */
    @PostMapping("/report")
    @Operation(summary = "生成产品报表", description = "生成指定时间范围内的产品分析报表")
    @PreAuthorize("hasAuthority('product:analysis:export')")
    public Result<ProductAnalysisVO> generateProductReport(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            ProductAnalysisVO vo = productAnalysisService.generateProductReport(startDate, endDate);
            return Result.success(vo, "报表生成成功");
        } catch (Exception e) {
            return Result.error("生成产品报表失败: " + e.getMessage());
        }
    }

    /**
     * 获取菜单工程数据
     * @return 菜单工程数据
     */
    @GetMapping("/menu-engineering")
    @Operation(summary = "菜单工程", description = "获取菜单工程分析数据")
    public Result<Map<String, Object>> getMenuEngineeringData() {
        try {
            Map<String, Object> data = productAnalysisService.getMenuEngineeringData();
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取菜单工程数据失败: " + e.getMessage());
        }
    }

    /**
     * 获取套餐表现分析
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 套餐表现数据
     */
    @GetMapping("/combo-performance")
    @Operation(summary = "套餐表现", description = "获取套餐销售表现分析数据")
    public Result<List<Map<String, Object>>> getComboPerformance(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            List<Map<String, Object>> data = productAnalysisService.getComboPerformance(startDate, endDate);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取套餐表现失败: " + e.getMessage());
        }
    }

    /**
     * 获取价格弹性分析
     * @param foodId 菜品ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 价格弹性数据
     */
    @GetMapping("/price-elasticity/{foodId}")
    @Operation(summary = "价格弹性", description = "获取指定菜品的价格弹性分析数据")
    public Result<Map<String, Object>> getPriceElasticity(
            @PathVariable Long foodId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            Map<String, Object> data = productAnalysisService.getPriceElasticity(foodId, startDate, endDate);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取价格弹性分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取客户口味偏好
     * @return 客户口味偏好数据
     */
    @GetMapping("/customer-preference")
    @Operation(summary = "客户偏好", description = "获取客户口味偏好分析数据")
    public Result<Map<String, Object>> getCustomerPreferenceAnalysis() {
        try {
            Map<String, Object> data = productAnalysisService.getCustomerPreferenceAnalysis();
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取客户口味偏好失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询产品报表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @PostMapping("/reports/query")
    @Operation(summary = "查询报表", description = "分页查询产品分析报表")
    public Result<PageResult<ProductAnalysisVO>> queryReports(@RequestBody ProductAnalysisQueryDTO queryDTO) {
        try {
            PageResult<ProductAnalysisVO> result = productAnalysisService.queryProductReports(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询产品报表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取报表详情
     * @param reportId 报表ID
     * @return 报表详情
     */
    @GetMapping("/reports/{reportId}")
    @Operation(summary = "报表详情", description = "根据ID获取产品分析报表详情")
    public Result<ProductAnalysisVO> getReportById(@PathVariable Long reportId) {
        try {
            ProductAnalysisVO vo = productAnalysisService.getReportById(reportId);
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error("获取报表详情失败: " + e.getMessage());
        }
    }
}
