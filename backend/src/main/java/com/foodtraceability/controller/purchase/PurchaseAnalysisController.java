package com.foodtraceability.controller.purchase;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.purchase.PurchaseReportCategoryItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportMonthlyItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportSupplierItemVO;
import com.foodtraceability.dto.purchase.PurchaseReportSummaryVO;
import com.foodtraceability.service.purchase.PurchaseAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 采购数据分析控制器
 *
 * <p>提供 4 个 RESTful 端点：
 * <ul>
 *   <li>GET /v1/purchase/analysis/trend      采购趋势分析（按月聚合）</li>
 *   <li>GET /v1/purchase/analysis/supplier   供应商采购占比分析</li>
 *   <li>GET /v1/purchase/analysis/category    品类采购分布分析</li>
 *   <li>GET /v1/purchase/analysis/summary      综合汇总指标</li>
 * </ul>
 * </p>
 *
 * <p>与 {@link PurchaseReportController} 的区别：
 * <ul>
 *   <li>聚焦"分析视角"，仅按日期范围聚合，不支持 supplierId/category 过滤</li>
 *   <li>复用 PurchaseReport 系列 VO，避免重复定义</li>
 *   <li>所有金额字段以"分"为单位返回（Long），由前端 DataConverter 转换为"元"</li>
 * </ul>
 * </p>
 */
@Tag(name = "采购数据分析", description = "采购趋势、供应商占比、品类分布及综合汇总接口")
@RestController
@RequestMapping("/v1/purchase/analysis")
public class PurchaseAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseAnalysisController.class);

    private final PurchaseAnalysisService analysisService;

    public PurchaseAnalysisController(PurchaseAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * 采购趋势分析（按月份聚合）
     * @param startDate 开始日期（yyyy-MM-dd，可选）
     * @param endDate 结束日期（yyyy-MM-dd，可选）
     * @return 月份/采购金额/订单数量趋势数据列表
     */
    @Operation(summary = "采购趋势分析", description = "按月份返回采购金额与订单数量趋势数据（按月份升序）")
    @GetMapping("/trend")
    @PreAuthorize("isAuthenticated()")
    public Result<List<PurchaseReportMonthlyItemVO>> getTrend(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<PurchaseReportMonthlyItemVO> data = analysisService.getTrend(startDate, endDate);
            return Result.success(data, "查询成功");
        } catch (Exception e) {
            log.error("获取采购趋势分析数据失败: startDate={}, endDate={}", startDate, endDate, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询失败");
        }
    }

    /**
     * 供应商采购占比分析
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 供应商/采购金额占比数据列表
     */
    @Operation(summary = "供应商采购占比分析", description = "按供应商维度返回采购金额、订单数、准时交付率、合格率等数据（按金额降序）")
    @GetMapping("/supplier")
    @PreAuthorize("isAuthenticated()")
    public Result<List<PurchaseReportSupplierItemVO>> getSupplier(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<PurchaseReportSupplierItemVO> data = analysisService.getSupplier(startDate, endDate);
            return Result.success(data, "查询成功");
        } catch (Exception e) {
            log.error("获取供应商采购占比数据失败: startDate={}, endDate={}", startDate, endDate, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询失败");
        }
    }

    /**
     * 品类采购分布分析
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 品类/采购金额分布数据列表
     */
    @Operation(summary = "品类采购分布分析", description = "按品类维度返回采购金额、数量、订单数等数据（按金额降序）")
    @GetMapping("/category")
    @PreAuthorize("isAuthenticated()")
    public Result<List<PurchaseReportCategoryItemVO>> getCategory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<PurchaseReportCategoryItemVO> data = analysisService.getCategory(startDate, endDate);
            return Result.success(data, "查询成功");
        } catch (Exception e) {
            log.error("获取品类采购分布数据失败: startDate={}, endDate={}", startDate, endDate, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询失败");
        }
    }

    /**
     * 综合汇总指标
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 总采购金额/订单数/平均订单金额/供应商数等汇总数据
     */
    @Operation(summary = "综合汇总指标", description = "返回总采购金额、订单数、已结算金额、平均订单金额、供应商数、准时交付率、合格率等汇总指标")
    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public Result<PurchaseReportSummaryVO> getSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            PurchaseReportSummaryVO summary = analysisService.getSummary(startDate, endDate);
            return Result.success(summary, "查询成功");
        } catch (Exception e) {
            log.error("获取采购综合汇总指标失败: startDate={}, endDate={}", startDate, endDate, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "查询失败");
        }
    }
}
